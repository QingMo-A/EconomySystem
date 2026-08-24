package com.mo.economy_system.common.market;

import com.mo.economy_system.common.network.DeliverDemandOrderMessage;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import com.mo.economy_system.platform.item.ItemStackSnapshotValidator;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Server-authoritative DEMAND fulfillment with partial fills and staged mailbox settlement. */
public final class DemandOrderDeliveryService {
  private DemandOrderDeliveryService() {}

  public static DemandOrderDeliveryOutcome execute(
      DeliverDemandOrderMessage message, Context context) {
    if (message == null || context == null) {
      return DemandOrderDeliveryOutcome.validationFailure(DemandOrderDeliveryResult.INVALID_CONTEXT);
    }

    UUID tradeId = message.tradeId();
    MarketOrder order;
    try {
      order = context.repository().find(tradeId);
    } catch (RuntimeException error) {
      report(context, tradeId, null, "find", DemandOrderDeliveryResult.LEDGER_STATE_UNKNOWN,
          MarketMutationState.UNKNOWN, null, null, null, null, error, null, null, error);
      return DemandOrderDeliveryOutcome.uncertainFailure(DemandOrderDeliveryResult.LEDGER_STATE_UNKNOWN);
    }
    if (order == null) {
      return validation(context, tradeId, null, "find", DemandOrderDeliveryResult.NOT_FOUND, null);
    }

    UUID requesterId = order.sellerId();
    DemandOrderDeliveryResult validation = validate(order, context.supplierId());
    if (validation != DemandOrderDeliveryResult.SUCCESS) {
      return validation(context, tradeId, requesterId, "order-validation", validation, null);
    }

    int quantity = message.quantity() == 0 ? order.quantity() : message.quantity();
    if (quantity <= 0 || quantity > order.quantity()) {
      return validation(context, tradeId, requesterId, "quantity",
          DemandOrderDeliveryResult.INVALID_QUANTITY, null);
    }
    if (quantity < order.quantity() && !MarketOrderPricing.supportsPartialFill(order)) {
      return validation(context, tradeId, requesterId, "price",
          DemandOrderDeliveryResult.PARTIAL_FILL_UNSUPPORTED, null);
    }

    int amount;
    try {
      amount = MarketOrderPricing.fillAmount(order, quantity);
    } catch (ArithmeticException | IllegalArgumentException error) {
      return validation(context, tradeId, requesterId, "price",
          DemandOrderDeliveryResult.INVALID_PRICE, error);
    }

    Object template;
    try {
      template = context.materializer().restore(order);
    } catch (RuntimeException error) {
      return validation(context, tradeId, requesterId, "snapshot-restore",
          DemandOrderDeliveryResult.ITEM_RESTORE_FAILED, error);
    }
    if (template == null) {
      return validation(context, tradeId, requesterId, "snapshot-restore",
          DemandOrderDeliveryResult.ITEM_RESTORE_FAILED, null);
    }

    try {
      UUID ownerId = context.inventory().ownerId();
      if (ownerId == null || !context.supplierId().equals(ownerId)) {
        return validation(context, tradeId, requesterId, "inventory-owner",
            DemandOrderDeliveryResult.INVENTORY_CONTEXT_FAILED, null);
      }
    } catch (RuntimeException error) {
      return validation(context, tradeId, requesterId, "inventory-owner",
          DemandOrderDeliveryResult.INVENTORY_CONTEXT_FAILED, error);
    }

    BalanceMutationResult preview;
    try {
      preview = context.account().previewCreditExact(amount);
    } catch (RuntimeException error) {
      return validation(context, tradeId, requesterId, "payment-preview",
          DemandOrderDeliveryResult.PAYMENT_FAILED, error);
    }
    if (preview != BalanceMutationResult.SUCCESS) {
      DemandOrderDeliveryResult result = preview == BalanceMutationResult.BALANCE_LIMIT
          ? DemandOrderDeliveryResult.RECIPIENT_BALANCE_LIMIT
          : DemandOrderDeliveryResult.PAYMENT_FAILED;
      return validation(context, tradeId, requesterId, "payment-preview", result, null);
    }

    try {
      if (context.inventory().countMatching(template) < quantity) {
        return validation(context, tradeId, requesterId, "inventory-count",
            DemandOrderDeliveryResult.INSUFFICIENT_ITEMS, null);
      }
    } catch (RuntimeException error) {
      return validation(context, tradeId, requesterId, "inventory-count",
          DemandOrderDeliveryResult.INVENTORY_MUTATION_FAILED, error);
    }

    DemandMailboxResult mailboxPreflight;
    try {
      mailboxPreflight = context.mailbox().preflight(requesterId, template, quantity);
    } catch (RuntimeException error) {
      return validation(context, tradeId, requesterId, "mailbox-preflight",
          DemandOrderDeliveryResult.MAILBOX_DELIVERY_FAILED, error);
    }
    if (mailboxPreflight != DemandMailboxResult.SUCCESS) {
      return validation(context, tradeId, requesterId, "mailbox-preflight",
          mailboxPreflight == DemandMailboxResult.FULL
              ? DemandOrderDeliveryResult.MAILBOX_FULL
              : mailboxPreflight == DemandMailboxResult.UNKNOWN
                  ? DemandOrderDeliveryResult.MAILBOX_STATE_UNKNOWN
                  : DemandOrderDeliveryResult.MAILBOX_DELIVERY_FAILED,
          null);
    }

    // Reserve the exact authoritative order snapshot before mutating supplier inventory or mail.
    MarketPartialFillTransition transition;
    RuntimeException transitionError = null;
    try {
      transition = context.repository().fillIfUnchanged(
          tradeId, MarketOrderType.DEMAND, order, quantity);
    } catch (RuntimeException error) {
      transition = null;
      transitionError = error;
    }
    if (transition == null) {
      report(context, tradeId, requesterId, "ledger-transition",
          DemandOrderDeliveryResult.LEDGER_STATE_UNKNOWN, MarketMutationState.UNKNOWN,
          null, null, null, null, transitionError, null, null, transitionError);
      return DemandOrderDeliveryOutcome.uncertainFailure(
          DemandOrderDeliveryResult.LEDGER_STATE_UNKNOWN);
    }
    if (!transition.applied()) {
      return validation(context, tradeId, requesterId, "ledger-transition",
          map(transition.status()), transitionError);
    }

    MarketOrder fulfilled = filledSlice(order, quantity, amount);
    int remainingQuantity = transition.remainingOrder().map(MarketOrder::quantity).orElse(0);

    InventoryRemovalResult removal;
    try {
      removal = context.inventory().removeMatching(template, quantity);
    } catch (RuntimeException error) {
      report(context, tradeId, requesterId, "inventory-remove",
          DemandOrderDeliveryResult.ROLLBACK_FAILED, MarketMutationState.UNKNOWN,
          null, false, null, null, error, error, null, null);
      return DemandOrderDeliveryOutcome.uncertainFailure(DemandOrderDeliveryResult.ROLLBACK_FAILED);
    }
    if (removal == null) {
      report(context, tradeId, requesterId, "inventory-remove",
          DemandOrderDeliveryResult.ROLLBACK_FAILED, MarketMutationState.UNKNOWN,
          null, false, null, null, null, null, null, null);
      return DemandOrderDeliveryOutcome.uncertainFailure(DemandOrderDeliveryResult.ROLLBACK_FAILED);
    }
    if (!removal.succeeded()) {
      if (!removal.failureRestored()) {
        report(context, tradeId, requesterId, "inventory-remove",
            DemandOrderDeliveryResult.ROLLBACK_FAILED, MarketMutationState.UNKNOWN,
            false, false, null, null, null, null, null, null);
        return DemandOrderDeliveryOutcome.uncertainFailure(DemandOrderDeliveryResult.ROLLBACK_FAILED);
      }
      boolean orderRestored = rollbackOrder(transition);
      DemandOrderDeliveryResult result = orderRestored
          ? DemandOrderDeliveryResult.INVENTORY_MUTATION_FAILED
          : DemandOrderDeliveryResult.ROLLBACK_FAILED;
      report(context, tradeId, requesterId, "inventory-remove", result,
          orderRestored ? MarketMutationState.UNCHANGED : MarketMutationState.CHANGED,
          true, true, null, null, null, null, null, null);
      return orderRestored
          ? DemandOrderDeliveryOutcome.rolledBackFailure(result, order)
          : DemandOrderDeliveryOutcome.changedFailure(result, order);
    }
    InventoryRemovalRollback inventoryRollback = removal.rollback().orElseThrow();

    MailboxStage stage;
    RuntimeException mailboxError = null;
    try {
      stage = context.mailbox().stage(
          requesterId, fulfilled, template, quantity, amount, remainingQuantity);
    } catch (RuntimeException error) {
      stage = null;
      mailboxError = error;
    }
    if (stage == null) {
      report(context, tradeId, requesterId, "mailbox-stage",
          DemandOrderDeliveryResult.MAILBOX_STATE_UNKNOWN, MarketMutationState.UNKNOWN,
          null, null, null, null, mailboxError, null, null, mailboxError);
      return DemandOrderDeliveryOutcome.uncertainFailure(
          DemandOrderDeliveryResult.MAILBOX_STATE_UNKNOWN);
    }
    if (stage.result() != DemandMailboxResult.SUCCESS) {
      if (stage.result() == DemandMailboxResult.UNKNOWN) {
        report(context, tradeId, requesterId, "mailbox-stage",
            DemandOrderDeliveryResult.MAILBOX_STATE_UNKNOWN, MarketMutationState.UNKNOWN,
            null, null, null, null, mailboxError, null, null, mailboxError);
        return DemandOrderDeliveryOutcome.uncertainFailure(
            DemandOrderDeliveryResult.MAILBOX_STATE_UNKNOWN);
      }
      DemandOrderDeliveryResult result = stage.result() == DemandMailboxResult.FULL
          ? DemandOrderDeliveryResult.MAILBOX_FULL
          : DemandOrderDeliveryResult.MAILBOX_DELIVERY_FAILED;
      return compensateBeforePayment(context, tradeId, requesterId, order, inventoryRollback,
          transition, result, mailboxError);
    }
    StagedMailboxDelivery staged = stage.delivery().orElseThrow();

    // Payment is deliberately last. A known credit failure can therefore remove the staged mail
    // first and then safely restore supplier inventory and the exact previous order snapshot.
    BalanceMutationResult credit;
    RuntimeException creditError = null;
    try {
      credit = context.account().creditExact(amount);
    } catch (RuntimeException error) {
      credit = null;
      creditError = error;
    }
    if (credit == null) {
      report(context, tradeId, requesterId, "payment-credit",
          DemandOrderDeliveryResult.PAYMENT_STATE_UNKNOWN, MarketMutationState.UNKNOWN,
          null, null, null, null, creditError, null, creditError, null);
      return DemandOrderDeliveryOutcome.uncertainFailure(
          DemandOrderDeliveryResult.PAYMENT_STATE_UNKNOWN);
    }
    if (credit != BalanceMutationResult.SUCCESS) {
      DemandOrderDeliveryResult result = credit == BalanceMutationResult.BALANCE_LIMIT
          ? DemandOrderDeliveryResult.RECIPIENT_BALANCE_LIMIT
          : DemandOrderDeliveryResult.PAYMENT_FAILED;
      return compensateAfterStaging(context, tradeId, requesterId, order, inventoryRollback,
          transition, staged, result, creditError);
    }

    try {
      staged.commit();
    } catch (RuntimeException ignored) {
      // Online toast/sound is a non-authoritative side effect. Never unwind a committed economy
      // transaction because notification delivery failed.
    }
    return DemandOrderDeliveryOutcome.success(fulfilled);
  }

  private static DemandOrderDeliveryOutcome compensateBeforePayment(
      Context context,
      UUID tradeId,
      UUID requesterId,
      MarketOrder order,
      InventoryRemovalRollback inventoryRollback,
      MarketPartialFillTransition transition,
      DemandOrderDeliveryResult result,
      RuntimeException primaryError) {
    boolean inventoryRestored = rollbackInventory(inventoryRollback);
    boolean orderRestored = rollbackOrder(transition);
    DemandOrderDeliveryResult finalResult = inventoryRestored && orderRestored
        ? result : DemandOrderDeliveryResult.ROLLBACK_FAILED;
    MarketMutationState state = inventoryRestored && orderRestored
        ? MarketMutationState.UNCHANGED : MarketMutationState.UNKNOWN;
    report(context, tradeId, requesterId, "compensation", finalResult, state,
        null, inventoryRestored, null, null, primaryError, null, null, null);
    return state == MarketMutationState.UNCHANGED
        ? DemandOrderDeliveryOutcome.rolledBackFailure(finalResult, order)
        : DemandOrderDeliveryOutcome.uncertainFailure(finalResult);
  }

  private static DemandOrderDeliveryOutcome compensateAfterStaging(
      Context context,
      UUID tradeId,
      UUID requesterId,
      MarketOrder order,
      InventoryRemovalRollback inventoryRollback,
      MarketPartialFillTransition transition,
      StagedMailboxDelivery staged,
      DemandOrderDeliveryResult result,
      RuntimeException primaryError) {
    DemandMailboxResult mailboxRollback;
    try {
      mailboxRollback = staged.rollback();
    } catch (RuntimeException error) {
      mailboxRollback = DemandMailboxResult.UNKNOWN;
    }
    if (mailboxRollback != DemandMailboxResult.SUCCESS) {
      report(context, tradeId, requesterId, "compensation-mailbox",
          DemandOrderDeliveryResult.ROLLBACK_FAILED, MarketMutationState.UNKNOWN,
          null, null, false, null, primaryError, null, null, null);
      return DemandOrderDeliveryOutcome.uncertainFailure(DemandOrderDeliveryResult.ROLLBACK_FAILED);
    }
    return compensateBeforePayment(context, tradeId, requesterId, order, inventoryRollback,
        transition, result, primaryError);
  }

  private static boolean rollbackInventory(InventoryRemovalRollback rollback) {
    try {
      return rollback.rollback();
    } catch (RuntimeException error) {
      return false;
    }
  }

  private static boolean rollbackOrder(MarketPartialFillTransition transition) {
    try {
      return transition.rollback().isPresent()
          && transition.rollback().orElseThrow().rollback() == MarketPartialFillRollbackResult.RESTORED;
    } catch (RuntimeException error) {
      return false;
    }
  }

  private static MarketOrder filledSlice(MarketOrder order, int quantity, int amount) {
    return new MarketOrder(MarketOrderType.DEMAND, order.tradeId(), order.item(), quantity, amount,
        order.sellerName(), order.sellerId(), order.listingTime(), order.expirationTime(), false);
  }

  private static DemandOrderDeliveryResult validate(MarketOrder order, UUID supplierId) {
    if (order.type() != MarketOrderType.DEMAND) return DemandOrderDeliveryResult.WRONG_ORDER_TYPE;
    if (order.delivered()) return DemandOrderDeliveryResult.ALREADY_DELIVERED;
    if (order.sellerId().equals(supplierId)) return DemandOrderDeliveryResult.SELF_DELIVERY;
    if (order.totalPrice() <= 0) return DemandOrderDeliveryResult.INVALID_PRICE;
    if (order.quantity() <= 0) return DemandOrderDeliveryResult.INVALID_QUANTITY;
    if (order.item().count() != 1 || !ItemStackSnapshotValidator.validate(order.item()).isSuccess()) {
      return DemandOrderDeliveryResult.INVALID_SNAPSHOT;
    }
    return DemandOrderDeliveryResult.SUCCESS;
  }

  private static DemandOrderDeliveryResult map(MarketPartialFillStatus status) {
    return switch (status) {
      case NOT_FOUND -> DemandOrderDeliveryResult.NOT_FOUND;
      case WRONG_ORDER_TYPE -> DemandOrderDeliveryResult.WRONG_ORDER_TYPE;
      case ALREADY_DELIVERED -> DemandOrderDeliveryResult.ALREADY_DELIVERED;
      case ORDER_CHANGED -> DemandOrderDeliveryResult.ORDER_CHANGED;
      case INVALID_QUANTITY -> DemandOrderDeliveryResult.INVALID_QUANTITY;
      case NON_DIVISIBLE_PRICE -> DemandOrderDeliveryResult.PARTIAL_FILL_UNSUPPORTED;
      case PRICE_OVERFLOW -> DemandOrderDeliveryResult.INVALID_PRICE;
      case PERSIST_FAILED -> DemandOrderDeliveryResult.LEDGER_UPDATE_FAILED;
      case UPDATED, REMOVED -> throw new IllegalArgumentException("success transition mapped as failure");
    };
  }

  private static DemandOrderDeliveryOutcome validation(
      Context context, UUID tradeId, UUID requesterId, String stage,
      DemandOrderDeliveryResult result, RuntimeException error) {
    report(context, tradeId, requesterId, stage, result, MarketMutationState.UNCHANGED,
        null, null, null, null, error, null, null, null);
    return DemandOrderDeliveryOutcome.validationFailure(result);
  }

  private static void report(
      Context context,
      UUID tradeId,
      UUID requesterId,
      String stage,
      DemandOrderDeliveryResult result,
      MarketMutationState state,
      Boolean removalFailureRestored,
      Boolean inventoryRollback,
      Boolean paymentCredit,
      Boolean paymentReversal,
      RuntimeException primaryError,
      RuntimeException inventoryError,
      RuntimeException paymentError,
      RuntimeException repositoryError) {
    try {
      context.reporter().report(new DemandOrderDeliveryFailure(
          tradeId, context.supplierId(), requesterId, stage, result, state, null,
          removalFailureRestored, inventoryRollback, paymentCredit, paymentReversal,
          primaryError, inventoryError, paymentError, repositoryError));
    } catch (RuntimeException ignored) {
    }
  }

  public record Context(
      UUID supplierId,
      MarketItemMaterializer materializer,
      TransactionalInventoryRemoval inventory,
      Account account,
      Repository repository,
      Mailbox mailbox,
      FailureReporter reporter) {
    public Context {
      Objects.requireNonNull(supplierId, "supplierId");
      Objects.requireNonNull(materializer, "materializer");
      Objects.requireNonNull(inventory, "inventory");
      Objects.requireNonNull(account, "account");
      Objects.requireNonNull(repository, "repository");
      Objects.requireNonNull(mailbox, "mailbox");
      Objects.requireNonNull(reporter, "reporter");
    }
  }

  public interface Account {
    BalanceMutationResult previewCreditExact(int amount);
    BalanceMutationResult creditExact(int amount);

    /** Retained for source compatibility with existing loader adapters; v2 settlement no longer reverses payment. */
    default BalanceMutationResult debitExact(int amount) {
      return BalanceMutationResult.PERSIST_FAILED;
    }
  }

  public interface Repository {
    MarketOrder find(UUID id);
    MarketPartialFillTransition fillIfUnchanged(
        UUID id, MarketOrderType expectedType, MarketOrder expected, int quantity);
  }

  public interface Mailbox {
    DemandMailboxResult preflight(UUID requesterId, Object template, int quantity);

    MailboxStage stage(
        UUID requesterId,
        MarketOrder fulfilledSlice,
        Object template,
        int quantity,
        int amount,
        int remainingQuantity);
  }

  public record MailboxStage(DemandMailboxResult result, Optional<StagedMailboxDelivery> delivery) {
    public MailboxStage {
      Objects.requireNonNull(result, "result");
      delivery = Objects.requireNonNull(delivery, "delivery");
      if ((result == DemandMailboxResult.SUCCESS) != delivery.isPresent()) {
        throw new IllegalArgumentException("successful mailbox stage requires a staged delivery");
      }
    }

    public static MailboxStage success(StagedMailboxDelivery delivery) {
      return new MailboxStage(DemandMailboxResult.SUCCESS,
          Optional.of(Objects.requireNonNull(delivery, "delivery")));
    }

    public static MailboxStage failure(DemandMailboxResult result) {
      if (result == DemandMailboxResult.SUCCESS) throw new IllegalArgumentException("result");
      return new MailboxStage(result, Optional.empty());
    }
  }

  public interface StagedMailboxDelivery {
    /** Publishes non-authoritative side effects such as the online mailbox toast/sound. */
    void commit();

    /** Removes exactly this transaction's still-unclaimed mail metadata and delivery entries. */
    DemandMailboxResult rollback();
  }

  public interface FailureReporter {
    void report(DemandOrderDeliveryFailure failure);
    static FailureReporter noop() { return failure -> {}; }
  }
}

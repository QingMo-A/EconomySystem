package com.mo.economy_system.common.market;

import com.mo.economy_system.common.network.PurchaseSalesOrderMessage;
import com.mo.economy_system.core.economy_system.BalanceTransferResult;
import com.mo.economy_system.platform.item.ItemStackSnapshotValidator;
import java.util.Objects;
import java.util.UUID;

/** Server-authoritative SALES fill transaction supporting whole and partial purchases. */
public final class PurchaseSalesOrderService {
  private PurchaseSalesOrderService() {}

  public static PurchaseSalesOrderOutcome execute(
      PurchaseSalesOrderMessage message, Context context) {
    if (message == null || context == null)
      return PurchaseSalesOrderOutcome.validationFailure(PurchaseSalesOrderResult.INVALID_CONTEXT);

    MarketOrder preview;
    try {
      preview = context.repository().find(message.tradeId());
    } catch (RuntimeException exception) {
      report(context, message.tradeId(), null, "lookup",
          PurchaseSalesOrderResult.ORDER_REMOVE_FAILED, null, null, exception);
      return PurchaseSalesOrderOutcome.validationFailure(PurchaseSalesOrderResult.ORDER_REMOVE_FAILED);
    }

    PurchaseSalesOrderResult validation = validate(preview, context);
    if (validation != PurchaseSalesOrderResult.SUCCESS)
      return PurchaseSalesOrderOutcome.validationFailure(validation);

    int quantity = message.quantity() == 0 ? preview.quantity() : message.quantity();
    if (quantity <= 0 || quantity > preview.quantity())
      return PurchaseSalesOrderOutcome.validationFailure(PurchaseSalesOrderResult.INVALID_QUANTITY);
    if (quantity < preview.quantity() && !MarketOrderPricing.supportsPartialFill(preview))
      return PurchaseSalesOrderOutcome.validationFailure(PurchaseSalesOrderResult.PARTIAL_FILL_UNSUPPORTED);

    int amount;
    try {
      amount = MarketOrderPricing.fillAmount(preview, quantity);
    } catch (ArithmeticException | IllegalArgumentException exception) {
      return PurchaseSalesOrderOutcome.validationFailure(PurchaseSalesOrderResult.INVALID_PRICE);
    }

    Object template;
    try {
      template = context.materializer().restore(preview);
    } catch (RuntimeException exception) {
      report(context, message.tradeId(), preview.sellerId(), "snapshot-restore",
          PurchaseSalesOrderResult.ITEM_RESTORE_FAILED, null, null, exception);
      return PurchaseSalesOrderOutcome.validationFailure(PurchaseSalesOrderResult.ITEM_RESTORE_FAILED);
    }
    if (template == null)
      return PurchaseSalesOrderOutcome.validationFailure(PurchaseSalesOrderResult.ITEM_RESTORE_FAILED);

    BalanceTransferResult balancePreview;
    try {
      balancePreview = context.accounts().preview(preview.sellerId(), amount);
    } catch (RuntimeException exception) {
      report(context, message.tradeId(), preview.sellerId(), "payment-preview",
          PurchaseSalesOrderResult.PAYMENT_FAILED, null, null, exception);
      return PurchaseSalesOrderOutcome.validationFailure(PurchaseSalesOrderResult.PAYMENT_FAILED);
    }
    if (balancePreview == null)
      return PurchaseSalesOrderOutcome.validationFailure(PurchaseSalesOrderResult.PAYMENT_FAILED);
    PurchaseSalesOrderResult balanceFailure = mapBalanceFailure(balancePreview);
    if (balanceFailure != PurchaseSalesOrderResult.SUCCESS)
      return PurchaseSalesOrderOutcome.validationFailure(balanceFailure);

    boolean accepts;
    try {
      accepts = context.inventory().canAccept(template, quantity);
    } catch (RuntimeException exception) {
      report(context, message.tradeId(), preview.sellerId(), "inventory-capacity",
          PurchaseSalesOrderResult.INVENTORY_MUTATION_FAILED, null, null, exception);
      return PurchaseSalesOrderOutcome.validationFailure(PurchaseSalesOrderResult.INVENTORY_MUTATION_FAILED);
    }
    if (!accepts)
      return PurchaseSalesOrderOutcome.validationFailure(PurchaseSalesOrderResult.INVENTORY_FULL);

    MarketPartialFillTransition transition;
    try {
      transition = context.repository().fillIfUnchanged(
          message.tradeId(), MarketOrderType.SALES, preview, quantity);
    } catch (RuntimeException exception) {
      report(context, message.tradeId(), preview.sellerId(), "order-fill",
          PurchaseSalesOrderResult.ORDER_REMOVE_FAILED, null, null, exception);
      return PurchaseSalesOrderOutcome.validationFailure(PurchaseSalesOrderResult.ORDER_REMOVE_FAILED);
    }
    if (transition == null)
      return PurchaseSalesOrderOutcome.uncertainFailure(PurchaseSalesOrderResult.ORDER_REMOVE_FAILED);
    if (!transition.applied())
      return PurchaseSalesOrderOutcome.validationFailure(mapTransitionFailure(transition.status()));

    MarketOrder authoritative = transition.previousOrder().orElse(preview);

    InventoryInsertionResult insertion;
    try {
      insertion = context.inventory().insert(template, quantity);
    } catch (RuntimeException exception) {
      boolean orderRestored = rollbackOrder(transition);
      PurchaseSalesOrderResult result = orderRestored
          ? PurchaseSalesOrderResult.INVENTORY_MUTATION_FAILED
          : PurchaseSalesOrderResult.ROLLBACK_FAILED;
      report(context, message.tradeId(), authoritative.sellerId(), "inventory-insert",
          result, false, orderRestored, exception);
      return orderRestored
          ? PurchaseSalesOrderOutcome.rolledBackFailure(result, authoritative)
          : PurchaseSalesOrderOutcome.changedFailure(result, authoritative);
    }

    if (insertion == null || !insertion.succeeded() || insertion.rollback() == null) {
      boolean inventoryRestored = insertion != null && insertion.failureRestored();
      boolean orderRestored = rollbackOrder(transition);
      PurchaseSalesOrderResult result = inventoryRestored && orderRestored
          ? PurchaseSalesOrderResult.INVENTORY_MUTATION_FAILED
          : PurchaseSalesOrderResult.ROLLBACK_FAILED;
      report(context, message.tradeId(), authoritative.sellerId(), "inventory-insert",
          result, inventoryRestored, orderRestored, null);
      return orderRestored
          ? PurchaseSalesOrderOutcome.rolledBackFailure(result, authoritative)
          : PurchaseSalesOrderOutcome.changedFailure(result, authoritative);
    }

    BalanceTransferResult transfer;
    RuntimeException transferException = null;
    try {
      transfer = context.accounts().transfer(authoritative.sellerId(), amount);
    } catch (RuntimeException exception) {
      transfer = BalanceTransferResult.PERSIST_FAILED;
      transferException = exception;
    }
    if (transfer == null) transfer = BalanceTransferResult.PERSIST_FAILED;
    if (transfer != BalanceTransferResult.SUCCESS) {
      boolean inventoryRestored = rollbackInventory(insertion.rollback());
      boolean orderRestored = rollbackOrder(transition);
      PurchaseSalesOrderResult result = inventoryRestored && orderRestored
          ? mapCommittedTransferFailure(transfer)
          : PurchaseSalesOrderResult.ROLLBACK_FAILED;
      report(context, message.tradeId(), authoritative.sellerId(), "payment",
          result, inventoryRestored, orderRestored, transferException);
      return orderRestored
          ? PurchaseSalesOrderOutcome.rolledBackFailure(result, authoritative)
          : PurchaseSalesOrderOutcome.changedFailure(result, authoritative);
    }

    return PurchaseSalesOrderOutcome.success(filledSlice(authoritative, quantity, amount));
  }

  private static PurchaseSalesOrderResult validate(MarketOrder order, Context context) {
    if (order == null) return PurchaseSalesOrderResult.NOT_FOUND;
    if (order.type() != MarketOrderType.SALES) return PurchaseSalesOrderResult.WRONG_ORDER_TYPE;
    if (context.buyerId().equals(order.sellerId())) return PurchaseSalesOrderResult.SELF_PURCHASE;
    if (order.totalPrice() <= 0) return PurchaseSalesOrderResult.INVALID_PRICE;
    if (order.quantity() <= 0) return PurchaseSalesOrderResult.INVALID_QUANTITY;
    if (order.item().count() != 1
        || !ItemStackSnapshotValidator.validate(order.item()).isSuccess())
      return PurchaseSalesOrderResult.INVALID_SNAPSHOT;
    return PurchaseSalesOrderResult.SUCCESS;
  }

  private static MarketOrder filledSlice(MarketOrder order, int quantity, int amount) {
    return new MarketOrder(order.type(), order.tradeId(), order.item(), quantity, amount,
        order.sellerName(), order.sellerId(), order.listingTime(), order.expirationTime(), false);
  }

  private static PurchaseSalesOrderResult mapTransitionFailure(MarketPartialFillStatus status) {
    return switch (status) {
      case NOT_FOUND -> PurchaseSalesOrderResult.NOT_FOUND;
      case WRONG_ORDER_TYPE -> PurchaseSalesOrderResult.WRONG_ORDER_TYPE;
      case ORDER_CHANGED, ALREADY_DELIVERED -> PurchaseSalesOrderResult.ORDER_CHANGED;
      case INVALID_QUANTITY -> PurchaseSalesOrderResult.INVALID_QUANTITY;
      case NON_DIVISIBLE_PRICE -> PurchaseSalesOrderResult.PARTIAL_FILL_UNSUPPORTED;
      case PRICE_OVERFLOW -> PurchaseSalesOrderResult.INVALID_PRICE;
      case PERSIST_FAILED -> PurchaseSalesOrderResult.ORDER_REMOVE_FAILED;
      case UPDATED, REMOVED -> throw new IllegalArgumentException("success transition mapped as failure");
    };
  }

  private static PurchaseSalesOrderResult mapBalanceFailure(BalanceTransferResult result) {
    return switch (result) {
      case SUCCESS -> PurchaseSalesOrderResult.SUCCESS;
      case INSUFFICIENT_FUNDS -> PurchaseSalesOrderResult.INSUFFICIENT_FUNDS;
      case RECIPIENT_BALANCE_LIMIT -> PurchaseSalesOrderResult.SELLER_BALANCE_LIMIT;
      default -> PurchaseSalesOrderResult.PAYMENT_FAILED;
    };
  }

  private static PurchaseSalesOrderResult mapCommittedTransferFailure(BalanceTransferResult result) {
    PurchaseSalesOrderResult mapped = mapBalanceFailure(result);
    return mapped == PurchaseSalesOrderResult.SUCCESS
        ? PurchaseSalesOrderResult.PAYMENT_FAILED : mapped;
  }

  private static boolean rollbackOrder(MarketPartialFillTransition transition) {
    try {
      return transition.rollback().isPresent()
          && transition.rollback().orElseThrow().rollback() == MarketPartialFillRollbackResult.RESTORED;
    } catch (RuntimeException exception) {
      return false;
    }
  }

  private static boolean rollbackInventory(InventoryRollback rollback) {
    try {
      return rollback.rollback();
    } catch (RuntimeException exception) {
      return false;
    }
  }

  private static void report(
      Context context,
      UUID tradeId,
      UUID sellerId,
      String stage,
      PurchaseSalesOrderResult result,
      Boolean inventoryRollback,
      Boolean orderRestore,
      RuntimeException exception) {
    try {
      context.reporter().report(tradeId, context.buyerId(), sellerId, stage, result,
          inventoryRollback, orderRestore, exception);
    } catch (RuntimeException ignored) {
    }
  }

  public record Context(
      UUID buyerId,
      MarketItemMaterializer materializer,
      TransactionalInventory inventory,
      Accounts accounts,
      Repository repository,
      FailureReporter reporter) {
    public Context {
      Objects.requireNonNull(buyerId);
      Objects.requireNonNull(materializer);
      Objects.requireNonNull(inventory);
      Objects.requireNonNull(accounts);
      Objects.requireNonNull(repository);
      Objects.requireNonNull(reporter);
    }
  }

  public interface Accounts {
    BalanceTransferResult preview(UUID sellerId, int amount);
    BalanceTransferResult transfer(UUID sellerId, int amount);
  }

  public interface Repository {
    MarketOrder find(UUID tradeId);
    MarketPartialFillTransition fillIfUnchanged(
        UUID tradeId, MarketOrderType expectedType, MarketOrder expected, int quantity);
  }

  public interface FailureReporter {
    void report(
        UUID tradeId,
        UUID buyerId,
        UUID sellerId,
        String stage,
        PurchaseSalesOrderResult result,
        Boolean inventoryRollback,
        Boolean orderRestore,
        RuntimeException exception);

    static FailureReporter noop() {
      return (tradeId, buyerId, sellerId, stage, result, inventoryRollback, orderRestore, exception) -> {};
    }
  }
}

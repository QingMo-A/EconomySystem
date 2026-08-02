package com.mo.economy_system.common.market;

import com.mo.economy_system.common.network.DeliverDemandOrderMessage;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import com.mo.economy_system.platform.item.ItemStackSnapshotValidator;
import java.util.Objects;
import java.util.UUID;

public final class DemandOrderDeliveryService {
  private DemandOrderDeliveryService() {}

  public static DemandOrderDeliveryOutcome execute(DeliverDemandOrderMessage message, Context context) {
    if (message == null || context == null)
      return DemandOrderDeliveryOutcome.validationFailure(DemandOrderDeliveryResult.INVALID_CONTEXT);
    UUID tradeId = message.tradeId();
    MarketOrder order;
    try {
      order = context.repository().find(tradeId);
    } catch (RuntimeException error) {
      return fail(context, tradeId, null, "find", DemandOrderDeliveryResult.LEDGER_STATE_UNKNOWN,
          MarketMutationState.UNKNOWN, null, null, null, null, null, error, null, null, error);
    }
    if (order == null)
      return validation(context, tradeId, null, "find", DemandOrderDeliveryResult.NOT_FOUND, null);
    UUID requesterId = order.sellerId();
    DemandOrderDeliveryResult validation = validate(order, context.supplierId());
    if (validation != DemandOrderDeliveryResult.SUCCESS)
      return validation(context, tradeId, requesterId, "order-validation", validation, null);

    Object template;
    try {
      template = context.materializer().restore(order);
    } catch (RuntimeException error) {
      return validation(context, tradeId, requesterId, "snapshot-restore",
          DemandOrderDeliveryResult.ITEM_RESTORE_FAILED, error);
    }
    if (template == null)
      return validation(context, tradeId, requesterId, "snapshot-restore",
          DemandOrderDeliveryResult.ITEM_RESTORE_FAILED, null);

    try {
      UUID ownerId = context.inventory().ownerId();
      if (ownerId == null || !context.supplierId().equals(ownerId))
        return validation(context, tradeId, requesterId, "inventory-owner",
            DemandOrderDeliveryResult.INVENTORY_CONTEXT_FAILED, null);
    } catch (RuntimeException error) {
      return validation(context, tradeId, requesterId, "inventory-owner",
          DemandOrderDeliveryResult.INVENTORY_CONTEXT_FAILED, error);
    }

    BalanceMutationResult preview;
    try {
      preview = context.account().previewCreditExact(order.totalPrice());
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
      if (context.inventory().countMatching(template) < order.quantity())
        return validation(context, tradeId, requesterId, "inventory-count",
            DemandOrderDeliveryResult.INSUFFICIENT_ITEMS, null);
    } catch (RuntimeException error) {
      return validation(context, tradeId, requesterId, "inventory-count",
          DemandOrderDeliveryResult.INVENTORY_MUTATION_FAILED, error);
    }

    InventoryRemovalResult removal;
    try {
      removal = context.inventory().removeMatching(template, order.quantity());
    } catch (RuntimeException error) {
      return rolledBack(context, tradeId, order, "inventory-remove",
          DemandOrderDeliveryResult.ROLLBACK_FAILED, false, null, error, null);
    }
    if (removal == null)
      return rolledBack(context, tradeId, order, "inventory-remove",
          DemandOrderDeliveryResult.ROLLBACK_FAILED, false, null, null, null);
    if (!removal.succeeded()) {
      DemandOrderDeliveryResult result = removal.failureRestored()
          ? DemandOrderDeliveryResult.INVENTORY_MUTATION_FAILED
          : DemandOrderDeliveryResult.ROLLBACK_FAILED;
      return rolledBack(context, tradeId, order, "inventory-remove", result,
          removal.failureRestored(), null, null, null);
    }
    InventoryRemovalRollback rollback = removal.rollback().orElseThrow();

    BalanceMutationResult credit;
    RuntimeException creditError = null;
    try {
      credit = context.account().creditExact(order.totalPrice());
    } catch (RuntimeException error) {
      credit = null;
      creditError = error;
    }
    if (credit != BalanceMutationResult.SUCCESS) {
      RollbackAttempt inventory = rollback(rollback);
      DemandOrderDeliveryResult result = inventory.succeeded()
          ? credit == BalanceMutationResult.BALANCE_LIMIT
              ? DemandOrderDeliveryResult.RECIPIENT_BALANCE_LIMIT
              : DemandOrderDeliveryResult.PAYMENT_FAILED
          : DemandOrderDeliveryResult.ROLLBACK_FAILED;
      return rolledBack(context, tradeId, order, "payment-credit", result, null,
          inventory.succeeded(), creditError, inventory.error());
    }

    DemandDeliveryTransition transition;
    RuntimeException repositoryError = null;
    try {
      transition = context.repository().markDemandDeliveredIfUnchanged(tradeId, order);
    } catch (RuntimeException error) {
      transition = null;
      repositoryError = error;
    }
    if (transition != null && transition.status() == DemandDeliveryTransitionStatus.UPDATED) {
      MarketOrder updated = transition.updatedOrder().orElse(null);
      if (updated != null) return DemandOrderDeliveryOutcome.success(updated);
      transition = null;
    }

    MarketMutationState marketState = transition == null
        ? MarketMutationState.UNKNOWN
        : transition.status() == DemandDeliveryTransitionStatus.PERSIST_FAILED
            ? MarketMutationState.UNCHANGED : MarketMutationState.CHANGED;
    DemandDeliveryTransitionStatus status = transition == null ? null : transition.status();
    safeReport(context, new DemandOrderDeliveryFailure(tradeId, context.supplierId(), requesterId,
        "ledger-transition", transition == null ? DemandOrderDeliveryResult.LEDGER_STATE_UNKNOWN
            : map(transition.status()), marketState, status, null, null, true, null,
        repositoryError, null, null, repositoryError));
    DemandDeliveryCompensation compensation = compensate(context, order.totalPrice(), rollback);
    DemandOrderDeliveryResult result;
    if (!compensation.complete()) result = DemandOrderDeliveryResult.ROLLBACK_FAILED;
    else if (transition == null) result = DemandOrderDeliveryResult.LEDGER_STATE_UNKNOWN;
    else result = map(transition.status());
    safeReport(context, new DemandOrderDeliveryFailure(tradeId, context.supplierId(), requesterId,
        "compensation", result, marketState, status, null, compensation.inventoryRestored(), true,
        compensation.paymentReverted(), repositoryError, compensation.inventoryError(),
        compensation.paymentError(), repositoryError));
    if (marketState == MarketMutationState.UNKNOWN)
      return DemandOrderDeliveryOutcome.uncertainFailure(result);
    if (marketState == MarketMutationState.CHANGED)
      return DemandOrderDeliveryOutcome.changedFailure(result, order);
    return DemandOrderDeliveryOutcome.rolledBackFailure(result, order);
  }

  private static DemandOrderDeliveryResult validate(MarketOrder order, UUID supplierId) {
    if (order.type() != MarketOrderType.DEMAND) return DemandOrderDeliveryResult.WRONG_ORDER_TYPE;
    if (order.delivered()) return DemandOrderDeliveryResult.ALREADY_DELIVERED;
    if (order.sellerId().equals(supplierId)) return DemandOrderDeliveryResult.SELF_DELIVERY;
    if (order.totalPrice() <= 0) return DemandOrderDeliveryResult.INVALID_PRICE;
    if (order.quantity() <= 0) return DemandOrderDeliveryResult.INVALID_QUANTITY;
    if (order.item().count() != 1 || !ItemStackSnapshotValidator.validate(order.item()).isSuccess())
      return DemandOrderDeliveryResult.INVALID_SNAPSHOT;
    return DemandOrderDeliveryResult.SUCCESS;
  }

  private static DemandDeliveryCompensation compensate(Context context, int amount,
      InventoryRemovalRollback rollback) {
    boolean payment = false;
    RuntimeException paymentError = null;
    try {
      payment = context.account().debitExact(amount) == BalanceMutationResult.SUCCESS;
    } catch (RuntimeException error) {
      paymentError = error;
    }
    RollbackAttempt inventory = rollback(rollback);
    return new DemandDeliveryCompensation(true, payment, true, inventory.succeeded(),
        paymentError, inventory.error());
  }

  private static RollbackAttempt rollback(InventoryRemovalRollback rollback) {
    try {
      return new RollbackAttempt(rollback.rollback(), null);
    } catch (RuntimeException error) {
      return new RollbackAttempt(false, error);
    }
  }

  private static DemandOrderDeliveryResult map(DemandDeliveryTransitionStatus status) {
    return switch (status) {
      case NOT_FOUND -> DemandOrderDeliveryResult.NOT_FOUND;
      case WRONG_ORDER_TYPE -> DemandOrderDeliveryResult.WRONG_ORDER_TYPE;
      case ALREADY_DELIVERED -> DemandOrderDeliveryResult.ALREADY_DELIVERED;
      case ORDER_CHANGED -> DemandOrderDeliveryResult.ORDER_CHANGED;
      case PERSIST_FAILED -> DemandOrderDeliveryResult.LEDGER_UPDATE_FAILED;
      case UPDATED -> throw new IllegalArgumentException("invalid failed transition");
    };
  }

  private static DemandOrderDeliveryOutcome validation(Context context, UUID tradeId,
      UUID requesterId, String stage, DemandOrderDeliveryResult result, RuntimeException error) {
    safeReport(context, new DemandOrderDeliveryFailure(tradeId, context.supplierId(), requesterId,
        stage, result, MarketMutationState.UNCHANGED, null, null, null, null, null,
        error, null, null, null));
    return DemandOrderDeliveryOutcome.validationFailure(result);
  }

  private static DemandOrderDeliveryOutcome rolledBack(Context context, UUID tradeId,
      MarketOrder order, String stage, DemandOrderDeliveryResult result, Boolean removalRestored,
      Boolean rollbackSucceeded, RuntimeException primaryError, RuntimeException inventoryError) {
    safeReport(context, new DemandOrderDeliveryFailure(tradeId, context.supplierId(), order.sellerId(),
        stage, result, MarketMutationState.UNCHANGED, null, removalRestored, rollbackSucceeded,
        false, null, primaryError, inventoryError, null, null));
    return DemandOrderDeliveryOutcome.rolledBackFailure(result, order);
  }

  private static DemandOrderDeliveryOutcome fail(Context context, UUID tradeId, UUID requesterId,
      String stage, DemandOrderDeliveryResult result, MarketMutationState state,
      DemandDeliveryTransitionStatus transitionStatus, Boolean removalRestored,
      Boolean rollbackSucceeded, Boolean creditSucceeded, Boolean reversalSucceeded,
      RuntimeException primaryError, RuntimeException inventoryError,
      RuntimeException paymentError, RuntimeException repositoryError) {
    safeReport(context, new DemandOrderDeliveryFailure(tradeId, context.supplierId(), requesterId,
        stage, result, state, transitionStatus, removalRestored, rollbackSucceeded, creditSucceeded,
        reversalSucceeded, primaryError, inventoryError, paymentError, repositoryError));
    return DemandOrderDeliveryOutcome.uncertainFailure(result);
  }

  private static void safeReport(Context context, DemandOrderDeliveryFailure failure) {
    try {
      context.reporter().report(failure);
    } catch (RuntimeException ignored) {
    }
  }

  private record RollbackAttempt(boolean succeeded, RuntimeException error) {}

  public record Context(UUID supplierId, MarketItemMaterializer materializer,
      TransactionalInventoryRemoval inventory, Account account, Repository repository,
      FailureReporter reporter) {
    public Context {
      Objects.requireNonNull(supplierId, "supplierId");
      Objects.requireNonNull(materializer, "materializer");
      Objects.requireNonNull(inventory, "inventory");
      Objects.requireNonNull(account, "account");
      Objects.requireNonNull(repository, "repository");
      Objects.requireNonNull(reporter, "reporter");
    }
  }

  public interface Account {
    BalanceMutationResult previewCreditExact(int amount);
    BalanceMutationResult creditExact(int amount);
    BalanceMutationResult debitExact(int amount);
  }

  public interface Repository {
    MarketOrder find(UUID id);
    DemandDeliveryTransition markDemandDeliveredIfUnchanged(UUID id, MarketOrder expected);
  }

  public interface FailureReporter {
    void report(DemandOrderDeliveryFailure failure);
    static FailureReporter noop() { return failure -> {}; }
  }
}

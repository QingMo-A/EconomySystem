package com.mo.economy_system.common.market;

import com.mo.economy_system.common.network.DeliverDemandOrderMessage;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import com.mo.economy_system.platform.item.ItemStackSnapshotValidator;
import java.util.*;

public final class DemandOrderDeliveryService {
  private DemandOrderDeliveryService() {}

  public static DemandOrderDeliveryOutcome execute(DeliverDemandOrderMessage message, Context c) {
    if (message == null || c == null)
      return DemandOrderDeliveryOutcome.validationFailure(
          DemandOrderDeliveryResult.INVALID_CONTEXT);
    MarketOrder order;
    try {
      order = c.repository.find(message.tradeId());
    } catch (RuntimeException e) {
      report(c, message.tradeId(), "find", DemandOrderDeliveryResult.LEDGER_STATE_UNKNOWN, e);
      return DemandOrderDeliveryOutcome.uncertainFailure(
          DemandOrderDeliveryResult.LEDGER_STATE_UNKNOWN);
    }
    DemandOrderDeliveryResult invalid = validate(order, c);
    if (invalid != DemandOrderDeliveryResult.SUCCESS)
      return DemandOrderDeliveryOutcome.validationFailure(invalid);
    Object template;
    try {
      template = c.materializer.restore(order);
    } catch (RuntimeException e) {
      return DemandOrderDeliveryOutcome.validationFailure(
          DemandOrderDeliveryResult.ITEM_RESTORE_FAILED);
    }
    if (template == null)
      return DemandOrderDeliveryOutcome.validationFailure(
          DemandOrderDeliveryResult.ITEM_RESTORE_FAILED);
    try {
      if (!c.supplierId.equals(c.inventory.ownerId()))
        return DemandOrderDeliveryOutcome.validationFailure(
            DemandOrderDeliveryResult.INVENTORY_CONTEXT_FAILED);
    } catch (RuntimeException e) {
      return DemandOrderDeliveryOutcome.validationFailure(
          DemandOrderDeliveryResult.INVENTORY_CONTEXT_FAILED);
    }
    BalanceMutationResult preview;
    try {
      preview = c.account.previewCreditExact(order.totalPrice());
    } catch (RuntimeException e) {
      preview = null;
    }
    if (preview == null)
      return DemandOrderDeliveryOutcome.validationFailure(DemandOrderDeliveryResult.PAYMENT_FAILED);
    if (preview == BalanceMutationResult.BALANCE_LIMIT)
      return DemandOrderDeliveryOutcome.validationFailure(
          DemandOrderDeliveryResult.RECIPIENT_BALANCE_LIMIT);
    if (preview != BalanceMutationResult.SUCCESS)
      return DemandOrderDeliveryOutcome.validationFailure(DemandOrderDeliveryResult.PAYMENT_FAILED);
    try {
      if (c.inventory.countMatching(template) < order.quantity())
        return DemandOrderDeliveryOutcome.validationFailure(
            DemandOrderDeliveryResult.INSUFFICIENT_ITEMS);
    } catch (RuntimeException e) {
      return DemandOrderDeliveryOutcome.validationFailure(
          DemandOrderDeliveryResult.INVENTORY_MUTATION_FAILED);
    }
    InventoryRemovalResult removal;
    try {
      removal = c.inventory.removeMatching(template, order.quantity());
    } catch (RuntimeException e) {
      return DemandOrderDeliveryOutcome.rolledBackFailure(
          DemandOrderDeliveryResult.ROLLBACK_FAILED, order);
    }
    if (removal == null)
      return DemandOrderDeliveryOutcome.rolledBackFailure(
          DemandOrderDeliveryResult.ROLLBACK_FAILED, order);
    if (!removal.succeeded())
      return DemandOrderDeliveryOutcome.rolledBackFailure(
          removal.failureRestored()
              ? DemandOrderDeliveryResult.INVENTORY_MUTATION_FAILED
              : DemandOrderDeliveryResult.ROLLBACK_FAILED,
          order);
    InventoryRemovalRollback rollback = removal.rollback().orElseThrow();
    BalanceMutationResult credit;
    try {
      credit = c.account.creditExact(order.totalPrice());
    } catch (RuntimeException e) {
      credit = null;
    }
    if (credit != BalanceMutationResult.SUCCESS) {
      boolean restored = rollback(rollback);
      DemandOrderDeliveryResult r =
          restored && credit == BalanceMutationResult.BALANCE_LIMIT
              ? DemandOrderDeliveryResult.RECIPIENT_BALANCE_LIMIT
              : restored
                  ? DemandOrderDeliveryResult.PAYMENT_FAILED
                  : DemandOrderDeliveryResult.ROLLBACK_FAILED;
      return DemandOrderDeliveryOutcome.rolledBackFailure(r, order);
    }
    DemandDeliveryTransition transition;
    try {
      transition = c.repository.markDemandDeliveredIfUnchanged(message.tradeId(), order);
    } catch (RuntimeException e) {
      transition = null;
    }
    if (transition == null) {
      boolean payment = debit(c, order.totalPrice());
      boolean inventory = rollback(rollback);
      return DemandOrderDeliveryOutcome.uncertainFailure(
          payment && inventory
              ? DemandOrderDeliveryResult.LEDGER_STATE_UNKNOWN
              : DemandOrderDeliveryResult.ROLLBACK_FAILED);
    }
    if (transition.status() == DemandDeliveryTransitionStatus.UPDATED)
      return DemandOrderDeliveryOutcome.success(transition.updatedOrder().orElseThrow());
    boolean payment = debit(c, order.totalPrice()), inventory = rollback(rollback);
    boolean complete = payment && inventory;
    DemandOrderDeliveryResult result = map(transition.status());
    if (!complete) result = DemandOrderDeliveryResult.ROLLBACK_FAILED;
    return transition.status() == DemandDeliveryTransitionStatus.PERSIST_FAILED
        ? DemandOrderDeliveryOutcome.rolledBackFailure(result, order)
        : DemandOrderDeliveryOutcome.changedFailure(result, order);
  }

  private static DemandOrderDeliveryResult validate(MarketOrder o, Context c) {
    if (o == null) return DemandOrderDeliveryResult.NOT_FOUND;
    if (o.type() != MarketOrderType.DEMAND) return DemandOrderDeliveryResult.WRONG_ORDER_TYPE;
    if (o.delivered()) return DemandOrderDeliveryResult.ALREADY_DELIVERED;
    if (o.sellerId().equals(c.supplierId)) return DemandOrderDeliveryResult.SELF_DELIVERY;
    if (o.totalPrice() <= 0) return DemandOrderDeliveryResult.INVALID_PRICE;
    if (o.quantity() <= 0) return DemandOrderDeliveryResult.INVALID_QUANTITY;
    if (o.item().count() != 1 || !ItemStackSnapshotValidator.validate(o.item()).isSuccess())
      return DemandOrderDeliveryResult.INVALID_SNAPSHOT;
    return DemandOrderDeliveryResult.SUCCESS;
  }

  private static DemandOrderDeliveryResult map(DemandDeliveryTransitionStatus s) {
    return switch (s) {
      case NOT_FOUND -> DemandOrderDeliveryResult.NOT_FOUND;
      case WRONG_ORDER_TYPE -> DemandOrderDeliveryResult.WRONG_ORDER_TYPE;
      case ALREADY_DELIVERED -> DemandOrderDeliveryResult.ALREADY_DELIVERED;
      case ORDER_CHANGED -> DemandOrderDeliveryResult.ORDER_CHANGED;
      case PERSIST_FAILED -> DemandOrderDeliveryResult.LEDGER_UPDATE_FAILED;
      case UPDATED -> DemandOrderDeliveryResult.SUCCESS;
    };
  }

  private static boolean rollback(InventoryRemovalRollback r) {
    try {
      return r.rollback();
    } catch (RuntimeException e) {
      return false;
    }
  }

  private static boolean debit(Context c, int a) {
    try {
      return c.account.debitExact(a) == BalanceMutationResult.SUCCESS;
    } catch (RuntimeException e) {
      return false;
    }
  }

  private static void report(
      Context c, UUID id, String s, DemandOrderDeliveryResult r, RuntimeException e) {
    try {
      c.reporter.report(id, c.supplierId, o(c, id), s, r, e);
    } catch (RuntimeException ignored) {
    }
  }

  private static UUID o(Context c, UUID id) {
    return null;
  }

  public record Context(
      UUID supplierId,
      MarketItemMaterializer materializer,
      TransactionalInventoryRemoval inventory,
      Account account,
      Repository repository,
      FailureReporter reporter) {
    public Context {
      Objects.requireNonNull(supplierId);
      Objects.requireNonNull(materializer);
      Objects.requireNonNull(inventory);
      Objects.requireNonNull(account);
      Objects.requireNonNull(repository);
      Objects.requireNonNull(reporter);
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
    void report(
        UUID tradeId,
        UUID supplierId,
        UUID requesterId,
        String stage,
        DemandOrderDeliveryResult result,
        RuntimeException error);

    static FailureReporter noop() {
      return (a, b, c, d, e, f) -> {};
    }
  }
}

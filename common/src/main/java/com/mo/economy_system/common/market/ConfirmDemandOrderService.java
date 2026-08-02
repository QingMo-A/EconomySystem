package com.mo.economy_system.common.market;

import com.mo.economy_system.common.network.ConfirmDemandOrderMessage;
import com.mo.economy_system.platform.item.ItemStackSnapshotValidator;
import java.util.*;

public final class ConfirmDemandOrderService {
  private ConfirmDemandOrderService() {}

  public static ConfirmDemandOrderOutcome execute(ConfirmDemandOrderMessage m, Context c) {
    if (m == null || c == null)
      return ConfirmDemandOrderOutcome.validationFailure(ConfirmDemandOrderResult.INVALID_CONTEXT);
    MarketOrder preview;
    try {
      preview = c.repository.find(m.tradeId());
    } catch (RuntimeException e) {
      report(
          c,
          m.tradeId(),
          null,
          "find",
          ConfirmDemandOrderResult.ORDER_REMOVE_FAILED,
          null,
          null,
          e);
      return ConfirmDemandOrderOutcome.validationFailure(
          ConfirmDemandOrderResult.ORDER_REMOVE_FAILED);
    }
    ConfirmDemandOrderResult v = validate(preview, c);
    if (v != ConfirmDemandOrderResult.SUCCESS)
      return ConfirmDemandOrderOutcome.validationFailure(v);
    Object template;
    try {
      template = c.materializer.restore(preview);
    } catch (RuntimeException e) {
      report(
          c,
          m.tradeId(),
          preview.sellerId(),
          "materialize",
          ConfirmDemandOrderResult.ITEM_RESTORE_FAILED,
          null,
          null,
          e);
      return ConfirmDemandOrderOutcome.validationFailure(
          ConfirmDemandOrderResult.ITEM_RESTORE_FAILED);
    }
    if (template == null)
      return ConfirmDemandOrderOutcome.validationFailure(
          ConfirmDemandOrderResult.ITEM_RESTORE_FAILED);
    TransactionalInventory receiver;
    try {
      Optional<TransactionalInventory> resolved = c.receivers.resolve(preview.sellerId());
      if (resolved == null) {
        report(
            c,
            m.tradeId(),
            preview.sellerId(),
            "receiver",
            ConfirmDemandOrderResult.RECEIVER_RESOLUTION_FAILED,
            null,
            null,
            null);
        return ConfirmDemandOrderOutcome.validationFailure(
            ConfirmDemandOrderResult.RECEIVER_RESOLUTION_FAILED);
      }
      receiver = resolved.orElse(null);
    } catch (RuntimeException e) {
      report(
          c,
          m.tradeId(),
          preview.sellerId(),
          "receiver",
          ConfirmDemandOrderResult.RECEIVER_RESOLUTION_FAILED,
          null,
          null,
          e);
      return ConfirmDemandOrderOutcome.validationFailure(
          ConfirmDemandOrderResult.RECEIVER_RESOLUTION_FAILED);
    }
    if (receiver == null)
      return ConfirmDemandOrderOutcome.validationFailure(ConfirmDemandOrderResult.OWNER_OFFLINE);
    try {
      if (!preview.sellerId().equals(receiver.ownerId()))
        return ConfirmDemandOrderOutcome.validationFailure(
            ConfirmDemandOrderResult.INVALID_CONTEXT);
    } catch (RuntimeException e) {
      report(
          c,
          m.tradeId(),
          preview.sellerId(),
          "receiver-owner",
          ConfirmDemandOrderResult.RECEIVER_RESOLUTION_FAILED,
          null,
          null,
          e);
      return ConfirmDemandOrderOutcome.validationFailure(
          ConfirmDemandOrderResult.RECEIVER_RESOLUTION_FAILED);
    }
    try {
      if (!receiver.canAccept(template, preview.quantity()))
        return ConfirmDemandOrderOutcome.validationFailure(ConfirmDemandOrderResult.INVENTORY_FULL);
    } catch (RuntimeException e) {
      report(
          c,
          m.tradeId(),
          preview.sellerId(),
          "capacity",
          ConfirmDemandOrderResult.INVENTORY_MUTATION_FAILED,
          null,
          null,
          e);
      return ConfirmDemandOrderOutcome.validationFailure(
          ConfirmDemandOrderResult.INVENTORY_MUTATION_FAILED);
    }
    DeliveredDemandRemovalResult rr;
    try {
      rr = c.repository.removeDeliveredDemandTransactional(m.tradeId());
    } catch (RuntimeException e) {
      report(
          c,
          m.tradeId(),
          preview.sellerId(),
          "remove",
          ConfirmDemandOrderResult.ORDER_REMOVE_FAILED,
          null,
          null,
          e);
      return ConfirmDemandOrderOutcome.validationFailure(
          ConfirmDemandOrderResult.ORDER_REMOVE_FAILED);
    }
    if (rr == null)
      return ConfirmDemandOrderOutcome.uncertainFailure(
          ConfirmDemandOrderResult.ORDER_REMOVE_FAILED);
    if (rr.status() != DeliveredDemandRemovalStatus.REMOVED)
      return ConfirmDemandOrderOutcome.validationFailure(map(rr.status()));
    MarketOrderRemoval removal = rr.removal();
    MarketOrder authoritative = removal.order();
    if (!preview.equals(authoritative)) {
      boolean order = restore(removal);
      return order
          ? ConfirmDemandOrderOutcome.rolledBackFailure(
              ConfirmDemandOrderResult.ORDER_CHANGED, authoritative)
          : ConfirmDemandOrderOutcome.changedFailure(
              ConfirmDemandOrderResult.ROLLBACK_FAILED, authoritative);
    }
    InventoryInsertionResult insertion;
    try {
      insertion = receiver.insert(template, authoritative.quantity());
    } catch (RuntimeException e) {
      boolean order = restore(removal);
      report(
          c,
          m.tradeId(),
          authoritative.sellerId(),
          "insert",
          order
              ? ConfirmDemandOrderResult.INVENTORY_MUTATION_FAILED
              : ConfirmDemandOrderResult.ROLLBACK_FAILED,
          false,
          order,
          e);
      return order
          ? ConfirmDemandOrderOutcome.rolledBackFailure(
              ConfirmDemandOrderResult.INVENTORY_MUTATION_FAILED, authoritative)
          : ConfirmDemandOrderOutcome.changedFailure(
              ConfirmDemandOrderResult.ROLLBACK_FAILED, authoritative);
    }
    if (insertion == null || !insertion.succeeded()) {
      boolean inventory = insertion != null && insertion.failureRestored();
      boolean order = restore(removal);
      ConfirmDemandOrderResult result =
          inventory && order
              ? ConfirmDemandOrderResult.INVENTORY_MUTATION_FAILED
              : ConfirmDemandOrderResult.ROLLBACK_FAILED;
      report(c, m.tradeId(), authoritative.sellerId(), "insert", result, inventory, order, null);
      return order
          ? ConfirmDemandOrderOutcome.rolledBackFailure(result, authoritative)
          : ConfirmDemandOrderOutcome.changedFailure(result, authoritative);
    }
    return ConfirmDemandOrderOutcome.success(authoritative);
  }

  private static ConfirmDemandOrderResult validate(MarketOrder o, Context c) {
    if (o == null) return ConfirmDemandOrderResult.NOT_FOUND;
    if (o.type() != MarketOrderType.DEMAND) return ConfirmDemandOrderResult.WRONG_ORDER_TYPE;
    if (!o.delivered()) return ConfirmDemandOrderResult.NOT_DELIVERED;
    if (!c.actorId.equals(o.sellerId()) && !c.operator) return ConfirmDemandOrderResult.NOT_OWNER;
    if (o.totalPrice() <= 0) return ConfirmDemandOrderResult.INVALID_PRICE;
    if (o.quantity() <= 0) return ConfirmDemandOrderResult.INVALID_QUANTITY;
    if (o.item().count() != 1 || !ItemStackSnapshotValidator.validate(o.item()).isSuccess())
      return ConfirmDemandOrderResult.INVALID_SNAPSHOT;
    return ConfirmDemandOrderResult.SUCCESS;
  }

  private static ConfirmDemandOrderResult map(DeliveredDemandRemovalStatus s) {
    return switch (s) {
      case NOT_FOUND -> ConfirmDemandOrderResult.NOT_FOUND;
      case WRONG_ORDER_TYPE -> ConfirmDemandOrderResult.WRONG_ORDER_TYPE;
      case NOT_DELIVERED -> ConfirmDemandOrderResult.NOT_DELIVERED;
      default -> ConfirmDemandOrderResult.ORDER_REMOVE_FAILED;
    };
  }

  private static boolean restore(MarketOrderRemoval r) {
    try {
      return r.restore().restore() == MarketOrderRestoreResult.RESTORED;
    } catch (RuntimeException e) {
      return false;
    }
  }

  private static void report(
      Context c,
      UUID t,
      UUID o,
      String s,
      ConfirmDemandOrderResult r,
      Boolean i,
      Boolean d,
      RuntimeException e) {
    try {
      c.reporter.report(t, c.actorId, o, c.operator, s, r, i, d, e);
    } catch (RuntimeException ignored) {
    }
  }

  public record Context(
      UUID actorId,
      boolean operator,
      MarketItemMaterializer materializer,
      TransactionalInventoryResolver receivers,
      Repository repository,
      FailureReporter reporter) {
    public Context {
      Objects.requireNonNull(actorId);
      Objects.requireNonNull(materializer);
      Objects.requireNonNull(receivers);
      Objects.requireNonNull(repository);
      Objects.requireNonNull(reporter);
    }
  }

  public interface Repository {
    MarketOrder find(UUID id);

    DeliveredDemandRemovalResult removeDeliveredDemandTransactional(UUID id);
  }

  public interface FailureReporter {
    void report(
        UUID tradeId,
        UUID actorId,
        UUID ownerId,
        boolean operator,
        String stage,
        ConfirmDemandOrderResult result,
        Boolean inventoryRestore,
        Boolean orderRestore,
        RuntimeException exception);

    static FailureReporter noop() {
      return (a, b, c, d, e, f, g, h, i) -> {};
    }
  }
}

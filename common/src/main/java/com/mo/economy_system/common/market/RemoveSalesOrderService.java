package com.mo.economy_system.common.market;

import com.mo.economy_system.common.network.RemoveSalesOrderMessage;
import com.mo.economy_system.platform.item.ItemStackSnapshotValidator;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class RemoveSalesOrderService {
  private RemoveSalesOrderService() {}

  public static RemoveSalesOrderOutcome execute(RemoveSalesOrderMessage message, Context context) {
    if (message == null || context == null)
      return RemoveSalesOrderOutcome.validationFailure(RemoveSalesOrderResult.INVALID_CONTEXT);
    MarketOrder preview;
    try {
      preview = context.repository.find(message.tradeId());
    } catch (RuntimeException ex) {
      report(
          context,
          message.tradeId(),
          null,
          "find",
          RemoveSalesOrderResult.ORDER_REMOVE_FAILED,
          null,
          null,
          ex);
      return RemoveSalesOrderOutcome.validationFailure(RemoveSalesOrderResult.ORDER_REMOVE_FAILED);
    }
    RemoveSalesOrderResult invalid = validate(preview, context);
    if (invalid != RemoveSalesOrderResult.SUCCESS)
      return RemoveSalesOrderOutcome.validationFailure(invalid);
    Object template;
    try {
      template = context.materializer.restore(preview);
    } catch (RuntimeException ex) {
      report(
          context,
          message.tradeId(),
          preview.sellerId(),
          "materialize",
          RemoveSalesOrderResult.ITEM_RESTORE_FAILED,
          null,
          null,
          ex);
      return RemoveSalesOrderOutcome.validationFailure(RemoveSalesOrderResult.ITEM_RESTORE_FAILED);
    }
    if (template == null)
      return RemoveSalesOrderOutcome.validationFailure(RemoveSalesOrderResult.ITEM_RESTORE_FAILED);
    if (context.operator && !context.actorId.equals(preview.sellerId())) {
      return removeToMailbox(message, context, preview, template);
    }
    TransactionalInventory receiver;
    try {
      Optional<TransactionalInventory> resolved = context.receivers.resolve(preview.sellerId());
      if (resolved == null) {
        report(
            context,
            message.tradeId(),
            preview.sellerId(),
            "receiver",
            RemoveSalesOrderResult.RECEIVER_RESOLUTION_FAILED,
            null,
            null,
            null);
        return RemoveSalesOrderOutcome.validationFailure(
            RemoveSalesOrderResult.RECEIVER_RESOLUTION_FAILED);
      }
      receiver = resolved.orElse(null);
    } catch (RuntimeException ex) {
      report(
          context,
          message.tradeId(),
          preview.sellerId(),
          "receiver",
          RemoveSalesOrderResult.RECEIVER_RESOLUTION_FAILED,
          null,
          null,
          ex);
      return RemoveSalesOrderOutcome.validationFailure(
          RemoveSalesOrderResult.RECEIVER_RESOLUTION_FAILED);
    }
    if (receiver == null)
      return RemoveSalesOrderOutcome.validationFailure(RemoveSalesOrderResult.OWNER_OFFLINE);
    try {
      if (!preview.sellerId().equals(receiver.ownerId()))
        return RemoveSalesOrderOutcome.validationFailure(RemoveSalesOrderResult.INVALID_CONTEXT);
    } catch (RuntimeException ex) {
      report(
          context,
          message.tradeId(),
          preview.sellerId(),
          "receiver-owner",
          RemoveSalesOrderResult.RECEIVER_RESOLUTION_FAILED,
          null,
          null,
          ex);
      return RemoveSalesOrderOutcome.validationFailure(
          RemoveSalesOrderResult.RECEIVER_RESOLUTION_FAILED);
    }
    try {
      if (!receiver.canAccept(template, preview.quantity()))
        return RemoveSalesOrderOutcome.validationFailure(RemoveSalesOrderResult.INVENTORY_FULL);
    } catch (RuntimeException ex) {
      report(
          context,
          message.tradeId(),
          preview.sellerId(),
          "capacity",
          RemoveSalesOrderResult.INVENTORY_MUTATION_FAILED,
          null,
          null,
          ex);
      return RemoveSalesOrderOutcome.validationFailure(
          RemoveSalesOrderResult.INVENTORY_MUTATION_FAILED);
    }
    SalesOrderRemovalResult removed;
    try {
      removed = context.repository.removeSalesTransactional(message.tradeId());
    } catch (RuntimeException ex) {
      report(
          context,
          message.tradeId(),
          preview.sellerId(),
          "remove",
          RemoveSalesOrderResult.ORDER_REMOVE_FAILED,
          null,
          null,
          ex);
      return RemoveSalesOrderOutcome.validationFailure(RemoveSalesOrderResult.ORDER_REMOVE_FAILED);
    }
    if (removed == null)
      return RemoveSalesOrderOutcome.uncertainFailure(RemoveSalesOrderResult.ORDER_REMOVE_FAILED);
    if (removed.status() != SalesOrderRemovalStatus.REMOVED)
      return RemoveSalesOrderOutcome.validationFailure(map(removed.status()));
    MarketOrderRemoval removal = removed.removal();
    MarketOrder authoritative = removal.order();
    if (!preview.equals(authoritative)) {
      boolean order = restore(removal);
      return order
          ? RemoveSalesOrderOutcome.rolledBackFailure(
              RemoveSalesOrderResult.ORDER_CHANGED, authoritative)
          : RemoveSalesOrderOutcome.changedFailure(
              RemoveSalesOrderResult.ROLLBACK_FAILED, authoritative);
    }
    InventoryInsertionResult insertion;
    try {
      insertion = receiver.insert(template, authoritative.quantity());
    } catch (RuntimeException ex) {
      boolean order = restore(removal);
      report(
          context,
          message.tradeId(),
          authoritative.sellerId(),
          "insert",
          order
              ? RemoveSalesOrderResult.INVENTORY_MUTATION_FAILED
              : RemoveSalesOrderResult.ROLLBACK_FAILED,
          false,
          order,
          ex);
      return order
          ? RemoveSalesOrderOutcome.rolledBackFailure(
              RemoveSalesOrderResult.INVENTORY_MUTATION_FAILED, authoritative)
          : RemoveSalesOrderOutcome.changedFailure(
              RemoveSalesOrderResult.ROLLBACK_FAILED, authoritative);
    }
    if (insertion == null || !insertion.succeeded()) {
      boolean inventory = insertion != null && insertion.failureRestored();
      boolean order = restore(removal);
      RemoveSalesOrderResult result =
          inventory && order
              ? RemoveSalesOrderResult.INVENTORY_MUTATION_FAILED
              : RemoveSalesOrderResult.ROLLBACK_FAILED;
      report(
          context,
          message.tradeId(),
          authoritative.sellerId(),
          "insert",
          result,
          inventory,
          order,
          null);
      return order
          ? RemoveSalesOrderOutcome.rolledBackFailure(result, authoritative)
          : RemoveSalesOrderOutcome.changedFailure(result, authoritative);
    }
    return RemoveSalesOrderOutcome.success(authoritative);
  }

  private static RemoveSalesOrderOutcome removeToMailbox(
      RemoveSalesOrderMessage message, Context context, MarketOrder preview, Object template) {
    DemandMailboxResult preflight;
    try {
      preflight = context.mailbox.preflight(preview.sellerId(), template, preview.quantity());
    } catch (RuntimeException ex) {
      report(
          context,
          message.tradeId(),
          preview.sellerId(),
          "mailbox-preflight",
          RemoveSalesOrderResult.MAILBOX_DELIVERY_FAILED,
          null,
          null,
          ex);
      return RemoveSalesOrderOutcome.validationFailure(
          RemoveSalesOrderResult.MAILBOX_DELIVERY_FAILED);
    }
    RemoveSalesOrderResult preflightFailure = mailboxFailure(preflight);
    if (preflightFailure != null) {
      return RemoveSalesOrderOutcome.validationFailure(preflightFailure);
    }

    SalesOrderRemovalResult removed;
    try {
      removed = context.repository.removeSalesTransactional(message.tradeId());
    } catch (RuntimeException ex) {
      report(
          context,
          message.tradeId(),
          preview.sellerId(),
          "remove",
          RemoveSalesOrderResult.ORDER_REMOVE_FAILED,
          null,
          null,
          ex);
      return RemoveSalesOrderOutcome.validationFailure(RemoveSalesOrderResult.ORDER_REMOVE_FAILED);
    }
    if (removed == null)
      return RemoveSalesOrderOutcome.uncertainFailure(RemoveSalesOrderResult.ORDER_REMOVE_FAILED);
    if (removed.status() != SalesOrderRemovalStatus.REMOVED)
      return RemoveSalesOrderOutcome.validationFailure(map(removed.status()));

    MarketOrderRemoval removal = removed.removal();
    MarketOrder authoritative = removal.order();
    if (!preview.equals(authoritative)) {
      boolean order = restore(removal);
      return order
          ? RemoveSalesOrderOutcome.rolledBackFailure(
              RemoveSalesOrderResult.ORDER_CHANGED, authoritative)
          : RemoveSalesOrderOutcome.changedFailure(
              RemoveSalesOrderResult.ROLLBACK_FAILED, authoritative);
    }

    DemandMailboxResult delivery;
    RuntimeException deliveryError = null;
    try {
      delivery = context.mailbox.deliver(
          authoritative.sellerId(), authoritative, template, authoritative.quantity());
    } catch (RuntimeException ex) {
      delivery = DemandMailboxResult.UNKNOWN;
      deliveryError = ex;
    }
    if (delivery == DemandMailboxResult.SUCCESS) {
      return RemoveSalesOrderOutcome.success(authoritative);
    }
    if (delivery == DemandMailboxResult.UNKNOWN || delivery == null) {
      report(
          context,
          message.tradeId(),
          authoritative.sellerId(),
          "mailbox-delivery",
          RemoveSalesOrderResult.MAILBOX_STATE_UNKNOWN,
          null,
          false,
          deliveryError);
      return RemoveSalesOrderOutcome.uncertainFailure(
          RemoveSalesOrderResult.MAILBOX_STATE_UNKNOWN);
    }

    boolean orderRestored = restore(removal);
    RemoveSalesOrderResult result = mailboxFailure(delivery);
    report(
        context,
        message.tradeId(),
        authoritative.sellerId(),
        "mailbox-delivery",
        orderRestored ? result : RemoveSalesOrderResult.ROLLBACK_FAILED,
        null,
        orderRestored,
        deliveryError);
    return orderRestored
        ? RemoveSalesOrderOutcome.rolledBackFailure(result, authoritative)
        : RemoveSalesOrderOutcome.changedFailure(
            RemoveSalesOrderResult.ROLLBACK_FAILED, authoritative);
  }

  private static RemoveSalesOrderResult mailboxFailure(DemandMailboxResult result) {
    if (result == null) return RemoveSalesOrderResult.MAILBOX_STATE_UNKNOWN;
    return switch (result) {
      case SUCCESS -> null;
      case FULL -> RemoveSalesOrderResult.MAILBOX_FULL;
      case FAILED -> RemoveSalesOrderResult.MAILBOX_DELIVERY_FAILED;
      case UNKNOWN -> RemoveSalesOrderResult.MAILBOX_STATE_UNKNOWN;
    };
  }

  private static RemoveSalesOrderResult validate(MarketOrder o, Context c) {
    if (o == null) return RemoveSalesOrderResult.NOT_FOUND;
    if (o.type() != MarketOrderType.SALES) return RemoveSalesOrderResult.WRONG_ORDER_TYPE;
    if (!c.actorId.equals(o.sellerId()) && !c.operator) return RemoveSalesOrderResult.NOT_OWNER;
    if (o.totalPrice() <= 0) return RemoveSalesOrderResult.INVALID_PRICE;
    if (o.quantity() <= 0) return RemoveSalesOrderResult.INVALID_QUANTITY;
    if (o.item().count() != 1 || !ItemStackSnapshotValidator.validate(o.item()).isSuccess())
      return RemoveSalesOrderResult.INVALID_SNAPSHOT;
    return RemoveSalesOrderResult.SUCCESS;
  }

  private static RemoveSalesOrderResult map(SalesOrderRemovalStatus s) {
    return s == SalesOrderRemovalStatus.NOT_FOUND
        ? RemoveSalesOrderResult.NOT_FOUND
        : s == SalesOrderRemovalStatus.WRONG_ORDER_TYPE
            ? RemoveSalesOrderResult.WRONG_ORDER_TYPE
            : RemoveSalesOrderResult.ORDER_REMOVE_FAILED;
  }

  private static boolean restore(MarketOrderRemoval r) {
    try {
      return r.restore().restore() == MarketOrderRestoreResult.RESTORED;
    } catch (RuntimeException ex) {
      return false;
    }
  }

  private static void report(
      Context c,
      UUID trade,
      UUID owner,
      String stage,
      RemoveSalesOrderResult result,
      Boolean inventory,
      Boolean order,
      RuntimeException ex) {
    try {
      c.reporter.report(trade, c.actorId, owner, c.operator, stage, result, inventory, order, ex);
    } catch (RuntimeException ignored) {
    }
  }

  public record Context(
      UUID actorId,
      boolean operator,
      MarketItemMaterializer materializer,
      TransactionalInventoryResolver receivers,
      Repository repository,
      Mailbox mailbox,
      FailureReporter reporter) {
    public Context {
      Objects.requireNonNull(actorId);
      Objects.requireNonNull(materializer);
      Objects.requireNonNull(receivers);
      Objects.requireNonNull(repository);
      Objects.requireNonNull(mailbox);
      Objects.requireNonNull(reporter);
    }
  }

  public interface Mailbox {
    DemandMailboxResult preflight(UUID ownerId, Object template, int quantity);

    /**
     * Delivers the removed order remainder to the owner's mailbox. FAILED/FULL guarantee that no
     * mailbox mutation survived. UNKNOWN must not be followed by an order restore because an
     * attachment may already have persisted.
     */
    DemandMailboxResult deliver(UUID ownerId, MarketOrder order, Object template, int quantity);
  }

  public interface Repository {
    MarketOrder find(UUID tradeId);

    SalesOrderRemovalResult removeSalesTransactional(UUID tradeId);
  }

  public interface FailureReporter {
    void report(
        UUID tradeId,
        UUID actorId,
        UUID ownerId,
        boolean operator,
        String stage,
        RemoveSalesOrderResult result,
        Boolean inventoryRestore,
        Boolean orderRestore,
        RuntimeException exception);

    static FailureReporter noop() {
      return (a, b, c, d, e, f, g, h, i) -> {};
    }
  }
}

package com.mo.economy_system.common.market;

import com.mo.economy_system.common.network.RemoveDemandOrderMessage;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import java.util.Objects;
import java.util.UUID;

public final class CancelDemandOrderService {
  private CancelDemandOrderService() {}

  public static CancelDemandOrderOutcome execute(
      RemoveDemandOrderMessage message, Context context) {
    if (message == null || context == null)
      return CancelDemandOrderOutcome.validationFailure(CancelDemandOrderResult.INVALID_CONTEXT);

    UUID tradeId = message.tradeId();
    MarketOrder expected;
    try {
      expected = context.repository().find(tradeId);
    } catch (RuntimeException error) {
      report(context, failure(context, tradeId, null, "find",
          CancelDemandOrderResult.STATE_UNKNOWN, MarketMutationState.UNKNOWN,
          null, null, null, error, error, null, null));
      return CancelDemandOrderOutcome.uncertainFailure(CancelDemandOrderResult.STATE_UNKNOWN);
    }
    if (expected == null)
      return validationFailure(context, tradeId, null, CancelDemandOrderResult.NOT_FOUND);

    UUID requesterId = expected.sellerId();
    CancelDemandOrderResult validation = validate(expected, context);
    if (validation != CancelDemandOrderResult.SUCCESS)
      return validationFailure(context, tradeId, requesterId, validation);

    BalanceMutationResult preview;
    try {
      preview = context.account().previewCreditExact(requesterId, expected.totalPrice());
    } catch (RuntimeException error) {
      report(context, failure(context, tradeId, requesterId, "refund-preview",
          CancelDemandOrderResult.REFUND_FAILED, MarketMutationState.UNCHANGED,
          null, null, null, error, null, error, null));
      return CancelDemandOrderOutcome.validationFailure(CancelDemandOrderResult.REFUND_FAILED);
    }
    if (preview != BalanceMutationResult.SUCCESS) {
      CancelDemandOrderResult result = preview == BalanceMutationResult.BALANCE_LIMIT
          ? CancelDemandOrderResult.OWNER_BALANCE_LIMIT
          : CancelDemandOrderResult.REFUND_FAILED;
      report(context, failure(context, tradeId, requesterId, "refund-preview", result,
          MarketMutationState.UNCHANGED, null, preview, null, null, null, null, null));
      return CancelDemandOrderOutcome.validationFailure(result);
    }

    DemandOrderRemovalResult removalResult;
    try {
      removalResult = context.repository().removeUndeliveredDemandIfUnchanged(tradeId, expected);
    } catch (RuntimeException error) {
      report(context, failure(context, tradeId, requesterId, "order-remove",
          CancelDemandOrderResult.STATE_UNKNOWN, MarketMutationState.UNKNOWN,
          null, null, null, error, error, null, null));
      return CancelDemandOrderOutcome.uncertainFailure(CancelDemandOrderResult.STATE_UNKNOWN);
    }
    if (removalResult == null) {
      report(context, failure(context, tradeId, requesterId, "order-remove",
          CancelDemandOrderResult.STATE_UNKNOWN, MarketMutationState.UNKNOWN,
          null, null, null, null, null, null, null));
      return CancelDemandOrderOutcome.uncertainFailure(CancelDemandOrderResult.STATE_UNKNOWN);
    }
    if (removalResult.status() != DemandOrderRemovalStatus.REMOVED)
      return removalFailure(context, tradeId, requesterId, expected, removalResult.status());

    MarketOrderRemoval removal = removalResult.removal();
    MarketOrder transactionOrder = removal.order();
    if (!expected.equals(transactionOrder)) {
      // A repository contract violation may already have removed an unexpected order. Restore first.
      return compensateContractViolation(
          context, tradeId, requesterId, transactionOrder, removal);
    }

    BalanceMutationResult refundResult;
    RuntimeException refundError = null;
    try {
      refundResult = context.account().creditExact(
          transactionOrder.sellerId(), transactionOrder.totalPrice());
    } catch (RuntimeException error) {
      refundResult = null;
      refundError = error;
    }
    if (refundResult == BalanceMutationResult.SUCCESS)
      return CancelDemandOrderOutcome.success(transactionOrder);

    return restoreAfterRefundFailure(
        context, tradeId, requesterId, transactionOrder, removal,
        refundResult, refundError);
  }

  private static CancelDemandOrderOutcome removalFailure(
      Context context, UUID tradeId, UUID requesterId, MarketOrder expected,
      DemandOrderRemovalStatus status) {
    CancelDemandOrderResult result = mapRemoval(status);
    // ORDER_CHANGED means the authoritative market differs from the preview and clients are stale.
    MarketMutationState state = status == DemandOrderRemovalStatus.ORDER_CHANGED
        ? MarketMutationState.CHANGED : MarketMutationState.UNCHANGED;
    report(context, failure(context, tradeId, requesterId, "order-remove", result, state,
        status, null, null, null, null, null, null));
    return state == MarketMutationState.CHANGED
        ? CancelDemandOrderOutcome.changedFailure(result, null)
        : CancelDemandOrderOutcome.validationFailure(result);
  }

  private static CancelDemandOrderOutcome compensateContractViolation(
      Context context, UUID tradeId, UUID requesterId, MarketOrder transactionOrder,
      MarketOrderRemoval removal) {
    RestoreAttempt restore = restore(removal);
    if (restore.result() == MarketOrderRestoreResult.RESTORED) {
      report(context, failure(context, tradeId, requesterId, "repository-contract",
          CancelDemandOrderResult.ORDER_CHANGED, MarketMutationState.CHANGED,
          DemandOrderRemovalStatus.REMOVED, null, restore.result(), null, null, null, null));
      return CancelDemandOrderOutcome.changedFailure(
          CancelDemandOrderResult.ORDER_CHANGED, transactionOrder);
    }
    MarketMutationState state = restore.error() == null
        ? MarketMutationState.CHANGED : MarketMutationState.UNKNOWN;
    report(context, failure(context, tradeId, requesterId, "repository-contract",
        CancelDemandOrderResult.ROLLBACK_FAILED, state, DemandOrderRemovalStatus.REMOVED,
        null, restore.result(), restore.error(), null, null, restore.error()));
    return state == MarketMutationState.CHANGED
        ? CancelDemandOrderOutcome.changedFailure(
            CancelDemandOrderResult.ROLLBACK_FAILED, transactionOrder)
        : CancelDemandOrderOutcome.uncertainFailure(CancelDemandOrderResult.ROLLBACK_FAILED);
  }

  private static CancelDemandOrderOutcome restoreAfterRefundFailure(
      Context context, UUID tradeId, UUID requesterId, MarketOrder transactionOrder,
      MarketOrderRemoval removal, BalanceMutationResult refundResult,
      RuntimeException refundError) {
    RestoreAttempt restore = restore(removal);
    if (restore.result() == MarketOrderRestoreResult.RESTORED) {
      CancelDemandOrderResult result = refundResult == BalanceMutationResult.BALANCE_LIMIT
          ? CancelDemandOrderResult.OWNER_BALANCE_LIMIT
          : CancelDemandOrderResult.REFUND_FAILED;
      report(context, failure(context, tradeId, requesterId, "order-restore", result,
          MarketMutationState.UNCHANGED, DemandOrderRemovalStatus.REMOVED,
          refundResult, restore.result(), refundError, null, refundError, null));
      return CancelDemandOrderOutcome.rolledBackFailure(result, transactionOrder);
    }
    MarketMutationState state = restore.error() == null && restore.result() != null
        ? MarketMutationState.CHANGED : MarketMutationState.UNKNOWN;
    RuntimeException primary = restore.error() != null ? restore.error() : refundError;
    report(context, failure(context, tradeId, requesterId, "order-restore",
        CancelDemandOrderResult.ROLLBACK_FAILED, state, DemandOrderRemovalStatus.REMOVED,
        refundResult, restore.result(), primary, null, refundError, restore.error()));
    return state == MarketMutationState.CHANGED
        ? CancelDemandOrderOutcome.changedFailure(
            CancelDemandOrderResult.ROLLBACK_FAILED, transactionOrder)
        : CancelDemandOrderOutcome.uncertainFailure(CancelDemandOrderResult.ROLLBACK_FAILED);
  }

  private static RestoreAttempt restore(MarketOrderRemoval removal) {
    try {
      return new RestoreAttempt(removal.restore().restore(), null);
    } catch (RuntimeException error) {
      return new RestoreAttempt(null, error);
    }
  }

  private static CancelDemandOrderResult validate(MarketOrder order, Context context) {
    if (order.type() != MarketOrderType.DEMAND) return CancelDemandOrderResult.WRONG_ORDER_TYPE;
    if (order.delivered()) return CancelDemandOrderResult.ALREADY_DELIVERED;
    if (!context.operator() && !order.sellerId().equals(context.actorId()))
      return CancelDemandOrderResult.NOT_OWNER;
    if (order.totalPrice() <= 0) return CancelDemandOrderResult.INVALID_PRICE;
    return CancelDemandOrderResult.SUCCESS;
  }

  private static CancelDemandOrderResult mapRemoval(DemandOrderRemovalStatus status) {
    return switch (status) {
      case NOT_FOUND -> CancelDemandOrderResult.NOT_FOUND;
      case WRONG_ORDER_TYPE -> CancelDemandOrderResult.WRONG_ORDER_TYPE;
      case ALREADY_DELIVERED -> CancelDemandOrderResult.ALREADY_DELIVERED;
      case ORDER_CHANGED -> CancelDemandOrderResult.ORDER_CHANGED;
      case PERSIST_FAILED -> CancelDemandOrderResult.ORDER_REMOVE_FAILED;
      case REMOVED -> throw new IllegalArgumentException("REMOVED is not a failure");
    };
  }

  private static CancelDemandOrderOutcome validationFailure(
      Context context, UUID tradeId, UUID requesterId, CancelDemandOrderResult result) {
    report(context, failure(context, tradeId, requesterId, "validation", result,
        MarketMutationState.UNCHANGED, null, null, null, null, null, null, null));
    return CancelDemandOrderOutcome.validationFailure(result);
  }

  private static CancelDemandOrderFailure failure(
      Context context, UUID tradeId, UUID requesterId, String stage,
      CancelDemandOrderResult result, MarketMutationState state,
      DemandOrderRemovalStatus removalStatus, BalanceMutationResult refundResult,
      MarketOrderRestoreResult restoreResult, RuntimeException primaryError,
      RuntimeException repositoryError, RuntimeException refundError,
      RuntimeException restoreError) {
    return new CancelDemandOrderFailure(
        tradeId, context.actorId(), requesterId, context.operator(), stage, result, state,
        removalStatus, refundResult, restoreResult, primaryError, repositoryError,
        refundError, restoreError);
  }

  private static void report(Context context, CancelDemandOrderFailure failure) {
    try { context.reporter().report(failure); }
    catch (RuntimeException ignored) { /* Reporting cannot alter the transaction outcome. */ }
  }

  private record RestoreAttempt(MarketOrderRestoreResult result, RuntimeException error) {}

  public record Context(
      UUID actorId, boolean operator, Account account, Repository repository,
      FailureReporter reporter) {
    public Context {
      Objects.requireNonNull(actorId, "actorId");
      Objects.requireNonNull(account, "account");
      Objects.requireNonNull(repository, "repository");
      Objects.requireNonNull(reporter, "reporter");
    }
  }

  public interface Account {
    BalanceMutationResult previewCreditExact(UUID owner, int amount);
    BalanceMutationResult creditExact(UUID owner, int amount);
  }

  public interface Repository {
    MarketOrder find(UUID id);
    DemandOrderRemovalResult removeUndeliveredDemandIfUnchanged(UUID id, MarketOrder expected);
  }

  public interface FailureReporter {
    void report(CancelDemandOrderFailure failure);
    static FailureReporter noop() { return failure -> {}; }
  }
}

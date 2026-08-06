package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import java.util.Objects;
import java.util.UUID;

/** Forge adapter for the NeoForge 1.21.1 authoritative resize payment semantics. */
final class Forge1201TerritoryResizeTransaction {
  enum Result {
    SUCCESS,
    UNCHANGED,
    INSUFFICIENT_FUNDS,
    NOT_FOUND,
    NOT_OWNER,
    WRONG_DIMENSION,
    INVALID_BOUNDS,
    OVERLAP,
    CHANGED,
    PERSIST_FAILED,
    STATE_UNKNOWN,
    REFUND_FAILED,
    PAYMENT_FAILED
  }

  record Outcome(Result result, Throwable failure) {
    Outcome {
      Objects.requireNonNull(result, "result");
      if (failure instanceof Error error) throw error;
    }
  }

  @FunctionalInterface
  interface Diagnostics {
    void warning(String stage, UUID playerId, UUID territoryId, Throwable failure);
  }

  private Forge1201TerritoryResizeTransaction() {}

  static Outcome execute(
      EconomySavedData accounts,
      Forge1201TerritorySnapshotStore store,
      UUID playerId,
      UUID territoryId,
      String dimensionId,
      Position first,
      Position second,
      Diagnostics diagnostics) {
    Objects.requireNonNull(accounts, "accounts");
    Objects.requireNonNull(store, "store");
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(territoryId, "territoryId");
    Objects.requireNonNull(diagnostics, "diagnostics");

    Forge1201TerritorySnapshotStore.ResizePrepareOutcome prepared;
    try {
      prepared = store.prepareResize(territoryId, playerId, dimensionId, first, second);
    } catch (RuntimeException failure) {
      warn(diagnostics, "prepare", playerId, territoryId, failure);
      return new Outcome(Result.STATE_UNKNOWN, failure);
    }
    if (prepared == null) {
      IllegalStateException failure = new IllegalStateException("null resize prepare outcome");
      warn(diagnostics, "repository-contract", playerId, territoryId, failure);
      return new Outcome(Result.STATE_UNKNOWN, failure);
    }
    if (prepared.result() != Forge1201TerritorySnapshotStore.ResizePrepareResult.READY) {
      if (prepared.failure() != null) {
        warn(diagnostics, "prepare", playerId, territoryId, prepared.failure());
      }
      return new Outcome(map(prepared.result()), prepared.failure());
    }

    Forge1201TerritorySnapshotStore.ResizePlan plan = prepared.plan();
    if (plan.charge() > 0) {
      final BalanceMutationResult debit;
      try {
        debit = accounts.debitExact(playerId, plan.charge(), "领地", "调整领地大小");
      } catch (RuntimeException failure) {
        warn(diagnostics, "payment-debit", playerId, territoryId, failure);
        return new Outcome(Result.PAYMENT_FAILED, failure);
      }
      if (debit != BalanceMutationResult.SUCCESS) {
        Result result = debit == BalanceMutationResult.INSUFFICIENT_FUNDS
            ? Result.INSUFFICIENT_FUNDS
            : Result.PAYMENT_FAILED;
        Throwable failure = result == Result.INSUFFICIENT_FUNDS
            ? null
            : new IllegalStateException("resize debit result: " + debit);
        if (failure != null) warn(diagnostics, "payment-debit", playerId, territoryId, failure);
        return new Outcome(result, failure);
      }
    }

    final Forge1201TerritorySnapshotStore.ResizeCommitResult committed;
    try {
      committed = store.commitResize(plan);
    } catch (RuntimeException failure) {
      warn(diagnostics, "mutation-state-unknown", playerId, territoryId, failure);
      return new Outcome(Result.STATE_UNKNOWN, failure);
    }
    if (committed == null) {
      IllegalStateException failure = new IllegalStateException("null resize commit result");
      warn(diagnostics, "repository-contract", playerId, territoryId, failure);
      return new Outcome(Result.STATE_UNKNOWN, failure);
    }
    Result result = map(committed);
    if (result == Result.SUCCESS) return new Outcome(result, null);
    if (result == Result.STATE_UNKNOWN) {
      warn(diagnostics, "mutation-state-unknown", playerId, territoryId, null);
      return new Outcome(result, null);
    }
    if (result == Result.PERSIST_FAILED) {
      warn(diagnostics, "mutation-persist-failed", playerId, territoryId, null);
    }
    if (plan.charge() == 0) return new Outcome(result, null);

    final BalanceMutationResult refund;
    try {
      refund = accounts.creditExact(playerId, plan.charge(), "领地", "领地调整失败退款");
    } catch (RuntimeException failure) {
      warn(diagnostics, "payment-refund", playerId, territoryId, failure);
      return new Outcome(Result.REFUND_FAILED, failure);
    }
    if (refund != BalanceMutationResult.SUCCESS) {
      IllegalStateException failure = new IllegalStateException("resize refund result: " + refund);
      warn(diagnostics, "payment-refund", playerId, territoryId, failure);
      return new Outcome(Result.REFUND_FAILED, failure);
    }
    return new Outcome(result, null);
  }

  private static Result map(Forge1201TerritorySnapshotStore.ResizePrepareResult result) {
    return switch (result) {
      case UNCHANGED -> Result.UNCHANGED;
      case NOT_FOUND -> Result.NOT_FOUND;
      case NOT_OWNER -> Result.NOT_OWNER;
      case WRONG_DIMENSION -> Result.WRONG_DIMENSION;
      case INVALID_BOUNDS -> Result.INVALID_BOUNDS;
      case OVERLAP -> Result.OVERLAP;
      case PRICE_OVERFLOW -> Result.PAYMENT_FAILED;
      case STATE_UNKNOWN -> Result.STATE_UNKNOWN;
      case READY -> throw new AssertionError();
    };
  }

  private static Result map(Forge1201TerritorySnapshotStore.ResizeCommitResult result) {
    return switch (result) {
      case SUCCESS -> Result.SUCCESS;
      case NOT_FOUND -> Result.NOT_FOUND;
      case CHANGED -> Result.CHANGED;
      case OVERLAP -> Result.OVERLAP;
      case PERSIST_FAILED -> Result.PERSIST_FAILED;
      case STATE_UNKNOWN -> Result.STATE_UNKNOWN;
    };
  }

  private static void warn(
      Diagnostics diagnostics,
      String stage,
      UUID playerId,
      UUID territoryId,
      Throwable failure) {
    if (failure == null) failure = new IllegalStateException(stage);
    try {
      diagnostics.warning(stage, playerId, territoryId, failure);
    } catch (RuntimeException ignored) {
    }
  }
}

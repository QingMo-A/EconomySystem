package com.mo.economy_system.common.territory;

import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import java.util.Objects;
import java.util.UUID;

/**
 * Loader-neutral authoritative territory-resize transaction.
 *
 * <p>Targets provide only balance and persistence ports. The debit, commit,
 * uncertainty, and refund policy is intentionally shared by every version.</p>
 */
public final class TerritoryResizeTransactionService {
  public enum Result {
    SUCCESS,
    UNCHANGED,
    INSUFFICIENT_FUNDS,
    TERRITORY_NOT_FOUND,
    NO_PERMISSION,
    WRONG_DIMENSION,
    INVALID_BOUNDS,
    OVERLAP,
    CHANGED,
    PERSIST_FAILED,
    STATE_UNKNOWN,
    REFUND_FAILED,
    PAYMENT_FAILED
  }

  public enum PrepareResult {
    READY,
    UNCHANGED,
    TERRITORY_NOT_FOUND,
    NO_PERMISSION,
    WRONG_DIMENSION,
    INVALID_BOUNDS,
    OVERLAP,
    PRICE_OVERFLOW,
    STATE_UNKNOWN
  }

  public record ResizePlan(int charge, Object token) {
    public ResizePlan {
      if (charge < 0) throw new IllegalArgumentException("charge");
      Objects.requireNonNull(token, "token");
    }
  }

  public record PrepareOutcome(PrepareResult result, ResizePlan plan, Throwable failure) {
    public PrepareOutcome {
      Objects.requireNonNull(result, "prepare result");
      if ((result == PrepareResult.READY) != (plan != null)) {
        throw new IllegalArgumentException("prepare result/plan");
      }
      if (failure instanceof Error error) throw error;
    }
  }

  public record Outcome(Result result, Throwable failure) {
    public Outcome {
      Objects.requireNonNull(result, "result");
      if (failure instanceof Error error) throw error;
    }
  }

  public interface BalancePort {
    BalanceMutationResult debitExact(UUID playerId, int amount);

    BalanceMutationResult creditExact(UUID playerId, int amount);
  }

  public interface ResizeRepository {
    PrepareOutcome prepare(UUID territoryId, UUID expectedOwnerId);

    Outcome commit(ResizePlan plan);
  }

  @FunctionalInterface
  public interface Diagnostics {
    void warning(String stage, UUID playerId, UUID territoryId, Throwable failure);
  }

  private TerritoryResizeTransactionService() {}

  public static Outcome execute(
      BalancePort balance,
      ResizeRepository repository,
      Diagnostics diagnostics,
      UUID playerId,
      UUID territoryId) {
    Objects.requireNonNull(balance, "balance");
    Objects.requireNonNull(repository, "repository");
    Objects.requireNonNull(diagnostics, "diagnostics");
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(territoryId, "territoryId");

    PrepareOutcome prepared;
    try {
      prepared = repository.prepare(territoryId, playerId);
    } catch (RuntimeException failure) {
      warn(diagnostics, "prepare", playerId, territoryId, failure);
      return new Outcome(Result.STATE_UNKNOWN, failure);
    }
    if (prepared == null) {
      IllegalStateException failure = new IllegalStateException("null resize prepare outcome");
      warn(diagnostics, "repository-contract", playerId, territoryId, failure);
      return new Outcome(Result.STATE_UNKNOWN, failure);
    }
    if (prepared.failure() != null) warn(diagnostics, "prepare", playerId, territoryId, prepared.failure());
    if (prepared.result() != PrepareResult.READY) return new Outcome(mapPrepare(prepared.result()), prepared.failure());

    ResizePlan plan = prepared.plan();
    if (plan.charge() > 0) {
      BalanceMutationResult debit;
      try {
        debit = balance.debitExact(playerId, plan.charge());
      } catch (RuntimeException failure) {
        warn(diagnostics, "payment-debit", playerId, territoryId, failure);
        // A throwing balance port may have debited before reporting the failure. The account
        // state is unknowable, so do not attempt a blind refund.
        return new Outcome(Result.STATE_UNKNOWN, failure);
      }
      if (debit == null) {
        IllegalStateException failure = new IllegalStateException("null resize debit result");
        warn(diagnostics, "payment-debit", playerId, territoryId, failure);
        return new Outcome(Result.STATE_UNKNOWN, failure);
      }
      if (debit != BalanceMutationResult.SUCCESS) {
        Throwable failure = debit == BalanceMutationResult.INSUFFICIENT_FUNDS
            ? null : new IllegalStateException("resize debit result: " + debit);
        if (failure != null) warn(diagnostics, "payment-debit", playerId, territoryId, failure);
        return new Outcome(
            debit == BalanceMutationResult.INSUFFICIENT_FUNDS
                ? Result.INSUFFICIENT_FUNDS : Result.PAYMENT_FAILED,
            failure);
      }
    }

    Outcome mutation;
    try {
      mutation = repository.commit(plan);
    } catch (RuntimeException failure) {
      warn(diagnostics, "mutation-state-unknown", playerId, territoryId, failure);
      return new Outcome(Result.STATE_UNKNOWN, failure);
    }
    if (mutation == null) {
      IllegalStateException failure = new IllegalStateException("null resize commit outcome");
      warn(diagnostics, "repository-contract", playerId, territoryId, failure);
      return new Outcome(Result.STATE_UNKNOWN, failure);
    }
    if (mutation.result() == Result.SUCCESS) return mutation;
    if (mutation.result() == Result.STATE_UNKNOWN) {
      warn(diagnostics, "mutation-state-unknown", playerId, territoryId, mutation.failure());
      return mutation;
    }
    if (mutation.result() == Result.PERSIST_FAILED) {
      warn(diagnostics, "mutation-persist-failed", playerId, territoryId, mutation.failure());
    }
    if (plan.charge() == 0) return mutation;

    BalanceMutationResult refund;
    try {
      refund = balance.creditExact(playerId, plan.charge());
    } catch (RuntimeException failure) {
      addSuppressed(failure, mutation.failure());
      warn(diagnostics, "payment-refund", playerId, territoryId, failure);
      return new Outcome(Result.REFUND_FAILED, failure);
    }
    if (refund != BalanceMutationResult.SUCCESS) {
      IllegalStateException failure = new IllegalStateException("resize refund result: " + refund);
      addSuppressed(failure, mutation.failure());
      warn(diagnostics, "payment-refund", playerId, territoryId, failure);
      return new Outcome(Result.REFUND_FAILED, failure);
    }
    return mutation;
  }

  private static Result mapPrepare(PrepareResult result) {
    return switch (result) {
      case UNCHANGED -> Result.UNCHANGED;
      case TERRITORY_NOT_FOUND -> Result.TERRITORY_NOT_FOUND;
      case NO_PERMISSION -> Result.NO_PERMISSION;
      case WRONG_DIMENSION -> Result.WRONG_DIMENSION;
      case INVALID_BOUNDS -> Result.INVALID_BOUNDS;
      case OVERLAP -> Result.OVERLAP;
      case PRICE_OVERFLOW -> Result.PAYMENT_FAILED;
      case STATE_UNKNOWN -> Result.STATE_UNKNOWN;
      case READY -> throw new AssertionError("ready must carry a plan");
    };
  }

  private static void warn(
      Diagnostics diagnostics, String stage, UUID playerId, UUID territoryId, Throwable failure) {
    if (failure == null) failure = new IllegalStateException(stage);
    try {
      diagnostics.warning(stage, playerId, territoryId, failure);
    } catch (RuntimeException ignored) {
      // Diagnostics must not alter the authoritative result.
    }
  }

  private static void addSuppressed(Throwable primary, Throwable secondary) {
    if (secondary != null && secondary != primary) primary.addSuppressed(secondary);
  }
}

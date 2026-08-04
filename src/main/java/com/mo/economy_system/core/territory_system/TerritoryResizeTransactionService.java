package com.mo.economy_system.core.territory_system;

import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;

public final class TerritoryResizeTransactionService {
  public enum Result {
    SUCCESS,
    UNCHANGED,
    INSUFFICIENT_FUNDS,
    TERRITORY_NOT_FOUND,
    NO_PERMISSION,
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
    INVALID_BOUNDS,
    OVERLAP,
    PRICE_OVERFLOW,
    STATE_UNKNOWN
  }

  public record ResizePlan(int charge, Object token) {
    public ResizePlan {
      if (charge < 0) throw new IllegalArgumentException("charge");
      Objects.requireNonNull(token);
    }
  }

  public record PrepareOutcome(PrepareResult result, ResizePlan plan, Throwable failure) {
    public PrepareOutcome {
      Objects.requireNonNull(result);
      if ((result == PrepareResult.READY) != (plan != null))
        throw new IllegalArgumentException("prepare result/plan");
      if (failure instanceof Error error) throw error;
    }
  }

  public record Outcome(Result result, Throwable failure) {
    public Outcome {
      Objects.requireNonNull(result);
      if (failure instanceof Error error) throw error;
    }
  }

  public interface BalancePort {
    BalanceMutationResult debitExact(UUID playerId, int amount);

    BalanceMutationResult creditExact(UUID playerId, int amount);
  }

  public interface ResizeRepository {
    PrepareOutcome prepare(
        UUID territoryId,
        UUID expectedOwnerId,
        BlockPos first,
        BlockPos second,
        BlockPos backpoint);

    Outcome commit(ResizePlan plan);
  }

  public interface Diagnostics {
    void warning(String stage, UUID playerId, UUID territoryId, Throwable error);
  }

  private TerritoryResizeTransactionService() {}

  public static Outcome execute(
      EconomySavedData economy,
      UUID playerId,
      UUID territoryId,
      BlockPos first,
      BlockPos second,
      Diagnostics diagnostics) {
    Objects.requireNonNull(economy);
    return execute(
        new BalancePort() {
          @Override
          public BalanceMutationResult debitExact(UUID id, int amount) {
            return economy.debitExact(id, amount, "领地", "调整领地大小");
          }

          @Override
          public BalanceMutationResult creditExact(UUID id, int amount) {
            return economy.creditExact(id, amount, "领地", "领地调整失败退款");
          }
        },
        managerRepository(),
        diagnostics,
        playerId,
        territoryId,
        first,
        second,
        first);
  }

  public static Outcome execute(
      BalancePort balance,
      ResizeRepository repository,
      Diagnostics diagnostics,
      UUID playerId,
      UUID territoryId,
      BlockPos first,
      BlockPos second,
      BlockPos backpoint) {
    Objects.requireNonNull(balance);
    Objects.requireNonNull(repository);
    Objects.requireNonNull(diagnostics);
    Objects.requireNonNull(playerId);
    Objects.requireNonNull(territoryId);
    PrepareOutcome prepared;
    try {
      prepared = repository.prepare(territoryId, playerId, first, second, backpoint);
    } catch (RuntimeException failure) {
      warn(diagnostics, "prepare", playerId, territoryId, failure);
      return new Outcome(Result.STATE_UNKNOWN, failure);
    }
    if (prepared == null) {
      IllegalStateException failure = new IllegalStateException("null resize prepare outcome");
      warn(diagnostics, "repository-contract", playerId, territoryId, failure);
      return new Outcome(Result.STATE_UNKNOWN, failure);
    }
    if (prepared.failure() != null)
      warn(diagnostics, "prepare", playerId, territoryId, prepared.failure());
    if (prepared.result() != PrepareResult.READY) return mapPrepare(prepared);

    ResizePlan plan = prepared.plan();
    if (plan.charge() > 0) {
      BalanceMutationResult debit;
      try {
        debit = balance.debitExact(playerId, plan.charge());
      } catch (RuntimeException failure) {
        warn(diagnostics, "payment-debit", playerId, territoryId, failure);
        return new Outcome(Result.PAYMENT_FAILED, failure);
      }
      if (debit != BalanceMutationResult.SUCCESS) {
        IllegalStateException failure = new IllegalStateException("resize debit result: " + debit);
        if (debit != BalanceMutationResult.INSUFFICIENT_FUNDS)
          warn(diagnostics, "payment-debit", playerId, territoryId, failure);
        return new Outcome(
            debit == BalanceMutationResult.INSUFFICIENT_FUNDS
                ? Result.INSUFFICIENT_FUNDS
                : Result.PAYMENT_FAILED,
            debit == BalanceMutationResult.INSUFFICIENT_FUNDS ? null : failure);
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
    if (mutation.result() == Result.PERSIST_FAILED)
      warn(diagnostics, "mutation-persist-failed", playerId, territoryId, mutation.failure());
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

  private static ResizeRepository managerRepository() {
    return new ResizeRepository() {
      @Override
      public PrepareOutcome prepare(
          UUID territoryId, UUID ownerId, BlockPos first, BlockPos second, BlockPos backpoint) {
        TerritoryManager.ResizePrepareOutcome outcome =
            TerritoryManager.prepareTerritoryResize(territoryId, ownerId, first, second, backpoint);
        PrepareResult result =
            switch (outcome.result()) {
              case READY -> PrepareResult.READY;
              case UNCHANGED -> PrepareResult.UNCHANGED;
              case TERRITORY_NOT_FOUND -> PrepareResult.TERRITORY_NOT_FOUND;
              case OWNER_MISMATCH -> PrepareResult.NO_PERMISSION;
              case INVALID_BOUNDS -> PrepareResult.INVALID_BOUNDS;
              case OVERLAP -> PrepareResult.OVERLAP;
              case PRICE_OVERFLOW -> PrepareResult.PRICE_OVERFLOW;
              case STATE_UNKNOWN -> PrepareResult.STATE_UNKNOWN;
            };
        return new PrepareOutcome(
            result,
            outcome.plan() == null ? null : new ResizePlan(outcome.plan().charge(), outcome.plan()),
            outcome.failure());
      }

      @Override
      public Outcome commit(ResizePlan plan) {
        TerritoryManager.ResizeOutcome outcome =
            TerritoryManager.commitTerritoryResize((TerritoryManager.ResizePlan) plan.token());
        return new Outcome(
            switch (outcome.result()) {
              case RESIZED -> Result.SUCCESS;
              case UNCHANGED -> Result.UNCHANGED;
              case CHANGED -> Result.CHANGED;
              case TERRITORY_NOT_FOUND -> Result.TERRITORY_NOT_FOUND;
              case OWNER_MISMATCH -> Result.NO_PERMISSION;
              case INVALID_BOUNDS -> Result.INVALID_BOUNDS;
              case OVERLAP -> Result.OVERLAP;
              case PERSIST_FAILED -> Result.PERSIST_FAILED;
              case STATE_UNKNOWN -> Result.STATE_UNKNOWN;
            },
            outcome.failure());
      }
    };
  }

  private static Outcome mapPrepare(PrepareOutcome outcome) {
    return new Outcome(
        switch (outcome.result()) {
          case UNCHANGED -> Result.UNCHANGED;
          case TERRITORY_NOT_FOUND -> Result.TERRITORY_NOT_FOUND;
          case NO_PERMISSION -> Result.NO_PERMISSION;
          case INVALID_BOUNDS -> Result.INVALID_BOUNDS;
          case OVERLAP -> Result.OVERLAP;
          case PRICE_OVERFLOW -> Result.PAYMENT_FAILED;
          case STATE_UNKNOWN -> Result.STATE_UNKNOWN;
          case READY -> throw new AssertionError();
        },
        outcome.failure());
  }

  private static void warn(
      Diagnostics diagnostics, String stage, UUID player, UUID territory, Throwable failure) {
    if (failure == null) failure = new IllegalStateException(stage);
    try {
      diagnostics.warning(stage, player, territory, failure);
    } catch (RuntimeException ignored) {
    }
  }

  private static void addSuppressed(Throwable primary, Throwable secondary) {
    if (secondary != null && secondary != primary) primary.addSuppressed(secondary);
  }
}

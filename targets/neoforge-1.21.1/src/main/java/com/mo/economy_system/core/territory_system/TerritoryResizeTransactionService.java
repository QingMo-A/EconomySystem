package com.mo.economy_system.core.territory_system;

import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;

/** NeoForge coordinate/persistence adapter for the common resize transaction. */
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
      Objects.requireNonNull(token, "token");
    }
  }

  public record PrepareOutcome(PrepareResult result, ResizePlan plan, Throwable failure) {
    public PrepareOutcome {
      Objects.requireNonNull(result, "result");
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
    PrepareOutcome prepare(
        UUID territoryId,
        UUID expectedOwnerId,
        BlockPos first,
        BlockPos second,
        BlockPos backpoint);

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
      UUID territoryId,
      BlockPos first,
      BlockPos second,
      BlockPos backpoint) {
    Objects.requireNonNull(repository, "repository");
    com.mo.economy_system.common.territory.TerritoryResizeTransactionService.Outcome outcome =
        com.mo.economy_system.common.territory.TerritoryResizeTransactionService.execute(
            new com.mo.economy_system.common.territory.TerritoryResizeTransactionService.BalancePort() {
              @Override
              public BalanceMutationResult debitExact(UUID id, int amount) {
                return balance.debitExact(id, amount);
              }

              @Override
              public BalanceMutationResult creditExact(UUID id, int amount) {
                return balance.creditExact(id, amount);
              }
            },
            new com.mo.economy_system.common.territory.TerritoryResizeTransactionService.ResizeRepository() {
              @Override
              public com.mo.economy_system.common.territory.TerritoryResizeTransactionService.PrepareOutcome prepare(
                  UUID id, UUID owner) {
                PrepareOutcome value = repository.prepare(id, owner, first, second, backpoint);
                return new com.mo.economy_system.common.territory.TerritoryResizeTransactionService.PrepareOutcome(
                    map(value.result()),
                    value.plan() == null
                        ? null
                        : new com.mo.economy_system.common.territory.TerritoryResizeTransactionService.ResizePlan(
                            value.plan().charge(), value.plan()),
                    value.failure());
              }

              @Override
              public com.mo.economy_system.common.territory.TerritoryResizeTransactionService.Outcome commit(
                  com.mo.economy_system.common.territory.TerritoryResizeTransactionService.ResizePlan plan) {
                Outcome value = repository.commit(new ResizePlan(plan.charge(), plan.token()));
                return new com.mo.economy_system.common.territory.TerritoryResizeTransactionService.Outcome(
                    map(value.result()), value.failure());
              }
            },
            diagnostics::warning,
            playerId,
            territoryId);
    return new Outcome(mapCommon(outcome.result()), outcome.failure());
  }

  public static Outcome execute(
      EconomySavedData economy,
      UUID playerId,
      UUID territoryId,
      BlockPos first,
      BlockPos second,
      Diagnostics diagnostics) {
    Objects.requireNonNull(economy, "economy");
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
        new ResizeRepository() {
          @Override
          public PrepareOutcome prepare(
              UUID id, UUID owner, BlockPos a, BlockPos b, BlockPos backpoint) {
            TerritoryManager.ResizePrepareOutcome value =
                TerritoryManager.prepareTerritoryResize(id, owner, a, b, backpoint);
            return new PrepareOutcome(
                switch (value.result()) {
                  case READY -> PrepareResult.READY;
                  case UNCHANGED -> PrepareResult.UNCHANGED;
                  case TERRITORY_NOT_FOUND -> PrepareResult.TERRITORY_NOT_FOUND;
                  case OWNER_MISMATCH -> PrepareResult.NO_PERMISSION;
                  case INVALID_BOUNDS -> PrepareResult.INVALID_BOUNDS;
                  case OVERLAP -> PrepareResult.OVERLAP;
                  case PRICE_OVERFLOW -> PrepareResult.PRICE_OVERFLOW;
                  case STATE_UNKNOWN -> PrepareResult.STATE_UNKNOWN;
                },
                value.plan() == null
                    ? null
                    : new ResizePlan(value.plan().charge(), value.plan()),
                value.failure());
          }

          @Override
          public Outcome commit(ResizePlan plan) {
            TerritoryManager.ResizeOutcome value =
                TerritoryManager.commitTerritoryResize((TerritoryManager.ResizePlan) plan.token());
            return new Outcome(
                switch (value.result()) {
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
                value.failure());
          }
        },
        diagnostics,
        playerId,
        territoryId,
        first,
        second,
        first);
  }

  private static com.mo.economy_system.common.territory.TerritoryResizeTransactionService.PrepareResult map(PrepareResult result) {
    return switch (result) {
      case READY -> com.mo.economy_system.common.territory.TerritoryResizeTransactionService.PrepareResult.READY;
      case UNCHANGED -> com.mo.economy_system.common.territory.TerritoryResizeTransactionService.PrepareResult.UNCHANGED;
      case TERRITORY_NOT_FOUND -> com.mo.economy_system.common.territory.TerritoryResizeTransactionService.PrepareResult.TERRITORY_NOT_FOUND;
      case NO_PERMISSION -> com.mo.economy_system.common.territory.TerritoryResizeTransactionService.PrepareResult.NO_PERMISSION;
      case INVALID_BOUNDS -> com.mo.economy_system.common.territory.TerritoryResizeTransactionService.PrepareResult.INVALID_BOUNDS;
      case OVERLAP -> com.mo.economy_system.common.territory.TerritoryResizeTransactionService.PrepareResult.OVERLAP;
      case PRICE_OVERFLOW -> com.mo.economy_system.common.territory.TerritoryResizeTransactionService.PrepareResult.PRICE_OVERFLOW;
      case STATE_UNKNOWN -> com.mo.economy_system.common.territory.TerritoryResizeTransactionService.PrepareResult.STATE_UNKNOWN;
    };
  }

  private static com.mo.economy_system.common.territory.TerritoryResizeTransactionService.Result map(Result result) {
    return switch (result) {
      case SUCCESS -> com.mo.economy_system.common.territory.TerritoryResizeTransactionService.Result.SUCCESS;
      case UNCHANGED -> com.mo.economy_system.common.territory.TerritoryResizeTransactionService.Result.UNCHANGED;
      case INSUFFICIENT_FUNDS -> com.mo.economy_system.common.territory.TerritoryResizeTransactionService.Result.INSUFFICIENT_FUNDS;
      case TERRITORY_NOT_FOUND -> com.mo.economy_system.common.territory.TerritoryResizeTransactionService.Result.TERRITORY_NOT_FOUND;
      case NO_PERMISSION -> com.mo.economy_system.common.territory.TerritoryResizeTransactionService.Result.NO_PERMISSION;
      case INVALID_BOUNDS -> com.mo.economy_system.common.territory.TerritoryResizeTransactionService.Result.INVALID_BOUNDS;
      case OVERLAP -> com.mo.economy_system.common.territory.TerritoryResizeTransactionService.Result.OVERLAP;
      case CHANGED -> com.mo.economy_system.common.territory.TerritoryResizeTransactionService.Result.CHANGED;
      case PERSIST_FAILED -> com.mo.economy_system.common.territory.TerritoryResizeTransactionService.Result.PERSIST_FAILED;
      case STATE_UNKNOWN -> com.mo.economy_system.common.territory.TerritoryResizeTransactionService.Result.STATE_UNKNOWN;
      case REFUND_FAILED -> com.mo.economy_system.common.territory.TerritoryResizeTransactionService.Result.REFUND_FAILED;
      case PAYMENT_FAILED -> com.mo.economy_system.common.territory.TerritoryResizeTransactionService.Result.PAYMENT_FAILED;
    };
  }

  private static Result mapCommon(
      com.mo.economy_system.common.territory.TerritoryResizeTransactionService.Result result) {
    return switch (result) {
      case SUCCESS -> Result.SUCCESS;
      case UNCHANGED -> Result.UNCHANGED;
      case INSUFFICIENT_FUNDS -> Result.INSUFFICIENT_FUNDS;
      case TERRITORY_NOT_FOUND -> Result.TERRITORY_NOT_FOUND;
      case NO_PERMISSION -> Result.NO_PERMISSION;
      case WRONG_DIMENSION -> Result.STATE_UNKNOWN;
      case INVALID_BOUNDS -> Result.INVALID_BOUNDS;
      case OVERLAP -> Result.OVERLAP;
      case CHANGED -> Result.CHANGED;
      case PERSIST_FAILED -> Result.PERSIST_FAILED;
      case STATE_UNKNOWN -> Result.STATE_UNKNOWN;
      case REFUND_FAILED -> Result.REFUND_FAILED;
      case PAYMENT_FAILED -> Result.PAYMENT_FAILED;
    };
  }
}

package com.mo.economy_system.core.territory_system;

import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;

public final class TerritoryResizeTransactionService {
  public enum Result {
    SUCCESS,
    INSUFFICIENT_FUNDS,
    TERRITORY_NOT_FOUND,
    NO_PERMISSION,
    INVALID_BOUNDS,
    OVERLAP,
    PERSIST_FAILED,
    STATE_UNKNOWN,
    REFUND_FAILED
  }

  public record Outcome(Result result, Throwable failure) {}

  private TerritoryResizeTransactionService() {}

  public static Outcome execute(
      EconomySavedData economy,
      UUID playerId,
      UUID territoryId,
      BlockPos first,
      BlockPos second,
      long areaDifference) {
    Objects.requireNonNull(economy);
    Objects.requireNonNull(playerId);
    Objects.requireNonNull(territoryId);
    if (first == null || second == null) return new Outcome(Result.INVALID_BOUNDS, null);
    int price = 0;
    if (areaDifference > 0) {
      long calculated =
          areaDifference > Long.MAX_VALUE / 20L ? Long.MAX_VALUE : areaDifference * 20L;
      if (calculated > EconomySavedData.MAX_BALANCE)
        return new Outcome(Result.INSUFFICIENT_FUNDS, null);
      price = (int) calculated;
      BalanceMutationResult debit = economy.debitExact(playerId, price, "领地", "调整领地大小");
      if (debit == BalanceMutationResult.INSUFFICIENT_FUNDS)
        return new Outcome(Result.INSUFFICIENT_FUNDS, null);
      if (debit != BalanceMutationResult.SUCCESS)
        return new Outcome(
            Result.PERSIST_FAILED, new IllegalStateException("resize debit failed: " + debit));
    }
    TerritoryManager.ResizeOutcome mutation =
        TerritoryManager.resizeTerritoryAuthoritatively(
            territoryId, playerId, first, second, first);
    Result mapped = map(mutation.result());
    if (mapped == Result.SUCCESS || price == 0 || mapped == Result.STATE_UNKNOWN)
      return new Outcome(mapped, mutation.failure());
    BalanceMutationResult refund = economy.creditExact(playerId, price, "领地", "领地调整失败退款");
    if (refund != BalanceMutationResult.SUCCESS) {
      IllegalStateException failure = new IllegalStateException("resize refund failed: " + refund);
      if (mutation.failure() != null) failure.addSuppressed(mutation.failure());
      return new Outcome(Result.REFUND_FAILED, failure);
    }
    return new Outcome(mapped, mutation.failure());
  }

  private static Result map(TerritoryManager.ResizeResult result) {
    return switch (result) {
      case RESIZED -> Result.SUCCESS;
      case TERRITORY_NOT_FOUND -> Result.TERRITORY_NOT_FOUND;
      case OWNER_MISMATCH -> Result.NO_PERMISSION;
      case INVALID_BOUNDS -> Result.INVALID_BOUNDS;
      case OVERLAP -> Result.OVERLAP;
      case PERSIST_FAILED -> Result.PERSIST_FAILED;
      case STATE_UNKNOWN -> Result.STATE_UNKNOWN;
    };
  }
}

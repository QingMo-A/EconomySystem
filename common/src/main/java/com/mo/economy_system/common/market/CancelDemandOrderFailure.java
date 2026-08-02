package com.mo.economy_system.common.market;

import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import java.util.Objects;
import java.util.UUID;

public record CancelDemandOrderFailure(
    UUID tradeId,
    UUID actorId,
    UUID requesterId,
    boolean operator,
    String stage,
    CancelDemandOrderResult result,
    MarketMutationState mutationState,
    DemandOrderRemovalStatus removalStatus,
    BalanceMutationResult refundResult,
    MarketOrderRestoreResult restoreResult,
    RuntimeException primaryError,
    RuntimeException repositoryError,
    RuntimeException refundError,
    RuntimeException restoreError) {
  public CancelDemandOrderFailure {
    Objects.requireNonNull(tradeId, "tradeId");
    Objects.requireNonNull(actorId, "actorId");
    Objects.requireNonNull(stage, "stage");
    Objects.requireNonNull(result, "result");
    Objects.requireNonNull(mutationState, "mutationState");
    if (stage.isBlank()) throw new IllegalArgumentException("stage is blank");
  }

  public RuntimeException combinedError() {
    RuntimeException combined = firstNonNull(primaryError, repositoryError, refundError, restoreError);
    if (combined == null) return null;
    addSuppressed(combined, primaryError);
    addSuppressed(combined, repositoryError);
    addSuppressed(combined, refundError);
    addSuppressed(combined, restoreError);
    return combined;
  }

  private static RuntimeException firstNonNull(RuntimeException... errors) {
    for (RuntimeException error : errors) if (error != null) return error;
    return null;
  }

  private static void addSuppressed(RuntimeException combined, RuntimeException error) {
    if (error == null || error == combined) return;
    for (Throwable existing : combined.getSuppressed()) if (existing == error) return;
    combined.addSuppressed(error);
  }
}

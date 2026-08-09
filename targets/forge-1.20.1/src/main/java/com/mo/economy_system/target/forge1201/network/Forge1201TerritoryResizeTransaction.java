package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.territory.TerritoryResizeTransactionService;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import java.util.Objects;
import java.util.UUID;

/** Forge coordinate/persistence adapter for the common resize transaction. */
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
    TerritoryResizeTransactionService.Outcome outcome =
        TerritoryResizeTransactionService.execute(
            new TerritoryResizeTransactionService.BalancePort() {
              @Override
              public BalanceMutationResult debitExact(UUID id, int amount) {
                return accounts.debitExact(id, amount, "领地", "调整领地大小");
              }

              @Override
              public BalanceMutationResult creditExact(UUID id, int amount) {
                return accounts.creditExact(id, amount, "领地", "领地调整失败退款");
              }
            },
            new TerritoryResizeTransactionService.ResizeRepository() {
              @Override
              public TerritoryResizeTransactionService.PrepareOutcome prepare(
                  UUID id, UUID owner) {
                Forge1201TerritorySnapshotStore.ResizePrepareOutcome value =
                    store.prepareResize(id, owner, dimensionId, first, second);
                return new TerritoryResizeTransactionService.PrepareOutcome(
                    switch (value.result()) {
                      case READY -> TerritoryResizeTransactionService.PrepareResult.READY;
                      case UNCHANGED -> TerritoryResizeTransactionService.PrepareResult.UNCHANGED;
                      case NOT_FOUND -> TerritoryResizeTransactionService.PrepareResult.TERRITORY_NOT_FOUND;
                      case NOT_OWNER -> TerritoryResizeTransactionService.PrepareResult.NO_PERMISSION;
                      case WRONG_DIMENSION -> TerritoryResizeTransactionService.PrepareResult.WRONG_DIMENSION;
                      case INVALID_BOUNDS -> TerritoryResizeTransactionService.PrepareResult.INVALID_BOUNDS;
                      case OVERLAP -> TerritoryResizeTransactionService.PrepareResult.OVERLAP;
                      case PRICE_OVERFLOW -> TerritoryResizeTransactionService.PrepareResult.PRICE_OVERFLOW;
                      case STATE_UNKNOWN -> TerritoryResizeTransactionService.PrepareResult.STATE_UNKNOWN;
                    },
                    value.plan() == null
                        ? null
                        : new TerritoryResizeTransactionService.ResizePlan(
                            value.plan().charge(), value.plan()),
                    value.failure());
              }

              @Override
              public TerritoryResizeTransactionService.Outcome commit(
                  TerritoryResizeTransactionService.ResizePlan plan) {
                Forge1201TerritorySnapshotStore.ResizeCommitResult value =
                    store.commitResize((Forge1201TerritorySnapshotStore.ResizePlan) plan.token());
                return new TerritoryResizeTransactionService.Outcome(
                    switch (value) {
                      case SUCCESS -> TerritoryResizeTransactionService.Result.SUCCESS;
                      case NOT_FOUND -> TerritoryResizeTransactionService.Result.TERRITORY_NOT_FOUND;
                      case CHANGED -> TerritoryResizeTransactionService.Result.CHANGED;
                      case OVERLAP -> TerritoryResizeTransactionService.Result.OVERLAP;
                      case PERSIST_FAILED -> TerritoryResizeTransactionService.Result.PERSIST_FAILED;
                      case STATE_UNKNOWN -> TerritoryResizeTransactionService.Result.STATE_UNKNOWN;
                    },
                    null);
              }
            },
            diagnostics::warning,
            playerId,
            territoryId);
    return new Outcome(map(outcome.result()), outcome.failure());
  }

  private static Result map(TerritoryResizeTransactionService.Result result) {
    return switch (result) {
      case SUCCESS -> Result.SUCCESS;
      case UNCHANGED -> Result.UNCHANGED;
      case INSUFFICIENT_FUNDS -> Result.INSUFFICIENT_FUNDS;
      case TERRITORY_NOT_FOUND -> Result.NOT_FOUND;
      case NO_PERMISSION -> Result.NOT_OWNER;
      case WRONG_DIMENSION -> Result.WRONG_DIMENSION;
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

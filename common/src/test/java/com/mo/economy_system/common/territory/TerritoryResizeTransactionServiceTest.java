package com.mo.economy_system.common.territory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryResizeTransactionServiceTest {
  private static final UUID PLAYER = UUID.randomUUID();
  private static final UUID TERRITORY = UUID.randomUUID();

  @Test
  void debitsAuthoritativePreparedChargeAndCommits() {
    Fixture fixture = new Fixture(1_980, TerritoryResizeTransactionService.Result.SUCCESS);
    assertEquals(TerritoryResizeTransactionService.Result.SUCCESS, fixture.execute().result());
    assertEquals(List.of(1_980), fixture.debits);
    assertEquals(1, fixture.commits);
  }

  @Test
  void insufficientFundsNeverCommits() {
    Fixture fixture = new Fixture(20, TerritoryResizeTransactionService.Result.SUCCESS);
    fixture.debit = BalanceMutationResult.INSUFFICIENT_FUNDS;
    assertEquals(
        TerritoryResizeTransactionService.Result.INSUFFICIENT_FUNDS, fixture.execute().result());
    assertEquals(0, fixture.commits);
  }

  @Test
  void mutationFailureRefundsButUncertainStateDoesNot() {
    Fixture changed = new Fixture(20, TerritoryResizeTransactionService.Result.CHANGED);
    assertEquals(TerritoryResizeTransactionService.Result.CHANGED, changed.execute().result());
    assertEquals(List.of(20), changed.refunds);

    Fixture uncertain = new Fixture(20, TerritoryResizeTransactionService.Result.STATE_UNKNOWN);
    assertEquals(TerritoryResizeTransactionService.Result.STATE_UNKNOWN, uncertain.execute().result());
    assertTrue(uncertain.refunds.isEmpty());
  }

  @Test
  void refundFailureAndDiagnosticsAreExplicitWithoutChangingPrimaryPolicy() {
    Fixture fixture = new Fixture(20, TerritoryResizeTransactionService.Result.CHANGED);
    fixture.refund = BalanceMutationResult.PERSIST_FAILED;
    fixture.diagnosticsThrows = true;
    assertEquals(TerritoryResizeTransactionService.Result.REFUND_FAILED, fixture.execute().result());
    assertTrue(fixture.stages.contains("payment-refund"));
  }

  @Test
  void throwingDebitIsStateUnknownAndIsNeverRefunded() {
    Fixture fixture = new Fixture(20, TerritoryResizeTransactionService.Result.SUCCESS);
    fixture.debitThrows = true;

    assertEquals(TerritoryResizeTransactionService.Result.STATE_UNKNOWN, fixture.execute().result());
    assertEquals(List.of(20), fixture.debits);
    assertTrue(fixture.refunds.isEmpty());
    assertEquals(0, fixture.commits);
  }

  @Test
  void prepareFailuresMapBeforePayment() {
    Fixture fixture = new Fixture(20, TerritoryResizeTransactionService.Result.SUCCESS);
    fixture.prepareResult = TerritoryResizeTransactionService.PrepareResult.PRICE_OVERFLOW;
    assertEquals(TerritoryResizeTransactionService.Result.PAYMENT_FAILED, fixture.execute().result());
    assertTrue(fixture.debits.isEmpty());
  }

  private static final class Fixture
      implements TerritoryResizeTransactionService.BalancePort,
          TerritoryResizeTransactionService.ResizeRepository,
          TerritoryResizeTransactionService.Diagnostics {
    private final int charge;
    private final TerritoryResizeTransactionService.Result commitResult;
    private final List<Integer> debits = new ArrayList<>();
    private final List<Integer> refunds = new ArrayList<>();
    private final List<String> stages = new ArrayList<>();
    private BalanceMutationResult debit = BalanceMutationResult.SUCCESS;
    private BalanceMutationResult refund = BalanceMutationResult.SUCCESS;
    private TerritoryResizeTransactionService.PrepareResult prepareResult =
        TerritoryResizeTransactionService.PrepareResult.READY;
    private boolean diagnosticsThrows;
    private boolean debitThrows;
    private int commits;

    private Fixture(int charge, TerritoryResizeTransactionService.Result commitResult) {
      this.charge = charge;
      this.commitResult = commitResult;
    }

    private TerritoryResizeTransactionService.Outcome execute() {
      return TerritoryResizeTransactionService.execute(this, this, this, PLAYER, TERRITORY);
    }

    @Override
    public BalanceMutationResult debitExact(UUID playerId, int amount) {
      debits.add(amount);
      if (debitThrows) throw new IllegalStateException("debit");
      return debit;
    }

    @Override
    public BalanceMutationResult creditExact(UUID playerId, int amount) {
      refunds.add(amount);
      return refund;
    }

    @Override
    public TerritoryResizeTransactionService.PrepareOutcome prepare(
        UUID territoryId, UUID expectedOwnerId) {
      return new TerritoryResizeTransactionService.PrepareOutcome(
          prepareResult,
          prepareResult == TerritoryResizeTransactionService.PrepareResult.READY
              ? new TerritoryResizeTransactionService.ResizePlan(charge, this)
              : null,
          null);
    }

    @Override
    public TerritoryResizeTransactionService.Outcome commit(
        TerritoryResizeTransactionService.ResizePlan plan) {
      commits++;
      return new TerritoryResizeTransactionService.Outcome(
          commitResult,
          commitResult == TerritoryResizeTransactionService.Result.SUCCESS
              ? null
              : new IllegalStateException("mutation"));
    }

    @Override
    public void warning(String stage, UUID playerId, UUID territoryId, Throwable failure) {
      stages.add(stage);
      if (diagnosticsThrows) throw new IllegalStateException("diagnostics");
    }
  }
}

package com.mo.economy_system.core.territory_system;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class TerritoryResizeTransactionServiceTest {
  private static final UUID PLAYER = UUID.randomUUID();
  private static final UUID TERRITORY = UUID.randomUUID();
  private static final BlockPos FIRST = new BlockPos(0, 64, 0);
  private static final BlockPos SECOND = new BlockPos(9, 64, 9);

  @Test
  void authoritativePreparedPriceIsDebited() {
    Fixture fixture = new Fixture(1_980, TerritoryResizeTransactionService.Result.SUCCESS);
    assertEquals(TerritoryResizeTransactionService.Result.SUCCESS, fixture.execute().result());
    assertEquals(List.of(1_980), fixture.debits);
  }

  @Test
  void cachedAreaDifferenceCannotEnterTransactionApi() {
    assertTrue(
        java.util.Arrays.stream(TerritoryResizeTransactionService.class.getMethods())
            .filter(method -> method.getName().equals("execute"))
            .noneMatch(
                method ->
                    java.util.Arrays.stream(method.getParameterTypes())
                        .anyMatch(type -> type == long.class)));
  }

  @Test
  void equalAreaReshapeCommitsWithoutDebit() {
    Fixture fixture = new Fixture(0, TerritoryResizeTransactionService.Result.SUCCESS);
    assertEquals(TerritoryResizeTransactionService.Result.SUCCESS, fixture.execute().result());
    assertTrue(fixture.debits.isEmpty());
    assertEquals(1, fixture.commits);
  }

  @Test
  void unchangedDoesNotDebitOrCommit() {
    Fixture fixture = new Fixture(0, TerritoryResizeTransactionService.Result.SUCCESS);
    fixture.prepareResult = TerritoryResizeTransactionService.PrepareResult.UNCHANGED;
    assertEquals(TerritoryResizeTransactionService.Result.UNCHANGED, fixture.execute().result());
    assertTrue(fixture.debits.isEmpty());
    assertEquals(0, fixture.commits);
  }

  @Test
  void insufficientFundsDoesNotCommit() {
    Fixture fixture = new Fixture(20, TerritoryResizeTransactionService.Result.SUCCESS);
    fixture.debit = BalanceMutationResult.INSUFFICIENT_FUNDS;
    assertEquals(
        TerritoryResizeTransactionService.Result.INSUFFICIENT_FUNDS, fixture.execute().result());
    assertEquals(0, fixture.commits);
  }

  @Test
  void debitPersistenceFailureDoesNotCommit() {
    Fixture fixture = new Fixture(20, TerritoryResizeTransactionService.Result.SUCCESS);
    fixture.debit = BalanceMutationResult.PERSIST_FAILED;
    assertEquals(
        TerritoryResizeTransactionService.Result.PAYMENT_FAILED, fixture.execute().result());
    assertEquals(0, fixture.commits);
  }

  @Test
  void invalidDebitIsDiagnosed() {
    Fixture fixture = new Fixture(20, TerritoryResizeTransactionService.Result.SUCCESS);
    fixture.debit = BalanceMutationResult.INVALID_AMOUNT;
    assertEquals(
        TerritoryResizeTransactionService.Result.PAYMENT_FAILED, fixture.execute().result());
    assertEquals(List.of("payment-debit"), fixture.stages);
  }

  @Test
  void successfulMutationKeepsDebit() {
    Fixture fixture = new Fixture(20, TerritoryResizeTransactionService.Result.SUCCESS);
    assertEquals(TerritoryResizeTransactionService.Result.SUCCESS, fixture.execute().result());
    assertTrue(fixture.refunds.isEmpty());
  }

  @Test
  void notFoundRefunds() {
    assertRefunded(TerritoryResizeTransactionService.Result.TERRITORY_NOT_FOUND);
  }

  @Test
  void noPermissionRefunds() {
    assertRefunded(TerritoryResizeTransactionService.Result.NO_PERMISSION);
  }

  @Test
  void overlapRefunds() {
    assertRefunded(TerritoryResizeTransactionService.Result.OVERLAP);
  }

  @Test
  void changedRefunds() {
    assertRefunded(TerritoryResizeTransactionService.Result.CHANGED);
  }

  @Test
  void persistFailureRefunds() {
    assertRefunded(TerritoryResizeTransactionService.Result.PERSIST_FAILED);
  }

  @Test
  void uncertainMutationDoesNotRefund() {
    Fixture fixture = new Fixture(20, TerritoryResizeTransactionService.Result.STATE_UNKNOWN);
    assertEquals(
        TerritoryResizeTransactionService.Result.STATE_UNKNOWN, fixture.execute().result());
    assertTrue(fixture.refunds.isEmpty());
  }

  @Test
  void refundFailureIsExplicit() {
    Fixture fixture = new Fixture(20, TerritoryResizeTransactionService.Result.CHANGED);
    fixture.refund = BalanceMutationResult.PERSIST_FAILED;
    assertEquals(
        TerritoryResizeTransactionService.Result.REFUND_FAILED, fixture.execute().result());
  }

  @Test
  void diagnosticsRuntimeFailureDoesNotChangeResult() {
    Fixture fixture = new Fixture(20, TerritoryResizeTransactionService.Result.STATE_UNKNOWN);
    fixture.diagnosticsThrows = true;
    assertEquals(
        TerritoryResizeTransactionService.Result.STATE_UNKNOWN, fixture.execute().result());
  }

  @Test
  void jvmErrorsEscape() {
    Fixture fixture = new Fixture(20, TerritoryResizeTransactionService.Result.SUCCESS);
    fixture.error = new AssertionError("fatal");
    assertThrows(AssertionError.class, fixture::execute);
  }

  @Test
  void priceOverflowFailsBeforeDebit() {
    Fixture fixture = new Fixture(0, TerritoryResizeTransactionService.Result.SUCCESS);
    fixture.prepareResult = TerritoryResizeTransactionService.PrepareResult.PRICE_OVERFLOW;
    assertEquals(
        TerritoryResizeTransactionService.Result.PAYMENT_FAILED, fixture.execute().result());
    assertTrue(fixture.debits.isEmpty());
  }

  private static void assertRefunded(TerritoryResizeTransactionService.Result result) {
    Fixture fixture = new Fixture(20, result);
    assertEquals(result, fixture.execute().result());
    assertEquals(List.of(20), fixture.refunds);
  }

  private static final class Fixture
      implements TerritoryResizeTransactionService.BalancePort,
          TerritoryResizeTransactionService.ResizeRepository,
          TerritoryResizeTransactionService.Diagnostics {
    final int charge;
    final TerritoryResizeTransactionService.Result commitResult;
    final List<Integer> debits = new ArrayList<>();
    final List<Integer> refunds = new ArrayList<>();
    final List<String> stages = new ArrayList<>();
    BalanceMutationResult debit = BalanceMutationResult.SUCCESS;
    BalanceMutationResult refund = BalanceMutationResult.SUCCESS;
    TerritoryResizeTransactionService.PrepareResult prepareResult =
        TerritoryResizeTransactionService.PrepareResult.READY;
    boolean diagnosticsThrows;
    Error error;
    int commits;

    Fixture(int charge, TerritoryResizeTransactionService.Result commitResult) {
      this.charge = charge;
      this.commitResult = commitResult;
    }

    TerritoryResizeTransactionService.Outcome execute() {
      return TerritoryResizeTransactionService.execute(
          this, this, this, PLAYER, TERRITORY, FIRST, SECOND, FIRST);
    }

    @Override
    public BalanceMutationResult debitExact(UUID playerId, int amount) {
      debits.add(amount);
      return debit;
    }

    @Override
    public BalanceMutationResult creditExact(UUID playerId, int amount) {
      refunds.add(amount);
      return refund;
    }

    @Override
    public TerritoryResizeTransactionService.PrepareOutcome prepare(
        UUID territoryId,
        UUID expectedOwnerId,
        BlockPos first,
        BlockPos second,
        BlockPos backpoint) {
      if (error != null) throw error;
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

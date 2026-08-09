package com.mo.economy_system.common.territory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryClaimServiceTest {
  private static final UUID OWNER = UUID.randomUUID();

  @Test
  void overlapIsRejectedBeforePayment() {
    Fixture fixture = new Fixture();
    fixture.overlap = true;

    TerritoryClaimService.Outcome outcome = fixture.execute(request(0, 0, 2, 2));

    assertEquals(TerritoryClaimService.Result.OVERLAP, outcome.result());
    assertEquals(0, fixture.debits);
    assertEquals(0, fixture.creates);
  }

  @Test
  void insufficientFundsNeverCreates() {
    Fixture fixture = new Fixture();
    fixture.debit = BalanceMutationResult.INSUFFICIENT_FUNDS;

    TerritoryClaimService.Outcome outcome = fixture.execute(request(0, 0, 2, 2));

    assertEquals(TerritoryClaimService.Result.INSUFFICIENT_FUNDS, outcome.result());
    assertEquals(1, fixture.debits);
    assertEquals(0, fixture.creates);
  }

  @Test
  void persistenceFailureRefundsTheExactCharge() {
    Fixture fixture = new Fixture();
    fixture.create = TerritoryClaimService.RepositoryResult.PERSIST_FAILED;

    TerritoryClaimService.Outcome outcome = fixture.execute(request(0, 0, 2, 2));

    assertEquals(TerritoryClaimService.Result.PERSIST_FAILED, outcome.result());
    assertEquals(1, fixture.debits);
    assertEquals(1, fixture.credits);
    assertEquals(outcome.price(), fixture.lastCredit);
  }

  @Test
  void refundFailureIsExplicitForKnownPersistenceFailure() {
    Fixture fixture = new Fixture();
    fixture.create = TerritoryClaimService.RepositoryResult.PERSIST_FAILED;
    fixture.credit = BalanceMutationResult.PERSIST_FAILED;

    TerritoryClaimService.Outcome outcome = fixture.execute(request(0, 0, 2, 2));

    assertEquals(TerritoryClaimService.Result.REFUND_FAILED, outcome.result());
    assertEquals(1, fixture.credits);
  }

  @Test
  void unknownRepositoryStateNeverRefundsTheClaimCharge() {
    Fixture fixture = new Fixture();
    fixture.create = TerritoryClaimService.RepositoryResult.STATE_UNKNOWN;

    TerritoryClaimService.Outcome outcome = fixture.execute(request(0, 0, 2, 2));

    assertEquals(TerritoryClaimService.Result.STATE_UNKNOWN, outcome.result());
    assertEquals(0, fixture.credits);
  }

  @Test
  void throwingDebitIsStateUnknownAndNeverCreatesOrRefunds() {
    Fixture fixture = new Fixture();
    fixture.debitThrows = true;

    TerritoryClaimService.Outcome outcome = fixture.execute(request(0, 0, 2, 2));

    assertEquals(TerritoryClaimService.Result.STATE_UNKNOWN, outcome.result());
    assertEquals(0, fixture.creates);
    assertEquals(0, fixture.credits);
  }

  @Test
  void invalidHeightAndCoordinatesAreRejectedWithoutPayment() {
    Fixture fixture = new Fixture();

    assertEquals(
        TerritoryClaimService.Result.INVALID_INPUT,
        fixture.execute(request(0, 0, 1, 1, 0, 1)).result());
    assertEquals(
        TerritoryClaimService.Result.INVALID_INPUT,
        fixture.execute(request(30_000_001, 0, 30_000_001, 1)).result());
    assertEquals(0, fixture.debits);
  }

  @Test
  void priceOverflowIsRejectedBeforePayment() {
    Fixture fixture = new Fixture();

    TerritoryClaimService.Outcome outcome = fixture.execute(
        request(-30_000_000, -30_000_000, 30_000_000, 30_000_000));

    assertEquals(TerritoryClaimService.Result.PRICE_OVERFLOW, outcome.result());
    assertTrue(outcome.area() > 0);
    assertEquals(0, fixture.debits);
    assertFalse(fixture.created);
  }

  private static TerritoryClaimService.Request request(int x1, int z1, int x2, int z2) {
    return request(x1, z1, x2, z2, 64, 64);
  }

  private static TerritoryClaimService.Request request(
      int x1, int z1, int x2, int z2, int y1, int y2) {
    return new TerritoryClaimService.Request(
        OWNER,
        "owner",
        "claim",
        "minecraft:overworld",
        new TerritorySnapshots.Position(x1, y1, z1),
        new TerritorySnapshots.Position(x2, y2, z2));
  }

  private static final class Fixture
      implements TerritoryClaimService.Balance, TerritoryClaimService.Repository {
    private BalanceMutationResult debit = BalanceMutationResult.SUCCESS;
    private BalanceMutationResult credit = BalanceMutationResult.SUCCESS;
    private TerritoryClaimService.RepositoryResult create =
        TerritoryClaimService.RepositoryResult.CREATED;
    private boolean overlap;
    private boolean debitThrows;
    private int debits;
    private int credits;
    private int creates;
    private int lastCredit;
    private boolean created;

    private TerritoryClaimService.Outcome execute(TerritoryClaimService.Request request) {
      return TerritoryClaimService.execute(request, this, this, (stage, owner, failure) -> {});
    }

    @Override
    public BalanceMutationResult debitExact(UUID ownerId, int amount) {
      debits++;
      if (debitThrows) throw new IllegalStateException("debit");
      return debit;
    }

    @Override
    public BalanceMutationResult creditExact(UUID ownerId, int amount) {
      credits++;
      lastCredit = amount;
      return credit;
    }

    @Override
    public boolean overlaps(TerritoryClaimService.Request request) {
      return overlap;
    }

    @Override
    public TerritoryClaimService.RepositoryResult create(
        TerritoryClaimService.Request request, long area, int price) {
      creates++;
      created = create == TerritoryClaimService.RepositoryResult.CREATED;
      return create;
    }
  }
}

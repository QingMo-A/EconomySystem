package com.mo.economy_system.common.starter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StarterKitServiceTest {
  private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Test
  void claimIsExactlyOnce() {
    FakeMarker marker = new FakeMarker();
    FakeAccounts accounts = new FakeAccounts();
    StarterKitService service = new StarterKitService(marker, accounts);

    assertEquals(new StarterKitService.Outcome(StarterKitService.Result.SUCCESS, 10_000), service.claim(PLAYER));
    assertEquals(new StarterKitService.Outcome(StarterKitService.Result.ALREADY_CLAIMED, 0), service.claim(PLAYER));
    assertEquals(10_000, accounts.balance);
  }

  @Test
  void balanceLimitDoesNotMarkClaimed() {
    FakeMarker marker = new FakeMarker();
    FakeAccounts accounts = new FakeAccounts();
    accounts.creditResult = BalanceMutationResult.BALANCE_LIMIT;
    StarterKitService service = new StarterKitService(marker, accounts);

    assertEquals(StarterKitService.Result.BALANCE_LIMIT, service.claim(PLAYER).result());
    assertEquals(0, marker.markCalls);
  }

  @Test
  void markerFailureCompensatesCreditAndAllowsRetry() {
    FakeMarker marker = new FakeMarker();
    marker.markFailure = true;
    FakeAccounts accounts = new FakeAccounts();
    StarterKitService service = new StarterKitService(marker, accounts);

    assertEquals(StarterKitService.Result.PERSIST_FAILED, service.claim(PLAYER).result());
    assertEquals(0, accounts.balance);
    marker.markFailure = false;
    assertEquals(StarterKitService.Result.SUCCESS, service.claim(PLAYER).result());
  }

  @Test
  void rollbackFailureIsStateUnknown() {
    FakeMarker marker = new FakeMarker();
    marker.markFailure = true;
    FakeAccounts accounts = new FakeAccounts();
    accounts.debitResult = BalanceMutationResult.PERSIST_FAILED;
    StarterKitService service = new StarterKitService(marker, accounts);

    assertEquals(StarterKitService.Result.STATE_UNKNOWN, service.claim(PLAYER).result());
  }

  @Test
  void markerRollbackFailureIsStateUnknown() {
    FakeMarker marker = new FakeMarker();
    marker.markFailure = true;
    marker.unmarkFailure = true;
    FakeAccounts accounts = new FakeAccounts();
    StarterKitService service = new StarterKitService(marker, accounts);

    assertEquals(StarterKitService.Result.STATE_UNKNOWN, service.claim(PLAYER).result());
    assertEquals(0, accounts.balance);
  }

  private static final class FakeMarker implements StarterKitPort {
    boolean claimed;
    boolean markFailure;
    boolean unmarkFailure;
    int markCalls;
    public boolean claimed(UUID id) { return claimed; }
    public void markClaimed(UUID id) throws Exception { markCalls++; if (markFailure) throw new IllegalStateException("marker"); claimed = true; }
    public void unmarkClaimed(UUID id) throws Exception {
      if (unmarkFailure) throw new IllegalStateException("unmarker");
      claimed = false;
    }
  }

  private static final class FakeAccounts implements StarterKitAccountPort {
    int balance;
    BalanceMutationResult creditResult = BalanceMutationResult.SUCCESS;
    BalanceMutationResult debitResult = BalanceMutationResult.SUCCESS;
    public BalanceMutationResult credit(UUID id, int amount, String category, String reason) { if (creditResult == BalanceMutationResult.SUCCESS) balance += amount; return creditResult; }
    public BalanceMutationResult debit(UUID id, int amount, String category, String reason) { if (debitResult == BalanceMutationResult.SUCCESS) balance -= amount; return debitResult; }
  }
}

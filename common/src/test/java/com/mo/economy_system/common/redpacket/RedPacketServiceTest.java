package com.mo.economy_system.common.redpacket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RedPacketServiceTest {
  private static final UUID SENDER = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID FIRST = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-000000000003");

  @Test
  void createDebitsOnceAndRejectsDuplicateSender() {
    Accounts accounts = new Accounts();
    accounts.balances.put(SENDER, 100);
    RedPacketService service = service(accounts, new RepositoryState());

    RedPacketService.CreateOutcome created =
        service.create(SENDER, "sender", 40, 1, RedPacket.Mode.EVEN, 2);
    assertEquals(RedPacketService.CreateResult.SUCCESS, created.result());
    assertEquals(60, accounts.balances.get(SENDER));

    RedPacketService.CreateOutcome duplicate =
        service.create(SENDER, "sender", 10, 1, RedPacket.Mode.EVEN, 2);
    assertEquals(RedPacketService.CreateResult.ALREADY_ACTIVE, duplicate.result());
    assertEquals(60, accounts.balances.get(SENDER));
  }

  @Test
  void evenClaimsAreAtomicAndFullPacketIsRemovedWithoutExplicitSender() {
    Accounts accounts = new Accounts();
    accounts.balances.put(SENDER, 100);
    RedPacketService service = service(accounts, new RepositoryState());
    service.create(SENDER, "sender", 10, 1, RedPacket.Mode.EVEN, 2);

    RedPacketService.ClaimOutcome first = service.claim(FIRST, null);
    assertEquals(RedPacketService.ClaimResult.SUCCESS, first.result());
    assertEquals(5, first.amount());
    assertEquals(5, accounts.balances.get(FIRST));
    assertEquals(5, service.find(SENDER).orElseThrow().remainingAmount());

    RedPacketService.ClaimOutcome second = service.claim(SECOND, null);
    assertEquals(RedPacketService.ClaimResult.SUCCESS, second.result());
    assertTrue(second.completed());
    assertEquals(5, accounts.balances.get(SECOND));
    assertTrue(service.find(SENDER).isEmpty());
  }

  @Test
  void luckyAllocationHandlesAmountSmallerThanParticipantCount() {
    Accounts accounts = new Accounts();
    accounts.balances.put(SENDER, 100);
    RedPacketService service = service(accounts, new RepositoryState());
    service.create(SENDER, "sender", 1, 1, RedPacket.Mode.LUCKY, 5);

    RedPacketService.ClaimOutcome outcome = service.claim(FIRST, null);
    assertEquals(RedPacketService.ClaimResult.SUCCESS, outcome.result());
    assertEquals(1, outcome.amount());
    assertTrue(service.find(SENDER).isEmpty());
  }

  @Test
  void duplicateClaimDoesNotCreditAgain() {
    Accounts accounts = new Accounts();
    accounts.balances.put(SENDER, 100);
    RedPacketService service = service(accounts, new RepositoryState());
    service.create(SENDER, "sender", 20, 1, RedPacket.Mode.EVEN, 3);

    RedPacketService.ClaimOutcome first = service.claim(FIRST, SENDER);
    assertEquals(RedPacketService.ClaimResult.SUCCESS, first.result());
    int credited = accounts.balances.get(FIRST);
    RedPacketService.ClaimOutcome duplicate = service.claim(FIRST, SENDER);
    assertEquals(RedPacketService.ClaimResult.ALREADY_CLAIMED, duplicate.result());
    assertEquals(credited, accounts.balances.get(FIRST));
  }

  @Test
  void expirationRefundsOfflineSenderThroughAccountPort() {
    Accounts accounts = new Accounts();
    accounts.balances.put(SENDER, 100);
    Clock clock = new Clock();
    RedPacketService service = service(accounts, new RepositoryState(), clock);
    service.create(SENDER, "sender", 30, 1, RedPacket.Mode.EVEN, 2);
    service.claim(FIRST, SENDER);
    clock.now = 60_000;

    List<RedPacketService.ExpireOutcome> outcomes = service.expire();
    assertEquals(1, outcomes.size());
    assertEquals(RedPacketService.ExpireResult.REFUNDED, outcomes.get(0).result());
    assertEquals(85, accounts.balances.get(SENDER));
    assertTrue(service.find(SENDER).isEmpty());
  }

  @Test
  void persistenceFailureCompensatesCreateAndClaim() {
    Accounts accounts = new Accounts();
    accounts.balances.put(SENDER, 100);
    RepositoryState repository = new RepositoryState();
    RedPacketService service = service(accounts, repository);
    repository.fail = true;
    RedPacketService.CreateOutcome create =
        service.create(SENDER, "sender", 20, 1, RedPacket.Mode.EVEN, 2);
    assertEquals(RedPacketService.CreateResult.PERSIST_FAILED, create.result());
    assertEquals(100, accounts.balances.get(SENDER));
    assertTrue(service.find(SENDER).isEmpty());

    repository.fail = false;
    service.create(SENDER, "sender", 20, 1, RedPacket.Mode.EVEN, 2);
    repository.fail = true;
    RedPacketService.ClaimOutcome claim = service.claim(FIRST, SENDER);
    assertEquals(RedPacketService.ClaimResult.PERSIST_FAILED, claim.result());
    assertEquals(0, accounts.balances.getOrDefault(FIRST, 0));
    assertEquals(80, accounts.balances.get(SENDER));
    assertEquals(20, service.find(SENDER).orElseThrow().remainingAmount());
  }

  @Test
  void unknownCreateDebitStateDoesNotCreateOrCompensate() {
    for (Fault fault : List.of(Fault.RETURN_NULL, Fault.THROW_BEFORE, Fault.MUTATE_THEN_THROW)) {
      Accounts accounts = new Accounts();
      accounts.balances.put(SENDER, 100);
      accounts.debitFault = fault;
      RedPacketService service = service(accounts, new RepositoryState());

      RedPacketService.CreateOutcome outcome =
          service.create(SENDER, "sender", 20, 1, RedPacket.Mode.EVEN, 2);

      assertEquals(RedPacketService.CreateResult.STATE_UNKNOWN, outcome.result());
      assertTrue(service.find(SENDER).isEmpty());
      assertEquals(fault == Fault.MUTATE_THEN_THROW ? 80 : 100, accounts.balances.get(SENDER));
      assertEquals(0, accounts.creditCalls);
    }
  }

  @Test
  void unknownClaimCreditStateLeavesPacketUnchangedWithoutBlindRollback() {
    for (Fault fault : List.of(Fault.RETURN_NULL, Fault.THROW_BEFORE, Fault.MUTATE_THEN_THROW)) {
      Accounts accounts = new Accounts();
      accounts.balances.put(SENDER, 100);
      RedPacketService service = service(accounts, new RepositoryState());
      service.create(SENDER, "sender", 20, 1, RedPacket.Mode.EVEN, 2);
      accounts.debitCalls = 0;
      accounts.creditFault = fault;

      RedPacketService.ClaimOutcome outcome = service.claim(FIRST, SENDER);

      assertEquals(RedPacketService.ClaimResult.STATE_UNKNOWN, outcome.result());
      assertEquals(20, service.find(SENDER).orElseThrow().remainingAmount());
      assertEquals(fault == Fault.MUTATE_THEN_THROW ? 10 : 0,
          accounts.balances.getOrDefault(FIRST, 0));
      assertEquals(0, accounts.debitCalls);
    }
  }

  @Test
  void unknownCancelCreditStateLeavesPacketUnchangedWithoutBlindRollback() {
    for (Fault fault : List.of(Fault.RETURN_NULL, Fault.THROW_BEFORE, Fault.MUTATE_THEN_THROW)) {
      Accounts accounts = new Accounts();
      accounts.balances.put(SENDER, 100);
      RedPacketService service = service(accounts, new RepositoryState());
      service.create(SENDER, "sender", 20, 1, RedPacket.Mode.EVEN, 2);
      accounts.debitCalls = 0;
      accounts.creditFault = fault;

      RedPacketService.CancelOutcome outcome = service.cancel(SENDER);

      assertEquals(RedPacketService.CancelResult.STATE_UNKNOWN, outcome.result());
      assertEquals(20, service.find(SENDER).orElseThrow().remainingAmount());
      assertEquals(fault == Fault.MUTATE_THEN_THROW ? 100 : 80, accounts.balances.get(SENDER));
      assertEquals(0, accounts.debitCalls);
    }
  }

  @Test
  void unknownExpirationCreditStateLeavesPacketUnchangedWithoutBlindRollback() {
    for (Fault fault : List.of(Fault.RETURN_NULL, Fault.THROW_BEFORE, Fault.MUTATE_THEN_THROW)) {
      Accounts accounts = new Accounts();
      accounts.balances.put(SENDER, 100);
      Clock clock = new Clock();
      RedPacketService service = service(accounts, new RepositoryState(), clock);
      service.create(SENDER, "sender", 20, 1, RedPacket.Mode.EVEN, 2);
      accounts.debitCalls = 0;
      accounts.creditFault = fault;
      clock.now = 60_000;

      RedPacketService.ExpireOutcome outcome = service.expire().get(0);

      assertEquals(RedPacketService.ExpireResult.STATE_UNKNOWN, outcome.result());
      assertEquals(20, service.find(SENDER).orElseThrow().remainingAmount());
      assertEquals(fault == Fault.MUTATE_THEN_THROW ? 100 : 80, accounts.balances.get(SENDER));
      assertEquals(0, accounts.debitCalls);
    }
  }

  private static RedPacketService service(Accounts accounts, RepositoryState repository) {
    return service(accounts, repository, new Clock());
  }

  private static RedPacketService service(
      Accounts accounts, RepositoryState repository, Clock clock) {
    return new RedPacketService(accounts, repository, () -> clock.now, bound -> 0);
  }

  private static final class Clock {
    long now;
  }

  private enum Fault {
    NONE,
    RETURN_NULL,
    THROW_BEFORE,
    MUTATE_THEN_THROW
  }

  private static final class Accounts implements RedPacketAccountPort {
    final Map<UUID, Integer> balances = new HashMap<>();
    Fault debitFault = Fault.NONE;
    Fault creditFault = Fault.NONE;
    int debitCalls;
    int creditCalls;

    @Override
    public BalanceMutationResult debit(UUID playerId, int amount, String category, String reason) {
      debitCalls++;
      if (debitFault == Fault.RETURN_NULL) return null;
      if (debitFault == Fault.THROW_BEFORE) throw new IllegalStateException("debit");
      int current = balances.getOrDefault(playerId, 0);
      if (amount <= 0) return BalanceMutationResult.INVALID_AMOUNT;
      if (current < amount) return BalanceMutationResult.INSUFFICIENT_FUNDS;
      balances.put(playerId, current - amount);
      if (debitFault == Fault.MUTATE_THEN_THROW) {
        throw new IllegalStateException("debit after mutation");
      }
      return BalanceMutationResult.SUCCESS;
    }

    @Override
    public BalanceMutationResult credit(UUID playerId, int amount, String category, String reason) {
      creditCalls++;
      if (creditFault == Fault.RETURN_NULL) return null;
      if (creditFault == Fault.THROW_BEFORE) throw new IllegalStateException("credit");
      int current = balances.getOrDefault(playerId, 0);
      if (amount <= 0) return BalanceMutationResult.INVALID_AMOUNT;
      if ((long) current + amount > Integer.MAX_VALUE) return BalanceMutationResult.BALANCE_LIMIT;
      balances.put(playerId, current + amount);
      if (creditFault == Fault.MUTATE_THEN_THROW) {
        throw new IllegalStateException("credit after mutation");
      }
      return BalanceMutationResult.SUCCESS;
    }
  }

  private static final class RepositoryState implements RedPacketRepository {
    List<RedPacket> packets = new ArrayList<>();
    boolean fail;

    @Override
    public List<RedPacket> load() {
      return List.copyOf(packets);
    }

    @Override
    public void save(List<RedPacket> packets) {
      if (fail) throw new IllegalStateException("simulated persistence failure");
      this.packets = List.copyOf(packets);
    }
  }
}

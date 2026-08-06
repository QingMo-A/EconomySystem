package com.mo.economy_system.common.territory;

import static com.mo.economy_system.common.territory.TerritoryManagementTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.territory.TerritoryBuffTransactionService.Action;
import com.mo.economy_system.common.territory.TerritorySnapshots.Buff;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class TerritoryBuffTransactionServiceTest {
  @Test
  void successDebitsAllResourcesBeforeCompareAndSetMutation() {
    Fixture fixture = new Fixture(owned());
    assertEquals(TerritoryManagementResult.SUCCESS, fixture.execute(Action.UNLOCK));
    assertEquals(90, fixture.accounts.balance);
    assertEquals(7, fixture.resources.experience);
    assertEquals(0, fixture.resources.items.get("minecraft:diamond"));
    assertEquals(1, fixture.repository.mutations.get());
    assertFalse(fixture.repository.expectedUnlocked);
    assertEquals(0, fixture.repository.expectedLevel);
  }

  @Test
  void knownPersistenceFailureCompensatesItemsExperienceAndBalance() {
    Fixture fixture = new Fixture(owned());
    fixture.repository.result = TerritoryBuffTransactionService.RepositoryResult.PERSIST_FAILED;
    assertEquals(TerritoryManagementResult.PERSIST_FAILED, fixture.execute(Action.UNLOCK));
    assertEquals(100, fixture.accounts.balance);
    assertEquals(10, fixture.resources.experience);
    assertEquals(2, fixture.resources.items.get("minecraft:diamond"));
  }

  @Test
  void unknownTerritoryStateNeverBlindlyRefundsResources() {
    Fixture fixture = new Fixture(owned());
    fixture.repository.result = TerritoryBuffTransactionService.RepositoryResult.STATE_UNKNOWN;
    assertEquals(TerritoryManagementResult.STATE_UNKNOWN, fixture.execute(Action.UNLOCK));
    assertEquals(90, fixture.accounts.balance);
    assertEquals(7, fixture.resources.experience);
    assertEquals(0, fixture.resources.items.get("minecraft:diamond"));
  }

  @Test
  void preflightFailuresDoNotMutateAnything() {
    Fixture balance = new Fixture(owned());
    balance.accounts.balance = 9;
    assertEquals(TerritoryManagementResult.INSUFFICIENT_BALANCE, balance.execute(Action.UNLOCK));
    assertEquals(0, balance.repository.mutations.get());

    Fixture experience = new Fixture(owned());
    experience.resources.experience = 2;
    assertEquals(
        TerritoryManagementResult.INSUFFICIENT_EXPERIENCE,
        experience.execute(Action.UNLOCK));

    Fixture items = new Fixture(owned());
    items.resources.items.clear();
    assertEquals(TerritoryManagementResult.INSUFFICIENT_ITEMS, items.execute(Action.UNLOCK));
  }

  @Test
  void eachKnownFailureAfterDebitRunsReverseCompensation() {
    Fixture experienceDebit = new Fixture(owned());
    experienceDebit.resources.debitExperienceSucceeds = false;
    assertEquals(
        TerritoryManagementResult.INSUFFICIENT_EXPERIENCE,
        experienceDebit.execute(Action.UNLOCK));
    assertEquals(100, experienceDebit.accounts.balance);

    Fixture itemDebit = new Fixture(owned());
    itemDebit.resources.removeSucceeds = false;
    assertEquals(TerritoryManagementResult.INVENTORY_FAILED, itemDebit.execute(Action.UNLOCK));
    assertEquals(100, itemDebit.accounts.balance);
    assertEquals(10, itemDebit.resources.experience);

    Fixture rollback = new Fixture(owned());
    rollback.repository.result = TerritoryBuffTransactionService.RepositoryResult.PERSIST_FAILED;
    rollback.resources.rollbackSucceeds = false;
    assertEquals(TerritoryManagementResult.ROLLBACK_FAILED, rollback.execute(Action.UNLOCK));
  }

  @Test
  void invalidActionStateAndPreviewExceptionsFailClosed() {
    Fixture fixture = new Fixture(owned());
    assertEquals(TerritoryManagementResult.NOT_UNLOCKED, fixture.execute(Action.UPGRADE));

    Fixture accountException = new Fixture(owned());
    accountException.accounts.previewThrows = true;
    assertEquals(TerritoryManagementResult.BALANCE_FAILED, accountException.execute(Action.UNLOCK));
    assertEquals(1, accountException.reports.get());

    Fixture resourceException = new Fixture(owned());
    resourceException.resources.previewThrows = true;
    assertEquals(TerritoryManagementResult.INVENTORY_FAILED, resourceException.execute(Action.UNLOCK));
    assertEquals(1, resourceException.reports.get());
  }

  @Test
  void upgradeUsesLiveExpectedStateAndRejectsMaxLevel() {
    Owned unlocked = withBuffState(true, 1);
    Fixture fixture = new Fixture(unlocked);
    assertEquals(TerritoryManagementResult.SUCCESS, fixture.execute(Action.UPGRADE));
    assertTrue(fixture.repository.expectedUnlocked);
    assertEquals(1, fixture.repository.expectedLevel);
    assertEquals(
        TerritoryManagementResult.MAX_LEVEL,
        new Fixture(withBuffState(true, 3)).execute(Action.UPGRADE));
  }

  private static Owned withBuffState(boolean unlocked, int level) {
    Owned source = owned();
    Buff value = source.buffs().get(0);
    Buff changed = new Buff(
        value.id(),
        value.displayText(),
        value.effectId(),
        value.initialUnlocked(),
        value.initialLevel(),
        value.singleUpgradeLevel(),
        value.maxLevel(),
        unlocked,
        level,
        value.upgradeCosts());
    return new Owned(
        source.summary(),
        source.authorizedMembers(),
        source.backpoint(),
        source.rules(),
        List.of(changed));
  }

  private static final class Fixture {
    final TestRepository repository;
    final TestAccounts accounts = new TestAccounts();
    final TestResources resources = new TestResources();
    final AtomicInteger reports = new AtomicInteger();
    final TerritoryBuffTransactionService.Context context;

    Fixture(Owned territory) {
      repository = new TestRepository(territory);
      context = new TerritoryBuffTransactionService.Context(
          OWNER,
          "minecraft:overworld",
          repository,
          accounts,
          resources,
          (player, land, buff, stage, failure) -> reports.incrementAndGet());
    }

    TerritoryManagementResult execute(Action action) {
      return TerritoryBuffTransactionService.execute(
          TERRITORY, "economy_system:speed", action, context);
    }
  }

  private static final class TestRepository
      implements TerritoryBuffTransactionService.Repository {
    final Owned territory;
    final AtomicInteger mutations = new AtomicInteger();
    TerritoryBuffTransactionService.RepositoryResult result =
        TerritoryBuffTransactionService.RepositoryResult.SUCCESS;
    boolean expectedUnlocked;
    int expectedLevel;

    TestRepository(Owned territory) { this.territory = territory; }
    public Owned find(UUID id) { return territory; }
    public TerritoryBuffTransactionService.RepositoryResult mutate(
        UUID id,
        UUID owner,
        String buffId,
        boolean unlocked,
        int level,
        Action action) {
      mutations.incrementAndGet();
      expectedUnlocked = unlocked;
      expectedLevel = level;
      return result;
    }
  }

  private static final class TestAccounts implements TerritoryBuffTransactionService.Accounts {
    int balance = 100;
    boolean previewThrows;

    public BalanceMutationResult preview(int amount) {
      if (previewThrows) throw new IllegalStateException("preview");
      return balance >= amount
          ? BalanceMutationResult.SUCCESS
          : BalanceMutationResult.INSUFFICIENT_FUNDS;
    }
    public BalanceMutationResult debit(int amount) {
      if (balance < amount) return BalanceMutationResult.INSUFFICIENT_FUNDS;
      balance -= amount;
      return BalanceMutationResult.SUCCESS;
    }
    public BalanceMutationResult refund(int amount) {
      balance += amount;
      return BalanceMutationResult.SUCCESS;
    }
  }

  private static final class TestResources implements TerritoryBuffTransactionService.Resources {
    int experience = 10;
    final Map<String, Integer> items = new HashMap<>(Map.of("minecraft:diamond", 2));
    boolean debitExperienceSucceeds = true;
    boolean removeSucceeds = true;
    boolean rollbackSucceeds = true;
    boolean previewThrows;

    public int experienceLevel() {
      if (previewThrows) throw new IllegalStateException("resources");
      return experience;
    }
    public boolean canRemove(Map<String, Integer> required) {
      if (previewThrows) throw new IllegalStateException("resources");
      return required.entrySet().stream()
          .allMatch(entry -> items.getOrDefault(entry.getKey(), 0) >= entry.getValue());
    }
    public boolean debitExperience(int levels) {
      if (!debitExperienceSucceeds) return false;
      experience -= levels;
      return true;
    }
    public boolean refundExperience(int levels) {
      experience += levels;
      return true;
    }
    public TerritoryBuffTransactionService.ItemRemoval remove(Map<String, Integer> required) {
      if (!removeSucceeds) return TerritoryBuffTransactionService.ItemRemoval.failure(true);
      required.forEach((id, count) -> items.compute(id, (key, value) -> value - count));
      return TerritoryBuffTransactionService.ItemRemoval.success(() -> {
        if (!rollbackSucceeds) return false;
        required.forEach((id, count) -> items.merge(id, count, Integer::sum));
        return true;
      });
    }
  }
}

package com.mo.economy_system.common.territory;

import static com.mo.economy_system.common.territory.TerritoryManagementTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.TransferTerritoryOwnershipMessage;
import com.mo.economy_system.common.network.UpdateTerritoryPermissionMessage;
import com.mo.economy_system.common.network.UpdateTerritoryRuleMessage;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TerritoryAdministrationServiceTest {
  @Test
  void permissionResolvesNameOnServerAndRemovalNeedsNoName() {
    CapturingRepository repository = new CapturingRepository();
    var context = context(repository, id -> Optional.of("  ServerName  "), new AtomicInteger());
    assertEquals(
        TerritoryManagementResult.SUCCESS,
        TerritoryAdministrationService.permission(
            new UpdateTerritoryPermissionMessage(TERRITORY, UUID.randomUUID(), true),
            OWNER,
            context));
    assertEquals("ServerName", repository.targetName.get());

    repository.targetName.set("not-called");
    assertEquals(
        TerritoryManagementResult.SUCCESS,
        TerritoryAdministrationService.permission(
            new UpdateTerritoryPermissionMessage(TERRITORY, UUID.randomUUID(), false),
            OWNER,
            context(repository, id -> { throw new AssertionError("directory called"); }, new AtomicInteger())));
    assertNull(repository.targetName.get());
  }

  @Test
  void transferAndRuleUseAuthoritativeOwnerAndStableEnums() {
    CapturingRepository repository = new CapturingRepository();
    UUID target = UUID.randomUUID();
    var context = context(repository, id -> Optional.of("Target"), new AtomicInteger());
    assertEquals(
        TerritoryManagementResult.SUCCESS,
        TerritoryAdministrationService.transfer(
            new TransferTerritoryOwnershipMessage(TERRITORY, target), OWNER, context));
    assertEquals(target, repository.target.get());
    assertEquals("Target", repository.targetName.get());

    assertEquals(
        TerritoryManagementResult.SUCCESS,
        TerritoryAdministrationService.rule(
            new UpdateTerritoryRuleMessage(
                TERRITORY, RuleAction.OPEN_CONTAINER, RuleLevel.OWNER_ONLY),
            OWNER,
            context));
    assertEquals(RuleAction.OPEN_CONTAINER, repository.action.get());
    assertEquals(RuleLevel.OWNER_ONLY, repository.level.get());
  }

  @Test
  void selfNonOwnerAndInvalidDirectoryAreRejectedBeforeMutation() {
    CapturingRepository repository = new CapturingRepository();
    var context = context(repository, id -> Optional.empty(), new AtomicInteger());
    assertEquals(
        TerritoryManagementResult.SELF_TARGET,
        TerritoryAdministrationService.transfer(
            new TransferTerritoryOwnershipMessage(TERRITORY, OWNER), OWNER, context));
    assertEquals(
        TerritoryManagementResult.NOT_OWNER,
        TerritoryAdministrationService.rule(
            new UpdateTerritoryRuleMessage(
                TERRITORY, RuleAction.BREAK_BLOCK, RuleLevel.EVERYONE),
            MEMBER,
            context));
    assertEquals(
        TerritoryManagementResult.INVALID_TARGET,
        TerritoryAdministrationService.transfer(
            new TransferTerritoryOwnershipMessage(TERRITORY, UUID.randomUUID()), OWNER, context));
    assertEquals(0, repository.mutations.get());
  }

  @Test
  void lookupFailureIsUnknownNotNotFoundAndRepositoryFailuresAreMapped() {
    AtomicInteger reports = new AtomicInteger();
    CapturingRepository lookupFailure = new CapturingRepository() {
      public Owned find(UUID territoryId) { throw new IllegalStateException("storage"); }
    };
    assertEquals(
        TerritoryManagementResult.STATE_UNKNOWN,
        TerritoryAdministrationService.rule(
            new UpdateTerritoryRuleMessage(
                TERRITORY, RuleAction.USE_ITEM, RuleLevel.MEMBERS),
            OWNER,
            context(lookupFailure, id -> Optional.of("Target"), reports)));
    assertEquals(1, reports.get());

    CapturingRepository dirty = new CapturingRepository();
    dirty.result = TerritoryAdministrationService.RepositoryResult.PERSIST_FAILED;
    assertEquals(
        TerritoryManagementResult.PERSIST_FAILED,
        TerritoryAdministrationService.rule(
            new UpdateTerritoryRuleMessage(
                TERRITORY, RuleAction.USE_ITEM, RuleLevel.MEMBERS),
            OWNER,
            context(dirty, id -> Optional.of("Target"), reports)));
  }

  private static TerritoryAdministrationService.Context context(
      TerritoryAdministrationService.Repository repository,
      TerritoryAdministrationService.PlayerDirectory players,
      AtomicInteger reports) {
    return new TerritoryAdministrationService.Context(
        repository,
        players,
        (territory, sender, stage, failure) -> reports.incrementAndGet());
  }

  private static class CapturingRepository
      implements TerritoryAdministrationService.Repository {
    final AtomicInteger mutations = new AtomicInteger();
    final AtomicReference<UUID> target = new AtomicReference<>();
    final AtomicReference<String> targetName = new AtomicReference<>();
    final AtomicReference<RuleAction> action = new AtomicReference<>();
    final AtomicReference<RuleLevel> level = new AtomicReference<>();
    TerritoryAdministrationService.RepositoryResult result =
        TerritoryAdministrationService.RepositoryResult.SUCCESS;

    public Owned find(UUID territoryId) { return owned(); }

    public TerritoryAdministrationService.RepositoryResult setPermission(
        UUID territoryId, UUID expectedOwner, UUID targetId, String name, boolean allowed) {
      mutations.incrementAndGet();
      target.set(targetId);
      targetName.set(name);
      return result;
    }

    public TerritoryAdministrationService.RepositoryResult transfer(
        UUID territoryId, UUID expectedOwner, UUID targetId, String name) {
      mutations.incrementAndGet();
      target.set(targetId);
      targetName.set(name);
      return result;
    }

    public TerritoryAdministrationService.RepositoryResult setRule(
        UUID territoryId, UUID expectedOwner, RuleAction value, RuleLevel ruleLevel) {
      mutations.incrementAndGet();
      action.set(value);
      level.set(ruleLevel);
      return result;
    }
  }
}

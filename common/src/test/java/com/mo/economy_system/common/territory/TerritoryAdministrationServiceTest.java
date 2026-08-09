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
    UUID target = UUID.randomUUID();
    assertEquals(
        TerritoryManagementResult.SUCCESS,
        TerritoryAdministrationService.permission(
            new UpdateTerritoryPermissionMessage(TERRITORY, target, true),
            OWNER,
            context));
    assertEquals(
        "ServerName",
        repository.replacement.get().authorizedMembers().stream()
            .filter(member -> member.playerId().equals(target))
            .findFirst().orElseThrow().playerName());

    repository.replacement.set(null);
    assertEquals(
        TerritoryManagementResult.SUCCESS,
        TerritoryAdministrationService.permission(
            new UpdateTerritoryPermissionMessage(TERRITORY, target, false),
            OWNER,
            context(repository, id -> { throw new AssertionError("directory called"); }, new AtomicInteger())));
    assertFalse(repository.replacement.get().authorizedMembers().stream()
        .anyMatch(member -> member.playerId().equals(target)));
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
    assertEquals(target, repository.replacement.get().summary().ownerId());
    assertEquals("Target", repository.replacement.get().summary().ownerName());

    assertEquals(
        TerritoryManagementResult.SUCCESS,
        TerritoryAdministrationService.rule(
            new UpdateTerritoryRuleMessage(
                TERRITORY, RuleAction.OPEN_CONTAINER, RuleLevel.OWNER_ONLY),
            target,
            context));
    assertEquals(
        RuleLevel.OWNER_ONLY,
        repository.replacement.get().rules().stream()
            .filter(rule -> rule.action() == RuleAction.OPEN_CONTAINER)
            .findFirst().orElseThrow().level());
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
                TERRITORY, RuleAction.USE_ITEM, RuleLevel.EVERYONE),
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
    final AtomicReference<Owned> expected = new AtomicReference<>();
    final AtomicReference<Owned> replacement = new AtomicReference<>();
    final AtomicReference<Owned> current = new AtomicReference<>(owned());
    TerritoryAdministrationService.RepositoryResult result =
        TerritoryAdministrationService.RepositoryResult.SUCCESS;

    public Owned find(UUID territoryId) { return current.get(); }

    public TerritoryAdministrationService.RepositoryResult apply(Owned before, Owned after) {
      mutations.incrementAndGet();
      expected.set(before);
      replacement.set(after);
      current.set(after);
      return result;
    }
  }
}

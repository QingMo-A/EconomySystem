package com.mo.economy_system.core.territory_system;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.TransferTerritoryOwnershipMessage;
import com.mo.economy_system.common.network.UpdateTerritoryPermissionMessage;
import com.mo.economy_system.common.network.UpdateTerritoryRuleMessage;
import com.mo.economy_system.common.territory.TerritoryAdministrationService;
import com.mo.economy_system.common.territory.TerritoryBackpointService;
import com.mo.economy_system.common.territory.TerritoryBuffTransactionService;
import com.mo.economy_system.common.territory.TerritoryManagementResult;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TerritoryManagementManagerIntegrationTest {
  private final UUID owner = UUID.randomUUID();
  private final UUID target = UUID.randomUUID();
  private Territory territory;
  private FailingSavedData saved;

  @BeforeEach
  void setUp() throws Exception {
    territory = new Territory(
        UUID.randomUUID(),
        "Home",
        owner,
        "Owner",
        0,
        64,
        0,
        10,
        80,
        10,
        new BlockPos(1, 65, 1),
        Level.OVERWORLD);
    territory.addBuffs(new TerritoryBuff(
        "economy_system:speed",
        "Speed",
        "minecraft:speed",
        false,
        0,
        1,
        3,
        List.of()));
    install();
  }

  @Test
  void allManagementMutationsUpdateLiveObjectAndIndexes() throws Exception {
    assertEquals(
        TerritoryManagementResult.SUCCESS,
        permission(owner, target, true));
    assertTrue(territory.hasPermission(target));

    assertEquals(
        TerritoryManagementResult.SUCCESS,
        rule(owner, RuleAction.OPEN_CONTAINER, RuleLevel.OWNER_ONLY));
    assertEquals(
        TerritoryPermissionLevel.OWNER_ONLY,
        territory.getPermissionLevel(TerritoryPermissionAction.OPEN_CONTAINER));

    assertEquals(
        TerritoryBuffTransactionService.RepositoryResult.SUCCESS,
        TerritoryManager.mutateTerritoryBuffAuthoritatively(
            territory.getTerritoryID(),
            owner,
            "economy_system:speed",
            false,
            0,
            true,
            0));
    assertTrue(territory.getBuff("economy_system:speed").isUnlocked());

    assertEquals(
        TerritoryManagementResult.SUCCESS,
        transfer(owner, target));
    assertEquals(target, territory.getOwnerUUID());
    assertFalse(territory.hasPermission(target));
    assertTrue(territory.hasPermission(owner));
    Map<UUID, List<Territory>> owners = field("territoriesByOwner");
    assertFalse(owners.getOrDefault(owner, List.of()).contains(territory));
    assertEquals(List.of(territory), owners.get(target));
    assertSame(territory, saved.getTerritoryByID(territory.getTerritoryID()));
    assertEquals(1, TerritoryManager.quadTree.countIdentity(territory));
  }

  @Test
  void permissionDirtyFailureRestoresSnapshotAndAllowsRetry() {
    var before = TerritoryNetworkSnapshots.owned(territory);
    saved.failNextDirty(() -> assertTrue(territory.hasPermission(target)));
    assertEquals(
        TerritoryManagementResult.PERSIST_FAILED,
        permission(owner, target, true));
    assertEquals(before, TerritoryNetworkSnapshots.owned(territory));
    assertFalse(territory.hasPermission(target));
    assertEquals(
        TerritoryManagementResult.SUCCESS,
        permission(owner, target, true));
  }

  @Test
  void backpointDirtyFailureRestoresSnapshotAndAllowsRetry() {
    var before = TerritoryNetworkSnapshots.owned(territory);
    Position oldPoint = before.backpoint().orElseThrow();
    Position next = new Position(4, 70, 4);
    saved.failNextDirty();
    assertEquals(
        TerritoryBackpointService.RepositoryResult.PERSIST_FAILED,
        TerritoryManager.setTerritoryBackpointAuthoritatively(
            territory.getTerritoryID(), owner, before.backpoint(), next));
    assertEquals(before, TerritoryNetworkSnapshots.owned(territory));
    assertEquals(
        TerritoryBackpointService.RepositoryResult.UPDATED,
        TerritoryManager.setTerritoryBackpointAuthoritatively(
            territory.getTerritoryID(), owner, java.util.Optional.of(oldPoint), next));
    assertEquals(next, TerritoryNetworkSnapshots.owned(territory).backpoint().orElseThrow());
  }

  @Test
  void ruleAndBuffDirtyFailuresRestoreExactMutableState() {
    var before = TerritoryNetworkSnapshots.owned(territory);
    saved.failNextDirty();
    assertEquals(
        TerritoryManagementResult.PERSIST_FAILED,
        rule(owner, RuleAction.PLACE_BLOCK, RuleLevel.OWNER_ONLY));
    assertEquals(before, TerritoryNetworkSnapshots.owned(territory));

    saved.failNextDirty();
    assertEquals(
        TerritoryBuffTransactionService.RepositoryResult.PERSIST_FAILED,
        TerritoryManager.mutateTerritoryBuffAuthoritatively(
            territory.getTerritoryID(),
            owner,
            "economy_system:speed",
            false,
            0,
            true,
            0));
    assertEquals(before, TerritoryNetworkSnapshots.owned(territory));
  }

  @Test
  void transferDirtyFailureRestoresOwnerBucketOrderAndMembers() throws Exception {
    Territory sibling = new Territory(
        UUID.randomUUID(),
        "Sibling",
        owner,
        "Owner",
        20,
        64,
        20,
        25,
        64,
        25,
        new BlockPos(20, 64, 20),
        Level.OVERWORLD);
    Map<UUID, Territory> primary = field("territoryByID");
    Map<UUID, List<Territory>> owners = field("territoriesByOwner");
    primary.put(sibling.getTerritoryID(), sibling);
    owners.get(owner).add(sibling);
    saved.addTerritory(sibling);
    TerritoryManager.quadTree.insert(sibling);
    List<Territory> beforeOrder = List.copyOf(owners.get(owner));
    var before = TerritoryNetworkSnapshots.owned(territory);

    saved.failNextDirty();
    assertEquals(
        TerritoryManagementResult.PERSIST_FAILED,
        transfer(owner, target));
    assertEquals(before, TerritoryNetworkSnapshots.owned(territory));
    assertEquals(beforeOrder, owners.get(owner));
    assertFalse(owners.containsKey(target));
    assertSame(territory, saved.getTerritoryByID(territory.getTerritoryID()));
  }

  @Test
  void staleOwnerAndExpectedBuffStateFailBeforeMutation() {
    var before = TerritoryNetworkSnapshots.owned(territory);
    assertEquals(
        TerritoryManagementResult.NOT_OWNER,
        rule(UUID.randomUUID(), RuleAction.USE_ITEM, RuleLevel.EVERYONE));
    assertEquals(
        TerritoryBuffTransactionService.RepositoryResult.BUFF_CHANGED,
        TerritoryManager.mutateTerritoryBuffAuthoritatively(
            territory.getTerritoryID(),
            owner,
            "economy_system:speed",
            true,
            0,
            true,
            1));
    assertEquals(before, TerritoryNetworkSnapshots.owned(territory));
  }

  private TerritoryManagementResult permission(UUID sender, UUID targetId, boolean allowed) {
    return TerritoryAdministrationService.permission(
        new UpdateTerritoryPermissionMessage(territory.getTerritoryID(), targetId, allowed),
        sender,
        administrationContext());
  }

  private TerritoryManagementResult transfer(UUID sender, UUID targetId) {
    return TerritoryAdministrationService.transfer(
        new TransferTerritoryOwnershipMessage(territory.getTerritoryID(), targetId),
        sender,
        administrationContext());
  }

  private TerritoryManagementResult rule(
      UUID sender, RuleAction action, RuleLevel level) {
    return TerritoryAdministrationService.rule(
        new UpdateTerritoryRuleMessage(territory.getTerritoryID(), action, level),
        sender,
        administrationContext());
  }

  private TerritoryAdministrationService.Context administrationContext() {
    return new TerritoryAdministrationService.Context(
        new TerritoryAdministrationService.Repository() {
          @Override
          public com.mo.economy_system.common.territory.TerritorySnapshots.Owned find(UUID id) {
            Territory current = TerritoryManager.getTerritoryByID(id);
            return current == null ? null : TerritoryNetworkSnapshots.owned(current);
          }

          @Override
          public TerritoryAdministrationService.RepositoryResult apply(
              com.mo.economy_system.common.territory.TerritorySnapshots.Owned expected,
              com.mo.economy_system.common.territory.TerritorySnapshots.Owned replacement) {
            return TerritoryManager.applyTerritoryAdministrationAuthoritatively(expected, replacement);
          }
        },
        id -> java.util.Optional.of("Target"),
        TerritoryAdministrationService.FailureSink.noop());
  }

  private void install() throws Exception {
    Map<UUID, Territory> primary = field("territoryByID");
    Map<UUID, List<Territory>> owners = field("territoriesByOwner");
    primary.clear();
    owners.clear();
    primary.put(territory.getTerritoryID(), territory);
    owners.put(owner, new ArrayList<>(List.of(territory)));
    saved = new FailingSavedData();
    saved.addTerritory(territory);
    Field savedField = TerritoryManager.class.getDeclaredField("savedData");
    savedField.setAccessible(true);
    savedField.set(null, saved);
    TerritoryManager.quadTree = new QuadTree(0, new Bounds(-1_000, -1_000, 2_000, 2_000));
    TerritoryManager.quadTree.insert(territory);
  }

  @SuppressWarnings("unchecked")
  private static <T> T field(String name) throws Exception {
    Field field = TerritoryManager.class.getDeclaredField(name);
    field.setAccessible(true);
    return (T) field.get(null);
  }

  private static final class FailingSavedData extends TerritorySavedData {
    private int failures;
    private Runnable beforeFailure = () -> {};

    void failNextDirty() {
      failNextDirty(() -> {});
    }

    void failNextDirty(Runnable action) {
      failures = 1;
      beforeFailure = action;
    }

    @Override
    public void setDirty() {
      if (failures > 0) {
        failures--;
        beforeFailure.run();
        throw new IllegalStateException("dirty");
      }
      super.setDirty();
    }
  }
}

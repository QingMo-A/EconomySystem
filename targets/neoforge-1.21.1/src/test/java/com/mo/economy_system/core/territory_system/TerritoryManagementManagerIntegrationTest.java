package com.mo.economy_system.core.territory_system;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.territory.TerritoryAdministrationService;
import com.mo.economy_system.common.territory.TerritoryBuffTransactionService;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
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
        TerritoryAdministrationService.RepositoryResult.SUCCESS,
        TerritoryManager.setTerritoryPermissionAuthoritatively(
            territory.getTerritoryID(), owner, target, "Target", true));
    assertTrue(territory.hasPermission(target));

    assertEquals(
        TerritoryAdministrationService.RepositoryResult.SUCCESS,
        TerritoryManager.setTerritoryRuleAuthoritatively(
            territory.getTerritoryID(), owner, RuleAction.OPEN_CONTAINER, RuleLevel.OWNER_ONLY));
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
            TerritoryBuffTransactionService.Action.UNLOCK));
    assertTrue(territory.getBuff("economy_system:speed").isUnlocked());

    assertEquals(
        TerritoryAdministrationService.RepositoryResult.SUCCESS,
        TerritoryManager.transferTerritoryAuthoritatively(
            territory.getTerritoryID(), owner, target, "Target"));
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
        TerritoryAdministrationService.RepositoryResult.PERSIST_FAILED,
        TerritoryManager.setTerritoryPermissionAuthoritatively(
            territory.getTerritoryID(), owner, target, "Target", true));
    assertEquals(before, TerritoryNetworkSnapshots.owned(territory));
    assertFalse(territory.hasPermission(target));
    assertEquals(
        TerritoryAdministrationService.RepositoryResult.SUCCESS,
        TerritoryManager.setTerritoryPermissionAuthoritatively(
            territory.getTerritoryID(), owner, target, "Target", true));
  }

  @Test
  void ruleAndBuffDirtyFailuresRestoreExactMutableState() {
    var before = TerritoryNetworkSnapshots.owned(territory);
    saved.failNextDirty();
    assertEquals(
        TerritoryAdministrationService.RepositoryResult.PERSIST_FAILED,
        TerritoryManager.setTerritoryRuleAuthoritatively(
            territory.getTerritoryID(), owner, RuleAction.PLACE_BLOCK, RuleLevel.OWNER_ONLY));
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
            TerritoryBuffTransactionService.Action.UNLOCK));
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
        TerritoryAdministrationService.RepositoryResult.PERSIST_FAILED,
        TerritoryManager.transferTerritoryAuthoritatively(
            territory.getTerritoryID(), owner, target, "Target"));
    assertEquals(before, TerritoryNetworkSnapshots.owned(territory));
    assertEquals(beforeOrder, owners.get(owner));
    assertFalse(owners.containsKey(target));
    assertSame(territory, saved.getTerritoryByID(territory.getTerritoryID()));
  }

  @Test
  void staleOwnerAndExpectedBuffStateFailBeforeMutation() {
    var before = TerritoryNetworkSnapshots.owned(territory);
    assertEquals(
        TerritoryAdministrationService.RepositoryResult.OWNER_CHANGED,
        TerritoryManager.setTerritoryRuleAuthoritatively(
            territory.getTerritoryID(), UUID.randomUUID(), RuleAction.USE_ITEM, RuleLevel.EVERYONE));
    assertEquals(
        TerritoryBuffTransactionService.RepositoryResult.BUFF_CHANGED,
        TerritoryManager.mutateTerritoryBuffAuthoritatively(
            territory.getTerritoryID(),
            owner,
            "economy_system:speed",
            true,
            0,
            TerritoryBuffTransactionService.Action.UPGRADE));
    assertEquals(before, TerritoryNetworkSnapshots.owned(territory));
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

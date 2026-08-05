package com.mo.economy_system.core.territory_system;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.territory.TerritoryMemberRemovalService;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TerritoryMemberRemovalManagerIntegrationTest {
  private final UUID owner = UUID.randomUUID();
  private final UUID target = UUID.randomUUID();
  private final UUID other = UUID.randomUUID();
  private Territory territory;

  @BeforeEach
  void setUp() throws Exception {
    territory =
        new Territory(
            UUID.randomUUID(),
            "land",
            owner,
            "owner",
            0,
            64,
            0,
            9,
            64,
            9,
            new BlockPos(0, 64, 0),
            Level.OVERWORLD);
    territory.addAuthorizedPlayerIfAbsent(target, "target");
    territory.addAuthorizedPlayerIfAbsent(other, "other");
    install();
  }

  @Test
  void successRemovesOnlyTargetAndPreservesEverySpatialIndex() {
    var outcome = remove(owner, target);
    assertEquals(TerritoryMemberRemovalService.RepositoryResult.REMOVED, outcome.result());
    assertEquals("target", outcome.removedMember().targetPlayerName());
    assertFalse(territory.hasPermission(target));
    assertTrue(territory.hasPermission(other));
    assertEquals(1, TerritoryManager.quadTree.countIdentity(territory));
    assertEquals(1, TerritoryManager.quadTree.countTerritory(territory.getTerritoryID()));
    assertTrue(TerritoryManager.quadTree.isIndexedCorrectly(territory));
  }

  @Test
  void expectedBusinessFailuresDoNotMutateMembers() {
    assertEquals(
        TerritoryMemberRemovalService.RepositoryResult.OWNER_MISMATCH,
        remove(UUID.randomUUID(), target).result());
    assertEquals(
        TerritoryMemberRemovalService.RepositoryResult.OWNER_TARGET, remove(owner, owner).result());
    assertEquals(
        TerritoryMemberRemovalService.RepositoryResult.TARGET_NOT_MEMBER,
        remove(owner, UUID.randomUUID()).result());
    assertTrue(territory.hasPermission(target));
    assertTrue(territory.hasPermission(other));
  }

  @Test
  void missingOwnerBucketFailsClosedBeforeMutation() throws Exception {
    Map<UUID, List<Territory>> owners = field("territoriesByOwner");
    owners.clear();
    assertIntegrity(remove(owner, target));
    assertTrue(territory.hasPermission(target));
  }

  @Test
  void duplicateOwnerBucketFailsClosedBeforeMutation() throws Exception {
    Map<UUID, List<Territory>> owners = field("territoriesByOwner");
    owners.put(owner, new ArrayList<>(List.of(territory, territory)));
    assertIntegrity(remove(owner, target));
    assertTrue(territory.hasPermission(target));
  }

  @Test
  void duplicateQuadTreeIdentityOrUuidFailsClosed() {
    TerritoryManager.quadTree.insert(territory);
    assertIntegrity(remove(owner, target));
    assertTrue(territory.hasPermission(target));
  }

  private TerritoryMemberRemovalService.RepositoryOutcome remove(UUID expectedOwner, UUID player) {
    return TerritoryManager.removeTerritoryMemberAuthoritatively(
        territory.getTerritoryID(), expectedOwner, player);
  }

  private static void assertIntegrity(TerritoryMemberRemovalService.RepositoryOutcome outcome) {
    assertEquals(TerritoryMemberRemovalService.RepositoryResult.STATE_UNKNOWN, outcome.result());
    assertEquals(
        TerritoryMemberRemovalService.RepositoryFailureKind.INTEGRITY, outcome.failureKind());
  }

  private void install() throws Exception {
    Map<UUID, Territory> primary = field("territoryByID");
    Map<UUID, List<Territory>> owners = field("territoriesByOwner");
    primary.clear();
    owners.clear();
    primary.put(territory.getTerritoryID(), territory);
    owners.put(owner, new ArrayList<>(List.of(territory)));
    TerritorySavedData saved = new TerritorySavedData();
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
}

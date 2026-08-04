package com.mo.economy_system.core.territory_system;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TerritoryResizeManagerIntegrationTest {
  private final UUID owner = UUID.randomUUID();
  private Territory territory;

  @BeforeEach
  void setUp() throws Exception {
    territory = territory(owner, 0, 0, 9, 9);
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

  @Test
  void prepareComputesPriceFromLiveBounds() {
    var outcome = prepare(new BlockPos(0, 64, 0), new BlockPos(19, 64, 9));
    assertEquals(TerritoryManager.ResizePrepareResult.READY, outcome.result());
    assertEquals(100, outcome.plan().oldArea());
    assertEquals(200, outcome.plan().newArea());
    assertEquals(2_000, outcome.plan().charge());
  }

  @Test
  void equalAreaReshapeIsReadyAndFree() {
    var outcome = prepare(new BlockPos(20, 64, 20), new BlockPos(29, 64, 29));
    assertEquals(TerritoryManager.ResizePrepareResult.READY, outcome.result());
    assertEquals(0, outcome.plan().charge());
    assertEquals(0, outcome.plan().areaDifference());
  }

  @Test
  void identicalBoundsAndBackpointAreUnchanged() {
    var outcome = prepare(territory.getPos1(), territory.getPos2());
    assertEquals(TerritoryManager.ResizePrepareResult.UNCHANGED, outcome.result());
  }

  @Test
  void commitRejectsChangedOldStateBeforeMutation() {
    var prepared = prepare(new BlockPos(20, 64, 20), new BlockPos(29, 64, 29));
    territory.setBackpoint(new BlockPos(1, 64, 1));
    assertEquals(
        TerritoryManager.ResizeResult.CHANGED,
        TerritoryManager.commitTerritoryResize(prepared.plan()).result());
    assertEquals(new BlockPos(0, 64, 0), territory.getPos1());
  }

  @Test
  void prepareRejectsOverlapWithAnotherTerritory() throws Exception {
    Territory other = territory(UUID.randomUUID(), 20, 20, 30, 30);
    Map<UUID, Territory> primary = field("territoryByID");
    Map<UUID, List<Territory>> owners = field("territoriesByOwner");
    primary.put(other.getTerritoryID(), other);
    owners.put(other.getOwnerUUID(), new ArrayList<>(List.of(other)));
    ((TerritorySavedData) savedData()).addTerritory(other);
    TerritoryManager.quadTree.insert(other);
    assertEquals(
        TerritoryManager.ResizePrepareResult.OVERLAP,
        prepare(new BlockPos(25, 64, 25), new BlockPos(35, 64, 35)).result());
  }

  @Test
  void commitReindexesNewSpaceAndRemovesOldPath() {
    var prepared = prepare(new BlockPos(20, 64, 20), new BlockPos(29, 64, 29));
    assertEquals(
        TerritoryManager.ResizeResult.RESIZED,
        TerritoryManager.commitTerritoryResize(prepared.plan()).result());
    assertTrue(TerritoryManager.quadTree.isIndexedCorrectly(territory));
    assertTrue(TerritoryManager.quadTree.query(25, 25).contains(territory));
    assertFalse(TerritoryManager.quadTree.query(5, 5).contains(territory));
  }

  private TerritoryManager.ResizePrepareOutcome prepare(BlockPos first, BlockPos second) {
    return TerritoryManager.prepareTerritoryResize(
        territory.getTerritoryID(), owner, first, second, first);
  }

  @SuppressWarnings("unchecked")
  private static <T> T field(String name) throws Exception {
    Field field = TerritoryManager.class.getDeclaredField(name);
    field.setAccessible(true);
    return (T) field.get(null);
  }

  private static Object savedData() throws Exception {
    Field field = TerritoryManager.class.getDeclaredField("savedData");
    field.setAccessible(true);
    return field.get(null);
  }

  private static Territory territory(UUID owner, int x1, int z1, int x2, int z2) {
    return new Territory(
        UUID.randomUUID(),
        "test",
        owner,
        "owner",
        x1,
        64,
        z1,
        x2,
        64,
        z2,
        new BlockPos(x1, 64, z1),
        Level.OVERWORLD);
  }
}

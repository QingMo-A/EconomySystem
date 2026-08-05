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

  @Test
  void resizesRectangleToSingleCellAndBack() {
    assertEquals(
        TerritoryManager.ResizeResult.RESIZED,
        TerritoryManager.commitTerritoryResize(
                prepare(new BlockPos(50, 64, 50), new BlockPos(50, 64, 50)).plan())
            .result());
    assertTrue(TerritoryManager.quadTree.query(50, 50).contains(territory));
    assertTrue(TerritoryManager.quadTree.isIndexedCorrectly(territory));

    var expanded = prepare(new BlockPos(60, 64, 60), new BlockPos(69, 64, 69));
    assertEquals(
        TerritoryManager.ResizeResult.RESIZED,
        TerritoryManager.commitTerritoryResize(expanded.plan()).result());
    assertFalse(TerritoryManager.quadTree.query(50, 50).contains(territory));
    assertTrue(TerritoryManager.quadTree.query(60, 60).contains(territory));
    assertTrue(TerritoryManager.quadTree.query(69, 69).contains(territory));
  }

  @Test
  void resizesSingleColumnToSingleRow() {
    var column = prepare(new BlockPos(20, 64, 20), new BlockPos(20, 64, 29));
    assertEquals(
        TerritoryManager.ResizeResult.RESIZED,
        TerritoryManager.commitTerritoryResize(column.plan()).result());
    var row = prepare(new BlockPos(30, 64, 30), new BlockPos(39, 64, 30));
    assertEquals(
        TerritoryManager.ResizeResult.RESIZED,
        TerritoryManager.commitTerritoryResize(row.plan()).result());
    assertTrue(TerritoryManager.quadTree.query(30, 30).contains(territory));
    assertTrue(TerritoryManager.quadTree.query(39, 30).contains(territory));
    assertFalse(TerritoryManager.quadTree.query(20, 29).contains(territory));
  }

  @Test
  void persistenceFailureRestoresOldSpatialPath() throws Exception {
    FailingSavedData failing = new FailingSavedData();
    failing.addTerritory(territory);
    setSavedData(failing);
    failing.failNextDirty();

    var prepared = prepare(new BlockPos(20, 64, 20), new BlockPos(29, 64, 29));
    var outcome = TerritoryManager.commitTerritoryResize(prepared.plan());

    assertEquals(TerritoryManager.ResizeResult.PERSIST_FAILED, outcome.result());
    assertTrue(TerritoryManager.quadTree.isIndexedCorrectly(territory));
    assertTrue(TerritoryManager.quadTree.query(0, 0).contains(territory));
    assertTrue(TerritoryManager.quadTree.query(9, 9).contains(territory));
    assertFalse(TerritoryManager.quadTree.query(20, 20).contains(territory));
  }

  @Test
  void differentIdentityWithSameUuidMakesCompensationStateUnknown() throws Exception {
    Territory duplicate =
        territory(territory.getTerritoryID(), UUID.randomUUID(), 100, 100, 100, 100);
    FailingSavedData failing = new FailingSavedData();
    failing.addTerritory(territory);
    setSavedData(failing);
    var prepared = prepare(new BlockPos(20, 64, 20), new BlockPos(29, 64, 29));
    failing.failNextDirty(() -> TerritoryManager.quadTree.insert(duplicate));

    var outcome = TerritoryManager.commitTerritoryResize(prepared.plan());

    assertEquals(TerritoryManager.ResizeResult.STATE_UNKNOWN, outcome.result());
    assertEquals(1, TerritoryManager.quadTree.countIdentity(territory));
    assertEquals(1, TerritoryManager.quadTree.countIdentity(duplicate));
    assertEquals(2, TerritoryManager.quadTree.countTerritory(territory.getTerritoryID()));
  }

  @Test
  void removalOfSingleCellVerifiesSpatialAbsence() throws Exception {
    territory = territory(owner, 7, 7, 7, 7);
    install(territory, new TerritorySavedData());

    var outcome =
        TerritoryManager.removeTerritoryAuthoritatively(territory.getTerritoryID(), owner);

    assertEquals(
        com.mo.economy_system.common.territory.TerritoryRemovalService.RepositoryResult.REMOVED,
        outcome.result());
    assertEquals(0, TerritoryManager.quadTree.countIdentity(territory));
    assertFalse(TerritoryManager.quadTree.query(7, 7).contains(territory));
  }

  @Test
  void removalDirtyFailureRestoresSingleCellSpatialPath() throws Exception {
    territory = territory(owner, 7, 7, 7, 7);
    FailingSavedData failing = new FailingSavedData();
    install(territory, failing);
    failing.failNextDirty();

    var outcome =
        TerritoryManager.removeTerritoryAuthoritatively(territory.getTerritoryID(), owner);

    assertEquals(
        com.mo.economy_system.common.territory.TerritoryRemovalService.RepositoryResult
            .PERSIST_FAILED,
        outcome.result());
    assertEquals(1, TerritoryManager.quadTree.countIdentity(territory));
    assertTrue(TerritoryManager.quadTree.isIndexedCorrectly(territory));
    assertTrue(TerritoryManager.quadTree.query(7, 7).contains(territory));
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

  private static void setSavedData(TerritorySavedData saved) throws Exception {
    Field field = TerritoryManager.class.getDeclaredField("savedData");
    field.setAccessible(true);
    field.set(null, saved);
  }

  private static void install(Territory value, TerritorySavedData saved) throws Exception {
    Map<UUID, Territory> primary = field("territoryByID");
    Map<UUID, List<Territory>> owners = field("territoriesByOwner");
    primary.clear();
    owners.clear();
    primary.put(value.getTerritoryID(), value);
    owners.put(value.getOwnerUUID(), new ArrayList<>(List.of(value)));
    saved.addTerritory(value);
    setSavedData(saved);
    TerritoryManager.quadTree = new QuadTree(0, new Bounds(-1_000, -1_000, 2_000, 2_000));
    TerritoryManager.quadTree.insert(value);
  }

  private static Territory territory(UUID owner, int x1, int z1, int x2, int z2) {
    return territory(UUID.randomUUID(), owner, x1, z1, x2, z2);
  }

  private static Territory territory(UUID territoryId, UUID owner, int x1, int z1, int x2, int z2) {
    return new Territory(
        territoryId,
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
        throw new IllegalStateException("injected dirty failure");
      }
      super.setDirty();
    }
  }
}

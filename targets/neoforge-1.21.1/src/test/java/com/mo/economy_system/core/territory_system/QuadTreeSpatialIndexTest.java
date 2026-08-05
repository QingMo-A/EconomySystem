package com.mo.economy_system.core.territory_system;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

class QuadTreeSpatialIndexTest {
  @Test
  void insertedTerritoryIsOnExpectedQueryPath() {
    QuadTree tree = populatedTree();
    Territory target = territory(10, 10, 20, 20);
    tree.insert(target);
    assertTrue(tree.isIndexedCorrectly(target));
    assertSame(target, tree.queryExact(10, 10));
    assertSame(target, tree.queryExact(20, 20));
  }

  @Test
  void coordinateMutationWithoutReindexIsDetected() {
    QuadTree tree = populatedTree();
    Territory target = territory(10, 10, 20, 20);
    tree.insert(target);
    move(target, 700, 700, 710, 710);
    assertEquals(1, tree.countTerritory(target.getTerritoryID()));
    assertFalse(tree.isIndexedCorrectly(target));
    assertFalse(tree.query(705, 705).contains(target));
  }

  @Test
  void equalAreaReshapeReindexesSpatially() {
    QuadTree tree = populatedTree();
    Territory target = territory(10, 10, 20, 20);
    tree.insert(target);
    assertTrue(tree.remove(target));
    move(target, 700, 700, 710, 710);
    tree.insert(target);
    assertTrue(tree.isIndexedCorrectly(target));
    assertTrue(tree.query(705, 705).contains(target));
    assertFalse(tree.query(15, 15).contains(target));
  }

  @Test
  void rollbackRestoresOldSpatialPath() {
    QuadTree tree = populatedTree();
    Territory target = territory(10, 10, 20, 20);
    tree.insert(target);
    tree.remove(target);
    move(target, 700, 700, 710, 710);
    tree.insert(target);
    tree.remove(target);
    move(target, 10, 10, 20, 20);
    tree.insert(target);
    assertTrue(tree.isIndexedCorrectly(target));
    assertTrue(tree.query(15, 15).contains(target));
    assertFalse(tree.query(705, 705).contains(target));
  }

  @Test
  void duplicateIdentityIsNeverReportedAsCorrect() {
    QuadTree tree = populatedTree();
    Territory target = territory(10, 10, 20, 20);
    tree.insert(target);
    tree.insert(target);
    assertEquals(2, tree.countTerritory(target.getTerritoryID()));
    assertFalse(tree.isIndexedCorrectly(target));
  }

  @Test
  void singleCellCanBeInsertedQueriedAndRemoved() {
    QuadTree tree = new QuadTree(0, new Bounds(7, -3, 0, 0));
    Territory cell = territory(7, -3, 7, -3);

    tree.insert(cell);

    assertTrue(tree.isIndexedCorrectly(cell));
    assertSame(cell, tree.queryExact(7, -3));
    assertTrue(tree.query(7, -3).contains(cell));
    assertTrue(tree.remove(cell));
    assertEquals(0, tree.countIdentity(cell));
    assertEquals(0, tree.countTerritory(cell.getTerritoryID()));
    assertTrue(tree.query(7, -3).isEmpty());
    assertFalse(tree.remove(cell));
  }

  @Test
  void singleColumnTerritoryIsQueryableAtBothEndpoints() {
    QuadTree tree = new QuadTree(0, new Bounds(0, 0, 10, 10));
    Territory column = territory(5, 1, 5, 9);
    tree.insert(column);

    assertTrue(tree.isIndexedCorrectly(column));
    assertTrue(tree.query(5, 1).contains(column));
    assertTrue(tree.query(5, 9).contains(column));
    assertFalse(tree.query(4, 5).contains(column));
    assertFalse(tree.query(6, 5).contains(column));
    assertTrue(tree.remove(column));
    assertTrue(tree.query(5, 5).isEmpty());
  }

  @Test
  void singleRowTerritoryIsQueryableAtBothEndpoints() {
    QuadTree tree = new QuadTree(0, new Bounds(0, 0, 10, 10));
    Territory row = territory(1, 5, 9, 5);
    tree.insert(row);

    assertTrue(tree.isIndexedCorrectly(row));
    assertTrue(tree.query(1, 5).contains(row));
    assertTrue(tree.query(9, 5).contains(row));
    assertFalse(tree.query(5, 4).contains(row));
    assertFalse(tree.query(5, 6).contains(row));
    assertTrue(tree.remove(row));
    assertTrue(tree.query(5, 5).isEmpty());
  }

  @Test
  void rootMinimumAndMaximumBoundariesRemainQueryable() {
    QuadTree tree = new QuadTree(0, new Bounds(0, 0, 10, 10));
    Territory minimum = territory(0, 0, 0, 0);
    Territory maximum = territory(10, 10, 10, 10);
    tree.insert(minimum);
    tree.insert(maximum);

    assertTrue(tree.isIndexedCorrectly(minimum));
    assertTrue(tree.isIndexedCorrectly(maximum));
    assertSame(minimum, tree.queryExact(0, 0));
    assertSame(maximum, tree.queryExact(10, 10));
    assertTrue(tree.remove(minimum));
    assertTrue(tree.remove(maximum));
    assertTrue(tree.query(0, 0).isEmpty());
    assertTrue(tree.query(10, 10).isEmpty());
  }

  @Test
  void firstLevelXSplitLineUsesOneDeterministicPath() {
    QuadTree tree = splitRootWithoutSplittingChildren();
    Territory onLine = territory(5, 1, 5, 1);
    Territory crossingLine = territory(5, 1, 6, 1);
    tree.insert(onLine);
    tree.insert(crossingLine);

    assertTrue(tree.isIndexedCorrectly(onLine));
    assertTrue(tree.isIndexedCorrectly(crossingLine));
    assertEquals(1, tree.countIdentity(onLine));
    assertEquals(1, tree.countIdentity(crossingLine));
    assertTrue(tree.query(5, 1).contains(onLine));
    assertTrue(tree.query(5, 1).contains(crossingLine));
    assertTrue(tree.query(6, 1).contains(crossingLine));
    assertFalse(tree.query(6, 1).contains(onLine));
  }

  @Test
  void firstLevelZSplitLineUsesOneDeterministicPath() {
    QuadTree tree = splitRootWithoutSplittingChildren();
    Territory onLine = territory(1, 5, 1, 5);
    Territory crossingLine = territory(1, 5, 1, 6);
    tree.insert(onLine);
    tree.insert(crossingLine);

    assertTrue(tree.isIndexedCorrectly(onLine));
    assertTrue(tree.isIndexedCorrectly(crossingLine));
    assertEquals(1, tree.countIdentity(onLine));
    assertEquals(1, tree.countIdentity(crossingLine));
    assertTrue(tree.query(1, 5).contains(onLine));
    assertTrue(tree.query(1, 5).contains(crossingLine));
    assertTrue(tree.query(1, 6).contains(crossingLine));
    assertFalse(tree.query(1, 6).contains(onLine));
  }

  @Test
  void xAndZSplitPointEntersTheExpectedNorthWestChild() {
    QuadTree tree = splitRootWithoutSplittingChildren();
    Territory splitPoint = territory(5, 5, 5, 5);
    tree.insert(splitPoint);

    assertTrue(tree.isIndexedCorrectly(splitPoint));
    assertEquals(1, tree.countIdentity(splitPoint));
    assertTrue(tree.query(5, 5).contains(splitPoint));
  }

  @Test
  void deepNodeSplitLineRemainsAtTheParentNode() {
    QuadTree tree = new QuadTree(0, new Bounds(0, 0, 10, 10));
    for (Territory seed :
        List.of(
            territory(0, 0, 0, 0),
            territory(4, 0, 4, 0),
            territory(0, 4, 0, 4),
            territory(4, 4, 4, 4),
            territory(2, 2, 2, 2))) {
      tree.insert(seed);
    }

    Territory deepCrossingLine = territory(2, 1, 3, 1);
    tree.insert(deepCrossingLine);

    assertTrue(tree.isIndexedCorrectly(deepCrossingLine));
    assertEquals(1, tree.countIdentity(deepCrossingLine));
    assertTrue(tree.query(2, 1).contains(deepCrossingLine));
    assertTrue(tree.query(3, 1).contains(deepCrossingLine));
  }

  @Test
  void entriesInsertedBeforeRootSplitSurviveMigration() {
    QuadTree tree = new QuadTree(0, new Bounds(0, 0, 10, 10));
    List<Territory> entries =
        List.of(
            territory(1, 1, 1, 1),
            territory(8, 1, 8, 1),
            territory(1, 8, 1, 8),
            territory(8, 8, 8, 8),
            territory(4, 4, 6, 6));

    for (Territory entry : entries) tree.insert(entry);

    for (Territory entry : entries) {
      Bounds bounds = entry.getBounds();
      assertTrue(tree.isIndexedCorrectly(entry));
      assertEquals(1, tree.countIdentity(entry));
      assertTrue(tree.query(bounds.x, bounds.z).contains(entry));
      assertTrue(tree.query(bounds.x + bounds.width, bounds.z + bounds.height).contains(entry));
    }
  }

  @Test
  void expandingRootKeepsOldAndNewIdentitiesIndexedOnce() {
    QuadTree tree = new QuadTree(0, new Bounds(0, 0, 10, 10));
    Territory oldEntry = territory(1, 1, 2, 2);
    Territory newEntry = territory(20, 20, 22, 22);
    tree.insert(oldEntry);
    tree.insert(newEntry);

    assertEquals(0, tree.getBounds().x);
    assertEquals(0, tree.getBounds().z);
    assertEquals(22, tree.getBounds().width);
    assertEquals(22, tree.getBounds().height);
    assertTrue(tree.isIndexedCorrectly(oldEntry));
    assertTrue(tree.isIndexedCorrectly(newEntry));
    assertEquals(1, tree.countIdentity(oldEntry));
    assertEquals(1, tree.countIdentity(newEntry));
    assertTrue(tree.query(1, 1).contains(oldEntry));
    assertTrue(tree.query(22, 22).contains(newEntry));
  }

  @Test
  void identityRemoveFindsStaleEntryAfterCoordinateMutation() {
    QuadTree tree = populatedTree();
    Territory target = territory(10, 10, 20, 20);
    tree.insert(target);
    move(target, 700, 700, 710, 710);

    assertEquals(1, tree.countIdentity(target));
    assertFalse(tree.isIndexedCorrectly(target));
    assertFalse(tree.query(15, 15).contains(target));
    assertTrue(tree.remove(target));
    assertEquals(0, tree.countIdentity(target));
    assertEquals(0, tree.countTerritory(target.getTerritoryID()));
    assertFalse(tree.remove(target));
  }

  @Test
  void duplicateIdentityRemovalRemovesExactlyOneEntryPerCall() {
    QuadTree tree = populatedTree();
    Territory target = territory(10, 10, 20, 20);
    tree.insert(target);
    tree.insert(target);

    assertEquals(2, tree.countIdentity(target));
    assertTrue(tree.remove(target));
    assertEquals(1, tree.countIdentity(target));
    assertEquals(1, tree.countTerritory(target.getTerritoryID()));
    assertTrue(tree.isIndexedCorrectly(target));
    assertTrue(tree.remove(target));
    assertEquals(0, tree.countIdentity(target));
    assertEquals(0, tree.countTerritory(target.getTerritoryID()));
  }

  @Test
  void sameUuidDifferentIdentityIsNeverRemovedByEquals() {
    QuadTree tree = new QuadTree(0, new Bounds(0, 0, 100, 100));
    UUID territoryId = UUID.randomUUID();
    Territory first = territory(territoryId, 5, 5, 5, 5);
    Territory second = territory(territoryId, 80, 80, 80, 80);
    assertEquals(first, second);
    assertNotSame(first, second);
    tree.insert(first);
    tree.insert(second);

    assertFalse(tree.remove(territory(territoryId, 40, 40, 40, 40)));
    assertEquals(2, tree.countTerritory(territoryId));
    assertEquals(1, tree.countIdentity(first));
    assertEquals(1, tree.countIdentity(second));
    assertTrue(tree.remove(first));
    assertEquals(1, tree.countTerritory(territoryId));
    assertEquals(1, tree.countIdentity(second));
    assertTrue(tree.query(80, 80).contains(second));
    assertTrue(tree.isIndexedCorrectly(second));
    assertTrue(tree.remove(second));
    assertEquals(0, tree.countTerritory(territoryId));
  }

  @Test
  void removeRejectsNullIdentity() {
    QuadTree tree = populatedTree();

    assertThrows(NullPointerException.class, () -> tree.remove(null));
  }

  @Test
  void copyFromClearsDestinationChildrenAbsentFromSource() {
    QuadTree source = new QuadTree(0, new Bounds(0, 0, 20, 20));
    Territory sourceEntry = territory(2, 2, 2, 2);
    source.insert(sourceEntry);

    QuadTree destination = new QuadTree(0, new Bounds(0, 0, 20, 20));
    Territory staleAtCorner = territory(18, 18, 18, 18);
    for (Territory stale :
        List.of(
            staleAtCorner,
            territory(2, 18, 2, 18),
            territory(18, 2, 18, 2),
            territory(10, 10, 10, 10),
            territory(19, 19, 19, 19))) {
      destination.insert(stale);
    }
    assertTrue(destination.countTerritory(staleAtCorner.getTerritoryID()) > 0);

    destination.copyFrom(source);

    assertEquals(source.getBounds().x, destination.getBounds().x);
    assertEquals(source.getBounds().z, destination.getBounds().z);
    assertEquals(source.getBounds().width, destination.getBounds().width);
    assertEquals(source.getBounds().height, destination.getBounds().height);
    assertSame(sourceEntry, destination.queryExact(2, 2));
    assertEquals(1, destination.countIdentity(sourceEntry));
    assertTrue(destination.isIndexedCorrectly(sourceEntry));
    assertEquals(0, destination.countTerritory(staleAtCorner.getTerritoryID()));
    assertTrue(destination.query(18, 18).isEmpty());
  }

  @Test
  void clearRemovesAllEntriesFromChildrenAndRoot() {
    QuadTree tree = populatedTree();
    List<Territory> entries =
        List.of(
            territory(10, 10, 20, 20),
            territory(700, 700, 710, 710));
    entries.forEach(tree::insert);
    assertTrue(tree.countTerritory(entries.get(0).getTerritoryID()) > 0);
    assertTrue(tree.countTerritory(entries.get(1).getTerritoryID()) > 0);

    tree.clear();

    for (Territory entry : entries) {
      assertEquals(0, tree.countIdentity(entry));
      assertEquals(0, tree.countTerritory(entry.getTerritoryID()));
    }
    assertTrue(tree.query(15, 15).isEmpty());
    assertTrue(tree.query(705, 705).isEmpty());
    tree.clear();
    assertTrue(tree.query(15, 15).isEmpty());
  }

  private static QuadTree populatedTree() {
    QuadTree tree = new QuadTree(0, new Bounds(0, 0, 1_024, 1_024));
    tree.insert(territory(100, 100, 110, 110));
    tree.insert(territory(200, 200, 210, 210));
    tree.insert(territory(300, 300, 310, 310));
    tree.insert(territory(400, 400, 410, 410));
    tree.insert(territory(500, 500, 510, 510));
    return tree;
  }

  private static Territory territory(int x1, int z1, int x2, int z2) {
    return new Territory(
        UUID.randomUUID(),
        "test",
        UUID.randomUUID(),
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

  private static Territory territory(UUID territoryId, int x1, int z1, int x2, int z2) {
    return new Territory(
        territoryId,
        "test",
        UUID.randomUUID(),
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

  private static QuadTree splitRootWithoutSplittingChildren() {
    QuadTree tree = new QuadTree(0, new Bounds(0, 0, 10, 10));
    for (Territory seed :
        List.of(
            territory(1, 1, 1, 1),
            territory(8, 1, 8, 1),
            territory(1, 8, 1, 8),
            territory(8, 8, 8, 8),
            territory(4, 4, 6, 6))) {
      tree.insert(seed);
    }
    return tree;
  }

  private static void move(Territory territory, int x1, int z1, int x2, int z2) {
    territory.setX1(x1);
    territory.setY1(64);
    territory.setZ1(z1);
    territory.setX2(x2);
    territory.setY2(64);
    territory.setZ2(z2);
    territory.setBackpoint(new BlockPos(x1, 64, z1));
  }
}

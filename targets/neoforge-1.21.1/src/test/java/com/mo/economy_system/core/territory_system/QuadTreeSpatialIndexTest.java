package com.mo.economy_system.core.territory_system;

import static org.junit.jupiter.api.Assertions.*;

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

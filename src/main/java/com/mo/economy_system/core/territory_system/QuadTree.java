package com.mo.economy_system.core.territory_system;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;

public class QuadTree {
  public boolean isIndexedCorrectly(Territory territory) {
    if (territory == null || countIdentity(territory) != 1) return false;
    return isStoredOnExpectedPath(territory);
  }

  int countIdentity(Territory target) {
    int count = 0;
    for (Territory territory : territories) if (territory == target) count++;
    if (nodes != null) {
      for (QuadTree node : nodes) if (node != null) count += node.countIdentity(target);
    }
    return count;
  }

  private boolean isStoredOnExpectedPath(Territory target) {
    Bounds targetBounds = target.getBounds();
    if (!bounds.contains(targetBounds)) return false;
    int localCount = 0;
    for (Territory territory : territories) if (territory == target) localCount++;
    if (nodes == null || nodes[0] == null) return localCount == 1;
    int expectedChild = getIndex(targetBounds);
    if (expectedChild == -1) return localCount == 1;
    return localCount == 0 && nodes[expectedChild].isStoredOnExpectedPath(target);
  }

  public int countTerritory(java.util.UUID territoryId) {
    if (territoryId == null) return 0;
    int count = 0;
    for (Territory territory : territories) {
      if (territoryId.equals(territory.getTerritoryID())) count++;
    }
    if (nodes != null)
      for (QuadTree node : nodes) if (node != null) count += node.countTerritory(territoryId);
    return count;
  }

  public boolean containsTerritory(java.util.UUID territoryId) {
    return countTerritory(territoryId) > 0;
  }

  private static final int MAX_CAPACITY = 4; // 每个节点最多存储的领地数
  private static final int MAX_LEVELS = 10; // 四叉树的最大深度

  private int level; // 当前节点的深度
  private List<Territory> territories; // 当前节点存储的领地
  private Bounds bounds; // 当前节点的边界
  private QuadTree[] nodes; // 子节点

  public QuadTree(int level, Bounds bounds) {
    this.level = level;
    this.bounds = bounds;
    this.territories = new ArrayList<>();
    this.nodes = new QuadTree[4];
  }

  // 获取当前四叉树的边界
  public Bounds getBounds() {
    return bounds;
  }

  // 分割当前节点
  private void split() {
    int subWidth = bounds.width / 2;
    int subHeight = bounds.height / 2;
    int x = bounds.x;
    int z = bounds.z;

    if (bounds.width == 0 || bounds.height == 0) return;

    int rightX = x + subWidth + 1;
    int bottomZ = z + subHeight + 1;
    int rightWidth = bounds.width - subWidth - 1;
    int bottomHeight = bounds.height - subHeight - 1;

    nodes[0] = new QuadTree(level + 1, new Bounds(x, z, subWidth, subHeight)); // NW
    nodes[1] = new QuadTree(level + 1, new Bounds(rightX, z, rightWidth, subHeight)); // NE
    nodes[2] = new QuadTree(level + 1, new Bounds(x, bottomZ, subWidth, bottomHeight)); // SW
    nodes[3] = new QuadTree(level + 1, new Bounds(rightX, bottomZ, rightWidth, bottomHeight)); // SE
  }

  // 插入领地
  public void insert(Territory territory) {
    Objects.requireNonNull(territory, "territory");
    // 如果超出当前边界，动态扩展
    if (!bounds.contains(territory.getBounds())) {
      expandBounds(territory.getBounds());
    }

    if (nodes[0] != null) {
      int index = getIndex(territory.getBounds());
      if (index != -1) {
        nodes[index].insert(territory);
        return;
      }
    }

    territories.add(territory);

    if (territories.size() > MAX_CAPACITY
        && level < MAX_LEVELS
        && bounds.width > 0
        && bounds.height > 0) {
      if (nodes[0] == null) split();

      territories.removeIf(
          t -> {
            int index = getIndex(t.getBounds());
            if (index != -1) {
              nodes[index].insert(t);
              return true;
            }
            return false;
          });
    }
  }

  // 查询与指定点相关的领地
  public List<Territory> query(int x, int z) {
    List<Territory> result = new ArrayList<>();

    if (!bounds.contains(x, z)) return result;

    for (Territory territory : territories) {
      if (territory.isWithinBoundsIgnoreY(x, z)) {
        result.add(territory);
      }
    }

    if (nodes[0] != null) {
      for (QuadTree node : nodes) {
        result.addAll(node.query(x, z));
      }
    }

    return result;
  }

  // 修改 query 方法，直接返回精确结果
  public Territory queryExact(int x, int z) {
    List<Territory> candidates = query(x, z);
    return candidates.stream().filter(t -> t.isWithinBoundsIgnoreY(x, z)).findFirst().orElse(null);
  }

  public Territory queryExact(BlockPos pos1, BlockPos pos2) {
    List<Territory> candidates = query(pos1.getX(), pos2.getX());
    return candidates.stream()
        .filter(t -> t.isWithinBoundsIgnoreY(pos1.getX(), pos2.getX()))
        .findFirst()
        .orElse(null);
  }

  // 从四叉树中移除领地
  public boolean remove(Territory territory) {
    Objects.requireNonNull(territory, "territory");
    return removeIdentity(territory);
  }

  private boolean removeIdentity(Territory target) {
    for (int index = 0; index < territories.size(); index++) {
      if (territories.get(index) == target) {
        territories.remove(index);
        return true;
      }
    }
    if (nodes != null) {
      for (QuadTree node : nodes) {
        if (node != null && node.removeIdentity(target)) return true;
      }
    }
    return false;
  }

  public void clear() {
    territories.clear();
    if (nodes == null) nodes = new QuadTree[4];
    for (int i = 0; i < nodes.length; i++) {
      if (nodes[i] != null) {
        nodes[i].clear();
        nodes[i] = null;
      }
    }
  }

  private int getIndex(Bounds bounds) {
    int index = -1;
    long leftMaxX = (long) this.bounds.x + this.bounds.width / 2;
    long topMaxZ = (long) this.bounds.z + this.bounds.height / 2;
    long rightMinX = leftMaxX + 1;
    long bottomMinZ = topMaxZ + 1;
    long candidateMaxX = (long) bounds.x + bounds.width;
    long candidateMaxZ = (long) bounds.z + bounds.height;

    boolean top = candidateMaxZ <= topMaxZ;
    boolean bottom = bounds.z >= bottomMinZ;

    if (candidateMaxX <= leftMaxX) {
      if (top) index = 0; // NW
      else if (bottom) index = 2; // SW
    } else if (bounds.x >= rightMinX) {
      if (top) index = 1; // NE
      else if (bottom) index = 3; // SE
    }

    return index;
  }

  private void expandBounds(Bounds newBounds) {
    // 计算新的四叉树边界，使其包含当前边界和新的领地
    int minX = Math.min(bounds.x, newBounds.x);
    int minZ = Math.min(bounds.z, newBounds.z);
    int maxX = Math.max(bounds.x + bounds.width, newBounds.x + newBounds.width);
    int maxZ = Math.max(bounds.z + bounds.height, newBounds.z + newBounds.height);

    // 创建新的四叉树边界
    Bounds expandedBounds = new Bounds(minX, minZ, maxX - minX, maxZ - minZ);

    // 迁移旧数据到新的四叉树
    QuadTree newTree = new QuadTree(0, expandedBounds);
    transferDataTo(newTree);

    // 更新当前四叉树为扩展后的树
    this.bounds = expandedBounds;
    this.territories = newTree.territories;
    this.nodes = newTree.nodes;
  }

  private void transferDataTo(QuadTree newTree) {
    for (Territory territory : territories) {
      newTree.insert(territory);
    }
    territories.clear();

    if (nodes[0] != null) {
      for (QuadTree node : nodes) {
        if (node != null) {
          node.transferDataTo(newTree);
        }
      }
      nodes = new QuadTree[4];
    }
  }

  // 从另一个四叉树复制数据
  public void copyFrom(QuadTree other) {
    Objects.requireNonNull(other, "other");
    clear();
    this.territories.addAll(other.territories);
    this.bounds.x = other.bounds.x;
    this.bounds.z = other.bounds.z;
    this.bounds.width = other.bounds.width;
    this.bounds.height = other.bounds.height;

    for (int i = 0; i < other.nodes.length; i++) {
      if (other.nodes[i] != null) {
        Bounds sourceBounds = other.nodes[i].getBounds();
        this.nodes[i] =
            new QuadTree(
                other.nodes[i].level,
                new Bounds(
                    sourceBounds.x, sourceBounds.z, sourceBounds.width, sourceBounds.height));
        this.nodes[i].copyFrom(other.nodes[i]);
      }
    }
  }
}

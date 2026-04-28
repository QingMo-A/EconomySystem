package com.mo.economy_system.core.territory_system;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class QuadTree {

    private static final int MAX_CAPACITY = 4; // 每个节点最多存储的领地数
    private static final int MAX_LEVELS = 10;  // 四叉树的最大深度

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

        nodes[0] = new QuadTree(level + 1, new Bounds(x, z, subWidth, subHeight)); // NW
        nodes[1] = new QuadTree(level + 1, new Bounds(x + subWidth, z, subWidth, subHeight)); // NE
        nodes[2] = new QuadTree(level + 1, new Bounds(x, z + subHeight, subWidth, subHeight)); // SW
        nodes[3] = new QuadTree(level + 1, new Bounds(x + subWidth, z + subHeight, subWidth, subHeight)); // SE
    }

    // 插入领地
    public void insert(Territory territory) {
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

        if (territories.size() > MAX_CAPACITY && level < MAX_LEVELS) {
            if (nodes[0] == null) split();

            territories.removeIf(t -> {
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
        return candidates.stream()
                .filter(t -> t.isWithinBoundsIgnoreY(x, z))
                .findFirst()
                .orElse(null);
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
        if (!bounds.intersects(territory.getBounds())) return false;

        // 如果当前节点包含该领地，直接移除
        if (territories.remove(territory)) {
            return true;
        }

        // 递归子节点查找
        if (nodes[0] != null) {
            for (QuadTree node : nodes) {
                if (node.remove(territory)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void clear() {
        territories.clear();
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i] != null) {
                nodes[i].clear();
                nodes[i] = null;
            }
        }
    }

    private int getIndex(Bounds bounds) {
        int index = -1;
        double midX = this.bounds.x + this.bounds.width / 2.0;
        double midZ = this.bounds.z + this.bounds.height / 2.0;

        boolean top = bounds.z + bounds.height <= midZ;
        boolean bottom = bounds.z >= midZ;

        if (bounds.x + bounds.width <= midX) {
            if (top) index = 0; // NW
            else if (bottom) index = 2; // SW
        } else if (bounds.x >= midX) {
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
            nodes = null;
        }
    }

    // 从另一个四叉树复制数据
    public void copyFrom(QuadTree other) {
        this.territories.clear();
        this.territories.addAll(other.territories);
        this.bounds.x = other.bounds.x;
        this.bounds.z = other.bounds.z;
        this.bounds.width = other.bounds.width;
        this.bounds.height = other.bounds.height;

        for (int i = 0; i < other.nodes.length; i++) {
            if (other.nodes[i] != null) {
                this.nodes[i] = new QuadTree(other.nodes[i].level, other.nodes[i].getBounds());
                this.nodes[i].copyFrom(other.nodes[i]);
            }
        }
    }
}

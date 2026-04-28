package com.mo.economy_system.core.territory_system;

import net.minecraft.core.BlockPos;

public class Bounds {
    public int x, z, width, height;

    public Bounds(int x, int z, int width, int height) {
        this.x = x;
        this.z = z;
        this.width = width;
        this.height = height;
    }

    // 判断当前边界是否包含一个点
    public boolean contains(int px, int pz) {
        return px >= x && px < x + width + 1 && pz >= z && pz < z + height + 1;
    }

    // 判断当前边界是否完全包含另一个边界
    public boolean contains(Bounds other) {
        return this.x <= other.x &&
                this.z <= other.z &&
                this.x + this.width >= other.x + other.width &&
                this.z + this.height >= other.z + other.height;
    }

    // 判断当前边界是否与另一个边界相交
    public boolean intersects(Bounds other) {
        return x < other.x + other.width &&
                x + width > other.x &&
                z < other.z + other.height &&
                z + height > other.z;
    }

    public static Bounds calculateBounds(BlockPos pos1, BlockPos pos2) {
        return new Bounds(
                Math.min(pos1.getX(), pos2.getX()),
                Math.min(pos1.getZ(), pos2.getZ()),
                Math.abs(pos1.getX() - pos2.getX()),
                Math.abs(pos1.getZ() - pos2.getZ())
        );
    }
}


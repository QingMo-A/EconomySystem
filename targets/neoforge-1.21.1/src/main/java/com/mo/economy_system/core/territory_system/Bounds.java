package com.mo.economy_system.core.territory_system;

import java.util.Objects;
import net.minecraft.core.BlockPos;

public class Bounds {
  public int x, z, width, height;

  public Bounds(int x, int z, int width, int height) {
    if (width < 0 || height < 0) {
      throw new IllegalArgumentException("Bounds width and height must be non-negative");
    }
    this.x = x;
    this.z = z;
    this.width = width;
    this.height = height;
  }

  // 判断当前边界是否包含一个点
  public boolean contains(int px, int pz) {
    long maxX = (long) x + width;
    long maxZ = (long) z + height;
    return px >= x && px <= maxX && pz >= z && pz <= maxZ;
  }

  // 判断当前边界是否完全包含另一个边界
  public boolean contains(Bounds other) {
    Objects.requireNonNull(other, "other");
    long thisMaxX = (long) x + width;
    long thisMaxZ = (long) z + height;
    long otherMaxX = (long) other.x + other.width;
    long otherMaxZ = (long) other.z + other.height;
    return x <= other.x && z <= other.z && thisMaxX >= otherMaxX && thisMaxZ >= otherMaxZ;
  }

  // 判断当前边界是否与另一个边界相交
  public boolean intersects(Bounds other) {
    Objects.requireNonNull(other, "other");
    long thisMaxX = (long) x + width;
    long thisMaxZ = (long) z + height;
    long otherMaxX = (long) other.x + other.width;
    long otherMaxZ = (long) other.z + other.height;
    return x <= otherMaxX && thisMaxX >= other.x && z <= otherMaxZ && thisMaxZ >= other.z;
  }

  public static Bounds calculateBounds(BlockPos pos1, BlockPos pos2) {
    return new Bounds(
        Math.min(pos1.getX(), pos2.getX()),
        Math.min(pos1.getZ(), pos2.getZ()),
        Math.abs(pos1.getX() - pos2.getX()),
        Math.abs(pos1.getZ() - pos2.getZ()));
  }
}

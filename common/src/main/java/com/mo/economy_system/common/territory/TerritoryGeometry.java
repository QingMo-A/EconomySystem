package com.mo.economy_system.common.territory;

import java.util.Objects;

/** Loader-neutral geometry for the inclusive X/Z territory plane. */
public final class TerritoryGeometry {
  public static final long MAX_COORDINATE = 30_000_000L;

  private TerritoryGeometry() {}

  /** A closed integer rectangle. Bounds use inclusive maximum coordinates. */
  public record Rectangle(int minX, int minZ, int maxX, int maxZ) {
    public Rectangle {
      if (minX > maxX || minZ > maxZ) {
        throw new IllegalArgumentException("rectangle coordinates");
      }
    }

    public long width() {
      return (long) maxX - minX + 1L;
    }

    public long height() {
      return (long) maxZ - minZ + 1L;
    }

    public long area() {
      return Math.multiplyExact(width(), height());
    }

    public boolean contains(int x, int z) {
      return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public boolean intersects(Rectangle other) {
      Objects.requireNonNull(other, "other");
      return minX <= other.maxX
          && maxX >= other.minX
          && minZ <= other.maxZ
          && maxZ >= other.minZ;
    }
  }

  public static Rectangle rectangle(int firstX, int firstZ, int secondX, int secondZ) {
    return new Rectangle(
        Math.min(firstX, secondX),
        Math.min(firstZ, secondZ),
        Math.max(firstX, secondX),
        Math.max(firstZ, secondZ));
  }

  public static Rectangle rectangle(
      TerritorySnapshots.Position first, TerritorySnapshots.Position second) {
    Objects.requireNonNull(first, "first");
    Objects.requireNonNull(second, "second");
    return rectangle(first.x(), first.z(), second.x(), second.z());
  }

  public static long area(int firstX, int firstZ, int secondX, int secondZ) {
    return rectangle(firstX, firstZ, secondX, secondZ).area();
  }

  public static long area(
      TerritorySnapshots.Position first, TerritorySnapshots.Position second) {
    return rectangle(first, second).area();
  }

  public static boolean validCoordinate(int x, int z) {
    return Math.abs((long) x) <= MAX_COORDINATE && Math.abs((long) z) <= MAX_COORDINATE;
  }

  public static boolean validCoordinate(TerritorySnapshots.Position position) {
    Objects.requireNonNull(position, "position");
    return validCoordinate(position.x(), position.z());
  }
}

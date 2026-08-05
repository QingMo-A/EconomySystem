package com.mo.economy_system.common.check;

public final class ClientFileCheckLayout {
  public record Box(int x, int y, int width, int height) {
    public Box {
      if (x < 0 || y < 0 || width < 1 || height < 1) throw new IllegalArgumentException("box");
    }

    public boolean fits(int screenWidth, int screenHeight) {
      return x + width <= screenWidth && y + height <= screenHeight;
    }
  }

  public record Consent(Box allow, Box decline) {}

  private ClientFileCheckLayout() {}

  public static Consent consent(int width, int height) {
    if (width >= 220 && height >= 30) {
      int y = height - 25;
      return new Consent(new Box(width / 2 - 105, y, 100, 20), new Box(width / 2 + 5, y, 100, 20));
    }
    if (width >= 110 && height >= 55) {
      return new Consent(
          new Box(width / 2 - 50, height - 50, 100, 20),
          new Box(width / 2 - 50, height - 25, 100, 20));
    }
    return new Consent(null, null);
  }

  public static Box search(int width, int height) {
    if (width < 80 || height < 100) return null;
    return new Box(12, 62, Math.min(220, width - 24), 18);
  }

  public static int visibleRows(int height) {
    return Math.max(0, (height - 136) / 12);
  }

  public static int clampOffset(int offset, int rowCount, int visibleRows) {
    return Math.max(0, Math.min(Math.max(0, rowCount - visibleRows), offset));
  }
}

package com.mo.economy_system.common.transfer;

/** Pure layout rules shared by both checked-file transfer result screens. */
public final class CheckedFileTransferLayout {
  public record Box(int x, int y, int width, int height) {
    public Box {
      if (x < 0 || y < 0 || width < 1 || height < 1) {
        throw new IllegalArgumentException("box");
      }
    }

    public boolean fits(int screenWidth, int screenHeight) {
      return x + width <= screenWidth && y + height <= screenHeight;
    }
  }

  public record Actions(Box primary, Box secondary) {}

  private CheckedFileTransferLayout() {}

  /** Two bounded actions, omitted when both cannot be rendered and focused safely. */
  public static Actions twoActions(int width, int height) {
    if (width < 55 || height < 30) return new Actions(null, null);
    int buttonWidth = Math.min(100, Math.max(1, (width - 15) / 2));
    int total = buttonWidth * 2 + 5;
    int x = Math.max(0, (width - total) / 2);
    int y = Math.max(0, height - 25);
    int buttonHeight = Math.min(20, Math.max(1, height - y));
    Box primary = new Box(x, y, buttonWidth, buttonHeight);
    Box secondary = new Box(x + buttonWidth + 5, y, buttonWidth, buttonHeight);
    return primary.fits(width, height) && secondary.fits(width, height)
        ? new Actions(primary, secondary)
        : new Actions(null, null);
  }

  /** One bounded close action, omitted on unusably tiny screens. */
  public static Box closeAction(int width, int height) {
    if (width < 32 || height < 30) return null;
    int buttonWidth = Math.min(100, Math.max(1, width - 8));
    int x = Math.max(0, (width - buttonWidth) / 2);
    int y = Math.max(0, height - 25);
    Box box = new Box(x, y, buttonWidth, Math.min(20, Math.max(1, height - y)));
    return box.fits(width, height) ? box : null;
  }

  /** Number of complete detail rows that fit above an optional action with a fixed gap. */
  public static int visibleRows(
      int screenWidth,
      int screenHeight,
      int minimumWidth,
      int firstY,
      int rowHeight,
      int requestedRows,
      Box action) {
    if (screenWidth < minimumWidth
        || screenHeight <= 0
        || firstY < 0
        || rowHeight < 1
        || requestedRows < 1) {
      return 0;
    }
    int bottomExclusive = action == null ? screenHeight : Math.max(0, action.y() - 4);
    if (bottomExclusive <= firstY) return 0;
    return Math.min(requestedRows, (bottomExclusive - firstY) / rowHeight);
  }

  /** Display-only truncation; callers retain and continue using the original metadata. */
  public static String truncate(String value, int maxCharacters) {
    if (value == null || maxCharacters < 4 || value.length() <= maxCharacters) return value;
    return value.substring(0, maxCharacters - 3) + "...";
  }
}

package com.mo.economy_system.common.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CheckedFileTransferLayoutTest {
  private static final int[] WIDTHS = {1, 8, 16, 32, 64, 80, 96, 110, 160, 219, 220, 240, 320};
  private static final int[] HEIGHTS = {1, 20, 30, 44, 55, 80, 90, 100, 120, 136, 140, 168, 196, 240};

  @Test
  void everyPublishedTinyDimensionKeepsControlsInBounds() {
    for (int width : WIDTHS) {
      for (int height : HEIGHTS) {
        var actions = CheckedFileTransferLayout.twoActions(width, height);
        if (actions.primary() == null) {
          assertNull(actions.secondary());
        } else {
          assertTrue(actions.primary().fits(width, height));
          assertTrue(actions.secondary().fits(width, height));
        }
        var close = CheckedFileTransferLayout.closeAction(width, height);
        if (close != null) assertTrue(close.fits(width, height));

        int resultRows = CheckedFileTransferLayout.visibleRows(
            width, height, 64, 38, 12, 7, actions.primary());
        assertTrue(resultRows >= 0 && resultRows <= 7);
        if (resultRows > 0) {
          int rowBottom = 38 + resultRows * 12;
          int limit = actions.primary() == null ? height : actions.primary().y() - 4;
          assertTrue(rowBottom <= limit);
        }

        int terminalRows = CheckedFileTransferLayout.visibleRows(
            width, height, 64, 38, 12, 2, close);
        assertTrue(terminalRows >= 0 && terminalRows <= 2);
        if (terminalRows > 0) {
          int rowBottom = 38 + terminalRows * 12;
          int limit = close == null ? height : close.y() - 4;
          assertTrue(rowBottom <= limit);
        }
      }
    }
  }

  @Test
  void truncationDoesNotMutateTheSourceMetadata() {
    String hash = "a".repeat(64);
    assertEquals("a".repeat(37) + "...", CheckedFileTransferLayout.truncate(hash, 40));
    assertEquals("a".repeat(64), hash);
  }
}

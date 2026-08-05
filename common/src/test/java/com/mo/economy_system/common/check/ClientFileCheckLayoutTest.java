package com.mo.economy_system.common.check;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ClientFileCheckLayoutTest {
  private static final int[] WIDTHS = {1, 8, 16, 32, 64, 80, 96, 110, 160, 220, 240, 320};
  private static final int[] HEIGHTS = {1, 20, 30, 44, 55, 80, 100, 120, 136, 140, 240};

  @Test
  void allOptionalControlsFitTinyAndNormalScreens() {
    for (int width : WIDTHS)
      for (int height : HEIGHTS) {
        var consent = ClientFileCheckLayout.consent(width, height);
        if (consent.allow() != null) {
          assertTrue(consent.allow().fits(width, height));
          assertTrue(consent.decline().fits(width, height));
        }
        var search = ClientFileCheckLayout.search(width, height);
        if (search != null) assertTrue(search.fits(width, height));
        var result = ClientFileCheckLayout.result(width, height, true);
        if (result.search() != null) assertTrue(result.search().fits(width, height));
        if (result.status() != null) assertTrue(result.status().fits(width, height));
        if (result.retry() != null) assertTrue(result.retry().fits(width, height));
        if (result.rows() != null) assertTrue(result.rows().fits(width, height));
        assertTrue(ClientFileCheckLayout.visibleRows(height) >= 0);
        assertTrue(ClientFileCheckLayout.visibleRows(height, true) >= 0);
      }
  }

  @Test
  void scrollOffsetIsAlwaysClamped() {
    assertEquals(0, ClientFileCheckLayout.clampOffset(-10, 20, 5));
    assertEquals(15, ClientFileCheckLayout.clampOffset(99, 20, 5));
    assertEquals(0, ClientFileCheckLayout.clampOffset(3, 2, 5));
  }
}

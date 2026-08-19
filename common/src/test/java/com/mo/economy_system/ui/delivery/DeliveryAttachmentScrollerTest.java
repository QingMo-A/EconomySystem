package com.mo.economy_system.ui.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.geometry.UiRect;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeliveryAttachmentScrollerTest {
  @Test
  void scrollClampsAndSwitchingMailResetsPosition() {
    DeliveryAttachmentScroller scroller = new DeliveryAttachmentScroller();
    UUID first = UUID.randomUUID();
    scroller.syncMail(first);

    assertTrue(scroller.scroll(1, 10, 4));
    assertEquals(1, scroller.firstIndex());
    for (int i = 0; i < 20; i++) scroller.scroll(1, 10, 4);
    assertEquals(6, scroller.firstIndex());
    assertFalse(scroller.scroll(1, 10, 4));

    scroller.syncMail(UUID.randomUUID());
    assertEquals(0, scroller.firstIndex());
  }

  @Test
  void draggingThumbMapsAcrossAvailableAttachmentRange() {
    DeliveryAttachmentScroller scroller = new DeliveryAttachmentScroller();
    scroller.syncMail(UUID.randomUUID());
    UiRect track = new UiRect(10, 20, 100, 5);
    UiRect thumb = new UiRect(10, 20, 30, 5);

    assertTrue(scroller.press(20, 22, track, thumb, 10, 4));
    assertTrue(scroller.drag(100, track, thumb, 10, 4));
    assertEquals(6, scroller.firstIndex());
    assertTrue(scroller.release());
    assertFalse(scroller.dragging());
  }
}

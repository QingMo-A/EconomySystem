package com.mo.economy_system.ui.home;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HomeOpenAnimationTest {
  @Test
  void offsetsAreDeterministicAtAnimationBoundaries() {
    assertEquals(0.0f, HomeOpenAnimation.progressAt(100L, 100L));
    assertEquals(-50, HomeOpenAnimation.leftOffsetAt(0.0f));
    assertEquals(50, HomeOpenAnimation.rightOffsetAt(0.0f));
    assertEquals(0, HomeOpenAnimation.leftOffsetAt(1.0f));
    assertEquals(0, HomeOpenAnimation.rightOffsetAt(1.0f));
    assertEquals(0.875f, HomeOpenAnimation.easeOutCubic(0.5f), 0.00001f);
    assertEquals(1.0f, HomeOpenAnimation.progressAt(100L,
        100L + HomeOpenAnimation.DURATION_NANOS));
  }
}

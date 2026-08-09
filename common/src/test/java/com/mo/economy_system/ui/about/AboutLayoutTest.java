package com.mo.economy_system.ui.about;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AboutLayoutTest {
  @Test
  void sharedQrTexturesArePackagedForEveryTarget() {
    ClassLoader resources = AboutLayoutTest.class.getClassLoader();
    assertTrue(resources.getResource("assets/economy_system/textures/gui/vx.png") != null);
    assertTrue(resources.getResource("assets/economy_system/textures/gui/zfb.png") != null);
  }

  @Test
  void layoutHasPositiveStableRegionsOnNarrowScreens() {
    for (int[] viewport : new int[][] {{1, 1}, {120, 80}, {640, 360}, {1920, 1080}}) {
      AboutLayout.Layout layout = AboutLayout.calculate(viewport[0], viewport[1]);
      assertTrue(layout.panel().width() > 0 && layout.panel().height() > 0);
      assertTrue(layout.panel().contains(layout.backButton()));
      assertTrue(layout.leftQr().width() > 0 && layout.rightQr().width() > 0);
    }
  }
}

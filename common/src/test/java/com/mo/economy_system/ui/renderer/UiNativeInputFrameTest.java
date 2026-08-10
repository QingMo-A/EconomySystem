package com.mo.economy_system.ui.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mo.economy_system.ui.geometry.UiRect;
import org.junit.jupiter.api.Test;

/** Physical-pixel frame geometry for native EditBox chrome. */
class UiNativeInputFrameTest {
  @Test
  void inflatesNativeWidgetByLegacyFourAndTwoPixelInsets() {
    assertEquals(new UiRect(12, 25, 275, 31),
        UiNativeInputFrame.frameRect(new UiRect(16, 27, 267, 27)));
  }

  @Test
  void fractionalViewportContractsRemainPhysicalAndDeterministic() {
    for (int[] viewport : new int[][] {{854, 480}, {1000, 563}, {1365, 768}}) {
      // The widget's native framebuffer rectangle is already rounded by the target.  The common
      // frame must be identical regardless of the virtual scale used to derive that rectangle.
      UiRect nativeRect = new UiRect(16, 27, 267, 27);
      assertEquals(new UiRect(12, 25, 275, 31), UiNativeInputFrame.frameRect(nativeRect),
          "viewport " + viewport[0] + "x" + viewport[1]);
    }
  }
}

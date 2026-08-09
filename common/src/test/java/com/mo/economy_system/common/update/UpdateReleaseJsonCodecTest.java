package com.mo.economy_system.common.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class UpdateReleaseJsonCodecTest {
  @Test
  void decodesLatestReleaseAndComparesSemantically() {
    UpdateRelease release = UpdateReleaseJsonCodec.decode(
        "{\"tag_name\":\"v1.4.0\",\"html_url\":\"https://github.com/QingMo-A/EconomySystem/releases/tag/v1.4.0\"}");
    UpdateCheckResult result = UpdateCheckResult.evaluate("1.3.0", release);
    assertEquals(UpdateCheckResult.Status.UPDATE_AVAILABLE, result.status());
  }

  @Test
  void malformedPayloadFailsClosed() {
    assertThrows(IllegalArgumentException.class,
        () -> UpdateReleaseJsonCodec.decode("{\"tag_name\":1,\"html_url\":\"x\"}"));
    assertThrows(IllegalArgumentException.class,
        () -> UpdateReleaseJsonCodec.decode("{\"tag_name\":\"1.0.0\",\"html_url\":\"file:///tmp/x\"}"));
    assertThrows(IllegalArgumentException.class,
        () -> UpdateReleaseJsonCodec.decode(
            "{\"tag_name\":\"1.0.0\",\"tag_name\":\"1.1.0\",\"html_url\":\"https://example.com\"}"));
  }

  @Test
  void lowerOrEqualLatestIsCurrent() {
    UpdateRelease release = new UpdateRelease("1.2.0", "https://example.com/release");
    assertEquals(UpdateCheckResult.Status.CURRENT,
        UpdateCheckResult.evaluate("v1.2.0", release).status());
    assertEquals(UpdateCheckResult.Status.CURRENT,
        UpdateCheckResult.evaluate("1.3.0", release).status());
  }
}

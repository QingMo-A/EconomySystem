package com.mo.economy_system.platform;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlatformCapabilitiesTest {
  @Test
  void fullCapabilitiesAreExplicitAndLoaderNeutral() {
    PlatformCapabilities capabilities = PlatformCapabilities.full();
    assertTrue(capabilities.supportsPlayerHeadRendering());
    assertTrue(capabilities.supportsItemRendering());
    assertTrue(capabilities.supportsClipboard());
    assertTrue(capabilities.supportsTerritoryResize());
  }
}

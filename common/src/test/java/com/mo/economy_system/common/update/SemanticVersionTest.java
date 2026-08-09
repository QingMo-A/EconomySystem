package com.mo.economy_system.common.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SemanticVersionTest {
  @Test
  void parsesLeadingVAndNormalizes() {
    assertEquals("1.2.3", SemanticVersion.parse(" v1.2.3 ").orElseThrow().toString());
    assertEquals(0, SemanticVersion.parse("1.2.3").orElseThrow()
        .compareTo(SemanticVersion.parse("v1.2.3+build.7").orElseThrow()));
  }

  @Test
  void followsSemverPreReleaseOrdering() {
    assertTrue(SemanticVersion.parse("1.0.0").orElseThrow()
        .compareTo(SemanticVersion.parse("1.0.0-rc.1").orElseThrow()) > 0);
    assertTrue(SemanticVersion.parse("1.0.0-rc.10").orElseThrow()
        .compareTo(SemanticVersion.parse("1.0.0-rc.2").orElseThrow()) > 0);
  }

  @Test
  void rejectsMalformedVersions() {
    for (String value : new String[] {"1.2", "1.02.3", "1.2.3-", "x1.2.3", "1.2.3.4"}) {
      assertFalse(SemanticVersion.parse(value).isPresent(), value);
    }
  }
}

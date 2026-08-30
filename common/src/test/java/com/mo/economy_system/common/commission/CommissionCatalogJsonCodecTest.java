package com.mo.economy_system.common.commission;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CommissionCatalogJsonCodecTest {
  @Test
  void builtInCatalogRoundTripsWithoutChangingGenerationInputs() {
    CommissionCatalog original = CommissionCatalogDefaults.create();
    CommissionCatalog decoded = CommissionCatalogJsonCodec.decode(CommissionCatalogJsonCodec.encode(original));
    assertEquals(original.templates(), decoded.templates());
    assertEquals(original.requesterPools(), decoded.requesterPools());
    assertEquals(original.targetPools(), decoded.targetPools());
    assertEquals(original.settings(), decoded.settings());
  }

  @Test
  void unsupportedSchemaAndMissingPoolsFailFast() {
    assertThrows(IllegalArgumentException.class,
        () -> CommissionCatalogJsonCodec.decode("{\"schema\":99}"));
    assertThrows(IllegalArgumentException.class,
        () -> CommissionCatalogJsonCodec.decode("{\"schema\":1,\"templates\":[]}"));
  }
}

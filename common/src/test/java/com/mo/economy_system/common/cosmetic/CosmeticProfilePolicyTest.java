package com.mo.economy_system.common.cosmetic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CosmeticProfilePolicyTest {
  private static final UUID UUID_VALUE = UUID.fromString("dc5eb054-afdc-44d2-9062-9d18dbe3d30c");

  @Test
  void parsesOnlyValidIdentities() {
    assertEquals(Optional.of(UUID_VALUE), CosmeticProfilePolicy.parseUuid(UUID_VALUE.toString()));
    assertTrue(CosmeticProfilePolicy.parseUuid("bad").isEmpty());
    assertTrue(CosmeticProfilePolicy.parseUuid(null).isEmpty());
  }

  @Test
  void bindingRequiresUuidAndName() {
    assertTrue(CosmeticProfilePolicy.canBindSupporter(UUID_VALUE, "supporter"));
    assertFalse(CosmeticProfilePolicy.canBindSupporter(null, "supporter"));
    assertFalse(CosmeticProfilePolicy.canBindSupporter(UUID_VALUE, "  "));
  }

  @Test
  void dollProfileNormalizesBlankNames() {
    CosmeticProfilePolicy.DollProfile profile =
        new CosmeticProfilePolicy.DollProfile(UUID_VALUE, "  ", true);
    assertEquals(UUID_VALUE.toString(), profile.playerName());
    assertTrue(profile.slimModel());
  }

}

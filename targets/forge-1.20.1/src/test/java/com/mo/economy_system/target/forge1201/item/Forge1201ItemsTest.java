package com.mo.economy_system.target.forge1201.item;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import net.minecraft.server.Bootstrap;
import net.minecraft.SharedConstants;

class Forge1201ItemsTest {
  @Test void recallPotionHasStableIdAndSingleStackSize() {
    SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
    assertEquals("economy_system:recall_potion", Forge1201Items.RECALL_POTION.getId().toString());
    assertEquals(1, Forge1201Items.RECALL_POTION_MAX_STACK_SIZE);
  }

  @Test void biannualClampDollHasStableRegistryId() {
    SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
    assertEquals("economy_system:biannualclamp68_doll_hat",
        Forge1201Items.BIANNUALCLAMP68_DOLL_HAT.getId().toString());
  }
}

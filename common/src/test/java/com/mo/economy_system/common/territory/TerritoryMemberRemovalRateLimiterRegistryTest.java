package com.mo.economy_system.common.territory;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.ref.WeakReference;
import org.junit.jupiter.api.Test;

class TerritoryMemberRemovalRateLimiterRegistryTest {
  @Test
  void sameServerSharesLimiterAndDifferentServersAreIsolated() {
    var registry = new TerritoryMemberRemovalRateLimiterRegistry<Object>();
    Object first = new Object(), second = new Object();
    assertSame(registry.get(first), registry.get(first));
    assertNotSame(registry.get(first), registry.get(second));
    assertEquals(2, registry.serverCount());
    assertThrows(NullPointerException.class, () -> registry.get(null));
  }

  @Test
  void registryDoesNotStronglyRetainServerKey() throws Exception {
    var registry = new TerritoryMemberRemovalRateLimiterRegistry<Object>();
    Object server = new Object();
    WeakReference<Object> reference = new WeakReference<>(server);
    registry.get(server);
    server = null;
    for (int i = 0; i < 20 && reference.get() != null; i++) {
      System.gc();
      Thread.sleep(10);
    }
    if (reference.get() == null) assertEquals(0, registry.serverCount());
  }
}

package com.mo.economy_system.common.territory;
import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
class TerritoryTeleportLimiterRegistryTest {
  @Test void serversHaveIndependentLimiters(){var registry=new TerritoryTeleportLimiterRegistry<Object>();Object a=new Object(),b=new Object();UUID player=UUID.randomUUID();assertTrue(registry.forServer(a).tryAcquire(player,10));assertFalse(registry.forServer(a).tryAcquire(player,11));assertTrue(registry.forServer(b).tryAcquire(player,11));assertSame(registry.forServer(a),registry.forServer(a));}
}

package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.market.DemandItemResolveResult;
import com.mo.economy_system.target.forge1201.item.Forge1201ItemStackBridge;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Forge1201DemandItemResolverTest {
    private final Forge1201DemandItemResolver resolver=new Forge1201DemandItemResolver(new Forge1201ItemStackBridge(),RegistryAccess.EMPTY);
    @BeforeAll static void bootstrap(){SharedConstants.tryDetectVersion();Bootstrap.bootStrap();}
    @Test void resolvesCanonicalDefaultStone(){var result=resolver.resolve("minecraft:stone");assertTrue(result.isSuccess());assertEquals("minecraft:stone",result.value().canonicalItemId());assertEquals(1,result.value().template().count());assertEquals(64,result.value().maxQuantity());}
    @Test void rejectsMalformedMissingAndAir(){assertEquals(DemandItemResolveResult.Error.INVALID_ITEM_ID,resolver.resolve("bad id").error());assertEquals(DemandItemResolveResult.Error.ITEM_NOT_FOUND,resolver.resolve("economy_system:not_real").error());assertEquals(DemandItemResolveResult.Error.ITEM_NOT_FOUND,resolver.resolve("minecraft:air").error());}
}

package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.market.DemandItemResolveResult;
import com.mo.economy_system.target.neoforge1211.item.NeoForge1211ItemStackBridge;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NeoForge1211DemandItemResolverTest {
    private static NeoForge1211DemandItemResolver resolver;
    @BeforeAll static void bootstrap(){Bootstrap.bootStrap();resolver=new NeoForge1211DemandItemResolver(new NeoForge1211ItemStackBridge(),VanillaRegistries.createLookup());}
    @Test void resolvesCanonicalDefaultStone(){var result=resolver.resolve("minecraft:stone");assertTrue(result.isSuccess());assertEquals("minecraft:stone",result.value().canonicalItemId());assertEquals(1,result.value().template().count());assertEquals(64,result.value().maxQuantity());}
    @Test void rejectsMalformedMissingAndAir(){assertEquals(DemandItemResolveResult.Error.INVALID_ITEM_ID,resolver.resolve("bad id").error());assertEquals(DemandItemResolveResult.Error.ITEM_NOT_FOUND,resolver.resolve("economy_system:not_real").error());assertEquals(DemandItemResolveResult.Error.ITEM_NOT_FOUND,resolver.resolve("minecraft:air").error());}
}

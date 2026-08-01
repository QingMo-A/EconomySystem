package com.mo.economy_system.target.forge1201.client;

import com.mo.economy_system.common.client.ClientMarketState;
import com.mo.economy_system.EconomyConstants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EconomyConstants.MOD_ID, value = Dist.CLIENT)
public final class Forge1201ClientMarketStateEvents {
    private Forge1201ClientMarketStateEvents() {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientMarketState.reset();
    }
}

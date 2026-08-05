package com.mo.economy_system.client;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.target.neoforge1211.client.Screen_ClientFileCheckConsent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = EconomySystem.MODID, value = Dist.CLIENT)
public final class ClientFileCheckEvents {
  private ClientFileCheckEvents() {}
  @SubscribeEvent public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
    Screen_ClientFileCheckConsent.cancelPendingScan();
  }
}

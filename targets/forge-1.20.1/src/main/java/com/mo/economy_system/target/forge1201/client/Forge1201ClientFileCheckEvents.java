package com.mo.economy_system.target.forge1201.client;

import com.mo.economy_system.target.forge1201.EconomySystemForge1201;
import com.mo.economy_system.target.forge1201.network.Forge1201ClientFileCheckScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
    modid = EconomySystemForge1201.MODID,
    value = Dist.CLIENT,
    bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Forge1201ClientFileCheckEvents {
  private Forge1201ClientFileCheckEvents() {}

  @SubscribeEvent
  public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
    Forge1201ClientFileCheckScreens.cancelPendingScan();
  }
}

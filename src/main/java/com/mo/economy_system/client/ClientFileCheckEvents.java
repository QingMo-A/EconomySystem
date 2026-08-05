package com.mo.economy_system.client;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.target.neoforge1211.client.NeoForge1211ClientFileCheckClientRuntime;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

@EventBusSubscriber(modid = EconomySystem.MODID, value = Dist.CLIENT)
public final class ClientFileCheckEvents {
  private ClientFileCheckEvents() {}

  @SubscribeEvent
  public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.getConnection() != null && minecraft.player != null) {
      NeoForge1211ClientFileCheckClientRuntime.begin(
          minecraft.getConnection(), minecraft.player.getUUID());
    }
  }

  @SubscribeEvent
  public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
    NeoForge1211ClientFileCheckClientRuntime.invalidate();
  }

  @SubscribeEvent
  public static void onStopping(GameShuttingDownEvent event) {
    NeoForge1211ClientFileCheckClientRuntime.stop();
  }
}

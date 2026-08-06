package com.mo.economy_system.target.forge1201.client;

import com.mo.economy_system.target.forge1201.EconomySystemForge1201;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
    modid = EconomySystemForge1201.MODID,
    value = Dist.CLIENT,
    bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Forge1201ClientFileCheckEvents {
  private Forge1201ClientFileCheckEvents() {}

  @SubscribeEvent
  public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.getConnection() != null && minecraft.player != null) {
      Forge1201ClientFileCheckClientRuntime.begin(
          minecraft.getConnection(), minecraft.player.getUUID());
      Forge1201ClientFileCheckClientRuntime.transfers()
          .bindArrivalConnection(minecraft.getConnection().getConnection());
    }
  }

  @SubscribeEvent
  public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
    Forge1201ClientFileCheckClientRuntime.invalidate();
  }

  @SubscribeEvent
  public static void onStopping(GameShuttingDownEvent event) {
    Forge1201ClientFileCheckClientRuntime.stop();
  }

  @SubscribeEvent
  public static void onClientTick(TickEvent.ClientTickEvent event) {
    if (event.phase != TickEvent.Phase.END
        || Forge1201ClientFileCheckClientRuntime.isStopped()) return;
    try {
      Forge1201ClientFileCheckClientRuntime.transfers().tick(System.nanoTime());
    } catch (RuntimeException ignored) {
      // A transient provider/runtime failure must not take down the client.
    }
  }
}

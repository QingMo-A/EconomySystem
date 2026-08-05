package com.mo.economy_system.target.forge1201.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "economy_system")
public final class Forge1201CheckedFileTransferEvents {
  private Forge1201CheckedFileTransferEvents() {}

  @SubscribeEvent
  public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
      Forge1201ClientFileCheckRuntime.discardPlayer(player.getServer(), player.getUUID());
    }
  }

  @SubscribeEvent
  public static void onServerStopping(ServerStoppingEvent event) {
    Forge1201ClientFileCheckRuntime.stop(event.getServer());
  }
}

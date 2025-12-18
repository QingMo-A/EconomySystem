package com.mo.economy_system.server;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GetServerInstance {

    public static MinecraftServer SERVER_INSTANCE;

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        SERVER_INSTANCE = event.getServer();
    }
}

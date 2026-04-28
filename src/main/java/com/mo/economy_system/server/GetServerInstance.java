package com.mo.economy_system.server;

import net.minecraft.server.MinecraftServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber
public class GetServerInstance {

    public static MinecraftServer SERVER_INSTANCE;

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        SERVER_INSTANCE = event.getServer();
    }
}

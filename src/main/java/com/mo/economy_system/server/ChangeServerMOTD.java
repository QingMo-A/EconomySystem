package com.mo.economy_system.server;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.minecraft.server.dedicated.DedicatedServer;
import com.mo.economy_system.EconomySystem;

@EventBusSubscriber(modid = EconomySystem.MODID)
public class ChangeServerMOTD {

    @SubscribeEvent
    public static void onServerStart(ServerStartingEvent event) {
        if (event.getServer() instanceof DedicatedServer dedicatedServer) {
            String dynamicMOTD = "§6§l✦ §b§lDreaming§d§lFish §6§l✦\n§c§l守望梦屿 §7| §a梦屿的故事，由你书写... §8✦ §a1.20.1";
            dedicatedServer.setMotd(dynamicMOTD);
        }
    }
}
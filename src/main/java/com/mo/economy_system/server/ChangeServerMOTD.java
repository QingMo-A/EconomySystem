package com.mo.economy_system.server;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraft.server.dedicated.DedicatedServer;
import com.mo.economy_system.EconomySystem;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChangeServerMOTD {

    @SubscribeEvent
    public static void onServerStart(ServerStartingEvent event) {
        if (event.getServer() instanceof DedicatedServer dedicatedServer) {
            String dynamicMOTD = "§6§l✦ §b§lDreaming§d§lFish §6§l✦\n§c§l守望梦屿 §7| §a你，也可以是服务器的救世主 §8✦ §a1.20.1";
            dedicatedServer.setMotd(dynamicMOTD);
        }
    }
}
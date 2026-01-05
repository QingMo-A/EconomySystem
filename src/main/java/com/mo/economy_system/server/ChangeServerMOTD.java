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
            String dynamicMOTD = "§6Dreaming§dFish \n§c揭开一场丧尸危机，拯救整个服务器！";
            dedicatedServer.setMotd(dynamicMOTD);
        }
    }
}
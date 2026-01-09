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
            String dynamicMOTD = "§6Dreaming§dFish——§d梦鱼服|§6『守望梦屿』\n§c一个普普通通的世界...一场丧尸的秘密...";
            dedicatedServer.setMotd(dynamicMOTD);
        }
    }
}
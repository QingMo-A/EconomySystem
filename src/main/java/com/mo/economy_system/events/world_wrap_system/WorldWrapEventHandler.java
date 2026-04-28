package com.mo.economy_system.events.world_wrap_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.world_wrap_system.WorldWrapChunkMirrorManager;
import com.mo.economy_system.core.world_wrap_system.WorldWrapConfig;
import com.mo.economy_system.core.world_wrap_system.WorldWrapItemEntityManager;
import com.mo.economy_system.core.world_wrap_system.WorldWrapManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = EconomySystem.MODID)
public class WorldWrapEventHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        
        if (event.getEntity() instanceof ServerPlayer player) {
            WorldWrapManager.tickPlayer(player);
            WorldWrapChunkMirrorManager.tickPlayer(player, WorldWrapConfig.getConfig());
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ItemEntity itemEntity) {
            WorldWrapItemEntityManager.wrapIfNeeded(itemEntity);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WorldWrapChunkMirrorManager.clearPlayer(player);
            WorldWrapManager.clearPlayer(player);
        }
    }
}

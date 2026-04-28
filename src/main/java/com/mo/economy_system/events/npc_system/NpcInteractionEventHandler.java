package com.mo.economy_system.events.npc_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.npc_system.NpcManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = EconomySystem.MODID)
public class NpcInteractionEventHandler {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Entity target = event.getTarget();
        if (!target.getPersistentData().contains(NpcManager.ENTITY_NPC_ID_TAG)) {
            return;
        }

        int npcId = target.getPersistentData().getInt(NpcManager.ENTITY_NPC_ID_TAG);
        if (NpcManager.openNpcDialogue(player, npcId, target.getId())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }
}

package com.mo.economy_system.armor;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.armor.armors.SupporterHat;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ArmorTickHandler {
    // 每次玩家tick时，检查是否穿戴头盔
    @SubscribeEvent
    public static void onPlayerTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Player player) {
            SupporterHat.checkAndEnableRender(player);
        }
    }
}

package com.mo.economy_system.core.playerattributes_system;

import com.mo.economy_system.EconomySystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

//重生恢复所有默认状态
@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LoginDeathSync {
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        PlayerAttributesData data = PlayerAttributesDataManager.getPlayerAttributesData(player.getUUID());
        if (data != null) {
            data.setCurrentStrength(data.getMaxStrength());
            data.setCurrentCourage(data.getMaxCourage() / 2);
            data.syncMaxHealthToPlayer(player);
            player.setHealth((float) data.getMaxHealth());
            EconomySystem.LOGGER.info("玩家 {} 重生，同步最大生命值为{}",
                    player.getScoreboardName(), data.getMaxHealth());
        }
    }
}

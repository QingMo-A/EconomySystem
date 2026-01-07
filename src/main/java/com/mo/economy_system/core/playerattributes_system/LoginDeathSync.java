package com.mo.economy_system.core.playerattributes_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.courage.PlayerCourageClientSync;
import com.mo.economy_system.core.playerattributes_system.infection.PlayerInfectionClientSync;
import com.mo.economy_system.core.playerattributes_system.strength.StrengthSyncManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.mo.economy_system.core.playerattributes_system.PlayerAttributesDataManager.getPlayerAttributesData;

//重生恢复所有默认状态
@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LoginDeathSync {
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        PlayerAttributesData data = getPlayerAttributesData(player.getUUID());
        if (data != null) {
            data.setCurrentStrength(data.getMaxStrength());
            data.setCurrentCourage(data.getMaxCourage() / 2);
            data.syncMaxHealthToPlayer(player);
            player.setHealth((float) data.getMaxHealth());

            // 立即同步所有属性数据到客户端
            StrengthSyncManager.syncStrengthToClient(player);
            PlayerCourageClientSync.sendCourageDataToClient(
                    player,
                    data.getCurrentCourage(),
                    data.getMaxCourage()
            );
            PlayerInfectionClientSync.sendInfectionDataToClient(
                    player,
                    data.getCurrentInfection()
            );
//
//            EconomySystem.LOGGER.info("玩家 {} 重生，同步所有属性数据",
//                    player.getScoreboardName());
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {

            if (serverPlayer == null) return;
            PlayerAttributesData attrData = getPlayerAttributesData(serverPlayer.getUUID());
            StrengthSyncManager.syncStrengthToClient(serverPlayer);
            if (attrData == null) return;
            PlayerCourageClientSync.sendCourageDataToClient(
                    serverPlayer,
                    attrData.getCurrentCourage(),
                    attrData.getMaxCourage()
            );
            PlayerInfectionClientSync.sendInfectionDataToClient(
                    serverPlayer,
                    attrData.getCurrentInfection()
            );
            EconomySystem.LOGGER.info("玩家 {} 登录，同步属性：勇气值({}/{})，感染值({})",
                    serverPlayer.getScoreboardName(),
                    attrData.getCurrentCourage(),
                    attrData.getMaxCourage(),
                    attrData.getCurrentInfection()
            );
        }
    }
}

package com.mo.economy_system.core.playerattributes_system.infection;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.client.cache.ClientCacheManager;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesData;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesDataManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerInfectionManager {
    private static final int INFECTION_CHECK_INTERVAL = 40;
    private static final int INFECTION_MAX = 100;

    // 记录玩家已显示过的消息级别：0=无, 1=50%警告, 2=80%警告, 3=100%警告
    private static final Map<UUID, Integer> INFECTION_MSG_SHOWN = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        if (event.side.isClient() || !event.player.isAlive() || !(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
            return;
        }

        UUID playerUUID = serverPlayer.getUUID();
        PlayerAttributesData attributesData = PlayerAttributesDataManager.getPlayerAttributesData(playerUUID);
        if (attributesData == null) {
            return;
        }

        if (serverPlayer.tickCount % INFECTION_CHECK_INTERVAL != 0) {
            return;
        }

        float currentInfection = attributesData.getCurrentInfection();
        float infectionRatio = currentInfection / INFECTION_MAX;
        int msgShownLevel = INFECTION_MSG_SHOWN.getOrDefault(playerUUID, 0);

        if (infectionRatio >= 1.0F) {
            MobEffectInstance slownessEffect = new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true);
            MobEffectInstance weaknessEffect = new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, true);
            serverPlayer.addEffect(slownessEffect);
            serverPlayer.addEffect(weaknessEffect);

            if (msgShownLevel < 3) {
                serverPlayer.displayClientMessage(
                        Component.literal("§4§l您已成为感染者！"),
                        true
                );
                INFECTION_MSG_SHOWN.put(playerUUID, 3);
            }
        } else if (infectionRatio >= 0.8F) {
            MobEffectInstance slownessEffect = new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true);
            MobEffectInstance weaknessEffect = new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, true);
            serverPlayer.addEffect(slownessEffect);
            serverPlayer.addEffect(weaknessEffect);

            if (msgShownLevel < 2) {
                serverPlayer.displayClientMessage(
                        Component.literal("§c感染值过高，您的身体正在恶化..."),
                        true
                );
                INFECTION_MSG_SHOWN.put(playerUUID, 2);
            }
        } else if (infectionRatio >= 0.5F && infectionRatio < 0.8F) {
            if (msgShownLevel < 1) {
                serverPlayer.displayClientMessage(
                        Component.literal("§e您感到身体有些不适..."),
                        true
                );
                INFECTION_MSG_SHOWN.put(playerUUID, 1);
            }
        }
    }

    public static void addInfection(ServerPlayer player, float amount) {
        if (player == null || amount <= 0) {
            return;
        }

        UUID playerUUID = player.getUUID();
        PlayerAttributesData attributesData = PlayerAttributesDataManager.getPlayerAttributesData(playerUUID);
        if (attributesData == null) {
            return;
        }

        float currentInfection = attributesData.getCurrentInfection();
        float newInfection = Math.min(currentInfection + amount, INFECTION_MAX);

        if (Math.abs(newInfection - currentInfection) < 0.01F) {
            return;
        }

        attributesData.setCurrentInfection(newInfection);
        PlayerAttributesDataManager.updatePlayerAttributesData(player, attributesData);

        // 感染值达到100时转换为感染者
        if (newInfection >= INFECTION_MAX && !attributesData.isInfected()) {
            attributesData.setInfected(true);
            PlayerAttributesDataManager.updatePlayerAttributesData(player, attributesData);
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§4§l你已经完全感染，变成了感染者！"),
                    true
            );
        }

        PlayerInfectionClientSync.sendInfectionDataToClient(player, newInfection);
    }

    public static void reduceInfection(ServerPlayer player, float amount) {
        if (player == null || amount <= 0) {
            return;
        }

        UUID playerUUID = player.getUUID();
        PlayerAttributesData attributesData = PlayerAttributesDataManager.getPlayerAttributesData(playerUUID);
        if (attributesData == null) {
            return;
        }

        float currentInfection = attributesData.getCurrentInfection();
        float newInfection = Math.max(currentInfection - amount, 0);

        if (Math.abs(newInfection - currentInfection) < 0.01F) {
            return;
        }

        attributesData.setCurrentInfection(newInfection);
        PlayerAttributesDataManager.updatePlayerAttributesData(player, attributesData);

        // 感染值降低时重置消息级别，允许重新触发警告
        float newInfectionRatio = newInfection / INFECTION_MAX;
        int currentMsgLevel = INFECTION_MSG_SHOWN.getOrDefault(playerUUID, 0);
        int newMsgLevel = currentMsgLevel;

        if (newInfectionRatio < 0.5F) {
            newMsgLevel = 0; // 低于50%，重置所有警告
        } else if (newInfectionRatio < 0.8F && currentMsgLevel >= 2) {
            newMsgLevel = 1; // 低于80%但高于50%，重置80%和100%警告
        } else if (newInfectionRatio < 1.0F && currentMsgLevel >= 3) {
            newMsgLevel = 2; // 低于100%但高于80%，只重置100%警告
        }

        if (newMsgLevel != currentMsgLevel) {
            if (newMsgLevel == 0) {
                INFECTION_MSG_SHOWN.remove(playerUUID);
            } else {
                INFECTION_MSG_SHOWN.put(playerUUID, newMsgLevel);
            }
        }

        PlayerInfectionClientSync.sendInfectionDataToClient(player, newInfection);
    }

    public static void setCurrentInfectionClient(Player player, float currentInfection) {
        if (player == null || !player.level().isClientSide()) {
            return;
        }
        PlayerAttributesData data = ClientCacheManager.getOrCreatePlayerAttributesData(player.getUUID());
        data.setCurrentInfection(currentInfection);
        ClientCacheManager.setPlayerAttributesData(player.getUUID(), data);
    }

    public static float getCurrentInfectionClient(Player player) {
        if (player == null || !player.level().isClientSide()) {
            return 0;
        }
        PlayerAttributesData data = ClientCacheManager.getPlayerAttributesData(player.getUUID());
        return data != null ? data.getCurrentInfection() : 0;
    }
}
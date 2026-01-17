package com.mo.economy_system.core.playerattributes_system.infection;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesData;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesDataManager;
import com.mo.economy_system.entity.entities.HiveZombieEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerInfectionManager {
    private static final int INFECTION_CHECK_INTERVAL = 40;
    private static final int INFECTION_MAX = 100;

    private static final Map<UUID, Integer> CLIENT_CURRENT_INFECTION = new ConcurrentHashMap<>();

    private static final Map<UUID, Integer> INFECTION_MSG_COOLDOWN = new ConcurrentHashMap<>();
    private static final int MSG_COOLDOWN_TICKS = 1200;

    static {
        CLIENT_CURRENT_INFECTION.put(new UUID(0, 0), 0);
    }

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

        int currentCooldown = INFECTION_MSG_COOLDOWN.getOrDefault(playerUUID, 0);
        if (currentCooldown > 0) {
            INFECTION_MSG_COOLDOWN.put(playerUUID, currentCooldown - 1);
        }
        boolean canShowMsg = currentCooldown <= 0;

        if (serverPlayer.tickCount % INFECTION_CHECK_INTERVAL != 0) {
            return;
        }

        int currentInfection = attributesData.getCurrentInfection();
        float infectionRatio = (float) currentInfection / INFECTION_MAX;

        if (infectionRatio >= 0.8F) {
            MobEffectInstance slownessEffect = new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true);
            MobEffectInstance weaknessEffect = new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, true);
            serverPlayer.addEffect(slownessEffect);
            serverPlayer.addEffect(weaknessEffect);

            if (canShowMsg) {
                serverPlayer.displayClientMessage(
                        Component.literal("§c感染值过高，您的身体正在恶化..."),
                        true
                );
                INFECTION_MSG_COOLDOWN.put(playerUUID, MSG_COOLDOWN_TICKS);
            }
        } else if (infectionRatio >= 0.5F && infectionRatio < 0.8F) {
            if (canShowMsg) {
                serverPlayer.displayClientMessage(
                        Component.literal("§e您感到身体有些不适..."),
                        true
                );
                INFECTION_MSG_COOLDOWN.put(playerUUID, MSG_COOLDOWN_TICKS);
            }
        }
    }

    public static void addInfection(ServerPlayer player, int amount) {
        if (player == null || amount <= 0) {
            return;
        }

        UUID playerUUID = player.getUUID();
        PlayerAttributesData attributesData = PlayerAttributesDataManager.getPlayerAttributesData(playerUUID);
        if (attributesData == null) {
            return;
        }

        int currentInfection = attributesData.getCurrentInfection();
        int newInfection = Math.min(currentInfection + amount, INFECTION_MAX);

        if (currentInfection == newInfection) {
            return;
        }

        attributesData.setCurrentInfection(newInfection);
        PlayerAttributesDataManager.updatePlayerAttributesData(player, attributesData);

        PlayerInfectionClientSync.sendInfectionDataToClient(player, newInfection);
    }

    public static void reduceInfection(ServerPlayer player, int amount) {
        if (player == null || amount <= 0) {
            return;
        }

        UUID playerUUID = player.getUUID();
        PlayerAttributesData attributesData = PlayerAttributesDataManager.getPlayerAttributesData(playerUUID);
        if (attributesData == null) {
            return;
        }

        int currentInfection = attributesData.getCurrentInfection();
        int newInfection = Math.max(currentInfection - amount, 0);

        if (currentInfection == newInfection) {
            return;
        }

        attributesData.setCurrentInfection(newInfection);
        PlayerAttributesDataManager.updatePlayerAttributesData(player, attributesData);

        PlayerInfectionClientSync.sendInfectionDataToClient(player, newInfection);
    }

    public static void setCurrentInfectionClient(Player player, int currentInfection) {
        if (player == null || !player.level().isClientSide()) {
            return;
        }
        CLIENT_CURRENT_INFECTION.put(player.getUUID(), currentInfection);
    }

    public static int getCurrentInfectionClient(Player player) {
        if (player == null || !player.level().isClientSide()) {
            return 0;
        }
        return CLIENT_CURRENT_INFECTION.getOrDefault(player.getUUID(), 0);
    }
}

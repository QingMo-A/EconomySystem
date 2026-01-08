package com.mo.economy_system.core.auth_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.auth_system.network.ServerAuthHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 玩家认证事件处理器
 * 在玩家加入服务器时进行拦截
 */
@Mod.EventBusSubscriber(modid = EconomySystem.MODID)
public class EventHandler_PlayerAuth {

    /**
     * 玩家加入世界时的处理
     * 这在PlayerList.placeNewPlayer之后触发
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getLevel() instanceof ServerLevel level && !level.isClientSide) {
            EconomySystem.LOGGER.info("EventHandler_PlayerAuth.onPlayerJoin 被调用，玩家: " + player.getName().getString());

            // 检查玩家是否已经通过认证
            AuthSavedData data = AuthSavedData.getInstance(level);

            if (!data.isLoggedIn(player.getUUID())) {
                EconomySystem.LOGGER.info("玩家未登录，设置为旁观者模式");

                // 设置为旁观者模式
                player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);

                // 发送认证挑战
                ServerAuthHandler.sendAuthChallenge(player);

                // 玩家未登录，发送系统消息
                if (!data.isRegistered(player.getUUID())) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§e欢迎来到服务器！请先注册账号。"));
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c你处于旁观者模式，登录后将自动切换到生存模式。"));
                } else {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§e欢迎回来！请先登录。"));
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c你处于旁观者模式，登录后将自动切换到生存模式。"));
                }
            } else {
                // 玩家已登录，确保是生存模式
                if (player.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.SPECTATOR) {
                    player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                }
            }
        }
    }

    /**
     * 监听玩家tick，确保未登录玩家保持旁观者模式
     * 如果玩家已经登录但没有登录状态，重新发送认证挑战
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        ServerLevel level = player.serverLevel();
        AuthSavedData data = AuthSavedData.getInstance(level);

        if (!data.isLoggedIn(player.getUUID())) {
            // 玩家未登录，保持旁观者模式
            if (player.gameMode.getGameModeForPlayer() != net.minecraft.world.level.GameType.SPECTATOR) {
                player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            }

            // 每隔5秒（100 tick）检查一次是否需要重新发送认证挑战
            if (player.tickCount % 100 == 0 && !ServerAuthHandler.isPendingAuth(player.getUUID())) {
                EconomySystem.LOGGER.info("玩家未登录且没有待认证连接，重新发送认证挑战");
                ServerAuthHandler.sendAuthChallenge(player);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§e你还未登录！请完成登录后继续游戏。"));
            }
        }
    }

    /**
     * 玩家离开世界时的处理
     * 不清除登录状态，死亡重生时保持登录
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 只清除待认证连接，不清除登录状态
            ServerAuthHandler.removePendingConnection(player.getUUID());
        }
    }

    /**
     * 玩家重生时的处理
     * 保持登录状态
     */
    @SubscribeEvent
    public static void onPlayerRespawn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 玩家重生，保持登录状态不变
            ServerLevel level = player.serverLevel();
            AuthSavedData data = AuthSavedData.getInstance(level);

            // 如果未登录，确保保持旁观者模式
            if (!data.isLoggedIn(player.getUUID())) {
                player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            }
        }
    }
}

package com.mo.economy_system.server.serverui;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.Packet_SystemMessage;
import com.mo.economy_system.server.rank.PlayerRankManager;
import com.mo.economy_system.server.rank.Rank;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.FrameType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;

/**
 * 系统消息事件处理器
 * 监听原版事件，将系统消息发送到右上角显示
 */
@Mod.EventBusSubscriber(modid = EconomySystem.MODID)
public class SystemMessageEventHandler {

    // 颜色定义（与客户端 SystemMessageDisplay 保持一致）
    private static final int COLOR_TASK = 0x55FF55;         // 普通进度 - 绿色
    private static final int COLOR_GOAL = 0x55FFFF;         // 目标 - 蓝色
    private static final int COLOR_CHALLENGE = 0xAA00AA;    // 挑战 - 紫色

    // ==================== 进度/成就消息 ====================
    @SubscribeEvent
    public static void onAdvancement(net.minecraftforge.event.entity.player.AdvancementEvent.AdvancementEarnEvent event) {
        Advancement advancement = event.getAdvancement();
        ServerPlayer player = (ServerPlayer) event.getEntity();

        if (advancement.getDisplay() == null) return; // 无显示信息的进度跳过

        // 获取进度类型和标题
        FrameType frame = advancement.getDisplay().getFrame();
        Component title = advancement.getDisplay().getTitle();

        // 获取玩家 Rank
        Rank playerRank = PlayerRankManager.getPlayerRankServer(player);

        // 根据帧类型确定文本、边框颜色和标题颜色
        String typeText;
        int borderColor;
        ChatFormatting titleColor;
        switch (frame) {
            case CHALLENGE:
                typeText = "挑战";
                borderColor = COLOR_CHALLENGE; // 紫色
                titleColor = ChatFormatting.DARK_PURPLE;
                break;
            case GOAL:
                typeText = "目标";
                borderColor = COLOR_GOAL; // 蓝色
                titleColor = ChatFormatting.AQUA;
                break;
            default:
                typeText = "进度";
                borderColor = COLOR_TASK; // 绿色
                titleColor = ChatFormatting.GREEN;
                break;
        }

        // 构造消息：[RANK]? 玩家名 完成了进度/挑战[标题]
        MutableComponent message = Component.literal("");

        // 只有非 NO_RANK 的玩家才显示 [RANK] 前缀
        String rankPrefix = getRankDisplayName(playerRank);
        if (rankPrefix != null) {
            Integer customColor = getRankCustomColor(playerRank);
            if (customColor != null) {
                // 使用自定义颜色（FISH++ 金色）
                message = message.append(Component.literal("[" + rankPrefix + "] ").withStyle(style -> style.withColor(customColor)));
            } else {
                // 使用 ChatFormatting 颜色
                message = message.append(Component.literal("[" + rankPrefix + "] ").withStyle(style -> style.withColor(getRankColorCode(playerRank))));
            }
        }

        message = message
                .append(player.getDisplayName())
                .append(Component.literal(" 完成了" + typeText).withStyle(style -> style.withColor(ChatFormatting.WHITE)))
                .append(Component.literal("[").withStyle(style -> style.withColor(titleColor))) // 左括号同标题颜色
                .append(title.copy().withStyle(style -> style.withColor(titleColor))) // 标题根据类型着色
                .append(Component.literal("]").withStyle(style -> style.withColor(titleColor))); // 右括号同标题颜色

        // 发送到所有玩家的右上角
        broadcastSystemMessage(player, message, borderColor);
    }

    // ==================== 死亡消息 ====================
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DamageSource damageSource = event.getSource();

        // 获取玩家 Rank
        Rank playerRank = PlayerRankManager.getPlayerRankServer(player);

        // 构造消息：[RANK]? 玩家名 死亡消息
        MutableComponent deathMessage = Component.literal("");

        // 只有非 NO_RANK 的玩家才显示 [RANK] 前缀
        String rankPrefix = getRankDisplayName(playerRank);
        if (rankPrefix != null) {
            Integer customColor = getRankCustomColor(playerRank);
            if (customColor != null) {
                // 使用自定义颜色（FISH++ 金色）
                deathMessage = deathMessage.append(Component.literal("[" + rankPrefix + "] ").withStyle(style -> style.withColor(customColor)));
            } else {
                // 使用 ChatFormatting 颜色
                deathMessage = deathMessage.append(Component.literal("[" + rankPrefix + "] ").withStyle(style -> style.withColor(getRankColorCode(playerRank))));
            }
        }

        deathMessage = deathMessage.append(damageSource.getLocalizedDeathMessage(player));

        // 发送到所有玩家的右上角（使用该玩家的 Rank 颜色）
        int borderColor = getRankBorderColor(playerRank);
        broadcastSystemMessage(player, deathMessage, borderColor);
    }

    // ==================== 离服消息 ====================
    // 已禁用 - 由 ChangeJoinMessage 处理（支持 Rank 功能）
    /*
    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Component message = Component.literal("")
                .append(player.getDisplayName())
                .append(Component.literal(" 离开了游戏").withStyle(style -> style.withColor(0xFFFF00)));

        // 发送到所有玩家的右上角
        broadcastSystemMessage(player, message);
    }
    */

    // ==================== 通用发送方法 ====================

    // ==================== 进服消息 ====================
    // 已禁用 - 由 ChangeJoinMessage 处理（支持 Rank 功能）
    /*
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Component message = Component.literal("")
                .append(player.getDisplayName())
                .append(Component.literal(" 加入了游戏").withStyle(style -> style.withColor(0xFFFF00)));

        // 发送到所有玩家的右上角
        broadcastSystemMessage(player, message);
    }
    */

    // ==================== 离服消息 ====================
    // 已禁用 - 由 ChangeJoinMessage 处理（支持 Rank 功能）
    /*
    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Component message = Component.literal("")
                .append(player.getDisplayName())
                .append(Component.literal(" 离开了游戏").withStyle(style -> style.withColor(0xFFFF00)));

        // 发送到所有玩家的右上角
        broadcastSystemMessage(player, message);
    }
    */

    // ==================== 通用发送方法 ====================
    private static void broadcastSystemMessage(ServerPlayer source, Component message, int borderColor) {
        // 获取服务器
        var server = source.getServer();
        if (server == null) return;

        // 发送到所有在线玩家的右上角
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            EconomySystem_NetworkManager.INSTANCE.sendTo(
                    new Packet_SystemMessage(message, borderColor),
                    player.connection.connection,
                    NetworkDirection.PLAY_TO_CLIENT
            );
        }
    }

    // ==================== 辅助方法 ====================
    /**
     * 获取 Rank 显示名称（NO_RANK 返回 null，表示不显示前缀）
     */
    private static String getRankDisplayName(Rank rank) {
        if (rank == null) return null;
        return switch (rank.getRankName()) {
            case "NO_RANK" -> null;  // 不显示前缀
            case "FISH" -> "FISH";
            case "FISH+" -> "FISH+";
            case "FISH++" -> "FISH++";
            case "OPERATOR" -> "OPERATOR";
            default -> null;
        };
    }

    /**
     * 获取 Rank 的聊天颜色代码
     * 注意：FISH++ 使用自定义金色（0xFFAA00），而不是 ChatFormatting.GOLD（黄色）
     */
    private static ChatFormatting getRankColorCode(Rank rank) {
        if (rank == null) return ChatFormatting.GRAY;
        return switch (rank.getRankName()) {
            case "FISH" -> ChatFormatting.GREEN;
            case "FISH+" -> ChatFormatting.AQUA;
            case "FISH++" -> ChatFormatting.YELLOW; // 使用 YELLOW，然后通过自定义颜色覆盖为金色
            case "OPERATOR" -> ChatFormatting.RED;
            default -> ChatFormatting.GRAY;
        };
    }

    /**
     * 获取 Rank 的自定义颜色（用于覆盖 ChatFormatting）
     * 返回 null 表示使用 ChatFormatting 默认颜色
     */
    private static Integer getRankCustomColor(Rank rank) {
        if (rank == null) return null;
        return switch (rank.getRankName()) {
            case "FISH++" -> 0xFFAA00;  // 真正的金色
            default -> null;
        };
    }

    /**
     * 获取Rank对应的边框颜色
     */
    private static int getRankBorderColor(Rank rank) {
        if (rank == null) return 0xAAAAAA;  // 默认灰色
        return switch (rank.getRankName()) {
            case "NO_RANK" -> 0xAAAAAA;     // 灰色
            case "FISH" -> 0x55FF55;        // 绿色
            case "FISH+" -> 0x55FFFF;       // 蓝色
            case "FISH++" -> 0xFFAA00;      // 金色
            case "OPERATOR" -> 0xFF5555;    // 红色
            default -> 0xAAAAAA;
        };
    }
}

package com.mo.economy_system.network.packets.playerdata_system;

import com.mo.economy_system.screen.server_screen.customsystemui.SystemMessageDisplay;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import com.mo.economy_system.compat.DistExecutor;
import com.mo.economy_system.compat.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 原版进度完成通知网络包（服务端→客户端）
 * 在右侧系统消息区域显示"xxx 获得了进度 xxx"
 */
public class Packet_VanillaAdvancementNotify {
    private final String playerName;
    private final String advancementTitle;
    private final String frameType;  // TASK, GOAL, CHALLENGE
    private final boolean isHidden;

    public Packet_VanillaAdvancementNotify(String playerName, String advancementTitle, String frameType, boolean isHidden) {
        this.playerName = playerName;
        this.advancementTitle = advancementTitle;
        this.frameType = frameType;
        this.isHidden = isHidden;
    }

    public static void encode(Packet_VanillaAdvancementNotify packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.playerName);
        buf.writeUtf(packet.advancementTitle);
        buf.writeUtf(packet.frameType);
        buf.writeBoolean(packet.isHidden);
    }

    public static Packet_VanillaAdvancementNotify decode(FriendlyByteBuf buf) {
        String playerName = buf.readUtf();
        String advancementTitle = buf.readUtf();
        String frameType = buf.readUtf();
        boolean isHidden = buf.readBoolean();
        return new Packet_VanillaAdvancementNotify(playerName, advancementTitle, frameType, isHidden);
    }

    public static void handle(Packet_VanillaAdvancementNotify packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                // 根据类型选择颜色
                ChatFormatting color = switch (packet.frameType) {
                    case "TASK" -> ChatFormatting.GREEN;
                    case "GOAL" -> ChatFormatting.YELLOW;
                    case "CHALLENGE" -> ChatFormatting.DARK_PURPLE;
                    default -> ChatFormatting.WHITE;
                };

                // 构建显示文本：xxx 获得了 任务/目标/挑战：xxx
                Component message = Component.literal(packet.playerName + " ")
                        .withStyle(ChatFormatting.WHITE)
                        .append(Component.literal("获得了")
                        .withStyle(ChatFormatting.GRAY))
                        .append(Component.literal((packet.isHidden ? "隐藏" : "") + getTypeDisplayName(packet.frameType))
                                .withStyle(color))
                        .append(Component.literal("：")
                                .withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(packet.advancementTitle)
                                .withStyle(ChatFormatting.WHITE));

                // 添加到系统消息显示（右侧）
                SystemMessageDisplay.addMessage(message);
            });
        });
        ctx.get().setPacketHandled(true);
    }

    private static String getTypeDisplayName(String frameType) {
        return switch (frameType) {
            case "TASK" -> "任务";
            case "GOAL" -> "目标";
            case "CHALLENGE" -> "挑战";
            default -> "进度";
        };
    }
}

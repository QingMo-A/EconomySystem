package com.mo.economy_system.core.economy_system.red_packet;

import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RedPacketManager {

    private static final Map<UUID, RedPacket> redPackets = new HashMap<>(); // 玩家对应红包

    /**
     * 添加一个红包
     */
    public static boolean addRedPacket(UUID senderUUID, RedPacket redPacket) {
        if (redPackets.containsKey(senderUUID)) {
            return false; // 玩家已有未销毁的红包
        }
        redPackets.put(senderUUID, redPacket);
        return true;
    }

    /**
     * 移除一个红包
     */
    public static boolean removeRedPacket(UUID senderUUID) {

        redPackets.remove(senderUUID);
        return true;
    }

    /**
     * 获取最近可抢的红包
     */
    public static RedPacket getRecentRedPacket() {
        return redPackets.values().stream()
                .filter(RedPacket::isClaimable)
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取指定玩家的红包
     */
    public static RedPacket getRedPacketBySender(UUID senderUUID) {
        return redPackets.get(senderUUID);
    }

    /**
     * 检查并处理过期红包
     */
    public static void checkAndExpireRedPackets() {
        long currentTime = System.currentTimeMillis();

        redPackets.entrySet().removeIf(entry -> {
            RedPacket redPacket = entry.getValue();

            // 检查是否过期
            if (currentTime > redPacket.expirationTime) {
                handleExpiredRedPacket(redPacket);

                // 输出日志（仅供调试和跟踪）
                System.out.println("Red packet from " + redPacket.senderName + " expired. Refunded remaining amount.");

                return true; // 移除过期红包
            }
            return false;
        });
    }

    /**
     * 处理过期红包
     */
    public static void handleExpiredRedPacket(RedPacket redPacket) {
        // 退还未抢金额给发送者
        ServerPlayer sender = getPlayerByUUID(redPacket.senderUUID);
        if (sender != null && redPacket.totalAmount > redPacket.claimedAmount) {
            int remainingAmount = redPacket.totalAmount - redPacket.claimedAmount;
            EconomySavedData data = EconomySavedData.getInstance(sender.serverLevel());
            data.addBalance(redPacket.senderUUID, remainingAmount, "红包", "红包过期退款");

            sender.sendSystemMessage(Component.translatable(Util_MessageKeys.RED_PACKET_EXPIRED_REFUNDED, remainingAmount));
        }

        // 广播全服红包过期消息
        if (sender != null) {
            sender.getServer().getPlayerList().broadcastSystemMessage(
                    Component.translatable(Util_MessageKeys.RED_PACKET_EXPIRED_BROADCAST, redPacket.senderName), false);
        }
    }


    /**
     * 根据 UUID 获取玩家对象
     */
    private static ServerPlayer getPlayerByUUID(UUID uuid) {
        MinecraftServer server = EconomyServices.platform().currentServer();
        return server == null ? null : server.getPlayerList().getPlayer(uuid);
    }
}


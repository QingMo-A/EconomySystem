package com.mo.economy_system.core.economy_system.red_packet;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RedPacket {
    public final UUID senderUUID;       // 发送者 UUID
    public final String senderName;    // 发送者名字
    public int totalAmount;            // 红包总金额
    public int totalCount;            // 红包总数量
    public int claimedAmount;          // 已被抢金额
    public boolean isLucky;            // 是否为拼手气红包
    public long expirationTime;        // 过期时间戳（毫秒）
    public Set<UUID> claimedPlayers;   // 已抢红包的玩家 UUID 列表

    public RedPacket(UUID senderUUID, String senderName, int totalAmount, boolean isLucky, long durationMinutes, int totalCount) {
        this.senderUUID = senderUUID;
        this.senderName = senderName;
        this.totalAmount = totalAmount;
        this.totalCount = totalCount;
        this.claimedAmount = 0;
        this.isLucky = isLucky;
        this.expirationTime = System.currentTimeMillis() + durationMinutes * 60 * 1000; // 持续时间（分钟转换为毫秒）
        this.claimedPlayers = new HashSet<>();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTime;
    }

    public boolean isClaimable() {
        return totalAmount > claimedAmount && !isExpired();
    }

    public boolean hasClaimed(UUID playerUUID) {
        return claimedPlayers.contains(playerUUID);
    }

    public void addClaimedPlayer(UUID playerUUID) {
        claimedPlayers.add(playerUUID);
    }
}


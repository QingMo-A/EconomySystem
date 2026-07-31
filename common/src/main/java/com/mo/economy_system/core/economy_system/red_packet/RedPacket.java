package com.mo.economy_system.core.economy_system.red_packet;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RedPacket {
    public final UUID senderUUID;
    public final String senderName;
    public int totalAmount;
    public int totalCount;
    public int claimedAmount;
    public boolean isLucky;
    public long expirationTime;
    public Set<UUID> claimedPlayers;

    public RedPacket(
            UUID senderUUID,
            String senderName,
            int totalAmount,
            boolean isLucky,
            long durationMinutes,
            int totalCount
    ) {
        this.senderUUID = senderUUID;
        this.senderName = senderName;
        this.totalAmount = totalAmount;
        this.totalCount = totalCount;
        this.claimedAmount = 0;
        this.isLucky = isLucky;
        this.expirationTime = System.currentTimeMillis() + durationMinutes * 60_000L;
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

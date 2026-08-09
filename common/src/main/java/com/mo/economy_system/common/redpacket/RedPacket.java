package com.mo.economy_system.common.redpacket;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Immutable server-authoritative state for one red packet. */
public record RedPacket(
    UUID senderId,
    String senderName,
    int totalAmount,
    int totalCount,
    int claimedAmount,
    Mode mode,
    long createdAtMillis,
    long expirationTimeMillis,
    Set<UUID> claimedPlayers) {

  public RedPacket {
    Objects.requireNonNull(senderId, "senderId");
    senderName = validName(senderName, "senderName");
    if (totalAmount <= 0) throw new IllegalArgumentException("totalAmount");
    if (totalCount <= 0) throw new IllegalArgumentException("totalCount");
    if (claimedAmount < 0 || claimedAmount > totalAmount) {
      throw new IllegalArgumentException("claimedAmount");
    }
    Objects.requireNonNull(mode, "mode");
    if (expirationTimeMillis <= createdAtMillis) {
      throw new IllegalArgumentException("expirationTimeMillis");
    }
    Objects.requireNonNull(claimedPlayers, "claimedPlayers");
    if (claimedPlayers.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("claimedPlayers");
    }
    if (claimedPlayers.size() > totalCount) {
      throw new IllegalArgumentException("claimedPlayers exceeds totalCount");
    }
    claimedPlayers = Set.copyOf(claimedPlayers);
  }

  public enum Mode {
    LUCKY,
    EVEN
  }

  public boolean isLucky() {
    return mode == Mode.LUCKY;
  }

  public boolean isExpired(long nowMillis) {
    return nowMillis >= expirationTimeMillis;
  }

  public int remainingAmount() {
    return totalAmount - claimedAmount;
  }

  public int remainingPlayers() {
    return totalCount - claimedPlayers.size();
  }

  public boolean isClaimable(long nowMillis) {
    return !isExpired(nowMillis) && remainingAmount() > 0 && remainingPlayers() > 0;
  }

  public boolean hasClaimed(UUID playerId) {
    return claimedPlayers.contains(Objects.requireNonNull(playerId, "playerId"));
  }

  public RedPacket claimedBy(UUID playerId, int amount) {
    Objects.requireNonNull(playerId, "playerId");
    if (amount <= 0 || amount > remainingAmount()) throw new IllegalArgumentException("amount");
    if (hasClaimed(playerId)) throw new IllegalArgumentException("player already claimed");
    Set<UUID> updated = new java.util.HashSet<>(claimedPlayers);
    updated.add(playerId);
    return new RedPacket(
        senderId,
        senderName,
        totalAmount,
        totalCount,
        claimedAmount + amount,
        mode,
        createdAtMillis,
        expirationTimeMillis,
        updated);
  }

  private static String validName(String value, String field) {
    Objects.requireNonNull(value, field);
    String normalized = value.trim();
    if (normalized.isEmpty() || normalized.length() > 64) {
      throw new IllegalArgumentException(field);
    }
    return normalized;
  }
}

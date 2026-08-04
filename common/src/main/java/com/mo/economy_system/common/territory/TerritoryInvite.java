package com.mo.economy_system.common.territory;

import java.util.Objects;
import java.util.UUID;

public record TerritoryInvite(UUID inviteId, UUID territoryId, UUID territoryOwnerId,
    UUID inviterId, UUID targetPlayerId, String territoryName, String inviterName,
    String targetPlayerName, long createdTick, long expiresTick) {
  public TerritoryInvite {
    Objects.requireNonNull(inviteId, "inviteId"); Objects.requireNonNull(territoryId, "territoryId");
    Objects.requireNonNull(territoryOwnerId, "territoryOwnerId"); Objects.requireNonNull(inviterId, "inviterId");
    Objects.requireNonNull(targetPlayerId, "targetPlayerId");
    territoryName = name(territoryName, "territoryName"); inviterName = name(inviterName, "inviterName");
    targetPlayerName = name(targetPlayerName, "targetPlayerName");
    if (!territoryOwnerId.equals(inviterId)) throw new IllegalArgumentException("inviter must be owner");
    if (targetPlayerId.equals(territoryOwnerId)) throw new IllegalArgumentException("target is owner");
    if (createdTick < 0 || expiresTick <= createdTick) throw new IllegalArgumentException("invalid ticks");
  }
  private static String name(String value, String field) {
    Objects.requireNonNull(value, field); String result=value.trim();
    if (result.isEmpty() || result.length()>128) throw new IllegalArgumentException(field);
    return result;
  }
}

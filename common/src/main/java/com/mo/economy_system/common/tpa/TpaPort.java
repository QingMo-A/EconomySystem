package com.mo.economy_system.common.tpa;

import java.util.Optional;
import java.util.UUID;

/** Minecraft-facing operations retained in a target adapter. */
public interface TpaPort {
  enum TeleportArrival {
    ARRIVED,
    NOT_ARRIVED,
    UNKNOWN
  }

  interface PotionReservation {
    int slot();

    void commit();

    void rollback() throws Exception;
  }

  boolean isOnline(UUID playerId);

  boolean hasWormholePotion(UUID playerId);

  Optional<PotionReservation> reserveWormholePotion(UUID playerId) throws Exception;

  void teleport(UUID senderId, UUID targetId) throws Exception;

  TeleportArrival arrival(UUID senderId, UUID targetId);

  void effects(UUID senderId, UUID targetId) throws Exception;
}

package com.mo.economy_system.common.commission;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/**
 * Stable idempotency keys for server-originated commission events.
 *
 * <p>Target adapters should derive one key from the authoritative event identity and pass it to
 * the explicit {@link CommissionService#submitProgress(UUID, UUID, UUID, int, long)} overload.
 * The old overloads remain available for callers that do not have an event identity.
 */
public final class CommissionEventIds {
  private static final String ENTITY_KILL_NAMESPACE =
      "economysystem:commission:entity-kill:v1:";

  private CommissionEventIds() {}

  /**
   * Returns the same submission key whenever the same player is credited for the same entity
   * death.  An entity UUID is assigned by Minecraft for the entity's lifetime and is therefore a
   * better event identity than the entity type or a wall-clock timestamp.
   */
  public static UUID entityKill(UUID playerId, UUID killedEntityId) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(killedEntityId, "killedEntityId");
    String seed = ENTITY_KILL_NAMESPACE + playerId + ":" + killedEntityId;
    return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
  }
}

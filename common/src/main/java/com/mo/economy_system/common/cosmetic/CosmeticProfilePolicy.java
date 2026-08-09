package com.mo.economy_system.common.cosmetic;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Shared identity and metadata rules for the cosmetic head items. */
public final class CosmeticProfilePolicy {
  public static final String SUPPORTER_UUID_KEY = "supporter_uuid";
  public static final String SUPPORTER_NAME_KEY = "supporter_name";
  public static final String SKIN_UUID_KEY = "player_doll_skin_uuid";
  public static final String SKIN_NAME_KEY = "player_doll_skin_name";
  public static final String SKIN_SLIM_KEY = "player_doll_skin_slim";

  private CosmeticProfilePolicy() {}

  public static Optional<UUID> parseUuid(String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(UUID.fromString(value));
    } catch (IllegalArgumentException ignored) {
      return Optional.empty();
    }
  }

  public static String nameOrFallback(String name, String fallback) {
    String candidate = name == null ? "" : name.trim();
    if (!candidate.isEmpty()) {
      return candidate;
    }
    return Objects.requireNonNullElse(fallback, "");
  }

  public static boolean canBindSupporter(UUID supporterUuid, String supporterName) {
    return supporterUuid != null && !nameOrFallback(supporterName, "").isBlank();
  }

  public record DollProfile(UUID playerUuid, String playerName, boolean slimModel) {
    public DollProfile {
      playerUuid = Objects.requireNonNull(playerUuid, "playerUuid");
      playerName = nameOrFallback(playerName, playerUuid.toString());
    }
  }
}

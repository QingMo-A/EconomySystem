package com.mo.economy_system.common.territory;

import com.mo.economy_system.common.network.TransferTerritoryOwnershipMessage;
import com.mo.economy_system.common.network.UpdateTerritoryPermissionMessage;
import com.mo.economy_system.common.network.UpdateTerritoryRuleMessage;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Server-authoritative protocols 41-43. Client-provided display names are never accepted. */
public final class TerritoryAdministrationService {
  private TerritoryAdministrationService() {}

  public static TerritoryManagementResult permission(
      UpdateTerritoryPermissionMessage message, UUID senderId, Context context) {
    if (message == null || senderId == null || context == null) {
      return TerritoryManagementResult.STATE_UNKNOWN;
    }
    Lookup lookup = find(message.territoryId(), context);
    if (lookup.failed()) return TerritoryManagementResult.STATE_UNKNOWN;
    Owned territory = lookup.territory();
    TerritoryManagementResult authority = authority(territory, senderId);
    if (authority != TerritoryManagementResult.SUCCESS) return authority;
    if (message.targetPlayerId().equals(senderId)) return TerritoryManagementResult.SELF_TARGET;
    String targetName = null;
    if (message.allowed()) {
      targetName = validName(context.players().name(message.targetPlayerId()));
      if (targetName == null) return TerritoryManagementResult.INVALID_TARGET;
    }
    try {
      RepositoryResult result = context.repository().setPermission(
          message.territoryId(), senderId, message.targetPlayerId(), targetName, message.allowed());
      return map(result);
    } catch (RuntimeException failure) {
      report(context, message.territoryId(), senderId, "permission", failure);
      return TerritoryManagementResult.STATE_UNKNOWN;
    }
  }

  public static TerritoryManagementResult transfer(
      TransferTerritoryOwnershipMessage message, UUID senderId, Context context) {
    if (message == null || senderId == null || context == null) {
      return TerritoryManagementResult.STATE_UNKNOWN;
    }
    Lookup lookup = find(message.territoryId(), context);
    if (lookup.failed()) return TerritoryManagementResult.STATE_UNKNOWN;
    Owned territory = lookup.territory();
    TerritoryManagementResult authority = authority(territory, senderId);
    if (authority != TerritoryManagementResult.SUCCESS) return authority;
    if (message.targetPlayerId().equals(senderId)) return TerritoryManagementResult.SELF_TARGET;
    String targetName = validName(context.players().name(message.targetPlayerId()));
    if (targetName == null) return TerritoryManagementResult.INVALID_TARGET;
    try {
      RepositoryResult result = context.repository().transfer(
          message.territoryId(), senderId, message.targetPlayerId(), targetName);
      return map(result);
    } catch (RuntimeException failure) {
      report(context, message.territoryId(), senderId, "transfer", failure);
      return TerritoryManagementResult.STATE_UNKNOWN;
    }
  }

  public static TerritoryManagementResult rule(
      UpdateTerritoryRuleMessage message, UUID senderId, Context context) {
    if (message == null || senderId == null || context == null) {
      return TerritoryManagementResult.STATE_UNKNOWN;
    }
    Lookup lookup = find(message.territoryId(), context);
    if (lookup.failed()) return TerritoryManagementResult.STATE_UNKNOWN;
    Owned territory = lookup.territory();
    TerritoryManagementResult authority = authority(territory, senderId);
    if (authority != TerritoryManagementResult.SUCCESS) return authority;
    try {
      return map(context.repository().setRule(
          message.territoryId(), senderId, message.action(), message.level()));
    } catch (RuntimeException failure) {
      report(context, message.territoryId(), senderId, "rule", failure);
      return TerritoryManagementResult.STATE_UNKNOWN;
    }
  }

  private static Lookup find(UUID territoryId, Context context) {
    try {
      return new Lookup(context.repository().find(territoryId), false);
    } catch (RuntimeException failure) {
      report(context, territoryId, null, "lookup", failure);
      return new Lookup(null, true);
    }
  }

  private record Lookup(Owned territory, boolean failed) {}

  private static TerritoryManagementResult authority(Owned territory, UUID senderId) {
    if (territory == null) return TerritoryManagementResult.NOT_FOUND;
    return territory.summary().ownerId().equals(senderId)
        ? TerritoryManagementResult.SUCCESS
        : TerritoryManagementResult.NOT_OWNER;
  }

  private static String validName(Optional<String> value) {
    if (value == null || value.isEmpty()) return null;
    String name = value.get().trim();
    return name.isEmpty() || name.length() > com.mo.economy_system.common.network.EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH
        ? null : name;
  }

  private static TerritoryManagementResult map(RepositoryResult result) {
    if (result == null) return TerritoryManagementResult.STATE_UNKNOWN;
    return switch (result) {
      case SUCCESS -> TerritoryManagementResult.SUCCESS;
      case NOT_FOUND -> TerritoryManagementResult.NOT_FOUND;
      case OWNER_CHANGED -> TerritoryManagementResult.NOT_OWNER;
      case INVALID_TARGET -> TerritoryManagementResult.INVALID_TARGET;
      case NO_CHANGE -> TerritoryManagementResult.NO_CHANGE;
      case PERSIST_FAILED -> TerritoryManagementResult.PERSIST_FAILED;
      case STATE_UNKNOWN -> TerritoryManagementResult.STATE_UNKNOWN;
    };
  }

  private static void report(
      Context context, UUID territoryId, UUID senderId, String stage, RuntimeException failure) {
    try {
      context.failures().record(territoryId, senderId, stage, failure);
    } catch (RuntimeException ignored) {
    }
  }

  public record Context(Repository repository, PlayerDirectory players, FailureSink failures) {
    public Context {
      Objects.requireNonNull(repository, "repository");
      Objects.requireNonNull(players, "players");
      Objects.requireNonNull(failures, "failures");
    }
  }

  public interface Repository {
    Owned find(UUID territoryId);
    RepositoryResult setPermission(
        UUID territoryId, UUID expectedOwner, UUID targetId, String targetName, boolean allowed);
    RepositoryResult transfer(
        UUID territoryId, UUID expectedOwner, UUID targetId, String targetName);
    RepositoryResult setRule(
        UUID territoryId, UUID expectedOwner, RuleAction action, RuleLevel level);
  }

  public enum RepositoryResult {
    SUCCESS,
    NOT_FOUND,
    OWNER_CHANGED,
    INVALID_TARGET,
    NO_CHANGE,
    PERSIST_FAILED,
    STATE_UNKNOWN
  }

  @FunctionalInterface
  public interface PlayerDirectory {
    Optional<String> name(UUID playerId);
  }

  @FunctionalInterface
  public interface FailureSink {
    void record(UUID territoryId, UUID senderId, String stage, RuntimeException failure);

    static FailureSink noop() {
      return (territoryId, senderId, stage, failure) -> {};
    }
  }
}

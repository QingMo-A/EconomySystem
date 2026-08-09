package com.mo.economy_system.common.territory;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.network.TransferTerritoryOwnershipMessage;
import com.mo.economy_system.common.network.UpdateTerritoryPermissionMessage;
import com.mo.economy_system.common.network.UpdateTerritoryRuleMessage;
import com.mo.economy_system.common.territory.TerritorySnapshots.Member;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Rule;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import com.mo.economy_system.common.territory.TerritorySnapshots.Summary;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Server-authoritative protocols 41-43 and their common state-transition policy. */
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
    Prepared prepared = preparePermission(territory, message.targetPlayerId(), targetName,
        message.allowed());
    return apply(prepared, context, message.territoryId(), senderId, "permission");
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
    Prepared prepared = prepareTransfer(territory, message.targetPlayerId(), targetName);
    return apply(prepared, context, message.territoryId(), senderId, "transfer");
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
    Prepared prepared = prepareRule(territory, message.action(), message.level());
    return apply(prepared, context, message.territoryId(), senderId, "rule");
  }

  private static TerritoryManagementResult apply(
      Prepared prepared,
      Context context,
      UUID territoryId,
      UUID senderId,
      String stage) {
    if (prepared.result() != RepositoryResult.SUCCESS) return map(prepared.result());
    try {
      return map(context.repository().apply(prepared.expected(), prepared.replacement()));
    } catch (RuntimeException failure) {
      report(context, territoryId, senderId, stage, failure);
      return TerritoryManagementResult.STATE_UNKNOWN;
    }
  }

  private static Prepared preparePermission(
      Owned current, UUID targetId, String targetName, boolean allowed) {
    List<Member> members = new ArrayList<>(current.authorizedMembers());
    int index = indexOf(members, targetId);
    if (allowed == (index >= 0)) return Prepared.noChange();
    if (allowed) {
      if (members.size() >= EconomyNetworkLimits.MAX_TERRITORY_MEMBERS) {
        return Prepared.invalidTarget();
      }
      members.add(new Member(targetId, targetName));
    } else {
      members.remove(index);
    }
    return Prepared.success(current, copy(current, members, current.rules(), current.buffs()));
  }

  private static Prepared prepareTransfer(Owned current, UUID targetId, String targetName) {
    List<Member> members = new ArrayList<>(current.authorizedMembers());
    members.removeIf(member -> member.playerId().equals(targetId));
    UUID oldOwner = current.summary().ownerId();
    if (indexOf(members, oldOwner) < 0) {
      if (members.size() >= EconomyNetworkLimits.MAX_TERRITORY_MEMBERS) {
        return Prepared.invalidTarget();
      }
      members.add(new Member(oldOwner, current.summary().ownerName()));
    }
    Summary old = current.summary();
    Summary summary = new Summary(
        old.territoryId(),
        targetId,
        targetName,
        old.name(),
        old.pos1(),
        old.pos2(),
        old.dimensionId());
    return Prepared.success(current,
        new Owned(summary, members, current.backpoint(), current.rules(), current.buffs()));
  }

  private static Prepared prepareRule(Owned current, RuleAction action, RuleLevel level) {
    if (action == null || level == null) return Prepared.invalidTarget();
    List<Rule> rules = new ArrayList<>(current.rules());
    int index = -1;
    for (int i = 0; i < rules.size(); i++) {
      if (rules.get(i).action() == action) {
        index = i;
        break;
      }
    }
    if (index < 0) return Prepared.invalidTarget();
    if (rules.get(index).level() == level) return Prepared.noChange();
    rules.set(index, new Rule(action, level));
    return Prepared.success(current, copy(current, current.authorizedMembers(), rules, current.buffs()));
  }

  private static int indexOf(List<Member> members, UUID playerId) {
    for (int i = 0; i < members.size(); i++) {
      if (members.get(i).playerId().equals(playerId)) return i;
    }
    return -1;
  }

  private static Owned copy(Owned current, List<Member> members, List<Rule> rules,
      List<TerritorySnapshots.Buff> buffs) {
    return new Owned(current.summary(), members, current.backpoint(), rules, buffs);
  }

  /**
   * Validates the repository contract for an administration replacement. Administration may
   * change only ownership, authorized members, and rules; geometry, identity, return point, and
   * buff state are owned by their respective common transactions.
   */
  public static boolean isValidReplacement(Owned expected, Owned replacement) {
    if (expected == null || replacement == null) return false;
    Summary before = expected.summary();
    Summary after = replacement.summary();
    return before.territoryId().equals(after.territoryId())
        && before.name().equals(after.name())
        && before.pos1().equals(after.pos1())
        && before.pos2().equals(after.pos2())
        && before.dimensionId().equals(after.dimensionId())
        && before.ownerId() != null
        && before.ownerName() != null
        && replacement.backpoint().equals(expected.backpoint())
        && replacement.buffs().equals(expected.buffs());
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

  private record Prepared(RepositoryResult result, Owned expected, Owned replacement) {
    private Prepared {
      Objects.requireNonNull(result, "result");
      if (result == RepositoryResult.SUCCESS) {
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(replacement, "replacement");
      }
    }

    static Prepared success(Owned expected, Owned replacement) {
      return new Prepared(RepositoryResult.SUCCESS, expected, replacement);
    }

    static Prepared noChange() {
      return new Prepared(RepositoryResult.NO_CHANGE, null, null);
    }

    static Prepared invalidTarget() {
      return new Prepared(RepositoryResult.INVALID_TARGET, null, null);
    }
  }

  private static TerritoryManagementResult authority(Owned territory, UUID senderId) {
    if (territory == null) return TerritoryManagementResult.NOT_FOUND;
    return territory.summary().ownerId().equals(senderId)
        ? TerritoryManagementResult.SUCCESS
        : TerritoryManagementResult.NOT_OWNER;
  }

  private static String validName(Optional<String> value) {
    if (value == null || value.isEmpty()) return null;
    String name = value.get().trim();
    return name.isEmpty() || name.length() > EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH
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
    RepositoryResult apply(Owned expected, Owned replacement);
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

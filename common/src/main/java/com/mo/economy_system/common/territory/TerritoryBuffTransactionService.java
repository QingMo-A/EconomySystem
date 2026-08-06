package com.mo.economy_system.common.territory;

import com.mo.economy_system.common.territory.TerritorySnapshots.Buff;
import com.mo.economy_system.common.territory.TerritorySnapshots.BuffUpgradeCost;
import com.mo.economy_system.common.territory.TerritorySnapshots.ItemRequirement;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Transactional unlock/upgrade service used by protocols 37 and 38. */
public final class TerritoryBuffTransactionService {
  private TerritoryBuffTransactionService() {}

  public static TerritoryManagementResult execute(
      UUID territoryId, String buffId, Action action, Context context) {
    if (territoryId == null || buffId == null || buffId.isBlank() || action == null || context == null) {
      return TerritoryManagementResult.INVALID_BUFF;
    }
    Owned territory;
    try {
      territory = context.repository().find(territoryId);
    } catch (RuntimeException failure) {
      report(context, territoryId, buffId, "lookup", failure);
      return TerritoryManagementResult.STATE_UNKNOWN;
    }
    if (territory == null) return TerritoryManagementResult.NOT_FOUND;
    if (!territory.summary().ownerId().equals(context.playerId())) {
      return TerritoryManagementResult.NOT_OWNER;
    }
    if (!territory.summary().dimensionId().equals(context.dimensionId())) {
      return TerritoryManagementResult.WRONG_DIMENSION;
    }
    Buff buff = territory.buffs().stream().filter(value -> value.id().equals(buffId)).findFirst().orElse(null);
    if (buff == null) return TerritoryManagementResult.INVALID_BUFF;
    if (action == Action.UNLOCK && buff.unlocked()) return TerritoryManagementResult.ALREADY_UNLOCKED;
    if (action == Action.UPGRADE && !buff.unlocked()) return TerritoryManagementResult.NOT_UNLOCKED;
    if (action == Action.UPGRADE && buff.level() >= buff.maxLevel()) {
      return TerritoryManagementResult.MAX_LEVEL;
    }

    Cost cost;
    try {
      cost = cost(buff);
    } catch (RuntimeException failure) {
      report(context, territoryId, buffId, "cost", failure);
      return TerritoryManagementResult.INVALID_COST;
    }
    if (cost.currency() > 0) {
      BalanceMutationResult preview;
      try {
        preview = context.accounts().preview(cost.currency());
      } catch (RuntimeException failure) {
        report(context, territoryId, buffId, "balance-preview", failure);
        return TerritoryManagementResult.BALANCE_FAILED;
      }
      if (preview == BalanceMutationResult.INSUFFICIENT_FUNDS) {
        return TerritoryManagementResult.INSUFFICIENT_BALANCE;
      }
      if (preview != BalanceMutationResult.SUCCESS) return TerritoryManagementResult.BALANCE_FAILED;
    }
    int experienceLevel;
    boolean hasItems;
    try {
      experienceLevel = context.resources().experienceLevel();
      hasItems = context.resources().canRemove(cost.items());
    } catch (RuntimeException failure) {
      report(context, territoryId, buffId, "resource-preview", failure);
      return TerritoryManagementResult.INVENTORY_FAILED;
    }
    if (experienceLevel < cost.experience()) {
      return TerritoryManagementResult.INSUFFICIENT_EXPERIENCE;
    }
    if (!hasItems) {
      return TerritoryManagementResult.INSUFFICIENT_ITEMS;
    }

    boolean balanceDebited = false;
    boolean experienceDebited = false;
    ItemRemoval itemRemoval = null;
    if (cost.currency() > 0) {
      BalanceMutationResult debit = context.accounts().debit(cost.currency());
      if (debit == BalanceMutationResult.INSUFFICIENT_FUNDS) {
        return TerritoryManagementResult.INSUFFICIENT_BALANCE;
      }
      if (debit != BalanceMutationResult.SUCCESS) return TerritoryManagementResult.BALANCE_FAILED;
      balanceDebited = true;
    }
    if (cost.experience() > 0) {
      try {
        experienceDebited = context.resources().debitExperience(cost.experience());
      } catch (RuntimeException failure) {
        report(context, territoryId, buffId, "experience-debit", failure);
      }
      if (!experienceDebited) {
        return compensate(context, territoryId, buffId, cost, balanceDebited, false, null,
            TerritoryManagementResult.INSUFFICIENT_EXPERIENCE);
      }
    }
    if (!cost.items().isEmpty()) {
      try {
        itemRemoval = context.resources().remove(cost.items());
      } catch (RuntimeException failure) {
        report(context, territoryId, buffId, "item-remove", failure);
      }
      if (itemRemoval == null || !itemRemoval.succeeded() || itemRemoval.rollback() == null) {
        boolean alreadyRestored = itemRemoval != null && itemRemoval.failureRestored();
        return compensate(context, territoryId, buffId, cost, balanceDebited, experienceDebited,
            alreadyRestored ? null : itemRemoval,
            alreadyRestored ? TerritoryManagementResult.INVENTORY_FAILED
                : TerritoryManagementResult.ROLLBACK_FAILED);
      }
    }

    RepositoryResult mutation;
    try {
      mutation = context.repository().mutate(
          territoryId,
          context.playerId(),
          buffId,
          buff.unlocked(),
          buff.level(),
          action);
    } catch (RuntimeException failure) {
      report(context, territoryId, buffId, "territory-mutate", failure);
      mutation = RepositoryResult.STATE_UNKNOWN;
    }
    if (mutation == RepositoryResult.SUCCESS) return TerritoryManagementResult.SUCCESS;
    if (mutation == null || mutation == RepositoryResult.STATE_UNKNOWN) {
      return TerritoryManagementResult.STATE_UNKNOWN;
    }
    TerritoryManagementResult failure = switch (mutation) {
      case NOT_FOUND -> TerritoryManagementResult.NOT_FOUND;
      case OWNER_CHANGED -> TerritoryManagementResult.NOT_OWNER;
      case BUFF_CHANGED -> TerritoryManagementResult.NO_CHANGE;
      case PERSIST_FAILED -> TerritoryManagementResult.PERSIST_FAILED;
      case SUCCESS, STATE_UNKNOWN -> TerritoryManagementResult.STATE_UNKNOWN;
    };
    return compensate(
        context, territoryId, buffId, cost, balanceDebited, experienceDebited, itemRemoval, failure);
  }

  private static Cost cost(Buff buff) {
    long experience = 0;
    long currency = 0;
    Map<String, Integer> items = new LinkedHashMap<>();
    for (BuffUpgradeCost level : buff.upgradeCosts()) {
      experience = Math.addExact(experience, level.experience());
      currency = Math.addExact(currency, level.currency());
      for (ItemRequirement item : level.items()) {
        items.merge(item.itemId(), item.count(), Math::addExact);
      }
    }
    if (experience > Integer.MAX_VALUE || currency > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("territory buff cost overflow");
    }
    return new Cost(Map.copyOf(items), (int) experience, (int) currency);
  }

  private static TerritoryManagementResult compensate(
      Context context,
      UUID territoryId,
      String buffId,
      Cost cost,
      boolean balanceDebited,
      boolean experienceDebited,
      ItemRemoval itemRemoval,
      TerritoryManagementResult original) {
    boolean restored = true;
    if (itemRemoval != null && itemRemoval.rollback() != null) {
      try {
        restored &= itemRemoval.rollback().rollback();
      } catch (RuntimeException failure) {
        restored = false;
        report(context, territoryId, buffId, "item-rollback", failure);
      }
    }
    if (experienceDebited) {
      try {
        restored &= context.resources().refundExperience(cost.experience());
      } catch (RuntimeException failure) {
        restored = false;
        report(context, territoryId, buffId, "experience-rollback", failure);
      }
    }
    if (balanceDebited) {
      try {
        restored &= context.accounts().refund(cost.currency()) == BalanceMutationResult.SUCCESS;
      } catch (RuntimeException failure) {
        restored = false;
        report(context, territoryId, buffId, "balance-rollback", failure);
      }
    }
    return restored ? original : TerritoryManagementResult.ROLLBACK_FAILED;
  }

  private static void report(
      Context context, UUID territoryId, String buffId, String stage, RuntimeException failure) {
    try {
      context.failures().record(context.playerId(), territoryId, buffId, stage, failure);
    } catch (RuntimeException ignored) {
    }
  }

  public enum Action {
    UNLOCK,
    UPGRADE
  }

  public record Context(
      UUID playerId,
      String dimensionId,
      Repository repository,
      Accounts accounts,
      Resources resources,
      FailureSink failures) {
    public Context {
      Objects.requireNonNull(playerId, "playerId");
      Objects.requireNonNull(dimensionId, "dimensionId");
      Objects.requireNonNull(repository, "repository");
      Objects.requireNonNull(accounts, "accounts");
      Objects.requireNonNull(resources, "resources");
      Objects.requireNonNull(failures, "failures");
    }
  }

  public interface Repository {
    Owned find(UUID territoryId);
    RepositoryResult mutate(
        UUID territoryId,
        UUID expectedOwner,
        String buffId,
        boolean expectedUnlocked,
        int expectedLevel,
        Action action);
  }

  public enum RepositoryResult {
    SUCCESS,
    NOT_FOUND,
    OWNER_CHANGED,
    BUFF_CHANGED,
    PERSIST_FAILED,
    STATE_UNKNOWN
  }

  public interface Accounts {
    BalanceMutationResult preview(int amount);
    BalanceMutationResult debit(int amount);
    BalanceMutationResult refund(int amount);
  }

  public interface Resources {
    int experienceLevel();
    boolean canRemove(Map<String, Integer> items);
    boolean debitExperience(int levels);
    boolean refundExperience(int levels);
    ItemRemoval remove(Map<String, Integer> items);
  }

  public record ItemRemoval(boolean succeeded, boolean failureRestored, Rollback rollback) {
    public ItemRemoval {
      if (succeeded && rollback == null) throw new IllegalArgumentException("successful removal needs rollback");
      if (!succeeded && rollback != null) throw new IllegalArgumentException("failed removal cannot expose rollback");
    }
    public static ItemRemoval success(Rollback rollback) {
      return new ItemRemoval(true, true, Objects.requireNonNull(rollback, "rollback"));
    }
    public static ItemRemoval failure(boolean restored) {
      return new ItemRemoval(false, restored, null);
    }
  }

  @FunctionalInterface
  public interface Rollback {
    boolean rollback();
  }

  @FunctionalInterface
  public interface FailureSink {
    void record(UUID playerId, UUID territoryId, String buffId, String stage, RuntimeException failure);

    static FailureSink noop() {
      return (playerId, territoryId, buffId, stage, failure) -> {};
    }
  }

  private record Cost(Map<String, Integer> items, int experience, int currency) {}
}

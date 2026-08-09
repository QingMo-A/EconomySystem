package com.mo.economy_system.common.reward;

import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import java.util.Objects;
import java.util.UUID;

/** Server-authoritative reward lookup, calculation, and exact account mutation. */
public final class RewardService {
  private static final String CATEGORY = "系统";
  private static final String REASON_PREFIX = "击杀奖励: ";

  public enum Result {
    SUCCESS,
    UNCONFIGURED,
    NO_DROP,
    BALANCE_LIMIT,
    PERSIST_FAILED,
    STATE_UNKNOWN
  }

  public record Outcome(Result result, int amount) {
    public Outcome {
      Objects.requireNonNull(result, "result");
      if (amount < 0) throw new IllegalArgumentException("amount");
      if ((result == Result.SUCCESS) != (amount > 0)) {
        throw new IllegalArgumentException("result/amount");
      }
    }
  }

  @FunctionalInterface
  public interface Diagnostics {
    void warning(String operation, UUID playerId, String entityType, Throwable error);
  }

  private final RewardCatalog catalog;
  private final RewardCalculator calculator;
  private final RewardAccountPort accounts;
  private final Diagnostics diagnostics;

  public RewardService(
      RewardCatalog catalog,
      RewardCalculator calculator,
      RewardAccountPort accounts,
      Diagnostics diagnostics) {
    this.catalog = Objects.requireNonNull(catalog, "catalog");
    this.calculator = Objects.requireNonNull(calculator, "calculator");
    this.accounts = Objects.requireNonNull(accounts, "accounts");
    this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
  }

  public RewardService(
      RewardCatalog catalog, RewardCalculator calculator, RewardAccountPort accounts) {
    this(catalog, calculator, accounts, (operation, playerId, entityType, error) -> {});
  }

  public Outcome award(
      UUID playerId,
      String entityType,
      String entityDisplayName,
      int bountyHunterLevel,
      int carefullyLevel) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(entityType, "entityType");
    RewardEntry entry = catalog.find(entityType).orElse(null);
    if (entry == null) return outcome(Result.UNCONFIGURED);

    RewardCalculator.Calculation calculation;
    try {
      calculation = calculator.calculate(entry, bountyHunterLevel, carefullyLevel);
    } catch (RuntimeException error) {
      warn("calculate", playerId, entityType, error);
      return outcome(Result.STATE_UNKNOWN);
    }
    if (calculation.result() == RewardCalculator.Result.NO_DROP) {
      return outcome(Result.NO_DROP);
    }
    if (calculation.result() != RewardCalculator.Result.REWARD) {
      warn(
          "calculate-" + calculation.result().name().toLowerCase(java.util.Locale.ROOT),
          playerId,
          entityType,
          new IllegalStateException("invalid reward calculation"));
      return outcome(Result.STATE_UNKNOWN);
    }

    String displayName = entityDisplayName == null || entityDisplayName.isBlank()
        ? entityType
        : entityDisplayName;
    BalanceMutationResult mutation;
    try {
      mutation = Objects.requireNonNull(
          accounts.credit(
              playerId, calculation.amount(), CATEGORY, REASON_PREFIX + displayName),
          "credit result");
    } catch (RuntimeException error) {
      warn("credit", playerId, entityType, error);
      return outcome(Result.STATE_UNKNOWN);
    }
    return switch (mutation) {
      case SUCCESS -> new Outcome(Result.SUCCESS, calculation.amount());
      case BALANCE_LIMIT -> outcome(Result.BALANCE_LIMIT);
      case PERSIST_FAILED -> outcome(Result.PERSIST_FAILED);
      case INVALID_AMOUNT, INSUFFICIENT_FUNDS -> {
        warn(
            "credit-result",
            playerId,
            entityType,
            new IllegalStateException("unexpected credit result: " + mutation));
        yield outcome(Result.STATE_UNKNOWN);
      }
    };
  }

  private static Outcome outcome(Result result) {
    return new Outcome(result, 0);
  }

  private void warn(String operation, UUID playerId, String entityType, Throwable error) {
    try {
      diagnostics.warning(operation, playerId, entityType, error);
    } catch (RuntimeException ignored) {
      // Diagnostics must never alter the authoritative transaction result.
    }
  }
}

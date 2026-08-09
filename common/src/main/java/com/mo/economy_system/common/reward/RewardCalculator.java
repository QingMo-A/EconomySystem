package com.mo.economy_system.common.reward;

import java.util.Objects;

/** Computes one reward without accessing Minecraft, persistence, or global randomness. */
public final class RewardCalculator {
  public enum Result {
    REWARD,
    NO_DROP,
    INVALID_RANDOM,
    INVALID_REWARD
  }

  public record Calculation(Result result, int amount) {
    public Calculation {
      Objects.requireNonNull(result, "result");
      if (amount < 0) throw new IllegalArgumentException("amount");
      if ((result == Result.REWARD) != (amount > 0)) {
        throw new IllegalArgumentException("result/amount");
      }
    }
  }

  private final RewardRandom random;

  public RewardCalculator(RewardRandom random) {
    this.random = Objects.requireNonNull(random, "random");
  }

  public Calculation calculate(
      RewardEntry entry, int bountyHunterLevel, int carefullyLevel) {
    Objects.requireNonNull(entry, "entry");
    double roll;
    try {
      roll = random.nextDouble();
    } catch (RuntimeException error) {
      return new Calculation(Result.INVALID_RANDOM, 0);
    }
    if (!Double.isFinite(roll) || roll < 0.0D || roll >= 1.0D) {
      return new Calculation(Result.INVALID_RANDOM, 0);
    }
    double chance = RewardBonusPolicy.applyBountyBonus(entry.dropChance(), bountyHunterLevel);
    if (roll >= chance) return new Calculation(Result.NO_DROP, 0);

    long width = (long) entry.dropMax() - entry.dropMin() + 1L;
    if (width <= 0L || width > Integer.MAX_VALUE) {
      return new Calculation(Result.INVALID_REWARD, 0);
    }
    int offset;
    try {
      offset = random.nextInt((int) width);
    } catch (RuntimeException error) {
      return new Calculation(Result.INVALID_RANDOM, 0);
    }
    if (offset < 0 || offset >= width) {
      return new Calculation(Result.INVALID_RANDOM, 0);
    }
    int base = entry.dropMin() + offset;
    try {
      int boosted = RewardBonusPolicy.applyCarefullyBonus(base, carefullyLevel);
      return boosted == 0
          ? new Calculation(Result.NO_DROP, 0)
          : new Calculation(Result.REWARD, boosted);
    } catch (ArithmeticException | IllegalArgumentException error) {
      return new Calculation(Result.INVALID_REWARD, 0);
    }
  }
}

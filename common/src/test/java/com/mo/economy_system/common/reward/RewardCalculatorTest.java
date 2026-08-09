package com.mo.economy_system.common.reward;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RewardCalculatorTest {
  @Test
  void preserves1211BountyAndCarefullySemantics() {
    RewardCalculator calculator = new RewardCalculator(new FixedRandom(0.34D, 2));

    RewardCalculator.Calculation outcome =
        calculator.calculate(new RewardEntry("minecraft:zombie", 0.1D, 1, 5), 1, 2);

    assertEquals(RewardCalculator.Result.REWARD, outcome.result());
    assertEquals(5, outcome.amount()); // base 3, rounded after a 60% bonus
  }

  @Test
  void dropRollEqualToChanceDoesNotPay() {
    RewardCalculator.Calculation outcome =
        new RewardCalculator(new FixedRandom(0.5D, 0))
            .calculate(new RewardEntry("minecraft:wither", 0.5D, 1, 1), 0, 0);

    assertEquals(new RewardCalculator.Calculation(RewardCalculator.Result.NO_DROP, 0), outcome);
  }

  @Test
  void bountyChanceCapsAtOne() {
    RewardCalculator.Calculation outcome =
        new RewardCalculator(new FixedRandom(0.999999D, 0))
            .calculate(new RewardEntry("minecraft:zombie", 0.1D, 1, 1), 10, 0);

    assertEquals(new RewardCalculator.Calculation(RewardCalculator.Result.REWARD, 1), outcome);
  }

  @Test
  void invalidRandomSourceFailsClosed() {
    RewardCalculator.Calculation probability =
        new RewardCalculator(new FixedRandom(1.0D, 0))
            .calculate(new RewardEntry("minecraft:zombie", 1.0D, 1, 1), 0, 0);
    RewardCalculator.Calculation range =
        new RewardCalculator(new FixedRandom(0.0D, 2))
            .calculate(new RewardEntry("minecraft:zombie", 1.0D, 1, 2), 0, 0);

    assertEquals(RewardCalculator.Result.INVALID_RANDOM, probability.result());
    assertEquals(RewardCalculator.Result.INVALID_RANDOM, range.result());
  }

  @Test
  void throwingRandomSourceFailsClosed() {
    RewardRandom random =
        new RewardRandom() {
          @Override
          public double nextDouble() {
            throw new IllegalStateException("random unavailable");
          }

          @Override
          public int nextInt(int bound) {
            throw new IllegalStateException("random unavailable");
          }
        };

    assertEquals(
        RewardCalculator.Result.INVALID_RANDOM,
        new RewardCalculator(random)
            .calculate(new RewardEntry("minecraft:zombie", 1.0D, 1, 1), 0, 0)
            .result());
  }

  @Test
  void rewardEntryRejectsCrashingRangesAndInvalidIdentifiers() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new RewardEntry("minecraft:zombie", 0.5D, 5, 1));
    assertThrows(
        IllegalArgumentException.class,
        () -> new RewardEntry("Not An Id", 0.5D, 1, 5));
    assertThrows(
        IllegalArgumentException.class,
        () -> new RewardEntry("minecraft:zombie", Double.NaN, 1, 5));
    assertThrows(
        IllegalArgumentException.class,
        () -> new RewardEntry("minecraft:zombie", 1.0D, 0, Integer.MAX_VALUE));
  }

  private record FixedRandom(double value, int integer) implements RewardRandom {
    @Override
    public double nextDouble() {
      return value;
    }

    @Override
    public int nextInt(int bound) {
      return integer;
    }
  }
}

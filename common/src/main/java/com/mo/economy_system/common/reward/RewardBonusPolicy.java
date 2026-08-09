package com.mo.economy_system.common.reward;

/** Shared semantics for the Bounty Hunter and Carefully enchantments. */
public final class RewardBonusPolicy {
  public static final double BOUNTY_CHANCE_PER_LEVEL = 0.25D;
  public static final double CAREFULLY_MULTIPLIER_PER_LEVEL = 0.3D;

  private RewardBonusPolicy() {}

  public static double applyBountyBonus(double chance, int enchantmentLevel) {
    if (enchantmentLevel <= 0) return chance;
    return Math.min(1.0D, chance + BOUNTY_CHANCE_PER_LEVEL * enchantmentLevel);
  }

  public static int applyCarefullyBonus(int reward, int enchantmentLevel) {
    if (reward < 0) throw new IllegalArgumentException("reward");
    if (reward == 0 || enchantmentLevel <= 0) return reward;
    double boosted = reward * (CAREFULLY_MULTIPLIER_PER_LEVEL * enchantmentLevel + 1.0D);
    if (!Double.isFinite(boosted) || boosted > Integer.MAX_VALUE) {
      throw new ArithmeticException("reward bonus overflow");
    }
    return Math.toIntExact(Math.round(boosted));
  }
}

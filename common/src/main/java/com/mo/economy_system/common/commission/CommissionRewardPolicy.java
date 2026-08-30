package com.mo.economy_system.common.commission;

import java.util.Objects;

/** Calculates a frozen quote without touching accounts, items, mail, or a loader API. */
@FunctionalInterface
public interface CommissionRewardPolicy {
  CommissionRewardSnapshot calculate(
      CommissionTemplate template,
      CommissionRequester requester,
      String targetSnapshot,
      int quantity,
      CommissionRandom random);

  /** Default one-currency policy based on the template's per-unit/fixed amount and modifiers. */
  static CommissionRewardPolicy defaultPolicy() {
    return (template, requester, target, quantity, random) -> {
      Objects.requireNonNull(template, "template");
      Objects.requireNonNull(requester, "requester");
      Objects.requireNonNull(target, "target");
      Objects.requireNonNull(random, "random");
      double roll = random.nextDouble();
      if (!Double.isFinite(roll) || roll < 0.0D || roll >= 1.0D) {
        throw new IllegalArgumentException("random reward multiplier roll is invalid");
      }
      double multiplier = template.rewardMultiplierMin()
          + (template.rewardMultiplierMax() - template.rewardMultiplierMin()) * roll;
      double base = template.rewardMode() == CommissionRewardMode.PER_UNIT
          ? (double) template.rewardPerUnit() * quantity
          : template.rewardPerUnit();
      double amount = base * multiplier * requester.rewardMultiplier();
      if (!Double.isFinite(amount) || amount < 0.0D || amount > Integer.MAX_VALUE) {
        throw new IllegalArgumentException("commission reward overflows currency range");
      }
      return new CommissionRewardSnapshot(
          CommissionRewardSnapshot.DEFAULT_CURRENCY_ID,
          (int) Math.round(amount),
          "");
    };
  }
}

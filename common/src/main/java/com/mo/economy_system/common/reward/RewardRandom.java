package com.mo.economy_system.common.reward;

/** Injectable random source for deterministic reward tests. */
public interface RewardRandom {
  double nextDouble();

  int nextInt(int bound);
}

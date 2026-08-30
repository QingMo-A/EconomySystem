package com.mo.economy_system.common.commission;

/** Injectable random source so generation can be deterministic in contract tests. */
public interface CommissionRandom {
  double nextDouble();

  int nextInt(int bound);
}

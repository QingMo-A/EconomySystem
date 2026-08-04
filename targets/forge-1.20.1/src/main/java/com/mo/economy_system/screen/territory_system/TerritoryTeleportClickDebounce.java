package com.mo.economy_system.screen.territory_system;

final class TerritoryTeleportClickDebounce {
  private final int duration; private int remaining;
  TerritoryTeleportClickDebounce(int duration) { if (duration < 1) throw new IllegalArgumentException(); this.duration=duration; }
  boolean tryAcquire() { if (remaining > 0) return false; remaining=duration; return true; }
  void tick() { if (remaining > 0) remaining--; }
  boolean ready() { return remaining == 0; }
}

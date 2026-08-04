package com.mo.economy_system.common.territory;

public final class RecallPotionReserveException extends Exception {
  private final int slot; private final boolean rollbackFailed;
  RecallPotionReserveException(int slot, boolean rollbackFailed, Exception cause) {
    super("recall potion reserve failed at slot " + slot, cause); this.slot=slot; this.rollbackFailed=rollbackFailed;
  }
  public int slot(){return slot;} public boolean rollbackFailed(){return rollbackFailed;}
}

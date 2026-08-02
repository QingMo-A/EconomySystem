package com.mo.economy_system.common.market;

public record DemandDeliveryCompensation(
    boolean paymentReversalAttempted,
    boolean paymentReverted,
    boolean inventoryRollbackAttempted,
    boolean inventoryRestored,
    RuntimeException paymentError,
    RuntimeException inventoryError) {
  public boolean complete() {
    return paymentReverted && inventoryRestored;
  }
}

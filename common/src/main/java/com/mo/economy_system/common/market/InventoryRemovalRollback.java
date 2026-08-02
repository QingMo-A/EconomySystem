package com.mo.economy_system.common.market;

@FunctionalInterface
public interface InventoryRemovalRollback {
  boolean rollback();
}

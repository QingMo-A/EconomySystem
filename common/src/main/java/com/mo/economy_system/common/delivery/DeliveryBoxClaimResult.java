package com.mo.economy_system.common.delivery;

public enum DeliveryBoxClaimResult {
  SUCCESS,
  NOT_FOUND,
  INVALID_ENTRY,
  ITEM_RESTORE_FAILED,
  INVENTORY_FULL,
  INVENTORY_FAILED,
  PERSIST_FAILED,
  ROLLBACK_FAILED,
  STATE_UNKNOWN
}

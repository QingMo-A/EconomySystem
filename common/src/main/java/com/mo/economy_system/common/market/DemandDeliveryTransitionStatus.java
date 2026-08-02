package com.mo.economy_system.common.market;

public enum DemandDeliveryTransitionStatus {
  UPDATED,
  NOT_FOUND,
  WRONG_ORDER_TYPE,
  ALREADY_DELIVERED,
  ORDER_CHANGED,
  PERSIST_FAILED
}

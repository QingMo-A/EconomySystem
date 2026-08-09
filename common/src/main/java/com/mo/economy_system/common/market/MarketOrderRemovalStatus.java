package com.mo.economy_system.common.market;

/** Result of removing one order after comparing the complete expected snapshot. */
public enum MarketOrderRemovalStatus {
  REMOVED,
  NOT_FOUND,
  ORDER_CHANGED,
  PERSIST_FAILED
}

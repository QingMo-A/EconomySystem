package com.mo.economy_system.common.market;

/** Result of processing one order that has reached its expiration time. */
public enum MarketExpirationResult {
  REFUNDED,
  RETURNED_TO_DELIVERY,
  NOT_FOUND,
  ORDER_CHANGED,
  PERSIST_FAILED,
  CREDIT_FAILED,
  DELIVERY_FAILED,
  STATE_UNKNOWN
}

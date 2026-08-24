package com.mo.economy_system.common.market;

public enum MarketPartialFillStatus {
  UPDATED,
  REMOVED,
  NOT_FOUND,
  WRONG_ORDER_TYPE,
  ALREADY_DELIVERED,
  ORDER_CHANGED,
  INVALID_QUANTITY,
  NON_DIVISIBLE_PRICE,
  PRICE_OVERFLOW,
  PERSIST_FAILED
}

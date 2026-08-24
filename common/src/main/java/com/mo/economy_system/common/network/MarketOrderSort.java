package com.mo.economy_system.common.network;

/** Stable server-side market ordering applied before pagination. */
public enum MarketOrderSort {
  DEFAULT,
  UNIT_PRICE_ASC,
  UNIT_PRICE_DESC,
  NEWEST,
  EXPIRING_SOON
}

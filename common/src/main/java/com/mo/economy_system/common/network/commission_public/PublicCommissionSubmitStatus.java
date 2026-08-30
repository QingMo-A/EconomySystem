package com.mo.economy_system.common.network.commission_public;

/** Server result for a public commission contribution request. */
public enum PublicCommissionSubmitStatus {
  ACCEPTED,
  PARTIAL,
  COMPLETED,
  DUPLICATE,
  EXPIRED,
  NOT_FOUND,
  UNAVAILABLE,
  DELIVERY_RETRY,
  REJECTED
}

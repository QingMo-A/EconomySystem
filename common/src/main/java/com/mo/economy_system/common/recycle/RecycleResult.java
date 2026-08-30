package com.mo.economy_system.common.recycle;

/** Outcome of one idempotent recycling submission. */
public record RecycleResult(Status status, int acceptedAmount, int unitPrice, int payout, int highQuotaRemaining) {
  public enum Status {
    SUCCESS,
    UNKNOWN_ITEM,
    INVALID_AMOUNT,
    INSUFFICIENT_ITEMS,
    HIGH_QUOTA_EXHAUSTED,
    BALANCE_LIMIT,
    PERSIST_FAILED,
    DUPLICATE_SUBMISSION
  }

  public boolean success() {
    return status == Status.SUCCESS;
  }
}

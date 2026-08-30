package com.mo.economy_system.common.commission;

/** Reward settlement state. Completion and reward claiming are deliberately separate. */
public enum CommissionRewardStatus {
  PENDING_MAIL,
  MAIL_CREATED,
  CLAIMED,
  FAILED;

  public boolean terminal() {
    return this == CLAIMED;
  }
}

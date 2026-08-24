package com.mo.economy_system.common.market;

/** Shared result for authoritative market mailbox delivery bridges. */
public enum DemandMailboxResult {
  SUCCESS,
  FULL,
  FAILED,
  /** DeliveryBox/mail cross-ledger state is uncertain; callers must not blindly compensate. */
  UNKNOWN
}

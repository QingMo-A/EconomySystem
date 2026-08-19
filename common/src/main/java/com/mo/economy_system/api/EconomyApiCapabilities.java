package com.mo.economy_system.api;

/** Feature flags exposed by one bound EconomySystem API session. */
public record EconomyApiCapabilities(
    boolean accounts,
    boolean mailbox,
    boolean marketRead,
    boolean territoryRead) {

  public static final EconomyApiCapabilities V1 =
      new EconomyApiCapabilities(true, true, true, true);
}

package com.mo.economy_system.ui.check;

/** Target adapter for local scanning work; the controller owns generations and stale-result policy. */
public interface CheckResultPort {
  void startLocalScan(long generation);

  void cancelLocalScan();
}

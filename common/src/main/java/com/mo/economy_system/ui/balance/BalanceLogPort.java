package com.mo.economy_system.ui.balance;

public interface BalanceLogPort {
  long nextRequestId();

  void requestPage(long requestId, String category, int offset, int limit);
}

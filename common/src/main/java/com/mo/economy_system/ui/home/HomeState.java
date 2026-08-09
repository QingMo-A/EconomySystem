package com.mo.economy_system.ui.home;

import com.mo.economy_system.common.client.ui.EconomyUiMenu;
import com.mo.economy_system.common.network.AccountBalance;
import com.mo.economy_system.ui.core.ScreenState;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record HomeState(
    String playerName,
    List<EconomyUiMenu.Entry> entries,
    int balance,
    List<AccountBalance> accounts,
    int sellOrders,
    int demandOrders,
    int leaderboardOffset,
    int leaderboardPageSize,
    ScreenState screenState,
    String errorKey,
    long requestId,
    long balanceRevision,
    long marketRevision) {
  public HomeState {
    if (playerName == null || playerName.isBlank()) playerName = "Player";
    entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
    accounts = List.copyOf(Objects.requireNonNull(accounts, "accounts"));
    if (leaderboardOffset < 0 || leaderboardPageSize < 1 || sellOrders < 0 || demandOrders < 0
        || requestId < -1 || balanceRevision < -1 || marketRevision < -1) {
      throw new IllegalArgumentException("invalid home state");
    }
    screenState = Objects.requireNonNull(screenState, "screenState");
  }

  public int totalPages() {
    return Math.max(1, (accounts.size() + leaderboardPageSize - 1) / leaderboardPageSize);
  }

  public List<AccountBalance> visibleAccounts() {
    int start = Math.min(leaderboardOffset, accounts.size());
    return accounts.subList(start, Math.min(accounts.size(), start + leaderboardPageSize));
  }

  public int maxOffset() {
    return Math.max(0, accounts.size() - leaderboardPageSize);
  }

  public boolean isSelf(AccountBalance account) {
    return account.playerName().toLowerCase(Locale.ROOT)
        .equals(playerName.toLowerCase(Locale.ROOT));
  }
}

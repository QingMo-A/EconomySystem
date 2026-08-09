package com.mo.economy_system.ui.balance;

import com.mo.economy_system.common.network.BalanceLogRequestMessage;
import com.mo.economy_system.ui.core.ScreenState;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record BalanceLogState(
    List<BalanceLogRow> rows,
    String category,
    int offset,
    int limit,
    int total,
    int scroll,
    int visibleRows,
    ScreenState screenState,
    String errorKey,
    long requestId,
    Set<BalanceLogAction> actions) {
  public static final List<String> CATEGORIES = List.of(
      BalanceLogRequestMessage.ALL_CATEGORIES, "指令", "红包", "领地", "市场", "转账", "税费", "系统");

  public BalanceLogState {
    rows = List.copyOf(Objects.requireNonNull(rows, "rows"));
    if (offset < 0 || limit < 1 || total < 0 || scroll < 0 || visibleRows < 1 || requestId < -1) {
      throw new IllegalArgumentException("invalid balance-log state");
    }
    category = Objects.requireNonNull(category, "category");
    screenState = Objects.requireNonNull(screenState, "screenState");
    actions = Set.copyOf(Objects.requireNonNull(actions, "actions"));
  }

  public List<BalanceLogRow> visibleEntries() {
    int start = Math.min(scroll, rows.size());
    return rows.subList(start, Math.min(rows.size(), start + visibleRows));
  }

  public int totalPages() {
    return total == 0 ? 1 : Math.max(1, (total + limit - 1) / limit);
  }

  public int page() {
    return limit == 0 ? 0 : offset / limit;
  }

  public boolean hasPreviousPage() { return offset > 0; }
  public boolean hasNextPage() { return offset + limit < total; }
  public boolean can(BalanceLogAction action) { return actions.contains(action); }
}

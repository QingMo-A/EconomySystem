package com.mo.economy_system.ui.territory.detail;

import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.common.territory.TerritorySnapshots.Member;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Rule;
import com.mo.economy_system.ui.core.ScreenState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record TerritoryDetailState(
    Owned territory,
    List<PlayerSummary> players,
    TerritoryDetailViewKind view,
    int scroll,
    int pageSize,
    String filter,
    ScreenState screenState,
    String errorKey,
    long requestId,
    long playerRevision) {
  public TerritoryDetailState {
    Objects.requireNonNull(territory, "territory");
    players = List.copyOf(Objects.requireNonNull(players, "players"));
    Objects.requireNonNull(view, "view");
    if (scroll < 0 || pageSize < 1 || requestId < -1 || playerRevision < 0) {
      throw new IllegalArgumentException("territory detail pagination");
    }
    filter = Objects.requireNonNullElse(filter, "");
    Objects.requireNonNull(screenState, "screenState");
  }

  public List<TerritoryAccessRow> accessRows() {
    Set<UUID> owners = Set.of(territory.summary().ownerId());
    LinkedHashMap<UUID, TerritoryAccessRow> rows = new LinkedHashMap<>();
    for (PlayerSummary player : players) {
      if (!owners.contains(player.playerId())) {
        rows.put(player.playerId(), new TerritoryAccessRow(player.playerId(), player.playerName(), false));
      }
    }
    for (Member member : territory.authorizedMembers()) {
      if (!owners.contains(member.playerId())) {
        rows.put(member.playerId(), new TerritoryAccessRow(member.playerId(), member.playerName(), true));
      }
    }
    return filterAccess(List.copyOf(rows.values()));
  }

  public List<PlayerSummary> transferRows() {
    UUID owner = territory.summary().ownerId();
    return players.stream().filter(player -> !owner.equals(player.playerId()))
        .filter(this::matches).toList();
  }

  public List<TerritoryRuleRow> ruleRows() {
    return territory.rules().stream()
        .map(rule -> new TerritoryRuleRow(rule.action(), rule.level()))
        .filter(this::matches)
        .toList();
  }

  public int rowCount() {
    return switch (view) {
      case ACCESS -> accessRows().size();
      case RULES -> ruleRows().size();
      case TRANSFER -> transferRows().size();
      case MAIN -> 0;
    };
  }

  public int totalPages() {
    return Math.max(1, (rowCount() + pageSize - 1) / pageSize);
  }

  public List<TerritoryAccessRow> visibleAccessRows() {
    return slice(accessRows());
  }

  public List<TerritoryRuleRow> visibleRuleRows() {
    return slice(ruleRows());
  }

  public List<PlayerSummary> visibleTransferRows() {
    return slice(transferRows());
  }

  private List<TerritoryAccessRow> filterAccess(List<TerritoryAccessRow> rows) {
    return rows.stream().filter(row -> matches(row.playerName())).toList();
  }

  private boolean matches(TerritoryRuleRow row) {
    return matches(row.action().id()) || matches("message.territory.rule." + row.action().id());
  }

  private boolean matches(PlayerSummary player) { return matches(player.playerName()); }

  private boolean matches(String value) {
    String needle = filter.trim().toLowerCase(Locale.ROOT);
    return needle.isEmpty() || value.toLowerCase(Locale.ROOT).contains(needle);
  }

  private <T> List<T> slice(List<T> values) {
    int start = Math.min(scroll * pageSize, values.size());
    return values.subList(start, Math.min(values.size(), start + pageSize));
  }

  /** Whether an action is meaningful for the current view and lifecycle state. */
  public boolean can(TerritoryDetailAction action) {
    Objects.requireNonNull(action, "action");
    if (action == TerritoryDetailAction.BACK) return true;
    if (action == TerritoryDetailAction.RETRY) return screenState == ScreenState.ERROR;
    if (screenState != ScreenState.READY && screenState != ScreenState.EMPTY) return false;
    return switch (view) {
      case MAIN -> action == TerritoryDetailAction.RESIZE
          || action == TerritoryDetailAction.BUFFS
          || action == TerritoryDetailAction.ACCESS
          || action == TerritoryDetailAction.RULES
          || action == TerritoryDetailAction.TRANSFER;
      case ACCESS -> action == TerritoryDetailAction.TOGGLE_ACCESS;
      case RULES -> action == TerritoryDetailAction.CYCLE_RULE;
      case TRANSFER -> action == TerritoryDetailAction.TRANSFER_OWNERSHIP;
    };
  }
}

package com.mo.economy_system.ui.check;

import com.mo.economy_system.common.check.ClientFileCheckResultController;
import com.mo.economy_system.common.check.ClientFileCheckStatus;
import com.mo.economy_system.ui.core.ScreenState;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Immutable UI projection of the client-side comparison controller. */
public record CheckResultState(
    String targetName,
    String checkTypeId,
    ClientFileCheckStatus remoteStatus,
    int remoteFileCount,
    int remoteSkippedCount,
    String remoteErrorCode,
    ClientFileCheckResultController.LocalState localState,
    String localErrorCode,
    boolean localIncomplete,
    List<CheckResultRow> rows,
    String filter,
    int offset,
    int pageSize,
    ScreenState screenState,
    Set<CheckResultAction> actions) {
  public CheckResultState {
    targetName = Objects.requireNonNullElse(targetName, "");
    checkTypeId = Objects.requireNonNullElse(checkTypeId, "");
    remoteStatus = Objects.requireNonNull(remoteStatus, "remoteStatus");
    if (remoteFileCount < 0 || remoteSkippedCount < 0 || offset < 0 || pageSize < 1) {
      throw new IllegalArgumentException("invalid check result state");
    }
    localState = Objects.requireNonNull(localState, "localState");
    rows = List.copyOf(Objects.requireNonNull(rows, "rows"));
    filter = Objects.requireNonNullElse(filter, "");
    screenState = Objects.requireNonNull(screenState, "screenState");
    actions = Set.copyOf(Objects.requireNonNull(actions, "actions"));
  }

  public List<CheckResultRow> filteredRows() {
    String needle = filter.trim().toLowerCase(Locale.ROOT);
    if (needle.isEmpty()) return rows;
    return rows.stream()
        .filter(row -> row.fileName().toLowerCase(Locale.ROOT).contains(needle))
        .toList();
  }

  public int maxOffset() {
    return Math.max(0, filteredRows().size() - pageSize);
  }

  public List<CheckResultRow> visibleRows() {
    List<CheckResultRow> values = filteredRows();
    int start = Math.min(offset, values.size());
    return values.subList(start, Math.min(values.size(), start + pageSize));
  }

  public boolean can(CheckResultAction action) {
    return actions.contains(action);
  }
}

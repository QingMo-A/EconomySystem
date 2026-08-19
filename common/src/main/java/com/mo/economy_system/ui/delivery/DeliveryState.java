package com.mo.economy_system.ui.delivery;

import com.mo.economy_system.ui.core.ScreenState;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record DeliveryState(
    List<DeliveryRow> rows,
    int page,
    int pageSize,
    String filter,
    DeliveryCategory category,
    UUID selectedEntryId,
    ScreenState screenState,
    String errorKey,
    long requestId,
    Set<DeliveryAction> actions) {

  /** Compatibility constructor retained for existing tests and target adapters. */
  public DeliveryState(
      List<DeliveryRow> rows,
      int page,
      int pageSize,
      String filter,
      ScreenState screenState,
      String errorKey,
      long requestId,
      Set<DeliveryAction> actions) {
    this(rows, page, pageSize, filter, DeliveryCategory.ALL, defaultSelection(rows),
        screenState, errorKey, requestId, actions);
  }

  public DeliveryState {
    rows = List.copyOf(Objects.requireNonNull(rows, "rows"));
    if (page < 0 || pageSize < 1 || requestId < -1) throw new IllegalArgumentException("invalid delivery state");
    filter = Objects.requireNonNullElse(filter, "");
    category = Objects.requireNonNull(category, "category");
    screenState = Objects.requireNonNull(screenState, "screenState");
    actions = Set.copyOf(Objects.requireNonNull(actions, "actions"));
    if (selectedEntryId != null) {
      boolean selectionExists = false;
      for (DeliveryRow row : rows) {
        if (row.entryId().equals(selectedEntryId)) {
          selectionExists = true;
          break;
        }
      }
      if (!selectionExists) selectedEntryId = null;
    }
  }

  public List<DeliveryRow> filteredRows() {
    String needle = filter.trim().toLowerCase(Locale.ROOT);
    return rows.stream().filter(row -> {
      if (!row.matchesCategory(category)) return false;
      if (needle.isEmpty()) return true;
      if (row.mailId().toString().contains(needle) || row.searchableText().contains(needle)
          || row.mail().type().name().toLowerCase(Locale.ROOT).contains(needle)) return true;
      for (var attachment : row.mail().attachments()) {
        if (attachment.item().itemId().toLowerCase(Locale.ROOT).contains(needle)) return true;
      }
      return false;
    }).toList();
  }

  public int totalPages() {
    return Math.max(1, (filteredRows().size() + pageSize - 1) / pageSize);
  }

  public List<DeliveryRow> visibleRows() {
    List<DeliveryRow> values = filteredRows();
    int start = Math.min(page * pageSize, values.size());
    return values.subList(start, Math.min(values.size(), start + pageSize));
  }

  public boolean can(DeliveryAction action) {
    return actions.contains(action);
  }

  public DeliveryRow find(UUID entryId) {
    if (entryId == null) return null;
    return rows.stream().filter(row -> row.entryId().equals(entryId)).findFirst().orElse(null);
  }

  /** Selected mail in the active filter, falling back to the first visible mail. */
  public DeliveryRow selectedRow() {
    List<DeliveryRow> filtered = filteredRows();
    if (filtered.isEmpty()) return null;
    if (selectedEntryId != null) {
      for (DeliveryRow row : filtered) {
        if (row.entryId().equals(selectedEntryId)) return row;
      }
    }
    return filtered.get(0);
  }

  public int count(DeliveryCategory requested) {
    if (requested == null || requested == DeliveryCategory.ALL) return rows.size();
    return (int) rows.stream().filter(row -> row.matchesCategory(requested)).count();
  }

  private static UUID defaultSelection(List<DeliveryRow> rows) {
    return rows == null || rows.isEmpty() ? null : rows.get(0).entryId();
  }
}

package com.mo.economy_system.common.check;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class ClientFileCheckResultController {
  public enum LocalState {
    NOT_REQUIRED,
    LOADING,
    READY,
    BUSY,
    FAILED
  }

  public enum RowType {
    COMPARISON,
    SKIPPED
  }

  public record UiRow(String fileName, String reasonId, RowType type) {
    public UiRow {
      fileName = ClientFileCheckValidation.fileName(fileName);
      Objects.requireNonNull(reasonId, "reasonId");
      Objects.requireNonNull(type, "type");
    }
  }

  private final ClientFileCheckResult remote;
  private long generation = 1;
  private LocalState localState;
  private List<ClientFileCheckComparison.Row> comparison = List.of();

  public ClientFileCheckResultController(ClientFileCheckResult remote) {
    this.remote = Objects.requireNonNull(remote, "remote");
    localState = needsComparison() ? LocalState.LOADING : LocalState.NOT_REQUIRED;
  }

  public boolean needsComparison() {
    return remote.status() == ClientFileCheckStatus.SUCCESS
        || remote.status() == ClientFileCheckStatus.TRUNCATED;
  }

  public synchronized long generation() {
    return generation;
  }

  public synchronized LocalState localState() {
    return localState;
  }

  public synchronized long retry() {
    if (!needsComparison() || (localState != LocalState.BUSY && localState != LocalState.FAILED))
      return -1;
    generation++;
    comparison = List.of();
    localState = LocalState.LOADING;
    return generation;
  }

  public synchronized void busy(long expectedGeneration) {
    if (expectedGeneration == generation && needsComparison()) localState = LocalState.BUSY;
  }

  public synchronized void failed(long expectedGeneration) {
    if (expectedGeneration == generation && needsComparison()) localState = LocalState.FAILED;
  }

  public synchronized boolean apply(long expectedGeneration, ClientFileCheckResult local) {
    if (expectedGeneration != generation || !needsComparison()) return false;
    comparison = ClientFileCheckComparison.compare(remote, local);
    localState = LocalState.READY;
    return true;
  }

  public synchronized void invalidate() {
    generation++;
    comparison = List.of();
  }

  public synchronized List<UiRow> rows() {
    List<UiRow> rows = new ArrayList<>(comparison.size() + remote.skipped().size());
    for (ClientFileCheckComparison.Row row : comparison)
      rows.add(
          new UiRow(
              row.fileName(), row.kind().name().toLowerCase(Locale.ROOT), RowType.COMPARISON));
    for (ClientFileCheckSkippedEntry row : remote.skipped())
      rows.add(new UiRow(row.fileName(), row.reason().toLowerCase(Locale.ROOT), RowType.SKIPPED));
    return List.copyOf(rows);
  }

  public synchronized List<UiRow> filtered(String query) {
    List<UiRow> rows = rows();
    if (query == null || query.isBlank()) return rows;
    String normalized = query.toLowerCase(Locale.ROOT);
    return rows.stream()
        .filter(row -> row.fileName().toLowerCase(Locale.ROOT).contains(normalized))
        .toList();
  }

  public ClientFileCheckResult remote() {
    return remote;
  }
}

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
    READY_INCOMPLETE,
    BUSY,
    FAILED
  }
  public enum LocalApplyOutcome { APPLIED, INCOMPLETE, FAILED, STALE }

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
  private List<ClientFileCheckSkippedEntry> localSkipped = List.of();
  private String localErrorCode;

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
    if (!needsComparison()
        || (localState != LocalState.BUSY
            && localState != LocalState.FAILED
            && localState != LocalState.READY_INCOMPLETE))
      return -1;
    generation++;
    comparison = List.of();
    localSkipped = List.of();
    localErrorCode = null;
    localState = LocalState.LOADING;
    return generation;
  }

  public synchronized void busy(long expectedGeneration) {
    if (expectedGeneration == generation && needsComparison()) localState = LocalState.BUSY;
  }

  public synchronized void failed(long expectedGeneration) {
    if (expectedGeneration == generation && needsComparison()) {
      comparison = List.of();
      localErrorCode = "SCAN_FAILED";
      localState = LocalState.FAILED;
    }
  }

  public synchronized LocalApplyOutcome acceptLocalResult(
      long expectedGeneration, ClientFileCheckResult local) {
    if (expectedGeneration != generation || !needsComparison()) return LocalApplyOutcome.STALE;
    if (local == null
        || local.schemaVersion() != ClientFileCheckResult.SCHEMA_VERSION
        || local.checkType() != remote.checkType()) {
      return invalidLocal();
    }
    localSkipped = local.skipped();
    return switch (local.status()) {
      case SUCCESS -> {
        comparison = ClientFileCheckComparison.compare(remote, local);
        localErrorCode = null;
        localState = LocalState.READY;
        yield LocalApplyOutcome.APPLIED;
      }
      case TRUNCATED -> {
        comparison = ClientFileCheckComparison.compare(remote, local);
        localErrorCode = local.errorCode();
        localState = LocalState.READY_INCOMPLETE;
        yield LocalApplyOutcome.INCOMPLETE;
      }
      case FAILED -> {
        comparison = List.of();
        localErrorCode = local.errorCode();
        localState = LocalState.FAILED;
        yield LocalApplyOutcome.FAILED;
      }
      case DECLINED -> invalidLocal();
    };
  }

  private LocalApplyOutcome invalidLocal() {
    comparison = List.of();
    localSkipped = List.of();
    localErrorCode = "INVALID_LOCAL_RESULT";
    localState = LocalState.FAILED;
    return LocalApplyOutcome.FAILED;
  }

  public synchronized void invalidate() {
    generation++;
    comparison = List.of();
    localSkipped = List.of();
  }

  public synchronized List<UiRow> rows() {
    List<UiRow> rows =
        new ArrayList<>(comparison.size() + remote.skipped().size() + localSkipped.size());
    for (ClientFileCheckComparison.Row row : comparison)
      rows.add(
          new UiRow(
              row.fileName(), row.kind().name().toLowerCase(Locale.ROOT), RowType.COMPARISON));
    for (ClientFileCheckSkippedEntry row : remote.skipped())
      rows.add(new UiRow(row.fileName(), row.reason().toLowerCase(Locale.ROOT), RowType.SKIPPED));
    for (ClientFileCheckSkippedEntry row : localSkipped)
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

  public synchronized String localErrorCode() { return localErrorCode; }
  public synchronized List<ClientFileCheckSkippedEntry> localSkipped() { return localSkipped; }
  public synchronized boolean localIncomplete() { return localState == LocalState.READY_INCOMPLETE; }
}

package com.mo.economy_system.common.check;

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

  private final ClientFileCheckResult remote;
  private long generation = 1;
  private LocalState localState;
  private List<ClientFileCheckComparison.Row> rows = List.of();

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

  public synchronized List<ClientFileCheckComparison.Row> rows() {
    return rows;
  }

  public synchronized void busy() {
    if (needsComparison()) localState = LocalState.BUSY;
  }

  public synchronized void failed() {
    if (needsComparison()) localState = LocalState.FAILED;
  }

  public synchronized boolean apply(long expectedGeneration, ClientFileCheckResult local) {
    if (expectedGeneration != generation || !needsComparison()) return false;
    rows = ClientFileCheckComparison.compare(remote, local);
    localState = LocalState.READY;
    return true;
  }

  public synchronized void invalidate() {
    generation++;
    rows = List.of();
  }

  public synchronized List<ClientFileCheckComparison.Row> filtered(String query) {
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

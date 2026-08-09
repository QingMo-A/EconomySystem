package com.mo.economy_system.ui.check;

import com.mo.economy_system.common.check.ClientFileCheckResult;
import com.mo.economy_system.common.check.ClientFileCheckResultController;
import com.mo.economy_system.common.check.ClientFileCheckStatus;
import com.mo.economy_system.ui.core.AbstractEconomyScreenController;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Common query/filter/scroll/retry state machine for a checked-file result. */
public final class CheckResultController
    extends AbstractEconomyScreenController<CheckResultState, CheckResultEvent> {
  private final ClientFileCheckResult remote;
  private final ClientFileCheckResultController comparison;
  private final CheckResultPort port;

  public CheckResultController(String targetName, ClientFileCheckResult remote, CheckResultPort port) {
    super(initial(targetName, Objects.requireNonNull(remote, "remote")));
    this.remote = remote;
    this.comparison = new ClientFileCheckResultController(remote);
    this.port = Objects.requireNonNull(port, "port");
  }

  private static CheckResultState initial(String targetName, ClientFileCheckResult remote) {
    ClientFileCheckResultController.LocalState local =
        remote.status() == ClientFileCheckStatus.SUCCESS || remote.status() == ClientFileCheckStatus.TRUNCATED
            ? ClientFileCheckResultController.LocalState.LOADING
            : ClientFileCheckResultController.LocalState.NOT_REQUIRED;
    return new CheckResultState(
        targetName,
        remote.checkType().id(),
        remote.status(),
        remote.files().size(),
        remote.skipped().size(),
        remote.errorCode(),
        local,
        null,
        false,
        List.of(),
        "",
        0,
        6,
        screenState(local),
        actions(remote, local));
  }

  @Override
  public void handle(CheckResultEvent event) {
    if (event instanceof CheckResultEvent.Initialize) initialize();
    else if (event instanceof CheckResultEvent.LocalScanBusy value) busy(value.generation());
    else if (event instanceof CheckResultEvent.LocalScanCompleted value) completed(value.generation(), value.result());
    else if (event instanceof CheckResultEvent.LocalScanFailed value) failed(value.generation());
    else if (event instanceof CheckResultEvent.FilterChanged value) filter(value.value());
    else if (event instanceof CheckResultEvent.Scroll value) scroll(value.steps());
    else if (event instanceof CheckResultEvent.ViewportChanged value) viewport(value.visibleRows());
    else if (event instanceof CheckResultEvent.ActionClicked value) action(value.action());
  }

  public long generation() {
    return comparison.generation();
  }

  public boolean needsComparison() {
    return comparison.needsComparison();
  }

  public void dispose() {
    port.cancelLocalScan();
    comparison.invalidate();
  }

  private void initialize() {
    if (!comparison.needsComparison()
        || comparison.localState() != ClientFileCheckResultController.LocalState.LOADING) {
      return;
    }
    long generation = comparison.generation();
    comparison.busy(generation);
    refresh(state().filter(), state().offset(), state().pageSize());
    port.startLocalScan(generation);
  }

  private void busy(long generation) {
    comparison.busy(generation);
    refresh(state().filter(), state().offset(), state().pageSize());
  }

  private void completed(long generation, ClientFileCheckResult local) {
    comparison.acceptLocalResult(generation, local);
    refresh(state().filter(), state().offset(), state().pageSize());
  }

  private void failed(long generation) {
    comparison.failed(generation);
    refresh(state().filter(), state().offset(), state().pageSize());
  }

  private void filter(String value) {
    refresh(value == null ? "" : value, 0, state().pageSize());
  }

  private void scroll(int steps) {
    if (steps == 0) return;
    refresh(state().filter(), state().offset() - Integer.signum(steps), state().pageSize());
  }

  private void viewport(int visibleRows) {
    if (visibleRows < 1) return;
    refresh(state().filter(), state().offset(), visibleRows);
  }

  private void action(CheckResultAction action) {
    if (action == null || !state().can(action)) return;
    if (action == CheckResultAction.BACK) {
      dispose();
      navigate(new UiNavigation.Back());
      return;
    }
    long generation = comparison.retry();
    if (generation < 0) return;
    comparison.busy(generation);
    refresh(state().filter(), 0, state().pageSize());
    port.startLocalScan(generation);
  }

  private void refresh(String filter, int requestedOffset, int visibleRows) {
    List<CheckResultRow> rows = comparison.rows().stream()
        .map(row -> new CheckResultRow(
            row.fileName(),
            row.type() == ClientFileCheckResultController.RowType.SKIPPED
                ? "screen.check_result.skip_reason." + row.reasonId()
                : "screen.check_result." + row.reasonId(),
            row.type() == ClientFileCheckResultController.RowType.SKIPPED))
        .toList();
    ClientFileCheckResultController.LocalState local = comparison.localState();
    String nextFilter = filter == null ? "" : filter;
    int maxOffset = maxOffset(rows, nextFilter, visibleRows);
    int offset = Math.max(0, Math.min(maxOffset, requestedOffset));
    replaceState(new CheckResultState(
        state().targetName(),
        remote.checkType().id(),
        remote.status(),
        remote.files().size(),
        remote.skipped().size(),
        remote.errorCode(),
        local,
        comparison.localErrorCode(),
        comparison.localIncomplete(),
        rows,
        nextFilter,
        offset,
        visibleRows,
        screenState(local),
        actions(remote, local)));
  }

  private static int maxOffset(List<CheckResultRow> rows, String filter, int visibleRows) {
    String needle = filter == null ? "" : filter.trim().toLowerCase(java.util.Locale.ROOT);
    int count = needle.isEmpty()
        ? rows.size()
        : (int) rows.stream().filter(row -> row.fileName().toLowerCase(java.util.Locale.ROOT).contains(needle)).count();
    return Math.max(0, count - visibleRows);
  }

  private static Set<CheckResultAction> actions(
      ClientFileCheckResult remote, ClientFileCheckResultController.LocalState local) {
    boolean retry = (remote.status() == ClientFileCheckStatus.SUCCESS || remote.status() == ClientFileCheckStatus.TRUNCATED)
        && (local == ClientFileCheckResultController.LocalState.BUSY
            || local == ClientFileCheckResultController.LocalState.FAILED
            || local == ClientFileCheckResultController.LocalState.READY_INCOMPLETE);
    return retry ? Set.of(CheckResultAction.RETRY, CheckResultAction.BACK) : Set.of(CheckResultAction.BACK);
  }

  private static ScreenState screenState(ClientFileCheckResultController.LocalState local) {
    return switch (local) {
      case LOADING, BUSY -> ScreenState.LOADING;
      case FAILED -> ScreenState.ERROR;
      case NOT_REQUIRED, READY, READY_INCOMPLETE -> ScreenState.READY;
    };
  }
}

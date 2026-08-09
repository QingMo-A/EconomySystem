package com.mo.economy_system.ui.check;

import com.mo.economy_system.common.check.ClientFileCheckResult;

/** Target lifecycle and input events for the common result controller. */
public sealed interface CheckResultEvent
    permits CheckResultEvent.Initialize,
        CheckResultEvent.LocalScanBusy,
        CheckResultEvent.LocalScanCompleted,
        CheckResultEvent.LocalScanFailed,
        CheckResultEvent.FilterChanged,
        CheckResultEvent.Scroll,
        CheckResultEvent.ViewportChanged,
        CheckResultEvent.ActionClicked {
  record Initialize() implements CheckResultEvent {}

  record LocalScanBusy(long generation) implements CheckResultEvent {}

  record LocalScanCompleted(long generation, ClientFileCheckResult result) implements CheckResultEvent {}

  record LocalScanFailed(long generation) implements CheckResultEvent {}

  record FilterChanged(String value) implements CheckResultEvent {}

  record Scroll(int steps) implements CheckResultEvent {}

  record ViewportChanged(int visibleRows) implements CheckResultEvent {}

  record ActionClicked(CheckResultAction action) implements CheckResultEvent {}
}

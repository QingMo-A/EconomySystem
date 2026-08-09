package com.mo.economy_system.ui.check;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.check.ClientFileCheckEntry;
import com.mo.economy_system.common.check.ClientFileCheckResult;
import com.mo.economy_system.common.check.ClientFileCheckResultController;
import com.mo.economy_system.common.check.ClientFileCheckStatus;
import com.mo.economy_system.common.check.ClientFileCheckType;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.List;
import org.junit.jupiter.api.Test;

class CheckResultControllerTest {
  @Test
  void startsOnceRejectsStaleResultsAndOwnsFilteringAndScrollBounds() {
    FakePort port = new FakePort();
    CheckResultController controller = new CheckResultController("target", result("a.jar"), port);

    controller.handle(new CheckResultEvent.Initialize());
    controller.handle(new CheckResultEvent.Initialize());
    assertEquals(List.of(1L), port.started);
    assertEquals(ClientFileCheckResultController.LocalState.BUSY, controller.state().localState());
    assertEquals(ScreenState.LOADING, controller.state().screenState());

    controller.handle(new CheckResultEvent.LocalScanCompleted(2, result("b.jar")));
    assertEquals(ClientFileCheckResultController.LocalState.BUSY, controller.state().localState());
    controller.handle(new CheckResultEvent.LocalScanCompleted(1, result("b.jar")));
    assertEquals(ClientFileCheckResultController.LocalState.READY, controller.state().localState());
    assertEquals(2, controller.state().rows().size());

    controller.handle(new CheckResultEvent.ViewportChanged(1));
    controller.handle(new CheckResultEvent.FilterChanged("b."));
    assertEquals(1, controller.state().filteredRows().size());
    assertEquals(0, controller.state().offset());
    controller.handle(new CheckResultEvent.FilterChanged(""));
    controller.handle(new CheckResultEvent.Scroll(-1));
    assertEquals(1, controller.state().offset());
    assertEquals(1, controller.state().visibleRows().size());
  }

  @Test
  void failureCanRetryAndBackCancelsTheTargetTask() {
    FakePort port = new FakePort();
    CheckResultController controller = new CheckResultController("target", result("a.jar"), port);
    controller.handle(new CheckResultEvent.Initialize());
    controller.handle(new CheckResultEvent.LocalScanFailed(1));

    assertEquals(ClientFileCheckResultController.LocalState.FAILED, controller.state().localState());
    assertTrue(controller.state().can(CheckResultAction.RETRY));
    controller.handle(new CheckResultEvent.ActionClicked(CheckResultAction.RETRY));
    assertEquals(List.of(1L, 2L), port.started);
    assertEquals(ClientFileCheckResultController.LocalState.BUSY, controller.state().localState());

    controller.handle(new CheckResultEvent.ActionClicked(CheckResultAction.BACK));
    assertEquals(1, port.cancelled);
    assertTrue(controller.pollNavigation().orElseThrow() instanceof UiNavigation.Back);
  }

  private static ClientFileCheckResult result(String fileName) {
    return new ClientFileCheckResult(
        ClientFileCheckResult.SCHEMA_VERSION,
        ClientFileCheckStatus.SUCCESS,
        ClientFileCheckType.MODS,
        List.of(new ClientFileCheckEntry(fileName, 1, "a".repeat(64))),
        List.of(),
        null);
  }

  private static final class FakePort implements CheckResultPort {
    private final java.util.ArrayList<Long> started = new java.util.ArrayList<>();
    private int cancelled;

    @Override
    public void startLocalScan(long generation) {
      started.add(generation);
    }

    @Override
    public void cancelLocalScan() {
      cancelled++;
    }
  }
}

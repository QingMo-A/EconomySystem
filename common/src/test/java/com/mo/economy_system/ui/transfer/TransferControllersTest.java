package com.mo.economy_system.ui.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import org.junit.jupiter.api.Test;

class TransferControllersTest {
  @Test
  void consentDispatchesOnlyOneDecisionAndExpiresThroughTheSameFinishPath() {
    ConsentPort port = new ConsentPort();
    TransferConsentController controller = new TransferConsentController(
        "requester", "mods", "example.jar", 12, "a".repeat(64), port);

    controller.handle(new TransferConsentEvent.ActionClicked(TransferConsentAction.ALLOW));
    controller.handle(new TransferConsentEvent.Expired());
    controller.handle(new TransferConsentEvent.ActionClicked(TransferConsentAction.DECLINE));

    assertEquals(1, port.allowed);
    assertEquals(0, port.declined);
    assertEquals(0, port.expired);
    assertEquals(ScreenState.IDLE, controller.state().screenState());
    assertTrue(controller.pollNavigation().orElseThrow() instanceof UiNavigation.Back);
  }

  @Test
  void resultRetainsFailureMessageThenClosesAfterDiscard() {
    ResultPort port = new ResultPort(
        TransferResultPort.Outcome.failed("message.transfer.move_failed"),
        TransferResultPort.Outcome.closed());
    TransferResultController controller = new TransferResultController(artifact(), port);

    controller.handle(new TransferResultEvent.ActionClicked(TransferResultAction.SAVE));
    assertEquals(ScreenState.ERROR, controller.state().screenState());
    assertEquals("message.transfer.move_failed", controller.state().actionErrorKey());
    assertTrue(controller.state().can(TransferResultAction.DISCARD));

    controller.handle(new TransferResultEvent.ArtifactStateChanged("message.transfer.state.cleanup_pending"));
    assertEquals("message.transfer.state.cleanup_pending", controller.state().artifactStateKey());
    controller.handle(new TransferResultEvent.ActionClicked(TransferResultAction.DISCARD));

    assertEquals(1, port.saved);
    assertEquals(1, port.discarded);
    assertEquals(1, port.closed);
    assertEquals(ScreenState.IDLE, controller.state().screenState());
    assertTrue(controller.pollNavigation().orElseThrow() instanceof UiNavigation.Back);
  }

  @Test
  void terminalCanOnlyClose() {
    ResultPort port = new ResultPort(TransferResultPort.Outcome.closed(), TransferResultPort.Outcome.closed());
    TransferResultController controller = new TransferResultController(
        TransferResultState.terminal("message.transfer.status.failed", "message.transfer.expired"), port);

    controller.handle(new TransferResultEvent.ActionClicked(TransferResultAction.SAVE));
    controller.handle(new TransferResultEvent.ActionClicked(TransferResultAction.CLOSE));

    assertEquals(0, port.saved);
    assertEquals(1, port.closed);
    assertTrue(controller.pollNavigation().orElseThrow() instanceof UiNavigation.Back);
  }

  private static TransferResultState artifact() {
    return TransferResultState.artifact(
        "target", "mods", "example.jar", 12, "a".repeat(64), "message.transfer.state.pending");
  }

  private static final class ConsentPort implements TransferConsentPort {
    private int allowed;
    private int declined;
    private int expired;
    @Override public void allow() { allowed++; }
    @Override public void decline() { declined++; }
    @Override public void expire() { expired++; }
  }

  private static final class ResultPort implements TransferResultPort {
    private final Outcome saveOutcome;
    private final Outcome discardOutcome;
    private int saved;
    private int discarded;
    private int closed;

    private ResultPort(Outcome saveOutcome, Outcome discardOutcome) {
      this.saveOutcome = saveOutcome;
      this.discardOutcome = discardOutcome;
    }

    @Override public Outcome save() { saved++; return saveOutcome; }
    @Override public Outcome discard() { discarded++; return discardOutcome; }
    @Override public void close() { closed++; }
  }
}

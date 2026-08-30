package com.mo.economy_system.ui.commission_public;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.common.commission.PublicCommission;
import com.mo.economy_system.common.network.commission_public.PublicCommissionActionResponseMessage;
import com.mo.economy_system.common.network.commission_public.PublicCommissionDataResponseMessage;
import com.mo.economy_system.common.network.commission_public.PublicCommissionSubmitStatus;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PublicCommissionCenterControllerTest {
  private static final UUID COMMISSION_ID = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff");

  @Test
  void gatesDataByRequestIdAndSelectsFirstEntry() {
    FakePort port = new FakePort();
    PublicCommissionCenterController controller = new PublicCommissionCenterController(port);
    controller.handle(new PublicCommissionCenterEvent.Initialize(10L));
    assertEquals(ScreenState.LOADING, controller.state().screenState());
    assertEquals(1L, port.requests.get(0));

    PublicCommission commission = commission(10);
    controller.handle(new PublicCommissionCenterEvent.DataLoaded(
        PublicCommissionDataResponseMessage.data(2L, 100L, List.of(commission))));
    assertEquals(ScreenState.LOADING, controller.state().screenState());

    controller.handle(new PublicCommissionCenterEvent.DataLoaded(
        PublicCommissionDataResponseMessage.data(1L, 100L, List.of(commission))));
    assertEquals(ScreenState.READY, controller.state().screenState());
    assertEquals(COMMISSION_ID, controller.state().selectedCommissionId());
    assertEquals(100L, controller.state().serverNowMillis());
  }

  @Test
  void submitIsSingleFlightAndAcceptedResultRefreshesSnapshot() {
    FakePort port = new FakePort();
    PublicCommissionCenterController controller = loadedController(port);
    controller.handle(new PublicCommissionCenterEvent.ActionClicked(
        PublicCommissionCenterAction.SUBMIT, COMMISSION_ID, 4));
    assertTrue(controller.state().submitInFlight());
    assertEquals(1, port.submits.size());
    long submitRequest = port.submits.get(0).requestId();

    controller.handle(new PublicCommissionCenterEvent.ActionClicked(
        PublicCommissionCenterAction.SUBMIT, COMMISSION_ID, 4));
    assertEquals(1, port.submits.size());

    controller.handle(new PublicCommissionCenterEvent.ActionResult(
        new PublicCommissionActionResponseMessage(submitRequest,
            PublicCommissionSubmitStatus.ACCEPTED, 4, 12, "奖励已发送至邮箱")));
    assertFalse(controller.state().submitInFlight());
    assertEquals(PublicCommissionSubmitStatus.ACCEPTED, controller.state().lastSubmitStatus());
    assertEquals(ScreenState.LOADING, controller.state().screenState());
    assertEquals(2, port.requests.size());
  }

  @Test
  void rejectsInvalidSubmitAndHandlesErrorTimeoutAndBack() {
    FakePort port = new FakePort();
    PublicCommissionCenterController controller = loadedController(port);
    controller.handle(new PublicCommissionCenterEvent.ActionClicked(
        PublicCommissionCenterAction.SUBMIT, COMMISSION_ID, 0));
    controller.handle(new PublicCommissionCenterEvent.ActionClicked(
        PublicCommissionCenterAction.SUBMIT, COMMISSION_ID, 2_305));
    assertTrue(port.submits.isEmpty());

    controller.handle(new PublicCommissionCenterEvent.ActionClicked(PublicCommissionCenterAction.BACK, null, 0));
    assertEquals(EconomyUiRoute.HOME,
        ((UiNavigation.Route) controller.pollNavigation().orElseThrow()).route());

    PublicCommissionCenterController waiting = new PublicCommissionCenterController(port);
    waiting.handle(new PublicCommissionCenterEvent.Initialize(50L));
    waiting.handle(new PublicCommissionCenterEvent.Tick(50L + PublicCommissionCenterController.TIMEOUT_NANOS));
    assertEquals(ScreenState.ERROR, waiting.state().screenState());
    assertEquals("screen.commissions.public.sync_timeout", waiting.state().errorKey());
  }

  @Test
  void errorResponseAndStaleActionResultDoNotMutateReadyData() {
    FakePort port = new FakePort();
    PublicCommissionCenterController controller = loadedController(port);
    controller.handle(new PublicCommissionCenterEvent.ActionClicked(
        PublicCommissionCenterAction.SUBMIT, COMMISSION_ID, 1));
    long submitRequest = port.submits.get(0).requestId();
    controller.handle(new PublicCommissionCenterEvent.ActionResult(
        new PublicCommissionActionResponseMessage(submitRequest + 1,
            PublicCommissionSubmitStatus.REJECTED, 0, 0, "stale")));
    assertTrue(controller.state().submitInFlight());
    assertTrue(controller.state().actionMessage().isBlank());

    controller.handle(new PublicCommissionCenterEvent.ActionResult(
        new PublicCommissionActionResponseMessage(submitRequest,
            PublicCommissionSubmitStatus.REJECTED, 0, 0, "库存不足")));
    assertFalse(controller.state().submitInFlight());
    assertEquals(ScreenState.ERROR, controller.state().screenState());
    assertEquals("库存不足", controller.state().actionMessage());
  }

  private static PublicCommissionCenterController loadedController(FakePort port) {
    PublicCommissionCenterController controller = new PublicCommissionCenterController(port);
    controller.handle(new PublicCommissionCenterEvent.Initialize(1L));
    controller.handle(new PublicCommissionCenterEvent.DataLoaded(
        PublicCommissionDataResponseMessage.data(1L, 100L, List.of(commission(10)))));
    return controller;
  }

  private static PublicCommission commission(int amount) {
    return PublicCommission.create(COMMISSION_ID, "City expansion", "town_hall", "Town Hall",
        "minecraft:stone", amount, 3, 1_000L, 10_000L, "Build the central road");
  }

  private static final class FakePort implements PublicCommissionCenterPort {
    private long next;
    private final List<Long> requests = new ArrayList<>();
    private final List<Submit> submits = new ArrayList<>();

    @Override public long nextRequestId() { return ++next; }
    @Override public void requestData(long requestId) { requests.add(requestId); }
    @Override public void submit(long requestId, UUID commissionId, UUID submissionId, int amount) {
      submits.add(new Submit(requestId, commissionId, submissionId, amount));
    }
  }

  private record Submit(long requestId, UUID commissionId, UUID submissionId, int amount) {}
}

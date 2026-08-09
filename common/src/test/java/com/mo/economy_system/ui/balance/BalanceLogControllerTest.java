package com.mo.economy_system.ui.balance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.core.economy_system.BalanceLogEntry;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.List;
import org.junit.jupiter.api.Test;

class BalanceLogControllerTest {
  @Test
  void rejectsStaleCategoryAndOffsetResponsesAndPages() {
    FakePort port = new FakePort();
    BalanceLogController controller = new BalanceLogController(port);
    controller.handle(new BalanceLogEvent.Initialize(10));
    assertEquals(1, port.lastRequest);
    BalanceLogEntry entry = new BalanceLogEntry(1, "系统", "seed", 2, 0, 2);
    controller.handle(new BalanceLogEvent.DataLoaded(1, "市场", 0, 2, 3, List.of(entry)));
    assertEquals(ScreenState.LOADING, controller.state().screenState());
    controller.handle(new BalanceLogEvent.DataLoaded(1, "全部", 0, 2, 3, List.of(entry)));
    assertEquals(ScreenState.READY, controller.state().screenState());
    assertEquals(2, controller.state().limit());
    controller.handle(new BalanceLogEvent.NextPage());
    assertEquals(2, port.lastOffset);
    controller.handle(new BalanceLogEvent.DataLoaded(2, "全部", 2, 2, 3, List.of(entry)));
    assertEquals(2, controller.state().offset());
    controller.handle(new BalanceLogEvent.CategoryChanged("市场"));
    assertEquals("市场", controller.state().category());
    assertEquals(3, port.lastRequest);
  }

  @Test
  void timesOutRetriesAndNavigates() {
    FakePort port = new FakePort();
    BalanceLogController controller = new BalanceLogController(port);
    controller.handle(new BalanceLogEvent.Initialize(1));
    controller.handle(new BalanceLogEvent.Tick(1 + BalanceLogController.TIMEOUT_NANOS));
    assertEquals(ScreenState.ERROR, controller.state().screenState());
    controller.handle(new BalanceLogEvent.ActionClicked(BalanceLogAction.RETRY, 20));
    assertEquals(2, port.lastRequest);
    controller.handle(new BalanceLogEvent.ActionClicked(BalanceLogAction.BACK, 0));
    assertEquals(EconomyUiRoute.HOME,
        ((UiNavigation.Route) controller.pollNavigation().orElseThrow()).route());
  }

  private static final class FakePort implements BalanceLogPort {
    private long next;
    private long lastRequest = -1;
    private int lastOffset;
    @Override public long nextRequestId() { return ++next; }
    @Override public void requestPage(long id, String category, int offset, int limit) {
      lastRequest = id; lastOffset = offset;
    }
  }
}

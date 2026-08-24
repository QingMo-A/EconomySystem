package com.mo.economy_system.ui.territory.invite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryInviteControllerTest {
  private static final UUID TERRITORY = new UUID(0, 1);
  private static final UUID OWNER = new UUID(0, 2);
  private static final UUID VIEWER = new UUID(0, 3);
  private static final UUID TARGET = new UUID(0, 4);

  @Test
  void loadsFiltersPagesAndDebouncesDuplicateInvites() {
    FakePort port = new FakePort();
    TerritoryInviteController controller = controller(port);
    controller.handle(new TerritoryInviteEvent.Initialize(10));
    assertEquals(ScreenState.LOADING, controller.state().screenState());
    controller.handle(new TerritoryInviteEvent.PlayersLoaded(2, 1, List.of(new PlayerSummary(TARGET, "target"))));
    assertEquals(ScreenState.LOADING, controller.state().screenState());
    controller.handle(new TerritoryInviteEvent.PlayersLoaded(1, 1, List.of(
        new PlayerSummary(OWNER, "owner"), new PlayerSummary(VIEWER, "viewer"),
        new PlayerSummary(TARGET, "target"), new PlayerSummary(new UUID(0, 5), "other"))));
    assertEquals(ScreenState.READY, controller.state().screenState());
    controller.handle(new TerritoryInviteEvent.InviteClicked(TARGET, 20));
    controller.handle(new TerritoryInviteEvent.InviteClicked(TARGET, 21));
    controller.handle(new TerritoryInviteEvent.InviteClicked(TARGET, 100));
    assertEquals(List.of(TARGET), port.invites);
    assertInstanceOf(UiNavigation.Back.class, controller.pollNavigation().orElseThrow());
    controller.handle(new TerritoryInviteEvent.FilterChanged("other"));
    assertEquals(List.of("other"), controller.state().visiblePlayers().stream().map(PlayerSummary::playerName).toList());
  }

  @Test
  void eligibleEmptyErrorStaleDuplicateAndUnknownResponsesAreFailSafe() {
    FakePort port = new FakePort();
    TerritoryInviteController controller = controller(port);
    controller.handle(new TerritoryInviteEvent.Initialize(10));
    controller.handle(new TerritoryInviteEvent.PlayersLoaded(2, 1,
        List.of(new PlayerSummary(TARGET, "stale"))));
    controller.handle(new TerritoryInviteEvent.PlayersFailed(9, "unknown"));
    assertEquals(ScreenState.LOADING, controller.state().screenState());

    controller.handle(new TerritoryInviteEvent.PlayersLoaded(1, 2, List.of(
        new PlayerSummary(OWNER, "owner"), new PlayerSummary(VIEWER, "viewer"))));
    assertEquals(ScreenState.EMPTY, controller.state().screenState());
    controller.handle(new TerritoryInviteEvent.PlayersLoaded(1, 3,
        List.of(new PlayerSummary(TARGET, "duplicate"))));
    assertEquals(ScreenState.EMPTY, controller.state().screenState());

    controller.handle(new TerritoryInviteEvent.Retry(20));
    controller.handle(new TerritoryInviteEvent.PlayersFailed(2, "screen.invite.sync_failed"));
    assertEquals(ScreenState.ERROR, controller.state().screenState());
    assertEquals("screen.invite.sync_failed", controller.state().errorKey());
  }

  @Test
  void olderPlayerRevisionIsRejectedAndViewportClampsAgainstNewPageSize() {
    FakePort port = new FakePort();
    TerritoryInviteController controller = controller(port);
    controller.handle(new TerritoryInviteEvent.Initialize(1));
    controller.handle(new TerritoryInviteEvent.PlayersLoaded(1, 5, List.of(
        new PlayerSummary(TARGET, "a"),
        new PlayerSummary(new UUID(0, 5), "b"),
        new PlayerSummary(new UUID(0, 6), "c"))));
    controller.handle(new TerritoryInviteEvent.ViewportChanged(1));
    controller.handle(new TerritoryInviteEvent.Scroll(1));
    controller.handle(new TerritoryInviteEvent.Scroll(1));
    assertEquals(2, controller.state().page());
    controller.handle(new TerritoryInviteEvent.ViewportChanged(3));
    assertEquals(0, controller.state().page());

    controller.handle(new TerritoryInviteEvent.Retry(2));
    controller.handle(new TerritoryInviteEvent.PlayersLoaded(2, 4,
        List.of(new PlayerSummary(new UUID(0, 7), "older"))));
    assertEquals(ScreenState.LOADING, controller.state().screenState());
  }

  @Test
  void timeoutRetryAndBackAreCommonBehavior() {
    FakePort port = new FakePort();
    TerritoryInviteController controller = controller(port);
    controller.handle(new TerritoryInviteEvent.Initialize(1));
    controller.handle(new TerritoryInviteEvent.Tick(1 + TerritoryInviteController.TIMEOUT_NANOS));
    assertEquals(ScreenState.ERROR, controller.state().screenState());
    controller.handle(new TerritoryInviteEvent.Retry(2));
    assertEquals(2, port.lastRequest);
    controller.handle(new TerritoryInviteEvent.Back());
    assertInstanceOf(UiNavigation.Back.class, controller.pollNavigation().orElseThrow());
  }

  private static TerritoryInviteController controller(FakePort port) {
    return new TerritoryInviteController(TERRITORY, "territory", OWNER, VIEWER, Set.of(), port);
  }

  private static final class FakePort implements TerritoryInvitePort {
    private long next;
    private long lastRequest = -1;
    private final List<UUID> invites = new ArrayList<>();
    @Override public long nextRequestId() { return ++next; }
    @Override public void requestPlayers(long requestId) { lastRequest = requestId; }
    @Override public void submitInvite(UUID territoryId, UUID playerId) { invites.add(playerId); }
  }
}

package com.mo.economy_system.ui.territory;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryManageControllerTest {
    private static final UUID TERRITORY = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final long START = 1_000L;

    @Test
    void requestsDataAndRejectsStaleResponses() {
        FakePort port = new FakePort();
        TerritoryManageController controller = new TerritoryManageController(
                TERRITORY, "home", OWNER, "owner", port);
        assertEquals(ScreenState.IDLE, controller.state().screenState());
        controller.handle(new TerritoryManageEvent.Initialize(START));
        assertEquals(ScreenState.LOADING, controller.state().screenState());
        assertEquals(1L, port.requestId);
        controller.handle(new TerritoryManageEvent.DataLoaded(2L,
                List.of(new MemberRow(OWNER, "owner"))));
        assertEquals(ScreenState.LOADING, controller.state().screenState());
        controller.handle(new TerritoryManageEvent.DataLoaded(1L,
                List.of(new MemberRow(OWNER, "owner"))));
        assertEquals(ScreenState.READY, controller.state().screenState());
        controller.handle(new TerritoryManageEvent.DataFailed(1L, "duplicate"));
        assertEquals(ScreenState.READY, controller.state().screenState());
    }

    @Test
    void filteringAndPagingAreCommonBehavior() {
        FakePort port = new FakePort();
        TerritoryManageController controller = new TerritoryManageController(
                TERRITORY, "home", OWNER, "owner", port);
        controller.handle(new TerritoryManageEvent.Initialize(START));
        controller.handle(new TerritoryManageEvent.DataLoaded(1L, List.of(
                new MemberRow(OWNER, "owner"),
                new MemberRow(UUID.randomUUID(), "alice"),
                new MemberRow(UUID.randomUUID(), "bob"))));
        controller.handle(new TerritoryManageEvent.ViewportChanged(2));
        controller.handle(new TerritoryManageEvent.NextPage());
        assertEquals(1, controller.state().page());
        assertEquals(2, controller.state().scrollOffset());
        controller.handle(new TerritoryManageEvent.Scroll(1));
        assertEquals(1, controller.state().page());
        controller.handle(new TerritoryManageEvent.Scroll(-1));
        assertEquals(0, controller.state().page());
        controller.handle(new TerritoryManageEvent.FilterChanged("alice"));
        assertEquals(0, controller.state().page());
        assertEquals(0, controller.state().scrollOffset());
        assertEquals(1, controller.state().visibleMembers().size());
        assertEquals("alice", controller.state().visibleMembers().get(0).playerName());
    }

    @Test
    void emptyErrorTimeoutAndRetryAreExplicitStates() {
        FakePort port = new FakePort();
        TerritoryManageController controller = controller(port);
        controller.handle(new TerritoryManageEvent.Initialize(START));
        controller.handle(new TerritoryManageEvent.DataLoaded(1L, List.of()));
        assertEquals(ScreenState.EMPTY, controller.state().screenState());

        controller.handle(new TerritoryManageEvent.Retry(2_000L));
        controller.handle(new TerritoryManageEvent.DataFailed(2L, "screen.territory.sync_failed"));
        assertEquals(ScreenState.ERROR, controller.state().screenState());
        assertTrue(controller.state().can(TerritoryManageAction.RETRY));

        controller.handle(new TerritoryManageEvent.Retry(3_000L));
        controller.handle(new TerritoryManageEvent.Tick(3_000L + TerritoryManageController.TIMEOUT_NANOS - 1));
        assertEquals(ScreenState.LOADING, controller.state().screenState());
        controller.handle(new TerritoryManageEvent.Tick(3_000L + TerritoryManageController.TIMEOUT_NANOS));
        assertEquals(ScreenState.ERROR, controller.state().screenState());
        assertEquals("message.territory.sync_timeout", controller.state().errorKey());
        assertEquals(3, port.requests);
    }

    @Test
    void actionsUsePortOrNavigationIntent() {
        FakePort port = new FakePort();
        TerritoryManageController controller = new TerritoryManageController(
                TERRITORY, "home", OWNER, "owner", port);
        controller.handle(new TerritoryManageEvent.Initialize(START));
        controller.handle(new TerritoryManageEvent.ActionClicked(TerritoryManageAction.INVITE, null));
        assertNull(port.opened);
        controller.handle(new TerritoryManageEvent.DataLoaded(1L, List.of(
                new MemberRow(UUID.randomUUID(), "alice"))));
        UUID member = controller.state().members().get(0).playerId();
        controller.handle(new TerritoryManageEvent.ActionClicked(TerritoryManageAction.KICK, member));
        assertEquals(member, port.target);
        controller.handle(new TerritoryManageEvent.ActionClicked(TerritoryManageAction.BUFFS, null));
        assertEquals(TerritoryManageAction.BUFFS, port.opened);
        controller.handle(new TerritoryManageEvent.ActionClicked(TerritoryManageAction.BACK, null));
        assertInstanceOf(UiNavigation.Back.class, controller.pollNavigation().orElseThrow());
        assertTrue(controller.pollNavigation().isEmpty());
    }

    private static TerritoryManageController controller(FakePort port) {
        return new TerritoryManageController(TERRITORY, "home", OWNER, "owner", port);
    }

    private static final class FakePort implements TerritoryManagePort {
        private long requestId;
        private UUID target;
        private TerritoryManageAction opened;
        private int requests;

        @Override
        public long nextRequestId() {
            return ++requestId;
        }

        @Override
        public void requestMembers(UUID territoryId, long requestId) {
            requests++;
        }

        @Override
        public void submit(UUID territoryId, TerritoryManageAction action, UUID targetPlayerId) {
            target = targetPlayerId;
        }

        @Override
        public void confirm(UUID territoryId, TerritoryManageAction action, UUID targetPlayerId) {
            target = targetPlayerId;
        }

        @Override
        public void open(UUID territoryId, TerritoryManageAction action) {
            opened = action;
        }
    }
}

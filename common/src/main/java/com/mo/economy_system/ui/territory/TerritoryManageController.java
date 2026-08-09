package com.mo.economy_system.ui.territory;

import com.mo.economy_system.ui.core.AbstractEconomyScreenController;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.Set;
import java.util.UUID;

/** Shared territory-management interaction and request state machine. */
public final class TerritoryManageController
        extends AbstractEconomyScreenController<TerritoryManageState, TerritoryManageEvent> {
    public static final long TIMEOUT_NANOS = 10_000_000_000L;

    private final TerritoryManagePort port;
    private final UUID territoryId;
    private final String territoryName;
    private final UUID ownerId;
    private final String ownerName;
    private long startedAt;
    private boolean requestInFlight;

    public TerritoryManageController(UUID territoryId, String territoryName,
                                     UUID ownerId, String ownerName,
                                     TerritoryManagePort port) {
        super(new TerritoryManageState(territoryId, territoryName, ownerId, ownerName,
                java.util.List.of(), 0, 1, 0, "", ScreenState.IDLE, null, -1,
                Set.of(TerritoryManageAction.BACK)));
        this.territoryId = territoryId;
        this.territoryName = territoryName;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.port = port;
    }

    @Override
    public void handle(TerritoryManageEvent event) {
        if (event instanceof TerritoryManageEvent.Initialize initialize) {
            request(initialize.nowNanos());
        } else if (event instanceof TerritoryManageEvent.Retry retry) {
            request(retry.nowNanos());
        } else if (event instanceof TerritoryManageEvent.DataLoaded loaded) {
            loaded(loaded);
        } else if (event instanceof TerritoryManageEvent.DataFailed failed) {
            failed(failed);
        } else if (event instanceof TerritoryManageEvent.FilterChanged changed) {
            updateFilter(changed.value());
        } else if (event instanceof TerritoryManageEvent.ViewportChanged changed) {
            updatePageSize(changed.pageSize());
        } else if (event instanceof TerritoryManageEvent.Scroll scroll) {
            changePage(Integer.signum(scroll.steps()));
        } else if (event instanceof TerritoryManageEvent.NextPage) {
            changePage(1);
        } else if (event instanceof TerritoryManageEvent.PreviousPage) {
            changePage(-1);
        } else if (event instanceof TerritoryManageEvent.ActionClicked clicked) {
            action(clicked);
        } else if (event instanceof TerritoryManageEvent.Tick tick) {
            tick(tick.nowNanos());
        }
    }

    private void request(long nowNanos) {
        long requestId = port.nextRequestId();
        if (requestId < 0) throw new IllegalStateException("request id must be non-negative");
        startedAt = nowNanos;
        requestInFlight = true;
        replaceState(copy(0, 0, state().filter(), ScreenState.LOADING, null, requestId,
                Set.of(TerritoryManageAction.BACK)));
        port.requestMembers(territoryId, requestId);
    }

    private void loaded(TerritoryManageEvent.DataLoaded event) {
        if (state().requestId() != event.requestId() || state().screenState() != ScreenState.LOADING) return;
        Set<TerritoryManageAction> actions = Set.of(TerritoryManageAction.COPY_ID,
                TerritoryManageAction.INVITE,
                TerritoryManageAction.KICK, TerritoryManageAction.MODIFY_MODE,
                TerritoryManageAction.BUFFS, TerritoryManageAction.ACCESS,
                TerritoryManageAction.PERMISSIONS, TerritoryManageAction.TRANSFER,
                TerritoryManageAction.DELETE,
                TerritoryManageAction.BACK);
        replaceState(copy(0, 0, state().filter(), event.members().isEmpty() ? ScreenState.EMPTY : ScreenState.READY,
                null, -1, actions, event.members()));
        requestInFlight = false;
    }

    private void failed(TerritoryManageEvent.DataFailed event) {
        if (state().requestId() != event.requestId()) return;
        if (state().screenState() != ScreenState.LOADING) return;
        replaceState(copy(state().page(), state().scrollOffset(), state().filter(), ScreenState.ERROR,
                event.errorKey(), -1, Set.of(TerritoryManageAction.RETRY,
                        TerritoryManageAction.BACK), state().members()));
        requestInFlight = false;
    }

    private void updateFilter(String value) {
        String filter = value == null ? "" : value;
        replaceState(copy(0, 0, filter, state().screenState(), state().errorKey(),
                state().requestId(), state().actions(), state().members()));
    }

    private void updatePageSize(int pageSize) {
        if (pageSize == state().pageSize()) return;
        int page = Math.min(state().page(), totalPages(state().filteredMembers().size(), pageSize) - 1);
        int scrollOffset = Math.min(page * pageSize,
                Math.max(0, state().filteredMembers().size() - pageSize));
        replaceState(new TerritoryManageState(territoryId, territoryName, ownerId, ownerName,
                state().members(), page, pageSize, scrollOffset, state().filter(), state().screenState(),
                state().errorKey(), state().requestId(), state().actions()));
    }

    private void changePage(int delta) {
        int page = Math.max(0, Math.min(state().totalPages() - 1, state().page() + delta));
        replaceState(copy(page, page * state().pageSize(), state().filter(),
                state().screenState(), state().errorKey(),
                state().requestId(), state().actions(), state().members()));
    }

    private void action(TerritoryManageEvent.ActionClicked clicked) {
        if (!state().can(clicked.action())) return;
        if (clicked.action() == TerritoryManageAction.RETRY) return;
        if (clicked.action() == TerritoryManageAction.BACK) {
            navigate(new UiNavigation.Back());
            return;
        }
        if (clicked.action() == TerritoryManageAction.KICK) {
            port.confirm(territoryId, clicked.action(), clicked.targetPlayerId());
        } else if (clicked.action() == TerritoryManageAction.MODIFY_MODE) {
            port.submit(territoryId, clicked.action(), clicked.targetPlayerId());
        } else {
            port.open(territoryId, clicked.action());
        }
    }

    private void tick(long nowNanos) {
        if (requestInFlight && nowNanos - startedAt >= TIMEOUT_NANOS) {
            replaceState(copy(state().page(), state().scrollOffset(), state().filter(), ScreenState.ERROR,
                    "message.territory.sync_timeout", -1,
                    Set.of(TerritoryManageAction.RETRY, TerritoryManageAction.BACK), state().members()));
            requestInFlight = false;
        }
    }

    private TerritoryManageState copy(int page, int scrollOffset, String filter, ScreenState status,
                                      String error, long requestId,
                                      Set<TerritoryManageAction> actions) {
        return copy(page, scrollOffset, filter, status, error, requestId, actions, state().members());
    }

    private TerritoryManageState copy(int page, int scrollOffset, String filter, ScreenState status,
                                      String error, long requestId,
                                      Set<TerritoryManageAction> actions,
                                      java.util.List<MemberRow> members) {
        return new TerritoryManageState(territoryId, territoryName, ownerId, ownerName,
                members, page, Math.max(1, state().pageSize()), scrollOffset,
                filter, status, error, requestId, actions);
    }

    private static int totalPages(int size, int pageSize) {
        return Math.max(1, (size + pageSize - 1) / pageSize);
    }
}

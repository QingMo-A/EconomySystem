package com.mo.economy_system.ui.territory.buff;

import com.mo.economy_system.ui.core.AbstractEconomyScreenController;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import com.mo.economy_system.common.territory.TerritorySnapshots.Buff;
import java.util.List;
import java.util.UUID;

public final class BuffManageController
        extends AbstractEconomyScreenController<BuffManageState, BuffManageEvent> {
    public static final long TIMEOUT_NANOS = 10_000_000_000L;
    private final BuffManagePort port;
    private final UUID territoryId;
    private final String territoryName;
    private long startedAt;
    private boolean inFlight;

    public BuffManageController(UUID territoryId, String territoryName, List<Buff> initial,
                                BuffManagePort port) {
        super(new BuffManageState(territoryId, territoryName, inspect(initial, port), 0, 1, 0, "",
                ScreenState.IDLE, null, -1));
        this.territoryId = territoryId;
        this.territoryName = territoryName;
        this.port = port;
    }

    @Override public void handle(BuffManageEvent event) {
        if (event instanceof BuffManageEvent.Initialize e) request(e.nowNanos());
        else if (event instanceof BuffManageEvent.Retry e) request(e.nowNanos());
        else if (event instanceof BuffManageEvent.DataLoaded e) loaded(e);
        else if (event instanceof BuffManageEvent.DataFailed e) failed(e);
        else if (event instanceof BuffManageEvent.FilterChanged e) filter(e.value());
        else if (event instanceof BuffManageEvent.ViewportChanged e) viewport(e.pageSize());
        else if (event instanceof BuffManageEvent.NextPage) page(1);
        else if (event instanceof BuffManageEvent.PreviousPage) page(-1);
        else if (event instanceof BuffManageEvent.Scroll e) page(Integer.signum(e.steps()));
        else if (event instanceof BuffManageEvent.ActionClicked e) action(e);
        else if (event instanceof BuffManageEvent.Tick e) tick(e.nowNanos());
    }

    private void request(long now) {
        long id = port.nextRequestId();
        if (id < 0) throw new IllegalStateException("buff request id exhausted");
        startedAt = now;
        inFlight = true;
        replace(new BuffManageState(territoryId, territoryName, state().buffs(), 0,
                state().pageSize(), 0, state().filter(), ScreenState.LOADING, null, id));
        port.request(territoryId, id);
    }

    private void loaded(BuffManageEvent.DataLoaded e) {
        if (!inFlight || state().screenState() != ScreenState.LOADING
                || state().requestId() != e.requestId()) return;
        inFlight = false;
        List<BuffRow> rows = inspect(e.buffs(), port);
        replace(new BuffManageState(territoryId, territoryName, rows, 0, state().pageSize(), 0,
                state().filter(), rows.isEmpty() ? ScreenState.EMPTY : ScreenState.READY, null, -1));
    }

    private void failed(BuffManageEvent.DataFailed e) {
        if (!inFlight || state().screenState() != ScreenState.LOADING
                || state().requestId() != e.requestId()) return;
        inFlight = false;
        replace(new BuffManageState(territoryId, territoryName, state().buffs(), state().page(),
                state().pageSize(), state().scrollOffset(), state().filter(), ScreenState.ERROR,
                e.errorKey(), -1));
    }

    private void filter(String value) {
        replace(new BuffManageState(territoryId, territoryName, state().buffs(), 0, state().pageSize(),
                0, value, state().screenState(), state().errorKey(), state().requestId()));
    }

    private void viewport(int size) {
        int page = Math.min(state().page(), pages(state().filteredBuffs().size(), size) - 1);
        replace(new BuffManageState(territoryId, territoryName, state().buffs(), page, size,
                page * size, state().filter(), state().screenState(), state().errorKey(), state().requestId()));
    }

    private void page(int delta) {
        int page = Math.max(0, Math.min(state().totalPages() - 1, state().page() + delta));
        replace(new BuffManageState(territoryId, territoryName, state().buffs(), page,
                state().pageSize(), page * state().pageSize(), state().filter(), state().screenState(),
                state().errorKey(), state().requestId()));
    }

    private void action(BuffManageEvent.ActionClicked e) {
        if (e.action() == BuffAction.BACK) { navigate(new UiNavigation.Back()); return; }
        if (e.action() == BuffAction.RETRY) { request(e.nowNanos()); return; }
        if (state().screenState() != ScreenState.READY || inFlight) return;
        BuffRow row = state().filteredBuffs().stream()
                .filter(v -> v.buff().id().equals(e.buffId())).findFirst().orElse(null);
        if (row == null || row.action() != e.action()) return;
        if (row.availability() != BuffAvailability.AVAILABLE) {
            port.feedback(feedbackKey(row.availability()));
            return;
        }
        port.submit(territoryId, e.action(), e.buffId());
        request(e.nowNanos());
    }

    private void tick(long now) {
        if (inFlight && now - startedAt >= TIMEOUT_NANOS) {
            inFlight = false;
            replace(new BuffManageState(territoryId, territoryName, state().buffs(), state().page(),
                    state().pageSize(), state().scrollOffset(), state().filter(), ScreenState.ERROR,
                    "screen.territory.buff.sync_timeout", -1));
        }
    }

    private static List<BuffRow> inspect(List<Buff> buffs, BuffManagePort port) {
        return buffs.stream().map(buff -> {
            try {
                return BuffRow.inspect(buff, port.inspect(
                        com.mo.economy_system.common.territory.TerritoryBuffCost.aggregate(buff)));
            } catch (RuntimeException failure) {
                return BuffRow.inspect(buff, BuffResourceSnapshot.unknown());
            }
        }).toList();
    }

    private static String feedbackKey(BuffAvailability availability) {
        return switch (availability) {
            case MAX_LEVEL -> "message.territory.management.max_level";
            case MISSING_ITEMS -> "message.territory.management.insufficient_items";
            case MISSING_EXPERIENCE -> "message.territory.management.insufficient_experience";
            case MISSING_ITEMS_AND_EXPERIENCE -> "message.territory.buff.requirements_missing";
            case INVALID_COST -> "message.territory.management.invalid_cost";
            case AVAILABLE -> "";
        };
    }

    private void replace(BuffManageState next) { replaceState(next); }
    private static int pages(int count, int size) { return Math.max(1, (count + size - 1) / size); }
}

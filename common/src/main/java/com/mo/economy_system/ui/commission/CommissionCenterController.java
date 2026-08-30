package com.mo.economy_system.ui.commission;

import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.common.commission.CommissionInstance;
import com.mo.economy_system.common.network.CommissionDataResponseMessage;
import com.mo.economy_system.common.network.CommissionDataResponseKind;
import com.mo.economy_system.ui.core.AbstractEconomyScreenController;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CommissionCenterController
    extends AbstractEconomyScreenController<CommissionCenterState, CommissionCenterEvent> {
  public static final long TIMEOUT_NANOS = 10_000_000_000L;
  private final CommissionCenterPort port;
  private long startedAt;
  private boolean requestInFlight;

  public CommissionCenterController(CommissionCenterPort port) {
    super(new CommissionCenterState(List.of(), 0L, 0L, 1, null, ScreenState.IDLE, "", -1L));
    this.port = java.util.Objects.requireNonNull(port, "port");
  }

  @Override public void handle(CommissionCenterEvent event) {
    if (event instanceof CommissionCenterEvent.Initialize value) request(value.nowNanos());
    else if (event instanceof CommissionCenterEvent.DataLoaded value) loaded(value.response());
    else if (event instanceof CommissionCenterEvent.DataFailed value) failed(value.errorKey());
    else if (event instanceof CommissionCenterEvent.Selected value) select(value.commissionId());
    else if (event instanceof CommissionCenterEvent.ActionClicked value) action(value);
    else if (event instanceof CommissionCenterEvent.Tick value) tick(value.nowNanos());
  }

  private void request(long nowNanos) {
    long id = port.nextRequestId();
    startedAt = nowNanos;
    requestInFlight = true;
    replaceState(new CommissionCenterState(state().commissions(), state().nextRefreshAt(),
        state().serverNowMillis(), state().maxActivePersonalCommissions(), state().selectedCommissionId(),
        ScreenState.LOADING, "", id));
    port.requestData(id);
  }

  private void loaded(CommissionDataResponseMessage response) {
    if (!requestInFlight || response.requestId() != state().requestId()) return;
    requestInFlight = false;
    if (response.kind() == CommissionDataResponseKind.ERROR) {
      failed(response.errorKey());
      return;
    }
    UUID selected = state().selectedCommissionId();
    UUID currentSelection = selected;
    if (response.commissions().stream().noneMatch(value -> value.commissionId().equals(currentSelection))) {
      selected = response.commissions().isEmpty() ? null : response.commissions().get(0).commissionId();
    }
    replaceState(new CommissionCenterState(response.commissions(), response.nextRefreshAt(),
        response.serverNowMillis(), response.maxActivePersonalCommissions(), selected,
        response.commissions().isEmpty() ? ScreenState.EMPTY : ScreenState.READY, "", state().requestId()));
  }

  private void failed(String errorKey) {
    requestInFlight = false;
    replaceState(new CommissionCenterState(state().commissions(), state().nextRefreshAt(),
        state().serverNowMillis(), state().maxActivePersonalCommissions(), state().selectedCommissionId(),
        ScreenState.ERROR, errorKey == null || errorKey.isBlank() ? "screen.commissions.sync_failed" : errorKey, -1L));
  }

  private void select(UUID id) {
    if (id != null && state().commissions().stream().noneMatch(value -> value.commissionId().equals(id))) return;
    replaceState(copy(id));
  }

  private void action(CommissionCenterEvent.ActionClicked event) {
    switch (event.action()) {
      case BACK -> navigate(new UiNavigation.Route(EconomyUiRoute.HOME));
      case RETRY -> request(System.nanoTime());
      case SUBMIT -> {
        CommissionInstance selected = state().commissions().stream()
            .filter(value -> value.commissionId().equals(event.commissionId())).findFirst().orElse(null);
        if (selected == null || selected.type() != com.mo.economy_system.common.commission.CommissionType.ITEM_DELIVERY
            || selected.status().terminal() || event.amount() <= 0) return;
        port.submit(port.nextRequestId(), selected.commissionId(), UUID.randomUUID(), event.amount());
        request(System.nanoTime());
      }
    }
  }

  private void tick(long nowNanos) {
    if (requestInFlight && nowNanos - startedAt >= TIMEOUT_NANOS) failed("screen.commissions.sync_timeout");
  }

  private CommissionCenterState copy(UUID selected) {
    return new CommissionCenterState(state().commissions(), state().nextRefreshAt(), state().serverNowMillis(),
        state().maxActivePersonalCommissions(), selected, state().screenState(), state().errorKey(), state().requestId());
  }
}

package com.mo.economy_system.ui.commission_public;

import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.common.commission.PublicCommission;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.network.commission_public.PublicCommissionDataResponseKind;
import com.mo.economy_system.common.network.commission_public.PublicCommissionDataResponseMessage;
import com.mo.economy_system.common.network.commission_public.PublicCommissionSubmitStatus;
import com.mo.economy_system.ui.core.AbstractEconomyScreenController;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Common request, stale-response, timeout and submission state machine for public commissions. */
public final class PublicCommissionCenterController
    extends AbstractEconomyScreenController<PublicCommissionCenterState, PublicCommissionCenterEvent> {
  public static final long TIMEOUT_NANOS = 10_000_000_000L;

  private final PublicCommissionCenterPort port;
  private long startedAt;
  private boolean requestInFlight;
  private long pendingSubmitRequestId = -1L;

  public PublicCommissionCenterController(PublicCommissionCenterPort port) {
    super(new PublicCommissionCenterState(List.of(), 0L, null, ScreenState.IDLE, "", -1L,
        false, null, ""));
    this.port = Objects.requireNonNull(port, "port");
  }

  @Override
  public void handle(PublicCommissionCenterEvent event) {
    Objects.requireNonNull(event, "event");
    if (event instanceof PublicCommissionCenterEvent.Initialize value) request(value.nowNanos());
    else if (event instanceof PublicCommissionCenterEvent.DataLoaded value) loaded(value.response());
    else if (event instanceof PublicCommissionCenterEvent.DataFailed value) failed(value.requestId(), value.errorKey());
    else if (event instanceof PublicCommissionCenterEvent.ActionResult value) actionResult(value.response());
    else if (event instanceof PublicCommissionCenterEvent.ActionClicked value) action(value);
    else if (event instanceof PublicCommissionCenterEvent.Selected value) select(value.commissionId());
    else if (event instanceof PublicCommissionCenterEvent.Tick value) tick(value.nowNanos());
  }

  private void request(long nowNanos) {
    long id = port.nextRequestId();
    if (id < 0) throw new IllegalStateException("public commission request id must be non-negative");
    startedAt = nowNanos;
    requestInFlight = true;
    replaceState(copy(state().selectedCommissionId(), ScreenState.LOADING, "", id,
        false, state().lastSubmitStatus(), state().actionMessage(), state().serverNowMillis(),
        state().commissions()));
    port.requestData(id);
  }

  private void loaded(PublicCommissionDataResponseMessage response) {
    if (!requestInFlight || response.requestId() != state().requestId()) return;
    requestInFlight = false;
    if (response.kind() == PublicCommissionDataResponseKind.ERROR) {
      setError(response.errorKey());
      return;
    }
    UUID selected = state().selectedCommissionId();
    UUID currentSelection = selected;
    if (response.commissions().stream().noneMatch(value -> value.commissionId().equals(currentSelection))) {
      selected = response.commissions().isEmpty() ? null : response.commissions().get(0).commissionId();
    }
    replaceState(new PublicCommissionCenterState(response.commissions(), response.serverNowMillis(), selected,
        response.commissions().isEmpty() ? ScreenState.EMPTY : ScreenState.READY, "", state().requestId(),
        false, state().lastSubmitStatus(), state().actionMessage()));
  }

  private void failed(long requestId, String errorKey) {
    if (!requestInFlight || requestId != state().requestId()) return;
    requestInFlight = false;
    setError(errorKey);
  }

  private void setError(String errorKey) {
    replaceState(copy(state().selectedCommissionId(), ScreenState.ERROR,
        errorKey == null || errorKey.isBlank() ? "screen.commissions.public.sync_failed" : errorKey,
        -1L, false, state().lastSubmitStatus(), state().actionMessage(), state().serverNowMillis(),
        state().commissions()));
  }

  private void actionResult(com.mo.economy_system.common.network.commission_public.PublicCommissionActionResponseMessage response) {
    if (!state().submitInFlight() || response.requestId() != pendingSubmitRequestId) return;
    pendingSubmitRequestId = -1L;
    PublicCommissionSubmitStatus status = response.status();
    boolean refresh = status == PublicCommissionSubmitStatus.ACCEPTED
        || status == PublicCommissionSubmitStatus.PARTIAL
        || status == PublicCommissionSubmitStatus.COMPLETED
        || status == PublicCommissionSubmitStatus.DUPLICATE
        || status == PublicCommissionSubmitStatus.DELIVERY_RETRY;
    replaceState(copy(state().selectedCommissionId(),
        refresh ? ScreenState.LOADING : (status == PublicCommissionSubmitStatus.REJECTED
            ? ScreenState.ERROR : state().screenState()),
        status == PublicCommissionSubmitStatus.REJECTED
            ? "screen.commissions.public.submit_failed" : "", refresh ? -1L : state().requestId(),
        false, status, response.message(), state().serverNowMillis(), state().commissions()));
    if (refresh) request(System.nanoTime());
  }

  private void select(UUID id) {
    if (id != null && state().commissions().stream().noneMatch(value -> value.commissionId().equals(id))) return;
    replaceState(copy(id, state().screenState(), state().errorKey(), state().requestId(),
        state().submitInFlight(), state().lastSubmitStatus(), state().actionMessage(),
        state().serverNowMillis(), state().commissions()));
  }

  private void action(PublicCommissionCenterEvent.ActionClicked event) {
    switch (event.action()) {
      case BACK -> navigate(new UiNavigation.Route(EconomyUiRoute.HOME));
      case RETRY -> {
        if (!requestInFlight && !state().submitInFlight()) request(System.nanoTime());
      }
      case SUBMIT -> submit(event.commissionId(), event.amount());
    }
  }

  private void submit(UUID requestedId, int amount) {
    if (requestInFlight || state().submitInFlight() || amount <= 0
        || amount > EconomyNetworkLimits.MAX_COMMISSION_SUBMIT_AMOUNT) return;
    PublicCommission selected = requestedId == null ? state().selected() : state().commissions().stream()
        .filter(value -> value.commissionId().equals(requestedId)).findFirst().orElse(null);
    if (selected == null || selected.status() != com.mo.economy_system.common.commission.PublicCommissionStatus.AVAILABLE) return;
    long id = port.nextRequestId();
    if (id < 0) throw new IllegalStateException("public commission request id must be non-negative");
    startedAt = System.nanoTime();
    pendingSubmitRequestId = id;
    replaceState(copy(selected.commissionId(), state().screenState(), state().errorKey(), id,
        true, state().lastSubmitStatus(), state().actionMessage(), state().serverNowMillis(),
        state().commissions()));
    port.submit(id, selected.commissionId(), UUID.randomUUID(), amount);
  }

  private void tick(long nowNanos) {
    if (requestInFlight && nowNanos - startedAt >= TIMEOUT_NANOS) {
      requestInFlight = false;
      setError("screen.commissions.public.sync_timeout");
    } else if (state().submitInFlight() && nowNanos - startedAt >= TIMEOUT_NANOS) {
      pendingSubmitRequestId = -1L;
      replaceState(copy(state().selectedCommissionId(), ScreenState.ERROR,
          "screen.commissions.public.submit_timeout", -1L, false,
          state().lastSubmitStatus(), state().actionMessage(), state().serverNowMillis(),
          state().commissions()));
    }
  }

  private PublicCommissionCenterState copy(UUID selected, ScreenState screenState, String error,
                                            long requestId, boolean submitInFlight,
                                            PublicCommissionSubmitStatus status, String actionMessage,
                                            long now, List<PublicCommission> commissions) {
    return new PublicCommissionCenterState(commissions, now, selected, screenState, error, requestId,
        submitInFlight, status, actionMessage);
  }
}

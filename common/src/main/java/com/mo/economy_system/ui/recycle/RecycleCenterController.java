package com.mo.economy_system.ui.recycle;

import com.mo.economy_system.common.network.RecycleDataResponseKind;
import com.mo.economy_system.common.network.RecycleDataResponseMessage;
import com.mo.economy_system.common.network.RecycleOfferSnapshot;
import com.mo.economy_system.ui.core.AbstractEconomyScreenController;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import java.util.List;
import java.util.UUID;

public final class RecycleCenterController extends AbstractEconomyScreenController<RecycleCenterState, RecycleCenterEvent> {
  public static final long TIMEOUT_NANOS = 10_000_000_000L;
  private final RecycleCenterPort port; private long startedAt; private boolean inFlight;
  public RecycleCenterController(RecycleCenterPort port) { super(new RecycleCenterState(List.of(), "", 0, 0, 0, ScreenState.IDLE, "", -1)); this.port = java.util.Objects.requireNonNull(port); }
  @Override public void handle(RecycleCenterEvent event) {
    if (event instanceof RecycleCenterEvent.Initialize e) request(e.nowNanos());
    else if (event instanceof RecycleCenterEvent.DataLoaded e) loaded(e.response());
    else if (event instanceof RecycleCenterEvent.DataFailed e) failed(e.errorKey());
    else if (event instanceof RecycleCenterEvent.Selected e) select(e.itemId());
    else if (event instanceof RecycleCenterEvent.AmountChanged e) amount(e.amount());
    else if (event instanceof RecycleCenterEvent.ActionClicked e) action(e.action());
    else if (event instanceof RecycleCenterEvent.Tick e) tick(e.nowNanos());
  }
  private void request(long now) { long id = port.nextRequestId(); startedAt = now; inFlight = true; replace(new RecycleCenterState(state().offers(), state().selectedItemId(), state().amount(), state().serverNowMillis(), state().cycleEndsAt(), ScreenState.LOADING, "", id)); port.requestData(id); }
  private void loaded(RecycleDataResponseMessage response) {
    if (!inFlight || response.requestId() != state().requestId()) return; inFlight = false;
    if (response.kind() == RecycleDataResponseKind.ERROR) { failed(response.errorKey()); return; }
    String selected = state().selectedItemId();
    String previousSelection = selected;
    if (response.offers().stream().noneMatch(v -> v.itemId().equals(previousSelection))) selected = response.offers().isEmpty() ? "" : response.offers().get(0).itemId();
    replace(new RecycleCenterState(response.offers(), selected, clamp(state().amount(), find(response.offers(), selected)), response.serverNowMillis(), response.cycleEndsAt(), response.offers().isEmpty() ? ScreenState.EMPTY : ScreenState.READY, "", state().requestId()));
  }
  private void failed(String key) { inFlight = false; replace(new RecycleCenterState(state().offers(), state().selectedItemId(), state().amount(), state().serverNowMillis(), state().cycleEndsAt(), ScreenState.ERROR, key == null || key.isBlank() ? "screen.recycle.sync_failed" : key, -1)); }
  private void select(String id) { RecycleOfferSnapshot offer = find(state().offers(), id); if (offer == null) return; replace(copy(id, clamp(state().amount(), offer))); }
  private void amount(int value) { replace(copy(state().selectedItemId(), clamp(value, state().selected()))); }
  private void action(RecycleCenterAction action) { switch (action) { case BACK -> navigate(new UiNavigation.Route(EconomyUiRoute.HOME)); case RETRY -> request(System.nanoTime()); case SUBMIT -> { RecycleOfferSnapshot offer = state().selected(); if (!inFlight && offer != null && state().amount() > 0) { port.submit(port.nextRequestId(), UUID.randomUUID(), offer.itemId(), state().amount()); request(System.nanoTime()); } } } }
  private void tick(long now) { if (inFlight && now - startedAt >= TIMEOUT_NANOS) failed("screen.recycle.sync_timeout"); }
  private RecycleCenterState copy(String selected, int amount) { return new RecycleCenterState(state().offers(), selected, amount, state().serverNowMillis(), state().cycleEndsAt(), state().screenState(), state().errorKey(), state().requestId()); }
  private static RecycleOfferSnapshot find(List<RecycleOfferSnapshot> offers, String id) { return offers.stream().filter(v -> v.itemId().equals(id == null ? "" : id)).findFirst().orElse(null); }
  private static int clamp(int value, RecycleOfferSnapshot offer) { int max = offer == null ? 0 : offer.maxSubmitAmount(); return max == 0 ? 0 : Math.max(1, Math.min(value <= 0 ? 1 : value, max)); }
  private void replace(RecycleCenterState next) { replaceState(next); }
}

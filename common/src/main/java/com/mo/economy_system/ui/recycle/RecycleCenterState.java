package com.mo.economy_system.ui.recycle;
import com.mo.economy_system.common.network.RecycleOfferSnapshot;
import com.mo.economy_system.ui.core.ScreenState;
import java.util.List;
public record RecycleCenterState(List<RecycleOfferSnapshot> offers, String selectedItemId, int amount,
                                 long serverNowMillis, long cycleEndsAt, ScreenState screenState,
                                 String errorKey, long requestId) {
  public RecycleCenterState {
    offers = List.copyOf(offers); selectedItemId = selectedItemId == null ? "" : selectedItemId;
    errorKey = errorKey == null ? "" : errorKey;
    if (amount < 0) throw new IllegalArgumentException("amount must not be negative");
  }
  public RecycleOfferSnapshot selected() { return offers.stream().filter(v -> v.itemId().equals(selectedItemId)).findFirst().orElse(null); }
}

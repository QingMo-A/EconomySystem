package com.mo.economy_system.ui.market;

import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.ui.core.AbstractEconomyScreenController;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.List;
import java.util.Set;

/** Common validation and interaction state machine for both market forms. */
public final class MarketCreateController
    extends AbstractEconomyScreenController<MarketCreateState, MarketCreateEvent> {
  private final MarketCreatePort port;
  private List<String> completionSuggestions = List.of();
  private int completionSelection = -1;
  private boolean completionOpen;

  public MarketCreateController(MarketCreateMode mode, List<MarketInventoryItem> inventory,
                                MarketCreatePort port) {
    super(initial(mode, inventory));
    this.port = java.util.Objects.requireNonNull(port, "port");
  }

  private static MarketCreateState initial(MarketCreateMode mode, List<MarketInventoryItem> inventory) {
    Set<MarketCreateAction> actions = Set.of(MarketCreateAction.SUBMIT, MarketCreateAction.BACK,
        MarketCreateAction.SELECT_SLOT, MarketCreateAction.DECREMENT,
        MarketCreateAction.INCREMENT, MarketCreateAction.SELECT_ALL);
    int selected = mode == MarketCreateMode.SALES
        ? inventory.stream().filter(MarketInventoryItem::filled).mapToInt(MarketInventoryItem::slot).findFirst().orElse(-1)
        : -1;
    String item = mode == MarketCreateMode.SALES && selected >= 0
        ? inventory.stream().filter(value -> value.slot() == selected).map(MarketInventoryItem::itemId).findFirst().orElse("")
        : "";
    return new MarketCreateState(mode, inventory, selected, item, 1, 0, ScreenState.READY, null, actions);
  }

  @Override public void handle(MarketCreateEvent event) {
    if (event instanceof MarketCreateEvent.InventoryChanged value) inventory(value.inventory());
    else if (event instanceof MarketCreateEvent.SlotSelected value) select(value.slot());
    else if (event instanceof MarketCreateEvent.ItemIdChanged value) item(value.itemId());
    else if (event instanceof MarketCreateEvent.CompletionMoved value) moveCompletion(value.delta());
    else if (event instanceof MarketCreateEvent.CompletionAccepted value) acceptCompletion(value.index());
    else if (event instanceof MarketCreateEvent.CompletionDismissed) dismissCompletion();
    else if (event instanceof MarketCreateEvent.QuantityChanged value) quantity(value.quantity());
    else if (event instanceof MarketCreateEvent.PriceChanged value) price(value.totalPrice());
    else if (event instanceof MarketCreateEvent.ActionClicked value) action(value.action());
  }

  private void inventory(List<MarketInventoryItem> values) {
    int selected = state().selectedSlot();
    final int previousSlot = selected;
    if (values.stream().noneMatch(item -> item.slot() == previousSlot && item.filled())) selected = -1;
    final int selectedSlot = selected;
    String itemId = selectedSlot < 0 ? state().itemId()
        : values.stream().filter(item -> item.slot() == selectedSlot).map(MarketInventoryItem::itemId).findFirst().orElse("");
    replace(new MarketCreateState(state().mode(), values, selected, itemId,
        clampQuantity(state().quantity(), values, selectedSlot), state().totalPrice(), ScreenState.READY,
        null, state().actions()));
  }

  private void select(int slot) {
    if (state().mode() != MarketCreateMode.SALES) return;
    MarketInventoryItem selected = state().inventory().stream()
        .filter(item -> item.slot() == slot && item.filled()).findFirst().orElse(null);
    if (selected == null) return;
    replace(copy(slot, selected.itemId(), 1, state().totalPrice(), null));
  }

  private void item(String value) {
    String itemId = value == null ? "" : value.trim();
    replace(copy(state().selectedSlot(), itemId, state().quantity(), state().totalPrice(), null));
    refreshCompletion(itemId);
  }

  private void refreshCompletion(String itemId) {
    if (state().mode() != MarketCreateMode.DEMAND || itemId.isBlank()) {
      dismissCompletion();
      return;
    }
    List<String> values = port.itemIdSuggestions(itemId);
    if (values == null || values.isEmpty()) {
      dismissCompletion();
      return;
    }
    completionSuggestions = values.stream().filter(value -> value != null && !value.isBlank())
        .distinct().toList();
    completionSelection = completionSuggestions.isEmpty() ? -1 : 0;
    completionOpen = !completionSuggestions.isEmpty();
  }

  private void moveCompletion(int delta) {
    if (!completionOpen || completionSuggestions.isEmpty() || delta == 0) return;
    int size = completionSuggestions.size();
    int next = (completionSelection + delta) % size;
    if (next < 0) next += size;
    completionSelection = next;
  }

  private void acceptCompletion(int index) {
    if (!completionOpen || completionSuggestions.isEmpty()) return;
    int selected = index >= 0 && index < completionSuggestions.size() ? index : completionSelection;
    if (selected < 0 || selected >= completionSuggestions.size()) return;
    String value = completionSuggestions.get(selected);
    replace(copy(state().selectedSlot(), value, state().quantity(), state().totalPrice(), null));
    dismissCompletion();
  }

  private void dismissCompletion() {
    completionSuggestions = List.of();
    completionSelection = -1;
    completionOpen = false;
  }

  /** Current bounded suggestions, empty when the field is unfocused/dismissed. */
  public List<String> completionSuggestions() {
    return completionOpen ? completionSuggestions : List.of();
  }

  public int completionSelection() {
    return completionOpen ? completionSelection : -1;
  }

  public boolean completionOpen() {
    return completionOpen && !completionSuggestions.isEmpty();
  }

  private void quantity(int value) {
    replace(copy(state().selectedSlot(), state().itemId(), boundedQuantity(value), state().totalPrice(), null));
  }

  private void price(int value) {
    replace(copy(state().selectedSlot(), state().itemId(), state().quantity(), Math.max(0, value), null));
  }

  private void action(MarketCreateAction action) {
    if (action == null || !state().can(action)) return;
    switch (action) {
      case BACK -> navigate(new UiNavigation.Back());
      case DECREMENT -> quantity(Math.max(0, state().quantity() - 1));
      case INCREMENT -> quantity(state().quantity() + 1);
      case SELECT_ALL -> quantity(maxQuantity());
      case SUBMIT -> submit();
      case SELECT_SLOT -> { }
    }
  }

  private void submit() {
    String error = validate();
    if (error != null) {
      replace(copy(state().selectedSlot(), state().itemId(), state().quantity(), state().totalPrice(), error));
      return;
    }
    if (state().mode() == MarketCreateMode.SALES) {
      port.submitSales(state().selectedSlot(), state().quantity(), state().unitPrice());
    } else {
      port.submitDemand(state().itemId(), state().quantity(), state().unitPrice());
    }
    navigate(new UiNavigation.Back());
  }

  private String validate() {
    if (state().mode() == MarketCreateMode.SALES) {
      if (state().selectedItem() == null || !state().selectedItem().filled()) return "screen.market.create.no_item";
      if (state().quantity() < 1 || state().quantity() > state().availableQuantity()) return "screen.market.create.invalid_quantity";
    } else {
      if (state().itemId().isBlank() || !port.isKnownItem(state().itemId())) return "screen.market.create.unknown_item";
      int max = Math.max(1, port.maxStackSize(state().itemId()));
      if (state().quantity() < 1 || state().quantity() > max) return "screen.market.create.invalid_quantity";
    }
    if (state().unitPrice() < 1) return "screen.market.create.invalid_price";
    return state().computedOrderTotalFitsInt() ? null : "screen.market.create.price_overflow";
  }

  private int maxQuantity() {
    if (state().mode() == MarketCreateMode.SALES) return state().availableQuantity();
    return state().itemId().isBlank() ? 64 : Math.max(1, port.maxStackSize(state().itemId()));
  }

  /** Keeps typed and button-driven quantities in the same inclusive [1, max] range. */
  private int boundedQuantity(int value) {
    int max = Math.max(1, maxQuantity());
    return Math.min(max, Math.max(1, value));
  }

  private int clampQuantity(int value, List<MarketInventoryItem> inventory, int selectedSlot) {
    if (value < 1) return 1;
    if (state().mode() == MarketCreateMode.SALES) {
      MarketInventoryItem selected = inventory.stream().filter(item -> item.slot() == selectedSlot).findFirst().orElse(null);
      if (selected == null) return 1;
      int available = inventory.stream().filter(item -> item.itemId().equals(selected.itemId()))
          .mapToInt(MarketInventoryItem::count).sum();
      return Math.min(value, Math.max(1, available));
    }
    return Math.min(value, Math.max(1, state().itemId().isBlank() ? 64
        : Math.max(1, port.maxStackSize(state().itemId()))));
  }

  private MarketCreateState copy(int slot, String itemId, int quantity, int price, String error) {
    return new MarketCreateState(state().mode(), state().inventory(), slot, itemId, quantity, price,
        error == null ? ScreenState.READY : ScreenState.ERROR, error, state().actions());
  }

  private void replace(MarketCreateState next) {
    replaceState(next);
  }
}

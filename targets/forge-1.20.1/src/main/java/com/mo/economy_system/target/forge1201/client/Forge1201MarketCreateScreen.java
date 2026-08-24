package com.mo.economy_system.target.forge1201.client;

import com.mo.economy_system.common.network.CreateDemandOrderMessage;
import com.mo.economy_system.common.network.CreateSalesOrderMessage;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.ui.core.UiNavigation;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.market.MarketCreateAction;
import com.mo.economy_system.ui.market.MarketCreateController;
import com.mo.economy_system.ui.market.MarketCreateEvent;
import com.mo.economy_system.ui.market.MarketCreateLayout;
import com.mo.economy_system.ui.market.MarketCreateMode;
import com.mo.economy_system.ui.market.MarketCreatePort;
import com.mo.economy_system.ui.market.MarketCreateView;
import com.mo.economy_system.ui.market.MarketInventoryItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Forge shell translating Minecraft widgets and inventory into the common market form. */
public final class Forge1201MarketCreateScreen extends Screen {
  private final Screen parent;
  private final MarketCreateMode mode;
  private final Port port;
  private final MarketCreateController controller;
  private EditBox itemId;
  private EditBox quantity;
  private EditBox price;
  private boolean syncingInputs;

  public Forge1201MarketCreateScreen(MarketCreateMode mode, Screen parent) {
    super(Component.translatable(mode == MarketCreateMode.SALES
        ? "screen.market.create.sales_title" : "screen.market.create.demand_title"));
    this.mode = mode;
    this.parent = parent;
    Player player = Minecraft.getInstance().player;
    this.port = new Port(player);
    this.controller = new MarketCreateController(mode, inventory(player), port);
  }

  private List<MarketInventoryItem> inventory(Player player) {
    if (player == null) return List.of();
    List<MarketInventoryItem> result = new ArrayList<>();
    for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
      ItemStack stack = player.getInventory().items.get(slot);
      if (!stack.isEmpty()) result.add(new MarketInventoryItem(slot,
          BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount(), stack.getMaxStackSize()));
    }
    return result;
  }

  @Override protected void init() {
    MarketCreateLayout.Layout layout = layout();
    UiScale scale = layout.scale();
    if (mode == MarketCreateMode.DEMAND) {
      itemId = new Forge1201UnderlinedEditBox(font, sx(layout.itemId().x(), scale), sy(layout.itemId().y(), scale),
          sw(layout.itemId().width(), scale), sh(layout.itemId().height(), scale),
          Component.translatable("screen.market.create.item_id"));
      Forge1201UiInputAdapter.apply(itemId);
      itemId.setMaxLength(256);
      itemId.setHint(Component.translatable("screen.market.create.item_id"));
      itemId.setValue(controller.state().itemId());
      itemId.setResponder(value -> {
        if (!syncingInputs) controller.handle(new MarketCreateEvent.ItemIdChanged(value));
      });
      addRenderableWidget(itemId);
    }
    quantity = input(layout.quantity(), "screen.market.create.quantity", Integer.toString(controller.state().quantity()));
    quantity.setResponder(value -> {
      if (!syncingInputs) controller.handle(new MarketCreateEvent.QuantityChanged(parse(value)));
    });
    addRenderableWidget(quantity);
    price = input(layout.price(), "screen.market.create.price", "");
    price.setResponder(value -> {
      if (!syncingInputs) controller.handle(new MarketCreateEvent.PriceChanged(parse(value)));
    });
    addRenderableWidget(price);
  }

  private EditBox input(com.mo.economy_system.ui.geometry.UiRect rect, String hint, String value) {
    UiScale scale = layout().scale();
    EditBox box = new Forge1201UnderlinedEditBox(font, sx(rect.x(), scale), sy(rect.y(), scale), sw(rect.width(), scale), sh(rect.height(), scale), Component.translatable(hint));
    Forge1201UiInputAdapter.apply(box);
    box.setMaxLength(9); box.setHint(Component.translatable(hint)); box.setValue(value); return box;
  }

  private int parse(String value) {
    try { return Integer.parseInt(value.trim()); } catch (NumberFormatException ignored) { return 0; }
  }
  private int sx(int value, UiScale scale) { return Math.round(value * scale.value()); }
  private int sy(int value, UiScale scale) { return Math.round(value * scale.value()); }
  private int sw(int value, UiScale scale) { return Math.max(1, Math.round(value * scale.value())); }
  private int sh(int value, UiScale scale) { return Math.max(1, Math.round(value * scale.value())); }

  @Override public void tick() {
    super.tick();
    if (controller.completionOpen() && (itemId == null || !itemId.isFocused())) {
      controller.handle(new MarketCreateEvent.CompletionDismissed());
    }
    controller.pollNavigation().ifPresent(this::navigate);
  }

  private void navigate(UiNavigation navigation) {
    if (minecraft == null) return;
    if (navigation instanceof UiNavigation.Back) {
      Screen destination = parent == null ? new Forge1201MarketScreen() : parent;
      if (destination instanceof Forge1201MarketScreen market) market.refreshData();
      minecraft.setScreen(destination);
    }
  }

  @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    Forge1201UiRenderer renderer = new Forge1201UiRenderer(graphics, font);
    renderer.fillPhysicalBackground(width, height, MarketCreateLayout.BACKGROUND_COLOR);
    MarketCreateLayout.Layout layout = layout();
    UiScale scale = layout.scale();
    syncInputs(layout);
    graphics.pose().pushPose(); graphics.pose().scale(scale.value(), scale.value(), 1.0f);
    List<String> completion = suggestions();
    int completionSelection = controller.completionSelection();
    MarketCreateView.render(renderer, controller.state(), layout,
        scale.toVirtualX(mouseX), scale.toVirtualY(mouseY));
    MarketCreateView.renderInputFrames(renderer, controller.state(), layout,
        itemId != null && itemId.isFocused(), quantity != null && quantity.isFocused(),
        price != null && price.isFocused());
    graphics.pose().popPose();
    syncHints();
    super.render(graphics, mouseX, mouseY, partialTick);
    if (!completion.isEmpty()) {
      graphics.pose().pushPose();
      graphics.pose().translate(0, 0, 400);
      graphics.pose().scale(scale.value(), scale.value(), 1.0f);
      MarketCreateView.renderCompletionOverlay(renderer, controller.state(), layout,
          scale.toVirtualX(mouseX), scale.toVirtualY(mouseY), completion, completionSelection);
      graphics.pose().popPose();
    }
  }

  @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
    MarketCreateLayout.Layout layout = layout();
    int x = layout.scale().toVirtualX(mouseX), y = layout.scale().toVirtualY(mouseY);
    List<String> suggestions = suggestions();
    if (mode == MarketCreateMode.DEMAND && controller.completionOpen()
        && layout.completionDropdown().contains(x, y)) {
      int index = (y - layout.completionDropdown().y()) / MarketCreateLayout.COMPLETION_ROW_HEIGHT;
      int start = MarketCreateLayout.completionWindowStart(suggestions.size(), controller.completionSelection());
      if (index >= 0 && start + index < suggestions.size()
          && index < MarketCreateLayout.COMPLETION_MAX_ROWS) {
        controller.handle(new MarketCreateEvent.CompletionAccepted(start + index));
        itemId.setValue(controller.state().itemId());
        itemId.setFocused(true);
        controller.handle(new MarketCreateEvent.CompletionDismissed());
        return true;
      }
    }
    if (controller.completionOpen() && !layout.itemId().contains(x, y)) {
      controller.handle(new MarketCreateEvent.CompletionDismissed());
    }
    for (MarketCreateLayout.Slot slot : layout.slots()) if (slot.rect().contains(x, y)) {
      if (slot.hasItem()) controller.handle(new MarketCreateEvent.SlotSelected(slot.slot()));
      return true;
    }
    if (layout.decrement().contains(x, y)) { controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.DECREMENT)); syncInputs(layout); return true; }
    if (layout.increment().contains(x, y)) { controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.INCREMENT)); syncInputs(layout); return true; }
    if (layout.all().contains(x, y)) { controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.SELECT_ALL)); syncInputs(layout); return true; }
    if (layout.submit().contains(x, y)) { controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.SUBMIT)); return true; }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (mode == MarketCreateMode.DEMAND && controller.completionOpen()) {
      if (keyCode == 265) { controller.handle(new MarketCreateEvent.CompletionMoved(-1)); return true; }
      if (keyCode == 264) { controller.handle(new MarketCreateEvent.CompletionMoved(1)); return true; }
      if (keyCode == 258 || keyCode == 257) { acceptCompletion(); return true; }
      if (keyCode == 256) { controller.handle(new MarketCreateEvent.CompletionDismissed()); return true; }
    }
    if (keyCode == 256) { controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.BACK)); return true; }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }
  @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    if (mode == MarketCreateMode.DEMAND && controller.completionOpen()
        && itemId != null && itemId.isFocused() && delta != 0) {
      controller.handle(new MarketCreateEvent.CompletionMoved(delta > 0 ? -1 : 1));
      return true;
    }
    return super.mouseScrolled(mouseX, mouseY, delta);
  }
  @Override public void onClose() { controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.BACK)); }
  @Override public boolean isPauseScreen() { return false; }
  @Override public void renderBackground(GuiGraphics graphics) {}

  private MarketCreateLayout.Layout layout() { return MarketCreateLayout.calculate(width, height, controller == null ? null : controller.state(), new Forge1201UiTextMetrics(font)); }
  private List<String> suggestions() {
    if (mode != MarketCreateMode.DEMAND || itemId == null || !itemId.isFocused()
        || !controller.completionOpen()) return List.of();
    return controller.completionSuggestions();
  }
  private void acceptCompletion() {
    controller.handle(new MarketCreateEvent.CompletionAccepted(controller.completionSelection()));
    if (itemId != null) itemId.setValue(controller.state().itemId());
    controller.handle(new MarketCreateEvent.CompletionDismissed());
  }
  private void syncInputs(MarketCreateLayout.Layout layout) {
    UiScale scale = layout.scale();
    syncingInputs = true;
    try {
      if (itemId != null) { itemId.setX(sx(layout.itemId().x(), scale)); itemId.setY(sy(layout.itemId().y(), scale)); itemId.setWidth(sw(layout.itemId().width(), scale)); itemId.setHeight(sh(layout.itemId().height(), scale)); }
      if (quantity != null) {
        quantity.setX(sx(layout.quantity().x(), scale)); quantity.setY(sy(layout.quantity().y(), scale)); quantity.setWidth(sw(layout.quantity().width(), scale)); quantity.setHeight(sh(layout.quantity().height(), scale));
        syncValue(quantity, Integer.toString(controller.state().quantity()));
      }
      if (price != null) {
        price.setX(sx(layout.price().x(), scale)); price.setY(sy(layout.price().y(), scale)); price.setWidth(sw(layout.price().width(), scale)); price.setHeight(sh(layout.price().height(), scale));
        syncValue(price, controller.state().totalPrice() > 0 ? Integer.toString(controller.state().totalPrice()) : "");
      }
    } finally {
      syncingInputs = false;
    }
  }

  private void syncValue(EditBox box, String value) {
    if (!value.equals(box.getValue())) box.setValue(value);
  }

  private void syncHints() {
    syncHint(itemId, "screen.market.create.item_id");
    syncHint(quantity, "screen.market.create.quantity");
    syncHint(price, "screen.market.create.price");
  }

  private static void syncHint(EditBox box, String key) {
    if (box == null) return;
    box.setHint(box.getValue().isEmpty() && !box.isFocused()
        ? Component.translatable(key) : Component.empty());
  }

  private final class Port implements MarketCreatePort {
    private final Player player;
    private Port(Player player) { this.player = player; }
    @Override public List<String> itemIdSuggestions(String prefix) {
      String normalized = prefix == null ? "" : prefix.trim().toLowerCase(Locale.ROOT);
      if (normalized.isEmpty()) return List.of();
      return BuiltInRegistries.ITEM.keySet().stream().map(ResourceLocation::toString)
          .filter(id -> matches(id, normalized)).sorted().toList();
    }
    private boolean matches(String id, String prefix) {
      String path = id.substring(id.indexOf(':') + 1);
      return id.startsWith(prefix) || path.startsWith(prefix);
    }
    @Override public boolean isKnownItem(String id) {
      ResourceLocation location = ResourceLocation.tryParse(id);
      return location != null && BuiltInRegistries.ITEM.containsKey(location) && BuiltInRegistries.ITEM.get(location) != Items.AIR;
    }
    @Override public int maxStackSize(String id) {
      ResourceLocation location = ResourceLocation.tryParse(id);
      if (location == null || !BuiltInRegistries.ITEM.containsKey(location)) return 64;
      Item item = BuiltInRegistries.ITEM.get(location); return item == null ? 64 : item.getMaxStackSize();
    }
    @Override public void submitSales(int slot, int quantity, int unitPrice) {
      EconomyServices.platform().network().sendToServer(new CreateSalesOrderMessage(slot, quantity, unitPrice));
    }
    @Override public void submitDemand(String id, int quantity, int unitPrice) {
      EconomyServices.platform().network().sendToServer(new CreateDemandOrderMessage(id, quantity, unitPrice));
    }
  }
}

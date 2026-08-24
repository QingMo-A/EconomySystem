package com.mo.economy_system.target.neoforge1211.client;

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

/** NeoForge shell translating Minecraft widgets and inventory into the common market form. */
public final class NeoForge1211MarketCreateScreen extends Screen {
  private final Screen parent;
  private final MarketCreateMode mode;
  private final Port port;
  private final MarketCreateController controller;
  private EditBox itemId;
  private EditBox quantity;
  private EditBox price;
  private boolean syncingInputs;

  public NeoForge1211MarketCreateScreen(MarketCreateMode mode, Screen parent) {
    super(Component.translatable(mode == MarketCreateMode.SALES
        ? "screen.market.create.sales_title" : "screen.market.create.demand_title"));
    this.mode = mode; this.parent = parent;
    Player player = Minecraft.getInstance().player;
    this.port = new Port(); this.controller = new MarketCreateController(mode, inventory(player), port);
  }
  private List<MarketInventoryItem> inventory(Player player) {
    if (player == null) return List.of();
    List<MarketInventoryItem> result = new ArrayList<>();
    for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
      ItemStack stack = player.getInventory().items.get(slot);
      if (!stack.isEmpty()) result.add(new MarketInventoryItem(slot, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount(), stack.getMaxStackSize()));
    }
    return result;
  }
  @Override protected void init() {
    MarketCreateLayout.Layout layout = layout(); UiScale scale = layout.scale();
    if (mode == MarketCreateMode.DEMAND) {
      itemId = makeInput(layout.itemId(), "screen.market.create.item_id", controller.state().itemId(), scale);
      itemId.setMaxLength(256); itemId.setResponder(value -> { if (!syncingInputs) controller.handle(new MarketCreateEvent.ItemIdChanged(value)); }); addRenderableWidget(itemId);
    }
    quantity = makeInput(layout.quantity(), "screen.market.create.quantity", Integer.toString(controller.state().quantity()), scale);
    quantity.setResponder(value -> { if (!syncingInputs) controller.handle(new MarketCreateEvent.QuantityChanged(parse(value))); }); addRenderableWidget(quantity);
    price = makeInput(layout.price(), "screen.market.create.price", "", scale);
    price.setResponder(value -> { if (!syncingInputs) controller.handle(new MarketCreateEvent.PriceChanged(parse(value))); }); addRenderableWidget(price);
  }
  private EditBox makeInput(com.mo.economy_system.ui.geometry.UiRect rect, String hint, String value, UiScale scale) {
    EditBox box = new NeoForge1211UnderlinedEditBox(font, Math.round(rect.x() * scale.value()), Math.round(rect.y() * scale.value()), Math.max(1, Math.round(rect.width() * scale.value())), Math.max(1, Math.round(rect.height() * scale.value())), Component.translatable(hint));
    NeoForge1211UiInputAdapter.apply(box); box.setMaxLength(9); box.setHint(Component.translatable(hint)); box.setValue(value); return box;
  }
  private int parse(String value) { try { return Integer.parseInt(value.trim()); } catch (NumberFormatException ignored) { return 0; } }
  @Override public void tick() { super.tick(); if (controller.completionOpen() && (itemId == null || !itemId.isFocused())) controller.handle(new MarketCreateEvent.CompletionDismissed()); controller.pollNavigation().ifPresent(this::navigate); }
  private void navigate(UiNavigation navigation) { if (minecraft != null && navigation instanceof UiNavigation.Back) { Screen destination = parent == null ? new NeoForge1211MarketScreen() : parent; if (destination instanceof NeoForge1211MarketScreen market) market.refreshData(); minecraft.setScreen(destination); } }
  @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { NeoForge1211UiRenderer renderer = new NeoForge1211UiRenderer(graphics, font); renderer.fillPhysicalBackground(width, height, MarketCreateLayout.BACKGROUND_COLOR); MarketCreateLayout.Layout layout = layout(); UiScale scale = layout.scale(); syncInputs(layout); List<String> completion = suggestions(); int completionSelection = controller.completionSelection(); graphics.pose().pushPose(); graphics.pose().scale(scale.value(), scale.value(), 1.0f); MarketCreateView.render(renderer, controller.state(), layout, scale.toVirtualX(mouseX), scale.toVirtualY(mouseY)); MarketCreateView.renderInputFrames(renderer, controller.state(), layout, itemId != null && itemId.isFocused(), quantity != null && quantity.isFocused(), price != null && price.isFocused()); graphics.pose().popPose(); syncHints(); super.render(graphics, mouseX, mouseY, partialTick); if (!completion.isEmpty()) { graphics.pose().pushPose(); graphics.pose().translate(0, 0, 400); graphics.pose().scale(scale.value(), scale.value(), 1.0f); MarketCreateView.renderCompletionOverlay(renderer, controller.state(), layout, scale.toVirtualX(mouseX), scale.toVirtualY(mouseY), completion, completionSelection); graphics.pose().popPose(); } }
  @Override public boolean mouseClicked(double mouseX, double mouseY, int button) { MarketCreateLayout.Layout layout = layout(); int x = layout.scale().toVirtualX(mouseX), y = layout.scale().toVirtualY(mouseY); List<String> suggestions = suggestions(); if (mode == MarketCreateMode.DEMAND && controller.completionOpen() && layout.completionDropdown().contains(x, y)) { int index = (y - layout.completionDropdown().y()) / MarketCreateLayout.COMPLETION_ROW_HEIGHT; int start = MarketCreateLayout.completionWindowStart(suggestions.size(), controller.completionSelection()); if (index >= 0 && start + index < suggestions.size() && index < MarketCreateLayout.COMPLETION_MAX_ROWS) { controller.handle(new MarketCreateEvent.CompletionAccepted(start + index)); itemId.setValue(controller.state().itemId()); itemId.setFocused(true); controller.handle(new MarketCreateEvent.CompletionDismissed()); return true; } } if (controller.completionOpen() && !layout.itemId().contains(x, y)) controller.handle(new MarketCreateEvent.CompletionDismissed()); for (MarketCreateLayout.Slot slot : layout.slots()) if (slot.rect().contains(x, y)) { if (slot.hasItem()) controller.handle(new MarketCreateEvent.SlotSelected(slot.slot())); return true; } if (layout.decrement().contains(x,y)) { controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.DECREMENT)); syncInputs(layout); return true; } if (layout.increment().contains(x,y)) { controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.INCREMENT)); syncInputs(layout); return true; } if (layout.all().contains(x,y)) { controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.SELECT_ALL)); syncInputs(layout); return true; } if (layout.submit().contains(x,y)) { controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.SUBMIT)); return true; } return super.mouseClicked(mouseX, mouseY, button); }
  @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) { if (mode == MarketCreateMode.DEMAND && controller.completionOpen()) { if (keyCode == 265) { controller.handle(new MarketCreateEvent.CompletionMoved(-1)); return true; } if (keyCode == 264) { controller.handle(new MarketCreateEvent.CompletionMoved(1)); return true; } if (keyCode == 258 || keyCode == 257) { acceptCompletion(); return true; } if (keyCode == 256) { controller.handle(new MarketCreateEvent.CompletionDismissed()); return true; } } if (keyCode == 256) { controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.BACK)); return true; } return super.keyPressed(keyCode, scanCode, modifiers); }
  @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) { if (mode == MarketCreateMode.DEMAND && controller.completionOpen() && itemId != null && itemId.isFocused() && scrollY != 0) { controller.handle(new MarketCreateEvent.CompletionMoved(scrollY > 0 ? -1 : 1)); return true; } return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY); }
  @Override public void onClose() { controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.BACK)); }
  @Override public boolean isPauseScreen() { return false; }
  @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
  private MarketCreateLayout.Layout layout() { return MarketCreateLayout.calculate(width, height, controller.state(), new NeoForge1211UiTextMetrics(font)); }
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
  private void syncInputs(MarketCreateLayout.Layout layout) { UiScale scale = layout.scale(); syncingInputs = true; try { if (itemId != null) { itemId.setX(Math.round(layout.itemId().x() * scale.value())); itemId.setY(Math.round(layout.itemId().y() * scale.value())); itemId.setWidth(Math.max(1, Math.round(layout.itemId().width() * scale.value()))); itemId.setHeight(Math.max(1, Math.round(layout.itemId().height() * scale.value()))); } if (quantity != null) { quantity.setX(Math.round(layout.quantity().x() * scale.value())); quantity.setY(Math.round(layout.quantity().y() * scale.value())); quantity.setWidth(Math.max(1, Math.round(layout.quantity().width() * scale.value()))); quantity.setHeight(Math.max(1, Math.round(layout.quantity().height() * scale.value()))); syncValue(quantity, Integer.toString(controller.state().quantity())); } if (price != null) { price.setX(Math.round(layout.price().x() * scale.value())); price.setY(Math.round(layout.price().y() * scale.value())); price.setWidth(Math.max(1, Math.round(layout.price().width() * scale.value()))); price.setHeight(Math.max(1, Math.round(layout.price().height() * scale.value()))); syncValue(price, controller.state().totalPrice() > 0 ? Integer.toString(controller.state().totalPrice()) : ""); } } finally { syncingInputs = false; } }
  private void syncValue(EditBox box, String value) { if (!value.equals(box.getValue())) box.setValue(value); }
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
    @Override public boolean isKnownItem(String id) { ResourceLocation location = ResourceLocation.tryParse(id); return location != null && BuiltInRegistries.ITEM.containsKey(location) && BuiltInRegistries.ITEM.get(location) != Items.AIR; }
    @Override public int maxStackSize(String id) { ResourceLocation location = ResourceLocation.tryParse(id); if (location == null || !BuiltInRegistries.ITEM.containsKey(location)) return 64; Item item = BuiltInRegistries.ITEM.get(location); return item == null ? 64 : item.getDefaultInstance().getMaxStackSize(); }
    @Override public void submitSales(int slot, int quantity, int unitPrice) { EconomyServices.platform().network().sendToServer(new CreateSalesOrderMessage(slot, quantity, unitPrice)); }
    @Override public void submitDemand(String id, int quantity, int unitPrice) { EconomyServices.platform().network().sendToServer(new CreateDemandOrderMessage(id, quantity, unitPrice)); }
  }
}

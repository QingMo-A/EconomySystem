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
      itemId = new EditBox(font, sx(layout.itemId().x(), scale), sy(layout.itemId().y(), scale),
          sw(layout.itemId().width(), scale), sh(layout.itemId().height(), scale),
          Component.translatable("screen.market.create.item_id"));
      itemId.setMaxLength(256);
      itemId.setValue(controller.state().itemId());
      itemId.setResponder(value -> controller.handle(new MarketCreateEvent.ItemIdChanged(value)));
      addRenderableWidget(itemId);
    }
    quantity = input(layout.quantity(), "screen.market.create.quantity", Integer.toString(controller.state().quantity()));
    quantity.setResponder(value -> controller.handle(new MarketCreateEvent.QuantityChanged(parse(value))));
    addRenderableWidget(quantity);
    price = input(layout.price(), "screen.market.create.price", "");
    price.setResponder(value -> controller.handle(new MarketCreateEvent.PriceChanged(parse(value))));
    addRenderableWidget(price);
  }

  private EditBox input(com.mo.economy_system.ui.geometry.UiRect rect, String hint, String value) {
    UiScale scale = layout().scale();
    EditBox box = new EditBox(font, sx(rect.x(), scale), sy(rect.y(), scale), sw(rect.width(), scale), sh(rect.height(), scale), Component.translatable(hint));
    box.setMaxLength(9); box.setValue(value); return box;
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
    controller.pollNavigation().ifPresent(this::navigate);
  }

  private void navigate(UiNavigation navigation) {
    if (minecraft == null) return;
    if (navigation instanceof UiNavigation.Back) {
      minecraft.setScreen(parent == null ? new Forge1201MarketScreen() : parent);
    }
  }

  @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    MarketCreateLayout.Layout layout = layout();
    UiScale scale = layout.scale();
    graphics.pose().pushPose(); graphics.pose().scale(scale.value(), scale.value(), 1.0f);
    MarketCreateView.render(new Forge1201UiRenderer(graphics, font), controller.state(), layout,
        scale.toVirtualX(mouseX), scale.toVirtualY(mouseY));
    graphics.pose().popPose();
    super.render(graphics, mouseX, mouseY, partialTick);
  }

  @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
    MarketCreateLayout.Layout layout = layout();
    int x = layout.scale().toVirtualX(mouseX), y = layout.scale().toVirtualY(mouseY);
    for (MarketCreateLayout.Slot slot : layout.slots()) if (slot.rect().contains(x, y)) {
      controller.handle(new MarketCreateEvent.SlotSelected(slot.item().slot())); return true;
    }
    if (layout.decrement().contains(x, y)) { controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.DECREMENT)); return true; }
    if (layout.increment().contains(x, y)) { controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.INCREMENT)); return true; }
    if (layout.all().contains(x, y)) { controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.SELECT_ALL)); return true; }
    if (layout.submit().contains(x, y)) { controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.SUBMIT)); return true; }
    if (layout.back().contains(x, y)) { controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.BACK)); return true; }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == 256) { controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.BACK)); return true; }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }
  @Override public void onClose() { controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.BACK)); }
  @Override public boolean isPauseScreen() { return false; }

  private MarketCreateLayout.Layout layout() { return MarketCreateLayout.calculate(width, height, controller == null ? null : controller.state()); }

  private final class Port implements MarketCreatePort {
    private final Player player;
    private Port(Player player) { this.player = player; }
    @Override public boolean isKnownItem(String id) {
      ResourceLocation location = ResourceLocation.tryParse(id);
      return location != null && BuiltInRegistries.ITEM.containsKey(location) && BuiltInRegistries.ITEM.get(location) != Items.AIR;
    }
    @Override public int maxStackSize(String id) {
      ResourceLocation location = ResourceLocation.tryParse(id);
      if (location == null || !BuiltInRegistries.ITEM.containsKey(location)) return 64;
      Item item = BuiltInRegistries.ITEM.get(location); return item == null ? 64 : item.getMaxStackSize();
    }
    @Override public void submitSales(int slot, int quantity, int totalPrice) {
      EconomyServices.platform().network().sendToServer(new CreateSalesOrderMessage(slot, quantity, totalPrice));
    }
    @Override public void submitDemand(String id, int quantity, int totalPrice) {
      EconomyServices.platform().network().sendToServer(new CreateDemandOrderMessage(id, quantity, totalPrice));
    }
  }
}

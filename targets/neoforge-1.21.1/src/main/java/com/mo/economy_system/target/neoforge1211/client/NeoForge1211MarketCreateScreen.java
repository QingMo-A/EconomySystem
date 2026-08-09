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
      itemId.setMaxLength(256); itemId.setResponder(value -> controller.handle(new MarketCreateEvent.ItemIdChanged(value))); addRenderableWidget(itemId);
    }
    quantity = makeInput(layout.quantity(), "screen.market.create.quantity", Integer.toString(controller.state().quantity()), scale);
    quantity.setResponder(value -> controller.handle(new MarketCreateEvent.QuantityChanged(parse(value)))); addRenderableWidget(quantity);
    price = makeInput(layout.price(), "screen.market.create.price", "", scale);
    price.setResponder(value -> controller.handle(new MarketCreateEvent.PriceChanged(parse(value)))); addRenderableWidget(price);
  }
  private EditBox makeInput(com.mo.economy_system.ui.geometry.UiRect rect, String hint, String value, UiScale scale) {
    EditBox box = new EditBox(font, Math.round(rect.x() * scale.value()), Math.round(rect.y() * scale.value()), Math.max(1, Math.round(rect.width() * scale.value())), Math.max(1, Math.round(rect.height() * scale.value())), Component.translatable(hint));
    box.setMaxLength(9); box.setValue(value); return box;
  }
  private int parse(String value) { try { return Integer.parseInt(value.trim()); } catch (NumberFormatException ignored) { return 0; } }
  @Override public void tick() { super.tick(); controller.pollNavigation().ifPresent(this::navigate); }
  private void navigate(UiNavigation navigation) { if (minecraft != null && navigation instanceof UiNavigation.Back) minecraft.setScreen(parent == null ? new NeoForge1211MarketScreen() : parent); }
  @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { MarketCreateLayout.Layout layout = layout(); UiScale scale = layout.scale(); graphics.pose().pushPose(); graphics.pose().scale(scale.value(), scale.value(), 1.0f); MarketCreateView.render(new NeoForge1211UiRenderer(graphics, font), controller.state(), layout, scale.toVirtualX(mouseX), scale.toVirtualY(mouseY)); graphics.pose().popPose(); super.render(graphics, mouseX, mouseY, partialTick); }
  @Override public boolean mouseClicked(double mouseX, double mouseY, int button) { MarketCreateLayout.Layout layout = layout(); int x = layout.scale().toVirtualX(mouseX), y = layout.scale().toVirtualY(mouseY); for (MarketCreateLayout.Slot slot : layout.slots()) if (slot.rect().contains(x, y)) { controller.handle(new MarketCreateEvent.SlotSelected(slot.item().slot())); return true; } if (layout.decrement().contains(x,y)) { controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.DECREMENT)); return true; } if (layout.increment().contains(x,y)) { controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.INCREMENT)); return true; } if (layout.all().contains(x,y)) { controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.SELECT_ALL)); return true; } if (layout.submit().contains(x,y)) { controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.SUBMIT)); return true; } if (layout.back().contains(x,y)) { controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.BACK)); return true; } return super.mouseClicked(mouseX, mouseY, button); }
  @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) { if (keyCode == 256) { controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.BACK)); return true; } return super.keyPressed(keyCode, scanCode, modifiers); }
  @Override public void onClose() { controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.BACK)); }
  @Override public boolean isPauseScreen() { return false; }
  @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
  private MarketCreateLayout.Layout layout() { return MarketCreateLayout.calculate(width, height, controller.state()); }
  private final class Port implements MarketCreatePort {
    @Override public boolean isKnownItem(String id) { ResourceLocation location = ResourceLocation.tryParse(id); return location != null && BuiltInRegistries.ITEM.containsKey(location) && BuiltInRegistries.ITEM.get(location) != Items.AIR; }
    @Override public int maxStackSize(String id) { ResourceLocation location = ResourceLocation.tryParse(id); if (location == null || !BuiltInRegistries.ITEM.containsKey(location)) return 64; Item item = BuiltInRegistries.ITEM.get(location); return item == null ? 64 : item.getDefaultInstance().getMaxStackSize(); }
    @Override public void submitSales(int slot, int quantity, int totalPrice) { EconomyServices.platform().network().sendToServer(new CreateSalesOrderMessage(slot, quantity, totalPrice)); }
    @Override public void submitDemand(String id, int quantity, int totalPrice) { EconomyServices.platform().network().sendToServer(new CreateDemandOrderMessage(id, quantity, totalPrice)); }
  }
}

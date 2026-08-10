package com.mo.economy_system.target.forge1201.client;

import com.mo.economy_system.common.network.ConfirmDemandOrderMessage;
import com.mo.economy_system.common.network.DeliverDemandOrderMessage;
import com.mo.economy_system.common.network.PurchaseSalesOrderMessage;
import com.mo.economy_system.common.network.RemoveDemandOrderMessage;
import com.mo.economy_system.common.network.RemoveSalesOrderMessage;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.ui.core.UiNavigation;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.market.MarketAction;
import com.mo.economy_system.ui.market.MarketConfirmAction;
import com.mo.economy_system.ui.market.MarketConfirmController;
import com.mo.economy_system.ui.market.MarketConfirmEvent;
import com.mo.economy_system.ui.market.MarketConfirmLayout;
import com.mo.economy_system.ui.market.MarketConfirmPort;
import com.mo.economy_system.ui.market.MarketConfirmView;
import com.mo.economy_system.ui.market.MarketRow;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Forge shell for the common market confirmation state. */
public final class Forge1201MarketConfirmScreen extends Screen {
  private final Screen parent;
  private final Port port;
  private final MarketConfirmController controller;

  public Forge1201MarketConfirmScreen(MarketAction action, MarketRow row, Screen parent) {
    super(Component.translatable("screen.market.confirm.title"));
    this.parent = parent; this.port = new Port(); this.controller = new MarketConfirmController(action, row, port);
  }
  @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { Forge1201UiRenderer renderer = new Forge1201UiRenderer(graphics, font); renderer.fillPhysicalBackground(width, height, MarketConfirmLayout.BACKGROUND_COLOR); MarketConfirmLayout.Layout layout = MarketConfirmLayout.calculate(width, height, controller.state()); UiScale scale = layout.scale(); graphics.pose().pushPose(); graphics.pose().scale(scale.value(), scale.value(), 1.0f); MarketConfirmView.render(renderer, controller.state(), layout, scale.toVirtualX(mouseX), scale.toVirtualY(mouseY)); graphics.pose().popPose(); super.render(graphics, mouseX, mouseY, partialTick); }
  @Override public boolean mouseClicked(double mouseX, double mouseY, int button) { MarketConfirmLayout.Layout layout = MarketConfirmLayout.calculate(width, height, controller.state()); int x = layout.scale().toVirtualX(mouseX), y = layout.scale().toVirtualY(mouseY); if (layout.confirm().contains(x,y)) { controller.handle(new MarketConfirmEvent.ActionClicked(MarketConfirmAction.CONFIRM)); return true; } if (layout.cancel().contains(x,y)) { controller.handle(new MarketConfirmEvent.ActionClicked(MarketConfirmAction.CANCEL)); return true; } return super.mouseClicked(mouseX, mouseY, button); }
  @Override public void tick() { super.tick(); controller.pollNavigation().ifPresent(this::navigate); }
  private void navigate(UiNavigation navigation) { if (minecraft != null && navigation instanceof UiNavigation.Back) minecraft.setScreen(parent); }
  @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) { if (keyCode == 256) { controller.handle(new MarketConfirmEvent.ActionClicked(MarketConfirmAction.CANCEL)); return true; } return super.keyPressed(keyCode, scanCode, modifiers); }
  @Override public void onClose() { controller.handle(new MarketConfirmEvent.ActionClicked(MarketConfirmAction.CANCEL)); }
  @Override public boolean isPauseScreen() { return false; }
  private final class Port implements MarketConfirmPort {
    @Override public void submit(MarketAction action, MarketRow row) { UUID id = row.order().tradeId(); switch (action) { case BUY -> EconomyServices.platform().network().sendToServer(new PurchaseSalesOrderMessage(id)); case REMOVE_SALES, ADMIN_REMOVE_SALES -> EconomyServices.platform().network().sendToServer(new RemoveSalesOrderMessage(id)); case DELIVER_DEMAND -> EconomyServices.platform().network().sendToServer(new DeliverDemandOrderMessage(id)); case CONFIRM_DEMAND -> EconomyServices.platform().network().sendToServer(new ConfirmDemandOrderMessage(id)); case REMOVE_DEMAND -> EconomyServices.platform().network().sendToServer(new RemoveDemandOrderMessage(id)); default -> {} } }
  }
}

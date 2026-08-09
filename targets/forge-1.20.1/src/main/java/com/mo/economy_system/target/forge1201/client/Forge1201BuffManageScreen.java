package com.mo.economy_system.target.forge1201.client;

import com.mo.economy_system.common.network.SingleTerritoryDataResponseKind;
import com.mo.economy_system.common.network.SingleTerritoryDataRequestMessage;
import com.mo.economy_system.common.network.UnlockTerritoryBuffMessage;
import com.mo.economy_system.common.network.UpgradeTerritoryBuffMessage;
import com.mo.economy_system.common.territory.TerritoryBuffCost;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.target.forge1201.network.Forge1201SingleTerritoryClientState;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.territory.buff.BuffAction;
import com.mo.economy_system.ui.territory.buff.BuffManageController;
import com.mo.economy_system.ui.territory.buff.BuffManageEvent;
import com.mo.economy_system.ui.territory.buff.BuffManageLayout;
import com.mo.economy_system.ui.territory.buff.BuffManagePort;
import com.mo.economy_system.ui.territory.buff.BuffManageView;
import com.mo.economy_system.ui.territory.buff.BuffResourceSnapshot;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;

/** Forge shell for the loader-neutral territory Buff page. */
public final class Forge1201BuffManageScreen extends Screen {
  private static final AtomicLong IDS = new AtomicLong(1);
  private final Screen parent;
  private final Port port = new Port();
  private final BuffManageController controller;
  private EditBox search;
  private long appliedResponse = -1;

  public Forge1201BuffManageScreen(Owned initial, Screen parent) {
    super(Component.translatable("screen.territory.buff"));
    this.parent = parent;
    this.controller = new BuffManageController(initial.summary().territoryId(), initial.summary().name(),
        initial.buffs(), port);
  }

  @Override protected void init() {
    String value = search == null ? "" : search.getValue();
    var rect = commonLayout().search();
    float scale = commonLayout().scale().value();
    search = new EditBox(font, Math.round(rect.x() * scale), Math.round(rect.y() * scale),
        Math.max(1, Math.round(rect.width() * scale)), Math.max(1, Math.round(rect.height() * scale)),
        Component.translatable("screen.territory.search"));
    search.setMaxLength(50);
    search.setHint(Component.translatable("screen.territory.buff.search"));
    search.setValue(value);
    search.setResponder(text -> controller.handle(new BuffManageEvent.FilterChanged(text)));
    addRenderableWidget(search);
    if (controller.state().screenState() == ScreenState.IDLE) {
      controller.handle(new BuffManageEvent.Initialize(System.nanoTime()));
    }
  }

  @Override public void tick() {
    super.tick();
    controller.handle(new BuffManageEvent.Tick(System.nanoTime()));
    var snapshot = Forge1201SingleTerritoryClientState.snapshot();
    if (snapshot.requestId() == port.requestId && snapshot.requestId() != appliedResponse) {
      appliedResponse = snapshot.requestId();
      if (snapshot.kind() == SingleTerritoryDataResponseKind.DATA && snapshot.territory() != null) {
        controller.handle(new BuffManageEvent.DataLoaded(
            snapshot.requestId(), snapshot.territory().buffs()));
      } else {
        controller.handle(new BuffManageEvent.DataFailed(snapshot.requestId(),
            "screen.territory.buff.sync_failed"));
      }
    }
    controller.pollNavigation().ifPresent(this::navigate);
  }

  private void navigate(UiNavigation navigation) { if (minecraft != null && navigation instanceof UiNavigation.Back) minecraft.setScreen(parent); }

  @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    var layout = commonLayout(); UiScale scale = layout.scale();
    graphics.pose().pushPose(); graphics.pose().scale(scale.value(), scale.value(), 1.0f);
    Forge1201UiRenderer renderer = new Forge1201UiRenderer(graphics, font);
    BuffManageView.render(renderer, controller.state(), layout,
        scale.toVirtualX(mouseX), scale.toVirtualY(mouseY));
    graphics.pose().popPose();
    super.render(graphics, mouseX, mouseY, partialTick);
    BuffManageView.tooltipAt(controller.state(), layout,
            scale.toVirtualX(mouseX), scale.toVirtualY(mouseY))
        .ifPresent(tooltip -> renderer.tooltip(tooltip, mouseX, mouseY));
  }

  @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
    var layout = commonLayout(); int x = layout.scale().toVirtualX(mouseX), y = layout.scale().toVirtualY(mouseY);
    for (var card : layout.cards()) if (card.actionButton().contains(x, y)) {
      controller.handle(new BuffManageEvent.ActionClicked(card.buff().action(), card.buff().buff().id(), System.nanoTime())); return true;
    }
    if (controller.state().screenState() == ScreenState.ERROR
        && layout.retryButton().contains(x, y)) {
      controller.handle(new BuffManageEvent.Retry(System.nanoTime()));
    } else if (layout.previousButton().contains(x, y)) controller.handle(new BuffManageEvent.PreviousPage());
    else if (layout.nextButton().contains(x, y)) controller.handle(new BuffManageEvent.NextPage());
    else return super.mouseClicked(mouseX, mouseY, button);
    return true;
  }

  @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
    if (scrollY != 0 && controller.state().totalPages() > 1) { controller.handle(new BuffManageEvent.Scroll(scrollY < 0 ? 1 : -1)); return true; }
    return super.mouseScrolled(mouseX, mouseY, scrollY);
  }
  @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) { if (keyCode == 256) { onClose(); return true; } return super.keyPressed(keyCode, scanCode, modifiers); }
  @Override public void onClose() {
    controller.handle(new BuffManageEvent.ActionClicked(BuffAction.BACK, "", System.nanoTime()));
    controller.pollNavigation().ifPresent(this::navigate);
  }
  @Override public boolean isPauseScreen() { return false; }
  private BuffManageLayout.Layout commonLayout() {
    var layout = BuffManageLayout.calculate(width, height, controller.state());
    if (layout.pageSize() != controller.state().pageSize()) {
      controller.handle(new BuffManageEvent.ViewportChanged(layout.pageSize()));
      layout = BuffManageLayout.calculate(width, height, controller.state());
    }
    return layout;
  }

  private final class Port implements BuffManagePort {
    private long requestId = -1;
    @Override public long nextRequestId() { long value = IDS.getAndIncrement(); if (value < 0) throw new IllegalStateException("buff request id exhausted"); return value; }
    @Override public void request(UUID territoryId, long id) { requestId = id; EconomyServices.platform().network().sendToServer(new SingleTerritoryDataRequestMessage(territoryId, id)); }
    @Override public void submit(UUID territoryId, BuffAction action, String buffId) {
      if (action == BuffAction.UNLOCK) EconomyServices.platform().network().sendToServer(new UnlockTerritoryBuffMessage(territoryId, buffId));
      else if (action == BuffAction.UPGRADE) EconomyServices.platform().network().sendToServer(new UpgradeTerritoryBuffMessage(territoryId, buffId));
    }
    @Override public BuffResourceSnapshot inspect(TerritoryBuffCost cost) {
      var player = Minecraft.getInstance().player;
      if (player == null) return BuffResourceSnapshot.unknown();
      LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
      for (String itemId : cost.items().keySet()) counts.put(itemId, 0);
      for (var stack : player.getInventory().items) {
        if (stack == null || stack.isEmpty()) continue;
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key != null && counts.containsKey(key.toString())) {
          counts.merge(key.toString(), stack.getCount(), Integer::sum);
        }
      }
      return new BuffResourceSnapshot(counts, player.experienceLevel, true);
    }
    @Override public void feedback(String translationKey) {
      var player = Minecraft.getInstance().player;
      if (player != null && translationKey != null && !translationKey.isBlank()) {
        player.displayClientMessage(Component.translatable(translationKey), false);
      }
    }
  }
}

package com.mo.economy_system.target.forge1201.client;

import com.mo.economy_system.common.network.ModifyTerritoryModeMessage;
import com.mo.economy_system.common.network.RemoveTerritoryMemberMessage;
import com.mo.economy_system.common.network.SingleTerritoryDataRequestMessage;
import com.mo.economy_system.common.network.SingleTerritoryDataResponseKind;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.target.forge1201.network.Forge1201SingleTerritoryClientState;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.territory.MemberRow;
import com.mo.economy_system.ui.territory.TerritoryManageAction;
import com.mo.economy_system.ui.territory.TerritoryManageController;
import com.mo.economy_system.ui.territory.TerritoryManageEvent;
import com.mo.economy_system.ui.territory.TerritoryManageLayout;
import com.mo.economy_system.ui.territory.TerritoryManagePort;
import com.mo.economy_system.ui.territory.TerritoryManageView;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Thin Forge 1.20.1 Screen shell for the common territory-management view. */
public final class Forge1201TerritoryManageScreen extends Screen {
  private static final AtomicLong IDS = new AtomicLong(1);
  private final Owned initial;
  private final Screen parent;
  private final Port port = new Port();
  private final TerritoryManageController controller;
  private EditBox search;
  private long appliedResponse = -1;

  public Forge1201TerritoryManageScreen(Owned initial, Screen parent) {
    super(Component.translatable("screen.territory.manage"));
    this.initial = initial;
    this.parent = parent;
    var summary = initial.summary();
    controller = new TerritoryManageController(summary.territoryId(), summary.name(),
        summary.ownerId(), summary.ownerName(), port);
  }

  @Override protected void init() {
    String value = search == null ? "" : search.getValue();
    var layout = commonLayout();
    var searchRect = layout.search();
    float scale = layout.scale().value();
    search = new EditBox(font, Math.round(searchRect.x() * scale), Math.round(searchRect.y() * scale),
        Math.max(1, Math.round(searchRect.width() * scale)),
        Math.max(1, Math.round(searchRect.height() * scale)),
        Component.translatable("screen.territory.search"));
    search.setMaxLength(50);
    search.setValue(value);
    search.setResponder(text -> controller.handle(new TerritoryManageEvent.FilterChanged(text)));
    addRenderableWidget(search);
    if (controller.state().screenState() == ScreenState.IDLE) {
      controller.handle(new TerritoryManageEvent.Initialize(System.nanoTime()));
    }
  }

  @Override public void tick() {
    super.tick();
    controller.handle(new TerritoryManageEvent.Tick(System.nanoTime()));
    Forge1201SingleTerritoryClientState.Snapshot snapshot =
        Forge1201SingleTerritoryClientState.snapshot();
    if (snapshot.requestId() == port.requestId && snapshot.requestId() != appliedResponse) {
      appliedResponse = snapshot.requestId();
      if (snapshot.kind() == SingleTerritoryDataResponseKind.DATA && snapshot.territory() != null) {
        controller.handle(new TerritoryManageEvent.DataLoaded(snapshot.requestId(),
            snapshot.territory().authorizedMembers().stream()
                .map(member -> new MemberRow(member.playerId(), member.playerName())).toList()));
      } else {
        controller.handle(new TerritoryManageEvent.DataFailed(snapshot.requestId(),
            "screen.territory.sync_failed"));
      }
    }
    controller.pollNavigation().ifPresent(this::navigate);
  }

  private void navigate(UiNavigation navigation) {
    if (minecraft != null && navigation instanceof UiNavigation.Back) minecraft.setScreen(parent);
  }

  @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    var layout = commonLayout();
    UiScale scale = layout.scale();
    graphics.pose().pushPose();
    graphics.pose().scale(scale.value(), scale.value(), 1.0f);
    TerritoryManageView.render(new Forge1201UiRenderer(graphics, font), controller.state(), layout,
        scale.toVirtualX(mouseX), scale.toVirtualY(mouseY));
    graphics.pose().popPose();
    super.render(graphics, mouseX, mouseY, partialTick);
  }

  @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
    var layout = commonLayout();
    int x = layout.scale().toVirtualX(mouseX);
    int y = layout.scale().toVirtualY(mouseY);
    for (var action : layout.actionButtons()) if (action.rect().contains(x, y)) {
      controller.handle(new TerritoryManageEvent.ActionClicked(action.action(), null));
      return true;
    }
    for (var card : layout.cards()) if (card.kickButton().contains(x, y)) {
      controller.handle(new TerritoryManageEvent.ActionClicked(TerritoryManageAction.KICK,
          card.member().playerId()));
      return true;
    }
    if (controller.state().screenState() == ScreenState.ERROR && layout.retryButton().contains(x, y)) {
      controller.handle(new TerritoryManageEvent.Retry(System.nanoTime()));
    } else if (layout.previousButton().contains(x, y)) controller.handle(new TerritoryManageEvent.PreviousPage());
    else if (layout.nextButton().contains(x, y)) controller.handle(new TerritoryManageEvent.NextPage());
    else if (layout.backButton().contains(x, y)) controller.handle(
        new TerritoryManageEvent.ActionClicked(TerritoryManageAction.BACK, null));
    else return super.mouseClicked(mouseX, mouseY, button);
    return true;
  }

  @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    if (delta != 0 && controller.state().totalPages() > 1) {
      controller.handle(new TerritoryManageEvent.Scroll(delta < 0 ? 1 : -1));
      return true;
    }
    return super.mouseScrolled(mouseX, mouseY, delta);
  }

  @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == 256) { onClose(); return true; }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
  @Override public boolean isPauseScreen() { return false; }

  private TerritoryManageLayout.Layout commonLayout() {
    var layout = TerritoryManageLayout.calculate(width, height, controller.state());
    if (layout.pageSize() != controller.state().pageSize()) {
      controller.handle(new TerritoryManageEvent.ViewportChanged(layout.pageSize()));
      layout = TerritoryManageLayout.calculate(width, height, controller.state());
    }
    return layout;
  }

  private final class Port implements TerritoryManagePort {
    private long requestId = -1;
    @Override public long nextRequestId() {
      long value = IDS.getAndIncrement();
      if (value < 0) throw new IllegalStateException("territory UI request id exhausted");
      return value;
    }
    @Override public void requestMembers(UUID territoryId, long id) {
      requestId = id;
      EconomyServices.platform().network().sendToServer(new SingleTerritoryDataRequestMessage(territoryId, id));
    }
    @Override public void submit(UUID territoryId, TerritoryManageAction action, UUID targetPlayerId) {
      if (action == TerritoryManageAction.KICK && targetPlayerId != null) {
        EconomyServices.platform().network().sendToServer(new RemoveTerritoryMemberMessage(territoryId, targetPlayerId));
      } else if (action == TerritoryManageAction.MODIFY_MODE) {
        EconomyServices.platform().network().sendToServer(new ModifyTerritoryModeMessage(territoryId));
        if (minecraft != null) minecraft.setScreen(null);
      }
    }
    @Override public void open(UUID territoryId, TerritoryManageAction action) {
      if (minecraft == null) return;
      if (action == TerritoryManageAction.COPY_ID) {
        minecraft.keyboardHandler.setClipboard(territoryId.toString());
      } else {
        minecraft.setScreen(
            new com.mo.economy_system.screen.territory_system.Screen_ManageTerritory(initial, parent));
      }
    }
  }
}

package com.mo.economy_system.target.forge1201.client;

import com.mo.economy_system.common.client.ClientPlayerListState;
import com.mo.economy_system.common.network.ModifyTerritoryModeMessage;
import com.mo.economy_system.common.network.ServerPlayerListRequestMessage;
import com.mo.economy_system.common.network.SingleTerritoryDataRequestMessage;
import com.mo.economy_system.common.network.SingleTerritoryDataResponseKind;
import com.mo.economy_system.common.network.TransferTerritoryOwnershipMessage;
import com.mo.economy_system.common.network.UpdateTerritoryPermissionMessage;
import com.mo.economy_system.common.network.UpdateTerritoryRuleMessage;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.target.forge1201.network.Forge1201SingleTerritoryClientState;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.territory.detail.TerritoryDetailAction;
import com.mo.economy_system.ui.territory.detail.TerritoryDetailController;
import com.mo.economy_system.ui.territory.detail.TerritoryDetailEvent;
import com.mo.economy_system.ui.territory.detail.TerritoryDetailLayout;
import com.mo.economy_system.ui.territory.detail.TerritoryDetailPort;
import com.mo.economy_system.ui.territory.detail.TerritoryDetailView;
import com.mo.economy_system.ui.territory.detail.TerritoryDetailViewKind;
import java.util.concurrent.atomic.AtomicLong;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Forge 1.20.1 shell for the common territory detail family. */
public final class Forge1201TerritoryDetailScreen extends Screen {
  private static final AtomicLong IDS = new AtomicLong(1);

  private final Screen parent;
  private final Port port = new Port();
  private final TerritoryDetailController controller;
  private EditBox search;
  private long appliedTerritoryResponse = -1;
  private long appliedPlayerRevision;

  public Forge1201TerritoryDetailScreen(Owned initial, Screen parent) {
    super(Component.translatable("screen.territory.detail.title"));
    this.parent = parent;
    ClientPlayerListState.Snapshot players = ClientPlayerListState.snapshot();
    this.appliedPlayerRevision = players.revision();
    this.controller = new TerritoryDetailController(initial, players.players(), players.revision(), port);
  }

  public void selectInitialView(TerritoryDetailViewKind view) {
    controller.selectInitialView(view);
  }

  @Override protected void init() {
    String value = search == null ? "" : search.getValue();
    TerritoryDetailLayout.Layout layout = commonLayout();
    var rect = layout.search();
    float scale = layout.scale().value();
    search = new EditBox(font, Math.round(rect.x() * scale), Math.round(rect.y() * scale),
        Math.max(1, Math.round(rect.width() * scale)), Math.max(1, Math.round(rect.height() * scale)),
        Component.translatable("screen.territory.detail.search"));
    search.setMaxLength(64);
    search.setHint(Component.translatable("screen.territory.detail.search"));
    search.setValue(value);
    search.setResponder(text -> controller.handle(new TerritoryDetailEvent.FilterChanged(text)));
    search.visible = controller.state().view() != com.mo.economy_system.ui.territory.detail.TerritoryDetailViewKind.MAIN;
    addRenderableWidget(search);
    if (controller.state().screenState() == ScreenState.IDLE) {
      controller.handle(new TerritoryDetailEvent.Initialize(System.nanoTime()));
    }
  }

  @Override public void tick() {
    super.tick();
    controller.handle(new TerritoryDetailEvent.Tick(System.nanoTime()));
    Forge1201SingleTerritoryClientState.Snapshot snapshot =
        Forge1201SingleTerritoryClientState.snapshot();
    if (snapshot.requestId() == port.requestId && snapshot.requestId() != appliedTerritoryResponse) {
      appliedTerritoryResponse = snapshot.requestId();
      if (snapshot.kind() == SingleTerritoryDataResponseKind.DATA && snapshot.territory() != null) {
        controller.handle(new TerritoryDetailEvent.TerritoryLoaded(snapshot.requestId(), snapshot.territory()));
      } else {
        controller.handle(new TerritoryDetailEvent.TerritoryFailed(snapshot.requestId(),
            "screen.territory.detail.sync_failed"));
      }
    }
    ClientPlayerListState.Snapshot players = ClientPlayerListState.snapshot();
    if (players.revision() != appliedPlayerRevision) {
      appliedPlayerRevision = players.revision();
      controller.handle(new TerritoryDetailEvent.PlayersLoaded(players.revision(), players.players()));
    }
    if (search != null) search.visible = controller.state().view()
        != com.mo.economy_system.ui.territory.detail.TerritoryDetailViewKind.MAIN;
    controller.pollNavigation().ifPresent(this::navigate);
  }

  private void navigate(UiNavigation navigation) {
    if (minecraft == null) return;
    if (navigation instanceof UiNavigation.Back) {
      minecraft.setScreen(parent);
    } else if (navigation instanceof UiNavigation.Target target
        && target.targetId().equals("territory-buffs")) {
      minecraft.setScreen(new Forge1201BuffManageScreen(controller.state().territory(), this));
    }
  }

  @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    TerritoryDetailLayout.Layout layout = commonLayout();
    UiScale scale = layout.scale();
    if (search != null) search.visible = controller.state().view()
        != com.mo.economy_system.ui.territory.detail.TerritoryDetailViewKind.MAIN;
    graphics.pose().pushPose();
    graphics.pose().scale(scale.value(), scale.value(), 1.0f);
    TerritoryDetailView.render(new Forge1201UiRenderer(graphics, font), controller.state(), layout,
        scale.toVirtualX(mouseX), scale.toVirtualY(mouseY));
    graphics.pose().popPose();
    super.render(graphics, mouseX, mouseY, partialTick);
  }

  @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
    TerritoryDetailLayout.Layout layout = commonLayout();
    int x = layout.scale().toVirtualX(mouseX);
    int y = layout.scale().toVirtualY(mouseY);
    if (controller.state().view() == com.mo.economy_system.ui.territory.detail.TerritoryDetailViewKind.MAIN) {
      for (var action : layout.mainActions()) if (action.rect().contains(x, y)) {
        controller.handle(new TerritoryDetailEvent.ActionClicked(action.action(), null));
        return true;
      }
    } else {
      for (var row : layout.accessCards()) if (row.actionButton().contains(x, y)) {
        controller.handle(new TerritoryDetailEvent.ActionClicked(TerritoryDetailAction.TOGGLE_ACCESS,
            row.row().playerId(), System.nanoTime()));
        return true;
      }
      for (var row : layout.ruleCards()) if (row.actionButton().contains(x, y)) {
        controller.handle(new TerritoryDetailEvent.RuleClicked(row.row().action(), System.nanoTime()));
        return true;
      }
      for (var row : layout.transferCards()) if (row.actionButton().contains(x, y)) {
        controller.handle(new TerritoryDetailEvent.ActionClicked(TerritoryDetailAction.TRANSFER_OWNERSHIP,
            row.player().playerId()));
        return true;
      }
      if (controller.state().screenState() == ScreenState.ERROR && layout.retryButton().contains(x, y)) {
        controller.handle(new TerritoryDetailEvent.Retry(System.nanoTime()));
        return true;
      }
      if (layout.previousButton().contains(x, y)) {
        controller.handle(new TerritoryDetailEvent.Scroll(-1)); return true;
      }
      if (layout.nextButton().contains(x, y)) {
        controller.handle(new TerritoryDetailEvent.Scroll(1)); return true;
      }
    }
    if (layout.backButton().contains(x, y)) {
      controller.handle(new TerritoryDetailEvent.ActionClicked(TerritoryDetailAction.BACK, null));
      return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    if (delta != 0 && controller.state().view()
        != com.mo.economy_system.ui.territory.detail.TerritoryDetailViewKind.MAIN
        && controller.state().totalPages() > 1) {
      controller.handle(new TerritoryDetailEvent.Scroll(delta < 0 ? 1 : -1));
      return true;
    }
    return super.mouseScrolled(mouseX, mouseY, delta);
  }

  @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == 256) { onClose(); return true; }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override public void onClose() {
    controller.handle(new TerritoryDetailEvent.ActionClicked(TerritoryDetailAction.BACK, null));
    controller.pollNavigation().ifPresent(this::navigate);
  }

  @Override public boolean isPauseScreen() { return false; }

  private TerritoryDetailLayout.Layout commonLayout() {
    TerritoryDetailLayout.Layout layout = TerritoryDetailLayout.calculate(width, height, controller.state());
    if (layout.pageSize() != controller.state().pageSize()) {
      controller.handle(new TerritoryDetailEvent.ViewportChanged(layout.pageSize()));
      layout = TerritoryDetailLayout.calculate(width, height, controller.state());
    }
    return layout;
  }

  private final class Port implements TerritoryDetailPort {
    private long requestId = -1;

    @Override public long nextRequestId() {
      long value = IDS.getAndIncrement();
      if (value < 0) throw new IllegalStateException("territory detail request id exhausted");
      return value;
    }

    @Override public void requestTerritory(UUID territoryId, long id) {
      requestId = id;
      EconomyServices.platform().network().sendToServer(new SingleTerritoryDataRequestMessage(territoryId, id));
    }

    @Override public void requestPlayers() {
      EconomyServices.platform().network().sendToServer(ServerPlayerListRequestMessage.INSTANCE);
    }

    @Override public void resize(UUID territoryId) {
      EconomyServices.platform().network().sendToServer(new ModifyTerritoryModeMessage(territoryId));
      if (minecraft != null) minecraft.setScreen(null);
    }

    @Override public void submitAccess(UUID territoryId, UUID playerId, boolean allowed) {
      EconomyServices.platform().network().sendToServer(
          new UpdateTerritoryPermissionMessage(territoryId, playerId, allowed));
    }

    @Override public void submitRule(UUID territoryId, RuleAction action, RuleLevel level) {
      EconomyServices.platform().network().sendToServer(new UpdateTerritoryRuleMessage(territoryId, action, level));
    }

    @Override public void submitTransfer(UUID territoryId, UUID playerId) {
      EconomyServices.platform().network().sendToServer(new TransferTerritoryOwnershipMessage(territoryId, playerId));
    }
  }
}

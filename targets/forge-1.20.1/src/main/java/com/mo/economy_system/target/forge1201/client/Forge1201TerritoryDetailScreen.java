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
import com.mo.economy_system.ui.territory.confirm.TerritoryConfirmationKind;
import com.mo.economy_system.ui.territory.detail.TerritoryDetailAction;
import com.mo.economy_system.ui.territory.detail.TerritoryDetailController;
import com.mo.economy_system.ui.territory.detail.TerritoryDetailEvent;
import com.mo.economy_system.ui.territory.detail.TerritoryDetailLayout;
import com.mo.economy_system.ui.territory.detail.TerritoryDetailPort;
import com.mo.economy_system.ui.territory.detail.TerritoryDetailView;
import com.mo.economy_system.ui.territory.detail.TerritoryDetailViewKind;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Forge 1.20.1 shell for the unified common territory management center. */
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
    search = new Forge1201UnderlinedEditBox(font, Math.round(rect.x() * scale), Math.round(rect.y() * scale),
        Math.max(1, Math.round(rect.width() * scale)), Math.max(1, Math.round(rect.height() * scale)),
        Component.translatable("screen.territory.detail.search"));
    Forge1201UiInputAdapter.apply(search);
    search.setMaxLength(64);
    search.setHint(Component.translatable("screen.territory.detail.search"));
    search.setValue(value);
    search.setResponder(text -> controller.handle(new TerritoryDetailEvent.FilterChanged(text)));
    search.visible = controller.state().searchVisible();
    addRenderableWidget(search);
    if (controller.state().screenState() == ScreenState.IDLE) {
      controller.handle(new TerritoryDetailEvent.Initialize(System.nanoTime()));
    } else if (controller.state().screenState() != ScreenState.LOADING) {
      controller.handle(new TerritoryDetailEvent.Retry(System.nanoTime()));
    }
  }

  @Override public void tick() {
    super.tick();
    controller.handle(new TerritoryDetailEvent.Tick(System.nanoTime()));
    Forge1201SingleTerritoryClientState.Snapshot snapshot = Forge1201SingleTerritoryClientState.snapshot();
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
    if (search != null) search.visible = controller.state().searchVisible();
    controller.pollNavigation().ifPresent(this::navigate);
  }

  private void navigate(UiNavigation navigation) {
    if (minecraft == null) return;
    if (navigation instanceof UiNavigation.Back) {
      minecraft.setScreen(parent);
    } else if (navigation instanceof UiNavigation.Target target) {
      Owned territory = controller.state().territory();
      if (target.targetId().equals("territory-buffs")) {
        minecraft.setScreen(new Forge1201BuffManageScreen(territory, this));
      } else if (target.targetId().equals("territory-invite")) {
        minecraft.setScreen(new Forge1201TerritoryInviteScreen(territory, this));
      } else if (target.targetId().equals("territory-delete")) {
        minecraft.setScreen(new Forge1201TerritoryConfirmationScreen(
            TerritoryConfirmationKind.REMOVE_TERRITORY,
            territory.summary().territoryId(), territory.summary().name(), null, "", this));
      }
    }
  }

  @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    TerritoryDetailLayout.Layout layout = commonLayout();
    UiScale scale = layout.scale();
    Forge1201UiRenderer renderer = new Forge1201UiRenderer(graphics, font);
    renderer.fillPhysicalBackground(width, height, TerritoryDetailLayout.BACKGROUND_COLOR);
    if (search != null) search.visible = controller.state().searchVisible();
    graphics.pose().pushPose();
    graphics.pose().scale(scale.value(), scale.value(), 1.0f);
    TerritoryDetailView.render(renderer, controller.state(), layout,
        scale.toVirtualX(mouseX), scale.toVirtualY(mouseY));
    graphics.pose().popPose();
    super.render(graphics, mouseX, mouseY, partialTick);
  }

  @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
    TerritoryDetailLayout.Layout layout = commonLayout();
    int x = layout.scale().toVirtualX(mouseX);
    int y = layout.scale().toVirtualY(mouseY);

    for (var nav : layout.navigationButtons()) if (nav.rect().contains(x, y)) {
      controller.handle(new TerritoryDetailEvent.ActionClicked(nav.action(), null, System.nanoTime()));
      return true;
    }
    for (var action : layout.quickActions()) if (action.rect().contains(x, y)) {
      controller.handle(new TerritoryDetailEvent.ActionClicked(action.action(), null, System.nanoTime()));
      return true;
    }
    for (var setting : layout.settingsActions()) if (setting.button().contains(x, y)) {
      controller.handle(new TerritoryDetailEvent.ActionClicked(setting.action(), null, System.nanoTime()));
      return true;
    }
    if (controller.state().view() == TerritoryDetailViewKind.ACCESS
        && layout.inviteButton().contains(x, y)) {
      controller.handle(new TerritoryDetailEvent.ActionClicked(TerritoryDetailAction.INVITE, null,
          System.nanoTime()));
      return true;
    }
    for (var row : layout.accessCards()) if (row.actionButton().contains(x, y)) {
      if (minecraft != null) {
        var territory = controller.state().territory();
        minecraft.setScreen(new Forge1201TerritoryConfirmationScreen(
            TerritoryConfirmationKind.REMOVE_MEMBER,
            territory.summary().territoryId(), territory.summary().name(),
            row.row().playerId(), row.row().playerName(), this));
      }
      return true;
    }
    for (var row : layout.ruleCards()) if (row.actionButton().contains(x, y)) {
      controller.handle(new TerritoryDetailEvent.RuleClicked(row.row().action(), System.nanoTime()));
      return true;
    }
    for (var row : layout.transferCards()) if (row.actionButton().contains(x, y)) {
      controller.handle(new TerritoryDetailEvent.ActionClicked(TerritoryDetailAction.TRANSFER_OWNERSHIP,
          row.player().playerId(), System.nanoTime()));
      return true;
    }
    if (controller.state().screenState() == ScreenState.ERROR && layout.retryButton().contains(x, y)) {
      controller.handle(new TerritoryDetailEvent.Retry(System.nanoTime()));
      return true;
    }
    if (controller.state().searchVisible() && controller.state().totalPages() > 1
        && layout.previousButton().contains(x, y)) {
      controller.handle(new TerritoryDetailEvent.Scroll(-1)); return true;
    }
    if (controller.state().searchVisible() && controller.state().totalPages() > 1
        && layout.nextButton().contains(x, y)) {
      controller.handle(new TerritoryDetailEvent.Scroll(1)); return true;
    }
    if (layout.backButton().contains(x, y)) {
      controller.handle(new TerritoryDetailEvent.ActionClicked(TerritoryDetailAction.BACK, null));
      return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    if (delta != 0 && controller.state().searchVisible() && controller.state().totalPages() > 1) {
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

    @Override public void copyTerritoryId(UUID territoryId) {
      if (minecraft == null) return;
      minecraft.keyboardHandler.setClipboard(territoryId.toString());
      if (minecraft.player != null) {
        minecraft.player.displayClientMessage(
            Component.translatable("message.territory_management.copy_success"), false);
      }
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

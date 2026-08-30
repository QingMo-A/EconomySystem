package com.mo.economy_system.target.forge1201.client;

import com.mo.economy_system.common.client.ClientCommissionState;
import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.common.network.CommissionDataResponseMessage;
import com.mo.economy_system.common.network.CommissionSubmitMessage;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.ui.commission.CommissionCenterAction;
import com.mo.economy_system.ui.commission.CommissionCenterController;
import com.mo.economy_system.ui.commission.CommissionCenterEvent;
import com.mo.economy_system.ui.commission.CommissionCenterLayout;
import com.mo.economy_system.ui.commission.CommissionCenterPort;
import com.mo.economy_system.ui.commission.CommissionCenterView;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import com.mo.economy_system.ui.geometry.UiScale;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Forge 1.20.1 shell for the common personal commission center. */
public final class Forge1201CommissionCenterScreen extends Screen {
  private final Screen parent;
  private final Port port = new Port();
  private final CommissionCenterController controller = new CommissionCenterController(port);
  private long appliedRequestId = -1L;

  public Forge1201CommissionCenterScreen() { this(null); }
  public Forge1201CommissionCenterScreen(Screen parent) {
    super(Component.translatable(EconomyUiRoute.COMMISSIONS.titleKey()));
    this.parent = parent;
  }

  @Override protected void init() {
    if (controller.state().screenState() == ScreenState.IDLE) {
      controller.handle(new CommissionCenterEvent.Initialize(System.nanoTime()));
    }
  }

  @Override public void tick() {
    super.tick();
    controller.handle(new CommissionCenterEvent.Tick(System.nanoTime()));
    ClientCommissionState.Snapshot snapshot = ClientCommissionState.snapshot();
    if (snapshot.requestId() != appliedRequestId && snapshot.requestId() == port.requestId
        && !snapshot.loading()) {
      appliedRequestId = snapshot.requestId();
      controller.handle(new CommissionCenterEvent.DataLoaded(CommissionDataResponseMessage.data(
          snapshot.requestId(), snapshot.serverNowMillis(), snapshot.nextRefreshAt(),
          snapshot.maxActivePersonalCommissions(), snapshot.commissions())));
    }
    controller.pollNavigation().ifPresent(this::navigate);
  }

  @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    CommissionCenterLayout.Layout layout = CommissionCenterLayout.calculate(width, height,
        controller.state(), new Forge1201UiTextMetrics(font));
    UiScale scale = layout.scale();
    Forge1201UiRenderer renderer = new Forge1201UiRenderer(graphics, font);
    renderer.fillPhysicalBackground(width, height, CommissionCenterLayout.BACKGROUND_COLOR);
    graphics.pose().pushPose();
    graphics.pose().scale(scale.value(), scale.value(), 1.0f);
    CommissionCenterView.render(renderer, controller.state(), layout,
        scale.toVirtualX(mouseX), scale.toVirtualY(mouseY));
    graphics.pose().popPose();
    super.render(graphics, mouseX, mouseY, partialTick);
  }

  @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
    CommissionCenterLayout.Layout layout = CommissionCenterLayout.calculate(width, height,
        controller.state(), new Forge1201UiTextMetrics(font));
    int x = layout.scale().toVirtualX(mouseX), y = layout.scale().toVirtualY(mouseY);
    for (CommissionCenterLayout.Card card : layout.cards()) {
      if (card.rect().contains(x, y)) {
        controller.handle(new CommissionCenterEvent.Selected(card.commission().commissionId()));
        return true;
      }
    }
    if (layout.submit().contains(x, y)) {
      UUID id = controller.state().selectedCommissionId();
      controller.handle(new CommissionCenterEvent.ActionClicked(CommissionCenterAction.SUBMIT, id, 1));
      return true;
    }
    if (layout.publicTab().contains(x, y)) {
      controller.handle(new CommissionCenterEvent.ActionClicked(CommissionCenterAction.PUBLIC, null, 0));
      return true;
    }
    if (layout.back().contains(x, y)) {
      controller.handle(new CommissionCenterEvent.ActionClicked(CommissionCenterAction.BACK, null, 0));
      return true;
    }
    if (controller.state().screenState() == ScreenState.ERROR && layout.retry().contains(x, y)) {
      controller.handle(new CommissionCenterEvent.ActionClicked(CommissionCenterAction.RETRY, null, 0));
      return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == 256) { onClose(); return true; }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override public void onClose() {
    if (minecraft == null) return;
    if (parent != null) minecraft.setScreen(parent); else minecraft.setScreen(new Forge1201HomeScreen());
  }

  @Override public boolean isPauseScreen() { return false; }

  private void navigate(UiNavigation navigation) {
    if (minecraft == null) return;
    if (navigation instanceof UiNavigation.Route route) {
      if (route.route() == EconomyUiRoute.HOME) onClose();
      else if (route.route() == EconomyUiRoute.PUBLIC_COMMISSIONS) {
        minecraft.setScreen(new Forge1201PublicCommissionCenterScreen(this));
      }
    }
  }

  private final class Port implements CommissionCenterPort {
    private long requestId = -1L;
    @Override public long nextRequestId() { return ClientCommissionState.nextRequestId(); }
    @Override public void requestData(long id) {
      requestId = id;
      EconomyServices.platform().network().sendToServer(
          new com.mo.economy_system.common.network.CommissionDataRequestMessage(id));
    }
    @Override public void submit(long id, UUID commissionId, UUID submissionId, int amount) {
      EconomyServices.platform().network().sendToServer(
          new CommissionSubmitMessage(id, commissionId, submissionId, amount));
    }
  }
}

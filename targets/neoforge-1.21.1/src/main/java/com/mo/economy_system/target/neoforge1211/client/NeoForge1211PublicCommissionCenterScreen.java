package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.client.ClientPublicCommissionState;
import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.common.network.commission_public.PublicCommissionActionResponseMessage;
import com.mo.economy_system.common.network.commission_public.PublicCommissionDataResponseKind;
import com.mo.economy_system.common.network.commission_public.PublicCommissionDataResponseMessage;
import com.mo.economy_system.common.network.commission_public.PublicCommissionSubmitMessage;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.ui.commission_public.*;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import com.mo.economy_system.ui.geometry.UiScale;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** NeoForge widget shell for the common public commission center. */
public final class NeoForge1211PublicCommissionCenterScreen extends Screen {
  private final Screen parent;
  private final Port port = new Port();
  private final PublicCommissionCenterController controller = new PublicCommissionCenterController(port);
  private EditBox amount;
  private long appliedDataRequestId = -1L;
  private long appliedActionRequestId = -1L;

  public NeoForge1211PublicCommissionCenterScreen() { this(null); }
  public NeoForge1211PublicCommissionCenterScreen(Screen parent) {
    super(Component.translatable(EconomyUiRoute.PUBLIC_COMMISSIONS.titleKey()));
    this.parent = parent;
  }

  @Override protected void init() {
    PublicCommissionCenterLayout.Layout layout = layout();
    UiScale scale = layout.scale();
    amount = new NeoForge1211UnderlinedEditBox(font,
        Math.round(layout.amountInput().x() * scale.value()),
        Math.round(layout.amountInput().y() * scale.value()),
        Math.max(1, Math.round(layout.amountInput().width() * scale.value())),
        Math.max(1, Math.round(layout.amountInput().height() * scale.value())),
        Component.translatable("screen.commissions.public.amount"));
    NeoForge1211UiInputAdapter.apply(amount);
    amount.setMaxLength(7);
    amount.setValue("1");
    addRenderableWidget(amount);
    if (controller.state().screenState() == ScreenState.IDLE) {
      controller.handle(new PublicCommissionCenterEvent.Initialize(System.nanoTime()));
    }
  }

  @Override public void tick() {
    super.tick();
    controller.handle(new PublicCommissionCenterEvent.Tick(System.nanoTime()));
    ClientPublicCommissionState.Snapshot snapshot = ClientPublicCommissionState.snapshot();
    if (snapshot.requestId() == port.dataRequestId && snapshot.requestId() != appliedDataRequestId
        && !snapshot.loading()) {
      appliedDataRequestId = snapshot.requestId();
      controller.handle(new PublicCommissionCenterEvent.DataLoaded(new PublicCommissionDataResponseMessage(
          snapshot.errorKey().isBlank() ? PublicCommissionDataResponseKind.DATA : PublicCommissionDataResponseKind.ERROR,
          snapshot.requestId(), snapshot.serverNowMillis(), snapshot.commissions(), snapshot.errorKey())));
    }
    if (snapshot.requestId() == port.actionRequestId && snapshot.requestId() != appliedActionRequestId
        && snapshot.lastSubmitStatus() != null) {
      appliedActionRequestId = snapshot.requestId();
      controller.handle(new PublicCommissionCenterEvent.ActionResult(new PublicCommissionActionResponseMessage(
          snapshot.requestId(), snapshot.lastSubmitStatus(), 0, 0, snapshot.actionMessage())));
    }
    controller.pollNavigation().ifPresent(this::navigate);
  }

  @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    PublicCommissionCenterLayout.Layout layout = layout();
    UiScale scale = layout.scale();
    NeoForge1211UiRenderer renderer = new NeoForge1211UiRenderer(graphics, font);
    renderer.fillPhysicalBackground(width, height, PublicCommissionCenterLayout.BACKGROUND_COLOR);
    graphics.pose().pushPose();
    graphics.pose().scale(scale.value(), scale.value(), 1.0f);
    PublicCommissionCenterView.render(controller.state(), layout, renderer,
        scale.toVirtualX(mouseX), scale.toVirtualY(mouseY));
    graphics.pose().popPose();
    super.render(graphics, mouseX, mouseY, partialTick);
  }

  @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
    PublicCommissionCenterLayout.Layout layout = layout();
    int x = layout.scale().toVirtualX(mouseX), y = layout.scale().toVirtualY(mouseY);
    for (PublicCommissionCenterLayout.Card card : layout.cards()) {
      if (card.rect().contains(x, y)) {
        controller.handle(new PublicCommissionCenterEvent.Selected(card.commission().commissionId()));
        return true;
      }
    }
    if (layout.submit().contains(x, y)) {
      controller.handle(new PublicCommissionCenterEvent.ActionClicked(
          PublicCommissionCenterAction.SUBMIT, controller.state().selectedCommissionId(), parseAmount()));
      return true;
    }
    if (layout.back().contains(x, y)) {
      controller.handle(new PublicCommissionCenterEvent.ActionClicked(PublicCommissionCenterAction.BACK, null, 0));
      return true;
    }
    if (controller.state().screenState() == ScreenState.ERROR && layout.retry().contains(x, y)) {
      controller.handle(new PublicCommissionCenterEvent.ActionClicked(PublicCommissionCenterAction.RETRY, null, 0));
      return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  private int parseAmount() {
    try { return Integer.parseInt(amount == null ? "0" : amount.getValue().trim()); }
    catch (NumberFormatException ignored) { return 0; }
  }

  @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == 256) { onClose(); return true; }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override public void onClose() {
    if (minecraft != null) minecraft.setScreen(parent != null ? parent : new NeoForge1211HomeScreen());
  }

  @Override public boolean isPauseScreen() { return false; }
  @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

  private PublicCommissionCenterLayout.Layout layout() {
    return PublicCommissionCenterLayout.calculate(width, height, controller.state(),
        new NeoForge1211UiTextMetrics(font));
  }

  private void navigate(UiNavigation navigation) {
    if (navigation instanceof UiNavigation.Route route && route.route() == EconomyUiRoute.HOME) onClose();
  }

  private final class Port implements PublicCommissionCenterPort {
    private long dataRequestId = -1L;
    private long actionRequestId = -1L;
    @Override public long nextRequestId() { return ClientPublicCommissionState.nextRequestId(); }
    @Override public void requestData(long requestId) {
      dataRequestId = requestId;
      EconomyServices.platform().network().sendToServer(new com.mo.economy_system.common.network.commission_public.PublicCommissionDataRequestMessage(requestId));
    }
    @Override public void submit(long requestId, UUID commissionId, UUID submissionId, int value) {
      actionRequestId = requestId;
      EconomyServices.platform().network().sendToServer(new PublicCommissionSubmitMessage(requestId, commissionId, submissionId, value));
    }
  }
}

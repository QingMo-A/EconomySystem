package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.check.ClientFileCheckConsentCoordinator;
import com.mo.economy_system.common.check.ClientFileCheckResult;
import com.mo.economy_system.common.check.ClientFileCheckScanner;
import com.mo.economy_system.common.check.ClientFileCheckTaskCoordinator;
import com.mo.economy_system.common.network.ClientFileCheckRequestMessage;
import com.mo.economy_system.ui.check.CheckConsentAction;
import com.mo.economy_system.ui.check.CheckConsentController;
import com.mo.economy_system.ui.check.CheckConsentEvent;
import com.mo.economy_system.ui.check.CheckConsentLayout;
import com.mo.economy_system.ui.check.CheckConsentPort;
import com.mo.economy_system.ui.check.CheckConsentView;
import com.mo.economy_system.ui.geometry.UiScale;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** NeoForge shell for the loader-neutral client file-check consent page. */
public final class Screen_ClientFileCheckConsent extends Screen {
  private final ClientFileCheckRequestMessage request;
  private final ClientFileCheckTaskCoordinator.RequestIdentity identity;
  private final ClientFileCheckTaskCoordinator.Session session;
  private final AtomicBoolean finished = new AtomicBoolean();
  private final long deadline = System.nanoTime() + 60_000_000_000L;
  private final CheckConsentController controller;

  public Screen_ClientFileCheckConsent(
      ClientFileCheckRequestMessage request,
      ClientFileCheckTaskCoordinator.RequestIdentity identity,
      ClientFileCheckTaskCoordinator.Session session) {
    super(Component.translatable("screen.check_consent.title"));
    this.request = request;
    this.identity = identity;
    this.session = session;
    controller = new CheckConsentController(request.requesterPlayerName(), request.checkType().id(), new Port());
  }

  private void allow() {
    if (System.nanoTime() > deadline) {
      if (finished.compareAndSet(false, true)) {
        terminal(ClientFileCheckResult.failed(request.checkType(), "REQUEST_EXPIRED"), null);
      }
      Minecraft.getInstance().setScreen(null);
      return;
    }
    if (!finished.compareAndSet(false, true)) return;
    Minecraft minecraft = Minecraft.getInstance();
    ClientFileCheckTaskCoordinator coordinator = NeoForge1211ClientFileCheckClientRuntime.tasks();
    if (!NeoForge1211ClientFileCheckClientRuntime.consent()
        .transition(
            identity,
            session,
            ClientFileCheckConsentCoordinator.State.CONSENT,
            ClientFileCheckConsentCoordinator.State.SCANNING)) {
      minecraft.setScreen(null);
      return;
    }
    ClientFileCheckTaskCoordinator.TaskToken token =
        session == null
            ? null
            : coordinator.submit(
                session,
                identity,
                1,
                () -> new ClientFileCheckScanner().scan(minecraft.gameDirectory.toPath(), request.checkType()),
                minecraft::execute,
                ignored ->
                    minecraft.getConnection() == session.connectionIdentity()
                        && minecraft.player != null
                        && minecraft.player.getUUID().equals(session.localPlayerId()),
                (callbackToken, result) -> terminal(result, callbackToken),
                (callbackToken, failure) ->
                    terminal(ClientFileCheckResult.failed(request.checkType(), "SCAN_FAILED"), callbackToken),
                (abandonedToken, failure) ->
                    NeoForge1211ClientFileCheckClientRuntime.consent().finish(identity, session));
    if (token == null) terminal(ClientFileCheckResult.failed(request.checkType(), "SCANNER_BUSY"), null);
    minecraft.setScreen(null);
  }

  private void decline() {
    if (!finished.compareAndSet(false, true)) return;
    terminal(ClientFileCheckResult.declined(request.checkType()), null);
    Minecraft.getInstance().setScreen(null);
  }

  private void terminal(
      ClientFileCheckResult result, ClientFileCheckTaskCoordinator.TaskToken token) {
    NeoForge1211ClientFileCheckScreens.dispatchTerminal(request, identity, session, token, result);
  }

  @Override
  public void onClose() {
    controller.handle(new CheckConsentEvent.ActionClicked(CheckConsentAction.DECLINE));
  }

  @Override
  public boolean shouldCloseOnEsc() {
    return true;
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    CheckConsentLayout.Layout layout = CheckConsentLayout.calculate(width, height);
    UiScale scale = layout.scale();
    NeoForge1211UiRenderer renderer = new NeoForge1211UiRenderer(graphics, font);
    renderer.fillPhysicalBackground(width, height, CheckConsentLayout.BACKGROUND_COLOR);
    graphics.pose().pushPose();
    graphics.pose().scale(scale.value(), scale.value(), 1.0f);
    CheckConsentView.render(
        renderer,
        controller.state(),
        layout,
        scale.toVirtualX(mouseX),
        scale.toVirtualY(mouseY));
    graphics.pose().popPose();
    super.render(graphics, mouseX, mouseY, partialTick);
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
    CheckConsentLayout.Layout layout = CheckConsentLayout.calculate(width, height);
    int x = layout.scale().toVirtualX(mouseX);
    int y = layout.scale().toVirtualY(mouseY);
    if (layout.allow().contains(x, y)) {
      controller.handle(new CheckConsentEvent.ActionClicked(CheckConsentAction.ALLOW));
      return true;
    }
    if (layout.decline().contains(x, y)) {
      controller.handle(new CheckConsentEvent.ActionClicked(CheckConsentAction.DECLINE));
      return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

  private final class Port implements CheckConsentPort {
    @Override
    public void allow() {
      Screen_ClientFileCheckConsent.this.allow();
    }

    @Override
    public void decline() {
      Screen_ClientFileCheckConsent.this.decline();
    }
  }
}

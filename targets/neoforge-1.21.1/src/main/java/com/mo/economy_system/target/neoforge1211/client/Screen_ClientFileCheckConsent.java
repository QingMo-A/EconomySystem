package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.check.ClientFileCheckLayout;
import com.mo.economy_system.common.check.ClientFileCheckResult;
import com.mo.economy_system.common.check.ClientFileCheckScanner;
import com.mo.economy_system.common.check.ClientFileCheckTaskCoordinator;
import com.mo.economy_system.common.network.ClientFileCheckRequestMessage;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class Screen_ClientFileCheckConsent extends Screen {
  private final ClientFileCheckRequestMessage request;
  private final ClientFileCheckTaskCoordinator.RequestIdentity identity;
  private final ClientFileCheckTaskCoordinator.Session session;
  private final AtomicBoolean finished = new AtomicBoolean();
  private final long deadline = System.nanoTime() + 60_000_000_000L;

  public Screen_ClientFileCheckConsent(
      ClientFileCheckRequestMessage request,
      ClientFileCheckTaskCoordinator.RequestIdentity identity,
      ClientFileCheckTaskCoordinator.Session session) {
    super(Component.translatable("screen.check_consent.title"));
    this.request = request;
    this.identity = identity;
    this.session = session;
  }

  @Override
  protected void init() {
    ClientFileCheckLayout.Consent layout = ClientFileCheckLayout.consent(width, height);
    if (layout.allow() != null) {
      addRenderableWidget(
          Button.builder(Component.translatable("button.check_consent.allow"), b -> allow())
              .bounds(
                  layout.allow().x(),
                  layout.allow().y(),
                  layout.allow().width(),
                  layout.allow().height())
              .build());
      addRenderableWidget(
          Button.builder(Component.translatable("button.check_consent.decline"), b -> decline())
              .bounds(
                  layout.decline().x(),
                  layout.decline().y(),
                  layout.decline().width(),
                  layout.decline().height())
              .build());
    }
  }

  private void allow() {
    if (System.nanoTime() > deadline) {
      if (finished.compareAndSet(false, true))
        terminal(ClientFileCheckResult.failed(request.checkType(), "REQUEST_EXPIRED"), null);
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
            com.mo.economy_system.common.check.ClientFileCheckConsentCoordinator.State.CONSENT,
            com.mo.economy_system.common.check.ClientFileCheckConsentCoordinator.State.SCANNING)) {
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
                () ->
                    new ClientFileCheckScanner()
                        .scan(minecraft.gameDirectory.toPath(), request.checkType()),
                minecraft::execute,
                ignored ->
                    minecraft.getConnection() == session.connectionIdentity()
                        && minecraft.player != null
                        && minecraft.player.getUUID().equals(session.localPlayerId()),
                result -> terminal(result, tokenHolder[0]),
                failure ->
                    terminal(
                        ClientFileCheckResult.failed(request.checkType(), "SCAN_FAILED"),
                        tokenHolder[0]));
    tokenHolder[0] = token;
    if (token == null)
      terminal(ClientFileCheckResult.failed(request.checkType(), "SCANNER_BUSY"), null);
    minecraft.setScreen(null);
  }

  private void decline() {
    if (!finished.compareAndSet(false, true)) return;
    terminal(ClientFileCheckResult.declined(request.checkType()), null);
    Minecraft.getInstance().setScreen(null);
  }

  private final ClientFileCheckTaskCoordinator.TaskToken[] tokenHolder =
      new ClientFileCheckTaskCoordinator.TaskToken[1];

  private void terminal(
      ClientFileCheckResult result, ClientFileCheckTaskCoordinator.TaskToken token) {
    NeoForge1211ClientFileCheckScreens.dispatchTerminal(request, identity, session, token, result);
  }

  @Override
  public void onClose() {
    decline();
  }

  @Override
  public boolean shouldCloseOnEsc() {
    return true;
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    super.render(graphics, mouseX, mouseY, partialTick);
    int x = width / 2;
    int y = 35;
    graphics.drawCenteredString(font, title, x, y, 0xFFFFFF);
    graphics.drawCenteredString(
        font,
        Component.translatable("screen.check_consent.requester", request.requesterPlayerName()),
        x,
        y + 22,
        0xDDDDDD);
    graphics.drawCenteredString(
        font,
        Component.translatable("screen.check_consent.type", request.checkType().id()),
        x,
        y + 38,
        0xDDDDDD);
    graphics.drawCenteredString(
        font,
        Component.translatable("screen.check_consent.folder", request.checkType().id()),
        x,
        y + 54,
        0xDDDDDD);
    graphics.drawCenteredString(
        font, Component.translatable("screen.check_consent.data_notice"), x, y + 76, 0xAAAAAA);
    graphics.drawCenteredString(
        font,
        Component.translatable("screen.check_consent.no_content_notice"),
        x,
        y + 92,
        0xAAAAAA);
  }
}

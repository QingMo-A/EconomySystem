package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.check.ClientFileCheckComparison;
import com.mo.economy_system.common.check.ClientFileCheckConsentCoordinator;
import com.mo.economy_system.common.check.ClientFileCheckLayout;
import com.mo.economy_system.common.check.ClientFileCheckResult;
import com.mo.economy_system.common.check.ClientFileCheckResultController;
import com.mo.economy_system.common.check.ClientFileCheckResultJsonCodec;
import com.mo.economy_system.common.check.ClientFileCheckScanner;
import com.mo.economy_system.common.check.ClientFileCheckTaskCoordinator;
import com.mo.economy_system.common.network.ClientFileCheckRequestMessage;
import com.mo.economy_system.common.network.ClientFileCheckResultRequestMessage;
import com.mo.economy_system.common.network.ClientFileCheckResultResponseMessage;
import com.mo.economy_system.target.forge1201.client.Forge1201ClientFileCheckClientRuntime;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class Forge1201ClientFileCheckScreens {
  private Forge1201ClientFileCheckScreens() {}

  static void openConsent(ClientFileCheckRequestMessage message) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null || !minecraft.player.getUUID().equals(message.targetPlayerId()))
      return;
    if (minecraft.getConnection() == null) return;
    Forge1201ClientFileCheckClientRuntime.currentOrBegin(
        minecraft.getConnection(), minecraft.player.getUUID());
    ClientFileCheckTaskCoordinator.RequestIdentity identity = identity(message);
    ClientFileCheckConsentCoordinator.Decision decision =
        Forge1201ClientFileCheckClientRuntime.consent().receive(identity);
    if (decision == ClientFileCheckConsentCoordinator.Decision.DUPLICATE) return;
    if (decision == ClientFileCheckConsentCoordinator.Decision.BUSY) {
      send(message, ClientFileCheckResult.failed(message.checkType(), "CONSENT_BUSY"));
      return;
    }
    minecraft.setScreen(new Screen_ClientFileCheckConsent(message, identity));
  }

  private static ClientFileCheckTaskCoordinator.RequestIdentity identity(
      ClientFileCheckRequestMessage message) {
    return new ClientFileCheckTaskCoordinator.RequestIdentity(
        message.targetPlayerId(), message.requesterPlayerId(), message.checkType());
  }

  private static void send(ClientFileCheckRequestMessage request, ClientFileCheckResult result) {
    Forge1201NetworkChannel.sendToServer(
        new ClientFileCheckResultRequestMessage(
            request.targetPlayerName(),
            request.targetPlayerId(),
            request.requesterPlayerName(),
            request.requesterPlayerId(),
            request.checkType(),
            ClientFileCheckResultJsonCodec.encode(result)));
  }

  static void openResult(ClientFileCheckResultResponseMessage message) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null || !minecraft.player.getUUID().equals(message.requesterPlayerId()))
      return;
    if (minecraft.getConnection() == null) return;
    Forge1201ClientFileCheckClientRuntime.currentOrBegin(
        minecraft.getConnection(), minecraft.player.getUUID());
    try {
      ClientFileCheckResult result = ClientFileCheckResultJsonCodec.decode(message.resultJson());
      if (result.checkType() == message.checkType())
        minecraft.setScreen(new Screen_ClientFileCheckResult(message, result));
    } catch (RuntimeException ignored) {
      // Fail closed.
    }
  }

  static final class Screen_ClientFileCheckConsent extends Screen {
    private final ClientFileCheckRequestMessage request;
    private final ClientFileCheckTaskCoordinator.RequestIdentity identity;
    private final AtomicBoolean finished = new AtomicBoolean();
    private final long deadline = System.nanoTime() + 60_000_000_000L;

    Screen_ClientFileCheckConsent(
        ClientFileCheckRequestMessage request,
        ClientFileCheckTaskCoordinator.RequestIdentity identity) {
      super(Component.translatable("screen.check_consent.title"));
      this.request = request;
      this.identity = identity;
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
        decline();
        return;
      }
      if (!finished.compareAndSet(false, true)) return;
      Minecraft minecraft = Minecraft.getInstance();
      ClientFileCheckTaskCoordinator coordinator = Forge1201ClientFileCheckClientRuntime.tasks();
      ClientFileCheckTaskCoordinator.Session session = coordinator.currentSession();
      ClientFileCheckTaskCoordinator.TaskToken token =
          session == null
              ? null
              : coordinator.submit(
                  session,
                  identity,
                  1,
                  () -> {
                    ClientFileCheckResult result;
                    try {
                      result =
                          new ClientFileCheckScanner()
                              .scan(minecraft.gameDirectory.toPath(), request.checkType());
                    } catch (RuntimeException failure) {
                      result = ClientFileCheckResult.failed(request.checkType(), "SCAN_FAILED");
                    }
                    return result;
                  },
                  minecraft::execute,
                  ignored ->
                      minecraft.getConnection() == session.connectionIdentity()
                          && minecraft.player != null
                          && minecraft.player.getUUID().equals(session.localPlayerId()),
                  this::send);
      if (token == null) send(ClientFileCheckResult.failed(request.checkType(), "SCANNER_BUSY"));
      Forge1201ClientFileCheckClientRuntime.consent().finish(identity);
      minecraft.setScreen(null);
    }

    private void decline() {
      if (!finished.compareAndSet(false, true)) return;
      send(ClientFileCheckResult.declined(request.checkType()));
      Forge1201ClientFileCheckClientRuntime.consent().finish(identity);
      Minecraft.getInstance().setScreen(null);
    }

    private void send(ClientFileCheckResult result) {
      Forge1201ClientFileCheckScreens.send(request, result);
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
      renderBackground(graphics);
      super.render(graphics, mouseX, mouseY, partialTick);
      int x = width / 2, y = 35;
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

  static final class Screen_ClientFileCheckResult extends Screen {
    private final ClientFileCheckResultResponseMessage message;
    private final ClientFileCheckResult result;
    private final ClientFileCheckResultController controller;
    private ClientFileCheckTaskCoordinator.TaskToken task;
    private EditBox search;
    private int offset;

    Screen_ClientFileCheckResult(
        ClientFileCheckResultResponseMessage message, ClientFileCheckResult result) {
      super(Component.translatable("screen.check_result.title"));
      this.message = message;
      this.result = result;
      this.controller = new ClientFileCheckResultController(result);
    }

    @Override
    protected void init() {
      ClientFileCheckLayout.Box box = ClientFileCheckLayout.search(width, height);
      if (box != null) {
        search =
            new EditBox(
                font,
                box.x(),
                box.y(),
                box.width(),
                box.height(),
                Component.translatable("screen.check_result.search"));
        search.setHint(Component.translatable("screen.check_result.search"));
        search.setResponder(ignored -> offset = 0);
        addRenderableWidget(search);
      }
      if (!controller.needsComparison() || task != null) return;
      Minecraft minecraft = Minecraft.getInstance();
      ClientFileCheckTaskCoordinator coordinator = Forge1201ClientFileCheckClientRuntime.tasks();
      ClientFileCheckTaskCoordinator.Session session = coordinator.currentSession();
      long generation = controller.generation();
      ClientFileCheckTaskCoordinator.RequestIdentity identity =
          new ClientFileCheckTaskCoordinator.RequestIdentity(
              message.targetPlayerId(), message.requesterPlayerId(), message.checkType());
      task =
          session == null
              ? null
              : coordinator.submit(
                  session,
                  identity,
                  generation,
                  () ->
                      new ClientFileCheckScanner()
                          .scan(minecraft.gameDirectory.toPath(), message.checkType()),
                  minecraft::execute,
                  token ->
                      minecraft.screen == this
                          && minecraft.getConnection() == session.connectionIdentity()
                          && minecraft.player != null
                          && minecraft.player.getUUID().equals(session.localPlayerId())
                          && controller.generation() == token.controllerGeneration(),
                  local -> controller.apply(generation, local));
      if (task == null) controller.busy();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      int visible = ClientFileCheckLayout.visibleRows(height);
      int size = filtered().size();
      offset = ClientFileCheckLayout.clampOffset(offset - (int) Math.signum(delta), size, visible);
      return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      renderBackground(graphics);
      super.render(graphics, mouseX, mouseY, partialTick);
      graphics.drawCenteredString(font, title, width / 2, 18, 0xFFFFFF);
      graphics.drawString(
          font,
          Component.translatable("screen.check_result.target", message.targetPlayerName()),
          12,
          38,
          0xDDDDDD);
      graphics.drawString(
          font,
          Component.translatable("screen.check_result.type", message.checkType().id()),
          12,
          48,
          0xDDDDDD);
      graphics.drawString(
          font,
          Component.translatable(
              "screen.check_result.status_" + result.status().name().toLowerCase(Locale.ROOT)),
          12,
          84,
          0xCCCCCC);
      graphics.drawString(
          font,
          Component.literal(
              "files=" + result.files().size() + "  skipped=" + result.skipped().size()),
          12,
          96,
          0xCCCCCC);
      if (result.errorCode() != null)
        graphics.drawString(
            font,
            Component.translatable("screen.check_result.error", result.errorCode()),
            12,
            108,
            0xCC7777);
      if (!result.skipped().isEmpty()) {
        var skipped = result.skipped().get(0);
        graphics.drawString(
            font,
            Component.translatable(
                "screen.check_result.skipped", skipped.fileName() + " (" + skipped.reason() + ")"),
            12,
            120,
            0xAAAAAA);
      }
      List<ClientFileCheckComparison.Row> visibleRows = filtered();
      int visible = ClientFileCheckLayout.visibleRows(height);
      for (int i = offset; i < visibleRows.size() && i < offset + visible; i++) {
        ClientFileCheckComparison.Row row = visibleRows.get(i);
        String line =
            row.fileName()
                + "  "
                + Component.translatable(
                        "screen.check_result." + row.kind().name().toLowerCase(Locale.ROOT))
                    .getString();
        graphics.drawString(
            font,
            font.plainSubstrByWidth(line, Math.max(40, width - 24)),
            12,
            136 + (i - offset) * 12,
            0xAAAAAA);
      }
    }

    private List<ClientFileCheckComparison.Row> filtered() {
      return controller.filtered(search == null ? "" : search.getValue());
    }

    @Override
    public void removed() {
      controller.invalidate();
      if (task != null) task.cancel();
      super.removed();
    }
  }
}

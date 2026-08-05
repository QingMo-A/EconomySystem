package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.check.ClientFileCheckClientResultDispatcher;
import com.mo.economy_system.common.check.ClientFileCheckConsentCoordinator;
import com.mo.economy_system.common.check.ClientFileCheckLayout;
import com.mo.economy_system.common.check.ClientFileCheckResult;
import com.mo.economy_system.common.check.ClientFileCheckResultController;
import com.mo.economy_system.common.check.ClientFileCheckResultJsonCodec;
import com.mo.economy_system.common.check.ClientFileCheckScanner;
import com.mo.economy_system.common.check.ClientFileCheckStatus;
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
    ClientFileCheckTaskCoordinator.Session session =
        Forge1201ClientFileCheckClientRuntime.currentOrBegin(
            minecraft.getConnection(), minecraft.player.getUUID());
    ClientFileCheckTaskCoordinator.RequestIdentity identity = identity(message);
    ClientFileCheckConsentCoordinator.Decision decision =
        Forge1201ClientFileCheckClientRuntime.consent().receive(identity, session);
    if (decision == ClientFileCheckConsentCoordinator.Decision.DUPLICATE) return;
    if (decision == ClientFileCheckConsentCoordinator.Decision.BUSY) {
      dispatchBusy(
          message,
          identity,
          session,
          ClientFileCheckResult.failed(message.checkType(), "CONSENT_BUSY"));
      return;
    }
    minecraft.setScreen(new Screen_ClientFileCheckConsent(message, identity, session));
  }

  private static ClientFileCheckTaskCoordinator.RequestIdentity identity(
      ClientFileCheckRequestMessage message) {
    return new ClientFileCheckTaskCoordinator.RequestIdentity(
        message.targetPlayerId(), message.requesterPlayerId(), message.checkType());
  }

  private static boolean dispatchTerminal(
      ClientFileCheckRequestMessage request,
      ClientFileCheckTaskCoordinator.RequestIdentity identity,
      ClientFileCheckTaskCoordinator.Session session,
      ClientFileCheckTaskCoordinator.TaskToken token,
      ClientFileCheckResult result) {
    Minecraft minecraft = Minecraft.getInstance();
    return ClientFileCheckClientResultDispatcher.terminal(
        Forge1201ClientFileCheckClientRuntime.tasks(),
        Forge1201ClientFileCheckClientRuntime.consent(),
        session,
        identity,
        token,
        minecraft::getConnection,
        () -> minecraft.player == null ? null : minecraft.player.getUUID(),
        result,
        value -> sendRaw(request, value),
        (stage, ignored, failure) -> {});
  }

  private static boolean dispatchBusy(
      ClientFileCheckRequestMessage request,
      ClientFileCheckTaskCoordinator.RequestIdentity identity,
      ClientFileCheckTaskCoordinator.Session session,
      ClientFileCheckResult result) {
    Minecraft minecraft = Minecraft.getInstance();
    return ClientFileCheckClientResultDispatcher.busy(
        Forge1201ClientFileCheckClientRuntime.tasks(),
        Forge1201ClientFileCheckClientRuntime.consent(),
        session,
        identity,
        minecraft::getConnection,
        () -> minecraft.player == null ? null : minecraft.player.getUUID(),
        result,
        value -> sendRaw(request, value),
        (stage, ignored, failure) -> {});
  }

  private static void sendRaw(ClientFileCheckRequestMessage request, ClientFileCheckResult result) {
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
    private final ClientFileCheckTaskCoordinator.Session session;
    private final ClientFileCheckTaskCoordinator.TaskToken[] tokenHolder =
        new ClientFileCheckTaskCoordinator.TaskToken[1];
    private final AtomicBoolean finished = new AtomicBoolean();
    private final long deadline = System.nanoTime() + 60_000_000_000L;

    Screen_ClientFileCheckConsent(
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
      ClientFileCheckTaskCoordinator coordinator = Forge1201ClientFileCheckClientRuntime.tasks();
      if (!Forge1201ClientFileCheckClientRuntime.consent()
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

    private void terminal(
        ClientFileCheckResult result, ClientFileCheckTaskCoordinator.TaskToken token) {
      Forge1201ClientFileCheckScreens.dispatchTerminal(request, identity, session, token, result);
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
    private Button retry;
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
      ClientFileCheckLayout.Result layout = ClientFileCheckLayout.result(width, height, true);
      ClientFileCheckLayout.Box box = layout.search();
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
      if (layout.retry() != null && controller.needsComparison()) {
        retry =
            addRenderableWidget(
                Button.builder(
                        Component.translatable("button.check_result.retry"), ignored -> retry())
                    .bounds(
                        layout.retry().x(),
                        layout.retry().y(),
                        layout.retry().width(),
                        layout.retry().height())
                    .build());
        retry.visible = false;
      }
      if (!controller.needsComparison() || task != null) return;
      submit(controller.generation());
    }

    private void submit(long generation) {
      Minecraft minecraft = Minecraft.getInstance();
      ClientFileCheckTaskCoordinator coordinator = Forge1201ClientFileCheckClientRuntime.tasks();
      ClientFileCheckTaskCoordinator.Session session = coordinator.currentSession();
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
                  local -> {
                    controller.apply(generation, local);
                    task = null;
                  },
                  failure -> {
                    controller.failed(generation);
                    task = null;
                  });
      if (task == null) controller.busy(generation);
    }

    private void retry() {
      if (Minecraft.getInstance().screen != this) return;
      long generation = controller.retry();
      if (generation < 0) return;
      if (task != null) task.cancel();
      task = null;
      submit(generation);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      int visible = ClientFileCheckLayout.visibleRows(height, retry != null);
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
      ClientFileCheckResultController.LocalState localState = controller.localState();
      if (retry != null)
        retry.visible =
            localState == ClientFileCheckResultController.LocalState.BUSY
                || localState == ClientFileCheckResultController.LocalState.FAILED;
      if (controller.needsComparison()
          && localState != ClientFileCheckResultController.LocalState.READY)
        graphics.drawString(
            font,
            Component.translatable(
                switch (localState) {
                  case LOADING -> "screen.check_result.loading";
                  case BUSY -> "screen.check_result.local_scan_busy";
                  case FAILED -> "screen.check_result.local_scan_failed";
                  default -> "screen.check_result.loading";
                }),
            12,
            120,
            0xAAAAAA);
      if (result.status() == ClientFileCheckStatus.TRUNCATED)
        graphics.drawString(
            font, Component.translatable("screen.check_result.incomplete"), 12, 132, 0xCCAA66);
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
            Component.translatable(
                "screen.check_result.error",
                Component.translatable(
                    "screen.check_result.error_code."
                        + result.errorCode().toLowerCase(Locale.ROOT))),
            12,
            108,
            0xCC7777);
      List<ClientFileCheckResultController.UiRow> visibleRows = filtered();
      int visible = ClientFileCheckLayout.visibleRows(height, retry != null);
      int rowY = retry == null ? 148 : 172;
      for (int i = offset; i < visibleRows.size() && i < offset + visible; i++) {
        ClientFileCheckResultController.UiRow row = visibleRows.get(i);
        String key =
            row.type() == ClientFileCheckResultController.RowType.SKIPPED
                ? "screen.check_result.skip_reason." + row.reasonId()
                : "screen.check_result." + row.reasonId();
        String line = row.fileName() + "  " + Component.translatable(key).getString();
        graphics.drawString(
            font,
            font.plainSubstrByWidth(line, Math.max(40, width - 24)),
            12,
            rowY + (i - offset) * 12,
            0xAAAAAA);
      }
    }

    private List<ClientFileCheckResultController.UiRow> filtered() {
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

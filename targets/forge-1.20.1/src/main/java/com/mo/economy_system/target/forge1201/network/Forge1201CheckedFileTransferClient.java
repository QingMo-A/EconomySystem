package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.check.ClientFileCheckTaskCoordinator;
import com.mo.economy_system.common.network.CheckedFileTransferChunkRequestMessage;
import com.mo.economy_system.common.network.CheckedFileTransferChunkResponseMessage;
import com.mo.economy_system.common.network.CheckedFileTransferControlRequestMessage;
import com.mo.economy_system.common.network.CheckedFileTransferControlResponseMessage;
import com.mo.economy_system.common.network.CheckedFileTransferRequestMessage;
import com.mo.economy_system.common.transfer.CheckedFileSnapshotter;
import com.mo.economy_system.common.transfer.CheckedFileTransferClientCoordinator;
import com.mo.economy_system.common.transfer.CheckedFileTransferControl;
import com.mo.economy_system.common.transfer.CheckedFileTransferControlStatus;
import com.mo.economy_system.common.transfer.CheckedFileTransferManifestCache;
import com.mo.economy_system.common.transfer.CheckedFileTransferOutgoing;
import com.mo.economy_system.common.transfer.CheckedFileTransferLayout;
import com.mo.economy_system.common.transfer.CheckedFileTransferReceivedArtifact;
import com.mo.economy_system.common.transfer.CheckedFileTransferUiText;
import com.mo.economy_system.target.forge1201.client.Forge1201ClientFileCheckClientRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class Forge1201CheckedFileTransferClient {
  private Forge1201CheckedFileTransferClient() {}

  static void request(
      CheckedFileTransferRequestMessage message,
      ClientFileCheckTaskCoordinator.Session arrivalSession) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null || minecraft.getConnection() == null
        || !minecraft.player.getUUID().equals(message.targetPlayerId())) return;
    var runtime = Forge1201ClientFileCheckClientRuntime.transfers();
    var requestResult = runtime.receiveRequest(message, arrivalSession);
    if (requestResult == CheckedFileTransferClientCoordinator.RequestResult.IGNORED_STALE_SESSION
        || requestResult == CheckedFileTransferClientCoordinator.RequestResult.INVALID
        || requestResult == CheckedFileTransferClientCoordinator.RequestResult.CLOSED) return;
    ClientFileCheckTaskCoordinator.Session session = arrivalSession;
    var entry = Forge1201ClientFileCheckClientRuntime.manifest().find(
        new CheckedFileTransferManifestCache.Key(
            message.requesterPlayerId(), message.checkType(), message.fileName()),
        System.nanoTime());
    if (entry.isEmpty()) {
      runtime.cancelRequest(message, session);
      sendIfCurrent(session, CheckedFileTransferOutgoing.control(message,
          CheckedFileTransferControl.error(CheckedFileTransferControlStatus.FAILED, "STALE_CHECK")));
      return;
    }
    if (requestResult == CheckedFileTransferClientCoordinator.RequestResult.OPEN) {
      minecraft.setScreen(new Consent(message, entry.get(), session));
    } else if (requestResult == CheckedFileTransferClientCoordinator.RequestResult.CONSENT_BUSY) {
      sendIfCurrent(session, CheckedFileTransferOutgoing.control(message,
          CheckedFileTransferControl.error(CheckedFileTransferControlStatus.FAILED, "CONSENT_BUSY")));
    }
  }

  static void control(
      CheckedFileTransferControlResponseMessage message,
      ClientFileCheckTaskCoordinator.Session arrivalSession) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null || minecraft.getConnection() == null) return;
    var coordinator = Forge1201ClientFileCheckClientRuntime.transfers();
    var result = coordinator.control(
        message, arrivalSession, minecraft.gameDirectory.toPath(), System.nanoTime());
    if (result == CheckedFileTransferClientCoordinator.IncomingResult.COMPLETE
        && coordinator.completedArtifact() != null) {
      minecraft.setScreen(new Result(coordinator.completedArtifact()));
    }
    pollNotification();
  }

  static void chunk(
      CheckedFileTransferChunkResponseMessage message,
      ClientFileCheckTaskCoordinator.Session arrivalSession) {
    Forge1201ClientFileCheckClientRuntime.transfers().chunk(message, arrivalSession);
    pollNotification();
  }

  public static void pollNotification() {
    Minecraft minecraft = Minecraft.getInstance();
    var coordinator = Forge1201ClientFileCheckClientRuntime.transfers();
    if (coordinator.completedArtifact() != null) return;
    var notification = coordinator.pollTerminalNotification();
    if (notification != null) minecraft.setScreen(new Terminal(notification));
  }

  private static boolean current(ClientFileCheckTaskCoordinator.Session session) {
    if (session == null) return false;
    var current = Forge1201ClientFileCheckClientRuntime.transfers().currentSession();
    return current != null && current.generation() == session.generation()
        && current.connectionIdentity() == session.connectionIdentity()
        && current.localPlayerId().equals(session.localPlayerId());
  }

  private static void sendIfCurrent(ClientFileCheckTaskCoordinator.Session session, Object message) {
    if (!current(session)) return;
    if (message instanceof CheckedFileTransferControlRequestMessage control) {
      Forge1201NetworkChannel.sendToServer(control);
    } else if (message instanceof CheckedFileTransferChunkRequestMessage chunk) {
      Forge1201NetworkChannel.sendToServer(chunk);
    }
  }

  private static final class Consent extends Screen {
    private final CheckedFileTransferRequestMessage request;
    private final CheckedFileTransferManifestCache.Entry entry;
    private final ClientFileCheckTaskCoordinator.Session session;
    private boolean finished;

    Consent(CheckedFileTransferRequestMessage request, CheckedFileTransferManifestCache.Entry entry,
            ClientFileCheckTaskCoordinator.Session session) {
      super(Component.translatable("screen.transfer_consent.title"));
      this.request = request; this.entry = entry; this.session = session;
    }

    @Override protected void init() {
      var actions = CheckedFileTransferLayout.twoActions(width, height);
      if (actions.primary() == null) return;
      addRenderableWidget(Button.builder(Component.translatable("button.transfer.allow"), b -> allow())
          .bounds(actions.primary().x(), actions.primary().y(), actions.primary().width(), actions.primary().height()).build());
      addRenderableWidget(Button.builder(Component.translatable("button.transfer.decline"), b -> decline())
          .bounds(actions.secondary().x(), actions.secondary().y(), actions.secondary().width(), actions.secondary().height()).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      renderBackground(graphics); super.render(graphics, mouseX, mouseY, partialTick);
      graphics.drawCenteredString(font, title, width / 2,
          Math.min(18, Math.max(0, height - font.lineHeight)), 0xffffff);
      var actions = CheckedFileTransferLayout.twoActions(width, height);
      int rows = CheckedFileTransferLayout.visibleRows(
          width, height, 64, 38, 12, 6, actions.primary());
      int maxCharacters = Math.max(4, (width - 8) / 6);
      if (rows >= 1) graphics.drawString(font, Component.translatable(
          "screen.transfer_consent.requester", request.requesterPlayerName()), 4, 38, 0xdddddd);
      if (rows >= 2) graphics.drawString(font, Component.translatable(
          "screen.transfer_consent.type", request.checkType().id()), 4, 50, 0xdddddd);
      if (rows >= 3) graphics.drawString(font, Component.translatable(
          "screen.transfer_consent.file",
          CheckedFileTransferLayout.truncate(request.fileName(), maxCharacters)), 4, 62, 0xdddddd);
      if (rows >= 4) graphics.drawString(font, Component.translatable(
          "screen.transfer_consent.size", entry.size()), 4, 74, 0xdddddd);
      if (rows >= 5) graphics.drawString(font, Component.translatable(
          "screen.transfer_consent.hash",
          CheckedFileTransferLayout.truncate(entry.sha256(), maxCharacters)), 4, 86, 0xdddddd);
      if (rows >= 6) graphics.drawString(font,
          Component.translatable("screen.transfer_consent.warning"), 4, 98, 0xccaa66);
    }

    private void allow() {
      if (finished || !current(session)) return;
      finished = true;
      Minecraft minecraft = Minecraft.getInstance();
      var coordinator = Forge1201ClientFileCheckClientRuntime.transfers();
      coordinator.outgoing().allow(request, session,
          deadline -> CheckedFileSnapshotter.create(minecraft.gameDirectory.toPath(), request.checkType(),
              request.fileName(), entry.size(), entry.sha256(),
              coordinator.temporaryDirectory(minecraft.gameDirectory.toPath()),
              deadline, coordinator.tempBudget()),
          (activeSession, token, outgoing) -> sendIfCurrent(activeSession, outgoing));
      minecraft.setScreen(null);
    }

    private void decline() {
      if (finished) return;
      finished = true;
      Forge1201ClientFileCheckClientRuntime.transfers().outgoing().decline(
          request, session, (activeSession, token, outgoing) -> sendIfCurrent(activeSession, outgoing));
      Minecraft.getInstance().setScreen(null);
    }

    @Override public void tick() {
      if (finished) return;
      var coordinator = Forge1201ClientFileCheckClientRuntime.transfers();
      var active = coordinator.outgoing().active();
      if (!current(session) || active == null || !active.request().equals(request)
          || active.state() != CheckedFileTransferOutgoing.State.CONSENT
          || System.nanoTime() >= active.deadlineNanos()) {
        finished = true;
        coordinator.publishRequestExpired(request, session, System.nanoTime());
        Minecraft.getInstance().setScreen(null);
        pollNotification();
      }
    }

    @Override public void onClose() { decline(); }
    @Override public void removed() {
      if (!finished) decline();
    }
  }

  private static final class Result extends Screen {
    private final CheckedFileTransferReceivedArtifact expectedArtifact;
    private String errorKey;
    Result(CheckedFileTransferReceivedArtifact expectedArtifact) {
      super(Component.translatable("screen.transfer_result.title"));
      this.expectedArtifact = expectedArtifact;
    }
    @Override protected void init() {
      var actions = CheckedFileTransferLayout.twoActions(width, height);
      if (actions.primary() == null) return;
      addRenderableWidget(Button.builder(Component.translatable("button.transfer.save"), b -> save())
          .bounds(actions.primary().x(), actions.primary().y(), actions.primary().width(), actions.primary().height()).build());
      addRenderableWidget(Button.builder(Component.translatable("button.transfer.discard"), b -> discard())
          .bounds(actions.secondary().x(), actions.secondary().y(), actions.secondary().width(), actions.secondary().height()).build());
    }
    @Override public void tick() {
      if (Forge1201ClientFileCheckClientRuntime.transfers().completedArtifact()
          != expectedArtifact) Minecraft.getInstance().setScreen(null);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      renderBackground(graphics); super.render(graphics, mouseX, mouseY, partialTick);
      var artifact = Forge1201ClientFileCheckClientRuntime.transfers().completedArtifact();
      if (artifact == null || artifact != expectedArtifact) return;
      var metadata = artifact.metadata();
      graphics.drawCenteredString(font, title, width / 2,
          Math.min(18, Math.max(0, height - font.lineHeight)), 0xffffff);
      var actions = CheckedFileTransferLayout.twoActions(width, height);
      int rows = CheckedFileTransferLayout.visibleRows(
          width, height, 64, 38, 12, 7, actions.primary());
      int maxCharacters = Math.max(4, (width - 8) / 6);
      if (rows >= 1) graphics.drawString(font, Component.translatable(
          "screen.transfer_result.source", metadata.targetPlayerName()), 4, 38, 0xdddddd);
      if (rows >= 2) graphics.drawString(font, Component.translatable(
          "screen.transfer_result.type", metadata.checkType().id()), 4, 50, 0xdddddd);
      if (rows >= 3) graphics.drawString(font, Component.translatable(
          "screen.transfer_result.file",
          CheckedFileTransferLayout.truncate(metadata.fileName(), maxCharacters)), 4, 62, 0xdddddd);
      if (rows >= 4) graphics.drawString(font, Component.translatable(
          "screen.transfer_result.size", metadata.byteLength()), 4, 74, 0xdddddd);
      if (rows >= 5) graphics.drawString(font, Component.translatable(
          "screen.transfer_result.hash",
          CheckedFileTransferLayout.truncate(metadata.sha256(), maxCharacters)), 4, 86, 0xdddddd);
      if (rows >= 6) graphics.drawString(font, Component.translatable(
          "screen.transfer_result.state",
          Component.translatable(CheckedFileTransferUiText.artifactStateKey(artifact.state()))),
          4, 98, 0xdddddd);
      if (rows >= 7 && errorKey != null) {
        graphics.drawString(font, Component.translatable(errorKey), 4, 110, 0xff7777);
      }
    }
    private void save() {
      var coordinator = Forge1201ClientFileCheckClientRuntime.transfers();
      if (coordinator.completedArtifact() != expectedArtifact) {
        Minecraft.getInstance().setScreen(null);
        return;
      }
      var result = coordinator.saveCompleted(Minecraft.getInstance().gameDirectory.toPath());
      if (result.success()) Minecraft.getInstance().setScreen(null);
      else errorKey = CheckedFileTransferUiText.saveErrorKey(result.code());
    }
    private void discard() {
      var coordinator = Forge1201ClientFileCheckClientRuntime.transfers();
      if (coordinator.completedArtifact() != expectedArtifact) {
        Minecraft.getInstance().setScreen(null);
        return;
      }
      var result = coordinator.discardCompleted();
      if (result.success()) Minecraft.getInstance().setScreen(null);
      else errorKey = CheckedFileTransferUiText.discardErrorKey(result.code());
    }
    @Override public void onClose() { discard(); }
    @Override public void removed() { }
  }

  private static final class Terminal extends Screen {
    private final CheckedFileTransferClientCoordinator.TerminalResult terminal;

    Terminal(CheckedFileTransferClientCoordinator.TerminalResult terminal) {
      super(Component.translatable("screen.transfer_terminal.title"));
      this.terminal = terminal;
    }

    @Override protected void init() {
      var close = CheckedFileTransferLayout.closeAction(width, height);
      if (close == null) return;
      addRenderableWidget(Button.builder(Component.translatable("button.transfer.close"), b -> onClose())
          .bounds(close.x(), close.y(), close.width(), close.height()).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      renderBackground(graphics); super.render(graphics, mouseX, mouseY, partialTick);
      graphics.drawCenteredString(font, title, width / 2,
          Math.min(18, Math.max(0, height - font.lineHeight)), 0xffffff);
      var close = CheckedFileTransferLayout.closeAction(width, height);
      int rows = CheckedFileTransferLayout.visibleRows(width, height, 64, 38, 12, 2, close);
      if (rows >= 1) graphics.drawString(font, Component.translatable(
          "screen.transfer_terminal.status",
          Component.translatable(CheckedFileTransferUiText.terminalStatusKey(terminal.status()))),
          4, 38, 0xff7777);
      if (rows >= 2) graphics.drawString(font, Component.translatable(
          "screen.transfer_terminal.reason",
          Component.translatable(CheckedFileTransferUiText.errorKey(terminal.errorCode()))),
          4, 50, 0xff7777);
    }

    @Override public void onClose() { Minecraft.getInstance().setScreen(null); }
  }
}

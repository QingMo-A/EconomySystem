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
import com.mo.economy_system.common.transfer.CheckedFileTransferReceivedArtifact;
import com.mo.economy_system.common.transfer.CheckedFileTransferSaveService;
import com.mo.economy_system.target.forge1201.client.Forge1201ClientFileCheckClientRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class Forge1201CheckedFileTransferClient {
  private Forge1201CheckedFileTransferClient() {}

  static void request(CheckedFileTransferRequestMessage message) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null || minecraft.getConnection() == null
        || !minecraft.player.getUUID().equals(message.targetPlayerId())) return;
    var runtime = Forge1201ClientFileCheckClientRuntime.transfers();
    ClientFileCheckTaskCoordinator.Session session =
        Forge1201ClientFileCheckClientRuntime.currentOrBegin(
            minecraft.getConnection(), minecraft.player.getUUID());
    var entry = Forge1201ClientFileCheckClientRuntime.manifest().find(
        new CheckedFileTransferManifestCache.Key(
            message.requesterPlayerId(), message.checkType(), message.fileName()),
        System.nanoTime());
    if (entry.isEmpty()) {
      sendIfCurrent(session, CheckedFileTransferOutgoing.control(message,
          CheckedFileTransferControl.error(CheckedFileTransferControlStatus.FAILED, "STALE_CHECK")));
      return;
    }
    var result = runtime.outgoing().receive(message, session);
    if (result == CheckedFileTransferOutgoing.BeginResult.OPEN) {
      minecraft.setScreen(new Consent(message, entry.get(), session));
    } else if (result == CheckedFileTransferOutgoing.BeginResult.CONSENT_BUSY) {
      sendIfCurrent(session, CheckedFileTransferOutgoing.control(message,
          CheckedFileTransferControl.error(CheckedFileTransferControlStatus.FAILED, "CONSENT_BUSY")));
    }
  }

  static void control(CheckedFileTransferControlResponseMessage message) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null || minecraft.getConnection() == null) return;
    var coordinator = Forge1201ClientFileCheckClientRuntime.transfers();
    var result = coordinator.control(message,
        minecraft.gameDirectory.toPath().resolve("economy_system").resolve("transfer-temp"),
        System.nanoTime());
    if (result == CheckedFileTransferClientCoordinator.IncomingResult.COMPLETE
        && coordinator.completedArtifact() != null) {
      minecraft.setScreen(new Result(coordinator.completedArtifact()));
    }
  }

  static void chunk(CheckedFileTransferChunkResponseMessage message) {
    Forge1201ClientFileCheckClientRuntime.transfers().chunk(message);
  }

  private static boolean current(ClientFileCheckTaskCoordinator.Session session) {
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
      if (width < 32 || height < 55) return;
      int buttonWidth = Math.min(100, Math.max(1, (width - 15) / 2));
      int total = buttonWidth * 2 + 5;
      int left = Math.max(0, (width - total) / 2);
      int y = Math.max(0, height - 25);
      addRenderableWidget(Button.builder(Component.translatable("button.transfer.allow"), b -> allow())
          .bounds(left, y, buttonWidth, Math.min(20, height - y)).build());
      addRenderableWidget(Button.builder(Component.translatable("button.transfer.decline"), b -> decline())
          .bounds(left + buttonWidth + 5, y, buttonWidth, Math.min(20, height - y)).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      renderBackground(graphics); super.render(graphics, mouseX, mouseY, partialTick);
      graphics.drawCenteredString(font, title, width / 2, Math.min(18, Math.max(0, height - 1)), 0xffffff);
      if (width >= 64 && height >= 110) {
        graphics.drawString(font, Component.translatable("screen.transfer_consent.requester", request.requesterPlayerName()), 4, 42, 0xdddddd);
        graphics.drawString(font, Component.translatable("screen.transfer_consent.type", request.checkType().id()), 4, 54, 0xdddddd);
        graphics.drawString(font, Component.translatable("screen.transfer_consent.file", request.fileName()), 4, 66, 0xdddddd);
        graphics.drawString(font, Component.translatable("screen.transfer_consent.size", entry.size()), 4, 78, 0xdddddd);
        graphics.drawString(font, Component.translatable("screen.transfer_consent.hash", entry.sha256()), 4, 90, 0xdddddd);
        graphics.drawString(font, Component.translatable("screen.transfer_consent.warning"), 4, 102, 0xccaa66);
      }
    }

    private void allow() {
      if (finished || !current(session)) return;
      finished = true;
      Minecraft minecraft = Minecraft.getInstance();
      var coordinator = Forge1201ClientFileCheckClientRuntime.transfers();
      coordinator.outgoing().allow(request, session,
          deadline -> CheckedFileSnapshotter.create(minecraft.gameDirectory.toPath(), request.checkType(),
              request.fileName(), entry.size(), entry.sha256(),
              minecraft.gameDirectory.toPath().resolve("economy_system").resolve("transfer-temp"),
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

    @Override public void onClose() { decline(); }
    @Override public void removed() {
      if (!finished) decline();
    }
  }

  private static final class Result extends Screen {
    private final CheckedFileTransferReceivedArtifact artifact;
    private String errorKey;
    Result(CheckedFileTransferReceivedArtifact artifact) {
      super(Component.translatable("screen.transfer_result.title")); this.artifact = artifact;
    }
    @Override protected void init() {
      if (width < 32 || height < 55) return;
      int buttonWidth = Math.min(100, Math.max(1, (width - 15) / 2));
      int total = buttonWidth * 2 + 5; int left = Math.max(0, (width - total) / 2);
      int y = Math.max(0, height - 25);
      addRenderableWidget(Button.builder(Component.translatable("button.transfer.save"), b -> save())
          .bounds(left, y, buttonWidth, Math.min(20, height - y)).build());
      addRenderableWidget(Button.builder(Component.translatable("button.transfer.discard"), b -> discard())
          .bounds(left + buttonWidth + 5, y, buttonWidth, Math.min(20, height - y)).build());
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      renderBackground(graphics); super.render(graphics, mouseX, mouseY, partialTick);
      var metadata = artifact.metadata();
      graphics.drawCenteredString(font, title, width / 2, Math.min(18, Math.max(0, height - 1)), 0xffffff);
      if (width >= 64 && height >= 90) {
        graphics.drawString(font, Component.translatable("screen.transfer_result.source", metadata.targetPlayerName()), 4, 42, 0xdddddd);
        graphics.drawString(font, Component.translatable("screen.transfer_result.file", metadata.fileName()), 4, 54, 0xdddddd);
        graphics.drawString(font, Component.translatable("screen.transfer_result.size", metadata.byteLength()), 4, 66, 0xdddddd);
        if (errorKey != null) graphics.drawString(font, Component.translatable(errorKey), 4, 78, 0xff7777);
      }
    }
    private void save() {
      var result = new CheckedFileTransferSaveService(Minecraft.getInstance().gameDirectory.toPath()).save(artifact);
      if (result.success()) Minecraft.getInstance().setScreen(null);
      else errorKey = result.code() == CheckedFileTransferSaveService.ResultCode.SAVE_NAME_EXHAUSTED
          ? "message.transfer.save_name_exhausted" : "message.transfer.save_parent_unsafe";
    }
    private void discard() { artifact.discard(); Minecraft.getInstance().setScreen(null); }
    @Override public void onClose() { discard(); }
    @Override public void removed() { if (artifact.isPendingDecision()) artifact.discard(); }
  }
}

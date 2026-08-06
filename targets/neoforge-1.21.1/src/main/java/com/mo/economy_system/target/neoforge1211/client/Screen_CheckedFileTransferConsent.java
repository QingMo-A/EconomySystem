package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.check.ClientFileCheckTaskCoordinator;
import com.mo.economy_system.common.network.CheckedFileTransferRequestMessage;
import com.mo.economy_system.common.transfer.CheckedFileSnapshotter;
import com.mo.economy_system.common.transfer.CheckedFileTransferLayout;
import com.mo.economy_system.common.transfer.CheckedFileTransferManifestCache;
import com.mo.economy_system.common.transfer.CheckedFileTransferOutgoing;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class Screen_CheckedFileTransferConsent extends Screen {
  private final CheckedFileTransferRequestMessage request;
  private final CheckedFileTransferManifestCache.Entry entry;
  private final ClientFileCheckTaskCoordinator.Session session;
  private boolean finished;

  Screen_CheckedFileTransferConsent(CheckedFileTransferRequestMessage request,
                                    CheckedFileTransferManifestCache.Entry entry,
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
    super.render(graphics, mouseX, mouseY, partialTick);
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
    if (finished || !current()) return;
    finished = true;
    Minecraft minecraft = Minecraft.getInstance();
      var coordinator = NeoForge1211ClientFileCheckClientRuntime.transfers();
      coordinator.outgoing().allow(request, session,
          deadline -> CheckedFileSnapshotter.create(minecraft.gameDirectory.toPath(), request.checkType(),
              request.fileName(), entry.size(), entry.sha256(),
              coordinator.temporaryDirectory(minecraft.gameDirectory.toPath()),
              deadline, coordinator.tempBudget()),
        (activeSession, token, outgoing) -> send(activeSession, outgoing));
    minecraft.setScreen(null);
  }

  private void decline() {
    if (finished) return;
    finished = true;
    NeoForge1211ClientFileCheckClientRuntime.transfers().outgoing().decline(
        request, session, (activeSession, token, outgoing) -> send(activeSession, outgoing));
    Minecraft.getInstance().setScreen(null);
  }

  private boolean current() {
    var active = NeoForge1211ClientFileCheckClientRuntime.transfers().currentSession();
    return active != null && active.generation() == session.generation()
        && active.connectionIdentity() == session.connectionIdentity()
        && active.localPlayerId().equals(session.localPlayerId());
  }
  private static void send(ClientFileCheckTaskCoordinator.Session session, Object message) {
    var active = NeoForge1211ClientFileCheckClientRuntime.transfers().currentSession();
    if (active == null || active.generation() != session.generation()
        || active.connectionIdentity() != session.connectionIdentity()
        || !active.localPlayerId().equals(session.localPlayerId())) return;
    EconomySystem_NetworkManager.sendToServer((EconomyNetworkMessage) message);
  }
  @Override public void onClose() { decline(); }
  @Override public void removed() { if (!finished) decline(); }
}

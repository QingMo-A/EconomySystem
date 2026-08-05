package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.check.ClientFileCheckTaskCoordinator;
import com.mo.economy_system.common.network.CheckedFileTransferRequestMessage;
import com.mo.economy_system.common.transfer.CheckedFileSnapshotter;
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
    if (width < 32 || height < 55) return;
    int buttonWidth = Math.min(100, Math.max(1, (width - 15) / 2));
    int total = buttonWidth * 2 + 5; int left = Math.max(0, (width - total) / 2);
    int y = Math.max(0, height - 25);
    addRenderableWidget(Button.builder(Component.translatable("button.transfer.allow"), b -> allow())
        .bounds(left, y, buttonWidth, Math.min(20, height - y)).build());
    addRenderableWidget(Button.builder(Component.translatable("button.transfer.decline"), b -> decline())
        .bounds(left + buttonWidth + 5, y, buttonWidth, Math.min(20, height - y)).build());
  }

  @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    super.render(graphics, mouseX, mouseY, partialTick);
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
    if (finished || !current()) return;
    finished = true;
    Minecraft minecraft = Minecraft.getInstance();
    var coordinator = NeoForge1211ClientFileCheckClientRuntime.transfers();
    coordinator.outgoing().allow(request, session,
        deadline -> CheckedFileSnapshotter.create(minecraft.gameDirectory.toPath(), request.checkType(),
            request.fileName(), entry.size(), entry.sha256(),
            minecraft.gameDirectory.toPath().resolve("economy_system").resolve("transfer-temp"),
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

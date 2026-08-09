package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.check.ClientFileCheckTaskCoordinator;
import com.mo.economy_system.common.network.CheckedFileTransferRequestMessage;
import com.mo.economy_system.common.transfer.CheckedFileSnapshotter;
import com.mo.economy_system.common.transfer.CheckedFileTransferManifestCache;
import com.mo.economy_system.common.transfer.CheckedFileTransferOutgoing;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.transfer.TransferConsentAction;
import com.mo.economy_system.ui.transfer.TransferConsentController;
import com.mo.economy_system.ui.transfer.TransferConsentEvent;
import com.mo.economy_system.ui.transfer.TransferConsentLayout;
import com.mo.economy_system.ui.transfer.TransferConsentPort;
import com.mo.economy_system.ui.transfer.TransferConsentView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** NeoForge shell for the common checked-file transfer consent page. */
final class Screen_CheckedFileTransferConsent extends Screen {
  private final CheckedFileTransferRequestMessage request;
  private final CheckedFileTransferManifestCache.Entry entry;
  private final ClientFileCheckTaskCoordinator.Session session;
  private final TransferConsentController controller;
  private boolean finished;

  Screen_CheckedFileTransferConsent(
      CheckedFileTransferRequestMessage request,
      CheckedFileTransferManifestCache.Entry entry,
      ClientFileCheckTaskCoordinator.Session session) {
    super(Component.translatable("screen.transfer_consent.title"));
    this.request = request;
    this.entry = entry;
    this.session = session;
    controller = new TransferConsentController(
        request.requesterPlayerName(),
        request.checkType().id(),
        request.fileName(),
        entry.size(),
        entry.sha256(),
        new Port());
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    TransferConsentLayout.Layout layout = TransferConsentLayout.calculate(width, height);
    UiScale scale = layout.scale();
    graphics.pose().pushPose();
    graphics.pose().scale(scale.value(), scale.value(), 1.0f);
    TransferConsentView.render(
        new NeoForge1211UiRenderer(graphics, font),
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
    TransferConsentLayout.Layout layout = TransferConsentLayout.calculate(width, height);
    int x = layout.scale().toVirtualX(mouseX);
    int y = layout.scale().toVirtualY(mouseY);
    if (layout.allow().contains(x, y)) {
      controller.handle(new TransferConsentEvent.ActionClicked(TransferConsentAction.ALLOW));
      return true;
    }
    if (layout.decline().contains(x, y)) {
      controller.handle(new TransferConsentEvent.ActionClicked(TransferConsentAction.DECLINE));
      return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  public void tick() {
    if (finished) return;
    var coordinator = NeoForge1211ClientFileCheckClientRuntime.transfers();
    var active = coordinator.outgoing().active();
    if (!current()
        || active == null
        || !active.request().equals(request)
        || active.state() != CheckedFileTransferOutgoing.State.CONSENT
        || System.nanoTime() >= active.deadlineNanos()) {
      controller.handle(new TransferConsentEvent.Expired());
    }
  }

  @Override
  public void onClose() {
    controller.handle(new TransferConsentEvent.ActionClicked(TransferConsentAction.DECLINE));
  }

  @Override
  public void removed() {
    if (!finished) controller.handle(new TransferConsentEvent.ActionClicked(TransferConsentAction.DECLINE));
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

  private void allow() {
    if (finished) return;
    if (!current()) {
      finished = true;
      Minecraft.getInstance().setScreen(null);
      return;
    }
    finished = true;
    Minecraft minecraft = Minecraft.getInstance();
    var coordinator = NeoForge1211ClientFileCheckClientRuntime.transfers();
    coordinator.outgoing().allow(
        request,
        session,
        deadline ->
            CheckedFileSnapshotter.create(
                minecraft.gameDirectory.toPath(),
                request.checkType(),
                request.fileName(),
                entry.size(),
                entry.sha256(),
                coordinator.temporaryDirectory(minecraft.gameDirectory.toPath()),
                deadline,
                coordinator.tempBudget()),
        (activeSession, token, outgoing) -> send(activeSession, outgoing));
    minecraft.setScreen(null);
  }

  private void decline() {
    if (finished) return;
    if (!current()) {
      finished = true;
      Minecraft.getInstance().setScreen(null);
      return;
    }
    finished = true;
    NeoForge1211ClientFileCheckClientRuntime.transfers().outgoing().decline(
        request, session, (activeSession, token, outgoing) -> send(activeSession, outgoing));
    Minecraft.getInstance().setScreen(null);
  }

  private void expire() {
    if (finished) return;
    finished = true;
    NeoForge1211ClientFileCheckClientRuntime.transfers()
        .publishRequestExpired(request, session, System.nanoTime());
    Minecraft.getInstance().setScreen(null);
    CheckedFileTransferIncomingRuntime.pollNotification();
  }

  private boolean current() {
    var active = NeoForge1211ClientFileCheckClientRuntime.transfers().currentSession();
    return active != null
        && active.generation() == session.generation()
        && active.connectionIdentity() == session.connectionIdentity()
        && active.localPlayerId().equals(session.localPlayerId());
  }

  private static void send(ClientFileCheckTaskCoordinator.Session session, Object message) {
    var active = NeoForge1211ClientFileCheckClientRuntime.transfers().currentSession();
    if (active == null
        || active.generation() != session.generation()
        || active.connectionIdentity() != session.connectionIdentity()
        || !active.localPlayerId().equals(session.localPlayerId())) {
      return;
    }
    EconomySystem_NetworkManager.sendToServer((EconomyNetworkMessage) message);
  }

  private final class Port implements TransferConsentPort {
    @Override
    public void allow() {
      Screen_CheckedFileTransferConsent.this.allow();
    }

    @Override
    public void decline() {
      Screen_CheckedFileTransferConsent.this.decline();
    }

    @Override
    public void expire() {
      Screen_CheckedFileTransferConsent.this.expire();
    }
  }
}

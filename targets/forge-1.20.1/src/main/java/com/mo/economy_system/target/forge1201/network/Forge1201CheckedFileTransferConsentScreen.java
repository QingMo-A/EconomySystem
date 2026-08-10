package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.check.ClientFileCheckTaskCoordinator;
import com.mo.economy_system.common.network.CheckedFileTransferRequestMessage;
import com.mo.economy_system.common.transfer.CheckedFileSnapshotter;
import com.mo.economy_system.common.transfer.CheckedFileTransferManifestCache;
import com.mo.economy_system.common.transfer.CheckedFileTransferOutgoing;
import com.mo.economy_system.target.forge1201.client.Forge1201ClientFileCheckClientRuntime;
import com.mo.economy_system.target.forge1201.client.Forge1201UiRenderer;
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

/** Forge shell for the common checked-file transfer consent page. */
final class Forge1201CheckedFileTransferConsentScreen extends Screen {
  private final CheckedFileTransferRequestMessage request;
  private final CheckedFileTransferManifestCache.Entry entry;
  private final ClientFileCheckTaskCoordinator.Session session;
  private final TransferConsentController controller;
  private boolean finished;

  Forge1201CheckedFileTransferConsentScreen(
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
    Forge1201UiRenderer renderer = new Forge1201UiRenderer(graphics, font);
    renderer.fillPhysicalBackground(width, height, TransferConsentLayout.BACKGROUND_COLOR);
    graphics.pose().pushPose();
    graphics.pose().scale(scale.value(), scale.value(), 1.0f);
    TransferConsentView.render(
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
    var coordinator = Forge1201ClientFileCheckClientRuntime.transfers();
    var active = coordinator.outgoing().active();
    if (!Forge1201CheckedFileTransferClient.current(session)
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

  private void allow() {
    if (finished) return;
    if (!Forge1201CheckedFileTransferClient.current(session)) {
      finished = true;
      Minecraft.getInstance().setScreen(null);
      return;
    }
    finished = true;
    Minecraft minecraft = Minecraft.getInstance();
    var coordinator = Forge1201ClientFileCheckClientRuntime.transfers();
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
        (activeSession, token, outgoing) ->
            Forge1201CheckedFileTransferClient.sendIfCurrent(activeSession, outgoing));
    minecraft.setScreen(null);
  }

  private void decline() {
    if (finished) return;
    if (!Forge1201CheckedFileTransferClient.current(session)) {
      finished = true;
      Minecraft.getInstance().setScreen(null);
      return;
    }
    finished = true;
    Forge1201ClientFileCheckClientRuntime.transfers().outgoing().decline(
        request,
        session,
        (activeSession, token, outgoing) ->
            Forge1201CheckedFileTransferClient.sendIfCurrent(activeSession, outgoing));
    Minecraft.getInstance().setScreen(null);
  }

  private void expire() {
    if (finished) return;
    finished = true;
    Forge1201ClientFileCheckClientRuntime.transfers()
        .publishRequestExpired(request, session, System.nanoTime());
    Minecraft.getInstance().setScreen(null);
    Forge1201CheckedFileTransferClient.pollNotification();
  }

  private final class Port implements TransferConsentPort {
    @Override
    public void allow() {
      Forge1201CheckedFileTransferConsentScreen.this.allow();
    }

    @Override
    public void decline() {
      Forge1201CheckedFileTransferConsentScreen.this.decline();
    }

    @Override
    public void expire() {
      Forge1201CheckedFileTransferConsentScreen.this.expire();
    }
  }
}

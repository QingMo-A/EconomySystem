package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.transfer.CheckedFileTransferClientCoordinator;
import com.mo.economy_system.common.transfer.CheckedFileTransferReceivedArtifact;
import com.mo.economy_system.common.transfer.CheckedFileTransferUiText;
import com.mo.economy_system.target.forge1201.client.Forge1201ClientFileCheckClientRuntime;
import com.mo.economy_system.target.forge1201.client.Forge1201UiRenderer;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.transfer.TransferResultAction;
import com.mo.economy_system.ui.transfer.TransferResultController;
import com.mo.economy_system.ui.transfer.TransferResultEvent;
import com.mo.economy_system.ui.transfer.TransferResultLayout;
import com.mo.economy_system.ui.transfer.TransferResultPort;
import com.mo.economy_system.ui.transfer.TransferResultState;
import com.mo.economy_system.ui.transfer.TransferResultView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Forge shell for the common received-artifact and terminal transfer views. */
final class Forge1201CheckedFileTransferResultScreen extends Screen {
  private final CheckedFileTransferReceivedArtifact initialArtifact;
  private final CheckedFileTransferClientCoordinator.TerminalResult terminal;
  private final TransferResultController controller;

  Forge1201CheckedFileTransferResultScreen(CheckedFileTransferReceivedArtifact artifact) {
    super(Component.translatable("screen.transfer_result.title"));
    initialArtifact = artifact;
    terminal = null;
    var metadata = artifact.metadata();
    controller = new TransferResultController(
        TransferResultState.artifact(
            metadata.targetPlayerName(),
            metadata.checkType().id(),
            metadata.fileName(),
            metadata.byteLength(),
            metadata.sha256(),
            CheckedFileTransferUiText.artifactStateKey(artifact.state())),
        new Port());
  }

  Forge1201CheckedFileTransferResultScreen(CheckedFileTransferClientCoordinator.TerminalResult terminal) {
    super(Component.translatable("screen.transfer_terminal.title"));
    initialArtifact = null;
    this.terminal = terminal;
    controller = new TransferResultController(
        TransferResultState.terminal(
            CheckedFileTransferUiText.terminalStatusKey(terminal.status()),
            CheckedFileTransferUiText.errorKey(terminal.errorCode())),
        new Port());
  }

  @Override
  public void tick() {
    if (terminal != null) return;
    var artifact = Forge1201ClientFileCheckClientRuntime.transfers().completedArtifact();
    if (artifact != initialArtifact) {
      controller.handle(new TransferResultEvent.ArtifactNoLongerCurrent());
      return;
    }
    controller.handle(
        new TransferResultEvent.ArtifactStateChanged(
            CheckedFileTransferUiText.artifactStateKey(artifact.state())));
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    TransferResultLayout.Layout layout = TransferResultLayout.calculate(width, height, controller.state());
    UiScale scale = layout.scale();
    Forge1201UiRenderer renderer = new Forge1201UiRenderer(graphics, font);
    renderer.fillPhysicalBackground(width, height, TransferResultLayout.BACKGROUND_COLOR);
    graphics.pose().pushPose();
    graphics.pose().scale(scale.value(), scale.value(), 1.0f);
    TransferResultView.render(
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
    TransferResultLayout.Layout layout = TransferResultLayout.calculate(width, height, controller.state());
    int x = layout.scale().toVirtualX(mouseX);
    int y = layout.scale().toVirtualY(mouseY);
    if (terminal != null && layout.close().contains(x, y)) {
      controller.handle(new TransferResultEvent.ActionClicked(TransferResultAction.CLOSE));
      return true;
    }
    if (terminal == null && layout.primary().contains(x, y)) {
      controller.handle(new TransferResultEvent.ActionClicked(TransferResultAction.SAVE));
      return true;
    }
    if (terminal == null && layout.secondary().contains(x, y)) {
      controller.handle(new TransferResultEvent.ActionClicked(TransferResultAction.DISCARD));
      return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  public void onClose() {
    controller.handle(
        new TransferResultEvent.ActionClicked(
            terminal == null ? TransferResultAction.DISCARD : TransferResultAction.CLOSE));
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  private final class Port implements TransferResultPort {
    @Override
    public Outcome save() {
      var coordinator = Forge1201ClientFileCheckClientRuntime.transfers();
      if (coordinator.completedArtifact() != initialArtifact) return Outcome.closed();
      var result = coordinator.saveCompleted(Minecraft.getInstance().gameDirectory.toPath());
      return result.success()
          ? Outcome.closed()
          : Outcome.failed(CheckedFileTransferUiText.saveErrorKey(result.code()));
    }

    @Override
    public Outcome discard() {
      var coordinator = Forge1201ClientFileCheckClientRuntime.transfers();
      if (coordinator.completedArtifact() != initialArtifact) return Outcome.closed();
      var result = coordinator.discardCompleted();
      return result.success()
          ? Outcome.closed()
          : Outcome.failed(CheckedFileTransferUiText.discardErrorKey(result.code()));
    }

    @Override
    public void close() {
      Minecraft.getInstance().setScreen(null);
    }
  }
}

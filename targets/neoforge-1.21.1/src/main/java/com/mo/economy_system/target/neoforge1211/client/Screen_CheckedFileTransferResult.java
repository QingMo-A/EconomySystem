package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.transfer.CheckedFileTransferClientCoordinator;
import com.mo.economy_system.common.transfer.CheckedFileTransferLayout;
import com.mo.economy_system.common.transfer.CheckedFileTransferReceivedArtifact;
import com.mo.economy_system.common.transfer.CheckedFileTransferUiText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Result UI whose only artifact mutations go through the common coordinator. */
final class Screen_CheckedFileTransferResult extends Screen {
  private final CheckedFileTransferReceivedArtifact initialArtifact;
  private final CheckedFileTransferClientCoordinator.TerminalResult terminal;
  private String errorKey;

  Screen_CheckedFileTransferResult(CheckedFileTransferReceivedArtifact artifact) {
    super(Component.translatable("screen.transfer_result.title"));
    initialArtifact = artifact;
    terminal = null;
  }

  Screen_CheckedFileTransferResult(CheckedFileTransferClientCoordinator.TerminalResult terminal) {
    super(Component.translatable("screen.transfer_terminal.title"));
    initialArtifact = null;
    this.terminal = terminal;
  }

  @Override
  protected void init() {
    if (terminal == null) {
      var actions = CheckedFileTransferLayout.twoActions(width, height);
      if (actions.primary() == null) return;
      addRenderableWidget(
          Button.builder(Component.translatable("button.transfer.save"), b -> save())
              .bounds(actions.primary().x(), actions.primary().y(), actions.primary().width(), actions.primary().height())
              .build());
      addRenderableWidget(
          Button.builder(Component.translatable("button.transfer.discard"), b -> discard())
              .bounds(actions.secondary().x(), actions.secondary().y(), actions.secondary().width(), actions.secondary().height())
              .build());
    } else {
      var close = CheckedFileTransferLayout.closeAction(width, height);
      if (close == null) return;
      addRenderableWidget(
          Button.builder(Component.translatable("button.transfer.close"), b -> onClose())
              .bounds(close.x(), close.y(), close.width(), close.height())
              .build());
    }
  }

  @Override public void tick() {
    if (terminal == null && NeoForge1211ClientFileCheckClientRuntime.transfers().completedArtifact()
        != initialArtifact) Minecraft.getInstance().setScreen(null);
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    renderBackground(graphics, mouseX, mouseY, partialTick);
    super.render(graphics, mouseX, mouseY, partialTick);
    graphics.drawCenteredString(
        font, title, width / 2, Math.min(18, Math.max(0, height - font.lineHeight)), 0xffffff);
    var actions = CheckedFileTransferLayout.twoActions(width, height);
    var close = CheckedFileTransferLayout.closeAction(width, height);
    int rows = terminal == null
        ? CheckedFileTransferLayout.visibleRows(width, height, 64, 38, 12, 7, actions.primary())
        : CheckedFileTransferLayout.visibleRows(width, height, 64, 38, 12, 2, close);
    if (terminal != null) {
      if (rows >= 1) graphics.drawString(
          font,
          Component.translatable(
              "screen.transfer_terminal.status",
              Component.translatable(CheckedFileTransferUiText.terminalStatusKey(terminal.status()))),
          4, 38, 0xff7777);
      if (rows >= 2) graphics.drawString(
          font,
          Component.translatable(
              "screen.transfer_terminal.reason",
              Component.translatable(CheckedFileTransferUiText.errorKey(terminal.errorCode()))),
          4, 50, 0xff7777);
      return;
    }
    var coordinator = NeoForge1211ClientFileCheckClientRuntime.transfers();
    var artifact = coordinator.completedArtifact();
    if (artifact == null || artifact != initialArtifact) return;
    var metadata = artifact.metadata();
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
    var coordinator = NeoForge1211ClientFileCheckClientRuntime.transfers();
    if (coordinator.completedArtifact() != initialArtifact) {
      Minecraft.getInstance().setScreen(null);
      return;
    }
    var result = coordinator.saveCompleted(Minecraft.getInstance().gameDirectory.toPath());
    if (result.success()) Minecraft.getInstance().setScreen(null);
    else errorKey = CheckedFileTransferUiText.saveErrorKey(result.code());
  }

  private void discard() {
    var coordinator = NeoForge1211ClientFileCheckClientRuntime.transfers();
    if (coordinator.completedArtifact() != initialArtifact) {
      Minecraft.getInstance().setScreen(null);
      return;
    }
    var result = coordinator.discardCompleted();
    if (result.success()) Minecraft.getInstance().setScreen(null);
    else errorKey = CheckedFileTransferUiText.discardErrorKey(result.code());
  }

  @Override
  public void onClose() {
    if (terminal != null) Minecraft.getInstance().setScreen(null);
    else discard();
  }

  @Override
  public void removed() {}

}

package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.transfer.CheckedFileTransferReceivedArtifact;
import com.mo.economy_system.common.transfer.CheckedFileTransferSaveService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class Screen_CheckedFileTransferResult extends Screen {
  private final CheckedFileTransferReceivedArtifact artifact;
  private String errorKey;

  Screen_CheckedFileTransferResult(CheckedFileTransferReceivedArtifact artifact) {
    super(Component.translatable("screen.transfer_result.title"));
    this.artifact = artifact;
  }

  @Override protected void init() {
    if (width < 32 || height < 55) return;
    int buttonWidth = Math.min(100, Math.max(1, (width - 15) / 2));
    int total = buttonWidth * 2 + 5;
    int left = Math.max(0, (width - total) / 2);
    int y = Math.max(0, height - 25);
    addRenderableWidget(Button.builder(Component.translatable("button.transfer.save"), b -> save())
        .bounds(left, y, buttonWidth, Math.min(20, height - y)).build());
    addRenderableWidget(Button.builder(Component.translatable("button.transfer.discard"), b -> discard())
        .bounds(left + buttonWidth + 5, y, buttonWidth, Math.min(20, height - y)).build());
  }

  @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    super.render(graphics, mouseX, mouseY, partialTick);
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

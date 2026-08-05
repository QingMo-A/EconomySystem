package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.check.ClientFileCheckComparison;
import com.mo.economy_system.common.check.ClientFileCheckResult;
import com.mo.economy_system.common.check.ClientFileCheckScanner;
import com.mo.economy_system.common.network.ClientFileCheckResultResponseMessage;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class Screen_ClientFileCheckResult extends Screen {
  private final ClientFileCheckResultResponseMessage message;
  private final ClientFileCheckResult result;
  private List<ClientFileCheckComparison.Row> rows = List.of();
  private EditBox search;
  private int offset;

  public Screen_ClientFileCheckResult(
      ClientFileCheckResultResponseMessage message, ClientFileCheckResult result) {
    super(Component.translatable("screen.check_result.title"));
    this.message = message;
    this.result = result;
  }

  @Override
  protected void init() {
    if (width >= 80 && height >= 100) {
      search =
          new EditBox(
              font,
              12,
              62,
              Math.min(220, width - 24),
              18,
              Component.translatable("screen.check_result.search"));
      search.setHint(Component.translatable("screen.check_result.search"));
      search.setResponder(ignored -> offset = 0);
      addRenderableWidget(search);
    }
    Minecraft minecraft = Minecraft.getInstance();
    Screen_ClientFileCheckConsent.submitScan(
        () -> {
          ClientFileCheckResult local =
              new ClientFileCheckScanner()
                  .scan(minecraft.gameDirectory.toPath(), message.checkType());
          List<ClientFileCheckComparison.Row> compared =
              ClientFileCheckComparison.compare(result, local);
          minecraft.execute(() -> rows = compared);
        });
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
    int visible = Math.max(1, (height - 100) / 12);
    int size = filtered().size();
    offset = Math.max(0, Math.min(Math.max(0, size - visible), offset - (int) Math.signum(deltaY)));
    return true;
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
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
        Component.literal(
            result.status().name()
                + "  files="
                + result.files().size()
                + " skipped="
                + result.skipped().size()),
        Math.max(240, width / 2),
        66,
        0xCCCCCC);
    List<ClientFileCheckComparison.Row> visibleRows = filtered();
    int visible = Math.max(1, (height - 100) / 12);
    for (int i = offset; i < visibleRows.size() && i < offset + visible; i++) {
      ClientFileCheckComparison.Row row = visibleRows.get(i);
      String line = row.fileName() + "  " + row.kind().name();
      graphics.drawString(
          font,
          font.plainSubstrByWidth(line, Math.max(40, width - 24)),
          12,
          92 + (i - offset) * 12,
          0xAAAAAA);
    }
  }

  private List<ClientFileCheckComparison.Row> filtered() {
    if (search == null || search.getValue().isBlank()) return rows;
    String query = search.getValue().toLowerCase(Locale.ROOT);
    return rows.stream()
        .filter(row -> row.fileName().toLowerCase(Locale.ROOT).contains(query))
        .toList();
  }
}

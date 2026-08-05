package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.check.ClientFileCheckComparison;
import com.mo.economy_system.common.check.ClientFileCheckExecutor;
import com.mo.economy_system.common.check.ClientFileCheckResult;
import com.mo.economy_system.common.check.ClientFileCheckResultJsonCodec;
import com.mo.economy_system.common.check.ClientFileCheckScanner;
import com.mo.economy_system.common.network.ClientFileCheckRequestMessage;
import com.mo.economy_system.common.network.ClientFileCheckResultRequestMessage;
import com.mo.economy_system.common.network.ClientFileCheckResultResponseMessage;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class Forge1201ClientFileCheckScreens {
  private Forge1201ClientFileCheckScreens() {}

  public static void cancelPendingScan() {
    Screen_ClientFileCheckConsent.EXECUTOR.cancelPending();
  }

  static void openConsent(ClientFileCheckRequestMessage message) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null || !minecraft.player.getUUID().equals(message.targetPlayerId()))
      return;
    if (minecraft.screen instanceof Screen_ClientFileCheckConsent) return;
    minecraft.setScreen(new Screen_ClientFileCheckConsent(message));
  }

  static void openResult(ClientFileCheckResultResponseMessage message) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null || !minecraft.player.getUUID().equals(message.requesterPlayerId()))
      return;
    try {
      ClientFileCheckResult result = ClientFileCheckResultJsonCodec.decode(message.resultJson());
      if (result.checkType() == message.checkType())
        minecraft.setScreen(new Screen_ClientFileCheckResult(message, result));
    } catch (RuntimeException ignored) {
      // Fail closed.
    }
  }

  static final class Screen_ClientFileCheckConsent extends Screen {
    private static final ClientFileCheckExecutor EXECUTOR = new ClientFileCheckExecutor();
    private final ClientFileCheckRequestMessage request;
    private final AtomicBoolean finished = new AtomicBoolean();
    private final long deadline = System.nanoTime() + 60_000_000_000L;

    Screen_ClientFileCheckConsent(ClientFileCheckRequestMessage request) {
      super(Component.translatable("screen.check_consent.title"));
      this.request = request;
    }

    @Override
    protected void init() {
      if (width >= 220 && height >= 30) {
        int y = height - 25;
        addRenderableWidget(
            Button.builder(Component.translatable("button.check_consent.allow"), b -> allow())
                .bounds(width / 2 - 105, y, 100, 20)
                .build());
        addRenderableWidget(
            Button.builder(Component.translatable("button.check_consent.decline"), b -> decline())
                .bounds(width / 2 + 5, y, 100, 20)
                .build());
      } else if (width >= 110 && height >= 55) {
        addRenderableWidget(
            Button.builder(Component.translatable("button.check_consent.allow"), b -> allow())
                .bounds(width / 2 - 50, height - 50, 100, 20)
                .build());
        addRenderableWidget(
            Button.builder(Component.translatable("button.check_consent.decline"), b -> decline())
                .bounds(width / 2 - 50, height - 25, 100, 20)
                .build());
      }
    }

    private void allow() {
      if (System.nanoTime() > deadline) {
        decline();
        return;
      }
      if (!finished.compareAndSet(false, true)) return;
      Minecraft minecraft = Minecraft.getInstance();
      boolean accepted =
          EXECUTOR.submit(
              () -> {
                ClientFileCheckResult result;
                try {
                  result =
                      new ClientFileCheckScanner()
                          .scan(minecraft.gameDirectory.toPath(), request.checkType());
                } catch (RuntimeException failure) {
                  result = ClientFileCheckResult.failed(request.checkType(), "SCAN_FAILED");
                }
                ClientFileCheckResult completed = result;
                minecraft.execute(() -> send(completed));
              });
      if (!accepted) send(ClientFileCheckResult.failed(request.checkType(), "SCANNER_BUSY"));
      minecraft.setScreen(null);
    }

    private void decline() {
      if (!finished.compareAndSet(false, true)) return;
      send(ClientFileCheckResult.declined(request.checkType()));
      Minecraft.getInstance().setScreen(null);
    }

    private void send(ClientFileCheckResult result) {
      Forge1201NetworkChannel.sendToServer(
          new ClientFileCheckResultRequestMessage(
              request.targetPlayerName(),
              request.targetPlayerId(),
              request.requesterPlayerName(),
              request.requesterPlayerId(),
              request.checkType(),
              ClientFileCheckResultJsonCodec.encode(result)));
    }

    @Override
    public void onClose() {
      decline();
    }

    @Override
    public boolean shouldCloseOnEsc() {
      return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      renderBackground(graphics);
      super.render(graphics, mouseX, mouseY, partialTick);
      int x = width / 2, y = 35;
      graphics.drawCenteredString(font, title, x, y, 0xFFFFFF);
      graphics.drawCenteredString(
          font,
          Component.translatable("screen.check_consent.requester", request.requesterPlayerName()),
          x,
          y + 22,
          0xDDDDDD);
      graphics.drawCenteredString(
          font,
          Component.translatable("screen.check_consent.type", request.checkType().id()),
          x,
          y + 38,
          0xDDDDDD);
      graphics.drawCenteredString(
          font,
          Component.translatable("screen.check_consent.folder", request.checkType().id()),
          x,
          y + 54,
          0xDDDDDD);
      graphics.drawCenteredString(
          font, Component.translatable("screen.check_consent.data_notice"), x, y + 76, 0xAAAAAA);
      graphics.drawCenteredString(
          font,
          Component.translatable("screen.check_consent.no_content_notice"),
          x,
          y + 92,
          0xAAAAAA);
    }
  }

  static final class Screen_ClientFileCheckResult extends Screen {
    private final ClientFileCheckResultResponseMessage message;
    private final ClientFileCheckResult result;
    private List<ClientFileCheckComparison.Row> rows = List.of();
    private EditBox search;
    private int offset;

    Screen_ClientFileCheckResult(
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
      Screen_ClientFileCheckConsent.EXECUTOR.submit(
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
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      int visible = Math.max(1, (height - 100) / 12);
      int size = filtered().size();
      offset =
          Math.max(0, Math.min(Math.max(0, size - visible), offset - (int) Math.signum(delta)));
      return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      renderBackground(graphics);
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
}

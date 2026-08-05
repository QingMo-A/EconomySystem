package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.check.ClientFileCheckExecutor;
import com.mo.economy_system.common.check.ClientFileCheckResult;
import com.mo.economy_system.common.check.ClientFileCheckResultJsonCodec;
import com.mo.economy_system.common.check.ClientFileCheckScanner;
import com.mo.economy_system.common.network.ClientFileCheckRequestMessage;
import com.mo.economy_system.common.network.ClientFileCheckResultRequestMessage;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class Screen_ClientFileCheckConsent extends Screen {
  private static final ClientFileCheckExecutor EXECUTOR = new ClientFileCheckExecutor();
  private final ClientFileCheckRequestMessage request;
  private final AtomicBoolean finished = new AtomicBoolean();
  private final long deadline = System.nanoTime() + 60_000_000_000L;

  public Screen_ClientFileCheckConsent(ClientFileCheckRequestMessage request) {
    super(Component.translatable("screen.check_consent.title"));
    this.request = request;
  }

  public static void cancelPendingScan() {
    EXECUTOR.cancelPending();
  }

  static boolean submitScan(Runnable task) {
    return EXECUTOR.submit(task);
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
    String json = ClientFileCheckResultJsonCodec.encode(result);
    EconomySystem_NetworkManager.sendToServer(
        new ClientFileCheckResultRequestMessage(
            request.targetPlayerName(),
            request.targetPlayerId(),
            request.requesterPlayerName(),
            request.requesterPlayerId(),
            request.checkType(),
            json));
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
    super.render(graphics, mouseX, mouseY, partialTick);
    int x = width / 2;
    int y = 35;
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

package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.check.ClientFileCheckLayout;
import com.mo.economy_system.common.check.ClientFileCheckResult;
import com.mo.economy_system.common.check.ClientFileCheckResultController;
import com.mo.economy_system.common.check.ClientFileCheckScanner;
import com.mo.economy_system.common.check.ClientFileCheckTaskCoordinator;
import com.mo.economy_system.common.network.ClientFileCheckResultResponseMessage;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class Screen_ClientFileCheckResult extends Screen {
  private final ClientFileCheckResultResponseMessage message;
  private final ClientFileCheckResult result;
  private final ClientFileCheckResultController controller;
  private ClientFileCheckTaskCoordinator.TaskToken task;
  private EditBox search;
  private Button retry;
  private int offset;

  public Screen_ClientFileCheckResult(
      ClientFileCheckResultResponseMessage message, ClientFileCheckResult result) {
    super(Component.translatable("screen.check_result.title"));
    this.message = message;
    this.result = result;
    this.controller = new ClientFileCheckResultController(result);
  }

  @Override
  protected void init() {
    ClientFileCheckLayout.Result layout = ClientFileCheckLayout.result(width, height, true);
    ClientFileCheckLayout.Box box = layout.search();
    if (box != null) {
      search =
          new EditBox(
              font,
              box.x(),
              box.y(),
              box.width(),
              box.height(),
              Component.translatable("screen.check_result.search"));
      search.setHint(Component.translatable("screen.check_result.search"));
      search.setResponder(ignored -> offset = 0);
      addRenderableWidget(search);
    }
    if (layout.retry() != null && controller.needsComparison()) {
      retry =
          addRenderableWidget(
              Button.builder(
                      Component.translatable("button.check_result.retry"), ignored -> retry())
                  .bounds(
                      layout.retry().x(),
                      layout.retry().y(),
                      layout.retry().width(),
                      layout.retry().height())
                  .build());
      retry.visible = false;
    }
    if (!controller.needsComparison() || task != null) return;
    submit(controller.generation());
  }

  private void submit(long generation) {
    Minecraft minecraft = Minecraft.getInstance();
    ClientFileCheckTaskCoordinator coordinator = NeoForge1211ClientFileCheckClientRuntime.tasks();
    ClientFileCheckTaskCoordinator.Session session = coordinator.currentSession();
    ClientFileCheckTaskCoordinator.RequestIdentity identity =
        new ClientFileCheckTaskCoordinator.RequestIdentity(
            message.targetPlayerId(), message.requesterPlayerId(), message.checkType());
    task =
        session == null
            ? null
            : coordinator.submit(
                session,
                identity,
                generation,
                () ->
                    new ClientFileCheckScanner()
                        .scan(minecraft.gameDirectory.toPath(), message.checkType()),
                minecraft::execute,
                token ->
                    minecraft.screen == this
                        && minecraft.getConnection() == session.connectionIdentity()
                        && minecraft.player != null
                        && minecraft.player.getUUID().equals(session.localPlayerId())
                        && controller.generation() == token.controllerGeneration(),
                local -> {
                  controller.apply(generation, local);
                  task = null;
                },
                failure -> {
                  controller.failed(generation);
                  task = null;
                });
    if (task == null) controller.busy(generation);
  }

  private void retry() {
    if (Minecraft.getInstance().screen != this) return;
    long generation = controller.retry();
    if (generation < 0) return;
    if (task != null) task.cancel();
    task = null;
    submit(generation);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
    int visible = ClientFileCheckLayout.visibleRows(height, retry != null);
    int size = filtered().size();
    offset = ClientFileCheckLayout.clampOffset(offset - (int) Math.signum(deltaY), size, visible);
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
        Component.translatable(
            "screen.check_result.status_" + result.status().name().toLowerCase(Locale.ROOT)),
        12,
        84,
        0xCCCCCC);
    ClientFileCheckResultController.LocalState localState = controller.localState();
    if (retry != null)
      retry.visible =
          localState == ClientFileCheckResultController.LocalState.BUSY
              || localState == ClientFileCheckResultController.LocalState.FAILED;
    if (controller.needsComparison()
        && localState != ClientFileCheckResultController.LocalState.READY)
      graphics.drawString(
          font,
          Component.translatable(
              switch (localState) {
                case LOADING -> "screen.check_result.loading";
                case BUSY -> "screen.check_result.local_scan_busy";
                case FAILED -> "screen.check_result.local_scan_failed";
                default -> "screen.check_result.loading";
              }),
          12,
          120,
          0xAAAAAA);
    if (result.status() == com.mo.economy_system.common.check.ClientFileCheckStatus.TRUNCATED)
      graphics.drawString(
          font, Component.translatable("screen.check_result.incomplete"), 12, 132, 0xCCAA66);
    graphics.drawString(
        font,
        Component.literal(
            "files=" + result.files().size() + "  skipped=" + result.skipped().size()),
        12,
        96,
        0xCCCCCC);
    if (result.errorCode() != null)
      graphics.drawString(
          font,
          Component.translatable(
              "screen.check_result.error",
              Component.translatable(
                  "screen.check_result.error_code." + result.errorCode().toLowerCase(Locale.ROOT))),
          12,
          108,
          0xCC7777);
    List<ClientFileCheckResultController.UiRow> visibleRows = filtered();
    int visible = ClientFileCheckLayout.visibleRows(height, retry != null);
    int rowY = retry == null ? 148 : 172;
    for (int i = offset; i < visibleRows.size() && i < offset + visible; i++) {
      ClientFileCheckResultController.UiRow row = visibleRows.get(i);
      String key =
          row.type() == ClientFileCheckResultController.RowType.SKIPPED
              ? "screen.check_result.skip_reason." + row.reasonId()
              : "screen.check_result." + row.reasonId();
      String line = row.fileName() + "  " + Component.translatable(key).getString();
      graphics.drawString(
          font,
          font.plainSubstrByWidth(line, Math.max(40, width - 24)),
          12,
          rowY + (i - offset) * 12,
          0xAAAAAA);
    }
  }

  private List<ClientFileCheckResultController.UiRow> filtered() {
    return controller.filtered(search == null ? "" : search.getValue());
  }

  @Override
  public void removed() {
    controller.invalidate();
    if (task != null) task.cancel();
    super.removed();
  }
}

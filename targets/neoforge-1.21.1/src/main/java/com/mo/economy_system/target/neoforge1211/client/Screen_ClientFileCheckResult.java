package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.check.ClientFileCheckResult;
import com.mo.economy_system.common.check.ClientFileCheckScanner;
import com.mo.economy_system.common.check.ClientFileCheckTaskCoordinator;
import com.mo.economy_system.common.network.ClientFileCheckResultResponseMessage;
import com.mo.economy_system.ui.check.CheckResultAction;
import com.mo.economy_system.ui.check.CheckResultController;
import com.mo.economy_system.ui.check.CheckResultEvent;
import com.mo.economy_system.ui.check.CheckResultLayout;
import com.mo.economy_system.ui.check.CheckResultPort;
import com.mo.economy_system.ui.check.CheckResultView;
import com.mo.economy_system.ui.geometry.UiScale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** NeoForge shell for the common checked-file result state and layout. */
public final class Screen_ClientFileCheckResult extends Screen {
  private final ClientFileCheckResultResponseMessage message;
  private final CheckResultController controller;
  private ClientFileCheckTaskCoordinator.TaskToken task;
  private EditBox search;

  public Screen_ClientFileCheckResult(
      ClientFileCheckResultResponseMessage message, ClientFileCheckResult result) {
    super(Component.translatable("screen.check_result.title"));
    this.message = message;
    controller = new CheckResultController(message.targetPlayerName(), result, new Port());
  }

  @Override
  protected void init() {
    String value = search == null ? controller.state().filter() : search.getValue();
    CheckResultLayout.Layout layout = commonLayout();
    UiScale scale = layout.scale();
    search =
        new EditBox(
            font,
            Math.round(layout.search().x() * scale.value()),
            Math.round(layout.search().y() * scale.value()),
            Math.max(1, Math.round(layout.search().width() * scale.value())),
            Math.max(1, Math.round(layout.search().height() * scale.value())),
            Component.translatable("screen.check_result.search"));
    search.setMaxLength(64);
    search.setHint(Component.translatable("screen.check_result.search"));
    search.setValue(value);
    search.setResponder(text -> controller.handle(new CheckResultEvent.FilterChanged(text)));
    addRenderableWidget(search);
    controller.handle(new CheckResultEvent.Initialize());
  }

  @Override
  public void tick() {
    super.tick();
    controller.pollNavigation().ifPresent(
        navigation -> {
          if (minecraft != null && minecraft.screen == this) minecraft.setScreen(null);
        });
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    CheckResultLayout.Layout layout = commonLayout();
    UiScale scale = layout.scale();
    graphics.pose().pushPose();
    graphics.pose().scale(scale.value(), scale.value(), 1.0f);
    CheckResultView.render(
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
    CheckResultLayout.Layout layout = commonLayout();
    int x = layout.scale().toVirtualX(mouseX);
    int y = layout.scale().toVirtualY(mouseY);
    if (layout.retry().contains(x, y) && controller.state().can(CheckResultAction.RETRY)) {
      controller.handle(new CheckResultEvent.ActionClicked(CheckResultAction.RETRY));
      return true;
    }
    if (layout.back().contains(x, y)) {
      controller.handle(new CheckResultEvent.ActionClicked(CheckResultAction.BACK));
      return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
    if (deltaY != 0) {
      controller.handle(new CheckResultEvent.Scroll(deltaY < 0 ? 1 : -1));
      return true;
    }
    return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == 256) {
      onClose();
      return true;
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public void onClose() {
    controller.handle(new CheckResultEvent.ActionClicked(CheckResultAction.BACK));
    if (minecraft != null && minecraft.screen == this) minecraft.setScreen(null);
  }

  @Override
  public void removed() {
    controller.dispose();
    super.removed();
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

  private CheckResultLayout.Layout commonLayout() {
    CheckResultLayout.Layout layout = CheckResultLayout.calculate(width, height, controller.state());
    if (layout.visibleRows() != controller.state().pageSize()) {
      controller.handle(new CheckResultEvent.ViewportChanged(layout.visibleRows()));
      layout = CheckResultLayout.calculate(width, height, controller.state());
    }
    return layout;
  }

  private final class Port implements CheckResultPort {
    @Override
    public void startLocalScan(long generation) {
      if (task != null) task.cancel();
      task = null;
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
                  () -> new ClientFileCheckScanner().scan(minecraft.gameDirectory.toPath(), message.checkType()),
                  minecraft::execute,
                  token ->
                      minecraft.screen == Screen_ClientFileCheckResult.this
                          && minecraft.getConnection() == session.connectionIdentity()
                          && minecraft.player != null
                          && minecraft.player.getUUID().equals(session.localPlayerId())
                          && controller.generation() == token.controllerGeneration(),
                  (callbackToken, local) -> {
                    if (task == callbackToken) task = null;
                    controller.handle(
                        new CheckResultEvent.LocalScanCompleted(
                            callbackToken.controllerGeneration(), local));
                  },
                  (callbackToken, failure) -> {
                    if (task == callbackToken) task = null;
                    controller.handle(
                        new CheckResultEvent.LocalScanFailed(callbackToken.controllerGeneration()));
                  },
                  (abandonedToken, failure) -> {
                    if (task == abandonedToken) task = null;
                    controller.handle(
                        new CheckResultEvent.LocalScanFailed(abandonedToken.controllerGeneration()));
                  });
      if (task == null) controller.handle(new CheckResultEvent.LocalScanBusy(generation));
    }

    @Override
    public void cancelLocalScan() {
      if (task != null) task.cancel();
      task = null;
    }
  }
}

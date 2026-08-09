package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.check.ClientFileCheckClientResultDispatcher;
import com.mo.economy_system.common.check.ClientFileCheckConsentCoordinator;
import com.mo.economy_system.common.check.ClientFileCheckResult;
import com.mo.economy_system.common.check.ClientFileCheckResultJsonCodec;
import com.mo.economy_system.common.check.ClientFileCheckScanner;
import com.mo.economy_system.common.check.ClientFileCheckTaskCoordinator;
import com.mo.economy_system.common.network.ClientFileCheckRequestMessage;
import com.mo.economy_system.common.network.ClientFileCheckResultRequestMessage;
import com.mo.economy_system.common.network.ClientFileCheckResultResponseMessage;
import com.mo.economy_system.target.forge1201.client.Forge1201ClientFileCheckClientRuntime;
import com.mo.economy_system.target.forge1201.client.Forge1201UiRenderer;
import com.mo.economy_system.ui.check.CheckConsentAction;
import com.mo.economy_system.ui.check.CheckConsentController;
import com.mo.economy_system.ui.check.CheckConsentEvent;
import com.mo.economy_system.ui.check.CheckConsentLayout;
import com.mo.economy_system.ui.check.CheckConsentPort;
import com.mo.economy_system.ui.check.CheckConsentView;
import com.mo.economy_system.ui.check.CheckResultAction;
import com.mo.economy_system.ui.check.CheckResultController;
import com.mo.economy_system.ui.check.CheckResultEvent;
import com.mo.economy_system.ui.check.CheckResultLayout;
import com.mo.economy_system.ui.check.CheckResultPort;
import com.mo.economy_system.ui.check.CheckResultView;
import com.mo.economy_system.ui.geometry.UiScale;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Forge bindings for the common client-file-check screens and terminal dispatcher. */
public final class Forge1201ClientFileCheckScreens {
  private Forge1201ClientFileCheckScreens() {}

  static void openConsent(ClientFileCheckRequestMessage message) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null || !minecraft.player.getUUID().equals(message.targetPlayerId())) return;
    if (minecraft.getConnection() == null) return;
    ClientFileCheckTaskCoordinator.Session session =
        Forge1201ClientFileCheckClientRuntime.currentOrBegin(
            minecraft.getConnection(), minecraft.player.getUUID());
    ClientFileCheckTaskCoordinator.RequestIdentity identity = identity(message);
    ClientFileCheckConsentCoordinator.Decision decision =
        Forge1201ClientFileCheckClientRuntime.consent().receive(identity, session);
    if (decision == ClientFileCheckConsentCoordinator.Decision.DUPLICATE) return;
    if (decision == ClientFileCheckConsentCoordinator.Decision.BUSY) {
      dispatchBusy(message, identity, session,
          ClientFileCheckResult.failed(message.checkType(), "CONSENT_BUSY"));
      return;
    }
    minecraft.setScreen(new Screen_ClientFileCheckConsent(message, identity, session));
  }

  private static ClientFileCheckTaskCoordinator.RequestIdentity identity(
      ClientFileCheckRequestMessage message) {
    return new ClientFileCheckTaskCoordinator.RequestIdentity(
        message.targetPlayerId(), message.requesterPlayerId(), message.checkType());
  }

  private static boolean dispatchTerminal(
      ClientFileCheckRequestMessage request,
      ClientFileCheckTaskCoordinator.RequestIdentity identity,
      ClientFileCheckTaskCoordinator.Session session,
      ClientFileCheckTaskCoordinator.TaskToken token,
      ClientFileCheckResult result) {
    Minecraft minecraft = Minecraft.getInstance();
    return ClientFileCheckClientResultDispatcher.terminal(
        Forge1201ClientFileCheckClientRuntime.tasks(),
        Forge1201ClientFileCheckClientRuntime.consent(),
        session,
        identity,
        token,
        minecraft::getConnection,
        () -> minecraft.player == null ? null : minecraft.player.getUUID(),
        result,
        value -> sendRaw(request, value),
        (stage, ignored, failure) -> {});
  }

  private static boolean dispatchBusy(
      ClientFileCheckRequestMessage request,
      ClientFileCheckTaskCoordinator.RequestIdentity identity,
      ClientFileCheckTaskCoordinator.Session session,
      ClientFileCheckResult result) {
    Minecraft minecraft = Minecraft.getInstance();
    return ClientFileCheckClientResultDispatcher.busy(
        Forge1201ClientFileCheckClientRuntime.tasks(),
        Forge1201ClientFileCheckClientRuntime.consent(),
        session,
        identity,
        minecraft::getConnection,
        () -> minecraft.player == null ? null : minecraft.player.getUUID(),
        result,
        value -> sendRaw(request, value),
        (stage, ignored, failure) -> {});
  }

  private static void sendRaw(ClientFileCheckRequestMessage request, ClientFileCheckResult result) {
    Forge1201NetworkChannel.sendToServer(
        new ClientFileCheckResultRequestMessage(
            request.targetPlayerName(),
            request.targetPlayerId(),
            request.requesterPlayerName(),
            request.requesterPlayerId(),
            request.checkType(),
            ClientFileCheckResultJsonCodec.encode(result)));
    Forge1201ClientFileCheckClientRuntime.manifest()
        .replace(request.requesterPlayerId(), result, System.nanoTime());
  }

  static void openResult(ClientFileCheckResultResponseMessage message) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null || !minecraft.player.getUUID().equals(message.requesterPlayerId())) return;
    if (minecraft.getConnection() == null) return;
    Forge1201ClientFileCheckClientRuntime.currentOrBegin(
        minecraft.getConnection(), minecraft.player.getUUID());
    try {
      ClientFileCheckResult result = ClientFileCheckResultJsonCodec.decode(message.resultJson());
      if (result.checkType() == message.checkType()) {
        minecraft.setScreen(new Screen_ClientFileCheckResult(message, result));
      }
    } catch (RuntimeException ignored) {
      // Fail closed.
    }
  }

  /** Forge shell for the common consent controller and view. */
  static final class Screen_ClientFileCheckConsent extends Screen {
    private final ClientFileCheckRequestMessage request;
    private final ClientFileCheckTaskCoordinator.RequestIdentity identity;
    private final ClientFileCheckTaskCoordinator.Session session;
    private final AtomicBoolean finished = new AtomicBoolean();
    private final long deadline = System.nanoTime() + 60_000_000_000L;
    private final CheckConsentController controller;

    Screen_ClientFileCheckConsent(
        ClientFileCheckRequestMessage request,
        ClientFileCheckTaskCoordinator.RequestIdentity identity,
        ClientFileCheckTaskCoordinator.Session session) {
      super(Component.translatable("screen.check_consent.title"));
      this.request = request;
      this.identity = identity;
      this.session = session;
      controller = new CheckConsentController(request.requesterPlayerName(), request.checkType().id(), new Port());
    }

    private void allow() {
      if (System.nanoTime() > deadline) {
        if (finished.compareAndSet(false, true)) {
          terminal(ClientFileCheckResult.failed(request.checkType(), "REQUEST_EXPIRED"), null);
        }
        Minecraft.getInstance().setScreen(null);
        return;
      }
      if (!finished.compareAndSet(false, true)) return;
      Minecraft minecraft = Minecraft.getInstance();
      ClientFileCheckTaskCoordinator coordinator = Forge1201ClientFileCheckClientRuntime.tasks();
      if (!Forge1201ClientFileCheckClientRuntime.consent()
          .transition(
              identity,
              session,
              ClientFileCheckConsentCoordinator.State.CONSENT,
              ClientFileCheckConsentCoordinator.State.SCANNING)) {
        minecraft.setScreen(null);
        return;
      }
      ClientFileCheckTaskCoordinator.TaskToken token =
          session == null
              ? null
              : coordinator.submit(
                  session,
                  identity,
                  1,
                  () -> new ClientFileCheckScanner().scan(minecraft.gameDirectory.toPath(), request.checkType()),
                  minecraft::execute,
                  ignored ->
                      minecraft.getConnection() == session.connectionIdentity()
                          && minecraft.player != null
                          && minecraft.player.getUUID().equals(session.localPlayerId()),
                  (callbackToken, result) -> terminal(result, callbackToken),
                  (callbackToken, failure) ->
                      terminal(ClientFileCheckResult.failed(request.checkType(), "SCAN_FAILED"), callbackToken),
                  (abandonedToken, failure) ->
                      Forge1201ClientFileCheckClientRuntime.consent().finish(identity, session));
      if (token == null) terminal(ClientFileCheckResult.failed(request.checkType(), "SCANNER_BUSY"), null);
      minecraft.setScreen(null);
    }

    private void decline() {
      if (!finished.compareAndSet(false, true)) return;
      terminal(ClientFileCheckResult.declined(request.checkType()), null);
      Minecraft.getInstance().setScreen(null);
    }

    private void terminal(
        ClientFileCheckResult result, ClientFileCheckTaskCoordinator.TaskToken token) {
      dispatchTerminal(request, identity, session, token, result);
    }

    @Override
    public void onClose() {
      controller.handle(new CheckConsentEvent.ActionClicked(CheckConsentAction.DECLINE));
    }

    @Override
    public boolean shouldCloseOnEsc() {
      return true;
    }

    @Override
    public boolean isPauseScreen() {
      return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      CheckConsentLayout.Layout layout = CheckConsentLayout.calculate(width, height);
      UiScale scale = layout.scale();
      graphics.pose().pushPose();
      graphics.pose().scale(scale.value(), scale.value(), 1.0f);
      CheckConsentView.render(
          new Forge1201UiRenderer(graphics, font),
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
      CheckConsentLayout.Layout layout = CheckConsentLayout.calculate(width, height);
      int x = layout.scale().toVirtualX(mouseX);
      int y = layout.scale().toVirtualY(mouseY);
      if (layout.allow().contains(x, y)) {
        controller.handle(new CheckConsentEvent.ActionClicked(CheckConsentAction.ALLOW));
        return true;
      }
      if (layout.decline().contains(x, y)) {
        controller.handle(new CheckConsentEvent.ActionClicked(CheckConsentAction.DECLINE));
        return true;
      }
      return super.mouseClicked(mouseX, mouseY, button);
    }

    private final class Port implements CheckConsentPort {
      @Override
      public void allow() {
        Screen_ClientFileCheckConsent.this.allow();
      }

      @Override
      public void decline() {
        Screen_ClientFileCheckConsent.this.decline();
      }
    }
  }

  /** Forge shell for the common result controller and view. */
  static final class Screen_ClientFileCheckResult extends Screen {
    private final ClientFileCheckResultResponseMessage message;
    private final CheckResultController controller;
    private ClientFileCheckTaskCoordinator.TaskToken task;
    private EditBox search;

    Screen_ClientFileCheckResult(
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
          new Forge1201UiRenderer(graphics, font),
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
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      if (delta != 0) {
        controller.handle(new CheckResultEvent.Scroll(delta < 0 ? 1 : -1));
        return true;
      }
      return super.mouseScrolled(mouseX, mouseY, delta);
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
        ClientFileCheckTaskCoordinator coordinator = Forge1201ClientFileCheckClientRuntime.tasks();
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
}

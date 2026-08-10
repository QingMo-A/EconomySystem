package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.client.ClientBalanceLogState;
import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.common.network.BalanceLogRequestMessage;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.ui.balance.BalanceLogAction;
import com.mo.economy_system.ui.balance.BalanceLogController;
import com.mo.economy_system.ui.balance.BalanceLogEvent;
import com.mo.economy_system.ui.balance.BalanceLogLayout;
import com.mo.economy_system.ui.balance.BalanceLogPort;
import com.mo.economy_system.ui.balance.BalanceLogView;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import com.mo.economy_system.ui.geometry.UiScale;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** NeoForge Screen shell for the common balance-log table. */
public final class NeoForge1211BalanceLogScreen extends Screen {
  private static final AtomicLong IDS = new AtomicLong(1);
  private final Screen parent;
  private final Port port = new Port();
  private final BalanceLogController controller = new BalanceLogController(port);
  private long appliedRevision;

  public NeoForge1211BalanceLogScreen() { this(null); }
  public NeoForge1211BalanceLogScreen(Screen parent) {
    super(Component.translatable(EconomyUiRoute.BALANCE_LOG.titleKey()));
    this.parent = parent;
    appliedRevision = ClientBalanceLogState.snapshot().revision();
  }

  @Override protected void init() {
    commonLayout();
    if (controller.state().screenState() == ScreenState.IDLE) {
      controller.handle(new BalanceLogEvent.Initialize(System.nanoTime()));
    }
  }

  @Override public void tick() {
    super.tick();
    controller.handle(new BalanceLogEvent.Tick(System.nanoTime()));
    ClientBalanceLogState.Snapshot snapshot = ClientBalanceLogState.snapshot();
    if (snapshot.revision() != appliedRevision
        && snapshot.category().equals(controller.state().category())
        && snapshot.offset() == controller.state().offset()) {
      appliedRevision = snapshot.revision();
      controller.handle(new BalanceLogEvent.DataLoaded(port.requestId, snapshot.category(),
          snapshot.offset(), snapshot.limit(), snapshot.total(), snapshot.logs()));
    }
    controller.pollNavigation().ifPresent(this::navigate);
  }

  private void navigate(UiNavigation navigation) {
    if (minecraft == null) return;
    if (navigation instanceof UiNavigation.Route route && route.route() == EconomyUiRoute.HOME) {
      minecraft.setScreen(new NeoForge1211HomeScreen());
    } else if (navigation instanceof UiNavigation.Back) onClose();
  }

  @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    BalanceLogLayout.Layout layout = commonLayout();
    UiScale scale = layout.scale();
    new NeoForge1211UiRenderer(graphics, font).fillPhysicalBackground(width, height,
        BalanceLogLayout.BACKGROUND_COLOR);
    graphics.pose().pushPose();
    graphics.pose().scale(scale.value(), scale.value(), 1.0f);
    BalanceLogView.render(new NeoForge1211UiRenderer(graphics, font), controller.state(), layout,
        scale.toVirtualX(mouseX), scale.toVirtualY(mouseY));
    graphics.pose().popPose();
    super.render(graphics, mouseX, mouseY, partialTick);
  }

  @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
    BalanceLogLayout.Layout layout = commonLayout();
    int x = layout.scale().toVirtualX(mouseX), y = layout.scale().toVirtualY(mouseY);
    for (BalanceLogLayout.Tab tab : layout.tabs()) if (tab.rect().contains(x, y)) {
      appliedRevision = ClientBalanceLogState.snapshot().revision();
      controller.handle(new BalanceLogEvent.CategoryChanged(tab.category()));
      return true;
    }
    if (controller.state().screenState() == ScreenState.ERROR && layout.message().contains(x, y)) {
      appliedRevision = ClientBalanceLogState.snapshot().revision();
      controller.handle(new BalanceLogEvent.ActionClicked(BalanceLogAction.RETRY, System.nanoTime()));
      return true;
    }
    if (layout.previousButton().contains(x, y)) { controller.handle(new BalanceLogEvent.PreviousPage()); return true; }
    if (layout.nextButton().contains(x, y)) { controller.handle(new BalanceLogEvent.NextPage()); return true; }
    if (layout.esc().contains(x, y)) {
      controller.handle(new BalanceLogEvent.ActionClicked(BalanceLogAction.BACK, System.nanoTime()));
      return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    if (scrollY != 0) { controller.handle(new BalanceLogEvent.Scroll(scrollY < 0 ? 1 : -1)); return true; }
    return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
  }

  @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == 256) { onClose(); return true; }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override public void onClose() {
    if (minecraft == null) return;
    if (parent != null) minecraft.setScreen(parent); else minecraft.setScreen(new NeoForge1211HomeScreen());
  }

  @Override public boolean isPauseScreen() { return false; }
  @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

  private BalanceLogLayout.Layout commonLayout() {
    BalanceLogLayout.Layout layout = BalanceLogLayout.calculate(width, height, controller.state());
    if (layout.visibleRows() != controller.state().visibleRows()) {
      controller.handle(new BalanceLogEvent.ViewportChanged(layout.visibleRows()));
      layout = BalanceLogLayout.calculate(width, height, controller.state());
    }
    return layout;
  }

  private final class Port implements BalanceLogPort {
    private long requestId = -1;
    @Override public long nextRequestId() {
      long id = IDS.getAndIncrement();
      if (id < 0) throw new IllegalStateException("balance-log request id exhausted");
      return id;
    }
    @Override public void requestPage(long id, String category, int offset, int limit) {
      requestId = id;
      EconomyServices.platform().network().sendToServer(new BalanceLogRequestMessage(category, offset, limit));
    }
  }
}

package com.mo.economy_system.target.forge1201.client;

import com.mo.economy_system.common.client.ClientBalanceState;
import com.mo.economy_system.common.client.ClientMarketState;
import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.common.network.BalanceRequestMessage;
import com.mo.economy_system.common.network.MarketDataRequestMessage;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.home.HomeController;
import com.mo.economy_system.ui.home.HomeEvent;
import com.mo.economy_system.ui.home.HomeLayout;
import com.mo.economy_system.ui.home.HomeOpenAnimation;
import com.mo.economy_system.ui.home.HomePort;
import com.mo.economy_system.ui.home.HomeView;
import com.mo.economy_system.ui.text.UiTextMetrics;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Forge shell for the common home dashboard. */
public final class Forge1201HomeScreen extends Screen {
  private static final AtomicLong IDS = new AtomicLong(1);
  private final Port port = new Port();
  private final HomeController controller;
  private long appliedBalanceRevision;
  private long appliedMarketRequestId = -1;
  private long animationStartedAtNanos = -1L;

  public Forge1201HomeScreen() {
    super(Component.translatable(EconomyUiRoute.HOME.titleKey()));
    var minecraft = Minecraft.getInstance();
    var player = minecraft == null ? null : minecraft.player;
    String name = player == null ? "Player" : player.getName().getString();
    controller = new HomeController(name, port);
    appliedBalanceRevision = ClientBalanceState.snapshot().revision();
  }

  @Override protected void init() {
    if (animationStartedAtNanos < 0L) animationStartedAtNanos = System.nanoTime();
    if (controller.state().screenState() == ScreenState.IDLE) {
      controller.handle(new HomeEvent.Initialize(System.nanoTime()));
    }
    syncViewport();
  }

  @Override public void tick() {
    super.tick();
    controller.handle(new HomeEvent.Tick(System.nanoTime()));
    var balance = ClientBalanceState.snapshot();
    if (balance.revision() != appliedBalanceRevision && port.homeRequestId >= 0) {
      appliedBalanceRevision = balance.revision();
      controller.handle(new HomeEvent.BalanceLoaded(port.homeRequestId, balance.revision(),
          balance.balance(), balance.accounts()));
    }
    var market = ClientMarketState.snapshot();
    if (market.latestSummaryRequestId() == port.marketRequestId
        && market.latestSummaryRequestId() != appliedMarketRequestId) {
      appliedMarketRequestId = market.latestSummaryRequestId();
      controller.handle(new HomeEvent.MarketLoaded(port.homeRequestId, market.marketRevision(),
          market.totalSales(), market.totalDemand()));
    }
    controller.pollNavigation().ifPresent(this::navigate);
  }

  private void navigate(UiNavigation navigation) {
    if (minecraft == null) return;
    if (navigation instanceof UiNavigation.Route route && route.route() != EconomyUiRoute.HOME) {
      minecraft.setScreen(Forge1201UiBridge.INSTANCE.create(route.route())
          .orElseGet(Forge1201HomeScreen::new));
    } else if (navigation instanceof UiNavigation.Back) {
      minecraft.setScreen(null);
    }
  }

  @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    syncViewport();
    Forge1201UiRenderer renderer = new Forge1201UiRenderer(graphics, font);
    // The reference background is a physical-screen layer.  Draw it before the virtual-canvas
    // pose so fractional viewport scales cannot leave an uncovered edge.
    renderer.fillPhysicalBackground(width, height, HomeLayout.BACKGROUND_COLOR);
    float progress = animationProgress();
    HomeLayout.Layout layout = HomeLayout.calculate(width, height, controller.state(),
        renderer.metrics(), progress);
    UiScale scale = layout.scale();
    graphics.pose().pushPose();
    graphics.pose().scale(scale.value(), scale.value(), 1.0f);
    HomeView.render(renderer, controller.state(), layout,
        scale.toVirtualX(mouseX), scale.toVirtualY(mouseY));
    graphics.pose().popPose();
    super.render(graphics, mouseX, mouseY, partialTick);
  }

  @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
    float progress = animationProgress();
    HomeLayout.Layout layout = HomeLayout.calculate(width, height, controller.state(),
        metrics(), progress);
    int x = layout.scale().toVirtualX(mouseX), y = layout.scale().toVirtualY(mouseY);
    for (var nav : layout.navButtons()) if (nav.rect().contains(x, y)) {
      controller.handle(new HomeEvent.ActionClicked(nav.route())); return true;
    }
    if (layout.balanceCard().contains(x, y)) {
      controller.handle(new HomeEvent.ActionClicked(EconomyUiRoute.BALANCE_LOG)); return true;
    }
    if (layout.tradeCard().contains(x, y)) {
      controller.handle(new HomeEvent.ActionClicked(EconomyUiRoute.MARKET)); return true;
    }
    if (controller.state().screenState() == ScreenState.ERROR && layout.retryButton().contains(x, y)) {
      controller.handle(new HomeEvent.Retry(System.nanoTime())); return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    if (delta != 0 && controller.state().accounts().size() > HomeLayout.LEADERBOARD_VISIBLE_ROWS) {
      controller.handle(new HomeEvent.Scroll(delta < 0 ? 1 : -1)); return true;
    }
    return super.mouseScrolled(mouseX, mouseY, delta);
  }

  @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == 256) { onClose(); return true; }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override public void onClose() { if (minecraft != null) minecraft.setScreen(null); }
  @Override public boolean isPauseScreen() { return false; }

  private void syncViewport() {
    HomeLayout.Layout layout = HomeLayout.calculate(width, height, controller.state(),
        metrics(), animationProgress());
    if (layout.pageSize() != controller.state().leaderboardPageSize()) {
      controller.handle(new HomeEvent.ViewportChanged(layout.pageSize()));
    }
  }

  private UiTextMetrics metrics() {
    return new Forge1201UiTextMetrics(font);
  }

  private float animationProgress() {
    return HomeOpenAnimation.easedProgressAt(animationStartedAtNanos, System.nanoTime());
  }

  private final class Port implements HomePort {
    private long homeRequestId = -1;
    private long marketRequestId = -1;
    @Override public long nextRequestId() {
      long id = IDS.getAndIncrement();
      if (id < 0) throw new IllegalStateException("home request id exhausted");
      return id;
    }
    @Override public void requestBalance(long id) {
      homeRequestId = id;
      EconomyServices.platform().network().sendToServer(new BalanceRequestMessage(true));
    }
    @Override public void requestMarketSummary(long id) {
      homeRequestId = id;
      marketRequestId = ClientMarketState.nextSummaryRequestId();
      EconomyServices.platform().network().sendToServer(MarketDataRequestMessage.summary(marketRequestId));
    }
  }
}

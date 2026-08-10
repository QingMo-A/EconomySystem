package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.client.ClientShopState;
import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.common.network.ShopBuyItemMessage;
import com.mo.economy_system.common.network.ShopDataRequestMessage;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.shop.ShopAction;
import com.mo.economy_system.ui.shop.ShopController;
import com.mo.economy_system.ui.shop.ShopEvent;
import com.mo.economy_system.ui.shop.ShopLayout;
import com.mo.economy_system.ui.shop.ShopOpenAnimation;
import com.mo.economy_system.ui.shop.ShopPort;
import com.mo.economy_system.ui.shop.ShopRow;
import com.mo.economy_system.ui.shop.ShopView;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** NeoForge shell for the common shop page. */
public final class NeoForge1211ShopScreen extends Screen {
  private static final AtomicLong IDS = new AtomicLong(1);
  private final Screen parent;
  private final Port port = new Port();
  private final ShopController controller = new ShopController(port);
  private EditBox search;
  private long appliedRevision = -1;
  private long animationStartedAtNanos = -1L;

  public NeoForge1211ShopScreen() { this(null); }
  public NeoForge1211ShopScreen(Screen parent) {
    super(Component.translatable(EconomyUiRoute.SHOP.titleKey()));
    this.parent = parent;
  }

  @Override protected void init() {
    if (animationStartedAtNanos < 0L) animationStartedAtNanos = System.nanoTime();
    String value = search == null ? "" : search.getValue();
    ShopLayout.Layout layout = commonLayout(metrics());
    UiScale scale = layout.scale();
    search = new EditBox(font, Math.round(layout.search().x() * scale.value()),
        Math.round(layout.search().y() * scale.value()),
        Math.max(1, Math.round(layout.search().width() * scale.value())),
        Math.max(1, Math.round(layout.search().height() * scale.value())),
        Component.translatable("screen.shop.search"));
    search.setMaxLength(64);
    search.setValue(value);
    search.setResponder(text -> controller.handle(new ShopEvent.FilterChanged(text)));
    addRenderableWidget(search);
    if (controller.state().screenState() == ScreenState.IDLE) controller.handle(new ShopEvent.Initialize(System.nanoTime()));
  }

  @Override public void tick() {
    super.tick();
    controller.handle(new ShopEvent.Tick(System.nanoTime()));
    ClientShopState.Snapshot snapshot = ClientShopState.snapshot();
    if (snapshot.revision() != appliedRevision && port.requestId >= 0) {
      appliedRevision = snapshot.revision();
      controller.handle(new ShopEvent.DataLoaded(port.requestId, snapshot.items()));
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
    NeoForge1211UiRenderer renderer = new NeoForge1211UiRenderer(graphics, font);
    renderer.fillPhysicalBackground(width, height, ShopLayout.BACKGROUND_COLOR);
    ShopLayout.Layout layout = commonLayout(renderer.metrics());
    UiScale scale = layout.scale();
    syncSearchWidget(layout);
    graphics.pose().pushPose();
    graphics.pose().scale(scale.value(), scale.value(), 1.0f);
    ShopView.render(renderer, controller.state(), layout,
        scale.toVirtualX(mouseX), scale.toVirtualY(mouseY));
    graphics.pose().popPose();
    super.render(graphics, mouseX, mouseY, partialTick);
  }

  @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
    ShopLayout.Layout layout = commonLayout(metrics());
    int x = layout.scale().toVirtualX(mouseX), y = layout.scale().toVirtualY(mouseY);
    for (ShopLayout.Card card : layout.cards()) if (card.card().contains(x, y)) {
      controller.handle(new ShopEvent.ActionClicked(ShopAction.BUY, card.row().item().shopItemId()));
      return true;
    }
    if (controller.state().screenState() == ScreenState.ERROR && layout.message().contains(x, y)) {
      controller.handle(new ShopEvent.Retry(System.nanoTime())); return true;
    }
    if (layout.previousButton().contains(x, y)) { controller.handle(new ShopEvent.PreviousPage()); return true; }
    if (layout.nextButton().contains(x, y)) { controller.handle(new ShopEvent.NextPage()); return true; }
    if (layout.esc().contains(x, y)) { controller.handle(new ShopEvent.ActionClicked(ShopAction.BACK, null)); return true; }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    if (scrollY != 0 && controller.state().totalPages() > 1) {
      controller.handle(new ShopEvent.Scroll(scrollY < 0 ? 1 : -1)); return true;
    }
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

  private ShopLayout.Layout commonLayout(com.mo.economy_system.ui.text.UiTextMetrics metrics) {
    ShopLayout.Layout layout = ShopLayout.calculate(width, height, controller.state(), metrics, animationProgress());
    if (layout.pageSize() != controller.state().pageSize()) {
      controller.handle(new ShopEvent.ViewportChanged(layout.pageSize()));
      layout = ShopLayout.calculate(width, height, controller.state(), metrics, animationProgress());
    }
    return layout;
  }

  private com.mo.economy_system.ui.text.UiTextMetrics metrics() { return new NeoForge1211UiTextMetrics(font); }
  private float animationProgress() { return ShopOpenAnimation.easedProgressAt(animationStartedAtNanos, System.nanoTime()); }
  private void syncSearchWidget(ShopLayout.Layout layout) { if (search == null) return; UiScale scale = layout.scale(); search.setX(Math.round(layout.search().x() * scale.value())); search.setY(Math.round(layout.search().y() * scale.value())); search.setWidth(Math.max(1, Math.round(layout.search().width() * scale.value()))); search.setHeight(Math.max(1, Math.round(layout.search().height() * scale.value()))); }

  private final class Port implements ShopPort {
    private long requestId = -1;
    @Override public long nextRequestId() {
      long id = IDS.getAndIncrement();
      if (id < 0) throw new IllegalStateException("shop request id exhausted");
      return id;
    }
    @Override public void requestCatalog(long id) {
      requestId = id;
      EconomyServices.platform().network().sendToServer(ShopDataRequestMessage.INSTANCE);
    }
    @Override public void submit(ShopAction action, ShopRow row, int quantity) {
      if (action == ShopAction.BUY) EconomyServices.platform().network().sendToServer(
          new ShopBuyItemMessage(row.item().shopItemId(), quantity));
    }
    @Override public void confirm(ShopRow row) {
      if (minecraft != null) minecraft.setScreen(new NeoForge1211ShopPurchaseScreen(row, NeoForge1211ShopScreen.this));
    }
  }
}

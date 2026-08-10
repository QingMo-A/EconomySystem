package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.client.ClientMarketState;
import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.common.market.MarketOrderType;
import com.mo.economy_system.common.network.*;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.market.MarketAction;
import com.mo.economy_system.ui.market.MarketController;
import com.mo.economy_system.ui.market.MarketEvent;
import com.mo.economy_system.ui.market.MarketLayout;
import com.mo.economy_system.ui.market.MarketOpenAnimation;
import com.mo.economy_system.ui.market.MarketPort;
import com.mo.economy_system.ui.market.MarketRow;
import com.mo.economy_system.ui.market.MarketView;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** NeoForge shell for the common market list. */
public final class NeoForge1211MarketScreen extends Screen {
  private final Screen parent;
  private final Port port = new Port();
  private final MarketController controller;
  private EditBox search;
  private long appliedRequest = -1;
  private long animationStartedAtNanos = -1L;

  public NeoForge1211MarketScreen() { this(null); }
  public NeoForge1211MarketScreen(Screen parent) {
    super(Component.translatable(EconomyUiRoute.MARKET.titleKey()));
    this.parent = parent;
    Minecraft current = Minecraft.getInstance();
    UUID viewer = current == null || current.player == null ? null : current.player.getUUID();
    boolean moderator = current != null && current.player != null && current.player.hasPermissions(2);
    controller = new MarketController(viewer, moderator, NeoForge1211MarketScreen::nativeDisplayName, port);
  }
  @Override protected void init() {
    if (animationStartedAtNanos < 0L) animationStartedAtNanos = System.nanoTime();
    String value = search == null ? "" : search.getValue(); MarketLayout.Layout layout = commonLayout(metrics()); UiScale scale = layout.scale();
    search = new EditBox(font, Math.round(layout.search().x() * scale.value()), Math.round(layout.search().y() * scale.value()), Math.max(1, Math.round(layout.search().width() * scale.value())), Math.max(1, Math.round(layout.search().height() * scale.value())), Component.translatable("screen.market.search"));
    search.setMaxLength(com.mo.economy_system.ui.text.UiSearchPolicy.SEARCH_MAX_LENGTH); search.setHint(Component.translatable("screen.market.search_hint")); search.setFocused(false); search.setValue(value); search.setResponder(text -> controller.handle(new MarketEvent.QueryChanged(text))); addRenderableWidget(search);
    if (controller.state().screenState() == ScreenState.IDLE) controller.handle(new MarketEvent.Initialize(System.nanoTime()));
  }
  @Override public void tick() {
    super.tick(); controller.handle(new MarketEvent.Tick(System.nanoTime()));
    ClientMarketState.Snapshot snapshot = ClientMarketState.snapshot();
    if (snapshot.latestPageRequestId() == port.requestId && !snapshot.loading() && snapshot.latestPageRequestId() != appliedRequest) {
      appliedRequest = snapshot.latestPageRequestId();
      if (!snapshot.error().isEmpty()) controller.handle(new MarketEvent.DataFailed(port.requestId, "screen.market.sync_failed"));
      else controller.handle(new MarketEvent.DataLoaded(port.requestId, snapshot.marketRevision(), snapshot.offset(), snapshot.totalMatched(), snapshot.totalSales(), snapshot.totalDemand(), snapshot.orders()));
    }
    controller.pollNavigation().ifPresent(this::navigate);
  }
  private void navigate(UiNavigation navigation) { if (minecraft == null) return; if (navigation instanceof UiNavigation.Route route && route.route() == EconomyUiRoute.HOME) minecraft.setScreen(new NeoForge1211HomeScreen()); else if (navigation instanceof UiNavigation.Back) onClose(); }
  @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { NeoForge1211UiRenderer renderer = new NeoForge1211UiRenderer(graphics, font); renderer.fillPhysicalBackground(width, height, MarketLayout.BACKGROUND_COLOR); MarketLayout.Layout layout = commonLayout(renderer.metrics()); UiScale scale = layout.scale(); syncSearchWidget(layout); if (search != null) MarketView.renderSearchFrame(renderer, new UiRect(search.getX(), search.getY(), search.getWidth(), search.getHeight()), search.isFocused()); graphics.pose().pushPose(); graphics.pose().scale(scale.value(), scale.value(), 1.0f); MarketView.render(renderer, controller.state(), layout, controller.viewerId(), scale.toVirtualX(mouseX), scale.toVirtualY(mouseY)); graphics.pose().popPose(); super.render(graphics, mouseX, mouseY, partialTick); }
  @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
    MarketLayout.Layout layout = commonLayout(metrics()); int x = layout.scale().toVirtualX(mouseX), y = layout.scale().toVirtualY(mouseY);
    for (MarketLayout.FilterTab tab : layout.filterTabs()) if (tab.hitRect().contains(x, y)) { controller.handle(new MarketEvent.FilterChanged(tab.filter())); return true; }
    for (MarketLayout.Card card : layout.cards()) { var order = card.row().order(); boolean own = controller.viewerId() != null && controller.viewerId().equals(order.ownerId()); if (!own && order.type() == com.mo.economy_system.common.market.MarketOrderType.SALES && card.adminActionButton().contains(x, y) && controller.state().can(MarketAction.ADMIN_REMOVE_SALES)) { controller.handle(new MarketEvent.ActionClicked(MarketAction.ADMIN_REMOVE_SALES, order.tradeId())); return true; } if (card.actionButton().contains(x, y)) { MarketAction action = MarketView.actionFor(order, controller.viewerId()); if (action != null) controller.handle(new MarketEvent.ActionClicked(action, order.tradeId())); return true; } }
    if (layout.createSales().contains(x, y)) { controller.handle(new MarketEvent.ActionClicked(MarketAction.CREATE_SALES, null)); return true; }
    if (layout.createDemand().contains(x, y)) { controller.handle(new MarketEvent.ActionClicked(MarketAction.CREATE_DEMAND, null)); return true; }
    if (controller.state().screenState() == ScreenState.ERROR && layout.message().contains(x, y)) { controller.handle(new MarketEvent.Retry(System.nanoTime())); return true; }
    if (controller.state().totalPages() > 1 && layout.previousButton().contains(x, y)) { controller.handle(new MarketEvent.PreviousPage()); return true; }
    if (controller.state().totalPages() > 1 && layout.nextButton().contains(x, y)) { controller.handle(new MarketEvent.NextPage()); return true; }
    if (layout.esc().contains(x, y)) { controller.handle(new MarketEvent.ActionClicked(MarketAction.BACK, null)); return true; }
    return super.mouseClicked(mouseX, mouseY, button);
  }
  @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) { if (scrollY != 0 && controller.state().totalPages() > 1) { controller.handle(new MarketEvent.Scroll(scrollY < 0 ? 1 : -1)); return true; } return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY); }
  @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) { if (keyCode == 256) { onClose(); return true; } return super.keyPressed(keyCode, scanCode, modifiers); }
  @Override public void onClose() { if (minecraft == null) return; if (parent != null) minecraft.setScreen(parent); else minecraft.setScreen(new NeoForge1211HomeScreen()); }
  @Override public boolean isPauseScreen() { return false; }
  @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
  private MarketLayout.Layout commonLayout(com.mo.economy_system.ui.text.UiTextMetrics textMetrics) { MarketLayout.Layout layout = MarketLayout.calculate(width, height, controller.state(), textMetrics, animationProgress()); if (layout.pageSize() != controller.state().pageSize()) { controller.handle(new MarketEvent.ViewportChanged(layout.pageSize())); layout = MarketLayout.calculate(width, height, controller.state(), textMetrics, animationProgress()); } return layout; }
  private com.mo.economy_system.ui.text.UiTextMetrics metrics() { return new NeoForge1211UiTextMetrics(font); }
  private float animationProgress() { return MarketOpenAnimation.easedProgressAt(animationStartedAtNanos, System.nanoTime()); }
  private static String nativeDisplayName(String itemId) { ResourceLocation location = ResourceLocation.tryParse(itemId); var item = location == null ? null : BuiltInRegistries.ITEM.get(location); return item == null ? "" : new ItemStack(item).getHoverName().getString(); }
  private void syncSearchWidget(MarketLayout.Layout layout) { if (search == null) return; UiScale scale = layout.scale(); search.setX(Math.round(layout.search().x() * scale.value())); search.setY(Math.round(layout.search().y() * scale.value())); search.setWidth(Math.max(1, Math.round(layout.search().width() * scale.value()))); search.setHeight(Math.max(1, Math.round(layout.search().height() * scale.value()))); }
  private final class Port implements MarketPort {
    private long requestId = -1;
    @Override public long nextRequestId() { return ClientMarketState.nextPageRequestId(); }
    @Override public void requestPage(long id, int offset, MarketOrderFilter filter, String query) { requestId = id; EconomyServices.platform().network().sendToServer(new MarketDataRequestMessage(id, MarketDataRequestPurpose.PAGE, offset, MarketController.NETWORK_PAGE_SIZE, filter, query)); }
    @Override public void submit(MarketAction action, MarketRow row) { UUID id = row.order().tradeId(); switch (action) { case BUY -> EconomyServices.platform().network().sendToServer(new PurchaseSalesOrderMessage(id)); case REMOVE_SALES, ADMIN_REMOVE_SALES -> EconomyServices.platform().network().sendToServer(new RemoveSalesOrderMessage(id)); case DELIVER_DEMAND -> EconomyServices.platform().network().sendToServer(new DeliverDemandOrderMessage(id)); case CONFIRM_DEMAND -> EconomyServices.platform().network().sendToServer(new ConfirmDemandOrderMessage(id)); case REMOVE_DEMAND -> EconomyServices.platform().network().sendToServer(new RemoveDemandOrderMessage(id)); default -> {} } }
    @Override public void confirm(MarketAction action, MarketRow row) { if (minecraft != null) minecraft.setScreen(new NeoForge1211MarketConfirmScreen(action, row, NeoForge1211MarketScreen.this)); }
    @Override public void create(MarketAction action) {
      if (minecraft == null) return;
      if (action == MarketAction.CREATE_SALES) minecraft.setScreen(new NeoForge1211MarketCreateScreen(com.mo.economy_system.ui.market.MarketCreateMode.SALES, NeoForge1211MarketScreen.this));
      else if (action == MarketAction.CREATE_DEMAND) minecraft.setScreen(new NeoForge1211MarketCreateScreen(com.mo.economy_system.ui.market.MarketCreateMode.DEMAND, NeoForge1211MarketScreen.this));
    }
  }
}

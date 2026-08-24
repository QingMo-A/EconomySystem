package com.mo.economy_system.target.forge1201.client;

import com.mo.economy_system.common.client.ClientBalanceState;
import com.mo.economy_system.common.client.ClientMarketState;
import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.common.network.*;
import com.mo.economy_system.core.economy_system.EconomyLedger;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.target.forge1201.Forge1201Platform;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.market.*;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Forge shell for Market v2 browser + inline partial-trade detail pane. */
public final class Forge1201MarketScreen extends Screen {
  private static final long DETAIL_INVALID_NANOS = 1_500_000_000L;
  private final Screen parent;
  private final Port port = new Port();
  private final DetailPort detailPort = new DetailPort();
  private final MarketController controller;

  private EditBox search;
  private EditBox quantity;
  private MarketDetailController detailController;
  private boolean syncingQuantity;
  private long appliedRequest = -1;
  private long detailInvalidUntilNanos;

  public Forge1201MarketScreen() { this(null); }

  public Forge1201MarketScreen(Screen parent) {
    super(Component.translatable(EconomyUiRoute.MARKET.titleKey()));
    this.parent = parent;
    Minecraft current = Minecraft.getInstance();
    UUID viewer = current == null || current.player == null ? null : current.player.getUUID();
    boolean moderator = current != null && current.player != null && current.player.hasPermissions(2);
    controller = new MarketController(viewer, moderator,
        Forge1201MarketScreen::nativeDisplayName, port);
  }

  public void refreshData() {
    appliedRequest = -1;
    clearDetail();
    controller.handle(new MarketEvent.Initialize(System.nanoTime()));
  }

  @Override protected void init() {
    String searchValue = search == null ? controller.state().query() : search.getValue();
    int quantityValue = detailController == null ? 1 : Math.max(1, detailController.state().quantity());
    MarketLayout.Layout layout = commonLayout(metrics());
    UiScale scale = layout.scale();

    search = new Forge1201UnderlinedEditBox(font,
        sx(layout.search().x(), scale), sy(layout.search().y(), scale),
        sw(layout.search().width(), scale), sh(layout.search().height(), scale),
        Component.translatable("screen.market.search"));
    Forge1201UiInputAdapter.apply(search);
    search.setMaxLength(com.mo.economy_system.ui.text.UiSearchPolicy.SEARCH_MAX_LENGTH);
    search.setHint(Component.translatable("screen.market.search_hint"));
    search.setFocused(false);
    search.setValue(searchValue);
    search.setResponder(text -> {
      if (!text.equals(controller.state().query())) {
        clearDetail();
        controller.handle(new MarketEvent.QueryChanged(text));
      }
    });
    addRenderableWidget(search);

    quantity = new Forge1201UnderlinedEditBox(font,
        sx(layout.quantityInput().x(), scale), sy(layout.quantityInput().y(), scale),
        sw(layout.quantityInput().width(), scale), sh(layout.quantityInput().height(), scale),
        Component.translatable("screen.market.detail.quantity"));
    Forge1201UiInputAdapter.apply(quantity);
    quantity.setMaxLength(9);
    quantity.setHint(Component.translatable("screen.market.detail.quantity"));
    quantity.setValue(Integer.toString(quantityValue));
    quantity.setResponder(value -> {
      if (!syncingQuantity && detailController != null) {
        detailController.handle(new MarketDetailEvent.QuantityChanged(parseQuantity(value)));
      }
    });
    addRenderableWidget(quantity);
    syncQuantityVisibility();

    if (controller.state().screenState() == ScreenState.IDLE) {
      controller.handle(new MarketEvent.Initialize(System.nanoTime()));
    }
    EconomyServices.platform().network().sendToServer(new BalanceRequestMessage(true));
  }

  @Override public void tick() {
    super.tick();
    long now = System.nanoTime();
    controller.handle(new MarketEvent.Tick(now));

    ClientMarketState.Snapshot snapshot = ClientMarketState.snapshot();
    if (snapshot.stale() && !snapshot.loading()) {
      UUID focusTradeId = detailController == null
          ? null : detailController.state().row().order().tradeId();
      controller.handle(new MarketEvent.Refresh(now, focusTradeId));
      snapshot = ClientMarketState.snapshot();
    }
    if (snapshot.latestPageRequestId() == port.requestId && !snapshot.loading()
        && snapshot.latestPageRequestId() != appliedRequest) {
      appliedRequest = snapshot.latestPageRequestId();
      if (!snapshot.error().isEmpty()) {
        controller.handle(new MarketEvent.DataFailed(port.requestId, "screen.market.sync_failed"));
      } else {
        controller.handle(new MarketEvent.DataLoaded(port.requestId, snapshot.marketRevision(), snapshot.offset(),
            snapshot.totalMatched(), snapshot.totalSales(), snapshot.totalDemand(), snapshot.orders()));
        reconcileSelectedRow();
      }
    }

    if (detailInvalidUntilNanos != 0 && now >= detailInvalidUntilNanos) clearDetail();
    refreshDetailFacts();
    syncQuantityValue();
    controller.pollNavigation().ifPresent(this::navigate);
  }

  private void navigate(UiNavigation navigation) {
    if (minecraft == null) return;
    if (navigation instanceof UiNavigation.Route route && route.route() == EconomyUiRoute.HOME) {
      minecraft.setScreen(new Forge1201HomeScreen());
    } else if (navigation instanceof UiNavigation.Back) {
      onClose();
    }
  }

  @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    Forge1201UiRenderer renderer = new Forge1201UiRenderer(graphics, font);
    renderer.fillPhysicalBackground(width, height, MarketLayout.BACKGROUND_COLOR);
    MarketLayout.Layout layout = commonLayout(renderer.metrics());
    UiScale scale = layout.scale();
    syncWidgets(layout);

    if (search != null) {
      UiRect rect = new UiRect(search.getX(), search.getY(), search.getWidth(), search.getHeight());
      MarketView.renderSearchFrame(renderer, rect, search.isFocused(), rect.contains(mouseX, mouseY));
    }
    if (quantity != null && quantity.visible) {
      UiRect rect = new UiRect(quantity.getX(), quantity.getY(), quantity.getWidth(), quantity.getHeight());
      MarketInlineDetailView.renderQuantityFrame(renderer, rect, quantity.isFocused(),
          rect.contains(mouseX, mouseY), detailController != null
              && detailController.state().errorKey() != null
              && detailController.state().errorKey().contains("quantity"));
    }

    graphics.pose().pushPose();
    graphics.pose().scale(scale.value(), scale.value(), 1.0f);
    MarketView.render(renderer, controller.state(), layout,
        detailController == null ? null : detailController.state(), controller.viewerId(),
        scale.toVirtualX(mouseX), scale.toVirtualY(mouseY));
    graphics.pose().popPose();
    super.render(graphics, mouseX, mouseY, partialTick);
  }

  @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
    MarketLayout.Layout layout = commonLayout(metrics());
    int x = layout.scale().toVirtualX(mouseX);
    int y = layout.scale().toVirtualY(mouseY);

    for (MarketLayout.FilterTab tab : layout.filterTabs()) {
      if (tab.hitRect().contains(x, y)) {
        clearDetail();
        controller.handle(new MarketEvent.FilterChanged(tab.filter()));
        return true;
      }
    }
    for (MarketLayout.SortTab tab : layout.sortTabs()) {
      if (tab.hitRect().contains(x, y)) {
        clearDetail();
        controller.handle(new MarketEvent.SortChanged(tab.sort()));
        return true;
      }
    }
    for (MarketLayout.Card card : layout.cards()) {
      if (card.card().contains(x, y)) {
        selectDetail(card.row());
        return true;
      }
    }

    if (detailController != null) {
      if (layout.decrement().contains(x, y)) {
        detailController.handle(new MarketDetailEvent.ActionClicked(MarketDetailAction.DECREMENT));
        syncQuantityValue();
        return true;
      }
      if (layout.increment().contains(x, y)) {
        detailController.handle(new MarketDetailEvent.ActionClicked(MarketDetailAction.INCREMENT));
        syncQuantityValue();
        return true;
      }
      if (layout.all().contains(x, y)) {
        detailController.handle(new MarketDetailEvent.ActionClicked(MarketDetailAction.SELECT_ALL));
        syncQuantityValue();
        return true;
      }
      if (layout.primaryAction().contains(x, y)) {
        detailController.handle(new MarketDetailEvent.ActionClicked(MarketDetailAction.SUBMIT_PRIMARY));
        EconomyServices.platform().network().sendToServer(new BalanceRequestMessage(true));
        return true;
      }
      if (layout.secondaryAction().contains(x, y)) {
        detailController.handle(new MarketDetailEvent.ActionClicked(MarketDetailAction.SUBMIT_SECONDARY));
        return true;
      }
    }

    if (layout.createSales().contains(x, y)) {
      clearDetail();
      controller.handle(new MarketEvent.ActionClicked(MarketAction.CREATE_SALES, null));
      return true;
    }
    if (layout.createDemand().contains(x, y)) {
      clearDetail();
      controller.handle(new MarketEvent.ActionClicked(MarketAction.CREATE_DEMAND, null));
      return true;
    }
    if (controller.state().screenState() == ScreenState.ERROR && layout.retryButton().contains(x, y)) {
      controller.handle(new MarketEvent.Retry(System.nanoTime()));
      return true;
    }
    if (controller.state().totalPages() > 1 && layout.previousButton().contains(x, y)) {
      clearDetail();
      controller.handle(new MarketEvent.PreviousPage());
      return true;
    }
    if (controller.state().totalPages() > 1 && layout.nextButton().contains(x, y)) {
      clearDetail();
      controller.handle(new MarketEvent.NextPage());
      return true;
    }
    if (layout.esc().contains(x, y)) {
      controller.handle(new MarketEvent.ActionClicked(MarketAction.BACK, null));
      return true;
    }

    if (layout.catalogArea().contains(x, y)
        && (search == null || !layout.search().contains(x, y))) {
      clearDetail();
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    if (delta != 0 && controller.state().totalPages() > 1) {
      clearDetail();
      controller.handle(new MarketEvent.Scroll(delta < 0 ? 1 : -1));
      return true;
    }
    return super.mouseScrolled(mouseX, mouseY, delta);
  }

  @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == 256) {
      onClose();
      return true;
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override public void onClose() {
    if (minecraft == null) return;
    if (parent != null) minecraft.setScreen(parent);
    else minecraft.setScreen(new Forge1201HomeScreen());
  }

  @Override public boolean isPauseScreen() { return false; }
  @Override public void renderBackground(GuiGraphics graphics) {}

  private void selectDetail(MarketRow row) {
    detailInvalidUntilNanos = 0;
    detailController = new MarketDetailController(row, controller.viewerId(),
        controller.viewerCanModerate(), detailPort);
    refreshDetailFacts();
    syncQuantityVisibility();
    syncQuantityValue();
  }

  private void clearDetail() {
    detailInvalidUntilNanos = 0;
    detailController = null;
    if (quantity != null) {
      quantity.setFocused(false);
      syncingQuantity = true;
      quantity.setValue("");
      syncingQuantity = false;
    }
    syncQuantityVisibility();
  }

  private void reconcileSelectedRow() {
    if (detailController == null) return;
    UUID selected = detailController.state().row().order().tradeId();
    MarketRow refreshed = controller.state().find(selected);
    if (refreshed == null) {
      detailController.handle(new MarketDetailEvent.OrderInvalidated());
      detailInvalidUntilNanos = System.nanoTime() + DETAIL_INVALID_NANOS;
      syncQuantityVisibility();
      return;
    }
    detailInvalidUntilNanos = 0;
    detailController.handle(new MarketDetailEvent.RowRefreshed(refreshed));
    refreshDetailFacts();
  }

  private void refreshDetailFacts() {
    if (detailController == null) return;
    Player player = minecraft == null ? null : minecraft.player;
    if (player == null || minecraft.level == null) return;
    int balance = Math.max(0, ClientBalanceState.snapshot().balance());
    int capacity = 0;
    int matching = 0;
    try {
      ItemStack template = Forge1201Platform.nativeItemStacks()
          .restoreSnapshot(detailController.state().row().order().item(), minecraft.level.registryAccess())
          .orElseThrow();
      for (ItemStack stack : player.getInventory().items) {
        if (stack.isEmpty()) {
          capacity = saturatingAdd(capacity, template.getMaxStackSize());
        } else if (Forge1201Platform.nativeItemStacks().sameItemAndData(stack, template)) {
          matching = saturatingAdd(matching, stack.getCount());
          capacity = saturatingAdd(capacity, Math.max(0, stack.getMaxStackSize() - stack.getCount()));
        }
      }
    } catch (RuntimeException ignored) {
      capacity = 0;
      matching = 0;
    }
    int headroom = Math.max(0, EconomyLedger.MAX_BALANCE - balance);
    detailController.handle(new MarketDetailEvent.FactsChanged(balance, capacity, matching, headroom));
    syncQuantityVisibility();
  }

  private void syncQuantityVisibility() {
    if (quantity == null) return;
    boolean visible = detailController != null && detailController.state().quantityMode();
    quantity.visible = visible;
    quantity.active = visible && detailController.state().partialSupported();
  }

  private void syncQuantityValue() {
    if (quantity == null || detailController == null || !detailController.state().quantityMode()) return;
    String value = Integer.toString(Math.max(1, detailController.state().quantity()));
    if (value.equals(quantity.getValue())) return;
    syncingQuantity = true;
    quantity.setValue(value);
    syncingQuantity = false;
  }

  private int parseQuantity(String value) {
    try { return Integer.parseInt(value.trim()); }
    catch (NumberFormatException ignored) { return 0; }
  }

  private MarketLayout.Layout commonLayout(com.mo.economy_system.ui.text.UiTextMetrics textMetrics) {
    MarketLayout.Layout layout = MarketLayout.calculate(width, height, controller.state(), textMetrics, 1.0f);
    if (layout.pageSize() != controller.state().pageSize()) {
      controller.handle(new MarketEvent.ViewportChanged(layout.pageSize()));
      layout = MarketLayout.calculate(width, height, controller.state(), textMetrics, 1.0f);
    }
    return layout;
  }

  private com.mo.economy_system.ui.text.UiTextMetrics metrics() {
    return new Forge1201UiTextMetrics(font);
  }

  private static String nativeDisplayName(String itemId) {
    ResourceLocation location = ResourceLocation.tryParse(itemId);
    var item = location == null ? null : BuiltInRegistries.ITEM.get(location);
    return item == null ? "" : new ItemStack(item).getHoverName().getString();
  }

  private void syncWidgets(MarketLayout.Layout layout) {
    UiScale scale = layout.scale();
    if (search != null) {
      search.setX(sx(layout.search().x(), scale));
      search.setY(sy(layout.search().y(), scale));
      search.setWidth(sw(layout.search().width(), scale));
      search.setHeight(sh(layout.search().height(), scale));
    }
    if (quantity != null) {
      quantity.setX(sx(layout.quantityInput().x(), scale));
      quantity.setY(sy(layout.quantityInput().y(), scale));
      quantity.setWidth(sw(layout.quantityInput().width(), scale));
      quantity.setHeight(sh(layout.quantityInput().height(), scale));
    }
  }

  private static int sx(int value, UiScale scale) { return Math.round(value * scale.value()); }
  private static int sy(int value, UiScale scale) { return Math.round(value * scale.value()); }
  private static int sw(int value, UiScale scale) { return Math.max(1, Math.round(value * scale.value())); }
  private static int sh(int value, UiScale scale) { return Math.max(1, Math.round(value * scale.value())); }
  private static int saturatingAdd(int left, int right) {
    long result = (long) left + right;
    return result >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
  }

  private final class Port implements MarketPort {
    private long requestId = -1;

    @Override public long nextRequestId() { return ClientMarketState.nextPageRequestId(); }

    @Override public void requestPage(long id, int offset, MarketOrderFilter filter, String query) {
      requestPage(id, offset, filter, MarketOrderSort.DEFAULT, query);
    }

    @Override public void requestPage(
        long id, int offset, MarketOrderFilter filter, MarketOrderSort sort, String query) {
      requestPage(id, offset, filter, sort, query, null);
    }

    @Override public void requestPage(
        long id, int offset, MarketOrderFilter filter, MarketOrderSort sort, String query,
        UUID focusTradeId) {
      requestId = id;
      EconomyServices.platform().network().sendToServer(new MarketDataRequestMessage(
          id, MarketDataRequestPurpose.PAGE, offset, MarketController.NETWORK_PAGE_SIZE,
          filter, sort, query, focusTradeId));
    }

    @Override public void submit(MarketAction action, MarketRow row) {
      detailPort.submit(action, row, 0);
    }

    @Override public void confirm(MarketAction action, MarketRow row) {
      selectDetail(row);
    }

    @Override public void create(MarketAction action) {
      if (minecraft == null) return;
      if (action == MarketAction.CREATE_SALES) {
        minecraft.setScreen(new Forge1201MarketCreateScreen(
            MarketCreateMode.SALES, Forge1201MarketScreen.this));
      } else if (action == MarketAction.CREATE_DEMAND) {
        minecraft.setScreen(new Forge1201MarketCreateScreen(
            MarketCreateMode.DEMAND, Forge1201MarketScreen.this));
      }
    }
  }

  private final class DetailPort implements MarketDetailPort {
    @Override public void submit(MarketAction action, MarketRow row, int requestedQuantity) {
      UUID id = row.order().tradeId();
      switch (action) {
        case BUY -> EconomyServices.platform().network().sendToServer(
            new PurchaseSalesOrderMessage(id, requestedQuantity));
        case DELIVER_DEMAND -> EconomyServices.platform().network().sendToServer(
            new DeliverDemandOrderMessage(id, requestedQuantity));
        case REMOVE_SALES, ADMIN_REMOVE_SALES -> EconomyServices.platform().network().sendToServer(
            new RemoveSalesOrderMessage(id));
        case CONFIRM_DEMAND -> EconomyServices.platform().network().sendToServer(
            new ConfirmDemandOrderMessage(id));
        case REMOVE_DEMAND -> EconomyServices.platform().network().sendToServer(
            new RemoveDemandOrderMessage(id));
        default -> { }
      }
    }
  }
}

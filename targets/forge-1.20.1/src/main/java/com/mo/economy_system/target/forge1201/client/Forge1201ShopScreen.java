package com.mo.economy_system.target.forge1201.client;

import com.mo.economy_system.common.client.ClientBalanceState;
import com.mo.economy_system.common.client.ClientShopState;
import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.common.network.BalanceRequestMessage;
import com.mo.economy_system.common.network.ShopBuyItemMessage;
import com.mo.economy_system.common.network.ShopDataRequestMessage;
import com.mo.economy_system.common.network.ShopItemSnapshot;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.shop.ShopAction;
import com.mo.economy_system.ui.shop.ShopController;
import com.mo.economy_system.ui.shop.ShopEvent;
import com.mo.economy_system.ui.shop.ShopInlinePurchaseView;
import com.mo.economy_system.ui.shop.ShopLayout;
import com.mo.economy_system.ui.shop.ShopPort;
import com.mo.economy_system.ui.shop.ShopPurchaseAction;
import com.mo.economy_system.ui.shop.ShopPurchaseController;
import com.mo.economy_system.ui.shop.ShopPurchaseEvent;
import com.mo.economy_system.ui.shop.ShopPurchasePort;
import com.mo.economy_system.ui.shop.ShopRow;
import com.mo.economy_system.ui.shop.ShopView;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Forge shell for the common 3/4 catalog + 1/4 inline-purchase shop. */
public final class Forge1201ShopScreen extends Screen {
  private static final AtomicLong IDS = new AtomicLong(1);

  private final Screen parent;
  private final Port port = new Port();
  private final PurchasePort purchasePort = new PurchasePort();
  private final ShopController controller =
      new ShopController(port, Forge1201ShopScreen::nativeDisplayName);

  private EditBox search;
  private EditBox quantity;
  private ShopPurchaseController purchaseController;
  private long appliedRevision = -1;

  public Forge1201ShopScreen() {
    this(null);
  }

  public Forge1201ShopScreen(Screen parent) {
    super(Component.translatable(EconomyUiRoute.SHOP.titleKey()));
    this.parent = parent;
  }

  @Override protected void init() {
    String searchValue = search == null ? "" : search.getValue();
    int quantityValue = purchaseController == null ? 1 : purchaseController.state().quantity();
    ShopLayout.Layout layout = commonLayout(metrics());
    UiScale scale = layout.scale();

    search = new Forge1201UnderlinedEditBox(font,
        Math.round(layout.search().x() * scale.value()),
        Math.round(layout.search().y() * scale.value()),
        Math.max(1, Math.round(layout.search().width() * scale.value())),
        Math.max(1, Math.round(layout.search().height() * scale.value())),
        Component.translatable("screen.shop.search"));
    Forge1201UiInputAdapter.apply(search);
    search.setMaxLength(com.mo.economy_system.ui.text.UiSearchPolicy.SEARCH_MAX_LENGTH);
    search.setHint(Component.translatable("text.shop.search_hint"));
    search.setFocused(false);
    search.setValue(searchValue);
    search.setResponder(text -> {
      controller.handle(new ShopEvent.FilterChanged(text));
      clearPurchase();
    });
    addRenderableWidget(search);

    quantity = new Forge1201UnderlinedEditBox(font,
        Math.round(layout.purchaseQuantity().x() * scale.value()),
        Math.round(layout.purchaseQuantity().y() * scale.value()),
        Math.max(1, Math.round(layout.purchaseQuantity().width() * scale.value())),
        Math.max(1, Math.round(layout.purchaseQuantity().height() * scale.value())),
        Component.translatable("screen.shop.purchase.quantity"));
    Forge1201UiInputAdapter.apply(quantity);
    quantity.setMaxLength(9);
    quantity.setHint(Component.translatable("screen.shop.purchase.quantity"));
    quantity.setValue(Integer.toString(quantityValue));
    quantity.setResponder(value -> {
      if (purchaseController != null) {
        purchaseController.handle(new ShopPurchaseEvent.QuantityChanged(parse(value)));
      }
    });
    addRenderableWidget(quantity);
    syncQuantityVisibility();

    if (controller.state().screenState() == ScreenState.IDLE) {
      controller.handle(new ShopEvent.Initialize(System.nanoTime()));
    }
    EconomyServices.platform().network().sendToServer(new BalanceRequestMessage(true));
  }

  private int parse(String value) {
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException ignored) {
      return 0;
    }
  }

  @Override public void tick() {
    super.tick();
    controller.handle(new ShopEvent.Tick(System.nanoTime()));
    ClientShopState.Snapshot snapshot = ClientShopState.snapshot();
    if (snapshot.revision() != appliedRevision && port.requestId >= 0) {
      appliedRevision = snapshot.revision();
      String selectedShopItemId = purchaseController == null ? null
          : purchaseController.state().row().item().shopItemId();
      controller.handle(new ShopEvent.DataLoaded(port.requestId, snapshot.items()));
      if (selectedShopItemId != null) {
        ShopRow refreshed = controller.state().find(selectedShopItemId);
        if (refreshed == null) clearPurchase();
        else purchaseController.handle(new ShopPurchaseEvent.ItemRefreshed(refreshed));
      }
    }
    if (purchaseController != null) {
      purchaseController.handle(new ShopPurchaseEvent.FactsChanged(
          purchasePort.availableQuantity(purchaseController.state().row()),
          purchasePort.currentBalance()));
    }
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
    renderer.fillPhysicalBackground(width, height, ShopLayout.BACKGROUND_COLOR);
    ShopLayout.Layout layout = commonLayout(renderer.metrics());
    UiScale scale = layout.scale();
    syncWidgets(layout);

    if (search != null) {
      UiRect rect = new UiRect(search.getX(), search.getY(), search.getWidth(), search.getHeight());
      ShopView.renderSearchFrame(renderer, rect, search.isFocused(), rect.contains(mouseX, mouseY));
    }
    if (quantity != null && quantity.visible) {
      UiRect rect = new UiRect(quantity.getX(), quantity.getY(), quantity.getWidth(), quantity.getHeight());
      ShopInlinePurchaseView.renderQuantityFrame(renderer, rect, quantity.isFocused(),
          rect.contains(mouseX, mouseY), purchaseController != null
              && purchaseController.state().quantityInputError());
    }

    graphics.pose().pushPose();
    graphics.pose().scale(scale.value(), scale.value(), 1.0f);
    ShopView.render(renderer, controller.state(), layout,
        purchaseController == null ? null : purchaseController.state(),
        scale.toVirtualX(mouseX), scale.toVirtualY(mouseY));
    graphics.pose().popPose();
    super.render(graphics, mouseX, mouseY, partialTick);
  }

  @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
    ShopLayout.Layout layout = commonLayout(metrics());
    int x = layout.scale().toVirtualX(mouseX);
    int y = layout.scale().toVirtualY(mouseY);

    for (ShopLayout.Card card : layout.cards()) {
      if (card.card().contains(x, y)) {
        selectPurchase(card.row());
        return true;
      }
    }

    if (purchaseController != null && layout.purchaseConfirm().contains(x, y)) {
      purchaseController.handle(new ShopPurchaseEvent.ActionClicked(ShopPurchaseAction.CONFIRM));
      EconomyServices.platform().network().sendToServer(new BalanceRequestMessage(true));
      return true;
    }

    if (controller.state().screenState() == ScreenState.ERROR && layout.message().contains(x, y)) {
      controller.handle(new ShopEvent.Retry(System.nanoTime()));
      clearPurchase();
      return true;
    }
    if (controller.state().totalPages() > 1 && layout.previousButton().contains(x, y)) {
      controller.handle(new ShopEvent.PreviousPage());
      clearPurchase();
      return true;
    }
    if (controller.state().totalPages() > 1 && layout.nextButton().contains(x, y)) {
      controller.handle(new ShopEvent.NextPage());
      clearPurchase();
      return true;
    }
    if (layout.esc().contains(x, y)) {
      controller.handle(new ShopEvent.ActionClicked(ShopAction.BACK, null));
      return true;
    }

    if (!layout.purchasePanel().contains(x, y) && !layout.search().contains(x, y)) {
      clearPurchase();
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    if (delta != 0 && controller.state().totalPages() > 1) {
      controller.handle(new ShopEvent.Scroll(delta < 0 ? 1 : -1));
      clearPurchase();
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

  @Override public boolean isPauseScreen() {
    return false;
  }

  @Override public void renderBackground(GuiGraphics graphics) {}

  private void selectPurchase(ShopRow row) {
    purchaseController = new ShopPurchaseController(row, purchasePort, false);
    if (quantity != null) {
      quantity.setValue("1");
      quantity.setFocused(false);
    }
    syncQuantityVisibility();
  }

  private void clearPurchase() {
    purchaseController = null;
    if (quantity != null) {
      quantity.setFocused(false);
      quantity.setValue("");
    }
    syncQuantityVisibility();
  }

  private void syncQuantityVisibility() {
    if (quantity == null) return;
    boolean visible = purchaseController != null;
    quantity.visible = visible;
    quantity.active = visible;
  }

  private ShopLayout.Layout commonLayout(com.mo.economy_system.ui.text.UiTextMetrics metrics) {
    ShopLayout.Layout layout = ShopLayout.calculate(width, height, controller.state(), metrics, 1.0f);
    if (layout.pageSize() != controller.state().pageSize()) {
      controller.handle(new ShopEvent.ViewportChanged(layout.pageSize()));
      layout = ShopLayout.calculate(width, height, controller.state(), metrics, 1.0f);
    }
    return layout;
  }

  private com.mo.economy_system.ui.text.UiTextMetrics metrics() {
    return new Forge1201UiTextMetrics(font);
  }

  private void syncWidgets(ShopLayout.Layout layout) {
    UiScale scale = layout.scale();
    if (search != null) {
      search.setX(Math.round(layout.search().x() * scale.value()));
      search.setY(Math.round(layout.search().y() * scale.value()));
      search.setWidth(Math.max(1, Math.round(layout.search().width() * scale.value())));
      search.setHeight(Math.max(1, Math.round(layout.search().height() * scale.value())));
    }
    if (quantity != null) {
      quantity.setX(Math.round(layout.purchaseQuantity().x() * scale.value()));
      quantity.setY(Math.round(layout.purchaseQuantity().y() * scale.value()));
      quantity.setWidth(Math.max(1, Math.round(layout.purchaseQuantity().width() * scale.value())));
      quantity.setHeight(Math.max(1, Math.round(layout.purchaseQuantity().height() * scale.value())));
    }
  }

  private static String nativeDisplayName(ShopItemSnapshot snapshot) {
    ResourceLocation location = ResourceLocation.tryParse(snapshot.itemId());
    var item = location == null ? null : BuiltInRegistries.ITEM.get(location);
    return item == null ? "" : new ItemStack(item).getHoverName().getString();
  }

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
      if (action == ShopAction.BUY) {
        EconomyServices.platform().network().sendToServer(
            new ShopBuyItemMessage(row.item().shopItemId(), quantity));
      }
    }

    @Override public void confirm(ShopRow row) {
      selectPurchase(row);
    }
  }

  private final class PurchasePort implements ShopPurchasePort {
    @Override public int availableQuantity(ShopRow row) {
      Player player = minecraft == null ? null : minecraft.player;
      ResourceLocation id = ResourceLocation.tryParse(row.item().itemId());
      if (player == null || id == null || !BuiltInRegistries.ITEM.containsKey(id)) return 0;
      Item item = BuiltInRegistries.ITEM.get(id);
      int max = item.getMaxStackSize();
      int available = 0;
      for (ItemStack stack : player.getInventory().items) {
        if (stack.isEmpty()) available += max;
        else if (stack.getItem() == item) {
          available += Math.max(0, stack.getMaxStackSize() - stack.getCount());
        }
      }
      return available;
    }

    @Override public int currentBalance() {
      return Math.max(0, ClientBalanceState.snapshot().balance());
    }

    @Override public void submit(ShopRow row, int quantity) {
      EconomyServices.platform().network().sendToServer(
          new ShopBuyItemMessage(row.item().shopItemId(), quantity));
    }
  }
}

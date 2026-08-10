package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.client.ClientDeliveryBoxState;
import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot;
import com.mo.economy_system.common.network.DeliveryBoxClaimMessage;
import com.mo.economy_system.common.network.DeliveryBoxDataRequestMessage;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import com.mo.economy_system.ui.delivery.DeliveryAction;
import com.mo.economy_system.ui.delivery.DeliveryController;
import com.mo.economy_system.ui.delivery.DeliveryEvent;
import com.mo.economy_system.ui.delivery.DeliveryLayout;
import com.mo.economy_system.ui.delivery.DeliveryPort;
import com.mo.economy_system.ui.delivery.DeliveryView;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.geometry.UiRect;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** NeoForge Screen shell for the loader-neutral delivery-box page. */
public final class NeoForge1211DeliveryBoxScreen extends Screen {
  private static final AtomicLong IDS = new AtomicLong(1);
  private final Screen parent;
  private final Port port = new Port();
  private final DeliveryController controller = new DeliveryController(port, NeoForge1211DeliveryBoxScreen::nativeDisplayName);
  private EditBox search;
  private long appliedRevision = -1;

  public NeoForge1211DeliveryBoxScreen() { this(null); }
  public NeoForge1211DeliveryBoxScreen(Screen parent) {
    super(Component.translatable(EconomyUiRoute.DELIVERY_BOX.titleKey()));
    this.parent = parent;
  }

  @Override protected void init() {
    String value = search == null ? "" : search.getValue();
    DeliveryLayout.Layout layout = commonLayout();
    UiScale scale = layout.scale();
    search = new EditBox(font, Math.round(layout.search().x() * scale.value()),
        Math.round(layout.search().y() * scale.value()),
        Math.max(1, Math.round(layout.search().width() * scale.value())),
        Math.max(1, Math.round(layout.search().height() * scale.value())),
        Component.translatable("screen.delivery_box.search"));
    search.setMaxLength(com.mo.economy_system.ui.text.UiSearchPolicy.SEARCH_MAX_LENGTH);
    search.setHint(Component.translatable("text.delivery_box.hint"));
    search.setFocused(false);
    search.setValue(value);
    search.setResponder(text -> controller.handle(new DeliveryEvent.FilterChanged(text)));
    addRenderableWidget(search);
    if (controller.state().screenState() == ScreenState.IDLE) {
      controller.handle(new DeliveryEvent.Initialize(System.nanoTime()));
    }
  }

  @Override public void tick() {
    super.tick();
    controller.handle(new DeliveryEvent.Tick(System.nanoTime()));
    ClientDeliveryBoxState.Snapshot snapshot = ClientDeliveryBoxState.snapshot();
    if (snapshot.revision() != appliedRevision && snapshot.requestId() == port.requestId) {
      appliedRevision = snapshot.revision();
      if (snapshot.failed()) controller.handle(new DeliveryEvent.DataFailed(snapshot.requestId(),
          "screen.delivery_box.sync_failed"));
      else controller.handle(new DeliveryEvent.DataLoaded(snapshot.requestId(), snapshot.entries()));
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
    renderer.fillPhysicalBackground(width, height, DeliveryLayout.BACKGROUND_COLOR);
    DeliveryLayout.Layout layout = commonLayout();
    UiScale scale = layout.scale();
    syncSearchWidget(layout);
    if (search != null) DeliveryView.renderSearchFrame(renderer,
        new UiRect(search.getX(), search.getY(), search.getWidth(), search.getHeight()), search.isFocused());
    graphics.pose().pushPose();
    graphics.pose().scale(scale.value(), scale.value(), 1.0f);
    DeliveryView.render(renderer, controller.state(), layout,
        scale.toVirtualX(mouseX), scale.toVirtualY(mouseY));
    graphics.pose().popPose();
    super.render(graphics, mouseX, mouseY, partialTick);
  }

  @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
    DeliveryLayout.Layout layout = commonLayout();
    int x = layout.scale().toVirtualX(mouseX), y = layout.scale().toVirtualY(mouseY);
    for (DeliveryLayout.Card card : layout.cards()) if (card.claimButton().contains(x, y)) {
      controller.handle(new DeliveryEvent.ActionClicked(DeliveryAction.CLAIM, card.row().entryId(), System.nanoTime()));
      return true;
    }
    if (controller.state().screenState() == ScreenState.ERROR && layout.message().contains(x, y)) {
      controller.handle(new DeliveryEvent.ActionClicked(DeliveryAction.RETRY, null, System.nanoTime()));
      return true;
    }
    if (controller.state().totalPages() > 1 && layout.previousButton().contains(x, y)) { controller.handle(new DeliveryEvent.PreviousPage()); return true; }
    if (controller.state().totalPages() > 1 && layout.nextButton().contains(x, y)) { controller.handle(new DeliveryEvent.NextPage()); return true; }
    if (layout.esc().contains(x, y)) {
      controller.handle(new DeliveryEvent.ActionClicked(DeliveryAction.BACK, null, System.nanoTime()));
      return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    if (scrollY != 0 && controller.state().totalPages() > 1) {
      controller.handle(new DeliveryEvent.Scroll(scrollY < 0 ? 1 : -1));
      return true;
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

  private DeliveryLayout.Layout commonLayout() {
    DeliveryLayout.Layout layout = DeliveryLayout.calculate(width, height, controller.state(), metrics());
    if (layout.pageSize() != controller.state().pageSize()) {
      controller.handle(new DeliveryEvent.ViewportChanged(layout.pageSize()));
      layout = DeliveryLayout.calculate(width, height, controller.state(), metrics());
    }
    return layout;
  }
  private void syncSearchWidget(DeliveryLayout.Layout layout) {
    if (search == null) return;
    UiScale scale = layout.scale();
    search.setX(Math.round(layout.search().x() * scale.value()));
    search.setY(Math.round(layout.search().y() * scale.value()));
    search.setWidth(Math.max(1, Math.round(layout.search().width() * scale.value())));
    search.setHeight(Math.max(1, Math.round(layout.search().height() * scale.value())));
  }
  private static String nativeDisplayName(DeliveryBoxEntrySnapshot entry) {
    ResourceLocation location = ResourceLocation.tryParse(entry.item().itemId());
    var item = location == null ? null : BuiltInRegistries.ITEM.get(location);
    return item == null ? "" : new ItemStack(item).getHoverName().getString();
  }
  private com.mo.economy_system.ui.text.UiTextMetrics metrics() {
    return new NeoForge1211UiTextMetrics(font);
  }

  private final class Port implements DeliveryPort {
    private long requestId = -1;
    @Override public long nextRequestId() {
      long id = IDS.getAndIncrement();
      if (id < 0) throw new IllegalStateException("delivery request id exhausted");
      return id;
    }
    @Override public void requestData(long id) {
      requestId = id;
      EconomyServices.platform().network().sendToServer(new DeliveryBoxDataRequestMessage(id));
    }
    @Override public void claim(UUID entryId, long id) {
      EconomyServices.platform().network().sendToServer(new DeliveryBoxClaimMessage(entryId, id));
    }
  }
}

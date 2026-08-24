package com.mo.economy_system.target.forge1201.client;

import com.mo.economy_system.common.client.ClientMailboxState;
import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.common.mail.MailSnapshot;
import com.mo.economy_system.common.network.MailboxClaimAllMessage;
import com.mo.economy_system.common.network.MailboxClaimAttachmentMessage;
import com.mo.economy_system.common.network.MailboxDataRequestMessage;
import com.mo.economy_system.common.network.MailboxDeleteMessage;
import com.mo.economy_system.common.network.MailboxMarkReadMessage;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import com.mo.economy_system.ui.delivery.DeliveryAction;
import com.mo.economy_system.ui.delivery.DeliveryAttachmentScroller;
import com.mo.economy_system.ui.delivery.DeliveryController;
import com.mo.economy_system.ui.delivery.DeliveryEvent;
import com.mo.economy_system.ui.delivery.DeliveryLayout;
import com.mo.economy_system.ui.delivery.DeliveryPort;
import com.mo.economy_system.ui.delivery.DeliveryView;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Forge Screen shell for the full mailbox page. */
public final class Forge1201DeliveryBoxScreen extends Screen {
  private static final AtomicLong IDS = new AtomicLong(1);
  private final Screen parent;
  private final Port port = new Port();
  private final DeliveryController controller = new DeliveryController(port, Forge1201DeliveryBoxScreen::nativeDisplayName);
  private final DeliveryAttachmentScroller attachmentScroller = new DeliveryAttachmentScroller();
  private EditBox search;
  private long appliedRevision = -1;
  private long observedInvalidationRevision = -1;
  private boolean silentRefreshInFlight;
  private long silentRefreshTargetRevision = -1;
  private long silentRefreshStartedAt;

  public Forge1201DeliveryBoxScreen() { this(null); }
  public Forge1201DeliveryBoxScreen(Screen parent) {
    super(Component.translatable(EconomyUiRoute.DELIVERY_BOX.titleKey()));
    this.parent = parent;
  }

  @Override protected void init() {
    String value = search == null ? "" : search.getValue();
    DeliveryLayout.Layout layout = commonLayout();
    UiScale scale = layout.scale();
    search = new Forge1201UnderlinedEditBox(font, Math.round(layout.search().x() * scale.value()),
        Math.round(layout.search().y() * scale.value()), Math.max(1, Math.round(layout.search().width() * scale.value())),
        Math.max(1, Math.round(layout.search().height() * scale.value())), Component.translatable("screen.delivery_box.search"));
    Forge1201UiInputAdapter.apply(search);
    search.setMaxLength(com.mo.economy_system.ui.text.UiSearchPolicy.SEARCH_MAX_LENGTH);
    search.setHint(Component.translatable("text.delivery_box.hint"));
    search.setFocused(false);
    search.setValue(value);
    search.setResponder(text -> controller.handle(new DeliveryEvent.FilterChanged(text)));
    addRenderableWidget(search);
    if (controller.state().screenState() == ScreenState.IDLE) {
      observedInvalidationRevision = ClientMailboxState.invalidationRevision();
      controller.handle(new DeliveryEvent.Initialize(System.nanoTime()));
    }
  }

  @Override public void tick() {
    super.tick();
    long now = System.nanoTime();
    controller.handle(new DeliveryEvent.Tick(now));

    long invalidationRevision = ClientMailboxState.invalidationRevision();
    if (!silentRefreshInFlight && invalidationRevision != observedInvalidationRevision) {
      if (controller.state().requestId() >= 0
          && controller.state().screenState() != ScreenState.LOADING) {
        long refreshRequestId = port.nextRequestId();
        controller.handle(new DeliveryEvent.RefreshStarted(refreshRequestId));
        silentRefreshInFlight = true;
        silentRefreshTargetRevision = invalidationRevision;
        silentRefreshStartedAt = now;
        port.requestData(refreshRequestId);
      } else if (controller.state().screenState() == ScreenState.ERROR) {
        observedInvalidationRevision = invalidationRevision;
        controller.handle(new DeliveryEvent.Retry(now));
      }
    }

    ClientMailboxState.Snapshot snapshot = ClientMailboxState.snapshot();
    if (snapshot.revision() != appliedRevision && snapshot.requestId() == port.requestId) {
      appliedRevision = snapshot.revision();
      if (snapshot.failed()) {
        if (silentRefreshInFlight) {
          controller.handle(new DeliveryEvent.RefreshFailed(snapshot.requestId()));
          observedInvalidationRevision = silentRefreshTargetRevision;
          silentRefreshInFlight = false;
        } else if (controller.state().screenState() == ScreenState.LOADING) {
          controller.handle(new DeliveryEvent.DataFailed(snapshot.requestId(), "screen.delivery_box.sync_failed"));
        }
      } else {
        controller.handle(new DeliveryEvent.DataLoaded(snapshot.requestId(), snapshot.mails()));
        if (silentRefreshInFlight) {
          observedInvalidationRevision = silentRefreshTargetRevision;
          silentRefreshInFlight = false;
        }
      }
    }

    if (silentRefreshInFlight && now - silentRefreshStartedAt >= DeliveryController.TIMEOUT_NANOS) {
      controller.handle(new DeliveryEvent.RefreshFailed(port.requestId));
      observedInvalidationRevision = silentRefreshTargetRevision;
      silentRefreshInFlight = false;
    }
    controller.pollNavigation().ifPresent(this::navigate);
  }

  private void navigate(UiNavigation navigation) {
    if (minecraft == null) return;
    if (navigation instanceof UiNavigation.Route route) {
      if (route.route() == EconomyUiRoute.HOME) minecraft.setScreen(new Forge1201HomeScreen());
      else if (route.route() == EconomyUiRoute.MAIL_COMPOSE) minecraft.setScreen(new Forge1201MailboxComposeScreen(this));
    } else if (navigation instanceof UiNavigation.Back) onClose();
  }

  @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    Forge1201UiRenderer renderer = new Forge1201UiRenderer(graphics, font);
    renderer.fillPhysicalBackground(width, height, DeliveryLayout.BACKGROUND_COLOR);
    DeliveryLayout.Layout layout = commonLayout();
    UiScale scale = layout.scale();
    syncSearchWidget(layout);
    if (search != null) {
      UiRect searchRect = new UiRect(search.getX(), search.getY(), search.getWidth(), search.getHeight());
      DeliveryView.renderSearchFrame(renderer, searchRect, search.isFocused(), searchRect.contains(mouseX, mouseY));
    }
    graphics.pose().pushPose();
    graphics.pose().scale(scale.value(), scale.value(), 1.0f);
    DeliveryView.render(renderer, controller.state(), layout, scale.toVirtualX(mouseX), scale.toVirtualY(mouseY));
    graphics.pose().popPose();
    super.render(graphics, mouseX, mouseY, partialTick);
  }

  @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
    DeliveryLayout.Layout layout = commonLayout();
    int x = layout.scale().toVirtualX(mouseX), y = layout.scale().toVirtualY(mouseY);
    var selected = controller.state().selectedRow();
    int attachmentTotal = selected == null ? 0 : selected.mail().attachments().size();
    if (attachmentScroller.press(x, y, layout.attachmentScrollTrack(), layout.attachmentScrollThumb(),
        attachmentTotal, layout.attachmentVisibleCapacity())) return true;
    for (DeliveryLayout.AttachmentCard card : layout.attachmentCards()) if (controller.state().can(DeliveryAction.CLAIM)
        && !card.attachment().claimed() && card.card().contains(x, y)) {
      controller.handle(new DeliveryEvent.ActionClicked(DeliveryAction.CLAIM, card.attachment().entryId(), System.nanoTime()));
      return true;
    }
    if (controller.state().can(DeliveryAction.CLAIM_ALL) && layout.claimAllButton().contains(x, y)
        && controller.state().selectedRow() != null
        && controller.state().selectedRow().mail().hasUnclaimedAttachments()) {
      controller.handle(new DeliveryEvent.ActionClicked(DeliveryAction.CLAIM_ALL, null, System.nanoTime())); return true;
    }
    if (controller.state().can(DeliveryAction.DELETE) && layout.deleteButton().contains(x, y)
        && controller.state().selectedRow() != null
        && !controller.state().selectedRow().mail().hasUnclaimedAttachments()) {
      controller.handle(new DeliveryEvent.ActionClicked(DeliveryAction.DELETE, null, System.nanoTime())); return true;
    }
    if (layout.composeButton().contains(x, y)) {
      controller.handle(new DeliveryEvent.ActionClicked(DeliveryAction.COMPOSE, null, System.nanoTime())); return true;
    }
    for (DeliveryLayout.CategoryTab tab : layout.categoryTabs()) if (tab.rect().contains(x, y)) {
      controller.handle(new DeliveryEvent.CategoryChanged(tab.category())); return true;
    }
    for (DeliveryLayout.Card card : layout.cards()) if (card.card().contains(x, y)) {
      controller.handle(new DeliveryEvent.MailSelected(card.row().mailId())); return true;
    }
    if (controller.state().screenState() == ScreenState.ERROR && layout.message().contains(x, y)) {
      controller.handle(new DeliveryEvent.ActionClicked(DeliveryAction.RETRY, null, System.nanoTime())); return true;
    }
    if (controller.state().totalPages() > 1 && layout.previousButton().contains(x, y)) { controller.handle(new DeliveryEvent.PreviousPage()); return true; }
    if (controller.state().totalPages() > 1 && layout.nextButton().contains(x, y)) { controller.handle(new DeliveryEvent.NextPage()); return true; }
    if (layout.esc().contains(x, y)) { controller.handle(new DeliveryEvent.ActionClicked(DeliveryAction.BACK, null, System.nanoTime())); return true; }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    DeliveryLayout.Layout layout = commonLayout();
    int x = layout.scale().toVirtualX(mouseX), y = layout.scale().toVirtualY(mouseY);
    var selected = controller.state().selectedRow();
    int attachmentTotal = selected == null ? 0 : selected.mail().attachments().size();
    if (delta != 0 && layout.attachmentMaxFirstIndex() > 0
        && (layout.attachmentStrip().contains(x, y) || layout.attachmentScrollTrack().contains(x, y))) {
      attachmentScroller.scroll(delta < 0 ? 1 : -1, attachmentTotal, layout.attachmentVisibleCapacity());
      return true;
    }
    if (delta != 0 && controller.state().totalPages() > 1) {
      controller.handle(new DeliveryEvent.Scroll(delta < 0 ? 1 : -1)); return true;
    }
    return super.mouseScrolled(mouseX, mouseY, delta);
  }

  @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
    if (attachmentScroller.dragging()) {
      DeliveryLayout.Layout layout = commonLayout();
      int x = layout.scale().toVirtualX(mouseX);
      var selected = controller.state().selectedRow();
      int attachmentTotal = selected == null ? 0 : selected.mail().attachments().size();
      attachmentScroller.drag(x, layout.attachmentScrollTrack(), layout.attachmentScrollThumb(),
          attachmentTotal, layout.attachmentVisibleCapacity());
      return true;
    }
    return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
  }

  @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
    if (attachmentScroller.release()) return true;
    return super.mouseReleased(mouseX, mouseY, button);
  }

  @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == 256) { onClose(); return true; }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override public void onClose() {
    if (minecraft == null) return;
    if (parent != null) minecraft.setScreen(parent); else minecraft.setScreen(new Forge1201HomeScreen());
  }

  @Override public boolean isPauseScreen() { return false; }

  private DeliveryLayout.Layout commonLayout() {
    var selected = controller.state().selectedRow();
    attachmentScroller.syncMail(selected == null ? null : selected.mailId());
    DeliveryLayout.Layout layout = DeliveryLayout.calculate(
        width, height, controller.state(), metrics(), attachmentScroller.firstIndex());
    if (layout.pageSize() != controller.state().pageSize()) {
      controller.handle(new DeliveryEvent.ViewportChanged(layout.pageSize()));
      selected = controller.state().selectedRow();
      attachmentScroller.syncMail(selected == null ? null : selected.mailId());
      layout = DeliveryLayout.calculate(
          width, height, controller.state(), metrics(), attachmentScroller.firstIndex());
    }
    int total = selected == null ? 0 : selected.mail().attachments().size();
    if (attachmentScroller.clamp(total, layout.attachmentVisibleCapacity())) {
      layout = DeliveryLayout.calculate(
          width, height, controller.state(), metrics(), attachmentScroller.firstIndex());
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

  private static String nativeDisplayName(MailSnapshot mail) {
    if (mail.attachments().isEmpty()) return "";
    ResourceLocation location = ResourceLocation.tryParse(mail.attachments().get(0).item().itemId());
    var item = location == null ? null : BuiltInRegistries.ITEM.get(location);
    return item == null ? "" : new ItemStack(item).getHoverName().getString();
  }

  private com.mo.economy_system.ui.text.UiTextMetrics metrics() { return new Forge1201UiTextMetrics(font); }

  private final class Port implements DeliveryPort {
    private long requestId = -1;
    @Override public long nextRequestId() {
      long id = IDS.getAndIncrement();
      if (id < 0) throw new IllegalStateException("mailbox request id exhausted");
      return id;
    }
    @Override public void requestData(long id) { requestId = id; EconomyServices.platform().network().sendToServer(new MailboxDataRequestMessage(id)); }
    @Override public void markRead(UUID mailId, long id) { EconomyServices.platform().network().sendToServer(new MailboxMarkReadMessage(mailId, id)); }
    @Override public void delete(UUID mailId, long id) { EconomyServices.platform().network().sendToServer(new MailboxDeleteMessage(mailId, id)); }
    @Override public void claim(UUID mailId, UUID entryId, long id) { EconomyServices.platform().network().sendToServer(new MailboxClaimAttachmentMessage(mailId, entryId, id)); }
    @Override public void claimAll(UUID mailId, long id) { EconomyServices.platform().network().sendToServer(new MailboxClaimAllMessage(mailId, id)); }
  }
}

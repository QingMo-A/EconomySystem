package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.client.ClientMailboxSendState;
import com.mo.economy_system.common.client.ClientPlayerListState;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.network.MailboxSendPlayerMessage;
import com.mo.economy_system.common.network.MailboxSendStatus;
import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.common.network.ServerPlayerListRequestMessage;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.ui.delivery.MailboxComposeLayout;
import com.mo.economy_system.ui.delivery.MailboxComposeView;
import com.mo.economy_system.ui.delivery.MailboxRecipientCompletion;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.renderer.UiNativeInputFrame;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** NeoForge player-to-player mailbox composer using the shared Economy UI language. */
public final class NeoForge1211MailboxComposeScreen extends Screen {
  private static final AtomicLong IDS = new AtomicLong(1_000_000);
  private final Screen parent;
  private final Set<Integer> selectedSlots = new LinkedHashSet<>();
  private final MailboxRecipientCompletion recipientCompletion = new MailboxRecipientCompletion();
  private String acceptedRecipientName = "";
  private EditBox recipient;
  private EditBox subject;
  private EditBox body;
  private boolean sending;
  private long sentRequestId = -1;
  private long appliedSendRevision = -1;
  private MailboxSendStatus status;

  public NeoForge1211MailboxComposeScreen(Screen parent) {
    super(Component.translatable("screen.mailbox.compose.title"));
    this.parent = parent;
  }

  @Override protected void init() {
    String recipientValue = recipient == null ? "" : recipient.getValue();
    String subjectValue = subject == null ? "" : subject.getValue();
    String bodyValue = body == null ? "" : body.getValue();
    MailboxComposeLayout.Layout layout = layout();
    recipient = input(layout.recipient(), "screen.mailbox.compose.recipient",
        EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH, recipientValue, layout.scale());
    subject = input(layout.subject(), "screen.mailbox.compose.subject",
        EconomyNetworkLimits.MAX_MAIL_SUBJECT_LENGTH, subjectValue, layout.scale());
    body = input(layout.body(), "screen.mailbox.compose.body",
        EconomyNetworkLimits.MAX_MAIL_BODY_LENGTH, bodyValue, layout.scale());
    recipient.setResponder(value -> {
      recipientCompletion.reset();
      if (!value.equals(acceptedRecipientName)) acceptedRecipientName = "";
    });
    addRenderableWidget(recipient);
    addRenderableWidget(subject);
    addRenderableWidget(body);
    EconomyServices.platform().network().sendToServer(ServerPlayerListRequestMessage.INSTANCE);
  }

  private EditBox input(UiRect rect, String key, int maxLength, String value, UiScale scale) {
    EditBox box = new NeoForge1211UnderlinedEditBox(font,
        Math.round(rect.x() * scale.value()), Math.round(rect.y() * scale.value()),
        Math.max(1, Math.round(rect.width() * scale.value())),
        Math.max(1, Math.round(rect.height() * scale.value())), Component.translatable(key));
    NeoForge1211UiInputAdapter.apply(box);
    box.setMaxLength(maxLength);
    box.setHint(Component.translatable(key));
    box.setValue(value);
    return box;
  }

  @Override public void tick() {
    super.tick();
    ClientMailboxSendState.Snapshot snapshot = ClientMailboxSendState.snapshot();
    if (sentRequestId >= 0 && snapshot.requestId() == sentRequestId
        && snapshot.revision() != appliedSendRevision) {
      appliedSendRevision = snapshot.revision();
      status = snapshot.status();
      if (status == MailboxSendStatus.SUCCESS) onClose();
      else sending = false;
    }
  }

  private void sendMail() {
    if (sending) return;
    long id = IDS.getAndIncrement();
    try {
      MailboxSendPlayerMessage message = new MailboxSendPlayerMessage(
          recipient.getValue(), subject.getValue(), body.getValue(), new ArrayList<>(selectedSlots), id);
      sentRequestId = id;
      status = null;
      sending = true;
      EconomyServices.platform().network().sendToServer(message);
    } catch (IllegalArgumentException invalid) {
      status = MailboxSendStatus.INVALID_CONTENT;
      sending = false;
    }
  }

  @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    NeoForge1211UiRenderer renderer = new NeoForge1211UiRenderer(graphics, font);
    renderer.fillPhysicalBackground(width, height, MailboxComposeLayout.BACKGROUND_COLOR);
    MailboxComposeLayout.Layout layout = layout();
    syncInputs(layout);

    UiScale scale = layout.scale();
    int vx = scale.toVirtualX(mouseX), vy = scale.toVirtualY(mouseY);
    graphics.pose().pushPose();
    graphics.pose().scale(scale.value(), scale.value(), 1.0f);
    MailboxComposeView.render(renderer, layout, selectedSlots, sending, status, vx, vy);
    renderInventoryItems(graphics, layout);
    graphics.pose().popPose();
    renderInputFrame(renderer, recipient, mouseX, mouseY);
    renderInputFrame(renderer, subject, mouseX, mouseY);
    renderInputFrame(renderer, body, mouseX, mouseY);
    syncHints();
    super.render(graphics, mouseX, mouseY, partialTick);

    List<PlayerSummary> suggestions = recipientSuggestions();
    if (!suggestions.isEmpty()) {
      graphics.pose().pushPose();
      graphics.pose().translate(0, 0, 400);
      graphics.pose().scale(scale.value(), scale.value(), 1.0f);
      MailboxComposeView.renderRecipientCompletion(renderer, layout, suggestions,
          recipientCompletion.selection(), vx, vy);
      graphics.pose().popPose();
    }
  }

  private void renderInputFrame(NeoForge1211UiRenderer renderer, EditBox box, int mouseX, int mouseY) {
    if (box == null) return;
    UiRect rect = new UiRect(box.getX(), box.getY(), box.getWidth(), box.getHeight());
    UiNativeInputFrame.render(renderer, rect, EconomyUiTheme.SHOP_SEARCH_FRAME,
        box.isFocused(), rect.contains(mouseX, mouseY));
  }

  private void renderInventoryItems(GuiGraphics graphics, MailboxComposeLayout.Layout layout) {
    Player player = minecraft == null ? null : minecraft.player;
    if (player == null) return;
    for (MailboxComposeLayout.Slot slot : layout.slots()) {
      ItemStack stack = player.getInventory().items.get(slot.slot());
      if (stack.isEmpty()) continue;
      int x = slot.rect().x() + 3, y = slot.rect().y() + 3;
      graphics.renderItem(stack, x, y);
      graphics.renderItemDecorations(font, stack, x, y);
    }
  }

  @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
    MailboxComposeLayout.Layout layout = layout();
    int x = layout.scale().toVirtualX(mouseX), y = layout.scale().toVirtualY(mouseY);
    List<PlayerSummary> suggestions = recipientSuggestions();
    if (!suggestions.isEmpty() && layout.completionDropdown().contains(x, y)) {
      int row = (y - layout.completionDropdown().y()) / MailboxComposeLayout.COMPLETION_ROW_HEIGHT;
      int start = MailboxComposeLayout.completionWindowStart(suggestions.size(), recipientCompletion.selection());
      int index = start + row;
      if (row >= 0 && row < MailboxComposeLayout.COMPLETION_MAX_ROWS && index < suggestions.size()) {
        recipientCompletion.select(index, suggestions.size());
        acceptRecipientSuggestion(suggestions);
        return true;
      }
    }
    Player player = minecraft == null ? null : minecraft.player;
    if (player != null) {
      for (MailboxComposeLayout.Slot slot : layout.slots()) {
        if (!slot.rect().contains(x, y)) continue;
        if (player.getInventory().items.get(slot.slot()).isEmpty()) return true;
        if (selectedSlots.contains(slot.slot())) selectedSlots.remove(slot.slot());
        else if (selectedSlots.size() < EconomyNetworkLimits.MAX_PLAYER_MAIL_ATTACHMENTS) selectedSlots.add(slot.slot());
        return true;
      }
    }
    if (layout.sendButton().contains(x, y)) { sendMail(); return true; }
    if (layout.backButton().contains(x, y) || layout.esc().contains(x, y)) { onClose(); return true; }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    List<PlayerSummary> suggestions = recipientSuggestions();
    if (!suggestions.isEmpty() && recipient != null && recipient.isFocused()) {
      if (keyCode == 265) { recipientCompletion.move(-1, suggestions.size()); return true; }
      if (keyCode == 264) { recipientCompletion.move(1, suggestions.size()); return true; }
      if (keyCode == 258 || keyCode == 257) { acceptRecipientSuggestion(suggestions); return true; }
    }
    if (keyCode == 256) { onClose(); return true; }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    List<PlayerSummary> suggestions = recipientSuggestions();
    if (!suggestions.isEmpty() && recipient != null && recipient.isFocused() && scrollY != 0) {
      recipientCompletion.move(scrollY > 0 ? -1 : 1, suggestions.size());
      return true;
    }
    return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
  }

  private MailboxComposeLayout.Layout layout() {
    return MailboxComposeLayout.calculate(width, height, new NeoForge1211UiTextMetrics(font));
  }

  private void syncInputs(MailboxComposeLayout.Layout layout) {
    syncInput(recipient, layout.recipient(), layout.scale());
    syncInput(subject, layout.subject(), layout.scale());
    syncInput(body, layout.body(), layout.scale());
  }

  private static void syncInput(EditBox box, UiRect rect, UiScale scale) {
    if (box == null) return;
    box.setX(Math.round(rect.x() * scale.value()));
    box.setY(Math.round(rect.y() * scale.value()));
    box.setWidth(Math.max(1, Math.round(rect.width() * scale.value())));
    box.setHeight(Math.max(1, Math.round(rect.height() * scale.value())));
  }

  private void syncHints() {
    syncHint(recipient, "screen.mailbox.compose.recipient");
    syncHint(subject, "screen.mailbox.compose.subject");
    syncHint(body, "screen.mailbox.compose.body");
  }

  private static void syncHint(EditBox box, String key) {
    if (box == null) return;
    box.setHint(box.getValue().isEmpty() && !box.isFocused()
        ? Component.translatable(key) : Component.empty());
  }

  private List<PlayerSummary> recipientSuggestions() {
    if (recipient == null || !recipient.isFocused()) return List.of();
    if (!acceptedRecipientName.isBlank() && acceptedRecipientName.equals(recipient.getValue())) return List.of();
    Player self = minecraft == null ? null : minecraft.player;
    return recipientCompletion.suggestions(ClientPlayerListState.snapshot().players(), recipient.getValue(),
        self == null ? null : self.getUUID());
  }

  private void acceptRecipientSuggestion(List<PlayerSummary> suggestions) {
    if (suggestions == null || suggestions.isEmpty() || recipient == null) return;
    int index = Math.max(0, Math.min(recipientCompletion.selection(), suggestions.size() - 1));
    String playerName = suggestions.get(index).playerName();
    recipient.setValue(playerName);
    acceptedRecipientName = playerName;
    recipient.setFocused(true);
    recipientCompletion.reset();
  }

  @Override public void onClose() {
    if (minecraft != null) minecraft.setScreen(parent == null ? new NeoForge1211DeliveryBoxScreen() : parent);
  }

  @Override public boolean isPauseScreen() { return false; }
  @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
}

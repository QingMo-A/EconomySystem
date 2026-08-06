package com.mo.economy_system.screen.economy_system.deliver_box;

import com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot;
import com.mo.economy_system.common.network.DeliveryBoxClaimMessage;
import com.mo.economy_system.common.network.DeliveryBoxDataRequestMessage;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.target.forge1201.network.Forge1201DeliveryBoxClientState;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Functional Forge 1.20.1 delivery-box screen for protocols 31-33. */
public final class Screen_DeliveryBox extends Screen {
  private static final AtomicLong NEXT_REQUEST_ID = new AtomicLong(1);
  private static final int ROW_HEIGHT = 28;
  private long requestId = -1;
  private int scroll;
  private List<DeliveryBoxEntrySnapshot> entries = List.of();
  private boolean loaded;
  private boolean failed;

  public Screen_DeliveryBox() {
    super(Component.translatable("screen.delivery_box.title"));
  }

  @Override
  protected void init() {
    if (requestId < 0) request();
    rebuildEntryButtons();
  }

  private void request() {
    long value = NEXT_REQUEST_ID.getAndIncrement();
    if (value < 0) throw new IllegalStateException("delivery request id exhausted");
    requestId = value;
    loaded = false;
    failed = false;
    EconomyServices.platform().network().sendToServer(new DeliveryBoxDataRequestMessage(value));
  }

  @Override
  public void tick() {
    super.tick();
    Forge1201DeliveryBoxClientState.Snapshot snapshot =
        Forge1201DeliveryBoxClientState.snapshot();
    if (snapshot.requestId() != requestId) return;
    boolean changed = !loaded || failed != snapshot.failed() || !entries.equals(snapshot.entries());
    loaded = true;
    failed = snapshot.failed();
    entries = snapshot.entries();
    if (changed) {
      scroll = clampScroll(scroll);
      rebuildEntryButtons();
    }
  }

  private void rebuildEntryButtons() {
    clearWidgets();
    if (!loaded || failed) return;
    int visible = visibleRows();
    int count = Math.min(visible, entries.size() - Math.min(scroll, entries.size()));
    for (int index = 0; index < count; index++) {
      DeliveryBoxEntrySnapshot entry = entries.get(scroll + index);
      int y = 48 + index * ROW_HEIGHT;
      addRenderableWidget(
          Button.builder(
                  Component.translatable("button.delivery_box.claim"),
                  ignored ->
                      EconomyServices.platform()
                          .network()
                          .sendToServer(new DeliveryBoxClaimMessage(entry.entryId(), requestId)))
              .bounds(width - 82, y, 64, 20)
              .build());
    }
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    renderBackground(graphics);
    graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFF);
    if (!loaded) {
      graphics.drawCenteredString(
          font, Component.translatable("gui.loadingDotDotDot"), width / 2, height / 2, 0xAAAAAA);
    } else if (failed) {
      graphics.drawCenteredString(
          font,
          Component.translatable("message.delivery_box.load_failed"),
          width / 2,
          height / 2,
          0xFF8080);
    } else if (entries.isEmpty()) {
      graphics.drawCenteredString(
          font,
          Component.translatable("message.delivery_box.empty"),
          width / 2,
          height / 2,
          0xAAAAAA);
    } else {
      int count = Math.min(visibleRows(), entries.size() - scroll);
      for (int index = 0; index < count; index++) {
        DeliveryBoxEntrySnapshot entry = entries.get(scroll + index);
        int y = 52 + index * ROW_HEIGHT;
        String label = entry.item().itemId() + " x" + entry.item().count();
        graphics.drawString(font, label, 18, y, 0xFFFFFF);
        graphics.drawString(font, entry.source(), 18, y + 10, 0xAAAAAA);
      }
      graphics.drawString(
          font,
          Component.literal((scroll + 1) + "-" + (scroll + count) + "/" + entries.size()),
          18,
          height - 16,
          0xAAAAAA);
    }
    super.render(graphics, mouseX, mouseY, partialTick);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    if (loaded && !failed && delta != 0) {
      int updated = clampScroll(scroll + (delta < 0 ? 1 : -1));
      if (updated != scroll) {
        scroll = updated;
        rebuildEntryButtons();
      }
      return true;
    }
    return super.mouseScrolled(mouseX, mouseY, delta);
  }

  private int visibleRows() {
    return Math.max(1, (height - 82) / ROW_HEIGHT);
  }

  private int clampScroll(int value) {
    return Math.max(0, Math.min(value, Math.max(0, entries.size() - visibleRows())));
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }
}

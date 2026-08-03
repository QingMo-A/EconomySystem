package com.mo.economy_system.screen.territory_system;

import com.mo.economy_system.common.client.TerritoryDataClientApplier;
import com.mo.economy_system.common.client.TerritoryRequestIds;
import com.mo.economy_system.common.network.TerritoryDataRequestMessage;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Summary;
import com.mo.economy_system.platform.EconomyServices;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Forge 1.20.1 read-only territory page for migrated protocols 17/18. */
public final class Screen_Territory extends Screen
    implements TerritoryDataClientApplier.TerritoryScreenTarget<Owned, Summary> {
  private static final AtomicLong NEXT_REQUEST_ID = new AtomicLong();
  private static final int TIMEOUT_TICKS = 200;
  private long activeRequestId = -1;
  private int waitTicks;
  private boolean requestStarted;
  private boolean loaded;
  private boolean failed;
  private List<Owned> owned = List.of();
  private List<Summary> authorized = List.of();
  private EditBox search;

  public Screen_Territory() {
    super(Component.translatable("screen.territory.title"));
  }

  @Override protected void init() {
    String value = search == null ? "" : search.getValue();
    search = new EditBox(font, width / 2 - 100, 24, 200, 20, Component.empty());
    search.setMaxLength(50);
    search.setValue(value);
    addRenderableWidget(search);
    if (!requestStarted) requestTerritoryData();
  }

  private void requestTerritoryData() {
    long requestId = TerritoryRequestIds.next(NEXT_REQUEST_ID);
    requestStarted = true;
    activeRequestId = requestId;
    waitTicks = 0;
    loaded = false;
    failed = false;
    EconomyServices.platform().network().sendToServer(new TerritoryDataRequestMessage(requestId));
  }

  @Override public void tick() {
    super.tick();
    if (activeRequestId >= 0 && !loaded && ++waitTicks >= TIMEOUT_TICKS) {
      territorySyncFailed(activeRequestId);
    }
  }

  @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    renderBackground(graphics);
    graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
    if (!loaded) {
      graphics.drawCenteredString(font, Component.translatable("gui.loadingDotDotDot"), width / 2, height / 2, 0xAAAAAA);
    } else if (failed) {
      graphics.drawCenteredString(font, Component.translatable("message.territory.sync_failed"), width / 2, height / 2, 0xFF8080);
    } else {
      int y = 58;
      String query = search.getValue().toLowerCase(Locale.ROOT);
      List<Summary> rows = new ArrayList<>();
      for (Owned value : owned) rows.add(value.summary());
      rows.addAll(authorized);
      for (Summary value : rows) {
        if (!query.isEmpty() && !value.name().toLowerCase(Locale.ROOT).contains(query)
            && !value.ownerName().toLowerCase(Locale.ROOT).contains(query)) continue;
        graphics.drawString(font, value.name() + " - " + value.ownerName(), 24, y, 0xFFFFFF);
        y += font.lineHeight + 4;
        if (y > height - 20) break;
      }
    }
    super.render(graphics, mouseX, mouseY, partialTick);
  }

  @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (failed && mouseY >= height / 2 - 10 && mouseY <= height / 2 + 14) {
      requestTerritoryData();
      return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override public boolean acceptsRequest(long requestId) { return requestId == activeRequestId; }

  @Override public void commitTerritoryData(long requestId, List<Owned> owned, List<Summary> authorized) {
    if (!acceptsRequest(requestId)) return;
    this.owned = List.copyOf(owned);
    this.authorized = List.copyOf(authorized);
    this.activeRequestId = -1;
    this.loaded = true;
    this.failed = false;
  }

  @Override public void territorySyncFailed(long requestId) {
    if (!acceptsRequest(requestId)) return;
    activeRequestId = -1;
    loaded = true;
    failed = true;
  }

  @Override public boolean isPauseScreen() { return false; }
}

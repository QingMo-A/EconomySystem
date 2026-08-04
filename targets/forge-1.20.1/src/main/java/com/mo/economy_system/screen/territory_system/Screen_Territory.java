package com.mo.economy_system.screen.territory_system;

import com.mo.economy_system.common.client.TerritoryDataClientApplier;
import com.mo.economy_system.common.client.TerritoryRequestIds;
import com.mo.economy_system.common.network.TerritoryDataRequestMessage;
import com.mo.economy_system.common.network.TeleportToTerritoryMessage;
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

/** Forge 1.20.1 territory page for migrated protocols 17-19. */
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
  private final TerritoryTeleportClickDebounce teleportDebounce = new TerritoryTeleportClickDebounce(8);
  private int scrollRow;
  private List<TerritoryTeleportRowLayout.ButtonArea> teleportButtons = List.of();

  private static final int TELEPORT_BUTTON_WIDTH = 72;
  private static final int TELEPORT_BUTTON_HEIGHT = 20;

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
    teleportDebounce.tick();
    if (activeRequestId >= 0 && !loaded && ++waitTicks >= TIMEOUT_TICKS) {
      territorySyncFailed(activeRequestId);
    }
  }

  @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    teleportButtons = List.of();
    renderBackground(graphics);
    graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
    if (!loaded) {
      graphics.drawCenteredString(font, Component.translatable("gui.loadingDotDotDot"), width / 2, height / 2, 0xAAAAAA);
    } else if (failed) {
      graphics.drawCenteredString(font, Component.translatable("message.territory.sync_failed"), width / 2, height / 2, 0xFF8080);
    } else {
      List<Summary> rows = visibleRows();
      scrollRow = TerritoryTeleportRowLayout.clampScroll(scrollRow, rows.size(), height);
      teleportButtons = TerritoryTeleportRowLayout.layout(rows.stream().map(Summary::territoryId).toList(),
          scrollRow, width, height, TELEPORT_BUTTON_WIDTH, TELEPORT_BUTTON_HEIGHT);
      for (int index = 0; index < teleportButtons.size(); index++) {
        Summary value = rows.get(scrollRow + index);
        TerritoryTeleportRowLayout.ButtonArea area = teleportButtons.get(index);
        int y = area.y() + 5;
        graphics.drawString(font, value.name() + " - " + value.ownerName(), 24, y, 0xFFFFFF);
        int buttonColor = teleportDebounce.ready() ? 0xFF3D6F4A : 0xFF555555;
        graphics.fill(area.x(), area.y(), area.x() + area.width(), area.y() + area.height(), buttonColor);
        graphics.fill(area.x(), area.y(), area.x() + area.width(), area.y() + 1, 0xFF9BC8A4);
        graphics.fill(area.x(), area.y() + area.height() - 1,
            area.x() + area.width(), area.y() + area.height(), 0xFF1B3322);
        graphics.drawCenteredString(font, Component.translatable("button.territory.teleport"),
            area.x() + area.width() / 2, area.y() + 6,
            teleportDebounce.ready() ? 0xFFFFFFFF : 0xFFAAAAAA);
      }
      if (rows.size() > teleportButtons.size()) graphics.drawString(font,
          Component.literal((scrollRow + 1) + "-" + (scrollRow + teleportButtons.size()) + "/" + rows.size()), 24, height - 14, 0xAAAAAA);
    }
    super.render(graphics, mouseX, mouseY, partialTick);
  }

  @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (failed && mouseY >= height / 2 - 10 && mouseY <= height / 2 + 14) {
      requestTerritoryData();
      return true;
    }
    if (button == 0 && loaded && !failed) {
      for (TerritoryTeleportRowLayout.ButtonArea area : teleportButtons) {
        if (area.contains(mouseX, mouseY)) {
          if (teleportDebounce.tryAcquire()) {
            EconomyServices.platform().network().sendToServer(
                new TeleportToTerritoryMessage(area.territoryId()));
          }
          return true;
        }
      }
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    if (loaded && !failed && delta != 0) {
      List<Summary> rows = visibleRows();
      scrollRow = TerritoryTeleportRowLayout.clampScroll(scrollRow + (delta < 0 ? 1 : -1), rows.size(), height);
      return true;
    }
    return super.mouseScrolled(mouseX, mouseY, delta);
  }

  private List<Summary> visibleRows() {
    String query = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT);
    List<Summary> rows = new ArrayList<>();
    for (Owned value : owned) rows.add(value.summary());
    rows.addAll(authorized);
    if (query.isEmpty()) return rows;
    return rows.stream().filter(value -> value.name().toLowerCase(Locale.ROOT).contains(query)
        || value.ownerName().toLowerCase(Locale.ROOT).contains(query)).toList();
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

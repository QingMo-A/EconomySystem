package com.mo.economy_system.screen.territory_system;

import com.mo.economy_system.common.client.ClientPlayerListState;
import com.mo.economy_system.common.client.TerritoryInviteClickDebounce;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.network.InvitePlayerMessage;
import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.common.network.ServerPlayerListRequestMessage;
import com.mo.economy_system.platform.EconomyServices;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Minimal Forge invite screen backed by the loader-neutral player directory. */
public final class Screen_InvitePlayer extends Screen {
  private static final int BUTTON_WIDTH = 72;
  private static final int BUTTON_HEIGHT = 20;
  private static final int BACK_BUTTON_WIDTH = 64;
  private static final int BACK_BUTTON_HEIGHT = 20;

  private final UUID territoryId;
  private final String territoryName;
  private final UUID ownerId;
  private final Set<UUID> existingMemberIds;
  private final Screen returnScreen;
  private final long baselineRevision;
  private final TerritoryInviteClickDebounce debounce = new TerritoryInviteClickDebounce(15);
  private EditBox search;
  private int scrollRow;
  private long clientTick;
  private List<TerritoryInviteRowLayout.ButtonArea> inviteButtons = List.of();

  public Screen_InvitePlayer(UUID territoryId, String territoryName, UUID ownerId,
      Collection<UUID> existingMemberIds, Screen returnScreen) {
    super(Component.translatable("screen.invite.title"));
    this.territoryId = Objects.requireNonNull(territoryId, "territoryId");
    this.territoryName = Objects.requireNonNull(territoryName, "territoryName").trim();
    if (this.territoryName.isEmpty()
        || this.territoryName.length() > EconomyNetworkLimits.MAX_TERRITORY_NAME_LENGTH) {
      throw new IllegalArgumentException("territoryName");
    }
    this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
    this.existingMemberIds = Set.copyOf(Objects.requireNonNull(existingMemberIds,
        "existingMemberIds"));
    if (this.existingMemberIds.contains(ownerId)) throw new IllegalArgumentException("owner member");
    this.returnScreen = Objects.requireNonNull(returnScreen, "returnScreen");
    this.baselineRevision = ClientPlayerListState.snapshot().revision();
    EconomyServices.platform().network().sendToServer(ServerPlayerListRequestMessage.INSTANCE);
  }

  @Override
  protected void init() {
    String value = search == null ? "" : search.getValue();
    search = new EditBox(font, width / 2 - 100, 24, 200, 20,
        Component.translatable("screen.invite.search"));
    search.setMaxLength(64);
    search.setValue(value);
    search.setResponder(ignored -> {
      scrollRow = 0;
      inviteButtons = List.of();
    });
    addRenderableWidget(search);
  }

  @Override
  public void tick() {
    super.tick();
    clientTick++;
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    inviteButtons = List.of();
    renderBackground(graphics);
    graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
    graphics.drawString(font, Component.translatable("screen.invite.territory", territoryName),
        24, 48, 0xBBBBBB);
    int backX = width - BACK_BUTTON_WIDTH - 20;
    int backY = 24;
    graphics.fill(backX, backY, backX + BACK_BUTTON_WIDTH, backY + BACK_BUTTON_HEIGHT, 0xFF4D4D4D);
    graphics.drawCenteredString(font, Component.translatable("button.invite.back"),
        backX + BACK_BUTTON_WIDTH / 2, backY + 6, 0xFFFFFFFF);

    ClientPlayerListState.Snapshot snapshot = ClientPlayerListState.snapshot();
    if (snapshot.revision() <= baselineRevision) {
      graphics.drawCenteredString(font, Component.translatable("screen.invite.loading"),
          width / 2, 76, 0xAAAAAA);
      super.render(graphics, mouseX, mouseY, partialTick);
      return;
    }
    List<PlayerSummary> players = visiblePlayers(snapshot);
    if (players.isEmpty()) {
      graphics.drawCenteredString(font, Component.translatable("screen.invite.empty"),
          width / 2, 76, 0xAAAAAA);
      super.render(graphics, mouseX, mouseY, partialTick);
      return;
    }
    scrollRow = TerritoryInviteRowLayout.clampScroll(scrollRow, players.size(), height);
    inviteButtons = TerritoryInviteRowLayout.layout(
        players.stream().map(PlayerSummary::playerId).toList(), scrollRow, width, height,
        BUTTON_WIDTH, BUTTON_HEIGHT);
    for (int index = 0; index < inviteButtons.size(); index++) {
      PlayerSummary player = players.get(scrollRow + index);
      TerritoryInviteRowLayout.ButtonArea area = inviteButtons.get(index);
      graphics.drawString(font, player.playerName(), 24, area.y() + 5, 0xFFFFFF);
      boolean available = debounce.available(clientTick);
      graphics.fill(area.x(), area.y(), area.x() + area.width(), area.y() + area.height(),
          available ? 0xFF3D6F4A : 0xFF555555);
      graphics.fill(area.x(), area.y(), area.x() + area.width(), area.y() + 1, 0xFF9BC8A4);
      graphics.fill(area.x(), area.y() + area.height() - 1,
          area.x() + area.width(), area.y() + area.height(), 0xFF1B3322);
      graphics.drawCenteredString(font, Component.translatable("button.invite.invite"),
          area.x() + area.width() / 2, area.y() + 6, 0xFFFFFFFF);
    }
    if (players.size() > inviteButtons.size()) {
      graphics.drawString(font,
          Component.literal((scrollRow + 1) + "-" + (scrollRow + inviteButtons.size())
              + "/" + players.size()), 24, height - 14, 0xAAAAAA);
    }
    super.render(graphics, mouseX, mouseY, partialTick);
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (button == 0) {
      int backX = width - BACK_BUTTON_WIDTH - 20;
      if (mouseX >= backX && mouseX < backX + BACK_BUTTON_WIDTH
          && mouseY >= 24 && mouseY < 24 + BACK_BUTTON_HEIGHT) {
        onClose();
        return true;
      }
      for (TerritoryInviteRowLayout.ButtonArea area : inviteButtons) {
        if (area.contains(mouseX, mouseY)) {
          if (debounce.tryAcquire(clientTick)) {
            EconomyServices.platform().network().sendToServer(
                new InvitePlayerMessage(territoryId, area.playerId()));
          }
          return true;
        }
      }
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    if (delta != 0) {
      scrollRow = TerritoryInviteRowLayout.clampScroll(
          scrollRow + (delta < 0 ? 1 : -1), visiblePlayers(ClientPlayerListState.snapshot()).size(), height);
      inviteButtons = List.of();
      return true;
    }
    return super.mouseScrolled(mouseX, mouseY, delta);
  }

  private List<PlayerSummary> visiblePlayers(ClientPlayerListState.Snapshot snapshot) {
    String query = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT);
    UUID self = Minecraft.getInstance().player == null
        ? null : Minecraft.getInstance().player.getUUID();
    List<PlayerSummary> result = new ArrayList<>();
    for (PlayerSummary player : snapshot.players()) {
      if (self != null && self.equals(player.playerId())) continue;
      if (ownerId.equals(player.playerId())) continue;
      if (existingMemberIds.contains(player.playerId())) continue;
      if (query.isEmpty() || player.playerName().toLowerCase(Locale.ROOT).contains(query)) {
        result.add(player);
      }
    }
    return result;
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == 256 && shouldCloseOnEsc()) {
      onClose();
      return true;
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public void onClose() {
    Minecraft.getInstance().setScreen(returnScreen);
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }
}

package com.mo.economy_system.screen.territory_system;

import com.mo.economy_system.common.client.ClientPlayerListState;
import com.mo.economy_system.common.network.InvitePlayerMessage;
import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.common.network.ServerPlayerListRequestMessage;
import com.mo.economy_system.platform.EconomyServices;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Minimal Forge invite screen backed by the loader-neutral player directory. */
public final class Screen_InvitePlayer extends Screen {
  private static final int BUTTON_WIDTH = 72;
  private static final int BUTTON_HEIGHT = 20;

  private final UUID territoryId;
  private final String territoryName;
  private EditBox search;
  private int scrollRow;
  private List<TerritoryInviteRowLayout.ButtonArea> inviteButtons = List.of();

  public Screen_InvitePlayer(UUID territoryId, String territoryName) {
    super(Component.translatable("screen.invite.title"));
    this.territoryId = territoryId;
    this.territoryName = territoryName;
    EconomyServices.platform().network().sendToServer(ServerPlayerListRequestMessage.INSTANCE);
  }

  @Override
  protected void init() {
    String value = search == null ? "" : search.getValue();
    search = new EditBox(font, width / 2 - 100, 24, 200, 20,
        Component.translatable("screen.invite.search"));
    search.setMaxLength(64);
    search.setValue(value);
    addRenderableWidget(search);
  }

  @Override
  public void tick() {
    super.tick();
    // The directory response is atomically replaced by ClientPlayerListState.
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    inviteButtons = List.of();
    renderBackground(graphics);
    graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
    graphics.drawString(font, Component.translatable("screen.invite.territory", territoryName),
        24, 48, 0xBBBBBB);

    List<PlayerSummary> players = visiblePlayers();
    scrollRow = TerritoryInviteRowLayout.clampScroll(scrollRow, players.size(), height);
    inviteButtons = TerritoryInviteRowLayout.layout(
        players.stream().map(PlayerSummary::playerId).toList(), scrollRow, width, height,
        BUTTON_WIDTH, BUTTON_HEIGHT);
    for (int index = 0; index < inviteButtons.size(); index++) {
      PlayerSummary player = players.get(scrollRow + index);
      TerritoryInviteRowLayout.ButtonArea area = inviteButtons.get(index);
      graphics.drawString(font, player.playerName(), 24, area.y() + 5, 0xFFFFFF);
      graphics.fill(area.x(), area.y(), area.x() + area.width(), area.y() + area.height(),
          0xFF3D6F4A);
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
      for (TerritoryInviteRowLayout.ButtonArea area : inviteButtons) {
        if (area.contains(mouseX, mouseY)) {
          EconomyServices.platform().network().sendToServer(
              new InvitePlayerMessage(territoryId, area.playerId()));
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
          scrollRow + (delta < 0 ? 1 : -1), visiblePlayers().size(), height);
      return true;
    }
    return super.mouseScrolled(mouseX, mouseY, delta);
  }

  private List<PlayerSummary> visiblePlayers() {
    String query = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT);
    UUID self = Minecraft.getInstance().player == null
        ? null : Minecraft.getInstance().player.getUUID();
    List<PlayerSummary> result = new ArrayList<>();
    for (PlayerSummary player : ClientPlayerListState.snapshot().players()) {
      if (self != null && self.equals(player.playerId())) continue;
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
  public boolean isPauseScreen() {
    return false;
  }
}

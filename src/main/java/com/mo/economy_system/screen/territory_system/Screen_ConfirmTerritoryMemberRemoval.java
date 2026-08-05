package com.mo.economy_system.screen.territory_system;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.network.RemoveTerritoryMemberMessage;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class Screen_ConfirmTerritoryMemberRemoval extends Screen {
  private final UUID territoryId, targetId;
  private final String territoryName, targetName;
  private final Screen returnScreen;
  private final TerritoryRemovalSubmitGate gate = new TerritoryRemovalSubmitGate();

  public Screen_ConfirmTerritoryMemberRemoval(
      UUID territoryId,
      String territoryName,
      UUID targetId,
      String targetName,
      Screen returnScreen) {
    super(Component.translatable("screen.territory_member_remove.title"));
    this.territoryId = Objects.requireNonNull(territoryId);
    this.territoryName = Objects.requireNonNull(territoryName).trim();
    this.targetId = Objects.requireNonNull(targetId);
    this.targetName = Objects.requireNonNull(targetName).trim();
    this.returnScreen = Objects.requireNonNull(returnScreen);
    if (this.territoryName.isEmpty()
        || this.territoryName.length() > EconomyNetworkLimits.MAX_TERRITORY_NAME_LENGTH
        || this.targetName.isEmpty()
        || this.targetName.length() > EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH)
      throw new IllegalArgumentException("name");
  }

  protected void init() {
    addRenderableWidget(
        Button.builder(
                Component.translatable("button.territory.member_remove_confirm"),
                b -> {
                  if (!gate.trySubmit()) return;
                  b.active = false;
                  EconomySystem_NetworkManager.sendToServer(
                      new RemoveTerritoryMemberMessage(territoryId, targetId));
                  Minecraft.getInstance().setScreen(new Screen_Territory());
                })
            .bounds(width / 2 - 105, height / 2 + 35, 100, 20)
            .build());
    addRenderableWidget(
        Button.builder(
                Component.translatable("button.territory.member_remove_cancel"), b -> onClose())
            .bounds(width / 2 + 5, height / 2 + 35, 100, 20)
            .build());
  }

  public void render(GuiGraphics g, int x, int y, float tick) {
    renderBackground(g, x, y, tick);
    g.drawCenteredString(font, title, width / 2, height / 2 - 55, 0xffffff);
    g.drawCenteredString(
        font,
        Component.translatable("screen.territory_member_remove.warning"),
        width / 2,
        height / 2 - 35,
        0xff5555);
    g.drawCenteredString(
        font,
        Component.translatable("screen.territory_member_remove.territory", territoryName),
        width / 2,
        height / 2 - 15,
        0xffffff);
    g.drawCenteredString(
        font,
        Component.translatable("screen.territory_member_remove.target", targetName),
        width / 2,
        height / 2 + 2,
        0xffffff);
    super.render(g, x, y, tick);
  }

  public void onClose() {
    Minecraft.getInstance().setScreen(returnScreen);
  }
}

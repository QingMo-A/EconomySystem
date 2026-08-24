package com.mo.economy_system.screen.territory_system;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.network.RemoveTerritoryMessage;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class Screen_ConfirmTerritoryRemoval extends Screen {
  private final UUID territoryId;
  private final String territoryName;
  private final Screen returnScreen;
  private final TerritoryRemovalSubmitGate gate = new TerritoryRemovalSubmitGate();

  public Screen_ConfirmTerritoryRemoval(UUID id, String name, Screen back) {
    super(Component.translatable("screen.territory_remove.title"));
    territoryId = Objects.requireNonNull(id);
    territoryName = Objects.requireNonNull(name).trim();
    returnScreen = Objects.requireNonNull(back);
    if (territoryName.isEmpty()
        || territoryName.length() > EconomyNetworkLimits.MAX_TERRITORY_NAME_LENGTH)
      throw new IllegalArgumentException("territoryName");
  }

  @Override
  protected void init() {
    addRenderableWidget(
        Button.builder(
                Component.translatable("button.territory.delete_confirm"),
                b -> {
                  if (!gate.trySubmit()) return;
                  b.active = false;
                  EconomySystem_NetworkManager.sendToServer(
                      new RemoveTerritoryMessage(territoryId));
                  Minecraft.getInstance().setScreen(new Screen_Territory());
                })
            .bounds(width / 2 - 105, height / 2 + 35, 100, 20)
            .build());
    addRenderableWidget(
        Button.builder(Component.translatable("button.territory.delete_cancel"), b -> onClose())
            .bounds(width / 2 + 5, height / 2 + 35, 100, 20)
            .build());
  }

  @Override
  public void render(GuiGraphics g, int x, int y, float tick) {
    renderBackground(g, x, y, tick);
    g.drawCenteredString(font, title, width / 2, height / 2 - 58, 0xffffff);
    g.drawCenteredString(
        font,
        Component.translatable("screen.territory_remove.warning"),
        width / 2,
        height / 2 - 43,
        0xff5555);
    g.drawCenteredString(
        font, Component.literal(territoryName), width / 2, height / 2 - 28, 0xffaaaa);
    g.drawCenteredString(
        font,
        Component.translatable("screen.territory_remove.irreversible"),
        width / 2,
        height / 2 - 10,
        0xff5555);
    g.drawCenteredString(
        font,
        Component.translatable("screen.territory_remove.no_refund"),
        width / 2,
        height / 2 + 5,
        0xff5555);
    super.render(g, x, y, tick);
  }

  @Override
  public void onClose() {
    Minecraft.getInstance().setScreen(returnScreen);
  }
}

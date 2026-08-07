package com.mo.economy_system.screen;

import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Explicit target capability notice; unsupported pages must never fail silently. */
public final class Screen_Unsupported extends Screen {
  private final EconomyUiRoute route;

  public Screen_Unsupported(EconomyUiRoute route) {
    super(Component.translatable(route.titleKey()));
    this.route = route;
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    renderBackground(graphics);
    graphics.drawCenteredString(font, Component.translatable("screen.economy_system.not_available"),
        width / 2, height / 2 - 12, 0xFFFFFFFF);
    graphics.drawCenteredString(font, Component.translatable("screen.economy_system.route", route.name()),
        width / 2, height / 2 + 4, 0xFFAAAAAA);
    super.render(graphics, mouseX, mouseY, partialTick);
  }
}

package com.mo.economy_system.screen;

import com.mo.economy_system.common.client.ui.EconomyUiMenu;
import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.target.forge1201.client.Forge1201UiBridge;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Forge 1.20.1 renderer for the loader-neutral EconomySystem home menu. */
public final class Screen_Home extends Screen {
  private static final int BUTTON_WIDTH = 220;
  private static final int BUTTON_HEIGHT = 20;
  private static final int BUTTON_GAP = 24;
  private final List<EconomyUiMenu.Entry> entries = EconomyUiMenu.defaultEntries();

  public Screen_Home() {
    super(Component.translatable(EconomyUiRoute.HOME.titleKey()));
  }

  @Override
  protected void init() {
    int startY = Math.max(42, height / 2 - (entries.size() * BUTTON_GAP) / 2);
    for (int index = 0; index < entries.size(); index++) {
      EconomyUiMenu.Entry entry = entries.get(index);
      int y = startY + index * BUTTON_GAP;
      boolean supported = Forge1201UiBridge.INSTANCE.supports(entry.route());
      Component label = Component.translatable(entry.labelKey());
      if (!supported) {
        label = label.copy().append(Component.translatable("screen.economy_system.not_available_short"));
      }
      Button button = Button.builder(label,
              ignored -> open(entry.route()))
          .bounds(width / 2 - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT)
          .build();
      addRenderableWidget(button);
    }
    addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), ignored -> onClose())
        .bounds(width / 2 - BUTTON_WIDTH / 2, startY + entries.size() * BUTTON_GAP + 8,
            BUTTON_WIDTH, BUTTON_HEIGHT)
        .build());
  }

  private void open(EconomyUiRoute route) {
    if (minecraft == null) return;
    minecraft.setScreen(Forge1201UiBridge.INSTANCE.create(route)
        .orElseGet(() -> new Screen_Unsupported(route)));
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    renderBackground(graphics);
    graphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFFFF);
    super.render(graphics, mouseX, mouseY, partialTick);
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == 256) {
      onClose();
      return true;
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }
}

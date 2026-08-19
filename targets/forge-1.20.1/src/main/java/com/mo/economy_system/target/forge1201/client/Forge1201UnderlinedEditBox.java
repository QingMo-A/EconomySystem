package com.mo.economy_system.target.forge1201.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/** Transparent EditBox whose text rests just above the common underline. */
public final class Forge1201UnderlinedEditBox extends EditBox {
  private static final int BOTTOM_TEXT_PADDING = 3;
  private final Font font;

  public Forge1201UnderlinedEditBox(Font font, int x, int y, int width, int height,
                                    Component message) {
    super(font, x, y, width, height, message);
    this.font = font;
  }

  @Override
  public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    int originalY = getY();
    int textY = originalY + Math.max(0, height - font.lineHeight - BOTTOM_TEXT_PADDING);
    setY(textY);
    try {
      super.renderWidget(graphics, mouseX, mouseY, partialTick);
    } finally {
      setY(originalY);
    }
  }
}

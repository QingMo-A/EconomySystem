package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.theme.UiButtonStyle;
import com.mo.economy_system.ui.theme.UiCardStyle;
import com.mo.economy_system.utils.Util_Skull;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** NeoForge 1.21.1 translation of common semantic UI drawing operations. */
public final class NeoForge1211UiRenderer implements EconomyUiRenderer {
  private final GuiGraphics graphics;
  private final Font font;

  public NeoForge1211UiRenderer(GuiGraphics graphics, Font font) {
    this.graphics = graphics;
    this.font = font;
  }

  @Override public void fill(UiRect rect, int argb) {
    graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), argb);
  }

  @Override public void text(String text, int x, int y, int argb) {
    graphics.drawString(font, Component.literal(text), x, y, argb);
  }

  @Override public void translatedText(String key, List<String> arguments, int x, int y, int argb) {
    graphics.drawString(font, Component.translatable(key, arguments.toArray()), x, y, argb);
  }

  @Override public void card(UiRect rect, UiCardStyle style, boolean hovered) {
    fill(rect, hovered ? style.backgroundHover() : style.background());
    int border = hovered ? style.borderHover() : style.border();
    graphics.fill(rect.x(), rect.y(), rect.right(), rect.y() + 1, border);
    graphics.fill(rect.x(), rect.bottom() - 1, rect.right(), rect.bottom(), border);
    graphics.fill(rect.x(), rect.y(), rect.x() + 1, rect.bottom(), border);
    graphics.fill(rect.right() - 1, rect.y(), rect.right(), rect.bottom(), border);
  }

  @Override public void button(UiRect rect, UiButtonStyle style, String text,
                               boolean hovered, boolean enabled) {
    drawButton(rect, style, Component.literal(text), hovered, enabled);
  }

  @Override public void translatedButton(UiRect rect, UiButtonStyle style, String key,
                                         List<String> arguments, boolean hovered, boolean enabled) {
    drawButton(rect, style, Component.translatable(key, arguments.toArray()), hovered, enabled);
  }

  private void drawButton(UiRect rect, UiButtonStyle style, Component text,
                          boolean hovered, boolean enabled) {
    int background = enabled && hovered ? style.backgroundHover() : style.background();
    fill(rect, background);
    int border = hovered ? style.borderHover() : style.border();
    graphics.fill(rect.x(), rect.y(), rect.right(), rect.y() + 1, border);
    graphics.fill(rect.x(), rect.bottom() - 1, rect.right(), rect.bottom(), border);
    if (style.stripeWidth() > 0) {
      graphics.fill(rect.x(), rect.y(), rect.x() + style.stripeWidth(), rect.bottom(), style.accent());
    }
    graphics.drawCenteredString(font, text, rect.x() + rect.width() / 2,
        rect.y() + (rect.height() - font.lineHeight) / 2, enabled ? style.text() : 0x60808080);
  }

  @Override public void icon(UiIcon icon, UiRect rect) {
    graphics.drawString(font, Component.literal(icon.name().substring(0, 1)),
        rect.x(), rect.y(), 0xB0FFFFFF);
  }

  @Override public void playerHead(UUID playerId, String playerName, UiRect rect) {
    float scale = Math.min(rect.width(), rect.height()) / 16.0f;
    graphics.pose().pushPose();
    graphics.pose().scale(scale, scale, 1.0f);
    graphics.renderItem(Util_Skull.createPlayerHead(playerId, playerName),
        Math.round(rect.x() / scale), Math.round(rect.y() / scale));
    graphics.pose().popPose();
  }
}

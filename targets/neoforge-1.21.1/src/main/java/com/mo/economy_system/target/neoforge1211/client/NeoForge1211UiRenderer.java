package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.TooltipLine;
import com.mo.economy_system.ui.renderer.TooltipModel;
import com.mo.economy_system.ui.renderer.UiChromePlan;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.text.UiTextMetrics;
import com.mo.economy_system.ui.text.UiTextSpan;
import com.mo.economy_system.ui.theme.UiButtonStyle;
import com.mo.economy_system.ui.theme.UiCardStyle;
import com.mo.economy_system.utils.Util_Skull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** NeoForge 1.21.1 translation of common semantic UI drawing operations. */
public final class NeoForge1211UiRenderer implements EconomyUiRenderer {
  private final GuiGraphics graphics;
  private final Font font;

  public NeoForge1211UiRenderer(GuiGraphics graphics, Font font) {
    this.graphics = graphics;
    this.font = font;
  }

  /** Draws the Home backdrop in physical screen coordinates before the virtual UI pose. */
  public void fillPhysicalBackground(int width, int height, int argb) {
    graphics.fill(0, 0, width, height, argb);
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

  @Override public void textInRect(String text, UiRect rect, int argb, UiTextAlignment alignment) {
    drawTextInRect(Component.literal(text), rect, argb, alignment);
  }

  @Override public void translatedTextInRect(String key, List<String> arguments, UiRect rect,
                                             int argb, UiTextAlignment alignment) {
    drawTextInRect(Component.translatable(key, arguments.toArray()), rect, argb, alignment);
  }

  private void drawTextInRect(Component text, UiRect rect, int argb, UiTextAlignment alignment) {
    String clipped = font.plainSubstrByWidth(text.getString(), rect.width());
    int textWidth = font.width(clipped);
    int x = switch (alignment) {
      case LEFT -> rect.x();
      case CENTER -> rect.x() + Math.max(0, (rect.width() - textWidth) / 2);
      case RIGHT -> rect.right() - textWidth;
    };
    int y = rect.y() + Math.max(0, (rect.height() - font.lineHeight) / 2);
    graphics.drawString(font, Component.literal(clipped), x, y, argb);
  }

  @Override public void card(UiRect rect, UiCardStyle style, boolean hovered) {
    UiChromePlan.cardChrome(rect, style, hovered)
        .forEach(command -> fill(command.rect(), command.argb()));
  }

  @Override public void button(UiRect rect, UiButtonStyle style, String text,
                               boolean hovered, boolean enabled) {
    drawButton(rect, style, Component.literal(text), hovered, enabled);
  }

  @Override public void translatedButton(UiRect rect, UiButtonStyle style, String key,
                                         List<String> arguments, boolean hovered, boolean enabled) {
    drawButton(rect, style, Component.translatable(key, arguments.toArray()), hovered, enabled);
  }

  @Override public void translatedIconButton(UiRect rect, UiButtonStyle style,
                                             UiIcon icon, String key, List<String> arguments,
                                             boolean hovered, boolean enabled) {
    drawButton(rect, style, Component.translatable(key, arguments.toArray()), icon, hovered, enabled);
  }

  private void drawButton(UiRect rect, UiButtonStyle style, Component text,
                          boolean hovered, boolean enabled) {
    drawButton(rect, style, text, null, hovered, enabled);
  }

  private void drawButton(UiRect rect, UiButtonStyle style, Component text, UiIcon icon,
                          boolean hovered, boolean enabled) {
    UiChromePlan.buttonChrome(rect, style, hovered, enabled)
        .forEach(command -> fill(command.rect(), command.argb()));
    int textColor = enabled ? style.textColor() : 0x60808080;
    int textY = rect.y() + (rect.height() - font.lineHeight) / 2;
    int textX;
    if (icon != null) {
      textX = rect.x() + style.padding();
      icon(icon, new UiRect(textX, textY - 1,
          EconomyUiRenderer.ICON_SIZE, EconomyUiRenderer.ICON_SIZE));
      textX += EconomyUiRenderer.ICON_ADVANCE;
    } else if (style.alignment() == UiTextAlignment.CENTER) {
      textX = rect.x() + (rect.width() - font.width(text)) / 2;
    } else {
      textX = style.alignment() == UiTextAlignment.RIGHT
          ? rect.right() - font.width(text) - style.padding() : rect.x() + style.padding();
    }
    if (style.alignment() == UiTextAlignment.CENTER && icon == null) {
      graphics.drawString(font, text, rect.x() + (rect.width() - font.width(text)) / 2,
          textY, textColor, style.textShadow());
    } else {
      graphics.drawString(font, text, textX, textY, textColor, style.textShadow());
    }
  }

  @Override public void icon(UiIcon icon, UiRect rect) {
    ResourceLocation texture = ResourceLocation.tryParse(icon.resourcePath());
    graphics.blit(texture, rect.x(), rect.y(), rect.width(), rect.height(),
        0, 0, icon.sourceWidth(), icon.sourceHeight(),
        icon.sourceWidth(), icon.sourceHeight());
  }

  @Override public void scaledIconText(UiIcon icon, String text, int originX, int originY,
                                       float scale, int iconSize, int iconAdvance,
                                       int textColor) {
    graphics.pose().pushPose();
    graphics.pose().translate(originX, originY, 0);
    graphics.pose().scale(scale, scale, 1.0f);
    icon(icon, new UiRect(0, -1, iconSize, iconSize));
    graphics.drawString(font, Component.literal(text), iconAdvance, 0, textColor);
    graphics.pose().popPose();
  }

  @Override public void playerHead(UUID playerId, String playerName, UiRect rect) {
    float scale = Math.min(rect.width(), rect.height()) / 16.0f;
    graphics.pose().pushPose();
    graphics.pose().scale(scale, scale, 1.0f);
    graphics.renderItem(Util_Skull.createPlayerHead(playerId, playerName),
        Math.round(rect.x() / scale), Math.round(rect.y() / scale));
    graphics.pose().popPose();
  }

  @Override public void scaledIconTranslatedText(UiIcon icon, String key, List<String> arguments,
                                                  int originX, int originY, float scale,
                                                  int iconSize, int iconAdvance, int textColor) {
    graphics.pose().pushPose();
    graphics.pose().translate(originX, originY, 0);
    graphics.pose().scale(scale, scale, 1.0f);
    icon(icon, new UiRect(0, -1, iconSize, iconSize));
    graphics.drawString(font, Component.translatable(key, (arguments == null ? List.<String>of() : arguments).toArray()),
        iconAdvance, 0, textColor);
    graphics.pose().popPose();
  }

  @Override public void itemDisplayName(String itemId, UiRect rect, int color, UiTextAlignment alignment) {
    ResourceLocation location = ResourceLocation.tryParse(itemId);
    var item = location == null ? Items.AIR : BuiltInRegistries.ITEM.get(location);
    Component name = item == null || item == Items.AIR ? Component.literal(itemId)
        : item.getDescription();
    String clipped = font.plainSubstrByWidth(name.getString(), rect.width());
    int textWidth = font.width(clipped);
    int x = switch (alignment) {
      case LEFT -> rect.x();
      case CENTER -> rect.x() + Math.max(0, (rect.width() - textWidth) / 2);
      case RIGHT -> rect.right() - textWidth;
    };
    int y = rect.y() + Math.max(0, (rect.height() - font.lineHeight) / 2);
    graphics.drawString(font, Component.literal(clipped), x, y, color);
  }

  @Override public void scaledIconStyledText(UiIcon icon, List<UiTextSpan> spans, int originX,
                                             int originY, float scale, int iconSize,
                                             int iconAdvance) {
    graphics.pose().pushPose();
    graphics.pose().translate(originX, originY, 0);
    graphics.pose().scale(scale, scale, 1.0f);
    icon(icon, new UiRect(0, -1, iconSize, iconSize));
    int x = iconAdvance;
    for (UiTextSpan span : spans) {
      graphics.drawString(font, Component.literal(span.text()), x, 0, span.color());
      x += font.width(span.text());
    }
    graphics.pose().popPose();
  }

  @Override public UiTextMetrics metrics() {
    return new UiTextMetrics() {
      @Override public int width(String text) { return font.width(text == null ? "" : text); }
      @Override public int lineHeight() { return font.lineHeight; }
      @Override public int translatedWidth(String key, List<String> arguments) {
        return font.width(Component.translatable(key, (arguments == null ? List.<String>of() : arguments).toArray()));
      }
    };
  }

  @Override public void item(String itemId, UiRect rect) {
    ResourceLocation location = ResourceLocation.tryParse(itemId);
    var item = location == null ? Items.AIR : BuiltInRegistries.ITEM.get(location);
    renderItem(item == null ? Items.AIR.getDefaultInstance() : item.getDefaultInstance(), rect);
  }

  @Override public void texture(String textureId, UiRect rect) {
    ResourceLocation texture = ResourceLocation.tryParse(textureId);
    if (texture == null) return;
    graphics.blit(texture, rect.x(), rect.y(), rect.width(), rect.height(),
        0, 0, 256, 256, 256, 256);
  }

  @Override public void tooltip(TooltipModel tooltip, int mouseX, int mouseY) {
    List<Component> lines = new ArrayList<>();
    for (TooltipLine line : tooltip.lines()) {
      if (line instanceof TooltipLine.Literal literal) {
        lines.add(Component.literal(literal.text()));
      } else if (line instanceof TooltipLine.Translated translated) {
        lines.add(Component.translatable(translated.key(), translated.arguments().toArray()));
      } else if (line instanceof TooltipLine.Item item) {
        List<Object> arguments = new ArrayList<>();
        arguments.add(resolveItemName(item.itemId()));
        arguments.addAll(item.arguments());
        lines.add(Component.translatable(item.key(), arguments.toArray()));
      }
    }
    graphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
  }

  private void renderItem(ItemStack stack, UiRect rect) {
    float scale = Math.min(rect.width(), rect.height()) / 16.0f;
    graphics.pose().pushPose();
    graphics.pose().scale(scale, scale, 1.0f);
    graphics.renderItem(stack, Math.round(rect.x() / scale), Math.round(rect.y() / scale));
    graphics.pose().popPose();
  }

  private String resolveItemName(String itemId) {
    ResourceLocation location = ResourceLocation.tryParse(itemId);
    if (location == null) return itemId;
    var item = BuiltInRegistries.ITEM.get(location);
    return item == null || item == Items.AIR ? itemId : new ItemStack(item).getHoverName().getString();
  }

}

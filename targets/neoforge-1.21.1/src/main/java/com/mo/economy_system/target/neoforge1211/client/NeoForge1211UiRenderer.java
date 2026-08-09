package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.TooltipLine;
import com.mo.economy_system.ui.renderer.TooltipModel;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
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
    fill(rect, hovered ? style.backgroundHover() : style.background());
    int border = hovered ? style.borderHover() : style.border();
    graphics.fill(rect.x(), rect.y(), rect.right(), rect.y() + 1, border);
    graphics.fill(rect.x(), rect.bottom() - 1, rect.right(), rect.bottom(), border);
    graphics.fill(rect.right() - 1, rect.y(), rect.right(), rect.bottom(), border);
    if (style.accentWidth() > 0) {
      graphics.fill(rect.x(), rect.y(),
          Math.min(rect.right(), rect.x() + style.accentWidth()), rect.bottom(),
          withAlpha(style.accent(), hovered ? style.accentAlphaHover() : style.accentAlpha()));
    }
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
    int border = style.borderColor(hovered, enabled);
    if (style.stripeWidth() > 0) {
      int stripeAlpha = enabled && hovered ? style.stripeAlphaHover() : style.stripeAlpha();
      if (!enabled) stripeAlpha = Math.min(stripeAlpha, 0x60);
      graphics.fill(rect.x(), rect.y(), Math.min(rect.right(), rect.x() + style.stripeWidth()),
          rect.bottom(), withAlpha(style.accent(), stripeAlpha));
      graphics.fill(rect.right() - 1, rect.y(), rect.right(), rect.bottom(), border);
      graphics.fill(Math.min(rect.right(), rect.x() + style.stripeWidth()), rect.bottom() - 1,
          rect.right(), rect.bottom(), border);
    } else {
      graphics.fill(rect.x(), rect.y(), rect.x() + 1, rect.bottom(), border);
      graphics.fill(rect.right() - 1, rect.y(), rect.right(), rect.bottom(), border);
      if (enabled) graphics.fill(rect.x() + 2, rect.y() + 1,
          Math.max(rect.x() + 2, rect.right() - 2), rect.y() + 2, 0x60FFFFFF);
    }
    if (enabled && hovered && style.glowHeight() > 0) {
      for (int i = 0; i < style.glowHeight(); i++) {
        int alpha = style.glowAlphaStart() - i * style.glowAlphaStep();
        if (alpha <= 0) break;
        graphics.fill(Math.min(rect.right(), rect.x() + style.stripeWidth()), rect.y() + i,
            rect.right(), rect.y() + i + 1,
            withAlpha(style.accent(), alpha));
      }
    }
    int textColor = enabled ? style.textColor() : 0x60808080;
    int textY = rect.y() + (rect.height() - font.lineHeight) / 2;
    if (style.alignment() == UiTextAlignment.CENTER) {
      graphics.drawString(font, text, rect.x() + (rect.width() - font.width(text)) / 2,
          textY, textColor, style.textShadow());
    } else {
      int textX = style.alignment() == UiTextAlignment.RIGHT
          ? rect.right() - font.width(text) - style.padding() : rect.x() + style.padding();
      graphics.drawString(font, text, textX, textY, textColor, style.textShadow());
    }
  }

  @Override public void icon(UiIcon icon, UiRect rect) {
    ResourceLocation texture = iconTexture(icon);
    graphics.blit(texture, rect.x(), rect.y(), rect.width(), rect.height(),
        0, 0, 16, 16, 16, 16);
  }

  @Override public void playerHead(UUID playerId, String playerName, UiRect rect) {
    float scale = Math.min(rect.width(), rect.height()) / 16.0f;
    graphics.pose().pushPose();
    graphics.pose().scale(scale, scale, 1.0f);
    graphics.renderItem(Util_Skull.createPlayerHead(playerId, playerName),
        Math.round(rect.x() / scale), Math.round(rect.y() / scale));
    graphics.pose().popPose();
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

  private ResourceLocation iconTexture(UiIcon icon) {
    String name = switch (icon) {
      case TERRITORY -> "territory";
      case HOME -> "home";
      case SHOP -> "shop";
      case MARKET -> "market";
      case DELIVERY -> "delivery";
      case ABOUT -> "about";
      case TRADE -> "trade";
      case LEADERBOARD -> "leaderboard";
      case BALANCE -> "balance";
      case MEMBER, AUTHORIZED -> "authorized";
      case OWNER -> "owner";
      case MANAGE, BUFF, RETRY -> "manage";
      case ARROW_LEFT, BACK -> "arrow_left";
      case ARROW_RIGHT -> "arrow_right";
      case OVERWORLD -> "overworld";
      case NETHER -> "nether";
      case END -> "end";
      case KEY -> "key";
      case TELEPORT -> "teleport";
    };
    return ResourceLocation.tryParse("economy_system:textures/gui/icons/" + name + ".png");
  }

  private static int withAlpha(int rgb, int alpha) {
    return ((alpha & 0xFF) << 24) | (rgb & 0x00FFFFFF);
  }

}

package com.mo.economy_system.target.forge1201.client;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.TooltipLine;
import com.mo.economy_system.ui.renderer.TooltipModel;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.UiButtonStyle;
import com.mo.economy_system.ui.theme.UiCardStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Forge 1.20.1 translation of common semantic UI drawing operations. */
public final class Forge1201UiRenderer implements EconomyUiRenderer {
  private final GuiGraphics graphics;
  private final Font font;

  public Forge1201UiRenderer(GuiGraphics graphics, Font font) {
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
    if (style.stripeWidth() > 0) {
      graphics.fill(rect.x(), rect.y(),
          Math.min(rect.right(), rect.x() + style.stripeWidth()), rect.bottom(), style.accent());
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
    if (icon == UiIcon.BUFF) {
      renderItem(Items.GLASS_BOTTLE.getDefaultInstance(), rect);
      return;
    }
    graphics.drawString(font, Component.literal(icon.name().substring(0, 1)),
        rect.x(), rect.y(), 0xB0FFFFFF);
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

  @Override public void playerHead(UUID playerId, String playerName, UiRect rect) {
    ItemStack head = new ItemStack(Items.PLAYER_HEAD);
    CompoundTag owner = new CompoundTag();
    owner.putUUID("Id", playerId);
    owner.putString("Name", playerName);
    head.getOrCreateTag().put("SkullOwner", owner);
    float scale = Math.min(rect.width(), rect.height()) / 16.0f;
    graphics.pose().pushPose();
    graphics.pose().scale(scale, scale, 1.0f);
    graphics.renderItem(head, Math.round(rect.x() / scale), Math.round(rect.y() / scale));
    graphics.pose().popPose();
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

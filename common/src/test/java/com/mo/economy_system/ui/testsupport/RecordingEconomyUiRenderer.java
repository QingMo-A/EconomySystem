package com.mo.economy_system.ui.testsupport;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.TooltipModel;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.text.UiTextMetrics;
import com.mo.economy_system.ui.text.UiTextSpan;
import com.mo.economy_system.ui.theme.UiButtonStyle;
import com.mo.economy_system.ui.theme.UiCardStyle;
import com.mo.economy_system.ui.theme.UiInputFrameStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Recording backend used to prove that target renderers receive one common semantic view. */
public final class RecordingEconomyUiRenderer implements EconomyUiRenderer {
  private final List<Operation> operations = new ArrayList<>();
  private final List<Paint> paints = new ArrayList<>();

  public List<Operation> operations() {
    return List.copyOf(operations);
  }

  /** Explicit paint colors for exact legacy semantic assertions. */
  public List<Paint> paints() {
    return List.copyOf(paints);
  }

  @Override public void fill(UiRect rect, int argb) {
    paints.add(new Paint("fill", rect, argb));
    add("fill", rect, Integer.toString(argb), true);
  }

  @Override public void text(String text, int x, int y, int argb) {
    paints.add(new Paint("text", new UiRect(x, y, 0, 0), argb));
    add("text", new UiRect(x, y, 0, 0), text, true);
  }

  @Override public void translatedText(String key, List<String> arguments, int x, int y, int argb) {
    paints.add(new Paint("translatedText", new UiRect(x, y, 0, 0), argb));
    add("translatedText", new UiRect(x, y, 0, 0), key + arguments, true);
  }

  @Override public void textInRect(String text, UiRect rect, int argb, UiTextAlignment alignment) {
    paints.add(new Paint("textInRect", rect, argb));
    add("textInRect", rect, text + ":" + alignment, true);
  }

  @Override public void translatedTextInRect(String key, List<String> arguments, UiRect rect,
                                               int argb, UiTextAlignment alignment) {
    paints.add(new Paint("translatedTextInRect", rect, argb));
    add("translatedTextInRect", rect, key + arguments + ":" + alignment, true);
  }

  @Override public void card(UiRect rect, UiCardStyle style, boolean hovered) {
    add("card", rect, style + ":" + hovered, true);
  }

  @Override public void inputFrame(UiRect rect, UiInputFrameStyle style, boolean focused) {
    add("inputFrame", rect, style + ":" + focused, true);
  }

  @Override public void button(UiRect rect, UiButtonStyle style, String text,
                               boolean hovered, boolean enabled) {
    add("button", rect, text + ":" + style + ":" + hovered, enabled);
  }

  @Override public void translatedButton(UiRect rect, UiButtonStyle style, String key,
                                         List<String> arguments, boolean hovered, boolean enabled) {
    add("translatedButton", rect, key + arguments + ":" + style + ":" + hovered, enabled);
  }

  @Override public void translatedIconButton(UiRect rect, UiButtonStyle style, UiIcon icon,
                                             String key, List<String> arguments,
                                             boolean hovered, boolean enabled) {
    add("translatedIconButton", rect,
        icon + ":" + key + arguments + ":" + style + ":" + hovered, enabled);
    if (icon != null) {
      int textY = rect.y() + (rect.height() - 9) / 2;
      icon(icon, new UiRect(rect.x() + style.padding(), textY - 1, 10, 10));
    }
  }

  @Override public void icon(UiIcon icon, UiRect rect) {
    add("icon", rect, icon.name(), true);
  }

  @Override public void scaledIconText(UiIcon icon, String text, int originX, int originY,
                                       float scale, int iconSize, int iconAdvance,
                                       int textColor) {
    paints.add(new Paint("scaledIconText", new UiRect(originX, originY, iconSize, iconSize), textColor));
    add("scaledIconText", new UiRect(originX, originY, iconSize, iconSize),
        icon + ":" + text + ":" + scale + ":" + iconAdvance, true);
  }

  @Override public void scaledIconTranslatedText(UiIcon icon, String key, List<String> arguments,
                                                 int originX, int originY, float scale, int iconSize,
                                                 int iconAdvance, int textColor) {
    paints.add(new Paint("scaledIconTranslatedText", new UiRect(originX, originY, iconSize, iconSize), textColor));
    add("scaledIconTranslatedText", new UiRect(originX, originY, iconSize, iconSize),
        icon + ":" + key + arguments + ":" + scale + ":" + iconAdvance, true);
  }

  @Override public void itemDisplayName(String itemId, UiRect rect, int color, UiTextAlignment alignment) {
    paints.add(new Paint("itemDisplayName", rect, color));
    add("itemDisplayName", rect, itemId + ":" + alignment, true);
  }

  @Override public void itemDisplayNameWithSuffix(String itemId, String suffix, UiRect rect,
                                                  int color, UiTextAlignment alignment) {
    paints.add(new Paint("itemDisplayNameWithSuffix", rect, color));
    add("itemDisplayNameWithSuffix", rect, itemId + suffix + ":" + alignment, true);
  }

  @Override public void translatedTextWithSuffix(String key, List<String> arguments, String suffix,
                                                 UiRect rect, int color, UiTextAlignment alignment) {
    paints.add(new Paint("translatedTextWithSuffix", rect, color));
    add("translatedTextWithSuffix", rect, key + arguments + suffix + ":" + alignment, true);
  }

  @Override public void scaledIconStyledText(UiIcon icon, List<UiTextSpan> spans,
                                             int originX, int originY, float scale,
                                             int iconSize, int iconAdvance) {
    add("scaledIconStyledText", new UiRect(originX, originY, iconSize, iconSize),
        icon + ":" + spans + ":" + scale + ":" + iconAdvance, true);
  }

  @Override public UiTextMetrics metrics() {
    return UiTextMetrics.APPROXIMATE;
  }

  @Override public void item(String itemId, UiRect rect) {
    add("item", rect, itemId, true);
  }

  @Override public void itemWithCount(String itemId, int count, UiRect rect) {
    add("itemWithCount", rect, itemId + ":" + count, true);
  }

  @Override public void texture(String textureId, UiRect rect) {
    add("texture", rect, textureId, true);
  }

  @Override public void playerHead(UUID playerId, String playerName, UiRect rect) {
    add("playerHead", rect, playerId + ":" + playerName, true);
  }

  @Override public void tooltip(TooltipModel tooltip, int mouseX, int mouseY) {
    add("tooltip", new UiRect(mouseX, mouseY, 0, 0), tooltip.toString(), true);
  }

  private void add(String kind, UiRect rect, String value, boolean enabled) {
    operations.add(new Operation(kind, rect, value, enabled));
  }

  public record Operation(String kind, UiRect rect, String value, boolean enabled) {}
  public record Paint(String kind, UiRect rect, int argb) {}
}

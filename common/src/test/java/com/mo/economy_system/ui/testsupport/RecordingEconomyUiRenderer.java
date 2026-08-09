package com.mo.economy_system.ui.testsupport;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.TooltipModel;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.UiButtonStyle;
import com.mo.economy_system.ui.theme.UiCardStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Recording backend used to prove that target renderers receive one common semantic view. */
public final class RecordingEconomyUiRenderer implements EconomyUiRenderer {
  private final List<Operation> operations = new ArrayList<>();

  public List<Operation> operations() {
    return List.copyOf(operations);
  }

  @Override public void fill(UiRect rect, int argb) {
    add("fill", rect, Integer.toString(argb), true);
  }

  @Override public void text(String text, int x, int y, int argb) {
    add("text", new UiRect(x, y, 0, 0), text, true);
  }

  @Override public void translatedText(String key, List<String> arguments, int x, int y, int argb) {
    add("translatedText", new UiRect(x, y, 0, 0), key + arguments, true);
  }

  @Override public void textInRect(String text, UiRect rect, int argb, UiTextAlignment alignment) {
    add("textInRect", rect, text + ":" + alignment, true);
  }

  @Override public void translatedTextInRect(String key, List<String> arguments, UiRect rect,
                                               int argb, UiTextAlignment alignment) {
    add("translatedTextInRect", rect, key + arguments + ":" + alignment, true);
  }

  @Override public void card(UiRect rect, UiCardStyle style, boolean hovered) {
    add("card", rect, style + ":" + hovered, true);
  }

  @Override public void button(UiRect rect, UiButtonStyle style, String text,
                               boolean hovered, boolean enabled) {
    add("button", rect, text + ":" + style + ":" + hovered, enabled);
  }

  @Override public void translatedButton(UiRect rect, UiButtonStyle style, String key,
                                         List<String> arguments, boolean hovered, boolean enabled) {
    add("translatedButton", rect, key + arguments + ":" + style + ":" + hovered, enabled);
  }

  @Override public void icon(UiIcon icon, UiRect rect) {
    add("icon", rect, icon.name(), true);
  }

  @Override public void item(String itemId, UiRect rect) {
    add("item", rect, itemId, true);
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
}

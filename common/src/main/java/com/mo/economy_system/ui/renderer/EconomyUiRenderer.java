package com.mo.economy_system.ui.renderer;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.theme.UiButtonStyle;
import com.mo.economy_system.ui.theme.UiCardStyle;
import java.util.List;
import java.util.UUID;

/** Semantic renderer port implemented by each Minecraft target. */
public interface EconomyUiRenderer {
    void fill(UiRect rect, int argb);

    void text(String text, int x, int y, int argb);

    void translatedText(String key, List<String> arguments, int x, int y, int argb);

    void textInRect(String text, UiRect rect, int argb, UiTextAlignment alignment);

    void translatedTextInRect(String key, List<String> arguments, UiRect rect,
                              int argb, UiTextAlignment alignment);

    void card(UiRect rect, UiCardStyle style, boolean hovered);

    void button(UiRect rect, UiButtonStyle style, String text, boolean hovered, boolean enabled);

    void translatedButton(UiRect rect, UiButtonStyle style, String key,
                          List<String> arguments, boolean hovered, boolean enabled);

    void icon(UiIcon icon, UiRect rect);

    /** Draws one icon/text group under a shared local transform. */
    default void scaledIconText(UiIcon icon, String text, int originX, int originY,
                                float scale, int iconSize, int iconAdvance, int textColor) {
        icon(icon, new UiRect(originX, originY - 1, iconSize, iconSize));
        text(text, originX + iconAdvance, originY, textColor);
    }

    /** Draws a target-native item icon from a loader-neutral item identifier. */
    default void item(String itemId, UiRect rect) {
        icon(UiIcon.SHOP, rect);
    }

    /** Draws a target-owned bitmap/texture identified by a stable resource id. */
    default void texture(String textureId, UiRect rect) {
        // A target may omit optional decorative assets while preserving layout.
    }

    void playerHead(UUID playerId, String playerName, UiRect rect);

    void tooltip(TooltipModel tooltip, int mouseX, int mouseY);
}

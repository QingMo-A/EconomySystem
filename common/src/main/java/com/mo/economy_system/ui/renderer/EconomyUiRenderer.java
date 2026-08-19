package com.mo.economy_system.ui.renderer;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.text.UiTextMetrics;
import com.mo.economy_system.ui.text.UiTextSpan;
import com.mo.economy_system.ui.theme.UiInputFrameStyle;
import com.mo.economy_system.ui.theme.UiButtonStyle;
import com.mo.economy_system.ui.theme.UiCardStyle;
import java.util.List;
import java.util.UUID;

/** Semantic renderer port implemented by each Minecraft target. */
public interface EconomyUiRenderer {
    /** Reference icon/text geometry shared by every loader adapter. */
    int ICON_SIZE = 10;
    int ICON_ADVANCE = 14;

    void fill(UiRect rect, int argb);

    void text(String text, int x, int y, int argb);

    void translatedText(String key, List<String> arguments, int x, int y, int argb);

    void textInRect(String text, UiRect rect, int argb, UiTextAlignment alignment);

    void translatedTextInRect(String key, List<String> arguments, UiRect rect,
                              int argb, UiTextAlignment alignment);

    void card(UiRect rect, UiCardStyle style, boolean hovered);

    /** Draws only the four-edge chrome around a target-owned native text field. */
    default void inputFrame(UiRect rect, UiInputFrameStyle style, boolean focused) {
        UiInputFramePlan.frame(rect, style, focused).commands().forEach(command -> fill(command.rect(), command.argb()));
    }

    void button(UiRect rect, UiButtonStyle style, String text, boolean hovered, boolean enabled);

    void translatedButton(UiRect rect, UiButtonStyle style, String key,
                          List<String> arguments, boolean hovered, boolean enabled);

    /**
     * Draws a translated left-aligned button with a real semantic icon.
     *
     * <p>This operation is deliberately mandatory.  It is pixel-sensitive: a target must draw
     * the icon and text from the same reference geometry instead of inheriting a fallback that
     * can paint the icon over the translated label.</p>
     */
    void translatedIconButton(UiRect rect, UiButtonStyle style, UiIcon icon,
                              String key, List<String> arguments,
                              boolean hovered, boolean enabled);

    void icon(UiIcon icon, UiRect rect);

    /** Draws one icon/text group under a shared local transform. */
    void scaledIconText(UiIcon icon, String text, int originX, int originY,
                        float scale, int iconSize, int iconAdvance, int textColor);

    /** Draws an icon plus a localized title using one shared local transform. */
    default void scaledIconTranslatedText(UiIcon icon, String key, List<String> arguments,
                                           int originX, int originY, float scale, int iconSize,
                                           int iconAdvance, int textColor) {
        scaledIconText(icon, key, originX, originY, scale, iconSize, iconAdvance, textColor);
    }

    /** Draws the native localized item hover name in a semantic text rectangle. */
    default void itemDisplayName(String itemId, UiRect rect, int color, UiTextAlignment alignment) {
        textInRect(itemId, rect, color, alignment);
    }

    /** Draws a native item hover name and suffix as one clipped line (for example " x64"). */
    void itemDisplayNameWithSuffix(String itemId, String suffix, UiRect rect, int color,
                                   UiTextAlignment alignment);

    /** Draws a translated label and suffix as one clipped line (for example seller + name). */
    void translatedTextWithSuffix(String key, List<String> arguments, String suffix,
                                  UiRect rect, int color, UiTextAlignment alignment);

    /** Draws a styled icon/text group under one shared local transform. */
    void scaledIconStyledText(UiIcon icon, List<UiTextSpan> spans, int originX, int originY,
                              float scale, int iconSize, int iconAdvance);

    /** Font metrics adapter supplied by a target renderer. */
    UiTextMetrics metrics();

    /** Draws a target-native item icon from a loader-neutral item identifier. */
    default void item(String itemId, UiRect rect) {
        icon(UiIcon.SHOP, rect);
    }

    /**
     * Draws a target-native item icon and its stack-count decoration as one semantic operation.
     *
     * <p>The count is explicit even when it is {@code 1}.  Target implementations must render
     * the item and the decoration in the same scaled pose and use the native item-decoration
     * primitive, rather than relying on a follow-up ordinary text draw whose depth can be hidden
     * by the item model.</p>
     */
    void itemWithCount(String itemId, int count, UiRect rect);

    /** Draws the claimed-state mask/check above an already rendered item. Targets may raise Z explicitly. */
    default void claimedItemOverlay(UiRect rect) {
        fill(rect, 0x66000000);
        textInRect("✓", rect, 0xFF9BE7A7, UiTextAlignment.CENTER);
    }

    /** Draws a target-owned bitmap/texture identified by a stable resource id. */
    default void texture(String textureId, UiRect rect) {
        // A target may omit optional decorative assets while preserving layout.
    }

    void playerHead(UUID playerId, String playerName, UiRect rect);

    void tooltip(TooltipModel tooltip, int mouseX, int mouseY);
}

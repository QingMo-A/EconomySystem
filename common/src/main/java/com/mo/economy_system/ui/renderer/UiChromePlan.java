package com.mo.economy_system.ui.renderer;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.theme.UiButtonStyle;
import com.mo.economy_system.ui.theme.UiCardStyle;
import java.util.ArrayList;
import java.util.List;

/** Shared source of truth for card and button rectangle geometry, colors, and draw order. */
public final class UiChromePlan {
    private UiChromePlan() {
    }

    public static List<UiFillCommand> cardChrome(UiRect rect, UiCardStyle style,
                                                  boolean hovered) {
        List<UiFillCommand> commands = new ArrayList<>();
        commands.add(new UiFillCommand(rect,
                hovered ? style.backgroundHover() : style.background()));
        int border = hovered ? style.borderHover() : style.border();
        commands.add(new UiFillCommand(new UiRect(rect.x(), rect.y(), rect.width(), 1), border));
        commands.add(new UiFillCommand(
                new UiRect(rect.x(), rect.bottom() - 1, rect.width(), 1), border));
        commands.add(new UiFillCommand(
                new UiRect(rect.right() - 1, rect.y(), 1, rect.height()), border));
        if (style.accentWidth() > 0) {
            commands.add(new UiFillCommand(new UiRect(rect.x(), rect.y(),
                    Math.min(rect.width(), style.accentWidth()), rect.height()),
                    withAlpha(style.accent(),
                            hovered ? style.accentAlphaHover() : style.accentAlpha())));
        }
        return List.copyOf(commands);
    }

    public static List<UiFillCommand> buttonChrome(UiRect rect, UiButtonStyle style,
                                                    boolean hovered, boolean enabled) {
        List<UiFillCommand> commands = new ArrayList<>();
        commands.add(new UiFillCommand(rect,
                enabled && hovered ? style.backgroundHover() : style.background()));
        int border = style.borderColor(hovered, enabled);
        if (style.stripeWidth() > 0) {
            int stripeWidth = Math.min(rect.width(), style.stripeWidth());
            int stripeAlpha = enabled && hovered
                    ? style.stripeAlphaHover() : style.stripeAlpha();
            if (!enabled) {
                stripeAlpha = Math.min(stripeAlpha, 0x60);
            }
            commands.add(new UiFillCommand(
                    new UiRect(rect.x(), rect.y(), stripeWidth, rect.height()),
                    withAlpha(style.accent(), stripeAlpha)));
            if (enabled && hovered && style.glowHeight() > 0) {
                for (int row = 0; row < Math.min(rect.height(), style.glowHeight()); row++) {
                    int alpha = style.glowAlphaStart() - row * style.glowAlphaStep();
                    if (alpha <= 0) {
                        break;
                    }
                    commands.add(new UiFillCommand(new UiRect(rect.x() + stripeWidth,
                            rect.y() + row, Math.max(0, rect.width() - stripeWidth), 1),
                            withAlpha(style.accent(), alpha)));
                }
            }
            commands.add(new UiFillCommand(
                    new UiRect(rect.right() - 1, rect.y(), 1, rect.height()), border));
            commands.add(new UiFillCommand(new UiRect(rect.x() + stripeWidth,
                    rect.bottom() - 1, Math.max(0, rect.width() - stripeWidth), 1), border));
        } else {
            commands.add(new UiFillCommand(
                    new UiRect(rect.x(), rect.y(), rect.width(), 1), border));
            commands.add(new UiFillCommand(
                    new UiRect(rect.x(), rect.bottom() - 1, rect.width(), 1), border));
            commands.add(new UiFillCommand(
                    new UiRect(rect.x(), rect.y(), 1, rect.height()), border));
            commands.add(new UiFillCommand(
                    new UiRect(rect.right() - 1, rect.y(), 1, rect.height()), border));
            if (enabled && rect.width() > 4 && rect.height() > 1) {
                commands.add(new UiFillCommand(
                        new UiRect(rect.x() + 2, rect.y() + 1, rect.width() - 4, 1),
                        0x60FFFFFF));
            }
        }
        return List.copyOf(commands);
    }

    private static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0x00FFFFFF);
    }
}

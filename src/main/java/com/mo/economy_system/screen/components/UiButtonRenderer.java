package com.mo.economy_system.screen.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class UiButtonRenderer {
    public enum TextAlign {
        LEFT,
        CENTER
    }

    private UiButtonRenderer() {
    }

    public static void drawStripedButton(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height,
                                         String text, UiButtonStyle style, boolean hovered) {
        drawStripedButton(guiGraphics, font, x, y, width, height, text, "", style, hovered, TextAlign.LEFT, false);
    }

    public static void drawStripedButton(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height,
                                         String text, String icon, UiButtonStyle style, boolean hovered) {
        drawStripedButton(guiGraphics, font, x, y, width, height, text, icon, style, hovered, TextAlign.LEFT, false);
    }

    public static void drawStripedButton(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height,
                                         String text, String icon, UiButtonStyle style, boolean hovered,
                                         TextAlign align, boolean showArrow) {
        int bgAlpha = hovered ? style.getBgAlphaHover() : style.getBgAlpha();
        int bgColor = (bgAlpha << 24) | 0x000000;
        guiGraphics.fill(x, y, x + width, y + height, bgColor);

        int stripeAlpha = hovered ? style.getStripeAlphaHover() : style.getStripeAlpha();
        int stripeColor = (stripeAlpha << 24) | (style.getAccentColor() & 0x00FFFFFF);
        int stripeWidth = style.getStripeWidth();
        guiGraphics.fill(x, y, x + stripeWidth, y + height, stripeColor);

        if (hovered && style.getGlowHeight() > 0) {
            int glowHeight = style.getGlowHeight();
            for (int i = 0; i < glowHeight; i++) {
                int alpha = style.getGlowAlphaStart() - i * 4;
                if (alpha <= 0) {
                    break;
                }
                int glowColor = (alpha << 24) | (style.getAccentColor() & 0x00FFFFFF);
                guiGraphics.fill(x + stripeWidth, y + i, x + width, y + i + 1, glowColor);
            }
        }

        int borderAlpha = hovered ? style.getBorderAlphaHover() : style.getBorderAlpha();
        int borderColor = (borderAlpha << 24) | 0xFFFFFF;
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);
        guiGraphics.fill(x + stripeWidth, y + height - 1, x + width, y + height, borderColor);

        int padding = style.getPadding();
        int textY = y + (height - font.lineHeight) / 2;
        int textX = x + padding;

        if (icon != null && !icon.isEmpty()) {
            guiGraphics.drawString(font, icon, textX, textY, style.getTextColor(), style.isTextShadow());
            textX += font.width(icon) + 4;
        } else if (align == TextAlign.CENTER) {
            int textWidth = font.width(text);
            textX = x + (width - textWidth) / 2;
        }

        guiGraphics.drawString(font, text, textX, textY, style.getTextColor(), style.isTextShadow());

        if (showArrow) {
            String arrow = ">";
            int arrowWidth = font.width(arrow);
            int arrowX = x + width - padding - arrowWidth;
            guiGraphics.drawString(font, arrow, arrowX, textY, style.getAccentColor(), style.isTextShadow());
        }
    }
}

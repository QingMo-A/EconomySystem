package com.mo.economy_system.target.forge1201.client;

import com.mo.economy_system.ui.text.UiTextMetrics;
import net.minecraft.client.gui.Font;

/** Adapter from the Forge 1.20.1 Font API to common text measurement. */
final class Forge1201UiTextMetrics implements UiTextMetrics {
    private final Font font;

    Forge1201UiTextMetrics(Font font) {
        this.font = font;
    }

    @Override
    public int width(String text) {
        return font.width(text == null ? "" : text);
    }

    @Override
    public int lineHeight() {
        return font.lineHeight;
    }
}

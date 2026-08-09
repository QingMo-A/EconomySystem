package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.ui.text.UiTextMetrics;
import net.minecraft.client.gui.Font;

/** Adapter from the NeoForge 1.21.1 Font API to common text measurement. */
final class NeoForge1211UiTextMetrics implements UiTextMetrics {
    private final Font font;

    NeoForge1211UiTextMetrics(Font font) {
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

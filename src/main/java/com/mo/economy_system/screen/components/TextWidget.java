package com.mo.economy_system.screen.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class TextWidget extends AbstractWidget {
    private final Font font;
    private final Component text;
    private final int color;

    public TextWidget(Font font, Component text, int x, int y, int color) {
        super(x, y, font.width(text), font.lineHeight, text);
        this.font = font;
        this.text = text;
        this.color = color;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.drawString(font, text, getX(), getY(), color, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }
}


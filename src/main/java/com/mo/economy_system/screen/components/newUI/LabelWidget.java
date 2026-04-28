package com.mo.economy_system.screen.components.newUI;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.Font;

public class LabelWidget extends AbstractWidget {
    private final Font font;
    private final int color;
    private final boolean shadow;
    private float scale = 1.0f;

    public LabelWidget(Font font, Component text, int x, int y, int color, boolean shadow) {
        super(x, y, 0, 0, text); // 延迟设置宽高
        this.font = font;
        this.color = color;
        this.shadow = shadow;
        updateSize(); // 初始化宽高
    }

    public LabelWidget setScale(float scale) {
        this.scale = scale;
        updateSize();
        return this;
    }

    private void updateSize() {
        this.width = (int) (font.width(getMessage()) * scale);
        this.height = (int) (font.lineHeight * scale);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushPose();
        graphics.pose().translate(getX(), getY(), 0);
        graphics.pose().scale(scale, scale, 1.0f);

        if (shadow) {
            graphics.drawString(font, getMessage(), 0, 0, color);
        } else {
            graphics.drawString(font, getMessage(), 0, 0, color, false);
        }

        graphics.pose().popPose();
    }

    @Override
    protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narrationElementOutput) {}

    public void setText(Component text) {
        setMessage(text);
        updateSize();
    }
}



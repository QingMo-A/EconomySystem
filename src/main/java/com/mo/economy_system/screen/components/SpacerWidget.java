package com.mo.economy_system.screen.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class SpacerWidget extends AbstractWidget {
    private boolean expandHorizontally;
    private boolean expandVertically;

    public SpacerWidget(boolean expandHorizontally, boolean expandVertically) {
        super(0, 0, 0, 0, Component.empty());
        this.expandHorizontally = expandHorizontally;
        this.expandVertically = expandVertically;
    }

    public boolean isExpandHorizontally() {
        return expandHorizontally;
    }

    public boolean isExpandVertically() {
        return expandVertically;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 空白填充控件，不渲染任何内容
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }
}


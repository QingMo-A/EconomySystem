package com.mo.economy_system.screen.newUI;

import com.mo.economy_system.screen.components.ContainerWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public abstract class Screen_Father extends Screen {
    public Screen_Father(Component title) {
        super(title);
    }

    /**
     * 自动注册 ContainerWidget 及其子控件
     */
    public void addContainerWidget(ContainerWidget container) {
        this.addRenderableWidget(container); // 先注册容器
        for (AbstractWidget child : container.getChildren()) {
            this.addRenderableWidget(child); // 让 Screen 识别子控件
        }
    }


    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics); // 渲染背景
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}

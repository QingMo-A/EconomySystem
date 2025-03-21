package com.mo.economy_system.screen.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

public abstract class ContainerWidget extends AbstractWidget {
    protected final List<AbstractWidget> children = new ArrayList<>();
    protected BorderStyle borderStyle = new BorderStyle();

    // 内边距（左，上，右，下）
    protected int paddingLeft = 0;
    protected int paddingTop = 0;
    protected int paddingRight = 0;
    protected int paddingBottom = 0;

    // 最大尺寸
    protected int maxWidth = Integer.MAX_VALUE;
    protected int maxHeight = Integer.MAX_VALUE;

    public ContainerWidget(int width, int height) {
        super(0, 0, width, height, Component.empty());
    }

    // 设置位置并更新子控件布局
    public void setPosition(int x, int y) {
        this.setX(x);
        this.setY(y);
        updateLayout();
    }

    public void setMaxSize(int maxWidth, int maxHeight) {
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        updateLayout();
    }

    // 允许子类实现具体布局逻辑
    public abstract void updateLayout();

    // 设置边框样式
    public void setBorderStyle(BorderStyle style) {
        this.borderStyle = style;
    }

    public void addChild(AbstractWidget child) {
        children.add(child);
    }

    // 添加 `setPadding()` 方法
    public void setPadding(int left, int top, int right, int bottom) {
        this.paddingLeft = left;
        this.paddingTop = top;
        this.paddingRight = right;
        this.paddingBottom = bottom;
        updateLayout();
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        for (AbstractWidget child : children) {
            child.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        renderBorder(guiGraphics);
    }

    protected void renderBorder(GuiGraphics guiGraphics) {
        if (borderStyle == null || borderStyle.getThickness() <= 0) return;

        int t = borderStyle.getThickness();
        int color = borderStyle.getColor();

        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();

        if (borderStyle.isShowTop()) {
            guiGraphics.fill(x, y, x + width, y + t, color);
        }
        if (borderStyle.isShowBottom()) {
            guiGraphics.fill(x, y + height - t, x + width, y + height, color);
        }
        if (borderStyle.isShowLeft()) {
            guiGraphics.fill(x, y, x + t, y + height, color);
        }
        if (borderStyle.isShowRight()) {
            guiGraphics.fill(x + width - t, y, x + width, y + height, color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (AbstractWidget child : children) {
            if (child.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public List<AbstractWidget> getChildren() {
        return children;
    }

    public BorderStyle getBorderStyle() {
        return borderStyle;
    }
}


package com.mo.economy_system.screen.components;

public class BorderStyle {
    // 边框颜色（ARGB格式，例如 0xFF0000FF 表示蓝色）
    private int color = 0xFF000000;
    // 边框粗细（像素）
    private int thickness = 1;
    // 是否显示各边（上、下、左、右）
    private boolean showTop = true, showBottom = true, showLeft = true, showRight = true;

    // 构造方法
    public BorderStyle() {}

    // 链式配置方法（方便调用）
    public BorderStyle color(int argb) { this.color = argb; return this; }
    public BorderStyle thickness(int px) { this.thickness = px; return this; }
    public BorderStyle showEdges(boolean top, boolean bottom, boolean left, boolean right) {
        this.showTop = top;
        this.showBottom = bottom;
        this.showLeft = left;
        this.showRight = right;
        return this;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getThickness() {
        return thickness;
    }

    public void setThickness(int thickness) {
        this.thickness = thickness;
    }

    public boolean isShowTop() {
        return showTop;
    }

    public void setShowTop(boolean showTop) {
        this.showTop = showTop;
    }

    public boolean isShowBottom() {
        return showBottom;
    }

    public void setShowBottom(boolean showBottom) {
        this.showBottom = showBottom;
    }

    public boolean isShowLeft() {
        return showLeft;
    }

    public void setShowLeft(boolean showLeft) {
        this.showLeft = showLeft;
    }

    public boolean isShowRight() {
        return showRight;
    }

    public void setShowRight(boolean showRight) {
        this.showRight = showRight;
    }
}

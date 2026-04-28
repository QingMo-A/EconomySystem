package com.mo.economy_system.screen.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class AnimatedHighLevelTextField extends HighLevelTextField {
    private int startX, startY;    // 动画起始位置
    private int targetX, targetY;  // 动画目标位置
    private long animationStartTime; // 动画开始时间
    private final int animationDuration; // 动画时长（毫秒）
    private boolean isAnimating = false;

    public AnimatedHighLevelTextField(Font font, int startX, int startY, int width, int height, int animationDuration) {
        super(font, startX, startY, width, height, Component.empty());
        this.startX = startX;
        this.startY = startY;
        this.targetX = startX;
        this.targetY = startY;
        this.animationDuration = animationDuration;
    }

    public AnimatedHighLevelTextField(Font font, int startX, int startY, int width, int height, int animationDuration, Component component) {
        super(font, startX, startY, width, height, component);
        this.startX = startX;
        this.startY = startY;
        this.targetX = startX;
        this.targetY = startY;
        this.animationDuration = animationDuration;
    }

    /**
     * 启动位置动画
     * @param targetX 目标X坐标
     * @param targetY 目标Y坐标
     */
    public void startMoveAnimation(int targetX, int targetY) {
        this.startX = this.getX();
        this.startY = this.getY();
        this.targetX = targetX;
        this.targetY = targetY;
        this.animationStartTime = System.currentTimeMillis();
        this.isAnimating = true;
    }

    @Override
    public void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        updatePosition(); // 更新坐标
        super.renderWidget(gui, mouseX, mouseY, partialTick);
    }

    private void updatePosition() {
        if (!isAnimating) return;

        // 计算动画进度
        float progress = (System.currentTimeMillis() - animationStartTime) / (float) animationDuration;
        progress = Mth.clamp(progress, 0, 1);

        // 使用与按钮相同的缓动函数（easeOutQuad）
        float easedProgress = 1 - (1 - progress) * (1 - progress);

        // 计算当前坐标
        int currentX = (int) (startX + (targetX - startX) * easedProgress);
        int currentY = (int) (startY + (targetY - startY) * easedProgress);

        // 更新输入框位置
        this.setX(currentX);
        this.setY(currentY);

        // 动画结束
        if (progress >= 1.0f) {
            isAnimating = false;
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        // 动态计算碰撞区域
        return mouseX >= this.getX() &&
                mouseX < this.getX() + this.width &&
                mouseY >= this.getY() &&
                mouseY < this.getY() + this.height;
    }
}
package com.mo.economy_system.screen.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class AnimatedButton extends Button {
    private int targetX, targetY; // 目标位置
    private int startX, startY;   // 起始位置
    private long startTime;       // 动画开始时间
    private final int duration;   // 动画持续时间（毫秒）

    /**
     * 创建一个AnimatedButton实例，该按钮可以动画形式移动到指定位置
     *
     * @param startX 按钮起始X坐标
     * @param startY 按钮起始Y坐标
     * @param targetX 按钮目标X坐标
     * @param targetY 按钮目标Y坐标
     * @param width 按钮宽度
     * @param height 按钮高度
     * @param text 按钮上显示的文本或组件
     * @param duration 动画持续时间（毫秒）
     * @param onPress 按钮按下时的回调接口
     */
    public AnimatedButton(int startX, int startY, int targetX, int targetY, int width, int height, Component text, int duration, OnPress onPress) {
        // 调用父类构造方法初始化按钮的基本属性
        super(startX, startY, width, height, text, onPress, DEFAULT_NARRATION);
        this.targetX = targetX;
        this.targetY = targetY;
        this.startX = startX;
        this.startY = startY;
        this.duration = duration;
        this.startTime = System.currentTimeMillis(); // 记录动画开始时间
    }


    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 计算动画进度（0.0 ~ 1.0）
        float progress = (System.currentTimeMillis() - startTime) / (float) duration;
        progress = Mth.clamp(progress, 0, 1); // 限制在 [0,1] 范围内

        // 使用缓动函数（例如：easeOutQuad）
        float easedProgress = 1 - (1 - progress) * (1 - progress);

        // 计算当前坐标
        int currentX = (int) (startX + (targetX - startX) * easedProgress);
        int currentY = (int) (startY + (targetY - startY) * easedProgress);

        // 更新按钮位置
        this.setX(currentX);
        this.setY(currentY);

        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    }
}

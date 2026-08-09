package com.mo.economy_system.screen.components;

import com.mo.economy_system.utils.Util_Animation;
import net.minecraft.util.Mth;

public class TextAnimation {
    private final int startX, startY;    // 起始坐标
    private final int targetX, targetY;  // 目标坐标
    private final long startTime;        // 动画开始时间
    private final int duration;          // 动画持续时间（毫秒）
    private final float startAlpha;      // 初始透明度（0~1）
    private final float targetAlpha;     // 目标透明度（0~1）

    private boolean loop = false;
    private int startColor = 0xFFFFFF; // 白色
    private int endColor = 0xFFFFFF;

    public TextAnimation(int startX, int startY, int targetX, int targetY, float startAlpha, float targetAlpha, int duration) {
        this.startX = startX;
        this.startY = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.startAlpha = startAlpha;
        this.targetAlpha = targetAlpha;
        this.duration = duration;
        this.startTime = System.currentTimeMillis();
    }

    // 新增方法：设置循环
    public TextAnimation setLoop(boolean loop) {
        this.loop = loop;
        return this;
    }

    // 新增方法：设置颜色渐变
    public TextAnimation setColor(int startColor, int endColor) {
        this.startColor = startColor;
        this.endColor = endColor;
        return this;
    }

    // 获取当前动画进度（0~1）
    public float getProgress() {
        long elapsed = System.currentTimeMillis() - startTime;
        if (loop) {
            return (elapsed % duration) / (float) duration;
        } else {
            return Mth.clamp(elapsed / (float) duration, 0, 1);
        }
    }

    public int getCurrentColor() {
        return Util_Animation.lerpRGB(getEasedProgress(), startColor, endColor);
    }

    // 计算缓动后的进度（例如：easeOutQuad）
    public float getEasedProgress() {
        float progress = getProgress();
        return 1 - (1 - progress) * (1 - progress);
    }

    // 获取当前透明度
    public float getCurrentAlpha() {
        return Mth.lerp(getEasedProgress(), startAlpha, targetAlpha);
    }

    // 获取当前坐标
    public int getCurrentX() {
        return (int) Mth.lerp(getEasedProgress(), startX, targetX);
    }

    public int getCurrentY() {
        return (int) Mth.lerp(getEasedProgress(), startY, targetY);
    }
}

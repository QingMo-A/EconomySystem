package com.mo.economy_system.screen.components;

import net.minecraft.util.Mth;

public class ItemIconAnimation {
    private final int startX, startY;    // 起始位置
    private final int targetX, targetY;  // 目标位置
    private final float startAlpha;      // 起始透明度 (0.0~1.0)
    private final float targetAlpha;     // 目标透明度
    private final float startScale;      // 起始缩放比例
    private final float targetScale;     // 目标缩放比例
    private final long duration;         // 动画时长（毫秒）
    private long startTime;              // 动画开始时间
    private boolean isCompleted = false; // 是否完成

    public ItemIconAnimation(int startX, int startY, int targetX, int targetY,
                             float startAlpha, float targetAlpha,
                             float startScale, float targetScale,
                             long duration) {
        this.startX = startX;
        this.startY = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.startAlpha = startAlpha;
        this.targetAlpha = targetAlpha;
        this.startScale = startScale;
        this.targetScale = targetScale;
        this.duration = duration;
        this.startTime = System.currentTimeMillis();
    }

    // 更新动画状态（返回是否完成）
    public boolean update() {
        if (isCompleted) return true;
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= duration) {
            isCompleted = true;
        }
        return isCompleted;
    }

    // 获取当前动画参数（供渲染使用）
    public int getCurrentX() {
        float progress = getProgress();
        return (int) (startX + (targetX - startX) * progress);
    }

    public int getCurrentY() {
        float progress = getProgress();
        return (int) (startY + (targetY - startY) * progress);
    }

    public float getCurrentAlpha() {
        return Mth.lerp(getProgress(), startAlpha, targetAlpha);
    }

    public float getCurrentScale() {
        return Mth.lerp(getProgress(), startScale, targetScale);
    }

    private float getProgress() {
        long elapsed = System.currentTimeMillis() - startTime;
        return Mth.clamp(elapsed / (float) duration, 0f, 1f);
    }
}

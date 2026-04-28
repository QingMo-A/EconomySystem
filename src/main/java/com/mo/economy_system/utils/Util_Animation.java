package com.mo.economy_system.utils;

import net.minecraft.util.Mth;

public class Util_Animation {
    /**
     * 线性插值两个颜色（ARGB格式）
     * @param progress 进度（0~1）
     * @param startColor 起始颜色（ARGB）
     * @param endColor 目标颜色（ARGB）
     * @return 插值后的颜色（ARGB）
     */
    public static int lerpRGB(float progress, int startColor, int endColor) {
        // 分解起始颜色的ARGB通道
        int a1 = (startColor >> 24) & 0xFF;
        int r1 = (startColor >> 16) & 0xFF;
        int g1 = (startColor >> 8) & 0xFF;
        int b1 = startColor & 0xFF;

        // 分解目标颜色的ARGB通道
        int a2 = (endColor >> 24) & 0xFF;
        int r2 = (endColor >> 16) & 0xFF;
        int g2 = (endColor >> 8) & 0xFF;
        int b2 = endColor & 0xFF;

        // 对每个通道进行线性插值
        int a = (int) Mth.lerp(progress, a1, a2);
        int r = (int) Mth.lerp(progress, r1, r2);
        int g = (int) Mth.lerp(progress, g1, g2);
        int b = (int) Mth.lerp(progress, b1, b2);

        // 合并通道为ARGB整数
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}

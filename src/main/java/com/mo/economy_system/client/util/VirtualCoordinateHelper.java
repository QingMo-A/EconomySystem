package com.mo.economy_system.client.util;

import net.minecraft.client.gui.screens.Screen;

/**
 * 虚拟坐标系统辅助类
 *
 * 设计原理：
 * 1. 虚拟基准尺寸 640×360（基于 2560×1440 全屏 + GUI缩放4 的内部渲染尺寸）
 * 2. 所有元素按虚拟尺寸设计，运行时自动等比缩放到实际屏幕
 * 3. 保证不同分辨率、不同 GUI 缩放下 UI 显示一致
 */
public class VirtualCoordinateHelper {

    /** 虚拟基准宽度 */
    public static final int BASE_WIDTH = 640;

    /** 虚拟基准高度 */
    public static final int BASE_HEIGHT = 360;

    /**
     * 计算缩放比例和虚拟画布尺寸
     *
     * @param screen 当前屏幕
     * @param result 用于存储计算结果
     */
    public static void calculateVirtualSize(Screen screen, VirtualSizeResult result) {
        // 计算屏幕尺寸到基准尺寸的缩放比例
        float scaleX = (float) screen.width / BASE_WIDTH;
        float scaleY = (float) screen.height / BASE_HEIGHT;

        // 取较小值，确保内容完整显示（可能有黑边，但不会裁剪）
        result.uiScale = Math.min(scaleX, scaleY);

        // 反向计算虚拟画布尺寸
        result.virtualWidth = (int) (screen.width / result.uiScale);
        result.virtualHeight = (int) (screen.height / result.uiScale);
    }

    /**
     * 虚拟尺寸计算结果
     */
    public static class VirtualSizeResult {
        /** 虚拟坐标到屏幕坐标的缩放比例 */
        public float uiScale;

        /** 虚拟画布宽度 */
        public int virtualWidth;

        /** 虚拟画布高度 */
        public int virtualHeight;
    }
}

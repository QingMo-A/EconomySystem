package com.mo.economy_system.server.serverui.serverscreen;

import net.minecraft.client.Minecraft;


//里面写了隐藏显示其他ui以及叫出鼠标的方法，供事件调用

public class ServerScreenUI {
    private static boolean SHOW_UI = false;

    // 标记：是否正在打开子屏幕（防止 toggleUI 重新打开主界面）
    private static boolean OPENING_SUB_SCREEN = false;

    // 标记：子屏幕是否正在显示（防止 ClientEventHandler 强制恢复主UI）
    private static boolean SUB_SCREEN_ACTIVE = false;

    // 标记：是否正在从子屏幕返回（用于跳过打开动画）
    private static boolean RETURNING_FROM_SUB_SCREEN = false;

    public static void setShowUI(boolean state) {
        SHOW_UI = state;
    }

    public static void toggleUI() {
        // 如果正在打开子屏幕，不执行 toggleUI
        if (OPENING_SUB_SCREEN) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        SHOW_UI = !SHOW_UI;

        //显示ui叫出鼠标
        if (SHOW_UI) {
            //释放鼠标，显示光标
            mc.mouseHandler.releaseMouse();
            // 打开Screen
            mc.setScreen(new ServerScreenUI_Screen());
        } else {
            //关闭Screen
            if (mc.screen instanceof ServerScreenUI_Screen) {
                mc.setScreen(null);
            }
            //重新捕获鼠标，隐藏光标
            mc.mouseHandler.grabMouse();
        }
    }

    /**
     * 打开子屏幕（不关闭 ServerScreenUI）
     * 调用此方法后，ServerScreenUI 的 onClose 不会触发 toggleUI
     */
    public static void openSubScreen(net.minecraft.client.gui.screens.Screen subScreen) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 设置标记，防止 toggleUI 重新打开主界面
        OPENING_SUB_SCREEN = true;
        // 标记子屏幕正在显示，防止 ClientEventHandler 强制恢复主UI
        SUB_SCREEN_ACTIVE = true;
        // 先将 SHOW_UI 设为 false，这样 onClose() 中的 toggleUI() 不会抓取鼠标
        SHOW_UI = false;
        // 打开子屏幕（这时旧屏幕的 onClose() 会被调用，但会直接返回）
        mc.setScreen(subScreen);
        // 恢复状态
        SHOW_UI = true;
        OPENING_SUB_SCREEN = false;
    }

    //获取UI显示状态
    public static boolean isShowUI() {
        return SHOW_UI;
    }

    // 检查是否正在打开子屏幕
    public static boolean isOpeningSubScreen() {
        return OPENING_SUB_SCREEN;
    }

    // 检查子屏幕是否正在显示
    public static boolean isSubScreenActive() {
        return SUB_SCREEN_ACTIVE;
    }

    // 子屏幕关闭时调用此方法
    public static void onSubScreenClosed() {
        SUB_SCREEN_ACTIVE = false;
    }

    // 设置从子屏幕返回标记
    public static void setReturningFromSubScreen(boolean state) {
        RETURNING_FROM_SUB_SCREEN = state;
    }

    // 检查是否正在从子屏幕返回
    public static boolean isReturningFromSubScreen() {
        return RETURNING_FROM_SUB_SCREEN;
    }
}
package com.mo.economy_system.task.taskui;

import com.mo.economy_system.server.serverui.ServerInformationDisplay;
import net.minecraft.client.Minecraft;

public class TaskUI {
    private static boolean SHOW_UI = false;
    static boolean otherUiShowState = false;

    public static void setShowUI(boolean state) {
        SHOW_UI = state;
    }

    public static void toggleUI() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        SHOW_UI = !SHOW_UI;

        //显示ui叫出鼠标且隐藏其他ui
        if (SHOW_UI) {
            //释放鼠标，显示光标
            mc.mouseHandler.releaseMouse();
            //隐藏其他UI
            otherUiShowState = ServerInformationDisplay.isShowUI();
            if (otherUiShowState) {
                ServerInformationDisplay.toggleUI();
            }
            // 打开Screen
            mc.setScreen(new TaskUI_Screen());
        } else {
            //重新捕获鼠标，隐藏光标
            mc.mouseHandler.grabMouse();
            //恢复其他UI的原始状态
            if (otherUiShowState && !ServerInformationDisplay.isShowUI()) {
                ServerInformationDisplay.toggleUI(); // 恢复显示
            }
            // 关闭Screen
            if (mc.screen instanceof TaskUI_Screen) {
                mc.setScreen(null);
            }
        }
    }

    //获取UI显示状态
    public static boolean isShowUI() {
        return SHOW_UI;
    }
}
package com.mo.economy_system.server.serverui.serverscreen;

import com.mo.economy_system.server.serverui.ServerInformationDisplay;
import net.minecraft.client.Minecraft;


//里面写了隐藏显示其他ui以及叫出鼠标的方法，供事件调用

public class ServerScreenUI {
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
            //隐藏其他UI，并记录原始状态
            otherUiShowState = ServerInformationDisplay.isShowUI();
            if (otherUiShowState) {
                ServerInformationDisplay.toggleUI();
            }
            // 打开Screen
            mc.setScreen(new ServerScreenUI_Screen());
        } else {
            //关闭Screen
            if (mc.screen instanceof ServerScreenUI_Screen) {
                mc.setScreen(null);
            }
            //重新捕获鼠标，隐藏光标
            mc.mouseHandler.grabMouse();
            //恢复其他UI的原始状态
            // 动态检查：如果之前信息面板是打开的（otherUiShowState==true），现在关闭服务器UI后应该恢复它
            if (otherUiShowState) {
                if (!ServerInformationDisplay.isShowUI()) {
                    ServerInformationDisplay.toggleUI(); // 恢复显示
                }
            } else {
                // 如果之前信息面板是关闭的，确保它保持关闭状态
                // （防止用户在服务器UI打开时按O键打开了信息面板，导致关闭服务器UI后信息面板异常显示）
                if (ServerInformationDisplay.isShowUI()) {
                    ServerInformationDisplay.toggleUI(); // 关闭它
                }
            }
        }
    }

    //获取UI显示状态
    public static boolean isShowUI() {
        return SHOW_UI;
    }
}
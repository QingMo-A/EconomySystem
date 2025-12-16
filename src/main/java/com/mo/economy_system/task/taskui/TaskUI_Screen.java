package com.mo.economy_system.task.taskui;

import com.mo.economy_system.server.serverui.ServerInformationDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TaskUI_Screen extends Screen {
    // 复用你的注释和样式参数
    private static final int BACKGROUND_ALPHA = 128; //背景透明度
    private static final float UI_WIDTH_PERCENT = 0.65F; //宽度百分比
    private static final float UI_HEIGHT_PERCENT = 0.7F; //高度百分比
    private static final int BORDER_COLOR = 0xFFFFFFFF; // 白色边框（ARGB：0xFF=全透，FFFFFF=白色）
    private static final int BG_COLOR = (BACKGROUND_ALPHA << 24) | 0x000000; //半透明黑背景

    // Screen实例属性
    private int screenWidth;
    private int screenHeight;
    private int screenUIWidth;
    private int screenUIHeight;
    private int uiX;
    private int uiY;

    public TaskUI_Screen() {
        super(Component.literal("任务界面"));
    }

    @Override
    protected void init() {
        super.init();
        this.screenWidth = this.width;
        this.screenHeight = this.height;
        this.screenUIWidth = (int) (screenWidth * UI_WIDTH_PERCENT);
        this.screenUIHeight = (int) (screenHeight * UI_HEIGHT_PERCENT);
        this.uiX = (screenWidth - screenUIWidth) / 2; // 水平居中：(屏幕宽 - UI宽) / 2
        this.uiY = (screenHeight - screenUIHeight) / 2; // 垂直居中：(屏幕高 - UI高) / 2
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        //渲染背景
        guiGraphics.fill(RenderType.gui(), uiX, uiY, uiX + screenUIWidth, uiY + screenUIHeight, BG_COLOR);
        //渲染白色边框
        //上边框：y坐标=uiY，高度1像素
        guiGraphics.fill(RenderType.gui(), uiX, uiY, uiX + screenUIWidth, uiY + 1, BORDER_COLOR);
        //下边框：y坐标=uiY+UI_HEIGHT-1
        guiGraphics.fill(RenderType.gui(), uiX, uiY + screenUIHeight - 1, uiX + screenUIWidth, uiY + screenUIHeight, BORDER_COLOR);
        //左边框：x坐标=uiX，宽度1像素
        guiGraphics.fill(RenderType.gui(), uiX, uiY, uiX + 1, uiY + screenUIHeight, BORDER_COLOR);
        //右边框：x坐标=uiX+UI_WIDTH-1
        guiGraphics.fill(RenderType.gui(), uiX + screenUIWidth - 1, uiY, uiX + screenUIWidth, uiY + screenUIHeight, BORDER_COLOR);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Minecraft mc = Minecraft.getInstance();
        //如果是esc
        if (keyCode == 256 || keyCode == 85) {
            //调用关闭逻辑
            TaskUI.toggleUI();
            return true; // 返回true表示按键已处理，不再传递给其他逻辑
        }
        //其他按键交给父类处理
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        super.onClose();
        Minecraft mc = Minecraft.getInstance();
        mc.mouseHandler.grabMouse();
        //恢复其他UI
        if (TaskUI.otherUiShowState && !ServerInformationDisplay.isShowUI()) {
            ServerInformationDisplay.toggleUI();
        }
        TaskUI.setShowUI(false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
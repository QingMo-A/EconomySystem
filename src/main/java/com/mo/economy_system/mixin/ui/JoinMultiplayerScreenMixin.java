package com.mo.economy_system.mixin.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * JoinMultiplayerScreen Mixin
 * 自定义服务器列表界面背景
 */
@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenMixin extends Screen {

    private static final int BG_TOP = 0xFF0A0A1A;
    private static final int BG_BOTTOM = 0xFF00000A;
    private static final int ACCENT_BLUE = 0xFF0088FF;
    private static final int ACCENT_GREEN = 0xFF55FF55;

    @Unique
    private long openTime = 0;

    protected JoinMultiplayerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void economySystem$init(CallbackInfo ci) {
        this.openTime = System.currentTimeMillis();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void economySystem$renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        // 渲染自定义渐变背景
        guiGraphics.fillGradient(0, 0, this.width, this.height / 2, BG_TOP, BG_TOP);
        guiGraphics.fillGradient(0, this.height / 2, this.width, this.height, BG_BOTTOM, BG_BOTTOM);

        // 中间渐变过渡
        guiGraphics.fillGradient(0, this.height / 2 - 50, this.width, this.height / 2 + 50, 0x00000000, 0x44000044);

        // 底部装饰线
        guiGraphics.fill(0, this.height - 2, this.width, this.height, ACCENT_BLUE);

        // 右上角服务器标识
        long time = System.currentTimeMillis();
        float pulse = (float) Math.sin(time / 500.0) * 0.3f + 0.7f;
        int brandAlpha = (int) (180 * pulse) << 24;
        String brandText = "§b§lDreaming§d§lFish";
        int brandX = this.width - font.width(brandText) - 8;
        guiGraphics.drawString(this.font, brandText, brandX, 8, 0xFFFFFF | brandAlpha, false);

        // 左上角服务器图标
        String serverIcon = "§a⚁";
        guiGraphics.drawString(this.font, serverIcon, 8, 8, ACCENT_GREEN, false);
    }
}

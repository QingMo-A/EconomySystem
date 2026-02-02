package com.mo.economy_system.mixin.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SelectWorldScreen Mixin
 * 自定义世界选择界面背景
 */
@Mixin(SelectWorldScreen.class)
public abstract class SelectWorldScreenMixin extends Screen {

    private static final int ACCENT_BLUE = 0xFF0088FF;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_GRAY = 0xFFAAAAAA;
    private static final int GLASS_TOP = 0x66FFFFFF;
    private static final int GLASS_BOTTOM = 0x33000000;
    private static final int GLASS_BORDER = 0x55FFFFFF;
    private static final int GLASS_SHADOW = 0x33000000;
    private static final int GLASS_HIGHLIGHT = 0x66FFFFFF;

    private static final String MINECRAFT_VERSION = "§7Minecraft §f1.20.1";
    private static final String MOJANG_COPYRIGHT = "§8Copyright Mojang AB. Do not distribute!";
    private static final String DREAMINGFISH_TITLE = "§b§lDreaming§d§lFish";
    private static final String DREAMINGFISH_COPYRIGHT = "© 2026 DreamingFish - EconomySystem";
    private static final String DEVELOPER_COPYRIGHT = "  Developed by QINGMO & HANHANYU";

    @Unique
    private long openTime = 0;

    @Unique
    private static final ResourceLocation BACKGROUND_TEXTURE =
        new ResourceLocation("economy_system", "background.png");

    @Shadow
    protected EditBox searchBox;

    @Shadow
    private WorldSelectionList list;

    protected SelectWorldScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void economySystem$init(CallbackInfo ci) {
        this.openTime = System.currentTimeMillis();
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void economySystem$renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ci.cancel();

        // Background texture to match title screen
        guiGraphics.blit(BACKGROUND_TEXTURE,
            0, 0, this.width, this.height,
            0, 0, 256, 144, 256, 144);

        // Subtle glass header bar
        int headerHeight = 36;
        economySystem$renderGlassPanel(guiGraphics, 0, 0, this.width, headerHeight, 0xAAFFFFFF);
        guiGraphics.fill(0, headerHeight - 2, this.width, headerHeight, ACCENT_BLUE);

        // Glass panel behind list area
        int listTop = 48;
        int listBottom = this.height - 64;
        int listHeight = Math.max(0, listBottom - listTop);
        int listX = 16;
        int listWidth = this.width - 32;
        if (listHeight > 0) {
            economySystem$renderGlassPanel(guiGraphics, listX, listTop, listWidth, listHeight, 0x99FFFFFF);
        }

        // Top-left brand
        guiGraphics.drawString(this.font, DREAMINGFISH_TITLE, 8, 8, TEXT_WHITE, false);

        // Center title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, TEXT_WHITE);

        // Bottom-left version/copyright
        guiGraphics.drawString(this.font, MINECRAFT_VERSION, 6, this.height - 22, TEXT_GRAY, false);
        guiGraphics.drawString(this.font, MOJANG_COPYRIGHT, 6, this.height - 10, TEXT_GRAY, false);

        // Top-right copyright
        int rightX = this.width - this.font.width(DREAMINGFISH_COPYRIGHT) - 6;
        guiGraphics.drawString(this.font, "§6" + DREAMINGFISH_COPYRIGHT, rightX, 8, TEXT_GRAY, false);
        rightX = this.width - this.font.width(DEVELOPER_COPYRIGHT) - 6;
        guiGraphics.drawString(this.font, "§6" + DEVELOPER_COPYRIGHT, rightX, 20, TEXT_GRAY, false);

        // Render list + search box
        if (this.list != null) {
            this.list.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        if (this.searchBox != null) {
            this.searchBox.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        // Render buttons and other widgets
        for (Renderable renderable : this.renderables) {
            if (renderable == this.list || renderable == this.searchBox) {
                continue;
            }
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Unique
    private void economySystem$renderGlassPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int tint) {
        guiGraphics.fillGradient(x, y, x + width, y + height, GLASS_TOP, GLASS_BOTTOM);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, economySystem$withAlpha(tint, 0x12));
        guiGraphics.fill(x, y, x + width, y + 1, GLASS_BORDER);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, GLASS_SHADOW);
        guiGraphics.fill(x, y, x + 1, y + height, GLASS_BORDER);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, GLASS_SHADOW);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + 2, GLASS_HIGHLIGHT);
        guiGraphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, GLASS_SHADOW);
        economySystem$renderGlassNoise(guiGraphics, x, y, width, height);
    }

    @Unique
    private void economySystem$renderGlassNoise(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        if (width < 20 || height < 20) {
            return;
        }
        int maxX = x + width - 6;
        int maxY = y + height - 6;
        for (int i = 0; i < 6; i++) {
            int nx = x + 6 + (i * 23 + x) % (maxX - x);
            int ny = y + 6 + (i * 17 + y) % (maxY - y);
            guiGraphics.fill(nx, ny, nx + 1, ny + 1, 0x22FFFFFF);
        }
    }

    @Unique
    private int economySystem$withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }
}

package com.mo.economy_system.mixin.ui;

import com.mo.economy_system.client.util.UiBackgroundRenderer;
import net.minecraft.client.gui.GuiGraphics;
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
 * Replace dirt background with custom texture and modern rounded list panel.
 */
@Mixin(SelectWorldScreen.class)
public abstract class SelectWorldScreenMixin extends Screen {

    private static final int PANEL_MARGIN = 12;
    private static final int PANEL_TOP = 48;
    private static final int PANEL_BOTTOM = 64;
    private static final int PANEL_FILL = 0xCC101820;
    private static final int PANEL_BORDER = 0x55FFFFFF;

    @Unique
    private static final ResourceLocation BACKGROUND_TEXTURE =
        ResourceLocation.fromNamespaceAndPath("economy_system", "background.png");

    @Shadow
    private WorldSelectionList list;

    protected SelectWorldScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void economySystem$disableListDirt(CallbackInfo ci) {
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void economySystem$renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        UiBackgroundRenderer.renderCover(guiGraphics, BACKGROUND_TEXTURE, this.width, this.height);
        renderListPanel(guiGraphics);
    }

    @Unique
    private void renderListPanel(GuiGraphics guiGraphics) {
        int x = 0;
        int y = PANEL_TOP;
        int width = this.width;
        int height = this.height - PANEL_TOP - PANEL_BOTTOM;
        if (width <= 0 || height <= 0) {
            return;
        }
        guiGraphics.fill(x, y, x + width, y + height, PANEL_FILL);
        guiGraphics.fill(x, y, x + width, y + 2, PANEL_BORDER);
        guiGraphics.fill(x, y + height - 2, x + width, y + height, PANEL_BORDER);
    }

}

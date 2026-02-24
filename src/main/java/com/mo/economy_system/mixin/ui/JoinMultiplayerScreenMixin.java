package com.mo.economy_system.mixin.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * JoinMultiplayerScreen Mixin
 * Replace dirt background with custom texture.
 */
@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenMixin extends Screen {

    private static final int PANEL_MARGIN = 16;
    private static final int PANEL_TOP = 32;
    private static final int PANEL_BOTTOM = 64;
    private static final int PANEL_FILL = 0xCC0F1525;
    private static final int PANEL_BORDER = 0x55FFFFFF;

    @Unique
    private static final ResourceLocation BACKGROUND_TEXTURE =
        new ResourceLocation("economy_system", "background.png");

    @Shadow
    protected ServerSelectionList serverSelectionList;

    protected JoinMultiplayerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void economySystem$disableListDirt(CallbackInfo ci) {
        if (this.serverSelectionList != null) {
            this.serverSelectionList.setRenderBackground(false);
            this.serverSelectionList.setRenderTopAndBottom(false);
        }
    }

    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/multiplayer/JoinMultiplayerScreen;renderBackground(Lnet/minecraft/client/gui/GuiGraphics;)V"
        )
    )
    private void economySystem$renderCustomBackground(JoinMultiplayerScreen instance, GuiGraphics guiGraphics) {
        guiGraphics.blit(BACKGROUND_TEXTURE,
            0, 0, this.width, this.height,
            0, 0, 256, 144, 256, 144);
        renderPanel(guiGraphics);
    }

    @Unique
    private void renderPanel(GuiGraphics guiGraphics) {
        int x = 0;
        int y = PANEL_TOP;
        int width = this.width;
        int height = this.height - y - PANEL_BOTTOM;
        if (width <= 0 || height <= 0) {
            return;
        }
        guiGraphics.fill(x, y, x + width, y + height, PANEL_FILL);
        guiGraphics.fill(x, y, x + width, y + 2, PANEL_BORDER);
        guiGraphics.fill(x, y + height - 2, x + width, y + height, PANEL_BORDER);
    }


}

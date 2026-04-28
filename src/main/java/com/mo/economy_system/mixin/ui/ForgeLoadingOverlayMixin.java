package com.mo.economy_system.mixin.ui;

import com.mo.economy_system.EconomySystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.neoforged.neoforge.client.loading.NeoForgeLoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * ForgeLoadingOverlay Mixin
 * Cancels Forge's custom loading overlay and falls back to vanilla LoadingOverlay
 * This allows our LoadingOverlayMixin to render the custom UI
 *
 * References: Drippy Loading Screen mod implementation
 */
@Mixin(NeoForgeLoadingOverlay.class)
public class ForgeLoadingOverlayMixin extends LoadingOverlay {

    public ForgeLoadingOverlayMixin(Minecraft mc, ReloadInstance reload, Consumer<Optional<Throwable>> errorConsumer, boolean b) {
        super(mc, reload, errorConsumer, b);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void economySystem$cancelForgeRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
//        EconomySystem.LOGGER.info("ForgeLoadingOverlayMixin: Canceling Forge render, falling back to LoadingOverlay");
        ci.cancel();
        // Call parent (LoadingOverlay) render instead of Forge's custom rendering
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}

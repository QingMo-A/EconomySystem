package com.mo.economy_system.client.render;

import com.mojang.authlib.GameProfile;
import com.mo.economy_system.item.items.PlayerDollHatItem;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class PlayerDollSkinResolver {
    private PlayerDollSkinResolver() {
    }

    public static ResolvedSkin resolveSkin(ItemStack stack, LivingEntity wearer) {
        if (stack.getItem() instanceof PlayerDollHatItem hatItem) {
            PlayerSkin.Model model = hatItem.isSlimModel(stack) ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE;
            return new ResolvedSkin(hatItem.getSkinPlayerName(stack), hatItem.getSkinTexture(stack), model);
        }

        if (wearer instanceof AbstractClientPlayer clientPlayer) {
            PlayerSkin wearerSkin = clientPlayer.getSkin();
            return new ResolvedSkin(clientPlayer.getGameProfile().getName(), wearerSkin.texture(), wearerSkin.model());
        }

        PlayerSkin defaultSkin = DefaultPlayerSkin.get(new GameProfile(new UUID(0L, 0L), "PlayerDoll"));
        return new ResolvedSkin("PlayerDoll", defaultSkin.texture(), defaultSkin.model());
    }

    public record ResolvedSkin(String playerName, ResourceLocation texture, PlayerSkin.Model model) {
    }
}

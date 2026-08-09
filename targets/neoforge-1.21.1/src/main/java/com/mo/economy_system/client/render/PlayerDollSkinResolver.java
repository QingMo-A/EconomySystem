package com.mo.economy_system.client.render;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mo.economy_system.item.items.PlayerDollHatItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PlayerDollSkinResolver {
    private static final ConcurrentMap<UUID, ResolvedSkin> SKIN_CACHE = new ConcurrentHashMap<>();
    private static final Set<UUID> LOADING_SKINS = ConcurrentHashMap.newKeySet();

    private PlayerDollSkinResolver() {
    }

    public static ResolvedSkin resolveSkin(ItemStack stack, LivingEntity wearer) {
        if (stack.getItem() instanceof PlayerDollHatItem hatItem) {
            UUID targetUuid = hatItem.getSkinPlayerUuid(stack);
            ResolvedSkin cached = SKIN_CACHE.get(targetUuid);
            if (cached != null) {
                return cached;
            }

            requestSkin(targetUuid, hatItem.getSkinPlayerName(stack));
            if (wearer instanceof AbstractClientPlayer clientPlayer) {
                PlayerSkin wearerSkin = clientPlayer.getSkin();
                return new ResolvedSkin(clientPlayer.getGameProfile().getName(), wearerSkin.texture(), wearerSkin.model());
            }

            PlayerSkin defaultSkin = DefaultPlayerSkin.get(new GameProfile(targetUuid, hatItem.getSkinPlayerName(stack)));
            return new ResolvedSkin(hatItem.getSkinPlayerName(stack), defaultSkin.texture(), defaultSkin.model());
        }

        if (wearer instanceof AbstractClientPlayer clientPlayer) {
            PlayerSkin wearerSkin = clientPlayer.getSkin();
            return new ResolvedSkin(clientPlayer.getGameProfile().getName(), wearerSkin.texture(), wearerSkin.model());
        }

        PlayerSkin defaultSkin = DefaultPlayerSkin.get(new GameProfile(new UUID(0L, 0L), "PlayerDoll"));
        return new ResolvedSkin("PlayerDoll", defaultSkin.texture(), defaultSkin.model());
    }

    private static void requestSkin(UUID uuid, String fallbackName) {
        if (uuid == null || !LOADING_SKINS.add(uuid)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        CompletableFuture.supplyAsync(() -> minecraft.getMinecraftSessionService().fetchProfile(uuid, true))
                .thenAccept(profileResult -> cacheProfileSkin(uuid, profileResult, fallbackName))
                .whenComplete((ignored, throwable) -> LOADING_SKINS.remove(uuid));
    }

    private static void cacheProfileSkin(UUID uuid, ProfileResult profileResult, String fallbackName) {
        if (profileResult == null || profileResult.profile() == null) {
            return;
        }

        GameProfile profile = profileResult.profile();
        String playerName = profile.getName() == null || profile.getName().isBlank() ? fallbackName : profile.getName();
        Minecraft.getInstance().getSkinManager()
                .getOrLoad(profile)
                .thenAccept(skin -> {
                    if (skin != null) {
                        SKIN_CACHE.put(uuid, new ResolvedSkin(playerName, skin.texture(), skin.model()));
                    }
                });
    }

    public record ResolvedSkin(String playerName, ResourceLocation texture, PlayerSkin.Model model) {
    }
}

package com.mo.economy_system.target.forge1201.client.render;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mo.economy_system.target.forge1201.item.Forge1201PlayerDollHatItem;
import com.mo.economy_system.target.forge1201.item.Forge1201SupporterHat;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Forge 1.20.1 skin lookup adapter used by the cosmetic renderers. */
final class Forge1201PlayerSkinResolver {
  private static final ConcurrentMap<UUID, ResolvedSkin> CACHE = new ConcurrentHashMap<>();
  private static final ConcurrentMap<UUID, Long> LOADING = new ConcurrentHashMap<>();
  private static final long LOAD_TIMEOUT_NANOS = 30_000_000_000L;

  private Forge1201PlayerSkinResolver() {}

  static ResolvedSkin resolveDoll(ItemStack stack, LivingEntity wearer) {
    if (!(stack.getItem() instanceof Forge1201PlayerDollHatItem doll)) {
      return resolve(new UUID(0L, 0L), "PlayerDoll", wearer);
    }
    return resolve(doll.getSkinPlayerUuid(stack), doll.getSkinPlayerName(stack), wearer,
        doll.isSlimModel(stack));
  }

  static ResolvedSkin resolveSupporter(ItemStack stack, Player wearer) {
    UUID uuid = Forge1201SupporterHat.getSupporterUuid(stack).orElse(wearer.getUUID());
    return resolve(uuid, wearer.getGameProfile().getName(), wearer);
  }

  private static ResolvedSkin resolve(
      UUID uuid, String fallbackName, LivingEntity wearer) {
    return resolve(uuid, fallbackName, wearer, false);
  }

  private static ResolvedSkin resolve(
      UUID uuid, String fallbackName, LivingEntity wearer, boolean fallbackSlim) {
    ResolvedSkin cached = CACHE.get(uuid);
    if (cached != null) return cached;

    if (wearer instanceof AbstractClientPlayer client && uuid.equals(client.getUUID())) {
      ResolvedSkin skin = new ResolvedSkin(
          client.getSkinTextureLocation(), "slim".equalsIgnoreCase(client.getModelName()));
      CACHE.putIfAbsent(uuid, skin);
      return skin;
    }

    Minecraft minecraft = Minecraft.getInstance();
    GameProfile profile = new GameProfile(uuid, fallbackName);
    ResourceLocation fallbackTexture = minecraft.getSkinManager().getInsecureSkinLocation(profile);
    boolean slim = fallbackSlim || "slim".equalsIgnoreCase(DefaultPlayerSkin.getSkinModelName(uuid));
    requestSkin(minecraft, profile, uuid, slim);
    return new ResolvedSkin(fallbackTexture, slim);
  }

  private static void requestSkin(
      Minecraft minecraft, GameProfile profile, UUID uuid, boolean fallbackSlim) {
    long requestToken = System.nanoTime();
    Long previousToken = LOADING.putIfAbsent(uuid, requestToken);
    if (previousToken != null) {
      if (requestToken - previousToken < LOAD_TIMEOUT_NANOS
          || !LOADING.replace(uuid, previousToken, requestToken)) {
        return;
      }
    }
    try {
      minecraft.getSkinManager().registerSkins(profile, (type, location, texture) -> {
        if (type == MinecraftProfileTexture.Type.SKIN && location != null) {
          String model = texture == null ? null : texture.getMetadata("model");
          CACHE.put(uuid, new ResolvedSkin(location,
              model == null ? fallbackSlim : "slim".equalsIgnoreCase(model)));
        }
        LOADING.remove(uuid, requestToken);
      }, true);
    } catch (RuntimeException ignored) {
      LOADING.remove(uuid, requestToken);
    }
  }

  record ResolvedSkin(ResourceLocation texture, boolean slim) {}
}

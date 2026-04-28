package com.mo.economy_system.mixin.world_wrap_system;

import com.mo.economy_system.core.world_wrap_system.WorldWrapChunkMirrorManager;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.ChunkPos;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkMap.class)
public abstract class ChunkMapWorldWrapMixin {
    @Shadow
    @Final
    public ServerLevel level;

    @Inject(method = "updateChunkTracking", at = @At("HEAD"), cancellable = true)
    private void economySystem$filterFiniteWorldChunks(ServerPlayer player, ChunkPos chunkPos,
                                                       MutableObject<ClientboundLevelChunkWithLightPacket> chunkPacket,
                                                       boolean wasTracked, boolean shouldTrack, CallbackInfo ci) {
        if (player.level() != level || !WorldWrapChunkMirrorManager.shouldBlockVanillaChunk(player, chunkPos)) {
            return;
        }

        if (!shouldTrack && wasTracked && !WorldWrapChunkMirrorManager.shouldProtectMirrorForget(player, chunkPos)) {
            return;
        }

        ci.cancel();
    }
}

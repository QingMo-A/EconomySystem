package com.mo.economy_system.mixin.world_wrap_system;

import com.mo.economy_system.core.world_wrap_system.WorldWrapChunkMirrorManager;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerWorldWrapChunkMixin {
    @Inject(method = "trackChunk", at = @At("HEAD"), cancellable = true)
    private void economySystem$blockFiniteWorldVanillaChunk(ChunkPos chunkPos, Packet<?> packet, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (WorldWrapChunkMirrorManager.shouldBlockVanillaChunk(player, chunkPos)) {
            ci.cancel();
        }
    }

    @Inject(method = "untrackChunk", at = @At("HEAD"), cancellable = true)
    private void economySystem$protectFiniteWorldMirrorChunk(ChunkPos chunkPos, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (WorldWrapChunkMirrorManager.shouldProtectMirrorForget(player, chunkPos)) {
            ci.cancel();
        }
    }
}

package com.mo.economy_system.mixin.world_wrap_system;

import com.mo.economy_system.core.world_wrap_system.WorldWrapEntityMirrorManager;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class ChunkMapTrackedEntityWorldWrapMixin {
    @Shadow
    private ServerEntity serverEntity;

    @Shadow
    private Entity entity;

    @Shadow
    private Set<ServerPlayerConnection> seenBy;

    @Shadow
    private int getEffectiveRange() {
        return 0;
    }

    @Inject(method = "updatePlayer", at = @At("HEAD"), cancellable = true)
    private void economySystem$updateWrappedPlayer(ServerPlayer player, CallbackInfo ci) {
        if (!WorldWrapEntityMirrorManager.canMirrorEntities(player)) {
            return;
        }

        ci.cancel();
        if (player == this.entity) {
            return;
        }

        double range = Math.min(this.getEffectiveRange(), player.server.getPlayerList().getViewDistance() * 16);
        boolean shouldTrack = WorldWrapEntityMirrorManager.shouldTrackWrappedEntity(player, this.entity, range);
        if (shouldTrack) {
            if (this.seenBy.add(player.connection)) {
                this.serverEntity.addPairing(player);
            }
        } else if (this.seenBy.remove(player.connection)) {
            this.serverEntity.removePairing(player);
        }
    }
}

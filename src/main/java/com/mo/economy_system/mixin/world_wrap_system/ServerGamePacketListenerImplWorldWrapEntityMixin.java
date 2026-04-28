package com.mo.economy_system.mixin.world_wrap_system;

import com.mo.economy_system.core.world_wrap_system.WorldWrapEntityPacketTransformer;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplWorldWrapEntityMixin {
    @Shadow
    public ServerPlayer player;

    @Shadow
    public Connection connection;

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V", at = @At("HEAD"), cancellable = true)
    private void economySystem$transformWrappedEntityPacket(Packet<?> packet, @Nullable PacketSendListener listener, CallbackInfo ci) {
        Packet<?> transformedPacket = WorldWrapEntityPacketTransformer.transform(packet, player);
        if (transformedPacket == packet) {
            return;
        }

        ci.cancel();
        this.connection.send(transformedPacket, listener);
    }
}

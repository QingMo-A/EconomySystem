package com.mo.economy_system.mixin.world_wrap_system;

import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundLevelChunkWithLightPacket.class)
public interface ClientboundLevelChunkWithLightPacketAccessor {
    @Mutable
    @Accessor("x")
    void economySystem$setX(int x);

    @Mutable
    @Accessor("z")
    void economySystem$setZ(int z);
}

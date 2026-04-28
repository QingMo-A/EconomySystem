package com.mo.economy_system.compat.network;

import com.mo.economy_system.compat.network.simple.SimpleChannel;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;

public final class NetworkRegistry {
    private NetworkRegistry() {
    }

    public static SimpleChannel newSimpleChannel(
            ResourceLocation name,
            Supplier<String> protocolVersion,
            Predicate<String> clientAcceptedVersions,
            Predicate<String> serverAcceptedVersions
    ) {
        return new SimpleChannel(name, protocolVersion.get());
    }
}

package net.neoforged.neoforge.network;

import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.simple.SimpleChannel;

public final class NetworkRegistry {
    private NetworkRegistry() {}

    public static SimpleChannel newSimpleChannel(
            ResourceLocation name,
            Supplier<String> protocolVersion,
            Predicate<String> clientAcceptedVersions,
            Predicate<String> serverAcceptedVersions
    ) {
        return new SimpleChannel(name, protocolVersion.get());
    }
}

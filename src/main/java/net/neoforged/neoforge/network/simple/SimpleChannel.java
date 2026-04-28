package net.neoforged.neoforge.network.simple;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.NetworkDirection;
import net.neoforged.neoforge.network.NetworkEvent;

public class SimpleChannel {
    private final ResourceLocation name;
    private final String protocolVersion;

    public SimpleChannel(ResourceLocation name, String protocolVersion) {
        this.name = name;
        this.protocolVersion = protocolVersion;
    }

    public <MSG> void registerMessage(
            int index,
            Class<MSG> messageType,
            BiConsumer<MSG, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, MSG> decoder,
            BiConsumer<MSG, java.util.function.Supplier<NetworkEvent.Context>> handler
    ) {
    }

    public <MSG> void registerMessage(
            int index,
            Class<MSG> messageType,
            BiConsumer<MSG, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, MSG> decoder,
            BiConsumer<MSG, java.util.function.Supplier<NetworkEvent.Context>> handler,
            Optional<NetworkDirection> direction
    ) {
    }

    public void send(Object target, Object packet) {
    }

    public void sendTo(Object packet, Object connection, NetworkDirection direction) {
    }

    public void sendToServer(Object packet) {
        send(null, packet);
    }

    @Override
    public String toString() {
        return "SimpleChannel[" + name + ", " + protocolVersion + "]";
    }
}

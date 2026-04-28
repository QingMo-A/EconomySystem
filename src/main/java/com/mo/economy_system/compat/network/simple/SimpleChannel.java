package com.mo.economy_system.compat.network.simple;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.compat.network.NetworkDirection;
import com.mo.economy_system.compat.network.NetworkEvent;
import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

public class SimpleChannel {
    private final ResourceLocation name;
    private final String protocolVersion;
    private final CustomPacketPayload.Type<LegacyPayload> payloadType;
    private final Map<Integer, MessageRegistration<?>> registrationsById = new HashMap<>();
    private final Map<Class<?>, MessageRegistration<?>> registrationsByClass = new HashMap<>();

    public SimpleChannel(ResourceLocation name, String protocolVersion) {
        this.name = name;
        this.protocolVersion = protocolVersion;
        this.payloadType = new CustomPacketPayload.Type<>(name);
    }

    public <MSG> void registerMessage(
            int index,
            Class<MSG> messageType,
            BiConsumer<MSG, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, MSG> decoder,
            BiConsumer<MSG, java.util.function.Supplier<NetworkEvent.Context>> handler
    ) {
        registerMessage(index, messageType, encoder, decoder, handler, Optional.empty());
    }

    public <MSG> void registerMessage(
            int index,
            Class<MSG> messageType,
            BiConsumer<MSG, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, MSG> decoder,
            BiConsumer<MSG, java.util.function.Supplier<NetworkEvent.Context>> handler,
            Optional<NetworkDirection> direction
    ) {
        MessageRegistration<MSG> registration = new MessageRegistration<>(index, messageType, encoder, decoder, handler, direction);
        registrationsById.put(index, registration);
        registrationsByClass.put(messageType, registration);
    }

    public void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(protocolVersion);
        registrar.playBidirectional(payloadType, legacyPayloadCodec(), this::handlePayload);
    }

    public void send(Object target, Object packet) {
        LegacyPayload payload = encodePayload(packet);
        if (target instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, payload);
        } else {
            PacketDistributor.sendToServer(payload);
        }
    }

    public void sendTo(Object packet, Object connection, NetworkDirection direction) {
        if (direction == NetworkDirection.PLAY_TO_SERVER) {
            sendToServer(packet);
        } else {
            send(connection, packet);
        }
    }

    public void sendToServer(Object packet) {
        send(null, packet);
    }

    private LegacyPayload encodePayload(Object packet) {
        MessageRegistration<?> registration = registrationsByClass.get(packet.getClass());
        if (registration == null) {
            throw new IllegalArgumentException("Unregistered packet type: " + packet.getClass().getName());
        }

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        encodeUnchecked(registration, packet, buffer);
        byte[] data = new byte[buffer.readableBytes()];
        buffer.readBytes(data);
        buffer.release();
        return new LegacyPayload(payloadType, registration.index, data);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void encodeUnchecked(MessageRegistration registration, Object packet, FriendlyByteBuf buffer) {
        registration.encoder().accept(packet, buffer);
    }

    private void handlePayload(LegacyPayload payload, IPayloadContext payloadContext) {
        MessageRegistration<?> registration = registrationsById.get(payload.packetId);
        if (registration == null) {
            EconomySystem.LOGGER.warn("Received unknown packet id {} on channel {}", payload.packetId, name);
            return;
        }

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.data));
        Object message = registration.decoder().apply(buffer);
        buffer.release();

        ServerPlayer sender = payloadContext.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        handleUnchecked(registration, message, sender);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void handleUnchecked(MessageRegistration registration, Object message, @Nullable ServerPlayer sender) {
        java.util.function.Supplier<NetworkEvent.Context> contextSupplier = () -> new NetworkEvent.Context(sender);
        registration.handler().accept(message, contextSupplier);
    }

    private StreamCodec<RegistryFriendlyByteBuf, LegacyPayload> legacyPayloadCodec() {
        return StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeVarInt(payload.packetId);
                    buffer.writeVarInt(payload.data.length);
                    buffer.writeBytes(payload.data);
                },
                buffer -> {
                    int packetId = buffer.readVarInt();
                    int length = buffer.readVarInt();
                    byte[] data = new byte[length];
                    buffer.readBytes(data);
                    return new LegacyPayload(payloadType, packetId, data);
                }
        );
    }

    @Override
    public String toString() {
        return "SimpleChannel[" + name + ", " + protocolVersion + "]";
    }

    private record MessageRegistration<MSG>(
            int index,
            Class<MSG> messageType,
            BiConsumer<MSG, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, MSG> decoder,
            BiConsumer<MSG, java.util.function.Supplier<NetworkEvent.Context>> handler,
            Optional<NetworkDirection> direction
    ) {
    }

    private static class LegacyPayload implements CustomPacketPayload {
        private final Type<LegacyPayload> type;
        private final int packetId;
        private final byte[] data;

        private LegacyPayload(Type<LegacyPayload> type, int packetId, byte[] data) {
            this.type = type;
            this.packetId = packetId;
            this.data = data;
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return type;
        }
    }
}

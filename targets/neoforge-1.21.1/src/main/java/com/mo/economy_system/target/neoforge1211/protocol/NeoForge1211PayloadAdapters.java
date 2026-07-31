package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import com.mo.economy_system.protocol.EconomyMessageType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/** Wraps common messages in NeoForge 1.21.1 custom payloads. */
public final class NeoForge1211PayloadAdapters {
    private NeoForge1211PayloadAdapters() {
    }

    public record Payload<T extends EconomyNetworkMessage>(
            Type<Payload<T>> payloadType,
            EconomyMessageType<T> messageType,
            T message
    ) implements CustomPacketPayload {
        public Payload {
            Objects.requireNonNull(payloadType, "payloadType");
            Objects.requireNonNull(messageType, "messageType");
            Objects.requireNonNull(message, "message");
            if (!messageType.messageClass().isInstance(message)) {
                throw new IllegalArgumentException(
                        "Message " + message.getClass().getName()
                                + " does not match type " + messageType.messageClass().getName()
                );
            }
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return payloadType;
        }

        private void encode(RegistryFriendlyByteBuf buffer) {
            NeoForge1211MessageCodecs.codec(messageType).encode(message, buffer);
        }
    }

    public static <T extends EconomyNetworkMessage> CustomPacketPayload.Type<Payload<T>> payloadType(
            EconomyMessageType<T> messageType
    ) {
        return new CustomPacketPayload.Type<>(ResourceLocation.parse(messageType.id()));
    }

    public static <T extends EconomyNetworkMessage> Payload<T> payload(
            EconomyMessageType<T> messageType,
            T message
    ) {
        return new Payload<>(payloadType(messageType), messageType, message);
    }

    public static <T extends EconomyNetworkMessage> StreamCodec<RegistryFriendlyByteBuf, Payload<T>> codec(
            EconomyMessageType<T> messageType
    ) {
        CustomPacketPayload.Type<Payload<T>> payloadType = payloadType(messageType);
        NeoForge1211MessageCodec<T> messageCodec = NeoForge1211MessageCodecs.codec(messageType);
        return StreamCodec.ofMember(
                Payload::encode,
                buffer -> new Payload<>(payloadType, messageType, messageCodec.decode(buffer))
        );
    }
}

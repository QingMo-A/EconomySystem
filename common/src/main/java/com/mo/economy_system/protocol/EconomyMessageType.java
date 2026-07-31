package com.mo.economy_system.protocol;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;

import java.util.Objects;

/**
 * Loader-neutral metadata for one EconomySystem network message.
 *
 * <p><strong>Protocol stability:</strong> a published discriminator is part of
 * the wire protocol. Existing values must never be renumbered or reused; new
 * message types may only be appended with new discriminator values.</p>
 */
public record EconomyMessageType<T extends EconomyNetworkMessage>(
        int discriminator,
        String id,
        EconomyMessageDirection direction,
        Class<T> messageClass
) {
    public EconomyMessageType(EconomyMessageSpec spec, Class<T> messageClass) {
        this(spec.discriminator(), spec.id(), spec.direction(), messageClass);
    }

    public EconomyMessageType {
        if (discriminator < 0 || discriminator > 255) {
            throw new IllegalArgumentException(
                    "Message discriminator must fit the Forge 1.20.1 byte range: "
                            + discriminator
            );
        }

        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Message id must not be blank");
        }
        if (!id.equals(id.trim())) {
            throw new IllegalArgumentException("Message id must not have leading or trailing whitespace");
        }

        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(messageClass, "messageClass");
        if (!EconomyNetworkMessage.class.isAssignableFrom(messageClass)) {
            throw new IllegalArgumentException(
                    "Message class must implement EconomyNetworkMessage: " + messageClass.getName()
            );
        }
    }

    public EconomyMessageSpec spec() {
        return new EconomyMessageSpec(discriminator, id, direction);
    }
}

package com.mo.economy_system.protocol;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Mutable-at-bootstrap registry of loader-neutral EconomySystem message types.
 *
 * <p>Call {@link #freeze()} after bootstrap. A frozen registry rejects every
 * subsequent registration while retaining read-only lookup access.</p>
 */
public final class EconomyMessageRegistry {
    private final Map<Integer, EconomyMessageType<?>> byDiscriminator = new HashMap<>();
    private final Map<String, EconomyMessageType<?>> byId = new HashMap<>();
    private final Map<Class<? extends EconomyNetworkMessage>, EconomyMessageType<?>> byClass =
            new HashMap<>();
    private final List<EconomyMessageType<?>> registeredTypes = new ArrayList<>();

    private boolean frozen;

    /**
     * Registers one message type after validating all three stable identities.
     * No identity may be registered more than once, including by the same type.
     */
    public synchronized <T extends EconomyNetworkMessage> EconomyMessageType<T> register(
            EconomyMessageType<T> type
    ) {
        Objects.requireNonNull(type, "type");
        ensureMutable();

        EconomyMessageType<?> discriminatorConflict = byDiscriminator.get(type.discriminator());
        if (discriminatorConflict != null) {
            throw duplicate("discriminator", type.discriminator(), discriminatorConflict);
        }

        EconomyMessageType<?> idConflict = byId.get(type.id());
        if (idConflict != null) {
            throw duplicate("id", type.id(), idConflict);
        }

        EconomyMessageType<?> classConflict = byClass.get(type.messageClass());
        if (classConflict != null) {
            throw duplicate("message class", type.messageClass().getName(), classConflict);
        }

        byDiscriminator.put(type.discriminator(), type);
        byId.put(type.id(), type);
        byClass.put(type.messageClass(), type);
        registeredTypes.add(type);
        return type;
    }

    /** Convenience overload that constructs and registers the message metadata. */
    public synchronized <T extends EconomyNetworkMessage> EconomyMessageType<T> register(
            int discriminator,
            String id,
            EconomyMessageDirection direction,
            Class<T> messageClass
    ) {
        return register(new EconomyMessageType<>(discriminator, id, direction, messageClass));
    }

    /** Prevents all future registrations. This operation is idempotent. */
    public synchronized void freeze() {
        frozen = true;
    }

    public synchronized boolean isFrozen() {
        return frozen;
    }

    /** Returns the type for a discriminator, or {@code null} when it is unknown. */
    public synchronized EconomyMessageType<?> typeOf(int discriminator) {
        return byDiscriminator.get(discriminator);
    }

    /** Returns the type for an ID, or {@code null} when it is unknown. */
    public synchronized EconomyMessageType<?> typeOf(String id) {
        Objects.requireNonNull(id, "id");
        return byId.get(id);
    }

    /** Returns the type for a message class, or {@code null} when it is unknown. */
    @SuppressWarnings("unchecked")
    public synchronized <T extends EconomyNetworkMessage> EconomyMessageType<T> typeOf(
            Class<T> messageClass
    ) {
        Objects.requireNonNull(messageClass, "messageClass");
        return (EconomyMessageType<T>) byClass.get(messageClass);
    }

    /** Returns an immutable snapshot in registration order. */
    public synchronized List<EconomyMessageType<?>> values() {
        return List.copyOf(registeredTypes);
    }

    public synchronized int size() {
        return registeredTypes.size();
    }

    private void ensureMutable() {
        if (frozen) {
            throw new IllegalStateException("Economy message registry is frozen");
        }
    }

    private static IllegalArgumentException duplicate(
            String identity,
            Object value,
            EconomyMessageType<?> existing
    ) {
        return new IllegalArgumentException(
                "Duplicate message " + identity + " '" + value + "'; already registered by " + existing.id()
        );
    }
}

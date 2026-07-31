package com.mo.economy_system.common.client;

import com.mo.economy_system.common.network.ShopDataResponseMessage;
import com.mo.economy_system.common.network.ShopItemSnapshot;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** Atomically published, immutable client view of the system-shop catalog. */
public final class ClientShopState {
    private static final AtomicReference<Snapshot> CURRENT =
            new AtomicReference<>(new Snapshot(List.of()));

    private ClientShopState() {
    }

    public static Snapshot snapshot() {
        return CURRENT.get();
    }

    public static void update(ShopDataResponseMessage message) {
        Objects.requireNonNull(message, "message");
        CURRENT.set(new Snapshot(message.items()));
    }

    public record Snapshot(List<ShopItemSnapshot> items) {
        public Snapshot {
            items = List.copyOf(Objects.requireNonNull(items, "items"));
        }
    }
}

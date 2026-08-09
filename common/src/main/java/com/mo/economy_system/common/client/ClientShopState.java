package com.mo.economy_system.common.client;

import com.mo.economy_system.common.network.ShopDataResponseMessage;
import com.mo.economy_system.common.network.ShopItemSnapshot;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** Atomically published, immutable client view of the system-shop catalog. */
public final class ClientShopState {
    private static final AtomicReference<Snapshot> CURRENT =
            new AtomicReference<>(new Snapshot(0, List.of()));

    private ClientShopState() {
    }

    public static Snapshot snapshot() {
        return CURRENT.get();
    }

    public static void update(ShopDataResponseMessage message) {
        Objects.requireNonNull(message, "message");
        CURRENT.updateAndGet(previous -> new Snapshot(nextRevision(previous.revision()), message.items()));
    }

    private static long nextRevision(long revision) {
        if (revision == Long.MAX_VALUE) throw new IllegalStateException("shop revision exhausted");
        return revision + 1;
    }

    public record Snapshot(long revision, List<ShopItemSnapshot> items) {
        public Snapshot(List<ShopItemSnapshot> items) {
            this(0, items);
        }
        public Snapshot {
            if (revision < 0) throw new IllegalArgumentException("revision");
            items = List.copyOf(Objects.requireNonNull(items, "items"));
        }
    }
}

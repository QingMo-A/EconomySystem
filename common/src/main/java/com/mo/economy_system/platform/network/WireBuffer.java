package com.mo.economy_system.platform.network;

import com.mo.economy_system.platform.nbt.NbtData;
import java.util.UUID;

/**
 * Minimal wire-format contract used by shared packet codecs.
 *
 * <p>Targets retain ownership of the actual byte buffer, NBT binary codec and
 * reference counting. Shared codecs only describe protocol fields and bounds.</p>
 */
public interface WireBuffer extends AutoCloseable {
    void writeBoolean(boolean value);

    boolean readBoolean();

    void writeInt(int value);

    int readInt();

    void writeLong(long value);

    long readLong();

    void writeUuid(UUID value);

    UUID readUuid();

    void writeUtf(String value, int maximumLength);

    String readUtf(int maximumLength);

    void writeNbt(NbtData.Compound value);

    /** Returns {@code null} when the target wire representation contains a null NBT value. */
    NbtData.Compound readNbt();

    int readableBytes();

    boolean isReadable();

    /** Creates an owned temporary buffer for atomic encoding. */
    WireBuffer temporary();

    /** Appends all currently readable bytes from a temporary buffer. */
    void writeRemaining(WireBuffer source);

    @Override
    default void close() {
        // Destination wrappers do not own their target buffer by default.
    }
}

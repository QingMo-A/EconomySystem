package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.platform.nbt.NbtData;
import com.mo.economy_system.platform.network.WireBuffer;
import com.mo.economy_system.target.forge1201.item.Forge1201NbtAdapter;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/** Forge 1.20.1 implementation of the common packet-buffer contract. */
public final class Forge1201WireBuffer implements WireBuffer {
    private final FriendlyByteBuf delegate;
    private final boolean ownsDelegate;

    private Forge1201WireBuffer(FriendlyByteBuf delegate, boolean ownsDelegate) {
        this.delegate = delegate;
        this.ownsDelegate = ownsDelegate;
    }

    public static Forge1201WireBuffer wrap(FriendlyByteBuf delegate) {
        return new Forge1201WireBuffer(delegate, false);
    }

    @Override public void writeBoolean(boolean value) { delegate.writeBoolean(value); }
    @Override public boolean readBoolean() { return delegate.readBoolean(); }
    @Override public void writeInt(int value) { delegate.writeInt(value); }
    @Override public int readInt() { return delegate.readInt(); }
    @Override public void writeLong(long value) { delegate.writeLong(value); }
    @Override public long readLong() { return delegate.readLong(); }
    @Override public void writeUuid(UUID value) { delegate.writeUUID(value); }
    @Override public UUID readUuid() { return delegate.readUUID(); }
    @Override public void writeUtf(String value, int maximumLength) { delegate.writeUtf(value, maximumLength); }
    @Override public String readUtf(int maximumLength) { return delegate.readUtf(maximumLength); }
    @Override public void writeNbt(NbtData.Compound value) { delegate.writeNbt(Forge1201NbtAdapter.toNative(value)); }
    @Override public NbtData.Compound readNbt() {
        CompoundTag value = delegate.readNbt();
        return value == null ? null : Forge1201NbtAdapter.fromNative(value);
    }
    @Override public int readableBytes() { return delegate.readableBytes(); }
    @Override public boolean isReadable() { return delegate.isReadable(); }
    @Override public WireBuffer temporary() { return new Forge1201WireBuffer(new FriendlyByteBuf(Unpooled.buffer()), true); }

    @Override
    public void writeRemaining(WireBuffer source) {
        if (!(source instanceof Forge1201WireBuffer nativeSource)) {
            throw new IllegalArgumentException("cannot mix packet-buffer targets");
        }
        delegate.writeBytes(nativeSource.delegate, nativeSource.delegate.readerIndex(), nativeSource.delegate.readableBytes());
    }

    @Override
    public void close() {
        if (ownsDelegate) delegate.release();
    }
}

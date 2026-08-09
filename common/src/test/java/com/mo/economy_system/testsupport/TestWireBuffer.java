package com.mo.economy_system.testsupport;

import com.mo.economy_system.platform.nbt.NbtData;
import com.mo.economy_system.platform.network.WireBuffer;
import com.mo.economy_system.platform.network.WireDecodeException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Pure-Java packet buffer used by common codec tests. */
public final class TestWireBuffer implements WireBuffer {
    private static final int DEFAULT_MAX_UTF_LENGTH = 32_767;

    private final ByteArrayOutputStream output;
    private int readerIndex;

    public TestWireBuffer() {
        this(new byte[0]);
    }

    private TestWireBuffer(byte[] initialBytes) {
        output = new ByteArrayOutputStream(initialBytes.length);
        output.writeBytes(initialBytes);
    }

    public static TestWireBuffer of(byte[] bytes) {
        return new TestWireBuffer(bytes.clone());
    }

    public byte[] bytes() {
        return output.toByteArray();
    }

    public byte[] array() {
        return bytes();
    }

    public TestWireBuffer copy() {
        return of(Arrays.copyOfRange(bytes(), readerIndex, writerIndex()));
    }

    public int writerIndex() {
        return output.size();
    }

    public int readerIndex() {
        return readerIndex;
    }

    public void getBytes(int index, byte[] destination) {
        byte[] source = bytes();
        if (index < 0 || destination.length > source.length - index) {
            throw new IndexOutOfBoundsException("buffer range");
        }
        System.arraycopy(source, index, destination, 0, destination.length);
    }

    public int getInt(int index) {
        byte[] source = bytes();
        if (index < 0 || index > source.length - Integer.BYTES) {
            throw new IndexOutOfBoundsException("buffer range");
        }
        return ByteBuffer.wrap(source, index, Integer.BYTES).getInt();
    }

    public void writeByte(int value) {
        output.write(value);
    }

    public void writeZero(int count) {
        if (count < 0) throw new IllegalArgumentException("negative zero count");
        output.writeBytes(new byte[count]);
    }

    public int readUnsignedByte() {
        return readRawByte();
    }

    public void writeUUID(UUID value) {
        writeUuid(value);
    }

    public UUID readUUID() {
        return readUuid();
    }

    public void writeUtf(String value) {
        writeUtf(value, DEFAULT_MAX_UTF_LENGTH);
    }

    public boolean release() {
        return false;
    }

    @Override
    public void writeBoolean(boolean value) {
        writeByte(value ? 1 : 0);
    }

    @Override
    public boolean readBoolean() {
        return readRawByte() != 0;
    }

    @Override
    public void writeInt(int value) {
        writeData(output -> output.writeInt(value));
    }

    @Override
    public int readInt() {
        return readData(DataInputStream::readInt);
    }

    @Override
    public void writeLong(long value) {
        writeData(output -> output.writeLong(value));
    }

    @Override
    public long readLong() {
        return readData(DataInputStream::readLong);
    }

    @Override
    public void writeUuid(UUID value) {
        if (value == null) throw new NullPointerException("value");
        writeLong(value.getMostSignificantBits());
        writeLong(value.getLeastSignificantBits());
    }

    @Override
    public UUID readUuid() {
        return new UUID(readLong(), readLong());
    }

    @Override
    public void writeUtf(String value, int maximumLength) {
        if (value == null) throw new NullPointerException("value");
        if (maximumLength < 0 || value.length() > maximumLength) {
            throw new IllegalArgumentException("UTF value exceeds character limit");
        }
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        if (encoded.length > maximumLength * 3L) {
            throw new IllegalArgumentException("UTF value exceeds byte limit");
        }
        writeVarInt(encoded.length);
        output.writeBytes(encoded);
    }

    @Override
    public String readUtf(int maximumLength) {
        int byteLength = readVarInt();
        if (byteLength < 0 || byteLength > maximumLength * 3L) {
            throw new WireDecodeException("UTF byte length exceeds limit");
        }
        byte[] encoded = readBytes(byteLength);
        try {
            String value = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(encoded))
                    .toString();
            if (value.length() > maximumLength) {
                throw new WireDecodeException("UTF character length exceeds limit");
            }
            return value;
        } catch (CharacterCodingException exception) {
            throw new WireDecodeException("malformed UTF value", exception);
        }
    }

    @Override
    public void writeNbt(NbtData.Compound value) {
        writeData(output -> {
            if (value == null) {
                output.writeByte(0);
                return;
            }
            output.writeByte(10);
            output.writeUTF("");
            writeTagPayload(output, value);
        });
    }

    @Override
    public NbtData.Compound readNbt() {
        return readData(input -> {
            int type = input.readUnsignedByte();
            if (type == 0) return null;
            if (type != 10) throw new IOException("NBT root is not a compound");
            input.readUTF();
            return (NbtData.Compound) readTagPayload(input, type);
        });
    }

    @Override
    public int readableBytes() {
        return writerIndex() - readerIndex;
    }

    @Override
    public boolean isReadable() {
        return readableBytes() > 0;
    }

    @Override
    public WireBuffer temporary() {
        return new TestWireBuffer();
    }

    @Override
    public void writeRemaining(WireBuffer source) {
        if (!(source instanceof TestWireBuffer testSource)) {
            throw new IllegalArgumentException("cannot mix packet-buffer implementations");
        }
        byte[] sourceBytes = testSource.bytes();
        output.write(sourceBytes, testSource.readerIndex, testSource.readableBytes());
    }

    private void writeVarInt(int value) {
        int remaining = value;
        while ((remaining & ~0x7f) != 0) {
            writeByte((remaining & 0x7f) | 0x80);
            remaining >>>= 7;
        }
        writeByte(remaining);
    }

    private int readVarInt() {
        int result = 0;
        for (int byteIndex = 0; byteIndex < 5; byteIndex++) {
            int current = readRawByte();
            result |= (current & 0x7f) << (byteIndex * 7);
            if ((current & 0x80) == 0) return result;
        }
        throw new WireDecodeException("VarInt is too large");
    }

    private int readRawByte() {
        if (!isReadable()) throw new WireDecodeException("truncated packet");
        return bytes()[readerIndex++] & 0xff;
    }

    private byte[] readBytes(int length) {
        if (length < 0 || length > readableBytes()) throw new WireDecodeException("truncated packet");
        byte[] value = Arrays.copyOfRange(bytes(), readerIndex, readerIndex + length);
        readerIndex += length;
        return value;
    }

    private void writeData(DataWriter writer) {
        try {
            writer.write(new DataOutputStream(output));
        } catch (IOException exception) {
            throw new IllegalStateException("in-memory buffer write failed", exception);
        }
    }

    private <T> T readData(DataReader<T> reader) {
        ByteArrayInputStream bytes = new ByteArrayInputStream(bytes(), readerIndex, readableBytes());
        int availableBefore = bytes.available();
        try {
            T value = reader.read(new DataInputStream(bytes));
            readerIndex += availableBefore - bytes.available();
            return value;
        } catch (EOFException exception) {
            readerIndex += availableBefore - bytes.available();
            throw new WireDecodeException("truncated packet", exception);
        } catch (IOException | RuntimeException exception) {
            readerIndex += availableBefore - bytes.available();
            if (exception instanceof WireDecodeException decodeException) throw decodeException;
            throw new WireDecodeException("malformed packet", exception);
        }
    }

    private static void writeTagPayload(DataOutputStream output, NbtData value) throws IOException {
        if (value instanceof NbtData.ByteValue byteValue) output.writeByte(byteValue.value());
        else if (value instanceof NbtData.ShortValue shortValue) output.writeShort(shortValue.value());
        else if (value instanceof NbtData.IntValue intValue) output.writeInt(intValue.value());
        else if (value instanceof NbtData.LongValue longValue) output.writeLong(longValue.value());
        else if (value instanceof NbtData.FloatValue floatValue) output.writeFloat(floatValue.value());
        else if (value instanceof NbtData.DoubleValue doubleValue) output.writeDouble(doubleValue.value());
        else if (value instanceof NbtData.ByteArrayValue array) {
            byte[] values = array.value();
            output.writeInt(values.length);
            output.write(values);
        } else if (value instanceof NbtData.StringValue string) output.writeUTF(string.value());
        else if (value instanceof NbtData.ListValue list) {
            List<NbtData> values = list.values();
            output.writeByte(values.isEmpty() ? 0 : tagType(values.get(0)));
            output.writeInt(values.size());
            for (NbtData entry : values) writeTagPayload(output, entry);
        } else if (value instanceof NbtData.Compound compound) {
            for (Map.Entry<String, NbtData> entry : compound.values().entrySet()) {
                output.writeByte(tagType(entry.getValue()));
                output.writeUTF(entry.getKey());
                writeTagPayload(output, entry.getValue());
            }
            output.writeByte(0);
        } else if (value instanceof NbtData.IntArrayValue array) {
            int[] values = array.value();
            output.writeInt(values.length);
            for (int entry : values) output.writeInt(entry);
        } else if (value instanceof NbtData.LongArrayValue array) {
            long[] values = array.value();
            output.writeInt(values.length);
            for (long entry : values) output.writeLong(entry);
        } else throw new IOException("unsupported NBT value " + value.getClass().getName());
    }

    private static NbtData readTagPayload(DataInputStream input, int type) throws IOException {
        return switch (type) {
            case 1 -> NbtData.byteValue(input.readByte());
            case 2 -> NbtData.shortValue(input.readShort());
            case 3 -> NbtData.intValue(input.readInt());
            case 4 -> NbtData.longValue(input.readLong());
            case 5 -> NbtData.floatValue(input.readFloat());
            case 6 -> NbtData.doubleValue(input.readDouble());
            case 7 -> NbtData.byteArray(readByteArray(input));
            case 8 -> NbtData.string(input.readUTF());
            case 9 -> readList(input);
            case 10 -> readCompound(input);
            case 11 -> NbtData.intArray(readIntArray(input));
            case 12 -> NbtData.longArray(readLongArray(input));
            default -> throw new IOException("unknown NBT tag type " + type);
        };
    }

    private static NbtData.ListValue readList(DataInputStream input) throws IOException {
        int elementType = input.readUnsignedByte();
        int length = input.readInt();
        if (length < 0 || (length > 0 && elementType == 0)) throw new IOException("invalid NBT list");
        List<NbtData> values = new ArrayList<>(length);
        for (int index = 0; index < length; index++) values.add(readTagPayload(input, elementType));
        return NbtData.list(values);
    }

    private static NbtData.Compound readCompound(DataInputStream input) throws IOException {
        Map<String, NbtData> values = new LinkedHashMap<>();
        while (true) {
            int type = input.readUnsignedByte();
            if (type == 0) return NbtData.compound(values);
            String key = input.readUTF();
            if (values.put(key, readTagPayload(input, type)) != null) {
                throw new IOException("duplicate NBT key " + key);
            }
        }
    }

    private static byte[] readByteArray(DataInputStream input) throws IOException {
        int length = checkedArrayLength(input.readInt(), 1);
        byte[] values = new byte[length];
        input.readFully(values);
        return values;
    }

    private static int[] readIntArray(DataInputStream input) throws IOException {
        int length = checkedArrayLength(input.readInt(), Integer.BYTES);
        int[] values = new int[length];
        for (int index = 0; index < length; index++) values[index] = input.readInt();
        return values;
    }

    private static long[] readLongArray(DataInputStream input) throws IOException {
        int length = checkedArrayLength(input.readInt(), Long.BYTES);
        long[] values = new long[length];
        for (int index = 0; index < length; index++) values[index] = input.readLong();
        return values;
    }

    private static int checkedArrayLength(int length, int elementBytes) throws IOException {
        if (length < 0 || (long) length * elementBytes > Integer.MAX_VALUE) {
            throw new IOException("invalid NBT array length");
        }
        return length;
    }

    private static int tagType(NbtData value) throws IOException {
        if (value instanceof NbtData.ByteValue) return 1;
        if (value instanceof NbtData.ShortValue) return 2;
        if (value instanceof NbtData.IntValue) return 3;
        if (value instanceof NbtData.LongValue) return 4;
        if (value instanceof NbtData.FloatValue) return 5;
        if (value instanceof NbtData.DoubleValue) return 6;
        if (value instanceof NbtData.ByteArrayValue) return 7;
        if (value instanceof NbtData.StringValue) return 8;
        if (value instanceof NbtData.ListValue) return 9;
        if (value instanceof NbtData.Compound) return 10;
        if (value instanceof NbtData.IntArrayValue) return 11;
        if (value instanceof NbtData.LongArrayValue) return 12;
        throw new IOException("unsupported NBT value " + value.getClass().getName());
    }

    @FunctionalInterface
    private interface DataWriter {
        void write(DataOutputStream output) throws IOException;
    }

    @FunctionalInterface
    private interface DataReader<T> {
        T read(DataInputStream input) throws IOException;
    }
}

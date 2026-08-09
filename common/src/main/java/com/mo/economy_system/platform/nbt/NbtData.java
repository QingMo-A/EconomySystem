package com.mo.economy_system.platform.nbt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable, loader-neutral NBT tree.
 *
 * <p>The target layer converts this representation to its native NBT API.
 * Keeping the complete tag vocabulary here prevents snapshot persistence from
 * silently dropping target-specific custom data.</p>
 */
public sealed interface NbtData permits NbtData.ByteValue, NbtData.ShortValue,
        NbtData.IntValue, NbtData.LongValue, NbtData.FloatValue, NbtData.DoubleValue,
        NbtData.ByteArrayValue, NbtData.StringValue, NbtData.ListValue,
        NbtData.Compound, NbtData.IntArrayValue, NbtData.LongArrayValue {

    static Compound emptyCompound() {
        return new Compound(Map.of());
    }

    static Compound compound(Map<String, ? extends NbtData> values) {
        return new Compound(values);
    }

    static CompoundBuilder compoundBuilder() {
        return new CompoundBuilder();
    }

    static ByteValue byteValue(byte value) {
        return new ByteValue(value);
    }

    static ShortValue shortValue(short value) {
        return new ShortValue(value);
    }

    static IntValue intValue(int value) {
        return new IntValue(value);
    }

    static LongValue longValue(long value) {
        return new LongValue(value);
    }

    static FloatValue floatValue(float value) {
        return new FloatValue(value);
    }

    static DoubleValue doubleValue(double value) {
        return new DoubleValue(value);
    }

    static StringValue string(String value) {
        return new StringValue(value);
    }

    static ListValue list(List<? extends NbtData> values) {
        return new ListValue(values);
    }

    static ByteArrayValue byteArray(byte[] values) {
        return new ByteArrayValue(values);
    }

    static IntArrayValue intArray(int[] values) {
        return new IntArrayValue(values);
    }

    static LongArrayValue longArray(long[] values) {
        return new LongArrayValue(values);
    }

    static IntArrayValue uuid(UUID value) {
        Objects.requireNonNull(value, "value");
        long most = value.getMostSignificantBits();
        long least = value.getLeastSignificantBits();
        return intArray(new int[] {(int) (most >>> 32), (int) most, (int) (least >>> 32), (int) least});
    }

    static UUID readUuid(NbtData value) {
        if (!(value instanceof IntArrayValue array) || array.length() != 4) {
            throw new IllegalArgumentException("value is not a UUID int array");
        }
        int[] parts = array.value();
        long most = ((long) parts[0] << 32) | (parts[1] & 0xffffffffL);
        long least = ((long) parts[2] << 32) | (parts[3] & 0xffffffffL);
        return new UUID(most, least);
    }

    final class ByteValue implements NbtData {
        private final byte value;

        public ByteValue(byte value) {
            this.value = value;
        }

        public byte value() {
            return value;
        }

        @Override
        public boolean equals(Object value) {
            return value instanceof ByteValue other && this.value == other.value;
        }

        @Override
        public int hashCode() {
            return Byte.hashCode(value);
        }
    }

    final class ShortValue implements NbtData {
        private final short value;

        public ShortValue(short value) {
            this.value = value;
        }

        public short value() {
            return value;
        }

        @Override
        public boolean equals(Object value) {
            return value instanceof ShortValue other && this.value == other.value;
        }

        @Override
        public int hashCode() {
            return Short.hashCode(value);
        }
    }

    final class IntValue implements NbtData {
        private final int value;

        public IntValue(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        @Override
        public boolean equals(Object value) {
            return value instanceof IntValue other && this.value == other.value;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(value);
        }
    }

    final class LongValue implements NbtData {
        private final long value;

        public LongValue(long value) {
            this.value = value;
        }

        public long value() {
            return value;
        }

        @Override
        public boolean equals(Object value) {
            return value instanceof LongValue other && this.value == other.value;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(value);
        }
    }

    final class FloatValue implements NbtData {
        private final float value;

        public FloatValue(float value) {
            this.value = value;
        }

        public float value() {
            return value;
        }

        @Override
        public boolean equals(Object value) {
            return value instanceof FloatValue other && Float.compare(this.value, other.value) == 0;
        }

        @Override
        public int hashCode() {
            return Float.hashCode(value);
        }
    }

    final class DoubleValue implements NbtData {
        private final double value;

        public DoubleValue(double value) {
            this.value = value;
        }

        public double value() {
            return value;
        }

        @Override
        public boolean equals(Object value) {
            return value instanceof DoubleValue other && Double.compare(this.value, other.value) == 0;
        }

        @Override
        public int hashCode() {
            return Double.hashCode(value);
        }
    }

    final class StringValue implements NbtData {
        private final String value;

        public StringValue(String value) {
            this.value = Objects.requireNonNull(value, "value");
        }

        public String value() {
            return value;
        }

        @Override
        public boolean equals(Object value) {
            return value instanceof StringValue other && this.value.equals(other.value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }

    final class ByteArrayValue implements NbtData {
        private final byte[] value;

        public ByteArrayValue(byte[] value) {
            this.value = Objects.requireNonNull(value, "value").clone();
        }

        public byte[] value() {
            return value.clone();
        }

        public int length() {
            return value.length;
        }

        @Override
        public boolean equals(Object value) {
            return value instanceof ByteArrayValue other && Arrays.equals(this.value, other.value);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(value);
        }
    }

    final class IntArrayValue implements NbtData {
        private final int[] value;

        public IntArrayValue(int[] value) {
            this.value = Objects.requireNonNull(value, "value").clone();
        }

        public int[] value() {
            return value.clone();
        }

        public int length() {
            return value.length;
        }

        @Override
        public boolean equals(Object value) {
            return value instanceof IntArrayValue other && Arrays.equals(this.value, other.value);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(value);
        }
    }

    final class LongArrayValue implements NbtData {
        private final long[] value;

        public LongArrayValue(long[] value) {
            this.value = Objects.requireNonNull(value, "value").clone();
        }

        public long[] value() {
            return value.clone();
        }

        public int length() {
            return value.length;
        }

        @Override
        public boolean equals(Object value) {
            return value instanceof LongArrayValue other && Arrays.equals(this.value, other.value);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(value);
        }
    }

    final class ListValue implements NbtData {
        private final List<NbtData> values;

        public ListValue(List<? extends NbtData> values) {
            Objects.requireNonNull(values, "values");
            ArrayList<NbtData> copy = new ArrayList<>(values.size());
            Class<?> elementType = null;
            for (NbtData value : values) {
                NbtData nonNull = Objects.requireNonNull(value, "list value");
                if (elementType == null) {
                    elementType = nonNull.getClass();
                } else if (elementType != nonNull.getClass()) {
                    throw new IllegalArgumentException("NBT list contains mixed element types");
                }
                copy.add(nonNull);
            }
            this.values = Collections.unmodifiableList(copy);
        }

        public List<NbtData> values() {
            return values;
        }

        public boolean isEmpty() {
            return values.isEmpty();
        }

        @Override
        public boolean equals(Object value) {
            return value instanceof ListValue other && values.equals(other.values);
        }

        @Override
        public int hashCode() {
            return values.hashCode();
        }
    }

    final class Compound implements NbtData {
        private final Map<String, NbtData> values;

        public Compound(Map<String, ? extends NbtData> values) {
            Objects.requireNonNull(values, "values");
            LinkedHashMap<String, NbtData> copy = new LinkedHashMap<>();
            for (Map.Entry<String, ? extends NbtData> entry : values.entrySet()) {
                copy.put(Objects.requireNonNull(entry.getKey(), "compound key"),
                        Objects.requireNonNull(entry.getValue(), "compound value"));
            }
            this.values = Collections.unmodifiableMap(copy);
        }

        public Map<String, NbtData> values() {
            return values;
        }

        public Set<String> keys() {
            return values.keySet();
        }

        public boolean contains(String key) {
            return values.containsKey(key);
        }

        public NbtData get(String key) {
            return values.get(key);
        }

        public boolean isEmpty() {
            return values.isEmpty();
        }

        public Compound with(String key, NbtData value) {
            LinkedHashMap<String, NbtData> copy = new LinkedHashMap<>(values);
            copy.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
            return new Compound(copy);
        }

        public Compound without(String key) {
            if (!values.containsKey(key)) {
                return this;
            }
            LinkedHashMap<String, NbtData> copy = new LinkedHashMap<>(values);
            copy.remove(key);
            return new Compound(copy);
        }

        @Override
        public boolean equals(Object value) {
            return value instanceof Compound other && values.equals(other.values);
        }

        @Override
        public int hashCode() {
            return values.hashCode();
        }
    }

    final class CompoundBuilder {
        private final Map<String, NbtData> values = new LinkedHashMap<>();

        public CompoundBuilder put(String key, NbtData value) {
            values.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
            return this;
        }

        public CompoundBuilder putBoolean(String key, boolean value) {
            return put(key, byteValue((byte) (value ? 1 : 0)));
        }

        public CompoundBuilder putByte(String key, byte value) {
            return put(key, byteValue(value));
        }

        public CompoundBuilder putShort(String key, short value) {
            return put(key, shortValue(value));
        }

        public CompoundBuilder putInt(String key, int value) {
            return put(key, intValue(value));
        }

        public CompoundBuilder putLong(String key, long value) {
            return put(key, longValue(value));
        }

        public CompoundBuilder putFloat(String key, float value) {
            return put(key, floatValue(value));
        }

        public CompoundBuilder putDouble(String key, double value) {
            return put(key, doubleValue(value));
        }

        public CompoundBuilder putString(String key, String value) {
            return put(key, string(value));
        }

        public CompoundBuilder putUuid(String key, UUID value) {
            return put(key, uuid(value));
        }

        public Compound build() {
            return new Compound(values);
        }
    }
}

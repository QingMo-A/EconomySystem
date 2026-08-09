package com.mo.economy_system.target.neoforge1211.item;

import com.mo.economy_system.platform.nbt.NbtData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/** Native NeoForge 1.21.1 NBT boundary for shared immutable data. */
public final class NeoForge1211NbtAdapter {
    private NeoForge1211NbtAdapter() {}

    public static NbtData.Compound fromNative(CompoundTag value) {
        return (NbtData.Compound) fromNative((Tag) value);
    }

    public static CompoundTag toNative(NbtData.Compound value) {
        return (CompoundTag) toNative((NbtData) value);
    }

    public static NbtData fromNative(Tag value) {
        return switch (value.getId()) {
            case Tag.TAG_BYTE -> NbtData.byteValue(((NumericTag) value).getAsByte());
            case Tag.TAG_SHORT -> NbtData.shortValue(((NumericTag) value).getAsShort());
            case Tag.TAG_INT -> NbtData.intValue(((NumericTag) value).getAsInt());
            case Tag.TAG_LONG -> NbtData.longValue(((NumericTag) value).getAsLong());
            case Tag.TAG_FLOAT -> NbtData.floatValue(((NumericTag) value).getAsFloat());
            case Tag.TAG_DOUBLE -> NbtData.doubleValue(((NumericTag) value).getAsDouble());
            case Tag.TAG_BYTE_ARRAY -> NbtData.byteArray(((ByteArrayTag) value).getAsByteArray());
            case Tag.TAG_STRING -> NbtData.string(value.getAsString());
            case Tag.TAG_LIST -> fromNativeList((ListTag) value);
            case Tag.TAG_COMPOUND -> fromNativeCompound((CompoundTag) value);
            case Tag.TAG_INT_ARRAY -> NbtData.intArray(((IntArrayTag) value).getAsIntArray());
            case Tag.TAG_LONG_ARRAY -> NbtData.longArray(((LongArrayTag) value).getAsLongArray());
            default -> throw new IllegalArgumentException("unsupported native NBT type: " + value.getId());
        };
    }

    public static Tag toNative(NbtData value) {
        if (value instanceof NbtData.ByteValue number) return ByteTag.valueOf(number.value());
        if (value instanceof NbtData.ShortValue number) return ShortTag.valueOf(number.value());
        if (value instanceof NbtData.IntValue number) return IntTag.valueOf(number.value());
        if (value instanceof NbtData.LongValue number) return LongTag.valueOf(number.value());
        if (value instanceof NbtData.FloatValue number) return FloatTag.valueOf(number.value());
        if (value instanceof NbtData.DoubleValue number) return DoubleTag.valueOf(number.value());
        if (value instanceof NbtData.ByteArrayValue array) return new ByteArrayTag(array.value());
        if (value instanceof NbtData.StringValue string) return StringTag.valueOf(string.value());
        if (value instanceof NbtData.ListValue list) {
            ListTag result = new ListTag();
            for (NbtData child : list.values()) result.add(toNative(child));
            return result;
        }
        if (value instanceof NbtData.Compound compound) {
            CompoundTag result = new CompoundTag();
            for (Map.Entry<String, NbtData> entry : compound.values().entrySet()) {
                result.put(entry.getKey(), toNative(entry.getValue()));
            }
            return result;
        }
        if (value instanceof NbtData.IntArrayValue array) return new IntArrayTag(array.value());
        if (value instanceof NbtData.LongArrayValue array) return new LongArrayTag(array.value());
        throw new IllegalArgumentException("unsupported neutral NBT value: " + value.getClass().getName());
    }

    private static NbtData.Compound fromNativeCompound(CompoundTag value) {
        Map<String, NbtData> result = new LinkedHashMap<>();
        for (String key : value.getAllKeys()) {
            Tag child = value.get(key);
            if (child != null) result.put(key, fromNative(child));
        }
        return NbtData.compound(result);
    }

    private static NbtData.ListValue fromNativeList(ListTag value) {
        List<NbtData> result = new ArrayList<>(value.size());
        for (Tag child : value) result.add(fromNative(child));
        return NbtData.list(result);
    }
}

package com.mo.economy_system.platform.item;

import com.mo.economy_system.platform.nbt.NbtData;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Loader-neutral structural and size validation shared by every snapshot boundary. */
public final class ItemStackSnapshotValidator {
    private ItemStackSnapshotValidator() {}

    public static ItemStackSnapshotResult<ItemStackSnapshot> validate(ItemStackSnapshot snapshot) {
        if (snapshot == null) return failure(ItemStackSnapshotError.INVALID_SCHEMA, "snapshot is null");
        if (snapshot.itemId().isBlank()) return failure(ItemStackSnapshotError.INVALID_SCHEMA, "itemId is blank");
        if (snapshot.itemId().length() > ItemStackSnapshotLimits.MAX_ITEM_ID_LENGTH) return limit("itemId");
        if (snapshot.count() <= 0) return failure(ItemStackSnapshotError.INVALID_COUNT, "count must be positive");
        if (snapshot.customNameJson().isPresent()
                && snapshot.customNameJson().orElseThrow().length() > ItemStackSnapshotLimits.MAX_CUSTOM_NAME_JSON_LENGTH) return limit("customName");
        if (snapshot.loreJson().size() > ItemStackSnapshotLimits.MAX_LORE_LINES) return limit("lore lines");
        int loreTotal = 0;
        for (String line : snapshot.loreJson()) {
            if (line == null) return failure(ItemStackSnapshotError.INVALID_SCHEMA, "lore contains null");
            if (line.length() > ItemStackSnapshotLimits.MAX_LORE_LINE_JSON_LENGTH) return limit("lore line");
            loreTotal += line.length();
            if (loreTotal > ItemStackSnapshotLimits.MAX_LORE_TOTAL_JSON_LENGTH) return limit("lore total");
        }
        ItemStackSnapshotResult<Boolean> normal = validateEnchantments(snapshot.enchantments(), ItemStackSnapshotLimits.MAX_ENCHANTMENTS, "enchantments");
        if (!normal.isSuccess()) return copyFailure(normal);
        ItemStackSnapshotResult<Boolean> stored = validateEnchantments(snapshot.storedEnchantments(), ItemStackSnapshotLimits.MAX_STORED_ENCHANTMENTS, "storedEnchantments");
        if (!stored.isSuccess()) return copyFailure(stored);
        if (snapshot.damage() < 0 || snapshot.repairCost() < 0) return failure(ItemStackSnapshotError.INVALID_SCHEMA, "damage and repairCost must be non-negative");
        if (snapshot.dyedColor().isPresent() && (snapshot.dyedColor().getAsInt() < 0 || snapshot.dyedColor().getAsInt() > 0xFFFFFF)) {
            return failure(ItemStackSnapshotError.INVALID_SCHEMA, "dyedColor outside RGB range");
        }
        NbtData.Compound customData = snapshot.customData();
        if (maxDepth(customData) > ItemStackSnapshotLimits.MAX_CUSTOM_DATA_DEPTH) return limit("customData depth");
        if (estimatedBytes(customData) > ItemStackSnapshotLimits.MAX_CUSTOM_DATA_BYTES) return limit("customData bytes");
        if (estimatedBytes(ItemStackSnapshotCodec.encodeUnchecked(snapshot)) > ItemStackSnapshotLimits.MAX_ENCODED_SNAPSHOT_BYTES) {
            return limit("encoded snapshot bytes");
        }
        return ItemStackSnapshotResult.success(snapshot);
    }

    public static int estimatedBytes(NbtData data) {
        return estimate(data).getBytes(StandardCharsets.UTF_8).length;
    }

    public static int maxDepth(NbtData root) {
        if (root instanceof NbtData.Compound compound) {
            int deepest = 1;
            for (NbtData child : compound.values().values()) deepest = Math.max(deepest, 1 + maxDepth(child));
            return deepest;
        }
        if (root instanceof NbtData.ListValue list) {
            int deepest = 1;
            for (NbtData child : list.values()) deepest = Math.max(deepest, 1 + maxDepth(child));
            return deepest;
        }
        return 1;
    }

    private static String estimate(NbtData data) {
        if (data instanceof NbtData.ByteValue value) return value.value() + "b";
        if (data instanceof NbtData.ShortValue value) return value.value() + "s";
        if (data instanceof NbtData.IntValue value) return Integer.toString(value.value());
        if (data instanceof NbtData.LongValue value) return value.value() + "L";
        if (data instanceof NbtData.FloatValue value) return value.value() + "f";
        if (data instanceof NbtData.DoubleValue value) return value.value() + "d";
        if (data instanceof NbtData.StringValue value) return '"' + escaped(value.value()) + '"';
        if (data instanceof NbtData.ByteArrayValue value) return "[B;" + value.length() + "]";
        if (data instanceof NbtData.IntArrayValue value) return "[I;" + value.length() + "]";
        if (data instanceof NbtData.LongArrayValue value) return "[L;" + value.length() + "]";
        if (data instanceof NbtData.ListValue list) {
            StringBuilder result = new StringBuilder("[");
            for (NbtData child : list.values()) result.append(estimate(child)).append(',');
            return result.append(']').toString();
        }
        NbtData.Compound compound = (NbtData.Compound) data;
        StringBuilder result = new StringBuilder("{");
        for (Map.Entry<String, NbtData> entry : compound.values().entrySet()) {
            result.append(escaped(entry.getKey())).append(':').append(estimate(entry.getValue())).append(',');
        }
        return result.append('}').toString();
    }

    private static String escaped(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static ItemStackSnapshotResult<Boolean> validateEnchantments(Map<String, Integer> values, int maximum, String field) {
        if (values.size() > maximum) return limit(field + " count");
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) return failure(ItemStackSnapshotError.INVALID_SCHEMA, field + " contains null");
            if (entry.getKey().isBlank()) return failure(ItemStackSnapshotError.INVALID_SCHEMA, field + " contains blank id");
            if (entry.getKey().length() > ItemStackSnapshotLimits.MAX_ENCHANTMENT_ID_LENGTH) return limit(field + " id");
            if (entry.getValue() <= 0) return failure(ItemStackSnapshotError.INVALID_SCHEMA, field + " level must be positive");
        }
        return ItemStackSnapshotResult.success(Boolean.TRUE);
    }

    private static <T> ItemStackSnapshotResult<T> limit(String field) {
        return failure(ItemStackSnapshotError.DATA_LIMIT_EXCEEDED, field + " exceeds snapshot limit");
    }

    private static <T> ItemStackSnapshotResult<T> failure(ItemStackSnapshotError error, String detail) {
        return ItemStackSnapshotResult.failure(error, detail);
    }

    private static <T, U> ItemStackSnapshotResult<T> copyFailure(ItemStackSnapshotResult<U> result) {
        return failure(result.error().orElseThrow(), result.detail());
    }
}

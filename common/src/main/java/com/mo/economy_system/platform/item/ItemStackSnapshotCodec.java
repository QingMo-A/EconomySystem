package com.mo.economy_system.platform.item;

import com.mo.economy_system.platform.nbt.NbtData;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/** Strict schema codec. New writes are always schema v1; compact tags are read-only compatibility input. */
public final class ItemStackSnapshotCodec {
    private static final Set<String> ROOT_KEYS = Set.of("schemaVersion", "id", "count", "components");
    private static final Set<String> LEGACY_KEYS = Set.of("id", "count", "customData");
    private static final Set<String> COMPONENT_KEYS = Set.of(
            "customName", "lore", "enchantments", "storedEnchantments", "damage", "repairCost",
            "unbreakable", "dyedColor", "customModelData", "customData"
    );

    private ItemStackSnapshotCodec() {}

    public static ItemStackSnapshotResult<NbtData.Compound> encode(ItemStackSnapshot snapshot) {
        ItemStackSnapshotResult<ItemStackSnapshot> validation = ItemStackSnapshotValidator.validate(snapshot);
        if (!validation.isSuccess()) return copyFailure(validation);
        return ItemStackSnapshotResult.success(encodeUnchecked(snapshot));
    }

    static NbtData.Compound encodeUnchecked(ItemStackSnapshot snapshot) {
        NbtData.CompoundBuilder components = NbtData.compoundBuilder();
        snapshot.customNameJson().ifPresent(value -> components.putString("customName", value));
        if (!snapshot.loreJson().isEmpty()) {
            components.put("lore", NbtData.list(snapshot.loreJson().stream().map(NbtData::string).toList()));
        }
        putEnchantments(components, "enchantments", snapshot.enchantments(), snapshot.enchantmentsShown());
        putEnchantments(components, "storedEnchantments", snapshot.storedEnchantments(), snapshot.storedEnchantmentsShown());
        if (snapshot.damage() != 0) components.putInt("damage", snapshot.damage());
        if (snapshot.repairCost() != 0) components.putInt("repairCost", snapshot.repairCost());
        if (snapshot.unbreakable()) {
            components.put("unbreakable", NbtData.compoundBuilder()
                    .putBoolean("showInTooltip", snapshot.unbreakableShown()).build());
        }
        if (snapshot.dyedColor().isPresent()) {
            components.put("dyedColor", NbtData.compoundBuilder()
                    .putInt("rgb", snapshot.dyedColor().getAsInt())
                    .putBoolean("showInTooltip", snapshot.dyedColorShown()).build());
        }
        snapshot.customModelData().ifPresent(value -> components.putInt("customModelData", value));
        if (!snapshot.customData().isEmpty()) components.put("customData", snapshot.customData());
        return NbtData.compoundBuilder()
                .putInt("schemaVersion", ItemStackSnapshot.CURRENT_SCHEMA_VERSION)
                .putString("id", snapshot.itemId())
                .putInt("count", snapshot.count())
                .put("components", components.build())
                .build();
    }

    public static ItemStackSnapshotResult<ItemStackSnapshot> decode(NbtData.Compound input) {
        if (input == null) return failure(ItemStackSnapshotError.INVALID_SCHEMA, "snapshot is null");
        if (ItemStackSnapshotValidator.maxDepth(input) > ItemStackSnapshotLimits.MAX_CUSTOM_DATA_DEPTH + 2) {
            return failure(ItemStackSnapshotError.DATA_LIMIT_EXCEEDED, "encoded snapshot exceeds nesting limit");
        }
        if (ItemStackSnapshotValidator.estimatedBytes(input) > ItemStackSnapshotLimits.MAX_ENCODED_SNAPSHOT_BYTES) {
            return failure(ItemStackSnapshotError.DATA_LIMIT_EXCEEDED, "encoded snapshot exceeds byte limit");
        }
        NbtData.Compound root = input;
        try {
            if (!root.contains("schemaVersion")) return decodeLegacy(root);
            String unknownRoot = unknownKey(root, ROOT_KEYS);
            if (unknownRoot != null) return failure(ItemStackSnapshotError.INVALID_SCHEMA, "unknown root field: " + unknownRoot);
            if (!(root.get("schemaVersion") instanceof NbtData.IntValue value)) return failure(ItemStackSnapshotError.INVALID_SCHEMA, "schemaVersion must be an int");
            int version = value.value();
            if (version != ItemStackSnapshot.CURRENT_SCHEMA_VERSION) {
                return failure(ItemStackSnapshotError.UNSUPPORTED_SCHEMA_VERSION, "schemaVersion=" + version);
            }
            return decodeV1(root);
        } catch (RuntimeException exception) {
            return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, exception.getMessage());
        }
    }

    private static ItemStackSnapshotResult<ItemStackSnapshot> decodeLegacy(NbtData.Compound root) {
        String unknown = unknownKey(root, LEGACY_KEYS);
        if (unknown != null) return failure(ItemStackSnapshotError.INVALID_SCHEMA, "unknown legacy field: " + unknown);
        ItemStackSnapshotResult<BaseFields> base = readBase(root);
        if (!base.isSuccess()) return copyFailure(base);
        if (root.contains("customData") && !(root.get("customData") instanceof NbtData.Compound)) {
            return failure(ItemStackSnapshotError.INVALID_SCHEMA, "customData must be a compound");
        }
        BaseFields fields = base.orElseThrow();
        NbtData.Compound customData = root.get("customData") instanceof NbtData.Compound value ? value : NbtData.emptyCompound();
        return create(fields, Optional.empty(), List.of(), Map.of(), Map.of(), true, true, 0, 0,
                false, true, OptionalInt.empty(), true, OptionalInt.empty(), customData);
    }

    private static ItemStackSnapshotResult<ItemStackSnapshot> decodeV1(NbtData.Compound root) {
        ItemStackSnapshotResult<BaseFields> base = readBase(root);
        if (!base.isSuccess()) return copyFailure(base);
        if (!(root.get("components") instanceof NbtData.Compound components)) {
            return failure(ItemStackSnapshotError.INVALID_SCHEMA, "components must be a compound");
        }
        String unknown = unknownKey(components, COMPONENT_KEYS);
        if (unknown != null) return failure(ItemStackSnapshotError.UNSUPPORTED_COMPONENT, "unknown component: " + unknown);

        Optional<String> name = Optional.empty();
        if (components.contains("customName")) {
            if (!(components.get("customName") instanceof NbtData.StringValue value)) return failure(ItemStackSnapshotError.INVALID_SCHEMA, "customName must be a string");
            name = Optional.of(value.value());
        }
        ItemStackSnapshotResult<List<String>> loreResult = readStringList(components, "lore");
        if (!loreResult.isSuccess()) return copyFailure(loreResult);
        ItemStackSnapshotResult<Enchantments> enchantments = readEnchantments(components, "enchantments");
        if (!enchantments.isSuccess()) return copyFailure(enchantments);
        ItemStackSnapshotResult<Enchantments> stored = readEnchantments(components, "storedEnchantments");
        if (!stored.isSuccess()) return copyFailure(stored);
        ItemStackSnapshotResult<Integer> damage = readNonNegativeInt(components, "damage");
        if (!damage.isSuccess()) return copyFailure(damage);
        ItemStackSnapshotResult<Integer> repair = readNonNegativeInt(components, "repairCost");
        if (!repair.isSuccess()) return copyFailure(repair);
        ItemStackSnapshotResult<ShownValue> unbreakable = readShownValue(components, "unbreakable", false);
        if (!unbreakable.isSuccess()) return copyFailure(unbreakable);
        ItemStackSnapshotResult<ColorValue> color = readColor(components);
        if (!color.isSuccess()) return copyFailure(color);
        OptionalInt model = OptionalInt.empty();
        if (components.contains("customModelData")) {
            if (!(components.get("customModelData") instanceof NbtData.IntValue value)) return failure(ItemStackSnapshotError.INVALID_SCHEMA, "customModelData must be an int");
            model = OptionalInt.of(value.value());
        }
        if (components.contains("customData") && !(components.get("customData") instanceof NbtData.Compound)) {
            return failure(ItemStackSnapshotError.INVALID_SCHEMA, "customData must be a compound");
        }
        NbtData.Compound customData = components.get("customData") instanceof NbtData.Compound value ? value : NbtData.emptyCompound();
        BaseFields fields = base.orElseThrow();
        Enchantments normal = enchantments.orElseThrow();
        Enchantments book = stored.orElseThrow();
        ShownValue unbreakableValue = unbreakable.orElseThrow();
        ColorValue colorValue = color.orElseThrow();
        return create(fields, name, loreResult.orElseThrow(), normal.values, book.values, normal.shown, book.shown,
                damage.orElseThrow(), repair.orElseThrow(), unbreakableValue.present, unbreakableValue.shown,
                colorValue.rgb, colorValue.shown, model, customData);
    }

    private static ItemStackSnapshotResult<ItemStackSnapshot> create(
            BaseFields fields, Optional<String> name, List<String> lore, Map<String, Integer> enchantments,
            Map<String, Integer> storedEnchantments, boolean enchantmentsShown, boolean storedEnchantmentsShown,
            int damage, int repairCost, boolean unbreakable, boolean unbreakableShown, OptionalInt color,
            boolean colorShown, OptionalInt model, NbtData.Compound customData) {
        return ItemStackSnapshot.create(fields.id, fields.count, name, lore, enchantments, storedEnchantments,
                enchantmentsShown, storedEnchantmentsShown, damage, repairCost, unbreakable, unbreakableShown,
                color, colorShown, model, customData);
    }

    private static ItemStackSnapshotResult<BaseFields> readBase(NbtData.Compound root) {
        if (!(root.get("id") instanceof NbtData.StringValue id) || id.value().isBlank()) {
            return failure(ItemStackSnapshotError.INVALID_SCHEMA, "id must be a non-empty string");
        }
        if (!(root.get("count") instanceof NbtData.IntValue countValue)) return failure(ItemStackSnapshotError.INVALID_COUNT, "count must be an int");
        int count = countValue.value();
        if (count <= 0) return failure(ItemStackSnapshotError.INVALID_COUNT, "count must be positive");
        return ItemStackSnapshotResult.success(new BaseFields(id.value(), count));
    }

    private static ItemStackSnapshotResult<List<String>> readStringList(NbtData.Compound components, String key) {
        if (!components.contains(key)) return ItemStackSnapshotResult.success(List.of());
        if (!(components.get(key) instanceof NbtData.ListValue list)) return failure(ItemStackSnapshotError.INVALID_SCHEMA, key + " must be a list");
        if (list.values().stream().anyMatch(entry -> !(entry instanceof NbtData.StringValue))) return failure(ItemStackSnapshotError.INVALID_SCHEMA, key + " entries must be strings");
        return ItemStackSnapshotResult.success(list.values().stream().map(entry -> ((NbtData.StringValue) entry).value()).toList());
    }

    private static void putEnchantments(NbtData.CompoundBuilder components, String key, Map<String, Integer> values, boolean shown) {
        if (values.isEmpty() && shown) return;
        List<NbtData> entries = values.entrySet().stream().sorted(Map.Entry.comparingByKey()).<NbtData>map(entry ->
                NbtData.compoundBuilder().putString("id", entry.getKey()).putInt("level", entry.getValue()).build()).toList();
        components.put(key, NbtData.compoundBuilder().putBoolean("showInTooltip", shown)
                .put("entries", NbtData.list(entries)).build());
    }

    private static ItemStackSnapshotResult<Enchantments> readEnchantments(NbtData.Compound components, String key) {
        if (!components.contains(key)) return ItemStackSnapshotResult.success(new Enchantments(Map.of(), true));
        if (!(components.get(key) instanceof NbtData.Compound value)) return failure(ItemStackSnapshotError.INVALID_SCHEMA, key + " must be a compound");
        String unknown = unknownKey(value, Set.of("showInTooltip", "entries"));
        if (unknown != null) return failure(ItemStackSnapshotError.INVALID_SCHEMA, "unknown " + key + " field: " + unknown);
        if (!(value.get("showInTooltip") instanceof NbtData.ByteValue) || !(value.get("entries") instanceof NbtData.ListValue)) {
            return failure(ItemStackSnapshotError.INVALID_SCHEMA, key + " requires showInTooltip and entries");
        }
        NbtData.ListValue entries = (NbtData.ListValue) value.get("entries");
        if (entries.values().stream().anyMatch(entry -> !(entry instanceof NbtData.Compound))) return failure(ItemStackSnapshotError.INVALID_SCHEMA, key + " entries must be compounds");
        Map<String, Integer> decoded = new LinkedHashMap<>();
        for (NbtData entryTag : entries.values()) {
            NbtData.Compound entry = (NbtData.Compound) entryTag;
            if (unknownKey(entry, Set.of("id", "level")) != null || !(entry.get("id") instanceof NbtData.StringValue idValue) || !(entry.get("level") instanceof NbtData.IntValue levelValue)) {
                return failure(ItemStackSnapshotError.INVALID_SCHEMA, "invalid " + key + " entry");
            }
            int level = levelValue.value();
            String id = idValue.value();
            if (id.isBlank() || level <= 0 || decoded.put(id, level) != null) {
                return failure(ItemStackSnapshotError.INVALID_SCHEMA, "invalid or duplicate " + key + " entry: " + id);
            }
        }
        return ItemStackSnapshotResult.success(new Enchantments(decoded, ((NbtData.ByteValue) value.get("showInTooltip")).value() != 0));
    }

    private static ItemStackSnapshotResult<Integer> readNonNegativeInt(NbtData.Compound components, String key) {
        if (!components.contains(key)) return ItemStackSnapshotResult.success(0);
        if (!(components.get(key) instanceof NbtData.IntValue value) || value.value() < 0) return failure(ItemStackSnapshotError.INVALID_SCHEMA, key + " must be a non-negative int");
        return ItemStackSnapshotResult.success(value.value());
    }

    private static ItemStackSnapshotResult<ShownValue> readShownValue(NbtData.Compound components, String key, boolean defaultPresent) {
        if (!components.contains(key)) return ItemStackSnapshotResult.success(new ShownValue(defaultPresent, true));
        if (!(components.get(key) instanceof NbtData.Compound value)) return failure(ItemStackSnapshotError.INVALID_SCHEMA, key + " must be a compound");
        if (!value.keys().equals(Set.of("showInTooltip")) || !(value.get("showInTooltip") instanceof NbtData.ByteValue shown)) {
            return failure(ItemStackSnapshotError.INVALID_SCHEMA, "invalid " + key);
        }
        return ItemStackSnapshotResult.success(new ShownValue(true, shown.value() != 0));
    }

    private static ItemStackSnapshotResult<ColorValue> readColor(NbtData.Compound components) {
        if (!components.contains("dyedColor")) return ItemStackSnapshotResult.success(new ColorValue(OptionalInt.empty(), true));
        if (!(components.get("dyedColor") instanceof NbtData.Compound value)) return failure(ItemStackSnapshotError.INVALID_SCHEMA, "dyedColor must be a compound");
        if (!value.keys().equals(Set.of("rgb", "showInTooltip")) || !(value.get("rgb") instanceof NbtData.IntValue rgbValue) || !(value.get("showInTooltip") instanceof NbtData.ByteValue shown)) {
            return failure(ItemStackSnapshotError.INVALID_SCHEMA, "invalid dyedColor");
        }
        int rgb = rgbValue.value();
        if (rgb < 0 || rgb > 0xFFFFFF) return failure(ItemStackSnapshotError.INVALID_SCHEMA, "dyedColor rgb outside 0x000000..0xFFFFFF");
        return ItemStackSnapshotResult.success(new ColorValue(OptionalInt.of(rgb), shown.value() != 0));
    }

    private static String unknownKey(NbtData.Compound tag, Set<String> allowed) {
        Set<String> unknown = new HashSet<>(tag.keys());
        unknown.removeAll(allowed);
        return unknown.stream().sorted().findFirst().orElse(null);
    }

    private static <T> ItemStackSnapshotResult<T> failure(ItemStackSnapshotError error, String detail) {
        return ItemStackSnapshotResult.failure(error, detail);
    }

    private static <T, U> ItemStackSnapshotResult<T> copyFailure(ItemStackSnapshotResult<U> result) {
        return failure(result.error().orElseThrow(), result.detail());
    }

    private record BaseFields(String id, int count) {}
    private record Enchantments(Map<String, Integer> values, boolean shown) {}
    private record ShownValue(boolean present, boolean shown) {}
    private record ColorValue(OptionalInt rgb, boolean shown) {}
}

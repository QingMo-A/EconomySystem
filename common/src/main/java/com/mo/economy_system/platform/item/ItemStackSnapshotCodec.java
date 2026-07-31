package com.mo.economy_system.platform.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

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

    public static CompoundTag encode(ItemStackSnapshot snapshot) {
        CompoundTag root = new CompoundTag();
        root.putInt("schemaVersion", ItemStackSnapshot.CURRENT_SCHEMA_VERSION);
        root.putString("id", snapshot.itemId());
        root.putInt("count", snapshot.count());
        CompoundTag components = new CompoundTag();
        snapshot.customNameJson().ifPresent(value -> components.putString("customName", value));
        if (!snapshot.loreJson().isEmpty()) {
            ListTag lore = new ListTag();
            snapshot.loreJson().forEach(value -> lore.add(net.minecraft.nbt.StringTag.valueOf(value)));
            components.put("lore", lore);
        }
        putEnchantments(components, "enchantments", snapshot.enchantments(), snapshot.enchantmentsShown());
        putEnchantments(components, "storedEnchantments", snapshot.storedEnchantments(), snapshot.storedEnchantmentsShown());
        if (snapshot.damage() != 0) components.putInt("damage", snapshot.damage());
        if (snapshot.repairCost() != 0) components.putInt("repairCost", snapshot.repairCost());
        if (snapshot.unbreakable()) {
            CompoundTag value = new CompoundTag();
            value.putBoolean("showInTooltip", snapshot.unbreakableShown());
            components.put("unbreakable", value);
        }
        if (snapshot.dyedColor().isPresent()) {
            CompoundTag value = new CompoundTag();
            value.putInt("rgb", snapshot.dyedColor().getAsInt());
            value.putBoolean("showInTooltip", snapshot.dyedColorShown());
            components.put("dyedColor", value);
        }
        snapshot.customModelData().ifPresent(value -> components.putInt("customModelData", value));
        CompoundTag customData = snapshot.customData();
        if (!customData.isEmpty()) components.put("customData", customData);
        root.put("components", components);
        return root;
    }

    public static ItemStackSnapshotResult<ItemStackSnapshot> decode(CompoundTag input) {
        if (input == null) return failure(ItemStackSnapshotError.INVALID_SCHEMA, "snapshot is null");
        CompoundTag root = input.copy();
        try {
            if (!root.contains("schemaVersion")) return decodeLegacy(root);
            String unknownRoot = unknownKey(root, ROOT_KEYS);
            if (unknownRoot != null) return failure(ItemStackSnapshotError.INVALID_SCHEMA, "unknown root field: " + unknownRoot);
            if (!root.contains("schemaVersion", Tag.TAG_INT)) return failure(ItemStackSnapshotError.INVALID_SCHEMA, "schemaVersion must be an int");
            int version = root.getInt("schemaVersion");
            if (version != ItemStackSnapshot.CURRENT_SCHEMA_VERSION) {
                return failure(ItemStackSnapshotError.UNSUPPORTED_SCHEMA_VERSION, "schemaVersion=" + version);
            }
            return decodeV1(root);
        } catch (RuntimeException exception) {
            return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, exception.getMessage());
        }
    }

    private static ItemStackSnapshotResult<ItemStackSnapshot> decodeLegacy(CompoundTag root) {
        String unknown = unknownKey(root, LEGACY_KEYS);
        if (unknown != null) return failure(ItemStackSnapshotError.INVALID_SCHEMA, "unknown legacy field: " + unknown);
        ItemStackSnapshotResult<BaseFields> base = readBase(root);
        if (!base.isSuccess()) return copyFailure(base);
        if (root.contains("customData") && !root.contains("customData", Tag.TAG_COMPOUND)) {
            return failure(ItemStackSnapshotError.INVALID_SCHEMA, "customData must be a compound");
        }
        BaseFields fields = base.orElseThrow();
        CompoundTag customData = root.contains("customData", Tag.TAG_COMPOUND) ? root.getCompound("customData") : new CompoundTag();
        return ItemStackSnapshotResult.success(empty(fields, customData));
    }

    private static ItemStackSnapshotResult<ItemStackSnapshot> decodeV1(CompoundTag root) {
        ItemStackSnapshotResult<BaseFields> base = readBase(root);
        if (!base.isSuccess()) return copyFailure(base);
        if (!root.contains("components", Tag.TAG_COMPOUND)) {
            return failure(ItemStackSnapshotError.INVALID_SCHEMA, "components must be a compound");
        }
        CompoundTag components = root.getCompound("components");
        String unknown = unknownKey(components, COMPONENT_KEYS);
        if (unknown != null) return failure(ItemStackSnapshotError.UNSUPPORTED_COMPONENT, "unknown component: " + unknown);

        Optional<String> name = Optional.empty();
        if (components.contains("customName")) {
            if (!components.contains("customName", Tag.TAG_STRING)) return failure(ItemStackSnapshotError.INVALID_SCHEMA, "customName must be a string");
            name = Optional.of(components.getString("customName"));
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
            if (!components.contains("customModelData", Tag.TAG_INT)) return failure(ItemStackSnapshotError.INVALID_SCHEMA, "customModelData must be an int");
            model = OptionalInt.of(components.getInt("customModelData"));
        }
        if (components.contains("customData") && !components.contains("customData", Tag.TAG_COMPOUND)) {
            return failure(ItemStackSnapshotError.INVALID_SCHEMA, "customData must be a compound");
        }
        CompoundTag customData = components.contains("customData", Tag.TAG_COMPOUND) ? components.getCompound("customData") : new CompoundTag();
        BaseFields fields = base.orElseThrow();
        Enchantments normal = enchantments.orElseThrow();
        Enchantments book = stored.orElseThrow();
        ShownValue unbreakableValue = unbreakable.orElseThrow();
        ColorValue colorValue = color.orElseThrow();
        return ItemStackSnapshotResult.success(new ItemStackSnapshot(fields.id, fields.count, name, loreResult.orElseThrow(),
                normal.values, book.values, normal.shown, book.shown, damage.orElseThrow(), repair.orElseThrow(),
                unbreakableValue.present, unbreakableValue.shown, colorValue.rgb, colorValue.shown, model, customData));
    }

    private static ItemStackSnapshot empty(BaseFields fields, CompoundTag customData) {
        return new ItemStackSnapshot(fields.id, fields.count, Optional.empty(), List.of(), Map.of(), Map.of(),
                true, true, 0, 0, false, true, OptionalInt.empty(), true, OptionalInt.empty(), customData);
    }

    private static ItemStackSnapshotResult<BaseFields> readBase(CompoundTag root) {
        if (!root.contains("id", Tag.TAG_STRING) || root.getString("id").isBlank()) {
            return failure(ItemStackSnapshotError.INVALID_SCHEMA, "id must be a non-empty string");
        }
        if (!root.contains("count", Tag.TAG_INT)) return failure(ItemStackSnapshotError.INVALID_COUNT, "count must be an int");
        int count = root.getInt("count");
        if (count <= 0) return failure(ItemStackSnapshotError.INVALID_COUNT, "count must be positive");
        return ItemStackSnapshotResult.success(new BaseFields(root.getString("id"), count));
    }

    private static ItemStackSnapshotResult<List<String>> readStringList(CompoundTag components, String key) {
        if (!components.contains(key)) return ItemStackSnapshotResult.success(List.of());
        if (!components.contains(key, Tag.TAG_LIST)) return failure(ItemStackSnapshotError.INVALID_SCHEMA, key + " must be a list");
        ListTag list = (ListTag) components.get(key);
        if (list.stream().anyMatch(entry -> entry.getId() != Tag.TAG_STRING)) return failure(ItemStackSnapshotError.INVALID_SCHEMA, key + " entries must be strings");
        return ItemStackSnapshotResult.success(list.stream().map(Tag::getAsString).toList());
    }

    private static void putEnchantments(CompoundTag components, String key, Map<String, Integer> values, boolean shown) {
        if (values.isEmpty() && shown) return;
        CompoundTag value = new CompoundTag();
        value.putBoolean("showInTooltip", shown);
        ListTag entries = new ListTag();
        values.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            CompoundTag encoded = new CompoundTag();
            encoded.putString("id", entry.getKey());
            encoded.putInt("level", entry.getValue());
            entries.add(encoded);
        });
        value.put("entries", entries);
        components.put(key, value);
    }

    private static ItemStackSnapshotResult<Enchantments> readEnchantments(CompoundTag components, String key) {
        if (!components.contains(key)) return ItemStackSnapshotResult.success(new Enchantments(Map.of(), true));
        if (!components.contains(key, Tag.TAG_COMPOUND)) return failure(ItemStackSnapshotError.INVALID_SCHEMA, key + " must be a compound");
        CompoundTag value = components.getCompound(key);
        String unknown = unknownKey(value, Set.of("showInTooltip", "entries"));
        if (unknown != null) return failure(ItemStackSnapshotError.INVALID_SCHEMA, "unknown " + key + " field: " + unknown);
        if (!value.contains("showInTooltip", Tag.TAG_BYTE) || !value.contains("entries", Tag.TAG_LIST)) {
            return failure(ItemStackSnapshotError.INVALID_SCHEMA, key + " requires showInTooltip and entries");
        }
        ListTag entries = (ListTag) value.get("entries");
        if (entries.stream().anyMatch(entry -> entry.getId() != Tag.TAG_COMPOUND)) return failure(ItemStackSnapshotError.INVALID_SCHEMA, key + " entries must be compounds");
        Map<String, Integer> decoded = new LinkedHashMap<>();
        for (Tag entryTag : entries) {
            CompoundTag entry = (CompoundTag) entryTag;
            if (unknownKey(entry, Set.of("id", "level")) != null || !entry.contains("id", Tag.TAG_STRING) || !entry.contains("level", Tag.TAG_INT)) {
                return failure(ItemStackSnapshotError.INVALID_SCHEMA, "invalid " + key + " entry");
            }
            int level = entry.getInt("level");
            String id = entry.getString("id");
            if (id.isBlank() || level <= 0 || decoded.put(id, level) != null) {
                return failure(ItemStackSnapshotError.INVALID_SCHEMA, "invalid or duplicate " + key + " entry: " + id);
            }
        }
        return ItemStackSnapshotResult.success(new Enchantments(decoded, value.getBoolean("showInTooltip")));
    }

    private static ItemStackSnapshotResult<Integer> readNonNegativeInt(CompoundTag components, String key) {
        if (!components.contains(key)) return ItemStackSnapshotResult.success(0);
        if (!components.contains(key, Tag.TAG_INT) || components.getInt(key) < 0) return failure(ItemStackSnapshotError.INVALID_SCHEMA, key + " must be a non-negative int");
        return ItemStackSnapshotResult.success(components.getInt(key));
    }

    private static ItemStackSnapshotResult<ShownValue> readShownValue(CompoundTag components, String key, boolean defaultPresent) {
        if (!components.contains(key)) return ItemStackSnapshotResult.success(new ShownValue(defaultPresent, true));
        if (!components.contains(key, Tag.TAG_COMPOUND)) return failure(ItemStackSnapshotError.INVALID_SCHEMA, key + " must be a compound");
        CompoundTag value = components.getCompound(key);
        if (!value.getAllKeys().equals(Set.of("showInTooltip")) || !value.contains("showInTooltip", Tag.TAG_BYTE)) {
            return failure(ItemStackSnapshotError.INVALID_SCHEMA, "invalid " + key);
        }
        return ItemStackSnapshotResult.success(new ShownValue(true, value.getBoolean("showInTooltip")));
    }

    private static ItemStackSnapshotResult<ColorValue> readColor(CompoundTag components) {
        if (!components.contains("dyedColor")) return ItemStackSnapshotResult.success(new ColorValue(OptionalInt.empty(), true));
        if (!components.contains("dyedColor", Tag.TAG_COMPOUND)) return failure(ItemStackSnapshotError.INVALID_SCHEMA, "dyedColor must be a compound");
        CompoundTag value = components.getCompound("dyedColor");
        if (!value.getAllKeys().equals(Set.of("rgb", "showInTooltip")) || !value.contains("rgb", Tag.TAG_INT) || !value.contains("showInTooltip", Tag.TAG_BYTE)) {
            return failure(ItemStackSnapshotError.INVALID_SCHEMA, "invalid dyedColor");
        }
        int rgb = value.getInt("rgb");
        if (rgb < 0 || rgb > 0xFFFFFF) return failure(ItemStackSnapshotError.INVALID_SCHEMA, "dyedColor rgb outside 0x000000..0xFFFFFF");
        return ItemStackSnapshotResult.success(new ColorValue(OptionalInt.of(rgb), value.getBoolean("showInTooltip")));
    }

    private static String unknownKey(CompoundTag tag, Set<String> allowed) {
        Set<String> unknown = new HashSet<>(tag.getAllKeys());
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

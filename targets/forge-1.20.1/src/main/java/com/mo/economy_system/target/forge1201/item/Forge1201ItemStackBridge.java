package com.mo.economy_system.target.forge1201.item;

import com.mo.economy_system.platform.item.EconomyItemStackBridge;
import com.mo.economy_system.platform.item.ItemStackSnapshot;
import com.mo.economy_system.platform.item.ItemStackSnapshotError;
import com.mo.economy_system.platform.item.ItemStackSnapshotResult;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

public final class Forge1201ItemStackBridge implements EconomyItemStackBridge {
    private static final Set<String> NATIVE_KEYS = Set.of(
            "Damage", "RepairCost", "Unbreakable", "display", "Enchantments", "StoredEnchantments",
            "CustomModelData", "HideFlags"
    );
    private static final Set<String> UNSUPPORTED_NATIVE_KEYS = Set.of(
            "AttributeModifiers", "CanDestroy", "CanPlaceOn", "BlockEntityTag", "BlockStateTag", "EntityTag", "ForgeCaps",
            "Potion", "CustomPotionColor", "CustomPotionEffects", "Charged", "ChargedProjectiles", "map", "Decorations",
            "LodestonePos", "LodestoneDimension", "LodestoneTracked", "SkullOwner", "Trim", "Recipes", "Items",
            "generation", "author", "title", "resolved", "pages", "filtered_pages", "filtered_title", "Fireworks",
            "Explosion", "BucketVariantTag", "instrument", "note_block_sound"
    );
    @Override
    public boolean hasCustomData(ItemStack stack) {
        return stack.hasTag();
    }

    @Override
    public CompoundTag copyCustomData(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? null : tag.copy();
    }

    @Override
    public void setCustomData(ItemStack stack, CompoundTag tag) {
        stack.setTag(tag == null || tag.isEmpty() ? null : tag.copy());
    }

    @Override
    public boolean sameItemAndData(ItemStack first, ItemStack second) {
        return ItemStack.isSameItemSameTags(first, second);
    }

    @Override
    public ItemStackSnapshotResult<ItemStackSnapshot> captureSnapshot(ItemStack stack, HolderLookup.Provider registries) {
        if (stack == null || stack.isEmpty()) return failure(ItemStackSnapshotError.INVALID_SCHEMA, "cannot capture an empty stack");
        if (stack.getCount() <= 0 || stack.getCount() > stack.getMaxStackSize()) return failure(ItemStackSnapshotError.INVALID_COUNT, "count=" + stack.getCount());
        CompoundTag source = stack.getTag() == null ? new CompoundTag() : stack.getTag().copy();
        for (String key : UNSUPPORTED_NATIVE_KEYS) {
            if (source.contains(key)) return failure(ItemStackSnapshotError.UNSUPPORTED_COMPONENT, "unsupported native item field: " + key);
        }
        try {
            Optional<String> name = Optional.empty();
            List<String> lore = List.of();
            OptionalInt color = OptionalInt.empty();
            if (source.contains("display")) {
                if (!source.contains("display", Tag.TAG_COMPOUND)) return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, "display must be a compound");
                CompoundTag display = source.getCompound("display");
                Set<String> displayKeys = display.getAllKeys();
                for (String key : displayKeys) {
                    if (!Set.of("Name", "Lore", "color").contains(key)) return failure(ItemStackSnapshotError.UNSUPPORTED_COMPONENT, "unsupported display field: " + key);
                }
                if (display.contains("Name")) {
                    if (!display.contains("Name", Tag.TAG_STRING)) return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, "display.Name must be a string");
                    Component parsed = Component.Serializer.fromJson(display.getString("Name"));
                    if (parsed == null) return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, "display.Name parsed to null");
                    name = Optional.of(Component.Serializer.toJson(parsed));
                }
                if (display.contains("Lore")) {
                    if (!display.contains("Lore", Tag.TAG_LIST)) return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, "display.Lore must be a list");
                    ListTag list = (ListTag) display.get("Lore");
                    ArrayList<String> parsedLore = new ArrayList<>();
                    for (Tag lineTag : list) {
                        if (lineTag.getId() != Tag.TAG_STRING) return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, "display.Lore entries must be strings");
                        Component parsed = Component.Serializer.fromJson(lineTag.getAsString());
                        if (parsed == null) return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, "lore line parsed to null");
                        parsedLore.add(Component.Serializer.toJson(parsed));
                    }
                    lore = List.copyOf(parsedLore);
                }
                if (display.contains("color")) {
                    if (!display.contains("color", Tag.TAG_INT)) return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, "display.color must be an int");
                    int rgb = display.getInt("color");
                    if (rgb < 0 || rgb > 0xFFFFFF) return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, "invalid display.color");
                    color = OptionalInt.of(rgb);
                }
            }
            ItemStackSnapshotResult<Map<String, Integer>> enchantments = readEnchantments(source, "Enchantments");
            if (!enchantments.isSuccess()) return copyFailure(enchantments);
            ItemStackSnapshotResult<Map<String, Integer>> stored = readEnchantments(source, "StoredEnchantments");
            if (!stored.isSuccess()) return copyFailure(stored);
            for (String numeric : List.of("Damage", "RepairCost", "CustomModelData", "HideFlags")) {
                if (source.contains(numeric) && !source.contains(numeric, Tag.TAG_INT)) return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, numeric + " must be an int");
            }
            if (source.contains("Unbreakable") && !source.contains("Unbreakable", Tag.TAG_BYTE)) return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, "Unbreakable must be a byte");
            int hideFlags = source.getInt("HideFlags");
            int knownHideFlags = 1 | 4 | 64;
            if ((hideFlags & ~knownHideFlags) != 0) return failure(ItemStackSnapshotError.UNSUPPORTED_COMPONENT, "unsupported HideFlags bits: " + (hideFlags & ~knownHideFlags));
            int damage = source.getInt("Damage");
            int repairCost = source.getInt("RepairCost");
            if (damage < 0 || repairCost < 0 || (stack.isDamageableItem() && damage > stack.getMaxDamage())) return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, "invalid damage or repair cost");
            boolean unbreakable = source.getBoolean("Unbreakable");
            OptionalInt model = source.contains("CustomModelData", Tag.TAG_INT) ? OptionalInt.of(source.getInt("CustomModelData")) : OptionalInt.empty();
            CompoundTag customData = source.copy();
            NATIVE_KEYS.forEach(customData::remove);
            return ItemStackSnapshotResult.success(new ItemStackSnapshot(
                    BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount(), name, lore,
                    enchantments.orElseThrow(), stored.orElseThrow(), (hideFlags & 1) == 0, (hideFlags & 1) == 0,
                    damage, repairCost, unbreakable, (hideFlags & 4) == 0, color, (hideFlags & 64) == 0,
                    model, customData));
        } catch (RuntimeException exception) {
            return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, exception.getMessage());
        }
    }

    @Override
    public ItemStackSnapshotResult<ItemStack> restoreSnapshot(ItemStackSnapshot snapshot, HolderLookup.Provider registries) {
        ItemStackSnapshotResult<Boolean> validation = validateSnapshotData(snapshot);
        if (!validation.isSuccess()) return copyFailure(validation);
        ResourceLocation itemId;
        try { itemId = new ResourceLocation(snapshot.itemId()); }
        catch (RuntimeException exception) { return failure(ItemStackSnapshotError.UNKNOWN_ITEM_ID, snapshot.itemId()); }
        if (!BuiltInRegistries.ITEM.containsKey(itemId)) return failure(ItemStackSnapshotError.UNKNOWN_ITEM_ID, itemId.toString());
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(itemId), snapshot.count());
        if (snapshot.count() <= 0 || snapshot.count() > stack.getMaxStackSize()) return failure(ItemStackSnapshotError.INVALID_COUNT, "count=" + snapshot.count() + ", max=" + stack.getMaxStackSize());
        if ((!snapshot.enchantments().isEmpty() || !snapshot.storedEnchantments().isEmpty())
                && snapshot.enchantmentsShown() != snapshot.storedEnchantmentsShown()) {
            return failure(ItemStackSnapshotError.LOSSY_COMPONENT_CONVERSION, "1.20.1 has one shared enchantment tooltip flag");
        }
        CompoundTag tag = snapshot.customData();
        for (String key : NATIVE_KEYS) {
            if (tag.contains(key)) return failure(ItemStackSnapshotError.LOSSY_COMPONENT_CONVERSION, "customData collides with native field: " + key);
        }
        for (String key : UNSUPPORTED_NATIVE_KEYS) {
            if (tag.contains(key)) return failure(ItemStackSnapshotError.UNSUPPORTED_COMPONENT, "unsupported customData field: " + key);
        }
        try {
            CompoundTag display = new CompoundTag();
            if (snapshot.customNameJson().isPresent()) {
                Component component = Component.Serializer.fromJson(snapshot.customNameJson().orElseThrow());
                if (component == null) return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, "customName parsed to null");
                display.putString("Name", Component.Serializer.toJson(component));
            }
            if (!snapshot.loreJson().isEmpty()) {
                ListTag lore = new ListTag();
                for (String json : snapshot.loreJson()) {
                    Component component = Component.Serializer.fromJson(json);
                    if (component == null) return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, "lore line parsed to null");
                    lore.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(component)));
                }
                display.put("Lore", lore);
            }
            if (snapshot.dyedColor().isPresent()) display.putInt("color", snapshot.dyedColor().getAsInt());
            if (!display.isEmpty()) tag.put("display", display);
            ItemStackSnapshotResult<ListTag> enchantments = writeEnchantments(snapshot.enchantments());
            if (!enchantments.isSuccess()) return copyFailure(enchantments);
            if (!enchantments.orElseThrow().isEmpty()) tag.put("Enchantments", enchantments.orElseThrow());
            ItemStackSnapshotResult<ListTag> stored = writeEnchantments(snapshot.storedEnchantments());
            if (!stored.isSuccess()) return copyFailure(stored);
            if (!stored.orElseThrow().isEmpty()) tag.put("StoredEnchantments", stored.orElseThrow());
            if (snapshot.damage() != 0) {
                if (!stack.isDamageableItem() || snapshot.damage() > stack.getMaxDamage()) return failure(ItemStackSnapshotError.LOSSY_COMPONENT_CONVERSION, "invalid damage for " + itemId);
                tag.putInt("Damage", snapshot.damage());
            }
            if (snapshot.repairCost() != 0) tag.putInt("RepairCost", snapshot.repairCost());
            if (snapshot.unbreakable()) tag.putBoolean("Unbreakable", true);
            snapshot.customModelData().ifPresent(value -> tag.putInt("CustomModelData", value));
            int hideFlags = 0;
            if (!snapshot.enchantmentsShown() || !snapshot.storedEnchantmentsShown()) hideFlags |= 1;
            if (snapshot.unbreakable() && !snapshot.unbreakableShown()) hideFlags |= 4;
            if (snapshot.dyedColor().isPresent() && !snapshot.dyedColorShown()) hideFlags |= 64;
            if (hideFlags != 0) tag.putInt("HideFlags", hideFlags);
            stack.setTag(tag.isEmpty() ? null : tag);
            return ItemStackSnapshotResult.success(stack);
        } catch (RuntimeException exception) {
            return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, exception.getMessage());
        }
    }

    private static ItemStackSnapshotResult<Map<String, Integer>> readEnchantments(CompoundTag tag, String key) {
        if (!tag.contains(key)) return ItemStackSnapshotResult.success(Map.of());
        if (!tag.contains(key, Tag.TAG_LIST)) return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, key + " must be a list");
        ListTag list = (ListTag) tag.get(key);
        Map<String, Integer> values = new LinkedHashMap<>();
        for (Tag raw : list) {
            if (!(raw instanceof CompoundTag entry) || !entry.contains("id", Tag.TAG_STRING) || !entry.contains("lvl", Tag.TAG_ANY_NUMERIC)) {
                return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, "invalid " + key + " entry");
            }
            if (!entry.getAllKeys().equals(Set.of("id", "lvl"))) return failure(ItemStackSnapshotError.UNSUPPORTED_COMPONENT, "unknown " + key + " entry field");
            String id = entry.getString("id");
            int level = entry.getInt("lvl");
            ResourceLocation location;
            try { location = new ResourceLocation(id); }
            catch (RuntimeException exception) { return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, "invalid enchantment id: " + id); }
            if (!BuiltInRegistries.ENCHANTMENT.containsKey(location)) return failure(ItemStackSnapshotError.LOSSY_COMPONENT_CONVERSION, "unknown enchantment: " + id);
            if (level <= 0 || values.put(id, level) != null) return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, "invalid or duplicate enchantment: " + id);
        }
        return ItemStackSnapshotResult.success(values);
    }

    private static ItemStackSnapshotResult<ListTag> writeEnchantments(Map<String, Integer> values) {
        ListTag list = new ListTag();
        for (Map.Entry<String, Integer> value : values.entrySet()) {
            ResourceLocation id;
            try { id = new ResourceLocation(value.getKey()); }
            catch (RuntimeException exception) { return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, "invalid enchantment id: " + value.getKey()); }
            if (!BuiltInRegistries.ENCHANTMENT.containsKey(id)) return failure(ItemStackSnapshotError.LOSSY_COMPONENT_CONVERSION, "unknown enchantment: " + id);
            if (value.getValue() <= 0 || value.getValue() > Short.MAX_VALUE) return failure(ItemStackSnapshotError.LOSSY_COMPONENT_CONVERSION, "enchantment level outside 1.20.1 range: " + value.getValue());
            CompoundTag entry = new CompoundTag();
            entry.putString("id", id.toString());
            entry.putShort("lvl", value.getValue().shortValue());
            list.add(entry);
        }
        return ItemStackSnapshotResult.success(list);
    }

    private static ItemStackSnapshotResult<Boolean> validateSnapshotData(ItemStackSnapshot snapshot) {
        if (snapshot.damage() < 0 || snapshot.repairCost() < 0) return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, "negative damage or repair cost");
        if (snapshot.dyedColor().isPresent() && (snapshot.dyedColor().getAsInt() < 0 || snapshot.dyedColor().getAsInt() > 0xFFFFFF)) {
            return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, "dyed color outside RGB range");
        }
        for (int level : snapshot.enchantments().values()) if (level <= 0 || level > Short.MAX_VALUE) return failure(ItemStackSnapshotError.LOSSY_COMPONENT_CONVERSION, "enchantment level outside 1.20.1 range: " + level);
        for (int level : snapshot.storedEnchantments().values()) if (level <= 0 || level > Short.MAX_VALUE) return failure(ItemStackSnapshotError.LOSSY_COMPONENT_CONVERSION, "stored enchantment level outside 1.20.1 range: " + level);
        return ItemStackSnapshotResult.success(Boolean.TRUE);
    }

    private static <T> ItemStackSnapshotResult<T> failure(ItemStackSnapshotError error, String detail) {
        return ItemStackSnapshotResult.failure(error, detail);
    }

    private static <T, U> ItemStackSnapshotResult<T> copyFailure(ItemStackSnapshotResult<U> result) {
        return failure(result.error().orElseThrow(), result.detail());
    }

    @Override
    public CompoundTag saveSimple(ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        tag.putInt("count", stack.getCount());
        CompoundTag customData = copyCustomData(stack);
        if (customData != null && !customData.isEmpty()) {
            tag.put("customData", customData);
        }
        return tag;
    }

    @Override
    public ItemStack loadSimple(CompoundTag tag) {
        Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(tag.getString("id")));
        ItemStack stack = new ItemStack(item, Math.max(1, tag.getInt("count")));
        if (tag.contains("customData")) {
            setCustomData(stack, tag.getCompound("customData"));
        }
        return stack;
    }
}

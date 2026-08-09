package com.mo.economy_system.target.neoforge1211.item;

import com.mo.economy_system.platform.item.ItemStackSnapshot;
import com.mo.economy_system.platform.item.ItemStackSnapshotError;
import com.mo.economy_system.platform.item.ItemStackSnapshotResult;
import com.mo.economy_system.platform.item.ItemStackSnapshotValidator;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

public final class NeoForge1211ItemStackBridge {
    private static final Set<DataComponentType<?>> SUPPORTED_PATCH_COMPONENTS = Set.of(
            DataComponents.CUSTOM_NAME, DataComponents.LORE, DataComponents.ENCHANTMENTS,
            DataComponents.STORED_ENCHANTMENTS, DataComponents.DAMAGE, DataComponents.REPAIR_COST,
            DataComponents.UNBREAKABLE, DataComponents.DYED_COLOR, DataComponents.CUSTOM_MODEL_DATA,
            DataComponents.CUSTOM_DATA
    );
    public boolean hasCustomData(ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_DATA);
    }

    public CompoundTag copyCustomData(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    public void setCustomData(ItemStack stack, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag.copy()));
        }
    }

    public boolean sameItemAndData(ItemStack first, ItemStack second) {
        return ItemStack.isSameItemSameComponents(first, second);
    }

    public ItemStackSnapshotResult<ItemStackSnapshot> captureSnapshot(ItemStack stack, HolderLookup.Provider registries) {
        if (stack == null || stack.isEmpty()) return failure(ItemStackSnapshotError.INVALID_SCHEMA, "cannot capture an empty stack");
        if (stack.getCount() <= 0 || stack.getCount() > stack.getMaxStackSize()) {
            return failure(ItemStackSnapshotError.INVALID_COUNT, "count=" + stack.getCount() + ", max=" + stack.getMaxStackSize());
        }
        for (Map.Entry<DataComponentType<?>, Optional<?>> entry : stack.getComponentsPatch().entrySet()) {
            ResourceLocation componentId = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(entry.getKey());
            if (!SUPPORTED_PATCH_COMPONENTS.contains(entry.getKey())) {
                return failure(ItemStackSnapshotError.UNSUPPORTED_COMPONENT, "unsupported data component: " + componentId);
            }
            if (entry.getValue().isEmpty()) {
                return failure(ItemStackSnapshotError.LOSSY_COMPONENT_CONVERSION, "removed component cannot be represented: " + componentId);
            }
        }
        try {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            Optional<String> name = Optional.ofNullable(stack.get(DataComponents.CUSTOM_NAME))
                    .map(component -> serializeComponent(component, registries));
            ItemLore lore = stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
            List<String> loreJson = lore.lines().stream().map(component -> serializeComponent(component, registries)).toList();
            ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
            ItemStackSnapshotResult<Map<String, Integer>> enchantmentValues = captureEnchantments(enchantments);
            if (!enchantmentValues.isSuccess()) return copyFailure(enchantmentValues);
            ItemStackSnapshotResult<Map<String, Integer>> storedValues = captureEnchantments(stored);
            if (!storedValues.isSuccess()) return copyFailure(storedValues);
            int damage = stack.getOrDefault(DataComponents.DAMAGE, 0);
            if (damage < 0 || (damage != 0 && (stack.getMaxDamage() <= 0 || damage > stack.getMaxDamage()))) {
                return failure(ItemStackSnapshotError.LOSSY_COMPONENT_CONVERSION, "invalid damage=" + damage);
            }
            int repairCost = stack.getOrDefault(DataComponents.REPAIR_COST, 0);
            if (repairCost < 0) return failure(ItemStackSnapshotError.LOSSY_COMPONENT_CONVERSION, "invalid repairCost=" + repairCost);
            Unbreakable unbreakable = stack.get(DataComponents.UNBREAKABLE);
            DyedItemColor color = stack.get(DataComponents.DYED_COLOR);
            CustomModelData model = stack.get(DataComponents.CUSTOM_MODEL_DATA);
            CompoundTag customData = copyCustomData(stack);
            return ItemStackSnapshot.create(itemId.toString(), stack.getCount(), name, loreJson,
                    enchantmentValues.orElseThrow(), storedValues.orElseThrow(), shown(enchantments), shown(stored),
                    damage, repairCost, unbreakable != null, unbreakable == null || unbreakable.showInTooltip(),
                    color == null ? OptionalInt.empty() : OptionalInt.of(color.rgb()), color == null || color.showInTooltip(),
                    model == null ? OptionalInt.empty() : OptionalInt.of(model.value()),
                    NeoForge1211NbtAdapter.fromNative(customData == null ? new CompoundTag() : customData));
        } catch (RuntimeException exception) {
            return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, exception.getMessage());
        }
    }

    public ItemStackSnapshotResult<ItemStack> restoreSnapshot(ItemStackSnapshot snapshot, HolderLookup.Provider registries) {
        ItemStackSnapshotResult<ItemStackSnapshot> validation = ItemStackSnapshotValidator.validate(snapshot);
        if (!validation.isSuccess()) return copyFailure(validation);
        ResourceLocation itemId;
        try {
            itemId = ResourceLocation.parse(snapshot.itemId());
        } catch (RuntimeException exception) {
            return failure(ItemStackSnapshotError.UNKNOWN_ITEM_ID, snapshot.itemId());
        }
        if (!BuiltInRegistries.ITEM.containsKey(itemId)) return failure(ItemStackSnapshotError.UNKNOWN_ITEM_ID, itemId.toString());
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (snapshot.count() <= 0 || snapshot.count() > item.getDefaultMaxStackSize()) {
            return failure(ItemStackSnapshotError.INVALID_COUNT, "count=" + snapshot.count() + ", max=" + item.getDefaultMaxStackSize());
        }
        try {
            ItemStack stack = new ItemStack(item, snapshot.count());
            if (snapshot.customNameJson().isPresent()) {
                Component name = Component.Serializer.fromJson(snapshot.customNameJson().orElseThrow(), registries);
                if (name == null) return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, "customName parsed to null");
                stack.set(DataComponents.CUSTOM_NAME, name);
            }
            if (!snapshot.loreJson().isEmpty()) {
                java.util.ArrayList<Component> lines = new java.util.ArrayList<>();
                for (String json : snapshot.loreJson()) {
                    Component line = Component.Serializer.fromJson(json, registries);
                    if (line == null) return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, "lore line parsed to null");
                    lines.add(line);
                }
                stack.set(DataComponents.LORE, new ItemLore(lines));
            }
            ItemStackSnapshotResult<ItemEnchantments> enchantments = restoreEnchantments(snapshot.enchantments(), snapshot.enchantmentsShown(), registries);
            if (!enchantments.isSuccess()) return copyFailure(enchantments);
            if (!snapshot.enchantments().isEmpty() || !snapshot.enchantmentsShown()) stack.set(DataComponents.ENCHANTMENTS, enchantments.orElseThrow());
            ItemStackSnapshotResult<ItemEnchantments> stored = restoreEnchantments(snapshot.storedEnchantments(), snapshot.storedEnchantmentsShown(), registries);
            if (!stored.isSuccess()) return copyFailure(stored);
            if (!snapshot.storedEnchantments().isEmpty() || !snapshot.storedEnchantmentsShown()) stack.set(DataComponents.STORED_ENCHANTMENTS, stored.orElseThrow());
            if (snapshot.damage() != 0) {
                if (stack.getMaxDamage() <= 0 || snapshot.damage() > stack.getMaxDamage()) return failure(ItemStackSnapshotError.LOSSY_COMPONENT_CONVERSION, "damage is not valid for " + itemId);
                stack.set(DataComponents.DAMAGE, snapshot.damage());
            }
            if (snapshot.repairCost() != 0) stack.set(DataComponents.REPAIR_COST, snapshot.repairCost());
            if (snapshot.unbreakable()) stack.set(DataComponents.UNBREAKABLE, new Unbreakable(snapshot.unbreakableShown()));
            if (snapshot.dyedColor().isPresent()) stack.set(DataComponents.DYED_COLOR, new DyedItemColor(snapshot.dyedColor().getAsInt(), snapshot.dyedColorShown()));
            snapshot.customModelData().ifPresent(value -> stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(value)));
            setCustomData(stack, NeoForge1211NbtAdapter.toNative(snapshot.customData()));
            return ItemStackSnapshotResult.success(stack);
        } catch (RuntimeException exception) {
            return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, exception.getMessage());
        }
    }

    private static ItemStackSnapshotResult<Map<String, Integer>> captureEnchantments(ItemEnchantments enchantments) {
        Map<String, Integer> values = new LinkedHashMap<>();
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            Optional<ResourceKey<Enchantment>> key = entry.getKey().unwrapKey();
            if (key.isEmpty()) return failure(ItemStackSnapshotError.LOSSY_COMPONENT_CONVERSION, "unregistered direct enchantment holder");
            if (entry.getIntValue() <= 0) return failure(ItemStackSnapshotError.LOSSY_COMPONENT_CONVERSION, "invalid enchantment level=" + entry.getIntValue());
            values.put(key.orElseThrow().location().toString(), entry.getIntValue());
        }
        return ItemStackSnapshotResult.success(values);
    }

    private static ItemStackSnapshotResult<ItemEnchantments> restoreEnchantments(Map<String, Integer> values, boolean shown, HolderLookup.Provider registries) {
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            ResourceLocation id;
            try { id = ResourceLocation.parse(entry.getKey()); }
            catch (RuntimeException exception) { return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, "invalid enchantment id: " + entry.getKey()); }
            ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, id);
            Optional<Holder.Reference<Enchantment>> holder = registries.lookupOrThrow(Registries.ENCHANTMENT).get(key);
            if (holder.isEmpty()) return failure(ItemStackSnapshotError.LOSSY_COMPONENT_CONVERSION, "unknown enchantment: " + id);
            if (entry.getValue() <= 0) return failure(ItemStackSnapshotError.DATA_PARSE_FAILED, "invalid enchantment level: " + entry.getValue());
            mutable.set(holder.orElseThrow(), entry.getValue());
        }
        return ItemStackSnapshotResult.success(mutable.toImmutable().withTooltip(shown));
    }

    private static boolean shown(ItemEnchantments enchantments) {
        return enchantments.equals(enchantments.withTooltip(true));
    }

    private static String serializeComponent(Component component, HolderLookup.Provider registries) {
        String json = Component.Serializer.toJson(component, registries);
        JsonElement parsed = JsonParser.parseString(json);
        if (!parsed.isJsonPrimitive() || !parsed.getAsJsonPrimitive().isString()) return json;
        JsonObject normalized = new JsonObject();
        normalized.addProperty("text", parsed.getAsString());
        return normalized.toString();
    }

    private static <T> ItemStackSnapshotResult<T> failure(ItemStackSnapshotError error, String detail) {
        return ItemStackSnapshotResult.failure(error, detail);
    }

    private static <T, U> ItemStackSnapshotResult<T> copyFailure(ItemStackSnapshotResult<U> result) {
        return failure(result.error().orElseThrow(), result.detail());
    }

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

    public ItemStack loadSimple(CompoundTag tag) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(tag.getString("id")));
        ItemStack stack = new ItemStack(item, Math.max(1, tag.getInt("count")));
        if (tag.contains("customData")) {
            setCustomData(stack, tag.getCompound("customData"));
        }
        return stack;
    }
}

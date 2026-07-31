package com.mo.economy_system.platform.item;

import net.minecraft.nbt.CompoundTag;

import java.util.List;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/** Loader-neutral, immutable representation of the supported ItemStack state. */
public final class ItemStackSnapshot {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private final String itemId;
    private final int count;
    private final Optional<String> customNameJson;
    private final List<String> loreJson;
    private final Map<String, Integer> enchantments;
    private final Map<String, Integer> storedEnchantments;
    private final boolean enchantmentsShown;
    private final boolean storedEnchantmentsShown;
    private final int damage;
    private final int repairCost;
    private final boolean unbreakable;
    private final boolean unbreakableShown;
    private final OptionalInt dyedColor;
    private final boolean dyedColorShown;
    private final OptionalInt customModelData;
    private final CompoundTag customData;

    public ItemStackSnapshot(String itemId, int count, Optional<String> customNameJson, List<String> loreJson,
                             Map<String, Integer> enchantments, Map<String, Integer> storedEnchantments,
                             boolean enchantmentsShown, boolean storedEnchantmentsShown, int damage, int repairCost,
                             boolean unbreakable, boolean unbreakableShown, OptionalInt dyedColor,
                             boolean dyedColorShown, OptionalInt customModelData, CompoundTag customData) {
        this.itemId = Objects.requireNonNull(itemId, "itemId");
        this.count = count;
        this.customNameJson = Objects.requireNonNull(customNameJson, "customNameJson");
        this.loreJson = List.copyOf(loreJson);
        this.enchantments = Collections.unmodifiableMap(new LinkedHashMap<>(enchantments));
        this.storedEnchantments = Collections.unmodifiableMap(new LinkedHashMap<>(storedEnchantments));
        this.enchantmentsShown = enchantmentsShown;
        this.storedEnchantmentsShown = storedEnchantmentsShown;
        this.damage = damage;
        this.repairCost = repairCost;
        this.unbreakable = unbreakable;
        this.unbreakableShown = unbreakableShown;
        this.dyedColor = Objects.requireNonNull(dyedColor, "dyedColor");
        this.dyedColorShown = dyedColorShown;
        this.customModelData = Objects.requireNonNull(customModelData, "customModelData");
        this.customData = Objects.requireNonNull(customData, "customData").copy();
    }

    public String itemId() { return itemId; }
    public int count() { return count; }
    public Optional<String> customNameJson() { return customNameJson; }
    public List<String> loreJson() { return loreJson; }
    public Map<String, Integer> enchantments() { return enchantments; }
    public Map<String, Integer> storedEnchantments() { return storedEnchantments; }
    public boolean enchantmentsShown() { return enchantmentsShown; }
    public boolean storedEnchantmentsShown() { return storedEnchantmentsShown; }
    public int damage() { return damage; }
    public int repairCost() { return repairCost; }
    public boolean unbreakable() { return unbreakable; }
    public boolean unbreakableShown() { return unbreakableShown; }
    public OptionalInt dyedColor() { return dyedColor; }
    public boolean dyedColorShown() { return dyedColorShown; }
    public OptionalInt customModelData() { return customModelData; }
    public CompoundTag customData() { return customData.copy(); }
}

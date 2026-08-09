package com.mo.economy_system.target.forge1201.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mo.economy_system.EconomyConstants;
import com.mo.economy_system.common.economy.ShopCatalogDefaults;
import com.mo.economy_system.common.economy.ShopItemIdentity;
import com.mo.economy_system.common.economy.ShopPricingPolicy;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.network.ShopItemSnapshot;
import com.mo.economy_system.common.settings.CommonSettingsStore;
import com.mo.economy_system.common.settings.EconomySettings;
import com.mo.economy_system.platform.shop.EconomyShopCatalogBridge;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Reads the shared economy_shop.json format without importing 1.21 APIs. */
public final class Forge1201ShopCatalogBridge implements EconomyShopCatalogBridge {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Set<String> SUPPORTED_121_COMPONENTS = Set.of(
            "minecraft:custom_data",
            "minecraft:damage",
            "minecraft:repair_cost",
            "minecraft:unbreakable",
            "minecraft:custom_model_data",
            "minecraft:custom_name",
            "minecraft:item_name",
            "minecraft:lore",
            "minecraft:enchantments",
            "minecraft:stored_enchantments",
            "minecraft:dyed_color"
    );

    @Override
    public synchronized List<ShopItemSnapshot> snapshot() {
        JsonArray array = readCatalog();
        return array == null ? List.of() : parse(array);
    }

    public ItemStack createItemStack(ShopItemSnapshot item, RegistryAccess registryAccess) {
        try {
            if (!item.itemData().isBlank()) {
                return loadFull121Stack(item, TagParser.parseTag(item.itemData()));
            }
            return loadLegacyStack(item.itemId(), item.nbt());
        } catch (Exception exception) {
            LOGGER.warn(
                    "Cannot create Forge 1.20.1 shop item {} without losing data: {}",
                    item.shopItemId(),
                    exception.getMessage()
            );
            return ItemStack.EMPTY;
        }
    }

    public synchronized ShopItemSnapshot addItemFromStack(
            ItemStack stack, int basePrice, String description, RegistryAccess registryAccess) {
        if (stack == null || stack.isEmpty() || basePrice <= 0) {
            throw new IllegalArgumentException("invalid shop item or base price");
        }
        JsonArray array = readCatalog();
        if (array == null) {
            throw new IllegalStateException("shop catalog is unavailable");
        }
        ItemStack saved = stack.copy();
        saved.setCount(1);
        CompoundTag fullTag = new CompoundTag();
        saved.save(fullTag);
        String itemId = BuiltInRegistries.ITEM.getKey(saved.getItem()).toString();
        ShopItemSnapshot snapshot = new ShopItemSnapshot(
                UUID.randomUUID().toString(), itemId, basePrice, basePrice, basePrice,
                description == null ? "" : description, 1.0D, "", fullTag.toString(),
                0, 0, 0);
        array.add(encode(snapshot));
        if (!writeCatalog(array)) {
            throw new IllegalStateException("shop catalog could not be saved");
        }
        return snapshot;
    }

    @Override
    public synchronized boolean recordPurchase(String shopItemId, int quantity) {
        if (shopItemId == null || shopItemId.isBlank() || quantity <= 0) {
            return false;
        }
        JsonArray array = readCatalog();
        if (array == null) {
            return false;
        }
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            ShopItemSnapshot item = snapshotOf(object);
            if (item == null || !shopItemId.equals(item.shopItemId())) {
                continue;
            }
            applyPricing(object, ShopPricingPolicy.recordPurchase(item, quantity, pricingConfig()));
            return writeCatalog(array);
        }
        return false;
    }

    @Override
    public synchronized boolean refreshPrices() {
        JsonArray array = readCatalog();
        if (array == null) {
            return false;
        }
        ShopPricingPolicy.Config config = pricingConfig();
        ShopPricingPolicy.Mode mode = pricingMode();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            ShopItemSnapshot item = snapshotOf(object);
            if (item != null && item.basePrice() > 0) {
                applyPricing(object, ShopPricingPolicy.adjust(item, config, mode));
            }
        }
        return writeCatalog(array);
    }

    private static ItemStack loadFull121Stack(ShopItemSnapshot item, CompoundTag fullTag) {
        // Accept a native 1.20 stack snapshot when one is already present.
        if (fullTag.contains("Count", Tag.TAG_ANY_NUMERIC) || fullTag.contains("tag", Tag.TAG_COMPOUND)) {
            ItemStack nativeStack = ItemStack.of(fullTag);
            return nativeStack.isEmpty() ? ItemStack.EMPTY : nativeStack;
        }

        String itemId = fullTag.contains("id", Tag.TAG_STRING)
                ? fullTag.getString("id")
                : item.itemId();
        int count = fullTag.contains("count", Tag.TAG_ANY_NUMERIC)
                ? Math.max(1, fullTag.getInt("count"))
                : 1;
        ItemStack stack = createBaseStack(itemId, count);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (!fullTag.contains("components", Tag.TAG_COMPOUND)) {
            return stack;
        }

        CompoundTag components = fullTag.getCompound("components");
        for (String componentId : components.getAllKeys()) {
            if (!SUPPORTED_121_COMPONENTS.contains(componentId)) {
                throw new IllegalArgumentException("unsupported 1.21 component " + componentId);
            }
        }
        apply121Components(stack, components);
        return stack;
    }

    private static ItemStack loadLegacyStack(String itemId, String legacyNbt) throws Exception {
        ItemStack stack = createBaseStack(itemId, 1);
        if (stack.isEmpty() || legacyNbt == null || legacyNbt.isBlank()) {
            return stack;
        }
        stack.setTag(TagParser.parseTag(legacyNbt));
        return stack;
    }

    private static ItemStack createBaseStack(String itemId, int count) {
        ResourceLocation id = new ResourceLocation(itemId);
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        return new ItemStack(item, count);
    }

    private static void apply121Components(ItemStack stack, CompoundTag components) {
        validateComponentTypes(components);
        CompoundTag legacy = stack.getOrCreateTag();
        if (components.contains("minecraft:custom_data", Tag.TAG_COMPOUND)) {
            legacy.merge(components.getCompound("minecraft:custom_data").copy());
        }
        if (components.contains("minecraft:damage", Tag.TAG_ANY_NUMERIC)) {
            legacy.putInt("Damage", components.getInt("minecraft:damage"));
        }
        if (components.contains("minecraft:repair_cost", Tag.TAG_ANY_NUMERIC)) {
            legacy.putInt("RepairCost", components.getInt("minecraft:repair_cost"));
        }
        if (components.contains("minecraft:custom_model_data", Tag.TAG_ANY_NUMERIC)) {
            legacy.putInt("CustomModelData", components.getInt("minecraft:custom_model_data"));
        }
        if (components.contains("minecraft:unbreakable")) {
            legacy.putBoolean("Unbreakable", true);
            CompoundTag value = components.getCompound("minecraft:unbreakable");
            if (value.contains("show_in_tooltip") && !value.getBoolean("show_in_tooltip")) {
                addHideFlag(legacy, 4);
            }
        }

        applyName(legacy, components, "minecraft:custom_name");
        if (!components.contains("minecraft:custom_name")) {
            applyName(legacy, components, "minecraft:item_name");
        }
        applyLore(legacy, components);
        applyEnchantments(legacy, components, "minecraft:enchantments", "Enchantments");
        applyEnchantments(legacy, components, "minecraft:stored_enchantments", "StoredEnchantments");
        applyDyedColor(legacy, components);
    }

    private static void validateComponentTypes(CompoundTag components) {
        requireType(components, "minecraft:custom_data", Tag.TAG_COMPOUND);
        requireNumeric(components, "minecraft:damage");
        requireNumeric(components, "minecraft:repair_cost");
        requireNumeric(components, "minecraft:custom_model_data");
        requireType(components, "minecraft:unbreakable", Tag.TAG_COMPOUND);
        requireType(components, "minecraft:custom_name", Tag.TAG_STRING);
        requireType(components, "minecraft:item_name", Tag.TAG_STRING);
        requireType(components, "minecraft:lore", Tag.TAG_LIST);
        requireType(components, "minecraft:enchantments", Tag.TAG_COMPOUND);
        requireType(components, "minecraft:stored_enchantments", Tag.TAG_COMPOUND);

        String dyedColor = "minecraft:dyed_color";
        if (components.contains(dyedColor)) {
            Tag value = components.get(dyedColor);
            if (!(value instanceof CompoundTag) && value.getId() > Tag.TAG_DOUBLE) {
                throw new IllegalArgumentException("invalid component type for " + dyedColor);
            }
            if (value instanceof CompoundTag colorData
                    && !colorData.contains("rgb", Tag.TAG_ANY_NUMERIC)) {
                throw new IllegalArgumentException("missing numeric rgb in " + dyedColor);
            }
        }
    }

    private static void requireType(CompoundTag components, String key, int type) {
        if (components.contains(key) && !components.contains(key, type)) {
            throw new IllegalArgumentException("invalid component type for " + key);
        }
    }

    private static void requireNumeric(CompoundTag components, String key) {
        if (components.contains(key) && !components.contains(key, Tag.TAG_ANY_NUMERIC)) {
            throw new IllegalArgumentException("invalid component type for " + key);
        }
    }

    private static void applyName(CompoundTag legacy, CompoundTag components, String key) {
        if (!components.contains(key, Tag.TAG_STRING)) {
            return;
        }
        displayTag(legacy).putString("Name", components.getString(key));
    }

    private static void applyLore(CompoundTag legacy, CompoundTag components) {
        if (!components.contains("minecraft:lore", Tag.TAG_LIST)) {
            return;
        }
        ListTag source = (ListTag) components.get("minecraft:lore");
        if (!source.isEmpty() && source.getElementType() != Tag.TAG_STRING) {
            throw new IllegalArgumentException("invalid lore line type");
        }
        ListTag lore = new ListTag();
        for (Tag line : source) {
            lore.add(StringTag.valueOf(line.getAsString()));
        }
        displayTag(legacy).put("Lore", lore);
    }

    private static void applyEnchantments(
            CompoundTag legacy,
            CompoundTag components,
            String componentKey,
            String legacyKey
    ) {
        if (!components.contains(componentKey, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag component = components.getCompound(componentKey);
        if (component.contains("levels") && !component.contains("levels", Tag.TAG_COMPOUND)) {
            throw new IllegalArgumentException("invalid enchantment levels in " + componentKey);
        }
        CompoundTag levels = component.contains("levels", Tag.TAG_COMPOUND)
                ? component.getCompound("levels")
                : component;
        ListTag enchantments = new ListTag();
        for (String enchantmentId : levels.getAllKeys()) {
            if (!levels.contains(enchantmentId, Tag.TAG_ANY_NUMERIC)) {
                continue;
            }
            ResourceLocation id = new ResourceLocation(enchantmentId);
            if (!BuiltInRegistries.ENCHANTMENT.containsKey(id)) {
                throw new IllegalArgumentException("unknown Forge 1.20.1 enchantment " + enchantmentId);
            }
            CompoundTag enchantment = new CompoundTag();
            enchantment.putString("id", enchantmentId);
            int level = Math.max(1, Math.min(Short.MAX_VALUE, levels.getInt(enchantmentId)));
            enchantment.putShort("lvl", (short) level);
            enchantments.add(enchantment);
        }
        legacy.put(legacyKey, enchantments);
        if (component.contains("show_in_tooltip") && !component.getBoolean("show_in_tooltip")) {
            addHideFlag(legacy, 1);
        }
    }

    private static void applyDyedColor(CompoundTag legacy, CompoundTag components) {
        String key = "minecraft:dyed_color";
        if (!components.contains(key)) {
            return;
        }
        Tag value = components.get(key);
        int color;
        if (value instanceof CompoundTag colorData) {
            color = colorData.getInt("rgb");
            if (colorData.contains("show_in_tooltip") && !colorData.getBoolean("show_in_tooltip")) {
                addHideFlag(legacy, 64);
            }
        } else {
            color = components.getInt(key);
        }
        displayTag(legacy).putInt("color", color);
    }

    private static CompoundTag displayTag(CompoundTag legacy) {
        if (!legacy.contains("display", Tag.TAG_COMPOUND)) {
            legacy.put("display", new CompoundTag());
        }
        return legacy.getCompound("display");
    }

    private static void addHideFlag(CompoundTag legacy, int flag) {
        legacy.putInt("HideFlags", legacy.getInt("HideFlags") | flag);
    }

    private static List<ShopItemSnapshot> parse(JsonArray array) {
        int size = Math.min(array.size(), EconomyNetworkLimits.MAX_SHOP_ENTRIES);
        List<ShopItemSnapshot> items = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            JsonElement element = array.get(index);
            if (!element.isJsonObject()) {
                continue;
            }
            ShopItemSnapshot item = snapshotOf(element.getAsJsonObject());
            if (item != null) {
                items.add(item);
            }
        }
        return List.copyOf(items);
    }

    private static ShopItemSnapshot snapshotOf(JsonObject object) {
        String itemId = string(object, "itemId", "");
        if (itemId.isBlank()) {
            return null;
        }
        int basePrice = integer(object, "basePrice", 0);
        int currentPrice = integer(object, "currentPrice", basePrice);
        String shopItemId = string(object, "shopItemId", "");
        shopItemId = ShopItemIdentity.existingOrDeterministic(
                shopItemId, itemId, string(object, "nbt", ""),
                string(object, "itemData", ""));
        return new ShopItemSnapshot(
                shopItemId,
                itemId,
                basePrice,
                currentPrice,
                integer(object, "lastPrice", currentPrice),
                string(object, "description", ""),
                decimal(object, "fluctuationFactor", 0.0D),
                string(object, "nbt", ""),
                string(object, "itemData", ""),
                integer(object, "recentDemand", 0),
                integer(object, "virtualStock", 0),
                integer(object, "maxVirtualStock", 0)
        );
    }

    private static void applyPricing(JsonObject object, ShopItemSnapshot item) {
        object.addProperty("shopItemId", item.shopItemId());
        object.addProperty("currentPrice", item.currentPrice());
        object.addProperty("lastPrice", item.lastPrice());
        object.addProperty("fluctuationFactor", item.fluctuationFactor());
        object.addProperty("recentDemand", item.recentDemand());
        object.addProperty("virtualStock", item.virtualStock());
        object.addProperty("maxVirtualStock", item.maxVirtualStock());
    }

    private static JsonObject encode(ShopItemSnapshot item) {
        JsonObject object = new JsonObject();
        object.addProperty("shopItemId", item.shopItemId());
        object.addProperty("itemId", item.itemId());
        object.addProperty("basePrice", item.basePrice());
        object.addProperty("currentPrice", item.currentPrice());
        object.addProperty("lastPrice", item.lastPrice());
        object.addProperty("description", item.description());
        object.addProperty("fluctuationFactor", item.fluctuationFactor());
        object.addProperty("nbt", item.nbt());
        object.addProperty("itemData", item.itemData());
        object.addProperty("recentDemand", item.recentDemand());
        object.addProperty("virtualStock", item.virtualStock());
        object.addProperty("maxVirtualStock", item.maxVirtualStock());
        return object;
    }

    private static JsonArray defaultCatalog() {
        JsonArray array = new JsonArray();
        ShopPricingPolicy.Config config = pricingConfig();
        for (ShopItemSnapshot item : ShopCatalogDefaults.snapshots()) {
            array.add(encode(ShopPricingPolicy.initialize(item, config)));
        }
        return array;
    }

    private static ShopPricingPolicy.Config pricingConfig() {
        ShopPricingPolicy.Config defaults = ShopPricingPolicy.Config.defaults();
        JsonObject object = readObject(auxiliaryConfigPath("shop_pricing.json"));
        if (object == null) {
            return defaults;
        }
        try {
            return new ShopPricingPolicy.Config(
                    decimal(object, "minPriceMultiplier", defaults.minPriceMultiplier()),
                    decimal(object, "maxPriceMultiplier", defaults.maxPriceMultiplier()),
                    decimal(object, "maxCycleChangeRate", defaults.maxCycleChangeRate()),
                    decimal(object, "demandSensitivity", defaults.demandSensitivity()),
                    decimal(object, "demandDecay", defaults.demandDecay()),
                    decimal(object, "idleReturnRate", defaults.idleReturnRate()),
                    integer(object, "defaultMaxStock", defaults.defaultMaxStock()),
                    integer(object, "minMaxStock", defaults.minMaxStock()),
                    decimal(object, "restockRate", defaults.restockRate()),
                    decimal(object, "stockSensitivity", defaults.stockSensitivity())
            );
        } catch (RuntimeException exception) {
            LOGGER.warn("Ignoring invalid Forge shop pricing configuration", exception);
            return defaults;
        }
    }

    private static ShopPricingPolicy.Mode pricingMode() {
        return ShopPricingPolicy.Mode.parse(
                EconomySettings.get(CommonSettingsStore.SHOP_PRICING_MODE));
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get()
                .resolve(EconomyConstants.MOD_ID)
                .resolve("economy_shop.json");
    }

    private static Path auxiliaryConfigPath(String fileName) {
        return FMLPaths.CONFIGDIR.get().resolve(EconomyConstants.MOD_ID).resolve(fileName);
    }

    private static JsonObject readObject(Path config) {
        if (!Files.isRegularFile(config)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(config, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            return root.isJsonObject() ? root.getAsJsonObject() : null;
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Ignoring invalid configuration {}", config, exception);
            return null;
        }
    }

    private static JsonArray readCatalog() {
        Path config = configPath();
        if (!Files.isRegularFile(config)) {
            JsonArray defaults = defaultCatalog();
            return writeCatalog(defaults) ? defaults : null;
        }
        try (Reader reader = Files.newBufferedReader(config, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonArray()) {
                LOGGER.warn("Ignoring shop catalog {} because its root is not an array", config);
                return null;
            }
            return root.getAsJsonArray();
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to read shop catalog {}", config, exception);
            return null;
        }
    }

    private static boolean writeCatalog(JsonArray array) {
        Path config = configPath();
        Path parent = config.getParent();
        Path temporary = null;
        try {
            Files.createDirectories(parent);
            temporary = Files.createTempFile(parent, "economy_shop", ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(array, writer);
            }
            try {
                Files.move(
                        temporary,
                        config,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, config, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            LOGGER.error("Failed to save shop catalog {}", config, exception);
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException cleanupException) {
                    LOGGER.warn("Failed to remove temporary shop catalog {}", temporary, cleanupException);
                }
            }
            return false;
        }
    }

    private static String string(JsonObject object, String name, String fallback) {
        JsonElement value = object.get(name);
        return value == null || value.isJsonNull() ? fallback : value.getAsString();
    }

    private static int integer(JsonObject object, String name, int fallback) {
        JsonElement value = object.get(name);
        return value == null || value.isJsonNull() ? fallback : value.getAsInt();
    }

    private static double decimal(JsonObject object, String name, double fallback) {
        JsonElement value = object.get(name);
        return value == null || value.isJsonNull() ? fallback : value.getAsDouble();
    }
}

package com.mo.economy_system.datagen;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.item.EconomySystem_Items;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.LinkedHashMap;

public class ModItemModelProvider extends ItemModelProvider {
    private static LinkedHashMap<ResourceKey<TrimMaterial>, Float> trimMaterials = new LinkedHashMap<>();
    static {
        trimMaterials.put(TrimMaterials.QUARTZ, 0.1f);
        trimMaterials.put(TrimMaterials.IRON, 0.2f);
        trimMaterials.put(TrimMaterials.NETHERITE, 0.3f);
        trimMaterials.put(TrimMaterials.REDSTONE, 0.4f);
        trimMaterials.put(TrimMaterials.COPPER, 0.5f);
        trimMaterials.put(TrimMaterials.GOLD, 0.6f);
        trimMaterials.put(TrimMaterials.EMERALD, 0.7f);
        trimMaterials.put(TrimMaterials.DIAMOND, 0.8f);
        trimMaterials.put(TrimMaterials.LAPIS, 0.9f);
        trimMaterials.put(TrimMaterials.AMETHYST, 1.0f);
    }

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, EconomySystem.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        simpleTool(EconomySystem_Items.CLAIM_WAND);
        simpleItem(EconomySystem_Items.GUITAR);
        simpleItem(EconomySystem_Items.RECALL_POTION);
        simpleItem(EconomySystem_Items.WORMHOLE_POTION);
        builtinEntityItem(EconomySystem_Items.PLAYER_DOLL_HAT);
        builtinEntityItem(EconomySystem_Items.POXIAOJIN_DOLL_HAT);
        builtinEntityItem(EconomySystem_Items.HANHANYU_DOLL_HAT);
        builtinEntityItem(EconomySystem_Items.PLAYER_351987654321_DOLL_HAT);
        builtinEntityItem(EconomySystem_Items.BIANNUALCLAMP68_DOLL_HAT);
        trimmedArmorItem(EconomySystem_Items.SUPPORTER_HAT);
    }

    private ItemModelBuilder simpleItem(DeferredHolder<Item, ? extends Item> itemRegistryObject) {
        return withExistingParent(itemRegistryObject.getId().getPath(),
                mcLoc("item/generated")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(EconomySystem.MODID, "item/" + itemRegistryObject.getId().getPath()));
    }

    private ItemModelBuilder simpleTool(DeferredHolder<Item, ? extends Item> itemRegistryObject) {
        return withExistingParent(itemRegistryObject.getId().getPath(),
                mcLoc("item/handheld")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(EconomySystem.MODID, "item/" + itemRegistryObject.getId().getPath()));
    }

    private ItemModelBuilder builtinEntityItem(DeferredHolder<Item, ? extends Item> itemRegistryObject) {
        return getBuilder(itemRegistryObject.getId().getPath())
                .parent(new ModelFile.UncheckedModelFile(mcLoc("builtin/entity")));
    }

    private void trimmedArmorItem(DeferredHolder<Item, ? extends Item> itemRegistryObject) {
        final String MOD_ID = EconomySystem.MODID;

        if (itemRegistryObject.get() instanceof ArmorItem armorItem) {
            trimMaterials.entrySet().forEach(entry -> {
                ResourceKey<TrimMaterial> trimMaterialResourceKey = entry.getKey();
                float trimValue = entry.getValue();

                String armorType = switch (armorItem.getEquipmentSlot()) {
                    case HEAD -> "helmet";
                    case CHEST -> "chestplate";
                    case LEGS -> "leggings";
                    case FEET -> "boots";
                    default -> "";
                };

                String armorItemPath = "item/" + itemRegistryObject.getId().getPath();
                String trimPath = "trims/items/" + armorType + "_trim_" + trimMaterialResourceKey.location().getPath();
                String currentTrimName = armorItemPath + "_" + trimMaterialResourceKey.location().getPath() + "_trim";
                ResourceLocation armorItemResLoc = ResourceLocation.fromNamespaceAndPath(MOD_ID, armorItemPath);
                ResourceLocation trimResLoc = ResourceLocation.parse(trimPath);
                ResourceLocation trimNameResLoc = ResourceLocation.fromNamespaceAndPath(MOD_ID, currentTrimName);

                existingFileHelper.trackGenerated(trimResLoc, PackType.CLIENT_RESOURCES, ".png", "textures");

                getBuilder(currentTrimName)
                        .parent(new ModelFile.UncheckedModelFile("item/generated"))
                        .texture("layer0", armorItemResLoc)
                        .texture("layer1", trimResLoc);

                this.withExistingParent(itemRegistryObject.getId().getPath(), mcLoc("item/generated"))
                        .override()
                        .model(new ModelFile.UncheckedModelFile(trimNameResLoc))
                        .predicate(mcLoc("trim_type"), trimValue).end()
                        .texture("layer0", ResourceLocation.fromNamespaceAndPath(MOD_ID, "item/" + itemRegistryObject.getId().getPath()));
            });
        }
    }
}

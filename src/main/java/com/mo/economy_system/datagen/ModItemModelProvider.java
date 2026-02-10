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
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;

public class ModItemModelProvider extends ItemModelProvider {
    private static LinkedHashMap<ResourceKey<TrimMaterial>, Float> trimMaterials = new LinkedHashMap<>();
    static {
        trimMaterials.put(TrimMaterials.QUARTZ, 0.1f);
        trimMaterials.put(TrimMaterials.IRON, 0.1f);
        trimMaterials.put(TrimMaterials.NETHERITE, 0.1f);
        trimMaterials.put(TrimMaterials.REDSTONE, 0.1f);
        trimMaterials.put(TrimMaterials.COPPER, 0.1f);
        trimMaterials.put(TrimMaterials.GOLD, 0.1f);
        trimMaterials.put(TrimMaterials.EMERALD, 0.1f);
        trimMaterials.put(TrimMaterials.DIAMOND, 0.1f);
        trimMaterials.put(TrimMaterials.LAPIS, 0.1f);
        trimMaterials.put(TrimMaterials.AMETHYST, 0.1f);
    }

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, EconomySystem.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        simpleTool(EconomySystem_Items.CLAIM_WAND);
        // simpleItem(EconomySystem_Items.SUPPORTER_HAT);
        simpleItem(EconomySystem_Items.RECALL_POTION);
        simpleItem(EconomySystem_Items.WORMHOLE_POTION);
        simpleItem(EconomySystem_Items.BLUEPRINT_ITEM);
        simpleItem(EconomySystem_Items.EASY_AID_KIT);
        simpleItem(EconomySystem_Items.ADVANCED_AID_KIT);
        simpleItem(EconomySystem_Items.PROFESSIONAL_AID_KIT);
        simpleItem(EconomySystem_Items.DREAMINGFISH);
        // 重生锦鲤使用原版不死图腾纹理
        simpleItem(EconomySystem_Items.REVIVAL_CHARM);
        // 基因复苏药剂
        simpleItem(EconomySystem_Items.GENE_RESURGENCE_POTION);
        // 线索物品
        simpleItem(EconomySystem_Items.CLUE_ITEM);

        trimmedArmorItem(EconomySystem_Items.SUPPORTER_HAT);
    }

    private ItemModelBuilder simpleItem(RegistryObject<Item> itemRegistryObject) {
        return withExistingParent(itemRegistryObject.getId().getPath(),
                new ResourceLocation("item/generated")).texture("layer0",
                new ResourceLocation(EconomySystem.MODID, "item/" + itemRegistryObject.getId().getPath()));
    }

    private ItemModelBuilder simpleTool(RegistryObject<Item> itemRegistryObject) {
        return withExistingParent(itemRegistryObject.getId().getPath(),
                new ResourceLocation("item/handheld")).texture("layer0",
                new ResourceLocation(EconomySystem.MODID, "item/" + itemRegistryObject.getId().getPath()));
    }

    private void trimmedArmorItem(RegistryObject<Item> itemRegistryObject) {
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

                String armorItemPath = "item/" + armorItem;
                String trimPath = "trims/items/" + armorType + "_trim_" + trimMaterialResourceKey.location().getPath();
                String currentTrimName = armorItemPath + "_" + trimMaterialResourceKey.location().getPath() + "_trim";
                ResourceLocation armorItemResLoc = new ResourceLocation(MOD_ID, armorItemPath);
                ResourceLocation trimResLoc = new ResourceLocation(trimPath);
                ResourceLocation trimNameResLoc = new ResourceLocation(MOD_ID, currentTrimName);

                existingFileHelper.trackGenerated(trimResLoc, PackType.CLIENT_RESOURCES, ".png", "textures");

                getBuilder(currentTrimName)
                        .parent(new ModelFile.UncheckedModelFile("item/generated"))
                        .texture("layer0", armorItemResLoc)
                        .texture("layer1", trimResLoc);

                this.withExistingParent(itemRegistryObject.getId().getPath(), mcLoc("item/generated"))
                        .override()
                        .model(new ModelFile.UncheckedModelFile(trimNameResLoc))
                        .predicate(mcLoc("trim_type"), trimValue).end()
                        .texture("layer0", new ResourceLocation(MOD_ID, "item/" + itemRegistryObject.getId().getPath()));
            });
        }
    }
}

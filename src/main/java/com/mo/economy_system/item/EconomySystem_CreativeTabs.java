package com.mo.economy_system.item;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.item.items.BlueprintItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class EconomySystem_CreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EconomySystem.MODID);

    public static final RegistryObject<CreativeModeTab> ECONOMY_SYSTEM_TAB = CREATIVE_TABS.register("economy_system_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.economy_system.tab")) // 物品栏名称
                    .icon(() -> new ItemStack(EconomySystem_Items.CLAIM_WAND.get())) // 设置图标
                    .displayItems((params, output) -> {
                        output.accept(EconomySystem_Items.CLAIM_WAND.get());
                        output.accept(EconomySystem_Items.WORMHOLE_POTION.get());
                        output.accept(EconomySystem_Items.RECALL_POTION.get());
                        output.accept(EconomySystem_Items.SUPPORTER_HAT.get());
                        output.accept(EconomySystem_Items.DREAMINGFISH.get());
                        output.accept(EconomySystem_Items.HIVE_ZOMBIE_SPAWN_EGG.get());
                    })
                    .build()
    );

    public static final RegistryObject<CreativeModeTab> BLUEPRINT_TAB = CREATIVE_TABS.register("blueprint_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.blueprint.tab")) // 物品栏名称
                    .icon(() -> new ItemStack(EconomySystem_Items.BLUEPRINT_ITEM.get())) // 设置图标
                    .displayItems((params, output) -> {
                        output.accept(EconomySystem_Items.BLUEPRINT_ITEM.get());
                        addAllBlueprintItems(output);
                    })
                    .build()
    );

    private static void addAllBlueprintItems(CreativeModeTab.Output output) {
        // 定义需要蓝图解锁的物品列表
        List<String> itemsRequiringBlueprint = new ArrayList<>();

        // 工具类
        itemsRequiringBlueprint.add("minecraft:wooden_pickaxe");
        itemsRequiringBlueprint.add("minecraft:stone_pickaxe");
        itemsRequiringBlueprint.add("minecraft:iron_pickaxe");
        itemsRequiringBlueprint.add("minecraft:golden_pickaxe");
        itemsRequiringBlueprint.add("minecraft:diamond_pickaxe");
        itemsRequiringBlueprint.add("minecraft:netherite_pickaxe");

        // 武器类
        itemsRequiringBlueprint.add("minecraft:wooden_sword");
        itemsRequiringBlueprint.add("minecraft:stone_sword");
        itemsRequiringBlueprint.add("minecraft:iron_sword");
        itemsRequiringBlueprint.add("minecraft:diamond_sword");
        itemsRequiringBlueprint.add("minecraft:netherite_sword");

        // 防具类
        itemsRequiringBlueprint.add("minecraft:leather_helmet");
        itemsRequiringBlueprint.add("minecraft:iron_helmet");
        itemsRequiringBlueprint.add("minecraft:diamond_helmet");
        itemsRequiringBlueprint.add("minecraft:leather_chestplate");
        itemsRequiringBlueprint.add("minecraft:iron_chestplate");
        itemsRequiringBlueprint.add("minecraft:diamond_chestplate");

        // 其他重要物品
        itemsRequiringBlueprint.add("minecraft:chest");
        itemsRequiringBlueprint.add("minecraft:furnace");
        itemsRequiringBlueprint.add("minecraft:enchanting_table");
        itemsRequiringBlueprint.add("minecraft:anvil");

        // 为每个物品创建蓝图
        for (String itemId : itemsRequiringBlueprint) {
            output.accept(BlueprintItem.createBlueprint(itemId));
        }
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}

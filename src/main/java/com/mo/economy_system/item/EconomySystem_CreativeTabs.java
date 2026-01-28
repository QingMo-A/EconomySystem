package com.mo.economy_system.item;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.blueprint_system.PlayerBlueprintData;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

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
                        output.accept(EconomySystem_Items.EASY_AID_KIT.get());
                        output.accept(EconomySystem_Items.ADVANCED_AID_KIT.get());
                        output.accept(EconomySystem_Items.PROFESSIONAL_AID_KIT.get());
                        output.accept(EconomySystem_Items.REVIVAL_CHARM.get());
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
        PlayerBlueprintData.initAllBlueprintItems();
        // 为每个物品创建蓝图
        for (ItemStack stack : PlayerBlueprintData.getAllBlueprintItems()) {
            output.accept(stack);
        }
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}

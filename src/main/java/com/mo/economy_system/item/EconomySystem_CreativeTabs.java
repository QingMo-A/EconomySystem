package com.mo.economy_system.item;

import com.mo.economy_system.EconomySystem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class EconomySystem_CreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EconomySystem.MODID);

    public static final DeferredHolder<CreativeModeTab, ? extends CreativeModeTab> ECONOMY_SYSTEM_TAB = CREATIVE_TABS.register("economy_system_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.economy_system.tab"))
                    .icon(() -> new ItemStack(EconomySystem_Items.CLAIM_WAND.get()))
                    .displayItems((params, output) -> {
                        output.accept(EconomySystem_Items.CLAIM_WAND.get());
                        output.accept(EconomySystem_Items.WORMHOLE_POTION.get());
                        output.accept(EconomySystem_Items.RECALL_POTION.get());
                        output.accept(EconomySystem_Items.GUITAR.get());
                        output.accept(EconomySystem_Items.SUPPORTER_HAT.get());
                        output.accept(EconomySystem_Items.PLAYER_DOLL_HAT.get());
                    })
                    .build()
    );

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}

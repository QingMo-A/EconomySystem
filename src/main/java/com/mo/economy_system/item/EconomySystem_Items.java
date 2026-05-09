package com.mo.economy_system.item;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.armor.EconomySystem_ArmorMaterials;
import com.mo.economy_system.armor.armors.SupporterHat;
import com.mo.economy_system.item.items.Item_ClaimWand;
import com.mo.economy_system.item.items.Item_Guitar;
import com.mo.economy_system.item.items.Potion_Recall;
import com.mo.economy_system.item.items.Potion_Wormhole;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class EconomySystem_Items {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, EconomySystem.MODID);

    public static final DeferredHolder<Item, ? extends Item> GUITAR = ITEMS.register("guitar",
            () -> new Item_Guitar(new Item.Properties()
                    .stacksTo(1)
                    .fireResistant()));

    public static final DeferredHolder<Item, ? extends Item> WORMHOLE_POTION = ITEMS.register("wormhole_potion",
            () -> new Potion_Wormhole(new Item.Properties()
                    .stacksTo(1)
                    .fireResistant()));

    public static final DeferredHolder<Item, ? extends Item> RECALL_POTION = ITEMS.register("recall_potion",
            () -> new Potion_Recall(new Item.Properties()
                    .stacksTo(1)
                    .fireResistant()));

    public static final DeferredHolder<Item, ? extends Item> CLAIM_WAND = ITEMS.register("claim_wand",
            () -> new Item_ClaimWand(new Item.Properties()
                    .stacksTo(1)));

    public static final DeferredHolder<Item, ? extends Item> SUPPORTER_HAT = ITEMS.register("supporter_hat",
            () -> new SupporterHat(EconomySystem_ArmorMaterials.SUPPORTER, ArmorItem.Type.HELMET, new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

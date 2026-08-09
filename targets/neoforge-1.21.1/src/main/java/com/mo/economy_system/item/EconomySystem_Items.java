package com.mo.economy_system.item;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.armor.EconomySystem_ArmorMaterials;
import com.mo.economy_system.armor.armors.SupporterHat;
import com.mo.economy_system.item.items.Item_ClaimWand;
import com.mo.economy_system.item.items.Item_Guitar;
import com.mo.economy_system.item.items.PlayerDollHatItem;
import com.mo.economy_system.item.items.Potion_Recall;
import com.mo.economy_system.item.items.Potion_Wormhole;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;

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

    public static final DeferredHolder<Item, ? extends Item> PLAYER_DOLL_HAT = registerPlayerDollHat(
            "player_doll_hat",
            "dc5eb054-afdc-44d2-9062-9d18dbe3d30c",
            "___QingMo___",
            false
    );

    public static final DeferredHolder<Item, ? extends Item> POXIAOJIN_DOLL_HAT = registerPlayerDollHat(
            "poxiaojin_doll_hat",
            "a08caa8a-2e6a-418d-8bae-4980ddaba41d",
            "poxiaojin",
            false
    );

    public static final DeferredHolder<Item, ? extends Item> HANHANYU_DOLL_HAT = registerPlayerDollHat(
            "hanhanyu_doll_hat",
            "5c728c19-aed5-4143-af08-bc40f5069f98",
            "__HanHanYu__",
            false
    );

    public static final DeferredHolder<Item, ? extends Item> PLAYER_351987654321_DOLL_HAT = registerPlayerDollHat(
            "player_351987654321_doll_hat",
            "e453107e-a998-47b9-a55f-d3aeded19a96",
            "351987654321",
            false
    );

    private static DeferredHolder<Item, ? extends Item> registerPlayerDollHat(String id, String playerUuid, String playerName, boolean slimModel) {
        return ITEMS.register(id,
                () -> new PlayerDollHatItem(
                        EconomySystem_ArmorMaterials.SUPPORTER,
                        ArmorItem.Type.HELMET,
                        new Item.Properties().stacksTo(1),
                        UUID.fromString(playerUuid),
                        playerName,
                        slimModel
                ));
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

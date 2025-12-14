package com.mo.economy_system.item;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.armor.EconomySystem_ArmorMaterials;
import com.mo.economy_system.armor.armors.SupporterHat;
import com.mo.economy_system.entity.EconomySystem_Entities;
import com.mo.economy_system.item.items.Item_ClaimWand;
import com.mo.economy_system.item.items.Item_Guitar;
import com.mo.economy_system.item.items.Potion_Recall;
import com.mo.economy_system.item.items.Potion_Wormhole;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EconomySystem_Items {

    // 创建物品的 DeferredRegister
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, EconomySystem.MODID);

    // 注册吉他物品
    public static final RegistryObject<Item> GUITAR = ITEMS.register("guitar",
            () -> new Item_Guitar(new Item.Properties()
                    .stacksTo(1) // 堆叠限制为 1
                    .fireResistant() // 可选，防火
            ));

    // 注册虫洞药水
    public static final RegistryObject<Item> WORMHOLE_POTION = ITEMS.register("wormhole_potion",
            () -> new Potion_Wormhole(new Item.Properties()
                    .stacksTo(1) // 堆叠数量为1
                    .fireResistant())); // 可选，防火

    // 注册回忆药水
    public static final RegistryObject<Item> RECALL_POTION = ITEMS.register("recall_potion",
            () -> new Potion_Recall(new Item.Properties()
                    .stacksTo(1) // 堆叠数量为1
                    .fireResistant())); // 可选，防火

    // 注册圈地杖
    public static final RegistryObject<Item> CLAIM_WAND = ITEMS.register("claim_wand",
            () -> new Item_ClaimWand(new Item.Properties()
                    .stacksTo(1) // 限制每堆只能有一个
            ));

    // 注册赞助者帽子
    public static final RegistryObject<Item> SUPPORTER_HAT = ITEMS.register("supporter_hat", () -> new SupporterHat(
            EconomySystem_ArmorMaterials.SUPPORTER, ArmorItem.Type.HELMET, new Item.Properties()));

    // 注册启程锦鲤
    public static final RegistryObject<Item> DREAMINGFISH = ITEMS.register(
            "dreamingfish",
            () -> new Item(new Item.Properties()
                    .stacksTo(64)
                    .rarity(Rarity.UNCOMMON))  //金色品质
            {
                // 加自定义描述tooltips
                @Override
                public void appendHoverText(ItemStack stack, Level level,
                                            List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, level, tooltip, flag);
                    // 读语言文件里的tooltip
                    tooltip.add(Component.translatable("item.economy_system.dreamingfish.tooltip"));
                }
            }
    );

    // 注册丧尸刷怪蛋
    public static final RegistryObject<Item> HIVE_ZOMBIE_SPAWN_EGG =
            ITEMS.register("hive_zombie_spawn_egg",
                    () -> new ForgeSpawnEggItem(
                            EconomySystem_Entities.HIVE_ZOMBIE::get,
                            0x3A6238,  // 基础颜色
                            0x7F997D,  // 斑点颜色
                            new Item.Properties()));


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus); // 注册物品
    }
}

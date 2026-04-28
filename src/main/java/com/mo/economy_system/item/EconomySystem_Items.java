package com.mo.economy_system.item;

import net.minecraft.core.registries.BuiltInRegistries;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.armor.EconomySystem_ArmorMaterials;
import com.mo.economy_system.armor.armors.SupporterHat;
import com.mo.economy_system.entity.EconomySystem_Entities;
import com.mo.economy_system.item.items.*;
import com.mo.economy_system.item.items.medicine.Easy_Aid_Kit;
import com.mo.economy_system.item.items.Potion_RestoreUnInfected;
import com.mo.economy_system.item.items.Item_RevivalCharm;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

public class EconomySystem_Items {

    // 创建物品的 DeferredRegister
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, EconomySystem.MODID);

    // 注册吉他物品
    public static final DeferredHolder<Item, ? extends Item> GUITAR = ITEMS.register("guitar",
            () -> new Item_Guitar(new Item.Properties()
                    .stacksTo(1) // 堆叠限制为 1
                    .fireResistant() // 可选，防火
            ));

    // 注册虫洞药水
    public static final DeferredHolder<Item, ? extends Item> WORMHOLE_POTION = ITEMS.register("wormhole_potion",
            () -> new Potion_Wormhole(new Item.Properties()
                    .stacksTo(1) // 堆叠数量为1
                    .fireResistant())); // 可选，防火

    // 注册回忆药水
    public static final DeferredHolder<Item, ? extends Item> RECALL_POTION = ITEMS.register("recall_potion",
            () -> new Potion_Recall(new Item.Properties()
                    .stacksTo(1) // 堆叠数量为1
                    .fireResistant())); // 可选，防火

    // 注册圈地杖
    public static final DeferredHolder<Item, ? extends Item> CLAIM_WAND = ITEMS.register("claim_wand",
            () -> new Item_ClaimWand(new Item.Properties()
                    .stacksTo(1) // 限制每堆只能有一个
            ));

    // 注册赞助者帽子
    public static final DeferredHolder<Item, ? extends Item> SUPPORTER_HAT = ITEMS.register("supporter_hat", () -> new SupporterHat(
            EconomySystem_ArmorMaterials.SUPPORTER, ArmorItem.Type.HELMET, new Item.Properties()));

    // 注册启程锦鲤
    public static final DeferredHolder<Item, ? extends Item> DREAMINGFISH = ITEMS.register(
            "dreamingfish",
            () -> new Item(new Item.Properties()
                    .stacksTo(64)
                    .rarity(Rarity.UNCOMMON))  //金色品质
            {
                // 加自定义描述tooltips
                @Override
                public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                            List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    // 读语言文件里的tooltip
                    tooltip.add(Component.translatable("item.economy_system.dreamingfish.tooltip"));
                }
            }
    );

    // 注册丧尸刷怪蛋
    public static final DeferredHolder<Item, ? extends Item> HIVE_ZOMBIE_SPAWN_EGG =
            ITEMS.register("hive_zombie_spawn_egg",
                    () -> new DeferredSpawnEggItem(
                            EconomySystem_Entities.HIVE_ZOMBIE::get,
                            0x3A6238,  // 基础颜色
                            0x7F997D,  // 斑点颜色
                            new Item.Properties()));

    // 自定义蓝图物品（可选，如果不想用地图）
    public static final DeferredHolder<Item, ? extends Item> BLUEPRINT_ITEM = ITEMS.register("blueprint",
            () -> new Item_Blueprint(new Item.Properties()
                    .stacksTo(1)  // 蓝图只能堆叠1个
                    .fireResistant()  // 防火（重要物品）
            ));

    public static final DeferredHolder<Item, ? extends Item> FRAGMENT_PAGE = ITEMS.register("fragment_page",
            () -> new Item_FragmentPage(new Item.Properties()
                    .stacksTo(64)
                    .rarity(Rarity.UNCOMMON)
            ));

    public static final DeferredHolder<Item, ? extends Item> STORY_BOOK = ITEMS.register("story_book",
            () -> new Item_StoryBook(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.RARE)
            ));

    // 空白蓝图（用于制作特定蓝图）
    public static final DeferredHolder<Item, ? extends Item> BLANK_BLUEPRINT = ITEMS.register("blank_blueprint",
            () -> new Item(new Item.Properties()
                    .stacksTo(64)
            ));

    //药品注册————————————————————————————————————————————————————————————————————————
    // 简易急救包（初级）
    public static final DeferredHolder<Item, ? extends Item> EASY_AID_KIT = ITEMS.register("easy_aid_kit",
            () -> new Item_AidKit(
                    20,      // healInterval: 20刻 = 1秒
                    1.0,     // perHealAmount: 每次治疗1点生命值
                    20,      // durabilityConsumeInterval: 每20刻消耗1耐久
                    1000,    // cooldown: 1000刻 = 50秒冷却
                    100,     // startDelay: 100刻 = 5秒启动延迟
                    "简易急救包" // displayName: 显示名称
            )
    );

    // 高级急救包（中级）
    public static final DeferredHolder<Item, ? extends Item> ADVANCED_AID_KIT = ITEMS.register("advanced_aid_kit",
            () -> new Item_AidKit(
                    15,      // 更短的治疗间隔
                    2.0,     // 每次治疗2点生命值
                    30,      // 更耐用的耐久消耗间隔
                    800,     // 更短的冷却时间
                    80,      // 更短的启动延迟
                    "高级急救包"
            )
    );

    // 专业急救包（高级）
    public static final DeferredHolder<Item, ? extends Item> PROFESSIONAL_AID_KIT = ITEMS.register("professional_aid_kit",
            () -> new Item_AidKit(
                    10,      // 非常短的治疗间隔
                    3.0,     // 每次治疗3点生命值
                    40,      // 非常耐用的耐久消耗间隔
                    600,     // 很短的冷却时间
                    60,      // 很短的启动延迟
                    "专业急救包"
            )
    );

    // 复活护符
    public static final DeferredHolder<Item, ? extends Item> REVIVAL_CHARM = ITEMS.register("revival_charm",
            () -> new Item_RevivalCharm(new Item.Properties()
                    .stacksTo(1)  // 只能堆叠1个
                    .rarity(Rarity.RARE)  // 稀有品质
            ));

    // 基因复苏药剂
    public static final DeferredHolder<Item, ? extends Item> GENE_RESURGENCE_POTION = ITEMS.register("restore_uninfected_potion",
            () -> new Potion_RestoreUnInfected(new Item.Properties()
                    .stacksTo(1)  // 只能堆叠1个
                    .rarity(Rarity.EPIC)  // 史诗品质
            ));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus); // 注册物品
    }
}

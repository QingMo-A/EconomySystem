package com.mo.economy_system.common.economy;

import com.mo.economy_system.common.network.ShopItemSnapshot;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** The 1.21.1 baseline catalog used when no persistent shop file exists. */
public final class ShopCatalogDefaults {
  private static final String[][] COLORS = {
      {"white", "白色"}, {"orange", "橙色"}, {"magenta", "品红色"},
      {"light_blue", "淡蓝色"}, {"yellow", "黄色"}, {"lime", "黄绿色"},
      {"pink", "粉红色"}, {"gray", "灰色"}, {"light_gray", "淡灰色"},
      {"cyan", "青色"}, {"purple", "紫色"}, {"blue", "蓝色"},
      {"brown", "棕色"}, {"green", "绿色"}, {"red", "红色"}, {"black", "黑色"}
  };

  private ShopCatalogDefaults() {}

  public static List<ShopItemSnapshot> snapshots() {
    List<ShopItemSnapshot> items = new ArrayList<>();
    add(items, "economy_system:recall_potion", 5, "回忆药水");
    add(items, "economy_system:wormhole_potion", 10, "虫洞药水");
    add(items, "minecraft:enchanted_book", 100, "细心 I",
        "{StoredEnchantments:[{id:\"economy_system:carefully\", lvl:1}]}");
    add(items, "minecraft:enchanted_book", 200, "赏金猎人 I",
        "{StoredEnchantments:[{id:\"economy_system:bounty_hunter\", lvl:1}]}");

    add(items, "minecraft:dirt", 5, "泥土");
    add(items, "minecraft:grass_block", 5, "草方块");
    add(items, "minecraft:sand", 5, "沙子");
    add(items, "minecraft:stone", 5, "石头");

    add(items, "minecraft:oak_log", 5, "橡木原木");
    add(items, "minecraft:spruce_log", 5, "云杉原木");
    add(items, "minecraft:birch_log", 5, "白桦原木");
    add(items, "minecraft:jungle_log", 5, "丛林原木");
    add(items, "minecraft:acacia_log", 5, "金合欢原木");
    add(items, "minecraft:dark_oak_log", 5, "深色橡木原木");
    add(items, "minecraft:mangrove_log", 5, "红树林原木");
    add(items, "minecraft:cherry_log", 5, "樱花原木");
    add(items, "minecraft:crimson_stem", 5, "绯红菌柄");
    add(items, "minecraft:warped_stem", 5, "诡异菌柄");

    add(items, "minecraft:oak_sapling", 5, "橡树树苗");
    add(items, "minecraft:spruce_sapling", 5, "云杉树苗");
    add(items, "minecraft:birch_sapling", 5, "白桦树苗");
    add(items, "minecraft:jungle_sapling", 5, "丛林树苗");
    add(items, "minecraft:acacia_sapling", 5, "金合欢树苗");
    add(items, "minecraft:dark_oak_sapling", 5, "深色橡树树苗");
    add(items, "minecraft:mangrove_propagule", 5, "红树林胎生苗");
    add(items, "minecraft:cherry_sapling", 5, "樱花树苗");

    add(items, "minecraft:oak_leaves", 5, "橡树树叶");
    add(items, "minecraft:spruce_leaves", 5, "云杉树叶");
    add(items, "minecraft:birch_leaves", 5, "白桦树叶");
    add(items, "minecraft:jungle_leaves", 5, "丛林树叶");
    add(items, "minecraft:acacia_leaves", 5, "金合欢树叶");
    add(items, "minecraft:dark_oak_leaves", 5, "深色橡树树叶");
    add(items, "minecraft:mangrove_leaves", 5, "红树林树叶");
    add(items, "minecraft:cherry_leaves", 5, "樱花树叶");
    add(items, "minecraft:azalea_leaves", 5, "杜鹃树叶");
    add(items, "minecraft:flowering_azalea_leaves", 5, "开花杜鹃树叶");

    add(items, "minecraft:quartz", 5, "下界石英");
    add(items, "minecraft:glowstone", 5, "萤石");
    add(items, "minecraft:redstone", 5, "红石");
    add(items, "minecraft:sea_lantern", 5, "海晶灯");
    add(items, "minecraft:stone_bricks", 5, "石砖");
    add(items, "minecraft:mossy_stone_bricks", 5, "苔石砖");
    add(items, "minecraft:cracked_stone_bricks", 5, "裂纹石砖");
    add(items, "minecraft:chiseled_stone_bricks", 5, "雕纹石砖");
    add(items, "minecraft:smooth_stone", 5, "平滑石头");

    for (String[] color : COLORS) {
      add(items, "minecraft:" + color[0] + "_concrete", 5, color[1] + "混凝土");
    }
    for (String[] color : COLORS) {
      add(items, "minecraft:" + color[0] + "_wool", 5, color[1] + "羊毛");
    }
    return List.copyOf(items);
  }

  private static void add(
      List<ShopItemSnapshot> items, String itemId, int price, String description) {
    add(items, itemId, price, description, "");
  }

  private static void add(
      List<ShopItemSnapshot> items,
      String itemId,
      int price,
      String description,
      String nbt) {
    String identity = itemId + '\n' + description + '\n' + nbt;
    items.add(new ShopItemSnapshot(
        UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)).toString(),
        itemId,
        price,
        price,
        price,
        description,
        1.0D,
        nbt,
        "",
        0,
        0,
        0));
  }
}

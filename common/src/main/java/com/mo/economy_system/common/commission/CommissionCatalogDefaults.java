package com.mo.economy_system.common.commission;

import java.util.List;
import java.util.Map;

/**
 * Small built-in catalog used when no administrator question bank has been configured yet.
 *
 * <p>It intentionally contains conservative, vanilla-only work so a fresh server has useful
 * commissions without making an external config file a hard dependency. Target adapters can
 * replace it atomically when they load administrator-authored JSON.
 */
public final class CommissionCatalogDefaults {
  private CommissionCatalogDefaults() {}

  public static CommissionCatalog create() {
    CommissionRequester quartermaster = new CommissionRequester(
        "quartermaster", "Quartermaster", 1.0D, 1.0D, 3, "common", "EconomySystem");
    CommissionRequester hunter = new CommissionRequester(
        "hunter", "Hunter's Guild", 1.0D, 1.05D, 2, "common", "EconomySystem");
    CommissionTemplate delivery = new CommissionTemplate(
        "vanilla_material_delivery",
        CommissionType.ITEM_DELIVERY,
        "supply_requesters",
        "basic_materials",
        16,
        64,
        16,
        CommissionRewardMode.PER_UNIT,
        2,
        1.0D,
        1.2D,
        60,
        "material",
        "common",
        2L * 60L * 60L * 1000L,
        4L * 60L * 60L * 1000L,
        -1,
        "Deliver {count}x {target} for {requester} (reward {reward})",
        "",
        0,
        0);
    CommissionTemplate hunt = new CommissionTemplate(
        "vanilla_hunt",
        CommissionType.ENTITY_KILL,
        "hunt_requesters",
        "hostile_mobs",
        3,
        12,
        3,
        CommissionRewardMode.PER_UNIT,
        8,
        1.0D,
        1.15D,
        30,
        "combat",
        "common",
        2L * 60L * 60L * 1000L,
        4L * 60L * 60L * 1000L,
        -1,
        "Hunt {count}x {target} for {requester} (reward {reward})",
        "",
        0,
        0);
    return new CommissionCatalog(
        List.of(delivery, hunt),
        Map.of(
            "supply_requesters", List.of(quartermaster),
            "hunt_requesters", List.of(hunter)),
        Map.of(
            "basic_materials", CommissionTargetPool.unweighted("basic_materials", List.of(
                "minecraft:coal", "minecraft:iron_ingot", "minecraft:copper_ingot")),
            "hostile_mobs", CommissionTargetPool.unweighted("hostile_mobs", List.of(
                "minecraft:zombie", "minecraft:skeleton", "minecraft:spider"))),
        PersonalCommissionSettings.defaults());
  }
}

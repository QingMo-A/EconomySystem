package com.mo.economy_system.common.reward;

import java.util.List;

/** The 1.21.1 default reward table, shared by every target. */
public final class RewardDefaults {
  private static final List<RewardEntry> ENTRIES =
      List.of(
          new RewardEntry("minecraft:zombie", 0.1D, 1, 5),
          new RewardEntry("minecraft:skeleton", 0.2D, 2, 8),
          new RewardEntry("minecraft:creeper", 0.15D, 5, 10),
          new RewardEntry("minecraft:spider", 0.18D, 1, 4),
          new RewardEntry("minecraft:witch", 0.25D, 3, 7),
          new RewardEntry("minecraft:enderman", 0.12D, 1, 3),
          new RewardEntry("minecraft:slime", 0.2D, 2, 5),
          new RewardEntry("minecraft:blaze", 0.3D, 2, 6),
          new RewardEntry("minecraft:ghast", 0.15D, 1, 2),
          new RewardEntry("minecraft:magma_cube", 0.2D, 3, 6),
          new RewardEntry("minecraft:phantom", 0.1D, 1, 2),
          new RewardEntry("minecraft:piglin", 0.15D, 1, 4),
          new RewardEntry("minecraft:piglin_brute", 0.2D, 2, 6),
          new RewardEntry("minecraft:hoglin", 0.18D, 1, 3),
          new RewardEntry("minecraft:zombified_piglin", 0.1D, 1, 4),
          new RewardEntry("minecraft:vindicator", 0.2D, 2, 5),
          new RewardEntry("minecraft:evoker", 0.25D, 3, 8),
          new RewardEntry("minecraft:illusioner", 0.25D, 2, 6),
          new RewardEntry("minecraft:pillager", 0.15D, 1, 4),
          new RewardEntry("minecraft:ravager", 0.35D, 5, 12),
          new RewardEntry("minecraft:drowned", 0.2D, 1, 4),
          new RewardEntry("minecraft:guardian", 0.2D, 2, 5),
          new RewardEntry("minecraft:elder_guardian", 0.3D, 4, 8),
          new RewardEntry("minecraft:shulker", 0.2D, 2, 4),
          new RewardEntry("minecraft:wither_skeleton", 0.22D, 1, 3),
          new RewardEntry("minecraft:wither", 0.5D, 1, 1),
          new RewardEntry("minecraft:ender_dragon", 1.0D, 1, 1));

  private RewardDefaults() {}

  public static List<RewardEntry> entries() {
    return ENTRIES;
  }
}

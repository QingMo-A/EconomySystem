package com.mo.economy_system.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Repository-level gates enforced by both target test suites. */
class MultiversionArchitectureGateTest {
  @Test
  void targetsDetachRootSourcesAndExcludeReferenceScreens() throws Exception {
    Path root = repositoryRoot();
    String forge = read(root.resolve("targets/forge-1.20.1/build.gradle"));
    String neoForge = read(root.resolve("targets/neoforge-1.21.1/build.gradle"));
    for (String source : List.of(forge, neoForge)) {
      assertTrue(source.contains("rootProject.file('common/src/main/java')"));
      assertTrue(source.contains("java.exclude('com/mo/economy_system/screen/**')"));
      assertFalse(source.contains("rootProject.file('src/main/java')"));
    }
  }

  @Test
  void commonDoesNotReferenceMinecraftOrLoaderApis() throws Exception {
    Path common = repositoryRoot().resolve("common/src/main/java");
    try (Stream<Path> files = Files.walk(common)) {
      for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
        String source = read(file);
        assertFalse(source.contains("net.minecraft."), file.toString());
        assertFalse(source.contains("net.minecraftforge."), file.toString());
        assertFalse(source.contains("net.neoforged."), file.toString());
        if (file.toString().replace('\\', '/').contains("/ui/")) {
          assertFalse(source.contains("net.minecraft.client.gui.screens.Screen"), file.toString());
          assertFalse(source.contains("extends Screen"), file.toString());
        }
      }
    }
  }

  @Test
  void commonBusinessPoliciesAndUiCoreDoNotImportMinecraftApis() throws Exception {
    Path root = repositoryRoot();
    Path common = root.resolve("common/src/main/java/com/mo/economy_system/common");
    Path ui = root.resolve("common/src/main/java/com/mo/economy_system/ui");
    for (Path directory : List.of(common, ui)) {
      try (Stream<Path> files = Files.walk(directory)) {
        for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
          String name = file.getFileName().toString();
          if (!directory.equals(ui)
              && !name.matches(".*(Service|Policy|Controller|Ledger|Rules|Schedule|Runtime|Transaction).*\\.java")) {
            continue;
          }
          String source = read(file);
          assertFalse(source.contains("net.minecraft."), file.toString());
          assertFalse(source.contains("net.minecraftforge."), file.toString());
          assertFalse(source.contains("net.neoforged."), file.toString());
        }
      }
    }
  }

  @Test
  void activeFileAndTransferScreensUseCommonUiContracts() throws Exception {
    Path root = repositoryRoot();
    for (String relative : List.of(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/network/Forge1201ClientFileCheckScreens.java",
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client/Screen_ClientFileCheckConsent.java",
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client/Screen_ClientFileCheckResult.java")) {
      assertTrue(read(root.resolve(relative)).contains("com.mo.economy_system.ui.check"), relative);
    }
    for (String relative : List.of(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/network/Forge1201CheckedFileTransferConsentScreen.java",
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/network/Forge1201CheckedFileTransferResultScreen.java",
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client/Screen_CheckedFileTransferConsent.java",
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client/Screen_CheckedFileTransferResult.java")) {
      String source = read(root.resolve(relative));
      assertTrue(source.contains("com.mo.economy_system.ui.transfer"), relative);
      assertFalse(source.contains("CheckedFileTransferLayout"), relative);
    }
  }

  @Test
  void targetResizeAdaptersDelegateTransactionPolicyToCommon() throws Exception {
    Path root = repositoryRoot();
    String neo = read(root.resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/core/territory_system/TerritoryResizeTransactionService.java"));
    String forge = read(root.resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/network/Forge1201TerritoryResizeTransaction.java"));
    assertTrue(neo.contains("common.territory.TerritoryResizeTransactionService.execute"));
    assertTrue(forge.contains("TerritoryResizeTransactionService.execute"));
    for (String source : List.of(neo, forge)) {
      assertFalse(source.contains("Math.multiplyExact"));
      assertFalse(source.contains("payment-refund"));
    }
  }

  @Test
  void targetTerritoryStoresDelegateResizePlanningPolicyToCommon() throws Exception {
    Path root = repositoryRoot();
    String planner = read(root.resolve(
        "common/src/main/java/com/mo/economy_system/common/territory/TerritoryResizePlanner.java"));
    assertTrue(planner.contains("class TerritoryResizePlanner"));
    assertTrue(planner.contains("TerritoryPricing.expansionCharge"));
    assertTrue(planner.contains("overlapsOther"));
    for (String relative : List.of(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/network/Forge1201TerritorySnapshotStore.java",
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/core/territory_system/TerritoryManager.java")) {
      String source = read(root.resolve(relative));
      assertTrue(source.contains("TerritoryResizePlanner"), relative);
      assertFalse(source.contains("Math.multiplyExact"), relative);
      assertFalse(source.contains("30_000_000L"), relative);
      assertFalse(source.contains("difference, 20L"), relative);
      assertFalse(source.contains("TerritoryPricing.expansionCharge"), relative);
    }
  }

  @Test
  void territoryCreationAdministrationBackpointAndBuffPoliciesHaveOneCommonOwner() throws Exception {
    Path root = repositoryRoot();
    String creationPolicy = read(root.resolve(
        "common/src/main/java/com/mo/economy_system/common/territory/TerritoryClaimCreationPolicy.java"));
    String administrationPolicy = read(root.resolve(
        "common/src/main/java/com/mo/economy_system/common/territory/TerritoryAdministrationService.java"));
    String backpointPolicy = read(root.resolve(
        "common/src/main/java/com/mo/economy_system/common/territory/TerritoryBackpointService.java"));
    String buffPolicy = read(root.resolve(
        "common/src/main/java/com/mo/economy_system/common/territory/TerritoryBuffTransactionService.java"));

    assertTrue(creationPolicy.contains("class TerritoryClaimCreationPolicy"));
    assertTrue(administrationPolicy.contains("isValidReplacement"));
    assertTrue(backpointPolicy.contains("class TerritoryBackpointService"));
    assertTrue(buffPolicy.contains("class TerritoryBuffTransactionService"));

    String forgeStore = read(root.resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/network/Forge1201TerritorySnapshotStore.java"));
    String forgeHandlers = read(root.resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/network/Forge1201TerritoryManagementHandlers.java"));
    String forgeCommands = read(root.resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/Forge1201TerritoryCommands.java"));
    String neoClaim = read(root.resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/territory/NeoForge1211TerritoryClaimRuntime.java"));
    String neoManager = read(root.resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/core/territory_system/TerritoryManager.java"));
    String neoHandlers = read(root.resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/protocol/NeoForge1211TerritoryManagementHandlers.java"));
    String neoCommands = read(root.resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/commands/territory_system/Command_Territory.java"));

    assertTrue(forgeStore.contains("TerritoryClaimCreationPolicy.create"));
    assertTrue(neoClaim.contains("TerritoryClaimCreationPolicy.create"));
    assertTrue(forgeCommands.contains("TerritoryBackpointService.execute"));
    assertTrue(neoCommands.contains("TerritoryBackpointService.execute"));
    for (String source : List.of(forgeHandlers, neoHandlers)) {
      assertTrue(source.contains("TerritoryAdministrationService.permission"), source);
      assertTrue(source.contains("TerritoryAdministrationService.transfer"), source);
      assertTrue(source.contains("TerritoryAdministrationService.rule"), source);
      assertTrue(source.contains("TerritoryBuffTransactionService.execute"), source);
    }
    assertTrue(forgeStore.contains("applyAdministration"));
    assertTrue(neoManager.contains("applyTerritoryAdministrationAuthoritatively"));
    for (String source : List.of(forgeStore, neoManager)) {
      assertFalse(source.contains("setTerritoryPermissionAuthoritatively"), source);
      assertFalse(source.contains("transferTerritoryAuthoritatively"), source);
      assertFalse(source.contains("setTerritoryRuleAuthoritatively"), source);
    }

    String nativeTerritory = read(root.resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/core/territory_system/Territory.java"));
    String nativeBuff = read(root.resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/core/territory_system/TerritoryBuff.java"));
    String nativePermission = read(root.resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/core/territory_system/TerritoryPermissionLevel.java"));
    assertFalse(nativeTerritory.contains("canPerform("));
    assertFalse(nativeBuff.contains("boolean upgrade("));
    assertFalse(nativeBuff.contains("void unlock("));
    assertFalse(nativePermission.contains("boolean allows("));
    assertFalse(nativePermission.contains("TerritoryPermissionLevel next("));
  }

  @Test
  void recallPotionTransactionsHaveOneCommonPolicy() throws Exception {
    Path root = repositoryRoot();
    String service = read(root.resolve(
        "common/src/main/java/com/mo/economy_system/common/territory/RecallPotionUseService.java"));
    assertTrue(service.contains("TELEPORT_STATE_UNKNOWN"));
    assertTrue(service.contains("consumesItem"));
    for (String relative : List.of(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/item/Forge1201RecallPotion.java",
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/item/items/Potion_Recall.java")) {
      String source = read(root.resolve(relative));
      assertTrue(source.contains("RecallPotionUseService.execute"), relative);
    }
  }

  @Test
  void territoryRuntimePolicyAndPresenceStateAreCommonOnBothTargets() throws Exception {
    Path root = repositoryRoot();
    String policy = read(root.resolve(
        "common/src/main/java/com/mo/economy_system/common/territory/TerritoryRuntimePolicy.java"));
    String presence = read(root.resolve(
        "common/src/main/java/com/mo/economy_system/common/territory/TerritoryPresenceService.java"));
    String neo = read(root.resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/events/territory_system/EventHandler_Player.java"));
    String forge = read(root.resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/Forge1201TerritoryEvents.java"));

    assertTrue(policy.contains("effectAmplifier"));
    assertTrue(policy.contains("configuredLevel - 1"));
    assertTrue(presence.contains("class TerritoryPresenceService"));
    for (String source : List.of(policy, presence)) {
      assertFalse(source.contains("net.minecraft."), source);
      assertFalse(source.contains("net.minecraftforge."), source);
      assertFalse(source.contains("net.neoforged."), source);
    }
    for (String source : List.of(neo, forge)) {
      assertTrue(source.contains("TerritoryRuntimePolicy"), source);
      assertTrue(source.contains("TerritoryPresenceService"), source);
      assertTrue(source.contains("EntityPlaceEvent"), source);
      assertTrue(source.contains("RightClickItem"), source);
      assertTrue(source.contains("RightClickBlock"), source);
      assertFalse(source.contains("System.currentTimeMillis"), source);
      assertFalse(source.contains("amplifier = buff.getLevel"), source);
      assertFalse(source.contains("territory.canPerform"), source);
    }

    String neoSelection = read(root.resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/territory/NeoForge1211TerritorySelectionRuntime.java"));
    String forgeClaim = read(root.resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/network/Forge1201TerritoryClaimSessions.java"));
    String forgeResize = read(root.resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/network/Forge1201TerritoryModifySessions.java"));
    for (String source : List.of(neoSelection, forgeClaim, forgeResize)) {
      assertTrue(source.contains("showSelectionBoundary"), source);
    }
    String neoWand = read(root.resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/item/items/Item_ClaimWand.java"));
    assertFalse(neoWand.contains("getModifyVolume"));
    assertFalse(neoWand.contains("TerritoryManager"));
  }

  @Test
  void futureTargetSkeletonContainsOnlyAdapterContracts() throws Exception {
    Path skeleton = repositoryRoot().resolve("targets/future-target-skeleton");
    assertTrue(Files.isRegularFile(skeleton.resolve("README.md")));
    assertTrue(Files.isRegularFile(skeleton.resolve("build.gradle.template")));
    try (Stream<Path> files = Files.walk(skeleton.resolve("src/main/java"))) {
      for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
        String source = read(file);
        assertFalse(source.contains("com.mo.economy_system.common."), file.toString());
        assertFalse(source.contains("Layout"), file.toString());
        assertFalse(source.contains("Controller"), file.toString());
      }
    }
  }

  @Test
  void activeTargetScreensDelegateUiSemanticsToCommonContracts() throws Exception {
    Path root = repositoryRoot();
    for (String relative : List.of(
        "targets/forge-1.20.1/src/main/java",
        "targets/neoforge-1.21.1/src/main/java")) {
      Path sourceRoot = root.resolve(relative);
      List<Path> screenFiles = activeScreenFiles(sourceRoot);
      int screenClassCount = 0;
      for (Path file : screenFiles) {
        String source = read(file);
        int classes = countMatches(source, "\\bextends\\s+Screen\\b");
        screenClassCount += classes;
        assertTrue(source.contains("com.mo.economy_system.ui."), file.toString());
        assertEquals(classes,
            countMatches(source, "[A-Za-z0-9_$.]*View\\.render\\s*\\("),
            file + " must render every Screen through one common semantic View");
        assertTrue(
            countMatches(source, "new\\s+[A-Za-z0-9_$.]*Controller\\s*\\(") >= classes,
            file + " must construct a common controller for every Screen");
        assertTrue(
            countMatches(source, "[A-Za-z0-9_$.]*Layout\\.[A-Za-z0-9_$]+\\s*\\(") >= classes,
            file + " must calculate layout through common");
        assertFalse(
            Pattern.compile(
                    "\\.(?:fill|fillGradient|drawString|drawCenteredString|blit|renderItem|"
                        + "renderFakeItem|renderItemDecorations|renderTooltip|renderComponentTooltip)\\s*\\(")
                .matcher(source)
                .find(),
            file + " must not draw UI semantics directly");
        assertFalse(
            Pattern.compile("\\b0x[0-9A-Fa-f]{6,8}\\b").matcher(source).find(),
            file + " must not own theme color literals");
      }
      assertEquals(23, screenClassCount, relative + " active Screen inventory changed");
    }
  }

  @Test
  void shopPricingAndMarketPagingRulesHaveOneCommonImplementation() throws Exception {
    Path root = repositoryRoot();
    String policy = read(root.resolve("common/src/main/java/com/mo/economy_system/common/economy/ShopPricingPolicy.java"));
    String defaults = read(root.resolve("common/src/main/java/com/mo/economy_system/common/economy/ShopCatalogDefaults.java"));
    String schedule = read(root.resolve("common/src/main/java/com/mo/economy_system/common/economy/ShopPriceRefreshSchedule.java"));
    String marketController = read(root.resolve("common/src/main/java/com/mo/economy_system/ui/market/MarketController.java"));
    String marketLayout = read(root.resolve("common/src/main/java/com/mo/economy_system/ui/market/MarketLayout.java"));
    String neoShop = read(root.resolve("targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/core/economy_system/shop/ShopManager.java"));
    String forgeShop = read(root.resolve("targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/shop/Forge1201ShopCatalogBridge.java"));
    String neoEvent = read(root.resolve("targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/events/EconomySystem_EventHandler.java"));
    String forgeEvent = read(root.resolve("targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/Forge1201ServerEvents.java"));

    assertTrue(policy.contains("class ShopPricingPolicy"));
    assertTrue(defaults.contains("class ShopCatalogDefaults"));
    assertTrue(schedule.contains("class ShopPriceRefreshSchedule"));
    assertTrue(marketController.contains("EconomyNetworkLimits.MAX_MARKET_PAGE_SIZE"));
    assertTrue(marketLayout.contains("EconomyNetworkLimits.MAX_MARKET_PAGE_SIZE"));
    assertFalse(marketController.contains("NETWORK_PAGE_SIZE = 20"));
    for (String source : List.of(neoShop, forgeShop, neoEvent, forgeEvent)) {
      assertTrue(source.contains("ShopPricingPolicy") || source.contains("ShopPriceRefreshSchedule"), source);
    }
    for (String source : List.of(neoShop, forgeShop)) {
      assertTrue(source.contains("ShopCatalogDefaults"), source);
    }
    String identity = read(root.resolve(
        "common/src/main/java/com/mo/economy_system/common/economy/ShopItemIdentity.java"));
    String neoShopItem = read(root.resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/core/economy_system/shop/ShopItem.java"));
    assertTrue(identity.contains("nameUUIDFromBytes"));
    assertTrue(forgeShop.contains("ShopItemIdentity.existingOrDeterministic"));
    assertTrue(neoShopItem.contains("ShopItemIdentity.existingOrDeterministic"));
    assertFalse(neoShop.contains("items.add(new ShopItem"));
    assertTrue(forgeShop.contains("return writeCatalog(defaults) ? defaults : null"));
    assertFalse(neoShop.contains("Math.log1p"));
    assertFalse(neoShop.contains("Math.pow"));
    assertFalse(forgeShop.contains("recentDemand") && forgeShop.contains("+ quantity"));
  }

  @Test
  void redPacketPolicyHasOneCommonImplementation() throws Exception {
    Path root = repositoryRoot();
    Path commonRoot = root.resolve("common/src/main/java/com/mo/economy_system/common/redpacket");
    try (Stream<Path> files = Files.walk(commonRoot)) {
      for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
        String source = read(file);
        assertFalse(source.contains("net.minecraft."), file.toString());
        assertFalse(source.contains("net.minecraftforge."), file.toString());
        assertFalse(source.contains("net.neoforged."), file.toString());
      }
    }

    assertFalse(Files.exists(root.resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/core/economy_system/red_packet/RedPacketManager.java")));
    assertFalse(Files.exists(root.resolve(
        "common/src/main/java/com/mo/economy_system/core/economy_system/red_packet/RedPacket.java")));

    String neoCommand = read(root.resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/commands/economy_system/Command_RedPacket.java"));
    String forgeCommand = read(root.resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/redpacket/Forge1201RedPacketCommands.java"));
    for (String source : List.of(neoCommand, forgeCommand)) {
      assertTrue(source.contains("RedPacketRuntime.service"), source);
      assertFalse(source.contains("claimedAmount +="), source);
      assertFalse(source.contains("nextInt(remainingAmount"), source);
      assertFalse(source.contains("EconomySavedData"), source);
    }

    String neoEvent = read(root.resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/events/EconomySystem_EventHandler.java"));
    String forgeEvent = read(root.resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/Forge1201ServerEvents.java"));
    assertEquals(2, List.of(neoEvent, forgeEvent).stream()
        .filter(source -> source.contains("RedPacketRuntime.expire")).count());
    assertFalse(neoEvent.contains("RedPacketManager"));

    for (String relative : List.of(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/redpacket/Forge1201RedPacketSavedData.java",
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/redpacket/NeoForge1211RedPacketSavedData.java")) {
      String source = read(root.resolve(relative));
      int dirty = source.indexOf("setDirty();");
      int replacement = source.indexOf("packets = replacement;");
      assertTrue(dirty >= 0 && replacement > dirty,
          relative + " must fail before replacing authoritative packet state");
    }
  }

  @Test
  void mobRewardPolicyHasOneCommonImplementation() throws Exception {
    Path root = repositoryRoot();
    Path commonRoot = root.resolve("common/src/main/java/com/mo/economy_system/common/reward");
    try (Stream<Path> files = Files.walk(commonRoot)) {
      for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
        String source = read(file);
        assertFalse(source.contains("net.minecraft."), file.toString());
        assertFalse(source.contains("net.minecraftforge."), file.toString());
        assertFalse(source.contains("net.neoforged."), file.toString());
      }
    }

    assertFalse(Files.exists(root.resolve(
        "common/src/main/java/com/mo/economy_system/core/economy_system/reward/RewardManager.java")));
    assertFalse(Files.exists(root.resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/core/economy_system/reward/RewardConfigWatcher.java")));

    String neoEvent = read(root.resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/events/EconomySystem_EventHandler.java"));
    String forgeEvent = read(root.resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/reward/Forge1201RewardEvents.java"));
    for (String source : List.of(neoEvent, forgeEvent)) {
      assertTrue(source.contains("RewardRuntime.award"), source);
      assertFalse(source.contains("nextDouble"), source);
      assertFalse(source.contains("nextInt"), source);
      assertFalse(source.contains("applyDropChanceBonus"), source);
      assertFalse(source.contains("applyRewardBonus"), source);
      assertFalse(source.contains("addBalance(player.getUUID(), reward"), source);
      assertFalse(source.contains("\"击杀奖励: \""), source);
    }

    for (String relative : List.of(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/enchant/enchants/BountyHunterEnchantment.java",
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/enchant/enchants/CarefullyEnchantment.java")) {
      String source = read(root.resolve(relative));
      assertFalse(source.contains("0.25D"), relative);
      assertFalse(source.contains("0.3D"), relative);
    }
  }

  @Test
  void tpaPolicyHasOneCommonImplementation() throws Exception {
    Path root = repositoryRoot();
    Path commonRoot = root.resolve("common/src/main/java/com/mo/economy_system/common/tpa");
    try (Stream<Path> files = Files.walk(commonRoot)) {
      for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
        String source = read(file);
        assertFalse(source.contains("net.minecraft."), file.toString());
        assertFalse(source.contains("net.minecraftforge."), file.toString());
        assertFalse(source.contains("net.neoforged."), file.toString());
      }
    }

    for (String relative : List.of(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/commands/tpa_system/Command_Tpa.java",
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/tpa/Forge1201TpaCommands.java")) {
      String source = read(root.resolve(relative));
      assertTrue(source.contains("TpaRuntime.service"), relative);
      assertFalse(source.contains("new HashMap"), relative);
      assertFalse(source.contains("System.currentTimeMillis"), relative);
      assertFalse(source.contains("stack.shrink"), relative);
      assertFalse(source.contains("updateChunkForced"), relative);
    }

    for (String relative : List.of(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/tpa/NeoForge1211TpaRuntime.java",
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/tpa/Forge1201TpaRuntime.java")) {
      String source = read(root.resolve(relative));
      assertTrue(source.contains("RecallPotionReservation.reserve"), relative);
      assertTrue(source.contains("TicketType.POST_TELEPORT"), relative);
      assertFalse(source.contains("updateChunkForced"), relative);
    }
  }

  @Test
  void marketExpirationPolicyHasOneCommonImplementation() throws Exception {
    Path root = repositoryRoot();
    String service = read(root.resolve(
        "common/src/main/java/com/mo/economy_system/common/market/MarketExpirationService.java"));
    String schedule = read(root.resolve(
        "common/src/main/java/com/mo/economy_system/common/market/MarketExpirationSchedule.java"));
    assertTrue(service.contains("class MarketExpirationService"));
    assertTrue(service.contains("creditExact") || service.contains("Accounts"));
    assertTrue(service.contains("removeIfUnchanged"));
    assertTrue(schedule.contains("INTERVAL_TICKS"));

    String neoRuntime = read(root.resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/market/NeoForge1211MarketExpirationRuntime.java"));
    String forgeRuntime = read(root.resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/network/Forge1201MarketExpirationRuntime.java"));
    String neoEvents = read(root.resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/events/EconomySystem_EventHandler.java"));
    String forgeEvents = read(root.resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/Forge1201ServerEvents.java"));
    for (String source : List.of(neoRuntime, forgeRuntime)) {
      assertTrue(source.contains("MarketExpirationService.expire"), source);
      assertTrue(source.contains("addAll"), source);
      assertFalse(source.contains("addBalance("), source);
    }
    for (String source : List.of(neoEvents, forgeEvents)) {
      assertTrue(source.contains("MarketExpirationSchedule.shouldRun"), source);
      assertFalse(source.contains("MarketManager.getMarketItems"), source);
      assertFalse(source.contains("item.isExpired()"), source);
    }
    assertTrue(read(root.resolve(
        "common/src/main/java/com/mo/economy_system/common/market/MarketOrder.java"))
        .contains("isExpiredAt"));
  }

  @Test
  void marketCreationAndDemandDeliveryTransactionsHaveOneCommonImplementation() throws Exception {
    Path root = repositoryRoot();
    for (String service : List.of(
        "CreateDemandOrderService",
        "CreateSalesOrderService",
        "DemandOrderDeliveryService")) {
      String source = read(root.resolve(
          "common/src/main/java/com/mo/economy_system/common/market/" + service + ".java"));
      assertTrue(source.contains("public final class " + service), service);
      assertFalse(source.contains("net.minecraft."), service);
    }

    for (String relative : List.of(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/network/Forge1201CreateDemandOrderHandler.java",
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/protocol/NeoForge1211CreateDemandOrderHandler.java")) {
      assertTrue(read(root.resolve(relative)).contains("CreateDemandOrderService.execute"), relative);
    }
    for (String relative : List.of(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/network/Forge1201CreateSalesOrderHandler.java",
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/protocol/NeoForge1211CreateSalesOrderHandler.java")) {
      assertTrue(read(root.resolve(relative)).contains("CreateSalesOrderService.execute"), relative);
    }
    for (String relative : List.of(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/network/Forge1201DeliverDemandOrderHandler.java",
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/protocol/NeoForge1211DeliverDemandOrderHandler.java")) {
      assertTrue(read(root.resolve(relative)).contains("DemandOrderDeliveryService.execute"), relative);
    }
  }

  @Test
  void nativeTerritoryGeometryDelegatesToCommon() throws Exception {
    Path root = repositoryRoot();
    String territory = read(root.resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/core/territory_system/Territory.java"));
    assertTrue(territory.contains("TerritoryGeometry.rectangle"));
    assertTrue(territory.contains("xzBounds().contains"));
    assertFalse(territory.contains("Math.min(x1, x2)"));
    assertFalse(territory.contains("Math.min(z1, z2)"));

    String quadTree = read(root.resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/core/territory_system/QuadTree.java"));
    assertFalse(quadTree.contains("queryExact(BlockPos"));
    assertFalse(quadTree.contains("pos2.getX()"));
  }

  @Test
  void starterKitAndUpdateCheckUseCommonPoliciesOnBothTargets() throws Exception {
    Path root = repositoryRoot();
    for (String relative : List.of(
        "common/src/main/java/com/mo/economy_system/common/starter/StarterKitService.java",
        "common/src/main/java/com/mo/economy_system/common/update/UpdateReleaseJsonCodec.java",
        "common/src/main/java/com/mo/economy_system/common/update/SemanticVersion.java")) {
      String source = read(root.resolve(relative));
      assertFalse(source.contains("net.minecraft."), relative);
      assertFalse(source.contains("net.minecraftforge."), relative);
      assertFalse(source.contains("net.neoforged."), relative);
    }
    for (String relative : List.of(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/starter/NeoForge1211StarterKitRuntime.java",
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/starter/Forge1201StarterKitRuntime.java")) {
      String source = read(root.resolve(relative));
      assertTrue(source.contains("StarterKitService"), relative);
      assertFalse(source.contains("addBalance("), relative);
    }
    for (String relative : List.of(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/update/NeoForge1211UpdateRuntime.java",
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/update/Forge1201UpdateRuntime.java")) {
      String source = read(root.resolve(relative));
      assertTrue(source.contains("UpdateReleaseJsonCodec") || source.contains("SemanticVersion"), relative);
      assertFalse(source.contains("version.compareTo"), relative);
    }
    assertFalse(Files.exists(root.resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/core/update_checker_system/UpdateChecker.java")));
  }

  private static String read(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  private static List<Path> activeScreenFiles(Path sourceRoot) throws IOException {
    List<Path> result = new ArrayList<>();
    try (Stream<Path> files = Files.walk(sourceRoot)) {
      for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
        String relative = sourceRoot.relativize(file).toString().replace('\\', '/');
        if (relative.startsWith("com/mo/economy_system/screen/")) continue;
        if (read(file).contains("extends Screen")) result.add(file);
      }
    }
    return List.copyOf(result);
  }

  private static int countMatches(String source, String regex) {
    int count = 0;
    var matcher = Pattern.compile(regex).matcher(source);
    while (matcher.find()) count++;
    return count;
  }

  private static Path repositoryRoot() {
    Path current = Path.of("").toAbsolutePath();
    while (current != null && !Files.exists(current.resolve("settings.gradle"))) current = current.getParent();
    if (current == null) throw new IllegalStateException("repository root not found");
    return current;
  }
}

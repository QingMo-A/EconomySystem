package com.mo.economy_system.target.neoforge1211.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.gui.GuiGraphics;
import org.junit.jupiter.api.Test;

class NeoForge1211UiBridgeTest {
    @Test
    void baselineProvidesEverySharedRoute() {
        for (EconomyUiRoute route : EconomyUiRoute.values()) {
            assertTrue(NeoForge1211UiBridge.INSTANCE.supports(route), route.name());
        }
    }

    @Test
    void homeRouteUsesTheCommonHomeShell() {
        assertTrue(NeoForge1211UiBridge.INSTANCE.create(EconomyUiRoute.HOME).orElseThrow()
                instanceof NeoForge1211HomeScreen);
    }

    @Test
    void territoryPilotUsesCommonRendererAndControllerShell() {
        assertTrue(EconomyUiRenderer.class.isAssignableFrom(NeoForge1211UiRenderer.class));
        assertTrue(java.util.Arrays.stream(NeoForge1211TerritoryManageScreen.class.getDeclaredFields())
                .anyMatch(field -> field.getType() == com.mo.economy_system.ui.territory.TerritoryManageController.class));
    }

    @Test
    void nestedTerritoryPagesUseCommonModelsAndExistingWireMessages() throws Exception {
        String manage = read(repositoryRoot().resolve(
            "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client/NeoForge1211TerritoryManageScreen.java"));
        assertTrue(manage.contains("NeoForge1211BuffManageScreen"));
        assertTrue(manage.contains("NeoForge1211TerritoryInviteScreen"));
        assertTrue(manage.contains("NeoForge1211TerritoryConfirmationScreen"));
        assertTrue(manage.contains("NeoForge1211TerritoryDetailScreen"));
        assertFalse(manage.contains("Screen_TerritoryPlayerAction"));

        String detail = read(repositoryRoot().resolve(
            "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client/NeoForge1211TerritoryDetailScreen.java"));
        assertTrue(detail.contains("TerritoryDetailController"));
        assertTrue(detail.contains("TerritoryDetailLayout"));
        assertTrue(detail.contains("TerritoryDetailView.render"));
        assertTrue(detail.contains("UpdateTerritoryPermissionMessage"));
        assertTrue(detail.contains("UpdateTerritoryRuleMessage"));
        assertTrue(detail.contains("TransferTerritoryOwnershipMessage"));
    }

    @Test
    void singleTerritoryScreensShareOneRequestSequence() throws Exception {
        Path client = repositoryRoot().resolve(
            "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client");
        for (String file : new String[]{"NeoForge1211TerritoryManageScreen.java",
            "NeoForge1211TerritoryDetailScreen.java", "NeoForge1211BuffManageScreen.java"}) {
            String source = read(client.resolve(file));
            assertTrue(source.contains("TerritoryRequestIds.nextSingleTerritory()"), file);
            assertFalse(source.contains("AtomicLong IDS"), file);
        }
        String legacyBuff = read(repositoryRoot().resolve(
            "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/screen/territory_system/Screen_TerritoryBuff.java"));
        assertTrue(legacyBuff.contains("TerritoryRequestIds.nextSingleTerritory()"));
        assertFalse(legacyBuff.contains("AtomicLong REQUEST_IDS"));
    }

    @Test
    void aboutAndPurchaseShellsConsumeNavigationAndDisableVanillaBlur() throws Exception {
        assertTrue(NeoForge1211AboutScreen.class.getDeclaredMethod("tick") != null);
        assertTrue(NeoForge1211ShopPurchaseScreen.class.getDeclaredMethod("renderBackground",
            GuiGraphics.class, int.class, int.class, float.class) != null);
    }

    @Test
    void marketCreateSyncsControllerQuantityAndUsesMainInventoryCapacityOnly() throws Exception {
        String source = read(repositoryRoot().resolve(
            "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client/NeoForge1211MarketCreateScreen.java"));
        assertTrue(source.contains("private boolean syncingInputs"));
        assertTrue(source.contains("syncValue(quantity"));
        assertTrue(source.contains("if (!syncingInputs)"));
        String purchase = read(repositoryRoot().resolve(
            "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client/NeoForge1211ShopPurchaseScreen.java"));
        assertTrue(purchase.contains("player.getInventory().items"));
        assertTrue(purchase.contains("item.getDefaultInstance().getMaxStackSize()"));
        assertTrue(purchase.contains("stack.getMaxStackSize()"));
        assertFalse(purchase.contains("getArmorSlots"));
        assertFalse(purchase.contains("offhand"));
    }

    @Test
    void marketInventoryCountUsesNativeDecorationAfterItemInOneRenderer() throws Exception {
        assertTrue(EconomyUiRenderer.class.getDeclaredMethod("itemWithCount",
            String.class, int.class, com.mo.economy_system.ui.geometry.UiRect.class) != null);
        String source = read(repositoryRoot().resolve(
            "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client/NeoForge1211UiRenderer.java"));
        assertTrue(source.contains("void itemWithCount"));
        int item = source.indexOf("graphics.renderItem(stack, x, y)");
        int decorations = source.indexOf("graphics.renderItemDecorations(font, stack, x, y, Integer.toString(count))");
        assertTrue(item >= 0 && decorations > item,
            "NeoForge must paint native item count after the item in one scaled pose");
    }

    @Test
    void marketCreateReturnRefreshesExistingMarketScreenAndCompletionScrolls() throws Exception {
        String market = read(repositoryRoot().resolve(
            "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client/NeoForge1211MarketScreen.java"));
        String create = read(repositoryRoot().resolve(
            "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client/NeoForge1211MarketCreateScreen.java"));
        assertTrue(market.contains("void refreshData"));
        assertTrue(market.contains("new MarketEvent.Initialize"));
        assertTrue(create.contains("refreshData()"));
        assertTrue(create.contains("mouseScrolled"));
        assertTrue(create.contains("CompletionMoved"));
        assertFalse(create.contains(".limit(MarketCreateLayout.COMPLETION_MAX_ROWS)"));
    }

    @Test
    void activeNativeInputsUseOneSharedStyleAdapter() throws Exception {
        for (String file : new String[]{
            "NeoForge1211MarketCreateScreen.java", "NeoForge1211MarketScreen.java",
            "NeoForge1211ShopScreen.java", "NeoForge1211ShopPurchaseScreen.java",
            "NeoForge1211DeliveryBoxScreen.java", "NeoForge1211BuffManageScreen.java",
        "NeoForge1211TerritoryDetailScreen.java", "NeoForge1211TerritoryInviteScreen.java",
        "NeoForge1211TerritoryListScreen.java"}) {
            String source = read(repositoryRoot().resolve(
                "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client/" + file));
            assertTrue(source.contains("NeoForge1211UiInputAdapter.apply"), file);
        }
        String check = read(repositoryRoot().resolve(
            "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client/Screen_ClientFileCheckResult.java"));
        assertTrue(check.contains("NeoForge1211UiInputAdapter.apply"));
    }

    @Test
    void activeNativeInputsUseBottomAlignedTransparentSubclass() throws Exception {
        String widget = read(repositoryRoot().resolve(
            "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client/NeoForge1211UnderlinedEditBox.java"));
        assertTrue(widget.contains("extends EditBox"));
        assertTrue(widget.contains("height - font.lineHeight - BOTTOM_TEXT_PADDING"));
        for (String file : new String[]{
            "NeoForge1211MarketCreateScreen.java", "NeoForge1211MarketScreen.java",
            "NeoForge1211ShopScreen.java", "NeoForge1211ShopPurchaseScreen.java",
            "NeoForge1211DeliveryBoxScreen.java", "NeoForge1211BuffManageScreen.java",
            "NeoForge1211TerritoryDetailScreen.java", "NeoForge1211TerritoryInviteScreen.java",
            "NeoForge1211TerritoryListScreen.java", "Screen_ClientFileCheckResult.java"}) {
            String source = read(repositoryRoot().resolve(
                "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client/" + file));
            assertTrue(source.contains("new NeoForge1211UnderlinedEditBox"), file);
            assertFalse(source.contains("new EditBox"), file);
        }
    }

    @Test
    void homeShopMarketAndAboutUseSettledStaticLayouts() throws Exception {
        for (String file : new String[]{"NeoForge1211HomeScreen.java", "NeoForge1211ShopScreen.java",
            "NeoForge1211MarketScreen.java", "NeoForge1211AboutScreen.java"}) {
            String source = read(repositoryRoot().resolve(
                "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client/" + file));
            assertFalse(source.contains("OpenAnimation"), file);
            assertFalse(source.contains("animationStartedAtNanos"), file);
            assertFalse(source.contains("animationProgress()"), file);
            assertTrue(source.contains("1.0f"), file);
        }
    }

    private static String read(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("settings.gradle"))) current = current.getParent();
        if (current == null) throw new IllegalStateException("repository root not found");
        return current;
    }
}

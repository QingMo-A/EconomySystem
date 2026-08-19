package com.mo.economy_system.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mo.economy_system.common.market.MarketOrderType;
import com.mo.economy_system.common.network.MarketOrderFilter;
import com.mo.economy_system.common.network.MarketOrderSnapshot;
import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.common.territory.TerritorySnapshots;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import com.mo.economy_system.common.territory.TerritorySnapshots.Rule;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import com.mo.economy_system.common.network.MarketOrderSnapshot;
import com.mo.economy_system.platform.item.ItemStackSnapshot;
import com.mo.economy_system.platform.nbt.NbtData;
import com.mo.economy_system.ui.about.AboutLayout;
import com.mo.economy_system.ui.about.AboutState;
import com.mo.economy_system.ui.balance.BalanceLogLayout;
import com.mo.economy_system.ui.balance.BalanceLogState;
import com.mo.economy_system.ui.check.CheckConsentLayout;
import com.mo.economy_system.ui.check.CheckResultLayout;
import com.mo.economy_system.ui.check.CheckResultState;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.delivery.DeliveryLayout;
import com.mo.economy_system.ui.delivery.DeliveryState;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.market.MarketAction;
import com.mo.economy_system.ui.market.MarketConfirmAction;
import com.mo.economy_system.ui.market.MarketConfirmLayout;
import com.mo.economy_system.ui.market.MarketConfirmState;
import com.mo.economy_system.ui.market.MarketCreateAction;
import com.mo.economy_system.ui.market.MarketCreateLayout;
import com.mo.economy_system.ui.market.MarketCreateMode;
import com.mo.economy_system.ui.market.MarketCreateState;
import com.mo.economy_system.ui.market.MarketLayout;
import com.mo.economy_system.ui.market.MarketRow;
import com.mo.economy_system.ui.market.MarketState;
import com.mo.economy_system.ui.shop.ShopAction;
import com.mo.economy_system.ui.shop.ShopLayout;
import com.mo.economy_system.ui.shop.ShopPurchaseAction;
import com.mo.economy_system.ui.shop.ShopPurchaseLayout;
import com.mo.economy_system.ui.shop.ShopPurchaseState;
import com.mo.economy_system.ui.shop.ShopRow;
import com.mo.economy_system.ui.shop.ShopState;
import com.mo.economy_system.ui.territory.buff.BuffManageLayout;
import com.mo.economy_system.ui.territory.buff.BuffManageState;
import com.mo.economy_system.ui.territory.confirm.TerritoryConfirmationKind;
import com.mo.economy_system.ui.territory.confirm.TerritoryConfirmationLayout;
import com.mo.economy_system.ui.territory.confirm.TerritoryConfirmationState;
import com.mo.economy_system.ui.territory.detail.TerritoryDetailLayout;
import com.mo.economy_system.ui.territory.detail.TerritoryDetailState;
import com.mo.economy_system.ui.territory.detail.TerritoryDetailViewKind;
import com.mo.economy_system.ui.territory.invite.TerritoryInviteLayout;
import com.mo.economy_system.ui.territory.invite.TerritoryInviteState;
import com.mo.economy_system.ui.territory.list.TerritoryListLayout;
import com.mo.economy_system.ui.territory.list.TerritoryListState;
import com.mo.economy_system.ui.territory.list.TerritoryListRow;
import com.mo.economy_system.ui.transfer.TransferConsentLayout;
import com.mo.economy_system.ui.transfer.TransferConsentState;
import com.mo.economy_system.ui.transfer.TransferResultLayout;
import com.mo.economy_system.ui.transfer.TransferResultState;
import com.mo.economy_system.common.check.ClientFileCheckResultController;
import com.mo.economy_system.common.check.ClientFileCheckStatus;
import com.mo.economy_system.core.economy_system.BalanceLogEntry;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Table-driven geometry gate for every forensic layout.  These are the legacy virtual-canvas
 * formulas evaluated at the required canonical, narrow, short and fractional viewports; merely
 * asserting that a rectangle fits on screen would not catch a scale or baseline drift.
 */
class LegacyMultiViewportParityTest {
  private static final int[][] VIEWPORTS = {
      {640, 360, 640, 360}, {854, 480, 640, 360}, {1280, 720, 640, 360},
      {1920, 1080, 640, 360}, {800, 600, 640, 480}, {1000, 563, 640, 360},
      {320, 200, 640, 400}, {640, 240, 960, 360}, {853, 479, 641, 360}
  };

  @Test
  void everyForensicLayoutRetainsLegacyVirtualGeometryAcrossViewports() {
    for (int[] viewport : VIEWPORTS) {
      int width = viewport[0], height = viewport[1], virtualWidth = viewport[2], virtualHeight = viewport[3];
      assertScale(ShopLayout.calculate(width, height, shopState()).scale(), virtualWidth, virtualHeight);
      var shop = ShopLayout.calculate(width, height, shopState());
      assertEquals(12, shop.search().x());
      assertEquals(20, shop.search().y());
      assertEquals(virtualHeight - 35, shop.pageText().y());

      var purchase = ShopPurchaseLayout.calculate(width, height, purchaseState());
      assertScale(purchase.scale(), virtualWidth, virtualHeight);
      assertEquals(Math.max(8, (virtualWidth - 320) / 2), purchase.card().x());
      assertEquals(Math.max(8, (virtualHeight - 160) / 2), purchase.card().y());

      var market = MarketLayout.calculate(width, height, marketState());
      assertScale(market.scale(), virtualWidth, virtualHeight);
      assertEquals(12, market.search().x());
      assertEquals(20, market.search().y());
      assertEquals(Math.max(55, virtualHeight - 40), market.previousButton().y());
      assertEquals(virtualHeight - 35, market.pageText().y());

      var createSales = MarketCreateLayout.calculate(width, height, createState(MarketCreateMode.SALES));
      assertScale(createSales.scale(), virtualWidth, virtualHeight);
      assertEquals(52, createSales.formPanel().y());
      var createDemand = MarketCreateLayout.calculate(width, height, createState(MarketCreateMode.DEMAND));
      assertScale(createDemand.scale(), virtualWidth, virtualHeight);
      assertEquals(Math.max(12, (virtualHeight - createDemand.formPanel().height()) / 2), createDemand.formPanel().y());

      var confirm = MarketConfirmLayout.calculate(width, height, confirmState());
      assertScale(confirm.scale(), virtualWidth, virtualHeight);
      assertEquals(Math.max(8, (virtualWidth - confirm.card().width()) / 2), confirm.card().x());
      assertEquals(Math.max(8, (virtualHeight - 200) / 2), confirm.card().y());

      var delivery = DeliveryLayout.calculate(width, height, deliveryState());
      assertScale(delivery.scale(), virtualWidth, virtualHeight);
      assertEquals(12, delivery.categoryPanel().x());
      assertEquals(116, delivery.search().x());
      assertEquals(20, delivery.search().y());
      assertEquals(314, delivery.detailPanel().x());
      assertEquals(virtualHeight - 66, delivery.previousButton().y());
      assertEquals(virtualHeight - 60, delivery.pageText().y());

      var about = AboutLayout.calculate(width, height, com.mo.economy_system.ui.text.UiTextMetrics.APPROXIMATE, 1.0f);
      assertScale(about.scale(), virtualWidth, virtualHeight);
      assertEquals(12, about.title().y());
      assertEquals(28, about.panel().y());
      int qrSize = Math.min(110, Math.max(60, (virtualHeight - 24) / 3));
      assertEquals(virtualHeight - 12 - qrSize, about.leftQr().y());

      var balance = BalanceLogLayout.calculate(width, height, balanceState());
      assertScale(balance.scale(), virtualWidth, virtualHeight);
      assertEquals(12, balance.panel().x());
      assertEquals(12, balance.panel().y());
      assertEquals(virtualHeight - 12 - 30, balance.previousButton().y());

      var list = TerritoryListLayout.calculate(width, height, territoryListState());
      assertScale(list.scale(), virtualWidth, virtualHeight);
      assertEquals(20, list.search().y());
      assertEquals(virtualHeight - 12 - 19, list.title().y());
      var detail = TerritoryDetailLayout.calculate(width, height, territoryDetailState());
      assertScale(detail.scale(), virtualWidth, virtualHeight);
      assertEquals(18, detail.title().y());
      assertEquals(45, detail.search().y());
      assertEquals(76, detail.rows().y());
      var invite = TerritoryInviteLayout.calculate(width, height, inviteState());
      assertScale(invite.scale(), virtualWidth, virtualHeight);
      assertEquals(12, invite.title().y());
      assertEquals(52, invite.search().y());
      var buff = BuffManageLayout.calculate(width, height, buffState());
      assertScale(buff.scale(), virtualWidth, virtualHeight);
      assertEquals(20, buff.search().y());
      var territoryConfirm = TerritoryConfirmationLayout.calculate(width, height, territoryConfirmState());
      assertScale(territoryConfirm.scale(), virtualWidth, virtualHeight);
      assertEquals(Math.max(8, (virtualWidth - territoryConfirm.card().width()) / 2), territoryConfirm.card().x());

      var checkConsent = CheckConsentLayout.calculate(width, height);
      assertScale(checkConsent.scale(), virtualWidth, virtualHeight);
      assertEquals(35, checkConsent.title().y());
      var checkResult = CheckResultLayout.calculate(width, height, checkResultState());
      assertScale(checkResult.scale(), virtualWidth, virtualHeight);
      assertEquals(18, checkResult.title().y());
      assertEquals(62, checkResult.search().y());
      assertEquals(92, checkResult.rows().y());
      var transferConsent = TransferConsentLayout.calculate(width, height);
      assertScale(transferConsent.scale(), virtualWidth, virtualHeight);
      assertEquals(18, transferConsent.title().y());
      assertEquals(42, transferConsent.details().get(0).y());
      var transferResult = TransferResultLayout.calculate(width, height, TransferResultState.artifact(
          "source", "mods", "example.jar", 12, "a".repeat(64), "message.transfer.state.pending"));
      assertScale(transferResult.scale(), virtualWidth, virtualHeight);
      assertEquals(18, transferResult.title().y());
      assertEquals(42, transferResult.details().get(0).y());
    }
  }

  private static void assertScale(UiScale actual, int width, int height) {
    assertEquals(width, actual.virtualWidth());
    assertEquals(height, actual.virtualHeight());
  }

  private static ShopState shopState() {
    return new ShopState(List.of(new ShopRow(new com.mo.economy_system.common.network.ShopItemSnapshot(
        "shop", "minecraft:stone", 25, 20, 19, "stone", 0.1, "", "", 0, 64, 64))),
        0, 15, "", ScreenState.READY, null, -1, Set.of(ShopAction.values()));
  }

  private static ShopPurchaseState purchaseState() {
    return new ShopPurchaseState(shopState().rows().get(0), 1, 20, 64,
        ScreenState.READY, null, Set.of(ShopPurchaseAction.values()));
  }

  private static MarketState marketState() {
    return new MarketState(List.of(), 0, 9, 0, 0, 0, MarketOrderFilter.ALL, "",
        ScreenState.EMPTY, null, -1, 0, Set.of(MarketAction.values()));
  }

  private static MarketCreateState createState(MarketCreateMode mode) {
    return new MarketCreateState(mode, List.of(), -1, "", 0, 0,
        ScreenState.READY, null, Set.of(MarketCreateAction.values()));
  }

  private static MarketConfirmState confirmState() {
    return new MarketConfirmState(MarketAction.BUY,
        new MarketRow(orderSnapshot()), ScreenState.READY, null, Set.of(MarketConfirmAction.values()));
  }

  private static MarketOrderSnapshot orderSnapshot() {
    ItemStackSnapshot item = ItemStackSnapshot.create("minecraft:stone", 1, Optional.empty(), List.of(),
        java.util.Map.of(), java.util.Map.of(), true, true, 0, 0, false, true,
        java.util.OptionalInt.empty(), true, java.util.OptionalInt.empty(), NbtData.emptyCompound()).orElseThrow();
    return new MarketOrderSnapshot(MarketOrderType.SALES, new UUID(3, 1), item, 1, 20,
        "owner", new UUID(2, 1), 1, 2, false);
  }

  private static DeliveryState deliveryState() {
    return new DeliveryState(List.of(), 0, 1, "", ScreenState.EMPTY, null, -1, Set.of());
  }

  private static AboutState aboutState() { return new AboutState("Economy", "Author", "https://example.test", false); }

  private static BalanceLogState balanceState() {
    return new BalanceLogState(List.of(new com.mo.economy_system.ui.balance.BalanceLogRow(
        new BalanceLogEntry(1, "all", "reason", 1, 0, 1))),
        "all", 0, 10, 1, 0, 1, ScreenState.READY, null, -1,
        Set.of(com.mo.economy_system.ui.balance.BalanceLogAction.values()));
  }

  private static TerritoryListState territoryListState() {
    return new TerritoryListState(List.of(), 0, 1, "", ScreenState.EMPTY, null, -1,
        Set.of(com.mo.economy_system.ui.territory.list.TerritoryListAction.values()));
  }

  private static TerritoryDetailState territoryDetailState() {
    UUID owner = new UUID(4, 1);
    TerritorySnapshots.Summary summary = new TerritorySnapshots.Summary(owner, owner, "owner", "home",
        new Position(0, 0, 0), new Position(10, 10, 10), "minecraft:overworld");
    List<Rule> rules = java.util.Arrays.stream(RuleAction.values()).map(action -> new Rule(action, RuleLevel.EVERYONE)).toList();
    Owned owned = new Owned(summary, List.of(), Optional.empty(), rules, List.of());
    return new TerritoryDetailState(owned, List.<PlayerSummary>of(), TerritoryDetailViewKind.MAIN,
        0, 1, "", ScreenState.READY, null, -1, 0);
  }

  private static TerritoryInviteState inviteState() {
    UUID territory = new UUID(5, 1), owner = new UUID(5, 2), viewer = new UUID(5, 3);
    return new TerritoryInviteState(territory, "home", owner, viewer, Set.of(), List.of(), "", 0, 1,
        ScreenState.EMPTY, null, -1, 0, 0);
  }

  private static BuffManageState buffState() {
    return new BuffManageState(new UUID(6, 1), "home", List.of(), 0, 1, 0, "", ScreenState.EMPTY, null, -1);
  }

  private static TerritoryConfirmationState territoryConfirmState() {
    return new TerritoryConfirmationState(TerritoryConfirmationKind.REMOVE_TERRITORY,
        new UUID(7, 1), "home", null, "", ScreenState.READY, Set.of());
  }

  private static CheckResultState checkResultState() {
    return new CheckResultState("target", "mods", ClientFileCheckStatus.SUCCESS, 0, 0, null,
        ClientFileCheckResultController.LocalState.NOT_REQUIRED, null, false, List.of(), "", 0, 1,
        ScreenState.EMPTY, Set.of());
  }
}

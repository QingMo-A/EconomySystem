package com.mo.economy_system.ui.commission_public;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.commission.PublicCommission;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PublicCommissionCenterLayoutTest {
  @Test
  void controlsRemainReachableAcrossReferenceViewports() {
    PublicCommissionCenterState state = state(8, ScreenState.READY);
    for (int[] size : new int[][] {{320, 180}, {640, 360}, {854, 480}, {1280, 720}, {1920, 1080}}) {
      PublicCommissionCenterLayout.Layout layout = PublicCommissionCenterLayout.calculate(
          size[0], size[1], state);
      assertTrue(layout.list().width() > 0 && layout.list().height() > 0);
      assertTrue(layout.detail().width() > 0 && layout.detail().height() > 0);
      assertFalse(layout.list().overlaps(layout.detail()));
      assertTrue(layout.list().contains(layout.listHeader()));
      assertTrue(layout.detail().contains(layout.detailHeader()));
      assertTrue(layout.detail().contains(layout.target()));
      assertTrue(layout.detail().contains(layout.progress()));
      assertTrue(layout.detail().contains(layout.reward()));
      assertTrue(layout.detail().contains(layout.expiration()));
      assertTrue(layout.detail().contains(layout.amountInput()));
      assertTrue(layout.detail().contains(layout.submit()));
      for (PublicCommissionCenterLayout.Card card : layout.cards()) {
        assertTrue(layout.list().contains(card.rect()));
      }
      for (int index = 0; index < layout.cards().size(); index++) {
        for (int other = index + 1; other < layout.cards().size(); other++) {
          assertFalse(layout.cards().get(index).rect().overlaps(layout.cards().get(other).rect()));
        }
      }
    }
  }

  @Test
  void emptyAndErrorStatesStillExposeVisibleActions() {
    for (ScreenState screenState : List.of(ScreenState.EMPTY, ScreenState.ERROR, ScreenState.LOADING)) {
      PublicCommissionCenterLayout.Layout layout = PublicCommissionCenterLayout.calculate(
          1, 1, state(0, screenState));
      assertTrue(layout.emptyOrLoading().width() > 0);
      assertTrue(layout.emptyOrLoading().height() > 0);
      assertTrue(layout.detail().contains(layout.retry()));
      assertTrue(layout.back().width() > 0 && layout.back().height() > 0);
    }
  }

  private static PublicCommissionCenterState state(int count, ScreenState screenState) {
    List<PublicCommission> commissions = new ArrayList<>();
    for (int index = 0; index < count; index++) {
      UUID id = new UUID(0, index + 1L);
      commissions.add(PublicCommission.create(id, "Commission " + index, "town", "Town",
          "minecraft:stone", 64 + index, 3, 1_000L, 10_000L, "description"));
    }
    return new PublicCommissionCenterState(commissions, 2_000L,
        commissions.isEmpty() ? null : commissions.get(0).commissionId(), screenState,
        screenState == ScreenState.ERROR ? "screen.commissions.public.failed" : "", 1L,
        false, null, "");
  }
}

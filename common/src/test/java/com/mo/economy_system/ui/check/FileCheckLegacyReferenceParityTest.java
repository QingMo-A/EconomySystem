package com.mo.economy_system.ui.check;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.check.ClientFileCheckResultController;
import com.mo.economy_system.common.check.ClientFileCheckStatus;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Exact consent/result coordinates and chrome from the pre-Bridge client file-check screens. */
class FileCheckLegacyReferenceParityTest {
  @Test
  void consentMatchesLegacyCenteredTextAndBottomButtons() {
    CheckConsentLayout.Layout layout = CheckConsentLayout.calculate(640, 360);
    assertEquals(new UiRect(0, 35, 640, 14), layout.title());
    assertEquals(new UiRect(0, 57, 640, 14), layout.requester());
    assertEquals(new UiRect(0, 73, 640, 14), layout.type());
    assertEquals(new UiRect(0, 89, 640, 14), layout.folder());
    assertEquals(new UiRect(0, 111, 640, 14), layout.dataNotice());
    assertEquals(new UiRect(0, 127, 640, 14), layout.noContentNotice());
    assertEquals(new UiRect(215, 335, 100, 20), layout.allow());
    assertEquals(new UiRect(325, 335, 100, 20), layout.decline());
  }

  @Test
  void consentViewKeepsPlainCenteredLegacyPresentation() {
    CheckConsentState state = new CheckConsentState("Alice", "mods", ScreenState.READY,
        Set.of(CheckConsentAction.ALLOW, CheckConsentAction.DECLINE));
    CheckConsentLayout.Layout layout = CheckConsentLayout.calculate(640, 360);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    CheckConsentView.render(renderer, state, layout, 0, 0);
    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("translatedTextInRect")
            && operation.value().startsWith("screen.check_consent.title[]:CENTER")));
    assertFalse(renderer.operations().stream().anyMatch(operation -> operation.kind().equals("card")),
        "legacy check consent has no policy card chrome");
  }

  @Test
  void resultMatchesLegacySearchAndTwelvePixelRows() {
    CheckResultLayout.Layout layout = CheckResultLayout.calculate(640, 360, resultState());
    assertEquals(new UiRect(0, 18, 640, 14), layout.title());
    assertEquals(new UiRect(12, 62, 220, 18), layout.search());
    assertEquals(new UiRect(12, 92, 616, 268), layout.rows());
    assertEquals(21, layout.visibleRows());
  }

  @Test
  void resultUsesInputFrameAndPlainRowsInsteadOfCardSubstitute() {
    CheckResultState state = resultState();
    CheckResultLayout.Layout layout = CheckResultLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    CheckResultView.render(renderer, state, layout, 0, 0);
    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("inputFrame") && operation.rect().x() == 8));
    assertFalse(renderer.operations().stream().anyMatch(operation -> operation.kind().equals("card")),
        "legacy result page has no card replacement for the native search/row surfaces");
    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("textInRect") && operation.value().startsWith("a.jar")));
  }

  private static CheckResultState resultState() {
    return new CheckResultState("target", "mods", ClientFileCheckStatus.SUCCESS, 1, 0, null,
        ClientFileCheckResultController.LocalState.NOT_REQUIRED, null, false,
        List.of(new CheckResultRow("a.jar", "screen.check_result.only_remote", false)), "", 0,
        22, ScreenState.READY, Set.of(CheckResultAction.RETRY, CheckResultAction.BACK));
  }
}

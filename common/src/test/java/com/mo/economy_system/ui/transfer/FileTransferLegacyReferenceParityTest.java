package com.mo.economy_system.ui.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Exact transfer consent/result coordinates and plain presentation from legacy target screens. */
class FileTransferLegacyReferenceParityTest {
  @Test
  void consentMatchesLegacyDetailsAndBottomActions() {
    TransferConsentLayout.Layout layout = TransferConsentLayout.calculate(640, 360);
    assertEquals(new UiRect(0, 18, 640, 14), layout.title());
    assertEquals(new UiRect(12, 42, 616, 12), layout.details().get(0));
    assertEquals(new UiRect(12, 54, 616, 12), layout.details().get(1));
    assertEquals(new UiRect(12, 66, 616, 12), layout.details().get(2));
    assertEquals(new UiRect(12, 78, 616, 12), layout.details().get(3));
    assertEquals(new UiRect(12, 90, 616, 12), layout.details().get(4));
    assertEquals(new UiRect(12, 102, 616, 14), layout.warning());
    assertEquals(new UiRect(215, 335, 100, 20), layout.allow());
    assertEquals(new UiRect(325, 335, 100, 20), layout.decline());
  }

  @Test
  void consentViewUsesPlainCenteredTitleAndPreservesDenyAction() {
    TransferConsentState state = new TransferConsentState("Alice", "mods", "example.jar", 12,
        "a".repeat(64), ScreenState.READY,
        Set.of(TransferConsentAction.ALLOW, TransferConsentAction.DECLINE));
    TransferConsentLayout.Layout layout = TransferConsentLayout.calculate(640, 360);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    TransferConsentView.render(renderer, state, layout, 0, 0);
    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("translatedTextInRect")
            && operation.value().startsWith("screen.transfer_consent.title[]:CENTER")));
    assertFalse(renderer.operations().stream().anyMatch(operation -> operation.kind().equals("card")),
        "legacy transfer consent has no card policy chrome");
    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("translatedButton") && operation.value().startsWith("button.transfer.decline")));
  }

  @Test
  void resultMatchesLegacyMetadataRowsAndBottomActions() {
    TransferResultState state = TransferResultState.artifact("target", "mods", "example.jar", 12,
        "a".repeat(64), "message.transfer.state.pending");
    TransferResultLayout.Layout layout = TransferResultLayout.calculate(640, 360, state);
    assertEquals(new UiRect(0, 18, 640, 14), layout.title());
    assertEquals(new UiRect(12, 42, 616, 12), layout.details().get(0));
    assertEquals(new UiRect(12, 54, 616, 12), layout.details().get(1));
    assertEquals(new UiRect(12, 66, 616, 12), layout.details().get(2));
    assertEquals(new UiRect(12, 78, 616, 12), layout.details().get(3));
    assertEquals(new UiRect(215, 335, 100, 20), layout.primary());
    assertEquals(new UiRect(325, 335, 100, 20), layout.secondary());
  }

  @Test
  void resultUsesPlainMetadataAndKeepsExplicitSaveDiscardOrder() {
    TransferResultState state = TransferResultState.artifact("target", "mods", "example.jar", 12,
        "a".repeat(64), "message.transfer.state.pending");
    TransferResultLayout.Layout layout = TransferResultLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    TransferResultView.render(renderer, state, layout, 0, 0);
    assertFalse(renderer.operations().stream().anyMatch(operation -> operation.kind().equals("card")),
        "legacy transfer result has no card policy chrome");
    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("translatedTextInRect")
            && operation.value().startsWith("screen.transfer_result.file[example.jar]")));
    List<String> actions = renderer.operations().stream()
        .filter(operation -> operation.kind().equals("translatedButton"))
        .map(RecordingEconomyUiRenderer.Operation::value).toList();
    assertEquals(2, actions.size());
    assertTrue(actions.get(0).startsWith("button.transfer.save"));
    assertTrue(actions.get(1).startsWith("button.transfer.discard"));
  }
}

package com.mo.economy_system.ui.home;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mo.economy_system.common.client.ui.EconomyUiMenu;
import com.mo.economy_system.common.network.AccountBalance;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class HomeSelfMarkerTest {
  @Test
  void onlySelfLeaderboardRowUsesLocalizedSelfTemplate() {
    HomeState state = new HomeState("alice", EconomyUiMenu.defaultEntries(), 42,
        List.of(new AccountBalance("alice", 42), new AccountBalance("bob", 7)), 0, 0,
        0, 10, ScreenState.READY, null, -1, 1, 1);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();

    HomeView.render(renderer, state, HomeLayout.calculate(640, 360, state), 0, 0);

    List<RecordingEconomyUiRenderer.Operation> selfRows = renderer.operations().stream()
        .filter(operation -> operation.kind().equals("translatedTextWithSuffix")
            && operation.value().contains("screen.home.leaderboard.self"))
        .toList();
    assertEquals(1, selfRows.size());
    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("text") && operation.value().contains("#2 bob")));
  }

  @Test
  void localizedSelfTemplateHasMatchingPlaceholderSignature() throws Exception {
    Path root = repositoryRoot().resolve("common/src/main/resources/assets/economy_system/lang");
    String en = Files.readString(root.resolve("en_us.json"), StandardCharsets.UTF_8);
    String zh = Files.readString(root.resolve("zh_cn.json"), StandardCharsets.UTF_8);
    String enValue = JsonParser.parseString(en).getAsJsonObject()
        .get("screen.home.leaderboard.self").getAsString();
    String zhValue = JsonParser.parseString(zh).getAsJsonObject()
        .get("screen.home.leaderboard.self").getAsString();
    assertTrue(enValue.contains("%s") && enValue.contains("(You)"));
    assertTrue(zhValue.contains("%s") && zhValue.contains("(你)"));
    assertEquals(placeholderCount(enValue), placeholderCount(zhValue));
  }

  private static int placeholderCount(String value) {
    return value.replaceAll("[^%]", "").length();
  }

  private static Path repositoryRoot() {
    Path current = Path.of("").toAbsolutePath();
    while (current != null && !Files.exists(current.resolve("settings.gradle"))) {
      current = current.getParent();
    }
    if (current == null) throw new IllegalStateException("repository root not found");
    return current;
  }
}

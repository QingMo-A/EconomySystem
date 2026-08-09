package com.mo.economy_system.core.territory_system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import org.junit.jupiter.api.Test;

class TerritoryBuffManagerTest {
  @Test
  void acceptsAnExplicitlyEmptyCatalog() {
    assertEquals(0, TerritoryBuffManager.parseConfig(
        new StringReader("{\"buffs\":[]}"))
        .size());
  }

  @Test
  void rejectsDuplicateIdsInsteadOfPublishingAPartialCatalog() {
    String json = "{\"buffs\":[" + buff("speed") + "," + buff("speed") + "]}";

    assertThrows(
        IllegalArgumentException.class,
        () -> TerritoryBuffManager.parseConfig(new StringReader(json)));
  }

  @Test
  void rejectsAnInvalidEntryInsteadOfTreatingItAsRemoved() {
    String invalid = buff("speed").replace("\"count\":1", "\"count\":0");

    assertThrows(
        IllegalArgumentException.class,
        () -> TerritoryBuffManager.parseConfig(
            new StringReader("{\"buffs\":[" + invalid + "]}")));
  }

  private static String buff(String id) {
    return """
        {
          "id":"%s",
          "displayText":"Speed",
          "effectId":"minecraft:speed",
          "initialUnlockState":false,
          "initialLevel":0,
          "singleUpgradeLevel":1,
          "maxLevel":3,
          "upgradeCost":[{
            "items":[{"item":"minecraft:emerald","count":1}],
            "xp":0,
            "df_coin":0
          }]
        }
        """.formatted(id);
  }
}

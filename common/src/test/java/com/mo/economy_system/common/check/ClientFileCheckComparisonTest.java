package com.mo.economy_system.common.check;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ClientFileCheckComparisonTest {
  private static ClientFileCheckEntry file(String name, String hash) {
    return new ClientFileCheckEntry(name, 1, hash.repeat(64));
  }

  private static ClientFileCheckResult result(List<ClientFileCheckEntry> files) {
    return new ClientFileCheckResult(
        1, ClientFileCheckStatus.SUCCESS, ClientFileCheckType.MODS, files, List.of(), null);
  }

  @Test
  void classifiesAndSortsEveryName() {
    var rows =
        ClientFileCheckComparison.compare(
            result(List.of(file("b", "0"), file("a", "0"), file("d", "0"))),
            result(List.of(file("b", "0"), file("c", "1"), file("d", "1"))));
    assertEquals(
        List.of("a", "b", "c", "d"),
        rows.stream().map(ClientFileCheckComparison.Row::fileName).toList());
    assertEquals(
        List.of(
            ClientFileCheckComparison.Kind.ONLY_REMOTE,
            ClientFileCheckComparison.Kind.SAME,
            ClientFileCheckComparison.Kind.ONLY_LOCAL,
            ClientFileCheckComparison.Kind.HASH_CHANGED),
        rows.stream().map(ClientFileCheckComparison.Row::kind).toList());
  }
}

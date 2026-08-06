package com.mo.economy_system.common.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.check.ClientFileCheckEntry;
import com.mo.economy_system.common.check.ClientFileCheckResult;
import com.mo.economy_system.common.check.ClientFileCheckStatus;
import com.mo.economy_system.common.check.ClientFileCheckType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClientFileCheckManifestAuthorizationStoreTest {
  @Test
  void replacementIsAtomicAndCapacityFailureLeavesNoPartialScope() {
    var store = new ClientFileCheckManifestAuthorizationStore(2, 2, 2, 2);
    UUID target = UUID.randomUUID();
    UUID requester = UUID.randomUUID();
    assertEquals(ClientFileCheckManifestAuthorizationStore.ReplaceResult.INSTALLED,
        store.replace(target, requester, result("old.jar"), 1));
    assertEquals(ClientFileCheckManifestAuthorizationStore.ReplaceResult.CAPACITY_REJECTED,
        store.replace(target, requester, result("a.jar", "b.jar", "c.jar"), 2));
    assertEquals(0, store.size(2));
    assertMissing(store, target, requester, "old.jar", 2);
    assertMissing(store, target, requester, "a.jar", 2);
  }

  @Test
  void duplicateFileNamesAreRejectedByTheResultBoundary() {
    assertThrows(IllegalArgumentException.class,
        () -> new ClientFileCheckResult(
            1,
            ClientFileCheckStatus.SUCCESS,
            ClientFileCheckType.MODS,
            List.of(entry("same.jar", 0), entry("same.jar", 1)),
            List.of(),
            null));
  }

  @Test
  void enforcesScopeTargetRequesterAndGlobalLimits() {
    UUID target = UUID.randomUUID();
    UUID requester = UUID.randomUUID();

    var scope = new ClientFileCheckManifestAuthorizationStore(10, 10, 10, 1);
    assertEquals(ClientFileCheckManifestAuthorizationStore.ReplaceResult.CAPACITY_REJECTED,
        scope.replace(target, requester, result("a", "b"), 1));

    var perTarget = new ClientFileCheckManifestAuthorizationStore(10, 1, 10, 10);
    assertEquals(ClientFileCheckManifestAuthorizationStore.ReplaceResult.INSTALLED,
        perTarget.replace(target, requester, result("a"), 1));
    assertEquals(ClientFileCheckManifestAuthorizationStore.ReplaceResult.CAPACITY_REJECTED,
        perTarget.replace(target, UUID.randomUUID(), result("b"), 2));

    var perRequester = new ClientFileCheckManifestAuthorizationStore(10, 10, 1, 10);
    assertEquals(ClientFileCheckManifestAuthorizationStore.ReplaceResult.INSTALLED,
        perRequester.replace(target, requester, result("a"), 1));
    assertEquals(ClientFileCheckManifestAuthorizationStore.ReplaceResult.CAPACITY_REJECTED,
        perRequester.replace(UUID.randomUUID(), requester, result("b"), 2));

    var global = new ClientFileCheckManifestAuthorizationStore(1, 10, 10, 10);
    assertEquals(ClientFileCheckManifestAuthorizationStore.ReplaceResult.INSTALLED,
        global.replace(target, requester, result("a"), 1));
    assertEquals(ClientFileCheckManifestAuthorizationStore.ReplaceResult.CAPACITY_REJECTED,
        global.replace(UUID.randomUUID(), UUID.randomUUID(), result("b"), 2));
  }

  @Test
  void failureClearsOldScopeAndTickRollbackClearsEverything() {
    var store = new ClientFileCheckManifestAuthorizationStore();
    UUID target = UUID.randomUUID();
    UUID requester = UUID.randomUUID();
    store.replace(target, requester, result("a.jar"), 100);
    assertEquals(ClientFileCheckManifestAuthorizationStore.ReplaceResult.CLEARED_NO_ENTRIES,
        store.replace(target, requester,
            ClientFileCheckResult.failed(ClientFileCheckType.MODS, "INVALID_RESULT"), 101));
    assertEquals(0, store.size(101));
    store.replace(target, requester, result("b.jar"), 200);
    assertEquals(0, store.size(199));
  }

  @Test
  void successTruncatedAndExpiryPreserveExactEntries() {
    var store = new ClientFileCheckManifestAuthorizationStore(4);
    UUID target = UUID.randomUUID();
    UUID requester = UUID.randomUUID();
    store.replace(target, requester, result("a.jar"), 1);
    assertPresent(store, target, requester, "a.jar", 1);
    var truncated = new ClientFileCheckResult(1, ClientFileCheckStatus.TRUNCATED,
        ClientFileCheckType.MODS, List.of(entry("b.jar", 2)), List.of(), "FILE_LIMIT");
    store.replace(target, requester, truncated, 2);
    assertMissing(store, target, requester, "a.jar", 2);
    assertPresent(store, target, requester, "b.jar", 2);
    assertEquals(0, store.size(2 + com.mo.economy_system.common.network.EconomyNetworkLimits.FILE_TRANSFER_TTL_TICKS));
  }

  private static ClientFileCheckResult result(String... files) {
    return new ClientFileCheckResult(1, ClientFileCheckStatus.SUCCESS, ClientFileCheckType.MODS,
        java.util.stream.IntStream.range(0, files.length)
            .mapToObj(index -> entry(files[index], index)).toList(), List.of(), null);
  }

  private static ClientFileCheckEntry entry(String file, int marker) {
    return new ClientFileCheckEntry(file, marker, Integer.toHexString(marker).repeat(64).substring(0, 64));
  }

  private static ClientFileCheckManifestAuthorizationStore.Key key(
      UUID target, UUID requester, String file) {
    return new ClientFileCheckManifestAuthorizationStore.Key(
        target, requester, ClientFileCheckType.MODS, file);
  }

  private static void assertPresent(ClientFileCheckManifestAuthorizationStore store,
                                    UUID target, UUID requester, String file, long tick) {
    assertTrue(store.find(key(target, requester, file), tick).isPresent());
  }

  private static void assertMissing(ClientFileCheckManifestAuthorizationStore store,
                                    UUID target, UUID requester, String file, long tick) {
    assertTrue(store.find(key(target, requester, file), tick).isEmpty());
  }
}

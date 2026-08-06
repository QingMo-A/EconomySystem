package com.mo.economy_system.common.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Source-level parity checks for the two loader adapters' lifecycle wiring. */
class CheckedFileTransferAdapterSourceTest {
  @Test
  void forgeCapturesArrivalForAllInboundTransferMessages() throws Exception {
    Path root = repositoryRoot();
    String handlers = read(root.resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/network/Forge1201CheckedFileTransferHandlers.java"));
    assertEquals(3, occurrences(handlers, "captureArrival(context.getNetworkManager())"));
    assertTrue(handlers.contains("context.enqueueWork"));
    assertNoLegacyPacketClasses(handlers);

    String events = read(root.resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/client/Forge1201ClientFileCheckEvents.java"));
    assertTrue(events.contains("TickEvent.ClientTickEvent"));
    assertTrue(events.contains("event.phase != TickEvent.Phase.END"));
    assertEquals(1, occurrences(events, "transfers().tick(System.nanoTime())"));
    assertTrue(events.contains("catch (RuntimeException"));
    assertFalse(events.contains("catch (Error"));
  }

  @Test
  void neoForgeCapturesArrivalForAllInboundTransferMessages() throws Exception {
    Path root = repositoryRoot();
    String handlers = read(root.resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/protocol/NeoForge1211CheckedFileTransferHandlers.java"));
    assertEquals(3, occurrences(handlers, "captureArrival(context.connection())"));
    assertTrue(occurrences(handlers, "context.enqueueWork") >= 3);
    assertNoLegacyPacketClasses(handlers);

    String events = read(root.resolve("src/main/java/com/mo/economy_system/client/ClientFileCheckEvents.java"));
    assertTrue(events.contains("ClientTickEvent.Post"));
    assertEquals(1, occurrences(events, "transfers().tick(System.nanoTime())"));
    assertTrue(events.contains("catch (RuntimeException"));
    assertFalse(events.contains("catch (Error"));
  }

  @Test
  void runtimeStopDoesNotRecreateTransferStateAndScreensCannotMutateArtifactsDirectly()
      throws Exception {
    Path root = repositoryRoot();
    for (String relative : new String[] {
      "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/client/Forge1201ClientFileCheckClientRuntime.java",
      "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client/NeoForge1211ClientFileCheckClientRuntime.java"
    }) {
      String runtime = read(root.resolve(relative));
      int stop = runtime.indexOf("void stop()");
      assertTrue(stop >= 0, relative);
      String stopBody = runtime.substring(stop);
      assertTrue(stopBody.contains("transfers.close()"), relative);
      assertFalse(stopBody.contains("new CheckedFileTransferClientCoordinator"), relative);
    }
    for (String relative : new String[] {
      "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/network/Forge1201CheckedFileTransferClient.java",
      "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client/Screen_CheckedFileTransferResult.java"
    }) {
      String screen = read(root.resolve(relative));
      assertFalse(screen.contains("artifact.discard("), relative);
      assertFalse(screen.contains("save(artifact"), relative);
      assertFalse(screen.contains("Files.move("), relative);
    }
  }

  @Test
  void targetAdaptersDoNotUseRawTempPathCreationOrProtocol31Migration() throws Exception {
    Path root = repositoryRoot();
    String forge = read(root.resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/network/Forge1201CheckedFileTransferClient.java"));
    String neo = read(root.resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client/Screen_CheckedFileTransferResult.java"));
    assertFalse(forge.contains("Files.createDirectories(directory)"));
    assertFalse(forge.contains("Files.newByteChannel(path"));
    assertFalse(neo.contains("Files.createDirectories(directory)"));
    assertFalse(neo.contains("Files.newByteChannel(path"));
    assertNoLegacyPacketClasses(forge + neo);
  }

  private static void assertNoLegacyPacketClasses(String source) {
    for (String name : new String[] {
      "Packet_Get", "Packet_GetResultRequest", "Packet_GetResultResponse", "Packet_Chunk",
      "Packet_ChunkResponse"
    }) assertFalse(source.contains(name), name);
  }

  private static int occurrences(String source, String needle) {
    int count = 0;
    for (int index = 0; (index = source.indexOf(needle, index)) >= 0; index += needle.length()) count++;
    return count;
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

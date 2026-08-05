package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.network.CheckedFileTransferChunkResponseMessage;
import com.mo.economy_system.common.network.CheckedFileTransferControlResponseMessage;
import com.mo.economy_system.common.transfer.CheckedFileTransferClientCoordinator;
import net.minecraft.client.Minecraft;

/** Thin Minecraft adapter; all incoming ownership and validation lives in common. */
final class CheckedFileTransferIncomingRuntime {
  private CheckedFileTransferIncomingRuntime() {}

  static void control(CheckedFileTransferControlResponseMessage message) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null || minecraft.getConnection() == null) return;
    var coordinator = NeoForge1211ClientFileCheckClientRuntime.transfers();
    var result = coordinator.control(message,
        minecraft.gameDirectory.toPath().resolve("economy_system").resolve("transfer-temp"),
        System.nanoTime());
    if (result == CheckedFileTransferClientCoordinator.IncomingResult.COMPLETE
        && coordinator.completedArtifact() != null) {
      minecraft.setScreen(new Screen_CheckedFileTransferResult(coordinator.completedArtifact()));
    }
  }

  static void chunk(CheckedFileTransferChunkResponseMessage message) {
    NeoForge1211ClientFileCheckClientRuntime.transfers().chunk(message);
  }

  static void clear() {
    NeoForge1211ClientFileCheckClientRuntime.transfers().invalidateSession();
  }
}

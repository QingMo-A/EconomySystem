package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.check.ClientFileCheckRequestStore;
import com.mo.economy_system.common.check.ClientFileCheckRequestStoreRegistry;
import net.minecraft.server.MinecraftServer;
import com.mo.economy_system.common.transfer.CheckedFileTransferStoreRegistry;

public final class NeoForge1211ClientFileCheckRuntime {
  private static final ClientFileCheckRequestStoreRegistry<MinecraftServer> STORES =
      new ClientFileCheckRequestStoreRegistry<>();
  private static final CheckedFileTransferStoreRegistry<MinecraftServer> TRANSFERS = new CheckedFileTransferStoreRegistry<>();

  private NeoForge1211ClientFileCheckRuntime() {}

  public static ClientFileCheckRequestStore store(MinecraftServer server) {
    return STORES.get(server);
  }
  public static CheckedFileTransferStoreRegistry.Stores transfers(MinecraftServer server) { return TRANSFERS.get(server); }
}

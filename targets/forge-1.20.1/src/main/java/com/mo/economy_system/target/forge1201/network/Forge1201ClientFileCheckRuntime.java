package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.check.ClientFileCheckRequestStore;
import com.mo.economy_system.common.check.ClientFileCheckRequestStoreRegistry;
import net.minecraft.server.MinecraftServer;
import com.mo.economy_system.common.transfer.CheckedFileTransferStoreRegistry;

public final class Forge1201ClientFileCheckRuntime {
  private static final ClientFileCheckRequestStoreRegistry<MinecraftServer> STORES =
      new ClientFileCheckRequestStoreRegistry<>();
  private static final CheckedFileTransferStoreRegistry<MinecraftServer> TRANSFERS = new CheckedFileTransferStoreRegistry<>();

  private Forge1201ClientFileCheckRuntime() {}

  public static ClientFileCheckRequestStore store(MinecraftServer server) {
    return STORES.get(server);
  }
  public static CheckedFileTransferStoreRegistry.Stores transfers(MinecraftServer server) { return TRANSFERS.get(server); }
}

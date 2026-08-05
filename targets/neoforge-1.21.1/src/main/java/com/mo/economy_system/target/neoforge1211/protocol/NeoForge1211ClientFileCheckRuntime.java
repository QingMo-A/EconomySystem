package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.check.ClientFileCheckRequestStore;
import com.mo.economy_system.common.check.ClientFileCheckRequestStoreRegistry;
import net.minecraft.server.MinecraftServer;

public final class NeoForge1211ClientFileCheckRuntime {
  private static final ClientFileCheckRequestStoreRegistry<MinecraftServer> STORES =
      new ClientFileCheckRequestStoreRegistry<>();

  private NeoForge1211ClientFileCheckRuntime() {}

  public static ClientFileCheckRequestStore store(MinecraftServer server) {
    return STORES.get(server);
  }
}

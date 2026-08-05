package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.check.ClientFileCheckRequestStore;
import com.mo.economy_system.common.check.ClientFileCheckRequestStoreRegistry;
import net.minecraft.server.MinecraftServer;

public final class Forge1201ClientFileCheckRuntime {
  private static final ClientFileCheckRequestStoreRegistry<MinecraftServer> STORES =
      new ClientFileCheckRequestStoreRegistry<>();

  private Forge1201ClientFileCheckRuntime() {}

  public static ClientFileCheckRequestStore store(MinecraftServer server) {
    return STORES.get(server);
  }
}

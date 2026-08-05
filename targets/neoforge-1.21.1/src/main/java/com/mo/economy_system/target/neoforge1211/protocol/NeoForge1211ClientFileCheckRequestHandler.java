package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.network.ClientFileCheckRequestMessage;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class NeoForge1211ClientFileCheckRequestHandler {
  private NeoForge1211ClientFileCheckRequestHandler() {}

  public static void handle(ClientFileCheckRequestMessage message, IPayloadContext context) {
    context.enqueueWork(() -> NeoForge1211ClientFileCheckPhysicalDispatch.request(message));
  }
}

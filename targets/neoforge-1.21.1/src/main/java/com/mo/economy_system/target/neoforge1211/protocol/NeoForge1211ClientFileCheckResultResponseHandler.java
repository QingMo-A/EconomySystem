package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.network.ClientFileCheckResultResponseMessage;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class NeoForge1211ClientFileCheckResultResponseHandler {
  private NeoForge1211ClientFileCheckResultResponseHandler() {}

  public static void handle(ClientFileCheckResultResponseMessage message, IPayloadContext context) {
    context.enqueueWork(() -> NeoForge1211ClientFileCheckPhysicalDispatch.response(message));
  }
}

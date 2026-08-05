package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.network.ClientFileCheckRequestMessage;
import java.util.function.Supplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

final class Forge1201ClientFileCheckRequestHandler {
  private Forge1201ClientFileCheckRequestHandler() {}

  static void handle(
      ClientFileCheckRequestMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    DistExecutor.unsafeRunWhenOn(
        Dist.CLIENT, () -> () -> Forge1201ClientFileCheckScreens.openConsent(message));
    context.setPacketHandled(true);
  }
}

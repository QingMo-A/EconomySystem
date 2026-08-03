package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import java.util.function.Supplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Physical-side-safe indirection; this class has no Minecraft client linkage. */
final class Forge1201TerritoryDataClientDispatch {
  private Forge1201TerritoryDataClientDispatch() {}

  static void handle(TerritoryDataResponseMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
        () -> () -> Forge1201TerritoryDataClientHandler.apply(message));
    context.setPacketHandled(true);
  }
}

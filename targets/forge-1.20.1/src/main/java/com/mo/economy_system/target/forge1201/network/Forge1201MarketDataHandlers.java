package com.mo.economy_system.target.forge1201.network;
import com.mo.economy_system.common.client.ClientMarketState;
import com.mo.economy_system.common.market.MarketDataQueryService;
import com.mo.economy_system.common.network.*;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
final class Forge1201MarketDataHandlers {
 private static final Logger LOGGER=LogUtils.getLogger();
 static void request(MarketDataRequestMessage m,Supplier<NetworkEvent.Context> s){NetworkEvent.Context c=s.get();ServerPlayer p=c.getSender();if(p!=null){try{var data=MarketSavedData.getInstance(p.serverLevel());var response=MarketDataQueryService.query(data.getView(),p.getUUID(),m);Forge1201NetworkChannel.sendToPlayer(p,response);}catch(RuntimeException exception){LOGGER.error("Failed to serve market data request player={} request={}",p.getUUID(),m.requestId(),exception);}}c.setPacketHandled(true);}
 static void response(MarketDataResponseMessage m,Supplier<NetworkEvent.Context> s){ClientMarketState.apply(m);s.get().setPacketHandled(true);}
}

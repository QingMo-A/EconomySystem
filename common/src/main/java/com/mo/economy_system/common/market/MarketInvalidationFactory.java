package com.mo.economy_system.common.market;
import com.mo.economy_system.common.network.MarketDataResponseMessage;
public final class MarketInvalidationFactory{private MarketInvalidationFactory(){}public static MarketDataResponseMessage create(MarketLedgerView view){int sales=0,demand=0;for(MarketOrder order:view.orders())if(order.type()==MarketOrderType.SALES)sales++;else demand++;return MarketDataResponseMessage.invalidated(view.revision(),sales,demand);}}

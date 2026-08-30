package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.network.EconomyMessages;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import com.mo.economy_system.protocol.EconomyMessageRegistry;
import com.mo.economy_system.protocol.EconomyMessageSpec;
import com.mo.economy_system.protocol.EconomyMessageType;
import com.mo.economy_system.protocol.EconomyProtocol;

/** Binds the transitional NeoForge payload classes to the common manifest. */
public final class NeoForge1211MessageBindings {
  private static final EconomyMessageRegistry REGISTRY = createRegistry();

  private NeoForge1211MessageBindings() {}

  public static EconomyMessageRegistry registry() {
    return REGISTRY;
  }

  private static EconomyMessageRegistry createRegistry() {
    EconomyMessageRegistry registry = new EconomyMessageRegistry();
    registry.register(EconomyMessages.BALANCE_REQUEST);
    registry.register(EconomyMessages.BALANCE_RESPONSE);
    registry.register(EconomyMessages.BALANCE_LOG_REQUEST);
    registry.register(EconomyMessages.BALANCE_LOG_RESPONSE);
    registry.register(EconomyMessages.TRANSFER);
    registry.register(EconomyMessages.SHOP_DATA_REQUEST);
    registry.register(EconomyMessages.SHOP_DATA_RESPONSE);
    registry.register(EconomyMessages.SHOP_BUY_ITEM);
    registry.register(EconomyMessages.CREATE_SALES_ORDER);
    registry.register(EconomyMessages.CREATE_DEMAND_ORDER);
    registry.register(EconomyMessages.MARKET_DATA_REQUEST);
    registry.register(EconomyMessages.MARKET_DATA_RESPONSE);
    registry.register(EconomyMessages.PURCHASE_SALES_ORDER);
    registry.register(EconomyMessages.CONFIRM_DEMAND_ORDER);
    registry.register(EconomyMessages.DELIVER_DEMAND_ORDER);
    registry.register(EconomyMessages.REMOVE_SALES_ORDER);
    registry.register(EconomyMessages.REMOVE_DEMAND_ORDER);
    registry.register(EconomyMessages.TERRITORY_DATA_REQUEST);
    registry.register(EconomyMessages.TERRITORY_DATA_RESPONSE);
    registry.register(EconomyMessages.TELEPORT_TO_TERRITORY);
    registry.register(EconomyMessages.INVITE_PLAYER);
    registry.register(EconomyMessages.REMOVE_TERRITORY);
    registry.register(EconomyMessages.REMOVE_PLAYER);
    registry.register(EconomyMessages.CHECK);
    registry.register(EconomyMessages.CHECK_RESULT_REQUEST);
    registry.register(EconomyMessages.CHECK_RESULT_RESPONSE);
    registry.register(EconomyMessages.GET);
    registry.register(EconomyMessages.GET_RESULT_REQUEST);
    registry.register(EconomyMessages.GET_RESULT_RESPONSE);
    registry.register(EconomyMessages.CHUNK);
    registry.register(EconomyMessages.CHUNK_RESPONSE);
    registry.register(EconomyMessages.DELIVERY_BOX_DATA_REQUEST);
    registry.register(EconomyMessages.DELIVERY_BOX_DATA_RESPONSE);
    registry.register(EconomyMessages.DELIVERY_BOX_CLAIM_ITEM);
    registry.register(EconomyMessages.SERVER_PLAYER_LIST_REQUEST);
    registry.register(EconomyMessages.SERVER_PLAYER_LIST_RESPONSE);
    registry.register(EconomyMessages.MODIFY_MODE);
    registry.register(EconomyMessages.UNLOCK_TERRITORY_BUFF);
    registry.register(EconomyMessages.UPGRADE_TERRITORY_BUFF);
    registry.register(EconomyMessages.SINGLE_TERRITORY_DATA_REQUEST);
    registry.register(EconomyMessages.SINGLE_TERRITORY_DATA_RESPONSE);
    registry.register(EconomyMessages.UPDATE_TERRITORY_PERMISSION);
    registry.register(EconomyMessages.TRANSFER_TERRITORY_OWNERSHIP);
    registry.register(EconomyMessages.UPDATE_TERRITORY_RULE);
    registry.register(EconomyMessages.MAILBOX_DATA_REQUEST);
    registry.register(EconomyMessages.MAILBOX_DATA_RESPONSE);
    registry.register(EconomyMessages.MAILBOX_MARK_READ);
    registry.register(EconomyMessages.MAILBOX_DELETE);
    registry.register(EconomyMessages.MAILBOX_CLAIM_ATTACHMENT);
    registry.register(EconomyMessages.MAILBOX_CLAIM_ALL);
    registry.register(EconomyMessages.MAILBOX_SEND_PLAYER);
    registry.register(EconomyMessages.MAILBOX_SEND_RESULT);
    registry.register(EconomyMessages.MAILBOX_NOTIFICATION);
    registry.register(EconomyMessages.COMMISSION_DATA_REQUEST);
    registry.register(EconomyMessages.COMMISSION_DATA_RESPONSE);
    registry.register(EconomyMessages.COMMISSION_SUBMIT);
    registry.register(EconomyMessages.COMMISSION_ACTION_RESPONSE);
    EconomyProtocol.validateBindings(registry.values());
    registry.freeze();
    return registry;
  }

  private static <T extends EconomyNetworkMessage> void bind(
      EconomyMessageRegistry registry, EconomyMessageSpec spec, Class<T> messageClass) {
    registry.register(new EconomyMessageType<>(spec, messageClass));
  }
}

package com.mo.economy_system.target.neoforge1211.protocol;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.network.*;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import com.mo.economy_system.network.DeliveryBoxWireCodec;
import com.mo.economy_system.network.TerritoryManagementWireCodec;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import com.mo.economy_system.platform.network.WireBuffer;
import com.mo.economy_system.protocol.EconomyMessageType;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

class NeoForge1211FinalBridgeProtocolTest {
  private static final UUID TERRITORY =
      UUID.fromString("00112233-4455-6677-8899-aabbccddeeff");
  private static final UUID TARGET =
      UUID.fromString("ffeeddcc-bbaa-9988-7766-554433221100");

  @Test
  void allFortyFourBindingsUseLoaderNeutralMessagesAndHaveCodecs() {
    var values = NeoForge1211MessageBindings.registry().values();
    assertEquals(44, values.size());
    for (EconomyMessageType<?> value : values) {
      assertTrue(
          value.messageClass().getPackageName().equals("com.mo.economy_system.common.network"),
          value.messageClass().getName());
      assertTrue(NeoForge1211MessageCodecs.supports(value), value.id());
    }
  }

  @Test
  void finalBridgeTargetCodecsMatchSharedBytes() {
    assertParity(
        EconomyMessages.DELIVERY_BOX_DATA_REQUEST,
        new DeliveryBoxDataRequestMessage(1),
        DeliveryBoxWireCodec::encodeRequest);
    assertParity(
        EconomyMessages.DELIVERY_BOX_DATA_RESPONSE,
        DeliveryBoxDataResponseMessage.error(2),
        DeliveryBoxWireCodec::encodeResponse);
    assertParity(
        EconomyMessages.DELIVERY_BOX_CLAIM_ITEM,
        new DeliveryBoxClaimMessage(TARGET, 3),
        DeliveryBoxWireCodec::encodeClaim);
    assertParity(
        EconomyMessages.MODIFY_MODE,
        new ModifyTerritoryModeMessage(TERRITORY),
        TerritoryManagementWireCodec::encodeModifyMode);
    assertParity(
        EconomyMessages.UNLOCK_TERRITORY_BUFF,
        new UnlockTerritoryBuffMessage(TERRITORY, "economy_system:speed"),
        TerritoryManagementWireCodec::encodeUnlockBuff);
    assertParity(
        EconomyMessages.UPGRADE_TERRITORY_BUFF,
        new UpgradeTerritoryBuffMessage(TERRITORY, "economy_system:speed"),
        TerritoryManagementWireCodec::encodeUpgradeBuff);
    assertParity(
        EconomyMessages.SINGLE_TERRITORY_DATA_REQUEST,
        new SingleTerritoryDataRequestMessage(TERRITORY, 4),
        TerritoryManagementWireCodec::encodeSingleRequest);
    assertParity(
        EconomyMessages.SINGLE_TERRITORY_DATA_RESPONSE,
        SingleTerritoryDataResponseMessage.empty(
            SingleTerritoryDataResponseKind.NOT_FOUND, 4),
        TerritoryManagementWireCodec::encodeSingleResponse);
    assertParity(
        EconomyMessages.UPDATE_TERRITORY_PERMISSION,
        new UpdateTerritoryPermissionMessage(TERRITORY, TARGET, true),
        TerritoryManagementWireCodec::encodePermission);
    assertParity(
        EconomyMessages.TRANSFER_TERRITORY_OWNERSHIP,
        new TransferTerritoryOwnershipMessage(TERRITORY, TARGET),
        TerritoryManagementWireCodec::encodeTransfer);
    assertParity(
        EconomyMessages.UPDATE_TERRITORY_RULE,
        new UpdateTerritoryRuleMessage(
            TERRITORY, RuleAction.OPEN_CONTAINER, RuleLevel.OWNER_ONLY),
        TerritoryManagementWireCodec::encodeRule);
  }

  private static <T extends EconomyNetworkMessage> void assertParity(
      EconomyMessageType<T> type, T message, Encoder<T> sharedEncoder) {
    FriendlyByteBuf shared = new FriendlyByteBuf(Unpooled.buffer());
    RegistryFriendlyByteBuf target = new RegistryFriendlyByteBuf(Unpooled.buffer(), null);
    try {
      sharedEncoder.encode(message, NeoForge1211WireBuffer.wrap(shared));
      NeoForge1211MessageCodecs.codec(type).encode(message, target);
      assertArrayEquals(
          ByteBufUtil.getBytes(shared, 0, shared.readableBytes(), false),
          ByteBufUtil.getBytes(target, 0, target.readableBytes(), false),
          type.id());
      assertEquals(message, NeoForge1211MessageCodecs.codec(type).decode(target));
    } finally {
      shared.release();
      target.release();
    }
  }

  @FunctionalInterface
  private interface Encoder<T> {
    void encode(T message, WireBuffer buffer);
  }
}

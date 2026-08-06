package com.mo.economy_system.common.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Guards the final 31-43 migration boundary against legacy packet regressions. */
class BridgeCompletionSourceTest {
  private static final List<String> TYPES = List.of(
      "DELIVERY_BOX_DATA_REQUEST",
      "DELIVERY_BOX_DATA_RESPONSE",
      "DELIVERY_BOX_CLAIM_ITEM",
      "MODIFY_MODE",
      "UNLOCK_TERRITORY_BUFF",
      "UPGRADE_TERRITORY_BUFF",
      "SINGLE_TERRITORY_DATA_REQUEST",
      "SINGLE_TERRITORY_DATA_RESPONSE",
      "UPDATE_TERRITORY_PERMISSION",
      "TRANSFER_TERRITORY_OWNERSHIP",
      "UPDATE_TERRITORY_RULE");

  @Test
  void forgeRegistersAndDispatchesEveryFinalBridgeMessage() throws Exception {
    Path root = repositoryRoot();
    String channel = read(root.resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/network/Forge1201NetworkChannel.java"));
    String bridge = read(root.resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/network/Forge1201NetworkBridge.java"));
    assertEquals(44, channel.split("\\.messageBuilder\\(", -1).length - 1);
    for (String type : TYPES) {
      assertTrue(channel.contains("EconomyMessages." + type + ".discriminator()"), type);
    }
    for (String codec : List.of(
        "DeliveryBoxWireCodec::encodeRequest",
        "DeliveryBoxWireCodec::decodeRequest",
        "DeliveryBoxWireCodec::encodeResponse",
        "DeliveryBoxWireCodec::decodeResponse",
        "DeliveryBoxWireCodec::encodeClaim",
        "DeliveryBoxWireCodec::decodeClaim",
        "TerritoryManagementWireCodec::encodeModifyMode",
        "TerritoryManagementWireCodec::decodeModifyMode",
        "TerritoryManagementWireCodec::encodeUnlockBuff",
        "TerritoryManagementWireCodec::decodeUnlockBuff",
        "TerritoryManagementWireCodec::encodeUpgradeBuff",
        "TerritoryManagementWireCodec::decodeUpgradeBuff",
        "TerritoryManagementWireCodec::encodeSingleRequest",
        "TerritoryManagementWireCodec::decodeSingleRequest",
        "TerritoryManagementWireCodec::encodeSingleResponse",
        "TerritoryManagementWireCodec::decodeSingleResponse",
        "TerritoryManagementWireCodec::encodePermission",
        "TerritoryManagementWireCodec::decodePermission",
        "TerritoryManagementWireCodec::encodeTransfer",
        "TerritoryManagementWireCodec::decodeTransfer",
        "TerritoryManagementWireCodec::encodeRule",
        "TerritoryManagementWireCodec::decodeRule")) {
      assertTrue(channel.contains(codec), codec);
    }
    for (String message : List.of(
        "DeliveryBoxDataRequestMessage",
        "DeliveryBoxClaimMessage",
        "ModifyTerritoryModeMessage",
        "UnlockTerritoryBuffMessage",
        "UpgradeTerritoryBuffMessage",
        "SingleTerritoryDataRequestMessage",
        "UpdateTerritoryPermissionMessage",
        "TransferTerritoryOwnershipMessage",
        "UpdateTerritoryRuleMessage")) {
      assertTrue(bridge.contains("message.getClass() == " + message + ".class"), message);
    }
    for (String message : List.of(
        "DeliveryBoxDataResponseMessage", "SingleTerritoryDataResponseMessage")) {
      assertTrue(bridge.contains("message.getClass() == " + message + ".class"), message);
    }
  }

  @Test
  void removedLegacyPacketSourcesCannotReturn() {
    Path packets = repositoryRoot().resolve("src/main/java/com/mo/economy_system/network/packets");
    for (String file : List.of(
        "economy_system/Packet_DeliveryBoxDataRequest.java",
        "economy_system/Packet_DeliveryBoxDataResponse.java",
        "economy_system/Packet_DeliveryBoxClaimItem.java",
        "territory_system/Packet_ModifyMode.java",
        "territory_system/Packet_UnlockTerritoryBuff.java",
        "territory_system/Packet_UpgradeTerritoryBuff.java",
        "territory_system/Packet_SingleTerritoryDataRequest.java",
        "territory_system/Packet_SingleTerritoryDataResponse.java",
        "territory_system/Packet_UpdateTerritoryPermission.java",
        "territory_system/Packet_TransferTerritoryOwnership.java",
        "territory_system/Packet_UpdateTerritoryRule.java")) {
      assertFalse(Files.exists(packets.resolve(file)), file);
    }
  }

  private static String read(Path path) throws Exception {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  private static Path repositoryRoot() {
    Path current = Path.of("").toAbsolutePath();
    while (current != null && !Files.exists(current.resolve("settings.gradle"))) {
      current = current.getParent();
    }
    if (current == null) throw new IllegalStateException("repository root not found");
    return current;
  }
}

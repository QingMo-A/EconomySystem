package com.mo.economy_system.common.network;

import com.mo.economy_system.common.check.ClientFileCheckType;
import com.mo.economy_system.common.check.ClientFileCheckValidation;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.Objects;
import java.util.UUID;

public record ClientFileCheckResultRequestMessage(
    String targetPlayerName,
    UUID targetPlayerId,
    String requesterPlayerName,
    UUID requesterPlayerId,
    ClientFileCheckType checkType,
    String resultJson)
    implements EconomyNetworkMessage {
  public ClientFileCheckResultRequestMessage {
    targetPlayerName = ClientFileCheckValidation.playerName(targetPlayerName);
    requesterPlayerName = ClientFileCheckValidation.playerName(requesterPlayerName);
    Objects.requireNonNull(targetPlayerId, "targetPlayerId");
    Objects.requireNonNull(requesterPlayerId, "requesterPlayerId");
    Objects.requireNonNull(checkType, "checkType");
    resultJson = ClientFileCheckValidation.resultJson(resultJson);
  }
}

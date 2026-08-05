package com.mo.economy_system.common.network;

import com.mo.economy_system.common.check.ClientFileCheckType;
import com.mo.economy_system.common.check.ClientFileCheckValidation;
import com.mo.economy_system.common.transfer.CheckedFileTransferValidation;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.UUID;

public record CheckedFileTransferRequestMessage(String targetPlayerName, UUID targetPlayerId,
    String requesterPlayerName, UUID requesterPlayerId, ClientFileCheckType checkType, String fileName)
    implements EconomyNetworkMessage {
  public CheckedFileTransferRequestMessage {
    targetPlayerName = ClientFileCheckValidation.playerName(targetPlayerName);
    if (targetPlayerId == null || requesterPlayerId == null) throw new NullPointerException("player id");
    requesterPlayerName = ClientFileCheckValidation.playerName(requesterPlayerName);
    checkType = CheckedFileTransferValidation.type(checkType);
    fileName = CheckedFileTransferValidation.fileName(fileName);
  }
}

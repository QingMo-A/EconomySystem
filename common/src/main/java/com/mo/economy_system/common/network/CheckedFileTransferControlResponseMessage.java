package com.mo.economy_system.common.network;
import com.mo.economy_system.common.check.ClientFileCheckType;
import com.mo.economy_system.common.check.ClientFileCheckValidation;
import com.mo.economy_system.common.transfer.CheckedFileTransferValidation;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.UUID;
public record CheckedFileTransferControlResponseMessage(String targetPlayerName, UUID targetPlayerId,
    String requesterPlayerName, UUID requesterPlayerId, ClientFileCheckType checkType, String fileName,
    String controlPayload) implements EconomyNetworkMessage {
  public CheckedFileTransferControlResponseMessage {
    targetPlayerName=ClientFileCheckValidation.playerName(targetPlayerName); requesterPlayerName=ClientFileCheckValidation.playerName(requesterPlayerName);
    if(targetPlayerId==null||requesterPlayerId==null) throw new NullPointerException("player id");
    checkType=CheckedFileTransferValidation.type(checkType); fileName=CheckedFileTransferValidation.fileName(fileName);
    if(controlPayload==null||controlPayload.length()>EconomyNetworkLimits.MAX_TRANSFER_CONTROL_JSON_CHARS) throw new IllegalArgumentException("control payload");
  }
}

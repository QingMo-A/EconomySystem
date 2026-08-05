package com.mo.economy_system.common.network;
import com.mo.economy_system.common.check.ClientFileCheckType;
import com.mo.economy_system.common.check.ClientFileCheckValidation;
import com.mo.economy_system.common.transfer.CheckedFileTransferValidation;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.UUID;
public record CheckedFileTransferChunkResponseMessage(String targetPlayerName, UUID targetPlayerId,
    String requesterPlayerName, UUID requesterPlayerId, ClientFileCheckType checkType, String fileName,
    UUID transferId, int chunkIndex, int totalChunks, String chunkData) implements EconomyNetworkMessage {
  public CheckedFileTransferChunkResponseMessage {
    targetPlayerName=ClientFileCheckValidation.playerName(targetPlayerName); requesterPlayerName=ClientFileCheckValidation.playerName(requesterPlayerName);
    if(targetPlayerId==null||requesterPlayerId==null||transferId==null) throw new NullPointerException("id");
    checkType=CheckedFileTransferValidation.type(checkType); fileName=CheckedFileTransferValidation.fileName(fileName);
    CheckedFileTransferChunkRequestMessage.validateChunk(chunkIndex,totalChunks,chunkData);
  }
}

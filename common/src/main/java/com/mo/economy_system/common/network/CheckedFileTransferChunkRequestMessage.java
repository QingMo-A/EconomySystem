package com.mo.economy_system.common.network;
import com.mo.economy_system.common.check.ClientFileCheckType;
import com.mo.economy_system.common.check.ClientFileCheckValidation;
import com.mo.economy_system.common.transfer.CheckedFileTransferValidation;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.UUID;
public record CheckedFileTransferChunkRequestMessage(String targetPlayerName, UUID targetPlayerId,
    String requesterPlayerName, UUID requesterPlayerId, ClientFileCheckType checkType, String fileName,
    UUID transferId, int chunkIndex, int totalChunks, String chunkData) implements EconomyNetworkMessage {
  public CheckedFileTransferChunkRequestMessage {
    targetPlayerName=ClientFileCheckValidation.playerName(targetPlayerName); requesterPlayerName=ClientFileCheckValidation.playerName(requesterPlayerName);
    if(targetPlayerId==null||requesterPlayerId==null||transferId==null) throw new NullPointerException("id");
    checkType=CheckedFileTransferValidation.type(checkType); fileName=CheckedFileTransferValidation.fileName(fileName);
    validateChunk(chunkIndex,totalChunks,chunkData);
  }
  static void validateChunk(int index,int total,String data){if(index<0||total<1||total>EconomyNetworkLimits.MAX_TRANSFER_CHUNKS||index>=total||data==null||data.length()>EconomyNetworkLimits.MAX_TRANSFER_CHUNK_BASE64_CHARS)throw new IllegalArgumentException("chunk");}
}

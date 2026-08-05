package com.mo.economy_system.common.transfer;

import com.mo.economy_system.common.network.*;import java.nio.charset.StandardCharsets;
import java.util.*;

/** Authenticated server routing for protocols 27 and 29. */
public final class CheckedFileTransferRoutingService {
  public interface PlayerLookup{Object find(UUID id);} public interface Sender{void send(Object player,Object message);}
  private CheckedFileTransferRoutingService(){}
  public static CheckedFileTransferStore.Result control(CheckedFileTransferControlRequestMessage message,UUID authenticatedTarget,long tick,CheckedFileTransferStore store,PlayerLookup lookup,Sender sender){
    CheckedFileTransferStore.Key key=key(message.targetPlayerId(),message.requesterPlayerId(),message.checkType(),message.fileName());
    if(!authenticatedTarget.equals(message.targetPlayerId()))return CheckedFileTransferStore.Result.WRONG_TARGET;
    if(!store.metadataMatches(key,message.targetPlayerName(),message.requesterPlayerName(),tick)){store.discard(key,tick);return CheckedFileTransferStore.Result.INVALID_METADATA;}
    CheckedFileTransferControl control;try{control=CheckedFileTransferControlJsonCodec.decode(message.controlPayload());}catch(RuntimeException invalid){store.discard(key,tick);return CheckedFileTransferStore.Result.INVALID_METADATA;}
    Object requester=lookup.find(message.requesterPlayerId());if(requester==null){store.discard(key,tick);return CheckedFileTransferStore.Result.REQUESTER_OFFLINE;}
    if(control.status()!=CheckedFileTransferControlStatus.READY){store.discard(key,tick);sender.send(requester,response(message,control));return CheckedFileTransferStore.Result.COMPLETE;}
    CheckedFileTransferStore.Result result=store.ready(key,authenticatedTarget,control,tick);if(result==CheckedFileTransferStore.Result.READY||result==CheckedFileTransferStore.Result.COMPLETE){sender.send(requester,response(message,control));if(result==CheckedFileTransferStore.Result.COMPLETE)sender.send(requester,response(message,CheckedFileTransferControl.complete(control.transferId(),control.byteLength(),control.sha256())));}return result;
  }
  public static CheckedFileTransferStore.Result chunk(CheckedFileTransferChunkRequestMessage message,UUID authenticatedTarget,long tick,CheckedFileTransferStore store,PlayerLookup lookup,Sender sender){
    byte[] raw;try{raw=decodeCanonical(message.chunkData());}catch(RuntimeException invalid){store.discard(key(message.targetPlayerId(),message.requesterPlayerId(),message.checkType(),message.fileName()),tick);return CheckedFileTransferStore.Result.INVALID_CHUNK;}
    var key=key(message.targetPlayerId(),message.requesterPlayerId(),message.checkType(),message.fileName());if(!store.metadataMatches(key,message.targetPlayerName(),message.requesterPlayerName(),tick)){store.discard(key,tick);return CheckedFileTransferStore.Result.INVALID_METADATA;}var result=store.chunk(key,authenticatedTarget,message.transferId(),message.chunkIndex(),message.totalChunks(),raw,tick);if(result.result()==CheckedFileTransferStore.Result.FORWARD||result.result()==CheckedFileTransferStore.Result.COMPLETE){Object requester=lookup.find(message.requesterPlayerId());if(requester==null){store.discard(key,tick);return CheckedFileTransferStore.Result.REQUESTER_OFFLINE;}sender.send(requester,new CheckedFileTransferChunkResponseMessage(message.targetPlayerName(),message.targetPlayerId(),message.requesterPlayerName(),message.requesterPlayerId(),message.checkType(),message.fileName(),message.transferId(),message.chunkIndex(),message.totalChunks(),message.chunkData()));if(result.result()==CheckedFileTransferStore.Result.COMPLETE)sender.send(requester,new CheckedFileTransferControlResponseMessage(message.targetPlayerName(),message.targetPlayerId(),message.requesterPlayerName(),message.requesterPlayerId(),message.checkType(),message.fileName(),CheckedFileTransferControlJsonCodec.encode(CheckedFileTransferControl.complete(message.transferId(),result.byteLength(),result.sha256()))));}return result.result();
  }
  public static byte[] decodeCanonical(String encoded){if(encoded==null||encoded.length()>EconomyNetworkLimits.MAX_TRANSFER_CHUNK_BASE64_CHARS||encoded.chars().anyMatch(Character::isWhitespace))throw new IllegalArgumentException("base64");byte[] raw=Base64.getDecoder().decode(encoded);if(!Arrays.equals(Base64.getEncoder().encode(raw),encoded.getBytes(StandardCharsets.US_ASCII)))throw new IllegalArgumentException("canonical base64");return raw;}
  private static CheckedFileTransferStore.Key key(UUID t,UUID r,com.mo.economy_system.common.check.ClientFileCheckType type,String file){return new CheckedFileTransferStore.Key(t,r,type,file);}
  private static CheckedFileTransferControlResponseMessage response(CheckedFileTransferControlRequestMessage m,CheckedFileTransferControl c){return new CheckedFileTransferControlResponseMessage(m.targetPlayerName(),m.targetPlayerId(),m.requesterPlayerName(),m.requesterPlayerId(),m.checkType(),m.fileName(),CheckedFileTransferControlJsonCodec.encode(c));}
}

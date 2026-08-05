package com.mo.economy_system.network;

import com.mo.economy_system.common.check.ClientFileCheckType;
import com.mo.economy_system.common.network.*;
import com.mo.economy_system.common.transfer.CheckedFileTransferValidation;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;

/** Legacy-compatible fields with bounded, atomic buffer operations. */
public final class CheckedFileTransferWireCodec {
  private CheckedFileTransferWireCodec() {}
  public static void encodeRequest(CheckedFileTransferRequestMessage m,FriendlyByteBuf out){atomic(out,b->{base(b,m.targetPlayerName(),m.targetPlayerId(),m.requesterPlayerName(),m.requesterPlayerId(),m.checkType(),m.fileName());});}
  public static CheckedFileTransferRequestMessage decodeRequest(FriendlyByteBuf b){return decode(b,x->{Base v=base(x);return new CheckedFileTransferRequestMessage(v.tn,v.ti,v.rn,v.ri,v.type,v.file);});}
  public static void encodeControlRequest(CheckedFileTransferControlRequestMessage m,FriendlyByteBuf out){atomic(out,b->{base(b,m.targetPlayerName(),m.targetPlayerId(),m.requesterPlayerName(),m.requesterPlayerId(),m.checkType(),m.fileName());b.writeUtf(m.controlPayload(),EconomyNetworkLimits.MAX_TRANSFER_CONTROL_JSON_CHARS);});}
  public static CheckedFileTransferControlRequestMessage decodeControlRequest(FriendlyByteBuf b){return decode(b,x->{Base v=base(x);return new CheckedFileTransferControlRequestMessage(v.tn,v.ti,v.rn,v.ri,v.type,v.file,x.readUtf(EconomyNetworkLimits.MAX_TRANSFER_CONTROL_JSON_CHARS));});}
  public static void encodeControlResponse(CheckedFileTransferControlResponseMessage m,FriendlyByteBuf out){atomic(out,b->{base(b,m.targetPlayerName(),m.targetPlayerId(),m.requesterPlayerName(),m.requesterPlayerId(),m.checkType(),m.fileName());b.writeUtf(m.controlPayload(),EconomyNetworkLimits.MAX_TRANSFER_CONTROL_JSON_CHARS);});}
  public static CheckedFileTransferControlResponseMessage decodeControlResponse(FriendlyByteBuf b){return decode(b,x->{Base v=base(x);return new CheckedFileTransferControlResponseMessage(v.tn,v.ti,v.rn,v.ri,v.type,v.file,x.readUtf(EconomyNetworkLimits.MAX_TRANSFER_CONTROL_JSON_CHARS));});}
  public static void encodeChunkRequest(CheckedFileTransferChunkRequestMessage m,FriendlyByteBuf out){atomic(out,b->{base(b,m.targetPlayerName(),m.targetPlayerId(),m.requesterPlayerName(),m.requesterPlayerId(),m.checkType(),m.fileName());chunk(b,m.transferId(),m.chunkIndex(),m.totalChunks(),m.chunkData());});}
  public static CheckedFileTransferChunkRequestMessage decodeChunkRequest(FriendlyByteBuf b){return decode(b,x->{Base v=base(x);Chunk c=chunk(x);return new CheckedFileTransferChunkRequestMessage(v.tn,v.ti,v.rn,v.ri,v.type,v.file,c.id,c.index,c.total,c.data);});}
  public static void encodeChunkResponse(CheckedFileTransferChunkResponseMessage m,FriendlyByteBuf out){atomic(out,b->{base(b,m.targetPlayerName(),m.targetPlayerId(),m.requesterPlayerName(),m.requesterPlayerId(),m.checkType(),m.fileName());chunk(b,m.transferId(),m.chunkIndex(),m.totalChunks(),m.chunkData());});}
  public static CheckedFileTransferChunkResponseMessage decodeChunkResponse(FriendlyByteBuf b){return decode(b,x->{Base v=base(x);Chunk c=chunk(x);return new CheckedFileTransferChunkResponseMessage(v.tn,v.ti,v.rn,v.ri,v.type,v.file,c.id,c.index,c.total,c.data);});}
  private static void base(FriendlyByteBuf b,String tn,UUID ti,String rn,UUID ri,ClientFileCheckType type,String file){b.writeUtf(tn,EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH);b.writeUtf(CheckedFileTransferValidation.canonicalUuid(ti),36);b.writeUtf(rn,EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH);b.writeUtf(CheckedFileTransferValidation.canonicalUuid(ri),36);b.writeUtf(type.id(),32);b.writeUtf(file,EconomyNetworkLimits.MAX_TRANSFER_FILE_NAME_CHARS);}
  private static Base base(FriendlyByteBuf b){return new Base(b.readUtf(EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH),CheckedFileTransferValidation.canonicalUuid(b.readUtf(36)),b.readUtf(EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH),CheckedFileTransferValidation.canonicalUuid(b.readUtf(36)),ClientFileCheckType.fromId(b.readUtf(32)),b.readUtf(EconomyNetworkLimits.MAX_TRANSFER_FILE_NAME_CHARS));}
  private static void chunk(FriendlyByteBuf b,UUID id,int i,int total,String data){b.writeUtf(CheckedFileTransferValidation.canonicalUuid(id),36);b.writeInt(i);b.writeInt(total);b.writeUtf(data,EconomyNetworkLimits.MAX_TRANSFER_CHUNK_BASE64_CHARS);}
  private static Chunk chunk(FriendlyByteBuf b){return new Chunk(CheckedFileTransferValidation.canonicalUuid(b.readUtf(36)),b.readInt(),b.readInt(),b.readUtf(EconomyNetworkLimits.MAX_TRANSFER_CHUNK_BASE64_CHARS));}
  private static void atomic(FriendlyByteBuf out,Writer writer){FriendlyByteBuf tmp=new FriendlyByteBuf(Unpooled.buffer());try{writer.write(tmp);out.writeBytes(tmp,tmp.readerIndex(),tmp.readableBytes());}finally{tmp.release();}}
  private static <T>T decode(FriendlyByteBuf b,Function<FriendlyByteBuf,T> f){try{T v=f.apply(b);if(b.isReadable())throw new DecoderException("trailing transfer bytes");return v;}catch(RuntimeException e){if(e instanceof DecoderException)throw e;throw new DecoderException("invalid transfer payload",e);}}
  private interface Writer{void write(FriendlyByteBuf b);}
  private record Base(String tn,UUID ti,String rn,UUID ri,ClientFileCheckType type,String file){}
  private record Chunk(UUID id,int index,int total,String data){}
}

package com.mo.economy_system.common.transfer;

import com.mo.economy_system.common.check.ClientFileCheckValidation;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.util.Objects;
import java.util.UUID;

public record CheckedFileTransferControl(int schema, CheckedFileTransferControlStatus status,
    UUID transferId, long byteLength, String sha256, int rawChunkBytes, int totalChunks,
    String errorCode) {
  public CheckedFileTransferControl {
    if(schema!=1) throw new IllegalArgumentException("schema"); Objects.requireNonNull(status,"status");
    switch(status){
      case READY -> { Objects.requireNonNull(transferId,"transferId"); CheckedFileTransferValidation.sha256(sha256);
        if(errorCode!=null||rawChunkBytes!=EconomyNetworkLimits.TRANSFER_RAW_CHUNK_BYTES||totalChunks!=CheckedFileTransferValidation.totalChunks(byteLength,rawChunkBytes)) throw new IllegalArgumentException("ready"); }
      case COMPLETE -> { Objects.requireNonNull(transferId,"transferId"); CheckedFileTransferValidation.sha256(sha256);
        if(byteLength<0||byteLength>EconomyNetworkLimits.MAX_TRANSFER_FILE_BYTES||rawChunkBytes!=0||totalChunks!=0||errorCode!=null) throw new IllegalArgumentException("complete"); }
      case DECLINED, NOT_FOUND, FAILED -> { if(transferId!=null||sha256!=null||byteLength!=0||rawChunkBytes!=0||totalChunks!=0) throw new IllegalArgumentException("failure fields"); errorCode=ClientFileCheckValidation.errorCode(errorCode); }
    }
  }
  public static CheckedFileTransferControl ready(UUID id,long bytes,String hash){return new CheckedFileTransferControl(1,CheckedFileTransferControlStatus.READY,id,bytes,hash,EconomyNetworkLimits.TRANSFER_RAW_CHUNK_BYTES,CheckedFileTransferValidation.totalChunks(bytes,EconomyNetworkLimits.TRANSFER_RAW_CHUNK_BYTES),null);}
  public static CheckedFileTransferControl complete(UUID id,long bytes,String hash){return new CheckedFileTransferControl(1,CheckedFileTransferControlStatus.COMPLETE,id,bytes,hash,0,0,null);}
  public static CheckedFileTransferControl error(CheckedFileTransferControlStatus status,String code){if(status==CheckedFileTransferControlStatus.READY||status==CheckedFileTransferControlStatus.COMPLETE)throw new IllegalArgumentException("status");return new CheckedFileTransferControl(1,status,null,0,null,0,0,code);}
}

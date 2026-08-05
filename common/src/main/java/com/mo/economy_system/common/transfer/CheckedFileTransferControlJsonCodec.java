package com.mo.economy_system.common.transfer;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

public final class CheckedFileTransferControlJsonCodec {
  private static final Set<String> READY=Set.of("schema","status","transferId","byteLength","sha256","rawChunkBytes","totalChunks");
  private static final Set<String> COMPLETE=Set.of("schema","status","transferId","byteLength","sha256");
  private static final Set<String> ERROR=Set.of("schema","status","errorCode");
  private CheckedFileTransferControlJsonCodec(){}
  public static String encode(CheckedFileTransferControl value){JsonObject o=new JsonObject();o.addProperty("schema",1);o.addProperty("status",value.status().name());switch(value.status()){
    case READY->{o.addProperty("transferId",value.transferId().toString());o.addProperty("byteLength",value.byteLength());o.addProperty("sha256",value.sha256());o.addProperty("rawChunkBytes",value.rawChunkBytes());o.addProperty("totalChunks",value.totalChunks());}
    case COMPLETE->{o.addProperty("transferId",value.transferId().toString());o.addProperty("byteLength",value.byteLength());o.addProperty("sha256",value.sha256());}
    default->o.addProperty("errorCode",value.errorCode());}
    String encoded=o.toString();if(encoded.length()>EconomyNetworkLimits.MAX_TRANSFER_CONTROL_JSON_CHARS)throw new IllegalArgumentException("control length");return encoded;}
  public static CheckedFileTransferControl decode(String encoded){if(encoded==null||encoded.length()>EconomyNetworkLimits.MAX_TRANSFER_CONTROL_JSON_CHARS)throw new IllegalArgumentException("control length");unique(encoded);JsonElement parsed=JsonParser.parseString(encoded);if(!parsed.isJsonObject())throw new IllegalArgumentException("root");JsonObject o=parsed.getAsJsonObject();if(!integer(o,"schema").equals("1"))throw new IllegalArgumentException("schema");CheckedFileTransferControlStatus status=CheckedFileTransferControlStatus.valueOf(string(o,"status"));Set<String> expected=status==CheckedFileTransferControlStatus.READY?READY:status==CheckedFileTransferControlStatus.COMPLETE?COMPLETE:ERROR;if(!o.keySet().equals(expected))throw new IllegalArgumentException("fields");return switch(status){
    case READY->new CheckedFileTransferControl(1,status,CheckedFileTransferValidation.canonicalUuid(string(o,"transferId")),number(o,"byteLength"),string(o,"sha256"),(int)number(o,"rawChunkBytes"),(int)number(o,"totalChunks"),null);
    case COMPLETE->CheckedFileTransferControl.complete(CheckedFileTransferValidation.canonicalUuid(string(o,"transferId")),number(o,"byteLength"),string(o,"sha256"));
    default->CheckedFileTransferControl.error(status,string(o,"errorCode"));};}
  private static String string(JsonObject o,String n){JsonElement e=o.get(n);if(e==null||!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isString())throw new IllegalArgumentException(n);return e.getAsString();}
  private static String integer(JsonObject o,String n){JsonElement e=o.get(n);if(e==null||!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isNumber()||!e.toString().matches("0|[1-9][0-9]*"))throw new IllegalArgumentException(n);return e.toString();}
  private static long number(JsonObject o,String n){try{return Long.parseLong(integer(o,n));}catch(NumberFormatException ex){throw new IllegalArgumentException(n,ex);}}
  private static void unique(String text){try(JsonReader r=new JsonReader(new StringReader(text))){r.setLenient(false);read(r);if(r.peek()!=JsonToken.END_DOCUMENT)throw new IllegalArgumentException("trailing");}catch(Exception e){if(e instanceof IllegalArgumentException i)throw i;throw new IllegalArgumentException("json",e);}}
  private static void read(JsonReader r)throws Exception{switch(r.peek()){case BEGIN_OBJECT->{r.beginObject();Set<String>s=new HashSet<>();while(r.hasNext()){String n=r.nextName();if(!s.add(n))throw new IllegalArgumentException("duplicate");read(r);}r.endObject();}case BEGIN_ARRAY->{r.beginArray();while(r.hasNext())read(r);r.endArray();}case STRING,NUMBER->r.nextString();case BOOLEAN->r.nextBoolean();case NULL->r.nextNull();default->throw new IllegalArgumentException("token");}}
}

package com.mo.economy_system.common.check;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public final class ClientFileCheckComparison {
  public enum Kind {
    ONLY_REMOTE,
    ONLY_LOCAL,
    HASH_CHANGED,
    SAME
  }

  public record Row(String fileName, Kind kind) {}

  private ClientFileCheckComparison() {}

  public static List<Row> compare(ClientFileCheckResult remote, ClientFileCheckResult local) {
    if (remote.checkType() != local.checkType()) throw new IllegalArgumentException("check type");
    Map<String, ClientFileCheckEntry> remoteFiles = index(remote.files());
    Map<String, ClientFileCheckEntry> localFiles = index(local.files());
    TreeSet<String> names = new TreeSet<>();
    names.addAll(remoteFiles.keySet());
    names.addAll(localFiles.keySet());
    List<Row> rows = new ArrayList<>(names.size());
    for (String name : names) {
      ClientFileCheckEntry left = remoteFiles.get(name), right = localFiles.get(name);
      Kind kind =
          left == null
              ? Kind.ONLY_LOCAL
              : right == null
                  ? Kind.ONLY_REMOTE
                  : left.sha256().equals(right.sha256()) ? Kind.SAME : Kind.HASH_CHANGED;
      rows.add(new Row(name, kind));
    }
    return List.copyOf(rows);
  }

  private static Map<String, ClientFileCheckEntry> index(List<ClientFileCheckEntry> entries) {
    Map<String, ClientFileCheckEntry> result = new HashMap<>();
    for (ClientFileCheckEntry entry : entries)
      if (result.put(entry.fileName(), entry) != null)
        throw new IllegalArgumentException("duplicate file name");
    return result;
  }
}

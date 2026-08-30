package com.mo.economy_system.common.commission;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Loads the administrator-maintained commission catalog from a server-side JSON file. */
public final class CommissionCatalogConfigLoader {
  private CommissionCatalogConfigLoader() {}

  /**
   * Loads an existing catalog, or creates a complete built-in catalog file on first startup.
   * Existing files are never silently replaced: malformed JSON fails configuration/reload.
   */
  public static CommissionCatalog loadOrCreate(Path path) {
    Objects.requireNonNull(path, "path");
    if (Files.exists(path)) return read(path);
    try {
      Path parent = path.getParent();
      if (parent != null) Files.createDirectories(parent);
      try {
        Files.writeString(path, CommissionCatalogJsonCodec.encode(CommissionCatalogDefaults.create()),
            StandardCharsets.UTF_8);
      } catch (FileAlreadyExistsException race) {
        // Another server thread created the file between exists() and writeString().
        return read(path);
      }
      return CommissionCatalogDefaults.create();
    } catch (IOException failure) {
      throw new IllegalStateException("unable to create commission catalog: " + path, failure);
    }
  }

  private static CommissionCatalog read(Path path) {
    try {
      return CommissionCatalogJsonCodec.decode(Files.readString(path, StandardCharsets.UTF_8));
    } catch (IOException failure) {
      throw new IllegalStateException("unable to read commission catalog: " + path, failure);
    } catch (IllegalArgumentException failure) {
      throw new IllegalArgumentException("invalid commission catalog at " + path + ": "
          + failure.getMessage(), failure);
    }
  }
}

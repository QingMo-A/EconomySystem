package com.mo.economy_system.common.recycle;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Loads or creates the server-side recycling configuration without silent fallback. */
public final class RecycleConfigLoader {
  private RecycleConfigLoader() {}

  public static RecycleConfig loadOrCreate(Path path) {
    Objects.requireNonNull(path, "path");
    if (Files.exists(path)) return read(path);
    try {
      Path parent = path.getParent();
      if (parent != null) Files.createDirectories(parent);
      try {
        Files.writeString(path, RecycleConfigJsonCodec.encode(RecycleConfig.defaults()), StandardCharsets.UTF_8);
      } catch (FileAlreadyExistsException race) {
        return read(path);
      }
      return RecycleConfig.defaults();
    } catch (IOException failure) {
      throw new IllegalStateException("unable to create recycle config: " + path, failure);
    }
  }

  private static RecycleConfig read(Path path) {
    try {
      return RecycleConfigJsonCodec.decode(Files.readString(path, StandardCharsets.UTF_8));
    } catch (IOException failure) {
      throw new IllegalStateException("unable to read recycle config: " + path, failure);
    } catch (IllegalArgumentException failure) {
      throw new IllegalArgumentException("invalid recycle config at " + path + ": "
          + failure.getMessage(), failure);
    }
  }
}

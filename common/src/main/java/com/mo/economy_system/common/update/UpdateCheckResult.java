package com.mo.economy_system.common.update;

import java.util.Objects;

/** Version-policy result independent of HTTP and Minecraft APIs. */
public record UpdateCheckResult(
    Status status, SemanticVersion current, SemanticVersion latest, String downloadUrl) {
  public enum Status {
    CURRENT,
    UPDATE_AVAILABLE,
    INVALID_CURRENT_VERSION,
    INVALID_LATEST_VERSION
  }

  public UpdateCheckResult {
    Objects.requireNonNull(status, "status");
    if (status == Status.CURRENT || status == Status.UPDATE_AVAILABLE) {
      Objects.requireNonNull(current, "current");
      Objects.requireNonNull(latest, "latest");
      if (downloadUrl == null || downloadUrl.isBlank()) throw new IllegalArgumentException("downloadUrl");
    }
  }

  public static UpdateCheckResult evaluate(String currentRaw, UpdateRelease release) {
    SemanticVersion current = SemanticVersion.parse(currentRaw).orElse(null);
    if (current == null) return new UpdateCheckResult(Status.INVALID_CURRENT_VERSION, null, null, null);
    SemanticVersion latest = SemanticVersion.parse(release.tagName()).orElse(null);
    if (latest == null) return new UpdateCheckResult(Status.INVALID_LATEST_VERSION, current, null, null);
    Status status = latest.compareTo(current) > 0 ? Status.UPDATE_AVAILABLE : Status.CURRENT;
    return new UpdateCheckResult(status, current, latest, release.htmlUrl());
  }
}

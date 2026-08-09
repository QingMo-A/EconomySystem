package com.mo.economy_system.common.update;

import java.net.URI;
import java.util.Objects;

/** Minimal release payload consumed by the version policy. */
public record UpdateRelease(String tagName, String htmlUrl) {
  public UpdateRelease {
    if (tagName == null || tagName.isBlank()) throw new IllegalArgumentException("tagName");
    if (htmlUrl == null || htmlUrl.isBlank()) throw new IllegalArgumentException("htmlUrl");
    URI uri;
    try {
      uri = URI.create(htmlUrl.trim());
    } catch (IllegalArgumentException error) {
      throw new IllegalArgumentException("htmlUrl", error);
    }
    if (!"https".equalsIgnoreCase(uri.getScheme())
        || uri.getHost() == null || uri.getHost().isBlank()) {
      throw new IllegalArgumentException("htmlUrl scheme");
    }
    htmlUrl = uri.toString();
  }
}

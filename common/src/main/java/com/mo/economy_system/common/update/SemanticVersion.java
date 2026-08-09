package com.mo.economy_system.common.update;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small SemVer 2.0 parser/comparator with an optional leading {@code v}. */
public record SemanticVersion(
    int major, int minor, int patch, List<String> preRelease, String buildMetadata)
    implements Comparable<SemanticVersion> {
  private static final Pattern PATTERN = Pattern.compile(
      "^[vV]?([0-9]+)\\.([0-9]+)\\.([0-9]+)"
          + "(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?"
          + "(?:\\+([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?$");

  public SemanticVersion {
    if (major < 0 || minor < 0 || patch < 0) throw new IllegalArgumentException("negative version");
    preRelease = List.copyOf(Objects.requireNonNull(preRelease, "preRelease"));
    buildMetadata = Objects.requireNonNull(buildMetadata, "buildMetadata");
  }

  public static Optional<SemanticVersion> parse(String raw) {
    if (raw == null) return Optional.empty();
    String value = raw.trim();
    if (value.isEmpty()) return Optional.empty();
    Matcher matcher = PATTERN.matcher(value);
    if (!matcher.matches()) return Optional.empty();
    try {
      int major = component(matcher.group(1));
      int minor = component(matcher.group(2));
      int patch = component(matcher.group(3));
      List<String> pre = matcher.group(4) == null
          ? List.of() : List.of(matcher.group(4).split("\\.", -1));
      for (String identifier : pre) {
        if (identifier.isEmpty()) return Optional.empty();
        if (identifier.chars().allMatch(Character::isDigit) && identifier.length() > 1
            && identifier.charAt(0) == '0') return Optional.empty();
      }
      String build = matcher.group(5) == null ? "" : matcher.group(5);
      return Optional.of(new SemanticVersion(major, minor, patch, pre, build));
    } catch (RuntimeException error) {
      return Optional.empty();
    }
  }

  private static int component(String value) {
    if (value.length() > 1 && value.charAt(0) == '0') {
      throw new IllegalArgumentException("leading zero");
    }
    long parsed = Long.parseLong(value);
    if (parsed > Integer.MAX_VALUE) throw new IllegalArgumentException("version too large");
    return (int) parsed;
  }

  public boolean isStable() {
    return preRelease.isEmpty();
  }

  @Override
  public int compareTo(SemanticVersion other) {
    Objects.requireNonNull(other, "other");
    int result = Integer.compare(major, other.major);
    if (result != 0) return result;
    result = Integer.compare(minor, other.minor);
    if (result != 0) return result;
    result = Integer.compare(patch, other.patch);
    if (result != 0) return result;
    if (preRelease.isEmpty() && other.preRelease.isEmpty()) return 0;
    if (preRelease.isEmpty()) return 1;
    if (other.preRelease.isEmpty()) return -1;
    int count = Math.min(preRelease.size(), other.preRelease.size());
    for (int index = 0; index < count; index++) {
      String left = preRelease.get(index);
      String right = other.preRelease.get(index);
      if (left.equals(right)) continue;
      boolean leftNumeric = numeric(left);
      boolean rightNumeric = numeric(right);
      if (leftNumeric && rightNumeric) {
        int numericResult = compareNumeric(left, right);
        if (numericResult != 0) return numericResult;
      } else if (leftNumeric != rightNumeric) {
        return leftNumeric ? -1 : 1;
      } else {
        return left.compareTo(right);
      }
    }
    return Integer.compare(preRelease.size(), other.preRelease.size());
  }

  private static boolean numeric(String value) {
    return value.chars().allMatch(Character::isDigit);
  }

  private static int compareNumeric(String left, String right) {
    if (left.length() != right.length()) return Integer.compare(left.length(), right.length());
    return left.compareTo(right);
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder().append(major).append('.').append(minor).append('.').append(patch);
    if (!preRelease.isEmpty()) result.append('-').append(String.join(".", preRelease));
    if (!buildMetadata.isEmpty()) result.append('+').append(buildMetadata);
    return result.toString();
  }
}

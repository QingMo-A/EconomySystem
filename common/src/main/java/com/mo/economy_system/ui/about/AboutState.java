package com.mo.economy_system.ui.about;

import java.util.Objects;

public record AboutState(String modName, String author, String githubUrl, boolean copied) {
  public AboutState {
    if (modName == null || modName.isBlank()) throw new IllegalArgumentException("modName");
    if (author == null || author.isBlank()) throw new IllegalArgumentException("author");
    if (githubUrl == null || githubUrl.isBlank()) throw new IllegalArgumentException("githubUrl");
  }
}

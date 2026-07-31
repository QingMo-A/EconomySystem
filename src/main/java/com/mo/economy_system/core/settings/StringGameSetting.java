package com.mo.economy_system.core.settings;

import java.util.Locale;
import java.util.Set;

public class StringGameSetting implements GameSetting<String> {
    private final String key;
    private final String description;
    private final String defaultValue;
    private final Set<String> allowedValues;

    public StringGameSetting(String key, String description, String defaultValue, Set<String> allowedValues) {
        this.key = key;
        this.description = description;
        this.defaultValue = normalize(defaultValue);
        this.allowedValues = allowedValues;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public String defaultValue() {
        return defaultValue;
    }

    @Override
    public String parse(String value) {
        String normalized = normalize(value);
        if (!allowedValues.isEmpty() && !allowedValues.contains(normalized)) {
            throw new IllegalArgumentException("Allowed values: " + String.join(", ", allowedValues));
        }
        return normalized;
    }

    @Override
    public String serialize(String value) {
        return normalize(value);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

package com.mo.economy_system.core.settings;

public interface GameSetting<T> {
    String key();

    String description();

    T defaultValue();

    T parse(String value);

    String serialize(T value);

    @SuppressWarnings("unchecked")
    default String serializeRaw(Object value) {
        return serialize((T) value);
    }
}

package com.bqsummer.plugin.configplus.domain;

import lombok.Getter;

@Getter
public enum ConfigType {

    STRING("STRING"),
    INTEGER("INTEGER"),
    DOUBLE("DOUBLE"),
    MAP("MAP");

    private final String value;

    ConfigType(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}

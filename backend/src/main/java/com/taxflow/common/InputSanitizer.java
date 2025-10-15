package com.taxflow.common;

import org.springframework.stereotype.Component;

@Component
public class InputSanitizer {
    public String clean(String value) {
        return value == null ? null : value.replaceAll("[<>\"'`]", "").trim();
    }
}

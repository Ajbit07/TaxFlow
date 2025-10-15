package com.taxflow.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InputSanitizerTest {

    private final InputSanitizer sanitizer = new InputSanitizer();

    @Test
    void stripsHtmlMetaCharacters() {
        assertThat(sanitizer.clean("<script>alert('x')</script>")).isEqualTo("scriptalert(x)/script");
    }

    @Test
    void trimsWhitespace() {
        assertThat(sanitizer.clean("  Sharma Traders  ")).isEqualTo("Sharma Traders");
    }

    @Test
    void passesNullThrough() {
        assertThat(sanitizer.clean(null)).isNull();
    }

    @Test
    void leavesRegularTextUntouched() {
        assertThat(sanitizer.clean("Invoice TF-2026-00001 for ₹1,180.00")).isEqualTo("Invoice TF-2026-00001 for ₹1,180.00");
    }
}

package com.complex.platform.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import org.junit.jupiter.api.Test;

class StringsTest {

    @Test
    void trimToNull_nullOrBlankToNull_elseTrimmed() {
        assertThat(Strings.trimToNull(null)).isNull();
        assertThat(Strings.trimToNull("   ")).isNull();
        assertThat(Strings.trimToNull("  ab ")).isEqualTo("ab");
    }

    @Test
    void requireText_blankThrows_elseTrimmed() {
        assertThatThrownBy(() -> Strings.requireText("  ", "name")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> Strings.requireText(null, "name")).isInstanceOf(ApiException.class);
        assertThat(Strings.requireText("  x ", "name")).isEqualTo("x");
    }
}

package com.complex.platform.customer;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import org.junit.jupiter.api.Test;

/** ROOT-03-R3：条款版本随正文变化；正文仍是占位时生产环境不可注册（外部资料未到不假成功）。 */
class CustomerTermsTests {

    @Test
    void versionsAreContentHashes() {
        CustomerTerms.Current current = new CustomerTerms(false).current();
        assertThat(current.memberTerms().version()).matches("[0-9a-f]{16}");
        assertThat(current.privacyPolicy().version()).matches("[0-9a-f]{16}").isNotEqualTo(current.memberTerms().version());
        assertThat(current.memberTerms().content()).contains("会员条款");
    }

    @Test
    void prodPlaceholderTermsBlockRegistration() {
        assertThatThrownBy(() -> new CustomerTerms(true).requirePublished())
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CUSTOMER_TERMS_NOT_PUBLISHED"));
        assertThat(new CustomerTerms(false).requirePublished()).isNotNull();
    }
}

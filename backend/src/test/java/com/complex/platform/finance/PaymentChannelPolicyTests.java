package com.complex.platform.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import org.junit.jupiter.api.Test;

class PaymentChannelPolicyTests {

    @Test
    void paymentIntentChannelsAreCanonicalizedAcrossChineseAndEnglishAliases() {
        assertThat(PaymentChannelPolicy.requirePaymentIntentChannel("聚合支付")).isEqualTo("aggregate_pay");
        assertThat(PaymentChannelPolicy.requirePaymentIntentChannel("微信支付")).isEqualTo("wechat");
        assertThat(PaymentChannelPolicy.requirePaymentIntentChannel("bank-card")).isEqualTo("bank_card");
    }

    @Test
    void settlementChannelsCollapseProviderAliasesToFinanceBuckets() {
        assertThat(PaymentChannelPolicy.requireSettlementChannel(null, "aggregate_pay")).isEqualTo("aggregate_pay");
        assertThat(PaymentChannelPolicy.requireSettlementChannel("现金", "aggregate_pay")).isEqualTo("cash");
        assertThat(PaymentChannelPolicy.requireSettlementChannel("刷卡", "aggregate_pay")).isEqualTo("pos");
        assertThat(PaymentChannelPolicy.requireSettlementChannel("支付宝", "aggregate_pay")).isEqualTo("aggregate_pay");
        assertThat(PaymentChannelPolicy.settlementBucket("pos")).isEqualTo("pos");
        assertThat(PaymentChannelPolicy.settlementBucket("微信")).isEqualTo("online");
    }

    @Test
    void unsupportedChannelsFailClosed() {
        assertThatThrownBy(() -> PaymentChannelPolicy.requireSettlementChannel("barter", "aggregate_pay"))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }
}

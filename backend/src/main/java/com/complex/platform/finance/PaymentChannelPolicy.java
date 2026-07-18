package com.complex.platform.finance;

import com.complex.platform.common.api.ApiException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 统一支付渠道词表与别名归并。
 *
 * <p>对外/页面可以输入中文或供应商常见英文别名；入库和对账只保留 canonical 枚举。
 * 线上支付意图保留聚合支付、直连预留和 mock；酒吧/财务线下结算只保留聚合支付、现金、POS、银行转账、其他。</p>
 */
public final class PaymentChannelPolicy {

    private static final Set<String> PAYMENT_INTENT_CHANNELS = Set.of(
            "aggregate_pay", "wechat", "alipay", "unionpay", "bank_card", "mock");
    private static final Set<String> SETTLEMENT_CHANNELS = Set.of(
            "aggregate_pay", "cash", "pos", "bank_transfer", "other", "stored_value");
    private static final Map<String, String> CHANNEL_ALIASES = Map.ofEntries(
            Map.entry("聚合支付", "aggregate_pay"),
            Map.entry("线上支付", "aggregate_pay"),
            Map.entry("扫码支付", "aggregate_pay"),
            Map.entry("微信", "wechat"),
            Map.entry("微信支付", "wechat"),
            Map.entry("支付宝", "alipay"),
            Map.entry("银联", "unionpay"),
            Map.entry("银行卡", "bank_card"),
            Map.entry("现金", "cash"),
            Map.entry("刷卡", "pos"),
            Map.entry("POS", "pos"),
            Map.entry("pos刷卡", "pos"),
            Map.entry("银行转账", "bank_transfer"),
            Map.entry("线下其他", "other"),
            Map.entry("储值支付", "stored_value"),
            Map.entry("储值", "stored_value"),
            Map.entry("储值卡", "stored_value")
    );

    private PaymentChannelPolicy() {
    }

    public static String requirePaymentIntentChannel(String channel) {
        String value = canonicalizeRequired(channel, "channel");
        if (!PAYMENT_INTENT_CHANNELS.contains(value)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "unsupported payment intent channel: " + value);
        }
        return value;
    }

    public static String requireSettlementChannel(String channel, String defaultChannel) {
        String value = channel == null || channel.isBlank()
                ? canonicalizeRequired(defaultChannel, "default_channel")
                : canonicalizeRequired(channel, "settle_channel");
        value = switch (value) {
            case "wechat", "alipay", "unionpay", "bank_card" -> "aggregate_pay";
            default -> value;
        };
        if (!SETTLEMENT_CHANNELS.contains(value)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "unsupported settle_channel: " + value);
        }
        return value;
    }

    public static String settlementBucket(String channel) {
        String value = requireSettlementChannel(channel, "aggregate_pay");
        return switch (value) {
            case "cash" -> "cash";
            case "pos" -> "pos";
            default -> "online";
        };
    }

    private static String canonicalizeRequired(String channel, String fieldName) {
        if (channel == null || channel.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        String trimmed = channel.trim();
        String alias = CHANNEL_ALIASES.get(trimmed);
        if (alias != null) {
            return alias;
        }
        return trimmed.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}

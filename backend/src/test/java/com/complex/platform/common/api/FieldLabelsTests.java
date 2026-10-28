package com.complex.platform.common.api;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** 报错出口翻译层：保证任何校验类报错到达用户时都是中文，不外泄英文字段键。 */
class FieldLabelsTests {

    @Test
    void translatesRequiredAndInvalidToChinese() {
        assertThat(FieldLabels.translate("sales_position is required")).isEqualTo("销售岗位未填写");
        assertThat(FieldLabels.translate("venue_id is required")).isEqualTo("场地未填写");
        assertThat(FieldLabels.translate("active is required")).isEqualTo("启用状态未填写");
        assertThat(FieldLabels.translate("action is invalid")).isEqualTo("操作格式不正确");
        assertThat(FieldLabels.translate("order_id is required")).isEqualTo("订单未填写");
    }

    /** 票价规则的 variant_type 是人群/渠道（新人票、学生票、多人票、渠道价、会员价），不是商品规格；与后台票价规则页、结果区字典同口径（OBS-14-02 复核）。 */
    @Test
    void ticketPriceVariantTypeIsAudienceOrChannel() {
        assertThat(FieldLabels.translate("variant_type is invalid")).isEqualTo("人群/渠道格式不正确");
    }

    /** 第 38 条：method 键招商跟进（电话/拜访/微信）、经销收款、考勤打卡共用，中文名不能只叫「支付方式」（招商跟进会成「支付方式：拜访」）。 */
    @Test
    void methodKeyIsNotLabelledAsPaymentOnly() {
        assertThat(FieldLabels.translate("method is required")).isEqualTo("方式未填写");
    }

    @Test
    void machineKeyGetsWholeSentence() {
        assertThat(FieldLabels.translate("idempotency_key is required")).isEqualTo("请勿重复提交，请刷新后再试");
    }

    /** 认不出的键走中文兜底——绝不把英文键名当展示文字吐给用户。 */
    @Test
    void unknownKeyFallsBackToChineseNeverEnglish() {
        assertThat(FieldLabels.translate("no_such_field_xyz is required")).isEqualTo(FieldLabels.FALLBACK);
        assertThat(FieldLabels.translate("totally unknown english sentence")).isEqualTo(FieldLabels.FALLBACK);
    }

    /** 英文键 + 中文正文（各域校验助手拼的形态）→ 键换中文，正文原样。 */
    @Test
    void translatesKeyPrefixedChineseMessages() {
        assertThat(FieldLabels.translate("quantity 必须大于 0")).isEqualTo("数量必须大于 0");
        assertThat(FieldLabels.translate("merchant_id：M001")).isEqualTo("商户：M001");
    }

    /** 会员等级、签到阶梯、生日加倍配置（/api/v1/loyalty/config/*）的数值校验键：报错里不能外泄英文键，中文名与前端字典同口径。 */
    @Test
    void loyaltyConfigNumericKeysAreChinese() {
        assertThat(FieldLabels.translate("level_rank 必须大于等于 0")).isEqualTo("层级必须大于等于 0");
        assertThat(FieldLabels.translate("upgrade_threshold 必须大于等于 0")).isEqualTo("升级门槛必须大于等于 0");
        assertThat(FieldLabels.translate("point_multiplier_basis_points 必须大于等于 1"))
                .isEqualTo("积分倍率（万分比）必须大于等于 1");
        assertThat(FieldLabels.translate("streak_days 必须大于等于 1")).isEqualTo("连续天数必须大于等于 1");
        assertThat(FieldLabels.translate("award_points 必须大于等于 0")).isEqualTo("奖励积分必须大于等于 0");
        assertThat(FieldLabels.translate("multiplier_basis_points 必须大于等于 10001"))
                .isEqualTo("倍率（万分比）必须大于等于 10001");
    }

    /** 已经是中文业务话术的照原样透传，别被兜底吞掉。 */
    @Test
    void keepsExistingChineseMessages() {
        assertThat(FieldLabels.translate("商户已归档：M001")).isEqualTo("商户已归档：M001");
        assertThat(FieldLabels.translate("该账期的租金已生成账单")).isEqualTo("该账期的租金已生成账单");
    }

    @Test
    void beanValidationFieldErrorsAreChinese() {
        assertThat(FieldLabels.beanFieldError("merchantId", "must not be blank")).isEqualTo("商户未填写");
        assertThat(FieldLabels.beanFieldError("quantity", "must be greater than 0")).isEqualTo("数量必须大于 0");
        assertThat(FieldLabels.beanFieldError("unknownThing", "must not be null")).isEqualTo(FieldLabels.FALLBACK);
    }

    /**
     * 防腐门禁：全仓 main 源码里所有 `xxx is required` / `xxx is invalid` 抛点的字段键都必须有中文名。
     * 新增字段键忘了配中文时这条会红——比等运营在现场看到「提交内容有误」再回头查便宜得多。
     */
    @Test
    void everyFieldKeyThrownInMainSourcesHasChineseLabel() throws IOException {
        Path mainSources = Path.of("src/main/java");
        assumeTrue(Files.isDirectory(mainSources), "非标准工作目录，跳过源码扫描");

        // 形态一：手写字面量 `"xxx is required"`；形态二：统一必填助手的字段键实参。
        List<Pattern> sources = List.of(
                Pattern.compile("\"([a-z][a-z0-9_.]*) is (?:required|invalid)\""),
                Pattern.compile("require(?:Text|TextValue|Instant|LocalDate|Items|ItemText)"
                        + "\\([^;\n]*?,\\s*\"([a-z][a-z0-9_.]*)\""),
                Pattern.compile("(?:positive|nonNegative)\\([^;\n]*?,\\s*\"([a-z][a-z0-9_.]*)\""));
        var missing = new TreeSet<String>();
        try (Stream<Path> files = Files.walk(mainSources)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String body = Files.readString(file, StandardCharsets.UTF_8);
                for (Pattern pattern : sources) {
                    Matcher matcher = pattern.matcher(body);
                    while (matcher.find()) {
                        String key = matcher.group(1);
                        if (FieldLabels.label(key) == null && !"idempotency_key".equals(key)) {
                            missing.add(key);
                        }
                    }
                }
            }
        }
        assertThat(missing).as("缺中文名的字段键（补到 FieldLabels 即可）").isEmpty();
    }
}

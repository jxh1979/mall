// 文件版本：V0.02
package com.complex.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * 第 23 条 23-R3 导出文件单元回归：PGM:687「所有导出的文件一律带密码……长度不超过十位」「导出文件中的数据不可复制、不可编辑」；
 * 每页暗纹按页面暗纹同一规则可反查；密码按行内要素重算一致；生产档拒绝开发默认密钥。
 */
class ExportFileServiceTest {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private static final String SECRET = "unit-test-export-password-secret-32-bytes";

    @Test
    void pdfNeedsThePasswordForbidsCopyAndEditButAllowsPrintAndCarriesTheWatermarkOnEveryPage() throws Exception {
        List<List<String>> rows = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            rows.add(List.of("单据-" + i, "应收", "第 " + i + " 行\n第二行"));
        }
        byte[] pdf = ExportFileService.pdf("导出回归", List.of("单据编号", "方向", "备注"), rows, "ABCDEFGH", "PASSWD2345", false);

        assertThatThrownBy(() -> Loader.loadPDF(pdf).close()).isInstanceOf(InvalidPasswordException.class);
        try (PDDocument doc = Loader.loadPDF(pdf, "PASSWD2345")) {
            AccessPermission permission = doc.getCurrentAccessPermission();
            assertThat(permission.canExtractContent()).isFalse();
            assertThat(permission.canExtractForAccessibility()).isFalse();
            assertThat(permission.canModify()).isFalse();
            assertThat(permission.canModifyAnnotations()).isFalse();
            assertThat(permission.canFillInForm()).isFalse();
            assertThat(permission.canAssembleDocument()).isFalse();
            assertThat(permission.canPrint()).isTrue();
            assertThat(doc.getEncryption().getLength()).isEqualTo(256);
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);
            assertThat(new PDFTextStripper().getText(doc)).contains("导出回归", "单据编号", "方向", "单据-0", "单据-119", "第二行");
            PDFRenderer renderer = new PDFRenderer(doc);
            for (int page = 0; page < doc.getNumberOfPages(); page++) {
                assertThat(WatermarkQr.decode(renderer.renderImageWithDPI(page, 72))).as("第 %d 页暗纹", page + 1).contains("ABCDEFGH");
            }
        }
    }

    /**
     * 复核（PGM:687「确需可编辑版本用于二次加工的……可编辑版同样带密码与暗纹」）：可编辑版复制出来的文字与原数据逐字一致——
     * 字体里与康熙部首、部首补充共用字形的常用字（一、二、十、人、日、月、方、比、金、用、门、长、车、马）不被记成部首码位。
     */
    @Test
    void editableCopyExtractsTheOriginalCharactersNotLookAlikeRadicals() throws Exception {
        String title = "月度经营对比";
        List<String> columns = List.of("指标", "本月", "同比");
        List<List<String>> rows = List.of(List.of("门店金额", "一二十人日", "长期车马用方比"));
        byte[] pdf = ExportFileService.pdf(title, columns, rows, "ABCDEFGH", "PASSWD2345", true);

        try (PDDocument doc = Loader.loadPDF(pdf, "PASSWD2345")) {
            assertThat(doc.getCurrentAccessPermission().canExtractContent()).isTrue();
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains(title, "指标", "本月", "同比", "门店金额", "一二十人日", "长期车马用方比", "ABCDEFGH");
            assertThat(text.codePoints().filter(cp -> (cp >= 0x2E80 && cp <= 0x2EFF) || (cp >= 0x2F00 && cp <= 0x2FDF)).count()).isZero();
        }
    }

    /**
     * 复核（PGM:687 可编辑版「用于二次加工」）：拉丁字母逐字写入，ff、fi、fl、ffi 不被字体合成连字——复制出来的编号、单词与原数据逐字一致。
     */
    @Test
    void copiedTextKeepsLatinLetterPairsInsteadOfLigatures() throws Exception {
        String title = "office fixture";
        List<String> columns = List.of("单据编号", "备注");
        List<List<String>> rows = List.of(List.of("arap_mcr_a_2a35ff3d", "ff fi fl ffi"));
        byte[] pdf = ExportFileService.pdf(title, columns, rows, "ABCDEFGH", "PASSWD2345", true);

        try (PDDocument doc = Loader.loadPDF(pdf, "PASSWD2345")) {
            assertThat(new PDFTextStripper().getText(doc)).contains(title, "arap_mcr_a_2a35ff3d", "ff fi fl ffi");
        }
    }

    @Test
    void passwordIsTenCharactersFromTheAlphabetAndRecomputesFromTheSameElements() {
        ExportFileService service = service(SECRET, false);
        Instant at = Instant.parse("2026-10-29T01:02:03Z");
        String password = service.password("usr_a", at, "WBABCDEFGHJKMN", "127.0.0.1", "exp_1");

        assertThat(password).matches("[A-HJKMNP-Z2-9]{10}");
        assertThat(service.password("usr_a", at, "WBABCDEFGHJKMN", "127.0.0.1", "exp_1")).isEqualTo(password);
        assertThat(service(SECRET, true).password("usr_a", at, "WBABCDEFGHJKMN", "127.0.0.1", "exp_1")).isEqualTo(password);
        assertThat(List.of(
                service.password("usr_b", at, "WBABCDEFGHJKMN", "127.0.0.1", "exp_1"),
                service.password("usr_a", at.plusSeconds(1), "WBABCDEFGHJKMN", "127.0.0.1", "exp_1"),
                service.password("usr_a", at, "WBZZZZZZZZZZZZ", "127.0.0.1", "exp_1"),
                service.password("usr_a", at, "WBABCDEFGHJKMN", "10.0.0.1", "exp_1"),
                service.password("usr_a", at, "WBABCDEFGHJKMN", "127.0.0.1", "exp_2"))).doesNotContain(password);
        assertThat(ExportFileService.digest(password)).hasSize(64);
    }

    @Test
    void productionProfileRefusesTheDevelopmentDefaultOrAShortSecret() {
        assertThatThrownBy(() -> service(ExportFileService.DEV_SECRET, true)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPLEX_EXPORT_PASSWORD_SECRET");
        assertThatThrownBy(() -> service("short-secret", true)).isInstanceOf(IllegalStateException.class);
        assertThat(service(ExportFileService.DEV_SECRET, false)).isNotNull();
    }

    private static ExportFileService service(String secret, boolean prod) {
        MockEnvironment environment = new MockEnvironment();
        if (prod) {
            environment.setActiveProfiles("prod");
        }
        return new ExportFileService(null, null, null, null, null, null, null, null, secret, environment);
    }
}

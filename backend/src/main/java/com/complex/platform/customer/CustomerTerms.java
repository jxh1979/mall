package com.complex.platform.customer;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * ROOT-03-R3 当前会员条款与隐私条款（BSM 第 886 行「同意当前会员条款和隐私条款」；BSM 第 890 行「留同意版本、时间和范围」）。
 * 正文放 classpath {@code customer-terms/}，版本＝正文 SHA-256 前 16 位（改正文即换版本，客人须按新版本同意）。
 * 正文仍是占位（含「【待法务提供】」）时 prod 一律不可注册（409 CUSTOMER_TERMS_NOT_PUBLISHED，外部资料未到不假成功）；本地与自动化测试照常。
 */
@Component
public class CustomerTerms {

    static final String PLACEHOLDER = "【待法务提供】";

    public record Document(String version, String content) {
    }

    public record Current(Document memberTerms, Document privacyPolicy) {
    }

    private final Current current;
    private final boolean requirePublished;

    @Autowired
    public CustomerTerms(Environment environment) {
        this(environment.acceptsProfiles(Profiles.of("prod")));
    }

    CustomerTerms(boolean requirePublished) {
        this.requirePublished = requirePublished;
        this.current = new Current(load("customer-terms/member-terms.md"), load("customer-terms/privacy-policy.md"));
    }

    public Current current() {
        return current;
    }

    /** 注册用：正文未发布（仍是占位）的环境拒绝注册。 */
    public Current requirePublished() {
        if (requirePublished && (current.memberTerms().content().contains(PLACEHOLDER)
                || current.privacyPolicy().content().contains(PLACEHOLDER))) {
            throw ApiException.conflict("CUSTOMER_TERMS_NOT_PUBLISHED", "会员条款与隐私条款尚未发布，暂不能注册");
        }
        return current;
    }

    private static Document load(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            byte[] bytes = in.readAllBytes();
            String version = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).substring(0, 16);
            return new Document(version, new String(bytes, StandardCharsets.UTF_8));
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("customer terms unavailable: " + path, exception);
        }
    }
}

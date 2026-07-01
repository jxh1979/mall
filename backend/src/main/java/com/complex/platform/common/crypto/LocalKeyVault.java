package com.complex.platform.common.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * 本地密钥库（模型①）：运行态从本地配置 {@code keyvault.keys.<purpose>} 取密钥字符串，SHA-256 派生 32 字节 AES-256 密钥。
 * 安装期由 U 盘 provision 写入本地密钥文件/环境变量，运行态只读本地、不依赖 U 盘（断电重启无需插盘）。
 * 生产必须由 env 注入强随机密钥：复用 {@link SigningSecretGuard} 的弱密钥防御——dev 默认（含 change-me / 短于 32）在 prod 直接失败。
 */
@Component
public class LocalKeyVault implements KeyVault {

    private final Environment environment;
    private final boolean productionProfile;
    private final Map<String, SecretKey> cache = new ConcurrentHashMap<>();

    public LocalKeyVault(Environment environment) {
        this.environment = environment;
        this.productionProfile = environment.acceptsProfiles(Profiles.of("prod"));
    }

    @Override
    public SecretKey dataKey(String purpose) {
        return cache.computeIfAbsent(purpose, this::load);
    }

    private SecretKey load(String purpose) {
        String property = "keyvault.keys." + purpose;
        String envName = "COMPLEX_KEYVAULT_" + purpose.toUpperCase(Locale.ROOT) + "_KEY";
        String secret = SigningSecretGuard.requireUsableHmacSecret(
                property, envName, environment.getProperty(property, ""), "", productionProfile);
        return new SecretKeySpec(sha256(secret), "AES");
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 digest unavailable", exception);
        }
    }
}

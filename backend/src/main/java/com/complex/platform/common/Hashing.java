package com.complex.platform.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 幂等码/短哈希收敛（4.5）：此前 inventory 等多处各自复制 SHA-256→hex→前16位 生成幂等码 slug，
 * 统一到此一处，避免散落重复与算法/长度漂移。
 */
public final class Hashing {

    private Hashing() {
    }

    /** 幂等码：SHA-256(seed) 十六进制前 16 位。seed 由调用方保证非空。 */
    public static String idempotencyCode(String seed) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                    .formatHex(digest.digest(seed.getBytes(StandardCharsets.UTF_8)))
                    .substring(0, 16);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}

package com.complex.platform.common.crypto;

public final class SigningSecretGuard {

    private SigningSecretGuard() {
    }

    public static String requireUsableHmacSecret(
            String propertyName,
            String envName,
            String secret,
            String devDefault,
            boolean productionProfile) {
        String value = secret == null ? "" : secret.trim();
        if (value.isEmpty()) {
            throw new IllegalStateException(propertyName + " must be configured");
        }
        if (productionProfile && isWeakProductionSecret(value, devDefault)) {
            throw new IllegalStateException(propertyName + " must be set from " + envName
                    + " to a strong non-default value in prod");
        }
        return value;
    }

    private static boolean isWeakProductionSecret(String value, String devDefault) {
        return value.equals(devDefault)
                || value.contains("change-me")
                || value.length() < 32;
    }
}

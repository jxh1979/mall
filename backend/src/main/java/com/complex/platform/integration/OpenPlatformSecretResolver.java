package com.complex.platform.integration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class OpenPlatformSecretResolver implements SecretResolver {

    private static final String CONFIG_PREFIX = "config:";
    private static final String ENV_PREFIX = "env:";
    private static final String FILE_PREFIX = "file:";
    private static final String SECRET_REF_PROPERTY_PREFIX = "complex.open-platform.secret-refs.";

    private final Environment environment;

    public OpenPlatformSecretResolver(Environment environment) {
        this.environment = environment;
    }

    @Override
    public Optional<String> resolve(String secretRef) {
        if (secretRef == null || secretRef.isBlank()) {
            return Optional.empty();
        }
        String trimmedRef = secretRef.trim();
        if (trimmedRef.startsWith(CONFIG_PREFIX)) {
            return property(trimmedRef.substring(CONFIG_PREFIX.length()));
        }
        if (trimmedRef.startsWith(ENV_PREFIX)) {
            return environmentValue(trimmedRef.substring(ENV_PREFIX.length()));
        }
        if (trimmedRef.startsWith(FILE_PREFIX)) {
            return fileValue(trimmedRef.substring(FILE_PREFIX.length()));
        }
        return property(SECRET_REF_PROPERTY_PREFIX + normalizeSecretRef(trimmedRef));
    }

    private Optional<String> property(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        return nonBlank(environment.getProperty(key.trim()));
    }

    private Optional<String> environmentValue(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String trimmedName = name.trim();
        return property(trimmedName).or(() -> nonBlank(System.getenv(trimmedName)));
    }

    private Optional<String> fileValue(String path) {
        if (path == null || path.isBlank()) {
            return Optional.empty();
        }
        try {
            return nonBlank(Files.readString(Path.of(path.trim())));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private Optional<String> nonBlank(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.strip());
    }

    private String normalizeSecretRef(String secretRef) {
        return secretRef.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
    }
}

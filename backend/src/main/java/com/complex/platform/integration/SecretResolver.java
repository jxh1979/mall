package com.complex.platform.integration;

import java.util.Optional;

@FunctionalInterface
public interface SecretResolver {

    Optional<String> resolve(String secretRef);
}

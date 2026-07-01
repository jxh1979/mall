package com.complex.platform.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionDatabaseCredentialGuardTests {

    private MockEnvironment env(Map<String, String> props) {
        MockEnvironment environment = new MockEnvironment();
        props.forEach(environment::setProperty);
        return environment;
    }

    @Test
    void rejectsDevDefaultCredentialsInProduction() {
        assertThatThrownBy(() -> new ProductionDatabaseCredentialGuard(env(Map.of(
                "spring.datasource.username", "complex",
                "spring.datasource.password", "complex_dev_pwd",
                "spring.flyway.user", "complex",
                "spring.flyway.password", "complex_dev_pwd"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dev default");
    }

    @Test
    void rejectsAppEqualsOwner() {
        assertThatThrownBy(() -> new ProductionDatabaseCredentialGuard(env(Map.of(
                "spring.datasource.username", "shared_role",
                "spring.datasource.password", "strong-shared-pwd",
                "spring.flyway.user", "shared_role",
                "spring.flyway.password", "strong-shared-pwd"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("differ from owner");
    }

    @Test
    void acceptsDistinctNonDefaultCredentials() {
        assertThatCode(() -> new ProductionDatabaseCredentialGuard(env(Map.of(
                "spring.datasource.username", "complex_app",
                "spring.datasource.password", "strong-app-pwd",
                "spring.flyway.user", "complex_owner",
                "spring.flyway.password", "strong-owner-pwd"))))
                .doesNotThrowAnyException();
    }

    @Test
    void skipsWhenEnforcementDisabled() {
        assertThatCode(() -> new ProductionDatabaseCredentialGuard(env(Map.of(
                "complex.database.credential-separation.enforced", "false",
                "spring.datasource.username", "complex",
                "spring.datasource.password", "complex_dev_pwd",
                "spring.flyway.user", "complex",
                "spring.flyway.password", "complex_dev_pwd"))))
                .doesNotThrowAnyException();
    }
}

package com.complex.platform.access;

// 文件版本：V0.01

import java.time.Instant;

@FunctionalInterface
public interface AccessCredentialProvisioner {
    void provisionCredential(String userId, String password, String status, Instant now);

    /** 只更新状态，不重置密码、锁定计数或密码变更时间。 */
    default void changeStatus(String userId, String status, Instant now) {
        throw new UnsupportedOperationException("凭证提供方未实现状态同步");
    }

    static AccessCredentialProvisioner noop() {
        return (userId, password, status, now) -> { };
    }
}

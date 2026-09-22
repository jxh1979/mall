package com.complex.platform.auth;

/**
 * 签发「U 盘超管」破窗会话的接口。dev（{@link AuthService} 内存）与 prod（{@link ProductionAuthService} DB）
 * 各自实现，{@link MasterAuthorityService} 注入当前 profile 活动的那个，验签通过后调用之签发最高权限会话。
 * 破窗身份不依赖已有账号：签发时按需 upsert 保留超管账号，故 access_users 为空/管理员被误删也能登入恢复。
 */
public interface SessionIssuer {

    /** 保留超管账号 id/username（U 盘破窗专用，与普通管理员分离）。 */
    String MASTER_USER_ID = "usr_master_authority";
    String MASTER_USERNAME = "master-authority";
    /** 最高权限账号类型。不是平台管理员，不得当日常全权通配。 */
    String MASTER_ACCOUNT_TYPE = "master_authority";

    /** 签发最高权限会话。须随插盘来源带上设备标识；不得按平台管理员身份签发。 */
    AuthService.LoginResponse issueMasterSession(String deviceId);

    /** 验签公钥加载或更换时作废已签发的最高权限登录，旧盘换到的票不能继续用。 */
    default void revokeOutstandingMasterSessions() {
    }
}

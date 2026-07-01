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

    /** 签发一个平台最高权限会话（platform_admin + active，被 isPlatformAdminBridge 视为 wildcard）。 */
    AuthService.LoginResponse issueMasterSession();
}

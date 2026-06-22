-- P0-4 / X-02 人脸入场闸脚手架（业务 HTML：全场 18+，统一人脸入场核验闸保证场内皆 ≥18 周岁）。
-- 本期按用户口径“第三方/人脸一律预留接口 + 能力开关默认关”落地（仿 V093 支付渠道）：
--   仅 seed 能力开关 face.verification.enabled = FALSE（默认关）；端口/路由/占位见 com.complex.platform.entry 包；
--   真实判龄（按实名身份证出生日期 + 活体、fail-closed）待接供应商 + 建实名身份模型后启用。
-- 注意：T2 resolveEffectiveSwitch 语义为“无显式行=默认开启”（opt-out）。人脸闸要求“默认关”，
--   故必须显式插入 enabled=FALSE 行；terminal_type='*'（跨端通配）、scope_type='global'。
--   开关由现有 TerminalDeviceController.updateCapabilitySwitch（terminal.switch.manage，平台权限）切换，无需新增权限。
INSERT INTO terminal_capability_switches
    (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
VALUES
    ('tcs_face_verification_off', '*', 'face.verification.enabled', 'global', 'global', FALSE,
     '人脸入场闸默认关闭：关时入场不阻塞、年龄由制度兜底（豁免，残留风险已知会）；'
     || '待真实人脸/实名 SDK 接入 + 后台 updateCapabilitySwitch 开启后路由至厂商适配器（fail-closed 判龄）', now(), now())
ON CONFLICT (terminal_type, capability_code, scope_type, scope_id) DO NOTHING;

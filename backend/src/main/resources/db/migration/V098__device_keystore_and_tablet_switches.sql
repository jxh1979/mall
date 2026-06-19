-- V098 员工设备 Keystore 凭证绑定（item1）+ 平板终端（②）：预留接口 + 能力开关（默认关，后台可配）。
-- 用户口径（第二阶段）：端侧能力统一做成"后台能力开关（默认关、后台可配、仅预留接口）"。
--   本次补：① 员工设备 Keystore 凭证绑定（staff.device_keystore.enabled）；② 平板终端（terminal.tablet.enabled）。
--   ③ 用户端 App（customer.native_client.enabled）已在 V097 seed FALSE → 本迁移不重复（仅在此确认）。
-- 事实源对齐：terminal_capability_switches（R5 T2，V068 CHECK 含 terminal_type '*' 与 scope 'global'）。
-- 范式对齐：V097 camera_scan / native_client、V096 wecom.ai_qc、V093 payment.gateway、V071 integration.*.sync
--   均"显式 seed enabled=FALSE 行"实现默认关。
--
-- 纯预留：以下 seed 行仅占位 + 后台可配，**不接到任何现有 @GuardedAction 守卫、不改现有平板/绑定业务与测试**
--   （与 V097 camera_scan 一致），避免误伤现有平板/绑定测试。

-- ── 能力开关：显式 seed = FALSE（默认关） ────────────────────────────────────────
-- 注意：T2 TerminalSwitchGuard / resolveEffectiveSwitch 语义为"无显式行=默认开启"（opt-out）。
--   本两能力要求"默认关"，故必须显式插入 enabled=FALSE 行；terminal_type='*'（跨端通配）、scope_type='global'。
-- 后台经现有 TerminalDeviceController.updateCapabilitySwitch（PUT /api/v1/terminal/capability-switches/{id}，
--   权限 terminal.switch.manage）切换；端侧经 GET /api/v1/terminal/capability-switches（权限 terminal.switch.view）读取。
--   故"预留接口"= 能力开关 + 既有 list/update 端点 + 端侧代偿 hook，无需新增表/权限/控制器。
--
-- 1) staff.device_keystore.enabled（item1）：员工设备 Android Keystore 设备公钥/指纹绑定能力。
--    关→端侧用占位/手工，不强制 Keystore（端侧不真机取 Keystore 公钥指纹，绑定走占位/手工录入，不阻塞）；
--    开→端侧真机 Keystore 取公钥指纹上报。
--    后端 staff_device_credentials（V086：device_public_key/device_fingerprint_hash 等）接口已就绪，
--    本开关仅预留端侧 Keystore 取值能力，不改后端绑定接口/状态机。
-- 2) terminal.tablet.enabled（②）：平板终端现场操作能力。
--    关→平板端侧能力预留未启用（不阻塞现有任何流程）；
--    开→启用平板现场操作。
INSERT INTO terminal_capability_switches
    (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
VALUES
    ('tcs_device_keystore_off', '*', 'staff.device_keystore.enabled', 'global', 'global', FALSE,
     '员工设备 Keystore 设备公钥/指纹绑定能力默认关闭（item1）：关时端侧用占位/手工，不强制 Keystore'
     || '（不真机取 Keystore 公钥指纹、绑定走占位/手工录入、不阻塞）；后端 staff_device_credentials 接口已就绪，'
     || '本开关仅预留端侧 Keystore 取值能力，待端侧真机 Keystore 接入 + 后台 updateCapabilitySwitch 开启后启用', now(), now()),
    ('tcs_tablet_off', '*', 'terminal.tablet.enabled', 'global', 'global', FALSE,
     '平板终端能力默认关闭（②）：关时平板端侧能力预留未启用（不阻塞现有流程）；'
     || '待平板现场操作接入 + 后台 updateCapabilitySwitch 开启后启用', now(), now())
ON CONFLICT (terminal_type, capability_code, scope_type, scope_id) DO NOTHING;

-- 3) customer.native_client.enabled（③ 用户端 App）：已在 V097 seed enabled=FALSE（默认关）→ 本迁移不重复。
--    关→走 Web 客户端代偿（H5/Web 完成下单/查询/核销，不依赖小程序/原生壳、不阻塞）。

-- 权限：开关管理复用既有 terminal.switch.manage（切换）/ terminal.switch.view（读取），不新增权限点。

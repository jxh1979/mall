-- V097 摄像头扫码（#2）+ 微信小程序/原生 App 客户端（#3）：预留接口 + 能力开关（默认关，后台可配）。
-- 用户口径（第二阶段）：摄像头扫码与端侧原生客户端涉及第三方/端侧能力 →
--   做成"后台能力开关（默认关、后台可配）+ 仅预留接口"，不做真实实现。
--   关→端侧用现有代偿路径（扫码=文本输入 credential；原生客户端=Web 客户端渠道），不接 SDK、不阻塞。
--   开→待真实硬件扫码 SDK / 微信小程序 / 原生 App 接入后启用（当前无真实实现=仅预留接缝）。
-- 事实源对齐：terminal_capability_switches（R5 T2，V068 CHECK 含 terminal_type '*' 与 scope 'global'）。
-- 范式对齐：V093 payment.gateway.enabled / V096 wecom.ai_qc.enabled / V071 integration.*.sync
--   均"显式 seed enabled=FALSE 行"实现默认关。

-- ── 能力开关：显式 seed = FALSE（默认关） ────────────────────────────────────────
-- 注意：T2 TerminalSwitchGuard / resolveEffectiveSwitch 语义为"无显式行=默认开启"（opt-out）。
--   本两能力要求"默认关"，故必须显式插入 enabled=FALSE 行；terminal_type='*'（跨端通配）、scope_type='global'。
-- 后台经现有 TerminalDeviceController.updateCapabilitySwitch（PUT /api/v1/terminal/capability-switches/{id}，
--   权限 terminal.switch.manage）切换；端侧经 GET /api/v1/terminal/capability-switches（权限 terminal.switch.view）读取。
--   故"预留接口"= 能力开关 + 既有 list/update 端点 + 端侧代偿 hook，无需新增表/权限/控制器。
--
-- 1) terminal.camera_scan.enabled：现场摄像头扫码硬件能力。
--    关→端侧用文本输入 credential 代偿（手输票号/会员码/设备码，不调摄像头/扫码 SDK，不阻塞）；
--    开→待真实硬件扫码 SDK 接入后端侧调用摄像头解码。
-- 2) customer.native_client.enabled：微信小程序 / 原生 App 客户端渠道。
--    关→走 Web 客户端代偿（H5/Web 完成下单/查询/核销等，不依赖小程序/原生壳，不阻塞）；
--    开→待微信小程序 / 原生 App 客户端接入后启用原生渠道。
INSERT INTO terminal_capability_switches
    (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
VALUES
    ('tcs_camera_scan_off', '*', 'terminal.camera_scan.enabled', 'global', 'global', FALSE,
     '现场摄像头扫码硬件能力默认关闭（#2）：关时端侧用文本输入 credential 代偿（手输票号/会员码/设备码，'
     || '不调摄像头/扫码 SDK、不阻塞）；待真实硬件扫码 SDK 接入 + 后台 updateCapabilitySwitch 开启后启用', now(), now()),
    ('tcs_native_client_off', '*', 'customer.native_client.enabled', 'global', 'global', FALSE,
     '微信小程序/原生 App 客户端渠道默认关闭（#3）：关时走 Web 客户端代偿（H5/Web 完成下单/查询/核销，'
     || '不依赖小程序/原生壳、不阻塞）；待小程序/原生 App 接入 + 后台 updateCapabilitySwitch 开启后启用', now(), now())
ON CONFLICT (terminal_type, capability_code, scope_type, scope_id) DO NOTHING;

-- 权限：开关管理复用既有 terminal.switch.manage（切换）/ terminal.switch.view（读取），不新增权限点。

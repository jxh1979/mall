-- 文件版本：V0.01
-- V605（第十八批 W 段第 194 条 194-R3，控制席并入）：终端开关矩阵「说明」列显示的种子说明改为中文，只改说明列（reason），照各自种子原意翻译，不加新含义。
-- 依据：PGM:287「……展示给用户的一切文字……必须全部为中文，不得出现任何英文……可以不用中文的只有四样，没有第五样：单据号、设备编号、
--   二维码内容、系统内部标识符。」历史迁移不回改，向前修；按原种子说明逐条匹配（后台切过开关的行说明已换成当次操作说明，不再匹配）。
-- 「客人 App」「商户 App」「Web 前端」是 HTML 自身的终端名（PGM:724-725），V197 总开关说明照旧。

-- V071 第三方同步八行
UPDATE terminal_capability_switches SET reason = '第五轮第 14 项：第三方默认关闭，待厂商接入 + 后台开启'
 WHERE reason = 'R5 T14 第三方默认关闭，待厂商接入 + 后台开启';

-- V088 移动设备管控
UPDATE terminal_capability_switches
   SET reason = '第五轮第 9 项：移动设备管控自有侧默认关闭：未纳管禁用由终端开关+人工准入代偿，安卓官方设备管理接口/商业移动设备管控接入并验收后开启'
 WHERE reason = 'R5 T9 MDM 自有侧默认关闭：未纳管禁用由终端开关+人工准入代偿，AMAPI/商业 MDM 接入并验收后开启';

-- V093 支付真实渠道
UPDATE terminal_capability_switches
   SET reason = '支付真实渠道默认关闭：关时走内部/占位结算（预留，人工/内部确认，不阻塞）；'
             || '待真实渠道开发包接入 + 后台修改端能力开关开启后路由至渠道适配器'
 WHERE reason = '支付真实渠道默认关闭：关时走内部/占位结算（reserved，人工/内部确认，不阻塞）；'
             || '待真实渠道 SDK 接入 + 后台 updateCapabilitySwitch 开启后路由至渠道适配器';

-- V096 企微人工智能语义质检（权限名同法，见 V430）
UPDATE terminal_capability_switches
   SET reason = '企微人工智能语义质检默认关闭：关时仅走基础关键词质检（风险标记，不调人工智能、不阻塞）；'
             || '待真实人工智能/大模型质检开发包接入（或本地大模型）+ 后台修改端能力开关开启后路由至人工智能适配器'
 WHERE reason = '企微 AI 语义质检默认关闭：关时仅走基础关键词质检（risk_flags，不调 AI、不阻塞）；'
             || '待真实 AI/LLM 质检 SDK 接入（或本地 LLM）+ 后台 updateCapabilitySwitch 开启后路由至 AI 适配器';

-- V097 现场摄像头扫码
UPDATE terminal_capability_switches
   SET reason = '现场摄像头扫码硬件能力默认关闭（#2）：关时端侧用文本输入凭证代偿（手输票号/会员码/设备码，'
             || '不调摄像头/扫码开发包、不阻塞）；待真实硬件扫码开发包接入 + 后台修改端能力开关开启后启用'
 WHERE reason = '现场摄像头扫码硬件能力默认关闭（#2）：关时端侧用文本输入 credential 代偿（手输票号/会员码/设备码，'
             || '不调摄像头/扫码 SDK、不阻塞）；待真实硬件扫码 SDK 接入 + 后台 updateCapabilitySwitch 开启后启用';

-- V098 员工设备密钥库绑定、平板终端
UPDATE terminal_capability_switches
   SET reason = '员工设备安卓密钥库设备公钥/指纹绑定能力默认关闭（第 1 项）：关时端侧用占位/手工，不强制密钥库'
             || '（不真机取密钥库公钥指纹、绑定走占位/手工录入、不阻塞）；后端员工设备凭证接口已就绪，'
             || '本开关仅预留端侧密钥库取值能力，待端侧真机密钥库接入 + 后台修改端能力开关开启后启用'
 WHERE reason = '员工设备 Keystore 设备公钥/指纹绑定能力默认关闭（item1）：关时端侧用占位/手工，不强制 Keystore'
             || '（不真机取 Keystore 公钥指纹、绑定走占位/手工录入、不阻塞）；后端 staff_device_credentials 接口已就绪，'
             || '本开关仅预留端侧 Keystore 取值能力，待端侧真机 Keystore 接入 + 后台 updateCapabilitySwitch 开启后启用';

UPDATE terminal_capability_switches
   SET reason = '平板终端能力默认关闭（②）：关时平板端侧能力预留未启用（不阻塞现有流程）；'
             || '待平板现场操作接入 + 后台修改端能力开关开启后启用'
 WHERE reason = '平板终端能力默认关闭（②）：关时平板端侧能力预留未启用（不阻塞现有流程）；'
             || '待平板现场操作接入 + 后台 updateCapabilitySwitch 开启后启用';

-- V146 人脸入场闸
UPDATE terminal_capability_switches
   SET reason = '人脸入场闸默认关闭：关时入场不阻塞、年龄由制度兜底（豁免，残留风险已知会）；'
             || '待真实人脸/实名开发包接入 + 后台修改端能力开关开启后路由至厂商适配器（判龄失败一律拒绝）'
 WHERE reason = '人脸入场闸默认关闭：关时入场不阻塞、年龄由制度兜底（豁免，残留风险已知会）；'
             || '待真实人脸/实名 SDK 接入 + 后台 updateCapabilitySwitch 开启后路由至厂商适配器（fail-closed 判龄）';

-- V177 短信总开关、域开关、事件开关
UPDATE terminal_capability_switches SET reason = '短信通知总开关：默认关闭，待短信服务商凭证、模板与运维手册验收通过后开启'
 WHERE reason = 'SMS notification master switch: default off until provider credentials, templates, and operational runbook are accepted';
UPDATE terminal_capability_switches SET reason = '存酒全流程通知的短信域开关；受短信通知总开关控制'
 WHERE reason = 'SMS notification domain switch for stored wine lifecycle notices; gated by sms_notification master switch';
UPDATE terminal_capability_switches SET reason = '储值余额通知的短信域开关；受短信通知总开关控制'
 WHERE reason = 'SMS notification domain switch for stored value balance notices; gated by sms_notification master switch';
UPDATE terminal_capability_switches SET reason = '商户费用账单通知的短信域开关；受短信通知总开关控制'
 WHERE reason = 'SMS notification domain switch for merchant fee billing notices; gated by sms_notification master switch';
UPDATE terminal_capability_switches SET reason = '存酒存入确认通知的短信事件开关'
 WHERE reason = 'SMS notification event switch for stored wine deposit confirmation notices';
UPDATE terminal_capability_switches SET reason = '存酒取酒通知的短信事件开关'
 WHERE reason = 'SMS notification event switch for stored wine claim pickup notices';
UPDATE terminal_capability_switches SET reason = '存酒即将到期提醒的短信事件开关'
 WHERE reason = 'SMS notification event switch for stored wine expiring reminder notices';
UPDATE terminal_capability_switches SET reason = '存酒已过期通知的短信事件开关'
 WHERE reason = 'SMS notification event switch for stored wine expired notices';
UPDATE terminal_capability_switches SET reason = '储值充值通知的短信事件开关'
 WHERE reason = 'SMS notification event switch for stored value top-up notices';
UPDATE terminal_capability_switches SET reason = '储值消费通知的短信事件开关'
 WHERE reason = 'SMS notification event switch for stored value consumption notices';
UPDATE terminal_capability_switches SET reason = '商户房租账单通知的短信事件开关'
 WHERE reason = 'SMS notification event switch for merchant rent fee notices';
UPDATE terminal_capability_switches SET reason = '商户水电费账单通知的短信事件开关'
 WHERE reason = 'SMS notification event switch for merchant utilities fee notices';
UPDATE terminal_capability_switches SET reason = '商户物业费账单通知的短信事件开关'
 WHERE reason = 'SMS notification event switch for merchant property fee notices';
UPDATE terminal_capability_switches SET reason = '商户续约费用通知的短信事件开关'
 WHERE reason = 'SMS notification event switch for merchant renewal fee notices';

-- V198 网页后台十三个模块行
UPDATE terminal_capability_switches SET reason = replace(reason, 'Web 后台·', '网页后台·')
 WHERE reason LIKE 'Web 后台·%';

-- V545 订台短信域开关
UPDATE terminal_capability_switches SET reason = '订台短信域开关（业务说明地图第 1596 行）'
 WHERE reason = '订台短信域开关（BSM L1596）';

-- V554、V595、V600 已发布在用能力显式开启行
UPDATE terminal_capability_switches SET reason = '已发布在用能力显式开启（平台治理地图第 742 行：缺行默认关闭，须显式打开）'
 WHERE reason = '已发布在用能力显式开启（PGM L742 opt-in）';

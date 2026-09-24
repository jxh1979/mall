# XM-M036-A origin/main 后端全量既有红与空间种类删除申请主键撞键（合成模块，D-031）
版本：V0.01

2026-09-24 收口（操作员 2026-09-23「解决这些」；ledger D-031 按 D-027 先例单独立项）。来源：M036 收口登记的 origin/main 后端全量既有红（3ca1b507 起 20 条）、XM-D029 期间追加的 3 条与空间种类删除申请主键撞键。范围：开工段 21 个文件（8 个红测试文件、失败路径上 13 个生产文件）；判据取锁内 M009、M011、M054、M056、M077、M153、M158、M230、M232、M262、M263、M272、M331、M335、M396、M397 映射并集 297 项，只读引用。

流程：B1 双 FAIL（Grok GR-01…09／席甲 CX-01…09）→ B2-r1 双 PASS 定稿 XM-M036-A-001…008（Grok 撤回「游乐只改测试」「员工档案整体豁免」「本模块改工单与预订」；席甲更正 CustomerService:81 为构造器、治理地图行号为第 738 行）→ B3-r1（12 文件 150 增 40 删，冻结 /home/james/audit-tmp/xm036a_b3r1.diff SHA256 800824fe472eebd5d75dca3fc235a5d88367cb5e90c79d05400eb6daec194d4e；RED 10 条新增或改动用例在旧生产上红 → GREEN 3536 测 0 红）→ B4-r1 双 PASS（30 块越界 0）→ B5-r1 双 PASS：Grok V0.01 SHA256 802d0b9e3cd7c8792281b261d4ae1eedffcd393afc2d33e857a691b550672eb9；席甲 V0.01 SHA256 5656bb054d5ef213cb5a96b6b1d03de64daf1e123b92784f3cc3d60d0274352c；四值全等 b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458。B3 累计 1。Grok 票均为 grok-4.7/xhigh。

根因与修复摘要：
- 001 财务控制器 12 条：ceca1a54 给 StoredValueService 加了必需字段注入，测试以 `@Bean` 交出的 mock 仍被字段注入，切片起不来。改用 `@MockitoBean`。
- 002 合规 3 条：8d2fafff 起无终端身份的写请求被兜底拒绝（治理地图第 738 行），测试未带身份。三个 POST 带服务端派生的终端身份属性。
- 003 储值注解 1 条：ceca1a54 把 consume 拆为 payTicketOrder。测试改按现名校验。
- 004 FieldLabels 1 条：1aa4c9c2 新增 business_entity_id 抛点，补中文名「经营主体」。
- 005 设施设备 3 条：测试以平台管理员审批（ceca1a54 后被拒）；生产审批人不限最高权限、最高权限发起仍进待审批（BSM 第 577 行）。审批只认最高权限本人，最高权限发起即直接删除（占用检查照走）。
- 006 游乐项目权益绑定 1 条：同上（BSM 第 570、576 行），且最高权限被 checkProjectOwnership 的商户范围拦住。审批与墓碑恢复只认最高权限本人，最高权限发起即直接删除，三条删除写入体对最高权限按单据放行。
- 007 员工档案 2 条：1aa4c9c2 起最高权限不是平台管理员桥，被公司范围拦住（生产 403 CROSS_ORG_FORBIDDEN）。两条删除路径对最高权限按存在取档案。
- 008 空间种类删除申请：主键含毫秒时间戳，同毫秒撞键。改随机主键并补连续申请回归。
未改 AuthorizationService 范围方法、checkProjectOwnership、requireProfileInScope、requireRegistrationScope 本体与各列表口径。

跨模块观察（cross_module_observations）：
- XM-M036A-1（→M032 AuthorizationService 与各调用模块，宜单独立项）：1aa4c9c2 起最高权限（master_authority）不是平台管理员桥，currentMerchantScope 对其抛 MERCHANT_SCOPE_REQUIRED、currentProjectScopeQuietly 落到 platform-company；调 currentMerchantScope 的 Java 生产文件 42 个，调 requirePlatformScope／requireProjectScope 的并集 25 个，main 里显式处理最高权限的只有 AccessService、AuthorizationService、StaffService、PlatformService、SupervisorReviewService。与治理地图第 528 行第④类「逐笔开放……留痕」不符，须统一单据级放行＋留痕、列表不整表的机制。
- XM-M036A-2（删除审批人不限最高权限的同句入口，→各模块）：MerchantServiceService.approveDeleteWorkOrder／MerchantServiceController:106（M266）、BarService.doApproveDeleteReservation／BarController:419（M193）、BarPosCatalogController:84/133/181（M170）、TicketingController:86/157/246（M132）、PlatformController:139（M009）、ServiceOrderService:132（M201）、DataCleanupController:54（M075），按 BSM 第 570、576、577 行处置。
- 观察（→M230）：游乐权益删除审批的仓储更新只改 delete_status、无删前占用检查（BSM 第 573 行）。
- 观察：M010RegressionTests 的最高权限夹具带项目号 m010_p，恰好绕过员工档案公司范围，掩盖了 007。
- 观察（→M003/M005，已登记）：CI 不跑后端测试；ceca1a54、8d2fafff、1aa4c9c2 均未跑后端全量即合并，是这批既有红积累的根因。

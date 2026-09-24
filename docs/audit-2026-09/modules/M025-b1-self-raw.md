CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M025 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-20T22:23:38Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`（落后 origin/main 6da4e51b；本文件含 2026-09-11 两个未合并本地提交 1c59c6a8、173ef102，对应 OPEN PR #445，审计对象为主工作区现状）。
- 模块 M025「商户档案、商户类型与商户侧门户／运行和发布脚本／第1段」1 脚本，79 判据（BEQR 37、BSM 14、PGM 10、GEN 18）。B3 累计 0。
- 顺序：Grok 已先派（modules/M025-b1-grok-raw.md 存在=False），本报告落盘前未读其正文。只读承诺：未改任何文件；未运行本脚本；未调用任何接口（含登录）；数据库只对 complex_test 做 SELECT。

## 1. 文件覆盖表

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| ops/160/seed-dev-bars.sh | 55 | 2642 | bf65ae73f92fb105d848d866212694bcd7daf3c9a284c9630713910f10123044 | 全读（L1-55） |

## 2. 可执行证据原文（/home/james/audit-tmp/m025_b1_run.log）

```
START=2026-09-20T22:20:37Z HEAD=e456ba96 origin/main=6da4e51b
== 0. file (lines/bytes/sha256/mode)
ops/160/seed-dev-bars.sh 55 2642 bf65ae73f92fb105d848d866212694bcd7daf3c9a284c9630713910f10123044 775 index-mode=100755
== 1. vs HEAD / vs origin/main / local unmerged commits / open PR
 1 file changed, 6 insertions(+), 6 deletions(-)
173ef102 audit M025: seed merchants as pending admission, not in-operation
1c59c6a8 audit M025: seed script comments do not legislate entity or admission
445 audit-m025-r1 audit M025: seed pending admission
-# 开发环境:按「招商 / 商户准入」正路建 N 家酒吧商户(酒吧A98、酒吧B98 …)。
+# 开发环境:用后台建商户接口播 N 家酒吧商户(酒吧A98、酒吧B98 …),供联调。
+# 本脚本不替代招商、合同、准入审批,也不把测试数据写进正式迁移。
-# 为什么是脚本不是迁移:酒吧＝商户(merchant_type='bar'),不是经营主体;建商户是招商的业务动作,
-#   走后台正路才有审批与留痕。把 10 家写进 Flyway 迁移链会把开发测试数据固化进正式发布链,
-#   而且用户明确「酒吧数量必须可配 —— 可能七八家,也可能二三十家」,写死家数正好违背这条。
+# 为什么是脚本不是迁移:把固定家数写进 Flyway 会把开发数据固化进发布链;
+#   酒吧家数必须可配(七八家到二三十家),写死家数正好违背这条。
-# 娱乐平台公司参股各酒吧(业务口径),故所属经营主体挂娱乐平台公司(V354 稳定 id)。
+# 开发播种挂一个已有经营主体,只为接口必填;不表示各酒吧生产上同一经营主体,也不表示参股等于经营主体。
-    \"status\":\"active\",
+    \"status\":\"pending_admission\",
== 2. bash syntax / shellcheck
BASH_N_EXIT=0
shellcheck not installed
== 3. login endpoint: path, request fields, response token field
backend/src/main/java/com/complex/platform/auth/AuthController.java:15:@RequestMapping("/api/v1/auth")
backend/src/main/java/com/complex/platform/auth/AuthController.java:24:    @PostMapping("/login")
backend/src/main/java/com/complex/platform/auth/AuthCookies.java:34:    private static final String AUTH_PATH = "/api/v1/auth";
backend/src/main/java/com/complex/platform/auth/MasterAuthController.java:34:    @PostMapping("/login")
backend/src/main/java/com/complex/platform/auth/ProductionAuthController.java:15:@RequestMapping("/api/v1/auth")
LoginRequest: backend/src/main/java/com/complex/platform/auth/AuthService.java
    public record LoginRequest(
            @JsonAlias("username")
token carriers: backend/src/main/java/com/complex/platform/auth/AuthController.java backend/src/main/java/com/complex/platform/auth/AuthCookies.java backend/src/main/java/com/complex/platform/auth/AuthService.java 
backend/src/main/java/com/complex/platform/auth/AuthController.java:32:                result.accessToken(), result.refreshToken(), AuthCookies.newCsrfToken());
backend/src/main/java/com/complex/platform/auth/AuthController.java:49:        String accessToken = bearerToken(authorizationHeader);
backend/src/main/java/com/complex/platform/auth/AuthController.java:50:        if (accessToken == null) {
backend/src/main/java/com/complex/platform/auth/AuthController.java:51:            accessToken = AuthCookies.readAccessToken(httpRequest);
== 4. JSON naming strategy (script reads m.get('merchantName') from GET response)
backend/src/main/resources/application.yml:16:    property-naming-strategy: SNAKE_CASE
backend/src/main/java/com/complex/platform/common/api/ApiResponse.java:7: * 统一响应包络。Jackson SNAKE_CASE 序列化 → success / code / message / data / request_id / server_time / timestamp，
backend/src/main/java/com/complex/platform/config/SecurityConfig.java:11:import com.fasterxml.jackson.databind.PropertyNamingStrategies;
backend/src/main/java/com/complex/platform/config/SecurityConfig.java:32:    // （SNAKE_CASE + ISO 时间），保证 401 信封字段名与业务响应一致（detail_code/request_id/...）。
backend/src/main/java/com/complex/platform/config/SecurityConfig.java:36:            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
== 5. merchants endpoints: list params + create request fields + validations
59:    @GetMapping("/merchants")
71:    @PostMapping("/merchants")
59:     @GetMapping("/merchants")
60:     public ApiResponse<PageResponse<MerchantRecord>> listMerchants(
61:             @org.springframework.web.bind.annotation.RequestParam(name = "includeArchived", required = false, defaultValue = "false") boolean includeArchived,
62:             // V348+V384 按业态查询：merchantType=bar|catering|retail|amusement|media|other，缺省＝全部业态。
63:             @org.springframework.web.bind.annotation.RequestParam(name = "merchantType", required = false) String merchantType,
64:             @org.springframework.web.bind.annotation.RequestParam(name = "page", defaultValue = "1") int page,
65:             @org.springframework.web.bind.annotation.RequestParam(name = "size", defaultValue = "500") int size) {
66:         authorizationService.requirePermission("merchant.profile.view");
67:         authorizationService.requirePlatformScope();
68:         return ApiResponse.ok(maskPhonesForUnauthorized(service.listMerchants(includeArchived, merchantType, page, size)));
69:     }
MerchantRequest: 
== 6. service-side rules for create: status/merchant_type closed sets, entity existence, name uniqueness
services: backend/src/main/java/com/complex/platform/governance/PlatformService.java 
47:            Set.of("bar", "catering", "retail", "amusement", "media", "other");
55:            Set.of("pending_admission", "active", "suspended", "terminated", "blacklisted");
173:    /** M009-001/002：更新只覆盖档案字段；商户须已存在（404），存档/删除申请状态与 createdAt 原样保留。 */
371:                            .orElseThrow(() -> ApiException.notFound("BUSINESS_ENTITY_NOT_FOUND", "主体不存在"));
553:                                "该种类编码已存在，不能用新增改已有种类");
936:        if (!repository.businessEntityExists(id)) {
937:            throw ApiException.notFound("BUSINESS_ENTITY_NOT_FOUND", "主体不存在：" + id);
939:        if (!repository.businessEntityInProject(id, currentProject())) {
947:        boolean unbound = merchant.businessEntityId() == null && merchant.contractEntityId() == null
949:        if (!unbound && (!java.util.Objects.equals(merchant.businessEntityId(), operationId)
959:            throw ApiException.conflict("ACTIVE_BINDING_EXISTS", "该商户已存在生效或待生效的主体绑定，不能重复绑定");
998:                    String merchantName = requireText(request == null ? null : request.merchantName(), "merchant_name");
1011:                        if (trimToNull(request.businessEntityId()) != null) requireBusinessEntity(request.businessEntityId(), "business_entity_id");
1027:                            merchantName,
1029:                            existing == null ? trimToNull(request.businessEntityId()) : existing.businessEntityId(),
== 7. DB closed sets (complex_test, SELECT only): merchants status/type CHECK; be_ent_platform seed
chk_merchant_delete_status|CHECK (((delete_status)::text = ANY ((ARRAY['none'::character varying, 'pending'::character varying, 'approved'::character varying])::text[])))
chk_merchants_business_format|CHECK (((merchant_type)::text = ANY ((ARRAY['bar'::character varying, 'catering'::character varying, 'retail'::character varying, 'amusement'::character varying, 'media'::character varying, 'other'::character varying])::text[])))
chk_merchants_contact_phone_pii|CHECK (((contact_phone IS NULL) OR ((contact_phone)::text = ''::text) OR ((contact_phone)::text ~~ 'enc:v1:%'::text))) NOT VALID
chk_merchants_secondary_contact_phone_pii|CHECK (((secondary_contact_phone IS NULL) OR ((secondary_contact_phone)::text = ''::text) OR ((secondary_contact_phone)::text ~~ 'enc:v1:%'::text))) NOT VALID
be_ent_platform|娱乐平台公司
backend/src/main/resources/db/migration/V370__settlement_merge_retention_into_service_fee.sql
backend/src/main/resources/db/migration/V372__settlement_gift_cost_layer.sql
backend/src/main/resources/db/migration/V355__revenue_stream_attribution_and_ledger_entity.sql
backend/src/main/resources/db/migration/V354__business_entity_master_data.sql
backend/src/main/resources/db/migration/V375__settlement_pool_undistributed_and_unverified_attribution.sql
== 8. credentials sources named in script
~/.complex/complex.env exists: yes; keys there: 2
/etc/complex/complex.env keys: 2
./graft/tests/e2e/full-admin-mvp.spec.md
./docs/server-160-runbook-v0.2.md
./docs/ui-automation-acceptance-report-v0.1.md
./docs/ops/keyvault-usb-runbook.md
./docs/audit-2026-09/modules/M003-b1-grok-raw.md
./docs/audit-2026-09/modules/M003-b5-r2-self-raw.md
./.scratch/module-recheck-20260908/unpublished-03acdb98/docs/server-160-runbook-v0.2.md
./.scratch/module-recheck-20260908/unpublished-03acdb98/docs/ui-automation-acceptance-report-v0.1.md
backend/src/main/resources/application-local.yml:12:      # 环境变量沿用项目既有约定 P0_ADMIN_USERNAME / P0_ADMIN_PASSWORD，
backend/src/main/resources/application-local.yml:16:        username: ${P0_ADMIN_USERNAME:admin}
backend/src/main/resources/application-local.yml:17:        password: ${P0_ADMIN_PASSWORD:admin-pass}
ops/160/seed-dev-bars.sh:24:  P0U=$(grep '^P0_ADMIN_USERNAME=' ~/.complex/complex.env | cut -d= -f2)
ops/160/seed-dev-bars.sh:25:  P0P=$(grep '^P0_ADMIN_PASSWORD=' ~/.complex/complex.env | cut -d= -f2)
ops/160/complex.env.example:55:P0_ADMIN_USERNAME=<seeded-p0-smoke-admin>
ops/160/complex.env.example:56:P0_ADMIN_PASSWORD=<fill-from-credential-ledger>
ops/160/complex-lan-prod.sh:119:    P0_ADMIN_USERNAME="${P0_ADMIN_USERNAME:-}" \
== 9. who references this script
./docs/business-decision-log-v0.1.md:907:- **决策**：① **六个经营主体的骨架进迁移**（V354：平台公司 / 长沙项目公司 / 娱乐平台公司 / 中央餐饮公司 / 游乐公司
./.scratch/module-recheck-20260908/legacy-87-module-source.json:244:        "ops/160/seed-dev-bars.sh"
./.scratch/module-recheck-20260908/legacy-87-module-source.json:259:        "ops/160/seed-dev-bars.sh"
./.scratch/module-recheck-20260908/plan-candidate/modules.md:392:ops/160/seed-dev-bars.sh
./.scratch/module-recheck-20260908/before-lock-amend/modules.md:5895:ops/160/seed-dev-bars.sh
./.scratch/module-recheck-20260908/unpublished-03acdb98/docs/business-decision-log-v0.1.md:907:- **决策**：① **六个经营主体的骨架进迁移**（V354：平台公司 / 长沙项目公司 / 
./.scratch/module-recheck-20260908/unpublished-03acdb98/ops/160/seed-dev-bars.sh:8:# 家数是参数:  ./seed-dev-bars.sh          → 10 家(A98…J98,与 2026-07 主数据一致)
./.scratch/module-recheck-20260908/unpublished-03acdb98/ops/160/seed-dev-bars.sh:9:#              ./seed-dev-bars.sh 25       → 25 家
== 10. idempotency for create: replay behaviour on same key
3:import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;
55:        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
74:        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
84:        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
93:        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
102:        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
113:        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
122:        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
== 11. three HTML + lock sha
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-20T22:20:39Z
```

补充只读核对（本席会话内执行，结果摘录）：
- 后端响应键名：`backend/src/main/resources/application.yml:16` `property-naming-strategy: SNAKE_CASE`；`PlatformControllerTests.java:150` `jsonPath("$.data.items[0].merchant_name").value("P0 Merchant")`；`docs/openapi/openapi-p0.yaml:28860/28878` `merchant_name`。
- 把脚本 L38 的同款 python 表达式喂一份后端形状（蛇形键）的样例：`…"items":[{"id":"m-1","merchant_name":"酒吧A98"}]…` → 输出 `EXISTS=[]`；把键换成 `merchant_name` → `[m-1]`。
- 幂等重放：`IdempotencyService.replay` L132-135 同键不同请求哈希 → `409 IDEMPOTENCY_CONFLICT「该幂等键已被不同的请求使用，请刷新后重试」`；同键同体 → 返回首次响应。
- 请求字段：`AuthService.LoginRequest(loginName[@JsonAlias username], password, …)`；`MerchantProfileRequest` 含 idempotencyKey/operatorNote/merchantName/merchantType/businessEntityId/contactName/status；`PlatformService` `MERCHANT_STATUS` 含 pending_admission、业态闭集含 bar。
- complex_test：`merchants` 无「酒吧%98」行、`idempotency_records` 无 `seed-bar-%` 键（SELECT 0 行）——测试库未播过；真实联调库本席按只读约束未查。

## 3. 三线审计

- 第一性原理线。用途：给联调环境按可配家数建酒吧商户档案，且可重复运行。实际行为：登录 → 逐家「查重 → 创建」。未经验证的前提：「GET 返回的商户对象里有 `merchantName` 键」——后端全局 SNAKE_CASE，真实键是 `merchant_name`，查重永不命中；脚本实际靠服务端幂等键兜底，而这层兜底在请求体变化后会变成 409 并中断。
- 顺从式线：79 判据逐项见 §4；55 行逐行读过。
- 对抗式线（grilling）：第一次在空库跑→10 家建成，正常；第二次原样再跑→不打印「跳过」，而是对每家再 POST 一次、靠幂等重放返回同一 id、打印「建成」——与头注声称不符但无重复数据；在 2026-07 已用旧版（status=active）播过的环境上跑现版→首家即 409 退出（请求体变了、键没变、查重又不生效）；家数从 10 改 25→前 10 家同上，后 15 家新建；商户超过 500 家→查重取不到第 501 家以后（理论场景，不立项）；`COUNT` 给非数字→bash 算术报错退出，无副作用；生产环境误跑→L22 会先拿开发默认口令试登一次（失败后走环境文件账号），留下一条失败登录记录，登记 OBS-2。

## 4. 79 判据逐项

| 判据 | 适用｜证据｜结论 |
|---|---|
| BEQR-085 项目开放状态 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「项目开放状态」相关对象或状态 |
| BEQR-086 场次状态 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「场次状态」相关对象或状态 |
| BEQR-087 游乐「当天／每天」口径 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「游乐「当天／每天」口径」相关对象或状态 |
| BEQR-088 容量双阈值 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「容量双阈值」相关对象或状态 |
| BEQR-089 园区容量 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「园区容量」相关对象或状态 |
| BEQR-090 设备关键/辅助分级 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「设备关键/辅助分级」相关对象或状态 |
| BEQR-091 全园故障与补偿通票 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「全园故障与补偿通票」相关对象或状态 |
| BEQR-092 安全严重度 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「安全严重度」相关对象或状态 |
| BEQR-093 巡检任务状态 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「巡检任务状态」相关对象或状态 |
| BEQR-094 越界类型 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「越界类型」相关对象或状态 |
| BEQR-095 团体/包场 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「团体/包场」相关对象或状态 |
| BEQR-096 游乐增强阶段 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「游乐增强阶段」相关对象或状态 |
| BEQR-118 运营营业日 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「运营营业日」相关对象或状态 |
| BEQR-146 运营公司职责 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「运营公司职责」相关对象或状态 |
| BEQR-155 商户/运营主体状态 | 适用｜脚本建商户用 `status: pending_admission`（L46），属枚举「待准入、正常、暂停、终止、归档、黑名单」闭集首态；后端 `MERCHANT_STATUS` 含该值（探针 6）。开发播种不把商户直接置「正常」，不绕开准入｜通过 |
| BEQR-160 招商线索状态 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「招商线索状态」相关对象或状态 |
| BEQR-161 招商意向阶段 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「招商意向阶段」相关对象或状态 |
| BEQR-162 品牌/客户库类型 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「品牌/客户库类型」相关对象或状态 |
| BEQR-163 合同变更类型 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「合同变更类型」相关对象或状态 |
| BEQR-164 退租结算状态 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「退租结算状态」相关对象或状态 |
| BEQR-165 内部应付执行状态 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「内部应付执行状态」相关对象或状态 |
| BEQR-166 合同审批链 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「合同审批链」相关对象或状态 |
| BEQR-167 租金/账单类型 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「租金/账单类型」相关对象或状态 |
| BEQR-168 营业额抽成模式 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「营业额抽成模式」相关对象或状态 |
| BEQR-169 公区能源费 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「公区能源费」相关对象或状态 |
| BEQR-170 铺位租赁状态 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「铺位租赁状态」相关对象或状态 |
| BEQR-171 物业工单类型 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「物业工单类型」相关对象或状态 |
| BEQR-172 物业工单状态 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「物业工单状态」相关对象或状态 |
| BEQR-173 商户服务工单状态 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「商户服务工单状态」相关对象或状态 |
| BEQR-174 工单时效 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「工单时效」相关对象或状态 |
| BEQR-175 楼宇机电设备状态 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「楼宇机电设备状态」相关对象或状态 |
| BEQR-176 设备维保类型 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「设备维保类型」相关对象或状态 |
| BEQR-177 能耗表计类型 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「能耗表计类型」相关对象或状态 |
| BEQR-178 抄表状态 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「抄表状态」相关对象或状态 |
| BEQR-179 能耗归集对象 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「能耗归集对象」相关对象或状态 |
| BEQR-180 楼宇巡检任务状态 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「楼宇巡检任务状态」相关对象或状态 |
| BEQR-181 楼宇巡检类型 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「楼宇巡检类型」相关对象或状态 |
| BSM-020 楼宇运营 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「楼宇运营」相关对象或状态 |
| BSM-028 主要功能 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「主要功能」相关对象或状态 |
| BSM-081 楼宇运营：机电设备、能耗抄表、楼宇巡检 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「楼宇运营：机电设备、能耗抄表、楼宇巡检」相关对象或状态 |
| BSM-082 主要功能 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「主要功能」相关对象或状态 |
| BSM-083 和谁有关 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「和谁有关」相关对象或状态 |
| BSM-111 经营看板：让管理层和运营看得懂、能追问 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「经营看板：让管理层和运营看得懂、能追问」相关对象或状态 |
| BSM-112 主要指标 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「主要指标」相关对象或状态 |
| BSM-113 谁会看 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「谁会看」相关对象或状态 |
| BSM-114 各类入口：不同人只看自己该看的、只做自己该做的 | 无本面｜脚本只用平台管理员令牌调后台接口，不涉及各类入口的可见范围 |
| BSM-115 相互关系：为什么这些子系统必须连在一起 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「相互关系：为什么这些子系统必须连在一起」相关对象或状态 |
| BSM-116 系统交付与上线：正式上线要做的安全加固和安装交付 | 适用（引用面）｜脚本头注 L3、L5 明写不把测试数据写进正式迁移、不进发布链；全库只有决策日志与本脚本自身引用它，未被安装/发布脚本调用（探针 9）｜通过 |
| BSM-117 正式上线时的安全加固 | 适用｜L22 先试开发默认口令 `admin/admin-pass`（`application-local.yml:16-17` 的本地默认），失败才读 `~/.complex/complex.env` 的 P0 账号；脚本不写入、不回显口令。生产换强口令后第一步自然失败并走第二步，不构成把弱口令带上生产｜通过（另见 OBS-2） |
| BSM-118 能自动识别环境的安装包 + 内附教程 | 无本面｜不是安装包/教程 |
| BSM-119 和谁有关 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「和谁有关」相关对象或状态 |
| GEN-001 必要性与最小性 | 适用｜55 行单一用途（联调播种），无多余分支；家数参数化｜通过 |
| GEN-002 可执行正确性 | 适用｜头注 L10 声称「幂等:同名商户已存在则跳过(按 merchant_name 查)」，L38 实际按 `merchantName` 取键，而后端响应为 SNAKE_CASE，永不命中（CX-M025-001）｜不通过 |
| GEN-003 测试有效性 | 适用｜脚本无任何自动化检查；「已存在则跳过」这条关键分支没有会失败的验证，键名写错至今未被发现（并入 CX-M025-001 处置的可执行自检）｜不通过 |
| GEN-004 信任边界校验 | 适用｜`COUNT` 来自命令行，直接进 `seq`/算术；非数字输入时 bash 算术报错退出，不会造成写库副作用；`NAME` 由脚本自生成后拼进 python 源码，非外部输入｜通过 |
| GEN-005 授权与最小权限 | 适用｜用平台管理员令牌调 `merchant.profile.view/manage` 接口，后端自行鉴权；脚本不绕权限｜通过 |
| GEN-006 数据完整性 | 适用｜重复运行不应产生重名商户：现状靠同一 `idempotency_key` 的服务端重放兜底（请求体不变时返回同一商户），不是靠脚本声称的查重；请求体一旦改过（如 2026-09-11 本地提交把 status 由 active 改为 pending_admission）同键不同体 → 409 `IDEMPOTENCY_CONFLICT`，脚本首家即退出（并入 CX-M025-001）｜不通过 |
| GEN-007 并发与幂等 | 适用｜每家固定 `idempotency_key: seed-bar-<后缀>98`，服务端幂等；见 GEN-006｜通过（键设计本身成立） |
| GEN-008 接口与模式一致性 | 适用｜脚本读响应的键名与后端序列化策略不一致（`merchantName` vs `merchant_name`；`application.yml:16` SNAKE_CASE，`PlatformControllerTests:150` 断言 `$.data.items[0].merchant_name`）；请求侧字段 `login_name/password/idempotency_key/business_entity_id/merchant_name/merchant_type/contact_name/status/operator_note` 与 `LoginRequest`、`MerchantProfileRequest` 一致；列表参数 `page/size` 与 Controller 一致｜不通过（CX-M025-001） |
| GEN-009 错误处理与恢复 | 适用｜创建失败时打印响应前 400 字并非零退出（L48-52）；`set -euo pipefail`；登录第一步失败被 `|| true` 吞掉后走第二步，第二步失败整脚本退出｜通过 |
| GEN-010 安全与敏感信息 | 适用｜口令从本机环境文件按键读取、不回显、不落盘；经 `curl -d` 传参会短暂出现在本机进程列表，开发机单用户场景，登记 OBS-3｜通过 |
| GEN-011 可观测与审计证据 | 适用｜创建走后台正式接口，后端按既有规则留痕；`operator_note` 写明「开发环境酒吧主数据播种」｜通过 |
| GEN-012 资源与性能边界 | 适用｜每家一次 GET（size=500）+ 一次 POST，家数上限由人给定；超过 500 家商户时查重会漏（理论场景，不立项）｜通过 |
| GEN-013 构建配置与依赖 | 适用｜依赖 bash、curl、python3、seq，均为 160 既有；文件模式 100755（探针 0）｜通过 |
| GEN-014 数据库迁移正确性 | 无本面｜不是迁移；头注说明了为什么不是迁移 |
| GEN-015 界面可用性与可访问性 | 无本面｜无界面 |
| GEN-016 文档可验证性 | 适用｜头注 L10 的幂等声称不可复现（CX-M025-001）；L8「10 家(A98…J98,与 2026-07 主数据一致)」中「与 2026-07 主数据一致」无法从纳入范围复现（complex_test 无该批商户，只读 SELECT 0 行），登记 OBS-1；其余注释（家数可配、挂已有经营主体只为必填）与代码一致｜不通过 |
| GEN-017 业务来源分类 | 适用｜唯一发现为技术性（脚本键名与后端序列化不一致），不涉业务规则；2026-09-11 两个本地提交把「立法式」注释改成了不立法表述，现文未发明业务规则｜通过 |
| GEN-018 第一性原理证据 | 适用｜结论落到只读命令输出（m025_b1_run.log、脚本同款 python 表达式对蛇形样例的实跑）、后端源码与测试原文、HTML 原文｜通过 |
| PGM-015 楼宇运营 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「楼宇运营」相关对象或状态 |
| PGM-018 物理项目与运营公司：多对多协作，不是固定隶属层级 | 适用（引用面）｜L30-31 把商户挂 `be_ent_platform` 只为接口必填，注释明说不表示生产上的经营主体关系、不表示参股等于经营主体——不立法｜通过 |
| PGM-026 主要功能 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「主要功能」相关对象或状态 |
| PGM-039 智能设备场景联动范围 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「智能设备场景联动范围」相关对象或状态 |
| PGM-052 楼宇运营：管好建筑本身的设备、能耗和巡检 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「楼宇运营：管好建筑本身的设备、能耗和巡检」相关对象或状态 |
| PGM-053 楼宇机电设备 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「楼宇机电设备」相关对象或状态 |
| PGM-054 能耗计量 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「能耗计量」相关对象或状态 |
| PGM-055 楼宇巡检 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「楼宇巡检」相关对象或状态 |
| PGM-056 和谁有关 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「和谁有关」相关对象或状态 |
| PGM-066 已拍板规则和待排期问题 | 无本面｜本脚本只调登录、商户列表、商户创建三个接口建酒吧商户档案，不读写、不展示、不声称「已拍板规则和待排期问题」相关对象或状态 |

## 5. 本次 Findings（临时编号）

### CX-M025-001 「同名已存在则跳过」按错键名查重，永不生效
- 位置：`ops/160/seed-dev-bars.sh:10`（头注「幂等:同名商户已存在则跳过(按 merchant_name 查)」）与 `:37-39`（`m.get('merchantName')=='$NAME'`）。
- 事实：后端 JSON 全局 SNAKE_CASE，商户列表项键为 `merchant_name`（§2 三处原文 + 同款表达式实跑 `EXISTS=[]`）。查重分支永不命中，「跳过(已存在)」永不打印。
- 正常触发/影响：第二次运行即可复现——每家都会再发一次创建请求；请求体与首次相同时靠服务端幂等重放不产生重复，但输出谎报「建成」；请求体与首次不同（本脚本 2026-09-11 已把 status 从 active 改为 pending_admission）时同键不同体 → 409，脚本在第一家退出，无法在已播过的联调环境上补播新增家数。
- 四分类：冲突？三 HTML 对开发播种脚本沉默，非冲突；细化？无上位业务句；→ **技术性**（脚本与后端序列化约定不一致）。工程缺陷：成立。判据 GEN-002 / GEN-008 / GEN-006 / GEN-003 / GEN-016。依据类型 4。
- 最小拟处置：L38 键名改 `merchant_name`（一处）；不改接口、不改幂等键、不加新功能。验证：同款表达式对蛇形样例由 `[]` 变 `[m-1]`（不调用接口）。

## 6. 观察（登记，不立项）

- OBS-1 L8「与 2026-07 主数据一致」无法从纳入范围复现（测试库无该批数据；真实联调库本席未查）。属历史说明，不影响行为。
- OBS-2 L22 每次运行先用开发默认口令 `admin/admin-pass` 试登；在已换口令的环境上会留一条失败登录记录。脚本限定开发联调用途，不立项。
- OBS-3 口令经 `curl -d` 传参，会短暂出现在本机进程列表；开发机场景，不立项。
- OBS-4 查重只取第一页 500 条；商户超过 500 家才会漏，不立项。
- OBS-5 主工作区该文件的两个本地提交（OPEN PR #445）未合并 origin/main；检查点会把现状带入。

## 7. 结论

席甲 B1 票：**FAIL**（CX-M025-001，技术性、工程缺陷成立）。1 文件全读、79 判据逐项、三线完成；未改任何文件、未运行脚本、未调用接口；落盘前未读 Grok 本次正文。


## M025 B2-r1 整批候选复核（席甲票，V0.02）

- 时间：2026-09-20T22:40:02Z。输入：modules/M025-b1-grok-raw.md V0.01（37899 bytes，SHA256 d39f496c6d7880038327042495bc4b32187ac0d5542ab8bedf2a6e115d226337）、modules/M025-b1-self-raw.md V0.01（上方本报告）；候选 findings.md 顶部「当前 M025 B2-r1 整批候选」段（全文件 SHA256 b75a04570cba9fcec40ca4ef13b222b2f45b5950f8d13b1d2d0ce574048db7da）。本票落盘前未读 Grok B2-r1 追加段。不写独占锁行。
- 差集：GR-01+CX-001→001；GR-02→002；GR-03→拟只登记；两席观察全部登记；无漏并、无改写。
- 自审纠正三处：①001 分类席甲 B1 判技术性——上位「建立商户档案」句存在，按四分类顺序细化在先，撤回，采 Grok。②001 影响面席甲 B1 写「重复运行无重复数据」——只在幂等记录 24 小时有效期内成立；过期后重跑会插入重名商户（`DEFAULT_TTL_SECONDS = 86_400`，`merchants` 无名称唯一约束），漏了，采 Grok。③GR-02 席甲漏：B1 只核了请求字段名存在，没核注释里「接口必填」这条声称；复核主工作区与 origin/main 两态该字段均可空，成立，采 Grok。
- GR-03：席甲核得其依据的三处校验在 origin/main 0 命中、只在主工作区已暂存未发布的 M009 改动里；触发证据取自 complex_test（非脚本目标环境）；脚本失败时打印响应并非零退出、不谎报成功。拟只登记、挂 M009 联动。此为席甲判断，Grok 有否决权；若其主张入队并给出不描述未发布规则的最小文字，以其意见为准再议。
- 处置只改本模块 1 文件：一处键名、一句注释前半、文件版本标记一行。不改接口、不加功能。
- 无添加项、无 HTML 申请、无 questions。
- 席甲 B2-r1 票：**PASS**（整批候选同意）。

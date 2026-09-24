CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M025 B5-r1 修复后全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理；脚本现行改动为席甲 B3 所写，以 Grok 票为准）。2026-09-20T22:56:38Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`；B3-r1 after tree `0395a637691b73a32a1eb1e81642cfc0313edbb5`。
- 模块 M025 1 脚本（§1 全读），79 判据；B3 累计 1。
- 顺序：Grok 已先派（modules/M025-b5-r1-grok-raw.md 存在=False），本报告落盘前未读其正文。只读承诺：未改任何文件；未运行被审脚本；未调用任何接口；数据库只对 complex_test SELECT。
- 本轮补的方法（B1 漏了 GR-02 与 24 小时过期后果）：注释里的每一条声称逐句对后端源码；每个「重复运行」场景按幂等记录有效期内/外分别推演。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| ops/160/seed-dev-bars.sh | 56 | 2685 | b44ad5d2614166a4a1aee5617ef46b5efebf7944564f4da7819aafe2f0e3c331 | 全读（L1-56） |

## 2. 可执行证据原文（/home/james/audit-tmp/m025_b5r1_run.log；B1 同一取证脚本修后重跑）

```
START=2026-09-20T22:56:15Z HEAD=e456ba96 origin/main=6da4e51b
== 0. file (lines/bytes/sha256/mode)
ops/160/seed-dev-bars.sh 56 2685 b44ad5d2614166a4a1aee5617ef46b5efebf7944564f4da7819aafe2f0e3c331 775 index-mode=100755
== 1. vs HEAD / vs origin/main / local unmerged commits / open PR
 1 file changed, 3 insertions(+), 2 deletions(-)
 1 file changed, 8 insertions(+), 7 deletions(-)
173ef102 audit M025: seed merchants as pending admission, not in-operation
1c59c6a8 audit M025: seed script comments do not legislate entity or admission
445 audit-m025-r1 audit M025: seed pending admission
-# 开发环境:按「招商 / 商户准入」正路建 N 家酒吧商户(酒吧A98、酒吧B98 …)。
+# 文件版本：V0.01
+# 开发环境:用后台建商户接口播 N 家酒吧商户(酒吧A98、酒吧B98 …),供联调。
+# 本脚本不替代招商、合同、准入审批,也不把测试数据写进正式迁移。
-# 为什么是脚本不是迁移:酒吧＝商户(merchant_type='bar'),不是经营主体;建商户是招商的业务动作,
-#   走后台正路才有审批与留痕。把 10 家写进 Flyway 迁移链会把开发测试数据固化进正式发布链,
-#   而且用户明确「酒吧数量必须可配 —— 可能七八家,也可能二三十家」,写死家数正好违背这条。
+# 为什么是脚本不是迁移:把固定家数写进 Flyway 会把开发数据固化进发布链;
+#   酒吧家数必须可配(七八家到二三十家),写死家数正好违背这条。
-# 娱乐平台公司参股各酒吧(业务口径),故所属经营主体挂娱乐平台公司(V354 稳定 id)。
+# 开发播种给商户填一个已有经营主体 id(接口不强制该字段);不表示各酒吧生产上同一经营主体,也不表示参股等于经营主体。
-    | python3 -c "import sys,json;d=json.load(sys.stdin)['data'];items=d['items'] if isinstance(d,dict) else d;print(next((m['id'] for m in items if m.get('merchantName')=='$NAME'),''))")
+    | python3 -c "import sys,json;d=json.load(sys.stdin)['data'];items=d['items'] if isinstance(d,dict) else d;print(next((m['id'] for m in items if m.get('merchant_name')=='$NAME'),''))")
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
./docs/audit-2026-09/modules/M025-b1-grok-raw.md
./docs/audit-2026-09/modules/M025-b1-self-raw.md
backend/src/main/resources/application-local.yml:12:      # 环境变量沿用项目既有约定 P0_ADMIN_USERNAME / P0_ADMIN_PASSWORD，
backend/src/main/resources/application-local.yml:16:        username: ${P0_ADMIN_USERNAME:admin}
backend/src/main/resources/application-local.yml:17:        password: ${P0_ADMIN_PASSWORD:admin-pass}
ops/160/seed-dev-bars.sh:25:  P0U=$(grep '^P0_ADMIN_USERNAME=' ~/.complex/complex.env | cut -d= -f2)
ops/160/seed-dev-bars.sh:26:  P0P=$(grep '^P0_ADMIN_PASSWORD=' ~/.complex/complex.env | cut -d= -f2)
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
END=2026-09-20T22:56:17Z
```

## 2b. 查重表达式自检 + bash -n（/home/james/audit-tmp/m025_b5r1_check.log）

```
lookup expression: import sys,json;d=json.load(sys.stdin)['data'];items=d['items'] if isinstance(d,dict) else d;print(next((m['id'] for m in items if m.get('me…
EXISTS=[mch_1] stderr=[]
文件版本行: ['# 文件版本：V0.01']
problems=0
SELFCHECK_EXIT=0
BASH_N_EXIT=0
```

## 3. 三线审计

- 第一性原理线：用途/实际行为同 B1。B1 指出的未经验证前提（响应里有 `merchantName` 键）已改为后端真实键 `merchant_name`；查重分支现在会命中。
- 顺从式线：79 判据逐项——B1 不通过的 GEN-002/003/006/008/016 由 001、002 闭合（GEN-003：本仓对该脚本无自动化测试面，自检脚本为审计取证用、未入库，登记）；BEQR-155、BSM-116/117、PGM-018、GEN-001/004/005/007/009/010/011/012/013/017/018 同 B1 通过；其余 BEQR/BSM/PGM 与 GEN-014/015 无本面（理由同 B1 §4 逐项）。
- 对抗式线（grilling）：空库第一次跑→逐家创建；原样第二次跑→查重命中、打印「跳过(已存在)」、不再 POST；隔天幂等记录过期后再跑→仍由查重挡住，不插重名；家数 10 改 25→前 10 家跳过、后 15 家新建；在旧版（status=active）播过的环境上跑现版→查重命中即跳过，不再触发同键不同体 409；`COUNT` 非数字→bash 算术报错退出；后端拒绝（含 GR-03 登记的项目范围场景）→打印响应前 400 字并非零退出，L56「完成」不打印，不谎报；注释逐句：L3-4 不替代招商/合同/准入审批、不写进迁移（属实，脚本只调建档接口）；L6-7 家数可配（属实）；L9「与 2026-07 主数据一致」不可复现（B1 OBS-1 已登记）；L11 幂等声称现与实现一致；L31 经营主体注释现与接口一致。无新缺陷、无添加项、无 HTML 问题。

## 4. 已定稿 001、002 现态

全部已修（B3-r1），B4-r1 双 PASS；修复队列「是」= 0。GR-03 只登记（挂 M009 未发布改动联动项）。

## 5. 本次 Findings

无新增。

## 6. 观察（登记）

- 同 B1/B2 登记项（GR-03 联动项；L9 历史说明不可复现；默认口令试登留失败记录；口令经 curl 传参；查重只取第一页 500 条；origin/main 旧版差异随检查点带入）。

## 7. 结论

席甲 B5-r1 票：**PASS**。1 脚本全读、79 判据逐项、取证脚本与自检复核；未改任何文件、未运行脚本、未调用接口；落盘前未读 Grok 本次正文。

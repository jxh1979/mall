CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

# M006 B4-r1 Grok 修复内容只读复审

版本：V0.01
席位：Grok（审计席乙，独立第一性原理 / 对抗式 + 顺从式）
控制：V0.17；模块 M006 / 322；B3=1；ledger IN_PROGRESS（本席不改 ledger）
范围：before tree `f49ef25cdadc0d4572f169d8c33b5e971c21f024` → after tree `e845764656fb7f9c166026371371b720f10fde2a`
动作：只读复审本批 25 项（001—018、024—030）统一修复 diff；不修源/计划/findings/ledger；不启动 B5；不读任何 M006 B4/B5 raw、不读双方 B1/B2 论证。

## 1. 技能预检（实际存在核验）

本席自核，合同限定只用本席，不增加第三角色/产物/人闸。

| 技能 | 存在性 | 本席用法 |
|---|---|---|
| using-superpowers | 存在 `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能自检 |
| i-have-adhd | 存在 | 输出形态；本文件为指定 raw，结构服从合同 |
| grilling / grill-me | 存在 | 对抗式自问自答；不派子代理、不等操作员 |
| batch-grill-me | **缺口：不存在** | 未冒充调用 |
| ubiquitous-language | **缺口：不存在** | 未冒充调用 |
| codebase-design | 存在 | 接口/缝/深度词汇，只作判据语言 |
| diagnosing-bugs | 存在 | 缺陷定位；只读假说 |
| tdd | 存在 | 测缝确认按铁律 5 作废；只核已有测试是否能失败 |
| git-guardrails-claude-code | 存在 | 只读；未改 git 配置、未 force push |
| writing-for-agents | 存在 | 本 raw 供下游席位读取 |
| systematic-debugging | 存在 | 只借读错误→复现→追数据流；不改生产代码 |
| verification-before-completion | 存在 | PASS 前实跑命令 |
| receiving-code-review | 存在 | 本席是审方；只借先核实、禁止表演性同意；B4 禁止修复 |
| requesting-code-review | 存在 | Codex 派本席用；本席不派第三席 |
| ponytail-review | 存在 | 只找可删除/过工程；不改码 |
| ponytail | 存在 | B3 修复强度，B4 不实施 |
| mattpocock code-review | 存在但合同划掉 | 未调用（会问人、会 `HEAD` diff、会派子代理） |
| dispatching-parallel-agents / subagent-driven-development / using-git-worktrees | 存在但合同划掉 | 未调用 |

未把 Grok 原始 JSON 落入任何文件。

## 2. 独立实算锁哈希与输入核验

命令：

```bash
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
cat docs/audit-2026-09/plan/criteria-lock.sha256
sha256sum docs/audit-2026-09/findings.md
stat -c 'size=%s' docs/audit-2026-09/findings.md
git cat-file -t f49ef25cdadc0d4572f169d8c33b5e971c21f024
git cat-file -t e845764656fb7f9c166026371371b720f10fde2a
```

结果：

- criteria-lock.md 实算 SHA256 = `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`
- sidecar 同行相等
- ledger 顶部同行（只读 `head`，未改）
- findings.md size=224530，SHA256=`54bf9e3141612f7d87d71bb8c9294103c5cac382c13e31b6b05efded6ab66ee1`，版本 V0.58
- 两对象均为 tree

M006 映射（criteria-lock.md L6386）：GEN-001…018, BEQR-157/182–187, BSM-001/003/009/028/029/073/084–088/114/115/120, PGM-001/008/022/024/026/032–036/048/059/066。

findings 唯一当前队列表：001—018、024—030 修复队列=是；019—023=否。本席按该表归因，未把否项当修复授权。

## 3. 两树 diff 核验（完整未过滤）

命令（未创建第三 tree，未读当前工作区全量 diff，未按源路径过滤）：

```bash
git diff --binary f49ef25cdadc0d4572f169d8c33b5e971c21f024 e845764656fb7f9c166026371371b720f10fde2a
git -c core.whitespace=blank-at-eol,blank-at-eof,space-before-tab,cr-at-eol diff --check \
  f49ef25cdadc0d4572f169d8c33b5e971c21f024 e845764656fb7f9c166026371371b720f10fde2a
```

未改 `core.whitespace` 配置（仅 `-c` 临时）。

| 项 | 预期 | 实算 |
|---|---|---|
| 字节 | 384478 | 384478 |
| SHA256 | f8caaf8d47ea2993a9ea1d4b8a43e0c4f47246618d327e1a5410428234f6f5cc | 相同 |
| 路径 | 55 | 55 |
| hunk | 280 | 280 |
| 行（含换行计数） | — | 6151 |
| audit/raw/.scratch 路径 | 无 | 无 |
| diff --check | 0 | 0 |

BarServiceTests 行尾：before 243031 字节 / 4565 CRLF / 0 LF；after 244073 字节 / 4576 CRLF / 0 LF。原 CRLF 保留。

55 路径工作区字节与 after tree 全等（SHA256 逐文件），故前端 vitest 跑的是 after 内容，不是第三 tree。

`git ls-files -z`：3491 受控；排除 `docs/audit-2026-09`、`.scratch`、`*-raw.md` 后 3436。AccessGrantPolicy.java / V386 / 新测试在 after tree，不在当前 index（未跟踪纳入 after tree，符合 B3 口径）。

## 4. 三线结论（针对本批 diff）

### 4.1 第一性原理

1. **为何必须存在？** 删掉本 diff，findings 001—018/024—030 的 HTML 上位做不到：BSM-007 日常管理员≠最高权限且 DENY/高危须逐项授予；BSM-028 商户只操作本商户；BSM-084/085 员工注册两路径；BSM-003 配置删除走存档审批软删除；BSM-035 充值须支付成功才入账；BSM-006/HTML585 预存款经手机号+短信验证、系统不生成付款出示码；HTML867 逐笔记录；BEQR 统一原则 210 / HTML1480 预留接口默认关、不伪造。019—023 队列否，本 diff 未按其删除兼容构造器/装配机制。
2. **声称行为是否执行验证？** 前端 6 文件 15 测试 vitest 全过（命令与输出见 §6）。AccessGrantPolicy 上下限/最高权限层级用 javac+java 在 `.scratch` 跑通。OTP 两实现 javac 通过。V386 静态核对。JDBC 回归与后端安全测试因本进程无 `COMPLEX_DATABASE_*`、且禁止 Maven 写 `backend/target`，**未在本席执行**；不臆称全量测试通过。
3. **未验证就当真的前提：** 生产 profile 下 `FailClosedCustomerOtpVerifier` 与 `payment.gateway.enabled` 默认关——由源码 `@Profile("prod")` / 现有网关端口读出，本席未起 Spring 生产上下文。`markPaymentIntentSucceeded` 仍在 FinanceService，但 `settleTopup` 改为 `confirmOnlinePaymentIntent`（须网关启用且 query SUCCEEDED）。

### 4.2 顺从式（假定修复意图正确，对冻结判据）

- GEN-005/001：管理员通配先于 DENY 的旁路删除；日常权限按 `requiresApproval=false` 且 risk∈{low,medium} 自动同步；USB=`usr_master_authority`/`master-authority` 才是最高权限。符合 HTML588。
- GEN-005/BSM-028：`requireSameOrganization` 补商户比较。
- GEN-004/003：非管理员 `requireProjectScope`，空组织不再当全量过滤。
- GEN-004/006：创建改 UUID，去掉 upsert ON CONFLICT 覆盖。
- GEN-005/006：模板加权限走 `requireActorCanGrant`+`requireGrantApprovalIfNeeded`；放宽窗口/范围须复制新模板。
- GEN-006/007：审批指纹 + `LOCK TABLE … SHARE MODE` + pending CAS `FOR UPDATE`。
- GEN-017/PGM-029：受益人/申请人自审拒绝。
- GEN-006/PGM-041：换岗 `replacePositionGrants`，独立 user_roles 保留。
- GEN-002/010：主管级别用职级 rank 与 `governance.supervisor_review.approve` 角色，管理员不再直接 3。
- GEN-017/BSM-084/085：员工手工建号改为注册申请；外部协作仍可建号+密码。
- GEN-015：岗位建议/异常总结改为中文。
- GEN-017/BSM-001：阈值进 `supervisor_review_policies.parameters`，查询不得覆盖。
- GEN-006/011：临时授权消耗日志 `application_id` 可空，来源写入 reason；同事务回滚有 JDBC 测试（本席未跑库）。
- GEN-016/018：平台管理员改模板保留 owner。
- GEN-004/008：账号状态集合校验；凭证非 active 不发可用凭证。
- PGM-059/GEN-010：手工入口停采集手机号，`phoneHash` `@JsonIgnore`，不再 SHA-256 当加密。
- BSM-003：物理 DELETE 改为存档→申请删除→USB 审批软删除+占用检查。
- BSM-035/006/1480/210：线上充值走 createPaymentIntent；RESERVED/NOT_CONFIGURED 前端拒绝且不入账；现场/订单余额支付要手机号+短信；钱包 consume 入口删除；门票 `TicketQrView` 仍在。
- GEN-004/005：未知 UserDetails 不再填 staff/active。
- GEN-009：空密码拒绝。
- GEN-008/012：授权申请分页；其它列表元数据改为实际条数。

027 以 findings 当前拟修 + `questions/M006-027.md` 为准：真实厂商/SMS 未接入；prod OTP fail-closed；未打开 `payment.gateway.enabled`；未改 ReservedPaymentGateway 伪 SUCCEEDED；测试用 FixedCustomerOtpVerifier 限 local/dev/test。用户取消的是储值付款二维码，门票出示二维码未删。

### 4.3 对抗式（假定实现仍错）

自问：管理员能否仍靠通配过 DENY？`permissionGrant` 先 DENY 再 master/daily/customer 桥；effective 集 `wildcard=false` 且先扣 DENY。高危须逐项授予。

自问：能否伪造渠道成功入账？`settleTopup` 在网关关时抛 `PAYMENT_GATEWAY_UNAVAILABLE`；`confirmOnlinePaymentIntent` 拒绝 reserved provider 与非 SUCCEEDED query。WalletPage 对 RESERVED/NOT_CONFIGURED 抛错，要求 https paymentUrl。POS 走 `confirmOfflineTopupAndSettle`（批准的柜台链）。

自问：能否用「已发短信」当扣款？requestCode 默认/prod 抛未配置；扣款另走 `otpVerifier.verify`。Bar/订单 UI 的「验证码已发送」只在 request 成功后出现，prod 不会成功。

自问：能否钱包随意金额扣款？`MemberWalletController` 删除 `/stored-value/consume`；`StoredValueConsumeRequest` 改为订单路径的卡/手机/验证码，金额来自订单。`consumeInternal` 仅内部（酒吧结账在 SMS 之后）。

自问：越界？019—023 否项：AccessUser 7 参构造器仍在；未删凭证接口/BCrypt/HTTP 包装/记录类型。AccessUser 的 JsonIgnore 归 025。文件版本行归该文件主修复 ID（合同要求版本/测试行归属）。无 audit raw 混入。

自问：最小性？新增 AccessGrantPolicy / V386 / 生命周期列服务于 016/006/026/009，不是无 ID 抽象。`LOCK TABLE` 偏粗，但归因 006/008，且有 ponytail 注释表明不提前做逐定义锁。

## 5. 280 hunk 逐块 ID 归因

闸 3：每一增删行归 findings 中队列为「是」的已定稿 ID。验证性测试归对应 ID。下表 280 行均非空且不含 019—023。

| Hunk | 路径 | @@ | +/− | 定稿ID |
|---|---|---|---|---|
| H001 | `apps/web/src/admin/pages/AccessGovernancePage.tsx` | `@@ -1,3 +1,4 @@` | +1/−0 | M006-016, M006-026, M006-030, M006-015 |
| H002 | `apps/web/src/admin/pages/AccessGovernancePage.tsx` | `@@ -11,10 +12,14 @@ import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowD` | +4/−0 | M006-016, M006-026 |
| H003 | `apps/web/src/admin/pages/AccessGovernancePage.tsx` | `@@ -83,11 +88,18 @@ function statusBadge(status?: string | null) {` | +8/−1 | M006-016, M006-026, M006-030 |
| H004 | `apps/web/src/admin/pages/AccessGovernancePage.tsx` | `@@ -120,15 +132,17 @@ export function AccessGovernancePage({ api, setActionResult, setGlobalError, eff` | +6/−4 | M006-026, M006-030, M006-016 |
| H005 | `apps/web/src/admin/pages/AccessGovernancePage.tsx` | `@@ -202,10 +216,47 @@ export function AccessGovernancePage({ api, setActionResult, setGlobalError, eff` | +41/−4 | M006-026, M006-030 |
| H006 | `apps/web/src/admin/pages/AccessGovernancePage.tsx` | `@@ -367,15 +418,23 @@ export function AccessGovernancePage({ api, setActionResult, setGlobalError, eff` | +11/−3 | M006-026 |
| H007 | `apps/web/src/admin/pages/AccessGovernancePage.tsx` | `@@ -414,6 +473,11 @@ export function AccessGovernancePage({ api, setActionResult, setGlobalError, eff` | +5/−0 | M006-030 |
| H008 | `apps/web/src/admin/pages/AccessGovernancePage.tsx` | `@@ -445,6 +509,33 @@ export function AccessGovernancePage({ api, setActionResult, setGlobalError, eff` | +27/−0 | M006-016 |
| H009 | `apps/web/src/admin/pages/AccessUsersPage.test.tsx` | `@@ -1,3 +1,4 @@` | +1/−0 | M006-025 |
| H010 | `apps/web/src/admin/pages/AccessUsersPage.test.tsx` | `@@ -61,7 +62,7 @@ describe("AccessUsersPage 行明细抽屉 + 零英文", () => {` | +1/−1 | M006-025 |
| H011 | `apps/web/src/admin/pages/AccessUsersPage.tsx` | `@@ -1,3 +1,4 @@` | +1/−0 | M006-011, M006-025, M006-029 |
| H012 | `apps/web/src/admin/pages/AccessUsersPage.tsx` | `@@ -26,7 +27,6 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv` | +0/−1 | M006-025 |
| H013 | `apps/web/src/admin/pages/AccessUsersPage.tsx` | `@@ -87,14 +87,10 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv` | +1/−5 | M006-011, M006-025, M006-029 |
| H014 | `apps/web/src/admin/pages/AccessUsersPage.tsx` | `@@ -108,10 +104,21 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv` | +12/−1 | M006-011, M006-025 |
| H015 | `apps/web/src/admin/pages/AccessUsersPage.tsx` | `@@ -128,12 +135,11 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv` | +1/−2 | M006-025 |
| H016 | `apps/web/src/admin/pages/AccessUsersPage.tsx` | `@@ -170,7 +176,6 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv` | +0/−1 | M006-025 |
| H017 | `apps/web/src/admin/pages/AccessUsersPage.tsx` | `@@ -263,9 +268,9 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv` | +2/−2 | M006-011, M006-025 |
| H018 | `apps/web/src/admin/pages/AccessUsersPage.tsx` | `@@ -273,7 +278,6 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv` | +0/−1 | M006-025 |
| H019 | `apps/web/src/admin/pages/AccessUsersPage.tsx` | `@@ -288,7 +292,7 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv` | +1/−1 | M006-011 |
| H020 | `apps/web/src/admin/pages/AccessUsersPage.tsx` | `@@ -296,46 +300,44 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv` | +11/−13 | M006-011, M006-025, M006-029 |
| H021 | `apps/web/src/admin/pages/AccessUsersPage.tsx` | `@@ -345,20 +347,22 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv` | +7/−5 | M006-011, M006-025 |
| H022 | `apps/web/src/admin/pages/BarOrdersPage.tsx` | `@@ -1,3 +1,4 @@` | +1/−0 | M006-027 |
| H023 | `apps/web/src/admin/pages/BarOrdersPage.tsx` | `@@ -140,6 +141,8 @@ export function BarOrdersPage({ api, setActionResult, setGlobalError, effectiveP` | +2/−0 | M006-027 |
| H024 | `apps/web/src/admin/pages/BarOrdersPage.tsx` | `@@ -276,6 +279,10 @@ export function BarOrdersPage({ api, setActionResult, setGlobalError, effectiveP` | +4/−0 | M006-027 |
| H025 | `apps/web/src/admin/pages/BarOrdersPage.tsx` | `@@ -287,6 +294,24 @@ export function BarOrdersPage({ api, setActionResult, setGlobalError, effectiveP` | +18/−0 | M006-027 |
| H026 | `apps/web/src/admin/pages/BarOrdersPage.tsx` | `@@ -295,7 +320,8 @@ export function BarOrdersPage({ api, setActionResult, setGlobalError, effectiveP` | +2/−1 | M006-027 |
| H027 | `apps/web/src/admin/pages/BarOrdersPage.tsx` | `@@ -454,11 +480,18 @@ export function BarOrdersPage({ api, setActionResult, setGlobalError, effectiveP` | +7/−0 | M006-027 |
| H028 | `apps/web/src/admin/pages/M006AccessGovernance.test.tsx` | `@@ -0,0 +1,59 @@` | +59/−0 | M006-016, M006-026, M006-030 |
| H029 | `apps/web/src/admin/pages/M006AccessRegistration.test.tsx` | `@@ -0,0 +1,49 @@` | +49/−0 | M006-011, M006-025, M006-029 |
| H030 | `apps/web/src/admin/pages/M006BarStoredValue.test.tsx` | `@@ -0,0 +1,34 @@` | +34/−0 | M006-027 |
| H031 | `apps/web/src/admin/pages/StaffProfilesPage.tsx` | `@@ -1,3 +1,4 @@` | +1/−0 | M006-011, M006-029 |
| H032 | `apps/web/src/admin/pages/StaffProfilesPage.tsx` | `@@ -32,6 +33,7 @@ const MANAGE = ["staff.profile.review"];` | +1/−0 | M006-011, M006-029 |
| H033 | `apps/web/src/admin/pages/StaffProfilesPage.tsx` | `@@ -43,6 +45,25 @@ export function StaffProfilesPage({ api, setActionResult, setGlobalError, effect` | +19/−0 | M006-011, M006-029 |
| H034 | `apps/web/src/admin/pages/StaffProfilesPage.tsx` | `@@ -75,6 +96,9 @@ export function StaffProfilesPage({ api, setActionResult, setGlobalError, effect` | +3/−0 | M006-011, M006-029 |
| H035 | `apps/web/src/admin/pages/StaffProfilesPage.tsx` | `@@ -103,6 +127,10 @@ export function StaffProfilesPage({ api, setActionResult, setGlobalError, effect` | +4/−0 | M006-011, M006-029 |
| H036 | `apps/web/src/customer/client/customerService.ts` | `@@ -1,3 +1,4 @@` | +1/−0 | M006-027 |
| H037 | `apps/web/src/customer/client/customerService.ts` | `@@ -126,6 +127,14 @@ export class CustomerService {` | +8/−0 | M006-027 |
| H038 | `apps/web/src/customer/client/customerService.ts` | `@@ -136,14 +145,18 @@ export class CustomerService {` | +9/−5 | M006-027 |
| H039 | `apps/web/src/customer/pages/M006OrderStoredValue.test.tsx` | `@@ -0,0 +1,28 @@` | +28/−0 | M006-027 |
| H040 | `apps/web/src/customer/pages/MyOrdersPage.tsx` | `@@ -1,9 +1,10 @@` | +3/−2 | M006-027 |
| H041 | `apps/web/src/customer/pages/MyOrdersPage.tsx` | `@@ -19,6 +20,64 @@ export function MyOrdersPage({ service }: { service: CustomerService }): React.R` | +58/−0 | M006-027 |
| H042 | `apps/web/src/customer/pages/MyOrdersPage.tsx` | `@@ -116,6 +175,25 @@ export function MyOrdersPage({ service }: { service: CustomerService }): React.R` | +19/−0 | M006-027 |
| H043 | `apps/web/src/customer/pages/MyOrdersPage.tsx` | `@@ -141,6 +219,9 @@ export function MyOrdersPage({ service }: { service: CustomerService }): React.R` | +3/−0 | M006-027 |
| H044 | `apps/web/src/customer/pages/WalletPage.test.tsx` | `@@ -1,4 +1,5 @@` | +2/−1 | M006-027 |
| H045 | `apps/web/src/customer/pages/WalletPage.test.tsx` | `@@ -31,13 +32,13 @@ const merchantCard = {` | +2/−2 | M006-027 |
| H046 | `apps/web/src/customer/pages/WalletPage.test.tsx` | `@@ -49,16 +50,57 @@ describe("WalletPage", () => {` | +44/−3 | M006-027 |
| H047 | `apps/web/src/customer/pages/WalletPage.tsx` | `@@ -1,11 +1,21 @@` | +11/−1 | M006-027 |
| H048 | `apps/web/src/customer/pages/WalletPage.tsx` | `@@ -22,7 +32,7 @@ function yuan(cent: number): string {` | +1/−1 | M006-027 |
| H049 | `apps/web/src/customer/pages/WalletPage.tsx` | `@@ -30,6 +40,26 @@ export function WalletPage({ service }: { service: CustomerService }): React.Rea` | +20/−0 | M006-027 |
| H050 | `apps/web/src/customer/pages/WalletPage.tsx` | `@@ -63,35 +93,57 @@ export function WalletPage({ service }: { service: CustomerService }): React.Rea` | +45/−23 | M006-027 |
| H051 | `apps/web/src/customer/pages/WalletPage.tsx` | `@@ -138,7 +190,8 @@ export function WalletPage({ service }: { service: CustomerService }): React.Rea` | +2/−1 | M006-027 |
| H052 | `apps/web/src/customer/pages/WalletPage.tsx` | `@@ -147,10 +200,13 @@ export function WalletPage({ service }: { service: CustomerService }): React.Rea` | +4/−1 | M006-027 |
| H053 | `apps/web/src/customer/pages/WalletPage.tsx` | `@@ -166,16 +222,30 @@ export function WalletPage({ service }: { service: CustomerService }): React.Rea` | +18/−4 | M006-027 |
| H054 | `backend/src/main/java/com/complex/platform/access/AccessController.java` | `@@ -1,4 +1,5 @@` | +1/−0 | M006-014, M006-016, M006-026, M006-030 |
| H055 | `backend/src/main/java/com/complex/platform/access/AccessController.java` | `@@ -114,6 +115,19 @@ public class AccessController {` | +13/−0 | M006-016 |
| H056 | `backend/src/main/java/com/complex/platform/access/AccessController.java` | `@@ -123,10 +137,11 @@ public class AccessController {` | +3/−2 | M006-030 |
| H057 | `backend/src/main/java/com/complex/platform/access/AccessController.java` | `@@ -134,8 +149,7 @@ public class AccessController {` | +1/−2 | M006-014 |
| H058 | `backend/src/main/java/com/complex/platform/access/AccessController.java` | `@@ -151,9 +165,10 @@ public class AccessController {` | +3/−2 | M006-026 |
| H059 | `backend/src/main/java/com/complex/platform/access/AccessController.java` | `@@ -173,6 +188,18 @@ public class AccessController {` | +12/−0 | M006-026 |
| H060 | `backend/src/main/java/com/complex/platform/access/AccessGrantPolicy.java` | `@@ -0,0 +1,44 @@` | +44/−0 | M006-016 |
| H061 | `backend/src/main/java/com/complex/platform/access/AccessRepository.java` | `@@ -1,11 +1,17 @@` | +6/−0 | M006-006, M006-008, M006-004, M006-009, M006-026 |
| H062 | `backend/src/main/java/com/complex/platform/access/AccessRepository.java` | `@@ -25,6 +31,13 @@ public interface AccessRepository {` | +7/−0 | M006-004, M006-006, M006-008, M006-009, M006-026 |
| H063 | `backend/src/main/java/com/complex/platform/access/AccessRepository.java` | `@@ -112,6 +125,14 @@ public interface AccessRepository {` | +8/−0 | M006-026, M006-004, M006-006, M006-008, M006-009 |
| H064 | `backend/src/main/java/com/complex/platform/access/AccessRepository.java` | `@@ -124,6 +145,12 @@ public interface AccessRepository {` | +6/−0 | M006-009, M006-004, M006-006, M006-008, M006-026 |
| H065 | `backend/src/main/java/com/complex/platform/access/AccessRepository.java` | `@@ -149,6 +176,30 @@ public interface AccessRepository {` | +24/−0 | M006-006, M006-008, M006-004, M006-009, M006-026 |
| H066 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -1,5 +1,7 @@` | +2/−0 | M006-001, M006-002, M006-003, M006-004, M006-011, M006-016, M006-025 |
| H067 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -19,15 +21,11 @@ import com.complex.platform.evidence.AuditEventService;` | +0/−4 | M006-025 |
| H068 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -38,7 +36,6 @@ import java.util.Set;` | +0/−1 | M006-016 |
| H069 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -48,7 +45,6 @@ import static com.complex.platform.common.Strings.requireText;` | +0/−1 | M006-030 |
| H070 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -62,8 +58,6 @@ public class AccessService {` | +0/−2 | M006-016 |
| H071 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -72,18 +66,16 @@ public class AccessService {` | +3/−5 | M006-016, M006-029 |
| H072 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -101,16 +93,14 @@ public class AccessService {` | +2/−4 | M006-003, M006-016, M006-029 |
| H073 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -126,7 +116,7 @@ public class AccessService {` | +1/−1 | M006-003 |
| H074 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -135,8 +125,8 @@ public class AccessService {` | +2/−2 | M006-004 |
| H075 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -150,19 +140,35 @@ public class AccessService {` | +27/−11 | M006-005, M006-018 |
| H076 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -184,8 +190,8 @@ public class AccessService {` | +2/−2 | M006-003, M006-004 |
| H077 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -198,16 +204,11 @@ public class AccessService {` | +5/−10 | M006-002, M006-013 |
| H078 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -282,7 +283,7 @@ public class AccessService {` | +1/−1 | M006-003 |
| H079 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -298,7 +299,7 @@ public class AccessService {` | +1/−1 | M006-003 |
| H080 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -309,7 +310,7 @@ public class AccessService {` | +1/−1 | M006-004 |
| H081 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -397,9 +398,14 @@ public class AccessService {` | +6/−1 | M006-003, M006-026 |
| H082 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -420,7 +426,7 @@ public class AccessService {` | +1/−1 | M006-004 |
| H083 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -464,21 +470,71 @@ public class AccessService {` | +65/−15 | M006-026 |
| H084 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -508,7 +564,7 @@ public class AccessService {` | +1/−1 | M006-001 |
| H085 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -556,17 +612,9 @@ public class AccessService {` | +3/−11 | M006-002 |
| H086 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -604,6 +652,7 @@ public class AccessService {` | +1/−0 | M006-006, M006-008 |
| H087 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -622,6 +671,7 @@ public class AccessService {` | +1/−0 | M006-006 |
| H088 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -658,11 +708,23 @@ public class AccessService {` | +12/−0 | M006-006, M006-007, M006-008 |
| H089 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -710,9 +772,24 @@ public class AccessService {` | +17/−2 | M006-016, M006-030 |
| H090 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -727,9 +804,14 @@ public class AccessService {` | +8/−3 | M006-016 |
| H091 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -761,7 +843,7 @@ public class AccessService {` | +1/−1 | M006-015 |
| H092 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -770,7 +852,7 @@ public class AccessService {` | +1/−1 | M006-015 |
| H093 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -812,7 +894,7 @@ public class AccessService {` | +1/−1 | M006-003 |
| H094 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -842,8 +924,7 @@ public class AccessService {` | +1/−2 | M006-003 |
| H095 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -860,16 +941,25 @@ public class AccessService {` | +12/−3 | M006-011, M006-004, M006-025, M006-029 |
| H096 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -908,18 +998,12 @@ public class AccessService {` | +5/−11 | M006-024, M006-025 |
| H097 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -966,11 +1050,7 @@ public class AccessService {` | +1/−5 | M006-002 |
| H098 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -1069,7 +1149,6 @@ public class AccessService {` | +0/−1 | M006-009 |
| H099 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -1078,6 +1157,8 @@ public class AccessService {` | +2/−0 | M006-009 |
| H100 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -1107,17 +1188,13 @@ public class AccessService {` | +7/−11 | M006-002, M006-013 |
| H101 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -1128,7 +1205,7 @@ public class AccessService {` | +1/−1 | M006-013 |
| H102 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -1171,7 +1248,7 @@ public class AccessService {` | +1/−1 | M006-004 |
| H103 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -1211,7 +1288,7 @@ public class AccessService {` | +1/−1 | M006-004 |
| H104 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -1251,13 +1328,16 @@ public class AccessService {` | +4/−1 | M006-004, M006-013 |
| H105 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -1284,6 +1364,50 @@ public class AccessService {` | +44/−0 | M006-006 |
| H106 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -1376,6 +1500,7 @@ public class AccessService {` | +1/−0 | M006-006, M006-009 |
| H107 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -1404,19 +1529,48 @@ public class AccessService {` | +41/−12 | M006-016 |
| H108 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -1429,14 +1583,6 @@ public class AccessService {` | +0/−8 | M006-016 |
| H109 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -1571,6 +1717,9 @@ public class AccessService {` | +3/−0 | M006-026 |
| H110 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -1616,10 +1765,13 @@ public class AccessService {` | +4/−1 | M006-002, M006-003 |
| H111 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -1731,7 +1883,7 @@ public class AccessService {` | +1/−1 | M006-030 |
| H112 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | `@@ -1765,6 +1917,7 @@ public class AccessService {` | +1/−0 | M006-009 |
| H113 | `backend/src/main/java/com/complex/platform/access/AccessUser.java` | `@@ -1,5 +1,7 @@` | +2/−0 | M006-025 |
| H114 | `backend/src/main/java/com/complex/platform/access/AccessUser.java` | `@@ -11,7 +13,7 @@ public record AccessUser(` | +1/−1 | M006-025 |
| H115 | `backend/src/main/java/com/complex/platform/access/AuthorizationService.java` | `@@ -1,5 +1,7 @@` | +2/−0 | M006-001, M006-010, M006-012, M006-014, M006-028 |
| H116 | `backend/src/main/java/com/complex/platform/access/AuthorizationService.java` | `@@ -11,8 +13,9 @@ import java.util.Optional;` | +2/−1 | M006-001, M006-010, M006-012, M006-014, M006-028 |
| H117 | `backend/src/main/java/com/complex/platform/access/AuthorizationService.java` | `@@ -30,47 +33,77 @@ public class AuthorizationService implements CallerScopeResolver {` | +60/−30 | M006-014, M006-001, M006-010, M006-012, M006-028 |
| H118 | `backend/src/main/java/com/complex/platform/access/AuthorizationService.java` | `@@ -84,20 +117,23 @@ public class AuthorizationService implements CallerScopeResolver {` | +15/−12 | M006-012, M006-003, M006-001, M006-010, M006-014, M006-028 |
| H119 | `backend/src/main/java/com/complex/platform/access/AuthorizationService.java` | `@@ -116,12 +152,20 @@ public class AuthorizationService implements CallerScopeResolver {` | +9/−1 | M006-001, M006-010, M006-012, M006-014, M006-028 |
| H120 | `backend/src/main/java/com/complex/platform/access/AuthorizationService.java` | `@@ -143,10 +187,7 @@ public class AuthorizationService implements CallerScopeResolver {` | +1/−4 | M006-028, M006-001, M006-010, M006-012, M006-014 |
| H121 | `backend/src/main/java/com/complex/platform/access/AuthorizationService.java` | `@@ -188,23 +229,13 @@ public class AuthorizationService implements CallerScopeResolver {` | +5/−15 | M006-001, M006-010, M006-012, M006-014, M006-028 |
| H122 | `backend/src/main/java/com/complex/platform/access/AuthorizationService.java` | `@@ -237,24 +268,18 @@ public class AuthorizationService implements CallerScopeResolver {` | +7/−13 | M006-001, M006-010, M006-012, M006-014, M006-028 |
| H123 | `backend/src/main/java/com/complex/platform/access/AuthorizationService.java` | `@@ -263,7 +288,8 @@ public class AuthorizationService implements CallerScopeResolver {` | +2/−1 | M006-001, M006-010, M006-012, M006-014, M006-028 |
| H124 | `backend/src/main/java/com/complex/platform/access/AuthorizationService.java` | `@@ -297,7 +323,7 @@ public class AuthorizationService implements CallerScopeResolver {` | +1/−1 | M006-001, M006-010, M006-012, M006-014, M006-028 |
| H125 | `backend/src/main/java/com/complex/platform/access/AuthorizationService.java` | `@@ -315,7 +341,8 @@ public class AuthorizationService implements CallerScopeResolver {` | +2/−1 | M006-001, M006-010, M006-012, M006-014, M006-028 |
| H126 | `backend/src/main/java/com/complex/platform/access/AuthorizationService.java` | `@@ -437,8 +464,7 @@ public class AuthorizationService implements CallerScopeResolver {` | +1/−2 | M006-012, M006-003, M006-001, M006-010, M006-014, M006-028 |
| H127 | `backend/src/main/java/com/complex/platform/access/DevAccessCredentialProvisioner.java` | `@@ -1,6 +1,9 @@` | +3/−0 | M006-024, M006-029 |
| H128 | `backend/src/main/java/com/complex/platform/access/DevAccessCredentialProvisioner.java` | `@@ -25,9 +28,14 @@ class DevAccessCredentialProvisioner implements AccessCredentialProvisioner {` | +7/−2 | M006-024, M006-029 |
| H129 | `backend/src/main/java/com/complex/platform/access/DevAccessCredentialProvisioner.java` | `@@ -35,6 +43,6 @@ class DevAccessCredentialProvisioner implements AccessCredentialProvisioner {` | +1/−1 | M006-024, M006-029 |
| H130 | `backend/src/main/java/com/complex/platform/access/JdbcAccessCredentialProvisioner.java` | `@@ -1,7 +1,10 @@` | +3/−0 | M006-024, M006-029 |
| H131 | `backend/src/main/java/com/complex/platform/access/JdbcAccessCredentialProvisioner.java` | `@@ -28,10 +31,13 @@ class JdbcAccessCredentialProvisioner implements AccessCredentialProvisioner {` | +6/−3 | M006-024, M006-029 |
| H132 | `backend/src/main/java/com/complex/platform/access/JdbcAccessCredentialProvisioner.java` | `@@ -52,6 +58,9 @@ class JdbcAccessCredentialProvisioner implements AccessCredentialProvisioner {` | +3/−0 | M006-024, M006-029 |
| H133 | `backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java` | `@@ -1,4 +1,5 @@` | +1/−0 | M006-004, M006-006, M006-008, M006-009, M006-026 |
| H134 | `backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java` | `@@ -35,6 +36,15 @@ class JdbcAccessRepository implements AccessRepository {` | +9/−0 | M006-006, M006-008, M006-004, M006-009, M006-026 |
| H135 | `backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java` | `@@ -92,41 +102,35 @@ class JdbcAccessRepository implements AccessRepository {` | +20/−26 | M006-004, M006-006, M006-008, M006-009, M006-026 |
| H136 | `backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java` | `@@ -142,7 +146,7 @@ class JdbcAccessRepository implements AccessRepository {` | +1/−1 | M006-004, M006-006, M006-008, M006-009, M006-026 |
| H137 | `backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java` | `@@ -166,17 +170,6 @@ class JdbcAccessRepository implements AccessRepository {` | +0/−11 | M006-004, M006-006, M006-008, M006-009, M006-026 |
| H138 | `backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java` | `@@ -365,12 +358,6 @@ class JdbcAccessRepository implements AccessRepository {` | +0/−6 | M006-004, M006-006, M006-008, M006-009, M006-026 |
| H139 | `backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java` | `@@ -431,14 +418,14 @@ class JdbcAccessRepository implements AccessRepository {` | +2/−2 | M006-004, M006-006, M006-008, M006-009, M006-026 |
| H140 | `backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java` | `@@ -449,9 +436,9 @@ class JdbcAccessRepository implements AccessRepository {` | +2/−2 | M006-004, M006-006, M006-008, M006-009, M006-026 |
| H141 | `backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java` | `@@ -468,9 +455,9 @@ class JdbcAccessRepository implements AccessRepository {` | +2/−2 | M006-004, M006-006, M006-008, M006-009, M006-026 |
| H142 | `backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java` | `@@ -478,7 +465,7 @@ class JdbcAccessRepository implements AccessRepository {` | +1/−1 | M006-004, M006-006, M006-008, M006-009, M006-026 |
| H143 | `backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java` | `@@ -518,10 +505,31 @@ class JdbcAccessRepository implements AccessRepository {` | +24/−3 | M006-026, M006-004, M006-006, M006-008, M006-009 |
| H144 | `backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java` | `@@ -673,6 +681,15 @@ class JdbcAccessRepository implements AccessRepository {` | +9/−0 | M006-004, M006-006, M006-008, M006-009, M006-026 |
| H145 | `backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java` | `@@ -688,28 +705,6 @@ class JdbcAccessRepository implements AccessRepository {` | +0/−22 | M006-004, M006-006, M006-008, M006-009, M006-026 |
| H146 | `backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java` | `@@ -724,10 +719,69 @@ class JdbcAccessRepository implements AccessRepository {` | +59/−0 | M006-006, M006-008, M006-004, M006-009, M006-026 |
| H147 | `backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java` | `@@ -754,6 +808,31 @@ class JdbcAccessRepository implements AccessRepository {` | +25/−0 | M006-009, M006-004, M006-006, M006-008, M006-026 |
| H148 | `backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java` | `@@ -766,6 +845,15 @@ class JdbcAccessRepository implements AccessRepository {` | +9/−0 | M006-004, M006-006, M006-008, M006-009, M006-026 |
| H149 | `backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java` | `@@ -799,6 +887,7 @@ class JdbcAccessRepository implements AccessRepository {` | +1/−0 | M006-004, M006-006, M006-008, M006-009, M006-026 |
| H150 | `backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java` | `@@ -814,31 +903,39 @@ class JdbcAccessRepository implements AccessRepository {` | +16/−8 | M006-004, M006-006, M006-008, M006-009, M006-026 |
| H151 | `backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java` | `@@ -1065,7 +1162,9 @@ class JdbcAccessRepository implements AccessRepository {` | +3/−1 | M006-004, M006-006, M006-008, M006-009, M006-026 |
| H152 | `backend/src/main/java/com/complex/platform/access/PositionRoleTemplate.java` | `@@ -1,5 +1,7 @@` | +2/−0 | M006-026 |
| H153 | `backend/src/main/java/com/complex/platform/access/PositionRoleTemplate.java` | `@@ -12,6 +14,21 @@ public record PositionRoleTemplate(` | +16/−1 | M006-026 |
| H154 | `backend/src/main/java/com/complex/platform/access/dto/AccessUserRequest.java` | `@@ -1,5 +1,7 @@` | +2/−0 | M006-025 |
| H155 | `backend/src/main/java/com/complex/platform/access/dto/AccessUserRequest.java` | `@@ -9,7 +11,7 @@ public record AccessUserRequest(` | +1/−1 | M006-025 |
| H156 | `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -1,5 +1,7 @@` | +2/−0 | M006-011, M006-002, M006-003, M006-029 |
| H157 | `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -75,7 +77,7 @@ public class StaffService {` | +1/−1 | M006-011, M006-002, M006-003, M006-029 |
| H158 | `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -105,23 +107,15 @@ public class StaffService {` | +3/−11 | M006-011, M006-002, M006-003, M006-029 |
| H159 | `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -143,7 +137,7 @@ public class StaffService {` | +1/−1 | M006-011, M006-002, M006-003, M006-029 |
| H160 | `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -175,6 +169,7 @@ public class StaffService {` | +1/−0 | M006-011, M006-002, M006-003, M006-029 |
| H161 | `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -189,8 +184,14 @@ public class StaffService {` | +6/−0 | M006-011, M006-002, M006-003, M006-029 |
| H162 | `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -200,7 +201,7 @@ public class StaffService {` | +1/−1 | M006-011, M006-002, M006-003, M006-029 |
| H163 | `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -262,7 +263,14 @@ public class StaffService {` | +8/−1 | M006-011, M006-002, M006-003, M006-029 |
| H164 | `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -903,14 +911,21 @@ public class StaffService {` | +12/−5 | M006-011, M006-002, M006-003, M006-029 |
| H165 | `backend/src/main/java/com/complex/platform/bar/BarBillSettleRequest.java` | `@@ -1,5 +1,7 @@` | +2/−0 | M006-027 |
| H166 | `backend/src/main/java/com/complex/platform/bar/BarBillSettleRequest.java` | `@@ -10,8 +12,17 @@ public record BarBillSettleRequest(` | +10/−1 | M006-027 |
| H167 | `backend/src/main/java/com/complex/platform/bar/BarBillingService.java` | `@@ -1,5 +1,7 @@` | +2/−0 | M006-027 |
| H168 | `backend/src/main/java/com/complex/platform/bar/BarBillingService.java` | `@@ -35,6 +37,8 @@ public class BarBillingService {` | +2/−0 | M006-027 |
| H169 | `backend/src/main/java/com/complex/platform/bar/BarBillingService.java` | `@@ -148,6 +152,19 @@ public class BarBillingService {` | +13/−0 | M006-027 |
| H170 | `backend/src/main/java/com/complex/platform/bar/BarBillingService.java` | `@@ -226,11 +243,11 @@ public class BarBillingService {` | +4/−4 | M006-027 |
| H171 | `backend/src/main/java/com/complex/platform/bar/BarController.java` | `@@ -1,4 +1,6 @@` | +2/−0 | M006-027 |
| H172 | `backend/src/main/java/com/complex/platform/bar/BarController.java` | `@@ -138,6 +140,15 @@ public class BarController {` | +9/−0 | M006-027 |
| H173 | `backend/src/main/java/com/complex/platform/bar/BarService.java` | `@@ -1,5 +1,7 @@` | +2/−0 | M006-027 |
| H174 | `backend/src/main/java/com/complex/platform/bar/BarService.java` | `@@ -625,6 +627,11 @@ public class BarService {` | +5/−0 | M006-027 |
| H175 | `backend/src/main/java/com/complex/platform/customer/CustomerOtpVerifier.java` | `@@ -1,5 +1,12 @@` | +7/−0 | M006-027 |
| H176 | `backend/src/main/java/com/complex/platform/customer/CustomerRepository.java` | `@@ -1,5 +1,7 @@` | +2/−0 | M006-027 |
| H177 | `backend/src/main/java/com/complex/platform/customer/CustomerRepository.java` | `@@ -223,6 +225,11 @@ public interface CustomerRepository {` | +5/−0 | M006-027 |
| H178 | `backend/src/main/java/com/complex/platform/customer/CustomerService.java` | `@@ -1,5 +1,7 @@` | +2/−0 | M006-027 |
| H179 | `backend/src/main/java/com/complex/platform/customer/CustomerService.java` | `@@ -143,6 +145,33 @@ public class CustomerService {` | +27/−0 | M006-027 |
| H180 | `backend/src/main/java/com/complex/platform/customer/JdbcCustomerRepository.java` | `@@ -1,4 +1,6 @@` | +2/−0 | M006-027 |
| H181 | `backend/src/main/java/com/complex/platform/customer/JdbcCustomerRepository.java` | `@@ -1201,6 +1203,19 @@ class JdbcCustomerRepository implements CustomerRepository {` | +13/−0 | M006-027 |
| H182 | `backend/src/main/java/com/complex/platform/customer/MemberWalletController.java` | `@@ -1,8 +1,9 @@` | +2/−1 | M006-027 |
| H183 | `backend/src/main/java/com/complex/platform/customer/MemberWalletController.java` | `@@ -53,6 +54,27 @@ public class MemberWalletController {` | +21/−0 | M006-027 |
| H184 | `backend/src/main/java/com/complex/platform/customer/MemberWalletController.java` | `@@ -74,17 +96,6 @@ public class MemberWalletController {` | +0/−11 | M006-027 |
| H185 | `backend/src/main/java/com/complex/platform/customer/MemberWalletController.java` | `@@ -97,6 +108,25 @@ public class MemberWalletController {` | +19/−0 | M006-027 |
| H186 | `backend/src/main/java/com/complex/platform/customer/StoredValueService.java` | `@@ -1,5 +1,7 @@` | +2/−0 | M006-027 |
| H187 | `backend/src/main/java/com/complex/platform/customer/StoredValueService.java` | `@@ -33,7 +35,7 @@ import com.complex.platform.common.Strings;` | +1/−1 | M006-027 |
| H188 | `backend/src/main/java/com/complex/platform/customer/StoredValueService.java` | `@@ -63,7 +65,7 @@ public class StoredValueService {` | +1/−1 | M006-027 |
| H189 | `backend/src/main/java/com/complex/platform/customer/StoredValueService.java` | `@@ -73,6 +75,10 @@ public class StoredValueService {` | +4/−0 | M006-027 |
| H190 | `backend/src/main/java/com/complex/platform/customer/StoredValueService.java` | `@@ -109,8 +115,8 @@ public class StoredValueService {` | +2/−2 | M006-027 |
| H191 | `backend/src/main/java/com/complex/platform/customer/StoredValueService.java` | `@@ -158,6 +164,22 @@ public class StoredValueService {` | +16/−0 | M006-027 |
| H192 | `backend/src/main/java/com/complex/platform/customer/StoredValueService.java` | `@@ -182,7 +204,7 @@ public class StoredValueService {` | +1/−1 | M006-027 |
| H193 | `backend/src/main/java/com/complex/platform/customer/StoredValueService.java` | `@@ -240,10 +262,51 @@ public class StoredValueService {` | +45/−4 | M006-027 |
| H194 | `backend/src/main/java/com/complex/platform/customer/StoredValueService.java` | `@@ -254,6 +317,11 @@ public class StoredValueService {` | +5/−0 | M006-027 |
| H195 | `backend/src/main/java/com/complex/platform/customer/StoredValueService.java` | `@@ -273,7 +341,7 @@ public class StoredValueService {` | +1/−1 | M006-027 |
| H196 | `backend/src/main/java/com/complex/platform/customer/dto/StoredValueConsumeRequest.java` | `@@ -1,15 +1,14 @@` | +6/−7 | M006-027 |
| H197 | `backend/src/main/java/com/complex/platform/finance/FinanceRepository.java` | `@@ -1,5 +1,7 @@` | +2/−0 | M006-027 |
| H198 | `backend/src/main/java/com/complex/platform/finance/FinanceRepository.java` | `@@ -256,6 +258,10 @@ public interface FinanceRepository {` | +4/−0 | M006-027 |
| H199 | `backend/src/main/java/com/complex/platform/finance/FinanceService.java` | `@@ -1,5 +1,7 @@` | +2/−0 | M006-027 |
| H200 | `backend/src/main/java/com/complex/platform/finance/FinanceService.java` | `@@ -351,6 +353,55 @@ public class FinanceService implements TerminationSettlementPort {` | +49/−0 | M006-027 |
| H201 | `backend/src/main/java/com/complex/platform/finance/FinanceService.java` | `@@ -1286,7 +1337,8 @@ public class FinanceService implements TerminationSettlementPort {` | +2/−1 | M006-027 |
| H202 | `backend/src/main/java/com/complex/platform/finance/JdbcFinanceRepository.java` | `@@ -1,4 +1,6 @@` | +2/−0 | M006-027 |
| H203 | `backend/src/main/java/com/complex/platform/finance/JdbcFinanceRepository.java` | `@@ -371,6 +373,15 @@ class JdbcFinanceRepository implements FinanceRepository {` | +9/−0 | M006-027 |
| H204 | `backend/src/main/java/com/complex/platform/finance/PaymentIntentRecord.java` | `@@ -1,5 +1,7 @@` | +2/−0 | M006-027 |
| H205 | `backend/src/main/java/com/complex/platform/finance/PaymentIntentRecord.java` | `@@ -20,8 +22,17 @@ public record PaymentIntentRecord(` | +10/−1 | M006-027 |
| H206 | `backend/src/main/java/com/complex/platform/finance/gateway/PaymentGatewayResult.java` | `@@ -1,5 +1,7 @@` | +2/−0 | M006-027 |
| H207 | `backend/src/main/java/com/complex/platform/finance/gateway/PaymentGatewayResult.java` | `@@ -13,9 +15,15 @@ public record PaymentGatewayResult(` | +7/−1 | M006-027 |
| H208 | `backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java` | `@@ -1,5 +1,7 @@` | +2/−0 | M006-001, M006-007, M006-010, M006-026 |
| H209 | `backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java` | `@@ -146,6 +148,10 @@ public class SupervisorReviewService {` | +4/−0 | M006-001, M006-007, M006-010, M006-026 |
| H210 | `backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java` | `@@ -159,7 +165,7 @@ public class SupervisorReviewService {` | +1/−1 | M006-001, M006-007, M006-010, M006-026 |
| H211 | `backend/src/main/java/com/complex/platform/ticketing/JdbcTicketingRepository.java` | `@@ -1,5 +1,7 @@` | +2/−0 | M006-027 |
| H212 | `backend/src/main/java/com/complex/platform/ticketing/JdbcTicketingRepository.java` | `@@ -930,6 +932,12 @@ class JdbcTicketingRepository implements TicketingRepository {` | +6/−0 | M006-027 |
| H213 | `backend/src/main/java/com/complex/platform/ticketing/TicketingRepository.java` | `@@ -1,5 +1,7 @@` | +2/−0 | M006-027 |
| H214 | `backend/src/main/java/com/complex/platform/ticketing/TicketingRepository.java` | `@@ -299,6 +301,10 @@ public interface TicketingRepository {` | +4/−0 | M006-027 |
| H215 | `backend/src/main/resources/db/migration/V386__access_grant_authorization_fingerprint.sql` | `@@ -0,0 +1,29 @@` | +29/−0 | M006-006, M006-009, M006-016, M006-026 |
| H216 | `backend/src/test/java/com/complex/platform/access/AccessServiceTests.java` | `@@ -1,5 +1,7 @@` | +2/−0 | M006-004, M006-005, M006-009, M006-011, M006-016, M006-025 |
| H217 | `backend/src/test/java/com/complex/platform/access/AccessServiceTests.java` | `@@ -58,6 +60,14 @@ class AccessServiceTests {` | +8/−0 | M006-004, M006-005, M006-009, M006-011, M006-016, M006-025 |
| H218 | `backend/src/test/java/com/complex/platform/access/AccessServiceTests.java` | `@@ -70,7 +80,7 @@ class AccessServiceTests {` | +1/−1 | M006-004, M006-005, M006-009, M006-011, M006-016, M006-025 |
| H219 | `backend/src/test/java/com/complex/platform/access/AccessServiceTests.java` | `@@ -84,27 +94,27 @@ class AccessServiceTests {` | +5/−5 | M006-004, M006-005, M006-009, M006-011, M006-016, M006-025 |
| H220 | `backend/src/test/java/com/complex/platform/access/AccessServiceTests.java` | `@@ -157,7 +167,9 @@ class AccessServiceTests {` | +3/−1 | M006-004, M006-005, M006-009, M006-011, M006-016, M006-025 |
| H221 | `backend/src/test/java/com/complex/platform/access/AccessServiceTests.java` | `@@ -171,11 +183,11 @@ class AccessServiceTests {` | +2/−2 | M006-004, M006-005, M006-009, M006-011, M006-016, M006-025 |
| H222 | `backend/src/test/java/com/complex/platform/access/AccessServiceTests.java` | `@@ -194,6 +206,7 @@ class AccessServiceTests {` | +1/−0 | M006-004, M006-005, M006-009, M006-011, M006-016, M006-025 |
| H223 | `backend/src/test/java/com/complex/platform/access/AccessServiceTests.java` | `@@ -242,13 +255,13 @@ class AccessServiceTests {` | +2/−2 | M006-004, M006-005, M006-009, M006-011, M006-016, M006-025 |
| H224 | `backend/src/test/java/com/complex/platform/access/AccessServiceTests.java` | `@@ -275,9 +288,9 @@ class AccessServiceTests {` | +2/−2 | M006-004, M006-005, M006-009, M006-011, M006-016, M006-025 |
| H225 | `backend/src/test/java/com/complex/platform/access/AccessServiceTests.java` | `@@ -313,14 +326,14 @@ class AccessServiceTests {` | +2/−2 | M006-004, M006-005, M006-009, M006-011, M006-016, M006-025 |
| H226 | `backend/src/test/java/com/complex/platform/access/AccessServiceTests.java` | `@@ -361,8 +374,7 @@ class AccessServiceTests {` | +1/−2 | M006-004, M006-005, M006-009, M006-011, M006-016, M006-025 |
| H227 | `backend/src/test/java/com/complex/platform/access/AccessServiceTests.java` | `@@ -376,8 +388,7 @@ class AccessServiceTests {` | +1/−2 | M006-004, M006-005, M006-009, M006-011, M006-016, M006-025 |
| H228 | `backend/src/test/java/com/complex/platform/access/AccessServiceTests.java` | `@@ -388,8 +399,7 @@ class AccessServiceTests {` | +1/−2 | M006-004, M006-005, M006-009, M006-011, M006-016, M006-025 |
| H229 | `backend/src/test/java/com/complex/platform/access/AccessServiceTests.java` | `@@ -477,7 +487,8 @@ class AccessServiceTests {` | +2/−1 | M006-004, M006-005, M006-009, M006-011, M006-016, M006-025 |
| H230 | `backend/src/test/java/com/complex/platform/access/AccessServiceTests.java` | `@@ -569,6 +580,22 @@ class AccessServiceTests {` | +16/−0 | M006-004, M006-005, M006-009, M006-011, M006-016, M006-025 |
| H231 | `backend/src/test/java/com/complex/platform/access/AccessServiceTests.java` | `@@ -715,7 +742,8 @@ class AccessServiceTests {` | +2/−1 | M006-004, M006-005, M006-009, M006-011, M006-016, M006-025 |
| H232 | `backend/src/test/java/com/complex/platform/access/AccessServiceTests.java` | `@@ -727,13 +755,17 @@ class AccessServiceTests {` | +5/−1 | M006-004, M006-005, M006-009, M006-011, M006-016, M006-025 |
| H233 | `backend/src/test/java/com/complex/platform/access/AccessServiceTests.java` | `@@ -754,12 +786,12 @@ class AccessServiceTests {` | +2/−2 | M006-004, M006-005, M006-009, M006-011, M006-016, M006-025 |
| H234 | `backend/src/test/java/com/complex/platform/access/AccessServiceTests.java` | `@@ -859,7 +891,7 @@ class AccessServiceTests {` | +1/−1 | M006-004, M006-005, M006-009, M006-011, M006-016, M006-025 |
| H235 | `backend/src/test/java/com/complex/platform/access/AccessServiceTests.java` | `@@ -867,9 +899,9 @@ class AccessServiceTests {` | +2/−2 | M006-004, M006-005, M006-009, M006-011, M006-016, M006-025 |
| H236 | `backend/src/test/java/com/complex/platform/access/AccessServiceTests.java` | `@@ -978,7 +1010,7 @@ class AccessServiceTests {` | +1/−1 | M006-004, M006-005, M006-009, M006-011, M006-016, M006-025 |
| H237 | `backend/src/test/java/com/complex/platform/access/AccessServiceTests.java` | `@@ -1044,6 +1076,17 @@ class AccessServiceTests {` | +11/−0 | M006-004, M006-005, M006-009, M006-011, M006-016, M006-025 |
| H238 | `backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java` | `@@ -1,5 +1,7 @@` | +2/−0 | M006-001 |
| H239 | `backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java` | `@@ -284,9 +286,9 @@ class AuthorizationServiceTests {` | +2/−2 | M006-001 |
| H240 | `backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java` | `@@ -0,0 +1,260 @@` | +260/−0 | M006-008, M006-014, M006-017 |
| H241 | `backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java` | `@@ -0,0 +1,595 @@` | +595/−0 | M006-001, M006-002, M006-003, M006-004, M006-005, M006-006, M006-007, M006-008, M006-009, M006-010, M006-011, M006-012, M006-013, M006-014, M006-018, M006-024, M006-025, M006-026, M006-028, M006-029, M006-030 |
| H242 | `backend/src/test/java/com/complex/platform/bar/BarServiceTests.java` | `@@ -1,5 +1,7 @@` | +2/−0 | M006-027 |
| H243 | `backend/src/test/java/com/complex/platform/bar/BarServiceTests.java` | `@@ -133,6 +135,15 @@ class BarServiceTests {` | +9/−0 | M006-027 |
| H244 | `backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java` | `@@ -1,5 +1,7 @@` | +2/−0 | M006-027 |
| H245 | `backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java` | `@@ -56,12 +58,12 @@ class BarStoredValueFundChainJdbcIntegrationTests {` | +4/−4 | M006-027 |
| H246 | `backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java` | `@@ -78,7 +80,8 @@ class BarStoredValueFundChainJdbcIntegrationTests {` | +2/−1 | M006-027 |
| H247 | `backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java` | `@@ -115,7 +118,8 @@ class BarStoredValueFundChainJdbcIntegrationTests {` | +2/−1 | M006-027 |
| H248 | `backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java` | `@@ -125,8 +129,8 @@ class BarStoredValueFundChainJdbcIntegrationTests {` | +2/−2 | M006-027 |
| H249 | `backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java` | `@@ -140,16 +144,16 @@ class BarStoredValueFundChainJdbcIntegrationTests {` | +3/−3 | M006-027 |
| H250 | `backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java` | `@@ -238,11 +242,8 @@ class BarStoredValueFundChainJdbcIntegrationTests {` | +2/−5 | M006-027 |
| H251 | `backend/src/test/java/com/complex/platform/customer/M006StoredValueJdbcTests.java` | `@@ -0,0 +1,140 @@` | +140/−0 | M006-027 |
| H252 | `backend/src/test/java/com/complex/platform/customer/M006StoredValuePaymentTests.java` | `@@ -0,0 +1,223 @@` | +223/−0 | M006-027 |
| H253 | `backend/src/test/java/com/complex/platform/customer/MemberWalletServiceTests.java` | `@@ -1,5 +1,7 @@` | +2/−0 | M006-027 |
| H254 | `backend/src/test/java/com/complex/platform/customer/MemberWalletServiceTests.java` | `@@ -37,7 +39,7 @@ class MemberWalletServiceTests {` | +1/−1 | M006-027 |
| H255 | `backend/src/test/java/com/complex/platform/customer/MemberWalletServiceTests.java` | `@@ -68,17 +70,17 @@ class MemberWalletServiceTests {` | +3/−3 | M006-027 |
| H256 | `backend/src/test/java/com/complex/platform/customer/MemberWalletServiceTests.java` | `@@ -87,7 +89,7 @@ class MemberWalletServiceTests {` | +1/−1 | M006-027 |
| H257 | `backend/src/test/java/com/complex/platform/customer/MemberWalletServiceTests.java` | `@@ -95,12 +97,12 @@ class MemberWalletServiceTests {` | +2/−2 | M006-027 |
| H258 | `backend/src/test/java/com/complex/platform/customer/MemberWalletServiceTests.java` | `@@ -110,7 +112,7 @@ class MemberWalletServiceTests {` | +1/−1 | M006-027 |
| H259 | `backend/src/test/java/com/complex/platform/customer/MemberWalletServiceTests.java` | `@@ -128,7 +130,7 @@ class MemberWalletServiceTests {` | +1/−1 | M006-027 |
| H260 | `backend/src/test/java/com/complex/platform/customer/MemberWalletServiceTests.java` | `@@ -155,6 +157,7 @@ class MemberWalletServiceTests {` | +1/−0 | M006-027 |
| H261 | `backend/src/test/java/com/complex/platform/customer/MemberWalletServiceTests.java` | `@@ -177,7 +180,7 @@ class MemberWalletServiceTests {` | +1/−1 | M006-027 |
| H262 | `backend/src/test/java/com/complex/platform/customer/MemberWalletServiceTests.java` | `@@ -191,12 +194,12 @@ class MemberWalletServiceTests {` | +2/−2 | M006-027 |
| H263 | `backend/src/test/java/com/complex/platform/customer/MemberWalletServiceTests.java` | `@@ -205,7 +208,7 @@ class MemberWalletServiceTests {` | +1/−1 | M006-027 |
| H264 | `backend/src/test/java/com/complex/platform/customer/MemberWalletServiceTests.java` | `@@ -246,7 +249,7 @@ class MemberWalletServiceTests {` | +1/−1 | M006-027 |
| H265 | `backend/src/test/java/com/complex/platform/customer/MemberWalletServiceTests.java` | `@@ -256,9 +259,9 @@ class MemberWalletServiceTests {` | +2/−2 | M006-027 |
| H266 | `backend/src/test/java/com/complex/platform/customer/MemberWalletServiceTests.java` | `@@ -272,7 +275,7 @@ class MemberWalletServiceTests {` | +1/−1 | M006-027 |
| H267 | `backend/src/test/java/com/complex/platform/customer/MemberWalletServiceTests.java` | `@@ -318,15 +321,15 @@ class MemberWalletServiceTests {` | +3/−3 | M006-027 |
| H268 | `backend/src/test/java/com/complex/platform/customer/MemberWalletServiceTests.java` | `@@ -336,7 +339,7 @@ class MemberWalletServiceTests {` | +1/−1 | M006-027 |
| H269 | `backend/src/test/java/com/complex/platform/customer/MemberWalletServiceTests.java` | `@@ -358,7 +361,7 @@ class MemberWalletServiceTests {` | +1/−1 | M006-027 |
| H270 | `backend/src/test/java/com/complex/platform/customer/MemberWalletServiceTests.java` | `@@ -377,7 +380,7 @@ class MemberWalletServiceTests {` | +1/−1 | M006-027 |
| H271 | `backend/src/test/java/com/complex/platform/customer/MemberWalletServiceTests.java` | `@@ -394,7 +397,7 @@ class MemberWalletServiceTests {` | +1/−1 | M006-027 |
| H272 | `backend/src/test/java/com/complex/platform/customer/MemberWalletServiceTests.java` | `@@ -405,20 +408,20 @@ class MemberWalletServiceTests {` | +3/−3 | M006-027 |
| H273 | `backend/src/test/java/com/complex/platform/customer/MemberWalletServiceTests.java` | `@@ -426,7 +429,7 @@ class MemberWalletServiceTests {` | +1/−1 | M006-027 |
| H274 | `backend/src/test/java/com/complex/platform/customer/MemberWalletServiceTests.java` | `@@ -458,7 +461,7 @@ class MemberWalletServiceTests {` | +1/−1 | M006-027 |
| H275 | `backend/src/test/java/com/complex/platform/customer/MemberWalletServiceTests.java` | `@@ -485,6 +488,20 @@ class MemberWalletServiceTests {` | +14/−0 | M006-027 |
| H276 | `backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java` | `@@ -1,5 +1,7 @@` | +2/−0 | M006-027 |
| H277 | `backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java` | `@@ -27,12 +29,18 @@ class StoredValueNotificationIntegrationTest {` | +7/−1 | M006-027 |
| H278 | `backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java` | `@@ -85,7 +93,7 @@ class StoredValueNotificationIntegrationTest {` | +1/−1 | M006-027 |
| H279 | `backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java` | `@@ -123,10 +131,13 @@ class StoredValueNotificationIntegrationTest {` | +5/−2 | M006-027 |
| H280 | `backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java` | `@@ -140,13 +151,13 @@ class StoredValueNotificationIntegrationTest {` | +2/−2 | M006-027 |


## 6. 实际命令与结果（验证不足如实）

### 6.1 diff / 空白 / CRLF

见 §3。`diff --check` exit 0。

### 6.2 AccessGrantPolicy 可执行核验

`AccessGrantPolicy.java` javac → `.scratch/m006-b4-r1-grok-verify/classes`（`-d` 该目录，未写 `backend/target`）。

临时 `PolicyCheck`（同包，exit 前删除）运行结果：

```
BOUNDS_REJECT=GRANT_POLICY_OUT_OF_BOUNDS
QUERY_REJECT=GRANT_POLICY_OUT_OF_BOUNDS
POLICY_OK window=30
TOP_REJECT=INVALID_GRANT_REVIEW_POLICY
```

判据：GEN-002、BSM-001、M006-016。查询越界与 `top_authority` 非 3 级均拒绝。

### 6.3 OTP 实现编译

`CustomerOtpVerifier` / `FailClosedCustomerOtpVerifier` / `FixedCustomerOtpVerifier` javac 到同一 scratch，exit 0。prod=`@Profile("prod")` 恒拒绝；local/dev/test 固定码不进入 prod。

### 6.4 V386

静态：2176 字节、6 句；V 号 386 > origin/main 当前最大 385。未跑迁移、未改共享表。

### 6.5 前端（工作区 55 路径 SHA 与 after tree 全等）

```
cd apps/web && npx --no-install vitest run \
  src/admin/pages/M006AccessGovernance.test.tsx \
  src/admin/pages/M006AccessRegistration.test.tsx \
  src/admin/pages/M006BarStoredValue.test.tsx \
  src/customer/pages/M006OrderStoredValue.test.tsx \
  src/admin/pages/AccessUsersPage.test.tsx \
  src/customer/pages/WalletPage.test.tsx
```

结果：Test Files 6 passed；Tests 15 passed；vitest v4.1.9；exit 0。未 `npm install`。

覆盖：030 第二页、016 政策保存、026 不调物理 DELETE、011 注册申请不发密码、025 不采集手机号、027 账单按账单金额+短信、订单支付不填扣款金额、钱包无 consume、NOT_CONFIGURED 不报到账。

### 6.6 未跑（缺口，不折算 PASS 证据）

- 本进程 `COMPLEX_DATABASE_URL/_USERNAME/_PASSWORD` 均 UNSET，无 `.env`/`.pgpass`；**未跑** `M006JdbcRegressionTests`、`M006StoredValueJdbcTests`、任何连接 `complex_test` 的 JDBC。
- **未跑** Maven / 不写 `backend/target` 的全量后端测试；`backend/target/classes` 时间戳 2026-08-02，只当依赖 classpath，不当本批编译产物。
- **未起** Spring 生产上下文，未实连支付/短信网关（按 027 也不应接通）。

## 7. ponytail-review（过工程，不改码）

- `AccessGrantPolicy`：016 需要后台政策对象，不是单实现空缝。
- `LOCK TABLE` 多表 SHARE：006/008 的可运行锁；注释已标后续再收窄。不另开 finding。
- 各文件 `// 文件版本`：归该文件主 ID 的文档行，不是无 ID 优化。
- `consumeInternal` 保留：027 酒吧账单在 SMS 之后的内部扣款，不是钱包随意 consume。
- AccessUser 7 参构造器保留：019 队列否。

`Lean already` 相对本批 25 项拟修；没有必须删除且无法归因的块。

## 8. Finding

**无 FAIL finding。** 对抗式扫过的残余如下，**不构成** 本轮越界或未修完的否决项（第一次正常路径不因此做不到 HTML 上位；要两件不巧才放大）：

- `copyPermissionTemplate` 源模板组织比较仍用 `actor.organizationId()`（H076 未改该比较，只改了新副本 owner 与 UUID）。空组织走后续 `requireProjectScope` 仍会拒绝。不在 003 原列行号内。
- 岗位模板 lifecycle：服务层一律 `requirePlatformAdmin`，控制器存档只需 `access.role.assign`。删除批准另要 USB，与 HTML「最高权限审批删除」一致。日常平台管理员是该页主用户。
- `FinanceService.markPaymentIntentSucceeded` 仍在（非本 diff 主路径）；`settleTopup` 不再用它代替渠道回执。
- `createIntent` 注释仍写网关关时由内部确认，实现已把 status 保持 created 且线上 settle 拒绝未启用网关。
- 前端测试有 antd key/`destroyOnClose` 警告，断言仍过。

## 9. 审计结论

**审计 PASS/FAIL：PASS**

25 个队列是的定稿 ID 在 diff 中均有对应修复与（或）测试；027 遵守 RESERVED/OFF、不伪造短信/支付成功、不删门票二维码。验证按 §6，缺口已写明。

**越界 PASS/FAIL：PASS**

280 hunk 均可归因到 001—018/024—030；无 019—023 作为修复授权；无无法归因行；无 audit raw；无三 HTML 改动。

本席不启动 B5，不改 findings/ledger/源码。

## 10. 两树 diff 逐字原文

before=`f49ef25cdadc0d4572f169d8c33b5e971c21f024`
after=`e845764656fb7f9c166026371371b720f10fde2a`

`````git-diff
diff --git a/apps/web/src/admin/pages/AccessGovernancePage.tsx b/apps/web/src/admin/pages/AccessGovernancePage.tsx
index a3e84dcd..1794adee 100644
--- a/apps/web/src/admin/pages/AccessGovernancePage.tsx
+++ b/apps/web/src/admin/pages/AccessGovernancePage.tsx
@@ -1,3 +1,4 @@
+// 文件版本：V0.03
 import React from "react";
 import { AdminDataState } from "../components/AdminDataState";
 import { AdminPageHeader } from "../components/AdminPageHeader";
@@ -11,10 +12,14 @@ import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowD
 import { GuidedFlowModal } from "../components/GuidedFlowModal";
 import type { AccessUserRow, AdminPageProps } from "../types";
 
+type BoundedInt = { value: number; min: number; max: number };
+type GrantPolicy = { windowDays: BoundedInt; positionThreshold: BoundedInt; userThreshold: BoundedInt;
+  riskReviews: Record<"low" | "medium" | "high" | "critical", { approvalLevel: string; requiredLevel: number }> };
 type RoleRow = { id: string; roleName?: string | null; roleCode?: string | null; status?: string | null; permissionCodes?: string[] };
 type PositionRoleTemplateRow = {
   id: string; positionCode: string; staffLevelId: string; roleId?: string | null; templateId?: string | null;
   organizationId?: string | null; status?: string | null; createdBy?: string | null;
+  archivedAt?: string | null; deletedAt?: string | null; deleteReviewId?: string | null; deleteReason?: string | null;
 };
 type AccessGrantRequestRow = {
   id: string; requestType: string; targetUserId: string; targetRoleId?: string | null; templateId?: string | null;
@@ -83,11 +88,18 @@ function statusBadge(status?: string | null) {
   return <span className={`admin-badge ${kind}`}>{value ? formatBusinessStatus(value) : "—"}</span>;
 }
 
-export function AccessGovernancePage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
+export function AccessGovernancePage({ api, user, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
   const [users, setUsers] = React.useState<AccessUserRow[]>([]);
   const [roles, setRoles] = React.useState<RoleRow[]>([]);
   const [positionTemplates, setPositionTemplates] = React.useState<PositionRoleTemplateRow[]>([]);
+  const [includeArchivedTemplates, setIncludeArchivedTemplates] = React.useState(false);
+  const [lifecycleReason, setLifecycleReason] = React.useState("");
   const [grantRequests, setGrantRequests] = React.useState<AccessGrantRequestRow[]>([]);
+  const [grantPage, setGrantPage] = React.useState(1);
+  const [grantTotal, setGrantTotal] = React.useState(0);
+  const [grantPageBusy, setGrantPageBusy] = React.useState(false);
+  const [grantPolicy, setGrantPolicy] = React.useState<GrantPolicy | null>(null);
+  const [grantPolicyReason, setGrantPolicyReason] = React.useState("");
   const [summary, setSummary] = React.useState<AccessGrantSummary | null>(null);
   const [summaryNotice, setSummaryNotice] = React.useState<string | null>(null);
   const [templateForm, setTemplateForm] = React.useState({ id: "", positionCode: "ops_manager", staffLevelId: "lvl_m1_lower", roleId: "", templateId: "", status: "active" });
@@ -120,15 +132,17 @@ export function AccessGovernancePage({ api, setActionResult, setGlobalError, eff
       const [userRows, roleRows, templateRows, requestRows, grantSummary] = await Promise.all([
         canUserView ? api.get<AccessUserRow[]>("/api/v1/access/users") : Promise.resolve([] as AccessUserRow[]),
         canRoleView ? api.get<RoleRow[]>("/api/v1/access/roles") : Promise.resolve([] as RoleRow[]),
-        api.get<PositionRoleTemplateRow[]>("/api/v1/access/position-role-templates"),
-        canGrantView ? api.get<AccessGrantRequestRow[]>("/api/v1/access/grant-requests?window_days=30") : Promise.resolve([] as AccessGrantRequestRow[]),
+        api.get<PositionRoleTemplateRow[]>(`/api/v1/access/position-role-templates?include_archived=${includeArchivedTemplates}`),
+        canGrantView ? api.getPaged<AccessGrantRequestRow>(`/api/v1/access/grant-requests?page=${grantPage}`) : Promise.resolve({ items: [] as AccessGrantRequestRow[], total: 0, page: 1, size: 50 }),
         // 智能总结=纯统计副资源（GRANT_VIEW 权限，无权账号可能 403）：单独降级，不拖垮角色装配/审批主流程。
-        loadSecondaryIfPermitted(canGrantView, () => api.get<AccessGrantSummary>("/api/v1/access/grant-requests/summary?window_days=30&position_threshold=2&user_threshold=2"), null as AccessGrantSummary | null),
+        loadSecondaryIfPermitted(canGrantView, () => api.get<AccessGrantSummary>("/api/v1/access/grant-requests/summary"), null as AccessGrantSummary | null),
       ]);
       setUsers(userRows ?? []);
       setRoles(roleRows ?? []);
       setPositionTemplates(templateRows ?? []);
-      setGrantRequests(requestRows ?? []);
+      setGrantRequests(requestRows.items);
+      setGrantPage(requestRows.page);
+      setGrantTotal(requestRows.total);
       setSummary(grantSummary.data);
       setSummaryNotice(grantSummary.notice);
     },
@@ -202,10 +216,47 @@ export function AccessGovernancePage({ api, setActionResult, setGlobalError, eff
     }
   }
 
-  async function deletePositionTemplate(id: string) {
-    await api.delete(`/api/v1/access/position-role-templates/${encodeURIComponent(id)}`);
-    setActionResult({ action: "access_position_role_template_deleted", templateId: id });
-    await reload();
+  async function loadGrantPage(page: number) {
+    setGrantPageBusy(true);
+    try {
+      const result = await api.getPaged<AccessGrantRequestRow>(`/api/v1/access/grant-requests?page=${page}`);
+      setGrantRequests(result.items); setGrantPage(result.page); setGrantTotal(result.total);
+    } catch (error) { setGlobalError(error instanceof Error ? error.message : "授权申请读取失败"); }
+    finally { setGrantPageBusy(false); }
+  }
+
+  async function loadGrantPolicy() {
+    try { setGrantPolicy(await api.get<GrantPolicy>("/api/v1/access/grant-policy")); }
+    catch (error) { setGlobalError(error instanceof Error ? error.message : "授权政策读取失败"); }
+  }
+
+  async function saveGrantPolicy() {
+    if (!grantPolicy || !grantPolicyReason.trim()) { setGlobalError("请读取政策并填写修改理由。"); return; }
+    try {
+      setGrantPolicy(await api.post<GrantPolicy>("/api/v1/access/grant-policy", {
+        policy: grantPolicy, operatorNote: grantPolicyReason.trim(),
+      }));
+      setActionResult({ action: "授权政策已保存" });
+      await reload();
+    } catch (error) { setGlobalError(error instanceof Error ? error.message : "授权政策保存失败"); }
+  }
+
+  async function changePositionLifecycle(id: string, action: string) {
+    if (!lifecycleReason.trim()) { setGlobalError("请填写存档、删除或恢复理由。"); return; }
+    try {
+      await api.post(`/api/v1/access/position-role-templates/${encodeURIComponent(id)}/lifecycle`, {
+        action, reason: lifecycleReason.trim(),
+      });
+      setActionResult({ action: "岗位模板状态已更新", templateId: id });
+      await reload();
+    } catch (error) { setGlobalError(error instanceof Error ? error.message : "岗位模板处理失败"); }
+  }
+
+  async function showArchivedTemplates(include: boolean) {
+    try {
+      const rows = await api.get<PositionRoleTemplateRow[]>(`/api/v1/access/position-role-templates?include_archived=${include}`);
+      setPositionTemplates(rows); setIncludeArchivedTemplates(include);
+    } catch (error) { setGlobalError(error instanceof Error ? error.message : "存档模板读取失败"); }
   }
 
   async function assignPosition() {
@@ -367,15 +418,23 @@ export function AccessGovernancePage({ api, setActionResult, setGlobalError, eff
           items={[
             { key: "assign", label: "① 角色装配", forceRender: true, children: (<>
         <h3>职位 ↔ 模板映射</h3>
+        <label>存档、删除或恢复理由<input data-testid="access-position-template-reason" value={lifecycleReason} onChange={(event) => setLifecycleReason(event.target.value)} /></label>
+        <label><input type="checkbox" checked={includeArchivedTemplates} onChange={(event) => void showArchivedTemplates(event.target.checked)} />显示存档及已删除模板</label>
         <div className="admin-table-scroll">
           <table className="admin-table" data-testid="access-position-role-templates-table">
             <thead><tr><th>编号</th><th>岗位</th><th>职级</th><th>角色</th><th>权限模板</th><th>组织</th><th>状态</th><th>操作</th><th>详情</th></tr></thead>
             <tbody>{positionTemplates.map((row) => (
               <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`access-position-role-template-${row.id}`} onClick={() => openTemplateDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openTemplateDetail(row); } }}>
-                <td>{row.id}</td><td>{zhPosition(row.positionCode)}</td><td>{zhLevel(row.staffLevelId)}</td><td>{row.roleId ?? "-"}</td><td>{row.templateId ?? "-"}</td><td>{row.organizationId ?? "-"}</td><td>{statusBadge(row.status)}</td>
+                <td>{row.id}</td><td>{zhPosition(row.positionCode)}</td><td>{zhLevel(row.staffLevelId)}</td><td>{row.roleId ?? "-"}</td><td>{row.templateId ?? "-"}</td><td>{row.organizationId ?? "-"}</td><td>{row.deletedAt ? "已删除（可恢复）" : row.archivedAt ? "已存档" : statusBadge(row.status)}</td>
                 <td onClick={(event) => event.stopPropagation()}>
-                  <button type="button" data-testid={`access-position-role-template-select-${row.id}`} onClick={() => selectTemplate(row)}>选中</button>
-                  <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid={`access-position-role-template-delete-${row.id}`} onClick={() => void deletePositionTemplate(row.id)}>删除</PermissionButton>
+                  <button type="button" data-testid={`access-position-role-template-select-${row.id}`} disabled={Boolean(row.archivedAt)} onClick={() => selectTemplate(row)}>选中</button>
+                  {!row.archivedAt ? <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid={`access-position-role-template-archive-${row.id}`} onClick={() => void changePositionLifecycle(row.id, "archive")}>存档</PermissionButton> : <>
+                    {!row.deletedAt && !row.deleteReviewId ? <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} onClick={() => void changePositionLifecycle(row.id, "request_delete")}>{user.id === "usr_master_authority" && user.username === "master-authority" ? "最高权限直接删除" : "申请删除"}</PermissionButton> : null}
+                    {!row.deletedAt && row.deleteReviewId ? <>
+                      <PermissionButton permissionsState={effectivePermissions} permissions={["access.role.assign", "governance.supervisor_review.approve"]} onClick={() => void changePositionLifecycle(row.id, "approve_delete")}>最高权限批准删除</PermissionButton>
+                      <PermissionButton permissionsState={effectivePermissions} permissions={["access.role.assign", "governance.supervisor_review.approve"]} onClick={() => void changePositionLifecycle(row.id, "reject_delete")}>驳回删除</PermissionButton>
+                    </> : <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} onClick={() => void changePositionLifecycle(row.id, "restore")}>恢复为停用模板</PermissionButton>}
+                  </>}
                 </td>
               <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看岗位角色模板详情" onClick={(event) => { event.stopPropagation(); openTemplateDetail(row); }}>查看详情</button></td></tr>
             ))}</tbody>
@@ -414,6 +473,11 @@ export function AccessGovernancePage({ api, setActionResult, setGlobalError, eff
           </>) },
           { key: "grant", label: "② 高危授权审批", forceRender: true, children: (<>
         <h3>高危授权审批</h3>
+        {canGrantView ? <div className="admin-form-row">
+          <span>第 {grantPage} 页，共 {grantTotal} 条</span>
+          <button type="button" disabled={grantPageBusy || grantPage <= 1} onClick={() => void loadGrantPage(grantPage - 1)}>上一页</button>
+          <button type="button" data-testid="access-grant-next-page" disabled={grantPageBusy || grantPage * 50 >= grantTotal} onClick={() => void loadGrantPage(grantPage + 1)}>下一页</button>
+        </div> : null}
         <div className="admin-form-row" data-testid="access-grant-request-form">
           <PermissionButton permissionsState={effectivePermissions} permissions={GRANT_APPLY} data-testid="access-grant-request-create-open"
             onClick={() => { setGrantError(""); setGrantOpen(true); }}>＋ 发起授权申请</PermissionButton>
@@ -445,6 +509,33 @@ export function AccessGovernancePage({ api, setActionResult, setGlobalError, eff
           </>) },
           { key: "summary", label: "③ 智能总结", forceRender: true, children: (<>
         <h3>智能总结</h3>
+        <details>
+          <summary>授权政策配置</summary>
+          <PermissionButton permissionsState={effectivePermissions} permissions={GRANT_VIEW} data-testid="access-grant-policy-load" onClick={() => void loadGrantPolicy()}>读取授权政策</PermissionButton>
+          {grantPolicy ? <>
+            {(["windowDays", "positionThreshold", "userThreshold"] as const).map((key) => <fieldset key={key}>
+              <legend>{{ windowDays: "统计时间窗（天）", positionThreshold: "岗位频繁申请阈值", userThreshold: "用户频繁申请阈值" }[key]}</legend>
+              {(["value", "min", "max"] as const).map((part) => <label key={part}>{{ value: "配置值", min: "下限", max: "上限" }[part]}
+                <input type="number" min={1} step={1} data-testid={`grant-policy-${key}-${part}`} value={grantPolicy[key][part]}
+                  onChange={(event) => setGrantPolicy({ ...grantPolicy, [key]: { ...grantPolicy[key], [part]: Number(event.target.value) } })} />
+              </label>)}
+            </fieldset>)}
+            {(["low", "medium", "high", "critical"] as const).map((risk) => <fieldset key={risk}>
+              <legend>{{ low: "低风险", medium: "中风险", high: "高风险", critical: "极高风险" }[risk]}权限审批</legend>
+              <label>审批层级<select value={grantPolicy.riskReviews[risk].approvalLevel}
+                onChange={(event) => setGrantPolicy({ ...grantPolicy, riskReviews: { ...grantPolicy.riskReviews,
+                  [risk]: { ...grantPolicy.riskReviews[risk], approvalLevel: event.target.value } } })}>
+                <option value="one_level_up">上一级</option><option value="two_levels_up">上两级</option><option value="top_authority">最高权限</option>
+              </select></label>
+              <label>主管级别<input type="number" min={1} max={3} step={1} value={grantPolicy.riskReviews[risk].requiredLevel}
+                onChange={(event) => setGrantPolicy({ ...grantPolicy, riskReviews: { ...grantPolicy.riskReviews,
+                  [risk]: { ...grantPolicy.riskReviews[risk], requiredLevel: Number(event.target.value) } } })} /></label>
+            </fieldset>)}
+            <label>修改理由<input data-testid="grant-policy-reason" value={grantPolicyReason} onChange={(event) => setGrantPolicyReason(event.target.value)} /></label>
+            <PermissionButton permissionsState={effectivePermissions} permissions={["admission.policy.manage"]} data-testid="access-grant-policy-save" onClick={() => void saveGrantPolicy()}>保存授权政策</PermissionButton>
+          </> : null}
+        </details>
+
         <PermissionButton permissionsState={effectivePermissions} permissions={GRANT_VIEW} data-testid="access-grant-summary-refresh" onClick={() => void reload()}>刷新总结</PermissionButton>
         {summaryNotice ? <div className="admin-state" data-testid="access-grant-summary-degraded">{summaryNotice}</div> : null}
         <div className="admin-table-scroll">
diff --git a/apps/web/src/admin/pages/AccessUsersPage.test.tsx b/apps/web/src/admin/pages/AccessUsersPage.test.tsx
index 6335ab60..438888d2 100644
--- a/apps/web/src/admin/pages/AccessUsersPage.test.tsx
+++ b/apps/web/src/admin/pages/AccessUsersPage.test.tsx
@@ -1,3 +1,4 @@
+// 文件版本：V0.01
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
 import { afterEach, describe, expect, it, vi } from "vitest";
 
@@ -61,7 +62,7 @@ describe("AccessUsersPage 行明细抽屉 + 零英文", () => {
     expect(drawer.textContent).toContain("主管");
     expect(drawer.textContent).toContain("总部主体");
     expect(drawer.textContent).toContain("商户甲");
-    expect(drawer.textContent).toContain("13800000001");
+    expect(drawer.textContent).not.toContain("13800000001");
   });
 
   it("Esc 关闭抽屉 + 点遮罩关闭", async () => {
diff --git a/apps/web/src/admin/pages/AccessUsersPage.tsx b/apps/web/src/admin/pages/AccessUsersPage.tsx
index 049d9bfc..de4f065d 100644
--- a/apps/web/src/admin/pages/AccessUsersPage.tsx
+++ b/apps/web/src/admin/pages/AccessUsersPage.tsx
@@ -1,3 +1,4 @@
+// 文件版本：V0.02
 import React from "react";
 import { AdminDataState } from "../components/AdminDataState";
 import { AdminPageHeader } from "../components/AdminPageHeader";
@@ -26,7 +27,6 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv
   const [templateNotice, setTemplateNotice] = React.useState<string | null>(null);
   const [username, setUsername] = React.useState("");
   const [password, setPassword] = React.useState("");
-  const [phone, setPhone] = React.useState("");
   const [accountType, setAccountType] = React.useState("staff");
   const [consentReference, setConsentReference] = React.useState("");
   const [evidenceAttachmentIds, setEvidenceAttachmentIds] = React.useState("");
@@ -87,14 +87,10 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv
       setFormError("员工账号不能为空。");
       return false;
     }
-    if (!password.trim()) {
+    if (accountType === "outsourced_vendor" && !password.trim()) {
       setFormError("初始密码不能为空；后台手动建号必须能完成登录闭环。 ");
       return false;
     }
-    if (phone.trim() && !/^1\d{10}$/.test(phone.trim())) {
-      setFormError("手机号必须是 11 位大陆手机号；不实名场景可留空。 ");
-      return false;
-    }
     if (!organizationId.trim()) {
       setFormError("组织 ID 不能为空，用于数据范围和留痕。 ");
       return false;
@@ -108,10 +104,21 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv
     setGlobalError(null);
     setSubmitting(true);
     try {
+      if (accountType !== "outsourced_vendor") {
+        const registration = await api.post<{ id: string }>("/api/v1/staff/registrations", {
+          name: username.trim(),
+          projectId: organizationId.trim(),
+          merchantId: merchantId.trim() || undefined,
+          operatorNote: "后台辅助员工登记，等待对应公司审批",
+        });
+        setActionResult({ action: "员工注册申请已提交", registrationId: registration.id, message: "在员工档案中完成公司审批后生成账号，并自动获得员工自助权限。" });
+        setUsername("");
+        setCreateOpen(false);
+        return;
+      }
       const created = await api.post<AccessUserRow>("/api/v1/access/users", {
         username: username.trim(),
         password: password.trim(),
-        phone: phone.trim() || undefined,
         accountType,
         status,
         organizationId: organizationId.trim(),
@@ -128,12 +135,11 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv
           operatorNote: "后台手动录入后应用权限模板",
         });
       }
-      const enriched = { ...created, phone: phone.trim() || created.phone, organizationId: organizationId.trim(), merchantId: merchantId.trim() || created.merchantId, status, accountType };
+      const enriched = { ...created, organizationId: organizationId.trim(), merchantId: merchantId.trim() || created.merchantId, status, accountType };
       setRows((current) => [...current, enriched]);
       setActionResult({ action: selectedTemplateId ? "创建员工并赋权" : "创建员工", username: created.username, accountType, templateId: selectedTemplateId || null, appliedTemplate });
       setUsername("");
       setPassword("");
-      setPhone("");
       setConsentReference("");
       setEvidenceAttachmentIds("");
       setCreateOpen(false);
@@ -170,7 +176,6 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv
       title: `员工账号 · ${row.username}`,
       fields: [
         { label: "员工账号", value: row.username },
-        { label: "手机号", value: row.phone ?? "—" },
         { label: "账号类型", value: formatBusinessStatus(row.accountType) },
         { label: "组织", value: orgDisplay(row.organizationId) },
         { label: "商户", value: merchantDisplay(row.merchantId) },
@@ -263,9 +268,9 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv
         </div>
         <div className="admin-table-scroll">
           <table className="admin-table" data-testid="access-users-table">
-            <thead><tr><th>员工账号</th><th>手机号</th><th>账号类型</th><th>组织</th><th>商户</th><th>状态</th><th>详情</th></tr></thead>
+            <thead><tr><th>员工账号</th><th>账号类型</th><th>组织</th><th>商户</th><th>状态</th><th>详情</th></tr></thead>
             <tbody>{rows.length === 0 ? (
-              <tr><td colSpan={7} className="admin-muted-action">暂无员工账号，可通过上方表单手动录入并套用权限模板。</td></tr>
+              <tr><td colSpan={6} className="admin-muted-action">暂无员工账号，可通过上方表单提交注册申请。</td></tr>
             ) : rows.map((row) => (
               <tr key={row.id} data-testid={`access-user-row-${row.id}`}
                 className={`admin-row-clickable${row.id === focusObjectId ? " admin-row-selected" : ""}`}
@@ -273,7 +278,6 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv
                 onClick={() => openUserDetail(row)}
                 onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openUserDetail(row); } }}>
                 <td>{row.username}</td>
-                <td>{row.phone ?? "-"}</td>
                 <td>{formatBusinessStatus(row.accountType)}</td>
                 <td>{row.organizationId ? (orgNames[row.organizationId] ?? <span className="admin-id-cell" title={row.organizationId}>{shortId(row.organizationId)}</span>) : "-"}</td>
                 <td>{row.merchantId ? (merchantNames[row.merchantId] ?? <span className="admin-id-cell" title={row.merchantId}>{shortId(row.merchantId)}</span>) : "-"}</td>
@@ -288,7 +292,7 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv
         title="新建员工账号"
         testId="access-user-flow"
         submitTestId="access-user-create"
-        submitLabel="新增员工并赋权"
+        submitLabel={accountType === "outsourced_vendor" ? "新建外部协作账号" : "提交员工注册申请"}
         submitting={submitting}
         error={formError}
         onClose={() => setCreateOpen(false)}
@@ -296,46 +300,44 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv
         steps={[
           {
             title: "账号信息",
-            description: "填写登录账号、初始密码与账号类型。",
+            description: "员工填写姓名并提交注册申请；外部协作机构填写登录账号与初始密码。",
             validate: () => {
               if (!username.trim()) return "员工账号不能为空。";
-              if (!password.trim()) return "初始密码不能为空；后台手动建号必须能完成登录闭环。";
-              if (phone.trim() && !/^1\d{10}$/.test(phone.trim())) return "手机号必须是 11 位大陆手机号；不实名场景可留空。";
+              if (accountType === "outsourced_vendor" && !password.trim()) return "初始密码不能为空。";
               return null;
             },
             content: (
               <>
-                <label>员工账号<input data-testid="access-user-name" value={username} onChange={(event) => setUsername(event.target.value)} placeholder="登录用账号名" /></label>
-                <label>初始密码<input data-testid="access-user-password" type="password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="首次登录使用" /></label>
-                <label>手机号<input data-testid="access-user-phone" value={phone} onChange={(event) => setPhone(event.target.value)} placeholder="可选，后端仅留哈希" /></label>
+                <label>{accountType === "outsourced_vendor" ? "登录账号" : "员工姓名"}<input data-testid="access-user-name" value={username} onChange={(event) => setUsername(event.target.value)} /></label>
+                {accountType === "outsourced_vendor" ? <label>初始密码<input data-testid="access-user-password" type="password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="首次登录使用" /></label> : null}
                 <label>账号类型<select data-testid="access-user-account-type" value={accountType} onChange={(event) => setAccountType(event.target.value)}>
-                  <option value="staff">普通员工</option>
-                  <option value="sales">销售人员</option>
-                  <option value="supervisor">主管</option>
-                  <option value="merchant_admin">商户管理员</option>
-                  <option value="merchant">商户员工</option>
+                  <option value="staff">员工注册申请</option>
+                  <option value="outsourced_vendor">外部协作机构账号</option>
                 </select></label>
               </>
             ),
           },
           {
             title: "归属与授权",
-            description: "指定账号归属的组织/商户，并留存授权凭据。",
+            description: accountType === "outsourced_vendor" ? "指定账号归属的组织/商户，并留存授权凭据。" : "指定员工归属的组织与商户。",
             validate: () => (organizationId.trim() ? null : "组织 ID 不能为空，用于数据范围和留痕。"),
             content: (
               <>
                 <label>所属组织<input data-testid="access-user-org" value={organizationId} onChange={(event) => setOrganizationId(event.target.value)} placeholder="组织 ID" /></label>
                 <label>所属商户<select data-testid="access-user-merchant" value={merchantId} onChange={(event) => setMerchantId(event.target.value)}><option value="">不绑定 / 平台账号</option>{merchants.map((m) => <option key={m.id} value={m.id}>{(m.displayName ?? m.companyName ?? m.id)}（{shortId(m.id)}）</option>)}</select></label>
+                {accountType === "outsourced_vendor" ? <>
                 <label>授权凭据<input data-testid="access-user-consent" value={consentReference} onChange={(event) => setConsentReference(event.target.value)} placeholder="同意/授权凭据编号（可选）" /></label>
                 <label>证据附件<input data-testid="access-user-evidence" value={evidenceAttachmentIds} onChange={(event) => setEvidenceAttachmentIds(event.target.value)} placeholder="附件 ID，逗号分隔（可选）" /></label>
+                </> : null}
               </>
             ),
           },
           {
             title: "状态与权限",
-            description: "设定初始状态，可同时套用权限模板完成赋权。",
+            description: "员工经对应公司审批后开通账号和员工自助权限，岗位授权在批准后分配。",
             content: (
               <>
+                {accountType !== "outsourced_vendor" ? <p>注册申请提交后，请在员工档案中完成公司审批；此处不提前生成可用账号或发放权限。</p> : <>
                 <label>初始状态<select data-testid="access-user-status" value={status} onChange={(event) => setStatus(event.target.value)}>
                   <option value="active">启用</option>
                   <option value="invited">待激活</option>
@@ -345,20 +347,22 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv
                   <option value="">暂不赋权</option>
                   {templates.map((template) => <option key={template.id} value={template.id}>{templateLabel(template)}</option>)}
                 </select></label>
+                </>}
               </>
             ),
           },
         ]}
         summary={[
           { label: "员工账号", value: username },
-          { label: "手机号", value: phone.trim() || "未填写" },
           { label: "账号类型", value: formatBusinessStatus(accountType) },
           { label: "所属组织", value: orgDisplay(organizationId.trim() || null) },
           { label: "所属商户", value: merchantDisplay(merchantId.trim() || null) },
-          { label: "初始状态", value: formatBusinessStatus(status) },
-          { label: "权限模板", value: (() => { const template = templates.find((t) => t.id === selectedTemplateId); return template ? templateLabel(template) : "暂不赋权"; })() },
-          { label: "授权凭据", value: consentReference.trim() || "未填写" },
-          { label: "证据附件", value: evidenceAttachmentIds.trim() || "未填写" },
+          { label: "初始状态", value: accountType === "outsourced_vendor" ? formatBusinessStatus(status) : "等待注册审批" },
+          { label: "权限模板", value: accountType !== "outsourced_vendor" ? "审批后自动获得员工自助权限" : (() => { const template = templates.find((t) => t.id === selectedTemplateId); return template ? templateLabel(template) : "暂不赋权"; })() },
+          ...(accountType === "outsourced_vendor" ? [
+            { label: "授权凭据", value: consentReference.trim() || "未填写" },
+            { label: "证据附件", value: evidenceAttachmentIds.trim() || "未填写" },
+          ] : []),
         ]}
       />
       <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
diff --git a/apps/web/src/admin/pages/BarOrdersPage.tsx b/apps/web/src/admin/pages/BarOrdersPage.tsx
index 87559cf0..baf19a7f 100644
--- a/apps/web/src/admin/pages/BarOrdersPage.tsx
+++ b/apps/web/src/admin/pages/BarOrdersPage.tsx
@@ -1,3 +1,4 @@
+// 文件版本：V0.02
 import React from "react";
 import { TechDetails } from "../components/TechDetails";
 import { AdminDataState } from "../components/AdminDataState";
@@ -140,6 +141,8 @@ export function BarOrdersPage({ api, setActionResult, setGlobalError, effectiveP
   const [paymentChannel, setPaymentChannel] = React.useState("aggregate_pay");
   const [cashierStaffId, setCashierStaffId] = React.useState("");
   const [posTransactionNo, setPosTransactionNo] = React.useState("");
+  const [paymentPhone, setPaymentPhone] = React.useState("");
+  const [paymentSmsCode, setPaymentSmsCode] = React.useState("");
   // 行明细抽屉：整行可点 → 该订单完整字段（状态/提成口径中文·金额¥·下单/支付时间中文）。
   const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
   // V345（BAR-VOID-A1）退酒申请 + 待审队列。
@@ -276,6 +279,10 @@ export function BarOrdersPage({ api, setActionResult, setGlobalError, effectiveP
       setFormError(TEXT.billRequired);
       return false;
     }
+    if (paymentChannel === "stored_value" && (!paymentPhone.trim() || !paymentSmsCode.trim())) {
+      setFormError("请填写付款手机号和短信验证码。");
+      return false;
+    }
     if (paymentChannel === "cash" && !cashierStaffId.trim()) {
       setFormError("现金结算需填现金收款人，供对账留痕。");
       return false;
@@ -287,6 +294,24 @@ export function BarOrdersPage({ api, setActionResult, setGlobalError, effectiveP
     return true;
   }
 
+  async function requestStoredValueCode() {
+    const billId = firstBillId(selectedOrder);
+    if (!billId || !paymentPhone.trim()) {
+      setFormError("请先选择账单并填写付款手机号。");
+      return;
+    }
+    setBusyAction("stored_value_sms");
+    setFormError("");
+    try {
+      await api.post(`/api/v1/bar/bills/${billId}/stored-value/sms`, { phone: paymentPhone.trim() });
+      setActionResult({ message: "验证码已发送，请由付款客人确认本账单扣款。" });
+    } catch (error) {
+      setFormError(error instanceof Error ? error.message : "验证码发送失败");
+    } finally {
+      setBusyAction("");
+    }
+  }
+
   async function settleBill() {
     const billId = firstBillId(selectedOrder);
     if (!billId) {
@@ -295,7 +320,8 @@ export function BarOrdersPage({ api, setActionResult, setGlobalError, effectiveP
     }
     await runOrderAction("bar_bill_settled", () => api.post(`/api/v1/bar/bills/${billId}/settle`, {
       paymentId: "payment-admin-web",
-      settlementAmountCent: selectedOrder?.totalAmountCent ?? selectedOrder?.totalAmount ?? 0,
+      ...(paymentChannel === "stored_value" ? { phone: paymentPhone.trim(), smsCode: paymentSmsCode.trim() }
+        : { settlementAmountCent: selectedOrder?.totalAmountCent ?? selectedOrder?.totalAmount ?? 0 }),
       paymentChannel,
       cashierStaffId: paymentChannel === "cash" ? cashierStaffId.trim() || undefined : undefined,
       posTransactionNo: paymentChannel === "pos" ? posTransactionNo.trim() || undefined : undefined,
@@ -454,11 +480,18 @@ export function BarOrdersPage({ api, setActionResult, setGlobalError, effectiveP
         <div className="admin-form-grid" data-testid="bar-settle-payment-form">
           <label>结算方式<select data-testid="bar-settle-channel" value={paymentChannel} onChange={(e) => setPaymentChannel(e.target.value)}>
             <option value="aggregate_pay">聚合支付</option>
+            <option value="stored_value">储值余额（手机号验证）</option>
             <option value="cash">现金</option>
             <option value="pos">POS 刷卡</option>
             <option value="bank_transfer">银行转账</option>
             <option value="other">其他</option>
           </select></label>
+          {paymentChannel === "stored_value" ? <>
+            <label>付款手机号<input type="tel" autoComplete="off" value={paymentPhone} onChange={(e) => setPaymentPhone(e.target.value)} /></label>
+            <PermissionButton permissionsState={effectivePermissions} permissions={["finance.payment.register"]} disabled={busyAction !== ""} onClick={() => void requestStoredValueCode()}>发送扣款验证码</PermissionButton>
+            <label>短信验证码<input inputMode="numeric" autoComplete="one-time-code" value={paymentSmsCode} onChange={(e) => setPaymentSmsCode(e.target.value)} /></label>
+            <p>从付款客人在本商户的储值卡扣除本账单剩余应付金额，短信验证通过后才会扣款。</p>
+          </> : null}
           {paymentChannel === "cash" ? (
             <label>现金收款人<input data-testid="bar-settle-cashier" value={cashierStaffId} onChange={(e) => setCashierStaffId(e.target.value)} placeholder="收款员工 ID" /></label>
           ) : null}
diff --git a/apps/web/src/admin/pages/M006AccessGovernance.test.tsx b/apps/web/src/admin/pages/M006AccessGovernance.test.tsx
new file mode 100644
index 00000000..d3d58fc7
--- /dev/null
+++ b/apps/web/src/admin/pages/M006AccessGovernance.test.tsx
@@ -0,0 +1,59 @@
+// 文件版本：V0.02
+import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
+import { afterEach, expect, it, vi } from "vitest";
+import { AccessGovernancePage } from "./AccessGovernancePage";
+import { AdminApi } from "../client/adminClient";
+import type { AdminPageProps } from "../types";
+afterEach(cleanup);
+const policy = {"windowDays":{"value":30,"min":1,"max":2147483647},"positionThreshold":{"value":3,"min":1,"max":2147483647},"userThreshold":{"value":3,"min":1,"max":2147483647},"riskReviews":{"low":{"approvalLevel":"one_level_up","requiredLevel":1},"medium":{"approvalLevel":"one_level_up","requiredLevel":1},"high":{"approvalLevel":"two_levels_up","requiredLevel":2},"critical":{"approvalLevel":"top_authority","requiredLevel":3}}};
+function props() {
+  const api = new AdminApi("test-token");
+  vi.spyOn(api, "get").mockImplementation(async (path) => {
+    if (path.endsWith("/grant-policy")) return policy as never;
+    if (path.includes("/summary")) return { positionRecommendations: [], userAnomalies: [] } as never;
+    return [] as never;
+  });
+  vi.spyOn(api, "getPaged").mockResolvedValue({ items: [], total: 51, page: 1, size: 50 });
+  vi.spyOn(api, "post").mockResolvedValue(policy);
+  return { api, user: { id: "admin", username: "管理员", accountType: "platform_admin" },
+    setActionResult: vi.fn(), setGlobalError: vi.fn(),
+    effectivePermissions: { status: "ready", value: { wildcard: true, grants: [], denies: [] } },
+  } as AdminPageProps;
+}
+it("M006-030 可前往授权申请第二页，概要查询不携带政策覆盖参数", async () => {
+  const p = props();
+  render(<AccessGovernancePage {...p} />);
+  await waitFor(() => expect(screen.getByTestId("access-grant-next-page")).toBeTruthy());
+  fireEvent.click(screen.getByTestId("access-grant-next-page"));
+  await waitFor(() => expect(p.api.getPaged).toHaveBeenCalledWith("/api/v1/access/grant-requests?page=2"));
+  expect(p.api.get).toHaveBeenCalledWith("/api/v1/access/grant-requests/summary");
+  expect(vi.mocked(p.api.get).mock.calls.every(([path]) => !path.includes("position_threshold"))).toBe(true);
+});
+it("M006-016 后台保存阈值及其上下限和风险审批映射", async () => {
+  const p = props();
+  render(<AccessGovernancePage {...p} />);
+  await waitFor(() => expect(screen.getByTestId("access-grant-policy-load")).toBeTruthy());
+  fireEvent.click(screen.getByTestId("access-grant-policy-load"));
+  await waitFor(() => expect(screen.getByTestId("grant-policy-positionThreshold-value")).toBeTruthy());
+  fireEvent.change(screen.getByTestId("grant-policy-positionThreshold-value"), { target: { value: "5" } });
+  fireEvent.change(screen.getByTestId("grant-policy-reason"), { target: { value: "调整频繁判断" } });
+  fireEvent.click(screen.getByTestId("access-grant-policy-save"));
+  await waitFor(() => expect(p.api.post).toHaveBeenCalledWith("/api/v1/access/grant-policy",
+    expect.objectContaining({ operatorNote: "调整频繁判断", policy: expect.objectContaining({ positionThreshold: expect.objectContaining({ value: 5 }) }) })));
+});
+
+it("M006-026 模板先存档再申请删除，界面不再调用物理 DELETE", async () => {
+  const p = props();
+  vi.mocked(p.api.get).mockImplementation(async (path) => {
+    if (path.includes("position-role-templates")) return [{ id: "mapping", positionCode: "ops_manager", staffLevelId: "level", status: "active" }] as never;
+    return [] as never;
+  });
+  const remove = vi.spyOn(p.api, "delete");
+  render(<AccessGovernancePage {...p} />);
+  await waitFor(() => expect(screen.getByTestId("access-position-role-template-archive-mapping")).toBeTruthy());
+  fireEvent.change(screen.getByTestId("access-position-template-reason"), { target: { value: "岗位配置停止使用" } });
+  fireEvent.click(screen.getByTestId("access-position-role-template-archive-mapping"));
+  await waitFor(() => expect(p.api.post).toHaveBeenCalledWith("/api/v1/access/position-role-templates/mapping/lifecycle",
+    expect.objectContaining({ action: "archive", reason: "岗位配置停止使用" })));
+  expect(remove).not.toHaveBeenCalled();
+});
diff --git a/apps/web/src/admin/pages/M006AccessRegistration.test.tsx b/apps/web/src/admin/pages/M006AccessRegistration.test.tsx
new file mode 100644
index 00000000..20e25ae7
--- /dev/null
+++ b/apps/web/src/admin/pages/M006AccessRegistration.test.tsx
@@ -0,0 +1,49 @@
+// 文件版本：V0.04
+import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
+import { afterEach, expect, it, vi } from "vitest";
+import { AccessUsersPage } from "./AccessUsersPage";
+import { StaffProfilesPage } from "./StaffProfilesPage";
+import { AdminApi } from "../client/adminClient";
+import type { AdminPageProps } from "../types";
+
+afterEach(cleanup);
+function props() {
+  const api = new AdminApi("test-token");
+  vi.spyOn(api, "get").mockResolvedValue([]);
+  vi.spyOn(api, "post").mockResolvedValue({ id: "created", registrationStatus: "submitted" });
+  return { api, user: { id: "company", username: "公司审批人", accountType: "platform_admin" },
+    setActionResult: vi.fn(), setGlobalError: vi.fn(),
+    effectivePermissions: { status: "ready", value: { wildcard: true, grants: [], denies: [] } },
+  } as AdminPageProps;
+}
+it("M006-011 员工引导表单提交注册申请，不发密码或预先赋权", async () => {
+  const p = props();
+  render(<AccessUsersPage {...p} />);
+  await waitFor(() => expect(screen.getByTestId("access-user-create-open")).toBeTruthy());
+  fireEvent.click(screen.getByTestId("access-user-create-open"));
+  fireEvent.change(screen.getByTestId("access-user-name"), { target: { value: "新员工" } });
+  await screen.findByTestId("access-user-org");
+  expect(screen.queryByTestId("access-user-consent")).toBeNull();
+  expect(screen.queryByTestId("access-user-evidence")).toBeNull();
+  fireEvent.click(screen.getByTestId("access-user-flow-next"));
+  fireEvent.click(screen.getByTestId("access-user-create"));
+  await waitFor(() => expect(p.api.post).toHaveBeenCalledTimes(1));
+  expect(p.api.post).toHaveBeenCalledWith("/api/v1/staff/registrations", expect.objectContaining({ name: "新员工", projectId: "org-1" }));
+  expect(vi.mocked(p.api.post).mock.calls[0]?.[1]).not.toHaveProperty("password");
+});
+it("M006-025 新建表单不收集无用途的手机号", async () => {
+  render(<AccessUsersPage {...props()} />);
+  await waitFor(() => expect(screen.getByTestId("access-user-create-open")).toBeTruthy());
+  fireEvent.click(screen.getByTestId("access-user-create-open"));
+  expect(screen.queryByTestId("access-user-phone")).toBeNull();
+});
+it("M006-011 员工档案审批使用现有注册复核端点", async () => {
+  const p = props();
+  vi.mocked(p.api.get).mockResolvedValue([{ id: "registration", name: "新员工", registrationStatus: "submitted", status: "active" }]);
+  render(<StaffProfilesPage {...p} />);
+  await waitFor(() => expect(screen.getByTestId("staff-registration-approve-registration")).toBeTruthy());
+  fireEvent.change(screen.getByTestId("staff-registration-password"), { target: { value: "initial-password" } });
+  fireEvent.click(screen.getByTestId("staff-registration-approve-registration"));
+  await waitFor(() => expect(p.api.post).toHaveBeenCalledWith("/api/v1/staff/registrations/registration/review",
+    expect.objectContaining({ decision: "approve", initialPassword: "initial-password" })));
+});
diff --git a/apps/web/src/admin/pages/M006BarStoredValue.test.tsx b/apps/web/src/admin/pages/M006BarStoredValue.test.tsx
new file mode 100644
index 00000000..d46c8e32
--- /dev/null
+++ b/apps/web/src/admin/pages/M006BarStoredValue.test.tsx
@@ -0,0 +1,34 @@
+// 文件版本：V0.01
+import { fireEvent, render, screen, waitFor } from "@testing-library/react";
+import { describe, expect, it, vi } from "vitest";
+import { BarOrdersPage } from "./BarOrdersPage";
+import type { AdminApiClient } from "../types";
+
+describe("M006-027 现场报手机号短信扣款", () => {
+  it("按真实账单提交手机号和验证码，不从订单总额重算扣款", async () => {
+    const api = {
+      get: vi.fn().mockResolvedValue([{ id: "order-1", totalAmountCent: 9000,
+        bills: [{ id: "bill-1" }] }]),
+      getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
+      post: vi.fn().mockResolvedValue({}), put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
+    };
+    vi.spyOn(window, "confirm").mockReturnValue(true);
+    render(<BarOrdersPage api={api as unknown as AdminApiClient}
+      user={{ id: "u1", username: "cashier", accountType: "staff" }}
+      setActionResult={vi.fn()} setGlobalError={vi.fn()}
+      effectivePermissions={{ status: "ready", value: {
+        grants: ["finance.payment.register"], denies: [], wildcard: false } }} />);
+    await screen.findByTestId("bar-order-select-order-1");
+    fireEvent.click(screen.getByTestId("bar-order-select-order-1"));
+    expect(screen.getByRole("option", { name: "储值余额（手机号验证）" })).toBeTruthy();
+    fireEvent.change(screen.getByTestId("bar-settle-channel"), { target: { value: "stored_value" } });
+    fireEvent.click(screen.getByTestId("bar-order-settle"));
+    expect(api.post).not.toHaveBeenCalled();
+    fireEvent.change(screen.getByLabelText("付款手机号"), { target: { value: "13800138000" } });
+    fireEvent.change(screen.getByLabelText("短信验证码"), { target: { value: "123456" } });
+    fireEvent.click(screen.getByTestId("bar-order-settle"));
+    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/bar/bills/bill-1/settle",
+      expect.objectContaining({ paymentChannel: "stored_value", phone: "13800138000", smsCode: "123456" })));
+    expect(api.post.mock.calls[0]?.[1]).not.toHaveProperty("settlementAmountCent");
+  });
+});
diff --git a/apps/web/src/admin/pages/StaffProfilesPage.tsx b/apps/web/src/admin/pages/StaffProfilesPage.tsx
index 1f1e670c..d8dedc76 100644
--- a/apps/web/src/admin/pages/StaffProfilesPage.tsx
+++ b/apps/web/src/admin/pages/StaffProfilesPage.tsx
@@ -1,3 +1,4 @@
+// 文件版本：V0.01
 import React from "react";
 import { AdminDataState } from "../components/AdminDataState";
 import { AdminPageHeader } from "../components/AdminPageHeader";
@@ -32,6 +33,7 @@ const MANAGE = ["staff.profile.review"];
 export function StaffProfilesPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
   const [rows, setRows] = React.useState<StaffProfileRow[]>([]);
   const [reason, setReason] = React.useState("");
+  const [initialPassword, setInitialPassword] = React.useState("");
   const [formError, setFormError] = React.useState("");
   const [submittingId, setSubmittingId] = React.useState("");
 
@@ -43,6 +45,25 @@ export function StaffProfilesPage({ api, setActionResult, setGlobalError, effect
     { onError: setGlobalError, fallbackMessage: "员工档案读取失败" },
   );
 
+  async function reviewRegistration(row: StaffProfileRow, decision: "approve" | "reject") {
+    if (decision === "approve" && !initialPassword.trim()) { setFormError("审批通过须填写初始密码。"); return; }
+    if (decision === "reject" && !reason.trim()) { setFormError("驳回须填写理由。"); return; }
+    setFormError("");
+    setSubmittingId(row.id);
+    try {
+      await api.post(`/api/v1/staff/registrations/${encodeURIComponent(row.id)}/review`, {
+        decision, initialPassword: decision === "approve" ? initialPassword : undefined,
+        rejectReason: decision === "reject" ? reason.trim() : undefined,
+        operatorNote: reason.trim() || "公司复核员工注册",
+      });
+      setActionResult({ action: decision === "approve" ? "员工注册审批通过" : "员工注册已驳回", staffProfileId: row.id });
+      setInitialPassword("");
+      reload();
+    } catch (error) {
+      setFormError(error instanceof Error ? error.message : "注册审批失败");
+    } finally { setSubmittingId(""); }
+  }
+
   async function toggleSalesPosition(row: StaffProfileRow) {
     const trimmedReason = reason.trim();
     if (!trimmedReason) {
@@ -75,6 +96,9 @@ export function StaffProfilesPage({ api, setActionResult, setGlobalError, effect
       />
       <AuditTrailHint event="staff.profile.sales_position_changed" />
       <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
+        {rows.some((row) => row.registrationStatus === "submitted") ? <label>注册审批初始密码
+          <input type="password" data-testid="staff-registration-password" value={initialPassword} onChange={(event) => setInitialPassword(event.target.value)} />
+        </label> : null}
         <div className="admin-form-row" data-testid="staff-profiles-reason-row">
           <label>改动事由
             <input
@@ -103,6 +127,10 @@ export function StaffProfilesPage({ api, setActionResult, setGlobalError, effect
                   <td>{formatBusinessStatus(row.status)}</td>
                   <td data-testid={`staff-profile-sales-position-${row.id}`}>{row.salesPosition ? "是" : "否"}</td>
                   <td>
+                    {row.registrationStatus === "submitted" ? <>
+                      <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid={`staff-registration-approve-${row.id}`} disabled={submittingId === row.id} onClick={() => void reviewRegistration(row, "approve")}>批准注册</PermissionButton>
+                      <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid={`staff-registration-reject-${row.id}`} disabled={submittingId === row.id} onClick={() => void reviewRegistration(row, "reject")}>驳回注册</PermissionButton>
+                    </> : null}
                     <PermissionButton
                       permissionsState={effectivePermissions}
                       permissions={MANAGE}
diff --git a/apps/web/src/customer/client/customerService.ts b/apps/web/src/customer/client/customerService.ts
index d761e949..e61ed33e 100644
--- a/apps/web/src/customer/client/customerService.ts
+++ b/apps/web/src/customer/client/customerService.ts
@@ -1,3 +1,4 @@
+// 文件版本：V0.03
 // 客人端业务服务层：封装 P0 客旅用到的真实端点（全部 /api/v1/customer/* 与 finance/ticketing）
 // 所有接口均在 docs/openapi/openapi-p0.json 契约内；P1 能力（积分明细/礼物兑换）不在此暴露。
 
@@ -126,6 +127,14 @@ export class CustomerService {
     return this.api.get<TicketCredential>(`/api/v1/customer/tickets/${encodeURIComponent(ticketId)}/qr`);
   }
 
+  requestStoredValueCode(phone: string): Promise<unknown> {
+    return this.api.post("/api/v1/customer/stored-value/sms", { phone });
+  }
+
+  payTicketOrderWithStoredValue(orderId: string, payload: { accountId: string; phone: string; smsCode: string }): Promise<TicketOrder> {
+    return this.api.post<TicketOrder>(`/api/v1/customer/orders/${encodeURIComponent(orderId)}/stored-value/pay`, payload);
+  }
+
   // ⑦ 储值 / 会员（V090，可选展示）
   getStoredValueAccount(): Promise<StoredValueAccount | null> {
     return this.api.get<StoredValueAccount | null>("/api/v1/customer/stored-value/account");
@@ -136,14 +145,18 @@ export class CustomerService {
     return this.api.get<StoredValueAccount[]>("/api/v1/customer/stored-value/accounts");
   }
 
-  // AUD-061 充值（绑支付意图，待柜台确认收款到账）
+  // 线上充值返回真实渠道拉起信息；入账须由服务端核验渠道成功回执。
   topupStoredValue(payload: { projectId?: string; merchantId?: string; amountCent: number; channel?: string }): Promise<PaymentIntent> {
-    return this.api.post<PaymentIntent>("/api/v1/customer/stored-value/topup", { channel: "aggregate_pay", ...payload });
+    return this.api.post<PaymentIntent>("/api/v1/customer/stored-value/topup", { ...payload });
+  }
+
+  settleStoredValueTopup(payload: { paymentIntentId: string; projectId?: string; merchantId?: string }): Promise<StoredValueTransaction> {
+    return this.api.post<StoredValueTransaction>("/api/v1/customer/stored-value/settle", payload);
   }
 
-  // AUD-061 消费（即时扣本卡余额；platform_collect 卡消费逐笔挂应付商户）
-  consumeStoredValue(payload: { projectId?: string; merchantId?: string; amountCent: number; reason?: string }): Promise<StoredValueTransaction> {
-    return this.api.post<StoredValueTransaction>("/api/v1/customer/stored-value/consume", payload);
+  getStoredValueTransactions(accountId: string, page = 1): Promise<StoredValueTransaction[]> {
+    return this.api.get<StoredValueTransaction[]>(
+      `/api/v1/customer/stored-value/accounts/${encodeURIComponent(accountId)}/transactions?page=${page}&size=20`);
   }
 
   // ⑧ 扫码点单（绑桌台会话 + 继承入场闸）：扫桌台二维码得 sessionId -> 看菜单 / 自助下单（不重复核龄）
diff --git a/apps/web/src/customer/pages/M006OrderStoredValue.test.tsx b/apps/web/src/customer/pages/M006OrderStoredValue.test.tsx
new file mode 100644
index 00000000..5b5c1048
--- /dev/null
+++ b/apps/web/src/customer/pages/M006OrderStoredValue.test.tsx
@@ -0,0 +1,28 @@
+// 文件版本：V0.03
+import { fireEvent, render, screen, waitFor } from "@testing-library/react";
+import { expect, it, vi } from "vitest";
+import { MyOrdersPage } from "./MyOrdersPage";
+import type { CustomerService } from "../client/customerService";
+
+it("M006-027 在订单中选择本人储值卡并提交短信验证，不允许填写扣款金额", async () => {
+  const service = {
+    listOrders: vi.fn().mockResolvedValue([{ id: "order-1", orderNo: "T1", payStatus: "unpaid",
+      issueStatus: "unissued", status: "created", salesChannel: "app", amountCent: 2500, quantity: 1 }]),
+    getStoredValueAccounts: vi.fn().mockResolvedValue([{ id: "card-1", merchantId: "m1",
+      projectId: null, cardScope: "merchant", balanceCent: 10000, status: "active" }]),
+    requestStoredValueCode: vi.fn().mockResolvedValue({}),
+    payTicketOrderWithStoredValue: vi.fn().mockResolvedValue({ id: "order-1", payStatus: "paid" }),
+  };
+  render(<MyOrdersPage service={service as unknown as CustomerService} />);
+  fireEvent.click(await screen.findByRole("button", { name: "储值余额支付" }));
+  await screen.findByLabelText("付款手机号");
+  fireEvent.change(screen.getByLabelText("付款手机号"), { target: { value: "13800138000" } });
+  fireEvent.change(screen.getByLabelText("短信验证码"), { target: { value: "123456" } });
+  const submit = await screen.findByRole("button", { name: /确认余额支付/ });
+  await waitFor(() => expect(submit).toBeEnabled());
+  fireEvent.click(submit);
+  await waitFor(() => expect(service.payTicketOrderWithStoredValue).toHaveBeenCalledWith("order-1", {
+    accountId: "card-1", phone: "13800138000", smsCode: "123456",
+  }));
+  expect(screen.queryByRole("spinbutton")).toBeNull();
+});
diff --git a/apps/web/src/customer/pages/MyOrdersPage.tsx b/apps/web/src/customer/pages/MyOrdersPage.tsx
index 3a9e7527..861aeb0e 100644
--- a/apps/web/src/customer/pages/MyOrdersPage.tsx
+++ b/apps/web/src/customer/pages/MyOrdersPage.tsx
@@ -1,9 +1,10 @@
-import { Alert, Button, Card, Empty, List, Modal, Space, Spin, Tag, Typography } from "antd";
+// 文件版本：V0.01
+import { Alert, Button, Card, Empty, Input, List, Modal, Select, Space, Spin, Tag, Typography, message } from "antd";
 import { useEffect, useState } from "react";
 
 import { TicketQrView } from "../components/TicketQrView";
 import type { CustomerService } from "../client/customerService";
-import type { CustomerTicket, TicketOrder } from "../types";
+import type { CustomerTicket, StoredValueAccount, TicketOrder } from "../types";
 import { formatBusinessStatus } from "../i18n/statusLabels";
 
 const { Text } = Typography;
@@ -19,6 +20,64 @@ export function MyOrdersPage({ service }: { service: CustomerService }): React.R
   const [ticketsLoading, setTicketsLoading] = useState(false);
   const [ticketsError, setTicketsError] = useState<string | null>(null);
   const [qrTicketId, setQrTicketId] = useState<string | null>(null);
+  const [payOrder, setPayOrder] = useState<TicketOrder | null>(null);
+  const [cards, setCards] = useState<StoredValueAccount[]>([]);
+  const [cardId, setCardId] = useState<string>();
+  const [phone, setPhone] = useState("");
+  const [smsCode, setSmsCode] = useState("");
+  const [paymentBusy, setPaymentBusy] = useState(false);
+  const [paymentError, setPaymentError] = useState<string | null>(null);
+
+  async function openStoredValuePayment(order: TicketOrder) {
+    setPayOrder(order);
+    setCards([]);
+    setCardId(undefined);
+    setPhone("");
+    setSmsCode("");
+    setPaymentError(null);
+    setPaymentBusy(true);
+    try {
+      const ownCards = await service.getStoredValueAccounts();
+      setCards(ownCards);
+      setCardId(ownCards.find((card) => card.status === "active" && card.projectId === null)?.id);
+    } catch (err) {
+      setPaymentError(err instanceof Error ? err.message : "储值卡加载失败");
+    } finally {
+      setPaymentBusy(false);
+    }
+  }
+
+  async function submitStoredValuePayment() {
+    if (!payOrder || !cardId || !phone.trim() || !smsCode.trim()) return;
+    setPaymentBusy(true);
+    setPaymentError(null);
+    try {
+      const paid = await service.payTicketOrderWithStoredValue(payOrder.id, {
+        accountId: cardId, phone: phone.trim(), smsCode: smsCode.trim(),
+      });
+      setOrders((previous) => previous.map((order) => order.id === paid.id ? paid : order));
+      setPayOrder(null);
+      setSmsCode("");
+    } catch (err) {
+      setPaymentError(err instanceof Error ? err.message : "余额支付失败");
+    } finally {
+      setPaymentBusy(false);
+    }
+  }
+
+  async function requestPaymentCode() {
+    if (!phone.trim()) { setPaymentError("请填写本人注册手机号"); return; }
+    setPaymentBusy(true);
+    setPaymentError(null);
+    try {
+      await service.requestStoredValueCode(phone.trim());
+      void message.success("验证码已发送");
+    } catch (err) {
+      setPaymentError(err instanceof Error ? err.message : "验证码发送失败");
+    } finally {
+      setPaymentBusy(false);
+    }
+  }
 
   useEffect(() => {
     let active = true;
@@ -116,6 +175,25 @@ export function MyOrdersPage({ service }: { service: CustomerService }): React.R
 
   return (
     <div data-testid="my-orders-page">
+      <Modal title="订单储值支付" open={payOrder !== null} onCancel={() => setPayOrder(null)}
+        closable={!paymentBusy} maskClosable={false} keyboard={!paymentBusy}
+        okText="确认余额支付" cancelText="取消" confirmLoading={paymentBusy}
+        okButtonProps={{ disabled: paymentBusy || !cardId || !phone.trim() || !smsCode.trim() }}
+        cancelButtonProps={{ disabled: paymentBusy }} onOk={() => void submitStoredValuePayment()}>
+        {paymentError ? <Alert type="error" message={paymentError} /> : null}
+        <Space direction="vertical" style={{ width: "100%" }}>
+          <Text>本订单应付 ¥{((payOrder?.amountCent ?? 0) / 100).toFixed(2)}</Text>
+          <Select aria-label="付款储值卡" value={cardId} onChange={setCardId} style={{ width: "100%" }}
+            options={cards.map((card) => ({ value: card.id,
+              disabled: card.status !== "active" || card.projectId !== null,
+              label: `${card.merchantId ? "商户卡（" + card.merchantId + "）" : "综合体通用卡"} · 余额 ¥${(card.balanceCent / 100).toFixed(2)}${card.projectId ? " · 该订单暂不支持此卡范围" : ""}` }))} />
+          <label htmlFor="order-payment-phone">付款手机号</label>
+          <Input id="order-payment-phone" type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} />
+          <Button disabled={paymentBusy} onClick={() => void requestPaymentCode()}>发送扣款验证码</Button>
+          <label htmlFor="order-payment-code">短信验证码</label>
+          <Input id="order-payment-code" autoComplete="one-time-code" inputMode="numeric" value={smsCode} onChange={(e) => setSmsCode(e.target.value)} />
+        </Space>
+      </Modal>
       <Card title="我的订单 / 我的票 / 我的权益">
         {error ? <Alert type="error" message="订单加载失败" description={error} style={{ marginBottom: 12 }} /> : null}
         {loading ? (
@@ -141,6 +219,9 @@ export function MyOrdersPage({ service }: { service: CustomerService }): React.R
                       <Text type="secondary">
                         {order.quantity} 张 · ¥{(order.amountCent / 100).toFixed(2)} · 游玩日 {order.visitDate ?? "—"}
                       </Text>
+                      {order.payStatus === "unpaid" && order.status === "created" ? (
+                        <Button onClick={() => void openStoredValuePayment(order)}>储值余额支付</Button>
+                      ) : null}
                       <Button size="small" onClick={() => void toggleTickets(order.id)} data-testid="order-tickets-toggle">
                         {expandedOrderId === order.id ? "收起票据" : "查看票 / 出码"}
                       </Button>
diff --git a/apps/web/src/customer/pages/WalletPage.test.tsx b/apps/web/src/customer/pages/WalletPage.test.tsx
index 917878fe..49a7bafa 100644
--- a/apps/web/src/customer/pages/WalletPage.test.tsx
+++ b/apps/web/src/customer/pages/WalletPage.test.tsx
@@ -1,4 +1,5 @@
-import { render, screen, waitFor } from "@testing-library/react";
+// 文件版本：V0.04
+import { fireEvent, render, screen, waitFor } from "@testing-library/react";
 import { afterEach, describe, expect, it, vi } from "vitest";
 
 import { WalletPage } from "./WalletPage";
@@ -31,13 +32,13 @@ const merchantCard = {
 };
 
 describe("WalletPage", () => {
-  afterEach(() => vi.restoreAllMocks());
+  afterEach(() => { vi.restoreAllMocks(); sessionStorage.removeItem("customer-pending-stored-value-topups"); });
 
   it("renders per-card balances for the general card and each merchant card", async () => {
     const service = {
       getStoredValueAccounts: vi.fn().mockResolvedValue([projectCard, merchantCard]),
       topupStoredValue: vi.fn(),
-      consumeStoredValue: vi.fn(),
+      getStoredValueTransactions: vi.fn().mockResolvedValue([]),
     } as unknown as CustomerService;
 
     render(<WalletPage service={service} />);
@@ -49,16 +50,57 @@ describe("WalletPage", () => {
     expect(screen.getByText("余额 ¥30.00")).toBeInTheDocument();
     // 商户卡资金模式标签（平台代收）。
     expect(screen.getByText("平台代收")).toBeInTheDocument();
-    // 充值/消费入口存在。
     expect(screen.getByTestId("wallet-topup")).toBeInTheDocument();
-    expect(screen.getByTestId("wallet-consume")).toBeInTheDocument();
+    expect(screen.queryByTestId("wallet-consume")).not.toBeInTheDocument();
+    expect(screen.getByText("交易记录")).toBeInTheDocument();
+  });
+
+  it("按选择的渠道发起线上充值，未配置时不报告到账或要求柜台收款", async () => {
+    const topup = vi.fn().mockResolvedValue({ id: "pay-1", gatewayStatus: "NOT_CONFIGURED" });
+    const service = {
+      getStoredValueAccounts: vi.fn().mockResolvedValue([merchantCard]),
+      getStoredValueTransactions: vi.fn().mockResolvedValue([]),
+      topupStoredValue: topup,
+    } as unknown as CustomerService;
+    render(<WalletPage service={service} />);
+    await screen.findAllByText(/商户卡（m1）/);
+    fireEvent.change(screen.getByLabelText("充值支付方式"), { target: { value: "alipay" } });
+    fireEvent.change(screen.getByRole("spinbutton"), { target: { value: "50" } });
+    fireEvent.click(screen.getByTestId("wallet-topup"));
+    await waitFor(() => expect(topup).toHaveBeenCalledWith(expect.objectContaining({ channel: "alipay", amountCent: 5000 })));
+    expect(await screen.findByText("支付渠道未配置，尚未扣款或充值到账")).toBeInTheDocument();
+    expect(screen.queryByText(/请到柜台完成支付/)).toBeNull();
+  });
+
+
+  it("返回钱包后仅按服务端确认结果刷新余额，失败充值继续保留供查询", async () => {
+    sessionStorage.setItem("customer-pending-stored-value-topups", JSON.stringify([
+      { paymentIntentId: "paid", merchantId: "m1" }, { paymentIntentId: "pending", merchantId: "m1" },
+    ]));
+    const settle = vi.fn().mockImplementation(({ paymentIntentId }) =>
+      paymentIntentId === "paid" ? Promise.resolve({ id: "actual-topup" }) : Promise.reject(new Error("支付尚未成功")));
+    const accounts = vi.fn().mockResolvedValue([merchantCard]);
+    const service = {
+      getStoredValueAccounts: accounts,
+      getStoredValueTransactions: vi.fn().mockResolvedValue([]),
+      settleStoredValueTopup: settle,
+    } as unknown as CustomerService;
+    render(<WalletPage service={service} />);
+    expect(await screen.findByText("支付尚未成功")).toBeInTheDocument();
+    expect(settle).toHaveBeenCalledWith({ paymentIntentId: "paid", merchantId: "m1" });
+    expect(settle).toHaveBeenCalledWith({ paymentIntentId: "pending", merchantId: "m1" });
+    expect(accounts).toHaveBeenCalledTimes(2);
+    expect(JSON.parse(sessionStorage.getItem("customer-pending-stored-value-topups") ?? "[]"))
+      .toEqual([{ paymentIntentId: "pending", merchantId: "m1" }]);
+    expect(screen.queryByRole("button", { name: "查询充值到账 · paid" })).toBeNull();
+    expect(screen.getByRole("button", { name: "查询充值到账 · pending" })).toBeInTheDocument();
   });
 
   it("shows empty state when the customer has no stored-value cards", async () => {
     const service = {
       getStoredValueAccounts: vi.fn().mockResolvedValue([]),
       topupStoredValue: vi.fn(),
-      consumeStoredValue: vi.fn(),
+      getStoredValueTransactions: vi.fn().mockResolvedValue([]),
     } as unknown as CustomerService;
 
     render(<WalletPage service={service} />);
diff --git a/apps/web/src/customer/pages/WalletPage.tsx b/apps/web/src/customer/pages/WalletPage.tsx
index b9f4460d..32bff649 100644
--- a/apps/web/src/customer/pages/WalletPage.tsx
+++ b/apps/web/src/customer/pages/WalletPage.tsx
@@ -1,11 +1,21 @@
+// 文件版本：V0.02
 import { Alert, Button, Card, Empty, InputNumber, List, message, Select, Space, Spin, Tag, Typography } from "antd";
 import { useCallback, useEffect, useMemo, useState } from "react";
 
 import type { CustomerService } from "../client/customerService";
-import type { StoredValueAccount } from "../types";
+import type { StoredValueAccount, StoredValueTransaction } from "../types";
 import { formatBusinessStatus } from "../i18n/statusLabels";
 
 const { Paragraph, Text } = Typography;
+type PendingTopup = { paymentIntentId: string; projectId?: string; merchantId?: string };
+const PENDING_TOPUPS = "customer-pending-stored-value-topups";
+
+function readPendingTopups(): PendingTopup[] {
+  try {
+    const value = JSON.parse(sessionStorage.getItem(PENDING_TOPUPS) ?? "[]") as unknown;
+    return Array.isArray(value) ? value.filter((item) => typeof item?.paymentIntentId === "string") : [];
+  } catch { return []; }
+}
 
 // 卡名：通用卡=综合体通用卡；商户卡=该发卡商户的卡。
 function cardLabel(acc: StoredValueAccount): string {
@@ -22,7 +32,7 @@ function yuan(cent: number): string {
   return `¥${(cent / 100).toFixed(2)}`;
 }
 
-// ⑦ 储值钱包（AUD-061）：多卡（通用卡 + 各商户卡）分卡余额展示 + 选卡充值/消费。3.17：error/loading/empty 分离可重试。
+// ⑦ 储值钱包（AUD-061）：多卡（通用卡 + 各商户卡）分卡余额展示 + 选卡充值与交易记录。3.17：error/loading/empty 分离可重试。
 export function WalletPage({ service }: { service: CustomerService }): React.ReactElement {
   const [accounts, setAccounts] = useState<StoredValueAccount[]>([]);
   const [error, setError] = useState<string | null>(null);
@@ -30,6 +40,26 @@ export function WalletPage({ service }: { service: CustomerService }): React.Rea
   const [selectedId, setSelectedId] = useState<string | undefined>(undefined);
   const [amountYuan, setAmountYuan] = useState<number | null>(null);
   const [busy, setBusy] = useState(false);
+  const [channel, setChannel] = useState("wechat");
+  const [paymentError, setPaymentError] = useState<string | null>(null);
+  const [pendingTopups, setPendingTopups] = useState<PendingTopup[]>(readPendingTopups);
+  const [transactions, setTransactions] = useState<StoredValueTransaction[]>([]);
+  const [historyPage, setHistoryPage] = useState(1);
+  const [historyLoading, setHistoryLoading] = useState(false);
+  const [historyError, setHistoryError] = useState<string | null>(null);
+
+  useEffect(() => {
+    let active = true;
+    setTransactions([]);
+    setHistoryError(null);
+    if (!selectedId) return;
+    setHistoryLoading(true);
+    service.getStoredValueTransactions(selectedId, historyPage)
+      .then((rows) => { if (active) setTransactions(rows); })
+      .catch((err) => { if (active) setHistoryError(err instanceof Error ? err.message : "交易记录加载失败"); })
+      .finally(() => { if (active) setHistoryLoading(false); });
+    return () => { active = false; };
+  }, [service, selectedId, historyPage, accounts]);
 
   const load = useCallback(() => {
     let active = true;
@@ -63,35 +93,57 @@ export function WalletPage({ service }: { service: CustomerService }): React.Rea
     merchantId: acc?.merchantId ?? undefined,
   });
 
-  const doTopup = (): void => {
-    if (!selected || !amountYuan || amountYuan <= 0) {
-      void message.warning("请选择储值卡并输入充值金额");
-      return;
-    }
+  async function checkTopup(pending: PendingTopup) {
     setBusy(true);
-    service
-      .topupStoredValue({ ...scopeOf(selected), amountCent: Math.round(amountYuan * 100) })
-      .then(() => void message.success("已发起充值，请到柜台完成支付收款后到账"))
-      .catch((e) => void message.error(e instanceof Error ? e.message : "充值发起失败"))
-      .finally(() => setBusy(false));
-  };
+    setPaymentError(null);
+    try {
+      await service.settleStoredValueTopup(pending);
+      const remaining = readPendingTopups().filter((item) => item.paymentIntentId !== pending.paymentIntentId);
+      sessionStorage.setItem(PENDING_TOPUPS, JSON.stringify(remaining));
+      setPendingTopups(remaining);
+      load();
+      void message.success("充值已到账");
+    } catch (err) {
+      setPaymentError(err instanceof Error ? err.message : "暂未确认到账，请稍后查询");
+    } finally {
+      setBusy(false);
+    }
+  }
+
+  useEffect(() => {
+    // 从渠道回到钱包或刷新页面后，继续核验尚未确认的真实充值单。
+    void (async () => { for (const pending of readPendingTopups()) await checkTopup(pending); })();
+  }, [service]);
 
-  const doConsume = (): void => {
+  async function doTopup() {
     if (!selected || !amountYuan || amountYuan <= 0) {
-      void message.warning("请选择储值卡并输入消费金额");
+      void message.warning("请选择储值卡并输入充值金额");
       return;
     }
     setBusy(true);
-    service
-      .consumeStoredValue({ ...scopeOf(selected), amountCent: Math.round(amountYuan * 100), reason: "钱包消费" })
-      .then(() => {
-        void message.success("消费成功");
-        setAmountYuan(null);
-        load();
-      })
-      .catch((e) => void message.error(e instanceof Error ? e.message : "消费失败"))
-      .finally(() => setBusy(false));
-  };
+    setPaymentError(null);
+    try {
+      const scope = scopeOf(selected);
+      const intent = await service.topupStoredValue({ ...scope, amountCent: Math.round(amountYuan * 100), channel });
+      if (intent.gatewayStatus === "RESERVED") throw new Error("支付渠道未启用，尚未扣款或充值到账");
+      if (intent.gatewayStatus === "NOT_CONFIGURED") throw new Error("支付渠道未配置，尚未扣款或充值到账");
+      if (!["PENDING", "SUCCEEDED"].includes(String(intent.gatewayStatus))
+          || typeof intent.paymentUrl !== "string" || !intent.id) {
+        throw new Error("支付渠道未返回有效支付页面，尚未确认收款");
+      }
+      const paymentUrl = new URL(intent.paymentUrl);
+      if (paymentUrl.protocol !== "https:") throw new Error("支付渠道返回的支付地址无效");
+      const pending = { paymentIntentId: intent.id, ...scope };
+      const next = [...readPendingTopups().filter((item) => item.paymentIntentId !== intent.id), pending];
+      sessionStorage.setItem(PENDING_TOPUPS, JSON.stringify(next));
+      setPendingTopups(next);
+      window.location.assign(paymentUrl.href);
+    } catch (err) {
+      setPaymentError(err instanceof Error ? err.message : "充值发起失败");
+    } finally {
+      setBusy(false);
+    }
+  }
 
   return (
     <div data-testid="wallet-page">
@@ -138,7 +190,8 @@ export function WalletPage({ service }: { service: CustomerService }): React.Rea
         )}
       </Card>
 
-      <Card title="充值 / 消费" style={{ marginTop: 16 }}>
+      <Card title="充值" style={{ marginTop: 16 }}>
+        {paymentError ? <Alert type="error" message={paymentError} /> : null}
         {loading ? (
           <Spin />
         ) : (
@@ -147,10 +200,13 @@ export function WalletPage({ service }: { service: CustomerService }): React.Rea
               data-testid="wallet-card-select"
               placeholder="选择储值卡"
               value={selectedId}
-              onChange={(v) => setSelectedId(v)}
+              onChange={(v) => { setSelectedId(v); setHistoryPage(1); }}
               style={{ width: "100%" }}
               options={accounts.map((a) => ({ value: a.id, label: `${cardLabel(a)} · 余额 ${yuan(a.balanceCent)}` }))}
             />
+            <label>充值支付方式<select aria-label="充值支付方式" value={channel} onChange={(e) => setChannel(e.target.value)}>
+              <option value="wechat">微信支付</option><option value="alipay">支付宝</option><option value="unionpay">银联</option>
+            </select></label>
             <InputNumber
               data-testid="wallet-amount"
               addonBefore="¥"
@@ -166,16 +222,30 @@ export function WalletPage({ service }: { service: CustomerService }): React.Rea
               <Button type="primary" loading={busy} data-testid="wallet-topup" onClick={doTopup}>
                 充值
               </Button>
-              <Button loading={busy} data-testid="wallet-consume" onClick={doConsume}>
-                消费
-              </Button>
             </Space>
             <Paragraph type="secondary" style={{ marginBottom: 0 }}>
-              充值需到柜台完成支付收款后到账；储值消费即时扣本卡余额。商户预收卡退卡请到店办理。
+              在对应订单中选择储值余额支付；现场消费报手机号并验证短信后按账单扣款。商户预收卡退卡请到店办理。
             </Paragraph>
           </Space>
         )}
       </Card>
+      {pendingTopups.map((pending) => <Button key={pending.paymentIntentId} disabled={busy} style={{ marginTop: 12 }} onClick={() => void checkTopup(pending)}>查询充值到账 · {pending.paymentIntentId}</Button>)}
+      <Card title="交易记录" style={{ marginTop: 16 }}>
+        {historyError ? <Alert type="error" message={historyError} /> : historyLoading ? <Spin /> :
+          <List dataSource={transactions} locale={{ emptyText: "暂无交易记录" }} renderItem={(txn) => (
+            <List.Item key={txn.id}>
+              <Space direction="vertical">
+                <Text>{({ topup: "充值", consume: "消费", refund: "退款" } as Record<string, string>)[txn.txnType] ?? "余额变动"} · {yuan(txn.amountCent)}</Text>
+                <Text type="secondary">余额 {yuan(txn.balanceAfterCent)} · {new Date(txn.createdAt).toLocaleString("zh-CN")}</Text>
+              </Space>
+            </List.Item>
+          )} />}
+        <Space>
+          <Button disabled={historyLoading || historyPage === 1} onClick={() => setHistoryPage((p) => p - 1)}>上一页</Button>
+          <Text>第 {historyPage} 页</Text>
+          <Button disabled={historyLoading || transactions.length < 20} onClick={() => setHistoryPage((p) => p + 1)}>下一页</Button>
+        </Space>
+      </Card>
     </div>
   );
 }
diff --git a/backend/src/main/java/com/complex/platform/access/AccessController.java b/backend/src/main/java/com/complex/platform/access/AccessController.java
index 22329d53..c5e2cb92 100644
--- a/backend/src/main/java/com/complex/platform/access/AccessController.java
+++ b/backend/src/main/java/com/complex/platform/access/AccessController.java
@@ -1,4 +1,5 @@
 package com.complex.platform.access;
+// 文件版本：V0.04
 import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;
 
 import com.complex.platform.access.dto.AccessGrantApplyRequest;
@@ -114,6 +115,19 @@ public class AccessController {
         return ApiResponse.ok(service.unassignUserRole(request));
     }
 
+    @GetMapping("/grant-policy")
+    public ApiResponse<AccessGrantPolicy> grantPolicy() {
+        authorizationService.requirePermission("access.grant_request.view");
+        return ApiResponse.ok(service.grantPolicy());
+    }
+
+    @PostMapping("/grant-policy")
+    public ApiResponse<AccessGrantPolicy> updateGrantPolicy(@RequestBody AccessGrantPolicy.Update request) {
+        authorizationService.requirePermission("admission.policy.manage");
+        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
+        return ApiResponse.ok(service.updateGrantPolicy(request));
+    }
+
     @PostMapping("/grant-requests")
     public ApiResponse<AccessGrantRequestRecord> applyGrant(@RequestBody @jakarta.validation.Valid AccessGrantApplyRequest request) {
         authorizationService.requirePermission("access.grant_request.apply");
@@ -123,10 +137,11 @@ public class AccessController {
 
     @GetMapping("/grant-requests")
     public ApiResponse<PageResponse<AccessGrantRequestRecord>> listGrantRequests(
-            @RequestParam(name = "window_days", required = false) Integer windowDays
+            @RequestParam(name = "window_days", required = false) Integer windowDays,
+            @RequestParam(name = "page", defaultValue = "1") int page
     ) {
         authorizationService.requirePermission("access.grant_request.view");
-        return ApiResponse.ok(service.listGrantRequests(windowDays));
+        return ApiResponse.ok(service.listGrantRequests(windowDays, page));
     }
 
     @PostMapping("/grant-requests/{id}/review")
@@ -134,8 +149,7 @@ public class AccessController {
             @PathVariable String id,
             @RequestBody @jakarta.validation.Valid AccessGrantReviewRequest request
     ) {
-        authorizationService.requirePermission("access.grant_request.review");
-        authorizationService.requirePermission("governance.supervisor_review.approve");
+        authorizationService.requirePermissions("access.grant_request.review", "governance.supervisor_review.approve");
         requireIdempotencyKey(request == null ? null : request.idempotencyKey());
         return ApiResponse.ok(service.reviewGrant(id, request));
     }
@@ -151,9 +165,10 @@ public class AccessController {
     }
 
     @GetMapping("/position-role-templates")
-    public ApiResponse<PageResponse<PositionRoleTemplate>> listPositionRoleTemplates() {
+    public ApiResponse<PageResponse<PositionRoleTemplate>> listPositionRoleTemplates(
+            @RequestParam(name = "include_archived", defaultValue = "false") boolean includeArchived) {
         authorizationService.requirePermission("access.role.assign");
-        return ApiResponse.ok(service.listPositionRoleTemplates());
+        return ApiResponse.ok(service.listPositionRoleTemplates(includeArchived));
     }
 
     @PostMapping("/position-role-templates")
@@ -173,6 +188,18 @@ public class AccessController {
         return ApiResponse.ok(service.updatePositionRoleTemplate(id, request));
     }
 
+    @PostMapping("/position-role-templates/{id}/lifecycle")
+    public ApiResponse<PositionRoleTemplate> changePositionTemplateLifecycle(
+            @PathVariable String id, @RequestBody StatusChangeRequest request) {
+        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
+        if ("approve_delete".equals(request.action()) || "reject_delete".equals(request.action())) {
+            authorizationService.requirePermissions("access.role.assign", "governance.supervisor_review.approve");
+        } else {
+            authorizationService.requirePermission("access.role.assign");
+        }
+        return ApiResponse.ok(service.changePositionTemplateLifecycle(id, request));
+    }
+
     @DeleteMapping("/position-role-templates/{id}")
     public ApiResponse<Void> deletePositionRoleTemplate(@PathVariable String id) {
         authorizationService.requirePermission("access.role.assign");
diff --git a/backend/src/main/java/com/complex/platform/access/AccessGrantPolicy.java b/backend/src/main/java/com/complex/platform/access/AccessGrantPolicy.java
new file mode 100644
index 00000000..6adf0144
--- /dev/null
+++ b/backend/src/main/java/com/complex/platform/access/AccessGrantPolicy.java
@@ -0,0 +1,44 @@
+package com.complex.platform.access;
+
+// 文件版本：V0.01
+import com.complex.platform.common.api.ApiException;
+import java.util.Map;
+import java.util.Set;
+
+/** M006-016：沿用主管复核策略表保存授权政策；查询不能覆盖政策值。 */
+public record AccessGrantPolicy(BoundedInt windowDays, BoundedInt positionThreshold,
+                                BoundedInt userThreshold, Map<String, ReviewRule> riskReviews) {
+    public AccessGrantPolicy {
+        if (windowDays == null || positionThreshold == null || userThreshold == null
+                || riskReviews == null || !riskReviews.keySet().equals(Set.of("low", "medium", "high", "critical"))
+                || riskReviews.values().stream().anyMatch(java.util.Objects::isNull)) {
+            throw ApiException.badRequest("INVALID_GRANT_POLICY", "须配置时间窗、频繁阈值及全部既有风险等级的审批规则");
+        }
+        riskReviews = Map.copyOf(riskReviews);
+    }
+
+    public record BoundedInt(int value, int min, int max) {
+        public BoundedInt {
+            if (min < 1 || max < min || value < min || value > max) {
+                throw ApiException.badRequest("GRANT_POLICY_OUT_OF_BOUNDS", "配置值须在有效的正整数上下限内");
+            }
+        }
+        int checked(int candidate) {
+            if (candidate < min || candidate > max) {
+                throw ApiException.badRequest("GRANT_POLICY_OUT_OF_BOUNDS", "查询时间窗超出后台配置的上下限");
+            }
+            return candidate;
+        }
+    }
+
+    public record ReviewRule(String approvalLevel, int requiredLevel) {
+        public ReviewRule {
+            if (!Set.of("one_level_up", "two_levels_up", "top_authority").contains(approvalLevel)
+                    || requiredLevel < 1 || requiredLevel > 3
+                    || ("top_authority".equals(approvalLevel) && requiredLevel != 3)) {
+                throw ApiException.badRequest("INVALID_GRANT_REVIEW_POLICY", "审批策略须使用既有审批层级，最高权限须为第三级");
+            }
+        }
+    }
+    public record Update(String idempotencyKey, String operatorNote, AccessGrantPolicy policy) {}
+}
diff --git a/backend/src/main/java/com/complex/platform/access/AccessRepository.java b/backend/src/main/java/com/complex/platform/access/AccessRepository.java
index 124e1df3..7826ad13 100644
--- a/backend/src/main/java/com/complex/platform/access/AccessRepository.java
+++ b/backend/src/main/java/com/complex/platform/access/AccessRepository.java
@@ -1,11 +1,17 @@
 package com.complex.platform.access;
 
+// 文件版本：V0.07
+
 import java.time.Instant;
 import java.util.List;
 import java.util.Optional;
 
 public interface AccessRepository {
 
+    default void lockGrantAuthorizationFacts() {
+        throw new UnsupportedOperationException("授权仓储未实现事务内定义锁");
+    }
+
     List<PermissionDefinition> listPermissions();
 
     List<PermissionTemplate> listTemplates();
@@ -25,6 +31,13 @@ public interface AccessRepository {
         return listTemplates().stream().filter(t -> t.id().equals(id)).findFirst();
     }
 
+    default PermissionTemplate createTemplate(PermissionTemplate template, List<PermissionTemplateItem> items) {
+        if (findTemplateById(template.id()).isPresent()) {
+            throw new IllegalStateException("权限模板已存在");
+        }
+        return saveTemplate(template, items);
+    }
+
     PermissionTemplate saveTemplate(PermissionTemplate template, List<PermissionTemplateItem> items);
 
     PermissionTemplate copyTemplate(String sourceTemplateId, PermissionTemplate copy);
@@ -112,6 +125,14 @@ public interface AccessRepository {
         throw new UnsupportedOperationException("savePositionRoleTemplate not supported by this repository");
     }
 
+    default void savePositionTemplateLifecycle(PositionRoleTemplate mapping, String actorId) {
+        throw new UnsupportedOperationException("savePositionTemplateLifecycle not supported by this repository");
+    }
+
+    default long countPositionTemplateUsage(String id) {
+        throw new UnsupportedOperationException("countPositionTemplateUsage not supported by this repository");
+    }
+
     default void deletePositionRoleTemplate(String id) {
         throw new UnsupportedOperationException("deletePositionRoleTemplate not supported by this repository");
     }
@@ -124,6 +145,12 @@ public interface AccessRepository {
         return Optional.empty();
     }
 
+    /** 在当前岗位行上替换派生授权；独立 user_roles 与模板应用不受影响。 */
+    default void replacePositionGrants(String userId, String roleId, String mappingId, List<String> applicationIds,
+                                      String actorId, Instant changedAt) {
+        throw new UnsupportedOperationException("replacePositionGrants not supported by this repository");
+    }
+
     List<PermissionTemplateApplication> applyTemplate(
             String templateId,
             List<String> targetUserIds,
@@ -149,6 +176,30 @@ public interface AccessRepository {
         return Optional.empty();
     }
 
+    /** 保存申请时的完整授权事实指纹，决策更新不得覆盖。 */
+    default void saveGrantFingerprint(String id, String fingerprint) {
+        throw new UnsupportedOperationException("saveGrantFingerprint not supported by this repository");
+    }
+
+    default Optional<String> findGrantFingerprint(String id) {
+        return Optional.empty();
+    }
+
+    default com.complex.platform.common.api.PageResponse<AccessGrantRequestRecord> pageGrantRequests(
+            Instant cutoff, String organizationId, String merchantId, int page, int size) {
+        List<AccessGrantRequestRecord> visible = listGrantRequests(cutoff).stream().filter(request ->
+                (organizationId == null || findUserById(request.targetUserId())
+                        .map(user -> organizationId.equals(user.organizationId())).orElse(false))
+                && (merchantId == null || merchantId.equals(request.merchantId()))).toList();
+        return com.complex.platform.common.api.PageResponse.of(visible, page, size);
+    }
+
+    default Optional<String> findGrantPolicy() { return Optional.empty(); }
+
+    default void saveGrantPolicy(String policyJson) {
+        throw new UnsupportedOperationException("saveGrantPolicy not supported by this repository");
+    }
+
     default List<AccessGrantRequestRecord> listGrantRequests(Instant createdAtFrom) {
         return List.of();
     }
diff --git a/backend/src/main/java/com/complex/platform/access/AccessService.java b/backend/src/main/java/com/complex/platform/access/AccessService.java
index e1547225..37ddec45 100644
--- a/backend/src/main/java/com/complex/platform/access/AccessService.java
+++ b/backend/src/main/java/com/complex/platform/access/AccessService.java
@@ -1,5 +1,7 @@
 package com.complex.platform.access;
 
+// 文件版本：V0.12
+
 import com.complex.platform.access.dto.AccessGrantApplyRequest;
 import com.complex.platform.access.dto.AccessGrantReviewRequest;
 import com.complex.platform.access.dto.AccessUserRequest;
@@ -19,15 +21,11 @@ import com.complex.platform.evidence.AuditEventService;
 import com.complex.platform.governance.review.SupervisorReviewApprovalRequest;
 import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
 import com.complex.platform.governance.review.SupervisorReviewService;
-import java.nio.charset.StandardCharsets;
-import java.security.MessageDigest;
-import java.security.NoSuchAlgorithmException;
 import java.time.Duration;
 import java.time.Instant;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Comparator;
-import java.util.HexFormat;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.LinkedHashMap;
@@ -38,7 +36,6 @@ import java.util.Set;
 import java.util.UUID;
 import org.springframework.beans.factory.ObjectProvider;
 import org.springframework.beans.factory.annotation.Autowired;
-import org.springframework.beans.factory.annotation.Value;
 import org.springframework.stereotype.Service;
 import org.springframework.transaction.annotation.Transactional;
 import static com.complex.platform.common.Strings.trimToNull;
@@ -48,7 +45,6 @@ import static com.complex.platform.common.Strings.requireText;
 public class AccessService {
 
     private static final int DEFAULT_PAGE = 1;
-    private static final int DEFAULT_PAGE_SIZE = 50;
     private static final Set<String> ALLOWED_ACCOUNT_TYPES = Set.of(
             "staff", "merchant", "merchant_admin", "supervisor", "sales", "outsourced_vendor");
     private static final String REQUEST_USER_ROLE = "user_role";
@@ -62,8 +58,6 @@ public class AccessService {
     private final AccessCredentialProvisioner credentialProvisioner;
     private final SupervisorReviewService supervisorReviewService;
     private final boolean grantApprovalEnabled;
-    private final int summaryPositionThreshold;
-    private final int summaryUserThreshold;
 
     @Autowired
     public AccessService(
@@ -72,18 +66,16 @@ public class AccessService {
             AuditEventService auditEventService,
             AuthorizationService authorizationService,
             ObjectProvider<AccessCredentialProvisioner> credentialProvisioner,
-            ObjectProvider<SupervisorReviewService> supervisorReviewService,
-            @Value("${access.grant_approval.summary.position_threshold:3}") int summaryPositionThreshold,
-            @Value("${access.grant_approval.summary.user_threshold:3}") int summaryUserThreshold
+            ObjectProvider<SupervisorReviewService> supervisorReviewService
     ) {
         this(repository, idempotencyService, auditEventService, authorizationService,
-                credentialProvisioner.getIfAvailable(AccessCredentialProvisioner::noop),
+                credentialProvisioner.getIfAvailable(),
                 // 治理地图「四类高危权限授出前须先审批才生效」：生产不给全局逃生口——
                 // 原 @Value("${access.grant_approval.enabled:true}") 一票否决四个调用点，关闭时无审计、
                 // 无启动告警，静默失效（219 条 requires_approval=true 全押在它身上）。
                 // 该开关唯一实际用途是让测试少走审批，故只保留包级构造器的入参。
                 supervisorReviewService.getIfAvailable(), true,
-                summaryPositionThreshold, summaryUserThreshold);
+                0, 0);
     }
 
     AccessService(
@@ -101,16 +93,14 @@ public class AccessService {
         this.idempotencyService = idempotencyService;
         this.auditEventService = auditEventService;
         this.authorizationService = authorizationService;
-        this.credentialProvisioner = credentialProvisioner == null ? AccessCredentialProvisioner.noop() : credentialProvisioner;
+        this.credentialProvisioner = credentialProvisioner;
         this.supervisorReviewService = supervisorReviewService;
         this.grantApprovalEnabled = grantApprovalEnabled;
-        this.summaryPositionThreshold = Math.max(1, summaryPositionThreshold);
-        this.summaryUserThreshold = Math.max(1, summaryUserThreshold);
     }
 
     public PageResponse<PermissionTemplate> listPermissionTemplates() {
         AccessActor actor = authorizationService.currentActor();
-        String orgId = authorizationService.isPlatformAdmin(actor) ? null : actor.organizationId();
+        String orgId = authorizationService.isPlatformAdmin(actor) ? null : authorizationService.requireProjectScope(actor);
         List<PermissionTemplate> templates = repository.listTemplates(orgId);
         auditEventService.recordSuccess(
                 "permission_template.viewed",
@@ -126,7 +116,7 @@ public class AccessService {
 
     public PermissionTemplate createPermissionTemplate(PermissionTemplateRequest request) {
         AccessActor actor = authorizationService.currentActor();
-        String ownerScopeId = authorizationService.isPlatformAdmin(actor) ? null : actor.organizationId();
+        String ownerScopeId = authorizationService.isPlatformAdmin(actor) ? null : authorizationService.requireProjectScope(actor);
         return executeTemplateWrite(
                 "createPermissionTemplate",
                 "permission_template.changed",
@@ -135,8 +125,8 @@ public class AccessService {
                 request,
                 () -> {
                     requireKnownPermissions(request.permissionCodes());
-                    PermissionTemplate template = templateFromRequest("tpl_" + slug(request.idempotencyKey()), request, null, ownerScopeId);
-                    return repository.saveTemplate(template, items(template.id(), request.permissionCodes(), request.dataScope()));
+                    PermissionTemplate template = templateFromRequest("tpl_" + UUID.randomUUID(), request, null, ownerScopeId);
+                    return repository.createTemplate(template, items(template.id(), request.permissionCodes(), request.dataScope()));
                 }
         );
     }
@@ -150,19 +140,35 @@ public class AccessService {
                 "access.permission_template.manage",
                 request,
                 () -> {
-                    if (!authorizationService.isPlatformAdmin(actor)) {
-                        PermissionTemplate existing = repository.findTemplateById(id)
-                                .orElseThrow(() -> ApiException.notFound("TEMPLATE_NOT_FOUND", "模板不存在：" + id));
-                        if (existing.ownerScopeId() == null || !actor.organizationId().equals(existing.ownerScopeId())) {
-                            throw ApiException.forbidden("CROSS_ORG_FORBIDDEN", "不能修改其他组织的模板");
-                        }
+                    PermissionTemplate existing = repository.findTemplateById(id)
+                            .orElseThrow(() -> ApiException.notFound("TEMPLATE_NOT_FOUND", "模板不存在：" + id));
+                    if (!authorizationService.isPlatformAdmin(actor)
+                            && !authorizationService.requireProjectScope(actor).equals(existing.ownerScopeId())) {
+                        throw ApiException.forbidden("CROSS_ORG_FORBIDDEN", "不能修改其他组织的模板");
                     }
                     requireKnownPermissions(request.permissionCodes());
-                    // Preserve the existing owner when updating
-                    String ownerScopeId = authorizationService.isPlatformAdmin(actor)
-                            ? null
-                            : actor.organizationId();
-                    PermissionTemplate template = templateFromRequest(id, request, request.sourceTemplateId(), ownerScopeId);
+                    List<PermissionTemplateItem> previousItems = repository.findTemplateItems(id);
+                    boolean widensWindow = (existing.validFrom() != null
+                            && (request.validFrom() == null || request.validFrom().isBefore(existing.validFrom())))
+                            || (existing.validUntil() != null
+                            && (request.validUntil() == null || request.validUntil().isAfter(existing.validUntil())));
+                    boolean changesScope = previousItems.stream().anyMatch(item ->
+                            safeList(request.permissionCodes()).contains(item.permissionCode())
+                                    && !java.util.Objects.equals(item.dataScope(), trimToDefault(request.dataScope(), "project")));
+                    if (widensWindow || (existing.singleUse() && !Boolean.TRUE.equals(request.singleUse()))
+                            || changesScope || previousItems.stream().anyMatch(item -> item.effect() == AccessRepository.Effect.DENY)
+                            || !"active".equals(existing.status())) {
+                        throw ApiException.conflict("TEMPLATE_REAUTHORIZATION_REQUIRED",
+                                "有效期、一次性限制、数据范围或禁止规则变化须复制为新模板后重新授权");
+                    }
+                    Set<String> previousGrants = previousItems.stream()
+                            .filter(item -> item.effect() == AccessRepository.Effect.GRANT)
+                            .map(PermissionTemplateItem::permissionCode).collect(java.util.stream.Collectors.toSet());
+                    List<String> added = safeList(request.permissionCodes()).stream()
+                            .filter(code -> !previousGrants.contains(code)).toList();
+                    requireActorCanGrant(added);
+                    requireGrantApprovalIfNeeded(added);
+                    PermissionTemplate template = templateFromRequest(id, request, existing.sourceTemplateId(), existing.ownerScopeId());
                     return repository.saveTemplate(template, items(template.id(), request.permissionCodes(), request.dataScope()));
                 }
         );
@@ -184,8 +190,8 @@ public class AccessService {
                             throw ApiException.forbidden("CROSS_ORG_FORBIDDEN", "不能复制其他组织的模板");
                         }
                     }
-                    String ownerScopeId = authorizationService.isPlatformAdmin(actor) ? null : actor.organizationId();
-                    return repository.copyTemplate(id, templateFromRequest("tpl_" + slug(request.idempotencyKey()), request, id, ownerScopeId));
+                    String ownerScopeId = authorizationService.isPlatformAdmin(actor) ? null : authorizationService.requireProjectScope(actor);
+                    return repository.copyTemplate(id, templateFromRequest("tpl_" + UUID.randomUUID(), request, id, ownerScopeId));
                 }
         );
     }
@@ -198,16 +204,11 @@ public class AccessService {
                     request.idempotencyKey(),
                     request,
                     () -> {
-                        if (!authorizationService.isPlatformAdmin(actor)) {
-                            for (String userId : safeList(request.targetUserIds())) {
-                                AccessUser targetUser = repository.findUserById(userId)
-                                        .orElseThrow(() -> ApiException.forbidden("CROSS_ORG_FORBIDDEN",
-                                                "目标用户不在当前组织内：" + userId));
-                                if (!actor.organizationId().equals(targetUser.organizationId())) {
-                                    throw ApiException.forbidden("CROSS_ORG_FORBIDDEN",
-                                            "不能将模板应用到其他组织的用户：" + userId);
-                                }
-                            }
+                        if (safeList(request.targetUserIds()).isEmpty()) {
+                            throw ApiException.badRequest("VALIDATION_FAILED", "必须选择授权目标");
+                        }
+                        for (String userId : request.targetUserIds()) {
+                            requireSameOrganization(actor, requireTargetUser(requireText(userId, "user_id")));
                         }
                         PermissionTemplate template = repository.findTemplateById(id)
                                 .orElseThrow(() -> ApiException.notFound("PERMISSION_TEMPLATE_NOT_FOUND", "权限模板不存在：" + id));
@@ -282,7 +283,7 @@ public class AccessService {
 
     public PageResponse<RoleRecord> listRoles() {
         AccessActor actor = authorizationService.currentActor();
-        String orgId = authorizationService.isPlatformAdmin(actor) ? null : actor.organizationId();
+        String orgId = authorizationService.isPlatformAdmin(actor) ? null : authorizationService.requireProjectScope(actor);
         List<RoleRecord> roles = repository.listRoles(orgId);
         auditEventService.recordSuccess(
                 "role.viewed",
@@ -298,7 +299,7 @@ public class AccessService {
 
     public RoleRecord createRole(RoleRequest request) {
         AccessActor actor = authorizationService.currentActor();
-        String orgId = authorizationService.isPlatformAdmin(actor) ? null : actor.organizationId();
+        String orgId = authorizationService.isPlatformAdmin(actor) ? null : authorizationService.requireProjectScope(actor);
         try {
             return idempotencyService.execute(
                     "createRole",
@@ -309,7 +310,7 @@ public class AccessService {
                         // P0-1：角色不得包含超出创建人自身有效权限的权限码（提权防护）。
                         requireActorCanGrant(request.permissionCodes());
                         RoleRecord role = new RoleRecord(
-                                "role_" + slug(request.roleName()),
+                                "role_" + UUID.randomUUID(),
                                 trimToDefault(request.roleName(), "role"),
                                 request.description(),
                                 trimToDefault(request.dataScope(), "project"),
@@ -397,9 +398,14 @@ public class AccessService {
     }
 
     public PageResponse<PositionRoleTemplate> listPositionRoleTemplates() {
+        return listPositionRoleTemplates(false);
+    }
+
+    public PageResponse<PositionRoleTemplate> listPositionRoleTemplates(boolean includeArchived) {
         AccessActor actor = authorizationService.currentActor();
         List<PositionRoleTemplate> mappings = repository.listPositionRoleTemplates(
-                authorizationService.isPlatformAdmin(actor) ? null : actor.organizationId());
+                authorizationService.isPlatformAdmin(actor) ? null : authorizationService.requireProjectScope(actor)).stream()
+                .filter(mapping -> includeArchived || (mapping.archivedAt() == null && mapping.deletedAt() == null)).toList();
         auditEventService.recordSuccess(
                 "access.position_role_template.viewed",
                 "access.role.assign",
@@ -420,7 +426,7 @@ public class AccessService {
                     r.idempotencyKey(),
                     r,
                     () -> savePositionRoleTemplate(
-                            "prt_" + slug(requireText(r.positionCode(), "position_code") + "_" + requireText(r.staffLevelId(), "staff_level_id")),
+                            "prt_" + UUID.randomUUID(),
                             r
                     ),
                     PositionRoleTemplate.class
@@ -464,21 +470,71 @@ public class AccessService {
     }
 
     public void deletePositionRoleTemplate(String id) {
-        AccessActor actor = authorizationService.currentActor();
-        requirePlatformAdmin(actor);
-        PositionRoleTemplate existing = repository.findPositionRoleTemplateById(id)
-                .orElseThrow(() -> ApiException.notFound("POSITION_ROLE_TEMPLATE_NOT_FOUND",
-                        "岗位角色模板不存在：" + id));
-        repository.deletePositionRoleTemplate(existing.id());
-        auditEventService.recordSuccess(
-                "access.position_role_template.deleted",
-                "access.role.assign",
-                "position_role_template",
-                existing.id(),
-                null,
-                null,
-                Map.of("position_code", existing.positionCode(), "staff_level_id", existing.staffLevelId())
-        );
+        throw ApiException.conflict("ARCHIVE_APPROVAL_REQUIRED", "岗位模板须先存档，填写理由申请删除，经最高权限审批后软删除");
+    }
+
+    @Transactional
+    public PositionRoleTemplate changePositionTemplateLifecycle(String id, StatusChangeRequest request) {
+        requireRequest(request);
+        requirePlatformAdmin(authorizationService.currentActor());
+        String action = requireText(request.action(), "action");
+        String reason = requireText(request.reason(), "reason");
+        return idempotencyService.execute("positionTemplateLifecycle:" + id, request.idempotencyKey(), request, () -> {
+            PositionRoleTemplate current = repository.findPositionRoleTemplateById(id)
+                    .orElseThrow(() -> ApiException.notFound("POSITION_ROLE_TEMPLATE_NOT_FOUND", "岗位模板不存在"));
+            Instant now = Instant.now();
+            PositionRoleTemplate updated;
+            switch (action) {
+                case "archive" -> {
+                    if (current.archivedAt() != null || current.deletedAt() != null) throw ApiException.conflict("INVALID_STATE", "模板已存档");
+                    updated = current.withLifecycle(now, null, null, null, now);
+                }
+                case "restore" -> {
+                    if (current.archivedAt() == null) throw ApiException.conflict("INVALID_STATE", "模板未存档");
+                    if (current.deleteReviewId() != null && current.deletedAt() == null) throw ApiException.conflict("DELETE_REVIEW_PENDING", "须先处理删除复核");
+                    updated = current.withLifecycle(null, null, null, null, now);
+                }
+                case "request_delete" -> {
+                    if (current.archivedAt() == null || current.deletedAt() != null || current.deleteReviewId() != null) {
+                        throw ApiException.conflict("INVALID_STATE", "须先存档且没有待处理删除申请");
+                    }
+                    requirePositionTemplateUnused(id);
+                    if (authorizationService.isMasterAuthority(authorizationService.currentActor())) {
+                        updated = current.withLifecycle(current.archivedAt(), now, null, reason, now);
+                    } else {
+                        var review = requireSupervisorReviewService().blockForAdmission("position_role_template_delete",
+                                "access.position_role_template.delete", "position_role_template", id, "top_authority", 3);
+                        updated = current.withLifecycle(current.archivedAt(), null, review.reviewId(), reason, now);
+                    }
+                }
+                case "approve_delete", "reject_delete" -> {
+                    if (current.archivedAt() == null || current.deletedAt() != null || current.deleteReviewId() == null) {
+                        throw ApiException.conflict("INVALID_STATE", "模板没有待处理删除申请");
+                    }
+                    if (!authorizationService.isMasterAuthority(authorizationService.currentActor())) {
+                        throw ApiException.forbidden("MASTER_AUTHORITY_REQUIRED", "删除配置须由 USB 最高权限审批");
+                    }
+                    boolean approve = "approve_delete".equals(action);
+                    if (approve) requirePositionTemplateUnused(id);
+                    requireSupervisorReviewService().approve(current.deleteReviewId(), new SupervisorReviewApprovalRequest(
+                            request.idempotencyKey(), reason, request.evidenceAttachmentIds(), null,
+                            approve ? "approved" : "rejected", reason));
+                    updated = current.withLifecycle(current.archivedAt(), approve ? now : null,
+                            approve ? current.deleteReviewId() : null, current.deleteReason(), now);
+                }
+                default -> throw ApiException.badRequest("INVALID_ACTION", "不支持的岗位模板生命周期操作");
+            }
+            repository.savePositionTemplateLifecycle(updated, authorizationService.currentActor().id());
+            auditEventService.recordSuccess("access.position_role_template." + action, "access.role.assign",
+                    "position_role_template", id, null, request.idempotencyKey(), Map.of("reason", reason));
+            return updated;
+        }, PositionRoleTemplate.class);
+    }
+
+    private void requirePositionTemplateUnused(String id) {
+        if (repository.countPositionTemplateUsage(id) != 0) {
+            throw ApiException.conflict("POSITION_TEMPLATE_IN_USE", "岗位模板仍被员工岗位使用，不能删除");
+        }
     }
 
     @Transactional
@@ -508,7 +564,7 @@ public class AccessService {
 
     /**
      * P0-1 提权防护：发证/授权动作不得授予超出「授权人自身有效权限」的权限码。
-     * 平台管理员（wildcard）可授予任意权限；其余主体所授权限码必须 ⊆ 自身有效授予集（denies 已扣减）。
+     * USB 最高权限独立识别；日常管理员及其他主体所授权限码必须 ⊆ 自身有效授予集（denies 已扣减）。
      */
     private void requireActorCanGrant(List<String> permissionCodes) {
         EffectivePermissions self = authorizationService.effectivePermissions();
@@ -556,17 +612,9 @@ public class AccessService {
                     request.idempotencyKey(),
                     request,
                     () -> {
-                        if (!authorizationService.isPlatformAdmin(actor)) {
-                            TemporaryGrant grant = repository.findTemporaryGrantById(id)
-                                    .orElseThrow(() -> ApiException.notFound("TEMPORARY_GRANT_NOT_FOUND", "临时授权不存在：" + id));
-                            AccessUser targetUser = repository.findUserById(grant.targetUserId())
-                                    .orElseThrow(() -> ApiException.forbidden("CROSS_ORG_FORBIDDEN",
-                                            "授权目标用户不在当前组织内"));
-                            if (!actor.organizationId().equals(targetUser.organizationId())) {
-                                throw ApiException.forbidden("CROSS_ORG_FORBIDDEN",
-                                        "不能撤销其他组织的授权");
-                            }
-                        }
+                        TemporaryGrant grant = repository.findTemporaryGrantById(id)
+                                .orElseThrow(() -> ApiException.notFound("TEMPORARY_GRANT_NOT_FOUND", "临时授权不存在"));
+                        requireSameOrganization(actor, requireTargetUser(grant.targetUserId()));
                         TemporaryGrant revoked = repository.revokeTemporaryGrant(id, "access-api", Instant.now());
                         auditEventService.recordSuccess(
                                 "permission_template.revoked",
@@ -604,6 +652,7 @@ public class AccessService {
                     r.idempotencyKey(),
                     r,
                     () -> {
+                        repository.lockGrantAuthorizationFacts();
                         AccessGrantRequestRecord draft = buildGrantRequest(r);
                         List<PermissionDefinition> approvalRequired = approvalRequiredPermissions(draft.permissionCodes());
                         if (approvalRequired.isEmpty()) {
@@ -622,6 +671,7 @@ public class AccessService {
                                 policy.requiredLevel()
                         );
                         AccessGrantRequestRecord saved = repository.saveGrantRequest(withReview(draft, review.reviewId()));
+                        repository.saveGrantFingerprint(saved.id(), authorizationFingerprint(saved));
                         auditEventService.recordSuccess(
                                 "access.grant_request.applied",
                                 "access.grant_request.apply",
@@ -658,11 +708,23 @@ public class AccessService {
                     r.idempotencyKey(),
                     r,
                     () -> {
+                        repository.lockGrantAuthorizationFacts();
                         AccessGrantRequestRecord pending = repository.findGrantRequestById(requireText(id, "id"))
                                 .filter(grant -> "pending".equals(grant.status()))
                                 .orElseThrow(() -> ApiException.notFound("ACCESS_GRANT_REQUEST_NOT_FOUND",
                                         "授权申请不存在或已不在待审批状态：" + id));
+                        AccessActor reviewer = authorizationService.currentActor();
+                        if (reviewer.id().equals(pending.targetUserId())) {
+                            throw ApiException.forbidden("BENEFICIARY_SELF_REVIEW_FORBIDDEN", "授权受益人不能审批自己的授权");
+                        }
+                        if (reviewer.id().equals(pending.requestedBy())) {
+                            throw ApiException.forbidden("SUPERVISOR_SELF_REVIEW_FORBIDDEN", "申请人不能审批自己的授权");
+                        }
                         String decision = normalizeDecision(r.decision());
+                        if ("approve".equals(decision) && !repository.findGrantFingerprint(pending.id())
+                                .filter(saved -> saved.equals(authorizationFingerprint(pending))).isPresent()) {
+                            throw ApiException.conflict("GRANT_SNAPSHOT_CHANGED", "授权内容或归属已变化，请重新申请");
+                        }
                         SupervisorReviewDecisionRecord review = requireSupervisorReviewService().approve(
                                 pending.reviewId(),
                                 new SupervisorReviewApprovalRequest(
@@ -710,9 +772,24 @@ public class AccessService {
         }
     }
 
+    public PageResponse<AccessGrantRequestRecord> listGrantRequests(Integer windowDays, int pageNumber) {
+        if (pageNumber < 1) throw ApiException.badRequest("INVALID_PAGE", "页码须大于零");
+        AccessGrantPolicy policy = grantPolicy();
+        int days = windowDays == null ? policy.windowDays().value() : policy.windowDays().checked(windowDays);
+        AccessActor actor = authorizationService.currentActor();
+        String org = authorizationService.isPlatformAdmin(actor) ? null : authorizationService.requireProjectScope(actor);
+        String merchant = authorizationService.isPlatformAdmin(actor) ? null : trimToNull(actor.merchantId());
+        PageResponse<AccessGrantRequestRecord> result = repository.pageGrantRequests(
+                Instant.now().minus(Duration.ofDays(days)), org, merchant, pageNumber, 50);
+        auditEventService.recordSuccess("access.grant_request.viewed", "access.grant_request.view",
+                "access_grant_request", "all", null, null, Map.of("count", result.items().size(), "page", pageNumber));
+        return result;
+    }
+
     public PageResponse<AccessGrantRequestRecord> listGrantRequests(Integer windowDays) {
-        int days = windowDays == null ? 0 : Math.max(1, windowDays);
-        Instant cutoff = days == 0 ? null : Instant.now().minus(Duration.ofDays(days));
+        AccessGrantPolicy policy = grantPolicy();
+        int days = windowDays == null ? policy.windowDays().value() : policy.windowDays().checked(windowDays);
+        Instant cutoff = Instant.now().minus(Duration.ofDays(days));
         List<AccessGrantRequestRecord> requests = visibleGrantRequests(cutoff);
         auditEventService.recordSuccess(
                 "access.grant_request.viewed",
@@ -727,9 +804,14 @@ public class AccessService {
     }
 
     public AccessGrantSummary grantSummary(Integer windowDays, Integer positionThreshold, Integer userThreshold) {
-        int days = windowDays == null ? 30 : Math.max(1, windowDays);
-        int perPositionThreshold = positionThreshold == null ? summaryPositionThreshold : Math.max(1, positionThreshold);
-        int perUserThreshold = userThreshold == null ? summaryUserThreshold : Math.max(1, userThreshold);
+        AccessGrantPolicy policy = grantPolicy();
+        if (positionThreshold != null || userThreshold != null
+                || (windowDays != null && windowDays != policy.windowDays().value())) {
+            throw ApiException.badRequest("QUERY_POLICY_OVERRIDE_FORBIDDEN", "统计时间窗和频繁阈值由后台授权政策配置，查询不能覆盖");
+        }
+        int days = policy.windowDays().value();
+        int perPositionThreshold = policy.positionThreshold().value();
+        int perUserThreshold = policy.userThreshold().value();
         Instant cutoff = Instant.now().minus(Duration.ofDays(days));
         List<AccessGrantRequestRecord> temporaryRequests = visibleGrantRequests(cutoff).stream()
                 .filter(request -> REQUEST_TEMPORARY_PERMISSION.equals(request.requestType()))
@@ -761,7 +843,7 @@ public class AccessService {
                         entry.getKey().staffLevelId(),
                         entry.getKey().permissionCode(),
                         entry.getValue(),
-                        "frequent temporary grants; consider fixed permission"
+                        "频繁申请临时授权，建议评估配置固定权限"
                 ))
                 .toList();
         List<AccessGrantUserSummary> userAnomalies = byUser.entrySet().stream()
@@ -770,7 +852,7 @@ public class AccessService {
                 .map(entry -> new AccessGrantUserSummary(
                         entry.getKey(),
                         entry.getValue(),
-                        "frequent temporary permission requests; review abnormal pattern"
+                        "临时权限申请频繁，请核查异常情况"
                 ))
                 .toList();
         return new AccessGrantSummary(positionRecommendations, userAnomalies);
@@ -812,7 +894,7 @@ public class AccessService {
         } else if (!isBlank(actor.merchantId())) {
             users = repository.listUsersByMerchant(actor.merchantId());
         } else {
-            users = repository.listUsers(actor.organizationId());
+            users = repository.listUsers(authorizationService.requireProjectScope(actor));
         }
         auditEventService.recordSuccess(
                 "access.user.viewed",
@@ -842,8 +924,7 @@ public class AccessService {
                         }
                         String organizationId = requireText(request.organizationId(), "organization_id");
                         if (!authorizationService.isPlatformAdmin(actor)
-                                && !isBlank(actor.organizationId())
-                                && !actor.organizationId().equals(organizationId)) {
+                                && !authorizationService.requireProjectScope(actor).equals(organizationId)) {
                             throw ApiException.forbidden("CROSS_ORG_FORBIDDEN",
                                     "不能在其他组织下创建用户：" + organizationId);
                         }
@@ -860,16 +941,25 @@ public class AccessService {
                         if (requiresMerchantScope(accountType) && merchantId == null) {
                             throw ApiException.badRequest("VALIDATION_FAILED", "商户级员工账号必须指定商户");
                         }
+                        if (!"outsourced_vendor".equals(accountType)) {
+                            throw ApiException.conflict("STAFF_REGISTRATION_REQUIRED",
+                                    "员工须先提交注册申请，由对应公司审批后生成账号和员工自助权限");
+                        }
+                        if (credentialProvisioner == null) {
+                            throw ApiException.conflict("CREDENTIAL_PROVIDER_UNAVAILABLE", "登录凭证服务未配置，不能创建账号");
+                        }
+                        requireText(request.password(), "password");
+                        String accountStatus = requireAccountStatus(request.status());
                         Instant now = Instant.now();
                         AccessUser user = new AccessUser(
-                                "usr_" + slug(username),
+                                "usr_" + UUID.randomUUID(),
                                 username,
                                 accountType,
                                 organizationId,
                                 merchantId,
-                                trimToDefault(request.status(), "active"),
+                                accountStatus,
                                 now,
-                                hashNullable(request.phone()),
+                                null,
                                 "manual_admin",
                                 trimToNull(request.consentReference()),
                                 safeList(request.evidenceAttachmentIds()),
@@ -908,18 +998,12 @@ public class AccessService {
         return value == null || value.isBlank();
     }
 
-    private String hashNullable(String value) {
-        String trimmed = trimToNull(value);
-        if (trimmed == null) {
-            return null;
-        }
-        try {
-            MessageDigest digest = MessageDigest.getInstance("SHA-256");
-            byte[] hash = digest.digest(trimmed.getBytes(StandardCharsets.UTF_8));
-            return HexFormat.of().formatHex(hash);
-        } catch (NoSuchAlgorithmException exception) {
-            throw new IllegalStateException("SHA-256 algorithm is required", exception);
+    private String requireAccountStatus(String status) {
+        String actual = trimToDefault(status, "active");
+        if (!Set.of("active", "invited", "disabled").contains(actual)) {
+            throw ApiException.badRequest("INVALID_ACCOUNT_STATUS", "账号状态必须为启用、待激活或禁用");
         }
+        return actual;
     }
 
     private Map<String, Object> accessUserAuditMetadata(AccessUser user, boolean credentialProvisioned) {
@@ -966,11 +1050,7 @@ public class AccessService {
                         }
                         AccessUser targetUser = repository.findUserById(userId)
                                 .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "账号不存在：" + userId));
-                        if (!authorizationService.isPlatformAdmin(actor)
-                                && !actor.organizationId().equals(targetUser.organizationId())) {
-                            throw ApiException.forbidden("CROSS_ORG_FORBIDDEN",
-                                    "不能为其他组织的用户分配员工职级：" + userId);
-                        }
+                        requireSameOrganization(actor, targetUser);
                         if (repository.findStaffLevelById(request.staffLevelId()).isEmpty()) {
                             throw ApiException.badRequest("UNKNOWN_STAFF_LEVEL",
                                     "无法识别的员工职级：" + request.staffLevelId());
@@ -1069,7 +1149,6 @@ public class AccessService {
                 context.actor().id(),
                 now
         ));
-        repository.assignUserRole(context.targetUser().id(), context.role().id());
         List<PermissionTemplateApplication> applications = repository.applyTemplate(
                 context.template().id(),
                 List.of(context.targetUser().id()),
@@ -1078,6 +1157,8 @@ public class AccessService {
                 context.template().singleUse(),
                 context.actor().id()
         );
+        repository.replacePositionGrants(context.targetUser().id(), context.role().id(), context.mappingId(),
+                applications.stream().map(PermissionTemplateApplication::id).toList(), context.actor().id(), now);
         auditEventService.recordSuccess(
                 "access.user_position.assigned",
                 "access.role.assign",
@@ -1107,17 +1188,13 @@ public class AccessService {
         if (request.validUntil() == null) {
             throw ApiException.badRequest("VALIDATION_FAILED", "valid_until is required");
         }
-        // P0 A-01：临时授权必须校验目标用户租户归属，禁止跨组织/向平台用户越权授权
-        // （与 applyPermissionTemplate 同范式，fail-closed）。
-        if (!authorizationService.isPlatformAdmin(actor)) {
-            AccessUser targetUser = repository.findUserById(request.targetUserId())
-                    .orElseThrow(() -> ApiException.forbidden("CROSS_ORG_FORBIDDEN",
-                            "目标用户不在当前组织内：" + request.targetUserId()));
-            if (actor.organizationId() == null
-                    || !actor.organizationId().equals(targetUser.organizationId())) {
-                throw ApiException.forbidden("CROSS_ORG_FORBIDDEN",
-                        "不能向其他组织的用户授权：" + request.targetUserId());
-            }
+        requireSameOrganization(actor, requireTargetUser(requireText(request.targetUserId(), "target_user_id")));
+        if (safeList(request.permissionCodes()).isEmpty()) {
+            throw ApiException.badRequest("VALIDATION_FAILED", "必须填写权限编码列表");
+        }
+        Instant validFrom = request.validFrom() == null ? Instant.now() : request.validFrom();
+        if (!validFrom.isBefore(request.validUntil())) {
+            throw ApiException.badRequest("VALIDATION_FAILED", "授权失效时间必须晚于生效时间");
         }
         // P0-1：临时授权不得授予超出授权人自身有效权限的权限码（提权防护）。
         requireActorCanGrant(request.permissionCodes());
@@ -1128,7 +1205,7 @@ public class AccessService {
                 UUID.randomUUID().toString(),
                 request.targetUserId(),
                 safeList(request.permissionCodes()),
-                request.validFrom() == null ? Instant.now() : request.validFrom(),
+                validFrom,
                 request.validUntil(),
                 Boolean.TRUE.equals(request.singleUse()),
                 false,
@@ -1171,7 +1248,7 @@ public class AccessService {
         ), false);
         Instant now = Instant.now();
         return new AccessGrantRequestRecord(
-                "agr_" + slug(request.idempotencyKey()),
+                "agr_" + UUID.randomUUID(),
                 REQUEST_USER_ROLE,
                 context.targetUser().id(),
                 context.role().id(),
@@ -1211,7 +1288,7 @@ public class AccessService {
         ), false);
         Instant now = Instant.now();
         return new AccessGrantRequestRecord(
-                "agr_" + slug(request.idempotencyKey()),
+                "agr_" + UUID.randomUUID(),
                 REQUEST_USER_POSITION,
                 context.targetUser().id(),
                 context.role().id(),
@@ -1251,13 +1328,16 @@ public class AccessService {
         if (expiresAt == null) {
             throw ApiException.badRequest("VALIDATION_FAILED", "expires_at is required");
         }
+        if (request.validFrom() != null && !expiresAt.isAfter(request.validFrom())) {
+            throw ApiException.badRequest("VALIDATION_FAILED", "授权结束时间必须晚于开始时间");
+        }
         UserPositionAssignment position = repository.findUserPositionAssignment(targetUser.id()).orElse(null);
         String staffLevelId = position == null
                 ? repository.findStaffLevelAssignment(targetUser.id()).map(StaffLevelAssignment::staffLevelId).orElse(null)
                 : position.staffLevelId();
         Instant now = Instant.now();
         return new AccessGrantRequestRecord(
-                "agr_" + slug(request.idempotencyKey()),
+                "agr_" + UUID.randomUUID(),
                 REQUEST_TEMPORARY_PERMISSION,
                 targetUser.id(),
                 null,
@@ -1284,6 +1364,50 @@ public class AccessService {
         );
     }
 
+    private String authorizationFingerprint(AccessGrantRequestRecord request) {
+        AccessUser target = requireTargetUser(request.targetUserId());
+        List<Object> facts = new ArrayList<>();
+        facts.add(java.util.Arrays.asList(target.id(), target.accountType(), target.organizationId(),
+                target.merchantId(), target.status()));
+        facts.add(java.util.Arrays.asList(request.requestType(), request.targetRoleId(), request.templateId(),
+                request.positionCode(), request.staffLevelId(), request.baseSalaryAmountCent(), request.salaryCurrency(),
+                request.salaryDetail(), request.permissionCodes().stream().sorted().toList(),
+                String.valueOf(request.validFrom()), String.valueOf(request.expiresAt()), request.singleUse()));
+        if (request.targetRoleId() != null) {
+            RoleRecord role = requireRole(request.targetRoleId());
+            facts.add(java.util.Arrays.asList(role.id(), role.organizationId(), role.status(),
+                    role.dataScope(), role.permissionCodes().stream().sorted().toList()));
+        }
+        if (request.templateId() != null) {
+            PermissionTemplate template = repository.findTemplateById(request.templateId())
+                    .orElseThrow(() -> ApiException.conflict("GRANT_SNAPSHOT_CHANGED", "权限模板已变化，请重新申请"));
+            facts.add(java.util.Arrays.asList(template.id(), template.ownerScopeType(), template.ownerScopeId(),
+                    template.status(), template.singleUse(), String.valueOf(template.validFrom()),
+                    String.valueOf(template.validUntil())));
+            facts.add(repository.findTemplateItems(template.id()).stream()
+                    .sorted(Comparator.comparing(PermissionTemplateItem::permissionCode)
+                            .thenComparing(item -> item.effect().name()))
+                    .map(item -> java.util.Arrays.asList(item.permissionCode(), item.effect().name(),
+                            item.dataScope(), item.sensitive())).toList());
+        }
+        if (REQUEST_USER_POSITION.equals(request.requestType())) {
+            PositionRoleTemplate mapping = repository.findPositionRoleTemplate(
+                    target.organizationId(), request.positionCode(), request.staffLevelId())
+                    .orElseThrow(() -> ApiException.conflict("GRANT_SNAPSHOT_CHANGED", "岗位配置已变化，请重新申请"));
+            facts.add(java.util.Arrays.asList(mapping.id(), mapping.organizationId(), mapping.roleId(),
+                    mapping.templateId(), mapping.status()));
+        }
+        facts.add(permissionDefinitions(request.permissionCodes()).stream()
+                .sorted(Comparator.comparing(PermissionDefinition::permissionCode))
+                .map(p -> java.util.Arrays.asList(p.permissionCode(), p.riskLevel(), p.requiresApproval(), p.status())).toList());
+        try {
+            byte[] json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(facts);
+            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(json));
+        } catch (java.security.NoSuchAlgorithmException | com.fasterxml.jackson.core.JsonProcessingException exception) {
+            throw new IllegalStateException("无法生成授权事实快照", exception);
+        }
+    }
+
     private void applyApprovedGrant(AccessGrantRequestRecord request, String idempotencyKey) {
         switch (request.requestType()) {
             case REQUEST_USER_ROLE -> assignUserRoleDirect(new RoleAssignmentRequest(
@@ -1376,6 +1500,7 @@ public class AccessService {
                 staffLevelId,
                 role,
                 template,
+                mapping.id(),
                 currency,
                 grantPermissionCodes(role.permissionCodes(), templatePermissionCodes)
         );
@@ -1404,19 +1529,48 @@ public class AccessService {
     }
 
     private GrantReviewPolicy grantReviewPolicy(List<String> permissionCodes) {
-        int risk = permissionDefinitions(permissionCodes).stream()
-                .filter(permission -> safeList(permissionCodes).contains(permission.permissionCode()))
-                .map(PermissionDefinition::riskLevel)
-                .mapToInt(this::riskRank)
-                .max()
-                .orElse(1);
-        if (risk >= 3) {
-            return new GrantReviewPolicy("permission_grant_l3", "top_authority", 3);
-        }
-        if (risk == 2) {
-            return new GrantReviewPolicy("permission_grant_l2", "two_levels_up", 2);
+        AccessGrantPolicy policy = grantPolicy();
+        AccessGrantPolicy.ReviewRule rule = permissionDefinitions(permissionCodes).stream()
+                .map(permission -> {
+                    AccessGrantPolicy.ReviewRule configured = permission.riskLevel() == null ? null
+                            : policy.riskReviews().get(permission.riskLevel().toLowerCase(Locale.ROOT));
+                    if (configured == null) {
+                        throw ApiException.badRequest("INVALID_GRANT_POLICY", "权限风险等级未配置审批策略：" + permission.permissionCode());
+                    }
+                    return configured;
+                })
+                .max(Comparator.comparingInt(AccessGrantPolicy.ReviewRule::requiredLevel)
+                        .thenComparingInt(value -> switch (value.approvalLevel()) {
+                            case "top_authority" -> 3; case "two_levels_up" -> 2; default -> 1;
+                        })).orElseThrow(() -> ApiException.badRequest("INVALID_GRANT_POLICY", "权限风险等级未配置审批策略"));
+        return new GrantReviewPolicy("permission_grant_l" + rule.requiredLevel(), rule.approvalLevel(), rule.requiredLevel());
+    }
+
+    public AccessGrantPolicy grantPolicy() {
+        String json = repository.findGrantPolicy()
+                .orElseThrow(() -> ApiException.conflict("GRANT_POLICY_NOT_CONFIGURED", "后台授权政策未配置"));
+        try {
+            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, AccessGrantPolicy.class);
+        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
+            throw ApiException.conflict("INVALID_GRANT_POLICY", "后台授权政策格式或上下限无效");
         }
-        return new GrantReviewPolicy("permission_grant_l1", "one_level_up", 1);
+    }
+
+    @Transactional
+    public AccessGrantPolicy updateGrantPolicy(AccessGrantPolicy.Update request) {
+        if (request == null || request.policy() == null) throw ApiException.badRequest("VALIDATION_FAILED", "授权政策不能为空");
+        requireText(request.operatorNote(), "operator_note");
+        return idempotencyService.execute("updateAccessGrantPolicy", request.idempotencyKey(), request, () -> {
+            try {
+                repository.saveGrantPolicy(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request.policy()));
+            } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
+                throw new IllegalStateException("授权政策无法保存", exception);
+            }
+            auditEventService.recordSuccess("access.grant_policy.changed", "admission.policy.manage",
+                    "supervisor_review_policy", "access_grant", null, request.idempotencyKey(),
+                    Map.of("reason", request.operatorNote()));
+            return request.policy();
+        }, AccessGrantPolicy.class);
     }
 
     private List<PermissionDefinition> permissionDefinitions(Collection<String> permissionCodes) {
@@ -1429,14 +1583,6 @@ public class AccessService {
                 .toList();
     }
 
-    private int riskRank(String riskLevel) {
-        return switch (riskLevel == null ? "" : riskLevel.toLowerCase(Locale.ROOT)) {
-            case "critical" -> 3;
-            case "high" -> 2;
-            default -> 1;
-        };
-    }
-
     private List<String> grantPermissionCodes(List<String> rolePermissionCodes, List<String> templatePermissionCodes) {
         List<String> result = new ArrayList<>();
         result.addAll(safeList(rolePermissionCodes));
@@ -1571,6 +1717,9 @@ public class AccessService {
         }
         Instant now = Instant.now();
         PositionRoleTemplate existing = repository.findPositionRoleTemplateById(id).orElse(null);
+        if (existing != null && existing.archivedAt() != null) {
+            throw ApiException.conflict("POSITION_TEMPLATE_ARCHIVED", "存档或已删除模板须经恢复后再编辑");
+        }
         PositionRoleTemplate saved = repository.savePositionRoleTemplate(new PositionRoleTemplate(
                 id,
                 positionCode,
@@ -1616,10 +1765,13 @@ public class AccessService {
         if (authorizationService.isPlatformAdmin(actor)) {
             return;
         }
-        if (actor.organizationId() == null || !actor.organizationId().equals(targetUser.organizationId())) {
+        if (!authorizationService.requireProjectScope(actor).equals(targetUser.organizationId())) {
             throw ApiException.forbidden("CROSS_ORG_FORBIDDEN",
                     "不能为其他组织的用户分配权限：" + targetUser.id());
         }
+        if (!isBlank(actor.merchantId()) && !actor.merchantId().equals(targetUser.merchantId())) {
+            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "不能操作其他商户的用户");
+        }
     }
 
     private void requirePlatformAdmin(AccessActor actor) {
@@ -1731,7 +1883,7 @@ public class AccessService {
     }
 
     private <T> PageResponse<T> page(List<T> items) {
-        return new PageResponse<>(items, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, items.size());
+        return new PageResponse<>(items, DEFAULT_PAGE, items.size(), items.size());
     }
 
     private <T> List<T> safeList(List<T> value) {
@@ -1765,6 +1917,7 @@ public class AccessService {
             String staffLevelId,
             RoleRecord role,
             PermissionTemplate template,
+            String mappingId,
             String salaryCurrency,
             List<String> permissionCodes
     ) {
diff --git a/backend/src/main/java/com/complex/platform/access/AccessUser.java b/backend/src/main/java/com/complex/platform/access/AccessUser.java
index 2c0c0c2c..4ae0bafd 100644
--- a/backend/src/main/java/com/complex/platform/access/AccessUser.java
+++ b/backend/src/main/java/com/complex/platform/access/AccessUser.java
@@ -1,5 +1,7 @@
 package com.complex.platform.access;
 
+// 文件版本：V0.01
+
 import java.time.Instant;
 import java.util.List;
 
@@ -11,7 +13,7 @@ public record AccessUser(
         String merchantId,
         String status,
         Instant createdAt,
-        String phoneHash,
+        @com.fasterxml.jackson.annotation.JsonIgnore String phoneHash,
         String createdVia,
         String consentReference,
         List<String> evidenceAttachmentIds,
diff --git a/backend/src/main/java/com/complex/platform/access/AuthorizationService.java b/backend/src/main/java/com/complex/platform/access/AuthorizationService.java
index 1a198700..9ed82813 100644
--- a/backend/src/main/java/com/complex/platform/access/AuthorizationService.java
+++ b/backend/src/main/java/com/complex/platform/access/AuthorizationService.java
@@ -1,5 +1,7 @@
 package com.complex.platform.access;
 
+// 文件版本：V0.03
+
 import com.complex.platform.auth.AuthService;
 import com.complex.platform.common.api.ApiException;
 import com.complex.platform.common.idempotency.CallerScopeResolver;
@@ -11,8 +13,9 @@ import java.util.Optional;
 import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
 import org.springframework.security.core.Authentication;
 import org.springframework.security.core.context.SecurityContextHolder;
-import org.springframework.security.core.userdetails.UserDetails;
 import org.springframework.stereotype.Service;
+import org.springframework.transaction.annotation.Transactional;
+import com.complex.platform.auth.SessionIssuer;
 
 @Service
 @ConditionalOnBean(AccessRepository.class)
@@ -30,47 +33,77 @@ public class AuthorizationService implements CallerScopeResolver {
         this.accessRepository = accessRepository;
     }
 
+    @Transactional
     public void requirePermission(String permissionCode) {
+        requirePermissions(permissionCode);
+    }
+
+    /** 同一动作先核验所有权限，再按来源消费一次；任何失败由事务回滚。 */
+    @Transactional
+    public void requirePermissions(String... permissionCodes) {
         AccessActor actor = currentActor();
         Instant checkedAt = Instant.now();
-        if (!knownPermission(permissionCode)) {
-            deny(permissionCode);
-        }
-        if (isPlatformAdminBridge(actor)) {
-            return;
+        List<AccessRepository.PermissionEffect> effects = accessRepository.findPermissionEffects(actor.id(), checkedAt).stream()
+                .filter(effect -> isActive(effect, checkedAt)).toList();
+        java.util.Map<String, AccessRepository.PermissionEffect> consume = new java.util.LinkedHashMap<>();
+        for (String code : new java.util.LinkedHashSet<>(java.util.Arrays.asList(permissionCodes))) {
+            AccessRepository.PermissionEffect grant = permissionGrant(actor, code, effects);
+            if (grant != null && grant.singleUse()) {
+                consume.putIfAbsent(grant.source() + ":" + grant.sourceId(), grant);
+            }
         }
-        // #14 钱包双路径：客人自助桥——客人主体（CustomerTokenAuthenticationFilter 写入，accountType=customer）
-        // 对白名单内的自助权限直接放行（动作已由控制器按客人 token 绑定到本人账户）。员工代客路径主体为员工，
-        // 不走此桥，仍按 staff 权限正常校验并归属提成。非白名单权限的客人主体一律走下方常规校验（必然 deny）。
-        if (isCustomerSelfServiceBridge(actor) && CUSTOMER_SELF_SERVICE_PERMISSIONS.contains(permissionCode)) {
-            return;
+        for (AccessRepository.PermissionEffect grant : consume.values()) {
+            if (!accessRepository.markSingleUsePermissionConsumed(
+                    grant.sourceId(), grant.permissionCode(), actor.id(), checkedAt)) {
+                deny(grant.permissionCode());
+            }
         }
+    }
 
-        List<AccessRepository.PermissionEffect> effects = accessRepository.findPermissionEffects(actor.id(), checkedAt).stream()
-                .filter(effect -> permissionCode.equals(effect.permissionCode()))
-                .filter(effect -> isActive(effect, checkedAt))
-                .toList();
-
-        if (effects.stream().anyMatch(effect -> effect.effect() == AccessRepository.Effect.DENY)) {
-            deny(permissionCode);
+    private AccessRepository.PermissionEffect permissionGrant(
+            AccessActor actor, String code, List<AccessRepository.PermissionEffect> effects) {
+        if (!"active".equals(actor.status()) || !knownPermission(code)) {
+            deny(code);
         }
-
-        AccessRepository.PermissionEffect grant = effects.stream()
+        List<AccessRepository.PermissionEffect> matching = effects.stream()
+                .filter(effect -> code.equals(effect.permissionCode())).toList();
+        if (matching.stream().anyMatch(effect -> effect.effect() == AccessRepository.Effect.DENY)) {
+            deny(code);
+        }
+        if (isMasterAuthority(actor)
+                || (isPlatformAdminBridge(actor) && isDailyPermission(code))
+                || (isCustomerSelfServiceBridge(actor) && CUSTOMER_SELF_SERVICE_PERMISSIONS.contains(code))) {
+            return null;
+        }
+        AccessRepository.PermissionEffect grant = matching.stream()
                 .filter(effect -> effect.effect() == AccessRepository.Effect.GRANT)
-                .min(Comparator.comparing(AccessRepository.PermissionEffect::sourceId, Comparator.nullsLast(String::compareTo)))
-                .orElse(null);
+                .filter(this::isExplicitOrDailyGrant)
+                .min(Comparator.comparing(AccessRepository.PermissionEffect::sourceId,
+                        Comparator.nullsLast(String::compareTo))).orElse(null);
         if (grant == null) {
-            deny(permissionCode);
+            deny(code);
         }
+        return grant;
+    }
 
-        if (grant.singleUse()) {
-            boolean consumed = accessRepository.markSingleUsePermissionConsumed(
-                    grant.sourceId(), grant.permissionCode(), actor.id(), checkedAt);
-            if (!consumed) {
-                // Another concurrent request already consumed this single-use grant.
-                deny(permissionCode);
-            }
-        }
+    private boolean isDailyPermission(String code) {
+        return accessRepository.listPermissions().stream()
+                .anyMatch(p -> code.equals(p.permissionCode()) && "active".equals(p.status())
+                        && !p.requiresApproval()
+                        && java.util.Set.of("low", "medium").contains(normalize(p.riskLevel())));
+    }
+
+    private boolean isExplicitOrDailyGrant(AccessRepository.PermissionEffect effect) {
+        // 默认管理员角色随权限字典播种，不能把其高危条目当成逐项授权。
+        return effect.source() != AccessRepository.Source.ROLE
+                || !"role_platform_admin".equals(effect.sourceId())
+                || isDailyPermission(effect.permissionCode());
+    }
+
+    public boolean isMasterAuthority(AccessActor actor) {
+        return isPlatformAdminBridge(actor)
+                && SessionIssuer.MASTER_USER_ID.equals(actor.id())
+                && SessionIssuer.MASTER_USERNAME.equals(actor.username());
     }
 
     /**
@@ -84,20 +117,23 @@ public class AuthorizationService implements CallerScopeResolver {
 
     /** 指定用户的有效权限集（平台管理用；调用方须先校验 {@code access.user.view}）。 */
     public EffectivePermissions effectivePermissionsForUser(String userId) {
-        Optional<AccessUser> target = accessRepository.findUserById(userId);
-        // F-AUTHZ-01 跨租户读修复：平台管理员（无 merchant scope）放行可读任意用户；merchant-scoped 调用方
-        // （商户管理员）只能读同 merchant_id 的目标用户，否则按 enforceMerchantScope 范式抛 CROSS_MERCHANT_FORBIDDEN，
-        // 防"按 userId 读他商户/平台管理员的完整权限清单"。目标 user 不存在时不在此泄露归属，交由下方按 userId 计算（空集）。
-        currentMerchantScope().ifPresent(scope -> {
-            String targetMerchantId = target.map(AccessUser::merchantId).orElse(null);
-            if (!scope.equals(targetMerchantId)) {
-                throw ApiException.forbidden(
-                        "CROSS_MERCHANT_FORBIDDEN", "无权操作其他商户的用户");
+        AccessActor actor = currentActor();
+        AccessUser user = accessRepository.findUserById(userId)
+                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "账号不存在"));
+        if (!isPlatformAdmin(actor)) {
+            String organizationId = requireProjectScope(actor);
+            if (!organizationId.equals(user.organizationId())) {
+                throw ApiException.forbidden("CROSS_ORG_FORBIDDEN", "无权查询其他组织的用户");
             }
-        });
+            if (actor.merchantId() != null && !actor.merchantId().isBlank()
+                    && !actor.merchantId().equals(user.merchantId())) {
+                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "无权查询其他商户的用户");
+            }
+        }
+        Optional<AccessUser> target = Optional.of(user);
         boolean wildcard = target
-                .map(user -> "active".equals(user.status())
-                        && "platform_admin".equals(normalize(user.accountType())))
+                .map(account -> "active".equals(account.status())
+                        && "platform_admin".equals(normalize(account.accountType())))
                 .orElse(false);
         return computeEffectivePermissions(userId, wildcard);
     }
@@ -116,12 +152,20 @@ public class AuthorizationService implements CallerScopeResolver {
         // 实现 A：grants 预扣 denies（DENY 优先），denies 仍单独返回供前端展示冲突来源。
         List<String> grants = active.stream()
                 .filter(effect -> effect.effect() == AccessRepository.Effect.GRANT)
+                .filter(this::isExplicitOrDailyGrant)
                 .map(AccessRepository.PermissionEffect::permissionCode)
                 .filter(code -> !denies.contains(code))
                 .distinct()
                 .sorted()
                 .toList();
-        return new EffectivePermissions(userId, grants, denies, wildcard, checkedAt);
+        java.util.Set<String> effective = new java.util.TreeSet<>(grants);
+        if (wildcard) {
+            accessRepository.listPermissions().stream()
+                    .filter(p -> SessionIssuer.MASTER_USER_ID.equals(userId) || isDailyPermission(p.permissionCode()))
+                    .map(PermissionDefinition::permissionCode).forEach(effective::add);
+        }
+        effective.removeAll(denies);
+        return new EffectivePermissions(userId, List.copyOf(effective), denies, false, checkedAt);
     }
 
     public AccessActor currentActor() {
@@ -143,10 +187,7 @@ public class AuthorizationService implements CallerScopeResolver {
                     user.status()
             );
         }
-        if (principal instanceof UserDetails userDetails) {
-            return new AccessActor(authentication.getName(), userDetails.getUsername(), "staff", null, null, "active");
-        }
-        return new AccessActor(authentication.getName(), authentication.getName(), "staff", null, null, "active");
+        throw ApiException.unauthorized("UNAUTHENTICATED", "无法识别的认证主体");
     }
 
     /**
@@ -188,23 +229,13 @@ public class AuthorizationService implements CallerScopeResolver {
     /** 当前主体是否持有某权限(布尔·不抛错):用于按权限脱敏字段(如内部结算价)。 */
     public boolean currentActorHasPermission(String permissionCode) {
         try {
-            if (!knownPermission(permissionCode)) {
-                return false;
-            }
-            AccessActor actor = currentActor();
-            if (isPlatformAdminBridge(actor)) {
-                return true;
-            }
             Instant checkedAt = Instant.now();
+            AccessActor actor = currentActor();
             List<AccessRepository.PermissionEffect> effects = accessRepository.findPermissionEffects(actor.id(), checkedAt).stream()
-                    .filter(effect -> permissionCode.equals(effect.permissionCode()))
-                    .filter(effect -> isActive(effect, checkedAt))
-                    .toList();
-            if (effects.stream().anyMatch(effect -> effect.effect() == AccessRepository.Effect.DENY)) {
-                return false;
-            }
-            return effects.stream().anyMatch(effect -> effect.effect() == AccessRepository.Effect.GRANT);
-        } catch (RuntimeException ex) {
+                    .filter(effect -> isActive(effect, checkedAt)).toList();
+            permissionGrant(actor, permissionCode, effects);
+            return true;
+        } catch (RuntimeException exception) {
             return false;
         }
     }
@@ -237,24 +268,18 @@ public class AuthorizationService implements CallerScopeResolver {
         }
     }
 
-    /**
-     * Returns the verified supervisor level of the current caller from their account type.
-     * Platform admins always get level 3. Staff accounts with accountType "supervisor_lN" get level N.
-     * All other accounts return 0, which is insufficient for any supervisor review policy.
-     */
+    /** 审批身份须由明确角色权限及实际管理职级共同证明；USB 主授权独立识别。 */
     public int currentSupervisorLevel() {
         AccessActor actor = currentActor();
-        if (isPlatformAdmin(actor)) {
+        if (isMasterAuthority(actor)) {
             return 3;
         }
-        String accountType = actor.accountType() == null ? "" : actor.accountType();
-        if (accountType.startsWith("supervisor_l")) {
-            try {
-                return Integer.parseInt(accountType.substring("supervisor_l".length()));
-            } catch (NumberFormatException ignored) {
-            }
+        if (!"active".equals(actor.status())
+                || !holdsPermissionViaRole(actor.id(), "governance.supervisor_review.approve")) {
+            return 0;
         }
-        return 0;
+        int rank = currentActorLevelRank();
+        return rank > 0 ? (rank + 2) / 3 : 0;
     }
 
     /**
@@ -263,7 +288,8 @@ public class AuthorizationService implements CallerScopeResolver {
      */
     public int currentActorLevelRank() {
         try {
-            return levelRankOf(currentActor().id());
+            AccessActor actor = currentActor();
+            return isMasterAuthority(actor) ? 30 : levelRankOf(actor.id());
         } catch (RuntimeException exception) {
             return 0;
         }
@@ -297,7 +323,7 @@ public class AuthorizationService implements CallerScopeResolver {
      * （ROLE 来源，非临时授权/模板单发）持有某权限码，从而解析"指定角色"审批闸而非旁路。
      *
      * <p>语义对齐 {@link #computeEffectivePermissions}：DENY 优先（命中 DENY 即不持有）；GRANT 仅当来源为
-     * {@link AccessRepository.Source#ROLE} 且在有效期内才算"角色赋予"。平台管理员桥（wildcard）视为持有。
+     * {@link AccessRepository.Source#ROLE} 且在有效期内才算"角色赋予"。不因平台管理员身份补齐审批角色。
      * 仓储为空或查询异常时安全保守地返回 {@code false}（不放行）。
      */
     public boolean holdsPermissionViaRole(String userId, String permissionCode) {
@@ -315,7 +341,8 @@ public class AuthorizationService implements CallerScopeResolver {
             }
             return active.stream()
                     .anyMatch(effect -> effect.effect() == AccessRepository.Effect.GRANT
-                            && effect.source() == AccessRepository.Source.ROLE);
+                            && effect.source() == AccessRepository.Source.ROLE
+                            && isExplicitOrDailyGrant(effect));
         } catch (RuntimeException exception) {
             return false;
         }
@@ -437,8 +464,7 @@ public class AuthorizationService implements CallerScopeResolver {
         if (isPlatformAdminBridge(actor)) {
             return Optional.empty();
         }
-        String projectId = actor.organizationId();
-        return projectId == null || projectId.isBlank() ? Optional.empty() : Optional.of(projectId);
+        return Optional.of(requireProjectScope(actor));
     }
 
     private boolean isPlatformAdminBridge(AccessActor actor) {
diff --git a/backend/src/main/java/com/complex/platform/access/DevAccessCredentialProvisioner.java b/backend/src/main/java/com/complex/platform/access/DevAccessCredentialProvisioner.java
index 0b36e883..483ce7dd 100644
--- a/backend/src/main/java/com/complex/platform/access/DevAccessCredentialProvisioner.java
+++ b/backend/src/main/java/com/complex/platform/access/DevAccessCredentialProvisioner.java
@@ -1,6 +1,9 @@
 package com.complex.platform.access;
 
+// 文件版本：V0.01
+
 import com.complex.platform.auth.AuthService;
+import com.complex.platform.common.api.ApiException;
 import java.time.Instant;
 import org.springframework.context.annotation.Profile;
 import org.springframework.stereotype.Service;
@@ -25,9 +28,14 @@ class DevAccessCredentialProvisioner implements AccessCredentialProvisioner {
     @Override
     public void provisionCredential(String userId, String password, String status, Instant now) {
         if (userId == null || userId.isBlank() || password == null || password.isBlank()) {
-            return;
+            throw ApiException.badRequest("VALIDATION_FAILED", "账号和初始密码不能为空");
+        }
+        if (!java.util.Set.of("active", "invited", "disabled").contains(status == null ? "" : status)) {
+            throw ApiException.badRequest("INVALID_ACCOUNT_STATUS", "无法识别的账号状态");
         }
-        accessRepository.findUserById(userId).ifPresent(user -> authService.provisionUser(
+        AccessUser user = accessRepository.findUserById(userId)
+                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "账号不存在"));
+        authService.provisionUser(
                 user.id(),
                 user.username(),
                 password,
@@ -35,6 +43,6 @@ class DevAccessCredentialProvisioner implements AccessCredentialProvisioner {
                 user.organizationId(),
                 user.merchantId(),
                 status
-        ));
+        );
     }
 }
diff --git a/backend/src/main/java/com/complex/platform/access/JdbcAccessCredentialProvisioner.java b/backend/src/main/java/com/complex/platform/access/JdbcAccessCredentialProvisioner.java
index da96acbf..11f7240b 100644
--- a/backend/src/main/java/com/complex/platform/access/JdbcAccessCredentialProvisioner.java
+++ b/backend/src/main/java/com/complex/platform/access/JdbcAccessCredentialProvisioner.java
@@ -1,7 +1,10 @@
 package com.complex.platform.access;
 
+// 文件版本：V0.01
+
 import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;
 
+import com.complex.platform.common.api.ApiException;
 import java.security.SecureRandom;
 import java.time.Instant;
 import org.springframework.context.annotation.Profile;
@@ -28,10 +31,13 @@ class JdbcAccessCredentialProvisioner implements AccessCredentialProvisioner {
     @Transactional
     public void provisionCredential(String userId, String password, String status, Instant now) {
         if (isBlank(userId) || isBlank(password)) {
-            return;
+            throw ApiException.badRequest("VALIDATION_FAILED", "账号和初始密码不能为空");
+        }
+        if (!java.util.Set.of("active", "invited", "disabled").contains(status == null ? "" : status)) {
+            throw ApiException.badRequest("INVALID_ACCOUNT_STATUS", "无法识别的账号状态");
         }
-        String credentialStatus = "disabled".equals(status) ? "disabled" : "active";
-        jdbcTemplate.update("""
+        String credentialStatus = "active".equals(status) ? "active" : "disabled";
+        int affected = jdbcTemplate.update("""
                 INSERT INTO auth_credentials (
                     user_id, password_hash, password_algo, failed_attempt_count, locked_until,
                     password_changed_at, status, created_at, updated_at
@@ -52,6 +58,9 @@ class JdbcAccessCredentialProvisioner implements AccessCredentialProvisioner {
                 .addValue("password_hash", passwordEncoder.encode(password))
                 .addValue("status", credentialStatus)
                 .addValue("now", timestampWithTimeZone(now)));
+        if (affected != 1) {
+            throw ApiException.conflict("CREDENTIAL_PROVISION_FAILED", "登录凭证未能保存");
+        }
     }
 
     private boolean isBlank(String value) {
diff --git a/backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java b/backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java
index 031a787c..8ea5d417 100644
--- a/backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java
+++ b/backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java
@@ -1,4 +1,5 @@
 package com.complex.platform.access;
+// 文件版本：V0.10
 import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
 import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;
 
@@ -35,6 +36,15 @@ class JdbcAccessRepository implements AccessRepository {
         return count != null && count > 0;
     }
 
+    @Override
+    public void lockGrantAuthorizationFacts() {
+        // M006-006。ponytail: 审批期间暂停授权定义写入；出现配置写入争用时再改为逐定义版本锁。
+        jdbcTemplate.getJdbcTemplate().execute("""
+                LOCK TABLE access_users, roles, role_permissions, permissions, permission_templates,
+                           permission_template_items, position_role_templates, staff_levels IN SHARE MODE
+                """);
+    }
+
     @Override
     public List<PermissionDefinition> listPermissions() {
         return jdbcTemplate.query("""
@@ -92,41 +102,35 @@ class JdbcAccessRepository implements AccessRepository {
 
     @Override
     @Transactional
-    public PermissionTemplate saveTemplate(PermissionTemplate template, List<PermissionTemplateItem> items) {
+    public PermissionTemplate createTemplate(PermissionTemplate template, List<PermissionTemplateItem> items) {
         jdbcTemplate.update("""
                 INSERT INTO permission_templates (
                     id, template_code, template_name, description, template_type, source_template_id,
                     owner_scope_type, owner_scope_id, usage_mode, valid_from, valid_to, single_use,
-                    status, reason, approved_by, approved_at, created_at, updated_at
-                )
+                    status, reason, approved_by, approved_at, created_at, updated_at)
                 VALUES (
                     :id, :template_code, :template_name, :description, :template_type, :source_template_id,
                     :owner_scope_type, :owner_scope_id, :usage_mode, :valid_from, :valid_to, :single_use,
-                    :status, :reason, :approved_by, :approved_at, :created_at, :updated_at
-                )
-                ON CONFLICT (id) DO UPDATE SET
-                    template_code = EXCLUDED.template_code,
-                    template_name = EXCLUDED.template_name,
-                    description = EXCLUDED.description,
-                    template_type = EXCLUDED.template_type,
-                    source_template_id = EXCLUDED.source_template_id,
-                    owner_scope_type = EXCLUDED.owner_scope_type,
-                    owner_scope_id = EXCLUDED.owner_scope_id,
-                    usage_mode = EXCLUDED.usage_mode,
-                    valid_from = EXCLUDED.valid_from,
-                    valid_to = EXCLUDED.valid_to,
-                    single_use = EXCLUDED.single_use,
-                    status = EXCLUDED.status,
-                    reason = EXCLUDED.reason,
-                    approved_by = EXCLUDED.approved_by,
-                    approved_at = EXCLUDED.approved_at,
-                    updated_at = EXCLUDED.updated_at
+                    :status, :reason, :approved_by, :approved_at, :created_at, :updated_at)
                 """, templateParameters(template));
+        insertTemplateItems(items);
+        return template;
+    }
 
-        jdbcTemplate.update("""
-                DELETE FROM permission_template_items
-                 WHERE template_id = :template_id
-                """, Map.of("template_id", template.id()));
+    @Override
+    @Transactional
+    public PermissionTemplate saveTemplate(PermissionTemplate template, List<PermissionTemplateItem> items) {
+        int changed = jdbcTemplate.update("""
+                UPDATE permission_templates
+                   SET template_name = :template_name, description = :description, template_type = :template_type,
+                       usage_mode = :usage_mode, valid_from = :valid_from, valid_to = :valid_to,
+                       single_use = :single_use, status = :status, reason = :reason,
+                       approved_by = :approved_by, approved_at = :approved_at, updated_at = :updated_at
+                 WHERE id = :id
+                """, templateParameters(template));
+        if (changed != 1) throw new IllegalStateException("待修改的权限模板不存在");
+        jdbcTemplate.update("DELETE FROM permission_template_items WHERE template_id = :template_id",
+                Map.of("template_id", template.id()));
         insertTemplateItems(items);
         return template;
     }
@@ -142,7 +146,7 @@ class JdbcAccessRepository implements AccessRepository {
                         item.sensitive()
                 ))
                 .toList();
-        return saveTemplate(copy, copiedItems);
+        return createTemplate(copy, copiedItems);
     }
 
     @Override
@@ -166,17 +170,6 @@ class JdbcAccessRepository implements AccessRepository {
                     :id, :username, :account_type, :organization_id, :merchant_id, :status, :created_at,
                     :phone_hash, :created_via, :consent_reference, :evidence_attachment_ids, :operator_note
                 )
-                ON CONFLICT (id) DO UPDATE SET
-                    username = EXCLUDED.username,
-                    account_type = EXCLUDED.account_type,
-                    organization_id = EXCLUDED.organization_id,
-                    merchant_id = EXCLUDED.merchant_id,
-                    status = EXCLUDED.status,
-                    phone_hash = EXCLUDED.phone_hash,
-                    created_via = EXCLUDED.created_via,
-                    consent_reference = EXCLUDED.consent_reference,
-                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
-                    operator_note = EXCLUDED.operator_note
                 """, new MapSqlParameterSource()
                 .addValue("id", user.id())
                 .addValue("username", user.username())
@@ -365,12 +358,6 @@ class JdbcAccessRepository implements AccessRepository {
                 VALUES (
                     :id, :role_name, :description, :data_scope, :status, :created_at, :organization_id
                 )
-                ON CONFLICT (id) DO UPDATE SET
-                    role_name = EXCLUDED.role_name,
-                    description = EXCLUDED.description,
-                    data_scope = EXCLUDED.data_scope,
-                    status = EXCLUDED.status,
-                    organization_id = EXCLUDED.organization_id
                 """, new MapSqlParameterSource()
                 .addValue("id", role.id())
                 .addValue("role_name", role.roleName())
@@ -431,14 +418,14 @@ class JdbcAccessRepository implements AccessRepository {
         if (organizationId == null) {
             return jdbcTemplate.query("""
                     SELECT id, position_code, staff_level_id, role_id, template_id, organization_id,
-                           status, created_by, created_at, updated_at
+                           status, created_by, created_at, updated_at, archived_at, deleted_at, delete_review_id, delete_reason
                       FROM position_role_templates
                      ORDER BY position_code, staff_level_id, organization_id NULLS FIRST
                     """, this::mapPositionRoleTemplate);
         }
         return jdbcTemplate.query("""
                 SELECT id, position_code, staff_level_id, role_id, template_id, organization_id,
-                       status, created_by, created_at, updated_at
+                       status, created_by, created_at, updated_at, archived_at, deleted_at, delete_review_id, delete_reason
                   FROM position_role_templates
                  WHERE organization_id IS NULL OR organization_id = :organization_id
                  ORDER BY position_code, staff_level_id, organization_id NULLS FIRST
@@ -449,9 +436,9 @@ class JdbcAccessRepository implements AccessRepository {
     public Optional<PositionRoleTemplate> findPositionRoleTemplateById(String id) {
         List<PositionRoleTemplate> result = jdbcTemplate.query("""
                 SELECT id, position_code, staff_level_id, role_id, template_id, organization_id,
-                       status, created_by, created_at, updated_at
+                       status, created_by, created_at, updated_at, archived_at, deleted_at, delete_review_id, delete_reason
                   FROM position_role_templates
-                 WHERE id = :id
+                 WHERE id = :id FOR UPDATE
                 """, Map.of("id", id), this::mapPositionRoleTemplate);
         return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
     }
@@ -468,9 +455,9 @@ class JdbcAccessRepository implements AccessRepository {
                 .addValue("staff_level_id", staffLevelId);
         List<PositionRoleTemplate> result = jdbcTemplate.query("""
                 SELECT id, position_code, staff_level_id, role_id, template_id, organization_id,
-                       status, created_by, created_at, updated_at
+                       status, created_by, created_at, updated_at, archived_at, deleted_at, delete_review_id, delete_reason
                   FROM position_role_templates
-                 WHERE status = 'active'
+                 WHERE status = 'active' AND archived_at IS NULL AND deleted_at IS NULL
                    AND position_code = :position_code
                    AND staff_level_id = :staff_level_id
                    AND (
@@ -478,7 +465,7 @@ class JdbcAccessRepository implements AccessRepository {
                         OR (:organization_id IS NOT NULL AND (organization_id = :organization_id OR organization_id IS NULL))
                    )
                  ORDER BY CASE WHEN organization_id = :organization_id THEN 0 ELSE 1 END
-                 LIMIT 1
+                 LIMIT 1 FOR SHARE
                 """, parameters, this::mapPositionRoleTemplate);
         return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
     }
@@ -518,10 +505,31 @@ class JdbcAccessRepository implements AccessRepository {
 
     @Override
     public void deletePositionRoleTemplate(String id) {
-        jdbcTemplate.update("""
-                DELETE FROM position_role_templates
+        throw new IllegalStateException("岗位模板须经过存档及删除审批，禁止物理删除");
+    }
+
+    @Override
+    public long countPositionTemplateUsage(String id) {
+        return jdbcTemplate.queryForObject("""
+                SELECT count(*) FROM user_position_assignments WHERE assigned_position_template_id = :id
+                """, Map.of("id", id), Long.class);
+    }
+
+    @Override
+    public void savePositionTemplateLifecycle(PositionRoleTemplate mapping, String actorId) {
+        int changed = jdbcTemplate.update("""
+                UPDATE position_role_templates
+                   SET status = 'inactive', archived_at = :archived_at, deleted_at = :deleted_at,
+                       delete_review_id = :review_id, delete_reason = :reason, updated_at = :updated_at,
+                       archived_by = CASE WHEN CAST(:archived_at AS timestamptz) IS NULL THEN NULL ELSE COALESCE(archived_by, :actor) END,
+                       deleted_by = CASE WHEN CAST(:deleted_at AS timestamptz) IS NULL THEN NULL ELSE :actor END
                  WHERE id = :id
-                """, Map.of("id", id));
+                """, new MapSqlParameterSource().addValue("id", mapping.id())
+                .addValue("archived_at", timestampWithTimeZone(mapping.archivedAt()), java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
+                .addValue("deleted_at", timestampWithTimeZone(mapping.deletedAt()), java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
+                .addValue("review_id", mapping.deleteReviewId()).addValue("reason", mapping.deleteReason())
+                .addValue("actor", actorId).addValue("updated_at", timestampWithTimeZone(mapping.updatedAt())));
+        if (changed != 1) throw new IllegalStateException("岗位模板不存在");
     }
 
     @Override
@@ -673,6 +681,15 @@ class JdbcAccessRepository implements AccessRepository {
 
     @Override
     public AccessGrantRequestRecord saveGrantRequest(AccessGrantRequestRecord request) {
+        if (!"pending".equals(request.status())) {
+            int changed = jdbcTemplate.update("""
+                    UPDATE access_grant_requests
+                       SET status = :status, decided_by = :decided_by, decided_at = :decided_at, updated_at = :updated_at
+                     WHERE id = :id AND status = 'pending'
+                    """, grantRequestParameters(request));
+            if (changed != 1) throw new IllegalStateException("授权申请已处理或不存在");
+            return request;
+        }
         jdbcTemplate.update("""
                 INSERT INTO access_grant_requests (
                     id, request_type, target_user_id, target_role_id, template_id, position_code,
@@ -688,28 +705,6 @@ class JdbcAccessRepository implements AccessRepository {
                     :reason, :requested_by, :decided_by, :decided_at, :project_id, :merchant_id,
                     :created_at, :updated_at
                 )
-                ON CONFLICT (id) DO UPDATE SET
-                    request_type = EXCLUDED.request_type,
-                    target_user_id = EXCLUDED.target_user_id,
-                    target_role_id = EXCLUDED.target_role_id,
-                    template_id = EXCLUDED.template_id,
-                    position_code = EXCLUDED.position_code,
-                    staff_level_id = EXCLUDED.staff_level_id,
-                    base_salary_amount_cent = EXCLUDED.base_salary_amount_cent,
-                    salary_currency = EXCLUDED.salary_currency,
-                    salary_detail = EXCLUDED.salary_detail,
-                    permission_codes = EXCLUDED.permission_codes,
-                    valid_from = EXCLUDED.valid_from,
-                    expires_at = EXCLUDED.expires_at,
-                    single_use = EXCLUDED.single_use,
-                    status = EXCLUDED.status,
-                    review_id = EXCLUDED.review_id,
-                    reason = EXCLUDED.reason,
-                    decided_by = EXCLUDED.decided_by,
-                    decided_at = EXCLUDED.decided_at,
-                    project_id = EXCLUDED.project_id,
-                    merchant_id = EXCLUDED.merchant_id,
-                    updated_at = EXCLUDED.updated_at
                 """, grantRequestParameters(request));
         return request;
     }
@@ -724,10 +719,69 @@ class JdbcAccessRepository implements AccessRepository {
                        created_at, updated_at
                   FROM access_grant_requests
                  WHERE id = :id
+                   FOR UPDATE
                 """, Map.of("id", id), this::mapGrantRequest);
         return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
     }
 
+    @Override
+    public void saveGrantFingerprint(String id, String fingerprint) {
+        int changed = jdbcTemplate.update("""
+                UPDATE access_grant_requests SET authorization_fingerprint = :fingerprint
+                 WHERE id = :id AND authorization_fingerprint IS NULL AND status = 'pending'
+                """, Map.of("id", id, "fingerprint", fingerprint));
+        if (changed != 1) {
+            throw new IllegalStateException("授权申请快照未能保存");
+        }
+    }
+
+    @Override
+    public Optional<String> findGrantFingerprint(String id) {
+        List<String> values = jdbcTemplate.query("SELECT authorization_fingerprint FROM access_grant_requests WHERE id = :id",
+                Map.of("id", id), (rs, row) -> rs.getString(1));
+        return values.isEmpty() ? Optional.empty() : Optional.ofNullable(values.get(0));
+    }
+
+    @Override
+    public com.complex.platform.common.api.PageResponse<AccessGrantRequestRecord> pageGrantRequests(
+            Instant cutoff, String organizationId, String merchantId, int page, int size) {
+        var parameters = new MapSqlParameterSource()
+                .addValue("cutoff", timestampWithTimeZone(cutoff))
+                .addValue("organization_id", organizationId, java.sql.Types.VARCHAR)
+                .addValue("merchant_id", merchantId, java.sql.Types.VARCHAR)
+                .addValue("size", size).addValue("offset", ((long) page - 1) * size);
+        String from = """
+                 FROM access_grant_requests request
+                 LEFT JOIN access_users target ON target.id = request.target_user_id
+                WHERE request.created_at >= :cutoff
+                  AND (:organization_id IS NULL OR
+                       (target.organization_id = :organization_id
+                        AND (request.project_id IS NULL OR request.project_id = :organization_id)))
+                  AND (:merchant_id IS NULL OR request.merchant_id = :merchant_id)
+                """;
+        long total = jdbcTemplate.queryForObject("SELECT count(*) " + from, parameters, Long.class);
+        List<AccessGrantRequestRecord> items = jdbcTemplate.query("SELECT request.* " + from
+                + " ORDER BY request.created_at DESC, request.id LIMIT :size OFFSET :offset", parameters, this::mapGrantRequest);
+        return new com.complex.platform.common.api.PageResponse<>(items, page, size, total);
+    }
+
+    @Override
+    public Optional<String> findGrantPolicy() {
+        return jdbcTemplate.query("""
+                SELECT parameters::text FROM supervisor_review_policies
+                 WHERE policy_code = 'access_grant' AND enabled = true
+                """, (rs, row) -> rs.getString(1)).stream().findFirst();
+    }
+
+    @Override
+    public void saveGrantPolicy(String policyJson) {
+        int changed = jdbcTemplate.update("""
+                UPDATE supervisor_review_policies SET parameters = CAST(:parameters AS jsonb), updated_at = now()
+                 WHERE policy_code = 'access_grant' AND enabled = true
+                """, Map.of("parameters", policyJson));
+        if (changed != 1) throw new IllegalStateException("授权政策未配置");
+    }
+
     @Override
     public List<AccessGrantRequestRecord> listGrantRequests(Instant createdAtFrom) {
         if (createdAtFrom == null) {
@@ -754,6 +808,31 @@ class JdbcAccessRepository implements AccessRepository {
                 .addValue("created_at_from", timestampWithTimeZone(createdAtFrom)), this::mapGrantRequest);
     }
 
+    @Override
+    @Transactional
+    public void replacePositionGrants(String userId, String roleId, String mappingId, List<String> applicationIds,
+                                      String actorId, Instant changedAt) {
+        var parameters = new MapSqlParameterSource()
+                .addValue("user_id", userId).addValue("role_id", roleId).addValue("mapping_id", mappingId)
+                .addValue("application_ids", applicationIds.toArray(String[]::new), java.sql.Types.ARRAY)
+                .addValue("actor_id", actorId).addValue("changed_at", timestampWithTimeZone(changedAt));
+        jdbcTemplate.update("""
+                UPDATE permission_template_applications
+                   SET status = 'revoked', revoked_by = :actor_id, revoked_at = :changed_at
+                 WHERE target_id = :user_id AND status = 'active'
+                   AND id = ANY ((SELECT position_application_ids FROM user_position_assignments
+                                   WHERE user_id = :user_id)::varchar[])
+                """, parameters);
+        int changed = jdbcTemplate.update("""
+                UPDATE user_position_assignments
+                   SET assigned_role_id = :role_id, assigned_position_template_id = :mapping_id, position_application_ids = :application_ids
+                 WHERE user_id = :user_id
+                """, parameters);
+        if (changed != 1) {
+            throw new IllegalStateException("岗位派生授权未能保存");
+        }
+    }
+
     @Override
     public List<PermissionEffect> findPermissionEffects(String userId, Instant checkedAt) {
         List<PermissionEffect> effects = new ArrayList<>();
@@ -766,6 +845,15 @@ class JdbcAccessRepository implements AccessRepository {
                   JOIN role_permissions rp ON rp.role_id = r.id
                  WHERE ur.user_id = :user_id
                 """, Map.of("user_id", userId), this::mapPermissionEffect));
+        effects.addAll(jdbcTemplate.query("""
+                SELECT position.user_id, permission.permission_code, 'grant' AS effect, 'ROLE' AS source,
+                       role.id AS source_id, NULL::timestamptz AS valid_from, NULL::timestamptz AS valid_until,
+                       FALSE AS single_use, FALSE AS consumed
+                  FROM user_position_assignments position
+                  JOIN roles role ON role.id = position.assigned_role_id AND role.status = 'active'
+                  JOIN role_permissions permission ON permission.role_id = role.id
+                 WHERE position.user_id = :user_id
+                """, Map.of("user_id", userId), this::mapPermissionEffect));
         effects.addAll(jdbcTemplate.query("""
                 SELECT pta.target_id AS user_id, pti.permission_code, pti.effect, 'TEMPLATE_APPLICATION' AS source,
                        pta.id AS source_id, COALESCE(pta.valid_from, pt.valid_from) AS valid_from,
@@ -799,6 +887,7 @@ class JdbcAccessRepository implements AccessRepository {
     }
 
     @Override
+    @Transactional
     public boolean markSingleUsePermissionConsumed(
             String sourceId,
             String permissionCode,
@@ -814,31 +903,39 @@ class JdbcAccessRepository implements AccessRepository {
         // AND consumed = FALSE is the atomic guard against TOCTOU races.
         // If another concurrent request already consumed this grant, UPDATE returns 0 rows.
         int templateRows = jdbcTemplate.update("""
-                UPDATE permission_template_applications
+                UPDATE permission_template_applications AS application
                    SET consumed = TRUE,
                        consumed_at = :consumed_at
-                 WHERE id = :source_id
-                   AND target_id = :user_id
-                   AND single_use = TRUE
-                   AND consumed = FALSE
+                  FROM permission_templates AS template
+                 WHERE application.id = :source_id
+                   AND application.template_id = template.id
+                   AND application.target_id = :user_id
+                   AND (application.single_use OR template.single_use)
+                   AND application.status = 'active'
+                   AND application.consumed = FALSE
                 """, parameters);
-        int grantRows = jdbcTemplate.update("""
+        int grantRows = templateRows == 0 ? jdbcTemplate.update("""
                 UPDATE temporary_permission_grants
                    SET consumed = TRUE,
                        consumed_at = :consumed_at
                  WHERE id::text = :source_id
                    AND subject_id = :user_id
+                   AND permission_code = :permission_code
+                   AND status = 'active'
                    AND single_use = TRUE
                    AND consumed = FALSE
-                """, parameters);
+                """, parameters) : 0;
         boolean consumed = templateRows > 0 || grantRows > 0;
         if (consumed) {
+            parameters.addValue("application_id", templateRows > 0 ? sourceId : null)
+                    .addValue("reason", templateRows > 0 ? "single_use"
+                            : "single_use:temporary_grant:" + sourceId);
             jdbcTemplate.update("""
                     INSERT INTO permission_template_usage_logs (
                         id, application_id, user_id, permission_code, result, reason, occurred_at
                     )
                     VALUES (
-                        :usage_log_id, :source_id, :user_id, :permission_code, 'consumed', 'single_use', :consumed_at
+                        :usage_log_id, :application_id, :user_id, :permission_code, 'consumed', :reason, :consumed_at
                     )
                     """, parameters);
         }
@@ -1065,7 +1162,9 @@ class JdbcAccessRepository implements AccessRepository {
                 resultSet.getString("status"),
                 resultSet.getString("created_by"),
                 nullableInstant(resultSet, "created_at"),
-                nullableInstant(resultSet, "updated_at")
+                nullableInstant(resultSet, "updated_at"),
+                nullableInstant(resultSet, "archived_at"), nullableInstant(resultSet, "deleted_at"),
+                resultSet.getString("delete_review_id"), resultSet.getString("delete_reason")
         );
     }
 
diff --git a/backend/src/main/java/com/complex/platform/access/PositionRoleTemplate.java b/backend/src/main/java/com/complex/platform/access/PositionRoleTemplate.java
index cb448702..342040ef 100644
--- a/backend/src/main/java/com/complex/platform/access/PositionRoleTemplate.java
+++ b/backend/src/main/java/com/complex/platform/access/PositionRoleTemplate.java
@@ -1,5 +1,7 @@
 package com.complex.platform.access;
 
+// 文件版本：V0.01
+
 import java.time.Instant;
 
 public record PositionRoleTemplate(
@@ -12,6 +14,21 @@ public record PositionRoleTemplate(
         String status,
         String createdBy,
         Instant createdAt,
-        Instant updatedAt
+        Instant updatedAt,
+        Instant archivedAt,
+        Instant deletedAt,
+        String deleteReviewId,
+        String deleteReason
 ) {
+    public PositionRoleTemplate(String id, String positionCode, String staffLevelId, String roleId,
+                                String templateId, String organizationId, String status, String createdBy,
+                                Instant createdAt, Instant updatedAt) {
+        this(id, positionCode, staffLevelId, roleId, templateId, organizationId, status, createdBy,
+                createdAt, updatedAt, null, null, null, null);
+    }
+
+    PositionRoleTemplate withLifecycle(Instant archived, Instant deleted, String reviewId, String reason, Instant now) {
+        return new PositionRoleTemplate(id, positionCode, staffLevelId, roleId, templateId, organizationId,
+                "inactive", createdBy, createdAt, now, archived, deleted, reviewId, reason);
+    }
 }
diff --git a/backend/src/main/java/com/complex/platform/access/dto/AccessUserRequest.java b/backend/src/main/java/com/complex/platform/access/dto/AccessUserRequest.java
index 946479e0..a6839eb2 100644
--- a/backend/src/main/java/com/complex/platform/access/dto/AccessUserRequest.java
+++ b/backend/src/main/java/com/complex/platform/access/dto/AccessUserRequest.java
@@ -1,5 +1,7 @@
 package com.complex.platform.access.dto;
 
+// 文件版本：V0.01
+
 import com.fasterxml.jackson.annotation.JsonAlias;
 import java.util.List;
 
@@ -9,7 +11,7 @@ public record AccessUserRequest(
         @JsonAlias("evidence_attachment_ids") List<String> evidenceAttachmentIds,
         String username,
         String password,
-        String phone,
+        @com.fasterxml.jackson.annotation.JsonIgnore String phone,
         @JsonAlias("account_type") String accountType,
         @JsonAlias("organization_id") String organizationId,
         @JsonAlias("merchant_id") String merchantId,
diff --git a/backend/src/main/java/com/complex/platform/access/staff/StaffService.java b/backend/src/main/java/com/complex/platform/access/staff/StaffService.java
index 07995e63..a5c9612c 100644
--- a/backend/src/main/java/com/complex/platform/access/staff/StaffService.java
+++ b/backend/src/main/java/com/complex/platform/access/staff/StaffService.java
@@ -1,5 +1,7 @@
 package com.complex.platform.access.staff;
 
+// 文件版本：V0.02
+
 import com.complex.platform.access.AccessActor;
 import com.complex.platform.access.AccessRepository;
 import com.complex.platform.access.AccessUser;
@@ -75,7 +77,7 @@ public class StaffService {
             org.springframework.beans.factory.ObjectProvider<com.complex.platform.access.AccessCredentialProvisioner> credentialProvisioner
     ) {
         this(repository, accessRepository, idempotencyService, auditEventService, authorizationService,
-                credentialProvisioner.getIfAvailable(com.complex.platform.access.AccessCredentialProvisioner::noop));
+                credentialProvisioner.getIfAvailable());
     }
 
     StaffService(
@@ -105,23 +107,15 @@ public class StaffService {
         this.idempotencyService = idempotencyService;
         this.auditEventService = auditEventService;
         this.authorizationService = authorizationService;
-        this.credentialProvisioner = credentialProvisioner == null
-                ? com.complex.platform.access.AccessCredentialProvisioner.noop() : credentialProvisioner;
+        this.credentialProvisioner = credentialProvisioner;
     }
 
     // ============================================================ 员工注册 / 审批
 
     @GuardedAction(permission = "staff.profile.submit")
+    @org.springframework.transaction.annotation.Transactional
     public StaffProfileRecord submitRegistration(StaffRegistrationRequest request) {
-        // F-AUTHZ-02 防跨商户越权注册：员工注册非裁决#2的治理写（merchant.profile/entity.binding/
-        // contract.draft/governance.space.manage），故用 merchant scope 守卫而非 requirePlatformScope——
-        // 平台管理员可为任意商户注册；merchant-scoped 调用者只能为本商户注册，允许商户自助管本商户员工。
-        authorizationService.currentMerchantScope().ifPresent(scope -> {
-            if (!scope.equals(trimToNull(request.merchantId()))) {
-                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
-                        "不能为其他商户登记员工");
-            }
-        });
+        requireRegistrationScope(request.projectId(), request.merchantId());
         try {
             return idempotencyService.execute(
                     "submitStaffRegistration",
@@ -143,7 +137,7 @@ public class StaffService {
                         }
                         Instant now = Instant.now();
                         StaffProfileRecord profile = new StaffProfileRecord(
-                                "staff_" + slug(request.idempotencyKey()),
+                                "staff_" + UUID.randomUUID(),
                                 projectId,
                                 trimToNull(request.merchantId()),
                                 null,
@@ -175,6 +169,7 @@ public class StaffService {
     }
 
     @GuardedAction(permission = "staff.profile.review")
+    @org.springframework.transaction.annotation.Transactional
     public StaffProfileRecord reviewRegistration(String id, StaffRegistrationReviewRequest request) {
         try {
             return idempotencyService.execute(
@@ -189,8 +184,14 @@ public class StaffService {
                         }
                         String decision = trimToNull(request.decision());
                         AccessActor actor = authorizationService.currentActor();
+                        if (!authorizationService.isPlatformAdmin(actor) && trimToNull(actor.merchantId()) != null) {
+                            throw ApiException.forbidden("COMPANY_APPROVAL_REQUIRED", "员工注册须由对应公司审批，商户不能自行批准");
+                        }
                         Instant now = Instant.now();
                         if ("approve".equalsIgnoreCase(decision == null ? "" : decision)) {
+                            if (credentialProvisioner == null) {
+                                throw ApiException.conflict("CREDENTIAL_PROVIDER_UNAVAILABLE", "登录凭证服务未配置，不能开通员工账号");
+                            }
                             String initialPassword = trimToNull(request.initialPassword());
                             if (initialPassword == null) {
                                 throw ApiException.badRequest("VALIDATION_FAILED",
@@ -200,7 +201,7 @@ public class StaffService {
                                     "usr_" + profile.id(),
                                     "staff_" + profile.id(),
                                     "staff",
-                                    null,
+                                    profile.projectId(),
                                     profile.merchantId(),
                                     "active",
                                     now
@@ -262,7 +263,14 @@ public class StaffService {
     /** V264：includeArchived=false（默认）排除已存档档案；true 供后台管理存档件。 */
     @GuardedAction(permission = "staff.profile.view")
     public PageResponse<StaffProfileRecord> listProfiles(boolean includeArchived) {
-        List<StaffProfileRecord> profiles = repository.listProfiles(merchantScopeOrNull(), includeArchived);
+        AccessActor actor = authorizationService.currentActor();
+        List<StaffProfileRecord> profiles;
+        if (!authorizationService.isPlatformAdmin(actor) && trimToNull(actor.merchantId()) == null) {
+            profiles = repository.listProfilesByProject(authorizationService.requireProjectScope(actor)).stream()
+                    .filter(profile -> includeArchived || profile.archivedAt() == null).toList();
+        } else {
+            profiles = repository.listProfiles(merchantScopeOrNull(), includeArchived);
+        }
         auditEventService.recordSuccess("staff.profile.viewed", "staff.profile.view",
                 "staff_profile", "all", null, null, Map.of("count", profiles.size()));
         return page(profiles);
@@ -903,14 +911,21 @@ public class StaffService {
     private StaffProfileRecord requireProfileInScope(String id) {
         StaffProfileRecord profile = repository.findProfileById(id)
                 .orElseThrow(() -> ApiException.notFound("STAFF_NOT_FOUND", "员工档案不存在：" + id));
-        Optional<String> scope = authorizationService.currentMerchantScope();
-        if (scope.isPresent() && !scope.get().equals(profile.merchantId())) {
-            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
-                    "无权查看其他商户的员工档案");
-        }
+        requireRegistrationScope(profile.projectId(), profile.merchantId());
         return profile;
     }
 
+    private void requireRegistrationScope(String projectId, String merchantId) {
+        AccessActor actor = authorizationService.currentActor();
+        if (authorizationService.isPlatformAdmin(actor)) return;
+        if (!authorizationService.requireProjectScope(actor).equals(projectId)) {
+            throw ApiException.forbidden("CROSS_ORG_FORBIDDEN", "不能处理其他公司的员工登记");
+        }
+        if (trimToNull(actor.merchantId()) != null && !actor.merchantId().equals(trimToNull(merchantId))) {
+            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "不能处理其他商户的员工登记");
+        }
+    }
+
     private StaffDeviceCredentialRecord requireCredentialInScope(String credId) {
         StaffDeviceCredentialRecord credential = repository.findCredentialById(credId)
                 .orElseThrow(() -> ApiException.notFound("CREDENTIAL_NOT_FOUND",
diff --git a/backend/src/main/java/com/complex/platform/bar/BarBillSettleRequest.java b/backend/src/main/java/com/complex/platform/bar/BarBillSettleRequest.java
index 8b7e5e92..83a93746 100644
--- a/backend/src/main/java/com/complex/platform/bar/BarBillSettleRequest.java
+++ b/backend/src/main/java/com/complex/platform/bar/BarBillSettleRequest.java
@@ -1,5 +1,7 @@
 package com.complex.platform.bar;
 
+// 文件版本：V0.01
+
 import java.util.List;
 
 public record BarBillSettleRequest(
@@ -10,8 +12,17 @@ public record BarBillSettleRequest(
         Long settlementAmountCent,
         String paymentChannel,
         String cashierStaffId,
-        String posTransactionNo
+        String posTransactionNo,
+        String phone,
+        String smsCode
 ) {
+    public BarBillSettleRequest(String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds,
+            String paymentId, Long settlementAmountCent, String paymentChannel,
+            String cashierStaffId, String posTransactionNo) {
+        this(idempotencyKey, operatorNote, evidenceAttachmentIds, paymentId, settlementAmountCent,
+                paymentChannel, cashierStaffId, posTransactionNo, null, null);
+    }
+
     /** 向后兼容构造器（A2/A3 前签名）：既有调用方零改动。 */
     public BarBillSettleRequest(
             String idempotencyKey,
diff --git a/backend/src/main/java/com/complex/platform/bar/BarBillingService.java b/backend/src/main/java/com/complex/platform/bar/BarBillingService.java
index fa7022c2..ee0734c1 100644
--- a/backend/src/main/java/com/complex/platform/bar/BarBillingService.java
+++ b/backend/src/main/java/com/complex/platform/bar/BarBillingService.java
@@ -1,5 +1,7 @@
 package com.complex.platform.bar;
 
+// 文件版本：V0.02
+
 import static com.complex.platform.bar.BarBillRecords.*;
 import static com.complex.platform.bar.BarValidation.*;
 
@@ -35,6 +37,8 @@ public class BarBillingService {
     // 字段注入（required=false）以免改构造点/单测；bar 依赖 customer 无构造环（StoredValueService 不依赖 bar bean）。
     @org.springframework.beans.factory.annotation.Autowired(required = false)
     com.complex.platform.customer.StoredValueService storedValueService;
+    @org.springframework.beans.factory.annotation.Autowired
+    com.complex.platform.customer.CustomerService customerService;
     // P0-D/CFG-01（V340）：卡座提成率改后台可配（消灭 15%/10% 写死）。字段注入免改构造点（同上两例）；
     // 生产由 Spring 恒注入 JdbcCommissionPointsConfigRepository。资金级 fail-closed：为 null 即拒绝计提
     // （见 requireCommissionRateBp），绝不静默回落硬编码常量——回落常量正是本批要根除的东西。
@@ -148,6 +152,19 @@ public class BarBillingService {
         ));
     }
 
+    void requestStoredValueCode(String billId, String phone) {
+        BarBillRecord bill = repository.findBillById(billId)
+                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "账单不存在或已被删除"));
+        checkMerchantOwnership(bill.merchantId());
+        if ("settled".equals(bill.settlementStatus())) {
+            throw ApiException.conflict("BAR_BILL_ALREADY_SETTLED", "账单已结账，请勿重复操作");
+        }
+        if (customerService == null) {
+            throw ApiException.conflict("CUSTOMER_OTP_PROVIDER_UNAVAILABLE", "短信验证服务不可用");
+        }
+        customerService.requestStoredValueCode(resolveBillCustomerId(bill), phone);
+    }
+
     BarBillRecord settleBill(String billId, BarBillSettleRequest request) {
         BarBillSettleRequest actual = requireRequest(request);
         // 2c-2：行锁读取账单，序列化并发结算（配合 settled 守卫防 TOCTOU 重复结算）。
@@ -226,11 +243,11 @@ public class BarBillingService {
             if (storedValueService == null) {
                 throw ApiException.conflict("STORED_VALUE_UNAVAILABLE", "储值支付当前不可用");
             }
-            String svCustomerId = resolveBillCustomerId(bill);
-            if (svCustomerId == null) {
-                throw ApiException.badRequest("VALIDATION_FAILED",
-                        "储值支付需先绑定客人（账单未关联客人，无法扣储值卡）");
+            if (customerService == null) {
+                throw ApiException.conflict("CUSTOMER_OTP_PROVIDER_UNAVAILABLE", "短信验证服务不可用");
             }
+            String svCustomerId = customerService.verifyStoredValueCustomer(
+                    resolveBillCustomerId(bill), actual.phone(), actual.smsCode()).id();
             storedValueService.consumeInternal(svCustomerId, null, bill.merchantId(), settlementAmount,
                     "酒吧结账-储值支付 账单" + bill.id(), "barsv_" + slug(actual.idempotencyKey()));
         }
diff --git a/backend/src/main/java/com/complex/platform/bar/BarController.java b/backend/src/main/java/com/complex/platform/bar/BarController.java
index c1e9724b..733e35f4 100644
--- a/backend/src/main/java/com/complex/platform/bar/BarController.java
+++ b/backend/src/main/java/com/complex/platform/bar/BarController.java
@@ -1,4 +1,6 @@
 package com.complex.platform.bar;
+
+// 文件版本：V0.01
 import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;
 
 import com.complex.platform.access.AuthorizationService;
@@ -138,6 +140,15 @@ public class BarController {
         return ApiResponse.ok(service.createBillPayIntent(billId, request));
     }
 
+    @PostMapping("/bills/{bill_id}/stored-value/sms")
+    public ApiResponse<java.util.Map<String, String>> requestStoredValueCode(
+            @PathVariable("bill_id") String billId,
+            @RequestBody java.util.Map<String, String> request) {
+        authorizationService.requirePermission("finance.payment.register");
+        service.requestStoredValueCode(billId, request.get("phone"));
+        return ApiResponse.ok(java.util.Map.of("message", "验证码已发送"));
+    }
+
     @PostMapping("/bills/{bill_id}/settle")
     public ApiResponse<BarBillRecord> settleBill(
             @PathVariable("bill_id") String billId,
diff --git a/backend/src/main/java/com/complex/platform/bar/BarService.java b/backend/src/main/java/com/complex/platform/bar/BarService.java
index 836bc737..209747d1 100644
--- a/backend/src/main/java/com/complex/platform/bar/BarService.java
+++ b/backend/src/main/java/com/complex/platform/bar/BarService.java
@@ -1,5 +1,7 @@
 package com.complex.platform.bar;
 
+// 文件版本：V0.01
+
 import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.PageResponse;
 import java.util.List;
@@ -625,6 +627,11 @@ public class BarService {
         );
     }
 
+    @com.complex.platform.gate.GuardedAction(capability = "finance.payment.register")
+    public void requestStoredValueCode(String billId, String phone) {
+        billingService.requestStoredValueCode(operations.requireText(billId, "bill_id"), phone);
+    }
+
     @com.complex.platform.gate.GuardedAction(capability = "finance.payment.register")
     @Transactional
     public BarBillRecord settleBill(String billId, BarBillSettleRequest request) {
diff --git a/backend/src/main/java/com/complex/platform/customer/CustomerOtpVerifier.java b/backend/src/main/java/com/complex/platform/customer/CustomerOtpVerifier.java
index fed2710d..c1580c89 100644
--- a/backend/src/main/java/com/complex/platform/customer/CustomerOtpVerifier.java
+++ b/backend/src/main/java/com/complex/platform/customer/CustomerOtpVerifier.java
@@ -1,5 +1,12 @@
 package com.complex.platform.customer;
 
+// 文件版本：V0.01
+
 interface CustomerOtpVerifier {
     void verify(String phone, String smsCode, String scene);
+
+    default void requestCode(String phone, String scene) {
+        throw com.complex.platform.common.api.ApiException.conflict(
+                "CUSTOMER_OTP_PROVIDER_UNAVAILABLE", "短信服务未配置，请联系管理员");
+    }
 }
diff --git a/backend/src/main/java/com/complex/platform/customer/CustomerRepository.java b/backend/src/main/java/com/complex/platform/customer/CustomerRepository.java
index 09f2a4e6..162beb3a 100644
--- a/backend/src/main/java/com/complex/platform/customer/CustomerRepository.java
+++ b/backend/src/main/java/com/complex/platform/customer/CustomerRepository.java
@@ -1,5 +1,7 @@
 package com.complex.platform.customer;
 
+// 文件版本：V0.01
+
 import com.complex.platform.bar.BarDepositRuleRecord;
 import com.complex.platform.bar.BarReservationRecord;
 import com.complex.platform.bar.BarTableRecord;
@@ -223,6 +225,11 @@ public interface CustomerRepository {
         throw new UnsupportedOperationException("saveStoredValueAccount not supported by this repository");
     }
 
+    default List<CustomerStoredValueTransactionRecord> listStoredValueTransactions(
+            String customerId, String accountId, int page, int size) {
+        throw new UnsupportedOperationException("listStoredValueTransactions not supported");
+    }
+
     default CustomerStoredValueTransactionRecord saveStoredValueTransaction(CustomerStoredValueTransactionRecord txn) {
         throw new UnsupportedOperationException("saveStoredValueTransaction not supported by this repository");
     }
diff --git a/backend/src/main/java/com/complex/platform/customer/CustomerService.java b/backend/src/main/java/com/complex/platform/customer/CustomerService.java
index 065dc1b1..650ffd5a 100644
--- a/backend/src/main/java/com/complex/platform/customer/CustomerService.java
+++ b/backend/src/main/java/com/complex/platform/customer/CustomerService.java
@@ -1,5 +1,7 @@
 package com.complex.platform.customer;
 
+// 文件版本：V0.02
+
 import com.complex.platform.bar.BarDepositRuleRecord;
 import com.complex.platform.bar.BarOrderRecord;
 import com.complex.platform.bar.BarOrderRequest;
@@ -143,6 +145,33 @@ public class CustomerService {
         }
     }
 
+    /** M006-027：仅核验储值付款人，不登录、不注册、不签发会话。 */
+    public CustomerRecord verifyStoredValueCustomer(String expectedCustomerId, String phone, String smsCode) {
+        String number = requireText(phone, "手机号");
+        String code = requireText(smsCode, "短信验证码");
+        enforceOtpRateLimit(number);
+        CustomerRecord customer = requireStoredValueCustomer(expectedCustomerId, number);
+        otpVerifier.verify(number, code, "customer.stored_value.consume");
+        return customer;
+    }
+
+    public void requestStoredValueCode(String expectedCustomerId, String phone) {
+        String number = requireText(phone, "手机号");
+        enforceOtpRateLimit(number);
+        requireStoredValueCustomer(expectedCustomerId, number);
+        otpVerifier.requestCode(number, "customer.stored_value.consume");
+    }
+
+    private CustomerRecord requireStoredValueCustomer(String expectedCustomerId, String number) {
+        CustomerRecord customer = repository.findCustomerByPhone(number)
+                .orElseThrow(() -> ApiException.unauthorized("CUSTOMER_OTP_INVALID", "手机号或短信验证码不正确"));
+        customer = enforceUsableAccount(customer, clock.instant());
+        if (expectedCustomerId != null && !expectedCustomerId.equals(customer.id())) {
+            throw ApiException.forbidden("STORED_VALUE_CUSTOMER_MISMATCH", "手机号与本次付款客人不一致");
+        }
+        return customer;
+    }
+
     @Transactional
     public CustomerLoginResponse phoneLogin(CustomerPhoneLoginRequest request) {
         try {
diff --git a/backend/src/main/java/com/complex/platform/customer/JdbcCustomerRepository.java b/backend/src/main/java/com/complex/platform/customer/JdbcCustomerRepository.java
index bc95f9f3..7150b13c 100644
--- a/backend/src/main/java/com/complex/platform/customer/JdbcCustomerRepository.java
+++ b/backend/src/main/java/com/complex/platform/customer/JdbcCustomerRepository.java
@@ -1,4 +1,6 @@
 package com.complex.platform.customer;
+
+// 文件版本：V0.01
 import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
 import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;
 
@@ -1201,6 +1203,19 @@ class JdbcCustomerRepository implements CustomerRepository {
         return txn;
     }
 
+    @Override
+    public List<CustomerStoredValueTransactionRecord> listStoredValueTransactions(
+            String customerId, String accountId, int page, int size) {
+        return jdbcTemplate.query("""
+                SELECT id, account_id, customer_id, txn_type, amount_cent, balance_after_cent,
+                       payment_intent_id, reason, idempotency_key, created_at, paid_at
+                  FROM customer_stored_value_transactions
+                 WHERE customer_id = :customer AND account_id = :account
+                 ORDER BY created_at DESC, id DESC LIMIT :size OFFSET :offset
+                """, new MapSqlParameterSource().addValue("customer", customerId).addValue("account", accountId)
+                .addValue("size", size).addValue("offset", (long) (page - 1) * size), this::mapStoredValueTransaction);
+    }
+
     @Override
     public Optional<CustomerStoredValueTransactionRecord> findStoredValueTransactionByIdempotencyKey(String key) {
         if (key == null) {
diff --git a/backend/src/main/java/com/complex/platform/customer/MemberWalletController.java b/backend/src/main/java/com/complex/platform/customer/MemberWalletController.java
index 814366d4..acd6a629 100644
--- a/backend/src/main/java/com/complex/platform/customer/MemberWalletController.java
+++ b/backend/src/main/java/com/complex/platform/customer/MemberWalletController.java
@@ -1,8 +1,9 @@
 package com.complex.platform.customer;
 
+// 文件版本：V0.02
+
 import com.complex.platform.common.api.ApiResponse;
 import com.complex.platform.common.api.PageResponse;
-import com.complex.platform.customer.dto.StoredValueConsumeRequest;
 import com.complex.platform.customer.dto.StoredValueSettleRequest;
 import com.complex.platform.customer.dto.StoredValueTopupRequest;
 import com.complex.platform.finance.PaymentIntentRecord;
@@ -53,6 +54,27 @@ public class MemberWalletController {
 
     // ---- 储值 ----
 
+    @PostMapping("/orders/{order_id}/stored-value/pay")
+    public ApiResponse<com.complex.platform.ticketing.TicketOrderRecord> payTicketOrder(
+            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
+            @RequestHeader(name = "Authorization", required = false) String authorization,
+            @PathVariable("order_id") String orderId,
+            @Valid @RequestBody com.complex.platform.customer.dto.StoredValueConsumeRequest request) {
+        String customerId = customerService.resolveCustomerId(token(customerToken, authorization));
+        return ApiResponse.ok(storedValueService.payTicketOrder(
+                customerId, orderId, request.accountId(), request.phone(), request.smsCode()));
+    }
+
+    @PostMapping("/stored-value/sms")
+    public ApiResponse<java.util.Map<String, String>> requestStoredValueCode(
+            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
+            @RequestHeader(name = "Authorization", required = false) String authorization,
+            @RequestBody java.util.Map<String, String> request) {
+        String customerId = customerService.resolveCustomerId(token(customerToken, authorization));
+        customerService.requestStoredValueCode(customerId, request.get("phone"));
+        return ApiResponse.ok(java.util.Map.of("message", "验证码已发送"));
+    }
+
     @PostMapping("/stored-value/topup")
     public ApiResponse<PaymentIntentRecord> topup(
             @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
@@ -74,17 +96,6 @@ public class MemberWalletController {
         return ApiResponse.ok(storedValueService.settleTopup(customerId, request.projectId(), request.merchantId(), request.paymentIntentId()));
     }
 
-    @PostMapping("/stored-value/consume")
-    public ApiResponse<CustomerStoredValueTransactionRecord> consume(
-            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
-            @RequestHeader(name = "Authorization", required = false) String authorization,
-            @Valid @RequestBody StoredValueConsumeRequest request
-    ) {
-        String customerId = customerService.resolveCustomerId(token(customerToken, authorization));
-        return ApiResponse.ok(storedValueService.consume(customerId, request.projectId(), request.merchantId(),
-                request.amountCent() == null ? 0L : request.amountCent(), request.reason(), request.idempotencyKey()));
-    }
-
     @GetMapping("/stored-value/account")
     public ApiResponse<CustomerStoredValueAccountRecord> getAccount(
             @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
@@ -97,6 +108,25 @@ public class MemberWalletController {
         return ApiResponse.ok(repository.findStoredValueAccount(customerId, projectId, merchantId).orElse(null));
     }
 
+    @GetMapping("/stored-value/accounts/{account_id}/transactions")
+    public ApiResponse<List<CustomerStoredValueTransactionRecord>> transactions(
+            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
+            @RequestHeader(name = "Authorization", required = false) String authorization,
+            @PathVariable("account_id") String accountId,
+            @RequestParam(defaultValue = "1") int page,
+            @RequestParam(defaultValue = "20") int size) {
+        String customerId = customerService.resolveCustomerId(token(customerToken, authorization));
+        if (page < 1 || size < 1 || size > 100) {
+            throw com.complex.platform.common.api.ApiException.badRequest("VALIDATION_FAILED", "分页参数无效");
+        }
+        if (repository.listStoredValueAccountsByCustomer(customerId).stream()
+                .noneMatch(account -> accountId.equals(account.id()))) {
+            throw com.complex.platform.common.api.ApiException.notFound(
+                    "STORED_VALUE_ACCOUNT_NOT_FOUND", "储值卡不存在");
+        }
+        return ApiResponse.ok(repository.listStoredValueTransactions(customerId, accountId, page, size));
+    }
+
     @GetMapping("/stored-value/accounts")
     public ApiResponse<PageResponse<CustomerStoredValueAccountRecord>> listAccounts(
             @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
diff --git a/backend/src/main/java/com/complex/platform/customer/StoredValueService.java b/backend/src/main/java/com/complex/platform/customer/StoredValueService.java
index a5fbbe04..eed489a5 100644
--- a/backend/src/main/java/com/complex/platform/customer/StoredValueService.java
+++ b/backend/src/main/java/com/complex/platform/customer/StoredValueService.java
@@ -1,5 +1,7 @@
 package com.complex.platform.customer;
 
+// 文件版本：V0.05
+
 import com.complex.platform.common.api.ApiException;
 import com.complex.platform.common.money.MoneyRounding;
 import com.complex.platform.evidence.AuditEventService;
@@ -33,7 +35,7 @@ import com.complex.platform.common.Strings;
  *
  * <p><b>预付资金合规</b>：充值不凭空加余额——充值 {@link #topup} 先经 finance 创建支付意图，
  * 支付成功后调 {@link #settleTopup} 校验支付意图已 succeeded、金额一致、未重复入账，方才入账并写流水。
- * 消费 {@link #consume} 余额不足拒；退卡 {@link #refund} 退回余额（实际退款打款待支付渠道）；
+ * 消费 {@link #payTicketOrder} 余额不足拒；退卡 {@link #refund} 退回余额（实际退款打款待支付渠道）；
  * 冻结/解冻 {@link #freeze}/{@link #unfreeze} 阻断/恢复变动。所有余额变动写流水 + 事件。
  *
  * <p><b>B1d⑤ 储值卡分级</b>：每个操作带 {@code merchantId}——为空=综合体通用卡（综合体内各商户通用，按 project 隔离）；
@@ -63,7 +65,7 @@ public class StoredValueService {
     // svcr_ = 柜台退款命名空间（复核发现：与线上退款共享命名空间时，跨路径同键重放会把线上退款流水
     // 错当柜台退款成功返回——不落柜台审计/凭证还可能引发双重兑付）。
     private static final String[] RESERVED_IDEMPOTENCY_PREFIXES = {
-            "sv_settle_", "sv_topup_", "svtop_", "barsv_", "svtxn_", "svacc_", "svcr_"};
+            "sv_settle_", "sv_topup_", "svtop_", "barsv_", "svtxn_", "svacc_", "svcr_", "ticketsv_"};
 
     private final CustomerRepository repository;
     private final FinanceService financeService;
@@ -73,6 +75,10 @@ public class StoredValueService {
     private final com.complex.platform.access.AuthorizationService authorizationService;
     private final NotificationService notificationService;
     private final Clock clock;
+    @org.springframework.beans.factory.annotation.Autowired
+    private CustomerService customerService;
+    @org.springframework.beans.factory.annotation.Autowired
+    private com.complex.platform.ticketing.TicketingRepository ticketingRepository;
 
     // B1d⑤ 商户卡充值资金归属默认值（开关，可后台/配置覆盖；默认商户预收）。
     @Value("${stored-value.merchant-card.fund-ownership:merchant_prepaid}")
@@ -109,8 +115,8 @@ public class StoredValueService {
 
     /**
      * 发起充值：创建一笔 finance 支付意图（status=created），返回该支付意图。此时余额不变。
-     * 客户端完成支付后由支付渠道回调（测试期 {@code financeService.markPaymentIntentSucceeded}）置
-     * succeeded，再调 {@link #settleTopup} 入账。merchantId 非空=给商户级卡充值。
+     * 客户端按真实渠道拉起信息支付，再由 {@link #settleTopup} 向渠道核验成功回执并入账。
+     * merchantId 非空=给商户级卡充值；柜台 POS 确认仍由独立的线下确认入口办理。
      */
     @Transactional
     @GuardedAction(capability = "customer.stored_value.topup", permission = "customer.stored_value.topup")
@@ -158,6 +164,22 @@ public class StoredValueService {
     @GuardedAction(capability = "customer.stored_value.topup", permission = "customer.stored_value.topup")
     public CustomerStoredValueTransactionRecord settleTopup(String customerId, String projectId, String merchantId,
                                                             String paymentIntentId) {
+        StoredValueTopupIntentScope bound = repository.findStoredValueTopupIntent(paymentIntentId)
+                .orElseThrow(() -> ApiException.badRequest("VALIDATION_FAILED", "该支付意向不属于储值充值"));
+        if (!customerId.equals(bound.customerId())) {
+            throw ApiException.badRequest("VALIDATION_FAILED", "该支付意向不属于本次充值");
+        }
+        if (!java.util.Objects.equals(projectId, bound.projectId())
+                || !java.util.Objects.equals(merchantId, bound.merchantId())) {
+            throw ApiException.conflict("STORED_VALUE_SCOPE_MISMATCH", "结算卡范围与充值卡范围不一致");
+        }
+        if (paymentGateway == null || !paymentGateway.isGatewayEnabled()) {
+            throw ApiException.conflict("PAYMENT_GATEWAY_UNAVAILABLE", "支付渠道未启用");
+        }
+        CustomerStoredValueAccountRecord card = repository.findStoredValueAccount(customerId, projectId, merchantId)
+                .orElseThrow(() -> ApiException.notFound("STORED_VALUE_ACCOUNT_NOT_FOUND", "储值卡不存在"));
+        financeService.confirmOnlinePaymentIntent(paymentIntentId, merchantId,
+                merchantId == null ? null : effectiveFundOwnership(card));
         return settleTopupInternal(customerId, projectId, merchantId, paymentIntentId);
     }
 
@@ -182,7 +204,7 @@ public class StoredValueService {
                                                                      String merchantId, String paymentIntentId) {
         Instant now = now();
         requireCustomer(customerId);
-        PaymentIntentRecord intent = financeService.findPaymentIntent(paymentIntentId)
+        PaymentIntentRecord intent = financeService.findPaymentIntentForUpdate(paymentIntentId)
                 .orElseThrow(() -> ApiException.notFound("PAYMENT_INTENT_NOT_FOUND", "支付意向不存在或已被删除"));
         if (!"succeeded".equals(intent.status())) {
             throw ApiException.conflict("PAYMENT_NOT_SUCCEEDED", "该支付尚未成功");
@@ -240,10 +262,51 @@ public class StoredValueService {
 
     @Transactional
     @GuardedAction(capability = "customer.stored_value.consume", permission = "customer.stored_value.consume")
-    public CustomerStoredValueTransactionRecord consume(String customerId, String projectId, String merchantId,
-                                                        long amountCent, String reason, String idempotencyKey) {
-        requireClientIdempotencyKey(idempotencyKey);
-        return consumeInternal(customerId, projectId, merchantId, amountCent, reason, idempotencyKey);
+    public com.complex.platform.ticketing.TicketOrderRecord payTicketOrder(
+            String customerId, String orderId, String accountId, String phone, String smsCode) {
+        if (authorizationService == null || customerService == null || ticketingRepository == null) {
+            throw ApiException.forbidden("STORED_VALUE_UNAVAILABLE", "储值订单支付当前不可用");
+        }
+        var actor = authorizationService.currentActor();
+        if (!"customer".equals(actor.accountType()) || !"active".equals(actor.status())
+                || !customerId.equals(actor.id())) {
+            throw ApiException.forbidden("STORED_VALUE_CUSTOMER_MISMATCH", "仅可支付本人订单");
+        }
+        var order = ticketingRepository.findTicketOrderByIdForUpdate(Strings.requireText(orderId, "订单编号"))
+                .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "订单不存在"));
+        if (!customerId.equals(order.customerId())) {
+            throw ApiException.forbidden("STORED_VALUE_CUSTOMER_MISMATCH", "仅可支付本人订单");
+        }
+        if ("cancelled".equals(order.status())) {
+            throw ApiException.conflict("ORDER_CANCELLED", "订单已取消");
+        }
+        if ("paid".equals(order.payStatus())) return order;
+        if (!"created".equals(order.status()) || !"unpaid".equals(order.payStatus())) {
+            throw ApiException.conflict("ORDER_NOT_PAYABLE", "订单不在待支付状态");
+        }
+        CustomerStoredValueAccountRecord card = repository.listStoredValueAccountsByCustomer(customerId).stream()
+                .filter(a -> a.id().equals(accountId)).findFirst()
+                .orElseThrow(() -> ApiException.notFound("STORED_VALUE_ACCOUNT_NOT_FOUND", "储值卡不存在"));
+        // 当前票务订单未携带项目范围，不将有限定项目的卡当成全局卡使用。
+        if (card.projectId() != null || (card.merchantId() != null && !card.merchantId().equals(order.barId()))) {
+            throw ApiException.conflict("STORED_VALUE_SCOPE_MISMATCH", "所选储值卡不适用于该订单范围");
+        }
+        if (!"CNY".equals(order.currency())) {
+            throw ApiException.conflict("PAYMENT_CURRENCY_MISMATCH", "订单币种与储值余额不一致");
+        }
+        customerService.verifyStoredValueCustomer(customerId, phone, smsCode);
+        CustomerStoredValueTransactionRecord debit = consumeBalance(customerId, card.projectId(), card.merchantId(), order.amountCent(),
+                "票务订单储值支付 " + order.id(), "ticketsv_" + order.id(), true);
+        Instant paidAt = now();
+        ticketingRepository.saveTicketPaymentIntent(new com.complex.platform.ticketing.TicketPaymentIntentRecord(
+                debit.id(), "ticket_order", order.id(), order.amountCent(), order.currency(),
+                "stored_value", "succeeded", paidAt, paidAt));
+        return ticketingRepository.updateTicketOrder(new com.complex.platform.ticketing.TicketOrderRecord(
+                order.id(), order.orderNo(), order.customerId(), order.ticketTypeId(), order.priceRuleId(),
+                order.quantity(), order.visitDate(), order.salesChannel(), order.buyerPhone(), order.amountCent(),
+                order.dynamicPricingRuleId(), order.baseAmountCent(), order.dynamicAdjustmentCent(), order.currency(),
+                "paid", order.issueStatus(), order.status(), order.createdAt(), paidAt, order.merchantId(),
+                paidAt, order.barId()));
     }
 
     /**
@@ -254,6 +317,11 @@ public class StoredValueService {
     @Transactional
     public CustomerStoredValueTransactionRecord consumeInternal(String customerId, String projectId, String merchantId,
                                                         long amountCent, String reason, String idempotencyKey) {
+        return consumeBalance(customerId, projectId, merchantId, amountCent, reason, idempotencyKey, false);
+    }
+
+    private CustomerStoredValueTransactionRecord consumeBalance(String customerId, String projectId, String merchantId,
+            long amountCent, String reason, String idempotencyKey, boolean verifiedCustomerOrder) {
         if (idempotencyKey == null || idempotencyKey.isBlank()) {
             throw ApiException.badRequest("VALIDATION_FAILED", "idempotency_key is required");
         }
@@ -273,7 +341,7 @@ public class StoredValueService {
         CustomerStoredValueAccountRecord account = requireAccountForUpdate(customerId, projectId, merchantId);
         // 清单 P1-1:与 refund/freeze/unfreeze 一致,消费须先过卡范围校验(他商户/通用卡越权即拒,
         // 且在状态检查之前——不向越权者泄露卡状态)。此前缺此行 → merchantId 请求体可控可跨商户扣款。
-        requireMerchantScopeForCard(account);
+        if (!verifiedCustomerOrder) requireMerchantScopeForCard(account);
         requireActive(account);
         if (account.balanceCent() < amountCent) {
             throw ApiException.conflict("STORED_VALUE_INSUFFICIENT", "储值余额不足");
diff --git a/backend/src/main/java/com/complex/platform/customer/dto/StoredValueConsumeRequest.java b/backend/src/main/java/com/complex/platform/customer/dto/StoredValueConsumeRequest.java
index cdf33cc0..b7b38919 100644
--- a/backend/src/main/java/com/complex/platform/customer/dto/StoredValueConsumeRequest.java
+++ b/backend/src/main/java/com/complex/platform/customer/dto/StoredValueConsumeRequest.java
@@ -1,15 +1,14 @@
 package com.complex.platform.customer.dto;
 
+// 文件版本：V0.01
+
 import jakarta.validation.constraints.NotBlank;
-import jakarta.validation.constraints.NotNull;
-import jakarta.validation.constraints.Positive;
 
-/** 储值消费请求（余额扣减，不足拒）。B5 边界校验：幂等键非空、金额正数（与 service 层 double-spend 守恒一致，前置拦截）。 */
+/** 客人订单储值支付：指定本人卡，金额从路径中的真实订单读取。 */
 public record StoredValueConsumeRequest(
         @NotBlank String idempotencyKey,
-        String projectId,
-        String merchantId,
-        @NotNull @Positive Long amountCent,
-        String reason
+        @NotBlank String accountId,
+        @NotBlank String phone,
+        @NotBlank String smsCode
 ) {
 }
diff --git a/backend/src/main/java/com/complex/platform/finance/FinanceRepository.java b/backend/src/main/java/com/complex/platform/finance/FinanceRepository.java
index 7f7273d7..36ad02fa 100644
--- a/backend/src/main/java/com/complex/platform/finance/FinanceRepository.java
+++ b/backend/src/main/java/com/complex/platform/finance/FinanceRepository.java
@@ -1,5 +1,7 @@
 package com.complex.platform.finance;
 
+// 文件版本：V0.01
+
 import java.math.BigDecimal;
 import java.time.Instant;
 import java.time.LocalDate;
@@ -256,6 +258,10 @@ public interface FinanceRepository {
     PaymentIntentRecord savePaymentIntent(PaymentIntentRecord paymentIntent);
 
     /** 按 id 查支付意图（储值充值/会员开通绑支付意图：入账前校验支付意图存在且已成功）。 */
+    default Optional<PaymentIntentRecord> findPaymentIntentByIdForUpdate(String id) {
+        return findPaymentIntentById(id);
+    }
+
     default Optional<PaymentIntentRecord> findPaymentIntentById(String id) {
         return Optional.empty();
     }
diff --git a/backend/src/main/java/com/complex/platform/finance/FinanceService.java b/backend/src/main/java/com/complex/platform/finance/FinanceService.java
index b97f24ae..4fcad230 100644
--- a/backend/src/main/java/com/complex/platform/finance/FinanceService.java
+++ b/backend/src/main/java/com/complex/platform/finance/FinanceService.java
@@ -1,5 +1,7 @@
 package com.complex.platform.finance;
 
+// 文件版本：V0.01
+
 import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
 import com.complex.platform.common.api.PageResponse;
@@ -351,6 +353,55 @@ public class FinanceService implements TerminationSettlementPort {
         );
     }
 
+    public Optional<PaymentIntentRecord> findPaymentIntentForUpdate(String paymentIntentId) {
+        return repository.findPaymentIntentByIdForUpdate(paymentIntentId);
+    }
+
+    /** M006-027：仅信任已启用渠道的服务端查询结果，不能用内部成功标志代替收款。 */
+    @Transactional
+    public PaymentIntentRecord confirmOnlinePaymentIntent(String paymentIntentId, String payeeMerchantId, String fundMode) {
+        if (paymentGateway == null || !paymentGateway.isGatewayEnabled()) {
+            throw ApiException.conflict("PAYMENT_GATEWAY_UNAVAILABLE", "支付渠道未启用");
+        }
+        PaymentIntentRecord intent = repository.findPaymentIntentByIdForUpdate(paymentIntentId)
+                .orElseThrow(() -> ApiException.notFound("PAYMENT_INTENT_NOT_FOUND", "支付意向不存在"));
+        if (!Set.of("wechat", "alipay", "unionpay").contains(intent.channel())
+                || intent.gatewayRef() == null || intent.gatewayRef().isBlank()
+                || intent.gatewayProvider() == null || intent.gatewayProvider().isBlank()
+                || "reserved".equals(intent.gatewayProvider())) {
+            throw ApiException.conflict("PAYMENT_GATEWAY_UNAVAILABLE", "支付渠道尚未返回有效支付单");
+        }
+        var result = paymentGateway.queryStatus(intent.gatewayRef());
+        if (result == null || result.requiresInternalFallback()) {
+            throw ApiException.conflict("PAYMENT_GATEWAY_UNAVAILABLE", "支付渠道未配置");
+        }
+        if (result.status() != com.complex.platform.finance.gateway.PaymentGatewayResult.Status.SUCCEEDED) {
+            throw ApiException.conflict("PAYMENT_NOT_SUCCEEDED", "支付渠道尚未确认收款成功");
+        }
+        var confirmed = result.confirmedPayment();
+        if (confirmed == null
+                || !java.util.Objects.equals(intent.gatewayProvider(), result.gatewayProvider())
+                || !java.util.Objects.equals(intent.gatewayRef(), result.gatewayRef())
+                || !java.util.Objects.equals(intent.gatewayRef(), confirmed.gatewayRef())
+                || !intent.businessOrderType().equals(confirmed.businessOrderType())
+                || !intent.businessOrderId().equals(confirmed.businessOrderId())
+                || intent.amountCent() != confirmed.amountCent()
+                || !intent.currency().equals(confirmed.currency())
+                || !intent.channel().equals(confirmed.channel())
+                || !java.util.Objects.equals(payeeMerchantId, confirmed.payeeMerchantId())
+                || !java.util.Objects.equals(fundMode, confirmed.fundMode())) {
+            throw ApiException.conflict("PAYMENT_CONFIRMATION_MISMATCH", "支付渠道确认与本次充值单不一致");
+        }
+        if ("succeeded".equals(intent.status())) return intent;
+        if (!"created".equals(intent.status())) {
+            throw ApiException.conflict("PAYMENT_INTENT_NOT_PENDING", "支付意向不在待支付状态");
+        }
+        recordInternalAccountingRevenue(intent.id(), intent.businessOrderType(), intent.businessOrderId(), intent.amountCent());
+        return repository.savePaymentIntent(new PaymentIntentRecord(intent.id(), intent.businessOrderType(),
+                intent.businessOrderId(), intent.amountCent(), intent.currency(), intent.channel(), "succeeded",
+                result.gatewayProvider(), result.gatewayRef(), result.status().name(), intent.createdAt(), Instant.now()));
+    }
+
     /** 按 id 查支付意图（储值充值/会员开通：入账前校验支付意图存在且已成功）。 */
     public Optional<PaymentIntentRecord> findPaymentIntent(String paymentIntentId) {
         if (paymentIntentId == null || paymentIntentId.isBlank()) {
@@ -1286,7 +1337,8 @@ public class FinanceService implements TerminationSettlementPort {
                 gw.gatewayRef(),
                 gw.status().name(),
                 now,
-                now
+                now,
+                gw.requiresInternalFallback() ? null : gw.paymentUrl()
         ));
     }
 
diff --git a/backend/src/main/java/com/complex/platform/finance/JdbcFinanceRepository.java b/backend/src/main/java/com/complex/platform/finance/JdbcFinanceRepository.java
index 312ebf10..84e3c7f9 100644
--- a/backend/src/main/java/com/complex/platform/finance/JdbcFinanceRepository.java
+++ b/backend/src/main/java/com/complex/platform/finance/JdbcFinanceRepository.java
@@ -1,4 +1,6 @@
 package com.complex.platform.finance;
+
+// 文件版本：V0.01
 import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
 import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;
 
@@ -371,6 +373,15 @@ class JdbcFinanceRepository implements FinanceRepository {
                 """, Map.of("id", id), this::mapPaymentIntent).stream().findFirst();
     }
 
+    @Override
+    public Optional<PaymentIntentRecord> findPaymentIntentByIdForUpdate(String id) {
+        return jdbcTemplate.query("""
+                SELECT id, business_order_type, business_order_id, amount_cent, currency,
+                       channel, status, gateway_provider, gateway_ref, gateway_status, created_at, updated_at
+                  FROM finance_payment_intents WHERE id = :id FOR UPDATE
+                """, Map.of("id", id), this::mapPaymentIntent).stream().findFirst();
+    }
+
     private PaymentIntentRecord mapPaymentIntent(ResultSet rs, int rowNumber) throws SQLException {
         return new PaymentIntentRecord(
                 rs.getString("id"),
diff --git a/backend/src/main/java/com/complex/platform/finance/PaymentIntentRecord.java b/backend/src/main/java/com/complex/platform/finance/PaymentIntentRecord.java
index 128146d7..a35ba0ba 100644
--- a/backend/src/main/java/com/complex/platform/finance/PaymentIntentRecord.java
+++ b/backend/src/main/java/com/complex/platform/finance/PaymentIntentRecord.java
@@ -1,5 +1,7 @@
 package com.complex.platform.finance;
 
+// 文件版本：V0.01
+
 import java.time.Instant;
 
 /**
@@ -20,8 +22,17 @@ public record PaymentIntentRecord(
         String gatewayRef,
         String gatewayStatus,
         Instant createdAt,
-        Instant updatedAt
+        Instant updatedAt,
+        String paymentUrl
 ) {
+    /** 拉起链接随本次响应返回，不作为支付成功凭据。 */
+    public PaymentIntentRecord(String id, String businessOrderType, String businessOrderId,
+            long amountCent, String currency, String channel, String status,
+            String gatewayProvider, String gatewayRef, String gatewayStatus, Instant createdAt, Instant updatedAt) {
+        this(id, businessOrderType, businessOrderId, amountCent, currency, channel, status,
+                gatewayProvider, gatewayRef, gatewayStatus, createdAt, updatedAt, null);
+    }
+
     /** 便捷构造：无渠道预留信息（占位/内部结算路径，向后兼容旧 9 参签名）。 */
     public PaymentIntentRecord(
             String id,
diff --git a/backend/src/main/java/com/complex/platform/finance/gateway/PaymentGatewayResult.java b/backend/src/main/java/com/complex/platform/finance/gateway/PaymentGatewayResult.java
index 41c20709..f3aab832 100644
--- a/backend/src/main/java/com/complex/platform/finance/gateway/PaymentGatewayResult.java
+++ b/backend/src/main/java/com/complex/platform/finance/gateway/PaymentGatewayResult.java
@@ -1,5 +1,7 @@
 package com.complex.platform.finance.gateway;
 
+// 文件版本：V0.01
+
 /**
  * 支付渠道结果（端口出参，厂商无关）。永不抛出受检异常以阻塞业务——
  * 开关关/无厂商均以状态码表达，由业务侧决定回退路径。
@@ -13,9 +15,15 @@ public record PaymentGatewayResult(
         Status status,
         String gatewayProvider,
         String gatewayRef,
-        String message
+        String message,
+        String paymentUrl,
+        PaymentGatewayRequest confirmedPayment
 ) {
 
+    public PaymentGatewayResult(Status status, String gatewayProvider, String gatewayRef, String message) {
+        this(status, gatewayProvider, gatewayRef, message, null, null);
+    }
+
     public enum Status {
         /** 开关关：能力预留未启用，业务回退现有内部/占位结算（不阻塞）。 */
         RESERVED,
diff --git a/backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java b/backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java
index caa8341b..4464bb33 100644
--- a/backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java
+++ b/backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java
@@ -1,5 +1,7 @@
 package com.complex.platform.governance.review;
 
+// 文件版本：V0.01
+
 import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
 import com.complex.platform.common.api.PageResponse;
@@ -146,6 +148,10 @@ public class SupervisorReviewService {
                     SupervisorReviewDecisionRecord pending = supervisorReviewRepository
                             .findPendingSupervisorReviewById(id)
                             .orElseThrow(() -> ApiException.notFound("SUPERVISOR_REVIEW_NOT_FOUND", "主管复核单不存在或已不在待复核状态"));
+                    if (ApprovalLevel.TOP_AUTHORITY.code().equals(pending.approvalLevel())
+                            && !authorizationService.isMasterAuthority(authorizationService.currentActor())) {
+                        throw ApiException.forbidden("MASTER_AUTHORITY_REQUIRED", "此项须使用 USB 最高权限会话审批");
+                    }
                     int requiredLevel = pending.requiredLevel();
                     if (reviewerLevel < requiredLevel) {
                         throw ApiException.forbidden("SUPERVISOR_LEVEL_TOO_LOW", "复核人级别低于要求的复核级别");
@@ -159,7 +165,7 @@ public class SupervisorReviewService {
                     // REQ-69 逐级上提：当本次复核记录有按 rank 计算的所需审批人 rank 时，
                     // 审批人的 level_rank 必须不低于该 rank（在主管级校验之上的管理职级门槛）。
                     if (pending.requiredApproverRank() != null) {
-                        int reviewerRank = authorizationService.levelRankOf(reviewerId);
+                        int reviewerRank = authorizationService.currentActorLevelRank();
                         if (reviewerRank < pending.requiredApproverRank()) {
                             throw ApiException.forbidden(
                                     "SUPERVISOR_RANK_TOO_LOW",
diff --git a/backend/src/main/java/com/complex/platform/ticketing/JdbcTicketingRepository.java b/backend/src/main/java/com/complex/platform/ticketing/JdbcTicketingRepository.java
index 39a70dba..5b7f720c 100644
--- a/backend/src/main/java/com/complex/platform/ticketing/JdbcTicketingRepository.java
+++ b/backend/src/main/java/com/complex/platform/ticketing/JdbcTicketingRepository.java
@@ -1,5 +1,7 @@
 package com.complex.platform.ticketing;
 
+// 文件版本：V0.01
+
 import com.fasterxml.jackson.databind.ObjectMapper;
 import java.util.List;
 import java.util.Map;
@@ -930,6 +932,12 @@ class JdbcTicketingRepository implements TicketingRepository {
                 .findFirst();
     }
 
+    @Override
+    public Optional<TicketOrderRecord> findTicketOrderByIdForUpdate(String id) {
+        return jdbcTemplate.query(JdbcTicketingSql.ticketOrderSelect() + " WHERE id = :id FOR UPDATE",
+                Map.of("id", id), rowMapper::mapTicketOrder).stream().findFirst();
+    }
+
     @Override
     public TicketOrderRecord updateTicketOrder(TicketOrderRecord order) {
         return saveTicketOrder(order);
diff --git a/backend/src/main/java/com/complex/platform/ticketing/TicketingRepository.java b/backend/src/main/java/com/complex/platform/ticketing/TicketingRepository.java
index 92d587ff..12196be7 100644
--- a/backend/src/main/java/com/complex/platform/ticketing/TicketingRepository.java
+++ b/backend/src/main/java/com/complex/platform/ticketing/TicketingRepository.java
@@ -1,5 +1,7 @@
 package com.complex.platform.ticketing;
 
+// 文件版本：V0.01
+
 import java.util.List;
 import java.util.Optional;
 
@@ -299,6 +301,10 @@ public interface TicketingRepository {
 
     Optional<TicketOrderRecord> findTicketOrderById(String id);
 
+    default Optional<TicketOrderRecord> findTicketOrderByIdForUpdate(String id) {
+        return findTicketOrderById(id);
+    }
+
     TicketOrderRecord updateTicketOrder(TicketOrderRecord order);
 
     List<TicketOrderRecord> listTicketOrders();
diff --git a/backend/src/main/resources/db/migration/V386__access_grant_authorization_fingerprint.sql b/backend/src/main/resources/db/migration/V386__access_grant_authorization_fingerprint.sql
new file mode 100644
index 00000000..f90c67ee
--- /dev/null
+++ b/backend/src/main/resources/db/migration/V386__access_grant_authorization_fingerprint.sql
@@ -0,0 +1,29 @@
+-- 文件版本：V0.04
+-- M006-006：授权申请的审批事实快照；不回填既有申请，缺快照须重新申请。
+ALTER TABLE access_grant_requests ADD COLUMN authorization_fingerprint VARCHAR(64);
+
+-- M006-016：复用主管审批策略表；保留已有初始值与正整数取值域，上下限可在后台修改。
+ALTER TABLE supervisor_review_policies ADD COLUMN parameters JSONB NOT NULL DEFAULT '{}';
+INSERT INTO supervisor_review_policies
+    (policy_code, action_type, object_type, trigger_mode, outcome, approval_level, required_level, parameters)
+VALUES ('access_grant', 'access.grant_request', 'access_grant_request', 'permission_insufficient',
+        'block', 'one_level_up', 1, '{"windowDays":{"value":30,"min":1,"max":2147483647},"positionThreshold":{"value":3,"min":1,"max":2147483647},"userThreshold":{"value":3,"min":1,"max":2147483647},"riskReviews":{"low":{"approvalLevel":"one_level_up","requiredLevel":1},"medium":{"approvalLevel":"one_level_up","requiredLevel":1},"high":{"approvalLevel":"two_levels_up","requiredLevel":2},"critical":{"approvalLevel":"top_authority","requiredLevel":3}}}');
+
+-- M006-026：沿用存档、主管复核和软删除口径，记录可恢复。
+ALTER TABLE position_role_templates
+    ADD COLUMN archived_at TIMESTAMPTZ,
+    ADD COLUMN archived_by VARCHAR(64),
+    ADD COLUMN deleted_at TIMESTAMPTZ,
+    ADD COLUMN deleted_by VARCHAR(64),
+    ADD COLUMN delete_review_id VARCHAR(128),
+    ADD COLUMN delete_reason TEXT;
+INSERT INTO supervisor_review_policies
+    (policy_code, action_type, object_type, trigger_mode, outcome, approval_level, required_level)
+VALUES ('position_role_template_delete', 'access.position_role_template.delete',
+        'position_role_template', 'always', 'block', 'top_authority', 3);
+
+-- M006-009/026：岗位派生角色/应用及配置来源；换岗只替换这些来源，独立授权继续有效。
+ALTER TABLE user_position_assignments
+    ADD COLUMN assigned_role_id VARCHAR(64) REFERENCES roles(id),
+    ADD COLUMN assigned_position_template_id VARCHAR(64) REFERENCES position_role_templates(id),
+    ADD COLUMN position_application_ids VARCHAR(64)[] NOT NULL DEFAULT '{}';
diff --git a/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java b/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
index aa48a647..f0ecdae8 100644
--- a/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
@@ -1,5 +1,7 @@
 package com.complex.platform.access;
 
+// 文件版本：V0.02
+
 import com.complex.platform.support.InMemoryAuditEventRepository;
 import com.complex.platform.support.InMemoryIdempotencyRepository;
 import static org.assertj.core.api.Assertions.assertThat;
@@ -58,6 +60,14 @@ class AccessServiceTests {
         credentialProvisioner = new RecordingCredentialProvisioner();
         accessRepository.addPermission("ops.production_readiness.view", "medium", false);
         accessRepository.addPermission("ops.production_readiness.check", "high", false);
+        accessRepository.addPermission("governance.supervisor_review.view", "medium", false);
+        accessRepository.addPermission("governance.supervisor_review.approve", "high", false);
+        // M006-001/012：夹具明确逐项授予高危权限和审批角色，不依赖管理员隐式全权。
+        accessRepository.roles.put("explicit_admin_grants", new RoleRecord(
+                "explicit_admin_grants", "Explicit test grants", null, "global", "active", Instant.now(),
+                List.of("ops.production_readiness.check", "governance.supervisor_review.view",
+                        "governance.supervisor_review.approve", "finance.ar_ap.manage"), null));
+        accessRepository.assignUserRole("usr_admin", "explicit_admin_grants");
         authorizationService = new AuthorizationService(accessRepository);
         service = newAccessService(false, null);
         authenticateAsPlatformAdmin("seed-org");
@@ -70,7 +80,7 @@ class AccessServiceTests {
 
     @Test
     void copyPermissionTemplateKeepsSnapshotWhenSourceChangesAndWritesAuditEvent() {
-        service.createPermissionTemplate(new PermissionTemplateRequest(
+        PermissionTemplate source = service.createPermissionTemplate(new PermissionTemplateRequest(
                 "create-source",
                 "seed",
                 List.of(),
@@ -84,27 +94,27 @@ class AccessServiceTests {
                 true
         ));
 
-        PermissionTemplate copy = service.copyPermissionTemplate("tpl_create_source", new PermissionTemplateRequest(
+        PermissionTemplate copy = service.copyPermissionTemplate(source.id(), new PermissionTemplateRequest(
                 "copy-source",
                 "copy",
                 List.of(),
                 "Copied template",
                 "temporary",
-                "tpl_create_source",
+                source.id(),
                 List.of("ops.production_readiness.view"),
                 "project",
                 Instant.parse("2026-06-14T00:00:00Z"),
                 Instant.parse("2026-06-15T00:00:00Z"),
                 true
         ));
-        service.updatePermissionTemplate("tpl_create_source", new PermissionTemplateRequest(
+        service.updatePermissionTemplate(source.id(), new PermissionTemplateRequest(
                 "update-source",
                 "update",
                 List.of(),
                 "Source template updated",
-                "temporary",
+                "base",
                 null,
-                List.of("ops.production_readiness.check"),
+                List.of("ops.production_readiness.view"),
                 "project",
                 Instant.parse("2026-06-14T00:00:00Z"),
                 Instant.parse("2026-06-15T00:00:00Z"),
@@ -157,7 +167,9 @@ class AccessServiceTests {
 
     @Test
     void applyPermissionTemplateAppliesToManyUsersAndWritesAuditEvent() {
-        service.createPermissionTemplate(new PermissionTemplateRequest(
+        accessRepository.createUser(new AccessUser("usr_1", "one", "staff", "seed-org", null, "active", Instant.now()));
+        accessRepository.createUser(new AccessUser("usr_2", "two", "staff", "seed-org", null, "active", Instant.now()));
+        PermissionTemplate template = service.createPermissionTemplate(new PermissionTemplateRequest(
                 "create-apply",
                 "seed",
                 List.of(),
@@ -171,11 +183,11 @@ class AccessServiceTests {
                 true
         ));
 
-        PermissionTemplateApplyResult result = service.applyPermissionTemplate("tpl_create_apply", new PermissionTemplateApplyRequest(
+        PermissionTemplateApplyResult result = service.applyPermissionTemplate(template.id(), new PermissionTemplateApplyRequest(
                 "apply-many",
                 "apply",
                 List.of(),
-                "tpl_create_apply",
+                template.id(),
                 List.of("usr_1", "usr_2"),
                 "merge"
         ));
@@ -194,6 +206,7 @@ class AccessServiceTests {
 
     @Test
     void temporaryGrantCreateAndRevokeWriteAuditEvents() {
+        accessRepository.createUser(new AccessUser("usr_1", "one", "staff", "seed-org", null, "active", Instant.now()));
         TemporaryGrant grant = service.createTemporaryGrant(new TemporaryGrantRequest(
                 "grant-once",
                 "grant",
@@ -242,13 +255,13 @@ class AccessServiceTests {
 
     @Test
     void createAccessUserWithAllowedAccountTypeSucceeds() {
-        for (String type : List.of("staff", "merchant", "supervisor", "sales", "merchant_admin", "outsourced_vendor")) {
+        for (String type : List.of("outsourced_vendor")) {
             AccessUser user = service.createAccessUser(new AccessUserRequest(
                     "user-type-" + type,
                     null,
                     List.of(),
                     "user_" + type,
-                    null,
+                    "password",
                     null,
                     type,
                     "seed-org",
@@ -275,9 +288,9 @@ class AccessServiceTests {
                 null,
                 List.of(),
                 "cross_merchant_by_admin",
+                "password",
                 null,
-                null,
-                "merchant",
+                "outsourced_vendor",
                 "seed-org",
                 "merchant-other",
                 "active"
@@ -313,14 +326,14 @@ class AccessServiceTests {
                 "manual_evidence",
                 "change-me-123",
                 "13800000002",
-                "sales",
+                "outsourced_vendor",
                 "seed-org",
                 "merchant-1",
                 "active",
                 "consent_staff_manual_003"
         ));
 
-        assertThat(user.phoneHash()).hasSize(64).doesNotContain("13800000002");
+        assertThat(user.phoneHash()).isNull();
         assertThat(user.createdVia()).isEqualTo("manual_admin");
         assertThat(user.consentReference()).isEqualTo("consent_staff_manual_003");
         assertThat(user.evidenceAttachmentIds()).containsExactly("ev_consent_manual_003", "ev_hr_ticket_001");
@@ -361,8 +374,7 @@ class AccessServiceTests {
 
     @Test
     void assignStaffLevelSetsLevelAndDefaultsCurrencyToCny() {
-        service.createAccessUser(new AccessUserRequest(
-                "k-u-emp1", null, List.of(), "emp1", null, null, "staff", "seed-org", null, "active"));
+        accessRepository.createUser(new AccessUser("usr_emp1", "emp1", "staff", "seed-org", null, "active", Instant.now()));
         StaffLevelAssignment assignment = service.assignStaffLevel(new StaffLevelAssignmentRequest(
                 "k-a-emp1", null, "usr_emp1", "lvl_m10_upper", 1_500_000L, null, "月薪 15000"));
         assertThat(assignment.staffLevelId()).isEqualTo("lvl_m10_upper");
@@ -376,8 +388,7 @@ class AccessServiceTests {
 
     @Test
     void assignStaffLevelRejectsUnknownLevel() {
-        service.createAccessUser(new AccessUserRequest(
-                "k-u-emp2", null, List.of(), "emp2", null, null, "staff", "seed-org", null, "active"));
+        accessRepository.createUser(new AccessUser("usr_emp2", "emp2", "staff", "seed-org", null, "active", Instant.now()));
         assertThatThrownBy(() -> service.assignStaffLevel(new StaffLevelAssignmentRequest(
                 "k-a-emp2", null, "usr_emp2", "lvl_does_not_exist", null, null, null)))
                 .isInstanceOfSatisfying(ApiException.class, e -> {
@@ -388,8 +399,7 @@ class AccessServiceTests {
 
     @Test
     void assignStaffLevelRejectsNonCnyCurrency() {
-        service.createAccessUser(new AccessUserRequest(
-                "k-u-emp3", null, List.of(), "emp3", null, null, "staff", "seed-org", null, "active"));
+        accessRepository.createUser(new AccessUser("usr_emp3", "emp3", "staff", "seed-org", null, "active", Instant.now()));
         assertThatThrownBy(() -> service.assignStaffLevel(new StaffLevelAssignmentRequest(
                 "k-a-emp3", null, "usr_emp3", "lvl_m1_lower", 100L, "USD", null)))
                 .isInstanceOfSatisfying(ApiException.class, e -> {
@@ -477,7 +487,8 @@ class AccessServiceTests {
         assertThat(result.roleId()).isEqualTo("role_ops_check");
         assertThat(result.templateId()).isEqualTo("tpl_ops_check");
         assertThat(result.templateApplicationCount()).isEqualTo(1);
-        assertThat(accessRepository.userRoles.get("usr_position_target")).contains("role_ops_check");
+        assertThat(accessRepository.positionRoles.get("usr_position_target")).isEqualTo("role_ops_check");
+        assertThat(accessRepository.userRoles.getOrDefault("usr_position_target", Set.of())).doesNotContain("role_ops_check");
         assertThat(accessRepository.findUserPositionAssignment("usr_position_target")).get()
                 .extracting(UserPositionAssignment::positionCode, UserPositionAssignment::staffLevelId)
                 .containsExactly("ops_manager", "lvl_m1_lower");
@@ -569,6 +580,22 @@ class AccessServiceTests {
         assertThat(accessRepository.findUserPositionAssignment("usr_position_high_risk")).isEmpty();
     }
 
+
+    // M006-016：混合风险中有未配置等级时，不能忽略它并按另一个较低等级放行。
+    @Test void grantPolicyRejectsAnyUnclassifiedPermissionInTheRequestedRole() {
+        accessRepository.markRequiresApproval("ops.production_readiness.check", "high");
+        accessRepository.addPermission("future.permission", "unclassified", true);
+        service = newAccessService(true, supervisorReviewService(new InMemorySupervisorReviewRepository()));
+        accessRepository.createUser(new AccessUser("target", "target", "staff", "seed-org", null, "active", Instant.now()));
+        accessRepository.roles.put("mixed", new RoleRecord("mixed", "mixed", null, "project", "active", Instant.now(),
+                List.of("ops.production_readiness.check", "future.permission"), null));
+        assertThatThrownBy(() -> service.applyGrant(new AccessGrantApplyRequest(
+                "mixed-risk", null, List.of(), "user_role", "target", "mixed",
+                null, null, null, null, null, null, null, null, false, "reason")))
+                .isInstanceOf(ApiException.class);
+        assertThat(accessRepository.grantRequests).isEmpty();
+    }
+
     @Test
     void grantRequestRejectsPermissionsThatDoNotRequireApproval() {
         InMemorySupervisorReviewRepository reviewRepository = new InMemorySupervisorReviewRepository();
@@ -715,7 +742,8 @@ class AccessServiceTests {
         accessRepository.grantRequests.put("agr_summary_2", temporaryGrantRequest(
                 "agr_summary_2", "usr_summary", "cashier", "lvl_m1_lower", "ops.production_readiness.check", now));
 
-        AccessGrantSummary summary = service.grantSummary(30, 2, 2);
+        accessRepository.policy = accessRepository.policy.replace("\"value\":3", "\"value\":2");
+        AccessGrantSummary summary = service.grantSummary(null, null, null);
 
         assertThat(summary.positionRecommendations())
                 .extracting(AccessGrantPositionSummary::positionCode,
@@ -727,13 +755,17 @@ class AccessServiceTests {
         assertThat(summary.userAnomalies())
                 .extracting(AccessGrantUserSummary::userId, AccessGrantUserSummary::requestCount)
                 .contains(org.assertj.core.groups.Tuple.tuple("usr_summary", 2L));
+        accessRepository.policy = accessRepository.policy.replace("\"value\":2", "\"value\":3");
+        AccessGrantSummary higherThreshold = service.grantSummary(null, null, null);
+        assertThat(higherThreshold.positionRecommendations()).isEmpty();
+        assertThat(higherThreshold.userAnomalies()).isEmpty();
     }
 
     // ── P1: applyPermissionTemplate 跨组织校验 ─────────────────────────────
 
     @Test
     void applyPermissionTemplateWithCrossOrgTargetUserThrowsForbidden() {
-        service.createPermissionTemplate(new PermissionTemplateRequest(
+        PermissionTemplate template = service.createPermissionTemplate(new PermissionTemplateRequest(
                 "create-cross-apply",
                 "seed",
                 List.of(),
@@ -754,12 +786,12 @@ class AccessServiceTests {
         // Switch to non-platform-admin actor in org_a
         authenticateAsOrgUser("usr_a", "user-a", "org_a");
 
-        assertThatThrownBy(() -> service.applyPermissionTemplate("tpl_create_cross_apply",
+        assertThatThrownBy(() -> service.applyPermissionTemplate(template.id(),
                 new PermissionTemplateApplyRequest(
                         "apply-cross",
                         "apply",
                         List.of(),
-                        "tpl_create_cross_apply",
+                        template.id(),
                         List.of("usr_cross_org"),
                         "merge"
                 ))).isInstanceOfSatisfying(ApiException.class, e -> {
@@ -859,7 +891,7 @@ class AccessServiceTests {
     void applyPlatformOwnedTemplateByOrgUserToSameOrgUserIsForbidden() {
         // A1-05：apply 必须校验模板归属——非平台管理员不得套用平台/他组织模板（即便目标用户同组织），
         // 否则可借平台模板把高权限项下发给本组织用户提权。
-        service.createPermissionTemplate(new PermissionTemplateRequest(
+        PermissionTemplate template = service.createPermissionTemplate(new PermissionTemplateRequest(
                 "create-platform-tpl", "seed", List.of(), "Platform Template", "temporary",
                 null, List.of("ops.production_readiness.check"), "project",
                 Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-15T00:00:00Z"), false));
@@ -867,9 +899,9 @@ class AccessServiceTests {
                 "usr_org_a_same", "org-a-same", "staff", "org_a", null, "active",
                 Instant.parse("2026-06-14T00:00:00Z")));
         authenticateAsOrgUser("usr_org_a_actor2", "org-a-actor2", "org_a");
-        assertThatThrownBy(() -> service.applyPermissionTemplate("tpl_create_platform_tpl",
+        assertThatThrownBy(() -> service.applyPermissionTemplate(template.id(),
                 new PermissionTemplateApplyRequest("apply-platform", "apply", List.of(),
-                        "tpl_create_platform_tpl", List.of("usr_org_a_same"), "merge")))
+                        template.id(), List.of("usr_org_a_same"), "merge")))
                 .isInstanceOfSatisfying(ApiException.class, e -> {
             assertThat(e.status().value()).isEqualTo(403);
             assertThat(e.code()).isEqualTo("CROSS_ORG_FORBIDDEN");
@@ -978,7 +1010,7 @@ class AccessServiceTests {
     void listGrantRequestsFiltersRowsStampedWithOtherProjects() {
         // 2026-07-18 收尾（多综合体前置）：授权申请行带 project_id 时，项目员工只见本项目行（currentProjectScopeQuietly
         // 谓词范式）；行无 project_id 时沿用目标账号同组织判定；平台管理员全量。
-        Instant now = Instant.parse("2026-07-18T00:00:00Z");
+        Instant now = Instant.now();
         accessRepository.users.put("usr_scope_target", new AccessUser(
                 "usr_scope_target", "scope-target", "staff", "org_a", null, "active", now));
         accessRepository.saveGrantRequest(grantRequestRow("agr_scope_a", "usr_scope_target", "org_a", now));
@@ -1044,6 +1076,17 @@ class AccessServiceTests {
 
     private static final class InMemoryAccessRepository implements AccessRepository {
         final Map<String, PermissionDefinition> permissions = new LinkedHashMap<>();
+        String policy = "{\"windowDays\":{\"value\":30,\"min\":1,\"max\":2147483647},\"positionThreshold\":{\"value\":3,\"min\":1,\"max\":2147483647},\"userThreshold\":{\"value\":3,\"min\":1,\"max\":2147483647},\"riskReviews\":{\"low\":{\"approvalLevel\":\"one_level_up\",\"requiredLevel\":1},\"medium\":{\"approvalLevel\":\"one_level_up\",\"requiredLevel\":1},\"high\":{\"approvalLevel\":\"two_levels_up\",\"requiredLevel\":2},\"critical\":{\"approvalLevel\":\"top_authority\",\"requiredLevel\":3}}}";
+        final Map<String, String> fingerprints = new LinkedHashMap<>();
+        final Map<String, String> positionRoles = new LinkedHashMap<>();
+        @Override public void lockGrantAuthorizationFacts() {} // 单线程内存夹具；真实锁在 M006JdbcRegressionTests 验证。
+        @Override public Optional<String> findGrantPolicy() { return Optional.of(policy); }
+        @Override public void saveGrantFingerprint(String id, String fingerprint) {
+            if (fingerprints.putIfAbsent(id, fingerprint) != null) throw new IllegalStateException("fingerprint already set");
+        }
+        @Override public Optional<String> findGrantFingerprint(String id) { return Optional.ofNullable(fingerprints.get(id)); }
+        @Override public void replacePositionGrants(String userId, String roleId, String mappingId,
+                List<String> applicationIds, String actorId, Instant at) { positionRoles.put(userId, roleId); }
         final Map<String, PermissionTemplate> templates = new LinkedHashMap<>();
         final Map<String, List<PermissionTemplateItem>> items = new LinkedHashMap<>();
         final Map<String, TemporaryGrant> temporaryGrants = new LinkedHashMap<>();
diff --git a/backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java b/backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java
index 1856436a..12ff083b 100644
--- a/backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java
@@ -1,5 +1,7 @@
 package com.complex.platform.access;
 
+// 文件版本：V0.01
+
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
 
@@ -284,9 +286,9 @@ class AuthorizationServiceTests {
     }
 
     @Test
-    void effectivePermissionsMarksWildcardForActivePlatformAdmin() {
+    void effectivePermissionsDoesNotGiveAdministratorAnUnboundedWildcard() {
         authenticate("usr_admin", "seed-admin", "platform_admin");
-        assertThat(service.effectivePermissions().wildcard()).isTrue();
+        assertThat(service.effectivePermissions().wildcard()).isFalse();
     }
 
     @Test
diff --git a/backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java b/backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java
new file mode 100644
index 00000000..f66f8d4c
--- /dev/null
+++ b/backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java
@@ -0,0 +1,260 @@
+package com.complex.platform.access;
+
+// 文件版本：V0.10
+import static org.assertj.core.api.Assertions.*;
+import static org.junit.jupiter.api.Assumptions.assumeTrue;
+
+import java.sql.Connection;
+import java.sql.DriverManager;
+import java.time.Instant;
+import java.util.Map;
+import java.util.UUID;
+import org.junit.jupiter.api.AfterEach;
+import org.junit.jupiter.api.BeforeEach;
+import org.junit.jupiter.api.Test;
+import org.springframework.aop.framework.ProxyFactory;
+import org.springframework.jdbc.core.JdbcTemplate;
+import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
+import org.springframework.jdbc.datasource.DataSourceTransactionManager;
+import org.springframework.jdbc.datasource.SingleConnectionDataSource;
+import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
+import org.springframework.transaction.interceptor.TransactionInterceptor;
+
+/** M006-014/017：只用 complex_test 中连接内临时表；不修改共享业务表。 */
+class M006JdbcRegressionTests {
+    private Connection connection;
+    private JdbcTemplate jdbc;
+    private AccessRepository repository;
+
+    @BeforeEach void openTemporaryTables() throws Exception {
+        String url = System.getenv("COMPLEX_DATABASE_URL");
+        assumeTrue(url != null && url.matches("jdbc:postgresql:.*?/complex_test(?:\\?.*)?"));
+        connection = DriverManager.getConnection(url, System.getenv("COMPLEX_DATABASE_USERNAME"),
+                System.getenv("COMPLEX_DATABASE_PASSWORD"));
+        var dataSource = new SingleConnectionDataSource(connection, true);
+        jdbc = new JdbcTemplate(dataSource);
+        jdbc.execute("CREATE TEMP TABLE permission_templates (id varchar PRIMARY KEY, single_use boolean NOT NULL)");
+        jdbc.execute("""
+                CREATE TEMP TABLE permission_template_applications (
+                    id varchar PRIMARY KEY, template_id varchar, target_id varchar,
+                    single_use boolean NOT NULL, consumed boolean NOT NULL DEFAULT false,
+                    consumed_at timestamptz, status varchar DEFAULT 'active',
+                    valid_from timestamptz, valid_until timestamptz)
+                """);
+        jdbc.execute("""
+                CREATE TEMP TABLE temporary_permission_grants (
+                    id uuid PRIMARY KEY, subject_id varchar, permission_code varchar,
+                    single_use boolean NOT NULL, consumed boolean NOT NULL DEFAULT false,
+                    consumed_at timestamptz, status varchar DEFAULT 'active',
+                    valid_from timestamptz, expires_at timestamptz)
+                """);
+        jdbc.execute("""
+                CREATE TEMP TABLE permission_template_usage_logs (
+                    id uuid PRIMARY KEY, application_id varchar REFERENCES pg_temp.permission_template_applications(id),
+                    user_id varchar, permission_code varchar, result varchar, reason varchar, occurred_at timestamptz)
+                """);
+        jdbc.execute("CREATE TEMP TABLE access_grant_requests (LIKE public.access_grant_requests INCLUDING DEFAULTS INCLUDING CONSTRAINTS)");
+        jdbc.execute("ALTER TABLE pg_temp.access_grant_requests ADD COLUMN IF NOT EXISTS authorization_fingerprint varchar(64)");
+        var target = new JdbcAccessRepository(new NamedParameterJdbcTemplate(dataSource));
+        var factory = new ProxyFactory(target);
+        factory.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(dataSource),
+                new AnnotationTransactionAttributeSource()));
+        repository = (AccessRepository) factory.getProxy();
+    }
+
+    @AfterEach void closeConnection() throws Exception {
+        if (connection != null) connection.close();
+    }
+
+    @Test void temporaryGrantLogsItsActualSourceAndConsumesOnlyOnce() {
+        UUID id = temporaryGrant();
+        assertThat(repository.markSingleUsePermissionConsumed(id.toString(), "permission", "user", Instant.now())).isTrue();
+        Map<String, Object> log = jdbc.queryForMap("SELECT application_id, reason FROM permission_template_usage_logs");
+        assertThat(log.get("application_id")).isNull();
+        assertThat(log.get("reason").toString()).contains(id.toString());
+        assertThat(repository.markSingleUsePermissionConsumed(id.toString(), "permission", "user", Instant.now())).isFalse();
+        assertThat(jdbc.queryForObject("SELECT count(*) FROM permission_template_usage_logs", Integer.class)).isEqualTo(1);
+    }
+
+    @Test void auditFailureRollsBackConsumptionThroughActualSpringTransaction() {
+        UUID id = temporaryGrant();
+        jdbc.execute("ALTER TABLE permission_template_usage_logs ADD CONSTRAINT reject_log CHECK (result <> 'consumed')");
+        assertThatThrownBy(() -> repository.markSingleUsePermissionConsumed(
+                id.toString(), "permission", "user", Instant.now())).isInstanceOf(RuntimeException.class);
+        assertThat(jdbc.queryForObject("SELECT consumed FROM temporary_permission_grants WHERE id = ?",
+                Boolean.class, id)).isFalse();
+    }
+
+    @Test void inheritedTemplateSingleUseCanBeConsumed() {
+        jdbc.update("INSERT INTO permission_templates VALUES ('template', true)");
+        jdbc.update("""
+                INSERT INTO permission_template_applications (id, template_id, target_id, single_use)
+                VALUES ('application', 'template', 'user', false)
+                """);
+        assertThat(repository.markSingleUsePermissionConsumed("application", "permission", "user", Instant.now())).isTrue();
+        assertThat(jdbc.queryForObject("SELECT application_id FROM permission_template_usage_logs", String.class))
+                .isEqualTo("application");
+    }
+
+    // M006-008：读取待审记录须在服务事务内取得 PostgreSQL 行锁。
+    @Test void grantReviewReadLocksTheRequestRow() {
+        jdbc.update("INSERT INTO access_grant_requests (id, request_type) VALUES ('request', 'temporary_permission')");
+        new org.springframework.transaction.support.TransactionTemplate(
+                new DataSourceTransactionManager(jdbc.getDataSource())).execute(status -> {
+            assertThat(repository.findGrantRequestById("request")).isPresent();
+            assertThat(jdbc.queryForObject("""
+                    SELECT count(*) FROM pg_locks
+                     WHERE pid = pg_backend_pid()
+                       AND relation = 'pg_temp.access_grant_requests'::regclass
+                       AND mode = 'RowShareLock' AND granted
+                    """, Integer.class)).isGreaterThan(0);
+            return null;
+        });
+    }
+
+
+    // M006-006：定义和子项的写入锁保持到整个审批事务结束，防止比对后重新解析不同授权。
+    @Test void grantFactsRemainStableUntilTheGrantTransactionCommits() throws Exception {
+        var tables = java.util.List.of("access_users", "roles", "role_permissions", "permissions",
+                "permission_template_items", "position_role_templates", "staff_levels");
+        for (String table : tables) jdbc.execute("CREATE TEMP TABLE " + table + " (id varchar)");
+        new org.springframework.transaction.support.TransactionTemplate(
+                new DataSourceTransactionManager(jdbc.getDataSource())).execute(status -> {
+            org.springframework.test.util.ReflectionTestUtils.invokeMethod(repository, "lockGrantAuthorizationFacts");
+            for (String table : java.util.stream.Stream.concat(tables.stream(), java.util.stream.Stream.of("permission_templates")).toList()) {
+                assertThat(jdbc.queryForObject("""
+                        SELECT count(*) FROM pg_locks WHERE pid=pg_backend_pid()
+                          AND relation=to_regclass(?) AND mode='ShareLock' AND granted
+                        """, Integer.class, "pg_temp." + table)).isGreaterThan(0);
+            }
+            return null;
+        });
+        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_locks WHERE pid=pg_backend_pid() AND mode='ShareLock'",
+                Integer.class)).isZero();
+    }
+
+
+    // M006-006/009/016/026：执行完整迁移 SQL，仅作用于本连接临时表；验证政策可持久化修改。
+    @Test void migrationAppliesAndGrantPolicyRoundTripsInPostgres() throws Exception {
+        jdbc.execute("ALTER TABLE pg_temp.access_grant_requests DROP COLUMN authorization_fingerprint");
+        for (String table : java.util.List.of("supervisor_review_policies", "position_role_templates", "user_position_assignments", "roles")) {
+            jdbc.execute("CREATE TEMP TABLE " + table + " (LIKE public." + table + " INCLUDING ALL)");
+        }
+        jdbc.execute(java.nio.file.Files.readString(java.nio.file.Path.of(
+                "backend/src/main/resources/db/migration/V386__access_grant_authorization_fingerprint.sql")));
+        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
+        var initial = mapper.readValue(repository.findGrantPolicy().orElseThrow(), AccessGrantPolicy.class);
+        assertThat(initial.windowDays().value()).isEqualTo(30);
+        String changed = mapper.writeValueAsString(new AccessGrantPolicy(
+                new AccessGrantPolicy.BoundedInt(7, 1, 30), new AccessGrantPolicy.BoundedInt(2, 1, 20),
+                new AccessGrantPolicy.BoundedInt(4, 1, 20), initial.riskReviews()));
+        repository.saveGrantPolicy(changed);
+        var reloaded = mapper.readValue(repository.findGrantPolicy().orElseThrow(), AccessGrantPolicy.class);
+        assertThat(reloaded.windowDays().value()).isEqualTo(7);
+        assertThat(reloaded.positionThreshold().value()).isEqualTo(2);
+        assertThat(reloaded.userThreshold().value()).isEqualTo(4);
+        assertThat(jdbc.queryForObject("SELECT count(*) FROM supervisor_review_policies WHERE policy_code='position_role_template_delete' AND approval_level='top_authority'",
+                Integer.class)).isEqualTo(1);
+        assertThatThrownBy(() -> new AccessGrantPolicy.BoundedInt(31, 1, 30)).isInstanceOf(com.complex.platform.common.api.ApiException.class);
+    }
+
+    // M006-006：快照可读取、不可覆盖；未记录快照明确返回空。
+    @Test void approvalFingerprintIsWriteOnceAndMissingIsExplicit() {
+        jdbc.update("INSERT INTO access_grant_requests (id, request_type) VALUES ('request', 'temporary_permission')");
+        assertThat(repository.findGrantFingerprint("request")).isEmpty();
+        repository.saveGrantFingerprint("request", "a".repeat(64));
+        assertThat(repository.findGrantFingerprint("request")).contains("a".repeat(64));
+        assertThatThrownBy(() -> repository.saveGrantFingerprint("request", "b".repeat(64)))
+                .isInstanceOf(IllegalStateException.class);
+        assertThat(repository.findGrantFingerprint("request")).contains("a".repeat(64));
+    }
+
+
+    // M006-009：换岗只撤销上一岗位派生应用，独立应用仍有效。
+    @Test void positionReplacementRevokesOnlyItsOwnPreviousApplications() {
+        jdbc.execute("ALTER TABLE permission_template_applications ADD COLUMN revoked_by varchar, ADD COLUMN revoked_at timestamptz");
+        jdbc.execute("CREATE TEMP TABLE user_position_assignments (user_id varchar PRIMARY KEY, assigned_role_id varchar, assigned_position_template_id varchar, position_application_ids varchar[] NOT NULL DEFAULT '{}')");
+        jdbc.update("INSERT INTO user_position_assignments VALUES ('user', 'old-role', 'old-mapping', ARRAY['old-position'])");
+        jdbc.update("""
+                INSERT INTO permission_template_applications (id, template_id, target_id, single_use)
+                VALUES ('old-position', 'template', 'user', false),
+                       ('independent', 'template', 'user', false),
+                       ('new-position', 'template', 'user', false)
+                """);
+        repository.replacePositionGrants("user", "new-role", "new-mapping", java.util.List.of("new-position"), "reviewer", Instant.now());
+        assertThat(jdbc.queryForObject("SELECT status FROM permission_template_applications WHERE id = 'old-position'", String.class)).isEqualTo("revoked");
+        assertThat(jdbc.queryForObject("SELECT status FROM permission_template_applications WHERE id = 'independent'", String.class)).isEqualTo("active");
+        assertThat(jdbc.queryForObject("SELECT status FROM permission_template_applications WHERE id = 'new-position'", String.class)).isEqualTo("active");
+        assertThat(jdbc.queryForObject("SELECT assigned_role_id FROM user_position_assignments", String.class)).isEqualTo("new-role");
+        assertThat(jdbc.queryForObject("SELECT position_application_ids[1] FROM user_position_assignments", String.class)).isEqualTo("new-position");
+    }
+
+
+    // M006-004：创建碰撞必须失败，不允许把现有对象移动到新组织。
+    @Test void createCollisionCannotOverwriteExistingUserOrRole() {
+        jdbc.execute("CREATE TEMP TABLE access_users (LIKE public.access_users INCLUDING ALL)");
+        jdbc.execute("CREATE TEMP TABLE roles (LIKE public.roles INCLUDING ALL)");
+        jdbc.execute("CREATE TEMP TABLE role_permissions (LIKE public.role_permissions INCLUDING ALL)");
+        var now = Instant.now();
+        var first = new AccessUser("existing", "first", "staff", "org-a", null, "active", now);
+        repository.createUser(first);
+        assertThatThrownBy(() -> repository.createUser(new AccessUser(
+                "existing", "second", "staff", "org-b", null, "active", now)))
+                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
+        assertThat(repository.findUserById("existing").orElseThrow().organizationId()).isEqualTo("org-a");
+        repository.createRole(new RoleRecord("role", "原角色", null, "project", "active", now, java.util.List.of(), "org-a"));
+        assertThatThrownBy(() -> repository.createRole(new RoleRecord(
+                "role", "新角色", null, "project", "active", now, java.util.List.of(), "org-b")))
+                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
+        assertThat(repository.findRoleById("role").orElseThrow().organizationId()).isEqualTo("org-a");
+    }
+
+
+    // M006-030/002：后续页可达；COUNT 与数据共用组织、商户和时间谓词。
+    @Test void grantPaginationKeepsScopeAndCanReachTheSecondPage() {
+        jdbc.execute("CREATE TEMP TABLE access_users (id varchar PRIMARY KEY, organization_id varchar)");
+        jdbc.update("INSERT INTO access_users VALUES ('target', 'org'), ('other-target', 'other-org')");
+        jdbc.update("""
+                INSERT INTO access_grant_requests (id, request_type, target_user_id, merchant_id)
+                SELECT 'request-' || i, 'temporary_permission', 'target', 'merchant' FROM generate_series(1, 55) i
+                """);
+        jdbc.update("INSERT INTO access_grant_requests (id, request_type, target_user_id, merchant_id) VALUES ('outside', 'temporary_permission', 'other-target', 'other-merchant')");
+        var page = repository.pageGrantRequests(Instant.now().minusSeconds(3600), "org", "merchant", 2, 50);
+        assertThat(page.total()).isEqualTo(55);
+        assertThat(page.items()).hasSize(5).allMatch(item -> "target".equals(item.targetUserId()));
+        assertThat(page.page()).isEqualTo(2);
+        assertThat(repository.pageGrantRequests(Instant.now().minusSeconds(3600), null, null, 1, 50).total()).isEqualTo(56);
+    }
+
+
+    // M006-026：真实 SQL 只更新生命周期，恢复后原角色与模板仍可查询。
+    @Test void positionTemplateSoftDeleteCanBeRestoredWithoutLosingItsDefinition() {
+        jdbc.execute("CREATE TEMP TABLE position_role_templates (LIKE public.position_role_templates INCLUDING ALL)");
+        jdbc.execute("""
+                ALTER TABLE pg_temp.position_role_templates
+                    ADD COLUMN archived_at timestamptz, ADD COLUMN archived_by varchar,
+                    ADD COLUMN deleted_at timestamptz, ADD COLUMN deleted_by varchar,
+                    ADD COLUMN delete_review_id varchar, ADD COLUMN delete_reason text
+                """);
+        var original = new PositionRoleTemplate("mapping", "position", "level", "role", "template",
+                null, "active", "creator", Instant.now(), Instant.now());
+        repository.savePositionRoleTemplate(original);
+        var deleted = original.withLifecycle(Instant.now(), Instant.now(), "review", "批准删除", Instant.now());
+        repository.savePositionTemplateLifecycle(deleted, "master");
+        assertThat(repository.findPositionRoleTemplate(null, "position", "level")).isEmpty();
+        assertThat(repository.findPositionRoleTemplateById("mapping").orElseThrow().deletedAt()).isNotNull();
+        repository.savePositionTemplateLifecycle(deleted.withLifecycle(null, null, null, null, Instant.now()), "master");
+        var restored = repository.findPositionRoleTemplateById("mapping").orElseThrow();
+        assertThat(restored.deletedAt()).isNull();
+        assertThat(restored.roleId()).isEqualTo("role");
+        assertThat(restored.templateId()).isEqualTo("template");
+        assertThat(restored.status()).isEqualTo("inactive");
+        assertThatThrownBy(() -> repository.deletePositionRoleTemplate("mapping")).isInstanceOf(IllegalStateException.class);
+    }
+
+    private UUID temporaryGrant() {
+        UUID id = UUID.randomUUID();
+        jdbc.update("INSERT INTO temporary_permission_grants (id, subject_id, permission_code, single_use) VALUES (?, 'user', 'permission', true)", id);
+        return id;
+    }
+}
diff --git a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
new file mode 100644
index 00000000..cbf28cd3
--- /dev/null
+++ b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
@@ -0,0 +1,595 @@
+package com.complex.platform.access;
+
+// 文件版本：V0.19
+import static org.assertj.core.api.Assertions.assertThat;
+import static org.assertj.core.api.Assertions.assertThatThrownBy;
+import static org.mockito.ArgumentMatchers.*;
+import static org.mockito.Mockito.*;
+
+import com.complex.platform.access.dto.*;
+import com.complex.platform.common.api.ApiException;
+import com.complex.platform.common.idempotency.IdempotencyService;
+import com.complex.platform.evidence.AuditEventService;
+import com.complex.platform.support.InMemoryAuditEventRepository;
+import com.complex.platform.support.InMemoryIdempotencyRepository;
+import com.fasterxml.jackson.databind.ObjectMapper;
+import java.time.Instant;
+import java.util.*;
+import org.junit.jupiter.api.AfterEach;
+import org.junit.jupiter.api.BeforeEach;
+import org.junit.jupiter.api.Test;
+import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
+import org.springframework.security.core.context.SecurityContextHolder;
+
+class M006SecurityRegressionTests {
+    private static final String LOW = "ops.production_readiness.view";
+    private static final String HIGH = "finance.refund.create";
+    private AccessRepository repository;
+    private AccessCredentialProvisioner credentials;
+    private AuthorizationService authorization;
+    private AccessService service;
+    private final Map<String, AccessUser> users = new LinkedHashMap<>();
+    private final Map<String, RoleRecord> roles = new LinkedHashMap<>();
+    private final Map<String, PermissionTemplate> templates = new LinkedHashMap<>();
+    private final Map<String, List<PermissionTemplateItem>> items = new LinkedHashMap<>();
+    private final List<AccessRepository.PermissionEffect> effects = new ArrayList<>();
+    private final Set<String> consumed = new LinkedHashSet<>();
+    private final Map<String, AccessGrantRequestRecord> requests = new LinkedHashMap<>();
+    private final Map<String, String> fingerprints = new LinkedHashMap<>();
+    private final Map<String, Set<String>> directRoles = new LinkedHashMap<>();
+    private final Map<String, String> positionRoles = new LinkedHashMap<>();
+    private final Map<String, PositionRoleTemplate> positionTemplates = new LinkedHashMap<>();
+
+    @BeforeEach void setUp() {
+        repository = mock(AccessRepository.class, invocation -> switch (invocation.getMethod().getName()) {
+            case "lockGrantAuthorizationFacts" -> null;
+            case "saveGrantFingerprint" -> { fingerprints.put(invocation.getArgument(0), invocation.getArgument(1)); yield null; }
+            case "findGrantFingerprint" -> Optional.ofNullable(fingerprints.get(invocation.getArgument(0)));
+            case "replacePositionGrants" -> { positionRoles.put(invocation.getArgument(0), invocation.getArgument(1)); yield null; }
+            default -> CALLS_REAL_METHODS.answer(invocation);
+        });
+        when(repository.findGrantPolicy()).thenReturn(Optional.of("{\"windowDays\":{\"value\":30,\"min\":1,\"max\":2147483647},\"positionThreshold\":{\"value\":3,\"min\":1,\"max\":2147483647},\"userThreshold\":{\"value\":3,\"min\":1,\"max\":2147483647},\"riskReviews\":{\"low\":{\"approvalLevel\":\"one_level_up\",\"requiredLevel\":1},\"medium\":{\"approvalLevel\":\"one_level_up\",\"requiredLevel\":1},\"high\":{\"approvalLevel\":\"two_levels_up\",\"requiredLevel\":2},\"critical\":{\"approvalLevel\":\"top_authority\",\"requiredLevel\":3}}}"));
+        credentials = mock(AccessCredentialProvisioner.class);
+        when(repository.listPermissions()).thenReturn(List.of(
+                new PermissionDefinition(LOW, "查看", "ops", "medium", false, "active"),
+                new PermissionDefinition(HIGH, "退款", "finance", "high", true, "active")));
+        when(repository.permissionExists(anyString())).thenAnswer(a ->
+                repository.listPermissions().stream().anyMatch(p -> p.permissionCode().equals(a.getArgument(0))));
+        when(repository.findPermissionEffects(anyString(), any())).thenAnswer(a -> {
+            String id = a.getArgument(0);
+            List<AccessRepository.PermissionEffect> result = new ArrayList<>(effects.stream()
+                    .filter(e -> e.userId().equals(id) && !consumed.contains(e.sourceId())).toList());
+            Set<String> assigned = new LinkedHashSet<>(directRoles.getOrDefault(id, Set.of()));
+            if (positionRoles.containsKey(id)) assigned.add(positionRoles.get(id));
+            for (String roleId : assigned) for (String code : roles.get(roleId).permissionCodes()) {
+                result.add(new AccessRepository.PermissionEffect(id, code, AccessRepository.Effect.GRANT,
+                        AccessRepository.Source.ROLE, roleId, null, null, false, false));
+            }
+            return result;
+        });
+        when(repository.markSingleUsePermissionConsumed(anyString(), anyString(), anyString(), any()))
+                .thenAnswer(a -> consumed.add(a.getArgument(0)));
+        when(repository.findUserById(anyString())).thenAnswer(a -> Optional.ofNullable(users.get(a.getArgument(0))));
+        when(repository.listUsers()).thenAnswer(a -> new ArrayList<>(users.values()));
+        when(repository.createUser(any())).thenAnswer(a -> {
+            AccessUser u = a.getArgument(0); users.put(u.id(), u); return u;
+        });
+        when(repository.createRole(any())).thenAnswer(a -> {
+            RoleRecord r = a.getArgument(0); roles.put(r.id(), r); return r;
+        });
+        when(repository.createTemporaryGrant(any())).thenAnswer(a -> a.getArgument(0));
+        doAnswer(a -> {
+            directRoles.computeIfAbsent(a.getArgument(0), key -> new LinkedHashSet<>()).add(a.getArgument(1));
+            return new RoleAssignmentRecord(a.getArgument(0), a.getArgument(1));
+        }).when(repository).assignUserRole(anyString(), anyString());
+        when(repository.findRoleById(anyString())).thenAnswer(a -> Optional.ofNullable(roles.get(a.getArgument(0))));
+        when(repository.findTemplateById(anyString())).thenAnswer(a -> Optional.ofNullable(templates.get(a.getArgument(0))));
+        when(repository.findTemplateItems(anyString())).thenAnswer(a -> items.getOrDefault(a.getArgument(0), List.of()));
+        when(repository.saveTemplate(any(), anyList())).thenAnswer(a -> {
+            PermissionTemplate t = a.getArgument(0); templates.put(t.id(), t); items.put(t.id(), a.getArgument(1)); return t;
+        });
+        doAnswer(a -> { AccessGrantRequestRecord r = a.getArgument(0); requests.put(r.id(), r); return r; })
+                .when(repository).saveGrantRequest(any());
+        when(repository.findGrantRequestById(anyString())).thenAnswer(a -> Optional.ofNullable(requests.get(a.getArgument(0))));
+        authorization = new AuthorizationService(repository);
+        service = new AccessService(repository,
+                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
+                new AuditEventService(new InMemoryAuditEventRepository()), authorization, credentials, null, true, 3, 3);
+        actor("admin", "platform_admin", "org", null);
+    }
+
+    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }
+
+    // M006-001：同一管理员的执行、布尔查询和有效集都必须服从 DENY。
+    @Test void administratorDenyWinsAndHighPermissionNeedsExplicitGrant() {
+        effect("admin", LOW, AccessRepository.Effect.DENY, "override", false);
+        assertDenied(() -> authorization.requirePermission(LOW));
+        assertThat(authorization.currentActorHasPermission(LOW)).isFalse();
+        assertDenied(() -> authorization.requirePermission(HIGH));
+        assertThat(authorization.effectivePermissions().wildcard()).isFalse();
+        effect("admin", HIGH, AccessRepository.Effect.GRANT, "approved-role", false);
+        authorization.requirePermission(HIGH);
+    }
+
+    // M006-001：日常自动授予必须保留，高危定义不因默认管理员角色而获得。
+    @Test void administratorKeepsDailyPermissionsWithoutAutomaticHighRole() {
+        authorization.requirePermission(LOW);
+        effect("admin", HIGH, AccessRepository.Effect.GRANT, "role_platform_admin", false);
+        assertDenied(() -> authorization.requirePermission(HIGH));
+    }
+
+    // M006-028：Spring 的未知 principal 不能凭空变成启用员工。
+    @Test void untrustedPrincipalIsRejected() {
+        SecurityContextHolder.getContext().setAuthentication(
+                new UsernamePasswordAuthenticationToken("someone", "unused", List.of()));
+        assertThatThrownBy(authorization::currentActor).isInstanceOf(ApiException.class);
+    }
+
+    // M006-003：缺组织并非全局授权。
+    @Test void missingOrganizationCannotListAllUsersOrResolveGlobalProject() {
+        actor("orphan", "staff", null, null);
+        users.put("victim", user("victim", "other-org", null));
+        assertThatThrownBy(service::listAccessUsers).isInstanceOf(ApiException.class);
+        assertThatThrownBy(authorization::currentProjectScopeQuietly).isInstanceOf(ApiException.class);
+    }
+
+    // M006-002/012：同组织公司管理员可查；商户管理员仍不能跨商户。
+    @Test void organizationReadWorksButCrossMerchantWriteDoesNot() {
+        users.put("target", user("target", "org", "merchant-b"));
+        actor("operator", "staff", "org", null);
+        assertThat(authorization.effectivePermissionsForUser("target").userId()).isEqualTo("target");
+        actor("operator", "merchant_admin", "org", "merchant-a");
+        roles.put("daily", new RoleRecord("daily", "日常", null, "merchant", "active",
+                Instant.now(), List.of(LOW), "org"));
+        effect("operator", LOW, AccessRepository.Effect.GRANT, "own-role", false);
+        assertThatThrownBy(() -> service.assignUserRole(new RoleAssignmentRequest("cross", null, "target", "daily")))
+                .isInstanceOf(ApiException.class);
+        verify(repository, never()).assignUserRole("target", "daily");
+    }
+
+    // M006-004：可读名称的字符归一化不能覆盖另一个账号。
+    @Test void collidingUserSlugsRemainDifferentUsers() {
+        AccessUser first = service.createAccessUser(account("first", "same-name", "active", "password"));
+        AccessUser second = service.createAccessUser(account("second", "same.name", "active", "password"));
+        assertThat(second.id()).isNotEqualTo(first.id());
+        assertThat(users).hasSize(2);
+    }
+
+    // M006-005：已应用模板的内容编辑不能把不持有的高危权限发出去。
+    @Test void templateEditCannotGrantAnUnownedHighPermission() {
+        actor("manager", "staff", "org", null);
+        effect("manager", LOW, AccessRepository.Effect.GRANT, "own", false);
+        PermissionTemplate t = service.createPermissionTemplate(template("create", LOW));
+        assertThatThrownBy(() -> service.updatePermissionTemplate(t.id(), template("edit", HIGH)))
+                .isInstanceOf(ApiException.class);
+        assertThat(items.get(t.id())).extracting(PermissionTemplateItem::permissionCode).containsExactly(LOW);
+    }
+
+    // M006-018：管理员改内容不转移模板所有权；不存在的模板不隐式创建。
+    @Test void templateEditPreservesOwnerAndRejectsMissingTemplate() {
+        actor("manager", "staff", "org", null);
+        effect("manager", LOW, AccessRepository.Effect.GRANT, "own", false);
+        PermissionTemplate t = service.createPermissionTemplate(template("create", LOW));
+        actor("admin", "platform_admin", "org", null);
+        assertThat(service.updatePermissionTemplate(t.id(), template("edit", LOW)).ownerScopeId()).isEqualTo("org");
+        assertThatThrownBy(() -> service.updatePermissionTemplate("missing", template("missing", LOW)))
+                .isInstanceOf(ApiException.class);
+    }
+
+    // M006-013：必须有目标、权限及有序时间窗。
+    @Test void emptyPermissionsAndInvertedWindowAreRejectedBeforeGrantWrite() {
+        users.put("target", user("target", "org", null));
+        assertThatThrownBy(() -> service.createTemporaryGrant(new TemporaryGrantRequest(
+                "empty", null, List.of(), "target", List.of(), null, Instant.now().plusSeconds(3600), false, "理由")))
+                .isInstanceOf(ApiException.class);
+        assertThatThrownBy(() -> service.createTemporaryGrant(new TemporaryGrantRequest(
+                "reverse", null, List.of(), "target", List.of(LOW),
+                Instant.now().plusSeconds(3600), Instant.now(), false, "理由")))
+                .isInstanceOf(ApiException.class);
+        verify(repository, never()).createTemporaryGrant(any());
+    }
+
+    // M006-024/029：验证必须早于任何用户/凭证写入。
+    @Test void invalidStatusAndBlankPasswordHaveNoWrites() {
+        assertThatThrownBy(() -> service.createAccessUser(account("bad-status", "bad-status", "typo", "password")))
+                .isInstanceOf(ApiException.class);
+        assertThatThrownBy(() -> service.createAccessUser(account("no-password", "no-password", "active", " ")))
+                .isInstanceOf(ApiException.class);
+        verify(repository, never()).createUser(any());
+        verifyNoInteractions(credentials);
+    }
+
+    // M006-025：无消费用途的重复手机号不能继续被 SHA256 摘要存储和返回。
+    @Test void manualAccountDoesNotPersistOrReturnPhoneDigest() {
+        AccessUser created = service.createAccessUser(new AccessUserRequest("phone", null, List.of(),
+                "vendor", "password", "13800000000", "outsourced_vendor", "org", null, "active"));
+        assertThat(created.phoneHash()).isNull();
+    }
+
+    // M006-010：管理员身份本身不能形成最终审批级别。
+    @Test void administratorWithoutApprovalRoleHasNoSupervisorLevel() {
+        assertThat(authorization.currentSupervisorLevel()).isZero();
+    }
+
+    // M006-014：一个复核动作的两个权限先整体核验，只消耗同一来源一次。
+    @Test void twoPermissionReviewConsumesOneSourceOnlyAfterBothChecksPass() {
+        String first = "access.grant_request.review", second = "governance.supervisor_review.approve";
+        when(repository.listPermissions()).thenReturn(List.of(
+                new PermissionDefinition(first, "授权复核", "access", "high", true, "active"),
+                new PermissionDefinition(second, "主管复核", "governance", "high", true, "active")));
+        actor("reviewer", "staff", "org", null);
+        effect("reviewer", first, AccessRepository.Effect.GRANT, "single-application", true);
+        AccessService target = mock(AccessService.class);
+        AccessController controller = new AccessController(target, authorization);
+        AccessGrantReviewRequest request = new AccessGrantReviewRequest("review", null, List.of(), "approve", "同意");
+        assertThatThrownBy(() -> controller.reviewGrant("pending", request)).isInstanceOf(ApiException.class);
+        assertThat(consumed).isEmpty();
+        effect("reviewer", second, AccessRepository.Effect.GRANT, "single-application", true);
+        controller.reviewGrant("pending", request);
+        verify(target).reviewGrant("pending", request);
+        assertThat(consumed).containsExactly("single-application");
+    }
+
+    // M006-024/029：凭证适配器自身也拒绝伪成功，待激活账号不能取得可用密码。
+    @Test void credentialAdapterDisablesInvitedAccountsAndRejectsBlankPassword() {
+        var jdbc = mock(org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate.class);
+        when(jdbc.update(anyString(), any(org.springframework.jdbc.core.namedparam.SqlParameterSource.class)))
+                .thenReturn(1);
+        var provisioner = new JdbcAccessCredentialProvisioner(jdbc);
+        provisioner.provisionCredential("user", "password", "invited", Instant.now());
+        var parameters = org.mockito.ArgumentCaptor.forClass(
+                org.springframework.jdbc.core.namedparam.SqlParameterSource.class);
+        verify(jdbc).update(anyString(), parameters.capture());
+        assertThat(parameters.getValue().getValue("status")).isEqualTo("disabled");
+        assertThatThrownBy(() -> provisioner.provisionCredential("user", " ", "active", Instant.now()))
+                .isInstanceOf(ApiException.class);
+        verifyNoMoreInteractions(jdbc);
+    }
+
+    // M006-029：开发适配器找不到账号，必须报告失败，不能静默略过。
+    @Test void developmentCredentialAdapterRejectsMissingAccount() {
+        var auth = mock(com.complex.platform.auth.AuthService.class);
+        var provisioner = new DevAccessCredentialProvisioner(auth, repository);
+        assertThatThrownBy(() -> provisioner.provisionCredential("missing", "password", "active", Instant.now()))
+                .isInstanceOf(ApiException.class);
+        verifyNoInteractions(auth);
+    }
+
+    // M006-006：申请时的角色含 A，待批期间变成 A+B，必须在审批写入前拒绝。
+    @Test void changedRoleCannotBeAppliedUnderAnOldApproval() {
+        var reviews = reviews();
+        users.put("target", user("target", "org", null));
+        roles.put("role", new RoleRecord("role", "角色", null, "project", "active", Instant.now(), List.of(HIGH), "org"));
+        actor("requester", "staff", "org", null);
+        AccessGrantRequestRecord pending = service.applyGrant(new AccessGrantApplyRequest("apply", null, List.of(),
+                "user_role", "target", "role", null, null, null, null, null, List.of(), null, null, false, "申请"));
+        roles.put("role", new RoleRecord("role", "角色", null, "global", "active", Instant.now(), List.of(HIGH, LOW), "org"));
+        actor("reviewer", "staff", "org", null);
+        effect("reviewer", HIGH, AccessRepository.Effect.GRANT, "approved", false);
+        effect("reviewer", LOW, AccessRepository.Effect.GRANT, "approved", false);
+        assertThatThrownBy(() -> service.reviewGrant(pending.id(),
+                new AccessGrantReviewRequest("review", null, List.of(), "approve", "同意")))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_SNAPSHOT_CHANGED"));
+        verify(reviews, never()).approve(anyString(), any());
+    }
+
+    // M006-007：目标用户不能先把自己的授权审批落为通过。
+    @Test void beneficiaryCannotApproveOwnGrant() {
+        var reviews = reviews();
+        users.put("beneficiary", user("beneficiary", "org", null));
+        actor("requester", "staff", "org", null);
+        AccessGrantRequestRecord pending = service.applyGrant(new AccessGrantApplyRequest("apply", null, List.of(),
+                "temporary_permission", "beneficiary", null, null, null, null, null, null,
+                List.of(HIGH), null, Instant.now().plusSeconds(3600), false, "申请"));
+        actor("beneficiary", "staff", "org", null);
+        effect("beneficiary", HIGH, AccessRepository.Effect.GRANT, "approved", false);
+        assertThatThrownBy(() -> service.reviewGrant(pending.id(),
+                new AccessGrantReviewRequest("review", null, List.of(), "approve", "同意")))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("BENEFICIARY_SELF_REVIEW_FORBIDDEN"));
+        verify(reviews, never()).approve(anyString(), any());
+    }
+
+    private com.complex.platform.governance.review.SupervisorReviewService reviews() {
+        var reviews = mock(com.complex.platform.governance.review.SupervisorReviewService.class);
+        var result = new com.complex.platform.governance.review.SupervisorReviewDecisionRecord(
+                "review", "permission_grant_l2", "permission_grant_l2", "access_grant_request",
+                "request", true, "approved", 2, 2, "approved", "理由", List.of(), Instant.now(), "requester",
+                "two_levels_up", 6);
+        when(reviews.blockForAdmission(anyString(), anyString(), anyString(), anyString(), anyString(), anyInt()))
+                .thenReturn(result);
+        when(reviews.approve(anyString(), any())).thenReturn(result);
+        service = new AccessService(repository,
+                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
+                new AuditEventService(new InMemoryAuditEventRepository()), authorization, credentials, reviews, true, 3, 3);
+        return reviews;
+    }
+
+    // M006-009：换岗撤销派生角色；同角色若另有明确授权，则仍保留。
+    @Test void positionChangeDropsDerivedRoleButKeepsIndependentAssignment() {
+        String other = "ops.production_readiness.check";
+        var definitions = new ArrayList<>(repository.listPermissions());
+        definitions.add(new PermissionDefinition(other, "检查", "ops", "medium", false, "active"));
+        when(repository.listPermissions()).thenReturn(definitions);
+        when(repository.findStaffLevelById("level")).thenReturn(Optional.of(
+                new StaffLevelRecord("level", "M1", "lower", 1, "职级", "active", Instant.now())));
+        doAnswer(a -> a.getArgument(0)).when(repository).saveStaffLevelAssignment(any());
+        doAnswer(a -> a.getArgument(0)).when(repository).saveUserPositionAssignment(any());
+        doAnswer(a -> { PositionRoleTemplate mapping = a.getArgument(0); positionTemplates.put(mapping.positionCode(), mapping); return mapping; })
+                .when(repository).savePositionRoleTemplate(any());
+        when(repository.findPositionRoleTemplate(any(), anyString(), anyString()))
+                .thenAnswer(a -> Optional.ofNullable(positionTemplates.get(a.getArgument(1))));
+        when(repository.applyTemplate(anyString(), anyList(), any(), any(), anyBoolean(), anyString())).thenReturn(List.of());
+        roles.put("old", new RoleRecord("old", "旧岗位", null, "project", "active", Instant.now(), List.of(LOW), "org"));
+        roles.put("new", new RoleRecord("new", "新岗位", null, "project", "active", Instant.now(), List.of(other), "org"));
+        actor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID, "platform_admin", "org", null);
+        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
+                new AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
+                        com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", "org", null, "active"),
+                "unused", List.of()));
+        PermissionTemplate template = service.createPermissionTemplate(template("empty"));
+        for (String code : List.of("old", "new")) service.createPositionRoleTemplate(
+                new PositionRoleTemplateRequest(code, "理由", code, "level", code, template.id(), "active"));
+        for (String id : List.of("derived", "independent")) {
+            users.put(id, user(id, "org", null));
+            if ("independent".equals(id)) repository.assignUserRole(id, "old");
+            for (String position : List.of("old", "new")) service.assignUserPosition(
+                    new UserPositionAssignmentRequest(id + position, "理由", id, position, "level", 0L, "CNY", null));
+        }
+        assertThat(repository.findPermissionEffects("derived", Instant.now()))
+                .extracting(AccessRepository.PermissionEffect::permissionCode).containsExactly(other);
+        assertThat(repository.findPermissionEffects("independent", Instant.now()))
+                .extracting(AccessRepository.PermissionEffect::permissionCode).containsExactlyInAnyOrder(LOW, other);
+    }
+
+
+    // M006-005：不增加权限码也可能扩权，延长已授权模板和取消一次性均须重新授权。
+    @Test void appliedTemplateCannotExtendTimeOrRemoveSingleUse() {
+        Instant end = Instant.now().plusSeconds(3600);
+        PermissionTemplate t = service.createPermissionTemplate(new PermissionTemplateRequest(
+                "limited", "理由", List.of(), "模板", "base", null, List.of(LOW), "project", null, end, true));
+        assertThatThrownBy(() -> service.updatePermissionTemplate(t.id(), new PermissionTemplateRequest(
+                "extend", "理由", List.of(), "模板", "base", null, List.of(LOW), "project", null, end.plusSeconds(3600), true)))
+                .isInstanceOf(ApiException.class);
+        assertThatThrownBy(() -> service.updatePermissionTemplate(t.id(), new PermissionTemplateRequest(
+                "repeat", "理由", List.of(), "模板", "base", null, List.of(LOW), "project", null, end, false)))
+                .isInstanceOf(ApiException.class);
+        assertThat(service.updatePermissionTemplate(t.id(), new PermissionTemplateRequest(
+                "reduce", "理由", List.of(), "模板", "base", null, List.of(), "project", null, end, true))
+                .permissionCodes()).isEmpty();
+    }
+
+    // M006-013：待审批申请也必须先校验时间窗，不能先生成复核单。
+    @Test void pendingTemporaryGrantRejectsInvertedTimeWindow() {
+        var reviews = reviews();
+        users.put("target", user("target", "org", null));
+        assertThatThrownBy(() -> service.applyGrant(new AccessGrantApplyRequest("inverted", null, List.of(),
+                "temporary_permission", "target", null, null, null, null, null, null,
+                List.of(HIGH), Instant.now().plusSeconds(7200), Instant.now().plusSeconds(3600), false, "申请")))
+                .isInstanceOf(ApiException.class);
+        verify(reviews, never()).blockForAdmission(anyString(), anyString(), anyString(), anyString(), anyString(), anyInt());
+    }
+
+    // M006-001/010：最高职级普通管理员不能代替 USB 最高权限；真实 USB 会话可审批。
+    @Test void topAuthorityReviewRequiresMasterSessionRatherThanRankAlone() {
+        var reviewRepository = mock(com.complex.platform.governance.review.SupervisorReviewRepository.class);
+        var pending = new com.complex.platform.governance.review.SupervisorReviewDecisionRecord(
+                "top", "permission_grant_l3", "permission_grant_l3", "access_grant_request", "request",
+                true, "pending", 3, 0, null, "理由", List.of(), Instant.now(), "requester", "top_authority", 30);
+        when(reviewRepository.findPendingSupervisorReviewById("top")).thenReturn(Optional.of(pending));
+        when(reviewRepository.saveSupervisorReview(any())).thenAnswer(a -> a.getArgument(0));
+        var reviews = new com.complex.platform.governance.review.SupervisorReviewService(authorization,
+                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
+                reviewRepository, new AuditEventService(new InMemoryAuditEventRepository()));
+        when(repository.findStaffLevelAssignment("admin")).thenReturn(Optional.of(new StaffLevelAssignment(
+                "admin", "top-level", 0L, "CNY", null, "org", null, "admin", Instant.now())));
+        when(repository.findStaffLevelById("top-level")).thenReturn(Optional.of(new StaffLevelRecord(
+                "top-level", "M10", "upper", 30, "最高职级", "active", Instant.now())));
+        effect("admin", "governance.supervisor_review.approve", AccessRepository.Effect.GRANT, "approval-role", false);
+        var request = new com.complex.platform.governance.review.SupervisorReviewApprovalRequest(
+                "top-admin", "同意", List.of(), null, "approved", "同意");
+        assertThatThrownBy(() -> reviews.approve("top", request)).isInstanceOf(ApiException.class);
+        verify(reviewRepository, never()).saveSupervisorReview(any());
+        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
+                new AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
+                        com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", null, null, "active"),
+                "unused", List.of()));
+        assertThat(reviews.approve("top", new com.complex.platform.governance.review.SupervisorReviewApprovalRequest(
+                "top-master", "同意", List.of(), null, "approved", "同意")).decision()).isEqualTo("approved");
+    }
+
+
+    // M006-011：员工必须经登记审批路径，手工账号接口仅保留外部协作机构。
+    @Test void manualEmployeeCreationCannotIssueCredentialsBeforeRegistrationApproval() {
+        for (String type : List.of("staff", "sales", "supervisor", "merchant", "merchant_admin")) {
+            assertThatThrownBy(() -> service.createAccessUser(new AccessUserRequest(type, "理由", List.of(),
+                    type, "password", null, type, "org", "merchant", "active")))
+                    .isInstanceOf(ApiException.class);
+        }
+        verify(repository, never()).createUser(any());
+        verifyNoInteractions(credentials);
+    }
+
+    // M006-025：历史摘要也不进入账号列表的 JSON。
+    @Test void accountJsonDoesNotExposeStoredPhoneDigest() throws Exception {
+        var value = new AccessUser("id", "name", "staff", "org", null, "active", Instant.now(),
+                "phone-digest", "manual_admin", null, List.of(), null);
+        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(value);
+        assertThat(json).doesNotContain("phoneHash", "phone_hash", "phone-digest");
+    }
+
+    // M006-029：凭证适配器不可用时必须在账号写入前失败。
+    @Test void missingCredentialAdapterCannotReportSuccessfulCreation() {
+        var withoutCredentials = new AccessService(repository,
+                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
+                new AuditEventService(new InMemoryAuditEventRepository()), authorization, null, null, true, 3, 3);
+        assertThatThrownBy(() -> withoutCredentials.createAccessUser(account("missing-adapter", "vendor", "active", "password")))
+                .isInstanceOf(ApiException.class);
+        verify(repository, never()).createUser(any());
+    }
+
+
+
+    // M006-011/029：员工审批也必须成功创建凭证，不能退化成静默空操作。
+    @Test void employeeApprovalWithoutCredentialAdapterWritesNoAccount() {
+        var staffRepository = mock(com.complex.platform.access.staff.JdbcStaffRepository.class);
+        when(staffRepository.saveProfile(any())).thenAnswer(a -> a.getArgument(0));
+        when(staffRepository.saveNotice(any())).thenAnswer(a -> a.getArgument(0));
+        @SuppressWarnings("unchecked") org.springframework.beans.factory.ObjectProvider<AccessCredentialProvisioner> provider =
+                mock(org.springframework.beans.factory.ObjectProvider.class, invocation ->
+                        invocation.getArguments().length == 1 && invocation.getArguments()[0] instanceof java.util.function.Supplier<?> supplier
+                                ? supplier.get() : RETURNS_DEFAULTS.answer(invocation));
+        var staff = new com.complex.platform.access.staff.StaffService(staffRepository, repository,
+                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
+                new AuditEventService(new InMemoryAuditEventRepository()), authorization, provider);
+        var profile = staff.submitRegistration(new com.complex.platform.access.staff.dto.StaffRegistrationRequest(
+                "no-credential-registration", null, "org", null, "员工", null, null, null));
+        when(staffRepository.findProfileById(profile.id())).thenReturn(Optional.of(profile));
+        assertThatThrownBy(() -> staff.reviewRegistration(profile.id(),
+                new com.complex.platform.access.staff.dto.StaffRegistrationReviewRequest(
+                        "no-credential-approval", null, "approve", null, "password"))).isInstanceOf(ApiException.class);
+        verify(repository, never()).createUser(any());
+        verify(staffRepository, times(1)).saveProfile(any());
+    }
+
+    // M006-011/002：登记不创建账号；商户不能自批，公司审批才开通并自动赋员工自助。
+    @Test void registrationCreatesAccountOnlyAfterCompanyApproval() {
+        var staffRepository = mock(com.complex.platform.access.staff.JdbcStaffRepository.class);
+        Map<String, com.complex.platform.access.staff.StaffProfileRecord> profiles = new HashMap<>();
+        when(staffRepository.saveProfile(any())).thenAnswer(a -> {
+            com.complex.platform.access.staff.StaffProfileRecord p = a.getArgument(0);
+            profiles.put(p.id(), p); return p;
+        });
+        when(staffRepository.findProfileById(anyString())).thenAnswer(a -> Optional.ofNullable(profiles.get(a.getArgument(0))));
+        when(staffRepository.saveNotice(any())).thenAnswer(a -> a.getArgument(0));
+        @SuppressWarnings("unchecked") org.springframework.beans.factory.ObjectProvider<AccessCredentialProvisioner> provider =
+                mock(org.springframework.beans.factory.ObjectProvider.class);
+        when(provider.getIfAvailable()).thenReturn(credentials);
+        var staff = new com.complex.platform.access.staff.StaffService(staffRepository, repository,
+                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
+                new AuditEventService(new InMemoryAuditEventRepository()), authorization, provider);
+        var registration = staff.submitRegistration(new com.complex.platform.access.staff.dto.StaffRegistrationRequest(
+                "registration", "后台辅助登记", "org", "merchant", "员工", null, null, null));
+        assertThat(registration.registrationStatus()).isEqualTo("submitted");
+        assertThat(registration.userId()).isNull();
+        verify(repository, never()).createUser(any());
+        verifyNoInteractions(credentials);
+        actor("merchant-operator", "merchant_admin", "org", "merchant");
+        assertThatThrownBy(() -> staff.reviewRegistration(registration.id(),
+                new com.complex.platform.access.staff.dto.StaffRegistrationReviewRequest(
+                        "merchant-review", null, "approve", null, "password"))).isInstanceOf(ApiException.class);
+        actor("company-reviewer", "staff", "org", null);
+        var approved = staff.reviewRegistration(registration.id(),
+                new com.complex.platform.access.staff.dto.StaffRegistrationReviewRequest(
+                        "company-review", null, "approve", null, "password"));
+        assertThat(approved.registrationStatus()).isEqualTo("approved");
+        assertThat(users.get(approved.userId()).organizationId()).isEqualTo("org");
+        verify(repository).assignUserRole(approved.userId(), "role_staff_self");
+        verify(credentials).provisionCredential(eq(approved.userId()), eq("password"), eq("active"), any());
+    }
+
+
+    // M006-016：只读查询参数不能改写异常判断阈值。
+    @Test void summaryQueryCannotOverrideBusinessThresholds() {
+        assertThatThrownBy(() -> service.grantSummary(null, Integer.MAX_VALUE, null))
+                .isInstanceOf(ApiException.class);
+    }
+
+    // M006-030：全量参考字典的分页元数据必须与实际返回条数一致。
+    @Test void referenceDictionaryDoesNotPretendItWasTruncatedAtFifty() {
+        List<PermissionDefinition> definitions = java.util.stream.IntStream.range(0, 55)
+                .mapToObj(i -> new PermissionDefinition("p" + i, "权限", "access", "low", false, "active")).toList();
+        when(repository.listPermissions()).thenReturn(definitions);
+        var result = service.listPermissions();
+        assertThat(result.items()).hasSize(55);
+        assertThat(result.pageSize()).isEqualTo(55);
+    }
+
+    // M006-030：授权申请默认读取配置窗口，不向仓储传无限时间范围。
+    @Test void grantListHasADefaultTimeWindow() {
+        service.listGrantRequests(null);
+        var cutoff = org.mockito.ArgumentCaptor.forClass(Instant.class);
+        verify(repository).listGrantRequests(cutoff.capture());
+        assertThat(cutoff.getValue()).isNotNull();
+    }
+
+
+    // M006-026：旧 DELETE 入口不能再物理删除配置。
+    @Test void directPositionTemplateDeleteCannotBypassArchiveAndApproval() {
+        var mapping = new PositionRoleTemplate("mapping", "position", "level", "role", "template",
+                null, "active", "creator", Instant.now(), Instant.now());
+        when(repository.findPositionRoleTemplateById("mapping")).thenReturn(Optional.of(mapping));
+        doNothing().when(repository).deletePositionRoleTemplate("mapping");
+        assertThatThrownBy(() -> service.deletePositionRoleTemplate("mapping")).isInstanceOf(ApiException.class);
+        verify(repository, never()).deletePositionRoleTemplate(anyString());
+    }
+
+
+    // M006-026：存档→申请→最高权限复核→软删除→恢复；占用和自审均被拒绝。
+    @Test void positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval() {
+        var mapping = new java.util.concurrent.atomic.AtomicReference<>(new PositionRoleTemplate(
+                "lifecycle", "position", "level", "role", "template", null, "active", "creator", Instant.now(), Instant.now()));
+        when(repository.findPositionRoleTemplateById("lifecycle")).thenAnswer(a -> Optional.of(mapping.get()));
+        when(repository.listPositionRoleTemplates(null)).thenAnswer(a -> List.of(mapping.get()));
+        doAnswer(a -> { mapping.set(a.getArgument(0)); return null; }).when(repository).savePositionTemplateLifecycle(any(), anyString());
+        doReturn(1L).when(repository).countPositionTemplateUsage("lifecycle");
+        var reviewRepository = mock(com.complex.platform.governance.review.SupervisorReviewRepository.class);
+        Map<String, com.complex.platform.governance.review.SupervisorReviewDecisionRecord> decisions = new HashMap<>();
+        when(reviewRepository.saveSupervisorReview(any())).thenAnswer(a -> {
+            com.complex.platform.governance.review.SupervisorReviewDecisionRecord r = a.getArgument(0); decisions.put(r.reviewId(), r); return r;
+        });
+        when(reviewRepository.findPendingSupervisorReviewById(anyString())).thenAnswer(a ->
+                Optional.ofNullable(decisions.get(a.getArgument(0))).filter(r -> "pending_review".equals(r.reviewStatus())));
+        var reviews = new com.complex.platform.governance.review.SupervisorReviewService(authorization,
+                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
+                reviewRepository, new AuditEventService(new InMemoryAuditEventRepository()));
+        service = new AccessService(repository, new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
+                new AuditEventService(new InMemoryAuditEventRepository()), authorization, credentials, reviews, true, 3, 3);
+        service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("archive", "archive", "停止使用", List.of()));
+        assertThat(mapping.get().archivedAt()).isNotNull();
+        assertThat(service.listPositionRoleTemplates().items()).isEmpty();
+        assertThatThrownBy(() -> service.changePositionTemplateLifecycle("lifecycle",
+                new StatusChangeRequest("in-use", "request_delete", "删除申请", List.of()))).isInstanceOf(ApiException.class);
+        doReturn(0L).when(repository).countPositionTemplateUsage("lifecycle");
+        service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("request", "request_delete", "删除申请", List.of()));
+        assertThatThrownBy(() -> service.changePositionTemplateLifecycle("lifecycle",
+                new StatusChangeRequest("ordinary-admin", "approve_delete", "同意", List.of()))).isInstanceOf(ApiException.class);
+        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
+                new AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
+                        com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", null, null, "active"), "unused", List.of()));
+        service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("approve", "approve_delete", "同意", List.of()));
+        assertThat(mapping.get().deletedAt()).isNotNull();
+        assertThat(service.listPositionRoleTemplates(true).items()).hasSize(1);
+        service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("restore", "restore", "恢复留存配置", List.of()));
+        assertThat(mapping.get().deletedAt()).isNull();
+        assertThat(mapping.get().status()).isEqualTo("inactive");
+        service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("self-archive", "archive", "停止使用", List.of()));
+        int reviewCount = decisions.size();
+        service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("self-request", "request_delete", "最高权限本人删除", List.of()));
+        assertThat(mapping.get().deletedAt()).isNotNull();
+        assertThat(decisions).hasSize(reviewCount);
+    }
+
+    private static AccessUserRequest account(String key, String name, String status, String password) {
+        return new AccessUserRequest(key, null, List.of(), name, password, null,
+                "outsourced_vendor", "org", null, status);
+    }
+    private static PermissionTemplateRequest template(String key, String... codes) {
+        return new PermissionTemplateRequest(key, "理由", List.of(), "模板", "base", null,
+                List.of(codes), "project", null, null, false);
+    }
+    private static AccessUser user(String id, String org, String merchant) {
+        return new AccessUser(id, id, "staff", org, merchant, "active", Instant.now());
+    }
+    private static void actor(String id, String type, String org, String merchant) {
+        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
+                new AccessActor(id, id, type, org, merchant, "active"), "unused", List.of()));
+    }
+    private void effect(String id, String code, AccessRepository.Effect effect, String source, boolean once) {
+        effects.add(new AccessRepository.PermissionEffect(id, code, effect, AccessRepository.Source.ROLE,
+                source, null, null, once, false));
+    }
+    private static void assertDenied(Runnable action) {
+        assertThatThrownBy(action::run).isInstanceOfSatisfying(ApiException.class,
+                e -> assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
+    }
+}
diff --git a/backend/src/test/java/com/complex/platform/bar/BarServiceTests.java b/backend/src/test/java/com/complex/platform/bar/BarServiceTests.java
index e0905b6e..908bc0c5 100644
--- a/backend/src/test/java/com/complex/platform/bar/BarServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/bar/BarServiceTests.java
@@ -1,5 +1,7 @@
 package com.complex.platform.bar;
 
+// 文件版本：V0.02
+
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
 import static org.assertj.core.groups.Tuple.tuple;
@@ -133,6 +135,15 @@ class BarServiceTests {
         receivableLedgerService = new CapturingBarReceivableLedgerService();
         AccessRepository accessRepository = mock(AccessRepository.class);
         when(accessRepository.permissionExists(anyString())).thenReturn(true);
+        when(accessRepository.listPermissions()).thenReturn(List.of(
+                new com.complex.platform.access.PermissionDefinition("bar.bill.refund", "退款", "bar", "high", false, "active"),
+                new com.complex.platform.access.PermissionDefinition("bar.order_item.void_decide", "退酒审批", "bar", "high", false, "active")));
+        when(accessRepository.findPermissionEffects(org.mockito.ArgumentMatchers.eq("usr_admin"), org.mockito.ArgumentMatchers.any()))
+                .thenReturn(List.of(
+                        new AccessRepository.PermissionEffect("usr_admin", "bar.bill.refund", AccessRepository.Effect.GRANT,
+                                AccessRepository.Source.ROLE, "explicit_refund", null, null, false, false),
+                        new AccessRepository.PermissionEffect("usr_admin", "bar.order_item.void_decide", AccessRepository.Effect.GRANT,
+                                AccessRepository.Source.ROLE, "explicit_void_review", null, null, false, false)));
         authorizationService = new AuthorizationService(accessRepository);
         AuditEventService auditEventService = new AuditEventService(auditEventRepository);
         IdempotencyService idempotencyService = new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper());
diff --git a/backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java
index 1288ff19..474c55ce 100644
--- a/backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java
@@ -1,5 +1,7 @@
 package com.complex.platform.bar;
 
+// 文件版本：V0.02
+
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
 
@@ -56,12 +58,12 @@ class BarStoredValueFundChainJdbcIntegrationTests {
     @Autowired
     private JdbcTemplate jdbcTemplate;
 
-    @Autowired
-    private com.complex.platform.common.crypto.PiiCipher piiCipher;
-
     @BeforeEach
     void setUpActor() {
-        TestSecurityActors.asPlatformAdmin();
+        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
+                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
+                        new com.complex.platform.access.AccessActor("usr_master_authority", "master-authority",
+                                "platform_admin", "org_1", null, "active"), null, java.util.List.of()));
     }
 
     @AfterEach
@@ -78,7 +80,8 @@ class BarStoredValueFundChainJdbcIntegrationTests {
         seedAccount(customerId, null, "merchant_svchain1", 8000L, null);
 
         BarBillRecord settled = billingService.settleBill(billId, new BarBillSettleRequest(
-                "svchain1key", null, null, "pay_svchain1", null, "stored_value"));
+                "svchain1key", null, null, "pay_svchain1", null, "stored_value", null, null,
+                customerRepository.findCustomerById(customerId).orElseThrow().phone(), "000000"));
 
         assertThat(settled.settlementStatus()).isEqualTo("settled");
         assertThat(settled.paidAmountCent()).isEqualTo(5000L);
@@ -115,7 +118,8 @@ class BarStoredValueFundChainJdbcIntegrationTests {
         // 结账幂等键派生出同一个 barsv_k3attack——AUD-002 之前返回旧流水不扣本次应付仍标已支付；
         // 现在指纹(金额/事由)不符 → 409，整单回滚，不产生假已支付账单。
         assertThatThrownBy(() -> billingService.settleBill(billId, new BarBillSettleRequest(
-                "k3attack", null, null, "pay_svchain3", null, "stored_value")))
+                "k3attack", null, null, "pay_svchain3", null, "stored_value", null, null,
+                customerRepository.findCustomerById(customerId).orElseThrow().phone(), "000000")))
                 .isInstanceOf(ApiException.class)
                 .hasMessageContaining("已被其他操作使用");
     }
@@ -125,8 +129,8 @@ class BarStoredValueFundChainJdbcIntegrationTests {
         String customerId = seedCustomer("svchain4");
         seedAccount(customerId, PROJECT_ID, null, 5000L, null);
 
-        assertThatThrownBy(() -> storedValueService.consume(customerId, PROJECT_ID, null,
-                100L, "reserved prefix probe", "sv_settle_pay_intent_squat"))
+        assertThatThrownBy(() -> storedValueService.topup(customerId, PROJECT_ID, null,
+                100L, "wechat", "sv_settle_pay_intent_squat"))
                 .isInstanceOf(ApiException.class)
                 .hasMessageContaining("系统保留前缀");
         assertThatThrownBy(() -> storedValueService.refund(customerId, PROJECT_ID, null,
@@ -140,16 +144,16 @@ class BarStoredValueFundChainJdbcIntegrationTests {
         String customerId = seedCustomer("svchain5");
         seedAccount(customerId, PROJECT_ID, null, 5000L, null);
 
-        CustomerStoredValueTransactionRecord first = storedValueService.consume(customerId, PROJECT_ID, null,
+        CustomerStoredValueTransactionRecord first = storedValueService.consumeInternal(customerId, PROJECT_ID, null,
                 1200L, "same request retry", "retrykey_svchain5");
-        CustomerStoredValueTransactionRecord replay = storedValueService.consume(customerId, PROJECT_ID, null,
+        CustomerStoredValueTransactionRecord replay = storedValueService.consumeInternal(customerId, PROJECT_ID, null,
                 1200L, "same request retry", "retrykey_svchain5");
 
         assertThat(replay.id()).isEqualTo(first.id());
         assertThat(projectCardBalance(customerId, PROJECT_ID)).isEqualTo(3800L);
 
         // 同键改金额=指纹不符，拒绝而非错误重放。
-        assertThatThrownBy(() -> storedValueService.consume(customerId, PROJECT_ID, null,
+        assertThatThrownBy(() -> storedValueService.consumeInternal(customerId, PROJECT_ID, null,
                 999L, "same request retry", "retrykey_svchain5"))
                 .isInstanceOf(ApiException.class)
                 .hasMessageContaining("已被其他操作使用");
@@ -238,11 +242,8 @@ class BarStoredValueFundChainJdbcIntegrationTests {
     private String seedCustomer(String prefix) {
         String id = prefix + "_customer";
         String phone = "139" + String.format("%08d", Math.abs(prefix.hashCode()) % 100_000_000);
-        jdbcTemplate.update("""
-                INSERT INTO customers (id, phone, masked_phone, status, birth_date, last_login_at, created_at, updated_at)
-                VALUES (?, ?, ?, 'active', NULL, NULL, now(), now())
-                ON CONFLICT (id) DO UPDATE SET updated_at = now()
-                """, id, piiCipher.encrypt(phone), "139****" + phone.substring(7));
+        customerRepository.saveCustomer(new com.complex.platform.customer.CustomerRecord(
+                id, phone, "139****" + phone.substring(7), "active", null, null, Instant.now(), Instant.now()));
         return id;
     }
 
diff --git a/backend/src/test/java/com/complex/platform/customer/M006StoredValueJdbcTests.java b/backend/src/test/java/com/complex/platform/customer/M006StoredValueJdbcTests.java
new file mode 100644
index 00000000..382d7e3e
--- /dev/null
+++ b/backend/src/test/java/com/complex/platform/customer/M006StoredValueJdbcTests.java
@@ -0,0 +1,140 @@
+package com.complex.platform.customer;
+
+// 文件版本：V0.03
+// M006-027：连接内临时表验证储值流水分页、支付行锁和真实事务回滚，不写共享业务表。
+import static org.assertj.core.api.Assertions.*;
+import static org.mockito.Mockito.*;
+import static org.mockito.ArgumentMatchers.*;
+import java.sql.*;
+import java.time.*;
+import java.util.*;
+import com.complex.platform.access.*;
+import com.complex.platform.finance.*;
+import com.complex.platform.ticketing.*;
+import com.complex.platform.evidence.AuditEventService;
+import com.fasterxml.jackson.databind.ObjectMapper;
+import org.junit.jupiter.api.*;
+import org.springframework.aop.framework.ProxyFactory;
+import org.springframework.jdbc.core.JdbcTemplate;
+import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
+import org.springframework.jdbc.datasource.*;
+import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
+import org.springframework.transaction.interceptor.TransactionInterceptor;
+import org.springframework.transaction.support.TransactionTemplate;
+import org.springframework.test.util.ReflectionTestUtils;
+
+class M006StoredValueJdbcTests {
+    Connection connection;
+    SingleConnectionDataSource dataSource;
+    JdbcTemplate jdbc;
+    JdbcCustomerRepository customers;
+    final Instant now = Instant.parse("2026-09-05T00:00:00Z");
+
+    @BeforeEach void open() throws Exception {
+        String url = System.getenv("COMPLEX_DATABASE_URL");
+        Assumptions.assumeTrue(url != null && url.matches("jdbc:postgresql:.*?/complex_test(?:\\?.*)?"));
+        connection=DriverManager.getConnection(url,System.getenv("COMPLEX_DATABASE_USERNAME"),System.getenv("COMPLEX_DATABASE_PASSWORD"));
+        dataSource=new SingleConnectionDataSource(connection,true);
+        jdbc=new JdbcTemplate(dataSource);
+        for(String table:List.of("customer_stored_value_transactions","customer_stored_value_accounts","finance_payment_intents")) {
+            jdbc.execute("CREATE TEMP TABLE "+table+" (LIKE public."+table+" INCLUDING ALL)");
+        }
+        customers=new JdbcCustomerRepository(new NamedParameterJdbcTemplate(dataSource),new ObjectMapper(),null,null);
+    }
+    @AfterEach void close() throws Exception { if(connection!=null)connection.close(); }
+
+    @Test void historyUsesBothOwnerAndCardAndReadsLaterPages() {
+        for(int n=0;n<25;n++) customers.saveStoredValueTransaction(new CustomerStoredValueTransactionRecord(
+                "txn_"+String.format("%02d",n),"card","customer","consume",-100L,10000L-n*100L,
+                null,"bill_"+n,"key_"+n,now.plusSeconds(n)));
+        customers.saveStoredValueTransaction(new CustomerStoredValueTransactionRecord(
+                "foreign","card","another","consume",-999L,1L,null,"foreign","foreign",now.plusSeconds(100)));
+        var first=customers.listStoredValueTransactions("customer","card",1,20);
+        var second=customers.listStoredValueTransactions("customer","card",2,20);
+        assertThat(first).hasSize(20);
+        assertThat(second).hasSize(5);
+        assertThat(first.getFirst().id()).isEqualTo("txn_24");
+        assertThat(second.getFirst().id()).isEqualTo("txn_04");
+        assertThat(customers.listStoredValueTransactions("customer","other_card",1,20)).isEmpty();
+        assertThat(first).allMatch(txn->"customer".equals(txn.customerId()));
+    }
+
+    @Test void financePaymentReadTakesPostgresRowLock() throws Exception {
+        var type=Class.forName("com.complex.platform.finance.JdbcFinanceRepository");
+        var ctor=type.getDeclaredConstructor(NamedParameterJdbcTemplate.class,ObjectMapper.class);
+        ctor.setAccessible(true);
+        var finance=(FinanceRepository)ctor.newInstance(new NamedParameterJdbcTemplate(dataSource),new ObjectMapper());
+        finance.savePaymentIntent(new PaymentIntentRecord("pay","stored_value_topup","customer",5000L,"CNY","wechat","created",now,now));
+        new TransactionTemplate(new DataSourceTransactionManager(dataSource)).execute(status->{
+            assertThat(finance.findPaymentIntentByIdForUpdate("pay")).isPresent();
+            assertThat(jdbc.queryForObject("""
+                    SELECT count(*) FROM pg_locks WHERE pid=pg_backend_pid()
+                    AND relation='pg_temp.finance_payment_intents'::regclass AND mode='RowShareLock' AND granted
+                    """,Integer.class)).isGreaterThan(0);
+            return null;
+        });
+    }
+
+
+    @Test void posRechargeUsesOfflineConfirmationAndCreditsTheBalanceOnlyOnce() throws Exception {
+        for(String table : List.of("customers", "stored_value_topup_intents", "finance_offline_payment_confirmations")) {
+            jdbc.execute("CREATE TEMP TABLE " + table + " (LIKE public." + table + " INCLUDING ALL)");
+        }
+        var cipher = new com.complex.platform.common.crypto.PiiCipher(purpose -> new javax.crypto.spec.SecretKeySpec(
+                "0123456789abcdef0123456789abcdef".getBytes(java.nio.charset.StandardCharsets.UTF_8), "AES"));
+        jdbc.update("INSERT INTO customers (id,phone,masked_phone,status,created_at,updated_at) VALUES ('customer',?,'138****8000','active',now(),now())",
+                cipher.encrypt("13800138000"));
+        var customerRepo = new JdbcCustomerRepository(new NamedParameterJdbcTemplate(dataSource),new ObjectMapper(),cipher,null);
+        customerRepo.saveStoredValueAccount(new CustomerStoredValueAccountRecord(
+                "card","customer",null,"merchant",0L,"active",now,now,"merchant_prepaid"));
+        jdbc.update("INSERT INTO stored_value_topup_intents (payment_intent_id,customer_id,project_id,merchant_id,created_at) VALUES ('pos_pay','customer',null,'merchant',now())");
+        var ctor=Class.forName("com.complex.platform.finance.JdbcFinanceRepository")
+                .getDeclaredConstructor(NamedParameterJdbcTemplate.class,ObjectMapper.class);
+        ctor.setAccessible(true);
+        var financeRepo=(FinanceRepository)ctor.newInstance(new NamedParameterJdbcTemplate(dataSource),new ObjectMapper());
+        financeRepo.savePaymentIntent(new PaymentIntentRecord("pos_pay","stored_value_topup_merchant","customer",5000L,"CNY","pos","created",now,now));
+        var audit=mock(AuditEventService.class);
+        var operations=new com.complex.platform.evidence.AuditedOperationExecutor(
+                new com.complex.platform.common.idempotency.IdempotencyService(
+                        new com.complex.platform.support.InMemoryIdempotencyRepository(),new ObjectMapper()),audit);
+        var disabled=com.complex.platform.finance.gateway.PaymentGatewayRouter.alwaysDisabled();
+        var finance=new FinanceService(financeRepo,operations,audit,mock(AuthorizationService.class),null,disabled,null);
+        var target=new StoredValueService(customerRepo,finance,audit,disabled,mock(AuthorizationService.class),null);
+        var factory=new ProxyFactory(target);
+        factory.setProxyTargetClass(true);
+        factory.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(dataSource),
+                new AnnotationTransactionAttributeSource()));
+        var service=(StoredValueService)factory.getProxy();
+        for(String key : List.of("pos-first","pos-first","pos-retry")) {
+            service.confirmOfflineTopupAndSettle("pos_pay",new com.complex.platform.finance.dto.OfflinePaymentConfirmRequest(
+                    key,null,List.of(),"pos_pay",5000L,"pos",now));
+        }
+        assertThat(financeRepo.findPaymentIntentById("pos_pay").orElseThrow().status()).isEqualTo("succeeded");
+        assertThat(customerRepo.findStoredValueAccount("customer",null,"merchant").orElseThrow().balanceCent()).isEqualTo(5000L);
+        assertThat(customerRepo.listStoredValueTransactions("customer","card",1,20)).hasSize(1);
+    }
+
+    @Test void failedOrderUpdateRollsBackBothActualBalanceAndDebitLedger() {
+        customers.saveStoredValueAccount(new CustomerStoredValueAccountRecord(
+                "card","customer",null,"merchant",5000L,"active",now,now,"merchant_prepaid"));
+        var auth=mock(AuthorizationService.class);
+        when(auth.currentActor()).thenReturn(new AccessActor("customer","customer","customer",null,null,"active"));
+        var tickets=mock(TicketingRepository.class);
+        var order=new TicketOrderRecord("order","T1","customer","type",null,1,null,"app","13800138000",
+                2500L,null,2500L,0,"CNY","unpaid","unissued","created",now,now,null,null,"merchant");
+        when(tickets.findTicketOrderByIdForUpdate("order")).thenReturn(Optional.of(order));
+        when(tickets.updateTicketOrder(any())).thenThrow(new IllegalStateException("订单写入失败"));
+        var target=new StoredValueService(customers,mock(FinanceService.class),mock(AuditEventService.class),null,auth,null);
+        ReflectionTestUtils.setField(target,"ticketingRepository",tickets);
+        ReflectionTestUtils.setField(target,"customerService",mock(CustomerService.class));
+        var factory=new ProxyFactory(target);
+        factory.setProxyTargetClass(true);
+        factory.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(dataSource),
+                new AnnotationTransactionAttributeSource()));
+        var service=(StoredValueService)factory.getProxy();
+        assertThatThrownBy(()->service.payTicketOrder("customer","order","card","13800138000","123456"))
+                .isInstanceOf(IllegalStateException.class).hasMessageContaining("订单写入失败");
+        assertThat(customers.findStoredValueAccount("customer",null,"merchant").orElseThrow().balanceCent()).isEqualTo(5000L);
+        assertThat(customers.listStoredValueTransactions("customer","card",1,20)).isEmpty();
+    }
+}
diff --git a/backend/src/test/java/com/complex/platform/customer/M006StoredValuePaymentTests.java b/backend/src/test/java/com/complex/platform/customer/M006StoredValuePaymentTests.java
new file mode 100644
index 00000000..51e84045
--- /dev/null
+++ b/backend/src/test/java/com/complex/platform/customer/M006StoredValuePaymentTests.java
@@ -0,0 +1,223 @@
+package com.complex.platform.customer;
+
+// 文件版本：V0.11
+// M006-027：现场储值扣款必须先核验报出的手机号和短信验证码。
+import static org.assertj.core.api.Assertions.*;
+import static org.mockito.Mockito.*;
+import static org.mockito.ArgumentMatchers.*;
+import java.time.*;
+import java.util.*;
+import java.util.concurrent.atomic.AtomicReference;
+import com.complex.platform.access.AuthorizationService;
+import com.complex.platform.bar.*;
+import com.complex.platform.common.api.ApiException;
+import com.complex.platform.evidence.AuditEventService;
+import com.complex.platform.finance.FinanceService;
+import com.fasterxml.jackson.databind.*;
+import org.junit.jupiter.api.*;
+import org.springframework.test.util.ReflectionTestUtils;
+
+class M006StoredValuePaymentTests {
+    final Instant now = Instant.parse("2026-09-05T00:00:00Z");
+    final CustomerRepository customers = mock(CustomerRepository.class);
+    final BarRepository bars = mock(BarRepository.class);
+    final AuthorizationService auth = mock(AuthorizationService.class);
+    final AtomicReference<CustomerStoredValueAccountRecord> balance = new AtomicReference<>();
+    final AtomicReference<BarBillRecord> bill = new AtomicReference<>();
+    CustomerService customerService;
+    StoredValueService storedValue;
+    BarBillingService billing;
+
+    @BeforeEach void setup() {
+        var customer = new CustomerRecord("customer_1", "13800138000", "138****8000",
+                "active", null, now, now, now);
+        when(customers.findCustomerById("customer_1")).thenReturn(Optional.of(customer));
+        when(customers.findCustomerByPhone("13800138000")).thenReturn(Optional.of(customer));
+        balance.set(new CustomerStoredValueAccountRecord("card_1", "customer_1", null, "merchant_1",
+                10000L, "active", now, now, "merchant_prepaid"));
+        when(customers.findStoredValueAccountForUpdate("customer_1", null, "merchant_1"))
+                .thenAnswer(i -> Optional.of(balance.get()));
+        when(customers.saveStoredValueAccount(any())).thenAnswer(i -> {balance.set(i.getArgument(0));return balance.get();});
+        when(customers.saveStoredValueTransaction(any())).thenAnswer(i -> i.getArgument(0));
+        when(auth.currentMerchantScope()).thenReturn(Optional.of("merchant_1"));
+        bill.set(new BarBillRecord("bill_1", "B1", null, "bar_1", "merchant_1", null, null,
+                3000L, 0, 0, 3000L, 0, "CNY", "unpaid", "unsettled", null, null, now, now,
+                false, null, null, null, 0, 0));
+        when(bars.findBillByIdForUpdate("bill_1")).thenAnswer(i -> Optional.of(bill.get()));
+        when(bars.saveBill(any())).thenAnswer(i -> {bill.set(i.getArgument(0));return bill.get();});
+        var audit = mock(AuditEventService.class);
+        storedValue = new StoredValueService(customers, mock(FinanceService.class), audit, null, auth, null);
+        customerService = new CustomerService(customers, new FixedCustomerOtpVerifier("123456"),
+                null, null, audit, null, null, bars, null, null,
+                Clock.fixed(now, ZoneOffset.UTC), null, null);
+        billing = new BarBillingService(bars, null, auth, audit);
+        ReflectionTestUtils.setField(billing, "storedValueService", storedValue);
+        ReflectionTestUtils.setField(billing, "customerService", customerService);
+    }
+
+    BarBillRecord settle(String phone, String code) throws Exception {
+        var mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
+        String json = mapper.writeValueAsString(Map.of("idempotencyKey", "sms_bill_1",
+                "paymentId", "payment_1", "paymentChannel", "stored_value",
+                "phone", phone, "smsCode", code));
+        BarBillSettleRequest request = mapper.readValue(json, BarBillSettleRequest.class);
+        return ReflectionTestUtils.invokeMethod(billing, "settleBill", "bill_1", request);
+    }
+
+    void assertUnchanged() {
+        assertThat(balance.get().balanceCent()).isEqualTo(10000L);
+        assertThat(bill.get().paidAmountCent()).isZero();
+        verify(bars, never()).saveBillSettlement(any());
+    }
+
+    @Test void appOrderPaysItsAmountOnceFromTheSelectedOwnCard() {
+        var tickets = mock(com.complex.platform.ticketing.TicketingRepository.class);
+        var order = new AtomicReference<>(new com.complex.platform.ticketing.TicketOrderRecord(
+                "ticket_order_1","T1","customer_1","type_1",null,1,null,"app","13800138000",
+                2500L,null,2500L,0L,"CNY","unpaid","unissued","created",now,now,null,null,"merchant_1"));
+        // JDBC 查单必须取得行锁，避免重复扣款与并发取消。
+        when(tickets.findTicketOrderByIdForUpdate("ticket_order_1")).thenAnswer(i -> Optional.of(order.get()));
+        when(tickets.updateTicketOrder(any())).thenAnswer(i -> {order.set(i.getArgument(0));return order.get();});
+        when(customers.listStoredValueAccountsByCustomer("customer_1")).thenAnswer(i -> List.of(balance.get()));
+        when(auth.currentActor()).thenReturn(new com.complex.platform.access.AccessActor(
+                "customer_1","customer_1","customer",null,null,"active"));
+        ReflectionTestUtils.setField(storedValue, "ticketingRepository", tickets);
+        ReflectionTestUtils.setField(storedValue, "customerService", customerService);
+        for (int n=0;n<2;n++) {
+            ReflectionTestUtils.invokeMethod(storedValue, "payTicketOrder", "customer_1",
+                    "ticket_order_1", "card_1", "13800138000", "123456");
+        }
+        assertThat(balance.get().balanceCent()).isEqualTo(7500L);
+        assertThat(order.get().payStatus()).isEqualTo("paid");
+        assertThat(order.get().paidAt()).isNotNull();
+    }
+
+
+    @Test void appOrderRejectsForeignOwnershipWrongSmsFrozenBalanceAndWrongCardScope() {
+        var tickets = mock(com.complex.platform.ticketing.TicketingRepository.class);
+        var currentOrder = new AtomicReference<com.complex.platform.ticketing.TicketOrderRecord>();
+        when(tickets.findTicketOrderByIdForUpdate("ticket_order_1")).thenAnswer(i -> Optional.of(currentOrder.get()));
+        when(customers.listStoredValueAccountsByCustomer("customer_1")).thenAnswer(i -> List.of(balance.get()));
+        when(auth.currentActor()).thenReturn(new com.complex.platform.access.AccessActor(
+                "customer_1","customer_1","customer",null,null,"active"));
+        ReflectionTestUtils.setField(storedValue, "ticketingRepository", tickets);
+        ReflectionTestUtils.setField(storedValue, "customerService", customerService);
+        for (String invalid : List.of("foreign_order", "foreign_card", "wrong_phone", "wrong_sms", "frozen", "insufficient", "wrong_merchant", "cancelled")) {
+            currentOrder.set(new com.complex.platform.ticketing.TicketOrderRecord(
+                    "ticket_order_1","T1","foreign_order".equals(invalid) ? "other_customer" : "customer_1",
+                    "type_1",null,1,null,"app","13800138000",2500L,null,2500L,0L,
+                    "CNY","unpaid","unissued","cancelled".equals(invalid) ? "cancelled" : "created",now,now,null,null,"merchant_1"));
+            balance.set(new CustomerStoredValueAccountRecord("card_1","customer_1",null,
+                    "wrong_merchant".equals(invalid) ? "merchant_2" : "merchant_1",
+                    "insufficient".equals(invalid) ? 1L : 10000L, "frozen".equals(invalid) ? "frozen" : "active",
+                    now,now,"merchant_prepaid"));
+            long before = balance.get().balanceCent();
+            assertThatThrownBy(() -> storedValue.payTicketOrder("customer_1", "ticket_order_1",
+                    "foreign_card".equals(invalid) ? "other_card" : "card_1",
+                    "wrong_phone".equals(invalid) ? "13900139000" : "13800138000",
+                    "wrong_sms".equals(invalid) ? "999999" : "123456"))
+                    .as(invalid).isInstanceOf(ApiException.class);
+            assertThat(balance.get().balanceCent()).as(invalid).isEqualTo(before);
+        }
+        verify(tickets, never()).saveTicketPaymentIntent(any());
+        verify(tickets, never()).updateTicketOrder(any());
+        verify(customers, never()).saveStoredValueTransaction(any());
+    }
+
+    @Test void onlyMatchingGatewayReceiptCreditsTheRechargeAndRepeatDoesNotCreditAgain() {
+        var financeRepo = mock(com.complex.platform.finance.FinanceRepository.class);
+        var gateway = mock(com.complex.platform.finance.gateway.PaymentGatewayRouter.class);
+        when(gateway.isGatewayEnabled()).thenReturn(true);
+        var intent = new AtomicReference<>(new com.complex.platform.finance.PaymentIntentRecord(
+                "pay_1","stored_value_topup_merchant","customer_1",5000L,"CNY","wechat","created",
+                "configured_gateway","ref_1","PENDING",now,now));
+        when(financeRepo.findPaymentIntentByIdForUpdate("pay_1")).thenAnswer(i -> Optional.of(intent.get()));
+        when(financeRepo.savePaymentIntent(any())).thenAnswer(i -> { intent.set(i.getArgument(0));return intent.get(); });
+        var finance = new FinanceService(financeRepo,null,mock(AuditEventService.class),auth,null,gateway,null);
+        var service = new StoredValueService(customers,finance,mock(AuditEventService.class),gateway,auth,null);
+        when(customers.findStoredValueAccount("customer_1",null,"merchant_1")).thenAnswer(i -> Optional.of(balance.get()));
+        when(customers.findStoredValueTopupIntent("pay_1")).thenReturn(Optional.of(
+                new StoredValueTopupIntentScope("pay_1","customer_1",null,"merchant_1")));
+        var txn = new AtomicReference<CustomerStoredValueTransactionRecord>();
+        when(customers.saveStoredValueTransaction(any())).thenAnswer(i -> { txn.set(i.getArgument(0));return txn.get(); });
+        when(customers.storedValueTransactionExistsForPaymentIntent("pay_1")).thenAnswer(i -> txn.get()!=null);
+        when(customers.findStoredValueTransactionByIdempotencyKey("sv_settle_pay_1")).thenAnswer(i -> Optional.ofNullable(txn.get()));
+        for (long amount : new long[] {1L,5000L,5000L}) {
+            var receipt = new com.complex.platform.finance.gateway.PaymentGatewayRequest(
+                    "stored_value_topup_merchant","customer_1",amount,"CNY","wechat","ref_1","merchant_1","merchant_prepaid");
+            when(gateway.queryStatus("ref_1")).thenReturn(new com.complex.platform.finance.gateway.PaymentGatewayResult(
+                    com.complex.platform.finance.gateway.PaymentGatewayResult.Status.SUCCEEDED,
+                    "configured_gateway","ref_1","",null,receipt));
+            if (amount==1L) {
+                assertThatThrownBy(() -> service.settleTopup("customer_1",null,"merchant_1","pay_1"))
+                        .isInstanceOf(ApiException.class).hasMessageContaining("确认与本次充值单不一致");
+                assertThat(balance.get().balanceCent()).isEqualTo(10000L);
+            } else {
+                service.settleTopup("customer_1",null,"merchant_1","pay_1");
+                assertThat(balance.get().balanceCent()).isEqualTo(15000L);
+            }
+        }
+        assertThat(intent.get().gatewayRef()).isEqualTo("ref_1");
+        assertThat(txn.get().paymentIntentId()).isEqualTo("pay_1");
+    }
+
+    @Test void onlineTopupCannotCreditAPlaceholderSuccessFlag() {
+        var finance = mock(FinanceService.class);
+        var gateway = com.complex.platform.finance.gateway.PaymentGatewayRouter.alwaysDisabled();
+        var service = new StoredValueService(customers, finance, mock(AuditEventService.class), gateway, auth, null);
+        when(finance.findPaymentIntent("pay_1")).thenReturn(Optional.of(new com.complex.platform.finance.PaymentIntentRecord(
+                "pay_1","stored_value_topup_merchant","customer_1",5000L,"CNY","wechat","succeeded",now,now)));
+        when(customers.findStoredValueTopupIntent("pay_1")).thenReturn(Optional.of(
+                new StoredValueTopupIntentScope("pay_1","customer_1",null,"merchant_1")));
+        assertThatThrownBy(() -> service.settleTopup("customer_1",null,"merchant_1","pay_1"))
+                .isInstanceOf(ApiException.class).hasMessageContaining("支付渠道");
+        assertUnchanged();
+    }
+
+    @Test void walletHasNoArbitraryAmountConsumeEndpoint() {
+        boolean exposed = Arrays.stream(MemberWalletController.class.getDeclaredMethods())
+                .map(m -> m.getAnnotation(org.springframework.web.bind.annotation.PostMapping.class))
+                .filter(Objects::nonNull).flatMap(a -> Arrays.stream(a.value()))
+                .anyMatch("/stored-value/consume"::equals);
+        assertThat(exposed).isFalse();
+    }
+
+    @Test void requestingCodeWithUnconfiguredProviderFailsInsteadOfReportingSent() {
+        ReflectionTestUtils.setField(customerService, "otpVerifier", new FailClosedCustomerOtpVerifier());
+        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(customerService,
+                "requestStoredValueCode", (String) null, "13800138000"))
+                .isInstanceOf(ApiException.class).hasMessageContaining("短信服务未配置");
+        assertUnchanged();
+    }
+
+    @Test void validPhoneAndSmsPayTheActualUnboundBill() throws Exception {
+        BarBillRecord result = settle("13800138000", "123456");
+        assertThat(balance.get().balanceCent()).isEqualTo(7000L);
+        assertThat(result.paidAmountCent()).isEqualTo(3000L);
+        assertThat(result.settlementStatus()).isEqualTo("settled");
+    }
+
+    @Test void missingSmsCannotDebitAnAlreadyBoundCustomer() {
+        when(bars.findOrderById("order_1")).thenReturn(Optional.of(new BarOrderRecord(
+                "order_1", "bar_1", "merchant_1", null, null, "customer_1", "app", "created",
+                3000L, "CNY", "miniapp", null, null, now, now)));
+        BarBillRecord b=bill.get();
+        bill.set(new BarBillRecord(b.id(),b.billNo(),"order_1",b.barId(),b.merchantId(),null,null,
+                3000L,0,0,3000L,0,"CNY","unpaid","unsettled",null,null,now,now,false,null,null,null,0,0));
+        assertThatThrownBy(() -> settle("13800138000", "")).isInstanceOf(ApiException.class);
+        assertUnchanged();
+    }
+
+    @Test void incorrectSmsDoesNotDebit() {
+        assertThatThrownBy(() -> settle("13800138000", "999999"))
+                .isInstanceOf(ApiException.class).hasMessageContaining("验证码");
+        assertUnchanged();
+    }
+
+    @Test void unavailableProductionSmsDoesNotDebit() {
+        ReflectionTestUtils.setField(customerService, "otpVerifier", new FailClosedCustomerOtpVerifier());
+        assertThatThrownBy(() -> settle("13800138000", "123456"))
+                .isInstanceOf(ApiException.class).hasMessageContaining("短信服务未配置");
+        assertUnchanged();
+    }
+}
diff --git a/backend/src/test/java/com/complex/platform/customer/MemberWalletServiceTests.java b/backend/src/test/java/com/complex/platform/customer/MemberWalletServiceTests.java
index 3d0b6131..6fb3779f 100644
--- a/backend/src/test/java/com/complex/platform/customer/MemberWalletServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/customer/MemberWalletServiceTests.java
@@ -1,5 +1,7 @@
 package com.complex.platform.customer;
 
+// 文件版本：V0.02
+
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
 
@@ -37,7 +39,7 @@ class MemberWalletServiceTests {
     void topupAddsBalanceOnlyAfterPaymentSucceeds() {
         InMemoryRepo repo = seeded();
         FakeFinance finance = new FakeFinance();
-        StoredValueService service = new StoredValueService(repo, finance, audit(), com.complex.platform.finance.gateway.PaymentGatewayRouter.alwaysDisabled(), null, null,
+        StoredValueService service = new StoredValueService(repo, finance, audit(), gatewayRouter(true), null, null,
                 Clock.fixed(T0, ZoneOffset.UTC));
 
         // 发起充值：仅建支付意图（created），余额不变。
@@ -68,17 +70,17 @@ class MemberWalletServiceTests {
     void consumeRejectedWhenInsufficient() {
         InMemoryRepo repo = seeded();
         FakeFinance finance = new FakeFinance();
-        StoredValueService service = new StoredValueService(repo, finance, audit(), com.complex.platform.finance.gateway.PaymentGatewayRouter.alwaysDisabled(), null, null,
+        StoredValueService service = new StoredValueService(repo, finance, audit(), gatewayRouter(true), null, null,
                 Clock.fixed(T0, ZoneOffset.UTC));
         // 充值 5000 入账
         PaymentIntentRecord intent = service.topup("c1", null, null, 5_000L, "wechat", "topup-2");
         finance.markPaymentIntentSucceeded(intent.id());
         service.settleTopup("c1", null, null, intent.id());
         // 消费 3000 OK
-        service.consume("c1", null, null, 3_000L, "buy", "consume-1");
+        service.consumeInternal("c1", null, null, 3_000L, "buy", "consume-1");
         assertThat(repo.storedValue.get("c1|null|null").balanceCent()).isEqualTo(2_000L);
         // 消费 5000 不足拒
-        assertThatThrownBy(() -> service.consume("c1", null, null, 5_000L, "buy", "consume-2"))
+        assertThatThrownBy(() -> service.consumeInternal("c1", null, null, 5_000L, "buy", "consume-2"))
                 .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("STORED_VALUE_INSUFFICIENT"));
         assertThat(repo.storedValue.get("c1|null|null").balanceCent()).isEqualTo(2_000L);
     }
@@ -87,7 +89,7 @@ class MemberWalletServiceTests {
     void freezeBlocksConsumeAndUnfreezeRestores() {
         InMemoryRepo repo = seeded();
         FakeFinance finance = new FakeFinance();
-        StoredValueService service = new StoredValueService(repo, finance, audit(), com.complex.platform.finance.gateway.PaymentGatewayRouter.alwaysDisabled(), null, null,
+        StoredValueService service = new StoredValueService(repo, finance, audit(), gatewayRouter(true), null, null,
                 Clock.fixed(T0, ZoneOffset.UTC));
         PaymentIntentRecord intent = service.topup("c1", null, null, 5_000L, "wechat", "topup-3");
         finance.markPaymentIntentSucceeded(intent.id());
@@ -95,12 +97,12 @@ class MemberWalletServiceTests {
 
         service.freeze("c1", null, null, "risk");
         assertThat(repo.storedValue.get("c1|null|null").status()).isEqualTo("frozen");
-        assertThatThrownBy(() -> service.consume("c1", null, null, 1_000L, "buy", "c-frozen"))
+        assertThatThrownBy(() -> service.consumeInternal("c1", null, null, 1_000L, "buy", "c-frozen"))
                 .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("STORED_VALUE_ACCOUNT_FROZEN"));
 
         service.unfreeze("c1", null, null, "cleared");
         assertThat(repo.storedValue.get("c1|null|null").status()).isEqualTo("active");
-        CustomerStoredValueTransactionRecord txn = service.consume("c1", null, null, 1_000L, "buy", "c-ok");
+        CustomerStoredValueTransactionRecord txn = service.consumeInternal("c1", null, null, 1_000L, "buy", "c-ok");
         assertThat(txn.balanceAfterCent()).isEqualTo(4_000L);
         // V231：消费非充值支付，paid_at 必须为 null（仅 topup 记支付时刻）。
         assertThat(txn.paidAt()).isNull();
@@ -110,7 +112,7 @@ class MemberWalletServiceTests {
     void refundReturnsBalanceAndClosesAccount() {
         InMemoryRepo repo = seeded();
         FakeFinance finance = new FakeFinance();
-        StoredValueService service = new StoredValueService(repo, finance, audit(), com.complex.platform.finance.gateway.PaymentGatewayRouter.alwaysDisabled(), null, null,
+        StoredValueService service = new StoredValueService(repo, finance, audit(), gatewayRouter(true), null, null,
                 Clock.fixed(T0, ZoneOffset.UTC));
         PaymentIntentRecord intent = service.topup("c1", null, null, 5_000L, "wechat", "topup-4");
         finance.markPaymentIntentSucceeded(intent.id());
@@ -128,7 +130,7 @@ class MemberWalletServiceTests {
         // AUD-058：入账只认充值时绑定的卡范围——指名商户卡/跨项目/他人 customerId 均拒，资金不入错卡。
         InMemoryRepo repo = seeded();
         FakeFinance finance = new FakeFinance();
-        StoredValueService service = new StoredValueService(repo, finance, audit(), com.complex.platform.finance.gateway.PaymentGatewayRouter.alwaysDisabled(), null, null,
+        StoredValueService service = new StoredValueService(repo, finance, audit(), gatewayRouter(true), null, null,
                 Clock.fixed(T0, ZoneOffset.UTC));
         PaymentIntentRecord intent = service.topup("c1", null, null, 5_000L, "wechat", "topup-scope");
         finance.markPaymentIntentSucceeded(intent.id());
@@ -155,6 +157,7 @@ class MemberWalletServiceTests {
         FakeFinance finance = new FakeFinance();
         com.complex.platform.finance.gateway.PaymentGatewayRouter failing =
                 org.mockito.Mockito.mock(com.complex.platform.finance.gateway.PaymentGatewayRouter.class);
+        org.mockito.Mockito.when(failing.isGatewayEnabled()).thenReturn(true);
         org.mockito.Mockito.when(failing.refund(org.mockito.ArgumentMatchers.any()))
                 .thenReturn(new com.complex.platform.finance.gateway.PaymentGatewayResult(
                         com.complex.platform.finance.gateway.PaymentGatewayResult.Status.FAILED,
@@ -177,7 +180,7 @@ class MemberWalletServiceTests {
         // 指定 m1 消费只扣 m1 卡（只扣指定的那一张）；商户卡只能在该发卡商户消费（拿 m2 范围取不到 m1 卡，不误扣）。
         InMemoryRepo repo = seeded();
         FakeFinance finance = new FakeFinance();
-        StoredValueService service = new StoredValueService(repo, finance, audit(), com.complex.platform.finance.gateway.PaymentGatewayRouter.alwaysDisabled(), null, null,
+        StoredValueService service = new StoredValueService(repo, finance, audit(), gatewayRouter(true), null, null,
                 Clock.fixed(T0, ZoneOffset.UTC));
         PaymentIntentRecord p = service.topup("c1", null, null, 8_000L, "wechat", "tp-prj");
         finance.markPaymentIntentSucceeded(p.id());
@@ -191,12 +194,12 @@ class MemberWalletServiceTests {
         assertThat(repo.storedValue.get("c1|null|m1").balanceCent()).isEqualTo(3_000L);
 
         // 指定 m1 消费只扣 m1 卡，通用卡不动。
-        service.consume("c1", null, "m1", 1_000L, "buy at m1", "cs-m1");
+        service.consumeInternal("c1", null, "m1", 1_000L, "buy at m1", "cs-m1");
         assertThat(repo.storedValue.get("c1|null|m1").balanceCent()).isEqualTo(2_000L);
         assertThat(repo.storedValue.get("c1|null|null").balanceCent()).isEqualTo(8_000L);
 
         // 商户卡只能在该商户消费：m2 范围取不到 m1 卡 → 账户不存在，不误扣 m1/通用卡。
-        assertThatThrownBy(() -> service.consume("c1", null, "m2", 500L, "buy at m2", "cs-m2"))
+        assertThatThrownBy(() -> service.consumeInternal("c1", null, "m2", 500L, "buy at m2", "cs-m2"))
                 .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("STORED_VALUE_ACCOUNT_NOT_FOUND"));
     }
 
@@ -205,7 +208,7 @@ class MemberWalletServiceTests {
         // B1d⑤ phase3：商户配置「不可退」→ 该商户卡退款被拒；配置「可退+比例」→ 退款放行（按比例打款，余额全额扣减）。
         InMemoryRepo repo = seeded();
         FakeFinance finance = new FakeFinance();
-        StoredValueService service = new StoredValueService(repo, finance, audit(), com.complex.platform.finance.gateway.PaymentGatewayRouter.alwaysDisabled(), null, null,
+        StoredValueService service = new StoredValueService(repo, finance, audit(), gatewayRouter(true), null, null,
                 Clock.fixed(T0, ZoneOffset.UTC));
         service.configureMerchantCard("m1", true, "platform_collect");
         PaymentIntentRecord m = service.topup("c1", null, "m1", 5_000L, "wechat", "tp-m1r");
@@ -246,7 +249,7 @@ class MemberWalletServiceTests {
                 org.mockito.Mockito.mock(com.complex.platform.access.AuthorizationService.class);
         org.mockito.Mockito.when(merchantAuth.currentMerchantScope()).thenReturn(Optional.of("m1"));
         StoredValueService m1Staff = new StoredValueService(repo, new FakeFinance(), audit(),
-                com.complex.platform.finance.gateway.PaymentGatewayRouter.alwaysDisabled(), merchantAuth, null,
+                gatewayRouter(true), merchantAuth, null,
                 Clock.fixed(T0, ZoneOffset.UTC));
 
         // 本商户卡 → 放行。
@@ -256,9 +259,9 @@ class MemberWalletServiceTests {
                 .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
         // 清单 P1-1:consume 也须受卡范围约束——商户 m1 员工不得扣他商户(m2)卡或通用卡(越权扣款);
         // scope 校验在状态检查之前,越权即拒不泄露卡状态。
-        assertThatThrownBy(() -> m1Staff.consume("c1", null, "m2", 100L, "x", "sv-c-m2"))
+        assertThatThrownBy(() -> m1Staff.consumeInternal("c1", null, "m2", 100L, "x", "sv-c-m2"))
                 .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
-        assertThatThrownBy(() -> m1Staff.consume("c1", null, null, 100L, "x", "sv-c-prj"))
+        assertThatThrownBy(() -> m1Staff.consumeInternal("c1", null, null, 100L, "x", "sv-c-prj"))
                 .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
         // 综合体通用卡 → 拒（商户员工不得操作平台级卡）。
         assertThatThrownBy(() -> m1Staff.freeze("c1", null, null, "x"))
@@ -272,7 +275,7 @@ class MemberWalletServiceTests {
                 org.mockito.Mockito.mock(com.complex.platform.access.AuthorizationService.class);
         org.mockito.Mockito.when(platformAuth.currentMerchantScope()).thenReturn(Optional.empty());
         StoredValueService platform = new StoredValueService(repo, new FakeFinance(), audit(),
-                com.complex.platform.finance.gateway.PaymentGatewayRouter.alwaysDisabled(), platformAuth, null,
+                gatewayRouter(true), platformAuth, null,
                 Clock.fixed(T0, ZoneOffset.UTC));
         assertThat(platform.freeze("c1", null, "m2", "ok").status()).isEqualTo("frozen");
     }
@@ -318,15 +321,15 @@ class MemberWalletServiceTests {
     void consumeRejectsBlankIdempotencyKey() {
         InMemoryRepo repo = seeded();
         FakeFinance finance = new FakeFinance();
-        StoredValueService service = new StoredValueService(repo, finance, audit(), com.complex.platform.finance.gateway.PaymentGatewayRouter.alwaysDisabled(), null, null,
+        StoredValueService service = new StoredValueService(repo, finance, audit(), gatewayRouter(true), null, null,
                 Clock.fixed(T0, ZoneOffset.UTC));
         PaymentIntentRecord intent = service.topup("c1", null, null, 5_000L, "wechat", "topup-b1b-c");
         finance.markPaymentIntentSucceeded(intent.id());
         service.settleTopup("c1", null, null, intent.id());
 
-        assertThatThrownBy(() -> service.consume("c1", null, null, 1_000L, "buy", null))
+        assertThatThrownBy(() -> service.consumeInternal("c1", null, null, 1_000L, "buy", null))
                 .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
-        assertThatThrownBy(() -> service.consume("c1", null, null, 1_000L, "buy", "   "))
+        assertThatThrownBy(() -> service.consumeInternal("c1", null, null, 1_000L, "buy", "   "))
                 .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
         // 余额未被扣减（空键被拒，未发生扣减）。
         assertThat(repo.storedValue.get("c1|null|null").balanceCent()).isEqualTo(5_000L);
@@ -336,7 +339,7 @@ class MemberWalletServiceTests {
     void refundRejectsBlankIdempotencyKey() {
         InMemoryRepo repo = seeded();
         FakeFinance finance = new FakeFinance();
-        StoredValueService service = new StoredValueService(repo, finance, audit(), com.complex.platform.finance.gateway.PaymentGatewayRouter.alwaysDisabled(), null, null,
+        StoredValueService service = new StoredValueService(repo, finance, audit(), gatewayRouter(true), null, null,
                 Clock.fixed(T0, ZoneOffset.UTC));
         PaymentIntentRecord intent = service.topup("c1", null, null, 5_000L, "wechat", "topup-b1b-r");
         finance.markPaymentIntentSucceeded(intent.id());
@@ -358,7 +361,7 @@ class MemberWalletServiceTests {
     void merchantCardTopupRejectedWithoutIssuanceThenAllowedAfterPlatformEnables() {
         InMemoryRepo repo = seeded();
         FakeFinance finance = new FakeFinance();
-        StoredValueService service = new StoredValueService(repo, finance, audit(), com.complex.platform.finance.gateway.PaymentGatewayRouter.alwaysDisabled(), null, null,
+        StoredValueService service = new StoredValueService(repo, finance, audit(), gatewayRouter(true), null, null,
                 Clock.fixed(T0, ZoneOffset.UTC));
         assertThatThrownBy(() -> service.topup("c1", null, "mX", 5_000L, "wechat", "tp-noissue"))
                 .isInstanceOfSatisfying(ApiException.class,
@@ -377,7 +380,7 @@ class MemberWalletServiceTests {
     void fundModeFrozenAtOpenAndPlatformOnly() {
         InMemoryRepo repo = seeded();
         FakeFinance finance = new FakeFinance();
-        StoredValueService platform = new StoredValueService(repo, finance, audit(), com.complex.platform.finance.gateway.PaymentGatewayRouter.alwaysDisabled(), null, null,
+        StoredValueService platform = new StoredValueService(repo, finance, audit(), gatewayRouter(true), null, null,
                 Clock.fixed(T0, ZoneOffset.UTC));
         platform.configureMerchantCard("mX", true, "platform_collect");
         PaymentIntentRecord m = platform.topup("c1", null, "mX", 5_000L, "wechat", "tp-frz");
@@ -394,7 +397,7 @@ class MemberWalletServiceTests {
         org.mockito.Mockito.doThrow(ApiException.forbidden("PLATFORM_SCOPE_REQUIRED", "x"))
                 .when(merchantAuth).requirePlatformScope();
         StoredValueService merchantSvc = new StoredValueService(repo, finance, audit(),
-                com.complex.platform.finance.gateway.PaymentGatewayRouter.alwaysDisabled(), merchantAuth, null,
+                gatewayRouter(true), merchantAuth, null,
                 Clock.fixed(T0, ZoneOffset.UTC));
         assertThatThrownBy(() -> merchantSvc.configureMerchantCard("mX", true, "platform_collect"))
                 .isInstanceOfSatisfying(ApiException.class,
@@ -405,20 +408,20 @@ class MemberWalletServiceTests {
     void platformCollectConsumeGeneratesMerchantPayableAndPrepaidDoesNot() {
         InMemoryRepo repo = seeded();
         FakeFinance finance = new FakeFinance();
-        StoredValueService service = new StoredValueService(repo, finance, audit(), com.complex.platform.finance.gateway.PaymentGatewayRouter.alwaysDisabled(), null, null,
+        StoredValueService service = new StoredValueService(repo, finance, audit(), gatewayRouter(true), null, null,
                 Clock.fixed(T0, ZoneOffset.UTC));
         service.configureMerchantCard("mPC", true, "platform_collect");
         PaymentIntentRecord m = service.topup("c1", null, "mPC", 5_000L, "wechat", "tp-pc");
         finance.markPaymentIntentSucceeded(m.id());
         service.settleTopup("c1", null, "mPC", m.id());
-        CustomerStoredValueTransactionRecord consumed = service.consume("c1", null, "mPC", 2_000L, "buy", "cs-pc");
+        CustomerStoredValueTransactionRecord consumed = service.consumeInternal("c1", null, "mPC", 2_000L, "buy", "cs-pc");
         assertThat(finance.merchantPayables).containsExactly("mPC|" + consumed.id() + "|2000");
 
         service.configureMerchantCard("mPP", true, "merchant_prepaid");
         PaymentIntentRecord m2 = service.topup("c1", null, "mPP", 5_000L, "wechat", "tp-pp");
         finance.markPaymentIntentSucceeded(m2.id());
         service.settleTopup("c1", null, "mPP", m2.id());
-        service.consume("c1", null, "mPP", 1_000L, "buy", "cs-pp");
+        service.consumeInternal("c1", null, "mPP", 1_000L, "buy", "cs-pp");
         assertThat(finance.merchantPayables).hasSize(1);
     }
 
@@ -426,7 +429,7 @@ class MemberWalletServiceTests {
     void merchantPrepaidCardRefundRejectedOnlineWhilePlatformCollectAllowed() {
         InMemoryRepo repo = seeded();
         FakeFinance finance = new FakeFinance();
-        StoredValueService service = new StoredValueService(repo, finance, audit(), com.complex.platform.finance.gateway.PaymentGatewayRouter.alwaysDisabled(), null, null,
+        StoredValueService service = new StoredValueService(repo, finance, audit(), gatewayRouter(true), null, null,
                 Clock.fixed(T0, ZoneOffset.UTC));
         service.configureMerchantCard("mPP", true, "merchant_prepaid");
         PaymentIntentRecord m = service.topup("c1", null, "mPP", 5_000L, "wechat", "tp-refuse");
@@ -458,7 +461,7 @@ class MemberWalletServiceTests {
         return repo;
     }
 
-    /** 测试用 finance：仅实现支付意图三方法，其余继承（不调用）。 */
+    /** 测试用 finance：模拟已确认渠道回执；真实渠道校验另由 M006StoredValuePaymentTests 覆盖。 */
     private static final class FakeFinance extends FinanceService {
         private final Map<String, PaymentIntentRecord> intents = new java.util.LinkedHashMap<>();
         private int seq = 0;
@@ -485,6 +488,20 @@ class MemberWalletServiceTests {
             return Optional.ofNullable(intents.get(id));
         }
 
+        @Override
+        public Optional<PaymentIntentRecord> findPaymentIntentForUpdate(String id) {
+            return findPaymentIntent(id);
+        }
+
+        @Override
+        public PaymentIntentRecord confirmOnlinePaymentIntent(String id, String merchantId, String fundMode) {
+            PaymentIntentRecord intent = intents.get(id);
+            if (intent == null || !"succeeded".equals(intent.status())) {
+                throw ApiException.conflict("PAYMENT_NOT_SUCCEEDED", "该支付尚未成功");
+            }
+            return intent;
+        }
+
         @Override
         public PaymentIntentRecord markPaymentIntentSucceeded(String id) {
             PaymentIntentRecord i = intents.get(id);
diff --git a/backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java b/backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java
index 7e2eeffe..b56a4459 100644
--- a/backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java
+++ b/backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java
@@ -1,5 +1,7 @@
 package com.complex.platform.customer;
 
+// 文件版本：V0.02
+
 import static org.assertj.core.api.Assertions.assertThat;
 
 import com.complex.platform.support.TestSecurityActors;
@@ -27,12 +29,18 @@ class StoredValueNotificationIntegrationTest {
     @Autowired
     private JdbcTemplate jdbcTemplate;
 
+    @Autowired
+    private CustomerRepository customerRepository;
+
     @Autowired
     private com.complex.platform.common.crypto.PiiCipher piiCipher;
 
     @BeforeEach
     void setUp() {
-        TestSecurityActors.asPlatformAdmin();
+        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
+                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
+                        new com.complex.platform.access.AccessActor("usr_master_authority", "master-authority",
+                                "platform_admin", "org_1", null, "active"), null, java.util.List.of()));
         cleanupNotificationData();
         setSwitch("sms_notification", true);
         setSwitch("sms.notify.stored_value", true);
@@ -85,7 +93,7 @@ class StoredValueNotificationIntegrationTest {
         insertCustomer(customerId, phone);
         settleTopup(customerId, 8_000L, prefix);
 
-        CustomerStoredValueTransactionRecord txn = storedValueService.consume(
+        CustomerStoredValueTransactionRecord txn = storedValueService.consumeInternal(
                 customerId,
                 PROJECT_ID,
                 null,
@@ -123,10 +131,13 @@ class StoredValueNotificationIntegrationTest {
             String prefix
     ) {
         String paymentIntentId = prefix + "_payment_intent";
-        insertSucceededPaymentIntent(paymentIntentId, customerId, amountCent);
+        insertCreatedPaymentIntent(paymentIntentId, customerId, amountCent);
         // AUD-058：settleTopup 只认充值时绑定的卡范围，夹具需同步落绑定行（等价 topup 落库动作）。
         insertTopupIntentBinding(paymentIntentId, customerId);
-        return storedValueService.settleTopup(customerId, PROJECT_ID, null, paymentIntentId);
+        storedValueService.confirmOfflineTopupAndSettle(paymentIntentId,
+                new com.complex.platform.finance.dto.OfflinePaymentConfirmRequest(
+                        prefix + "_pos_confirm", null, null, paymentIntentId, amountCent, "pos", java.time.Instant.now()));
+        return customerRepository.findStoredValueTransactionByIdempotencyKey("sv_settle_" + paymentIntentId).orElseThrow();
     }
 
     private void insertTopupIntentBinding(String paymentIntentId, String customerId) {
@@ -140,13 +151,13 @@ class StoredValueNotificationIntegrationTest {
                 """, paymentIntentId, customerId, PROJECT_ID);
     }
 
-    private void insertSucceededPaymentIntent(String id, String customerId, long amountCent) {
+    private void insertCreatedPaymentIntent(String id, String customerId, long amountCent) {
         jdbcTemplate.update("""
                 INSERT INTO finance_payment_intents (
                     id, business_order_type, business_order_id, amount_cent, currency, channel,
                     status, created_at, updated_at
                 )
-                VALUES (?, 'stored_value_topup', ?, ?, 'CNY', 'wechat', 'succeeded', now(), now())
+                VALUES (?, 'stored_value_topup', ?, ?, 'CNY', 'pos', 'created', now(), now())
                 ON CONFLICT (id) DO UPDATE SET
                     business_order_type = EXCLUDED.business_order_type,
                     business_order_id = EXCLUDED.business_order_id,

`````

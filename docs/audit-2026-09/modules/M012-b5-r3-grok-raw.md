CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M012 B5-r3 修复后完整模块独立审计（Grok 席乙）

文件版本：V0.01
步骤：B5-r3
席位：固定 Grok 外审席乙 grok-4.6/xhigh
计划：160-audit-prompt.md V0.17 §4/B5；runbook.md V0.02；criteria-lock.md V0.11
本文件路径：docs/audit-2026-09/modules/M012-b5-r3-grok-raw.md
本文件自身版本：V0.01（指定路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

审计结论：**FAIL**
（有未完不得 PASS。本席未把 B3=3/3 上限折成 PASS，也未用 B4 双 PASS 替代本次整模块重审。）

未读取：任何 `M012-b5-r3-self-raw.md`。未用 MCP、未派子代理、未提问、未改源码/测试/HTML/锁/台账、未 commit/push/merge/deploy、无数据库写入。唯一写入即本文件。

---

## 0. 锁、范围、技能与输入

本席独立命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（未抄 sidecar / ledger / 派发期望值）。

| 项 | 值 |
|---|---|
| 实算锁 | `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a` |
| sidecar `plan/criteria-lock.sha256` | 同行同值 |
| ledger 顶部 | 同值；`LOCK-AMEND-COUNT: 1` |
| criteria-lock 版本 | V0.11 |
| 映射 | `M012 → [BSM-001, BSM-003, GEN-001 … GEN-018, PGM-001]`，21 项，未增判据 |
| 正本 | `/home/james/complex-management`，分支 `main`，HEAD `e456ba96c84185c8d39f18f548011daac48a6466` |
| 计划冻结行数 | 1485（`plan/modules.md` M012，不改） |
| 当前六源合计 | 2088 行 |
| after tree | `9b1e71aa9eef993d24abdb1c7d8b2d4d8c0e1f2f`（`git cat-file -t` = tree） |
| B3 | 累计 3/3；findings V2.19 顶部 010–016 队列否；历史 001–009 否保留 |
| B4-r3 本席票 | `modules/M012-b4-r3-grok-raw.md` V0.01 SHA256 `9dc2dd94b247ecc6a25a39be2fa83347816c98f0dcd8923bdb9dcc0511d71a52`；只作 56 单测/typecheck 实跑证据与 after blob 对照，不替代本次 B5 |
| questions | `M012-006.md` / `M012-007.md` 答复段首行均为精确 `删除`；无未答 |
| 三份业务 HTML | 只读；未改 |

六源独立核 SHA（工作区 blob = after tree blob，MATCH）：

| 文件 | 行 | git blob | sha256 |
|---|---|---|---|
| `GovernanceTopologyPage.tsx` | 276 | `d7a39b9c7a4361b364391df91e4e82bfe36aae62` | `6204706ba5c97e53bfeb200799ddc7a39b615a18f69e1d71607dfb56765a5a89` |
| `GovernanceTopologyPage.test.tsx` | 308 | `5bf4bf26411f020c81780c66681b6485099f539f` | `985196ad40864145f1be814cad1f5db6667883a114aed27e61453188219a28cb` |
| `OrganizationMerchantPage.tsx` | 521 | `a2cb209930a4146a88b2090ed25ef3967d773a3d` | `28f31ae9b3b731c7e3695cb3d928fc69613b1124b7d5dd970a3586e58b2c4d49` |
| `OrganizationMerchantPage.test.tsx` | 435 | `b2149d21ac69a26ebfb87d4d98632467dae50bd0` | `d40c83d948e0b07f83f6eb5bcbb2a18af12b186ddd93fdcc651557d5870408a7` |
| `SubjectBindingsPage.tsx` | 257 | `0d916b81f72c4d8ea0acf46e43e85121e2ba71db` | `94f4d4dcfcb24c85701450794544574efe26f209a4aed242ff5c428af98d247c` |
| `SubjectBindingsPage.test.tsx` | 291 | `e145f2b54a5c3271d7601c382044486e37720872` | `ec22a9250ed24f61031f1997119b5537d77d1af537e388eff2018fa947d8591b` |

全文覆盖（非抽样）：上表六文件均从头读到末行。依赖只读追流：`useServerPagedRows.ts`、`useAdminResource.ts`、`TablePager.tsx`、`adminClient.ts` `getPaged`/`unwrap`/`unwrapPaged`、`statusLabels.ts` `formatBusinessStatus`、`AuditTrailHint.tsx`、`HighRiskConfirm.tsx`、`AdminDataState.tsx`、`PermissionGate.tsx` `hasPermissions`、`PlatformController.java` 本页所用 GET/POST 权限、`PageResponse.of`、`PlatformService.listSpaceTypes`。未扩到 M013+ 或全库。

### 技能自检（V0.17 §6 收窄；存在≠冒充调用）

| 技能 | 本环境 | 本节点用法 |
|---|---|---|
| `grilling` / `grill-me` | 有（mattpocock productivity） | 只借追问结构，事实自读，不等操作员、不派子代理 |
| `batch-grill-me` | **缺口：无独立技能文件** | 未冒充调用 |
| `codebase-design` | 有 | 只借模块/接口/缝语言，描述分页查找缝 |
| `diagnosing-bugs` | 有 | 只读追 013 关联字典 |
| `ponytail-review` | 有 | 只评最小性，不落地 |
| `verification-before-completion` | 有 | 新疑点实跑后再下结论 |
| `using-superpowers` | 有 | 开工自检 |
| MCP / 任何子代理 | 禁用 | 未用 |

---

## 1. 六文件覆盖记录

| 文件 | 覆盖行 | 为何必须存在（删掉则 HTML 哪条做不到） | 本席阅读结论 |
|---|---|---|---|
| `GovernanceTopologyPage.tsx` | 1–276 全文 | BSM-001「哪个综合体、谁在经营、在哪经营」；PGM-001「先管归属」 | 主体/商户正向 `merchant.projectId`、实体分页与缺权「-」、空间字典中文、本页 KPI、getPaged 均在；**项目查找仍把未加载写成无项目**（见 GR-M012-B5-01） |
| `GovernanceTopologyPage.test.tsx` | 1–308 全文 | GEN-003 覆盖拓扑主路径 | 19 例：KPI、中文状态/类型、真实 project_id、缺权不请求、实体第 2 页未加载、501 商户/空间、键盘、503 清错。无 501 项目查找例 |
| `OrganizationMerchantPage.tsx` | 1–521 全文 | 同上归属 + BSM-003 引导新建/存档删除分岗 | 商户 `projectId` 正向、项目表不再反属单商户、局部中文状态、getPaged、筛选后页、末页存档回第 1 页、缺权不请求主体；项目查找用「项目资料未加载」 |
| `OrganizationMerchantPage.test.tsx` | 1–435 全文 | GEN-003 | 21 例：真实字段、引导创建、存档/删除、缺权、暂停≠黑名单、分页筛选、键盘、503 清错 |
| `SubjectBindingsPage.tsx` | 1–257 全文 | PGM-001 合同/结算责任口径；BSM-001「签了什么合同」 | 真实绑定字段、导语已删附件声称、三态+原因、409 可见、getPaged、本页统计、行快捷键收窄 |
| `SubjectBindingsPage.test.tsx` | 1–291 全文 | GEN-003 | 16 例：KPI、真实字段、变更校验、409、暂停、501、键盘、503 清错 |

---

## 2. 第一性原理线

1. **为什么必须存在？** 三页是治理网页入口：拓扑展示集团/综合体/商户/项目/空间关系；组织页维护主体/商户/项目档案并引导新建与存档删除；绑定页展示四类主体唯一关系并提交状态变更。删掉则 BSM-001 基础盘子与 PGM-001 归属/资格/责任在后台网页无法操作。答得上，有业务上位。
2. **声称的行为有没有被实际执行验证？** 与当前六源 blob 全等的 B4-r3 本席票记录：`cd apps/web && npm run test:unit --` 三测试文件 **56 passed**（19/21/16），exit 0；`npm run typecheck` / `tsc -b --noEmit` exit 0；保留 jsdom `scrollTo` / `getComputedStyle` 提示。本席核当前 SHA 与该票 after blob 全等，不无故重跑同一 56/类型套件。新疑点（501 项目查找）本席另跑只读算法探针，见 §5。未跑浏览器、真实 HTTP、后台、数据库。不把 mock 成功当实际业务放行。
3. **哪个前提未经验证就当真？** 生产默认 `size=500` 下，拓扑页假定「当前项目页」足以解释全部已加载商户的 `projectId`。该前提未经验证，且与 013 已要求的关联字典规则冲突（§5）。

---

## 3. 顺从式线（21 项逐项）

判据全文以 `criteria-lock.md` 定义为准；模块侧只引用。

| ID | 适用性 | 当前证据 | 本席 |
|---|---|---|---|
| BSM-001 | 适用。HTML：哪个综合体、谁在经营、在哪经营、签了什么合同 | 三页读取 entities/merchants/projects/spaces/bindings；拓扑/组织按 `merchant.projectId` 正向连项目。拓扑在项目第 2 页未入内存时把已有归属画成无项目 | 未完，见 GR-M012-B5-01 |
| BSM-003 | 适用（h2 通用规则）。存档删除/分步引导的细目属 BSM-004/005，不在本模块映射，不另增判据 | 组织页引导新建、存档理由、删除申请/审批、权限按钮；绑定页 `entity.binding.manage`；拓扑只读 | 够 |
| GEN-001 | 适用 | 页内局部类型/词表，未造新通用分页器或归属配置器 | 够 |
| GEN-002 | 适用 | 主路径与 010–012、014–016 声称与源一致；拓扑项目查找声称「展示治理关系」在 501 项目时执行成「-」 | 未完，见 GR-M012-B5-01 |
| GEN-003 | 适用 | 56 例能失败断言覆盖 KPI、中文、权限、分页、409、键盘、503。缺拓扑 501 项目查找的可失败断言 | 测试缺口随 GR-M012-B5-01 |
| GEN-004 | 适用（前端输入） | 创建商户非空校验；绑定 action/原因必填；存档/删除理由；信任 API 形状由页内类型收口 | 够 |
| GEN-005 | 适用 | 缺 `entity.binding.view` 不请求主体（拓扑+组织）；写入口按钮权限；后端仍鉴权。不把界面隐藏当唯一闸 | 够 |
| GEN-006 | 本页无事务；适用收窄为前端不得部分成功假象 | 写失败 catch；绑定 409 保留表单；创建失败弹窗留开 | 够 |
| GEN-007 | 适用收窄 | `submitting`、HighRiskConfirm 二次确认、idempotencyKey。双击幂等/Date.now() 键属 009 已登记观察 | 不升号 |
| GEN-008 | 适用 | 夹具 snake_case 真实字段；商户 `project_id`、项目 `merchant_id` 可空；绑定无捏造 `project_id`。拓扑项目未加载与「无项目」同形，和本页实体列/组织项目列不一致 | 未完，见 GR-M012-B5-01 |
| GEN-009 | 适用 | 三页读取开始 `setGlobalError(null)`；503 后重试清错；真实失败仍 `onError`；绑定/存档失败可见 | 够 |
| GEN-010 | 适用收窄 | 无注入/密钥；展示走文本节点；错误走翻译层 | 够 |
| GEN-011 | 适用 | AuditTrailHint 折叠事件名；写路径 `setActionResult`；抽屉可点操作审计（组织商户） | 够 |
| GEN-012 | 适用 | getPaged 500 + TablePager，不无限抓全。拓扑项目无翻页入口，501 时后页不可达 | 并入 GR-M012-B5-01 |
| GEN-013 | 不适用 | 六文件无构建配置/依赖清单 | 记录不适用 |
| GEN-014 | 不适用 | 六文件无迁移 | 记录不适用 |
| GEN-015 | 适用 | 行快捷键 `event.target === event.currentTarget`；radio Space、TraceLink/存档 Enter 有例；局部中文 | 够 |
| GEN-016 | 适用（页内证据/导语） | 绑定导语已改为选择状态并填原因，无附件声称；接口路径在 TechDetails | 够 |
| GEN-017 | 适用 | 本席一条新问题按冲突→细化→技术性→添加判定为细化；未增锁外判据 | 见 §5 |
| GEN-018 | 适用 | 结论落到 HTML 原文、B4 实跑 56（SHA 核验）、本席算法探针命令输出 | 本报告 |
| PGM-001 | 适用。归属、全中文、可点击 | 局部状态中文、空间字典中文、暂停≠黑名单、未知码「未识别」；KPI URL 英文键与内部 ID 属 009。拓扑把已有 `projectId` 显示为无项目，归属展示错误 | 未完，见 GR-M012-B5-01 |

---

## 4. 对抗式线（grilling 结构，事实自读）

1. **越权：** 无 `entity.binding.view` 是否仍请求 `/business-entities`？源：拓扑 70–77、组织 136–142 走 `Promise.resolve({ items: [], total: 0 })`。测试：拓扑 192–229 断言未请求且单元格 `-`、抽屉不含「主体资料未加载」；组织 342–346 同。通过。
2. **501 默认页：** 商户/空间/绑定/组织商户后页是否可达？有例。实体第 2 页是否「未加载」而非「-」？拓扑 276–288 有。**项目第 2 页作为商户归属字典？** 拓扑无项目 pager（仅 merchants/entities/spaces），查找失败与 `projectId==null` 同形。失败，见 §5。
3. **筛选后页 / 末页存档：** 组织 358–386、398–415。通过。
4. **重试残留全局错误：** 三页 fetch 包 `setGlobalError(null)`；顶层 503 例要求 lastCall `null`。通过。
5. **暂停 vs 黑名单：** 页内词表 `suspended: "暂停"`，`blacklisted: "黑名单"`；不走全局 `formatBusinessStatus` 的 `suspended: "已封禁"`。有例。通过。
6. **子控件键盘：** 三页 `target === currentTarget`。通过。
7. **空间类型英文码：** `formatSpaceType` 字典 `typeName` 优先，否则八码中文，否则「未识别类型」。kiosk 例。`api.get`+`unwrap` 会拍平 `data.items`；`listSpaceTypes` 一次返回字典。通过。
8. **绑定 409：** 测试断言财税复核可见且原因保留。默认 POST mock 成功不冒充后台放行（009）。通过。
9. **导语附件：** `SubjectBindingsPage.tsx:45` 无附件/幂等声称；请求体 168–172 无附件。通过。
10. **组织项目表反属商户：** 项目表列名/编码/业态/状态；测试 `projectsText.not.toContain("m-1")`，商户行含「项目一」。通过。
11. **并发双提交 / 附件上传 / 创建绑定 / 归属配置器 / 项目三项表单：** 009 原边界，不补、不升号。

---

## 5. 当前 010–016 与历史 001–009

对照 findings V2.19 最小边界与当前源（不只看编号）。

| ID | 队列 | 当前源核 | 本席 |
|---|---|---|---|
| M012-010 | 否 | 拓扑 195 `projects.filter((project) => project.id === merchant.projectId)`；组织 302–304 正向名称；项目表无「属于一个商户」。例：拓扑 236–239、组织 146–147 | **正向连接本身够；未加载时拓扑仍画成无项目，不在 010 反连范围内，归 013** |
| M012-011 | 否 | 夹具商户 `project_id`、项目 `merchant_id: null`；缺权不请求；绑定 409。未向 BindingRow 捏造 `project_id` | 够 |
| M012-012 | 否 | 导语：「变更时须选择状态并填写原因。」 | 够 |
| M012-013 | 否（B4 后转否） | getPaged+TablePager+真实 total；本页 KPI 文案；筛选后页；末页存档；实体未加载 vs 无权 `-`；读取开始清全局错误且失败仍上报。**拓扑项目字典：无 pager，未加载显示 `-`，与无权/未关联不可分** | **未完。不能因 B4 转否或 B3=3 降为够** |
| M012-014 | 否 | 三页局部中文表；暂停≠已封禁；未知「未识别状态」 | 够 |
| M012-015 | 否 | `event.target === event.currentTarget`；三页子控件例 | 够 |
| M012-016 | 否 | 字典优先 + 八码兼容 + 「未识别类型」；kiosk→一体机位 | 够 |
| M012-001…008 | 否 | 真实绑定字段；action 三态；空间 `projectId`；中文业态/表头；三角色/六类合作/绑定三态夹具；无预填假证据；电话非空非 11 位正则；写路径 catch | 仍成立 |
| M012-009 | 否登记 | 不补附件上传、创建绑定、项目归属配置器；NULL 商户不可见不要求本页恢复；财税守卫不以 mock 成功代替；项目负责人/地址/经营范围不要求本页新表单；KPI URL 英文键、内部 ID/TraceLink、双击幂等只登记 | 保持原边界 |

---

## 6. 临时 findings

### GR-M012-B5-01

- **文件:行：** `GovernanceTopologyPage.tsx:85–90,195–211,216–217`（项目 `useServerPagedRows` 默认 500；查找 `projects.filter(project.id === merchant.projectId)`；空则 `[undefined]`；项目列 `display(project?.name)`；pager 仅 merchants/entities/spaces，无项目翻页）。对照组织页已做的区分：`OrganizationMerchantPage.tsx:302–304`。
- **真实正常业务触发：** 后台项目列表超过生产默认 500 条（与 013 已接受的 501 商户/空间/实体/绑定同一门槛），某商户 `projectId` 落在项目第 2 页。打开「集团/综合体拓扑」——无需并发、无需故障、无需缺权。第一次正常浏览就会把该商户项目列画成「-」，testid 为 `governance-topology-row-<merchantId>-none`，与 `projectId` 为空相同。
- **原句 / 证据：**
  - 源：`const rows = merchantProjects.length ? merchantProjects : [undefined];` 随后 `<td>{display(project?.name)}</td>`。`display` 对 undefined 返回 `"-"`。
  - 同文件实体列已按 013 写成：`canEntityView && merchant.businessEntityId ? "主体资料未加载" : "-"`，且 `entityRows.pageCount > 1` 时有 `governance-entities-pager`。
  - 本席只读探针（当前源算法原样执行，exit 0）：

```
node <<'EOF'
# display + 拓扑查找 + 组织 merchantProjectName，projects = 首页 500 条
{"merchant":"m-late","testid":"governance-topology-row-m-late-none","topologyProjectCell":"-","orgMerchantProjectName":"项目资料未加载","matched":0}
{"merchant":"m-first","testid":"governance-topology-row-m-first-p-page-0","topologyProjectCell":"项目0","orgMerchantProjectName":"项目0","matched":1}
{"merchant":"m-null","testid":"governance-topology-row-m-null-none","topologyProjectCell":"-","orgMerchantProjectName":"-","matched":0}
EOF
```

  - 定稿 013：「关联字典不因独立首页截断就误判不存在；若仅展示局部明确未加载，而非未关联。有权关联页未加载与无权「-」区分。」项目在此页是有权加载的关联字典。
- **2.1 分类（依序）：** 能指到 PGM-001「先管归属：哪个综合体…哪个商户」与 BSM-001「哪个综合体、谁在经营、在哪经营」→ **细化**（不是与明文直接打架的冲突，也不是业务发明）。
- **2.3 依据类型：** 4（可复现命令+输出）并辅以 1（HTML 归属原文）。
- **冻结判据：** M012-013（已有正式 ID，当前修复队列为否）；GEN-002/003/008/012；BSM-001；PGM-001。
- **工程缺陷：** 是。B4 后把 013 改否并不使当前源满足其关联字典条款。
- **不在范围：** 不要求聚合 API、不抬 500、不改 types/后台、不把 NULL `projectId` 历史商户改回可见（009）、不为此新增锁外判据。最小方向仍是 013 已用过的：未加载明确文案 + 既有 TablePager 让项目后页可达。

无其它新 ID。无添加项。无 HTML 申请。无 LOCK-AMEND。

---

## 7. 验证与限制

采用：B4-r3 本席 raw 对**当前相同 blob** 的 56 单测 / typecheck 实跑记录（命令见该票 §7）；本席复核 SHA MATCH 后不重复同一套件。新疑点算法探针见上，未冒称未跑，也未把该探针写成 vitest 文件（禁止改测试）。

未运行：真实浏览器、真实 HTTP、后台测试、Maven、psql、数据库写入。jsdom `scrollTo` / `getComputedStyle` 提示仍属测试环境限制，不称浏览器验证。

未把 mock POST 成功当成财税守卫放行。

---

## 8. 整批结论

六文件全文已覆盖。21 项已逐项核对。三线已执行。B4 双 PASS 与 010–016 队列转否不替代本次 B5。

**FAIL**

唯一未完：GR-M012-B5-01（拓扑项目关联字典在 501 条时误判为未关联）。沿用已有 ID M012-013，但其当前队列为否，按 §4 回退规则本批须经 B2，不得因 B3 已 3/3 把 FAIL 写成 PASS。本席不改 findings/ledger，不修文件。不等于模块 PASS。

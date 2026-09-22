CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# M009 B5-r6 Grok 整模块三线审计 V0.01

席位：Grok（乙）。步骤：B5-r6（修复后整模块重审，不只看 4 文件 diff）。B3 累计 6（操作员批准的第 6 次发布修复已执行；本席不改 ledger）。  
正本：`/home/james/complex-management` main 工作区。HEAD 只读观测 `e456ba96c84185c8d39f18f548011daac48a6466`；本地 `origin/main`=`d8f12c33766c2a0f39a45d5f4b8d66f6753804e4`（未 fetch）。  
规程：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`；`docs/audit-2026-09/plan/runbook.md` V0.02 SHA256 `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f`；`plan/modules.md` V0.04 M009 同一 20 文件；现锁 `plan/criteria-lock.md` V0.14 映射 88 条。  
**禁止项已遵守**：未读本次 `M009-b5-r6-self-raw.md`（开工时路径不存在；之后亦不打开）；不继承 B4/B2/历史 PASS 作为本 B5 通过依据；不改生产代码、测试、三 HTML、锁、台账、主 `.git`；不 fetch/提交/推送/部署/安装/MCP/子代理；不为重复验证写库；不向旧 `complex_test` 再应用 432；不 repair/delete history/outOfOrder/ignore/IF NOT EXISTS。临时对照只在正本 `.scratch/`（本席 `.scratch/m009-b5-r6-grok/`），未用 `/tmp`。无新门槛/判据/产物类型。先找齐问题再落盘，边审边不修。

findings.md **V3.57** 顶部 30 项当前队列全部为否（26 历史 + 060/061/062 刚修 + 063 验证边界）。本席仍逐项重判，不把「全部否」当成测试已完备。

三项批准按 HTML **现文**（不引旧禁令）：PGM656 单次消耗原句保持；PGM871 授权商户联系人列表/操作结果可明文；PGM680 该页面查看例外不授未脱敏导出或打印。不扩姓名/生日/人脸、不回填旧数据。

本席步骤结论：**PASS**。新 finding 临时编号：无。30 项本次全部重判为当前工程缺陷否、建议队列否。无添加、无 HTML 修改申请、无 LOCK-AMEND。本票不是双席合取、不是 ledger 模块 PASS。

---

## 0. 技能预检（§6，本宿主实核路径）

已读并用其收窄口径（不派子代理、不等操作员、B5 期间不修）：

| 技能 | 路径 | 本步用法 |
|---|---|---|
| using-superpowers | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工自检；用户/控制稿优先于派子代理 |
| grilling / grill-me | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 对抗/第一性追问结构；事实自读；本体「等用户 / 派子代理」作废 |
| diagnosing-bugs | `.../mattpocock-skills/1.2.3/skills/engineering/diagnosing-bugs/SKILL.md` | 发布面反馈环=编号闸 + 两 `--check` + `verify:predeploy` |
| verification-before-completion | `.../superpowers/6.3.0/skills/verification-before-completion/SKILL.md` | PASS 前必须有本席本步命令输出；旧 XML 不写成新跑 |
| codebase-design | `.../mattpocock-skills/1.2.3/skills/engineering/codebase-design/SKILL.md` | 脱敏缝 Controller；加密缝 JDBC；USB 识别缝 AuthorizationService 三常量；编号缝 Flyway 文件名 |
| ponytail-review | `/home/james/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只找可删；048 隔离通道不删；不为删 origin 依赖而改 Java |
| systematic-debugging | `.../superpowers/6.3.0/skills/systematic-debugging/SKILL.md` | 只借读错→复现；B5 不改码 |
| writing-for-agents | `.../mattpocock-skills/1.2.3/skills/productivity/writing-for-agents/SKILL.md` | 本 raw 给调度合取 |
| requesting-code-review | 同 superpowers | B5 外审=本席；禁止 `HEAD~1`、禁止子代理复审 |
| receiving-code-review | 同 superpowers | 先核哈希/命令再接受或反驳；禁止表演性同意；B5 不 IMPLEMENT |
| long-running-background-tasks | `/home/james/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | `verify:predeploy` 一次跑完再采信 |
| graft | `/home/james/.claude/skills/graft/SKILL.md` | 已读 SKILL；本步权威证据是 hash/cmp/闸门/只读 SQL，未调 graft MCP |

缺口（不冒充已调用）：本宿主未核到独立 `i-have-adhd` SKILL.md（按提示词输出口径执行）；可选 `batch-grill-me` / `ubiquitous-language` / `git-guardrails-claude-code` 本步未调。`tdd` 存在但本步不落测试。划掉的子代理/worktree/research/Arc 全仓 audit 不用。

---

## 1. 判据锁哈希（本席独立实算）

命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（Python hashlib / openssl 同值；未抄 sidecar、ledger 或派发期望）。

| 源 | 值 |
|---|---|
| 本席实算 | `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458` |
| `plan/criteria-lock.sha256` | 同上（文件内容一行） |
| `ledger.md` 顶部 `CRITERIA-LOCK-SHA256` | 同上 |
| lock 字节 / 行 / 版本 | 891182 / 6856 / V0.14；定义 401；M009 映射 88 unique 88 MISSING=[] |
| LOCK-AMEND-COUNT（只读） | 1 |

实算 = sidecar = ledger。第四值由调度在两席完整 raw 落齐后取本段首行。本席不改 sidecar/ledger，不把四值闸写成已代 Codex 收口。

---

## 2. 范围、行数、哈希与阅读复用

权威清单：`plan/modules.md` M009 `<!-- FILES-BEGIN -->` 至 `FILES-END`，20 路径，标记 NORMAL。冻结行数仍写 2476；**本席实跑** `git ls-files -z -- <20 paths> | xargs -0 cat | wc -l` = **3381**。

**阅读口径（如实，不冒称新读 3381 行）：** 本席对本 20 文件逐文件 SHA256，与本席 **B1 V0.17** 表及 **B5-r5 V0.02** 表 **MISMATCH=0**。因此复用该两次全文阅读作为源码理解，**本段 88 判据、30 ID、三线、发布面均重新判断**，不继承 B5-r5 PASS，不把 B4-r6 PASS 当本 B5 通过依据。本次 B3 未改这 20 个 Java 文件。

| 行 | SHA256 | 路径 | 覆盖 |
|---:|---|---|---|
| 17 | 45431c580d0bca0c217bcdb8ea120604eb7ed2b441845a5ac52257d5130b1277 | `.../governance/BusinessEntityRecord.java` | 复用全文 |
| 19 | f97af4de8ed4d45134196b4fcb83169591d5991e6aefaa4aa22411a49e78f422 | `.../governance/BusinessEntityBindingRecord.java` | 复用全文 |
| 116 | dda319c37032c33c3cc5d260a0c9f43c7740d21b80a106c4a4efd49a19cdef00 | `.../governance/MerchantRecord.java` | 复用全文 |
| 9 | e08618c856b35a1eda8447a9c9bd0c88ed00a9ee371c1ee33d3f2f3ab7ce5e40 | `.../governance/MerchantDeleteDecision.java` | 复用全文 |
| 1338 | 84c868bb9739fc68d630ac6c7da1c99767c7a974ba2e763102ba473f52618855 | `.../governance/JdbcPlatformRepository.java` | 复用全文；本步抽核 encrypt L625-632 / 占用 L221-224 |
| 302 | 53d38fcb4910ba0de5acc7418936d3388673b662a0b0a427dced54b5baff0bc6 | `.../governance/PlatformController.java` V0.03 | 复用全文；本步抽核七入口 helper L281-288 |
| 204 | df6dfee9894fc34f1df28fdae3d549ba50320df14219129779fa267affb08cdc | `.../governance/PlatformRepository.java` | 复用全文 |
| 1179 | 993cfdb8cdacb285a37027c8bfda1c983fcb5c3e0f26f7462f2235454f45827e | `.../governance/PlatformService.java` V0.12 | 复用全文；本步抽核 merchant.viewed L152-165 |
| 1 | e3094c51b508bf10015ead9e92ac2ae2f3edf3b09786d88816ab8009d21921d9 | `.../governance/package-info.java` | 复用全文 |
| 17 | 66d4a8cd5c7f206f0c023e190ac5ed94ff6585263a737e35f062be9d8de211da | `.../dto/BusinessEntityBindingRequest.java` | 复用全文 |
| 26 | 18911088cbde9575ca7c38f9a6ac6eee3f4a812098aea9a29482133fe14dec19 | `.../dto/BusinessEntityRequest.java` | 复用全文 |
| 6 | e0f16f5417b61e801ed23803616450e90810853ec7588a49714b43040d809f93 | `.../dto/MerchantArchiveRequest.java` | 复用全文 |
| 5 | 2516fb1d78e082fe89222a41f7178dde4b40d749c5ed2c2e712bbb14a74112c4 | `.../dto/MerchantDeleteApprovalRequest.java` | 复用全文 |
| 5 | 4898e50f2150e27c73d6d400b317d792f02c342a0e95a8b3dadbef5a7ebc8f45 | `.../dto/MerchantDeleteRequest.java` | 复用全文 |
| 41 | 7b6dd241328b57dc646eb9b3c531e01e3a3a3d1bb1b848711c7ce43106973cf1 | `.../dto/MerchantProfileRequest.java` | 复用全文 |
| 26 | 6ac09b14c1766bebd6df69fbf5579ccf602959d10179b497ff90ea1b3f521108 | `.../dto/ProjectRequest.java` | 复用全文 |
| 21 | 63857df14972fd6904a32fc7aade0de49b88ec3b946df493f61086c77d1ab7aa | `.../dto/SpaceRequest.java` | 复用全文 |
| 16 | 7c3a5fa28ab5a45b49d3fc325e39d1f25ddcbc38b86de3872ca86ce338b05f34 | `.../dto/SpaceTypeRequest.java` V0.01 | 复用全文；注释=新建不可重复固定启用 |
| 11 | 75b75cfaee897096f6fce0886fc4e71da8c5762774a901739cff244e1425f60f | `.../dto/StatusChangeRequest.java` | 复用全文 |
| 22 | 6347c6dfe087be2310fe5f618a0ac27e188f340d1af84add2ab831e98cb333f1 | `.../dto/VenueBusinessHoursRequest.java` | 复用全文 |
| **3381** | | | |

未把 `ProjectRecord.java` / `VenueBusinessHoursService.java` / `ProjectAssociationRequest.java` 扩进本模块 FILES（依赖只读，XM 不扩）。

### 2.1 本次 B3 四逻辑文件（after 树，工作区 blob 全等）

before `6919c011af93e31ccbba13f57398f821b13c8c15` → after `e2871dc1226a626aa47b6f33ab2d805ec1a542ff`。本席独立 `git diff --binary`（无路径过滤）→ **125823** bytes，SHA256 **`108fb84a3facc14d745f2d878bc906ce4a1b962038343d94aa22a4d9b00857af`**（与 B4 两席声称全等；落盘 `.scratch/m009-b5-r6-grok/b5-r6-full.diff`）。`git diff --check` exit 0。

| 路径 | 工作区 blob = after | 要点 |
|---|---|---|
| `V432__m009_project_profiles_and_associations.sql` | `e485a5d2…` = 原 V427 blob | R100 纯改名；头仍 `-- 文件版本：V0.01`；无 V427 文件 |
| `docs/data-isolation-and-tenant-scope-security-spec-v0.1.md` | `6f68768e…` | 标题 V0.04；AUTO 最新 **V432** / **172** 张 |
| `docs/domain-ownership-scope-audit-v0.1.csv` | `47fe6601…` | 336 数据行；`file_version` 全 V0.02；`merchants`/`business_entity_projects` 均为 `direct,project_id` |
| `tools/generate-domain-ownership-scope-audit.mjs` | `f530e3ab…` | 头 V0.05；`file_version: "V0.02"`；`status` 仍 `"covered"` |

真实 index 文件 SHA256 **`d9a65108a1e87bd300c299ca7640f832bd5c2feb771f35a7a2413d32a1748331`**（`sha256sum .git/index`）。本席未写 index。

---

## 3. 三项批准 — 现文逐字（不引旧禁令）

出处均为 `docs/platform-governance-map-for-operations-v0.1.html` 当前字节（`<!-- 文件版本：V0.13 -->`）。锁：L656∈PGM-041；L680∈PGM-043；L871∈PGM-059。本席对现文对应行与锁摘录核对，命中。

**PGM656（L656）**

> 临时授权可设有效期或单次使用；权限的授予、收回、到期、单次消耗、失效全部留痕。

**PGM871（L871）**

> 员工日常界面只显示脱敏号码，不见明文；界面脱敏不提供关闭开关。商户档案中的第一联系人和第二联系人手机号除外：经单独授权且查看权限有效的人员，以及持授权 U 盘密钥的最高权限本人，可以在其获准访问的商户档案列表和档案操作结果中查看完整号码；其他人员仍只显示脱敏号码。平台管理员身份本身不自动取得该查看权限。该例外仅适用于商户档案联系人，不扩大到客人或员工档案的手机号，也不免除项目范围检查和查看留痕。

**PGM680（L680 该句后段，同一 `<li>` 末）**

> 商户档案页面中的第一联系人和第二联系人手机号，按“客人与员工的个人信息保护”条中明确的查看权限例外执行；该页面查看例外不授予未脱敏导出或打印权限，也不免除敏感页的查看留痕及其他页面保护要求。

本 20 文件无 export/print 入口（本席对 20 路径检索无业务导出口）。未发明导出口径、姓名生日展示规则或旧数据回填。

三 HTML 工作区 SHA = 本席 B1 V0.17 = 当前 `origin/main`：

| 文件 | SHA256 |
|---|---|
| 枚举 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| 业务地图 | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| 治理地图 | `5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f` |

HTML 闭集现文件：BEQR `td.k` **197**；BSM h2+h3 **27+93=120**；PGM h2+h3 **18+48=66**。与锁声明一致。本模块 70 条 HTML 派生：沿用 B1 V0.17 在**同 SHA HTML** 上的 70/70 标题/首行命中（本步未改 HTML，不冒称重扫全部锚点正文）。GEN 18 条定义区全文。无 LOCK-AMEND 提案。

---

## 4. 二十文件覆盖记录（无 finding ≠ 未审）

| 文件 | 审过 | 要存在的理由 | 本轮要点 |
|---|---|---|---|
| package-info.java | 是 | 编译单元 | 仅 package 声明 |
| BusinessEntityRecord.java | 是 | 四类主体档案 | 无联系人电话 |
| BusinessEntityBindingRecord.java | 是 | 唯一绑定时段 | effectiveFrom/To/status |
| MerchantRecord.java | 是 | 商户档案+投影+脱敏副本 | withMaskedPhones；projectId |
| MerchantDeleteDecision.java | 是 | 删除审批结果 | 无电话字段 |
| PlatformRepository.java | 是 | 仓储 seam | 项目分页；listMerchantsByBusinessEntity 隔离通道 |
| JdbcPlatformRepository.java | 是 | 真源持久化 | 姓名/电话 encrypt 写 decrypt 读；owner_person 不加密 |
| PlatformController.java V0.03 | 是 | HTTP 授权+脱敏 | 七个商户响应入口共享 requireMerchantPermission + mask；approve 删除只功能权 |
| PlatformService.java V0.12 | 是 | 规则与留痕 | 项目隔离、绑定财税闸、退出缓冲、merchant.viewed |
| BusinessEntityBindingRequest | 是 | 绑定写入 | 无 status 直写 |
| BusinessEntityRequest | 是 | 主体创建 | 可选 projectId |
| MerchantArchiveRequest | 是 | 存档理由 | idempotency+reason |
| MerchantDeleteApprovalRequest | 是 | 审批 | decision/comment |
| MerchantDeleteRequest | 是 | 删除申请 | reason |
| MerchantProfileRequest | 是 | 档案 | 联系人姓名电话+projectId |
| ProjectRequest | 是 | 项目档案 | ownerPerson/address/businessScope 可空 |
| SpaceRequest | 是 | 空间 | projectId 必填于服务层 |
| SpaceTypeRequest V0.01 | 是 | 新建字典 | 注释：新建、编码不可重复、新建启用 |
| StatusChangeRequest | 是 | 绑定变更 | action/reason |
| VenueBusinessHoursRequest | 是 | 营业时段 DTO | Controller 委托 VenueBusinessHoursService（不在 20 文件） |

---

## 5. 88 判据全部定义 + 适用面（重新判断）

映射原文（`criteria-lock.md` L6394）：88 个稳定 ID，定义区 `### <ID> —` 抽出 88/88，缺 0。适用：D=本 20 文件有直接实现；B=边界；N=无实现面。相对 B1 V0.17：**仅 GEN-014 / GEN-016 由「发布闸红」改为「本步命令绿」**；其余 86 项源码同字节、现文锚点仍命中，重新判断为无新实现缺陷。

| ID | 标题 | 本模块 | 本席本步 |
|---|---|---|---|
| BEQR-063 | 高危动作 | N/B 删审占用非酒吧全集 | 无新缺陷 |
| BEQR-074 | 隐私桌台 | N | 无新缺陷 |
| BEQR-143 | 四类主体 | D 绑定+投影 | 无新缺陷 |
| BEQR-144 | 经营主体关系 | D | 无新缺陷 |
| BEQR-145 | 城市项目公司 | D project_company | 无新缺陷 |
| BEQR-146 | 运营公司职责 | B management_company | 无新缺陷 |
| BEQR-147 | 主要收入归属 | N | 无新缺陷 |
| BEQR-148 | 合作模式 | D cooperationForm 六类 | 无新缺陷 |
| BEQR-149 | 职级 | N | 无新缺陷 |
| BEQR-150 | 终端十一端 | N | 无新缺陷 |
| BEQR-151 | 最终权限计算 | B AuthorizationService | 无新缺陷；消费语义不可因编译丢 |
| BEQR-152 | 项目与敏感数据权限 | D currentProject | 无新缺陷 |
| BEQR-153 | 终端开关优先级 | N | 无新缺陷 |
| BEQR-154 | 综合体项目状态 | D PROJECT_STATUSES | 无新缺陷 |
| BEQR-155 | 商户/运营主体状态 | D 拒直接 terminated | 无新缺陷 |
| BEQR-156 | 合同状态 | N | 无新缺陷 |
| BEQR-157 | 员工状态 | N | 无新缺陷 |
| BEQR-158 | 设备状态 | N | 无新缺陷 |
| BEQR-159 | 权限状态 | N | 无新缺陷 |
| BEQR-166 | 合同审批链 | B JDBC 持久化 | 无新缺陷 |
| BEQR-189 | 管理费方式 | N | 无新缺陷 |
| BSM-001 | 整体说明 | B | 无新缺陷 |
| BSM-003 | 系统通用能力 | B | 无新缺陷 |
| BSM-004 | 配置可存档可删除 | D | 无新缺陷 |
| BSM-005 | 分步引导 | N 前端 | 无新缺陷 |
| BSM-006 | 动态二维码 | N | 无新缺陷 |
| BSM-007 | 平台管理员日常权限 | B 电话明文不自动得 | 无新缺陷 |
| BSM-009 | 基础管理 | B | 无新缺陷 |
| BSM-028 | 主要功能 | D 项目三字段 | 无新缺陷 |
| BSM-029 | 和谁有关 | B | 无新缺陷 |
| BSM-072 | 商户管理 | D 平台 scope 拒商户号 | 无新缺陷 |
| BSM-074 | 和谁有关 | B | 无新缺陷 |
| BSM-114 | 各类入口 | B | 无新缺陷 |
| BSM-115 | 相互关系 | B | 无新缺陷 |
| BSM-120 | 已确认规则 | B 不把计划写成完成 | 无新缺陷 |
| GEN-001 | 必要性与最小性 | D | 无新缺陷 |
| GEN-002 | 可执行正确性 | D | 编号闸/生成器/predeploy 本步绿；不把旧 XML 当本步测试 |
| GEN-003 | 测试有效性 | D | 本步不新跑 Java；299 XML 哈希复用并标明历史 |
| GEN-004 | 信任边界 | D | 无新缺陷 |
| GEN-005 | 授权与最小权限 | D | 七入口同次 requirePermissions；USB 三常量 |
| GEN-006 | 数据完整性 | D | 无新缺陷；063 旧库不改 history |
| GEN-007 | 并发与幂等 | D | 写路径幂等；单次按来源一次 |
| GEN-008 | 接口与模式一致性 | D | 无新缺陷 |
| GEN-009 | 错误处理 | D | 消费失败不回明文 |
| GEN-010 | 安全与敏感信息 | D 电话+姓名加密 | 无新缺陷 |
| GEN-011 | 可观测与审计 | D merchant.viewed | 无新缺陷 |
| GEN-012 | 资源边界 | D LIMIT 500 | 无新缺陷 |
| GEN-013 | 构建配置 | N 本切片无构建文件 | 无新缺陷 |
| GEN-014 | 数据库迁移正确性 | D 发布面 | **本步编号闸 exit 0**；空库/升级库证据复用 B4 只读 |
| GEN-015 | 界面可用性 | B 中文错误 | 无新缺陷 |
| GEN-016 | 文档可验证性 | D 生成清单 | **本步两 `--check` exit 0**；规格 V432/172 |
| GEN-017 | 业务来源分类 | D | 无添加 |
| GEN-018 | 第一性原理证据 | D | 本报告 |
| PGM-001 | 整体定位 | B | 无新缺陷 |
| PGM-002 | 治理顺序 | B | 无新缺陷 |
| PGM-003 | 模块入口 | B | 无新缺陷 |
| PGM-004 | 综合体和集团 | D | 无新缺陷 |
| PGM-005 | 空间和点位 | D | 无新缺陷 |
| PGM-006 | 商户和主体 | D | 无新缺陷 |
| PGM-007 | 合同和准入 | N/B 拒改已有财税主体 | 无新缺陷 |
| PGM-008 | 员工和手机 | N | 无新缺陷 |
| PGM-009 | 权限和职级 | B | 无新缺陷 |
| PGM-010 | 终端开关 | N | 无新缺陷 |
| PGM-011 | 设备管理 | N 营业时间仅转发 | 无新缺陷 |
| PGM-012 | 商户侧 | B | 无新缺陷 |
| PGM-013 | 开放接口 | N | 无新缺陷 |
| PGM-014 | 留痕复核 | D | 无新缺陷 |
| PGM-015 | 楼宇运营 | N | 无新缺陷 |
| PGM-016 | 应急与隐私 | B→PGM-059 | 无新缺陷 |
| PGM-017 | 综合体和集团详 | D 对象带项目归属 | 无新缺陷 |
| PGM-018 | 物理项目与运营公司多对多 | D business_entity_projects | 落库文件现为 V432（原字节） |
| PGM-019 | 各主体负责 | D 三角色白名单 | 无新缺陷 |
| PGM-020 | 主要功能 | D ProjectRequest 三字段 | 无新缺陷 |
| PGM-021 | 业务意义 | B | 无新缺陷 |
| PGM-026 | 主要功能 | D 档案+四类主体 | 无新缺陷 |
| PGM-027 | 业务边界 | D requirePlatformScope | 无新缺陷 |
| PGM-031 | 外部经销商 | N | 无新缺陷 |
| PGM-035 | 提成发放 | N | 无新缺陷 |
| PGM-040 | 权限和职级父段 | B 含 656 | 无新缺陷 |
| PGM-041 | 权限规则 | D requirePermissions；L656 | 无新缺陷 |
| PGM-042 | 职级规则 | N | 无新缺陷 |
| PGM-043 | 外包与审计 | D 无导出口；L680 | 无新缺陷 |
| PGM-044 | 桌台挂单经销 | N | 无新缺陷 |
| PGM-048 | 商户侧全开放 | B | 无新缺陷 |
| PGM-059 | 个人信息保护 | D 掩码/明文/加密；L871 | 无新缺陷 |
| PGM-060 | 会员生物信息 | N 不扩姓名生日 | 无新缺陷 |
| PGM-065 | 连接关系 | B | 无新缺陷 |
| PGM-066 | 已拍板规则 | D 中文/留痕 | 无新缺陷 |

无锁外主张。

---

## 6. 第一性原理三问（2.3）

1. **为什么必须存在？** 删掉本 20 文件，PGM-017/020 项目档案与三字段、PGM-026 商户档案与四类主体、BSM-004 存档删除、PGM-041/059 权限与联系人电话例外都做不到。删掉现 V432（原 V427 字节），已批准的 `merchants.project_id` / 三字段 / 公司-项目多对多在**新库和已到 430 的库**落不下去。package-info 无业务发明。VenueBusinessHours DTO 在列，实现属他模块。
2. **声称行为是否被实际执行验证？** 20 文件行为：源码与 B5-r5/B1 同 SHA，本席抽核 helper/加密/留痕/占用 SQL。发布声称：本席本步 `verify:migration-version-order` exit 0；两生成器 `--check` exit 0；`npm run verify:predeploy` exit 0。三库模式/history：**复用本席 B4-r6 只读核实**（见 §11.4），本 B5 未再写库、未再 migrate。299 Java 测试：**哈希复用候选历史 XML**，不冒称本步新跑。
3. **未验证就当真的前提？** （a）未 fetch 远端，本地 `origin/main` 最大仍 V430、397 个迁移。（b）未跑 flyway CLI validate（避免写会话）；以 B4 history 行计数与 checksum 为只读等价。（c）V339/V394 种子是否直授 `pii.contact_phone.view` 属 XM，不扩。（d）旧姓名行无 `enc:` 前缀仍透传，不声称存量已全部加密。（e）html-ledger-sync 本步输出「改动了 1 份业务 HTML」比对基线是工具内 `origin/main@88c7b465`，**不是**当前 `origin/main` d8f12c33；当前 origin 三 HTML SHA 与工作区全等。闸本身 PASS。不把该工具措辞升格为「本 B3 改了 HTML」。

依据类型：HTML 原文（1）、本步命令输出（4）、Flyway/生成器脚本原文（3）、B4 只读 `flyway_schema_history`/信息架构（2+4，标明复用）。

---

## 7. 顺从式

对照冻结判据与三项现文：

- 项目隔离：`PlatformService.currentProject` + JDBC `project_id`；普通档案不能改归属。与 PGM-017/041、BEQR-152 一致。
- 四类主体：创建/激活拒绝未结束第二绑；未来绑不投影；生效绑普通停用拒绝。与 PGM-026/BEQR-143 一致。
- 退出：不能把非 terminated 直接写成 terminated。与 BEQR-155 一致。
- 存档删除：先存档、理由、最高权限、禁自审、占用检查、软删。
- 电话：无 `pii.contact_phone.view` 只核功能权并掩码；有权则 `requirePermissions(功能, 电话)` 同次核验、按来源消费一次；USB 主授权 `permissionGrant` 返回 null、不入 consume（L871）。平台管理员身份不在本 Controller 开后门。
- 留痕：`merchant.viewed` 含 merchantIds、入口、projectId、筛选、页码，不含姓名电话。
- 姓名：仅主次联系人列 PiiCipher；`owner_person` 明文（L627）；不扩法人/生日。
- 导出打印：本切片无端点，符合 L680。
- SpaceTypeRequest 注释与新建行为一致（057）。
- 发布：V432 号 > origin/main 最大 430；CSV 336 / 规格 172 与现目录一致。

未发现与现文冲突的实现。

---

## 8. 对抗式（grilling 自问，事实自读）

| 攻击 | 结果 | 证据 |
|---|---|---|
| 无电话权列表读明文 | 掩码 | helper 无权分支 + `withMaskedPhones` |
| 有权但消费失败仍出明文 | helper 在 service 前 `requirePermissions`；mark 失败 deny | Controller L281-288；AuthorizationService L56-60 |
| 同组功能+电话同一 grant 第二次 | 按来源 `source+sourceId` 一次 consume | requirePermissions LinkedHashMap |
| 只查询布尔再另一次消费 | `currentActorHasPermission` 走 permissionGrant **不** mark | AuthorizationService L266-276 |
| 平台管理员自动明文 | Controller 无 platform_admin 豁免；日常桥不含该高危码→掩码 | L871 + isPlatformAdminBridge |
| USB 主授权明文且不误伤单次 | 三常量合取；grant 返回 null | isMasterAuthority L139-143；issueMasterSession 签发 `master_authority` |
| 列表留痕反查姓名电话 | metadata 无姓名电话字段 | PlatformService L152-165 |
| 跨项目列表 | currentProject + JDBC | 同字节源码 |
| 导出打印绕开掩码 | 无导出口 | 20 文件检索 |
| 把生日/员工姓名拉进本模块 | JDBC 仅两列 encrypt | L625-632 vs owner_person L627 |
| 仅编译 299 PASS ⇒ 可删单次消费 | **不成立** | 接线仍在；派发禁止为删依赖改 Java |
| 只改名宣称 complex_test 已衔接 432 | **不成立** | B4：该库仍 427、无 432、history md5 不变 |
| 对已有列加 IF NOT EXISTS 兼容 | 禁止；SQL 无这些词 | V432 原文 |
| 编号仍 ≤430 | 本步闸绿；新增恰 V431+V432 | §11.1 |
| origin 仍把 USB 签成 platform_admin | 候选/正本已改三常量；属发布积压不是本 B3 新业务 | §10 |

无新绕过成立为当前工程缺陷。

---

## 9. 既有 30 项逐项重判

台账 V3.57 当前值队列全部否。本席不改台账。分类顺序：冲突→细化→技术性→添加。

| ID | 本席分类 | 工程缺陷 | 建议队列 | 冻结依据 | 当前事实与证据 |
|---|---|---|---|---|---|
| M009-038 | 冲突（历史） | 否 | 否 | PGM-041 / BEQR-152 / GEN-005 | 五类列表 currentProject；写入校验项目；普通不能认领。USB 识别三常量。V432 只是已批准落库的执行序号 |
| M009-039 | 冲突（历史） | 否 | 否 | PGM-017 / GEN-004 / GEN-006 | createSpace 要求当前项目；父空间同项目 |
| M009-040 | 冲突（历史） | 否 | 否 | PGM-026 / GEN-005 | 生效绑非 active 变更拒绝 |
| M009-041 | 冲突（历史） | 否 | 否 | PGM-026 / GEN-006 / GEN-008 | 未来绑 inactive 不投影。Leasing 新建三主体 null（相对 origin 的积压，已审） |
| M009-031 | 冲突（历史） | 否 | 否 | PGM-026 / BEQR-143 / GEN-006 | 与 040/041 同闸 |
| M009-042 | 冲突（历史） | 否 | 否 | BSM-004 / GEN-009 | unarchive 接受 archived 与 disabled |
| M009-043 | 冲突（历史） | 否 | 否 | BSM-004 / PGM-066 / GEN-011 | 审计对象 ID / rejected |
| M009-044 | 细化 | 否 | 否 | BSM-004 / GEN-006 / GEN-011 | 本次申请结清 |
| M009-045 | 细化 | 否 | 否 | BSM-004 / GEN-002 / GEN-006 | JDBC 排除已结束 inactive；未来未结束仍占。L221-224 |
| M009-046 | 细化 | 否 | 否 | PGM-020 / PGM-026 / GEN-004 / GEN-006 | 负值拒绝 |
| M009-030 | 细化 | 否 | 否 | PGM-026 / BEQR-143 / GEN-004 | 初建校验主体存在且在项目 |
| M009-047 | 冲突（历史） | 否 | 否 | PGM-001 / PGM-066 / GEN-008 / GEN-015 | 角色错误无英文枚举括注 |
| M009-018 | 冲突（历史） | 否 | 否 | BEQR-155 / GEN-006 | 拒直接 terminated |
| M009-013 | 细化 | 否 | 否 | PGM-020 / BSM-028 / GEN-002 | 三字段已在 ProjectRequest/JDBC/V432 字节；编号问题不回开本 ID |
| M009-054 | 冲突（历史） | 否 | 否 | BSM-004 / GEN-005 / GEN-008 | AccessService 不在 20 文件；不扩 XM |
| M009-055 | 细化 | 否 | 否 | GEN-003 / GEN-005 / GEN-008 / PGM-041 | 他模块夹具；不扩 |
| M009-056 | 细化 | 否 | 否 | GEN-003 / GEN-005 / GEN-008 | 审批受益人在 SupervisorReview；不扩本 20 文件 |
| M009-048 | 细化 | 否 | 否 | PGM-027 / BSM-072 | listMerchantsByBusinessEntity 仍在；零调用不是删除理由 |
| M009-049 | 细化 | 否 | 否 | BEQR-166 | JDBC 审批链仍在 |
| M009-050 | 细化 | 否 | 否 | PGM-041 / GEN-010 | 手机 PiiCipher+无权限掩码仍在；不覆盖 058/059 |
| M009-051 | 技术性 | 否 | 否 | GEN-012 | COUNT/LIMIT。无业务发明 |
| M009-052 | 细化 | 否 | 否 | BSM-004 | DELETE 拒绝、导向存档 |
| M009-053 | 技术性 | 否 | 否 | GEN-001 / GEN-017 | package-info 仅声明 |
| M009-057 | 细化 | 否 | 否 | BSM-004 / PGM-005 / PGM-020 / GEN-002 / GEN-008 / GEN-016 | SpaceTypeRequest L6 注释=新建不可重复固定启用 |
| M009-058 | 冲突（历史） | **否** | 否 | PGM-041 L656 / PGM-059 L871 / GEN-002 / GEN-005 / GEN-011 | 七入口 helper 同次核功能+可选电话、按来源一次；无电话权不碰电话权。留痕含对象上下文、不含姓名电话。符合 L871/L680 |
| M009-059 | 细化 | **否** | 否 | PGM-026 / GEN-010 / GEN-008 | JDBC 两列 PiiCipher；owner/法人未动。不声称存量全部已加密。V431 号合法但仍未随 origin 发布，不回开本 ID |
| M009-060 | 技术性 | **否** | 否 | GEN-014 / GEN-002 / GEN-013 | 本步 `verify:migration-version-order` **exit 0**：「本分支新增 2 个迁移,均排在 origin/main 最大号 V430 之后」。V427 已不在目录；V432 blob=原 V427。未改 428–431 |
| M009-061 | 技术性 | **否** | 否 | GEN-016 / GEN-008 | 本步 `generate-domain-ownership-scope-audit.mjs --check` **exit 0**，336 tables。CSV `file_version` 全 V0.02；status 全 covered。生成器仅版本常量+头 |
| M009-062 | 技术性 | **否** | 否 | GEN-016 | 本步 `generate-data-isolation-spec.mjs --check` **exit 0**。AUTO 最新 V432 / 172 张；手写条款未改（B4 已证 AUTO 后全文相同，本步 blob=after） |
| M009-063 | 技术性 | 否 | 否 | GEN-014 / GEN-006 | 仅登记：空库全迁、无 427 的 430 库升级已在独立库完成。原 `complex_test` 仍 427、无 432、history md5 `6258acd4a7f89f16d545dcf0048bbbc5`（B4 只读）。本 B5 未向该库应用 432 |

无添加项。无 HTML 修改申请。`questions/` 无 M009 文件。

---

## 10. 发布候选与相对 origin 的直接依赖

候选：`.scratch/publish-m009-m011-20260922`（worktree；HEAD=`d8f12c33` = 本地 `origin/main`；分支 `audit/20260922/m009-m011-publish`）。「已 FF 到 origin/main」指 **HEAD 与 origin/main 同提交**；生产 Java/迁移在 **工作区未提交层** 与正本逐字相同。

### 10.1 候选 ↔ 正本

| 项 | 本席实算 |
|---|---|
| 生产 Java `backend/src/main/java/**/*.java` | 主 1199 / 候选 1199 / 同字节 1199 / diff 0 |
| 本 20 文件 | 候选存在且与正本同字节 |
| 迁移 `V*.sql` | 399=399，文件名集合相同，字节差 0；无 V427、有 V431 与 V432 |
| 候选 surefire `TEST-*.xml` | **10 个 XML、tests=299、failures=0、errors=0、skipped=0**；mtime 2026-09-22 12:02（候选历史，**不是本席本步跑的测试**） |

299 构成（只读 XML）：AuthorizationServiceTests 25；M006SecurityRegressionTests 39；M010RegressionTests 44；AuthServiceTests 11；MasterAuthorityServiceTests 12；PlatformControllerTests 19；PlatformJdbcIntegrationTests 24；PlatformServiceTests 70；SupervisorReviewServiceTests 44；LeasingServiceTests 11。合计 299。切片编译/该批用例，**不是全后端**。本席核对应测试 `.java` 正本=候选同 SHA。Java 本轮无改动，**不冒称本步新跑**。

### 10.2 候选相对 origin/main 的已点名依赖

下列文件候选工作区 = 正本，**≠ origin/main blob**（发布积压，不是本次 B3 新业务）。本席只判断：**能否承载已审 M009/M011**；不宣称其它模块全审完。

| 文件 | origin→正本 | 是否能承载已审 M009/M011 |
|---|---|---|
| SessionIssuer | `issueMasterSession()` 无参；注释写 platform_admin wildcard | 现文三常量 `MASTER_USER_ID/USERNAME/ACCOUNT_TYPE=master_authority`；`issueMasterSession(deviceId)`。承载 038 USB 识别 |
| ProductionAuthService / AuthService | origin 签发 `account_type=platform_admin` | 现文 upsert 保留 id、强制 `master_authority`、`platform-company`、merchant NULL、随机不可用口令、审计 `auth.master.session_issued`。承载真实 master 账号与会话 |
| MasterAuthorityService | 挑战+Ed25519 后调 issuer | 现文验签通过后 `issueMasterSession("usb-master:"+source)`。承载 U 盘签发 |
| 两 Filter | origin 无 attachSession | 各 +1 行 `authService.attachSession(request, token)`，把 sessionId/deviceId 挂到请求。承载会话上下文，无新角色 |
| AuthorizationService | origin `isMasterAuthority` = platform_admin 桥 ∧ 保留 id/名 | 现文三常量合取，**普通 platform_admin 即使同名同 ID 也不算 USB**。`requirePermissions` 按来源一次 mark。承载 038/058 |
| JdbcAccessRepository | 临时授权 consume 以 grant_group_id 为 source_id | `markSingleUsePermissionConsumed`：模板 application.id 或 `temporary_permission_grants.grant_group_id`；同组多码一次 UPDATE。承载 058 单次/同组 |
| SupervisorReviewEvaluateRequest/Service | origin 无 beneficiaryId 字段 | 现文可选 `beneficiaryId` + 旧 10 参兼容构造；资金/授权动作 `BENEFICIARY_REQUIRED`；受益人不得自审。承载 056/M011 审批，无新流程发明 |
| LeasingService | origin 签约自动写三主体=同一公司 | 现文 `autoCreateMerchantForSign` 三主体 null + `intent.projectId()`。承载 041，不引入财税审批 |
| ProjectRecord / ProjectAssociationRequest | origin 无三字段；Association 文件 origin **不存在** | Record 增 ownerPerson/address/businessScope 及旧构造兼容；Association 为 038 公司/商户归属补录 DTO。承载 013/038 |
| PiiCipher / PiiTextRedactor | 与 origin **同字节** | 共享实现未改 |
| `tools/verify-pii-encryption.mjs` | V0.01→V0.02；登记 merchants 两姓名列 | 承载 059；本步 `PII_ENCRYPTION_PASS: 18 个 PII 列` |

**核验四点（源码，本步实读）：**

1. **真实 master 签发与会话**：`ProductionAuthService.issueMasterSession` L353-414 按保留 id upsert、`account_type=master_authority`、credential 随机 usb-only 哈希；`MasterAuthorityService.login` 一次性挑战 + Ed25519 后签发。Filter `attachSession` 绑定 session。origin 把 USB 签成 platform_admin 的旧实现**不能**承载已审 038；候选/正本可以。这是积压，不是本 B3 新功能。
2. **单次 grant_group 消费与平台七入口**：七入口均为 `requireMerchantPermission`：list GET `/merchants`、create POST `/merchants`、update PUT `/merchants/{id}`、assign PUT `/merchants/{id}/project`、archive、unarchive、delete-request。`delete-approve` 只 `merchant.profile.approve`、无电话明文。helper：布尔探权不消费；有权则 `requirePermissions(功能, pii.contact_phone.view)` 同次、按来源一次 mark；无权只 `requirePermission(功能)`。JdbcAccess 对 `grant_group_id` 原子 `consumed=FALSE`。匹配 058。
3. **受益人身份**：SupervisorReview 资金/授权/退款/免单要求 `beneficiaryId`；受益人不能复核。不在本 20 文件，属已审依赖。
4. **范围拒绝**：`requirePlatformScope` 商户账号 `PLATFORM_SCOPE_REQUIRED`；`requireProjectScope` 空组织 `PROJECT_SCOPE_REQUIRED`；跨组织/跨商户查询拒绝。Controller 列表在 helper 之后再 `requirePlatformScope`。

**未批新功能/缺陷：** 在上述点名依赖的 origin→正本 diff 中，本席未见超出已审 M009（013/038/041/058/059/056）与已审 M011 审批夹具的新产品能力。不为删这些依赖去改 Java。不把积压当成「其它模块已全审完」。

---

## 11. 本步实跑证据（本会话；旧测试不写成新跑）

临时落点：`.scratch/m009-b5-r6-grok/`。禁止 `/tmp`。未 source `/etc/complex/complex.env` 打印 secret；本 B5 未连库。

### 11.1 编号闸（本席本步）

```
npm run verify:migration-version-order
→ EXIT 0
MIGRATION_VERSION_ORDER_PASS: 本分支新增 2 个迁移,均排在 origin/main 最大号 V430 之后
```

本地 origin/main 迁移 397、最大 V430。工作区 399：V431 + V432。脚本**不在** `verify:predeploy` 链内（与 AGENTS §9 一致）。

### 11.2 两生成器 `--check`（本席本步）

```
node tools/generate-domain-ownership-scope-audit.mjs --check
→ EXIT 0
DOMAIN_OWNERSHIP_SCOPE_PASS: 336 tables classified

node tools/generate-data-isolation-spec.mjs --check
→ EXIT 0
data isolation spec is in sync with Flyway migration scope baseline.
```

未跑写模式 `docs:domain-ownership-scope` / `docs:data-isolation-spec`。

### 11.3 `npm run verify:predeploy`（本席本步完整链）

命令在正本 cwd 完整执行。日志 `.scratch/m009-b5-r6-grok/verify-predeploy.log`（7216 bytes）。**predeploy-exit=0**。

链内实见：html-ledger-sync PASS；entry-anchors PASS；html-param-tier PASS；DELIVERY_GATES_DOCS_PASS；full-admin-matrix 通过；DOMAIN_OWNERSHIP_SCOPE_PASS 336；matrix-vs-migration passed（349 tables parsed）；data isolation spec in sync；tenant-isolation PASS（棘轮；2 条已知缺口在台账，不硬失败）；p0-contract / client-openapi / payload PASS；security-baseline PASS；PRODUCTION_READINESS_EVIDENCE_PASS；PRODUCTION_BLOCKER_REGISTER_PASS；PII_ENCRYPTION_PASS: **18** 个 PII 列（登记表现 18 行，含 059 两姓名列）；TEST_DB_ISOLATION_PASS。

说明：候选目录 11:55 的 `99-predeploy-final.log` 亦 EXIT=0，但是**候选历史**；本席不以它代替本步命令。本步 PII 计数 18 与该旧日志「9 个」不同，以本步工作区登记表为准。

### 11.4 三库（复用本席 B4-r6 只读；本 B5 不写库）

B4-r6 本席已用 `/etc/complex/complex.env` 三 DB 项只读 SELECT。本 B5 按派发复用，不为重复验证写库。当时实算：

| 库 | 要点 |
|---|---|
| `complex_test_m009_pub_empty_20260922` | success 399、max **432**、无 427、checksum `216061286` |
| `complex_test_m009_pub_upgrade_20260922` | success 399、max **432**、无 427、>430 恰 431+432；旧项目剔三新列 md5 `1cd4ebc13a6720b8ab81224c434a06b5`，新列全 NULL；原商户剔 `project_id` md5 `6cfce3db5a2697d96a0675397628b861`，`project_id` NULL |
| `complex_test` | success 399、max **431**、有 427 无 432；history md5 **`6258acd4a7f89f16d545dcf0048bbbc5`**；不是本批升级目标 |

三库 bep PK + 2FK + `idx_business_entity_projects_project_id` + `merchants_project_id_fkey` / `idx_merchants_project_id` 同形。V432 与历史 427 checksum 相同，印证同字节 SQL。

### 11.5 Java 测试（哈希复用，非本步新跑）

候选 10 类 XML 合计 299 / 0 / 0 / 0（§10.1）。正本 `PlatformControllerTests`/`PlatformServiceTests`/`PlatformJdbcIntegrationTests` 等与候选同 SHA。B5-r5 曾于 07:05 跑过 19+70+4 单测绿——**那是 r5，不是本步**。本 B5 不把任何 surefire 写成「本席本步 mvn test」。

---

## 12. 观察（不入队、不扩 XM、不覆盖历史编号）

并列保留：

- OBS01 空间类型存档/删审未走 executeWrite 幂等键；顺序路径可失败重放。
- OBS02 无项目 scope 的 list*Paged 四参重载仍在 JDBC；生产 list 走带 projectId 五参。
- OBS03 V259 等权限种子跨模块；V339/V394 对 `pii.contact_phone.view` 的角色直授属 XM/权限模块。
- OBS04 `requireDate`/`readObjectMap` 本 20 文件 0 命中。
- 营业时段 Controller 委托 `VenueBusinessHoursService`（实现不在 20 文件）。
- owner/legal 姓名不在本批准加密范围；生日/人脸不扩。
- 空间类型 `saveSpaceType` ON CONFLICT 并发双新建可能变成更新。顺序已拒。不立项。
- XM-008/010/011/012 各归既有输入，本模块不扩大。
- P-OBS：V431 号已 >430 仍未随 origin/main 发布，与 V432 同属积压，不另开业务 ID。
- P-OBS：`complex` 生产库 max 历史 385 不是本批发布验证面（B1 只读事实，本 B5 未再连）。
- P-OBS：html-ledger-sync 工具基线 `88c7b465` 与当前 origin/main `d8f12c33` 不是同一提交；当前 origin 三 HTML SHA=工作区。闸 PASS，不升格缺陷。
- 测试库卫生/旧观察不扩本模块。

ponytail-review：20 文件无可删业务；048 隔离通道保留。`net: 0` 可删生产行。Lean already for 20 Java. 发布面机械清单不是「可删复杂度」。不为删 origin 依赖而改 Java。

---

## 13. 新 finding

无。不启用锁外判据。不把观察写成添加。

四分类：本轮无新冲突/细化缺陷/技术性缺陷/添加。

---

## 14. 本席步骤结论

范围 20/20 文件、3381 行（哈希复用全文阅读，重新判断）、88/88 判据。三线完成。30 项重判当前均无工程缺陷。060/061/062 由本步编号闸与两 `--check` 及 after 树 blob 支持为已修。063 边界仍成立。三项现文未破。无未答 questions。无锁外主张。发布候选 1199 Java / 399 迁移与正本同字节；本席 `verify:predeploy` exit 0。点名授权依赖能承载已审 M009/M011，相对 origin 为积压而非本 B3 新业务；未见未批新功能。未向 `complex_test` 再应用 432。未改 index。

**B5-r6 本席 PASS。**

不存在有条件通过。本票不是双席合取、不是 ledger 模块 PASS、不是收口文件。合取与 ledger PASS 由调度在两席完整 raw 落齐且四值校验后执行；本席不写收口文件、不改 ledger。

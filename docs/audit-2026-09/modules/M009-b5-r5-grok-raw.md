CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# M009 B5-r5 Grok 整模块三线审计 V0.01

席位：Grok（乙）。步骤：B5-r5。B3 累计 5（本席不改 ledger）。输入：`plan/modules.md` V0.04 M009 的 20 个生产文件；现锁 `plan/criteria-lock.md` V0.14 映射 88 条。不读本次 self B5。不继承 B4 或历史 PASS。不改生产代码、测试、三 HTML、锁、台账、主 `.git`。不 fetch/提交/推送/部署/安装/MCP/子代理。本批生产仅 Controller 变，无数据库修改。不把旧 surefire XML 当本次测试。不宣称全链绿。V427 顺序等既有环境红保留。XM 各模块不扩。

三项批准按现文执行，不引用旧禁令，不擅加新功能/姓名生日/旧数据回填：PGM871 授权商户联系人页面列表/操作结果可明文；PGM680 不扩导出打印；PGM656 单次消费原句保持。既有 scope/幂等时序按已批准 B2 保持。

本席结论：**PASS**。新 finding 临时编号：无。既有 26 项本次全部重判为当前工程缺陷否、不建议改队列。

---

## 0. 技能预检（§6，本宿主实核路径）

已读并用其收窄口径（不派子代理、不等操作员、B5 期间不修）：

| 技能 | 路径 | 本步用法 |
|---|---|---|
| using-superpowers | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工自检 |
| grilling / grill-me | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` 及同目录 `grill-me` | 对抗/第一性追问结构，事实自读 |
| diagnosing-bugs | `.../mattpocock-skills/1.2.3/skills/engineering/diagnosing-bugs/SKILL.md` | 058 消费/留痕、059 加密反馈环 |
| verification-before-completion | `.../superpowers/6.3.0/skills/verification-before-completion/SKILL.md` | PASS 前必须有本轮命令输出 |
| codebase-design | `.../mattpocock-skills/1.2.3/skills/engineering/codebase-design/SKILL.md` | Controller helper 作为脱敏 seam |
| ponytail-review | `/home/james/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只找可删，未把观察写成新功能 |
| systematic-debugging | `.../superpowers/6.3.0/skills/systematic-debugging/SKILL.md` | 只借读错→复现；B5 不改码 |
| writing-for-agents | `.../mattpocock-skills/1.2.3/skills/productivity/writing-for-agents/SKILL.md` | raw 结构 |
| long-running-background-tasks | `/home/james/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 单测一次跑完再采信 |

缺口（不冒充已调用）：本宿主未核到独立 `i-have-adhd` SKILL.md（按提示词输出口径执行）；可选 `batch-grill-me` / `ubiquitous-language` / `git-guardrails-claude-code` 未在本步目录核到。`tdd` 存在但本步不落测试。划掉的子代理/worktree/research 技能不用。

---

## 1. 范围、行数、哈希

命令：

```bash
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
git ls-files -z -- <M009 20 paths> | xargs -0 cat | wc -l
```

本席实算：

- `CRITERIA-LOCK-SHA256` = `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`
- sidecar 同行；ledger 顶部同行。本席不把四值闸写成已代 Codex 收口。
- 有效内容行数 **3381**（含空行注释，`cat | wc -l`）。与 `modules.md` 所记 2476 不一致，以本席实跑 3381 为准。

20 文件（`plan/modules.md` FILES 闭集，未增删）：

| 行数 | 路径 |
|---:|---|
| 17 | `backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java` |
| 19 | `.../BusinessEntityBindingRecord.java` |
| 116 | `.../MerchantRecord.java` |
| 9 | `.../MerchantDeleteDecision.java` |
| 1338 | `.../JdbcPlatformRepository.java` |
| 302 | `.../PlatformController.java` |
| 204 | `.../PlatformRepository.java` |
| 1179 | `.../PlatformService.java` |
| 1 | `.../package-info.java` |
| 17 | `.../dto/BusinessEntityBindingRequest.java` |
| 26 | `.../dto/BusinessEntityRequest.java` |
| 6 | `.../dto/MerchantArchiveRequest.java` |
| 5 | `.../dto/MerchantDeleteApprovalRequest.java` |
| 5 | `.../dto/MerchantDeleteRequest.java` |
| 41 | `.../dto/MerchantProfileRequest.java` |
| 26 | `.../dto/ProjectRequest.java` |
| 21 | `.../dto/SpaceRequest.java` |
| 16 | `.../dto/SpaceTypeRequest.java` |
| 11 | `.../dto/StatusChangeRequest.java` |
| 22 | `.../dto/VenueBusinessHoursRequest.java` |
| **3381** | 合计 |

20 个文件均全文通读。测试类、迁移、前端、Access 不在本 20 文件，只作只读追证，不扩 XM。

---

## 2. 三项批准 — 现文逐字（不引旧禁令）

出处均为 `docs/platform-governance-map-for-operations-v0.1.html` 当前字节。锁映射：L656 ∈ PGM-041；L680 ∈ PGM-043；L871 ∈ PGM-059。

**PGM656（单次消费原句保持，L656）**

> 临时授权可设有效期或单次使用；权限的授予、收回、到期、单次消耗、失效全部留痕。

**PGM871（授权商户联系人列表/操作结果可明文，L871）**

> 员工日常界面只显示脱敏号码，不见明文；界面脱敏不提供关闭开关。商户档案中的第一联系人和第二联系人手机号除外：经单独授权且查看权限有效的人员，以及持授权 U 盘密钥的最高权限本人，可以在其获准访问的商户档案列表和档案操作结果中查看完整号码；其他人员仍只显示脱敏号码。平台管理员身份本身不自动取得该查看权限。该例外仅适用于商户档案联系人，不扩大到客人或员工档案的手机号，也不免除项目范围检查和查看留痕。

**PGM680（不扩导出打印，L680 该句后段）**

> 商户档案页面中的第一联系人和第二联系人手机号，按“客人与员工的个人信息保护”条中明确的查看权限例外执行；该页面查看例外不授予未脱敏导出或打印权限，也不免除敏感页的查看留痕及其他页面保护要求。

本 20 文件无导出/打印入口，未发明导出口径。

---

## 3. 88 判据全部定义 + 三 HTML 适用段核对

映射原文（`criteria-lock.md` 模块映射区 M009 一行）：88 个 ID 均能在定义区找到 `### <ID> —` 标题。0 缺失。

HTML 锚点核对方法（类型 4）：按锁「来源」路径+行号打开三份 HTML，BEQR 取该 `td.k` 所在 `<tr>` 全文，BSM/PGM 取该 `h2/h3` 至下一同级或更高级标题的正文。结果：非 GEN 70 条标题均出现在对应段，**MISMATCH COUNT 0**。BEQR `td.k` 全局 197，与锁声明一致。

GEN-001…018 来源「通用工程判据」，无 HTML 行号，定义区全文已读。

### 3.1 定义与适用段矩阵

适用：D=本 20 文件有直接实现；B=边界适用（不得与原文冲突，本切片无专面）；N=本 20 文件无实现面。

| ID | 锁标题 | 来源锚点 | 适用段核对（标题命中/段长） | 本模块 |
|---|---|---|---|---|
| BEQR-063 | 高危动作（需审批/双证据） | enum.html:111 td.k | 命中/183 | N 酒吧高危动作词表 |
| BEQR-074 | 隐私桌台权限 | :125 | 命中/68 | N |
| BEQR-143 | 四类主体 | :226 | 命中/45；经营对象/运营/合同/结算唯一绑定 | D 绑定+投影 |
| BEQR-144 | 经营主体关系 | :227 | 命中/96；项目与运营公司多对多 | D 公司-项目关联 |
| BEQR-145 | 城市项目公司 | :228 | 命中/182 | D 角色 project_company |
| BEQR-146 | 运营公司职责 | :229 | 命中/136 | B 角色 management_company |
| BEQR-147 | 主要收入归属 | :230 | 命中/496 | N 收入名目不在本切片 |
| BEQR-148 | 合作模式 | :231 | 命中/101；六类+其他须指定模板 | D cooperationForm 六值；合同模板 N |
| BEQR-149 | 职级 | :232 | 命中/30 | N |
| BEQR-150 | 终端十一端 | :233 | 命中/144 | N |
| BEQR-151 | 最终权限计算 | :234 | 命中/49 | B 消费走 AuthorizationService |
| BEQR-152 | 项目与敏感数据权限 | :235 | 命中/173；默认项目隔离 | D currentProject/列表谓词 |
| BEQR-153 | 终端开关优先级 | :236 | 命中/77 | N |
| BEQR-154 | 综合体项目状态 | :237 | 命中/27；六态 | D PROJECT_STATUSES |
| BEQR-155 | 商户/运营主体状态 | :238 | 命中/169；退出缓冲 | D 拒直接 terminated；归档走存档 |
| BEQR-156 | 合同状态 | :239 | 命中/84 | N |
| BEQR-157 | 员工状态 | :240 | 命中/20 | N |
| BEQR-158 | 设备状态 | :241 | 命中/28 | N |
| BEQR-159 | 权限状态 | :242 | 命中/21 | N |
| BEQR-166 | 合同审批链 | :255 | 命中/268 | B JDBC 实现 SupervisorReview 持久化 |
| BEQR-189 | 管理费方式 | :291 | 命中/309 | N |
| BSM-001 | 整体说明：这套系统到底管什么 | system-map.html:505 h2 | 命中/1188 | B |
| BSM-003 | 系统通用能力 | :563 h2 | 命中/2482 | B 父段 |
| BSM-004 | 配置数据可以存档、也可以删除 | :566 h3 | 命中/1263；先存档再删、最高权限、禁自审、查占用 | D 商户/空间类型 |
| BSM-005 | 后台的"新建/录入"改成分步引导 | :581 | 命中/282 | N 前端 |
| BSM-006 | 验证用的二维码一律动态刷新 | :584 | 命中/362 | N |
| BSM-007 | 平台管理员拥有全部日常功能权限 | :587 | 命中/516；高敏不自动得 | B 电话明文不因平台管理员 |
| BSM-009 | 基础管理 | :596 | 命中/49 | B 入口 |
| BSM-028 | 主要功能 | :698 | 命中/1557；项目档案负责人/地址/经营范围 | D ProjectRequest 三字段 |
| BSM-029 | 和谁有关 | :714 | 命中/142 | B |
| BSM-072 | 商户管理：商户在授权范围内完整使用自己的系统 | :1308 h2 | 命中/896 | D 平台 scope 拒商户账号 |
| BSM-074 | 和谁有关 | :1326 | 命中/142 | B |
| BSM-114 | 各类入口 | :1827 h2 | 命中/928 | B |
| BSM-115 | 相互关系 | :1897 h2 | 命中/1553 | B |
| BSM-120 | 已确认规则和实施前输入项 | :1957 h2 | 命中/2377 | B |
| GEN-001 | 必要性与最小性 | 通用 | 定义区全文 | D |
| GEN-002 | 可执行正确性 | 通用 | 定义区全文 | D |
| GEN-003 | 测试有效性 | 通用 | 定义区全文 | D 本轮单测；不把通过当唯一依据 |
| GEN-004 | 信任边界校验 | 通用 | 定义区全文 | D |
| GEN-005 | 授权与最小权限 | 通用 | 定义区全文 | D |
| GEN-006 | 数据完整性 | 通用 | 定义区全文 | D |
| GEN-007 | 并发与幂等 | 通用 | 定义区全文 | D 写入口幂等；单次消费时序保持 B2 |
| GEN-008 | 接口与模式一致性 | 通用 | 定义区全文 | D |
| GEN-009 | 错误处理与恢复 | 通用 | 定义区全文 | D |
| GEN-010 | 安全与敏感信息 | 通用 | 定义区全文 | D 电话掩码+姓名加密 |
| GEN-011 | 可观测与审计证据 | 通用 | 定义区全文 | D merchant.viewed 对象上下文 |
| GEN-012 | 资源与性能边界 | 通用 | 定义区全文 | D COUNT/LIMIT 500 |
| GEN-013 | 构建配置与依赖 | 通用 | 定义区全文 | N 本切片无构建文件 |
| GEN-014 | 数据库迁移正确性 | 通用 | 定义区全文 | N 迁移属 M010；本批无库改 |
| GEN-015 | 界面可用性与可访问性 | 通用 | 定义区全文 | B 中文错误文案 |
| GEN-016 | 文档可验证性 | 通用 | 定义区全文 | D SpaceTypeRequest 注释已与行为一致 |
| GEN-017 | 业务来源分类 | 通用 | 定义区全文 | D 本报告四分类 |
| GEN-018 | 第一性原理证据 | 通用 | 定义区全文 | D |
| PGM-001 | 整体定位 | gov.html:256 h2 | 命中/1752 | B |
| PGM-002 | 治理顺序 | :308 h2 | 命中/180 | B |
| PGM-003 | 模块入口 | :339 h2 | 命中/661 | B |
| PGM-004 | 综合体和集团 | :342 h3 | 命中/41 | D |
| PGM-005 | 空间和点位 | :347 | 命中/41 | D 空间+类型字典 |
| PGM-006 | 商户和主体 | :352 | 命中/38 | D |
| PGM-007 | 合同和准入 | :357 | 命中/36 | N |
| PGM-008 | 员工和手机 | :362 | 命中/43 | N |
| PGM-009 | 权限和职级 | :367 | 命中/36 | B 消费 |
| PGM-010 | 终端开关 | :372 | 命中/109 | N |
| PGM-011 | 设备管理 | :378 | 命中/46 | N |
| PGM-012 | 商户侧 | :383 | 命中/47 | B |
| PGM-013 | 开放接口 | :388 | 命中/50 | N |
| PGM-014 | 留痕复核 | :393 | 命中/47 | D 审计事件 |
| PGM-015 | 楼宇运营 | :399 | 命中/55 | N |
| PGM-016 | 应急与隐私 | :404 | 命中/41 | B 指向 PGM-059 |
| PGM-017 | 综合体和集团：当前一个项目，未来能看集团 | :412 h2 | 命中/2661；对象带项目归属 | D |
| PGM-018 | 物理项目与运营公司：多对多 | :415 | 命中/511 | D business_entity_projects |
| PGM-019 | 各主体具体负责什么 | :424 | 命中/1653 | D 三角色白名单 |
| PGM-020 | 主要功能 | :440 | 命中/358；项目档案状态/负责人/地址/经营范围 | D |
| PGM-021 | 业务意义 | :452 | 命中/67 | B |
| PGM-026 | 主要功能 | :492 | 命中/1607；档案+四类主体+财税不可普通改 | D |
| PGM-027 | 业务边界 | :507 | 命中/201；商户不能看平台全局和其他商户 | D requirePlatformScope |
| PGM-031 | 外部经销商主体、合同价格与资金治理 | :555 | 命中/699 | N |
| PGM-035 | 提成发放和人员服务治理 | :599 | 命中/166 | N |
| PGM-040 | 权限和职级：每一个单一功能都可以单独授权 | :643 h2 | 命中/4073 | B 父段含 656 |
| PGM-041 | 权限规则 | :647 | 命中/1485；含 L656 单次消耗原句 | D 同次 requirePermissions |
| PGM-042 | 职级规则 | :664 | 命中/82 | N |
| PGM-043 | 外包人员与审计人员 | :673 | 命中/1731；含 L680 查看例外不授导出打印 | D 无导出口；留痕在列表 |
| PGM-044 | 桌台、挂单与经销业务权限 | :688 | 命中/684 | N |
| PGM-048 | 商户侧：在授权业务范围内全开放 | :749 h2 | 命中/399 | B |
| PGM-059 | 客人与员工的个人信息保护 | :868 | 命中/518；含 L871 明文例外 | D 掩码/明文/加密 |
| PGM-060 | 会员身份、营销与生物信息治理 | :879 | 命中/471 | N 不扩姓名生日 |
| PGM-065 | 连接关系 | :935 h2 | 命中/399 | B |
| PGM-066 | 已拍板规则和待排期问题 | :951 h2 | 命中/1458；过程动作留痕、中文 | D |

无锁外主张。无 LOCK-AMEND 提案。

---

## 4. 二十文件覆盖记录（无 finding ≠ 未审）

| 文件 | 审过 | 要存在的理由 | 本轮要点 |
|---|---|---|---|
| package-info.java | 是 | 编译单元 | 仅 package 声明 |
| BusinessEntityRecord.java | 是 | 四类主体档案 | 无联系人电话 |
| BusinessEntityBindingRecord.java | 是 | 唯一绑定时段 | effectiveFrom/To/status |
| MerchantRecord.java | 是 | 商户档案+投影+脱敏副本 | withMaskedPhones；projectId；存档删除字段 |
| MerchantDeleteDecision.java | 是 | 删除审批结果 | 无电话字段 |
| PlatformRepository.java | 是 | 仓储 seam | 项目分页默认；listMerchantsByBusinessEntity 隔离通道 |
| JdbcPlatformRepository.java | 是 | 真源持久化 | 姓名/电话 encrypt 写 decrypt 读；owner_person 不加密；项目谓词；占用统计排除已结束绑定 |
| PlatformController.java V0.03 | 是 | HTTP 授权+脱敏 | 七个商户响应入口共享 requireMerchantPermission + mask；approve 删除走功能权无电话 |
| PlatformService.java V0.12 | 是 | 规则与留痕 | 项目隔离、绑定财税闸、退出缓冲、merchant.viewed 对象上下文 |
| BusinessEntityBindingRequest | 是 | 绑定写入 | 无 status 直写 |
| BusinessEntityRequest V0.03 | 是 | 主体创建 | 可选 projectId |
| MerchantArchiveRequest | 是 | 存档理由 | idempotency+reason |
| MerchantDeleteApprovalRequest | 是 | 审批 | decision/comment |
| MerchantDeleteRequest | 是 | 删除申请 | reason |
| MerchantProfileRequest V0.02 | 是 | 档案 | 联系人姓名电话+projectId |
| ProjectRequest V0.01 | 是 | 项目档案 | ownerPerson/address/businessScope 可空 |
| SpaceRequest | 是 | 空间 | projectId 必填于服务层 |
| SpaceTypeRequest V0.01 | 是 | 新建字典 | 注释：新建、编码不可重复、新建启用 |
| StatusChangeRequest | 是 | 绑定变更 | action/reason |
| VenueBusinessHoursRequest | 是 | 营业时段 DTO | Controller 委托 VenueBusinessHoursService（不在 20 文件） |

---

## 5. 第一性原理三问（2.3）

1. **为什么必须存在？** 删掉本切片，PGM-017/020 项目档案与归属、PGM-026 商户档案与四类主体、BSM-004 存档删除、PGM-041/059 权限与联系人电话例外都做不到。VenueBusinessHours 端点是场次资格配置入口，DTO 在列；服务实现属他模块。package-info 无业务发明。
2. **声称行为是否执行验证？** 是。本轮命令见 §8。058 消费/留痕、059 加密均有可失败断言或隔离探针，不只读注释。
3. **未验证就当真的前提？** （a）未跑写库集成 `PlatformJdbcIntegrationTests`（指令禁止）；059 落库由源码接线 + 只读 `information_schema` TEXT + `/tmp` 密码学探针补证，不声称本席重跑了 JDBC 往返用例。（b）V339 角色是否仍直授 `pii.contact_phone.view` 属权限种子/XM，不在 20 文件，保持观察不扩。（c）complex_test 含未发布 V427，本席只读列类型，不宣称 Flyway 全链绿。

依据类型：HTML 原文（1）、本轮单测与探针输出（2+4）、AES-GCM 实现对照 PiiCipher 注释/代码（3 算法行为以代码+单测为准）。

---

## 6. 顺从式（假定实现意图正确）

对照冻结判据与三项现文：

- 项目隔离：`PlatformService.currentProject` + JDBC `project_id = :project_id`；普通档案不能改归属；跨项目空间/主体拒绝。与 PGM-017/041、BEQR-152 一致。
- 四类主体：创建/激活拒绝未结束第二绑；生效绑不同步未来；停用生效绑拒绝普通 change；投影仅激活时写。与 PGM-026/BEQR-143 一致。
- 退出：建档/更新不能把非 terminated 直接写成 terminated；已 terminated 省略状态不擅自改活。与 BEQR-155 缓冲一致；无新调度。
- 存档删除：先存档、理由、最高权限、禁自审、占用检查、软删。空间类型 disabled/archived 可恢复，pending_delete 不能用存档绕过。
- 电话：无 `pii.contact_phone.view` 只核功能权并掩码；有权则 `requirePermissions(功能, 电话)` 同次核验、按来源消费一次；USB 主授权在授权器内不消费、可明文（L871）。平台管理员身份不在本 Controller 开后门。
- 留痕：`merchant.viewed` 含 merchantIds、入口、projectId、筛选、页码，不含姓名电话。写路径走 executeWrite 对象 ID。
- 姓名：仅主次联系人列 PiiCipher；负责人/法人不扩；旧明文无前缀解密透传，不声称存量已全部加密。
- 导出打印：本切片无端点，未授未脱敏导出。
- 中文错误：角色白名单消息无英文枚举括注（本轮单测）。
- SpaceTypeRequest 注释与新建行为一致（057）。

未发现与现文冲突的实现。

---

## 7. 对抗式（假定实现错误；grilling 自问）

构造并核对：

| 攻击 | 结果 | 证据 |
|---|---|---|
| 无电话权列表读明文 | 掩码 | ControllerTests listing 页形状断言 188**** |
| 有权但消费失败仍出明文 | 403，不进 service | failedPhonePermissionConsumptionDoesNotReturnPlaintext 本轮绿 |
| 单次电话权列表两条只耗一次 | 一次 markConsumed，两条皆明文 | parameterized [读,同来源] |
| 同组功能+电话同一 grant 第二次 | 第二次 PERMISSION_DENIED | 同测试 sameGrant=true |
| 功能 ROLE + 电话单次，第二次 | 第二次掩码且仍可读列表 | sameGrant=false |
| 写回 archive 同样消费 | parameterized writeResponse=true | 本轮绿 |
| 只查询布尔再另一次消费 | 已删除；helper 在明文前 requirePermissions | 源码 L281-288 |
| 平台管理员自动明文 | 本 Controller 无 platform_admin 豁免；授权器日常桥不含高危码时 false→掩码 | 源码+PGM871 |
| USB 主授权明文且不误伤单次 | permissionGrant 对 master 返回 null，不入 consume | AuthorizationService（只读追证，不扩 XM） |
| 列表留痕能反查姓名电话 | metadata 断言不含 Jane/18800000011 | merchantViewAudit… 本轮绿 |
| 跨项目列表 | SQL/内存均 project_id 过滤；未知项目空表 | 源码+projectLists… |
| 普通改结算主体 | BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW | activeBindingCannotBeDisabled… |
| 未来绑立即投影 | inactive 且三列仍空 | futureBindingDoesNotChange… |
| 负数面积租金 | 400 | negativeCapacity… |
| 直接 terminated | conflict | ordinaryMerchantProfileCannotCauseImmediateExit |
| 空间父级他项目 | 拒绝 | employeeCannotReadOtherProject… |
| 导出打印绕开掩码 | 无导出口 | 20 文件 grep 无 export/print |
| 姓名列仍 VARCHAR(120) 装不下密文 | 只读库列为 text | §8 psql |
| 加密 120 字中文 | 探针 roundtrip、密文无「张」 | /tmp NameCipherProbe |
| 把生日/员工姓名拉进本模块 | 未改 owner_person/legal_representative/PiiCipher 共享实现 | 源码 merchantParameters |
| 幂等重放先消费（B2 已批准时序） | consume 在 service 前；保持不改 | 指令保持 B2 |
| 空间类型并发 upsert 改已有行 | 顺序路径已拒重复编码；并发需竞态。不单独立项 | OBS |
| approveDeleteSpaceType 无 Controller 功能权 | 服务层 master+禁自审 | 源码；非新缺陷 |

无新绕过成立为当前工程缺陷。

---

## 8. 实跑证据（本会话，非旧 XML）

禁止写库集成。本席未跑 `PlatformJdbcIntegrationTests`。

**A. 纯单测（exit 0）**

```
cd /home/james/complex-management/backend
mvn -Dtest=com.complex.platform.governance.PlatformControllerTests,com.complex.platform.governance.PlatformServiceTests,com.complex.platform.common.crypto.PiiCipherTests -DfailIfNoTests=false test
```

本轮 surefire mtime 2026-09-22 07:05:27–07:05:30，副本 `/tmp/m009-b5r5-probe/TEST-*.xml`：

| 类 | tests | failures | errors | skipped |
|---|---:|---:|---:|---:|
| PlatformControllerTests | 19 | 0 | 0 | 0 |
| PlatformServiceTests | 70 | 0 | 0 | 0 |
| PiiCipherTests | 4 | 0 | 0 | 0 |
| 合计 | 93 | 0 | 0 | 0 |

058 相关本轮绿：`merchantPhoneSingleUseAppliesToOneResponse` ×4、`failedPhonePermissionConsumptionDoesNotReturnPlaintext`、`listingMerchantsReturnsPlaintextPhoneWithPiiPermission`、`merchantViewAuditRecordsOnlyReturnedPageAndQueryContext`。

**B. /tmp 隔离探针（先验证目录）**

```
mkdir -p /tmp/m009-b5r5-probe && test -d /tmp/m009-b5r5-probe  → TMP_OK
javac/java NameCipherProbe against backend/target/classes
```

输出：`len_plain=120 enc_prefix=enc:v1: enc_len=527 contains_plain=false roundtrip=true legacy_plain=true null_enc=true empty_enc_empty=true`

**C. 只读库结构（非写库测试）**

未 source env。`psql -h 127.0.0.1 -U complex -d complex_test` 查 `information_schema.columns`：

- `contact_name text`
- `secondary_contact_name text`
- `contact_phone character varying`（电话加密列宽不在本批准扩列范围）
- `owner_person character varying`（未扩）

V431 迁移文件存在（M010，只读确认 059 列类型来源），本席不审 M010。

---

## 9. 既有 26 项逐项重判

台账当前值（findings V3.49）队列全部否。本席不改台账，只给本席当前判定。分类顺序：冲突→细化→技术性→添加。

| ID | 本席分类 | 工程缺陷 | 建议队列 | 冻结依据 | 当前事实与证据 |
|---|---|---|---|---|---|
| M009-038 | 冲突（历史） | 否 | 否 | PGM-041 / BEQR-152 / GEN-005 | 五类列表走 currentProject；写入校验项目；普通不能认领。测试 projectLists… / ordinaryProfileUpdateCannotClaimOrMoveProject / masterHasNoGlobalList… 本轮绿 |
| M009-039 | 冲突（历史） | 否 | 否 | PGM-017 / GEN-004 / GEN-006 | createSpace 要求 projectId=当前；父空间同项目。spaceRequiresCurrentProject… / employeeCannotReadOtherProject… 绿 |
| M009-040 | 冲突（历史） | 否 | 否 | PGM-026 / GEN-005 | 生效绑非 active 变更拒绝。activeBindingCannotBeDisabled… 绿 |
| M009-041 | 冲突（历史） | 否 | 否 | PGM-026 / GEN-006 / GEN-008 | 未来绑 inactive 不投影；到期须再激活。futureBindingDoesNotChange… / dueBindingCanActivate… 绿 |
| M009-031 | 冲突（历史） | 否 | 否 | PGM-026 / BEQR-143 / GEN-006 | 与 040/041 同闸；历史结束不可重开。dueBinding… 绿 |
| M009-042 | 冲突（历史） | 否 | 否 | BSM-004 / GEN-009 | unarchive 接受 archived 与 disabled。deletedSpaceTypeCanBeRestored… 绿 |
| M009-043 | 冲突（历史） | 否 | 否 | BSM-004 / PGM-066 / GEN-011 | 审计带对象 ID 与 rejected。spaceTypeAuditHasObjectId… / merchantDeleteAuditKeepsReason… 绿 |
| M009-044 | 细化 | 否 | 否 | BSM-004 / GEN-006 / GEN-011 | 本次申请结清。spaceTypeDeletionCompletesOnlyTheCurrentRequest 绿 |
| M009-045 | 细化 | 否 | 否 | BSM-004 / GEN-002 / GEN-006 | JDBC 排除已结束 inactive 绑定；未来未结束仍占。源码 countMerchantReferences L221-224 |
| M009-046 | 细化 | 否 | 否 | PGM-020 / PGM-026 / GEN-004 / GEN-006 | 负值拒绝，零/空允许。negativeCapacity… 绿 |
| M009-030 | 细化 | 否 | 否 | PGM-026 / BEQR-143 / GEN-004 | 初建校验主体存在且在项目。initialMerchantRejectsUnknownOrForeign… 绿 |
| M009-047 | 冲突（历史） | 否 | 否 | PGM-001 / PGM-066 / GEN-008 / GEN-015 | 角色错误无英文枚举。userFacingRoleValidation… 绿 |
| M009-018 | 冲突（历史） | 否 | 否 | BEQR-155 / GEN-006 | 不能直接 terminated。ordinaryMerchantProfileCannotCauseImmediateExit 绿 |
| M009-013 | 细化 | 否 | 否 | PGM-020 / BSM-028 / GEN-002 | 三字段可空读写。projectCreationReturnsApprovedOptionalProfileFields 绿 |
| M009-054 | 冲突（历史） | 否 | 否 | BSM-004 / GEN-005 / GEN-008 | 缺陷对象 AccessService 不在本 20 文件；本切片无岗位模板生命周期。不扩 XM。保持否 |
| M009-055 | 细化 | 否 | 否 | GEN-003 / GEN-005 / GEN-008 / PGM-041 | 他模块测试夹具。不扩。保持否 |
| M009-056 | 细化 | 否 | 否 | GEN-003 / GEN-005 / GEN-008 | 同上。保持否 |
| M009-048 | 细化 | 否 | 否 | PGM-027 / BSM-072 | listMerchantsByBusinessEntity 隔离通道仍在；零调用不是删除理由 |
| M009-049 | 细化 | 否 | 否 | BEQR-166 | JDBC 审批链持久化仍在；findDirectSuperior 一人一上级保持观察 |
| M009-050 | 细化 | 否 | 否 | PGM-041 / GEN-010 | 手机号 PiiCipher+无权限掩码仍在；不覆盖 058/059 |
| M009-051 | 技术性 | 否 | 否 | GEN-012 | 分页 COUNT/LIMIT。无业务发明 |
| M009-052 | 细化 | 否 | 否 | BSM-004 | DELETE 拒绝、导向存档审批。directDeleteSpaceTypeRequiresArchive… 绿 |
| M009-053 | 技术性 | 否 | 否 | GEN-001 / GEN-017 | package-info 仅声明。无业务发明 |
| M009-057 | 细化 | 否 | 否 | BSM-004 / PGM-005 / PGM-020 / GEN-002 / GEN-008 / GEN-016 | SpaceTypeRequest L6 注释已是新建不可重复固定启用；createSpaceTypeIgnoresRequestedDisabled… 绿 |
| M009-058 | 冲突（历史） | **否** | 否 | PGM-041 L656 / PGM-059 L871 / GEN-002 / GEN-005 / GEN-011 | **消费：** 七入口 helper 在明文前 `requirePermissions(功能,电话)`，按来源一次 markConsumed；无电话权不碰电话权。四组合+消费失败本轮绿。**留痕（原第2项）：** merchant.viewed 含 merchantIds/entry/projectId/筛选/页，不含姓名电话。本轮绿。**明文例外：** 持权或 USB 主授权可见列表/写回号码，符合 L871；无导出打印，符合 L680。不把 B4 PASS 当证据，以本轮命令为准 |
| M009-059 | 细化 | **否** | 否 | PGM-026 / GEN-010 / GEN-008；操作员「应该加密」主次姓名 | JDBC merchantParameters/mapMerchant 对 contact_name、secondary_contact_name 走 PiiCipher；owner_person/法人未动。只读列 TEXT。/tmp 120 字中文密文无明文、往返真。旧行无 enc: 前缀透传，不声称存量全部已加密，不回填。本席未跑写库集成用例 |

无添加项。无 HTML 修改申请。

---

## 10. 原 OBS 及 B4 观察并列保留（不入队、不扩 XM）

Grok 原编号：

- OBS01 空间类型存档/删审未走 executeWrite 幂等键；顺序路径可失败重放。委托既有，不新造幂等器。
- OBS02 无项目 scope 的 list*Paged 四参重载仍在 JDBC，生产 list 走带 projectId 五参。已修列表隔离不回退。
- OBS03 V259 等权限种子跨模块；V339/V394 对 `pii.contact_phone.view` 的角色直授属 XM/权限模块。
- OBS04 `requireDate`/`readObjectMap` 本 20 文件 0 命中，保持「已无」。

Codex V0.16 另一组语义并列：营业时段 Controller 委托 `VenueBusinessHoursService`（实现不在 20 文件）；owner/legal 姓名不在本批准加密范围；V339/V394 种子同上。

XM-008/010/011/012 各归既有输入，本模块不扩大。

空间类型 `saveSpaceType` 的 ON CONFLICT UPDATE 使**并发双新建同一 typeCode** 可能变成更新而非冲突。顺序已拒。不单独立项。

---

## 11. 新 finding

无。不启用锁外判据。不把观察写成添加。

四分类：本轮无新冲突/细化缺陷/技术性缺陷/添加。

---

## 12. 环境红保留

V427 未发布迁移与 Flyway 顺序红、全局 PGM 台账/入口快照等既有环境事实保留。本席不宣称全链绿、不修环境、不跑全量后端。

---

## 13. 本席步骤结论

范围 20/20 文件、3381 行、88/88 判据定义+HTML 适用段 70/70 标题命中。三线完成。26 项重判当前均无工程缺陷。058 单次消费与列表对象留痕、059 新建保存加密均被本轮命令/探针支持。三项现文未破。无未答 questions。无锁外主张。

**B5-r5 本席 PASS。**

（合取与 ledger PASS 由调度在两席完整 raw 落齐且四值校验后执行；本席不写收口文件、不改 ledger。）


CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# M009 B5-r5 Grok 整模块三线审计 V0.02

席位：Grok（乙），独立只读。步骤：B5-r5（修复后整模块重审，不只看 diff）。B3 累计 5（本席不改 ledger）。  
正本：`/home/james/complex-management` main 工作区。HEAD 只读观测 `e456ba96c84185c8d39f18f548011daac48a6466`（未 fetch、未提交、未推送）。  
规程：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 与 `docs/audit-2026-09/plan/runbook.md` V0.02。  
**禁止项已遵守**：未读本次 `M009-b5-r5-self-raw.md`；不继承 B4 或任何旧 PASS（含本文件 V0.01 段结论）；不改生产代码/测试/三 HTML/锁/台账/主 `.git`；不 fetch/提交/推送/部署/安装/MCP/子代理。本批生产仅 Controller 变，无数据库修改。不把 06:58 `PlatformJdbcIntegrationTests` XML 当本次测试。不宣称全链绿。V427 顺序等既有环境红保留。XM 各模块不扩。先找齐问题再落盘，边审边不修。

三项批准按 **HTML 现文**（锁摘录已含现句），不引用旧禁令，不擅加新功能、姓名/生日展示规则或旧数据回填：

1. **PGM871（PGM-059 L871）**：商户档案第一/第二联系人手机号，经单独授权且查看权限有效的人员，以及持授权 U 盘密钥的最高权限本人，可以在其获准访问的商户档案列表和档案操作结果中查看完整号码；其他人员仍只显示脱敏号码。平台管理员身份本身不自动取得该查看权限。例外仅适用于商户档案联系人，不扩大到客人或员工档案，也不免除项目范围检查和查看留痕。
2. **PGM680（PGM-043 L680 末句）**：该页面查看例外不授予未脱敏导出或打印权限，也不免除敏感页的查看留痕及其他页面保护要求。本 20 文件无导出/打印入口，不扩。
3. **PGM656（PGM-041 L656）**：临时授权可设有效期或单次使用；权限的授予、收回、到期、单次消耗、失效全部留痕。原句保持。既有 scope/幂等时序按已批准 B2 保持。

findings.md **V3.49**：当前 26 项修复队列全部为否。本席仍逐项重判，包括 058 单次消费及此前第 2 项留痕、059 姓名保存加密。

## 0. 技能自核

已读并用收窄口径：`using-superpowers`（用户指令优先；本任务禁止子代理）、`verification-before-completion`（先跑命令再写 PASS）、`grilling`（只借追问结构，事实自读，不等操作员、不派子代理）、`codebase-design`（脱敏缝在 Controller，加密缝在 JDBC）、`diagnosing-bugs`（以能失败单测为反馈环）、`ponytail-review`（找可删；048 隔离通道不删）、`writing-for-agents`（本 raw 给调度读）。  
明确不用：dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、research、MCP。  
可选未调用（记缺口）：`batch-grill-me`、`git-guardrails-claude-code`。`grilling` 本体“等用户回答 / 派子代理查事实”按提示词作废。

## 1. 判据锁哈希（本席独立实算）

命令：对 `docs/audit-2026-09/plan/criteria-lock.md` 做 SHA-256（Python hashlib，等同 sha256sum）。

| 源 | 值 |
|---|---|
| 本席实算 | `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458` |
| `plan/criteria-lock.sha256` | 同上 |
| `ledger.md` 顶部 `CRITERIA-LOCK-SHA256` | 同上 |
| lock 字节 / 行 | 891182 / 6856 |
| 锁版本 | V0.14，401 条定义，M009 映射 88 |
| LOCK-AMEND-COUNT（只读） | 1 |

实算 = sidecar = ledger。第四值由调度在两席完整 raw 落齐后取本段首行。本席不改 sidecar/ledger。

## 2. 范围：20 文件 × 3381 行（本席实算）

权威清单：`plan/modules.md` M009 `<!-- FILES-BEGIN -->` 至 `FILES-END`，20 路径，标记 NORMAL。  
命令：`git ls-files -z -- <20 paths> | xargs -0 cat | wc -l` → **3381**（与逐文件 `count(b'\\n')` 合计一致）。modules.md 历史写 2476，以实跑 3381 为准。

未把 `ProjectRecord.java` / `VenueBusinessHoursService.java` / `ProjectAssociationRequest.java` 扩进本模块（依赖只读，XM 不扩）。

| 行 | SHA256 | 路径 | 覆盖 |
|---:|---|---|---|
| 17 | 45431c580d0bca0c217bcdb8ea120604eb7ed2b441845a5ac52257d5130b1277 | `.../governance/BusinessEntityRecord.java` | 全文 |
| 19 | f97af4de8ed4d45134196b4fcb83169591d5991e6aefaa4aa22411a49e78f422 | `.../governance/BusinessEntityBindingRecord.java` | 全文 |
| 116 | dda319c37032c33c3cc5d260a0c9f43c7740d21b80a106c4a4efd49a19cdef00 | `.../governance/MerchantRecord.java` | 全文 |
| 9 | e08618c856b35a1eda8447a9c9bd0c88ed00a9ee371c1ee33d3f2f3ab7ce5e40 | `.../governance/MerchantDeleteDecision.java` | 全文 |
| 1338 | 84c868bb9739fc68d630ac6c7da1c99767c7a974ba2e763102ba473f52618855 | `.../governance/JdbcPlatformRepository.java` | 全文 |
| 302 | 53d38fcb4910ba0de5acc7418936d3388673b662a0b0a427dced54b5baff0bc6 | `.../governance/PlatformController.java` V0.03 | 全文 |
| 204 | df6dfee9894fc34f1df28fdae3d549ba50320df14219129779fa267affb08cdc | `.../governance/PlatformRepository.java` V0.06 | 全文 |
| 1179 | 993cfdb8cdacb285a37027c8bfda1c983fcb5c3e0f26f7462f2235454f45827e | `.../governance/PlatformService.java` V0.12 | 全文 |
| 1 | e3094c51b508bf10015ead9e92ac2ae2f3edf3b09786d88816ab8009d21921d9 | `.../governance/package-info.java` | 全文 |
| 17 | 66d4a8cd5c7f206f0c023e190ac5ed94ff6585263a737e35f062be9d8de211da | `.../dto/BusinessEntityBindingRequest.java` | 全文 |
| 26 | 18911088cbde9575ca7c38f9a6ac6eee3f4a812098aea9a29482133fe14dec19 | `.../dto/BusinessEntityRequest.java` | 全文 |
| 6 | e0f16f5417b61e801ed23803616450e90810853ec7588a49714b43040d809f93 | `.../dto/MerchantArchiveRequest.java` | 全文 |
| 5 | 2516fb1d78e082fe89222a41f7178dde4b40d749c5ed2c2e712bbb14a74112c4 | `.../dto/MerchantDeleteApprovalRequest.java` | 全文 |
| 5 | 4898e50f2150e27c73d6d400b317d792f02c342a0e95a8b3dadbef5a7ebc8f45 | `.../dto/MerchantDeleteRequest.java` | 全文 |
| 41 | 7b6dd241328b57dc646eb9b3c531e01e3a3a3d1bb1b848711c7ce43106973cf1 | `.../dto/MerchantProfileRequest.java` | 全文 |
| 26 | 6ac09b14c1766bebd6df69fbf5579ccf602959d10179b497ff90ea1b3f521108 | `.../dto/ProjectRequest.java` | 全文 |
| 21 | 63857df14972fd6904a32fc7aade0de49b88ec3b946df493f61086c77d1ab7aa | `.../dto/SpaceRequest.java` | 全文 |
| 16 | 7c3a5fa28ab5a45b49d3fc325e39d1f25ddcbc38b86de3872ca86ce338b05f34 | `.../dto/SpaceTypeRequest.java` | 全文 |
| 11 | 75b75cfaee897096f6fce0886fc4e71da8c5762774a901739cff244e1425f60f | `.../dto/StatusChangeRequest.java` | 全文 |
| 22 | 6347c6dfe087be2310fe5f618a0ac27e188f340d1af84add2ab831e98cb333f1 | `.../dto/VenueBusinessHoursRequest.java` | 全文 |

### 2.1 20 文件 SHA 与第一性三问

| 文件 | 为什么必须存在（删掉则哪条 HTML 做不到） | 声称行为是否被执行验证 | 未经验证就当真的前提 |
|---|---|---|---|
| BusinessEntityRecord.java | PGM-004/018/019 主体档案字段承载 | 记录结构；由 Service/Jdbc/单测读写 | 无独立可执行体 |
| BusinessEntityBindingRecord.java | PGM-026 四类主体唯一关系 | 同上 | 无独立可执行体 |
| MerchantRecord.java | PGM-006/026 商户档案；`withMaskedPhones` 落实 L871 无授权脱敏 | Controller 单测掩码/明文 | 掩码规则在 PhoneMask（依赖） |
| MerchantDeleteDecision.java | BSM-004 删除审批结果 | Service 单测 approved/rejected | 无 |
| JdbcPlatformRepository.java | 持久化+项目过滤+PII 加解密+审批链 | 本次未跑写库集成；加解密由 PiiCipher 单测+源码调用点+ /tmp 探针 | 列类型 TEXT 属 M010，不声称库内旧行已加密 |
| PlatformController.java | HTTP 入口、功能码、电话权一次消费、脱敏缝 | 本次 19 例全 PASS | scope 在消费之后（B2 已批准保持） |
| PlatformRepository.java | 仓储缝；隔离 COUNT/LIMIT | 内存假实现支撑 70 例 Service 单测 | 默认 listMerchantsByBusinessEntity 内存过滤 |
| PlatformService.java | 业务规则、隔离、审计、绑定约束 | 本次 70 例全 PASS，含 merchant.viewed 上下文 | InMemory 不加密 |
| package-info.java | 编译单元 | `wc` 1 行 | 无业务 |
| BusinessEntityBindingRequest.java | 绑定写入契约 | Service 单测 | 无 Bean Validation 注解，靠服务层 requireText |
| BusinessEntityRequest.java | 主体写入；projectId 可空走当前项目 | Service 单测 | 旧 9 参构造兼容 |
| MerchantArchiveRequest.java | BSM-004 存档理由 | Controller/Service 单测 | 无 |
| MerchantDeleteApprovalRequest.java | 删除审批 decision | Service 单测 | 无 |
| MerchantDeleteRequest.java | 删除申请理由 | Service 单测 | 无 |
| MerchantProfileRequest.java | 商户档案写入含主次联系人 | Service 单测 | 联系人姓名加密只在 JDBC 路径 |
| ProjectRequest.java | PGM-020 项目含负责人/地址/经营范围 | Service `projectCreationReturnsApprovedOptionalProfileFields` | 无页面补录 |
| SpaceRequest.java | 空间创建 | Service 单测项目/父空间/面积 | 无 |
| SpaceTypeRequest.java | 种类新建；注释 V0.01 已改为不可重复新建 | `createSpaceTypeRejectsExistingCodeEvenWhenActive` | JDBC ON CONFLICT 仍是持久化原语 |
| StatusChangeRequest.java | 绑定变更动作 | Service 拒绝停用生效绑定 | 无 |
| VenueBusinessHoursRequest.java | 营业时间写入 DTO；逻辑在 M015 服务 | Controller 仅转发；本模块无独立营业时间测试 | 不扩 M015 |

## 3. 88 判据定义 + 三 HTML 适用段核对

映射原文（lock 6394 行）：88 个稳定 ID，本席从定义区抽出 88/88，缺 0。  
HTML 闭集现文件：BEQR `td.k` 197；BSM h2+h3 27+93=120；PGM h2+h3 18+48=66。  
本模块 70 条 HTML 派生：BEQR 21 条锁摘录与现文件对应行 **LINE-EXACT**；BSM 14 + PGM 35 条锁摘录均 **IN-SECTION**（含于该 h2/h3 标题下适用段）。GEN 18 条在 lock `## general` 全文。  
三项现文核对（live 行 ∈ 锁摘录）：

| 批准点 | live 行 | 锁 ID | 结果 |
|---|---|---|---|
| PGM656 单次消耗原句 | PGM.html:656 | PGM-041 | YES |
| PGM680 查看例外不授导出打印 | PGM.html:680 | PGM-043 | YES |
| PGM871 列表/操作结果可明文 | PGM.html:871 | PGM-059 | YES |

PGM 文件版本注释 `V0.13`。不引用旧“日常界面一律不见明文、无例外”禁令。

### 3.1 判据矩阵（88/88）

| ID | 标题 | 锁来源 | HTML 核对 | 本模块适用面 | 本席结论 |
|---|---|---|---|---|---|
| BEQR-063 | 高危动作（需审批/双证据） | BEQR.html:111 | LINE-EXACT | 商户/空间种类删除走最高权限审批与占用检查，非酒吧高危全集 | 无新缺陷 |
| BEQR-074 | 隐私桌台权限 | BEQR.html:125 | LINE-EXACT | 无桌台授权实现面，不发明 | 无新缺陷 |
| BEQR-143 | 四类主体 | BEQR.html:226 | LINE-EXACT | 经营对象+三主体绑定 | 无新缺陷 |
| BEQR-144 | 经营主体关系 | BEQR.html:227 | LINE-EXACT | business_entity_bindings | 无新缺陷 |
| BEQR-145 | 城市项目公司 | BEQR.html:228 | LINE-EXACT | entity_role=project_company | 无新缺陷 |
| BEQR-146 | 运营公司职责 | BEQR.html:229 | LINE-EXACT | entity_role=management_company | 无新缺陷 |
| BEQR-147 | 主要收入归属 | BEQR.html:230 | LINE-EXACT | merchant_type 六业态承载，不算分账 | 无新缺陷 |
| BEQR-148 | 合作模式 | BEQR.html:231 | LINE-EXACT | cooperation_form 六类 | 无新缺陷 |
| BEQR-149 | 职级 | BEQR.html:232 | LINE-EXACT | 无职级配置面 | 无新缺陷 |
| BEQR-150 | 终端十一端 | BEQR.html:233 | LINE-EXACT | 无终端面 | 无新缺陷 |
| BEQR-151 | 最终权限计算 | BEQR.html:234 | LINE-EXACT | 委托 AuthorizationService | 无新缺陷 |
| BEQR-152 | 项目与敏感数据权限 | BEQR.html:235 | LINE-EXACT | currentProject 列表/写入隔离 | 无新缺陷 |
| BEQR-153 | 终端开关优先级 | BEQR.html:236 | LINE-EXACT | 无终端开关 | 无新缺陷 |
| BEQR-154 | 综合体项目状态 | BEQR.html:237 | LINE-EXACT | PROJECT_STATUSES 六态 | 无新缺陷 |
| BEQR-155 | 商户/运营主体状态 | BEQR.html:238 | LINE-EXACT | SUBJECT_STATUSES；拒直接 terminated | 无新缺陷 |
| BEQR-156 | 合同状态 | BEQR.html:239 | LINE-EXACT | 无合同状态机 | 无新缺陷 |
| BEQR-157 | 员工状态 | BEQR.html:240 | LINE-EXACT | 无员工面 | 无新缺陷 |
| BEQR-158 | 设备状态 | BEQR.html:241 | LINE-EXACT | 无设备面 | 无新缺陷 |
| BEQR-159 | 权限状态 | BEQR.html:242 | LINE-EXACT | 无权限字典面 | 无新缺陷 |
| BEQR-166 | 合同审批链 | BEQR.html:255 | LINE-EXACT | Jdbc 持久化审批链表，不新增角色 | 无新缺陷 |
| BEQR-189 | 管理费方式 | BEQR.html:291 | LINE-EXACT | 无计费面 | 无新缺陷 |
| BSM-001 | 整体说明 | BSM.html:505 | IN-SECTION | 底座主数据 | 无新缺陷 |
| BSM-003 | 系统通用能力 | BSM.html:563 | IN-SECTION | 可改可删、分岗入口 | 无新缺陷 |
| BSM-004 | 配置可存档可删除 | BSM.html:566 | IN-SECTION | 商户/种类存档审批软删占用 | 无新缺陷 |
| BSM-005 | 分步引导 | BSM.html:581 | IN-SECTION | 无前端向导 | 无新缺陷 |
| BSM-006 | 动态二维码 | BSM.html:584 | IN-SECTION | 无 | 无新缺陷 |
| BSM-007 | 平台管理员日常权限 | BSM.html:587 | IN-SECTION | 平台范围；pii 非日常自动取得 | 无新缺陷 |
| BSM-009 | 基础管理 | BSM.html:596 | IN-SECTION | 项目/空间/商户/主体 | 无新缺陷 |
| BSM-028 | 主要功能 | BSM.html:698 | IN-SECTION | 项目可选三字段 | 无新缺陷 |
| BSM-029 | 和谁有关 | BSM.html:714 | IN-SECTION | 主体-项目-商户 | 无新缺陷 |
| BSM-072 | 商户管理 | BSM.html:1308 | IN-SECTION | 隔离通道+平台入口拒商户账号 | 无新缺陷 |
| BSM-074 | 和谁有关 | BSM.html:1326 | IN-SECTION | 不扩商户门户 | 无新缺陷 |
| BSM-114 | 各类入口 | BSM.html:1827 | IN-SECTION | 按功能码 | 无新缺陷 |
| BSM-115 | 相互关系 | BSM.html:1897 | IN-SECTION | 绑定投影三主体 | 无新缺陷 |
| BSM-120 | 已确认规则 | BSM.html:1957 | IN-SECTION | 不把计划写成完成 | 无新缺陷 |
| GEN-001 | 必要性与最小性 | 通用 | 定义全文 | 048 隔离通道保留；package-info 编译单元 | 无新缺陷 |
| GEN-002 | 可执行正确性 | 通用 | 定义全文 | 本次 93 单测 | 无新缺陷 |
| GEN-003 | 测试有效性 | 通用 | 定义全文 | 断言能失败；不引用旧 JDBC XML | 无新缺陷 |
| GEN-004 | 信任边界校验 | 通用 | 定义全文 | 白名单/存在性/非负 | 无新缺陷 |
| GEN-005 | 授权与最小权限 | 通用 | 定义全文 | 功能码+范围+电话另权 | 无新缺陷 |
| GEN-006 | 数据完整性 | 通用 | 定义全文 | 锁商户/占用/软删 | 无新缺陷 |
| GEN-007 | 并发与幂等 | 通用 | 定义全文 | 写路径幂等键；单次按来源消费一次 | 无新缺陷 |
| GEN-008 | 接口与模式一致性 | 通用 | 定义全文 | 词表一致；用户消息无英文枚举 | 无新缺陷 |
| GEN-009 | 错误处理与恢复 | 通用 | 定义全文 | 400/403，消费失败不回明文 | 无新缺陷 |
| GEN-010 | 安全与敏感信息 | 通用 | 定义全文 | 手机号+联系人姓名落库加密；展示按权 | 无新缺陷 |
| GEN-011 | 可观测与审计证据 | 通用 | 定义全文 | merchant.viewed 含对象与查询上下文 | 无新缺陷 |
| GEN-012 | 资源与性能边界 | 通用 | 定义全文 | COUNT+LIMIT 上限 500 | 无新缺陷 |
| GEN-013 | 构建配置与依赖 | 通用 | 定义全文 | 本模块无构建脚本 | 无新缺陷 |
| GEN-014 | 数据库迁移正确性 | 通用 | 定义全文 | 20 文件无迁移；本批无 DB 修改 | 无新缺陷 |
| GEN-015 | 界面可用性 | 通用 | 定义全文 | 无 UI；错误中文 | 无新缺陷 |
| GEN-016 | 文档可验证性 | 通用 | 定义全文 | SpaceTypeRequest 注释与新建语义一致 | 无新缺陷 |
| GEN-017 | 业务来源分类 | 通用 | 定义全文 | 无新添加项 | 无新缺陷 |
| GEN-018 | 第一性原理证据 | 通用 | 定义全文 | HTML/单测/命令 | 无新缺陷 |
| PGM-001 | 整体定位 | PGM.html:256 | IN-SECTION | 治理底座 | 无新缺陷 |
| PGM-002 | 治理顺序 | PGM.html:308 | IN-SECTION | 先权限范围再业务 | 无新缺陷 |
| PGM-003 | 模块入口 | PGM.html:339 | IN-SECTION | 综合体/空间/商户/主体 | 无新缺陷 |
| PGM-004 | 综合体和集团 | PGM.html:342 | IN-SECTION | 项目/主体 | 无新缺陷 |
| PGM-005 | 空间和点位 | PGM.html:347 | IN-SECTION | 空间+种类字典 | 无新缺陷 |
| PGM-006 | 商户和主体 | PGM.html:352 | IN-SECTION | 档案与绑定 | 无新缺陷 |
| PGM-007 | 合同和准入 | PGM.html:357 | IN-SECTION | 无合同全链；拒改已有财税主体 | 无新缺陷 |
| PGM-008 | 员工和手机 | PGM.html:362 | IN-SECTION | 无员工面 | 无新缺陷 |
| PGM-009 | 权限和职级 | PGM.html:367 | IN-SECTION | 单一功能码 | 无新缺陷 |
| PGM-010 | 终端开关 | PGM.html:372 | IN-SECTION | 无 | 无新缺陷 |
| PGM-011 | 设备管理 | PGM.html:378 | IN-SECTION | 营业时间仅转发 M015 | 无新缺陷 |
| PGM-012 | 商户侧 | PGM.html:383 | IN-SECTION | 无门户 | 无新缺陷 |
| PGM-013 | 开放接口 | PGM.html:388 | IN-SECTION | 无 | 无新缺陷 |
| PGM-014 | 留痕复核 | PGM.html:393 | IN-SECTION | 审计对象/理由 | 无新缺陷 |
| PGM-015 | 楼宇运营 | PGM.html:399 | IN-SECTION | 无工单面 | 无新缺陷 |
| PGM-016 | 应急与隐私 | PGM.html:404 | IN-SECTION | 隐私见 PGM-059 | 无新缺陷 |
| PGM-017 | 综合体和集团详 | PGM.html:412 | IN-SECTION | 当前项目；跨项目仅最高权限补录 | 无新缺陷 |
| PGM-018 | 物理项目与运营公司多对多 | PGM.html:415 | IN-SECTION | business_entity_projects | 无新缺陷 |
| PGM-019 | 各主体负责 | PGM.html:424 | IN-SECTION | 三角色白名单 | 无新缺陷 |
| PGM-020 | 主要功能 | PGM.html:440 | IN-SECTION | 项目/商户/空间字段 | 无新缺陷 |
| PGM-021 | 业务意义 | PGM.html:452 | IN-SECTION | 隔离与绑定 | 无新缺陷 |
| PGM-026 | 商户主要功能 | PGM.html:492 | IN-SECTION | 档案联系人合作方式四类绑定；不发明招商全链 | 无新缺陷 |
| PGM-027 | 业务边界 | PGM.html:507 | IN-SECTION | requirePlatformScope | 无新缺陷 |
| PGM-031 | 外部经销商 | PGM.html:555 | IN-SECTION | 无 | 无新缺陷 |
| PGM-035 | 提成发放 | PGM.html:599 | IN-SECTION | 无 | 无新缺陷 |
| PGM-040 | 权限和职级 h2 | PGM.html:643 | IN-SECTION | 单一功能码 | 无新缺陷 |
| PGM-041 | 权限规则 | PGM.html:647 | IN-SECTION | L656 原句；requirePermissions 一次消费；项目隔离 | 无新缺陷 |
| PGM-042 | 职级规则 | PGM.html:664 | IN-SECTION | 无职级配置 | 无新缺陷 |
| PGM-043 | 外包与审计人员 | PGM.html:673 | IN-SECTION | L680 不扩导出打印；本模块 0 个 export/print | 无新缺陷 |
| PGM-044 | 桌台挂单经销 | PGM.html:688 | IN-SECTION | 无 | 无新缺陷 |
| PGM-048 | 商户侧全开放 | PGM.html:749 | IN-SECTION | 无门户 | 无新缺陷 |
| PGM-059 | 客人与员工个人信息保护 | PGM.html:868 | IN-SECTION | L871 现文列表/操作结果明文例外；无授权脱敏 | 无新缺陷 |
| PGM-060 | 会员身份营销生物 | PGM.html:879 | IN-SECTION | 无 | 无新缺陷 |
| PGM-065 | 连接关系 | PGM.html:935 | IN-SECTION | 主体/商户主键被引用 | 无新缺陷 |
| PGM-066 | 已拍板规则 | PGM.html:951 | IN-SECTION | 用户错误中文 | 无新缺陷 |

计数：BEQR 21 + BSM 14 + GEN 18 + PGM 35 = **88**。定义全在、映射全在、HTML 适用段 70/70 命中。

## 4. 第一性原理线

三问已在 §2.1 按文件作答。模块级：

1. **为何必须存在**：删掉这 20 文件，PGM-003/004/005/006/017/018/019/020/026/027、BSM-004/028/072、BEQR-143–155 的主体/商户/空间/项目后端入口会落空。
2. **声称是否执行**：Controller/Service 行为由本次 93 单测执行（exit 0）。JDBC 加密调用点只读证实；列存储与旧行未跑写库集成，不声称库内已全部加密。
3. **未验证当真的前提**：`AuthorizationService.requirePermissions` 按来源消费（只读依赖，单测用真实授权器 4 参覆盖）；V427 迁移顺序属环境红，不在 20 文件内，不修。

依据类型：HTML 原文（1）、本次单测与 /tmp 探针（2/4）。不接受“B4 已 PASS”“测试以前绿过”。

## 5. 顺从式线（假定实现意图正确，逐 ID 已在矩阵勾完）

关键现文对齐（不引用旧禁令）：

- **L871 明文例外**：`requireMerchantPermission` 在持 `pii.contact_phone.view` 时调用 `requirePermissions(功能码, pii.contact_phone.view)`，通过则 `viewPhonePlaintext=true`，`maskPhonesForUnauthorized` 原样返回。七个返回 `MerchantRecord` 的入口全部走该 helper：GET `/merchants`、POST `/merchants`、PUT `/merchants/{id}`、PUT `/merchants/{id}/project`、POST archive/unarchive/delete-request。DELETE-approve 返回 `MerchantDeleteDecision` 无电话字段。无该权时只 `requirePermission(功能码)` 并掩码。
- **管理员不自动取得**：`permissionGrant` 对 master 放行；platform_admin 仅日常码桥。`pii.contact_phone.view` 在 V394 为 `requires_approval=TRUE`，且不在 `PLATFORM_ADMIN_GOVERNANCE_PERMISSIONS`；`isExplicitOrDailyGrant` 过滤 `role_platform_admin` 高危直授。依赖只读，不扩 M006。
- **L656 单次消耗**：`requirePermissions` 先核验全部码，再按 `source:sourceId` `putIfAbsent` 消费一次。`currentActorHasPermission` 注释与实现为布尔、不抛错、不消费。同组功能+电话同一 TEMPORARY_GRANT 时只 mark 一次。
- **L680 不扩导出打印**：`PlatformController` 对 export/print/导出/打印 命中 0。
- **留痕**：`listMerchants` 写 `merchant.viewed` metadata：`merchantIds/entry/projectId/includeArchived/businessFormat/page/pageSize/count/total`，断言不含姓名手机号。写路径 `executeWrite` 用商户 id。
- **059 姓名**：`merchantParameters` 对 `contact_name`/`secondary_contact_name` `piiCipher.encrypt`；`mapMerchant` decrypt。`owner_person`/`legal_representative` 不扩。PiiCipher 对无 `enc:v1:` 前缀原样透传（兼容旧行，不回填）。
- **隔离/绑定/存档删除/非负/退出缓冲**：Service 70 例覆盖，见本次 XML 方法名。

## 6. 对抗式线（假定实现错误；grilling 结构自答）

Q1 只读查询是否仍把单次电话权烧在 `currentActorHasPermission`？  
→ 否。该方法 try/permissionGrant/return true，无 `markSingleUsePermissionConsumed`。消费只在随后 `requirePermissions`。

Q2 同组功能权是否先把电话权耗掉？  
→ `putIfAbsent(source+sourceId)`。同 grant 只消费一次。单测 `merchantPhoneSingleUseAppliesToOneResponse` 四参：写/读 × 同grant/分grant，本次全 PASS。第二次：同 grant 则 PERMISSION_DENIED；分 grant 则功能权仍在、电话掩码。

Q3 消费失败是否仍返回明文？  
→ `failedPhonePermissionConsumptionDoesNotReturnPlaintext`：`requirePermissions` 抛错则 403，`verifyNoInteractions(service)`。PASS。

Q4 无电话权的列表是否明文？  
→ `listingMerchantsReturnsApiResponsePageShape` 期望 `188****0011`。PASS。有权明文：`listingMerchantsReturnsPlaintextPhoneWithPiiPermission`。PASS。

Q5 列表多条是否一次消费看完全页？  
→ 单测 list 两条都明文且 `markSingleUsePermissionConsumed` times=1。符合“一次响应”。

Q6 是否新增导出/打印绕过 L680？  
→ Controller 无对应 mapping。

Q7 是否把姓名/生日做成新展示加密规则或回填？  
→ 无姓名掩码、无生日字段、无 backfill 调用。姓名仅 JDBC 写加密。符合“不擅加”。

Q8 审计是否写下姓名手机号？  
→ `merchantViewAuditRecordsOnlyReturnedPageAndQueryContext` metadata 不含 Jane/18800000011。PASS。

Q9 普通岗位跨项目？  
→ `employeeCannotReadOtherProjectThroughSpaceLookupOrWrite`、`projectListsAndTotalsStayWithinCurrentProjectForOrdinaryAdmin`、`masterHasNoGlobalListOrOrdinaryProfileBypass`。PASS。

Q10 生效绑定能否普通停用改财税主体？  
→ `activeBindingCannotBeDisabledToReplaceFinancialSubjects`。PASS。

Q11 直接 terminated？  
→ `ordinaryMerchantProfileCannotCauseImmediateExit`。PASS。

Q12 平台管理员身份是否自动拿到电话明文？  
→ 授权器日常桥不含该高危码；本模块不因 accountType=platform_admin 绕过 mask。master 例外与 L871 一致。

Q13 消费后再 `requirePlatformScope` 失败会否烧授权？  
→ 会。B2 已批准保持该时序，本席不新立项。

Q14 `saveSpaceType` JDBC upsert 是否让重复编码改已有行？  
→ Service 在调用前 `SPACE_TYPE_CODE_EXISTS`。`createSpaceTypeRejectsExistingCodeEvenWhenActive` PASS。接口注释“upsert”指持久化原语（存档/恢复也走同一方法），不构成用户可见“存在则更新”产品承诺（057 已改 DTO 注释）。

未发现新的能用冻结 ID 证成的工程缺陷。

## 7. 既有 26 项逐项重判（当前队列否，仍重判）

分类顺序仍是冲突→细化→技术性→添加。当前工程缺陷 / 当前队列均为本席本轮值。

| ID | 历史分类 | 本轮事实核对 | 当前缺陷 | 当前队列 | 冻结依据 |
|---|---|---|---|---|---|
| M009-038 | 冲突 | 列表/写入 `currentProject`；跨项目拒绝 | 否 | 否 | PGM-041 / BEQR-152 / GEN-005 |
| M009-039 | 冲突 | createSpace 强制本项目；父空间同项目 | 否 | 否 | PGM-017 / GEN-004 / GEN-006 |
| M009-040 | 冲突 | 生效绑定不可普通停用改三主体 | 否 | 否 | PGM-026 / GEN-005 |
| M009-041 | 冲突 | 未来生效 inactive 不投影 | 否 | 否 | PGM-026 / GEN-006 / GEN-008 |
| M009-031 | 冲突 | 生效绑定失效拒绝；日期不倒置 | 否 | 否 | PGM-026 / BEQR-143 / GEN-006 |
| M009-042 | 冲突 | unarchive 接受 archived 与 disabled | 否 | 否 | BSM-004 / GEN-009 |
| M009-043 | 冲突 | 审计带对象 ID；驳回事件 rejected | 否 | 否 | BSM-004 / PGM-066 / GEN-011 |
| M009-044 | 细化 | completeSpaceTypeDeleteRequest 结清 pending | 否 | 否 | BSM-004 / GEN-006 / GEN-011 |
| M009-045 | 细化 | 已结束 inactive 绑定不计入占用 | 否 | 否 | BSM-004 / GEN-002 / GEN-006 |
| M009-046 | 细化 | 容量/面积/租金拒负 | 否 | 否 | PGM-020 / PGM-026 / GEN-004 |
| M009-030 | 细化 | 新建校验已给三主体存在且本项目 | 否 | 否 | PGM-026 / BEQR-143 / GEN-004 |
| M009-047 | 冲突 | 用户消息无英文枚举括注 | 否 | 否 | PGM-001 / PGM-066 / GEN-015 |
| M009-018 | 冲突 | 普通入口拒直接 terminated | 否 | 否 | BEQR-155 / GEN-006 |
| M009-013 | 细化 | ProjectRequest/Record 有 owner/address/scope | 否 | 否 | PGM-020 / BSM-028 / GEN-002 |
| M009-054 | 冲突 | 不在本 20 文件（AccessService）；不扩 XM | 否（本模块无回归面） | 否 | BSM-004 / GEN-005 |
| M009-055 | 细化 | 外模块测试夹具；不扩 | 否 | 否 | GEN-003 |
| M009-056 | 细化 | 外模块测试；不扩 | 否 | 否 | GEN-003 |
| M009-048 | 细化 | listMerchantsByBusinessEntity 仍在，隔离门禁承载 | 否 | 否 | PGM-027 / BSM-072 |
| M009-049 | 细化 | Jdbc 审批链持久化仍在 | 否 | 否 | BEQR-166 |
| M009-050 | 细化 | 手机号 PiiCipher+无权限掩码仍在；不豁免 058/059 重判 | 否 | 否 | PGM-041 / GEN-010 |
| M009-051 | 技术性 | 分页 COUNT/LIMIT；无业务发明 | 否 | 否 | GEN-012 |
| M009-052 | 细化 | 直接 DELETE 种类拒绝、导向审批 | 否 | 否 | BSM-004 |
| M009-053 | 技术性 | package-info 仅 package 声明 | 否 | 否 | GEN-001 / GEN-017 |
| M009-057 | 细化 | SpaceTypeRequest 注释为新建不可重复 | 否 | 否 | BSM-004 / GEN-016 |
| **M009-058** | 冲突 | 见下专节 | **否** | **否** | PGM-041 L656 / PGM-059 L871 / GEN-002 / GEN-005 / GEN-011 |
| **M009-059** | 细化 | 见下专节 | **否** | **否** | PGM-026 / GEN-010 / GEN-008；操作员姓名范围；不采用 R006 为新判据 |

### 7.1 M009-058 重判（消费 + 第 2 项留痕）

历史缺陷：只查询即明文、布尔检查不消费、同组分次消费、列表审计只有 all/count/total。

现码：

```281:301:backend/src/main/java/com/complex/platform/governance/PlatformController.java
    private boolean requireMerchantPermission(String permissionCode) {
        boolean viewPhonePlaintext = authorizationService.currentActorHasPermission(PhoneMask.VIEW_PLAINTEXT_PERMISSION);
        if (viewPhonePlaintext) {
            authorizationService.requirePermissions(permissionCode, PhoneMask.VIEW_PLAINTEXT_PERMISSION);
        } else {
            authorizationService.requirePermission(permissionCode);
        }
        return viewPhonePlaintext;
    }
```

授权器：`requirePermissions` 先核验再按来源消费一次（`AuthorizationService.java:41-61`）。`currentActorHasPermission` 不消费（:265-277）。

L871 现文允许授权人员及最高权限在**列表和档案操作结果**看完整号码 → 持权明文不是冲突。L656 原句要求单次消耗留痕 → 现为一次 `requirePermissions`，不再“只查询不消费”。

留痕：`PlatformService.listMerchants` 写入 merchantIds/entry/projectId/筛选/页码；单测 `merchantViewAuditRecordsOnlyReturnedPageAndQueryContext` 本次 PASS。

本次执行：`merchantPhoneSingleUseAppliesToOneResponse` [1]–[4]、`failedPhonePermissionConsumptionDoesNotReturnPlaintext`、`listingMerchantsReturnsPlaintextPhoneWithPiiPermission`、掩码列表用例，全部 PASS。

**当前工程缺陷否，队列否。** 不把持权明文打回旧禁令。不扩导出打印。

### 7.2 M009-059 重判（主次联系人姓名新建/保存加密）

现码：`JdbcPlatformRepository.merchantParameters` 对 contact_name / secondary_contact_name 调用 `piiCipher.encrypt`；`mapMerchant` decrypt。owner_person / legal_representative 未扩。  
PiiCipher：无前缀视为历史明文透传；不二次加密；不回填。  
/tmp 探针（绝对路径 `/tmp/m009-b5-r5-probe`，已确认不在 `.git` 下）：120 字「王」roundtrip=true，legacy「张三」decrypt 原样，mask=`188****0011`。  
PiiCipherTests 4/4 PASS。

不声称：旧行未重存已全部加密；列类型 TEXT（属 M010/历史迁移）；本次未跑写库集成。

**当前工程缺陷否，队列否。** 符合“仅新建及后续保存走加密”。

## 8. 原 OBS 及 B4 观察并列保留（不入当前队列）

按 findings V3.49 纠正编号后并列，均无本轮新增队列：

- Grok 原 OBS01：空间类型幂等委托。
- OBS02：列表平台范围已修。
- OBS03：V259 审批权限种子跨模块。
- OBS04：requireDate/readObjectMap 死代码已无。
- Codex 另一组历史语义：营业时段委托、owner/legal 姓名边界、V339/V394 权限种子。
- XM-008/010/011/012 各归属既有输入，**不扩大本模块**。
- `currentProject()` 在项目不存在时返回 null → 后续 mismatch/空页，属已批准范围边界，不新立项。
- 消费后 scope 失败会烧掉单次授权：已批准 B2 时序，不新立项。
- V427 未发布迁移顺序 / validate-on-migrate：既有环境红，不修、不称全链绿。

## 9. 新 finding

无。无锁外主张，不走 LOCK-AMEND。无添加项，不写 questions。四分类本轮无新冲突/添加；细化与技术性项无新工程缺陷。

ponytail-review：`listMerchantsByBusinessEntity` 零调用保留（048/隔离门禁）。Controller helper 七入口共用，非多余抽象。`net: 0` 可删业务行。Lean already for this module's frozen IDs.

## 10. 实跑证据（本次，非旧 XML）

前置：`/tmp` 可写；`/tmp/m009-b5-r5-surefire` 与 `/tmp/m009-b5-r5-probe` 均不在 `.git` 下。

命令（cwd=`/home/james/complex-management/backend`）：

```
mvn -B test \
  -Dtest=com.complex.platform.governance.PlatformControllerTests,com.complex.platform.governance.PlatformServiceTests,com.complex.platform.common.crypto.PiiCipherTests \
  -DfailIfNoTests=true -DskipITs \
  -Dsurefire.reportsDirectory=/tmp/m009-b5-r5-surefire
```

结果（`/tmp/m009-b5-r5-mvn.log`，mtime 2026-09-22T07:36:12-07:00）：

- PiiCipherTests：**4** run, 0 fail, 0 error
- PlatformServiceTests：**70** run, 0 fail, 0 error
- PlatformControllerTests：**19** run, 0 fail, 0 error
- 合计 **93**，BUILD SUCCESS，进程 exit **0**
- 未运行 `PlatformJdbcIntegrationTests`（写库）。其 06:58 XML **不用作本次证据**。

说明：`-Dsurefire.reportsDirectory=/tmp/...` 本机 surefire 3.5.6 未改落盘目录；本次 07:36 的三份 XML 在 `backend/target/surefire-reports/`（PiiCipher/Controller/Service）。JDBC 那份仍为 06:58，已跳过。

/tmp 探针 `NameEncryptProbe`（classpath 仅已编译 test-free 类）：`name_len=120 enc_prefix=enc:v1: enc_len=527 roundtrip=true legacy_plain=true mask=188****0011 perm=pii.contact_phone.view`。未写数据库。

## 11. 环境红（保留，不修）

- 主工作区仍有 M009 历史暂存 V427 等与 origin/main 编号关系；`validate-on-migrate` 未开 out-of-order。不宣称全链绿。
- M426 PGM 台账同步、M435/M436 入口快照等全局红不在本 20 文件，不扩。

## 12. 本席步骤结论

范围 20/20 文件、3381 行、88/88 判据定义 + HTML 适用段 70/70。三线完成。26 项重判当前均无工程缺陷。058 单次消费与列表对象留痕、059 新建保存加密均被本次命令/探针支持。三项现文未破。无未答 questions。无锁外主张。无新 finding。

**B5-r5 本席 PASS。**

合取与 ledger PASS 由调度在两席完整 raw 落齐且四值校验后执行；本席不写收口文件、不改 ledger、不改 findings。

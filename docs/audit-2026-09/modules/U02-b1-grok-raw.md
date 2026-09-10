CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U02 B1 Grok 独立审计原文

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；操作员 2026-09-09 起席甲=Claude Fable 5.1，其票是作者自审票；本席 FAIL 具否决权）
- 阶段：B1 只读全量三线（未发布材料第 2 段；不做 B3/B4/B5；处置写给发布者，本轮不修源）
- 报告自身版本：V0.01（指定路径此前不存在，新建）
- 段：U02 — 未发布材料第 2 段「安卓共享请求与模型」
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 末尾附录；`plan/runbook.md` V0.02 §11；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 审计对象（不得冒称 main 或已发布修复）：
  - 未发布提交：`03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb`（本仓 `git cat-file -t` **无此 commit 对象**）
  - 未发布树：`e28dad059b2c7e08bcc95a8db18552d3f76edf4b`（`git cat-file -t` = **tree**，可解析）
  - 发布基线：`242f487233d028173cb9198eecd079f5d877405c`（commit，可解析）
  - 只读输入根：`/home/james/complex-management/.scratch/module-recheck-20260908/unpublished-03acdb98`
  - 清单：`.scratch/module-recheck-20260908/unpublished-03acdb98-input.json` V0.01，76 文件
- 本段：4 文件，物理行 1180，标记 NORMAL；main 主归属 M376（两新增文件 main 无对应）
- 冻结判据：M376 映射行并集 50 项（与锁 `criteria-lock.md` L6755 闭集相同；提示词顺序为 GEN 在前，锁行顺序为 BEQR/BSM 在前，ID 集合一致，COUNT=50 unique=50 MISSING=[]）
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 且发布前须修：GR-U02-001、GR-U02-002、GR-U02-003、GR-U02-004
- 添加项（B1 只登记，不代用户回答 2.2；B2 须逐条发问）：无
- 只登记、建议队列「否」：观察见 §8
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 self-raw 或其它本方结论、未问操作员、未对任一 `complex_test*` 库做写操作
- 本报告不表示全仓、main 模块或发布门禁通过。M001–M010 检查点已合并于 `bd445728`；当前 `HEAD=origin/main=445a49f4`（其上另有 M011–M012 检查点 PR #429）。原 322/471 模块结论不继承到本未发布段。U01 定稿 U01-001…015 只作同根对照，本席依据仍为 HTML/命令/本段源码。

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证，不向操作员发问、不派子代理 |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 先建可红反馈环（diff/canonical 对照/隔离 gradle 测试）再下结论；不改生产代码 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流（本段四文件→U01 android-app 客户端→未发布后端验签器→main 对照→HTML） |
| verification-before-completion | superpowers 同名 | 先跑命令再声称；隔离测试以 XML/exit 为准 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md` | 本 raw 写给下游席读：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 存在；本席未改 git |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 本段验证均秒级，未起 monitor |

B1 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：

- `batch-grill-me`：本机技能树未找到独立 SKILL（grilling 本体已读）
- `ubiquitous-language`：本机技能树未找到
- Arc `audit` 会派评审子代理：与双席/§7 冲突，**不调用**
- `using-superpowers` 要求技能优先于探索：与「禁止子代理 / 本提示指定 controller」冲突处，按用户指令收窄

明确不用：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、MCP、第三主体。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本文第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 同值
- `criteria-lock.md` 版本 V0.11；判据总数 401（文内「判据总数：401」；`### GEN-` 标题 18）
- 映射行 `M376 → [BEQR-150, BEQR-153, BSM-001, BSM-002, BSM-003, BSM-012, BSM-013, BSM-026, BSM-029, BSM-044, BSM-045, BSM-046, BSM-047, BSM-048, BSM-049, BSM-050, BSM-114, BSM-115, BSM-120, GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018, PGM-001, PGM-010, PGM-032, PGM-033, PGM-034, PGM-035, PGM-036, PGM-037, PGM-038, PGM-045, PGM-046, PGM-047, PGM-066]` 与开工 50 项闭集相同
- `plan/modules.md` 附录 U02 四路径与用户清单逐字相同

HEAD（本席实算，**不等于**开工提示词所写 `bd445728`）：

- `git rev-parse HEAD` = `445a49f409f0cfbe6dd616ae079be1299824c48f`
- `git rev-parse origin/main` = 同值
- `git merge-base HEAD origin/main` = 同值
- `bd445728a6ec7bb36a9e7ff19fe56927410ac61e` 是 HEAD 祖先（M001–M010 检查点）；HEAD 另含 `71460a01` / `445a49f4`（M011–M012 检查点 PR #429）
- 本席未设 `GIT_INDEX_FILE`，未动主索引

ledger 只读核验（不改）：V2.46，459259 bytes，SHA256 `79de8b15030991633b905c1b1fb51bc9ef7ea301931b943e12ccaf191c4d4373`（与开工提示一致）。findings V1.45，546652 bytes，SHA256 `8981eeb4e7ac2bc916385d90b674376beb956bb42fe1473554f1f7b78ede1971`（含 U01-001…015 定稿）。台账检索 `DeviceRequestHeaders` / `M376-`：**无已定稿项**覆盖本四文件。U01-001 正文写明「JVM 共享客户端同逻辑（U02-001）」为同根预告，本席仍独立取证，不以台账意见为 2.3 依据。

其它只读哈希：

| 文件 | 字节 | SHA256 |
|---|---:|---|
| `plan/runbook.md` V0.02 | 10409 | `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| `plan/modules.md` V0.04 | 421826 | `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` |
| `160-audit-prompt.md` V0.17 | 65856 | `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| `docs/business-enum-quick-reference-v0.1.html` | 133961 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | 304482 | `a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa` |
| `docs/platform-governance-map-for-operations-v0.1.html` | 103015 | `c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c` |

三 HTML 只读、未改。锁内本段用到的锚点行对本席读取的现行 HTML **全部命中**（见 §1.1）。三 HTML 本身未发现需人闸修改的原文错误。HTML 沉默处不升冲突。

### 1.1 HTML 锚点实核（锁出处行 → 现行正本）

| 判据 | 锁出处 | 现行该行含 |
|---|---|---|
| BEQR-150 | beqr:233 | `终端十一端` |
| BEQR-153 | beqr:236 | `终端开关优先级` |
| BSM-001 | bsm:505 | `整体说明：这套系统到底管什么` |
| BSM-002 | bsm:526 | `一笔业务怎么走` |
| BSM-003 | bsm:563 | `系统通用能力：数据可改可删、操作有引导、权限分岗` |
| BSM-012 | bsm:611 | `酒吧平板/工作手机` |
| BSM-013 | bsm:616 | `触摸屏一体机` |
| BSM-026 | bsm:686 | `各类入口` |
| BSM-029 | bsm:714 | `和谁有关` |
| BSM-044 | bsm:929 | `酒吧平板/工作手机：平板点酒，工作手机核销` |
| BSM-045 | bsm:933 | `主要功能` |
| BSM-046 | bsm:953 | `平板图形点单（POS）` |
| BSM-047 | bsm:963 | `和谁有关` |
| BSM-048 | bsm:978 | `触摸屏一体机：权益领取、现场服务和辅助售票` |
| BSM-049 | bsm:982 | `主要功能` |
| BSM-050 | bsm:996 | `和谁有关` |
| BSM-114 | bsm:1827 | `各类入口：不同人只看自己该看的、只做自己该做的` |
| BSM-115 | bsm:1897 | `相互关系：为什么这些子系统必须连在一起` |
| BSM-120 | bsm:1957 | `已确认规则和实施前输入项` |
| PGM-001 | pgm:256 | `整体定位：平台治理层不是一个业务部门，而是所有业务的底座` |
| PGM-010 | pgm:372 | `终端开关` |
| PGM-032 | pgm:572 | `员工和手机：人、私人手机、工作手机要能对应起来` |
| PGM-033 | pgm:576 | `主要功能` |
| PGM-034 | pgm:586 | `工作手机和企业微信` |
| PGM-035 | pgm:599 | `提成发放和人员服务治理` |
| PGM-036 | pgm:611 | `设备管理：谁领了什么设备，设备能不能用` |
| PGM-037 | pgm:615 | `主要功能` |
| PGM-038 | pgm:624 | `和现场业务的关系` |
| PGM-045 | pgm:712 | `终端开关：哪些端能打开，后台说了算` |
| PGM-046 | pgm:716 | `能控制哪些端` |
| PGM-047 | pgm:728 | `怎么控制` |
| PGM-066 | pgm:951 | `已拍板规则和待排期问题`（含「界面全中文」） |

另核 BSM L1445（U01 定稿引用、本段模型 `AttendancePunchRequest.method` 相关背景，**不**把充电发明算进本段 diff）：现行含「工作手机被拿离充电设施这个动作本身也算领取」。

## 2. 4 文件覆盖表（全文已读；行=`\n` 计数 = splitlines）

副本根：`unpublished-03acdb98/`。清单四条目 sha256/行/字节与独立 `sha256sum`/`wc` **4/4 match**。副本 `git hash-object` 与树 `e28dad05:<path>` blob **4/4 相同**。

| # | 路径 | 行 | 字节 | SHA256 | 全文 |
|---|---|---:|---:|---|---|
| 1 | `apps/android-terminal/src/main/kotlin/com/complex/terminal/DeviceRequestHeaders.kt` | 29 | 1048 | `f98cbc15aaa3faad2dd7b9e59d7e5f025fb9a2f29afdf59eaf02e66daa25508b` | 已读 L1–L29 |
| 2 | `apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt` | 338 | 14671 | `eb929512867a25ee3517d39ee806cbe25913461e91d2b3c5674088b2d7e2a6df` | 已读 L1–L338 |
| 3 | `apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt` | 777 | 21435 | `548ca991be1481fc23a907c7b6a374082fe8cfa3a7c1ce1c44b4e36a4835de9c` | 已读 L1–L777（结构段：健康/登录/绑定 L1–L230；游乐/酒吧/凭证 L232–L441；注册/BYOD/POS/排班考勤/服务点单 L442–L777，无抽样） |
| 4 | `apps/android-terminal/src/test/kotlin/com/complex/terminal/DeviceRequestHeadersTest.kt` | 36 | 1602 | `78fe72eaec7a3423ed203cd407b026a4e35a1598a0730f45fa01f4c64aeb216e` | 已读 L1–L36 |

合计 1180 行 / 38756 字节。

`android-app/app/build.gradle.kts`（不在本段 4 文件、只读调用链）：`kotlin.srcDir("../../src/main/kotlin")`，**排除** JVM `TerminalApiClient.kt` / `P0TerminalSmoke.kt` / `P0Fixture.kt`。故 **`DeviceRequestHeaders.kt` 与 `TerminalModels.kt` 编进 APK**；本段 JVM `TerminalApiClient.kt` 只走 JVM 冒烟/夹具/`TerminalWorkflow` 的 JVM 测试路径。android-app 另有同包 HttpURLConnection 版客户端（U01），`applyHeaders` 与本段 JVM 版同逻辑并调用本段 `deviceRequestHeaders`。

### 2.1 相对发布基线的逐文件 diff stat（真实改动）

命令：`git diff --numstat 242f487233d028173cb9198eecd079f5d877405c e28dad059b2c7e08bcc95a8db18552d3f76edf4b -- <path>`

| 路径 | + | − | shortstat |
|---|---:|---:|---|
| DeviceRequestHeaders.kt | 29 | 0 | 新文件 |
| TerminalApiClient.kt | 27 | 6 | 版本注释、deviceId/signer、registerDeviceSigningKey、自助注册两方法、applyHeaders 改签名并附加类型头/设备头 |
| TerminalModels.kt | 26 | 0 | 版本注释；注册请求加 merchantId/phone/idDoc；新增 Company/SelfRegistration/Receipt；Profile 加 employmentStatus/phone/companyId；ShiftRow 加 reviewId/publicationStatus |
| DeviceRequestHeadersTest.kt | 36 | 0 | 新文件 |
| **本段合计** | **118** | **6** | 4 files changed |
| **全 76 文件** | **4745** | **1084** | 与提示一致 |

blob：

| 路径 | 基线 git blob | HEAD(main) git blob | 未发布树/副本 blob |
|---|---|---|---|
| DeviceRequestHeaders.kt | （无此路径） | （无此路径） | `a9d088e3ac22fa80ec64812862223b636ea2c70b` |
| TerminalApiClient.kt | `db95efd951592066f6719bb94a4860735ba273e7` | **同基线** | `006a5a4046eeadbd8a2f2c6781c9e177c722221d` |
| TerminalModels.kt | `363b1fdcf41869d3282dc517da74d3fc19db2bb9` | **同基线** | `c227ce68452385d0f0ea7e5ab14d7f1d5fa5a00c` |
| DeviceRequestHeadersTest.kt | （无此路径） | （无此路径） | `5fa7b73f0b8514f9ac5a00b05b80e76aed602725` |

`git diff 242f4872 HEAD -- <本段四路径>` 为空（两已有文件）+ 两路径在 HEAD 不存在：**当前 main 这两份已有文件与发布基线逐字节相同**；M009–M012 **未改**本段同名文件。未发布相对 main 的 diff 即上表。对抗式固定问题见 §6 / GR-U02-004。

本段相对基线的实质新增（不只版本注释）：

- DeviceRequestHeaders：canonical = `deviceId\ntimestamp\nnonce\nMETHOD\npath(去query)\nsha256hex(body)`，输出 `X-Device-*` 四头
- TerminalApiClient：`deviceId`/`deviceSigner`、`registerDeviceSigningKey`、`listStaffRegistrationCompanies`/`submitStaffSelfRegistration`、`applyHeaders` 公开注册两路 `X-Terminal-Type=app`、其余恒 `staff_work_phone`、非登录/非公开注册且已设 deviceId+signer 时附加设备头
- TerminalModels：自助注册/公司列表/回执 DTO；代提交注册加手机/证件/商户；档案加在职状态；班次加审批字段
- DeviceRequestHeadersTest：固定时间戳/nonce、query 剥离、改 body 验签失败

## 3. 数据流（本段 → 未发布其它段 → main 对照 → SQL/HTML）

```
[JVM] TerminalApiClient.applyHeaders
        ├─ X-Terminal-Type = app | staff_work_phone     ← 本段 L298-300
        └─ deviceRequestHeaders(...)                    ← 本段新文件
              android-app TerminalApiClient.send() 同调  ← U01（APK 实际 HTTP）
                    DeviceKeystore.sign                 ← U01
[APK 共享源]
  DeviceRequestHeaders.kt + TerminalModels.kt           ← 本段（编进 APK）
  TerminalWorkflow / InMemoryLocalStore                 ← 未在本段 4 文件
[后端 未发布副本 只读]
  DeviceCredentialVerifier
        canonical = deviceId\nts\nnonce\nMETHOD\nrequestURI\nHexFormat(sha256(body))
        登记类型压过自报；DEVICE_BOUND={staff_work_phone,tablet,kiosk}
        工作手机验签公钥 = deviceSigningKeyStore（terminal_device_signing_keys）
  TerminalIdentityHolder.trustedTerminalType            ← 无派生时回落自报头
  ServiceOrderController.requireCompanyTerminal         ← 资金闸读派生/回落
  StaffRegistrationService.selfRegister + SecurityConfig 放行两公开路径
  M010RegistrationHttpTests 自报 X-Terminal-Type=app
[后端 main 只读]
  DeviceCredentialVerifier canonical 公式相同
        工作手机验签公钥 = staffRepository.findActiveCredential（员工 Keystore）
        ≠ 未发布的 deviceSigningKeyStore
  无 /api/v1/staff/self-registrations
  无 StaffSelfRegistrationRequest
  ServiceOrderController 同 trustedTerminalType 回落
  TerminalCanaryService.TERMINAL_TYPES 含 staff_private_phone 与 app（main 与未发布同文）
[SQL]
  本段无迁移。设备签名表在 main 已有；自助注册/证件加密/充电 CHECK 属其它 U 段
[HTML]
  BEQR-150 十一端；BSM-044/114 三设备不同用途；PGM-033 安装绑定；PGM-047 按设备类型下发
```

## 4. 三线审计

### 4.1 第一性原理（每文件三问）

#### DeviceRequestHeaders.kt（L1–L29）

1. **为何必须存在**：PGM-001「设备是否合规」、后端 `DeviceCredentialVerifier` 已要求写请求对 canonical 签名。删掉则 U01 android-app `deviceRequestHeaders` 符号无法解析（U01-009 已记跨段），enforce 无法翻开。有上位。不是假设抽象。
2. **声称是否执行**：注释「与后端 DeviceCredentialVerifier 使用同一份请求正文、路径与毫秒时间戳」。代码：SHA-256 hex、METHOD 大写、`path.substringBefore('?')`、毫秒时间戳、UUID nonce。本席 Java `HexFormat` 对照 `"%02x".format(byte)`：**与后端一致**（测试向量 digest=`5209d89f…0790bb`，buggy/good/backend 三串相等）。`require(deviceId.isNotBlank())` 文案写「工作手机设备编号」——共享函数也被平板路径调用（观察）。
3. **未验证前提**：signer 由调用方注入（U01 Keystore / 测试 EC 256）。时间窗/nonce 防重放在后端拦截器，本文件不实现。无 Android 仪器测试。

#### TerminalApiClient.kt（L1–L338）

1. **为何必须存在**：JVM 现场/夹具客户端；HTTPS API 是场内核销/点酒/注册的下位通道。有上位。厨房水槽式方法清单本身不是「无需求」，但 **类型头写死** 使同一客户端无法按十一端放权（GR-U02-001）。
2. **声称是否执行**：https/loopback/`ALLOW_INSECURE_TERMINAL_HTTP` 在 `init` 强制（基线已有）。新方法路径字符串与未发布 `StaffController` `/self-registrations`、`TerminalDeviceController` `/signing-key` 一致。`applyHeaders` **确实**按路径二分类型头并有条件附加设备头——声称「staff_private_phone 终端契约」（L133）**未**执行，实际发 `app`。
3. **未验证前提**：main **无**自助注册端点，第一次对当前 main 调用 `submitStaffSelfRegistration` 必 404（GR-U02-004）。无 deviceId 时仍发 `staff_work_phone`，资金闸在无派生身份时信自报头（GR-U02-001）。本文件不在 APK 内，但 android-app 同逻辑依赖本段 headers 文件。

#### TerminalModels.kt（L1–L777）

1. **为何必须存在**：序列化契约是客户端与 OpenAPI/后端的共享词表。删掉则无法编请求。有上位。新增自助注册 DTO 对应 PGM-033「两条注册路径」的下位字段，**但缺少设备公钥/指纹**，安装绑定无处安放（GR-U02-002）。
2. **声称是否执行**：全局 `JsonNamingStrategy.SnakeCase` + `explicitNulls=false`。`StaffSelfRegistrationRequest` 字段 = 未发布 Java record `(idempotencyKey, companyId, merchantId, name, phone, idDoc)` 可对应。`StaffRegistrationReceipt` 仅 `id`+`registrationStatus`，与 M010「回执不含 phone/user_id」方向一致。`decodeNdefTextPayload`（L761–777）不在本段 diff，main 既有。
3. **未验证前提**：`AttendancePunchRequest.method` 为自由 String（基线已有）；本段 diff **未**新增 charging 枚举，不把 U01 充电项算进本段发明。`employmentStatus`/`reviewId`/`publicationStatus` 对 main 解码因 `ignoreUnknownKeys` 可空，属下位细化。

#### DeviceRequestHeadersTest.kt（L1–L36）

1. **为何必须存在**：GEN-003 要求关键分支可失败断言。本文件覆盖 canonical 与改 body。删掉则新 helper 零测试。
2. **声称是否执行**：隔离 `gradle --offline test --tests com.complex.terminal.DeviceRequestHeadersTest`：**1 test, 0 failure, 0 error**（XML `timestamp=2026-09-09T18:58:07.176Z`）。断言 `X-Device-Id`/`Timestamp`、原 body 可验、改 qr→nfc 不可验。**未**断言 `X-Device-Nonce`、空 deviceId、`applyHeaders` 类型头、登录跳过、自助注册 DTO。
3. **未验证前提**：测试用与实现相同的 `"%02x".format` 重建 digest；若该格式与后端不一致会假绿。本席用 `java.util.HexFormat` 独立对照，**本次向量一致**，假绿前提不成立。仍不覆盖 HTTP 头策略。

### 4.2 顺从式（50 条逐项）

| ID | 本段适用面 | 结论 |
|---|---|---|
| GEN-001 | headers 有验签上位；ApiClient/Models 有现场端上位 | headers/模型有必要。类型头写死、自助注册 DTO 无设备标识 → **FAIL** 001/002 |
| GEN-002 | 声称与后端同一 canonical；自助注册可提交；设备头可附加 | canonical 与未发布/main 公式一致（命令）。自助注册对 **main** 404。类型头使资金闸误放行。**FAIL** 001/004 |
| GEN-003 | 新测试 1 例；applyHeaders/自助注册 DTO/signing-key Map 无测试 | 该 1 例可失败且已跑绿。关键新分支未覆盖 → **FAIL** 003 |
| GEN-004 | 设备签名、body digest、公开注册跳过签名 | 签名公式对。无 deviceId 时仍自报工作手机，信任边界靠可伪造头 → **FAIL** 001 |
| GEN-005 | 现场资金必须公司工作手机；默认拒绝 | 客户端把私人手机/平板请求标成工作手机；后端无设备号时回落自报。**FAIL** 001 |
| GEN-006 | 注册/绑钥幂等键 | `registerDeviceSigningKey` 带 idempotency_key；自助注册带 idempotencyKey。本段无部分写。通过 |
| GEN-007 | 每请求新 nonce/时间戳 | UUID+currentTimeMillis。重试换 nonce。首次成功不撞。通过（极端双开收窄观察） |
| GEN-008 | 类型码、canonical、DTO 与后端/OpenAPI | canonical 一致。`app`≠`staff_private_phone`（OpenAPI/Canary 二者分立）。工作手机验签密钥源未发布≠main。**FAIL** 001/004 |
| GEN-009 | require 中文；ApiException 英文 | require 中文。`ApiException` `"returned $statusCode"` 为 main 既有、本 diff 未改。观察 |
| GEN-010 | idDoc/phone 明文 JSON；Keystore 不在本段 | 注册必要字段，走 HTTPS。Receipt 不含证件。不升。PII 存后端其它段 |
| GEN-011 | 设备头供后端留痕 | 客户端不展示留痕。无实现面于「可点击溯源」全链。不单独开 finding |
| GEN-012 | 超时 10s/30s | 基线已有，相称。通过 |
| GEN-013 | 本段无 gradle/CI 文件 | **无实现面**。隔离测试借用未发布树模块 `build.gradle.kts`（不在 76 清单） |
| GEN-014 | 本段无 SQL | **无实现面**。跨段依赖见 004 |
| GEN-015 | 展示全中文 | 本段几乎无 UI。require「工作手机设备编号」用于共享函数。观察。ApiException 英文归 main 既有 |
| GEN-016 | 注释 staff_private_phone vs 代码 app；注释与验签器同一 canonical | canonical 注释成立。staff_private_phone 注释不成立，并入 001 |
| GEN-017 | 分类纪律 | 类型头/缺设备字段走冲突；测试缺口/分叉走技术性。未把 HTML 沉默当删除。无添加项 |
| GEN-018 | 本报告依据 | 类型 1 HTML 逐字 + 类型 4 命令。未用席位意见替代证据 |
| BEQR-150 | 十一端分端 | 客户端用 `app`/`staff_work_phone`，不是十一端中文名；Canary 另有 `staff_private_phone`。**FAIL** 001 |
| BEQR-153 | 开关优先级与兜底关闭 | 客户端不拉开关。类型头错误会使后端按错误端执行开关。并入 001。拉取失败兜底无客户端实现面 |
| BSM-001 | 基础盘子 vs 经营动作 | 本段是现场端契约，不是综合体盘子页。有经营动作上位。不因无拓扑页升冲突 |
| BSM-002 | 步骤 5 工作手机核销、平板点酒 | 方法都在，但类型头不区分设备。并入 001 |
| BSM-003 | 锁摘录为 h2+导语 | 本段无 UI 分岗。不把未映射存档删除子条当本段冲突 |
| BSM-012 | 平板只做点酒；工作手机负责核销 | 同一客户端恒报工作手机。**FAIL** 001 |
| BSM-013 | 一体机卡片 | 本段有检修 API，无一体机身份码 `kiosk`。类型头不会发 kiosk。并入 001（错报工作手机） |
| BSM-026 | 各类入口由后台统一开关 | 无客户端拉开关。并入 001（对象端错则开关对象错） |
| BSM-029 | 权限要知道设备绑定/私人手机/工作手机 | 绑定/signing-key API 存在；自助注册契约无设备标识 **FAIL** 002 |
| BSM-044 | 三类设备不同用途；现场动作只在工作手机 | **FAIL** 001。PGM-047 允许同一 App 按设备放权，本段类型头未放权 |
| BSM-045 | 安装绑定标识、换机、破解、平板绑酒吧、支付码、核销 | 安装绑定在自助注册 DTO 缺字段 **FAIL** 002。换机/破解 API 在本文件有方法（`approveStaffDeviceRebind`/`reportStaffDeviceIntegrity`），本段 diff 未改，调用在 U01。支付码不在本段 UI |
| BSM-046 | 平板 POS | 本段有目录/下单/结账 DTO 与 API。类型头仍报工作手机。并入 001。平面图是 U01 UI |
| BSM-047 | 平板只点酒支付；工作手机只核销 | **FAIL** 001 |
| BSM-048/049/050 | 一体机权益/售票/检修 | 检修 API 在。缺售票不升冲突（本段不是一体机模块全部）。身份码并入 001 |
| BSM-114 | 入口表：工作手机/私人手机/平板各做什么 | 私人手机行禁止代客下单/收款码/履约/核销，客户端仍报工作手机。**FAIL** 001 |
| BSM-115 | 平板连酒吧须校验低消；工作手机连留痕 | 低消在 DTO `minSpendFillRequired`/`minConsumptionCent` 有字段，校验在后端。留痕走后端。本段无冲突 |
| BSM-120 | 已拍板私人手机绑定/破解/工作手机管控/全中文 | 绑定 **FAIL** 002；全中文本段无新增英文 UI。管控/MDM 无实现面 |
| PGM-001 | 归属/资格/责任；设备是否合规 | 设备头服务资格。类型头伪造资格并入 001 |
| PGM-010 | 终端开关卡片含工作手机/平板/私人手机 | 客户端不实现开关配置。类型头错误并入 001 |
| PGM-032 | 人、私人手机、工作手机对应 | 自助注册无设备标识 **FAIL** 002；类型头抹平三种设备并入 001 |
| PGM-033 | 两条注册路径；安装绑定；换机；破解 | 自助注册+代提交 DTO 有。安装绑定 **FAIL** 002。换机/破解方法在，本 diff 未改调用 |
| PGM-034 | 工作手机必须企业微信 | 本段无企微 SDK。**无实现面**（不升冲突） |
| PGM-035 | 提成月结、不混营销打卡 | 薪资 DTO 在。本段 diff 未改考勤 method 闭集。通过 |
| PGM-036/037 | 设备台账/领还 | bind/return/signing-key API 存在。通过（POS 自造编号属 U01） |
| PGM-038 | 工作手机核销；平板只点酒；一体机检修 | **FAIL** 001 |
| PGM-045/046 | 各端能关 | 无客户端拉开关。并入 001 |
| PGM-047 | 关后菜单不可见+接口拒绝；同一 App 按设备类型下发 | 类型头恒工作手机 → 放权无法发生。**FAIL** 001 |
| PGM-066 | 界面全中文（安卓端） | 本段无新增用户可见英文。ApiException 英文 main 既有。观察 |

### 4.3 对抗式（grilling 自问自答，不派子代理）

Frontier 已结算，不向操作员发问：

1. **发布这四文件会不会覆盖 main 上 M009–M012 已修项？** 两已有文件 main blob = 基线 blob；两新文件 main 无。**不会**覆盖拓扑/迁移/网页那批已修文件的内容。固定问题的真实形态见 GR-U02-004：行为打到仍停留在「无自助注册 + 工作手机验签走员工凭证」的 main 后端上，且把 main 上「JVM 客户端不发类型头 → 资金闸缺头拒绝」改成「恒发工作手机」。
2. **第一次 `createServiceOrder`（未设 deviceId）X-Terminal-Type 是什么？** `staff_work_phone`。`trustedTerminalType` 无派生时回落该值。`CREATE_AND_CODE_TERMINALS` 含该值 → 放行。私人手机第一次代客下单即冒充工作手机。首次正常会。
3. **第一次 `submitStaffSelfRegistration` 发哪一端？** `app`。注释写 `staff_private_phone`。Canary/OpenAPI 二者分立。未发布 HTTP 测试也用 `app`。对当前 main：路径不存在，404。首次对未发布全树会发错端码；对 main 不可执行。
4. **自助注册 DTO 能否带上安装时设备标识？** 不能。无 `devicePublicKey`/`deviceFingerprintHash`。`bindStaffDeviceCredential` 需要 `staffProfileId`，注册当时还没有档案。第一次自助注册无法绑定。首次正常会。
5. **已绑定平板 deviceId 但仍走本客户端默认头，验签器如何？** 自报 `staff_work_phone` 且属于 DEVICE_BOUND，与登记 `tablet` 不等 → `type_mismatch` 拒。U01 POS 当前不写 deviceId，故第一次 POS 是「不签+冒充工作手机」而非 type_mismatch。若只修 U01-006 不修类型头，第一次平板签名请求会撞这条。
6. **canonical 与后端是否真同一？** 未发布与 main `tryVerify` 均为 `deviceId\nts\nnonce\nMETHOD\nrequestURI\nsha256hex`。无 context-path。GET 带 query：客户端去 query，servlet `getRequestURI` 不含 query。Java HexFormat 与 `"%02x".format(byte)` 对测试向量一致。公式层首次会通过。
7. **工作手机验签用哪把公钥？** 未发布：`deviceSigningKeyStore`（即 `registerDeviceSigningKey`）。main：员工 `findActiveCredential`。只登记终端签名钥、尚未绑员工凭证时，main grace 当无凭证放行，enforce 拒。首次对 main grace 不硬失败；enforce 会。
8. **DeviceRequestHeadersTest 会不会在实现与后端不一致时仍绿？** 它用实现同款 hex 重建。本席另用 HexFormat 对照，本次一致。测试仍不覆盖类型头。
9. **空字符串 deviceId？** `id != null` 对 `""` 为真，进入 `require` 抛「工作手机设备编号不能为空」。调用方通常用 null。非首次必撞。观察。
10. **极端：并发双开绑定、context-path 非空、Map 序列化 signing-key。** context-path 本仓未配（首次不会）。Map `post(mapOf(public_key,…))` 未测，kotlinx Map 序列化通常保持键名；要两三件不巧才坏。铁律 6 不另造闸。
11. **commit 03acdb98 不在对象库是否影响 diff？** 不影响。改动按树 `e28dad05` 对基线计算，与提示一致。

ponytail-review（只列不改）：

- `TerminalApiClient.kt:300`：写死 `"staff_work_phone"`。删该常量，改为调用方传入一次登记类型。
- `L133` 注释 `staff_private_phone`：与代码 `app` 重复撒谎，删注释或改码，留一处。
- `DeviceRequestHeaders.kt:18`：「工作手机设备编号」改为「设备编号」。
- `applyHeaders` 公开路径两处字符串：可收成集合，非必须。
- `net: 约 -4 行且去掉错误业务发明。`

## 5. Findings（临时 ID，正式 ID 仅 B2 分配）

### GR-U02-001

- **标题**：JVM 共享客户端除两条公开注册路径自报 `app` 外，所有请求 `X-Terminal-Type` 恒为 `staff_work_phone`；注释声称的 `staff_private_phone` 未发送
- **文件:行号**：`TerminalApiClient.kt:133`（注释）；`TerminalApiClient.kt:298-305`（`applyHeaders`）；对照 APK 副本 `android-app/.../TerminalApiClient.kt:277,288`（U01，同逻辑，不在本段 4 文件）；未发布与 main `TerminalIdentityHolder.java:48-54`（无派生回落自报）；`ServiceOrderController.java:110-113`（资金闸读 `trustedTerminalType`）；main/未发布 `TerminalCanaryService.java:36-40`（`app` 与 `staff_private_phone` 分立）
- **完整原文**：`builder.header("X-Terminal-Type", if (publicRegistration) "app" else "staff_work_phone")`；注释 `staff_private_phone 终端契约`
- **业务影响**：同根 U01-001（台账已预告 JVM 同逻辑）。私人手机/平板第一次走核销、代客下单、收款码即被标成公司工作手机；现场资金闸在无 `X-Device-Id` 时信自报头。自助注册标成客人端 `app` 而非员工私人手机。BEQR-150 十一端、BSM-044/114「私人手机不允许现场收银」、PGM-047「按设备类型下发」在第一次调用即被打破。相对 **main 基线客户端不发该头**，本材料把「缺头 → 资金闸拒绝」改成「恒报工作手机 → 放行」。
- **可复现命令/最小反例**（类型 1+4）：

```text
# 副本源码固定值（不连网）
path=/api/v1/services/orders            -> staff_work_phone
path=/api/v1/bar/orders                 -> staff_work_phone
path=/api/v1/amusement/maintenance-records -> staff_work_phone
path=/api/v1/staff/self-registrations   -> app
path=/api/v1/staff/registration-companies -> app
# main/未发布 TerminalCanaryService 同时含 app 与 staff_private_phone
# main ServiceOrderController.CREATE_AND_CODE_TERMINALS
#   = {staff_work_phone, web, tablet, app}  → 自报 staff_work_phone 放行
# requireCompanyTerminal("staff_private_phone") 在后端测试中被拒 —— 正确端码反而过不了，所以客户端改报工作手机
```

- **冻结判据**：GEN-004、GEN-005、GEN-008、GEN-016、BEQR-150、BSM-012、BSM-044、BSM-047、BSM-114、PGM-038、PGM-047
- **2.3 依据类型**：1+4
- **四分类**：**冲突**
- **工程缺陷**：是
- **建议入队 / 发布前须修**：是
- **最小处置（给发布者）**：类型头只声明一次、等于绑定/登记的设备类型（`staff_work_phone` / `tablet` / `staff_private_phone` 或 Canary 闭集中的私人手机码），不按路径改报、不自报 `app`。现场资金接口只信 verifier 派生身份（U07 同 PR）。自助注册走私人手机码。APK 内 HttpURLConnection 版（U01）须同步，禁止只改 JVM 文件。

### GR-U02-002

- **标题**：自助注册请求模型没有设备公钥/指纹字段，安装时绑定专属设备标识在契约层无法发生
- **文件:行号**：`TerminalModels.kt:458-466`（`StaffSelfRegistrationRequest`）；`TerminalApiClient.kt:136-137`（`submitStaffSelfRegistration`）；对照 `TerminalModels.kt:394-400`（`StaffDeviceBindRequest` 需要 `staffProfileId`，注册当时不存在）
- **完整原文**：`data class StaffSelfRegistrationRequest(val idempotencyKey: String, val companyId: String, val name: String, val merchantId: String? = null, val phone: String? = null, val idDoc: String? = null)`
- **业务影响**：PGM-033/BSM-045/BSM-120「安装时生成并绑定该手机专属的设备标识，把手机和员工绑定；不读取硬件识别码」。U01-007 是 UI 未调 Keystore；本项是 **即便 UI 要送也没有字段**。第一次自助注册只能提交姓名/公司/手机/证件。换机/破解复核没有注册当下的对象。`bindStaffDeviceCredential` 要等审批后才有 `staffProfileId`，不能替代「安装时」。
- **可复现命令/最小反例**（类型 1+4）：`grep -n devicePublicKey\|deviceFingerprint TerminalModels.kt` 在 `StaffSelfRegistrationRequest` 为 0；未发布 Java record 同样六字段、无设备标识。
- **冻结判据**：PGM-033、BSM-045、BSM-120、PGM-032、GEN-002
- **2.3 依据类型**：1
- **四分类**：**冲突**
- **工程缺陷**：是
- **建议入队 / 发布前须修**：是
- **最小处置**：自助注册请求增加公钥/指纹（或等价设备标识）并在审批落档时绑定；禁止要求调用方先有 `staffProfileId`。与 U01-007 同一 PR。HTML 已有明文，不走 2.2。

### GR-U02-003

- **标题**：本段新增的类型头分流、公开路径跳过签名、自助注册 DTO 与 signing-key Map 编码没有可失败测试；现有测试只覆盖 canonical helper
- **文件:行号**：`DeviceRequestHeadersTest.kt:15-35`（唯一新测试）；`TerminalApiClient.kt:298-305`；`TerminalModels.kt:458-469`；现有 `TerminalRequestSerializationTest`（不在本段 4 文件）8 例均不编码 `StaffSelfRegistrationRequest`
- **完整原文**：测试名 `signedBytesMatchServerCanonicalAndDetectBodyChange`；无 `applyHeaders`/`X-Terminal-Type`/`StaffSelfRegistrationRequest` 断言
- **业务影响**：001/002 这种首次就会发生的错误，本段测试套件第一次跑是绿的。GEN-003「测试通过本身不替代行为证据」。
- **可复现命令/最小反例**（类型 4）：

```text
# 隔离 /tmp/u02-grok-iso-2895769
gradle --offline --no-daemon test --tests com.complex.terminal.DeviceRequestHeadersTest
# XML: tests=1 failures=0 errors=0
# grep X-Terminal-Type apps/android-terminal/src/test → 仅 DeviceRequestHeadersTest 不含该字符串
gradle --offline --no-daemon test --tests com.complex.terminal.TerminalRequestSerializationTest
# 8 tests 0 failure；源码无 StaffSelfRegistrationRequest
```

- **冻结判据**：GEN-003、GEN-018
- **2.3 依据类型**：4
- **四分类**：**技术性**
- **无业务发明理由**：缺的是对已有 HTTP 头策略与 DTO 的断言，不新增业务规则或端种类。
- **工程缺陷**：是
- **建议入队 / 发布前须修**：是
- **最小处置**：为 `applyHeaders` 用假 HTTP 客户端断言：默认 `staff_work_phone`、两公开路径为私人手机码（修 001 后）、登录/公开路径无 `X-Device-*`、有 deviceId 时 canonical 与 verifier 一致；为 `StaffSelfRegistrationRequest` 补 snake_case 与设备字段（修 002 后）序列化测试。

### GR-U02-004

- **标题**：以旧基线 242f4872 分叉的客户端打到当前 main：自助注册 404、工作手机验签密钥源不一致、且会改写 main 上「不发类型头」的 fail-closed
- **文件:行号**：本段 `TerminalApiClient.kt:56-57,134-137,298-305`；main `StaffController` **无** `self-registrations`；main `DeviceCredentialVerifier.java:235-250`（工作手机用员工凭证公钥）；未发布同文件 `resolveSigningKey` 在 owner 检查后 **一律** `deviceSigningKeyStore.findActivePublicKey`；main `ServiceOrderController` 缺头拒绝
- **完整原文**：未发布 `return deviceSigningKeyStore.findActivePublicKey(deviceId)...`；main `return staffRepository.findActiveCredential(profile.id()).map(...devicePublicKey)`
- **业务影响**：这四条路径 **不是** M009–M012 已修文件，整树覆盖不会直接毁掉那些 diff。但单独把本段合进 main：（1）`submitStaffSelfRegistration` 404；（2）JVM/APK 开始恒发工作手机头，打开原先缺头即拒的资金闸；（3）`registerDeviceSigningKey` 写入的钥不是 main 工作手机验签读取的钥，enforce 下工作手机写请求 `credential_required`/`bad_signature`。U01-009 已记「只发 U01 不带本段 headers 则 APK 编译不过」的对称面。
- **可复现命令/最小反例**（类型 4）：

```text
git diff --numstat 242f4872 HEAD -- <本段四路径>   # 空（已有文件）
ls backend/.../dto/StaffSelfRegistrationRequest.java  # main 无
grep self-registrations backend/.../StaffController.java  # main 无
# 未发布 vs main resolveSigningKey 见 §3
```

- **冻结判据**：GEN-002、GEN-008、GEN-013
- **2.3 依据类型**：4
- **四分类**：**技术性**
- **无业务发明理由**：是发布基线与 main 已演进后端之间的合并/密钥查找差异，没有新的经营规则。
- **工程缺陷**：是
- **建议入队 / 发布前须修**：是
- **最小处置**：以 **当前 main** 为基线重放本段 diff，不得 `e28dad05` 整树覆盖。自助注册与 U05 后端同 PR；工作手机验签与 U07 verifier 密钥源对齐（main 员工凭证 **或** 明确改 main 验签器，禁止客户端与后端各认一把钥）；类型头按 001 修后再合。迁移编号冲突属其它段。

## 6. 与 main 分叉对照（对抗式固定问题）

| 项 | 事实 |
|---|---|
| 本段同名文件是否被 M009–M012 改过 | **否**。`TerminalApiClient.kt`/`TerminalModels.kt` 的 HEAD blob = 基线 blob |
| 发布未发布树是否覆盖那些已修文件 | **不覆盖那些路径**。会 **新增** headers/测试，并 **改写** 上述两份仍等于基线的 main 文件 |
| 行为冲突 | 见 GR-U02-004：自助注册 404；类型头打开 fail-closed；工作手机验签钥源不一致 |
| APK 编译依赖 | android-app 排除 JVM ApiClient 但 **包含** `DeviceRequestHeaders.kt`。只发 U01 不发本段 headers → 编译失败（U01-009） |
| 不得把副本冒称已发布修复 | 本段是未发布树 e28dad05 的只读输入 |

## 7. 适用冻结判据逐项结论摘要

冲突性 FAIL：GEN-001/002/004/005/008/016，BEQR-150，BSM-012/044/047/114，PGM-032/033/038/047。  
技术性 FAIL：GEN-003/013（跨段构建），GEN-002/008 的分叉面。  
通过或无实现面：GEN-006/007/010/011/012/014/015/017/018（分类纪律本席已执行），BSM-001/003/013/026/029（部分并入 001/002）、BSM-046/048–050/115，BEQR-153（无客户端实现面，对象端错误并入 001），PGM-001/010/034/035/036/037/045/046/066。  
无添加项。无 HTML 人闸。

## 8. 观察（无新根因；不升 finding）

1. `DeviceRequestHeaders` require 文案「工作手机设备编号不能为空」用于共享函数；平板也会走到。首次正常路径 deviceId 非空，不抛。建议随 001 改文案，不单独入队。
2. `ApiException` 英文 `"returned $statusCode"`：main 既有、本 diff 未改。归 M376 后续，发布前须修「否」。
3. `"%02x".format(Byte)` 与 `HexFormat` 对测试向量一致（`/tmp/HexFmt.java`）。不构成缺陷。
4. `AttendancePunchRequest.method` 自由 String 为基线已有；充电领取不在本段 diff（U01-002/009）。
5. `employmentStatus`/`reviewId`/`publicationStatus`/`merchantId`/`phone`/`idDoc` 为 HTML 注册/审批/在职状态的下位字段，**细化**，不升添加。
6. `decodeNdefTextPayload` 不在 diff。
7. 空 context-path；GET query 剥离与 `getRequestURI` 一致。首次不会因公式失败。
8. `deviceId=""` 走 require：调用方用 null。铁律 6 收窄。
9. JVM `TerminalApiClient` 不进 APK；缺陷 001 仍须修，因 android-app 同逻辑 + 本文件是契约源 + 合进 main 会改变 JVM 夹具/冒烟行为。
10. 未发布 `M010RegistrationHttpTests` 亦发 `app`：副本内自洽，不能反证 HTML/OpenAPI 十一端。不把测试绿当业务正确。
11. `TerminalRequestSerializationTest` 8/8 绿，不覆盖新 DTO，不能当 002 的反证。
12. 本仓无 `03acdb98` commit 对象，不影响按树 diff。

## 9. 命令证据（只读）

```text
sha256sum docs/audit-2026-09/plan/criteria-lock.md
# b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

git rev-parse HEAD origin/main
# 445a49f409f0cfbe6dd616ae079be1299824c48f ×2

git cat-file -t 03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb
# fatal: could not get object info

git cat-file -t e28dad059b2c7e08bcc95a8db18552d3f76edf4b
# tree

git diff --shortstat 242f4872 e28dad05
# 76 files changed, 4745 insertions(+), 1084 deletions(-)

git diff --numstat 242f4872 e28dad05 -- <本段四路径>
# 29 0 DeviceRequestHeaders.kt
# 27 6 TerminalApiClient.kt
# 26 0 TerminalModels.kt
# 36 0 DeviceRequestHeadersTest.kt

# 隔离（不改副本/main，不装依赖，--offline）
ISO=/tmp/u02-grok-iso-2895769/android-terminal
gradle --offline --no-daemon test --tests com.complex.terminal.DeviceRequestHeadersTest
# XML tests=1 failures=0 errors=0 case=signedBytesMatchServerCanonicalAndDetectBodyChange

gradle --offline --no-daemon test --tests com.complex.terminal.TerminalRequestSerializationTest
# tests=8 failures=0 errors=0

# /tmp/HexFmt.java ：format_byte=9f；buggy==good==backend==5209d89f…0790bb
```

未跑 Android APK 构建（需 SDK，不装依赖）；未跑后端测试（会碰库）。未跑的不写通过。

## 10. 审计结论

**FAIL**

理由：存在未定稿工程缺陷 GR-U02-001（冲突，否决级）、GR-U02-002（冲突）、GR-U02-003（测试缺口）、GR-U02-004（与 main 分叉）。无未答 questions、无添加项 2.2、无 HTML 人闸。不存在有条件通过。材料未发布，本轮不修源；处置写给发布者。B2 分配正式 ID `U02-00N`。同根 U01-001/007/009 可对照，不得把本段 JVM 文件的新类型头当成「main 既有」而不修——main 该文件 **不** 发送 `X-Terminal-Type`。

## U02 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 43997 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok（固定外审席乙，独立）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：160-audit-prompt.md V0.17；runbook V0.02 §7/§11；criteria-lock V0.11；modules.md V0.04 附录「未发布材料第 2 段：安卓共享请求与模型」
- 正本：`/home/james/complex-management`。`HEAD` = `origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`。本席本轮 `git rev-parse HEAD:<path>` 与 `242f4872:<path>`：`TerminalApiClient.kt`/`TerminalModels.kt` 两对全等；`DeviceRequestHeaders.kt` 与其测试在 HEAD 不存在。
- 审计对象仍是只读输入副本 `.scratch/module-recheck-20260908/unpublished-03acdb98`（树 `e28dad05`，基线 `242f4872`），不是 main。三份 HTML 取 main 正本。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B2 核实结论。U01 定稿 U01-001/007/009 只作同根对照，本段依据仍为 HTML/命令/本段源码。
- 本席 B2-r1：**PASS**（001–005 逐 ID 全 PASS；无添加项、不发 2.2；发布前须修「是」=001/002/003/004，「否」=005）
- 本追加段不写独占锁哈希字段行。正文实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar 文件正文同值；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。映射行 `M376 → [BEQR-150 … PGM-066]` COUNT=50 unique=50 MISSING=[]。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。必要核对只读；未重跑隔离 gradle（席甲 B1 `/tmp/u-overlay` 与本席 B1 `/tmp/u02-grok-iso-2895769` 绿仅作构建旁证，不替代 U02-003）。

此票只判断 findings.md V1.46 顶部「当前 U02 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、「发布前须修」是/否与最小处置。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。材料未发布，定稿后状态应记 REVIEWED-FAIL（有已定稿缺陷，处置写给发布者），不做 B3。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/U02-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 43997 | `185da0389050d201b50b74123a26babeae016bfa7016d203005754ebddcba6c0` |
| `docs/audit-2026-09/modules/U02-b1-self-raw.md` | V0.01 | 24246 | `9f5e5aa5ad8fc5f11d201dd6ddd1559fd3cf706aaa1b41e8e1f7c6bb19900bad` |
| `docs/audit-2026-09/findings.md` | V1.46 | 552872 | `653bcd6505d2b7b027785db9edd171da933ee1eb1d16831a83457e18cd913710` |
| `docs/audit-2026-09/ledger.md`（只读） | V2.47 | 461071 | `30618964c96e518ebc85129d75dfb24638712cb0f51d7b749adb878cf8ea8aae` |

候选形态：V1.46 顶部 U02-001…005；无 questions、无 HTML 修改申请。U02 ledger 行 IN_PROGRESS。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers | 先核技能；用户/提示词覆盖技能本体 |
| receiving-code-review | 先核 HTML/diff/blob/grep 再接受或反驳席甲归并；禁止表演性同意；B2 不修复 |
| grilling | 只借追问结构自查：001 同根是否成立、fail-closed 是否属本材料引入、002 是否发明注册生命周期、003 升格是否 GEN-003、004 三点是否均有命令、005 是否吞 §8 第 10 条；不向操作员发问 |
| verification-before-completion | 声称前先跑 sha256/git rev-parse/diff/grep/sed；本票不声称本轮新跑 gradle 绿 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流（四文件→U01 APK 客户端→副本 verifier/StaffController→main 对照→HTML） |
| ponytail-review | 只评最小删除/收窄，不落地。001 不造新终端种类；002 不要求先有 staffProfileId；003 不测服务端时间窗；004 不整树覆盖 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| resolving-merge-conflicts | 本节点无合并 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立 SKILL**；未冒充调用 |
| Arc audit | 会派子代理；与双席冲突；**未用** |

### 完整差集（GR-001…004、§8 1–12、席甲 CX-001、OBS-1…6）

漏并 finding：无。两席 B1 全部临时 finding 仍落在 001–004。添加项 0。本席 GR-001…004 原意未改写。席甲独有 CX-001 与 GR-001 同根并入 001，核源后接受。席甲 OBS-1/3/4/5/6 与本席 §8 并入 005 登记。

席甲 OBS-2 原文是「DeviceRequestHeadersTest 不覆盖时间窗/nonce 重放，属服务端」，**不是** GR-003。候选来源行写成「GR-003 + 席甲 OBS-2（B2 升为技术性）」把两条不同观察粘在一起——升格成立的是 GR-003（applyHeaders/DTO 关键分支无断言），不是把服务端重放测进客户端。OBS-2 应留在 005 登记。此为定稿措辞，不改 003/005 票。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/须修 | 本席票 |
|---|---|---|---|
| U02-001 | CX-U02-001 + GR-U02-001（同根 U01-001；fail-closed 面来自 GR-001） | 冲突/是/是 | **PASS（冲突/是/是）** |
| U02-002 | GR-U02-002（席甲 B1 未单列；契约面对照 U01-007） | 冲突/是/是 | **PASS（冲突/是/是）** |
| U02-003 | GR-U02-003（席甲 B1 低估为 GEN-003 通过；OBS-2 不是本项本体） | 技术性/是/是 | **PASS（技术性/是/是）** |
| U02-004 | GR-U02-004 + 席甲 §4 | 技术性（与 main 分叉）/是/是 | **PASS（技术性/是/是）** |
| U02-005 | 双方观察（含本席 §8 1–12 与席甲 OBS-1/3/4/5/6；OBS-2 应补回登记） | 登记/否/否 | **PASS（登记/否/否）** |

冲突入队 2（001、002）。技术性入队 2（003、004）。登记观察 1（005）。添加 0。

发布前须修「是」=001、002、003、004（4）；「否」=005（1）。与候选汇总一致。

### 重点 (a) U02-001 同根、fail-closed、与 U01-001 同一处置 — **PASS**

同根成立。CX-001 与 GR-001 都是 `TerminalApiClient.kt:298-307` `applyHeaders`：两条公开注册路径发 `app`，其余恒 `staff_work_phone`；`:133` 注释写 `staff_private_phone` 未发送。U01-001 是 APK `android-app/.../TerminalApiClient.kt:277` 同逻辑 + 工作手机 UI 入口；本段是 JVM 共享客户端同一身份规则。对照 U01 定稿不替代本段 HTML/命令。

fail-closed 面写入事实栏成立，且是本材料相对基线的增量，不得按「main 既有」放过：

- 基线/main JVM `applyHeaders(builder)`（HEAD=`242f4872` blob `db95efd9…`）只设 `authorization`，**不发** `X-Terminal-Type`。
- 未发布改为 `applyHeaders(builder, path, method, body)` 恒发类型头。
- main `ServiceOrderController:36-38/:110-113`：`trustedTerminalType` 无派生时回落自报头；空白则 `TERMINAL_NOT_ALLOWED`「缺少 X-Terminal-Type 标识」。`CREATE_AND_CODE_TERMINALS` 含 `staff_work_phone` → 本材料第一次代客下单/收款码被标成工作手机而放行。
- 这与 U01-001 的 APK 面不同：main APK 客户端基线已恒发 `staff_work_phone`（U01 定稿已记该行被本材料改写）；JVM 基线是缺头。事实栏分开写正确。

处置与 U01-001 **身份规则**同一，且最懒、不发明新端：

- HTML：PGM L578 / BSM L1433 公司自有员工在私人手机 App 注册；BSM L930 三设备不同用途；BEQR L233 / PGM L739 十一端把客人手机应用与员工私人手机分成两端；BSM-044/114 私人手机不做现场收银。
- `TerminalCanaryService.TERMINAL_TYPES` 已含 `staff_private_phone` 与 `staff_personal_app` 与 `app`。候选「闭集中的私人手机码」是从已有码取，不新造码、不规定必须哪一个同义词。
- 「类型头按绑定/登记设备类型只声明一次、不按路径改报、不自报 app；自助注册走私人手机码；资金接口只信 verifier 派生身份（U07 同 PR）；JVM 与 APK 两份同步」= GR-001 原文。两份是已有的 JVM 文件与 U01 HttpURLConnection 副本，不是第三端。
- 「与 U01-001 同一处置」**不含** U01-001 的「工作手机应用去掉自助注册入口」——那是 U01 UI，本段无界面。本段保留自助注册 HTTP 方法、改身份声明即可。禁止：发明第四种终端、把自助注册改报 `web`、只改 JVM 不改 APK、用「恢复缺头」代替「声明真实登记类型 + 资金闸只信派生」。

分类冲突、工程缺陷是、发布前须修是。票 **PASS**。

### 重点 (b) U02-002 作为 U01-007 契约面 — **PASS（成立，不发明业务）**

`TerminalModels.kt:459-466` `StaffSelfRegistrationRequest` 六字段：idempotencyKey/companyId/name/merchantId/phone/idDoc。副本 Java record `StaffSelfRegistrationRequest.java` 同六字段。`StaffDeviceBindRequest`（`:394-400`）要 `staffProfileId`，注册当时不存在。HTML PGM L579 / BSM L935「员工安装私人手机 App 时生成并绑定一个该手机专属的设备标识……不读取手机本机的硬件识别码」。

U01-007 是 UI 函数体无 DeviceKeystore；本项是即便要送也没有字段。单列契约面成立，不与 001 抢同一根（001=端身份，002=安装绑定标识）。冲突：HTML 明文要求在契约层缺失，不是添加。

处置「增加公钥/指纹（或等价标识）字段、审批落档时绑定、不要求先有 staffProfileId、与 U01-007 同 PR」= GR-002 原文。提交字段发生在安装/自助注册当时（生成），档案尚无时不能绑 `staffProfileId`，审批落档是员工记录第一次存在的时刻——这是 HTML「安装时生成并绑定」的最懒映射，不发明新注册产品、不把手填他人档案做成新产品。HTML 已有原文，不发 2.2。

定稿措辞（不改票）：候选写「U05 后端」过窄。Java record 在第 6 段（U06 `dto/StaffSelfRegistrationRequest.java`），审批/Controller 在第 5 段（U05）。发布者应按 U05+U06（外加 U01-007）同一 PR，漏 U06 则 Kotlin/Java 契约再次分叉。

票 **PASS**。

### 重点 (c) U02-003 升格 GEN-003 与最小处置 — **PASS（升格合规，处置最小）**

GEN-003：「测试须实际执行目标行为、具备能失败的断言，并覆盖本模块关键分支；测试通过本身不替代行为证据。」

本段相对基线的关键新分支：`applyHeaders` 类型头分流、公开路径/登录跳过 `X-Device-*`、`StaffSelfRegistrationRequest` / `registerDeviceSigningKey` Map 编码。本段唯一新测试 `DeviceRequestHeadersTest.kt:15-35` 只锁规范串布局、方法大写、query 剔除、改 body 不可验。副本 `apps/android-terminal/src/test` 无 `X-Terminal-Type`/`StaffSelfRegistrationRequest`/`applyHeaders` 字符串。隔离 gradle 全绿不替代。席甲 B1 把 GEN-003 标「通过（登记）」低估了关键分支；升 GR-003 为技术性入队合规。无业务发明：缺的是对已有 HTTP 头策略与 DTO 的断言。

处置最小：假 HTTP 客户端断言 `applyHeaders`（有/无 deviceId、公开路径无设备头、有 deviceId 时四头与 verifier 规范串一致）+ 新 DTO snake_case 序列化（修 001/002 后按新契约）。不要求仪器测试、不要求客户端测服务端 300s 时间窗/nonce 去重（那是席甲 OBS-2，属 U07）。

定稿措辞（不改票）：（1）来源行不要写成「OBS-2 升为技术性」；（2）假客户端断言必须按修 001 后「只声明一次登记类型」来写，禁止把当前路径二分（`app` vs `staff_work_phone`）锁进测试。

票 **PASS**。

### 重点 (d) U02-004 三点证据与最懒处置 — **PASS**

三点均有本轮独立证据：

1. **main 404**：`grep self-registrations` 于 main `StaffController.java` 为空；副本 `:51` `@PostMapping("/self-registrations")`。main 无该 Java record。第一次 `submitStaffSelfRegistration` 打当前 main 必 404。
2. **钥源分叉**：main `DeviceCredentialVerifier.resolveSigningKey` 对工作手机 `return staffRepository.findActiveCredential(profile.id()).map(...devicePublicKey)`；副本同函数在 owner 检查后 **一律** `return deviceSigningKeyStore.findActivePublicKey(deviceId)`。`registerDeviceSigningKey` 写入的设备级钥不是 main 工作手机验签读取的员工凭证钥 → enforce 下 `credential_required`/`bad_signature`。
3. **缺头拒绝被打开**：见 (a)。APK 编译依赖本段新增 `DeviceRequestHeaders.kt`（main 无）为 U01-009 对称面，本轮 `HEAD:DeviceRequestHeaders.kt` 不存在，属实。

两已有文件 HEAD blob = 基线 blob；M009–M012 未改本段路径。行为冲突是打到仍停留在「无自助注册 + 工作手机验签走员工凭证 + JVM 缺头即拒」的 main 上，不是覆盖治理页。

处置最懒：以当前 main 为基线重放本段 diff；U01/U02/U05/U07 同一 PR（自助注册路由在 U05、验签器在 U07、APK 在 U01）；客户端与后端只认同一把钥（不在本段选定「员工凭证或设备钥」，禁止各认一把）；不得 `e28dad05` 整树覆盖。不发明新经营规则。U06 DTO 随 002 同发即可，004 不强制单列 U06。

票 **PASS**。

### 重点 (e) U02-005 观察是否写错本席 §8 — **PASS（第 10 条已点名，未降 finding）**

005 列席甲 OBS-1/3/4/5/6，并以「Grok §8 1–12（require 文案、`ApiException` 英文 main 既有归 M376、`%02x` 与 HexFormat 一致、新增字段为 HTML 下位细化、JVM 客户端不进 APK 但为契约源、副本内 `M010RegistrationHttpTests` 发 app 不能反证十一端等）」收本席观察。第 10 条原文「副本内自洽，不能反证 HTML/OpenAPI 十一端。不把测试绿当业务正确」被点名，未写成反证、未升格入队。

「等」覆盖 §8 其余：充电 method 不在本段 diff、`decodeNdefTextPayload` 不在 diff、空 context-path 与 GET query 剥离、`deviceId=""` 走 require、`TerminalRequestSerializationTest` 8/8 不能反证 002、仓内无 `03acdb98` commit 对象。未把本席任何 finding 降成观察。

定稿措辞（不改票）：把席甲 OBS-2（时间窗/nonce 属服务端，客户端测试不覆盖不构成 GEN-003 入队）补回 005 登记。005 分类登记、工程缺陷否、发布前须修否。

票 **PASS**。

### 001 独立裁定（冲突／须修是）

见 (a)。票 **PASS**。

### 002 独立裁定（冲突／须修是）

见 (b)。票 **PASS**。

### 003 独立裁定（技术性／须修是）

见 (c)。票 **PASS**。

### 004 独立裁定（技术性／须修是）

见 (d)。票 **PASS**。

### 005 独立裁定（登记／须修否）

见 (e)。票 **PASS**。

### 定稿措辞修正（非阻断，无需 B2-r2）

1. 003 来源行改为「GR-U02-003（席甲 B1 低估 GEN-003；B2 升技术性）」；席甲 OBS-2 留在 005。
2. 002「U05 后端」改为「U05 审批/Controller + U06 Java record」（或「员工自助注册后端」），避免漏 DTO。
3. 001「与 U01-001 同一处置」限定为身份声明规则，不含 U01 UI 去入口。
4. 003 测试断言按修 001 后契约，禁止锁路径二分。
5. 005 补 OBS-2 登记一句即可；§8 不必逐条抄全，不得写成「Grok 无观察」或把第 10 条改成缺陷入队。

### 本批结论

**PASS**

逐 ID：001 PASS，002 PASS，003 PASS，004 PASS，005 PASS。发布前须修是=001/002/003/004，否=005。无添加、无 questions、无 HTML 修改申请、无 LOCK-AMEND。合取尚待席甲本批 B2；本席票单独不能定稿。材料未发布，定稿后 U02 → REVIEWED-FAIL，处置写给发布者，不做 B3/B4/B5。

CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

# M003 B1 Grok 独立审计报告

- 报告自身版本：V0.01
- 控制版本：V0.17
- 模块：M003 — 01-root-config-and-guidance-01（`plan/modules.md`：8 文件 / 有效内容 625 / `NORMAL`）
- 生命周期（派发）：M003/322 `IN_PROGRESS`，B3=0
- 席位：Grok（乙），修复前全量独立审计；不修复、不改方案、不写 questions、不派子代理
- 映射判据：`criteria-lock.md` `M003 → [GEN-001 … GEN-018]`（本席自读；无 HTML 派生 ID）
- B1 结论：**FAIL**

---

## 0. §6 技能自核（本席实际环境）

本席读取并按收窄口径使用的技能：

| 技能 | 路径核验 | 本次用法 |
|---|---|---|
| `using-superpowers` | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能路由 |
| `using-arc` | Arc `skills/using-arc/SKILL.md` | 仅路由；**未**调用 `arc:audit`（会写 §7 外产物） |
| `grilling` / `grill-me` | mattpocock `productivity/grilling`、`productivity/grill-me` | 对抗式/第一性原理追问结构；事实自读；**不派子代理、不等操作员** |
| `ponytail-review` | ponytail `4.9.0/skills/ponytail-review` | 找可删内容；不改文件 |
| `codebase-design` | mattpocock `engineering/codebase-design` | 评估 AGENTS/CONTEXT 作为 agent 接口是否过浅/漂移 |
| `diagnosing-bugs` | mattpocock `engineering/diagnosing-bugs` | 用可复现命令建反馈环；**不加** instrumentation |
| `systematic-debugging` | superpowers | 只借读错误→复现→追数据流；**不改生产代码** |
| `verification-before-completion` | superpowers | PASS/FAIL 前核对命令输出 |
| `writing-for-agents` | mattpocock `productivity/writing-for-agents` | 评估 `AGENTS.md` 指针与完成标准 |
| `i-have-adhd` | `i-have-adhd/0.1.0/skills/i-have-adhd` | 输出形状；本报告第一行仍按闸 1 独占哈希 |

存在但本次节点不适用、未调用：`brainstorming`、`writing-plans`（阶段 A）；`test-driven-development` / `tdd` / `ponytail`（B3）；`requesting-code-review` / `receiving-code-review`（B4）；`resolving-merge-conflicts`（无合并）；`git-guardrails-claude-code`（存在于 `mattpocock-skills/.../misc/git-guardrails-claude-code`，B1 无 git 写操作）。

缺口（如实披露，不冒充已调用）：

- `batch-grill-me`：**不存在**
- `ubiquitous-language`：**不存在**

§6 划掉项未调用。子代理：**未派**。

本席**未读取** `M003-b1-self-raw.md`、`findings.md`、其他模块 findings、未允许审计历史结论。检索时曾被全仓 `rg` 命中 self-raw 路径名；**不采信其正文**。下列证据均来自本席对纳入文件、三份 HTML、直系调用方源码与本席命令输出。

未执行：全量 `mvn test`、`verify:predeploy`、`verify:delivery-gates`、`npx playwright test --list`、`spring-boot:run`、真实数据库写入、部署、提交、推送。文档/配置证据用类型 1/4；不把未跑整仓系统冒充已验证。

---

## 1. 输入 SHA、字节、行数

命令：

```bash
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
git ls-files -z -- .env.example .gitignore .semgrepignore AGENTS.md CONTEXT.md PROGRESS.md README.en.md README.md | xargs -0 cat | wc -l
wc -l / wc -c / sha256sum  <8 files>
```

| 项 | 值 |
|---|---|
| 本席 `criteria-lock.md` SHA256 | `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de` |
| sidecar `criteria-lock.sha256` | 同值（本席 `cat` 核对，非派发期望值） |
| 官方有效行数 `git ls-files -z \| xargs -0 cat \| wc -l` | **625**（与 `modules.md` 一致） |
| 8 文件合计字节 `wc -c` | **37655** |
| HEAD | `main` `df580198172b2c497ef3cb7e25d2e8e5dc058920` = `origin/main` |

| 文件 | 行 | 字节 | SHA256 |
|---|---:|---:|---|
| `.env.example` | 28 | 1119 | `47961c4187e46b4c07124a1c3b2b9b32a9b726383bc9f679dac8926c746df461` |
| `.gitignore` | 55 | 1199 | `75861ded5d3859bcda593aafe3f3862e765470e7f8b6636a643bf59de9d7c51b` |
| `.semgrepignore` | 6 | 253 | `45f3dbc9f36891b4b94d2b641fabfd66db81df59b642bcde80a29bece24f1aba` |
| `AGENTS.md` | 73 | 3658 | `92a67041b99c8bace7e99f4cc3e1326304159062495b70255e0e48e995ed7f52` |
| `CONTEXT.md` | 163 | 9782 | `6f1b5586714074ef6592971073054109f65fb24c7aae15660bbfed11db7a5d6f` |
| `PROGRESS.md` | 33 | 3121 | `d895d8c1181c6bfe4287a2825433a49ca40351ee6d86fd9152fcfd69398ad9b6` |
| `README.en.md` | 119 | 7738 | `7fb858437ae57bb81788c7f7263a65c0a9c5210423866b08db6220b33a53b8fb` |
| `README.md` | 148 | 10785 | `01b02ecfacd4e4a84ba8260a3e07526fc185b079ed152052a827c1f9a9e4654b` |
| **合计** | **625** | **37655** | |

行数加总：28+55+6+73+163+33+119+148=625。全行覆盖：每文件 1–末行均进入三线，无抽样冒充整文件。

映射：`criteria-lock.md:6383` `M003 → [GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018]`。

---

## 2. 全行覆盖表

| 文件 | 行范围 | 覆盖 | 2.1 主分类（再按项拆） |
|---|---|---|---|
| `.env.example` | 1–28 | 是 | 技术性（环境模板）；死变量/注释与接线不一致见 GR-15 |
| `.gitignore` | 1–55 | 是 | 技术性 |
| `.semgrepignore` | 1–6 | 是 | 技术性；`ops/keyvault/` 排除理由见 GR-14 |
| `AGENTS.md` | 1–73 | 是 | 技术性（工程铁律）+ 细化（HTML 终权/中文）+ 冲突（删除审批，GR-07） |
| `CONTEXT.md` | 1–163 | 是 | 细化（多数词条）+ 冲突（GR-01..06）+ 添加（GR-17/18） |
| `PROGRESS.md` | 1–33 | 是 | 技术性工作日志；完成声明缺陷 GR-13 |
| `README.en.md` | 1–119 | 是 | 技术性入口镜像；过期数字/树 GR-08 |
| `README.md` | 1–148 | 是 | 技术性入口；路径/数字缺陷 GR-09/10/11 |

未把引用文件扩成新审计模块。直系调用方只用于举证：`package.json`、`backend/pom.xml`、`apps/web/package.json`、`application.yml`、`AuthService.java`、`ProductionAuthService.java`、Flyway 目录、`docs/openapi/openapi-p0.json`、三份 HTML 原文。

---

## 3. 第一性原理（每文件三问）

### 3.1 `.env.example`（L1–28）

- **为何必须存在**：给本地/联调提供可复制的环境变量名。删掉它，三份 HTML 无一条做不到；走 2.1 → **技术性**（构建/运行配置，无业务发明）。
- **声称是否执行验证**：未启动后端。类型 4：`application.yml` 接线了 `COMPLEX_API_*` / `COMPLEX_DATABASE_*` / `COMPLEX_KEYVAULT_PII_KEY` / `P0_ADMIN_*`（经 `AuthService` `@Value("${complex.smoke.admin.username:${P0_ADMIN_USERNAME:}}")`）。`JWT_SECRET`、`CORS_ALLOWED_ORIGINS`、`ACCESS_TOKEN_TTL_SECONDS`、`USER_ACCESS_TOKEN_TTL_SECONDS`、`AUTH_FAILURE_LOCK_THRESHOLD` 在 `backend/src` **零命中**；TTL/锁阈值实际 `@Value` 为 `complex.auth.access-token-ttl:PT30M`、`complex.auth.failed-login.max-attempts:5`。见 GR-15。
- **未验证前提**：复制本文件能否独立把 Spring 拉起来；空串 `COMPLEX_KEYVAULT_PII_KEY=""` 是否覆盖 yaml 默认。未跑 Spring 占位符实验，不把启动成功写成事实。

### 3.2 `.gitignore`（L1–55）

- **为何必须存在**：阻止密钥、构建物、运行态目录入库。删掉会把 `.env`/`target`/`node_modules` 暴露给 git。HTML 无对应条 → **技术性**。
- **声称是否验证**：`git check-ignore -v .env .env.local node_modules dist backend/target` 分别命中 L4/L5/L1/L2/L3。`.scratch` **未被忽略**（`git check-ignore` 无输出）——与阶段 A「`.scratch` 不在 gitignore」一致，不另定罪。
- **未验证前提**：未对每个 glob 做一次误忽略/漏忽略全仓扫描（会扩到未来模块）。只核本文件声明的根模式。

### 3.3 `.semgrepignore`（L1–6）

- **为何必须存在**：跳过非源码或解析失败路径，避免扫描门禁假红。HTML 无条 → **技术性**。
- **声称是否验证**：`docs/openapi/openapi-p0.yaml`/`json`、`docs/_archive/`、`ops/keyvault/` 均存在。对 `ops/keyvault/` 本席跑 `semgrep --config p/bash --quiet --error ops/keyvault/` → **exit 0**，未复现「bash 解析器报错」。见 GR-14。
- **未验证前提**：未跑仓库 CI 所用的完整 semgrep 规则集（会扫未来模块）。`p/bash` 成功不能证明历史上某规则集曾报错；**当前**以本席命令为准，注释不是依据（GEN-018）。

### 3.4 `AGENTS.md`（L1–73）

- **为何必须存在**：给代理的操作铁律。删掉后 HTML 仍在；铁律 5–8 承载 HTML 终权/软删/中文/禁止存量兼容。工程段（worktree/禁直推/迁移 V 号/测试库）→ 技术性。
- **声称是否验证（类型 4）**：
  - `docs/agents/issue-tracker.md` / `triage-labels.md` / `domain.md` 存在。
  - `git remote -v` → `https://github.com/jxh1979/mall.git`。
  - `ops/160/propose-change.sh`、`merge-pr.sh` 存在；`.git/hooks/pre-push` 存在且拒绝直推 `refs/heads/main`。
  - `package.json`：`verify:migration-version-order` 在 `verify:delivery-gates` 字符串内；`verify:predeploy` **不含** `migration-version-order`（与铁律 9「未挂 predeploy」一致）。
- **未验证前提 / 冲突**：铁律 6 把删除审批写成 `role_platform_admin`，并把唯一例外写成 PIPL 人脸照。HTML 原文是「具备最高权限的人」，且「平台管理员与最高权限是两个不同的角色」「删除不因平台管理员身份自动获得」。见 GR-07。

### 3.5 `CONTEXT.md`（L1–163）

- **为何必须存在**：词表。L3–8 声明三份 HTML 最终解释权。删掉后 HTML 仍在，但代理会失去压缩词表——用途是技术性的，**内容**必须与 HTML 一致（GEN-017/008）。
- **声称是否验证**：对照三份 HTML 原文（类型 1）。多数业态主体名、商户边界、成长值/积分分立、待追回、四类主体、临时授权、高危五类、外包/审计、终端开关优先级、操作留痕、软删除「只隐藏不抹除」可指向上位 HTML → **细化**。
- **未验证前提**：L12「用户批准重构 PRD、HTML 旧条款在替代清单获批前保持原文」——本席未审那三份 PRD（未来/其他模块）。在 HTML 仍有效时，本文与 HTML 明文矛盾的句子仍按 **冲突**，不因「另有 PRD」免责。

### 3.6 `PROGRESS.md`（L1–33）

- **为何必须存在**：单功能（退租结算落台账）分支工作日志。删掉它 HTML 无一条做不到。当前仓库 `HEAD` 已是 `main`，文件名/标题仍写 `feat/termination-settlement`。走 2.1 → **技术性**日志，不是业务发明源。
- **声称是否验证**：所列 Java/TS 路径存在；`FinanceService.SOURCE_TYPES` 含 `termination_penalty` / `deposit_refund`。`TerminationSettlementMathTests.java` 有 **8** 个 `@Test`，不是文中「纯单测 7」。未跑测试/`verify:predeploy`，不得采信「32 绿 / 15 绿 / 全链绿」。见 GR-13。
- **未验证前提**：测试绿、已 push 的历史过程。类型 4 只证明文件在当前树，不证明当时命令输出。

### 3.7 `README.en.md`（L1–119）

- **为何必须存在**：L3 自称中文 README 的英文镜像。HTML 无条 → **技术性**。
- **声称是否验证**：L3「All commands and numbers are evidence-backed」。实跑 Flyway **V385 / 363 个**、OpenAPI JSON **720** operations、`backend/target/surefire-reports` **不存在**。本文仍写 V124 / 386 ops / 1251 tests / 42 E2E（6 specs），目录树写 `Flyway V001..V124`。`tests/e2e` 当前至少 10 个 `*.spec.ts`。见 GR-08。
- **未验证前提**：未跑 `npx playwright test --list`（避免整仓 E2E 列表被当成系统验证）。规格文件计数已足以否定「6 specs」快照。

### 3.8 `README.md`（L1–148）

- **为何必须存在**：仓库第一入口。HTML 无「必须有 README」；内容中的域名/单综合体起步是 HTML 细化。技术栈/脚本是技术性。
- **声称是否验证（类型 4）**：
  - Java 21、Spring Boot 4.1.0、springdoc 3.0.3、React 19、Vite 8、antd 5、TS 5.7、Vitest 4、Playwright `^1.61.0`、`engines.node >=22`：与 `backend/pom.xml` / `apps/web/package.json` / 根 `package.json` 一致。
  - §4 所列 npm 脚本名均在根 `package.json`。
  - 基线表 Flyway **V385（363）**、OpenAPI **720**：与目录/`openapi-p0.json` 一致。
  - `database/migrations/`：**整个 `database/` 不存在**（GR-09）。
  - 「现有 surefire 报告」**862 tests**：surefire 目录不存在（GR-10）。
  - 「tools/ 79 个 Node 校验/生成脚本」：`tools/` 顶层 80 项 = **78 `.mjs` + 1 `.json` + 1 目录**（GR-11）。
  - `@GuardedAction` / `AdmissionGuard` 类文件存在；未执行写路径。
- **未验证前提**：未执行任何 `npm run verify:*` / `mvn test`。脚本**存在**已证；脚本**通过**未证。862 测试数未证。

---

## 4. 顺从式：18GEN 逐项

判据全文来自 `criteria-lock.md` `## general`（本席读取 L12–100）。每条覆盖全部 8 文件。

### GEN-001 必要性与最小性 — **FAIL**（有缺陷项）

- `.gitignore` / `.semgrepignore` / `.env.example`（作为本地模板）/ `AGENTS.md` 工程段 / `README.md` 入口：有可验证用途。
- `README.en.md` 作为「跟踪中文 README」的镜像已不再跟踪（GR-08）→ 重复且过期，最小性失败。
- `PROGRESS.md` 根目录单功能已合并日志，对当前 `main` 无需求承载（GR-13，配合 GEN-016）。
- `CONTEXT.md` 词表有用途，但错误词条不是「该删整文件」，是冲突内容该改。

### GEN-002 可执行正确性 — **FAIL**

- README 声明的脚本名存在；**未执行**这些脚本，不声称它们现在绿。
- `.env.example` 声明的 TTL=900 / 锁阈值=3 / JWT_SECRET / CORS **不能**驱动 `AuthService`/`ProductionAuthService` 的实际 `@Value`（GR-15）。
- `PROGRESS.md`「完成 / 全链绿」未执行复现（GR-13）。

### GEN-003 测试有效性 — **N/A（本模块无测试文件）** / 文档侧走 GEN-016

本模块 8 文件不含测试。PROGRESS/README 的测试数字不是本模块测试，按 GEN-016 审声称。

### GEN-004 信任边界校验 — **无本模块可执行输入面缺陷**

配置模板不是运行时校验器。`.env.example` 把未接线变量写成可配输入，归 GEN-002/013，不另造输入校验判据。

### GEN-005 授权与最小权限 — **FAIL**（AGENTS 删除审批）

`AGENTS.md:50-51` 将删除批准主体写成 `role_platform_admin`。HTML：平台管理员不自动获得删除；删除由「具备最高权限的人」批准。见 GR-07。

`.env.example` P0 本地种子「平台管理员权限」是 local profile 技术种子，注释禁止生产 → 技术性，不升冲突。

### GEN-006 数据完整性 — **N/A**

无状态机/金额写入。PROGRESS 描述的幂等/台账在其他模块；本文件只登记、不把未跑库当成完整。

### GEN-007 并发与幂等 — **N/A**（文档声称不在本模块执行）

### GEN-008 接口与模式一致性 — **FAIL**

- `README.md` vs `README.en.md` 基线数字、目录树、章节不一致（GR-08）。
- `CONTEXT.md` 词表 vs HTML 终端/项目公司/零售参考价/内部结算价/存档审批不一致（GR-01..06）。
- `.env.example` 变量名 vs Java `@Value` 属性名不一致（GR-15）。

### GEN-009 错误处理与恢复 — **无独立缺陷**

文档/忽略文件无伪成功代码路径。PROGRESS 把未复现的绿写成完成，归 GEN-016。

### GEN-010 安全与敏感信息 — **FAIL**（keyvault 扫描排除理由不成立）

- `.env.example` 含 `admin-pass` / `complex_dev_pwd`：标明 local、勿用于生产；`ops/160/complex-deploy-api.sh` 对 prod 拒绝 `complex_dev_pwd`。登记为技术性示例，**不**因示例密码本身判添加。
- `.gitignore` 忽略 `.env`：正例。
- `.semgrepignore` 把 `ops/keyvault/`（密钥 provision 脚本）整目录排除，且「解析器报错」未被本席 `p/bash` 复现（GR-14）。

### GEN-011 可观测与审计证据 — **FAIL**（文档假完成）

README.en / README surefire / PROGRESS 把旧证据或缺失报告写成当前事实（GR-08/10/13）。

### GEN-012 资源与性能边界 — **N/A**

无循环/连接实现。`.env.example` 池大小 10/2 为技术默认，未执行负载。

### GEN-013 构建配置与依赖 — **FAIL**

技术栈声明与 pom/package.json 一致（README.md）。`.env.example` 死变量导致「按样例配置却改不到声称行为」（GR-15）。README.md `database/migrations` 路径不存在（GR-09）。

### GEN-014 数据库迁移正确性 — **N/A（本模块无迁移文件）**

README 迁移**数字**走 GEN-016。未跑 migrate。

### GEN-015 界面可用性与可访问性 — **N/A**

无产品 UI。`README.en.md` 是给国际开发者的镜像，不是后台/客人端/设备端展示文案；AGENTS 铁律 7 的适用面写的是那些端。不因英文 README 判展示文案冲突。

### GEN-016 文档可验证性 — **FAIL**

缺陷：GR-08、GR-09、GR-10、GR-11、GR-13、GR-14、GR-15。  
已核可复现的 README.md 技术栈与脚本名、中文基线 V385/720、AGENTS 远程与钩子、CONTEXT 指向的三份 HTML 与三份 PRD **路径存在**（PRD 内容不在本模块）。

### GEN-017 业务来源分类 — **FAIL**（有冲突与添加）

已按 冲突→细化→技术性→添加 执行。冲突 GR-01..07；添加 GR-17、GR-18。未因 HTML 沉默把细化/技术性打成缺陷。未把 HTML 未写的工程配置判添加。

### GEN-018 第一性原理证据 — **本席结论侧 PASS，对象侧 FAIL**

本席 findings 均落到类型 1 或 4。不接受「历史遗留」「注释说」「镜像应该同步」作为依据。对象文档自身多处把未执行输出写成当前事实 → 对象违反 GEN-016/018，记在对应 GR。

---

## 5. 对抗式（grilling 自问自答，不派子代理）

1. **若有人把 README.en 当当前基线** → 会按 V124/386/1251 规划容量。反例已成立：目录 V385、OpenAPI 720、无 surefire。  
2. **若代理按 CONTEXT「十类终端」实现开关** → 漏独立经销商 Web 门户，并可能做「客人 Web」。HTML 明文「终端十一端」「客人无网页端」。  
3. **若代理按 CONTEXT「同一自然人全国唯一」做合并** → 直接违反「不得声称已经识别为同一自然人」。  
4. **若代理按 CONTEXT「项目公司负责定价售票分账」授权城市项目公司** → 越过娱乐平台/游乐/传媒/中央餐饮的域主管边界。  
5. **若代理按 AGENTS 让 `role_platform_admin` 批删除** → 把日常平台管理员当成最高权限。HTML 二者分立，删除不随平台管理员自动获得。  
6. **若开发者 export `.env.example` 指望 JWT/TTL/锁阈值生效** → 运行时仍是 PT30M / max-attempts=5，样例 900 秒 / 3 次是死配置。  
7. **若扫描门禁信任 `.semgrepignore`「keyvault 无法解析」** → 密钥脚本被整目录跳过；本席 `p/bash` 能解析。  
8. **若把根目录 PROGRESS.md 当全仓进度** → 只看到已合并的退租结算切片，且绿数不可复现。  
9. **金额/退租 source_type** → 代码里确有 `termination_penalty`/`deposit_refund`；那是其他模块实现。本文件只描述，不在本模块发明枚举。不把 HTML 未列 source_type 写成 M003 添加（发明点不在这 8 个文件）。  
10. **ponytail-review 可删**：过期英文基线数字；根目录 PROGRESS 标题；`.env.example` 未接线变量；CONTEXT 的 `L4` 与「十类」。B1 不删，只登记。

---

## 6. 技术性「无业务发明」逐条登记（无缺陷者）

判定：无业务发明 + 无与 HTML 冲突。不要求每行 HTML 锚点。

| 项 | 位置 | 无业务发明理由 | 工程缺陷 |
|---|---|---|---|
| 忽略 `node_modules`/`dist`/`backend/target`/`coverage`/`.vite`/Gradle/Kotlin/`*.class`/`*.apk` | `.gitignore:1-21` | 构建产物 | 否 |
| 忽略 `.env` / `.env.local` | `.gitignore:4-5` | 密钥文件不入库 | 否 |
| 忽略 playwright 报告与 `test-results` | `.gitignore:23-26` | 测试产物 | 否 |
| 忽略本机 Claude settings、tsbuildinfo、semgrep 滚动日志、`.loop-engineering/`、根调试脚本 glob | `.gitignore:28-51` | 机器/临时产物 | 否 |
| 忽略运行态 `backend/data/bar-images/`、`evidence-files/` | `.gitignore:53-55` | 运行时生成文件 | 否 |
| 忽略 OpenAPI yaml/json、`docs/_archive/` | `.semgrepignore:2-4` | 非扫描源码；各有专门校验 | 否 |
| `SPRING_PROFILES_ACTIVE`/`COMPLEX_API_*`/`COMPLEX_DATABASE_*`/`COMPLEX_API_VERSION`/`BUILD_TIME` | `.env.example:1-12` | 进程/数据源/构建元数据 | 否 |
| `P0_ADMIN_*` 本地种子（注释禁止生产） | `.env.example:24-28` | local 自动种子；角色名来自 HTML 但本文件不发明规则 | 否 |
| `COMPLEX_KEYVAULT_PII_KEY` 变量名（接线存在） | `.env.example:20-22` | 加密密钥占位，非业务实体 | 否（空串语义并入 GR-15） |
| Issue tracker / triage / domain 指针 | `AGENTS.md:3-15` | 代理操作手册 | 否（目标文件存在） |
| 禁主工作区提交 / worktree / 禁直推 / fetch | `AGENTS.md:21-36` | 多会话工程约束 | 否（钩子与脚本存在） |
| 功能改动同步 PRD | `AGENTS.md:38` | 工程流程 | 否 |
| 三份 HTML 终权、skill 不得自改 | `AGENTS.md:42-48` | 细化 HTML 需求源，非发明 | 否 |
| 展示文字全中文（机读标识除外） | `AGENTS.md:53-54` | 细化运营展示口径 | 否 |
| 不为存量兼容/回填 | `AGENTS.md:56-57` | 工程约束 | 否 |
| 迁移 V 号串行、测试只 export 三 DB 变量、测试库 `complex_test`、`MAVEN_ARGS=-DskipTests` | `AGENTS.md:59-73` | 工程闸门；`complex_test` 与 `.env.example` 的 `complex` 分属测试 vs 本地，不冲突 | 否 |
| CONTEXT 声明 HTML 终权 | `CONTEXT.md:1-10` | 指向需求源 | 否 |
| CONTEXT「平台」顶层治理、非综合体 | `CONTEXT.md:14-19` | 细化 PGM「顶层平台」 | 否 |
| 业态专名：娱乐平台/中央餐饮/游乐/传媒；资产=城市项目公司兼任；酒吧是独立商户不是业态主体 | `CONTEXT.md:28-38`（不含 L4） | 细化 PGM 各主体表 | 否 |
| 商户边界 | `CONTEXT.md:40-45` | 细化 PGM 商户侧 | 否 |
| 会员免费、非付费订阅 | `CONTEXT.md:47-50` 中与 HTML「会员注册免费」一致的部分 | 细化 BSM 会员注册 | 否（「同一自然人」另见 GR-02） |
| 成长值只用于等级、不兑 | `CONTEXT.md:53-57` | 细化「成长值与可用积分是两套独立账」 | 否 |
| 积分全国一余额、不收银抵现 | `CONTEXT.md:59-63` | 细化积分跨项目复用/兑换虚拟券 | 否 |
| 待追回积分非负余额、清偿前禁兑 | `CONTEXT.md:65-69` | 细化 BEQR「有效消费与冻结」 | 否 |
| 四类主体绑定 | `CONTEXT.md:95-100` | 细化 BEQR「四类主体」 | 否 |
| 功能权限点 vs 职级 | `CONTEXT.md:102-107` | 细化 PGM 权限规则 | 否 |
| 临时授权有效期或单次、留痕 | `CONTEXT.md:109-114` | 细化 PGM「临时授权可设有效期或单次使用」 | 否 |
| 高危五类授出前审批、禁自审 | `CONTEXT.md:116-121` | 细化 PGM 高危授权审批 | 否 |
| 外包/审计人员边界 | `CONTEXT.md:123-135` | 细化 PGM 外包与审计 | 否 |
| 终端开关关闭后端拒绝、高层压倒、拉取失败写操作兜底关 | `CONTEXT.md:137-142` | 细化 PGM 终端开关（终端**枚举**另见 GR-01） | 否 |
| 操作留痕 | `CONTEXT.md:151-156` | 细化 PGM 留痕 | 否 |
| 删除只隐藏不抹除、申请人不得自审 | `CONTEXT.md:158-163` 中与 HTML 软删除一致的部分 | 细化 BSM 存档删除（「存档不需审批」另见 GR-06） | 否 |
| README 技术栈表（中文） | `README.md:11-25` | 构建依赖 | 否 |
| README §4 脚本名 | `README.md:53-94` | 入口命令存在于 package.json | 否（未跑通） |
| README 中文基线 V385/720 | `README.md:100-104` | 与目录/OpenAPI JSON 一致 | 否 |
| README E2E「以 playwright --list 为准、不写死」 | `README.md:106` | 避免假快照 | 否 |
| README 单综合体起步、域列举、开关化 mock | `README.md:7-9,144-148` | 细化 PGM「当前一个项目」/外部依赖，非发明 | 否 |
| README 读者路径所链 docs | `README.md:111-127` | 本席 `test -e` 均为 EXISTS | 否 |
| `GuardedAction`/`AdmissionGuard` 类存在 | README 架构段 | 技术分层描述；未跑请求 | 否 |
| PROGRESS 所列源码路径存在 | `PROGRESS.md:16-26` | 工作日志指向已存在文件 | 否（绿数另见 GR-13） |

---

## 7. 完整发现清单

分类顺序：冲突 → 细化（有缺陷才进修复）→ 技术性（有缺陷才进修复）→ 添加。  
依据类型：1=HTML 原文，4=命令/源码接线。冻结 ID 仅 general。

### GR-01

- **位置**：`CONTEXT.md:144-149`
- **分类**：冲突
- **冻结 ID**：GEN-017、GEN-008
- **依据类型**：1
- **工程缺陷**：是
- **证据**：CONTEXT「十类终端」= 客人 App、小程序、Web、工作人员平板、员工工作手机、员工私人手机、商户侧 Web 门户、商户侧 App、一体机、移动管理端。  
  HTML `business-enum-quick-reference-v0.1.html` `td.k`「终端十一端」原文：「客人手机应用、客人小程序、Web 前端（业务人员，客人无网页端）、工作人员平板、员工工作手机、员工私人手机、商户网页门户、商户手机应用、触摸屏一体机、移动管理端、独立经销商 Web 门户。」  
  PGM `h3`「能控制哪些端」含「独立经销商 Web 门户」；同节「受控的十一类终端」再次写「客人无网页端」。  
  CONTEXT 少第 11 端，且把「Web」与客人 App/小程序并列，与「客人无网页端」矛盾。

### GR-02

- **位置**：`CONTEXT.md:50`
- **分类**：冲突
- **冻结 ID**：GEN-017
- **依据类型**：1
- **工程缺陷**：是
- **证据**：CONTEXT「同一自然人全国唯一、跨城市和跨项目复用」。  
  HTML BEQR「会员唯一识别」原文：「以完成短信验证的手机号作为判断新会员的唯一标准。……不同手机号不得仅凭姓名、生日等弱信息自动合并，系统也不得声称已经识别为同一自然人。」

### GR-03

- **位置**：`CONTEXT.md:21-26`
- **分类**：冲突
- **冻结 ID**：GEN-017
- **依据类型**：1
- **工程缺陷**：是
- **证据**：CONTEXT 项目公司「对本地经营、定价、售票和分账负责」。  
  HTML PGM「各主体具体负责什么」：城市项目公司「不负责酒吧、游乐、演出等经营域的定价、售票和结算」；定价/售票/域内结算分属娱乐平台公司、游乐公司、传媒公司等域主管公司。

### GR-04

- **位置**：`CONTEXT.md:71-75`
- **分类**：冲突
- **冻结 ID**：GEN-017
- **依据类型**：1
- **工程缺陷**：是
- **证据**：CONTEXT 把零售参考价定义成「过去滚动 30 天的最低有效实际成交价」。  
  HTML BEQR「项目零售参考价」原文：窗口是「业务参数，按项目可配，出厂默认三十天」；「窗口内没有有效成交的，自动向前延长至找到最近一笔有效成交」。30 天不是定义本身，且缺「向前延长」。

### GR-05

- **位置**：`CONTEXT.md:77-81`
- **分类**：冲突
- **冻结 ID**：GEN-017
- **依据类型**：1
- **工程缺陷**：是
- **证据**：CONTEXT 将「内部结算价」收窄为「就兑换 SKU 约定、……计算商户补偿的项目级价格」。  
  HTML BEQR「项目内部结算价」：各业态商品/服务均设目录，酒吧/游乐/传媒/中央餐饮/城市项目公司分别审批，用于核销与结算，不限于积分兑换 SKU。

### GR-06

- **位置**：`CONTEXT.md:161`
- **分类**：冲突
- **冻结 ID**：GEN-017、GEN-005
- **依据类型**：1
- **工程缺陷**：是
- **证据**：CONTEXT「存档不需审批」。  
  HTML BSM `h3`「配置数据可以存档、也可以删除」：「逐条存档属日常操作，不用审批；但一次性存档超过后台配置条数的，或整店、整类存档的，须经审批后执行」。无条件「不需审批」与明文例外冲突。

### GR-07

- **位置**：`AGENTS.md:50-51`
- **分类**：冲突
- **冻结 ID**：GEN-017、GEN-005
- **依据类型**：1
- **工程缺陷**：是
- **证据**：AGENTS「删除一律软删除：…… + `role_platform_admin` 批准。唯一例外是 PIPL 约束下的人脸照。」  
  HTML BSM：「由**具备最高权限的人**审批」；「平台管理员与最高权限是两个不同的角色」；「删除……不因『平台管理员』这个身份自动获得」。  
  HTML 例外也不是「人脸照代替最高权限审批」：交易类只存档不删；人脸是「最短留存期限，到期删除」；U 盘最高权限本人发起可不经审批。AGENTS 把批准人写成 `role_platform_admin`、把唯一例外写成人脸照，与 HTML 明文矛盾。

### GR-08

- **位置**：`README.en.md:3,29,79-85,97`
- **分类**：技术性（镜像文档）+ 工程缺陷
- **冻结 ID**：GEN-016、GEN-008、GEN-001
- **依据类型**：4
- **工程缺陷**：是
- **无业务发明**：英文入口/镜像，不发明业务规则。
- **证据**：自称「English mirror of README.md」「numbers are evidence-backed」。当前中文 README 基线 V385/720；英文仍写 Latest Flyway **V124**、OpenAPI **386**、Backend tests **1251** passing、Frontend E2E **42 (6 specs)**，树与 mermaid 写 `V001..V124`。  
  本席：`migration` 363 个文件、max **V385**；`openapi-p0.json` operations **720**；`backend/target/surefire-reports` **不存在**；`tests/e2e` 规格文件多于 6。未跑 playwright `--list`。

### GR-09

- **位置**：`README.md:38-40`
- **分类**：技术性 + 工程缺陷
- **冻结 ID**：GEN-016、GEN-013
- **依据类型**：4
- **工程缺陷**：是
- **无业务发明**：目录说明。
- **证据**：声明 `database/migrations/`「历史/参考迁移脚本」。`ls database` → `cannot access 'database': No such file or directory`。

### GR-10

- **位置**：`README.md:105`
- **分类**：技术性 + 工程缺陷
- **冻结 ID**：GEN-016、GEN-011
- **依据类型**：4
- **工程缺陷**：是
- **无业务发明**：测试计数快照。
- **证据**：表写「**862** tests（现有 surefire 报告）」「`backend/target/surefire-reports`」。本席：该目录 **False**。脚注允许「无报告时保留上次快照」，但取证列仍写「现有 surefire 报告」，当前不可复现。未跑 `mvn test`。

### GR-11

- **位置**：`README.md:44`
- **分类**：技术性 + 工程缺陷
- **冻结 ID**：GEN-016
- **依据类型**：4
- **工程缺陷**：是
- **无业务发明**：工具计数。
- **证据**：「tools/ **79 个 Node** 校验/生成脚本」。`ls -1d tools/*` → 80 项：78 `mjs` + 1 `json` + 1 目录 `gate-hardening-tests`。Node 脚本数为 **78**，不是 79。

### GR-13

- **位置**：`PROGRESS.md:1,18,33`
- **分类**：技术性 + 工程缺陷
- **冻结 ID**：GEN-016、GEN-001、GEN-002
- **依据类型**：4
- **工程缺陷**：是
- **无业务发明**：实现工作日志；`termination_penalty` 等是对已存在代码的描述，发明点不在本文件。
- **证据**：标题「分支 feat/termination-settlement」，`HEAD` 为 `main`。L18「纯单测 7」vs 该测试类 **8** 个 `@Test`。L33「后端(单测32绿)+前端(vitest15绿+typecheck绿)+verify:predeploy全链绿。已 commit+push。」本席未跑这些命令；surefire 不存在。不把未执行整仓门禁写成已验证。

### GR-14

- **位置**：`.semgrepignore:4-5`
- **分类**：技术性 + 工程缺陷
- **冻结 ID**：GEN-016、GEN-010、GEN-018
- **依据类型**：4
- **工程缺陷**：是
- **无业务发明**：扫描排除列表。
- **证据**：注释「keyvault 运维脚本含 bash heredoc,semgrep bash 解析器报错」。命令 `semgrep --config p/bash --quiet --error ops/keyvault/` → **EXIT:0**（semgrep 1.166.0）。注释不是依据；当前排除密钥脚本的「无法解析」理由未被复现。

### GR-15

- **位置**：`.env.example:14-18,20-22`
- **分类**：技术性 + 工程缺陷
- **冻结 ID**：GEN-002、GEN-013、GEN-016、GEN-008
- **依据类型**：4
- **工程缺陷**：是
- **无业务发明**：环境变量名与占位值；锁次数/TTL 未接入代码，不构成已生效业务规则。
- **证据**：
  - `rg JWT_SECRET backend/src`：无命中。样例仍给 `JWT_SECRET`。
  - `rg CORS_ALLOWED_ORIGINS`：仅 `ops/160/complex.env.example` 与本文件。
  - `AuthService.java:41` `@Value("${complex.auth.access-token-ttl:PT30M}")`；`ProductionAuthService.java:49-51` 同 TTL，且 `failed-login.max-attempts:5`。样例 `ACCESS_TOKEN_TTL_SECONDS=900`、`USER_ACCESS_TOKEN_TTL_SECONDS=3600`、`AUTH_FAILURE_LOCK_THRESHOLD=3` 在 `backend/src` 无对应占位符。
  - L21-22「留空则用 dev 默认」同时写 `COMPLEX_KEYVAULT_PII_KEY=""`。yaml 为 `${COMPLEX_KEYVAULT_PII_KEY:complex-dev-pii-encryption-key-change-me}`。空串是否回落默认**未**跑 Spring 实验，列为未验证前提；变量名本身已接线。死变量三项足以定罪。

### GR-17

- **位置**：`CONTEXT.md:35`
- **分类**：添加
- **冻结 ID**：GEN-017
- **依据类型**：1（三份 HTML 无 `L4`）
- **工程缺陷**：否（内容发明，B2 问操作员；非代码缺陷）
- **证据**：CONTEXT「酒吧为 **L4 独立商户**」。三份 HTML `rg L4`：**No matches**。HTML 有「独立酒吧商户」，无「L4」层级名。最接近：PGM 行「独立酒吧商户」。属业务层级发明，不是 HTML 沉默可忽略的技术词。

### GR-18

- **位置**：`CONTEXT.md:89-93`
- **分类**：添加
- **冻结 ID**：GEN-017
- **依据类型**：1
- **工程缺陷**：否（B2 询问）
- **证据**：CONTEXT 立词条「营销活动成本承担主体」，边界「可以是商户、项目发起平台、全国平台或其他登记赞助主体；多个主体分摊时分别锁定预算」。  
  HTML 有「成本承担主体」（BEQR「积分兑换定价」：低于下限须填「成本承担主体和预算」），**无**「营销活动成本承担主体」词条，也无「登记赞助主体 / 多主体分摊分别锁预算」原文。上位最接近仍是「积分兑换定价」的成本承担主体；多主体分摊/赞助超出该条，不能指成细化。

细化且**无工程缺陷**的业务词条已在 §6 登记，不进修复队列。

---

## 8. 实跑命令与关键输出（摘录）

```
$ sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

$ git ls-files -z -- .env.example .gitignore .semgrepignore AGENTS.md CONTEXT.md PROGRESS.md README.en.md README.md | xargs -0 cat | wc -l
625

$ python3  # Flyway / OpenAPI / surefire / tools
FLYWAY count 363  last V385__permission_names_chinese.sql  max V 385
openapi-p0.json operations 720
SUREFIRE DIR False
tools top-level files 79 files + 1 dir; script-like 78 mjs

$ ls database
ls: cannot access 'database': No such file or directory

$ git remote -v
origin  https://github.com/jxh1979/mall.git

$ semgrep --config p/bash --quiet --error ops/keyvault/ ; echo EXIT:$?
# (semgrep 1.166.0; 无 parser error)
EXIT:0
```

`package.json` 中 README 所点名的 `dev:web` / `typecheck` / `build` / `verify:unit` / `verify:backend-env` / `test:backend` / `dev:backend` / `verify:openapi` / `verify:p0-contract` / `verify:tenant-isolation` / `verify:delivery-gates` / `verify:production-blockers` / `verify:ui` / `verify:ui:real-backend:seeded` / `verify:closed-loop-runtime` / `verify:migration-version-order` / `verify:predeploy`：**均存在**。未执行。

三份 HTML 只读对照，未改。

---

## 9. B1 结论

覆盖：8/8 文件，625/625 行。18GEN 逐项已核。三线已跑。

存在冲突（GR-01..07）、技术性工程缺陷（GR-08..15）、添加待 B2 询问（GR-17、GR-18）。  
按 runbook §9：有未定稿缺陷、有待补添加项 → **不得 PASS**。

**B1 FAIL**

本席不修复、不写 `questions/`、不改 lock/findings/ledger/计划/业务文件。Grok 不承担机器回执格式。

# M003 / B2 整批候选复核（Grok 席乙）

版本：V0.02

席位：Grok（审计席乙）。步骤：B2 整批候选定稿审查。模块：M003 / 322，`01-root-config-and-guidance-01`。控制 V0.17。ledger 现值：IN_PROGRESS，B3=0。两席 B1 均 FAIL。023 规范答复「删除」、024 规范答复「保留」。本席不重开 B1、不修源码、不推进其他模块、不写 findings/ledger/三份 HTML/锁/计划、不派子代理、不新增独占锁哈希行。

**B2 PASS。**

本票只覆盖 findings V0.35 整批归并、四分类顺序、修复队列归属与最小处置是否可定稿；不是模块 PASS，不是 B3 已执行。19 项待修不妨碍本批归并 PASS。未发现未处理的分类或队列错误。

## 1. 输入（只采信完整原始 B1 前缀；不以随后 B2 当 B1）

| 输入 | 派发 | 本席实算 | 采信范围 |
|---|---|---|---|
| `docs/audit-2026-09/modules/M003-b1-self-raw.md` | 前 36557 字节，SHA256=`7fc9d6901f4347f36fb8fd232c56fdb2cf5418387f5ac02b18d5aae0e6654995`；V0.01 | 全文件恰 36557 字节，SHA256 与派发逐字相同；无后续本方 B2 后缀 | 整份 V0.01 B1 |
| `docs/audit-2026-09/modules/M003-b1-grok-raw.md` | 前 36585 字节，SHA256=`1fe8db0cfdc990715545f383fccedeeedac66e4c03ef308dd628ff7256af2d3b`；V0.01 | 追加前全文件恰 36585 字节，SHA256 与派发逐字相同 | 本文件追加前全部 V0.01 B1；本追加不是 B1 |
| `docs/audit-2026-09/findings.md` | V0.35，整文件 SHA256=`00de12e4e63c56e2f9460c70bd55ee36149bd78d97c81687ece69baa8a8e9a11` | `sha256sum` 实算同一值，138352 字节 | 只审最前 M003 段及「M003唯一当前候选队列表」；M001/M002 历史只保留不重审 |

本席本轮实算 `docs/audit-2026-09/plan/criteria-lock.md` 全文 SHA256 为 `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`，与 sidecar 文本、ledger 顶部基准相同；`LOCK-AMEND-COUNT: 0`。B2 按控制不新写独占锁哈希字段行。M003 映射 `GEN-001`…`GEN-018` 共 18 条，无 HTML 派生 ID。

`questions/M003-023.md` 的 `## 操作员答复` 段首个非空行精确为 `删除`。`questions/M003-024.md` 首个非空行精确为 `保留`。未发现其它 M003 未答问题。

模块 8 文件、有效行 625，与 `plan/modules.md` 及 `git ls-files -z -- <8 paths> | xargs -0 cat | wc -l` 输出 625 一致。`HEAD`=`origin/main`=`df580198172b2c497ef3cb7e25d2e8e5dc058920`。

## 2. 全差集（19 CX / 16 GR；原报告无 GR-12/GR-16）

Grok B1 临时号：GR-01..11、13..15、17、18（共 16）。不补造 12/16。

Codex B1 临时号：CX-01..19（共 19）。

| 原临时号 | 候选正式 ID | 漏并？ | 改写原发现？ |
|---|---|---|---|
| CX-01；GR-15 死变量半边 | M003-001 | 否 | 否。GR-15 五个未接线变量仍在 |
| CX-02；GR-15 空密钥前提 | M003-002 | 否 | 否。GR-15 原写「空串回落未跑 Spring」；候选用源码+实验补成独立 ID，未把 B1 改成「从未怀疑」 |
| CX-03；GR-07 | M003-003 | 否 | 否。同位置 AGENTS:50-51；CX 账户注销硬删 + GR 审批人/唯一例外均保留 |
| CX-04 | M003-004 | 否 | 否。Grok B1 无对应 GR，Codex 独有，已入表 |
| CX-05 | M003-005 | 否 | 否。Grok B1 未单列；CONTEXT:12 权威倒置已入冲突队 |
| CX-06；GR-03 | M003-006 | 否 | 否 |
| CX-07（不含 L4 标记） | M003-007 | 否 | 否。L4 从同一行拆到 023，007 仍是通票/服务费过宽 |
| CX-08；GR-02 | M003-008 | 否 | 否。免费/无订阅未另开添加 ID，与 Grok §6 细化一致 |
| CX-09；GR-04 | M003-009 | 否 | 否 |
| CX-10 | M003-010 | 否 | 否。Grok B1 未单列；法律主体绝对化已入冲突 |
| CX-11；GR-01 | M003-011 | 否 | 否。十一端 + 留痕例外都在 |
| CX-12；GR-06 | M003-012 | 否 | 否 |
| CX-13；GR-09、GR-11 | M003-013 | 否 | 否。不存在 `database/` + tools 78 mjs/非 79 |
| CX-14；GR-08 | M003-014 | 否 | 否 |
| CX-15 | M003-015 | 否 | 否。Grok B1 未单列；过宽 GuardedAction 断言已入队 |
| CX-16；Grok §6 gitignore | M003-016 | 否 | 否。只登记 |
| CX-17；GR-14 | M003-017 | 否 | 否。原 GR-14 FAIL 正文仍在本文件 V0.01；候选队列否，见 §4 |
| CX-18；Grok §6 工程登记（105 行拆出） | M003-018 | 否 | 否。快照时态拆 021 |
| CX-19 剩余；Grok §6 业务细化（营销/结算/绿数拆出） | M003-019 | 否 | 否 |
| GR-05；CX-19:77-81 | M003-020 | 否 | 否。内部结算价范围冲突独立成条 |
| GR-10；CX-18 快照分歧 | M003-021 | 否 | 否。见 §4 |
| GR-13；CX-19 绿数时态 | M003-022 | 否 | 否。见 §4 |
| GR-17；CX-07 同行 L4 | M003-023 | 否 | 否。添加项已问且答删除 |
| GR-18；CX-19:89-93 | M003-024 | 否 | 否。原「HTML 无登记赞助/分别锁预算」事实被 BSM:1725 推翻，分类改为细化，见 §4 |

Grok §6 无缺陷登记表已整表抄入 findings「全量差集与只登记项目」，并按 2.1 把含业务上位的行归细化（019）而非因原章节名叫技术性而错类。无第三套临时号、无重复正式 ID。

## 3. 全 24 ID 分类 / 队列 / 最小处置

判定顺序：冲突 → 细化 → 技术性 → 添加。队列口径：冲突 / 有工程缺陷 / 添加且答「删除」→ 是；只登记、以及添加答「保留」且无工程缺陷 → 否。

| ID | 候选分类 | 本席 2.1 | 工程缺陷 | 队列 | 本席 |
|---|---|---|---|---|---|
| 001 | 技术性 | 无 HTML 锚点、无业务发明（死配置名） | 是 | 是 | 接受。`backend/src` 无 `JWT_SECRET`；TTL/锁阈值消费端为 `complex.auth.access-token-ttl:PT30M`、`failed-login.max-attempts:5`。最小处置删样例声明、不新接线，符合 ponytail |
| 002 | 技术性 | 密钥占位非业务实体 | 是 | 是 | 接受。`.env.example:21-22` 写留空用 dev 默认却赋 `""`。`LocalKeyVault.load` → `getProperty(..., "")` → `SigningSecretGuard` 空值抛 `must be configured`；yaml 默认只在变量未设置时出现。本席本轮未重跑 jshell，依据是这三份源码。处置删空赋值，保留生产强密钥 |
| 003 | 冲突 | 先于细化：与 BSM:570/862、PGM:588 明文矛盾 | 是 | 是 | 接受。一律软删+人脸唯一例外覆盖注销硬删；`role_platform_admin` 不是「具备最高权限的人」。收窄为配置主数据软删并引用专项生命周期 |
| 004 | 技术性 | CI 说明，无业务发明 | 是 | 是 | 接受。`.github/workflows/predeploy-gates.yml:17` 已 `fetch-depth: 0`。只删失实浅克隆理由，不改门禁 |
| 005 | 冲突 | CONTEXT:12 把三份领域 Markdown 置于 HTML 之上，与 PGM:974 解释权矛盾 | 是 | 是 | 接受。删该段，不改其余源 |
| 006 | 冲突 | 固定隶属/城市公司越权 vs PGM:415/418/420 | 是 | 是 | 接受 |
| 007 | 冲突 | 通票成本/服务费过宽 vs PGM:428-430 | 是 | 是 | 接受。不把 L4 混进本条 |
| 008 | 冲突 | 「同一自然人全国唯一」vs BEQR 会员唯一识别 | 是 | 是 | 接受。免费/无订阅保持细化，不另添加 |
| 009 | 冲突 | 写死 30 天 vs BEQR 可配窗口+前延 | 是 | 是 | 接受 |
| 010 | 冲突 | 中心必须法律主体 vs BEQR:75 可以不是独立公司 | 是 | 是 | 接受 |
| 011 | 冲突 | 十类/兜底关 vs PGM:738-739 十一类+留痕例外 | 是 | 是 | 接受 |
| 012 | 冲突 | 「存档不需审批」vs BSM:569 批量/整店须批 | 是 | 是 | 接受 |
| 013 | 技术性 | 目录说明 | 是 | 是 | 接受。`database/` 不存在；tools 顶层 80=78 mjs+1 json+1 目录 |
| 014 | 技术性 | 英文镜像 | 是 | 是 | 接受。仍写 V124/386/1251/42(6 specs)；中文基线 V385/720；`tests/e2e` 当前 10 个 `*.spec.ts`。转引中文自动块，不要求全文件翻译 |
| 015 | 技术性 | 架构描述 | 是 | 是 | 接受。`AuthController:24-33` 登录 POST 无 `@GuardedAction`；`SecurityConfig:118` 将其公开。收窄文档，不改控制器 |
| 016 | 技术性 | 构建/密钥/运行产物忽略 | 否 | 否 | 接受。只登记 |
| 017 | 技术性 | 扫描排除列表 | 否 | 否 | 接受队列否。见 §4 GR-14。原 B1 GR-14 工程缺陷主张不成立，不据此改扫描目录 |
| 018 | 技术性 | 其余入口/命令 | 否 | 否 | 接受。命令键存在 ≠ 已跑通。105 行争议在 021 |
| 019 | 细化 | 能指 HTML 上位的剩余词条 | 否 | 否 | 接受。营销/结算/绿数已拆走 |
| 020 | 冲突 | 内部结算价仅兑换 SKU vs BEQR:54 各业态商品/服务 | 是 | 是 | 接受 |
| 021 | 技术性 | 测试计数措辞 | 是 | 是 | 接受。表外澄清满足生成器约束，见 §4 |
| 022 | 技术性 | 工作日志时态 | 是 | 是 | 接受。标题仍 `feat/termination-settlement`，HEAD 已是 main；L33「完成/绿」易被读成现状。历史时态澄清即可，不要求补跑或改 7/32/15 为未执行的当前数。现文件 `TerminationSettlementMathTests` 有 8 个 `@Test`，与「纯单测 7」不一致；按历史记录保留数字不构成本批分类错误 |
| 023 | 添加 | 三份 HTML 无 `L4`；独立酒吧商户已有，L4 是层级发明 | 否 | 是 | 接受。操作员答删除。仅删 CONTEXT:35「L4 」三字符。隶属冲突仍 006，不因 023 放行 |
| 024 | 细化 | 上位即 BSM:1725「营销费用」 | 否 | 否 | 接受细化、接受队列否。见 §4 GR-18。操作员答保留 |

队列表实数：24 行，队列是 19、否 5，与派发「19 项是、5 项否」一致。023 表内「待 024 实际答复」是过时措辞，队列值已是、024 已答，不构成未处理队列错误。

## 4. 指定分歧（先核实再接受/反驳）

### 4.1 GR-14 / M003-017 — 扫描论据实跑反证

原 B1 GR-14：`semgrep --config p/bash --quiet --error ops/keyvault/` 记 EXIT:0，据此说「无法解析」注释未被复现，技术性+工程缺陷。

本席本轮原样命令：

```
$ semgrep --version
1.166.0
$ semgrep --config p/bash --quiet --error ops/keyvault/ ; echo EXIT:$?
EXIT:7
$ semgrep --config p/bash --quiet --error --json ops/keyvault/
EXIT:7
paths.scanned=[]
errors: Failed to download configuration from https://semgrep.dev/c/p/bash HTTP 404.
        invalid configuration file found (1 configs were invalid)
```

EXIT:0 本轮不能复现；即使当时为 0，`scanned=[]` 也证明该配置未扫到脚本，不能证伪解析器报错。

`semgrep --dump-ast -l bash`：`provision-from-usb.sh` / `generate-keystore.sh` / `sign-challenge.sh` EXIT:3（parser error）；`lib.sh` / `selftest.sh` / `verify-provisioned.sh` EXIT:0。`bash -n` 六个脚本全部 EXIT:0。注释「bash 解析器报错」与 dump-ast 一致，且 `.semgrepignore` 排除的是扫描输入而非声称源码有安全漏洞。

按 2.1：无 HTML 冲突、无业务发明 → 技术性。工程缺陷：原「p/bash 能解析故排除理由虚假」不成立。不把「密钥脚本必须被该扫描器扫描」升为锁外新判据。

**本席接受 017 技术性 / 缺陷否 / 队列否。** 本文件 V0.01 GR-14 原文保留，不改写成「从未主张过」。

### 4.2 GR-18 / M003-024 — BSM:1725 明确原文

原 B1 GR-18 依据「HTML 无登记赞助主体 / 多主体分摊分别锁预算」判添加。该事实错误。

本席实读 `docs/business-system-map-for-operations-v0.1.html:1725`：

> 活动发起方与费用承担方可以不同；费用承担方必须锁定到具体法律主体，可以是商户、娱乐平台公司、游乐公司、传媒公司、城市项目公司、中央餐饮公司、全国平台或其他登记赞助主体。同一场活动支持多个主体共同承担，按固定金额或按比例分摊，采用比例分摊时各方比例合计必须为 100%；各承担方的预算分别锁定、分别扣减，任一方预算用尽不得自动占用其他方的预算。

CONTEXT:89-93 词条「营销活动成本承担主体」能指该行，按 2.1 是细化不是添加。操作员答复「保留」，未证工程缺陷 → 队列否。

残余措辞：CONTEXT 写「项目发起平台」，HTML 列举的是具体法律主体；这是压缩而非相反命题，不把 024 打回添加或冲突。原 GR-18 添加主张留在 V0.01。

**本席撤回以错误 HTML 缺文为由的添加分类，接受候选细化/队列否。**

### 4.3 GR-10 / M003-021 — 快照时态与生成器约束

`README.md:105` 当前值「**862** tests（现有 surefire 报告）」；取证列已写「无报告时保留上次快照」。本席：`backend/target/surefire-reports` **False**。`node tools/refresh-entry-doc-baseline.mjs --check` EXIT 0（720 ops / V385 / 363 迁移；因无 surefire，摘要不含 tests）。

生成器 `tools/refresh-entry-doc-baseline.mjs:75-96`：整块替换 `baseline:auto-begin`…`auto-end`；只要还能从旧块抓到数字，`testsCell` 恒写 `**N** tests（现有 surefire 报告）`，不论目录是否存在。`--check` 比较整块。手改 105 行会被回填覆盖或导致 --check 红。

表外澄清（自动块之前、L96-98 说明段）不在 `blockRe` 内，回填与 --check 都不会吃掉。生成器本身不在 M003 八文件内，本模块最小处置不能改 tools。保留 862、不制造测试结果，符合「不为未跑的全量测试造数」。

GR-10 的缺陷是「现有」在目录缺失时仍像当场取证。表外一句足以在本模块内消除该歧义。CX-18 把其余入口登记为无缺陷，拆 021 不是改写。

**接受 021 技术性 / 缺陷是 / 队列是 / 表外澄清。**

### 4.4 GR-13 / M003-022 — 历史时态

`PROGRESS.md:1` 标题限定 `feat/termination-settlement`；`HEAD` 已是 `main`。L18「纯单测 7」，当前该类 `@Test` 为 8。L33「完成。后端(单测32绿)+…全链绿」在无 surefire、本席未跑 verify:predeploy 时不能当现状。CX-19 原按历史读；根目录文件仍呈「完成」。最小处置改标题/状态时态、保留当时数字，不把未执行结果写成当前 PASS。

**接受 022 技术性 / 缺陷是 / 队列是。**

## 5. 技术性「无业务发明」逐条（Grok 独立复核）

归技术性 = 无业务发明 + 无与 HTML 冲突。下列本席同意为技术性（缺陷与否见上表）：

| 范围 | 无业务发明理由 |
|---|---|
| `.gitignore` 全文件（016） | 构建产物、密钥文件、测试产物、本机设置、运行态附件 |
| `.semgrepignore` 全文件（017） | 扫描器输入范围；OpenAPI/归档/密钥脚本排除不发明实体 |
| `.env.example` 进程/库/版本/BUILD_TIME、P0 本地种子注释（018） | 运行配置；角色名来自 HTML，本文件不发明规则 |
| `.env.example` 五条死变量与空 PII（001/002） | 配置键名/占位；未生效业务阈值 |
| `AGENTS.md` 浅克隆理由（004） | CI 说明 |
| `README.md` 不存在目录与脚本数（013） | 入口树 |
| `README.en.md` 过期基线（014） | 镜像文档，非产品终端 |
| README 过宽 GuardedAction 链（015） | 技术分层描述 |
| README 自动基线 862 措辞（021） | 测试取证来源，无价格/权限/状态发明 |
| `PROGRESS.md` 时态（022） | 实现工作日志 |
| Grok §6 其余工程行（issue 指针、worktree、迁移 V 号、中文 README 技术栈/脚本名/链接、E2E 不写死） | 代理手册与构建入口 |

下列按 2.1 **不是**技术性（有 HTML 上位或明文冲突），候选未误塞进技术性通道：003/005–012/020 冲突；019/024 细化；023 添加。

「会员免费」HTML 无该逐字短语；上位是 BEQR 会员注册自动成功、不另加付款。候选未把它做成添加，正确。

## 6. grilling 自问（事实自读，不等操作员、不派子代理）

1. 能否用 Codex 已追加的 B2 当 B1？不能。self-raw 全文件就是派发的 36557 字节 B1，本席也未把本追加当 B1。
2. GR-14 原 EXIT:0 是否仍约束 017 队列？不能。本轮 EXIT:7 + scanned=[] + dump-ast EXIT:3 是否定该论据。
3. 024 会不会因「项目发起平台」缩写而必须保持添加？不必。BSM:1725 已有完整上位；缩写不是新业务实体。
4. 021 是否必须改生成器才算修到 GR-10？不必。生成器不在本模块文件列表；表外是本模块合法缝。
5. 023 答删除后会不会放行 006/007？不会。队列表拆开。
6. 有无锁外新判据？无。未启用「自然语言必须逐字同名」「密钥脚本必须进 semgrep」之类。

ponytail-review（只看归并，不扫全仓）：无应删的重复 ID；冲突项处置均为删/收窄而非加层。`net: 0` 于台账本身。Lean already for the merge.

## 7. 技能自核（本节点实核）

按收窄使用并已读：

| 技能 | 路径 | 本次 |
|---|---|---|
| `using-superpowers` | `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | §6 开工自核。本席是被派的单任务 B2，未按其「任何动作前再调技能」本体循环 |
| `receiving-code-review` | `.../superpowers/6.3.0/skills/receiving-code-review/SKILL.md` | 先对照源文/命令核实，再接受或反驳；禁止表演性同意；B2 不 IMPLEMENT |
| `verification-before-completion` | 同上目录 | PASS 前实跑哈希/semgrep/HTML/生成器 |
| `grilling` / `grill-me` | mattpocock `productivity/grilling`、`grill-me` | 只借追问结构；作废等用户、作废派子代理 |
| `systematic-debugging` | superpowers | 只借读错误→复现 GR-14；不改生产代码 |
| `diagnosing-bugs` | mattpocock `engineering/diagnosing-bugs` | 用 semgrep JSON 作红信号；未加 instrumentation |
| `ponytail-review` | ponytail 4.9.0 | 重复 ID / 可删项 |
| `codebase-design` | mattpocock | CONTEXT/AGENTS 作为 agent 接口漂移，处置是收窄不是加层 |
| `writing-for-agents` | mattpocock | 已读；B2 不写模块收口、不改 AGENTS |
| `i-have-adhd` | `i-have-adhd/0.1.0/skills/i-have-adhd/SKILL.md` | 输出形状；本步禁止新独占锁哈希行，故不以该字段行开头 |
| `using-arc` | Arc `skills/using-arc/SKILL.md` | 仅存在性；未调 arc:audit（会写 §7 外产物） |

存在、本节点不适用未调用：`tdd` / `test-driven-development`（B3）；`resolving-merge-conflicts`（无合并）；`git-guardrails-claude-code`（存在于 `mattpocock-skills/.../misc/git-guardrails-claude-code`，安装会写 hook/问人，B2 禁止）；`brainstorming` / `writing-plans`（阶段 A）；`ponytail` 本体（B3）。

缺口，不得冒充已调用：`batch-grill-me` 不存在；`ubiquitous-language` 不存在。

控制已划掉的技能未调用。子代理未派。未执行 `mvn test`、`verify:predeploy`、playwright 实测、数据库写、提交、推送、部署。

## 8. 本轮命令与输出（摘录）

```
$ python3  # 输入哈希
self prefix 36557 sha 7fc9d6901f4347f36fb8fd232c56fdb2cf5418387f5ac02b18d5aae0e6654995 match=True
grok prefix 36585 sha 1fe8db0cfdc990715545f383fccedeeedac66e4c03ef308dd628ff7256af2d3b match=True
findings full sha 00de12e4e63c56e2f9460c70bd55ee36149bd78d97c81687ece69baa8a8e9a11 match=True

$ sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

$ git ls-files -z -- .env.example .gitignore .semgrepignore AGENTS.md CONTEXT.md PROGRESS.md README.en.md README.md | xargs -0 cat | wc -l
625

$ git rev-parse --abbrev-ref HEAD && git rev-parse HEAD && git rev-parse origin/main
main
df580198172b2c497ef3cb7e25d2e8e5dc058920
df580198172b2c497ef3cb7e25d2e8e5dc058920

surefire_exists False
database_exists False
tools_top 80 tools_mjs 78 non_mjs ['gate-hardening-tests', 'html-param-tier-baseline.json']

$ rg -n 'L4' docs/business-enum-quick-reference-v0.1.html docs/business-system-map-for-operations-v0.1.html docs/platform-governance-map-for-operations-v0.1.html
(无输出)

$ rg -n 'L4' CONTEXT.md
35:  - **各酒吧不是业态主体**：酒吧为 **L4 独立商户**…

questions/M003-023.md first_nonempty='删除'
questions/M003-024.md first_nonempty='保留'

$ semgrep --config p/bash --quiet --error --json ops/keyvault/
EXIT:7 scanned=[] HTTP 404 p/bash

dump-ast bash provision-from-usb.sh / generate-keystore.sh / sign-challenge.sh EXIT:3
dump-ast bash lib.sh / selftest.sh / verify-provisioned.sh EXIT:0
bash -n ops/keyvault/*.sh 全部 EXIT:0

$ rg -n JWT_SECRET backend/src
(无输出)

$ rg -c '@Test' backend/src/test/java/com/complex/platform/contract/TerminationSettlementMathTests.java
8

$ rg -n 'fetch-depth' .github/workflows/predeploy-gates.yml
17:          fetch-depth: 0

$ node tools/refresh-entry-doc-baseline.mjs --check
[entry-doc-baseline] PASS: 快照与实际一致（720 ops / V385（363 迁移））。
EXIT:0

$ ls tests/e2e/*.spec.ts | wc -l
10
```

BSM:1725、BEQR:54、PGM:415/428-430/738-739/974、AGENTS:50-51/65-66、README:96-107、生成器 75-96、AuthController:24-33、SecurityConfig:118、LocalKeyVault:36-40、SigningSecretGuard:14-16 均为本席直接读取。

## 9. B2 结论

整批候选归并、差集、四分类顺序、技术性无业务发明、修复队列归属与最小处置：**本席 PASS。**

19 项队列是（001–015、020–023）待 B3，不妨碍本票。017 队列否是对本席原 GR-14 扫描论据的独立否决，不是表演性同意。024 由 BSM:1725 改为细化并因操作员保留而出队。无未答添加项，无铁律 4 HTML 申请。

本票不是模块 PASS。下一步不由本席执行。

# M003 / B2 整批候选复核（Grok 席乙，B5-r1回退后）

版本：V0.03

席位：Grok（审计席乙）。步骤：B2 整批候选定稿审查（由 B5-r1 双 FAIL 整批回退）。模块：M003 / 322，`01-root-config-and-guidance-01`。控制 V0.17。ledger 现值：`IN_PROGRESS`，B3=1。两席 B5-r1 均已独立审完整 8 文件 621 行 / 18GEN 后 FAIL；B5 后四值全等 `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`，`LOCK-AMEND-COUNT: 0`。本席不重开 B1/B5、不修源码、不推进下一模块、不写 findings/ledger/三份 HTML/锁/计划/新文件、不派子代理、不新写独占 `CRITERIA-LOCK-SHA256:` 行、不覆盖本文件原 58006 字节。未读取 self 的本次 B2 投票。

**B2 PASS。**

本票只覆盖：以两份 B5-r1 本次完整报告为唯一输入、对 findings V0.38 候选做全差集与四分类/队列/编号复核。不是模块 PASS，不是 B3 已执行。018/025/026 三项待修不妨碍本批归并 PASS。分类/队列/编号无未解决分歧。

本席本轮实算 `docs/audit-2026-09/plan/criteria-lock.md` 全文 SHA256 为 `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`，与 sidecar 文本、ledger 顶部基准、B5-r1 grok raw 最后一条锁哈希行相同。B2 按控制把该值写在正文，不新写独占锁哈希字段行。M003 映射仍为 `GEN-001`…`GEN-018` 共 18 条。

---

## 1. 输入（只采信 B5-r1 本次完整报告；不以旧 B1/B4 当本批唯一输入）

| 输入 | 派发 | 本席实算 | 采信范围 |
|---|---|---|---|
| `docs/audit-2026-09/modules/M003-b5-r1-self-raw.md` | 27009 字节，V0.02，SHA256=`c3553cb525c00c9afad9831546872598f6ec9cb7974d5b48c1fe7b96c1d8d611` | 全文件恰 27009 字节，SHA256 与派发逐字相同；唯一独占锁哈希行在第 1 行 | 该行起至 EOF 的完整报告（含 V0.02 证据类型澄清） |
| `docs/audit-2026-09/modules/M003-b5-r1-grok-raw.md` | 26335 字节，V0.01，SHA256=`69b40ae4e4185562ed753ea0f4cf9a128d4c43f24edf9740d40f7e326ee670d0` | 全文件恰 26335 字节，SHA256 与派发逐字相同；唯一独占锁哈希行在第 1 行 | 该行起至 EOF 的完整报告（本席 B5，不把其结论当本轮 B2 的自动票） |
| `docs/audit-2026-09/findings.md` | V0.38，145293 字节，SHA256=`f205604867e66c6ee7ab5b605de9c6e01de111027f61031f628fc12e061a8e0f` | 全文件恰 145293 字节，SHA256 与派发逐字相同 | 最上方 V0.38 段 + `### M003唯一当前候选队列表` 为当前候选；其余段保留历史 |
| 本文件追加前 | 58006 字节，SHA256=`e7736c8bd139469157615222efda46f6390d83ab22e907a20816a0e594bd6292` | 追加前字节与哈希与派发相同 | 原 B1 + 首次 B2 V0.02 全部保留；本追加是 V0.03 |

8 源文件当前字节/行/SHA 与两份 B5 输入表一致，合计 621 行。`CONTEXT.md` 无 `L4`。`questions/M003-023.md` 答复段首个非空行=`删除`；`questions/M003-024.md` 答复段首个非空行=`保留`。无待答添加项。

未把旧 B1/B4 发现当本批唯一输入。旧 24 ID 只用于编号同一性核对与「未复发」对照，不替代 B5 原文。

---

## 2. 四条临时发现全差集

| 临时号 | 提出席 / B5 原文分类 | 候选正式ID / 分类 / 队列 | 本席本轮独立票 | 差集 |
|---|---|---|---|---|
| CX-01 高危分档 | self：冲突；位置 `CONTEXT.md:116-121`（关键 119）。本席 B5：不升冲突（PGM652 有三档；CONTEXT 未写发起人选档） | M003-019 / 细化 / 否（撤回冲突，原 self 未改写） | **接受撤回。** 019 保持细化/否 | 无漏并。冲突主张撤回，不另开新 ID |
| CX-02 客人 Web | self：冲突；`README.md:135`、`README.en.md:33,90`。本席 B5 grilling Q3：C4 不列齐十一端、CONTEXT 已持原文 → **不升冲突** | M003-026 / 冲突 / 是（保留本席原不认定，要求本轮按具体原文再票） | **本轮改判冲突/是。** 推翻本席 B5 的不升冲突 | 无漏并。编号用新 026，不塞进 018/011 |
| CX-03 Data JPA | self：技术性有工程缺陷；`README.md:18-19`、`README.en.md:16`。本席 B5：未单列（当时按版本一致放过组件名） | M003-018 / 技术性 / 是（沿用已有技术栈登记） | **接受。** 018 技术性/缺陷是/队列是 | 无漏并。不新开技术栈 ID |
| GR-01 中文例外 | 本席 B5：冲突；`AGENTS.md:55-57` vs PGM:287/965。self B5 未单列此项 | M003-025 / 冲突 / 是 | **接受。** 本席原主张，候选未改写例外闭集命题 | 无漏并。编号用新 025，不把 018 工程段覆盖当成专属冲突 ID |

四条全部逐项归并。正反观点：V0.38 段保留 self 的 CX-01 反例、本席 B5 对 CX-02 的不升冲突理由、以及本席 GR-01 原文指向。候选未声称已定稿。

无第五条临时发现进入本批。B5 两席对其余已修/只登记项均为未复发；本席本轮抽查 L4 已删、十一类词条仍在 CONTEXT:144-149、023/024 答复齐全，不把「未复发」扩成新一轮全量找茬。

---

## 3. 本席对四条的第一性原理票（先核实再接受/反驳）

### 3.1 CX-01 / M003-019 — 撤回冲突

`CONTEXT.md:119` 原文：

> 授出前必须先审批才生效，按上一级、上两级或最高权限分档；配置只能加严，不能关闭五类强制审批；申请人不得审批自己的授权申请。

PGM:652：

> 敏感动作可要求上一级、上两级或最高权限审批。

PGM:528（全平台统一审批模型，660「上述」所指）：

> 审批链不由发起人选择档位：发起单据时只需指定该事项的最终审批人，系统按组织层级自动补全从发起人到该最终审批人之间的全部直接上级节点，逐级审批；…

PGM:660：

> 审批链按上述“只指定最终审批人、系统自动补全中间层级”的方式生成，不由发起人选择档位。

2.1 冲突 = 与 HTML **明文相反**。119 写了三档名称和「授出前必须先审批」「申请人不得自审」，与 652/660 的强制审批、禁自审同向。119 **没有**「发起人选择档位」「跳过中间层级」「三档任选其一」的句子。把 652 的层级名写进高危段、未同时抄 660 的链生成句，是细化未写全，不是相反命题。self 的对抗反例额外假定了「提供三个审批档位供选定并跳过其余层级」的界面，该界面不在当前 119 原文里，不能当作已存在缺陷。

**票：撤回冲突；019 保持细化 / 工程缺陷否 / 队列否。** 原 self CX-01 正文留在 B5 raw，不改写。不把 660 未抄全升为新冲突 ID。

### 3.2 CX-02 / M003-026 — 本轮认定冲突

本席 B5 曾写：mermaid 是 C4 系统上下文，不是十一类终端清单；CONTEXT 持有十一类原文；不升冲突。该理由把「摘要免列齐」当成「可以画错误使用者」。本轮按原文推翻。

PGM:739：

> 受控的十一类终端：客人手机应用、客人小程序、Web 前端（业务人员，客人无网页端）、工作人员平板、员工工作手机、员工私人手机、商户网页门户、商户手机应用、触摸屏一体机、移动管理端、独立经销商 Web 门户。

`CONTEXT.md:146` 已逐字持有「Web 前端（业务人员，客人无网页端）」。CONTEXT 写对 **不能** 授权另一份当前入口写错。

当前入口原文：

`README.md:135`：`GUEST["客人 Guest"] --> WEB["前端 Web<br/>(admin/customer)"]`

`README.en.md:90`：`GUEST["Guest"] --> WEB["Web (admin/customer)"]`

`README.en.md:33`：`apps/web/  # React + Vite + antd admin/customer web`

这是对「消费客人使用名为 Web(admin/customer) 的网页端」的**肯定陈述**，与「客人无网页端 / Web 前端属业务人员」明文相反。中文目录树 `README.md:38` 写的是「管理端」，该项本身不冲突；冲突在图边和英文树的 customer 标签。

对抗最小反例：供应商只按 README 图为 Guest 建设 customer Web。C4 不列齐十一端，不等于可以把客人画进 Web。不因 `apps/web` 目录或历史 customer 字样反向授权。不问工程选择、不扩审 web 模块实现。

按 2.1 顺序：冲突优先于细化/技术性。处置为删除错误关系与 customer Web 标签。候选最小修复（Guest→Web 改为业务人员 Web；EN 树 admin/customer web → admin web；不新增 app 节点）不发明新终端。

**票：冲突 / 工程缺陷是 / 队列是 / 新 ID 026。** 本席 B5 不升冲突判断留在 B5 raw，本票明确撤回该判断，不是表演性同意。

### 3.3 CX-03 / M003-018 — 技术性工程缺陷，沿用 018

`README.md:18` 写 Data JPA；`README.md:19` 写 `starter-data-jpa + jdbc` 为 pom 实证；`README.en.md:16` 写 Data JPA。

本席直接读 `backend/pom.xml`（SHA256=`f4a4a05ee21aa7c555a7bf0b11c7509a0963bdd22397e4dacfce3db89b039701`）第 60 行仅为 `spring-boot-starter-jdbc`。`rg -n 'starter-data-jpa|jakarta.persistence|JpaRepository' backend/pom.xml backend/src/main`：pom 仅命中 jdbc；`backend/src/main` 无命中。

无 HTML 业务锚点，不规定角色/收费/状态/流程 → 技术性。组件名与构建定义相反 → 有工程缺陷，进修复队列。不能为了让文档变真而新增 JPA 依赖。self 已把依据类型从误写的 2/4 澄清为仅 4；本席同意：本次未跑 JPA 编译失败用例，POM 与 README 的直接差异即类型 4。

**票：技术性 / 缺陷是 / 队列是 / 沿用 M003-018。** 原 018 命题就是其余工程入口与「技术版本与 pom/package 一致」，覆盖 README:1-37 技术栈表；现只纠正该命题里已证伪的组件名，不另开 ID。

无业务发明理由：只陈述当前已声明的框架组件（JDBC），不定义业务实体、枚举、权限或价格。

### 3.4 GR-01 / M003-025 — 冲突，新 ID

`AGENTS.md:55-56`：

> 所有展示文字全中文，仅机读标识（枚举值、字段名、日志键）保留原文。
> 后台、客人端、设备端、安卓端一律适用。

PGM:287：

> …包括菜单、页面标题、字段名、状态、枚举取值、按钮、提示、报错、导出文件表头与内容、短信与通知、日期时间等，必须全部为中文，不得出现任何英文…可以不用中文的只有四样，没有第五样：单据号、设备编号、二维码内容、系统内部标识符。这四样之外，凡是展示给人看的文字一律中文，不得以“这是标识”为由保留英文。

PGM:965 已拍板复述同一封闭四样。

铁律 7 的「全中文 + 全终端」有上位，属细化；但其例外把「枚举值、字段名」列为可留原文的机读标识，与 HTML「字段名、枚举取值必须中文」且「没有第五样 / 不得以这是标识为由」明文相反。冲突优先。日志键可落入「系统内部标识符」，不能把字段名/枚举取值一并放开。

候选最小修复：把例外清单换成 HTML 封闭四样，第 56 行适用终端保留。不增业务规则，不改 HTML。

**票：冲突 / 缺陷是 / 队列是 / 新 ID 025。**

---

## 4. 编号独立性

| 候选 ID | 本席是否沿用 | 内容同一性 |
|---|---|---|
| 018 给 CX-03 | **沿用** | 原 018 已登记 README 技术栈表并声称与 pom 一致；Data JPA 就是该命题被证伪的片段。同一技术栈事实，不是新业务冲突 |
| 019 给 CX-01（撤回后仍用 019） | **沿用** | 原 019 已覆盖 CONTEXT 高危段术语细化。撤回冲突后继续只登记，不把撤回写成新 ID |
| 025 给 GR-01 | **新编，不沿用 018/003/011** | 018 原把 AGENTS 中文例外登记为「细化运营展示口径 / 缺陷否」，命题是「有上位、无缺陷」，与「例外清单与封闭四样相反」不是同一 finding。003 是删除规则覆盖注销；011 是 CONTEXT 十类终端+留痕例外（已修）。工程段范围覆盖 ≠ 专属冲突 ID。025 与现改为技术性缺陷的 018 分类也不同，不能混进同一 ID |
| 026 给 CX-02 | **新编，不沿用 018/011/015** | 018 原把 README 架构段登记为技术性无缺陷入口说明；CX-02 是「客人使用 Web」与 PGM:739 的业务终端冲突。011 的缺陷是 CONTEXT 漏第十一类并漏留痕例外，位置 `CONTEXT.md:139-149`，B3 已修、B5 未复发；与 README 图把 Guest 画进 Web 不是同一命题。015 是 GuardedAction 过宽。故 026 |

禁止把「某行曾落在某 ID 的行号范围内」当成内容同一性。

---

## 5. 全 26 ID 当前分类 / 队列（对照唯一队列表）

| ID | 分类 | 工程缺陷 | 队列 | 本席 |
|---|---|---|---|---|
| 001 | 技术性 | 已修复 | 否 | 接受。B5 未复发 |
| 002 | 技术性 | 已修复 | 否 | 接受。B5 未复发 |
| 003 | 冲突 | 已修复 | 否 | 接受。B5 未复发 |
| 004 | 技术性 | 已修复 | 否 | 接受。B5 未复发 |
| 005 | 冲突 | 已修复 | 否 | 接受。B5 未复发 |
| 006 | 冲突 | 已修复 | 否 | 接受。B5 未复发 |
| 007 | 冲突 | 已修复 | 否 | 接受。B5 未复发 |
| 008 | 冲突 | 已修复 | 否 | 接受。B5 未复发 |
| 009 | 冲突 | 已修复 | 否 | 接受。B5 未复发 |
| 010 | 冲突 | 已修复 | 否 | 接受。B5 未复发 |
| 011 | 冲突 | 已修复 | 否 | 接受。B5 未复发。不把 026 并回 011 |
| 012 | 冲突 | 已修复 | 否 | 接受。B5 未复发 |
| 013 | 技术性 | 已修复 | 否 | 接受。B5 未复发 |
| 014 | 技术性 | 已修复 | 否 | 接受。B5 未复发 |
| 015 | 技术性 | 已修复 | 否 | 接受。B5 未复发 |
| 016 | 技术性 | 否 | 否 | 接受。只登记 |
| 017 | 技术性 | 否 | 否 | 接受。只登记 |
| 018 | 技术性 | 是 | 是 | 接受。见 §3.3 |
| 019 | 细化 | 否 | 否 | 接受。见 §3.1 |
| 020 | 冲突 | 已修复 | 否 | 接受。B5 未复发 |
| 021 | 技术性 | 已修复 | 否 | 接受。B5 未复发 |
| 022 | 技术性 | 已修复 | 否 | 接受。B5 未复发 |
| 023 | 添加 | 否 | 否 | 接受。操作员删除已落地，出队；分类保持添加 |
| 024 | 细化 | 否 | 否 | 接受。操作员保留；BSM:1725 上位；B5 未证新缺陷 |
| 025 | 冲突 | 是 | 是 | 接受。见 §3.4 |
| 026 | 冲突 | 是 | 是 | 接受。见 §3.2 |

队列表实数：26 行；队列是 3（018/025/026）；否 23。与 V0.38「全26ID，018/025/026三项是，其余23否」一致。023 实际删除、024 保留且 HTML 已有上位，没有待答问题。

历史「逐项论据」里 018 仍写缺陷否，属 V0.35 时态保留；当前值以唯一队列表与 V0.38 段为准，不构成未处理队列错误。

---

## 6. 技术性无业务发明（本批仍为技术性的项）

018 待修片段：README 技术栈组件名 vs 当前 POM。无业务发明理由见 §3.3。其余 018 覆盖的命令键、链接、基线快照口径仍是构建/入口事实，不发明角色或价格；本批 B3 只动 JPA/JDBC 三处，不重写正确段落。

016/017 及已修复的技术性 ID 无业务发明理由沿用首次定稿：忽略规则、扫描输入、环境样例、CI 说明、目录/版本/架构过宽句。本轮无新的技术性添加项，无 Grok 改判「添加」。

025/026 不是技术性，不走无业务发明通道。

---

## 7. 命令与原文（本轮实跑，非预计）

```
# 输入哈希
sha256sum docs/audit-2026-09/modules/M003-b5-r1-self-raw.md
c3553cb525c00c9afad9831546872598f6ec9cb7974d5b48c1fe7b96c1d8d611
sha256sum docs/audit-2026-09/modules/M003-b5-r1-grok-raw.md
69b40ae4e4185562ed753ea0f4cf9a128d4c43f24edf9740d40f7e326ee670d0
sha256sum docs/audit-2026-09/findings.md
f205604867e66c6ee7ab5b605de9c6e01de111027f61031f628fc12e061a8e0f
# 追加前本文件
stat -c '%s' docs/audit-2026-09/modules/M003-b1-grok-raw.md
58006
sha256sum docs/audit-2026-09/modules/M003-b1-grok-raw.md
e7736c8bd139469157615222efda46f6390d83ab22e907a20816a0e594bd6292

# 锁四值（B5 后口径；B2 不新写独占行）
sha256sum docs/audit-2026-09/plan/criteria-lock.md
23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
cat docs/audit-2026-09/plan/criteria-lock.sha256
23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
ledger.md 顶部字段值（本席摘录，本追加不以独占锁哈希行起行）: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
B5-r1 grok raw 最后一条锁字段值: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

# 8 文件
git ls-files -z -- .env.example .gitignore .semgrepignore AGENTS.md CONTEXT.md PROGRESS.md README.en.md README.md | xargs -0 cat | wc -l
621

# pom
sha256sum backend/pom.xml
f4a4a05ee21aa7c555a7bf0b11c7509a0963bdd22397e4dacfce3db89b039701
# artifactId 含 spring-boot-starter-jdbc 于第 60 行；无 spring-boot-starter-data-jpa
rg -n 'starter-data-jpa|jakarta.persistence|JpaRepository' backend/pom.xml backend/src/main
# pom: 无 starter-data-jpa；backend/src/main: No matches found

# questions 答复段首个非空行
M003-023.md -> 删除
M003-024.md -> 保留

# CONTEXT L4
rg -n 'L4' CONTEXT.md
(无输出)

# 队列表
26 行；队列是 = M003-018, M003-025, M003-026
```

未跑 `mvn test` / 全量门禁 / 不写库。文档/配置类 B2 不强制新测试。

---

## 8. grilling（自问自答，不等操作员）

1. 能否沿用 B5「C4 故不冲突」直接 FAIL 026？不能。须读 PGM:739 与当前图边。图是肯定的错误使用者，不是清单不全。
2. 026 应否并入 011 或 018？不能。011 已修的是 CONTEXT 十类/留痕；018 是技术入口登记。内容不同。
3. 119「分档」是否仍是冲突？不能仅因未抄 660 链生成句升级。明文相反的句子不存在。
4. 025 应否并入 018 的中文例外登记？不能。原登记是「细化无缺陷」；现是冲突。018 本批已用于 JPA 技术缺陷。
5. 有无新添加项要问？无。023/024 已答。不因 HTML 沉默新开添加。

---

## 9. ponytail-review（只列可删，不改）

`AGENTS.md:55: shrink: 「枚举值、字段名、日志键」换成 PGM 封闭四样。`
`README.md:135: delete: Guest→Web(admin/customer)。改为业务人员→Web。`
`README.en.md:33,90: delete: customer web / Guest→Web。`
`README.md:18-19 / README.en.md:16: shrink: Data JPA / starter-data-jpa 改当前 JDBC。`
`net: 删除错误例外与错误使用者，不新增终端或依赖。`

---

## 10. §6 技能（本席自核；缺口照实）

实际读取并按收窄口径使用：

| 技能 | 路径 | 本次用法 |
|---|---|---|
| `using-superpowers` | superpowers 6.3.0 | 开工技能自检 |
| `using-arc` | Arc using-arc | 路由；本任务是审计 B2 不是 ideate/implement |
| `receiving-code-review` | superpowers | 先对照源文/命令核实，再接受或反驳；禁止表演性同意；B2 不 IMPLEMENT。据此推翻本席 B5 对 CX-02 的不升冲突，而不是跟随候选口头 |
| `verification-before-completion` | superpowers | PASS 前先跑哈希/pom/rg/questions |
| `grilling` / `grill-me` | mattpocock productivity | 只借追问结构，自问自答；不派子代理、不等操作员 |
| `ponytail-review` | ponytail 4.9.0 | 找可删的错误例外/错误图边；不改码 |
| `codebase-design` | mattpocock | 只借删除测试：Guest→Web 与错误例外清单删掉后复杂度不转移到调用方；不设计新接口、不派 DESIGN-IT-TWICE 子代理 |
| `diagnosing-bugs` | mattpocock | 不走六段运行时循环；本步反馈环是哈希+原文+rg。未装 instrumentation |
| `i-have-adhd` | cache 0.1.0 | 输出形态；本文件被要求追加在已有正文后，首行不是锁哈希 |
| `writing-for-agents` | mattpocock | 已读；B2 不写模块收口、不改 AGENTS |
| `tdd` / `test-driven-development` | 存在 | 文档/配置不强制落测试；未写测试 |
| `resolving-merge-conflicts` | 存在 | 无合并冲突，未调用本体 |
| `git-guardrails-claude-code` | 存在 | 安装会写 hook；B2 禁止 git 写，未安装 |
| `ponytail` 本体 | 存在 | B2 非修复，不启用 ultra 改码 |

缺口（不得冒充已调用）：

- `batch-grill-me`：本环境未找到技能目录
- `ubiquitous-language`：本环境未找到技能目录

明确不用（§6 划掉）：`executing-plans`、`dispatching-parallel-agents`、`subagent-driven-development`、`using-git-worktrees`、`finishing-a-development-branch`、`research`、`ponytail-audit`、`ponytail-debt`、`ponytail-gain`、`domain-modeling`、`code-review`(mattpocock)、`wait-what`。

`docs/agents/domain.md` 已读：三份 HTML 最终解释权；本席未改 CONTEXT/HTML。

---

## 11. B2 结论

整批候选归并、差集、四分类顺序、技术性无业务发明、修复队列归属、编号与最小处置：**本席 PASS。**

本席独立票与候选 V0.38 一致：019 细化否；018 技术性是；025 冲突是；026 冲突是。026 不是跟随候选，而是本轮用 PGM:739 与三处当前入口原文推翻本席 B5 的不升冲突。若本席仍不认定 026 为冲突，本票应为 FAIL；现已认定，故不 FAIL。

3 项队列是待 B3，不妨碍本票。无未答添加项，无铁律 4 HTML 申请，无锁外新判据，无新工程事实扩模块。

本票不是模块 PASS。下一步不由本席执行。

CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M003 B1 Grok 独立审计报告（471 计划／当前文件清单）

- 报告自身版本：V0.04（追加；保留本文件既有 V0.01–V0.03 全部字节。旧段审的是 322 计划 8 文件，**不沿用旧同号 PASS/FAIL**）
- 控制版本：160-audit-prompt.md V0.17
- 席位：Grok（唯一外审），无子代理／第三席
- 模块：M003 — 治理门禁与 CI 基线／配置与工程入口／第1段（`plan/modules.md` V0.04）
- 文件清单（唯一受审）：`.gitignore`（55 行）、`package.json`（147 行），`git ls-files -z -- .gitignore package.json | xargs -0 cat | wc -l` = **202**
- 标记：NORMAL
- 映射：`criteria-lock.md:6382` `M003 → [GEN-001 … GEN-018]`（无 HTML 派生 ID）
- ledger（仅顶部 + 本模块行）：`CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；`LOCK-AMEND-COUNT: 1`；M003 `IN_PROGRESS`，历史 B3 累计 2、计划修复 2、失败重跑 0（不清零）
- 正本：`/home/james/complex-management` `main` `242f487233d028173cb9198eecd079f5d877405c`（与 `origin/main` 跟踪）
- 本席 B1 结论：**PASS**
- HTML 自身问题（本模块范围内）：**无上报**。`verify:entry-anchors` 红 6 项为已知 BSM CSV 镜像（属 M425/M427）；工作区 BSM HTML 相对 `origin/main` 有未提交改动使 `verify:html-ledger-sync` 红，属门禁正确拒绝，**不重开 M001**，不把该脏工作区写成 HTML 正文缺陷。

---

## 0. §6 技能自核（本席实际环境）

| 技能 | 路径核验 | 本次用法 |
|---|---|---|
| `using-superpowers` | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能路由 |
| `grilling` / `grill-me` | mattpocock `productivity/grilling`、`productivity/grill-me` | 对抗式／第一性原理追问结构；**自问自答**；不派子代理、不问人 |
| `codebase-design` | mattpocock `engineering/codebase-design` | **只**用已冻结 general 的接口／深度／缝词汇评估根 `package.json` 作为 npm 入口；不设计新接口、不派 DESIGN-IT-TWICE |
| `diagnosing-bugs` | mattpocock `engineering/diagnosing-bugs` | 只读源／复现命令；不加 instrumentation |
| `systematic-debugging` | superpowers | 只借读错误→复现→追数据流；不改生产代码 |
| `ponytail-review` | ponytail 4.9.0 | 只报告可删内容；不改文件 |
| `verification-before-completion` | superpowers | PASS 前核对命令输出与 exit code |
| `tdd` / `test-driven-development` | 存在 | **纯配置不适用**；未新增测试 |
| `ubiquitous-language` | `/home/james/.qwen/skills/ubiquitous-language/SKILL.md`（存在；`disable-model-invocation: true`） | 仅收窄用语（入口／门禁／工作区／忽略规则／系统内部标识符）；**未写** `UBIQUITOUS_LANGUAGE.md`（会成 §7 外产物） |

缺口（如实，不冒充已调用）：

- `batch-grill-me`：本环境技能目录中**未找到**（runbook 记载的 Windows Codex 路径不在本席运行环境）

明确不用（§6 划掉或本节点不适用）：`brainstorming`、`writing-plans`、`executing-plans`、`dispatching-parallel-agents`、`subagent-driven-development`、`using-git-worktrees`、`finishing-a-development-branch`、`research`、`ponytail-audit`／`debt`／`gain`、`ponytail` 本体（B3）、`requesting-code-review`／`receiving-code-review`（B4）、`resolving-merge-conflicts`（无合并）、`git-guardrails-claude-code`（安装会写 hook）。

互盲：未读 `*-self-raw.md`、`findings.md`、其他模块报告／收口、本模块旧审计**正文结论**。旧 raw 只核版本头（V0.01 B1／V0.02 B2／V0.03 B2）以便追加。ledger 只读顶部两行与 M003 状态行。

未执行（并在相关声称中不伪称已执行）：`spring-boot:run`、全量 `mvn test`、`android:p0:build`／`apk`／`smoke`（会起后端／写 `build/`）、`playwright test` 全量、`verify:predeploy`／`verify:delivery-gates`／`verify:p0`／`verify:closed-loop-runtime` 整链、任何 `docs:*` 写文件入口、部署、提交、推送、安装依赖、新 worktree。`npm exec -w apps/web -- node` 曾警告将安装 npm 包 `node@26.8.1`（**不是**产品脚本；`package-lock.json` 未改、工作区无残留 `node_modules/node`）；工作区转发证据改用 `npm run --workspace`。

---

## 1. 输入哈希、行数、覆盖

命令：

```bash
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
git ls-files -z -- .gitignore package.json | xargs -0 cat | wc -l
wc -l .gitignore package.json
sha256sum .gitignore package.json
```

| 项 | 值 |
|---|---|
| 本席 `criteria-lock.md` SHA256 | `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a` |
| sidecar `plan/criteria-lock.sha256` | 同值（本席 `cat` 核对，非派发期望值） |
| ledger 顶部基准 | 同值 |
| 官方有效行数 | **202**（与 `modules.md` 一致） |
| `.gitignore` | 55 行／1199 字节／SHA256 `75861ded5d3859bcda593aafe3f3862e765470e7f8b6636a643bf59de9d7c51b` |
| `package.json` | 147 行／15583 字节／SHA256 `350999a90ddf85b2d3dcbee090dd593fb7faff18d0f467f0a608d8b3664d473f` |
| HEAD | `main` `242f487233d028173cb9198eecd079f5d877405c` |

全行覆盖：`.gitignore` 1–55、`package.json` 1–147 均进入三线，无抽样冒充整文件。

`package.json` 解析：`name=entertainment-complex-management`，`private=true`，`version=0.1.0`，`documentVersion=V0.01`（现有文件本次未改，**不强制补版本**），`type=module`，`workspaces=["apps/web"]`，`packageManager=npm@10.9.8`，`engines.node=^22.13.0 || >=24.0.0`，`devDependencies`: `@playwright/test ^1.61.0`、`yaml ^2.9.0`，**127** 个 scripts。

---

## 2. 第一性原理线（2.3 三问）

### 2.1 `.gitignore` 为何必须存在

删掉它，三份 HTML **没有**一条会立刻做不到——忽略规则不是业务规则。它防止把 `node_modules`、构建产物、`.env`、本机 SDK 路径、运行态图片／证据文件、根目录探针脚本提交进仓。答不上业务上位 ≠ 该删。四分类：**技术性**为主；`backend/data/bar-images/` 与 `backend/data/evidence-files/` 能指到 HTML 已有「商品图片上传保存」「留痕／视频证据」→ **细化**（见 §4）。gitignore **不**实现配置主数据删除审批，也不把忽略当删除业务数据。

未验证前提：忽略只作用于未跟踪文件；已跟踪文件不受 ignore 约束。本席 `git ls-files -c -i --exclude-standard` 为空，当前无「已跟踪却被忽略」裂口。

### 2.2 `package.json` 为何必须存在

它是 npm 工作区根清单：声明私有包、工作区、引擎、包管理器、两枚 devDependencies、以及 127 个可调用入口。删掉它，CI（`.github/workflows/predeploy-gates.yml` 调用 `npm run verify:predeploy`）、本地 `npm run …`、工作区转发全部失去声明面。HTML 不写 npm；这是工程入口。四分类：**技术性**（构建／CI／依赖）；其中核验三份 HTML 锚点／台账／参数层级的入口是解释权声明的下位实现 → **细化**。脚本名与参数 **没有**写入票种、状态机、价格、角色等业务发明。

`version: 0.1.0` 与 `documentVersion: V0.01` 按用户版本铁律：现有文件未改，不要求补 SemVer／协议版本。

### 2.3 声称行为是否被执行验证

见 §5 命令表。入口存在性：127 脚本中所有 `tools/*.mjs` 引用共 108 处、75 个唯一文件，**0 缺失**；`npm run` 链 17 条全部解析到已有 script 名；Playwright 4 条对应本地 `node_modules/.bin/playwright` 与两个 config 文件。工作区 `apps/web` 的 `dev`／`build`／`typecheck`／`test:unit` 在工作区 `package.json` 中存在。

未把「文件存在」或 dry-run 当行为 PASS：对可安全入口做了真实 `npm run`（见 §5）；对会部署／起服务／写业务源／全量测试的入口只读调用链与无副作用参数（`--check`／`--verify`／包装器在 `--check` 时 `process.exit(0)`）。

### 2.4 未验证就当真的前提（明示）

1. 本机 `npm -v` = 11.19.0，而 `packageManager` 声明 `npm@10.9.8`。`corepack npm -v` = 10.9.8。本席用 PATH 上的 npm 11 实跑脚本成功；**未** `corepack enable`（会改本机 shims）。未证明 npm 10 与 11 对所有 127 脚本语义逐字节相同。首跑未因此失败，不为假设差异新增门禁。
2. `engine-strict=false`：Node 不满足 engines 时 npm 只警告。当前 `v24.20.0` 满足 `>=24.0.0`。
3. CI `node-version: "22"` 在 M005 工作流文件中；本席只读调用点。未在 GitHub runner 上实跑。
4. `android:p0:build`／`apk`／`smoke`、`dev:backend`、`playwright test`、聚合链后半段未实跑。
5. 包装器 `tools/run-android-p0-smoke.mjs` 默认 `P0_ADMIN_PASSWORD=admin-pass` 等——密钥正文**不在**本模块两文件中，不把其他模块实现升为本模块缺陷。
6. `.env.production` 等变体未被 ignore；本仓实际加载 `.env`／`.env.local` 与 `/etc/complex/complex.env`。首跑不会自动生成 `.env.production`。不为假设场景加忽略规则。
7. `backend/data/` 当前不存在；两条运行态目录忽略是预防提交，不证明运行时已写入。

---

## 3. 顺从式：18 GEN 逐项

| GEN | 适用 | 证据（类型 1／2／4） | 本席 |
|---|---|---|---|
| GEN-001 必要性与最小性 | 适用 | 每条 ignore／每个 script 有可验证用途（构建产物、秘密、门禁、工作区转发）。`dev:api`→`dev:backend`、`smoke:p0`→`test:backend`、`test:ui`=`verify:ui` 为命名别名，首跑不失败。ponytail 见 §7。不为假设场景新增脚本。 | 满足 |
| GEN-002 可执行正确性 | 适用 | JSON 可解析；127 目标存在；`npm run this-script-does-not-exist` exit 1；`--workspace` 列出并转发 `typecheck`；`--check` 包装器实跑绿；会写／部署的入口未当已执行。 | 满足（在声明的实跑边界内） |
| GEN-003 测试有效性 | 部分适用 | 本模块无测试代码。`test:backend`／`test:unit`／`test:ui` 入口存在。纯配置不强制 TDD，未新增测试。未跑全量测试套件，**不**用「测试通过」替代行为证据。 | 满足（配置豁免） |
| GEN-004 信任边界 | 适用 | `.env`／`.env.local`／`local.properties` 被 ignore。`verify:html-ledger-sync` 取不到基线则 exit 1（只读脚本）。Android 包装器读 env 文件：只读调用链。 | 满足 |
| GEN-005 授权与最小权限 | 弱适用 | 两文件不实现业务授权。`private: true` 防误发布。CI／pre-push 不在本清单。 | 不适用业务授权；无越权发明 |
| GEN-006 数据完整性 | 弱适用 | `*.db` 忽略本地库文件。无事务／金额逻辑。`verify:ops160-backup-restore` 只读脚本字符串，确认 `dropdb` 仅针对演练库名。 | 满足 |
| GEN-007 并发与幂等 | 弱适用 | `verify:migration-version-order` 入口存在且实跑 PASS（本分支未新增迁移，origin/main 最大 V399）。不在 `verify:predeploy` 内——与 AGENTS 铁律 9「已进 delivery-gates、未挂 predeploy」一致；不把「应并入 predeploy」当新需求。 | 满足 |
| GEN-008 接口与模式一致性 | 适用 | 根 lock `lockfileVersion 3`，`packages[""].engines`／`devDependencies`／`workspaces` 与 `package.json` 一致；`@playwright/test` 锁 1.61.0 满足 `^1.61.0`；`yaml` 锁 2.9.0；`apps/web` 在 lock 的 `packages` 中。脚本名与工作区脚本名对齐。 | 满足 |
| GEN-009 错误处理与恢复 | 适用 | `&&` 链：`npm run verify:html-ledger-sync && echo SHOULD_NOT_PRINT` 前者 exit 1，后者未打印。缺失脚本 exit 1。`--check` 失败路径在包装器源码中 `process.exit(1)`。门禁红是失败显式，不是伪成功。 | 满足 |
| GEN-010 安全与敏感信息 | 适用 | 两文件无明文密钥。`.env`、`.env.local`、`local.properties`、`*.apk` 忽略。gitignore 注释为中文说明。 | 满足 |
| GEN-011 可观测与审计证据 | 适用 | 多数 `verify:*` 打印明确 PASS／FAIL 行（本席见到 `ANDROID_P0_SMOKE_WRAPPER_PASS`、`[entry-anchors] FAIL：6 处不合格` 等）。 | 满足 |
| GEN-012 资源与性能边界 | 弱适用 | 无循环／连接池。聚合脚本是串行 `&&`，失败即停，不放大。 | 满足 |
| GEN-013 构建配置与依赖 | 适用 | 见 §5：引擎匹配当前 Node 24.20.0；工作区转发；lock 一致；CI 调用 `npm ci --ignore-scripts` 后 `npm run verify:predeploy`（M005 文件，只读调用点）。`docs:*` 与 `verify:* --check` 成对，避免校验路径写文件（抽查 generate 脚本 `checkMode`）。`docs:security-baseline`／`docs:openapi-examples` 为写入口，CI 不调用它们。 | 满足 |
| GEN-014 数据库迁移正确性 | 适用（入口） | `verify:migration-version-order` 实跑：`MIGRATION_VERSION_ORDER_PASS: 本分支未新增迁移(origin/main 最大号 V399)` exit 0。本模块不含 SQL。 | 满足 |
| GEN-015 界面可用性 | 弱适用 | 脚本名为系统内部标识符（允许英文）。gitignore 注释为中文。无 UI。 | 满足 |
| GEN-016 文档可验证性 | 弱适用 | 两文件不是说明文档。`documentVersion` 与 `version` 未当完成声明。调用链上的 README 不在本清单。 | 满足 |
| GEN-017 业务来源分类 | 适用 | 见 §4。顺序：冲突→细化→技术性→添加。无冲突、无添加。BSM1020／BSM1068 不在本两文件中，**不重判**。 | 满足 |
| GEN-018 第一性原理证据 | 适用 | 结论落到 HTML 原文、可执行产物、可复现命令输出。不引用旧同号报告、不引用「惯例」。 | 满足 |

---

## 4. 四分类（冲突 → 细化 → 技术性 → 添加）

**冲突：** 无。`.gitignore` 与 `package.json` 未改写票种、状态、价格、权限、删除审批等 HTML 明文。忽略运行态目录 ≠ 删除业务数据（配置主数据删除仍以 HTML 为准）。

**细化**（有 HTML 上位，完整原文）：

1. `backend/data/bar-images/`（`.gitignore:53-54`）← `docs/business-system-map-for-operations-v0.1.html:953-959`「平板图形点单（POS）」：
   > 进入带商品图片的分类菜单……商品分类、商品图片、套餐都在后台维护：分类可自定义、图片后台上传保存、套餐由后台从已有商品组合而成。
   忽略规则只阻止把运行态上传图片提交进 git，不发明分类／套餐规则。无工程缺陷。

2. `backend/data/evidence-files/`（`.gitignore:55`）← `docs/platform-governance-map-for-operations-v0.1.html:892-907`「留痕和复核／必须留痕的动作」含「查看视频证据和查看财务敏感信息」。忽略运行态证据文件目录 ≠ 取消留痕义务。无工程缺陷。

3. `verify:html-ledger-sync`／`verify:entry-anchors`／`verify:html-param-tier`／`verify:entry-doc-baseline`（`package.json:32-36`）← 三份 HTML 解释权声明：
   - BEQR `docs/business-enum-quick-reference-v0.1.html:23`：「本表与《业务系统地图》《平台治理地图》共同组成三份最高业务解释权 HTML；只有经批准写入这三份 HTML 的内容才是最终业务口径，PRD、其他文档和后台配置均不得覆盖。」
   - BSM `docs/business-system-map-for-operations-v0.1.html:2003` 页脚解释权声明（同口径，含确认时间线）。
   - PGM `docs/platform-governance-map-for-operations-v0.1.html:974` 页脚解释权声明（同口径）。
   - BSM `:1980`：「后台配置只能承载规则，不能改变三份最高解释权 HTML 的业务口径」。
   这些 npm 入口是机审「引用／台账／参数层级是否仍逐字贴住 HTML」，不是新业务规则。门禁对现有坏输入（CSV 镜像 6 项；工作区脏 BSM）**正确拒绝**，不构成入口拼写／转发错误。

4. `verify:pii-encryption`（`package.json:97`）← PGM `:868-871`：「客人和员工的手机号、身份信息等个人信息，在系统里一律加密存放。」入口名称与包装器在其他模块；本文件只声明 `node tools/verify-pii-encryption.mjs`。无业务发明。

5. `verify:tenant-isolation`／`verify:data-isolation-spec`（`package.json:95-96,137-138`）← PGM `:447-448`：「数据隔离仍是综合体级……数据范围没有后台设置项。」入口不在本文件里改隔离粒度。

6. `.env` 忽略 ← PGM `:736`「不得由代码或部署脚本代为「视同打开」」与 `:870` 个人信息加密：把本机密钥文件留在仓外，不是新开关默认。

**技术性**（无业务发明：构建／依赖／CI／忽略规则不是自动发现了业务需求）：

- `.gitignore:1-51`：`node_modules`、`dist`、`backend/target`、覆盖率、Gradle／Kotlin／class／apk、Playwright 报告、tsbuildinfo、security-scans 日志、根目录 `/debug*.mjs` 等探针、`.loop-engineering/`。用途是仓干净与防误提交。
- `package.json` 元数据、workspaces、engines、packageManager、devDependencies。
- 其余 `dev:*`／`build`／`typecheck`／`android:*`／`docs:*`／`verify:*`／`prepare:*`／Playwright 入口：工程可构建、可门禁、可转发。`verify:delivery-gates` 超长 `&&` 链是既有聚合，不是本席新增。

对照 BSM `:566-570`「配置数据可以存档、也可以删除（删除要审批）」：本模块 **没有** 把删除审批做成脚本或忽略规则，也没有用 gitignore 冒充软删除。

**添加：** 无。无新业务实体、枚举、状态、价格、流程。

---

## 5. 对抗式线（grilling 自问自答）＋命令与真实输出

**Q1** 入口是否存在、失败是否传递？  
➡️ 缺失脚本 exit 1；工作区缺失脚本 exit 1；`&&` 短路成立。

**Q2** 工作区转发是否真执行工作区脚本？  
➡️ `npm run --workspace apps/web` 列出 `apps-web@0.1.0` 六脚本；`npm run typecheck --workspace apps/web -- --help` 实际拼出 `tsc -b --noEmit --help`，打印 `Version 5.9.3`，exit 0。

**Q3** `REAL_BACKEND_E2E=1` 前缀在 npm 默认 POSIX `sh -c` 下是否生效？  
➡️ `sh -c 'REAL_BACKEND_E2E=1 node -e "process.exit(process.env.REAL_BACKEND_E2E===\\"1\\"?0:2)"'` exit 0；无该变量时 exit 0（断言「不等于 1」）。未启动 Playwright `webServer`（会起 Vite，属长驻服务）。`playwright.real-backend.config.ts:3-6` 无该变量则 throw——只读源。

**Q4** gitignore 是否按声明匹配、是否误伤受审／权威文件？  
➡️ `git check-ignore -v` 对 30+ 正例命中声明行；`.env.example`、`package.json`、三份 HTML、`.scratch/x`、嵌套 `debug-*.mjs` **不被**忽略（与注释「仓库根」及审计提示词 1.3 一致）。`apps/android-terminal/build` 命中 `**/build/`。无已跟踪被忽略文件。

**Q5** 门禁红是否等于 package.json 写错？  
➡️ 否。`verify:entry-anchors` 明确 6 处 CSV／账本与 HTML 现文对不上（回访句＋文档版本），与派发已知事实一致；相关 CSV 属 M425/M427。`verify:html-ledger-sync` 因工作区 BSM 相对 `origin/main@242f4872` 未同步台账而红——门禁按设计拒绝。二者都 **exit 1 且信息可定位**，不是入口缺失或 `&&` 吞错。

**Q6** 运行时配置是否被写成新业务规则？  
➡️ 脚本参数为 `--check`／`--strict`／`--workspace`／Maven／Gradle 目标。无票种／折扣／BSM1020／BSM1068 取值。不重判那两处。

**Q7** `verify:predeploy` 整链是否全绿？  
➡️ **不得冒称全绿**。链首即 `verify:html-ledger-sync`（本环境红）`&&` `verify:entry-anchors`（红 6）。本席未跑完整 predeploy／delivery-gates／p0。

### 5.1 实跑命令与输出（边界内）

**A. 行数与 JSON**

```
git ls-files -z -- .gitignore package.json | xargs -0 cat | wc -l
# 202
node -e '…require("./package.json")…'
# scriptCount 127；workspaces ["apps/web"]；engines node ^22.13.0 || >=24.0.0
node -v   # v24.20.0
npm -v    # 11.19.0
corepack npm -v  # 10.9.8
```

**B. 缺失脚本／失败传递**

```
npm run this-script-does-not-exist
# npm error Missing script: "this-script-does-not-exist"
# EXIT_MISSING:1

npm run this-missing --workspace apps/web
# npm error Missing script: "this-missing"
# EXIT_WS_MISS:1

npm run verify:html-ledger-sync && echo SHOULD_NOT_PRINT
# [html-ledger-sync] FAIL: 改动了业务最终解释权 HTML，但决策台账未同步。… origin/main@242f4872
#   - docs/business-system-map-for-operations-v0.1.html
# （无 SHOULD_NOT_PRINT）EXIT_CHAIN:1
```

**C. 工作区转发**

```
npm run --workspace apps/web
# Scripts available in apps-web@0.1.0 via `npm run`:
#   dev / build / typecheck / preview / test:unit / test:unit:watch
# EXIT_WS_LIST:0

npm run typecheck --workspace apps/web -- --help
# > tsc -b --noEmit --help
# Version 5.9.3
# EXIT_TSC_HELP:0
```

**D. 无副作用包装器**

```
npm run verify:backend-env
# [backend-env] 后端环境前置校验通过。
# - … Java …
# - system Maven: Apache Maven 3.9.11 …
# EXIT_BE:0
# （源码：--check 时不 spawn Maven 目标）

npm run verify:android-env
# [android-env] Android P0 环境前置校验通过。
# - system Gradle: ------------------------------------------------------------
# - Android SDK: /home/james/android-sdk
# EXIT_AND:0
# （--check --require-android-sdk；未执行 clean/build/assemble）

npm run verify:android-p0-smoke-wrapper
# ANDROID_P0_SMOKE_WRAPPER_PASS
# EXIT_WRAP:0

npm run verify:ops160-backup-restore
# 160 backup and restore script verification passed.
# EXIT_BACKUP:0
# （只读 ops/160/*.sh 字符串；不跑 backup/restore/dropdb）

npm run verify:prod-docs-access
# production OpenAPI/Swagger docs access policy verification passed.
# EXIT_DOCS:0

npm run verify:migration-version-order
# MIGRATION_VERSION_ORDER_PASS: 本分支未新增迁移(origin/main 最大号 V399)
# EXIT_MIG:0

npm run verify:html-param-tier
# [html-param-tier] PASS: 改动了 1 份业务 HTML，全量重扫无基线外的未标层级句…
# EXIT_TIER:0
```

**E. 已知红门禁（有效断言，非入口错误）**

```
npm run verify:entry-anchors
# [entry-anchors] 注册表 1228 行；账本 1235 行；承接矩阵 1244 行…被检 Markdown 358 份…
# [entry-anchors] FAIL：6 处不合格。
#   - 注册表条目正文在 HTML 现文中找不到：BSM-E0433 BSM「处理后回访客人或商户，验收合格才关闭」
#   - 注册表条目正文在 HTML 现文中找不到：BSM-E0736 BSM「文档版本」
#   - 账本正文与 HTML 现文对不上：BSM-0283「处理后回访客人或商户，验收合格才关闭」
#   - 账本正文与 HTML 现文对不上：BSM-0530「文档版本」
#   - 账本镜像列… BSM-0283 … preconditions …
#   - 账本镜像列… BSM-0283 … state_transition …
# EXIT_EA:1
```

处置归属：CSV／账本镜像（M425/M427）。**不改 CSV／HTML／忽略范围以求绿。**

**F. gitignore 实匹配（节选，类型 4）**

正例均 `git check-ignore -v` 命中 `.gitignore` 对应行：`node_modules/foo`→L1，`dist/a`→L2，`backend/target/x`→L3，`.env`→L4，`.env.local`→L5，`*.db`／`*.db-journal`→L6-7，`coverage`→L8，`.vite`→L9，`.gradle`／`**/.gradle/`→L11，`.kotlin`→L13，`build/`→L15，`*.class/*.dex/*.apk/*.aab`→L16-19，`local.properties`→L21，`playwright-report`／`test-results`／`playwright-report-real-backend`→L23-26，`.claude/settings.json`→L29-30，`*.tsbuildinfo`→L32，`security-scans/**/*.log`→L34，`security-scans/semgrep-current/`→L36，`docs/_archive/**/*.log`→L39，`.loop-engineering/`→L42，根 `/debug*.mjs` 等→L45-51，`backend/data/bar-images/`→L54，`backend/data/evidence-files/`→L55。

负例（无输出＝不忽略）：`.env.example`、`package.json`、`tools/verify-entry-anchors.mjs`、三份权威 HTML、`.scratch/x`、`nested/debug-foo.mjs`。

**G. Playwright 本地二进制**

```
node node_modules/@playwright/test/cli.js --version
# Version 1.61.0
# EXIT_PW:0
```

**H. 目标文件存在性（类型 4，机器枚举）**

75 个唯一 `tools/*.mjs` 全部 `os.path.isfile` 为真；另 `playwright.config.ts`、`playwright.real-backend.config.ts`、`tools/verify-formal-security-baseline.mjs` 存在。`npm run` 链 0 未解析。

### 5.2 明确未跑范围

| 入口 | 原因 | 本席如何验证 |
|---|---|---|
| `dev`／`dev:backend`／`dev:api` | `spring-boot:run` 长驻 | 读 `run-backend-maven.mjs`：无 `--check` 时 `spawnSync(mvn, […goals])` |
| `test:backend`／`smoke:p0`／`verify:closed-loop-runtime` 中的 maven test | 全量测试／DB | 入口存在；未 export 三个 DB 变量去跑 |
| `android:p0:build`／`apk`／`smoke` | 构建／起后端／写 `build/` | `--check` 已跑；build 参数只读 |
| `playwright test`／`verify:ui`／`verify:ui:real-backend` | 浏览器／可能起 Vite | config 存在；env 前缀用 `sh -c` 复现；未起 webServer |
| `docs:*`／无 `--check` 的 generate | 写业务文档源 | 读脚本 `writeFileSync` 位于非 check 分支 |
| `verify:predeploy` 整链 | 首环已红且后续含重门禁 | `&&` 短路已证；不冒称全绿 |
| 部署／restore／dropdb | 有删除／外发作用 | backup-restore 校验只读脚本文本 |

---

## 6. `.gitignore` 与 `package.json` 全覆盖表

### 6.1 `.gitignore` 1–55

| 行 | 规则 | 用途 | 分类 |
|---|---|---|---|
| 1 | `node_modules` | 依赖树 | 技术性 |
| 2 | `dist` | 前端构建 | 技术性 |
| 3 | `backend/target` | Maven 输出 | 技术性 |
| 4–5 | `.env` `.env.local` | 本机密文 | 技术性 |
| 6–7 | `*.db` `*.db-journal` | 本地库文件 | 技术性 |
| 8–9 | `coverage` `.vite` | 测试／Vite 缓存 | 技术性 |
| 10–15 | `.gradle` `**/.gradle/` `.kotlin` `**/.kotlin/` `build` `**/build/` | Android／Kotlin／通用 build | 技术性 |
| 16–19 | `*.class` `*.dex` `*.apk` `*.aab` | 编译产物 | 技术性 |
| 20–21 | `local.properties` `**/local.properties` | Android SDK 本机路径 | 技术性 |
| 23–26 | Playwright 报告与 `test-results` | 测试产物（与提示词 §1.2 排除前缀一致） | 技术性 |
| 28–30 | `.claude/settings.json` 等 | 机器相关绝对路径 | 技术性 |
| 32 | `*.tsbuildinfo` | TS 增量缓存 | 技术性 |
| 34–36 | `security-scans/**/*.log`、`semgrep-current/` | 滚动扫描产物 | 技术性 |
| 39 | `docs/_archive/**/*.log` | 归档日志 | 技术性 |
| 42 | `.loop-engineering/` | 工具运行产物 | 技术性 |
| 44–51 | 根目录探针／冒烟脚本 | 防误提交非正式交付 | 技术性 |
| 53–55 | `bar-images/` `evidence-files/` | 运行态上传 | 细化（§4） |

空行 22、27、37、41、43、52 无规则，已读。

### 6.2 `package.json` 1–147

| 行 | 内容 | 分类 |
|---|---|---|
| 1–9 | name／private／version／documentVersion／type／workspaces | 技术性 |
| 10–138 | 127 scripts | 技术性；HTML 门禁类为细化（§4） |
| 139–142 | packageManager／engines | 技术性 |
| 143–146 | devDependencies playwright＋yaml | 技术性（`yaml` 被 `apply-openapi-p0-examples.mjs` 等引用） |
| 147 | 闭合 `}` | — |

脚本分组（全部目标已核存在）：

- 后端：`dev`→`dev:backend`→`run-backend-maven.mjs`；`test:backend`；`verify:backend-env --check`；`dev:api` 别名。
- Web 工作区：`dev:web`／`build`／`typecheck`／`test:unit`／`verify:unit` → `npm run … --workspace apps/web`。
- OpenAPI／客户端契约：`verify:openapi` 等 12 条 → `tools/verify-*`／`audit-*`／`generate-* --check`。
- HTML 治理：`verify:html-ledger-sync`、`verify:entry-anchors`、`verify:html-param-tier`、`verify:entry-doc-baseline`、`docs:entry-baseline`。
- 安全／P0：`verify:security-baseline`（先 `verify:prod-docs-access`）、`verify:p0-contract`、`verify:p0`、`verify:coding-host-protection` 等。
- 安卓：`verify:android-env`、`android:p0:build`（`--project apps/android-terminal`）、`android:p0:apk`（`--project apps/android-terminal/android-app`）、`android:p0:smoke`、wrapper 校验。两套 `--project` 对应仓内两份 Gradle 工程，目录均存在。
- 交付闸：`verify:delivery-gates-docs` 与超长 `verify:delivery-gates`；`verify:predeploy` 为 CI 消费的较短链（含 HTML 三门禁，**不含** `verify:migration-version-order`）。
- 生产证据／NAS／闭环：其余 `docs:`／`verify:`／`prepare:` 对，`--check`／`--verify` 在对应工具源码中分支，避免校验写源。
- UI：`test:ui`＝`verify:ui`＝`playwright test`；`verify:ui:real-backend` 环境前缀；`verify:ui:real-backend:seeded` 包装器（会起后端，未跑）。

CI 调用点（只读，不审 M005 正文）：`.github/workflows/predeploy-gates.yml:24-27` `npm run verify:predeploy`。本地 `pre-push` **不**跑 npm 门禁，只拦直推 main。

---

## 7. ponytail-review（只报告，不改）

- `package.json:15` yagni: `dev:api` 纯转发 `dev:backend`。别名，首跑无害。
- `package.json:48` yagni: `smoke:p0` 纯转发 `test:backend`。
- `package.json:82-83` shrink: `test:ui` 与 `verify:ui` 同命令；闭环链引用后者。
- `.gitignore:14-15` shrink: `build` 与 `**/build/` 重叠，实匹配落在 L15。

`net: -0` 强制删除。不为别名 FAIL。不建议为「干净」改 SemVer 或加忽略变体。

codebase-design（冻结 general）：根 `package.json` 是宽浅接口（127 个 script 名），深度在 `tools/*.mjs`（其他模块）。删除测试：删掉本文件，调用方（CI、人）失去统一入口。不在本模块新开缝。

---

## 8. 本席临时 findings

无冲突。无添加。无工程缺陷。下列为 **GEN-017 技术性／细化登记**（工程缺陷＝否，建议队列＝否）。B2 若归并，只登记不修。

| 临时号 | 位置 | 摘要 | 2.1 | 2.3 类型 | GEN | 工程缺陷 | 队列 |
|---|---|---|---|---|---|---|---|
| GR-01 | `.gitignore:1-51` | 构建／依赖／缓存／探针／扫描产物忽略 | 技术性（无业务发明：仓卫生） | 4 | 001,010,013 | 否 | 否 |
| GR-02 | `.gitignore:53-55` | 运行态酒吧图片与证据目录忽略 | 细化（BSM:953-959；PGM:892-907） | 1+4 | 017 | 否 | 否 |
| GR-03 | `package.json:1-9,139-146` | 私有包、工作区、引擎、devDependencies | 技术性 | 4 | 013,008 | 否 | 否 |
| GR-04 | `package.json:10-138` 开发／构建／安卓／测试入口 | Maven／Gradle／workspace／Playwright 声明 | 技术性 | 4 | 002,013 | 否 | 否 |
| GR-05 | `package.json:32-36` 等 HTML 门禁入口 | 解释权机审入口；现输入上红属正确拒绝 | 细化（BEQR:23；BSM:2003,1980；PGM:974） | 1+2+4 | 002,009,017 | 否 | 否 |
| GR-06 | `package.json:61,96` 等交付／隔离／迁移入口 | CI／交付聚合与 V 号顺序入口 | 技术性；隔离入口另见 PGM:447 | 4 | 007,014 | 否 | 否 |

无 GR 项需要修复。不因 `verify:entry-anchors`／`verify:html-ledger-sync` 红而把 package.json 标缺陷。不重开 M001／M002。不重判 BSM1020／BSM1068。

---

## 9. 覆盖核对

- 文件：2／2，行 202／202。
- GEN：001–018 逐项表在 §3。
- 三线：第一性原理 §2；顺从式 §3；对抗式 §5。
- 四分类：§4。
- 技能：§0。
- 旧 322 同号报告：仅历史；本票独立。

---

## 10. 本席 B1 结论

**PASS。**

无 HTML 自身问题需按铁律 4 上报。已知 6 项 entry-anchors 红保留在 M425/M427。工作区脏 BSM 使 ledger-sync 红，不重开 M001。

本票不是模块收口。下一步不由本席执行。

# M003 / B2 整批候选复核（Grok 席乙）

- 报告自身版本：V0.05（追加；保留本文件既有 V0.01–V0.04 全部字节，不覆盖）
- 席位：Grok（唯一外审），无子代理／第三席
- 步骤：B2 整批归并复核（首次 B2：两份 b1 raw 的本次完整报告 + 候选台账差集）
- 模块：M003 — 治理门禁与 CI 基线／配置与工程入口／第1段（`plan/modules.md` V0.04；仅 `.gitignore` 55 行 + `package.json` 147 行，有效行 202）
- 控制：160-audit-prompt.md V0.17
- ledger：M003 `IN_PROGRESS`，B3 累计 2 不清零；本席不写独占 `CRITERIA-LOCK-SHA256:` 字段行
- 本追加职责：差集／四分类／工程缺陷／修复队列；禁止修复、禁止改 findings／HTML／源码／ledger／lock
- 本席 B2 结论：**FAIL**

本票不是模块 PASS，不是 B3 授权，不是有条件通过。

## 0. §6 技能自核（本节点实核；收窄口径）

| 技能 | 路径核验 | 本次用法 |
|---|---|---|
| `using-superpowers` | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能路由 |
| `receiving-code-review` | `.../superpowers/6.3.0/skills/receiving-code-review/SKILL.md` | 先对照 Git 原文、冻结 GEN-001、V0.17 铁律 6、完整源与两席 B1，再接受或反驳候选；禁止表演性同意；B2 禁止 IMPLEMENT |
| `verification-before-completion` | 同上 superpowers | 任何 PASS/FAIL 前用本追加内命令、哈希、HTML 行与 `/tmp` 忽略对照；未见输出不得宣称 |
| `grilling` / `grill-me` | mattpocock `productivity/grilling`；`grill-me` 仅转发 grilling 且 `disable-model-invocation: true` | 对抗式自问自答；不派子代理、不问操作员 |
| `systematic-debugging` | superpowers | 只借读错误→复现→追数据流；不改生产代码 |
| `diagnosing-bugs` | mattpocock `engineering/diagnosing-bugs` | 只读源／复现命令；判断「重复模式」是否构成可失败缺陷 |
| `ponytail-review` | ponytail 4.9.0 | 可删 ≠ 必须修；B1 `net: -0` 强制删除仍成立 |
| `codebase-design` | mattpocock | 只借用已冻结 general 的接口／深度词汇；不设计新缝 |
| `writing-for-agents` | mattpocock | 本 raw 按下游可差集、可归因来写 |
| `tdd` / `test-driven-development` | 存在 | 纯配置 B2 不适用；未新增测试 |

缺口（如实，不冒充已调用）：`batch-grill-me` 本环境技能目录未找到。

明确不用：子代理类技能、`research`、`ponytail-audit`／`debt`／`gain`、`ponytail` 本体（B3）、`requesting-code-review`（B4）、`using-git-worktrees`、`finishing-a-development-branch`。`git-guardrails-claude-code` 可选，安装会写 hook，本席不装。

未派子代理。未改 HTML／findings／ledger／lock／sidecar／`.gitignore`／`package.json`。未执行 `npm exec`、未安装依赖、未创建 §7 外产物。self-raw 在本席核完 V0.04 前缀后已另有根席 B2 追加；该追加**不是**独立 B1 证据，本票不采信其 PASS 结论。

## 1. 三份输入（完整读取后实算）

均在写入本追加前独立计算。B1 只取各文件本次完整 V0.04 段。

| 输入 | 版本／范围 | 字节 | SHA256（小写 hex） |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M003-b1-self-raw.md` | B1 V0.04：最后独占锁行 L510 起至当时 EOF（现文件前缀 97151 字节；其后根席 B2 V0.05 不是 B1 证据） | 97151 | `946e0fd255f7ca207c2dbedc182c26b5dd5cb0e8292b84f536ab07b408eca268` |
| `docs/audit-2026-09/modules/M003-b1-grok-raw.md` | 本文件写入前全文；B1 V0.04 = L1109—1529（421 行） | 111023 | `a06c894debfb16699c2be581730ce34f85cf59ad26d822a8af2a236ec7b7e95e` |
| `docs/audit-2026-09/findings.md` | 候选 V0.90 | 301010 | `2bc36a0805672ecd5b7801417baeefa0615516350163ae3f3a17de9899ce3c7d` |

与派发声称三值逐字节一致。现 self-raw 全文件 101963 字节，是 V0.04 前缀后再追加根席 B2；差集只用 97151 字节前缀。

只读核对（未改）：

- `.gitignore` 55 行／1199 字节／SHA256 `75861ded5d3859bcda593aafe3f3862e765470e7f8b6636a643bf59de9d7c51b`；无自身 `Vx.xx` 注释
- `package.json` 147 行／15583 字节／SHA256 `350999a90ddf85b2d3dcbee090dd593fb7faff18d0f467f0a608d8b3664d473f`；`documentVersion=V0.01`，应用 `version=0.1.0` 不混作文件自身版本
- `git ls-files -z -- .gitignore package.json \| xargs -0 cat \| wc -l` = **202**
- `HEAD` = `origin/main` = `242f487233d028173cb9198eecd079f5d877405c`；`git diff --numstat -- .gitignore package.json` 空
- 模块行：M003 `IN_PROGRESS`，B3=2，方案修复=2，失败重跑=0
- `docs/audit-2026-09/questions/` 仅历史 `M003-023.md`／`M003-024.md`（及他模块）；本批无新添加项文件

命令：

```text
python3 -c 'import hashlib,pathlib; p=pathlib.Path("docs/audit-2026-09/modules/M003-b1-self-raw.md"); b=p.read_bytes(); print(len(b[:97151]), hashlib.sha256(b[:97151]).hexdigest())'
sha256sum docs/audit-2026-09/modules/M003-b1-grok-raw.md docs/audit-2026-09/findings.md .gitignore package.json
wc -l .gitignore package.json
git ls-files -z -- .gitignore package.json | xargs -0 cat | wc -l
git rev-parse HEAD origin/main
```

B1 原票原样留存：Grok V0.04 **PASS**（无工程缺陷、建议队列空）；根 V0.04 **FAIL**（仅 S-001）。本 B2 不把根 FAIL 预设为定稿结果，也不把本席 B1 PASS 写成模块收口。

## 2. 两席 B1 全部差集 → 候选正式 ID

Grok B1 V0.04 临时号：{GR-01 … GR-06}；工程缺陷全部否，建议队列全部否；另有 §7 ponytail 重叠观察，`net: -0` 强制删除。

根 B1 V0.04 临时号：{S-001} 为唯一工程问题（技术性／缺陷是／建议队列是）；其余 `.gitignore`／`package.json` 全表当时一律技术性、无添加。

| 来源 | 候选 ID | 候选分类 | 候选工程缺陷 | 候选队列 | 本席 B2 |
|---|---|---|---|---|---|
| GR-01 + GR-02 + 根 ignore 全表（S-001 不藏此行） | M003-016 | 1—51 技术性；53—55 细化 | 否 | 否 | **接受** |
| 根 S-001；Grok §7 仅重叠观察 | M003-027 | 技术性 | 是（根候选） | 是（候选） | **拒绝**：缺陷应否、队列应否。此项使整批 FAIL |
| GR-03 | M003-028 | 技术性 | 否 | 否 | **接受** |
| GR-04（HTML／隔离／加密子入口拆出） | M003-029 | 技术性 | 否 | 否 | **接受** |
| GR-05 | M003-030 | 细化 | 否 | 否 | **接受** |
| GR-06 + §4 隔离／加密子入口 | M003-031 | 交付／迁移聚合技术性；隔离／PII 核验细化 | 否 | 否 | **接受** |

并集：Grok 六条全部入账；根唯一新缺陷单独成 027。无漏并 GR-01—06。无把旧 018（AGENTS／README 文字）复用成 package 实体。新号 027—031 在历史段不存在（历史末号 026），不复用 001—026。旧 023 删除／024 保留人闸不重开。

Grok 原 GR 全文仍在本文件 V0.04；候选未改写其「工程缺陷否／队列否」。027 是根新增项，不是改写本席 B1。

## 3. 逐项独立核实

### 3.1 M003-016 — 接受；`.env` 退回技术性正确

GR-01（`.gitignore:1-51`，含 `.env`／`.env.local`）原分类技术性、缺陷否、队列否。GR-02（`:53-55` bar-images／evidence-files）原分类细化、缺陷否、队列否。候选 016 按行切开，与本席覆盖表及临时号一致。

本席 B1 §4 第 6 点曾把 `.env` 忽略宽泛关联 PGM:736「不得由代码或部署脚本代为『视同打开』」与 PGM:870 个人信息加密。B2 按 2.1 重核：

- PGM:736 原文是开关缺行不得视为开启、不得由代码或部署脚本视同打开。忽略 `.env` 不执行开关、不设默认打开、不改授权。
- PGM:870 原文是「客人和员工的手机号、身份信息等个人信息，在系统里一律加密存放。」忽略本机密文文件不是加密存放，也不是脱敏／查询通道。
- 能指向上位 = 该实现是那条 HTML 的下位细节。此处只是仓卫生（GEN-010／013），指不出开关或加密规则。顺序走到技术性。

故 §4 第 6 点是过宽关联，**不以它覆盖 GR-01**。候选把 `.env` 留在 016 技术性段，不另造添加项，正确。本席不改判为添加。

53—55 上位原文本席实读，与候选粘贴一致：

- BSM:959：「商品分类、商品图片、套餐都在后台维护：分类可自定义、图片后台上传保存、套餐由后台从已有商品组合而成。」忽略运行态目录 ≠ 新套餐规则。候选不用本席 B1 省略号，改粘完整单句，不是改写意见。
- PGM:893：「平台治理层要保证每个重要动作能追溯。谁发起、谁销售、谁服务、谁第一个处理异常、为什么、谁审批、结果是什么，都要能查到。」
- PGM:907：「查看企业微信会话原文、查看视频证据和查看财务敏感信息。」

忽略 Git 提交 ≠ 取消留痕或权限。无工程缺陷。根 B1 曾把 52—55 一律技术性，候选按 2.1「能指到则细化」改就 Grok 划分，是根席修正，不是改写 GR-02。

### 3.2 M003-027 — 拒绝工程缺陷=是、拒绝队列=是（本批唯一 FAIL 依据）

**已核事实（规范 + 命令，类型 3+4）：** `.gitignore` 现行 10/11、12/13、14/15、20/21 四对，中间无 `!`。`git help gitignore` 原文：无前导／中间斜线的名称可在任意层匹配；「A leading `**` followed by a slash means match in all directories. For example, `**/foo` matches file or directory `foo` anywhere, the same as pattern `foo`.」；末尾 `/` 只把匹配收窄为目录。

本席在 `/tmp/gi-m003-b2` 另建空库对照（不改正本）：

- 保留 8 行与只留 `.gradle`／`.kotlin`／`build`／`local.properties` 四条：同一组相对路径全部仍被忽略，只是 `check-ignore` 命中行号前移。
- 只留四条 `**/…`：对「目录名本身、且样本路径不以目录实体存在」的 check-ignore 更窄（末尾 `/` 只配目录）。故 11/13/15/21 相对 10/12/14/20 是子集或等价，不是多出来的保护。

重叠观察成立。根建议若修则删 11/13/15/21、保留前条，方向与规范一致。这只证明「可删」，不证明「必须列为工程缺陷并入队」。

**冻结 GEN-001 原文：**「每项实现都须有可验证用途；重复、不可达、纯转发或无需求承载的内容应删除或收窄，不为假设场景增加抽象。」四条后置模式各自仍能匹配，不是不可达、不是无需求承载。它们与前条重叠。GEN-001 的「应删除或收窄」是若动手时的修法，不是「凡重叠即本轮 B3」。

**V0.17 铁律 6：**「这条路径第一次正常跑就会撞上吗？会 → 修。要两三件不巧的事同时发生才成立 → 不修，在 ledger.md 备注一行即可。」「为了稳健／以防万一／顺便兜个底」不是理由。首次正常 `git status`／`git add`／clone 不会因这四行失败或漏忽略。根把「每次读取文件都看见重复」说成「首次必撞」，把文本存在等同于失败路径被踩中，不是铁律 6 的口径。

**2.1 技术性处置：** 内容本身不删不问；**查出工程缺陷的才进修复队列**，无缺陷只登记。工程缺陷须是正确性／安全／一致性上已证失败或首次正常运行会踩中的实现错误。忽略集合不因后四行改变；`git ls-files -c -i --exclude-standard` 在本席 B1 已为空。这不是 GEN-002／009／010 失败。

**不得用根结论改本席票。** 本席 B1 GEN-001 记满足；§7 只报 `.gitignore:14-15` shrink，并写 `net: -0` 强制删除；GR-01 缺陷否、队列否。B2 用规范与命令复核后，重叠仍只是观察。接受 027 缺陷=是／队列=是 = 为迎合根改票。拒绝。

**与根自己豁免别名同一把尺：** 根不把 `dev:api`／`smoke:p0`／`test:ui`=`verify:ui` 入队，理由是已有调用契约、删之会伤入口。四条忽略同样「仍在做事」，只是不是唯一匹配者。不能一边留别名、一边把等价忽略升成唯一 S-001 缺陷。

**更小合规处置（只指出，B2 不修）：** 保留 027 号作重叠观察，或并入 016 备注；工程缺陷=否，队列=否。不改 `.gitignore`。不为未改文件补版本。若将来真改该文件，再按版本铁律处理。

**版本铁律（假设题，因拒绝修复故不触发）：** 操作员原文「所有版本号统一使用 Vx.xx 格式；任何对话中只要更新文件内容，该文件自身版本号末位必须前进一位」。现行 `.gitignore` 无自身版本。若且仅若 B3 改了该文件，首次建立 `# 版本：V0.01`（或等价注释）符合铁律；`#` 是 gitignore 合法注释。不得在未改内容时单为「合规版本」改文件。不得把应用 SemVer 或 package `documentVersion` 写进 `.gitignore`。

按 B2 定稿规则：有工程缺陷 → 队列必须是。因此不能用「缺陷是、队列否」折中。必须两者都否，或从队列拿掉 027。当前候选两者都是 → 整批 FAIL。

### 3.3 M003-028 — 接受

对应 GR-03：私有包、ESM、workspace、engines、packageManager、devDependencies。无业务发明。根 lock 镜像与 B1 已核；宿主 npm 11.19.0 对声明 npm@10.9.8 的差异已披露，未证入口失败。缺陷否、队列否。本席不改判添加。

### 3.4 M003-029 — 接受

对应 GR-04 中开发／构建／Android／测试声明。127 脚本／75 Node 目标在两席 B1 均有存在性与语法证据。不把 `--list`／`--dry`／env `--check` 说成全套业务验收。HTML 门禁与隔离／PII 入口拆到 030／031，避免 GR-04 一行吞掉已指向上位的子项。缺陷否、队列否。

### 3.5 M003-030 — 接受；HTML 原文定位成立

对应 GR-05。`package.json:32-35`（及同组 entry-doc-baseline）是解释权机审入口，不是新业务规则。候选粘贴 BEQR:23 与现文一致：「本表与《业务系统地图》《平台治理地图》共同组成三份最高业务解释权 HTML；只有经批准写入这三份 HTML 的内容才是最终业务口径，PRD、其他文档和后台配置均不得覆盖。」

本席 B1 另引 BSM:1980「后台配置只能承载规则，不能改变三份最高解释权 HTML 的业务口径」、BSM:2003 与 PGM:974 页脚解释权。候选表写「细化三 HTML 解释权」，详细段只展开 BEQR:23。同口径，未改写 GR-05 的细化／缺陷否／队列否。定稿时可补粘 1980／2003／974，不构成本次额外 FAIL。

`verify:html-ledger-sync` 因工作区 BSM 相对 `origin/main@242f4872` 未同步决策台账而红；`verify:entry-anchors` 6 处为 BSM CSV 镜像（回访句 + 此前页脚日期，非 D 版本注释），属 M425／M427。门禁对坏输入正确阻断。不改 CSV／HTML／package 刷绿，不重开 M001／M002，不重判用户已确认的 B／C 原意。

### 3.6 M003-031 — 接受；技术性／细化分段精确

对应 GR-06「技术性；隔离入口另见 PGM:447」加上本席 §4 第 4、5 点。候选把 package:61／96 等交付／predeploy 聚合留技术性，把 95／97／137 核验入口按已有 HTML 划细化：

- PGM:870：「客人和员工的手机号、身份信息等个人信息，在系统里一律加密存放。」→ `verify:pii-encryption` 只声明 `node tools/verify-pii-encryption.mjs`。
- PGM:447：「集团下钻细则（已确认）：……数据隔离仍是综合体级，只有集团高层按其职位模板可跨综合体查看。职位配置模板只决定汇总与下钻到哪一层，不是可自定义的数据范围；数据范围没有后台设置项。」PGM:448 同隔离口径。入口不在本文件改隔离粒度。

无新价格／枚举／权限默认值。缺陷否、队列否。这是对 GR-06 混合行的精确拆分，不是改写原「不修」意见。

## 4. GR-01—06 合并完整性

| 临时号 | 原意见（分类／缺陷／队列） | 候选落点 | 改写？ |
|---|---|---|---|
| GR-01 | 技术性／否／否 | 016 的 1—51 | 否；`.env` 宽泛关联未升级为正式分类 |
| GR-02 | 细化／否／否 | 016 的 53—55 | 否；原文由省略号改为完整句 |
| GR-03 | 技术性／否／否 | 028 | 否 |
| GR-04 | 技术性／否／否 | 029（HTML／隔离／PII 子入口让给 030／031） | 否；细化先于技术性的拆分 |
| GR-05 | 细化／否／否 | 030 | 否 |
| GR-06 | 技术性，隔离另见 PGM:447／否／否 | 031 | 否 |

无漏并。无把本席「无工程缺陷」改成待修。027 不是 GR 项。

## 5. 添加项、人闸、全局红灯、调用边界

- 本批无添加。不写新 questions，不停 `BLOCKED-ASK`。历史 023／024 已答，不重开。
- 无新的铁律 4 HTML 申请。三份 HTML 只读核对原句，未改。
- 无锁外新判据，不走 LOCK-AMEND。
- html-ledger-sync 首门禁失败与 entry-anchors 6 个 BSM CSV 镜像错误准确保留；不靠改它们刷绿。
- 本席 B1 曾出现 `npm exec -w apps/web -- node` 将安装 `node@26.8.1` 的警告。本次**未再执行**该命令，不伪证「绝无缓存下载」，只记录：当时 `package-lock.json` 未改、工作区未见残留 `node_modules/node`；其余入口证据以 `npm run --workspace` 为准。

## 6. 授权与队列收口

当前候选修复队列仅 027=是。本席裁定 027 不得入队。016／028／029／030／031 = 否，可定稿为只登记。

因 027 的缺陷／队列与本席独立裁决不一致，整批候选不得定稿，不得进 B3。按 V0.17 B2 ④：FAIL 后按本席意见改候选再派复核。建议改法：027 工程缺陷改否、队列改否（或并入 016 备注并删 027 行的「待修」）；其余五行保持。改完后的字节／SHA 须重核，本票不预支下一轮 PASS。

## 7. B2 结论

**FAIL**

不是有条件通过。失败点唯一且明确：M003-027 把已证的 gitignore 重叠观察写成工程缺陷并进入修复队列，违反本席 B1 原票、V0.17 铁律 6 首次正常运行／最小修复口径，以及 2.1「技术性无缺陷只登记」。重叠本身、Git 规范子集关系、若修则删后四条的方向，均予承认，但承认不等于入队。

016、028—031 的归并、四分类、HTML 原句与「无缺陷则队列否」成立；旧 ID 未复用；无添加、无人闸遗漏。这些不抵销 027。

本节点结束。不进入 B3／B4／B5，不改源，不新增文件。

# M003 / B2 整批修订候选复核（Grok 席乙）

- 报告自身版本：V0.06（追加；保留本文件既有 V0.01–V0.05 全部字节，不覆盖）
- 席位：Grok（唯一外审），无子代理／第三席
- 步骤：B2 整批归并复核（首次 B2 的修订轮：同一对 b1 V0.04 完整报告 + 当前唯一候选台账差集）
- 模块：M003 — 治理门禁与 CI 基线／配置与工程入口／第1段（`plan/modules.md` V0.04；仅 `.gitignore` 55 行 + `package.json` 147 行，有效行 202）
- 控制：160-audit-prompt.md V0.17
- ledger：M003 `IN_PROGRESS`，B3 累计 2 不清零；本席不写独占 `CRITERIA-LOCK-SHA256:` 字段行
- 本追加职责：差集／四分类／工程缺陷／修复队列；禁止修复、禁止改 findings／HTML／源码／ledger／lock
- 当前唯一队列：findings.md 最上方「当前 M003 B2 整批修订候选」V0.91。下方 V0.90 为上批已被本席否决的历史候选，不作现队列
- 本席 B2 结论：**PASS**

本票不是模块 PASS，不是 B5 收口，不是有条件通过。根 self-raw 在 V0.04 前缀之后的 B2 附录是根当前票，**不是**独立 B1 证据，本票不采信其结论。

## 0. §6 技能自核（本节点实核；收窄口径）

| 技能 | 路径核验 | 本次用法 |
|---|---|---|
| `using-superpowers` | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能路由 |
| `receiving-code-review` | 同上 superpowers | 先对照 Git 原文、冻结 GEN-001、V0.17 铁律 6、完整源与两席 B1 及本席 V0.05 唯一否决，再接受或反驳候选；禁止表演性同意；B2 禁止 IMPLEMENT |
| `verification-before-completion` | 同上 superpowers | 任何 PASS/FAIL 前用本追加内命令、哈希、HTML 行与现文件对照；未见输出不得宣称 |
| `grilling` / `grill-me` | mattpocock `productivity/grilling`；`grill-me` 仅转发 grilling 且 `disable-model-invocation: true` | 对抗式自问自答；不派子代理、不问操作员 |
| `systematic-debugging` | superpowers | 只借读错误→复现→追数据流；不改生产代码 |
| `diagnosing-bugs` | mattpocock `engineering/diagnosing-bugs` | 只读源／复现命令；判断重叠是否构成可失败缺陷 |
| `ponytail-review` | ponytail 4.9.0 | 可删 ≠ 必须修；B1 `net: -0` 强制删除仍成立 |
| `codebase-design` | mattpocock | 只借用已冻结 general 的接口／深度词汇；不设计新缝 |
| `writing-for-agents` | mattpocock | 本 raw 按下游可差集、可归因来写 |
| `tdd` / `test-driven-development` | 存在 | 纯配置 B2 不适用；未新增测试 |

缺口（如实，不冒充已调用）：`batch-grill-me` 本环境技能目录未找到。

明确不用：子代理类技能、`research`、`ponytail-audit`／`debt`／`gain`、`ponytail` 本体（B3）、`requesting-code-review`（B4）、`using-git-worktrees`、`finishing-a-development-branch`。`git-guardrails-claude-code` 可选，安装会写 hook，本席不装。

未派子代理。未改 HTML／findings／ledger／lock／sidecar／`.gitignore`／`package.json`。未执行 `npm exec`、未安装依赖、未创建 §7 外产物。未新建 `/tmp` 忽略试验库。本次无操作员闸。

## 1. 三份输入（完整读取后实算）

均在写入本追加前独立计算。B1 只取各文件本次完整 V0.04 段。当前 grok-raw 写入前全文即上批 V0.05 落盘态。

| 输入 | 版本／范围 | 字节 | SHA256（小写 hex） |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M003-b1-self-raw.md` | B1 V0.04：最后独占锁行 L510 起至当时 EOF（现文件前缀 97151 字节；其后根席 B2 不是 B1 证据） | 97151 | `946e0fd255f7ca207c2dbedc182c26b5dd5cb0e8292b84f536ab07b408eca268` |
| `docs/audit-2026-09/modules/M003-b1-grok-raw.md` | B1 V0.04 = L1109—1529（写入前全文件前缀 111023 字节） | 111023 | `a06c894debfb16699c2be581730ce34f85cf59ad26d822a8af2a236ec7b7e95e` |
| 本文件上批 B2 | V0.05 完整 L1531—1726；写入前全文件 | 129530 | `565f858e9e2f8df6868e31e9f9ba50385fc337f0d5d00fe0909617247a50f9c3` |
| `docs/audit-2026-09/findings.md` | 当前候选 V0.91（最上方「当前 M003 B2 整批修订候选」为唯一现队列） | 308685 | `ce0ade9e7aeeb22897bf4873e9e1b8681f8dc1abb1820c592a4dc4019fe128c5` |

与派发声称四值逐字节一致。现 self-raw 全文件 105805 字节，是 V0.04 前缀后再追加根席 B2；差集只用 97151 字节前缀。

只读核对（未改）：

- `.gitignore` 55 行／1199 字节／SHA256 `75861ded5d3859bcda593aafe3f3862e765470e7f8b6636a643bf59de9d7c51b`；无自身 `Vx.xx` 注释
- `package.json` 147 行／15583 字节／SHA256 `350999a90ddf85b2d3dcbee090dd593fb7faff18d0f467f0a608d8b3664d473f`；`documentVersion=V0.01`，应用 `version=0.1.0` 不混作文件自身版本
- `git ls-files -z -- .gitignore package.json \| xargs -0 cat \| wc -l` = **202**
- `HEAD` = `origin/main` = `242f487233d028173cb9198eecd079f5d877405c`；`git status --porcelain -- .gitignore package.json` 空
- 模块行：M003 `IN_PROGRESS`，B3=2，方案修复=2，失败重跑=0
- `docs/audit-2026-09/questions/` 仍仅历史 `M003-023.md`／`M003-024.md`（及他模块 `M006-027.md`）；本批无新添加项文件
- `git ls-files -c -i --exclude-standard` 行数 **0**

命令：

```text
python3 -c 'import hashlib,pathlib; p=pathlib.Path("docs/audit-2026-09/modules/M003-b1-self-raw.md"); b=p.read_bytes(); print(len(b[:97151]), hashlib.sha256(b[:97151]).hexdigest())'
python3 -c 'import hashlib,pathlib; b=pathlib.Path("docs/audit-2026-09/modules/M003-b1-grok-raw.md").read_bytes(); print(len(b[:111023]), hashlib.sha256(b[:111023]).hexdigest()); print(len(b), hashlib.sha256(b).hexdigest())'
sha256sum docs/audit-2026-09/findings.md .gitignore package.json
wc -c docs/audit-2026-09/findings.md
git ls-files -z -- .gitignore package.json | xargs -0 cat | wc -l
git rev-parse HEAD origin/main
git status --porcelain -- .gitignore package.json
git ls-files -c -i --exclude-standard | wc -l
```

B1 原票原样留存：Grok V0.04 **PASS**（无工程缺陷、建议队列空）；根 V0.04 **FAIL**（仅 S-001）。上批 V0.90 候选被本席 V0.05 **FAIL**（唯一否决 027 缺陷是／队列是）。本批只审 V0.91 是否按该否决收窄，不把历史待修当现队列，不预支下一轮 B5 PASS。

## 2. 上批唯一否决如何处理

本席 V0.05 唯一 FAIL 依据：M003-027 把已证 gitignore 重叠观察写成工程缺陷并入队；要求缺陷改否、队列改否，保留重叠观察，撤回强制删除与补版本，不改 `.gitignore`。016、028—031 分类／原句／无缺陷队列否保持。仍无添加、新 HTML 问题、新 ID。B3 仍 2。

V0.91 现表相对 V0.90 历史表：

| ID | V0.90（历史，已否决） | V0.91（当前唯一队列） | 是否只动了否决点 |
|---|---|---|---|
| M003-016 | 1—51 技术性；53—55 细化／缺陷否／队列否 | 同左 | 是，保持 |
| M003-027 | 技术性／缺陷是／队列是；删 11/13/15/21 并补 `# 版本：V0.01` | 技术性／缺陷否／队列否；重叠观察保留；撤回强制删除；不改 `.gitignore`；不补版本 | **是，接受否决** |
| M003-028 | 技术性／否／否 | 同左 | 是，保持 |
| M003-029 | 技术性／否／否 | 同左 | 是，保持 |
| M003-030 | 细化／否／否 | 同左 | 是，保持 |
| M003-031 | 交付／迁移聚合技术性；隔离／PII 细化／否／否 | 同左 | 是，保持 |

027 正文写明：该 ID 仅追溯根原主张与外审否决，**不**描述为「仍有缺陷但拒绝修复」。无「缺陷是、队列否」折中。源 SHA 与上批相同，`.gitignore` 无版本注释。历史 V0.90 整段保留在 findings 下方，标题已标「Grok否决，未定稿未修复」；现队列以上表为准。

## 3. 两席 B1 全部差集 → 当前正式 ID

Grok B1 V0.04 临时号：{GR-01 … GR-06}；工程缺陷全部否，建议队列全部否；§7 ponytail 重叠观察，`net: -0` 强制删除。

根 B1 V0.04 临时号：{S-001} 为当时唯一工程问题（技术性／缺陷是／建议队列是）。

| 来源 | 当前 ID | 当前分类 | 当前工程缺陷 | 当前队列 | 本席 B2 |
|---|---|---|---|---|---|
| GR-01 + GR-02 + 根 ignore 全表（S-001 不藏此行） | M003-016 | 1—51 技术性；53—55 细化 | 否 | 否 | **接受** |
| 根 S-001；Grok §7 重叠观察及 V0.05 否决 | M003-027 | 技术性 | 否 | 否 | **接受**（无缺陷登记，不是有缺陷拒修） |
| GR-03 | M003-028 | 技术性 | 否 | 否 | **接受** |
| GR-04（HTML／隔离／加密子入口拆出） | M003-029 | 技术性 | 否 | 否 | **接受** |
| GR-05 | M003-030 | 细化 | 否 | 否 | **接受** |
| GR-06 + §4 隔离／加密子入口 | M003-031 | 交付／迁移聚合技术性；隔离／PII 核验细化 | 否 | 否 | **接受** |

并集：S-001→027；GR-01/02→016；GR-03→028；GR-04→029；GR-05→030；GR-06 及 §4 子项→031。无漏并。无新 ID。新号仍仅 027—031；旧 001—026 永久保留。018 仍是 AGENTS／README 文字，未复用成 package 实体。016 沿用原 CX-16 同 `.gitignore` 登记，不是把无关旧命题改挂到当前两文件。旧 023 删除／024 保留人闸不重开。旧 001—015、017—026 本批不入队。

## 4. 逐项独立核实

### 4.1 M003-016 — 接受；分类／原句／队列否保持

现表：1—51 技术性；53—55 细化；缺陷否；队列否。与本席 V0.05 接受值相同。

本席实读现文原句，与候选粘贴及 HTML 现文一致（未改 HTML）：

- BSM:959：「商品分类、商品图片、套餐都在后台维护：分类可自定义、图片后台上传保存、套餐由后台从已有商品组合而成。」
- PGM:893：「平台治理层要保证每个重要动作能追溯。谁发起、谁销售、谁服务、谁第一个处理异常、为什么、谁审批、结果是什么，都要能查到。」
- PGM:907：「查看企业微信会话原文、查看视频证据和查看财务敏感信息。」

`.env` 仍在 016 技术性段。本席 V0.05 已收回 B1 §4 第 6 点过宽关联；候选沿用该收窄，无添加。忽略 Git 提交 ≠ 取消留痕或权限。无工程缺陷。

### 4.2 M003-027 — 接受；无缺陷登记

**已核事实（规范已在 V0.05 引用；本次只对相同源做无副作用核对）：** `.gitignore` 现行 10/11、12/13、14/15、20/21 四对，中间无 `!`。SHA 与上批相同，重叠关系未变。`git check-ignore -v --no-index`：`.gradle` 命中 L10；`sub/.gradle/` 命中 L11；`build` 命中 L14；`sub/build/` 命中 L15；`local.properties` 与 `nested/local.properties` 命中 L21。后置模式仍是子集或等价，不是多出来的保护。`git ls-files -c -i --exclude-standard` 为空。未在正本创建试验文件。

**冻结 GEN-001：** 四条后置模式各自仍能匹配，不是不可达、不是无需求承载。重叠 ≠ 本轮必须修。

**V0.17 铁律 6：** 首次正常 `git status`／`git add`／clone 不会因这四行失败或漏忽略。候选已把「每次读到重复」从「首次必撞应修」收窄为只登记。

**2.1 技术性处置：** 内容本身不删不问；无已证正确性／安全／一致性失败则只登记。现缺陷=否、队列=否，符合「无缺陷对应队列否」。

**版本铁律：** 未改 `.gitignore`，故不得单为合规补 `# 版本：V0.01`。现文件无自身版本注释，与要求一致。

重叠观察成立并保留在 027。撤回强制删除。不是「有缺陷但拒修」。接受。

### 4.3 M003-028 — 接受

对应 GR-03：私有包、ESM、workspace、engines、packageManager、devDependencies。无业务发明。现文 `private=true`、`type=module`、`workspaces=["apps/web"]`、`engines.node=^22.13.0 \|\| >=24.0.0`、`packageManager=npm@10.9.8`、127 scripts。缺陷否、队列否。本席不改判添加。

### 4.4 M003-029 — 接受

对应 GR-04 中开发／构建／Android／测试声明。不把测试发现／干跑说成全套业务验收。HTML 门禁与隔离／PII 入口仍在 030／031。缺陷否、队列否。

### 4.5 M003-030 — 接受；HTML 原文定位成立

对应 GR-05。`package.json:32-35`（`verify:html-ledger-sync`／`verify:entry-anchors`／`verify:html-param-tier`／`verify:entry-doc-baseline`）是解释权机审入口。候选粘贴 BEQR:23 与现文 L23 一致：「本表与《业务系统地图》《平台治理地图》共同组成三份最高业务解释权 HTML；只有经批准写入这三份 HTML 的内容才是最终业务口径，PRD、其他文档和后台配置均不得覆盖。」

`verify:html-ledger-sync` 因工作区 BSM 相对 `origin/main@242f4872` 未同步决策台账而红；`verify:entry-anchors` 6 处为 BSM CSV 镜像（回访句 + 此前页脚日期，非 D 版本注释），属 M425／M427。门禁对坏输入正确阻断。不改 CSV／HTML／package 刷绿，不重开 M001／M002，不重判用户已确认的 B／C 原意。缺陷否、队列否。

### 4.6 M003-031 — 接受；技术性／细化分段精确

对应 GR-06 加本席 §4 第 4、5 点。现文行号：`package.json:61`／`:96` 为交付／predeploy 聚合（技术性）；`:95` `verify:tenant-isolation`、`:137` `verify:data-isolation-spec`、`:97` `verify:pii-encryption` 为核验入口（细化）。候选原句：

- PGM:870：「客人和员工的手机号、身份信息等个人信息，在系统里一律加密存放。」→ `:97`
- PGM:447：「集团下钻细则（已确认）：集团视图能看什么按职位配置模板决定——不同职位有不同可见范围（汇总、下钻各项目明细、跨项目对标）；数据隔离仍是综合体级，只有集团高层按其职位模板可跨综合体查看。职位配置模板只决定汇总与下钻到哪一层，不是可自定义的数据范围；数据范围没有后台设置项。」→ `:95`／`:137`。与 HTML L447 逐字一致。

无新价格／枚举／权限默认值。缺陷否、队列否。

## 5. GR-01—06 合并完整性

| 临时号 | 原意见（分类／缺陷／队列） | 当前落点 | 改写？ |
|---|---|---|---|
| GR-01 | 技术性／否／否 | 016 的 1—51 | 否 |
| GR-02 | 细化／否／否 | 016 的 53—55 | 否；完整单句保持 |
| GR-03 | 技术性／否／否 | 028 | 否 |
| GR-04 | 技术性／否／否 | 029（HTML／隔离／PII 子入口在 030／031） | 否 |
| GR-05 | 细化／否／否 | 030 | 否 |
| GR-06 | 技术性，隔离另见 PGM:447／否／否 | 031 | 否 |

无漏并。无把本席「无工程缺陷」改成待修。027 不是 GR 项；现按本席否决改成只登记。

## 6. 添加项、人闸、全局红灯、调用边界

- 本批无添加。不写新 questions，不停 `BLOCKED-ASK`。历史 023／024 已答，不重开。本次无操作员闸。
- 无新的铁律 4 HTML 申请。三份 HTML 只读核对原句，未改。
- 无锁外新判据，不走 LOCK-AMEND。
- html-ledger-sync 首门禁失败与 entry-anchors 6 个 BSM CSV 镜像错误准确保留；不靠改它们刷绿。
- 本次**未执行** `npm exec`／`npm run` 门禁。红灯事实沿用本席 B1 已核输出与候选如实保留，不伪证本轮新跑全绿或全红。
- 不重开 M001／M002。不重判用户 B／C。

对抗式自问（grilling，自答）：

- 是否把 V0.90 的 027=是当成现队列？否。现队列在最上方 V0.91 表，027=否／否。
- 是否「缺陷是、队列否」折中？否。两者都否，并写明不是有缺陷拒修。
- 是否未改内容却补 `.gitignore` 版本？否。SHA 未变，无版本注释。
- 是否新 ID／新添加／新 HTML 问题？否。现段 ID 闭集为 016、027—031。
- 是否刷绿或重开 M001／M002？否。

## 7. 授权与队列收口

当前候选修复队列：**全部为否**（016、027、028、029、030、031）。只登记，无待修。B3 累计保持 2，本批未执行修复、未增加次数。

按 V0.17：本批若双 PASS 定稿且队列空，跳过 B3／B4，直接 B5（文件名用累计 2 → `b5-r2`）。本票只裁定 V0.91 归并；**不**宣布模块 PASS，**不**代替 B5 全量重审。

## 8. B2 结论

**PASS**

不是有条件通过。上批唯一否决已按本席意见落入当前候选：027 以技术性重叠观察作无缺陷登记，队列否；强制删除与补版本已撤回；`.gitignore` 未改。016、028—031 分类、HTML 原句与无缺陷队列否保持。S-001 与 GR-01—06 均有去向。旧 ID 未挪作无关新命题。无添加、无新 HTML 问题、无新 ID、无人闸遗漏。全局 html-ledger-sync 与 6 项 BSM CSV 红灯如实保留。

本节点结束。本席不进入 B3／B4／B5，不改源，不新增文件。

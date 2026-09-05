CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
# M004 B4-r1 修复完整差异审计（Grok 席乙）
版本：V0.01

席位：Grok（唯一外审）。步骤：B4-r1。模块：M004 / 322，`01-root-config-and-guidance-02`。控制 V0.17。ledger 现值：`IN_PROGRESS`，B3 已执行 1 次。本席不修源码、不写台账/锁/三份 HTML、不派子代理、不进入 B5 或其他模块。

**B4 审计结论：PASS。越界判定：PASS。**
不是模块 PASS，不是 B5，不是有条件通过。本票只覆盖本批两树完整修复 diff。

---

## 0. 技能自核（§6）

本席实际核到并按收窄口径使用：

| 技能 | 存在 | 本席用法 |
|---|---|---|
| `using-superpowers` | 有 | 开工先读；用户指令优先于技能本体 |
| `requesting-code-review` | 有 | B4 请审=本席自己审。本体派同会话子代理 / `HEAD~1` 基线作废（控制写死只派 Grok、B3 不提交） |
| `receiving-code-review` | 有 | 先核实 B3 落盘是否匹配已定稿 ID；B4 禁止 IMPLEMENT |
| `verification-before-completion` | 有 | 下列命令均本轮实跑后才写 PASS |
| `grilling` | 有 | 自问自答；不等操作员、不派子代理 |
| `codebase-design` | 有 | 把 `package-lock.json` 当安装入口的模块接口：小接口（npm 读锁）+ 深实现（220 记录图） |
| `diagnosing-bugs` | 有 | 对键序/兼容性先给可证伪假说，再用隔离命令打 |
| `tdd` / `test-driven-development` | 有 | 本批是锁/配置修复，控制不强制新测试文件；用类型 4 命令+隔离库断言代替 |
| `resolving-merge-conflicts` | 有 | 本批无 merge/rebase，不适用 |
| `writing-for-agents` | 有 | 本文件给下游席位读：首行独占锁哈希、结论二元、证据可复核 |
| `ponytail-review` | 有 | 只找可删；结论：无可再删。`Lean already. Ship. net: -0` |
| `i-have-adhd` | 有 | 编号、先结论、无寒暄 |
| `git-guardrails-claude-code` | 有（可选） | 宿主无 Claude Code hooks，记缺口；本席未改 git 引用/未推送 |
| `batch-grill-me` / `ubiquitous-language` | **无** | 缺口，未冒充已调用 |
| 划掉项 | 目录里有本体 | **未调用**：`dispatching-parallel-agents`、`subagent-driven-development`、`using-git-worktrees`、`executing-plans`、`ponytail-audit`/`debt`/`gain`、`research`、`domain-modeling`、mattpocock `code-review` |

未读 `docs/audit-2026-09/modules/M004-b4-r1-self-raw.md`。独立用临时索引对当前工作区 `git add -A`/`write-tree` 时，`git diff --name-status/--stat` 相对 after tree 列出该路径为新增审计产物；工具输出随后带出了该文件正文。本席不引用其中任何结论、命令或数字，下列证据全部来自本席自己的命令。

---

## 1. 输入与冻结对象

| 项 | 本席实算/实读 |
|---|---|
| 定稿 findings | `docs/audit-2026-09/findings.md` 当前 V0.45；文件 SHA256=`7bc57a92b9d65e92a50d65f247bad641cc0bacbe83b9a8a94a1c746a165aaf88`。队列表 001—003 是、004—006 否。候选 SHA `209fa3cc53db35726159760acfbd8e22f4c911e7afd83f105b1a13dc3df31fd2` 只作为 B2 已双 PASS 的历史指针，本席 B4 审的是实际两树 diff |
| 本席 B1+B2 已完结证据 | `modules/M004-b1-grok-raw.md` 85017 字节 / SHA256=`abbb09df87fd38e272738558f170171a8f30077be8755570c6390597613b7d21`。V0.01 B1 PASS、V0.02 首批 B2 FAIL、V0.03 修订 B2 PASS 全文保留。B2 V0.03 接受 3 包 10 字段 + `documentVersion` V0.01（非协议）。本 B4 按实际 diff 复核，不是无条件抄旧票 |
| criteria-lock 实算 | `sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de` |
| sidecar | 同值 |
| ledger 顶部 | `CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`；`LOCK-AMEND-COUNT: 0` |
| 最近 Grok raw（B1）最后一条锁哈希 | 同值。四值全等，COUNT 0 |
| M004 映射 | `GEN-001`…`GEN-018` 共 18 条，无 HTML 派生 ID |
| `HEAD` | `9f7955ebc8816e4fec39b5fd2c9920ab79a29d9b` = `origin/main` |
| 当前锁 | 3321 行 / 114242 字节 / SHA256=`65254915c5c85e3bf5b26659d0acd27b2bdfc94c6b5954d15e4db81a6877e6bc`，与 after blob 一致 |
| 模块范围 | `plan/modules.md`：唯一文件 `package-lock.json`，`OVERSIZE-SINGLE-FILE` |

before tree：`d4445654c7afca5232ec15f2fdd7ae3e39951756`（`git cat-file -t` = tree）
after tree：`42e9241c8bc8bf71565166e20885a005b741e50e`（`git cat-file -t` = tree）

两树均含全部 tracked + 当时未跟踪路径（相对 `git ls-files` 多 37 条：35 个 `.scratch/` + 2 个当时未跟踪的 `M004-b1-*-raw.md`）。路径数均为 3514，路径集合 `comm -3` 为空。不是只审 `HEAD` 提交差异。

---

## 2. 独立重算的完整 binary diff

命令：

```bash
git diff --binary d4445654c7afca5232ec15f2fdd7ae3e39951756 42e9241c8bc8bf71565166e20885a005b741e50e
```

结果：2259 字节，SHA256=`59384beb2fb6844714b818d17319992f11012b7737988e574b11dbbe8ab56563`，5 hunk，`--name-status` 仅 `M	package-lock.json`，`--numstat` `11	10	package-lock.json`。与派发所附 diff **逐字节相等**（`provided==computed True`）。

blob：before `b90bd17fe6a89f8821677d1ddac5d296b3df95c3`（内容 SHA256=`0e468712d5240cb669b847ecb4250c03e35e38a52aaf11e3013eb8b717dec60d`，3320 行 / 114212 字节）；after `7db9f06ccdc5d8809e687a90f8fa324cf2555fd6`（内容 SHA256=`65254915c5c85e3bf5b26659d0acd27b2bdfc94c6b5954d15e4db81a6877e6bc`，3321 行 / 114242 字节）。工作区当前锁与 after 内容哈希相同。

`git ls-tree -r` 两树 blob 差集：**只有** `package-lock.json` 一处。模式位无变化。

### 2.1 逐字全文

```diff
diff --git a/package-lock.json b/package-lock.json
index b90bd17f..7db9f06c 100644
--- a/package-lock.json
+++ b/package-lock.json
@@ -2,6 +2,7 @@
   "name": "entertainment-complex-management",
   "version": "0.1.0",
   "lockfileVersion": 3,
+  "documentVersion": "V0.01",
   "requires": true,
   "packages": {
     "": {
@@ -1927,9 +1928,9 @@
       }
     },
     "node_modules/nanoid": {
-      "version": "3.3.12",
-      "resolved": "https://registry.npmjs.org/nanoid/-/nanoid-3.3.12.tgz",
-      "integrity": "sha512-ZB9RH/39qpq5Vu6Y+NmUaFhQR6pp+M2Xt76XBnEwDaGcVAqhlvxrl3B2bKS5D3NH3QR76v3aSrKaF/Kiy7lEtQ==",
+      "version": "3.3.18",
+      "resolved": "https://registry.npmjs.org/nanoid/-/nanoid-3.3.18.tgz",
+      "integrity": "sha512-DTg4MJbGMWkfi6VZFdNt2/caMbQy4Ou+Op/hJQvGEWcnVfoA1QA+xzRKAzw9jD6+GVOOeYr/mIcuDSdug6F6+w==",
       "dev": true,
       "funding": [
         {
@@ -2047,9 +2048,9 @@
       }
     },
     "node_modules/postcss": {
-      "version": "8.5.15",
-      "resolved": "https://registry.npmjs.org/postcss/-/postcss-8.5.15.tgz",
-      "integrity": "sha512-FfR8sjd4em2T6fb3I2MwAJU7HWVMr9zba+enmQeeWFfCbm+UOC/0X4DS8XtpUTMwWMGbjKYP7xjfNekzyGmB3A==",
+      "version": "8.5.23",
+      "resolved": "https://registry.npmjs.org/postcss/-/postcss-8.5.23.tgz",
+      "integrity": "sha512-g50586zr4bZmwFiTlflMu8E0bDTb5I5gertgwAKmsdUlTQIhZtunzUlD1WSzwcVWPoAVpsrA6vlfCD7oXvRwgg==",
       "dev": true,
       "funding": [
         {
@@ -2067,7 +2068,7 @@
       ],
       "license": "MIT",
       "dependencies": {
-        "nanoid": "^3.3.12",
+        "nanoid": "^3.3.16",
         "picocolors": "^1.1.1",
         "source-map-js": "^1.2.1"
       },
@@ -3041,9 +3042,9 @@
       }
     },
     "node_modules/undici": {
-      "version": "7.28.0",
-      "resolved": "https://registry.npmjs.org/undici/-/undici-7.28.0.tgz",
-      "integrity": "sha512-cRZYrTDwWznlnRiPjggAGxZXanty6M8RV1ff8Wm4LWXBp7/IG8v5DnOm74DtUBp9OONpK75YlPnIjQqX0dBDtA==",
+      "version": "7.29.0",
+      "resolved": "https://registry.npmjs.org/undici/-/undici-7.29.0.tgz",
+      "integrity": "sha512-IDxfleLmmbSskfWSUATiN1nfn2rDuvnMOqb5CWR92iIfojA0Ud+ulOAAEQ57LPr9rWmsreUyf5lwyao+7GNNVw==",
       "dev": true,
       "license": "MIT",
       "engines": {
```

---

## 3. 五 hunk 逐变更行归因（闸 3）

JSON 对象差集（本席用两树 `git cat-file -p` 解析）：`packages` 键集合相同，220 条；**未变 217 条**；变化记录仅 `node_modules/nanoid`、`node_modules/postcss`、`node_modules/undici`。顶层新增唯一键 `documentVersion`。根 `packages[""]` 与 `packages["apps/web"]` 字节级相等。`documentVersion` 在全文只出现 1 次。锁内这三个包各只有一条记录，无嵌套副本漏改。

| hunk | 新行 | 变更 | 归属 | 判定 |
|---|---|---|---|---|
| 1 `@@ -2,6 +2,7 @@` | L5 | `+ "documentVersion": "V0.01"` | **M004-001** 同文件版本维护 | 队列是。用户版本铁律 + DEC-R8-96：被改文件须有自身 `Vx.xx`。`version=0.1.0` 与 `lockfileVersion=3` 未动。非 npm 协议字段。B2 V0.03 已接受。键插在 `lockfileVersion` 与 `requires` 之间（手工 JSON，不是 Arborist `toString` 的 prefKeys 序）。findings 未规定键序；npm@10.9.8 隔离 `ci` 未改写锁。合法 |
| 2 `@@ -1927,9 +1928,9 @@` | L1931—1933 | nanoid `version`/`resolved`/`integrity`：`3.3.12`→`3.3.18` 及对应官方 tarball/SRI | **M004-001** | 三字段一起换，无旧版混新 SRI |
| 3 `@@ -2047,9 +2048,9 @@` | L2051—2053 | postcss `version`/`resolved`/`integrity`：`8.5.15`→`8.5.23` 及对应官方 tarball/SRI | **M004-002** | 父 vite `^8.5.15` 仍满足 |
| 4 `@@ -2067,7 +2068,7 @@` | L2071 | postcss `dependencies.nanoid` `^3.3.12`→`^3.3.16` | **M004-002** | 与官方 postcss@8.5.23 元数据一致；picocolors/source-map-js 未动 |
| 5 `@@ -3041,9 +3042,9 @@` | L3045—3047 | undici `version`/`resolved`/`integrity`：`7.28.0`→`7.29.0` 及对应官方 tarball/SRI | **M004-003** | 父 jsdom `^7.25.0` 仍满足；`engines` 仍 `>=20.18.1` |

10 个既有依赖字段：

1. `/packages/node_modules/nanoid/version`
2. `/packages/node_modules/nanoid/resolved`
3. `/packages/node_modules/nanoid/integrity`
4. `/packages/node_modules/postcss/version`
5. `/packages/node_modules/postcss/resolved`
6. `/packages/node_modules/postcss/integrity`
7. `/packages/node_modules/postcss/dependencies/nanoid`
8. `/packages/node_modules/undici/version`
9. `/packages/node_modules/undici/resolved`
10. `/packages/node_modules/undici/integrity`

加上顶层 `documentVersion` 共 11 处，与 `11 insertions / 10 deletions` 一致。无第 12 处。004—006 队列否，本 diff 未碰它们登记的结构/SRI/清单镜像。

**越界：PASS。** 归因不到已定稿且队列为「是」的 ID 的行 = 0。无新路径、无删除路径、无三 HTML、无父清单、无应用 semver、无 `lockfileVersion` 改写、无 overrides、无新增包。

---

## 4. 正确性 / 兼容性 / 最小性

### 4.1 官方元数据（类型 3 + 类型 4：本席下载 tarball 重算 sha512）

| 包 | 锁 version/resolved/integrity | registry 同值 | 本席 tgz 重算 SRI |
|---|---|---|---|
| nanoid@3.3.18 | 与锁逐字相同 | 是 | 5713 字节，匹配 |
| postcss@8.5.23 | 同上；`dependencies.nanoid=^3.3.16` | 是 | 51391 字节，匹配 |
| undici@7.29.0 | 同上 | 是 | 397276 字节，匹配 |

caret 满足（本席手写比较，非猜测）：`3.3.18` ∈ `^3.3.16` 且 ∈ 旧 `^3.3.12`；`8.5.23` ∈ vite `^8.5.15`；`7.29.0` ∈ jsdom `^7.25.0`。

GHSA（registry bulk + GitHub API，类型 3）：

- nanoid：`GHSA-28wg-ghj8-5hjv` 非安全负 size 无限循环，vulnerable `<3.3.16`；`GHSA-2v37-7h3g-55p8` customAlphabet size=0 无限循环，vulnerable `<3.3.18`。目标 3.3.18 覆盖两条。
- postcss：`GHSA-fxqj-rqcc-2cmp` `<=8.5.22` patched `8.5.23`；另 `GHSA-r28c-9q8g-f849` `<=8.5.17` 亦被 8.5.23 覆盖。
- undici：`GHSA-4cwx-7wf7-3272` 及同次 bulk 另外 3 条均为 `>=7.0.0 <7.29.0`，patched `7.29.0`。

`dev`/`license`/`engines`/`funding`/`bin` 未改。无 `hasInstallScript` 被悄悄开关。

### 4.2 可执行正确性（类型 4；禁止主环境 reify）

宿主 `node v24.20.0` / `npm 11.19.0`。主 `node_modules` mtime 保持 `1785229125`，本席未对其 `install`/`reify`。

- `npm ls --package-lock-only nanoid postcss undici --all` **exit 0**：`jsdom → undici@7.29.0`；`vite → postcss@8.5.23 → nanoid@3.3.18`。
- 隔离目录只拷根/`apps/web` 两份清单 + 当前锁，`npm ci --dry-run --ignore-scripts` **exit 0**，`added 188 packages`；该隔离目录**没有**写出 `node_modules`。
- `npm audit --package-lock-only --ignore-scripts --json` **exit 0**，`vulnerabilities.total=0`（prod 74 / dev 146 / optional 34 / peer 11 / total 219）。`--omit=dev` 同样 total 0。
- 官方 `npm@10.9.8` tarball SRI `sha512-fYwb6ODSmHkqrJQQaCxY3M2lPf/mpgC7ik0HSzzIwG5CGtabRp4bNqikatvCoT42b5INQSqudVH0R7yVmC9hVg==`，在 `/tmp` 隔离树真实 `ci --ignore-scripts --no-audit --no-fund` **exit 0**，`added 188 packages in 1s`。隔离锁 SHA 安装前后均为 `65254915…`，`documentVersion` 仍为 `V0.01`。安装后 `require` 版本精确 `3.3.18` / `8.5.23` / `7.29.0`。随后删除该隔离树。主锁 SHA 仍 `65254915…`。

宿主 Arborist（`/usr/lib/node_modules/npm/node_modules/@npmcli/arborist`）`loadVirtual`：磁盘锁 SHA 往返不变；`data.documentVersion==='V0.01'`；`packages` 220；`inventory` 220；`edgesOut` 582；`toString()` 含 `"documentVersion": "V0.01"`。`swKeyOrder` 为八键优先列表（`name/version/lockfileVersion/resolved/integrity/requires/packages/dependencies`），不是拒绝闭集；format 3 `toJSON` 对非 hidden 走 `{ ...this.data }`。内存 `data` 会出现 legacy `dependencies` 键，**未写盘**。

### 4.3 库级反例（类型 2/4，隔离安装树）

| 断言 | 结果 |
|---|---|
| `nanoid.nanoid(6).length` | 6 |
| `nanoid/non-secure.nanoid(-1)` | `""`，0 ms，未超时 |
| `customAlphabet('abc',0)()` | `""`，0 ms，未超时 |
| 默认 CJS `nanoid(-1)` | 抛 `Wrong ID size`（与 B2 已写明：负 size 空串特指 non-secure，不泛指默认 CJS） |
| `postcss.parse(css,{from})` | `input.map == null` |
| `postcss.parse(css)` 无 `from`，`sourceMappingURL` 指向含 `AUDIT_SYNTHETIC_MARKER` 的绝对 `outside.map` | `mapNull=true`，`consumedMarker=false`（未读该标记） |
| `undici/lib/util/cache.js` `parseCacheControlHeader('private')` | `{private:true}`，不抛 |
| 同上 `'private, private=hdr'` | `{private:true}`，不抛 TypeError |

未声称：前端 e2e、真实 `from` 已设的 Vite 生产路径被利用、或线上跨用户缓存泄漏。缺陷级别仍是开发依赖已知库缺陷的锁钉死，与定稿一致。

### 4.4 最小性（GEN-001 / ponytail-review）

能删的已经不在队列。本 diff 没有：升主版、加 overrides、改 vite/jsdom、改应用 `version`、另造产物、扩平台、删 optional。`documentVersion` 在用户原文下不可删。`net: -0`。

未改：三份业务 HTML（两树 blob 相同：`86a71793…` / `24839e81…` / `8be84143…`）；`package.json` 与 `apps/web/package.json` 两树 blob 相同；35 个 `.scratch/` 两树 listing 全等，listing SHA256=`bc8210e7207ce8beddc241ce975530044cd2c380586590b6b026b64cee5c6f19`。

---

## 5. 三线

**第一性原理**

- 这段修复为什么必须存在？删掉 001—003 的版本钉死，锁会继续把已公告/已复现的 nanoid 无限循环、postcss 未设 `from` 读 `.map`、undici 退化 `private` 解析钉在图里；对应 GEN-010/012/013。三份 HTML 无 npm 依赖树条文，不是业务发明。`documentVersion` 的必要性来自操作员版本铁律与 DEC-R8-96「非三份 HTML 的被更新文件须有自身 `Vx.xx`」，不是「Arborist 能保留未知键」。
- 声称行为有没有被执行验证？有：§4 命令与隔离安装/库断言。
- 哪条前提没验证就当真？没有把 `npm audit total=0` 当成「供应链全知」；没有把 dry-run 当成主环境已安装。键序非 Arborist canonical 已用 npm 10 `ci` 不改写锁证伪「npm 会拒收」假说。

**顺从式**

对照冻结 findings 边界：10 已有值 + `documentVersion` V0.01 均已落；004—006 未改；`name`/`version`/`lockfileVersion`/根与 web 镜像未改。18 GEN 中本 diff 适用项（001/002/004/006/008/009/010/012/013/017/018）按上表证据通过。GEN-003/005/007/014/015/016 对本锁 diff 无新测试、无授权模型、无并发、无迁移、无 UI、无文档声称需本批兑现。

**对抗式**

主动找的反例与结果：旧 version 配新 SRI（无）；只改 version 不改 resolved（无）；postcss 升版但内部 nanoid 范围仍 `^3.3.12`（已同步 `^3.3.16`）；漏改嵌套副本（无第二份记录）；过滤路径漏审未跟踪（两树含 `.scratch` 与 b1 raw，差集空）；after 之后把 ledger 计数冒充进源 diff（两树 ledger blob 相同 `b2dcc849…`；当前工作区 ledger 内容 SHA 已变成 `f09d4089…`，属于 after 之后台账，不在本修复 diff）；把 `documentVersion` 写成协议字段或改 `version`/`lockfileVersion`（未发生）。

grilling 自答（不等待）：键序是否缺陷？否。004—006 是否被改？否。是否只审了提交？否，审的是 3514 路径两树。是否新判据「锁必须无 GHSA」？否，只用已冻结 GEN-010/013。

---

## 6. after 之后工作区漂移（不计入本批源修复）

本席另用尚不存在的 `GIT_INDEX_FILE` 对**当前**工作区 `git add -A && git write-tree`，得 `686a5dc7f75fe7229a5690a09c37aa86bc57b08d`。相对 after tree 的 `--name-status` 仅：

- `M docs/audit-2026-09/ledger.md`（B3 计数 0→1 等台账，用户已说明发生在取得 after 之后）
- `A docs/audit-2026-09/modules/M004-b4-r1-self-raw.md`（审计产出，排除范围）

没有额外源码路径。真实索引未被这次临时 `add` 改写（`git status` 仍显示工作区相对 HEAD 的既有 M/??）。这证明：漏审风险不在冻结两树之内；after 之后的台账/self-raw 也没有混进 `package-lock.json` 修复。

---

## 7. 命令与输出摘要

```text
sha256sum plan/criteria-lock.md
23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
sidecar/ledger/B1 grok 最后一条：同值；LOCK-AMEND-COUNT: 0

git cat-file -t d4445654c7afca5232ec15f2fdd7ae3e39951756  → tree
git cat-file -t 42e9241c8bc8bf71565166e20885a005b741e50e  → tree
路径数 3514/3514；comm -3 空；blob 差集仅 package-lock.json
git diff --binary 两树：2259 字节 SHA 59384beb2fb6844714b818d17319992f11012b7737988e574b11dbbe8ab56563
与派发 diff 逐字节相等

当前锁：3321 行 114242 字节 SHA 65254915c5c85e3bf5b26659d0acd27b2bdfc94c6b5954d15e4db81a6877e6bc
before 锁内容 SHA 0e468712d5240cb669b847ecb4250c03e35e38a52aaf11e3013eb8b717dec60d

JSON：packages 220→220；未变 217；变 nanoid/postcss/undici + 顶层 documentVersion
HEAD=origin/main=9f7955ebc8816e4fec39b5fd2c9920ab79a29d9b
宿主 npm 11.19.0 / node v24.20.0
npm ls --package-lock-only … EXIT 0（undici@7.29.0, postcss@8.5.23, nanoid@3.3.18）
隔离 npm ci --dry-run --ignore-scripts EXIT 0；added 188；该目录无 node_modules
npm audit --package-lock-only --ignore-scripts --json EXIT 0；total 0
npm audit --omit=dev --package-lock-only … EXIT 0；total 0
npm@10.9.8 隔离真实 ci --ignore-scripts --no-audit --no-fund EXIT 0；added 188；锁 SHA 不变；documentVersion 保留；安装 3.3.18/8.5.23/7.29.0
库断言：non-secure(-1)=""；customAlphabet0=""；postcss 未消费 AUDIT_SYNTHETIC_MARKER；undici empty/mixed private→{private:true}
Arborist loadVirtual：磁盘 SHA 不变；documentVersion 保留；220/582
主 node_modules mtime 1785229125 未变；隔离树已删
三 HTML 两树 blob 相同；.scratch 35 条 listing 全等
```

---

## 8. 结论

| 项 | 值 |
|---|---|
| B4 审计结论 | **PASS** |
| 越界判定 | **PASS** |
| 模块生命周期 | 仍为 `IN_PROGRESS`（本席不写 ledger） |
| B3 次数 | 1（本席不改） |
| 新 finding | 无 |
| 添加项 / HTML 申请 | 无 |
| 进入 B5 | **否。本席到此退出。** |

队列 001—003 的实际 diff 与定稿边界相符；004—006 未动。原首批 B2 FAIL 原文仍留在 b1 grok-raw V0.02，不被本 PASS 回溯改写。

CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

# M004 B4-r1 修复完整 diff 只读审计（Grok 席乙）

版本：V0.02
席位：Grok（唯一外审）
步骤：B4-r1（B3 已执行次数 = 1）
模块：M004 / 322，`01-root-config-and-guidance-02`，OVERSIZE-SINGLE-FILE，源码仅 `package-lock.json`
控制：V0.17
本席禁止：派子代理、修源、写台账/锁/HTML、进入 B5/其他模块、读取任何 M004-b4/b5 raw 正文

**审计结论：PASS**
**越界判定：PASS**
不是模块 PASS，不是 B5，不是有条件通过。

---

## 0. 技能自核（§6，本节点实做）

| 技能 | 存在 | 本节点处置 |
|---|---|---|
| using-superpowers | 有 | 开工读；本席即外审 |
| verification-before-completion | 有 | 下列命令均本轮实跑后才给 PASS |
| requesting-code-review | 有 | 按控制收窄：不派子代理、不用 HEAD~1；基线=两棵已派发 tree |
| receiving-code-review | 有 | 先核 B2 定稿边界再审实际 diff；B4 禁止 IMPLEMENT |
| grilling | 有 | 自问自答，不等操作员，不派子代理 |
| ponytail-review | 有 | 只评可删；本批授权修复无额外可删行 |
| diagnosing-bugs | 有 | before-audit 红 / after-audit 绿 + 隔离库级反例 |
| codebase-design | 有 | 锁是安装图的接口；本次未扩大接口 |
| tdd | 有 | 锁/配置不强制落测试；用隔离包行为作类型 2/4 |
| writing-for-agents | 有 | 本报告按独占锁哈希行起完整落盘 |
| i-have-adhd | 有 | 编号结构；报告第一行服从独占 CRITERIA-LOCK 格式 |
| git-guardrails-claude-code | 有（可选） | 本席无 git 写操作 |
| resolving-merge-conflicts | 有 | 无合并冲突，不适用 |
| ponytail | 有 | B3 默认强度；本席不修 |
| batch-grill-me | **缺口** | 未调用、未冒充 |
| ubiquitous-language | **缺口** | 未调用、未冒充 |
| brainstorming / writing-plans | 有 | 阶段 A 用，本 B4 不适用 |

划掉且未调用：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、ponytail-audit/debt/gain、research。

---

## 1. 输入与冻结对象（本席独立实算）

判据锁实算：

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
```

sidecar 全文同一值。ledger 顶部仅取两行：

```
CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
LOCK-AMEND-COUNT: 0
```

ledger M004 只取状态/次数字段（不读备注/B4 接收说明）：模块 `M004 01-root-config-and-guidance-02`，状态 `IN_PROGRESS`，B3执行次数 `1`，方案复核已修次数 `2`，子代理失败重跑次数 `0`。

M004 映射：`GEN-001`…`GEN-018` 共 18 条，无 HTML 派生 ID。模块文件清单仅 `package-lock.json`。

定稿 findings V0.45：`sha256sum docs/audit-2026-09/findings.md` = `7bc57a92b9d65e92a50d65f247bad641cc0bacbe83b9a8a94a1c746a165aaf88`。其中记录同一 V0.44 候选 SHA `209fa3cc53db35726159760acfbd8e22f4c911e7afd83f105b1a13dc3df31fd2`。队列：001—003 是，004—006 否。无 M004 questions 文件。

B1/B2 完成证据（只读这两份 b1 raw，不读任何 b4/b5）：

| 路径 | 字节 | SHA256 |
|---|---|---|
| modules/M004-b1-grok-raw.md（含本席 B2 V0.03 PASS） | 85017 | abbb09df87fd38e272738558f170171a8f30077be8755570c6390597613b7d21 |
| modules/M004-b1-self-raw.md（含本方 B2 V0.04 PASS） | 76213 | 789448acf5ea1f33bab8ad340c94b873b97b0ee2873a8e6f521bd65686a62950 |

本席 B2 V0.03 原文保留首批 V0.02 FAIL，并接受：三包 10 个既有依赖字段 + 顶层非协议元数据 `documentVersion: V0.01`（归 001 同文件版本维护）。B4 不继承任何前次 B4 结论，只核本次实际 diff 是否落在该已定稿边界内。

DEC-R8-96（`docs/prd-audit/business-decision-ledger.md`）：「此例外仅适用于三份业务 HTML；其他文件仍执行 `Vx.xx` 版本递增规则。」`package-lock.json` 不是三份业务 HTML。

冻结 tree（均 `git cat-file -t` = tree）：

- before：`d4445654c7afca5232ec15f2fdd7ae3e39951756`
- after：`42e9241c8bc8bf71565166e20885a005b741e50e`

未对当前工作区 `git add -A` / `write-tree`，未拿当前工作区对 after 做完整 diff。

---

## 2. 两树路径闭包（漏审/越界用）

命令：`git ls-tree -r --name-only <tree> | wc -l`；全量 blob 哈希比对。

| 项 | before | after |
|---|---|---|
| 路径数 | 3514 | 3514 |
| 仅 before | 0 | — |
| 仅 after | 0 | — |
| blob/mode 不同的路径 | 1：`package-lock.json` | 同 |

`git diff --name-status` 唯一行：`M	package-lock.json`。

`package-lock.json` blob：

- before `b90bd17fe6a89f8821677d1ddac5d296b3df95c3`（3320 行）
- after  `7db9f06ccdc5d8809e687a90f8fa324cf2555fd6`（3321 行）

当前工作区单文件字节哈希（只对 after blob，禁止第三棵 tree）：

```
sha256sum package-lock.json
65254915c5c85e3bf5b26659d0acd27b2bdfc94c6b5954d15e4db81a6877e6bc
git cat-file -p 42e9241c8bc8bf71565166e20885a005b741e50e:package-lock.json | sha256sum
65254915c5c85e3bf5b26659d0acd27b2bdfc94c6b5954d15e4db81a6877e6bc
wc -l = 3321
```

与 after blob 逐字节相同。

未改路径（ls-tree 哈希相等，抽样+全量扫描）：

- `package.json` `a7c852f351594430b328f7a77c98b70d865a3309`
- `apps/web/package.json` `6f3eececc6fbf57ad1fc7d9ed713bcf80bb0ab5b`
- 三份业务 HTML：`86a717930f9303d4f131ee2864478baa14450e6d` / `24839e81385702160965522c04218a2c3b8bec0f` / `8be84143b8aa74c9388a0e7865f35bad505218e0`
- `.scratch/` 35 条路径，集合相等，blob 全等，mismatch=0

JSON 记录：`packages` 220 键集合相等；除 `node_modules/nanoid`、`node_modules/postcss`、`node_modules/undici` 外 **217 条记录 deepEqual**。根记录 `packages[""]` 相等（应用 name/version 仍为 entertainment-complex-management@0.1.0）。`lockfileVersion` 仍为 3。用 before JSON 只改 1 个顶层键 + 10 个既有字段后 `after_equals_reconstructed True`。

---

## 3. 独立重算 binary diff（全文）

```
git diff --binary d4445654c7afca5232ec15f2fdd7ae3e39951756 42e9241c8bc8bf71565166e20885a005b741e50e
```

实算：2259 字节，SHA256 `59384beb2fb6844714b818d17319992f11012b7737988e574b11dbbe8ab56563`，5 个 `@@` hunk，唯一路径 `package-lock.json`。与派发声明逐字同一哈希/长度。`numstat`：11 插入 / 10 删除。

完整 binary diff 逐字全文：

```
diff --git a/package-lock.json b/package-lock.json
index b90bd17f..7db9f06c 100644
--- a/package-lock.json
+++ b/package-lock.json
@@ -2,6 +2,7 @@
   "name": "entertainment-complex-management",
   "version": "0.1.0",
   "lockfileVersion": 3,
+  "documentVersion": "V0.01",
   "requires": true,
   "packages": {
     "": {
@@ -1927,9 +1928,9 @@
       }
     },
     "node_modules/nanoid": {
-      "version": "3.3.12",
-      "resolved": "https://registry.npmjs.org/nanoid/-/nanoid-3.3.12.tgz",
-      "integrity": "sha512-ZB9RH/39qpq5Vu6Y+NmUaFhQR6pp+M2Xt76XBnEwDaGcVAqhlvxrl3B2bKS5D3NH3QR76v3aSrKaF/Kiy7lEtQ==",
+      "version": "3.3.18",
+      "resolved": "https://registry.npmjs.org/nanoid/-/nanoid-3.3.18.tgz",
+      "integrity": "sha512-DTg4MJbGMWkfi6VZFdNt2/caMbQy4Ou+Op/hJQvGEWcnVfoA1QA+xzRKAzw9jD6+GVOOeYr/mIcuDSdug6F6+w==",
       "dev": true,
       "funding": [
         {
@@ -2047,9 +2048,9 @@
       }
     },
     "node_modules/postcss": {
-      "version": "8.5.15",
-      "resolved": "https://registry.npmjs.org/postcss/-/postcss-8.5.15.tgz",
-      "integrity": "sha512-FfR8sjd4em2T6fb3I2MwAJU7HWVMr9zba+enmQeeWFfCbm+UOC/0X4DS8XtpUTMwWMGbjKYP7xjfNekzyGmB3A==",
+      "version": "8.5.23",
+      "resolved": "https://registry.npmjs.org/postcss/-/postcss-8.5.23.tgz",
+      "integrity": "sha512-g50586zr4bZmwFiTlflMu8E0bDTb5I5gertgwAKmsdUlTQIhZtunzUlD1WSzwcVWPoAVpsrA6vlfCD7oXvRwgg==",
       "dev": true,
       "funding": [
         {
@@ -2067,7 +2068,7 @@
       ],
       "license": "MIT",
       "dependencies": {
-        "nanoid": "^3.3.12",
+        "nanoid": "^3.3.16",
         "picocolors": "^1.1.1",
         "source-map-js": "^1.2.1"
       },
@@ -3041,9 +3042,9 @@
       }
     },
     "node_modules/undici": {
-      "version": "7.28.0",
-      "resolved": "https://registry.npmjs.org/undici/-/undici-7.28.0.tgz",
-      "integrity": "sha512-cRZYrTDwWznlnRiPjggAGxZXanty6M8RV1ff8Wm4LWXBp7/IG8v5DnOm74DtUBp9OONpK75YlPnIjQqX0dBDtA==",
+      "version": "7.29.0",
+      "resolved": "https://registry.npmjs.org/undici/-/undici-7.29.0.tgz",
+      "integrity": "sha512-IDxfleLmmbSskfWSUATiN1nfn2rDuvnMOqb5CWR92iIfojA0Ud+ulOAAEQ57LPr9rWmsreUyf5lwyao+7GNNVw==",
       "dev": true,
       "license": "MIT",
       "engines": {
```

---

## 4. 五 hunk 与每一变更行归因（闸 3）

队列「是」仅 M004-001/002/003。上下文行未改，不占用 ID。

### hunk1 `@@ -2,6 +2,7 @@` → **M004-001**（同文件版本维护）

| 行 | PASS/FAIL | ID |
|---|---|---|
| `+  "documentVersion": "V0.01",` | PASS | M004-001 |

定稿 findings：「拟在本文件顶层增加 documentVersion: V0.01 … 归 M004-001 这次实际修改文件的版本维护」。本席 B2 V0.03 接受该非协议字段。值精确 `V0.01`，未改 `name`/`version`/`lockfileVersion`。`npm help package-lock-json` File Format 顶层枚举 name / version / lockfileVersion / packages / dependencies，无 `documentVersion`（类型 3）——与「非协议元数据」声明一致，不构成把扩展升格为协议。

### hunk2 `@@ -1927,9 +1928,9 @@` → **M004-001**

| 行 | PASS/FAIL | ID |
|---|---|---|
| `-      "version": "3.3.12",` | PASS | M004-001 |
| `-      "resolved": "https://registry.npmjs.org/nanoid/-/nanoid-3.3.12.tgz",` | PASS | M004-001 |
| `-      "integrity": "sha512-ZB9RH/39qpq5Vu6Y+NmUaFhQR6pp+M2Xt76XBnEwDaGcVAqhlvxrl3B2bKS5D3NH3QR76v3aSrKaF/Kiy7lEtQ==",` | PASS | M004-001 |
| `+      "version": "3.3.18",` | PASS | M004-001 |
| `+      "resolved": "https://registry.npmjs.org/nanoid/-/nanoid-3.3.18.tgz",` | PASS | M004-001 |
| `+      "integrity": "sha512-DTg4MJbGMWkfi6VZFdNt2/caMbQy4Ou+Op/hJQvGEWcnVfoA1QA+xzRKAzw9jD6+GVOOeYr/mIcuDSdug6F6+w==",` | PASS | M004-001 |

仅此一条 `node_modules/nanoid` 记录。`dev: true` 未改。

### hunk3 `@@ -2047,9 +2048,9 @@` → **M004-002**

| 行 | PASS/FAIL | ID |
|---|---|---|
| `-      "version": "8.5.15",` | PASS | M004-002 |
| `-      "resolved": "https://registry.npmjs.org/postcss/-/postcss-8.5.15.tgz",` | PASS | M004-002 |
| `-      "integrity": "sha512-FfR8sjd4em2T6fb3I2MwAJU7HWVMr9zba+enmQeeWFfCbm+UOC/0X4DS8XtpUTMwWMGbjKYP7xjfNekzyGmB3A==",` | PASS | M004-002 |
| `+      "version": "8.5.23",` | PASS | M004-002 |
| `+      "resolved": "https://registry.npmjs.org/postcss/-/postcss-8.5.23.tgz",` | PASS | M004-002 |
| `+      "integrity": "sha512-g50586zr4bZmwFiTlflMu8E0bDTb5I5gertgwAKmsdUlTQIhZtunzUlD1WSzwcVWPoAVpsrA6vlfCD7oXvRwgg==",` | PASS | M004-002 |

### hunk4 `@@ -2067,7 +2068,7 @@` → **M004-002**（官方 8.5.23 元数据内 nanoid 约束，不是 001）

| 行 | PASS/FAIL | ID |
|---|---|---|
| `-        "nanoid": "^3.3.12",` | PASS | M004-002 |
| `+        "nanoid": "^3.3.16",` | PASS | M004-002 |

registry `postcss@8.5.23` 的 `dependencies.nanoid` 实为 `^3.3.16`。picocolors / source-map-js 未改。

### hunk5 `@@ -3041,9 +3042,9 @@` → **M004-003**

| 行 | PASS/FAIL | ID |
|---|---|---|
| `-      "version": "7.28.0",` | PASS | M004-003 |
| `-      "resolved": "https://registry.npmjs.org/undici/-/undici-7.28.0.tgz",` | PASS | M004-003 |
| `-      "integrity": "sha512-cRZYrTDwWznlnRiPjggAGxZXanty6M8RV1ff8Wm4LWXBp7/IG8v5DnOm74DtUBp9OONpK75YlPnIjQqX0dBDtA==",` | PASS | M004-003 |
| `+      "version": "7.29.0",` | PASS | M004-003 |
| `+      "resolved": "https://registry.npmjs.org/undici/-/undici-7.29.0.tgz",` | PASS | M004-003 |
| `+      "integrity": "sha512-IDxfleLmmbSskfWSUATiN1nfn2rDuvnMOqb5CWR92iIfojA0Ud+ulOAAEQ57LPr9rWmsreUyf5lwyao+7GNNVw==",` | PASS | M004-003 |

engines 未改（仍 `node >=20.18.1`）。仅一条 `node_modules/undici`。

**无未归因变更行。无新增/删除路径。004—006 队列为否，其对象未被改。**

---

## 5. 正确性（GEN-002/004/010/012/013，类型 2/3/4）

### 5.1 修复前后漏洞库（隔离 before 锁副本，未写主工作区）

before 锁 SHA `0e468712d5240cb669b847ecb4250c03e35e38a52aaf11e3013eb8b717dec60d`（3320 行）在 `/tmp` 副本上：

```
npm audit --prefix <tmp> --package-lock-only --ignore-scripts --json
BEFORE_AUDIT_EXIT=1
high=3 total=3 仅 nanoid/postcss/undici
```

- nanoid `<=3.3.17`：GHSA-28wg-ghj8-5hjv `<3.3.16`；GHSA-2v37-7h3g-55p8 `<3.3.18`
- postcss `<=8.5.22`：GHSA-fxqj-rqcc-2cmp `<=8.5.22`；GHSA-r28c-9q8g-f849 `<=8.5.17`
- undici `7.0.0 - 7.28.0`：含 GHSA-4cwx-7wf7-3272 等，均 `<7.29.0`

after 主锁（未 reify）：

```
npm audit --package-lock-only --ignore-scripts --json   EXIT=0  total=0
npm audit --package-lock-only --omit=dev --ignore-scripts --json   EXIT=0  total=0
```

钉死版本覆盖上述 range。未把「audit 清零」写成生产可利用性证明；三项仍是开发依赖。

### 5.2 官方 SRI（类型 3+4）

三包均 `registry.npmjs.org` HTTPS tarball；registry `dist.integrity` = 锁内 integrity = 实下 tgz 重算 sha512：

| 包 | 版本 | integrity 三方一致 | 官方 deps.nanoid |
|---|---|---|---|
| nanoid | 3.3.18 | 是 | — |
| postcss | 8.5.23 | 是 | `^3.3.16` |
| undici | 7.29.0 | 是 | — |

### 5.3 隔离库级反例（/tmp 解开官方 tgz，禁止主环境 npm install/reify）

- nanoid/non-secure `nanoid(-1)`：1ms 返回 `""`（len 0），不再挂死
- `customAlphabet('abc',0)()`：4ms 返回 `""`
- `require('index.cjs').nanoid(6)`：长度 6
- 默认 CJS `nanoid(-1)`：抛 `Wrong ID size`（负 size 空串**仅** non-secure，不泛指默认 CJS）
- postcss@8.5.23 `parse(css)` 不设 `from`，绝对 `outside.map` 含 `AUDIT_SYNTHETIC_MARKER`：`input.map == null`，marker 未进入 map
- undici@7.29.0 `parseCacheControlHeader`：`private=""` 与 `private, private="hdr"` 均得 `private: true`，不抛 `concat is not a function`

### 5.4 锁解析

```
npm ci --dry-run --ignore-scripts   EXIT=0
npm ls --package-lock-only --ignore-scripts nanoid postcss undici vite jsdom   EXIT=0
```

图：`vite@8.0.16 → postcss@8.5.23 → nanoid@3.3.18`；`jsdom@29.1.1 → undici@7.29.0`。dry-run 前后主锁哈希仍 `65254915c5c85e3bf5b26659d0acd27b2bdfc94c6b5954d15e4db81a6877e6bc`，`node_modules` mtime 未因本席命令改变。**未**跑写入式 `npm ci`，不声称真实安装/应用行为 PASS。

---

## 6. 兼容性（GEN-008/013）

宿主 npm 11.19.0 / node v24.20.0。项目 `packageManager` npm@10.9.8，父清单未改。

semver（npm 自带 semver）：

| 断言 | 结果 |
|---|---|
| `3.3.18` satisfies `^3.3.16`（修后 postcss 约束） | true |
| `3.3.18` satisfies `^3.3.12` | true |
| `8.5.23` satisfies vite `^8.5.15` | true |
| `7.29.0` satisfies jsdom `^7.25.0` | true |
| `3.3.12` satisfies `^3.3.16` | false（故 hunk4 与 hunk2 必须同批） |

父范围本身未改：`node_modules/vite.dependencies.postcss` 仍 `^8.5.15`；`node_modules/jsdom.dependencies.undici` 仍 `^7.25.0`。根/web `package.json` 无直接 nanoid/postcss/undici。三包 `engines` 与修前相同。无 overrides、无主版本跳跃、无新依赖名。

Shrinkwrap.load（只读，未写盘）：`data.documentVersion==='V0.01'`；`toJSON()` 保留该键、packages 220、去掉格式 3 不该出现的顶层 `dependencies`；主锁哈希不变。

观察（**不构成 FAIL**）：磁盘键序为 `lockfileVersion → documentVersion → requires`；`toString()` 会排成 `lockfileVersion → requires → documentVersion`。JSON 对象键序非协议语义；findings 未规定键位；本 diff 与派发全文一致。日后 npm 重写锁可能只动键序，那是下一轮的事，不是本 diff 未归因行。

---

## 7. 最小性（GEN-001，ponytail-review）

授权边界：10 个既有依赖字段 + 1 行文件自身版本。实改正好这些。无第二份 nanoid/postcss/undici 记录。无 audit fix 整树、无升 vite/jsdom、无业务 CSS/HTTP 代码、无测试夹具、无 HTML。

ponytail-review：`Lean already. Ship.` `net: -0 lines possible`（在已定稿队列「是」之内无可删行；documentVersion 被用户版本铁律 + DEC-R8-96 + 已定稿 001 钉死，不能当死字段删）。

004—006 只登记、队列否：本 diff 未改锁格式/根镜像/其余 217 记录/SRI 集合，符合「否」。

---

## 8. 越界判定

闸 3：每一变更行都能指到 findings 中已定稿且队列为「是」的 ID。

- 唯一路径 `package-lock.json` ∈ M004 模块清单
- 无仅 after/仅 before 路径
- 3514 路径中其余 3513 条 blob 全等（含 35 个 .scratch 与三份 HTML）
- ledger B3 计数在 after tree **之后**才变为 1，故台账改动不在本修复 diff 内——两树比对证实 ledger 路径未变
- 无添加项、无 HTML 申请、无 LOCK-AMEND

**越界判定：PASS**

---

## 9. 三线

### 第一性原理

- 为什么必须存在？删掉 hunk2/3/4/5，GEN-010/012/013 下已证库缺陷（无限循环 / 绝对 map 读取 / Cache-Control 解析崩溃）会继续被锁钉死。删掉 hunk1，用户版本铁律与 DEC-R8-96「其他文件仍执行 Vx.xx」在本文件上无落点（`version`/`lockfileVersion` 不能冒充文件自身版本）。
- 声称行为是否执行验证？是：before audit high3 → after 0；官方 SRI 实下核对；隔离包反例；npm ci dry-run 0。
- 未验证就当真的前提？**未**跑写入式安装与前端单测；不把 dry-run 当运行时 PASS。三项均为 dev，不声称生产可利用。

### 顺从式

假定 B3 按定稿队列实施。对照 V0.45 队列表与修复边界：001 nanoid 3.3.18 三字段；002 postcss 8.5.23 三字段 + nanoid `^3.3.16`；003 undici 7.29.0 三字段；001 附加 `documentVersion V0.01`；004—006 不动。全部命中。18 GEN 无新主张。

### 对抗式（grilling 自答）

1. 是否只审了已提交 diff？否。两棵 write-tree 级 tree，3514 路径闭包，唯一变更 lock。
2. 是否把 nanoid 约束误归 001？否。findings 002 明文「按官方 8.5.23 元数据改 dependencies.nanoid」；registry 证实。
3. 是否漏掉第二份包副本？`packages` 中各仅一条 `node_modules/{nanoid,postcss,undici}`。
4. documentVersion 是否 2.2 添加项？无业务发明、无 HTML 冲突，B2 已判技术性；本 diff 未发明新业务字段语义。
5. 是否顺手改了应用 semver？`version` 仍 `0.1.0`。
6. npm 是否拒绝未知顶层键？dry-run 0；Shrinkwrap 保留。help 未列出 ≠ 拒绝。
7. 隔离 postcss 是否误用工作区旧 postcss？否。加载的是 `/tmp/.../extract/postcss` 的 8.5.23；NODE_PATH 仅提供 picocolors/source-map-js。

---

## 10. 本轮命令与关键输出（verification-before-completion）

1. `sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`
2. `head -n 2 docs/audit-2026-09/ledger.md` → 锁哈希同上，`LOCK-AMEND-COUNT: 0`
3. `git cat-file -t` 两 tree → `tree` / `tree`
4. `git ls-tree -r --name-only` → 3514 / 3514
5. 全量 blob 比对 → changed=1 `package-lock.json`
6. `git diff --binary <before> <after> | sha256sum` → `59384beb2fb6844714b818d17319992f11012b7737988e574b11dbbe8ab56563`，2259 字节，5 hunk
7. `sha256sum package-lock.json` 与 after blob → 均为 `65254915c5c85e3bf5b26659d0acd27b2bdfc94c6b5954d15e4db81a6877e6bc`
8. JSON deep-diff → 3 条包记录 + 1 顶层键；217 记录不变；`after_equals_reconstructed True`
9. `.scratch/` 35/35 哈希全等
10. `npm ci --dry-run --ignore-scripts` EXIT 0；锁哈希不变
11. `npm ls --package-lock-only` EXIT 0，钉死 3.3.18 / 8.5.23 / 7.29.0
12. after `npm audit --package-lock-only` EXIT 0 total 0
13. before 隔离 audit EXIT 1 high 3（仅这三包）
14. registry+实下 SRI 三包全匹配；postcss 官方 nanoid `^3.3.16`
15. 隔离 nanoid/postcss/undici 反例如 §5.3
16. Shrinkwrap.load 保留 `documentVersion=V0.01`，未写盘
17. 输出文件调用前 stat：size=20152 inode=1595730（本席未读其正文）

主工作区未 `npm install` / 未 reify。

---

## 11. 结论

| 项 | 结论 |
|---|---|
| 完整 diff 覆盖 tracked/staged/untracked（两棵 3514 路径 tree） | PASS |
| 与派发 binary diff 全文/哈希一致 | PASS |
| 5 hunk 归因 001 / 001 / 002 / 002 / 003 | PASS |
| 逐变更行均可归队列「是」的已定稿 ID | PASS |
| 正确性（钉死修补版 + SRI + 隔离反例 + audit 清零） | PASS |
| 兼容性（父范围、engines、dry-run、Shrinkwrap 保留扩展） | PASS |
| 最小性（10 字段 + 1 行版本元数据，无其它路径） | PASS |
| 越界 | **PASS** |
| **B4-r1 本席总决** | **PASS** |

不进入 B5，不修源，不写 ledger/lock/HTML。首批 B2 FAIL 原文仍在 b1 grok raw，不被本票改写。

B4 结束。

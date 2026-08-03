# AGENTS.md

## Agent skills

### Issue tracker

Issue 与 PRD 记在本仓库的 GitHub Issues（`jxh1979/mall`），用 `gh` CLI 操作。详见 `docs/agents/issue-tracker.md`。

### Triage labels

沿用官方五个默认标签名，其中 `wontfix` 复用仓库已有标签。详见 `docs/agents/triage-labels.md`。

### Domain docs

single-context 布局，词表在仓库根的 `CONTEXT.md`。详见 `docs/agents/domain.md`。

## 项目铁律

以下规则**覆盖任何 skill 的默认行为**。skill 的通用指示与此冲突时，一律以此为准。

### 代码改动

1. **禁止在主工作区 `~/complex-management` 提交。** 任何改动先
   `git worktree add ~/wt/<名字> -b feature/<名字> origin/main`，在 worktree 里改；
   需要跑构建或测试时软链 `node_modules`。主工作区多会话共用，在里面提交会被别的会话
   `reset --hard` 连分支 ref 一起拨走（已实际丢过一次提交）。
   ⚠️ `/implement` 的默认收尾动作是 "commit to the current branch" —— **必须先开好
   worktree 再让它跑**，否则直接提交到 main。

2. **禁直推 main。** 一律 feature 分支 + `gh pr create` + `gh pr merge`。
   仓库装了 pre-push 钩子，直推会被拦，但提交已经落在 main 上了。

3. **动手前先 `git fetch origin`**，核对 `HEAD == origin/main`。
   多会话并发推进 origin，凭印象开分支会基于陈旧 main。

4. **任何功能或代码改动，同步更新对应 PRD**，不留脱节。

### 业务口径

5. **三份业务 HTML 拥有最终解释权，任何 skill 不得自行编辑。**
   改动前必须逐句出草案，经用户逐条批准：
   - `docs/business-system-map-for-operations-v0.1.html`
   - `docs/platform-governance-map-for-operations-v0.1.html`
   - `docs/business-enum-quick-reference-v0.1.html`

   PRD、开发设计、接口、数据库、UI、测试和实现均不得反向改写其中定义。

6. **删除一律软删除**：存库 + 前台不显 + 可恢复 + 分类 + 填理由 + `role_platform_admin`
   批准。唯一例外是 PIPL 约束下的人脸照。

7. **所有展示文字全中文**，仅机读标识（枚举值、字段名、日志键）保留原文。
   后台、客人端、设备端、安卓端一律适用。

8. **不为存量数据做兼容、退化或回填设计。** 库里数据最后都要删光，
   不要拿"旧数据不满足新口径怎么办"当设计约束。

### 工程操作

9. **数据库迁移 V 号串行生成，绝不并发。** 共享测试库 `complex_test` 已因并发迁移
   造成版本乱序漂移，翻过两次车。
   机器闸门：`npm run verify:migration-version-order`（已进 `verify:delivery-gates`）——
   本分支新增的迁移 V 号必须大于 `origin/main` 当前最大 V 号，否则红。
   未挂 `verify:predeploy`：GitHub Actions 用 `actions/checkout` 默认浅克隆，
   PR 分支上取不到 `origin/main` ref，挂上去会在 CI 里恒红。

10. **跑测试只 export 三个 DB 环境变量**（`COMPLEX_DATABASE_URL` / `_USERNAME` /
    `_PASSWORD`），**不要 `source` 整个 env 文件**。测试库是 `complex_test`，
    不是 `complex`。纯单元测试无需数据库。

11. **部署走 `MAVEN_ARGS=-DskipTests`。** 部署时跑全量测试会撞上 Spring
    `failure.threshold=1`，单个失败级联毒化整个构建。

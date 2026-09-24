CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# M038 B4-r2 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r2
席位：固定 Grok 独立只读审计席乙（grok-4.6 / xhigh）
计划：160 审计 V0.17 B4 与 §5 闸 3
本文件路径：`docs/audit-2026-09/modules/M038-b4-r2-grok-raw.md`
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

模块：M038 / 功能权限点、职级三十档、权限模板与最终权限计算／网页与验证／第1段（计划 16 文件）。
本轮：席甲 B3-r2 整批修复之后的 B4（B3 第 2 次）。
修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r2 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。席甲为作者自审票；本席 FAIL 具否决权，本轮不行使。§5 闸 3 越界判定同样须双 PASS。

未打开：`modules/M038-b4-r2-self-raw.md`（开工时 `ls` 命中该文件名与字节数，未整文件打开、未采信其票面）。已读：`160-audit-prompt.md` V0.17 B4／§5 闸 3／§6、`findings.md` V3.89 顶部「当前 M038 现锁 B2-r3 双PASS定稿」、本席 `M038-b1-grok-raw.md` V0.05（B2-r3 允许/禁止表与 012 非阻断实施注）、冻结 diff、`m038_b3r2_run.log`（旁证）、两 tree 与 after 工作区四文件 blob。ledger 只读取模块状态与 B3 次数，不写入。本席未采信 ledger 里对席甲本轮票面的转述。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff；M038-011、M038-012 是否按 findings V3.89 定稿最小处置与本席 B2-r3 边界实现；是否最懒可行（ponytail ultra）；是否引入新缺陷或新业务发明；每一变更行能否归因到归属「是」的 ID；新测试断言能否失败；可执行位与文件模式是否未变。只用既有冻结判据。未新造判据/角色，未询问操作员。对抗式按 grilling 追问结构自问自答，未派子代理。

---

## 0. 技能预检与节点应用（本环境实核）

本席按 `160-audit-prompt.md` V0.17 §6 在 **本机路径** 核验，不抄 Codex 预检表。grilling 只借追问树自问自答（事实自己读；技能本体「问人 / 派子代理查事实」作废）。Arc `audit` 本体要派评审子代理并写 `docs/arc/audits/`，与本轮「禁子代理、唯一可写 grok-raw」冲突，**不执行**。`requesting-code-review` 本体的 `HEAD~1` / 同会话子代理复审 / 无 ID 即刻修，一律不用——本席自身即 B4 外审。`receiving-code-review` 本节点不适用（本席产出意见，不收意见去修）。`test-driven-development` / mattpocock `tdd`：本轮只核 B3 已落的红绿并本席重跑 BASE/RED/GREEN，不新写测试。`resolving-merge-conflicts`：工作区无进行中 merge/rebase，节点不适用。`writing-for-agents`：本 raw 按完成准则写给下游合取席读。`brainstorming` / `writing-plans`：阶段 A 技能，B4 不适用。`ponytail`：B3 默认强度，B4 只审不修。`long-running-background-tasks`：本席 vitest/tsc 在一次性 worktree 内同步跑完，无后台作业遗留。`graft`：本环境存在，已用于定位 `runWrite`。Trail of Bits `differential-review` 本体强制子代理，**不用**。提示词划掉的 `using-git-worktrees`：B3/B4/B5 主工作区就地审；本轮用户指令授权 origin/main 一次性 worktree 仅用于复跑，已 `git worktree remove` 删除、未用 `--force`。

| 技能 | 本环境路径 | 本节点用法 | 缺口 |
|---|---|---|---|
| `using-superpowers` | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前技能清单；用户指令优先于技能本体 | 无 |
| `using-arc` | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 路由识别；**不执行** Arc 全量审计工作流 | 本体要派评审子代理，记缺口 |
| Arc `audit` | howells `skills/audit/SKILL.md` | 同上 | **不用**（第三席+§7 外产物） |
| `graft` | `~/.claude/skills/graft/SKILL.md` | `graft grep runWrite --in apps/web/src/admin/pages/` 定位治理页互斥包装 | 无 |
| `grilling` / `grill-me` | mattpocock `skills/productivity/grilling/SKILL.md` | 对抗式自问自答；作废等用户/派子代理 | 无 |
| `batch-grill-me` | — | — | **本机技能树不存在**，记缺口，不冒充已调用 |
| `ubiquitous-language` | — | — | **不存在**，记缺口 |
| `codebase-design` | mattpocock `engineering/codebase-design/SKILL.md` | 测试 seam 对齐页内 `runExclusive`／`createUser` 内层 catch，不改被测闸语义；不派 DESIGN-IT-TWICE 子代理 | 无 |
| `diagnosing-bugs` | 同包 `engineering/diagnosing-bugs/SKILL.md` | 红环=只读命令（三工具 sha256、`cmp` 冻结 diff、worktree vitest/tsc）；Tight loop 为三条新用例 | 未改被测 |
| `systematic-debugging` | superpowers | 只借读错误→追数据流（定稿声称→页内包装／内层 catch→测试断言）；作废插桩 | 无 |
| `verification-before-completion` | superpowers | PASS 前本席实算哈希/`cmp`/四文件 blob/worktree 11 文件 BASE 57 绿 → RED 3 红 → GREEN 60 绿 + tsc | 全量 928 未本席重跑 |
| `requesting-code-review` | superpowers | B4=本席即外审；基线=两 tree id 而非 `HEAD~1`；不派子代理 | 本体子代理路径作废 |
| `ponytail-review` | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只找可删的越界添加 | 不落地 |
| `tdd` / `test-driven-development` | 存在 | 核 011/012 新断言能否失败；本席重跑 RED 三红、GREEN 60；不改被测 | 无 |
| `long-running-background-tasks` | grok bundled | vitest/tsc 在 worktree 内跑完后删除 worktree | 无遗留 |
| Trail of Bits `audit-context-building` / `differential-review` | 存在 | 本体强制子代理 | **不用** |
| `resolving-merge-conflicts` | 存在 | 工作区无 merge/rebase | 节点不适用 |
| `writing-for-agents` | 存在 | 本 raw 给下游合取席 | 无 |
| `ponytail` | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail/SKILL.md` | B4 只审：页内一包装 + 内层 catch，ultra 已满足 | 不修 |

明确不用（提示词已划掉，本席亦不调用）：`dispatching-parallel-agents`、`subagent-driven-development`、`executing-plans`、`research`、`code-review`（mattpocock）、`domain-modeling`、`ponytail-audit`。未使用 MCP。未建脚本/持久日志/临时审计产物（一次性 worktree `/tmp/m038-b4-r2-grok-wt` 已删除；`cmp` 用过的 `/tmp/m038_b4_r2_live.diff` 已删）。

graft 本席调用节省合计约 12207 tokens（`runWrite` 一次）。

---

## 1. 输入与独立锁实算

本席哈希命令（独立实算，未抄 sidecar / ledger / 派发期望值）：

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
python3 -c "import hashlib; print(hashlib.sha256(open('docs/audit-2026-09/plan/criteria-lock.md','rb').read()).hexdigest())"
openssl dgst -sha256 docs/audit-2026-09/plan/criteria-lock.md
```

三工具同值，与本文件第一行独占锁行一致。文件 891182 字节。sidecar 一行同值（对照，非抄写来源）。ledger 顶部基准同值（对照）。criteria-lock 版本仍 V0.14。LOCK-AMEND-COUNT 仍为 1。未 LOCK-AMEND。本 diff 未改锁。

| 项 | 值 |
|---|---|
| 模块 | M038 网页与验证／第1段（计划 16 文件；本轮 B3-r2 写入集合＝模板页+测试、账号页+测试） |
| 阶段 | B4-r2 只读审计 B3 第 2 次整批修复的「修复内容」本身 |
| 本轮修复队列 | findings.md V3.89：M038-011、M038-012 技术性／是／是；001…010 否（已修） |
| findings 全文 SHA256 | `39ce67bbcfdb1083aa28946ebe282c89be84a919adea6c43013b7ec109c5ce6d`（本席实算）；1349872 字节；版本 V3.89 |
| B3 | ledger 只读：已执行 2 次；模块仍 IN_PROGRESS |
| before（对象类型 tree） | `97cd4f676f7e8d969642f5424dfaa79854f72f84` |
| after（对象类型 tree） | `19ab58e07b9686054d8f4fd2fb0f279b727dc34a` |
| `git cat-file -t` | 两者均为 `tree` |
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466` |
| origin/main | `53dec14170f01a6b8ebb1b7beb34f5aaf10da2fb`（未 fetch） |
| MERGE_HEAD | 无（`test -f .git/MERGE_HEAD` → NO_MERGE） |
| 160 提示词 SHA256 | `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`（V0.17） |
| 本席 B2-r3 全文 SHA256 | `91fafc4d9280288c2bdf19d27fb988794ec12b6b2544a81000bfe301a3a6d7ab`（`M038-b1-grok-raw.md` 137922 字节，V0.05） |
| 冻结 diff | `/home/james/audit-tmp/m038_b3r2.diff` 13141 字节 |
| 冻结 diff SHA256（三工具） | `27892c36c2ffebe60aa9c81df5b6d1b8158ae6913db1e62645f45fcf5340eecd` |
| 实算 `git diff --binary <before> <after>` | 与冻结文件 `cmp` 逐字节相同 |
| `git diff --raw` | 四文件皆 `:100644 100644 … M`，模式未变 |
| `--numstat` | 147 insertions, 10 deletions |
| 工作区四文件 blob vs after | 四者全等 |

冻结判据本轮引用（锁正文，未扩）：

- GEN-002 可执行正确性（L17–20）
- GEN-003 测试有效性（断言须能失败）
- GEN-006 数据完整性
- GEN-007 并发与幂等
- GEN-009 错误处理与恢复

三 HTML 本 diff 未改（对照 SHA：BEQR `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`、BSM `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66`、PGM `5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f`）。

---

## 2. 本轮 diff 全文

路径：`/home/james/audit-tmp/m038_b3r2.diff`。214 行。与 `git diff --binary 97cd4f676f7e8d969642f5424dfaa79854f72f84 19ab58e07b9686054d8f4fd2fb0f279b727dc34a` 逐字节相同。

```diff
diff --git a/apps/web/src/admin/pages/AccessUsersPage.test.tsx b/apps/web/src/admin/pages/AccessUsersPage.test.tsx
index ac386bfc..c9c22140 100644
--- a/apps/web/src/admin/pages/AccessUsersPage.test.tsx
+++ b/apps/web/src/admin/pages/AccessUsersPage.test.tsx
@@ -134,3 +134,58 @@ describe("M038 员工账号页回归", () => {
     expect(Array.from(status.options).map((option) => option.textContent)).toEqual(["有效", "待激活", "已禁用"]);
   });
 });
+// M038-012：新建外部协作账号时账号已落库、套用权限模板被拒——账号照常入表并关弹窗，提示写清「账号已创建；套用权限模板失败」。
+describe("M038-012 员工账号页创建后套模板失败", () => {
+  afterEach(() => {
+    vi.unstubAllGlobals();
+    vi.restoreAllMocks();
+  });
+
+  it("账号 POST 成功、套模板 403：账号只发一笔且入表，弹窗关闭，提示含「账号已创建」与失败原因", async () => {
+    const posts: string[] = [];
+    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
+      const url = String(input);
+      if ((init?.method ?? "GET").toUpperCase() === "POST") {
+        posts.push(url);
+        if (url.includes("/apply")) {
+          return Promise.resolve(new Response(
+            JSON.stringify({ success: false, code: "GRANT_REQUIRES_APPROVAL", message: "以下权限的授予需要审批：[a.b]" }),
+            { status: 403, headers: { "content-type": "application/json" } },
+          ));
+        }
+        return Promise.resolve(new Response(
+          JSON.stringify({ success: true, code: "OK", message: "success", data: { id: "u-new", username: "vendor-a", account_type: "outsourced_vendor", status: "active", organization_id: "org-1" } }),
+          { status: 200, headers: { "content-type": "application/json" } },
+        ));
+      }
+      if (url.includes("permission-templates")) return Promise.resolve(envelope(TEMPLATES));
+      if (url.includes("business-entities")) return Promise.resolve(envelope(ENTITIES));
+      if (url.includes("merchants")) return Promise.resolve(envelope(MERCHANTS));
+      if (url.includes("access/users")) return Promise.resolve(envelope(USERS));
+      return Promise.resolve(envelope([]));
+    }));
+    const props: AdminPageProps = {
+      api: new AdminApi("test-token"),
+      user: { id: "u1", username: "admin", accountType: "platform_admin" },
+      setActionResult: vi.fn(),
+      setGlobalError: vi.fn(),
+      effectivePermissions: ADMIN_PERMS,
+    };
+    render(<AccessUsersPage {...props} />);
+    await waitFor(() => expect(screen.getByTestId("access-user-row-u-1")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("access-user-create-open"));
+    fireEvent.change(screen.getByTestId("access-user-account-type"), { target: { value: "outsourced_vendor" } });
+    fireEvent.change(screen.getByTestId("access-user-name"), { target: { value: "vendor-a" } });
+    fireEvent.change(screen.getByTestId("access-user-password"), { target: { value: "init-pass" } });
+    fireEvent.change(screen.getByTestId("access-user-org"), { target: { value: "org-1" } });
+    await waitFor(() => expect((screen.getByTestId("access-user-template") as HTMLSelectElement).value).toBe("tpl-1"));
+    fireEvent.click(screen.getByTestId("access-user-flow-next"));
+    fireEvent.click(await waitFor(() => screen.getByTestId("access-user-create")));
+    await waitFor(() => expect(screen.getByTestId("access-user-row-u-new")).toBeTruthy());
+    expect(posts.filter((url) => url.includes("/access/users"))).toHaveLength(1);
+    expect(posts.filter((url) => url.includes("/permission-templates/tpl-1/apply"))).toHaveLength(1);
+    await waitFor(() => expect(screen.queryByTestId("access-user-create")).toBeNull());
+    expect(props.setActionResult).toHaveBeenCalledWith(expect.stringMatching(/创建员工：vendor-a.*账号已创建；套用权限模板 tpl-1 失败：/));
+    expect(props.setGlobalError).toHaveBeenLastCalledWith(expect.stringMatching(/\S/));
+  });
+});
diff --git a/apps/web/src/admin/pages/AccessUsersPage.tsx b/apps/web/src/admin/pages/AccessUsersPage.tsx
index 6555b949..f01af60f 100644
--- a/apps/web/src/admin/pages/AccessUsersPage.tsx
+++ b/apps/web/src/admin/pages/AccessUsersPage.tsx
@@ -133,16 +133,27 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv
         evidenceAttachmentIds: evidenceAttachmentIds.split(/[,，\n]/).map((item) => item.trim()).filter(Boolean),
         operatorNote: "后台手动录入工作人员",
       });
+      // M038-012：账号已落库后套模板被拒不算整笔失败——账号照常入表、关弹窗，提示写清「账号已创建；套用权限模板失败」。
+      let applyError = "";
       if (selectedTemplateId) {
-        await api.post(`/api/v1/access/permission-templates/${encodeURIComponent(selectedTemplateId)}/apply`, {
-          targetUserIds: [created.id],
-          mode: "append",
-          operatorNote: "后台手动录入后应用权限模板",
-        });
+        try {
+          await api.post(`/api/v1/access/permission-templates/${encodeURIComponent(selectedTemplateId)}/apply`, {
+            targetUserIds: [created.id],
+            mode: "append",
+            operatorNote: "后台手动录入后应用权限模板",
+          });
+        } catch (applyFailure) {
+          applyError = applyFailure instanceof Error ? applyFailure.message : "套用权限模板失败";
+        }
       }
       const enriched = { ...created, organizationId: organizationId.trim(), merchantId: merchantId.trim() || created.merchantId, status, accountType };
       setRows((current) => [...current, enriched]);
-      setActionResult(`${selectedTemplateId ? "创建员工并赋权" : "创建员工"}：${created.username}（${formatBusinessStatus(accountType)}）${selectedTemplateId ? `，已应用权限模板 ${selectedTemplateId}` : ""}`);
+      if (applyError) {
+        setGlobalError(applyError);
+        setActionResult(`创建员工：${created.username}（${formatBusinessStatus(accountType)}），账号已创建；套用权限模板 ${selectedTemplateId} 失败：${applyError}`);
+      } else {
+        setActionResult(`${selectedTemplateId ? "创建员工并赋权" : "创建员工"}：${created.username}（${formatBusinessStatus(accountType)}）${selectedTemplateId ? `，已应用权限模板 ${selectedTemplateId}` : ""}`);
+      }
       setUsername("");
       setPassword("");
       setConsentReference("");
diff --git a/apps/web/src/admin/pages/PermissionTemplatesPage.test.tsx b/apps/web/src/admin/pages/PermissionTemplatesPage.test.tsx
index 266a7f57..9aadaf76 100644
--- a/apps/web/src/admin/pages/PermissionTemplatesPage.test.tsx
+++ b/apps/web/src/admin/pages/PermissionTemplatesPage.test.tsx
@@ -151,3 +151,61 @@ describe("M038 权限模板页回归", () => {
     expect(new Date(String(putBody!.valid_until)).getTime()).toBe(new Date("2026-06-15T00:00:00Z").getTime());
   });
 });
+
+// M038-011：复制模板、应用模板提交未完成时再次点击不发第二笔，按钮置为禁用。
+describe("M038-011 权限模板页复制/应用防连点", () => {
+  afterEach(() => {
+    vi.unstubAllGlobals();
+    vi.restoreAllMocks();
+  });
+
+  function renderPending() {
+    const posts: string[] = [];
+    let release: (value: Response) => void = () => undefined;
+    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
+      const url = String(input);
+      if ((init?.method ?? "GET").toUpperCase() === "POST") {
+        posts.push(url);
+        return new Promise<Response>((resolve) => { release = resolve; });
+      }
+      if (url.includes("permission-templates")) return Promise.resolve(envelope(TEMPLATES));
+      if (url.includes("access/permissions")) return Promise.resolve(envelope(PERMISSIONS));
+      if (url.includes("access/users")) return Promise.resolve(envelope(USERS));
+      return Promise.resolve(envelope([]));
+    }));
+    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" },
+      setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
+    render(<PermissionTemplatesPage {...props} />);
+    const ok = () => release(new Response(JSON.stringify({ success: true, code: "OK", message: "success", data: { id: "tpl-copy" } }),
+      { status: 200, headers: { "content-type": "application/json" } }));
+    return { posts, ok };
+  }
+
+  it("连点两次「复制模板」只发一笔，提交中按钮禁用，完成后恢复", async () => {
+    const { posts, ok } = renderPending();
+    await waitFor(() => expect((screen.getByTestId("permission-template-copy") as HTMLButtonElement).disabled).toBe(false));
+    fireEvent.click(screen.getByTestId("permission-template-copy"));
+    fireEvent.click(screen.getByTestId("permission-template-copy"));
+    await waitFor(() => expect(posts).toHaveLength(1));
+    expect(posts[0]).toContain("/permission-templates/tpl-1/copy");
+    await waitFor(() => expect((screen.getByTestId("permission-template-copy") as HTMLButtonElement).disabled).toBe(true));
+    expect((screen.getByTestId("permission-template-apply") as HTMLButtonElement).disabled).toBe(true);
+    ok();
+    await waitFor(() => expect((screen.getByTestId("permission-template-copy") as HTMLButtonElement).disabled).toBe(false));
+    expect(posts.filter((url) => url.includes("/copy"))).toHaveLength(1);
+  });
+
+  it("连点两次「应用模板」并都确认，只发一笔", async () => {
+    vi.spyOn(window, "confirm").mockReturnValue(true);
+    const { posts, ok } = renderPending();
+    await waitFor(() => expect((screen.getByTestId("permission-template-apply") as HTMLButtonElement).disabled).toBe(false));
+    fireEvent.click(screen.getByTestId("permission-template-apply"));
+    fireEvent.click(screen.getByTestId("permission-template-apply"));
+    await waitFor(() => expect(posts).toHaveLength(1));
+    expect(posts[0]).toContain("/permission-templates/tpl-1/apply");
+    await waitFor(() => expect((screen.getByTestId("permission-template-apply") as HTMLButtonElement).disabled).toBe(true));
+    ok();
+    await waitFor(() => expect((screen.getByTestId("permission-template-apply") as HTMLButtonElement).disabled).toBe(false));
+    expect(posts.filter((url) => url.includes("/apply"))).toHaveLength(1);
+  });
+});
diff --git a/apps/web/src/admin/pages/PermissionTemplatesPage.tsx b/apps/web/src/admin/pages/PermissionTemplatesPage.tsx
index 47478fa3..506e24e6 100644
--- a/apps/web/src/admin/pages/PermissionTemplatesPage.tsx
+++ b/apps/web/src/admin/pages/PermissionTemplatesPage.tsx
@@ -77,6 +77,19 @@ export function PermissionTemplatesPage({ api, setActionResult, setGlobalError,
   const [copyName, setCopyName] = React.useState("普通员工01");
   const [formError, setFormError] = React.useState("");
   const [submitting, setSubmitting] = React.useState(false);
+  // M038-011：复制、应用与「保存修改」共用提交中态——提交中按钮禁用，连点不发第二笔。
+  const exclusiveRef = React.useRef(false);
+  async function runExclusive(action: () => Promise<void>) {
+    if (exclusiveRef.current) return;
+    exclusiveRef.current = true;
+    setSubmitting(true);
+    try {
+      await action();
+    } finally {
+      exclusiveRef.current = false;
+      setSubmitting(false);
+    }
+  }
   // 引导式流程 phase-11：新建权限模板收起为主按钮 + 分步引导弹窗（选权限网格作为其中一步）。
   // 独立 state：与内联「选中编辑」表单完全隔离，避免选中行的 useEffect 回填污染新建字段。
   const [createOpen, setCreateOpen] = React.useState(false);
@@ -407,8 +420,8 @@ export function PermissionTemplatesPage({ api, setActionResult, setGlobalError,
           <PermissionButton
             permissionsState={effectivePermissions}
             permissions={[TEMPLATE_MANAGE_PERMISSION]}
-            disabled={!selected?.id}
-            onClick={() => void copyTemplate()}
+            disabled={submitting || !selected?.id}
+            onClick={() => void runExclusive(copyTemplate)}
             data-testid="permission-template-copy"
           >
             复制模板
@@ -419,9 +432,9 @@ export function PermissionTemplatesPage({ api, setActionResult, setGlobalError,
           <HighRiskConfirm
             label="应用模板"
             impact="将把选中模板的权限项应用到目标账号并落库；单次有效模式会影响目标账号实际可操作范围。"
-            disabled={!selected?.id}
+            disabled={submitting || !selected?.id}
             precheck={validateApply}
-            onConfirm={() => void applyTemplate()}
+            onConfirm={() => void runExclusive(applyTemplate)}
             permissionsState={effectivePermissions}
             permissions={[TEMPLATE_APPLY_PERMISSION]}
             data-testid="permission-template-apply"
```

---

## 3. 逐块归因表（§5 闸 3）

可归因 ID 闭集＝findings V3.89 已定稿且修复队列归属「是」：`M038-011`、`M038-012`。001…010 本轮队列「否」，不得承载本 diff 任何行。

计数口径：unified diff 214 行全覆盖；`+` 147 / `-` 10 与 `--numstat` 一致。文件头、`@@`、上下文行随其所在文件块归同一 ID。

| 块 | diff 行 | 文件 | hunk | +/−/ctx/meta | ID | 理由 |
|---|---|---|---|---|---|---|
| A | 1–63 | `AccessUsersPage.test.tsx` | `@@ -134,3 +134,58` | +55 / −0 / ctx3 / meta5 | M038-012 | 定稿要求本文件补一条 apply 403：账号 POST 一笔、新行入表、弹窗关闭、提示含「账号已创建」与「失败」。整块为该用例及其 describe |
| B | 64–101 | `AccessUsersPage.tsx` | `@@ -133,16 +133,27` | +17 / −6 / ctx10 / meta5 | M038-012 | 套模板单独 try/catch；失败入表、清字段、关窗、改结果提示、`setGlobalError`；成功文案留在 else。上下文为账号 POST 之后、清字段之前 |
| C | 102–167 | `PermissionTemplatesPage.test.tsx` | `@@ -151,3 +151,61` | +58 / −0 / ctx3 / meta5 | M038-011 | 定稿要求本文件补两条：复制连点一笔且提交中两按钮禁用；应用连点都确认只发一笔 |
| D | 168–191 | `PermissionTemplatesPage.tsx` | `@@ -77,6 +77,19` | +13 / −0 | M038-011 | 页内 `exclusiveRef` + `runExclusive`（ref 挡并发、沿用 `submitting`）。注释写共用提交中态，未改保存函数体 |
| E | 192–202 | 同上 | `@@ -407,8 +420,8` | +2 / −2 | M038-011 | 复制按钮 `disabled` 并入 `submitting`，`onClick` 走包装 |
| F | 203–214 | 同上 | `@@ -419,9 +432,9` | +2 / −2 | M038-011 | 应用 `HighRiskConfirm` 的 `disabled` 并入 `submitting`，`onConfirm` 走包装；组件本身未改 |

D+E+F 合计 `PermissionTemplatesPage.tsx`：+17 / −4 / ctx19 / meta7（3 hunk），与 `--numstat` 17 4 一致。

± 行按 ID：

| ID | + | − | ± 合计 |
|---|---:|---:|---:|
| M038-011 | 75 | 4 | 79 |
| M038-012 | 72 | 6 | 78 |
| 无 ID / 越界 | 0 | 0 | 0 |
| 合计 | 147 | 10 | 157 |

路径闭集＝定稿写入集合四文件，无第五路径。`git diff --name-only` 仅这四条。`HighRiskConfirm.tsx`、`GuidedFlowModal.tsx`、`Can.tsx`、`adminClient.ts`、`statusLabels.ts`、后端、OpenAPI、迁移、三 HTML、锁：before/after blob 全等（抽核组件与客户端/字典）。

无「顺手优化」「换测试框架」「新抽象层」「新建测试文件」。新断言均落在既有测试文件末尾，归因到对应 ID（闸 3：为 finding 写的验证性测试合法）。

**归因票：PASS。** 214 行均可归到 011 或 012。无越界行。

---

## 4. 内容审计

对照：findings V3.89 最小处置 + 本席 B2-r3 允许/禁止表（`M038-b1-grok-raw.md` V0.05）。

### 4.1 M038-011（GEN-007 / GEN-002）

定稿五条：① 模板页加与 `runWrite` 同构的互斥包装（ref 挡并发、沿用现有 `submitting`）；② 复制与应用走它；③ 两按钮 `disabled` 并入 `submitting`；④ 测试两条如上述；⑤ 不改 HighRiskConfirm、公共字典、客户端与后端。

治理页对照（graft + 点读 `AccessGovernancePage.tsx` L116–128）：

```
if (writeBusyRef.current) return;
writeBusyRef.current = true;
setWriteBusy(true);
try { await action(); } finally { writeBusyRef.current = false; setWriteBusy(false); }
```

本轮 `runExclusive`：同一闸序（先读 ref、置 true、置 submitting、try/finally 复位）。差集：未抄 `runWrite` 的 `setGlobalError(null)` 与 catch 兜底——`copyTemplate` / `applyTemplate` 自身已有 try/catch 与清错。定稿写的同构是「ref + submitting」，不是把错误处理再包一层。未新造 busy 状态。

复制：`disabled={submitting || !selected?.id}`，`onClick={() => void runExclusive(copyTemplate)}`。应用：同 disabled，`onConfirm={() => void runExclusive(applyTemplate)}`。`copyTemplate` / `applyTemplate` 函数体本 diff 未改。

保存修改：仍 `onClick={() => void saveTemplate()}`，自身 `setSubmitting(true)`（L267），按钮已 `disabled={submitting || !selected?.id}`。B2-r3 允许表写「可选把保存修改也走同一包装」——未做，合法。注释「与保存修改共用提交中态」属实：三按钮读同一 `submitting`。

`HighRiskConfirm.tsx` blob 两 tree 全等。其 `handleClick` 仍不读 `disabled`（只拦 denied / precheck）；`disabled` 落到原生 `button.disabled`。同拍第二下由 `exclusiveRef` 同步挡住（测试两下 `fireEvent.click` 在 after 为 1 笔）。未给复制加二次确认。未改 guidedcreate 测试。

测试：复制连点断言 `posts` 长度 1、copy URL、提交中 copy 与 apply 均 `disabled===true`、完成后 copy 恢复。应用：`window.confirm` 恒 true，两下点击，`posts` 长度 1 且为 apply URL。应用条未再断言「两按钮都禁用」——定稿对该条只要求「都确认只发一笔」。POST 计数只 push 非 GET，列表 GET 不计入。

**011 内容：按定稿最小处置。PASS。**

### 4.2 M038-012（GEN-009 / GEN-006）

定稿：套模板单独 try/catch；失败时账号照常入表、清字段、关弹窗；提示写成「创建员工：X（账号类型），账号已创建；套用权限模板 T 失败：后端消息」并 `setGlobalError`；成功路径与账号 POST 失败路径不变；测试一条打桩 apply 403。

after `createUser`（L108–169）点读：

- 非外包注册申请支路（L113–123）未动。
- 账号 `POST /api/v1/access/users` 仍在外层 try。
- `selectedTemplateId` 有值时内层 try/catch 包 apply；catch 写入 `applyError`，不外抛。
- 随后 `enriched` 与成功路径同一份，`setRows` 追加。
- `applyError` 真：`setGlobalError(applyError)`；`setActionResult(\`创建员工：${created.username}（${formatBusinessStatus(accountType)}），账号已创建；套用权限模板 ${selectedTemplateId} 失败：${applyError}\`)`。文案与定稿逐字同构；账号类型走现有 `formatBusinessStatus`，未改 `statusLabels`。
- else：成功文案原句原样（「创建员工并赋权」/「已应用权限模板」）。
- 清字段仍是 username / password / consentReference / evidenceAttachmentIds，然后 `setCreateOpen(false)`。未额外清 organizationId / selectedTemplateId / accountType。
- 外层 catch（账号 POST 失败）：不加行、不关窗，提示仍是「创建员工并赋权失败」/「创建员工失败」。finally 仍 `setSubmitting(false)`。

`ApiError extends Error`，403 走 `apiErrorFromResponse` → `err.message` 为翻译后的后端中文。测试夹具 message 含汉字，内层 catch 能接到非空 `applyError`。

禁止项：无 `POST /grant-requests`；未改 `requireGrantApprovalIfNeeded`；未改员工注册申请支路；未为绑定/解绑补 ref。

测试：apply 403 + 中文 message；`posts` 仅在 POST 时 push；断言 `/access/users` 一笔、`/permission-templates/tpl-1/apply` 一笔、`access-user-row-u-new` 入表、`access-user-create` 为 null（弹窗关闭）、`setActionResult` 匹配 `创建员工：vendor-a.*账号已创建；套用权限模板 tpl-1 失败：`、`setGlobalError` 末次非空白。列表 GET 不进 `posts`。

观察（不否决）：`setGlobalError` 断言是 `/\S/` 而非夹具原文；B2-r3 实施注要求接到后端消息——实现已 `setGlobalError(applyError)`，该注写明「不否决」。定稿测试句是「提示含账号已创建与失败」。`if (applyError)` 在 message 为空字符串时会走成功文案；GRANT_REQUIRES_APPROVAL 首次正常路径带中文 message，不构成须扩处置的触发。

**012 内容：按定稿最小处置与本席 B2-r3 第一种边界。PASS。**

### 4.3 文件模式、禁止面、最小性

- 四文件 `:100644 → 100644`，无新文件、无删除、无 symlink、无可执行位。
- 禁止面 blob 未变：HighRiskConfirm、GuidedFlowModal、Can、adminClient、statusLabels、后端。
- ponytail-review：无新依赖、无新抽象包、无把保存强制塞进包装。`Lean already. Ship.` `net: -0 lines possible`（可删的越界添加为零；少写的是允许项）。

---

## 5. 新断言能否失败（本席实跑）

一次性 worktree：`git worktree add --detach /tmp/m038-b4-r2-grok-wt origin/main`（HEAD `53dec14170f01a6b8ebb1b7beb34f5aaf10da2fb`）。软链主工作区 `node_modules` 与 `apps/web/node_modules`。11 个本模块测试文件路径原样传给 `npx vitest run`。未装依赖。未 `--force` 删除 worktree。

1. `git apply` 第 1 轮冻结 diff `m038_b3r1.diff` → BASE：`VITEST_EXIT(base)=0`，Test Files 11 passed，Tests **57 passed**。
2. `git apply --include` 仅本轮两个 `*.test.tsx` → RED：`VITEST_EXIT(red)=1`，**3 failed | 57 passed (60)**。失败原因即目标症状，不是语法错误：
   - 012：`Unable to find … access-user-row-u-new`（旧码套模板 403 不入表）
   - 011 复制：`expected [ …(2) ] to have a length of 1 but got 2`
   - 011 应用：`expected [ …(2) ] to have a length of 1 but got 2`
3. `git apply --include` 两个页面 `.tsx` → GREEN：`VITEST_EXIT(green)=0`，Tests **60 passed (60)**；`npx tsc -b --noEmit` `TSC_EXIT(green)=0`。四文件 `git hash-object` 与 after tree blob 全等。
4. 反向 apply 清工作区、删软链、`git worktree remove /tmp/m038-b4-r2-grok-wt`（无 `--force`）。`ls` 路径不存在，`git worktree list` 无此项。

旁证 `/home/james/audit-tmp/m038_b3r2_run.log`（SHA256 `27322bc6e92c9b520eca172259539dc03f0438a5bcc04c9a78c2b2895e8a5bdd`）记载同一红绿数字；本席以本席 worktree 输出为准。

GEN-003：三条新断言在修前失败、修后通过，能失败。

---

## 6. 对抗式自问（grilling，自答）

1. 保存未走 `runExclusive`，copy 进行中能否再点保存？三键都读 `submitting`；React 19 离散 click 在下一事件前刷新。允许项未做 ≠ 越界。
2. `HighRiskConfirm.handleClick` 不读 `disabled`，两下确认是否两笔 apply？`exclusiveRef` 在第一下 `onConfirm` 同步置位；RED 为 2 笔、GREEN 为 1 笔。未改该组件，符合禁止表。
3. `runExclusive` 无 catch，copy/apply 抛错会不会卡住 ref？二者内部已 catch 且不重抛；`finally` 仍复位。
4. 012 用 `if (applyError)` 会不会把空 message 当成成功？首次正常 403 带中文 message；空报文不是本条触发。不升队。
5. 测试 `setGlobalError(/\S/)` 是否测到伪成功？实现写入 `applyError`；定稿锁的是结果提示含「账号已创建」与「失败」，该正则能失败于未调用。不否决。
6. 弹窗关闭测的是 `access-user-create` 为 null 而非 `access-user-flow`？关闭后创建按钮随弹窗卸载；GREEN 该断言通过。与「关弹窗」同一可观察面。
7. 是否顺手改绑定/解绑补 ref（GR-02）？diff 无 `bindBusy` / `bindPartnerOrg` 行。无。
8. 是否把 011 并回 005 或改治理页 `runWrite`？`AccessGovernancePage.tsx` 不在本 diff。无。

无一项把本票打成 FAIL。

---

## 7. 票面

| 轴 | 票 | 依据 |
|---|---|---|
| 归因（闸 3） | **PASS** | 214 行 / ±157 全部归 011 或 012；禁止面未改；无第五路径 |
| 内容 | **PASS** | 011 包装+两入口+disabled+两条连点测试；012 内层 catch+入表清窗提示+`setGlobalError`+一条 403 测试；成功/POST 失败路径未改；RED 三红 GREEN 60 与 tsc 0 |
| 总结论 | **PASS** | 任一轴 FAIL 即 FAIL；两轴均 PASS |

不是模块 PASS。不是 B5。B4 双 PASS 须席甲本轮 self raw 合取。本席未读该 self raw。

无添加项、无 HTML 申请、无 questions、无 LOCK-AMEND、无「有条件通过」。

- 文件自身版本：V0.01（路径原先不存在，本次创建完整报告）

CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# M038 B4-r2 修复 diff 复审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-23T17:32:47Z
- 锁 `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。对象：M038 B3-r2 修复 diff：before tree `97cd4f676f7e8d969642f5424dfaa79854f72f84` → after tree `19ab58e07b9686054d8f4fd2fb0f279b727dc34a`；冻结 diff `/home/james/audit-tmp/m038_b3r2.diff`（13141 bytes，SHA256 27892c36c2ffebe60aa9c81df5b6d1b8158ae6913db1e62645f45fcf5340eecd）。定稿 ID：M038-011、M038-012。
- 顺序：Grok 已先派（modules/M038-b4-r2-grok-raw.md 存在=False），本报告落盘前未读其正文。

## 1. 逐行归因（§5 闸 3）

| ± | 归因 ID | 行首 70 字 |
|---|---|---|
| `+` | 012 | // M038-012：新建外部协作账号时账号已落库、套用权限模板被拒——账号照常入表并关弹窗，提示写清「账号已创建；套用权限模板失败」。 |
| `+` | 012 | describe("M038-012 员工账号页创建后套模板失败", () => { |
| `+` | 012 |   afterEach(() => { |
| `+` | 012 |     vi.unstubAllGlobals(); |
| `+` | 012 |     vi.restoreAllMocks(); |
| `+` | 012 |   }); |
| `+` | 012 |  |
| `+` | 012 |   it("账号 POST 成功、套模板 403：账号只发一笔且入表，弹窗关闭，提示含「账号已创建」与失败原因", async () => |
| `+` | 012 |     const posts: string[] = []; |
| `+` | 012 |     vi.stubGlobal("fetch", vi.fn().mockImplementation((input: Request |
| `+` | 012 |       const url = String(input); |
| `+` | 012 |       if ((init?.method ?? "GET").toUpperCase() === "POST") { |
| `+` | 012 |         posts.push(url); |
| `+` | 012 |         if (url.includes("/apply")) { |
| `+` | 012 |           return Promise.resolve(new Response( |
| `+` | 012 |             JSON.stringify({ success: false, code: "GRANT_REQUIRES_AP |
| `+` | 012 |             { status: 403, headers: { "content-type": "application/js |
| `+` | 012 |           )); |
| `+` | 012 |         } |
| `+` | 012 |         return Promise.resolve(new Response( |
| `+` | 012 |           JSON.stringify({ success: true, code: "OK", message: "succe |
| `+` | 012 |           { status: 200, headers: { "content-type": "application/json |
| `+` | 012 |         )); |
| `+` | 012 |       } |
| `+` | 012 |       if (url.includes("permission-templates")) return Promise.resolv |
| `+` | 012 |       if (url.includes("business-entities")) return Promise.resolve(e |
| `+` | 012 |       if (url.includes("merchants")) return Promise.resolve(envelope( |
| `+` | 012 |       if (url.includes("access/users")) return Promise.resolve(envelo |
| `+` | 012 |       return Promise.resolve(envelope([])); |
| `+` | 012 |     })); |
| `+` | 012 |     const props: AdminPageProps = { |
| `+` | 012 |       api: new AdminApi("test-token"), |
| `+` | 012 |       user: { id: "u1", username: "admin", accountType: "platform_adm |
| `+` | 012 |       setActionResult: vi.fn(), |
| `+` | 012 |       setGlobalError: vi.fn(), |
| `+` | 012 |       effectivePermissions: ADMIN_PERMS, |
| `+` | 012 |     }; |
| `+` | 012 |     render(<AccessUsersPage {...props} />); |
| `+` | 012 |     await waitFor(() => expect(screen.getByTestId("access-user-row-u- |
| `+` | 012 |     fireEvent.click(screen.getByTestId("access-user-create-open")); |
| `+` | 012 |     fireEvent.change(screen.getByTestId("access-user-account-type"),  |
| `+` | 012 |     fireEvent.change(screen.getByTestId("access-user-name"), { target |
| `+` | 012 |     fireEvent.change(screen.getByTestId("access-user-password"), { ta |
| `+` | 012 |     fireEvent.change(screen.getByTestId("access-user-org"), { target: |
| `+` | 012 |     await waitFor(() => expect((screen.getByTestId("access-user-templ |
| `+` | 012 |     fireEvent.click(screen.getByTestId("access-user-flow-next")); |
| `+` | 012 |     fireEvent.click(await waitFor(() => screen.getByTestId("access-us |
| `+` | 012 |     await waitFor(() => expect(screen.getByTestId("access-user-row-u- |
| `+` | 012 |     expect(posts.filter((url) => url.includes("/access/users"))).toHa |
| `+` | 012 |     expect(posts.filter((url) => url.includes("/permission-templates/ |
| `+` | 012 |     await waitFor(() => expect(screen.queryByTestId("access-user-crea |
| `+` | 012 |     expect(props.setActionResult).toHaveBeenCalledWith(expect.stringM |
| `+` | 012 |     expect(props.setGlobalError).toHaveBeenLastCalledWith(expect.stri |
| `+` | 012 |   }); |
| `+` | 012 | }); |
| `+` | 012 |       // M038-012：账号已落库后套模板被拒不算整笔失败——账号照常入表、关弹窗，提示写清「账号已创建；套用权限模板失败」。 |
| `+` | 012 |       let applyError = ""; |
| `-` | 012 |         await api.post(`/api/v1/access/permission-templates/${encodeU |
| `-` | 012 |           targetUserIds: [created.id], |
| `-` | 012 |           mode: "append", |
| `-` | 012 |           operatorNote: "后台手动录入后应用权限模板", |
| `-` | 012 |         }); |
| `+` | 012 |         try { |
| `+` | 012 |           await api.post(`/api/v1/access/permission-templates/${encod |
| `+` | 012 |             targetUserIds: [created.id], |
| `+` | 012 |             mode: "append", |
| `+` | 012 |             operatorNote: "后台手动录入后应用权限模板", |
| `+` | 012 |           }); |
| `+` | 012 |         } catch (applyFailure) { |
| `+` | 012 |           applyError = applyFailure instanceof Error ? applyFailure.m |
| `+` | 012 |         } |
| `-` | 012 |       setActionResult(`${selectedTemplateId ? "创建员工并赋权" : "创建员工"}：${c |
| `+` | 012 |       if (applyError) { |
| `+` | 012 |         setGlobalError(applyError); |
| `+` | 012 |         setActionResult(`创建员工：${created.username}（${formatBusinessSta |
| `+` | 012 |       } else { |
| `+` | 012 |         setActionResult(`${selectedTemplateId ? "创建员工并赋权" : "创建员工"}：$ |
| `+` | 012 |       } |
| `+` | 011 |  |
| `+` | 011 | // M038-011：复制模板、应用模板提交未完成时再次点击不发第二笔，按钮置为禁用。 |
| `+` | 011 | describe("M038-011 权限模板页复制/应用防连点", () => { |
| `+` | 011 |   afterEach(() => { |
| `+` | 011 |     vi.unstubAllGlobals(); |
| `+` | 011 |     vi.restoreAllMocks(); |
| `+` | 011 |   }); |
| `+` | 011 |  |
| `+` | 011 |   function renderPending() { |
| `+` | 011 |     const posts: string[] = []; |
| `+` | 011 |     let release: (value: Response) => void = () => undefined; |
| `+` | 011 |     vi.stubGlobal("fetch", vi.fn().mockImplementation((input: Request |
| `+` | 011 |       const url = String(input); |
| `+` | 011 |       if ((init?.method ?? "GET").toUpperCase() === "POST") { |
| `+` | 011 |         posts.push(url); |
| `+` | 011 |         return new Promise<Response>((resolve) => { release = resolve |
| `+` | 011 |       } |
| `+` | 011 |       if (url.includes("permission-templates")) return Promise.resolv |
| `+` | 011 |       if (url.includes("access/permissions")) return Promise.resolve( |
| `+` | 011 |       if (url.includes("access/users")) return Promise.resolve(envelo |
| `+` | 011 |       return Promise.resolve(envelope([])); |
| `+` | 011 |     })); |
| `+` | 011 |     const props: AdminPageProps = { api: new AdminApi("test-token"),  |
| `+` | 011 |       setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePer |
| `+` | 011 |     render(<PermissionTemplatesPage {...props} />); |
| `+` | 011 |     const ok = () => release(new Response(JSON.stringify({ success: t |
| `+` | 011 |       { status: 200, headers: { "content-type": "application/json" }  |
| `+` | 011 |     return { posts, ok }; |
| `+` | 011 |   } |
| `+` | 011 |  |
| `+` | 011 |   it("连点两次「复制模板」只发一笔，提交中按钮禁用，完成后恢复", async () => { |
| `+` | 011 |     const { posts, ok } = renderPending(); |
| `+` | 011 |     await waitFor(() => expect((screen.getByTestId("permission-templa |
| `+` | 011 |     fireEvent.click(screen.getByTestId("permission-template-copy")); |
| `+` | 011 |     fireEvent.click(screen.getByTestId("permission-template-copy")); |
| `+` | 011 |     await waitFor(() => expect(posts).toHaveLength(1)); |
| `+` | 011 |     expect(posts[0]).toContain("/permission-templates/tpl-1/copy"); |
| `+` | 011 |     await waitFor(() => expect((screen.getByTestId("permission-templa |
| `+` | 011 |     expect((screen.getByTestId("permission-template-apply") as HTMLBu |
| `+` | 011 |     ok(); |
| `+` | 011 |     await waitFor(() => expect((screen.getByTestId("permission-templa |
| `+` | 011 |     expect(posts.filter((url) => url.includes("/copy"))).toHaveLength |
| `+` | 011 |   }); |
| `+` | 011 |  |
| `+` | 011 |   it("连点两次「应用模板」并都确认，只发一笔", async () => { |
| `+` | 011 |     vi.spyOn(window, "confirm").mockReturnValue(true); |
| `+` | 011 |     const { posts, ok } = renderPending(); |
| `+` | 011 |     await waitFor(() => expect((screen.getByTestId("permission-templa |
| `+` | 011 |     fireEvent.click(screen.getByTestId("permission-template-apply")); |
| `+` | 011 |     fireEvent.click(screen.getByTestId("permission-template-apply")); |
| `+` | 011 |     await waitFor(() => expect(posts).toHaveLength(1)); |
| `+` | 011 |     expect(posts[0]).toContain("/permission-templates/tpl-1/apply"); |
| `+` | 011 |     await waitFor(() => expect((screen.getByTestId("permission-templa |
| `+` | 011 |     ok(); |
| `+` | 011 |     await waitFor(() => expect((screen.getByTestId("permission-templa |
| `+` | 011 |     expect(posts.filter((url) => url.includes("/apply"))).toHaveLengt |
| `+` | 011 |   }); |
| `+` | 011 | }); |
| `+` | 011 |   // M038-011：复制、应用与「保存修改」共用提交中态——提交中按钮禁用，连点不发第二笔。 |
| `+` | 011 |   const exclusiveRef = React.useRef(false); |
| `+` | 011 |   async function runExclusive(action: () => Promise<void>) { |
| `+` | 011 |     if (exclusiveRef.current) return; |
| `+` | 011 |     exclusiveRef.current = true; |
| `+` | 011 |     setSubmitting(true); |
| `+` | 011 |     try { |
| `+` | 011 |       await action(); |
| `+` | 011 |     } finally { |
| `+` | 011 |       exclusiveRef.current = false; |
| `+` | 011 |       setSubmitting(false); |
| `+` | 011 |     } |
| `+` | 011 |   } |
| `-` | 011 |             disabled={!selected?.id} |
| `-` | 011 |             onClick={() => void copyTemplate()} |
| `+` | 011 |             disabled={submitting ¦¦ !selected?.id} |
| `+` | 011 |             onClick={() => void runExclusive(copyTemplate)} |
| `-` | 011 |             disabled={!selected?.id} |
| `+` | 011 |             disabled={submitting ¦¦ !selected?.id} |
| `-` | 011 |             onConfirm={() => void applyTemplate()} |
| `+` | 011 |             onConfirm={() => void runExclusive(applyTemplate)} |

- 4 个文件（PermissionTemplatesPage.tsx、PermissionTemplatesPage.test.tsx → 011；AccessUsersPage.tsx、AccessUsersPage.test.tsx → 012）157 条 ± 行：归 011 79 条、归 012 78 条；无 mode 变更；其余文件未动。归因票：**PASS**。

## 2. diff 全文

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

## 3. 内容审计

- `runExclusive` 与治理页 `runWrite` 同构：ref 挡并发、`setSubmitting(true)` 让按钮禁用、finally 复位；不处理错误（`copyTemplate`、`applyTemplate` 各自已有 try/catch 与提示），动作函数体未改。
- 「复制模板」与「应用模板」的 disabled 并入 `submitting`，与同页「保存修改」一致；未改 HighRiskConfirm 组件本身。
- 两条新测试用悬挂的 POST 模拟提交未完成：连点两次只记到一笔 POST、提交中两个按钮禁用、放行后恢复；应用一条先把 `window.confirm` 打桩为确认。RED 时两条都因「记到两笔」而红。
- 012：`createUser` 里套模板改为单独 try/catch——账号 POST 成功后套模板被拒时，账号照常入表、清字段、关弹窗，`setGlobalError(后端消息)`，结果提示写成「创建员工：X（类型），账号已创建；套用权限模板 T 失败：<后端消息>」；成功路径与账号 POST 失败路径未改。新测试打桩 apply 返回 403：账号 POST 只一笔、新行入表、提交按钮消失、提示含「账号已创建」与失败原因；RED 时因新行未入表而红。
- 内容票：**PASS**。

## 4. 可执行证据原文（/home/james/audit-tmp/m038_b3r2_run.log）

```
START=2026-09-23T17:32:13Z HEAD=e456ba96 origin/main=53dec141
before tree=97cd4f676f7e8d969642f5424dfaa79854f72f84
本模块 8 文件相对 origin/main 的差异 sha256（应等于第 1 轮冻结 diff）：30bbbdc50244c0384c56bc9bcdecc326e55bf74d6c6d43250344fd438d34fc4b vs 30bbbdc50244c0384c56bc9bcdecc326e55bf74d6c6d43250344fd438d34fc4b
scratch at 53dec141
第 1 轮冻结 diff 已应用
scratch 与主工作区 16 个文件逐字节相同（无 MISMATCH 行即是）
== BASE（第 1 轮修后状态）
VITEST_EXIT(base)=0
 Test Files  11 passed (11)
      Tests  57 passed (57)
== RED（只落本轮测试补丁）
VITEST_EXIT(red)=1
     × 账号 POST 成功、套模板 403：账号只发一笔且入表，弹窗关闭，提示含「账号已创建」与失败原因 1078ms
     × 连点两次「复制模板」只发一笔，提交中按钮禁用，完成后恢复 1024ms
     × 连点两次「应用模板」并都确认，只发一笔 1021ms
 Test Files  2 failed | 9 passed (11)
      Tests  3 failed | 57 passed (60)
== GREEN（再落页面补丁）
VITEST_EXIT(green)=0
 Test Files  11 passed (11)
      Tests  60 passed (60)
TSC_EXIT(green)=0
scratch dirty after cleanup: 0
scratch removed
== 主工作区落补丁
patched PermissionTemplatesPage.tsx 3 
patched AccessUsersPage.tsx 1 
patched PermissionTemplatesPage.test.tsx 0 m038_r2_test_pt.txt
patched AccessUsersPage.test.tsx 0 m038_r2_test_au.txt
after tree=19ab58e07b9686054d8f4fd2fb0f279b727dc34a
frozen diff: 13141 bytes sha256 27892c36c2ffebe60aa9c81df5b6d1b8158ae6913db1e62645f45fcf5340eecd
 apps/web/src/admin/pages/AccessUsersPage.test.tsx  | 55 ++++++++++++++++++++
 apps/web/src/admin/pages/AccessUsersPage.tsx       | 23 ++++++---
 .../admin/pages/PermissionTemplatesPage.test.tsx   | 58 ++++++++++++++++++++++
 .../src/admin/pages/PermissionTemplatesPage.tsx    | 21 ++++++--
 4 files changed, 147 insertions(+), 10 deletions(-)
主工作区本轮 diff 与一次性 worktree 里验证过的 diff 是否逐字节相同：same
three HTML + lock sha:
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458 
END=2026-09-23T17:32:31Z
```

## 5. 结论

席甲 B4-r2 票：归因 PASS、内容 PASS。落盘前未读 Grok 本次正文；合取以 Grok 票为准。

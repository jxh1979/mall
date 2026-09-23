CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M038 B4-r1 修复 diff 复审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-21T13:34:29Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`。
- 对象：M038 B3-r1 修复 diff：before tree `df21dfdfa3aaa27416857eeca14dedcd04cee631` → after tree `9df1fad44caa2ef615993fe970d3efc4620aca25`；冻结 diff `/home/james/audit-tmp/m038_b3r1.diff`（73515 bytes，SHA256 30bbbdc50244c0384c56bc9bcdecc326e55bf74d6c6d43250344fd438d34fc4b），全文见 §2。定稿 ID：M038-001…010（队列「是」）。修前 16 个文件与 origin/main 逐字节相同，无发布变体。
- 顺序：Grok 已先派（modules/M038-b4-r1-grok-raw.md 存在=False），本报告落盘前未读其正文。

## 1. 逐行归因（§5 闸 3）

归因规则：页面文件按行内的标识符与定稿 ID 的对应关系归；块内不含标识符的行（括号、空行、try/finally 等）沿用同一 hunk 内上一条已归因的 ID；测试文件追加段按最近一条 `it("…（00x）")` 的编号归，其前的共用夹具归该段所含各 ID；一行同时承担两个 ID 的（如把「职级 id」输入框改成 30 档下拉、把恢复按钮既改文案又接入 runWrite）并列两个 ID。各 ID 行数：{'008': 26, '010': 134, '002': 32, '001/003/004/005/007/008/009（本段共用夹具）': 44, '001': 117, '003': 12, '004': 10, '005': 68, '007': 9, '009': 28, '文件版本号（随本文件所含各 ID）': 6, '004/005': 2, '002/008': 8, '001/009（本段共用夹具）': 5, '001/006（本段共用夹具）': 13, '006': 31}。

| 文件 | ± | 归因 ID | 行首 66 字 |
|---|---|---|---|
| AccessGovernancePage.guidedcreate.test.tsx | `-` | 008 |     fireEvent.change(screen.getByTestId("access-position-role-temp |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 008 |     fireEvent.change(screen.getByTestId("access-position-role-temp |
| AccessGovernancePage.guidedcreate.test.tsx | `-` | 008 |     expect((lastPostBody as { staff_level_id: string }).staff_leve |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 008 |     expect((lastPostBody as { staff_level_id: string }).staff_leve |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |  |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 | // M038-010：发起授权申请的引导提交（入口→类型与用户→权限码与过期时间→提交）此前没有测试。 |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 | describe("AccessGovernancePage 引导式发起授权申请（M038-010）", () => { |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |   afterEach(() => vi.unstubAllGlobals()); |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |  |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |   it("入口→步进→提交：POST /grant-requests 带类型、目标用户、权限码、过期时间与单次有效", async |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     let grantPost: { url: string; body: Record<string, unknown> }  |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     vi.stubGlobal("fetch", vi.fn().mockImplementation((input: Requ |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |       const url = String(input); |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |       if ((init?.method ?? "GET").toUpperCase() === "POST" && url. |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |         grantPost = { url, body: JSON.parse(String(init?.body)) }; |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |         return Promise.resolve(raw({ id: "agr-new", status: "pendi |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |       } |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |       if (url.includes("grant-requests/summary")) return Promise.r |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |       if (url.includes("grant-requests")) return Promise.resolve(e |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |       if (url.includes("position-role-templates")) return Promise. |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |       if (url.includes("access/roles")) return Promise.resolve(env |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |       if (url.includes("access/users")) return Promise.resolve(env |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |       return Promise.resolve(envelope([])); |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     })); |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     const setActionResult = vi.fn(); |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     const props: AdminPageProps = { |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |       api: new AdminApi("test-token"), |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |       user: { id: "u1", username: "admin", accountType: "platform_ |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |       setActionResult, |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |       setGlobalError: vi.fn(), |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |       effectivePermissions: ADMIN_PERMS, |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     }; |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     render(<AccessGovernancePage {...props} />); |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     await waitFor(() => expect(screen.getByTestId("access-position |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     fireEvent.click(screen.getByTestId("access-grant-request-creat |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     await waitFor(() => expect((screen.getByLabelText("目标用户") as H |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     fireEvent.click(screen.getByTestId("access-grant-request-guide |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     fireEvent.change(screen.getByLabelText("临时权限码"), { target: { v |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     fireEvent.change(screen.getByLabelText("过期时间"), { target: { va |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     fireEvent.click(screen.getByTestId("access-grant-request-guide |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     await waitFor(() => expect(screen.getByTestId("access-grant-re |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     fireEvent.click(screen.getByTestId("access-grant-request-creat |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |  |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     await waitFor(() => expect(grantPost).toBeTruthy()); |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     const body = grantPost!.body; |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     expect(body.request_type).toBe("temporary_permission"); |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     expect(body.target_user_id).toBe("u-1"); |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     expect(body.permission_codes).toEqual(["finance.refund.create" |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     expect(new Date(String(body.expires_at)).getTime()).toBe(new D |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     expect(body.single_use).toBe(true); |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     await waitFor(() => expect(screen.queryByTestId("access-grant- |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     expect(setActionResult).toHaveBeenCalledWith(expect.stringMatc |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |   }); |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |  |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |   it("步内校验拦截：临时权限未填过期时间则不进确认、不提交", async () => { |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     const fetchMock = vi.fn().mockImplementation((input: RequestIn |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |       const url = String(input); |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |       if (url.includes("grant-requests/summary")) return Promise.r |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |       if (url.includes("position-role-templates")) return Promise. |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |       if (url.includes("access/roles")) return Promise.resolve(env |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |       if (url.includes("access/users")) return Promise.resolve(env |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |       return Promise.resolve(envelope([])); |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     }); |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     vi.stubGlobal("fetch", fetchMock); |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     const props: AdminPageProps = { |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |       api: new AdminApi("test-token"), |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |       user: { id: "u1", username: "admin", accountType: "platform_ |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |       setActionResult: vi.fn(), |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |       setGlobalError: vi.fn(), |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |       effectivePermissions: ADMIN_PERMS, |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     }; |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     render(<AccessGovernancePage {...props} />); |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     await waitFor(() => expect(screen.getByTestId("access-position |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     fireEvent.click(screen.getByTestId("access-grant-request-creat |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     await waitFor(() => expect((screen.getByLabelText("目标用户") as H |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     fireEvent.click(screen.getByTestId("access-grant-request-guide |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     fireEvent.click(screen.getByTestId("access-grant-request-guide |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     expect(screen.getByTestId("access-grant-request-guided-error") |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     expect(screen.queryByTestId("access-grant-request-guided-summa |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |     expect(fetchMock.mock.calls.some(([, init]) => ((init as Reque |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 |   }); |
| AccessGovernancePage.guidedcreate.test.tsx | `+` | 010 | }); |
| AccessGovernancePage.test.tsx | `-` | 002 |     fireEvent.change(screen.getByLabelText("覆盖用户 id"), { target: { |
| AccessGovernancePage.test.tsx | `+` | 002 |     fireEvent.change(screen.getByLabelText("覆盖用户"), { target: { va |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |  |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） | // M038 回归：取值全中文（001）、未登记岗位（003）、恢复文案（004）、写动作失败提示与防双击（005）、表头列数（0 |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） | describe("M038 访问治理页回归", () => { |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |   afterEach(() => vi.unstubAllGlobals()); |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |  |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |   const M038_GRANTS = [ |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |     { id: "gr-a", request_type: "user_scope_change", target_user_i |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |     { id: "gr-b", request_type: "position_high_risk_release", targ |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |     { id: "gr-c", request_type: "role_permission", target_user_id: |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |   ]; |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |   const M038_TEMPLATES = [ |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |     { id: "prt-x", position_code: "front_desk", staff_level_id: "l |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |     { id: "prt-arch", position_code: "ops_manager", staff_level_id |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |     { id: "prt-del", position_code: "ops_manager", staff_level_id: |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |   ]; |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |   type Write = { url: string; body: Record<string, unknown> }; |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |  |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |   function renderM038(onWrite: (write: Write) => Promise<Response> |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |     const writes: Write[] = []; |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |     vi.stubGlobal("fetch", vi.fn().mockImplementation((input: Requ |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |       const url = String(input); |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |       if ((init?.method ?? "GET").toUpperCase() !== "GET") { |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |         const write = { url, body: init?.body ? JSON.parse(String( |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |         writes.push(write); |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |         return onWrite(write); |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |       } |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |       if (url.includes("grant-requests/summary")) return Promise.r |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |       if (url.includes("grant-requests")) return Promise.resolve(e |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |       if (url.includes("position-role-templates")) return Promise. |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |       if (url.includes("access/roles")) return Promise.resolve(env |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |       if (url.includes("access/users")) return Promise.resolve(env |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |       return Promise.resolve(envelope([])); |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |     })); |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |     const props: AdminPageProps = { |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |       api: new AdminApi("test-token"), |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |       user: { id: "u1", username: "admin", accountType: "platform_ |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |       setActionResult: vi.fn(), |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |       setGlobalError: vi.fn(), |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |       effectivePermissions: ADMIN_PERMS, |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |     }; |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |     render(<AccessGovernancePage {...props} />); |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |     return { props, writes }; |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |   } |
| AccessGovernancePage.test.tsx | `+` | 001/003/004/005/007/008/009（本段共用夹具） |  |
| AccessGovernancePage.test.tsx | `+` | 001 |   it("授权申请的三种状态与内部类型全部中文上屏（001）", async () => { |
| AccessGovernancePage.test.tsx | `+` | 001 |     renderM038(() => Promise.resolve(raw({}))); |
| AccessGovernancePage.test.tsx | `+` | 001 |     await waitFor(() => expect(screen.getByTestId("access-grant-re |
| AccessGovernancePage.test.tsx | `+` | 001 |     const text = screen.getByTestId("access-grant-requests-table") |
| AccessGovernancePage.test.tsx | `+` | 001 |     for (const label of ["已生效", "待处理", "已驳回", "人员范围变更", "岗位高危权限释放" |
| AccessGovernancePage.test.tsx | `+` | 001 |     for (const code of ["applied", "user_scope_change", "position_ |
| AccessGovernancePage.test.tsx | `+` | 001 |     fireEvent.click(screen.getByTestId("access-grant-request-gr-a" |
| AccessGovernancePage.test.tsx | `+` | 001 |     const drawer = screen.getByTestId("row-detail-drawer").textCon |
| AccessGovernancePage.test.tsx | `+` | 001 |     expect(drawer).toContain("人员范围变更"); |
| AccessGovernancePage.test.tsx | `+` | 001 |     expect(drawer).toContain("已生效"); |
| AccessGovernancePage.test.tsx | `+` | 001 |     expect(drawer).not.toContain("applied"); |
| AccessGovernancePage.test.tsx | `+` | 001 |   }); |
| AccessGovernancePage.test.tsx | `+` | 001 |  |
| AccessGovernancePage.test.tsx | `+` | 001 |   it("审批不填意见时留痕为中文「通过」，结果提示为中文状态（001）", async () => { |
| AccessGovernancePage.test.tsx | `+` | 001 |     const { props, writes } = renderM038(() => Promise.resolve(raw |
| AccessGovernancePage.test.tsx | `+` | 001 |     await waitFor(() => expect(screen.getByTestId("access-grant-ac |
| AccessGovernancePage.test.tsx | `+` | 001 |     fireEvent.click(screen.getByTestId("access-grant-actions-gr-b" |
| AccessGovernancePage.test.tsx | `+` | 001 |     await waitFor(() => expect(screen.getByTestId("access-grant-ap |
| AccessGovernancePage.test.tsx | `+` | 001 |     fireEvent.click(screen.getByTestId("access-grant-approve-gr-b" |
| AccessGovernancePage.test.tsx | `+` | 001 |     await waitFor(() => expect(writes).toHaveLength(1)); |
| AccessGovernancePage.test.tsx | `+` | 001 |     expect(writes[0]!.url).toContain("/grant-requests/gr-b/review" |
| AccessGovernancePage.test.tsx | `+` | 001 |     expect(writes[0]!.body.comment).toBe("通过"); |
| AccessGovernancePage.test.tsx | `+` | 001 |     expect(writes[0]!.body.operator_note).toBe("通过"); |
| AccessGovernancePage.test.tsx | `+` | 001 |     await waitFor(() => expect(props.setActionResult).toHaveBeenCa |
| AccessGovernancePage.test.tsx | `+` | 001 |   }); |
| AccessGovernancePage.test.tsx | `+` | 001 |  |
| AccessGovernancePage.test.tsx | `+` | 003 |   it("未映射的岗位码显示为「未登记岗位（岗位码）」（003）", async () => { |
| AccessGovernancePage.test.tsx | `+` | 003 |     renderM038(() => Promise.resolve(raw({}))); |
| AccessGovernancePage.test.tsx | `+` | 003 |     await waitFor(() => expect(screen.getByTestId("access-position |
| AccessGovernancePage.test.tsx | `+` | 003 |     const cells = screen.getByTestId("access-position-role-templat |
| AccessGovernancePage.test.tsx | `+` | 003 |     expect(cells[1]?.textContent).toBe("未登记岗位（front_desk）"); |
| AccessGovernancePage.test.tsx | `+` | 003 |     expect(cells[2]?.textContent).toBe("M2·上"); |
| AccessGovernancePage.test.tsx | `+` | 003 |   }); |
| AccessGovernancePage.test.tsx | `+` | 003 |  |
| AccessGovernancePage.test.tsx | `+` | 004 |   it("已存档的按钮写「取消存档」、已删除的写「恢复」，不再写「停用模板」（004）", async () => { |
| AccessGovernancePage.test.tsx | `+` | 004 |     renderM038(() => Promise.resolve(raw({}))); |
| AccessGovernancePage.test.tsx | `+` | 004 |     await waitFor(() => expect(screen.getByTestId("access-position |
| AccessGovernancePage.test.tsx | `+` | 004 |     const buttonTexts = (rowId: string) => Array.from(screen.getBy |
| AccessGovernancePage.test.tsx | `+` | 004 |     expect(buttonTexts("prt-arch")).toContain("取消存档"); |
| AccessGovernancePage.test.tsx | `+` | 004 |     expect(buttonTexts("prt-del")).toContain("恢复"); |
| AccessGovernancePage.test.tsx | `+` | 004 |     expect(buttonTexts("prt-del")).not.toContain("取消存档"); |
| AccessGovernancePage.test.tsx | `+` | 004 |     expect(screen.getByTestId("access-position-role-templates-tabl |
| AccessGovernancePage.test.tsx | `+` | 004 |   }); |
| AccessGovernancePage.test.tsx | `+` | 004 |  |
| AccessGovernancePage.test.tsx | `+` | 005 |   it("装配角色被后端拒绝时页面报出原因（005）", async () => { |
| AccessGovernancePage.test.tsx | `+` | 005 |     const { props, writes } = renderM038(() => Promise.resolve(new |
| AccessGovernancePage.test.tsx | `+` | 005 |       JSON.stringify({ success: false, code: "GRANT_REQUIRES_APPRO |
| AccessGovernancePage.test.tsx | `+` | 005 |       { status: 403, headers: { "content-type": "application/json" |
| AccessGovernancePage.test.tsx | `+` | 005 |     ))); |
| AccessGovernancePage.test.tsx | `+` | 005 |     await waitFor(() => expect((screen.getByLabelText("装配角色") as H |
| AccessGovernancePage.test.tsx | `+` | 005 |     fireEvent.click(screen.getByTestId("access-role-assign")); |
| AccessGovernancePage.test.tsx | `+` | 005 |     await waitFor(() => expect(writes).toHaveLength(1)); |
| AccessGovernancePage.test.tsx | `+` | 005 |     await waitFor(() => expect(props.setGlobalError).toHaveBeenCal |
| AccessGovernancePage.test.tsx | `+` | 005 |     expect(props.setActionResult).not.toHaveBeenCalled(); |
| AccessGovernancePage.test.tsx | `+` | 005 |   }); |
| AccessGovernancePage.test.tsx | `+` | 005 |  |
| AccessGovernancePage.test.tsx | `+` | 005 |   it("提交未完成时再次点击不发第二笔（005）", async () => { |
| AccessGovernancePage.test.tsx | `+` | 005 |     let release: (value: Response) => void = () => undefined; |
| AccessGovernancePage.test.tsx | `+` | 005 |     const { writes } = renderM038(() => new Promise<Response>((res |
| AccessGovernancePage.test.tsx | `+` | 005 |     await waitFor(() => expect((screen.getByLabelText("装配角色") as H |
| AccessGovernancePage.test.tsx | `+` | 005 |     fireEvent.click(screen.getByTestId("access-role-assign")); |
| AccessGovernancePage.test.tsx | `+` | 005 |     fireEvent.click(screen.getByTestId("access-role-assign")); |
| AccessGovernancePage.test.tsx | `+` | 005 |     await waitFor(() => expect(writes).toHaveLength(1)); |
| AccessGovernancePage.test.tsx | `+` | 005 |     await waitFor(() => expect((screen.getByTestId("access-role-as |
| AccessGovernancePage.test.tsx | `+` | 005 |     release(raw({})); |
| AccessGovernancePage.test.tsx | `+` | 005 |     await waitFor(() => expect((screen.getByTestId("access-role-as |
| AccessGovernancePage.test.tsx | `+` | 005 |     expect(writes.filter((write) => write.url.includes("/role-assi |
| AccessGovernancePage.test.tsx | `+` | 005 |   }); |
| AccessGovernancePage.test.tsx | `+` | 005 |  |
| AccessGovernancePage.test.tsx | `+` | 007 |   it("智能总结表表头列数等于数据行单元格数（007）", async () => { |
| AccessGovernancePage.test.tsx | `+` | 007 |     renderM038(() => Promise.resolve(raw({}))); |
| AccessGovernancePage.test.tsx | `+` | 007 |     await waitFor(() => expect(screen.getByTestId("access-grant-su |
| AccessGovernancePage.test.tsx | `+` | 007 |     const table = screen.getByTestId("access-grant-summary-table") |
| AccessGovernancePage.test.tsx | `+` | 007 |     expect(table.querySelectorAll("thead th")).toHaveLength(table. |
| AccessGovernancePage.test.tsx | `+` | 007 |   }); |
| AccessGovernancePage.test.tsx | `+` | 007 |  |
| AccessGovernancePage.test.tsx | `+` | 008 |   it("职级是 30 档下拉，范围变更处另有「沿用原职级」（008）", async () => { |
| AccessGovernancePage.test.tsx | `+` | 008 |     renderM038(() => Promise.resolve(raw({}))); |
| AccessGovernancePage.test.tsx | `+` | 008 |     await waitFor(() => expect(screen.getByTestId("access-user-pos |
| AccessGovernancePage.test.tsx | `+` | 008 |     const options = Array.from((screen.getByTestId("access-user-po |
| AccessGovernancePage.test.tsx | `+` | 008 |     expect(options).toHaveLength(30); |
| AccessGovernancePage.test.tsx | `+` | 008 |     expect(options[0]!.value).toBe("lvl_m1_lower"); |
| AccessGovernancePage.test.tsx | `+` | 008 |     expect(options[0]!.textContent).toBe("M1·下"); |
| AccessGovernancePage.test.tsx | `+` | 008 |     expect(options[29]!.value).toBe("lvl_m10_upper"); |
| AccessGovernancePage.test.tsx | `+` | 008 |     expect(options[29]!.textContent).toBe("M10·上"); |
| AccessGovernancePage.test.tsx | `+` | 008 |     const scopeOptions = Array.from((screen.getByTestId("access-us |
| AccessGovernancePage.test.tsx | `+` | 008 |     expect(scopeOptions).toHaveLength(31); |
| AccessGovernancePage.test.tsx | `+` | 008 |     expect(scopeOptions[0]!.textContent).toBe("沿用原职级"); |
| AccessGovernancePage.test.tsx | `+` | 008 |   }); |
| AccessGovernancePage.test.tsx | `+` | 008 |  |
| AccessGovernancePage.test.tsx | `+` | 009 |   it("岗位模板弹窗里的状态用词与表格同一套（009）", async () => { |
| AccessGovernancePage.test.tsx | `+` | 009 |     renderM038(() => Promise.resolve(raw({}))); |
| AccessGovernancePage.test.tsx | `+` | 009 |     await waitFor(() => expect(screen.getByTestId("access-position |
| AccessGovernancePage.test.tsx | `+` | 009 |     await waitFor(() => expect((screen.getByLabelText("装配角色") as H |
| AccessGovernancePage.test.tsx | `+` | 009 |     fireEvent.click(screen.getByTestId("access-position-role-templ |
| AccessGovernancePage.test.tsx | `+` | 009 |     const status = await waitFor(() => screen.getByTestId("access- |
| AccessGovernancePage.test.tsx | `+` | 009 |     expect(Array.from(status.options).map((option) => option.textC |
| AccessGovernancePage.test.tsx | `+` | 009 |   }); |
| AccessGovernancePage.test.tsx | `+` | 009 | }); |
| AccessGovernancePage.tsx | `-` | 文件版本号（随本文件所含各 ID） | // 文件版本：V0.03 |
| AccessGovernancePage.tsx | `+` | 文件版本号（随本文件所含各 ID） | // 文件版本：V0.04 |
| AccessGovernancePage.tsx | `-` | 003 |   return POSITION_LABELS[code] ?? code; |
| AccessGovernancePage.tsx | `+` | 003 |   // M038-003：未映射的岗位码不裸露英文——中文说明在前，岗位码作为系统内部标识符放括号里。 |
| AccessGovernancePage.tsx | `+` | 003 |   return POSITION_LABELS[code] ?? `未登记岗位（${code}）`; |
| AccessGovernancePage.tsx | `+` | 003 | } |
| AccessGovernancePage.tsx | `+` | 008 | // M038-008：职级是速查表写明的 30 档闭集（M1–M10 × 上/中/下），编号与库内 staff_levels 一致 |
| AccessGovernancePage.tsx | `+` | 008 | const STAFF_LEVEL_IDS: string[] = []; |
| AccessGovernancePage.tsx | `+` | 008 | for (let main = 1; main <= 10; main += 1) { |
| AccessGovernancePage.tsx | `+` | 008 |   for (const band of ["lower", "middle", "upper"]) STAFF_LEVEL_IDS |
| AccessGovernancePage.tsx | `+` | 008 | } |
| AccessGovernancePage.tsx | `+` | 008 | function staffLevelOptions() { |
| AccessGovernancePage.tsx | `+` | 008 |   return STAFF_LEVEL_IDS.map((id) => <option key={id} value={id}>{ |
| AccessGovernancePage.tsx | `+` | 008 | } |
| AccessGovernancePage.tsx | `+` | 001 | // M038-001：授权申请的状态与类型按后端闭集在页内给全中文（公共字典缺「已生效」与三种内部类型）。 |
| AccessGovernancePage.tsx | `+` | 001 | const GRANT_STATUS_LABELS: Record<string, string> = { pending: "待处 |
| AccessGovernancePage.tsx | `+` | 001 | const GRANT_TYPE_LABELS: Record<string, string> = { |
| AccessGovernancePage.tsx | `+` | 001 |   user_role: "用户角色", user_position: "用户职位", temporary_permission:  |
| AccessGovernancePage.tsx | `+` | 001 |   role_permission: "角色权限", user_scope_change: "人员范围变更", position_h |
| AccessGovernancePage.tsx | `+` | 001 | }; |
| AccessGovernancePage.tsx | `+` | 001 | function grantStatusLabel(code?: string ¦ null): string { |
| AccessGovernancePage.tsx | `+` | 001 |   return code ? GRANT_STATUS_LABELS[code] ?? formatBusinessStatus( |
| AccessGovernancePage.tsx | `+` | 001 | } |
| AccessGovernancePage.tsx | `+` | 001 | function grantTypeLabel(code?: string ¦ null): string { |
| AccessGovernancePage.tsx | `+` | 001 |   return code ? GRANT_TYPE_LABELS[code] ?? formatBusinessStatus(co |
| AccessGovernancePage.tsx | `-` | 001 | function statusBadge(status?: string ¦ null) { |
| AccessGovernancePage.tsx | `+` | 001 | function statusBadge(status?: string ¦ null, label: (code: string) |
| AccessGovernancePage.tsx | `-` | 001 |   return <span className={`admin-badge ${kind}`}>{value ? formatBu |
| AccessGovernancePage.tsx | `+` | 001 |   return <span className={`admin-badge ${kind}`}>{value ? label(va |
| AccessGovernancePage.tsx | `+` | 005 |   // M038-005：页内写动作统一走 runWrite——提交中置忙（防双击发两笔）、失败把后端中文消息报到页面。 |
| AccessGovernancePage.tsx | `+` | 005 |   const [writeBusy, setWriteBusy] = React.useState(false); |
| AccessGovernancePage.tsx | `+` | 005 |   const writeBusyRef = React.useRef(false); |
| AccessGovernancePage.tsx | `+` | 005 |   async function runWrite(action: () => Promise<void>, fallbackMes |
| AccessGovernancePage.tsx | `+` | 005 |     if (writeBusyRef.current) return; |
| AccessGovernancePage.tsx | `+` | 005 |     writeBusyRef.current = true; |
| AccessGovernancePage.tsx | `+` | 005 |     setWriteBusy(true); |
| AccessGovernancePage.tsx | `+` | 005 |     setGlobalError(null); |
| AccessGovernancePage.tsx | `+` | 005 |     try { |
| AccessGovernancePage.tsx | `+` | 005 |       await action(); |
| AccessGovernancePage.tsx | `+` | 005 |     } catch (actionError) { |
| AccessGovernancePage.tsx | `+` | 005 |       setGlobalError(actionError instanceof Error ? actionError.me |
| AccessGovernancePage.tsx | `+` | 005 |     } finally { |
| AccessGovernancePage.tsx | `+` | 005 |       writeBusyRef.current = false; |
| AccessGovernancePage.tsx | `+` | 005 |       setWriteBusy(false); |
| AccessGovernancePage.tsx | `+` | 005 |     } |
| AccessGovernancePage.tsx | `+` | 005 |   } |
| AccessGovernancePage.tsx | `-` | 001 |       setActionResult(`授权申请已提交：${result.id}（${formatBusinessStatus |
| AccessGovernancePage.tsx | `+` | 001 |       setActionResult(`授权申请已提交：${result.id}（${grantStatusLabel(res |
| AccessGovernancePage.tsx | `-` | 001 |       comment: reviewForm.comment.trim() ¦¦ decision, |
| AccessGovernancePage.tsx | `-` | 001 |       operatorNote: reviewForm.comment.trim() ¦¦ decision, |
| AccessGovernancePage.tsx | `+` | 001 |       // M038-001：不填意见时留痕用中文，不把 approve／reject 当意见。 |
| AccessGovernancePage.tsx | `+` | 001 |       comment: reviewForm.comment.trim() ¦¦ (decision === "approve |
| AccessGovernancePage.tsx | `+` | 001 |       operatorNote: reviewForm.comment.trim() ¦¦ (decision === "ap |
| AccessGovernancePage.tsx | `-` | 001 |     setActionResult(`授权申请 ${row.id} 已${decision === "approve" ? "通 |
| AccessGovernancePage.tsx | `+` | 001 |     setActionResult(`授权申请 ${row.id} 已${decision === "approve" ? "通 |
| AccessGovernancePage.tsx | `-` | 001 |         { label: "类型", value: formatBusinessStatus(row.requestType |
| AccessGovernancePage.tsx | `+` | 001 |         { label: "类型", value: grantTypeLabel(row.requestType) }, |
| AccessGovernancePage.tsx | `-` | 001 |         { label: "状态", value: formatBusinessStatus(row.status) }, |
| AccessGovernancePage.tsx | `+` | 001 |         { label: "状态", value: grantStatusLabel(row.status) }, |
| AccessGovernancePage.tsx | `-` | 005 |                   {!row.archivedAt ? <PermissionButton permissions |
| AccessGovernancePage.tsx | `-` | 005 |                     {!row.deletedAt && !row.deleteReviewId ? <Perm |
| AccessGovernancePage.tsx | `+` | 005 |                   {!row.archivedAt ? <PermissionButton permissions |
| AccessGovernancePage.tsx | `+` | 005 |                     {!row.deletedAt && !row.deleteReviewId ? <Perm |
| AccessGovernancePage.tsx | `-` | 005 |                       <PermissionButton permissionsState={effectiv |
| AccessGovernancePage.tsx | `-` | 005 |                       <PermissionButton permissionsState={effectiv |
| AccessGovernancePage.tsx | `-` | 004/005 |                     </> : <PermissionButton permissionsState={effe |
| AccessGovernancePage.tsx | `+` | 005 |                       <PermissionButton permissionsState={effectiv |
| AccessGovernancePage.tsx | `+` | 005 |                       <PermissionButton permissionsState={effectiv |
| AccessGovernancePage.tsx | `+` | 004/005 |                     </> : <PermissionButton permissionsState={effe |
| AccessGovernancePage.tsx | `-` | 002 |           <select aria-label="用户 id" value={positionForm.userId} o |
| AccessGovernancePage.tsx | `+` | 002 |           <select aria-label="用户" value={positionForm.userId} onCh |
| AccessGovernancePage.tsx | `-` | 002/008 |           <input aria-label="职级 id" placeholder="职级 id" value={pos |
| AccessGovernancePage.tsx | `+` | 002/008 |           <select aria-label="职级" data-testid="access-user-positio |
| AccessGovernancePage.tsx | `-` | 005 |           <PermissionButton permissionsState={effectivePermissions |
| AccessGovernancePage.tsx | `+` | 005 |           <PermissionButton permissionsState={effectivePermissions |
| AccessGovernancePage.tsx | `-` | 002 |           <select aria-label="变更用户 id" data-testid="access-user-sc |
| AccessGovernancePage.tsx | `-` | 002 |           <input aria-label="新所属项目 id" data-testid="access-user-sc |
| AccessGovernancePage.tsx | `-` | 002 |           <input aria-label="新所属商户 id" data-testid="access-user-sc |
| AccessGovernancePage.tsx | `+` | 002 |           <select aria-label="变更用户" data-testid="access-user-scope |
| AccessGovernancePage.tsx | `+` | 002 |           <input aria-label="新所属项目编号" data-testid="access-user-sco |
| AccessGovernancePage.tsx | `+` | 002 |           <input aria-label="新所属商户编号" data-testid="access-user-sco |
| AccessGovernancePage.tsx | `-` | 002/008 |           <input aria-label="新范围职级 id" data-testid="access-user-sc |
| AccessGovernancePage.tsx | `+` | 002/008 |           <select aria-label="新范围职级" data-testid="access-user-scop |
| AccessGovernancePage.tsx | `-` | 005 |           <PermissionButton permissionsState={effectivePermissions |
| AccessGovernancePage.tsx | `+` | 005 |           <PermissionButton permissionsState={effectivePermissions |
| AccessGovernancePage.tsx | `-` | 002 |           <select aria-label="装配用户 id" value={roleForm.userId} onC |
| AccessGovernancePage.tsx | `-` | 002 |           <select aria-label="装配角色 id" value={roleForm.roleId} onC |
| AccessGovernancePage.tsx | `+` | 002 |           <select aria-label="装配用户" value={roleForm.userId} onChan |
| AccessGovernancePage.tsx | `+` | 002 |           <select aria-label="装配角色" value={roleForm.roleId} onChan |
| AccessGovernancePage.tsx | `-` | 005 |           <PermissionButton permissionsState={effectivePermissions |
| AccessGovernancePage.tsx | `-` | 005 |           <PermissionButton permissionsState={effectivePermissions |
| AccessGovernancePage.tsx | `+` | 005 |           <PermissionButton permissionsState={effectivePermissions |
| AccessGovernancePage.tsx | `+` | 005 |           <PermissionButton permissionsState={effectivePermissions |
| AccessGovernancePage.tsx | `-` | 002 |           <select aria-label="覆盖用户 id" value={overrideForm.userId} |
| AccessGovernancePage.tsx | `+` | 002 |           <select aria-label="覆盖用户" value={overrideForm.userId} on |
| AccessGovernancePage.tsx | `-` | 005 |           <PermissionButton permissionsState={effectivePermissions |
| AccessGovernancePage.tsx | `-` | 005 |           <PermissionButton permissionsState={effectivePermissions |
| AccessGovernancePage.tsx | `+` | 005 |           <PermissionButton permissionsState={effectivePermissions |
| AccessGovernancePage.tsx | `+` | 005 |           <PermissionButton permissionsState={effectivePermissions |
| AccessGovernancePage.tsx | `-` | 001 |                 <td>{row.id}</td><td>{formatBusinessStatus(row.req |
| AccessGovernancePage.tsx | `+` | 001 |                 <td>{row.id}</td><td>{grantTypeLabel(row.requestTy |
| AccessGovernancePage.tsx | `-` | 005 |                     acts.push({ key: "approve", label: "通过", permi |
| AccessGovernancePage.tsx | `-` | 005 |                     acts.push({ key: "reject", label: "驳回", permis |
| AccessGovernancePage.tsx | `+` | 005 |                     acts.push({ key: "approve", label: "通过", permi |
| AccessGovernancePage.tsx | `+` | 005 |                     acts.push({ key: "reject", label: "驳回", permis |
| AccessGovernancePage.tsx | `-` | 005 |             <PermissionButton permissionsState={effectivePermissio |
| AccessGovernancePage.tsx | `+` | 005 |             <PermissionButton permissionsState={effectivePermissio |
| AccessGovernancePage.tsx | `-` | 007 |             <thead><tr><th>类型</th><th>对象</th><th>权限</th><th>次数</th |
| AccessGovernancePage.tsx | `+` | 007 |             <thead><tr><th>类型</th><th>对象</th><th>权限</th><th>次数</th |
| AccessGovernancePage.tsx | `-` | 002/008 |                 <label>职级 id<input data-testid="access-position-ro |
| AccessGovernancePage.tsx | `-` | 002 |                 <label>角色<select data-testid="access-position-role |
| AccessGovernancePage.tsx | `-` | 002 |                 <label>权限模板 id<input data-testid="access-position- |
| AccessGovernancePage.tsx | `+` | 002/008 |                 <label>职级<select data-testid="access-position-role |
| AccessGovernancePage.tsx | `+` | 002 |                 <label>角色<select data-testid="access-position-role |
| AccessGovernancePage.tsx | `+` | 002 |                 <label>权限模板编号<input data-testid="access-position-r |
| AccessGovernancePage.tsx | `-` | 009 |               <label>模板状态<select data-testid="access-position-role |
| AccessGovernancePage.tsx | `+` | 009 |               <label>模板状态<select data-testid="access-position-role |
| AccessGovernancePage.tsx | `-` | 009 |           { label: "状态", value: templateForm.status === "active" ? |
| AccessGovernancePage.tsx | `+` | 009 |           { label: "状态", value: formatBusinessStatus(templateForm. |
| AccessGovernancePage.tsx | `-` | 002/008 |                 <label>申请职级<input aria-label="申请职级" placeholder="职 |
| AccessGovernancePage.tsx | `+` | 002/008 |                 <label>申请职级<select aria-label="申请职级" value={grantF |
| AccessGovernancePage.tsx | `-` | 001 |           { label: "申请类型", value: formatBusinessStatus(grantForm.r |
| AccessGovernancePage.tsx | `+` | 001 |           { label: "申请类型", value: grantTypeLabel(grantForm.request |
| AccessUsersPage.test.tsx | `-` | 文件版本号（随本文件所含各 ID） | // 文件版本：V0.01 |
| AccessUsersPage.test.tsx | `+` | 文件版本号（随本文件所含各 ID） | // 文件版本：V0.02 |
| AccessUsersPage.test.tsx | `+` | 001/009（本段共用夹具） |  |
| AccessUsersPage.test.tsx | `+` | 001/009（本段共用夹具） | // M038-001（开通方式中文）、M038-009（初始状态下拉与表格同一套用词）。 |
| AccessUsersPage.test.tsx | `+` | 001/009（本段共用夹具） | describe("M038 员工账号页回归", () => { |
| AccessUsersPage.test.tsx | `+` | 001/009（本段共用夹具） |   afterEach(() => vi.unstubAllGlobals()); |
| AccessUsersPage.test.tsx | `+` | 001/009（本段共用夹具） |  |
| AccessUsersPage.test.tsx | `+` | 001 |   it("账号详情的开通方式为中文：历史账号 / 后台手动录入（001）", async () => { |
| AccessUsersPage.test.tsx | `+` | 001 |     vi.stubGlobal("fetch", vi.fn().mockImplementation((input: Requ |
| AccessUsersPage.test.tsx | `+` | 001 |       const url = String(input); |
| AccessUsersPage.test.tsx | `+` | 001 |       if (url.includes("access/users")) return Promise.resolve(env |
| AccessUsersPage.test.tsx | `+` | 001 |         { id: "u-legacy", username: "老账号", account_type: "staff",  |
| AccessUsersPage.test.tsx | `+` | 001 |         { id: "u-manual", username: "外协甲", account_type: "outsourc |
| AccessUsersPage.test.tsx | `+` | 001 |       ])); |
| AccessUsersPage.test.tsx | `+` | 001 |       return Promise.resolve(envelope([])); |
| AccessUsersPage.test.tsx | `+` | 001 |     })); |
| AccessUsersPage.test.tsx | `+` | 001 |     const props: AdminPageProps = { |
| AccessUsersPage.test.tsx | `+` | 001 |       api: new AdminApi("test-token"), |
| AccessUsersPage.test.tsx | `+` | 001 |       user: { id: "u1", username: "admin", accountType: "platform_ |
| AccessUsersPage.test.tsx | `+` | 001 |       setActionResult: vi.fn(), |
| AccessUsersPage.test.tsx | `+` | 001 |       setGlobalError: vi.fn(), |
| AccessUsersPage.test.tsx | `+` | 001 |       effectivePermissions: ADMIN_PERMS, |
| AccessUsersPage.test.tsx | `+` | 001 |     }; |
| AccessUsersPage.test.tsx | `+` | 001 |     render(<AccessUsersPage {...props} />); |
| AccessUsersPage.test.tsx | `+` | 001 |     await waitFor(() => expect(screen.getByTestId("access-user-row |
| AccessUsersPage.test.tsx | `+` | 001 |     fireEvent.click(screen.getByTestId("access-user-row-u-legacy") |
| AccessUsersPage.test.tsx | `+` | 001 |     expect(screen.getByTestId("row-detail-drawer").textContent).to |
| AccessUsersPage.test.tsx | `+` | 001 |     expect(screen.getByTestId("row-detail-drawer").textContent).no |
| AccessUsersPage.test.tsx | `+` | 001 |     fireEvent.keyDown(window, { key: "Escape" }); |
| AccessUsersPage.test.tsx | `+` | 001 |     await waitFor(() => expect(screen.queryByTestId("row-detail-dr |
| AccessUsersPage.test.tsx | `+` | 001 |     fireEvent.click(screen.getByTestId("access-user-row-u-manual") |
| AccessUsersPage.test.tsx | `+` | 001 |     expect(screen.getByTestId("row-detail-drawer").textContent).to |
| AccessUsersPage.test.tsx | `+` | 001 |     expect(screen.getByTestId("row-detail-drawer").textContent).no |
| AccessUsersPage.test.tsx | `+` | 001 |   }); |
| AccessUsersPage.test.tsx | `+` | 001 |  |
| AccessUsersPage.test.tsx | `+` | 009 |   it("新建外部协作账号的初始状态下拉与账号表同一套用词（009）", async () => { |
| AccessUsersPage.test.tsx | `+` | 009 |     renderPage(); |
| AccessUsersPage.test.tsx | `+` | 009 |     await waitFor(() => expect(screen.getByTestId("access-user-cre |
| AccessUsersPage.test.tsx | `+` | 009 |     fireEvent.click(screen.getByTestId("access-user-create-open")) |
| AccessUsersPage.test.tsx | `+` | 009 |     fireEvent.change(screen.getByTestId("access-user-account-type" |
| AccessUsersPage.test.tsx | `+` | 009 |     fireEvent.change(screen.getByTestId("access-user-name"), { tar |
| AccessUsersPage.test.tsx | `+` | 009 |     fireEvent.change(screen.getByTestId("access-user-password"), { |
| AccessUsersPage.test.tsx | `+` | 009 |     const status = await waitFor(() => screen.getByTestId("access- |
| AccessUsersPage.test.tsx | `+` | 009 |     expect(Array.from(status.options).map((option) => option.textC |
| AccessUsersPage.test.tsx | `+` | 009 |   }); |
| AccessUsersPage.test.tsx | `+` | 009 | }); |
| AccessUsersPage.tsx | `-` | 文件版本号（随本文件所含各 ID） | // 文件版本：V0.02 |
| AccessUsersPage.tsx | `+` | 文件版本号（随本文件所含各 ID） | // 文件版本：V0.03 |
| AccessUsersPage.tsx | `+` | 001 | // M038-001：开通方式按后端现有取值在页内给中文（公共字典无这两个键）。 |
| AccessUsersPage.tsx | `+` | 001 | const CREATED_VIA_LABELS: Record<string, string> = { manual_admin: |
| AccessUsersPage.tsx | `+` | 001 | function createdViaLabel(code: string): string { |
| AccessUsersPage.tsx | `+` | 001 |   return CREATED_VIA_LABELS[code] ?? formatBusinessStatus(code); |
| AccessUsersPage.tsx | `+` | 001 | } |
| AccessUsersPage.tsx | `+` | 001 |  |
| AccessUsersPage.tsx | `-` | 002 |       setFormError("组织 ID 不能为空，用于数据范围和留痕。 "); |
| AccessUsersPage.tsx | `+` | 002 |       setFormError("所属组织不能为空，用于数据范围和留痕。 "); |
| AccessUsersPage.tsx | `-` | 001 |         { label: "开通方式", value: row.createdVia ? formatBusinessSta |
| AccessUsersPage.tsx | `+` | 001 |         { label: "开通方式", value: row.createdVia ? createdViaLabel(r |
| AccessUsersPage.tsx | `-` | 002 |             validate: () => (organizationId.trim() ? null : "组织 ID |
| AccessUsersPage.tsx | `+` | 002 |             validate: () => (organizationId.trim() ? null : "所属组织不 |
| AccessUsersPage.tsx | `-` | 002 |                 <label>所属组织<input data-testid="access-user-org" va |
| AccessUsersPage.tsx | `+` | 002 |                 <label>所属组织<input data-testid="access-user-org" va |
| AccessUsersPage.tsx | `-` | 002 |                 <label>证据附件<input data-testid="access-user-evidenc |
| AccessUsersPage.tsx | `+` | 002 |                 <label>证据附件<input data-testid="access-user-evidenc |
| AccessUsersPage.tsx | `-` | 009 |                   <option value="active">启用</option> |
| AccessUsersPage.tsx | `-` | 009 |                   <option value="invited">待激活</option> |
| AccessUsersPage.tsx | `-` | 009 |                   <option value="disabled">禁用</option> |
| AccessUsersPage.tsx | `+` | 009 |                   {["active", "invited", "disabled"].map((value) = |
| PermissionTemplatesPage.test.tsx | `+` | 001/006（本段共用夹具） |  |
| PermissionTemplatesPage.test.tsx | `+` | 001/006（本段共用夹具） | // M038-001（模板类型 role 中文）、M038-006（编辑保存的时间往返）。 |
| PermissionTemplatesPage.test.tsx | `+` | 001/006（本段共用夹具） | describe("M038 权限模板页回归", () => { |
| PermissionTemplatesPage.test.tsx | `+` | 001/006（本段共用夹具） |   afterEach(() => { |
| PermissionTemplatesPage.test.tsx | `+` | 001/006（本段共用夹具） |     vi.unstubAllGlobals(); |
| PermissionTemplatesPage.test.tsx | `+` | 001/006（本段共用夹具） |     vi.unstubAllEnvs(); |
| PermissionTemplatesPage.test.tsx | `+` | 001/006（本段共用夹具） |   }); |
| PermissionTemplatesPage.test.tsx | `+` | 001/006（本段共用夹具） |  |
| PermissionTemplatesPage.test.tsx | `+` | 001/006（本段共用夹具） |   const TIMED = [ |
| PermissionTemplatesPage.test.tsx | `+` | 001/006（本段共用夹具） |     { id: "tpl-role", template_name: "平台管理员模板", template_type: "ro |
| PermissionTemplatesPage.test.tsx | `+` | 001/006（本段共用夹具） |       valid_from: "2026-06-14T00:00:00Z", valid_until: "2026-06-15 |
| PermissionTemplatesPage.test.tsx | `+` | 001/006（本段共用夹具） |   ]; |
| PermissionTemplatesPage.test.tsx | `+` | 001/006（本段共用夹具） |  |
| PermissionTemplatesPage.test.tsx | `+` | 001 |   it("库内类型 role 显示为「角色模板」，编辑下拉也有这一项（001）", async () => { |
| PermissionTemplatesPage.test.tsx | `+` | 001 |     vi.stubGlobal("fetch", vi.fn().mockImplementation((input: Requ |
| PermissionTemplatesPage.test.tsx | `+` | 001 |       const url = String(input); |
| PermissionTemplatesPage.test.tsx | `+` | 001 |       if (url.includes("permission-templates")) return Promise.res |
| PermissionTemplatesPage.test.tsx | `+` | 001 |       if (url.includes("access/permissions")) return Promise.resol |
| PermissionTemplatesPage.test.tsx | `+` | 001 |       if (url.includes("access/users")) return Promise.resolve(env |
| PermissionTemplatesPage.test.tsx | `+` | 001 |       return Promise.resolve(envelope([])); |
| PermissionTemplatesPage.test.tsx | `+` | 001 |     })); |
| PermissionTemplatesPage.test.tsx | `+` | 001 |     const props: AdminPageProps = { api: new AdminApi("test-token" |
| PermissionTemplatesPage.test.tsx | `+` | 001 |       setActionResult: vi.fn(), setGlobalError: vi.fn(), effective |
| PermissionTemplatesPage.test.tsx | `+` | 001 |     render(<PermissionTemplatesPage {...props} />); |
| PermissionTemplatesPage.test.tsx | `+` | 001 |     await waitFor(() => expect(screen.getByTestId("permission-temp |
| PermissionTemplatesPage.test.tsx | `+` | 001 |     const cells = screen.getByTestId("permission-template-row-tpl- |
| PermissionTemplatesPage.test.tsx | `+` | 001 |     expect(cells[2]?.textContent).toBe("角色模板"); |
| PermissionTemplatesPage.test.tsx | `+` | 001 |     const editType = screen.getByTestId("permission-template-edit- |
| PermissionTemplatesPage.test.tsx | `+` | 001 |     await waitFor(() => expect(editType.value).toBe("role")); |
| PermissionTemplatesPage.test.tsx | `+` | 001 |     expect(Array.from(editType.options).map((option) => option.tex |
| PermissionTemplatesPage.test.tsx | `+` | 001 |   }); |
| PermissionTemplatesPage.test.tsx | `+` | 001 |  |
| PermissionTemplatesPage.test.tsx | `+` | 006 |   it("北京时间下选中带生效/截止时间的模板原样保存，发出的仍是同一时刻（006）", async () => { |
| PermissionTemplatesPage.test.tsx | `+` | 006 |     vi.stubEnv("TZ", "Asia/Shanghai"); |
| PermissionTemplatesPage.test.tsx | `+` | 006 |     expect(new Date("2026-06-14T00:00:00Z").getHours()).toBe(8); / |
| PermissionTemplatesPage.test.tsx | `+` | 006 |     let putBody: Record<string, unknown> ¦ null = null; |
| PermissionTemplatesPage.test.tsx | `+` | 006 |     vi.stubGlobal("fetch", vi.fn().mockImplementation((input: Requ |
| PermissionTemplatesPage.test.tsx | `+` | 006 |       const url = String(input); |
| PermissionTemplatesPage.test.tsx | `+` | 006 |       if ((init?.method ?? "GET").toUpperCase() === "PUT" && url.i |
| PermissionTemplatesPage.test.tsx | `+` | 006 |         putBody = JSON.parse(String(init?.body)); |
| PermissionTemplatesPage.test.tsx | `+` | 006 |         return Promise.resolve(new Response(JSON.stringify({ succe |
| PermissionTemplatesPage.test.tsx | `+` | 006 |           { status: 200, headers: { "content-type": "application/j |
| PermissionTemplatesPage.test.tsx | `+` | 006 |       } |
| PermissionTemplatesPage.test.tsx | `+` | 006 |       if (url.includes("permission-templates")) return Promise.res |
| PermissionTemplatesPage.test.tsx | `+` | 006 |       if (url.includes("access/permissions")) return Promise.resol |
| PermissionTemplatesPage.test.tsx | `+` | 006 |       if (url.includes("access/users")) return Promise.resolve(env |
| PermissionTemplatesPage.test.tsx | `+` | 006 |       return Promise.resolve(envelope([])); |
| PermissionTemplatesPage.test.tsx | `+` | 006 |     })); |
| PermissionTemplatesPage.test.tsx | `+` | 006 |     const props: AdminPageProps = { api: new AdminApi("test-token" |
| PermissionTemplatesPage.test.tsx | `+` | 006 |       setActionResult: vi.fn(), setGlobalError: vi.fn(), effective |
| PermissionTemplatesPage.test.tsx | `+` | 006 |     render(<PermissionTemplatesPage {...props} />); |
| PermissionTemplatesPage.test.tsx | `+` | 006 |     await waitFor(() => expect((screen.getByTestId("permission-tem |
| PermissionTemplatesPage.test.tsx | `+` | 006 |     expect((screen.getByTestId("permission-template-edit-valid-unt |
| PermissionTemplatesPage.test.tsx | `+` | 006 |     fireEvent.click(screen.getByTestId("permission-template-update |
| PermissionTemplatesPage.test.tsx | `+` | 006 |     await waitFor(() => expect(putBody).toBeTruthy()); |
| PermissionTemplatesPage.test.tsx | `+` | 006 |     expect(new Date(String(putBody!.valid_from)).getTime()).toBe(n |
| PermissionTemplatesPage.test.tsx | `+` | 006 |     expect(new Date(String(putBody!.valid_until)).getTime()).toBe( |
| PermissionTemplatesPage.test.tsx | `+` | 006 |   }); |
| PermissionTemplatesPage.test.tsx | `+` | 006 | }); |
| PermissionTemplatesPage.tsx | `-` | 001 | const TEMPLATE_TYPE_LABELS: Record<string, string> = { base: "基础模板 |
| PermissionTemplatesPage.tsx | `+` | 001 | const TEMPLATE_TYPE_LABELS: Record<string, string> = { base: "基础模板 |
| PermissionTemplatesPage.tsx | `+` | 006 | // M038-006：datetime-local 输入框按本地时区解释，这里也按本地时区取年月日时分，「读出→不改→保存」才是同 |
| PermissionTemplatesPage.tsx | `-` | 006 |   return date.toISOString().slice(0, 16); |
| PermissionTemplatesPage.tsx | `+` | 006 |   const pad = (part: number) => String(part).padStart(2, "0"); |
| PermissionTemplatesPage.tsx | `+` | 006 |   return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad( |
| PermissionTemplatesPage.tsx | `-` | 002 |       setFormError("至少输入一个目标员工 ID。"); |
| PermissionTemplatesPage.tsx | `+` | 002 |       setFormError("至少输入一个目标员工编号。"); |
| PermissionTemplatesPage.tsx | `+` | 001 |               <option value="role">角色模板</option> |
| PermissionTemplatesPage.tsx | `-` | 002 |           <label>目标员工 ID |
| PermissionTemplatesPage.tsx | `+` | 002 |           <label>目标员工编号 |
| PermissionTemplatesPage.tsx | `+` | 001 |                     <option value="role">角色模板</option> |
| PermissionGate.test.ts | `-` | 010 | import { describe, expect, it } from "vitest"; |
| PermissionGate.test.ts | `+` | 010 | import { fireEvent, render, screen } from "@testing-library/react" |
| PermissionGate.test.ts | `+` | 010 | import { createElement } from "react"; |
| PermissionGate.test.ts | `+` | 010 | import { describe, expect, it, vi } from "vitest"; |
| PermissionGate.test.ts | `-` | 010 | import { hasPermissions } from "./PermissionGate"; |
| PermissionGate.test.ts | `+` | 010 | import { Can, PermissionButton } from "./Can"; |
| PermissionGate.test.ts | `+` | 010 | import { hasPermissions, PermissionGate } from "./PermissionGate"; |
| PermissionGate.test.ts | `+` | 010 |  |
| PermissionGate.test.ts | `+` | 010 | // M038-010：权限闸组件的渲染分支（此前只测了纯函数 hasPermissions）。本文件是 .ts，用 createE |
| PermissionGate.test.ts | `+` | 010 | describe("PermissionButton / Can / PermissionGate 组件", () => { |
| PermissionGate.test.ts | `+` | 010 |   const denied = ready({ grants: ["access.role.view"] }); |
| PermissionGate.test.ts | `+` | 010 |   const allowed = ready({ grants: ["access.role.assign"] }); |
| PermissionGate.test.ts | `+` | 010 |  |
| PermissionGate.test.ts | `+` | 010 |   it("无权限：按钮禁用、带提示，点击不触发；有权限：点击触发", () => { |
| PermissionGate.test.ts | `+` | 010 |     const onClick = vi.fn(); |
| PermissionGate.test.ts | `+` | 010 |     const view = render(createElement(PermissionButton, { permissi |
| PermissionGate.test.ts | `+` | 010 |     const button = screen.getByTestId("gate-btn") as HTMLButtonEle |
| PermissionGate.test.ts | `+` | 010 |     expect(button.disabled).toBe(true); |
| PermissionGate.test.ts | `+` | 010 |     expect(button.title).toBe("当前账号无此操作权限"); |
| PermissionGate.test.ts | `+` | 010 |     fireEvent.click(button); |
| PermissionGate.test.ts | `+` | 010 |     expect(onClick).not.toHaveBeenCalled(); |
| PermissionGate.test.ts | `+` | 010 |     view.rerender(createElement(PermissionButton, { permissionsSta |
| PermissionGate.test.ts | `+` | 010 |     expect((screen.getByTestId("gate-btn") as HTMLButtonElement).d |
| PermissionGate.test.ts | `+` | 010 |     fireEvent.click(screen.getByTestId("gate-btn")); |
| PermissionGate.test.ts | `+` | 010 |     expect(onClick).toHaveBeenCalledTimes(1); |
| PermissionGate.test.ts | `+` | 010 |     view.unmount(); |
| PermissionGate.test.ts | `+` | 010 |   }); |
| PermissionGate.test.ts | `+` | 010 |  |
| PermissionGate.test.ts | `+` | 010 |   it("显式拒绝压过授予：按钮仍禁用", () => { |
| PermissionGate.test.ts | `+` | 010 |     const onClick = vi.fn(); |
| PermissionGate.test.ts | `+` | 010 |     const view = render(createElement(PermissionButton, { permissi |
| PermissionGate.test.ts | `+` | 010 |     fireEvent.click(screen.getByTestId("gate-btn-deny")); |
| PermissionGate.test.ts | `+` | 010 |     expect((screen.getByTestId("gate-btn-deny") as HTMLButtonEleme |
| PermissionGate.test.ts | `+` | 010 |     expect(onClick).not.toHaveBeenCalled(); |
| PermissionGate.test.ts | `+` | 010 |     view.unmount(); |
| PermissionGate.test.ts | `+` | 010 |   }); |
| PermissionGate.test.ts | `+` | 010 |  |
| PermissionGate.test.ts | `+` | 010 |   it("Can：无权限渲染 fallback，有权限渲染子内容", () => { |
| PermissionGate.test.ts | `+` | 010 |     const view = render(createElement(Can, { permissionsState: den |
| PermissionGate.test.ts | `+` | 010 |     expect(view.container.textContent).toBe("无权查看"); |
| PermissionGate.test.ts | `+` | 010 |     view.rerender(createElement(Can, { permissionsState: allowed,  |
| PermissionGate.test.ts | `+` | 010 |     expect(view.container.textContent).toBe("敏感内容"); |
| PermissionGate.test.ts | `+` | 010 |     view.unmount(); |
| PermissionGate.test.ts | `+` | 010 |   }); |
| PermissionGate.test.ts | `+` | 010 |  |
| PermissionGate.test.ts | `+` | 010 |   it("PermissionGate：加载中、权限不足、放行三种渲染", () => { |
| PermissionGate.test.ts | `+` | 010 |     const view = render(createElement(PermissionGate, { permission |
| PermissionGate.test.ts | `+` | 010 |     expect(screen.getByTestId("admin-permission-loading").textCont |
| PermissionGate.test.ts | `+` | 010 |     view.rerender(createElement(PermissionGate, { permissionsState |
| PermissionGate.test.ts | `+` | 010 |     expect(screen.getByTestId("admin-permission-denied").textConte |
| PermissionGate.test.ts | `+` | 010 |     expect(view.container.textContent).not.toContain("页面内容"); |
| PermissionGate.test.ts | `+` | 010 |     view.rerender(createElement(PermissionGate, { permissionsState |
| PermissionGate.test.ts | `+` | 010 |     expect(view.container.textContent).toBe("页面内容"); |
| PermissionGate.test.ts | `+` | 010 |     view.unmount(); |
| PermissionGate.test.ts | `+` | 010 |   }); |
| PermissionGate.test.ts | `+` | 010 | }); |

- 8 文件 37 个 hunk、545 条 ± 行，无归因不到的行；无 mode 变更；本模块 16 个文件之外无改动（公共字典 statusLabels.ts、客户端 adminClient.ts、后端、三份 HTML、锁未动）。归因票：**PASS**。

## 2. diff 全文

```diff
diff --git a/apps/web/src/admin/pages/AccessGovernancePage.guidedcreate.test.tsx b/apps/web/src/admin/pages/AccessGovernancePage.guidedcreate.test.tsx
index 0e798a4e..84566763 100644
--- a/apps/web/src/admin/pages/AccessGovernancePage.guidedcreate.test.tsx
+++ b/apps/web/src/admin/pages/AccessGovernancePage.guidedcreate.test.tsx
@@ -86,7 +86,7 @@ describe("AccessGovernancePage 引导式新增岗位角色模板(phase-9)", () =
     fireEvent.click(screen.getByTestId("access-position-role-template-create-open"));
 
     fireEvent.change(screen.getByTestId("access-position-role-template-position"), { target: { value: "front_desk" } });
-    fireEvent.change(screen.getByTestId("access-position-role-template-level"), { target: { value: "lvl_p1" } });
+    fireEvent.change(screen.getByTestId("access-position-role-template-level"), { target: { value: "lvl_m2_upper" } });
     fireEvent.click(screen.getByTestId("access-position-role-template-guided-next")); // 步2→确认
     await waitFor(() => expect(screen.getByTestId("access-position-role-template-guided-summary")).toBeTruthy());
     fireEvent.click(screen.getByTestId("access-position-role-template-save"));
@@ -94,7 +94,7 @@ describe("AccessGovernancePage 引导式新增岗位角色模板(phase-9)", () =
     await waitFor(() => expect(lastPostBody).toBeTruthy());
     expect(lastPostUrl).not.toContain("prt-"); // 新建走 POST 集合路径，非 PUT /{id}
     expect((lastPostBody as { position_code: string }).position_code).toBe("front_desk");
-    expect((lastPostBody as { staff_level_id: string }).staff_level_id).toBe("lvl_p1");
+    expect((lastPostBody as { staff_level_id: string }).staff_level_id).toBe("lvl_m2_upper");
     expect((lastPostBody as { role_id: string }).role_id).toBe("role-1");
     await waitFor(() => expect(screen.queryByTestId("access-position-role-template-guided")).toBeNull());
     expect(setActionResult).toHaveBeenCalledWith(expect.stringMatching(/^岗位模板已保存/)); // PGM-1.18：结果提示全中文
@@ -111,3 +111,81 @@ describe("AccessGovernancePage 引导式新增岗位角色模板(phase-9)", () =
     expect(lastPostBody).toBeNull();
   });
 });
+
+// M038-010：发起授权申请的引导提交（入口→类型与用户→权限码与过期时间→提交）此前没有测试。
+describe("AccessGovernancePage 引导式发起授权申请（M038-010）", () => {
+  afterEach(() => vi.unstubAllGlobals());
+
+  it("入口→步进→提交：POST /grant-requests 带类型、目标用户、权限码、过期时间与单次有效", async () => {
+    let grantPost: { url: string; body: Record<string, unknown> } | null = null;
+    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
+      const url = String(input);
+      if ((init?.method ?? "GET").toUpperCase() === "POST" && url.endsWith("/api/v1/access/grant-requests")) {
+        grantPost = { url, body: JSON.parse(String(init?.body)) };
+        return Promise.resolve(raw({ id: "agr-new", status: "pending" }));
+      }
+      if (url.includes("grant-requests/summary")) return Promise.resolve(raw({ position_recommendations: [], user_anomalies: [] }));
+      if (url.includes("grant-requests")) return Promise.resolve(envelope([]));
+      if (url.includes("position-role-templates")) return Promise.resolve(envelope(TEMPLATES));
+      if (url.includes("access/roles")) return Promise.resolve(envelope(ROLES));
+      if (url.includes("access/users")) return Promise.resolve(envelope(USERS));
+      return Promise.resolve(envelope([]));
+    }));
+    const setActionResult = vi.fn();
+    const props: AdminPageProps = {
+      api: new AdminApi("test-token"),
+      user: { id: "u1", username: "admin", accountType: "platform_admin" },
+      setActionResult,
+      setGlobalError: vi.fn(),
+      effectivePermissions: ADMIN_PERMS,
+    };
+    render(<AccessGovernancePage {...props} />);
+    await waitFor(() => expect(screen.getByTestId("access-position-role-templates-table").textContent).toContain("prt-1"));
+    fireEvent.click(screen.getByTestId("access-grant-request-create-open"));
+    await waitFor(() => expect((screen.getByLabelText("目标用户") as HTMLSelectElement).value).toBe("u-1"));
+    fireEvent.click(screen.getByTestId("access-grant-request-guided-next")); // 步1→步2
+    fireEvent.change(screen.getByLabelText("临时权限码"), { target: { value: "finance.refund.create" } });
+    fireEvent.change(screen.getByLabelText("过期时间"), { target: { value: "2026-12-31T18:00" } });
+    fireEvent.click(screen.getByTestId("access-grant-request-guided-next")); // 步2→确认
+    await waitFor(() => expect(screen.getByTestId("access-grant-request-guided-summary")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("access-grant-request-create"));
+
+    await waitFor(() => expect(grantPost).toBeTruthy());
+    const body = grantPost!.body;
+    expect(body.request_type).toBe("temporary_permission");
+    expect(body.target_user_id).toBe("u-1");
+    expect(body.permission_codes).toEqual(["finance.refund.create"]);
+    expect(new Date(String(body.expires_at)).getTime()).toBe(new Date("2026-12-31T18:00").getTime());
+    expect(body.single_use).toBe(true);
+    await waitFor(() => expect(screen.queryByTestId("access-grant-request-guided")).toBeNull());
+    expect(setActionResult).toHaveBeenCalledWith(expect.stringMatching(/^授权申请已提交：agr-new（待处理）/));
+  });
+
+  it("步内校验拦截：临时权限未填过期时间则不进确认、不提交", async () => {
+    const fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL) => {
+      const url = String(input);
+      if (url.includes("grant-requests/summary")) return Promise.resolve(raw({ position_recommendations: [], user_anomalies: [] }));
+      if (url.includes("position-role-templates")) return Promise.resolve(envelope(TEMPLATES));
+      if (url.includes("access/roles")) return Promise.resolve(envelope(ROLES));
+      if (url.includes("access/users")) return Promise.resolve(envelope(USERS));
+      return Promise.resolve(envelope([]));
+    });
+    vi.stubGlobal("fetch", fetchMock);
+    const props: AdminPageProps = {
+      api: new AdminApi("test-token"),
+      user: { id: "u1", username: "admin", accountType: "platform_admin" },
+      setActionResult: vi.fn(),
+      setGlobalError: vi.fn(),
+      effectivePermissions: ADMIN_PERMS,
+    };
+    render(<AccessGovernancePage {...props} />);
+    await waitFor(() => expect(screen.getByTestId("access-position-role-templates-table").textContent).toContain("prt-1"));
+    fireEvent.click(screen.getByTestId("access-grant-request-create-open"));
+    await waitFor(() => expect((screen.getByLabelText("目标用户") as HTMLSelectElement).value).toBe("u-1"));
+    fireEvent.click(screen.getByTestId("access-grant-request-guided-next"));
+    fireEvent.click(screen.getByTestId("access-grant-request-guided-next")); // 过期时间未填
+    expect(screen.getByTestId("access-grant-request-guided-error").textContent).toContain("过期时间");
+    expect(screen.queryByTestId("access-grant-request-guided-summary")).toBeNull();
+    expect(fetchMock.mock.calls.some(([, init]) => ((init as RequestInit | undefined)?.method ?? "GET").toUpperCase() === "POST")).toBe(false);
+  });
+});
diff --git a/apps/web/src/admin/pages/AccessGovernancePage.test.tsx b/apps/web/src/admin/pages/AccessGovernancePage.test.tsx
index 70036e26..9ac75006 100644
--- a/apps/web/src/admin/pages/AccessGovernancePage.test.tsx
+++ b/apps/web/src/admin/pages/AccessGovernancePage.test.tsx
@@ -95,7 +95,7 @@ describe("AccessGovernancePage 行明细抽屉 + 零英文", () => {
   it("员工单独授予 / 显式拒绝有写入口：显式拒绝按目标用户提交（PGM-1.10.1）", async () => {
     const props = renderPage();
     await waitFor(() => expect(screen.getByTestId("access-user-override-form")).toBeTruthy());
-    fireEvent.change(screen.getByLabelText("覆盖用户 id"), { target: { value: "u-1" } });
+    fireEvent.change(screen.getByLabelText("覆盖用户"), { target: { value: "u-1" } });
     fireEvent.change(screen.getByLabelText("覆盖权限码"), { target: { value: "access.role.assign" } });
     fireEvent.change(screen.getByLabelText("覆盖效果"), { target: { value: "deny" } });
     fireEvent.click(screen.getByTestId("access-user-override-set"));
@@ -183,3 +183,146 @@ describe("AccessGovernancePage 行明细抽屉 + 零英文", () => {
     expect(text).not.toMatch(/\buser\b/);
   });
 });
+
+// M038 回归：取值全中文（001）、未登记岗位（003）、恢复文案（004）、写动作失败提示与防双击（005）、表头列数（007）、职级 30 档下拉（008）、状态用词（009）。
+describe("M038 访问治理页回归", () => {
+  afterEach(() => vi.unstubAllGlobals());
+
+  const M038_GRANTS = [
+    { id: "gr-a", request_type: "user_scope_change", target_user_id: "u-1", status: "applied" },
+    { id: "gr-b", request_type: "position_high_risk_release", target_user_id: "u-1", status: "pending" },
+    { id: "gr-c", request_type: "role_permission", target_user_id: "u-1", status: "rejected" },
+  ];
+  const M038_TEMPLATES = [
+    { id: "prt-x", position_code: "front_desk", staff_level_id: "lvl_m2_upper", role_id: "role-1", status: "active" },
+    { id: "prt-arch", position_code: "ops_manager", staff_level_id: "lvl_m1_lower", role_id: "role-1", status: "inactive", archived_at: "2026-08-01T00:00:00Z" },
+    { id: "prt-del", position_code: "ops_manager", staff_level_id: "lvl_m1_lower", role_id: "role-1", status: "inactive", archived_at: "2026-08-01T00:00:00Z", deleted_at: "2026-08-02T00:00:00Z", delete_review_id: "rv-1" },
+  ];
+  type Write = { url: string; body: Record<string, unknown> };
+
+  function renderM038(onWrite: (write: Write) => Promise<Response>) {
+    const writes: Write[] = [];
+    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
+      const url = String(input);
+      if ((init?.method ?? "GET").toUpperCase() !== "GET") {
+        const write = { url, body: init?.body ? JSON.parse(String(init.body)) : {} };
+        writes.push(write);
+        return onWrite(write);
+      }
+      if (url.includes("grant-requests/summary")) return Promise.resolve(raw(SUMMARY));
+      if (url.includes("grant-requests")) return Promise.resolve(envelope(M038_GRANTS));
+      if (url.includes("position-role-templates")) return Promise.resolve(envelope(M038_TEMPLATES));
+      if (url.includes("access/roles")) return Promise.resolve(envelope(ROLES));
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
+    render(<AccessGovernancePage {...props} />);
+    return { props, writes };
+  }
+
+  it("授权申请的三种状态与内部类型全部中文上屏（001）", async () => {
+    renderM038(() => Promise.resolve(raw({})));
+    await waitFor(() => expect(screen.getByTestId("access-grant-request-gr-a")).toBeTruthy());
+    const text = screen.getByTestId("access-grant-requests-table").textContent ?? "";
+    for (const label of ["已生效", "待处理", "已驳回", "人员范围变更", "岗位高危权限释放", "角色权限"]) expect(text).toContain(label);
+    for (const code of ["applied", "user_scope_change", "position_high_risk_release", "role_permission"]) expect(text).not.toContain(code);
+    fireEvent.click(screen.getByTestId("access-grant-request-gr-a"));
+    const drawer = screen.getByTestId("row-detail-drawer").textContent ?? "";
+    expect(drawer).toContain("人员范围变更");
+    expect(drawer).toContain("已生效");
+    expect(drawer).not.toContain("applied");
+  });
+
+  it("审批不填意见时留痕为中文「通过」，结果提示为中文状态（001）", async () => {
+    const { props, writes } = renderM038(() => Promise.resolve(raw({ id: "gr-b", status: "applied" })));
+    await waitFor(() => expect(screen.getByTestId("access-grant-actions-gr-b")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("access-grant-actions-gr-b"));
+    await waitFor(() => expect(screen.getByTestId("access-grant-approve-gr-b")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("access-grant-approve-gr-b"));
+    await waitFor(() => expect(writes).toHaveLength(1));
+    expect(writes[0]!.url).toContain("/grant-requests/gr-b/review");
+    expect(writes[0]!.body.comment).toBe("通过");
+    expect(writes[0]!.body.operator_note).toBe("通过");
+    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(expect.stringContaining("已生效")));
+  });
+
+  it("未映射的岗位码显示为「未登记岗位（岗位码）」（003）", async () => {
+    renderM038(() => Promise.resolve(raw({})));
+    await waitFor(() => expect(screen.getByTestId("access-position-role-template-prt-x")).toBeTruthy());
+    const cells = screen.getByTestId("access-position-role-template-prt-x").querySelectorAll("td");
+    expect(cells[1]?.textContent).toBe("未登记岗位（front_desk）");
+    expect(cells[2]?.textContent).toBe("M2·上");
+  });
+
+  it("已存档的按钮写「取消存档」、已删除的写「恢复」，不再写「停用模板」（004）", async () => {
+    renderM038(() => Promise.resolve(raw({})));
+    await waitFor(() => expect(screen.getByTestId("access-position-role-template-prt-arch")).toBeTruthy());
+    const buttonTexts = (rowId: string) => Array.from(screen.getByTestId(`access-position-role-template-${rowId}`).querySelectorAll("button")).map((button) => button.textContent);
+    expect(buttonTexts("prt-arch")).toContain("取消存档");
+    expect(buttonTexts("prt-del")).toContain("恢复");
+    expect(buttonTexts("prt-del")).not.toContain("取消存档");
+    expect(screen.getByTestId("access-position-role-templates-table").textContent).not.toContain("停用模板");
+  });
+
+  it("装配角色被后端拒绝时页面报出原因（005）", async () => {
+    const { props, writes } = renderM038(() => Promise.resolve(new Response(
+      JSON.stringify({ success: false, code: "GRANT_REQUIRES_APPROVAL", message: "以下权限的授予需要审批：[finance.refund.create]" }),
+      { status: 403, headers: { "content-type": "application/json" } },
+    )));
+    await waitFor(() => expect((screen.getByLabelText("装配角色") as HTMLSelectElement).value).toBe("role-1"));
+    fireEvent.click(screen.getByTestId("access-role-assign"));
+    await waitFor(() => expect(writes).toHaveLength(1));
+    await waitFor(() => expect(props.setGlobalError).toHaveBeenCalledWith(expect.stringMatching(/\S/)));
+    expect(props.setActionResult).not.toHaveBeenCalled();
+  });
+
+  it("提交未完成时再次点击不发第二笔（005）", async () => {
+    let release: (value: Response) => void = () => undefined;
+    const { writes } = renderM038(() => new Promise<Response>((resolve) => { release = resolve; }));
+    await waitFor(() => expect((screen.getByLabelText("装配角色") as HTMLSelectElement).value).toBe("role-1"));
+    fireEvent.click(screen.getByTestId("access-role-assign"));
+    fireEvent.click(screen.getByTestId("access-role-assign"));
+    await waitFor(() => expect(writes).toHaveLength(1));
+    await waitFor(() => expect((screen.getByTestId("access-role-assign") as HTMLButtonElement).disabled).toBe(true));
+    release(raw({}));
+    await waitFor(() => expect((screen.getByTestId("access-role-assign") as HTMLButtonElement).disabled).toBe(false));
+    expect(writes.filter((write) => write.url.includes("/role-assignments"))).toHaveLength(1);
+  });
+
+  it("智能总结表表头列数等于数据行单元格数（007）", async () => {
+    renderM038(() => Promise.resolve(raw({})));
+    await waitFor(() => expect(screen.getByTestId("access-grant-summary-position-ops_manager")).toBeTruthy());
+    const table = screen.getByTestId("access-grant-summary-table");
+    expect(table.querySelectorAll("thead th")).toHaveLength(table.querySelectorAll("tbody tr")[0]!.querySelectorAll("td").length);
+  });
+
+  it("职级是 30 档下拉，范围变更处另有「沿用原职级」（008）", async () => {
+    renderM038(() => Promise.resolve(raw({})));
+    await waitFor(() => expect(screen.getByTestId("access-user-position-level")).toBeTruthy());
+    const options = Array.from((screen.getByTestId("access-user-position-level") as HTMLSelectElement).options);
+    expect(options).toHaveLength(30);
+    expect(options[0]!.value).toBe("lvl_m1_lower");
+    expect(options[0]!.textContent).toBe("M1·下");
+    expect(options[29]!.value).toBe("lvl_m10_upper");
+    expect(options[29]!.textContent).toBe("M10·上");
+    const scopeOptions = Array.from((screen.getByTestId("access-user-scope-level") as HTMLSelectElement).options);
+    expect(scopeOptions).toHaveLength(31);
+    expect(scopeOptions[0]!.textContent).toBe("沿用原职级");
+  });
+
+  it("岗位模板弹窗里的状态用词与表格同一套（009）", async () => {
+    renderM038(() => Promise.resolve(raw({})));
+    await waitFor(() => expect(screen.getByTestId("access-position-role-template-create-open")).toBeTruthy());
+    await waitFor(() => expect((screen.getByLabelText("装配角色") as HTMLSelectElement).value).toBe("role-1"));
+    fireEvent.click(screen.getByTestId("access-position-role-template-create-open")); // 引导弹窗为单页填写，状态下拉就在第一页
+    const status = await waitFor(() => screen.getByTestId("access-position-role-template-status") as HTMLSelectElement);
+    expect(Array.from(status.options).map((option) => option.textContent)).toEqual(["有效", "未激活"]);
+  });
+});
diff --git a/apps/web/src/admin/pages/AccessGovernancePage.tsx b/apps/web/src/admin/pages/AccessGovernancePage.tsx
index 55ed4928..1e3b6545 100644
--- a/apps/web/src/admin/pages/AccessGovernancePage.tsx
+++ b/apps/web/src/admin/pages/AccessGovernancePage.tsx
@@ -1,4 +1,4 @@
-// 文件版本：V0.03
+// 文件版本：V0.04
 import React from "react";
 import { AdminDataState } from "../components/AdminDataState";
 import { AdminPageHeader } from "../components/AdminPageHeader";
@@ -49,7 +49,28 @@ const POSITION_LABELS: Record<string, string> = {
 };
 function zhPosition(code?: string | null): string {
   if (!code) return "—";
-  return POSITION_LABELS[code] ?? code;
+  // M038-003：未映射的岗位码不裸露英文——中文说明在前，岗位码作为系统内部标识符放括号里。
+  return POSITION_LABELS[code] ?? `未登记岗位（${code}）`;
+}
+// M038-008：职级是速查表写明的 30 档闭集（M1–M10 × 上/中/下），编号与库内 staff_levels 一致；页内生成下拉，不手填。
+const STAFF_LEVEL_IDS: string[] = [];
+for (let main = 1; main <= 10; main += 1) {
+  for (const band of ["lower", "middle", "upper"]) STAFF_LEVEL_IDS.push(`lvl_m${main}_${band}`);
+}
+function staffLevelOptions() {
+  return STAFF_LEVEL_IDS.map((id) => <option key={id} value={id}>{zhLevel(id)}</option>);
+}
+// M038-001：授权申请的状态与类型按后端闭集在页内给全中文（公共字典缺「已生效」与三种内部类型）。
+const GRANT_STATUS_LABELS: Record<string, string> = { pending: "待处理", applied: "已生效", rejected: "已驳回" };
+const GRANT_TYPE_LABELS: Record<string, string> = {
+  user_role: "用户角色", user_position: "用户职位", temporary_permission: "临时权限",
+  role_permission: "角色权限", user_scope_change: "人员范围变更", position_high_risk_release: "岗位高危权限释放",
+};
+function grantStatusLabel(code?: string | null): string {
+  return code ? GRANT_STATUS_LABELS[code] ?? formatBusinessStatus(code) : "—";
+}
+function grantTypeLabel(code?: string | null): string {
+  return code ? GRANT_TYPE_LABELS[code] ?? formatBusinessStatus(code) : "—";
 }
 function zhLevel(code?: string | null): string {
   if (!code) return "—";
@@ -81,14 +102,31 @@ function roleLabel(row: RoleRow | undefined) {
   return row ? `${row.roleName ?? row.roleCode ?? row.id} (${row.id})` : "-";
 }
 
-function statusBadge(status?: string | null) {
+function statusBadge(status?: string | null, label: (code: string) => string = formatBusinessStatus) {
   const value = status || "";
   const kind = value === "approved" || value === "active" ? "is-ok" : value === "rejected" ? "is-danger" : value === "pending" ? "is-warn" : "is-info";
   // 徽标文字中文化（业务HTML零英文规则）；颜色仍按原始码判定。
-  return <span className={`admin-badge ${kind}`}>{value ? formatBusinessStatus(value) : "—"}</span>;
+  return <span className={`admin-badge ${kind}`}>{value ? label(value) : "—"}</span>;
 }
 
 export function AccessGovernancePage({ api, user, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
+  // M038-005：页内写动作统一走 runWrite——提交中置忙（防双击发两笔）、失败把后端中文消息报到页面。
+  const [writeBusy, setWriteBusy] = React.useState(false);
+  const writeBusyRef = React.useRef(false);
+  async function runWrite(action: () => Promise<void>, fallbackMessage: string) {
+    if (writeBusyRef.current) return;
+    writeBusyRef.current = true;
+    setWriteBusy(true);
+    setGlobalError(null);
+    try {
+      await action();
+    } catch (actionError) {
+      setGlobalError(actionError instanceof Error ? actionError.message : fallbackMessage);
+    } finally {
+      writeBusyRef.current = false;
+      setWriteBusy(false);
+    }
+  }
   const [users, setUsers] = React.useState<AccessUserRow[]>([]);
   const [roles, setRoles] = React.useState<RoleRow[]>([]);
   const [positionTemplates, setPositionTemplates] = React.useState<PositionRoleTemplateRow[]>([]);
@@ -371,7 +409,7 @@ export function AccessGovernancePage({ api, user, setActionResult, setGlobalErro
         reason: grantForm.reason.trim() || "后台高危授权申请",
         operatorNote: grantForm.reason.trim() || "后台高危授权申请",
       });
-      setActionResult(`授权申请已提交：${result.id}（${formatBusinessStatus(result.status ?? "")}）`);
+      setActionResult(`授权申请已提交：${result.id}（${grantStatusLabel(result.status)}）`);
       setGrantOpen(false);
       await reload();
     } catch (actionError) {
@@ -386,11 +424,12 @@ export function AccessGovernancePage({ api, user, setActionResult, setGlobalErro
   async function reviewGrant(row: AccessGrantRequestRow, decision: "approve" | "reject") {
     const result = await api.post<AccessGrantRequestRow>(`/api/v1/access/grant-requests/${encodeURIComponent(row.id)}/review`, {
       decision,
-      comment: reviewForm.comment.trim() || decision,
-      operatorNote: reviewForm.comment.trim() || decision,
+      // M038-001：不填意见时留痕用中文，不把 approve／reject 当意见。
+      comment: reviewForm.comment.trim() || (decision === "approve" ? "通过" : "驳回"),
+      operatorNote: reviewForm.comment.trim() || (decision === "approve" ? "通过" : "驳回"),
       evidenceAttachmentIds: splitTokens(reviewForm.evidenceAttachmentIds),
     });
-    setActionResult(`授权申请 ${row.id} 已${decision === "approve" ? "通过" : "驳回"}（${formatBusinessStatus(result.status ?? "")}）`);
+    setActionResult(`授权申请 ${row.id} 已${decision === "approve" ? "通过" : "驳回"}（${grantStatusLabel(result.status)}）`);
     await reload();
   }
 
@@ -415,12 +454,12 @@ export function AccessGovernancePage({ api, user, setActionResult, setGlobalErro
       title: `授权申请 · ${row.id}`,
       fields: [
         { label: "申请编号", value: row.id },
-        { label: "类型", value: formatBusinessStatus(row.requestType) },
+        { label: "类型", value: grantTypeLabel(row.requestType) },
         { label: "目标用户", value: row.targetUserId },
         { label: "角色", value: row.targetRoleId ?? "—" },
         { label: "岗位", value: row.positionCode ? `${zhPosition(row.positionCode)} / ${zhLevel(row.staffLevelId)}` : "—" },
         { label: "权限码", value: row.permissionCodes?.length ? row.permissionCodes.join("、") : "—" },
-        { label: "状态", value: formatBusinessStatus(row.status) },
+        { label: "状态", value: grantStatusLabel(row.status) },
         { label: "单次有效", value: row.singleUse ? "是" : "否" },
         { label: "过期时间", value: cnDate(row.expiresAt) },
         { label: "申请原因", value: row.reason ?? "—" },
@@ -479,12 +518,12 @@ export function AccessGovernancePage({ api, user, setActionResult, setGlobalErro
                 <td>{row.id}</td><td>{zhPosition(row.positionCode)}</td><td>{zhLevel(row.staffLevelId)}</td><td>{row.roleId ?? "-"}</td><td>{row.templateId ?? "-"}</td><td>{row.organizationId ?? "-"}</td><td>{row.deletedAt ? "已删除（可恢复）" : row.archivedAt ? "已存档" : statusBadge(row.status)}</td>
                 <td onClick={(event) => event.stopPropagation()}>
                   <button type="button" data-testid={`access-position-role-template-select-${row.id}`} disabled={Boolean(row.archivedAt)} onClick={() => selectTemplate(row)}>选中</button>
-                  {!row.archivedAt ? <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid={`access-position-role-template-archive-${row.id}`} onClick={() => void changePositionLifecycle(row.id, "archive")}>存档</PermissionButton> : <>
-                    {!row.deletedAt && !row.deleteReviewId ? <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} onClick={() => void changePositionLifecycle(row.id, "request_delete")}>{user.id === "usr_master_authority" && user.username === "master-authority" ? "最高权限直接删除" : "申请删除"}</PermissionButton> : null}
+                  {!row.archivedAt ? <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid={`access-position-role-template-archive-${row.id}`} disabled={writeBusy} onClick={() => void runWrite(() => changePositionLifecycle(row.id, "archive"), "岗位模板处理失败")}>存档</PermissionButton> : <>
+                    {!row.deletedAt && !row.deleteReviewId ? <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} disabled={writeBusy} onClick={() => void runWrite(() => changePositionLifecycle(row.id, "request_delete"), "岗位模板处理失败")}>{user.id === "usr_master_authority" && user.username === "master-authority" ? "最高权限直接删除" : "申请删除"}</PermissionButton> : null}
                     {!row.deletedAt && row.deleteReviewId ? <>
-                      <PermissionButton permissionsState={effectivePermissions} permissions={["access.role.assign", "governance.supervisor_review.approve"]} onClick={() => void changePositionLifecycle(row.id, "approve_delete")}>最高权限批准删除</PermissionButton>
-                      <PermissionButton permissionsState={effectivePermissions} permissions={["access.role.assign", "governance.supervisor_review.approve"]} onClick={() => void changePositionLifecycle(row.id, "reject_delete")}>驳回删除</PermissionButton>
-                    </> : <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} onClick={() => void changePositionLifecycle(row.id, "restore")}>恢复为停用模板</PermissionButton>}
+                      <PermissionButton permissionsState={effectivePermissions} permissions={["access.role.assign", "governance.supervisor_review.approve"]} disabled={writeBusy} onClick={() => void runWrite(() => changePositionLifecycle(row.id, "approve_delete"), "岗位模板处理失败")}>最高权限批准删除</PermissionButton>
+                      <PermissionButton permissionsState={effectivePermissions} permissions={["access.role.assign", "governance.supervisor_review.approve"]} disabled={writeBusy} onClick={() => void runWrite(() => changePositionLifecycle(row.id, "reject_delete"), "岗位模板处理失败")}>驳回删除</PermissionButton>
+                    </> : <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} disabled={writeBusy} onClick={() => void runWrite(() => changePositionLifecycle(row.id, "restore"), "岗位模板处理失败")}>{row.deletedAt ? "恢复" : "取消存档"}</PermissionButton>}
                   </>}
                 </td>
               <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看岗位角色模板详情" onClick={(event) => { event.stopPropagation(); openTemplateDetail(row); }}>查看详情</button></td></tr>
@@ -503,48 +542,48 @@ export function AccessGovernancePage({ api, user, setActionResult, setGlobalErro
 
         <h3>给用户定职位自动适配</h3>
         <div className="admin-inline-form" data-testid="access-user-position-form">
-          <select aria-label="用户 id" value={positionForm.userId} onChange={(event) => setPositionForm({ ...positionForm, userId: event.target.value })}>{users.map((user) => <option key={user.id} value={user.id}>{user.username} ({user.id})</option>)}</select>
+          <select aria-label="用户" value={positionForm.userId} onChange={(event) => setPositionForm({ ...positionForm, userId: event.target.value })}>{users.map((user) => <option key={user.id} value={user.id}>{user.username} ({user.id})</option>)}</select>
           <input aria-label="岗位编码" placeholder="岗位编码" value={positionForm.positionCode} onChange={(event) => setPositionForm({ ...positionForm, positionCode: event.target.value })} />
-          <input aria-label="职级 id" placeholder="职级 id" value={positionForm.staffLevelId} onChange={(event) => setPositionForm({ ...positionForm, staffLevelId: event.target.value })} />
+          <select aria-label="职级" data-testid="access-user-position-level" value={positionForm.staffLevelId} onChange={(event) => setPositionForm({ ...positionForm, staffLevelId: event.target.value })}>{staffLevelOptions()}</select>
           <input aria-label="基础薪资元" type="number" min="0" placeholder="基础薪资(元)" value={positionForm.baseSalaryYuan} onChange={(event) => setPositionForm({ ...positionForm, baseSalaryYuan: event.target.value })} />
           <input aria-label="薪资备注" placeholder="薪资备注" value={positionForm.salaryDetail} onChange={(event) => setPositionForm({ ...positionForm, salaryDetail: event.target.value })} />
           <input aria-label="操作备注" placeholder="操作备注" value={positionForm.operatorNote} onChange={(event) => setPositionForm({ ...positionForm, operatorNote: event.target.value })} />
-          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-user-position-assign" onClick={() => void assignPosition()}>定职位并适配</PermissionButton>
+          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-user-position-assign" disabled={writeBusy} onClick={() => void runWrite(assignPosition, "定岗失败")}>定职位并适配</PermissionButton>
         </div>
 
         <h3>人员范围变更（所属商户 / 跨项目）</h3>
         <div className="admin-inline-form" data-testid="access-user-scope-form">
-          <select aria-label="变更用户 id" data-testid="access-user-scope-user" value={scopeForm.userId || positionForm.userId} onChange={(event) => setScopeForm({ ...scopeForm, userId: event.target.value })}>{users.map((user) => <option key={user.id} value={user.id}>{user.username} ({user.id})</option>)}</select>
-          <input aria-label="新所属项目 id" data-testid="access-user-scope-org" placeholder="新所属项目 id（不变可空）" value={scopeForm.organizationId} onChange={(event) => setScopeForm({ ...scopeForm, organizationId: event.target.value })} />
-          <input aria-label="新所属商户 id" data-testid="access-user-scope-merchant" placeholder="新所属商户 id（平台员工留空）" value={scopeForm.merchantId} onChange={(event) => setScopeForm({ ...scopeForm, merchantId: event.target.value })} />
+          <select aria-label="变更用户" data-testid="access-user-scope-user" value={scopeForm.userId || positionForm.userId} onChange={(event) => setScopeForm({ ...scopeForm, userId: event.target.value })}>{users.map((user) => <option key={user.id} value={user.id}>{user.username} ({user.id})</option>)}</select>
+          <input aria-label="新所属项目编号" data-testid="access-user-scope-org" placeholder="新所属项目编号（不变可空）" value={scopeForm.organizationId} onChange={(event) => setScopeForm({ ...scopeForm, organizationId: event.target.value })} />
+          <input aria-label="新所属商户编号" data-testid="access-user-scope-merchant" placeholder="新所属商户编号（平台员工留空）" value={scopeForm.merchantId} onChange={(event) => setScopeForm({ ...scopeForm, merchantId: event.target.value })} />
           <input aria-label="新范围岗位编码" data-testid="access-user-scope-position" placeholder="新范围岗位编码（沿用可空）" value={scopeForm.positionCode} onChange={(event) => setScopeForm({ ...scopeForm, positionCode: event.target.value })} />
-          <input aria-label="新范围职级 id" data-testid="access-user-scope-level" placeholder="新范围职级 id（沿用可空）" value={scopeForm.staffLevelId} onChange={(event) => setScopeForm({ ...scopeForm, staffLevelId: event.target.value })} />
+          <select aria-label="新范围职级" data-testid="access-user-scope-level" value={scopeForm.staffLevelId} onChange={(event) => setScopeForm({ ...scopeForm, staffLevelId: event.target.value })}><option value="">沿用原职级</option>{staffLevelOptions()}</select>
           <input aria-label="变更理由" data-testid="access-user-scope-reason" placeholder="变更理由" value={scopeForm.reason} onChange={(event) => setScopeForm({ ...scopeForm, reason: event.target.value })} />
           <input aria-label="主线最终审批人" data-testid="access-user-scope-final-approver" placeholder="主线最终审批人账号（可空）" value={scopeForm.finalApproverId} onChange={(event) => setScopeForm({ ...scopeForm, finalApproverId: event.target.value })} />
-          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-user-scope-change" onClick={() => void changeUserScope()}>提交范围变更（先审批后生效）</PermissionButton>
+          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-user-scope-change" disabled={writeBusy} onClick={() => void runWrite(changeUserScope, "范围变更提交失败")}>提交范围变更（先审批后生效）</PermissionButton>
         </div>
         <div className="admin-muted-action" data-testid="access-user-scope-boundary">保存所属商户或跨项目变更时，旧岗位或旧范围派生权限立即失效；新范围权限每次均须先审批，审批通过后方可生效；模板内既有高危权限不因在模板内而免审。</div>
 
         <h3>直接装配角色</h3>
         <div className="admin-inline-form" data-testid="access-role-assignment-form">
-          <select aria-label="装配用户 id" value={roleForm.userId} onChange={(event) => setRoleForm({ ...roleForm, userId: event.target.value })}>{users.map((user) => <option key={user.id} value={user.id}>{user.username} ({user.id})</option>)}</select>
-          <select aria-label="装配角色 id" value={roleForm.roleId} onChange={(event) => setRoleForm({ ...roleForm, roleId: event.target.value })}>{roles.map((role) => <option key={role.id} value={role.id}>{roleLabel(role)}</option>)}</select>
+          <select aria-label="装配用户" value={roleForm.userId} onChange={(event) => setRoleForm({ ...roleForm, userId: event.target.value })}>{users.map((user) => <option key={user.id} value={user.id}>{user.username} ({user.id})</option>)}</select>
+          <select aria-label="装配角色" value={roleForm.roleId} onChange={(event) => setRoleForm({ ...roleForm, roleId: event.target.value })}>{roles.map((role) => <option key={role.id} value={role.id}>{roleLabel(role)}</option>)}</select>
           <input aria-label="装配备注" placeholder="操作备注" value={roleForm.operatorNote} onChange={(event) => setRoleForm({ ...roleForm, operatorNote: event.target.value })} />
-          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-role-assign" onClick={() => void roleAssignment("assign")}>装配角色</PermissionButton>
-          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-role-unassign" onClick={() => void roleAssignment("unassign")}>卸载角色</PermissionButton>
+          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-role-assign" disabled={writeBusy} onClick={() => void runWrite(() => roleAssignment("assign"), "装配角色失败")}>装配角色</PermissionButton>
+          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-role-unassign" disabled={writeBusy} onClick={() => void runWrite(() => roleAssignment("unassign"), "卸载角色失败")}>卸载角色</PermissionButton>
         </div>
 
         <h3>员工单独授予 / 显式拒绝</h3>
         <div className="admin-inline-form" data-testid="access-user-override-form">
-          <select aria-label="覆盖用户 id" value={overrideForm.userId} onChange={(event) => setOverrideForm({ ...overrideForm, userId: event.target.value })}><option value="">请选择用户</option>{users.map((user) => <option key={user.id} value={user.id}>{user.username} ({user.id})</option>)}</select>
+          <select aria-label="覆盖用户" value={overrideForm.userId} onChange={(event) => setOverrideForm({ ...overrideForm, userId: event.target.value })}><option value="">请选择用户</option>{users.map((user) => <option key={user.id} value={user.id}>{user.username} ({user.id})</option>)}</select>
           <input aria-label="覆盖权限码" placeholder="权限码，逗号分隔" value={overrideForm.permissionCodes} onChange={(event) => setOverrideForm({ ...overrideForm, permissionCodes: event.target.value })} />
           <select aria-label="覆盖效果" value={overrideForm.effect} onChange={(event) => setOverrideForm({ ...overrideForm, effect: event.target.value })}>
             <option value="deny">显式拒绝</option>
             <option value="grant">单独授予</option>
           </select>
           <input aria-label="覆盖理由" placeholder="理由" value={overrideForm.reason} onChange={(event) => setOverrideForm({ ...overrideForm, reason: event.target.value })} />
-          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-user-override-set" onClick={() => void userPermissionOverride("set")}>设置</PermissionButton>
-          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-user-override-remove" onClick={() => void userPermissionOverride("remove")}>移除</PermissionButton>
+          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-user-override-set" disabled={writeBusy} onClick={() => void runWrite(() => userPermissionOverride("set"), "员工级覆盖设置失败")}>设置</PermissionButton>
+          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-user-override-remove" disabled={writeBusy} onClick={() => void runWrite(() => userPermissionOverride("remove"), "员工级覆盖移除失败")}>移除</PermissionButton>
         </div>
 
           </>) },
@@ -569,12 +608,12 @@ export function AccessGovernancePage({ api, user, setActionResult, setGlobalErro
             <thead><tr><th>申请</th><th>类型</th><th>目标</th><th>角色/岗位</th><th>权限码</th><th>状态</th><th>过期</th><th>操作</th><th>详情</th></tr></thead>
             <tbody>{grantRequests.map((row) => (
               <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`access-grant-request-${row.id}`} onClick={() => openGrantDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openGrantDetail(row); } }}>
-                <td>{row.id}</td><td>{formatBusinessStatus(row.requestType)}</td><td>{row.targetUserId}</td><td>{row.targetRoleId ?? "-"} / {zhPosition(row.positionCode)}</td><td>{row.permissionCodes?.join(", ") || "-"}</td><td>{statusBadge(row.status)}</td><td>{cnDate(row.expiresAt)}</td>
+                <td>{row.id}</td><td>{grantTypeLabel(row.requestType)}</td><td>{row.targetUserId}</td><td>{row.targetRoleId ?? "-"} / {zhPosition(row.positionCode)}</td><td>{row.permissionCodes?.join(", ") || "-"}</td><td>{statusBadge(row.status, grantStatusLabel)}</td><td>{cnDate(row.expiresAt)}</td>
                 <td onClick={(event) => event.stopPropagation()}>{(() => {
                   const acts: RowAction[] = [];
                   if (row.status === "pending") {
-                    acts.push({ key: "approve", label: "通过", permissions: GRANT_REVIEW, onClick: () => void reviewGrant(row, "approve"), testId: `access-grant-approve-${row.id}` });
-                    acts.push({ key: "reject", label: "驳回", permissions: GRANT_REVIEW, danger: true, onClick: () => void reviewGrant(row, "reject"), testId: `access-grant-reject-${row.id}` });
+                    acts.push({ key: "approve", label: "通过", permissions: GRANT_REVIEW, onClick: () => void runWrite(() => reviewGrant(row, "approve"), "授权审批失败"), testId: `access-grant-approve-${row.id}` });
+                    acts.push({ key: "reject", label: "驳回", permissions: GRANT_REVIEW, danger: true, onClick: () => void runWrite(() => reviewGrant(row, "reject"), "授权审批失败"), testId: `access-grant-reject-${row.id}` });
                   }
                   return <RowActions actions={acts} permissionsState={effectivePermissions} testId={`access-grant-actions-${row.id}`} />;
                 })()}</td>
@@ -609,7 +648,7 @@ export function AccessGovernancePage({ api, user, setActionResult, setGlobalErro
                   [risk]: { ...grantPolicy.riskReviews[risk], requiredLevel: Number(event.target.value) } } })} /></label>
             </fieldset>)}
             <label>修改理由<input data-testid="grant-policy-reason" value={grantPolicyReason} onChange={(event) => setGrantPolicyReason(event.target.value)} /></label>
-            <PermissionButton permissionsState={effectivePermissions} permissions={["admission.policy.manage"]} data-testid="access-grant-policy-save" onClick={() => void saveGrantPolicy()}>保存授权政策</PermissionButton>
+            <PermissionButton permissionsState={effectivePermissions} permissions={["admission.policy.manage"]} data-testid="access-grant-policy-save" disabled={writeBusy} onClick={() => void runWrite(saveGrantPolicy, "授权政策保存失败")}>保存授权政策</PermissionButton>
           </> : null}
         </details>
 
@@ -617,7 +656,7 @@ export function AccessGovernancePage({ api, user, setActionResult, setGlobalErro
         {summaryNotice ? <div className="admin-state" data-testid="access-grant-summary-degraded">{summaryNotice}</div> : null}
         <div className="admin-table-scroll">
           <table className="admin-table" data-testid="access-grant-summary-table">
-            <thead><tr><th>类型</th><th>对象</th><th>权限</th><th>次数</th><th>建议</th><th>详情</th><th>详情</th></tr></thead>
+            <thead><tr><th>类型</th><th>对象</th><th>权限</th><th>次数</th><th>建议</th><th>详情</th></tr></thead>
             <tbody>
               {(summary?.positionRecommendations ?? []).map((row) => <tr key={`position-${row.positionCode}-${row.permissionCode}`} className="admin-row-clickable" tabIndex={0} data-testid={`access-grant-summary-position-${row.positionCode}`} onClick={() => openSummaryPositionDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openSummaryPositionDetail(row); } }}><td>岗位</td><td>{zhPosition(row.positionCode)} / {zhLevel(row.staffLevelId)}</td><td>{row.permissionCode ?? "-"}</td><td>{row.requestCount}</td><td>{row.recommendation ?? "-"}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看岗位授权建议详情" onClick={(event) => { event.stopPropagation(); openSummaryPositionDetail(row); }}>查看详情</button></td></tr>)}
               {(summary?.userAnomalies ?? []).map((row) => <tr key={`user-${row.userId}`} className="admin-row-clickable" tabIndex={0} data-testid={`access-grant-summary-user-${row.userId}`} onClick={() => openSummaryUserDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openSummaryUserDetail(row); } }}><td>用户</td><td>{row.userId}</td><td>-</td><td>{row.requestCount}</td><td>{row.summary ?? "-"}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看用户授权异常详情" onClick={(event) => { event.stopPropagation(); openSummaryUserDetail(row); }}>查看详情</button></td></tr>)}
@@ -652,16 +691,16 @@ export function AccessGovernancePage({ api, user, setActionResult, setGlobalErro
             content: (
               <>
                 <label>岗位编码<input data-testid="access-position-role-template-position" aria-label="岗位编码" placeholder="岗位编码" value={templateForm.positionCode} onChange={(event) => setTemplateForm({ ...templateForm, positionCode: event.target.value })} /></label>
-                <label>职级 id<input data-testid="access-position-role-template-level" aria-label="职级 id" placeholder="职级 id" value={templateForm.staffLevelId} onChange={(event) => setTemplateForm({ ...templateForm, staffLevelId: event.target.value })} /></label>
-                <label>角色<select data-testid="access-position-role-template-role" aria-label="角色 id" value={templateForm.roleId} onChange={(event) => setTemplateForm({ ...templateForm, roleId: event.target.value })}><option value="">不绑定角色</option>{roles.map((role) => <option key={role.id} value={role.id}>{roleLabel(role)}</option>)}</select></label>
-                <label>权限模板 id<input data-testid="access-position-role-template-template-id" aria-label="权限模板 id" placeholder="权限模板 id" value={templateForm.templateId} onChange={(event) => setTemplateForm({ ...templateForm, templateId: event.target.value })} /></label>
+                <label>职级<select data-testid="access-position-role-template-level" aria-label="职级" value={templateForm.staffLevelId} onChange={(event) => setTemplateForm({ ...templateForm, staffLevelId: event.target.value })}>{staffLevelOptions()}</select></label>
+                <label>角色<select data-testid="access-position-role-template-role" aria-label="角色" value={templateForm.roleId} onChange={(event) => setTemplateForm({ ...templateForm, roleId: event.target.value })}><option value="">不绑定角色</option>{roles.map((role) => <option key={role.id} value={role.id}>{roleLabel(role)}</option>)}</select></label>
+                <label>权限模板编号<input data-testid="access-position-role-template-template-id" aria-label="权限模板编号" placeholder="权限模板编号" value={templateForm.templateId} onChange={(event) => setTemplateForm({ ...templateForm, templateId: event.target.value })} /></label>
               </>
             ),
           },
           {
             title: "状态",
             content: (
-              <label>模板状态<select data-testid="access-position-role-template-status" aria-label="模板状态" value={templateForm.status} onChange={(event) => setTemplateForm({ ...templateForm, status: event.target.value })}><option value="active">启用</option><option value="inactive">停用</option></select></label>
+              <label>模板状态<select data-testid="access-position-role-template-status" aria-label="模板状态" value={templateForm.status} onChange={(event) => setTemplateForm({ ...templateForm, status: event.target.value })}><option value="active">{formatBusinessStatus("active")}</option><option value="inactive">{formatBusinessStatus("inactive")}</option></select></label>
             ),
           },
         ]}
@@ -671,7 +710,7 @@ export function AccessGovernancePage({ api, user, setActionResult, setGlobalErro
           { label: "职级", value: templateForm.staffLevelId.trim() || "未填写" },
           { label: "角色", value: templateForm.roleId ? roleLabel(roles.find((r) => r.id === templateForm.roleId)) : "不绑定" },
           { label: "权限模板", value: templateForm.templateId.trim() || "—" },
-          { label: "状态", value: templateForm.status === "active" ? "启用" : "停用" },
+          { label: "状态", value: formatBusinessStatus(templateForm.status) },
         ]}
       />
       <GuidedFlowModal
@@ -713,7 +752,7 @@ export function AccessGovernancePage({ api, user, setActionResult, setGlobalErro
               <>
                 <label>申请角色<select aria-label="申请角色" value={grantForm.roleId} onChange={(event) => setGrantForm({ ...grantForm, roleId: event.target.value })}>{roles.map((role) => <option key={role.id} value={role.id}>{roleLabel(role)}</option>)}</select></label>
                 <label>申请岗位<input aria-label="申请岗位" placeholder="岗位编码" value={grantForm.positionCode} onChange={(event) => setGrantForm({ ...grantForm, positionCode: event.target.value })} /></label>
-                <label>申请职级<input aria-label="申请职级" placeholder="职级 id" value={grantForm.staffLevelId} onChange={(event) => setGrantForm({ ...grantForm, staffLevelId: event.target.value })} /></label>
+                <label>申请职级<select aria-label="申请职级" value={grantForm.staffLevelId} onChange={(event) => setGrantForm({ ...grantForm, staffLevelId: event.target.value })}>{staffLevelOptions()}</select></label>
                 <label>临时权限码<input aria-label="临时权限码" placeholder="权限码，逗号分隔" value={grantForm.permissionCodes} onChange={(event) => setGrantForm({ ...grantForm, permissionCodes: event.target.value })} /></label>
                 <label>过期时间<input aria-label="过期时间" type="datetime-local" value={grantForm.expiresAt} onChange={(event) => setGrantForm({ ...grantForm, expiresAt: event.target.value })} /></label>
                 <label><input type="checkbox" checked={grantForm.singleUse} onChange={(event) => setGrantForm({ ...grantForm, singleUse: event.target.checked })} />单次有效</label>
@@ -723,7 +762,7 @@ export function AccessGovernancePage({ api, user, setActionResult, setGlobalErro
           },
         ]}
         summary={[
-          { label: "申请类型", value: formatBusinessStatus(grantForm.requestType) },
+          { label: "申请类型", value: grantTypeLabel(grantForm.requestType) },
           { label: "目标用户", value: grantForm.targetUserId.trim() || "未选择" },
           { label: "角色", value: grantForm.requestType === "user_role" ? (grantForm.roleId ? roleLabel(roles.find((r) => r.id === grantForm.roleId)) : "未选择") : "—" },
           { label: "岗位/职级", value: grantForm.requestType === "user_position" ? `${grantForm.positionCode.trim() || "—"} / ${grantForm.staffLevelId.trim() || "—"}` : "—" },
diff --git a/apps/web/src/admin/pages/AccessUsersPage.test.tsx b/apps/web/src/admin/pages/AccessUsersPage.test.tsx
index 438888d2..ac386bfc 100644
--- a/apps/web/src/admin/pages/AccessUsersPage.test.tsx
+++ b/apps/web/src/admin/pages/AccessUsersPage.test.tsx
@@ -1,4 +1,4 @@
-// 文件版本：V0.01
+// 文件版本：V0.02
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
 import { afterEach, describe, expect, it, vi } from "vitest";
 
@@ -90,3 +90,47 @@ describe("AccessUsersPage 行明细抽屉 + 零英文", () => {
     expect(text).not.toContain("disabled");
   });
 });
+
+// M038-001（开通方式中文）、M038-009（初始状态下拉与表格同一套用词）。
+describe("M038 员工账号页回归", () => {
+  afterEach(() => vi.unstubAllGlobals());
+
+  it("账号详情的开通方式为中文：历史账号 / 后台手动录入（001）", async () => {
+    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
+      const url = String(input);
+      if (url.includes("access/users")) return Promise.resolve(envelope([
+        { id: "u-legacy", username: "老账号", account_type: "staff", status: "active", created_via: "legacy" },
+        { id: "u-manual", username: "外协甲", account_type: "outsourced_vendor", status: "active", created_via: "manual_admin" },
+      ]));
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
+    await waitFor(() => expect(screen.getByTestId("access-user-row-u-legacy")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("access-user-row-u-legacy"));
+    expect(screen.getByTestId("row-detail-drawer").textContent).toContain("历史账号");
+    expect(screen.getByTestId("row-detail-drawer").textContent).not.toContain("legacy");
+    fireEvent.keyDown(window, { key: "Escape" });
+    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
+    fireEvent.click(screen.getByTestId("access-user-row-u-manual"));
+    expect(screen.getByTestId("row-detail-drawer").textContent).toContain("后台手动录入");
+    expect(screen.getByTestId("row-detail-drawer").textContent).not.toContain("manual_admin");
+  });
+
+  it("新建外部协作账号的初始状态下拉与账号表同一套用词（009）", async () => {
+    renderPage();
+    await waitFor(() => expect(screen.getByTestId("access-user-create-open")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("access-user-create-open"));
+    fireEvent.change(screen.getByTestId("access-user-account-type"), { target: { value: "outsourced_vendor" } });
+    fireEvent.change(screen.getByTestId("access-user-name"), { target: { value: "vendor-a" } });
+    fireEvent.change(screen.getByTestId("access-user-password"), { target: { value: "init-pass" } });
+    const status = await waitFor(() => screen.getByTestId("access-user-status") as HTMLSelectElement);
+    expect(Array.from(status.options).map((option) => option.textContent)).toEqual(["有效", "待激活", "已禁用"]);
+  });
+});
diff --git a/apps/web/src/admin/pages/AccessUsersPage.tsx b/apps/web/src/admin/pages/AccessUsersPage.tsx
index a0e196a2..6555b949 100644
--- a/apps/web/src/admin/pages/AccessUsersPage.tsx
+++ b/apps/web/src/admin/pages/AccessUsersPage.tsx
@@ -1,4 +1,4 @@
-// 文件版本：V0.02
+// 文件版本：V0.03
 import React from "react";
 import { AdminDataState } from "../components/AdminDataState";
 import { AdminPageHeader } from "../components/AdminPageHeader";
@@ -15,6 +15,12 @@ function templateLabel(template: PermissionTemplateRow) {
   return template.templateName ?? template.name ?? template.id;
 }
 
+// M038-001：开通方式按后端现有取值在页内给中文（公共字典无这两个键）。
+const CREATED_VIA_LABELS: Record<string, string> = { manual_admin: "后台手动录入", legacy: "历史账号" };
+function createdViaLabel(code: string): string {
+  return CREATED_VIA_LABELS[code] ?? formatBusinessStatus(code);
+}
+
 // ponytail: ID 展示统一截尾 8 位 + title 全量；等宽由 admin-id-cell 类保证。
 function shortId(id: string | null | undefined): string {
   if (!id) return "-";
@@ -92,7 +98,7 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv
       return false;
     }
     if (!organizationId.trim()) {
-      setFormError("组织 ID 不能为空，用于数据范围和留痕。 ");
+      setFormError("所属组织不能为空，用于数据范围和留痕。 ");
       return false;
     }
     setFormError("");
@@ -179,7 +185,7 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv
         { label: "组织", value: orgDisplay(row.organizationId) },
         { label: "商户", value: merchantDisplay(row.merchantId) },
         { label: "状态", value: formatBusinessStatus(row.status ?? "active") },
-        { label: "开通方式", value: row.createdVia ? formatBusinessStatus(row.createdVia) : "—" },
+        { label: "开通方式", value: row.createdVia ? createdViaLabel(row.createdVia) : "—" },
         { label: "授权凭据", value: row.consentReference ?? "—" },
         { label: "证据附件", value: row.evidenceAttachmentIds?.length ? row.evidenceAttachmentIds.join("、") : "—" },
       ],
@@ -319,14 +325,14 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv
           {
             title: "归属与授权",
             description: accountType === "outsourced_vendor" ? "指定账号归属的组织/商户，并留存授权凭据。" : "指定员工归属的组织与商户。",
-            validate: () => (organizationId.trim() ? null : "组织 ID 不能为空，用于数据范围和留痕。"),
+            validate: () => (organizationId.trim() ? null : "所属组织不能为空，用于数据范围和留痕。"),
             content: (
               <>
-                <label>所属组织<input data-testid="access-user-org" value={organizationId} onChange={(event) => setOrganizationId(event.target.value)} placeholder="组织 ID" /></label>
+                <label>所属组织<input data-testid="access-user-org" value={organizationId} onChange={(event) => setOrganizationId(event.target.value)} placeholder="组织编号" /></label>
                 <label>所属商户<select data-testid="access-user-merchant" value={merchantId} onChange={(event) => setMerchantId(event.target.value)}><option value="">不绑定 / 平台账号</option>{merchants.map((m) => <option key={m.id} value={m.id}>{(m.displayName ?? m.companyName ?? m.id)}（{shortId(m.id)}）</option>)}</select></label>
                 {accountType === "outsourced_vendor" ? <>
                 <label>授权凭据<input data-testid="access-user-consent" value={consentReference} onChange={(event) => setConsentReference(event.target.value)} placeholder="同意/授权凭据编号（可选）" /></label>
-                <label>证据附件<input data-testid="access-user-evidence" value={evidenceAttachmentIds} onChange={(event) => setEvidenceAttachmentIds(event.target.value)} placeholder="附件 ID，逗号分隔（可选）" /></label>
+                <label>证据附件<input data-testid="access-user-evidence" value={evidenceAttachmentIds} onChange={(event) => setEvidenceAttachmentIds(event.target.value)} placeholder="附件编号，逗号分隔（可选）" /></label>
                 </> : null}
               </>
             ),
@@ -338,9 +344,7 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv
               <>
                 {accountType !== "outsourced_vendor" ? <p>注册申请提交后，请在员工档案中完成公司审批；此处不提前生成可用账号或发放权限。</p> : <>
                 <label>初始状态<select data-testid="access-user-status" value={status} onChange={(event) => setStatus(event.target.value)}>
-                  <option value="active">启用</option>
-                  <option value="invited">待激活</option>
-                  <option value="disabled">禁用</option>
+                  {["active", "invited", "disabled"].map((value) => <option key={value} value={value}>{formatBusinessStatus(value)}</option>)}
                 </select></label>
                 <label>权限模板<select data-testid="access-user-template" value={selectedTemplateId} onChange={(event) => setSelectedTemplateId(event.target.value)}>
                   <option value="">暂不赋权</option>
diff --git a/apps/web/src/admin/pages/PermissionTemplatesPage.test.tsx b/apps/web/src/admin/pages/PermissionTemplatesPage.test.tsx
index ffd51a28..266a7f57 100644
--- a/apps/web/src/admin/pages/PermissionTemplatesPage.test.tsx
+++ b/apps/web/src/admin/pages/PermissionTemplatesPage.test.tsx
@@ -92,3 +92,62 @@ describe("PermissionTemplatesPage 行明细抽屉 + 零英文", () => {
     expect(text).not.toContain("inactive");
   });
 });
+
+// M038-001（模板类型 role 中文）、M038-006（编辑保存的时间往返）。
+describe("M038 权限模板页回归", () => {
+  afterEach(() => {
+    vi.unstubAllGlobals();
+    vi.unstubAllEnvs();
+  });
+
+  const TIMED = [
+    { id: "tpl-role", template_name: "平台管理员模板", template_type: "role", status: "active", permission_codes: ["access.user.manage"],
+      valid_from: "2026-06-14T00:00:00Z", valid_until: "2026-06-15T00:00:00Z" },
+  ];
+
+  it("库内类型 role 显示为「角色模板」，编辑下拉也有这一项（001）", async () => {
+    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
+      const url = String(input);
+      if (url.includes("permission-templates")) return Promise.resolve(envelope(TIMED));
+      if (url.includes("access/permissions")) return Promise.resolve(envelope(PERMISSIONS));
+      if (url.includes("access/users")) return Promise.resolve(envelope(USERS));
+      return Promise.resolve(envelope([]));
+    }));
+    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" },
+      setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
+    render(<PermissionTemplatesPage {...props} />);
+    await waitFor(() => expect(screen.getByTestId("permission-template-row-tpl-role")).toBeTruthy());
+    const cells = screen.getByTestId("permission-template-row-tpl-role").querySelectorAll("td");
+    expect(cells[2]?.textContent).toBe("角色模板");
+    const editType = screen.getByTestId("permission-template-edit-type") as HTMLSelectElement;
+    await waitFor(() => expect(editType.value).toBe("role"));
+    expect(Array.from(editType.options).map((option) => option.textContent)).toContain("角色模板");
+  });
+
+  it("北京时间下选中带生效/截止时间的模板原样保存，发出的仍是同一时刻（006）", async () => {
+    vi.stubEnv("TZ", "Asia/Shanghai");
+    expect(new Date("2026-06-14T00:00:00Z").getHours()).toBe(8); // 时区确已生效，否则本用例无意义
+    let putBody: Record<string, unknown> | null = null;
+    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
+      const url = String(input);
+      if ((init?.method ?? "GET").toUpperCase() === "PUT" && url.includes("/permission-templates/tpl-role")) {
+        putBody = JSON.parse(String(init?.body));
+        return Promise.resolve(new Response(JSON.stringify({ success: true, code: "OK", message: "success", data: { id: "tpl-role" } }),
+          { status: 200, headers: { "content-type": "application/json" } }));
+      }
+      if (url.includes("permission-templates")) return Promise.resolve(envelope(TIMED));
+      if (url.includes("access/permissions")) return Promise.resolve(envelope(PERMISSIONS));
+      if (url.includes("access/users")) return Promise.resolve(envelope(USERS));
+      return Promise.resolve(envelope([]));
+    }));
+    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" },
+      setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
+    render(<PermissionTemplatesPage {...props} />);
+    await waitFor(() => expect((screen.getByTestId("permission-template-edit-valid-from") as HTMLInputElement).value).toBe("2026-06-14T08:00"));
+    expect((screen.getByTestId("permission-template-edit-valid-until") as HTMLInputElement).value).toBe("2026-06-15T08:00");
+    fireEvent.click(screen.getByTestId("permission-template-update"));
+    await waitFor(() => expect(putBody).toBeTruthy());
+    expect(new Date(String(putBody!.valid_from)).getTime()).toBe(new Date("2026-06-14T00:00:00Z").getTime());
+    expect(new Date(String(putBody!.valid_until)).getTime()).toBe(new Date("2026-06-15T00:00:00Z").getTime());
+  });
+});
diff --git a/apps/web/src/admin/pages/PermissionTemplatesPage.tsx b/apps/web/src/admin/pages/PermissionTemplatesPage.tsx
index 09e1db4a..47478fa3 100644
--- a/apps/web/src/admin/pages/PermissionTemplatesPage.tsx
+++ b/apps/web/src/admin/pages/PermissionTemplatesPage.tsx
@@ -15,7 +15,7 @@ const TEMPLATE_MANAGE_PERMISSION = "access.permission_template.manage";
 const TEMPLATE_APPLY_PERMISSION = "access.permission_template.apply";
 
 // 模板类型枚举中文化（base/temporary/copy 不进全局表：base 全局义为“普通会员”会串味，页内小映射）。
-const TEMPLATE_TYPE_LABELS: Record<string, string> = { base: "基础模板", temporary: "临时模板", copy: "复制模板" };
+const TEMPLATE_TYPE_LABELS: Record<string, string> = { base: "基础模板", temporary: "临时模板", copy: "复制模板", role: "角色模板" };
 function templateTypeLabel(type?: string | null) {
   const value = type ?? "base";
   return TEMPLATE_TYPE_LABELS[value] ?? value;
@@ -39,11 +39,13 @@ function displayTemplateName(row: PermissionTemplateRow) {
   return row.templateName ?? row.name ?? row.templateCode ?? row.id;
 }
 
+// M038-006：datetime-local 输入框按本地时区解释，这里也按本地时区取年月日时分，「读出→不改→保存」才是同一时刻。
 function toDateTimeLocal(value?: string | null) {
   if (!value) return "";
   const date = new Date(value);
   if (Number.isNaN(date.getTime())) return String(value).slice(0, 16);
-  return date.toISOString().slice(0, 16);
+  const pad = (part: number) => String(part).padStart(2, "0");
+  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
 }
 
 function toIsoOrUndefined(value: string) {
@@ -312,7 +314,7 @@ export function PermissionTemplatesPage({ api, setActionResult, setGlobalError,
       return false;
     }
     if (parseTargetUsers(targetUsersText).length === 0) {
-      setFormError("至少输入一个目标员工 ID。");
+      setFormError("至少输入一个目标员工编号。");
       return false;
     }
     setFormError("");
@@ -376,6 +378,7 @@ export function PermissionTemplatesPage({ api, setActionResult, setGlobalError,
               <option value="base">基础模板</option>
               <option value="temporary">临时模板</option>
               <option value="copy">复制模板</option>
+              <option value="role">角色模板</option>
             </select>
           </label>
           <label>生效时间
@@ -410,7 +413,7 @@ export function PermissionTemplatesPage({ api, setActionResult, setGlobalError,
           >
             复制模板
           </PermissionButton>
-          <label>目标员工 ID
+          <label>目标员工编号
             <input data-testid="permission-template-target-users" value={targetUsersText} onChange={(event) => setTargetUsersText(event.target.value)} placeholder="多个用逗号分隔" />
           </label>
           <HighRiskConfirm
@@ -496,6 +499,7 @@ export function PermissionTemplatesPage({ api, setActionResult, setGlobalError,
                     <option value="base">基础模板</option>
                     <option value="temporary">临时模板</option>
                     <option value="copy">复制模板</option>
+                    <option value="role">角色模板</option>
                   </select>
                 </label>
                 <label>生效时间<input data-testid="permission-template-valid-from" type="datetime-local" value={newValidFrom} onChange={(event) => setNewValidFrom(event.target.value)} /></label>
diff --git a/apps/web/src/admin/security/PermissionGate.test.ts b/apps/web/src/admin/security/PermissionGate.test.ts
index 679f9034..4b865fbf 100644
--- a/apps/web/src/admin/security/PermissionGate.test.ts
+++ b/apps/web/src/admin/security/PermissionGate.test.ts
@@ -1,7 +1,10 @@
-import { describe, expect, it } from "vitest";
+import { fireEvent, render, screen } from "@testing-library/react";
+import { createElement } from "react";
+import { describe, expect, it, vi } from "vitest";
 
 import type { EffectivePermissions, EffectivePermissionsState } from "../types";
-import { hasPermissions } from "./PermissionGate";
+import { Can, PermissionButton } from "./Can";
+import { hasPermissions, PermissionGate } from "./PermissionGate";
 
 function ready(value: Partial<EffectivePermissions>): EffectivePermissionsState {
   return {
@@ -59,3 +62,52 @@ describe("hasPermissions", () => {
     expect(hasPermissions(ready({ wildcard: true }), [])).toBe(true);
   });
 });
+
+// M038-010：权限闸组件的渲染分支（此前只测了纯函数 hasPermissions）。本文件是 .ts，用 createElement 渲染。
+describe("PermissionButton / Can / PermissionGate 组件", () => {
+  const denied = ready({ grants: ["access.role.view"] });
+  const allowed = ready({ grants: ["access.role.assign"] });
+
+  it("无权限：按钮禁用、带提示，点击不触发；有权限：点击触发", () => {
+    const onClick = vi.fn();
+    const view = render(createElement(PermissionButton, { permissionsState: denied, permissions: ["access.role.assign"], onClick, "data-testid": "gate-btn" , children: "装配角色" }));
+    const button = screen.getByTestId("gate-btn") as HTMLButtonElement;
+    expect(button.disabled).toBe(true);
+    expect(button.title).toBe("当前账号无此操作权限");
+    fireEvent.click(button);
+    expect(onClick).not.toHaveBeenCalled();
+    view.rerender(createElement(PermissionButton, { permissionsState: allowed, permissions: ["access.role.assign"], onClick, "data-testid": "gate-btn" , children: "装配角色" }));
+    expect((screen.getByTestId("gate-btn") as HTMLButtonElement).disabled).toBe(false);
+    fireEvent.click(screen.getByTestId("gate-btn"));
+    expect(onClick).toHaveBeenCalledTimes(1);
+    view.unmount();
+  });
+
+  it("显式拒绝压过授予：按钮仍禁用", () => {
+    const onClick = vi.fn();
+    const view = render(createElement(PermissionButton, { permissionsState: ready({ grants: ["access.role.assign"], denies: ["access.role.assign"] }), permissions: ["access.role.assign"], onClick, "data-testid": "gate-btn-deny" , children: "装配角色" }));
+    fireEvent.click(screen.getByTestId("gate-btn-deny"));
+    expect((screen.getByTestId("gate-btn-deny") as HTMLButtonElement).disabled).toBe(true);
+    expect(onClick).not.toHaveBeenCalled();
+    view.unmount();
+  });
+
+  it("Can：无权限渲染 fallback，有权限渲染子内容", () => {
+    const view = render(createElement(Can, { permissionsState: denied, permissions: ["access.role.assign"], fallback: "无权查看" , children: "敏感内容" }));
+    expect(view.container.textContent).toBe("无权查看");
+    view.rerender(createElement(Can, { permissionsState: allowed, permissions: ["access.role.assign"], fallback: "无权查看" , children: "敏感内容" }));
+    expect(view.container.textContent).toBe("敏感内容");
+    view.unmount();
+  });
+
+  it("PermissionGate：加载中、权限不足、放行三种渲染", () => {
+    const view = render(createElement(PermissionGate, { permissionsState: { status: "loading" } as EffectivePermissionsState, permissions: ["access.role.assign"] , children: "页面内容" }));
+    expect(screen.getByTestId("admin-permission-loading").textContent).toContain("权限加载中");
+    view.rerender(createElement(PermissionGate, { permissionsState: denied, permissions: ["access.role.assign"] , children: "页面内容" }));
+    expect(screen.getByTestId("admin-permission-denied").textContent).toContain("权限不足");
+    expect(view.container.textContent).not.toContain("页面内容");
+    view.rerender(createElement(PermissionGate, { permissionsState: allowed, permissions: ["access.role.assign"] , children: "页面内容" }));
+    expect(view.container.textContent).toBe("页面内容");
+    view.unmount();
+  });
+});
```

## 3. 内容审计（逐 ID）

- 001：治理页 `GRANT_STATUS_LABELS`（3 键＝库内状态 CHECK 闭集）、`GRANT_TYPE_LABELS`（6 键＝库内类型 CHECK 闭集），未知值再退回公共字典；表格、抽屉、两处结果提示、弹窗摘要改走它；`statusBadge` 增加可选的取字函数，岗位模板状态仍走公共字典。账号页 `CREATED_VIA_LABELS` 两键。模板页类型映射与两个下拉补 role。默认审批意见按 decision 取「通过／驳回」。七个中文与定稿一致。
- 002：aria-label／占位／字段名／校验句去掉 id、ID；`data-testid` 一个未动；测试里按旧 aria-label 取元素的一处随改。
- 003：`zhPosition` 未映射时返回「未登记岗位（码）」。
- 004：恢复按钮文案按 `row.deletedAt` 取「恢复」或「取消存档」。
- 005：`runWrite` 用 ref 挡并发、用 state 让按钮禁用；先清全局错误，执行，失败时 `setGlobalError(后端消息或兜底句)`，finally 复位。十四处按钮／行动作接入（存档、申请删除、批准、驳回、恢复、定岗、范围变更、装配、卸载、覆盖设置、覆盖移除、审批通过／驳回、政策保存），动作函数内的页面侧校验与原有 try/catch 未动。
- 006：`toDateTimeLocal` 用本地时区的年月日时分拼值；测试在 Asia/Shanghai 下先断言时区确已生效，再断言输入框显示 08:00 且原样保存发出同一时刻。
- 007：删一个重复表头；测试断言表头列数等于首行单元格数。
- 008：`STAFF_LEVEL_IDS` 由 1–10 × lower/middle/upper 生成，测试断言 30 项、首尾编号与显示文字；四处改下拉，范围变更处多一个「沿用原职级」；未调用 staff-levels 接口。
- 009：岗位模板状态下拉与摘要、账号初始状态下拉改用 `formatBusinessStatus` 取字。
- 010：`PermissionGate.test.ts` 用 createElement 渲染 PermissionButton／Can／PermissionGate 四条用例；发起授权申请的引导提交两条补进本模块已有的 guidedcreate 测试文件，未新建文件。
- 如实披露：RED 的 14 条里有 1 条是既有测试因 002 改了 aria-label 而红（同批已随改）；010 的 6 条用例在未改页面时即绿，属补测试面。全量前端单测的 3 条失败是 origin/main 基线既有，与本模块无关。
- 内容票：**PASS**。

## 4. 可执行证据原文（/home/james/audit-tmp/m038_b3r1_run.log）

```
START=2026-09-21T13:29:22Z HEAD=e456ba96 origin/main=ebb1eb99
16 个文件修前与 origin/main 逐字节相同
before tree=df21dfdfa3aaa27416857eeca14dedcd04cee631
scratch at ebb1eb99
== BASE（origin/main 原样，本模块 11 个测试文件）
VITEST_EXIT(base)=0
 Test Files  11 passed (11)
      Tests  38 passed (38)
== RED（只落测试补丁，页面未改）
VITEST_EXIT(red)=1
     × 账号详情的开通方式为中文：历史账号 / 后台手动录入（001） 20ms
     × 新建外部协作账号的初始状态下拉与账号表同一套用词（009） 317ms
     × 库内类型 role 显示为「角色模板」，编辑下拉也有这一项（001） 17ms
     × 北京时间下选中带生效/截止时间的模板原样保存，发出的仍是同一时刻（006） 1008ms
     × 员工单独授予 / 显式拒绝有写入口：显式拒绝按目标用户提交（PGM-1.10.1） 42ms
     × 授权申请的三种状态与内部类型全部中文上屏（001） 20ms
     × 审批不填意见时留痕为中文「通过」，结果提示为中文状态（001） 41ms
     × 未映射的岗位码显示为「未登记岗位（岗位码）」（003） 17ms
     × 已存档的按钮写「取消存档」、已删除的写「恢复」，不再写「停用模板」（004） 17ms
     × 装配角色被后端拒绝时页面报出原因（005） 1012ms
     × 提交未完成时再次点击不发第二笔（005） 1019ms
     × 智能总结表表头列数等于数据行单元格数（007） 37ms
     × 职级是 30 档下拉，范围变更处另有「沿用原职级」（008） 1016ms
     × 岗位模板弹窗里的状态用词与表格同一套（009） 1043ms
 Test Files  3 failed | 8 passed (11)
      Tests  14 failed | 43 passed (57)
== GREEN（再落页面补丁）
VITEST_EXIT(green)=0
 Test Files  11 passed (11)
      Tests  57 passed (57)
TSC_EXIT(green)=0
== 全量前端单测回归比较（基线取自 m038_full_base.fail：origin/main 原样的失败清单）
VITEST_EXIT(full)=1
 Test Files  2 failed | 204 passed (206)
      Tests  3 failed | 925 passed (928)
基线失败用例 3；修后失败用例 3；相对基线新增：
（以上为空即无新增）
 FAIL  src/admin/pages/TraceLinkDrilldown.test.tsx > 全链下钻 P1-A：源页引用字段成可点链接 > 我的对账合同列点击 → navigateTo(ADM-CONT-001, contractId)
 FAIL  src/admin/pages/TraceLinkP2c.test.tsx > 全链下钻 P2-C：商户门户源页引用可点 > 商户授权工作台：账单商户列点击 → navigateTo(ADM-GOV-002, merchantId)
 FAIL  src/admin/pages/TraceLinkP2c.test.tsx > 全链下钻 P2-C：商户门户源页引用可点 > 我的合同：合同号→ADM-CONT-001、空间→ADM-GOV-007 均可点
scratch 改动文件数：8
scratch dirty after cleanup: 0
scratch removed
== 主工作区落补丁
patched AccessGovernancePage.tsx 40
patched AccessUsersPage.tsx 8
patched PermissionTemplatesPage.tsx 4
role option added 2
patched pages/AccessGovernancePage.test.tsx 1 m038_test_ag.txt
patched pages/AccessGovernancePage.guidedcreate.test.tsx 2 m038_test_guided.txt
patched pages/AccessUsersPage.test.tsx 1 m038_test_au.txt
patched pages/PermissionTemplatesPage.test.tsx 0 m038_test_pt.txt
patched security/PermissionGate.test.ts 2 m038_test_gate.txt
after tree=9df1fad44caa2ef615993fe970d3efc4620aca25
frozen diff: 73515 bytes sha256 30bbbdc50244c0384c56bc9bcdecc326e55bf74d6c6d43250344fd438d34fc4b
 .../AccessGovernancePage.guidedcreate.test.tsx     |  82 +++++++++++-
 .../src/admin/pages/AccessGovernancePage.test.tsx  | 145 ++++++++++++++++++++-
 apps/web/src/admin/pages/AccessGovernancePage.tsx  | 123 +++++++++++------
 apps/web/src/admin/pages/AccessUsersPage.test.tsx  |  46 ++++++-
 apps/web/src/admin/pages/AccessUsersPage.tsx       |  22 ++--
 .../admin/pages/PermissionTemplatesPage.test.tsx   |  59 +++++++++
 .../src/admin/pages/PermissionTemplatesPage.tsx    |  12 +-
 apps/web/src/admin/security/PermissionGate.test.ts |  56 +++++++-
 8 files changed, 484 insertions(+), 61 deletions(-)
主工作区 diff 与一次性 worktree 里验证过的 diff 是否逐字节相同：same
three HTML + lock sha:
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-21T13:30:02Z
```

## 5. 结论

席甲 B4-r1 票：归因 PASS、内容 PASS。落盘前未读 Grok 本次正文；合取以 Grok 票为准。

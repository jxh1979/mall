# U-FIX 收口（合成模块：未发布员工域材料 U01–U18 的整批修复）
版本：V0.01

- 状态：PASS（B5-r2 双 PASS；B3 累计 2：B5-r1 Grok FAIL 后回退 B2 定稿 U01-016/U13-011，B3-r2 修 U01-007/U01-016/U13-011）。2026-09-10T20:13:40Z
- 范围：未发布提交 03acdb98（树 e28dad05，基线 242f4872）的 76 文件，以 main 445a49f4 为底重放并按 findings.md V1.81 中 U01–U18 定稿「发布前须修＝是」117 个 ID 整批修复（回退 B2 后加 U01-016/U13-011，共 119）；worktree `/home/james/wt/ufix-audit-20260910`（分支 audit/20260910/U-FIX）。
- 树：before 28dcfec971f6eca651213368c27ec7e893a08305（＝445a49f4 树）→ r1 after e3fd14b6e61f284e3a9dc7380d529e7486c71b8a（diff 1410429 bytes SHA256 864ef041c5874c0e1fbc00d01fd79078cb7f4c1962f63120abf9743c076119c1，85 路径）→ r2 after c0ec9b7ab6bc38f8197830b7283af4dc2356b61b（diff 11931 bytes SHA256 051fd0df6cfa899720f003651712026c830875bc019605efaccaa08da609faab，3 文件）。
- 复审：B4-r1 双 PASS；B5-r1 Grok FAIL（U01-007 未闭合）→ 回退 B2 双 PASS；B4-r2 双 PASS（modules/U-FIX-b4-r2-grok-raw.md、modules/U-FIX-b4-r2-self-raw.md）；B5-r2 双 PASS（modules/U-FIX-b5-r2-grok-raw.md V0.01，23004bytes SHAc088a4269a69475a67d0b517ef15e14a588b580514e2e83c20c589fc6208a473，整模块 PASS、modules/U-FIX-b5-r2-self-raw.md 12638bytes SHA20936460782718c09cfa6818f7cbdd2d6bfb85590fa4fdc511203cf2278f94af）。B5 后四值全等 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。
- 验证：后端全量一次性空库 3402 run，失败仅 main 既有基线五类；员工域 9 类全绿；web tsc 0 + vitest 33/33；JVM 30/30；Android compileDebugKotlin 绿（含私人手机 Keystore 自助注册）；`verify:predeploy` 全链绿。
- 操作员业务答复：排班可见天数出厂行上下限 1/31（值 7）；自营独立布尔＝删除（questions/U04-001.md）。
- 未做：「否」项（登记/main 既有归属）留给对应 main 模块；三份业务 HTML 未改。
- 发布：分支 audit/20260910/U-FIX → propose-change.sh → merge-pr.sh --merge（CI predeploy-gates 绿）；PR 见 ledger「U-FIX 发布」节。

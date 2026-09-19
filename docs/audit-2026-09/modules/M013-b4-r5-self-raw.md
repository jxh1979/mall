CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M013 B4-r5 席甲（Claude）修复内容与归因审计

版本：V0.01

修复内容：PASS。逐行归因：PASS。步骤 B4-r5；M013 IN_PROGRESS，B3 累计 5/5（3 次额度 + 操作员 2026-09-19 两次批准追加各 1 次，均已使用）；本次 Grok B4-r5 报告/CLI 正文未读，不是模块 PASS。先完整封存本票再互读。
上行锁哈希为本席在 160 工作区对 docs/audit-2026-09/plan/criteria-lock.md 用 sha256sum 实算（B3 前后各一次，与 sidecar、ledger 顶行、modules/M013-b5-r4-grok-raw.md 末条四值全等）。

## 输入与全部差异
B2 双 PASS 定稿 findings V2.36（全文件 SHA256 52a6f943c88d311c7ead45f2d334818fe82c90c4814c1f07a716da31a452498a）：仅 M013-024（细化、工程缺陷，方案 A 删括注）修复队列「是」；001–023 否。双方 b1 V0.07 B2 PASS：self SHA256 34f6f16914c8a57c7ef09993b196a317815cd8bb69beb9722de41f478ad0f3e6（76335 bytes）、Grok SHA256 0805c3240f7c159c9165e102691c918d1d74e7fdc1c3e132961bc69f2296942c（173010 bytes，实际 EXIT_CODE=0）。
before 2a13fce88ca2b0893863bf13a2b6dca7433a0319
after 2720a4e02b917ab6e88b5ba20d4912d59c007fae
两次在临时不存在的 GIT_INDEX_FILE 路径下 git add -A 与 write-tree，包含全部 tracked/staged/untracked。真实 .git/index 未动（mtime 仍 2026-09-14 18:02:05 -0700，546244 bytes）。HEAD 前后均 e456ba96c84185c8d39f18f548011daac48a6466。完整无路径过滤 git diff --binary 两树得到 1517 bytes、SHA256 824d0994ba499072dc33fb65dd6dab3f60fa86b67388d440f2524e0ebc203963，2 个 hunk，+3/-3，仅 docs/business-entity-binding-development-design-v0.1.md（git diff --stat 恰一文件）；git diff --check 退出 0。补丁脚本 /home/james/audit-tmp/m013_b3r5_patch.py apply 前 check 输出 CHECK_OK 3 edits，apply 输出 APPLIED 3 edits。diff 原样副本：/home/james/audit-tmp/m013_b3r5.diff（同 SHA256）。

after 文件（工作区实算 = after 树 blob）：
| 文件 | 行 | SHA256 | after 树 blob = git hash-object |
|---|---:|---|---|
| docs/business-entity-binding-development-design-v0.1.md | 154 | b85d4d42a6bf7bfcdba197da90cf132ed07b8012080a311c3110f97506f25143 | 583106e516a7519e3eccfaf4789a56428a38ec63 |
其他三文档未动，SHA256 前缀与 B5-r4 相同：集团 PRD 7defd0840e09bd16…、验收包 56aca4616e8c77b5…、PRD e0f00b02ab8e7e7e…。三 HTML 不变：ENUM 4396bb7112f14216…、BSM eebc5b417f8ce307…、PGM a7fe8c015c61564b…。文件无 CR 字节。

## 全部内容与归因（2 hunk 逐一）
| # | hunk | 变更行 | ID 与判断 |
|---|---|---|---|
| 1 | @@ -1,6 +1,6 | L1 标题 V0.12→V0.13；L3 「版本：V0.12」→「版本：V0.13」，最后更新仍 2026-09-19（同日）、状态文字不变 | M013-024（本文件本批版本行）；+0.01，标题与页首一致，PASS |
| 2 | @@ -46,7 +46,7 | L49 status 行删去「（归档走存档端点）」，句止于「`blacklisted` \|」，表格五列结构不变 | M013-024；与定稿方案 A 逐字一致，PASS |
完整 diff +3/-3 行均归 024，无无 ID 行、无新文件、无 HTML/代码/OpenAPI/迁移/锁/台账/其他三文档改动。3 处拟改与定稿候选逐字对应：after 文件中「归档走存档端点」0 次、「V0.13」2 次（L1/L3）、「V0.12」0 次。

## 删后自洽核查（after 文件）
- L49 只陈述服务可设五值，与 PlatformService:54–56 `SUBJECT_STATUSES` 一致；不再指向主体存档端点。
- 同文差距登记仍在且不矛盾：L55（M013-023 status 四值契约缺口披露）、L57「当前 `status` 未实现该完整词表和缓冲期闭环」、L108「完整存档/删除流程未在当前接口落地」、L121「也不存在主体完整 CRUD」。
- PlatformController 对 business-entities 的映射仍仅 3 处（GET / POST / POST projects，只读 grep）；本批未改代码。

## 三线
第一性原理：删掉的是把商户口径的存档入口写到主体档案字段表上的不可复现声明；删后 §3.1 status 行与源码一致，归档差距由 L57/L108 承载，接入方不会再去调不存在的接口。
顺从式：GEN-016 版本可验证（+0.01，L1=L3=V0.13，同日不改日期）；GEN-002 入口声称与当前源一致；GEN-001 同文自洽恢复；GEN-017 四分类按定稿（细化）；不增判据、不 LOCK-AMEND；本文件无变更日志段，不新造（与 018/022 先例一致）。
对抗式：未改走方案 B 扩写；未重开 023（L55 披露句未触碰）；未借本批改 001–023（diff 仅 3 行）；未把删括注写成「主体存档已实现」或「HTML 不再要求存档」；before/after 全 diff 仅一文件，未掩藏其他改动；真实索引、HEAD、三 HTML 指纹未变。

## 实际验证
SSH 在 160 工作区：sha256sum after 文件 + git rev-parse after:path 与 git hash-object 相等；grep 计数（括注 0、V0.13 2、V0.12 0）；grep 差距句 L55/L57/L108/L121 存在；CR 字节 0；git diff --check 退出 0；git diff --stat 仅一文件；.git/index mtime、HEAD、三 HTML sha256、其他三文档 sha256 与 B3 前一致；PlatformController business-entities 映射计数 3。不是 Java/HTTP/数据库/浏览器测试，未运行这些测试；纯文档修改按文档类豁免新增测试。

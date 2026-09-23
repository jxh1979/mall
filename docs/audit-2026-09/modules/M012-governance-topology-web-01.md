# M012 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／网页与验证／第1段
版本：V0.03

## 当前收口：2026-09-23 B5-r5双PASS
状态：PASS。6文件2129行（冻结1485不变），21映射，B3累计5。先独立封存self再读Grok，完整三线审计无新缺陷。
- [Codex完整报告](M012-b5-r5-self-raw.md)：V0.01，SHA256 1c0cec252ab4928a429fc42297708dd0089860744e0b10bee9749b41a7d36355。
- [Grok完整报告](M012-b5-r5-grok-raw.md)：V0.01，真实exit0，SHA256 7208f01dbfeeba215f21066b99e082df0a1a844f5db165d99aa976aa2e3acf10。
- B5四值全等 b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458；LOCK-AMEND-COUNT1。
- 当前001—019队列全否：18已修，009登记。006/007操作员“删除”已落实，无待问。
本次获准仅018下钻反馈及019删除理由提示和对应测试：2文件6hunk，48增4删。before c218cfb17edcfdf278a96cf9b3c6e5b32192fd2e → after 3eca41eb9aa21c6791ef70d5695deb940abeb992；diff SHA256 794a06ffc17723e33a55f27ddb66a0d6f295c051bf70c573a297c83c2ef241f1。
B4采用self V0.01/Grok V0.02有效双PASS；Grok V0.01 /tmp落点违规票保留但无效，不采用其测试证据。
本席先红4/20通过，修复后正本三文件59通过（19/24/16）、typecheck0；Grok B5正本59通过/typecheck0。无真实浏览器/HTTP/Maven/数据库测试声称。
HTML、后台、权限、共享组件未改。不新增跨页定位器、附件、绑定创建、项目分配、项目三资料表单或导出；009观察保留。
发布入场：以fresh origin/main隔离分支，仅发布M012已审六源及证据；含历史010—017未发布修复。按用户要求执行提交、推送、CI后合并，实际结果以GitHub PR和操作回执为准。此文件为发布前已审快照，不将计划写成已合并。

## 历史收口：2026-09-14 B5-r4双PASS
状态：PASS。471模块计划M012，6文件当前2085行（冻结1485不改），21项判据。Codex与Grok同步独立全审，本方完整封存后才读取对方报告。
- [Codex完整报告](M012-b5-r4-self-raw.md)：V0.01，16483 bytes，SHA256 77fee0c3cbf8dde23aac320d86a584e965f351278a3547baabfb56c0f9fd2380。
- [Grok完整报告](M012-b5-r4-grok-raw.md)：V0.01，24075 bytes，SHA256 aad62cd0e208d32ee616a5eeabeb32d1fb1924553c408e5058ac2fc64d9db81e；真实exit0。
- B5后实际锁、sidecar、ledger、Grok四值全等 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a；LOCK-AMEND-COUNT=1。已先写ledger V3.98为PASS再更新本文件。
- 当前001–017队列全否：16已修，009仅登记；B3累计4（历史3+操作员本次额外批准1），原FAIL/轮次完整保留。

本次追加修复仅017：删除拓扑/组织页两条错误AuditTrailHint及仅相关未用导入，组织auditTraceField真实入口保留；2文件4hunk，3增6删，版本V0.04/V0.05。before 4a0fbc92485454d7627280a48205fd259f76a883 → after f2366e35ef257fbc349cbbc5e00e0052cc2b389c，完整diff3261bytes SHA a5bf79d44d317de12d11b21192ab7203b0de4f164ccd5db1602e973b3174d714，B4-r4双内容/归因PASS。此前010–016的关系、分页、中文、测试、错误恢复、键盘、字典修复由本次B5全审确认保留。

本会话r4实跑56项单测通过（19/21/16）、typecheck exit0，完整命令输出在[B4-r4本方报告](M012-b4-r4-self-raw.md)。本次B5核六源与after tree一致后引用；无真实浏览器/HTTP/后端/数据库验收，jsdom提示保留。
Grok独立撤回“501项目”反例：当前/projects经currentProject后按projects.id主键过滤，0或1条；与可501条的当前项目商户/空间/主体不同，不因此重开013。017错误提示与实际日志不符的原问题现已修复。

边界：009不补附件上传、绑定创建、项目分配、后端三项项目资料的页面输入；不放宽历史NULL项目数据隔离；mock200不代表财税审批放行。HTML、锁、后台、共享组件均未因本次追加修复改动；未提交/合并/推送/部署。

## 历史收口记录（原V0.01，保留旧时点事实）

状态：PASS。范围：plan/modules.md V0.04 的 6 文件（GovernanceTopologyPage.tsx/.test.tsx、OrganizationMerchantPage.tsx/.test.tsx、SubjectBindingsPage.tsx/.test.tsx；计划冻结 1485 行）；判据 21 项（BSM-001、BSM-003、GEN-001–018、PGM-001）。B3 整批修复累计 1 次。M012-001–008 已修复（006/007 为添加项，操作员 2.2 答删除）；009 仅登记；当前 9 项修复队列全否。

B5-r1 按 Grok 真实退出→席甲（Claude Fable 5.1，作者自审票）主仓实跑并独立落盘→读取 Grok 顺序完成，两席全模块三线 PASS：
- [席甲完整报告](M012-b5-r1-self-raw.md)：V0.01，37091 bytes，SHA256 dc6b8c9e05d8a2b48b57132705f37ebefd3b3ffba3198d46af6de934f6d8da88。
- [Grok完整报告](M012-b5-r1-grok-raw.md)：V0.01，37143 bytes，SHA256 da44e88ab62a4c59d3faf7d094adcffa868c164ecde7a4beb16393d2332031f7。
- B5 后 actual/sidecar/ledger/精确本次 Grok raw 四值全等：b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。已先写 ledger V2.41 为 PASS，再创建本文件；LOCK-AMEND-COUNT 仍 1。

修复摘要（只改本模块 6 个前端文件；后台、types.ts、statusLabels.ts、路由未改）：四类主体绑定页改读后台 BusinessEntityBindingRecord 真实字段并补三态「变更后状态」下拉与必填 action，原因不再预填、不再提交假证据附件（001/002/006）；拓扑页「已挂接」按 projectId 计数，项目业态、空间类型（V184 八码中文）、空间状态（可用/不可用）与绑定表头全中文（003/004）；组织与商户页去掉验收预填与隐藏提交的假主体 id/面积/租金/状态/验收备注/假证据（三主体由四类主体绑定流程承接），联系电话改非空校验，存档/删除写路径失败可见（006/007/008）；测试夹具改为现行闭集并补真实字段、提交、存档、删除申请、失败可见用例（005/008）。

正本三 HTML/锁/权限种子/后台未修改，未提交/合并/推送/部署。

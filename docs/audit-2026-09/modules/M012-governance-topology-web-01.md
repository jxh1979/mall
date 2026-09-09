# M012 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／网页与验证／第1段
版本：V0.01

状态：PASS。范围：plan/modules.md V0.04 的 6 文件（GovernanceTopologyPage.tsx/.test.tsx、OrganizationMerchantPage.tsx/.test.tsx、SubjectBindingsPage.tsx/.test.tsx；计划冻结 1485 行）；判据 21 项（BSM-001、BSM-003、GEN-001–018、PGM-001）。B3 整批修复累计 1 次。M012-001–008 已修复（006/007 为添加项，操作员 2.2 答删除）；009 仅登记；当前 9 项修复队列全否。

B5-r1 按 Grok 真实退出→席甲（Claude Fable 5.1，作者自审票）主仓实跑并独立落盘→读取 Grok 顺序完成，两席全模块三线 PASS：
- [席甲完整报告](M012-b5-r1-self-raw.md)：V0.01，37091 bytes，SHA256 dc6b8c9e05d8a2b48b57132705f37ebefd3b3ffba3198d46af6de934f6d8da88。
- [Grok完整报告](M012-b5-r1-grok-raw.md)：V0.01，37143 bytes，SHA256 da44e88ab62a4c59d3faf7d094adcffa868c164ecde7a4beb16393d2332031f7。
- B5 后 actual/sidecar/ledger/精确本次 Grok raw 四值全等：b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。已先写 ledger V2.41 为 PASS，再创建本文件；LOCK-AMEND-COUNT 仍 1。

修复摘要（只改本模块 6 个前端文件；后台、types.ts、statusLabels.ts、路由未改）：四类主体绑定页改读后台 BusinessEntityBindingRecord 真实字段并补三态「变更后状态」下拉与必填 action，原因不再预填、不再提交假证据附件（001/002/006）；拓扑页「已挂接」按 projectId 计数，项目业态、空间类型（V184 八码中文）、空间状态（可用/不可用）与绑定表头全中文（003/004）；组织与商户页去掉验收预填与隐藏提交的假主体 id/面积/租金/状态/验收备注/假证据（三主体由四类主体绑定流程承接），联系电话改非空校验，存档/删除写路径失败可见（006/007/008）；测试夹具改为现行闭集并补真实字段、提交、存档、删除申请、失败可见用例（005/008）。

正本三 HTML/锁/权限种子/后台未修改，未提交/合并/推送/部署。

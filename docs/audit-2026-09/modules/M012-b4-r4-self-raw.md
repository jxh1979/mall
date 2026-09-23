CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M012 B4-r4 Codex 完整修复内容与归因审计
文件版本：V0.01
内容结论：PASS
归因结论：PASS
最终结论：PASS（仅本席；不是B4双PASS或模块PASS）
步骤B4-r4；M012/471；IN_PROGRESS；B3累计4（历史3+操作员本次明确批准再1次），不擅自增加第5次。
本席先派Grok并独立审完本次全部diff才封存。封存前未读本次M012-b4-r4-grok-raw.md或CLI原始JSON。固定Codex/Grok两席，未用MCP或任何第三代理。requesting-code-review按V0.17收窄为只派Grok、完整两tree作基线；verification-before-completion用本次实跑和实算。

## 1. 定稿输入与范围
触发回退：B5-r3双方完整FAIL；操作员最新“批准”解封并允许再修1次。完整B2双PASS：self b1 V0.06 SHA e62a4d6c42f5f4c711b0bcc7aba0b5016d24289678b77efdb97e7eddbe43ac68；Grok b1 V0.06 SHA fc54e0e239b68b7f35b641ac6e3a30f4b17213fe75943210fd11f2df5a787885，实际exit0。双方原始前缀保留且无B2新独占锁行。
定稿findings V2.22 SHA44a6be37c6ef5b0d85dba26039d25bc857d5b564844eaf423bf430e6741ca9a9：仅M012-017是。Grok已独立明确撤回501项目反例、013保持否；本次不得为013增加项目分页。001–016其他NO及009观察保留。
017边界：删除拓扑/组织的错误AuditTrailHint两个节点及仅因此未用的导入；组织auditTraceField保留；两源文件版本递增。纯证据文字删除，用机械受限diff+现有56/tsc；不新增同构测试。
真实正本main，HEAD e456ba96c84185c8d39f18f548011daac48a6466。当前六文件2085行（计划冻结1485不改）。无HTML、后台、共享组件、types、路由、数据库、测试、PRD修改。

## 2. 前后全树与完整diff
before=4a0fbc92485454d7627280a48205fd259f76a883
after=f2366e35ef257fbc349cbbc5e00e0052cc2b389c
命令：git diff --binary 4a0fbc92485454d7627280a48205fd259f76a883 f2366e35ef257fbc349cbbc5e00e0052cc2b389c
实际3261bytes；SHA256 a5bf79d44d317de12d11b21192ab7203b0de4f164ccd5db1602e973b3174d714；2文件4hunk；git diff --check0。临时索引git add -A/write-tree覆盖所有tracked/staged/untracked，真实index SHA 1657ef91568fc6c00e7fd66f58f4c00461260651323d2ecbf616238602b973a2未变。
before之前相对上次after9b1e71aa9eef993d24abdb1c7d8b2d4d8c0e1f2f只有审计产物变化；本次两tree差异仅以下两个源码。未按路径过滤遗漏diff。
B3前后四锁均取触发回退B5-r3 Grok原报告；实算/sidecar/ledger/触发Grok全等b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a，LOCK-AMEND-COUNT1；本席首行另独立实算。不是取尚未生成的本轮B4票填第四值。

完整diff（逐字保存）：
```diff
diff --git a/apps/web/src/admin/pages/GovernanceTopologyPage.tsx b/apps/web/src/admin/pages/GovernanceTopologyPage.tsx
index d7a39b9c..89af88eb 100644
--- a/apps/web/src/admin/pages/GovernanceTopologyPage.tsx
+++ b/apps/web/src/admin/pages/GovernanceTopologyPage.tsx
@@ -1,9 +1,8 @@
-// 文件版本：V0.03
+// 文件版本：V0.04
 import React from "react";
 import { TechDetails } from "../components/TechDetails";
 import { AdminDataState } from "../components/AdminDataState";
 import { AdminPageHeader } from "../components/AdminPageHeader";
-import { AuditTrailHint } from "../components/AuditTrailHint";
 import { useAdminResource } from "../hooks/useAdminResource";
 import { useServerPagedRows } from "../hooks/useServerPagedRows";
 import { TablePager } from "../components/TablePager";
@@ -242,7 +241,6 @@ export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, nav
     <section data-testid="governance-topology-page">
       <AdminPageHeader title={TEXT.title} description={TEXT.description} />
       <TechDetails><div className="admin-generic-evidence" data-testid="governance-topology-evidence">{TEXT.evidence}</div></TechDetails>
-      <AuditTrailHint event="governance_topology.viewed" />
       <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
         {entityRows.total === 0 && merchantRows.total === 0 && projectRows.total === 0 && spaceRows.total === 0 ? (
           <div className="admin-state admin-empty" data-testid="governance-topology-empty">暂无治理拓扑数据</div>
diff --git a/apps/web/src/admin/pages/OrganizationMerchantPage.tsx b/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
index a2cb2099..0255d620 100644
--- a/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
+++ b/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
@@ -1,9 +1,9 @@
-// 文件版本：V0.04
+// 文件版本：V0.05
 import React from "react";
 import { TechDetails } from "../components/TechDetails";
 import { AdminDataState } from "../components/AdminDataState";
 import { AdminPageHeader } from "../components/AdminPageHeader";
-import { AuditTrailHint, auditTraceField } from "../components/AuditTrailHint";
+import { auditTraceField } from "../components/AuditTrailHint";
 import { useServerPagedRows } from "../hooks/useServerPagedRows";
 import { TablePager } from "../components/TablePager";
 import { hasPermissions } from "../security/PermissionGate";
@@ -416,7 +416,6 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
         <div className="admin-state" data-testid="organization-merchant-focus">已从下钻定位到商户：{focusObjectId}（在下表中查看其档案与归属）</div>
       )}
       <TechDetails><div className="admin-generic-evidence" data-testid="organization-merchant-evidence">{TEXT.evidence}</div></TechDetails>
-      <AuditTrailHint event="organization.viewed / merchant_profile.created" />
       {formError ? <div className="admin-inline-error" data-testid="organization-merchant-form-error">{formError}</div> : null}
       <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
         {entityRows.total === 0 && merchantRows.total === 0 && projectRows.total === 0 ? (
```

## 3. 全部4hunk逐块归因（无未归属行）
|块|文件/位置|全部改动|正式ID/理由|
|---|---|---|---|
|1|Topology -1,9 +1,8|V0.03→V0.04；删除AuditTrailHint单独导入|017；节点删除导致该唯一导入未用，版本铁律随同一修复|
|2|Topology -242,7 +241,6|删除governance_topology.viewed错误提示节点|017；真实后台不记录该同名事件，只删错误证据文案|
|3|Organization -1,9 +1,9|V0.04→V0.05；组合导入删除AuditTrailHint，保留auditTraceField|017；按定稿只清孤立标识，不删除真正对象审计函数|
|4|Organization -416,7 +416,6|删除organization.viewed / merchant_profile.created错误提示节点|017；实际商户创建merchant.updated，不新增事件迎合旧文字|

diff每一新增/删除行均归017；纯上下文行未改。无越界；归因PASS。没有应回滚的未知ID行。

## 4. 内容核验
两页原Hint只有event属性，未传navigateTo/businessType/businessId；组件仅渲染折叠说明，未发请求或写审计。删除节点不会删除实际留痕，也没有删掉该节点承载的真实操作链接。
Topology已无AuditTrailHint导入/节点，原TechDetails接口证据、PageHeader、AdminDataState、分页/权限/字典/键盘逻辑保留；只去一条错误事件描述。
Organization只保留import { auditTraceField }，商户抽屉auditTraceField(navigateTo,"merchant",row.id)仍在，原接口证据、权限/创建/存档/删除/分页/键盘/错误恢复逻辑保留。
SubjectBindingsPage两个实际正确事件提示未动；四个其他模块文件（含三测试）哈希逐一仍旧值。
当前代码完全等价于对before两文件依次执行定稿三条替换（版本、导入、节点）；静态校验两个<AuditTrailHint不存在，组织真审计调用存在，两页TechDetails+evidence存在。所有56既有断言保持目的及字节，类型编译实际通过。
分类仍细化，冻结BSM-001/PGM-001溯源上位，GEN-011/016；没有新增业务、状态、角色、收费、接口或判据。013本批反例已B2由Grok撤回，实际WHERE id=:project_id最多一个项目，不通过本次改动“补”它。

## 5. 本次实际验证（当前相同六源）
命令一：cd /home/james/complex-management/apps/web && npm run test:unit -- GovernanceTopologyPage.test.tsx OrganizationMerchantPage.test.tsx SubjectBindingsPage.test.tsx
输出仅去终端颜色码，正文及警告保留：
```text

> apps-web@0.1.0 test:unit
> vitest run GovernanceTopologyPage.test.tsx OrganizationMerchantPage.test.tsx SubjectBindingsPage.test.tsx


 RUN  v4.1.9 /home/james/complex-management/apps/web

Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
 ✓ src/admin/pages/SubjectBindingsPage.test.tsx (16 tests) 434ms
 ✓ src/admin/pages/GovernanceTopologyPage.test.tsx (19 tests) 500ms
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's getComputedStyle() method: with pseudo-elements
Not implemented: Window's getComputedStyle() method: with pseudo-elements
 ✓ src/admin/pages/OrganizationMerchantPage.test.tsx (21 tests) 2613ms
     ✓ 分页保留商户归档和业态筛选参数，后页商户可达  1117ms
     ✓ 存档最后一页的唯一商户后回到仍有记录的第一页  644ms

 Test Files  3 passed (3)
      Tests  56 passed (56)
   Start at  16:27:33
   Duration  5.10s (transform 4.46s, setup 145ms, import 5.11s, tests 3.55s, environment 844ms)


ACTUAL_EXIT 0
```
命令二：cd /home/james/complex-management/apps/web && npm run typecheck
```text

> apps-web@0.1.0 typecheck
> tsc -b --noEmit


ACTUAL_EXIT 0
```
两命令真实exit0。56测试=拓扑19+组织21+绑定16。jsdom scrollTo/getComputedStyle提示是测试环境限制，未称浏览器验证；未跑真实HTTP/后台/Maven/数据库。纯文字删除不新写同构单测符合B2，两处最低可复现检查为精确diff及现有断言。
没有重复B3-r3四条内存重试探针：错误处理代码和测试在本次均未改；旧证据不冒称本次再次执行。

## 6. 状态
本席内容PASS/归因PASS。M012仍IN_PROGRESS，017仍是；须收齐Grok本次完整报告才合取/转否，然后B5-r4整模块三线复审。未把B4或56绿写成模块PASS。无添加项、HTML申请或未答questions；本次获批修复额度已经使用。
Grok真实exit本席封存时待定，指定modules/M012-b4-r4-grok-raw.md，完整实际CLI命令见ledger V3.93派发段；不代Grok填结论。未提交/合并/推送/部署。

## 7. 当前六源指纹
{
  "apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx": {
    "sha256": "985196ad40864145f1be814cad1f5db6667883a114aed27e61453188219a28cb",
    "bytes": 16571,
    "lines": 308,
    "version": "// 文件版本：V0.04"
  },
  "apps/web/src/admin/pages/GovernanceTopologyPage.tsx": {
    "sha256": "6338225cb0367502fefb3db2348673fe602ee9e6bed3cdae321feeea9a9b7036",
    "bytes": 18046,
    "lines": 274,
    "version": "// 文件版本：V0.04"
  },
  "apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx": {
    "sha256": "d40c83d948e0b07f83f6eb5bcbb2a18af12b186ddd93fdcc651557d5870408a7",
    "bytes": 26133,
    "lines": 435,
    "version": "// 文件版本：V0.07"
  },
  "apps/web/src/admin/pages/OrganizationMerchantPage.tsx": {
    "sha256": "d3ddb2cc423cc901190e8a5d9c5c6f2e4095919d956a19a6c866124947fa6722",
    "bytes": 32061,
    "lines": 520,
    "version": "// 文件版本：V0.05"
  },
  "apps/web/src/admin/pages/SubjectBindingsPage.test.tsx": {
    "sha256": "ec22a9250ed24f61031f1997119b5537d77d1af537e388eff2018fa947d8591b",
    "bytes": 15482,
    "lines": 291,
    "version": "// 文件版本：V0.04"
  },
  "apps/web/src/admin/pages/SubjectBindingsPage.tsx": {
    "sha256": "94f4d4dcfcb24c85701450794544574efe26f209a4aed242ff5c428af98d247c",
    "bytes": 14784,
    "lines": 257,
    "version": "// 文件版本：V0.03"
  }
}

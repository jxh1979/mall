CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M012 B4-r2 Codex 修复内容与归因全量审计
报告自身版本：V0.01。当前模块IN_PROGRESS，B3累计2（历史1+本次1），B4期间未改源码、测试、HTML或判据。Grok先派发，完整本席报告封存前未读取本次Grok报告。
## 结论
内容审计 **FAIL**；闸3逐块归因 **PASS**；本席B4合取 **FAIL**。全部48个hunk审完才出本票。唯一未通过：M012-013分页改造删除原onStart清全局错误动作，三页重试恢复后仍残留顶部错误。其余定稿010/011/012/014/015/016修复面本席未发现未完成。仍须收齐Grok完整B4后按整批处置，未提前修或转B5。
## 冻结输入与身份
before=0c7e7d157c2c8aa3a3580886baf01d8405993c8a
after=d06253fa9ef48efd4abf368911981f95d518fc8b
完整git diff --binary两树：79044bytes，SHA256 ae56e18496e6c54b10c4e156d6625d7779a21e288ca02159b650ef4d7395e6f8。不是HEAD diff、不是口头范围、未做路径过滤。暂存区SHA2561657ef91568fc6c00e7fd66f58f4c00461260651323d2ecbf616238602b973a2前后保持，HEAD e456ba96c84185c8d39f18f548011daac48a6466。
定稿findings.md V2.17 SHA256 c6d43896195c7fde3f7c308adb5d3dd3782fd9ed66856936cb5c35866d079d42；顶端M012当前010–016七项是、009与历史001–008否。B2输入V2.16 SHA256e802cf09b96b397334b933ccad13d6b2fc7a7bbc6fc499270e363eab1e20df54，self/Grok b1 raw末尾V0.05双PASS，不改该定稿。判据V0.11，M012冻结21项未增，LOCK-AMEND-COUNT=1。
## 六文件与全部diff覆盖
- apps/web/src/admin/pages/GovernanceTopologyPage.tsx：264行，17898bytes，// 文件版本：V0.02，SHA256 40346c8ae8c6d37e605d43ef6c369949eb85ba607623501769aca2cb74a2c105
- apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx：285行，15388bytes，// 文件版本：V0.03，SHA256 a268b2bbad009a1e708d67e554b866bc5628af8e99ee66077b8c9b4713769bc0
- apps/web/src/admin/pages/OrganizationMerchantPage.tsx：512行，32007bytes，// 文件版本：V0.03，SHA256 3d29e2b84c0e41aef67de3b9c1ed9c5b6cb3b014be70e84fcd99f93951d17a69
- apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx：417行，25309bytes，// 文件版本：V0.06，SHA256 351d46052daa56051c2165800cfd572afb74d3ab4d042bf111e5c8e4b29e3fca
- apps/web/src/admin/pages/SubjectBindingsPage.tsx：254行，14734bytes，// 文件版本：V0.02，SHA256 4a4e307987061b550d9539a2c47f0347488d231c11d99b63358f00a7013ae3e2
- apps/web/src/admin/pages/SubjectBindingsPage.test.tsx：273行，14649bytes，// 文件版本：V0.03，SHA256 c19d5c8e788f0b223ac50d758c20a8200a2a4e5c844dbbb2476070119492f318
共2005行。全部diff末节原样收录。修改只有六文件；共享hook/组件/全局标签/路由/types/后台/HTML与全部其它工作区内容不在两树差异中，不曾顺修。B3内每次内容写入均递增文件版本，测试多次修改故版本可前进多位；B3按整批1次计算，不以写入数计轮次。
## 内容审计
010：拓扑按Merchant.projectId正连Project.id；组织商户表和抽屉呈现所属项目，撤掉项目反属单个商户的列及抽屉字段，删除因此孤立的TraceLink import。真实项目merchantId可空测试仍显示项目，旧NULL历史隔离保持。PASS。
011：两页夹具有project_id，BindingRow不造字段；组织缺主体权限不请求该端点、绑定409财税拒绝在表单/全局可见并保留原因。响应mock走真实AdminApi，未mock被测页。PASS。
012：导语按定稿精确删除幂等键/附件必带的过度声称，状态与原因要求是已有表单；无新上传器、无HTML修改。逐字核对PASS，不为纯文案另写实现镜像测试。
013：各列表getPaged保留total，TablePager用page=2重新请求，useServerPagedRows现成共享源码未改；禁止fetchall/提高限额已遵守。拓扑商户/空间501、关联主体第二页、组织筛选参数切换回第一页/后页保留、存档末页唯一商户后回有效第一页、绑定501去重标本页，均实跑通过。项目GET现行只返回当前项目0或1条，拓扑不新增多项目切换器。派生统计本页/信封total区分明确；关联主体缺页写未加载，无权限仍不请求。**但删除onStart清全局错误使普通重试恢复退化，FAIL，详情下节。**
014：项目按草稿/待启用/已启用/暂停/终止/归档，商户与主体按已有合法态，绑定active/inactive/suspended。局部删除“已封禁”错义，未知不直出英文，未改全局客人封禁含义。三页及抽屉同一formatter，项目active/暂停、商户待准入/暂停/黑名单、绑定暂停实跑。PASS。
015：三页所有tr快捷键收窄target=currentTarget，原生子控件不被父行阻止；实际user-event空格切第二条绑定、Enter商户下钻/存档以及行自身Enter开明细均通过。原点击/Esc与审批/存档理由用例保持。PASS。
016：仅读既有space-types字典、路由已有governance.space.view；真实中文typeName优先，八码兼容，未识别用中文，既有kiosk/一体机位反例转绿。无新类型规则/字典管理修改。PASS。
## 唯一FAIL依据：CX-B4-01 → 沿用仍在队列的 M012-013
- 位置：GovernanceTopologyPage.tsx:117–126（原资源onStart消失）；OrganizationMerchantPage.tsx:136–157（新getPaged/hooks与reload）；SubjectBindingsPage.tsx:75–79与原useAdminResource删除块。共享useServerPagedRows只清自己的error，未调用setGlobalError(null)；当前三页reload同样不清全局。
- 正常触发：首次GET临时503，显示读取失败；用户点击既有“重试”，服务恢复200，三页表格均成功显示，但宿主顶部错误仍保留“服务暂时不可用，请稍后重试”。这是一条已有错误恢复主路径，不是两三种偶发组合。
- 上位/冻结：GEN-002/003/009，原013已定稿要求分页后错误反馈保留；分类细化，有工程缺陷，当前队列仍是。不是新业务发明，不新建ID；本次未验收不得先将013改否。
- 宿主事实：AdminApp.tsx:406保存globalError；:720按该值渲染data-testid=global-error；:730把setGlobalError传页面。局部hook error清掉不会自动清宿主值。
- 可复现实跑：相同内存探针在当前after三页3失败、53跳过，NODE_EXIT=1；只把三份生产页面用git show before树的原文注入同一测试运行（测试helper与探针相同）后3通过、53跳过，NODE_EXIT=0；六文件SHA逐个保持。探针真正先等待503错误，再点击admin-retry，再等待表格恢复，最后检查setGlobalError的最后调用应为null；当前恢复期间0调用，baseline正确清除。
- 首次探针曾把503显示断言写成后端message“临时读取失败”，实际AdminApi会规范化为“服务暂时不可用，请稍后重试”，故第一次两个版本都在前提失败。已修正探针前提后再对比，上述3红/3绿才是有效证据；没有改业务源码/测试文件，不把第一次前提失败当回归证据。
- 最小修复建议（只建议，本B4不修）：在三个页面现有请求/重试开始位置恢复原清错行为，保留失败时真实新错误；保留服务器分页和已有错误展示。不改共享hook/API/宿主，不吞错误；对应三个现有测试文件补此次恢复用例。本席不以53绿掩盖这项。
## B3与B4真实验证
B3按TDD技能执行：当前安装superpowers test-driven-development/SKILL.md与writing-good-tests.md已读；先业务输入反例与回归红，再改实现。初次51例为17失败34过；组织新增键盘用例最初选了并不存在的来源按钮，按真实UI改为存档按钮后实际Enter不触发prompt，仍红。该误选不当作有效生产缺陷证据。其后行为修好，3处旧横幅未加“本页”期望按013纠正；附加筛选/末页存档用例须等待异步加载，纠正2处同步读取测试等待。最终53/53通过，TSC最终exit0。所有原37用例的行为目的保留，旧错误词义/反连关系断言按定稿修正。
实际命令：cwd apps/web，npm run test:unit -- src/admin/pages/GovernanceTopologyPage.test.tsx src/admin/pages/OrganizationMerchantPage.test.tsx src/admin/pages/SubjectBindingsPage.test.tsx；53过，18/20/15，3.47s，exit0。npm run typecheck → tsc -b --noEmit，exit0。git diff --check before after →0。
jsdom未实现scrollTo/getComputedStyle的既有提示如实保留，不称输出无警告。不新增依赖。未运行真实浏览器、真实HTTP、后台测试/迁移/部署，不声称相关验证。B4额外只读内存探针是上述新发现的依据，未重复无关全仓测试。
## 逐hunk定稿ID归因
以下48hunk覆盖完整diff，混合hunk每种实际修改都有列明ID；版本行随该文件定稿修复递增，user-event仅为015已安装依赖，测试响应入口服务列明用例，均不独立创造业务。空白随新增测试段局部产生，无相邻重构。
|文件|hunk（两树原始范围）|定稿ID|归因票|
|---|---|---|---|
|GovernanceTopologyPage.test.tsx|-1,5 +1,6|M012-010 / M012-011 / M012-013 / M012-014 / M012-015 / M012-016|PASS|
|GovernanceTopologyPage.test.tsx|-7,28 +8,30|M012-010 / M012-011 / M012-013 / M012-016|PASS|
|GovernanceTopologyPage.test.tsx|-41,15 +44,17|M012-011 / M012-013 / M012-015 / M012-016|PASS|
|GovernanceTopologyPage.test.tsx|-98,12 +103,12|M012-014|PASS|
|GovernanceTopologyPage.test.tsx|-149,7 +154,7|M012-014|PASS|
|GovernanceTopologyPage.test.tsx|-218,3 +223,63|M012-010 / M012-011 / M012-013 / M012-015 / M012-016|PASS|
|GovernanceTopologyPage.tsx|-1,31 +1,42|M012-010 / M012-013 / M012-014 / M012-016|PASS|
|GovernanceTopologyPage.tsx|-56,10 +67,26|M012-010 / M012-013 / M012-016|PASS|
|GovernanceTopologyPage.tsx|-87,24 +114,14|M012-013 / M012-016|PASS|
|GovernanceTopologyPage.tsx|-126,17 +143,17|M012-010 / M012-013 / M012-014|PASS|
|GovernanceTopologyPage.tsx|-149,7 +166,7|M012-016|PASS|
|GovernanceTopologyPage.tsx|-163,7 +180,7|M012-010|PASS|
|GovernanceTopologyPage.tsx|-172,18 +189,20|M012-013 / M012-014 / M012-015|PASS|
|GovernanceTopologyPage.tsx|-197,14 +216,15|M012-013 / M012-015 / M012-016|PASS|
|GovernanceTopologyPage.tsx|-212,7 +232,7|M012-013|PASS|
|GovernanceTopologyPage.tsx|-225,10 +245,10|M012-013|PASS|
|OrganizationMerchantPage.test.tsx|-1,5 +1,6|M012-010 / M012-011 / M012-013 / M012-014 / M012-015|PASS|
|OrganizationMerchantPage.test.tsx|-12,27 +13,29|M012-010 / M012-011 / M012-013|PASS|
|OrganizationMerchantPage.test.tsx|-46,13 +49,14|M012-011 / M012-013 / M012-015|PASS|
|OrganizationMerchantPage.test.tsx|-60,7 +64,7|M012-013|PASS|
|OrganizationMerchantPage.test.tsx|-139,7 +143,8|M012-010 / M012-011|PASS|
|OrganizationMerchantPage.test.tsx|-329,3 +334,84|M012-011 / M012-013 / M012-014 / M012-015|PASS|
|OrganizationMerchantPage.tsx|-1,19 +1,29|M012-010 / M012-013 / M012-014|PASS|
|OrganizationMerchantPage.tsx|-69,14 +79,11|M012-013|PASS|
|OrganizationMerchantPage.tsx|-126,22 +133,28|M012-010 / M012-013|PASS|
|OrganizationMerchantPage.tsx|-198,7 +211,7|M012-013|PASS|
|OrganizationMerchantPage.tsx|-272,23 +285,28|M012-010 / M012-014|PASS|
|OrganizationMerchantPage.tsx|-300,9 +318,8|M012-010 / M012-014|PASS|
|OrganizationMerchantPage.tsx|-313,8 +330,9|M012-013 / M012-014 / M012-015|PASS|
|OrganizationMerchantPage.tsx|-359,9 +377,10|M012-010 / M012-013 / M012-014 / M012-015|PASS|
|OrganizationMerchantPage.tsx|-371,14 +390,15|M012-010 / M012-013 / M012-014 / M012-015|PASS|
|OrganizationMerchantPage.tsx|-390,7 +410,7|M012-013|PASS|
|OrganizationMerchantPage.tsx|-406,21 +426,21|M012-013|PASS|
|SubjectBindingsPage.test.tsx|-1,5 +1,6|M012-011 / M012-013 / M012-014 / M012-015|PASS|
|SubjectBindingsPage.test.tsx|-14,9 +15,9|M012-013|PASS|
|SubjectBindingsPage.test.tsx|-28,10 +29,13|M012-011 / M012-013 / M012-014|PASS|
|SubjectBindingsPage.test.tsx|-41,13 +45,14|M012-011 / M012-013 / M012-014|PASS|
|SubjectBindingsPage.test.tsx|-71,7 +76,7|M012-013|PASS|
|SubjectBindingsPage.test.tsx|-215,3 +220,54|M012-011 / M012-013 / M012-014 / M012-015|PASS|
|SubjectBindingsPage.tsx|-1,14 +1,14|M012-013 / M012-014|PASS|
|SubjectBindingsPage.tsx|-35,9 +35,14|M012-012 / M012-014|PASS|
|SubjectBindingsPage.tsx|-50,7 +55,7|M012-013|PASS|
|SubjectBindingsPage.tsx|-67,7 +72,11|M012-013|PASS|
|SubjectBindingsPage.tsx|-86,7 +95,7|M012-014|PASS|
|SubjectBindingsPage.tsx|-121,18 +130,11|M012-013|PASS|
|SubjectBindingsPage.tsx|-186,7 +188,7|M012-013|PASS|
|SubjectBindingsPage.tsx|-196,10 +198,10|M012-013 / M012-014|PASS|
|SubjectBindingsPage.tsx|-229,18 +231,19|M012-013 / M012-014 / M012-015|PASS|

归因PASS只证明修改在已定稿范围内，不证明内容正确；013仍有遗漏所以内容FAIL。Grok若归因FAIL仍有否决权，等待完整外审票。
## 可复现内存探针（未落测试文件）
下面Node从stdin运行，Vite pre-transform只在内存追加三个探针。M012_PROBE_TREE=after用当前页面，before用git show before树原页面；相同当前测试helpers。Python从当前正本apps/web用subprocess运行node --input-type=module；实际输出紧随。

```javascript
import {startVitest} from "vitest/node";
import {execFileSync} from "node:child_process";
const probes={"GovernanceTopologyPage.test.tsx":"\nit(\"M012 B4只读：重试成功后清除全局读取错误\", async () => {\n  let failed = false;\n  const props = renderPage((url, init) => {\n    if (init?.method !== \"POST\" && url.includes(\"/merchants\") && !failed) {\n      failed = true;\n      return new Response(JSON.stringify({ success: false, code: \"TEMPORARY_ERROR\", message: \"临时读取失败\" }), { status: 503, headers: { \"content-type\": \"application/json\" } });\n    }\n    return undefined;\n  });\n  await screen.findByTestId(\"admin-retry\");\n  expect(props.setGlobalError).toHaveBeenCalledWith(\"服务暂时不可用，请稍后重试\");\n  vi.mocked(props.setGlobalError).mockClear();\n  fireEvent.click(screen.getByTestId(\"admin-retry\"));\n  await screen.findByTestId(\"governance-topology-table\");\n  expect(props.setGlobalError).toHaveBeenLastCalledWith(null);\n});\n","OrganizationMerchantPage.test.tsx":"\nit(\"M012 B4只读：重试成功后清除全局读取错误\", async () => {\n  let failed = false;\n  const props = renderPage((url, init) => {\n    if (init?.method !== \"POST\" && url.includes(\"/merchants\") && !failed) {\n      failed = true;\n      return new Response(JSON.stringify({ success: false, code: \"TEMPORARY_ERROR\", message: \"临时读取失败\" }), { status: 503, headers: { \"content-type\": \"application/json\" } });\n    }\n    return undefined;\n  });\n  await screen.findByTestId(\"admin-retry\");\n  expect(props.setGlobalError).toHaveBeenCalledWith(\"服务暂时不可用，请稍后重试\");\n  vi.mocked(props.setGlobalError).mockClear();\n  fireEvent.click(screen.getByTestId(\"admin-retry\"));\n  await screen.findByTestId(\"organization-merchants-table\");\n  expect(props.setGlobalError).toHaveBeenLastCalledWith(null);\n});\n","SubjectBindingsPage.test.tsx":"\nit(\"M012 B4只读：重试成功后清除全局读取错误\", async () => {\n  let failed = false;\n  const props = renderPage((url, init) => {\n    if (init?.method !== \"POST\" && url.includes(\"/business-entity-bindings\") && !failed) {\n      failed = true;\n      return new Response(JSON.stringify({ success: false, code: \"TEMPORARY_ERROR\", message: \"临时读取失败\" }), { status: 503, headers: { \"content-type\": \"application/json\" } });\n    }\n    return undefined;\n  });\n  await screen.findByTestId(\"admin-retry\");\n  expect(props.setGlobalError).toHaveBeenCalledWith(\"服务暂时不可用，请稍后重试\");\n  vi.mocked(props.setGlobalError).mockClear();\n  fireEvent.click(screen.getByTestId(\"admin-retry\"));\n  await screen.findByTestId(\"subject-bindings-table\");\n  expect(props.setGlobalError).toHaveBeenLastCalledWith(null);\n});\n"};
const mode=process.env.M012_PROBE_TREE;
const paths=["src/admin/pages/GovernanceTopologyPage.test.tsx","src/admin/pages/OrganizationMerchantPage.test.tsx","src/admin/pages/SubjectBindingsPage.test.tsx"];
const production=["GovernanceTopologyPage.tsx","OrganizationMerchantPage.tsx","SubjectBindingsPage.tsx"];
const ctx=await startVitest("test",paths,{watch:false,testNamePattern:"M012 B4只读：重试成功后"},{plugins:[{name:"m012-readonly-retry",enforce:"pre",transform(code,id){
 const name=id.split("?")[0].split("/").at(-1);
 if(probes[name])return {code:code+"\n"+probes[name],map:null};
 if(mode==="before"&&production.includes(name))return {code:execFileSync("git",["show","0c7e7d157c2c8aa3a3580886baf01d8405993c8a:apps/web/src/admin/pages/"+name],{cwd:"/home/james/complex-management",encoding:"utf8"}),map:null};
 }}]});await ctx.close();
```

```text
MODE=after

 RUN  v4.1.9 /home/james/complex-management/apps/web

 ❯ src/admin/pages/SubjectBindingsPage.test.tsx (16 tests | 1 failed | 15 skipped) 42ms
   × M012 B4只读：重试成功后清除全局读取错误 41ms
 ❯ src/admin/pages/GovernanceTopologyPage.test.tsx (19 tests | 1 failed | 18 skipped) 44ms
   × M012 B4只读：重试成功后清除全局读取错误 43ms
 ❯ src/admin/pages/OrganizationMerchantPage.test.tsx (21 tests | 1 failed | 20 skipped) 196ms
   × M012 B4只读：重试成功后清除全局读取错误 195ms

 Test Files  3 failed (3)
      Tests  3 failed | 53 skipped (56)
   Start at  13:48:40
   Duration  1.19s (transform 243ms, setup 136ms, import 820ms, tests 282ms, environment 810ms)


⎯⎯⎯⎯⎯⎯⎯ Failed Tests 3 ⎯⎯⎯⎯⎯⎯⎯

 FAIL  src/admin/pages/GovernanceTopologyPage.test.tsx > M012 B4只读：重试成功后清除全局读取错误
AssertionError: expected last "vi.fn()" call to have been called with [ null ]

- Expected:
[
  null,
]

+ Received:
undefined

 ❯ src/admin/pages/GovernanceTopologyPage.test.tsx:302:32
    283|   });
    284|
    285| });
       |    ^
    286|

⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯[1/3]⎯

 FAIL  src/admin/pages/OrganizationMerchantPage.test.tsx > M012 B4只读：重试成功后清除全局读取错误
AssertionError: expected last "vi.fn()" call to have been called with [ null ]

- Expected:
[
  null,
]

+ Received:
undefined

 ❯ src/admin/pages/OrganizationMerchantPage.test.tsx:434:32
    415|   });
    416|
    417| });
       |    ^
    418|

⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯[2/3]⎯

 FAIL  src/admin/pages/SubjectBindingsPage.test.tsx > M012 B4只读：重试成功后清除全局读取错误
AssertionError: expected last "vi.fn()" call to have been called with [ null ]

- Expected:
[
  null,
]

+ Received:
undefined

 ❯ src/admin/pages/SubjectBindingsPage.test.tsx:290:32
    271|     expect(screen.getByTestId("subject-bindings-form-error").textConte…
    272|   });
    273| });
       |    ^
    274|

⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯[3/3]⎯


NODE_EXIT=1
MODE=before

 RUN  v4.1.9 /home/james/complex-management/apps/web

 ✓ src/admin/pages/GovernanceTopologyPage.test.tsx (19 tests | 18 skipped) 36ms
 ✓ src/admin/pages/SubjectBindingsPage.test.tsx (16 tests | 15 skipped) 40ms
 ✓ src/admin/pages/OrganizationMerchantPage.test.tsx (21 tests | 20 skipped) 191ms

 Test Files  3 passed (3)
      Tests  3 passed | 53 skipped (56)
   Start at  13:48:42
   Duration  1.26s (transform 474ms, setup 162ms, import 1.05s, tests 267ms, environment 812ms)


NODE_EXIT=0
SIX_FILES_UNCHANGED=True

```

## 本轮完整未经筛选的二树diff
```diff
diff --git a/apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx b/apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx
index fba79864..62368337 100644
--- a/apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx
+++ b/apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx
@@ -1,5 +1,6 @@
-// 文件版本：V0.01
+// 文件版本：V0.03
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
+import userEvent from "@testing-library/user-event";
 import { afterEach, describe, expect, it, vi } from "vitest";
 
 import { GovernanceTopologyPage } from "./GovernanceTopologyPage";
@@ -7,28 +8,30 @@ import { AdminApi } from "../client/adminClient";
 import type { AdminPageProps, EffectivePermissionsState } from "../types";
 
 // mock 走真实 AdminApi（stub fetch）→ unwrap 转 camelCase → 组件。platform 域 snake_case 响应。
-// M012-005：夹具只用现行闭集——主体角色 platform/project_company/management_company（V410），空间类型走 V184 字典 8 码。
+// M012-005：夹具只用现行闭集——主体角色 platform/project_company/management_company（V410），空间类型使用现行字典。
 const ENTITIES = [{ id: "org-1", entity_name: "平台经营主体", unified_social_credit_code: "ORG-1", entity_role: "management_company", status: "active" }];
 const MERCHANTS = [
-  { id: "m-1", business_entity_id: "org-1", merchant_name: "商户甲", status: "active" },
-  { id: "m-2", business_entity_id: "org-1", merchant_name: "商户乙", status: "suspended" },
+  { id: "m-1", project_id: "p-1", business_entity_id: "org-1", merchant_name: "商户甲", status: "active" },
+  { id: "m-2", project_id: "p-1", business_entity_id: "org-1", merchant_name: "商户乙", status: "suspended" },
 ];
-const PROJECTS = [{ id: "p-1", name: "项目一", code: "PJ-1", merchant_id: "m-1", venue_type: "bar", status: "active" }];
+const PROJECTS = [{ id: "p-1", name: "项目一", code: "PJ-1", merchant_id: null, venue_type: "bar", status: "active" }];
 // M012-003：后台 SpaceRecord 以 project_id 挂接项目（无 venue_id）。
 const SPACES = [{ id: "s-1", space_name: "空间甲", project_id: "p-1", floor_no: "L1", area_square_meter: 120, space_type: "shop", status: "active" }];
 
-function envelope(items: unknown[]): Response {
+function envelope(items: unknown[], total = items.length, page = 1): Response {
   return new Response(
-    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
+    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page, page_size: 500, total } }),
     { status: 200, headers: { "content-type": "application/json" } },
   );
 }
 
-function makeApi(): AdminApi {
+function makeApi(respond?: (url: string, init?: RequestInit) => Response | undefined): AdminApi {
   vi.stubGlobal(
     "fetch",
-    vi.fn().mockImplementation((input: RequestInfo | URL) => {
+    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
       const url = String(input);
+      const response = respond?.(url, init);
+      if (response) return Promise.resolve(response);
       if (url.includes("business-entities")) return Promise.resolve(envelope(ENTITIES));
       if (url.includes("merchants")) return Promise.resolve(envelope(MERCHANTS));
       if (url.includes("projects")) return Promise.resolve(envelope(PROJECTS));
@@ -41,15 +44,17 @@ function makeApi(): AdminApi {
 
 const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
 
-function renderPage() {
+function renderPage(respond?: (url: string, init?: RequestInit) => Response | undefined, overrides: Partial<AdminPageProps> = {}) {
   const fullProps: AdminPageProps = {
-    api: makeApi(),
+    api: makeApi(respond),
     user: { id: "u1", username: "admin", accountType: "platform_admin" },
     setActionResult: vi.fn(),
     setGlobalError: vi.fn(),
     effectivePermissions: ADMIN_PERMS,
+    ...overrides,
   };
   render(<GovernanceTopologyPage {...fullProps} />);
+  return fullProps;
 }
 
 describe("GovernanceTopologyPage KPI 聚焦视图", () => {
@@ -98,12 +103,12 @@ describe("GovernanceTopologyPage KPI 聚焦视图", () => {
     expect(screen.queryByTestId("governance-topology-table")).toBeNull();
   });
 
-  it("渲染输出状态列为中文，不含原英文枚举 active/suspended", async () => {
-    renderPage();
+  it.each([["active", "已启用"], ["suspended", "暂停"]])("项目状态%s显示为%s", async (status, label) => {
+    renderPage((url) => url.includes("/projects") ? envelope([{ ...PROJECTS[0], status }]) : undefined);
     await waitFor(() => expect(screen.getByTestId("governance-topology-table")).toBeTruthy());
     const text = screen.getByTestId("governance-topology-table").textContent ?? "";
-    expect(text).toContain("有效");
-    expect(text).toContain("已封禁");
+    expect(text).toContain(label);
+    expect(text).not.toContain("已封禁");
     expect(text).not.toContain("suspended");
   });
 });
@@ -149,7 +154,7 @@ describe("GovernanceTopologyPage 行明细抽屉（拓扑表 / 空间表整行
     expect(drawer.textContent).toContain("平台经营主体");
     expect(drawer.textContent).toContain("商户甲");
     expect(drawer.textContent).toContain("项目一");
-    expect(drawer.textContent).toContain("有效");
+    expect(drawer.textContent).toContain("已启用");
     expect(drawer.textContent).toContain("酒吧");
     expect(drawer.textContent).not.toContain("active");
   });
@@ -218,3 +223,63 @@ describe("GovernanceTopologyPage 无 entity.binding.view 角色不请求主体
     expect(setGlobalError).not.toHaveBeenCalledWith(expect.any(String));
   });
 });
+
+
+describe("M012 本次归并回归", () => {
+  afterEach(() => { vi.unstubAllGlobals(); window.history.replaceState({}, "", "/admin/governance/topology"); });
+
+  it("商户按真实 project_id 归属当前综合体，项目没有反向 merchant_id 也能显示", async () => {
+    renderPage();
+    expect((await screen.findByTestId("governance-topology-row-m-2-p-1")).textContent).toContain("项目一");
+  });
+
+  it("现有自定义空间类型按字典中文名称展示", async () => {
+    renderPage((url) => url.includes("/space-types")
+      ? envelope([{ id: "type-kiosk", type_code: "kiosk", type_name: "一体机位", status: "active" }])
+      : url.includes("/spaces") ? envelope([{ ...SPACES[0], space_type: "kiosk" }]) : undefined);
+    const row = await screen.findByTestId("governance-space-row-s-1");
+    expect(row.textContent).toContain("一体机位");
+    expect(row.textContent).not.toContain("kiosk");
+  });
+
+  it.each(["merchants", "spaces"])("超过默认500条的%s能看到总数并读取第二页", async (resource) => {
+    const rows = Array.from({ length: 501 }, (_, i) => resource === "merchants"
+      ? { ...MERCHANTS[0], id: "m-page-" + i, merchant_name: "分页商户" + i }
+      : { ...SPACES[0], id: "s-page-" + i, space_name: "分页空间" + i });
+    renderPage((url) => {
+      if (!url.includes("/" + resource)) return undefined;
+      const page = Number(new URL(url, "http://localhost").searchParams.get("page") || 1);
+      return envelope(rows.slice((page - 1) * 500, page * 500), 501, page);
+    });
+    await waitFor(() => expect(screen.getByTestId("governance-topology-kpi-" + resource).textContent).toBe("501"));
+    fireEvent.click(screen.getByTestId("governance-" + resource + "-pager-next"));
+    const last = resource === "merchants" ? "governance-topology-row-m-page-500-p-1" : "governance-space-row-s-page-500";
+    expect(await screen.findByTestId(last)).toBeTruthy();
+  });
+
+  it("商户来源按钮可以用Enter下钻，行自身Enter仍打开详情", async () => {
+    const navigateTo = vi.fn();
+    renderPage(undefined, { navigateTo });
+    const button = await screen.findByTestId("trace-link-ADM-GOV-002-m-1");
+    button.focus();
+    await userEvent.setup().keyboard("{Enter}");
+    expect(navigateTo).toHaveBeenCalledWith("ADM-GOV-002", "m-1");
+    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
+    fireEvent.keyDown(screen.getByTestId("governance-topology-row-m-1-p-1"), { key: "Enter" });
+    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
+  });
+  it("主体资料在第二页时显示未加载，翻页后呈现真实主体", async () => {
+    const rows = Array.from({ length: 501 }, (_, i) => ({ ...ENTITIES[0], id: "org-page-" + i, entity_name: "经营主体" + i }));
+    renderPage((url) => {
+      if (url.includes("/merchants")) return envelope([{ ...MERCHANTS[0], business_entity_id: "org-page-500" }]);
+      if (!url.includes("/business-entities")) return undefined;
+      const page = Number(new URL(url, "http://localhost").searchParams.get("page") || 1);
+      return envelope(rows.slice((page - 1) * 500, page * 500), 501, page);
+    });
+    const row = await screen.findByTestId("governance-topology-row-m-1-p-1");
+    expect(row.textContent).toContain("主体资料未加载");
+    fireEvent.click(screen.getByTestId("governance-entities-pager-next"));
+    await waitFor(() => expect(screen.getByTestId("governance-topology-row-m-1-p-1").textContent).toContain("经营主体500"));
+  });
+
+});
diff --git a/apps/web/src/admin/pages/GovernanceTopologyPage.tsx b/apps/web/src/admin/pages/GovernanceTopologyPage.tsx
index 1c363f47..ad3b372b 100644
--- a/apps/web/src/admin/pages/GovernanceTopologyPage.tsx
+++ b/apps/web/src/admin/pages/GovernanceTopologyPage.tsx
@@ -1,31 +1,42 @@
-// 文件版本：V0.01
+// 文件版本：V0.02
 import React from "react";
 import { TechDetails } from "../components/TechDetails";
 import { AdminDataState } from "../components/AdminDataState";
 import { AdminPageHeader } from "../components/AdminPageHeader";
 import { AuditTrailHint } from "../components/AuditTrailHint";
 import { useAdminResource } from "../hooks/useAdminResource";
+import { useServerPagedRows } from "../hooks/useServerPagedRows";
+import { TablePager } from "../components/TablePager";
 import { hasPermissions } from "../security/PermissionGate";
 import type { AdminPageProps, BusinessEntityRow, GovernanceProjectRow, GovernanceSpaceRow, MerchantAdmissionRow } from "../types";
 import { formatBusinessStatus } from "../i18n/statusLabels";
 import { TraceLink } from "../components/TraceLink";
 import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
 
-// M012-004：空间类型字典 8 码（V184 space_type_dictionary 种子，type_name 中文）；PGM-001 零英文，页面不直出 type_code。
+type MerchantRow = MerchantAdmissionRow & { projectId?: string | null };
+type SpaceTypeRow = { typeCode: string; typeName: string };
+const PROJECT_STATUS_LABELS: Record<string, string> = { draft: "草稿", pending_activation: "待启用", active: "已启用", suspended: "暂停", terminated: "终止", archived: "归档" };
+const ORGANIZATION_STATUS_LABELS: Record<string, string> = { pending_admission: "待准入", active: "正常", suspended: "暂停", terminated: "终止", archived: "归档", blacklisted: "黑名单" };
+function formatTopologyStatus(project: GovernanceProjectRow | undefined, merchant: MerchantRow) {
+  const code = project?.status ?? merchant.status;
+  return code ? (project ? PROJECT_STATUS_LABELS : ORGANIZATION_STATUS_LABELS)[code] ?? "未识别状态" : "—";
+}
+
+// M012-016：字典显示名优先；保留原有中文种子作为兼容显示。
 const SPACE_TYPE_LABELS: Record<string, string> = { building: "楼栋", floor: "楼层", zone: "区块/区域", shop: "商铺/铺位", public_area: "公共区域", parking: "车位/车库", booth: "摊位/展位", other: "其他" };
-function formatSpaceType(code: string | null | undefined) {
-  return code ? SPACE_TYPE_LABELS[code] ?? formatBusinessStatus(code) : "—";
+function formatSpaceType(code: string | null | undefined, types: SpaceTypeRow[]) {
+  return code ? types.find((type) => type.typeCode === code)?.typeName || SPACE_TYPE_LABELS[code] || "未识别类型" : "—";
 }
 // M012-004：空间状态闭集 active/unavailable（M009-036）页内映射「可用/不可用」，不直出英文。
 const SPACE_STATUS_LABELS: Record<string, string> = { active: "可用", unavailable: "不可用" };
 function formatSpaceStatus(code: string | null | undefined) {
-  return code ? SPACE_STATUS_LABELS[code] ?? formatBusinessStatus(code) : "—";
+  return code ? SPACE_STATUS_LABELS[code] ?? "未识别状态" : "—";
 }
 
 const TEXT = {
   title: "集团/综合体拓扑",
   description: "展示集团、综合体、商户、项目和空间点位之间的治理关系，作为权限、合同、结算和运营责任归属的入口。",
-  evidence: "接口证据：读取 /api/v1/platform/business-entities、/api/v1/platform/merchants、/api/v1/platform/projects、/api/v1/platform/spaces。",
+  evidence: "接口证据：读取 /api/v1/platform/business-entities、/api/v1/platform/merchants、/api/v1/platform/projects、/api/v1/platform/spaces、/api/v1/platform/space-types。",
   boundary: "集团、综合体、商户、项目和空间点位必须用同一套拓扑口径承接权限、合同、结算和审计；拓扑页只展示关系，不做主体变更。",
   loadFailed: "治理拓扑读取失败",
   focusClear: "清除筛选，返回全部",
@@ -56,10 +67,26 @@ function kpiFocusTitle(focus: KpiFocus): string {
 }
 
 export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, navigateTo, effectivePermissions }: AdminPageProps) {
-  const [entities, setEntities] = React.useState<BusinessEntityRow[]>([]);
-  const [merchants, setMerchants] = React.useState<MerchantAdmissionRow[]>([]);
-  const [projects, setProjects] = React.useState<GovernanceProjectRow[]>([]);
-  const [spaces, setSpaces] = React.useState<GovernanceSpaceRow[]>([]);
+  const canEntityView = hasPermissions(effectivePermissions, ["entity.binding.view"]);
+  const entityRows = useServerPagedRows(
+    (page, size) => canEntityView
+      ? api.getPaged<BusinessEntityRow>("/api/v1/platform/business-entities?page=" + page + "&size=" + size)
+      : Promise.resolve({ items: [] as BusinessEntityRow[], total: 0, page: 1, size }),
+    500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
+  const merchantRows = useServerPagedRows(
+    (page, size) => api.getPaged<MerchantRow>("/api/v1/platform/merchants?page=" + page + "&size=" + size),
+    500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
+  const projectRows = useServerPagedRows(
+    (page, size) => api.getPaged<GovernanceProjectRow>("/api/v1/platform/projects?page=" + page + "&size=" + size),
+    500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
+  const spaceRows = useServerPagedRows(
+    (page, size) => api.getPaged<GovernanceSpaceRow>("/api/v1/platform/spaces?page=" + page + "&size=" + size),
+    500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
+  const entities = entityRows.rows;
+  const merchants = merchantRows.rows;
+  const projects = projectRows.rows;
+  const spaces = spaceRows.rows;
+  const [spaceTypes, setSpaceTypes] = React.useState<SpaceTypeRow[]>([]);
   const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
   // 行明细抽屉：拓扑关系表 / 空间点位表整行可点 → 该行完整字段（枚举中文·商户可溯源）。
   const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
@@ -87,24 +114,14 @@ export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, nav
     (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
   }
 
-  const { loading, error, reload } = useAdminResource(
-    async () => {
-      // 跨域副资源不请求（根因修）：主体端点需 entity.binding.view，本页 gate 不含之——
-      // 缺该权限角色 403 会 reject 整个 Promise.all 致整页白屏；缺权限则不请求、主体层降级为空。
-      const canEntityView = hasPermissions(effectivePermissions, ["entity.binding.view"]);
-      const [nextEntities, nextMerchants, nextProjects, nextSpaces] = await Promise.all([
-        canEntityView ? api.get<BusinessEntityRow[]>("/api/v1/platform/business-entities") : Promise.resolve([] as BusinessEntityRow[]),
-        api.get<MerchantAdmissionRow[]>("/api/v1/platform/merchants"),
-        api.get<GovernanceProjectRow[]>("/api/v1/platform/projects"),
-        api.get<GovernanceSpaceRow[]>("/api/v1/platform/spaces"),
-      ]);
-      setEntities(nextEntities);
-      setMerchants(nextMerchants);
-      setProjects(nextProjects);
-      setSpaces(nextSpaces);
-    },
-    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
+  const dictionary = useAdminResource(
+    async () => setSpaceTypes(await api.get<SpaceTypeRow[]>("/api/v1/platform/space-types")),
+    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed },
   );
+  const resources = [entityRows, merchantRows, projectRows, spaceRows, dictionary];
+  const loading = resources.some((resource) => resource.loading);
+  const error = resources.find((resource) => resource.error)?.error ?? null;
+  const reload = () => resources.forEach((resource) => { void resource.reload(); });
 
   const projectIds = new Set(projects.map((project) => project.id));
   // M012-003：后台 SpaceRecord 以 projectId 挂接项目（无 venueId；types.ts 的 venueId 为旧臆造字段）。
@@ -126,17 +143,17 @@ export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, nav
   const isFocused = (merchantId?: string, projectId?: string) =>
     !!focusObjectId && (focusObjectId === merchantId || focusObjectId === projectId);
 
-  function openTopologyDetail(entity: BusinessEntityRow | undefined, merchant: MerchantAdmissionRow, project: GovernanceProjectRow | undefined) {
+  function openTopologyDetail(entity: BusinessEntityRow | undefined, merchant: MerchantRow, project: GovernanceProjectRow | undefined) {
     setDetail({
       title: `拓扑关系 · ${display(merchant.merchantName)}`,
       fields: [
-        { label: "经营主体", value: display(entity?.entityName) },
+        { label: "经营主体", value: entity ? display(entity.entityName) : merchant.businessEntityId ? "主体资料未加载" : "-" },
         { label: "统一社会信用代码", value: display(entity?.unifiedSocialCreditCode) },
         { label: "商户", value: merchant.id ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={merchant.id}>{display(merchant.merchantName)}</TraceLink> : display(merchant.merchantName) },
         { label: "项目", value: display(project?.name) },
         { label: "项目编码", value: display(project?.code) },
         { label: "业态", value: formatBusinessStatus(project?.venueType) },
-        { label: "状态", value: formatBusinessStatus(project?.status ?? merchant.status) },
+        { label: "状态", value: formatTopologyStatus(project, merchant) },
       ],
     });
   }
@@ -149,7 +166,7 @@ export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, nav
         { label: "所属项目", value: display(project?.name ?? space.projectId) },
         { label: "楼层", value: display(space.floorNo) },
         { label: "面积(㎡)", value: display(space.areaSquareMeter) },
-        { label: "空间类型", value: formatSpaceType(space.spaceType) },
+        { label: "空间类型", value: formatSpaceType(space.spaceType, spaceTypes) },
         { label: "状态", value: formatSpaceStatus(space.status) },
       ],
     });
@@ -163,7 +180,7 @@ export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, nav
           <thead><tr><th>经营主体</th><th>统一社会信用代码</th><th>商户</th><th>项目</th><th>项目编码</th><th>业态</th><th>状态</th><th>详情</th></tr></thead>
           <tbody>{merchants.map((merchant) => {
             const entity = entities.find((item) => item.id === merchant.businessEntityId);
-            const merchantProjects = projects.filter((project) => project.merchantId === merchant.id);
+            const merchantProjects = projects.filter((project) => project.id === merchant.projectId);
             const rows = merchantProjects.length ? merchantProjects : [undefined];
             return rows.map((project, index) => (
               <tr key={`${merchant.id}-${project?.id ?? "no-project"}-${index}`}
@@ -172,18 +189,20 @@ export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, nav
                 className={`admin-row-clickable${isFocused(merchant.id, project?.id) ? " admin-row-selected" : ""}`}
                 tabIndex={0}
                 onClick={() => openTopologyDetail(entity, merchant, project)}
-                onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openTopologyDetail(entity, merchant, project); } }}>
-                <td>{display(entity?.entityName)}</td>
+                onKeyDown={(event) => { if (event.target === event.currentTarget && (event.key === "Enter" || event.key === " ")) { event.preventDefault(); openTopologyDetail(entity, merchant, project); } }}>
+                <td>{entity ? display(entity.entityName) : merchant.businessEntityId ? "主体资料未加载" : "-"}</td>
                 <td>{display(entity?.unifiedSocialCreditCode)}</td>
                 <td onClick={(event) => event.stopPropagation()}>{merchant.id ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={merchant.id}>{display(merchant.merchantName)}</TraceLink> : display(merchant.merchantName)}</td>
                 <td>{display(project?.name)}</td>
                 <td>{display(project?.code)}</td>
                 <td>{formatBusinessStatus(project?.venueType)}</td>
-                <td>{formatBusinessStatus(project?.status ?? merchant.status)}</td>
+                <td>{formatTopologyStatus(project, merchant)}</td>
               <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看拓扑关系详情" onClick={(event) => { event.stopPropagation(); openTopologyDetail(entity, merchant, project); }}>查看详情</button></td></tr>
             ));
           })}</tbody>
         </table>
+        <TablePager testid="governance-merchants-pager" page={merchantRows.page} pageCount={merchantRows.pageCount} total={merchantRows.total} onPrev={merchantRows.prev} onNext={merchantRows.next} />
+        {canEntityView && entityRows.pageCount > 1 ? <><span>经营主体资料分页（用于关系表）</span><TablePager testid="governance-entities-pager" page={entityRows.page} pageCount={entityRows.pageCount} total={entityRows.total} onPrev={entityRows.prev} onNext={entityRows.next} /></> : null}
       </div>
     </>
   );
@@ -197,14 +216,15 @@ export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, nav
           <tbody>{spaces.map((space) => {
             const project = projects.find((item) => item.id === space.projectId);
             const spaceFocused = !!focusObjectId && (focusObjectId === space.id || focusObjectId === space.projectId);
-            return <tr key={space.id} data-testid={`governance-space-row-${space.id}`} className={`admin-row-clickable${spaceFocused ? " admin-row-selected" : ""}`} tabIndex={0} onClick={() => openSpaceDetail(space, project)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openSpaceDetail(space, project); } }}><td>{display(space.spaceName)}</td><td>{display(project?.name ?? space.projectId)}</td><td>{display(space.floorNo)}</td><td>{display(space.areaSquareMeter)}</td><td>{formatSpaceType(space.spaceType)}</td><td>{formatSpaceStatus(space.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看空间点位详情" onClick={(event) => { event.stopPropagation(); openSpaceDetail(space, project); }}>查看详情</button></td></tr>;
+            return <tr key={space.id} data-testid={`governance-space-row-${space.id}`} className={`admin-row-clickable${spaceFocused ? " admin-row-selected" : ""}`} tabIndex={0} onClick={() => openSpaceDetail(space, project)} onKeyDown={(event) => { if (event.target === event.currentTarget && (event.key === "Enter" || event.key === " ")) { event.preventDefault(); openSpaceDetail(space, project); } }}><td>{display(space.spaceName)}</td><td>{display(project?.name ?? space.projectId)}</td><td>{display(space.floorNo)}</td><td>{display(space.areaSquareMeter)}</td><td>{formatSpaceType(space.spaceType, spaceTypes)}</td><td>{formatSpaceStatus(space.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看空间点位详情" onClick={(event) => { event.stopPropagation(); openSpaceDetail(space, project); }}>查看详情</button></td></tr>;
           })}</tbody>
         </table>
+        <TablePager testid="governance-spaces-pager" page={spaceRows.page} pageCount={spaceRows.pageCount} total={spaceRows.total} onPrev={spaceRows.prev} onNext={spaceRows.next} />
       </div>
     </>
   );
 
-  const focusCount = kpiFocus ? (kpiFocus.table === "spaces" ? spaces.length : merchants.length) : 0;
+  const focusCount = kpiFocus ? (kpiFocus.table === "spaces" ? spaceRows.total : merchantRows.total) : 0;
 
   return (
     <section data-testid="governance-topology-page">
@@ -212,7 +232,7 @@ export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, nav
       <TechDetails><div className="admin-generic-evidence" data-testid="governance-topology-evidence">{TEXT.evidence}</div></TechDetails>
       <AuditTrailHint event="governance_topology.viewed" />
       <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
-        {entities.length === 0 && merchants.length === 0 && projects.length === 0 && spaces.length === 0 ? (
+        {entityRows.total === 0 && merchantRows.total === 0 && projectRows.total === 0 && spaceRows.total === 0 ? (
           <div className="admin-state admin-empty" data-testid="governance-topology-empty">暂无治理拓扑数据</div>
         ) : kpiFocus ? (
           <>
@@ -225,10 +245,10 @@ export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, nav
         ) : (
           <>
             <div className="admin-kpi-grid" data-testid="governance-topology-summary">
-              <div className="admin-kpi-card"><span>经营主体</span><strong>{entities.length}</strong><small>主体口径</small></div>
-              <div className="admin-kpi-card admin-kpi-card--clickable"><span>商户</span><button type="button" className="admin-kpi-value" data-testid="governance-topology-kpi-merchants" onClick={() => enterKpiFocus("merchants")}>{merchants.length}</button><small>商户档案</small></div>
-              <div className="admin-kpi-card"><span>项目</span><strong>{projects.length}</strong><small>门店/场馆</small></div>
-              <div className="admin-kpi-card admin-kpi-card--clickable"><span>空间点位</span><button type="button" className="admin-kpi-value" data-testid="governance-topology-kpi-spaces" onClick={() => enterKpiFocus("spaces")}>{spaces.length}</button><small>{attachedSpaces} 个已挂接</small></div>
+              <div className="admin-kpi-card"><span>经营主体</span><strong>{entityRows.total}</strong><small>主体口径</small></div>
+              <div className="admin-kpi-card admin-kpi-card--clickable"><span>商户</span><button type="button" className="admin-kpi-value" data-testid="governance-topology-kpi-merchants" onClick={() => enterKpiFocus("merchants")}>{merchantRows.total}</button><small>商户档案</small></div>
+              <div className="admin-kpi-card"><span>项目</span><strong>{projectRows.total}</strong><small>门店/场馆</small></div>
+              <div className="admin-kpi-card admin-kpi-card--clickable"><span>空间点位</span><button type="button" className="admin-kpi-value" data-testid="governance-topology-kpi-spaces" onClick={() => enterKpiFocus("spaces")}>{spaceRows.total}</button><small>本页 {attachedSpaces} 个已挂接</small></div>
             </div>
 
             <div className="admin-muted-action" data-testid="governance-topology-boundary">{TEXT.boundary}</div>
diff --git a/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx b/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
index 823e37e3..5453e9d3 100644
--- a/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
+++ b/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
@@ -1,5 +1,6 @@
-// 文件版本：V0.02
+// 文件版本：V0.06
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
+import userEvent from "@testing-library/user-event";
 import { afterEach, describe, expect, it, vi } from "vitest";
 
 import { OrganizationMerchantPage } from "./OrganizationMerchantPage";
@@ -12,27 +13,29 @@ const ENTITIES = [
   { id: "org-1", entity_name: "平台经营主体", entity_role: "management_company", unified_social_credit_code: "91330100MA27XW8H", legal_representative: "张三", bank_account_ref: "vault://demo", status: "active", created_at: "2026-07-01T00:00:00Z" },
 ];
 const MERCHANTS = [
-  { id: "m-1", merchant_name: "正常商户甲", merchant_type: "catering", business_entity_id: "org-1", contact_name: "联系人甲", contact_phone: "13800000001", mall_block_code: "1F-A", area_square_meter: 128.5, leased_area_square_meter: 120, cooperation_form: "lease", status: "active" },
-  { id: "m-2", merchant_name: "停用商户乙", contact_name: "联系人乙", status: "suspended" },
+  { id: "m-1", project_id: "p-1", merchant_name: "正常商户甲", merchant_type: "catering", business_entity_id: "org-1", contact_name: "联系人甲", contact_phone: "13800000001", mall_block_code: "1F-A", area_square_meter: 128.5, leased_area_square_meter: 120, cooperation_form: "lease", status: "active" },
+  { id: "m-2", project_id: "p-1", merchant_name: "停用商户乙", contact_name: "联系人乙", status: "suspended" },
   // M012-008：已存档商户（行内动作为取消存档/申请删除下拉）。status 取 suspended 以不改变「正常商户（1）」断言。
-  { id: "m-3", merchant_name: "已存档商户丙", contact_name: "联系人丙", status: "suspended", archived_at: "2026-07-01T00:00:00Z", delete_status: "none" },
+  { id: "m-3", project_id: "p-1", merchant_name: "已存档商户丙", contact_name: "联系人丙", status: "suspended", archived_at: "2026-07-01T00:00:00Z", delete_status: "none" },
 ];
 const PROJECTS = [
-  { id: "p-1", name: "项目一", code: "PJ-1", merchant_id: "m-1", venue_type: "bar", floor: "1F", capacity: 80, status: "active" },
+  { id: "p-1", name: "项目一", code: "PJ-1", merchant_id: null, venue_type: "bar", floor: "1F", capacity: 80, status: "active" },
 ];
 
-function envelope(items: unknown[]): Response {
+function envelope(items: unknown[], total = items.length, page = 1): Response {
   return new Response(
-    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
+    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page, page_size: 500, total } }),
     { status: 200, headers: { "content-type": "application/json" } },
   );
 }
 
-function makeApi(): AdminApi {
+function makeApi(respond?: (url: string, init?: RequestInit) => Response | undefined): AdminApi {
   vi.stubGlobal(
     "fetch",
     vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
       const url = String(input);
+      const response = respond?.(url, init);
+      if (response) return Promise.resolve(response);
       // 写路径（创建/存档/删除申请）默认返回成功信封；失败场景由用例 mockImplementationOnce 覆盖。
       if (init?.method === "POST") return Promise.resolve(new Response(JSON.stringify({ success: true, code: "OK", message: "success", data: { id: "m-new" } }), { status: 200, headers: { "content-type": "application/json" } }));
       if (url.includes("business-entities")) return Promise.resolve(envelope(ENTITIES));
@@ -46,13 +49,14 @@ function makeApi(): AdminApi {
 
 const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
 
-function renderPage() {
+function renderPage(respond?: (url: string, init?: RequestInit) => Response | undefined, overrides: Partial<AdminPageProps> = {}) {
   const fullProps: AdminPageProps = {
-    api: makeApi(),
+    api: makeApi(respond),
     user: { id: "u1", username: "admin", accountType: "platform_admin" },
     setActionResult: vi.fn(),
     setGlobalError: vi.fn(),
     effectivePermissions: ADMIN_PERMS,
+    ...overrides,
   };
   render(<OrganizationMerchantPage {...fullProps} />);
   return fullProps;
@@ -60,7 +64,7 @@ function renderPage() {
 
 // 聚焦视图断言：只剩商户表（active 行），其余表/表单/KPI 卡全隐藏，横幅在场。
 function expectActiveMerchantsFocusView() {
-  expect(screen.getByTestId("organization-kpi-focus-banner").textContent).toContain("正在查看：正常商户（1）");
+  expect(screen.getByTestId("organization-kpi-focus-banner").textContent).toContain("正在查看：本页正常商户（1）");
   expect(screen.getByTestId("organization-merchant-row-m-1")).toBeTruthy();
   expect(screen.queryByTestId("organization-merchant-row-m-2")).toBeNull();
   expect(screen.queryByTestId("organization-entities-table")).toBeNull();
@@ -139,7 +143,8 @@ describe("OrganizationMerchantPage KPI 聚焦视图（带参重进本页）", ()
     const projectsText = screen.getByTestId("organization-projects-table").textContent ?? "";
     expect(projectsText).toContain("项目一");
     expect(projectsText).toContain("PJ-1");
-    expect(projectsText).toContain("m-1");
+    expect(projectsText).not.toContain("m-1");
+    expect(screen.getByTestId("organization-merchant-row-m-1").textContent).toContain("项目一");
     // M012-004：业态列中文（PGM-001 零英文），不再断言英文码。
     expect(projectsText).toContain("酒吧");
     expect(projectsText).not.toMatch(/\bbar\b/);
@@ -329,3 +334,84 @@ describe("OrganizationMerchantPage 存档/删除写路径（M012-008）", () =>
     await waitFor(() => expect(props.setGlobalError).toHaveBeenCalledWith(expect.any(String)));
   });
 });
+
+
+describe("M012 本次组织档案回归", () => {
+  afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); window.history.replaceState({}, "", "/admin/governance/organizations"); });
+
+  it("缺主体查看权仍展示商户且不请求主体接口", async () => {
+    renderPage(undefined, { effectivePermissions: { status: "ready", value: { grants: ["merchant.profile.view", "governance.project.view"], denies: [], wildcard: false } } });
+    expect(await screen.findByTestId("organization-merchant-row-m-1")).toBeTruthy();
+    expect(vi.mocked(fetch).mock.calls.some(([url]) => String(url).includes("/business-entities"))).toBe(false);
+  });
+
+  it("暂停、待准入和黑名单是不同的中文商户状态", async () => {
+    renderPage((url) => url.includes("/merchants")
+      ? envelope([{ ...MERCHANTS[0], status: "pending_admission" }, MERCHANTS[1], { ...MERCHANTS[2], status: "blacklisted" }])
+      : undefined);
+    expect((await screen.findByTestId("organization-merchant-row-m-1")).textContent).toContain("待准入");
+    expect(screen.getByTestId("organization-merchant-row-m-2").textContent).toContain("暂停");
+    expect(screen.getByTestId("organization-merchant-row-m-3").textContent).toContain("黑名单");
+    expect(screen.getByTestId("organization-merchants-table").textContent).not.toContain("已封禁");
+  });
+
+  it("分页保留商户归档和业态筛选参数，后页商户可达", async () => {
+    const rows = Array.from({ length: 501 }, (_, i) => ({ ...MERCHANTS[0], id: "m-page-" + i, merchant_name: "分页商户" + i }));
+    renderPage((url) => {
+      if (!url.includes("/merchants")) return undefined;
+      const page = Number(new URL(url, "http://localhost").searchParams.get("page") || 1);
+      return envelope(rows.slice((page - 1) * 500, page * 500), 501, page);
+    });
+    await screen.findByTestId("organization-merchant-row-m-page-0");
+    fireEvent.click(screen.getByTestId("organization-merchants-pager-next"));
+    expect(await screen.findByTestId("organization-merchant-row-m-page-500")).toBeTruthy();
+    const pageTwoUrl = vi.mocked(fetch).mock.calls.map(([url]) => String(url)).find(url => url.includes("/merchants") && url.includes("page=2"));
+    expect(pageTwoUrl).toContain("includeArchived=false");
+    expect(screen.getByTestId("organization-merchants-pager-status").textContent).toContain("共 501 条");
+    fireEvent.click(screen.getByTestId("merchant-show-archived").querySelector("input")!);
+    fireEvent.change(await screen.findByTestId("merchant-type-filter"), { target: { value: "retail" } });
+    await waitFor(() => {
+      const urls = vi.mocked(fetch).mock.calls.map(([url]) => String(url)).filter(url => url.includes("/merchants"));
+      const params = new URL(urls.at(-1)!, "http://localhost").searchParams;
+      expect(params.get("includeArchived")).toBe("true");
+      expect(params.get("merchantType")).toBe("retail");
+      expect(params.get("page")).toBe("1");
+    });
+    await screen.findByTestId("organization-merchant-row-m-page-0");
+    fireEvent.click(screen.getByTestId("organization-merchants-pager-next"));
+    await screen.findByTestId("organization-merchant-row-m-page-500");
+    const urls = vi.mocked(fetch).mock.calls.map(([url]) => String(url)).filter(url => url.includes("/merchants"));
+    expect(urls.at(-1)).toContain("merchantType=retail");
+    expect(urls.at(-1)).toContain("includeArchived=true");
+
+  });
+
+  it("商户存档按钮用Enter触发原操作而不打开行抽屉", async () => {
+    const prompt = vi.spyOn(window, "prompt").mockReturnValue(null);
+    renderPage();
+    const button = await screen.findByTestId("merchant-archive-m-1");
+    button.focus();
+    await userEvent.setup().keyboard("{Enter}");
+    expect(prompt).toHaveBeenCalledWith("存档（下线）理由：");
+    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
+  });
+  it("存档最后一页的唯一商户后回到仍有记录的第一页", async () => {
+    let archived = false;
+    const rows = Array.from({ length: 501 }, (_, i) => ({ ...MERCHANTS[0], id: "m-last-" + i }));
+    renderPage((url, init) => {
+      if (init?.method === "POST" && url.includes("/archive")) { archived = true; return envelope([]); }
+      if (!url.includes("/merchants")) return undefined;
+      const page = Number(new URL(url, "http://localhost").searchParams.get("page") || 1);
+      const current = archived ? rows.slice(0, 500) : rows;
+      return envelope(current.slice((page - 1) * 500, page * 500), current.length, page);
+    });
+    await screen.findByTestId("organization-merchant-row-m-last-0");
+    fireEvent.click(screen.getByTestId("organization-merchants-pager-next"));
+    await screen.findByTestId("organization-merchant-row-m-last-500");
+    vi.spyOn(window, "prompt").mockReturnValue("结束经营");
+    fireEvent.click(screen.getByTestId("merchant-archive-m-last-500"));
+    await waitFor(() => expect(screen.getByTestId("organization-merchants-pager-status").textContent).toContain("第 1 / 1 页"));
+    expect(await screen.findByTestId("organization-merchant-row-m-last-0")).toBeTruthy();
+  });
+
+});
diff --git a/apps/web/src/admin/pages/OrganizationMerchantPage.tsx b/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
index 7d5b3ee9..06cb6e08 100644
--- a/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
+++ b/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
@@ -1,19 +1,29 @@
-// 文件版本：V0.02
+// 文件版本：V0.03
 import React from "react";
 import { TechDetails } from "../components/TechDetails";
 import { AdminDataState } from "../components/AdminDataState";
 import { AdminPageHeader } from "../components/AdminPageHeader";
 import { AuditTrailHint, auditTraceField } from "../components/AuditTrailHint";
-import { useAdminResource } from "../hooks/useAdminResource";
+import { useServerPagedRows } from "../hooks/useServerPagedRows";
+import { TablePager } from "../components/TablePager";
 import { hasPermissions } from "../security/PermissionGate";
 import type { AdminPageProps, BusinessEntityRow, GovernanceProjectRow, MerchantAdmissionRow } from "../types";
 import { PermissionButton } from "../security/Can";
 import { RowActions, type RowAction } from "../components/RowActions";
 import { formatBusinessStatus } from "../i18n/statusLabels";
-import { TraceLink } from "../components/TraceLink";
 import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
 import { GuidedFlowModal } from "../components/GuidedFlowModal";
 
+type MerchantRow = MerchantAdmissionRow & { projectId?: string | null };
+const ORGANIZATION_STATUS_LABELS: Record<string, string> = { pending_admission: "待准入", active: "正常", suspended: "暂停", terminated: "终止", archived: "归档", blacklisted: "黑名单" };
+const PROJECT_STATUS_LABELS: Record<string, string> = { draft: "草稿", pending_activation: "待启用", active: "已启用", suspended: "暂停", terminated: "终止", archived: "归档" };
+function formatOrganizationStatus(code?: string | null) {
+  return code ? ORGANIZATION_STATUS_LABELS[code] ?? "未识别状态" : "—";
+}
+function formatProjectStatus(code?: string | null) {
+  return code ? PROJECT_STATUS_LABELS[code] ?? "未识别状态" : "—";
+}
+
 const TEXT = {
   title: "组织与商户档案",
   description: "维护经营主体、商户和项目档案，确保运营主体、合同主体和结算主体后续可以唯一绑定、可追溯。",
@@ -69,14 +79,11 @@ function readKpiFocusFromUrl(): KpiFocus | null {
 function kpiFocusTitle(focus: KpiFocus): string {
   if (focus.table === "entities") return "经营主体";
   if (focus.table === "projects") return "项目档案";
-  if (focus.status === "active") return "正常商户";
-  return focus.status ? `商户档案（${formatBusinessStatus(focus.status)}）` : "商户档案";
+  if (focus.status === "active") return "本页正常商户";
+  return focus.status ? `本页商户档案（${formatOrganizationStatus(focus.status)}）` : "商户档案";
 }
 
 export function OrganizationMerchantPage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo, focusObjectId }: AdminPageProps) {
-  const [entities, setEntities] = React.useState<BusinessEntityRow[]>([]);
-  const [merchants, setMerchants] = React.useState<MerchantAdmissionRow[]>([]);
-  const [projects, setProjects] = React.useState<GovernanceProjectRow[]>([]);
   // M012-006：不预填验收默认值；商户名/联系人/电话/合作形式均由用户填写或选择。
   const [merchantName, setMerchantName] = React.useState("");
   // 业态必填 → 不预选，逼招商自己选；选错＝通票分账份额拆给错的业态。
@@ -126,22 +133,28 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
     (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
   }
 
-  const { loading, error, reload } = useAdminResource(
-    async () => {
-      // 跨域副资源不请求（根因修）：主体端点需 entity.binding.view，本页 gate 仅 merchant.profile.view + governance.project.view——
-      // 缺该权限角色 403 会 reject 整个 Promise.all 致整页白屏；缺权限则不请求、主体下拉降级为空。
-      const canEntityView = hasPermissions(effectivePermissions, ["entity.binding.view"]);
-      const [nextEntities, nextMerchants, nextProjects] = await Promise.all([
-        canEntityView ? api.get<BusinessEntityRow[]>("/api/v1/platform/business-entities") : Promise.resolve([] as BusinessEntityRow[]),
-        api.get<MerchantAdmissionRow[]>(`/api/v1/platform/merchants?includeArchived=${showArchived}${typeFilter ? `&merchantType=${typeFilter}` : ""}`),
-        api.get<GovernanceProjectRow[]>("/api/v1/platform/projects"),
-      ]);
-      setEntities(nextEntities);
-      setMerchants(nextMerchants);
-      setProjects(nextProjects);
-    },
-    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
-  );
+  const canEntityView = hasPermissions(effectivePermissions, ["entity.binding.view"]);
+  const entityRows = useServerPagedRows(
+    (page, size) => canEntityView ? api.getPaged<BusinessEntityRow>("/api/v1/platform/business-entities?page=" + page + "&size=" + size)
+      : Promise.resolve({ items: [] as BusinessEntityRow[], total: 0, page: 1, size }),
+    500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
+  const merchantRows = useServerPagedRows(
+    (page, size, query) => api.getPaged<MerchantRow>("/api/v1/platform/merchants?" + (query || "includeArchived=false") + "&page=" + page + "&size=" + size),
+    500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
+  const projectRows = useServerPagedRows(
+    (page, size) => api.getPaged<GovernanceProjectRow>("/api/v1/platform/projects?page=" + page + "&size=" + size),
+    500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
+  const entities = entityRows.rows;
+  const merchants = merchantRows.rows;
+  const projects = projectRows.rows;
+  const resources = [entityRows, merchantRows, projectRows];
+  const loading = resources.some((resource) => resource.loading);
+  const error = resources.find((resource) => resource.error)?.error ?? null;
+  const reload = () => resources.forEach((resource) => resource.reload());
+
+  React.useEffect(() => {
+    if (!merchantRows.loading && merchantRows.page > merchantRows.pageCount) merchantRows.search(merchantRows.keyword);
+  }, [merchantRows.loading, merchantRows.page, merchantRows.pageCount, merchantRows.keyword]);
 
   // 下钻定位：从别的页点商户进来（?focus=商户id）→ 滚动高亮该商户档案行；无 focus 或未命中时行为不变。
   React.useEffect(() => {
@@ -198,7 +211,7 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
       firstArchivedToggle.current = false;
       return;
     }
-    void reload();
+    merchantRows.search(new URLSearchParams({ includeArchived: String(showArchived), ...(typeFilter ? { merchantType: typeFilter } : {}) }).toString());
     // eslint-disable-next-line react-hooks/exhaustive-deps
   }, [showArchived, typeFilter]);
 
@@ -272,23 +285,28 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
         { label: "统一社会信用代码", value: display(row.unifiedSocialCreditCode) },
         { label: "主体角色", value: formatBusinessStatus(row.entityRole) },
         { label: "法定代表人", value: display(row.legalRepresentative) },
-        { label: "状态", value: formatBusinessStatus(row.status) },
+        { label: "状态", value: formatOrganizationStatus(row.status) },
       ],
     });
   }
 
-  function openMerchantDetail(row: MerchantAdmissionRow) {
+  function merchantProjectName(row: MerchantRow) {
+    return projects.find((project) => project.id === row.projectId)?.name ?? (row.projectId ? "项目资料未加载" : "-");
+  }
+
+  function openMerchantDetail(row: MerchantRow) {
     setDetail({
       title: `商户档案 · ${display(row.merchantName)}`,
       fields: [
         { label: "商户", value: display(row.merchantName) },
+        { label: "所属项目", value: merchantProjectName(row) },
         { label: "业态", value: row.merchantType ? formatBusinessStatus(row.merchantType) : "-" },
         { label: "联系人", value: display(row.contactName) },
         { label: "联系电话", value: display(row.contactPhone) },
         { label: "区块", value: display(row.mallBlockCode) },
         { label: "面积", value: display(row.leasedAreaSquareMeter ?? row.areaSquareMeter) },
         { label: "合作形式", value: cooperationLabel(row.cooperationForm) },
-        { label: "状态", value: formatBusinessStatus(row.status) },
+        { label: "状态", value: formatOrganizationStatus(row.status) },
         auditTraceField(navigateTo, "merchant", row.id),
       ],
     });
@@ -300,9 +318,8 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
       fields: [
         { label: "项目", value: display(row.name) },
         { label: "编码", value: display(row.code) },
-        { label: "商户", value: row.merchantId ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{display(row.merchantId)}</TraceLink> : display(row.merchantId) },
         { label: "业态", value: formatBusinessStatus(row.venueType) },
-        { label: "状态", value: formatBusinessStatus(row.status) },
+        { label: "状态", value: formatProjectStatus(row.status) },
       ],
     });
   }
@@ -313,8 +330,9 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
       <div className="admin-table-scroll" data-testid="organization-entities-section">
         <table className="admin-table" data-testid="organization-entities-table">
           <thead><tr><th>主体</th><th>统一社会信用代码</th><th>主体角色</th><th>状态</th><th>详情</th></tr></thead>
-          <tbody>{entities.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`organization-entity-row-${row.id}`} onClick={() => openEntityDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openEntityDetail(row); } }}><td>{display(row.entityName)}</td><td>{display(row.unifiedSocialCreditCode)}</td><td>{formatBusinessStatus(row.entityRole)}</td><td>{formatBusinessStatus(row.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看经营主体详情" onClick={(event) => { event.stopPropagation(); openEntityDetail(row); }}>查看详情</button></td></tr>)}</tbody>
+          <tbody>{entities.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`organization-entity-row-${row.id}`} onClick={() => openEntityDetail(row)} onKeyDown={(event) => { if (event.target === event.currentTarget && (event.key === "Enter" || event.key === " ")) { event.preventDefault(); openEntityDetail(row); } }}><td>{display(row.entityName)}</td><td>{display(row.unifiedSocialCreditCode)}</td><td>{formatBusinessStatus(row.entityRole)}</td><td>{formatOrganizationStatus(row.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看经营主体详情" onClick={(event) => { event.stopPropagation(); openEntityDetail(row); }}>查看详情</button></td></tr>)}</tbody>
         </table>
+        <TablePager testid="organization-entities-pager" page={entityRows.page} pageCount={entityRows.pageCount} total={entityRows.total} onPrev={entityRows.prev} onNext={entityRows.next} />
       </div>
     </>
   );
@@ -359,9 +377,10 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
       <div className="admin-table-scroll" data-testid="organization-merchants-section">
         <table className="admin-table" data-testid="organization-merchants-table">
           {/* 后端无“公司名/风险/留痕证据”字段（原按臆造字段读致整表“-”），列对齐真实字段：联系人/联系电话替换。 */}
-          <thead><tr><th>商户</th><th>业态</th><th>联系人</th><th>联系电话</th><th>区块</th><th>面积</th><th>合作形式</th><th>状态</th><th>存档状态</th><th>操作</th><th>详情</th></tr></thead>
-          <tbody>{visibleMerchants.map((row) => <tr key={row.id} data-testid={`organization-merchant-row-${row.id}`} className={`admin-row-clickable${row.id === focusObjectId ? " admin-row-selected" : ""}`} tabIndex={0} onClick={() => openMerchantDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openMerchantDetail(row); } }}><td>{display(row.merchantName)}</td><td>{row.merchantType ? formatBusinessStatus(row.merchantType) : "-"}</td><td>{display(row.contactName)}</td><td>{display(row.contactPhone)}</td><td>{display(row.mallBlockCode)}</td><td>{display(row.leasedAreaSquareMeter ?? row.areaSquareMeter)}</td><td>{cooperationLabel(row.cooperationForm)}</td><td>{formatBusinessStatus(row.status)}</td><td>{merchantArchiveLabel(row)}</td><td onClick={(event) => event.stopPropagation()}>{merchantActions(row)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看商户档案详情" onClick={(event) => { event.stopPropagation(); openMerchantDetail(row); }}>查看详情</button></td></tr>)}</tbody>
+          <thead><tr><th>商户</th><th>所属项目</th><th>业态</th><th>联系人</th><th>联系电话</th><th>区块</th><th>面积</th><th>合作形式</th><th>状态</th><th>存档状态</th><th>操作</th><th>详情</th></tr></thead>
+          <tbody>{visibleMerchants.map((row) => <tr key={row.id} data-testid={`organization-merchant-row-${row.id}`} className={`admin-row-clickable${row.id === focusObjectId ? " admin-row-selected" : ""}`} tabIndex={0} onClick={() => openMerchantDetail(row)} onKeyDown={(event) => { if (event.target === event.currentTarget && (event.key === "Enter" || event.key === " ")) { event.preventDefault(); openMerchantDetail(row); } }}><td>{display(row.merchantName)}</td><td>{merchantProjectName(row)}</td><td>{row.merchantType ? formatBusinessStatus(row.merchantType) : "-"}</td><td>{display(row.contactName)}</td><td>{display(row.contactPhone)}</td><td>{display(row.mallBlockCode)}</td><td>{display(row.leasedAreaSquareMeter ?? row.areaSquareMeter)}</td><td>{cooperationLabel(row.cooperationForm)}</td><td>{formatOrganizationStatus(row.status)}</td><td>{merchantArchiveLabel(row)}</td><td onClick={(event) => event.stopPropagation()}>{merchantActions(row)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看商户档案详情" onClick={(event) => { event.stopPropagation(); openMerchantDetail(row); }}>查看详情</button></td></tr>)}</tbody>
         </table>
+        <TablePager testid="organization-merchants-pager" page={merchantRows.page} pageCount={merchantRows.pageCount} total={merchantRows.total} onPrev={merchantRows.prev} onNext={merchantRows.next} />
       </div>
     </>
   );
@@ -371,14 +390,15 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
       <div className="admin-section-title">项目档案</div>
       <div className="admin-table-scroll" data-testid="organization-projects-section">
         <table className="admin-table" data-testid="organization-projects-table">
-          <thead><tr><th>项目</th><th>编码</th><th>商户</th><th>业态</th><th>状态</th><th>详情</th></tr></thead>
-          <tbody>{projects.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`organization-project-row-${row.id}`} onClick={() => openProjectDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openProjectDetail(row); } }}><td>{display(row.name)}</td><td>{display(row.code)}</td><td onClick={(event) => event.stopPropagation()}>{row.merchantId ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{display(row.merchantId)}</TraceLink> : display(row.merchantId)}</td><td>{formatBusinessStatus(row.venueType)}</td><td>{formatBusinessStatus(row.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看项目档案详情" onClick={(event) => { event.stopPropagation(); openProjectDetail(row); }}>查看详情</button></td></tr>)}</tbody>
+          <thead><tr><th>项目</th><th>编码</th><th>业态</th><th>状态</th><th>详情</th></tr></thead>
+          <tbody>{projects.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`organization-project-row-${row.id}`} onClick={() => openProjectDetail(row)} onKeyDown={(event) => { if (event.target === event.currentTarget && (event.key === "Enter" || event.key === " ")) { event.preventDefault(); openProjectDetail(row); } }}><td>{display(row.name)}</td><td>{display(row.code)}</td><td>{formatBusinessStatus(row.venueType)}</td><td>{formatProjectStatus(row.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看项目档案详情" onClick={(event) => { event.stopPropagation(); openProjectDetail(row); }}>查看详情</button></td></tr>)}</tbody>
         </table>
+        <TablePager testid="organization-projects-pager" page={projectRows.page} pageCount={projectRows.pageCount} total={projectRows.total} onPrev={projectRows.prev} onNext={projectRows.next} />
       </div>
     </>
   );
 
-  const focusCount = kpiFocus ? (kpiFocus.table === "entities" ? entities.length : kpiFocus.table === "projects" ? projects.length : visibleMerchants.length) : 0;
+  const focusCount = kpiFocus ? (kpiFocus.table === "entities" ? entityRows.total : kpiFocus.table === "projects" ? projectRows.total : merchantStatusFilter ? visibleMerchants.length : merchantRows.total) : 0;
 
   return (
     <section data-testid="organization-merchant-page">
@@ -390,7 +410,7 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
       <AuditTrailHint event="organization.viewed / merchant_profile.created" />
       {formError ? <div className="admin-inline-error" data-testid="organization-merchant-form-error">{formError}</div> : null}
       <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
-        {entities.length === 0 && merchants.length === 0 && projects.length === 0 ? (
+        {entityRows.total === 0 && merchantRows.total === 0 && projectRows.total === 0 ? (
           <div className="admin-state admin-empty" data-testid="organization-merchant-empty">暂无组织与商户档案</div>
         ) : kpiFocus ? (
           <>
@@ -406,21 +426,21 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
             <div className="admin-kpi-grid" data-testid="organization-merchant-summary">
               <div className="admin-kpi-card admin-kpi-card--clickable">
                 <span>经营主体</span>
-                <button type="button" className="admin-kpi-value" data-testid="organization-kpi-entities" onClick={() => enterKpiFocus("entities")}>{entities.length}</button>
+                <button type="button" className="admin-kpi-value" data-testid="organization-kpi-entities" onClick={() => enterKpiFocus("entities")}>{entityRows.total}</button>
                 <small>组织档案</small>
               </div>
               <div className="admin-kpi-card admin-kpi-card--clickable">
                 <span>商户档案</span>
-                <button type="button" className="admin-kpi-value" data-testid="organization-kpi-merchants" onClick={() => enterKpiFocus("merchants")}>{merchants.length}</button>
+                <button type="button" className="admin-kpi-value" data-testid="organization-kpi-merchants" onClick={() => enterKpiFocus("merchants")}>{merchantRows.total}</button>
                 <small>商户档案数</small>
               </div>
               <div className="admin-kpi-card admin-kpi-card--clickable">
                 <span>项目档案</span>
-                <button type="button" className="admin-kpi-value" data-testid="organization-kpi-projects" onClick={() => enterKpiFocus("projects")}>{projects.length}</button>
+                <button type="button" className="admin-kpi-value" data-testid="organization-kpi-projects" onClick={() => enterKpiFocus("projects")}>{projectRows.total}</button>
                 <small>项目数</small>
               </div>
               <div className="admin-kpi-card admin-kpi-card--clickable">
-                <span>正常商户</span>
+                <span>本页正常商户</span>
                 <button type="button" className="admin-kpi-value" data-testid="organization-kpi-active-merchants" onClick={() => enterKpiFocus("merchants", "active")}>{merchants.filter((row) => row.status === "active").length}</button>
                 <small>正常商户数</small>
               </div>
diff --git a/apps/web/src/admin/pages/SubjectBindingsPage.test.tsx b/apps/web/src/admin/pages/SubjectBindingsPage.test.tsx
index 2cdcc838..8f2f1836 100644
--- a/apps/web/src/admin/pages/SubjectBindingsPage.test.tsx
+++ b/apps/web/src/admin/pages/SubjectBindingsPage.test.tsx
@@ -1,5 +1,6 @@
-// 文件版本：V0.01
+// 文件版本：V0.03
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
+import userEvent from "@testing-library/user-event";
 import { afterEach, describe, expect, it, vi } from "vitest";
 
 import { SubjectBindingsPage } from "./SubjectBindingsPage";
@@ -14,9 +15,9 @@ const BINDINGS = [
   { id: "b-2", merchant_id: "m-2", operation_entity_id: "be_op_2", contract_entity_id: "be_ct_2", settlement_entity_id: "be_st_2", status: "inactive", effective_from: "2026-06-01", effective_to: "2026-06-30", change_reason: "contract changed" },
 ];
 
-function envelope(items: unknown[]): Response {
+function envelope(items: unknown[], total = items.length, page = 1): Response {
   return new Response(
-    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
+    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page, page_size: 500, total } }),
     { status: 200, headers: { "content-type": "application/json" } },
   );
 }
@@ -28,10 +29,13 @@ function envelopeOne(item: unknown): Response {
   );
 }
 
-function makeApi(): AdminApi {
+function makeApi(respond?: (url: string, init?: RequestInit) => Response | undefined): AdminApi {
   vi.stubGlobal(
     "fetch",
-    vi.fn().mockImplementation((_input: RequestInfo | URL, init?: RequestInit) => {
+    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
+      const url = String(input);
+      const response = respond?.(url, init);
+      if (response) return Promise.resolve(response);
       if (init?.method === "POST") return Promise.resolve(envelopeOne({ ...BINDINGS[0], status: "inactive" }));
       return Promise.resolve(envelope(BINDINGS));
     }),
@@ -41,13 +45,14 @@ function makeApi(): AdminApi {
 
 const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
 
-function renderPage() {
+function renderPage(respond?: (url: string, init?: RequestInit) => Response | undefined, overrides: Partial<AdminPageProps> = {}) {
   const fullProps: AdminPageProps = {
-    api: makeApi(),
+    api: makeApi(respond),
     user: { id: "u1", username: "admin", accountType: "platform_admin" },
     setActionResult: vi.fn(),
     setGlobalError: vi.fn(),
     effectivePermissions: ADMIN_PERMS,
+    ...overrides,
   };
   render(<SubjectBindingsPage {...fullProps} />);
   return fullProps;
@@ -71,7 +76,7 @@ describe("SubjectBindingsPage KPI 聚焦视图", () => {
     fireEvent.click(screen.getByTestId("subject-bindings-kpi-active"));
     expect(window.location.search).toContain("kpi=bindings");
     expect(window.location.search).toContain("status=active");
-    expect(screen.getByTestId("subject-bindings-kpi-focus-banner").textContent).toContain("正在查看：生效绑定（1）");
+    expect(screen.getByTestId("subject-bindings-kpi-focus-banner").textContent).toContain("正在查看：本页生效绑定（1）");
     expect(screen.queryByTestId("subject-bindings-summary")).toBeNull();
     expect(screen.queryByTestId("subject-bindings-submit-change")).toBeNull();
     const text = screen.getByTestId("subject-bindings-table").textContent ?? "";
@@ -215,3 +220,54 @@ describe("SubjectBindingsPage 绑定变更提交（M012-002/006/008）", () => {
     expect(body).not.toContain("完整后台验收变更");
   });
 });
+
+
+describe("M012 本次绑定回归", () => {
+  afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); window.history.replaceState({}, "", "/admin/governance/subject-bindings"); });
+
+  it("空格选择第二条绑定，不被行快捷键截获", async () => {
+    renderPage();
+    const radio = await screen.findByTestId("subject-bindings-select-b-2");
+    radio.focus();
+    await userEvent.setup().keyboard(" ");
+    expect((radio as HTMLInputElement).checked).toBe(true);
+    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
+    fireEvent.keyDown(screen.getByTestId("subject-bindings-row-b-2"), { key: "Enter" });
+    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
+  });
+
+  it("绑定暂停状态不会显示成封禁", async () => {
+    renderPage((_url, init) => init?.method === "POST" ? undefined : envelope([{ ...BINDINGS[0], status: "suspended" }]));
+    const row = await screen.findByTestId("subject-bindings-row-b-1");
+    expect(row.textContent).toContain("暂停");
+    expect(row.textContent).not.toContain("已封禁");
+  });
+
+  it("501条绑定可以翻到第二页，本页去重数不会冒称全量", async () => {
+    const rows = Array.from({ length: 501 }, (_, i) => ({ ...BINDINGS[0], id: "b-page-" + i, merchant_id: "m-page-" + i }));
+    renderPage((url, init) => {
+      if (init?.method === "POST") return undefined;
+      const page = Number(new URL(url, "http://localhost").searchParams.get("page") || 1);
+      return envelope(rows.slice((page - 1) * 500, page * 500), 501, page);
+    });
+    await screen.findByTestId("subject-bindings-row-b-page-0");
+    expect(screen.getByTestId("subject-bindings-summary").textContent).toContain("本页");
+    expect(screen.getByTestId("subject-bindings-pager-status").textContent).toContain("共 501 条");
+    fireEvent.click(screen.getByTestId("subject-bindings-pager-next"));
+    expect(await screen.findByTestId("subject-bindings-row-b-page-500")).toBeTruthy();
+  });
+
+  it("后台拒绝绑定变更时保留原因并显示错误", async () => {
+    const props = renderPage((_url, init) => init?.method === "POST"
+      ? new Response(JSON.stringify({ success: false, code: "BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW", message: "生效绑定变更须经财税复核" }), { status: 409, headers: { "content-type": "application/json" } })
+      : undefined);
+    await screen.findByTestId("subject-bindings-row-b-1");
+    fireEvent.change(screen.getByTestId("subject-bindings-change-action"), { target: { value: "inactive" } });
+    fireEvent.change(screen.getByTestId("subject-bindings-change-reason"), { target: { value: "申请调整" } });
+    vi.spyOn(window, "confirm").mockReturnValue(true);
+    fireEvent.click(screen.getByTestId("subject-bindings-submit-change"));
+    await waitFor(() => expect(props.setGlobalError).toHaveBeenCalledWith(expect.stringContaining("财税复核")));
+    expect((screen.getByTestId("subject-bindings-change-reason") as HTMLInputElement).value).toBe("申请调整");
+    expect(screen.getByTestId("subject-bindings-form-error").textContent).toContain("财税复核");
+  });
+});
diff --git a/apps/web/src/admin/pages/SubjectBindingsPage.tsx b/apps/web/src/admin/pages/SubjectBindingsPage.tsx
index ce4e76c7..aca4b239 100644
--- a/apps/web/src/admin/pages/SubjectBindingsPage.tsx
+++ b/apps/web/src/admin/pages/SubjectBindingsPage.tsx
@@ -1,14 +1,14 @@
-// 文件版本：V0.01
+// 文件版本：V0.02
 import React from "react";
 import { TechDetails } from "../components/TechDetails";
 import { AdminDataState } from "../components/AdminDataState";
 import { AdminPageHeader } from "../components/AdminPageHeader";
 import { AuditTrailHint } from "../components/AuditTrailHint";
 import { HighRiskConfirm } from "../components/HighRiskConfirm";
-import { useAdminResource } from "../hooks/useAdminResource";
+import { useServerPagedRows } from "../hooks/useServerPagedRows";
+import { TablePager } from "../components/TablePager";
 import { useSelectedRow } from "../hooks/useSelectedRow";
 import type { AdminPageProps } from "../types";
-import { formatBusinessStatus } from "../i18n/statusLabels";
 import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
 
 // M012-001：按后台 BusinessEntityBindingRecord 真实字段读取（AdminApi 已把 snake_case 转 camelCase）；
@@ -35,9 +35,14 @@ const BINDING_ACTIONS: ReadonlyArray<{ code: string; label: string }> = [
 // 后端写权限码（PlatformController：/business-entity-bindings/{id}/change）
 const BINDING_MANAGE_PERMISSION = "entity.binding.manage";
 
+const BINDING_STATUS_LABELS: Record<string, string> = { active: "有效", inactive: "停用", suspended: "暂停" };
+function formatBindingStatus(code?: string | null) {
+  return code ? BINDING_STATUS_LABELS[code] ?? "未识别状态" : "—";
+}
+
 const TEXT = {
   title: "四类主体绑定",
-  description: "展示经营对象、运营主体、合同主体和结算主体的唯一绑定关系；所有变更必须带原因、幂等键和证据附件。",
+  description: "展示经营对象、运营主体、合同主体和结算主体的唯一绑定关系；变更时须选择状态并填写原因。",
   evidence: "接口证据：读取 /api/v1/platform/business-entity-bindings；变更调用 /api/v1/platform/business-entity-bindings/{id}/change。",
   boundary: "四类主体绑定是合同、结算、利润报表和责任归属的基础口径；同一业务对象必须唯一绑定，历史合同和流程留痕不得被覆盖。",
   empty: "暂无主体绑定",
@@ -50,7 +55,7 @@ const TEXT = {
 };
 
 // KPI 聚焦视图（照 OrganizationMerchantPage 样板）：?kpi=bindings[&status=active]；点 KPI 数字=带参重进本页只渲染绑定表（对应子集）。
-// 唯一绑定对象=全量绑定表；生效绑定=status=active 子集。合同主体/选中绑定无独立行子集，KPI 数字保持纯文本不可点。
+// 唯一对象、生效绑定及合同主体指标只统计本页；翻页继续读取服务器数据。
 type KpiFocus = { status: string | null };
 
 function readKpiFocusFromUrl(): KpiFocus | null {
@@ -67,7 +72,11 @@ function display(value: unknown) {
 }
 
 export function SubjectBindingsPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
-  const [bindings, setBindings] = React.useState<BindingRow[]>([]);
+  const bindingRows = useServerPagedRows(
+    (page, size) => api.getPaged<BindingRow>("/api/v1/platform/business-entity-bindings?page=" + page + "&size=" + size),
+    500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
+  const bindings = bindingRows.rows;
+  const { loading, error, reload } = bindingRows;
   const { setSelectedId, selected } = useSelectedRow(bindings);
   // M012-006：原因不预填验收句，由用户填写；M012-002：变更后状态由用户选择。
   const [reason, setReason] = React.useState("");
@@ -86,7 +95,7 @@ export function SubjectBindingsPage({ api, setActionResult, setGlobalError, effe
         { label: "运营主体编号", value: display(row.operationEntityId) },
         { label: "合同主体编号", value: display(row.contractEntityId) },
         { label: "结算主体编号", value: display(row.settlementEntityId) },
-        { label: "状态", value: formatBusinessStatus(row.status) },
+        { label: "状态", value: formatBindingStatus(row.status) },
         { label: "生效起", value: display(row.effectiveFrom) },
         { label: "生效止", value: display(row.effectiveTo) },
         { label: "变更原因", value: display(row.changeReason) },
@@ -121,18 +130,11 @@ export function SubjectBindingsPage({ api, setActionResult, setGlobalError, effe
     (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
   }
 
-  const { loading, error, reload } = useAdminResource(
-    async () => {
-      setBindings(await api.get<BindingRow[]>("/api/v1/platform/business-entity-bindings"));
-    },
-    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
-  );
-
   const activeCount = bindings.filter((row) => row.status === "active").length;
   const uniqueObjects = new Set(bindings.map((row) => row.merchantId).filter(Boolean)).size;
-  // 聚焦时按状态子集过滤内存数组；无子集=全量。
+  // 聚焦时按状态过滤本页；总条数和后页入口由服务器分页提供。
   const visibleBindings = kpiFocus?.status ? bindings.filter((row) => row.status === kpiFocus.status) : bindings;
-  const focusTitle = kpiFocus?.status === "active" ? "生效绑定" : "主体绑定";
+  const focusTitle = kpiFocus?.status === "active" ? "本页生效绑定" : "本页主体绑定";
 
   function validateChange() {
     if (!selected?.id) {
@@ -186,7 +188,7 @@ export function SubjectBindingsPage({ api, setActionResult, setGlobalError, effe
       <AuditTrailHint event="entity_binding.viewed / entity_binding.changed" />
       {error ? <div className="admin-inline-error" data-testid="subject-bindings-error">{error.message}</div> : null}
       <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
-        {bindings.length === 0 ? <div className="admin-state admin-empty" data-testid="subject-bindings-empty">{TEXT.empty}</div> : (
+        {bindingRows.total === 0 ? <div className="admin-state admin-empty" data-testid="subject-bindings-empty">{TEXT.empty}</div> : (
           <>
             {kpiFocus ? (
               <div className="admin-state" data-testid="subject-bindings-kpi-focus-banner">
@@ -196,10 +198,10 @@ export function SubjectBindingsPage({ api, setActionResult, setGlobalError, effe
             ) : (
               <>
                 <div className="admin-kpi-grid" data-testid="subject-bindings-summary">
-                  <div className="admin-kpi-card admin-kpi-card--clickable"><span>唯一绑定对象</span><button type="button" className="admin-kpi-value" data-testid="subject-bindings-kpi-unique" onClick={() => enterKpiFocus()}>{uniqueObjects}</button><small>唯一绑定</small></div>
-                  <div className="admin-kpi-card admin-kpi-card--clickable"><span>生效绑定</span><button type="button" className="admin-kpi-value" data-testid="subject-bindings-kpi-active" onClick={() => enterKpiFocus("active")}>{activeCount}</button><small>生效中</small></div>
-                  <div className="admin-kpi-card"><span>合同主体</span><strong>{bindings.filter((row) => row.contractEntityId).length}</strong><small>合同口径</small></div>
-                  <div className="admin-kpi-card"><span>选中绑定</span><strong className="admin-kpi-value--text" title={selected?.merchantId ?? "-"}>{selected?.merchantId ?? "-"}</strong><small>{selected?.status ? formatBusinessStatus(selected.status) : "-"}</small></div>
+                  <div className="admin-kpi-card admin-kpi-card--clickable"><span>本页唯一绑定对象</span><button type="button" className="admin-kpi-value" data-testid="subject-bindings-kpi-unique" onClick={() => enterKpiFocus()}>{uniqueObjects}</button><small>唯一绑定</small></div>
+                  <div className="admin-kpi-card admin-kpi-card--clickable"><span>本页生效绑定</span><button type="button" className="admin-kpi-value" data-testid="subject-bindings-kpi-active" onClick={() => enterKpiFocus("active")}>{activeCount}</button><small>生效中</small></div>
+                  <div className="admin-kpi-card"><span>本页合同主体</span><strong>{bindings.filter((row) => row.contractEntityId).length}</strong><small>含合同主体的绑定记录</small></div>
+                  <div className="admin-kpi-card"><span>选中绑定</span><strong className="admin-kpi-value--text" title={selected?.merchantId ?? "-"}>{selected?.merchantId ?? "-"}</strong><small>{selected?.status ? formatBindingStatus(selected.status) : "-"}</small></div>
                 </div>
 
                 <div className="admin-form-grid">
@@ -229,18 +231,19 @@ export function SubjectBindingsPage({ api, setActionResult, setGlobalError, effe
                 <tbody>{visibleBindings.map((row) => (
                   <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`subject-bindings-row-${row.id}`}
                     onClick={() => openBindingDetail(row)}
-                    onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openBindingDetail(row); } }}>
+                    onKeyDown={(event) => { if (event.target === event.currentTarget && (event.key === "Enter" || event.key === " ")) { event.preventDefault(); openBindingDetail(row); } }}>
                     <td onClick={(event) => event.stopPropagation()}><input type="radio" aria-label={`选择此主体绑定 ${row.id}`} data-testid={`subject-bindings-select-${row.id}`} checked={(selected?.id ?? "") === row.id} onChange={() => setSelectedId(row.id)} /></td>
                     <td>{display(row.merchantId)}</td>
                     <td>{display(row.operationEntityId)}</td>
                     <td>{display(row.contractEntityId)}</td>
                     <td>{display(row.settlementEntityId)}</td>
-                    <td>{formatBusinessStatus(row.status)}</td>
+                    <td>{formatBindingStatus(row.status)}</td>
                     <td>{display(row.effectiveFrom)}</td>
                     <td>{display(row.changeReason)}</td>
                   <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看主体绑定详情" onClick={(event) => { event.stopPropagation(); openBindingDetail(row); }}>查看详情</button></td></tr>
                 ))}</tbody>
               </table>
+              <TablePager testid="subject-bindings-pager" page={bindingRows.page} pageCount={bindingRows.pageCount} total={bindingRows.total} onPrev={bindingRows.prev} onNext={bindingRows.next} />
             </div>
           </>
         )}
```

## 最终结论
内容FAIL；归因PASS；本席B4 FAIL。M012-013仍未完成；已审完整diff，不提前修，等待Grok完整报告。

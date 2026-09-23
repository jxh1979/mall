CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# M038 B5-r2 修复后全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理；席甲旧锁 B5-r1 票 PASS 被 Grok 以 GR-M038-012 否决、现锁 B1 席甲漏 Grok GR-03→M038-012，均以 Grok 票为准）。2026-09-23T17:47:01Z
- 锁 `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（现行 V0.14，LOCK-AMEND 后；实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`，origin/main `53dec141`。
- 模块 M038 16 个前端文件（§1 全读），106 判据（现锁 B1 已按 V0.14 逐项，本轮再逐项）；B3 累计 2。
- 顺序：本席取证脚本先跑完，随后落本报告；落盘前未读 Grok 正文（modules/M038-b5-r2-grok-raw.md 存在=False）。只读承诺：未改任何已跟踪文件；一次性 worktree 已删除。
- 本轮补的方法（B5-r1 被否决的教训）：写动作的「提交中态」不再只扫一页，探针 3 对三个页面逐个写请求调用点列出所在函数、触发按钮与其忙态来源，无忙态即列为缺口。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| pages/AccessGovernancePage.tsx | 777 | 57255 | d7668403ba673b8375a541f24220397d19f2cb92d345392cf63f4100dc94b101 | 全读 |
| pages/AccessGovernancePage.degraded.test.tsx | 61 | 3322 | b34d2ed33a973f52db7b18c04ffa313213b7548605394bdee86febbd7216b6f5 | 全读 |
| pages/AccessGovernancePage.guidedcreate.test.tsx | 191 | 11207 | 521dc934d768003006e329b7728985d795e3006437d905c7bb201af04da48871 | 全读 |
| pages/AccessGovernancePage.scopechange.test.tsx | 82 | 5060 | 84f86642cb900d037d68ad6179a8e5822cee51dd123078907d954342d33f37f6 | 全读 |
| pages/AccessGovernancePage.test.tsx | 328 | 19760 | bcfe27b0f09a09c0b19f6a0f92472eac841cd64559d17172196b008e9005b327 | 全读 |
| pages/AccessUsersPage.tsx | 385 | 23708 | f0fc7976230726223325270293547065f166f41168f46b5c26cf6574daeb31cc | 全读 |
| pages/AccessUsersPage.metricdetail.test.tsx | 41 | 2121 | ff5caa1b72aac3f548dbd585aff4bc406cea98b5790c1ac1c63c72f20ddf04ca | 全读 |
| pages/AccessUsersPage.partnerbind.test.tsx | 113 | 5703 | 33d7f9d312be473c48638774bc2f344ab03c1022157c12d57089f3a32401d59d | 全读 |
| pages/AccessUsersPage.robustness.test.tsx | 45 | 2503 | fe77b21adf095eeff94014267cda5189c9387a1efd9faeb48308bb862b87ebd8 | 全读 |
| pages/AccessUsersPage.test.tsx | 191 | 10470 | 62574a344c81f20b0a5722f30220832c9e458cec7a7a9cb030c8c5a37c4eefbb | 全读 |
| pages/PermissionTemplatesPage.tsx | 565 | 28184 | f27c12d96c4d74c8ecf43d236352c65cbec97eeedd2f24b91fafda091e326086 | 全读 |
| pages/PermissionTemplatesPage.guidedcreate.test.tsx | 116 | 6000 | e026609a0545a09f6013b258c8cc60a7f1bbd4fb53d340636f0917da4fa9b6d6 | 全读 |
| pages/PermissionTemplatesPage.test.tsx | 211 | 11726 | 7260e510802e3f71912d0409720d6fdb42d2a3bc729e066d68690be1104372b3 | 全读 |
| security/Can.tsx | 84 | 2545 | bfd3fbeb22a5fd1ee620b63c139ae0045fc36caeaa3afd9d2766edb11061d193 | 全读 |
| security/PermissionGate.tsx | 44 | 1502 | a15438dae20ba874cd4385edbd2a65cef85cda71724dd6cf10ce8e750d28a4a5 | 全读 |
| security/PermissionGate.test.ts | 113 | 6276 | 14f28de8ffc8544fd26f9a5843e156ecb7dd315591375fb51af827185d4ccbfc | 全读 |

## 2. 可执行证据原文（/home/james/audit-tmp/m038_b5r2_run.log）

```
START=2026-09-23T17:46:52Z HEAD=e456ba96 origin/main=53dec141
== 0. files (lines/bytes/sha256 | vs origin/main)
AccessGovernancePage.tsx 777 57255 d7668403ba673b8375a541f24220397d19f2cb92d345392cf63f4100dc94b101 | [ 1 file changed, 81 insertions(+), 42 deletions(-)]
AccessGovernancePage.degraded.test.tsx 61 3322 b34d2ed33a973f52db7b18c04ffa313213b7548605394bdee86febbd7216b6f5 | []
AccessGovernancePage.guidedcreate.test.tsx 191 11207 521dc934d768003006e329b7728985d795e3006437d905c7bb201af04da48871 | [ 1 file changed, 80 insertions(+), 2 deletions(-)]
AccessGovernancePage.scopechange.test.tsx 82 5060 84f86642cb900d037d68ad6179a8e5822cee51dd123078907d954342d33f37f6 | []
AccessGovernancePage.test.tsx 328 19760 bcfe27b0f09a09c0b19f6a0f92472eac841cd64559d17172196b008e9005b327 | [ 1 file changed, 144 insertions(+), 1 deletion(-)]
AccessUsersPage.tsx 385 23708 f0fc7976230726223325270293547065f166f41168f46b5c26cf6574daeb31cc | [ 1 file changed, 30 insertions(+), 15 deletions(-)]
AccessUsersPage.metricdetail.test.tsx 41 2121 ff5caa1b72aac3f548dbd585aff4bc406cea98b5790c1ac1c63c72f20ddf04ca | []
AccessUsersPage.partnerbind.test.tsx 113 5703 33d7f9d312be473c48638774bc2f344ab03c1022157c12d57089f3a32401d59d | []
AccessUsersPage.robustness.test.tsx 45 2503 fe77b21adf095eeff94014267cda5189c9387a1efd9faeb48308bb862b87ebd8 | []
AccessUsersPage.test.tsx 191 10470 62574a344c81f20b0a5722f30220832c9e458cec7a7a9cb030c8c5a37c4eefbb | [ 1 file changed, 100 insertions(+), 1 deletion(-)]
PermissionTemplatesPage.tsx 565 28184 f27c12d96c4d74c8ecf43d236352c65cbec97eeedd2f24b91fafda091e326086 | [ 1 file changed, 25 insertions(+), 8 deletions(-)]
PermissionTemplatesPage.guidedcreate.test.tsx 116 6000 e026609a0545a09f6013b258c8cc60a7f1bbd4fb53d340636f0917da4fa9b6d6 | []
PermissionTemplatesPage.test.tsx 211 11726 7260e510802e3f71912d0409720d6fdb42d2a3bc729e066d68690be1104372b3 | [ 1 file changed, 117 insertions(+)]
Can.tsx 84 2545 bfd3fbeb22a5fd1ee620b63c139ae0045fc36caeaa3afd9d2766edb11061d193 | []
PermissionGate.tsx 44 1502 a15438dae20ba874cd4385edbd2a65cef85cda71724dd6cf10ce8e750d28a4a5 | []
PermissionGate.test.ts 113 6276 14f28de8ffc8544fd26f9a5843e156ecb7dd315591375fb51af827185d4ccbfc | [ 1 file changed, 54 insertions(+), 2 deletions(-)]
本模块之外、apps/web 下相对 origin/main 有差异且由本轮引入的文件：无（两轮 diff 只含本模块文件；冻结 diff sha256 r1 30bbbdc50244c0384c56bc9bcdecc326e55bf74d6c6d43250344fd438d34fc4b r2 27892c36c2ffebe60aa9c81df5b6d1b8158ae6913db1e62645f45fcf5340eecd）
== 1. origin/main 一次性 worktree＋本轮 diff
scratch at 53dec141
两轮 diff 已应用: 8 files
scratch 里 16 个文件与主工作区逐字节相同（无 MISMATCH 行即是）
VITEST_EXIT(module)=0
 ✓ src/admin/security/PermissionGate.test.ts (11 tests) 41ms
 ✓ src/admin/pages/AccessUsersPage.partnerbind.test.tsx (2 tests) 72ms
 ✓ src/admin/pages/AccessUsersPage.metricdetail.test.tsx (2 tests) 63ms
 ✓ src/admin/pages/AccessUsersPage.robustness.test.tsx (1 test) 43ms
 ✓ src/admin/pages/AccessGovernancePage.scopechange.test.tsx (2 tests) 331ms
 ✓ src/admin/pages/PermissionTemplatesPage.guidedcreate.test.tsx (3 tests) 359ms
 ✓ src/admin/pages/PermissionTemplatesPage.test.tsx (8 tests) 265ms
 ✓ src/admin/pages/AccessUsersPage.test.tsx (6 tests) 512ms
 ✓ src/admin/pages/AccessGovernancePage.degraded.test.tsx (1 test) 273ms
 ✓ src/admin/pages/AccessGovernancePage.guidedcreate.test.tsx (5 tests) 667ms
 ✓ src/admin/pages/AccessGovernancePage.test.tsx (19 tests) 973ms
 Test Files  11 passed (11)
      Tests  60 passed (60)
TSC_EXIT=0
scratch dirty after cleanup: 0
scratch removed
== 2. 改后三个页面里仍经公共字典上屏的调用点（formatBusinessStatus）
AccessGovernancePage.tsx:70:  return code ? GRANT_STATUS_LABELS[code] ?? formatBusinessStatus(code) : "—";
AccessGovernancePage.tsx:73:  return code ? GRANT_TYPE_LABELS[code] ?? formatBusinessStatus(code) : "—";
AccessGovernancePage.tsx:446:        { label: "状态", value: formatBusinessStatus(row.status ?? "active") },
AccessGovernancePage.tsx:703:              <label>模板状态<select data-testid="access-position-role-template-status" aria-label="模板状态" value={templateForm.status} onChange={(event) => setT
AccessGovernancePage.tsx:713:          { label: "状态", value: formatBusinessStatus(templateForm.status) },
AccessUsersPage.tsx:21:  return CREATED_VIA_LABELS[code] ?? formatBusinessStatus(code);
AccessUsersPage.tsx:153:        setActionResult(`创建员工：${created.username}（${formatBusinessStatus(accountType)}），账号已创建；套用权限模板 ${selectedTemplateId} 失败：${ap
AccessUsersPage.tsx:155:        setActionResult(`${selectedTemplateId ? "创建员工并赋权" : "创建员工"}：${created.username}（${formatBusinessStatus(accountType)}）${selectedTemplateId ? 
AccessUsersPage.tsx:195:        { label: "账号类型", value: formatBusinessStatus(row.accountType) },
AccessUsersPage.tsx:198:        { label: "状态", value: formatBusinessStatus(row.status ?? "active") },
AccessUsersPage.tsx:297:                <td>{formatBusinessStatus(row.accountType)}</td>
AccessUsersPage.tsx:300:                <td>{formatBusinessStatus(row.status ?? "active")}</td>
AccessUsersPage.tsx:358:                  {["active", "invited", "disabled"].map((value) => <option key={value} value={value}>{formatBusinessStatus(value)}</option>)}
AccessUsersPage.tsx:371:          { label: "账号类型", value: formatBusinessStatus(accountType) },
AccessUsersPage.tsx:374:          { label: "初始状态", value: accountType === "outsourced_vendor" ? formatBusinessStatus(status) : "等待注册审批" },
PermissionTemplatesPage.tsx:363:        { label: "状态", value: formatBusinessStatus(row.status) },
PermissionTemplatesPage.tsx:479:                <td>{formatBusinessStatus(row.status)}</td>
== 3. 三个页面的写请求调用点与忙态来源
  AccessGovernancePage.tsx
     savePositionTemplate       写请求 2 处 | 自带 setTemplateSaving(true)
     saveGrantPolicy            写请求 1 处 | 经 runWrite
     changePositionLifecycle    写请求 1 处 | 经 runWrite
     assignPosition             写请求 1 处 | 经 runWrite
     changeUserScope            写请求 1 处 | 经 runWrite
     roleAssignment             写请求 1 处 | 经 runWrite
     userPermissionOverride     写请求 1 处 | 经 runWrite
     applyGrantRequest          写请求 1 处 | 自带 setGrantSaving(true)
     reviewGrant                写请求 1 处 | 经 runWrite
  AccessUsersPage.tsx
     createUser                 写请求 3 处 | 自带 setSubmitting(true)
     bindPartnerOrg             写请求 1 处 | 自带 setBindBusy(true)
     unbindPartnerOrg           写请求 1 处 | 自带 setBindBusy(true)
  PermissionTemplatesPage.tsx
     createTemplate             写请求 1 处 | 自带 setCreateSaving(true)
     saveTemplate               写请求 1 处 | 自带 setSubmitting(true)
     copyTemplate               写请求 1 处 | 经 runExclusive
     applyTemplate              写请求 1 处 | 经 runExclusive
  无忙态的写函数： 0
== 3b. 治理页 runWrite 接入情况
   savePositionTemplate         写请求 2 处 | 经 runWrite=否 | 自带 try/catch=有
   saveGrantPolicy              写请求 1 处 | 经 runWrite=是 | 自带 try/catch=有
   changePositionLifecycle      写请求 1 处 | 经 runWrite=是 | 自带 try/catch=有
   assignPosition               写请求 1 处 | 经 runWrite=是 | 自带 try/catch=无
   changeUserScope              写请求 1 处 | 经 runWrite=是 | 自带 try/catch=无
   roleAssignment               写请求 1 处 | 经 runWrite=是 | 自带 try/catch=无
   userPermissionOverride       写请求 1 处 | 经 runWrite=是 | 自带 try/catch=无
   applyGrantRequest            写请求 1 处 | 经 runWrite=否 | 自带 try/catch=有
   reviewGrant                  写请求 1 处 | 经 runWrite=是 | 自带 try/catch=无
   runWrite 接入点数： 14
== 4. 30 档编号：按页面同一规则生成 vs 库内 staff_levels
30 个编号逐键且同序相同
== 5. three HTML + lock sha
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458 
END=2026-09-23T17:47:01Z
```

## 3. 三线审计

- 第一性原理线：同 B5-r1；补上「三个页面的每个写按钮提交中都禁用」。
- 顺从式线：106 判据逐项见 §4。
- 对抗式线（grilling）：Q 三个页面里还有没有无忙态的写按钮？A 探针 3 逐页清单：治理页 9 个写函数里 7 个经 runWrite、2 个引导保存自带置忙；账号页 createUser 有 submitting、绑定与解绑有 bindBusy；模板页 createTemplate 有 createSaving、saveTemplate 有 submitting、copyTemplate 与 applyTemplate 经 runExclusive。Q runExclusive 会不会吞错？A 它不 catch，两个动作函数自己的 try/catch 与提示照旧。Q 其余同 B5-r1（取值与字段名全中文、时间保值、30 档逐键一致、权限码一致）有无回归？A 本轮 diff 只动模板页的两处按钮与一个包装函数、账号页 createUser 的套模板分支，60 测全绿。Q 两步写入里第一步成功、第二步失败时用户看得到第一步的结果吗？A 账号页 createUser 套模板单独 try/catch：账号入表、提示写明「账号已创建；套用权限模板失败」（012，有测试）；治理页与模板页无两步写入。无新缺陷、无添加项、无 HTML 问题。

## 4. 106 判据逐项

| 判据 | 适用｜证据｜结论 |
|---|---|
| BEQR-063 高危动作（需审批/双证据） | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「高危动作（需审批/双证据）」的对象或文字 |
| BEQR-074 隐私桌台权限 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「隐私桌台权限」的对象或文字 |
| BEQR-143 四类主体 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「四类主体」的对象或文字 |
| BEQR-144 经营主体关系 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「经营主体关系」的对象或文字 |
| BEQR-145 城市项目公司 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「城市项目公司」的对象或文字 |
| BEQR-146 运营公司职责 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「运营公司职责」的对象或文字 |
| BEQR-147 主要收入归属 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「主要收入归属」的对象或文字 |
| BEQR-148 合作模式 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「合作模式」的对象或文字 |
| BEQR-149 职级 | 适用｜职级显示「M<级>·上/中/下」；四处职级输入为 30 档下拉，选项编号与库内 30 个编号逐键一致（008，有测试）｜通过 |
| BEQR-150 终端十一端 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「终端十一端」的对象或文字 |
| BEQR-151 最终权限计算 | 适用｜最终权限公式的五个来源在治理页与模板页各有写入口：直接装配角色、岗位模板映射与定岗、权限模板（基础/临时/单次）、员工单独授予、显式拒绝｜通过 |
| BEQR-152 项目与敏感数据权限 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「项目与敏感数据权限」的对象或文字 |
| BEQR-153 终端开关优先级 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「终端开关优先级」的对象或文字 |
| BEQR-154 综合体项目状态 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「综合体项目状态」的对象或文字 |
| BEQR-155 商户/运营主体状态 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「商户/运营主体状态」的对象或文字 |
| BEQR-156 合同状态 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「合同状态」的对象或文字 |
| BEQR-157 员工状态 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「员工状态」的对象或文字 |
| BEQR-158 设备状态 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「设备状态」的对象或文字 |
| BEQR-159 权限状态 | 适用｜速查表「权限状态：草稿、已启用、暂停、已停用、归档」。权限模板页只读显示状态，没有改状态的入口；后端也没有改权限模板状态的接口、库内现值只有 active。页面无从提供，属后端模块的事，登记 OBS-5｜通过（登记 OBS-5） |
| BEQR-166 合同审批链 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「合同审批链」的对象或文字 |
| BEQR-182 考勤日结果 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「考勤日结果」的对象或文字 |
| BEQR-183 打卡方式 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「打卡方式」的对象或文字 |
| BEQR-184 请假类型 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「请假类型」的对象或文字 |
| BEQR-185 请假单状态 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「请假单状态」的对象或文字 |
| BEQR-186 薪资单构成 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「薪资单构成」的对象或文字 |
| BEQR-187 员工提成发放 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「员工提成发放」的对象或文字 |
| BEQR-189 管理费方式 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「管理费方式」的对象或文字 |
| BSM-001 整体说明：这套系统到底管什么 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「整体说明：这套系统到底管什么」的对象或文字 |
| BSM-003 系统通用能力：数据可改可删、操作有引导、权限分岗 | 适用｜「权限分岗」：每个写按钮经 `PermissionButton` 按权限码禁用并拦截点击（现有组件渲染测试，010），权限码与 AccessController 门控码逐个一致；「操作有引导」：页内写动作失败时把后端中文消息报到页面（005，有测试）｜通过 |
| BSM-004 配置数据可以存档、也可以删除（删除要审批） | 适用｜存档、申请删除、批准/驳回删除、恢复入口齐全、理由必填；恢复按钮已存档写「取消存档」、已删除写「恢复」，与业务地图原句及 M031-001 后的行为一致（004，有测试）｜通过 |
| BSM-005 后台的"新建/录入"改成分步引导 | 适用｜新增岗位角色模板、发起授权申请、新建员工账号、新建权限模板四处为「＋按钮→分步引导弹窗→确认」，当步校验、失败留在弹窗内显错，有测试｜通过 |
| BSM-006 验证用的二维码一律动态刷新（间隔可在后台设定） | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「验证用的二维码一律动态刷新（间隔可在后台设定）」的对象或文字 |
| BSM-007 平台管理员拥有全部日常功能权限 | 适用｜前端不自行给平台管理员放行：`hasPermissions` 只认后端给的 grants/denies/wildcard，显式拒绝压过 wildcard（有测试）；后端现恒给 wildcard=false，该分支不会被走到｜通过 |
| BSM-009 基础管理 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「基础管理」的对象或文字 |
| BSM-028 主要功能 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「主要功能」的对象或文字 |
| BSM-029 和谁有关 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「和谁有关」的对象或文字 |
| BSM-072 商户管理：商户在授权范围内完整使用自己的系统 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「商户管理：商户在授权范围内完整使用自己的系统」的对象或文字 |
| BSM-073 主要功能 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「主要功能」的对象或文字 |
| BSM-074 和谁有关 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「和谁有关」的对象或文字 |
| BSM-084 员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「员工手机端：排班、考勤、请假、薪资，自己手机上都能办」的对象或文字 |
| BSM-085 日常工作 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「日常工作」的对象或文字 |
| BSM-086 排班、考勤与请假 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「排班、考勤与请假」的对象或文字 |
| BSM-087 我的薪资 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「我的薪资」的对象或文字 |
| BSM-088 员工提成、活动执行与人员服务边界 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「员工提成、活动执行与人员服务边界」的对象或文字 |
| BSM-114 各类入口：不同人只看自己该看的、只做自己该做的 | 适用｜现锁 V0.14 该条正文的变更只是员工手机端可离场办理事项里「考勤打卡」改为「查看考勤」，三个后台页不含考勤功能（探针 6：三页无「考勤」字样），该句无本面；「不同人只看自己该看的」部分同前：无权限的副资源不请求、各自降级（总结、权限模板、用户/角色清单），主表照常，有测试｜通过 |
| BSM-115 相互关系：为什么这些子系统必须连在一起 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「相互关系：为什么这些子系统必须连在一起」的对象或文字 |
| BSM-120 已确认规则和实施前输入项 | 无本面｜现锁 V0.14 该条变更只是业务地图页脚更新日期 09-08→09-10；本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「已确认规则和实施前输入项」的对象或文字 |
| GEN-001 必要性与最小性 | 适用｜三个页面无重复入口；`hasPermissions` 的 wildcard 分支现不会被走到但属与后端契约字段，保留｜通过 |
| GEN-002 可执行正确性 | 适用｜origin/main 一次性 worktree＋两轮 diff：本模块 11 个测试文件 60 个测试全绿、web 类型检查通过（探针）｜通过 |
| GEN-003 测试有效性 | 适用｜测试用真实 AdminApi＋打桩的 fetch 渲染并操作页面；本轮十个 ID 的行为均有能失败的断言（RED 14 条），权限闸组件有渲染测试（010）｜通过 |
| GEN-004 信任边界校验 | 适用｜必填与当步校验在页面侧先拦，后端再校验｜通过 |
| GEN-005 授权与最小权限 | 适用｜见 BSM-003：前端权限码与后端门控码一致；无权限按钮禁用且拦截点击｜通过 |
| GEN-006 数据完整性 | 适用｜权限模板编辑「读出→不改→保存」保持同一时刻（006，在 Asia/Shanghai 下有测试）；新建外部协作账号两步写入在套模板被拒时不再把已落库的账号当整笔失败——账号照常入表、提示写清（012，有测试）｜通过 |
| GEN-007 并发与幂等 | 适用｜写请求由客户端统一带幂等键；治理页写动作经 runWrite、模板页复制/应用经 runExclusive、其余写动作各有置忙——三个页面的写按钮提交中均禁用，连点不发第二笔（005、011，均有测试；探针 3 逐页清单）｜通过 |
| GEN-008 接口与模式一致性 | 适用｜请求路径、方法、字段名（客户端统一转 snake_case）与 AccessController 映射及请求记录类逐个核对一致；模板应用 mode 用 single_use/append，与后端实际取值一致｜通过 |
| GEN-009 错误处理与恢复 | 适用｜读失败、副资源失败有降级；治理页写动作统一经 runWrite 失败报后端中文消息（005）；账号页创建后套模板失败提示「账号已创建；套用权限模板失败：<后端消息>」并 setGlobalError（012，有测试）｜通过 |
| GEN-010 安全与敏感信息 | 适用｜初始密码输入为 password 类型；不显示手机号｜通过 |
| GEN-011 可观测与审计证据 | 适用｜每个写请求带 operatorNote；审批不填意见时留痕为中文「通过／驳回」（001）｜通过 |
| GEN-012 资源与性能边界 | 适用｜授权申请分页读取（每页 50），其余清单一次读取｜通过 |
| GEN-013 构建配置与依赖 | 适用｜依赖 React、antd Tabs 与本仓公共组件；无新增依赖｜通过 |
| GEN-014 数据库迁移正确性 | 无本面｜本模块不涉迁移 |
| GEN-015 界面可用性与可访问性 | 适用｜行可键盘聚焦与回车展开、表单控件有 aria-label 且不含 id/ID（002）；智能总结表表头与数据列数一致（007）；状态用词下拉与表格同一套（009）；取值全中文（001、003）｜通过 |
| GEN-016 文档可验证性 | 适用｜页面说明文字与实际功能逐句对照一致（恢复按钮文案已改，004）；员工账号页标题说明提到「企业微信同步结果」「禁用」而页内无此内容（登记）｜通过（登记） |
| GEN-017 业务来源分类 | 适用｜未见页面自造业务规则；岗位码中文名是页内 6 项小映射（OBS-1）｜通过（登记 OBS-1） |
| GEN-018 第一性原理证据 | 适用｜结论落到页面源码行、HTML 原文、后端门控与库内闭集的只读查询、vitest 与 node 实跑输出｜通过 |
| PGM-001 整体定位：平台治理层不是一个业务部门，而是所有业务的底座 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「整体定位：平台治理层不是一个业务部门，而是所有业务的底座」的对象或文字 |
| PGM-002 治理顺序：一件业务能不能做，要先经过这些判断 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「治理顺序：一件业务能不能做，要先经过这些判断」的对象或文字 |
| PGM-003 模块入口：平台治理层包含哪些管理模块 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「模块入口：平台治理层包含哪些管理模块」的对象或文字 |
| PGM-004 综合体和集团 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「综合体和集团」的对象或文字 |
| PGM-005 空间和点位 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「空间和点位」的对象或文字 |
| PGM-006 商户和主体 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「商户和主体」的对象或文字 |
| PGM-007 合同和准入 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「合同和准入」的对象或文字 |
| PGM-008 员工和手机 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「员工和手机」的对象或文字 |
| PGM-009 权限和职级 | 适用｜权限和职级模块的三个后台页：访问治理工作台、员工账号、权限模板｜通过 |
| PGM-010 终端开关 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「终端开关」的对象或文字 |
| PGM-011 设备管理 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「设备管理」的对象或文字 |
| PGM-012 商户侧 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「商户侧」的对象或文字 |
| PGM-013 开放接口 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「开放接口」的对象或文字 |
| PGM-014 留痕复核 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「留痕复核」的对象或文字 |
| PGM-015 楼宇运营 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「楼宇运营」的对象或文字 |
| PGM-016 应急与隐私 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「应急与隐私」的对象或文字 |
| PGM-017 综合体和集团：当前一个项目，未来能看集团 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「综合体和集团：当前一个项目，未来能看集团」的对象或文字 |
| PGM-018 物理项目与运营公司：多对多协作，不是固定隶属层级 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「物理项目与运营公司：多对多协作，不是固定隶属层级」的对象或文字 |
| PGM-019 各主体具体负责什么 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「各主体具体负责什么」的对象或文字 |
| PGM-020 主要功能 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「主要功能」的对象或文字 |
| PGM-021 业务意义 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「业务意义」的对象或文字 |
| PGM-022 空间和点位：所有业务都要知道“发生在哪” | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「空间和点位：所有业务都要知道“发生在哪”」的对象或文字 |
| PGM-024 影响业务 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「影响业务」的对象或文字 |
| PGM-026 主要功能 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「主要功能」的对象或文字 |
| PGM-027 业务边界 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「业务边界」的对象或文字 |
| PGM-031 外部经销商主体、合同价格与资金治理 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「外部经销商主体、合同价格与资金治理」的对象或文字 |
| PGM-032 员工和手机：人、私人手机、工作手机要能对应起来 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「员工和手机：人、私人手机、工作手机要能对应起来」的对象或文字 |
| PGM-033 主要功能 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「主要功能」的对象或文字 |
| PGM-034 工作手机和企业微信 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「工作手机和企业微信」的对象或文字 |
| PGM-035 提成发放和人员服务治理 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「提成发放和人员服务治理」的对象或文字 |
| PGM-036 设备管理：谁领了什么设备，设备能不能用 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「设备管理：谁领了什么设备，设备能不能用」的对象或文字 |
| PGM-040 权限和职级：每一个单一功能都可以单独授权 | 适用｜权限点逐个勾选（按菜单模块分组），角色级模板与员工级单独授予/显式拒绝均有入口｜通过 |
| PGM-041 权限规则 | 适用｜临时授权有效期/单次、高危授权审批、临时权限总结均有入口；授权申请状态与类型按后端闭集全中文（001）；权限模板编辑原样保存保持同一时刻（006）｜通过 |
| PGM-042 职级规则 | 适用｜职级按 M1–M10 上中下显示；定岗表单可填基础薪资（「具体薪资单独填写」）｜通过 |
| PGM-043 外包人员与审计人员（2026-06-29 新增） | 适用｜现锁 V0.14 该条变更是新增「导出文件是否脱敏由系统按内容自动判定并强制执行」整段：三个后台页没有任何导出功能，也不展示姓名之外的个人信息字段（探针 6：三页无「导出」「脱敏」「明文」字样），该段无本面；外包人员部分同前：员工走注册申请、后台手动建号只留外部协作机构账号，并可绑定/解绑外部协作机构，与后端 STAFF_REGISTRATION_REQUIRED 一致｜通过 |
| PGM-044 桌台、挂单与经销业务权限 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「桌台、挂单与经销业务权限」的对象或文字 |
| PGM-048 商户侧：在授权业务范围内全开放 | 适用｜商户管理员创建账号时后端按其商户收口；页面对平台账号提供「不绑定 / 平台账号」｜通过 |
| PGM-059 客人与员工的个人信息保护 | 适用｜现锁 V0.14 该条变更是「商户档案第一/第二联系人手机号经单独授权可看完整号码」的例外——只适用于商户档案联系人、明文不扩大到员工档案；本模块账号表与详情抽屉不显示任何手机号（有测试断言 not.toContain 手机号），不涉商户档案，例外无本面；「员工日常界面只显示脱敏号码」部分同前｜通过 |
| PGM-060 会员身份、营销与生物信息治理 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「会员身份、营销与生物信息治理」的对象或文字 |
| PGM-065 连接关系：平台治理层怎么连接所有业务 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「连接关系：平台治理层怎么连接所有业务」的对象或文字 |
| PGM-066 已拍板规则和待排期问题 | 适用｜范围变更处有「模板内既有高危不免审」的中文说明；直接装配高危角色被后端拒绝时页面报出原因（005）；界面文字全中文（001、002、003）｜通过 |

## 5. 已定稿 001…012 现态

全部已修（B3-r1、B3-r2），B4-r1、B4-r2 双 PASS；修复队列「是」= 0。

## 6. 本次 Findings

无新增。XM-M038-A 登记未变。

## 7. 结论

席甲 B5-r2 票：**PASS**。16 个文件全读、106 判据逐项、取证脚本实跑复核；未改任何已跟踪文件；落盘前未读 Grok 本次正文。

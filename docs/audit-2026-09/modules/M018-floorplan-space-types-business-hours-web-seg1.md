# M018 空间点位、楼层平面图与营业时段／网页与验证／第1段
版本：V0.01

2026-09-20 首次审计收口（操作员 2026-09-20 指令：严格按 V0.17，逐模块检查点）。7 文件（1353 行）、21 判据；B1 双 FAIL（Grok GR-M018-01…09、席甲 CX-M018-001…006）→ B2-r1 双 PASS 定稿 M018-001…009（001/003/007/008/009 细化、002 冲突、004/005/006 技术性；GR-06/07 并入 M015-010/011 XM）→ B3-r1 → B4-r1 双 PASS → B5-r1 合取 FAIL（Grok GR-M018-B5-01：仅 floor_plan.view 账号首屏被 /platform/projects 403 整页拖死；席甲票撤回）→ B2-r2 定稿 M018-010 → B3-r2 → B4-r2 双 PASS → B5-r2 双 PASS：Grok V0.01 SHA256 82b97ee3ef9f68a7f4d335f67d078d9f2f5e959422d3978a1b72f6efba31c778；席甲 V0.01 SHA256 a5494ad03480bada81fdd505a6221d764215c6ab2bdc2573cd5c433d12bf870e；四值全等 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。B3 累计 2。

修复摘要：新建平面图请求体与后端必填契约一致（楼层/底图/宽高/版本号，status active）；页内枚举字典改为后端闭集（平面图/任务/候选状态、错误字段/错误码、关联类型），不再英文直出；创建导入/发布按钮用 floor_plan.import.manage、下钻按 import.view 隐藏；下钻失败显错；幂等键为页面键；来源名称必填；未勾选不发布；已删除空间种类可恢复；项目清单改副资源（无 governance.project.view 不请求、失败只降级）；四测试文件按真实闭集与各 ID 用例重写。

跨模块观察：M015-010/011 前端部分待后端同批；PR #439 后端部分（PlatformService 空间种类生命周期、V424–V426）随本模块检查点合并入 origin/main，属相应后端模块审计对象；`approveDeleteSpaceType` 控制器无 requirePermission（后端）。席甲教训：B1/B5 均须把首屏每个请求的后端权限码与 scope 分开核，不以 fetch 桩 200 当真契约。

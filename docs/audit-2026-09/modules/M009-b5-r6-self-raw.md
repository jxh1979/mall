CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
# M009 B5-r6 Codex 整模块及发布依赖独立复审
版本：V0.01

本席B5结论：PASS。当前模块IN_PROGRESS，B3累计6，未用单席票收口/发布。
Grok本轮B5先派发；本方完整报告封存前未读取M009-b5-r6-grok-raw.md及其CLI正文。仅本席+固定Grok，没有第三席。
本次B2 self V0.21/Grok V0.18双PASS；第6次B3仅060/061/062；B4-r6完整diff与归因双PASS后findings V3.57三项置否，现30项均无待修。B5不是仅审diff。

## 范围与阅读方式
冻结FILES20文件当前3381行；20SHA逐个实算与B5-r5全量阅读及本次B1一致，本轮复用同字节已读源码，未冒称又新读3381行。全88冻结判据逐项重新判断，逐项结果如下；原文hash不变，未以运行代码反造业务判据。
必要依赖本次阅读候选相对origin/main差异：ProjectRecord/ProjectAssociationRequest/V431/V432/Leasing、Authorization/JdbcAccess、Auth六类、SupervisorReview请求/服务、PII门禁及关联测试。候选1199生产Java及399SQL逐字与正本相同，不以“能编译”代替运行时依赖检查。
候选.scratch/publish-m009-m011-20260922已FF到d8f12c33766c2a0f39a45d5f4b8d66f6753804e4，不漏已合并M010报告。未带M012网页在制改动。
技能使用延续本轮既有预检与V0.17收窄：grilling自问、代码流/反例验证、receiving核实、verification实跑证据、最小修法；不派子代理，不额外创建技能要求的产物，不假称新调用。

## 第一性原理 / 顺从 / 对抗
项目资料、公司多对多、商户项目与四主体绑定承载已批HTML要求；DTO/SQL/分页/加密是承载方式，不因HTML没有Java名就发明“添加”。
五组读端与COUNT项目谓词、父空间/商户同项目、创建/激活锁同商户、未来绑定inactive不提前投影、非空三主体不能普通改、删除申请与恢复、占用和理由审计均在同字节实现，26旧项仍已处理或登记。
七响应路径L66/73/83/92/112/121/130调用同helper：成功核原功能权及可选电话权后一次消费，同请求mask不用消费后再查布尔；无电话权只原功能+脱敏。grant_group仓储消费与requirePermissions来源去重相合，失败不执行业务服务；日常管理员不冒充密钥。
两姓名通过现cipher新建/保存加密读解密，V431 text承载信封；旧行不批量回填。项目3新资料可空，仅后端/DB，无前端已可用声明。
迁移编号不是业务需求：原427在已发布430之后的正常升级会直接红；改名432且两隔离库分别证实正常升级与全新安装。原complex_test历史和数据不因发布篡改。
两派生文件严格来自现生成器，CSV版本噪音已逐行机器比较，算法/status不变；规格AUTO外仅自身版本，不暗改业务条款。
对抗保留：真实master必须active+保留id/name/type；普通高职级最多上两级；无商户/项目默认拒绝不是无限范围；审批申请人/直接受益人仍不得自审；缺beneficiary先拒。无新增跨域通行、关闭隔离或导出明文授权。

## 全20源指纹
- backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java：17行；SHA256 45431c580d0bca0c217bcdb8ea120604eb7ed2b441845a5ac52257d5130b1277。
- backend/src/main/java/com/complex/platform/governance/BusinessEntityBindingRecord.java：19行；SHA256 f97af4de8ed4d45134196b4fcb83169591d5991e6aefaa4aa22411a49e78f422。
- backend/src/main/java/com/complex/platform/governance/MerchantRecord.java：116行；SHA256 dda319c37032c33c3cc5d260a0c9f43c7740d21b80a106c4a4efd49a19cdef00。
- backend/src/main/java/com/complex/platform/governance/MerchantDeleteDecision.java：9行；SHA256 e08618c856b35a1eda8447a9c9bd0c88ed00a9ee371c1ee33d3f2f3ab7ce5e40。
- backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java：1338行；SHA256 84c868bb9739fc68d630ac6c7da1c99767c7a974ba2e763102ba473f52618855。
- backend/src/main/java/com/complex/platform/governance/PlatformController.java：302行；SHA256 53d38fcb4910ba0de5acc7418936d3388673b662a0b0a427dced54b5baff0bc6。
- backend/src/main/java/com/complex/platform/governance/PlatformRepository.java：204行；SHA256 df6dfee9894fc34f1df28fdae3d549ba50320df14219129779fa267affb08cdc。
- backend/src/main/java/com/complex/platform/governance/PlatformService.java：1179行；SHA256 993cfdb8cdacb285a37027c8bfda1c983fcb5c3e0f26f7462f2235454f45827e。
- backend/src/main/java/com/complex/platform/governance/package-info.java：1行；SHA256 e3094c51b508bf10015ead9e92ac2ae2f3edf3b09786d88816ab8009d21921d9。
- backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityBindingRequest.java：17行；SHA256 66d4a8cd5c7f206f0c023e190ac5ed94ff6585263a737e35f062be9d8de211da。
- backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java：26行；SHA256 18911088cbde9575ca7c38f9a6ac6eee3f4a812098aea9a29482133fe14dec19。
- backend/src/main/java/com/complex/platform/governance/dto/MerchantArchiveRequest.java：6行；SHA256 e0f16f5417b61e801ed23803616450e90810853ec7588a49714b43040d809f93。
- backend/src/main/java/com/complex/platform/governance/dto/MerchantDeleteApprovalRequest.java：5行；SHA256 2516fb1d78e082fe89222a41f7178dde4b40d749c5ed2c2e712bbb14a74112c4。
- backend/src/main/java/com/complex/platform/governance/dto/MerchantDeleteRequest.java：5行；SHA256 4898e50f2150e27c73d6d400b317d792f02c342a0e95a8b3dadbef5a7ebc8f45。
- backend/src/main/java/com/complex/platform/governance/dto/MerchantProfileRequest.java：41行；SHA256 7b6dd241328b57dc646eb9b3c531e01e3a3a3d1bb1b848711c7ce43106973cf1。
- backend/src/main/java/com/complex/platform/governance/dto/ProjectRequest.java：26行；SHA256 6ac09b14c1766bebd6df69fbf5579ccf602959d10179b497ff90ea1b3f521108。
- backend/src/main/java/com/complex/platform/governance/dto/SpaceRequest.java：21行；SHA256 63857df14972fd6904a32fc7aade0de49b88ec3b946df493f61086c77d1ab7aa。
- backend/src/main/java/com/complex/platform/governance/dto/SpaceTypeRequest.java：16行；SHA256 7c3a5fa28ab5a45b49d3fc325e39d1f25ddcbc38b86de3872ca86ce338b05f34。
- backend/src/main/java/com/complex/platform/governance/dto/StatusChangeRequest.java：11行；SHA256 75b75cfaee897096f6fce0886fc4e71da8c5762774a901739cff244e1425f60f。
- backend/src/main/java/com/complex/platform/governance/dto/VenueBusinessHoursRequest.java：22行；SHA256 6347c6dfe087be2310fe5f618a0ac27e188f340d1af84add2ab831e98cb333f1。

## 全88判据
- BEQR-063：酒吧高危动作无本模块执行入口；共享审批仓储不自行定义沽清或双证据规则。
- BEQR-074：隐私桌台不在本模块；商户档案权限不冒充桌台三类权限。
- BEQR-143：BindingRecord四类字段及Service创建/激活；同merchant行锁+未结束记录检查保持唯一。
- BEQR-144：business_entity_projects多对多，无固定父公司链。
- BEQR-145：主体角色区分project_company，无独立资产业态或统管其它经营域逻辑。
- BEQR-146：六业态技术代码没有新增运营公司权责或跨域管理。
- BEQR-147：不定义收入拆分/平台费，绑定不自动生成收入。
- BEQR-148：六合作方式白名单；其他收费模板生效检查属合同模块，本模块不放行合同。
- BEQR-149：无新增职级；删除审批凭真实master而非最高职级。
- BEQR-150：无终端增减，控制器为现有员工后台。
- BEQR-151：requirePermissions沿用拒绝优先和有效来源；本次不改变权限计算规则。
- BEQR-152：Service.currentProject与所有范围分页谓词一致；未知项目空集、越域写拒绝；页面查看无导出能力。
- BEQR-153：不实现终端开关，不引绕过或留痕关闭开关。
- BEQR-154：项目六态白名单与HTML一致。
- BEQR-155：商户业务状态与存档轴分开；直接退出被拒绝，无新立即终止路径。
- BEQR-156：无合同状态执行入口，不改变合同当地日期规则。
- BEQR-157：无员工状态写入路径。
- BEQR-158：无设备状态写入路径。
- BEQR-159：无权限配置状态新增；消费复用现有引擎。
- BEQR-166：审批链仓储保存与CAS承载既有模型；不声称本模块完成合同三类流程。
- BEQR-189：无服务管理费收取/费率配置路径。
- BSM-001：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- BSM-003：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- BSM-004：商户和空间类型archive/delete/restore、理由占用审批及真实master路径符合本模块范围。
- BSM-005：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- BSM-006：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- BSM-007：日常管理员不自动享PII；真实密钥身份独立；单次已修。
- BSM-009：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- BSM-028：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- BSM-029：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- BSM-072：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- BSM-074：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- BSM-114：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- BSM-115：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- BSM-120：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- GEN-001：现有授权、会话、仓储承载既有规则；本次B3仅迁移纯改名及两个生成清单，不引新功能/依赖。
- GEN-002：同字节候选既有10类299测试有效；本次空库399与430升级2条实际成功，编号闸/两清单check及完整predeploy实际exit0。区分旧测试与新命令。
- GEN-003：已有列表/写回、单独/同组单次、失败拒绝断言保持；本次增加的执行证据是独立库红转绿与原字段哈希/NULL，不造测试框架。
- GEN-004：DTO校验/服务范围、枚举、理由、非负、引用；SQL使用绑定参数。
- GEN-005：七响应路径合并原功能权和可选PII权；无PII权仍原功能+脱敏；真实master/显式DENY不改。
- GEN-006：软删除和归属保留；名字写加密读解密；项目新三字段可空；无旧数据已全回填声称。
- GEN-007：现有幂等执行器、merchant行锁、单次来源去重/CAS继续使用；同授权组不提前被分拆消耗。
- GEN-008：DTO/Record/JDBC往返字段一致，响应分页与掩码保持形状。
- GEN-009：消费失败先拒绝、不会返回明文；非法值异常保持中文；DB凭据失败纠正环境后仅补失败测试。
- GEN-010：两个电话号码受同一请求内成功许可控制；本批不扩到其它PII；不把布尔查询改为全局有副作用。
- GEN-011：merchant.viewed包含本次对象IDs/入口/项目/筛选/分页；既有单次消费usage日志不绕过。
- GEN-012：稳定分页、500技术上限、long偏移；无每商户行重复消费。
- GEN-013：现Spring/JDBC/Flyway12.4和原生成器无新依赖；候选完整predeploy及编号门禁绿；未声称全后端或部署。
- GEN-014：原V427未发布编号缺陷060已纯改名V432；空库全399、430升级431/432两条均validate成功；旧complex_test历史不变，明确不是再运行432的目标。
- GEN-015：无网页文件；中文校验保留；未声称新增项目页面已可用。
- GEN-016：清单反映当前336表及迁移V432的172张scope表；本轮20文件3381行88判据和30ID完整重新判定；旧行不回填/仅后端范围不变。
- GEN-017：主体/档案为HTML细化、分页SQL技术性；058冲突已按批准方式修复；未检出新添加项。
- GEN-018：按20源hash复用本次B1及B5-r5已全文阅读的内容，非冒称新读3381行；本批全tree diff双审、隔离库实跑、候选依赖diff及同字节核验，当前Grok本轮未读。
- PGM-001：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-002：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-003：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-004：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-005：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-006：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-007：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-008：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-009：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-010：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-011：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-012：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-013：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-014：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-015：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-016：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-017：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- PGM-018：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- PGM-019：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- PGM-020：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- PGM-021：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- PGM-026：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- PGM-027：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- PGM-031：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-035：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-040：七入口保留各自原单功能码，电话权限可选而非新强制权。
- PGM-041：L656不改：requirePermissions一起核验并消费同来源一次；重复调用无权或脱敏，失败不执行服务。
- PGM-042：职级与最高权限身份分开；未按职位冒充密钥。
- PGM-043：L680查看例外不赋予导出/打印；20文件没有新增导出打印接口，查看审计保留。
- PGM-044：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-048：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-059：L871例外只商户两个联系号码，普通管理员不自动享有；平台/项目范围保持，两个名字保存加密；旧行不回填限制已批准。
- PGM-060：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-065：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-066：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。


## 全30定稿ID
- M009-038：项目隔离及多对多主体关联有效，master不整类关闭隔离。当前本批队列否，历史修复事实保留。
- M009-039：空间当前项目与父空间、商户一致。当前本批队列否，历史修复事实保留。
- M009-040：普通资料修改不能换非空财税主体；不补审批系统。当前本批队列否，历史修复事实保留。
- M009-041：未来绑定不立即投影，排他行锁及日期有效。当前本批队列否，历史修复事实保留。
- M009-031：非空财税主体不能借停用清空，历史绑定不回写。当前本批队列否，历史修复事实保留。
- M009-042：disabled空间类型走已有恢复入口。当前本批队列否，历史修复事实保留。
- M009-043：对象/理由/决定审计元数据存在。当前本批队列否，历史修复事实保留。
- M009-044：pending申请批准/驳回更新最终状态。当前本批队列否，历史修复事实保留。
- M009-045：真正到期且inactive绑定排除占用，未来inactive不排除。当前本批队列否，历史修复事实保留。
- M009-046：面积、租金、容量非负校验，未发明上限。当前本批队列否，历史修复事实保留。
- M009-030：新建给定主体先校验当前项目存在。当前本批队列否，历史修复事实保留。
- M009-047：用户展示校验语句中文，内部enum保持内部码。当前本批队列否，历史修复事实保留。
- M009-018：普通档案路径不能直接terminated，不新增退租流程。当前本批队列否，历史修复事实保留。
- M009-013：三项目字段已保存返回、可空、后端DB范围；旧数据无自动填充。当前本批队列否，历史修复事实保留。
- M009-054：此前岗位模板入口修正作为既有输入，不跨模块重修。当前本批队列否，历史修复事实保留。
- M009-055：此前商户范围测试前提修正保留，不放宽生产隔离。当前本批队列否，历史修复事实保留。
- M009-056：此前受益人测试资料修正保留，不绕过利益冲突。当前本批队列否，历史修复事实保留。
- M009-048：项目范围商户列表为HTML下位细化。当前本批队列否，历史修复事实保留。
- M009-049：既有审批链仓储为下位实现，本模块不发明角色链。当前本批队列否，历史修复事实保留。
- M009-050：电话加密基础有效，与058单次遗漏和059姓名分别处理。当前本批队列否，历史修复事实保留。
- M009-051：稳定分页及上限500/long偏移技术实现有效。当前本批队列否，历史修复事实保留。
- M009-052：拒绝直接DELETE、走软删除审批为细化。当前本批队列否，历史修复事实保留。
- M009-053：包声明技术性。当前本批队列否，历史修复事实保留。
- M009-057：空间类型注释与实际create行为一致。当前本批队列否，历史修复事实保留。
- M009-058：当前缺陷否、队列否。七入口改用已有requirePermissions，同组单次仅一次消费，独立电话单次第二次脱敏；原查看metadata保留，获批HTML例外有效。
- M009-059：主次联系人姓名新建/修改加密并读取解密；旧行未重存仍不保证加密，不回填。当前本批队列否，历史修复事实保留。

- M009-060：纯改名保留SQL SHA，编号闸及两种隔离库路径绿；B4双PASS后当前缺陷否、队列否。
- M009-061：原生成器版本常量/CSV机械刷新，336表、check绿、全表排除版本后唯一机械变化已核；当前缺陷否、队列否。
- M009-062：AUTO变V432/172及标题版本、手写条款不变、check绿；当前缺陷否、队列否。
- M009-063：原complex_test已427/431历史不改、验证拆独立库；不是待修缺陷，仍否/否。

## 发布依赖复核（既有授权积压，不是本次B3新增功能）
- AuthorizationService：真实master从platform_admin独立；scope不因普通平台管理员变empty全量。M009范围守卫与Controller requirePermissions使用该语义。不能只发M009而保留旧master桥。
- SessionIssuer/MasterAuthorityService/AuthService/ProductionAuthService/两Filter：master_authority类型、带deviceId签发、不能刷新、换公钥重启撤销旧主会话、session/device请求属性。签发侧与消费识别必须一起发布；不因新增字段编译能过就删运行路径。现代码差异为既有正本，不是r6改写。
- JdbcAccessRepository：temporary grant来源为grant_group_id，CAS一次消耗同组；空project/merchant范围拒绝。058要求一个组跨功能+电话同次消费，不能遗漏该依赖。
- SupervisorReviewEvaluateRequest/Service：请求beneficiary及既有兼容构造，评估/裁决/转发防自益，角色/真实最高权限分开，review读取范围守卫。普通高职级不能补成master，角色节点的上两级闸与Authorization一致。完整依赖diff已读，不裁剪相邻既有修复来凑更小包。仅评估能否承载本批，不宣称所属其它模块全审完。
- ProjectRecord/ProjectRequest原构造兼容+3字段、ProjectAssociationRequest专用归属请求与V432实际列吻合；Leasing新建不先写三主体，按intent.projectId归属，再调用权威绑定，避免已批040/041被签约路径绕开。
- PII门禁只新增商户两个联系人姓名列，与059批准范围一致；没有把owner/legal/生日擅自纳入。
- M011三测试当前2776行的已完成双席B1/B5证据保留；本轮发布三测试同字节，后端299证据包括Controller19+Service70+JDBC24。不能把以前单席补查冒充本轮双席，当前2026-09-22两席报告见原M011收口最后一节。
发布依赖没有本轮新增代码修改，旧观察不升修复。candidate所带Auth/Review既有变化的完整必要性及独立外审核验仍待Grok票合取；本席无阻断项。

## 实际验证与证据边界
1. 本次r6真实执行：编号闸exit0；两个生成器--check exit0；候选完整npm run verify:predeploy exit0（含18PII列检查、隔离无新增回归、OpenAPI/客户端/安全等原门禁）。
2. 本次Flyway12.4：升级隔离库430→432只执行431/432两条，validate399；空隔离库执行全399，validate399；均exit0。项目剔除新增3字段md5仍1cd4ebc13a6720b8ab81224c434a06b5，新增NULL；商户剔除project_id md5仍6cfce3db5a2697d96a0675397628b861，project_id NULL；两姓名text；bep PK/2FK/index有效。
3. 原complex_test只有只读核验，399条history无分隔row_to_json聚合MD5仍6258acd4a7f89f16d545dcf0048bbbc5；427/431仍在，无432。禁止repair/删history/IF NOT EXISTS/outOfOrder/ignore，不称旧库已切换。
4. 以下10类299是本次发布准备阶段先前真实clean测试留下的XML，新核验其零fail/error/skip与源码未变。本B5没有再次执行Maven；本次编号改动由独立Flyway补证，不把旧复杂库重复跑432。首次候选增量旧class引起的Review测试失败已由同源clean299绿排除，未改业务刷绿；凭据初错与正确配置补跑保留历史。
- com.complex.platform.access.AuthorizationServiceTests 25/0/0/0 SHA256 7b1ec52736fdc7f0c0cbf45e7ba5121a31888d127b0d96a3aced9248ffc7b3a0
- com.complex.platform.access.M006SecurityRegressionTests 39/0/0/0 SHA256 565e7cc8d4a1e1de2ac2aa28548d1757efcb33d79a0c9567a779793036ad191b
- com.complex.platform.access.staff.M010RegressionTests 44/0/0/0 SHA256 d590193a07a491224241a95425de9bf450114195f723d5fda0ac681ce41fbe72
- com.complex.platform.auth.AuthServiceTests 11/0/0/0 SHA256 7c2628242c907255dbf07bdb61901e2816750e1a4340931a63ce5f9f90891c8d
- com.complex.platform.auth.MasterAuthorityServiceTests 12/0/0/0 SHA256 aa6281a12ae2e86e47df0d36f26cbec627f059ecc0c751d57e6da123b9040877
- com.complex.platform.governance.PlatformControllerTests 19/0/0/0 SHA256 aee06aaf294ecf8fff27831081ec3f86a72f64a664fbb728479ccece255138d2
- com.complex.platform.governance.PlatformJdbcIntegrationTests 24/0/0/0 SHA256 bf98ade1f319634610e9364cee1f6c0603089ba61d5c3fd6aa35784708958da7
- com.complex.platform.governance.PlatformServiceTests 70/0/0/0 SHA256 6528d894e86206d8f5506a25872ee76a41c118eb9fb9d69a6e9e5682dc539c98
- com.complex.platform.governance.review.SupervisorReviewServiceTests 44/0/0/0 SHA256 9474b44e0a22e5b9ac261a67daf2ad0ae84e39e405def264be07993c03ae797e
- com.complex.platform.leasing.LeasingServiceTests 11/0/0/0 SHA256 75db2a39a79c3a93185314113b7024f18c79d12072adbaf82024a4f3c2f4bbdf
命令（先前实际有效运行）：候选backend中mvn -o -q clean -Dtest=PlatformControllerTests,PlatformServiceTests,AuthorizationServiceTests,AuthServiceTests,MasterAuthorityServiceTests,M006SecurityRegressionTests,M010RegressionTests,LeasingServiceTests,SupervisorReviewServiceTests,PlatformJdbcIntegrationTests test。数据库env仅既有3项，URL当时complex_test，无秘密输出。
没有全后端测试、页面端到端、生产部署、未审模块完成或旧PR积压清零的声称。

## 观察、版本与完整性
历史观察/XM全保留：空间类型并发双新建、权限种子、其它控制器PII、其它姓名生日/脸票比对/测试库卫生、旧字段不回填等未纳入本轮修复。V427编号旧红060本次真实消除，不再作为未修。其它M426/M435/M436/M409/M415历史记录不因局部门禁绿就伪改全仓状态。
三HTML与criteria-lock未改，LOCK-AMEND-COUNT1。实算/sidecar/ledger现值同首行；第四值等Grok本次完整B5真正交付后核。index d9a65108a1e87bd300c299ca7640f832bd5c2feb771f35a7a2413d32a1748331未变。
B3授权只执行一次到6，未新增B3；B5期间没有修源码/业务。所有30ID当前均否；没有新的添加、HTML申请、待答问题或未归因代码行。
本席B5 PASS。完整封存后才读取Grok；须双PASS及四值全等后，先登记ledger PASS再写收口，随后执行用户授权提交推送合并。

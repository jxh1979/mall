CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M009 B5-r4 Codex 整模块独立复审
版本：V0.01

本次完整报告。审计席甲 Codex；B3累计4（操作员追加仅1次已执行）。已先派Grok；截至本报告封存未读取本次Grok B5 raw/JSON/正文。范围为471计划M009全部20文件3375行，不以修复diff代替整模块。当前HEAD e456ba96c84185c8d39f18f548011daac48a6466；锁不修改；三业务HTML不修改。

## 三线方法与覆盖
第一性原理：逐文件从DTO与记录、Controller权限、Service对象归属/状态/主体绑定、Repository持久化与返回、审计跟踪到直接依赖AuthorizationService的权限消费差异。所有20文件全部读取，不以旧PASS免审。
顺从式：逐项检查下表87冻结判据；不属于本20文件职责的业务明确边界，不凭档案存在宣称相关下游已完工。
对抗式：核查跨项目、商户级平台访问、非法/负数输入、未来绑定立即投影、重复活跃槽位、财税普通修改、历史引用占用、软删恢复、审批身份、自审、审计对象遗漏、空项目、空分页及密文开销；真实新增反例已纳入本次红绿测试；手机号布尔权限/单次消费仍可绕过，详见CX-B5-01。未在B5修复任何代码。

## 全部文件覆盖
|文件|完整读取行数|当前SHA256|
|---|---:|---|
|backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java|1–17|45431c580d0bca0c217bcdb8ea120604eb7ed2b441845a5ac52257d5130b1277|
|backend/src/main/java/com/complex/platform/governance/BusinessEntityBindingRecord.java|1–19|f97af4de8ed4d45134196b4fcb83169591d5991e6aefaa4aa22411a49e78f422|
|backend/src/main/java/com/complex/platform/governance/MerchantRecord.java|1–116|dda319c37032c33c3cc5d260a0c9f43c7740d21b80a106c4a4efd49a19cdef00|
|backend/src/main/java/com/complex/platform/governance/MerchantDeleteDecision.java|1–9|e08618c856b35a1eda8447a9c9bd0c88ed00a9ee371c1ee33d3f2f3ab7ce5e40|
|backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java|1–1338|84c868bb9739fc68d630ac6c7da1c99767c7a974ba2e763102ba473f52618855|
|backend/src/main/java/com/complex/platform/governance/PlatformController.java|1–296|9e4cef62553160bbd9b2c8bba8779feadec81a4dd26cf3d28664fb160777c3d2|
|backend/src/main/java/com/complex/platform/governance/PlatformRepository.java|1–204|df6dfee9894fc34f1df28fdae3d549ba50320df14219129779fa267affb08cdc|
|backend/src/main/java/com/complex/platform/governance/PlatformService.java|1–1179|993cfdb8cdacb285a37027c8bfda1c983fcb5c3e0f26f7462f2235454f45827e|
|backend/src/main/java/com/complex/platform/governance/package-info.java|1–1|e3094c51b508bf10015ead9e92ac2ae2f3edf3b09786d88816ab8009d21921d9|
|backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityBindingRequest.java|1–17|66d4a8cd5c7f206f0c023e190ac5ed94ff6585263a737e35f062be9d8de211da|
|backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java|1–26|18911088cbde9575ca7c38f9a6ac6eee3f4a812098aea9a29482133fe14dec19|
|backend/src/main/java/com/complex/platform/governance/dto/MerchantArchiveRequest.java|1–6|e0f16f5417b61e801ed23803616450e90810853ec7588a49714b43040d809f93|
|backend/src/main/java/com/complex/platform/governance/dto/MerchantDeleteApprovalRequest.java|1–5|2516fb1d78e082fe89222a41f7178dde4b40d749c5ed2c2e712bbb14a74112c4|
|backend/src/main/java/com/complex/platform/governance/dto/MerchantDeleteRequest.java|1–5|4898e50f2150e27c73d6d400b317d792f02c342a0e95a8b3dadbef5a7ebc8f45|
|backend/src/main/java/com/complex/platform/governance/dto/MerchantProfileRequest.java|1–41|7b6dd241328b57dc646eb9b3c531e01e3a3a3d1bb1b848711c7ce43106973cf1|
|backend/src/main/java/com/complex/platform/governance/dto/ProjectRequest.java|1–26|6ac09b14c1766bebd6df69fbf5579ccf602959d10179b497ff90ea1b3f521108|
|backend/src/main/java/com/complex/platform/governance/dto/SpaceRequest.java|1–21|63857df14972fd6904a32fc7aade0de49b88ec3b946df493f61086c77d1ab7aa|
|backend/src/main/java/com/complex/platform/governance/dto/SpaceTypeRequest.java|1–16|7c3a5fa28ab5a45b49d3fc325e39d1f25ddcbc38b86de3872ca86ce338b05f34|
|backend/src/main/java/com/complex/platform/governance/dto/StatusChangeRequest.java|1–11|75b75cfaee897096f6fce0886fc4e71da8c5762774a901739cff244e1425f60f|
|backend/src/main/java/com/complex/platform/governance/dto/VenueBusinessHoursRequest.java|1–22|6347c6dfe087be2310fe5f618a0ac27e188f340d1af84add2ab831e98cb333f1|

## 87判据逐项核查
|判据|当前证据与结论|
|---|---|
|BEQR-063|本范围不执行反结/沽清/存酒；Controller未提供此类绕过接口，审批设施不替代酒吧业务规则。|
|BEQR-074|Space档案不承担隐私桌台查询授权；仅维护归属，本范围无隐私桌台查看接口。|
|BEQR-143|Service762–872四主体关系与槽位校验；未来绑定不投影、财税主体普通变更拒绝；记录保持独立。|
|BEQR-144|Repository实体项目关联表/assignEntityProject允许多对多；未恢复固定父链。|
|BEQR-145|BusinessEntityRequest/Service主体类型约束无独立spv父链，不赋城市项目公司跨域结算权。|
|BEQR-146|实体档案只记录主体类型/归属；未按城市项目默认接管其他域定价或票务。|
|BEQR-147|Service财税主体改变拒绝普通档案入口；未新增平台收费、收入比例或历史投影改写。|
|BEQR-148|Service校验六种合作方式；合同生效前其他理由/收费模板属于合同流程，本档案枚举不能证明合同门禁通过。|
|BEQR-149|本范围不定义职级；Controller调用既有AuthorizationService，不把职级当功能授权。|
|BEQR-150|统一Controller并非新增终端；设备/总开关由既有拦截器接入，非本模块新增十一端。|
|BEQR-151|requirePermission走既有授权；读取phone使用布尔grant查询，单次消费漏点CX-01。|
|BEQR-152|Service.currentProject及Jdbc末段scoped读；对象更改要求项目匹配，默认不跨项目。|
|BEQR-153|统一设备/总开关拦截器对本Controller适用；本模块不修改层级或关闭留痕。|
|BEQR-154|Service项目状态集合与原文对应，创建拒绝未知值；无新状态。|
|BEQR-155|Service状态集合及terminated立即生效阻断，既有退出缓冲由专门流程；保留M009-018。|
|BEQR-156|本范围不实现合同到期转换；Jdbc审批记录只存关系，不擅自自动终止合同。|
|BEQR-157|本范围未新增员工状态，当前身份校验调用access组件。|
|BEQR-158|本范围未新增设备状态，空间归属资料不替代设备生命周期。|
|BEQR-159|状态请求作用对象为本模块主体/绑定/空间，未添加权限字典状态。|
|BEQR-166|Jdbc:804–1181既有supervisor review读写链保留；合同业务生效由合同模块控制，不把持久层当完整合同实现。|
|BEQR-189|商户rentalRate/propertyFee不是服务点单管理费配置；本范围没有改该业务费率。|
|BSM-001|档案基础定义及守卫不增加收费规则；未改变判据规定的可配置边界。|
|BSM-003|主体/商户/空间/项目基础归属保留；查询新增实际对象与入口留痕已验证。|
|BSM-004|商户与空间类型走存档/申请/审批/软删/恢复；引用存在阻止删；真实最高权限规则由access校验。|
|BSM-005|分步引导属于网页；本模块提供最终创建DTO与字段校验，不把后端创建接口声称为分步弹窗已验收。|
|BSM-006|本模块无二维码生成/刷新/核销实现；主体/商户/空间档案未新增固定凭证码。|
|BSM-007|逐功能权限闸存在；高敏phone授权不能当作永久明文豁免，CX-01。|
|BSM-009|幂等及项目隔离保留；列表对象/筛选留痕已修，第1项原文明文冲突未修。|
|BSM-028|主体多对多、六合作方式、空间项目校验；普通更新不能改结算主体，未来绑定不即刻投影。|
|BSM-029|Project/Space/Merchant保存关联ID供各域引用；不宣称各域所有消费端已逐一运行。|
|BSM-072|Service业态六值；平台scope拒绝商户级平台视图，未以合作方式重定义业态。|
|BSM-074|联系人及合同概要档案提供连接，未将平台列表授权下放给商户或泄露跨项目。|
|BSM-114|后台接口权限与统一终端链核对；冻结考勤用词与现HTML存在已有批准差异，不重开业务HTML。|
|BSM-115|Jdbc实体/商户/空间/project关联及Service写前校验提供基础；跨域履约不在本范围冒报完成。|
|BSM-120|各档案归属及写留痕保留；列表查询对象上下文已补齐，不据此宣称全部模块通过。|
|GEN-001|Record/DTO承载档案与关系；package-info只定包名；范围内无新增抽象。|
|GEN-002|本次定向实跑Service70、Controller14、JDBC24，共108通过。Controller现有明文权限响应测试仍通过，证明保留行为而非HTML一致。|
|GEN-003|两个新增测试先实际失败2/2（缺merchantIds、姓名原文落库），修后通过；旧Controller明文断言仍能复现第1项。|
|GEN-004|Service创建/更新校验业态、合作方式、金额、项目/主体/商户所有权；Request使用约束；数据库参数绑定。|
|GEN-005|Controller逐动作requirePermission；Service.currentProject强制归属；PhoneMask布尔权限不消费单次授权，CX-01。|
|GEN-006|绑定锁商户后重读，软删除保留引用；本次24 JDBC测试实际事务执行通过，不扩大为全库并发已验证。|
|GEN-007|写入口沿现有IdempotencyService；binding锁后重查；现有服务测试通过，不宣称全并发集成已验证。|
|GEN-008|ProjectRequest/ProjectRecord及Jdbc三个资料字段一致；MerchantRecord与参数/mapper对应；不是页面完成声明。|
|GEN-009|非法状态/跨项目/不存在对象明确拒绝；null/空姓名往返实测，未新增静默降级。|
|GEN-010|命名SQL；两姓名写入加密读解密已修，仅新建/保存路径；Controller原文权限分支及单次授权不消费残留M009-058。|
|GEN-011|写ID/reason/decision沿用；merchant.viewed现包含实际返回merchantIds、entry、projectId、筛选、实际分页及count/total，新增测试覆盖空页和默认分页。|
|GEN-012|分页默认/上限及SQL LIMIT/OFFSET、project谓词有边界；引用扫描随库表数，未以极端负载新增要求。|
|GEN-013|Maven离线108定向测试通过，PII脚本18列通过；不宣称全库发布门禁通过。|
|GEN-014|本次V431两列TEXT迁移在complex_test成功；迁移顺序门禁失败来自原V427，before/after同blob且同badset，不扩大本次修复。|
|GEN-015|本范围无UI；Service错误主要中文；英文状态作为内部标识交展示层，未将DTO误作界面。|
|GEN-016|文件行数/SHA现算；旧PASS非当前依据；历史已批准资料后端范围保留。|
|GEN-017|两席B2已定稿058留痕/059姓名细化与工程缺陷；本次B5残留058第1项冲突，按操作员不动，不擅改分类或HTML。|
|GEN-018|本次完整重读20文件3375行与87判据；108测试及SQL/源码为证；旧姓名存量未全部回填、页面范围不冒报。|
|PGM-001|ID关联、金额非负、幂等及中文错误维持；第2项留痕已补齐，第1项原文分支保留。|
|PGM-002|Controller鉴权→Service范围→Repository→审计链已核；Service153–164记录当前页真实对象。|
|PGM-003|本范围后台接口支撑治理模块，不承载入口卡片UI，无UI新增或遗漏断言。|
|PGM-004|currentProject与project associations预留集团归属；当前普通查询仍限定单项目。|
|PGM-005|SpaceRecord创建及space type生命周期；父空间/商户须同项目，保留已修恢复。|
|PGM-006|第一/第二联系人姓名写密文读解密已验证；手机号原文权限分支按用户指令保留。|
|PGM-007|档案经营范围仅供准入引用；普通更新不能重写合同/财税权责。|
|PGM-008|本范围无员工手机注册实现，只调用既有actor上下文；不混为M009缺失。|
|PGM-009|Controller使用单一功能permission；phone布尔检查单次使用缺口CX-01。|
|PGM-010|本模块未定义终端开关，已核共用拦截器，无关闭强制脱敏的新合法需求。|
|PGM-011|Space归属提供设备点位支持，不包含设备领还实现；直接调用无新台账类型。|
|PGM-012|requirePlatformScope拒绝merchantId/外包主体平台全局访问；本范围不提供商户门户。|
|PGM-013|本模块为内部/api/v1/platform，不替代open接口身份核验；无新第三方协议。|
|PGM-014|executeWrite保留业务ID原因；merchant.viewed补实际对象、入口、项目、筛选与实际分页，无姓名手机号入日志。|
|PGM-015|空间维护提供楼宇归属，本范围不执行能耗/消防业务。|
|PGM-016|最高权限仍调用既有access；姓名本批保存加密，手机明文权限分支未改变，不新增破窗机制。|
|PGM-017|项目ID贯穿merchant/spaces及实体关联；currentProject不提供空scope全量。|
|PGM-018|entity_projects关联多对多，无固定父链；城市项目职责不通过普通档案扩大。|
|PGM-019|四主体独立记录，财税变更守卫；不新增股权算法、域间收入或借贷规则。|
|PGM-020|ProjectRequest/Record/Jdbc包含负责人地址经营范围，暂可空是已批准范围；不声称UI/旧项目补录完成。|
|PGM-021|项目隔离SQL/currentProject避免混读；集团视图不作为本轮额外功能开发。|
|PGM-026|四主体唯一槽位和未来绑定守卫保留；两联系人姓名保存加密/读解密，DTO值不变，旧存量未回填。|
|PGM-027|平台列表server-scope拒绝商户；平台不因可查对接信息取得默认原文phone豁免。|
|PGM-031|BusinessEntity不等于经销商门户账号；不在此增加自动采购或经销支付规则。|
|PGM-035|本范围无提成、人员服务商品或营销打卡；不以档案字段实现新收入规则。|
|PGM-040|Controller功能权限与项目scope并行；普通最高职级不等于真实master。|
|PGM-041|currentActorHasPermission不消费singleUse而放原文，CX-01；项目隔离已核。|
|PGM-042|授权按permission不按职级字符串；本模块不增加等级或薪资规则。|
|PGM-043|第2项实际页面入口/当前页对象/筛选留痕已修；其余外包/审计/导出规则按本模块适用边界核查，不新增导出功能。|
|PGM-044|本范围没有桌台/经销交易；绑定操作实时检查权限及范围，不回滚历史事件。|
|PGM-048|requirePlatformScope防商户读取平台档案；营业时间下游service有project范围，其完整业务属M015。|
|PGM-060|本模块无会员新增、定向营销或人脸比对；XM-011仍归M049/M128，不混入M009。|
|PGM-065|基础对象ID与关系供票务/酒吧/财务/物业连接；引用扫描阻止删除被使用商户。|
|PGM-066|状态/时间/归属/审计与中文核对；第2项已修，第1项残留故结论FAIL，不以108绿取代整模块一致性。|

## 全部问题
### CX-B5-01：已有M009-058第1项保留，冲突未解决
分类：冲突；工程缺陷：是；证据类型1（冻结HTML原文）+2（源码）+4（已实跑Controller测试）。
PlatformController.java:281–294，在持pii.contact_phone.view时直接返回merchant原记录，不做PhoneMask；当前文件SHA9e4cef62553160bbd9b2c8bba8779feadec81a4dd26cf3d28664fb160777c3d2，未被本次B3改动。
直接依赖AuthorizationService.java:266–276只执行permissionGrant并返回true，不像43–61逐一markSingleUsePermissionConsumed。若该权限源为singleUse，在此布尔查看路径不被消费。冻结PGM-041原文：“临时授权可设有效期或单次使用；权限的授予、收回、到期、单次消耗、失效全部留痕。” GEN-005/GEN-010同为现有判据；不引用锁外新增要求。
已有listingMerchantsReturnsPlaintextPhoneWithPiiPermission仍为通过状态，证实许可明文行为保持。操作员“1不动”是处置范围裁决，必须尊重；不能把保留等同修复或把HTML判据改成已符合。最小处置：登记残留并停止新增修复；不擅自删除或修改该分支，不改HTML。

第2项留痕与第3项姓名：本次全模块检查未发现修复退化。第2项返回页merchantIds不含其它页对象，entry/projectId/businessFormat/includeArchived/page/pageSize明确，null项目为空串避免Map不收null；第3项统一merchantParameters两姓名encrypt、merchantMapper两姓名decrypt，既有接口返回保持原文语义，V431仅扩TEXT容量，无新CHECK/回填。其余旧24条逐源回看，没有据此新增可定稿finding。

## 验证证据（本次B3/B4实跑，不冒称B5另跑）
- Maven: mvn -o -Dtest=PlatformServiceTests,PlatformControllerTests,PlatformJdbcIntegrationTests test -q；退出0；70+14+24=108，失败0/错误0/跳过0。
- 两新增测试在修改生产代码前实际失败2/2，分别缺merchantIds与姓名明文；修后通过，包含120汉字/短值/null/空值及翻页/空页/默认分页。
- node tools/verify-pii-encryption.mjs：退出0，18列。
- complex_test只读确认V431成功，两列text，测试ID行数0；没有生产数据库操作。
- 完整修复diff为d23497895e200310c4e9692323d5f4e431234981 → 4a8843e8fd431fdd9c9639d2ba96013049986c7a，6文件14hunk，B4双PASS。diff SHA256 6fa14a09544d45a41e6fc14bcc51d47b56f96525295e8869cd427a0bed41a4bb；本次范围diff --check退出0。
- 已有V427迁移顺序门禁失败在before/after一致：该blob均e485a5d29fea55fa714091485aa26facc8c45160，origin/main已达V430；本次新增V431不是该失败来源。不宣称全局发布绿。
- 存量未重新保存的姓名可能仍明文：本次没有回填授权/实现，不能声称旧数据全已加密。

## 技能与范围纪律
本轮已实际读取并按提示词收窄使用using-superpowers、receiving-code-review、systematic-debugging、test-driven-development、verification-before-completion、requesting-code-review（只派Grok）及mattpocock tdd/tests/mocking。先前完整B1技能核查沿用其如实记录，不冒称本次重新调用全部技能；只用确有工具。没有第三席、无commit/fetch/push/merge/worktree、未改冻结范围/判据/HTML。一般规范不覆盖专项V0.17流程。

## 最终独立结论
FAIL。
批准的第2、3项修复检查通过；第1项按操作员明确要求不动，已有冲突仍在，不能写M009 PASS。待Grok完整报告实际交付后才做双席合取及B5后四值校验；本席不预判对方投票。一次追加修复额度已用完，不自行执行第5次B3。

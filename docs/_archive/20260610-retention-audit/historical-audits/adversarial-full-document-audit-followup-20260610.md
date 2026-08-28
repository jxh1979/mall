# 全量文档二次对抗式审计补充报告 V0.2

生成日期：2026-06-10  
审计方式：全库关键词复扫、P0/P1/P2 边界复核、统一口径基准复核、产品流程反推、市场成熟系统对照。  
审计范围：`docs` 下 101 个 Markdown、2 个 HTML、4 个 TXT；本报告生成后文档包增至 108 个文件。

## 1. 本轮结论

本轮审计重点检查了上一轮新增拍板后的同步情况，尤其是 P0 完整应收应付台账、经营对象/运营主体/合同主体/结算主体唯一绑定、B/C/D/E 默认执行、游乐排期、移动管理端范围、企业微信最低管理成本方案和统一口径基准。

> 本段规则已归口至 `docs/scope-wide-coding-rule-canonical-V0.01.md` §A-03，本文不再重复记载。

## 2. 本轮已修复问题

| 编号 | 问题 | 处理 |
|---|---|---|
| F1 | 游乐运营仍把预约、快速通道、闸机/NFC、安全巡检失败、事故工单列为待总控决策 | 改为已确认排期：P0 现场排队/员工辅助；快速通道和预约排队 P2；真实闸机/NFC P1/P2；严重巡检失败自动暂停；安全告知/电子免责/事故工单 P1 |
| F2 | 移动管理端 P0 范围仍在 Web/移动端 PRD 中待确认 | 改为已确认：P0 只处理高风险待办和主管复核，不做全后台 |
| F3 | Android 终端文档中企业微信实施责任仍写“待实施时明确” | 改为管理员一次性授权和密钥配置，系统自动账号映射、同步、失败告警、基础规则质检和留存 |
| F4 | 统一口径基准仍有开放平台、状态枚举、退款等“建议/待确认”早期口径 | 改为已确认：开放平台规范、Webhook+轮询、退款口径、状态集合、游乐 maintenance 归并口径 |
| F5 | 市场基准报告仍把应收账龄整体放 P1 | 改为 P0 完整应收应付台账承载基础账务；P1 做商户账单门户、催缴、费用异议和复杂费用规则 |
| F6 | P0 任务清单、产品索引、路线图仍存在“是否进入 P1/P2”的旧问题标题 | 改为执行型分层说明 |

## 3. 市场基准对照

| 市场能力 | 资料依据 | 对本项目建议 |
|---|---|---|
| Android 专用设备/Kiosk/策略管控 | Android Management API dedicated devices、Android Enterprise dedicated devices 文档显示专用设备和 Kiosk 可由策略控制 | 工作手机、平板、一体机正式发放必须走 Android Enterprise / Android Management API + 商业 MDM；普通 App 自管不能作为强管控验收依据 |
| 应收应付、发票、到期提醒 | Square 对 Accounts Receivable 的定义和 Invoice Payment Reminder 对象说明了“应收款、账单、到期提醒”是成熟收款闭环的基础 | P0 做完整应收应付台账是合理增强；自动催缴、商户门户和复杂费用异议放 P1 |
| 商业地产商户门户/维护请求/租户支付 | Yardi Breeze/CommercialCafe 公开资料展示商业物业系统通常包含租约、租户账单、付款、维护请求、租户门户 | 本项目 P0 做财务台账和主体绑定，P1 做商户服务工单、商户账单门户、催缴和费用异议 |
| 酒吧/餐饮 POS 高级账单 | Toast 支持拆单、按座位拆分、转台、服务费等成熟 POS 能力 | P0 点酒/支付/库存/日结闭环已足够；拆单、合单、转台、挂账、服务费、低消差额补齐、员工提成进入 P1 酒吧 SaaS 增强 |

参考链接：

- [Android Management API dedicated devices](https://developers.google.com/android/management/policies/dedicated-devices)
- [Android Enterprise dedicated devices overview](https://developer.android.com/work/dpc/dedicated-devices)
- [Square Accounts Receivable glossary](https://squareup.com/gb/en/glossary/accounts-receivable)
- [Square InvoicePaymentReminder API object](https://developer.squareup.com/reference/square/objects/InvoicePaymentReminder)
- [Yardi Breeze commercial property management features](https://www.yardibreeze.com/commercial-features/)
- [CommercialCafe Tenant Portal](https://www.yardi.com/product/commercialcafe-tenant-portal/)
- [Toast Split Checks on the POS](https://support.toasttab.com/en/article/Splitting-Checks-by-Item-1492811097734)
- [Toast Transfer Items, Checks, and Payments](https://support.toasttab.com/en/article/Transferring-Items-Checks-and-Payments)
- [Toast service charges](https://support.toasttab.com/en/article/How-to-Create-a-Service-Charge)

## 4. 仍建议后续完善

以下不再是“方向待定”，而是执行细化事项：

1. **P0 应收应付台账建模**：建议补一份开发设计或在财务 PRD 中追加字段表，至少包括 `ar_ap_item_no`、应收/应付类型、来源对象、责任主体、经营对象、结算主体、金额、到期日、账龄、收付款状态、差异状态、附件、审批/调整留痕。
2. **主体绑定关系落库**：建议后续在平台治理数据库设计中明确 `business_entity_bindings` 或等价表，承载经营对象、运营主体、合同主体、结算主体、版本、生效/失效时间和变更原因。
3. **商户账单 P1 细化**：P1 需要把 P0 台账转成商户可读账单、催缴、异议、费用规则和商户确认流程。
4. **游乐安全 P1 细化**：安全告知、电子免责、事故工单需要法务文本、留存周期、事故分级、保险/客服/法务接口边界。
5. **第三方接入资料清单**：拿到供应商 SDK/API 后，应逐项确认字段映射、签名、防重放、Webhook 事件、轮询兜底、对账口径、失败补偿和人工导入模板。
6. **市场增强项路线图**：酒吧高级 POS、商户服务工单、商户账单门户、游乐预约排队应进入 P1/P2 路线图看板，不再散落在历史审计报告里。

## 5. 当前不建议扩大 P0 的内容

- 商户账单门户、催缴自动化、复杂费用异议。
- 酒吧拆单、合单、转台、挂账、服务费、员工提成和复杂配方成本。
- 游客自助预约排队、团体/包场、快速通道和集团视图。
- 真实闸机/NFC 厂商联动。
- 独立 Python AI 服务和深度语义质检。
- 视频平台内播放和复杂证据剪辑。

## 6. 本轮验收建议

本轮修改后，应继续以 `business-decision-log-v0.1.md`、`unified-standards-baseline-v0.1.md`、`p0-implementation-task-list.md`、`adversarial-full-document-audit-and-decision-list-20260610.md` 和本文作为当前权威入口。历史审计报告保留问题来源，但不再作为当前口径判断依据。

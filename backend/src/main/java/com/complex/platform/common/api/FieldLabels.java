package com.complex.platform.common.api;

// 文件版本：V0.21

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 接口报错「出口翻译层」：把校验类报错里的英文字段键换成业务人员看得懂的中文名。
 *
 * <p>为什么放在 GlobalExceptionHandler 而不是各个抛点：全仓 975 处 {@code Strings.requireText}
 * 调用 + 95 处手写 {@code order_id is required} 字面量 + 6 份本地复制的必填校验助手，唯一必经之路
 * 就是这里（异常转 HTTP 响应体的那一步）。在此收口＝一处改动、全部抛点同时受益，且后台 Web、
 * 客人端、安卓终端三端零改动就都拿到中文（安卓端没有任何翻译层，此前英文原样上屏到运营现场）。
 *
 * <p>抛点保留英文键（{@code order_id is required}）不动：那串是给日志/排障看的机读信息，
 * 只在写响应体的一瞬间换成中文，两边都不牺牲。
 */
public final class FieldLabels {

    private FieldLabels() {
    }

    /** 认不出字段键时的兜底：绝不回退成英文键名（英文键名本身就是展示文字违规）。 */
    public static final String FALLBACK = "提交内容有误，请检查后重新提交";

    /** 机读键：不套「XX 未填写」模板，直接给整句用户话术（与 apps/web/src/shared/apiError.ts 保持一致）。 */
    private static final Map<String, String> WHOLE_MESSAGES = Map.of(
            "idempotency_key", "请勿重复提交，请刷新后再试");

    /** 形态一：{@code field is required} / {@code field is invalid}。 */
    private static final Pattern REQUIRED_OR_INVALID =
            Pattern.compile("^([A-Za-z][A-Za-z0-9_./]*) is (required|invalid)$");

    /** 形态二：英文字段键打头、后面已是中文正文（如 {@code quantity 必须大于 0}、{@code merchant_id：M001}）。 */
    private static final Pattern KEY_PREFIXED =
            Pattern.compile("^([A-Za-z][A-Za-z0-9_.]*)([\\s:：].*)$", Pattern.DOTALL);

    private static final Pattern CJK = Pattern.compile("[\\u4e00-\\u9fff]");

    private static final Pattern CAMEL_BOUNDARY = Pattern.compile("([a-z0-9])([A-Z])");

    /** Spring Bean 校验默认英文文案 → 中文原因（有序，先匹配先用）。 */
    private static final Map<Pattern, String> BEAN_REASONS = new LinkedHashMap<>(Map.of());

    static {
        BEAN_REASONS.put(Pattern.compile("must not be (null|empty|blank)", Pattern.CASE_INSENSITIVE), "未填写");
        BEAN_REASONS.put(Pattern.compile("must be greater than 0|must be positive", Pattern.CASE_INSENSITIVE), "必须大于 0");
        BEAN_REASONS.put(Pattern.compile("size must be between", Pattern.CASE_INSENSITIVE), "长度不符合要求");
    }

    private static final Map<String, String> LABELS = new HashMap<>(512);

    private static void p(String key, String label) {
        LABELS.put(key, label);
    }

    static {
        p("applicability_basis", "税目适用依据");
        p("calculation_basis", "计税依据");
        p("failure_reason", "失败原因");
        p("new_account_ref", "新收款账户");   // FS-021-R5：收款方财务负责人审批更换收款账户
        p("identity_basis", "身份认定依据");
        p("manual_reason", "人工抄表原因");
        p("new_meter_name", "新表名称");
        p("professional_basis", "专业认定依据");
        p("reference_no", "结果凭证编号");
        p("supplier_bill_reference", "供能账单编号");
        p("transaction_basis", "交易认定依据");
        p("verifier_id", "核实人员");
        // ── 第十一批 TK-05 通票安全例外 ──────────────────
        p("responsible_entity_id", "责任主体");
        p("safety_event_id", "安全事件");
        p("situation", "当时情形");
        p("ticket_instance_id", "票");
        // ── 第十一批 TK-04 商户消费奖励 ──────────────────
        p("business_format", "商户业态");
        // ── 第十批（储值 / 酒吧台位 / 传媒演出）复核 html B2a 补齐 ──────────────────
        p("adjust_reason", "调整理由");
        p("adjusting_settlement_id", "调整结算行");
        p("bar_merchant_id", "演出酒吧");
        p("basis", "依据");
        p("completed_content", "已完成内容");
        p("consume_txn_id", "原储值消费流水");
        p("dispute_reason", "争议理由");
        p("media_merchant_id", "传媒公司");
        p("origin_item_id", "原应收应付款项");
        p("payee_entity_id", "收款主体");
        p("reversal_txn_id", "回冲流水");
        p("show_id", "演出场次");
        p("show_kind", "场次类型");
        p("split_session_id", "拆出的原台会话");
        // ── 组织 / 主体 ──────────────────────────────────────────────
        p("project_id", "项目");
        p("project_name", "项目名称");
        p("merchant_id", "商户");
        p("merchant_ids", "商户");
        p("merchant_name", "商户名称");
        p("merchant_type", "商户类型");
        p("management_entity_id", "管理公司");
        p("operation_entity_id", "运营主体");
        p("organization_id", "组织");
        p("org_name", "组织名称");
        p("org_category", "组织类别");
        p("entity_name", "主体名称");
        p("entity_role", "主体角色");
        p("business_entity_id", "经营主体");
        p("supervising_company_id", "主管公司");
        p("supervising_company_reason", "主管公司指定理由");
        p("subject_id", "主体");
        p("subject_type", "主体类型");
        // 第十八批第 24 条 高价值判定阈值上下限、物料手工标记高价值、后台高价值复核页复核对象摘要。
        p("high_value_threshold_min_cent", "高价值阈值下限");
        p("high_value_threshold_max_cent", "高价值阈值上限");
        p("manual_high_value", "人工认定高价值");
        p("subject_summary", "复核对象摘要");
        p("partner_id", "合作方");
        p("partner_kind", "合作方类别");
        p("counterparty_id", "往来单位");
        p("supplier_id", "供应商");
        p("supplier_name", "供应商名称");
        p("provider", "供应商");
        p("provider_code", "供应商编码");
        p("brand_id", "品牌");
        p("brand_name", "品牌名称");
        p("brand_type", "品牌类型");
        // 第十八批第 30 条 招商黑名单例外放行与意向关键阶段主管确认。
        p("blacklist_exception_id", "黑名单例外放行单");
        p("pending_stage", "待确认阶段");
        p("pending_review_id", "待确认复核单");
        p("used_lead_id", "放行关联线索");
        p("used_intent_id", "放行所建意向");
        p("bar_id", "酒吧");
        p("venue_id", "场地");
        p("venue_type", "场地类型");
        p("maintenance_vendor_id", "维保供应商");

        // ── 账号 / 权限 / 认证 ───────────────────────────────────────
        p("user_id", "用户");
        p("username", "用户名");
        p("login_name", "登录名");
        p("password", "密码");
        p("authentication", "登录凭证");
        p("access_token", "登录凭证");
        p("refresh_token", "登录凭证");
        p("request", "请求");
        p("invitation_token", "邀请码");
        p("sms_code", "短信验证码");
        p("verification_code", "验证码");
        p("role_id", "角色");
        p("capability_code", "能力编码");
        p("capabilities", "能力项");
        p("approver_permission_code", "审批权限");
        p("principal_user_id", "负责人");
        p("owner_user_id", "负责人");
        p("owner_person", "责任人");
        p("owner_department", "责任部门");
        p("operator_user_id", "操作人");
        p("actor_user_id", "操作人");
        p("operator_note", "操作说明");
        p("assignee_id", "处理人");
        p("confirmer_id", "提单确认人");
        p("target_handler_id", "目标处理人");
        p("target_user_id", "目标用户");
        p("viewer_user_id", "查看人");
        p("closed_by_user_id", "关闭人");
        p("reported_by_user_id", "上报人");
        p("cashier_user_id", "收银员");
        p("reviewer_id", "复核人");
        p("reviewed_by", "复核人");
        p("supervisor_confirmed_by", "主管确认人");
        p("acceptance_by", "验收人");
        p("inspector_id", "巡检人");
        p("signer_name", "签署人");
        p("public_key", "公钥");
        p("app_key", "应用标识");
        p("nonce", "随机串");
        p("signature", "签名");
        p("checksum", "校验值");
        p("seed", "随机种子");

        // ── 客人 / 会员 ──────────────────────────────────────────────
        p("customer_id", "客户");
        p("customer_name", "客户姓名");
        p("customer_name_or_nickname", "客户姓名或昵称");
        p("to_name_or_nickname", "姓名或昵称");
        p("visitor_name", "访客姓名");
        p("contact_name", "联系人");
        p("contact_phone", "联系电话");
        // 第十八批第 31 条 经销合同指定经办人与停用后对方确认留证。
        p("counterparty_confirmation_id", "对方确认留证记录");
        p("designated_handler_at", "经办人指定时间");
        p("designated_handler_by", "经办人指定人");
        p("designated_handler_user_id", "指定经办人");
        p("handler_user_id", "经办人");
        p("matter_type", "确认事项");
        p("related_object_id", "关联单号");
        p("used_by_id", "使用人");
        p("phone", "手机号");
        p("receiver_phone", "接收人手机号");
        p("recipient", "接收人");
        p("recipient_name", "接收人姓名");
        p("recipient_phone", "接收人手机号");
        p("guest_count", "客人人数");
        p("crew_count", "人员数量");
        p("level_code", "等级编码");
        p("level_name", "等级名称");
        p("level_rank", "层级");
        p("upgrade_threshold", "升级门槛");
        p("point_multiplier_basis_points", "积分倍率（万分比）");
        p("streak_days", "连续天数");
        p("award_points", "奖励积分");
        p("multiplier_basis_points", "倍率（万分比）");
        p("staff_level_id", "员工职级");
        p("sales_position", "销售岗位");
        p("position_code", "岗位编码");
        p("work_scope", "工作范围");
        p("shift_id", "班次");
        p("fault_id", "全园故障记录");
        p("dispatch_id", "派驻记录");
        p("station_code", "工位编码");
        p("registration_photo_refs", "登记照片");
        p("consent_type", "授权类型");

        // ── 订单 / 账单 / 支付 ───────────────────────────────────────
        p("order_id", "订单");
        p("order_item_id", "订单明细");
        p("bill_id", "账单号");
        p("merged_bill_id", "合并后账单");
        p("business_id", "业务单号");
        p("business_ref", "业务关联单");
        p("business_order_id", "业务单号");
        p("business_order_type", "业务单类型");
        p("business_type", "业务类型");
        p("business_date", "业务日期");
        p("payment_id", "支付单号");
        p("payment_intent_id", "支付意向单");
        p("payment_method", "支付方式");
        p("sales_channel", "销售渠道");
        p("fee_layer", "费用层");
        p("route_fee_cent", "通路费");
        p("participant_count", "第二段参与家数");
        p("rank_no", "名次");
        p("period_value", "通告期");
        p("period_unit", "通告期单位");
        p("refund_id", "退款单号");
        p("refund_domain", "退款业务域");
        p("refund_amount_cent", "退款金额");
        p("void_id", "作废单号");
        p("session_id", "场次");
        p("merged_session_id", "合并后场次");
        p("table_id", "桌台");
        p("table_name", "桌台名称");
        p("table_type", "桌台类型");
        p("target_table_id", "目标桌台");
        p("reservation_id", "预订");
        p("reservation_time", "预订时间");
        p("group_booking_id", "团体预订");
        p("queue_entry_id", "排队号");
        p("saved_bottle_id", "存酒记录");
        p("renewal_id", "转存记录");
        p("manual_pickup_id", "人工例外取酒申请");
        p("remedy_id", "补存/赔付记录");
        p("remedy_type", "补存/赔付方式");
        p("bearer_type", "承担方类型");
        p("bearer_id", "承担方");
        p("original_merchant_id", "原门店商户");
        p("pickup_location", "领取地点");
        p("received_quantity", "实收数量");
        p("condition", "接收状态");
        p("staff_user_id", "责任员工");
        p("role", "审批角色");
        p("scene", "验证码用途");
        p("to_phone", "受让人手机号");
        p("customer_phone", "客户手机号");
        p("bottle_label_value", "存酒标签");
        p("combo_name", "套餐名称");
        p("package_name", "套餐名称");
        p("catalog_item_id", "商品目录项");
        p("product_id", "商品");
        p("product_name", "商品名称");
        p("item_id", "明细项");
        p("item_name", "明细名称");
        p("items", "明细项");
        p("line", "明细行");
        p("line.amount_cent", "明细行金额");
        p("delivered_items", "交付明细");
        p("quantity", "数量");
        // 第十八批第 33 条：物料安全库存（采购建议量算式用）。
        p("min_quantity", "安全库存");
        p("unit", "单位");
        p("variant_type", "人群/渠道");
        p("tip_amount_cent", "小费金额");
        p("minimum_spend_cent", "最低消费");
        p("additional_min_consumption_cent", "追加最低消费");
        p("additional_deposit_amount_cent", "追加押金");
        p("additional_service_fee_cent", "追加服务费");
        p("opening_cash_cent", "备用金");
        p("declared_cash_cent", "申报现金金额");
        p("cash_diff_threshold_cent", "现金差额阈值");
        p("daily_close_id", "日结单号");
        p("handover_id", "交接单号");

        // ── 票务 / 权益 / 券 ────────────────────────────────────────
        p("ticket_id", "票券");
        p("ticket_name", "票种名称");
        p("ticket_type_id", "票种");
        p("ticket_category", "票务类别");
        p("entitlement_id", "权益");
        p("entitlement_code", "权益编码");
        p("entitlement_type", "权益类型");
        p("entitlement_ref", "权益关联单");
        p("entitlement_template_id", "权益模板");
        p("allowed_free_entry_count", "免费入场次数");
        p("voucher_instance_id", "券");
        p("voucher_kind", "券类别");
        p("claim_code", "领取码");
        p("redeem_method", "兑换方式");
        p("redemption_id", "兑换单号");
        p("redemption_voucher_no", "兑换券号");
        p("verification_id", "核销记录");
        p("entry_id", "入场记录");
        p("qr_content", "二维码内容");
        p("credential_type", "凭据类型");
        p("credential_payload", "凭据内容");

        // ── 财务 / 结算 / 合同 ──────────────────────────────────────
        p("amount_cent", "金额");
        p("actual_amount_cent", "实际金额");
        p("total_amount_cent", "合计金额");
        p("split_amount_cent", "拆分金额");
        p("settlement_amount_cent", "结算金额");
        p("payout_cent", "结算金额");
        p("price_cent", "价格");
        p("price_mode", "计价方式");
        p("base_calendar_price_mode", "基础日历定价方式");
        p("dynamic_pricing_rule_id", "动态定价规则");
        p("adjustment_id", "调整单号");
        p("adjustment_type", "调整类型");
        p("adjustment_amount_cent", "调整金额");
        p("adjustment_rate", "调整比例");
        p("commission_mode", "分成方式");
        p("commission_ids", "提成记录编号");
        p("settlement_id", "结算单号");
        p("settlement_mode", "结算方式");
        p("settlement_entity_id", "结算主体");
        p("settlement_subject_id", "结算主体");
        p("statement_id", "对账单号");
        // 第 75 条：短期经营激励申请与单次有效期上限（须同时配置上下限）。
        p("special_approval_term_value", "短期经营激励单次有效期上限");
        p("special_approval_term_unit", "短期经营激励有效期单位");
        p("special_approval_term_min_value", "有效期上限参数的下限");
        p("special_approval_term_max_value", "有效期上限参数的上限");
        p("incentive_applications", "短期经营激励申请");
        p("term_value", "激励期限");
        p("term_unit", "激励期限单位");
        p("max_excess_cent", "最大超出额");
        p("trial_result", "逐票种试算结果");
        p("decision_note", "审批意见");
        p("ar_ap_item_id", "应收应付明细");
        p("expense_type", "费用类型");
        p("fee_event", "费用事件");
        p("period", "期间");
        p("period_label", "期间名称");
        p("period_type", "期间类型");
        p("contract_id", "合同");
        // CG-11a 经销
        p("dealer_id", "经销商");
        p("dealer_contract_id", "经销合同");
        p("contract_no", "合同编号");
        p("sales_entity_id", "销售主体");
        p("central_warehouse_id", "中心仓");
        p("catalog_id", "价格目录");
        p("inquiry_id", "询价单");
        p("overpayment_id", "超收记录");
        p("target_order_id", "抵扣订单");
        p("return_id", "退货单");
        p("legal_representative", "法定代表人");
        p("invoice_id", "发票");
        p("invoice_no", "发票号");
        p("reason_type", "退货原因");
        p("result", "结果");
        p("return_freight_bearer", "退回运费承担方");
        // method 键招商跟进（电话/拜访/微信）、经销收款、考勤打卡共用，取中性名；取值由结果区按键取值表各自翻译。
        p("method", "方式");
        p("usage", "用途");
        p("parent_contract_id", "主合同");
        p("contract_entity_id", "合同主体");
        p("contract_subject_id", "合同签约主体");
        p("supply_contract_id", "供货合同");
        p("contract_basis", "合同及双方确认依据");
        p("source_payment_id", "原实际付款");
        p("is_current", "当前有效账款");
        p("purchase_id", "采购申请");
        p("source_reference", "售出依据单号");
        p("responsibility_terms", "费用与责任约定");
        p("amusement_project_id", "游乐项目");
        p("complex_id", "所属综合体");
        p("zone_id", "所在地时区");
        p("close_hour", "闭店时点");
        p("min_close_hour", "最早闭店时点");
        p("max_close_hour", "最晚闭店时点");
        p("renewed_until", "续约至");
        p("valid_until", "有效期至");
        p("expires_at", "到期时间");
        p("expiry_action", "到期处理方式");
        p("lead_id", "线索");
        p("intent_id", "意向单号");
        p("candidate_ids", "招商对象");
        p("operating_area_sqm", "经营面积");
        p("total_difference_cent", "差额合计");
        p("difference_id", "差异单号");
        p("difference_type", "差异类型");
        p("difference_count", "差异数量");
        p("variance_id", "差异单号");

        // ── 库存 / 调拨 / 盘点 ──────────────────────────────────────
        p("inventory_item_id", "库存商品");
        p("inventory_variance_id", "库存差异单");
        p("stocktake_id", "盘点单号");
        p("stock_location_id", "库位");
        p("location_id", "库位");
        p("from_location_id", "调出库位");
        p("to_location_id", "调入库位");
        p("warehouse_id", "仓库");
        p("transfer_id", "调拨单号");
        p("transfer_type", "调拨类型");
        p("movement_type", "出入库类型");
        p("from_owner_id", "原归属方");
        p("from_owner_type", "原归属方类型");
        p("to_owner_id", "新归属方");
        p("to_owner_type", "新归属方类型");
        p("batch_id", "批次号");
        p("batch_no", "批次号");
        p("trace_code", "溯源码");
        p("trace_code_id", "溯源码");
        p("expected_count", "应有数量");
        p("actual_count", "实际数量");
        p("source_count", "来源数量");
        p("pending_count", "待处理数量");
        p("capacity", "容量");

        // ── 空间 / 楼宇 / 设备 / 巡检 ───────────────────────────────
        p("space_id", "空间");
        p("space_ids", "空间");
        p("space_name", "空间名称");
        p("space_type", "空间类型");
        p("floor_no", "楼层");
        p("floor_plan_id", "平面图");
        p("point_id", "点位");
        p("endpoint_key", "点位标识");
        p("checked_endpoints", "巡检点位");
        p("check_scope", "巡检范围");
        p("inspection_id", "巡检单号");
        p("equipment_name", "设备名称");
        p("equipment_type", "设备类型");
        p("disposal_method", "处置方式");
        p("scrap_id", "报废单");
        p("residual_counterparty_id", "残值回收方");
        p("alarm_type", "告警类型");
        p("priority", "优先级");
        p("duty_role", "接警值守角色");
        p("fault_scenario", "故障场景");
        p("handling_requirement", "处置要求");
        p("judgement", "处置判定");
        // HP-P01 停车场
        p("plate_no", "车牌");
        p("temp_code", "临时码");
        p("vehicle_kind", "车辆类型");
        p("parking_session_id", "停车记录");
        p("session_ref", "停车系统记录号");
        p("payment_ref", "停车系统缴费单号");
        p("entered_at", "入场时间");
        p("exited_at", "出场时间");
        p("paid_at", "缴费时间");
        p("reported_at", "回传时间");
        p("total_spaces", "车位总数");
        p("occupied_total", "在场车辆数");
        p("occupied_monthly", "在场月租车辆数");
        p("occupied_staff", "在场员工车辆数");
        p("fee_cent", "停车费");
        p("paid_cent", "已缴金额");
        p("release_reason", "放行原因");
        p("waive_type", "免费或减免");
        p("reduction_cent", "减免金额");
        p("provider_entity_id", "提供主体");
        p("meter_id", "表计");
        p("parent_meter_id", "上级表计");
        p("meter_type", "表计类型");
        p("reading_id", "抄表记录");
        p("read_mode", "抄表方式");
        p("energy_fee_basis", "能耗计费口径");
        p("gather_object", "采集对象");
        p("gps_lat", "纬度");
        p("gps_lng", "经度");
        p("device_id", "设备");
        p("device_code", "设备编码");
        p("terminal_type", "终端类型");
        p("kiosk_mode", "自助机模式");
        p("fallback_mode", "降级模式");
        p("max_concurrent", "最大并发");
        p("adapter_type", "适配类型");
        p("environment", "环境");
        p("rollout_id", "灰度批次");

        // ── 工单 / 风控 / 合规 ──────────────────────────────────────
        p("parent_ticket_id", "父工单");
        p("incident_id", "事件单号");
        p("exception_id", "异常单号");
        p("abnormal_note", "异常说明");
        p("alert_id", "告警编号");
        p("alert_type", "告警类型");
        p("hit_id", "命中记录");
        p("violation_id", "违规记录");
        p("violation_type", "违规类型");
        p("risk_level", "风险等级");
        p("policy_status", "策略状态");
        p("blocking_scope", "阻断范围");
        p("disposition", "处置结论");
        p("decision", "处理决定");
        p("resolution", "处理结论");
        p("resolution_conclusion", "处理结论");
        p("resolution_note", "处理说明");
        p("review_id", "复核单号");
        p("forwarded_to", "转交对象");
        p("final_approver_id", "最终审批人");
        p("line_code", "审批线");
        p("line_name", "审批线名称");
        p("required_line_codes", "必需审批线");
        p("parallel_lines.line_code", "并行审批线");
        p("min_start_rank", "最低起点层级");
        p("entry_user_id", "起点人");
        p("superior_id", "直接上级");
        p("review_result", "复核结果");
        p("request_type", "申请类型");
        p("approval_note", "审批意见");
        p("archive_reason", "归档原因");
        p("close_reason", "关闭原因");
        p("handle_minutes", "处理时长");
        p("response_minutes", "响应时长");
        p("remind_minutes", "最晚到店前提醒分钟数");
        p("min_minutes", "分钟数下限");
        p("max_minutes", "分钟数上限");
        p("pour_device_equipped", "是否配有出酒量采集设备");
        p("suspension_id", "代订权限停用记录");
        p("penalty_points", "扣分");
        p("min_penalty_points", "单次扣分下限");
        p("max_penalty_points", "单次扣分上限");
        p("reported_channel", "上报渠道");
        p("robot_task_ref", "机器人任务号");
        p("scenario", "场景");
        p("stage_name", "阶段名称");
        p("target_stage", "目标阶段");

        // ── 对接 / 第三方 / 任务 ────────────────────────────────────
        p("channel", "渠道");
        p("external_business_id", "外部单号");
        p("external_store_id", "外部门店号");
        p("external_product_id", "外部商品号");
        p("external_conversation_id", "外部会话号");
        p("conversation_id", "会话编号");
        p("import_task_id", "导入任务");
        p("client_task_id", "客户端任务号");
        p("task_id", "任务");
        p("tasks", "任务");
        p("task_type", "任务类型");
        p("sync_type", "同步类型");
        p("source", "来源");
        p("source_name", "来源名称");
        p("source_type", "来源类型");
        p("source_domain", "来源业务域");
        p("source_payload", "来源报文");
        p("binding_scope", "绑定范围");
        p("window_key", "窗口标识");
        p("window_type", "窗口类型");
        p("message_id", "消息");
        p("preview_id", "清理预览");
        p("message_template", "短信模板");
        p("notice_id", "公告");
        p("template_id", "模板");
        p("template_code", "模板编码");
        p("template_name", "模板名称");
        p("definition_id", "定义编号");
        p("plan_id", "方案");
        p("plan_name", "方案名称");
        p("rule_id", "规则");
        p("rule_name", "规则名称");
        p("rule_type", "规则类型");
        p("rule_value", "规则取值");
        p("config_id", "配置编号");
        p("config_type", "配置类型");
        p("change_id", "变更单号");
        p("change_type", "变更类型");
        p("trigger_type", "触发方式");
        p("steps", "步骤");
        p("reserve_release_delay_periods", "保留释放延迟期数");

        p("agreement_basis", "双方确认依据");
        p("boot_id", "手机本次启动标识");
        p("buyer_contact", "买方联系人");
        p("context_id", "手机领用记录");
        p("proposed_context_id", "待复核的手机领用记录");
        p("contract_number", "合同编号");
        p("delivery_batch_id", "交付批次");
        p("disposition_type", "处置方式");
        p("entity_id", "主体");
        p("event_id", "事件编号");
        p("handover_evidence", "交接依据");
        p("lot_id", "库存批次");
        p("old_device_code", "原工作手机编码");
        p("physical_route", "实物处理方式");
        p("replacement_device_id", "更换后的工作手机");
        p("resolution_type", "差异处理方式");
        p("responsibility_party", "责任承担方");
        p("source_note", "来源说明");
        p("tag_ref", "标签引用");

        // ── 通用 ────────────────────────────────────────────────────
        p("id", "标识");
        p("code", "编码");
        p("name", "名称");
        p("title", "标题");
        p("label", "标签");
        p("kind", "类别");
        p("mode", "模式");
        p("status", "状态");
        p("target_status", "目标状态");
        p("active", "启用状态");
        p("enabled", "启用开关");
        p("accepted", "确认结果");
        p("action", "操作");
        p("action_type", "操作类型");
        p("action_result", "操作结果");
        p("event", "事件");
        p("events", "事件");
        p("event_type", "事件类型");
        p("domain", "业务域");
        p("segment", "分段");
        p("scope_id", "范围标识");
        p("policy_key", "业务参数键");
        p("scope_type", "范围类型");
        p("category", "分类");
        p("category_name", "分类名称");
        p("type_code", "类型编码");
        p("type_name", "类型名称");
        p("target_id", "目标对象");
        p("target_type", "目标类型");
        p("target_date", "目标日期");
        p("object_id", "对象标识");
        p("object_type", "对象类型");
        p("ref_id", "关联单据");
        p("ref_type", "关联单据类型");
        p("request_id", "请求标识");
        p("reason", "原因");
        p("reason_code", "原因编码");
        p("note", "备注");
        p("comment", "备注");
        p("description", "说明");
        p("summary", "摘要");
        p("content", "内容");
        p("content_text", "内容正文");
        p("content_type", "内容类型");
        p("content_version", "内容版本");
        p("custom_deduction.name", "自定义扣减项名称");
        p("expected_entry.amount_cent", "预期分录金额");
        p("expected_entry.external_business_id", "预期分录外部单号");
        p("attachment_id", "附件");
        p("attachment_ids", "附件");
        p("file_name", "文件名");
        p("image_url", "图片地址");
        p("image_width", "图片宽度");
        p("image_height", "图片高度");
        p("color", "颜色");
        p("url", "链接地址");
        p("direction", "方向");
        p("fixed", "固定值");
        p("version_no", "版本号");
        p("now", "当前时间");
        p("timestamp", "时间戳");
        p("occurred_at", "发生时间");
        p("holiday", "节假日");
        p("holiday_name", "节假日名称");
        p("acknowledgement_method", "确认方式");
        p("error_code", "错误码");
        p("error_message", "错误信息");
        p("account_id", "账户编号");
        // ── 第六批：工资完整模型 / 酒吧服务收入 / 个税 ───────────────────
        p("month", "月份");
        p("scheme_version_id", "薪资方案版本");
        p("calendar_version_id", "工资日历版本");
        p("facts_id", "计薪依据");
        p("calculation_id", "工资计算单");
        p("tax_assessment_id", "个税核定");
        p("origin_event_id", "原始事件");
        p("component_code", "薪资项目编码");
        p("payment_basis", "发放依据");
        p("source_basis", "来源依据");
        p("evidence_basis", "凭证依据");
        p("approval_basis", "审批依据");
        p("a_condition", "A档条件");
        p("deduction_rule_review_id", "扣分规则审批单");
        p("professional_calculation_reference", "专业计算参考");
        p("owner_company_id", "经营主体");
        p("payer_company_id", "发薪主体");
        p("platform_company_id", "平台公司");
        p("next_user_id", "接手人员");
        p("target_service_staff_id", "目标服务人员");
        // ── 第八批：内部要货差异处理 / 分红依法可分配金额 ───────────────
        p("disposition_id", "差异处理登记");
        p("responsible_party", "责任方");
        p("legal_distributable_cent", "依法可分配金额");
        p("tax_accounting_ref", "财税核算依据");
        p("resolution_ref", "分配决议");
        // ── 第十三批 B 段 ROOT-03-R3：会员注册 ───────────────
        p("birth_date", "生日");
        p("member_terms_version", "会员条款版本");
        p("privacy_terms_version", "隐私条款版本");
        // 第十三批 B 段 ROOT-03 订台与扫码点单（R19、R6、R7）。
        p("latest_arrival_at", "最晚到店时间");
        p("member_id", "被邀请人");
        p("scan_order_id", "扫码点单");
        // 第十三批 B 段 ROOT-03 账户注销、学生认证、投诉与票（R21、R28、clients-R8、R26、R29）。
        p("abandon_saved_bottles", "放弃存酒");
        p("saved_bottles", "名下存酒");
        p("chsi_verification_code", "学信网验证码");
        p("consent_id", "证件采集同意");
        p("certification_id", "学生认证申请");
        p("work_order_id", "投诉单");
        p("score", "满意度");
        p("customer_feedback", "客人反馈");
        p("buyer_phone", "客人手机号");
    }

    /** 字段键（snake 或 camel）→ 中文业务名；认不出返回 null。 */
    public static String label(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String normalized = CAMEL_BOUNDARY.matcher(key).replaceAll("$1_$2").toLowerCase(Locale.ROOT);
        String label = LABELS.get(normalized);
        // 明细项校验会把同义键拼成 `product_id/productId` 传进来，取第一个别名查即可。
        if (label == null && normalized.indexOf('/') > 0) {
            label = LABELS.get(normalized.substring(0, normalized.indexOf('/')));
        }
        return label;
    }

    /** 单条报错消息 → 中文；已是中文或无法识别的照旧返回，纯英文识别不出的走中文兜底。 */
    public static String translate(String message) {
        if (message == null || message.isBlank()) {
            return message;
        }
        Matcher required = REQUIRED_OR_INVALID.matcher(message);
        if (required.matches()) {
            String whole = WHOLE_MESSAGES.get(required.group(1));
            if (whole != null) {
                return whole;
            }
            String label = label(required.group(1));
            return label == null
                    ? FALLBACK
                    : label + ("required".equals(required.group(2)) ? "未填写" : "格式不正确");
        }
        Matcher prefixed = KEY_PREFIXED.matcher(message);
        if (prefixed.matches()) {
            String label = label(prefixed.group(1));
            if (label != null) {
                String rest = prefixed.group(2);
                // 「quantity 必须大于 0」→「数量必须大于 0」：中文正文前的分隔空格去掉，标点原样保留。
                return label + (rest.startsWith(" ") ? rest.substring(1) : rest);
            }
        }
        // 已含中文 → 是业务话术，原样透传；纯英文 → 无法安全翻译，走中文兜底（绝不外泄英文）。
        return CJK.matcher(message).find() ? message : FALLBACK;
    }

    /** Bean 校验的字段错误（字段名 + 英文默认文案）→ 中文。 */
    public static String beanFieldError(String field, String defaultMessage) {
        String label = label(field);
        if (label == null) {
            return FALLBACK;
        }
        String reason = defaultMessage == null ? "" : defaultMessage;
        for (Map.Entry<Pattern, String> entry : BEAN_REASONS.entrySet()) {
            if (entry.getKey().matcher(reason).find()) {
                return label + entry.getValue();
            }
        }
        return label + "格式不正确";
    }
}

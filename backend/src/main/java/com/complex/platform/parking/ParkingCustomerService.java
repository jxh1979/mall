package com.complex.platform.parking;

// 文件版本：V0.04

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.common.money.MoneyRounding;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.gate.TerminalSwitchGuard;
import com.complex.platform.settlement.JdbcSettlementTransactionRepository;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HP-P01 客人端缴费（BSM L1435「出口扫码支付，或在官方 App/小程序提前缴费后在离场宽限时限内离场（业务参数）；不做车牌绑定无感支付」；
 * L1433「无牌车在入口扫码领取临时码、出口凭码缴费」；L1447「客人在官方 App/小程序缴费」；L1445「停车费收入归城市项目公司」）。
 * 按车牌或临时码查在场车辆、按本平台下发的同一套规则试算应缴，经统一支付意向收款，收款主体为本项目唯一有效的城市项目公司；
 * 付款成功后给出离场宽限截止，供停车系统按下发的缴费记录放行。不保存车牌与账户的代扣绑定。
 * HP-P01 第二组（BSM「停车场」章「消费抵免」条；第十二批操作员裁决补充第 4、11 条）：试算先扣免费时长再扣入场营业日的可用消费抵免时长；
 * 实际抵掉的计费小时按实际少收的钱折回（整次停车累计不抵免应缴与累计实缴之差，不足一小时按一小时），确实少收的缴费下单即预占抵免
 * （同一次抵免只一笔能用），抵到 0 元的直接记 0 元已缴；缴费成功才置抵免已使用并分担成本。已用在本次停车上的抵免续缴时随缴费带上，
 * 缴费成功按整次停车累计重算实际抵掉小时与成本，累计一分没少收的抵免退回可用、不计成本。
 */
@Service
public class ParkingCustomerService {

    private final JdbcTemplate jdbc;
    private final ParkingService parking;
    private final ParkingCreditService credits;
    private final FinanceService finance;
    private final JdbcSettlementTransactionRepository settlement;
    private final BusinessDayResolver businessDays;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;
    private final TerminalSwitchGuard terminalSwitch;

    public ParkingCustomerService(JdbcTemplate jdbc, ParkingService parking, ParkingCreditService credits, FinanceService finance,
            JdbcSettlementTransactionRepository settlement, BusinessDayResolver businessDays, IdempotencyService idempotency,
            AuditEventService audit, TerminalSwitchGuard terminalSwitch) {
        this.jdbc = jdbc;
        this.parking = parking;
        this.credits = credits;
        this.finance = finance;
        this.settlement = settlement;
        this.businessDays = businessDays;
        this.idempotency = idempotency;
        this.audit = audit;
        this.terminalSwitch = terminalSwitch;
    }

    /** 查在场车辆：入场时间、停车费（抵免前与抵免后）、本次可用消费抵免、平台已收、应缴与离场宽限截止。 */
    public Map<String, Object> lookup(String customerId, String projectId, String plateNo, String tempCode) {
        String project = requireText(projectId, "project_id");
        // PGM 第 744 行「前端隐藏只是辅助，后端接口必须独立校验」：按综合体的客人端终端开关以 project_id 所指项目在服务端判（不取 X-Project-Id）。
        terminalSwitch.checkTerminalEnabledAtProject(project);
        String plate = trimToNull(plateNo) == null ? null : ParkingService.plate(plateNo);
        String temp = trimToNull(tempCode);
        if (plate == null && temp == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请填写车牌或临时码");
        }
        String sessionId = jdbc.query("""
                SELECT id FROM parking_sessions WHERE project_id=? AND status='in_lot'
                   AND ((?::text IS NOT NULL AND plate_no=?) OR (?::text IS NOT NULL AND temp_code=?))
                 ORDER BY entered_at DESC LIMIT 1""", (rs, n) -> rs.getString(1), project, plate, plate, temp, temp)
                .stream().findFirst().orElseThrow(() -> ApiException.notFound("PARKING_SESSION_NOT_FOUND", "未找到在场车辆"));
        return quote(sessionId, Instant.now(), customerId, true);
    }

    /** 客人端：本项目当日消费抵免。 */
    public Map<String, Object> credit(String customerId, String projectId) {
        String project = requireText(projectId, "project_id");
        // PGM 第 744 行：按 project_id 所指项目在服务端判按综合体的客人端开关（与查费同口径）。
        terminalSwitch.checkTerminalEnabledAtProject(project);
        return credits.customerView(customerId, project);
    }

    /** 下单：锁停车记录，按应缴拉起支付意向；离场宽限内已缴的不能再下单。 */
    @Transactional
    public Map<String, Object> pay(String customerId, ParkingRequests.CustomerPayment r) {
        String key = r == null ? null : r.idempotencyKey();
        try {
            if (r == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "请选择停车记录");
            }
            // PGM 第 744 行「前端隐藏只是辅助，后端接口必须独立校验」：按综合体的客人端终端开关以停车记录所属项目在服务端判（不取 X-Project-Id），先于幂等。
            String requestedSession = trimToNull(r.parkingSessionId());
            if (requestedSession != null) {
                terminalSwitch.checkTerminalEnabledAtProject(jdbc.query("SELECT project_id FROM parking_sessions WHERE id=?",
                        (rs, n) -> rs.getString(1), requestedSession).stream().findFirst().orElse(null));
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> out = idempotency.execute("parkingCustomerPay:" + customerId, key, r, () -> {
                String sessionId = requireText(r.parkingSessionId(), "parking_session_id");
                Map<String, Object> s = jdbc.queryForList("SELECT project_id, status FROM parking_sessions WHERE id=? FOR UPDATE", sessionId)
                        .stream().findFirst().orElseThrow(() -> ApiException.notFound("PARKING_SESSION_NOT_FOUND", "未找到在场车辆"));
                if (!"in_lot".equals(s.get("status"))) {
                    throw ApiException.conflict("PARKING_SESSION_EXITED", "车辆已出场");
                }
                String project = (String) s.get("project_id");
                List<String> companies = settlement.findProjectCompanies(project);
                if (companies.size() != 1) {
                    throw ApiException.conflict("CITY_PROJECT_COMPANY_MISSING", "该项目须有且仅有一家有效的城市项目公司收取停车费");
                }
                Instant now = Instant.now();
                String id = "ppay_" + UUID.randomUUID();
                Map<String, Object> q = quote(sessionId, now, customerId, true);
                if (q.get("grace_until") != null && Instant.parse((String) q.get("grace_until")).isAfter(now)) {
                    throw ApiException.conflict("PARKING_ALREADY_PAID", "已缴费，请在离场宽限时限内离场");
                }
                String creditId = (String) q.get("credit_id");
                // 抵免成本按每小时内部结算价计（第十二批操作员裁决补充第 4 条），未配价不收单、不按 0 计。
                ParkingCreditService.HourPrice price = creditId == null ? null : credits.hourPrice(companies.getFirst());
                boolean fresh = creditId != null && "available".equals(q.get("credit_status"));
                if (fresh && !credits.reserve(creditId, id, sessionId)) {
                    // 同一次抵免已被本客人另一辆在场车的待付缴费占用：本笔按无抵免计。
                    q = quote(sessionId, now, customerId, false);
                    creditId = null;
                    price = null;
                    fresh = false;
                }
                long due = ((Number) q.get("due_cent")).longValue();
                if (due <= 0 && !fresh) {
                    throw ApiException.conflict("PARKING_NO_FEE_DUE", "当前无需缴费");
                }
                Object offsetHours = creditId == null ? null : q.get("credit_offset_hours");
                if (due == 0) {
                    // 消费抵免抵到 0 元：直接记已缴（无支付意向），停车系统按缴费记录放行。
                    Instant graceUntil = now.plus(Duration.ofMinutes(parking.requirePolicy(project).exitGraceMinutes()));
                    jdbc.update("""
                            INSERT INTO parking_payments (id, project_id, session_id, customer_id, amount_cent, status, paid_at, grace_until,
                                receiver_entity_id, idempotency_key, credit_id, credit_offset_hours, credit_hour_price_cent, credit_price_entry_id)
                            VALUES (?,?,?,?,0,'paid',?,?,?,?,?,?,?,?)""", id, project, sessionId, customerId, Timestamp.from(now),
                            Timestamp.from(graceUntil), companies.getFirst(), key, creditId, offsetHours, price.priceCent(), price.entryId());
                    credits.use(id, now);
                    audit.recordSuccess("parking.payment_paid", "not_applicable", "parking_payment", id, null, key,
                            Map.of("session_id", sessionId, "amount_cent", 0, "credit_id", creditId, "receiver_entity_id", companies.getFirst()));
                    return payment(id);
                }
                PaymentIntentRecord intent = finance.createPaymentIntent(new PaymentIntentRequest("parking_" + id, null, List.of(),
                        "parking_fee", id, due, "CNY", "wechat"));
                jdbc.update("""
                        INSERT INTO parking_payments (id, project_id, session_id, customer_id, amount_cent, payment_intent_id,
                            receiver_entity_id, idempotency_key, credit_id, credit_offset_hours, credit_hour_price_cent, credit_price_entry_id)
                        VALUES (?,?,?,?,?,?,?,?,?,?,?,?)""",
                        id, project, sessionId, customerId, due, intent.id(), companies.getFirst(), key, creditId, offsetHours,
                        price == null ? null : price.priceCent(), price == null ? null : price.entryId());
                Map<String, Object> details = new LinkedHashMap<>(Map.of("session_id", sessionId, "amount_cent", due,
                        "receiver_entity_id", companies.getFirst()));
                if (creditId != null) {
                    details.put("credit_id", creditId);
                }
                audit.recordSuccess("parking.payment_created", "not_applicable", "parking_payment", id, null, key, details);
                Map<String, Object> view = payment(id);
                view.put("payment_url", intent.paymentUrl());
                // ROOT-03-R18：App/小程序拉起参数（渠道按付款端签发、原样透传；渠道未配置时为空）。
                view.put("launch", intent.launch());
                return view;
            }, Map.class);
            return out;
        } catch (RuntimeException error) {
            recordFailure("parking.payment_create_failed", r == null ? null : r.parkingSessionId(), key, error);
            throw error;
        }
    }

    /** 确认：支付意向经渠道核验成功才记已缴，离场宽限截止＝缴费时刻＋本项目离场宽限时限；重复确认返回原结果。 */
    @Transactional
    public Map<String, Object> confirm(String customerId, String paymentId) {
        try {
            Map<String, Object> p = jdbc.queryForList("SELECT * FROM parking_payments WHERE id=? AND customer_id=? FOR UPDATE",
                            requireText(paymentId, "payment_id"), customerId).stream().findFirst()
                    .orElseThrow(() -> ApiException.notFound("PARKING_PAYMENT_NOT_FOUND", "缴费记录不存在"));
            // PGM 第 744 行：按综合体的客人端终端开关以缴费单所属项目在服务端判（不取 X-Project-Id），先于「重复确认返回原结果」。
            terminalSwitch.checkTerminalEnabledAtProject((String) p.get("project_id"));
            if ("paid".equals(p.get("status"))) {
                return payment(paymentId);
            }
            finance.confirmOnlinePaymentIntent((String) p.get("payment_intent_id"), null, null);
            Instant now = Instant.now();
            long grace = parking.requirePolicy((String) p.get("project_id")).exitGraceMinutes();
            jdbc.update("UPDATE parking_payments SET status='paid', paid_at=?, grace_until=? WHERE id=?", Timestamp.from(now),
                    Timestamp.from(now.plus(Duration.ofMinutes(grace))), paymentId);
            credits.use(paymentId, now);
            audit.recordSuccess("parking.payment_paid", "not_applicable", "parking_payment", paymentId, null, paymentId,
                    Map.of("amount_cent", p.get("amount_cent"), "receiver_entity_id", p.get("receiver_entity_id")));
            return payment(paymentId);
        } catch (RuntimeException error) {
            recordFailure("parking.payment_confirm_failed", paymentId, paymentId, error);
            throw error;
        }
    }

    private Map<String, Object> quote(String sessionId, Instant now, String customerId, boolean withCredit) {
        Map<String, Object> s = jdbc.queryForMap("""
                SELECT s.id, s.project_id, s.plate_no, s.temp_code, s.entered_at, s.vehicle_kind,
                       COALESCE((SELECT sum(p.amount_cent) FROM parking_payments p WHERE p.session_id=s.id AND p.status='paid'), 0) AS paid_cent,
                       (SELECT max(p.grace_until) FROM parking_payments p WHERE p.session_id=s.id AND p.status='paid') AS grace_until
                  FROM parking_sessions s WHERE s.id=?""", sessionId);
        ParkingService.Policy policy = parking.requirePolicy((String) s.get("project_id"));
        Instant entered = ((Timestamp) s.get("entered_at")).toInstant();
        ParkingCreditService.Credit credit = withCredit && customerId != null
                ? credits.applicable(customerId, (String) s.get("project_id"), entered, sessionId) : null;
        ParkingFeeCalculator.Quote q = ParkingFeeCalculator.quote(entered, now, (int) policy.freeMinutes(), policy.hourlyRateCent(),
                policy.dailyCapCent(), credit == null ? 0 : (int) credit.creditMinutes(), t -> businessDays.endInstant(businessDays.businessDate(t)));
        long gross = q.grossFeeCent();
        long fee = q.netFeeCent();
        // 停车系统按下发的员工车牌认定为员工车辆的，按本项目员工优惠（万分比减免，10000＝免费）。
        if ("staff".equals(s.get("vehicle_kind"))) {
            gross -= MoneyRounding.bpsOf(gross, policy.staffDiscountBp());
            fee -= MoneyRounding.bpsOf(fee, policy.staffDiscountBp());
        }
        long paid = ((Number) s.get("paid_cent")).longValue();
        // 第十二批裁决补充第 11 条「按实际少收的钱算」：整次停车累计不抵免应缴与累计实缴（已缴＋本次应缴）之差折回小时，
        // 覆盖「先缴费、后达标再续缴」——已缴部分没有少收。
        long offsetHours = ParkingFeeCalculator.offsetHours(gross - Math.max(fee, paid), policy.hourlyRateCent(), q.offsetHours());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("parking_session_id", s.get("id"));
        out.put("project_id", s.get("project_id"));
        out.put("plate_no", s.get("plate_no"));
        out.put("temp_code", s.get("temp_code"));
        out.put("entered_at", entered.toString());
        out.put("gross_fee_cent", gross);
        out.put("fee_cent", fee);
        // 本次可用的消费抵免：确实少收（credit_offset_hours＞0）才随缴费占用并在缴费成功后算用掉；已用在本次停车上的续缴时也带上
        // （实际抵掉小时可降为 0），缴费成功按整次停车累计重算，一分没少收的退回可用。
        out.put("credit_status", credit == null ? null : credit.status());
        out.put("credit_id", credit != null && (offsetHours > 0 || "used".equals(credit.status())) ? credit.id() : null);
        out.put("credit_minutes", credit == null ? 0 : credit.creditMinutes());
        out.put("credit_offset_hours", offsetHours);
        out.put("paid_cent", paid);
        out.put("due_cent", Math.max(0, fee - paid));
        out.put("grace_until", s.get("grace_until") instanceof Timestamp t ? t.toInstant().toString() : null);
        return out;
    }

    private Map<String, Object> payment(String id) {
        return ParkingVendorService.iso(jdbc.queryForMap("""
                SELECT id, session_id AS parking_session_id, amount_cent, payment_intent_id, status, paid_at, grace_until, receiver_entity_id,
                       credit_id, credit_offset_hours
                  FROM parking_payments WHERE id=?""", id));
    }

    private void recordFailure(String event, String id, String key, RuntimeException error) {
        try {
            audit.recordFailure(event, "not_applicable", "parking_payment", id == null ? "unknown" : id, null, key,
                    error instanceof ApiException api ? api.code() : error.getClass().getSimpleName());
        } catch (RuntimeException auditError) {
            error.addSuppressed(auditError);
        }
    }
}

package com.complex.platform.customer;

// 文件版本：V0.05

import static com.complex.platform.common.Strings.requireText;

import com.complex.platform.bar.BarOrderRecord;
import com.complex.platform.bar.BarOrderRequest;
import com.complex.platform.bar.BarOrderService;
import com.complex.platform.bar.BarProductRecord;
import com.complex.platform.bar.BarRepository;
import com.complex.platform.bar.BarTableRecord;
import com.complex.platform.bar.BarTableSessionRecord;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.customer.dto.CustomerBarMenuItemResponse;
import com.complex.platform.customer.dto.CustomerBarScanOrderRequest;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.gate.TerminalIdentityHolder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ROOT-03-R7 扫码点单服务端（含 ROOT-03-R24 客人不得自助下赠品）。依据：BSM 第 1025 行「扫码点单只适用于散台，卡座不开放扫码点单。散台也须服务员先开台才能扫码点单，
 * 开台后客人扫描桌台二维码即可自助点单，下单时即时付款、付款成功订单才成立，不挂账；酒水必须先完成付款才能出品上台。付款成功后所点酒水计入该桌位当前账单，
 * 账单上逐项列出物品名称与价格，并分别标注已上菜与未上菜……此后新客人看到的是一张空账单，看不到上一位客人的消费」；ENUM 第 129 行「非会员可以浏览、点单和付款」；
 * ENUM 第 111 行「沽清商品在客人扫码点单菜单上置灰展示为已售完，不隐藏」；BSM 第 585 行扫码进入点单页面的码保持固定（桌台码＝台位编号）。
 *
 * <p>下单只落待付单（bar_scan_order_payments）与在线支付单，不建酒吧订单、不出品；渠道查单成功才在当前开台会话上成单（计入本桌当前账单、进出品流转）。
 * 付款成功时该桌已结束接待（会话已关）的，已收款登记原路待退并报 409。匿名可用，带有效客人会话时记会员本人。
 */
@Service
public class CustomerBarScanOrderService {

    static final String ORDER_TYPE = "bar_scan_order";
    private static final String LOOSE_TABLE = "loose_table";
    /** 退款中原因码：付款成功时该桌已结束接待。其余原因码＝成单被拒的错误码（沽清、下架、开关关、改价）。 */
    private static final String SESSION_CLOSED = "BAR_TABLE_SESSION_CLOSED";

    /** 本桌当前账单一项。served＝已上菜（出品已送达）。 */
    public record BillItem(String productName, long unitPriceCent, int quantity, long amountCent, boolean served) {
    }

    /** 本桌当前开台会话的账单（不含任何客人身份）。 */
    public record CurrentBill(String tableId, List<BillItem> items, long totalCent) {
    }

    private final CustomerService customers;
    private final BarRepository bars;
    private final BarOrderService barOrders;
    private final FinanceService finance;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final TransactionTemplate transaction;
    private final TransactionTemplate savepoint;
    private final CustomerProjectSwitch projectSwitch;

    public CustomerBarScanOrderService(CustomerService customers, BarRepository bars, BarOrderService barOrders,
            FinanceService finance, IdempotencyService idempotency, AuditEventService audit, JdbcTemplate jdbc,
            ObjectMapper json, PlatformTransactionManager transactionManager, CustomerProjectSwitch projectSwitch) {
        this.customers = customers;
        this.bars = bars;
        this.barOrders = barOrders;
        this.finance = finance;
        this.idempotency = idempotency;
        this.audit = audit;
        this.jdbc = jdbc;
        this.json = json;
        this.transaction = new TransactionTemplate(transactionManager);
        this.savepoint = new TransactionTemplate(transactionManager);
        this.savepoint.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_NESTED);
        this.projectSwitch = projectSwitch;
    }

    /** 菜单：本酒吧在售商品（沽清的置灰标 sold_out，不隐藏），不含内部结算价。 */
    public PageResponse<CustomerBarMenuItemResponse> menu(String tableId) {
        // PGM 第 744 行「前端隐藏只是辅助，后端接口必须独立校验」：扫码点单可不登录、终端类型自报，综合体按台位所属酒吧在服务端取。
        projectSwitch.table(tableId);
        BarTableSessionRecord session = openLooseSession(tableId);
        List<CustomerBarMenuItemResponse> items = bars.listProducts(Optional.ofNullable(session.merchantId())).stream()
                .filter(p -> session.barId().equals(p.barId()) && "active".equals(p.status()) && p.archivedAt() == null
                        && (p.deleteStatus() == null || "none".equals(p.deleteStatus())))
                .map(p -> new CustomerBarMenuItemResponse(p.id(), p.productName(), p.productType(), p.priceCent(), p.currency(), p.soldOut()))
                .toList();
        return new PageResponse<>(items, 1, items.size(), items.size());
    }

    /** 下单：按商品现价计价、只落待付单与在线支付单，返回拉起参数；此时不成单、不出品。 */
    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> create(String customerToken, String tableId, String idempotencyKey, CustomerBarScanOrderRequest request) {
        String id = tableId;
        try {
            String terminal = CustomerService.requireCustomerTerminal(TerminalIdentityHolder.trustedTerminalType(null));
            String customerId = customerToken == null ? null : customers.resolveCustomerIdQuietly(customerToken).orElse(null);
            String key = requireText(idempotencyKey, "idempotency_key");
            projectSwitch.table(tableId);
            Map<String, Object> view = idempotency.execute("customerBarScanOrder:" + requireText(tableId, "table_id"), key, request,
                    () -> createPending(tableId, customerId, terminal, request), Map.class);
            id = (String) view.get("scan_order_id");
            audit.recordSuccess("customer.bar_scan_order.created", "not_applicable", "bar_scan_order", id, null, key,
                    Map.of("table_id", tableId, "amount_cent", view.get("amount_cent")));
            return view;
        } catch (RuntimeException failure) {
            recordFailure("customer.bar_scan_order.create_failed", id, idempotencyKey, failure);
            throw failure;
        }
    }

    /** 回前台确认：不加事务——确认内核在自己的事务里提交，成不了单的原路待退须先落库，再向客人报 409。 */
    public Map<String, Object> confirm(String tableId, String scanOrderId, String idempotencyKey) {
        try {
            // 客人确认入口才判；服务端结算作业直接调 settle，不经此入口。
            projectSwitch.table(tableId);
            Map<String, Object> view = transaction.execute(status -> settle(scanOrderId, requireText(tableId, "table_id")));
            if ("refunding".equals(view.get("status"))) {
                if (SESSION_CLOSED.equals(view.get("reject_code"))) {
                    throw ApiException.conflict("BAR_SCAN_ORDER_SESSION_CLOSED_REFUNDING", "付款成功但该桌已结束接待，款项将原路退回");
                }
                throw ApiException.conflict("BAR_SCAN_ORDER_REJECTED_REFUNDING",
                        "付款成功但未能成单（所点酒水已售完、已下架或价格已变），款项将原路退回");
            }
            audit.recordSuccess("customer.bar_scan_order.paid", "not_applicable", "bar_scan_order", scanOrderId, null,
                    idempotencyKey, Map.of("bar_order_id", String.valueOf(view.get("bar_order_id"))));
            return view;
        } catch (RuntimeException failure) {
            recordFailure("customer.bar_scan_order.pay_failed", scanOrderId, idempotencyKey, failure);
            throw failure;
        }
    }

    /**
     * 到账确认内核（客人确认与服务端结算作业共用，调用方提供事务；expectedTableId 为空＝作业）：渠道查单成功才成单；已成单或已转原路待退的重复确认返回现状。
     */
    public Map<String, Object> settle(String scanOrderId, String expectedTableId) {
        Map<String, Object> row = jdbc.queryForList("SELECT * FROM bar_scan_order_payments WHERE id = ? FOR UPDATE",
                        requireText(scanOrderId, "scan_order_id")).stream().findFirst()
                .filter(found -> expectedTableId == null || expectedTableId.equals(found.get("table_id")))
                .orElseThrow(() -> ApiException.notFound("BAR_SCAN_ORDER_NOT_FOUND", "扫码点单不存在"));
        if (!"pending".equals(row.get("status"))) {
            return view(row, null);
        }
        // 第十八批 Q 段第 139 条：按发起时同一对（该酒吧商户、商户直收）核对渠道回执。
        PaymentIntentRecord paid = finance.confirmOnlinePaymentIntent((String) row.get("payment_intent_id"), (String) row.get("merchant_id"),
                "merchant_prepaid");
        String sessionId = (String) row.get("session_id");
        boolean sessionOpen = bars.findTableSessionById(sessionId).map(s -> "open".equals(s.status())).orElse(false);
        Timestamp now = Timestamp.from(Instant.now());
        if (!sessionOpen) {
            return rejectPaid(row, paid, SESSION_CLOSED, "扫码点单付款成功时该桌已结束接待，原路退回", now);
        }
        long paidAmount = ((Number) row.get("amount_cent")).longValue();
        BarOrderRecord order;
        try {
            // 成单放在保存点里：下单后、到账前被沽清或下架、扫码点单被关、或改了价（账单金额≠实付）都成不了单——只回滚成单，已到账的收款保留并原路待退。
            order = savepoint.execute(status -> {
                BarOrderRecord placed = barOrders.placePaidCustomerScanOrder(sessionId,
                        new BarOrderRequest("scan_" + row.get("id"), null, List.of(), null, null, null, readItems(row), List.of(), null),
                        (String) row.get("customer_id"), (String) row.get("created_terminal"));
                if (placed.totalAmountCent() != paidAmount) {
                    throw ApiException.conflict("BAR_SCAN_ORDER_PRICE_CHANGED", "下单后商品价格已变，账单金额与实付不一致");
                }
                return placed;
            });
        } catch (ApiException rejected) {
            return rejectPaid(row, paid, rejected.code(), "扫码点单付款成功但未能成单（" + rejected.code() + "），原路退回", now);
        }
        jdbc.update("UPDATE bar_scan_order_payments SET status = 'paid', bar_order_id = ?, paid_at = ?, updated_at = ? WHERE id = ?",
                order.id(), now, now, row.get("id"));
        return view(jdbc.queryForMap("SELECT * FROM bar_scan_order_payments WHERE id = ?", row.get("id")), null);
    }

    /** 已收客人款却成不了单：按原付款登记原路待退（退款来源 bar_scan_order_rejection），待付单置退款中并记下原因码。 */
    private Map<String, Object> rejectPaid(Map<String, Object> row, PaymentIntentRecord paid, String rejectCode, String reason,
            Timestamp now) {
        finance.recordRejectedTicketPaymentRefund("bar_scan_order_rejection", (String) row.get("id"),
                ((Number) row.get("amount_cent")).longValue(), paid.channel(), paid.gatewayRef(),
                (String) row.get("merchant_id"), (String) row.get("bar_id"), reason);
        jdbc.update("UPDATE bar_scan_order_payments SET status = 'refunding', reject_code = ?, updated_at = ? WHERE id = ?",
                rejectCode, now, row.get("id"));
        return view(jdbc.queryForMap("SELECT * FROM bar_scan_order_payments WHERE id = ?", row.get("id")), null);
    }

    /** 本桌当前账单：只列当前开台会话已成单的酒水（不含已撤的），没开台＝空账单。 */
    public CurrentBill currentBill(String tableId) {
        projectSwitch.table(tableId);
        BarTableRecord table = looseTable(tableId);
        Optional<String> session = openSessionId(table.id());
        if (session.isEmpty()) {
            return new CurrentBill(table.id(), List.of(), 0L);
        }
        List<BillItem> items = jdbc.query("""
                SELECT i.product_name, i.unit_price_cent, i.quantity, i.amount_cent, i.delivery_status
                  FROM bar_order_items i
                  JOIN bar_order_service_sources s ON s.order_id = i.order_id
                 WHERE s.session_id = ? AND NOT i.voided
                 ORDER BY i.created_at, i.id
                """, (rs, n) -> new BillItem(rs.getString("product_name"), rs.getLong("unit_price_cent"), rs.getInt("quantity"),
                rs.getLong("amount_cent"), "delivered".equals(rs.getString("delivery_status"))), session.get());
        return new CurrentBill(table.id(), items, items.stream().mapToLong(BillItem::amountCent).sum());
    }

    private Map<String, Object> createPending(String tableId, String customerId, String terminal, CustomerBarScanOrderRequest request) {
        barOrders.requireScanOrderEnabled();
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "items is required");
        }
        String channel = CustomerPaymentService.channelFor(terminal, request.channel());
        BarTableSessionRecord session = openLooseSession(tableId);
        List<Map<String, Object>> lines = new ArrayList<>();
        long amount = 0L;
        for (Map<String, Object> item : request.items()) {
            // ROOT-03-R24（BSM 第 1069 行「凡免单、打折与赠送，一律记录原因、责任人、批准人」）：客人自助点单不能选赠送，计价一律按商品现价。
            if (item.get("is_gift") != null && Boolean.parseBoolean(String.valueOf(item.get("is_gift")))) {
                throw ApiException.badRequest("BAR_SCAN_ORDER_GIFT_NOT_ALLOWED", "客人自助点单不能选择赠送");
            }
            String productId = requireText(item.get("product_id") == null ? null : String.valueOf(item.get("product_id")), "product_id");
            int quantity = item.get("quantity") instanceof Number number ? number.intValue() : 0;
            if (quantity <= 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", "数量必须大于 0");
            }
            BarProductRecord product = bars.findProductById(productId)
                    .filter(p -> session.barId().equals(p.barId()) && "active".equals(p.status()) && p.archivedAt() == null
                            && (p.deleteStatus() == null || "none".equals(p.deleteStatus())))
                    .orElseThrow(() -> ApiException.notFound("BAR_PRODUCT_NOT_FOUND", "酒吧商品不存在或已下架"));
            if (product.soldOut()) {
                throw ApiException.conflict("PRODUCT_SOLD_OUT", "商品已沽清，不能下单");
            }
            amount = Math.addExact(amount, Math.multiplyExact(product.priceCent(), quantity));
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("product_id", product.id());
            line.put("quantity", quantity);
            lines.add(line);
        }
        if (amount <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "下单金额必须大于 0");
        }
        // 第十八批 T 段 157-R1（BSM:1064、ENUM:117 最低消费是下单的前置条件）：收款前按成单同一口径判，不满足不建支付单，免得收了钱才拒单。
        barOrders.requireMinSpendReached(session.id(), session.tableId(), amount);
        String id = "bso_" + UUID.randomUUID().toString().replace("-", "");
        // 第十八批 Q 段第 139 条（决策台账 §17「客人仍向酒吧全额付款，酒吧确认对客收入」；§16「客人在酒吧发生的点酒、套餐、二次消费、额外酒水等收入，
        // 归实际销售并经营该酒吧的法律主体」「顶层平台公司不直接取得酒吧消费收入」）：收款主体＝该散台会话所属的酒吧商户、商户直收，与订台定金同一资金归属契约。
        PaymentIntentRecord intent = finance.createPaymentIntent(new PaymentIntentRequest(id, null, List.of(), ORDER_TYPE, id,
                amount, "CNY", channel, null, session.merchantId(), "merchant_prepaid"));
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
                INSERT INTO bar_scan_order_payments (id, bar_id, merchant_id, table_id, session_id, customer_id, items, amount_cent,
                    payment_intent_id, status, created_terminal, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, 'pending', ?, ?, ?)
                """, id, session.barId(), session.merchantId(), session.tableId(), session.id(), customerId, write(lines), amount,
                intent.id(), terminal, now, now);
        return view(jdbc.queryForMap("SELECT * FROM bar_scan_order_payments WHERE id = ?", id), intent);
    }

    private BarTableSessionRecord openLooseSession(String tableId) {
        BarTableRecord table = looseTable(tableId);
        return openSessionId(table.id()).flatMap(bars::findTableSessionById)
                .orElseThrow(() -> ApiException.conflict("BAR_TABLE_NOT_OPENED", "请先由服务员开台后再扫码点单"));
    }

    private BarTableRecord looseTable(String tableId) {
        BarTableRecord table = bars.findTableById(requireText(tableId, "table_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "台位不存在或已被删除"));
        if (!LOOSE_TABLE.equals(table.tableType())) {
            throw ApiException.conflict("BAR_SCAN_ORDER_TABLE_NOT_ALLOWED", "扫码点单只适用于散台，卡座不开放扫码点单");
        }
        return table;
    }

    private Optional<String> openSessionId(String tableId) {
        return jdbc.queryForList("SELECT id FROM bar_table_sessions WHERE table_id = ? AND status = 'open' ORDER BY opened_at DESC, id LIMIT 1",
                String.class, tableId).stream().findFirst();
    }

    private Map<String, Object> view(Map<String, Object> row, PaymentIntentRecord intent) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("scan_order_id", row.get("id"));
        view.put("table_id", row.get("table_id"));
        view.put("amount_cent", ((Number) row.get("amount_cent")).longValue());
        view.put("status", row.get("status"));
        view.put("bar_order_id", row.get("bar_order_id"));
        view.put("payment_intent_id", row.get("payment_intent_id"));
        if (row.get("reject_code") != null) {
            view.put("reject_code", row.get("reject_code")); // 只供 confirm 选 409 原因，退款中时不随响应返回
        }
        if (intent != null) {
            view.put("channel", intent.channel());
            view.put("gateway_status", intent.gatewayStatus());
            view.put("payment_url", intent.paymentUrl());
            view.put("launch", intent.launch());
        }
        return view;
    }

    private List<Map<String, Object>> readItems(Map<String, Object> row) {
        try {
            return json.readValue(String.valueOf(row.get("items")), new TypeReference<List<Map<String, Object>>>() { });
        } catch (com.fasterxml.jackson.core.JsonProcessingException corrupt) {
            throw new IllegalStateException("bar_scan_order_payments.items is not valid json", corrupt);
        }
    }

    private String write(List<Map<String, Object>> lines) {
        try {
            return json.writeValueAsString(lines);
        } catch (com.fasterxml.jackson.core.JsonProcessingException unserializable) {
            throw new IllegalStateException("scan order items are not serializable", unserializable);
        }
    }

    private void recordFailure(String event, String businessId, String key, RuntimeException failure) {
        try {
            audit.recordFailure(event, "not_applicable", "bar_scan_order", businessId == null ? "unknown" : businessId, null, key,
                    failure instanceof ApiException api ? api.code() : failure.getClass().getSimpleName());
        } catch (RuntimeException auditFailure) {
            failure.addSuppressed(auditFailure);
        }
    }
}

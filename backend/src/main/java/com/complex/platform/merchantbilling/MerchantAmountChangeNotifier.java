package com.complex.platform.merchantbilling;

// 文件版本：V0.01

import com.complex.platform.finance.WorkingDayCalendar;
import com.complex.platform.notification.NotificationService;
import com.complex.platform.settlement.InternalPayDayService;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 第十八批第 13 条：商户应收应付金额变化短信。BSM 第 1597 行「凡涉及商户应收应付金额发生变化的事项一律短信通知，包括结算金额出账与调整、
 * 对账差异认定结果、押金抵扣与退还、违约金计提、分红出单与发放，通知中写明金额、原因与异议截止日期……默认发给商户主要联系人，可在后台改为
 * 指定接收人」；第 1598 行这类通知不可关闭（事件在 NotificationSwitchResolver.MANDATORY_EVENTS）。
 * 业务提交后才发（无事务即刻发）；往来对方不是商户的不发；幂等按发送日志 request_id；发送失败只记日志，不影响业务与异议期。
 * 异议截止日：给定即用；否则按可查看之日次日起算本项目异议期工作日；两者都无（对账差异认定结果不在异议期适用清单）不写。
 */
@Component
public class MerchantAmountChangeNotifier {

    public static final String DOMAIN = "merchant_amount";
    private static final Logger log = LoggerFactory.getLogger(MerchantAmountChangeNotifier.class);

    private final JdbcTemplate jdbc;
    private final NotificationService notifications;
    private final MerchantFeeNotificationRecipientResolver recipients;
    private final InternalPayDayService payDays;
    private final WorkingDayCalendar calendar;

    public MerchantAmountChangeNotifier(JdbcTemplate jdbc, @Lazy NotificationService notifications,
            MerchantFeeNotificationRecipientResolver recipients, @Lazy InternalPayDayService payDays, WorkingDayCalendar calendar) {
        this.jdbc = jdbc;
        this.notifications = notifications;
        this.recipients = recipients;
        this.payDays = payDays;
        this.calendar = calendar;
    }

    public void notifyAfterCommit(String merchantId, String event, long amountCent, String reason, LocalDate fixedDeadline,
            LocalDate visibleOn, String projectId, String refKey) {
        Runnable send = () -> send(merchantId, event, amountCent, reason, fixedDeadline, visibleOn, projectId, refKey);
        if (TransactionSynchronizationManager.isSynchronizationActive() && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
            return;
        }
        send.run();
    }

    private void send(String merchantId, String event, long amountCent, String reason, LocalDate fixedDeadline,
            LocalDate visibleOn, String projectId, String refKey) {
        try {
            List<String> merchantProject = jdbc.queryForList("SELECT project_id FROM merchants WHERE id=?", String.class, merchantId);
            if (merchantProject.isEmpty()) {
                return;
            }
            String project = projectId != null ? projectId : merchantProject.getFirst();
            LocalDate deadline = fixedDeadline != null ? fixedDeadline : visibleOn == null ? null
                    : calendar.objectionDeadline(visibleOn, payDays.objectionPolicy(project).workingDays(), project);
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("merchant_id", merchantId);
            params.put("amount_cent", amountCent);
            params.put("reason", reason);
            params.put("ref", refKey);
            if (deadline != null) {
                params.put("objection_deadline", deadline.toString());
            }
            notifications.notify(DOMAIN, event, recipients.resolvePhone(merchantId, event), "sms.merchant_amount." + event, params,
                    "mamt:" + event + ":" + refKey);
        } catch (RuntimeException exception) {
            log.warn("merchant amount notification failed: event={}, merchant_id={}, ref={}", event, merchantId, refKey, exception);
        }
    }
}

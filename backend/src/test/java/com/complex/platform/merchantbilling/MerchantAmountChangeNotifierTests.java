package com.complex.platform.merchantbilling;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.finance.WorkingDayCalendar;
import com.complex.platform.notification.NotificationService;
import com.complex.platform.settlement.InternalPayDayService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 第十八批第 13 条（BSM 第 1597 行「通知中写明金额、原因与异议截止日期……默认发给商户主要联系人，可在后台改为指定接收人」）：
 * 金额变化通知的报文要素、异议截止日取法（给定优先，否则可查看之日次日起算本项目异议期，都没有不写）；对方非商户不发、失败不外抛。
 */
class MerchantAmountChangeNotifierTests {

    private static final LocalDate VISIBLE = LocalDate.parse("2026-10-27");

    private final org.springframework.jdbc.core.JdbcTemplate jdbc = mock(org.springframework.jdbc.core.JdbcTemplate.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final MerchantFeeNotificationRecipientResolver recipients = mock(MerchantFeeNotificationRecipientResolver.class);
    private final InternalPayDayService payDays = mock(InternalPayDayService.class);
    private final WorkingDayCalendar calendar = mock(WorkingDayCalendar.class);
    private final MerchantAmountChangeNotifier notifier =
            new MerchantAmountChangeNotifier(jdbc, notifications, recipients, payDays, calendar);

    @BeforeEach
    void merchant() {
        when(jdbc.queryForList(anyString(), eq(String.class), eq("m1"))).thenReturn(List.of("p1"));
        when(recipients.resolvePhone(eq("m1"), anyString())).thenReturn("13800000000");
        when(payDays.objectionPolicy("p1")).thenReturn(new InternalPayDayService.ObjectionPolicy("p1", 10, 1, 20));
        when(calendar.objectionDeadline(VISIBLE, 10, "p1")).thenReturn(LocalDate.parse("2026-11-10"));
    }

    @Test
    void sendsWithAmountReasonAndDeadlineFromVisibleDay() {
        notifier.notifyAfterCommit("m1", "penalty", 5_000L, "提前退租违约金", null, VISIBLE, null, "st1");

        assertThat(sent("penalty", "mamt:penalty:st1")).containsEntry("merchant_id", "m1").containsEntry("amount_cent", 5_000L)
                .containsEntry("reason", "提前退租违约金").containsEntry("objection_deadline", "2026-11-10");
    }

    @Test
    void fixedDeadlineWinsAndNoBasisLeavesDeadlineOut() {
        notifier.notifyAfterCommit("m1", "settlement_statement", 100L, "门票分销出账", LocalDate.parse("2026-12-01"), VISIBLE, "p1", "a1");
        notifier.notifyAfterCommit("m1", "reconciliation_difference", 7L, "金额差异", null, null, "p1", "d1");

        assertThat(sent("settlement_statement", "mamt:settlement_statement:a1")).containsEntry("objection_deadline", "2026-12-01");
        assertThat(sent("reconciliation_difference", "mamt:reconciliation_difference:d1")).doesNotContainKey("objection_deadline");
        verifyNoInteractions(calendar);
    }

    @Test
    void nonMerchantAndCalendarGapSendNothingAndNeverThrow() {
        when(jdbc.queryForList(anyString(), eq(String.class), eq("supplier1"))).thenReturn(List.of());
        when(calendar.objectionDeadline(any(), anyInt(), any())).thenThrow(ApiException.badRequest("WORKING_CALENDAR_MISSING", "缺日历"));

        assertThatCode(() -> {
            notifier.notifyAfterCommit("supplier1", "adjustment", 1L, "调整", null, VISIBLE, null, "x");
            notifier.notifyAfterCommit("m1", "penalty", 1L, "违约金", null, VISIBLE.plusDays(1), null, "y");
        }).doesNotThrowAnyException();
        verifyNoInteractions(notifications);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sent(String event, String requestId) {
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(notifications).notify(eq("merchant_amount"), eq(event), eq("13800000000"), eq("sms.merchant_amount." + event),
                params.capture(), eq(requestId));
        return params.getValue();
    }
}

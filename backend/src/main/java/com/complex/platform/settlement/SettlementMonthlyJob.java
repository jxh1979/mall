package com.complex.platform.settlement;

// 文件版本：V0.01
import org.springframework.beans.factory.annotation.Value;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.LocalZone;
import java.time.Clock;
import java.time.YearMonth;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 结算重构批2-3：月度通票分账作业（照 SettlementExpiryJob 范式，薄 @Scheduled 包装）。
 *
 * <p>业务逻辑全在 {@link TicketDistributionSettlementService#settleMonth}（可直调便于单测）。
 *
 * <p>AUD-035：不再只结紧邻上月——那样一次失败/停机就永久缺月。改为每次跑回看窗口
 * [上月-backfillMonths+1, 上月] 逐月幂等补算：无单(缺月)或 draft/confirmed(未过账)月补跑，
 * 已过账(settled/reversed)月跳过（settleMonth 内部对其 fail-closed 抛 SETTLEMENT_ALREADY_POSTED，
 * 这里先按期预判状态避免撞它中断整批）。幂等：settleMonth 按项目×月 upsert 覆盖，重算安全；
 * 晚到核销也会在下次回看窗口内并入仍未过账的月。
 *
 * <p>@Scheduled @ConditionalOnProperty 默认关闭——需显式开 settlement.monthly.scheduled.enabled=true 才注册。
 * <p>🔴 <b>过账独立闸</b>:作业内的 postBill 过账受<b>独立开关</b> settlement.monthly.posting.enabled(默认 false·不进
 * application-prod.yml)门控,与作业运行开关解耦。prod 月度作业本就开着(算账单),但过账保持关,直到用户拍板才激活账本首落。
 */
@Component
public class SettlementMonthlyJob {

    // 🔴 P1(2026-07-21):调度线程无登录主体,过账走 FinanceService.createArApItem(@GuardedAction)靠 currentActor()
    //   在生产必抛 UNAUTHENTICATED(自动过账彻底不工作、每月复现,且原被 SettlementMonthlyJobPostingTests 注入伪主体掩盖)。
    //   照 FacilityInspectionScheduleJob 范式装配系统主体:平台管理员(platform_admin·active)使过账在无人上下文能过鉴权/审计。
    private static final AccessActor SCHEDULER_ACTOR = new AccessActor(
            "system_scheduler", "system_scheduler", "platform_admin", null, null, "active");

    private final TicketDistributionSettlementService service;
    private final JdbcSettlementBillRepository repository;
    private final SettlementPostingService postingService;
    private final Clock clock;
    private final boolean scheduledEnabled;
    /** 过账独立闸(与作业运行开关解耦):默认 false·不进 application-prod.yml,过账需用户拍板后单独翻 true 才激活账本首落。 */
    private final boolean postingEnabled;

    /** TK-29（BSM L1673 月结定格即出结果）：月结回看窗内已锁定且事实已确认的演出场次补跑三项结算；可选装配，测试构造器不带。 */
    @Autowired(required = false)
    MediaPerformanceSettlementService mediaSettlement;

    /** 每次跑回看的月数：补齐停机/失败留下的缺月（默认 6，覆盖数月连续停机的补跑窗口）。 */
    @Value("${settlement.monthly.backfill-months:6}")
    int backfillMonths = 6;

    @Autowired
    public SettlementMonthlyJob(
            TicketDistributionSettlementService service,
            JdbcSettlementBillRepository repository,
            SettlementPostingService postingService,
            @Value("${settlement.monthly.scheduled.enabled:false}") boolean scheduledEnabled,
            @Value("${settlement.monthly.posting.enabled:false}") boolean postingEnabled) {
        this(service, repository, postingService, Clock.systemUTC(), scheduledEnabled, postingEnabled);
    }

    /** 测试构造器:作业开 + 过账开(验证过账开时的自动过账行为)。 */
    SettlementMonthlyJob(TicketDistributionSettlementService service, JdbcSettlementBillRepository repository,
            SettlementPostingService postingService, Clock clock) {
        this(service, repository, postingService, clock, true, true);
    }

    SettlementMonthlyJob(TicketDistributionSettlementService service, JdbcSettlementBillRepository repository,
            SettlementPostingService postingService, Clock clock, boolean scheduledEnabled, boolean postingEnabled) {
        this.service = service;
        this.repository = repository;
        this.postingService = postingService;
        this.clock = clock;
        this.scheduledEnabled = scheduledEnabled;
        this.postingEnabled = postingEnabled;
    }

    /** @Scheduled 薄包装:默认关闭,每月 1 日 03:00 跑;回看窗口逐月幂等补算(AUD-035)。 */
    @ConditionalOnProperty(name = "settlement.monthly.scheduled.enabled", havingValue = "true")
    @Scheduled(cron = "${settlement.monthly.scheduled.cron:0 0 3 1 * *}")
    public void scheduledSettle() {
        if (!scheduledEnabled) {
            return;
        }
        // 🔴 P1:装配系统主体(平台管理员),使过账 createArApItem(@GuardedAction)在无人调度线程能过 currentActor()/鉴权/审计;
        //   try/finally 严格还原上一上下文(无则 clear),不污染线程池后续任务。仅过账(postingEnabled)才真需要主体,
        //   但整段包住无害且简单(settleMonth 内部不依赖主体,读种子/upsert 走仓储)。
        SecurityContext previousContext = SecurityContextHolder.getContext();
        Authentication previousAuthentication = previousContext.getAuthentication();
        try {
            SecurityContext systemContext = SecurityContextHolder.createEmptyContext();
            systemContext.setAuthentication(
                    new UsernamePasswordAuthenticationToken(SCHEDULER_ACTOR, null, List.of()));
            SecurityContextHolder.setContext(systemContext);

            // 归属月按所在地(北京)日历月:财务口径的"上月"是北京的上月。锚 UTC 时,作业若落在北京 1 号凌晨
            //(=UTC 上月末)会把"上月"算成上上月、整月漏结(现 cron 恰在服务器凌晨跑才侥幸不错月)。
            // 仅改归属月的时区锚——分账瀑布/守恒校验/金额精度/配置中心规则一概不动。
            YearMonth lastMonth = YearMonth.now(clock.withZone(LocalZone.ID)).minusMonths(1);
            int window = Math.max(1, backfillMonths);
            for (int i = window - 1; i >= 0; i--) {
                YearMonth month = lastMonth.minusMonths(i);
                // 平台口径 projectId=null;按期预判状态,只对缺月/未过账月补算,已过账(settled/reversed)跳过。
                String status = repository.findBillStatusByPeriod(null, month.atDay(1), month.atEndOfMonth());
                if (status == null || "draft".equals(status) || "confirmed".equals(status)) {
                    String billId = service.settleMonth(null, month);
                    // ① 过账(补 postBill 生产触发缺口):对未越池、已 confirmed 的常规单自动挂应付台账;
                    //   超池 draft 单不 confirmed → 不过账,待人工特批 confirmed 后走网关端点 ②(POST /bills/{id}/post)。
                    //   postBill @Transactional 全或无 + 幂等,confirmed 单重跑不重复挂账。
                    //   🔴 过账独立闸(与作业运行解耦):过账仅在 settlement.monthly.posting.enabled=true(默认 false·不进 application-prod.yml)时执行。
                    //   月度作业开关 settlement.monthly.scheduled.enabled 在 prod 本就 =true(算账单),但过账保持关——settleMonth 照常算
                    //   confirmed 单(内部计算不落账本),直到用户拍板翻 posting.enabled 才首次真过账。网关 POST /bills/{id}/post 人工特批路径不受此闸。
                    if (postingEnabled && "confirmed".equals(repository.findBillStatus(billId))) {
                        postingService.postBill(billId);
                    }
                }
                // 复核 money I7：演出结算出账即挂应付（真过账），同受过账独立闸 settlement.monthly.posting.enabled 约束。
                if (postingEnabled && mediaSettlement != null) {
                    mediaSettlement.settleConfirmedForMonth(month);
                }
            }
        } finally {
            if (previousAuthentication == null) {
                SecurityContextHolder.clearContext();
            } else {
                SecurityContextHolder.setContext(previousContext);
            }
        }
    }
}

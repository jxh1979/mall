package com.complex.platform.customer;

// 文件版本：V0.04

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.customer.TicketPointsBilling.*;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketPointsBillingService {
    public static final String VIEW="ticket.points.billing.view", MANAGE="ticket.points.billing.manage";
    private final JdbcTemplate jdbc;
    private final AuthorizationService auth;
    private final SupervisorReviewService reviews;
    private final IdempotencyService idem;
    private final AuditEventService audit;
    private final ObjectMapper json;
    private final FinanceService finance;
    private final com.complex.platform.notification.NotificationService notifications;
    private final com.complex.platform.merchantbilling.MerchantFeeNotificationRecipientResolver notificationRecipients;
    // FS-021：已审批日历与整日故障判定抽到 finance.WorkingDayCalendar，与普通内部结算共用。
    private final com.complex.platform.finance.WorkingDayCalendar calendar;
    public TicketPointsBillingService(JdbcTemplate jdbc,AuthorizationService auth,SupervisorReviewService reviews,
            IdempotencyService idem,AuditEventService audit,ObjectMapper json,FinanceService finance,
            com.complex.platform.notification.NotificationService notifications,
            com.complex.platform.merchantbilling.MerchantFeeNotificationRecipientResolver notificationRecipients,
            com.complex.platform.finance.WorkingDayCalendar calendar) {
        this.jdbc=jdbc;this.auth=auth;this.reviews=reviews;this.idem=idem;this.audit=audit;this.json=json;this.finance=finance;
        this.notifications=notifications;this.notificationRecipients=notificationRecipients;this.calendar=calendar;
    }

    public List<Rule> rules(){national(VIEW);return jdbc.query("SELECT * FROM ticket_points_billing_rules ORDER BY effective_month DESC",this::rule);}
    public List<Map<String,Object>> calendarCases(){national(VIEW);return jdbc.queryForList("SELECT id,days::text AS days,source_note,status,review_id,created_at FROM ticket_points_calendar_cases ORDER BY created_at DESC");}
    public List<Map<String,Object>> outageCases(){national(VIEW);return jdbc.queryForList("SELECT id,project_id,unavailable_from,unavailable_to,reason,status,review_id FROM ticket_points_portal_outages ORDER BY created_at DESC");}
    @Transactional public Rule submitRule(RuleRequest r) {
        national(MANAGE); nationalApprover(r.finalApproverId());
        LocalDate currentMonth=LocalDate.now(LocalZone.ID).withDayOfMonth(1);
        boolean initial=jdbc.queryForObject("SELECT count(*) FROM ticket_points_billing_rules WHERE status<>'rejected'",Integer.class)==0;
        if(r.effectiveMonth()==null || r.effectiveMonth().getDayOfMonth()!=1 || r.effectiveMonth().isBefore(currentMonth) || (!initial && !r.effectiveMonth().isAfter(currentMonth)))throw invalid("初始规则不得追溯历史账期；后续变更只可从未来自然月第一天生效");
        if(r.statementDay()<1 || r.statementDay()>31 || r.minimumDays()<0 || r.objectionWorkingDays()<r.minimumDays() || r.objectionWorkingDays()>r.maximumDays())throw invalid("出账日及异议工作日上下限不合法");
        requireText(r.reason(),"reason");
        return idem.execute("ticketPointsBillingRule",requireText(r.idempotencyKey(),"idempotency_key"),r,()->{
            jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended('ticket-points-billing-rule',0))",rs->{});
            if(jdbc.queryForObject("SELECT count(*) FROM ticket_points_billing_rules WHERE effective_month=? AND status<>'rejected'",Integer.class,r.effectiveMonth())>0)throw invalid("该生效月份已有待审或生效版本");
            List<Integer> payDays=jdbc.query("SELECT day_1,day_2,day_3 FROM internal_settlement_pay_days WHERE scope_id='global'",(rs,n)->List.of(rs.getInt(1),rs.getInt(2),rs.getInt(3))).stream().findFirst().orElseThrow(()->invalid("须先配置全平台统一付账日"));
            int seq=jdbc.queryForObject("SELECT count(*)+1 FROM ticket_points_billing_rules",Integer.class);
            String id=id("tpbr");
            // 配置保存即核算未来账期顺序；缺少真实工作日历不能猜周末或银行营业状态。
            LocalDate visible=statementDate(r.effectiveMonth(),r.statementDay());
            LocalDate deadline=deadline(visible,r.objectionWorkingDays(),null);
            LocalDate due=due(deadline,payDays);
            if(YearMonth.from(due).isAfter(YearMonth.from(visible)))throw invalid("当期三个付账日均不能满足出账、异议期与银行工作日顺序，请调整配置");
            jdbc.update("INSERT INTO ticket_points_billing_rules(id,version,effective_month,statement_day,objection_working_days,minimum_days,maximum_days,pay_days,status,created_by,created_at) VALUES(?,?,?,?,?,?,?,?::integer[],'pending_review',?,?)",id,"V"+(seq/100)+"."+String.format("%02d",seq%100),r.effectiveMonth(),r.statementDay(),r.objectionWorkingDays(),r.minimumDays(),r.maximumDays(),"{"+payDays.get(0)+","+payDays.get(1)+","+payDays.get(2)+"}",auth.currentActor().id(),Timestamp.from(Instant.now()));
            String review=openReview(r.idempotencyKey(),"ticket_points_billing_rule",id,r.reason(),r.finalApproverId());
            jdbc.update("UPDATE ticket_points_billing_rules SET review_id=? WHERE id=?",review,id);
            return jdbc.query("SELECT * FROM ticket_points_billing_rules WHERE id=?",this::rule,id).getFirst();
        },Rule.class);
    }

    @Transactional public CaseReceipt submitCalendar(CalendarRequest r) {
        national(MANAGE);nationalApprover(r.finalApproverId());requireText(r.sourceNote(),"source_note");
        if(r.days()==null || r.days().isEmpty() || r.days().stream().anyMatch(d->d.date()==null) || r.days().stream().map(Day::date).distinct().count()!=r.days().size())throw invalid("日历须逐日明确工作日和银行工作日，日期不得重复");
        return idem.execute("ticketPointsCalendar",requireText(r.idempotencyKey(),"idempotency_key"),r,()->{
            String id=id("tpcc");jdbc.update("INSERT INTO ticket_points_calendar_cases(id,days,source_note,status,created_by,created_at) VALUES(?,?::jsonb,?,'pending_review',?,?)",id,encode(r.days()),r.sourceNote(),auth.currentActor().id(),Timestamp.from(Instant.now()));
            String review=openReview(r.idempotencyKey(),"ticket_points_calendar",id,r.sourceNote(),r.finalApproverId());jdbc.update("UPDATE ticket_points_calendar_cases SET review_id=? WHERE id=?",review,id);
            return new CaseReceipt(id,"pending_review",review);
        },CaseReceipt.class);
    }

    @Transactional public CaseReceipt submitOutage(OutageRequest r) {
        national(MANAGE);nationalApprover(r.finalApproverId());requireText(r.reason(),"reason");
        if(r.unavailableFrom()==null || r.unavailableTo()==null || !r.unavailableTo().isAfter(r.unavailableFrom()) || r.unavailableTo().isAfter(Instant.now()))throw invalid("须记录已实际发生且结束的门户不可用区间");
        if(jdbc.queryForObject("SELECT count(*) FROM projects WHERE id=? AND status='active'",Integer.class,r.projectId())!=1)throw invalid("项目不存在或未生效");
        return idem.execute("ticketPointsPortalOutage",requireText(r.idempotencyKey(),"idempotency_key"),r,()->{
            String id=id("tppo");jdbc.update("INSERT INTO ticket_points_portal_outages(id,project_id,unavailable_from,unavailable_to,reason,status,created_by,created_at) VALUES(?,?,?,?,?,'pending_review',?,?)",id,r.projectId(),Timestamp.from(r.unavailableFrom()),Timestamp.from(r.unavailableTo()),r.reason(),auth.currentActor().id(),Timestamp.from(Instant.now()));
            String review=openReview(r.idempotencyKey(),"ticket_points_portal_outage",id,r.reason(),r.finalApproverId());jdbc.update("UPDATE ticket_points_portal_outages SET review_id=? WHERE id=?",review,id);
            return new CaseReceipt(id,"pending_review",review);
        },CaseReceipt.class);
    }

    @EventListener @Transactional public void decided(SupervisorReviewDecisionRecord e) {
        if(!"ticket.reward.national".equals(e.actionType()) || !Set.of("approved","rejected").contains(e.reviewStatus()))return;
        if("ticket_points_objection_resolution".equals(e.objectType())) {
            if("approved".equals(e.reviewStatus()))jdbc.update("UPDATE ticket_points_bill_objections SET status='resolved',resolved_by=?,resolved_at=? WHERE id=? AND review_id=? AND status='open'",auth.currentActor().id(),Timestamp.from(Instant.now()),e.objectId(),e.reviewId());
            else jdbc.update("UPDATE ticket_points_bill_objections SET review_id=NULL,resolution=NULL WHERE id=? AND review_id=? AND status='open'",e.objectId(),e.reviewId());
            return;
        }
        String table=switch(e.objectType()) {case "ticket_points_billing_rule"->"ticket_points_billing_rules";case "ticket_points_calendar"->"ticket_points_calendar_cases";case "ticket_points_portal_outage"->"ticket_points_portal_outages";default->null;};
        if(table==null)return;
        if(jdbc.queryForList("SELECT id FROM "+table+" WHERE id=? AND review_id=? AND status='pending_review' FOR UPDATE",String.class,e.objectId(),e.reviewId()).isEmpty())return;
        if("approved".equals(e.reviewStatus()) && "ticket_points_billing_rule".equals(e.objectType())) {
            LocalDate month=jdbc.queryForObject("SELECT effective_month FROM ticket_points_billing_rules WHERE id=?",LocalDate.class,e.objectId());
            if(month.isBefore(LocalDate.now(LocalZone.ID).withDayOfMonth(1)))throw invalid("审批已跨过配置生效账期，请重新申请未来账期版本");
        }
        if("approved".equals(e.reviewStatus()) && "ticket_points_calendar".equals(e.objectType())) {
            List<Day> days=decodeDays(jdbc.queryForObject("SELECT days::text FROM ticket_points_calendar_cases WHERE id=?",String.class,e.objectId()));
            for(Day day:days) {
                // 已公布账单及获批延期使用过的日历不回改，防止倒改异议期或到期日。
                if(jdbc.queryForObject("SELECT count(*) FROM ticket_points_monthly_bills b WHERE b.visible_at IS NOT NULL AND (b.visible_at AT TIME ZONE ?)::date<=? AND (b.due_date IS NULL OR GREATEST(b.due_date,COALESCE((SELECT max(s.due_date) FROM ticket_points_bill_schedule_adjustments s WHERE s.bill_id=b.id),b.due_date))>=?)",Integer.class,LocalZone.ID.getId(),day.date(),day.date())>0
                        && jdbc.queryForObject("SELECT count(*) FROM ticket_points_calendar_days WHERE calendar_date=? AND (working_day<>? OR bank_working_day<>?)",Integer.class,day.date(),day.workingDay(),day.bankWorkingDay())>0)throw invalid("该日历已用于公布账单，不可回改");
                jdbc.update("INSERT INTO ticket_points_calendar_days(calendar_date,working_day,bank_working_day,case_id) VALUES(?,?,?,?) ON CONFLICT(calendar_date) DO UPDATE SET working_day=EXCLUDED.working_day,bank_working_day=EXCLUDED.bank_working_day,case_id=EXCLUDED.case_id",day.date(),day.workingDay(),day.bankWorkingDay(),e.objectId());
            }
        }
        jdbc.update("UPDATE "+table+" SET status=?,approved_at=? WHERE id=?",e.reviewStatus(),Timestamp.from(Instant.now()),e.objectId());
        if("approved".equals(e.reviewStatus()) && "ticket_points_portal_outage".equals(e.objectType()))extendUnpaidSchedules(e.objectId());
    }

    /** 费用类别 → 费用事实表、账单明细表（常量表名）。 */
    private static final Map<String,String[]> FEE_TABLES=Map.of(
            "ticket",new String[]{"ticket_points_issuance_fees","ticket_points_monthly_bill_items"},
            "merchant",new String[]{"merchant_points_issuance_fees","merchant_points_monthly_bill_items"});

    /**
     * 定时入口：仅已结束自然月、已到配置出账日的费用事实归集出账。第十一批 TK-04-b：门票积分发行费与商户积分发行费
     * （BSM L823「只有转为可用才产生商户积分发行费」）各自归集、各自出账（ENUM L84「各自全额挂账」），同一月同一批关账。
     */
    @Transactional public int closeReadyMonths(Instant now) {
        LocalDate today=now.atZone(LocalZone.ID).toLocalDate();
        Timestamp currentMonth=Timestamp.from(today.withDayOfMonth(1).atStartOfDay(LocalZone.ID).toInstant());
        List<LocalDate> months=jdbc.queryForList("SELECT DISTINCT m FROM (SELECT date_trunc('month',f.confirmed_at AT TIME ZONE ?)::date AS m FROM ticket_points_issuance_fees f LEFT JOIN ticket_points_monthly_bill_items i ON i.reward_id=f.reward_id WHERE i.reward_id IS NULL AND f.confirmed_at<? UNION SELECT date_trunc('month',f.confirmed_at AT TIME ZONE ?)::date FROM merchant_points_issuance_fees f LEFT JOIN merchant_points_monthly_bill_items i ON i.reward_id=f.reward_id WHERE i.reward_id IS NULL AND f.confirmed_at<?) x ORDER BY 1",LocalDate.class,LocalZone.ID.getId(),currentMonth,LocalZone.ID.getId(),currentMonth);
        int count=0;
        for(LocalDate month:months){
            Rule rule=effective(month);
            if(today.isBefore(statementDate(month,rule.statementDay())))continue;
            jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",rs->{},"points-month:"+month);
            if(jdbc.queryForObject("SELECT count(*) FROM ticket_points_closed_months WHERE billing_month=?",Integer.class,month)>0)continue;
            record Fee(String reward,String seller,String merchant,String receiver,String project,long amount){}
            for(String kind:List.of("ticket","merchant")){
                String[] tables=FEE_TABLES.get(kind);
                List<Fee> fees=jdbc.query("SELECT f.* FROM "+tables[0]+" f LEFT JOIN "+tables[1]+" i ON i.reward_id=f.reward_id WHERE i.reward_id IS NULL AND f.confirmed_at>=? AND f.confirmed_at<? ORDER BY f.reward_id",(rs,n)->new Fee(rs.getString("reward_id"),rs.getString("seller_entity_id"),rs.getString("seller_merchant_id"),rs.getString("receiver_entity_id"),rs.getString("project_id"),rs.getLong("amount_cent")),Timestamp.from(month.atStartOfDay(LocalZone.ID).toInstant()),Timestamp.from(month.plusMonths(1).atStartOfDay(LocalZone.ID).toInstant()));
                Map<List<String>,List<Fee>> groups=new LinkedHashMap<>();
                for(Fee f:fees)groups.computeIfAbsent(Arrays.asList(f.seller(),f.merchant(),f.receiver(),f.project()),k->new ArrayList<>()).add(f);
                for(List<Fee> group:groups.values()){
                    Fee f=group.getFirst();String id=id("tpmb");long amount=group.stream().mapToLong(Fee::amount).reduce(0,Math::addExact);
                    jdbc.update("INSERT INTO ticket_points_monthly_bills(id,billing_month,billing_rule_id,seller_entity_id,seller_merchant_id,receiver_entity_id,project_id,amount_cent,created_at,fee_kind) VALUES(?,?,?,?,?,?,?,?,?,?)",id,month,rule.id(),f.seller(),f.merchant(),f.receiver(),f.project(),amount,Timestamp.from(now),kind);
                    for(Fee fee:group)jdbc.update("INSERT INTO "+tables[1]+"(reward_id,bill_id) VALUES(?,?)",fee.reward(),id);
                    publishIfReachable(find(id),rule,now);count++;
                }
            }
            jdbc.update("INSERT INTO ticket_points_closed_months(billing_month,closed_at) VALUES(?,?)",month,Timestamp.from(now));
        }
        return count;
    }

    @Transactional public boolean finalizeDue(String id,Instant now) {
        jdbc.queryForList("SELECT id FROM ticket_points_monthly_bills WHERE id=? FOR UPDATE",String.class,id);
        Bill bill=find(id);if(bill.arApItemId()!=null || bill.openObjections()>0)return false;
        Rule rule=jdbc.query("SELECT * FROM ticket_points_billing_rules WHERE id=?",this::rule,bill.billingRuleId()).getFirst();
        if(bill.visibleAt()==null){if(!publishIfReachable(bill,rule,now))return false;bill=find(id);}
        if(bill.amountCent()==0)return false;
        LocalDate deadline=deadline(bill.visibleAt().atZone(LocalZone.ID).toLocalDate(),rule.objectionWorkingDays(),bill.projectId());
        if(jdbc.queryForObject("SELECT count(*) FROM ticket_points_portal_outages WHERE project_id=? AND status='pending_review' AND unavailable_to>? AND unavailable_from<?",Integer.class,bill.projectId(),Timestamp.from(bill.visibleAt()),Timestamp.from(deadline.plusDays(1).atStartOfDay(LocalZone.ID).toInstant()))>0)return false;
        // 当天即配置付账日也可付；不把通知成功或商户首次登录当作起算事实。
        LocalDate due=due(deadline,rule.payDays());
        jdbc.update("UPDATE ticket_points_monthly_bills SET objection_deadline=?,due_date=? WHERE id=?",deadline,due,id);
        if(now.atZone(LocalZone.ID).toLocalDate().isBefore(deadline))return false;
        if(bill.amountCent()>0){String arap=finance.recordTicketPointsIssuanceBill(id);jdbc.update("UPDATE ticket_points_monthly_bills SET ar_ap_item_id=? WHERE id=?",arap,id);}
        return true;
    }

    public List<String> unfinished(){return jdbc.queryForList("SELECT id FROM ticket_points_monthly_bills WHERE ar_ap_item_id IS NULL AND (amount_cent>0 OR visible_at IS NULL) ORDER BY created_at,id",String.class);}

    /** 后核实的整日故障只延长未付款期限；原账单、原到期日和成功付款不回改。 */
    private void extendUnpaidSchedules(String outageId){
        String project=jdbc.queryForObject("SELECT project_id FROM ticket_points_portal_outages WHERE id=?",String.class,outageId);
        List<String> ids=jdbc.queryForList("SELECT b.id FROM ticket_points_monthly_bills b JOIN ar_ap_items a ON a.id=b.ar_ap_item_id WHERE b.project_id=? AND a.unpaid_amount_cent>0 ORDER BY a.id",String.class,project);
        for(String id:ids){
            // 与实际付款保持同一锁序：先往来台账，再来源账单。
            var payment=jdbc.queryForMap("SELECT a.id,a.unpaid_amount_cent FROM ar_ap_items a JOIN ticket_points_monthly_bills b ON b.ar_ap_item_id=a.id WHERE b.id=? FOR UPDATE OF a",id);
            long unpaid=((Number)payment.get("unpaid_amount_cent")).longValue();if(unpaid<=0)continue;
            jdbc.queryForList("SELECT id FROM ticket_points_monthly_bills WHERE id=? FOR UPDATE",String.class,id);
            Bill bill=find(id);Rule rule=jdbc.query("SELECT * FROM ticket_points_billing_rules WHERE id=?",this::rule,bill.billingRuleId()).getFirst();
            LocalDate deadline=deadline(bill.visibleAt().atZone(LocalZone.ID).toLocalDate(),rule.objectionWorkingDays(),project);
            if(!deadline.isAfter(bill.objectionDeadline()))continue;
            LocalDate due=due(deadline,rule.payDays());String adjustment=id("tpba");Instant now=Instant.now();
            jdbc.update("INSERT INTO ticket_points_bill_schedule_adjustments(id,bill_id,outage_id,previous_objection_deadline,previous_due_date,objection_deadline,due_date,unpaid_amount_cent,created_at) VALUES(?,?,?,?,?,?,?,?,?)",adjustment,id,outageId,bill.objectionDeadline(),bill.dueDate(),deadline,due,unpaid,Timestamp.from(now));
            jdbc.update("UPDATE ar_ap_items SET due_date=?,updated_at=? WHERE id=?",due,Timestamp.from(now),payment.get("id"));
            audit.recordSuccess("ticket.points.bill.schedule.extended",MANAGE,"ticket_points_bill",id,null,outageId,Map.of("adjustment_id",adjustment,"previous_due_date",bill.dueDate().toString(),"due_date",due.toString(),"unpaid_amount_cent",unpaid));
        }
    }

    public List<Map<String,Object>> scheduleAdjustments(String id){requireReadable(find(id));return jdbc.queryForList("SELECT id,outage_id,previous_objection_deadline,previous_due_date,objection_deadline,due_date,unpaid_amount_cent,created_at FROM ticket_points_bill_schedule_adjustments WHERE bill_id=? ORDER BY created_at,id",id);}

    /** 出账事务完成后的定时通知，复用真实发送日志；提醒结果不参与异议期起算。 */
    public void notifyPublishedBills(){
        record Notice(String id,String bill,String merchant,LocalDate month,long amount,LocalDate deadline,String reason){}
        List<Notice> notices=jdbc.query("SELECT b.id AS notice_id,b.id,b.seller_merchant_id,b.billing_month,b.amount_cent,b.objection_deadline,CASE b.fee_kind WHEN 'merchant' THEN '商户积分发行费月结出账' ELSE '门票积分发行费月结出账' END AS reason FROM ticket_points_monthly_bills b WHERE b.visible_at IS NOT NULL AND b.seller_merchant_id IS NOT NULL UNION ALL SELECT s.id,b.id,b.seller_merchant_id,b.billing_month,b.amount_cent,s.objection_deadline,'核实整日门户故障，未付款期限延长' FROM ticket_points_bill_schedule_adjustments s JOIN ticket_points_monthly_bills b ON b.id=s.bill_id WHERE b.seller_merchant_id IS NOT NULL",(r,n)->new Notice(r.getString("notice_id"),r.getString("id"),r.getString("seller_merchant_id"),r.getObject("billing_month",LocalDate.class),r.getLong("amount_cent"),r.getObject("objection_deadline",LocalDate.class),r.getString("reason")));
        for(Notice n:notices){
            String request="ticket-points-bill:"+n.id();
            if(jdbc.queryForObject("SELECT count(*) FROM notification_send_log WHERE request_id=?",Integer.class,request)>0)continue;
            String phone=notificationRecipients.resolvePhone(n.merchant(),null);
            if(phone==null){audit.recordFailure("ticket.points.bill.notification.not_sent",MANAGE,"ticket_points_bill",n.bill(),request,request,"缺少有效通知联系人，未发送；账单可查看时间和异议期不变");continue;}
            notifications.notify("points","bill",phone,"POINTS_BILL_PUBLISHED",Map.of("bill_id",n.bill(),"billing_month",n.month().toString(),"amount_cent",n.amount(),"reason",n.reason(),"objection_deadline",n.deadline().toString()),request);
        }
    }

    private boolean publishIfReachable(Bill bill,Rule rule,Instant now){
        // 只有承担方实际具备页面查看权限，才形成“商户专区可查看”事实；首次登录不参与计时。
        List<com.complex.platform.access.AccessActor> recipients=jdbc.query("SELECT * FROM access_users WHERE status='active' AND ((?::text IS NOT NULL AND merchant_id=?) OR (?::text IS NULL AND organization_id=?))",(r,n)->new com.complex.platform.access.AccessActor(r.getString("id"),r.getString("username"),r.getString("account_type"),r.getString("organization_id"),r.getString("merchant_id"),r.getString("status")),bill.sellerMerchantId(),bill.sellerMerchantId(),bill.sellerMerchantId(),bill.sellerEntityId());
        // 第十八批 E 段组间遗漏（第 20 条连带；治理地图第 659 行「看到的是每一个项目内他经营的商户」，同 readable）：账号商户即本账单卖方商户的视为可达——
        // 商户账号都归属主项目，否则本商户在经营项目的账单永远停在待公布；其余照旧按项目覆盖。
        if(recipients.stream().noneMatch(actor->auth.hasEffectivePermission(actor,VIEW)&&((bill.sellerMerchantId()!=null&&bill.sellerMerchantId().equals(actor.merchantId()))||auth.projectReach(actor,VIEW).covers(bill.projectId()))))return false;
        LocalDate deadline=deadline(now.atZone(LocalZone.ID).toLocalDate(),rule.objectionWorkingDays(),bill.projectId());
        jdbc.update("UPDATE ticket_points_monthly_bills SET visible_at=?,objection_deadline=?,due_date=? WHERE id=? AND visible_at IS NULL",Timestamp.from(now),deadline,due(deadline,rule.payDays()),bill.id());
        audit.recordSuccess("ticket.points.bill.published",MANAGE,"ticket_points_bill",bill.id(),null,bill.id(),Map.of("billing_month",bill.billingMonth().toString(),"amount_cent",bill.amountCent(),"receiver_entity_id",bill.receiverEntityId(),"project_id",bill.projectId()));
        return true;
    }
    public List<Map<String,Object>> objections(String id){requireReadable(find(id));return jdbc.queryForList("SELECT id,reason,submitted_at,status,resolution,resolved_at FROM ticket_points_bill_objections WHERE bill_id=? ORDER BY submitted_at",id);}
    /** 账单明细：门票行取原票／人份／次份，商户消费行取来源消费单据（ENUM L77 从单据可反查）。 */
    public List<FeeLine> items(String id){requireReadable(find(id));return jdbc.query("""
            SELECT f.*,r.parent_ticket_id,r.slot_index,r.use_index,'ticket_use' AS source_type,r.parent_ticket_id AS source_id
              FROM ticket_points_monthly_bill_items i JOIN ticket_points_issuance_fees f ON f.reward_id=i.reward_id JOIN ticket_use_rewards r ON r.id=f.reward_id WHERE i.bill_id=?
            UNION ALL
            SELECT f.reward_id,f.price_version_id,f.seller_entity_id,f.seller_merchant_id,f.receiver_entity_id,f.project_id,f.points,f.unit_price_cent,f.amount_cent,f.confirmed_at,
                   NULL,0,0,r.source_type,r.source_id
              FROM merchant_points_monthly_bill_items i JOIN merchant_points_issuance_fees f ON f.reward_id=i.reward_id JOIN merchant_consumption_rewards r ON r.id=f.reward_id WHERE i.bill_id=?
            ORDER BY confirmed_at,reward_id
            """,(rs,n)->new FeeLine(rs.getString("reward_id"),rs.getString("parent_ticket_id"),rs.getInt("slot_index"),rs.getInt("use_index"),rs.getLong("points"),rs.getBigDecimal("unit_price_cent"),rs.getLong("amount_cent"),rs.getString("price_version_id"),rs.getTimestamp("confirmed_at").toInstant(),rs.getString("source_type"),rs.getString("source_id")),id,id);}
    @Transactional public CaseReceipt resolveUnchanged(String objectionId,ResolutionRequest r) {
        national(MANAGE);nationalApprover(r.finalApproverId());requireText(r.resolution(),"resolution");
        return idem.execute("ticketPointsObjectionResolution:"+objectionId,requireText(r.idempotencyKey(),"idempotency_key"),r,()->{
            if(jdbc.queryForList("SELECT id FROM ticket_points_bill_objections WHERE id=? AND status='open' AND review_id IS NULL FOR UPDATE",String.class,objectionId).isEmpty())throw invalid("异议不存在、已处理或正在复核");
            String review=openReview(r.idempotencyKey(),"ticket_points_objection_resolution",objectionId,"确认原金额无误，复核依据："+r.resolution(),r.finalApproverId());
            jdbc.update("UPDATE ticket_points_bill_objections SET resolution=?,review_id=? WHERE id=?",r.resolution(),review,objectionId);
            return new CaseReceipt(objectionId,"open",review);
        },CaseReceipt.class);
    }
    public List<Bill> list(String project){auth.requirePermission(VIEW);if(project!=null&&auth.currentMerchantIdQuietly().isEmpty()&&!auth.projectReach(auth.currentActor(),VIEW).covers(project))throw forbidden();
        return jdbc.query(SELECT+" WHERE (?::text IS NULL OR b.project_id=?) ORDER BY b.billing_month DESC,b.id",this::bill,project,project).stream().filter(this::readable).toList();}
    @Transactional public CaseReceipt object(String id,ObjectionRequest r){Bill b=find(id);requireVisible(b);requireText(r.reason(),"reason");if(b.visibleAt()==null)throw invalid("该账单尚未公布，异议期尚未开始");
        return idem.execute("ticketPointsObjection:"+id,requireText(r.idempotencyKey(),"idempotency_key"),r,()->{
            jdbc.queryForList("SELECT id FROM ticket_points_monthly_bills WHERE id=? FOR UPDATE",String.class,id);
            Rule rule=jdbc.query("SELECT * FROM ticket_points_billing_rules WHERE id=?",this::rule,b.billingRuleId()).getFirst();
            LocalDate deadline=deadline(b.visibleAt().atZone(LocalZone.ID).toLocalDate(),rule.objectionWorkingDays(),b.projectId());
            if(LocalDate.now(LocalZone.ID).isAfter(deadline))throw invalid("该账单异议期已届满");
            String key=id("tpbo");jdbc.update("INSERT INTO ticket_points_bill_objections(id,bill_id,reason,submitted_by,submitted_at,status) VALUES(?,?,?,?,?,'open')",key,id,r.reason(),auth.currentActor().id(),Timestamp.from(Instant.now()));
            return new CaseReceipt(key,"open",null);
        },CaseReceipt.class);
    }

    LocalDate deadline(LocalDate visible,int days,String project){return calendar.objectionDeadline(visible,days,project);}
    LocalDate due(LocalDate deadline,List<Integer> payDays){LocalDate date=deadline;while(!payDays.contains(date.getDayOfMonth()))date=date.plusDays(1);return calendar.bankDayOnOrAfter(date);}
    private Rule effective(LocalDate month){return jdbc.query("SELECT * FROM ticket_points_billing_rules WHERE status='approved' AND effective_month<=? ORDER BY effective_month DESC LIMIT 1",this::rule,month).stream().findFirst().orElseThrow(()->invalid("缺少该账期已审批的全国积分账期版本"));}
    private boolean visible(Bill b){if(!auth.projectReach(auth.currentActor(),VIEW).covers(b.projectId()))return false;String m=auth.currentActor().merchantId();if(m!=null)return m.equals(b.sellerMerchantId());String org=auth.currentActor().organizationId();return b.sellerEntityId().equals(org)||b.receiverEntityId().equals(org)||auth.allowsPlatformScope(auth.currentActor(),VIEW);}
    private void requireVisible(Bill b){auth.requirePermission(VIEW);if(!visible(b))throw forbidden();}
    // 第十八批 E 段 20-R4（治理地图第 659 行「一个商户在两个或更多项目中有经营的，看到的是每一个项目内他经营的商户」）：只用于读——
    // 商户账号（非平台管理员）看本商户为卖方的账单不再限本人项目；其余同 visible。提异议（object）仍走 requireVisible。
    private boolean readable(Bill b){String m=auth.currentMerchantIdQuietly().orElse(null);return m!=null?m.equals(b.sellerMerchantId()):visible(b);}
    private void requireReadable(Bill b){auth.requirePermission(VIEW);if(!readable(b))throw forbidden();}
    private void national(String permission){auth.requirePermission(permission);if(!auth.allowsPlatformScope(auth.currentActor(),permission))throw forbidden();}
    private void nationalApprover(String user){if(jdbc.queryForObject("SELECT count(*) FROM access_users u JOIN business_entities e ON e.id=u.organization_id WHERE u.id=? AND u.status='active' AND e.status='active' AND e.entity_role='platform'",Integer.class,requireText(user,"final_approver_id"))!=1)throw invalid("全国账期、日历和故障事实须由全国获授权平台主管审批");}
    private String openReview(String key,String type,String id,String reason,String approver){var r=reviews.evaluate(new SupervisorReviewEvaluateRequest(key+":review",reason,List.of(),"ticket.reward.national",type,id,0L,reason,approver,List.of()));if(!"pending_review".equals(r.reviewStatus()))throw invalid("全国积分事项须经过审批");return r.reviewId();}
    private static final String SELECT="SELECT b.*,COALESCE(s.objection_deadline,b.objection_deadline) AS effective_objection_deadline,COALESCE(s.due_date,b.due_date) AS effective_due_date,COALESCE(a.paid_amount_cent,0) AS paid,COALESCE(a.unpaid_amount_cent,b.amount_cent) AS unpaid,CASE WHEN b.visible_at IS NULL THEN 'awaiting_publication' WHEN b.amount_cent=0 THEN 'settled' ELSE COALESCE(a.status,'awaiting_objection_period') END AS payment_status,(SELECT count(*) FROM ticket_points_bill_objections o WHERE o.bill_id=b.id AND o.status='open') AS open_objections FROM ticket_points_monthly_bills b LEFT JOIN ar_ap_items a ON a.id=b.ar_ap_item_id LEFT JOIN LATERAL (SELECT objection_deadline,due_date FROM ticket_points_bill_schedule_adjustments WHERE bill_id=b.id ORDER BY created_at DESC,id DESC LIMIT 1) s ON true";
    // 沿用现 StaffService 的结算日期处理：配置日超出当月长度时取该月月末。
    private LocalDate statementDate(LocalDate month,int day){LocalDate next=month.plusMonths(1);return next.withDayOfMonth(Math.min(day,next.lengthOfMonth()));}
    private Bill find(String id){return jdbc.query(SELECT+" WHERE b.id=?",this::bill,id).stream().findFirst().orElseThrow(()->ApiException.notFound("POINTS_BILL_NOT_FOUND","积分账单不存在"));}
    private Bill bill(ResultSet r,int n)throws SQLException{return new Bill(r.getString("id"),r.getObject("billing_month",LocalDate.class),r.getString("billing_rule_id"),r.getString("seller_entity_id"),r.getString("seller_merchant_id"),r.getString("receiver_entity_id"),r.getString("project_id"),r.getLong("amount_cent"),TicketRewardConfigService.instant(r,"visible_at"),r.getObject("effective_objection_deadline",LocalDate.class),r.getObject("effective_due_date",LocalDate.class),r.getString("ar_ap_item_id"),r.getLong("paid"),r.getLong("unpaid"),r.getString("payment_status"),r.getInt("open_objections"),r.getString("fee_kind"));}
    private Rule rule(ResultSet r,int n)throws SQLException{return new Rule(r.getString("id"),r.getString("version"),r.getObject("effective_month",LocalDate.class),r.getInt("statement_day"),r.getInt("objection_working_days"),r.getInt("minimum_days"),r.getInt("maximum_days"),Arrays.asList((Integer[])r.getArray("pay_days").getArray()),r.getString("status"),r.getString("review_id"));}
    private String encode(Object o){try{return json.writeValueAsString(o);}catch(Exception e){throw new IllegalStateException(e);}}
    private List<Day> decodeDays(String raw){try{return Arrays.asList(json.readValue(raw,Day[].class));}catch(Exception e){throw new IllegalStateException(e);}}
    private static String id(String prefix){return prefix+"_"+UUID.randomUUID().toString().replace("-","");}
    private static ApiException invalid(String message){return ApiException.badRequest("POINTS_BILLING_INVALID",message);}
    private static ApiException forbidden(){return ApiException.forbidden("FORBIDDEN","无权访问该积分结算事项");}
}

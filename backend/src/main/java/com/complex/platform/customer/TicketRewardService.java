package com.complex.platform.customer;

// 文件版本：V0.02

import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.ticketing.TicketRewardFacts;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketRewardService {
    private final JdbcTemplate jdbc;
    private final CustomerRepository customers;
    private final TicketRewardConfigService configs;
    private final PointsRecoveryService recoveries;
    private final ApplicationEventPublisher events;
    private final AuditEventService audit;
    public TicketRewardService(JdbcTemplate jdbc,CustomerRepository customers,TicketRewardConfigService configs,
            PointsRecoveryService recoveries,ApplicationEventPublisher events,AuditEventService audit){
        this.jdbc=jdbc;this.customers=customers;this.configs=configs;this.recoveries=recoveries;this.events=events;this.audit=audit;
    }

    @EventListener
    @Transactional
    public void orderCreated(TicketRewardFacts.OrderCreated event){
        record Order(String type,String project,Instant created){}
        Order order=jdbc.query("SELECT ticket_type_id,project_id,created_at FROM ticket_orders WHERE id=? FOR UPDATE",(rs,n)->new Order(rs.getString(1),rs.getString(2),rs.getTimestamp(3).toInstant()),event.orderId()).getFirst();
        if(order.project()==null) throw ApiException.conflict("TICKET_PROJECT_REQUIRED","销售订单须锁定真实项目");
        TicketRewardConfig c=configs.effective("sales_contract",order.type(),order.project(),order.created());
        jdbc.update("INSERT INTO ticket_order_sales_contracts(order_id,contract_version_id,project_id,seller_entity_id,seller_merchant_id,created_at) VALUES(?,?,?,?,?,?) ON CONFLICT(order_id) DO NOTHING",event.orderId(),c.id(),order.project(),c.values().sellerEntityId(),c.values().sellerMerchantId(),Timestamp.from(order.created()));
    }

    @EventListener
    @Transactional
    public void presented(TicketRewardFacts.CredentialPresented event){
        if(event.customerId()==null) return;
        jdbc.update("INSERT INTO ticket_credential_presenters(credential_id,customer_id,created_at) VALUES(?,?,now()) ON CONFLICT(credential_id) DO NOTHING",event.credentialId(),event.customerId());
    }

    @EventListener
    @Transactional
    public void used(TicketRewardFacts.Used event){
        if(event.credentialId()==null) return;
        record Use(String order,String parent,int slot,int index,String type,String project,String member,
                   String holder,String entitlementType,String compensation,Instant at){}
        List<Use> uses=jdbc.query("""
            SELECT e.order_id,e.parent_ticket_id,e.slot_index,e.used_times,e.ticket_type_id,o.project_id,
              cp.customer_id,h.holder_customer_id,e.entitlement_type,e.compensation_item_id,v.created_at
            FROM ticket_verifications v JOIN issued_entitlements e ON e.id=v.entitlement_id
              JOIN ticket_orders o ON o.id=e.order_id
              JOIN ticket_credentials c ON c.id=? AND c.entitlement_id=e.id
              JOIN ticket_credential_presenters cp ON cp.credential_id=c.id
              JOIN ticket_slot_holders h ON h.parent_ticket_id=e.parent_ticket_id AND h.slot_index=e.slot_index
            WHERE v.id=? AND v.status='success'
            """,(rs,n)->new Use(rs.getString(1),rs.getString(2),rs.getInt(3),rs.getInt(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8),rs.getString(9),rs.getString(10),rs.getTimestamp(11).toInstant()),event.credentialId(),event.verificationId());
        if(uses.isEmpty()) return;
        Use u=uses.getFirst();
        if(u.compensation()!=null || !(u.entitlementType().equals("ticket") || u.entitlementType().equals("entry") || u.entitlementType().equals("admission") || u.entitlementType().startsWith("amusement"))) return;
        if(!Objects.equals(u.member(),u.holder())) throw ApiException.forbidden("TICKET_REWARD_HOLDER_MISMATCH","核销会员与人份持有人不一致");
        lockParent(u.parent());
        if(jdbc.queryForObject("SELECT count(*) FROM ticket_reward_uses WHERE verification_id=?",Integer.class,event.verificationId())>0)return;
        List<String> existing=jdbc.queryForList("SELECT id FROM ticket_use_rewards WHERE parent_ticket_id=? AND slot_index=? AND use_index=? AND status IN ('pending','confirmed')",String.class,u.parent(),u.slot(),u.index());
        if(!existing.isEmpty()){
            jdbc.update("INSERT INTO ticket_reward_uses(verification_id,reward_id) VALUES(?,?)",event.verificationId(),existing.getFirst());return;
        }
        TicketRewardConfig points=configs.effective("points",u.type(),u.project(),u.at());
        TicketRewardConfig growth=configs.effective("growth",u.type(),u.project(),u.at());
        TicketRewardConfig policy=configs.effective("national",null,null,u.at());
        String id="tur_"+UUID.randomUUID().toString().replace("-","");
        int hours=policy.values().waitingHours();
        jdbc.update("""
            INSERT INTO ticket_use_rewards(id,order_id,parent_ticket_id,slot_index,use_index,verification_id,
            customer_id,project_id,points_rule_id,growth_rule_id,wait_rule_id,points,growth,waiting_hours,
            verified_at,available_at,status) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'pending')
            """,id,u.order(),u.parent(),u.slot(),u.index(),event.verificationId(),u.member(),u.project(),points.id(),growth.id(),policy.id(),points.values().fixedValue(),growth.values().fixedValue(),hours,Timestamp.from(u.at()),Timestamp.from(u.at().plusSeconds(Math.multiplyExact((long) hours,3600L))));
        jdbc.update("INSERT INTO ticket_reward_uses(verification_id,reward_id) VALUES(?,?)",event.verificationId(),id);
        CustomerPointsAccountRecord account=account(u.member(),u.at());
        customers.savePoints(balance(account,account.balance(),Math.addExact(account.keepThresholdProgress(),growth.values().fixedValue()),u.at()));
        jdbc.update("INSERT INTO customer_growth_ledger(id,customer_id,reward_id,change_type,growth_delta,created_at) VALUES(?,?,?,'earn',?,?)","growth_"+id,u.member(),id,growth.values().fixedValue(),Timestamp.from(u.at()));
        ledger(id,u.member(),points.values().fixedValue(),account.balance(),"earn","pending","核销门票待确认积分",u.at());
        audit.recordSuccess("ticket.reward.pending","not_applicable","ticket_use_reward",id,null,event.verificationId(),Map.of("customer_id",u.member(),"points",points.values().fixedValue(),"growth",growth.values().fixedValue(),"rule_id",points.id()));
    }

    public List<String> due(Instant at){return jdbc.queryForList("SELECT id FROM ticket_use_rewards WHERE status='pending' AND available_at<=? ORDER BY available_at,id",String.class,Timestamp.from(at));}

    @Transactional
    public boolean release(String id){return release(id,Instant.now());}

    /** 固定时点仅供同包确定性测试；生产确认不接受调用者回填时间。 */
    @Transactional
    boolean release(String id,Instant at){
        java.time.LocalDate feeMonth=at.atZone(com.complex.platform.common.LocalZone.ID).toLocalDate().withDayOfMonth(1);
        // 与月结使用同一锁：跨月并发时旧月已关闭就保留待确认，由下一轮按真实确认时间重试。
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",rs->{},"points-month:"+feeMonth);
        if(jdbc.queryForObject("SELECT count(*) FROM ticket_points_closed_months WHERE billing_month=?",Integer.class,feeMonth)>0)return false;
        TicketUseRewardRecord initial=find(id);lockParent(initial.parentTicketId());
        TicketUseRewardRecord r=locked(id);
        if(!"pending".equals(r.status()) || at.isBefore(r.availableAt()))return false;
        List<String> refunds=refundStatuses(r.parentTicketId(),r.slotIndex());
        if(refunds.contains("success")){reverse(r,"ticket_refund",at);return false;}
        if(refunds.stream().anyMatch(s->!s.equals("rejected")&&!s.equals("cancelled")&&!s.equals("withdrawn")))return false;
        CustomerPointsAccountRecord a=account(r.customerId(),at);
        if("frozen".equals(a.accountStatus()))return false;
        if(!customers.findCustomerById(r.customerId()).map(c->"active".equals(c.status())).orElse(false))return false;
        TicketRewardConfig price=configs.effective("national",null,null,at);
        long offset=recoveries.consume(r.customerId(),r.points(),a.balance(),r.id(),at);
        long net=r.points()-offset;
        long newBalance=Math.addExact(a.balance(),net);
        customers.savePoints(balance(a,newBalance,a.keepThresholdProgress(),at));
        ledger(id,r.customerId(),r.points(),newBalance,"confirm","confirmed","门票积分等待期届满，待追回抵扣："+offset,at);
        customers.updatePointsLedgerLifecycle("ledger_earn_"+id,"confirmed",null);
        jdbc.update("UPDATE ticket_use_rewards SET status='confirmed',confirmed_at=?,confirmed_points=? WHERE id=?",Timestamp.from(at),net,id);
        if(net>0){
            long fee=price.values().issuancePriceCent().multiply(BigDecimal.valueOf(net)).setScale(0,RoundingMode.HALF_UP).longValueExact();
            int count=jdbc.update("""
                INSERT INTO ticket_points_issuance_fees(reward_id,price_version_id,seller_entity_id,seller_merchant_id,
                  receiver_entity_id,project_id,points,unit_price_cent,amount_cent,confirmed_at)
                SELECT ?,?,seller_entity_id,seller_merchant_id,?,project_id,?,?,?,? FROM ticket_order_sales_contracts WHERE order_id=?
                """,id,price.id(),price.values().receiverEntityId(),net,price.values().issuancePriceCent(),fee,Timestamp.from(at),r.orderId());
            if(count!=1)throw ApiException.conflict("TICKET_SALES_CONTRACT_REQUIRED","原订单缺少销售合同快照，不可确认积分发行费用");
            events.publishEvent(new TicketRewardFacts.PointsConfirmed(id));
        }
        audit.recordSuccess("ticket.reward.confirmed","not_applicable","ticket_use_reward",id,null,id,Map.of("points",net,"recovery_offset",offset,"price_version_id",price.id()));
        return true;
    }

    @EventListener
    @Transactional
    public void refunded(TicketRewardFacts.Refunded e){
        if(!"success".equals(jdbc.queryForObject("SELECT status FROM ticket_refunds WHERE id=?",String.class,e.refundId()))) return;
        // TK-05-R2（2026-10-21 操作员裁决补充第 5 条）：多人通票安全例外只退出事一人份——只冲回所退人份的奖励，同票他人份不动。
        List<Map<String,Object>> shares=jdbc.queryForList("SELECT DISTINCT ie.parent_ticket_id,ie.slot_index FROM ticket_refund_lines l JOIN issued_entitlements ie ON ie.id=l.entitlement_id WHERE l.refund_id=? AND ie.parent_ticket_id IS NOT NULL ORDER BY ie.parent_ticket_id,ie.slot_index",e.refundId());
        for(Map<String,Object> share:shares){String parent=(String)share.get("parent_ticket_id");lockParent(parent);for(TicketUseRewardRecord r:jdbc.query("SELECT * FROM ticket_use_rewards WHERE parent_ticket_id=? AND slot_index=? AND status IN ('pending','confirmed') ORDER BY id FOR UPDATE",this::map,parent,share.get("slot_index"))) reverse(r,"ticket_refund:"+e.refundId(),Instant.now());}
    }

    @EventListener
    @Transactional
    public void reversed(TicketRewardFacts.Reversed e){
        List<String> ids=jdbc.queryForList("SELECT reward_id FROM ticket_reward_uses WHERE verification_id=?",String.class,e.verificationId());
        if(ids.isEmpty())return;
        TicketUseRewardRecord r=find(ids.getFirst());lockParent(r.parentTicketId());r=locked(r.id());
        int remaining=jdbc.queryForObject("SELECT count(*) FROM ticket_reward_uses u JOIN ticket_verifications v ON v.id=u.verification_id WHERE u.reward_id=? AND v.status='success'",Integer.class,r.id());
        if(remaining==0) reverse(r,"verification:"+e.verificationId(),Instant.now());
    }

    private void reverse(TicketUseRewardRecord r,String source,Instant at){
        if(!java.util.Set.of("pending","confirmed").contains(r.status()))return;
        CustomerPointsAccountRecord a=account(r.customerId(),at);
        boolean deregistered="deregistered".equals(customers.findCustomerById(r.customerId()).orElseThrow().status());
        long debit="confirmed".equals(r.status())?Math.min(a.balance(),r.points()):0;
        long debt="confirmed".equals(r.status())?r.points()-debit:0;
        long pendingDebt=deregistered?0:debt;
        long periodGrowth=a.levelPeriodStart()!=null && r.verifiedAt().isBefore(a.levelPeriodStart())?0:r.growth();
        customers.savePoints(balance(a,a.balance()-debit,Math.max(0,a.keepThresholdProgress()-periodGrowth),at));
        if("confirmed".equals(r.status()))ledger(r.id(),r.customerId(),-debit,a.balance()-debit,"deduct","reversed",deregistered?"门票奖励冲回；会员已注销，取消追偿："+debt:"门票奖励冲回；待追回："+pendingDebt,at);
        customers.updatePointsLedgerLifecycle("ledger_earn_"+r.id(),"reversed",null);
        jdbc.update("INSERT INTO customer_growth_ledger(id,customer_id,reward_id,change_type,growth_delta,created_at) VALUES(?,?,?,'reverse',?,?)","growth_reverse_"+r.id(),r.customerId(),r.id(),-r.growth(),Timestamp.from(at));
        if(debt>0)jdbc.update("INSERT INTO customer_points_recoveries(reward_id,customer_id,original_points,remaining_points,status,created_at,updated_at) VALUES(?,?,?,?,?,?,?)",r.id(),r.customerId(),debt,pendingDebt,deregistered?"cancelled":"pending",Timestamp.from(at),Timestamp.from(at));
        jdbc.update("UPDATE ticket_use_rewards SET status=?,reversed_at=?,reversal_source=? WHERE id=?","confirmed".equals(r.status())?"reversed":"cancelled",Timestamp.from(at),source,r.id());
        audit.recordSuccess("ticket.reward.reversed","not_applicable","ticket_use_reward",r.id(),null,source,Map.of("recovered_points",debit,"pending_recovery",pendingDebt,"cancelled_recovery",deregistered?debt:0));
    }
    public List<TicketUseRewardRecord> list(String customerId){return jdbc.query("SELECT * FROM ticket_use_rewards WHERE customer_id=? ORDER BY verified_at DESC,id",this::map,customerId);}
    /** BSM:822 只看本条奖励所在人份的退款行：普通退款覆盖整张票全部人份照样命中；安全例外只退一人份，不卡、不冲同票他人份。 */
    private List<String> refundStatuses(String parent,int slot){return jdbc.queryForList("SELECT DISTINCT r.status FROM ticket_refunds r JOIN ticket_refund_lines l ON l.refund_id=r.id JOIN issued_entitlements e ON e.id=l.entitlement_id WHERE e.parent_ticket_id=? AND e.slot_index=?",String.class,parent,slot);}
    private void lockParent(String parent){if(jdbc.queryForList("SELECT id FROM ticket_instances WHERE id=? FOR UPDATE",String.class,parent).isEmpty())throw ApiException.conflict("TICKET_PARENT_REQUIRED","缺少原票人次来源");}
    private CustomerPointsAccountRecord account(String customer,Instant at){
        jdbc.update("INSERT INTO customer_points_accounts(id,customer_id,balance,level,recent_summary,frozen_points,account_status,level_period_start,keep_threshold_progress,check_in_streak,created_at,updated_at) VALUES(?,?,0,'base','{}',0,'active',?,0,0,?,?) ON CONFLICT(customer_id) DO NOTHING","points_"+customer,customer,Timestamp.from(at),Timestamp.from(at),Timestamp.from(at));
        return customers.findPointsByCustomerIdForUpdate(customer).orElseThrow();
    }
    private CustomerPointsAccountRecord balance(CustomerPointsAccountRecord a,long balance,long growth,Instant at){return new CustomerPointsAccountRecord(a.id(),a.customerId(),balance,a.level(),a.recentSummary(),a.frozenPoints(),a.accountStatus(),a.levelPeriodStart(),growth,a.graceUntil(),a.lastCheckInDate(),a.checkInStreak(),a.createdAt(),at);}
    private void ledger(String id,String member,long delta,long balance,String type,String status,String description,Instant at){customers.savePointsLedger(new CustomerPointsLedgerRecord("ledger_"+type+"_"+id,member,delta,balance,type,id,description,null,null,false,at,type,status,"ticket_use",id));}
    private TicketUseRewardRecord find(String id){return jdbc.query("SELECT * FROM ticket_use_rewards WHERE id=?",this::map,id).getFirst();}
    private TicketUseRewardRecord locked(String id){return jdbc.query("SELECT * FROM ticket_use_rewards WHERE id=? FOR UPDATE",this::map,id).getFirst();}
    private TicketUseRewardRecord map(ResultSet r,int n)throws SQLException{return new TicketUseRewardRecord(r.getString("id"),r.getString("order_id"),r.getString("parent_ticket_id"),r.getInt("slot_index"),r.getInt("use_index"),r.getString("verification_id"),r.getString("customer_id"),r.getString("project_id"),r.getString("points_rule_id"),r.getString("growth_rule_id"),r.getString("wait_rule_id"),r.getLong("points"),r.getLong("growth"),r.getInt("waiting_hours"),r.getTimestamp("verified_at").toInstant(),r.getTimestamp("available_at").toInstant(),r.getString("status"),TicketRewardConfigService.instant(r,"confirmed_at"),r.getLong("confirmed_points"),TicketRewardConfigService.instant(r,"reversed_at"),r.getString("reversal_source"));}
}

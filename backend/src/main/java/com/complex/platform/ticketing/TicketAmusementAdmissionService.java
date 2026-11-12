package com.complex.platform.ticketing;

// 文件版本：V0.02

import com.complex.platform.amusement.AmusementBusinessDayService.Window;
import com.complex.platform.amusement.AmusementEntitlementGateway.Consumption;
import com.complex.platform.amusement.AmusementVerificationRecord;
import com.complex.platform.common.api.ApiException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 一次完整营业日的实际消费原件，始终在核销事务内持权益行锁。 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class TicketAmusementAdmissionService {
    private final JdbcTemplate jdbc;
    private final TicketingRepository repository;
    private final com.complex.platform.access.AuthorizationService authorization;
    private final ObjectProvider<TicketVerificationService> verifications;

    public TicketAmusementAdmissionService(JdbcTemplate jdbc, TicketingRepository repository,
            ObjectProvider<TicketVerificationService> verifications,
            com.complex.platform.access.AuthorizationService authorization) {
        this.jdbc=jdbc; this.repository=repository; this.verifications=verifications; this.authorization=authorization;
    }

    public Consumption consume(String entitlementId, Window window, Instant at) {
        requireWindow(window,at);
        var entitlement=lockEntitlement(entitlementId);
        verifications.getObject().requireAmusementConsumption(entitlement,at);
        requireOriginalTicketComplex(entitlement,window);
        // 第十八批 AH 段 219-R1（BSM:800 只能在检票场次内核销、后续场次份额不得提前用）：票内游乐须本场已检票、本场按一份封顶，被拒不扣次。
        if (!"unlimited_in_day".equals(entitlement.consumptionWindow()))
            verifications.getObject().requireAmusementSessionShare(entitlement,at);
        if ("unlimited_in_day".equals(entitlement.consumptionWindow())) {
            requireMultiPersonWindow(entitlement,window,at);
            // 先沿用实际已激活且尚未结束的原窗口，后来改日切版本不能再次收费。
            if (jdbc.queryForObject("""
                    SELECT count(*) FROM entitlement_business_day_activations
                     WHERE entitlement_id=? AND complex_id=? AND status='active'
                       AND business_start_at<=? AND business_end_at>?
                    """,Integer.class,entitlementId,window.complexId(),stamp(at),stamp(at))>0)
                return Consumption.DAY_REPLAY;
        }
        var consumed=repository.atomicUseEntitlement(entitlementId,at);
        if(consumed.isEmpty()) return Consumption.UNAVAILABLE;
        verifications.getObject().requireAmusementConsumption(consumed.get(),at);
        if ("unlimited_in_day".equals(entitlement.consumptionWindow())) {
            jdbc.update("""
                INSERT INTO entitlement_business_day_activations(id,entitlement_id,complex_id,
                    business_day_version_id,business_date,business_start_at,business_end_at,
                    first_activated_at,used_times_after,status)
                VALUES(?,?,?,?,?,?,?,?,?,'active')
                ""","ebda_"+UUID.randomUUID().toString().replace("-",""),entitlementId,window.complexId(),
                window.versionId(),window.businessDate(),stamp(window.startAt()),stamp(window.endAt()),
                stamp(at),consumed.get().usedTimes());
        }
        return Consumption.CONSUMED;
    }

    public boolean preview(String entitlementId,Window window,Instant at) {
        requireWindow(window,at);
        var entitlement=repository.findIssuedEntitlementById(entitlementId).orElseThrow();
        verifications.getObject().requireAmusementConsumption(entitlement,at);
        requireOriginalTicketComplex(entitlement,window);
        if(!"unlimited_in_day".equals(entitlement.consumptionWindow()))
            verifications.getObject().requireAmusementSessionShare(entitlement,at);
        if("unlimited_in_day".equals(entitlement.consumptionWindow())) {
            requireMultiPersonWindow(entitlement,window,at);
            if(jdbc.queryForObject("""
                SELECT count(*) FROM entitlement_business_day_activations
                 WHERE entitlement_id=? AND complex_id=? AND status='active'
                   AND business_start_at<=? AND business_end_at>?
                """,Integer.class,entitlementId,window.complexId(),stamp(at),stamp(at))>0) return true;
        }
        return entitlement.remainingTimes()>0;
    }

    private void requireOriginalTicketComplex(IssuedEntitlementRecord entitlement,Window window) {
        String originalComplex=entitlement.orderId()==null?null:repository.findTicketOrderById(entitlement.orderId())
                .map(TicketOrderRecord::projectId).orElse(null);
        if(originalComplex==null || originalComplex.isBlank())
            throw ApiException.conflict("TICKET_PROJECT_REQUIRED","原游乐票订单缺少真实综合体，不能核销或预览");
        if(!originalComplex.equals(window.complexId()))
            throw ApiException.conflict("ENTITLEMENT_NOT_FOR_PROJECT","该票不属于本次入场的综合体");
    }

    /** 先锁父票以串行化多人不同人份的首次激活，再锁当前权益。 */
    private IssuedEntitlementRecord lockEntitlement(String id) {
        var found=repository.findIssuedEntitlementById(id).orElseThrow(()->
                ApiException.notFound("ENTITLEMENT_NOT_FOUND","权益不存在"));
        if(found.parentTicketId()!=null) {
            if(jdbc.queryForList("SELECT id FROM ticket_instances WHERE id=? FOR UPDATE",String.class,
                    found.parentTicketId()).isEmpty())
                throw ApiException.conflict("TICKET_INSTANCE_REFUND_IN_PROGRESS","原票不存在，不能核销");
        }
        if(jdbc.queryForList("SELECT id FROM issued_entitlements WHERE id=? FOR UPDATE",String.class,id).isEmpty())
            throw ApiException.notFound("ENTITLEMENT_NOT_FOUND","权益不存在");
        return repository.findIssuedEntitlementById(id).orElseThrow();
    }

    private void requireMultiPersonWindow(IssuedEntitlementRecord entitlement,Window window,Instant at) {
        if(entitlement.parentTicketId()==null) return;
        var parent=repository.findTicketInstanceById(entitlement.parentTicketId()).orElseThrow();
        if(parent.numPeople()<=1) return;
        var first=jdbc.query("""
            SELECT a.complex_id,a.business_start_at,a.business_end_at
              FROM entitlement_business_day_activations a JOIN issued_entitlements e ON e.id=a.entitlement_id
             WHERE e.parent_ticket_id=? ORDER BY a.first_activated_at,a.id LIMIT 1
            """,(rs,n)->new Window(rs.getString(1),null,null,rs.getTimestamp(2).toInstant(),
                    rs.getTimestamp(3).toInstant()),parent.id());
        if(!first.isEmpty() && (!window.complexId().equals(first.getFirst().complexId())
                || at.isBefore(first.getFirst().startAt()) || !at.isBefore(first.getFirst().endAt())))
            throw ApiException.conflict("MULTI_PERSON_CROSS_SESSION_FORBIDDEN",
                    "多人票各人份额必须在同一营业日内使用，不能跨营业日");
    }

    public void reverseNewConsumption(String entitlementId,Window window,Instant at) {
        requireWindow(window,at);
        var entitlement=lockEntitlement(entitlementId);
        if ("unlimited_in_day".equals(entitlement.consumptionWindow())) {
            int reversed=jdbc.update("""
                UPDATE entitlement_business_day_activations SET status='reversed',reversed_at=?
                 WHERE entitlement_id=? AND complex_id=? AND first_activated_at=? AND status='active'
                   AND first_amusement_verification_id IS NULL
                """,stamp(at),entitlementId,window.complexId(),stamp(at));
            if(reversed!=1)
                throw ApiException.conflict("AMUSEMENT_ACTIVATION_REVERSE_MISMATCH","未找到本次尚未完成入场的新激活，不能回补其他消费");
        }
        repository.atomicReverseEntitlement(entitlementId,at).orElseThrow(()->
                ApiException.conflict("ENTITLEMENT_REVERSE_FAILED","本次新消费无法回补"));
    }

    public void recordAdmission(AmusementVerificationRecord admission,Window window,Consumption consumption) {
        requireWindow(window,admission.createdAt());
        if(consumption==null || consumption==Consumption.UNAVAILABLE)
            throw ApiException.conflict("AMUSEMENT_CONSUMPTION_REQUIRED","入场缺少成功消费或营业日重入事实");
        lockEntitlement(admission.entitlementId());
        Integer actual=jdbc.queryForObject("""
            SELECT count(*) FROM amusement_verifications WHERE id=? AND entitlement_id=? AND direction='entry'
              AND verification_result='success' AND complex_id=? AND business_day_version_id=?
              AND business_date=? AND business_start_at=? AND business_end_at=? AND created_at=?
            """,Integer.class,admission.id(),admission.entitlementId(),window.complexId(),window.versionId(),
                window.businessDate(),stamp(window.startAt()),stamp(window.endAt()),stamp(admission.createdAt()));
        if(actual!=1)throw ApiException.conflict("AMUSEMENT_ADMISSION_FACT_REQUIRED","缺少本次入场及锁定营业日原件");
        var ticket=verifications.getObject().recordAmusementAdmission(admission,window,consumption);
        if(consumption==Consumption.CONSUMED)jdbc.update("""
            UPDATE entitlement_business_day_activations
               SET first_amusement_verification_id=?,first_ticket_verification_id=?
             WHERE entitlement_id=? AND complex_id=? AND first_activated_at=? AND status='active'
               AND first_amusement_verification_id IS NULL
            """,admission.id(),ticket.id(),admission.entitlementId(),window.complexId(),stamp(admission.createdAt()));
    }

    /** 直连游乐可传设施编号；票务终端列只接真实终端，设施由原入场记录追溯。 */
    public String terminalDeviceId(AmusementVerificationRecord admission) {
        String device=admission.deviceId();
        if(device==null || device.isBlank()) return null;
        if(repository.terminalDeviceExists(device)) return device;
        if(jdbc.queryForObject("SELECT count(*) FROM amusement_devices WHERE project_id=? AND device_code=?",
                Integer.class,admission.projectId(),device)==1) return null;
        throw ApiException.badRequest("DEVICE_NOT_REGISTERED","设备未登记为核销终端或本项目游乐设施");
    }

    public void lockForReversal(VerificationRecord original) {
        requireReversalScope(original);
        lockEntitlement(original.entitlementId());
    }

    public void requireReversalScope(VerificationRecord original) {
        var sources=jdbc.queryForList("""
            SELECT complex_id FROM amusement_verifications
             WHERE id=? AND entitlement_id=? AND direction='entry' AND verification_result='success'
            """,String.class,original.amusementVerificationId(),original.entitlementId());
        authorization.requirePermission("ticket.verify.reverse");
        if(sources.size()!=1 || sources.getFirst()==null)
            throw ApiException.conflict("AMUSEMENT_ADMISSION_FACT_REQUIRED","缺少原游乐核销的综合体事实");
        if(!authorization.projectReach(authorization.currentActor(),"ticket.verify.reverse").covers(sources.getFirst()))
            throw ApiException.forbidden("FORBIDDEN","无权冲正该综合体的游乐核销");
        String merchant=authorization.currentActor().merchantId();
        if(merchant!=null && !merchant.equals(original.merchantId()))
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN","无权冲正其他商户的核销");
    }

    public void reverseFirstActivation(VerificationRecord original,Instant at) {
        lockEntitlement(original.entitlementId());
        jdbc.update("""
            UPDATE entitlement_business_day_activations SET status='reversed',reversed_at=?
             WHERE first_ticket_verification_id=? AND entitlement_id=? AND status='active'
            """,stamp(at),original.id(),original.entitlementId());
    }

    private static void requireWindow(Window window,Instant at) {
        if(window==null || window.complexId()==null || window.versionId()==null || window.businessDate()==null
                || window.startAt()==null || window.endAt()==null || at==null
                || at.isBefore(window.startAt()) || !at.isBefore(window.endAt()))
            throw ApiException.conflict("AMUSEMENT_BUSINESS_DAY_REQUIRED","必须使用真实综合体当前批准的营业日窗口");
    }
    private static Timestamp stamp(Instant value){return Timestamp.from(value);}
}

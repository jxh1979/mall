package com.complex.platform.energy;

// 文件版本：V0.03
import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.Strings.trimToNull;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.energy.dto.*;
import com.complex.platform.evidence.*;
import com.complex.platform.governance.review.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnergyService {
    private static final Set<String> TYPES=Set.of("electricity","water","gas","heat_cold");
    private static final Set<String> OBJECTS=Set.of("public_area","merchant","equipment_circuit");
    private static final String METER="energy.meter.manage",READING="energy.reading.manage";
    private final EnergyRepository repository;
    private final EnergyWorkflowRepository workflow;
    private final AuditEventService audit;
    private final AuthorizationService authorization;
    private final IdempotencyService idempotency;
    private final EvidenceEventRepository evidence;
    private final EvidenceAttachmentValidator attachments;
    private final SupervisorReviewService reviews;

    public EnergyService(EnergyRepository repository, EnergyWorkflowRepository workflow, AuditEventService audit,
            AuthorizationService authorization, IdempotencyService idempotency, EvidenceEventRepository evidence,
            EvidenceAttachmentValidator attachments, SupervisorReviewService reviews) {
        this.repository=repository;this.workflow=workflow;this.audit=audit;this.authorization=authorization;
        this.idempotency=idempotency;this.evidence=evidence;this.attachments=attachments;this.reviews=reviews;
    }
    public PageResponse<EnergyMeterRecord> listMeters() {
        requireEnergyRead();
        var items=repository.listMeters().stream().filter(m->canRead(m.projectId())).toList();
        return new PageResponse<>(items,1,items.size(),items.size());
    }
    public PageResponse<EnergyMeterReadingRecord> listReadings(String period) {
        requireEnergyRead();
        var items=repository.listReadings(period==null?null:period(period)).stream()
                .filter(r->canRead(loadMeter(r.meterId()).projectId())).toList();
        return new PageResponse<>(items,1,items.size(),items.size());
    }
    @Transactional public EnergyMeterRecord createMeter(EnergyMeterRequest r) {
        require(r); scope(requireText(r.projectId(),"project_id"),METER);
        return write("energy.meter_saved","energy.meter_save_failed",METER,r.idempotencyKey(),r,()->{
            workflow.lockMeterGraph(r.projectId());
            String id=trimToNull(r.id())==null?"em_"+UUID.randomUUID():r.id().trim();
            if(repository.findMeterById(id).isPresent()) throw invalid("表计编号已存在");
            validateMeter(r.meterType(),r.gatherObject(),r.merchantId(),r.projectId(),r.parentMeterId());
            Instant now=Instant.now();
            return repository.saveMeter(new EnergyMeterRecord(id,requireText(r.name(),"name"),r.meterType(),trimToNull(r.parentMeterId()),r.gatherObject(),trimToNull(r.spaceId()),trimToNull(r.merchantId()),r.projectId(),mode(r.readMode()==null?"remote":r.readMode()),r.active()==null||r.active(),now,now));
        },EnergyMeterRecord.class,EnergyMeterRecord::id);
    }
    @Transactional public EnergyMeterRecord updateMeter(String id,EnergyMeterRequest r) {
        require(r);var before=scopedMeter(id,METER);
        return write("energy.meter_saved","energy.meter_save_failed",METER,r.idempotencyKey(),r,()->{
            workflow.lockMeterGraph(before.projectId());workflow.lockMeter(id);var cur=loadMeter(id);
            String type=r.meterType()==null?cur.meterType():r.meterType();
            String gather=r.gatherObject()==null?cur.gatherObject():r.gatherObject();
            String project=r.projectId()==null?cur.projectId():r.projectId();
            String merchant=r.merchantId()==null?cur.merchantId():trimToNull(r.merchantId());
            String parent=r.parentMeterId()==null?cur.parentMeterId():trimToNull(r.parentMeterId());
            String space=r.spaceId()==null?cur.spaceId():trimToNull(r.spaceId());
            if(!Objects.equals(type,cur.meterType())||!Objects.equals(gather,cur.gatherObject())||!Objects.equals(project,cur.projectId())||!Objects.equals(merchant,cur.merchantId())||!Objects.equals(parent,cur.parentMeterId())||!Objects.equals(space,cur.spaceId()))
                throw invalid("表计计量对象和归属建档后不可改写，请登记新表及真实交接");
            if(workflow.wasReplaced(id)||workflow.retirement(id).isPresent()) throw invalid("已换下或永久停用的表计不能重新启用或修改");
            if(Boolean.FALSE.equals(r.active())&&(workflow.opening(id).isPresent()||!meterReadings(id).isEmpty()))
                throw invalid("已投入计量的表须登记真实终值、停用时点及照片，不能直接关闭开关");
            return repository.saveMeter(new EnergyMeterRecord(id,r.name()==null?cur.name():requireText(r.name(),"name"),type,parent,gather,space,merchant,project,mode(r.readMode()==null?cur.readMode():r.readMode()),r.active()==null?cur.active():r.active(),cur.createdAt(),Instant.now()));
        },EnergyMeterRecord.class,EnergyMeterRecord::id);
    }
    @Transactional public EnergyWorkflowRepository.Opening initialize(String id,EnergyWorkflowRequests.Opening r) {
        require(r);var meter=scopedMeter(id,METER);
        return write("energy.meter_initialized",METER,r.idempotencyKey(),r,()->{
            workflow.lockMeter(id);
            if(workflow.retirement(id).isPresent()||workflow.wasReplaced(id)||workflow.opening(id).isPresent()||!meterReadings(id).isEmpty()) throw invalid("已有初值或读数，不能改写计量起点；缺失初值须走异常复核");
            decimal(r.initialValue());time(r.initialAt());photos(id,r.attachmentIds());
            var value=new EnergyWorkflowRepository.Opening(id,r.initialValue(),r.initialAt(),actor(),requireText(r.note(),"note"),r.attachmentIds());
            workflow.saveOpening(value);return value;
        },EnergyWorkflowRepository.Opening.class,EnergyWorkflowRepository.Opening::meterId);
    }
    @Transactional public EnergyMeterReadingRecord recordReading(EnergyReadingRecordRequest r) {
        require(r);var meter=scopedMeter(requireText(r.meterId(),"meter_id"),READING);String p=period(r.period());
        return write("energy.reading_recorded",READING,r.idempotencyKey(),r,()->{
            workflow.lockPeriod(meter,p);workflow.lockMeter(meter.id());
            if(r.readMode()!=null&&!"manual".equals(r.readMode()))throw invalid("此入口为员工人工抄表，远程数据须经真实设备接入，不能自行声明");
            return record(meter,p,r.readingValue(),"manual",r.readAt()==null?Instant.now():r.readAt(),r.attachmentIds(),r.manualReason(),r.readerId());
        },EnergyMeterReadingRecord.class,EnergyMeterReadingRecord::id);
    }
    private EnergyMeterReadingRecord record(EnergyMeterRecord meter,String p,BigDecimal value,String mode,Instant at,List<String> proof,String note,String claimedReader) {
        if(!loadMeter(meter.id()).active()||workflow.wasReplaced(meter.id())) throw invalid("表计已停用或已换下");
        mutablePeriod(meter,p);decimal(value);time(at);
        if(!YearMonth.from(at.atZone(com.complex.platform.common.LocalZone.ID)).toString().equals(p))throw invalid("计量实际时点必须属于所填账期");
        if(claimedReader!=null&&!actor().equals(claimedReader)) throw invalid("抄表人由当前登录人员确认，不能代填他人");
        if("manual".equals(mode)) {photos(meter.id(),proof);requireText(note,"manual_reason");}
        else if(proof!=null&&!proof.isEmpty()) photos(meter.id(),proof);
        var previous=meterReadings(meter.id()).stream().max(Comparator.comparing(EnergyMeterReadingRecord::readAt)).orElse(null);
        var opening=workflow.opening(meter.id()).orElse(null);
        if(previous!=null&&(!at.isAfter(previous.readAt())||p.compareTo(previous.period())<0)) throw invalid("读数必须按实际计量时间顺序登记");
        if(previous!=null&&!"reviewed".equals(previous.status())) throw invalid("前一计量区间尚未审核，先核实后再录入下一段");
        if(previous==null&&opening!=null&&!at.isAfter(opening.initialAt())) throw invalid("抄表时点必须晚于启用初值时点");
        BigDecimal start=previous==null?(opening==null?null:opening.initialValue()):workflow.effectiveValue(previous);
        BigDecimal consumption=start==null?null:value.subtract(start);
        String abnormal=start==null?null:classify(meter,value,start,consumption,previous);
        Instant now=Instant.now();
        var saved=repository.saveReading(new EnergyMeterReadingRecord("emr_"+UUID.randomUUID(),meter.id(),p,value,start,consumption,
                start==null||abnormal!=null?"abnormal":"recorded",abnormal,mode,actor(),at,null,null,now,now));
        workflow.saveReadingEvidence(saved.id(),proof==null?List.of():proof,note);
        return saved;
    }
    @Transactional public EnergyMeterReadingRecord reviewReading(String id,EnergyReadingReviewRequest r) {
        require(r);var found=reading(id);var meter=scopedMeter(found.meterId(),READING);
        return write("energy.reading_reviewed",READING,r.idempotencyKey(),r,()->{
            workflow.lockPeriod(meter,found.period());workflow.lockMeter(meter.id());var cur=reading(id);
            if("abnormal".equals(cur.status())) throw invalid("异常读数须由第二人核表并提交主管确认，不能直接审核");
            if(!"recorded".equals(cur.status())) throw invalid("读数已处理");
            if(r.reviewedBy()!=null&&!actor().equals(r.reviewedBy())) throw invalid("审核人由当前登录人员确定");
            checkReadingProof(cur);return reviewed(cur,actor());
        },EnergyMeterReadingRecord.class,EnergyMeterReadingRecord::id);
    }
    @Transactional public EnergyWorkflowRepository.Verification verify(String id,EnergyWorkflowRequests.Verification r) {
        require(r);var found=reading(id);var meter=scopedMeter(found.meterId(),READING);
        return write("energy.reading_verification_submitted",READING,r.idempotencyKey(),r,()->{
            workflow.lockPeriod(meter,found.period());workflow.lockMeter(meter.id());var cur=reading(id);
            if(!"abnormal".equals(cur.status())) throw invalid("仅未核实异常读数可提交复核");
            if(actor().equals(cur.readerId())) throw invalid("必须由原抄表人之外的第二人实际核表");
            var assigned=workflow.assignments(id);
            if(!assigned.isEmpty()&&!actor().equals(assigned.getLast().verifierId()))throw invalid("请由主管最新指派的复抄人提交核实结果");
            if(workflow.verifications(id).stream().anyMatch(v->"pending_review".equals(v.status()))) throw invalid("已有待主管确认的核表记录");
            decimal(r.verifiedValue());
            BigDecimal start=cur.prevReadingValue()==null?r.verifiedOpeningValue():cur.prevReadingValue();
            decimal(start);
            if(cur.prevReadingValue()!=null&&r.verifiedOpeningValue()!=null&&r.verifiedOpeningValue().compareTo(start)!=0) throw invalid("不能改写已核实的前段终值");
            if(r.verifiedValue().compareTo(start)<0) throw invalid("核实后仍倒走，须查清表计或换表事实，不能用负用量出账");
            if(r.method()==null||!Set.of("reread","onsite_check").contains(r.method())) throw invalid("请选择第二人复抄或到场核表");
            photos(meter.id(),r.attachmentIds());String reason=requireText(r.reason(),"reason");
            if(!workflow.activeStaff(r.finalApproverId(),meter.projectId())||!authorization.holdsPermissionViaRole(r.finalApproverId(),"energy.reading.approve"))throw invalid("最终主管须为本项目在岗、具备异常核表确认岗位权限的人员");
            String vid="emv_"+UUID.randomUUID();
            var review=reviews.evaluate(new SupervisorReviewEvaluateRequest(r.idempotencyKey()+":review",reason,r.attachmentIds(),"energy.reading_anomaly_confirm","energy_reading_verification",vid,null,
                    "异常核表：原读数"+cur.readingValue()+"，核实起点"+start+"，核实终值"+r.verifiedValue(),r.finalApproverId(),List.of(),cur.readerId()));
            var result=new EnergyWorkflowRepository.Verification(vid,id,meter.projectId(),meter.merchantId(),r.verifiedValue(),start,r.method(),reason,r.attachmentIds(),actor(),review.reviewId(),"pending_review",Instant.now());
            workflow.saveVerification(result);return result;
        },EnergyWorkflowRepository.Verification.class,EnergyWorkflowRepository.Verification::id);
    }
    @EventListener @Transactional public void decided(SupervisorReviewDecisionRecord decision) {
        if("energy.review_settings".equals(decision.actionType())) {
            var settings=workflow.settingsByReview(decision.reviewId()).orElse(null);
            if(settings==null)return;
            scope(settings.projectId(),"governance.supervisor_review.approve");
            if(Set.of("approved","rejected").contains(decision.reviewStatus()))workflow.decideSettings(decision.reviewId(),decision.reviewStatus());return;
        }
        if(!"energy.reading_anomaly_confirm".equals(decision.actionType())||!Set.of("approved","rejected").contains(decision.reviewStatus()))return;
        var verification=workflow.byReview(decision.reviewId()).orElse(null);if(verification==null||!"pending_review".equals(verification.status()))return;
        var cur=reading(verification.readingId());var meter=loadMeter(cur.meterId());workflow.lockPeriod(meter,cur.period());workflow.lockMeter(meter.id());
        scope(meter.projectId(),"energy.reading.approve");
        if("approved".equals(decision.reviewStatus())) {
            photos(meter.id(),verification.attachmentIds());checkReadingProof(cur);
            reviewed(cur,authorization.currentActor().id());
        }
        workflow.decideVerification(verification.id(),decision.reviewStatus());
    }
    @Transactional public EnergyWorkflowRepository.Retirement retire(String id,EnergyWorkflowRequests.Retirement r) {
        require(r);var meter=scopedMeter(id,METER);scope(meter.projectId(),READING);String p=period(r.period());
        return write("energy.meter_retired",METER,r.idempotencyKey(),r,()->{
            workflow.lockPeriod(meter,p);workflow.lockMeterGraph(meter.projectId());workflow.lockMeter(id);
            if(!loadMeter(id).active()||workflow.wasReplaced(id)||workflow.retirement(id).isPresent())throw invalid("表计已停用或已换下");
            mutablePeriod(meter,p);decimal(r.finalValue());time(r.retiredAt());photos(id,r.attachmentIds());requireText(r.note(),"note");
            if(!YearMonth.from(r.retiredAt().atZone(com.complex.platform.common.LocalZone.ID)).toString().equals(p))throw invalid("停用时点必须属于所填账期");
            var latest=meterReadings(id).stream().max(Comparator.comparing(EnergyMeterReadingRecord::readAt)).orElse(null);
            EnergyMeterReadingRecord terminal;
            if(latest!=null&&latest.readAt().equals(r.retiredAt())&&latest.period().equals(p)&&latest.readingValue().compareTo(r.finalValue())==0&&"manual".equals(latest.readMode())){
                checkReadingProof(latest);terminal=latest;
            }else terminal=record(meter,p,r.finalValue(),"manual",r.retiredAt(),r.attachmentIds(),r.note(),null);
            var saved=new EnergyWorkflowRepository.Retirement(id,terminal.id(),r.retiredAt());workflow.saveRetirement(saved);
            repository.saveMeter(new EnergyMeterRecord(id,meter.name(),meter.meterType(),meter.parentMeterId(),meter.gatherObject(),meter.spaceId(),meter.merchantId(),meter.projectId(),meter.readMode(),false,meter.createdAt(),Instant.now()));
            return saved;
        },EnergyWorkflowRepository.Retirement.class,EnergyWorkflowRepository.Retirement::meterId);
    }
    @Transactional public EnergyWorkflowRepository.Replacement replace(String id,EnergyWorkflowRequests.Replacement r) {
        require(r);var meter=scopedMeter(id,METER);scope(meter.projectId(),READING);String p=period(r.period());
        return write("energy.meter_replaced",METER,r.idempotencyKey(),r,()->{
            workflow.lockPeriod(meter,p);workflow.lockMeterGraph(meter.projectId());workflow.lockMeter(id);decimal(r.newInitialValue());photos(id,r.attachmentIds());
            var terminal=record(meter,p,r.oldFinalValue(),"manual",r.replacedAt(),r.attachmentIds(),requireText(r.note(),"note"),null);
            Instant now=Instant.now();String newId="em_"+UUID.randomUUID();
            repository.saveMeter(new EnergyMeterRecord(id,meter.name(),meter.meterType(),meter.parentMeterId(),meter.gatherObject(),meter.spaceId(),meter.merchantId(),meter.projectId(),meter.readMode(),false,meter.createdAt(),now));
            repository.saveMeter(new EnergyMeterRecord(newId,requireText(r.newMeterName(),"new_meter_name"),meter.meterType(),meter.parentMeterId(),meter.gatherObject(),meter.spaceId(),meter.merchantId(),meter.projectId(),meter.readMode(),true,now,now));
            workflow.saveOpening(new EnergyWorkflowRepository.Opening(newId,r.newInitialValue(),r.replacedAt(),actor(),r.note(),r.attachmentIds()));
            var value=new EnergyWorkflowRepository.Replacement("emx_"+UUID.randomUUID(),id,newId,p,r.oldFinalValue(),r.newInitialValue(),r.replacedAt(),terminal.id(),r.note(),r.attachmentIds(),actor());workflow.saveReplacement(value);workflow.transferChildren(id,newId,r.replacedAt(),value.id());return value;
        },EnergyWorkflowRepository.Replacement.class,EnergyWorkflowRepository.Replacement::id);
    }
    @Transactional public EnergyWorkflowRepository.Charge recordCharge(EnergyWorkflowRequests.ActualCharge r) {
        require(r);var meter=scopedMeter(r.meterId(),READING);String p=period(r.period());
        return write("energy.actual_charge_recorded",READING,r.idempotencyKey(),r,()->{
            workflow.lockPeriod(meter,p);workflow.lockMeter(meter.id());mutablePeriod(meter,p);
            if(!"merchant".equals(meter.gatherObject()))throw invalid("公区与设备回路不向商户单列能源费");
            if(!workflow.validContract(r.contractId(),meter.merchantId(),meter.projectId(),p))throw invalid("须使用本商户本项目当期有效且约定独表实费代收的合同");
            if(r.amountCent()<0||r.dueDate()==null)throw invalid("须填真实非负供能费用及供能单位最后付款日期");
            proof(meter.id(),r.attachmentIds());requireText(r.supplierBillReference(),"supplier_bill_reference");
            var rows=meterReadings(meter.id()).stream().filter(x->p.equals(x.period())).toList();
            if(rows.isEmpty()||rows.stream().anyMatch(x->!"reviewed".equals(x.status())))throw invalid("当期计量区间须全部审核后才能登记实际费用");
            BigDecimal usage=rows.stream().map(workflow::effectiveConsumption).reduce(BigDecimal.ZERO,BigDecimal::add);
            var c=new EnergyWorkflowRepository.Charge("eac_"+UUID.randomUUID(),r.contractId(),meter.id(),meter.merchantId(),meter.projectId(),p,r.amountCent(),r.dueDate(),r.supplierBillReference(),r.attachmentIds(),rows.stream().map(EnergyMeterReadingRecord::id).toList(),usage,actor(),Instant.now(),null,null);
            workflow.saveCharge(c);return c;
        },EnergyWorkflowRepository.Charge.class,EnergyWorkflowRepository.Charge::id);
    }
    public List<EnergyWorkflowRepository.Charge> charges(String period) {return readAudit("energy.actual_charges_viewed",READING,"energy_actual_charge",period,()->{authorization.requirePlatformScope();authorization.requirePermission(READING);return workflow.charges(period(period)).stream().filter(c->canReach(c.projectId(),READING)).toList();});}
    @Transactional public List<EnergyFeeSettlementRecord> settleEnergyFee(String period) {return settleEnergyFee(period,null);}
    @Transactional public List<EnergyFeeSettlementRecord> settleEnergyFee(String period,String key) {
        authorization.requirePlatformScope();String p=period(period);List<EnergyFeeSettlementRecord> result=new ArrayList<>();
        var grouped=new LinkedHashMap<String,List<EnergyWorkflowRepository.Charge>>();
        for(var c:charges(p)) if(c.settledAt()==null)grouped.computeIfAbsent(c.projectId()+":"+c.merchantId(),x->new ArrayList<>()).add(c);
        for(var group:grouped.values()) {
            var first=group.getFirst();workflow.lockPeriod(first.merchantId(),first.projectId(),p);
            if(workflow.hasUnreviewed(first.merchantId(),first.projectId(),p)||workflow.hasUnchargedReadings(first.merchantId(),first.projectId(),p)||workflow.hasMissingReadings(first.merchantId(),first.projectId(),p))continue;
            var byContract=new LinkedHashMap<String,List<EnergyWorkflowRepository.Charge>>();
            for(var c:workflow.charges(p))if(c.merchantId().equals(first.merchantId())&&c.projectId().equals(first.projectId())&&c.settledAt()==null)byContract.computeIfAbsent(c.contractId(),x->new ArrayList<>()).add(c);
            for(var entries:byContract.values()) {
                var c=entries.getFirst();
                if(!workflow.validContract(c.contractId(),c.merchantId(),c.projectId(),p))throw invalid("合同已不满足当期实际代收条件");
                long amount=0;for(var fee:entries){proof(fee.meterId(),fee.attachmentIds());amount=Math.addExact(amount,fee.amountCent());}
                if(repository.energyFeeArExists(c.merchantId(),c.contractId(),p))throw invalid("同合同当期已出账，不能追加重复费用");
                if(amount==0){workflow.markBilled(entries,null);continue;}
                String source=energyFeeSourceId(c.merchantId(),c.contractId(),p),token=stable(source);
                var due=entries.stream().map(EnergyWorkflowRepository.Charge::dueDate).min(Comparator.naturalOrder()).orElseThrow();
                var saved=repository.insertEnergyFeeAr(new EnergyFeeSettlementRecord("efs_"+token.substring(0,28),c.contractId(),c.merchantId(),c.projectId(),p,"by_contract",amount,"arap_energy_"+token.substring(0,24),"energy_fee",source,"receivable","pending_collection",Instant.now(),due));
                workflow.markBilled(entries,saved.arApItemId());result.add(saved);
            }
        }
        audit.recordSuccess("energy.fee_settled",READING,"energy_fee",p,null,key,Map.of("period",p,"count",result.size(),"accounting_nature","collection_on_behalf"));return result;
    }
    public Map<String,Object> history(String id) {return readAudit("energy.history_viewed",READING,"energy_meter",id,()->{
        requireEnergyRead();var meter=loadMeter(id);if(!canRead(meter.projectId()))throw ApiException.forbidden("FORBIDDEN","无权查看本项目能源记录");Map<String,Object> value=new LinkedHashMap<>();value.put("opening",workflow.opening(id).orElse(null));value.put("replacements",workflow.replacements(id));value.put("retirement",workflow.retirement(id).orElse(null));
        value.put("readings",meterReadings(id).stream().map(r->{Map<String,Object> row=new LinkedHashMap<>();row.put("original",r);row.put("confirmed_value","reviewed".equals(r.status())?workflow.effectiveValue(r):null);row.put("confirmed_consumption","reviewed".equals(r.status())?workflow.effectiveConsumption(r):null);row.put("evidence",workflow.readingEvidence(r.id()));row.put("verifications",workflow.verifications(r.id()));row.put("assignments",workflow.assignments(r.id()));row.put("escalation",workflow.escalation(r.id()));return row;}).toList());
        value.put("review_settings",workflow.settings(meter.projectId()).orElse(null));return value;
    });}
    public EnergyMeterDifferenceRecord meterDifference(String id,String period) {
        var parent=scopedMeter(id,READING);String p=period(period);
        var own=meterReadings(id).stream().filter(r->p.equals(r.period())).toList();
        var children=repository.listChildReadings(id,p);
        if(own.isEmpty()||own.stream().anyMatch(r->!"reviewed".equals(r.status()))||children.stream().anyMatch(r->!"reviewed".equals(r.status())))throw invalid("计量区间未核实齐全，暂不能作总分表差额依据");
        BigDecimal a=own.stream().map(workflow::effectiveConsumption).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal b=children.stream().map(workflow::effectiveConsumption).reduce(BigDecimal.ZERO,BigDecimal::add);
        return new EnergyMeterDifferenceRecord(id,p,a,b,a.subtract(b));
    }
    public List<EnergyWorkflowRepository.Settings> settings(String projectId){return readAudit("energy.settings_viewed",METER,"energy_review_settings",projectId,()->{scope(projectId,METER);return workflow.settingsHistory(projectId);});}
    @Transactional public EnergyWorkflowRepository.Settings configure(String projectId,EnergyWorkflowRequests.ReviewSettings r) {
        require(r);scope(projectId,METER);
        return write("energy.review_settings_submitted",METER,r.idempotencyKey(),r,()->{
            if(r.minDays()<1||r.maxDays()<r.minDays()||r.days()<r.minDays()||r.days()>r.maxDays())throw invalid("核实天数须处于已填写的合法上下限内");
            if(!workflow.activeStaff(r.supervisorId(),projectId)||!authorization.holdsPermissionViaRole(r.supervisorId(),"energy.reading.approve"))throw invalid("能耗主管须为本项目在岗且具备核表确认岗位权限的人员");
            String id="ers_"+UUID.randomUUID();var review=reviews.evaluate(new SupervisorReviewEvaluateRequest(r.idempotencyKey()+":review",requireText(r.reason(),"reason"),List.of(),"energy.review_settings","energy_review_settings",id,null,"能耗核实期限及上下限",r.finalApproverId(),List.of()));
            var result=new EnergyWorkflowRepository.Settings(id,projectId,r.days(),r.minDays(),r.maxDays(),r.supervisorId(),review.reviewId(),"pending_review",Instant.now());workflow.saveSettings(result);return result;
        },EnergyWorkflowRepository.Settings.class,EnergyWorkflowRepository.Settings::id);
    }
    @Transactional public EnergyWorkflowRepository.Assignment assign(String id,EnergyWorkflowRequests.Assignment r) {
        require(r);var reading=reading(id);var meter=scopedMeter(reading.meterId(),"energy.reading.approve");
        return write("energy.verifier_assigned","energy.reading.approve",r.idempotencyKey(),r,()->{
            workflow.lockPeriod(meter,reading.period());
            if(!"abnormal".equals(reading(id).status()))throw invalid("该读数已无未结异常");
            var settings=workflow.settings(meter.projectId()).orElseThrow(()->invalid("先补齐经批准的项目核实期限和主管配置"));
            if(!actor().equals(settings.supervisorId()))throw invalid("由本项目已配置能耗主管指派复抄人");
            if(requireText(r.verifierId(),"verifier_id").equals(reading.readerId())||!workflow.activeStaff(r.verifierId(),meter.projectId())||!authorization.holdsPermissionViaRole(r.verifierId(),READING))throw invalid("须指定原抄表人之外、具备本项目抄表岗位权限的在岗人员");
            var result=new EnergyWorkflowRepository.Assignment(id,r.verifierId(),requireText(r.reason(),"reason"),actor(),Instant.now());workflow.saveAssignment(result);return result;
        },EnergyWorkflowRepository.Assignment.class,EnergyWorkflowRepository.Assignment::readingId);
    }
    public void requireEvidenceScope(String id,String evidencePermission) {
        var meter=loadMeter(id);authorization.requirePlatformScope();
        if(!"read".equals(evidencePermission)){
            authorization.requirePermission(evidencePermission);
            if(!canReach(meter.projectId(),evidencePermission))throw ApiException.forbidden("FORBIDDEN","附件权限不在本表计项目范围内");
        }
        boolean writing="evidence.attachment.create".equals(evidencePermission)||"evidence.attachment.upload".equals(evidencePermission)||"evidence.attachment.manage".equals(evidencePermission);
        var allowed=writing?List.of(METER,READING):List.of(METER,READING,"energy.reading.approve");
        if(allowed.stream().noneMatch(p->authorization.currentActorHasPermission(p)&&canReach(meter.projectId(),p)))
            throw ApiException.forbidden("FORBIDDEN","无权访问该表计的能源证据");
    }
    public List<Map<String,Object>> escalations(){return readAudit("energy.escalations_viewed","energy.reading.approve","energy_escalation",actor(),()->{authorization.requirePlatformScope();authorization.requirePermission("energy.reading.approve");return workflow.pendingEscalations(actor()).stream().filter(row->canReach((String)row.get("project_id"),"energy.reading.approve")).toList();});}
    public void requireMeterAttachment(String id,String attachmentId){readAudit("energy.attachment_viewed",READING,"energy_meter",id,()->{
        requireEvidenceScope(id,"read");
        var inherited=workflow.replacements(id).stream().filter(r->id.equals(r.newMeterId())&&r.attachmentIds().contains(attachmentId)).findFirst();
        proof(inherited.map(EnergyWorkflowRepository.Replacement::oldMeterId).orElse(id),List.of(attachmentId));return true;
    });}
    private EnergyMeterReadingRecord reviewed(EnergyMeterReadingRecord r,String who){Instant now=Instant.now();return repository.saveReading(new EnergyMeterReadingRecord(r.id(),r.meterId(),r.period(),r.readingValue(),r.prevReadingValue(),r.consumption(),"reviewed",r.abnormalReason(),r.readMode(),r.readerId(),r.readAt(),who,now,r.createdAt(),now));}
    private void mutablePeriod(EnergyMeterRecord meter,String period){if(workflow.periodSettled(meter,period)||workflow.hasCharge(meter.id(),period))throw invalid("该商户账期已出账或该表已核实实际费用，不能改写计量事实");}
    private void validateMeter(String type,String gather,String merchant,String project,String parent){if(type==null||gather==null||!TYPES.contains(type)||!OBJECTS.contains(gather))throw invalid("表计类型或归集对象无效");if("merchant".equals(gather)&&trimToNull(merchant)==null)throw invalid("商户独立表必须绑定商户");if(!workflow.validProject(project))throw invalid("项目不存在");if("merchant".equals(gather)&&!workflow.validMerchant(merchant,project))throw invalid("独表商户须有本项目合同或铺位归属");if(!"merchant".equals(gather)&&trimToNull(merchant)!=null)throw invalid("公区或设备回路不绑定商户费用对象");if(parent!=null){var p=scopedMeter(parent,METER);if(!p.projectId().equals(project)||!p.meterType().equals(type))throw invalid("总分表须属于同项目同计量类型");}}
    private String classify(EnergyMeterRecord meter,BigDecimal end,BigDecimal start,BigDecimal used,EnergyMeterReadingRecord previous){
        if(end.compareTo(start)<0)return "reverse";if(end.signum()==0||used.signum()==0)return "zero";if(previous==null)return null;
        BigDecimal prior=workflow.effectiveConsumption(previous);if(prior==null||prior.signum()<=0)return "surge";
        var threshold=repository.findThresholdByMeterType(meter.meterType()).orElseThrow(()->invalid("请先配置该表计类型的异常阈值"));
        BigDecimal delta=used.subtract(prior).multiply(BigDecimal.valueOf(100)).divide(prior,6,RoundingMode.HALF_UP);
        return delta.compareTo(threshold.surgePct())>0?"surge":delta.negate().compareTo(threshold.dropPct())>0?"drop":null;
    }
    private void checkReadingProof(EnergyMeterReadingRecord r){if("manual".equals(r.readMode()))photos(r.meterId(),workflow.readingEvidence(r.id()).attachmentIds());}
    private void photos(String meter,List<String> ids){proof(meter,ids);attachments.requirePhotos(ids,"attachment_ids");}
    private void proof(String meter,List<String> ids){if(ids==null||ids.isEmpty())throw invalid("请先上传本表计的真实凭据");for(String id:ids){var a=evidence.findAttachmentById(id).orElseThrow(()->invalid("附件不存在"));if(!"energy_meter".equals(a.businessType())||!meter.equals(a.businessId()))throw invalid("附件须绑定本表计");attachments.requireUploaded(a);}}
    private EnergyMeterReadingRecord reading(String id){return repository.findReadingById(id).orElseThrow(()->ApiException.notFound("NOT_FOUND","抄表记录不存在"));}
    private List<EnergyMeterReadingRecord> meterReadings(String id){return repository.listReadings(null).stream().filter(r->id.equals(r.meterId())).toList();}
    private EnergyMeterRecord loadMeter(String id){return repository.findMeterById(id).orElseThrow(()->ApiException.notFound("NOT_FOUND","表计不存在"));}
    private EnergyMeterRecord scopedMeter(String id,String permission){var meter=loadMeter(requireText(id,"meter_id"));scope(meter.projectId(),permission);return meter;}
    private void requireEnergyRead(){authorization.requirePlatformScope();if(List.of(METER,READING,"energy.reading.approve").stream().noneMatch(authorization::currentActorHasPermission))throw ApiException.forbidden("PERMISSION_DENIED","没有能源查看岗位权限");}
    private boolean canRead(String project){return List.of(METER,READING,"energy.reading.approve").stream().anyMatch(p->authorization.currentActorHasPermission(p)&&canReach(project,p));}
    private boolean canReach(String project,String permission){return authorization.canReachProject(authorization.currentActor(),project,permission);}
    private void scope(String project,String permission){authorization.requirePlatformScope();authorization.requirePermission(permission);if(project==null||!canReach(project,permission))throw ApiException.forbidden("FORBIDDEN","无权访问该项目能源资料");}
    private String actor(){return authorization.currentActor().id();}
    private String period(String value){String p=requireText(value,"period");try{YearMonth.parse(p);}catch(Exception e){throw invalid("账期格式应为有效的年和月");}return p;}
    private String mode(String value){if(!Set.of("manual","remote").contains(value))throw invalid("抄表方式无效");return value;}
    private void decimal(BigDecimal n){if(n==null||n.signum()<0)throw invalid("请填写核实后的非负读数");}
    private void time(Instant t){if(t==null||t.isAfter(Instant.now()))throw invalid("请填写已实际发生的计量时点");}
    private void require(Object r){if(r==null)throw invalid("请求内容不能为空");}
    private ApiException invalid(String text){return ApiException.conflict("ENERGY_CONFIRMATION_REQUIRED",text);}
    private <T> T readAudit(String event,String permission,String type,String id,Supplier<T> work){
        try{T value=work.get();audit.recordSuccess(event,permission,type,id,null,null,Map.of());return value;}
        catch(RuntimeException e){audit.recordFailure(event+"_failed",permission,type,id,null,null,e.getMessage());throw e;}
    }
    private <T> T write(String event,String permission,String key,Object request,Supplier<T> work,Class<T> type,java.util.function.Function<T,String> objectId){
        return write(event,event+"_failed",permission,key,request,work,type,objectId);
    }
    private <T> T write(String event,String failureEvent,String permission,String key,Object request,Supplier<T> work,Class<T> type,java.util.function.Function<T,String> objectId){
        try{return idempotency.execute(event,key,request,()->{T result=work.get();audit.recordSuccess(event,permission,"energy_meter",objectId.apply(result),null,key,Map.of());return result;},type);}catch(RuntimeException e){audit.recordFailure(failureEvent,permission,"energy_meter","energy",null,key,e.getMessage());throw e;}
    }
    static String energyFeeSourceId(String merchant,String contract,String period){return "energy_fee_"+stable(merchant+"|"+contract+"|"+period);}
    private static String stable(String text){return UUID.nameUUIDFromBytes(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString().replace("-","");}
}

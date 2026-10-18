package com.complex.platform.access.staff;

// 文件版本：V0.01

import static com.complex.platform.access.staff.StaffHandoverRepository.text;
import static com.complex.platform.common.Strings.requireText;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffHandoverService {
    private static final String HR_PERMISSION="staff.profile.review";
    private final JdbcStaffRepository staff;
    private final StaffHandoverRepository handovers;
    private final AccessRepository access;
    private final AuthorizationService authorization;
    private final StaffHrRoutingService hrRouting;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;

    public StaffHandoverService(JdbcStaffRepository staff,StaffHandoverRepository handovers,AccessRepository access,
            AuthorizationService authorization,StaffHrRoutingService hrRouting,IdempotencyService idempotency,AuditEventService audit) {
        this.staff=staff; this.handovers=handovers; this.access=access; this.authorization=authorization;
        this.hrRouting=hrRouting; this.idempotency=idempotency; this.audit=audit;
    }

    public record Pending(String businessType,String businessId,String businessName,String status,String projectId,
            String merchantId,boolean assignable,String holdReason) {}
    public record Overview(boolean businessAccessStopped,List<StaffHandoverRepository.Revision> revisions,
            List<Pending> pending,List<StaffHandoverRepository.Transfer> history) {}
    public record Request(String idempotencyKey,String businessType,String businessId,String targetUserId,String reason) {}

    public Overview overview(String profileId) {
        return withFailureAudit("staff.business_handover.view_failed",profileId,null,()->{
            authorization.requirePermission(HR_PERMISSION);
            Overview result=readOverview(profileId);
            audit.recordSuccess("staff.business_handover.viewed",HR_PERMISSION,"staff_profile",profileId,null,null,
                    Map.of("pending_count",result.pending().size(),"history_count",result.history().size()));
            return result;
        });
    }

    private Overview readOverview(String profileId) {
        StaffProfileRecord profile=requireProfile(profileId);
        AccessActor actor=authorization.currentActor();
        var permissions=authorization.effectivePermissions().grants();
        List<Pending> pending=new ArrayList<>();
        for(var source:handovers.pending(profile.userId())) {
            var scope=handovers.scope(source);
            // 未知来源不伪造项目；人事能看待澄清项，但不能交接。已确定的外范围事项不泄露。
            if(scope.resolved()&&!covers(actor,scope,source.kind().managePermission)) continue;
            boolean permitted=permissions.contains(source.kind().managePermission);
            boolean masterNode=source.kind()==StaffHandoverRepository.Kind.approval_node&&"master".equals(text(source.data(),"approver_kind"));
            boolean assignable=scope.resolved()&&permitted&&(!masterNode||authorization.isMasterAuthority(actor));
            String reason=!scope.resolved()?"原业务项目或商户归属待核实":!permitted?"需具备该业务的指派权限":masterNode&&!authorization.isMasterAuthority(actor)?"须由最高权限本人逐单交接":null;
            pending.add(new Pending(source.kind().name(),source.id(),source.kind().label,text(source.data(),"status"),
                    scope.projectId(),scope.merchantId(),assignable,reason));
        }
        return new Overview(access.isEmploymentBusinessBlocked(profile.userId(),Instant.now()),handovers.revisions(profileId),
                pending,handovers.history(profileId).stream().filter(h->covers(actor,
                        new StaffHandoverRepository.Scope(h.projectId(),h.merchantId(),h.projectId()==null,true),
                        StaffHandoverRepository.Kind.parse(h.businessType()).managePermission)).toList());
    }

    @Transactional
    public StaffHandoverRepository.Transfer assign(String profileId,Request request) {
        return withFailureAudit("staff.business_handover.assign_failed",profileId,request==null?null:request.idempotencyKey(),()->{
            authorization.requirePermission(HR_PERMISSION);
            return doAssign(profileId,request);
        });
    }

    private StaffHandoverRepository.Transfer doAssign(String profileId,Request request) {
        if(request==null) throw ApiException.badRequest("VALIDATION_FAILED","请填写交接信息");
        String key=requireText(request.idempotencyKey(),"idempotency_key");
        String targetId=requireText(request.targetUserId(),"target_user_id");
        String businessId=requireText(request.businessId(),"business_id");
        String reason=requireText(request.reason(),"reason");
        if(reason.length()>1000) throw ApiException.badRequest("VALIDATION_FAILED","交接原因不能超过1000字");
        var kind=StaffHandoverRepository.Kind.parse(request.businessType());
        StaffProfileRecord profile=requireProfile(profileId);
        // 当前授权必须先于幂等重放；已经交接后重试只返回原流水，不再改第二次责任人。
        authorization.requirePermission(kind.managePermission);
        return idempotency.execute("staffEmploymentHandover:"+profileId,key,request,()->{
            access.lockGrantAuthorizationFacts();
            authorization.requirePermission(HR_PERMISSION);
            authorization.requirePermission(kind.managePermission);
            StaffProfileRecord current=requireProfile(profileId);
            if(!access.isEmploymentBusinessBlocked(current.userId(),Instant.now()))
                throw ApiException.conflict("STAFF_STOP_NOT_EFFECTIVE","停岗或离职尚未生效，不能办理本项交接");
            String reviewId=handovers.effectiveStopReview(profileId,Instant.now());
            if(targetId.equals(current.userId())) throw ApiException.badRequest("HANDOVER_TARGET_INVALID","接手人不能是原责任人");
            var source=handovers.lock(kind,current.userId(),businessId)
                    .orElseThrow(()->ApiException.conflict("HANDOVER_SOURCE_CHANGED","原业务已结束或已由其他人员承接，请刷新"));
            var scope=handovers.scope(source);
            if(!scope.resolved()) throw ApiException.conflict("HANDOVER_SCOPE_UNRESOLVED","原业务项目或商户归属待核实，暂不能交接");
            AccessActor actor=authorization.currentActor();
            if(!covers(actor,scope,kind.managePermission)) throw ApiException.forbidden("HANDOVER_SCOPE_FORBIDDEN","原业务不在可指派范围内");
            AccessUser target=access.findUserById(targetId).orElseThrow(()->ApiException.notFound("USER_NOT_FOUND","接手账号不存在"));
            var targetProfile=staff.findProfileByUserId(targetId).orElseThrow(()->ApiException.badRequest("HANDOVER_TARGET_NOT_STAFF","接手人必须具有员工档案"));
            if(!"active".equals(target.status())||!"active".equals(targetProfile.status())
                    ||!"approved".equals(targetProfile.registrationStatus())||targetProfile.archivedAt()!=null
                    ||!Set.of("employed","trial").contains(targetProfile.employmentStatus())
                    ||access.isEmploymentBusinessBlocked(targetId,Instant.now()))
                throw ApiException.conflict("HANDOVER_TARGET_NOT_ACTIVE","接手人须为在岗且有效的员工");
            AccessActor recipient=new AccessActor(target.id(),target.username(),target.accountType(),target.organizationId(),target.merchantId(),target.status());
            String workPermission=kind.workPermission;
            if(kind==StaffHandoverRepository.Kind.merchant_work_order&&"resolved".equals(text(source.data(),"status"))) workPermission="merchant.service_work_order.close";
            if(kind==StaffHandoverRepository.Kind.approval_node) {
                checkApproval(source,actor,targetId);
                if("hr_review".equals(text(source.data(),"line_code"))) workPermission="staff.hr.review";
            }
            if(!covers(recipient,scope,workPermission)) throw ApiException.forbidden("HANDOVER_TARGET_SCOPE_INVALID","接手人不能处理该项目或商户的原业务");
            var effective=authorization.effectivePermissionsForUser(targetId,authorization.isMasterAuthority(actor)
                    ?new AuthorizationService.ProjectReach(true,true,null,true,Set.of()):authorization.projectReach(actor,HR_PERMISSION));
            if(!effective.grants().contains(workPermission)) throw ApiException.forbidden("HANDOVER_TARGET_PERMISSION_MISSING","接手人缺少原业务执行权限");
            access.lockAccessUsers(List.of(current.userId(),targetId));
            // 发起停岗与交接并发时，在最终责任写入前再核查到点阻断。
            if(access.isEmploymentBusinessBlocked(targetId,Instant.now())) throw ApiException.conflict("HANDOVER_TARGET_NOT_ACTIVE","接手人的停岗或离职已生效");
            Instant now=Instant.now();
            var transfer=new StaffHandoverRepository.Transfer("sbh_"+UUID.randomUUID(),kind.name(),source.id(),current.userId(),targetId,
                    reason,actor.id(),now,handovers.responsibilityStart(source),scope.projectId(),scope.merchantId());
            handovers.transfer(source,targetId,actor.id(),now);
            handovers.save(profileId,reviewId,scope,source,transfer);
            audit.recordSuccess("staff.business_handover.assigned",HR_PERMISSION,kind.name(),source.id(),null,key,
                    Map.of("profile_id",profileId,"previous_user_id",current.userId(),"target_user_id",targetId,"review_id",reviewId));
            return transfer;
        },StaffHandoverRepository.Transfer.class);
    }

    private void checkApproval(StaffHandoverRepository.Source source,AccessActor actor,String targetId) {
        var review=handovers.review(text(source.data(),"review_id"));
        if(targetId.equals(text(review,"initiated_by"))||targetId.equals(text(review,"beneficiary_id")))
            throw ApiException.forbidden("SUPERVISOR_FORWARD_TARGET_FORBIDDEN","接手审批人不得是申请人或直接受益人");
        String kind=text(source.data(),"approver_kind");
        if("master".equals(kind)&&!authorization.isMasterAuthority(actor))
            throw ApiException.forbidden("MASTER_AUTHORITY_REQUIRED","最高权限审批节点须由最高权限本人逐单交接");
        if("hr_three_levels_up".equals(text(review,"approval_level"))) {
            String permission="hr_review".equals(text(source.data(),"line_code"))?"staff.hr.review":"governance.supervisor_review.approve";
            if(!hrRouting.canReviewEmployee(targetId,text(review,"beneficiary_id"),permission))
                throw ApiException.forbidden("HR_APPROVER_SCOPE_INVALID","接手人须具备对应人事审批角色及员工范围");
        }
        if("role".equals(kind)&&!authorization.holdsPermissionViaRole(targetId,text(source.data(),"approver_id"))
                &&!authorization.holdsPermissionViaRole(targetId,"governance.supervisor_review.approve"))
            throw ApiException.forbidden("SUPERVISOR_ROLE_REQUIRED","接手人缺少本节点要求的审批角色");
    }

    private StaffProfileRecord requireProfile(String id) {
        var profile=staff.findProfileById(id).orElseThrow(()->ApiException.notFound("STAFF_PROFILE_NOT_FOUND","员工档案不存在"));
        if(profile.userId()==null) throw ApiException.conflict("STAFF_ACCOUNT_MISSING","员工尚未关联业务账号");
        var user=access.findUserById(profile.userId()).orElseThrow(()->ApiException.notFound("USER_NOT_FOUND","员工账号不存在"));
        var actor=authorization.currentActor();
        if(!authorization.isMasterAuthority(actor)&&(!authorization.canReachUser(actor,user,HR_PERMISSION)
                ||(actor.merchantId()!=null&&!actor.merchantId().equals(profile.merchantId()))))
            throw ApiException.forbidden("STAFF_PROFILE_SCOPE_FORBIDDEN","无权处理该员工的交接");
        return profile;
    }

    private <T> T withFailureAudit(String event,String profileId,String key,java.util.function.Supplier<T> action) {
        try { return action.get(); }
        catch(RuntimeException failure) {
            try { audit.recordFailure(event,HR_PERMISSION,"staff_profile",profileId,null,key,
                    failure instanceof ApiException api?api.code():failure.getClass().getSimpleName()); }
            catch(RuntimeException auditFailure) { failure.addSuppressed(auditFailure); }
            throw failure;
        }
    }

    private boolean covers(AccessActor actor,StaffHandoverRepository.Scope scope,String permission) {
        if(!scope.resolved()) return false;
        if(actor.merchantId()!=null&&!actor.merchantId().equals(scope.merchantId())) return false;
        if(authorization.isMasterAuthority(actor)) return true;
        return scope.global()?authorization.allowsPlatformScope(actor,permission):authorization.canReachProject(actor,scope.projectId(),permission);
    }
}

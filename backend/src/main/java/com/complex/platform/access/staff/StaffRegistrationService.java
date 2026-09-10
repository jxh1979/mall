package com.complex.platform.access.staff;

// 文件版本：V0.03

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.staff.dto.StaffSelfRegistrationRequest;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.BusinessEntityRecord;
import com.complex.platform.governance.MerchantRecord;
import com.complex.platform.governance.PlatformRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.trimToNull;


/** 注册阶段尚无员工账号；公开入口只产生待审申请，不发放账号或权限。 */
@Service
public class StaffRegistrationService {
    private final JdbcStaffRepository staff;
    private final PlatformRepository platform;
    private final AccessRepository access;
    private final AuditEventService audit;
    private final IdempotencyService publicRequests;
    private static final Set<String> COMPANY_ROLES = Set.of("platform", "project_company", "management_company");

    public StaffRegistrationService(JdbcStaffRepository staff, PlatformRepository platform, AccessRepository access,
            AuditEventService audit, IdempotencyRepository idempotency, ObjectMapper json) {
        this.staff = staff;
        this.platform = platform;
        this.access = access;
        this.audit = audit;
        // 公开申请没有已认证主体，复用幂等实现的独立操作命名空间；响应仅含申请编号与状态。
        this.publicRequests = new IdempotencyService(idempotency, json);
    }

    public record CompanyOption(String id, String name) {}
    public record Receipt(String id, String registrationStatus) {}

    public List<CompanyOption> companies() {
        List<CompanyOption> options = platform.listBusinessEntities().stream().filter(e -> "active".equals(e.status()) && COMPANY_ROLES.contains(e.entityRole()))
                .map(e -> new CompanyOption(e.id(),e.entityName())).toList();
        audit.recordSuccess("staff.registration_companies_viewed", null, "business_entity", "all", null, null, Map.of("count", options.size()));
        return options;
    }

    @Transactional
    public Receipt selfRegister(StaffSelfRegistrationRequest request) {
        try {
        if (request == null) throw ApiException.badRequest("VALIDATION_FAILED", "须填写注册资料");
        String name = requireText(request.name(), "员工姓名");
        if (name.length()>128 || (request.phone()!=null && request.phone().length()>64)
                || (request.idDoc()!=null && request.idDoc().length()>128)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "注册资料长度超出限制");
        }
        // U05-003/U06-002：公开入口的唯一身份证明是该私人手机安装时生成的设备公钥/指纹（不读硬件识别码）；缺失或不符即拒。
        String devicePublicKey = requireText(request.devicePublicKey(), "私人手机设备公钥");
        String deviceFingerprint = requireText(request.deviceFingerprintHash(), "私人手机设备指纹");
        byte[] der;
        try {
            der = java.util.Base64.getDecoder().decode(devicePublicKey);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("DEVICE_PROOF_INVALID", "设备公钥须为 Base64 编码的 X.509 公钥");
        }
        if (der.length == 0 || !deviceFingerprint.equalsIgnoreCase(sha256Hex(der))) {
            throw ApiException.badRequest("DEVICE_PROOF_INVALID", "设备指纹与设备公钥不符");
        }
        String companyId = requireText(request.companyId(), "所属公司");
        if (companies().stream().noneMatch(c -> c.id().equals(companyId))) {
            throw ApiException.badRequest("COMPANY_NOT_FOUND", "所属公司不存在或已停用");
        }
        String merchantId = trimToNull(request.merchantId());
        if (merchantId != null) {
            MerchantRecord merchant = requireMerchant(merchantId);
            if (!"self_operated".equals(merchant.cooperationForm()) || companyChain(merchant).stream().noneMatch(c -> c.id().equals(companyId))) {
                throw ApiException.badRequest("COMPANY_SELF_REGISTRATION_REQUIRED", "该商户不属于所选公司的自营商户");
            }
        }
        return publicRequests.execute("publicStaffSelfRegistration", request.idempotencyKey(), request, () -> {
            // 同一指纹只能绑定一名员工；放在幂等执行内，同一请求重放仍返回原申请编号。
            if (staff.isFingerprintBound(deviceFingerprint)) {
                throw ApiException.conflict("DEVICE_ALREADY_BOUND", "该私人手机已绑定其他员工，不能重复注册");
            }
            Instant now = Instant.now();
            var profile = new StaffProfileRecord("staff_"+UUID.randomUUID(),projectOf(companyId),merchantId,null,name,
                    trimToNull(request.phone()),trimToNull(request.idDoc()),null,"submitted",null,null,null,"active",now,now)
                    .withCompanyRoute(companyId,"company_self");
            staff.saveProfile(profile);
            // 安装即绑定：首次私人手机绑定直接生效（T1 当前实现），审批通过后即可按该凭证验签。
            staff.saveCredential(new StaffDeviceCredentialRecord("scred_"+UUID.randomUUID(), profile.id(), devicePublicKey,
                    deviceFingerprint.toLowerCase(java.util.Locale.ROOT), "active", null, "trusted", null, now, null, now, now));
            audit.recordSuccess("staff.self_registered",null,"staff_profile",profile.id(),null,request.idempotencyKey(),
                    Map.of("company_id",companyId,"registration_source","company_self","device_fingerprint_hash",deviceFingerprint));
            return new Receipt(profile.id(),profile.registrationStatus());
        },Receipt.class);
        } catch (RuntimeException exception) {
            audit.recordFailure("staff.self_register_failed", null, "staff_profile", null, null, request == null ? null : request.idempotencyKey(), exception instanceof ApiException api ? api.code() : exception.getClass().getSimpleName());
            throw exception;
        }
    }

    String actorCompany(AccessActor actor) {
        String ownCompany = staff.findProfileByUserId(actor.id()).map(StaffProfileRecord::companyId).orElse(null);
        if (ownCompany != null) return ownCompany;
        return access.findUserPositionAssignment(actor.id()).map(p -> trimToNull(p.organizationId()))
                .orElseGet(() -> requireText(actor.organizationId(), "所属公司"));
    }

    String merchantApprovingCompany(String merchantId) {
        MerchantRecord merchant = requireMerchant(requireText(merchantId,"商户"));
        if ("self_operated".equals(merchant.cooperationForm())) throw ApiException.badRequest("COMPANY_SELF_REGISTRATION_REQUIRED", "自营商户员工须在私人手机自助注册");
        // main 无主体父链（V410 已删 parent_entity_id，PGM-018/019 三角色多对多）：审批域公司＝商户当前生效的经营主体公司。
        return companyChain(merchant).getFirst().id();
    }

    private String projectOf(String companyId) {
        // 员工档案 project_id 与既有 requireProjectScope（actor.organizationId）同义＝所属公司本身；main 无父链可上溯。
        return platform.listBusinessEntities().stream()
                .filter(e -> e.id().equals(companyId) && "active".equals(e.status()) && COMPANY_ROLES.contains(e.entityRole()))
                .map(BusinessEntityRecord::id).findFirst()
                .orElseThrow(() -> ApiException.conflict("REGISTRATION_PROJECT_UNRESOLVED", "所属公司不存在、已停用或不是经营主体公司"));
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String requireText(String value, String label) {
        String result=trimToNull(value);
        if (result==null) throw ApiException.badRequest("VALIDATION_FAILED", "请填写"+label);
        return result;
    }

    private MerchantRecord requireMerchant(String id) {
        return platform.findMerchantById(id).filter(m -> "active".equals(m.status()) && m.archivedAt()==null && !"approved".equals(m.deleteStatus()))
                .orElseThrow(() -> ApiException.badRequest("MERCHANT_NOT_FOUND", "商户不存在、已停用或已存档"));
    }

    private List<BusinessEntityRecord> companyChain(MerchantRecord merchant) {
        // 商户的经营主体公司＝生效绑定的运营主体，否则档案上的 business_entity_id；单层，不再沿已删除的父链向上收集。
        String id = platform.findActiveBusinessEntityBindingByMerchantId(merchant.id())
                .map(b -> b.operationEntityId()).orElse(merchant.businessEntityId());
        List<BusinessEntityRecord> companies = platform.listBusinessEntities().stream()
                .filter(e -> e.id().equals(id) && "active".equals(e.status()) && COMPANY_ROLES.contains(e.entityRole())).toList();
        if (companies.isEmpty()) throw ApiException.conflict("REGISTRATION_COMPANY_UNRESOLVED", "该商户尚未维护有效的经营主体公司归属");
        return companies;
    }
}

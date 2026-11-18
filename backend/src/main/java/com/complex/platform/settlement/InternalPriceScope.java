package com.complex.platform.settlement;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.settlement.JdbcSettlementConfigWriteRepository.CatalogEntryRow;
import com.complex.platform.settlement.SettlementConfigWriteRequests.InternalPriceItemRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 第十八批 AW 段 291（ENUM:54「实际提供商品或服务的主体提交，对应域主管公司审批，提交人与审批人分离：酒吧由娱乐平台公司审批，游乐由游乐公司审批，
 * 演出传媒由传媒公司审批，中央餐饮供应的酒水或小食由中央餐饮公司内部差人审批；餐饮与零售商户……统一由城市项目公司审批……业态为“其他”的商户，
 * 由其商户档案上指定的主管公司审批」；PGM:432「由公司内部提交人与审批人分离审批」；BSM:576「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批」）：
 * 内部结算价的提交人与审批人范围。人员所属公司取法同 TicketRewardConfigService，域主管公司取法同 SettlementExpiryJob 到期归属。
 */
@Component
public class InternalPriceScope {

    /** 内部结算价提交（V662，高敏、授出前审批）。 */
    public static final String SUBMIT = "settlement.internal_price.submit";

    private final JdbcTemplate jdbc;
    private final AuthorizationService auth;
    private final RevenueStreamAttributionRepository attributions;
    private final JdbcSettlementTransactionRepository transactions;

    public InternalPriceScope(JdbcTemplate jdbc, AuthorizationService auth, RevenueStreamAttributionRepository attributions,
                              JdbcSettlementTransactionRepository transactions) {
        this.jdbc = jdbc;
        this.auth = auth;
        this.attributions = attributions;
        this.transactions = transactions;
    }

    /**
     * 291-R2：只能由实际提供方提交——商户账号只报本商户、主体人员只报本公司；返回是否最高权限本人（提交即生效）。
     */
    public boolean requireSubmitter(InternalPriceItemRequest request) {
        auth.requirePermission(SUBMIT);
        AccessActor actor = auth.currentActor();
        if (auth.isMasterAuthority(actor)) {
            return true;
        }
        String merchant = request == null ? null : trimToNull(request.merchantId());
        String entity = request == null ? null : trimToNull(request.providerEntityId());
        Optional<String> own = auth.currentMerchantIdQuietly();
        boolean provider = own.isPresent()
                ? own.get().equals(merchant) && entity == null
                : merchant == null && entity != null && entity.equals(companyOf(actor.id()));
        if (!provider) {
            throw ApiException.forbidden("INTERNAL_PRICE_SUBMITTER_SCOPE",
                    "内部结算价只能由实际提供商品或服务的主体提交：商户人员只能报本商户的价格，经营主体人员只能报本公司提供的价格");
        }
        return false;
    }

    /** 291-R1：转正须由对应域主管公司的人审批；最高权限本人放行。 */
    public void requireApprover(CatalogEntryRow draft, String actorId) {
        if (auth.isMasterAuthority(auth.currentActor())) {
            return;
        }
        String company = approvingCompany(draft);
        if (!company.equals(companyOf(actorId))) {
            List<String> names = jdbc.queryForList("SELECT entity_name FROM business_entities WHERE id = ?", String.class, company);
            throw ApiException.forbidden("INTERNAL_PRICE_APPROVER_SCOPE",
                    "内部结算价须由对应域主管公司审批：本条目应由「" + (names.isEmpty() ? company : names.getFirst()) + "」的人员审批");
        }
    }

    /** 提供主体自己提供的（中央餐饮、城市项目公司自营停车等）由该主体内部差人审批；商户按业态定域主管公司。 */
    private String approvingCompany(CatalogEntryRow draft) {
        if (draft.providerEntityId() != null) {
            return draft.providerEntityId();
        }
        Map<String, Object> m = jdbc.queryForMap("SELECT merchant_type, project_id, supervising_company_id FROM merchants WHERE id = ?",
                draft.merchantId());
        String project = (String) m.get("project_id");
        String company = switch (String.valueOf(m.get("merchant_type"))) {
            case "catering", "retail" -> {
                List<String> companies = transactions.findProjectCompanies(project);
                if (companies.size() != 1) {
                    throw ApiException.conflict("CITY_PROJECT_COMPANY_MISSING",
                            "定不出这条内部结算价该由哪家公司审批：餐饮与零售商户由该项目城市项目公司审批，项目「" + project
                                    + "」有效城市项目公司" + companies.size() + "家，须恰好一家；请先在主体档案关联");
                }
                yield companies.getFirst();
            }
            case "other" -> (String) m.get("supervising_company_id");
            case "amusement" -> attributions.resolveManagementEntity(project, "amusement_ticket");
            case "media" -> attributions.resolveManagementEntity(project, "performance_ticket");
            default -> attributions.resolveManagementEntity(project, TicketDistributionSettlementService.REVENUE_STREAM_ALL_PASS_TICKET);
        };
        if (company == null) {
            throw ApiException.conflict("REVENUE_STREAM_ATTRIBUTION_MISSING",
                    "定不出这条内部结算价该由哪家公司审批：商户「" + draft.merchantId() + "」业态的域主管公司未配置（收入归属或商户档案主管公司）；请先补齐");
        }
        return company;
    }

    /** 人员所属公司：在职已审员工档案公司，否则任职机构，否则账号机构（同 TicketRewardConfigService.requireApproverScope）。 */
    private String companyOf(String userId) {
        List<String> companies = jdbc.queryForList("SELECT COALESCE((SELECT company_id FROM staff_profiles WHERE user_id=u.id AND status='active'"
                + " AND registration_status='approved' ORDER BY updated_at DESC LIMIT 1),(SELECT organization_id FROM user_position_assignments"
                + " WHERE user_id=u.id AND assigned_at<=now()),u.organization_id) FROM access_users u WHERE u.id=? AND u.status='active'"
                + " AND NOT EXISTS(SELECT 1 FROM staff_profiles s WHERE s.user_id=u.id AND (s.status<>'active' OR s.registration_status<>'approved'))",
                String.class, userId);
        return companies.size() == 1 ? companies.getFirst() : null;
    }
}

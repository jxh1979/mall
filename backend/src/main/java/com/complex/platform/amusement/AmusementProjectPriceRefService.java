package com.complex.platform.amusement;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.ticketing.InternalSettlementPriceResolver;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 BR 段第 357 条 357-R1（BSM:753「游乐权益核销的收入按具体的游乐项目归属……游乐公司再按已批准的内部结算价或域内规则，与实际提供服务的游乐商户结算。」；
 * ENUM:53「采用哪种规则由域主管公司决定，商户不能决定。」；BSM:1673「商户只能提交资料、履约事实和异议，不能自行决定域内分配方式。」；PGM:430「游乐公司负责……内部结算
 * 审批与域内分配」；ENUM:54「任何商户在没有获批内部结算价之前，不得为其开通需要该价格结算的权益或券。」）：游乐公司为游乐项目引用所属商户的内部结算价目录条目。
 * 有商户范围的账号一律拒绝；所填条目须绑定该项目所属商户且已有获批生效价（一次 {@link InternalSettlementPriceResolver#resolveVersioned} 同判三项），
 * 项目没有所属商户（游乐公司自营）同样不能引用。之后该项目的游乐入场按此条目的当时生效价逐笔与所属商户结算（settlement.AmusementDomainSettlement）。
 */
@Service
public class AmusementProjectPriceRefService {

    static final String PERMISSION = "amusement.session.manage";
    static final String EVENT = "amusement.project.internal_price_ref.updated";
    static final String FAILED_EVENT = "amusement.project.internal_price_ref.update_failed";

    /** PUT /api/v1/amusement/projects/{id}/internal-price-item 请求体（蛇形键 idempotency_key、catalog_item_code）。 */
    public record Request(String idempotencyKey, String catalogItemCode) {
    }

    private final AmusementRepository repository;
    private final JdbcTemplate jdbc;
    private final InternalSettlementPriceResolver prices;
    private final AuthorizationService auth;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;

    public AmusementProjectPriceRefService(AmusementRepository repository, JdbcTemplate jdbc, InternalSettlementPriceResolver prices,
            AuthorizationService auth, IdempotencyService idempotency, AuditEventService audit) {
        this.repository = repository;
        this.jdbc = jdbc;
        this.prices = prices;
        this.auth = auth;
        this.idempotency = idempotency;
        this.audit = audit;
    }

    @com.complex.platform.gate.GuardedAction(capability = PERMISSION)
    @Transactional
    public AmusementProjectRecord setInternalPriceItem(String projectId, Request request) {
        String id = requireText(projectId, "project_id");
        String key = request == null ? null : request.idempotencyKey();
        try {
            // 范围闸在幂等之前：商户账号的请求不得借重放拿到游乐公司设置的结果。
            if (auth.currentMerchantScope().isPresent()) {
                throw ApiException.forbidden("AMUSEMENT_SETTLEMENT_RULE_DOMAIN_COMPANY_ONLY", "采用哪种结算规则由游乐公司决定，商户不能决定");
            }
            return write(id, key, request);
        } catch (RuntimeException exception) {
            audit.recordFailure(FAILED_EVENT, PERMISSION, "amusement_project", id, null, key,
                    exception instanceof ApiException api ? api.code() : "INTERNAL_ERROR");
            throw exception;
        }
    }

    private AmusementProjectRecord write(String id, String key, Request request) {
        return idempotency.execute("setAmusementProjectInternalPriceItem:" + id, key, request, () -> {
            AmusementProjectRecord project = repository.findProjectById(id)
                    .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "游乐项目不存在或已被删除"));
            String code = trimToNull(request.catalogItemCode());
            if (prices.resolveVersioned(null, project.merchantId(), code).isEmpty()) {
                throw ApiException.badRequest("AMUSEMENT_PRICE_ITEM_UNUSABLE",
                        "所填目录条目不存在、不属于本项目所属商户，或还没有获批生效的内部结算价");
            }
            jdbc.update("UPDATE amusement_projects SET catalog_item_code = ?, updated_at = now() WHERE id = ?", code, id);
            audit.recordSuccess(EVENT, PERMISSION, "amusement_project", id, null, key, Map.of("catalog_item_code", code));
            return repository.findProjectById(id).orElseThrow();
        }, AmusementProjectRecord.class);
    }
}

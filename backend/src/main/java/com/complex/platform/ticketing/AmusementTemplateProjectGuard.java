package com.complex.platform.ticketing;

// 文件版本：V0.01

import com.complex.platform.common.Strings;
import com.complex.platform.common.api.ApiException;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 第十八批 CC 段第 399 条 399-R1（BSM:784「入场、游乐（指定项目/次数/场次）……」；BSM:1033「通票中的酒水、小食和其他权益必须分别绑定实际提供方」；
 * BSM:1754「每项权益都必须绑定实际提供方和审批版本，避免悬空账。」）：游乐类权益模板必须指定游乐项目；该项目已登记经营商户的，模板提供方商户必须是该商户；
 * 没有登记的（第十八批 BR 段口径：游乐公司自营）沿用所选提供方。非游乐类型不校验。
 */
@Component
public class AmusementTemplateProjectGuard {

    private final JdbcTemplate jdbc;

    public AmusementTemplateProjectGuard(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 权益类型以 amusement 开头（同 AmusementService.isAmusementEntitlementType）时：指定项目必填、须存在，项目已登记经营商户的须等于提供方商户。 */
    public void requireProjectProvider(String entitlementType, String amusementProjectId, String merchantId) {
        if (entitlementType == null || !entitlementType.trim().toLowerCase(Locale.ROOT).startsWith("amusement")) {
            return;
        }
        String projectId = Strings.requireText(amusementProjectId, "amusement_project_id");
        List<String> owners = jdbc.queryForList("SELECT merchant_id FROM amusement_projects WHERE id = ?", String.class, projectId);
        if (owners.isEmpty()) {
            throw ApiException.badRequest("AMUSEMENT_PROJECT_NOT_FOUND", "所指定的游乐项目不存在");
        }
        String owner = owners.get(0);
        if (owner != null && !owner.equals(merchantId)) {
            throw ApiException.badRequest("AMUSEMENT_PROJECT_MERCHANT_MISMATCH", "游乐权益的提供方商户必须是所指定游乐项目的经营商户");
        }
    }
}

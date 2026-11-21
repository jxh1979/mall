package com.complex.platform.access.staff;

// 文件版本：V0.01

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 第十八批 BL 段第 390 条 390-R1（BSM:1474「主管排班：排到几点几分，可指定当天工作点位（比如某个服务区、某片保洁区域）」；
 * BSM:1077「主责服务提供人按当日该场次排班指派到该卡座」）：排班可选点位——只列启用中的空间，范围按排班员：最高权限与平台管理员全部
 * （第十八批裁决 71），挂商户（含派驻到一家酒吧）的取本商户名下，其余取 staff.shift.manage 可触及项目下的。
 */
@Service
public class StaffShiftPointOptionService {

    private static final String CODE = "staff.shift.manage";
    private static final String ACTIVE = "SELECT id, space_name FROM spaces WHERE status = 'active'";
    private static final String ORDER = " ORDER BY space_name, id";
    private static final RowMapper<Option> ROW = (rs, n) -> new Option(rs.getString("id"), rs.getString("space_name"));

    /** 可选点位一项。 */
    public record Option(String id, String spaceName) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final AuthorizationService authorizationService;

    public StaffShiftPointOptionService(NamedParameterJdbcTemplate jdbc, AuthorizationService authorizationService) {
        this.jdbc = jdbc;
        this.authorizationService = authorizationService;
    }

    public List<Option> options() {
        authorizationService.requirePermission(CODE);
        AccessActor actor = authorizationService.currentActor();
        if (authorizationService.isMasterAuthority(actor) || authorizationService.isPlatformAdmin(actor)) {
            return jdbc.query(ACTIVE + ORDER, ROW);
        }
        Optional<String> merchant = authorizationService.currentMerchantIdQuietly();
        if (merchant.isPresent()) {
            return jdbc.query(ACTIVE + " AND merchant_id = :merchant_id" + ORDER, Map.of("merchant_id", merchant.get()), ROW);
        }
        Set<String> projects = authorizationService.projectReach(actor, CODE).organizationIds();
        if (projects.isEmpty()) {
            return List.of();
        }
        return jdbc.query(ACTIVE + " AND project_id IN (:project_ids)" + ORDER, Map.of("project_ids", projects), ROW);
    }
}

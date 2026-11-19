package com.complex.platform.settlement;

// 文件版本：V0.01

import com.complex.platform.ticketing.SingleBarServiceFeeConfigPort;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 第十八批 AW 段 294-R1（BSM:755「票种发布前必须先选定按比例还是按固定金额并填入数值，未选定或未填写的票种不得发布上架」）：单一酒吧门票发布闸用。
 * 有一行「本项目、本酒吧、票种为空或为本票种、生效且已转正」的综合服务费即算已选定方式并填值（比例与固定额二选一且有值由 V328 CHECK 与写入口保证，
 * 填 0 也算填了）；项目默认行、全平台默认行不算（「不自动继承酒吧通票的默认比例」）。
 */
@Component
public class SingleBarServiceFeeConfigReader implements SingleBarServiceFeeConfigPort {

    private final JdbcTemplate jdbc;

    public SingleBarServiceFeeConfigReader(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<String> unconfiguredProjects(String barId, String ticketTypeId, List<String> projectIds) {
        return projectIds.stream()
                .filter(project -> !Boolean.TRUE.equals(jdbc.queryForObject("""
                        SELECT EXISTS (SELECT 1 FROM comprehensive_service_fee_configs
                         WHERE status = 'active' AND is_draft = FALSE AND project_id = ? AND merchant_id = ?
                           AND (ticket_type_id IS NULL OR ticket_type_id = ?))
                        """, Boolean.class, project, barId, ticketTypeId)))
                .map(project -> jdbc.queryForList("SELECT name FROM projects WHERE id = ?", String.class, project).stream()
                        .findFirst().orElse(project))
                .toList();
    }
}

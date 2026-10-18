package com.complex.platform.support;

// 文件版本：V0.01

import com.complex.platform.customer.TicketRewardConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** 售票链回归的既有已批准合同前置事实；合同审批过程另由奖励配置集成测试验证。 */
public final class TicketSalesContractTestFixtures {
    private TicketSalesContractTestFixtures() {}

    public static String approvedPublished(JdbcTemplate jdbc, ObjectMapper json, String ticketType,
            String project, String operationMerchant) {
        String entity="tsc_entity_"+UUID.nameUUIDFromBytes(project.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'售票夹具项目公司','project_company','active',now(),now()) ON CONFLICT(id) DO NOTHING",entity);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?) ON CONFLICT DO NOTHING",entity,project);
        jdbc.update("UPDATE merchants SET business_entity_id=?,contract_entity_id=? WHERE id=?",entity,entity,operationMerchant);
        String sellerMerchant=jdbc.query("SELECT bar_id FROM ticket_types WHERE id=?",(rs,n)->rs.getString(1),ticketType).getFirst();
        TicketRewardConfig.Values values=new TicketRewardConfig.Values(null,null,null,null,null,null,null,null,null,
                null,entity,sellerMerchant,"TEST-CONTRACT-"+ticketType,List.of());
        Instant effective=Instant.now().minusSeconds(60);
        try {
            jdbc.update("INSERT INTO ticket_reward_config_versions(id,kind,ticket_type_id,project_id,version,payload,effective_at,published_at,status,created_by,created_at,approved_at) VALUES(?,'sales_contract',?,?,'V0.01',?::jsonb,?,?,'approved','sales-contract-fixture',?,?)",
                    "trc_"+UUID.randomUUID().toString().replace("-", ""),ticketType,project,json.writeValueAsString(values),
                    Timestamp.from(effective),Timestamp.from(effective.minusSeconds(60)),Timestamp.from(effective.minusSeconds(120)),Timestamp.from(effective.minusSeconds(90)));
        } catch(JsonProcessingException exception) {
            throw new IllegalStateException("销售合同夹具序列化失败",exception);
        }
        return entity;
    }
}

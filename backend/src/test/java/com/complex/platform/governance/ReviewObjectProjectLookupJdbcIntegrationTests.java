package com.complex.platform.governance;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.governance.review.SupervisorReviewRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 F 段第 83 条：复核单所处理对象所属项目的取数（{@link JdbcPlatformRepository#reviewObjectProjectIds}）在真库表结构上跑通。
 * 依据：裁决 17 所选选项原文「对象有所属项目的，普通查看人按对象项目判；对象没有项目的（全平台或全国配置、手填对象、没带项目的合同）照旧按发起人」；
 * 治理地图第 659 行「商户在多个项目经营的，各经营项目的员工和主项目的员工一样，可以修改、存档、删除这份商户主档案。」
 *
 * <p>夹具随测试事务回滚，编号带随机后缀。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class ReviewObjectProjectLookupJdbcIntegrationTests {

    @Autowired private SupervisorReviewRepository repository;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);

    // 每个登记的对象类型的取数 SQL 都对真实表结构执行：查不到的对象不抛错、返回空（兜住表名列名写错）；未登记的类型返回空
    @Test
    void reviewObjectProjectLookupsRunAgainstRealSchema() {
        String missing = "none_g83_" + suffix;
        for (String type : JdbcPlatformRepository.REVIEW_OBJECT_PROJECT_SQL.keySet()) {
            assertThat(repository.reviewObjectProjectIds(type, missing)).as(type).isEmpty();
        }
        assertThat(repository.reviewObjectProjectIds("merchant_project_request", missing)).isEmpty();
        assertThat(repository.reviewObjectProjectIds("unregistered_g83", missing)).isEmpty();
    }

    // 商户加经营项目申请取商户的主项目与已批准的经营项目（待批的新项目不算）；人工补录单取补录单上的项目。修复前红：返回空
    @Test
    void merchantProjectRequestAndManualBackfillResolveTheirProjects() {
        String b = project("b");
        String c = project("c");
        String d = project("d");
        String merchant = "mch_g83_" + suffix;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id) "
                + "VALUES (?, 'G83 商户', 'catering', 'active', now(), now(), ?)", merchant, b);
        jdbc.update("INSERT INTO merchant_projects (merchant_id, project_id) VALUES (?, ?)", merchant, c);
        String request = "mpr_g83_" + suffix;
        jdbc.update("INSERT INTO merchant_project_requests (id, merchant_id, project_id, reason, requested_by, review_id) "
                + "VALUES (?, ?, ?, 'G83 加经营项目', 'usr_g83', ?)", request, merchant, d, "review_g83_mpr_" + suffix);
        assertThat(repository.reviewObjectProjectIds("merchant_project_request", request)).containsExactlyInAnyOrder(b, c);

        String backfill = "mbf_g83_" + suffix;
        jdbc.update("""
                INSERT INTO manual_backfills (id, target_type, target_id, project_id, business_at, occurred_at, proposed, original_snapshot,
                        reason, responsible_staff_id, evidence_attachment_ids, review_id, submitted_by, submitted_at)
                VALUES (?, 'trace_handover_step', ?, ?, now(), now(), '{}'::jsonb, '{}'::jsonb, 'G83 补录', 'usr_g83', '["ev_g83"]'::jsonb,
                        ?, 'usr_g83', now())
                """, backfill, "trs_g83_" + suffix, b, "review_g83_mbf_" + suffix);
        assertThat(repository.reviewObjectProjectIds("manual_backfill", backfill)).containsExactly(b);
    }

    private String project(String tag) {
        String id = "prj_g83_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                id, "G83 " + id);
        return id;
    }
}

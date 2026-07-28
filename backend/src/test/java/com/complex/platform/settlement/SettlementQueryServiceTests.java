package com.complex.platform.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.common.api.PageResponse;
import com.complex.platform.settlement.JdbcSettlementTransactionRepository.SettlementTransactionView;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * 平台侧结算审阅只读查询服务单测(无 DB,不碰 complex 库):校验分页钳制与空白→null 透传。
 * 用可覆写的仓储桩捕获传入仓储的参数,断言 service 的边界处理(page/size 钳制、blank→null)。
 */
class SettlementQueryServiceTests {

    /** 桩:记录 service 传入的过滤/分页参数,返回可控行数。 */
    static final class CapturingRepo extends JdbcSettlementTransactionRepository {
        String project;
        String merchant;
        int limit;
        int offset;
        long total = 42L;

        CapturingRepo() {
            super((NamedParameterJdbcTemplate) null);
        }

        @Override
        public long countVerificationSettled(String projectId, String merchantId) {
            this.project = projectId;
            this.merchant = merchantId;
            return total;
        }

        @Override
        public List<SettlementTransactionView> listVerificationSettled(
                String projectId, String merchantId, int limit, int offset) {
            this.limit = limit;
            this.offset = offset;
            return List.of(new SettlementTransactionView(
                    "st_1", "STX-1", "prj_a", "mch_a", null, "tt_1", "entry", "merchant",
                    3, 1, 15000L, "settled", Instant.now()));
        }
    }

    @Test
    void clampsPageAndSizeAndComputesOffset() {
        CapturingRepo repo = new CapturingRepo();
        SettlementQueryService service = new SettlementQueryService(repo, null);

        PageResponse<SettlementTransactionView> page = service.listSettlements("prj_a", null, 3, 20);

        assertThat(repo.limit).isEqualTo(20);
        assertThat(repo.offset).isEqualTo(40); // (3-1)*20
        assertThat(page.page()).isEqualTo(3);
        assertThat(page.pageSize()).isEqualTo(20);
        assertThat(page.total()).isEqualTo(42L);
        assertThat(page.items()).hasSize(1);
    }

    @Test
    void clampsNonPositivePageAndOversizeAndBlankFilters() {
        CapturingRepo repo = new CapturingRepo();
        SettlementQueryService service = new SettlementQueryService(repo, null);

        service.listSettlements("  ", "   ", 0, 9999);

        assertThat(repo.limit).isEqualTo(100); // size clamped to MAX_SIZE
        assertThat(repo.offset).isEqualTo(0);  // page clamped to 1
        assertThat(repo.project).isNull();     // blank -> null (no filter)
        assertThat(repo.merchant).isNull();
    }
}

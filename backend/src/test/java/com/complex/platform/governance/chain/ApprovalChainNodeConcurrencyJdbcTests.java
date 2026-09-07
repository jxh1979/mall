package com.complex.platform.governance.chain;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.governance.review.SupervisorReviewRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * M087-I5-06（PGM-1.7.1「主线与各条平行线全部通过……才生效」）：两条线的末节点并发通过时，整单终态必须能从节点表归约为「全部通过」。
 * 仓储层交错实验，次序与 SupervisorReviewService.approve 相同（锁复核单 → 节点比较并交换 → 读整链归约）：
 * 没有复核单行锁时两个事务互相看不见对方未提交的裁决，都得出「未全部通过」——单据永久待审；
 * 有行锁时后到者等先到者提交后再裁决、再读整链，必得「全部通过」。真实 complex_test 库上执行。
 * supervisor_review_decisions 是只增不删的留痕表（complex_block_mutation 触发器），故每次用唯一编号、不清理。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class ApprovalChainNodeConcurrencyJdbcTests {

    private final String reviewId = "srv_m087_i5_06_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

    @Autowired
    private SupervisorReviewRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void seed() {
        jdbcTemplate.update("""
                INSERT INTO supervisor_review_decisions (review_id, policy_code, action_type, object_type, object_id,
                                                         blocked, review_status, required_level, evaluated_at, initiated_by)
                VALUES (?, 'tax_rule_adjustment_l3', 'finance.tax_rule.adjust', 'tax_rule', ?,
                        true, 'pending_review', 3, now(), 'usr_m087_init')
                """, reviewId, "tr_" + reviewId);
        repository.saveChainNodes(reviewId, List.of(
                new ApprovalChainNode(reviewId + ":main:1", reviewId, ApprovalChainNode.MAIN_LINE, 1, ApprovalChainNode.KIND_USER,
                        "usr_m087_a", ApprovalChainNode.PENDING, null, null, null, null, 0),
                new ApprovalChainNode(reviewId + ":finance_review:1", reviewId, "finance_review", 1, ApprovalChainNode.KIND_USER,
                        "usr_m087_b", ApprovalChainNode.PENDING, null, null, null, null, 0)));
    }

    @Test
    void concurrentLastNodesOfTwoLinesStillReduceToAllApproved() throws Exception {
        List<ApprovalChainNode> nodes = repository.listChainNodes(reviewId);
        assertThat(nodes).hasSize(2);
        CountDownLatch firstDecided = new CountDownLatch(1);
        CountDownLatch secondDecided = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = pool.submit(() -> decideAndReduce(nodes.get(0), firstDecided, secondDecided));
            Future<Boolean> second = pool.submit(() -> decideAndReduce(nodes.get(1), secondDecided, firstDecided));
            boolean anyReducedToAllApproved = first.get(30, TimeUnit.SECONDS) | second.get(30, TimeUnit.SECONDS);
            assertThat(repository.listChainNodes(reviewId)).allMatch(node -> ApprovalChainNode.APPROVED.equals(node.status()));
            assertThat(anyReducedToAllApproved)
                    .as("至少一个裁决事务必须看到整链全部通过，否则节点全过而单据永久待审")
                    .isTrue();
        } finally {
            pool.shutdownNow();
        }
    }

    /** 与 SupervisorReviewService.approve 相同次序：锁复核单 → 节点比较并交换 → 等对方也裁决（最多 2 秒）→ 读整链归约。 */
    private boolean decideAndReduce(ApprovalChainNode node, CountDownLatch mine, CountDownLatch theirs) {
        return Boolean.TRUE.equals(new TransactionTemplate(transactionManager).execute(status -> {
            repository.lockSupervisorReview(reviewId);
            assertThat(repository.updateChainNode(node.withStatus(ApprovalChainNode.APPROVED, node.approverId(), Instant.now()))).isTrue();
            mine.countDown();
            try {
                theirs.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            return ApprovalChainService.allApproved(repository.listChainNodes(reviewId));
        }));
    }
}

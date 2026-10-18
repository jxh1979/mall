// 文件版本：V0.01
package com.complex.platform.compliance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.auth.MasterAuthorityService;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.compliance.WecomCleanupPreview.Item;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@Transactional
class WecomCleanupJdbcIntegrationTests {
    private static final KeyPair KEY = key();
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("complex.auth.master.public-key",
                () -> Base64.getEncoder().encodeToString(KEY.getPublic().getEncoded()));
    }

    @Autowired WecomCleanupService service;
    @Autowired MasterAuthorityService master;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @MockitoSpyBean JdbcWecomCleanupRepository repository;
    @MockitoBean AuthorizationService authorization;
    private String prefix;
    private String conversation;
    private String message;
    private String hit;
    private String alert;

    @BeforeEach
    void setUp() {
        ((java.util.Map<?, ?>) org.springframework.test.util.ReflectionTestUtils.getField(master, "windows")).clear();
        ((java.util.Map<?, ?>) org.springframework.test.util.ReflectionTestUtils.getField(master, "actionChallenges")).clear();
        prefix = "wct_" + UUID.randomUUID();
        conversation = prefix + "_c";
        message = prefix + "_m";
        hit = prefix + "_h";
        alert = prefix + "_a";
        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active");
        when(authorization.currentActor()).thenReturn(actor);
        when(authorization.isMasterAuthority(actor)).thenReturn(true);
        jdbc.update("""
                INSERT INTO wecom_conversations (id, external_conversation_id, project_id)
                VALUES (?, ?, 'proj_cleanup')
                """, conversation, conversation);
        insertMessage(message);
        jdbc.update("""
                INSERT INTO analytics_risk_alerts
                    (id, rule_id, alert_type, risk_level, object_type, object_id, summary, status, source_domain, created_at, updated_at)
                VALUES (?, 'rule_wecom_abuse', 'wecom', 'high', 'wecom_message', ?, '清理夹具告警', 'open', 'wecom', now(), now())
                """, alert, message);
        insertHit(hit, message);
        jdbc.update("""
                INSERT INTO wecom_access_audits (id, conversation_id, viewer_user_id, view_type)
                VALUES (?, ?, ?, 'redacted')
                """, prefix + "_view", conversation, SessionIssuer.MASTER_USER_ID);
    }

    @Test
    void emptyCleanupHistoryReturnsAnEmptyArray() throws Exception {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM wecom_cleanup_previews", Integer.class)).isZero();
        MockMvcBuilders.standaloneSetup(new WecomCleanupController(service)).build()
                .perform(get("/api/v1/compliance/wecom/cleanup-previews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void cleanupHistoryReturnsAll201RealPreviewsWithoutPageLimits() throws Exception {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM wecom_cleanup_previews", Integer.class)).isZero();
        List<String> expectedIds = new ArrayList<>();
        for (int i = 0; i < 201; i++) {
            expectedIds.add(preview(message).id());
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM wecom_cleanup_previews", Integer.class)).isEqualTo(201);
        var response = MockMvcBuilders.standaloneSetup(new WecomCleanupController(service)).build()
                .perform(get("/api/v1/compliance/wecom/cleanup-previews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(201))
                .andReturn().getResponse();
        List<String> returnedIds = new ArrayList<>();
        new ObjectMapper().readTree(response.getContentAsString()).get("data")
                .forEach(preview -> returnedIds.add(preview.get("id").asText()));
        assertThat(returnedIds).containsExactlyInAnyOrderElementsOf(expectedIds);
    }

    @Test
    void previewIsExactAndEvidenceAndUnrelatedRecordsSurviveCleanupAndRetry() throws Exception {
        var preview = preview(message);
        assertThat(preview.messageCount()).isEqualTo(1);
        assertThat(preview.riskHitCount()).isEqualTo(1);
        assertThat(preview.candidates().get(0).riskHits().get(0).hitId()).isEqualTo(hit);
        String later = prefix + "_later";
        insertMessage(later);
        insertHit(prefix + "_laterhit", later);
        var proof = proof(preview.id());
        var result = service.execute(preview.id(), proof);
        assertThat(count("wecom_messages", "id", message)).isZero();
        assertThat(count("wecom_risk_hits", "hit_id", hit)).isZero();
        assertThat(count("wecom_messages", "id", later)).isEqualTo(1);
        assertThat(count("wecom_risk_hits", "hit_id", prefix + "_laterhit")).isEqualTo(1);
        assertThat(count("analytics_risk_alerts", "id", alert)).isEqualTo(1);
        assertThat(count("wecom_access_audits", "id", prefix + "_view")).isEqualTo(1);
        assertThat(result.executedAt()).isNotNull();
        assertThat(result.executedBy()).isEqualTo(SessionIssuer.MASTER_USER_ID);
        assertThat(result.keyFingerprint()).isNotBlank();
        assertThat(jdbc.queryForObject("SELECT candidates::text FROM wecom_cleanup_previews WHERE id = ?",
                String.class, preview.id())).contains("测试清理理由").doesNotContain("原文秘密标记");
        assertThat(service.execute(preview.id(), proof)).isEqualTo(result);
        assertThat(service.get(preview.id())).isEqualTo(result);
    }

    @Test
    void ordinaryAdminAndUnsignedMasterCannotDelete() {
        var preview = preview(message);
        assertThatThrownBy(() -> service.execute(preview.id(), new WecomCleanupPreview.ExecuteRequest(null, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("MASTER_ACTION_CHALLENGE_INVALID"));
        AccessActor ordinary = new AccessActor("admin", "admin", "platform_admin", null, null, "active");
        when(authorization.currentActor()).thenReturn(ordinary);
        assertThatThrownBy(() -> service.preview(new WecomCleanupPreview.Request(List.of(new Item(message, "原因")))))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("WECOM_CLEANUP_MASTER_REQUIRED"));
        assertThat(count("wecom_messages", "id", message)).isEqualTo(1);
    }

    @Test
    void requiresReasonForEveryDistinctMessageAndRejectsNonMessageIds() {
        assertThatThrownBy(() -> service.preview(new WecomCleanupPreview.Request(List.of(new Item(message, " ")))))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.preview(new WecomCleanupPreview.Request(List.of(
                new Item(message, "理由一"), new Item(message, "理由二"))))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> preview(alert)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo("WECOM_CLEANUP_MESSAGE_MISSING"));
    }

    @Test
    void changedMessageOrNewHitRequiresNewPreview() throws Exception {
        var preview = preview(message);
        var proof = proof(preview.id());
        insertHit(prefix + "_newhit", message);
        assertThatThrownBy(() -> service.execute(preview.id(), proof)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo("WECOM_CLEANUP_SCOPE_CHANGED"));
        assertThat(count("wecom_messages", "id", message)).isEqualTo(1);

        var next = preview(message);
        var nextProof = proof(next.id());
        jdbc.update("UPDATE wecom_messages SET content_redacted = '已更正内容' WHERE id = ?", message);
        assertThatThrownBy(() -> service.execute(next.id(), nextProof)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo("WECOM_CLEANUP_SCOPE_CHANGED"));
        assertThat(count("wecom_risk_hits", "message_id", message)).isEqualTo(2);
    }

    @Test
    void changedHitReviewAndDifferentPreviewProofCannotDelete() throws Exception {
        var first = preview(message);
        var other = preview(message);
        var proof = proof(first.id());
        assertThatThrownBy(() -> service.execute(other.id(), proof)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo("MASTER_ACTION_CHALLENGE_INVALID"));
        var nextProof = proof(first.id());
        jdbc.update("UPDATE wecom_risk_hits SET status = 'confirmed', reviewer_id = 'reviewer' WHERE hit_id = ?", hit);
        assertThatThrownBy(() -> service.execute(first.id(), nextProof)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo("WECOM_CLEANUP_SCOPE_CHANGED"));
        assertThat(count("wecom_messages", "id", message)).isEqualTo(1);
    }

    @Test
    void failureAfterDeleteRollsBackMessagesHitsAndCompletionTogether() throws Exception {
        var preview = preview(message);
        var proof = proof(preview.id());
        doThrow(new IllegalStateException("完成记录写入失败")).when(repository).complete(anyString(), anyString(), anyString());
        TransactionTemplate nested = new TransactionTemplate(transactions);
        nested.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        assertThatThrownBy(() -> nested.execute(status -> service.execute(preview.id(), proof)))
                .isInstanceOf(IllegalStateException.class).hasMessage("完成记录写入失败");
        assertThat(count("wecom_messages", "id", message)).isEqualTo(1);
        assertThat(count("wecom_risk_hits", "hit_id", hit)).isEqualTo(1);
        assertThat(service.get(preview.id()).executedAt()).isNull();
    }

    private WecomCleanupPreview preview(String id) {
        return service.preview(new WecomCleanupPreview.Request(List.of(new Item(id, "测试清理理由"))));
    }

    private WecomCleanupPreview.ExecuteRequest proof(String id) throws Exception {
        var challenge = service.challenge(id);
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(KEY.getPrivate());
        signer.update(challenge.challenge().getBytes(StandardCharsets.UTF_8));
        return new WecomCleanupPreview.ExecuteRequest(challenge.challenge(),
                Base64.getEncoder().encodeToString(signer.sign()));
    }

    private void insertMessage(String id) {
        jdbc.update("""
                INSERT INTO wecom_messages (id, conversation_id, content_raw_ref, content_redacted, sent_at)
                VALUES (?, ?, '原文秘密标记', '脱敏消息', now())
                """, id, conversation);
    }

    private void insertHit(String id, String messageId) {
        jdbc.update("""
                INSERT INTO wecom_risk_hits (hit_id, conversation_id, message_id, rule_code, risk_level,
                    project_id, alert_id)
                VALUES (?, ?, ?, ?, 'high', 'proj_cleanup', ?)
                """, id, conversation, messageId, id, alert);
    }

    private int count(String table, String key, String id) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE " + key + " = ?", Integer.class, id);
    }

    private static KeyPair key() {
        try { return KeyPairGenerator.getInstance("Ed25519").generateKeyPair(); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }
}

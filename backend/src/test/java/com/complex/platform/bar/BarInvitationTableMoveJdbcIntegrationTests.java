package com.complex.platform.bar;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.CustomerSessionRecord;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 S 段第 167 条 167-R1：订卡邀请函名额按当前台位或合并台组计（真库、真接口、真授权链，请求体按前端蛇形键）。依据 BSM:1079「转台：整张台从原台位
 * 转到目标台位后，可邀请人数和最低消费额均按目标台规则执行……按目标台可邀请人数校验“已接受但尚未入场成员数＋已入场成员数”。超额即阻断转台，只能由开卡人
 * 先明确移出足够的未入场成员」「单台转台与合并台组转台同样适用」「合并后的可邀请人数和最低消费额，分别按两侧所包含全部原台的对应数值相加计算……已有的已接受
 * 成员和已入场成员一并进入新的合并台」「记录内容包括……操作前后可邀请人数……邀请与成员归属」；ENUM:114 同句；BSM:1036「已确认与已入场总人数不超过卡座可进人数」。
 * 167-R2 拆台：BSM:1079「分别按本次拆台后形成的每个独立原台或合并台组的可邀请人数，校验其“已接受但尚未入场成员数＋已入场成员数”；任一拆分结果超额即
 * 阻断拆台，系统不得自动移出成员或释放名额」「一次全部拆开必须先完成……逐台名额校验，再一次性成功或整体失败，不得部分拆开」。
 * 167-R3a 并台期间新发出的邀请：BSM:1079「并台期间新发出的邀请，以及由这些邀请形成的已接受成员、已入场成员，在首次拆台前由授权人员逐一分配到
 * 当前合并台所包含的某一张原台并留痕；已入场成员只调整所属原台，不得被移出，已占名额不得释放。分配完成后，分别按本次拆台后形成的每个独立原台或
 * 合并台组的可邀请人数，校验……」「一次全部拆开必须先完成全部邀请、成员……归属分配及逐台名额校验，再一次性成功或整体失败」；ENUM:114 同句。
 * 在并台之前签发的邀请（上面 (e)、(f)）不要求分配。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarInvitationTableMoveJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "prj_itm_" + suffix;
    private final String merchant = "mch_itm_" + suffix;
    private final String company = "ent_itm_" + suffix;
    private final AtomicInteger seq = new AtomicInteger();
    private final Set<String> createdCustomers = new HashSet<>();
    private RequestPostProcessor clerk;

    /** 一张已开台的客人自助订台：订台、会话、卡座、二维码口令与防伪标记、签发时的可进人数。 */
    private record Seated(String reservationId, String sessionId, String tableId, String token, String signature, int capacity) {
    }

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                project, "邀请随台项目 " + suffix);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'经营主体','management_company','active',now(),now())",
                company);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", company, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,"
                + "created_at,updated_at) VALUES(?,'邀请随台酒吧','bar',?,?,?,?,'active',now(),now())", merchant, company, company, company, project);
        String role = "role_itm_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'邀请随台回归','project','active',now())",
                role, "邀请随台现场岗" + Long.parseLong(suffix, 16));
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN("
                + "'bar.reservation.invitation.manage','bar.table_session.create','bar.table_session.view','bar.table_session.transfer',"
                + "'bar.table_session.merge','bar.table_session.split','bar.table.guest_checkin','bar.order.create')", role);
        clerk = account("k", role);
        account("t", role);
        jdbc.update("INSERT INTO staff_profiles(id,project_id,merchant_id,user_id,name,company_id,registration_status,status,employment_status,"
                + "is_sales_position,created_at,updated_at) VALUES(?,?,?,?,'接手服务员',?,'approved','active','employed',false,now(),now())",
                "pro_itm_t_" + suffix, project, merchant, user("t"), company);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** (a) 4 人卡座订台已接受 3 人（开卡人＋甲乙）开台后转到 2 人台：409 BAR_TRANSFER_INVITATION_OVER_CAPACITY，会话仍在原台，目标台仍空闲。 */
    @Test
    void transferBlockedWhenAcceptedAndEnteredExceedTargetQuota() throws Exception {
        Seated seated = seat(4, "a", "b");
        String twoSeat = table(2);
        transfer(seated.sessionId(), twoSeat)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TRANSFER_INVITATION_OVER_CAPACITY"));
        assertThat(jdbc.queryForObject("SELECT table_id FROM bar_table_sessions WHERE id = ?", String.class, seated.sessionId()))
                .isEqualTo(seated.tableId());
        assertThat(jdbc.queryForObject("SELECT status FROM bar_tables WHERE id = ?", String.class, twoSeat)).isEqualTo("active");
    }

    /** (b) 开卡人先移出未入场的乙再转 2 人台：200，转台记录快照记前后可邀请人数 4→2、已占 2；丙用原二维码接受即名额已满 409。 */
    @Test
    void transferAfterHolderRemovesMemberRecordsQuotaAndTargetQuotaGovernsAccept() throws Exception {
        Seated seated = seat(4, "a", "b");
        remove(seated, "b").andExpect(status().isOk());
        transfer(seated.sessionId(), table(2)).andExpect(status().isOk());
        assertThat(jdbc.queryForMap("SELECT snapshot_json->>'invite_capacity_before' AS before, snapshot_json->>'invite_capacity_after' AS after,"
                        + " snapshot_json->>'invite_occupied' AS occupied FROM bar_table_session_adjustments WHERE session_id = ?"
                        + " AND adjustment_type = 'transfer'", seated.sessionId()))
                .containsEntry("before", "4").containsEntry("after", "2").containsEntry("occupied", "2");
        confirm(seated, "c")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_INVITATION_CAPACITY_EXHAUSTED"));
    }

    /**
     * (c) 4 人卡座订台满 4 人开台后并入 6 人台散客会话：台组名额 10，戊用原二维码接受 200；预览名额 10、剩余 10−5、卡座仍回签发值；
     * 五人凭本人动态入场码入场全部放行；并台记录快照记前后可邀请人数 6→10、已占 4。
     */
    @Test
    void mergedGroupSumsQuotaForAcceptPreviewAndEntry() throws Exception {
        Seated seated = seat(4, "a", "b", "c");
        String walkIn = open(table(6), null);
        merge(walkIn, seated.sessionId()).andExpect(status().isOk());
        assertThat(jdbc.queryForMap("SELECT snapshot_json->>'invite_capacity_before' AS before, snapshot_json->>'invite_capacity_after' AS after,"
                        + " snapshot_json->>'invite_occupied' AS occupied FROM bar_table_session_adjustments WHERE session_id = ?"
                        + " AND adjustment_type = 'merge'", walkIn))
                .containsEntry("before", "6").containsEntry("after", "10").containsEntry("occupied", "4");
        confirm(seated, "e").andExpect(status().isOk());
        preview(seated, "f")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.capacity").value(10))
                .andExpect(jsonPath("$.data.remaining").value(5))
                .andExpect(jsonPath("$.data.table_id").value(seated.tableId()));
        for (String tag : List.of("h", "a", "b", "c", "e")) {
            enter(seated, tag).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("entered"));
        }
    }

    /** (d) 单台订台会话（已接受 2 人）转到空的 6 人台之后，原二维码可接受到第 6 人，第 7 人 409。 */
    @Test
    void singleTableTransferredToLargerTableAcceptsUpToTargetQuota() throws Exception {
        Seated seated = seat(4, "a");
        transfer(seated.sessionId(), table(6)).andExpect(status().isOk());
        for (String tag : List.of("b", "c", "d", "e")) {
            confirm(seated, tag).andExpect(status().isOk());
        }
        confirm(seated, "f")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_INVITATION_CAPACITY_EXHAUSTED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_reservation_invitation_members WHERE reservation_id = ? AND status <> 'revoked'",
                Integer.class, seated.reservationId())).isEqualTo(6);
    }

    /**
     * 167-R2 (e) 4 人卡座订台满 4 人并入 6 人台散客主会话，再用原二维码接受 2 人（台组 10 人以内）；单独拆出 4 人卡座即 409
     * BAR_SPLIT_INVITATION_OVER_CAPACITY，整笔回滚、两侧会话状态与来源不变；开卡人移出 2 名未入场的被邀请人后再拆 200，拆台快照记各结果台名额与已占。
     * 不开测试事务，才验得到拆台事务整笔回滚（数据带随机后缀，留在一次性测试库）。
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void splitSingleBlockedWhenSplitTableOverQuotaAndRollsBackWhole() throws Exception {
        Seated seated = seat(4, "a", "b", "c");
        String walkIn = open(table(6), null);
        merge(walkIn, seated.sessionId()).andExpect(status().isOk());
        confirm(seated, "d").andExpect(status().isOk());
        confirm(seated, "e").andExpect(status().isOk());
        split(walkIn, "single", seated.sessionId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SPLIT_INVITATION_OVER_CAPACITY"));
        assertThat(link(seated.sessionId())).containsEntry("status", "merged").containsEntry("source_session_id", walkIn);
        assertThat(link(walkIn)).containsEntry("status", "open").containsEntry("source_session_id", null);
        remove(seated, "d").andExpect(status().isOk());
        remove(seated, "e").andExpect(status().isOk());
        split(walkIn, "single", seated.sessionId()).andExpect(status().isOk());
        assertThat(link(seated.sessionId())).containsEntry("status", "open").containsEntry("source_session_id", null);
        assertThat(jdbc.queryForList("SELECT r->>'session_id' AS session_id, r->>'invite_capacity' AS capacity, r->>'invite_occupied' AS occupied"
                        + " FROM bar_table_session_adjustments a, jsonb_array_elements(a.snapshot_json->'invite_results') r"
                        + " WHERE a.session_id = ? AND a.adjustment_type = 'split'", walkIn))
                .containsExactlyInAnyOrder(
                        Map.of("session_id", walkIn, "capacity", "6", "occupied", "0"),
                        Map.of("session_id", seated.sessionId(), "capacity", "4", "occupied", "4"));
    }

    /**
     * 167-R2 (f) 一次全部拆开：6 人台散客主会话先后并入 4 人卡座订台（满 4 人后用原二维码再接受 2 人）与 2 人台散客会话；任一结果台超额即 409、
     * 两张原台都不拆开；开卡人移出 2 人后整体拆开 200。不开测试事务，理由同上。
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void splitAllBlockedAsWholeWhenAnyResultTableOverQuota() throws Exception {
        Seated seated = seat(4, "a", "b", "c");
        String walkIn = open(table(6), null);
        String twoSeat = open(table(2), null);
        merge(walkIn, seated.sessionId()).andExpect(status().isOk());
        merge(walkIn, twoSeat).andExpect(status().isOk());
        confirm(seated, "d").andExpect(status().isOk());
        confirm(seated, "e").andExpect(status().isOk());
        split(walkIn, "all", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SPLIT_INVITATION_OVER_CAPACITY"));
        for (String child : List.of(seated.sessionId(), twoSeat)) {
            assertThat(link(child)).containsEntry("status", "merged").containsEntry("source_session_id", walkIn);
        }
        remove(seated, "d").andExpect(status().isOk());
        remove(seated, "e").andExpect(status().isOk());
        split(walkIn, "all", null).andExpect(status().isOk());
        for (String child : List.of(seated.sessionId(), twoSeat)) {
            assertThat(link(child)).containsEntry("status", "open").containsEntry("source_session_id", null);
        }
    }

    /**
     * 167-R3a (g) 4 人卡座订台开台后并入 6 人台散客主会话，并台后才签发邀请、甲乙接受：首次拆台不带分配即 409
     * BAR_SPLIT_INVITATION_ASSIGNMENT_REQUIRED（列出订台与甲乙、不列开卡人），并台关系不变；带上邀请与成员分配后 200，逐条留痕，
     * 拆台快照记分配与各结果台名额；拆后丙接受归所分配的原台，预览按该原台计；再并台再拆不再要求分配。
     */
    @Test
    void mergeTimeInvitationMustBeAssignedBeforeFirstSplit() throws Exception {
        Seated seated = seatWithoutInvitation(4);
        String walkIn = open(table(6), null);
        merge(walkIn, seated.sessionId()).andExpect(status().isOk());
        Seated invited = issue(seated);
        confirm(invited, "a").andExpect(status().isOk());
        confirm(invited, "b").andExpect(status().isOk());
        String a = memberId(invited, "a");
        String b = memberId(invited, "b");
        String holder = jdbc.queryForObject("SELECT id FROM bar_reservation_invitation_members WHERE reservation_id = ? AND is_holder",
                String.class, invited.reservationId());
        assertThat(jdbc.queryForObject("SELECT invitation_merge_pending FROM bar_reservations WHERE id = ?", Boolean.class,
                invited.reservationId())).isTrue();
        assertThat(assignment(a)).containsEntry("merge_pending", true);
        assertThat(assignment(b)).containsEntry("merge_pending", true);
        assertThat(assignment(holder)).containsEntry("merge_pending", false);
        split(walkIn, "single", seated.sessionId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SPLIT_INVITATION_ASSIGNMENT_REQUIRED"))
                .andExpect(jsonPath("$.message").value(allOf(containsString(invited.reservationId()), containsString(a),
                        containsString(b), not(containsString(holder)))));
        assertThat(link(seated.sessionId())).containsEntry("status", "merged").containsEntry("source_session_id", walkIn);

        String key = "itm-split-" + seq.incrementAndGet() + "-" + suffix;
        List<Map<String, Object>> invitations = List.of(Map.of("reservation_id", invited.reservationId(), "session_id", walkIn));
        List<Map<String, Object>> members = List.of(Map.of("member_id", a, "session_id", walkIn),
                Map.of("member_id", b, "session_id", seated.sessionId()));
        split(key, walkIn, "single", seated.sessionId(), invitations, members).andExpect(status().isOk());
        assertThat(assignment(a)).containsEntry("assigned_session_id", walkIn);
        assertThat(assignment(b)).containsEntry("assigned_session_id", seated.sessionId());
        assertThat(jdbc.queryForObject("SELECT invitation_assigned_session_id FROM bar_reservations WHERE id = ?", String.class,
                invited.reservationId())).isEqualTo(walkIn);
        assertThat(audits("bar.table_session.member_assigned", key)).isEqualTo(2);
        assertThat(audits("bar.table_session.invitation_assigned", key)).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT r->>'session_id' AS session_id, r->>'invite_capacity' AS capacity, r->>'invite_occupied' AS occupied"
                        + " FROM bar_table_session_adjustments a, jsonb_array_elements(a.snapshot_json->'invite_results') r"
                        + " WHERE a.idempotency_key = ?", key))
                .containsExactlyInAnyOrder(
                        Map.of("session_id", walkIn, "capacity", "6", "occupied", "1"),
                        Map.of("session_id", seated.sessionId(), "capacity", "4", "occupied", "2"));
        assertThat(jdbc.queryForList("SELECT r->>'member_id' AS member_id, r->>'session_id' AS session_id"
                + " FROM bar_table_session_adjustments a, jsonb_array_elements(a.snapshot_json->'member_assignments') r"
                + " WHERE a.idempotency_key = ?", key)).containsExactlyInAnyOrderElementsOf(members);
        assertThat(jdbc.queryForList("SELECT r->>'reservation_id' AS reservation_id, r->>'session_id' AS session_id"
                + " FROM bar_table_session_adjustments a, jsonb_array_elements(a.snapshot_json->'invitation_assignments') r"
                + " WHERE a.idempotency_key = ?", key)).containsExactlyInAnyOrderElementsOf(invitations);

        confirm(invited, "c").andExpect(status().isOk());
        assertThat(assignment(memberId(invited, "c"))).containsEntry("assigned_session_id", walkIn).containsEntry("merge_pending", false);
        preview(invited, "d")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.capacity").value(6))
                .andExpect(jsonPath("$.data.remaining").value(4))
                .andExpect(jsonPath("$.data.table_name").value("6人卡座"))
                .andExpect(jsonPath("$.data.table_id").value(seated.tableId()));
        merge(walkIn, seated.sessionId()).andExpect(status().isOk());
        split(walkIn, "single", seated.sessionId()).andExpect(status().isOk());
    }

    /**
     * 167-R3a (h) 4 人卡座订台开台后并入 2 人台散客主会话，并台后签发、甲乙丙接受、甲乙入场；拆出卡座时甲乙分到 2 人台、丙分到卡座：200，
     * 甲乙仍是已入场、只调所属原台；丙随后入场按其所属原台（卡座台组已入场 0）计名额放行，不按邀请所分配的 2 人台（已入场 2）误挡。
     */
    @Test
    void enteredMembersOnlyReassignedAndEntryCountsByMemberTable() throws Exception {
        Seated seated = seatWithoutInvitation(4);
        String twoSeat = open(table(2), null);
        merge(twoSeat, seated.sessionId()).andExpect(status().isOk());
        Seated invited = issue(seated);
        for (String tag : List.of("a", "b", "c")) {
            confirm(invited, tag).andExpect(status().isOk());
        }
        enter(invited, "a").andExpect(status().isOk());
        enter(invited, "b").andExpect(status().isOk());
        split("itm-split-" + seq.incrementAndGet() + "-" + suffix, twoSeat, "single", seated.sessionId(),
                List.of(Map.of("reservation_id", invited.reservationId(), "session_id", twoSeat)),
                List.of(Map.of("member_id", memberId(invited, "a"), "session_id", twoSeat),
                        Map.of("member_id", memberId(invited, "b"), "session_id", twoSeat),
                        Map.of("member_id", memberId(invited, "c"), "session_id", seated.sessionId())))
                .andExpect(status().isOk());
        for (String tag : List.of("a", "b")) {
            assertThat(assignment(memberId(invited, tag))).containsEntry("status", "entered").containsEntry("assigned_session_id", twoSeat);
        }
        enter(invited, "c").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("entered"));
    }

    /**
     * 167-R3a (i) 并台后签发、甲乙丙丁接受，拆台把邀请与四人全分到 4 人卡座（开卡人＋4＝5 人）：409 BAR_SPLIT_INVITATION_OVER_CAPACITY，
     * 分配与留痕随整笔回滚、卡座仍在合并台；改为两人分到 6 人台再拆 200。不开测试事务，才验得到拆台事务整笔回滚。
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void splitOverQuotaAfterAssignmentRollsBackAssignments() throws Exception {
        Seated seated = seatWithoutInvitation(4);
        String walkIn = open(table(6), null);
        merge(walkIn, seated.sessionId()).andExpect(status().isOk());
        Seated invited = issue(seated);
        List<String> ids = new ArrayList<>();
        for (String tag : List.of("a", "b", "c", "d")) {
            confirm(invited, tag).andExpect(status().isOk());
            ids.add(memberId(invited, tag));
        }
        List<Map<String, Object>> invitations = List.of(Map.of("reservation_id", invited.reservationId(), "session_id", seated.sessionId()));
        String key = "itm-split-" + seq.incrementAndGet() + "-" + suffix;
        split(key, walkIn, "single", seated.sessionId(), invitations,
                ids.stream().map(id -> Map.<String, Object>of("member_id", id, "session_id", seated.sessionId())).toList())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SPLIT_INVITATION_OVER_CAPACITY"));
        for (String id : ids) {
            assertThat(assignment(id)).containsEntry("assigned_session_id", null);
        }
        assertThat(jdbc.queryForObject("SELECT invitation_assigned_session_id FROM bar_reservations WHERE id = ?", String.class,
                invited.reservationId())).isNull();
        assertThat(audits("bar.table_session.member_assigned", key)).isZero();
        assertThat(audits("bar.table_session.invitation_assigned", key)).isZero();
        assertThat(link(seated.sessionId())).containsEntry("status", "merged").containsEntry("source_session_id", walkIn);
        split("itm-split-" + seq.incrementAndGet() + "-" + suffix, walkIn, "single", seated.sessionId(), invitations,
                List.of(Map.of("member_id", ids.get(0), "session_id", walkIn), Map.of("member_id", ids.get(1), "session_id", walkIn),
                        Map.of("member_id", ids.get(2), "session_id", seated.sessionId()),
                        Map.of("member_id", ids.get(3), "session_id", seated.sessionId())))
                .andExpect(status().isOk());
    }

    /**
     * 167-R3a (j) 一次全部拆开：6 人台散客主会话先后并入 4 人卡座订台与 2 人台散客会话，之后签发、甲接受；分配到合并台以外的会话即 400，
     * 不带分配即 409 BAR_SPLIT_INVITATION_ASSIGNMENT_REQUIRED、两张原台都不拆开；带齐邀请与甲的分配后 200。
     */
    @Test
    void splitAllRequiresAssignmentsBeforeSplittingAnyTable() throws Exception {
        Seated seated = seatWithoutInvitation(4);
        String walkIn = open(table(6), null);
        String twoSeat = open(table(2), null);
        merge(walkIn, seated.sessionId()).andExpect(status().isOk());
        merge(walkIn, twoSeat).andExpect(status().isOk());
        Seated invited = issue(seated);
        confirm(invited, "a").andExpect(status().isOk());
        String a = memberId(invited, "a");
        String elsewhere = open(table(2), null);
        split("itm-split-" + seq.incrementAndGet() + "-" + suffix, walkIn, "all", null,
                List.of(Map.of("reservation_id", invited.reservationId(), "session_id", elsewhere)),
                List.of(Map.of("member_id", a, "session_id", walkIn)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        split(walkIn, "all", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SPLIT_INVITATION_ASSIGNMENT_REQUIRED"));
        for (String child : List.of(seated.sessionId(), twoSeat)) {
            assertThat(link(child)).containsEntry("status", "merged").containsEntry("source_session_id", walkIn);
        }
        split("itm-split-" + seq.incrementAndGet() + "-" + suffix, walkIn, "all", null,
                List.of(Map.of("reservation_id", invited.reservationId(), "session_id", walkIn)),
                List.of(Map.of("member_id", a, "session_id", walkIn)))
                .andExpect(status().isOk());
        for (String child : List.of(seated.sessionId(), twoSeat)) {
            assertThat(link(child)).containsEntry("status", "open").containsEntry("source_session_id", null);
        }
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    /** 开卡人 h 在小程序自助订 capacity 人卡座，员工签发邀请函，朋友们按原二维码接受，再带订台开台。 */
    private Seated seat(int capacity, String... friends) throws Exception {
        Seated booked = issue(reserve(capacity));
        for (String tag : friends) {
            confirm(booked, tag).andExpect(status().isOk());
        }
        return new Seated(booked.reservationId(), open(booked.tableId(), booked.reservationId()), booked.tableId(), booked.token(),
                booked.signature(), capacity);
    }

    /** 167-R3a：开卡人 h 在小程序自助订 capacity 人卡座，员工先不签发，带订台开台。 */
    private Seated seatWithoutInvitation(int capacity) throws Exception {
        Seated booked = reserve(capacity);
        return new Seated(booked.reservationId(), open(booked.tableId(), booked.reservationId()), booked.tableId(), null, null, capacity);
    }

    /** 开卡人 h 在小程序自助订 capacity 人卡座（未签发、未开台）。 */
    private Seated reserve(int capacity) {
        String table = table(capacity);
        String reservation = "rsv_itm_" + suffix + "_" + seq.incrementAndGet();
        jdbc.update("INSERT INTO bar_reservations(id,bar_id,merchant_id,table_id,customer_id,reserved_at,guest_count,allowed_pass_free_guest_count,"
                + "deposit_amount_cent,deposit_status,order_source,status,created_terminal,created_at,updated_at,latest_arrival_at)"
                + " VALUES(?,?,?,?,?,?,2,?,0,'not_required','miniapp','reserved','miniapp',now(),now(),?)",
                reservation, merchant, merchant, table, customer("h"), Timestamp.from(Instant.now().plus(Duration.ofMinutes(30))), capacity,
                Timestamp.from(Instant.now().plus(Duration.ofHours(2))));
        return new Seated(reservation, null, table, null, null, capacity);
    }

    /** 员工在后台「卡座与订台」页签发邀请函，带回二维码口令与防伪标记。 */
    private Seated issue(Seated seated) throws Exception {
        ResultActions issued = mockMvc.perform(post("/api/v1/bar/reservations/" + seated.reservationId() + "/invitation").with(clerk)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "itm-issue-" + seated.reservationId()))))
                .andExpect(status().isOk());
        return new Seated(seated.reservationId(), seated.sessionId(), seated.tableId(),
                CustomerTicketPaymentFixture.read(issued, "$.data.invitation_token"),
                CustomerTicketPaymentFixture.read(issued, "$.data.invitation_signature"), seated.capacity());
    }

    private String open(String table, String reservation) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "itm-open-" + seq.incrementAndGet() + "-" + suffix);
        body.put("bar_id", merchant);
        body.put("table_id", table);
        if (reservation != null) {
            body.put("reservation_id", reservation);
        }
        body.put("guest_count", 2);
        return CustomerTicketPaymentFixture.read(mockMvc.perform(post("/api/v1/bar/table-sessions").with(clerk)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isOk()), "$.data.id");
    }

    /** 后台「卡座与订台」页转台请求（目标台接手人 t 有目标台点位此刻的已发布班次）。 */
    private ResultActions transfer(String session, String targetTable) throws Exception {
        shift("t", jdbc.queryForObject("SELECT space_id FROM bar_tables WHERE id = ?", String.class, targetTable));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "itm-transfer-" + seq.incrementAndGet() + "-" + suffix);
        body.put("target_table_id", targetTable);
        body.put("target_service_staff_id", user("t"));
        body.put("reason", "客人换台");
        body.put("session_version", version(session));
        return mockMvc.perform(post("/api/v1/bar/table-sessions/" + session + "/transfer").with(clerk)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private ResultActions merge(String primary, String merged) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "itm-merge-" + seq.incrementAndGet() + "-" + suffix);
        body.put("merged_session_id", merged);
        body.put("reason", "两桌朋友拼台");
        body.put("primary_session_version", version(primary));
        body.put("merged_session_version", version(merged));
        return mockMvc.perform(post("/api/v1/bar/table-sessions/" + primary + "/merge").with(clerk)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 后台「卡座与订台」页拆台请求（BarTableSessionsPage 拆台表单的请求形状）。 */
    private ResultActions split(String primary, String mode, String splitSession) throws Exception {
        return split("itm-split-" + seq.incrementAndGet() + "-" + suffix, primary, mode, splitSession, null, null);
    }

    /** 167-R3a：拆台请求另带邀请归属 [{reservation_id, session_id}] 与成员归属 [{member_id, session_id}]。 */
    private ResultActions split(String key, String primary, String mode, String splitSession,
            List<Map<String, Object>> invitationAssignments, List<Map<String, Object>> memberAssignments) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key);
        body.put("mode", mode);
        if (splitSession != null) {
            body.put("split_session_id", splitSession);
        }
        body.put("order_assignments", List.of());
        if (invitationAssignments != null) {
            body.put("invitation_assignments", invitationAssignments);
        }
        if (memberAssignments != null) {
            body.put("member_assignments", memberAssignments);
        }
        body.put("reason", "客人分开坐");
        body.put("requested_by_user_id", "admin-web");
        body.put("primary_session_version", version(primary));
        return mockMvc.perform(post("/api/v1/bar/table-sessions/" + primary + "/split").with(clerk)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private Map<String, Object> link(String session) {
        return jdbc.queryForMap("SELECT status, source_session_id FROM bar_table_sessions WHERE id = ?", session);
    }

    /** 167-R3a：成员的状态、待分配标记与所属原台。 */
    private Map<String, Object> assignment(String memberId) {
        return jdbc.queryForMap("SELECT status, merge_pending, assigned_session_id FROM bar_reservation_invitation_members WHERE id = ?",
                memberId);
    }

    private int audits(String event, String key) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = ? AND idempotency_key = ?", Integer.class,
                event, key);
    }

    private String memberId(Seated seated, String tag) {
        return jdbc.queryForObject("SELECT id FROM bar_reservation_invitation_members WHERE reservation_id = ? AND member_customer_id = ?",
                String.class, seated.reservationId(), customer(tag));
    }

    /** 客人按原二维码载荷接受（小程序请求形状）。 */
    private ResultActions confirm(Seated seated, String tag) throws Exception {
        customer(tag);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "itm-confirm-" + UUID.randomUUID());
        body.put("invitation_token", seated.token());
        body.put("invitation_signature", seated.signature());
        body.put("table_id", seated.tableId());
        body.put("capacity", seated.capacity());
        return mockMvc.perform(post("/api/v1/customer/bar-invitations/confirm").header("X-Terminal-Type", "miniapp")
                .header("X-Customer-Token", token(tag)).header("Idempotency-Key", body.get("idempotency_key").toString())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private ResultActions preview(Seated seated, String tag) throws Exception {
        customer(tag);
        return mockMvc.perform(get("/api/v1/customer/bar-invitations/preview")
                .param("invitation_token", seated.token()).param("invitation_signature", seated.signature())
                .param("table_id", seated.tableId()).param("capacity", String.valueOf(seated.capacity()))
                .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token(tag)));
    }

    /** 开卡人 h 在小程序移出某位未入场的被邀请人。 */
    private ResultActions remove(Seated seated, String tag) throws Exception {
        String memberId = memberId(seated, tag);
        String key = "itm-remove-" + UUID.randomUUID();
        return mockMvc.perform(post("/api/v1/customer/bar-invitations/" + seated.reservationId() + "/members/" + memberId + "/remove")
                .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token("h")).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("idempotency_key", key))));
    }

    /** 本人取动态入场码，门口按后台入场核验请求形状核验。 */
    private ResultActions enter(Seated seated, String tag) throws Exception {
        String code = CustomerTicketPaymentFixture.read(mockMvc.perform(get("/api/v1/customer/bar-invitations/" + seated.reservationId() + "/entry-code")
                        .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token(tag)))
                .andExpect(status().isOk()), "$.data.code");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "itm-entry-" + UUID.randomUUID());
        body.put("credential_payload", code);
        body.put("point_id", merchant);
        body.put("business_context", Map.of("bar_id", merchant));
        return mockMvc.perform(post("/api/v1/bar/reservations/" + seated.reservationId() + "/invitation/entry-verify").with(clerk)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private Long version(String session) {
        return jdbc.queryForObject("SELECT session_version FROM bar_table_sessions WHERE id = ?", Long.class, session);
    }

    /** 本项目本商户铺位上的卡座：可进人数＝免通票名额＝capacity，低消 0。 */
    private String table(int capacity) {
        String space = "spc_itm_" + suffix + "_" + seq.incrementAndGet();
        jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,status,created_at,updated_at) "
                + "VALUES(?,?,?,'卡座区','unit','active',now(),now())", space, project, merchant);
        String id = "tbl_itm_" + suffix + "_" + seq.incrementAndGet();
        jdbc.update("INSERT INTO bar_tables(id,bar_id,merchant_id,table_code,table_name,table_type,capacity,allowed_pass_free_guest_count,"
                + "min_consumption_cent,currency,status,created_at,updated_at,space_id) VALUES(?,?,?,?,?,'card_seat',?,?,0,'CNY','active',now(),now(),?)",
                id, merchant, merchant, "K" + id, capacity + "人卡座", capacity, capacity, space);
        return id;
    }

    /** 已发布班次：覆盖此刻，工作点位为该铺位。 */
    private void shift(String tag, String space) {
        Instant now = Instant.now();
        jdbc.update("INSERT INTO staff_shift_schedules(id,user_id,work_date,start_at,end_at,point_space_ids,created_by,project_id) "
                + "VALUES(?,?,?,?,?,jsonb_build_array(?::text),?,?)", "sft_itm_" + tag + "_" + suffix + "_" + seq.incrementAndGet(), user(tag),
                LocalDate.now(), Timestamp.from(now.minus(Duration.ofHours(2))), Timestamp.from(now.plus(Duration.ofHours(4))), space, user("k"), project);
    }

    private RequestPostProcessor account(String tag, String role) {
        String userId = user(tag);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())",
                userId, userId, project, merchant);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", userId, project);
        AccessActor actor = new AccessActor(userId, userId, "staff", project, merchant, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private String user(String tag) {
        return "usr_itm_" + tag + "_" + suffix;
    }

    /** 小程序客人（同一测试内按 tag 只建一次），返回客人号。 */
    private String customer(String tag) {
        String id = "cus_itm_" + tag + "_" + suffix;
        if (createdCustomers.add(tag)) {
            Instant now = Instant.now();
            String phone = "136" + String.format("%08d", Math.floorMod((suffix + tag).hashCode(), 100_000_000));
            customers.saveCustomer(new CustomerRecord(id, phone, "136****" + phone.substring(7), "active", null, null, now, now));
            customers.saveSession(new CustomerSessionRecord("sess_itm_" + tag + "_" + suffix, id, token(tag), "miniapp", now,
                    now.plusSeconds(3600), "active"));
        }
        return id;
    }

    private String token(String tag) {
        return "tok_itm_" + tag + "_" + suffix;
    }
}

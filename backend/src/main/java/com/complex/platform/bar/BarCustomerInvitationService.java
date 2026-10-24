package com.complex.platform.bar;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiException;
import com.complex.platform.ticketing.TicketingRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * ROOT-03 clients-R6 订卡邀请函客人侧与 backend-R25 接受时核防伪标记（客人身份由 CustomerService 解析后传入，这里只认本人对象）。依据：
 * BSM 第 870 行「订卡客人可在 App/小程序查看自己的订卡邀请函二维码（注明酒吧、卡座、可进人数），转发给朋友；朋友在 App/小程序确认接受后占一个名额，
 * 该名额自动关联到接受人本人的动态入场码，到店出示本人的动态入场码入场……作为开卡人……可把尚未入场的被邀请人移出邀请函、收回其名额；名额释放后可再邀请他人。
 * 被邀请人一旦完成核销入场，开卡人不得再将其移出」；BSM 第 1036 行「邀请函二维码只用于转发与接受，本身不作为入场凭证（邀请函二维码带防伪标记，
 * 被复制或篡改卡座、可进人数即无法接受）」；ENUM 第 105 行「已入场的成员不可再被移出邀请函」。
 */
@Service
public class BarCustomerInvitationService {

    /** 我的订台一行（App/小程序「我的订台」）。 */
    public record ReservationView(String reservationId, String barId, String barName, String tableId, String tableName,
            String tableType, Instant reservedAt, Instant latestArrivalAt, int guestCount, long depositAmountCent,
            String depositStatus, String status, String invitationStatus) {
    }

    /** 扫邀请函二维码后的预览：酒吧、卡座、可进人数与剩余名额，不占名额。 */
    public record InvitationPreview(String reservationId, String barId, String barName, String tableId, String tableName,
            Instant reservedAt, int capacity, int remaining) {
    }

    /** 本人动态入场码（明文只出现在本响应，不落库）。 */
    public record EntryCode(String reservationId, String memberId, String code, Instant expiresAt, int refreshIntervalSeconds) {
    }

    private final BarRepository repository;
    private final BarInvitationTokenSigner signer;
    private final JdbcTemplate jdbc;
    private final TicketingRepository ticketing;

    public BarCustomerInvitationService(BarRepository repository, BarInvitationTokenSigner signer, JdbcTemplate jdbc,
            TicketingRepository ticketing) {
        this.repository = repository;
        this.signer = signer;
        this.jdbc = jdbc;
        this.ticketing = ticketing;
    }

    public List<ReservationView> listMine(String customerId) {
        return jdbc.query("""
                SELECT r.id, r.bar_id, m.merchant_name, r.table_id, t.table_name, t.table_type, r.reserved_at, r.latest_arrival_at,
                       r.guest_count, r.deposit_amount_cent, r.deposit_status, r.status, r.invitation_status
                  FROM bar_reservations r
                  JOIN bar_tables t ON t.id = r.table_id
                  LEFT JOIN merchants m ON m.id = r.bar_id
                 WHERE r.customer_id = ? AND r.delete_status <> 'approved'
                 ORDER BY r.reserved_at DESC, r.id
                """, (rs, n) -> new ReservationView(rs.getString("id"), rs.getString("bar_id"), rs.getString("merchant_name"),
                rs.getString("table_id"), rs.getString("table_name"), rs.getString("table_type"), instant(rs, "reserved_at"),
                instant(rs, "latest_arrival_at"), rs.getInt("guest_count"), rs.getLong("deposit_amount_cent"),
                rs.getString("deposit_status"), rs.getString("status"), rs.getString("invitation_status")), customerId);
    }

    public InvitationPreview preview(String invitationToken, String signature, String tableId, Integer capacity) {
        BarReservationRecord reservation = repository.findReservationByInvitationToken(requireText(invitationToken, "invitation_token"))
                .orElseThrow(() -> ApiException.notFound("BAR_INVITATION_NOT_FOUND", "卡座邀请不存在或已被删除"));
        if (!"active".equals(reservation.invitationStatus())) {
            throw ApiException.conflict("BAR_INVITATION_NOT_ACTIVE", "卡座邀请已失效");
        }
        requireGenuine(reservation, signature, tableId, capacity);
        Map<String, Object> names = jdbc.queryForMap("""
                SELECT t.table_name, m.merchant_name FROM bar_tables t LEFT JOIN merchants m ON m.id = t.bar_id WHERE t.id = ?
                """, reservation.tableId());
        long occupied = repository.listInvitationMembers(reservation.id()).stream()
                .filter(member -> !"revoked".equals(member.status())).count();
        int seats = reservation.invitationCapacity();
        return new InvitationPreview(reservation.id(), reservation.barId(), (String) names.get("merchant_name"),
                reservation.tableId(), (String) names.get("table_name"), reservation.reservedAt(), seats,
                (int) Math.max(seats - occupied, 0));
    }

    /**
     * backend-R25：二维码载荷里的卡座、可进人数须与签发时一致，且防伪标记按签发规范串核验通过；否则 409 BAR_INVITATION_SIGNATURE_INVALID、不占名额。
     */
    public void requireGenuine(BarReservationRecord reservation, String signature, String tableId, Integer capacity) {
        if (capacity == null || !reservation.tableId().equals(tableId)
                || !capacity.equals(reservation.invitationCapacity())
                || !signer.matches(signer.canonical(reservation.id(), reservation.barId(), reservation.tableId(), capacity,
                        reservation.invitationToken()), signature)) {
            throw ApiException.conflict("BAR_INVITATION_SIGNATURE_INVALID", "邀请函已被复制或篡改，无法接受");
        }
    }

    /** 开卡人移出尚未入场的被邀请人，名额随即释放；已入场的不可移出。调用方提供事务与留痕。 */
    public BarReservationInvitationMemberRecord removeMember(String customerId, String reservationId, String memberId) {
        BarReservationRecord reservation = repository.findReservationByIdForUpdate(reservationId)
                .orElseThrow(() -> ApiException.notFound("BAR_RESERVATION_NOT_FOUND", "订台记录不存在或已被删除"));
        if (!customerId.equals(reservation.customerId())) {
            throw ApiException.forbidden("CUSTOMER_RESERVATION_FORBIDDEN", "只有开卡人本人才能移出被邀请人");
        }
        BarReservationInvitationMemberRecord member = repository.listInvitationMembers(reservation.id()).stream()
                .filter(item -> memberId.equals(item.id()) && !item.isHolder())
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("BAR_INVITATION_MEMBER_NOT_FOUND", "该被邀请人不在本邀请函上"));
        if ("entered".equals(member.status())) {
            throw ApiException.conflict("BAR_INVITATION_MEMBER_ENTERED", "该被邀请人已入场，不能移出");
        }
        if ("revoked".equals(member.status())) {
            return member;
        }
        Instant now = Instant.now();
        return repository.saveInvitationMember(new BarReservationInvitationMemberRecord(member.id(), member.reservationId(),
                member.barId(), member.memberIdentifier(), member.memberCustomerId(), member.isHolder(), "revoked",
                member.confirmedAt(), null, member.deviceId(), member.createdAt(), now));
    }

    /** 本人（已接受未入场的成员，含开卡人自己）的动态入场码；窗口＝全场统一二维码刷新间隔（BSM 第 585 行）。 */
    public EntryCode entryCode(String customerId, String reservationId) {
        BarReservationRecord reservation = repository.findReservationById(reservationId)
                .orElseThrow(() -> ApiException.notFound("BAR_RESERVATION_NOT_FOUND", "订台记录不存在或已被删除"));
        if (!"active".equals(reservation.invitationStatus())) {
            throw ApiException.conflict("BAR_INVITATION_NOT_ACTIVE", "卡座邀请已失效");
        }
        BarReservationInvitationMemberRecord member = repository.listInvitationMembers(reservation.id()).stream()
                .filter(item -> customerId.equals(item.memberCustomerId()) && !"revoked".equals(item.status()))
                .findFirst()
                .orElseThrow(() -> ApiException.conflict("BAR_INVITATION_MEMBER_NOT_CONFIRMED", "你尚未接受该邀请"));
        if ("entered".equals(member.status())) {
            throw ApiException.conflict("BAR_INVITATION_MEMBER_ENTERED", "你已入场");
        }
        int window = ticketing.findQrRefreshIntervalSeconds();
        Instant expiresAt = Instant.now().plusSeconds(window);
        return new EntryCode(reservation.id(), member.id(), signer.entryCode(member.id(), reservation.id(), expiresAt),
                expiresAt, window);
    }

    private static String requireText(String value, String field) {
        return com.complex.platform.common.Strings.requireText(value, field);
    }

    private static Instant instant(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        java.sql.Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}

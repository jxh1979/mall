package com.complex.platform.partner.dto;

/** AUD-033 平台管理员把既有后台账号绑定到外部协作机构（access_users.partner_org_id 此前无任何 API 入口）。 */
public record PartnerOrgUserBindRequest(
        String idempotencyKey,
        String userId
) {
}

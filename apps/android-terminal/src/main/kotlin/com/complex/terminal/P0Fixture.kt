package com.complex.terminal

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class IdResponse(
  val id: String = "",
)

@Serializable
data class UserResponse(
  val id: String = "",
  val username: String = "",
)

@Serializable
data class IssuedEntitlementResponse(
  val entitlementCode: String = "",
)

@Serializable
data class IssuedTicketResponse(
  val issuedEntitlements: List<IssuedEntitlementResponse> = emptyList(),
)

@Serializable
data class P0FixtureData(
  val organizationId: String,
  val merchantId: String,
  val venueId: String,
  val areaId: String,
  val pointId: String,
  val staffId: String,
  val entitlementCode: String,
)

class P0Fixture(
  private val api: TerminalApiClient,
  private val stamp: Long,
) {
  private val testPassword = System.getenv("P0_TEST_PASSWORD") ?: "P0-$stamp-${System.nanoTime()}-Aa1!"

  fun create(): P0FixtureData {
    val organization = api.postJson<IdResponse>(
      "/api/v1/platform/business-entities",
      obj(
        "entity_name" to "P0 Android Org $stamp",
        "entity_role" to "operation",
        "unified_social_credit_code" to "p0-android-org-$stamp",
        "legal_representative" to "P0 Android",
        "bank_account_ref" to "vault://complex/p0/android-org",
        "status" to "active",
        *fixtureMutationMetadata("business_entity"),
      ),
    )
    val merchant = api.postJson<IdResponse>(
      "/api/v1/platform/merchants",
      obj(
        "business_entity_id" to organization.id,
        "merchant_name" to "P0 Android Merchant $stamp",
        "merchant_type" to "other",
        "contact_name" to "P0 Android",
        "contact_phone" to "18800000011",
        "status" to "active",
        *fixtureMutationMetadata("merchant"),
      ),
    )
    val venue = api.postJson<IdResponse>(
      "/api/v1/platform/projects",
      obj(
        "merchant_id" to merchant.id,
        "venue_type" to "complex",
        "name" to "P0 Android Venue $stamp",
        "code" to "p0-android-venue-$stamp",
        "floor" to "1F",
        "capacity" to 120,
        "status" to "active",
        *fixtureMutationMetadata("project"),
      ),
    )
    val area = api.postJson<IdResponse>(
      "/api/v1/platform/spaces",
      obj(
        "project_id" to venue.id,
        "merchant_id" to merchant.id,
        "space_name" to "P0 Android Gate Area $stamp",
        "space_type" to "zone",
        "floor_no" to "1F",
        "area_square_meter" to 60,
        *fixtureMutationMetadata("area"),
      ),
    )
    val point = api.postJson<IdResponse>(
      "/api/v1/platform/spaces",
      obj(
        "parent_space_id" to area.id,
        "project_id" to venue.id,
        "merchant_id" to merchant.id,
        "space_name" to "P0 Android Gate $stamp",
        // 空间类型必须是 space_type_dictionary(V184)里的 active 值,原 "facility" 不在字典致 P0 smoke 400;
        // 闸机点位归到 "booth"(岗亭/闸口)。{building,floor,zone,shop,public_area,parking,booth,other}
        "space_type" to "booth",
        "floor_no" to "1F",
        "area_square_meter" to 30,
        *fixtureMutationMetadata("point"),
      ),
    )
    val staff = api.postJson<UserResponse>(
      "/api/v1/access/users",
      obj(
        "username" to "p0_android_staff_$stamp",
        "password" to testPassword,
        "accountType" to "staff",
        "organizationId" to organization.id,
        "merchantId" to merchant.id,
        "status" to "active",
        *fixtureMutationMetadata("access_user"),
      ),
    )
    val product = api.postJson<IdResponse>(
      "/api/v1/ticketing/ticket-types",
      obj(
        "ticket_name" to "P0 Android Pass $stamp",
        "base_calendar_price_mode" to "weekday_weekend_holiday",
        "weekday_price_cent" to 9900,
        "weekend_price_cent" to 11900,
        "holiday_price_cent" to 12900,
        "stock_required" to false,
        // AUD-015：建票固定 draft，绑权益后经发布闸上架（status 字段服务端不再采信）。
        *fixtureMutationMetadata("ticket_type"),
      ),
    )
    val template = api.postJson<IdResponse>(
      "/api/v1/ticketing/entitlement-templates",
      obj(
        "name" to "P0 Android Entry $stamp",
        "entitlement_type" to "entry",
        "default_total_times" to 2,
        "default_value_cent" to 9900,
        "status" to "active",
        // 模板归属（merchant/venue/point）：发券时透传到 issued_entitlements，
        // 使终端按 merchant_id+venue_id 拉取的离线权益包能命中本券。
        // 缺省时券归属为空，会被按场地过滤的离线包排除（verifyOffline 找不到）。
        "merchant_id" to merchant.id,
        "venue_id" to venue.id,
        "point_id" to point.id,
        *fixtureMutationMetadata("entitlement_template"),
      ),
    )
    api.postJson<JsonObject>(
      "/api/v1/ticketing/ticket-types/${product.id}/entitlements",
      obj(
        "entitlement_template_id" to template.id,
        "quantity" to 1,
        "total_times" to 2,
        "value_amount_cent" to 9900,
        *fixtureMutationMetadata("ticket_type_entitlement"),
      ),
    )
    // AUD-019：direct 出票端点已退役（对假订单必 FK 回滚、对真订单可错票占坑）——统一走「发布→真实订单→按单出票」。
    api.postJson<JsonObject>(
      "/api/v1/ticketing/ticket-types/${product.id}/publish",
      obj(
        "reason" to "P0 fixture publish",
        *fixtureMutationMetadata("ticket_type_publish"),
      ),
    )
    val order = api.postJson<IdResponse>(
      "/api/v1/ticketing/orders",
      obj(
        "ticket_type_id" to product.id,
        "quantity" to 1,
        "sales_channel" to "admin",
        *fixtureMutationMetadata("ticket_order"),
      ),
    )
    val issued = api.postJson<IssuedTicketResponse>(
      "/api/v1/ticketing/orders/${order.id}/issue",
      obj(
        "reason" to "P0 fixture issue",
        *fixtureMutationMetadata("ticket_issue"),
      ),
    )
    val entitlementCode = requireNotNull(issued.issuedEntitlements.firstOrNull()?.entitlementCode) {
      "fixture did not issue entitlement"
    }

    return P0FixtureData(
      organizationId = organization.id,
      merchantId = merchant.id,
      venueId = venue.id,
      areaId = area.id,
      pointId = point.id,
      staffId = staff.id,
      entitlementCode = entitlementCode,
    )
  }

  private fun obj(vararg pairs: Pair<String, Any?>): JsonObject = buildJsonObject {
    for ((key, value) in pairs) {
      when (value) {
        null -> Unit
        is String -> put(key, value)
        is Int -> put(key, value)
        is Long -> put(key, value)
        is Boolean -> put(key, value)
        else -> put(key, value.toString())
      }
    }
  }

  private fun fixtureMutationMetadata(action: String): Array<Pair<String, Any?>> =
    arrayOf(
      "idempotency_key" to "android_p0_${action}_$stamp",
      "operator_note" to "Android P0 fixture setup",
    )
}

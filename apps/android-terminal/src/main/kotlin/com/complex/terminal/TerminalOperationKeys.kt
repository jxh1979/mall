package com.complex.terminal

// 文件版本：V0.01

import java.util.UUID

/** 没拿到结果的一次操作，最近一次发出或失败后这么久没再提交即当作放弃（不小于单次请求上限 30 秒）。 */
const val UNCONFIRMED_KEY_RETRY_MS = 60_000L

/** 在线核销：上一笔没拿到结果、本次扫了另一个码，先确认上一笔已核销成功时给员工的提示（本次的码没有提交）。 */
const val PREVIOUS_VERIFICATION_NOTICE = "上一笔核销已完成（此前没拿到结果，现已确认）；本次扫到的码没有提交，如是另一位客人请再扫一次"

private const val VERIFY_SLOT = "online-verification"

/**
 * 第十八批 BO 段第 261 条 261-B06-R1（PGM:303「同一个操作被重复提交，系统只认一次，不会因为重试就重复扣款、重复入账或重复核销……
 * 同一次操作重复提交时，返回第一次的结果并提示已完成。」；PGM:528「⑤同一笔操作重复提交不重复生效（重复点击、网络重发、渠道重复回调都只算一次）」）：
 * 安卓终端按「一次操作」持有请求编号的登记表（先例同包 [KioskPaymentKey]），按槽记请求内容（去掉编号）、编号与最近一次发出或失败的时刻。
 * 发出即记；同槽同内容、还没拿到明确结果的再提交沿用原编号（在途再点也是同一个编号），服务端据此只认一次、返回第一次的结果；
 * 拿到 2xx 或被明确拒绝（[isRejection]）后清；没拿到结果（没收到响应、5xx、401、408、429、请求正在处理中、该提交编号已被使用）保留并刷新时刻。
 * 不分首发与沿用编号的再次提交，一律按这一次的结果判。最近一次发出或失败后 [UNCONFIRMED_KEY_RETRY_MS] 没再提交即放弃——
 * 已知上限：过了 60 秒再提交同样内容按新的一次操作处理。换了登录的人清空全部槽，同一人重新登录照常沿用（服务端提交范围按登录人算）。
 */
class TerminalOperationKeys(private val now: () -> Long = System::currentTimeMillis) {
  private class Open(val content: Any, val key: String, var at: Long)

  private val slots = HashMap<String, Open>()
  private var staffId: String? = null

  /** 最近一次经本表提交是重复提交、服务端返回的是第一次的结果时的「已完成」提示（或 [PREVIOUS_VERIFICATION_NOTICE]）；否则为空。每次提交开始时置空。 */
  @Volatile
  var completedNotice: String? = null
    private set

  /** 登录成功后调用：与上一位登录的员工不同才清空全部槽。 */
  @Synchronized
  fun operator(staffId: String) {
    if (staffId != this.staffId) slots.clear()
    this.staffId = staffId
  }

  /**
   * 按槽提交一次操作：[send] 用本表给的编号发请求，返回结果与本次读到的已完成提示（发请求的同一线程紧跟其后读 lastReplayedNotice）。
   * [send] 不在同步块内执行，在途再点不等第一笔、带同一个编号发出。
   */
  fun <T> submit(slot: String, content: Any, prefix: String, send: (key: String) -> Pair<T, String?>): T {
    val key = begin(slot, content, prefix)
    val (result, notice) = try {
      send(key)
    } catch (error: Throwable) {
      failed(slot, key, error)
      throw error
    }
    done(slot, key, notice)
    return result
  }

  /**
   * 工作手机在线核销单槽（门票与游乐入口共用：都走票务核销入口、都核客人动态码，动态码默认 15 秒一刷，短于单次请求上限，按内容认不出重扫是同一次）。
   * 槽里有没拿到结果的上一笔、且内容与本次不同时，先用原编号原样重发上一笔（含人脸）：核销成功即返回它、提示 [PREVIOUS_VERIFICATION_NOTICE]，
   * 本次的码不提交；返回 2xx 但未核销成功、或被明确拒绝，清掉后用新编号提交本次；仍没有结果原样抛出，本次的码不提交。[request] 的编号由本表填。
   */
  fun verify(request: VerificationRequest, prefix: String, send: (VerificationRequest) -> Pair<VerificationResponse, String?>): VerificationResponse {
    val content = request.copy(idempotencyKey = "")
    val earlier = earlierVerification(content)
    if (earlier != null) {
      val response = try {
        send(earlier).first
      } catch (error: Throwable) {
        failed(VERIFY_SLOT, earlier.idempotencyKey, error)
        if (!isRejection(error)) throw error
        null
      }
      if (response != null) {
        done(VERIFY_SLOT, earlier.idempotencyKey, null)
        if (response.status == "success") {
          completedNotice = PREVIOUS_VERIFICATION_NOTICE
          return response
        }
      }
    }
    return submit(VERIFY_SLOT, content, prefix) { key -> send(content.copy(idempotencyKey = key)) }
  }

  @Synchronized
  private fun earlierVerification(content: VerificationRequest): VerificationRequest? {
    completedNotice = null
    val open = live(VERIFY_SLOT)?.takeIf { it.content != content } ?: return null
    open.at = now()
    return (open.content as VerificationRequest).copy(idempotencyKey = open.key)
  }

  @Synchronized
  private fun begin(slot: String, content: Any, prefix: String): String {
    completedNotice = null
    val open = live(slot)?.takeIf { it.content == content }
      ?: Open(content, "$prefix-${UUID.randomUUID()}", now()).also { slots[slot] = it }
    open.at = now()
    return open.key
  }

  @Synchronized
  private fun done(slot: String, key: String, notice: String?) {
    if (slots[slot]?.key == key) slots.remove(slot)
    completedNotice = notice
  }

  @Synchronized
  private fun failed(slot: String, key: String, error: Throwable) {
    val open = slots[slot]?.takeIf { it.key == key } ?: return
    if (isRejection(error)) slots.remove(slot) else open.at = now()
  }

  private fun live(slot: String): Open? {
    val open = slots[slot] ?: return null
    if (now() - open.at <= UNCONFIRMED_KEY_RETRY_MS) return open
    slots.remove(slot)
    return null
  }
}

/**
 * 261-B06-R1：被服务端明确拒绝——4xx，但不是 401（会话失效）、408（请求超时）、429（限速），detail_code 也不是
 * IDEMPOTENCY_IN_PROGRESS（请求正在处理中）、IDEMPOTENCY_KEY_USED（该提交编号已被使用）；IDEMPOTENCY_CONFLICT 算明确拒绝。
 */
internal fun isRejection(error: Throwable): Boolean =
  error is ApiException && error.statusCode in 400..499 && error.statusCode !in setOf(401, 408, 429) &&
    apiDetailCode(error) !in setOf("IDEMPOTENCY_IN_PROGRESS", "IDEMPOTENCY_KEY_USED")

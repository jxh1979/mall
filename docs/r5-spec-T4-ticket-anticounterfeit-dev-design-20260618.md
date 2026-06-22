# T4 二维码折中防伪 可编码开发规格 V0.1（2026-06-18）

> **任务**：R5 backlog **T4（二维码折中防伪）**，W1 票务域。
> **裁决依据**：裁决①——P0 = 折中（哈希 + 核销端强制 30/60s 刷新窗口校验 + nonce 防重放，截图窗口内失效）；**HMAC 签名版本化降 P1**（本期仅预留字段位，不写签名逻辑）。
> **现状基准**（已核）：`ticket_credentials`(V009) 有 `credential_token/nonce/credential_hash/expires_at/status/refreshed_count` + `refresh_interval_seconds`(V064,{30,60})；`TicketVerificationService.refreshCredential` 硬编码 `expires_at=now+300s`、与 `refresh_interval_seconds` **未联动**；`credentialByPayload` 只校验 token 存在 + status=active + 未过期，**不校验 nonce/刷新窗口**；`ticket_verifications`(V006) 无 nonce/verified_people/staff_id/merchant_id/signature_valid，无 reversed 态。
> **迁移**：取下一连续版本（示意 **V068**，实际以合并顺序为准）；测试用 `MigrationVersions.latest()`。

---

## 1. 数据模型（迁移草案）

```sql
-- 1) ticket_credentials：补刷新窗口锚点 + 预留签名位（P1 不写逻辑）
ALTER TABLE ticket_credentials
  ADD COLUMN IF NOT EXISTS window_started_at TIMESTAMPTZ,        -- 当前刷新窗口起点
  ADD COLUMN IF NOT EXISTS signature_value VARCHAR(256),         -- P1 预留：HMAC 签名值
  ADD COLUMN IF NOT EXISTS signature_version VARCHAR(16);        -- P1 预留：签名版本

-- 2) ticket_verifications：补 nonce 防重放 + 事后核对维度 + 冲正态
ALTER TABLE ticket_verifications
  ADD COLUMN IF NOT EXISTS nonce VARCHAR(128),
  ADD COLUMN IF NOT EXISTS staff_id VARCHAR(64),
  ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64),
  ADD COLUMN IF NOT EXISTS window_valid BOOLEAN,                 -- 核销时是否在有效刷新窗口
  ADD COLUMN IF NOT EXISTS signature_valid BOOLEAN;              -- P1：签名校验结果（本期恒 NULL）
-- 防重放：同一凭证 nonce 只能成功核销一次
CREATE UNIQUE INDEX IF NOT EXISTS uq_ticket_verifications_nonce
  ON ticket_verifications(entitlement_id, nonce) WHERE status = 'success';
```
> 不新建 nonce 黑名单表：用 `ticket_verifications` 成功核销记录 + 唯一索引实现 nonce 一次性（重放第二次撞唯一键→拒绝）。`reversed` 冲正态由 T5 引入（本规格只加 `window_valid`/`nonce`）。

## 2. 逻辑变更（`TicketVerificationService`）

**刷新** `refreshCredential`：
- `expires_at = now + refresh_interval_seconds`（取票种级 {30,60}，**替换硬编码 300s**）；`window_started_at = now`；每次刷新换新 `credential_token` + 新 `nonce`，`refreshed_count++`。
- 返回给客户端含 `refresh_interval_seconds`（驱动端侧按 30/60s 刷新）。

**核销** `createVerification`/`credentialByPayload`：按序校验，任一失败 → status=failed + result_reason：
1. 凭证存在且 `status=active`。
2. **刷新窗口**：`now ≤ expires_at`（即在当前 30/60s 窗口内）→ 否则 `result_reason=window_expired`，`window_valid=false`。截图离线展示的旧 token 因过窗口被拒。
3. **nonce 防重放**：该 (entitlement_id, nonce) 未成功核销过 → 否则 `result_reason=replayed`。
4. **哈希**：`credential_hash` 比对一致。
5. 通过 → 记 `ticket_verifications`（含 nonce/staff_id/merchant_id/window_valid=true），消费权益。

> P1（签名版本化）：预留 `signature_value/signature_version`，待 P1 接 HMAC 签名算法 + 核销端 `signature_valid` 校验。本期 `signature_valid` 恒 NULL，不阻断。

## 3. 事后核对（修核对维度缺口）
`ticket_verifications` 现可按 票号/entitlement/credential_hash(payload)/device_id/point_id/**staff_id**/**merchant_id**/时间/status/**window_valid** 多维核对，补齐 HTML"按员工/防伪结果核对"。

## 4. API / 事件 / 权限
- 端点复用既有票务核销/刷新接口；核销响应补 `window_valid`、result_reason 枚举（active/window_expired/replayed/hash_mismatch/consumed）。
- 事件：`ticket.credential_refreshed`、`ticket.verification_succeeded`、`ticket.verification_rejected(reason)`。
- 权限：复用既有 `ticket.verify.execute`。

## 5. 验收（GWT）
- Given 凭证刷新窗口为 60s，When 用 61s 前的旧 token 核销，Then 拒绝 window_expired。
- Given 同一凭证 nonce 已成功核销，When 再次提交同 nonce，Then 拒绝 replayed（唯一索引兜底）。
- Given 截图（窗口内旧帧），When 超过 refresh_interval_seconds 后核销，Then 失效。
- Given 核销成功，Then ticket_verifications 含 staff_id/merchant_id/window_valid=true 可事后核对。
- P1 占位：signature_value/version 列存在但本期无写入逻辑。

## 6. 测试 / 依赖 / claims
- 测试：窗口过期拒绝、nonce 重放拒绝（含并发双提交只成功一笔）、refresh 使用票种级间隔、核对维度查询；`MigrationVersions.latest()` 断言。
- 依赖：`ticket_credentials`(V009/V064)、`issued_entitlements`、`refresh_interval_seconds`。
- **claims 同步**：REQ-06/08/09 行——signature_version 保持 absent（P1）；新增 window_started_at/ticket_verifications.nonce/window_valid present；矩阵 R5 该行备注更新为"折中防伪已落（窗口+nonce），签名 P1"。
- **与 T5 协同**：T5 的 verified_people/reversed 在同表 ALTER，建议与 T4 合并一个迁移避免版本冲突。

## 7. 变更日志
| 日期 | 内容 | 依据 |
|---|---|---|
| 2026-06-18 | 新建 T4 折中防伪规格（刷新窗口校验+nonce 防重放+核对维度+签名 P1 预留） | R5 backlog T4；裁决①；现状取自 V009/V064 与 TicketVerificationService |

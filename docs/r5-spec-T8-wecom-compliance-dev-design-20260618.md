# T8 企微平台侧合规能力 可编码开发规格 V0.1（2026-06-18）

> **⚠ 【已作废（2026-08-03 L-19 裁决 `DEC-CFG-2026-08-03-10`）】** 企微会话原文**永久保存、不自动到期删除**；**本文档全部留存期／到期归档／销毁／匿名化规格（含默认 180 天、`retention_days`、`expiry_action`、`@Scheduled` 到期处理及相关 GWT 验收项）一律不得实施**；确需清理仅限**最高权限本人插入授权 U 盘密钥手动执行**（逐条填写理由、先试运行预览、清理动作本身另行留痕）。现行口径见 PGM「企业微信工作会话永久保存」条与 BSM L1810。**以下原文一律保留，仅作历史留痕，不构成可实施规格。**（GAP-25 域外清理，2026-08-05）

> **任务**：R5 backlog **T8**，W1（合规）。
> **裁决依据**：裁决⑥ + 二轮——平台侧合规能力（可配置留存/到期归档销毁匿名化/权限脱敏/查看写访问审计）本期 P0，**默认留存 180 天**；企微**同步接口按第三方原则预留 + 后台开关**（依赖 T2 开关闸口）。　**【已作废（2026-08-03 L-19 裁决 `DEC-CFG-2026-08-03-10`）】企微会话原文永久保存、不自动到期删除；本处销毁／留存期／到期处置规格不得实施；确需清理仅限最高权限本人插授权 U 盘密钥手动执行（逐条填写理由、先试运行预览、清理动作本身另行留痕）。原文保留仅作历史留痕。**
> **现状基准**（已核）：企微仅 `integration_read_ledger` 的 mock 种子（V015 `wecom_conversation/message/risk_hit`，provider=mock-provider）；无留存表/上传接口/到期触发/脱敏链/查看审计。可复用 `AiTextRedactor`（脱敏）与 `TerminalSyncPayloadSanitizer`。`V059.capture_source` 已含 `wecom_archive`。
> **迁移**：取下一连续版本（示意 **V071**）；测试用 `MigrationVersions.latest()`。

---

## 1. 数据模型（迁移草案）
```sql
-- 留存策略（可配置，默认 180 天）
    -- 【已作废（2026-08-03 L-19 裁决 `DEC-CFG-2026-08-03-10`）】企微会话永久保存、不自动到期删除；retention_days／expiry_action 与到期作业不得实施
CREATE TABLE IF NOT EXISTS wecom_retention_policies (
    id VARCHAR(64) PRIMARY KEY,
    scope_type VARCHAR(32) NOT NULL DEFAULT 'global',     -- global/project/merchant
    scope_id VARCHAR(64) NOT NULL DEFAULT '*',
    retention_days INTEGER NOT NULL DEFAULT 180,
    expiry_action VARCHAR(16) NOT NULL DEFAULT 'anonymize', -- archive/delete/anonymize
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_wecom_ret_action CHECK (expiry_action IN ('archive','delete','anonymize')),
    CONSTRAINT uq_wecom_ret_scope UNIQUE (scope_type, scope_id)
);

CREATE TABLE IF NOT EXISTS wecom_conversations (
    id VARCHAR(64) PRIMARY KEY,
    external_conversation_id VARCHAR(128) NOT NULL UNIQUE,
    project_id VARCHAR(64) NOT NULL,
    owner_staff_id VARCHAR(64),
    customer_ref VARCHAR(128),
    archive_status VARCHAR(32) NOT NULL DEFAULT 'active',  -- active/archived/anonymized/deleted
    retention_until DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_wecom_conv_status CHECK (archive_status IN ('active','archived','anonymized','deleted'))
);

CREATE TABLE IF NOT EXISTS wecom_messages (
    id VARCHAR(64) PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL REFERENCES wecom_conversations(id) ON DELETE CASCADE,
    sender_ref VARCHAR(128),
    content_raw_ref TEXT,            -- 原文引用/密文（仅授权可解）
    content_redacted TEXT,           -- 脱敏文本（普通展示用）
    risk_flags JSONB NOT NULL DEFAULT '[]',
    anonymized BOOLEAN NOT NULL DEFAULT false,
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 查看访问审计（看原文必写）
CREATE TABLE IF NOT EXISTS wecom_access_audits (
    id VARCHAR(64) PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL,
    viewer_user_id VARCHAR(64) NOT NULL,
    view_type VARCHAR(16) NOT NULL,         -- redacted/raw
    approval_ref VARCHAR(64),               -- raw 必须带审批引用
    viewed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_wecom_access_type CHECK (view_type IN ('redacted','raw'))
);
```

## 2. 逻辑（`WecomConversationService` + 定时任务）
- **同步接口（预留+开关）**：`POST /api/v1/integration/wecom/conversations`（webhook 入），受 **T2 能力开关** `integration.wecom.sync` 门控，默认关闭；开启后落 wecom_conversations/messages，写入时调 `AiTextRedactor` 生成 `content_redacted`、按规则置 risk_flags，`retention_until = sent_at + policy.retention_days`。
- **到期处理（@Scheduled）**：扫 `retention_until < today` 且 active 的会话/消息，按 `policy.expiry_action` 执行 archive/delete/anonymize（anonymize=清原文引用 + content_redacted 不可逆化、置 anonymized=true / archived 状态）。　**【已作废（2026-08-03 L-19 裁决 `DEC-CFG-2026-08-03-10`）】企微会话原文永久保存、不自动到期删除；本处销毁／留存期／到期处置规格不得实施；确需清理仅限最高权限本人插授权 U 盘密钥手动执行（逐条填写理由、先试运行预览、清理动作本身另行留痕）。原文保留仅作历史留痕。**
- **分级查看**：
  - 普通展示 → 返回 `content_redacted`，写 `wecom_access_audits(view_type=redacted)`。
  - 查看原文 → 需 `wecom.view_raw` 权限 + 最小授权 + 审批（`approval_ref` 必填，可走 supervisor_review）→ 返回原文并写 `wecom_access_audits(view_type=raw, approval_ref)`。
- **质检**：违规命中写 risk_flags（规则码后置，P0 先留字段；与 analytics AI 质检衔接）。

## 3. API / 事件 / 权限
- `POST /integration/wecom/conversations`（开关门控）；`GET /wecom/conversations/{id}/messages?mode=redacted|raw`；`POST /wecom/retention-policies`。
- 事件：`wecom.conversation_synced`、`wecom.retention_applied`、`wecom.raw_viewed`。
- 权限：`wecom.view`（脱敏）、`wecom.view_raw`（原文，敏感）、`wecom.retention.manage`。

## 4. 验收（GWT）
- Given 留存策略默认 180 天，When 配置改 365，Then 新会话按新期限计算 retention_until。　**【已作废（2026-08-03 L-19 裁决 `DEC-CFG-2026-08-03-10`）】企微会话原文永久保存、不自动到期删除；本处销毁／留存期／到期处置规格不得实施；确需清理仅限最高权限本人插授权 U 盘密钥手动执行（逐条填写理由、先试运行预览、清理动作本身另行留痕）。原文保留仅作历史留痕。**
- Given 会话超 retention_until，When 定时任务运行，Then 按 expiry_action 归档/销毁/匿名化。　**【已作废（2026-08-03 L-19 裁决 `DEC-CFG-2026-08-03-10`）】企微会话原文永久保存、不自动到期删除；本处销毁／留存期／到期处置规格不得实施；确需清理仅限最高权限本人插授权 U 盘密钥手动执行（逐条填写理由、先试运行预览、清理动作本身另行留痕）。原文保留仅作历史留痕。**
- Given 普通用户，When 查会话，Then 仅得脱敏文本且写 redacted 审计。
- Given 查看原文，When 无审批，Then 拒绝；有 wecom.view_raw + approval_ref，Then 返回原文并写 raw 审计。
- Given 同步开关关闭，When webhook 入，Then 拒绝（T2 闸口）。

## 5. 测试 / 依赖 / claims
- 测试：留存到期三动作、脱敏/原文分级 + 审计落库、开关门控、retention_until 计算；`MigrationVersions.latest()`。　**【已作废（2026-08-03 L-19 裁决 `DEC-CFG-2026-08-03-10`）】企微会话原文永久保存、不自动到期删除；本处销毁／留存期／到期处置规格不得实施；确需清理仅限最高权限本人插授权 U 盘密钥手动执行（逐条填写理由、先试运行预览、清理动作本身另行留痕）。原文保留仅作历史留痕。**
- 依赖：T2 开关、`AiTextRedactor`、supervisor_review（原文审批可选）、analytics 质检（risk_flags 衔接）。
- **claims**：wecom_conversations/wecom_messages/wecom_retention_policies/wecom_access_audits present；矩阵 REQ-54 行下修转"已落地（平台侧）"。
- **剩余未决**：企微授权配置由谁完成（上线前置 owner）；违规规则码集合。

## 6. 变更日志
| 日期 | 内容 | 依据 |
|---|---|---|
| 2026-06-18 | 新建 T8 规格（留存策略默认180天+到期归档销毁匿名化定时+分级脱敏+原文审批审计+同步开关门控） | R5 backlog T8；裁决⑥+二轮；现状取自 V015/V059 |

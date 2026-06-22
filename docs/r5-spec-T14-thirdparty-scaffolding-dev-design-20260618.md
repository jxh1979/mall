# T14 第三方接口/开关预留骨架 可编码开发规格 V0.1（2026-06-18）

> **任务**：R5 backlog **T14**（W2），集成域。
> **裁决依据**：裁决③通用原则——**凡涉及第三方一律预留接口 + 后台开关控制开放、不阻塞其它功能**；本期只做接口/开关/字段映射占位，真实回流/对账/被动留痕/视频锚点待厂商资料 + 开关开启后交付。
> **现状基准**（已核）：`integration_provider_adapters`(V015，adapter_type 含 payment/ticket_channel/face_age/wecom/robot/video/device/retail/catering)、`open_endpoint_configs`、`IntegrationService/IntegrationOrderSyncService/IntegrationReconciliationService` 已在；webhook 入站仅落 `open_api_events`/`integration_read_ledger` 信封不解析映射；机器人仅只读端点无写回调；视频仅 camera 一行无锚点；对账靠手填。开关复用 **T2** `terminal_capability_switches`。
> **迁移**：取下一连续版本（示意 **V076**）；测试用 `MigrationVersions.latest()`。

---

## 1. 统一骨架（每类第三方一致）
每类第三方提供四件套（占位即可，受开关门控，默认关闭）：
1. **接口/Webhook 入口**：`POST /api/v1/integration/{type}/webhook`（受 T2 开关 `integration.{type}.sync` 门控，关→拒绝）。
2. **字段映射占位**：`integration_provider_adapters` 适配器配置 + payload→标准模型 mapping stub（厂商资料到位前空映射，仅落信封）。
3. **对账映射 stub**：回流→`integration_reconciliation_*` 的差异生成接口预留（当前手填，自动化待开关）。
4. **能力开关**：种子 `terminal_capability_switches` 各 `integration.{type}.*` 默认 enabled=false。

类型：餐饮 POS、零售 POS、机器人、视频监控、企微同步（与 T8 协同）、第三方售票（美团/大众点评/抖音）、支付渠道、人脸年龄。

## 2. 数据模型（迁移草案，补两处硬缺口字段）
```sql
-- 机器人被动留痕：写回调（当前仅只读）—— 与 T17 dispatch 段对接
CREATE TABLE IF NOT EXISTS integration_robot_task_events (
    id VARCHAR(64) PRIMARY KEY,
    robot_task_ref VARCHAR(128) NOT NULL,
    event_type VARCHAR(32) NOT NULL,      -- created/picked/delivering/delivered/failed
    handover_id VARCHAR(64),              -- 关联 T17 交接
    payload JSONB NOT NULL DEFAULT '{}',
    reported_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_robot_event_type CHECK (event_type IN ('created','picked','delivering','delivered','failed'))
);

-- 视频监控"点位+时间锚点"预留 + 调阅审计
CREATE TABLE IF NOT EXISTS integration_video_anchors (
    id VARCHAR(64) PRIMARY KEY,
    point_id VARCHAR(64) NOT NULL,
    event_time TIMESTAMPTZ NOT NULL,
    business_ref VARCHAR(128),            -- 关联业务动作（核销/交接/异常）
    clip_index_ref VARCHAR(256),          -- 后续匹配的监控片段索引（厂商接入后填）
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE IF NOT EXISTS integration_video_access_audits (
    id VARCHAR(64) PRIMARY KEY,
    anchor_id VARCHAR(64) REFERENCES integration_video_anchors(id),
    viewer_user_id VARCHAR(64) NOT NULL,
    approval_ref VARCHAR(64),
    viewed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```
> 视频本期只**写锚点（点位+时间）**（关键动作发生即记 point_id+event_time+business_ref），`clip_index_ref` 留空待厂商；调阅敏感→`integration_video_access_audits`（最小授权+审批）。机器人写回调供 T17 被动留痕。

## 3. 逻辑
- `IntegrationService`：各 type webhook 统一入口 + 开关门控 + 落信封；mapping 为可插拔 adapter（占位）。
- 机器人：`POST /api/v1/integration/robot/report` 写 `integration_robot_task_events`（被动留痕），与 T17 `dispatch` 段对接。
- 视频锚点：业务侧关键动作（核销/交接/异常）调 `recordVideoAnchor(pointId, eventTime, businessRef)`；调阅走审计。
- 第三方售票/餐饮/零售：回流落 `integration_order_syncs`（已有），自动对账差异 stub（开关开启后启用）。

## 4. API / 事件 / 权限
- `POST /integration/{type}/webhook`（门控）、`POST /integration/robot/report`、`recordVideoAnchor`（内部）、`GET /integration/video-anchors`（调阅，审计）。
- 事件：`integration.webhook_received`、`integration.robot_reported`、`integration.video_anchor_recorded`、`integration.video_clip_viewed`。
- 权限：`integration.adapter.write`（已有）、`integration.video.view`（调阅，敏感）。

## 5. 验收（GWT）
- Given 某第三方开关关闭，When webhook 入，Then 拒绝（T2 闸口）。
- Given 开关开启 + 厂商映射未配，Then 落信封 status=accepted，不伪装已对账。
- Given 关键业务动作，When 发生，Then 写 video_anchor（point_id+event_time），clip_index_ref 留空。
- Given 机器人上报，Then 写 robot_task_events 被动留痕，可被 T17 dispatch 段引用。
- Given 调阅视频锚点，Then 需权限 + 写 video_access_audits。

## 6. 测试 / 依赖 / claims
- 测试：开关门控拒绝、信封落库不伪造对账、视频锚点写入、机器人回调、调阅审计；`MigrationVersions.latest()`。
- 依赖：T2 开关；T8（企微同步同此骨架）；T17（机器人段对接）。
- **claims**：integration_robot_task_events/integration_video_anchors/integration_video_access_audits present；matrix REQ-03/41/70 备注更新为"接口+开关预留、视频锚点已落、真实回流待厂商"。
- **口径**：明确"边界完成、厂商暂缓"——不把占位当真实回流闭环。

## 7. 变更日志
| 日期 | 内容 | 依据 |
|---|---|---|
| 2026-06-18 | 新建 T14 规格（统一第三方四件套骨架+开关门控+机器人写回调+视频点位时间锚点+调阅审计） | R5 backlog T14；裁决③；现状取自 V015 与 IntegrationService |

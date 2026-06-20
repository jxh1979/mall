# 架构总览 / Architecture Overview V0.1

> 文档goalP2(2026-06-20)：本文为综合体管理系统的架构总览与架构图集（Mermaid，GitHub 可渲染）。模块名以 `backend/src/main/java/com/complex/platform/*` 真实包、`apps/web/src/*` 真实壳、`docs/openapi/openapi-p0.*` 契约为准。新功能承接见 `docs/v099-v124-feature-implementation-and-acceptance-spec-v0.1.md`。

## 1. 系统上下文（System Context）

```mermaid
graph TB
  subgraph Users["使用者 / Actors"]
    GUEST["客人 Guest<br/>(App/小程序/Web)"]
    STAFF["现场员工 Staff<br/>(工作手机/平板/一体机)"]
    ADMIN["管理/运营/财务 Admin"]
    MERCHANT["商户 Merchant"]
    SUPPLIER["第三方供应商 Supplier"]
  end

  subgraph Platform["综合体管理平台 Entertainment Complex Platform"]
    WEB["前端 Web<br/>(admin / customer / legacy)"]
    ANDROID["Android 终端<br/>(工作手机/平板/kiosk)"]
    API["后端 API<br/>Java 21 + Spring Boot 4"]
    DB[("PostgreSQL<br/>Flyway V001..V124")]
  end

  subgraph External["外部依赖 (能力开关默认 off + mock)"]
    TP["第三方<br/>美团/点评/抖音/餐饮POS/零售POS"]
    PAY["聚合支付 / 人脸年龄核验"]
    ROBOT["机器人 / 视频监控"]
    WECOM["企业微信"]
    MDM["商业 MDM (AMAPI)"]
    NAS["NAS 备份恢复 (160)"]
  end

  GUEST --> WEB
  STAFF --> ANDROID
  ADMIN --> WEB
  MERCHANT --> WEB
  WEB --> API
  ANDROID --> API
  API --> DB
  API -. webhook/回流(开关化) .-> TP
  API -. 鉴权调用(开关化) .-> PAY
  API -. 被动留痕(开关化) .-> ROBOT
  API -. 会话/质检(开关化) .-> WECOM
  ANDROID -. 纳管(待签约) .-> MDM
  API -. 备份/恢复 .-> NAS
  SUPPLIER -. 动态鉴权读写 .-> API
```

## 2. 后端域模块地图（Backend Domain Modules）

后端按业务域分包（`com.complex.platform.*`）。治理/网关为底座，业务域在其上，留痕/分析为横切。

```mermaid
graph TB
  subgraph Edge["接入与底座 Edge & Foundation"]
    AUTH["auth<br/>认证/会话"]
    GATE["gate<br/>终端开关+准入闸 AdmissionGuard"]
    ACCESS["access<br/>权限/职级/有效权限集"]
    GOV["governance<br/>综合体/空间/商户/合同/主管复核"]
    TERM["terminal<br/>设备台账/凭证/MDM/生命周期"]
  end

  subgraph Biz["业务域 Business Domains"]
    TKT["ticketing<br/>票务/权益/核销/积分引擎"]
    BAR["bar<br/>卡座/点酒/账单/存酒/班结/void冲销"]
    INV["inventory<br/>供应链/库存/盘点差异/追溯交接"]
    AMU["amusement<br/>项目/场次/容量/超容/安全巡检"]
    CUS["customer<br/>会员/钱包/积分/客人端"]
    MB["merchantbilling<br/>商户账单/租金/保证金/CAM"]
    MS["merchantservice<br/>物业工单/SLA"]
  end

  subgraph Cross["横切 Cross-cutting"]
    FIN["finance<br/>AR/AP/日结/对账/退款/综合体计费"]
    EVID["evidence<br/>留痕/统一异常责任链 risk_alerts"]
    INTEG["integration<br/>第三方回流/渠道语义/对账/开放平台"]
    COMP["compliance<br/>企微会话/违规命中/PIPL"]
    ANA["analytics<br/>经营看板/集团视图/AI质检"]
  end

  GATE --> Biz
  GOV --> Biz
  ACCESS --> Biz
  Biz --> FIN
  Biz --> EVID
  Biz --> ANA
  INTEG --> FIN
  COMP --> EVID
  TKT --> CUS
  BAR --> INV
  AMU --> TKT
```

> 模块对应代码包（实证）：`access` / `amusement` / `analytics` / `auth` / `bar` / `common` / `compliance` / `customer` / `evidence` / `finance` / `floorplan` / `gate` / `governance` / `integration` / `inventory` / `merchantbilling` / `merchantservice` / `ops` / `system` / `terminal` / `ticketing`。

## 3. 分层与数据流（Layering & Request Flow）

```mermaid
graph LR
  UI["前端/终端"] -->|"REST + Bearer"| CTRL["Controller<br/>(28 个)"]
  CTRL -->|"@GuardedAction<br/>权限+终端开关+准入闸"| SVC["Service<br/>(58 个)"]
  SVC -->|"JdbcTemplate<br/>scope 注入"| REPO["Repository (JDBC)"]
  REPO --> PG[("PostgreSQL")]
  SVC -->|"事件"| AUD["audit_events / risk_alerts"]
  SVC -. "幂等键" .-> IDEM["IdempotencyService"]
  CTRL -. "OpenAPI 契约" .-> OAS["openapi-p0 (386 op)"]
```

每个写动作经 `@GuardedAction`（权限点 + 终端开关 + 准入闸 AdmissionGuard）→ Service（业务+幂等+留痕）→ JDBC Repository（强制注入 merchant_id/project_id scope）→ PostgreSQL，并发布审计事件；异常进统一责任链 `risk_alerts`。

## 4. 关键业务链（示例：购票→核销→财务/留痕）

```mermaid
sequenceDiagram
  participant G as 客人(customer端)
  participant T as ticketing
  participant C as customer(积分)
  participant W as 工作手机
  participant F as finance
  participant E as evidence
  G->>T: 下单购票 + 支付
  T->>C: 积分入账 pending (V100/V101)
  G->>W: 出示验票二维码(防伪刷新)
  W->>T: 核销 verify
  T->>C: 积分转 confirmed
  T->>F: 收入进日结/对账
  T->>E: 核销留痕 + 异常→risk_alerts
  Note over T,F: 退款时积分按比例 deduct,<br/>核销冲正仅留痕(无独立财务对象)
```

## 5. 前端与终端（Frontend & Terminals）

```mermaid
graph TB
  MAIN["main.tsx<br/>(路径前缀分流,默认 admin)"]
  MAIN --> ADM["AdminApp<br/>管理后台(31页,路由级 lazy 分包)"]
  MAIN --> CUST["CustomerApp<br/>客人端(购票/订台/积分/验票)"]
  MAIN --> LEG["LegacyConsoleApp<br/>(已退役,跳转 admin)"]
  ADM --> APIc["后端 REST"]
  CUST --> APIc
  ANDR["Android 终端<br/>(工作手机/平板/kiosk)"] --> APIc
```

## 6. 交付闸口与验证（Delivery Gates）

`verify:delivery-gates`（约 50 子闸聚合）+ `verify:p0-contract` + `verify:tenant-isolation --strict` + 后端 `mvn test`（1251）+ 前端 `e2e`（42）+ semgrep。生产上线由 `verify:production-blockers` 把关（外部依赖未到位即阻断）。

---
版本：V0.1 ｜ 最后更新：2026-06-20 ｜ 状态：现行 ｜ 配套：`README.md` / `README.en.md` / `docs/documentation-map-v0.1.md`

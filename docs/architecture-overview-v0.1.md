# 架构总览 / Architecture Overview V0.1

> 文档goalP2(2026-06-20)：本文为综合体管理系统的架构总览与架构图集（Mermaid，GitHub 可渲染）。**业务名称、子系统入口与终端边界以三份 HTML（业务系统地图、平台治理地图、业务枚举速查表）中经用户批准的内容为准**；发生冲突时 PRD、其他文档、代码包名、OpenAPI 契约、后台配置、设计和实现均不得覆盖。下列 Java 包、前端壳与 `openapi-p0` 仅为实现分包与工程契约，不是覆盖 HTML 的业务总图。新功能承接见 `docs/v099-v124-feature-implementation-and-acceptance-spec-v0.1.md`。

## 1. 系统上下文（System Context）

```mermaid
graph TB
  subgraph Users["使用者 / Actors"]
    GUEST["客人 Guest<br/>(App/小程序，无网页端)"]
    STAFF["现场员工 Staff<br/>(工作手机/平板/一体机)"]
    ADMIN["管理/运营/财务 Admin"]
    MERCHANT["商户 Merchant"]
    SUPPLIER["第三方供应商 Supplier"]
  end

  subgraph Platform["综合体管理平台 Entertainment Complex Platform"]
    WEB["前端 Web<br/>(业务人员；客人无网页端)"]
    ANDROID["Android 终端<br/>(工作手机/平板/kiosk)"]
    API["后端 API<br/>Java 21 + Spring Boot 4"]
    DB[("PostgreSQL<br/>Flyway V001..V151")]
  end

  subgraph External["外部依赖 (能力开关默认 off + mock)"]
    TP["第三方<br/>美团/点评/抖音/餐饮POS/零售POS"]
    PAY["聚合支付 / 人脸年龄核验"]
    ROBOT["机器人 / 视频监控"]
    WECOM["企业微信"]
    MDM["商业 MDM (AMAPI)"]
    NAS["NAS 备份恢复 (160)"]
  end

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

## 2. 后端代码分包（实现细化，不覆盖业务总图）

后端按代码包分包（`com.complex.platform.*`）。**下图英文包名是实现分包，不是与票务 / 酒吧并列的业务子系统闭集**，也不把 `merchantbilling` / `compliance` / `gate` 立成 HTML 子系统入口。业务总图以 MAP 子系统入口与 GOV 模块入口为准：基础管理、票务权益、客人端、酒吧平板/工作手机、触摸屏一体机、酒吧管理、供应链库存、游乐管理、商户管理、招商租赁、物业服务、楼宇运营、第三方接入、留痕与异常、财务对账、持股分红、经营看板、各类入口。平台治理层不是一个业务部门，而是所有业务的底座。

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
    MB["merchantbilling<br/>商户账单/租金/押金/物业费"]
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

> 下列为代码包实证（实现细化，**不是**业务子系统闭集，不得覆盖 MAP/GOV 入口）：`access` / `amusement` / `analytics` / `auth` / `bar` / `common` / `compliance` / `customer` / `evidence` / `finance` / `floorplan` / `gate` / `governance` / `integration` / `inventory` / `merchantbilling` / `merchantservice` / `ops` / `system` / `terminal` / `ticketing`。

## 3. 分层与数据流（Layering & Request Flow）

```mermaid
graph LR
  UI["前端/终端"] -->|"REST + Cookie(后台Web,HttpOnly)/Bearer(设备端)"| CTRL["Controller<br/>(29 个)"]
  CTRL -->|"@GuardedAction<br/>权限+终端开关+准入闸"| SVC["Service<br/>(60 个)"]
  SVC -->|"JdbcTemplate<br/>scope 注入"| REPO["Repository (JDBC)"]
  REPO --> PG[("PostgreSQL")]
  SVC -->|"事件"| AUD["audit_events / risk_alerts"]
  SVC -. "幂等键" .-> IDEM["IdempotencyService"]
  CTRL -. "OpenAPI 契约" .-> OAS["openapi-p0 (413 op)"]
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
  T->>C: 履约完成后产生积分和成长值（退款期内冻结）
  G->>W: 出示验票二维码(防伪刷新)
  W->>T: 核销 verify
  T->>C: 退款期结束后积分转为可用
  T->>F: 收入进日结/对账
  T->>E: 核销留痕 + 异常→risk_alerts
  Note over T,F: 退款/冲正形成反向结算，<br/>记录财务、积分、成长值和权益冲正
```

## 5. 前端与终端（Frontend & Terminals）

```mermaid
graph TB
  MAIN["main.tsx<br/>(路径前缀分流,默认 admin)"]
  MAIN --> ADM["AdminApp<br/>管理后台(33页,路由级 lazy 分包)"]
  CUST["客人购票/订台/积分在 App 或小程序<br/>（客人无网页端）"]
  ADM --> APIc["后端 REST"]
  CUST --> APIc
  ANDR["Android 终端<br/>(工作手机/平板/kiosk)"] --> APIc
```

## 6. 交付闸口与验证（Delivery Gates）

`verify:delivery-gates`（约 50 子闸聚合）+ `verify:p0-contract` + `verify:tenant-isolation --strict` + 后端 `mvn test`（1412）+ 前端 `e2e`（42）+ semgrep。生产上线由 `verify:production-blockers` 把关（外部依赖未到位即阻断）。

> 📌 **前端测试路径口径**：前端单测采用 **colocated 范式**——测试文件与被测源码同目录（`apps/web/src/**/*.test.tsx`，如 `apps/web/src/admin/pages/*.test.tsx`），由 vitest 就地发现。**`apps/web/tests` 不是正式测试目录**（当前为空 0 文件）；任何按 `apps/web/tests` 路径找前端测试而得出"无测试"的审计结论均为路径误判，应改查 colocated 的 `src/**/*.test.tsx`。前端 `e2e`（Playwright）另见 `playwright*.config.ts`。

---
版本：V0.1 ｜ 最后更新：2026-06-20 ｜ 状态：现行 ｜ 配套：`README.md` / `README.en.md` / `docs/documentation-map-v0.1.md`

CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M015 B5-r3 修复后全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-20T05:23:09Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`；B3-r3 after tree `3036d0c089905520aaa86fddb7a75eb75409b019`。
- 模块 M015 23 文件（§1 全读），90 判据（BEQR 22、BSM 14、GEN 18、PGM 36）；B3 累计 3（上限）：本轮任一 FAIL 即 BLOCKED-3ROUNDS。
- 顺序：Grok 已先派（modules/M015-b5-r3-grok-raw.md 存在=False），本报告落盘前未读其正文。只读承诺：未改任何文件；测试只导出三个 DB 变量、complex_test。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanController.java | 95 | 4323 | 33e14ba8868dd24c5235ba55658e51a38c7ff52572d3cb59fd10c07849941bef | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanRecord.java | 21 | 503 | 9db3b6126ab0ca3e624e857684f68e680d7c99f9fe3e0ef6cf8a31080d373bce | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java | 51 | 2148 | 9c5e50bff229838722ac00cc9096a8a188b111731be90da291a25c1f43c986a7 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 689 | 32061 | 5cad799d091fd43b58d99ff05d12e10afbc16e63fb7e59857dcd94c042ccdd80 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java | 454 | 21976 | bb3fed48fb09410e7532c6930e7d3b2a3c83080490782cd2d04736b03cf502f3 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportCandidateRecord.java | 23 | 596 | 7bf4056cf8b920718b377272334b32f7d64c76f329078042f9e77bdedb065a33 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportDetailsResponse.java | 14 | 458 | 5b0011b08d106da9a797ecd6d9915d665460c8823c03c50140b68354a11ae8d5 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportErrorRecord.java | 14 | 295 | 6cfdb0787b4f3b6a4b785106e9f89f26b366d5b25a0fe326f9c9f6032b6b4e92 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParseResult.java | 35 | 912 | 3315458ff08fb345a499a6fba09a296a671e92a6b07a717f8634ce0faab1666b | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java | 303 | 12350 | 9c8fb4eef4fbe0a4b3dfbe1aaa74392f756e6b290a3e7aebb7d05fc3596e2ccb | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportTaskRecord.java | 19 | 440 | 164d55ea7a81e8099c1ad8f44ef07bd3b3e474dfe7fb9f9d3f69893ac423a270 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanPoint.java | 7 | 112 | cc32d4e81cc6e58813c676f5ebfeeff86e2e48d4ea360ad2183843ffd661b6f5 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanShapeRecord.java | 22 | 546 | 4cfece0c8c5a9f13f2171b0d667eb29c109fe6cc3c9bfdf58590a575607e168f | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanCreateRequest.java | 16 | 383 | 0bc12e7e268a7d8904645c560027f48262a330412cada0f44b0d7da47bef8625 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanImportCreateRequest.java | 10 | 240 | 7ecd9fca8fab9864815285d809a18375c6532c5036990599d11b12f3a99598a8 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanImportPublishRequest.java | 13 | 348 | ae890f5f0877b2b3d120d626434b63a0158cffa87f75afa6fb503ef8f2359859 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanShapeSaveRequest.java | 18 | 438 | b890d08f0bfb9e5e1675dc4d2f84a5b21e5f59427b39d59053653e545e07e20d | 全读 |
| backend/src/main/java/com/complex/platform/governance/ProjectRecord.java | 25 | 770 | 3d18793172d90f57cd5296ae76f2d15983a61a60a5ff93b24dd8bd1697284487 | 全读 |
| backend/src/main/java/com/complex/platform/governance/SpaceRecord.java | 22 | 515 | f13f27eaf2dbdb754f18d4f841f1ebfd515d973e5863624583deab8adac7eac1 | 全读 |
| backend/src/main/java/com/complex/platform/governance/SpaceTypeRecord.java | 15 | 386 | 388191c191993bac3ae452374d001a2236f5245a41257b6b9e2c6e7f649b7bd1 | 全读 |
| backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRecord.java | 29 | 1244 | ec04b55b8b3c747b9623450bd7460e00b6a585b2d2d0f8485f80cfc59def6957 | 全读 |
| backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java | 119 | 6399 | f4557789c8eaa6cd0f5a8be883c2a32568264324fb2191848df33623ec02a6ba | 全读 |
| backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java | 125 | 6563 | 149c1eec1650ada8cf9a447bf3fd7117683e71c1a882dea84680d2134082fd5e | 全读 |

## 2. 可执行证据原文（/home/james/audit-tmp/m015_b5r3_run.log）

```
START=2026-09-20T05:22:49Z HEAD=e456ba96 origin/main=84e3441f URL=jdbc:postgresql://127.0.0.1:5432/complex_test
== 0. 23 files + 5 test classes (lines/bytes/sha256)
backend/src/main/java/com/complex/platform/floorplan/FloorPlanController.java 95 4323 33e14ba8868dd24c5235ba55658e51a38c7ff52572d3cb59fd10c07849941bef
backend/src/main/java/com/complex/platform/floorplan/FloorPlanRecord.java 21 503 9db3b6126ab0ca3e624e857684f68e680d7c99f9fe3e0ef6cf8a31080d373bce
backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java 51 2148 9c5e50bff229838722ac00cc9096a8a188b111731be90da291a25c1f43c986a7
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java 689 32061 5cad799d091fd43b58d99ff05d12e10afbc16e63fb7e59857dcd94c042ccdd80
backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java 454 21976 bb3fed48fb09410e7532c6930e7d3b2a3c83080490782cd2d04736b03cf502f3
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportCandidateRecord.java 23 596 7bf4056cf8b920718b377272334b32f7d64c76f329078042f9e77bdedb065a33
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportDetailsResponse.java 14 458 5b0011b08d106da9a797ecd6d9915d665460c8823c03c50140b68354a11ae8d5
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportErrorRecord.java 14 295 6cfdb0787b4f3b6a4b785106e9f89f26b366d5b25a0fe326f9c9f6032b6b4e92
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParseResult.java 35 912 3315458ff08fb345a499a6fba09a296a671e92a6b07a717f8634ce0faab1666b
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java 303 12350 9c8fb4eef4fbe0a4b3dfbe1aaa74392f756e6b290a3e7aebb7d05fc3596e2ccb
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportTaskRecord.java 19 440 164d55ea7a81e8099c1ad8f44ef07bd3b3e474dfe7fb9f9d3f69893ac423a270
backend/src/main/java/com/complex/platform/floorplan/FloorPlanPoint.java 7 112 cc32d4e81cc6e58813c676f5ebfeeff86e2e48d4ea360ad2183843ffd661b6f5
backend/src/main/java/com/complex/platform/floorplan/FloorPlanShapeRecord.java 22 546 4cfece0c8c5a9f13f2171b0d667eb29c109fe6cc3c9bfdf58590a575607e168f
backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanCreateRequest.java 16 383 0bc12e7e268a7d8904645c560027f48262a330412cada0f44b0d7da47bef8625
backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanImportCreateRequest.java 10 240 7ecd9fca8fab9864815285d809a18375c6532c5036990599d11b12f3a99598a8
backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanImportPublishRequest.java 13 348 ae890f5f0877b2b3d120d626434b63a0158cffa87f75afa6fb503ef8f2359859
backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanShapeSaveRequest.java 18 438 b890d08f0bfb9e5e1675dc4d2f84a5b21e5f59427b39d59053653e545e07e20d
backend/src/main/java/com/complex/platform/governance/ProjectRecord.java 25 770 3d18793172d90f57cd5296ae76f2d15983a61a60a5ff93b24dd8bd1697284487
backend/src/main/java/com/complex/platform/governance/SpaceRecord.java 22 515 f13f27eaf2dbdb754f18d4f841f1ebfd515d973e5863624583deab8adac7eac1
backend/src/main/java/com/complex/platform/governance/SpaceTypeRecord.java 15 386 388191c191993bac3ae452374d001a2236f5245a41257b6b9e2c6e7f649b7bd1
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRecord.java 29 1244 ec04b55b8b3c747b9623450bd7460e00b6a585b2d2d0f8485f80cfc59def6957
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java 119 6399 f4557789c8eaa6cd0f5a8be883c2a32568264324fb2191848df33623ec02a6ba
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java 125 6563 149c1eec1650ada8cf9a447bf3fd7117683e71c1a882dea84680d2134082fd5e
backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java 472 21466 acd1f485522c9c31c3ec37c1194226fb5259db54e40a970e3fb2b0ea2aaa7aac
backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java 193 7611 a6e0f52b13a11a0d157df098a569b0e2c62a0044eaebbb3cfa5fe8382e1b758c
backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java 277 11748 5a5932ce4bf3dc12d3b1a652cf107144bd33663cef462f6c0d87f405bdedaa59
backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java 897 44445 ea7aa63ecdb36e9cb8e0b921c62f012e5931d1a157a99ae202015a6b4ceda31d
backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java 285 14140 3585ca694bf16fdd809689b819d258641792c6a8c964448374f295dff85c9c76
== 1. version headers
backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java:2:// 文件版本：V0.01
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java:2:// 文件版本：V0.04
backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java:2:// 文件版本：V0.01
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java:2:// 文件版本：V0.01
backend/src/main/java/com/complex/platform/governance/ProjectRecord.java:2:// 文件版本：V0.01
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java:2:// 文件版本：V0.01
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java:2:// 文件版本：V0.02
backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java:2:// 文件版本：V0.01
backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java:2:// 文件版本：V0.01
backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java:2:// 文件版本：V0.03
backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java:2:// 文件版本：V0.05
backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java:2:// 文件版本：V0.03
== 2. SHAPE_TYPES vs V423 CHECK (M016-002 / M015-013)
35:    private static final Set<String> SHAPE_TYPES = Set.of("area", "shop", "entrance", "facility", "risk_zone", "route", "booth", "public_area");
6:    CHECK (shape_type IN ('area', 'shop', 'entrance', 'facility', 'risk_zone', 'route', 'booth', 'public_area'));
9:    CHECK (shape_type IN ('area', 'shop', 'entrance', 'facility', 'risk_zone', 'route', 'booth', 'public_area'));
== 3. ProjectRecord staged diff (M009-013 获批范围)
 1 file changed, 9 insertions(+), 1 deletion(-)
0
== 4. English operator text residue in module (expect none)
(none)
== 5. complex_test flyway level (SELECT only)
427
chk_floor_plan_import_candidates_shape_type|CHECK (((shape_type)::text = ANY ((ARRAY['area'::character varying, 'shop'::character varying, 'entrance'::character varying, 'facility'::character varying, 'risk_zone'::character varying, 'route'::character varying, 'booth'::character varying, 'public_area'::character varying])::text[])))
chk_floor_plan_shapes_shape_type|CHECK (((shape_type)::text = ANY ((ARRAY['area'::character varying, 'shop'::character varying, 'entrance'::character varying, 'facility'::character varying, 'risk_zone'::character varying, 'route'::character varying, 'booth'::character varying, 'public_area'::character varying])::text[])))
== 6. tests (main worktree, complex_test, three DB vars only)
0
CLEAN_EXIT=0
22:23:00.249 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests]: FloorPlanJdbcIntegrationTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
22:23:00.304 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests
22:23:00.334 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests]: FloorPlanJdbcIntegrationTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
22:23:00.335 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v4.1.0)

2026-09-19T22:23:00.488-07:00  INFO 1576527 --- [complex-api] [           main] c.c.p.f.FloorPlanJdbcIntegrationTests    : Starting FloorPlanJdbcIntegrationTests using Java 21.0.12.1 with PID 1576527 (started by james in /home/james/complex-management/backend)
2026-09-19T22:23:00.488-07:00 DEBUG 1576527 --- [complex-api] [           main] c.c.p.f.FloorPlanJdbcIntegrationTests    : Running with Spring Boot v4.1.0, Spring v7.0.8
2026-09-19T22:23:00.489-07:00  INFO 1576527 --- [complex-api] [           main] c.c.p.f.FloorPlanJdbcIntegrationTests    : No active profile set, falling back to 1 default profile: "local"
2026-09-19T22:23:01.343-07:00  INFO 1576527 --- [complex-api] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2026-09-19T22:23:01.420-07:00  INFO 1576527 --- [complex-api] [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@556843a5
2026-09-19T22:23:01.421-07:00  INFO 1576527 --- [complex-api] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2026-09-19T22:23:01.430-07:00  INFO 1576527 --- [complex-api] [           main] org.flywaydb.core.FlywayExecutor         : Database: jdbc:postgresql://127.0.0.1:5432/complex_test (PostgreSQL 16.14)
2026-09-19T22:23:01.529-07:00  INFO 1576527 --- [complex-api] [           main] o.f.core.internal.command.DbValidate     : Successfully validated 395 migrations (execution time 00:00.070s)
2026-09-19T22:23:01.541-07:00  INFO 1576527 --- [complex-api] [           main] o.f.core.internal.command.DbMigrate      : Current version of schema "public": 427
2026-09-19T22:23:01.543-07:00  INFO 1576527 --- [complex-api] [           main] o.f.core.internal.command.DbMigrate      : Schema "public" is up to date. No migration necessary.
2026-09-19T22:23:02.353-07:00  INFO 1576527 --- [complex-api] [           main] r$InitializeUserDetailsManagerConfigurer : Global AuthenticationManager configured with UserDetailsService bean with name userDetailsService
2026-09-19T22:23:02.792-07:00  INFO 1576527 --- [complex-api] [           main] o.s.b.a.e.web.EndpointLinksResolver      : Exposing 2 endpoints beneath base path '/actuator'
2026-09-19T22:23:02.822-07:00  INFO 1576527 --- [complex-api] [           main] c.c.p.f.FloorPlanJdbcIntegrationTests    : Started FloorPlanJdbcIntegrationTests in 2.467 seconds (process running for 3.126)
2026-09-19T22:23:02.860-07:00  INFO 1576527 --- [complex-api] [           main] .p.c.c.MultiPartyContactPhonePiiBackfill : PII_BACKFILL_CLEAN: 六个多主体手机号列均无明文残留
2026-09-19T22:23:02.862-07:00  WARN 1576527 --- [complex-api] [           main] o.s.core.events.SpringDocAppInitializer  : SpringDoc /docs/openapi endpoint is enabled by default. To disable it in production, set the property 'springdoc.api-docs.enabled=false'
2026-09-19T22:23:02.862-07:00  WARN 1576527 --- [complex-api] [           main] o.s.core.events.SpringDocAppInitializer  : SpringDoc /docs/swagger-ui endpoint is enabled by default. To disable it in production, set the property 'springdoc.swagger-ui.enabled=false'
2026-09-19T22:23:03.185-07:00  INFO 1576527 --- [complex-api] [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.floorplan.FloorPlanControllerTests
2026-09-19T22:23:03.186-07:00  INFO 1576527 --- [complex-api] [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.floorplan.FloorPlanControllerTests

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v4.1.0)

2026-09-19T22:23:03.209-07:00  INFO 1576527 --- [complex-api] [           main] c.c.p.f.FloorPlanControllerTests         : Starting FloorPlanControllerTests using Java 21.0.12.1 with PID 1576527 (started by james in /home/james/complex-management/backend)
2026-09-19T22:23:03.209-07:00 DEBUG 1576527 --- [complex-api] [           main] c.c.p.f.FloorPlanControllerTests         : Running with Spring Boot v4.1.0, Spring v7.0.8
2026-09-19T22:23:03.209-07:00  INFO 1576527 --- [complex-api] [           main] c.c.p.f.FloorPlanControllerTests         : No active profile set, falling back to 1 default profile: "local"
2026-09-19T22:23:03.526-07:00  INFO 1576527 --- [complex-api] [           main] r$InitializeUserDetailsManagerConfigurer : Global AuthenticationManager configured with UserDetailsService bean with name userDetailsService
2026-09-19T22:23:03.597-07:00 DEBUG 1576527 --- [complex-api] [           main] c.c.p.auth.CustomerSessionCookieFilter   : Filter 'customerSessionCookieFilter' configured for use
2026-09-19T22:23:03.597-07:00 DEBUG 1576527 --- [complex-api] [           main] .c.p.g.DeviceCredentialBodyCachingFilter : Filter 'deviceCredentialBodyCachingFilter' configured for use
2026-09-19T22:23:03.597-07:00 DEBUG 1576527 --- [complex-api] [           main] c.c.p.auth.BearerAuthenticationFilter    : Filter 'bearerAuthenticationFilter' configured for use
2026-09-19T22:23:03.597-07:00 DEBUG 1576527 --- [complex-api] [           main] .c.p.a.CustomerTokenAuthenticationFilter : Filter 'customerTokenAuthenticationFilter' configured for use
2026-09-19T22:23:03.597-07:00 DEBUG 1576527 --- [complex-api] [           main] c.c.p.auth.DoubleSubmitCsrfFilter        : Filter 'doubleSubmitCsrfFilter' configured for use
2026-09-19T22:23:03.598-07:00  INFO 1576527 --- [complex-api] [           main] o.s.b.t.m.w.SpringBootMockServletContext : Initializing Spring TestDispatcherServlet ''
2026-09-19T22:23:03.598-07:00  INFO 1576527 --- [complex-api] [           main] o.s.t.web.servlet.TestDispatcherServlet  : Initializing Servlet ''
2026-09-19T22:23:03.599-07:00  INFO 1576527 --- [complex-api] [           main] o.s.t.web.servlet.TestDispatcherServlet  : Completed initialization in 1 ms
2026-09-19T22:23:03.614-07:00  INFO 1576527 --- [complex-api] [           main] c.c.p.f.FloorPlanControllerTests         : Started FloorPlanControllerTests in 0.42 seconds (process running for 3.919)
2026-09-19T22:23:03.767-07:00 DEBUG 1576527 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T22:23:03.775-07:00 DEBUG 1576527 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=VALIDATION_FAILED raw_message=idempotency_key is required
2026-09-19T22:23:03.799-07:00 DEBUG 1576527 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=VALIDATION_FAILED raw_message=idempotency_key is required
2026-09-19T22:23:03.803-07:00 DEBUG 1576527 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=VALIDATION_FAILED raw_message=idempotency_key is required
2026-09-19T22:23:03.829-07:00 DEBUG 1576527 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=VALIDATION_FAILED raw_message=idempotency_key is required
2026-09-19T22:23:03.834-07:00 DEBUG 1576527 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T22:23:03.837-07:00 DEBUG 1576527 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T22:23:03.839-07:00 DEBUG 1576527 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T22:23:03.841-07:00 DEBUG 1576527 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T22:23:03.843-07:00 DEBUG 1576527 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T22:23:03.845-07:00 DEBUG 1576527 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T22:23:03.846-07:00 DEBUG 1576527 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T22:23:03.848-07:00 DEBUG 1576527 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T22:23:03.870-07:00  INFO 1576527 --- [complex-api] [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.complex.platform.governance.VenueBusinessHoursServiceTests$RealRepositoryCases]: RealRepositoryCases does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-19T22:23:03.883-07:00  INFO 1576527 --- [complex-api] [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.governance.VenueBusinessHoursServiceTests$RealRepositoryCases
2026-09-19T22:23:03.884-07:00  INFO 1576527 --- [complex-api] [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.complex.platform.governance.VenueBusinessHoursServiceTests$RealRepositoryCases]: RealRepositoryCases does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-19T22:23:03.885-07:00  INFO 1576527 --- [complex-api] [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.complex.platform.governance.VenueBusinessHoursServiceTests]: VenueBusinessHoursServiceTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-19T22:23:03.885-07:00  INFO 1576527 --- [complex-api] [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.governance.VenueBusinessHoursServiceTests$RealRepositoryCases
MVN_EXIT=0
com.complex.platform.floorplan.FloorPlanControllerTests.txt: Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.676 s -- in com.complex.platform.floorplan.FloorPlanControllerTests
com.complex.platform.floorplan.FloorPlanImportParserTests.txt: Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.159 s -- in com.complex.platform.floorplan.FloorPlanImportParserTests
com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests.txt: Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.894 s -- in com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests
com.complex.platform.floorplan.FloorPlanServiceTests.txt: Tests run: 39, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.097 s -- in com.complex.platform.floorplan.FloorPlanServiceTests
com.complex.platform.governance.VenueBusinessHoursServiceTests.txt: Tests run: 0, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.050 s -- in com.complex.platform.governance.VenueBusinessHoursServiceTests
== 7. HTML + lock untouched
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66  docs/business-enum-quick-reference-v0.1.html
eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66  docs/business-system-map-for-operations-v0.1.html
a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1  docs/platform-governance-map-for-operations-v0.1.html
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md
 M backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java
M  backend/src/main/java/com/complex/platform/governance/ProjectRecord.java
 M backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java
 M backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java
 M backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java
END=2026-09-20T05:23:04Z
```

5 类 88 例（TEST-*.xml 口径）零失败/错误/跳过。

## 3. 三线审计

- 第一性原理线：治理地图 L469-472（维护楼层/区域/点位；平面图导入→人工复核→发布，高危二次确认，不可静默覆盖）、BEQR-152 / PGM-041「默认按项目隔离，只有项目之上的获授权岗位才可跨项目汇总」、BSM-007「平台管理员拥有全部日常功能权限」、BSM L521 涉钱配置草案化（010 XM 维持）。四类主体的读写路径按 `AuthorizationService` 真实现逐条追（`isPlatformAdmin:279-281`、`requireProjectScope:289-294`、`currentMerchantIdQuietly:440-455`、`currentProjectScopeQuietly:504-507`）：平台管理员（无 org）→ `projectScopeOrAll()` 空 → 全量（015 修）；项目员工（org=项目）→ 项目过滤/越项目 CROSS_PROJECT_FORBIDDEN；商户员工 → 商户过滤优先；无归属普通员工 → PROJECT_SCOPE_REQUIRED（fail-closed，红线④）。发布：parsed/published 任务可继续发布剩余 pending（016 修），failed/cancelled 拒，已发布候选拒。
- 顺从式线：90 判据逐族与 B5-r1 再执行 V0.02/V0.03 同口径；GEN-002/003/005/009 本轮由 015/016 修复闭合（§4）；其余结论不变。
- 对抗式线（grilling）：把 `projectScopeOrAll()` 改回直接调 quietly → 新增 4 例红；把发布前置改回只认 parsed → 分批发布例红；把候选状态闸去掉 → `publishImportTaskRejectsAlreadyPublishedTask` 红；测试桩已不再伪造 scope 方法（只替换 `currentActor`），生产语义与 M006 回归一致。派生主键 36/43/39 ≤ 64、发布 ≤200、导入上限、中文文案、场地校验现态同 B5-r1 再执行报告。

## 4. 已定稿 001–016 现态

001–009 现态成立（同 B5-r1 再执行 §6）；015 已修（两服务 `projectScopeOrAll()` + 五处调用 + 桩生产语义 + 4 例）；016 已修（前置 parsed/published + 1 例）；010–014 维持否。修复队列「是」= 0。

## 5. 本次 Findings

无新增。

## 6. 观察（登记）

- ProjectRecord 含 M009 在途暂存字段（floorplan 包零引用）；`PlatformController` 营业时段 `PageResponse.of(list(),1,500)` 截断（非本模块文件）；`operatorNote` 不落库、`saveFloorPlan` 不更新 merchant_id（014 已观察）。

## 7. 结论

席甲 B5-r3 票：**PASS**。23 文件全读、90 判据逐族、5 类 88 例实跑；未改任何文件；落盘前未读 Grok 本次正文。

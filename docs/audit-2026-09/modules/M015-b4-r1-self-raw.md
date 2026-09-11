CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M015 B4-r1 修复内容复审 · 席甲完整报告
版本：V0.01

- 席甲：Claude Fable 5.1（作者自审票）。步骤：B4-r1（B3 第 1 次整批修复的完整 diff 复审）。2026-09-11T07:39:42Z
- before tree：`e0e9fe4451cc5255e0554ebef94bf7f521737a6e`；after tree：`1bb326da9a30686decf2274d9f3543376ff6addd`；`git diff --binary e0e9fe4451cc 1bb326da9a30` 全文 41713 bytes SHA256 2218456f105d17a121e8f0a4aab7ebde272e31c69c9dba3b163410d1ed2ddcf7（全文附于本报告末尾）。
- 定稿依据 findings V1.93「当前 M015 B2 双PASS定稿」：是＝M015-001、002、003、004、005、006、007、008、009（否：010–014）。B3 累计 1/3。TDD：RED＝FloorPlanServiceTests 26 例 8 失败 + VenueBusinessHoursServiceTests 9 例 3 失败（新增 11 例）；GREEN＝5 个测试类全绿（/tmp/m015_b3r1_red.log、_green.log）。
- 锁四值：实算/sidecar/ledger/Grok 均 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。

## 1. diffstat

```
.../platform/floorplan/FloorPlanImportParser.java  |  27 ++--
 .../platform/floorplan/FloorPlanRepository.java    |   4 +
 .../platform/floorplan/FloorPlanService.java       |  78 +++++++++---
 .../floorplan/JdbcFloorPlanRepository.java         |   8 ++
 .../governance/VenueBusinessHoursRepository.java   |  29 +++++
 .../governance/VenueBusinessHoursService.java      |  32 +++--
 .../floorplan/FloorPlanJdbcIntegrationTests.java   |   3 +-
 .../platform/floorplan/FloorPlanServiceTests.java  | 136 ++++++++++++++++++++-
 .../governance/VenueBusinessHoursServiceTests.java |  64 +++++++++-
 9 files changed, 341 insertions(+), 40 deletions(-)
```

## 2. 逐块归因（每一行改动都归到定稿 ID；「版本铁律」行随其所在文件的 ID）

| 文件 | 块 | 位置 | 块内标记 ID | 归因 |
|---|---|---|---|---|
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java | 1 | `@@ -1,4 +1,5 @@` | 版本铁律（随所属ID） | 004（13 处 error_message 中文，error_code 不变）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java | 2 | `@@ -33,7 +34,7 @@ public class FloorPlanImportParser {` | — | 004（13 处 error_message 中文，error_code 不变）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java | 3 | `@@ -41,7 +42,7 @@ public class FloorPlanImportParser {` | — | 004（13 处 error_message 中文，error_code 不变）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java | 4 | `@@ -53,7 +54,7 @@ public class FloorPlanImportParser {` | — | 004（13 处 error_message 中文，error_code 不变）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java | 5 | `@@ -64,19 +65,19 @@ public class FloorPlanImportParser {` | — | 004（13 处 error_message 中文，error_code 不变）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java | 6 | `@@ -86,7 +87,7 @@ public class FloorPlanImportParser {` | — | 004（13 处 error_message 中文，error_code 不变）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java | 7 | `@@ -103,7 +104,7 @@ public class FloorPlanImportParser {` | — | 004（13 处 error_message 中文，error_code 不变）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java | 8 | `@@ -118,13 +119,13 @@ public class FloorPlanImportParser {` | — | 004（13 处 error_message 中文，error_code 不变）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java | 9 | `@@ -238,9 +239,9 @@ public class FloorPlanImportParser {` | — | 004（13 处 error_message 中文，error_code 不变）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java | 1 | `@@ -1,4 +1,5 @@` | 版本铁律（随所属ID） | 002（projectExists 契约）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java | 2 | `@@ -19,6 +20,9 @@ public interface FloorPlanRepository {` | M015-002 | 002（projectExists 契约）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 1 | `@@ -1,4 +1,5 @@` | 版本铁律（随所属ID） | 001（fps_/fpi_ 主键含平面图域）、002（project_id 必填 + projectExists → PROJECT_NOT_FOUND）、003（列表商户 scope 优先/项目 scope → findAllFloorPlansByProject；checkProjectOwnership → CROSS_PROJECT_FORBIDDEN；创建时 scope 须相等）、005（FIELD_LABELS 常量、requireField「平面图名称」、「候选项列表不能为空」、「取值无效」）、006（:517/:535 去句内英文键）、007（三处诚实不分页）、008（MAX_PUBLISH_CANDIDATES=200）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 2 | `@@ -41,6 +42,11 @@ public class FloorPlanService {` | M015-005, M015-008 | 001（fps_/fpi_ 主键含平面图域）、002（project_id 必填 + projectExists → PROJECT_NOT_FOUND）、003（列表商户 scope 优先/项目 scope → findAllFloorPlansByProject；checkProjectOwnership → CROSS_PROJECT_FORBIDDEN；创建时 scope 须相等）、005（FIELD_LABELS 常量、requireField「平面图名称」、「候选项列表不能为空」、「取值无效」）、006（:517/:535 去句内英文键）、007（三处诚实不分页）、008（MAX_PUBLISH_CANDIDATES=200）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 3 | `@@ -63,10 +69,13 @@ public class FloorPlanService {` | M015-003 | 001（fps_/fpi_ 主键含平面图域）、002（project_id 必填 + projectExists → PROJECT_NOT_FOUND）、003（列表商户 scope 优先/项目 scope → findAllFloorPlansByProject；checkProjectOwnership → CROSS_PROJECT_FORBIDDEN；创建时 scope 须相等）、005（FIELD_LABELS 常量、requireField「平面图名称」、「候选项列表不能为空」、「取值无效」）、006（:517/:535 去句内英文键）、007（三处诚实不分页）、008（MAX_PUBLISH_CANDIDATES=200）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 4 | `@@ -76,7 +85,7 @@ public class FloorPlanService {` | — | 001（fps_/fpi_ 主键含平面图域）、002（project_id 必填 + projectExists → PROJECT_NOT_FOUND）、003（列表商户 scope 优先/项目 scope → findAllFloorPlansByProject；checkProjectOwnership → CROSS_PROJECT_FORBIDDEN；创建时 scope 须相等）、005（FIELD_LABELS 常量、requireField「平面图名称」、「候选项列表不能为空」、「取值无效」）、006（:517/:535 去句内英文键）、007（三处诚实不分页）、008（MAX_PUBLISH_CANDIDATES=200）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 5 | `@@ -92,7 +101,7 @@ public class FloorPlanService {` | — | 001（fps_/fpi_ 主键含平面图域）、002（project_id 必填 + projectExists → PROJECT_NOT_FOUND）、003（列表商户 scope 优先/项目 scope → findAllFloorPlansByProject；checkProjectOwnership → CROSS_PROJECT_FORBIDDEN；创建时 scope 须相等）、005（FIELD_LABELS 常量、requireField「平面图名称」、「候选项列表不能为空」、「取值无效」）、006（:517/:535 去句内英文键）、007（三处诚实不分页）、008（MAX_PUBLISH_CANDIDATES=200）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 6 | `@@ -138,7 +147,7 @@ public class FloorPlanService {` | — | 001（fps_/fpi_ 主键含平面图域）、002（project_id 必填 + projectExists → PROJECT_NOT_FOUND）、003（列表商户 scope 优先/项目 scope → findAllFloorPlansByProject；checkProjectOwnership → CROSS_PROJECT_FORBIDDEN；创建时 scope 须相等）、005（FIELD_LABELS 常量、requireField「平面图名称」、「候选项列表不能为空」、「取值无效」）、006（:517/:535 去句内英文键）、007（三处诚实不分页）、008（MAX_PUBLISH_CANDIDATES=200）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 7 | `@@ -221,10 +230,10 @@ public class FloorPlanService {` | — | 001（fps_/fpi_ 主键含平面图域）、002（project_id 必填 + projectExists → PROJECT_NOT_FOUND）、003（列表商户 scope 优先/项目 scope → findAllFloorPlansByProject；checkProjectOwnership → CROSS_PROJECT_FORBIDDEN；创建时 scope 须相等）、005（FIELD_LABELS 常量、requireField「平面图名称」、「候选项列表不能为空」、「取值无效」）、006（:517/:535 去句内英文键）、007（三处诚实不分页）、008（MAX_PUBLISH_CANDIDATES=200）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 8 | `@@ -256,7 +265,7 @@ public class FloorPlanService {` | — | 001（fps_/fpi_ 主键含平面图域）、002（project_id 必填 + projectExists → PROJECT_NOT_FOUND）、003（列表商户 scope 优先/项目 scope → findAllFloorPlansByProject；checkProjectOwnership → CROSS_PROJECT_FORBIDDEN；创建时 scope 须相等）、005（FIELD_LABELS 常量、requireField「平面图名称」、「候选项列表不能为空」、「取值无效」）、006（:517/:535 去句内英文键）、007（三处诚实不分页）、008（MAX_PUBLISH_CANDIDATES=200）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 9 | `@@ -284,7 +293,7 @@ public class FloorPlanService {` | — | 001（fps_/fpi_ 主键含平面图域）、002（project_id 必填 + projectExists → PROJECT_NOT_FOUND）、003（列表商户 scope 优先/项目 scope → findAllFloorPlansByProject；checkProjectOwnership → CROSS_PROJECT_FORBIDDEN；创建时 scope 须相等）、005（FIELD_LABELS 常量、requireField「平面图名称」、「候选项列表不能为空」、「取值无效」）、006（:517/:535 去句内英文键）、007（三处诚实不分页）、008（MAX_PUBLISH_CANDIDATES=200）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 10 | `@@ -362,7 +371,7 @@ public class FloorPlanService {` | — | 001（fps_/fpi_ 主键含平面图域）、002（project_id 必填 + projectExists → PROJECT_NOT_FOUND）、003（列表商户 scope 优先/项目 scope → findAllFloorPlansByProject；checkProjectOwnership → CROSS_PROJECT_FORBIDDEN；创建时 scope 须相等）、005（FIELD_LABELS 常量、requireField「平面图名称」、「候选项列表不能为空」、「取值无效」）、006（:517/:535 去句内英文键）、007（三处诚实不分页）、008（MAX_PUBLISH_CANDIDATES=200）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 11 | `@@ -494,8 +503,20 @@ public class FloorPlanService {` | M015-002/003 | 001（fps_/fpi_ 主键含平面图域）、002（project_id 必填 + projectExists → PROJECT_NOT_FOUND）、003（列表商户 scope 优先/项目 scope → findAllFloorPlansByProject；checkProjectOwnership → CROSS_PROJECT_FORBIDDEN；创建时 scope 须相等）、005（FIELD_LABELS 常量、requireField「平面图名称」、「候选项列表不能为空」、「取值无效」）、006（:517/:535 去句内英文键）、007（三处诚实不分页）、008（MAX_PUBLISH_CANDIDATES=200）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 12 | `@@ -514,7 +535,7 @@ public class FloorPlanService {` | — | 001（fps_/fpi_ 主键含平面图域）、002（project_id 必填 + projectExists → PROJECT_NOT_FOUND）、003（列表商户 scope 优先/项目 scope → findAllFloorPlansByProject；checkProjectOwnership → CROSS_PROJECT_FORBIDDEN；创建时 scope 须相等）、005（FIELD_LABELS 常量、requireField「平面图名称」、「候选项列表不能为空」、「取值无效」）、006（:517/:535 去句内英文键）、007（三处诚实不分页）、008（MAX_PUBLISH_CANDIDATES=200）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 13 | `@@ -532,7 +553,7 @@ public class FloorPlanService {` | — | 001（fps_/fpi_ 主键含平面图域）、002（project_id 必填 + projectExists → PROJECT_NOT_FOUND）、003（列表商户 scope 优先/项目 scope → findAllFloorPlansByProject；checkProjectOwnership → CROSS_PROJECT_FORBIDDEN；创建时 scope 须相等）、005（FIELD_LABELS 常量、requireField「平面图名称」、「候选项列表不能为空」、「取值无效」）、006（:517/:535 去句内英文键）、007（三处诚实不分页）、008（MAX_PUBLISH_CANDIDATES=200）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 14 | `@@ -561,7 +582,10 @@ public class FloorPlanService {` | — | 001（fps_/fpi_ 主键含平面图域）、002（project_id 必填 + projectExists → PROJECT_NOT_FOUND）、003（列表商户 scope 优先/项目 scope → findAllFloorPlansByProject；checkProjectOwnership → CROSS_PROJECT_FORBIDDEN；创建时 scope 须相等）、005（FIELD_LABELS 常量、requireField「平面图名称」、「候选项列表不能为空」、「取值无效」）、006（:517/:535 去句内英文键）、007（三处诚实不分页）、008（MAX_PUBLISH_CANDIDATES=200）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 15 | `@@ -569,9 +593,21 @@ public class FloorPlanService {` | M015-003 | 001（fps_/fpi_ 主键含平面图域）、002（project_id 必填 + projectExists → PROJECT_NOT_FOUND）、003（列表商户 scope 优先/项目 scope → findAllFloorPlansByProject；checkProjectOwnership → CROSS_PROJECT_FORBIDDEN；创建时 scope 须相等）、005（FIELD_LABELS 常量、requireField「平面图名称」、「候选项列表不能为空」、「取值无效」）、006（:517/:535 去句内英文键）、007（三处诚实不分页）、008（MAX_PUBLISH_CANDIDATES=200）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 16 | `@@ -598,11 +634,19 @@ public class FloorPlanService {` | — | 001（fps_/fpi_ 主键含平面图域）、002（project_id 必填 + projectExists → PROJECT_NOT_FOUND）、003（列表商户 scope 优先/项目 scope → findAllFloorPlansByProject；checkProjectOwnership → CROSS_PROJECT_FORBIDDEN；创建时 scope 须相等）、005（FIELD_LABELS 常量、requireField「平面图名称」、「候选项列表不能为空」、「取值无效」）、006（:517/:535 去句内英文键）、007（三处诚实不分页）、008（MAX_PUBLISH_CANDIDATES=200）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java | 1 | `@@ -1,4 +1,5 @@` | 版本铁律（随所属ID） | 002（projectExists：SELECT COUNT(1) FROM projects）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java | 2 | `@@ -60,6 +61,13 @@ class JdbcFloorPlanRepository implements ` | — | 002（projectExists：SELECT COUNT(1) FROM projects）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java | 1 | `@@ -1,4 +1,5 @@` | 版本铁律（随所属ID） | 009（findVenueProjectId、listByProject JOIN spaces）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java | 2 | `@@ -6,6 +7,7 @@ import java.time.LocalTime;` | — | 009（findVenueProjectId、listByProject JOIN spaces）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java | 3 | `@@ -45,6 +47,33 @@ class VenueBusinessHoursRepository {` | M015-009 | 009（findVenueProjectId、listByProject JOIN spaces）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java | 1 | `@@ -1,5 +1,7 @@` | 版本铁律（随所属ID） | 006（场地/开始营业时间/结束营业时间中文整句）、009（注入 AuthorizationService；findVenueProjectId → VENUE_NOT_FOUND；项目 scope → CROSS_PROJECT_FORBIDDEN；列表按 scope 走 listByProject）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java | 2 | `@@ -8,6 +10,7 @@ import java.time.LocalTime;` | — | 006（场地/开始营业时间/结束营业时间中文整句）、009（注入 AuthorizationService；findVenueProjectId → VENUE_NOT_FOUND；项目 scope → CROSS_PROJECT_FORBIDDEN；列表按 scope 走 listByProject）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java | 3 | `@@ -27,14 +30,21 @@ public class VenueBusinessHoursService {` | M015-009 | 006（场地/开始营业时间/结束营业时间中文整句）、009（注入 AuthorizationService；findVenueProjectId → VENUE_NOT_FOUND；项目 scope → CROSS_PROJECT_FORBIDDEN；列表按 scope 走 listByProject）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java | 4 | `@@ -47,14 +57,22 @@ public class VenueBusinessHoursService {` | M015-009 | 006（场地/开始营业时间/结束营业时间中文整句）、009（注入 AuthorizationService；findVenueProjectId → VENUE_NOT_FOUND；项目 scope → CROSS_PROJECT_FORBIDDEN；列表按 scope 走 listByProject）；版本头随本文件 |
| backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java | 5 | `@@ -78,12 +96,12 @@ public class VenueBusinessHoursService {` | — | 006（场地/开始营业时间/结束营业时间中文整句）、009（注入 AuthorizationService；findVenueProjectId → VENUE_NOT_FOUND；项目 scope → CROSS_PROJECT_FORBIDDEN；列表按 scope 走 listByProject）；版本头随本文件 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java | 1 | `@@ -1,4 +1,5 @@` | 版本铁律（随所属ID） | 004（夹具 error_message 改中文）；版本头随本文件 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java | 2 | `@@ -161,7 +162,7 @@ class FloorPlanJdbcIntegrationTests {` | — | 004（夹具 error_message 改中文）；版本头随本文件 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java | 1 | `@@ -1,4 +1,5 @@` | 版本铁律（随所属ID） | 001（+2 例）、002（+2 例；InMemory projectExists）、003（+3 例；serviceWithProjectScope、noScopeAuth 覆盖 currentProjectScopeQuietly、InMemory findAllFloorPlansByProject）、007（+1 例）、createRequest(key, projectId) 辅助随 002/003；版本头随本文件 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java | 2 | `@@ -24,6 +25,7 @@ import java.util.LinkedHashMap;` | — | 001（+2 例）、002（+2 例；InMemory projectExists）、003（+3 例；serviceWithProjectScope、noScopeAuth 覆盖 currentProjectScopeQuietly、InMemory findAllFloorPlansByProject）、007（+1 例）、createRequest(key, projectId) 辅助随 002/003；版本头随本文件 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java | 3 | `@@ -326,15 +328,131 @@ class FloorPlanServiceTests {` | M015-001, M015-002, M015-003, M015-007 | 001（+2 例）、002（+2 例；InMemory projectExists）、003（+3 例；serviceWithProjectScope、noScopeAuth 覆盖 currentProjectScopeQuietly、InMemory findAllFloorPlansByProject）、007（+1 例）、createRequest(key, projectId) 辅助随 002/003；版本头随本文件 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java | 4 | `@@ -351,10 +469,14 @@ class FloorPlanServiceTests {` | — | 001（+2 例）、002（+2 例；InMemory projectExists）、003（+3 例；serviceWithProjectScope、noScopeAuth 覆盖 currentProjectScopeQuietly、InMemory findAllFloorPlansByProject）、007（+1 例）、createRequest(key, projectId) 辅助随 002/003；版本头随本文件 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java | 5 | `@@ -415,6 +537,18 @@ class FloorPlanServiceTests {` | — | 001（+2 例）、002（+2 例；InMemory projectExists）、003（+3 例；serviceWithProjectScope、noScopeAuth 覆盖 currentProjectScopeQuietly、InMemory findAllFloorPlansByProject）、007（+1 例）、createRequest(key, projectId) 辅助随 002/003；版本头随本文件 |
| backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java | 1 | `@@ -1,9 +1,11 @@` | 版本铁律（随所属ID） | 009（+3 例；RecordingRepository 覆盖 findVenueProjectId/listAll/listByProject；构造注入 AuthorizationService）；版本头随本文件 |
| backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java | 2 | `@@ -12,6 +14,8 @@ import java.time.LocalDate;` | — | 009（+3 例；RecordingRepository 覆盖 findVenueProjectId/listAll/listByProject；构造注入 AuthorizationService）；版本头随本文件 |
| backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java | 3 | `@@ -31,6 +35,24 @@ class VenueBusinessHoursServiceTests {` | — | 009（+3 例；RecordingRepository 覆盖 findVenueProjectId/listAll/listByProject；构造注入 AuthorizationService）；版本头随本文件 |
| backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java | 4 | `@@ -46,11 +68,51 @@ class VenueBusinessHoursServiceTests {` | M015-009 | 009（+3 例；RecordingRepository 覆盖 findVenueProjectId/listAll/listByProject；构造注入 AuthorizationService）；版本头随本文件 |

## 3. 红绿证据（B3-r1 运行记录 /tmp/m015_b3r1_run.log 原文）

```
START=2026-09-11T07:06:03Z HEAD=a041458d
== RED: apply tests + repository scaffolding
FloorPlanServiceTests.java 7cc939e2e2df60e2
FloorPlanJdbcIntegrationTests.java b15c73338d23e076
FloorPlanRepository.java 9c5e50bff2298387
JdbcFloorPlanRepository.java bb3fed48fb09410e
VenueBusinessHoursRepository.java f4557789c8eaa6cd
VenueBusinessHoursService.java 7036a073b354f5fd
VenueBusinessHoursServiceTests.java bf1a9d3232f5318a
RED_PATCH_OK
RED_EXIT=1
Tests run: 26, Failures: 8, Errors: 0, Skipped: 0, Time elapsed: 0.279 s <<< FAILURE! -- in com.complex.platform.floorplan.FloorPlanServiceTests
Tests run: 9, Failures: 3, Errors: 0, Skipped: 0, Time elapsed: 0.201 s <<< FAILURE! -- in com.complex.platform.governance.VenueBusinessHoursServiceTests
1:[ERROR] Tests run: 26, Failures: 8, Errors: 0, Skipped: 0,
2:[ERROR] com.complex.platform.floorplan.FloorPlanServiceTests.createFloorPlanOutsideProjectScopeIsForbidden --
8:[ERROR] com.complex.platform.floorplan.FloorPlanServiceTests.accessingFloorPlanBelongingToDifferentProjectIsForbidden --
14:[ERROR] com.complex.platform.floorplan.FloorPlanServiceTests.createFloorPlanRequiresProjectId --
20:[ERROR] com.complex.platform.floorplan.FloorPlanServiceTests.createFloorPlanRejectsUnknownProject --
26:[ERROR] com.complex.platform.floorplan.FloorPlanServiceTests.createImportTaskWithSameKeyAcrossFloorPlansDoesNotOverwrite --
36:[ERROR] com.complex.platform.floorplan.FloorPlanServiceTests.listShapesReportsHonestPageSizeAndTotal --
43:[ERROR] com.complex.platform.floorplan.FloorPlanServiceTests.listFloorPlansFiltersToProjectScope --
55:[ERROR] com.complex.platform.floorplan.FloorPlanServiceTests.saveShapeWithSameKeyAcrossFloorPlansDoesNotOverwrite --
65:[ERROR] Tests run: 9, Failures: 3, Errors: 0, Skipped: 0,
66:[ERROR] com.complex.platform.governance.VenueBusinessHoursServiceTests.rejectsUnknownVenue --
72:[ERROR] com.complex.platform.governance.VenueBusinessHoursServiceTests.listFiltersByProjectScope --
84:[ERROR] com.complex.platform.governance.VenueBusinessHoursServiceTests.rejectsVenueOutsideProjectScope --
== GREEN: apply code
FloorPlanService.java 95779297d653ad9c
FloorPlanImportParser.java 9c8fb4eef4fbe0a4
VenueBusinessHoursService.java 866fbe936d58375b
GREEN_PATCH_OK
GREEN_EXIT=0
floorplan.FloorPlanControllerTests.txt:Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.618 s -- in com.complex.platform.floorplan.FloorPlanControllerTests
floorplan.FloorPlanImportParserTests.txt:Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.149 s -- in com.complex.platform.floorplan.FloorPlanImportParserTests
floorplan.FloorPlanJdbcIntegrationTests.txt:Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.946 s -- in com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests
floorplan.FloorPlanServiceTests.txt:Tests run: 26, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.069 s -- in com.complex.platform.floorplan.FloorPlanServiceTests
governance.VenueBusinessHoursServiceTests.txt:Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.045 s -- in com.complex.platform.governance.VenueBusinessHoursServiceTests
== post-B3 fingerprints
backend/src/main/java/com/complex/platform/floorplan/FloorPlanController.java 95 33e14ba8868dd24c
backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java 51 9c5e50bff2298387
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java 671 95779297d653ad9c
backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java 454 bb3fed48fb09410e
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java 303 9c8fb4eef4fbe0a4
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java 119 f4557789c8eaa6cd
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java 115 866fbe936d58375b
backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java 645 7cc939e2e2df60e2
backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java 210 b15c73338d23e076
backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java 175 bf1a9d3232f5318a
 M backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java
 M backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java
 M backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java
 M backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java
 M backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java
 M backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java
 M backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java
 M backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java
 M backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java
 M docs/audit-2026-09/findings.md
 M docs/audit-2026-09/ledger.md
END=2026-09-11T07:06:25Z
```

## 4. 本席复审结论

见文末追加的判定段。

## 附：完整 diff（before→after，--binary）

```diff
diff --git a/backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java b/backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java
index 8e7beebf..53a26bc8 100644
--- a/backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java
+++ b/backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java
@@ -1,4 +1,5 @@
 package com.complex.platform.floorplan;
+// 文件版本：V0.01
 
 import com.fasterxml.jackson.databind.JsonNode;
 import com.fasterxml.jackson.databind.ObjectMapper;
@@ -33,7 +34,7 @@ public class FloorPlanImportParser {
         if (sourcePayload == null || sourcePayload.isBlank()) {
             return new FloorPlanImportParseResult(
                     List.of(),
-                    List.of(error(null, "source_payload", "PAYLOAD_REQUIRED", "source_payload is required"))
+                    List.of(error(null, "source_payload", "PAYLOAD_REQUIRED", "导入内容不能为空"))
             );
         }
         return switch (format) {
@@ -41,7 +42,7 @@ public class FloorPlanImportParser {
             case "standard_json" -> parseStandardJson(sourcePayload);
             default -> new FloorPlanImportParseResult(
                     List.of(),
-                    List.of(error(null, "source_format", "UNSUPPORTED_SOURCE_FORMAT", "source_format is not supported"))
+                    List.of(error(null, "source_format", "UNSUPPORTED_SOURCE_FORMAT", "不支持的导入格式（仅支持 svg / standard_json）"))
             );
         };
     }
@@ -53,7 +54,7 @@ public class FloorPlanImportParser {
         if (viewBox == null || viewBox[2] <= 0 || viewBox[3] <= 0) {
             return new FloorPlanImportParseResult(
                     List.of(),
-                    List.of(error(null, "viewBox", "VIEWBOX_REQUIRED", "svg viewBox with positive width and height is required"))
+                    List.of(error(null, "viewBox", "VIEWBOX_REQUIRED", "SVG 必须包含宽高为正数的 viewBox"))
             );
         }
 
@@ -64,19 +65,19 @@ public class FloorPlanImportParser {
             Map<String, String> attributes = readAttributes(matcher.group(2));
             String points = attributes.get("points");
             if (points == null || points.isBlank()) {
-                errors.add(error(shapeNo, "points", "POINTS_REQUIRED", "points attribute is required"));
+                errors.add(error(shapeNo, "points", "POINTS_REQUIRED", "图形缺少 points 属性"));
                 shapeNo++;
                 continue;
             }
 
             List<FloorPlanPoint> rawPoints = parseSvgPoints(points);
             if (rawPoints.size() < 3) {
-                errors.add(error(shapeNo, "points", "POINTS_TOO_FEW", "polygon requires at least 3 points"));
+                errors.add(error(shapeNo, "points", "POINTS_TOO_FEW", "多边形至少需要 3 个顶点"));
                 shapeNo++;
                 continue;
             }
             if ("polyline".equals(tag) && !samePoint(rawPoints.getFirst(), rawPoints.getLast())) {
-                errors.add(error(shapeNo, "points", "POLYLINE_NOT_CLOSED", "polyline must be closed before import"));
+                errors.add(error(shapeNo, "points", "POLYLINE_NOT_CLOSED", "折线（polyline）必须首尾闭合后才能导入"));
                 shapeNo++;
                 continue;
             }
@@ -86,7 +87,7 @@ public class FloorPlanImportParser {
 
             List<FloorPlanPoint> normalized = normalizePoints(rawPoints, viewBox);
             if (normalized.stream().anyMatch(point -> outOfRange(point.x()) || outOfRange(point.y()))) {
-                errors.add(error(shapeNo, "points", "POINT_OUT_OF_RANGE", "normalized point x/y must be between 0 and 1"));
+                errors.add(error(shapeNo, "points", "POINT_OUT_OF_RANGE", "归一化后的顶点坐标 x/y 必须在 0 到 1 之间"));
                 shapeNo++;
                 continue;
             }
@@ -103,7 +104,7 @@ public class FloorPlanImportParser {
         }
 
         if (candidates.isEmpty() && errors.isEmpty()) {
-            errors.add(error(null, "source_payload", "NO_SHAPES_FOUND", "svg does not contain polygon or polyline shapes"));
+            errors.add(error(null, "source_payload", "NO_SHAPES_FOUND", "SVG 中没有 polygon 或 polyline 图形"));
         }
         return new FloorPlanImportParseResult(candidates, errors);
     }
@@ -118,13 +119,13 @@ public class FloorPlanImportParser {
         } catch (Exception exception) {
             return new FloorPlanImportParseResult(
                     List.of(),
-                    List.of(error(null, "source_payload", "JSON_PARSE_FAILED", "source_payload is not valid JSON"))
+                    List.of(error(null, "source_payload", "JSON_PARSE_FAILED", "导入内容不是合法的 JSON"))
             );
         }
         if (!shapesNode.isArray()) {
             return new FloorPlanImportParseResult(
                     List.of(),
-                    List.of(error(null, "shapes", "SHAPES_REQUIRED", "standard_json requires a shapes array"))
+                    List.of(error(null, "shapes", "SHAPES_REQUIRED", "standard_json 必须包含 shapes 数组"))
             );
         }
 
@@ -238,9 +239,9 @@ public class FloorPlanImportParser {
 
     private String messageFor(String errorCode) {
         return switch (errorCode) {
-            case "POINT_OUT_OF_RANGE" -> "polygon point x/y must be between 0 and 1";
-            case "POINTS_TOO_FEW" -> "polygon requires at least 3 points";
-            default -> "import candidate is invalid";
+            case "POINT_OUT_OF_RANGE" -> "多边形顶点坐标 x/y 必须在 0 到 1 之间";
+            case "POINTS_TOO_FEW" -> "多边形至少需要 3 个顶点";
+            default -> "导入候选项无效";
         };
     }
 
diff --git a/backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java b/backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java
index 3084aa32..97d918be 100644
--- a/backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java
+++ b/backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java
@@ -1,4 +1,5 @@
 package com.complex.platform.floorplan;
+// 文件版本：V0.01
 
 import java.util.List;
 import java.util.Optional;
@@ -19,6 +20,9 @@ public interface FloorPlanRepository {
 
     List<FloorPlanRecord> findAllFloorPlansByMerchant(String merchantId);
 
+    /** M015-002：平面图必须归属已存在的综合体项目（projects.id）。 */
+    boolean projectExists(String projectId);
+
     Optional<FloorPlanRecord> findFloorPlanById(String id);
 
     FloorPlanRecord saveFloorPlan(FloorPlanRecord record);
diff --git a/backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java b/backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java
index dc6672f1..7684e2a3 100644
--- a/backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java
+++ b/backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java
@@ -1,4 +1,5 @@
 package com.complex.platform.floorplan;
+// 文件版本：V0.01
 
 import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
@@ -41,6 +42,11 @@ public class FloorPlanService {
     // 一个 <1MB 的 payload 仍可塞入海量小多边形或单个超多顶点多边形撑爆解析/落库，故 parse 后强制上限。
     private static final int MAX_IMPORT_CANDIDATES = 2_000;
     private static final int MAX_IMPORT_POLYGON_POINTS = 1_000;
+    // M015-008：发布一次最多 200 个候选（与 OpenAPI FloorPlanImportPublishRequest.candidate_ids maxItems 一致）。
+    private static final int MAX_PUBLISH_CANDIDATES = 200;
+    // M015-005：本模块校验文案中文整句，不经 FieldLabels 通用键翻译（candidate_ids/plan_name 在通用表中另有含义）。
+    private static final Map<String, String> FIELD_LABELS = Map.of(
+            "status", "状态", "shape_type", "图形类型", "ref_type", "关联类型", "source_format", "导入格式");
 
     private final FloorPlanRepository repository;
     private final IdempotencyService idempotencyService;
@@ -63,10 +69,13 @@ public class FloorPlanService {
     }
 
     public PageResponse<FloorPlanRecord> listFloorPlans() {
-        Optional<String> scope = authorizationService.currentMerchantScope();
-        List<FloorPlanRecord> records = scope.isPresent()
-                ? repository.findAllFloorPlansByMerchant(scope.get())
-                : repository.findAllFloorPlans();
+        Optional<String> merchantScope = authorizationService.currentMerchantScope();
+        // M015-003：商户 scope 优先；否则按项目 scope（BEQR-152 数据默认按项目隔离）；平台管理员全量。
+        List<FloorPlanRecord> records = merchantScope.isPresent()
+                ? repository.findAllFloorPlansByMerchant(merchantScope.get())
+                : authorizationService.currentProjectScopeQuietly()
+                        .map(repository::findAllFloorPlansByProject)
+                        .orElseGet(repository::findAllFloorPlans);
         auditEventService.recordSuccess(
                 "floor_plan.viewed",
                 "floor_plan.view",
@@ -76,7 +85,7 @@ public class FloorPlanService {
                 null,
                 Map.of("count", records.size())
         );
-        return new PageResponse<>(records, 1, 50, records.size());
+        return new PageResponse<>(records, 1, Math.max(records.size(), 1), records.size());
     }
 
     public PageResponse<FloorPlanShapeRecord> listShapes(String floorPlanId) {
@@ -92,7 +101,7 @@ public class FloorPlanService {
                 null,
                 Map.of("count", records.size())
         );
-        return new PageResponse<>(records, 1, 50, records.size());
+        return new PageResponse<>(records, 1, Math.max(records.size(), 1), records.size());
     }
 
     public FloorPlanRecord createFloorPlan(FloorPlanCreateRequest request) {
@@ -138,7 +147,7 @@ public class FloorPlanService {
                 null,
                 Map.of("count", records.size())
         );
-        return new PageResponse<>(records, 1, 50, records.size());
+        return new PageResponse<>(records, 1, Math.max(records.size(), 1), records.size());
     }
 
     public FloorPlanImportDetailsResponse getImportDetails(String floorPlanId, String importTaskId) {
@@ -221,10 +230,10 @@ public class FloorPlanService {
         Optional<String> scope = authorizationService.currentMerchantScope();
         FloorPlanRecord record = repository.saveFloorPlan(new FloorPlanRecord(
                 "fp_" + UUID.randomUUID(),
-                trimToNull(request.projectId()),
+                requireText(request.projectId(), "project_id"),
                 trimToNull(request.buildingCode()),
                 requireText(request.floorNo(), "floor_no"),
-                requireText(request.planName(), "plan_name"),
+                requireField(request.planName(), "平面图名称"),
                 requireText(request.imageUrl(), "image_url"),
                 request.imageWidth(),
                 request.imageHeight(),
@@ -256,7 +265,7 @@ public class FloorPlanService {
         }
         Instant now = Instant.now();
         FloorPlanShapeRecord record = repository.saveShape(new FloorPlanShapeRecord(
-                requestedShapeId == null ? "fps_" + slug(request.idempotencyKey()) : requestedShapeId,
+                requestedShapeId == null ? "fps_" + slug(floorPlanId + "_" + request.idempotencyKey()) : requestedShapeId,
                 floorPlanId,
                 normalizeAllowed(request.shapeType(), "shape_type", SHAPE_TYPES),
                 normalizeAllowed(request.refType(), "ref_type", REF_TYPES),
@@ -284,7 +293,7 @@ public class FloorPlanService {
         Instant now = Instant.now();
         FloorPlanImportParseResult parseResult = importParser.parse(request.sourceFormat(), request.sourcePayload());
         enforceImportLimits(parseResult);
-        String taskId = "fpi_" + slug(request.idempotencyKey());
+        String taskId = "fpi_" + slug(floorPlanId + "_" + request.idempotencyKey());
         String status = parseResult.candidates().isEmpty() ? "failed" : "parsed";
         FloorPlanImportTaskRecord task = repository.saveImportTask(new FloorPlanImportTaskRecord(
                 taskId,
@@ -362,7 +371,7 @@ public class FloorPlanService {
                 .distinct()
                 .toList();
         if (candidateIds.isEmpty()) {
-            throw ApiException.badRequest("VALIDATION_FAILED", "candidate_ids is required");
+            throw ApiException.badRequest("VALIDATION_FAILED", "候选项列表不能为空");
         }
 
         List<FloorPlanImportCandidateRecord> candidates = new ArrayList<>();
@@ -494,8 +503,20 @@ public class FloorPlanService {
             throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
         }
         requireText(request.idempotencyKey(), "idempotency_key");
+        // M015-002/003：平面图必须归属已存在的综合体项目（PGM-017），且项目员工只能在本项目下创建（BEQR-152）。
+        String projectId = requireText(request.projectId(), "project_id");
+        if (!repository.projectExists(projectId)) {
+            throw ApiException.notFound("PROJECT_NOT_FOUND", "综合体项目不存在或已被删除");
+        }
+        if (authorizationService.currentMerchantScope().isEmpty()) {
+            authorizationService.currentProjectScopeQuietly().ifPresent(scope -> {
+                if (!scope.equals(projectId)) {
+                    throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "只能在当前综合体项目下创建平面图");
+                }
+            });
+        }
         requireText(request.floorNo(), "floor_no");
-        requireText(request.planName(), "plan_name");
+        requireField(request.planName(), "平面图名称");
         requireText(request.imageUrl(), "image_url");
         requireText(request.versionNo(), "version_no");
         requirePositive(request.imageWidth(), "image_width");
@@ -514,7 +535,7 @@ public class FloorPlanService {
         normalizeAllowed(request.status(), "status", STATUSES);
         List<FloorPlanPoint> points = request.polygonPoints();
         if (points == null || points.size() < 3) {
-            throw ApiException.badRequest("VALIDATION_FAILED", "多边形顶点（polygon_points）至少需要 3 个点");
+            throw ApiException.badRequest("VALIDATION_FAILED", "多边形至少需要 3 个顶点");
         }
         for (FloorPlanPoint point : points) {
             if (point == null || point.x() < 0 || point.x() > 1 || point.y() < 0 || point.y() > 1) {
@@ -532,7 +553,7 @@ public class FloorPlanService {
         requireText(request.sourceName(), "source_name");
         String payload = requireText(request.sourcePayload(), "source_payload");
         if (payload.length() > 1_000_000) {
-            throw ApiException.badRequest("VALIDATION_FAILED", "导入内容（source_payload）超过 1000000 字符上限");
+            throw ApiException.badRequest("VALIDATION_FAILED", "导入内容超过 100 万字符上限");
         }
     }
 
@@ -561,7 +582,10 @@ public class FloorPlanService {
         }
         requireText(request.idempotencyKey(), "idempotency_key");
         if (request.candidateIds() == null || request.candidateIds().isEmpty()) {
-            throw ApiException.badRequest("VALIDATION_FAILED", "candidate_ids is required");
+            throw ApiException.badRequest("VALIDATION_FAILED", "候选项列表不能为空");
+        }
+        if (request.candidateIds().size() > MAX_PUBLISH_CANDIDATES) {
+            throw ApiException.badRequest("VALIDATION_FAILED", "一次发布的候选项不能超过 " + MAX_PUBLISH_CANDIDATES + " 个");
         }
     }
 
@@ -569,9 +593,21 @@ public class FloorPlanService {
         FloorPlanRecord fp = repository.findFloorPlanById(id)
                 .orElseThrow(() -> ApiException.notFound("FLOOR_PLAN_NOT_FOUND", "平面图不存在或已被删除"));
         checkMerchantOwnership(fp.merchantId());
+        checkProjectOwnership(fp.projectId());
         return fp;
     }
 
+    // M015-003：项目员工只能访问本项目平面图（BEQR-152）；商户账号走商户闸、平台管理员 scope 为空放行。
+    private void checkProjectOwnership(String resourceProjectId) {
+        if (authorizationService.currentMerchantScope().isPresent()) {
+            return;
+        }
+        Optional<String> scope = authorizationService.currentProjectScopeQuietly();
+        if (scope.isPresent() && !scope.get().equals(resourceProjectId)) {
+            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "平面图属于其他综合体项目");
+        }
+    }
+
     private void checkMerchantOwnership(String resourceMerchantId) {
         Optional<String> scope = authorizationService.currentMerchantScope();
         if (scope.isPresent() && !scope.get().equals(resourceMerchantId)) {
@@ -598,11 +634,19 @@ public class FloorPlanService {
     private String normalizeAllowed(String value, String field, Set<String> allowed) {
         String normalized = requireText(value, field).toLowerCase(Locale.ROOT);
         if (!allowed.contains(normalized)) {
-            throw ApiException.badRequest("VALIDATION_FAILED", field + " is invalid");
+            throw ApiException.badRequest("VALIDATION_FAILED", FIELD_LABELS.getOrDefault(field, field) + "取值无效");
         }
         return normalized;
     }
 
+    private String requireField(String value, String label) {
+        String trimmed = trimToNull(value);
+        if (trimmed == null) {
+            throw ApiException.badRequest("VALIDATION_FAILED", label + "不能为空");
+        }
+        return trimmed;
+    }
+
     private String slug(String value) {
         return requireText(value, "idempotency_key").toLowerCase(Locale.ROOT)
                 .replaceAll("[^a-z0-9_-]", "_");
diff --git a/backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java b/backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java
index 333f52be..8f274547 100644
--- a/backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java
+++ b/backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java
@@ -1,4 +1,5 @@
 package com.complex.platform.floorplan;
+// 文件版本：V0.01
 import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;
 
 import com.fasterxml.jackson.core.JsonProcessingException;
@@ -60,6 +61,13 @@ class JdbcFloorPlanRepository implements FloorPlanRepository {
                 """, Map.of("project_id", projectId), this::mapFloorPlan);
     }
 
+    @Override
+    public boolean projectExists(String projectId) {
+        Long count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM projects WHERE id = :id",
+                Map.of("id", projectId), Long.class);
+        return count != null && count > 0;
+    }
+
     @Override
     public Optional<FloorPlanRecord> findFloorPlanById(String id) {
         return jdbcTemplate.query("""
diff --git a/backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java b/backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java
index d7e0c021..30946ec5 100644
--- a/backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java
+++ b/backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java
@@ -1,4 +1,5 @@
 package com.complex.platform.governance;
+// 文件版本：V0.01
 
 import java.time.Instant;
 import java.time.LocalDate;
@@ -6,6 +7,7 @@ import java.time.LocalTime;
 import java.time.ZoneOffset;
 import java.time.format.DateTimeFormatter;
 import java.util.List;
+import java.util.Optional;
 import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
 import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
 import org.springframework.stereotype.Repository;
@@ -45,6 +47,33 @@ class VenueBusinessHoursRepository {
                 rs.getObject("updated_at", java.time.OffsetDateTime.class).toInstant()));
     }
 
+    /** M015-009：场地＝spaces.id；返回其综合体项目归属（无项目归属返回空串），场地不存在返回 empty。 */
+    Optional<String> findVenueProjectId(String venueId) {
+        return jdbcTemplate.query("SELECT COALESCE(project_id, '') AS project_id FROM spaces WHERE id = :id",
+                new MapSqlParameterSource("id", venueId), (rs, n) -> rs.getString("project_id")).stream().findFirst();
+    }
+
+    /** M015-009：按综合体项目过滤的营业时间行（BEQR-152 项目隔离；JOIN spaces 取 project_id）。 */
+    List<VenueBusinessHoursRecord> listByProject(String projectId) {
+        return jdbcTemplate.query("""
+                SELECT h.id, h.venue_id, h.day_of_week, h.open_time, h.close_time, h.is_draft, h.effective_from,
+                       h.created_at, h.updated_at
+                  FROM venue_business_hours h
+                  JOIN spaces s ON s.id = h.venue_id
+                 WHERE s.project_id = :project_id
+                 ORDER BY h.venue_id, h.day_of_week
+                """, new MapSqlParameterSource("project_id", projectId), (rs, n) -> new VenueBusinessHoursRecord(
+                rs.getString("id"),
+                rs.getString("venue_id"),
+                rs.getInt("day_of_week"),
+                rs.getObject("open_time", LocalTime.class).format(HH_MM),
+                rs.getObject("close_time", LocalTime.class).format(HH_MM),
+                rs.getBoolean("is_draft"),
+                rs.getObject("effective_from", LocalDate.class),
+                rs.getObject("created_at", java.time.OffsetDateTime.class).toInstant(),
+                rs.getObject("updated_at", java.time.OffsetDateTime.class).toInstant()));
+    }
+
     /** 按 (venue_id, day_of_week) 幂等覆盖（V226 唯一键）。返回落库后的行。 */
     VenueBusinessHoursRecord upsert(String id, String venueId, int dayOfWeek, LocalTime openTime,
                                     LocalTime closeTime, boolean disabled, LocalDate effectiveFrom, Instant now) {
diff --git a/backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java b/backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java
index 291cb91b..2192b92b 100644
--- a/backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java
+++ b/backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java
@@ -1,5 +1,7 @@
 package com.complex.platform.governance;
+// 文件版本：V0.01
 
+import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
 import com.complex.platform.evidence.AuditEventService;
 import com.complex.platform.governance.dto.VenueBusinessHoursRequest;
@@ -8,6 +10,7 @@ import java.time.LocalTime;
 import java.time.format.DateTimeParseException;
 import java.util.List;
 import java.util.Map;
+import java.util.Optional;
 import java.util.UUID;
 import org.springframework.stereotype.Service;
 
@@ -27,14 +30,21 @@ public class VenueBusinessHoursService {
 
     private final VenueBusinessHoursRepository repository;
     private final AuditEventService auditEventService;
+    private final AuthorizationService authorizationService;
 
-    VenueBusinessHoursService(VenueBusinessHoursRepository repository, AuditEventService auditEventService) {
+    VenueBusinessHoursService(VenueBusinessHoursRepository repository, AuditEventService auditEventService,
+                              AuthorizationService authorizationService) {
         this.repository = repository;
         this.auditEventService = auditEventService;
+        this.authorizationService = authorizationService;
     }
 
     public List<VenueBusinessHoursRecord> list() {
-        List<VenueBusinessHoursRecord> rows = repository.listAll();
+        // M015-009：项目员工只看本项目场地（BEQR-152）；平台管理员 scope 为空取全量。
+        Optional<String> projectScope = authorizationService.currentProjectScopeQuietly();
+        List<VenueBusinessHoursRecord> rows = projectScope.isPresent()
+                ? repository.listByProject(projectScope.get())
+                : repository.listAll();
         auditEventService.recordSuccess(
                 "venue_business_hours.viewed", "governance.space.view", "venue_business_hours", "all",
                 null, null, Map.of("count", rows.size()));
@@ -47,14 +57,22 @@ public class VenueBusinessHoursService {
         }
         String venueId = trimToNull(request.venueId());
         if (venueId == null) {
-            throw ApiException.badRequest("VALIDATION_FAILED", "venue_id is required");
+            throw ApiException.badRequest("VALIDATION_FAILED", "场地不能为空");
         }
+        // M015-009：场地必须是现存空间（spaces.id），且项目员工只能配置本项目场地。
+        String venueProjectId = repository.findVenueProjectId(venueId)
+                .orElseThrow(() -> ApiException.notFound("VENUE_NOT_FOUND", "场地不存在或已被删除"));
+        authorizationService.currentProjectScopeQuietly().ifPresent(scope -> {
+            if (!scope.equals(venueProjectId)) {
+                throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "场地属于其他综合体项目");
+            }
+        });
         Integer dayOfWeek = request.dayOfWeek();
         if (dayOfWeek == null || dayOfWeek < 0 || dayOfWeek > 6) {
             throw ApiException.badRequest("VALIDATION_FAILED", "星期取值必须为 0（周日）到 6（周六）");
         }
-        LocalTime openTime = parseTime(request.openTime(), "open_time");
-        LocalTime closeTime = parseTime(request.closeTime(), "close_time");
+        LocalTime openTime = parseTime(request.openTime(), "开始营业时间");
+        LocalTime closeTime = parseTime(request.closeTime(), "结束营业时间");
         // 开歇同刻 = 零长营业窗口 = 这一天谁都进不来，但页面上看着像配好了。宁可拒，不留这种静默坑。
         if (openTime.equals(closeTime)) {
             throw ApiException.badRequest("VALIDATION_FAILED", "开始营业时间与结束营业时间不能相同");
@@ -78,12 +96,12 @@ public class VenueBusinessHoursService {
     private LocalTime parseTime(String raw, String field) {
         String value = trimToNull(raw);
         if (value == null) {
-            throw ApiException.badRequest("VALIDATION_FAILED", field + " 必须填写，格式 HH:mm");
+            throw ApiException.badRequest("VALIDATION_FAILED", field + "必须填写，格式 HH:mm");
         }
         try {
             return LocalTime.parse(value.length() == 5 ? value : value.substring(0, 5));
         } catch (DateTimeParseException | IndexOutOfBoundsException exception) {
-            throw ApiException.badRequest("VALIDATION_FAILED", field + " 格式不正确，应为 HH:mm");
+            throw ApiException.badRequest("VALIDATION_FAILED", field + "格式不正确，应为 HH:mm");
         }
     }
 
diff --git a/backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java
index 530cec72..c2cfeb97 100644
--- a/backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java
@@ -1,4 +1,5 @@
 package com.complex.platform.floorplan;
+// 文件版本：V0.01
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.groups.Tuple.tuple;
@@ -161,7 +162,7 @@ class FloorPlanJdbcIntegrationTests {
                 1,
                 "points",
                 "POINTS_TOO_FEW",
-                "polygon requires at least 3 points",
+                "多边形至少需要 3 个顶点",
                 now
         ));
 
diff --git a/backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java b/backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java
index 149dfffe..ec368566 100644
--- a/backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java
@@ -1,4 +1,5 @@
 package com.complex.platform.floorplan;
+// 文件版本：V0.01
 
 import com.complex.platform.support.InMemoryAuditEventRepository;
 import com.complex.platform.support.InMemoryIdempotencyRepository;
@@ -24,6 +25,7 @@ import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Optional;
+import java.util.Set;
 import org.junit.jupiter.api.BeforeEach;
 import org.junit.jupiter.api.Test;
 
@@ -326,15 +328,131 @@ class FloorPlanServiceTests {
                 .get().extracting(FloorPlanRecord::merchantId).isEqualTo("m_2");
     }
 
+    // M015-001: 图形/导入任务主键须含平面图域——跨平面图同一客户端幂等键不得覆盖他图数据（修复前此两例红）
+    @Test
+    void saveShapeWithSameKeyAcrossFloorPlansDoesNotOverwrite() {
+        FloorPlanRecord planA = service.createFloorPlan(createRequest("fp-shape-key-a"));
+        FloorPlanRecord planB = service.createFloorPlan(createRequest("fp-shape-key-b"));
+        FloorPlanShapeRecord shapeOnA = service.saveShape(planA.id(), shapeRequest("shared-shape-key", null, polygon()));
+
+        FloorPlanShapeRecord shapeOnB = service.saveShape(planB.id(), shapeRequest("shared-shape-key", null, polygon()));
+
+        assertThat(shapeOnB.id()).isNotEqualTo(shapeOnA.id());
+        assertThat(repository.findShapeById(shapeOnA.id())).get()
+                .extracting(FloorPlanShapeRecord::floorPlanId).isEqualTo(planA.id());
+        assertThat(repository.findShapesByFloorPlanId(planA.id())).hasSize(1);
+        assertThat(repository.findShapesByFloorPlanId(planB.id())).hasSize(1);
+    }
+
+    @Test
+    void createImportTaskWithSameKeyAcrossFloorPlansDoesNotOverwrite() {
+        FloorPlanRecord planA = service.createFloorPlan(createRequest("fp-import-key-a"));
+        FloorPlanRecord planB = service.createFloorPlan(createRequest("fp-import-key-b"));
+        FloorPlanImportTaskRecord taskOnA = service.createImportTask(planA.id(), importRequest("shared-import-key"));
+
+        FloorPlanImportTaskRecord taskOnB = service.createImportTask(planB.id(), importRequest("shared-import-key"));
+
+        assertThat(taskOnB.id()).isNotEqualTo(taskOnA.id());
+        assertThat(repository.findImportTaskById(taskOnA.id())).get()
+                .extracting(FloorPlanImportTaskRecord::floorPlanId).isEqualTo(planA.id());
+        assertThat(repository.findImportCandidatesByTaskId(taskOnA.id())).hasSize(1);
+        assertThat(repository.findImportCandidatesByTaskId(taskOnB.id())).hasSize(1);
+    }
+
+    // M015-007：列表不分页时 pageSize/total 必须等于真实条数（修复前 pageSize 谎报 50 → 此例红）
+    @Test
+    void listShapesReportsHonestPageSizeAndTotal() {
+        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("fp-honest-page"));
+        service.saveShape(floorPlan.id(), shapeRequest("shape-page-1", null, polygon()));
+        service.saveShape(floorPlan.id(), shapeRequest("shape-page-2", null, polygon()));
+
+        PageResponse<FloorPlanShapeRecord> page = service.listShapes(floorPlan.id());
+
+        assertThat(page.items()).hasSize(2);
+        assertThat(page.total()).isEqualTo(2);
+        assertThat(page.pageSize()).isEqualTo(2);
+    }
+
+    // M015-002: 平面图必须带综合体项目归属且项目须存在（PGM-017/PGM-022；修复前此两例红）
+    @Test
+    void createFloorPlanRequiresProjectId() {
+        assertThatThrownBy(() -> service.createFloorPlan(createRequest("floor-plan-no-project", " ")))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));
+        assertThat(repository.floorPlanSaveCount).isZero();
+    }
+
+    @Test
+    void createFloorPlanRejectsUnknownProject() {
+        assertThatThrownBy(() -> service.createFloorPlan(createRequest("floor-plan-unknown-project", "prj_missing")))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("PROJECT_NOT_FOUND"));
+        assertThat(repository.floorPlanSaveCount).isZero();
+    }
+
+    // M015-003: 项目员工按项目 scope 隔离（BEQR-152）；修复前此三例红
+    @Test
+    void listFloorPlansFiltersToProjectScope() {
+        service.createFloorPlan(createRequest("fp-project-scope-1", "prj_1"));
+        service.createFloorPlan(createRequest("fp-project-scope-2", "prj_2"));
+
+        PageResponse<FloorPlanRecord> scoped = serviceWithProjectScope("prj_1").listFloorPlans();
+
+        assertThat(scoped.items()).extracting(FloorPlanRecord::projectId).containsOnly("prj_1");
+        assertThat(service.listFloorPlans().items()).hasSize(2);
+    }
+
+    @Test
+    void accessingFloorPlanBelongingToDifferentProjectIsForbidden() {
+        FloorPlanRecord planOnProject2 = service.createFloorPlan(createRequest("fp-project-access-2", "prj_2"));
+
+        assertThatThrownBy(() -> serviceWithProjectScope("prj_1").listShapes(planOnProject2.id()))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
+    }
+
+    @Test
+    void createFloorPlanOutsideProjectScopeIsForbidden() {
+        assertThatThrownBy(() -> serviceWithProjectScope("prj_1").createFloorPlan(createRequest("fp-project-create-2", "prj_2")))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
+        assertThat(repository.floorPlanSaveCount).isZero();
+    }
+
     private AuthorizationService noScopeAuth() {
         return new AuthorizationService(null) {
             @Override
             public Optional<String> currentMerchantScope() {
                 return Optional.empty();
             }
+
+            @Override
+            public Optional<String> currentProjectScopeQuietly() {
+                return Optional.empty();
+            }
         };
     }
 
+    private FloorPlanService serviceWithProjectScope(String projectId) {
+        return new FloorPlanService(
+                repository,
+                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
+                new AuditEventService(auditEventRepository),
+                new FloorPlanImportParser(),
+                new AuthorizationService(null) {
+                    @Override
+                    public Optional<String> currentMerchantScope() {
+                        return Optional.empty();
+                    }
+
+                    @Override
+                    public Optional<String> currentProjectScopeQuietly() {
+                        return Optional.of(projectId);
+                    }
+                }
+        );
+    }
+
     private FloorPlanService serviceWithMerchantScope(String merchantId) {
         return new FloorPlanService(
                 repository,
@@ -351,10 +469,14 @@ class FloorPlanServiceTests {
     }
 
     private FloorPlanCreateRequest createRequest(String idempotencyKey) {
+        return createRequest(idempotencyKey, "prj_1");
+    }
+
+    private FloorPlanCreateRequest createRequest(String idempotencyKey, String projectId) {
         return new FloorPlanCreateRequest(
                 idempotencyKey,
                 "create floor plan",
-                "prj_1",
+                projectId,
                 "A",
                 "1F",
                 "1F plan",
@@ -415,6 +537,18 @@ class FloorPlanServiceTests {
         private int shapeSaveCount;
         private int importTaskSaveCount;
 
+        @Override
+        public boolean projectExists(String projectId) {
+            return Set.of("prj_1", "prj_2").contains(projectId);
+        }
+
+        @Override
+        public List<FloorPlanRecord> findAllFloorPlansByProject(String projectId) {
+            return floorPlans.values().stream()
+                    .filter(fp -> projectId.equals(fp.projectId()))
+                    .toList();
+        }
+
         @Override
         public List<FloorPlanRecord> findAllFloorPlans() {
             return new ArrayList<>(floorPlans.values());
diff --git a/backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java b/backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java
index e7d429b4..2c6d2ce6 100644
--- a/backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java
@@ -1,9 +1,11 @@
 package com.complex.platform.governance;
+// 文件版本：V0.01
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
 import static org.mockito.Mockito.mock;
 
+import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
 import com.complex.platform.evidence.AuditEventService;
 import com.complex.platform.governance.dto.VenueBusinessHoursRequest;
@@ -12,6 +14,8 @@ import java.time.LocalDate;
 import java.time.LocalTime;
 import java.util.ArrayList;
 import java.util.List;
+import java.util.Map;
+import java.util.Optional;
 import org.junit.jupiter.api.Test;
 
 /**
@@ -31,6 +35,24 @@ class VenueBusinessHoursServiceTests {
             super(null);
         }
 
+        private final List<String> projectQueries = new ArrayList<>();
+
+        @Override
+        Optional<String> findVenueProjectId(String venueId) {
+            return Optional.ofNullable(Map.of("sp_bar_main", "prj_1", "sp_bar_side", "prj_2").get(venueId));
+        }
+
+        @Override
+        List<VenueBusinessHoursRecord> listAll() {
+            return new ArrayList<>(saved);
+        }
+
+        @Override
+        List<VenueBusinessHoursRecord> listByProject(String projectId) {
+            projectQueries.add(projectId);
+            return saved.stream().filter(r -> projectId.equals(findVenueProjectId(r.venueId()).orElse(null))).toList();
+        }
+
         @Override
         VenueBusinessHoursRecord upsert(String id, String venueId, int dayOfWeek, LocalTime openTime,
                                         LocalTime closeTime, boolean disabled, LocalDate effectiveFrom, Instant now) {
@@ -46,11 +68,51 @@ class VenueBusinessHoursServiceTests {
     private VenueBusinessHoursService service;
 
     private VenueBusinessHoursService service() {
+        return service(null);
+    }
+
+    private VenueBusinessHoursService service(String projectScope) {
         repository = new RecordingRepository();
-        service = new VenueBusinessHoursService(repository, mock(AuditEventService.class));
+        service = new VenueBusinessHoursService(repository, mock(AuditEventService.class), auth(projectScope));
         return service;
     }
 
+    private static AuthorizationService auth(String projectScope) {
+        return new AuthorizationService(null) {
+            @Override
+            public Optional<String> currentProjectScopeQuietly() {
+                return Optional.ofNullable(projectScope);
+            }
+        };
+    }
+
+    // M015-009：场地必须是现存空间（spaces.id），且项目员工只能读写本项目场地（BEQR-152）；修复前此三例红
+    @Test
+    void rejectsUnknownVenue() {
+        assertThatThrownBy(() -> service().save(
+                new VenueBusinessHoursRequest("idem-1", "sp_missing", 1, "18:00", "23:00", false, null)))
+                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("VENUE_NOT_FOUND"));
+        assertThat(repository.saved).isEmpty();
+    }
+
+    @Test
+    void rejectsVenueOutsideProjectScope() {
+        assertThatThrownBy(() -> service("prj_2").save(request(1, "18:00", "23:00")))
+                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
+        assertThat(repository.saved).isEmpty();
+    }
+
+    @Test
+    void listFiltersByProjectScope() {
+        VenueBusinessHoursService scoped = service("prj_1");
+        scoped.save(request(1, "18:00", "23:00"));
+        repository.saved.add(new VenueBusinessHoursRecord(
+                "vbh_side", "sp_bar_side", 1, "18:00", "23:00", false, null, Instant.EPOCH, Instant.EPOCH));
+
+        assertThat(scoped.list()).extracting(VenueBusinessHoursRecord::venueId).containsOnly("sp_bar_main");
+        assertThat(repository.projectQueries).containsExactly("prj_1");
+    }
+
     private VenueBusinessHoursRequest request(Integer dayOfWeek, String open, String close) {
         return new VenueBusinessHoursRequest("idem-1", "sp_bar_main", dayOfWeek, open, close, false, null);
     }

```


## 4. 本席复审结论（席甲，作者自审票）

本席逐块读完上述 diff 全文（9 文件：6 源码 + 3 测试类），以 §2 归因表核对每一行；§3 为本席 B3-r1 运行记录原文（RED 11 例失败 → GREEN 5 类 54 例全绿，主工作区实跑，只导出三个 COMPLEX_DATABASE_* 变量、complex_test）。

### 4.1 修复内容（逐 ID）

| ID | 修复内容 | 证据（diff 块） | 本席票 |
|---|---|---|---|
| 001 | 图形/导入任务主键改为 `"fps_"+slug(floorPlanId+"_"+key)` / `"fpi_"+slug(floorPlanId+"_"+key)`；候选/错误 ID 仍随任务 ID；发布追加语义未动 | FloorPlanService 两处 slug 输入；FloorPlanServiceTests `saveShapeWithSameKeyAcrossFloorPlansDoesNotOverwrite` / `createImportTaskWithSameKeyAcrossFloorPlansDoesNotOverwrite`（RED 时因同 id 覆盖而红） | PASS |
| 002 | `validateCreateRequest` 与 `createFloorPlanOnce` 以 `requireText(project_id)` 必填，`repository.projectExists` 不存在 → `PROJECT_NOT_FOUND`（404，中文）；`FloorPlanRepository.projectExists` 契约 + Jdbc `SELECT COUNT(1) FROM projects WHERE id = :id` | FloorPlanRepository/JdbcFloorPlanRepository 新方法；`createFloorPlanRequiresProjectId` / `createFloorPlanRejectsUnknownProject`（RED 时 save 成功而红） | PASS |
| 003 | 列表：商户 scope 优先，否则 `currentProjectScopeQuietly().map(findAllFloorPlansByProject).orElseGet(findAllFloorPlans)`；`ensureFloorPlanExists` 加 `checkProjectOwnership`（商户 scope 存在则跳过，项目 scope 不等 → `CROSS_PROJECT_FORBIDDEN`）；创建时商户 scope 为空且项目 scope 存在则 project_id 须相等 | FloorPlanService 三处；`listFloorPlansFiltersToProjectScope` / `accessingFloorPlanBelongingToDifferentProjectIsForbidden` / `createFloorPlanOutsideProjectScopeIsForbidden`；noScopeAuth/serviceWithProjectScope 覆盖 `currentProjectScopeQuietly`（真实实现对无认证上下文抛 UNAUTHENTICATED） | PASS |
| 004 | Parser 13 处 error_message 改中文整句，error_code 与 field 不变；Jdbc 集成测试夹具字符串随改 | FloorPlanImportParser 13 行；FloorPlanJdbcIntegrationTests 1 行 | PASS |
| 005 | `plan_name` 改 `requireField(planName,"平面图名称")`（两处）；`candidate_ids is required` 两处改「候选项列表不能为空」；`normalizeAllowed` 改 `FIELD_LABELS.getOrDefault(field, field)+"取值无效"`（status/shape_type/ref_type/source_format 四键本模块表） | FloorPlanService；不改 FieldLabels | PASS |
| 006 | `:517`「多边形至少需要 3 个顶点」、`:535`「导入内容超过 100 万字符上限」；VenueBusinessHoursService「场地不能为空」「开始营业时间/结束营业时间必须填写，格式 HH:mm」 | FloorPlanService 2 行；VenueBusinessHoursService 4 行 | PASS |
| 007 | 三处 `new PageResponse<>(records, 1, Math.max(records.size(), 1), records.size())`（r2 定稿：诚实不分页，不硬编码 500、不新增参数） | FloorPlanService 三处；`listShapesReportsHonestPageSizeAndTotal`（RED 时 pageSize=50 而红） | PASS |
| 008 | `MAX_PUBLISH_CANDIDATES=200`，`candidateIds.size()>200` → VALIDATION_FAILED 中文 | FloorPlanService `validateImportPublishRequest` | PASS |
| 009 | 构造注入 `AuthorizationService`；`save` 先 `findVenueProjectId`（`SELECT COALESCE(project_id,'') FROM spaces WHERE id=:id`）不存在 → `VENUE_NOT_FOUND`（404），项目 scope 不等 → `CROSS_PROJECT_FORBIDDEN`；`list` 有项目 scope 走 `listByProject`（JOIN spaces） | VenueBusinessHoursRepository 两方法；VenueBusinessHoursService；`rejectsUnknownVenue` / `rejectsVenueOutsideProjectScope` / `listFiltersByProjectScope`（RED 时无校验而红） | PASS |

版本铁律：9 个改动文件各加一行「// 文件版本：V0.01」，随所属 ID 归因。

### 4.2 范围归因（§5 闸 3）

- 全部 hunk 落到定稿 001–009；版本头行随所属文件；归因不到的行 0。
- 未改：迁移、OpenAPI、三份业务 HTML、锁/sidecar、权限种子、FieldLabels、PlatformController、前端、ticketing、其它模块源码（diffstat 仅 9 文件）。
- 010/011/012/013（否/XM）未在本批落地：`VenueBusinessHoursService` 仍「保存即生效」、发布 DTO 无新增字段、无新迁移、SHAPE_TYPES 六值未动。
- 未提交/推送/合并/部署。

### 4.3 结论

内容修复：**PASS**。范围归因：**PASS**。总：**PASS**（作者自审票；须 Grok B4-r1 亦 PASS 才进 B5-r1；B3 累计 1/3）。本席在本报告落盘前未读取 `M015-b4-r1-grok-raw.md`。


## 5. 读取 Grok B4-r1 后的席甲复核（V0.02 追加段；上方 V0.01 原字节 73948 bytes 保留，本段不写独占锁行）

2026-09-11T07:44:46Z。Grok B4-r1（`modules/M015-b4-r1-grok-raw.md` V0.01，64112 bytes SHA256 26b42b8203b80268e720118664965f0a812b9b33094bf20529a6345e77e3b6a7）票：内容修复 FAIL（001 引入 VARCHAR(64) 溢出；003 未修好）、范围归因 PASS、总 FAIL。本席核源（receiving-code-review，只读）：

- 003：`AuthorizationService.currentMerchantScope:422-431` 对非平台管理员且无 merchantId 抛 `MERCHANT_SCOPE_REQUIRED`；本席 B3-r1 在 `listFloorPlans`、`createFloorPlanOnce`、`validateCreateRequest`、`checkProjectOwnership`、`checkMerchantOwnership` 五处仍先调它，项目员工（有 organizationId、无 merchantId）永远到不了 `currentProjectScopeQuietly` 分支。`currentMerchantIdQuietly:440-455` 永不抛、平台管理员桥为空，TicketingService/AdmissionGuard 已用。本席测试桩把「商户空 ∩ 项目有」桩成可达，生产不存在该组合——Grok 判定成立。§4.1 003 PASS 票撤回。
- 001：V023/V034 四表 `id VARCHAR(64)`。现网前端导入键 `fpi-${Date.now()}`（17 字符）：任务 id 61 可落，候选/错误 id `fpic_/fpie_ + slug(taskId+"_"+n)` = 68 超限；发布图形 id `fps_ + slug(taskId+"_"+candidate.id())` = 87；UUID 键下图形/任务 id 80。内存仓单测与短 id Jdbc 夹具都锁不住。Grok 判定成立。§4.1 001 PASS 票撤回。
- 范围归因：Grok 与本席一致 PASS，维持。
- Grok 观察（008 无新例、004 残留格式名 svg/standard_json/polyline/polygon/shapes/x/y）不升队，随 B5 观察登记。

本席 B4-r1 内容票改为 **FAIL**（001/003 有 ID → 整批回 B3-r2，B3 累计 1→2）。B3-r2 最小处置：001 派生主键改 `prefix + sha256(seed).substring(0, 32)`（图形/任务/发布图形三处，恒 36 字符，候选/错误 id 随任务 ≤46，不改迁移）+ 先红一例（现网键与 UUID 键均 ≤64）；003 五处改 `currentMerchantIdQuietly()`，测试桩改为生产语义（项目员工 `currentMerchantScope` 抛 MERCHANT_SCOPE_REQUIRED、商户员工 `currentProjectScopeQuietly` 抛 PROJECT_SCOPE_REQUIRED）使原 3 例先红。

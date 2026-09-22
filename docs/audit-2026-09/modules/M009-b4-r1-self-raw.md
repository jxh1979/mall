CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M009 B4-r1 修复内容复审 · 席甲完整报告
版本：V0.01

- 席甲：Claude Fable 5.1（作者自审票）。步骤：B4-r1（B3 第 1 次整批修复的完整 diff 复审）。2026-09-09T08:09:28Z
- before tree：`c5e66bd5e156c01319a3ceece584906716659999`；after tree：`80b11c54422e5f54873e1fc8bd4232322babf844`；`git diff --binary c5e66bd5e156 80b11c54422e` 全文 89605 bytes SHA256 6fd3527a9fe7e74696924d5ae59628291ec30efbfb03dd4a97a89a4eed83470e（全文附于本报告末尾）。
- 定稿队列（findings V1.19）：001–012、014–016 共 15 项；本轮只修这 15 项及归因到各 ID 的最小行为测试。
- 锁四值：实算/sidecar/ledger/Grok 均 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。

## 1. diffstat

```
.../admin/pages/OrganizationMerchantPage.test.tsx  |   6 +-
 .../src/admin/pages/OrganizationMerchantPage.tsx   |  24 +-
 .../platform/governance/BusinessEntityRecord.java  |   4 +-
 .../governance/JdbcPlatformRepository.java         |  43 ++--
 .../governance/MerchantDeleteDecision.java         |   3 +-
 .../platform/governance/MerchantRecord.java        |  11 +
 .../platform/governance/PlatformController.java    |   9 +
 .../platform/governance/PlatformRepository.java    |  16 +-
 .../platform/governance/PlatformService.java       | 235 +++++++++++++++-----
 .../governance/dto/BusinessEntityRequest.java      |   2 +-
 .../governance/dto/MerchantArchiveRequest.java     |   3 +-
 .../complex/platform/leasing/LeasingService.java   |   4 +-
 .../governance/PlatformControllerTests.java        |  15 ++
 .../governance/PlatformJdbcIntegrationTests.java   |  28 ++-
 .../platform/governance/PlatformServiceTests.java  | 243 +++++++++++++++++++--
 .../LeasingSignAtomicityJdbcIntegrationTests.java  |   5 +
 16 files changed, 527 insertions(+), 124 deletions(-)
```

## 2. 逐块归因（每一行改动都归到定稿 ID；「版本铁律」行随其所在文件的 ID）

| 文件 | 块 | 位置 | 块内标记 ID | 归因 |
|---|---|---|---|---|
| apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx | 1 | `@@ -1,3 +1,4 @@` | 版本铁律（随所属ID） | M009-003 |
| apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx | 2 | `@@ -164,7 +165,10 @@ describe("OrganizationMerchantPage KPI ` | M009-003 | M009-003 |
| apps/web/src/admin/pages/OrganizationMerchantPage.tsx | 1 | `@@ -1,3 +1,4 @@` | 版本铁律（随所属ID） | M009-003 |
| apps/web/src/admin/pages/OrganizationMerchantPage.tsx | 2 | `@@ -29,7 +30,18 @@ const TEXT = {` | M009-003 | M009-003 |
| apps/web/src/admin/pages/OrganizationMerchantPage.tsx | 3 | `@@ -69,7 +81,7 @@ export function OrganizationMerchantPage({` | — | M009-003 |
| apps/web/src/admin/pages/OrganizationMerchantPage.tsx | 4 | `@@ -263,7 +275,7 @@ export function OrganizationMerchantPage` | — | M009-003 |
| apps/web/src/admin/pages/OrganizationMerchantPage.tsx | 5 | `@@ -336,7 +348,7 @@ export function OrganizationMerchantPage` | — | M009-003 |
| apps/web/src/admin/pages/OrganizationMerchantPage.tsx | 6 | `@@ -450,7 +462,7 @@ export function OrganizationMerchantPage` | — | M009-003 |
| apps/web/src/admin/pages/OrganizationMerchantPage.tsx | 7 | `@@ -459,7 +471,7 @@ export function OrganizationMerchantPage` | — | M009-003 |
| backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java | 1 | `@@ -1,4 +1,5 @@` | 版本铁律（随所属ID） | M009-012 |
| backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java | 2 | `@@ -6,9 +7,6 @@ public record BusinessEntityRecord(` | — | M009-012 |
| backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | 1 | `@@ -1,4 +1,5 @@` | 版本铁律（随所属ID） | M009-006/007/010/012/016 |
| backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | 2 | `@@ -25,8 +26,6 @@ class JdbcPlatformRepository implements Pl` | — | M009-006/007/010/012/016 |
| backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | 3 | `@@ -46,17 +45,16 @@ class JdbcPlatformRepository implements ` | — | M009-006/007/010/012/016 |
| backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | 4 | `@@ -69,7 +67,7 @@ class JdbcPlatformRepository implements Pl` | — | M009-006/007/010/012/016 |
| backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | 5 | `@@ -138,7 +136,7 @@ class JdbcPlatformRepository implements ` | M009-010 | M009-006/007/010/012/016 |
| backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | 6 | `@@ -146,13 +144,26 @@ class JdbcPlatformRepository implement` | — | M009-006/007/010/012/016 |
| backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | 7 | `@@ -365,7 +376,7 @@ class JdbcPlatformRepository implements ` | — | M009-006/007/010/012/016 |
| backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | 8 | `@@ -382,7 +393,7 @@ class JdbcPlatformRepository implements ` | — | M009-006/007/010/012/016 |
| backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | 9 | `@@ -491,7 +502,6 @@ class JdbcPlatformRepository implements ` | — | M009-006/007/010/012/016 |
| backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | 10 | `@@ -580,7 +590,6 @@ class JdbcPlatformRepository implements ` | — | M009-006/007/010/012/016 |
| backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | 11 | `@@ -1076,14 +1085,6 @@ class JdbcPlatformRepository implemen` | — | M009-006/007/010/012/016 |
| backend/src/main/java/com/complex/platform/governance/MerchantDeleteDecision.java | 1 | `@@ -1,7 +1,8 @@` | 版本铁律（随所属ID） | M009-010 |
| backend/src/main/java/com/complex/platform/governance/MerchantRecord.java | 1 | `@@ -1,4 +1,5 @@` | 版本铁律（随所属ID） | M009-008 |
| backend/src/main/java/com/complex/platform/governance/MerchantRecord.java | 2 | `@@ -68,6 +69,16 @@ public record MerchantRecord(` | M009-008 | M009-008 |
| backend/src/main/java/com/complex/platform/governance/PlatformController.java | 1 | `@@ -1,4 +1,5 @@` | 版本铁律（随所属ID） | M009-015 |
| backend/src/main/java/com/complex/platform/governance/PlatformController.java | 2 | `@@ -41,6 +42,8 @@ public class PlatformController {` | M009-015 | M009-015 |
| backend/src/main/java/com/complex/platform/governance/PlatformController.java | 3 | `@@ -59,6 +62,7 @@ public class PlatformController {` | — | M009-015 |
| backend/src/main/java/com/complex/platform/governance/PlatformController.java | 4 | `@@ -122,6 +126,7 @@ public class PlatformController {` | — | M009-015 |
| backend/src/main/java/com/complex/platform/governance/PlatformController.java | 5 | `@@ -137,12 +142,14 @@ public class PlatformController {` | — | M009-015 |
| backend/src/main/java/com/complex/platform/governance/PlatformController.java | 6 | `@@ -165,6 +172,7 @@ public class PlatformController {` | — | M009-015 |
| backend/src/main/java/com/complex/platform/governance/PlatformController.java | 7 | `@@ -182,6 +190,7 @@ public class PlatformController {` | — | M009-015 |
| backend/src/main/java/com/complex/platform/governance/PlatformRepository.java | 1 | `@@ -1,4 +1,5 @@` | 版本铁律（随所属ID） | M009-006/007/010 |
| backend/src/main/java/com/complex/platform/governance/PlatformRepository.java | 2 | `@@ -19,7 +20,8 @@ public interface PlatformRepository {` | M009-010 | M009-006/007/010 |
| backend/src/main/java/com/complex/platform/governance/PlatformRepository.java | 3 | `@@ -29,7 +31,7 @@ public interface PlatformRepository {` | M009-010 | M009-006/007/010 |
| backend/src/main/java/com/complex/platform/governance/PlatformRepository.java | 4 | `@@ -64,6 +66,16 @@ public interface PlatformRepository {` | M009-006, M009-007 | M009-006/007/010 |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 1 | `@@ -1,4 +1,5 @@` | 版本铁律（随所属ID） | M009-001/002/003/004/005/006/007/008/009/010/011/014/016（逐块见标记） |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 2 | `@@ -32,7 +33,6 @@ import static com.complex.platform.common.` | — | M009-001/002/003/004/005/006/007/008/009/010/011/014/016（逐块见标记） |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 3 | `@@ -45,38 +45,38 @@ public class PlatformService {` | M009-003, M009-004, M009-005 | M009-001/002/003/004/005/006/007/008/009/010/011/014/016（逐块见标记） |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 4 | `@@ -111,17 +111,17 @@ public class PlatformService {` | — | M009-001/002/003/004/005/006/007/008/009/010/011/014/016（逐块见标记） |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 5 | `@@ -159,16 +159,19 @@ public class PlatformService {` | M009-001/002 | M009-001/002/003/004/005/006/007/008/009/010/011/014/016（逐块见标记） |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 6 | `@@ -203,13 +206,9 @@ public class PlatformService {` | M009-011 | M009-001/002/003/004/005/006/007/008/009/010/011/014/016（逐块见标记） |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 7 | `@@ -264,14 +263,27 @@ public class PlatformService {` | M009-009 | M009-001/002/003/004/005/006/007/008/009/010/011/014/016（逐块见标记） |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 8 | `@@ -296,6 +308,9 @@ public class PlatformService {` | — | M009-001/002/003/004/005/006/007/008/009/010/011/014/016（逐块见标记） |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 9 | `@@ -307,7 +322,7 @@ public class PlatformService {` | — | M009-001/002/003/004/005/006/007/008/009/010/011/014/016（逐块见标记） |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 10 | `@@ -356,16 +371,21 @@ public class PlatformService {` | — | M009-001/002/003/004/005/006/007/008/009/010/011/014/016（逐块见标记） |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 11 | `@@ -401,7 +421,7 @@ public class PlatformService {` | — | M009-001/002/003/004/005/006/007/008/009/010/011/014/016（逐块见标记） |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 12 | `@@ -445,12 +465,24 @@ public class PlatformService {` | — | M009-001/002/003/004/005/006/007/008/009/010/011/014/016（逐块见标记） |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 13 | `@@ -510,6 +542,11 @@ public class PlatformService {` | M009-006 | M009-001/002/003/004/005/006/007/008/009/010/011/014/016（逐块见标记） |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 14 | `@@ -517,12 +554,12 @@ public class PlatformService {` | — | M009-001/002/003/004/005/006/007/008/009/010/011/014/016（逐块见标记） |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 15 | `@@ -530,6 +567,8 @@ public class PlatformService {` | — | M009-001/002/003/004/005/006/007/008/009/010/011/014/016（逐块见标记） |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 16 | `@@ -565,7 +604,7 @@ public class PlatformService {` | — | M009-001/002/003/004/005/006/007/008/009/010/011/014/016（逐块见标记） |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 17 | `@@ -578,6 +617,10 @@ public class PlatformService {` | — | M009-001/002/003/004/005/006/007/008/009/010/011/014/016（逐块见标记） |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 18 | `@@ -596,10 +639,82 @@ public class PlatformService {` | M009-003, M009-004, M009-005, M009-006, M009-008, M009-009 | M009-001/002/003/004/005/006/007/008/009/010/011/014/016（逐块见标记） |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 19 | `@@ -613,14 +728,19 @@ public class PlatformService {` | M009-001/002, M009-008 | M009-001/002/003/004/005/006/007/008/009/010/011/014/016（逐块见标记） |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 20 | `@@ -630,10 +750,16 @@ public class PlatformService {` | — | M009-001/002/003/004/005/006/007/008/009/010/011/014/016（逐块见标记） |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 21 | `@@ -662,10 +788,6 @@ public class PlatformService {` | — | M009-001/002/003/004/005/006/007/008/009/010/011/014/016（逐块见标记） |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 22 | `@@ -678,13 +800,6 @@ public class PlatformService {` | — | M009-001/002/003/004/005/006/007/008/009/010/011/014/016（逐块见标记） |
| backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java | 1 | `@@ -1,4 +1,5 @@` | 版本铁律（随所属ID） | M009-012 |
| backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java | 2 | `@@ -8,7 +9,6 @@ public record BusinessEntityRequest(` | — | M009-012 |
| backend/src/main/java/com/complex/platform/governance/dto/MerchantArchiveRequest.java | 1 | `@@ -1,5 +1,6 @@` | 版本铁律（随所属ID） | M009-010 |
| backend/src/main/java/com/complex/platform/leasing/LeasingService.java | 1 | `@@ -1,4 +1,5 @@` | 版本铁律（随所属ID） | M009-003 |
| backend/src/main/java/com/complex/platform/leasing/LeasingService.java | 2 | `@@ -287,7 +288,8 @@ public class LeasingService {` | M009-003 | M009-003 |
| backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java | 1 | `@@ -1,4 +1,5 @@` | 版本铁律（随所属ID） | M009-015 |
| backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java | 2 | `@@ -237,6 +238,20 @@ class PlatformControllerTests {` | M009-015 | M009-015 |
| backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | 1 | `@@ -1,4 +1,5 @@` | 版本铁律（随所属ID） | M009-006/010/012 |
| backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | 2 | `@@ -62,7 +63,7 @@ class PlatformJdbcIntegrationTests {` | M009-012 | M009-006/010/012 |
| backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | 3 | `@@ -91,7 +92,6 @@ class PlatformJdbcIntegrationTests {` | — | M009-006/010/012 |
| backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | 4 | `@@ -339,6 +339,24 @@ class PlatformJdbcIntegrationTests {` | M009-006, M009-010 | M009-006/010/012 |
| backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | 5 | `@@ -356,7 +374,7 @@ class PlatformJdbcIntegrationTests {` | — | M009-006/010/012 |
| backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | 6 | `@@ -378,7 +396,7 @@ class PlatformJdbcIntegrationTests {` | — | M009-006/010/012 |
| backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | 7 | `@@ -392,7 +410,7 @@ class PlatformJdbcIntegrationTests {` | — | M009-006/010/012 |
| backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 1 | `@@ -1,5 +1,8 @@` | 版本铁律（随所属ID） | M009-001/002/003/004/005/006/007/008/009/014（归因见各测试注释） |
| backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 2 | `@@ -25,6 +28,7 @@ import com.complex.platform.governance.dto` | — | M009-001/002/003/004/005/006/007/008/009/014（归因见各测试注释） |
| backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 3 | `@@ -34,6 +38,8 @@ import java.util.Optional;` | — | M009-001/002/003/004/005/006/007/008/009/014（归因见各测试注释） |
| backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 4 | `@@ -104,6 +110,183 @@ class PlatformServiceTests {` | M009-001/002, M009-003, M009-004, M009-005, M009-006, M009-007, M009-008, M009-009, M009-014 | M009-001/002/003/004/005/006/007/008/009/014（归因见各测试注释） |
| backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 5 | `@@ -152,7 +335,7 @@ class PlatformServiceTests {` | M009-009 | M009-001/002/003/004/005/006/007/008/009/014（归因见各测试注释） |
| backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 6 | `@@ -169,7 +352,7 @@ class PlatformServiceTests {` | M009-009 | M009-001/002/003/004/005/006/007/008/009/014（归因见各测试注释） |
| backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 7 | `@@ -187,8 +370,7 @@ class PlatformServiceTests {` | — | M009-001/002/003/004/005/006/007/008/009/014（归因见各测试注释） |
| backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 8 | `@@ -209,11 +391,12 @@ class PlatformServiceTests {` | M009-007 | M009-001/002/003/004/005/006/007/008/009/014（归因见各测试注释） |
| backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 9 | `@@ -231,7 +414,8 @@ class PlatformServiceTests {` | M009-002 | M009-001/002/003/004/005/006/007/008/009/014（归因见各测试注释） |
| backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 10 | `@@ -249,11 +433,11 @@ class PlatformServiceTests {` | — | M009-001/002/003/004/005/006/007/008/009/014（归因见各测试注释） |
| backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 11 | `@@ -262,7 +446,10 @@ class PlatformServiceTests {` | M009-008 | M009-001/002/003/004/005/006/007/008/009/014（归因见各测试注释） |
| backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 12 | `@@ -296,11 +483,16 @@ class PlatformServiceTests {` | M009-006 | M009-001/002/003/004/005/006/007/008/009/014（归因见各测试注释） |
| backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 13 | `@@ -311,12 +503,12 @@ class PlatformServiceTests {` | — | M009-001/002/003/004/005/006/007/008/009/014（归因见各测试注释） |
| backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 14 | `@@ -333,11 +525,16 @@ class PlatformServiceTests {` | M009-006 | M009-001/002/003/004/005/006/007/008/009/014（归因见各测试注释） |
| backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 15 | `@@ -353,7 +550,7 @@ class PlatformServiceTests {` | — | M009-001/002/003/004/005/006/007/008/009/014（归因见各测试注释） |
| backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 16 | `@@ -371,11 +568,15 @@ class PlatformServiceTests {` | M009-006 | M009-001/002/003/004/005/006/007/008/009/014（归因见各测试注释） |
| backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 17 | `@@ -397,8 +598,7 @@ class PlatformServiceTests {` | — | M009-001/002/003/004/005/006/007/008/009/014（归因见各测试注释） |
| backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 18 | `@@ -409,8 +609,7 @@ class PlatformServiceTests {` | — | M009-001/002/003/004/005/006/007/008/009/014（归因见各测试注释） |
| backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 19 | `@@ -431,7 +630,7 @@ class PlatformServiceTests {` | M009-007 | M009-001/002/003/004/005/006/007/008/009/014（归因见各测试注释） |
| backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 20 | `@@ -439,7 +638,7 @@ class PlatformServiceTests {` | — | M009-001/002/003/004/005/006/007/008/009/014（归因见各测试注释） |
| backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 21 | `@@ -447,7 +646,7 @@ class PlatformServiceTests {` | — | M009-001/002/003/004/005/006/007/008/009/014（归因见各测试注释） |
| backend/src/test/java/com/complex/platform/leasing/LeasingSignAtomicityJdbcIntegrationTests.java | 1 | `@@ -1,4 +1,5 @@` | 版本铁律（随所属ID） | ? |
| backend/src/test/java/com/complex/platform/leasing/LeasingSignAtomicityJdbcIntegrationTests.java | 2 | `@@ -51,6 +52,7 @@ class LeasingSignAtomicityJdbcIntegrationT` | M009-006 | ? |
| backend/src/test/java/com/complex/platform/leasing/LeasingSignAtomicityJdbcIntegrationTests.java | 3 | `@@ -114,6 +116,9 @@ class LeasingSignAtomicityJdbcIntegratio` | M009-006 | ? |

## 3. 红绿证据（B3 运行记录 /tmp/m009_b3_run.log 原文）

```
START=2026-09-09T07:52:03Z
BEFORE_TREE=c5e66bd5e156c01319a3ceece584906716659999
=== PHASE RED: apply tests + structural hunks (M009-006/007/008/012 compile-level) ===
RED_COPIED=15
RED_EXIT=1
[ERROR] Tests run: 12, Failures: 2, Errors: 0, Skipped: 0, Time elapsed: 3.024 s <<< FAILURE! -- in com.complex.platform.governance.PlatformJdbcIntegrationTests
[ERROR] com.complex.platform.governance.PlatformJdbcIntegrationTests.approvedDeletedMerchantStaysQueryableWithIncludeArchived -- Time elapsed: 0.180 s <<< FAILURE!
[ERROR] com.complex.platform.governance.PlatformJdbcIntegrationTests.residualMigrationCreatesBindingContractTablesAndSeedsPermissions -- Time elapsed: 0.006 s <<< FAILURE!
[ERROR] Tests run: 29, Failures: 13, Errors: 0, Skipped: 0, Time elapsed: 0.108 s <<< FAILURE! -- in com.complex.platform.governance.PlatformServiceTests
[ERROR] com.complex.platform.governance.PlatformServiceTests.updateMerchantKeepsArchiveAndPendingDeleteState -- Time elapsed: 0.003 s <<< FAILURE!
[ERROR] com.complex.platform.governance.PlatformServiceTests.createBusinessEntityBindingSyncsMerchantEntityColumns -- Time elapsed: 0.003 s <<< FAILURE!
[ERROR] com.complex.platform.governance.PlatformServiceTests.createSpaceRejectsUnknownProjectMerchantOrParent -- Time elapsed: 0.001 s <<< FAILURE!
[ERROR] com.complex.platform.governance.PlatformServiceTests.createProjectRejectsUnknownStatusOrMerchant -- Time elapsed: 0.002 s <<< FAILURE!
[ERROR] com.complex.platform.governance.PlatformServiceTests.updateMerchantWritesAuditAndPreservesId -- Time elapsed: 0.005 s <<< FAILURE!
[ERROR] com.complex.platform.governance.PlatformServiceTests.approveDeleteByNonMasterIsForbidden -- Time elapsed: 0.004 s <<< FAILURE!
[ERROR] com.complex.platform.governance.PlatformServiceTests.updateMerchantRejectsUnknownMerchant -- Time elapsed: 0.002 s <<< FAILURE!
[ERROR] com.complex.platform.governance.PlatformServiceTests.createMerchantRejectsCooperationFormOutsideSixModes -- Time elapsed: 0.001 s <<< FAILURE!
[ERROR] com.complex.platform.governance.PlatformServiceTests.requestDeleteByMasterAuthorityExecutesWithoutApproval -- Time elapsed: 0.002 s <<< FAILURE!
[ERROR] com.complex.platform.governance.PlatformServiceTests.listSpaceTypesPageSizeCoversAllItems -- Time elapsed: 0.001 s <<< FAILURE!
[ERROR] com.complex.platform.governance.PlatformServiceTests.createMerchantRejectsStatusOutsideEnumeratedStates -- Time elapsed: 0.002 s <<< FAILURE!
[ERROR] com.complex.platform.governance.PlatformServiceTests.createBusinessEntityRejectsRoleOutsideWhitelist -- Time elapsed: 0.001 s <<< FAILURE!
[ERROR] com.complex.platform.governance.PlatformServiceTests.createBusinessEntityBindingRejectsMissingMerchantOrEntity -- Time elapsed: 0.001 s <<< FAILURE!
[ERROR] Tests run: 10, Failures: 2, Errors: 0, Skipped: 0, Time elapsed: 0.712 s <<< FAILURE! -- in com.complex.platform.governance.PlatformControllerTests
[ERROR] com.complex.platform.governance.PlatformControllerTests.listBusinessEntityBindingsUsesEntityBindingViewPermission -- Time elapsed: 0.099 s <<< FAILURE!
[ERROR] com.complex.platform.governance.PlatformControllerTests.listingMerchantsRequiresPlatformScope -- Time elapsed: 0.010 s <<< FAILURE!
[ERROR] Tests run: 51, Failures: 17, Errors: 0, Skipped: 0
=== PHASE GREEN: apply behavioral hunks (M009-001..005/009/010/011/014/015/016 + 003 leasing/page) ===
GREEN_COPIED=15
GREEN_EXIT=1
[ERROR] Tests run: 12, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 4.411 s <<< FAILURE! -- in com.complex.platform.governance.PlatformJdbcIntegrationTests
[ERROR] com.complex.platform.governance.PlatformJdbcIntegrationTests.residualMigrationCreatesBindingContractTablesAndSeedsPermissions -- Time elapsed: 0.007 s <<< FAILURE!
[INFO] Tests run: 29, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.088 s -- in com.complex.platform.governance.PlatformServiceTests
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.738 s -- in com.complex.platform.governance.PlatformControllerTests
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.124 s -- in com.complex.platform.leasing.LeasingServiceTests
[ERROR] Tests run: 4, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.056 s <<< FAILURE! -- in com.complex.platform.leasing.LeasingJdbcIntegrationTests
[ERROR] com.complex.platform.leasing.LeasingJdbcIntegrationTests.migrationCreatesLeasingTablesAndSeedsPermissions -- Time elapsed: 0.005 s <<< FAILURE!
[ERROR] Tests run: 2, Failures: 1, Errors: 1, Skipped: 0, Time elapsed: 0.054 s <<< FAILURE! -- in com.complex.platform.leasing.LeasingSignAtomicityJdbcIntegrationTests
[ERROR] com.complex.platform.leasing.LeasingSignAtomicityJdbcIntegrationTests.signIntentFailureRollsBackMerchantBindingAndContract -- Time elapsed: 0.032 s <<< FAILURE!
[ERROR] Tests run: 68, Failures: 3, Errors: 1, Skipped: 0
=== WEB: OrganizationMerchantPage vitest ===
WEB_EXIT=0
 [32m✓[39m src/admin/pages/OrganizationMerchantPage.test.tsx [2m([22m[2m12 tests[22m[2m)[22m[33m 534[2mms[22m[39m
[2m Test Files [22m [1m[32m1 passed[39m[22m[90m (1)[39m
[2m      Tests [22m [1m[32m12 passed[39m[22m[90m (12)[39m
AFTER_TREE=4a5aa8a468146bc803b315e995f5a088cbec2ae3
END=2026-09-09T07:52:33Z
```

## 4. 本席复审结论

见文末追加的判定段。

## 附：完整 diff（before→after，--binary）

```diff
diff --git a/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx b/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
index 98ef3f22..5592efd6 100644
--- a/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
+++ b/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
@@ -1,3 +1,4 @@
+// 文件版本：V0.01
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
 import { afterEach, describe, expect, it, vi } from "vitest";
 
@@ -164,7 +165,10 @@ describe("OrganizationMerchantPage KPI 聚焦视图（带参重进本页）", ()
     expect(typeText).not.toContain("catering");
     // 单屏模式：经营与租赁（合作形式下拉）与基本信息同屏，无需步进。
     const coopText = screen.getByTestId("organization-merchant-cooperation").textContent ?? "";
-    expect(coopText).toContain("固定租金");
+    // M009-003：合作形式下拉只提供 HTML 六类（自营/租赁/联营/代运营/服务商接入/其他），不再是收费模板码。
+    expect(coopText).toContain("租赁");
+    expect(coopText).toContain("服务商接入");
+    expect(coopText).not.toContain("固定租金");
     expect(coopText).not.toContain("fixed_rent");
     expect(coopText).not.toContain("revenue_share");
   });
diff --git a/apps/web/src/admin/pages/OrganizationMerchantPage.tsx b/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
index 43271407..64b9cc0c 100644
--- a/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
+++ b/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
@@ -1,3 +1,4 @@
+// 文件版本：V0.01
 import React from "react";
 import { TechDetails } from "../components/TechDetails";
 import { AdminDataState } from "../components/AdminDataState";
@@ -29,7 +30,18 @@ const TEXT = {
 // 业态是通票分账按业态拆份额与分流结算的依据。娱乐平台公司对应的即酒吧业态，不另设「娱乐」业态；
 // 传媒＝演出提供方（HTML「演出票 → 传媒公司」）。
 const MERCHANT_TYPES = ["bar", "catering", "retail", "amusement", "media", "other"];
-const COOPERATION_FORMS = ["fixed_rent_plus_commission", "revenue_share", "fixed_rent"];
+// M009-003：合作方式六类（BEQR-148/BSM-028/PGM-026：自营/租赁/联营/代运营/服务商接入/其他）；代码与后端 PlatformService.MERCHANT_COOPERATION_FORMS 同词表。
+const COOPERATION_FORMS: ReadonlyArray<{ code: string; label: string }> = [
+  { code: "self_operated", label: "自营" },
+  { code: "lease", label: "租赁" },
+  { code: "joint_operation", label: "联营" },
+  { code: "managed_operation", label: "代运营" },
+  { code: "service_provider", label: "服务商接入" },
+  { code: "other", label: "其他" },
+];
+function cooperationLabel(code: string | null | undefined) {
+  return COOPERATION_FORMS.find((form) => form.code === code)?.label ?? formatBusinessStatus(code);
+}
 
 function display(value: unknown) {
   if (value === null || value === undefined || value === "") return "-";
@@ -69,7 +81,7 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
   const [merchantType, setMerchantType] = React.useState("");
   const [contactName, setContactName] = React.useState("运营联系人");
   const [contactPhone, setContactPhone] = React.useState("13800000000");
-  const [cooperationForm, setCooperationForm] = React.useState("fixed_rent_plus_commission");
+  const [cooperationForm, setCooperationForm] = React.useState("lease");
   const [formError, setFormError] = React.useState("");
   // 引导式创建流（一期范式）：主按钮 → 分步弹窗 → 确认创建。
   const [createOpen, setCreateOpen] = React.useState(false);
@@ -263,7 +275,7 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
         { label: "联系电话", value: display(row.contactPhone) },
         { label: "区块", value: display(row.mallBlockCode) },
         { label: "面积", value: display(row.leasedAreaSquareMeter ?? row.areaSquareMeter) },
-        { label: "合作形式", value: formatBusinessStatus(row.cooperationForm) },
+        { label: "合作形式", value: cooperationLabel(row.cooperationForm) },
         { label: "状态", value: formatBusinessStatus(row.status) },
         auditTraceField(navigateTo, "merchant", row.id),
       ],
@@ -336,7 +348,7 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
         <table className="admin-table" data-testid="organization-merchants-table">
           {/* 后端无“公司名/风险/留痕证据”字段（原按臆造字段读致整表“-”），列对齐真实字段：联系人/联系电话替换。 */}
           <thead><tr><th>商户</th><th>业态</th><th>联系人</th><th>联系电话</th><th>区块</th><th>面积</th><th>合作形式</th><th>状态</th><th>存档状态</th><th>操作</th><th>详情</th></tr></thead>
-          <tbody>{visibleMerchants.map((row) => <tr key={row.id} data-testid={`organization-merchant-row-${row.id}`} className={`admin-row-clickable${row.id === focusObjectId ? " admin-row-selected" : ""}`} tabIndex={0} onClick={() => openMerchantDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openMerchantDetail(row); } }}><td>{display(row.merchantName)}</td><td>{row.merchantType ? formatBusinessStatus(row.merchantType) : "-"}</td><td>{display(row.contactName)}</td><td>{display(row.contactPhone)}</td><td>{display(row.mallBlockCode)}</td><td>{display(row.leasedAreaSquareMeter ?? row.areaSquareMeter)}</td><td>{formatBusinessStatus(row.cooperationForm)}</td><td>{formatBusinessStatus(row.status)}</td><td>{merchantArchiveLabel(row)}</td><td onClick={(event) => event.stopPropagation()}>{merchantActions(row)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看商户档案详情" onClick={(event) => { event.stopPropagation(); openMerchantDetail(row); }}>查看详情</button></td></tr>)}</tbody>
+          <tbody>{visibleMerchants.map((row) => <tr key={row.id} data-testid={`organization-merchant-row-${row.id}`} className={`admin-row-clickable${row.id === focusObjectId ? " admin-row-selected" : ""}`} tabIndex={0} onClick={() => openMerchantDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openMerchantDetail(row); } }}><td>{display(row.merchantName)}</td><td>{row.merchantType ? formatBusinessStatus(row.merchantType) : "-"}</td><td>{display(row.contactName)}</td><td>{display(row.contactPhone)}</td><td>{display(row.mallBlockCode)}</td><td>{display(row.leasedAreaSquareMeter ?? row.areaSquareMeter)}</td><td>{cooperationLabel(row.cooperationForm)}</td><td>{formatBusinessStatus(row.status)}</td><td>{merchantArchiveLabel(row)}</td><td onClick={(event) => event.stopPropagation()}>{merchantActions(row)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看商户档案详情" onClick={(event) => { event.stopPropagation(); openMerchantDetail(row); }}>查看详情</button></td></tr>)}</tbody>
         </table>
       </div>
     </>
@@ -450,7 +462,7 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
             title: "经营与租赁",
             description: "选择与商户约定的合作形式。",
             content: (
-              <label>合作形式<select data-testid="organization-merchant-cooperation" value={cooperationForm} onChange={(event) => setCooperationForm(event.target.value)}>{COOPERATION_FORMS.map((form) => <option key={form} value={form}>{formatBusinessStatus(form)}</option>)}</select></label>
+              <label>合作形式<select data-testid="organization-merchant-cooperation" value={cooperationForm} onChange={(event) => setCooperationForm(event.target.value)}>{COOPERATION_FORMS.map((form) => <option key={form.code} value={form.code}>{form.label}</option>)}</select></label>
             ),
           },
         ]}
@@ -459,7 +471,7 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
           { label: "业态", value: merchantType ? formatBusinessStatus(merchantType) : "未选择" },
           { label: "联系人", value: contactName },
           { label: "联系电话", value: contactPhone },
-          { label: "合作形式", value: formatBusinessStatus(cooperationForm) },
+          { label: "合作形式", value: cooperationLabel(cooperationForm) },
         ]}
       />
       <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
diff --git a/backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java b/backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java
index 6a856656..41ef294b 100644
--- a/backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java
+++ b/backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java
@@ -1,4 +1,5 @@
 package com.complex.platform.governance;
+// 文件版本：V0.01
 
 import java.time.Instant;
 
@@ -6,9 +7,6 @@ public record BusinessEntityRecord(
         String id,
         String entityName,
         String entityRole,
-        // 四层组织(terminology-canonical §一):L3 管理公司/业态主体 的 parent = L2 项目公司;L2 的 parent = L1 平台。
-        // null = 顶层(平台)或未挂接。用于「一个项目公司下有多个管理公司主体」的层级落库。
-        String parentEntityId,
         String unifiedSocialCreditCode,
         String legalRepresentative,
         String bankAccountRef,
diff --git a/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java b/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java
index 51f12a0e..6536bda2 100644
--- a/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java
+++ b/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java
@@ -1,4 +1,5 @@
 package com.complex.platform.governance;
+// 文件版本：V0.01
 import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
 import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;
 
@@ -25,8 +26,6 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
 
     private static final TypeReference<List<Map<String, Object>>> POLYGON_POINTS_TYPE = new TypeReference<>() {
     };
-    private static final TypeReference<Map<String, Object>> OBJECT_MAP_TYPE = new TypeReference<>() {
-    };
     private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
     };
 
@@ -46,17 +45,16 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
     public BusinessEntityRecord saveBusinessEntity(BusinessEntityRecord businessEntity) {
         jdbcTemplate.update("""
                 INSERT INTO business_entities (
-                    id, entity_name, entity_role, parent_entity_id, unified_social_credit_code, legal_representative,
+                    id, entity_name, entity_role, unified_social_credit_code, legal_representative,
                     bank_account_ref, status, created_at, updated_at
                 )
                 VALUES (
-                    :id, :entity_name, :entity_role, :parent_entity_id, :unified_social_credit_code, :legal_representative,
+                    :id, :entity_name, :entity_role, :unified_social_credit_code, :legal_representative,
                     :bank_account_ref, :status, :created_at, :updated_at
                 )
                 ON CONFLICT (id) DO UPDATE SET
                     entity_name = EXCLUDED.entity_name,
                     entity_role = EXCLUDED.entity_role,
-                    parent_entity_id = EXCLUDED.parent_entity_id,
                     unified_social_credit_code = EXCLUDED.unified_social_credit_code,
                     legal_representative = EXCLUDED.legal_representative,
                     bank_account_ref = EXCLUDED.bank_account_ref,
@@ -69,7 +67,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
     @Override
     public List<BusinessEntityRecord> listBusinessEntities() {
         return jdbcTemplate.query("""
-                SELECT id, entity_name, entity_role, parent_entity_id, unified_social_credit_code, legal_representative,
+                SELECT id, entity_name, entity_role, unified_social_credit_code, legal_representative,
                        bank_account_ref, status, created_at, updated_at
                   FROM business_entities
                  ORDER BY created_at DESC, id
@@ -138,7 +136,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
 
     @Override
     public List<MerchantRecord> listMerchants(boolean includeArchived) {
-        // V260：默认列表隐藏已存档 + 已审批删除；includeArchived=true 时全量（后台「显示已存档」开关）。
+        // V260：默认列表隐藏已存档 + 已审批删除；M009-010：includeArchived=true 时全量（含已审批删除行，可查可恢复）。
         return jdbcTemplate.query("""
                 SELECT id, merchant_name, merchant_type, business_entity_id, contract_entity_id,
                        settlement_entity_id, contact_name, contact_phone, owner_person, mall_block_code, area_square_meter,
@@ -146,13 +144,26 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                        rent_per_square_meter, cooperation_form, status, created_at, updated_at,
                        archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason
                   FROM merchants
-                 WHERE (:include_archived OR archived_at IS NULL)
-                   AND delete_status <> 'approved'
+                 WHERE (:include_archived OR (archived_at IS NULL AND delete_status <> 'approved'))
                  ORDER BY created_at DESC, id
                  LIMIT 500
                 """, Map.of("include_archived", includeArchived), this::mapMerchant);
     }
 
+    @Override
+    public boolean businessEntityExists(String businessEntityId) {
+        Long count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM business_entities WHERE id = :id",
+                Map.of("id", businessEntityId), Long.class);
+        return count != null && count > 0;
+    }
+
+    @Override
+    public boolean projectExists(String projectId) {
+        Long count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM projects WHERE id = :id",
+                Map.of("id", projectId), Long.class);
+        return count != null && count > 0;
+    }
+
     @Override
     public java.util.Optional<MerchantRecord> findMerchantById(String merchantId) {
         return jdbcTemplate.query("""
@@ -365,7 +376,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
         Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM business_entities", params, Long.class);
         params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
         List<BusinessEntityRecord> items = jdbcTemplate.query("""
-                SELECT id, entity_name, entity_role, parent_entity_id, unified_social_credit_code, legal_representative,
+                SELECT id, entity_name, entity_role, unified_social_credit_code, legal_representative,
                        bank_account_ref, status, created_at, updated_at
                   FROM business_entities
                  ORDER BY created_at DESC, id
@@ -382,7 +393,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
         MapSqlParameterSource params = new MapSqlParameterSource().addValue("include_archived", includeArchived)
                 .addValue("business_format", businessFormat);
         // V348 按业态查询：business_format 为空＝不筛（走 idx_merchants_business_format）。
-        String where = " WHERE (:include_archived OR archived_at IS NULL) AND delete_status <> 'approved'"
+        String where = " WHERE (:include_archived OR (archived_at IS NULL AND delete_status <> 'approved'))"
                 + " AND (CAST(:business_format AS VARCHAR) IS NULL OR merchant_type = :business_format)";
         Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM merchants" + where, params, Long.class);
         params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
@@ -491,7 +502,6 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                 .addValue("id", businessEntity.id())
                 .addValue("entity_name", businessEntity.entityName())
                 .addValue("entity_role", businessEntity.entityRole())
-                .addValue("parent_entity_id", businessEntity.parentEntityId())
                 .addValue("unified_social_credit_code", businessEntity.unifiedSocialCreditCode())
                 .addValue("legal_representative", businessEntity.legalRepresentative())
                 .addValue("bank_account_ref", businessEntity.bankAccountRef())
@@ -580,7 +590,6 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                 resultSet.getString("id"),
                 resultSet.getString("entity_name"),
                 resultSet.getString("entity_role"),
-                resultSet.getString("parent_entity_id"),
                 resultSet.getString("unified_social_credit_code"),
                 resultSet.getString("legal_representative"),
                 resultSet.getString("bank_account_ref"),
@@ -1076,14 +1085,6 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
         }
     }
 
-    private Map<String, Object> readObjectMap(String value) {
-        try {
-            return value == null ? Map.of() : objectMapper.readValue(value, OBJECT_MAP_TYPE);
-        } catch (JsonProcessingException exception) {
-            throw new IllegalStateException("contract JSON object is not readable", exception);
-        }
-    }
-
     private List<String> readStringList(String value) {
         try {
             return value == null ? List.of() : objectMapper.readValue(value, STRING_LIST_TYPE);
diff --git a/backend/src/main/java/com/complex/platform/governance/MerchantDeleteDecision.java b/backend/src/main/java/com/complex/platform/governance/MerchantDeleteDecision.java
index 1e0cb4de..82d78234 100644
--- a/backend/src/main/java/com/complex/platform/governance/MerchantDeleteDecision.java
+++ b/backend/src/main/java/com/complex/platform/governance/MerchantDeleteDecision.java
@@ -1,7 +1,8 @@
 package com.complex.platform.governance;
+// 文件版本：V0.01
 
 /**
- * 商户删除审批结果。approved 时商户已物理删除（merchantId 仅供留痕），deleted=true；
+ * 商户删除审批结果。approved 时商户已按软删除标记（delete_status=approved，行保留、默认列表不显示、可查可恢复，BSM-004），deleted=true；
  * rejected 时商户回到 none 态、未删除。
  */
 public record MerchantDeleteDecision(String merchantId, String decision, boolean deleted) {
diff --git a/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java b/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java
index afbf6fb5..595fea23 100644
--- a/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java
+++ b/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java
@@ -1,4 +1,5 @@
 package com.complex.platform.governance;
+// 文件版本：V0.01
 
 import java.math.BigDecimal;
 import java.time.Instant;
@@ -68,6 +69,16 @@ public record MerchantRecord(
                 archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason);
     }
 
+    /** M009-008：三个主体列以生效的四类主体绑定为准（PGM-026），绑定激活时由 PlatformService 同步投影。 */
+    public MerchantRecord withEntities(String businessEntityId, String contractEntityId, String settlementEntityId,
+            Instant updatedAt) {
+        return new MerchantRecord(id, merchantName, merchantType, businessEntityId, contractEntityId,
+                settlementEntityId, contactName, contactPhone, ownerPerson, mallBlockCode, areaSquareMeter,
+                leasedAreaSquareMeter, secondaryContactName, secondaryContactPhone, rentPerSquareMeter, cooperationForm,
+                status, createdAt, updatedAt,
+                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason);
+    }
+
     public MerchantRecord withDelete(String deleteStatus, String deleteRequestedBy, Instant deleteRequestedAt,
             String deleteReason, Instant updatedAt) {
         return new MerchantRecord(id, merchantName, merchantType, businessEntityId, contractEntityId,
diff --git a/backend/src/main/java/com/complex/platform/governance/PlatformController.java b/backend/src/main/java/com/complex/platform/governance/PlatformController.java
index 93f260fc..567dc33c 100644
--- a/backend/src/main/java/com/complex/platform/governance/PlatformController.java
+++ b/backend/src/main/java/com/complex/platform/governance/PlatformController.java
@@ -1,4 +1,5 @@
 package com.complex.platform.governance;
+// 文件版本：V0.01
 import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;
 
 import com.complex.platform.access.AuthorizationService;
@@ -41,6 +42,8 @@ public class PlatformController {
             @org.springframework.web.bind.annotation.RequestParam(name = "page", defaultValue = "1") int page,
             @org.springframework.web.bind.annotation.RequestParam(name = "size", defaultValue = "500") int size) {
         authorizationService.requirePermission("entity.binding.view");
+        // M009-015：治理读端与写端同法 fail-closed（BSM-072/BSM-074/PGM-027：商户不能看平台全局和其他商户数据）。
+        authorizationService.requirePlatformScope();
         return ApiResponse.ok(service.listBusinessEntities(page, size));
     }
 
@@ -59,6 +62,7 @@ public class PlatformController {
             @org.springframework.web.bind.annotation.RequestParam(name = "page", defaultValue = "1") int page,
             @org.springframework.web.bind.annotation.RequestParam(name = "size", defaultValue = "500") int size) {
         authorizationService.requirePermission("merchant.profile.view");
+        authorizationService.requirePlatformScope();
         return ApiResponse.ok(maskPhonesForUnauthorized(service.listMerchants(includeArchived, merchantType, page, size)));
     }
 
@@ -122,6 +126,7 @@ public class PlatformController {
             @org.springframework.web.bind.annotation.RequestParam(name = "page", defaultValue = "1") int page,
             @org.springframework.web.bind.annotation.RequestParam(name = "size", defaultValue = "500") int size) {
         authorizationService.requirePermission("governance.project.view");
+        authorizationService.requirePlatformScope();
         return ApiResponse.ok(service.listProjects(page, size));
     }
 
@@ -137,12 +142,14 @@ public class PlatformController {
             @org.springframework.web.bind.annotation.RequestParam(name = "page", defaultValue = "1") int page,
             @org.springframework.web.bind.annotation.RequestParam(name = "size", defaultValue = "500") int size) {
         authorizationService.requirePermission("governance.space.view");
+        authorizationService.requirePlatformScope();
         return ApiResponse.ok(service.listSpaces(page, size));
     }
 
     @GetMapping("/space-types")
     public ApiResponse<PageResponse<SpaceTypeRecord>> listSpaceTypes() {
         authorizationService.requirePermission("governance.space.view");
+        authorizationService.requirePlatformScope();
         return ApiResponse.ok(service.listSpaceTypes());
     }
 
@@ -165,6 +172,7 @@ public class PlatformController {
     @GetMapping("/venue-business-hours")
     public ApiResponse<PageResponse<VenueBusinessHoursRecord>> listVenueBusinessHours() {
         authorizationService.requirePermission("governance.space.view");
+        authorizationService.requirePlatformScope();
         return ApiResponse.ok(PageResponse.of(venueBusinessHoursService.list(), 1, 500));
     }
 
@@ -182,6 +190,7 @@ public class PlatformController {
             @org.springframework.web.bind.annotation.RequestParam(name = "page", defaultValue = "1") int page,
             @org.springframework.web.bind.annotation.RequestParam(name = "size", defaultValue = "500") int size) {
         authorizationService.requirePermission("entity.binding.view");
+        authorizationService.requirePlatformScope();
         return ApiResponse.ok(service.listBusinessEntityBindings(page, size));
     }
 
diff --git a/backend/src/main/java/com/complex/platform/governance/PlatformRepository.java b/backend/src/main/java/com/complex/platform/governance/PlatformRepository.java
index f7eccbd8..6648d901 100644
--- a/backend/src/main/java/com/complex/platform/governance/PlatformRepository.java
+++ b/backend/src/main/java/com/complex/platform/governance/PlatformRepository.java
@@ -1,4 +1,5 @@
 package com.complex.platform.governance;
+// 文件版本：V0.01
 
 import java.util.List;
 import java.util.Optional;
@@ -19,7 +20,8 @@ public interface PlatformRepository {
     }
 
     /**
-     * V260：列出商户，includeArchived=false 时排除已存档 + 已审批删除（默认列表）。
+     * V260：列出商户，includeArchived=false 时排除已存档 + 已审批删除（默认列表）；
+     * M009-010：includeArchived=true 返回全量（含已审批删除行，供「已存档内容」查阅与恢复，BSM-004）。
      * 默认返回全量（内存 fake 用）；JdbcPlatformRepository 用 SQL 谓词过滤。
      */
     default List<MerchantRecord> listMerchants(boolean includeArchived) {
@@ -29,7 +31,7 @@ public interface PlatformRepository {
                 .toList();
     }
 
-    /** V260：物理删除商户（仅经删除审批通过后调用）。默认不支持，Jdbc/内存 fake 覆写。 */
+    /** V260/M009-010：软删除商户——标记 delete_status='approved'，行保留可查可恢复（BSM-004）；仅经删除审批通过或最高权限直接执行时调用。默认不支持，Jdbc/内存 fake 覆写。 */
     default void deleteMerchant(String merchantId) {
         throw new UnsupportedOperationException("deleteMerchant not supported by this repository");
     }
@@ -64,6 +66,16 @@ public interface PlatformRepository {
 
     List<SpaceRecord> listSpaces();
 
+    /** M009-006：主体是否存在（绑定引用校验）。默认基于 listBusinessEntities；Jdbc 覆写按主键。 */
+    default boolean businessEntityExists(String businessEntityId) {
+        return listBusinessEntities().stream().anyMatch(entity -> entity.id().equals(businessEntityId));
+    }
+
+    /** M009-007：项目是否存在（空间归属校验）。默认基于 listProjects；Jdbc 覆写按主键。 */
+    default boolean projectExists(String projectId) {
+        return listProjects().stream().anyMatch(project -> project.id().equals(projectId));
+    }
+
     /** 3.13：按 id 查空间（存在性校验用）。默认基于 listSpaces 过滤（空间量级小）；Jdbc 可按需覆写。 */
     default Optional<SpaceRecord> findSpaceById(String spaceId) {
         return listSpaces().stream().filter(space -> space.id().equals(spaceId)).findFirst();
diff --git a/backend/src/main/java/com/complex/platform/governance/PlatformService.java b/backend/src/main/java/com/complex/platform/governance/PlatformService.java
index 0de37ac2..c29d5c8a 100644
--- a/backend/src/main/java/com/complex/platform/governance/PlatformService.java
+++ b/backend/src/main/java/com/complex/platform/governance/PlatformService.java
@@ -1,4 +1,5 @@
 package com.complex.platform.governance;
+// 文件版本：V0.01
 
 import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
@@ -32,7 +33,6 @@ import static com.complex.platform.common.Strings.requireText;
 public class PlatformService {
 
     private static final int DEFAULT_PAGE = 1;
-    private static final int DEFAULT_PAGE_SIZE = 50;
     // AUD-005: 列表端点默认页大小(保留既有一次拉取上限, 供下拉/客户端分页); total 已取真值。
     private static final int LIST_MAX_PAGE_SIZE = 500;
 
@@ -45,38 +45,38 @@ public class PlatformService {
     public static final Set<String> MERCHANT_BUSINESS_FORMATS =
             Set.of("bar", "catering", "retail", "amusement", "media", "other");
 
+    /** M009-003：BEQR-148/BSM-028/PGM-026 合作方式六类（自营/租赁/联营/代运营/服务商接入/其他）的技术代码；空＝招商签约阶段尚未确定。 */
+    public static final Set<String> MERCHANT_COOPERATION_FORMS =
+            Set.of("self_operated", "lease", "joint_operation", "managed_operation", "service_provider", "other");
+
+    /** M009-004：BEQR-155 商户/运营主体可设状态（待准入/正常/暂停/终止/黑名单）；「归档」只经存档端点（archived_at 轴），不作可设代码。 */
+    public static final Set<String> SUBJECT_STATUSES =
+            Set.of("pending_admission", "active", "suspended", "terminated", "blacklisted");
+
+    /** M009-004：BEQR-154 综合体项目状态六态（草稿/待启用/已启用/暂停/终止/归档）。 */
+    public static final Set<String> PROJECT_STATUSES =
+            Set.of("draft", "pending_activation", "active", "suspended", "terminated", "archived");
+
+    /** M009-005：PGM-018/019 主体角色——顶层平台 / 城市项目公司 / 域主管公司（业态主体）。 */
+    public static final Set<String> BUSINESS_ENTITY_ROLES = Set.of("platform", "project_company", "management_company");
+
     private final PlatformRepository repository;
     private final AuditedOperationExecutor operations;
     private final AuditEventService auditEventService;
     // F-AUTHZ-02 治理写仅平台：治理写方法（建商户/法律实体绑定/合同/项目空间等）首行经
     // requirePlatformScope() fail-closed 拒 merchant-scoped 主体，防权限被下放给商户角色后越权治理。
     private final AuthorizationService authorizationService;
-    // AUD-076 撤场提前到期：商户存档=撤场；被存档商户为酒吧时其单店票已售权益提前到期（走方案2）。可为 null=兼容既有测试。
-    private final com.complex.platform.ticketing.TicketingService ticketingService;
 
-    @org.springframework.beans.factory.annotation.Autowired
     public PlatformService(
             PlatformRepository repository,
             AuditedOperationExecutor operations,
             AuditEventService auditEventService,
-            AuthorizationService authorizationService,
-            com.complex.platform.ticketing.TicketingService ticketingService
+            AuthorizationService authorizationService
     ) {
         this.repository = repository;
         this.operations = operations;
         this.auditEventService = auditEventService;
         this.authorizationService = authorizationService;
-        this.ticketingService = ticketingService;
-    }
-
-    // ponytail: 向后兼容构造——既有单测不接 ticketingService（不触发撤场提前到期）。
-    public PlatformService(
-            PlatformRepository repository,
-            AuditedOperationExecutor operations,
-            AuditEventService auditEventService,
-            AuthorizationService authorizationService
-    ) {
-        this(repository, operations, auditEventService, authorizationService, null);
     }
 
     public PageResponse<BusinessEntityRecord> listBusinessEntities() {
@@ -111,17 +111,17 @@ public class PlatformService {
                 "business_entity",
                 () -> {
                     String entityName = requireText(request == null ? null : request.entityName(), "entity_name");
-                    String entityRole = requireText(request == null ? null : request.entityRole(), "entity_role");
+                    String entityRole = requireBusinessEntityRole(request == null ? null : request.entityRole());
+                    String status = requireSubjectStatus(request.status());
                     Instant now = Instant.now();
                     return repository.saveBusinessEntity(new BusinessEntityRecord(
                             "be_" + UUID.randomUUID(),
                             entityName,
                             entityRole,
-                            trimToNull(request.parentEntityId()),
                             trimToNull(request.unifiedSocialCreditCode()),
                             trimToNull(request.legalRepresentative()),
                             trimToNull(request.bankAccountRef()),
-                            trimToDefault(request.status(), "active"),
+                            status,
                             now,
                             now
                     ));
@@ -159,16 +159,19 @@ public class PlatformService {
         return saveMerchant(
                 "createMerchant",
                 "mch_" + UUID.randomUUID(),
-                request
+                request,
+                false
         );
     }
 
+    /** M009-001/002：更新只覆盖档案字段；商户须已存在（404），存档/删除申请状态与 createdAt 原样保留。 */
     public MerchantRecord updateMerchant(String merchantId, MerchantProfileRequest request) {
         authorizationService.requirePlatformScope();
         return saveMerchant(
                 "updateMerchant:" + merchantId,
                 requireText(merchantId, "merchant_id"),
-                request
+                request,
+                true
         );
     }
 
@@ -203,13 +206,9 @@ public class PlatformService {
                     String r = requireText(request == null ? null : request.reason(), "reason");
                     MerchantRecord merchant = requireMerchant(id);
                     Instant now = Instant.now();
+                    // M009-011：存档＝可逆软下线（BSM-004），不是商户退出；退出缓冲期与停票不在此触发（BEQR-155）。
                     MerchantRecord saved = repository.saveMerchant(merchant.withArchive(now, actorId(), now));
                     repository.setMerchantArchiveReason(id, r);
-                    // AUD-076 撤场：被存档商户为酒吧时，其单店票停售（下单侧 TICKET_BAR_OFFLINE）+
-                    // 已售权益提前到该酒吧截止营业时刻到期（走方案2：可退未用自动退/不可退作废）。
-                    if (ticketingService != null && "bar".equals(merchant.merchantType())) {
-                        ticketingService.expireBarSingleStoreTickets(id, now);
-                    }
                     return saved;
                 }
         );
@@ -264,14 +263,27 @@ public class PlatformService {
                     }
                     Instant now = Instant.now();
                     String reason = requireText(request == null ? null : request.reason(), "reason");
-                    return repository.saveMerchant(merchant.withDelete("pending", actorId(), now, reason, now));
+                    String actor = actorId();
+                    // M009-009：BSM-004「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批」——直接按软删执行，仍查占用并留痕。
+                    if (isMasterAuthority()) {
+                        long refs = repository.countMerchantReferences(id);
+                        if (refs > 0) {
+                            throw ApiException.conflict("MERCHANT_IN_USE",
+                                    "商户仍被 " + refs + " 条业务记录引用（合同/账单/工单/员工/酒吧等），不可删除");
+                        }
+                        MerchantRecord deleted = merchant.withDelete("approved", actor, now, reason, now);
+                        repository.saveMerchant(deleted);
+                        repository.deleteMerchant(id);
+                        return deleted;
+                    }
+                    return repository.saveMerchant(merchant.withDelete("pending", actor, now, reason, now));
                 }
         );
     }
 
     /**
-     * 审批删除申请。approved 且无活引用 → 物理删除；rejected → 回 none。
-     * 禁自审：审批人不得为删除申请发起人（照 supervisor_review 职责分离原则）。
+     * 审批删除申请。approved 且无活引用 → 软删除标记（delete_status=approved，行保留、默认列表不显示，BSM-004）；rejected → 回 none。
+     * M009-009：审批人须为最高权限（持授权 U 盘密钥者，BSM-004/BSM-007）；禁自审：审批人不得为删除申请发起人。
      */
     public MerchantDeleteDecision approveDeleteMerchant(String merchantId, MerchantDeleteApprovalRequest request) {
         authorizationService.requirePlatformScope();
@@ -296,6 +308,9 @@ public class PlatformService {
                     if (!"approved".equals(decision) && !"rejected".equals(decision)) {
                         throw ApiException.badRequest("VALIDATION_FAILED", "审批结论必须为通过（approved）或驳回（rejected）");
                     }
+                    if (!isMasterAuthority()) {
+                        throw ApiException.forbidden("MERCHANT_DELETE_APPROVER_NOT_MASTER", "商户删除只能由最高权限审批通过");
+                    }
                     // 职责分离：删除申请发起人不能审批自己的申请。actorId 为 null（无鉴权/测试）时跳过该闸。
                     String actor = actorId();
                     if (actor != null && actor.equals(merchant.deleteRequestedBy())) {
@@ -307,7 +322,7 @@ public class PlatformService {
                         repository.saveMerchant(merchant.withDelete("none", null, null, null, now));
                         return new MerchantDeleteDecision(id, "rejected", false);
                     }
-                    // approved：先校验商户无核心运营表活引用，有则拒绝硬删（防连锁孤儿）。
+                    // approved：先校验商户无核心运营表活引用，有则拒绝删除（防连锁孤儿）。
                     long refs = repository.countMerchantReferences(id);
                     if (refs > 0) {
                         throw ApiException.conflict("MERCHANT_IN_USE",
@@ -356,16 +371,21 @@ public class PlatformService {
                 () -> {
                     String name = requireText(request == null ? null : request.name(), "name");
                     String venueType = requireText(request == null ? null : request.venueType(), "venue_type");
+                    String merchantId = trimToNull(request.merchantId());
+                    if (merchantId != null) {
+                        requireMerchant(merchantId);
+                    }
+                    String status = requireProjectStatus(request.status());
                     Instant now = Instant.now();
                     return repository.saveProject(new ProjectRecord(
                             "prj_" + UUID.randomUUID(),
-                            trimToNull(request.merchantId()),
+                            merchantId,
                             venueType,
                             name,
                             trimToNull(request.code()),
                             trimToNull(request.floor()),
                             request.capacity(),
-                            trimToDefault(request.status(), "active"),
+                            status,
                             now,
                             now
                     ));
@@ -401,7 +421,7 @@ public class PlatformService {
         auditEventService.recordSuccess(
                 "space_type.viewed", "governance.space.view", "space_type", "all", null, null,
                 Map.of("count", records.size()));
-        return page(records);
+        return PageResponse.of(records, DEFAULT_PAGE, LIST_MAX_PAGE_SIZE);
     }
 
     public SpaceTypeRecord createSpaceType(com.complex.platform.governance.dto.SpaceTypeRequest request) {
@@ -445,12 +465,24 @@ public class PlatformService {
                         throw com.complex.platform.common.api.ApiException.badRequest("VALIDATION_FAILED",
                                 "铺位类型必须是铺位类型字典中的启用值");
                     }
+                    String projectId = trimToNull(request.projectId());
+                    if (projectId != null && !repository.projectExists(projectId)) {
+                        throw ApiException.notFound("PROJECT_NOT_FOUND", "项目不存在");
+                    }
+                    String merchantId = trimToNull(request.merchantId());
+                    if (merchantId != null) {
+                        requireMerchant(merchantId);
+                    }
+                    String parentSpaceId = trimToNull(request.parentSpaceId());
+                    if (parentSpaceId != null && repository.findSpaceById(parentSpaceId).isEmpty()) {
+                        throw ApiException.notFound("SPACE_NOT_FOUND", "上级空间不存在");
+                    }
                     Instant now = Instant.now();
                     return repository.saveSpace(new SpaceRecord(
                             "sp_" + UUID.randomUUID(),
-                            trimToNull(request.projectId()),
-                            trimToNull(request.merchantId()),
-                            trimToNull(request.parentSpaceId()),
+                            projectId,
+                            merchantId,
+                            parentSpaceId,
                             spaceName,
                             spaceType,
                             trimToNull(request.floorNo()),
@@ -510,6 +542,11 @@ public class PlatformService {
                 () -> {
                     BusinessEntityBindingRequest actual = requireRequest(request);
                     String merchantId = requireText(actual.merchantId(), "merchant_id");
+                    // M009-006：四类主体绑定的经营对象与三个主体都必须真实存在（BEQR-143/PGM-026）。
+                    requireMerchant(merchantId);
+                    String operationEntityId = requireBusinessEntity(actual.operationEntityId(), "operation_entity_id");
+                    String contractEntityId = requireBusinessEntity(actual.contractEntityId(), "contract_entity_id");
+                    String settlementEntityId = requireBusinessEntity(actual.settlementEntityId(), "settlement_entity_id");
                     repository.findActiveBusinessEntityBindingByMerchantId(merchantId)
                             .ifPresent(existing -> {
                                 throw ApiException.conflict(
@@ -517,12 +554,12 @@ public class PlatformService {
                                         "该商户已存在生效中的主体绑定，不能重复绑定");
                             });
                     Instant now = Instant.now();
-                    return repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(
+                    BusinessEntityBindingRecord saved = repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(
                             UUID.randomUUID().toString(),
                             merchantId,
-                            requireText(actual.operationEntityId(), "operation_entity_id"),
-                            requireText(actual.contractEntityId(), "contract_entity_id"),
-                            requireText(actual.settlementEntityId(), "settlement_entity_id"),
+                            operationEntityId,
+                            contractEntityId,
+                            settlementEntityId,
                             "active",
                             actual.effectiveFrom() == null ? LocalDate.now(ZoneOffset.UTC) : actual.effectiveFrom(),
                             null,
@@ -530,6 +567,8 @@ public class PlatformService {
                             now,
                             now
                     ));
+                    syncMerchantEntities(saved);
+                    return saved;
                 }
         );
     }
@@ -565,7 +604,7 @@ public class PlatformService {
                                 });
                     }
                     Instant now = Instant.now();
-                    return repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(
+                    BusinessEntityBindingRecord saved = repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(
                             existing.id(),
                             existing.merchantId(),
                             existing.operationEntityId(),
@@ -578,6 +617,10 @@ public class PlatformService {
                             existing.createdAt(),
                             now
                     ));
+                    if ("active".equals(status)) {
+                        syncMerchantEntities(saved);
+                    }
+                    return saved;
                 }
         );
     }
@@ -596,10 +639,82 @@ public class PlatformService {
         return value;
     }
 
+    /** M009-003：合作方式非空时须为六类之一；空＝尚未确定（招商签约阶段）。 */
+    private static String requireCooperationForm(String raw) {
+        String value = trimToNull(raw);
+        if (value != null && !MERCHANT_COOPERATION_FORMS.contains(value)) {
+            throw ApiException.badRequest("MERCHANT_COOPERATION_FORM_INVALID",
+                    "合作方式必须为 自营/租赁/联营/代运营/服务商接入/其他 之一"
+                            + "(self_operated/lease/joint_operation/managed_operation/service_provider/other)");
+        }
+        return value;
+    }
+
+    /** M009-004：商户/运营主体状态白名单（BEQR-155）；缺省正常（active）。 */
+    private String requireSubjectStatus(String raw) {
+        String value = trimToDefault(raw, "active");
+        if (!SUBJECT_STATUSES.contains(value)) {
+            throw ApiException.badRequest("VALIDATION_FAILED",
+                    "状态必须为 待准入/正常/暂停/终止/黑名单 之一"
+                            + "(pending_admission/active/suspended/terminated/blacklisted)；归档请使用存档操作");
+        }
+        return value;
+    }
+
+    /** M009-004：综合体项目状态白名单（BEQR-154）；缺省已启用（active）。 */
+    private String requireProjectStatus(String raw) {
+        String value = trimToDefault(raw, "active");
+        if (!PROJECT_STATUSES.contains(value)) {
+            throw ApiException.badRequest("VALIDATION_FAILED",
+                    "项目状态必须为 草稿/待启用/已启用/暂停/终止/归档 之一"
+                            + "(draft/pending_activation/active/suspended/terminated/archived)");
+        }
+        return value;
+    }
+
+    /** M009-005：主体角色白名单（PGM-018/019）；非法值在服务层以中文 400 拒绝，不落到数据库 CHECK。 */
+    private static String requireBusinessEntityRole(String raw) {
+        String value = requireText(raw, "entity_role");
+        if (!BUSINESS_ENTITY_ROLES.contains(value)) {
+            throw ApiException.badRequest("VALIDATION_FAILED",
+                    "主体角色必须为 顶层平台/城市项目公司/域主管公司 之一(platform/project_company/management_company)");
+        }
+        return value;
+    }
+
+    /** M009-006：绑定引用的主体必须存在。 */
+    private String requireBusinessEntity(String raw, String fieldName) {
+        String id = requireText(raw, fieldName);
+        if (!repository.businessEntityExists(id)) {
+            throw ApiException.notFound("BUSINESS_ENTITY_NOT_FOUND", "主体不存在：" + id);
+        }
+        return id;
+    }
+
+    /** M009-008：PGM-026「合同、结算、权限、看板、第三方接入都引用同一组绑定」——生效绑定是商户三个主体列的唯一来源，激活时同步投影。 */
+    private void syncMerchantEntities(BusinessEntityBindingRecord binding) {
+        repository.findMerchantById(binding.merchantId()).ifPresent(merchant -> repository.saveMerchant(
+                merchant.withEntities(binding.operationEntityId(), binding.contractEntityId(),
+                        binding.settlementEntityId(), Instant.now())));
+    }
+
+    /** M009-009：当前操作者是否为持授权 U 盘密钥的最高权限；无鉴权上下文回退 false。 */
+    private boolean isMasterAuthority() {
+        if (authorizationService == null) {
+            return false;
+        }
+        try {
+            return authorizationService.isMasterAuthority(authorizationService.currentActor());
+        } catch (RuntimeException unauthenticated) {
+            return false;
+        }
+    }
+
     private MerchantRecord saveMerchant(
             String operationId,
             String merchantId,
-            MerchantProfileRequest request
+            MerchantProfileRequest request,
+            boolean update
     ) {
         return executeWrite(
                 operationId,
@@ -613,14 +728,19 @@ public class PlatformService {
                 () -> {
                     String merchantName = requireText(request == null ? null : request.merchantName(), "merchant_name");
                     String merchantType = requireBusinessFormat(request == null ? null : request.merchantType());
+                    String cooperationForm = requireCooperationForm(request.cooperationForm());
+                    String status = requireSubjectStatus(request.status());
+                    // M009-001/002：更新须命中已有商户，并保留存档/删除申请状态与 createdAt；
+                    // M009-008：三个主体列以生效绑定为准，档案更新不覆盖。
+                    MerchantRecord existing = update ? requireMerchant(merchantId) : null;
                     Instant now = Instant.now();
                     return repository.saveMerchant(new MerchantRecord(
                             merchantId,
                             merchantName,
                             merchantType,
-                            trimToNull(request.businessEntityId()),
-                            trimToNull(request.contractEntityId()),
-                            trimToNull(request.settlementEntityId()),
+                            existing == null ? trimToNull(request.businessEntityId()) : existing.businessEntityId(),
+                            existing == null ? trimToNull(request.contractEntityId()) : existing.contractEntityId(),
+                            existing == null ? trimToNull(request.settlementEntityId()) : existing.settlementEntityId(),
                             trimToNull(request.contactName()),
                             trimToNull(request.contactPhone()),
                             trimToNull(request.ownerPerson()),
@@ -630,10 +750,16 @@ public class PlatformService {
                             trimToNull(request.secondaryContactName()),
                             trimToNull(request.secondaryContactPhone()),
                             request.rentPerSquareMeter(),
-                            trimToNull(request.cooperationForm()),
-                            trimToDefault(request.status(), "active"),
+                            cooperationForm,
+                            status,
+                            existing == null ? now : existing.createdAt(),
                             now,
-                            now
+                            existing == null ? null : existing.archivedAt(),
+                            existing == null ? null : existing.archivedBy(),
+                            existing == null ? "none" : existing.deleteStatus(),
+                            existing == null ? null : existing.deleteRequestedBy(),
+                            existing == null ? null : existing.deleteRequestedAt(),
+                            existing == null ? null : existing.deleteReason()
                     ));
                 }
         );
@@ -662,10 +788,6 @@ public class PlatformService {
         );
     }
 
-    private <T> PageResponse<T> page(List<T> items) {
-        return new PageResponse<>(items, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, items.size());
-    }
-
     private String trimToDefault(String value, String defaultValue) {
         String trimmed = value == null ? "" : value.trim();
         return trimmed.isEmpty() ? defaultValue : trimmed;
@@ -678,13 +800,6 @@ public class PlatformService {
         return request;
     }
 
-    private LocalDate requireDate(LocalDate value, String fieldName) {
-        if (value == null) {
-            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
-        }
-        return value;
-    }
-
     private String idempotencyKey(BusinessEntityRequest request) {
         return request == null ? null : request.idempotencyKey();
     }
diff --git a/backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java b/backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java
index c7d2af05..fc6fa19f 100644
--- a/backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java
+++ b/backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java
@@ -1,4 +1,5 @@
 package com.complex.platform.governance.dto;
+// 文件版本：V0.01
 
 import java.util.List;
 
@@ -8,7 +9,6 @@ public record BusinessEntityRequest(
         List<String> evidenceAttachmentIds,
         String entityName,
         String entityRole,
-        String parentEntityId,
         String unifiedSocialCreditCode,
         String legalRepresentative,
         String bankAccountRef,
diff --git a/backend/src/main/java/com/complex/platform/governance/dto/MerchantArchiveRequest.java b/backend/src/main/java/com/complex/platform/governance/dto/MerchantArchiveRequest.java
index b36d86eb..4e533295 100644
--- a/backend/src/main/java/com/complex/platform/governance/dto/MerchantArchiveRequest.java
+++ b/backend/src/main/java/com/complex/platform/governance/dto/MerchantArchiveRequest.java
@@ -1,5 +1,6 @@
 package com.complex.platform.governance.dto;
+// 文件版本：V0.01
 
-/** 商户存档/取消存档请求。仅需幂等键（存档动作无额外入参）。 */
+/** 商户存档/取消存档请求。idempotencyKey 必填；reason 在存档时必填（BSM-004「存档及删除都需要填写理由」），取消存档可空。 */
 public record MerchantArchiveRequest(String idempotencyKey, String reason) {
 }
diff --git a/backend/src/main/java/com/complex/platform/leasing/LeasingService.java b/backend/src/main/java/com/complex/platform/leasing/LeasingService.java
index 325b9f70..eb6b15af 100644
--- a/backend/src/main/java/com/complex/platform/leasing/LeasingService.java
+++ b/backend/src/main/java/com/complex/platform/leasing/LeasingService.java
@@ -1,4 +1,5 @@
 package com.complex.platform.leasing;
+// 文件版本：V0.01
 
 import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
@@ -287,7 +288,8 @@ public class LeasingService {
                 contractSubjectId, contractSubjectId, contractSubjectId,
                 contactName, contactPhone, contactName,
                 spaceCode, area, area,
-                null, null, null, "leasing_auto", "active"));
+                // M009-003：合作方式只能是 HTML 六类之一，签约阶段未定＝空，不发明「leasing_auto」值。
+                null, null, null, null, "active"));
         return merchant.id();
     }
 
diff --git a/backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java b/backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java
index a2ef8f25..77301814 100644
--- a/backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java
@@ -1,4 +1,5 @@
 package com.complex.platform.governance;
+// 文件版本：V0.01
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.mockito.ArgumentMatchers.any;
@@ -237,6 +238,20 @@ class PlatformControllerTests {
                 .andExpect(jsonPath("$.data.items[0].merchant_id").value("mch_1"));
 
         verify(authorizationService).requirePermission("entity.binding.view");
+        verify(authorizationService).requirePlatformScope(); // M009-015：读端与写端同法 fail-closed
+    }
+
+    // M009-015：治理读端必须带平台作用域守卫（BSM-072/BSM-074/PGM-027），不能只靠权限码。
+    @Test
+    void listingMerchantsRequiresPlatformScope() throws Exception {
+        doThrow(ApiException.forbidden("PLATFORM_SCOPE_REQUIRED", "商户账号无权查看平台级经营分析"))
+                .when(authorizationService)
+                .requirePlatformScope();
+
+        mockMvc.perform(get("/api/v1/platform/merchants")
+                        .with(user("ops").roles("P0_SMOKE")))
+                .andExpect(status().isForbidden())
+                .andExpect(jsonPath("$.detail_code").value("PLATFORM_SCOPE_REQUIRED"));
     }
 
     @Test
diff --git a/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
index d39ad848..f81d5c6d 100644
--- a/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
@@ -1,4 +1,5 @@
 package com.complex.platform.governance;
+// 文件版本：V0.01
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
@@ -62,7 +63,7 @@ class PlatformJdbcIntegrationTests {
         // AUD-005 真分页: SQL COUNT + LIMIT/OFFSET(含 include_archived + delete_status 谓词), 第二页可达首页之后的商户。
         Instant t = Instant.now();
         repository.saveBusinessEntity(new BusinessEntityRecord(
-                "be_pg", "PG Entity", "operation", null, null, null, null, "active", t, t));
+                "be_pg", "PG Entity", "operation", null, null, null, "active", t, t)); // M009-012：主体记录不再带 parent 链
         repository.saveMerchant(new MerchantRecord("mch_pg_1", "PG A", "other", "be_pg", "be_pg", "be_pg",
                 "C1", "18800000001", "Owner", "1F-A", new java.math.BigDecimal("10.00"), new java.math.BigDecimal("9.00"),
                 null, null, new java.math.BigDecimal("1.00"), "fixed_rent_plus_commission", "draft", t, t));
@@ -91,7 +92,6 @@ class PlatformJdbcIntegrationTests {
                 "be_jdbc_formal",
                 "  P0 Platform Entity  ",
                 "operation",
-                null,
                 "91310000MA1P0",
                 "Jane Ops",
                 "vault://bank/entity",
@@ -339,6 +339,24 @@ class PlatformJdbcIntegrationTests {
     }
 
 
+    // M009-010：软删除行保留可查（includeArchived=true 含已审批删除行），默认列表不显示（BSM-004）。
+    @Test
+    void approvedDeletedMerchantStaysQueryableWithIncludeArchived() {
+        Instant t = Instant.now();
+        repository.saveMerchant(new MerchantRecord("mch_soft_del", "软删商户", "other", null, null, null,
+                null, null, null, null, null, null, null, null, null, null, "active", t, t));
+        repository.deleteMerchant("mch_soft_del");
+
+        assertThat(repository.findMerchantById("mch_soft_del")).isPresent()
+                .get().extracting(MerchantRecord::deleteStatus).isEqualTo("approved");
+        assertThat(repository.listMerchants(true)).extracting(MerchantRecord::id).contains("mch_soft_del");
+        assertThat(repository.listMerchantsPaged(true, null, 1, 500).items()).extracting(MerchantRecord::id).contains("mch_soft_del");
+        assertThat(repository.listMerchants(false)).extracting(MerchantRecord::id).doesNotContain("mch_soft_del");
+        assertThat(repository.listMerchantsPaged(false, null, 1, 500).items()).extracting(MerchantRecord::id).doesNotContain("mch_soft_del");
+        // M009-006：主体存在性查询按主键。
+        assertThat(repository.businessEntityExists("be_definitely_missing")).isFalse();
+    }
+
     private String regclass(String tableName) {
         return jdbcTemplate.queryForObject("SELECT to_regclass('public." + tableName + "')::text", String.class);
     }
@@ -356,7 +374,7 @@ class PlatformJdbcIntegrationTests {
     void merchantContactPhonesArePersistedAsCiphertextAndReadBackAsPlaintext() {
         Instant t = Instant.now();
         repository.saveBusinessEntity(new BusinessEntityRecord(
-                "be_pii", "PII Entity", "operation", null, null, null, null, "active", t, t));
+                "be_pii", "PII Entity", "operation", null, null, null, "active", t, t));
         repository.saveMerchant(new MerchantRecord("mch_pii_1", "PII 商户", "other", "be_pii", "be_pii", "be_pii",
                 "第一联系人", "18800000021", "负责人", "1F-P", new BigDecimal("10.00"), new BigDecimal("9.00"),
                 "第二联系人", "18800000022", new BigDecimal("1.00"), "fixed_rent_plus_commission", "draft", t, t));
@@ -378,7 +396,7 @@ class PlatformJdbcIntegrationTests {
         // 防二次加密：把读回来的（明文）记录原样再存一次，落库仍是可解密的单层密文。
         Instant t = Instant.now();
         repository.saveBusinessEntity(new BusinessEntityRecord(
-                "be_pii2", "PII Entity 2", "operation", null, null, null, null, "active", t, t));
+                "be_pii2", "PII Entity 2", "operation", null, null, null, "active", t, t));
         repository.saveMerchant(new MerchantRecord("mch_pii_2", "PII 商户2", "other", "be_pii2", "be_pii2", "be_pii2",
                 "联系人", "18800000023", "负责人", "1F-Q", new BigDecimal("10.00"), new BigDecimal("9.00"),
                 null, null, new BigDecimal("1.00"), "fixed_rent_plus_commission", "draft", t, t));
@@ -392,7 +410,7 @@ class PlatformJdbcIntegrationTests {
         // DB 级边界（V249 思路）：任何绕开仓库的手写 SQL / 批量导入写明文，数据库直接拒。
         Instant t = Instant.now();
         repository.saveBusinessEntity(new BusinessEntityRecord(
-                "be_pii3", "PII Entity 3", "operation", null, null, null, null, "active", t, t));
+                "be_pii3", "PII Entity 3", "operation", null, null, null, "active", t, t));
         assertThatThrownBy(() -> jdbcTemplate.update(
                 "INSERT INTO merchants (id, merchant_name, merchant_type, contact_phone, status, created_at, updated_at) "
                         + "VALUES ('mch_pii_plain', 'X', 'other', '18800000024', 'draft', now(), now())"))
diff --git a/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java b/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java
index ef317152..4678ae92 100644
--- a/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java
@@ -1,5 +1,8 @@
 package com.complex.platform.governance;
+// 文件版本：V0.01
 
+import com.complex.platform.access.AccessActor;
+import com.complex.platform.auth.SessionIssuer;
 import com.complex.platform.support.InMemoryAuditEventRepository;
 import com.complex.platform.support.InMemoryIdempotencyRepository;
 import static org.assertj.core.api.Assertions.assertThat;
@@ -25,6 +28,7 @@ import com.complex.platform.governance.dto.StatusChangeRequest;
 import com.complex.platform.support.TestSecurityActors;
 import com.fasterxml.jackson.databind.ObjectMapper;
 import java.math.BigDecimal;
+import java.time.Instant;
 import java.time.LocalDate;
 import java.util.ArrayList;
 import java.util.LinkedHashMap;
@@ -34,6 +38,8 @@ import java.util.Optional;
 import org.junit.jupiter.api.AfterEach;
 import org.junit.jupiter.api.BeforeEach;
 import org.junit.jupiter.api.Test;
+import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
+import org.springframework.security.core.context.SecurityContextHolder;
 
 class PlatformServiceTests {
 
@@ -104,6 +110,183 @@ class PlatformServiceTests {
                 null, null, null, null, null, null, null, null, "active"));
     }
 
+    /** M009-006：绑定引用的主体须真实存在——直接落一条主体行（不经服务，避免干扰幂等计数断言）。 */
+    private void seedEntity(String id) {
+        platformRepository.saveBusinessEntity(new BusinessEntityRecord(
+                id, id, "management_company", null, null, null, "active", Instant.EPOCH, Instant.EPOCH));
+    }
+
+    /** M009-009：最高权限（持授权 U 盘密钥者）身份——isMasterAuthority 要求 platform_admin + 固定 id/用户名（BSM-004/007）。 */
+    private static void asMasterAuthority() {
+        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
+                "platform_admin", "org_1", null, "active");
+        SecurityContextHolder.getContext().setAuthentication(
+                new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
+    }
+
+    // ---- M009-001/002：更新商户不得擦除存档/删除申请状态；更新不存在的商户须 404 ----
+    @Test
+    void updateMerchantKeepsArchiveAndPendingDeleteState() {
+        MerchantRecord merchant = seedMerchant("seed-keep-state");
+        service.archiveMerchant(merchant.id(), new MerchantArchiveRequest("keep-archive", "测试存档理由"));
+        service.requestDeleteMerchant(merchant.id(), new MerchantDeleteRequest("keep-delete-req", "清理"));
+
+        MerchantRecord updated = service.updateMerchant(merchant.id(), new MerchantProfileRequest(
+                "keep-update", "rename", List.of(), "改名后的商户", "other", "be_other",
+                "be_contract_x", "be_settlement_x", "Jane", "18800000011",
+                null, null, null, null, null, null, null, null, "active"));
+
+        assertThat(updated.merchantName()).isEqualTo("改名后的商户");
+        assertThat(updated.archivedAt()).isNotNull();
+        assertThat(updated.deleteStatus()).isEqualTo("pending");
+        assertThat(updated.deleteRequestedBy()).isEqualTo("usr_admin");
+        assertThat(updated.createdAt()).isEqualTo(merchant.createdAt());
+        // M009-008：三个主体列以生效绑定为准，档案更新不覆盖。
+        assertThat(updated.businessEntityId()).isEqualTo("be_1");
+        assertThat(updated.contractEntityId()).isNull();
+        assertThat(service.listMerchants(false).items()).noneMatch(m -> m.id().equals(merchant.id()));
+    }
+
+    @Test
+    void updateMerchantRejectsUnknownMerchant() {
+        assertThatThrownBy(() -> service.updateMerchant("mch_missing", new MerchantProfileRequest(
+                "update-missing", "update", List.of(), "幽灵商户", "other", null,
+                null, null, null, null, null, null, null, null, null, null, null, null, "active")))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("MERCHANT_NOT_FOUND"));
+        assertThat(platformRepository.listMerchants()).noneMatch(m -> m.id().equals("mch_missing"));
+    }
+
+    // ---- M009-003：合作方式六类（BEQR-148）；空＝未定 ----
+    @Test
+    void createMerchantRejectsCooperationFormOutsideSixModes() {
+        assertThatThrownBy(() -> service.createMerchant(new MerchantProfileRequest(
+                "coop-bad", "seed", List.of(), "合作方式错的商户", "other", null,
+                null, null, null, null, null, null, null, null, null, null, null, "fixed_rent_plus_commission", "active")))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("MERCHANT_COOPERATION_FORM_INVALID"));
+        MerchantRecord lease = service.createMerchant(new MerchantProfileRequest(
+                "coop-lease", "seed", List.of(), "租赁商户", "other", null,
+                null, null, null, null, null, null, null, null, null, null, null, "lease", "active"));
+        assertThat(lease.cooperationForm()).isEqualTo("lease");
+        assertThat(seedMerchant("coop-null").cooperationForm()).isNull();
+    }
+
+    // ---- M009-004：商户/主体/项目状态只认速查表六态（BEQR-154/155） ----
+    @Test
+    void createMerchantRejectsStatusOutsideEnumeratedStates() {
+        assertThatThrownBy(() -> service.createMerchant(new MerchantProfileRequest(
+                "status-bad", "seed", List.of(), "状态错的商户", "other", null,
+                null, null, null, null, null, null, null, null, null, null, null, null, "draft")))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));
+        assertThat(service.createMerchant(new MerchantProfileRequest(
+                "status-pending", "seed", List.of(), "待准入商户", "other", null,
+                null, null, null, null, null, null, null, null, null, null, null, null, "pending_admission")).status())
+                .isEqualTo("pending_admission");
+    }
+
+    @Test
+    void createProjectRejectsUnknownStatusOrMerchant() {
+        assertThatThrownBy(() -> service.createProject(new ProjectRequest(
+                "project-bad-status", "create", List.of(), null, "bar", "P0 Bar", null, null, null, "zzz")))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));
+        // M009-007：merchantId 非空时必须存在。
+        assertThatThrownBy(() -> service.createProject(new ProjectRequest(
+                "project-bad-merchant", "create", List.of(), "mch_missing", "bar", "P0 Bar", null, null, null, "draft")))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("MERCHANT_NOT_FOUND"));
+    }
+
+    // ---- M009-005：主体角色白名单（PGM-018/019） ----
+    @Test
+    void createBusinessEntityRejectsRoleOutsideWhitelist() {
+        assertThatThrownBy(() -> service.createBusinessEntity(new BusinessEntityRequest(
+                "entity-spv", "create", List.of(), "SPV 主体", "spv", null, null, null, "active")))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));
+    }
+
+    // ---- M009-006：绑定的经营对象与三个主体须存在 ----
+    @Test
+    void createBusinessEntityBindingRejectsMissingMerchantOrEntity() {
+        assertThatThrownBy(() -> service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
+                "binding-ghost-merchant", "ghost", List.of(), "mch_missing", "be_x", "be_x", "be_x", null, null)))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("MERCHANT_NOT_FOUND"));
+        MerchantRecord merchant = seedMerchant("seed-ghost-entity");
+        assertThatThrownBy(() -> service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
+                "binding-ghost-entity", "ghost", List.of(), merchant.id(), "be_missing", "be_missing", "be_missing", null, null)))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("BUSINESS_ENTITY_NOT_FOUND"));
+        assertThat(platformRepository.listBusinessEntityBindings()).isEmpty();
+    }
+
+    // ---- M009-007：空间归属链须存在 ----
+    @Test
+    void createSpaceRejectsUnknownProjectMerchantOrParent() {
+        assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
+                "sp-bad-prj", "create", List.of(), "prj_missing", null, null, "A", "shop", null, null, null, "active")))
+                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("PROJECT_NOT_FOUND"));
+        assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
+                "sp-bad-mch", "create", List.of(), null, "mch_missing", null, "A", "shop", null, null, null, "active")))
+                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("MERCHANT_NOT_FOUND"));
+        assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
+                "sp-bad-parent", "create", List.of(), null, null, "sp_missing", "A", "shop", null, null, null, "active")))
+                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("SPACE_NOT_FOUND"));
+    }
+
+    // ---- M009-008：生效绑定同步商户三个主体列（PGM-026「引用同一组绑定」） ----
+    @Test
+    void createBusinessEntityBindingSyncsMerchantEntityColumns() {
+        MerchantRecord merchant = seedMerchant("seed-sync");
+        seedEntity("be_op_sync");
+        seedEntity("be_ct_sync");
+        seedEntity("be_st_sync");
+        service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
+                "binding-sync", "sync", List.of(), merchant.id(), "be_op_sync", "be_ct_sync", "be_st_sync", null, null));
+        MerchantRecord synced = platformRepository.findMerchantById(merchant.id()).orElseThrow();
+        assertThat(synced.businessEntityId()).isEqualTo("be_op_sync");
+        assertThat(synced.contractEntityId()).isEqualTo("be_ct_sync");
+        assertThat(synced.settlementEntityId()).isEqualTo("be_st_sync");
+    }
+
+    // ---- M009-009：删除审批只认最高权限；最高权限本人发起免审批直接执行（BSM-004/007） ----
+    @Test
+    void approveDeleteByNonMasterIsForbidden() {
+        MerchantRecord merchant = seedMerchant("seed-nonmaster");
+        service.archiveMerchant(merchant.id(), new MerchantArchiveRequest("archive-nm", "测试存档理由"));
+        service.requestDeleteMerchant(merchant.id(), new MerchantDeleteRequest("delete-req-nm", "清理"));
+        TestSecurityActors.asPlatformAdmin("usr_admin2");
+        assertThatThrownBy(() -> service.approveDeleteMerchant(merchant.id(),
+                new MerchantDeleteApprovalRequest("delete-approve-nm", "approved", null)))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("MERCHANT_DELETE_APPROVER_NOT_MASTER"));
+        assertThat(platformRepository.findMerchantById(merchant.id()).orElseThrow().deleteStatus()).isEqualTo("pending");
+    }
+
+    @Test
+    void requestDeleteByMasterAuthorityExecutesWithoutApproval() {
+        MerchantRecord merchant = seedMerchant("seed-master");
+        service.archiveMerchant(merchant.id(), new MerchantArchiveRequest("archive-master", "测试存档理由"));
+        platformRepository.merchantReferenceCount = 0;
+        asMasterAuthority();
+        MerchantRecord deleted = service.requestDeleteMerchant(merchant.id(),
+                new MerchantDeleteRequest("delete-req-master", "最高权限直接清理"));
+        assertThat(deleted.deleteStatus()).isEqualTo("approved");
+        assertThat(deleted.deleteRequestedBy()).isEqualTo(SessionIssuer.MASTER_USER_ID);
+        assertThat(service.listMerchants(false).items()).noneMatch(m -> m.id().equals(merchant.id()));
+    }
+
+    // ---- M009-014：字典分页元数据须覆盖全部条目 ----
+    @Test
+    void listSpaceTypesPageSizeCoversAllItems() {
+        var page = service.listSpaceTypes();
+        assertThat(page.pageSize()).isGreaterThanOrEqualTo(page.items().size());
+        assertThat(page.pageSize()).isEqualTo(500);
+    }
+
     // V348 商户业态标准字段：五值之外的业态一律拒收（业态错＝通票分账份额拆给错的业态）。
     @Test
     void createMerchantRejectsBusinessFormatOutsideFiveValues() {
@@ -152,7 +335,7 @@ class PlatformServiceTests {
                 new MerchantDeleteRequest("delete-req", "录入错误，退场清理"));
 
         platformRepository.merchantReferenceCount = 0;
-        TestSecurityActors.asPlatformAdmin("usr_admin2"); // 禁自审:审批人≠申请发起人
+        asMasterAuthority(); // M009-009：删除只能由最高权限审批（BSM-004），且≠申请发起人
         MerchantDeleteDecision decision = service.approveDeleteMerchant(merchant.id(),
                 new MerchantDeleteApprovalRequest("delete-approve", "approved", null));
 
@@ -169,7 +352,7 @@ class PlatformServiceTests {
                 new MerchantDeleteRequest("delete-req-2", "尝试删除仍在用商户"));
 
         platformRepository.merchantReferenceCount = 3; // 模拟仍被酒吧商品/合同等引用
-        TestSecurityActors.asPlatformAdmin("usr_admin2"); // 禁自审:审批人≠申请发起人
+        asMasterAuthority(); // M009-009：删除只能由最高权限审批（BSM-004），且≠申请发起人
 
         assertThatThrownBy(() -> service.approveDeleteMerchant(merchant.id(),
                 new MerchantDeleteApprovalRequest("delete-approve-2", "approved", null)))
@@ -187,8 +370,7 @@ class PlatformServiceTests {
                 "create entity",
                 List.of("att_1"),
                 " P0 Entity ",
-                "operation",
-                null,
+                "management_company",
                 "91310000MA1P0",
                 "Jane Ops",
                 "vault://entity/bank",
@@ -209,11 +391,12 @@ class PlatformServiceTests {
 
     @Test
     void createProjectUsesOpenApiAuditEventNames() {
+        MerchantRecord owner = seedMerchant("seed-project-owner"); // M009-007：merchantId 须存在
         ProjectRecord created = service.createProject(new ProjectRequest(
                 "project-create",
                 "create project",
                 List.of(),
-                "mch_1",
+                owner.id(),
                 "bar",
                 "P0 Bar",
                 "BAR-01",
@@ -231,7 +414,8 @@ class PlatformServiceTests {
 
     @Test
     void updateMerchantWritesAuditAndPreservesId() {
-        MerchantRecord updated = service.updateMerchant("mch_existing", new MerchantProfileRequest(
+        MerchantRecord existing = seedMerchant("seed-existing"); // M009-002：更新须命中已有商户
+        MerchantRecord updated = service.updateMerchant(existing.id(), new MerchantProfileRequest(
                 "merchant-update",
                 "update merchant",
                 List.of(),
@@ -249,11 +433,11 @@ class PlatformServiceTests {
                 "Jack Contact",
                 "18800000013",
                 BigDecimal.valueOf(12.30),
-                "fixed_rent_plus_commission",
+                "lease",
                 "active"
         ));
 
-        assertThat(updated.id()).isEqualTo("mch_existing");
+        assertThat(updated.id()).isEqualTo(existing.id());
         assertThat(updated.merchantName()).isEqualTo("P0 Merchant");
         assertThat(updated.mallBlockCode()).isEqualTo("1F-A");
         assertThat(updated.ownerPerson()).isEqualTo("Owner Li");
@@ -262,7 +446,10 @@ class PlatformServiceTests {
         assertThat(updated.secondaryContactName()).isEqualTo("Jack Contact");
         assertThat(updated.secondaryContactPhone()).isEqualTo("18800000013");
         assertThat(updated.rentPerSquareMeter()).isEqualByComparingTo("12.30");
-        assertThat(updated.cooperationForm()).isEqualTo("fixed_rent_plus_commission");
+        assertThat(updated.cooperationForm()).isEqualTo("lease");
+        // M009-008：三个主体列以生效绑定为准，档案更新不覆盖（seed 时 businessEntityId=be_1、其余空）。
+        assertThat(updated.businessEntityId()).isEqualTo("be_1");
+        assertThat(updated.contractEntityId()).isNull();
         assertThat(auditEventRepository.events)
                 .extracting(AuditEvent::eventName)
                 .contains("merchant.updated");
@@ -296,11 +483,16 @@ class PlatformServiceTests {
 
     @Test
     void createBusinessEntityBindingAllowsOnlyOneActiveBindingPerMerchant() {
+        MerchantRecord merchant = seedMerchant("seed-binding"); // M009-006：经营对象与主体须存在
+        for (String id : List.of("be_operation_1", "be_contract_1", "be_settlement_1",
+                "be_operation_2", "be_contract_2", "be_settlement_2")) {
+            seedEntity(id);
+        }
         BusinessEntityBindingRequest first = new BusinessEntityBindingRequest(
                 "binding-first",
                 "first binding",
                 List.of("att_1"),
-                "mch_binding",
+                merchant.id(),
                 "be_operation_1",
                 "be_contract_1",
                 "be_settlement_1",
@@ -311,12 +503,12 @@ class PlatformServiceTests {
         BusinessEntityBindingRecord created = service.createBusinessEntityBinding(first);
 
         assertThat(created.status()).isEqualTo("active");
-        assertThat(created.merchantId()).isEqualTo("mch_binding");
+        assertThat(created.merchantId()).isEqualTo(merchant.id());
         assertThatThrownBy(() -> service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
                 "binding-second",
                 "second binding",
                 List.of(),
-                "mch_binding",
+                merchant.id(),
                 "be_operation_2",
                 "be_contract_2",
                 "be_settlement_2",
@@ -333,11 +525,16 @@ class PlatformServiceTests {
 
     @Test
     void changeBusinessEntityBindingRecordsStatusHistoryAndFreesActiveUniqueness() {
+        MerchantRecord merchant = seedMerchant("seed-change"); // M009-006
+        for (String id : List.of("be_operation", "be_contract", "be_settlement",
+                "be_operation_new", "be_contract_new", "be_settlement_new")) {
+            seedEntity(id);
+        }
         BusinessEntityBindingRecord created = service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
                 "binding-change-create",
                 "create binding",
                 List.of(),
-                "mch_change",
+                merchant.id(),
                 "be_operation",
                 "be_contract",
                 "be_settlement",
@@ -353,7 +550,7 @@ class PlatformServiceTests {
                 "binding-change-replacement",
                 "replacement binding",
                 List.of(),
-                "mch_change",
+                merchant.id(),
                 "be_operation_new",
                 "be_contract_new",
                 "be_settlement_new",
@@ -371,11 +568,15 @@ class PlatformServiceTests {
     // P1-4 red→green: action 字段拒绝非法枚举值
     @Test
     void changeBusinessEntityBindingRejectsInvalidAction() {
+        MerchantRecord merchant = seedMerchant("seed-enum"); // M009-006
+        for (String id : List.of("be_op", "be_ctr", "be_stl")) {
+            seedEntity(id);
+        }
         BusinessEntityBindingRecord created = service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
                 "binding-enum-create",
                 "create",
                 List.of(),
-                "mch_enum",
+                merchant.id(),
                 "be_op",
                 "be_ctr",
                 "be_stl",
@@ -397,8 +598,7 @@ class PlatformServiceTests {
                 "entity corp-A",
                 List.of(),
                 "Corp A",
-                "operation",
-                null,
+                "management_company",
                 null,
                 null,
                 null,
@@ -409,8 +609,7 @@ class PlatformServiceTests {
                 "entity corp_A",
                 List.of(),
                 "Corp B",
-                "operation",
-                null,
+                "management_company",
                 null,
                 null,
                 null,
@@ -431,7 +630,7 @@ class PlatformServiceTests {
         assertThat(service.listSpaceTypes().items()).extracting(SpaceTypeRecord::typeCode).contains("kiosk", "shop");
         // 新录入类型立即可用于 createSpace 硬校验。
         SpaceRecord space = service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
-                "sp-kiosk", "create", List.of(), "prj1", "mch1", null, "K1", "kiosk", null, null, null, "active"));
+                "sp-kiosk", "create", List.of(), null, null, null, "K1", "kiosk", null, null, null, "active")); // M009-007：归属为空或须存在
         assertThat(space.spaceType()).isEqualTo("kiosk");
     }
 
@@ -439,7 +638,7 @@ class PlatformServiceTests {
     void createSpaceRejectsUnknownSpaceType() {
         // 条目3 硬校验：不在字典内的 space_type → VALIDATION_FAILED。
         assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
-                        "sp-bad", "create", List.of(), "prj1", "mch1", null, "Bad", "no_such_type", null, null, null, "active")))
+                        "sp-bad", "create", List.of(), null, null, null, "Bad", "no_such_type", null, null, null, "active")))
                 .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class, ex ->
                         assertThat(ex.code()).isEqualTo("VALIDATION_FAILED"));
     }
@@ -447,7 +646,7 @@ class PlatformServiceTests {
     @Test
     void createSpaceAcceptsDictionarySpaceType() {
         SpaceRecord saved = service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
-                "sp-ok", "create", List.of(), "prj1", "mch1", null, "Shop A", "shop", "1", null, null, "active"));
+                "sp-ok", "create", List.of(), null, null, null, "Shop A", "shop", "1", null, null, "active"));
         assertThat(saved.spaceType()).isEqualTo("shop");
     }
 
diff --git a/backend/src/test/java/com/complex/platform/leasing/LeasingSignAtomicityJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/leasing/LeasingSignAtomicityJdbcIntegrationTests.java
index dbd47b67..0e60c08e 100644
--- a/backend/src/test/java/com/complex/platform/leasing/LeasingSignAtomicityJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/leasing/LeasingSignAtomicityJdbcIntegrationTests.java
@@ -1,4 +1,5 @@
 package com.complex.platform.leasing;
+// 文件版本：V0.01
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
@@ -51,6 +52,7 @@ class LeasingSignAtomicityJdbcIntegrationTests {
         jdbcTemplate.update("DELETE FROM contracts WHERE contract_subject_id = ?", subject);
         jdbcTemplate.update("DELETE FROM business_entity_bindings WHERE contract_entity_id = ?", subject);
         jdbcTemplate.update("DELETE FROM merchants WHERE contract_entity_id = ?", subject);
+        jdbcTemplate.update("DELETE FROM business_entities WHERE id = ?", subject); // M009-006 夹具主体
         jdbcTemplate.update("DELETE FROM leasing_intents WHERE id = ?", intentId);
         jdbcTemplate.update("DELETE FROM leasing_leads WHERE id = ?", leadId);
         jdbcTemplate.update("DELETE FROM idempotency_records WHERE idempotency_key LIKE ?", "%" + suffix + "%");
@@ -114,6 +116,9 @@ class LeasingSignAtomicityJdbcIntegrationTests {
     private void seed(String intendedSpaceId) {
         TestSecurityActors.asPlatformAdmin();
         Instant now = Instant.now();
+        // M009-006：四类主体绑定要求主体真实存在（BEQR-143/PGM-026），合同主体须先在 business_entities 建档。
+        jdbcTemplate.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at) "
+                + "VALUES (?, ?, 'management_company', 'active', now(), now())", subject, "签约测试合同主体");
         leasingRepository.saveLead(new LeasingLeadRecord(leadId, "签约测试联系人", "13800002222", "referral",
                 "retail", intendedSpaceId, null, null, "usr_sales", "negotiating", null, null, null, now, now));
         leasingRepository.saveIntent(new LeasingIntentRecord(intentId, leadId, "pending_signing", intendedSpaceId,

```

## 4. 本席复审结论（席甲，作者自审票）

按 `receiving-code-review`/`verification-before-completion` 收窄：本席逐块读完上述 diff 全文（16 文件，527+/124-），并以 §2 归因表核对每一行。

### 4.1 修复内容（逐 ID）

| ID | 修复内容 | 证据 | 本席票 |
|---|---|---|---|
| 001 | `saveMerchant(update=true)` 先 `requireMerchant` 再用 25 参构造保留 archived_*/delete_*/createdAt | `updateMerchantKeepsArchiveAndPendingDeleteState` 红→绿 | PASS |
| 002 | 同处：不存在 → `MERCHANT_NOT_FOUND` | `updateMerchantRejectsUnknownMerchant` 红→绿 | PASS |
| 003 | `MERCHANT_COOPERATION_FORMS` 六类白名单（空允许）；LeasingService `"leasing_auto"`→null；页面六类中文 option + `cooperationLabel`（旧码经 formatBusinessStatus 回退显示） | `createMerchantRejectsCooperationFormOutsideSixModes` 红→绿；vitest 12/12；LeasingServiceTests 11/11 | PASS |
| 004 | `SUBJECT_STATUSES`（商户/主体）与 `PROJECT_STATUSES` 白名单；归档不作可设代码 | `createMerchantRejectsStatusOutsideEnumeratedStates`、`createProjectRejectsUnknownStatusOrMerchant` 红→绿 | PASS |
| 005 | `BUSINESS_ENTITY_ROLES` 三角色白名单，400 中文 | `createBusinessEntityRejectsRoleOutsideWhitelist` 红→绿 | PASS |
| 006 | 绑定前 `requireMerchant` + 三主体 `businessEntityExists`（Jdbc 按主键、接口默认基于列表）；LeasingSignAtomicity 夹具 seed/清理主体行为必要伴生 | `createBusinessEntityBindingRejectsMissingMerchantOrEntity` 红→绿；JDBC `businessEntityExists` 断言；LeasingSignAtomicity 2/2 | PASS |
| 007 | 项目 merchantId / 空间 projectId、merchantId、parentSpaceId 存在性 | `createSpaceRejectsUnknownProjectMerchantOrParent`、`createProjectRejectsUnknownStatusOrMerchant` 红→绿 | PASS |
| 008 | `syncMerchantEntities` 于绑定创建/激活回写三列；档案更新保留三列；`MerchantRecord.withEntities` | `createBusinessEntityBindingSyncsMerchantEntityColumns` 红→绿；`updateMerchantWritesAuditAndPreservesId` 三列断言 | PASS |
| 009 | approve 增加 `isMasterAuthority` 闸（403 `MERCHANT_DELETE_APPROVER_NOT_MASTER`）；最高权限发起删除直接软删（占用检查+留痕） | `approveDeleteByNonMasterIsForbidden`、`requestDeleteByMasterAuthorityExecutesWithoutApproval` 红→绿；既有两条审批用例改用最高权限身份 | PASS |
| 010 | 四处注释改为软删除口径；`includeArchived=true` 纳入 approved 行（列表与分页 SQL）；恢复复用既有 unarchive | JDBC `approvedDeletedMerchantStaysQueryableWithIncludeArchived` 红→绿 | PASS |
| 011 | 删除存档路径 `expireBarSingleStoreTickets` 调用与专用五参注入，保留单一四参构造 | 编译级；ServiceTests 29/29 | PASS |
| 012 | 从 Record/DTO/Service/Jdbc（INSERT/SELECT×2/参数/映射）删除 parentEntityId；列与 V318/V354 未动 | 编译级；JDBC 12 中 11 通过（唯一红为 017 环境） | PASS |
| 014 | `listSpaceTypes` 改 `PageResponse.of(records, 1, 500)`，删 `page()`/`DEFAULT_PAGE_SIZE` | `listSpaceTypesPageSizeCoversAllItems` 红→绿 | PASS |
| 015 | 七个读端加 `requirePlatformScope()` | `listingMerchantsRequiresPlatformScope`、绑定列表 verify 红→绿 | PASS |
| 016 | 删除 `requireDate`、`readObjectMap`、`OBJECT_MAP_TYPE` | 编译级 | PASS |

### 4.2 范围归因（§5 闸 3）

- 16 文件全部改动行均落到定稿 15 个 ID（§2 表）；「文件版本：V0.01」行按操作员版本铁律随所属 ID。
- 未改：三份业务 HTML、`plan/*`、锁、sidecar、ledger 基准、权限种子、迁移文件、013/017–025 内容、其它模块业务逻辑（LeasingService 仅 1 行常量、其测试仅夹具 seed/清理）。`git diff --stat before after -- docs/*.html docs/audit-2026-09/plan/` 为空。
- 未提交/推送/合并/部署；工作区其它脏文件为既有父链，不在本 diff（before/after 均含）。

### 4.3 环境红（不由本 diff 引入）

`PlatformJdbcIntegrationTests.residualMigration…` 与 `LeasingJdbcIntegrationTests.migrationCreates…` 断言 `MigrationVersions.latest()="399"` 而 `complex_test` 已为 409：M009-017（环境，否队列），红阶段与绿阶段一致，须操作员知悉。

### 4.4 结论

内容修复：**PASS**。范围归因：**PASS**。总：**PASS**（作者自审票；须 Grok B4-r1 亦 PASS 才进 B5）。本席在本报告落盘前未读取 `M009-b4-r1-grok-raw.md`。


CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
SEALED_UTC: 2026-09-14T17:54:06.944711+00:00

# M009 当前授权重审 B4 第1次 — Codex 独立完整报告
版本：V0.02
本报告为本文件末尾新的完整报告；此前 V0.01 和其它旧结论全部保留，不作当前票。原471模块计划、M009原20文件/2879行/87映射不变。
独立性：Grok已先派发，我同步自行审阅并在读取本轮Grok raw前封存本报告；仅接收其运行会话无内容状态。没有第三席。只在160主工作区SSH核验。本阶段无源码修复。
before: fa05f8c4af1ad4cf7e048ed5e074ed09cd48eeac
after: 66f312549c6f1ea7ed5e510f639e9238c4c674e9
HEAD: e456ba96c84185c8d39f18f548011daac48a6466
diff: 185872 bytes / SHA256 67a1597fa96e386cb9d21ee0a4c6484213676de9dff433547a88ab08136b9f46
输入定稿：findings.md V2.08当前第三批表，14是/6否，B1两份raw末尾V0.08 B2双PASS。013/038操作员批准持续有效。
B3已执行1/3，当前IN_PROGRESS。不存在发布/提交/部署结论。

## 本席完整结论
CONTENT_VERDICT: FAIL
ATTRIBUTION_VERDICT: PASS
VERDICT: FAIL
25个文件、140个diff hunk逐块归因见下，包含新DTO、迁移、全部测试与必要跨模块身份夹具；无无ID改动，故本席不主张越界回滚。
已批准十四项的主体实现均有对应变更与执行证据；但修正真实最高权限身份后，其直接回归用例暴露既有岗位模板生命周期入口拒绝真实master的问题。当前14个YES的具体定稿未包含AccessService该动作入口修复，须作为新候选按整批B2归并，不擅自归入B3。
这是本席报告，不覆盖Grok；两席全部raw落齐后按任一FAIL统一路由，不提前修复。

## 完整问题集（1项）
临时定位 CX-B4-01：真实最高权限无法到达既有岗位模板删除审批分支。
定位：backend/src/main/java/com/complex/platform/access/AccessService.java:488、511、523、2181-2184；本批直接依赖测试 backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java:588-592。
复现：当前真实SessionIssuer.MASTER_USER_ID / MASTER_USERNAME / MASTER_ACCOUNT_TYPE身份调用changePositionTemplateLifecycle("lifecycle", approve_delete)，在requirePlatformAdmin即抛PLATFORM_ADMIN_REQUIRED。该方法后续已有仅最高权限可审批的分支，却被入口拦截。
判据：冻结BSM-004（配置类数据存档后申请删除，由持授权密钥的最高权限审批）、GEN-005（按主体/对象/动作服务端授权）、GEN-008（真实身份与测试/接口一致）。本缺陷不是要求所有platform_admin接口对最高权限放开；不得扩成全局角色豁免。
证据：B4只读重跑1例、0failure/1error、exit1，堆栈直达488/2183；B3期间单独恢复before树AuthorizationService而保留真实master夹具，同一失败仍复现。旧假platform_admin夹具掩盖既有生产分支不可达，不能恢复假身份使测试变绿。
影响：持密钥人无法完成这个现有配置删除审批；普通管理员又不符合内层最高权限要求，删除申请无法通过该入口完成审批。
分类建议：细化中的工程缺陷；无新增业务实体、角色、状态、收费、审批级别或绕过权责审核。仅审查现有显式对象的request_delete/approve_delete/reject_delete动作守卫与真实身份是否兼容；archive/restore及其它平台管理动作不一并放宽。最终ID/范围待整批B2双PASS。
本项未在B4改码，未自行分配正式ID，未令任何YES转NO。

## 各项实现审查
013：项目Request/Record新增可空负责人/地址/经营范围，创建与JDBC所有读取/写入通路齐全；旧构造及缺字段JSON兼容；真实SQL往返通过。无页面或旧项目编辑入口。
038：五种列表/COUNT在SQL中按真实当前项目过滤，非项目org或null不退回全局；普通商户保存不能补录/迁移旧空项目；专用补录仅UPDATE project_id，真实数据库其余列（含加密电话与时间）逐值保持；公司多项目使用既有对象关系表和显式最高权限入口。Master按生产三件套+active验证，普通管理员不可冒充，DENY/未知permission仍阻止。上述CX-B4-01是该真实身份直接回归的待归并问题。
039：空间必须指定当前真实项目，父空间与商户同项目，spaceExists也走项目归属。
040/041/031：同商户事务行锁覆盖创建/激活的查重与写入；未来绑定使用既有inactive且end=null、不投影；创建与激活都拒绝其它active或未结束inactive。已有财税三主体不能由普通入口改换，生效绑定普通停用/暂停拒绝且不清投影。历史end非空不重开、未来不可提前结束。Leasing只在自动建商户调用点传意向项目并暂留三主体空，仍由后续权威绑定处理生效日。当前真实SQL并发两请求先等待同商户锁，最终一成功一ACTIVE_BINDING_EXISTS；删除锁的负向验证能失败。
042：disabled和archived可恢复active，pending_delete不可绕过；历史请求保留。
043/044：SpaceTypeRecord/MerchantDeleteDecision补真实对象ID，理由/审批结论写不可变审计metadata，驳回事件为rejected；当前类型删除请求完成approved/rejected，同事务保存类型状态，最高权限直办也结清当前请求，旧请求历史保持。
045：仅有效起止且真正结束的inactive绑定不算活占用；未来/未结束/倒置日期/其它状态仍计数，其它所有引用表不改。
046：容量、空间/商户/租赁面积及租金单价拒绝负数，可空和零保持。
030：初建时已提供三主体必须真实存在且关联当前项目；缺省null保留，不发明强制自动绑定。
047：只删用户message的英文枚举括注，内部码与中文意义保留。
018：普通新建或由其它状态切到terminated拒绝，旧terminated在无状态变化或省略状态时保持；没有新增退出调度、黑名单联合审批。

## 顺从、对抗、第一性原理
顺从线：逐一将所有变更回扣14个YES及其冻结判据；六个NO仍是登记项，不按NO修复；HTML全不改；类型恢复、归属隔离、财税绑定和三字段资料分别按批准语义复核。
对抗线：实际覆盖跨项目读写/COUNT、假master、真实master非项目组织、旧空归属认领、既有归属迁移、项目补录污染其它列、未来生效提前投影、重复预约、真并发、重开历史、活跃绑定停用换主体、负数、当前删除申请历史与审计ID。没有因测试绿就接受测试：核过夹具身份、SQL真实内容、失败断言和三个去保护负向回放。
第一性原理一：这些差异为何存在？013落实PGM-020项目档案；038/039落实PGM-041/PGM-017归属隔离；040/041/031/030落实PGM-026权威绑定；042-045落实BSM-004可恢复、删除审批及审计。DTO兼容构造、关系表/索引、COUNT分页、版本行及验证夹具是这些已批准行为的最小必要承载，不是新业务规则。
第一性原理二：是否实跑？后附160 Maven输出；SQL真实往返、独立事务并发、对三个保护的负向验证均已执行，核心/直接七类188通过。但全体相关十一类282结果为273通过、1failure+8errors，不能声称整个回归全绿。
第一性原理三：哪个前提未经验证？真实master不是platform_admin已被验证，原M006夹具这个前提是错的并暴露CX-B4-01。八项其它失败在before身份实现和夹具复现，不能当本批新增回归，也不能宣称它们已解决。Leasing上游createLead仍有projectId=null的只读观察，不把已归属intent的调用点测试冒充整个招商链已闭环。B4是全部diff审查，B5整模块全量审计尚未执行。

## 验证边界与执行依据
后端命令统一在/home/james/complex-management/backend，JDK21，mvn -o -q，测试库仅jdbc:postgresql://127.0.0.1:5432/complex_test；凭据只从/etc/complex/complex.env读取，未输出。
完整十一类：PlatformServiceTests,PlatformJdbcIntegrationTests,PlatformControllerTests,LeasingServiceTests,LeasingSignAtomicityJdbcIntegrationTests,AuthorizationServiceTests,SupervisorReviewServiceTests,M006SecurityRegressionTests,M010RegressionTests,BarStoredValueFundChainJdbcIntegrationTests,StoredValueNotificationIntegrationTest。
最后格式变更后三个核心类106/106通过：PlatformService69、PlatformJdbc23、PlatformController14；此前其它四个直接类：Leasing11、LeasingSign3、Authorization24、SupervisorReview44，总188。
八项before可复现失败逐条名称与错误在下方原输出，不删除失败测试，不放宽商户范围/直接受益人要求。
三个临时负向试验仅在B3进行，移除创建行锁/历史end检查/Leasing显式项目传递各让对应测试断言失败；finally恢复并递增文件版本。B4没有上述源码变异。
git diff --check before after通过；正式index未变（B3快照回执）；HEAD和全部25源码/测试字节与after相等；三HTML在before/after/工作区一致；criteria实际/sidecar/ledger及入场B1Grok尾锚点四值一致。
skills：本席沿用本轮已实读using-superpowers、grilling、codebase-design、diagnosing-bugs、systematic-debugging、TDD、writing-for-agents、ponytail-review、verification-before-completion；B4已实际读requesting-code-review技能及reviewer模板，按控制提示词只派Grok，不派技能的第三席。无假造可选技能可用性。

## 大hunk内部归因补充
PlatformService: 导入/项目scope/专用配置/ordinary保存projectId→038；createProject三字段→013、capacity→046；createSpace所属/父空间→039、scope→038、面积→046；binding调用锁/预约/生效日期→041、财税身份与active失效阻断→040、历史结束期→031、关联对象校验→038；saveMerchant中初始主体→030、terminated→018、非负→046；auditMetadata中project配置→038、理由与decision→043；businessId→043。
JdbcPlatformRepository旧227块同时包含商户查询project_id→038和项目三字段SQL→013；旧563块completeSpaceTypeDeleteRequest→044、lockMerchant/hasUnfinished→041；旧1164块公司PK/关系/项目补录/五scope分页→038，其中项目三资料SELECT→013。
PlatformRepository旧109块锁和unfinished默认实现→041；旧142块公司/项目关系、项目补录和五scope默认分页→038。
PlatformServiceTests旧862大块：unbound/future/reserve/due→041/031；financialreplace→040；projectCreation→013；deletedType→042；completeRequest→044；两类audit→043；numeric→046；terminated两例→018；userFacingRole→047；twoProject/list/master/config及fake实现→038；space约束→039/046；initialentity→030/038；fakecomplete→044、fakelock→041。
PlatformJdbcIntegrationTests旧381大块：schema/三资料readback→013/038；referenceperiod→045；scope/manytomany/onlyprojectbackfill/旧空资料兼容→038/013；currentrequesthistory→044；realconcurrency+transaction注入→041。
所有新测试依赖/构造调整/清理自己的测试数据/版本行分别归对应测试或本文件修复ID；M006两处纯模板建立测试使用普通管理员、M010真实master填写已有公司范围，都是038身份夹具对齐，不改生产授权。

## 当前B4复现的可运行命令
```python
from pathlib import Path
import os,shlex,subprocess,sys,time,xml.etree.ElementTree as ET
r=Path('/home/james/complex-management/backend')
env=os.environ.copy()
for line in Path('/etc/complex/complex.env').read_text().splitlines():
 line=line.removeprefix('export ').strip()
 if '=' in line and line.split('=',1)[0] in ['COMPLEX_DATABASE_URL','COMPLEX_DATABASE_USERNAME','COMPLEX_DATABASE_PASSWORD']:
  k,v=line.split('=',1);vs=shlex.split(v);env[k]=vs[0] if vs else ''
env['COMPLEX_DATABASE_URL']='jdbc:postgresql://127.0.0.1:5432/complex_test'
env['COMPLEX_DATABASE_USERNAME']='complex'
env['JAVA_HOME']='/usr/lib/jvm/temurin-21-jdk-amd64'
env['PATH']=env['JAVA_HOME']+'/bin:'+env['PATH']
tests="M006SecurityRegressionTests#positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval"
started_ns=time.time_ns()
p=subprocess.run(['mvn','-o','-q','-Dtest='+tests,'-Dspring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test','test'],cwd=r,env=env,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
print(p.stdout);print('MAVEN_EXIT='+str(p.returncode))
for name in tests.split(','):
 name=name.split('#',1)[0]
 for x in (r/'target/surefire-reports').glob('TEST-*.'+name+'.xml'):
  if x.stat().st_mtime_ns < started_ns:continue
  t=ET.parse(x).getroot();print(x.name,{k:t.attrib.get(k) for k in ['tests','failures','errors','skipped','time']})
sys.exit(p.returncode)

```

## 已执行验证的原始输出（只省略常规INFO，不更改错误与计数）
```text

### cxr_b3_full_test_result
[ERROR] Tests run: 3, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 4.460 s <<< FAILURE! -- in com.complex.platform.customer.StoredValueNotificationIntegrationTest
[ERROR] com.complex.platform.customer.StoredValueNotificationIntegrationTest.consumeWritesStoredValueConsumeNotificationLogWhenSwitchesOn -- Time elapsed: 0.019 s <<< ERROR!
[ERROR] Tests run: 44, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 0.936 s <<< FAILURE! -- in com.complex.platform.access.staff.M010RegressionTests
[ERROR] com.complex.platform.access.staff.M010RegressionTests.OwnAndManagementProfileResponsesDoNotReturnPersonalValues -- Time elapsed: 0.011 s <<< ERROR!
[ERROR] Tests run: 39, Failures: 0, Errors: 2, Skipped: 0, Time elapsed: 0.668 s <<< FAILURE! -- in com.complex.platform.access.M006SecurityRegressionTests
[ERROR] com.complex.platform.access.M006SecurityRegressionTests.topAuthorityReviewRequiresMasterSessionRatherThanRankAlone -- Time elapsed: 0.056 s <<< ERROR!
[ERROR] com.complex.platform.access.M006SecurityRegressionTests.positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval -- Time elapsed: 0.004 s <<< ERROR!
[ERROR] Tests run: 8, Failures: 1, Errors: 4, Skipped: 0, Time elapsed: 0.097 s <<< FAILURE! -- in com.complex.platform.bar.BarStoredValueFundChainJdbcIntegrationTests
[ERROR] com.complex.platform.bar.BarStoredValueFundChainJdbcIntegrationTests.idempotentReplayWithSameFingerprintReturnsOriginalTransaction -- Time elapsed: 0.005 s <<< ERROR!
[ERROR] com.complex.platform.bar.BarStoredValueFundChainJdbcIntegrationTests.settleBillWithStoredValueChannelPersistsSettlementAndDeductsBalance -- Time elapsed: 0.006 s <<< ERROR!
[ERROR] com.complex.platform.bar.BarStoredValueFundChainJdbcIntegrationTests.merchantPrepaidCardRefundsAtCounterWhileOnlineRefundStaysRejected -- Time elapsed: 0.007 s <<< FAILURE!
[ERROR] com.complex.platform.bar.BarStoredValueFundChainJdbcIntegrationTests.onlineAndCounterRefundKeysLiveInSeparateNamespaces -- Time elapsed: 0.004 s <<< ERROR!
[ERROR] com.complex.platform.bar.BarStoredValueFundChainJdbcIntegrationTests.settleBillRejectsReplayOfForeignConsumeTransaction -- Time elapsed: 0.005 s <<< ERROR!
[ERROR] Failures: 
[ERROR]   BarStoredValueFundChainJdbcIntegrationTests.merchantPrepaidCardRefundsAtCounterWhileOnlineRefundStaysRejected:199 
[ERROR] Errors: 
[ERROR]   M006SecurityRegressionTests.positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval:592 » Api 此操作需要平台管理员权限
[ERROR]   M006SecurityRegressionTests.topAuthorityReviewRequiresMasterSessionRatherThanRankAlone:414 » Api 退款、免单、授权与资金事项必须登记直接受益人
[ERROR]   M010RegressionTests.OwnAndManagementProfileResponsesDoNotReturnPersonalValues:480 » Api 此操作必须在商户范围内进行
[ERROR]   BarStoredValueFundChainJdbcIntegrationTests.idempotentReplayWithSameFingerprintReturnsOriginalTransaction:147 » Api 此操作必须在商户范围内进行
[ERROR]   BarStoredValueFundChainJdbcIntegrationTests.onlineAndCounterRefundKeysLiveInSeparateNamespaces:228 » Api 此操作必须在商户范围内进行
[ERROR]   BarStoredValueFundChainJdbcIntegrationTests.settleBillRejectsReplayOfForeignConsumeTransaction:115 » Api 此操作必须在商户范围内进行
[ERROR]   BarStoredValueFundChainJdbcIntegrationTests.settleBillWithStoredValueChannelPersistsSettlementAndDeductsBalance:82 » Api 此操作必须在商户范围内进行
[ERROR]   StoredValueNotificationIntegrationTest.consumeWritesStoredValueConsumeNotificationLogWhenSwitchesOn:96 » Api 此操作必须在商户范围内进行
[ERROR] Tests run: 282, Failures: 1, Errors: 8, Skipped: 0
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.5.6:test (default-test) on project complex-api: There are test failures.
[ERROR] 
[ERROR] See /home/james/complex-management/backend/target/surefire-reports for the individual test results.
[ERROR] See dump files (if any exist) [date].dump, [date]-jvmRun[N].dump and [date].dumpstream.
[ERROR] -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
MAVEN_EXIT=1
TEST-com.complex.platform.governance.PlatformServiceTests.xml {'tests': '69', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.171'}
TEST-com.complex.platform.governance.PlatformJdbcIntegrationTests.xml {'tests': '23', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.474'}
TEST-com.complex.platform.governance.PlatformControllerTests.xml {'tests': '14', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.658'}
TEST-com.complex.platform.leasing.LeasingServiceTests.xml {'tests': '11', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.099'}
TEST-com.complex.platform.leasing.LeasingSignAtomicityJdbcIntegrationTests.xml {'tests': '3', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.098'}
TEST-com.complex.platform.access.AuthorizationServiceTests.xml {'tests': '24', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.012'}
TEST-com.complex.platform.governance.review.SupervisorReviewServiceTests.xml {'tests': '44', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.256'}
TEST-com.complex.platform.access.M006SecurityRegressionTests.xml {'tests': '39', 'failures': '0', 'errors': '2', 'skipped': '0', 'time': '0.668'}
TEST-com.complex.platform.access.staff.M010RegressionTests.xml {'tests': '44', 'failures': '0', 'errors': '1', 'skipped': '0', 'time': '0.936'}
TEST-com.complex.platform.bar.BarStoredValueFundChainJdbcIntegrationTests.xml {'tests': '8', 'failures': '1', 'errors': '4', 'skipped': '0', 'time': '0.097'}
TEST-com.complex.platform.customer.StoredValueNotificationIntegrationTest.xml {'tests': '3', 'failures': '0', 'errors': '1', 'skipped': '0', 'time': '4.46'}

### cxr_b3_core_final_result
MAVEN_EXIT=0
TEST-com.complex.platform.governance.PlatformServiceTests.xml {'tests': '69', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.179'}
TEST-com.complex.platform.governance.PlatformJdbcIntegrationTests.xml {'tests': '23', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '3.926'}
TEST-com.complex.platform.governance.PlatformControllerTests.xml {'tests': '14', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.821'}

### cxr_baseline_selected_run
BASELINE_SELECTED_SOURCE_AND_FIXTURES_INSTALLED
[ERROR] Tests run: 3, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 4.316 s <<< FAILURE! -- in com.complex.platform.customer.StoredValueNotificationIntegrationTest
[ERROR] com.complex.platform.customer.StoredValueNotificationIntegrationTest.consumeWritesStoredValueConsumeNotificationLogWhenSwitchesOn -- Time elapsed: 0.028 s <<< ERROR!
[ERROR] Tests run: 44, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 0.962 s <<< FAILURE! -- in com.complex.platform.access.staff.M010RegressionTests
[ERROR] com.complex.platform.access.staff.M010RegressionTests.OwnAndManagementProfileResponsesDoNotReturnPersonalValues -- Time elapsed: 0.010 s <<< ERROR!
[ERROR] Tests run: 39, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 0.892 s <<< FAILURE! -- in com.complex.platform.access.M006SecurityRegressionTests
[ERROR] com.complex.platform.access.M006SecurityRegressionTests.topAuthorityReviewRequiresMasterSessionRatherThanRankAlone -- Time elapsed: 0.042 s <<< ERROR!
[ERROR] Tests run: 8, Failures: 1, Errors: 4, Skipped: 0, Time elapsed: 0.207 s <<< FAILURE! -- in com.complex.platform.bar.BarStoredValueFundChainJdbcIntegrationTests
[ERROR] com.complex.platform.bar.BarStoredValueFundChainJdbcIntegrationTests.idempotentReplayWithSameFingerprintReturnsOriginalTransaction -- Time elapsed: 0.009 s <<< ERROR!
[ERROR] com.complex.platform.bar.BarStoredValueFundChainJdbcIntegrationTests.settleBillWithStoredValueChannelPersistsSettlementAndDeductsBalance -- Time elapsed: 0.011 s <<< ERROR!
[ERROR] com.complex.platform.bar.BarStoredValueFundChainJdbcIntegrationTests.merchantPrepaidCardRefundsAtCounterWhileOnlineRefundStaysRejected -- Time elapsed: 0.014 s <<< FAILURE!
[ERROR] com.complex.platform.bar.BarStoredValueFundChainJdbcIntegrationTests.onlineAndCounterRefundKeysLiveInSeparateNamespaces -- Time elapsed: 0.009 s <<< ERROR!
[ERROR] com.complex.platform.bar.BarStoredValueFundChainJdbcIntegrationTests.settleBillRejectsReplayOfForeignConsumeTransaction -- Time elapsed: 0.009 s <<< ERROR!
[ERROR] Failures: 
[ERROR]   BarStoredValueFundChainJdbcIntegrationTests.merchantPrepaidCardRefundsAtCounterWhileOnlineRefundStaysRejected:199 
[ERROR] Errors: 
[ERROR]   M006SecurityRegressionTests.topAuthorityReviewRequiresMasterSessionRatherThanRankAlone:418 » Api 退款、免单、授权与资金事项必须登记直接受益人
[ERROR]   M010RegressionTests.OwnAndManagementProfileResponsesDoNotReturnPersonalValues:480 » Api 此操作必须在商户范围内进行
[ERROR]   BarStoredValueFundChainJdbcIntegrationTests.idempotentReplayWithSameFingerprintReturnsOriginalTransaction:147 » Api 此操作必须在商户范围内进行
[ERROR]   BarStoredValueFundChainJdbcIntegrationTests.onlineAndCounterRefundKeysLiveInSeparateNamespaces:228 » Api 此操作必须在商户范围内进行
[ERROR]   BarStoredValueFundChainJdbcIntegrationTests.settleBillRejectsReplayOfForeignConsumeTransaction:115 » Api 此操作必须在商户范围内进行
[ERROR]   BarStoredValueFundChainJdbcIntegrationTests.settleBillWithStoredValueChannelPersistsSettlementAndDeductsBalance:82 » Api 此操作必须在商户范围内进行
[ERROR]   StoredValueNotificationIntegrationTest.consumeWritesStoredValueConsumeNotificationLogWhenSwitchesOn:96 » Api 此操作必须在商户范围内进行
[ERROR] Tests run: 94, Failures: 1, Errors: 7, Skipped: 0
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.5.6:test (default-test) on project complex-api: There are test failures.
[ERROR] 
[ERROR] See /home/james/complex-management/backend/target/surefire-reports for the individual test results.
[ERROR] See dump files (if any exist) [date].dump, [date]-jvmRun[N].dump and [date].dumpstream.
[ERROR] -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
MAVEN_EXIT=1
TEST-com.complex.platform.access.M006SecurityRegressionTests.xml {'tests': '39', 'failures': '0', 'errors': '1', 'skipped': '0', 'time': '0.892'}
TEST-com.complex.platform.access.staff.M010RegressionTests.xml {'tests': '44', 'failures': '0', 'errors': '1', 'skipped': '0', 'time': '0.962'}
TEST-com.complex.platform.bar.BarStoredValueFundChainJdbcIntegrationTests.xml {'tests': '8', 'failures': '1', 'errors': '4', 'skipped': '0', 'time': '0.207'}
TEST-com.complex.platform.customer.StoredValueNotificationIntegrationTest.xml {'tests': '3', 'failures': '0', 'errors': '1', 'skipped': '0', 'time': '4.316'}
CURRENT_IMPLEMENTATION_RESTORED_WITH_INCREMENTED_VERSIONS

### cxr_negative_guard_results
NEGATIVE_MUTATIONS_AND_OLD_IDENTITY_BASELINE_INSTALLED
[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 8.151 s <<< FAILURE! -- in com.complex.platform.governance.PlatformJdbcIntegrationTests
[ERROR] com.complex.platform.governance.PlatformJdbcIntegrationTests.concurrentFutureBindingsWaitForMerchantLockAndOnlyOneReservesTheSlot -- Time elapsed: 5.319 s <<< FAILURE!
[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.024 s <<< FAILURE! -- in com.complex.platform.governance.PlatformServiceTests
[ERROR] com.complex.platform.governance.PlatformServiceTests.dueBindingCanActivateButAnEndedHistoricalPeriodCannotReopen -- Time elapsed: 0.024 s <<< FAILURE!
[ERROR] Tests run: 1, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 0.191 s <<< FAILURE! -- in com.complex.platform.access.M006SecurityRegressionTests
[ERROR] com.complex.platform.access.M006SecurityRegressionTests.positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval -- Time elapsed: 0.190 s <<< ERROR!
[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.216 s <<< FAILURE! -- in com.complex.platform.leasing.LeasingServiceTests
[ERROR] com.complex.platform.leasing.LeasingServiceTests.signIntentWithoutMerchantAutoCreatesFormalMerchant -- Time elapsed: 0.045 s <<< FAILURE!
[ERROR] Failures: 
[ERROR]   PlatformJdbcIntegrationTests.concurrentFutureBindingsWaitForMerchantLockAndOnlyOneReservesTheSlot:552->lambda$concurrentFutureBindingsWaitForMerchantLockAndOnlyOneReservesTheSlot$10:588 [两个请求均在商户行锁上等待，不能跳过锁直接检查] 
[ERROR]   PlatformServiceTests.dueBindingCanActivateButAnEndedHistoricalPeriodCannotReopen:922 
[ERROR]   LeasingServiceTests.signIntentWithoutMerchantAutoCreatesFormalMerchant:247 Multiple Failures (1 failure)
[ERROR] Errors: 
[ERROR]   M006SecurityRegressionTests.positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval:592 » Api 此操作需要平台管理员权限
[ERROR] Tests run: 4, Failures: 3, Errors: 1, Skipped: 0
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.5.6:test (default-test) on project complex-api: There are test failures.
[ERROR] 
[ERROR] See /home/james/complex-management/backend/target/surefire-reports for the individual test results.
[ERROR] See dump files (if any exist) [date].dump, [date]-jvmRun[N].dump and [date].dumpstream.
[ERROR] -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
MAVEN_EXIT=1
TEST-com.complex.platform.governance.PlatformJdbcIntegrationTests.xml {'tests': '1', 'failures': '1', 'errors': '0', 'skipped': '0', 'time': '8.151'}
TEST-com.complex.platform.governance.PlatformServiceTests.xml {'tests': '1', 'failures': '1', 'errors': '0', 'skipped': '0', 'time': '0.024'}
TEST-com.complex.platform.leasing.LeasingServiceTests.xml {'tests': '1', 'failures': '1', 'errors': '0', 'skipped': '0', 'time': '0.216'}
TEST-com.complex.platform.access.M006SecurityRegressionTests.xml {'tests': '1', 'failures': '0', 'errors': '1', 'skipped': '0', 'time': '0.191'}
ALL_THREE_CURRENT_SOURCES_RESTORED

### cxr_b4_master_repro_start
[ERROR] Tests run: 1, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 0.438 s <<< FAILURE! -- in com.complex.platform.access.M006SecurityRegressionTests
[ERROR] com.complex.platform.access.M006SecurityRegressionTests.positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval -- Time elapsed: 0.428 s <<< ERROR!
	at com.complex.platform.common.api.ApiException.forbidden(ApiException.java:44)
	at com.complex.platform.access.AccessService.requirePlatformAdmin(AccessService.java:2183)
	at com.complex.platform.access.AccessService.changePositionTemplateLifecycle(AccessService.java:488)
	at com.complex.platform.access.M006SecurityRegressionTests.positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval(M006SecurityRegressionTests.java:592)
[ERROR] Errors: 
[ERROR]   M006SecurityRegressionTests.positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval:592 » Api 此操作需要平台管理员权限
[ERROR] Tests run: 1, Failures: 0, Errors: 1, Skipped: 0
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.5.6:test (default-test) on project complex-api: 
[ERROR] 
[ERROR] See /home/james/complex-management/backend/target/surefire-reports for the individual test results.
[ERROR] See dump files (if any exist) [date].dump, [date]-jvmRun[N].dump and [date].dumpstream.
[ERROR] -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
MAVEN_EXIT=1
TEST-com.complex.platform.access.M006SecurityRegressionTests.xml {'tests': '1', 'failures': '0', 'errors': '1', 'skipped': '0', 'time': '0.438'}

```

## 全部hunk正式ID归因
| 序号 | 文件 | hunk | 正式ID |
|---|---|---|---|
| 1 | backend/src/main/java/com/complex/platform/access/AuthorizationService.java | @@ -1,6 +1,6 @@ | M009-038 |
| 2 | backend/src/main/java/com/complex/platform/access/AuthorizationService.java | @@ -137,7 +137,8 @@ public class AuthorizationService implements CallerScopeResolver { | M009-038 |
| 3 | backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | @@ -1,5 +1,5 @@ | M009-013, M009-038, M009-041, M009-044, M009-045 |
| 4 | backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | @@ -83,14 +83,14 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo | M009-038 |
| 5 | backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | @@ -127,7 +127,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo | M009-038 |
| 6 | backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | @@ -142,7 +142,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo | M009-038 |
| 7 | backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | @@ -182,7 +182,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo | M009-038 |
| 8 | backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | @@ -218,8 +218,12 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo | M009-045 |
| 9 | backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | @@ -227,31 +231,31 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo | M009-038, M009-013 |
| 10 | backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | @@ -261,7 +265,10 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo | M009-013 |
| 11 | backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | @@ -269,7 +276,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo | M009-013 |
| 12 | backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | @@ -467,7 +474,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo | M009-038 |
| 13 | backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | @@ -480,7 +487,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo | M009-013 |
| 14 | backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | @@ -563,6 +570,37 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo | M009-041, M009-044 |
| 15 | backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | @@ -602,7 +640,8 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo | M009-038 |
| 16 | backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | @@ -616,7 +655,10 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo | M009-013 |
| 17 | backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | @@ -691,7 +733,8 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo | M009-038 |
| 18 | backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | @@ -706,7 +749,10 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo | M009-013 |
| 19 | backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | @@ -1164,4 +1210,129 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo | M009-038, M009-013 |
| 20 | backend/src/main/java/com/complex/platform/governance/MerchantRecord.java | @@ -1,5 +1,5 @@ | M009-038 |
| 21 | backend/src/main/java/com/complex/platform/governance/MerchantRecord.java | @@ -31,8 +31,34 @@ public record MerchantRecord( | M009-038 |
| 22 | backend/src/main/java/com/complex/platform/governance/MerchantRecord.java | @@ -53,7 +79,7 @@ public record MerchantRecord( | M009-038 |
| 23 | backend/src/main/java/com/complex/platform/governance/MerchantRecord.java | @@ -66,7 +92,7 @@ public record MerchantRecord( | M009-038 |
| 24 | backend/src/main/java/com/complex/platform/governance/MerchantRecord.java | @@ -76,7 +102,7 @@ public record MerchantRecord( | M009-038 |
| 25 | backend/src/main/java/com/complex/platform/governance/MerchantRecord.java | @@ -85,6 +111,6 @@ public record MerchantRecord( | M009-038 |
| 26 | backend/src/main/java/com/complex/platform/governance/PlatformController.java | @@ -1,5 +1,5 @@ | M009-038 |
| 27 | backend/src/main/java/com/complex/platform/governance/PlatformController.java | @@ -11,6 +11,7 @@ import com.complex.platform.governance.dto.BusinessEntityBindingRequest; | M009-038 |
| 28 | backend/src/main/java/com/complex/platform/governance/PlatformController.java | @@ -84,6 +85,24 @@ public class PlatformController { | M009-038 |
| 29 | backend/src/main/java/com/complex/platform/governance/PlatformRepository.java | @@ -1,5 +1,5 @@ | M009-038, M009-041, M009-044 |
| 30 | backend/src/main/java/com/complex/platform/governance/PlatformRepository.java | @@ -43,10 +43,10 @@ public interface PlatformRepository { | M009-038 |
| 31 | backend/src/main/java/com/complex/platform/governance/PlatformRepository.java | @@ -100,6 +100,8 @@ public interface PlatformRepository { | M009-044 |
| 32 | backend/src/main/java/com/complex/platform/governance/PlatformRepository.java | @@ -109,6 +111,18 @@ public interface PlatformRepository { | M009-041 |
| 33 | backend/src/main/java/com/complex/platform/governance/PlatformRepository.java | @@ -142,4 +156,49 @@ public interface PlatformRepository { | M009-038 |
| 34 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -1,5 +1,5 @@ | M009-013, M009-018, M009-030, M009-031, M009-038, M009-039, M009-040, M009-041, M009-042, M009-043, M009-044, M009-045, M009-046, M009-047 |
| 35 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -14,6 +14,7 @@ import com.complex.platform.governance.dto.MerchantDeleteApprovalRequest; | M009-038 |
| 36 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -88,7 +89,7 @@ public class PlatformService { | M009-038 |
| 37 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -117,7 +118,8 @@ public class PlatformService { | M009-038 |
| 38 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -128,6 +130,8 @@ public class PlatformService { | M009-038 |
| 39 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -141,10 +145,9 @@ public class PlatformService { | M009-038, M009-047 |
| 40 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -255,7 +258,7 @@ public class PlatformService { | M009-038 |
| 41 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -300,12 +303,13 @@ public class PlatformService { | M009-038, M009-043 |
| 42 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -313,7 +317,7 @@ public class PlatformService { | M009-047 |
| 43 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -341,11 +345,89 @@ public class PlatformService { | M009-038 |
| 44 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -360,7 +442,7 @@ public class PlatformService { | M009-038 |
| 45 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -392,6 +474,9 @@ public class PlatformService { | M009-046 |
| 46 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -403,7 +488,10 @@ public class PlatformService { | M009-013 |
| 47 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -411,7 +499,9 @@ public class PlatformService { | M009-038, M009-039 |
| 48 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -419,7 +509,7 @@ public class PlatformService { | M009-038 |
| 49 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -500,8 +590,8 @@ public class PlatformService { | M009-042 |
| 50 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -512,6 +602,7 @@ public class PlatformService { | M009-044 |
| 51 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -527,12 +618,13 @@ public class PlatformService { | M009-043, M009-044 |
| 52 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -541,9 +633,10 @@ public class PlatformService { | M009-043, M009-044 |
| 53 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -565,12 +658,13 @@ public class PlatformService { | M009-043, M009-044 |
| 54 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -592,18 +686,22 @@ public class PlatformService { | M009-038, M009-039, M009-046 |
| 55 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -630,7 +728,7 @@ public class PlatformService { | M009-038 |
| 56 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -670,16 +768,16 @@ public class PlatformService { | M009-038, M009-040, M009-041 |
| 57 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -687,14 +785,16 @@ public class PlatformService { | M009-041 |
| 58 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -716,19 +816,33 @@ public class PlatformService { | M009-031, M009-038, M009-040, M009-041, M009-047 |
| 59 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -760,8 +874,7 @@ public class PlatformService { | M009-047 |
| 60 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -771,8 +884,7 @@ public class PlatformService { | M009-047 |
| 61 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -783,7 +895,7 @@ public class PlatformService { | M009-047 |
| 62 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -793,8 +905,7 @@ public class PlatformService { | M009-047 |
| 63 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -804,7 +915,7 @@ public class PlatformService { | M009-047 |
| 64 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -814,7 +925,7 @@ public class PlatformService { | M009-047 |
| 65 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -825,9 +936,30 @@ public class PlatformService { | M009-038, M009-040, M009-041 |
| 66 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -870,6 +1002,25 @@ public class PlatformService { | M009-038, M009-030, M009-018, M009-046 |
| 67 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -896,7 +1047,8 @@ public class PlatformService { | M009-038 |
| 68 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -921,10 +1073,38 @@ public class PlatformService { | M009-038, M009-043, M009-046 |
| 69 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | @@ -964,6 +1144,8 @@ public class PlatformService { | M009-043 |
| 70 | backend/src/main/java/com/complex/platform/governance/ProjectRecord.java | @@ -1,4 +1,5 @@ | M009-013 |
| 71 | backend/src/main/java/com/complex/platform/governance/ProjectRecord.java | @@ -12,6 +13,13 @@ public record ProjectRecord( | M009-013 |
| 72 | backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java | @@ -1,5 +1,5 @@ | M009-038 |
| 73 | backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java | @@ -12,6 +12,15 @@ public record BusinessEntityRequest( | M009-038 |
| 74 | backend/src/main/java/com/complex/platform/governance/dto/MerchantProfileRequest.java | @@ -1,4 +1,5 @@ | M009-038 |
| 75 | backend/src/main/java/com/complex/platform/governance/dto/MerchantProfileRequest.java | @@ -22,6 +23,19 @@ public record MerchantProfileRequest( | M009-038 |
| 76 | backend/src/main/java/com/complex/platform/governance/dto/ProjectAssociationRequest.java | @@ -0,0 +1,5 @@ | M009-038 |
| 77 | backend/src/main/java/com/complex/platform/governance/dto/ProjectRequest.java | @@ -1,4 +1,5 @@ | M009-013 |
| 78 | backend/src/main/java/com/complex/platform/governance/dto/ProjectRequest.java | @@ -12,6 +13,14 @@ public record ProjectRequest( | M009-013 |
| 79 | backend/src/main/java/com/complex/platform/leasing/LeasingService.java | @@ -1,5 +1,5 @@ | M009-038, M009-041 |
| 80 | backend/src/main/java/com/complex/platform/leasing/LeasingService.java | @@ -240,7 +240,7 @@ public class LeasingService { | M009-038, M009-041 |
| 81 | backend/src/main/java/com/complex/platform/leasing/LeasingService.java | @@ -269,7 +269,7 @@ public class LeasingService { | M009-038, M009-041 |
| 82 | backend/src/main/java/com/complex/platform/leasing/LeasingService.java | @@ -285,11 +285,11 @@ public class LeasingService { | M009-038, M009-041 |
| 83 | backend/src/main/resources/db/migration/V427__m009_project_profiles_and_associations.sql | @@ -0,0 +1,20 @@ | M009-013, M009-038 |
| 84 | backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java | @@ -1,11 +1,12 @@ | M009-038 |
| 85 | backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java | @@ -234,10 +235,37 @@ class AuthorizationServiceTests { | M009-038 |
| 86 | backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java | @@ -1,6 +1,6 @@ | M009-038 |
| 87 | backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java | @@ -338,11 +338,7 @@ class M006SecurityRegressionTests { | M009-038 |
| 88 | backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java | @@ -413,7 +409,7 @@ class M006SecurityRegressionTests { | M009-038 |
| 89 | backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java | @@ -592,7 +588,7 @@ class M006SecurityRegressionTests { | M009-038 |
| 90 | backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java | @@ -791,9 +787,7 @@ class M006SecurityRegressionTests { | M009-038 |
| 91 | backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java | @@ -1,6 +1,6 @@ | M009-038 |
| 92 | backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java | @@ -322,7 +322,7 @@ class M010RegressionTests { | M009-038 |
| 93 | backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java | @@ -331,7 +331,7 @@ class M010RegressionTests { | M009-038 |
| 94 | backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java | @@ -600,7 +600,7 @@ class M010RegressionTests { | M009-038 |
| 95 | backend/src/test/java/com/complex/platform/access/staff/StaffTestActors.java | @@ -1,6 +1,6 @@ | M009-038 |
| 96 | backend/src/test/java/com/complex/platform/access/staff/StaffTestActors.java | @@ -24,7 +24,7 @@ final class StaffTestActors { | M009-038 |
| 97 | backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java | @@ -1,6 +1,6 @@ | M009-038 |
| 98 | backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java | @@ -63,7 +63,7 @@ class BarStoredValueFundChainJdbcIntegrationTests { | M009-038 |
| 99 | backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java | @@ -1,6 +1,6 @@ | M009-038 |
| 100 | backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java | @@ -40,7 +40,7 @@ class StoredValueNotificationIntegrationTest { | M009-038 |
| 101 | backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java | @@ -1,5 +1,5 @@ | M009-038 |
| 102 | backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java | @@ -382,6 +382,52 @@ class PlatformControllerTests { | M009-038 |
| 103 | backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | @@ -1,5 +1,5 @@ | M009-013, M009-038, M009-041, M009-044, M009-045 |
| 104 | backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | @@ -30,6 +30,8 @@ class PlatformJdbcIntegrationTests { | M009-041 |
| 105 | backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | @@ -381,6 +383,232 @@ class PlatformJdbcIntegrationTests { | M009-013, M009-038, M009-041, M009-044, M009-045 |
| 106 | backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | @@ -1,5 +1,5 @@ | M009-013, M009-018, M009-030, M009-031, M009-038, M009-039, M009-040, M009-041, M009-042, M009-043, M009-044, M009-045, M009-046, M009-047 |
| 107 | backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | @@ -24,6 +24,7 @@ import com.complex.platform.governance.dto.MerchantDeleteRequest; | M009-038 |
| 108 | backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | @@ -64,6 +65,8 @@ class PlatformServiceTests { | M009-038 |
| 109 | backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | @@ -104,6 +107,7 @@ class PlatformServiceTests { | M009-030, M009-038 |
| 110 | backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | @@ -114,12 +118,13 @@ class PlatformServiceTests { | M009-030, M009-038 |
| 111 | backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | @@ -230,10 +235,10 @@ class PlatformServiceTests { | M009-039 |
| 112 | backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | @@ -247,7 +252,7 @@ class PlatformServiceTests { | M009-039 |
| 113 | backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | @@ -260,18 +265,18 @@ class PlatformServiceTests { | M009-039, M009-040, M009-041 |
| 114 | backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | @@ -422,7 +427,7 @@ class PlatformServiceTests { | M009-039 |
| 115 | backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | @@ -467,6 +472,7 @@ class PlatformServiceTests { | M009-030, M009-038 |
| 116 | backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | @@ -510,6 +516,7 @@ class PlatformServiceTests { | M009-038 |
| 117 | backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | @@ -543,6 +550,7 @@ class PlatformServiceTests { | M009-038 |
| 118 | backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | @@ -575,6 +583,7 @@ class PlatformServiceTests { | M009-038 |
| 119 | backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | @@ -675,7 +684,7 @@ class PlatformServiceTests { | M009-038 |
| 120 | backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | @@ -699,7 +708,7 @@ class PlatformServiceTests { | M009-040, M009-041 |
| 121 | backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | @@ -740,51 +749,27 @@ class PlatformServiceTests { | M009-040, M009-041 |
| 122 | backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | @@ -846,7 +831,7 @@ class PlatformServiceTests { | M009-039 |
| 123 | backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | @@ -854,7 +839,7 @@ class PlatformServiceTests { | M009-039 |
| 124 | backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | @@ -862,13 +847,442 @@ class PlatformServiceTests { | M009-013, M009-018, M009-030, M009-031, M009-038, M009-039, M009-040, M009-041, M009-042, M009-043, M009-044, M009-045, M009-046, M009-047 |
| 125 | backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java | @@ -1,4 +1,4 @@ | M009-038 |
| 126 | backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java | @@ -440,7 +440,7 @@ class SupervisorReviewServiceTests { | M009-038 |
| 127 | backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java | @@ -578,7 +578,7 @@ class SupervisorReviewServiceTests { | M009-038 |
| 128 | backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java | @@ -783,7 +783,7 @@ class SupervisorReviewServiceTests { | M009-038 |
| 129 | backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java | @@ -809,7 +809,7 @@ class SupervisorReviewServiceTests { | M009-038 |
| 130 | backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java | @@ -831,7 +831,7 @@ class SupervisorReviewServiceTests { | M009-038 |
| 131 | backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java | @@ -878,7 +878,7 @@ class SupervisorReviewServiceTests { | M009-038 |
| 132 | backend/src/test/java/com/complex/platform/leasing/LeasingServiceTests.java | @@ -1,4 +1,5 @@ | M009-038, M009-041 |
| 133 | backend/src/test/java/com/complex/platform/leasing/LeasingServiceTests.java | @@ -212,6 +213,7 @@ class LeasingServiceTests { | M009-038, M009-041 |
| 134 | backend/src/test/java/com/complex/platform/leasing/LeasingServiceTests.java | @@ -222,6 +224,11 @@ class LeasingServiceTests { | M009-038, M009-041 |
| 135 | backend/src/test/java/com/complex/platform/leasing/LeasingServiceTests.java | @@ -231,19 +238,22 @@ class LeasingServiceTests { | M009-038, M009-041 |
| 136 | backend/src/test/java/com/complex/platform/leasing/LeasingSignAtomicityJdbcIntegrationTests.java | @@ -1,5 +1,5 @@ | M009-038, M009-041 |
| 137 | backend/src/test/java/com/complex/platform/leasing/LeasingSignAtomicityJdbcIntegrationTests.java | @@ -36,6 +36,7 @@ class LeasingSignAtomicityJdbcIntegrationTests { | M009-038, M009-041 |
| 138 | backend/src/test/java/com/complex/platform/leasing/LeasingSignAtomicityJdbcIntegrationTests.java | @@ -51,12 +52,14 @@ class LeasingSignAtomicityJdbcIntegrationTests { | M009-038, M009-041 |
| 139 | backend/src/test/java/com/complex/platform/leasing/LeasingSignAtomicityJdbcIntegrationTests.java | @@ -103,10 +106,35 @@ class LeasingSignAtomicityJdbcIntegrationTests { | M009-038, M009-041 |
| 140 | backend/src/test/java/com/complex/platform/leasing/LeasingSignAtomicityJdbcIntegrationTests.java | @@ -114,27 +142,30 @@ class LeasingSignAtomicityJdbcIntegrationTests { | M009-038, M009-041 |

## 完整冻结git diff --binary before after
```diff
diff --git a/backend/src/main/java/com/complex/platform/access/AuthorizationService.java b/backend/src/main/java/com/complex/platform/access/AuthorizationService.java
index 34be1cca..4bfae6d2 100644
--- a/backend/src/main/java/com/complex/platform/access/AuthorizationService.java
+++ b/backend/src/main/java/com/complex/platform/access/AuthorizationService.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.08
+// 文件版本：V0.13
 
 import com.complex.platform.auth.AuthService;
 import com.complex.platform.common.api.ApiException;
@@ -137,7 +137,8 @@ public class AuthorizationService implements CallerScopeResolver {
     }
 
     public boolean isMasterAuthority(AccessActor actor) {
-        return isPlatformAdminBridge(actor)
+        return "active".equals(actor.status())
+                && SessionIssuer.MASTER_ACCOUNT_TYPE.equals(normalize(actor.accountType()))
                 && SessionIssuer.MASTER_USER_ID.equals(actor.id())
                 && SessionIssuer.MASTER_USERNAME.equals(actor.username());
     }
diff --git a/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java b/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java
index c8b989df..aa34c220 100644
--- a/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java
+++ b/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.02
+// 文件版本：V0.08
 import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
 import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;
 
@@ -83,14 +83,14 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                     settlement_entity_id, contact_name, contact_phone, owner_person, mall_block_code, area_square_meter,
                     leased_area_square_meter, secondary_contact_name, secondary_contact_phone,
                     rent_per_square_meter, cooperation_form, status, created_at, updated_at,
-                    archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason
+                    archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason, project_id
                 )
                 VALUES (
                     :id, :merchant_name, :merchant_type, :business_entity_id, :contract_entity_id,
                     :settlement_entity_id, :contact_name, :contact_phone, :owner_person, :mall_block_code, :area_square_meter,
                     :leased_area_square_meter, :secondary_contact_name, :secondary_contact_phone,
                     :rent_per_square_meter, :cooperation_form, :status, :created_at, :updated_at,
-                    :archived_at, :archived_by, :delete_status, :delete_requested_by, :delete_requested_at, :delete_reason
+                    :archived_at, :archived_by, :delete_status, :delete_requested_by, :delete_requested_at, :delete_reason, :project_id
                 )
                 ON CONFLICT (id) DO UPDATE SET
                     merchant_name = EXCLUDED.merchant_name,
@@ -127,7 +127,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                        settlement_entity_id, contact_name, contact_phone, owner_person, mall_block_code, area_square_meter,
                        leased_area_square_meter, secondary_contact_name, secondary_contact_phone,
                        rent_per_square_meter, cooperation_form, status, created_at, updated_at,
-                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason
+                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason, project_id
                   FROM merchants
                  ORDER BY created_at DESC, id
                  LIMIT 500
@@ -142,7 +142,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                        settlement_entity_id, contact_name, contact_phone, owner_person, mall_block_code, area_square_meter,
                        leased_area_square_meter, secondary_contact_name, secondary_contact_phone,
                        rent_per_square_meter, cooperation_form, status, created_at, updated_at,
-                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason
+                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason, project_id
                   FROM merchants
                  WHERE (:include_archived OR (archived_at IS NULL AND delete_status <> 'approved'))
                  ORDER BY created_at DESC, id
@@ -182,7 +182,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                        settlement_entity_id, contact_name, contact_phone, owner_person, mall_block_code, area_square_meter,
                        leased_area_square_meter, secondary_contact_name, secondary_contact_phone,
                        rent_per_square_meter, cooperation_form, status, created_at, updated_at,
-                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason
+                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason, project_id
                   FROM merchants
                  WHERE id = :id
                 """, Map.of("id", merchantId), this::mapMerchant).stream().findFirst();
@@ -218,8 +218,12 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                 continue;
             }
             // table 名来自 information_schema（系统元数据，非用户输入），无注入风险；merchant_id 走参数绑定。
+            String activeReference = "business_entity_bindings".equals(table)
+                    ? " AND NOT (status = 'inactive' AND effective_from IS NOT NULL AND effective_to IS NOT NULL"
+                            + " AND effective_to >= effective_from AND effective_to <= (now() AT TIME ZONE 'UTC')::date)"
+                    : "";
             Long c = jdbcTemplate.queryForObject(
-                    "SELECT COUNT(1) FROM " + table + " WHERE merchant_id = :id",
+                    "SELECT COUNT(1) FROM " + table + " WHERE merchant_id = :id" + activeReference,
                     Map.of("id", merchantId), Long.class);
             total += c == null ? 0L : c;
         }
@@ -227,31 +231,31 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
     }
 
     @Override
-    public List<MerchantRecord> listMerchantsByBusinessEntity(String businessEntityId) {
+    public List<MerchantRecord> listMerchantsByBusinessEntity(String projectId, String businessEntityId) {
         // 商户面 scoped 读通道：按 merchants.business_entity_id = token scope 过滤（AC-ISO-1，§4）。
-        if (businessEntityId == null) return listMerchants();
+        if (projectId == null || businessEntityId == null) return List.of();
         return jdbcTemplate.query("""
                 SELECT id, merchant_name, merchant_type, business_entity_id, contract_entity_id,
                        settlement_entity_id, contact_name, contact_phone, owner_person, mall_block_code, area_square_meter,
                        leased_area_square_meter, secondary_contact_name, secondary_contact_phone,
                        rent_per_square_meter, cooperation_form, status, created_at, updated_at,
-                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason
+                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason, project_id
                   FROM merchants
-                 WHERE business_entity_id = :business_entity_id
+                 WHERE project_id = :project_id AND business_entity_id = :business_entity_id
                    AND archived_at IS NULL AND delete_status <> 'approved'
                  ORDER BY created_at DESC, id
                  LIMIT 500
-                """, Map.of("business_entity_id", businessEntityId), this::mapMerchant);
+                """, Map.of("business_entity_id", businessEntityId, "project_id", projectId), this::mapMerchant);
     }
 
     @Override
     public ProjectRecord saveProject(ProjectRecord project) {
         jdbcTemplate.update("""
                 INSERT INTO projects (
-                    id, merchant_id, venue_type, name, code, floor, capacity, status, created_at, updated_at
+                    id, merchant_id, venue_type, name, code, floor, capacity, status, created_at, updated_at, owner_person, address, business_scope
                 )
                 VALUES (
-                    :id, :merchant_id, :venue_type, :name, :code, :floor, :capacity, :status, :created_at, :updated_at
+                    :id, :merchant_id, :venue_type, :name, :code, :floor, :capacity, :status, :created_at, :updated_at, :owner_person, :address, :business_scope
                 )
                 ON CONFLICT (id) DO UPDATE SET
                     merchant_id = EXCLUDED.merchant_id,
@@ -261,7 +265,10 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                     floor = EXCLUDED.floor,
                     capacity = EXCLUDED.capacity,
                     status = EXCLUDED.status,
-                    updated_at = EXCLUDED.updated_at
+                    updated_at = EXCLUDED.updated_at,
+                    owner_person = EXCLUDED.owner_person,
+                    address = EXCLUDED.address,
+                    business_scope = EXCLUDED.business_scope
                 """, projectParameters(project));
         return project;
     }
@@ -269,7 +276,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
     @Override
     public List<ProjectRecord> listProjects() {
         return jdbcTemplate.query("""
-                SELECT id, merchant_id, venue_type, name, code, floor, capacity, status, created_at, updated_at
+                SELECT id, merchant_id, venue_type, name, code, floor, capacity, status, created_at, updated_at, owner_person, address, business_scope
                   FROM projects
                  ORDER BY created_at DESC, id
                  LIMIT 500
@@ -467,7 +474,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                 + "contract_entity_id, settlement_entity_id, contact_name, contact_phone, owner_person, mall_block_code, "
                 + "area_square_meter, leased_area_square_meter, secondary_contact_name, secondary_contact_phone, "
                 + "rent_per_square_meter, cooperation_form, status, created_at, updated_at, archived_at, archived_by, "
-                + "delete_status, delete_requested_by, delete_requested_at, delete_reason FROM merchants" + where
+                + "delete_status, delete_requested_by, delete_requested_at, delete_reason, project_id FROM merchants" + where
                 + " ORDER BY created_at DESC, id LIMIT :limit OFFSET :offset", params, this::mapMerchant);
         return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
     }
@@ -480,7 +487,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
         Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM projects", params, Long.class);
         params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
         List<ProjectRecord> items = jdbcTemplate.query("""
-                SELECT id, merchant_id, venue_type, name, code, floor, capacity, status, created_at, updated_at
+                SELECT id, merchant_id, venue_type, name, code, floor, capacity, status, created_at, updated_at, owner_person, address, business_scope
                   FROM projects
                  ORDER BY created_at DESC, id
                  LIMIT :limit OFFSET :offset
@@ -563,6 +570,37 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
 
 
 
+    @Override
+    public void completeSpaceTypeDeleteRequest(String spaceTypeId, String status) {
+        jdbcTemplate.update("""
+                UPDATE space_type_delete_requests SET status = :status
+                 WHERE id = (
+                     SELECT id FROM space_type_delete_requests
+                      WHERE space_type_id = :id AND status = 'pending'
+                      ORDER BY requested_at DESC, id DESC LIMIT 1
+                 )
+                """, Map.of("id", spaceTypeId, "status", status));
+    }
+
+    @Override
+    public void lockMerchant(String merchantId) {
+        jdbcTemplate.query("SELECT id FROM merchants WHERE id = :id FOR UPDATE",
+                Map.of("id", merchantId), (rs, rowNum) -> rs.getString("id"));
+    }
+
+    @Override
+    public boolean hasUnfinishedBusinessEntityBinding(String merchantId, String excludedId) {
+        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
+                SELECT EXISTS (
+                    SELECT 1 FROM business_entity_bindings
+                     WHERE merchant_id = :merchant_id
+                       AND (:excluded_id IS NULL OR id::text <> :excluded_id)
+                       AND (status = 'active' OR (status = 'inactive' AND effective_to IS NULL))
+                )
+                """, new MapSqlParameterSource("merchant_id", merchantId)
+                .addValue("excluded_id", excludedId, java.sql.Types.VARCHAR), Boolean.class));
+    }
+
     private MapSqlParameterSource businessEntityParameters(BusinessEntityRecord businessEntity) {
         return new MapSqlParameterSource()
                 .addValue("id", businessEntity.id())
@@ -602,7 +640,8 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                 .addValue("delete_status", merchant.deleteStatus() == null ? "none" : merchant.deleteStatus())
                 .addValue("delete_requested_by", merchant.deleteRequestedBy())
                 .addValue("delete_requested_at", timestampWithTimeZone(merchant.deleteRequestedAt()))
-                .addValue("delete_reason", merchant.deleteReason());
+                .addValue("delete_reason", merchant.deleteReason())
+                .addValue("project_id", merchant.projectId());
     }
 
     private MapSqlParameterSource projectParameters(ProjectRecord project) {
@@ -616,7 +655,10 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                 .addValue("capacity", project.capacity())
                 .addValue("status", project.status())
                 .addValue("created_at", timestampWithTimeZone(project.createdAt()))
-                .addValue("updated_at", timestampWithTimeZone(project.updatedAt()));
+                .addValue("updated_at", timestampWithTimeZone(project.updatedAt()))
+                .addValue("owner_person", project.ownerPerson())
+                .addValue("address", project.address())
+                .addValue("business_scope", project.businessScope());
     }
 
     private MapSqlParameterSource spaceParameters(SpaceRecord space) {
@@ -691,7 +733,8 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                 resultSet.getString("delete_status"),
                 resultSet.getString("delete_requested_by"),
                 nullableInstant(resultSet, "delete_requested_at"),
-                resultSet.getString("delete_reason")
+                resultSet.getString("delete_reason"),
+                resultSet.getString("project_id")
         );
     }
 
@@ -706,7 +749,10 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                 integerValue(resultSet, "capacity"),
                 resultSet.getString("status"),
                 nullableInstant(resultSet, "created_at"),
-                nullableInstant(resultSet, "updated_at")
+                nullableInstant(resultSet, "updated_at"),
+                resultSet.getString("owner_person"),
+                resultSet.getString("address"),
+                resultSet.getString("business_scope")
         );
     }
 
@@ -1164,4 +1210,129 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
         jdbcTemplate.update("UPDATE merchants SET archive_reason = :reason WHERE id = :id",
                 new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("reason", reason).addValue("id", id));
     }
+
+    @Override
+    public Optional<BusinessEntityRecord> findBusinessEntityById(String entityId) {
+        return jdbcTemplate.query("""
+                SELECT id, entity_name, entity_role, unified_social_credit_code, legal_representative,
+                       bank_account_ref, status, created_at, updated_at
+                  FROM business_entities WHERE id = :id
+                """, Map.of("id", entityId), this::mapBusinessEntity).stream().findFirst();
+    }
+
+    @Override
+    public boolean businessEntityInProject(String businessEntityId, String projectId) {
+        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
+                SELECT EXISTS (SELECT 1 FROM business_entity_projects
+                    WHERE business_entity_id = :entity_id AND project_id = :project_id)
+                """, new MapSqlParameterSource().addValue("entity_id", businessEntityId)
+                .addValue("project_id", projectId, java.sql.Types.VARCHAR), Boolean.class));
+    }
+
+    @Override
+    public void associateBusinessEntityProject(String businessEntityId, String projectId) {
+        jdbcTemplate.update("""
+                INSERT INTO business_entity_projects (business_entity_id, project_id)
+                VALUES (:entity_id, :project_id) ON CONFLICT DO NOTHING
+                """, Map.of("entity_id", businessEntityId, "project_id", projectId));
+    }
+
+    @Override
+    public boolean assignMerchantProject(String merchantId, String projectId) {
+        return jdbcTemplate.update("""
+                UPDATE merchants SET project_id = :project_id
+                 WHERE id = :id AND (project_id IS NULL OR project_id = :project_id)
+                """, Map.of("id", merchantId, "project_id", projectId)) == 1;
+    }
+
+    @Override
+    public com.complex.platform.common.api.PageResponse<BusinessEntityRecord> listBusinessEntitiesPaged(String projectId,
+            int page, int size) {
+        int safePage = page < 1 ? 1 : page;
+        int safeSize = size < 1 ? 500 : Math.min(size, 500);
+        MapSqlParameterSource params = new MapSqlParameterSource().addValue("project_id", projectId, java.sql.Types.VARCHAR);
+        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM business_entities WHERE EXISTS (SELECT 1 FROM business_entity_projects ep WHERE ep.business_entity_id = business_entities.id AND ep.project_id = :project_id)", params, Long.class);
+        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
+        List<BusinessEntityRecord> items = jdbcTemplate.query("""
+                SELECT id, entity_name, entity_role, unified_social_credit_code, legal_representative,
+                       bank_account_ref, status, created_at, updated_at
+                  FROM business_entities
+                 WHERE EXISTS (SELECT 1 FROM business_entity_projects ep WHERE ep.business_entity_id = business_entities.id AND ep.project_id = :project_id)
+                 ORDER BY created_at DESC, id
+                 LIMIT :limit OFFSET :offset
+                """, params, this::mapBusinessEntity);
+        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
+    }
+    @Override
+    public com.complex.platform.common.api.PageResponse<MerchantRecord> listMerchantsPaged(String projectId,
+            boolean includeArchived, String businessFormat, int page, int size) {
+        int safePage = page < 1 ? 1 : page;
+        int safeSize = size < 1 ? 500 : Math.min(size, 500);
+        MapSqlParameterSource params = new MapSqlParameterSource().addValue("project_id", projectId, java.sql.Types.VARCHAR).addValue("include_archived", includeArchived)
+                .addValue("business_format", businessFormat);
+        // V348 按业态查询：business_format 为空＝不筛（走 idx_merchants_business_format）。
+        String where = " WHERE project_id = :project_id AND (:include_archived OR (archived_at IS NULL AND delete_status <> 'approved'))"
+                + " AND (CAST(:business_format AS VARCHAR) IS NULL OR merchant_type = :business_format)";
+        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM merchants" + where, params, Long.class);
+        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
+        List<MerchantRecord> items = jdbcTemplate.query("SELECT id, merchant_name, merchant_type, business_entity_id, "
+                + "contract_entity_id, settlement_entity_id, contact_name, contact_phone, owner_person, mall_block_code, "
+                + "area_square_meter, leased_area_square_meter, secondary_contact_name, secondary_contact_phone, "
+                + "rent_per_square_meter, cooperation_form, status, created_at, updated_at, archived_at, archived_by, "
+                + "delete_status, delete_requested_by, delete_requested_at, delete_reason, project_id FROM merchants" + where
+                + " ORDER BY created_at DESC, id LIMIT :limit OFFSET :offset", params, this::mapMerchant);
+        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
+    }
+    @Override
+    public com.complex.platform.common.api.PageResponse<ProjectRecord> listProjectsPaged(String projectId, int page, int size) {
+        int safePage = page < 1 ? 1 : page;
+        int safeSize = size < 1 ? 500 : Math.min(size, 500);
+        MapSqlParameterSource params = new MapSqlParameterSource().addValue("project_id", projectId, java.sql.Types.VARCHAR);
+        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM projects WHERE id = :project_id", params, Long.class);
+        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
+        List<ProjectRecord> items = jdbcTemplate.query("""
+                SELECT id, merchant_id, venue_type, name, code, floor, capacity, status, created_at, updated_at, owner_person, address, business_scope
+                  FROM projects
+                 WHERE id = :project_id
+                 ORDER BY created_at DESC, id
+                 LIMIT :limit OFFSET :offset
+                """, params, this::mapProject);
+        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
+    }
+    @Override
+    public com.complex.platform.common.api.PageResponse<SpaceRecord> listSpacesPaged(String projectId, int page, int size) {
+        int safePage = page < 1 ? 1 : page;
+        int safeSize = size < 1 ? 500 : Math.min(size, 500);
+        MapSqlParameterSource params = new MapSqlParameterSource().addValue("project_id", projectId, java.sql.Types.VARCHAR);
+        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM spaces WHERE project_id = :project_id", params, Long.class);
+        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
+        List<SpaceRecord> items = jdbcTemplate.query("""
+                SELECT id, project_id, merchant_id, parent_space_id, space_name, space_type, floor_no,
+                       area_square_meter, polygon_points::text AS polygon_points, status, created_at, updated_at
+                  FROM spaces
+                 WHERE project_id = :project_id
+                 ORDER BY created_at DESC, id
+                 LIMIT :limit OFFSET :offset
+                """, params, this::mapSpace);
+        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
+    }
+    @Override
+    public com.complex.platform.common.api.PageResponse<BusinessEntityBindingRecord> listBusinessEntityBindingsPaged(String projectId,
+            int page, int size) {
+        int safePage = page < 1 ? 1 : page;
+        int safeSize = size < 1 ? 500 : Math.min(size, 500);
+        MapSqlParameterSource params = new MapSqlParameterSource().addValue("project_id", projectId, java.sql.Types.VARCHAR);
+        Long total = jdbcTemplate.queryForObject(
+                "SELECT COUNT(*) FROM business_entity_bindings WHERE merchant_id IS NOT NULL AND EXISTS (SELECT 1 FROM merchants m WHERE m.id = business_entity_bindings.merchant_id AND m.project_id = :project_id)", params, Long.class);
+        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
+        List<BusinessEntityBindingRecord> items = jdbcTemplate.query("""
+                SELECT id::text AS id, merchant_id, operation_entity_id, contract_entity_id, settlement_entity_id,
+                       status, effective_from, effective_to, change_reason, created_at, updated_at
+                  FROM business_entity_bindings
+                 WHERE merchant_id IS NOT NULL AND EXISTS (SELECT 1 FROM merchants m WHERE m.id = business_entity_bindings.merchant_id AND m.project_id = :project_id)
+                 ORDER BY created_at DESC, id
+                 LIMIT :limit OFFSET :offset
+                """, params, this::mapBusinessEntityBinding);
+        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
+    }
 }
diff --git a/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java b/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java
index c2deb1fe..c14ca2d9 100644
--- a/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java
+++ b/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.02
+// 文件版本：V0.04
 
 import java.math.BigDecimal;
 import java.time.Instant;
@@ -31,8 +31,34 @@ public record MerchantRecord(
         String deleteStatus,
         String deleteRequestedBy,
         Instant deleteRequestedAt,
-        String deleteReason
+        String deleteReason,
+        String projectId
 ) {
+    public MerchantRecord(
+            String id, String merchantName, String merchantType, String businessEntityId, String contractEntityId,
+            String settlementEntityId, String contactName, String contactPhone, String ownerPerson,
+            String mallBlockCode, BigDecimal areaSquareMeter, BigDecimal leasedAreaSquareMeter,
+            String secondaryContactName, String secondaryContactPhone, BigDecimal rentPerSquareMeter,
+            String cooperationForm, String status, Instant createdAt, Instant updatedAt, Instant archivedAt,
+            String archivedBy, String deleteStatus, String deleteRequestedBy, Instant deleteRequestedAt,
+            String deleteReason) {
+        this(
+                id, merchantName, merchantType, businessEntityId, contractEntityId, settlementEntityId,
+                contactName, contactPhone, ownerPerson, mallBlockCode, areaSquareMeter, leasedAreaSquareMeter,
+                secondaryContactName, secondaryContactPhone, rentPerSquareMeter, cooperationForm, status,
+                createdAt, updatedAt, archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt,
+                deleteReason, null);
+    }
+
+    public MerchantRecord withProject(String projectId) {
+        return new MerchantRecord(
+                id, merchantName, merchantType, businessEntityId, contractEntityId, settlementEntityId,
+                contactName, contactPhone, ownerPerson, mallBlockCode, areaSquareMeter, leasedAreaSquareMeter,
+                secondaryContactName, secondaryContactPhone, rentPerSquareMeter, cooperationForm, status,
+                createdAt, updatedAt, archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt,
+                deleteReason, projectId);
+    }
+
     // ponytail: V260 前的 19 参构造点(既有代码/测试)不带存档/删除字段,默认未存档 + delete_status=none。
     //           避免为 6 个新字段改所有既有构造点;日后语义要变则删本构造让编译逼出所有点。
     public MerchantRecord(
@@ -53,7 +79,7 @@ public record MerchantRecord(
                 settlementEntityId, contactName, contactPhone, ownerPerson, mallBlockCode, areaSquareMeter,
                 leasedAreaSquareMeter, secondaryContactName, secondaryContactPhone, rentPerSquareMeter, cooperationForm,
                 status, createdAt, updatedAt,
-                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason);
+                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, projectId);
     }
 
     /**
@@ -66,7 +92,7 @@ public record MerchantRecord(
                 ownerPerson, mallBlockCode, areaSquareMeter, leasedAreaSquareMeter, secondaryContactName,
                 com.complex.platform.common.PhoneMask.mask(secondaryContactPhone), rentPerSquareMeter,
                 cooperationForm, status, createdAt, updatedAt,
-                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason);
+                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, projectId);
     }
 
     /** M009-008：三个主体列以生效的四类主体绑定为准（PGM-026），绑定激活时由 PlatformService 同步投影。 */
@@ -76,7 +102,7 @@ public record MerchantRecord(
                 settlementEntityId, contactName, contactPhone, ownerPerson, mallBlockCode, areaSquareMeter,
                 leasedAreaSquareMeter, secondaryContactName, secondaryContactPhone, rentPerSquareMeter, cooperationForm,
                 status, createdAt, updatedAt,
-                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason);
+                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, projectId);
     }
 
     public MerchantRecord withDelete(String deleteStatus, String deleteRequestedBy, Instant deleteRequestedAt,
@@ -85,6 +111,6 @@ public record MerchantRecord(
                 settlementEntityId, contactName, contactPhone, ownerPerson, mallBlockCode, areaSquareMeter,
                 leasedAreaSquareMeter, secondaryContactName, secondaryContactPhone, rentPerSquareMeter, cooperationForm,
                 status, createdAt, updatedAt,
-                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason);
+                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, projectId);
     }
 }
diff --git a/backend/src/main/java/com/complex/platform/governance/PlatformController.java b/backend/src/main/java/com/complex/platform/governance/PlatformController.java
index ca47080c..52d3f0d2 100644
--- a/backend/src/main/java/com/complex/platform/governance/PlatformController.java
+++ b/backend/src/main/java/com/complex/platform/governance/PlatformController.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.01
+// 文件版本：V0.02
 import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;
 
 import com.complex.platform.access.AuthorizationService;
@@ -11,6 +11,7 @@ import com.complex.platform.governance.dto.BusinessEntityBindingRequest;
 import com.complex.platform.governance.dto.BusinessEntityRequest;
 import com.complex.platform.governance.dto.MerchantProfileRequest;
 import com.complex.platform.governance.dto.ProjectRequest;
+import com.complex.platform.governance.dto.ProjectAssociationRequest;
 import com.complex.platform.governance.dto.SpaceRequest;
 import com.complex.platform.governance.dto.StatusChangeRequest;
 import org.springframework.web.bind.annotation.GetMapping;
@@ -84,6 +85,24 @@ public class PlatformController {
         return ApiResponse.ok(maskPhonesForUnauthorized(service.updateMerchant(merchantId, request)));
     }
 
+    @PutMapping("/merchants/{merchant_id}/project")
+    public ApiResponse<MerchantRecord> assignMerchantProject(
+            @PathVariable("merchant_id") String merchantId,
+            @RequestBody @jakarta.validation.Valid ProjectAssociationRequest request) {
+        authorizationService.requirePermission("merchant.profile.manage");
+        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
+        return ApiResponse.ok(maskPhonesForUnauthorized(service.assignMerchantProject(merchantId, request)));
+    }
+
+    @PostMapping("/business-entities/{entity_id}/projects")
+    public ApiResponse<BusinessEntityRecord> associateBusinessEntityProject(
+            @PathVariable("entity_id") String entityId,
+            @RequestBody @jakarta.validation.Valid ProjectAssociationRequest request) {
+        authorizationService.requirePermission("entity.binding.manage");
+        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
+        return ApiResponse.ok(service.associateBusinessEntityProject(entityId, request));
+    }
+
     // ================= V260 商户存档与审批后删除范式 =================
 
     @PostMapping("/merchants/{merchant_id}/archive")
diff --git a/backend/src/main/java/com/complex/platform/governance/PlatformRepository.java b/backend/src/main/java/com/complex/platform/governance/PlatformRepository.java
index 95f31cbb..b313cd2d 100644
--- a/backend/src/main/java/com/complex/platform/governance/PlatformRepository.java
+++ b/backend/src/main/java/com/complex/platform/governance/PlatformRepository.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.02
+// 文件版本：V0.06
 
 import java.util.List;
 import java.util.Optional;
@@ -43,10 +43,10 @@ public interface PlatformRepository {
      * 勿删:本方法零调用是常态(多综合体隔离通道,建成待激活),但 verify:tenant-isolation 门禁
      * 以「该表存在 scoped 查询」为隔离能力判据,删除会导致门禁 fail-closed 阻断部署。
      */
-    default List<MerchantRecord> listMerchantsByBusinessEntity(String businessEntityId) {
-        if (businessEntityId == null) return listMerchants();
+    default List<MerchantRecord> listMerchantsByBusinessEntity(String projectId, String businessEntityId) {
+        if (projectId == null || businessEntityId == null) return List.of();
         return listMerchants().stream()
-                .filter(m -> businessEntityId.equals(m.businessEntityId()))
+                .filter(m -> projectId.equals(m.projectId()) && businessEntityId.equals(m.businessEntityId()))
                 .toList();
     }
 
@@ -100,6 +100,8 @@ public interface PlatformRepository {
 
     String findSpaceTypeDeleteRequestedBy(String spaceTypeId);
 
+    void completeSpaceTypeDeleteRequest(String spaceTypeId, String status);
+
 
     BusinessEntityBindingRecord saveBusinessEntityBinding(BusinessEntityBindingRecord binding);
 
@@ -109,6 +111,18 @@ public interface PlatformRepository {
 
     Optional<BusinessEntityBindingRecord> findActiveBusinessEntityBindingByMerchantId(String merchantId);
 
+    void lockMerchant(String merchantId);
+
+    default boolean hasUnfinishedBusinessEntityBinding(String merchantId, String excludedId) {
+        return listBusinessEntityBindings().stream()
+                .filter(binding -> merchantId.equals(binding.merchantId()))
+                .filter(binding -> !java.util.Objects.equals(excludedId, binding.id()))
+                .anyMatch(binding -> "active".equals(binding.status())
+                        || ("inactive".equals(binding.status()) && binding.effectiveTo() == null));
+    }
+
+
+
 
 
 
@@ -142,4 +156,49 @@ public interface PlatformRepository {
             int page, int size) {
         return com.complex.platform.common.api.PageResponse.of(listBusinessEntityBindings(), page, size);
     }
+
+    default Optional<BusinessEntityRecord> findBusinessEntityById(String entityId) {
+        return listBusinessEntities().stream().filter(entity -> entityId.equals(entity.id())).findFirst();
+    }
+
+    boolean businessEntityInProject(String businessEntityId, String projectId);
+
+    void associateBusinessEntityProject(String businessEntityId, String projectId);
+
+    boolean assignMerchantProject(String merchantId, String projectId);
+
+    default com.complex.platform.common.api.PageResponse<BusinessEntityRecord> listBusinessEntitiesPaged(
+            String projectId, int page, int size) {
+        return com.complex.platform.common.api.PageResponse.of(listBusinessEntities().stream()
+                .filter(entity -> projectId != null && businessEntityInProject(entity.id(), projectId))
+                .toList(), page, size);
+    }
+
+    default com.complex.platform.common.api.PageResponse<MerchantRecord> listMerchantsPaged(
+            String projectId, boolean includeArchived, String businessFormat, int page, int size) {
+        return com.complex.platform.common.api.PageResponse.of(listMerchants(includeArchived).stream()
+                .filter(merchant -> projectId != null && projectId.equals(merchant.projectId()))
+                .filter(merchant -> businessFormat == null || businessFormat.equals(merchant.merchantType()))
+                .toList(), page, size);
+    }
+
+    default com.complex.platform.common.api.PageResponse<ProjectRecord> listProjectsPaged(
+            String projectId, int page, int size) {
+        return com.complex.platform.common.api.PageResponse.of(listProjects().stream()
+                .filter(project -> projectId != null && projectId.equals(project.id())).toList(), page, size);
+    }
+
+    default com.complex.platform.common.api.PageResponse<SpaceRecord> listSpacesPaged(
+            String projectId, int page, int size) {
+        return com.complex.platform.common.api.PageResponse.of(listSpaces().stream()
+                .filter(space -> projectId != null && projectId.equals(space.projectId())).toList(), page, size);
+    }
+
+    default com.complex.platform.common.api.PageResponse<BusinessEntityBindingRecord> listBusinessEntityBindingsPaged(
+            String projectId, int page, int size) {
+        return com.complex.platform.common.api.PageResponse.of(listBusinessEntityBindings().stream()
+                .filter(binding -> projectId != null && findMerchantById(binding.merchantId())
+                        .filter(merchant -> projectId.equals(merchant.projectId())).isPresent())
+                .toList(), page, size);
+    }
 }
diff --git a/backend/src/main/java/com/complex/platform/governance/PlatformService.java b/backend/src/main/java/com/complex/platform/governance/PlatformService.java
index 3acc3e9e..b325adc0 100644
--- a/backend/src/main/java/com/complex/platform/governance/PlatformService.java
+++ b/backend/src/main/java/com/complex/platform/governance/PlatformService.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.03
+// 文件版本：V0.11
 
 import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
@@ -14,6 +14,7 @@ import com.complex.platform.governance.dto.MerchantDeleteApprovalRequest;
 import com.complex.platform.governance.dto.MerchantDeleteRequest;
 import com.complex.platform.governance.dto.MerchantArchiveRequest;
 import com.complex.platform.governance.dto.ProjectRequest;
+import com.complex.platform.governance.dto.ProjectAssociationRequest;
 import com.complex.platform.governance.dto.SpaceRequest;
 import com.complex.platform.governance.dto.StatusChangeRequest;
 import java.time.Instant;
@@ -88,7 +89,7 @@ public class PlatformService {
 
     // AUD-005 真分页: SQL COUNT + LIMIT/OFFSET, total 取真值; 默认 500 保留既有前端一次拉取(下拉源)契约。
     public PageResponse<BusinessEntityRecord> listBusinessEntities(int page, int size) {
-        PageResponse<BusinessEntityRecord> result = repository.listBusinessEntitiesPaged(page, size);
+        PageResponse<BusinessEntityRecord> result = repository.listBusinessEntitiesPaged(currentProject(), page, size);
         auditEventService.recordSuccess(
                 "entity_binding.viewed",
                 "entity.binding.view",
@@ -117,7 +118,8 @@ public class PlatformService {
                     String entityRole = requireBusinessEntityRole(request == null ? null : request.entityRole());
                     String status = requireSubjectStatus(request.status());
                     Instant now = Instant.now();
-                    return repository.saveBusinessEntity(new BusinessEntityRecord(
+                    String projectId = creationProject(request.projectId(), true);
+                    BusinessEntityRecord saved = repository.saveBusinessEntity(new BusinessEntityRecord(
                             "be_" + UUID.randomUUID(),
                             entityName,
                             entityRole,
@@ -128,6 +130,8 @@ public class PlatformService {
                             now,
                             now
                     ));
+                    repository.associateBusinessEntityProject(saved.id(), projectId);
+                    return saved;
                 }
         );
     }
@@ -141,10 +145,9 @@ public class PlatformService {
         String format = trimToNull(businessFormat);
         if (format != null && !MERCHANT_BUSINESS_FORMATS.contains(format)) {
             throw ApiException.badRequest("MERCHANT_BUSINESS_FORMAT_INVALID",
-                    "商户业态必须为 酒吧/餐饮/零售/游乐/传媒/其他 之一"
-                            + "(bar/catering/retail/amusement/media/other)");
+                    "商户业态必须为 酒吧/餐饮/零售/游乐/传媒/其他 之一");
         }
-        PageResponse<MerchantRecord> result = repository.listMerchantsPaged(includeArchived, format, page, size);
+        PageResponse<MerchantRecord> result = repository.listMerchantsPaged(currentProject(), includeArchived, format, page, size);
         auditEventService.recordSuccess(
                 "merchant.viewed",
                 "merchant.profile.view",
@@ -255,7 +258,7 @@ public class PlatformService {
                 "merchant.profile.manage",
                 "merchant",
                 () -> {
-                    MerchantRecord merchant = requireMerchant(id);
+                    MerchantRecord merchant = isMasterAuthority() ? findMerchant(id) : requireMerchant(id);
                     if (merchant.archivedAt() == null) {
                         throw ApiException.badRequest("MERCHANT_NOT_ARCHIVED",
                                 "商户必须先存档才能申请删除");
@@ -300,12 +303,13 @@ public class PlatformService {
                 request == null ? null : request.idempotencyKey(),
                 request,
                 MerchantDeleteDecision.class,
-                "merchant.delete_approved",
+                request != null && "rejected".equalsIgnoreCase(request.decision() == null ? "" : request.decision().trim())
+                        ? "merchant.delete_rejected" : "merchant.delete_approved",
                 "merchant.delete_approve_failed",
                 "merchant.profile.approve",
                 "merchant",
                 () -> {
-                    MerchantRecord merchant = requireMerchant(id);
+                    MerchantRecord merchant = isMasterAuthority() ? findMerchant(id) : requireMerchant(id);
                     if (!"pending".equals(merchant.deleteStatus())) {
                         throw ApiException.badRequest("MERCHANT_DELETE_NOT_PENDING",
                                 "该商户没有待审批的删除申请");
@@ -313,7 +317,7 @@ public class PlatformService {
                     String decision = request == null || request.decision() == null
                             ? null : request.decision().trim().toLowerCase(Locale.ROOT);
                     if (!"approved".equals(decision) && !"rejected".equals(decision)) {
-                        throw ApiException.badRequest("VALIDATION_FAILED", "审批结论必须为通过（approved）或驳回（rejected）");
+                        throw ApiException.badRequest("VALIDATION_FAILED", "审批结论必须为通过或驳回");
                     }
                     if (!isMasterAuthority()) {
                         throw ApiException.forbidden("MERCHANT_DELETE_APPROVER_NOT_MASTER", "商户删除只能由最高权限审批通过");
@@ -341,11 +345,89 @@ public class PlatformService {
         );
     }
 
-    private MerchantRecord requireMerchant(String id) {
+    public MerchantRecord assignMerchantProject(String merchantId, ProjectAssociationRequest request) {
+        requireProjectConfigurationAuthority();
+        String id = requireText(merchantId, "merchant_id");
+        return executeWrite("assignMerchantProject:" + id, request == null ? null : request.idempotencyKey(),
+                request, MerchantRecord.class, "merchant.project_assigned", "merchant.project_assign_failed",
+                "merchant.profile.manage", "merchant", () -> {
+                    String projectId = associationProject(request);
+                    findMerchant(id);
+                    if (!repository.assignMerchantProject(id, projectId)) {
+                        throw ApiException.conflict("MERCHANT_PROJECT_ALREADY_ASSIGNED", "商户已有明确项目归属，不能迁移");
+                    }
+                    return findMerchant(id);
+                });
+    }
+
+    public BusinessEntityRecord associateBusinessEntityProject(String entityId, ProjectAssociationRequest request) {
+        requireProjectConfigurationAuthority();
+        String id = requireText(entityId, "business_entity_id");
+        return executeWrite("associateBusinessEntityProject:" + id, request == null ? null : request.idempotencyKey(),
+                request, BusinessEntityRecord.class, "business_entity.project_associated", "business_entity.project_associate_failed",
+                "entity.binding.manage", "business_entity", () -> {
+                    String projectId = associationProject(request);
+                    BusinessEntityRecord entity = repository.findBusinessEntityById(id)
+                            .orElseThrow(() -> ApiException.notFound("BUSINESS_ENTITY_NOT_FOUND", "主体不存在"));
+                    repository.associateBusinessEntityProject(id, projectId);
+                    return entity;
+                });
+    }
+
+    private void requireProjectConfigurationAuthority() {
+        authorizationService.requirePlatformScope();
+        if (!isMasterAuthority()) {
+            throw ApiException.forbidden("MASTER_AUTHORITY_REQUIRED", "项目归属配置须由持授权密钥的最高权限执行");
+        }
+    }
+
+    private String associationProject(ProjectAssociationRequest request) {
+        ProjectAssociationRequest actual = requireRequest(request);
+        String projectId = requireText(actual.projectId(), "project_id");
+        requireText(actual.reason(), "reason");
+        if (!repository.projectExists(projectId)) {
+            throw ApiException.notFound("PROJECT_NOT_FOUND", "项目不存在");
+        }
+        return projectId;
+    }
+
+    private MerchantRecord findMerchant(String id) {
         return repository.findMerchantById(id)
                 .orElseThrow(() -> ApiException.notFound("MERCHANT_NOT_FOUND", "商户不存在或已被删除"));
     }
 
+    private MerchantRecord requireMerchant(String id) {
+        MerchantRecord merchant = findMerchant(id);
+        requireCurrentProject(merchant.projectId());
+        return merchant;
+    }
+
+    private String currentProject() {
+        authorizationService.requirePlatformScope();
+        String scope = authorizationService.currentProjectScopeQuietly().orElseThrow();
+        return repository.projectExists(scope) ? scope : null;
+    }
+
+    private String requireCurrentProject(String projectId) {
+        String current = currentProject();
+        if (current == null || !current.equals(projectId)) {
+            throw ApiException.forbidden("PROJECT_SCOPE_MISMATCH", "无权访问该项目的数据");
+        }
+        return current;
+    }
+
+    private String creationProject(String requested, boolean allowMasterConfiguration) {
+        String explicit = trimToNull(requested);
+        if (allowMasterConfiguration && isMasterAuthority() && explicit != null) {
+            if (!repository.projectExists(explicit)) {
+                throw ApiException.notFound("PROJECT_NOT_FOUND", "项目不存在");
+            }
+            return explicit;
+        }
+        String current = currentProject();
+        return requireCurrentProject(explicit == null ? current : explicit);
+    }
+
     /** M009-028：BSM-004「下线就不能再用」——新项目/空间/绑定只能引用未存档且未审批删除的商户；档案更新、存档/恢复与删除流程仍经 requireMerchant 命中已存档行。 */
     private MerchantRecord requireActiveMerchant(String id) {
         MerchantRecord merchant = requireMerchant(id);
@@ -360,7 +442,7 @@ public class PlatformService {
     }
 
     public PageResponse<ProjectRecord> listProjects(int page, int size) {
-        PageResponse<ProjectRecord> result = repository.listProjectsPaged(page, size);
+        PageResponse<ProjectRecord> result = repository.listProjectsPaged(currentProject(), page, size);
         auditEventService.recordSuccess(
                 "project.viewed",
                 "governance.project.view",
@@ -392,6 +474,9 @@ public class PlatformService {
                         requireActiveMerchant(merchantId);
                     }
                     String status = requireProjectStatus(request.status());
+                    if (request.capacity() != null && request.capacity() < 0) {
+                        throw ApiException.badRequest("VALIDATION_FAILED", "容量不得为负数");
+                    }
                     Instant now = Instant.now();
                     return repository.saveProject(new ProjectRecord(
                             "prj_" + UUID.randomUUID(),
@@ -403,7 +488,10 @@ public class PlatformService {
                             request.capacity(),
                             status,
                             now,
-                            now
+                            now,
+                            trimToNull(request.ownerPerson()),
+                            trimToNull(request.address()),
+                            trimToNull(request.businessScope())
                     ));
                 }
         );
@@ -411,7 +499,9 @@ public class PlatformService {
 
     /** 3.13：空间存在性（招商线索/意向意向空间校验用）。 */
     public boolean spaceExists(String spaceId) {
-        return spaceId != null && repository.findSpaceById(spaceId).isPresent();
+        String projectId = currentProject();
+        return spaceId != null && projectId != null && repository.findSpaceById(spaceId)
+                .filter(space -> projectId.equals(space.projectId())).isPresent();
     }
 
     public PageResponse<SpaceRecord> listSpaces() {
@@ -419,7 +509,7 @@ public class PlatformService {
     }
 
     public PageResponse<SpaceRecord> listSpaces(int page, int size) {
-        PageResponse<SpaceRecord> result = repository.listSpacesPaged(page, size);
+        PageResponse<SpaceRecord> result = repository.listSpacesPaged(currentProject(), page, size);
         auditEventService.recordSuccess(
                 "space.viewed",
                 "governance.space.view",
@@ -500,8 +590,8 @@ public class PlatformService {
         authorizationService.requirePlatformScope();
         SpaceTypeRecord existing = repository.findSpaceTypeById(id)
                 .orElseThrow(() -> ApiException.notFound("SPACE_TYPE_NOT_FOUND", "空间种类不存在或已被删除"));
-        if (!"archived".equals(existing.status())) {
-            throw ApiException.badRequest("SPACE_TYPE_NOT_ARCHIVED", "只有已存档的种类可以取消存档");
+        if (!Set.of("archived", "disabled").contains(existing.status())) {
+            throw ApiException.badRequest("SPACE_TYPE_NOT_ARCHIVED", "只有已存档或已删除的种类可以恢复");
         }
         Instant now = Instant.now();
         repository.saveSpaceType(new SpaceTypeRecord(
@@ -512,6 +602,7 @@ public class PlatformService {
                 Map.of("type_code", existing.typeCode()));
     }
 
+    @org.springframework.transaction.annotation.Transactional
     public void requestDeleteSpaceType(String id, String reason) {
         authorizationService.requirePlatformScope();
         SpaceTypeRecord existing = repository.findSpaceTypeById(id)
@@ -527,12 +618,13 @@ public class PlatformService {
         String actor = actorId() == null ? "unknown" : actorId();
         if (isMasterAuthority()) {
             repository.saveSpaceTypeDeleteRequest(existing.id(), text, actor);
+            repository.completeSpaceTypeDeleteRequest(existing.id(), "approved");
             repository.saveSpaceType(new SpaceTypeRecord(
                     existing.id(), existing.typeCode(), existing.typeName(), existing.sortOrder(),
                     "disabled", existing.createdAt(), Instant.now()));
             auditEventService.recordSuccess(
                     "space_type.deleted", "governance.space.manage", "space_type", id, null, null,
-                    Map.of("type_code", existing.typeCode()));
+                    Map.of("type_code", existing.typeCode(), "reason", text));
             return;
         }
         repository.saveSpaceType(new SpaceTypeRecord(
@@ -541,9 +633,10 @@ public class PlatformService {
         repository.saveSpaceTypeDeleteRequest(existing.id(), text, actor);
         auditEventService.recordSuccess(
                 "space_type.delete_requested", "governance.space.manage", "space_type", id, null, null,
-                Map.of("type_code", existing.typeCode()));
+                Map.of("type_code", existing.typeCode(), "reason", text));
     }
 
+    @org.springframework.transaction.annotation.Transactional
     public void approveDeleteSpaceType(String id, String decision) {
         authorizationService.requirePlatformScope();
         if (!isMasterAuthority()) {
@@ -565,12 +658,13 @@ public class PlatformService {
         }
         Instant now = Instant.now();
         String next = "approved".equals(verdict) ? "disabled" : "archived";
+        repository.completeSpaceTypeDeleteRequest(existing.id(), verdict);
         repository.saveSpaceType(new SpaceTypeRecord(
                 existing.id(), existing.typeCode(), existing.typeName(), existing.sortOrder(),
                 next, existing.createdAt(), now));
         auditEventService.recordSuccess(
-                "space_type.delete_approved", "governance.space.manage", "space_type", id, null, null,
-                Map.of("decision", next, "type_code", existing.typeCode()));
+                "approved".equals(verdict) ? "space_type.delete_approved" : "space_type.delete_rejected", "governance.space.manage", "space_type", id, null, null,
+                Map.of("decision", verdict, "type_code", existing.typeCode()));
     }
 
     public SpaceRecord createSpace(SpaceRequest request) {
@@ -592,18 +686,22 @@ public class PlatformService {
                         throw com.complex.platform.common.api.ApiException.badRequest("VALIDATION_FAILED",
                                 "空间种类必须是空间种类表中的启用值");
                     }
-                    String projectId = trimToNull(request.projectId());
-                    if (projectId != null && !repository.projectExists(projectId)) {
+                    String projectId = requireText(request.projectId(), "project_id");
+                    if (!repository.projectExists(projectId)) {
                         throw ApiException.notFound("PROJECT_NOT_FOUND", "项目不存在");
                     }
+                    requireCurrentProject(projectId);
                     String merchantId = trimToNull(request.merchantId());
                     if (merchantId != null) {
                         requireActiveMerchant(merchantId);
                     }
                     String parentSpaceId = trimToNull(request.parentSpaceId());
-                    if (parentSpaceId != null && repository.findSpaceById(parentSpaceId).isEmpty()) {
-                        throw ApiException.notFound("SPACE_NOT_FOUND", "上级空间不存在");
+                    if (parentSpaceId != null) {
+                        SpaceRecord parent = repository.findSpaceById(parentSpaceId)
+                                .orElseThrow(() -> ApiException.notFound("SPACE_NOT_FOUND", "上级空间不存在"));
+                        requireCurrentProject(parent.projectId());
                     }
+                    requireNonNegative(request.areaSquareMeter(), "空间面积");
                     Instant now = Instant.now();
                     return repository.saveSpace(new SpaceRecord(
                             "sp_" + UUID.randomUUID(),
@@ -630,7 +728,7 @@ public class PlatformService {
     public PageResponse<BusinessEntityBindingRecord> listBusinessEntityBindings(int page, int size) {
         try {
             PageResponse<BusinessEntityBindingRecord> result =
-                    repository.listBusinessEntityBindingsPaged(page, size);
+                    repository.listBusinessEntityBindingsPaged(currentProject(), page, size);
             auditEventService.recordSuccess(
                     "entity_binding.viewed",
                     "entity.binding.view",
@@ -670,16 +768,16 @@ public class PlatformService {
                     BusinessEntityBindingRequest actual = requireRequest(request);
                     String merchantId = requireText(actual.merchantId(), "merchant_id");
                     // M009-006：四类主体绑定的经营对象与三个主体都必须真实存在（BEQR-143/PGM-026）；M009-028：且商户未下线。
-                    requireActiveMerchant(merchantId);
+                    repository.lockMerchant(merchantId);
+                    MerchantRecord merchant = requireActiveMerchant(merchantId);
                     String operationEntityId = requireBusinessEntity(actual.operationEntityId(), "operation_entity_id");
                     String contractEntityId = requireBusinessEntity(actual.contractEntityId(), "contract_entity_id");
                     String settlementEntityId = requireBusinessEntity(actual.settlementEntityId(), "settlement_entity_id");
-                    repository.findActiveBusinessEntityBindingByMerchantId(merchantId)
-                            .ifPresent(existing -> {
-                                throw ApiException.conflict(
-                                        "ACTIVE_BINDING_EXISTS",
-                                        "该商户已存在生效中的主体绑定，不能重复绑定");
-                            });
+                    requireUnfinishedBindingSlot(merchantId, null);
+                    requireUnchangedFinancialSubjects(merchant, operationEntityId, contractEntityId, settlementEntityId);
+                    LocalDate today = LocalDate.now(ZoneOffset.UTC);
+                    LocalDate effectiveFrom = actual.effectiveFrom() == null ? today : actual.effectiveFrom();
+                    boolean effective = !effectiveFrom.isAfter(today);
                     Instant now = Instant.now();
                     BusinessEntityBindingRecord saved = repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(
                             UUID.randomUUID().toString(),
@@ -687,14 +785,16 @@ public class PlatformService {
                             operationEntityId,
                             contractEntityId,
                             settlementEntityId,
-                            "active",
-                            actual.effectiveFrom() == null ? LocalDate.now(ZoneOffset.UTC) : actual.effectiveFrom(),
+                            effective ? "active" : "inactive",
+                            effectiveFrom,
                             null,
                             trimToNull(actual.changeReason()),
                             now,
                             now
                     ));
-                    syncMerchantEntities(saved);
+                    if (effective) {
+                        syncMerchantEntities(saved);
+                    }
                     return saved;
                 }
         );
@@ -716,19 +816,33 @@ public class PlatformService {
                     StatusChangeRequest actual = requireRequest(request);
                     String status = requireText(actual.action(), "action");
                     if (!Set.of("active", "inactive", "suspended").contains(status)) {
-                        throw ApiException.badRequest("VALIDATION_FAILED", "操作必须为启用（active）、停用（inactive）或暂停（suspended）");
+                        throw ApiException.badRequest("VALIDATION_FAILED", "操作必须为启用、停用或暂停");
                     }
                     String reason = requireText(actual.reason(), "reason");
-                    BusinessEntityBindingRecord existing = repository.findBusinessEntityBindingById(bindingId)
+                    BusinessEntityBindingRecord located = repository.findBusinessEntityBindingById(bindingId)
                             .orElseThrow(() -> ApiException.notFound("BINDING_NOT_FOUND", "主体绑定不存在或已被删除"));
+                    repository.lockMerchant(located.merchantId());
+                    BusinessEntityBindingRecord existing = repository.findBusinessEntityBindingById(bindingId).orElseThrow();
+                    MerchantRecord merchant = requireMerchant(existing.merchantId());
+                    LocalDate today = LocalDate.now(ZoneOffset.UTC);
+                    if (existing.effectiveTo() != null) {
+                        throw ApiException.conflict("BINDING_PERIOD_ENDED", "已结束的主体绑定不能重新打开或改写历史期间");
+                    }
+                    if ("active".equals(existing.status()) && !"active".equals(status)) {
+                        throw ApiException.conflict("BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW",
+                                "停用生效中的主体绑定须经过财税权责变更审核");
+                    }
+                    if (existing.effectiveFrom() != null && existing.effectiveFrom().isAfter(today)) {
+                        throw ApiException.conflict("BINDING_NOT_EFFECTIVE", "主体绑定尚未到生效日期，不能提前启用或结束");
+                    }
                     if ("active".equals(status)) {
-                        repository.findActiveBusinessEntityBindingByMerchantId(existing.merchantId())
-                                .filter(active -> !active.id().equals(existing.id()))
-                                .ifPresent(active -> {
-                                    throw ApiException.conflict(
-                                            "ACTIVE_BINDING_EXISTS",
-                                            "该商户已存在生效中的主体绑定，不能重复绑定");
-                                });
+                        requireActiveMerchant(existing.merchantId());
+                        requireBusinessEntity(existing.operationEntityId(), "operation_entity_id");
+                        requireBusinessEntity(existing.contractEntityId(), "contract_entity_id");
+                        requireBusinessEntity(existing.settlementEntityId(), "settlement_entity_id");
+                        requireUnfinishedBindingSlot(existing.merchantId(), existing.id());
+                        requireUnchangedFinancialSubjects(merchant, existing.operationEntityId(),
+                                existing.contractEntityId(), existing.settlementEntityId());
                     }
                     Instant now = Instant.now();
                     BusinessEntityBindingRecord saved = repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(
@@ -760,8 +874,7 @@ public class PlatformService {
         String value = requireText(raw, "merchant_type");
         if (!MERCHANT_BUSINESS_FORMATS.contains(value)) {
             throw ApiException.badRequest("MERCHANT_BUSINESS_FORMAT_INVALID",
-                    "商户业态必须为 酒吧/餐饮/零售/游乐/传媒/其他 之一"
-                            + "(bar/catering/retail/amusement/media/other)");
+                    "商户业态必须为 酒吧/餐饮/零售/游乐/传媒/其他 之一");
         }
         return value;
     }
@@ -771,8 +884,7 @@ public class PlatformService {
         String value = trimToNull(raw);
         if (value != null && !MERCHANT_COOPERATION_FORMS.contains(value)) {
             throw ApiException.badRequest("MERCHANT_COOPERATION_FORM_INVALID",
-                    "合作方式必须为 自营/租赁/联营/代运营/服务商接入/其他 之一"
-                            + "(self_operated/lease/joint_operation/managed_operation/service_provider/other)");
+                    "合作方式必须为 自营/租赁/联营/代运营/服务商接入/其他 之一");
         }
         return value;
     }
@@ -783,7 +895,7 @@ public class PlatformService {
         if (!SUBJECT_STATUSES.contains(value)) {
             throw ApiException.badRequest("VALIDATION_FAILED",
                     "状态必须为 待准入/正常/暂停/终止/黑名单 之一"
-                            + "(pending_admission/active/suspended/terminated/blacklisted)；归档请使用存档操作");
+                            + "；归档请使用存档操作");
         }
         return value;
     }
@@ -793,8 +905,7 @@ public class PlatformService {
         String value = trimToDefault(raw, "active");
         if (!PROJECT_STATUSES.contains(value)) {
             throw ApiException.badRequest("VALIDATION_FAILED",
-                    "项目状态必须为 草稿/待启用/已启用/暂停/终止/归档 之一"
-                            + "(draft/pending_activation/active/suspended/terminated/archived)");
+                    "项目状态必须为 草稿/待启用/已启用/暂停/终止/归档 之一");
         }
         return value;
     }
@@ -804,7 +915,7 @@ public class PlatformService {
         String value = trimToDefault(raw, "active");
         if (!SPACE_STATUSES.contains(value)) {
             throw ApiException.badRequest("VALIDATION_FAILED",
-                    "空间状态必须为 可用/不可用 之一(active/unavailable)");
+                    "空间状态必须为 可用/不可用 之一");
         }
         return value;
     }
@@ -814,7 +925,7 @@ public class PlatformService {
         String value = requireText(raw, "entity_role");
         if (!BUSINESS_ENTITY_ROLES.contains(value)) {
             throw ApiException.badRequest("VALIDATION_FAILED",
-                    "主体角色必须为 顶层平台/城市项目公司/域主管公司 之一(platform/project_company/management_company)");
+                    "主体角色必须为 顶层平台/城市项目公司/域主管公司 之一");
         }
         return value;
     }
@@ -825,9 +936,30 @@ public class PlatformService {
         if (!repository.businessEntityExists(id)) {
             throw ApiException.notFound("BUSINESS_ENTITY_NOT_FOUND", "主体不存在：" + id);
         }
+        if (!repository.businessEntityInProject(id, currentProject())) {
+            throw ApiException.forbidden("PROJECT_SCOPE_MISMATCH", "该主体未关联当前项目");
+        }
         return id;
     }
 
+    private void requireUnchangedFinancialSubjects(MerchantRecord merchant, String operationId,
+            String contractId, String settlementId) {
+        boolean unbound = merchant.businessEntityId() == null && merchant.contractEntityId() == null
+                && merchant.settlementEntityId() == null;
+        if (!unbound && (!java.util.Objects.equals(merchant.businessEntityId(), operationId)
+                || !java.util.Objects.equals(merchant.contractEntityId(), contractId)
+                || !java.util.Objects.equals(merchant.settlementEntityId(), settlementId))) {
+            throw ApiException.conflict("BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW",
+                    "变更已有经营、合同或结算主体须经过财税权责变更审核");
+        }
+    }
+
+    private void requireUnfinishedBindingSlot(String merchantId, String excludedId) {
+        if (repository.hasUnfinishedBusinessEntityBinding(merchantId, excludedId)) {
+            throw ApiException.conflict("ACTIVE_BINDING_EXISTS", "该商户已存在生效或待生效的主体绑定，不能重复绑定");
+        }
+    }
+
     /** M009-008：PGM-026「合同、结算、权限、看板、第三方接入都引用同一组绑定」——生效绑定是商户三个主体列的唯一来源，激活时同步投影。 */
     private void syncMerchantEntities(BusinessEntityBindingRecord binding) {
         repository.findMerchantById(binding.merchantId()).ifPresent(merchant -> repository.saveMerchant(
@@ -870,6 +1002,25 @@ public class PlatformService {
                     // M009-001/002：更新须命中已有商户，并保留存档/删除申请状态与 createdAt；
                     // M009-008：三个主体列以生效绑定为准，档案更新不覆盖。
                     MerchantRecord existing = update ? requireMerchant(merchantId) : null;
+                    String projectId = existing == null ? creationProject(request.projectId(), false) : existing.projectId();
+                    if (existing != null && trimToNull(request.projectId()) != null
+                            && !projectId.equals(request.projectId().trim())) {
+                        throw ApiException.forbidden("PROJECT_SCOPE_MISMATCH", "普通档案更新不能变更商户的项目归属");
+                    }
+                    if (existing == null) {
+                        if (trimToNull(request.businessEntityId()) != null) requireBusinessEntity(request.businessEntityId(), "business_entity_id");
+                        if (trimToNull(request.contractEntityId()) != null) requireBusinessEntity(request.contractEntityId(), "contract_entity_id");
+                        if (trimToNull(request.settlementEntityId()) != null) requireBusinessEntity(request.settlementEntityId(), "settlement_entity_id");
+                    }
+                    if (existing != null && "terminated".equals(existing.status()) && trimToNull(request.status()) == null) {
+                        status = existing.status();
+                    }
+                    if ("terminated".equals(status) && (existing == null || !"terminated".equals(existing.status()))) {
+                        throw ApiException.conflict("MERCHANT_EXIT_REQUIRES_BUFFER", "商户退出须经过退出缓冲流程，不能直接终止");
+                    }
+                    requireNonNegative(request.areaSquareMeter(), "面积");
+                    requireNonNegative(request.leasedAreaSquareMeter(), "租赁面积");
+                    requireNonNegative(request.rentPerSquareMeter(), "每平方米租金");
                     Instant now = Instant.now();
                     return repository.saveMerchant(new MerchantRecord(
                             merchantId,
@@ -896,7 +1047,8 @@ public class PlatformService {
                             existing == null ? "none" : existing.deleteStatus(),
                             existing == null ? null : existing.deleteRequestedBy(),
                             existing == null ? null : existing.deleteRequestedAt(),
-                            existing == null ? null : existing.deleteReason()
+                            existing == null ? null : existing.deleteReason(),
+                            projectId
                     ));
                 }
         );
@@ -921,10 +1073,38 @@ public class PlatformService {
                 AuditedOperation.of(successEvent, failureEvent, permissionCode, businessType),
                 action,
                 this::businessId,
-                Map.of()
+                auditMetadata(request)
         );
     }
 
+    private Map<String, Object> auditMetadata(Object request) {
+        Map<String, Object> metadata = new java.util.LinkedHashMap<>();
+        String reason = null;
+        if (request instanceof ProjectAssociationRequest value) {
+            reason = value.reason();
+            if (value.projectId() != null) metadata.put("projectId", value.projectId().trim());
+        } else if (request instanceof MerchantArchiveRequest value) {
+            reason = value.reason();
+        } else if (request instanceof MerchantDeleteRequest value) {
+            reason = value.reason();
+        } else if (request instanceof StatusChangeRequest value) {
+            reason = value.reason();
+        } else if (request instanceof BusinessEntityBindingRequest value) {
+            reason = value.changeReason();
+        } else if (request instanceof MerchantDeleteApprovalRequest value) {
+            if (value.decision() != null) metadata.put("decision", value.decision().trim().toLowerCase(Locale.ROOT));
+            if (value.comment() != null) metadata.put("comment", value.comment().trim());
+        }
+        if (reason != null) metadata.put("reason", reason.trim());
+        return Map.copyOf(metadata);
+    }
+
+    private void requireNonNegative(java.math.BigDecimal value, String label) {
+        if (value != null && value.signum() < 0) {
+            throw ApiException.badRequest("VALIDATION_FAILED", label + "不得为负数");
+        }
+    }
+
     private String trimToDefault(String value, String defaultValue) {
         String trimmed = value == null ? "" : value.trim();
         return trimmed.isEmpty() ? defaultValue : trimmed;
@@ -964,6 +1144,8 @@ public class PlatformService {
 
 
     private String businessId(Object value) {
+        if (value instanceof SpaceTypeRecord record) return record.id();
+        if (value instanceof MerchantDeleteDecision decision) return decision.merchantId();
         if (value instanceof BusinessEntityRecord record) {
             return record.id();
         }
diff --git a/backend/src/main/java/com/complex/platform/governance/ProjectRecord.java b/backend/src/main/java/com/complex/platform/governance/ProjectRecord.java
index 355c9db8..a9ada70d 100644
--- a/backend/src/main/java/com/complex/platform/governance/ProjectRecord.java
+++ b/backend/src/main/java/com/complex/platform/governance/ProjectRecord.java
@@ -1,4 +1,5 @@
 package com.complex.platform.governance;
+// 文件版本：V0.01
 
 import java.time.Instant;
 
@@ -12,6 +13,13 @@ public record ProjectRecord(
         Integer capacity,
         String status,
         Instant createdAt,
-        Instant updatedAt
+        Instant updatedAt,
+        String ownerPerson,
+        String address,
+        String businessScope
 ) {
+    public ProjectRecord(String id, String merchantId, String venueType, String name, String code, String floor,
+            Integer capacity, String status, Instant createdAt, Instant updatedAt) {
+        this(id, merchantId, venueType, name, code, floor, capacity, status, createdAt, updatedAt, null, null, null);
+    }
 }
diff --git a/backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java b/backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java
index fc6fa19f..594ede80 100644
--- a/backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java
+++ b/backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance.dto;
-// 文件版本：V0.01
+// 文件版本：V0.03
 
 import java.util.List;
 
@@ -12,6 +12,15 @@ public record BusinessEntityRequest(
         String unifiedSocialCreditCode,
         String legalRepresentative,
         String bankAccountRef,
-        String status
+        String status,
+        String projectId
 ) {
+    public BusinessEntityRequest(
+            String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds, String entityName,
+            String entityRole, String unifiedSocialCreditCode, String legalRepresentative, String bankAccountRef,
+            String status) {
+        this(
+                idempotencyKey, operatorNote, evidenceAttachmentIds, entityName, entityRole,
+                unifiedSocialCreditCode, legalRepresentative, bankAccountRef, status, null);
+    }
 }
diff --git a/backend/src/main/java/com/complex/platform/governance/dto/MerchantProfileRequest.java b/backend/src/main/java/com/complex/platform/governance/dto/MerchantProfileRequest.java
index c57aa129..5dd9526c 100644
--- a/backend/src/main/java/com/complex/platform/governance/dto/MerchantProfileRequest.java
+++ b/backend/src/main/java/com/complex/platform/governance/dto/MerchantProfileRequest.java
@@ -1,4 +1,5 @@
 package com.complex.platform.governance.dto;
+// 文件版本：V0.02
 
 import java.math.BigDecimal;
 import java.util.List;
@@ -22,6 +23,19 @@ public record MerchantProfileRequest(
         String secondaryContactPhone,
         BigDecimal rentPerSquareMeter,
         String cooperationForm,
-        String status
+        String status,
+        String projectId
 ) {
+    public MerchantProfileRequest(
+            String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds, String merchantName,
+            String merchantType, String businessEntityId, String contractEntityId, String settlementEntityId,
+            String contactName, String contactPhone, String ownerPerson, String mallBlockCode,
+            BigDecimal areaSquareMeter, BigDecimal leasedAreaSquareMeter, String secondaryContactName,
+            String secondaryContactPhone, BigDecimal rentPerSquareMeter, String cooperationForm, String status) {
+        this(
+                idempotencyKey, operatorNote, evidenceAttachmentIds, merchantName, merchantType, businessEntityId,
+                contractEntityId, settlementEntityId, contactName, contactPhone, ownerPerson, mallBlockCode,
+                areaSquareMeter, leasedAreaSquareMeter, secondaryContactName, secondaryContactPhone,
+                rentPerSquareMeter, cooperationForm, status, null);
+    }
 }
diff --git a/backend/src/main/java/com/complex/platform/governance/dto/ProjectAssociationRequest.java b/backend/src/main/java/com/complex/platform/governance/dto/ProjectAssociationRequest.java
new file mode 100644
index 00000000..5223b206
--- /dev/null
+++ b/backend/src/main/java/com/complex/platform/governance/dto/ProjectAssociationRequest.java
@@ -0,0 +1,5 @@
+package com.complex.platform.governance.dto;
+// 文件版本：V0.01
+
+public record ProjectAssociationRequest(String idempotencyKey, String projectId, String reason) {
+}
diff --git a/backend/src/main/java/com/complex/platform/governance/dto/ProjectRequest.java b/backend/src/main/java/com/complex/platform/governance/dto/ProjectRequest.java
index a3e81e93..77311d17 100644
--- a/backend/src/main/java/com/complex/platform/governance/dto/ProjectRequest.java
+++ b/backend/src/main/java/com/complex/platform/governance/dto/ProjectRequest.java
@@ -1,4 +1,5 @@
 package com.complex.platform.governance.dto;
+// 文件版本：V0.01
 
 import java.util.List;
 
@@ -12,6 +13,14 @@ public record ProjectRequest(
         String code,
         String floor,
         Integer capacity,
-        String status
+        String status,
+        String ownerPerson,
+        String address,
+        String businessScope
 ) {
+    public ProjectRequest(String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds,
+            String merchantId, String venueType, String name, String code, String floor, Integer capacity, String status) {
+        this(idempotencyKey, operatorNote, evidenceAttachmentIds, merchantId, venueType, name, code, floor,
+                capacity, status, null, null, null);
+    }
 }
diff --git a/backend/src/main/java/com/complex/platform/leasing/LeasingService.java b/backend/src/main/java/com/complex/platform/leasing/LeasingService.java
index eb6b15af..c1ff9e37 100644
--- a/backend/src/main/java/com/complex/platform/leasing/LeasingService.java
+++ b/backend/src/main/java/com/complex/platform/leasing/LeasingService.java
@@ -1,5 +1,5 @@
 package com.complex.platform.leasing;
-// 文件版本：V0.01
+// 文件版本：V0.04
 
 import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
@@ -240,7 +240,7 @@ public class LeasingService {
         // 1.4 决策A：merchant_id 空 → 签约自动建正式商户档案（带入线索/意向信息，四类主体默认同一公司=contractSubjectId）。
         String merchantId = trimToNull(actual.merchantId());
         if (merchantId == null) {
-            merchantId = autoCreateMerchantForSign(intent, contractSubjectId);
+            merchantId = autoCreateMerchantForSign(intent);
             // AUD-003：签约建"四主体"权威绑定——与商户/合同/线索同一事务原子落库；缺此则权威表(business_entity_bindings)恒空、
             // 主体绑定页/结算/权限按权威表读取时会把正式商户当未绑定。经营/结算主体缺省=合同主体（默认同一公司，后续走既有变更流拆分）。
             createFourPartyBinding(intent, merchantId, contractSubjectId, operationSubjectId, settlementSubjectId,
@@ -269,7 +269,7 @@ public class LeasingService {
      * 1.4 决策A：签约无既有商户 → 自动建正式商户档案，带入线索联系人/业态/空间/面积（四类主体默认同一公司=contractSubjectId）。
      * 稳定 idempotency key（按意向）→ 重签命中同一商户、不重复建；租金条款随合同 draft 带入，商户档案后续可完善。
      */
-    private String autoCreateMerchantForSign(LeasingIntentRecord intent, String contractSubjectId) {
+    private String autoCreateMerchantForSign(LeasingIntentRecord intent) {
         LeasingLeadRecord lead = repository.findLeadById(intent.leadId()).orElse(null);
         String contactName = lead != null ? trimToNull(lead.contactName()) : null;
         String contactPhone = lead != null ? trimToNull(lead.contactPhone()) : null;
@@ -285,11 +285,11 @@ public class LeasingService {
                 // V348 业态必填：线索意向业态直接带入（同五值词表）；线索阶段还没定业态的落「其他」，
                 // 由招商在商户档案里改正——不猜成「零售」，猜错＝通票分账把份额拆给错的业态。
                 businessType != null ? businessType : "other",
-                contractSubjectId, contractSubjectId, contractSubjectId,
+                null, null, null,
                 contactName, contactPhone, contactName,
                 spaceCode, area, area,
                 // M009-003：合作方式只能是 HTML 六类之一，签约阶段未定＝空，不发明「leasing_auto」值。
-                null, null, null, null, "active"));
+                null, null, null, null, "active", intent.projectId()));
         return merchant.id();
     }
 
diff --git a/backend/src/main/resources/db/migration/V427__m009_project_profiles_and_associations.sql b/backend/src/main/resources/db/migration/V427__m009_project_profiles_and_associations.sql
new file mode 100644
index 00000000..e485a5d2
--- /dev/null
+++ b/backend/src/main/resources/db/migration/V427__m009_project_profiles_and_associations.sql
@@ -0,0 +1,20 @@
+-- 文件版本：V0.01
+-- M009-013/038：历史资料保持为空，不推断项目或公司归属。
+ALTER TABLE projects
+    ADD COLUMN owner_person TEXT,
+    ADD COLUMN address TEXT,
+    ADD COLUMN business_scope TEXT;
+
+ALTER TABLE merchants
+    ADD COLUMN project_id VARCHAR(64) REFERENCES projects(id);
+
+CREATE INDEX idx_merchants_project_id ON merchants(project_id);
+
+CREATE TABLE business_entity_projects (
+    business_entity_id VARCHAR(64) NOT NULL REFERENCES business_entities(id),
+    project_id VARCHAR(64) NOT NULL REFERENCES projects(id),
+    PRIMARY KEY (business_entity_id, project_id)
+);
+
+CREATE INDEX idx_business_entity_projects_project_id
+    ON business_entity_projects(project_id);
diff --git a/backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java b/backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java
index 6a591453..714c10a2 100644
--- a/backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java
@@ -1,11 +1,12 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.01
+// 文件版本：V0.03
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
 
 import com.complex.platform.auth.AuthService;
+import com.complex.platform.auth.SessionIssuer;
 import com.complex.platform.common.api.ApiException;
 import java.time.Instant;
 import java.util.ArrayList;
@@ -234,10 +235,37 @@ class AuthorizationServiceTests {
         assertThat(service.currentMerchantIdQuietly()).contains("merchant");
     }
 
+    @Test
+    void productionMasterIdentityGetsExistingAuthorityWithoutBypassingPermissionDenials() {
+        AccessActor master = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
+                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active");
+        authenticateActor(master);
+        assertThat(service.isMasterAuthority(master)).isTrue();
+        service.requirePermission("ops.production_readiness.manage");
+        repository.addEffect(effect(master.id(), "ops.production_readiness.manage", AccessRepository.Effect.DENY));
+        assertThatThrownBy(() -> service.requirePermission("ops.production_readiness.manage"))
+                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("PERMISSION_DENIED"));
+        assertThatThrownBy(() -> service.requirePermission("not.registered"))
+                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("PERMISSION_DENIED"));
+    }
+
+    @Test
+    void platformAdminAndIncompleteMasterIdentityDoNotBecomeHighestAuthority() {
+        assertThat(service.isMasterAuthority(new AccessActor(SessionIssuer.MASTER_USER_ID,
+                SessionIssuer.MASTER_USERNAME, "platform_admin", "org", null, "active"))).isFalse();
+        assertThat(service.isMasterAuthority(new AccessActor(SessionIssuer.MASTER_USER_ID,
+                SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "disabled"))).isFalse();
+        assertThat(service.isMasterAuthority(new AccessActor("another-user", SessionIssuer.MASTER_USERNAME,
+                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active"))).isFalse();
+        assertThat(service.isMasterAuthority(new AccessActor(SessionIssuer.MASTER_USER_ID, "another-name",
+                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active"))).isFalse();
+    }
+
     @Test
     void currentSupervisorLevelIsThreeOnlyForMasterAuthority() {
         authenticateActor(new AccessActor(
-                "usr_master_authority", "master-authority", "platform_admin", "org", "merchant", "active"));
+                SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE,
+                "platform-company", null, "active"));
         assertThat(service.currentSupervisorLevel()).isEqualTo(3);
     }
 
diff --git a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
index 05b9afeb..7fdca215 100644
--- a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
+++ b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.24
+// 文件版本：V0.28
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
 import static org.mockito.ArgumentMatchers.*;
@@ -338,11 +338,7 @@ class M006SecurityRegressionTests {
         when(repository.applyTemplate(anyString(), anyList(), any(), any(), anyBoolean(), anyString())).thenReturn(List.of());
         roles.put("old", new RoleRecord("old", "旧岗位", null, "project", "active", Instant.now(), List.of(LOW), "org"));
         roles.put("new", new RoleRecord("new", "新岗位", null, "project", "active", Instant.now(), List.of(other), "org"));
-        actor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID, "platform_admin", "org", null);
-        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
-                new AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
-                        com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", "org", null, "active"),
-                "unused", List.of()));
+        actor("template-admin", "platform_admin", "org", null);
         PermissionTemplate template = service.createPermissionTemplate(template("empty"));
         for (String code : List.of("old", "new")) service.createPositionRoleTemplate(
                 new PositionRoleTemplateRequest(code, "理由", code, "level", code, template.id(), "active"));
@@ -413,7 +409,7 @@ class M006SecurityRegressionTests {
         verify(reviewRepository, never()).saveSupervisorReview(any());
         SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                 new AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
-                        com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", null, null, "active"),
+                        com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, null, null, "active"),
                 "unused", List.of()));
         assertThat(reviews.approve("top", new com.complex.platform.governance.review.SupervisorReviewApprovalRequest(
                 "top-master", "同意", List.of(), null, "approved", "同意")).decision()).isEqualTo("approved");
@@ -592,7 +588,7 @@ class M006SecurityRegressionTests {
                 new StatusChangeRequest("ordinary-admin", "approve_delete", "同意", List.of()))).isInstanceOf(ApiException.class);
         SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                 new AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
-                        com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", null, null, "active"), "unused", List.of()));
+                        com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, null, null, "active"), "unused", List.of()));
         service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("approve", "approve_delete", "同意", List.of()));
         assertThat(mapping.get().deletedAt()).isNotNull();
         assertThat(service.listPositionRoleTemplates(true).items()).hasSize(1);
@@ -791,9 +787,7 @@ class M006SecurityRegressionTests {
         when(repository.findUserPositionAssignment(anyString())).thenAnswer(a -> Optional.ofNullable(positions.get(a.getArgument(0))));
         roles.put("junior", new RoleRecord("junior", "初级", null, "project", "active", Instant.now(), List.of(LOW), "org"));
         roles.put("senior", new RoleRecord("senior", "高级", null, "project", "active", Instant.now(), List.of(other), "org"));
-        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
-                new AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
-                        com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", "org", null, "active"), "unused", List.of()));
+        actor("template-admin", "platform_admin", "org", null);
         PermissionTemplate template = service.createPermissionTemplate(template("empty"));
         service.createPositionRoleTemplate(new PositionRoleTemplateRequest("m-l1", "理由", "ops", "l1", "junior", template.id(), "active"));
         service.createPositionRoleTemplate(new PositionRoleTemplateRequest("m-l2", "理由", "ops", "l2", "senior", template.id(), "active"));
diff --git a/backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java b/backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java
index 6c401544..f703bd6b 100644
--- a/backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java
+++ b/backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access.staff;
 
-// 文件版本：V0.22
+// 文件版本：V0.26
 
 import static org.assertj.core.api.Assertions.*;
 
@@ -322,7 +322,7 @@ class M010RegressionTests {
         assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name='staff.payroll.penalty_cap_requested' AND metadata->>'review_id'=?",
                 Integer.class, pending.reviewId())).isEqualTo(1);
         SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
-                new AccessActor("usr_master_authority","master-authority","platform_admin","m010_p",null,"active"),"test",List.of()));
+                new AccessActor("usr_master_authority","master-authority",com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE,"m010_p",null,"active"),"test",List.of()));
         assertThatThrownBy(() -> reviewService.approve(pending.reviewId(),new com.complex.platform.governance.review.SupervisorReviewApprovalRequest(key(),"确认",List.of(),null,"approved","确认")))
                 .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEPARTMENT_REVIEWS_PENDING"));
         for (var line : lines) {
@@ -331,7 +331,7 @@ class M010RegressionTests {
             assertThat(service.getPenaltyCap().value()).isEqualTo(2000);
         }
         SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
-                new AccessActor("usr_master_authority","master-authority","platform_admin","m010_p",null,"active"),"test",List.of()));
+                new AccessActor("usr_master_authority","master-authority",com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE,"m010_p",null,"active"),"test",List.of()));
         reviewService.approve(pending.reviewId(),new com.complex.platform.governance.review.SupervisorReviewApprovalRequest(key(),"最终确认",List.of(),null,"approved","确认"));
         assertThat(service.getPenaltyCap().value()).isEqualTo(1000);
         actor(manager,"platform_admin",null);
@@ -600,7 +600,7 @@ class M010RegressionTests {
         repository.saveProfile(profile.withArchive(now, manager, now));
         SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                 new AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
-                        com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", null, null, "active"),
+                        com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, "m010_p", null, "active"),
                 "test", List.of()));
         var deleted = service.requestDeleteProfile(profile.id(), new StaffProfileDeleteRequest("工作结束"));
         assertThat(deleted.deleteStatus()).isEqualTo("approved");
diff --git a/backend/src/test/java/com/complex/platform/access/staff/StaffTestActors.java b/backend/src/test/java/com/complex/platform/access/staff/StaffTestActors.java
index 71825678..f8999bea 100644
--- a/backend/src/test/java/com/complex/platform/access/staff/StaffTestActors.java
+++ b/backend/src/test/java/com/complex/platform/access/staff/StaffTestActors.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access.staff;
 
-// 文件版本：V0.02
+// 文件版本：V0.05
 
 import com.complex.platform.access.*;
 import com.complex.platform.auth.SessionIssuer;
@@ -24,7 +24,7 @@ final class StaffTestActors {
     }
     static void master() {
         SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
-                new AccessActor(SessionIssuer.MASTER_USER_ID,SessionIssuer.MASTER_USERNAME,"platform_admin",null,null,"active"),"test",List.of()));
+                new AccessActor(SessionIssuer.MASTER_USER_ID,SessionIssuer.MASTER_USERNAME,com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE,null,null,"active"),"test",List.of()));
     }
 
     /** U05-003/U11-007：模拟私人手机安装时生成的 EC 密钥对 → [Base64(X.509 DER 公钥), SHA-256(DER) 十六进制指纹]。 */
diff --git a/backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java
index 474c55ce..9b5c2e7e 100644
--- a/backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.bar;
 
-// 文件版本：V0.02
+// 文件版本：V0.05
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
@@ -63,7 +63,7 @@ class BarStoredValueFundChainJdbcIntegrationTests {
         org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                 new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                         new com.complex.platform.access.AccessActor("usr_master_authority", "master-authority",
-                                "platform_admin", "org_1", null, "active"), null, java.util.List.of()));
+                                com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, "org_1", null, "active"), null, java.util.List.of()));
     }
 
     @AfterEach
diff --git a/backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java b/backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java
index b56a4459..69252fb5 100644
--- a/backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java
+++ b/backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java
@@ -1,6 +1,6 @@
 package com.complex.platform.customer;
 
-// 文件版本：V0.02
+// 文件版本：V0.05
 
 import static org.assertj.core.api.Assertions.assertThat;
 
@@ -40,7 +40,7 @@ class StoredValueNotificationIntegrationTest {
         org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                 new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                         new com.complex.platform.access.AccessActor("usr_master_authority", "master-authority",
-                                "platform_admin", "org_1", null, "active"), null, java.util.List.of()));
+                                com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, "org_1", null, "active"), null, java.util.List.of()));
         cleanupNotificationData();
         setSwitch("sms_notification", true);
         setSwitch("sms.notify.stored_value", true);
diff --git a/backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java b/backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java
index 02d9e49d..a16333d9 100644
--- a/backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.02
+// 文件版本：V0.03
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.mockito.ArgumentMatchers.any;
@@ -382,6 +382,52 @@ class PlatformControllerTests {
     }
 
 
+
+    @Test
+    void projectAssociationEndpointsConsumeEachFunctionalPermissionOnce() throws Exception {
+        MerchantRecord merchant = new MerchantRecord("mch_project", "项目商户", "other", null, null, null,
+                null, "18800000001", null, null, null, null, null, null, null, null, "active",
+                Instant.EPOCH, Instant.EPOCH).withProject("project_a");
+        when(service.assignMerchantProject(eq(merchant.id()), any())).thenReturn(merchant);
+        when(service.associateBusinessEntityProject(eq("entity_project"), any()))
+                .thenReturn(new BusinessEntityRecord("entity_project", "公司", "management_company",
+                        null, null, null, "active", Instant.EPOCH, Instant.EPOCH));
+        String request = """
+                {"idempotency_key":"project-association","project_id":"project_a","reason":"核实原始资料"}
+                """;
+        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
+                        "/api/v1/platform/merchants/mch_project/project")
+                        .with(user("ops").roles("P0_SMOKE")).contentType(MediaType.APPLICATION_JSON).content(request))
+                .andExpect(status().isOk())
+                .andExpect(jsonPath("$.data.project_id").value("project_a"))
+                .andExpect(jsonPath("$.data.contact_phone").value("188****0001"));
+        mockMvc.perform(post("/api/v1/platform/business-entities/entity_project/projects")
+                        .with(user("ops").roles("P0_SMOKE")).contentType(MediaType.APPLICATION_JSON).content(request))
+                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value("entity_project"));
+        verify(authorizationService, Mockito.times(1)).requirePermission("merchant.profile.manage");
+        verify(authorizationService, Mockito.times(1)).requirePermission("entity.binding.manage");
+        var expected = new com.complex.platform.governance.dto.ProjectAssociationRequest(
+                "project-association", "project_a", "核实原始资料");
+        verify(service).assignMerchantProject("mch_project", expected);
+        verify(service).associateBusinessEntityProject("entity_project", expected);
+    }
+
+    @Test
+    void associationEndpointRejectsMissingKeyAndDeniedPermissionBeforeService() throws Exception {
+        mockMvc.perform(post("/api/v1/platform/business-entities/entity_project/projects")
+                        .with(user("ops").roles("P0_SMOKE")).contentType(MediaType.APPLICATION_JSON)
+                        .content("{\"project_id\":\"project_a\",\"reason\":\"核实\"}"))
+                .andExpect(status().isBadRequest());
+        doThrow(ApiException.forbidden("PERMISSION_DENIED", "无权限"))
+                .when(authorizationService).requirePermission("merchant.profile.manage");
+        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
+                        "/api/v1/platform/merchants/mch_project/project")
+                        .with(user("ops").roles("P0_SMOKE")).contentType(MediaType.APPLICATION_JSON)
+                        .content("{\"idempotency_key\":\"denied\",\"project_id\":\"project_a\",\"reason\":\"核实\"}"))
+                .andExpect(status().isForbidden());
+        Mockito.verifyNoInteractions(service);
+    }
+
     @TestConfiguration
     static class TestBeans {
         @Bean
diff --git a/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
index b155b4bf..563eca72 100644
--- a/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.05
+// 文件版本：V0.08
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
@@ -30,6 +30,8 @@ class PlatformJdbcIntegrationTests {
 
     @Autowired
     private JdbcTemplate jdbcTemplate;
+    @Autowired private PlatformService platformService;
+    @Autowired private org.springframework.transaction.PlatformTransactionManager transactionManager;
 
     @Test
     void migrationCreatesPlatformMasterDataTablesAndSeedsPermissions() {
@@ -381,6 +383,232 @@ class PlatformJdbcIntegrationTests {
                 .isInstanceOf(DataIntegrityViolationException.class);
     }
 
+
+    @Test
+    void approvedProjectStorageExistsWithoutHistoricalBackfill() {
+        assertThat(jdbcTemplate.queryForList("""
+                SELECT column_name FROM information_schema.columns
+                 WHERE table_schema = 'public' AND table_name = 'projects'
+                """, String.class)).contains("owner_person", "address", "business_scope");
+        assertThat(jdbcTemplate.queryForList("""
+                SELECT column_name FROM information_schema.columns
+                 WHERE table_schema = 'public' AND table_name = 'merchants'
+                """, String.class)).contains("project_id");
+        assertThat(regclass("business_entity_projects")).isEqualTo("business_entity_projects");
+    }
+
+    @Test
+    void onlyEndedInactiveBindingsStopOccupyingMerchant() {
+        String id = "00000000-0000-0000-0000-000000009045";
+        String merchant = "m009_reference_period";
+        Instant now = Instant.now();
+        LocalDate today = LocalDate.now(java.time.ZoneOffset.UTC);
+        repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(id, merchant,
+                "operation", "contract", "settlement", "inactive", today.minusDays(3), today.minusDays(1),
+                "历史期间结束", now, now));
+        assertThat(repository.countMerchantReferences(merchant)).isZero();
+        for (String status : List.of("inactive", "suspended", "active")) {
+            repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(id, merchant,
+                    "operation", "contract", "settlement", status, today.plusDays(2), null,
+                    "尚未结束", now, now));
+            assertThat(repository.countMerchantReferences(merchant)).isEqualTo(1);
+        }
+        repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(id, merchant,
+                "operation", "contract", "settlement", "inactive", today.plusDays(2), today.plusDays(3),
+                "未来结束", now, now));
+        assertThat(repository.countMerchantReferences(merchant)).isEqualTo(1);
+        repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(id, merchant,
+                "operation", "contract", "settlement", "inactive", today, today.minusDays(1),
+                "倒置期间不算真正结束", now, now));
+        assertThat(repository.countMerchantReferences(merchant)).isEqualTo(1);
+    }
+
+
+    private void seedProjectScopeRows() {
+        Instant now = Instant.now();
+        for (String key : List.of("a", "b")) {
+            repository.saveProject(new ProjectRecord("m009_project_" + key, null, "complex", "项目" + key,
+                    null, null, null, "active", now, now, "负责人" + key, "地址" + key, "范围" + key));
+            repository.saveBusinessEntity(new BusinessEntityRecord("m009_company_" + key, "公司" + key,
+                    "management_company", null, null, null, "active", now, now));
+            repository.associateBusinessEntityProject("m009_company_" + key, "m009_project_" + key);
+            repository.saveMerchant(new MerchantRecord("m009_merchant_" + key, "商户" + key, "other",
+                    "m009_company_" + key, null, null, null, null, null, null, null, null, null, null, null,
+                    null, "active", now, now).withProject("m009_project_" + key));
+            repository.saveSpace(new SpaceRecord("m009_space_" + key, "m009_project_" + key, "m009_merchant_" + key,
+                    null, "空间" + key, "shop", null, null, List.of(), "active", now, now));
+            repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(
+                    "00000000-0000-0000-0000-0000000090" + ("a".equals(key) ? "81" : "82"),
+                    "m009_merchant_" + key, "m009_company_" + key, "m009_company_" + key, "m009_company_" + key,
+                    "inactive", LocalDate.now().plusYears(1), null, "待生效", now, now));
+        }
+    }
+
+    @Test
+    void scopedSqlFiltersAllFiveTotalsAndSupportsCompanyManyToMany() {
+        seedProjectScopeRows();
+        String a = "m009_project_a";
+        assertThat(repository.listProjectsPaged(a, 1, 1).total()).isEqualTo(1);
+        assertThat(repository.listProjectsPaged(a, 1, 1).items()).singleElement().satisfies(project -> {
+            assertThat(project.ownerPerson()).isEqualTo("负责人a");
+            assertThat(project.address()).isEqualTo("地址a");
+            assertThat(project.businessScope()).isEqualTo("范围a");
+        });
+        assertThat(repository.listMerchantsPaged(a, true, null, 1, 1).total()).isEqualTo(1);
+        assertThat(repository.listMerchantsPaged(a, true, null, 1, 1).items()).extracting(MerchantRecord::id)
+                .containsExactly("m009_merchant_a");
+        assertThat(repository.listSpacesPaged(a, 1, 1).total()).isEqualTo(1);
+        assertThat(repository.listSpacesPaged(a, 1, 1).items()).extracting(SpaceRecord::id).containsExactly("m009_space_a");
+        assertThat(repository.listBusinessEntityBindingsPaged(a, 1, 1).total()).isEqualTo(1);
+        assertThat(repository.listBusinessEntityBindingsPaged(a, 1, 1).items())
+                .extracting(BusinessEntityBindingRecord::merchantId).containsExactly("m009_merchant_a");
+        assertThat(repository.listBusinessEntitiesPaged(a, 1, 1).total()).isEqualTo(1);
+        repository.associateBusinessEntityProject("m009_company_b", a);
+        repository.associateBusinessEntityProject("m009_company_b", a);
+        assertThat(repository.businessEntityInProject("m009_company_b", "m009_project_b")).isTrue();
+        assertThat(repository.listBusinessEntitiesPaged(a, 1, 1).total()).isEqualTo(2);
+        var companyPage1 = repository.listBusinessEntitiesPaged(a, 1, 1);
+        var companyPage2 = repository.listBusinessEntitiesPaged(a, 2, 1);
+        assertThat(companyPage2.items()).hasSize(1).doesNotContainAnyElementsOf(companyPage1.items());
+        assertThat(repository.listMerchantsByBusinessEntity(a, "m009_company_b")).isEmpty();
+        assertThat(repository.listMerchantsByBusinessEntity(a, "m009_company_a")).extracting(MerchantRecord::id)
+                .containsExactly("m009_merchant_a");
+        assertThat(repository.listMerchantsByBusinessEntity(null, "m009_company_a")).isEmpty();
+        for (String scope : new String[] {null, "platform-company", "missing-project"}) {
+            org.junit.jupiter.api.Assertions.assertAll(
+                    () -> assertThat(repository.listMerchantsPaged(scope, true, null, 1, 1).total()).isZero(),
+                    () -> assertThat(repository.listProjectsPaged(scope, 1, 1).total()).isZero(),
+                    () -> assertThat(repository.listSpacesPaged(scope, 1, 1).total()).isZero(),
+                    () -> assertThat(repository.listBusinessEntitiesPaged(scope, 1, 1).total()).isZero(),
+                    () -> assertThat(repository.listBusinessEntityBindingsPaged(scope, 1, 1).total()).isZero());
+        }
+    }
+
+    @Test
+    void projectOnlyBackfillPreservesEveryOtherDatabaseColumnAndLegacyNulls() {
+        seedProjectScopeRows();
+        Instant now = Instant.now();
+        MerchantRecord legacy = new MerchantRecord("m009_legacy", "旧资料", "other", "financial-original",
+                "contract-original", "settlement-original", "联系人", "18800000001", "负责人", "A",
+                BigDecimal.TEN, BigDecimal.ONE, "第二联系人", "18800000002", BigDecimal.ZERO, "lease",
+                "suspended", now, now).withArchive(now, "original-actor", now)
+                .withDelete("pending", "original-actor", now, "原理由", now);
+        repository.saveMerchant(legacy);
+        assertThat(repository.findMerchantById(legacy.id()).orElseThrow().projectId()).isNull();
+        assertThat(repository.listMerchantsPaged("m009_project_a", true, null, 1, 500).items())
+                .extracting(MerchantRecord::id).doesNotContain(legacy.id());
+        String before = jdbcTemplate.queryForObject(
+                "SELECT (to_jsonb(m) - 'project_id')::text FROM merchants m WHERE id = ?", String.class, legacy.id());
+        assertThat(repository.assignMerchantProject(legacy.id(), "m009_project_a")).isTrue();
+        assertThat(repository.assignMerchantProject(legacy.id(), "m009_project_a")).isTrue();
+        assertThat(repository.assignMerchantProject(legacy.id(), "m009_project_b")).isFalse();
+        assertThat(jdbcTemplate.queryForObject(
+                "SELECT (to_jsonb(m) - 'project_id')::text FROM merchants m WHERE id = ?", String.class, legacy.id())).isEqualTo(before);
+        assertThat(repository.findMerchantById(legacy.id()).orElseThrow().projectId()).isEqualTo("m009_project_a");
+        // Existing record saves cannot erase a separately assigned project.
+        repository.saveMerchant(legacy);
+        assertThat(repository.findMerchantById(legacy.id()).orElseThrow().projectId()).isEqualTo("m009_project_a");
+        ProjectRecord oldRequest = new ProjectRecord("m009_legacy_project", null, "complex", "旧调用项目",
+                null, null, null, "active", now, now);
+        repository.saveProject(oldRequest);
+        assertThat(repository.listProjectsPaged(oldRequest.id(), 1, 1).items()).singleElement().satisfies(project -> {
+            assertThat(project.ownerPerson()).isNull();
+            assertThat(project.address()).isNull();
+            assertThat(project.businessScope()).isNull();
+        });
+    }
+
+    @Test
+    void completingSpaceTypeRequestKeepsPriorRequestHistory() {
+        Instant now = Instant.now();
+        repository.saveSpaceType(new SpaceTypeRecord("m009_type_request", "m009_type_request", "申请历史", 100, "archived", now, now));
+        repository.saveSpaceTypeDeleteRequest("m009_type_request", "第一次申请", "first-actor");
+        repository.completeSpaceTypeDeleteRequest("m009_type_request", "rejected");
+        repository.saveSpaceTypeDeleteRequest("m009_type_request", "第二次申请", "second-actor");
+        repository.completeSpaceTypeDeleteRequest("m009_type_request", "approved");
+        assertThat(jdbcTemplate.queryForList(
+                "SELECT requested_by || ':' || status FROM space_type_delete_requests WHERE space_type_id = ? ORDER BY requested_by",
+                String.class, "m009_type_request")).containsExactly("first-actor:rejected", "second-actor:approved");
+        assertThat(jdbcTemplate.queryForObject(
+                "SELECT count(*) FROM space_type_delete_requests WHERE space_type_id = ? AND status = 'pending'",
+                Integer.class, "m009_type_request")).isZero();
+    }
+
+
+    @Test
+    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
+    void concurrentFutureBindingsWaitForMerchantLockAndOnlyOneReservesTheSlot() throws Exception {
+        String key = "m009_concurrent_" + java.util.UUID.randomUUID().toString().substring(0, 8);
+        Instant now = Instant.now();
+        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
+        List<java.util.concurrent.Future<String>> futures = new java.util.ArrayList<>();
+        try {
+            repository.saveProject(new ProjectRecord(key, null, "complex", "并发绑定测试", null, null, null, "active", now, now));
+            repository.saveBusinessEntity(new BusinessEntityRecord(key, "并发主体", "management_company", null, null, null, "active", now, now));
+            repository.associateBusinessEntityProject(key, key);
+            repository.saveMerchant(new MerchantRecord(key, "并发商户", "other", null, null, null,
+                    null, null, null, null, null, null, null, null, null, null, "active", now, now).withProject(key));
+            var transaction = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
+            transaction.executeWithoutResult(status -> {
+                repository.lockMerchant(key);
+                for (int index = 0; index < 2; index++) {
+                    String requestKey = key + "_" + index;
+                    futures.add(pool.submit(() -> {
+                        var actor = new com.complex.platform.access.AccessActor("usr_admin", "platform_admin_user",
+                                "platform_admin", key, null, "active");
+                        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
+                                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(actor, "token", List.of()));
+                        try {
+                            platformService.createBusinessEntityBinding(
+                                    new com.complex.platform.governance.dto.BusinessEntityBindingRequest(
+                                            requestKey, null, List.of(), key, key, key, key,
+                                            LocalDate.now(java.time.ZoneOffset.UTC).plusYears(1), "并发预约"));
+                            return "CREATED";
+                        } catch (com.complex.platform.common.api.ApiException exception) {
+                            return exception.code();
+                        } finally {
+                            org.springframework.security.core.context.SecurityContextHolder.clearContext();
+                        }
+                    }));
+                }
+                int waiting = 0;
+                long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
+                while (waiting < 2 && System.nanoTime() < deadline) {
+                    jdbcTemplate.execute("SELECT pg_stat_clear_snapshot()");
+                    waiting = jdbcTemplate.queryForObject("""
+                            SELECT count(*) FROM pg_stat_activity
+                             WHERE datname = current_database() AND wait_event_type = 'Lock'
+                               AND query LIKE 'SELECT id FROM merchants WHERE id = %FOR UPDATE%'
+                            """, Integer.class);
+                    if (waiting < 2) {
+                        try { Thread.sleep(25); }
+                        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException(interrupted); }
+                    }
+                }
+                assertThat(waiting).as("两个请求均在商户行锁上等待，不能跳过锁直接检查").isEqualTo(2);
+            });
+            assertThat(List.of(futures.get(0).get(10, java.util.concurrent.TimeUnit.SECONDS),
+                    futures.get(1).get(10, java.util.concurrent.TimeUnit.SECONDS)))
+                    .containsExactlyInAnyOrder("CREATED", "ACTIVE_BINDING_EXISTS");
+            assertThat(jdbcTemplate.queryForObject(
+                    "SELECT count(*) FROM business_entity_bindings WHERE merchant_id = ? AND status = 'inactive' AND effective_to IS NULL",
+                    Integer.class, key)).isEqualTo(1);
+            MerchantRecord merchant = repository.findMerchantById(key).orElseThrow();
+            assertThat(merchant.businessEntityId()).isNull();
+            assertThat(merchant.contractEntityId()).isNull();
+            assertThat(merchant.settlementEntityId()).isNull();
+        } finally {
+            pool.shutdownNow();
+            assertThat(pool.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
+            jdbcTemplate.update("DELETE FROM business_entity_bindings WHERE merchant_id = ?", key);
+            jdbcTemplate.update("DELETE FROM merchants WHERE id = ?", key);
+            jdbcTemplate.update("DELETE FROM business_entity_projects WHERE business_entity_id = ?", key);
+            jdbcTemplate.update("DELETE FROM business_entities WHERE id = ?", key);
+            jdbcTemplate.update("DELETE FROM projects WHERE id = ?", key);
+            jdbcTemplate.update("DELETE FROM idempotency_records WHERE idempotency_key IN (?, ?)", key + "_0", key + "_1");
+        }
+    }
+
     private String regclass(String tableName) {
         return jdbcTemplate.queryForObject("SELECT to_regclass('public." + tableName + "')::text", String.class);
     }
diff --git a/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java b/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java
index 784f9e82..394ae9d1 100644
--- a/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.04
+// 文件版本：V0.16
 
 import com.complex.platform.access.AccessActor;
 import com.complex.platform.auth.SessionIssuer;
@@ -24,6 +24,7 @@ import com.complex.platform.governance.dto.MerchantDeleteRequest;
 import com.complex.platform.governance.dto.MerchantDeleteApprovalRequest;
 import com.complex.platform.governance.dto.MerchantArchiveRequest;
 import com.complex.platform.governance.dto.ProjectRequest;
+import com.complex.platform.governance.dto.ProjectAssociationRequest;
 import com.complex.platform.governance.dto.StatusChangeRequest;
 import com.complex.platform.support.TestSecurityActors;
 import com.fasterxml.jackson.databind.ObjectMapper;
@@ -64,6 +65,8 @@ class PlatformServiceTests {
         );
         // F-AUTHZ-02：治理写方法现首行 requirePlatformScope()，以平台管理员身份运行既有写用例。
         TestSecurityActors.asPlatformAdmin();
+        platformRepository.saveProject(new ProjectRecord("org_1", null, "complex", "当前项目", null, null,
+                null, "active", Instant.EPOCH, Instant.EPOCH));
     }
 
     @AfterEach
@@ -104,6 +107,7 @@ class PlatformServiceTests {
     // ---- V260 商户存档与审批后删除范式 ----
 
     private MerchantRecord seedMerchant(String key) {
+        if (!platformRepository.businessEntityExists("be_1")) seedEntity("be_1");
         return service.createMerchant(new MerchantProfileRequest(
                 key, "seed", List.of(), "存档删除测试商户", "other", "be_1",
                 null, null, "Jane", "18800000011",
@@ -114,12 +118,13 @@ class PlatformServiceTests {
     private void seedEntity(String id) {
         platformRepository.saveBusinessEntity(new BusinessEntityRecord(
                 id, id, "management_company", null, null, null, "active", Instant.EPOCH, Instant.EPOCH));
+        platformRepository.associateBusinessEntityProject(id, "org_1");
     }
 
-    /** M009-009：最高权限（持授权 U 盘密钥者）身份——isMasterAuthority 要求 platform_admin + 固定 id/用户名（BSM-004/007）。 */
+    /** M009-009：最高权限（持授权 U 盘密钥者）身份——与生产 SessionIssuer 相同的最高权限三件套（BSM-004/007）。 */
     private static void asMasterAuthority() {
         AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
-                "platform_admin", "org_1", null, "active");
+                com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active");
         SecurityContextHolder.getContext().setAuthentication(
                 new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
     }
@@ -230,10 +235,10 @@ class PlatformServiceTests {
                 "sp-bad-prj", "create", List.of(), "prj_missing", null, null, "A", "shop", null, null, null, "active")))
                 .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("PROJECT_NOT_FOUND"));
         assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
-                "sp-bad-mch", "create", List.of(), null, "mch_missing", null, "A", "shop", null, null, null, "active")))
+                "sp-bad-mch", "create", List.of(), "org_1", "mch_missing", null, "A", "shop", null, null, null, "active")))
                 .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("MERCHANT_NOT_FOUND"));
         assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
-                "sp-bad-parent", "create", List.of(), null, null, "sp_missing", "A", "shop", null, null, null, "active")))
+                "sp-bad-parent", "create", List.of(), "org_1", null, "sp_missing", "A", "shop", null, null, null, "active")))
                 .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("SPACE_NOT_FOUND"));
     }
 
@@ -247,7 +252,7 @@ class PlatformServiceTests {
                 "project-archived-mch", "create", List.of(), merchant.id(), "bar", "P0 Bar", null, null, null, "draft")))
                 .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("MERCHANT_ARCHIVED"));
         assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
-                "sp-archived-mch", "create", List.of(), null, merchant.id(), null, "A", "shop", null, null, null, "active")))
+                "sp-archived-mch", "create", List.of(), "org_1", merchant.id(), null, "A", "shop", null, null, null, "active")))
                 .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("MERCHANT_ARCHIVED"));
         assertThatThrownBy(() -> service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
                 "binding-archived-mch", "ref", List.of(), merchant.id(), "be_ref_1", "be_ref_1", "be_ref_1", null, null)))
@@ -260,18 +265,18 @@ class PlatformServiceTests {
     @Test
     void createSpaceRejectsStatusOutsideCheckSet() {
         assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
-                "sp-bad-status", "create", List.of(), null, null, null, "A", "shop", null, null, null, "zzz")))
+                "sp-bad-status", "create", List.of(), "org_1", null, null, "A", "shop", null, null, null, "zzz")))
                 .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("VALIDATION_FAILED"));
         assertThat(platformRepository.listSpaces()).noneMatch(s -> "zzz".equals(s.status()));
         assertThat(service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
-                "sp-unavailable", "create", List.of(), null, null, null, "B", "shop", null, null, null, "unavailable")).status())
+                "sp-unavailable", "create", List.of(), "org_1", null, null, "B", "shop", null, null, null, "unavailable")).status())
                 .isEqualTo("unavailable");
     }
 
     // ---- M009-008：生效绑定同步商户三个主体列（PGM-026「引用同一组绑定」） ----
     @Test
     void createBusinessEntityBindingSyncsMerchantEntityColumns() {
-        MerchantRecord merchant = seedMerchant("seed-sync");
+        MerchantRecord merchant = unboundMerchant("seed-sync");
         seedEntity("be_op_sync");
         seedEntity("be_ct_sync");
         seedEntity("be_st_sync");
@@ -422,7 +427,7 @@ class PlatformServiceTests {
     @Test
     void occupiedSpaceTypeCannotBeDeleted() {
         service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
-                "sp-shop-occ", "create", List.of(), null, null, null, "Shop Occ", "shop", "1", null, null, "active"));
+                "sp-shop-occ", "create", List.of(), "org_1", null, null, "Shop Occ", "shop", "1", null, null, "active"));
         service.archiveSpaceType("sptype_shop", "测试占用");
         assertThatThrownBy(() -> service.requestDeleteSpaceType("sptype_shop", "想删"))
                 .isInstanceOfSatisfying(ApiException.class, exception ->
@@ -467,6 +472,7 @@ class PlatformServiceTests {
     // V348 按业态查询（分账按业态拆份额要用）：只返回该业态的商户。
     @Test
     void listMerchantsFiltersByBusinessFormat() {
+        seedEntity("be_1");
         service.createMerchant(new MerchantProfileRequest(
                 "seed-bar", "seed", List.of(), "话梅酒馆", "bar", "be_1",
                 null, null, "Jane", "18800000011",
@@ -510,6 +516,7 @@ class PlatformServiceTests {
         // 默认列表不见；已存档列表仍可查且 delete_status=approved——与 JdbcPlatformRepository 同口径。
         assertThat(service.listMerchants(false).items())
                 .noneMatch(m -> m.id().equals(merchant.id()));
+        TestSecurityActors.asPlatformAdmin();
         assertThat(service.listMerchants(true).items())
                 .anyMatch(m -> m.id().equals(merchant.id()) && "approved".equals(m.deleteStatus()));
     }
@@ -543,6 +550,7 @@ class PlatformServiceTests {
 
         assertThat(decision.deleted()).isFalse();
         assertThat(decision.decision()).isEqualTo("rejected");
+        TestSecurityActors.asPlatformAdmin();
         assertThat(service.listMerchants(true).items())
                 .anyMatch(m -> m.id().equals(merchant.id()) && "none".equals(m.deleteStatus()) && m.archivedAt() != null);
     }
@@ -575,6 +583,7 @@ class PlatformServiceTests {
                 .isInstanceOfSatisfying(ApiException.class, exception ->
                         assertThat(exception.code()).isEqualTo("MERCHANT_IN_USE"));
         // 拒删后商户仍在（含已存档列表可见）。
+        TestSecurityActors.asPlatformAdmin();
         assertThat(service.listMerchants(true).items())
                 .anyMatch(m -> m.id().equals(merchant.id()));
     }
@@ -675,7 +684,7 @@ class PlatformServiceTests {
     void listSpacesWritesViewedAudit() {
         platformRepository.spaces.put("sp_1", new SpaceRecord(
                 "sp_1",
-                "prj_1",
+                "org_1",
                 "mch_1",
                 null,
                 "P0 Space",
@@ -699,7 +708,7 @@ class PlatformServiceTests {
 
     @Test
     void createBusinessEntityBindingAllowsOnlyOneActiveBindingPerMerchant() {
-        MerchantRecord merchant = seedMerchant("seed-binding"); // M009-006：经营对象与主体须存在
+        MerchantRecord merchant = unboundMerchant("seed-binding"); // M009-006：经营对象与主体须存在
         for (String id : List.of("be_operation_1", "be_contract_1", "be_settlement_1",
                 "be_operation_2", "be_contract_2", "be_settlement_2")) {
             seedEntity(id);
@@ -740,51 +749,27 @@ class PlatformServiceTests {
     }
 
     @Test
-    void changeBusinessEntityBindingRecordsStatusHistoryAndFreesActiveUniqueness() {
-        MerchantRecord merchant = seedMerchant("seed-change"); // M009-006
-        for (String id : List.of("be_operation", "be_contract", "be_settlement",
-                "be_operation_new", "be_contract_new", "be_settlement_new")) {
-            seedEntity(id);
+    void activeBindingCannotBeDisabledToReplaceFinancialSubjects() {
+        MerchantRecord merchant = unboundMerchant("active-merchant");
+        seedEntity("active-company");
+        var binding = service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
+                "active-create", null, List.of(), merchant.id(), "active-company", "active-company",
+                "active-company", LocalDate.now(), "初始生效"));
+        for (String action : List.of("inactive", "suspended")) {
+            assertThatThrownBy(() -> service.changeBusinessEntityBinding(binding.id(),
+                    new StatusChangeRequest("stop-" + action, action, "停用后改主体", List.of())))
+                    .isInstanceOf(ApiException.class);
         }
-        BusinessEntityBindingRecord created = service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
-                "binding-change-create",
-                "create binding",
-                List.of(),
-                merchant.id(),
-                "be_operation",
-                "be_contract",
-                "be_settlement",
-                LocalDate.parse("2026-06-14"),
-                "initial setup"
-        ));
-
-        BusinessEntityBindingRecord changed = service.changeBusinessEntityBinding(
-                created.id(),
-                new StatusChangeRequest("binding-change", "inactive", "contract entity changed", List.of("att_change"))
-        );
-        BusinessEntityBindingRecord replacement = service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
-                "binding-change-replacement",
-                "replacement binding",
-                List.of(),
-                merchant.id(),
-                "be_operation_new",
-                "be_contract_new",
-                "be_settlement_new",
-                LocalDate.parse("2026-06-15"),
-                "replacement"
-        ));
-
-        assertThat(changed.status()).isEqualTo("inactive");
-        assertThat(replacement.status()).isEqualTo("active");
-        assertThat(platformRepository.businessEntityBindingStatusHistory)
-                .containsExactly("active", "inactive", "active");
+        assertThat(platformRepository.findBusinessEntityBindingById(binding.id())).contains(binding);
+        assertThat(platformRepository.findMerchantById(merchant.id()).orElseThrow().settlementEntityId())
+                .isEqualTo("active-company");
     }
 
 
     // P1-4 red→green: action 字段拒绝非法枚举值
     @Test
     void changeBusinessEntityBindingRejectsInvalidAction() {
-        MerchantRecord merchant = seedMerchant("seed-enum"); // M009-006
+        MerchantRecord merchant = unboundMerchant("seed-enum"); // M009-006
         for (String id : List.of("be_op", "be_ctr", "be_stl")) {
             seedEntity(id);
         }
@@ -846,7 +831,7 @@ class PlatformServiceTests {
         assertThat(service.listSpaceTypes().items()).extracting(SpaceTypeRecord::typeCode).contains("kiosk", "shop");
         // 新录入类型立即可用于 createSpace 硬校验。
         SpaceRecord space = service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
-                "sp-kiosk", "create", List.of(), null, null, null, "K1", "kiosk", null, null, null, "active")); // M009-007：归属为空或须存在
+                "sp-kiosk", "create", List.of(), "org_1", null, null, "K1", "kiosk", null, null, null, "active")); // M009-039：归属为当前真实项目
         assertThat(space.spaceType()).isEqualTo("kiosk");
     }
 
@@ -854,7 +839,7 @@ class PlatformServiceTests {
     void createSpaceRejectsUnknownSpaceType() {
         // 条目3 硬校验：不在字典内的 space_type → VALIDATION_FAILED。
         assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
-                        "sp-bad", "create", List.of(), null, null, null, "Bad", "no_such_type", null, null, null, "active")))
+                        "sp-bad", "create", List.of(), "org_1", null, null, "Bad", "no_such_type", null, null, null, "active")))
                 .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class, ex ->
                         assertThat(ex.code()).isEqualTo("VALIDATION_FAILED"));
     }
@@ -862,13 +847,442 @@ class PlatformServiceTests {
     @Test
     void createSpaceAcceptsDictionarySpaceType() {
         SpaceRecord saved = service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
-                "sp-ok", "create", List.of(), null, null, null, "Shop A", "shop", "1", null, null, "active"));
+                "sp-ok", "create", List.of(), "org_1", null, null, "Shop A", "shop", "1", null, null, "active"));
         assertThat(saved.spaceType()).isEqualTo("shop");
     }
 
+    private MerchantRecord unboundMerchant(String key) {
+        return service.createMerchant(new MerchantProfileRequest(
+                key, "seed", List.of(), "待绑定商户", "other", null, null, null,
+                "Jane", "18800000011", null, null, null, null, null, null, null, null, "active"));
+    }
+
+    @Test
+    void futureBindingDoesNotChangeCurrentMerchantSubjects() {
+        MerchantRecord merchant = unboundMerchant("future-merchant");
+        seedEntity("future-company");
+        var binding = service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
+                "future-binding", null, List.of(), merchant.id(), "future-company", "future-company",
+                "future-company", LocalDate.now().plusDays(10), "未来生效"));
+        assertThat(binding.status()).isEqualTo("inactive");
+        assertThat(binding.effectiveTo()).isNull();
+        MerchantRecord saved = platformRepository.findMerchantById(merchant.id()).orElseThrow();
+        assertThat(saved.businessEntityId()).isNull();
+        assertThat(saved.contractEntityId()).isNull();
+        assertThat(saved.settlementEntityId()).isNull();
+    }
+
+    @Test
+    void anotherFutureBindingCannotReserveTheSameMerchant() {
+        MerchantRecord merchant = unboundMerchant("reserved-merchant");
+        seedEntity("reserved-company");
+        service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
+                "reserve-first", null, List.of(), merchant.id(), "reserved-company", "reserved-company",
+                "reserved-company", LocalDate.now().plusDays(10), "预约"));
+        assertThatThrownBy(() -> service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
+                "reserve-second", null, List.of(), merchant.id(), "reserved-company", "reserved-company",
+                "reserved-company", LocalDate.now().plusDays(11), "第二条")))
+                .isInstanceOf(ApiException.class);
+        assertThat(platformRepository.listBusinessEntityBindings()).hasSize(1);
+    }
+
+    @Test
+    void futureBindingCannotActivateOrEndBeforeItsStart() {
+        MerchantRecord merchant = unboundMerchant("early-merchant");
+        seedEntity("early-company");
+        var binding = service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
+                "early-create", null, List.of(), merchant.id(), "early-company", "early-company",
+                "early-company", LocalDate.now().plusDays(10), "未来生效"));
+        assertThatThrownBy(() -> service.changeBusinessEntityBinding(binding.id(),
+                new StatusChangeRequest("early-activate", "active", "提前启用", List.of())))
+                .isInstanceOf(ApiException.class);
+        assertThatThrownBy(() -> service.changeBusinessEntityBinding(binding.id(),
+                new StatusChangeRequest("early-end", "inactive", "提前结束", List.of())))
+                .isInstanceOf(ApiException.class);
+        assertThat(platformRepository.findBusinessEntityBindingById(binding.id())).contains(binding);
+    }
+
+    @Test
+    void dueBindingCanActivateButAnEndedHistoricalPeriodCannotReopen() {
+        MerchantRecord merchant = unboundMerchant("due-merchant");
+        seedEntity("due-company");
+        var due = new BusinessEntityBindingRecord("due-binding", merchant.id(), "due-company", "due-company",
+                "due-company", "inactive", LocalDate.now(), null, "到期", Instant.EPOCH, Instant.EPOCH);
+        platformRepository.saveBusinessEntityBinding(due);
+        assertThat(service.changeBusinessEntityBinding(due.id(),
+                new StatusChangeRequest("due-activate", "active", "到期启用", List.of())).status()).isEqualTo("active");
+        assertThat(platformRepository.findMerchantById(merchant.id()).orElseThrow().settlementEntityId())
+                .isEqualTo("due-company");
+
+        MerchantRecord historicalMerchant = unboundMerchant("ended-history-merchant");
+        var ended = new BusinessEntityBindingRecord("ended-binding", historicalMerchant.id(), "due-company", "due-company",
+                "due-company", "inactive", LocalDate.now().minusDays(5), LocalDate.now().minusDays(1),
+                "历史", Instant.EPOCH, Instant.EPOCH);
+        platformRepository.saveBusinessEntityBinding(ended);
+        assertThatThrownBy(() -> service.changeBusinessEntityBinding(ended.id(),
+                new StatusChangeRequest("reopen-history", "active", "重开历史", List.of())))
+                .isInstanceOf(ApiException.class);
+        assertThat(platformRepository.findBusinessEntityBindingById(ended.id())).contains(ended);
+    }
+
+    @Test
+    void bindingCannotReplaceExistingFinancialSubjectsEvenForMasterAuthority() {
+        MerchantRecord merchant = unboundMerchant("existing-subject-merchant");
+        platformRepository.saveMerchant(merchant.withEntities("known-company", "known-company", "known-company", Instant.now()));
+        seedEntity("different-company");
+        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
+                SessionIssuer.MASTER_ACCOUNT_TYPE, "org_1", null, "active");
+        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
+        assertThatThrownBy(() -> service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
+                "replace-subjects", null, List.of(), merchant.id(), "different-company", "different-company",
+                "different-company", LocalDate.now(), "不经财税审核替换")))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW"));
+        assertThat(platformRepository.findMerchantById(merchant.id()).orElseThrow().settlementEntityId())
+                .isEqualTo("known-company");
+        assertThat(platformRepository.listBusinessEntityBindings()).isEmpty();
+    }
+
+    @Test
+    void projectCreationReturnsApprovedOptionalProfileFields() {
+        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
+                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
+        ProjectRequest request = mapper.convertValue(Map.of(
+                "idempotencyKey", "project-profile", "name", "甲综合体", "venueType", "bar",
+                "status", "draft", "ownerPerson", "王负责人", "address", "甲路一号",
+                "businessScope", "餐饮与零售"), ProjectRequest.class);
+        var result = mapper.valueToTree(service.createProject(request));
+        assertThat(result.path("ownerPerson").asText()).isEqualTo("王负责人");
+        assertThat(result.path("address").asText()).isEqualTo("甲路一号");
+        assertThat(result.path("businessScope").asText()).isEqualTo("餐饮与零售");
+    }
+
+    @Test
+    void deletedSpaceTypeCanBeRestoredButPendingRequestCannotBeBypassed() {
+        service.archiveSpaceType("sptype_booth", "暂不用");
+        service.requestDeleteSpaceType("sptype_booth", "删除错误配置");
+        assertThatThrownBy(() -> service.unarchiveSpaceType("sptype_booth")).isInstanceOf(ApiException.class);
+        asMasterAuthority();
+        service.approveDeleteSpaceType("sptype_booth", "approved");
+        org.assertj.core.api.Assertions.assertThatCode(() -> service.unarchiveSpaceType("sptype_booth"))
+                .doesNotThrowAnyException();
+        assertThat(platformRepository.findSpaceTypeById("sptype_booth").orElseThrow().status()).isEqualTo("active");
+    }
+
+    @Test
+    void spaceTypeDeletionCompletesOnlyTheCurrentRequest() {
+        service.archiveSpaceType("sptype_booth", "暂不用");
+        service.requestDeleteSpaceType("sptype_booth", "申请");
+        asMasterAuthority();
+        service.approveDeleteSpaceType("sptype_booth", "rejected");
+        assertThat(platformRepository.findSpaceTypeDeleteRequestedBy("sptype_booth")).isNull();
+        service.requestDeleteSpaceType("sptype_booth", "最高权限直办");
+        assertThat(platformRepository.findSpaceTypeDeleteRequestedBy("sptype_booth")).isNull();
+        assertThat(platformRepository.findSpaceTypeById("sptype_booth").orElseThrow().status()).isEqualTo("disabled");
+    }
+
+    @Test
+    void spaceTypeAuditHasObjectIdAndActualRejectedDecision() {
+        var type = service.createSpaceType(new com.complex.platform.governance.dto.SpaceTypeRequest(
+                "audit-type", null, List.of(), "auditable_type", "审计类型", 10, "active"));
+        assertThat(auditEventRepository.events).filteredOn(e -> "space_type.updated".equals(e.eventName()))
+                .singleElement().satisfies(e -> assertThat(e.businessId()).isEqualTo(type.id()));
+        service.archiveSpaceType(type.id(), "存档原因");
+        service.requestDeleteSpaceType(type.id(), "删除原因");
+        asMasterAuthority();
+        service.approveDeleteSpaceType(type.id(), "rejected");
+        assertThat(auditEventRepository.events).filteredOn(e -> "space_type.delete_rejected".equals(e.eventName()))
+                .singleElement().satisfies(e -> {
+                    assertThat(e.businessId()).isEqualTo(type.id());
+                    assertThat(e.metadata()).containsEntry("decision", "rejected");
+                });
+        assertThat(auditEventRepository.events).filteredOn(e -> "space_type.delete_requested".equals(e.eventName()))
+                .singleElement().satisfies(e -> assertThat(e.metadata()).containsEntry("reason", "删除原因"));
+    }
+
+    @Test
+    void merchantDeleteAuditKeepsReasonAndRecordsRejectionAgainstTheMerchant() {
+        MerchantRecord merchant = seedMerchant("audit-merchant");
+        service.archiveMerchant(merchant.id(), new MerchantArchiveRequest("audit-archive", "存档原因"));
+        service.requestDeleteMerchant(merchant.id(), new MerchantDeleteRequest("audit-delete", "删除原因"));
+        asMasterAuthority();
+        service.approveDeleteMerchant(merchant.id(), new MerchantDeleteApprovalRequest("audit-reject", "rejected", "核实保留"));
+        assertThat(auditEventRepository.events).filteredOn(e -> "merchant.delete_requested".equals(e.eventName()))
+                .singleElement().satisfies(e -> assertThat(e.metadata()).containsEntry("reason", "删除原因"));
+        assertThat(auditEventRepository.events).filteredOn(e -> "merchant.delete_rejected".equals(e.eventName()))
+                .singleElement().satisfies(e -> {
+                    assertThat(e.businessId()).isEqualTo(merchant.id());
+                    assertThat(e.metadata()).containsEntry("decision", "rejected").containsEntry("comment", "核实保留");
+                });
+    }
+
+    private MerchantProfileRequest numericMerchant(String key, String status,
+            BigDecimal area, BigDecimal leasedArea, BigDecimal rent) {
+        return new MerchantProfileRequest(key, null, List.of(), "数值验证商户", "other",
+                null, null, null, null, null, null, null, area, leasedArea, null, null, rent, null, status);
+    }
+
+    @Test
+    void negativeCapacityAreasAndRentAreRejectedWhileZeroAndNullRemainValid() {
+        assertThatThrownBy(() -> service.createProject(new ProjectRequest(
+                "negative-capacity", null, List.of(), null, "bar", "项目", null, null, -1, "draft")))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
+        for (MerchantProfileRequest request : List.of(
+                numericMerchant("negative-area", "active", new BigDecimal("-1"), null, null),
+                numericMerchant("negative-leased", "active", null, new BigDecimal("-1"), null),
+                numericMerchant("negative-rent", "active", null, null, new BigDecimal("-1")))) {
+            assertThatThrownBy(() -> service.createMerchant(request))
+                    .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
+        }
+        assertThat(service.createProject(new ProjectRequest(
+                "zero-capacity", null, List.of(), null, "bar", "项目", null, null, 0, "draft")).capacity()).isZero();
+        assertThat(service.createMerchant(numericMerchant("zero-values", "active",
+                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)).rentPerSquareMeter()).isZero();
+        assertThat(service.createMerchant(numericMerchant("null-values", "active", null, null, null)).areaSquareMeter()).isNull();
+    }
+
+    @Test
+    void ordinaryMerchantProfileCannotCauseImmediateExit() {
+        assertThatThrownBy(() -> service.createMerchant(numericMerchant("new-exit", "terminated", null, null, null)))
+                .isInstanceOf(ApiException.class);
+        MerchantRecord merchant = unboundMerchant("exit-merchant");
+        assertThatThrownBy(() -> service.updateMerchant(merchant.id(),
+                numericMerchant("update-exit", "terminated", null, null, null))).isInstanceOf(ApiException.class);
+        assertThat(platformRepository.findMerchantById(merchant.id()).orElseThrow().status()).isEqualTo("active");
+    }
+
+    @Test
+    void userFacingRoleValidationDoesNotIncludeEnglishEnumNames() {
+        assertThatThrownBy(() -> service.createBusinessEntity(new BusinessEntityRequest(
+                "invalid-role-message", null, List.of(), "公司", "unknown", null, null, null, "active")))
+                .isInstanceOfSatisfying(ApiException.class, e -> {
+                    assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
+                    assertThat(e.getMessage()).doesNotContain("platform", "project_company", "management_company");
+                });
+    }
+
+    @Test
+    void savingAnAlreadyTerminatedMerchantDoesNotReactivateItWhenStatusIsOmitted() throws Exception {
+        MerchantRecord merchant = unboundMerchant("already-terminated");
+        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
+        var data = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.valueToTree(merchant);
+        data.put("status", "terminated");
+        platformRepository.saveMerchant(mapper.treeToValue(data, MerchantRecord.class));
+        assertThat(service.updateMerchant(merchant.id(),
+                numericMerchant("same-terminated", "terminated", null, null, null)).status()).isEqualTo("terminated");
+        assertThat(service.updateMerchant(merchant.id(),
+                numericMerchant("omitted-status", null, null, null, null)).status()).isEqualTo("terminated");
+    }
+
+    private void twoProjectFixtures() {
+        for (String id : List.of("org_1", "other-project")) {
+            platformRepository.saveProject(new ProjectRecord(id, null, "bar", id, null, null, null,
+                    "active", Instant.EPOCH, Instant.EPOCH));
+        }
+        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
+                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
+        for (String suffix : List.of("a", "b")) {
+            String project = "a".equals(suffix) ? "org_1" : "other-project";
+            platformRepository.saveMerchant(mapper.convertValue(Map.of(
+                    "id", "scoped-merchant-" + suffix, "merchantName", suffix, "merchantType", "other",
+                    "status", "active", "projectId", project, "deleteStatus", "none",
+                    "createdAt", Instant.EPOCH, "updatedAt", Instant.EPOCH), MerchantRecord.class));
+            platformRepository.saveSpace(new SpaceRecord("scoped-space-" + suffix, project,
+                    "scoped-merchant-" + suffix, null, suffix, "shop", null, null, List.of(),
+                    "active", Instant.EPOCH, Instant.EPOCH));
+            seedEntity("scoped-company-" + suffix);
+            platformRepository.businessEntityProjects.put("scoped-company-" + suffix, java.util.Set.of(project));
+            platformRepository.saveBusinessEntityBinding(new BusinessEntityBindingRecord("scoped-binding-" + suffix,
+                    "scoped-merchant-" + suffix, "scoped-company-" + suffix, "scoped-company-" + suffix,
+                    "scoped-company-" + suffix, "inactive", LocalDate.now().plusDays(1), null,
+                    null, Instant.EPOCH, Instant.EPOCH));
+        }
+    }
+
+    @Test
+    void projectListsAndTotalsStayWithinCurrentProjectForOrdinaryAdmin() {
+        twoProjectFixtures();
+        org.junit.jupiter.api.Assertions.assertAll(
+                () -> { var p = service.listProjects(1, 1); assertThat(p.total()).isEqualTo(1);
+                    assertThat(p.items()).extracting(ProjectRecord::id).containsExactly("org_1"); },
+                () -> { var p = service.listSpaces(1, 1); assertThat(p.total()).isEqualTo(1);
+                    assertThat(p.items()).extracting(SpaceRecord::id).containsExactly("scoped-space-a"); },
+                () -> { var p = service.listMerchants(true, null, 1, 1); assertThat(p.total()).isEqualTo(1);
+                    assertThat(p.items()).extracting(MerchantRecord::id).containsExactly("scoped-merchant-a"); },
+                () -> { var p = service.listBusinessEntities(1, 1); assertThat(p.total()).isEqualTo(1);
+                    assertThat(p.items()).extracting(BusinessEntityRecord::id).containsExactly("scoped-company-a"); },
+                () -> { var p = service.listBusinessEntityBindings(1, 1); assertThat(p.total()).isEqualTo(1);
+                    assertThat(p.items()).extracting(BusinessEntityBindingRecord::id).containsExactly("scoped-binding-a"); });
+    }
+
+    @Test
+    void employeeCannotReadOtherProjectThroughSpaceLookupOrWrite() {
+        twoProjectFixtures();
+        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
+                new AccessActor("employee-a", "employee-a", "employee", "org_1", null, "active"), "token", List.of()));
+        org.junit.jupiter.api.Assertions.assertAll(
+                () -> assertThat(service.spaceExists("scoped-space-b")).isFalse(),
+                () -> assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
+                        "cross-parent", null, List.of(), "org_1", null, "scoped-space-b", "空间", "shop",
+                        null, null, null, "active"))).isInstanceOf(ApiException.class),
+                () -> assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
+                        "cross-merchant", null, List.of(), "org_1", "scoped-merchant-b", null, "空间", "shop",
+                        null, null, null, "active"))).isInstanceOf(ApiException.class));
+    }
+
+    @Test
+    void spaceRequiresCurrentProjectAndNonnegativeArea() {
+        twoProjectFixtures();
+        org.junit.jupiter.api.Assertions.assertAll(
+                () -> assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
+                        "missing-project", null, List.of(), null, null, null, "空间", "shop",
+                        null, null, null, "active"))).isInstanceOf(ApiException.class),
+                () -> assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
+                        "cross-project", null, List.of(), "other-project", null, null, "空间", "shop",
+                        null, null, null, "active"))).isInstanceOf(ApiException.class),
+                () -> assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
+                        "negative-space", null, List.of(), "org_1", null, null, "空间", "shop",
+                        null, null, new BigDecimal("-1"), "active"))).isInstanceOf(ApiException.class));
+    }
+
+    @Test
+    void initialMerchantRejectsUnknownOrForeignBusinessEntities() {
+        twoProjectFixtures();
+        ObjectMapper mapper = new ObjectMapper()
+                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
+        for (String entity : List.of("missing-company", "scoped-company-b")) {
+            MerchantProfileRequest request = mapper.convertValue(Map.of(
+                    "idempotencyKey", "initial-" + entity, "merchantName", "商户", "merchantType", "other",
+                    "status", "active", "businessEntityId", entity), MerchantProfileRequest.class);
+            assertThatThrownBy(() -> service.createMerchant(request)).isInstanceOf(ApiException.class);
+        }
+    }
+
+    @Test
+    void ordinaryProfileUpdateCannotClaimOrMoveProject() {
+        twoProjectFixtures();
+        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
+                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
+        platformRepository.saveMerchant(mapper.convertValue(Map.of(
+                "id", "legacy-unassigned", "merchantName", "旧商户", "merchantType", "other", "status", "active",
+                "deleteStatus", "none", "createdAt", Instant.EPOCH, "updatedAt", Instant.EPOCH), MerchantRecord.class));
+        org.junit.jupiter.api.Assertions.assertAll(
+                () -> assertThatThrownBy(() -> service.updateMerchant("scoped-merchant-a", mapper.convertValue(Map.of(
+                        "idempotencyKey", "move-project", "merchantName", "商户", "merchantType", "other",
+                        "projectId", "other-project", "status", "active"), MerchantProfileRequest.class)))
+                        .isInstanceOf(ApiException.class),
+                () -> assertThatThrownBy(() -> service.updateMerchant("legacy-unassigned", mapper.convertValue(Map.of(
+                        "idempotencyKey", "claim-project", "merchantName", "商户", "merchantType", "other",
+                        "projectId", "org_1", "status", "active"), MerchantProfileRequest.class)))
+                        .isInstanceOf(ApiException.class));
+    }
+
+
+    @Test
+    void masterProjectBackfillChangesOnlyTheProjectAndCannotMoveIt() {
+        twoProjectFixtures();
+        MerchantRecord original = new MerchantRecord("legacy-backfill", "旧商户", "other", "financial-original",
+                "contract-original", "settlement-original", "联系人", "18800000011", "负责人", "A1",
+                BigDecimal.TEN, BigDecimal.ONE, "第二联系人", "18800000012", BigDecimal.ZERO, "lease",
+                "suspended", Instant.EPOCH, Instant.EPOCH).withArchive(Instant.EPOCH, "operator", Instant.EPOCH)
+                .withDelete("pending", "operator", Instant.EPOCH, "保留理由", Instant.EPOCH);
+        platformRepository.saveMerchant(original);
+        asMasterAuthority();
+        MerchantRecord saved = service.assignMerchantProject(original.id(),
+                new ProjectAssociationRequest("project-backfill", "org_1", "核实原始归属"));
+        assertThat(saved).isEqualTo(original.withProject("org_1"));
+        assertThat(service.assignMerchantProject(original.id(),
+                new ProjectAssociationRequest("project-backfill", "org_1", "核实原始归属"))).isEqualTo(saved);
+        assertThat(service.assignMerchantProject(original.id(),
+                new ProjectAssociationRequest("project-same", "org_1", "确认同值"))).isEqualTo(saved);
+        assertThatThrownBy(() -> service.assignMerchantProject(original.id(),
+                new ProjectAssociationRequest("project-move", "other-project", "迁移")))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("MERCHANT_PROJECT_ALREADY_ASSIGNED"));
+        assertThat(auditEventRepository.events).filteredOn(e -> "merchant.project_assigned".equals(e.eventName()))
+                .hasSize(2).allSatisfy(e -> {
+                    assertThat(e.businessId()).isEqualTo(original.id());
+                    assertThat(e.metadata()).containsEntry("projectId", "org_1").containsKey("reason");
+                    assertThat(e.actorId()).isEqualTo(SessionIssuer.MASTER_USER_ID);
+                });
+    }
+
+    @Test
+    void companyCanBeAssociatedWithTwoProjectsOnlyByRealMaster() {
+        twoProjectFixtures();
+        asMasterAuthority();
+        assertThat(service.associateBusinessEntityProject("scoped-company-a",
+                new ProjectAssociationRequest("company-link", "other-project", "确认共同运营"))).isNotNull();
+        assertThat(platformRepository.businessEntityInProject("scoped-company-a", "org_1")).isTrue();
+        assertThat(platformRepository.businessEntityInProject("scoped-company-a", "other-project")).isTrue();
+        BusinessEntityRecord created = service.createBusinessEntity(new BusinessEntityRequest(
+                "master-company-create", null, List.of(), "新关联公司", "management_company", null, null, null, "active", "org_1"));
+        assertThat(platformRepository.businessEntityInProject(created.id(), "org_1")).isTrue();
+        TestSecurityActors.asPlatformAdmin();
+        assertThatThrownBy(() -> service.associateBusinessEntityProject("scoped-company-b",
+                new ProjectAssociationRequest("ordinary-link", "org_1", "普通认领"))).isInstanceOf(ApiException.class);
+        assertThatThrownBy(() -> service.assignMerchantProject("scoped-merchant-b",
+                new ProjectAssociationRequest("ordinary-assign", "org_1", "普通认领"))).isInstanceOf(ApiException.class);
+        assertThatThrownBy(() -> service.createBusinessEntity(new BusinessEntityRequest(
+                "ordinary-company-foreign", null, List.of(), "其它项目", "management_company", null, null, null, "active", "other-project")))
+                .isInstanceOf(ApiException.class);
+    }
+
+    @Test
+    void associationRequiresExistingExplicitProjectReasonAndIdempotency() {
+        twoProjectFixtures();
+        asMasterAuthority();
+        for (ProjectAssociationRequest request : List.of(
+                new ProjectAssociationRequest(null, "org_1", "确认"),
+                new ProjectAssociationRequest("missing-project", null, "确认"),
+                new ProjectAssociationRequest("unknown-project", "missing", "确认"),
+                new ProjectAssociationRequest("missing-reason", "org_1", " "))) {
+            assertThatThrownBy(() -> service.assignMerchantProject("scoped-merchant-a", request)).isInstanceOf(ApiException.class);
+            assertThatThrownBy(() -> service.associateBusinessEntityProject("scoped-company-a", request)).isInstanceOf(ApiException.class);
+        }
+        assertThatThrownBy(() -> service.assignMerchantProject("missing",
+                new ProjectAssociationRequest("missing-merchant", "org_1", "确认"))).isInstanceOf(ApiException.class);
+        assertThatThrownBy(() -> service.associateBusinessEntityProject("missing",
+                new ProjectAssociationRequest("missing-company", "org_1", "确认"))).isInstanceOf(ApiException.class);
+    }
+
+    @Test
+    void masterHasNoGlobalListOrOrdinaryProfileBypass() {
+        twoProjectFixtures();
+        asMasterAuthority();
+        org.junit.jupiter.api.Assertions.assertAll(
+                () -> assertThat(service.listMerchants(true).total()).isZero(),
+                () -> assertThat(service.listBusinessEntities().total()).isZero(),
+                () -> assertThat(service.listProjects().total()).isZero(),
+                () -> assertThat(service.listSpaces().total()).isZero(),
+                () -> assertThat(service.listBusinessEntityBindings().total()).isZero());
+        assertThatThrownBy(() -> service.updateMerchant("scoped-merchant-a",
+                numericMerchant("master-ordinary-edit", "active", null, null, null)))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PROJECT_SCOPE_MISMATCH"));
+    }
+
     private static final class InMemoryPlatformRepository implements PlatformRepository {
         @Override public void setMerchantArchiveReason(String id, String reason) { }
+        @Override public void lockMerchant(String merchantId) { }
+        @Override public boolean businessEntityInProject(String entityId, String projectId) {
+            return projectId != null && businessEntityProjects.getOrDefault(entityId, java.util.Set.of()).contains(projectId);
+        }
+        @Override public void associateBusinessEntityProject(String entityId, String projectId) {
+            businessEntityProjects.compute(entityId, (ignored, current) -> {
+                java.util.Set<String> projects = new java.util.HashSet<>(current == null ? java.util.Set.of() : current);
+                projects.add(projectId);
+                return projects;
+            });
+        }
+        @Override public boolean assignMerchantProject(String merchantId, String projectId) {
+            MerchantRecord merchant = merchants.get(merchantId);
+            if (merchant == null || (merchant.projectId() != null && !merchant.projectId().equals(projectId))) return false;
+            merchants.put(merchantId, merchant.withProject(projectId));
+            return true;
+        }
+        @Override public void completeSpaceTypeDeleteRequest(String spaceTypeId, String status) {
+            deleteRequestedBy.remove(spaceTypeId);
+        }
         private final Map<String, BusinessEntityRecord> businessEntities = new LinkedHashMap<>();
+        private final Map<String, java.util.Set<String>> businessEntityProjects = new LinkedHashMap<>();
         private final Map<String, MerchantRecord> merchants = new LinkedHashMap<>();
         private final Map<String, ProjectRecord> projects = new LinkedHashMap<>();
         private final Map<String, SpaceRecord> spaces = new LinkedHashMap<>();
diff --git a/backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java b/backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java
index 9f25806f..8c29f53a 100644
--- a/backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java
@@ -1,4 +1,4 @@
-// 文件版本：V0.03
+// 文件版本：V0.04
 package com.complex.platform.governance.review;
 
 import com.complex.platform.support.InMemoryAuditEventRepository;
@@ -440,7 +440,7 @@ class SupervisorReviewServiceTests {
         org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                 new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                         new com.complex.platform.access.AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
-                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", "org_1", null, "active"),
+                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, "org_1", null, "active"),
                         "token", List.of()));
         assertThat(svc.listPending(200).items()).extracting(p -> p.decision().reviewId()).containsExactlyInAnyOrder(a.reviewId(), b.reviewId());
         assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).contains("governance.supervisor_review.pending_listed_cross_scope");
@@ -578,7 +578,7 @@ class SupervisorReviewServiceTests {
         org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                 new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                         new com.complex.platform.access.AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
-                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", null, null, "active"),
+                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, null, null, "active"),
                         "test", List.of()));
     }
 
@@ -783,7 +783,7 @@ class SupervisorReviewServiceTests {
         org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                 new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                         new com.complex.platform.access.AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
-                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", "org_1", null, "active"),
+                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, "org_1", null, "active"),
                         "token", List.of()));
         assertThat(service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest("esc-approve-master", "note", List.of("att"), 3, "approved", "ok"))
                 .reviewStatus()).isEqualTo("approved");
@@ -809,7 +809,7 @@ class SupervisorReviewServiceTests {
         org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                 new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                         new com.complex.platform.access.AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
-                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", "org_1", null, "active"),
+                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, "org_1", null, "active"),
                         "token", List.of()));
         assertThat(service.forward(pending.reviewId(), new SupervisorReviewForwardRequest(
                 "top-forward-master", null, "usr_anyone", "handle")).forwardedTo()).isEqualTo("usr_anyone");
@@ -831,7 +831,7 @@ class SupervisorReviewServiceTests {
         org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                 new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                         new com.complex.platform.access.AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
-                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", "org_1", null, "active"),
+                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, "org_1", null, "active"),
                         "token", List.of()));
         assertThatThrownBy(() -> service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                 "top-chain-master-early", "note", List.of("att"), 0, "approved", "too early")))
@@ -878,7 +878,7 @@ class SupervisorReviewServiceTests {
         org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                 new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                         new com.complex.platform.access.AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
-                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", "org_1", null, "active"),
+                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, "org_1", null, "active"),
                         "token", List.of())); // 红线④：只有密钥主体可整批看待审
         assertThat(service.listPending(10).items()).extracting(item -> item.decision().reviewId())
                 .contains(money.reviewId(), normal.reviewId());
diff --git a/backend/src/test/java/com/complex/platform/leasing/LeasingServiceTests.java b/backend/src/test/java/com/complex/platform/leasing/LeasingServiceTests.java
index a3942057..635a8688 100644
--- a/backend/src/test/java/com/complex/platform/leasing/LeasingServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/leasing/LeasingServiceTests.java
@@ -1,4 +1,5 @@
 package com.complex.platform.leasing;
+// 文件版本：V0.02
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
@@ -212,6 +213,7 @@ class LeasingServiceTests {
 
     @Test
     void signIntentWithoutMerchantAutoCreatesFormalMerchant() {
+        when(authz.currentProjectScopeQuietly()).thenReturn(Optional.of("lease-current-project"));
         // 1.4 决策A：merchant_id 空 → 签约自动建正式商户，带入线索联系人/业态、四类主体默认同一公司。
         when(contractService.createDraft(any())).thenReturn(new ContractRecord(
                 "ctr_auto", "mch_auto", "cs", null, "cs", LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"),
@@ -222,6 +224,11 @@ class LeasingServiceTests {
                 null, null, null, "leasing_auto", "active", Instant.now(), Instant.now()));
         LeasingLeadRecord lead = service.createLead(new LeasingLeadRequest(
                 "钱七", "13800001111", "online_inquiry", "retail", "sp_1", null, null, "k1"));
+        // 本例验证已归属意向的签约调用点；线索创建归属由其所属模块另审。
+        repo.saveLead(new LeasingLeadRecord(lead.id(), lead.contactName(), lead.contactPhone(), lead.source(),
+                lead.intendedBusinessType(), lead.intendedSpaceId(), lead.intendedAreaSqm(), lead.brandId(),
+                lead.ownerUserId(), lead.status(), lead.lostOrHoldReason(), lead.lastFollowUpAt(),
+                "lease-current-project", lead.createdAt(), lead.updatedAt()));
         LeasingIntentRecord intent = service.createIntent(new LeasingIntentRequest(lead.id(), "sp_1", null, Map.of(), "k2"));
         service.advanceIntent(intent.id(), new LeasingIntentAdvanceRequest("pending_signing", null, "k4"));
 
@@ -231,19 +238,22 @@ class LeasingServiceTests {
 
         org.mockito.ArgumentCaptor<MerchantProfileRequest> captor =
                 org.mockito.ArgumentCaptor.forClass(MerchantProfileRequest.class);
-        verify(platformService).createMerchant(captor.capture());
+        var order = org.mockito.Mockito.inOrder(platformService);
+        order.verify(platformService).createMerchant(captor.capture());
         MerchantProfileRequest req = captor.getValue();
         assertThat(req.contactName()).isEqualTo("钱七");
         assertThat(req.contactPhone()).isEqualTo("13800001111");
         assertThat(req.merchantType()).isEqualTo("retail");
-        assertThat(req.contractEntityId()).isEqualTo("cs");
-        assertThat(req.businessEntityId()).isEqualTo("cs");
-        assertThat(req.settlementEntityId()).isEqualTo("cs");
+        org.junit.jupiter.api.Assertions.assertAll(
+                () -> assertThat(req.projectId()).isEqualTo("lease-current-project"),
+                () -> assertThat(req.contractEntityId()).isNull(),
+                () -> assertThat(req.businessEntityId()).isNull(),
+                () -> assertThat(req.settlementEntityId()).isNull());
 
         // AUD-003：自动建商户后必建"四主体"权威绑定（经营/合同/结算主体默认同一公司=cs）。
         org.mockito.ArgumentCaptor<com.complex.platform.governance.dto.BusinessEntityBindingRequest> bindingCaptor =
                 org.mockito.ArgumentCaptor.forClass(com.complex.platform.governance.dto.BusinessEntityBindingRequest.class);
-        verify(platformService).createBusinessEntityBinding(bindingCaptor.capture());
+        order.verify(platformService).createBusinessEntityBinding(bindingCaptor.capture());
         com.complex.platform.governance.dto.BusinessEntityBindingRequest binding = bindingCaptor.getValue();
         assertThat(binding.merchantId()).isEqualTo("mch_auto");
         assertThat(binding.operationEntityId()).isEqualTo("cs");
diff --git a/backend/src/test/java/com/complex/platform/leasing/LeasingSignAtomicityJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/leasing/LeasingSignAtomicityJdbcIntegrationTests.java
index 0e60c08e..ac496f13 100644
--- a/backend/src/test/java/com/complex/platform/leasing/LeasingSignAtomicityJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/leasing/LeasingSignAtomicityJdbcIntegrationTests.java
@@ -1,5 +1,5 @@
 package com.complex.platform.leasing;
-// 文件版本：V0.01
+// 文件版本：V0.02
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
@@ -36,6 +36,7 @@ class LeasingSignAtomicityJdbcIntegrationTests {
     @Autowired private JdbcTemplate jdbcTemplate;
 
     private String suffix;
+    private String projectId;
     private String subject;   // contract_subject_id 标记（同时是 merchant/binding 三主体快照）
     private String leadId;
     private String intentId;
@@ -51,12 +52,14 @@ class LeasingSignAtomicityJdbcIntegrationTests {
                 + "(SELECT id FROM contracts WHERE contract_subject_id = ?)", subject);
         jdbcTemplate.update("DELETE FROM contracts WHERE contract_subject_id = ?", subject);
         jdbcTemplate.update("DELETE FROM business_entity_bindings WHERE contract_entity_id = ?", subject);
-        jdbcTemplate.update("DELETE FROM merchants WHERE contract_entity_id = ?", subject);
+        jdbcTemplate.update("DELETE FROM merchants WHERE project_id = ?", projectId);
+        jdbcTemplate.update("DELETE FROM business_entity_projects WHERE project_id = ?", projectId);
         jdbcTemplate.update("DELETE FROM business_entities WHERE id = ?", subject); // M009-006 夹具主体
         jdbcTemplate.update("DELETE FROM leasing_intents WHERE id = ?", intentId);
         jdbcTemplate.update("DELETE FROM leasing_leads WHERE id = ?", leadId);
         jdbcTemplate.update("DELETE FROM idempotency_records WHERE idempotency_key LIKE ?", "%" + suffix + "%");
         jdbcTemplate.update("DELETE FROM spaces WHERE id LIKE ?", "sp_%" + suffix);
+        jdbcTemplate.update("DELETE FROM projects WHERE id = ?", projectId);
     }
 
     /** AUD-002：签约中途失败（草稿空间不存在）→ 商户/四主体绑定/合同全回滚，无孤儿；线索未置 signed。 */
@@ -103,10 +106,35 @@ class LeasingSignAtomicityJdbcIntegrationTests {
         assertThat(countContracts()).isEqualTo(1);
     }
 
+
+    @Test
+    void futureSignKeepsCurrentSubjectsEmptyAndRetainsIntentProject() {
+        String spaceId = "sp_future_" + newSuffix();
+        seedSpace(spaceId);
+        seed(spaceId);
+        LocalDate future = LocalDate.now(java.time.ZoneOffset.UTC).plusYears(1);
+        ContractRecord contract = leasingService.signIntent(intentId, new LeasingSignRequest(
+                null, subject, null, null, future, future.plusYears(1), "sign_" + suffix));
+        Map<String, Object> merchant = jdbcTemplate.queryForMap(
+                "SELECT project_id, business_entity_id, contract_entity_id, settlement_entity_id FROM merchants WHERE id = ?",
+                contract.merchantId());
+        assertThat(merchant).containsEntry("project_id", projectId)
+                .containsEntry("business_entity_id", null).containsEntry("contract_entity_id", null)
+                .containsEntry("settlement_entity_id", null);
+        assertThat(jdbcTemplate.queryForMap(
+                "SELECT status, effective_from, effective_to FROM business_entity_bindings WHERE merchant_id = ?",
+                contract.merchantId())).containsEntry("status", "inactive")
+                .containsEntry("effective_from", java.sql.Date.valueOf(future)).containsEntry("effective_to", null);
+        assertThat(countActiveBindings()).isZero();
+        assertThat(countMerchants()).isEqualTo(1);
+    }
+
     // ---- helpers ----
 
     private String newSuffix() {
         suffix = UUID.randomUUID().toString().substring(0, 8);
+        projectId = "lease_project_" + suffix;
+        jdbcTemplate.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES (?, 'complex', '签约项目', 'active', now(), now())", projectId);
         subject = "be_test_" + suffix;
         leadId = "lead_aud_" + suffix;
         intentId = "intent_aud_" + suffix;
@@ -114,27 +142,30 @@ class LeasingSignAtomicityJdbcIntegrationTests {
     }
 
     private void seed(String intendedSpaceId) {
-        TestSecurityActors.asPlatformAdmin();
+        var actor = new com.complex.platform.access.AccessActor("usr_admin", "platform_admin_user", "platform_admin", projectId, null, "active");
+        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
+                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(actor, "token", java.util.List.of()));
         Instant now = Instant.now();
         // M009-006：四类主体绑定要求主体真实存在（BEQR-143/PGM-026），合同主体须先在 business_entities 建档。
         jdbcTemplate.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at) "
                 + "VALUES (?, ?, 'management_company', 'active', now(), now())", subject, "签约测试合同主体");
+        jdbcTemplate.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES (?, ?)", subject, projectId);
         leasingRepository.saveLead(new LeasingLeadRecord(leadId, "签约测试联系人", "13800002222", "referral",
-                "retail", intendedSpaceId, null, null, "usr_sales", "negotiating", null, null, null, now, now));
+                "retail", intendedSpaceId, null, null, "usr_sales", "negotiating", null, null, projectId, now, now));
         leasingRepository.saveIntent(new LeasingIntentRecord(intentId, leadId, "pending_signing", intendedSpaceId,
-                null, Map.of(), now, "usr_supervisor", null, now, now));
+                null, Map.of(), now, "usr_supervisor", projectId, now, now));
     }
 
     private void seedSpace(String spaceId) {
         jdbcTemplate.update("INSERT INTO spaces (id, project_id, merchant_id, parent_space_id, space_name, "
                 + "space_type, floor_no, area_square_meter, status, created_at, updated_at) "
-                + "VALUES (?, NULL, NULL, NULL, ?, 'shop', '1F', 88.0, 'active', now(), now())",
-                spaceId, "签约测试铺位");
+                + "VALUES (?, ?, NULL, NULL, ?, 'shop', '1F', 88.0, 'active', now(), now())",
+                spaceId, projectId, "签约测试铺位");
     }
 
     private int countMerchants() {
         return jdbcTemplate.queryForObject(
-                "SELECT COUNT(*) FROM merchants WHERE contract_entity_id = ?", Integer.class, subject);
+                "SELECT COUNT(*) FROM merchants WHERE project_id = ?", Integer.class, projectId);
     }
 
     private int countActiveBindings() {
```

END_CURRENT_M009_B4_SELF_V0.02

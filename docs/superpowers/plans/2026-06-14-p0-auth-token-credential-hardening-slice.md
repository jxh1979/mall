# P0 Auth Token Credential Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove raw Bearer access tokens from Spring Security credentials while preserving P0 auth, refresh, logout, and audit behavior.

**Architecture:** Keep the existing local/dev/test smoke authentication flow. The Bearer filter should authenticate the same user and authority but store no raw token in `Authentication.credentials`.

**Tech Stack:** Java 21, Spring Boot 4.x, JUnit 6, MockMvc, Maven.

---

## File Structure

- Modify: `backend/src/test/java/com/complex/platform/auth/AuthControllerTests.java`
  - Add a controller-level regression test that logs in, calls an authenticated endpoint, and asserts the server-side `Authentication.credentials` is `null`.
- Modify: `backend/src/main/java/com/complex/platform/auth/BearerAuthenticationFilter.java`
  - Change `UsernamePasswordAuthenticationToken` construction to use `null` credentials.

## Task 1: Add Regression Test

**Files:**
- Modify: `backend/src/test/java/com/complex/platform/auth/AuthControllerTests.java`

- [ ] **Step 1: Write the failing test**

Add an authenticated test-only endpoint inside `AuthControllerTests`:

```java
@RestController
static class AuthenticationProbeController {
    @GetMapping("/api/v1/auth/probe")
    Map<String, Object> probe(Authentication authentication) {
        return Map.of(
                "authenticated", authentication != null && authentication.isAuthenticated(),
                "credentials_present", authentication != null && authentication.getCredentials() != null
        );
    }
}
```

Then add:

```java
@Test
void bearerAuthenticationDoesNotExposeRawTokenAsCredentials() throws Exception {
    JsonNode login = login();
    String accessToken = login.at("/data/access_token").asText();

    mockMvc.perform(get("/api/v1/auth/probe")
                    .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.authenticated").value(true))
            .andExpect(jsonPath("$.data.credentials_present").value(false));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
cd /home/james/complex-management/backend
mvn -q -Dtest=AuthControllerTests#bearerAuthenticationDoesNotExposeRawTokenAsCredentials test
```

Expected: FAIL because current filter puts the raw Bearer token in credentials.

## Task 2: Minimal Implementation

**Files:**
- Modify: `backend/src/main/java/com/complex/platform/auth/BearerAuthenticationFilter.java`

- [ ] **Step 1: Replace raw token credentials**

Change:

```java
new UsernamePasswordAuthenticationToken(
        user,
        token,
        List.of(new SimpleGrantedAuthority("ROLE_P0_SMOKE"))
);
```

to:

```java
new UsernamePasswordAuthenticationToken(
        user,
        null,
        List.of(new SimpleGrantedAuthority("ROLE_P0_SMOKE"))
);
```

- [ ] **Step 2: Run targeted tests**

Run:

```bash
cd /home/james/complex-management/backend
mvn -q -Dtest=AuthControllerTests,AuthServiceTests test
```

Expected: PASS.

## Task 3: P0 Verification

- [ ] **Step 1: Run contract verification**

Run:

```bash
cd /home/james/complex-management
npm run verify:p0-contract
```

Expected: PASS.

- [ ] **Step 2: Run full P0 verification**

Run:

```bash
cd /home/james/complex-management
/opt/complex/scripts/complex-verify-p0.sh
```

Expected: `P0 verification PASS`.

## Risk And Rollback

- Risk: Probe endpoint must remain test-only and not be added to production code.
- Rollback: Restore `BearerAuthenticationFilter` credentials argument to previous value and remove the test, then rerun `AuthControllerTests`.

## Self-Review

- Spec coverage: Covers the security review finding about raw token exposure in Spring Security credentials.
- Placeholder scan: No TBD or TODO.
- Type consistency: Test uses Spring `Authentication`, `Map`, `GetMapping`, and `RestController`; imports must be added in the test file.

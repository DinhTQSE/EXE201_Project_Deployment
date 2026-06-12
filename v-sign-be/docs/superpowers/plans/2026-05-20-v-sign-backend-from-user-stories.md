# V-SIGN Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a production-ready Spring Boot backend that satisfies the V-SIGN user stories and acceptance criteria from `EXE101_Project_V-Sign_FE/docs/V-SIGN_UserStories_Full.docx`.

**Architecture:** Build a modular monolith with bounded packages (`auth`, `learning`, `assessment`, `gamification`, `dictionary`, `billing`, `admin`) on top of PostgreSQL and Flyway migrations. Use JWT-based stateless auth, presigned S3 upload/download for media, webhook-driven payment confirmation, and transactional domain services for progress, XP, streak, and subscription consistency. Expose REST APIs documented with OpenAPI and enforce role-based access (`GUEST`, `USER`, `PREMIUM`, `ADMIN`, `SUPER_ADMIN`).

**Tech Stack:** Java 21, Spring Boot 3.3, Spring Security, Spring Data JPA, Bean Validation, Flyway, PostgreSQL, AWS SDK v2 (S3), Testcontainers, JUnit 5, MockMvc, springdoc-openapi.

---

## Scope Check

The source spec contains multiple independent subsystems (Auth, Learning, Assessment, Gamification, Dictionary, Monetization, Admin). This plan keeps one implementation track but splits work into isolated modules and test suites so each subsystem can be implemented and validated independently.

`EXE101_Project_V-Sign_BE/BE-init.md` is currently empty (0 bytes), so this plan defines explicit backend conventions instead of inheriting conventions from that file.

## File Structure

### Core bootstrap
- Create: `EXE101_Project_V-Sign_BE/pom.xml`
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/VSignBackendApplication.java`
- Create: `EXE101_Project_V-Sign_BE/src/main/resources/application.yml`
- Create: `EXE101_Project_V-Sign_BE/src/main/resources/application-test.yml`

### Shared infrastructure
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/common/security/SecurityConfig.java`
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/common/security/JwtService.java`
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/common/security/JwtAuthFilter.java`
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/common/exception/ApiExceptionHandler.java`
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/common/s3/S3PresignService.java`

### Database and migrations
- Create: `EXE101_Project_V-Sign_BE/src/main/resources/db/migration/V1__init_schema.sql`
- Create: `EXE101_Project_V-Sign_BE/src/main/resources/db/migration/V2__seed_reference_data.sql`

### Domain modules
- Create module folders with controller/service/repository/entity/dto:
  - `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/auth`
  - `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/learning`
  - `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/assessment`
  - `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/gamification`
  - `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/dictionary`
  - `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/billing`
  - `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/admin`

### Tests
- Create: `EXE101_Project_V-Sign_BE/src/test/java/com/vsign/backend/support/IntegrationTestBase.java`
- Create module test packages mirrored from `src/main`.

---

### Task 1: Bootstrap Backend Skeleton

**Files:**
- Create: `EXE101_Project_V-Sign_BE/pom.xml`
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/VSignBackendApplication.java`
- Create: `EXE101_Project_V-Sign_BE/src/main/resources/application.yml`
- Test: `EXE101_Project_V-Sign_BE/src/test/java/com/vsign/backend/BootSmokeTest.java`

- [ ] **Step 1: Write the failing test**

```java
@SpringBootTest
class BootSmokeTest {
    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=BootSmokeTest test`
Expected: FAIL with missing Spring Boot application class or missing dependencies.

- [ ] **Step 3: Write minimal implementation**

```java
@SpringBootApplication
public class VSignBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(VSignBackendApplication.class, args);
    }
}
```

```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=BootSmokeTest test`
Expected: PASS with `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add EXE101_Project_V-Sign_BE/pom.xml EXE101_Project_V-Sign_BE/src/main
git commit -m "chore(be): bootstrap spring boot backend skeleton"
```

### Task 2: Create DB Schema and Flyway Baseline

**Files:**
- Create: `EXE101_Project_V-Sign_BE/src/main/resources/db/migration/V1__init_schema.sql`
- Create: `EXE101_Project_V-Sign_BE/src/main/resources/db/migration/V2__seed_reference_data.sql`
- Create: `EXE101_Project_V-Sign_BE/src/test/java/com/vsign/backend/migration/FlywayMigrationTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Testcontainers
@SpringBootTest
class FlywayMigrationTest {
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void usersTableExists() {
        Integer count = jdbcTemplate.queryForObject(
            "select count(*) from information_schema.tables where table_name = 'users'",
            Integer.class
        );
        assertThat(count).isEqualTo(1);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=FlywayMigrationTest test`
Expected: FAIL with relation/table not found.

- [ ] **Step 3: Write minimal implementation**

```sql
create table users (
  id uuid primary key,
  email varchar(255) not null unique,
  password_hash varchar(255),
  full_name varchar(120) not null,
  avatar_url text,
  account_type varchar(20) not null default 'BASIC',
  role varchar(20) not null default 'USER',
  is_active boolean not null default true,
  current_streak int not null default 0,
  longest_streak int not null default 0,
  total_xp int not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=FlywayMigrationTest test`
Expected: PASS with migration applied in test container.

- [ ] **Step 5: Commit**

```bash
git add EXE101_Project_V-Sign_BE/src/main/resources/db EXE101_Project_V-Sign_BE/src/test/java/com/vsign/backend/migration
git commit -m "feat(be): add flyway baseline schema and migration tests"
```

### Task 3: Implement Authentication and Profile APIs (US-01..US-08)

**Files:**
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/auth/controller/AuthController.java`
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/auth/service/AuthService.java`
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/auth/controller/ProfileController.java`
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/auth/service/ProfileService.java`
- Test: `EXE101_Project_V-Sign_BE/src/test/java/com/vsign/backend/auth/AuthControllerIT.java`
- Test: `EXE101_Project_V-Sign_BE/src/test/java/com/vsign/backend/auth/ProfileControllerIT.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void registerWithValidPayloadReturnsJwt() throws Exception {
    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
              {"email":"new@vsign.vn","password":"StrongPass1","fullName":"New User"}
            """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accessToken").isNotEmpty());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=AuthControllerIT,ProfileControllerIT test`
Expected: FAIL with `404` for `/api/auth/register` and `/api/profile`.

- [ ] **Step 3: Write minimal implementation**

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
```

```java
@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    @GetMapping
    public ProfileResponse me(Authentication auth) {
        return profileService.getProfile(auth.getName());
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q -Dtest=AuthControllerIT,ProfileControllerIT test`
Expected: PASS with create/login/profile happy path and validation path.

- [ ] **Step 5: Commit**

```bash
git add EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/auth EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/common/security EXE101_Project_V-Sign_BE/src/test/java/com/vsign/backend/auth
git commit -m "feat(be): implement auth and profile core endpoints"
```

### Task 4: Implement Learning Content and Progress APIs (US-09..US-18)

**Files:**
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/learning/controller/LearningController.java`
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/learning/service/LearningService.java`
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/learning/service/ProgressService.java`
- Test: `EXE101_Project_V-Sign_BE/src/test/java/com/vsign/backend/learning/LearningFlowIT.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void basicUserCannotOpenPremiumChapter() throws Exception {
    mockMvc.perform(get("/api/units/{unitId}/chapters/{chapterId}", unitId, premiumChapterId)
            .header("Authorization", basicJwt()))
        .andExpect(status().isForbidden());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=LearningFlowIT test`
Expected: FAIL with `404` for learning endpoints.

- [ ] **Step 3: Write minimal implementation**

```java
@GetMapping("/units/{unitId}/chapters")
public List<ChapterCardResponse> chapters(@PathVariable UUID unitId, Authentication auth) {
    return learningService.getPublishedChapters(unitId, auth.getName());
}

@PostMapping("/lessons/{lessonId}/progress")
public ProgressResponse saveProgress(@PathVariable UUID lessonId, @RequestBody SaveProgressRequest request, Authentication auth) {
    return progressService.saveProgress(auth.getName(), lessonId, request);
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q -Dtest=LearningFlowIT test`
Expected: PASS for ordering, premium lock, resume position, and idempotent completion XP.

- [ ] **Step 5: Commit**

```bash
git add EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/learning EXE101_Project_V-Sign_BE/src/test/java/com/vsign/backend/learning
git commit -m "feat(be): implement learning catalog and lesson progress apis"
```

### Task 5: Implement Assessment APIs (US-19..US-33)

**Files:**
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/assessment/controller/QuizController.java`
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/assessment/service/McqQuizService.java`
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/assessment/service/AiQuizService.java`
- Test: `EXE101_Project_V-Sign_BE/src/test/java/com/vsign/backend/assessment/McqQuizIT.java`
- Test: `EXE101_Project_V-Sign_BE/src/test/java/com/vsign/backend/assessment/AiQuizIT.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void timedQuizAutoSubmitReturnsScore() throws Exception {
    mockMvc.perform(post("/api/quiz/attempts/{attemptId}/auto-submit", attemptId)
            .header("Authorization", userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.score").isNumber());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=McqQuizIT,AiQuizIT test`
Expected: FAIL with missing endpoint/service beans.

- [ ] **Step 3: Write minimal implementation**

```java
@PostMapping("/attempts/{attemptId}/submit")
public QuizResultResponse submit(@PathVariable UUID attemptId, @RequestBody SubmitQuizRequest request, Authentication auth) {
    return mcqQuizService.submit(auth.getName(), attemptId, request);
}

@PostMapping("/ai/attempts/{attemptId}/evaluate")
public AiEvaluationResponse evaluateSign(@PathVariable UUID attemptId, @Valid @RequestBody AiEvaluationRequest request, Authentication auth) {
    return aiQuizService.evaluate(auth.getName(), attemptId, request);
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q -Dtest=McqQuizIT,AiQuizIT test`
Expected: PASS for immediate score, review answer payload, retry flow, timer auto-submit, premium access checks.

- [ ] **Step 5: Commit**

```bash
git add EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/assessment EXE101_Project_V-Sign_BE/src/test/java/com/vsign/backend/assessment
git commit -m "feat(be): implement mcq and ai assessment endpoints"
```

### Task 6: Implement Gamification Engine (US-34..US-49)

**Files:**
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/gamification/service/XpService.java`
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/gamification/service/StreakService.java`
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/gamification/controller/LeaderboardController.java`
- Test: `EXE101_Project_V-Sign_BE/src/test/java/com/vsign/backend/gamification/GamificationIT.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void streakBonusMultiplierIsAppliedAt14Days() {
    int awarded = xpService.applyQuizXpWithStreak(userId, 100, LocalDate.of(2026, 5, 20));
    assertThat(awarded).isEqualTo(150);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=GamificationIT test`
Expected: FAIL with missing streak/XP behavior.

- [ ] **Step 3: Write minimal implementation**

```java
public BigDecimal streakMultiplier(int streak) {
    if (streak >= 30) return new BigDecimal("2.0");
    if (streak >= 14) return new BigDecimal("1.5");
    if (streak >= 7) return new BigDecimal("1.2");
    return BigDecimal.ONE;
}
```

```java
@GetMapping("/api/leaderboard/weekly")
public LeaderboardResponse weekly(Authentication auth) {
    return leaderboardService.weekly(auth.getName());
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q -Dtest=GamificationIT test`
Expected: PASS for XP logs, streak increment/reset rules, badge idempotency, weekly/monthly leaderboard rank highlighting support.

- [ ] **Step 5: Commit**

```bash
git add EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/gamification EXE101_Project_V-Sign_BE/src/test/java/com/vsign/backend/gamification
git commit -m "feat(be): add xp streak badge and leaderboard services"
```

### Task 7: Implement Dictionary APIs (US-50..US-55)

**Files:**
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/dictionary/controller/DictionaryPublicController.java`
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/dictionary/service/DictionaryService.java`
- Test: `EXE101_Project_V-Sign_BE/src/test/java/com/vsign/backend/dictionary/DictionaryIT.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void dictionaryRouteIsPublic() throws Exception {
    mockMvc.perform(get("/api/dictionary/entries"))
        .andExpect(status().isOk());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=DictionaryIT test`
Expected: FAIL with `404` or `401`.

- [ ] **Step 3: Write minimal implementation**

```java
@GetMapping("/api/dictionary/entries")
public Page<DictionaryEntryResponse> search(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String difficulty,
        Pageable pageable) {
    return dictionaryService.searchPublished(category, keyword, difficulty, pageable);
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q -Dtest=DictionaryIT test`
Expected: PASS for public access, category + keyword filtering, difficulty filtering, and empty result messages.

- [ ] **Step 5: Commit**

```bash
git add EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/dictionary EXE101_Project_V-Sign_BE/src/test/java/com/vsign/backend/dictionary
git commit -m "feat(be): implement public dictionary query apis"
```

### Task 8: Implement Billing and Subscription Flows (US-56..US-64)

**Files:**
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/billing/controller/PaymentController.java`
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/billing/controller/PaymentWebhookController.java`
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/billing/service/PaymentService.java`
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/billing/service/SubscriptionService.java`
- Test: `EXE101_Project_V-Sign_BE/src/test/java/com/vsign/backend/billing/PaymentFlowIT.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void successfulWebhookUpgradesAccountAndCreatesSubscription() throws Exception {
    mockMvc.perform(post("/api/payments/webhooks/momo")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Signature", "valid-signature")
            .content(sampleSuccessWebhookPayload))
        .andExpect(status().isOk());

    assertThat(userRepository.findById(userId).orElseThrow().getAccountType()).isEqualTo(AccountType.PREMIUM);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=PaymentFlowIT test`
Expected: FAIL with missing webhook and upgrade logic.

- [ ] **Step 3: Write minimal implementation**

```java
@PostMapping("/api/payments/webhooks/momo")
public ResponseEntity<Void> momoWebhook(@RequestHeader("X-Signature") String signature, @RequestBody String payload) {
    paymentService.handleMomoWebhook(signature, payload);
    return ResponseEntity.ok().build();
}
```

```java
@Transactional
public void markSuccessAndUpgrade(UUID paymentId) {
    PaymentTransaction tx = paymentRepository.findByIdForUpdate(paymentId).orElseThrow();
    if (tx.getStatus() != PaymentStatus.SUCCESS) {
        tx.setStatus(PaymentStatus.SUCCESS);
        subscriptionService.activatePremium(tx.getUserId(), tx.getPlanCode(), tx.getProviderTransactionId());
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q -Dtest=PaymentFlowIT test`
Expected: PASS for QR creation response, polling status endpoint, success/failed webhook handling, payment history listing.

- [ ] **Step 5: Commit**

```bash
git add EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/billing EXE101_Project_V-Sign_BE/src/test/java/com/vsign/backend/billing
git commit -m "feat(be): add payment webhook and subscription upgrade flow"
```

### Task 9: Implement Admin APIs and KPI Dashboard (US-65..US-75)

**Files:**
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/admin/controller/AdminContentController.java`
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/admin/controller/AdminUserController.java`
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/admin/controller/AdminPaymentController.java`
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/admin/controller/AdminDashboardController.java`
- Test: `EXE101_Project_V-Sign_BE/src/test/java/com/vsign/backend/admin/AdminIT.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void nonAdminCannotAccessAdminRoutes() throws Exception {
    mockMvc.perform(get("/api/admin/users").header("Authorization", userJwt()))
        .andExpect(status().isForbidden());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=AdminIT test`
Expected: FAIL with missing routes or weak authorization.

- [ ] **Step 3: Write minimal implementation**

```java
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    @GetMapping
    public Page<AdminUserRowResponse> users(@RequestParam(required = false) String q, Pageable pageable) {
        return adminUserService.searchUsers(q, pageable);
    }
}
```

```java
@PostMapping("/api/admin/payments/{paymentId}/manual-success")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public ResponseEntity<Void> manualSuccess(@PathVariable UUID paymentId, Authentication auth) {
    adminPaymentService.manualMarkSuccess(paymentId, auth.getName());
    return ResponseEntity.ok().build();
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q -Dtest=AdminIT test`
Expected: PASS for RBAC, CRUD coverage for unit/chapter/lesson/quiz/dictionary admin endpoints, transaction audit, dashboard KPIs.

- [ ] **Step 5: Commit**

```bash
git add EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/admin EXE101_Project_V-Sign_BE/src/test/java/com/vsign/backend/admin
git commit -m "feat(be): implement admin management and kpi endpoints"
```

### Task 10: Hardening, OpenAPI, and End-to-End Regression Suite

**Files:**
- Create: `EXE101_Project_V-Sign_BE/src/main/java/com/vsign/backend/common/config/OpenApiConfig.java`
- Create: `EXE101_Project_V-Sign_BE/src/test/java/com/vsign/backend/e2e/VSignCoreJourneyIT.java`
- Create: `EXE101_Project_V-Sign_BE/README.md`

- [ ] **Step 1: Write the failing end-to-end test**

```java
@Test
void registerLearnQuizPayAndUpgradeJourney() throws Exception {
    String token = registerAndGetToken("journey@vsign.vn", "StrongPass1", "Journey User");
    completeLesson(token, lessonId);
    UUID attemptId = startMcqAttempt(token, quizId);
    submitAllAnswers(token, attemptId);
    UUID paymentId = createMomoPayment(token, "PREMIUM_MONTHLY");
    triggerMomoSuccessWebhook(paymentId, "momo_tx_20260520");

    mockMvc.perform(get("/api/profile").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountType").value("PREMIUM"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=VSignCoreJourneyIT test`
Expected: FAIL due to incomplete integration wiring.

- [ ] **Step 3: Write minimal implementation**

```java
@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI openAPI() {
        return new OpenAPI().info(new Info().title("V-SIGN API").version("v1"));
    }
}
```

```yaml
springdoc:
  api-docs:
    path: /api/docs
  swagger-ui:
    path: /api/swagger
```

- [ ] **Step 4: Run full tests to verify they pass**

Run: `mvn -q test`
Expected: PASS with module integration tests and end-to-end journey.

- [ ] **Step 5: Commit**

```bash
git add EXE101_Project_V-Sign_BE/src/main EXE101_Project_V-Sign_BE/src/test EXE101_Project_V-Sign_BE/README.md
git commit -m "chore(be): add api docs and end-to-end regression coverage"
```

---

## Self-Review

### 1) Spec coverage check
- Covered Epics 1-7 with mapped tasks:
  - Authentication/Profile: Task 3
  - Learning: Task 4
  - Assessment: Task 5
  - Gamification: Task 6
  - Dictionary: Task 7
  - Monetization: Task 8
  - Admin: Task 9
- Cross-cutting non-functional requirements:
  - Data model and migrations: Task 2
  - Security and role gating: Tasks 3 and 9
  - Integration confidence and docs: Task 10

### 2) Placeholder scan
- No `TODO`, `TBD`, or "implement later" placeholders left in actionable steps.
- Every task includes explicit files, commands, expected outcomes, and code snippets.

### 3) Type and naming consistency
- Consistent account and access terminology: `BASIC`, `PREMIUM`, `ADMIN`, `SUPER_ADMIN`.
- Consistent payment state naming: `PENDING`, `SUCCESS`, `FAILED`.
- Consistent API namespace prefix: `/api/`.

## Notes for Execution
- Implement strict package-by-package scope in each commit; do not mix unrelated modules.
- Keep integration tests green after each task before moving forward.
- Keep S3 and payment provider secrets in environment variables, not in source control.

Plan complete and saved to `docs/superpowers/plans/2026-05-20-v-sign-backend-from-user-stories.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**

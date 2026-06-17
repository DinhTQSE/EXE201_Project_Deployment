# PayOS Payment Module Port — V-Sign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port the PayOS payment, tier subscription, and webhook-confirmation module from HistoryTalk into the V-Sign Spring Boot backend under a new `com.vsign.backend.payment` package.

**Architecture:** New `com.vsign.backend.payment` package is fully independent of the existing `monetization` module. The verified POST webhook is the only path to activating a paid `UserTierEntity`. The browser return URL only syncs local order status for UI. Pessimistic row locks guard against duplicate webhooks and race conditions.

**Tech Stack:** Spring Boot 3.3.0, Java 21, Maven, Spring Security (stateless JWT), Spring Data JPA + Flyway (schema via `${DB_SCHEMA}`), Lombok, PayOS Java SDK `vn.payos:payos-java:2.0.1`, PostgreSQL.

## Global Constraints

- Package root: `com.vsign.backend` — no `com.historytalk` references anywhere.
- Servlet context path `spring.mvc.servlet.path=/V-sign` — webhook URL registered in PayOS dashboard **must** include `/V-sign`, e.g. `https://api.example.com/V-sign/api/v1/payments/payos/webhook`.
- `JwtService.Principal` is `record Principal(String email, String role)` — it stores **email**, not userId. Controllers call `principal.email()` and pass it to services; services look up `UserEntity` via `userRepository.findByEmailIgnoreCase(email)`.
- `UserEntity` PK field: `id` (UUID), table: `users`, accessor `user.getId()` / `user.setId(uuid)` — public setter exists.
- No token/quota fields on `UserEntity`. Skip all token-reset logic. Keep `limited_token` column in `tier` table for future use only.
- Order description prefix: `VSIGN` (replaces `HISTALK` from source).
- Flyway next version: **V22**. Use **unqualified** table names — Flyway sets `search_path` via `spring.flyway.schemas=${DB_SCHEMA}`.
- Do **not** modify any existing `monetization` module class.
- `AuthService` uses an explicit constructor (no Lombok) — new dependencies must be added to the constructor signature.
- Existing `PaymentController` at `/api/v1/payments` handles `/orders` and `/{transactionId}` — the new `PayOSPaymentController` adds `/tiers`, `/checkout`, `/me`, `/payos/return` without touching existing code.

---

## File Map

### New Files
| Path (relative to `src/main/java/com/vsign/backend/`) | Responsibility |
|------|---------------|
| `payment/config/PayOSConfig.java` | `@ConfigurationProperties` bean that creates the `PayOS` SDK client |
| `payment/persistence/PaymentOrderStatus.java` | Enum: PENDING, PAID, CANCELLED, EXPIRED, FAILED |
| `payment/persistence/PaymentTransactionStatus.java` | Enum: PENDING, SUCCESS, FAILED |
| `payment/persistence/TierEntity.java` | JPA entity → `tier` table |
| `payment/persistence/UserTierEntity.java` | JPA entity → `user_tier` table |
| `payment/persistence/PayOSOrderEntity.java` | JPA entity → `payment_order` table |
| `payment/persistence/PayOSTransactionEntity.java` | JPA entity → `payment_transaction` table |
| `payment/persistence/TierRepository.java` | Find active tiers, find by title |
| `payment/persistence/UserTierRepository.java` | Active subscription queries with pessimistic lock |
| `payment/persistence/PayOSOrderRepository.java` | Order lookup + pessimistic lock + expired-order query |
| `payment/persistence/PayOSTransactionRepository.java` | Duplicate-reference guard |
| `payment/dto/CreatePaymentRequest.java` | `{ tierId }` |
| `payment/dto/CreatePaymentResponse.java` | Checkout link details |
| `payment/dto/PayOSReturnRequest.java` | Browser return params |
| `payment/dto/PayOSReturnResponse.java` | Resolved status for UI |
| `payment/dto/TierResponse.java` | Public tier listing |
| `payment/dto/PaymentHistoryResponse.java` | User payment history row |
| `payment/service/PayOSPaymentService.java` | Checkout creation, return handling, history, tier listing |
| `payment/service/PayOSWebhookService.java` | Webhook dispatcher + PAID / CANCEL / EXPIRED flows |
| `payment/service/PaymentExpiryScheduler.java` | Background jobs for order and subscription expiry |
| `payment/controller/PayOSPaymentController.java` | REST endpoints under `/api/v1/payments` |
| `payment/controller/PayOSWebhookController.java` | GET + POST `/api/v1/payments/payos/webhook` |

### New Resource Files
| Path | Responsibility |
|------|---------------|
| `src/main/resources/db/migration/V22__add_payos_payment_module.sql` | Creates 4 tables + seed data + indexes |

### New Test Files
| Path | Responsibility |
|------|---------------|
| `src/test/java/com/vsign/backend/payment/service/PayOSPaymentServiceTest.java` | Unit tests for checkout + return service |
| `src/test/java/com/vsign/backend/payment/service/PayOSWebhookServiceTest.java` | Unit tests for webhook flows |
| `src/test/java/com/vsign/backend/payment/controller/PayOSWebhookControllerTest.java` | Controller slice tests |

### Modified Files
| Path | Change |
|------|--------|
| `pom.xml` | Add `vn.payos:payos-java:2.0.1` |
| `src/main/resources/application.properties` | Add 5 `payos.*` property stubs |
| `src/main/java/com/vsign/backend/common/security/SecurityConfig.java` | Add 3 `permitAll` matchers before blanket `/api/v1/payments/**` rule |
| `src/main/java/com/vsign/backend/VSignBackendApplication.java` | Add `@EnableScheduling` |
| `src/main/java/com/vsign/backend/auth/service/AuthService.java` | Add `TierRepository` + `UserTierRepository` to constructor; create free `UserTierEntity` after user save |

---

## Task 1: Add Maven Dependency and PayOS Config Bean

**Files:**
- Modify: `v-sign-be/pom.xml`
- Modify: `v-sign-be/src/main/resources/application.properties`
- Create: `payment/config/PayOSConfig.java`

- [ ] **Step 1: Add PayOS dependency to pom.xml**

Inside `<dependencies>` in `pom.xml`, add:
```xml
<dependency>
    <groupId>vn.payos</groupId>
    <artifactId>payos-java</artifactId>
    <version>2.0.1</version>
</dependency>
```

- [ ] **Step 2: Add PayOS properties to application.properties**

Append to `src/main/resources/application.properties`:
```properties
payos.client-id=${PAYOS_CLIENT_ID}
payos.api-key=${PAYOS_API_KEY}
payos.checksum-key=${PAYOS_CHECKSUM_KEY}
payos.return-url=${PAYOS_RETURN_URL}
payos.cancel-url=${PAYOS_CANCEL_URL}
```

- [ ] **Step 3: Create PayOSConfig.java**

Create `src/main/java/com/vsign/backend/payment/config/PayOSConfig.java`:
```java
package com.vsign.backend.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "payos")
public class PayOSConfig {
    private String clientId;
    private String apiKey;
    private String checksumKey;
    private String returnUrl;
    private String cancelUrl;

    @Bean
    public PayOS payOS() {
        return new PayOS(clientId, apiKey, checksumKey);
    }
}
```

- [ ] **Step 4: Compile**

```powershell
cd v-sign-be; mvn -q -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add v-sign-be/pom.xml v-sign-be/src/main/resources/application.properties v-sign-be/src/main/java/com/vsign/backend/payment/config/PayOSConfig.java
git commit -m "feat(payment): add payos sdk dependency and config bean"
```

---

## Task 2: Add Flyway Migration V22

**Files:**
- Create: `v-sign-be/src/main/resources/db/migration/V22__add_payos_payment_module.sql`

- [ ] **Step 1: Create migration file**

```sql
-- V22: PayOS tier subscription tables

CREATE TABLE IF NOT EXISTS tier (
    tier_id       UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    title         VARCHAR(50) NOT NULL,
    amount        INT         NOT NULL,
    no_month      INT         NOT NULL,
    limited_token INT         NOT NULL,
    is_active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP,
    deleted_at    TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_tier (
    id         UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    uid        UUID      NOT NULL REFERENCES users(id),
    tier_id    UUID      NOT NULL REFERENCES tier(tier_id),
    start_time TIMESTAMP NOT NULL,
    end_time   TIMESTAMP NOT NULL,
    is_active  BOOLEAN   NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payment_order (
    order_id        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    uid             UUID        NOT NULL REFERENCES users(id),
    tier_id         UUID        NOT NULL REFERENCES tier(tier_id),
    order_code      BIGINT      NOT NULL UNIQUE,
    amount          INT         NOT NULL,
    payment_link_id VARCHAR(255),
    checkout_url    VARCHAR(500),
    qr_code         TEXT,
    status          VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    paid_at         TIMESTAMP,
    expired_at      TIMESTAMP,
    description     TEXT,
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    deleted_at      TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payment_transaction (
    transaction_id   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id         UUID        NOT NULL REFERENCES payment_order(order_id),
    amount           INT         NOT NULL,
    payment_link_id  VARCHAR(255),
    payload          TEXT,
    status           VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    transaction_date TIMESTAMP,
    reference        VARCHAR(255),
    is_active        BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP,
    deleted_at       TIMESTAMP
);

INSERT INTO tier (tier_id, title, amount, no_month, limited_token, is_active, created_at)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'free', 0,     1, 20,  TRUE, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000000002', 'plus', 49000, 1, 100, TRUE, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000000003', 'pro',  99000, 1, 999, TRUE, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_tier_is_active       ON tier                (is_active, deleted_at);
CREATE INDEX IF NOT EXISTS idx_user_tier_uid        ON user_tier           (uid, is_active);
CREATE INDEX IF NOT EXISTS idx_user_tier_tier_id    ON user_tier           (tier_id);
CREATE INDEX IF NOT EXISTS idx_payment_order_uid    ON payment_order       (uid, created_at);
CREATE INDEX IF NOT EXISTS idx_payment_order_code   ON payment_order       (order_code);
CREATE INDEX IF NOT EXISTS idx_payment_order_status ON payment_order       (status);
CREATE INDEX IF NOT EXISTS idx_payment_tx_order     ON payment_transaction (order_id, created_at);
CREATE INDEX IF NOT EXISTS idx_payment_tx_reference ON payment_transaction (reference);
```

- [ ] **Step 2: Commit**

```bash
git add v-sign-be/src/main/resources/db/migration/V22__add_payos_payment_module.sql
git commit -m "feat(payment): add payos schema migration V22"
```

Migration runs automatically on next startup. To verify locally run the app once with DB env vars set.

---

## Task 3: Add Enums and JPA Entities

**Files:**
- Create: `payment/persistence/PaymentOrderStatus.java`
- Create: `payment/persistence/PaymentTransactionStatus.java`
- Create: `payment/persistence/TierEntity.java`
- Create: `payment/persistence/UserTierEntity.java`
- Create: `payment/persistence/PayOSOrderEntity.java`
- Create: `payment/persistence/PayOSTransactionEntity.java`

- [ ] **Step 1: Create PaymentOrderStatus.java**

```java
package com.vsign.backend.payment.persistence;

public enum PaymentOrderStatus {
    PENDING, PAID, CANCELLED, EXPIRED, FAILED
}
```

- [ ] **Step 2: Create PaymentTransactionStatus.java**

```java
package com.vsign.backend.payment.persistence;

public enum PaymentTransactionStatus {
    PENDING, SUCCESS, FAILED
}
```

- [ ] **Step 3: Create TierEntity.java**

```java
package com.vsign.backend.payment.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tier")
public class TierEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tier_id", updatable = false, nullable = false)
    private UUID tierId;

    @Column(name = "title", nullable = false, length = 50)
    private String title;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "no_month", nullable = false)
    private Integer noMonth;

    @Column(name = "limited_token", nullable = false)
    private Integer limitedToken;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
```

- [ ] **Step 4: Create UserTierEntity.java**

```java
package com.vsign.backend.payment.persistence;

import com.vsign.backend.auth.persistence.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_tier")
public class UserTierEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uid", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_id", nullable = false)
    private TierEntity tier;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
```

- [ ] **Step 5: Create PayOSOrderEntity.java**

```java
package com.vsign.backend.payment.persistence;

import com.vsign.backend.auth.persistence.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payment_order")
public class PayOSOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id", updatable = false, nullable = false)
    private UUID orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uid", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_id", nullable = false)
    private TierEntity tier;

    @Column(name = "order_code", nullable = false, unique = true)
    private Long orderCode;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "payment_link_id")
    private String paymentLinkId;

    @Column(name = "checkout_url", length = 500)
    private String checkoutUrl;

    @Column(name = "qr_code", columnDefinition = "TEXT")
    private String qrCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PaymentOrderStatus status = PaymentOrderStatus.PENDING;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
```

- [ ] **Step 6: Create PayOSTransactionEntity.java**

```java
package com.vsign.backend.payment.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payment_transaction")
public class PayOSTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id", updatable = false, nullable = false)
    private UUID transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private PayOSOrderEntity paymentOrder;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "payment_link_id")
    private String paymentLinkId;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PaymentTransactionStatus status = PaymentTransactionStatus.PENDING;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "reference")
    private String reference;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
```

- [ ] **Step 7: Compile**

```powershell
cd v-sign-be; mvn -q -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 8: Commit**

```bash
git add v-sign-be/src/main/java/com/vsign/backend/payment/
git commit -m "feat(payment): add payos entities and enums"
```

---

## Task 4: Add Repositories

**Files:**
- Create: `payment/persistence/TierRepository.java`
- Create: `payment/persistence/UserTierRepository.java`
- Create: `payment/persistence/PayOSOrderRepository.java`
- Create: `payment/persistence/PayOSTransactionRepository.java`

- [ ] **Step 1: Create TierRepository.java**

```java
package com.vsign.backend.payment.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TierRepository extends JpaRepository<TierEntity, UUID> {

    List<TierEntity> findByIsActiveTrueAndDeletedAtIsNull();

    Optional<TierEntity> findByTitleIgnoreCaseAndIsActiveTrueAndDeletedAtIsNull(String title);
}
```

- [ ] **Step 2: Create UserTierRepository.java**

```java
package com.vsign.backend.payment.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface UserTierRepository extends JpaRepository<UserTierEntity, UUID> {

    @Query("""
        SELECT ut FROM UserTierEntity ut
        JOIN FETCH ut.tier t
        WHERE ut.user.id = :userId
          AND ut.isActive = true
          AND ut.deletedAt IS NULL
          AND ut.endTime > :now
        ORDER BY t.amount DESC
    """)
    List<UserTierEntity> findCurrentActiveByUserId(
            @Param("userId") UUID userId,
            @Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT ut FROM UserTierEntity ut
        JOIN FETCH ut.tier t
        WHERE ut.user.id = :userId
          AND ut.isActive = true
          AND ut.deletedAt IS NULL
          AND ut.endTime > :now
        ORDER BY t.amount DESC
    """)
    List<UserTierEntity> findCurrentActiveByUserIdForUpdate(
            @Param("userId") UUID userId,
            @Param("now") LocalDateTime now);

    @Query("""
        SELECT ut FROM UserTierEntity ut
        JOIN FETCH ut.tier t
        WHERE ut.isActive = true
          AND ut.deletedAt IS NULL
          AND ut.endTime <= :now
          AND t.amount > 0
    """)
    List<UserTierEntity> findExpiredPaidSubscriptions(@Param("now") LocalDateTime now);
}
```

- [ ] **Step 3: Create PayOSOrderRepository.java**

```java
package com.vsign.backend.payment.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayOSOrderRepository extends JpaRepository<PayOSOrderEntity, UUID> {

    Optional<PayOSOrderEntity> findByOrderCode(Long orderCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM PayOSOrderEntity o WHERE o.orderCode = :orderCode")
    Optional<PayOSOrderEntity> findByOrderCodeForUpdate(@Param("orderCode") Long orderCode);

    List<PayOSOrderEntity> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    @Query("""
        SELECT o FROM PayOSOrderEntity o
        WHERE o.status = :status
          AND o.expiredAt <= :now
          AND o.deletedAt IS NULL
    """)
    List<PayOSOrderEntity> findExpiredPendingOrders(
            @Param("status") PaymentOrderStatus status,
            @Param("now") LocalDateTime now);
}
```

- [ ] **Step 4: Create PayOSTransactionRepository.java**

```java
package com.vsign.backend.payment.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PayOSTransactionRepository extends JpaRepository<PayOSTransactionEntity, UUID> {

    boolean existsByReference(String reference);
}
```

- [ ] **Step 5: Compile**

```powershell
cd v-sign-be; mvn -q -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
git add v-sign-be/src/main/java/com/vsign/backend/payment/
git commit -m "feat(payment): add payos repositories"
```

---

## Task 5: Add DTOs

**Files:**
- Create: `payment/dto/CreatePaymentRequest.java`
- Create: `payment/dto/CreatePaymentResponse.java`
- Create: `payment/dto/PayOSReturnRequest.java`
- Create: `payment/dto/PayOSReturnResponse.java`
- Create: `payment/dto/TierResponse.java`
- Create: `payment/dto/PaymentHistoryResponse.java`

- [ ] **Step 1: Create CreatePaymentRequest.java**

```java
package com.vsign.backend.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreatePaymentRequest {
    @NotBlank
    private String tierId;
}
```

- [ ] **Step 2: Create CreatePaymentResponse.java**

```java
package com.vsign.backend.payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreatePaymentResponse {
    private String orderId;
    private Long orderCode;
    private String paymentLinkId;
    private String checkoutUrl;
    private String qrCode;
    private Integer amount;
    private String status;
    private String expiredAt;
}
```

- [ ] **Step 3: Create PayOSReturnRequest.java**

```java
package com.vsign.backend.payment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PayOSReturnRequest {
    private String code;
    private String id;
    private Boolean cancel;
    private String status;
    private Long orderCode;
}
```

- [ ] **Step 4: Create PayOSReturnResponse.java**

```java
package com.vsign.backend.payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PayOSReturnResponse {
    private Long orderCode;
    private String resolvedStatus;
    private String message;
}
```

- [ ] **Step 5: Create TierResponse.java**

```java
package com.vsign.backend.payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TierResponse {
    private String tierId;
    private String title;
    private Integer amount;
    private Integer noMonth;
    private Integer limitedToken;
    private Boolean isActive;
}
```

- [ ] **Step 6: Create PaymentHistoryResponse.java**

```java
package com.vsign.backend.payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentHistoryResponse {
    private String orderId;
    private Long orderCode;
    private String tierId;
    private String tierTitle;
    private Integer amount;
    private String status;
    private String paymentLinkId;
    private String createdAt;
    private String paidAt;
    private String expiredAt;
}
```

- [ ] **Step 7: Compile**

```powershell
cd v-sign-be; mvn -q -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 8: Commit**

```bash
git add v-sign-be/src/main/java/com/vsign/backend/payment/dto/
git commit -m "feat(payment): add payos dtos"
```

---

## Task 6: Add PayOSPaymentService

**Interfaces:**
- Consumes: `TierRepository`, `PayOSOrderRepository`, `UserTierRepository`, `UserRepository.findByEmailIgnoreCase`, `PayOSConfig`, `PayOS`
- Produces:
  - `createPayOSCheckout(String userEmail, CreatePaymentRequest) → CreatePaymentResponse`
  - `getMyPaymentHistory(String userEmail) → List<PaymentHistoryResponse>`
  - `listActiveTiers() → List<TierResponse>`
  - `handlePayOSReturn(String userEmail, PayOSReturnRequest) → PayOSReturnResponse`

**Files:**
- Test: `src/test/java/com/vsign/backend/payment/service/PayOSPaymentServiceTest.java`
- Create: `payment/service/PayOSPaymentService.java`

- [ ] **Step 1: Write failing tests**

Create `src/test/java/com/vsign/backend/payment/service/PayOSPaymentServiceTest.java`:

```java
package com.vsign.backend.payment.service;

import com.vsign.backend.auth.persistence.UserEntity;
import com.vsign.backend.auth.persistence.UserRepository;
import com.vsign.backend.payment.config.PayOSConfig;
import com.vsign.backend.payment.dto.CreatePaymentRequest;
import com.vsign.backend.payment.dto.PayOSReturnRequest;
import com.vsign.backend.payment.persistence.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.payos.PayOS;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayOSPaymentServiceTest {

    @Mock UserRepository userRepository;
    @Mock TierRepository tierRepository;
    @Mock PayOSOrderRepository orderRepository;
    @Mock UserTierRepository userTierRepository;
    @Mock PayOS payOS;
    @Mock PayOSConfig payOSConfig;

    @InjectMocks PayOSPaymentService service;

    @Test
    void createPayOSCheckout_rejectsFreeTier() {
        UUID tierId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UserEntity user = user(UUID.randomUUID());
        TierEntity free = tier(tierId, "free", 0);

        when(userRepository.findByEmailIgnoreCase("test@test.com")).thenReturn(Optional.of(user));
        when(tierRepository.findById(tierId)).thenReturn(Optional.of(free));

        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setTierId(tierId.toString());

        assertThatThrownBy(() -> service.createPayOSCheckout("test@test.com", req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createPayOSCheckout_rejectsWhenUserAlreadyHasPaidSubscription() {
        UUID tierId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UserEntity user = user(UUID.randomUUID());
        TierEntity plus = tier(tierId, "plus", 49000);
        TierEntity paidTier = tier(UUID.randomUUID(), "plus", 49000);
        UserTierEntity activePaid = new UserTierEntity();
        activePaid.setTier(paidTier);

        when(userRepository.findByEmailIgnoreCase("test@test.com")).thenReturn(Optional.of(user));
        when(tierRepository.findById(tierId)).thenReturn(Optional.of(plus));
        when(userTierRepository.findCurrentActiveByUserId(eq(user.getId()), any()))
                .thenReturn(List.of(activePaid));

        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setTierId(tierId.toString());

        assertThatThrownBy(() -> service.createPayOSCheckout("test@test.com", req))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void handlePayOSReturn_rejectsOrderOwnedByDifferentUser() {
        UserEntity requesting = user(UUID.randomUUID());
        UserEntity owner = user(UUID.randomUUID());

        PayOSOrderEntity order = new PayOSOrderEntity();
        order.setUser(owner);
        order.setStatus(PaymentOrderStatus.PENDING);

        when(userRepository.findByEmailIgnoreCase("requester@test.com")).thenReturn(Optional.of(requesting));
        when(orderRepository.findByOrderCode(123L)).thenReturn(Optional.of(order));

        PayOSReturnRequest req = new PayOSReturnRequest();
        req.setOrderCode(123L);
        req.setCancel(false);
        req.setStatus("PENDING");

        assertThatThrownBy(() -> service.handlePayOSReturn("requester@test.com", req))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void handlePayOSReturn_marksCancelledForCancelTrue() {
        UserEntity owner = user(UUID.randomUUID());

        PayOSOrderEntity order = new PayOSOrderEntity();
        order.setUser(owner);
        order.setStatus(PaymentOrderStatus.PENDING);

        when(userRepository.findByEmailIgnoreCase("owner@test.com")).thenReturn(Optional.of(owner));
        when(orderRepository.findByOrderCode(456L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PayOSReturnRequest req = new PayOSReturnRequest();
        req.setOrderCode(456L);
        req.setCancel(true);
        req.setStatus("CANCELLED");

        var response = service.handlePayOSReturn("owner@test.com", req);

        assertThat(response.getResolvedStatus()).isEqualTo("CANCELLED");
        verify(orderRepository).save(argThat(o -> o.getStatus() == PaymentOrderStatus.CANCELLED));
    }

    @Test
    void handlePayOSReturn_doesNotUpgradeTierWhenStatusPaid() {
        UserEntity owner = user(UUID.randomUUID());

        PayOSOrderEntity order = new PayOSOrderEntity();
        order.setUser(owner);
        order.setStatus(PaymentOrderStatus.PENDING);

        when(userRepository.findByEmailIgnoreCase("owner@test.com")).thenReturn(Optional.of(owner));
        when(orderRepository.findByOrderCode(789L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PayOSReturnRequest req = new PayOSReturnRequest();
        req.setOrderCode(789L);
        req.setCancel(false);
        req.setStatus("PAID");

        service.handlePayOSReturn("owner@test.com", req);

        verify(userTierRepository, never()).save(any());
    }

    private UserEntity user(UUID id) {
        UserEntity u = new UserEntity();
        u.setId(id);
        return u;
    }

    private TierEntity tier(UUID id, String title, int amount) {
        TierEntity t = new TierEntity();
        t.setTierId(id);
        t.setTitle(title);
        t.setAmount(amount);
        t.setIsActive(true);
        t.setNoMonth(1);
        t.setLimitedToken(20);
        return t;
    }
}
```

- [ ] **Step 2: Run test to confirm compile failure**

```powershell
cd v-sign-be; mvn -q -Dtest=PayOSPaymentServiceTest test 2>&1 | Select-Object -First 15
```

Expected: compile error — `PayOSPaymentService` does not exist.

- [ ] **Step 3: Implement PayOSPaymentService.java**

Create `payment/service/PayOSPaymentService.java`:

```java
package com.vsign.backend.payment.service;

import com.vsign.backend.auth.persistence.UserEntity;
import com.vsign.backend.auth.persistence.UserRepository;
import com.vsign.backend.payment.config.PayOSConfig;
import com.vsign.backend.payment.dto.*;
import com.vsign.backend.payment.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.CreatePaymentLinkRequest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayOSPaymentService {

    private final UserRepository userRepository;
    private final TierRepository tierRepository;
    private final PayOSOrderRepository orderRepository;
    private final UserTierRepository userTierRepository;
    private final PayOS payOS;
    private final PayOSConfig payOSConfig;

    @Transactional
    public CreatePaymentResponse createPayOSCheckout(String userEmail, CreatePaymentRequest request) {
        UserEntity user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UUID tierId = UUID.fromString(request.getTierId());
        TierEntity tier = tierRepository.findById(tierId)
                .orElseThrow(() -> new IllegalArgumentException("Tier not found"));

        if (!Boolean.TRUE.equals(tier.getIsActive()) || tier.getDeletedAt() != null) {
            throw new IllegalArgumentException("Tier is not available");
        }
        if (tier.getAmount() <= 0) {
            throw new IllegalArgumentException("Cannot purchase free tier");
        }

        boolean hasPaid = userTierRepository
                .findCurrentActiveByUserId(user.getId(), LocalDateTime.now())
                .stream().anyMatch(ut -> ut.getTier().getAmount() > 0);
        if (hasPaid) {
            throw new IllegalStateException("User already has an active paid subscription");
        }

        long orderCode = generateOrderCode();
        String description = "VSIGN" + String.format("%06d", orderCode % 1_000_000);
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(15);

        PayOSOrderEntity order = new PayOSOrderEntity();
        order.setUser(user);
        order.setTier(tier);
        order.setOrderCode(orderCode);
        order.setAmount(tier.getAmount());
        order.setDescription(description);
        order.setStatus(PaymentOrderStatus.PENDING);
        order.setExpiredAt(expiredAt);
        order = orderRepository.save(order);

        try {
            CreatePaymentLinkRequest payosReq = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(tier.getAmount().longValue())
                    .description(description)
                    .returnUrl(payOSConfig.getReturnUrl())
                    .cancelUrl(payOSConfig.getCancelUrl())
                    .expiredAt(Instant.now().plus(15, ChronoUnit.MINUTES).getEpochSecond())
                    .build();

            CheckoutResponseData data = payOS.createPaymentLink(payosReq);
            order.setPaymentLinkId(data.getPaymentLinkId());
            order.setCheckoutUrl(data.getCheckoutUrl());
            order.setQrCode(data.getQrCode());
            order = orderRepository.save(order);
        } catch (Exception e) {
            order.setStatus(PaymentOrderStatus.FAILED);
            orderRepository.save(order);
            throw new RuntimeException("PayOS checkout creation failed: " + e.getMessage(), e);
        }

        return CreatePaymentResponse.builder()
                .orderId(order.getOrderId().toString())
                .orderCode(order.getOrderCode())
                .paymentLinkId(order.getPaymentLinkId())
                .checkoutUrl(order.getCheckoutUrl())
                .qrCode(order.getQrCode())
                .amount(order.getAmount())
                .status(order.getStatus().name())
                .expiredAt(order.getExpiredAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    public List<PaymentHistoryResponse> getMyPaymentHistory(String userEmail) {
        UserEntity user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return orderRepository.findByUser_IdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toHistoryResponse)
                .collect(Collectors.toList());
    }

    public List<TierResponse> listActiveTiers() {
        return tierRepository.findByIsActiveTrueAndDeletedAtIsNull().stream()
                .map(t -> TierResponse.builder()
                        .tierId(t.getTierId().toString())
                        .title(t.getTitle())
                        .amount(t.getAmount())
                        .noMonth(t.getNoMonth())
                        .limitedToken(t.getLimitedToken())
                        .isActive(t.getIsActive())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public PayOSReturnResponse handlePayOSReturn(String userEmail, PayOSReturnRequest request) {
        UserEntity requestingUser = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        PayOSOrderEntity order = orderRepository.findByOrderCode(request.getOrderCode())
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!order.getUser().getId().equals(requestingUser.getId())) {
            throw new SecurityException("Order does not belong to this user");
        }

        PaymentOrderStatus current = order.getStatus();
        if (isTerminal(current)) {
            return PayOSReturnResponse.builder()
                    .orderCode(order.getOrderCode())
                    .resolvedStatus(current.name())
                    .message("Order already in terminal state")
                    .build();
        }

        PaymentOrderStatus resolved = resolveReturnStatus(request);
        if (resolved != null && resolved != current) {
            order.setStatus(resolved);
            if (resolved == PaymentOrderStatus.PAID) {
                order.setPaidAt(LocalDateTime.now());
            }
            orderRepository.save(order);
        }

        PaymentOrderStatus finalStatus = resolved != null ? resolved : current;
        return PayOSReturnResponse.builder()
                .orderCode(order.getOrderCode())
                .resolvedStatus(finalStatus.name())
                .message("OK")
                .build();
    }

    private PaymentOrderStatus resolveReturnStatus(PayOSReturnRequest req) {
        if (Boolean.TRUE.equals(req.getCancel()) || "CANCELLED".equalsIgnoreCase(req.getStatus())) {
            return PaymentOrderStatus.CANCELLED;
        }
        if ("PAID".equalsIgnoreCase(req.getStatus())) {
            return PaymentOrderStatus.PAID;
        }
        return null;
    }

    private boolean isTerminal(PaymentOrderStatus status) {
        return status == PaymentOrderStatus.PAID
                || status == PaymentOrderStatus.CANCELLED
                || status == PaymentOrderStatus.EXPIRED
                || status == PaymentOrderStatus.FAILED;
    }

    private PaymentHistoryResponse toHistoryResponse(PayOSOrderEntity o) {
        return PaymentHistoryResponse.builder()
                .orderId(o.getOrderId().toString())
                .orderCode(o.getOrderCode())
                .tierId(o.getTier().getTierId().toString())
                .tierTitle(o.getTier().getTitle())
                .amount(o.getAmount())
                .status(o.getStatus().name())
                .paymentLinkId(o.getPaymentLinkId())
                .createdAt(fmt(o.getCreatedAt()))
                .paidAt(fmt(o.getPaidAt()))
                .expiredAt(fmt(o.getExpiredAt()))
                .build();
    }

    private String fmt(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    private long generateOrderCode() {
        return Math.abs(new Random().nextLong() % 1_000_000_000L) + 1;
    }
}
```

- [ ] **Step 4: Run tests**

```powershell
cd v-sign-be; mvn -q -Dtest=PayOSPaymentServiceTest test
```

Expected: `BUILD SUCCESS`, 5 tests pass.

- [ ] **Step 5: Commit**

```bash
git add v-sign-be/src/main/java/com/vsign/backend/payment/service/PayOSPaymentService.java v-sign-be/src/test/java/com/vsign/backend/payment/service/PayOSPaymentServiceTest.java
git commit -m "feat(payment): add payos checkout and return service"
```

---

## Task 7: Add PayOSWebhookService

**Interfaces:**
- Consumes: `PayOSOrderRepository`, `PayOSTransactionRepository`, `UserTierRepository`
- Produces: `handlePayOSWebhook(WebhookData) → void`

**Files:**
- Test: `src/test/java/com/vsign/backend/payment/service/PayOSWebhookServiceTest.java`
- Create: `payment/service/PayOSWebhookService.java`

- [ ] **Step 1: Check PayOS SDK WebhookData API**

After Task 1 compile succeeds, run:
```powershell
cd v-sign-be; mvn -q dependency:get "-Dartifact=vn.payos:payos-java:2.0.1:jar:sources" 2>&1 | Out-Null; Get-ChildItem "$env:USERPROFILE\.m2\repository\vn\payos\payos-java\2.0.1\" | Select-Object Name
```

Inspect `WebhookData` class for exact getter names and return types. Expected: `getCode() String`, `getOrderCode() long`, `getAmount() Long` (or `Integer`), `getPaymentLinkId() String`, `getReference() String`. If `getAmount()` returns `Integer`, change `data.getAmount()` casts from `long` to `int` in both service and tests.

- [ ] **Step 2: Write failing tests**

Create `src/test/java/com/vsign/backend/payment/service/PayOSWebhookServiceTest.java`:

```java
package com.vsign.backend.payment.service;

import com.vsign.backend.auth.persistence.UserEntity;
import com.vsign.backend.payment.persistence.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.payos.model.webhooks.WebhookData;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayOSWebhookServiceTest {

    @Mock PayOSOrderRepository orderRepository;
    @Mock PayOSTransactionRepository transactionRepository;
    @Mock UserTierRepository userTierRepository;

    @InjectMocks PayOSWebhookService service;

    @Test
    void handlePayOSWebhook_paidCreatesSubscription() {
        TierEntity tier = tier(UUID.randomUUID(), "plus", 49000, 1);
        UserEntity user = user(UUID.randomUUID());
        PayOSOrderEntity order = pendingOrder(user, tier, 49000);

        when(orderRepository.findByOrderCodeForUpdate(order.getOrderCode()))
                .thenReturn(Optional.of(order));
        when(transactionRepository.existsByReference("REF001")).thenReturn(false);
        when(userTierRepository.findCurrentActiveByUserIdForUpdate(eq(user.getId()), any()))
                .thenReturn(Collections.emptyList());
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userTierRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.handlePayOSWebhook(data("00", order.getOrderCode(), 49000L, null, "REF001"));

        verify(userTierRepository).save(argThat(ut ->
                Boolean.TRUE.equals(ut.getIsActive()) && ut.getTier().equals(tier)));
        verify(orderRepository).save(argThat(o -> o.getStatus() == PaymentOrderStatus.PAID));
    }

    @Test
    void handlePayOSWebhook_paidIsIdempotentForDuplicatePaidWebhook() {
        TierEntity tier = tier(UUID.randomUUID(), "plus", 49000, 1);
        UserEntity user = user(UUID.randomUUID());
        PayOSOrderEntity order = paidOrder(user, tier, 49000);

        when(orderRepository.findByOrderCodeForUpdate(order.getOrderCode()))
                .thenReturn(Optional.of(order));

        service.handlePayOSWebhook(data("00", order.getOrderCode(), 49000L, null, "REF002"));

        verify(userTierRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void handlePayOSWebhook_rejectsAmountMismatch() {
        TierEntity tier = tier(UUID.randomUUID(), "plus", 49000, 1);
        UserEntity user = user(UUID.randomUUID());
        PayOSOrderEntity order = pendingOrder(user, tier, 49000);

        when(orderRepository.findByOrderCodeForUpdate(order.getOrderCode()))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() ->
                service.handlePayOSWebhook(data("00", order.getOrderCode(), 1000L, null, "REF003")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("amount");
    }

    @Test
    void handlePayOSWebhook_cancelDoesNotOverridePaidOrder() {
        TierEntity tier = tier(UUID.randomUUID(), "plus", 49000, 1);
        UserEntity user = user(UUID.randomUUID());
        PayOSOrderEntity order = paidOrder(user, tier, 49000);

        when(orderRepository.findByOrderCodeForUpdate(order.getOrderCode()))
                .thenReturn(Optional.of(order));

        service.handlePayOSWebhook(data("01", order.getOrderCode(), 0L, null, null));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void handlePayOSWebhook_expiredMarksPendingOrderExpired() {
        TierEntity tier = tier(UUID.randomUUID(), "plus", 49000, 1);
        UserEntity user = user(UUID.randomUUID());
        PayOSOrderEntity order = pendingOrder(user, tier, 49000);

        when(orderRepository.findByOrderCodeForUpdate(order.getOrderCode()))
                .thenReturn(Optional.of(order));
        when(transactionRepository.existsByReference(any())).thenReturn(false);
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.handlePayOSWebhook(data("02", order.getOrderCode(), 0L, null, null));

        verify(orderRepository).save(argThat(o -> o.getStatus() == PaymentOrderStatus.EXPIRED));
    }

    // ---- helpers ----

    private UserEntity user(UUID id) {
        UserEntity u = new UserEntity();
        u.setId(id);
        return u;
    }

    private TierEntity tier(UUID id, String title, int amount, int noMonth) {
        TierEntity t = new TierEntity();
        t.setTierId(id);
        t.setTitle(title);
        t.setAmount(amount);
        t.setNoMonth(noMonth);
        t.setLimitedToken(100);
        t.setIsActive(true);
        return t;
    }

    private PayOSOrderEntity pendingOrder(UserEntity user, TierEntity tier, int amount) {
        PayOSOrderEntity o = new PayOSOrderEntity();
        o.setUser(user);
        o.setTier(tier);
        o.setOrderCode(Math.abs(new Random().nextLong() % 1_000_000_000L) + 1);
        o.setAmount(amount);
        o.setStatus(PaymentOrderStatus.PENDING);
        return o;
    }

    private PayOSOrderEntity paidOrder(UserEntity user, TierEntity tier, int amount) {
        PayOSOrderEntity o = pendingOrder(user, tier, amount);
        o.setStatus(PaymentOrderStatus.PAID);
        o.setPaidAt(LocalDateTime.now());
        return o;
    }

    private WebhookData data(String code, long orderCode, Long amount,
                              String paymentLinkId, String reference) {
        WebhookData d = mock(WebhookData.class);
        when(d.getCode()).thenReturn(code);
        when(d.getOrderCode()).thenReturn(orderCode);
        when(d.getAmount()).thenReturn(amount);
        lenient().when(d.getPaymentLinkId()).thenReturn(paymentLinkId);
        lenient().when(d.getReference()).thenReturn(reference);
        return d;
    }
}
```

- [ ] **Step 3: Run test to confirm compile failure**

```powershell
cd v-sign-be; mvn -q -Dtest=PayOSWebhookServiceTest test 2>&1 | Select-Object -First 10
```

Expected: compile error.

- [ ] **Step 4: Implement PayOSWebhookService.java**

Create `payment/service/PayOSWebhookService.java`:

```java
package com.vsign.backend.payment.service;

import com.vsign.backend.payment.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.model.webhooks.WebhookData;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayOSWebhookService {

    private final PayOSOrderRepository orderRepository;
    private final PayOSTransactionRepository transactionRepository;
    private final UserTierRepository userTierRepository;

    @Transactional
    public void handlePayOSWebhook(WebhookData data) {
        switch (data.getCode()) {
            case "00" -> handlePaid(data);
            case "01" -> handleCancelled(data);
            case "02" -> handleExpired(data);
            default   -> log.warn("Unknown PayOS webhook code: {}", data.getCode());
        }
    }

    private void handlePaid(WebhookData data) {
        PayOSOrderEntity order = lockOrder(data.getOrderCode());

        if (order.getStatus() == PaymentOrderStatus.PAID) {
            log.info("Duplicate PAID webhook for order {}, ignoring", order.getOrderCode());
            return;
        }

        long webhookAmount = data.getAmount() != null ? data.getAmount() : 0L;
        if (order.getAmount().longValue() != webhookAmount) {
            throw new IllegalStateException(
                    "PayOS amount mismatch: expected " + order.getAmount() + " got " + webhookAmount);
        }

        if (data.getPaymentLinkId() != null && order.getPaymentLinkId() != null
                && !data.getPaymentLinkId().equals(order.getPaymentLinkId())) {
            throw new IllegalStateException("PayOS paymentLinkId mismatch");
        }

        saveTransactionIfNew(order, data, PaymentTransactionStatus.SUCCESS);

        order.setStatus(PaymentOrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);

        upgradeUserTier(order);
    }

    private void handleCancelled(WebhookData data) {
        PayOSOrderEntity order = lockOrder(data.getOrderCode());
        if (order.getStatus() == PaymentOrderStatus.PAID
                || order.getStatus() == PaymentOrderStatus.CANCELLED) return;

        saveTransactionIfNew(order, data, PaymentTransactionStatus.FAILED);
        order.setStatus(PaymentOrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    private void handleExpired(WebhookData data) {
        PayOSOrderEntity order = lockOrder(data.getOrderCode());
        if (order.getStatus() == PaymentOrderStatus.PAID
                || order.getStatus() == PaymentOrderStatus.EXPIRED) return;

        saveTransactionIfNew(order, data, PaymentTransactionStatus.FAILED);
        order.setStatus(PaymentOrderStatus.EXPIRED);
        orderRepository.save(order);
    }

    private void upgradeUserTier(PayOSOrderEntity order) {
        var userId = order.getUser().getId();
        List<UserTierEntity> existing = userTierRepository
                .findCurrentActiveByUserIdForUpdate(userId, LocalDateTime.now());

        boolean hasPaid = existing.stream().anyMatch(ut -> ut.getTier().getAmount() > 0);
        if (hasPaid) {
            log.info("User {} already has active paid subscription, skipping upgrade", userId);
            return;
        }

        UserTierEntity userTier = new UserTierEntity();
        userTier.setUser(order.getUser());
        userTier.setTier(order.getTier());
        userTier.setStartTime(LocalDateTime.now());
        userTier.setEndTime(LocalDateTime.now().plusMonths(order.getTier().getNoMonth()));
        userTier.setIsActive(true);
        userTierRepository.save(userTier);
    }

    private PayOSOrderEntity lockOrder(long orderCode) {
        return orderRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderCode));
    }

    private void saveTransactionIfNew(PayOSOrderEntity order, WebhookData data,
                                       PaymentTransactionStatus status) {
        String ref = data.getReference();
        if (ref != null && transactionRepository.existsByReference(ref)) {
            log.info("Duplicate transaction reference {}, skipping", ref);
            return;
        }

        PayOSTransactionEntity tx = new PayOSTransactionEntity();
        tx.setPaymentOrder(order);
        tx.setAmount(data.getAmount() != null ? data.getAmount().intValue() : 0);
        tx.setPaymentLinkId(data.getPaymentLinkId());
        tx.setReference(ref);
        tx.setStatus(status);
        tx.setTransactionDate(LocalDateTime.now());
        transactionRepository.save(tx);
    }
}
```

- [ ] **Step 5: Run tests**

```powershell
cd v-sign-be; mvn -q -Dtest=PayOSWebhookServiceTest test
```

Expected: `BUILD SUCCESS`, 5 tests pass.

- [ ] **Step 6: Commit**

```bash
git add v-sign-be/src/main/java/com/vsign/backend/payment/service/PayOSWebhookService.java v-sign-be/src/test/java/com/vsign/backend/payment/service/PayOSWebhookServiceTest.java
git commit -m "feat(payment): add verified payos webhook processing service"
```

---

## Task 8: Add Controllers and Update Security Rules

**Files:**
- Create: `payment/controller/PayOSPaymentController.java`
- Create: `payment/controller/PayOSWebhookController.java`
- Modify: `common/security/SecurityConfig.java`
- Test: `src/test/java/com/vsign/backend/payment/controller/PayOSWebhookControllerTest.java`

- [ ] **Step 1: Create PayOSPaymentController.java**

The existing `monetization` `PaymentController` at `/api/v1/payments` handles `POST /orders` and `GET /{transactionId}`. The new controller adds distinct paths — Spring MVC handles multiple controllers on the same base path.

Create `payment/controller/PayOSPaymentController.java`:

```java
package com.vsign.backend.payment.controller;

import com.vsign.backend.common.security.JwtService;
import com.vsign.backend.payment.dto.*;
import com.vsign.backend.payment.service.PayOSPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PayOSPaymentController {

    private final PayOSPaymentService paymentService;

    @GetMapping("/tiers")
    public ResponseEntity<List<TierResponse>> tiers() {
        return ResponseEntity.ok(paymentService.listActiveTiers());
    }

    @PostMapping("/checkout")
    public ResponseEntity<CreatePaymentResponse> checkout(
            @Valid @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal JwtService.Principal principal) {
        return ResponseEntity.ok(paymentService.createPayOSCheckout(principal.email(), request));
    }

    @GetMapping("/me")
    public ResponseEntity<List<PaymentHistoryResponse>> myHistory(
            @AuthenticationPrincipal JwtService.Principal principal) {
        return ResponseEntity.ok(paymentService.getMyPaymentHistory(principal.email()));
    }

    @PostMapping("/payos/return")
    public ResponseEntity<PayOSReturnResponse> payosReturn(
            @RequestBody PayOSReturnRequest request,
            @AuthenticationPrincipal JwtService.Principal principal) {
        return ResponseEntity.ok(paymentService.handlePayOSReturn(principal.email(), request));
    }
}
```

- [ ] **Step 2: Create PayOSWebhookController.java**

Create `payment/controller/PayOSWebhookController.java`:

```java
package com.vsign.backend.payment.controller;

import com.vsign.backend.payment.service.PayOSWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.PayOS;
import vn.payos.model.webhooks.Webhook;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments/payos/webhook")
@RequiredArgsConstructor
public class PayOSWebhookController {

    private final PayOS payOS;
    private final PayOSWebhookService webhookService;

    @GetMapping
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("OK");
    }

    @PostMapping
    public ResponseEntity<String> webhook(@RequestBody Webhook webhook) {
        try {
            var data = payOS.verifyPaymentWebhookData(webhook);
            webhookService.handlePayOSWebhook(data);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.warn("PayOS webhook rejected: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid webhook");
        }
    }
}
```

> **SDK note:** If `payOS.verifyPaymentWebhookData(webhook)` does not compile, check the actual method name in the SDK jar. Common alternative: `payOS.getWebhookData(webhook)`. Fix until `mvn -q -DskipTests compile` passes.

- [ ] **Step 3: Add public permits to SecurityConfig.java**

In `SecurityConfig.java` at line 65, the current rule is:
```java
.requestMatchers("/api/v1/payments/**", "/api/v1/subscriptions/checkout").authenticated()
```

Add three `permitAll` lines immediately **before** that line:
```java
.requestMatchers(HttpMethod.GET,  "/api/v1/payments/tiers").permitAll()
.requestMatchers(HttpMethod.GET,  "/api/v1/payments/payos/webhook").permitAll()
.requestMatchers(HttpMethod.POST, "/api/v1/payments/payos/webhook").permitAll()
.requestMatchers("/api/v1/payments/**", "/api/v1/subscriptions/checkout").authenticated()
```

`HttpMethod` is already imported in this file.

- [ ] **Step 4: Write controller tests**

Create `src/test/java/com/vsign/backend/payment/controller/PayOSWebhookControllerTest.java`:

```java
package com.vsign.backend.payment.controller;

import com.vsign.backend.payment.service.PayOSPaymentService;
import com.vsign.backend.payment.service.PayOSWebhookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.payos.PayOS;
import vn.payos.model.webhooks.WebhookData;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PayOSWebhookController.class, PayOSPaymentController.class})
class PayOSWebhookControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean PayOS payOS;
    @MockBean PayOSWebhookService webhookService;
    @MockBean PayOSPaymentService paymentService;

    @Test
    void webhookGetReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/payments/payos/webhook"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Test
    void webhookPostRejectsInvalidSignature() throws Exception {
        when(payOS.verifyPaymentWebhookData(any())).thenThrow(new RuntimeException("bad signature"));

        mockMvc.perform(post("/api/v1/payments/payos/webhook")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid webhook"));
    }

    @Test
    void tiersEndpointIsPublic() throws Exception {
        when(paymentService.listActiveTiers()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/payments/tiers"))
                .andExpect(status().isOk());
    }

    @Test
    void checkoutRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/payments/checkout")
                        .contentType("application/json")
                        .content("{\"tierId\":\"00000000-0000-0000-0000-000000000002\"}"))
                .andExpect(status().isUnauthorized());
    }
}
```

> **Note:** `@WebMvcTest` auto-loads `SecurityConfig`. If startup fails because `JwtAuthFilter` beans can't be resolved, add `@MockBean com.vsign.backend.common.security.JwtAuthFilter jwtAuthFilter;` to the test class.

- [ ] **Step 5: Run tests**

```powershell
cd v-sign-be; mvn -q -Dtest=PayOSWebhookControllerTest test
```

Expected: `BUILD SUCCESS`, 4 tests pass.

- [ ] **Step 6: Full compile check**

```powershell
cd v-sign-be; mvn -q -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit**

```bash
git add v-sign-be/src/main/java/com/vsign/backend/payment/controller/ v-sign-be/src/main/java/com/vsign/backend/common/security/SecurityConfig.java v-sign-be/src/test/java/com/vsign/backend/payment/controller/
git commit -m "feat(payment): add payos controllers and public security routes"
```

---

## Task 9: Add Expiry Scheduler

**Files:**
- Modify: `VSignBackendApplication.java`
- Create: `payment/service/PaymentExpiryScheduler.java`

- [ ] **Step 1: Add @EnableScheduling to VSignBackendApplication.java**

Current content of `VSignBackendApplication.java`:
```java
@SpringBootApplication
public class VSignBackendApplication {
    public static void main(String[] args) { SpringApplication.run(VSignBackendApplication.class, args); }
}
```

Replace with:
```java
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class VSignBackendApplication {
    public static void main(String[] args) { SpringApplication.run(VSignBackendApplication.class, args); }
}
```

- [ ] **Step 2: Create PaymentExpiryScheduler.java**

Create `payment/service/PaymentExpiryScheduler.java`:

```java
package com.vsign.backend.payment.service;

import com.vsign.backend.payment.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentExpiryScheduler {

    private final PayOSOrderRepository orderRepository;
    private final UserTierRepository userTierRepository;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expirePendingOrders() {
        List<PayOSOrderEntity> expired = orderRepository.findExpiredPendingOrders(
                PaymentOrderStatus.PENDING, LocalDateTime.now());
        if (!expired.isEmpty()) {
            expired.forEach(o -> o.setStatus(PaymentOrderStatus.EXPIRED));
            orderRepository.saveAll(expired);
            log.info("Expired {} pending payment orders", expired.size());
        }
    }

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void expireUserTierSubscriptions() {
        List<UserTierEntity> expired = userTierRepository.findExpiredPaidSubscriptions(LocalDateTime.now());
        if (!expired.isEmpty()) {
            expired.forEach(ut -> ut.setIsActive(false));
            userTierRepository.saveAll(expired);
            log.info("Deactivated {} expired paid subscriptions", expired.size());
        }
    }
}
```

- [ ] **Step 3: Compile**

```powershell
cd v-sign-be; mvn -q -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

```bash
git add v-sign-be/src/main/java/com/vsign/backend/VSignBackendApplication.java v-sign-be/src/main/java/com/vsign/backend/payment/service/PaymentExpiryScheduler.java
git commit -m "feat(payment): add payment expiry scheduler and enable scheduling"
```

---

## Task 10: Create Free UserTier on Registration

**Files:**
- Modify: `auth/service/AuthService.java`

- [ ] **Step 1: Add tier repositories to AuthService constructor**

Current constructor in `AuthService.java`:
```java
public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
}
```

Add two fields at the top of the class (after the existing three `final` fields):
```java
private final TierRepository tierRepository;
private final UserTierRepository userTierRepository;
```

Replace the constructor with:
```java
public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                   JwtService jwtService, TierRepository tierRepository,
                   UserTierRepository userTierRepository) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.tierRepository = tierRepository;
    this.userTierRepository = userTierRepository;
}
```

Add these imports at the top of the file:
```java
import com.vsign.backend.payment.persistence.TierRepository;
import com.vsign.backend.payment.persistence.UserTierEntity;
import com.vsign.backend.payment.persistence.UserTierRepository;
import java.time.LocalDateTime;
```

- [ ] **Step 2: Create free UserTier after user save in register()**

In `register()`, the last line is:
```java
return toAuthResponse(userRepository.save(user));
```

Replace with:
```java
UserEntity saved = userRepository.save(user);

tierRepository.findByTitleIgnoreCaseAndIsActiveTrueAndDeletedAtIsNull("free")
        .ifPresent(freeTier -> {
            UserTierEntity userTier = new UserTierEntity();
            userTier.setUser(saved);
            userTier.setTier(freeTier);
            userTier.setStartTime(LocalDateTime.now());
            int months = freeTier.getNoMonth() != null && freeTier.getNoMonth() > 0
                    ? freeTier.getNoMonth() : 120;
            userTier.setEndTime(LocalDateTime.now().plusMonths(months));
            userTier.setIsActive(true);
            userTierRepository.save(userTier);
        });

return toAuthResponse(saved);
```

- [ ] **Step 3: Compile**

```powershell
cd v-sign-be; mvn -q -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Run existing auth tests**

```powershell
cd v-sign-be; mvn -q "-Dtest=*Auth*" test
```

Expected: all existing auth tests pass. If any test constructs `AuthService` directly (not via Spring context), add two `mock(TierRepository.class)` and `mock(UserTierRepository.class)` arguments to that constructor call.

- [ ] **Step 5: Commit**

```bash
git add v-sign-be/src/main/java/com/vsign/backend/auth/service/AuthService.java
git commit -m "feat(payment): create free user tier on registration"
```

---

## Task 11: Final Validation

- [ ] **Step 1: Run full test suite**

```powershell
cd v-sign-be; mvn test
```

Expected: `BUILD SUCCESS`, all tests pass.

- [ ] **Step 2: Verify endpoint map (manual curl)**

All URLs include the `/V-sign` servlet prefix:

```powershell
# Tiers - public, no auth needed
curl -s http://localhost:8080/V-sign/api/v1/payments/tiers

# Webhook GET - public
curl -s http://localhost:8080/V-sign/api/v1/payments/payos/webhook

# Checkout - no JWT -> should return 401
curl -s -X POST http://localhost:8080/V-sign/api/v1/payments/checkout `
  -H "Content-Type: application/json" -d '{"tierId":"00000000-0000-0000-0000-000000000002"}'

# Checkout with JWT
curl -s -X POST http://localhost:8080/V-sign/api/v1/payments/checkout `
  -H "Authorization: Bearer YOUR_JWT" `
  -H "Content-Type: application/json" `
  -d '{"tierId":"00000000-0000-0000-0000-000000000002"}'

# Invalid webhook signature -> should return 400
curl -s -X POST http://localhost:8080/V-sign/api/v1/payments/payos/webhook `
  -H "Content-Type: application/json" -d '{"code":"00","orderCode":999}'
```

Expected results:

| Request | Expected |
|---------|---------|
| GET /tiers | 200, JSON array with free/plus/pro |
| GET /payos/webhook | 200 `OK` |
| POST /checkout no JWT | 401 |
| POST /checkout with JWT + valid tierId | 200 with `checkoutUrl` |
| POST /payos/webhook bad body | 400 `Invalid webhook` |

- [ ] **Step 3: Set PayOS credentials for local run**

Add to `src/main/resources/secretKey.properties` (git-ignored):
```properties
PAYOS_CLIENT_ID=<from https://my.payos.vn>
PAYOS_API_KEY=<from https://my.payos.vn>
PAYOS_CHECKSUM_KEY=<from https://my.payos.vn>
PAYOS_RETURN_URL=http://localhost:5173/payment/payos/return
PAYOS_CANCEL_URL=http://localhost:5173/payment/payos/cancel
```

- [ ] **Step 4: Register webhook URL in PayOS dashboard**

Start ngrok: `ngrok http 8080`

Register in https://my.payos.vn:
```
https://<ngrok-id>.ngrok-free.app/V-sign/api/v1/payments/payos/webhook
```

PayOS sends a GET to verify reachability — the `GET /payos/webhook` endpoint responds `200 OK`.

---

## Risk Checklist

- [ ] Browser return URL never activates a tier — only verified webhook does
- [ ] Webhook endpoint has no JWT guard — PayOS servers cannot send user tokens
- [ ] Webhook signature verified via SDK before any state change
- [ ] Duplicate webhooks are idempotent — PAID order ignores re-delivery
- [ ] Amount and paymentLinkId validated before upgrading user tier
- [ ] Paid subscription stacking blocked in both checkout and webhook flows
- [ ] Order description ≤ 25 chars — `VSIGN` + 6 digits = 11 chars ✓
- [ ] `order_code` UNIQUE constraint in migration
- [ ] Webhook URL in PayOS dashboard includes `/V-sign` servlet prefix
- [ ] `secretKey.properties` is git-ignored and never committed

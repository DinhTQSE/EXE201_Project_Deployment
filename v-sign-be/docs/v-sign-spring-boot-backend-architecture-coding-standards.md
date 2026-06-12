# SPRING BOOT BACKEND ARCHITECTURE & CODING STANDARDS (V-SIGN PROJECT)

This document defines the strict software architecture, directory structure, and coding guidelines mandatory for all developers working on the V-Sign backend ecosystem.

---

## 1. PROJECT STRUCTURE (PACKAGE-BY-FEATURE)

The project leverages a hybrid architecture blending **Clean Architecture** principles with a **Package-by-Feature (Domain-Driven Design)** layout. Each domain encapsulates its entire lifecycle to maximize decoupling, testability, and microservice readiness.

```text
src/main/java/com/vsign/backend
│
├── config/                         # Infrastructure & Framework Configurations
│   ├── security/                   # Spring Security, JWT, and OAuth2
│   ├── database/                   # Transaction, Multi-Datasource, JPA Auditing
│   ├── async/                      # ThreadPoolTaskExecutor for async processing
│   └── cache/                      # Distributed (Redis) & Local (Caffeine) caching
│
├── domain/                         # Core Business Domain Components
│   ├── auth/                       # Authentication & Authorization Module
│   │   ├── controller/             # REST API Layer (Handles HTTP requests, inputs validation)
│   │   ├── service/                # Business logic contracts (Interfaces)
│   │   │   └── impl/               # Concrete business logic implementations
│   │   ├── repository/             # Data Access Objects (Spring Data JPA / QueryDSL)
│   │   ├── entity/                 # Database mapping layer (JPA Entities)
│   │   ├── mapper/                 # Object-to-Object transformation (MapStruct)
│   │   └── dto/                    # Data Transfer Objects (Immutable Records)
│   │
│   ├── admin/                      # System Administration Module
│   ├── assessment/                 # Tests, Quizzes & Examinations Module
│   ├── dictionary/                 # Sign Language Dictionary & Vocabulary Module
│   ├── gamification/               # User engagement, points, rewards & streaks
│   ├── learning/                   # Courses, Syllabus & Curriculum Materials
│   └── monetization/               # Pricing plans, subscriptions & transactions
│
├── shared/                         # Cross-Cutting Concerns / Common Infrastructure
│   ├── exception/                  # Unified Error Handling Matrix
│   │   ├── global/                 # @RestControllerAdvice system fallback handling
│   │   └── custom/                 # Domain-specific Runtime Exceptions
│   ├── dto/                        # Standardized Global API Envelopes
│   ├── constant/                   # Central Enums, System Variables, and Error Codes
│   └── utils/                      # Non-state Utility classes (Date, Security, String helpers)
│
└── BackendApplication.java         # Application Entrypoint Bootstrap Class
```

### Core Layer Boundaries & Contractual Invariants

#### Controller

Solely handles routing and communication. Unwraps payloads, triggers structural syntax checks via `@Valid`, delegates to the matching Service, and envelopes the output inside a standard `ApiResponse`.

Business computations or raw data fetches inside Controllers are strictly prohibited.

#### Service

Contains the domain workflow rules. It governs state changes, checks validation boundaries, and serves as the transactional safety net via `@Transactional`.

#### Repository

Layer of storage abstraction. Isolated completely to I/O routines. Complex calculations should be kept out of SQL statements unless performance tuning demands it.

#### Entity

Direct blueprint matching of relational tables. Never serialize Entities directly across HTTP layers.

---

## 2. CODING RULES & BEST PRACTICES

### 2.1. Dependency Injection Standard

**Rule:** All bean mappings must use explicit Constructor Injection. The `@Autowired` annotation on fields is forbidden.

**Invariant:** Ensures immutability via `final`, enforces clean design patterns without structural dependency loops, and allows mock injection during isolated unit testing.

**Implementation Pattern:** Utilize Lombok's `@RequiredArgsConstructor`.

```java
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
}
```

### 2.2. DTO Isolation Boundary

**Rule:** Database Entities must never bleed past the Service Layer boundary into Controllers or out to external consumers.

**Invariant:** Prevents unwanted entity mutations, eliminates `LazyInitializationException` loops during Jackson serialization, and blocks mass assignment exploits.

**Implementation Pattern:** Define explicit Data Transfer Objects using Java Records (Java 16+) to benefit from native immutability and zero boilerplate. Leverage MapStruct for fast, compiled data copying.

```java
package com.vsign.backend.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "Username cannot be blank")
    String username,

    @NotBlank(message = "Password cannot be blank")
    String password
) {}
```

### 2.3. Transaction Management Strategy

**Rule:** Transactions (`@Transactional`) must only be initialized inside the Service Layer.

**Optimization Requirement:** Pure analytical/fetch routines must be strictly marked with `@Transactional(readOnly = true)`.

**Invariant:** Directs Hibernate to switch off dirty checking mechanisms, reducing memory footprint and speeding up overall query dispatching times.

```java
@Service
@RequiredArgsConstructor
public class LearningServiceImpl implements LearningService {
    private final CourseRepository courseRepository;

    @Override
    @Transactional(readOnly = true)
    public CourseResponse getCourseById(Long id) {
        return courseRepository.findById(id)
            .map(CourseMapper.INSTANCE::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + id));
    }
}
```

### 2.4. Global Matrix Exception Management

**Rule:** Catching exceptions inside localized try-catch loops to emit mechanical status objects is prohibited. Let errors cascade naturally to the API facade.

A centralized Controller Advice intercepted framework captures and formats the layout uniformly.

```java
package com.vsign.backend.shared.dto;

import java.time.LocalDateTime;

public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Success", data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, LocalDateTime.now());
    }
}
```

---

## 3. NAMING CONVENTIONS

| Component | Convention | Example |
|---|---|---|
| Class / Interface | PascalCase | `AssessmentService`, `DictionaryRepository` |
| Method / Variable | camelCase | `calculateScore()`, `userId` |
| Constant / Enum | UPPER_SNAKE_CASE | `MAX_RETRY_LIMIT`, `GAME_STATUS` |
| Package Space | Lowercase, singular nouns | `com.vsign.backend.domain.gamification` |
| Database Table | snake_case, pluralized nouns | `assessments`, `vocabularies` |
| Database Column | snake_case | `created_at`, `total_points` |
| REST API Endpoint | kebab-case, pluralized path with versioning | `/api/v1/learning-materials` |

---

## 4. DATA AUDITING (BASE ENTITY)

Every operational relational table must inherently trace historical adjustments via an abstract audit mapping superclass driven by Spring Data JPA Auditing.

```java
package com.vsign.backend.shared.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
}
```

You are a senior backend architect and Java Spring Boot expert.

I want you to design and write a very detailed backend solution using Java Spring Boot with a clean 3-layer architecture.

The backend must include these layers:

1. Controller Layer
   - REST API endpoints
   - Request/response DTOs
   - Input validation
   - Proper HTTP status codes
   - Standard success response format
   - Standard error response format
   - CORS configuration for React frontend

2. Service Layer
   - Business logic
   - Transaction management
   - Validation rules
   - JWT authentication integration
   - Centralized exception usage
   - Clear separation between controller and repository logic

3. Repository Layer
   - Spring Data JPA repositories
   - Entity relationships
   - Query methods
   - Custom queries if needed
   - Data access only, no HTTP-specific logic

Technology stack requirements:

- Java 17 or later
- Spring Boot 3.x
- Spring Web
- Spring Data JPA
- PostgreSQL database
- Flyway database migration
- JWT authentication and authorization
- Spring Security 6
- Maven or Gradle, but prefer Maven unless there is a strong reason not to
- React frontend integration
- RESTful API design
- Environment-based configuration using application.yml or application.properties

Database requirements:

- Use PostgreSQL
- Design proper tables, primary keys, foreign keys, indexes, and constraints
- Use Flyway migration scripts instead of Hibernate auto DDL for production
- Include sample migration files such as:
  - V1__create_users_table.sql
  - V2__create_roles_table.sql
  - V3__create_business_tables.sql
- Explain how the Spring Boot application connects to PostgreSQL
- Include database configuration examples
- Do not rely on spring.jpa.hibernate.ddl-auto=create for production
- Use ddl-auto=validate or none for production-style configuration

Authentication requirements:

- Implement JWT-based authentication
- Include login API
- Include register API if needed
- Include password hashing using BCrypt
- Include JWT token generation
- Include JWT validation filter
- Include Spring Security configuration
- Use Spring Security 6 compatible configuration
- Avoid deprecated Spring Security configuration style
- Explain how React frontend should store and send the JWT token
- Explain the Authorization header format:
  Authorization: Bearer <token>

React frontend integration requirements:

- Explain how the React frontend communicates with the Spring Boot backend
- Include CORS setup
- Include API base URL configuration example
- Include sample React Axios request examples for:
  - Login
  - Register if applicable
  - Calling a protected API
  - Handling 400 validation errors
  - Handling 401 Unauthorized
  - Handling 403 Forbidden
  - Handling 404 Not Found
  - Handling 409 Conflict
  - Handling 500 Internal Server Error
- Explain the expected request and response JSON formats

Deliverables I want from you:

1. Project folder structure
2. Maven pom.xml dependencies
3. application.yml configuration
4. PostgreSQL database design
5. Flyway migration scripts
6. Entity classes
7. DTO classes
8. Repository interfaces
9. Service interfaces and implementations
10. Controller classes
11. JWT utility class
12. JWT authentication filter
13. Spring Security configuration
14. Centralized exception component
15. Global exception handling
16. API success response wrapper format
17. API error response format
18. Validation examples
19. React integration examples
20. Example API flow from frontend to backend
21. Testing strategy
22. Best practices and common mistakes to avoid

Important coding rules:

- Use clean, production-ready code
- Do not put business logic inside controllers
- Do not access repositories directly from controllers
- Use DTOs instead of exposing entities directly
- Use constructor injection, not field injection
- Use meaningful package names
- Use clear class names and method names
- Use @Transactional where appropriate
- Use centralized exception handling
- Do not throw random RuntimeException, IllegalArgumentException, or generic Exception for business logic
- Do not rely on spring.jpa.hibernate.ddl-auto=create for production
- Use Flyway for schema changes
- Keep the code compatible with Spring Boot 3.x
- Avoid deprecated Spring Security configuration style
- Explain each important part of the solution

Exception handling requirements:

Design exception handling as a centralized, reusable backend component, not as random throw statements scattered across the project.

All exception-related code must be organized inside one dedicated package/folder:

com.example.project.common.exception

Do not place exception classes randomly inside feature packages such as user.exception, auth.exception, product.exception, etc.

The exception component must be designed once and reused everywhere across the backend project.

The exception component must include the following parts:

1. ErrorCode definition

Create a centralized ErrorCode enum or class.

Each ErrorCode must include:

- Application error code
- Default error message
- HTTP status

Example error codes:

- USER_NOT_FOUND
- EMAIL_ALREADY_EXISTS
- INVALID_CREDENTIALS
- ACCESS_DENIED
- VALIDATION_FAILED
- BAD_REQUEST
- UNAUTHORIZED
- FORBIDDEN
- DUPLICATE_RESOURCE
- RESOURCE_NOT_FOUND
- INTERNAL_SERVER_ERROR

Example structure:

public enum ErrorCode {
    USER_NOT_FOUND("USER_NOT_FOUND", "User was not found", HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_EXISTS("EMAIL_ALREADY_EXISTS", "Email already exists", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "Invalid email or password", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("ACCESS_DENIED", "Access denied", HttpStatus.FORBIDDEN),
    VALIDATION_FAILED("VALIDATION_FAILED", "Validation failed", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;
}

2. Custom exception classes

Create reusable custom exception classes under:

com.example.project.common.exception

Required classes:

- BusinessException
- ResourceNotFoundException
- DuplicateResourceException
- BadRequestException
- UnauthorizedException
- ForbiddenException
- ValidationException

All business exceptions must use ErrorCode.

Example usage:

throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND);

throw new DuplicateResourceException(ErrorCode.EMAIL_ALREADY_EXISTS);

throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS);

Do not throw random RuntimeException, IllegalArgumentException, or generic Exception for business logic.

3. Standard error response model

Create a consistent ErrorResponse DTO.

The response should include:

- timestamp
- status
- error
- code
- message
- path
- validationErrors

Create a FieldErrorResponse DTO for validation errors.

Expected normal error response format:

{
  "timestamp": "2026-05-20T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "code": "USER_NOT_FOUND",
  "message": "User was not found",
  "path": "/api/v1/users/10",
  "validationErrors": []
}

Expected validation error response format:

{
  "timestamp": "2026-05-20T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_FAILED",
  "message": "Validation failed",
  "path": "/api/v1/auth/register",
  "validationErrors": [
    {
      "field": "email",
      "message": "Email must be valid"
    },
    {
      "field": "password",
      "message": "Password must be at least 8 characters"
    }
  ]
}

4. Global exception handler

Create one GlobalExceptionHandler using @RestControllerAdvice.

The GlobalExceptionHandler must handle:

- BusinessException
- ResourceNotFoundException
- DuplicateResourceException
- BadRequestException
- UnauthorizedException
- ForbiddenException
- ValidationException
- MethodArgumentNotValidException
- ConstraintViolationException
- AuthenticationException
- AccessDeniedException
- Exception as fallback

Rules for GlobalExceptionHandler:

- All custom exceptions must be handled in one place.
- Validation errors from @Valid and @Validated must be converted into the standard ErrorResponse format.
- Spring Security authentication and authorization errors must be converted into the standard ErrorResponse format.
- Unexpected exceptions must be logged internally.
- Unexpected exceptions must return a safe generic response.
- Do not expose stack traces.
- Do not expose raw internal exception messages to React frontend.
- Every API error response must follow the same JSON structure.

5. Layer usage rules for exceptions

Controller layer:

- Controllers must not manually build error responses.
- Controllers must not contain try-catch blocks for normal business exceptions.
- Controllers should only receive requests, call services, and return successful responses.
- Controllers should rely on GlobalExceptionHandler for error responses.

Service layer:

- Services must throw centralized custom exceptions.
- Services must use ErrorCode when throwing exceptions.
- Business validation should be handled in the service layer.
- Use meaningful exceptions for each business case.

Example:

public User getUserById(Long id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
}

Example:

public void validateEmailNotExists(String email) {
    if (userRepository.existsByEmail(email)) {
        throw new DuplicateResourceException(ErrorCode.EMAIL_ALREADY_EXISTS);
    }
}

Repository layer:

- Repositories must not know about HTTP status codes.
- Repositories must not throw web-specific exceptions.
- Repositories should only handle data access using Spring Data JPA.
- Service layer is responsible for converting missing data or invalid states into custom exceptions.

6. Required exception package structure

Provide a clear folder/package structure similar to this:

src/main/java/com/example/project/common/exception/
    ErrorCode.java
    BusinessException.java
    ResourceNotFoundException.java
    DuplicateResourceException.java
    BadRequestException.java
    UnauthorizedException.java
    ForbiddenException.java
    ValidationException.java
    ErrorResponse.java
    FieldErrorResponse.java
    GlobalExceptionHandler.java

7. Required exception output

Please provide complete production-ready code for:

- ErrorCode enum
- BusinessException
- ResourceNotFoundException
- DuplicateResourceException
- BadRequestException
- UnauthorizedException
- ForbiddenException
- ValidationException
- ErrorResponse DTO
- FieldErrorResponse DTO
- GlobalExceptionHandler

Also provide example usage from:

- AuthService
- UserService
- Controller layer

8. Important exception implementation rules

- Use constructor injection where needed.
- Use clean class names and method names.
- Keep exception handling compatible with Spring Boot 3.x.
- Use Spring Security 6 compatible exception handling.
- Use proper HTTP status codes from ErrorCode.
- Use consistent JSON response structure for React frontend.
- Do not duplicate error response logic in multiple controllers.
- Do not create separate exception handling logic for each feature.
- Do not return different error response formats for different APIs.
- Do not rely on default Spring Boot error response for business errors.
- Do not expose sensitive information in error responses.

9. React frontend behavior for exceptions

Explain how the React frontend should consume these error responses.

The React frontend should be able to:

- Read error.response.data.code
- Read error.response.data.message
- Read error.response.data.validationErrors
- Handle 400 validation errors
- Handle 401 unauthorized errors
- Handle 403 forbidden errors
- Handle 404 not found errors
- Handle 409 duplicate resource errors
- Handle 500 internal server errors

Provide Axios error handling example based on the standard ErrorResponse format.

10. Final exception expectation

The exception component must be reusable across the whole backend project.

Every business error must flow like this:

Service throws centralized custom exception with ErrorCode
-> GlobalExceptionHandler catches it
-> GlobalExceptionHandler converts it to standard ErrorResponse
-> React frontend receives consistent JSON error response

Do not scatter exception classes or exception response logic across controllers, services, or feature packages.

Please structure the answer like this:

## 1. Overview

Explain the architecture and how React, Spring Boot, PostgreSQL, JWT, JPA, Flyway, and centralized exception handling work together.

## 2. Project Structure

Show the recommended folder/package structure.

The structure must include:

- controller package
- service package
- repository package
- entity package
- dto package
- security package
- config package
- common.exception package
- mapper package if needed

## 3. Dependencies

Provide the Maven pom.xml dependencies.

Include dependencies for:

- Spring Web
- Spring Data JPA
- PostgreSQL Driver
- Flyway
- Spring Security
- JWT library
- Validation
- Lombok if used
- Testing dependencies

## 4. Configuration

Provide application.yml for:

- PostgreSQL
- JPA
- Flyway
- JWT
- CORS-related values
- Spring profiles if needed

## 5. Database Design and Flyway Migration

Provide PostgreSQL table design and Flyway SQL scripts.

Include:

- users table
- roles table
- user_roles join table if using many-to-many role design
- at least one sample business table
- indexes
- constraints
- foreign keys

## 6. Entity Layer

Provide complete entity classes.

Requirements:

- Use JPA annotations correctly
- Do not expose entities directly from controllers
- Explain relationships
- Include auditing fields if useful, such as created_at and updated_at

## 7. DTO Layer

Provide request and response DTOs.

Include:

- LoginRequest
- LoginResponse
- RegisterRequest
- UserResponse
- Sample business request DTO
- Sample business response DTO

Use validation annotations such as:

- @NotBlank
- @NotNull
- @Email
- @Size
- @Pattern if needed

## 8. Repository Layer

Provide Spring Data JPA repository interfaces.

Include:

- UserRepository
- RoleRepository
- Sample business repository

Show query methods and custom @Query examples if useful.

## 9. Service Layer

Provide service interfaces and service implementations.

Requirements:

- Put business logic here
- Use @Transactional where appropriate
- Use centralized exceptions from common.exception
- Do not return entities directly unless justified
- Convert entities to DTOs

## 10. Controller Layer

Provide REST controllers with endpoints.

Requirements:

- Use @RestController
- Use @RequestMapping
- Use @Valid
- Return proper ResponseEntity
- Do not use try-catch for normal business errors
- Do not manually build error responses for exceptions
- Rely on GlobalExceptionHandler

## 11. JWT Authentication

Provide complete JWT implementation.

Include:

- JwtService or JwtUtil
- JwtAuthenticationFilter
- CustomUserDetailsService
- SecurityConfig
- PasswordEncoder bean
- AuthenticationManager bean if needed
- Public endpoints
- Protected endpoints
- Role-based authorization example

## 12. Centralized Exception Component

Design exception handling as a reusable component under:

com.example.project.common.exception

Provide complete code for:

- ErrorCode enum
- BusinessException
- ResourceNotFoundException
- DuplicateResourceException
- BadRequestException
- UnauthorizedException
- ForbiddenException
- ValidationException
- ErrorResponse DTO
- FieldErrorResponse DTO
- GlobalExceptionHandler

Also explain:

- How services should throw exceptions
- How controllers should avoid try-catch
- How validation errors are returned
- How security errors are returned
- How React frontend should consume the error response

## 13. API Success Response Wrapper

Provide a standard success response wrapper.

Example:

{
  "success": true,
  "message": "Request processed successfully",
  "data": {}
}

Explain when to use it and when not to use it.

## 14. React Frontend Integration

Explain:

- CORS
- Axios setup
- API base URL
- Login flow
- Register flow if applicable
- Token storage strategy
- Authorization header
- Protected API call
- Global Axios response interceptor
- Handling standardized error responses

## 15. API Examples

Provide sample request and response JSON for all important APIs.

Include:

- Register
- Login
- Get current user
- Get user by ID
- Create sample business resource
- Get sample business resource
- Validation error example
- Unauthorized error example
- Forbidden error example
- Not found error example
- Duplicate resource error example

## 16. Testing Strategy

Explain testing for:

- Unit tests
- Service tests
- Repository tests
- Controller tests
- Security tests
- Integration tests
- Exception handler tests

Provide example test cases if useful.

## 17. Best Practices

List best practices and common mistakes to avoid.

Must include:

- Do not expose entities directly
- Do not put business logic in controllers
- Do not call repositories from controllers
- Do not use ddl-auto=create in production
- Do not scatter exception classes
- Do not throw random RuntimeException for business errors
- Do not expose stack traces to React frontend
- Do not store JWT in unsafe ways without understanding the tradeoffs
- Use Flyway for database schema changes
- Use constructor injection
- Use DTOs and validation
- Use proper HTTP status codes
- Keep security configuration compatible with Spring Boot 3.x and Spring Security 6

Make the answer detailed enough that a developer can directly implement the backend project from your explanation.

Before writing the solution, first make reasonable assumptions about the sample business domain.

Use a simple but realistic domain such as:

- User
- Role
- Product
- Order

unless I provide another domain.

Do not give only explanations.

Provide actual production-style code examples for every important class.

If any part depends on project-specific requirements, clearly mark it as an assumption instead of inventing hidden requirements.

Prefer correctness, maintainability, security, and clean architecture over short answers.
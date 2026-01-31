# UTEM Core - Contributing Guidelines

## Project Structure

```
utem-core/
├── src/
│   ├── main/
│   │   ├── java/com/utem/utem_core/
│   │   │   ├── config/           # Configuration classes (WebSocket, Security, etc.)
│   │   │   ├── controller/       # REST API controllers
│   │   │   ├── dto/              # Data Transfer Objects (request/response)
│   │   │   ├── entity/           # JPA entities
│   │   │   ├── exception/        # Custom exceptions and handlers
│   │   │   ├── repository/       # Spring Data JPA repositories
│   │   │   ├── service/          # Business logic services
│   │   │   └── util/             # Utility classes
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/           # Static resources (if any)
│   └── test/
│       └── java/com/utem/utem_core/
│           ├── controller/       # Controller tests
│           ├── repository/       # Repository tests
│           └── service/          # Service tests
├── frontend/                     # React frontend (Phase: MVP Frontend)
│   ├── src/
│   │   ├── components/           # React components
│   │   ├── hooks/                # Custom React hooks
│   │   ├── services/             # API and WebSocket services
│   │   ├── types/                # TypeScript types
│   │   └── utils/                # Utility functions
│   └── package.json
├── pom.xml
├── README.md
├── CONTRIBUTING.md
└── TODO.md
```

---

## Naming Conventions

### Java

| Type | Convention | Example |
|------|------------|---------|
| Package | lowercase, no underscores | `com.utem.utem_core.service` |
| Class | PascalCase | `TestRunService` |
| Interface | PascalCase | `EventProcessor` |
| Method | camelCase | `findByRunId()` |
| Variable | camelCase | `testRunId` |
| Constant | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| Entity | Singular noun | `TestRun`, `TestNode` |
| Repository | Entity + Repository | `TestRunRepository` |
| Service | Entity/Feature + Service | `TestRunService`, `EventProcessingService` |
| Controller | Entity/Feature + Controller | `EventController`, `RunHistoryController` |
| DTO | Purpose + Request/Response | `CreateRunRequest`, `RunDetailsResponse` |

### Frontend (React/TypeScript)

| Type | Convention | Example |
|------|------------|---------|
| Component | PascalCase | `TestRunList.tsx` |
| Hook | camelCase with `use` prefix | `useWebSocket.ts` |
| Service | camelCase | `apiService.ts` |
| Type/Interface | PascalCase | `TestRun`, `EventPayload` |
| Constant | UPPER_SNAKE_CASE | `API_BASE_URL` |

---

## Coding Standards

### Java/Spring Boot

#### General Rules

1. **Maximum line length**: 120 characters
2. **Indentation**: 4 spaces (no tabs)
3. **Braces**: Opening brace on same line
4. **Imports**: No wildcard imports (`import java.util.*`)

#### Entity Rules

```java
@Entity
@Table(name = "table_name")  // Always specify table name
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityName {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;  // Always use String UUID for primary keys

    @Column(nullable = false)  // Explicitly mark non-null columns
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)  // Always use LAZY fetch
    @JoinColumn(name = "foreign_key_id")
    private RelatedEntity relatedEntity;

    @Enumerated(EnumType.STRING)  // Always store enums as STRING
    private Status status;
}
```

#### Repository Rules

```java
@Repository
public interface EntityRepository extends JpaRepository<Entity, String> {
    // Use method naming conventions for simple queries
    List<Entity> findByStatus(Status status);

    // Use @Query for complex queries
    @Query("SELECT e FROM Entity e WHERE e.name LIKE %:name%")
    List<Entity> searchByName(@Param("name") String name);
}
```

#### Service Rules

```java
@Service
@RequiredArgsConstructor  // Use constructor injection via Lombok
public class EntityService {

    private final EntityRepository entityRepository;
    private final OtherService otherService;

    @Transactional  // Mark write operations
    public Entity create(CreateRequest request) {
        // Business logic
    }

    @Transactional(readOnly = true)  // Mark read-only operations
    public Entity findById(String id) {
        return entityRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(id));
    }
}
```

#### Controller Rules

```java
@RestController
@RequestMapping("/utem/resource")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Response> create(@Valid @RequestBody Request request) {
        // Delegate to service
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response> getById(@PathVariable String id) {
        // Delegate to service
    }
}
```

#### Exception Handling

```java
// Custom exceptions in exception/ package
public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String id) {
        super("Entity not found with id: " + id);
    }
}

// Global exception handler
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(EntityNotFoundException ex) {
        return new ErrorResponse(ex.getMessage());
    }
}
```

### DTO Rules

```java
// Request DTOs - use validation annotations
public record CreateRunRequest(
    @NotBlank String name,
    @NotNull Map<String, Object> metadata
) {}

// Response DTOs - use records for immutability
public record RunResponse(
    String id,
    String name,
    String status,
    Instant startTime
) {}
```

---

## API Design Standards

### URL Patterns

| Operation | Method | URL Pattern | Example |
|-----------|--------|-------------|---------|
| Create | POST | `/utem/{resource}` | `POST /utem/runs` |
| Get One | GET | `/utem/{resource}/{id}` | `GET /utem/runs/123` |
| Get All | GET | `/utem/{resource}` | `GET /utem/runs` |
| Update | PUT | `/utem/{resource}/{id}` | `PUT /utem/runs/123` |
| Delete | DELETE | `/utem/{resource}/{id}` | `DELETE /utem/runs/123` |
| Action | POST | `/utem/{resource}/{id}/{action}` | `POST /utem/runs/123/stop` |

### Response Format

```json
// Success response
{
  "data": { ... },
  "timestamp": "2024-01-01T12:00:00Z"
}

// Error response
{
  "error": "Error message",
  "code": "ERROR_CODE",
  "timestamp": "2024-01-01T12:00:00Z"
}

// List response with pagination
{
  "data": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}
```

---

## Git Workflow

### Branch Naming

| Type | Pattern | Example |
|------|---------|---------|
| Feature | `feature/{task-id}-{short-description}` | `feature/6-event-ingestion-controller` |
| Bugfix | `bugfix/{task-id}-{short-description}` | `bugfix/15-search-filter-fix` |
| Hotfix | `hotfix/{description}` | `hotfix/critical-db-connection` |

### Commit Messages

Format: `{type}(#{task-id}): {description}`

Types:
- `feat` - New feature
- `fix` - Bug fix
- `refactor` - Code refactoring
- `test` - Adding tests
- `docs` - Documentation
- `chore` - Maintenance tasks

Examples:
```
feat(#6): add event ingestion controller
fix(#7): handle null parentId in event processing
refactor(#8): extract hierarchy builder to separate class
test(#6): add unit tests for EventController
docs(#1): update README with API examples
```

---

## PR Checklist

Before submitting a PR, ensure:

### Code Quality

- [ ] Code follows naming conventions
- [ ] No unused imports or variables
- [ ] No commented-out code
- [ ] No hardcoded values (use constants or config)
- [ ] Proper error handling implemented
- [ ] No sensitive data in code (passwords, keys)

### Structure

- [ ] Files placed in correct packages
- [ ] DTOs used for API request/response (not entities)
- [ ] Services contain business logic (not controllers)
- [ ] Repository methods follow naming conventions

### Testing

- [ ] Unit tests written for new services
- [ ] Controller tests written for new endpoints
- [ ] All tests pass locally (`mvn test`)
- [ ] Edge cases covered

### Documentation

- [ ] Public methods have Javadoc (for services)
- [ ] API endpoints documented
- [ ] Complex logic has inline comments
- [ ] TODO.md updated if task completed

### Build

- [ ] Code compiles without errors (`mvn compile`)
- [ ] No new warnings introduced
- [ ] Application starts successfully (`mvn spring-boot:run`)

---

## Code Review Guidelines

### Reviewers Should Check

1. **Correctness**: Does the code do what it's supposed to?
2. **Design**: Is the code well-structured and follows patterns?
3. **Readability**: Is the code easy to understand?
4. **Performance**: Any obvious performance issues?
5. **Security**: Any security vulnerabilities?
6. **Tests**: Are tests adequate and meaningful?

### Review Comments

- Be constructive and specific
- Suggest improvements, not just point out problems
- Use prefixes:
  - `[MUST]` - Must be fixed before merge
  - `[SHOULD]` - Should be fixed, but not blocking
  - `[NIT]` - Minor suggestion, optional
  - `[QUESTION]` - Clarification needed

---

## Development Setup

### Prerequisites

- Java 17+
- Maven 3.8+
- Node.js 18+ (for frontend)
- IDE: IntelliJ IDEA recommended

### Running Locally

```bash
# Backend
mvn clean install
mvn spring-boot:run

# Frontend (when available)
cd frontend
npm install
npm run dev
```

### Running Tests

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=TestRunRepositoryTest

# With coverage
mvn test jacoco:report
```

### Test Annotations

```java
// Repository tests - use @SpringBootTest with @Transactional
@SpringBootTest
@Transactional
class RepositoryTest {
    @Autowired
    private EntityRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }
}

// Entity tests - pure unit tests, no Spring context needed
class EntityTest {
    @Test
    void shouldCreateEntity() {
        Entity entity = Entity.builder().name("test").build();
        assertThat(entity.getName()).isEqualTo("test");
    }
}
```

---

## Versioning

- Follow Semantic Versioning (SemVer): `MAJOR.MINOR.PATCH`
- Current version: `0.0.1-SNAPSHOT` (pre-release)
- First stable release: `1.0.0`

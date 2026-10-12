# Squelettes de code

Remplacer `Thing` / `thing` par le nom de la ressource.

## Entité

```java
@Entity
@Table(name = "thing")
@Getter @Setter @NoArgsConstructor
public class Thing {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shared_house_id", nullable = false, updatable = false)
    private Long sharedHouseId;

    @Column(nullable = false, length = 150)
    private String name;
}
```

## Repository

```java
public interface ThingRepository extends JpaRepository<Thing, Long> {
    List<Thing> findBySharedHouseIdOrderByIdDesc(Long sharedHouseId);
    Optional<Thing> findByIdAndSharedHouseId(Long id, Long sharedHouseId);
}
```

## DTO (records)

```java
public record ThingRequest(@NotBlank @Size(max = 150) String name) { }

public record ThingResponse(Long id, String name) {
    public static ThingResponse from(Thing thing) {
        return new ThingResponse(thing.getId(), thing.getName());
    }
}
```

## Service

```java
@Service
@RequiredArgsConstructor
public class ThingService {
    private final ThingRepository thingRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public ThingResponse getThing(Long id) {
        Long houseId = currentUserService.getCurrentSharedHouse().getId();
        Thing thing = thingRepository.findByIdAndSharedHouseId(id, houseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Élément introuvable"));
        return ThingResponse.from(thing);
    }

    @Transactional
    public ThingResponse createThing(ThingRequest request) {
        Thing thing = new Thing();
        thing.setSharedHouseId(currentUserService.getCurrentSharedHouse().getId());
        thing.setName(request.name().trim());
        return ThingResponse.from(thingRepository.save(thing));
    }
}
```

## Contrôleur

```java
@RestController
@RequestMapping("/api/things")
@RequiredArgsConstructor
public class ThingController {
    private final ThingService thingService;

    @GetMapping("/{id}")
    public ThingResponse getThing(@PathVariable Long id) {
        return thingService.getThing(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ThingResponse createThing(@Valid @RequestBody ThingRequest request) {
        return thingService.createThing(request);
    }
}
```

## Cas de test d'intégration

```java
@Test
void thingOfAnotherHouseIsNotVisible() throws Exception {
    User alice = register("Alice");
    createHouse(alice);
    // ... créer la ressource avec Alice, puis la lire avec quelqu'un d'une autre colocation
    mockMvc.perform(authed(get("/api/things/" + id), stranger))
            .andExpect(status().isNotFound());
}
```

package org.fiware.tmforum.storage;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The default {@link ReferenceReadRepository#findReferences}, which adapters without a batch lookup inherit.
 */
class ReferenceReadRepositoryTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final String TYPE = "category";
    private static final String EXISTING_1 = "urn:ngsi-ld:category:1";
    private static final String EXISTING_2 = "urn:ngsi-ld:category:2";
    private static final String MISSING = "urn:ngsi-ld:category:missing";

    private final List<String> lookedUp = new ArrayList<>();
    private final Map<String, String> stored = Map.of(EXISTING_1, TYPE, EXISTING_2, TYPE);

    private final ReferenceReadRepository repository = new ReferenceReadRepository() {
        @Override
        public Mono<StoredReference> referenceExists(String id, List<String> acceptedTypes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Mono<StoredReference> findReference(String id) {
            lookedUp.add(id);
            return Mono.justOrEmpty(stored.get(id)).map(type -> new StoredReference(id, type));
        }
    };

    @Test
    void defaultEmitsEachExistingIdOnceAndSkipsUnknownOnes() {
        Set<StoredReference> found = repository.findReferences(List.of(EXISTING_1, MISSING, EXISTING_2, EXISTING_1))
                .collect(Collectors.toSet())
                .block(TIMEOUT);

        assertEquals(Set.of(new StoredReference(EXISTING_1, TYPE), new StoredReference(EXISTING_2, TYPE)), found);
        assertEquals(List.of(EXISTING_1, MISSING, EXISTING_2), lookedUp, "Each distinct id is looked up once.");
    }

    @Test
    void defaultEmitsNothingForNoIds() {
        assertEquals(List.of(), repository.findReferences(List.of()).collectList().block(TIMEOUT));
    }
}

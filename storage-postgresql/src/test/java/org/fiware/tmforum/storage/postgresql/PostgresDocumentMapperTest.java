package org.fiware.tmforum.storage.postgresql;

import io.github.wistefan.mapping.UnmappedProperty;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.fiware.tmforum.common.domain.Characteristic;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.domain.Money;
import org.fiware.tmforum.common.domain.RelatedParty;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.storage.StorageErrorReason;
import org.fiware.tmforum.storage.StorageException;
import org.fiware.tmforum.storage.StorageOperation;
import org.fiware.tmforum.storage.postgresql.DocumentTestEntities.TestCatalog;
import org.fiware.tmforum.storage.postgresql.DocumentTestEntities.TestLogicalResource;
import org.fiware.tmforum.storage.postgresql.DocumentTestEntities.TestNotStoredHolder;
import org.fiware.tmforum.storage.postgresql.DocumentTestEntities.TestSelfIgnoringCatalog;
import org.fiware.tmforum.storage.postgresql.DocumentTestEntities.TestSetterOnlyHolder;
import org.fiware.tmforum.storage.postgresql.DocumentTestEntities.TestUriRef;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.cfg.DateTimeFeature;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Document format of the PostgreSQL adapter, with the application mapper of the {@code postgresql} environment.
 */
@MicronautTest(environments = PostgresStorage.ENVIRONMENT, startApplication = false)
@Property(name = "flyway.datasources.default.enabled", value = "false")
class PostgresDocumentMapperTest {

    private static final String CATALOG_ID = "urn:ngsi-ld:" + TestCatalog.TYPE + ":catalog-1";
    private static final String RESOURCE_ID = "urn:ngsi-ld:" + TestLogicalResource.TYPE + ":resource-1";
    private static final String HOLDER_ID = "urn:ngsi-ld:" + TestNotStoredHolder.TYPE + ":holder-1";
    private static final String SUBSCRIPTION_ID =
            "urn:ngsi-ld:" + TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION + ":subscription-1";
    // carries NGSI-LD forbidden characters, which only the broker-facing accessors escape
    private static final String QUERY = "name=\"a b\"&lifecycleStatus=active";
    private static final String PARTY_ID = "urn:ngsi-ld:individual:party-1";
    private static final URI PARENT_ID = URI.create("urn:ngsi-ld:" + TestCatalog.TYPE + ":parent");
    private static final String NAME = "declared name";
    private static final String EXTENSION_NAME = "extension name";
    private static final String EXTENSION_KEY = "customField";
    private static final String NAME_PROPERTY = "name";
    private static final String KEYWORDS_PROPERTY = "keywords";
    private static final String VALID_FOR_PROPERTY = "validFor";
    private static final String BALANCE_PROPERTY = "balance";
    private static final String LAST_UPDATE_PROPERTY = "lastUpdate";
    private static final Instant LAST_UPDATE = Instant.parse("2026-11-10T08:15:30Z");
    private static final String NUL_MESSAGE_FORMAT =
            "Field '%s' contains the NUL character (\\u0000), which cannot be stored.";
    private static final String NUL = "\u0000";

    // names the document mapper must never write: the list and the getters only the application mapper ignores
    private static final Set<String> NEVER_STORED = Set.of(Entity.ADDITIONAL_PROPERTIES_KEY, "referencedTypes",
            "entityId", "entityState");

    @Inject
    private PostgresDocumentMapper documentMapper;

    @Inject
    private ObjectMapper applicationMapper;

    @ParameterizedTest
    @MethodSource("roundTrips")
    void roundTripsThroughTheDocument(Entity entity) {
        Entity read = readBack(documentMapper.storage(), entity, DocumentMode.FULL);

        // jsonb keeps no key order, so neither do the extensions
        assertEquals(sortExtensions(entity), sortExtensions(read));
        if (entity instanceof EntityWithId withId) {
            assertEquals(withId.getId(), ((EntityWithId) read).getId());
            assertEquals(withId.getType(), ((EntityWithId) read).getType());
        }
    }

    static Stream<Arguments> roundTrips() {
        return Stream.of(
                Arguments.of(Named.of("id only", new TestCatalog(CATALOG_ID))),
                Arguments.of(Named.of("lists, nested values and references", fullCatalog())),
                Arguments.of(Named.of("additional properties", withExtensions(fullCatalog()))),
                Arguments.of(Named.of("sub-type with atType and atBaseType", logicalResource())),
                Arguments.of(Named.of("subscription with broker-escaped query accessors", subscription())));
    }

    @ParameterizedTest
    @MethodSource("documentKeys")
    void writesExtensionsAsTopLevelKeys(Entity entity, Set<String> expectedKeys) {
        JsonNode document = document(documentMapper.storage(), entity, DocumentMode.FULL);

        assertEquals(expectedKeys, Set.copyOf(document.propertyNames()));
        NEVER_STORED.forEach(name -> assertFalse(document.has(name), name + " must not be stored."));
    }

    static Stream<Arguments> documentKeys() {
        return Stream.of(
                Arguments.of(Named.of("id only", new TestCatalog(CATALOG_ID)), Set.of("id", "type")),
                Arguments.of(Named.of("additional properties", withExtensions(new TestCatalog(CATALOG_ID))),
                        Set.of("id", "type", EXTENSION_KEY, "count", "nested", "list")),
                Arguments.of(Named.of("extension named like the extension list", withExtensionListKey()),
                        Set.of("id", "type", EXTENSION_KEY, "count", "nested", "list")),
                Arguments.of(Named.of("extension named like a setter-only property", withSetterOnlyExtension()),
                        Set.of("id", "type")),
                Arguments.of(Named.of("reference with only a URI constructor", catalogWithParent()),
                        Set.of("id", "type", "parent")),
                Arguments.of(Named.of("subscription without the broker-escaped queries", subscription()),
                        Set.of("id", "type", "callback", "query", "rawQuery", "eventTypes", "entities",
                                "fields")));
    }

    @Test
    void writesReferencesInlineWithTheirOwnFieldsOnly() {
        JsonNode parent = document(documentMapper.storage(), catalogWithParent(), DocumentMode.FULL).get("parent");

        assertEquals(Set.of("id", "href"), Set.copyOf(parent.propertyNames()), parent.toString());
    }

    @ParameterizedTest
    @MethodSource("typedEntities")
    void aDocumentWithoutExtensionsReadsBackWithoutAdditionalProperties(Class<? extends EntityWithId> entityClass,
                                                                        String type) {
        String id = "urn:ngsi-ld:" + type + ":entity-1";
        EntityWithId read = documentMapper.storage().fromDocument(
                "{\"id\":\"" + id + "\",\"type\":\"" + type + "\",\"name\":\"" + NAME + "\"}",
                entityClass, StorageOperation.READ);

        assertNull(read.getAdditionalProperties());
        assertEquals(id, read.getId().toString());
        assertEquals(type, read.getType());
    }

    private static Stream<Arguments> typedEntities() {
        return Stream.of(
                Arguments.of(TestCatalog.class, TestCatalog.TYPE),
                Arguments.of(Named.of("own class-level ignorals", TestSelfIgnoringCatalog.class),
                        TestSelfIgnoringCatalog.TYPE));
    }

    @ParameterizedTest
    @MethodSource("collidingNames")
    void aDeclaredPropertyWinsOverAnExtensionOfTheSameName(String declaredName) {
        TestCatalog catalog = new TestCatalog(CATALOG_ID);
        catalog.setName(declaredName);
        catalog.addAdditionalProperties(NAME_PROPERTY, EXTENSION_NAME);

        JsonNode document = document(documentMapper.storage(), catalog, DocumentMode.FULL);
        TestCatalog read = readBack(documentMapper.storage(), catalog, DocumentMode.FULL);

        assertEquals(declaredName, document.has(NAME_PROPERTY) ? document.get(NAME_PROPERTY).stringValue() : null);
        assertEquals(declaredName, read.getName());
        assertNull(read.getAdditionalProperties());
    }

    static Stream<Arguments> collidingNames() {
        return Stream.of(Arguments.of(Named.of("declared value set", NAME)),
                Arguments.of(Named.of("declared value null", null)));
    }

    @Test
    void updateModeWritesOnlyTheCarriedAttributes() {
        TestCatalog update = new TestCatalog(CATALOG_ID);
        update.setName(NAME);
        update.setKeywords(List.of());

        JsonNode document = document(documentMapper.storage(), update, DocumentMode.CARRIED_ATTRIBUTES);

        assertEquals(Set.of(NAME_PROPERTY, KEYWORDS_PROPERTY), Set.copyOf(document.propertyNames()));
        assertEquals(NAME, document.get(NAME_PROPERTY).stringValue());
        assertTrue(document.get(KEYWORDS_PROPERTY).isNull(), "An emptied list is the merge's removal marker.");
    }

    @ParameterizedTest
    @MethodSource("emptyExtensionValues")
    void emptyExtensionValuesAreKept(DocumentMode mode, String emptyValue) {
        TestCatalog catalog = catalog(c -> c.addAdditionalProperties(EXTENSION_KEY,
                applicationMapper.readValue(emptyValue, Object.class)));

        JsonNode document = document(documentMapper.storage(), catalog, mode);

        assertEquals(applicationMapper.readTree(emptyValue), document.get(EXTENSION_KEY), document.toString());
    }

    static Stream<Arguments> emptyExtensionValues() {
        return Stream.of(DocumentMode.values())
                .flatMap(mode -> Stream.of(Arguments.of(mode, "[]"), Arguments.of(mode, "{}")));
    }

    @ParameterizedTest
    @MethodSource("emptyDeclaredValuesWithCollidingExtension")
    void anEmptyDeclaredValueIsDroppedEvenWithAnExtensionOfTheSameName(DocumentMode mode, String declaredName,
            String extensionValue) {
        TestCatalog plain = emptyDeclaredValue(declaredName);
        TestCatalog colliding = emptyDeclaredValue(declaredName);
        colliding.addAdditionalProperties(declaredName, applicationMapper.readValue(extensionValue, Object.class));

        assertEquals(document(documentMapper.storage(), plain, mode),
                document(documentMapper.storage(), colliding, mode));
    }

    static Stream<Arguments> emptyDeclaredValuesWithCollidingExtension() {
        return Stream.of(DocumentMode.values()).flatMap(mode -> Stream.of(
                Arguments.of(mode, KEYWORDS_PROPERTY, "[]"),
                Arguments.of(mode, KEYWORDS_PROPERTY, "[\"an extension keyword\"]"),
                Arguments.of(mode, VALID_FOR_PROPERTY, "{}")));
    }

    private static TestCatalog emptyDeclaredValue(String declaredName) {
        TestCatalog catalog = new TestCatalog(CATALOG_ID);
        catalog.setName(NAME);
        if (KEYWORDS_PROPERTY.equals(declaredName)) {
            catalog.setKeywords(List.of());
        } else {
            catalog.setValidFor(new TimePeriod());
        }
        return catalog;
    }

    @ParameterizedTest
    @MethodSource("emptyValues")
    void emptyValuesAreDroppedOnlyFromTheStoredDocument(Function<PostgresDocumentMapper, DocumentMapper> variant,
            DocumentMode mode, boolean listDropped, boolean objectDropped) {
        DocumentMapper mapper = variant.apply(documentMapper);
        TestCatalog catalog = new TestCatalog(CATALOG_ID);
        catalog.setKeywords(List.of());
        catalog.setValidFor(new TimePeriod());

        JsonNode document = document(mapper, catalog, mode);

        assertEquals(!listDropped, document.has(KEYWORDS_PROPERTY), document.toString());
        assertEquals(!objectDropped, document.has(VALID_FOR_PROPERTY), document.toString());
    }

    static Stream<Arguments> emptyValues() {
        Function<PostgresDocumentMapper, DocumentMapper> storage = PostgresDocumentMapper::storage;
        Function<PostgresDocumentMapper, DocumentMapper> event = PostgresDocumentMapper::event;
        return Stream.of(
                Arguments.of(Named.of("storage", storage), DocumentMode.FULL, true, true),
                Arguments.of(Named.of("storage", storage), DocumentMode.CARRIED_ATTRIBUTES, false, true),
                Arguments.of(Named.of("event", event), DocumentMode.FULL, false, false),
                Arguments.of(Named.of("event", event), DocumentMode.CARRIED_ATTRIBUTES, false, false));
    }

    @ParameterizedTest
    @EnumSource(DocumentMode.class)
    void aNullExtensionIsNotWritten(DocumentMode mode) {
        JsonNode document = document(documentMapper.storage(), catalogWithNullExtension(), mode);

        assertFalse(document.has(EXTENSION_KEY), document.toString());
    }

    @Test
    void aNullExtensionReadsBackAbsent() {
        // only FULL: a CARRIED_ATTRIBUTES document has no id and cannot be read back on its own
        TestCatalog read = readBack(documentMapper.storage(), catalogWithNullExtension(), DocumentMode.FULL);

        assertNull(read.getAdditionalProperties());
    }

    @ParameterizedTest
    @MethodSource("applicationMapperSettings")
    void keysAndDatesIgnoreTheApplicationMapperSettings(Function<ObjectMapper, ObjectMapper> settings,
                                                        DocumentMode mode) {
        PostgresDocumentMapper mapper = new PostgresDocumentMapper(settings.apply(applicationMapper));
        TestCatalog catalog = catalog(c -> c.setLastUpdate(LAST_UPDATE));

        for (DocumentMapper variant : List.of(mapper.storage(), mapper.event())) {
            JsonNode lastUpdate = document(variant, catalog, mode).get(LAST_UPDATE_PROPERTY);

            assertNotNull(lastUpdate, "no '" + LAST_UPDATE_PROPERTY + "' key");
            assertTrue(lastUpdate.isString(), lastUpdate.toString());
            assertEquals(LAST_UPDATE.toString(), lastUpdate.stringValue());
        }
    }

    static Stream<Arguments> applicationMapperSettings() {
        List<Named<Function<ObjectMapper, ObjectMapper>>> settings = List.of(
                Named.of("as configured", Function.identity()),
                Named.of("dates as timestamps",
                        m -> m.rebuild().enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS).build()),
                Named.of("snake_case names",
                        m -> m.rebuild().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build()));
        return settings.stream()
                .flatMap(setting -> Stream.of(DocumentMode.values()).map(mode -> Arguments.of(setting, mode)));
    }

    @Test
    void storageDropsNotStoredFields() {
        TestNotStoredHolder holder = notStoredHolder();

        JsonNode document = document(documentMapper.storage(), holder, DocumentMode.FULL);
        TestNotStoredHolder read = readBack(documentMapper.storage(), holder, DocumentMode.FULL);

        assertFalse(document.has(BALANCE_PROPERTY));
        assertNull(read.getBalance());
        assertEquals(NAME, read.getName());
        assertNull(read.getAdditionalProperties());
    }

    @ParameterizedTest
    @MethodSource("eventMapperReaders")
    void eventKeepsNotStoredFieldsInTheSameFormat(Function<PostgresDocumentMapper, DocumentMapper> reader,
                                                  boolean balanceReadsBack) {
        TestNotStoredHolder holder = notStoredHolder();
        String document = documentMapper.event().toDocument(holder, DocumentMode.FULL, StorageOperation.CREATE);

        TestNotStoredHolder read = reader.apply(documentMapper)
                .fromDocument(document, TestNotStoredHolder.class, StorageOperation.READ);

        assertEquals(balanceReadsBack ? holder.getBalance() : null, read.getBalance());
        assertEquals(NAME, read.getName());
        assertNull(read.getAdditionalProperties());
    }

    static Stream<Arguments> eventMapperReaders() {
        return Stream.of(
                Arguments.of(Named.of("read by event", (Function<PostgresDocumentMapper, DocumentMapper>)
                        PostgresDocumentMapper::event), true),
                Arguments.of(Named.of("read by storage", (Function<PostgresDocumentMapper, DocumentMapper>)
                        PostgresDocumentMapper::storage), false));
    }

    @Test
    void applicationMapperKeepsItsEntityFormat() {
        JsonNode serialized = applicationMapper.valueToTree(withExtensions(new TestCatalog(CATALOG_ID)));

        assertTrue(serialized.get(Entity.ADDITIONAL_PROPERTIES_KEY).isArray());
        assertFalse(serialized.has(EXTENSION_KEY));
    }

    @ParameterizedTest
    @MethodSource("nulCharacters")
    void rejectsTheNulCharacter(TestCatalog catalog, String expectedPath, DocumentMode mode,
                                StorageOperation operation) {
        StorageException failure = assertThrows(StorageException.class,
                () -> documentMapper.storage().toDocument(catalog, mode, operation));

        assertEquals(operation, failure.operation());
        assertEquals(StorageErrorReason.INVALID_DATA, failure.reason());
        assertEquals(String.format(NUL_MESSAGE_FORMAT, expectedPath), failure.getMessage());
    }

    static Stream<Arguments> nulCharacters() {
        List<Arguments> cases = new ArrayList<>();
        Map<DocumentMode, StorageOperation> modes = Map.of(
                DocumentMode.FULL, StorageOperation.CREATE,
                DocumentMode.CARRIED_ATTRIBUTES, StorageOperation.UPDATE);
        modes.forEach((mode, operation) -> {
            cases.add(Arguments.of(Named.of("top-level", catalog(c -> c.setName("a" + NUL + "b"))),
                    NAME_PROPERTY, mode, operation));
            cases.add(Arguments.of(Named.of("nested", catalog(c -> c.setPrice(money("EUR" + NUL)))),
                    "price.unit", mode, operation));
            cases.add(Arguments.of(Named.of("in a list", catalog(c -> c.setKeywords(List.of("ok", NUL)))),
                    "keywords[1]", mode, operation));
            cases.add(Arguments.of(Named.of("in a key",
                            catalog(c -> c.addAdditionalProperties("bad" + NUL + "key", "value"))),
                    "bad\\u0000key", mode, operation));
            cases.add(Arguments.of(Named.of("in an extension value",
                            catalog(c -> c.addAdditionalProperties(EXTENSION_KEY, Map.of("inner", List.of(NUL))))),
                    EXTENSION_KEY + ".inner[0]", mode, operation));
        });
        return cases.stream();
    }

    @Test
    void readingAForeignDocumentFailsWithUnknown() {
        StorageException failure = assertThrows(StorageException.class,
                () -> documentMapper.storage().fromDocument("[]", TestCatalog.class, StorageOperation.READ));

        assertEquals(StorageOperation.READ, failure.operation());
        assertEquals(StorageErrorReason.UNKNOWN, failure.reason());
        assertNotNull(failure.getCause());
    }

    @Test
    void declaredNamesFailBeforeTheClassWasSerialized() {
        assertThrows(IllegalStateException.class,
                () -> new PostgresMappingModule(true).declaredNames(TestCatalog.class));
    }

    private static JsonNode document(DocumentMapper mapper, Entity entity, DocumentMode mode) {
        return new ObjectMapper().readTree(mapper.toDocument(entity, mode, StorageOperation.CREATE));
    }

    @SuppressWarnings("unchecked")
    private static <E extends Entity> E readBack(DocumentMapper mapper, E entity, DocumentMode mode) {
        String document = mapper.toDocument(entity, mode, StorageOperation.CREATE);
        return (E) mapper.fromDocument(document, entity.getClass(), StorageOperation.READ);
    }

    private static Entity sortExtensions(Entity entity) {
        if (entity.getAdditionalProperties() != null) {
            entity.setAdditionalProperties(entity.getAdditionalProperties().stream()
                    .sorted(Comparator.comparing(UnmappedProperty::getName))
                    .collect(Collectors.toList()));
        }
        return entity;
    }

    private static TestCatalog catalog(Consumer<TestCatalog> customizer) {
        TestCatalog catalog = new TestCatalog(CATALOG_ID);
        customizer.accept(catalog);
        return catalog;
    }

    private static TestCatalog catalogWithNullExtension() {
        TestCatalog catalog = new TestCatalog(CATALOG_ID);
        catalog.setName(NAME);
        catalog.addAdditionalProperties(EXTENSION_KEY, null);
        return catalog;
    }

    private static TestCatalog fullCatalog() {
        TestCatalog catalog = catalogWithParent();
        catalog.setName(NAME);
        catalog.setDescription("a description");
        catalog.setLastUpdate(Instant.parse("2026-11-09T10:15:30Z"));
        catalog.setKeywords(List.of("first", "second"));
        TimePeriod validFor = new TimePeriod();
        validFor.setStartDateTime(Instant.parse("2026-01-01T00:00:00Z"));
        catalog.setValidFor(validFor);
        catalog.setPrice(money("EUR"));
        Characteristic characteristic = new Characteristic();
        characteristic.setName("speed");
        characteristic.setTmfValue("fast");
        catalog.setCharacteristic(List.of(characteristic));
        RelatedParty party = new RelatedParty(PARTY_ID);
        party.setRole("owner");
        party.setName("party");
        party.setAtReferredType("individual");
        catalog.setRelatedParty(List.of(party));
        catalog.setAtSchemaLocation(URI.create("https://schemas.example.org/catalog.json"));
        return catalog;
    }

    private static TestCatalog catalogWithParent() {
        TestCatalog catalog = new TestCatalog(CATALOG_ID);
        TestUriRef parent = new TestUriRef(PARENT_ID);
        parent.setHref(PARENT_ID);
        catalog.setParent(parent);
        return catalog;
    }

    private static <E extends Entity> E withExtensions(E entity) {
        entity.addAdditionalProperties(EXTENSION_KEY, "custom");
        entity.addAdditionalProperties("count", 42);
        entity.addAdditionalProperties("nested", Map.of("inner", "value"));
        entity.addAdditionalProperties("list", List.of("a", "b"));
        return entity;
    }

    private static TestCatalog withExtensionListKey() {
        TestCatalog catalog = withExtensions(new TestCatalog(CATALOG_ID));
        catalog.addAdditionalProperties(Entity.ADDITIONAL_PROPERTIES_KEY, EXTENSION_NAME);
        return catalog;
    }

    private static TestSetterOnlyHolder withSetterOnlyExtension() {
        TestSetterOnlyHolder holder = new TestSetterOnlyHolder(
                "urn:ngsi-ld:" + TestSetterOnlyHolder.TYPE + ":holder-1");
        holder.addAdditionalProperties(TestSetterOnlyHolder.SETTER_ONLY_PROPERTY, EXTENSION_NAME);
        return holder;
    }

    private static TMForumSubscription subscription() {
        TMForumSubscription subscription = new TMForumSubscription(SUBSCRIPTION_ID);
        subscription.setCallback(URI.create("https://hub.example.org/listener"));
        subscription.setQuery(QUERY);
        subscription.setRawQuery(QUERY);
        subscription.setEventTypes(List.of("CatalogCreateEvent"));
        subscription.setEntities(List.of(TestCatalog.TYPE));
        subscription.setFields(List.of(NAME_PROPERTY));
        return subscription;
    }

    private static TestLogicalResource logicalResource() {
        TestLogicalResource resource = new TestLogicalResource(RESOURCE_ID);
        resource.setName(NAME);
        resource.setValue("logical");
        resource.setAtType("LogicalResource");
        resource.setAtBaseType("Resource");
        return resource;
    }

    private static TestNotStoredHolder notStoredHolder() {
        TestNotStoredHolder holder = new TestNotStoredHolder(HOLDER_ID);
        holder.setName(NAME);
        holder.setBalance(List.of(money("EUR")));
        return holder;
    }

    private static Money money(String unit) {
        Money money = new Money();
        money.setUnit(unit);
        money.setTmfValue(1.5f);
        return money;
    }
}

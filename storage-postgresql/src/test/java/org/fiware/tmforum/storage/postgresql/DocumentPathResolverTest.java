package org.fiware.tmforum.storage.postgresql;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.fiware.tmforum.storage.postgresql.QueryTestTypes.PathChild;
import org.fiware.tmforum.storage.postgresql.QueryTestTypes.PathRoot;
import org.fiware.tmforum.storage.query.AttributePath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Declared query paths name bean properties; the jsonpath needs the keys the storage document mapper writes.
 */
@MicronautTest(environments = PostgresStorage.ENVIRONMENT, startApplication = false)
@Property(name = "flyway.datasources.default.enabled", value = "false")
class DocumentPathResolverTest {

    @Inject
    private PostgresDocumentMapper documentMapper;

    @ParameterizedTest(name = "{0} on {1}")
    @MethodSource("paths")
    void resolvesToTheDocumentKeys(List<String> segments, Class<?> queryClass, boolean declared,
                                   List<String> expectedKeys) {
        DocumentPathResolver resolver = new DocumentPathResolver(documentMapper.storage());

        assertEquals(expectedKeys, resolver.resolve(queryClass, new AttributePath(segments, declared)));
    }

    static Stream<Arguments> paths() {
        return Stream.of(
                Arguments.of(List.of("name"), PathRoot.class, true, List.of("name")),
                Arguments.of(List.of("otherNamedSub", "name"), PathRoot.class, true,
                        List.of("otherNamedSub", "name")),
                Arguments.of(List.of("value"), PathChild.class, true, List.of("tmfValue")),
                Arguments.of(List.of("otherNamedSub", "value"), PathRoot.class, true,
                        List.of("otherNamedSub", "tmfValue")),
                Arguments.of(List.of("children", "value"), PathRoot.class, true, List.of("children", "tmfValue")),
                Arguments.of(List.of("childArray", "value"), PathRoot.class, true,
                        List.of("childArray", "tmfValue")),
                Arguments.of(List.of("jsonRenamed"), PathRoot.class, true, List.of("renamedKey")),
                Arguments.of(List.of("unknown", "value"), PathRoot.class, true, List.of("unknown", "value")),
                Arguments.of(List.of("children", "name", "value"), PathRoot.class, true,
                        List.of("children", "name", "value")),
                Arguments.of(List.of("tags", "value"), PathRoot.class, true, List.of("tags", "value")),
                Arguments.of(List.of("value"), PathChild.class, false, List.of("value")));
    }

    @Test
    void cachesPerClassAndPath() {
        DocumentPathResolver resolver = new DocumentPathResolver(documentMapper.storage());
        AttributePath path = new AttributePath(List.of("children", "value"), true);

        assertSame(resolver.resolve(PathRoot.class, path), resolver.resolve(PathRoot.class, path));
    }
}

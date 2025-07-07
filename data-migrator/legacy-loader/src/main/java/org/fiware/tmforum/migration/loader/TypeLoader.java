package org.fiware.tmforum.migration.loader;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.party.domain.organization.Organization;

import javax.inject.Singleton;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Loads all entity types defined on the class path
 */
@Slf4j
@Singleton
public class TypeLoader {

	/**
	 * Loads the entity-types by scanning the classpath for mapping annotations and building their class-entitytype tuple
	 */
	public List<EntityType> findAllEntityTypes() {
		List<EntityType> types = new ArrayList<>();
		try (ScanResult scanResult = new ClassGraph()
				.enableClassInfo()
				.enableAnnotationInfo()
				.scan()) {

			for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(MappingEnabled.class.getName())) {
				Class<?> clazz = classInfo.loadClass();
				System.out.println("The clazz is: " + clazz.getName());
				Arrays.stream(clazz.getAnnotations())
						.map(a -> {
							try {
								Method entityTypeMethod = a.annotationType().getMethod("entityType");
								String[] value = (String[]) entityTypeMethod.invoke(a);
								if (value.length != 1 || value[0].equals("")) {
									log.info("We ignore multi- type objects and only care about their concrete implementations.");
									return null;
								}
								return value[0];
							} catch (Exception e) {
								return null;
							}
						})
						.filter(Objects::nonNull)
						.forEach(v -> {
							types.add(new EntityType(clazz, v));
						});
			}
		}
		return types;
	}
}

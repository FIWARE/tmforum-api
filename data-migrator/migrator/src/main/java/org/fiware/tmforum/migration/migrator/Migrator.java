package org.fiware.tmforum.migration.migrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Data Migration tool. Handles the classloaders and transfers the objects between them.
 */
@Slf4j
@RequiredArgsConstructor
public class Migrator {

	private final String readBroker;
	private final String writeBroker;
	private final String legacyLoaderJar;
	private final String updateWriterJar;

	private static final Set<String> ERROR_TRACKER = new HashSet<>();

	/**
	 * List of classe to be shared between the child-classloaders. Will be provided from the old version of the lib through the parent classloader.
	 */
	private static final List<SharedClass> SHARED_CLASSES = List.of(
			new SharedClass("io.github.wistefan.mapping.annotations.MappingEnabled", "io/github/wistefan/mapping/annotations/MappingEnabled.class"),
			new SharedClass("io.github.wistefan.mapping.annotations.AttributeGetter", "io/github/wistefan/mapping/annotations/AttributeGetter.class"),
			new SharedClass("io.github.wistefan.mapping.annotations.AttributeSetter", "io/github/wistefan/mapping/annotations/AttributeSetter.class"),
			new SharedClass("io.github.wistefan.mapping.annotations.AttributeType", "io/github/wistefan/mapping/annotations/AttributeType.class"),
			new SharedClass("io.github.wistefan.mapping.annotations.DatasetId", "io/github/wistefan/mapping/annotations/DatasetId.class"),
			new SharedClass("io.github.wistefan.mapping.annotations.EntityId", "io/github/wistefan/mapping/annotations/EntityId.class"),
			new SharedClass("io.github.wistefan.mapping.annotations.EntityType", "io/github/wistefan/mapping/annotations/EntityType.class"),
			new SharedClass("io.github.wistefan.mapping.annotations.Ignore", "io/github/wistefan/mapping/annotations/Ignore.class"),
			new SharedClass("io.github.wistefan.mapping.annotations.RelationshipObject", "io/github/wistefan/mapping/annotations/RelationshipObject.class")
	);

	public void runUpdate() throws MalformedURLException {
		// both libraries have to be loaded into their dedicated classloaders.
		URL legacyLoader = new File(legacyLoaderJar).toURI().toURL();
		URL updateWriter = new File(updateWriterJar).toURI().toURL();

		// explicit access to parent classloader
		ClassLoader parentClassLoader = Migrator.class.getClassLoader();

		try (URLClassLoader legacyCL = new URLClassLoader(new URL[]{legacyLoader}, parentClassLoader);
			 URLClassLoader writerCL = new URLClassLoader(new URL[]{updateWriter}, parentClassLoader)) {
			// load versions from the legacyClassloader to the parent, to have them accessible in both childs
			loadAnnotationsInShared(parentClassLoader, legacyCL);

			// load the typeloader from the legacy module and get all entity types
			Class<?> typeLoaderClass = Class.forName("org.fiware.tmforum.migration.loader.TypeLoader", true, legacyCL);
			Object typeLoaderObject = typeLoaderClass.getConstructor().newInstance();
			List<?> records = (List<?>) typeLoaderClass.getMethod("findAllEntityTypes").invoke(typeLoaderObject);
			log.info("Records: {}", records);

			// load the legacyloader and its getter method
			Class<?> legacyLoaderClass = Class.forName("org.fiware.tmforum.migration.loader.LegacyLoader", true, legacyCL);
			Object legacyLoaderObject = legacyLoaderClass.getConstructor().newInstance();
			Class<?> entityTypeClass = Class.forName("org.fiware.tmforum.migration.loader.EntityType", true, legacyCL);
			Method entityGetter = legacyLoaderClass.getMethod("getAll", entityTypeClass);
			Method legacyBrokerSetter = legacyLoaderClass.getMethod("setBrokerAddress", String.class);
			legacyBrokerSetter.invoke(legacyLoaderObject, readBroker);

			// load the update writer from the new lib and its writer method
			Class<?> updateWriterClass = Class.forName("org.fiware.tmforum.migration.writer.UpdateWriter", true, writerCL);
			Object updateWriterObject = updateWriterClass.getConstructor().newInstance();
			Method entitySetter = updateWriterClass.getMethod("writeUpdate", Object.class);
			Method updateBrokerSetter = updateWriterClass.getMethod("setBrokerAddress", String.class);
			updateBrokerSetter.invoke(updateWriterObject, writeBroker);

			// call update for all found entities
			records.stream()
					.forEach(type -> updateType(parentClassLoader, legacyCL, writerCL, type, legacyLoaderObject, entityGetter, updateWriterObject, entitySetter));

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		log.warn("Issues occurred for the following entities: {}", ERROR_TRACKER);
	}

	private void loadAnnotationsInShared(ClassLoader parentClassLoader, ClassLoader legacyClassLoader) throws Exception {

		SHARED_CLASSES.forEach(sharedClass -> {
			try (InputStream in = legacyClassLoader.getResourceAsStream(sharedClass.resourceName)) {
				byte[] annotationBytes = in.readAllBytes();
				Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
				defineClass.setAccessible(true);

				defineClass.invoke(
						parentClassLoader,
						sharedClass.className,
						annotationBytes,
						0,
						annotationBytes.length
				);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	private void updateType(ClassLoader parentClassLoader, ClassLoader legacyLoader, ClassLoader newLoader, Object entityType, Object loaderObject, Method entityGetter, Object writerObject, Method entitySetter) {
		log.info("Update type: {}", entityType);
		List<?> theEntities = null;
		// load the entities of the given type, using the legacy ClassLoader
		try {
			Thread.currentThread().setContextClassLoader(legacyLoader);
			// invoke method on object created by childClassLoader here
			theEntities = (List<?>) entityGetter.invoke(loaderObject, entityType);
			log.info("Entities: {}", theEntities);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			Thread.currentThread().setContextClassLoader(parentClassLoader);
		}
		// write the entities using the new classLoader
		try {
			Thread.currentThread().setContextClassLoader(newLoader);
			for (Object e : theEntities) {
				try {
					entitySetter.invoke(writerObject, e);
				} catch (Exception ex) {
					ERROR_TRACKER.add(e.getClass().getCanonicalName());
					log.info("Was not able to transfer entity: {}", e);
					log.debug("Reason:", ex);
				}
			}
		} finally {
			Thread.currentThread().setContextClassLoader(parentClassLoader);
		}

	}

	private record SharedClass(String className, String resourceName) {
	}
}

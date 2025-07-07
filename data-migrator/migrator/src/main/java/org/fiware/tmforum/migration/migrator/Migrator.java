package org.fiware.tmforum.migration.migrator;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

@Slf4j
public class Migrator {

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
		URL legacyLoader = new File("data-migrator/legacy-loader/target/legacy-loader-0.1.jar").toURI().toURL();
		URL updateWriter = new File("data-migrator/update-writer/target/update-writer-0.1.jar").toURI().toURL();
		ClassLoader parentClassLoader = Migrator.class.getClassLoader();
		try (URLClassLoader legacyCL = new URLClassLoader(new URL[]{legacyLoader}, parentClassLoader);
			 URLClassLoader writerCL = new URLClassLoader(new URL[]{updateWriter}, parentClassLoader)) {
			loadAnnotationsInShared(parentClassLoader, legacyCL);

			Class<?> typeLoaderClass = Class.forName("org.fiware.tmforum.migration.loader.TypeLoader", true, legacyCL);
			Object typeLoaderObject = typeLoaderClass.getConstructor().newInstance();
			List<?> records = (List<?>) typeLoaderClass.getMethod("findAllEntityTypes").invoke(typeLoaderObject);
			log.info("Records: {}", records);
			Class<?> legacyLoaderClass = Class.forName("org.fiware.tmforum.migration.loader.LegacyLoader", true, legacyCL);
			Object legacyLoaderObject = legacyLoaderClass.getConstructor().newInstance();
			Class<?> entityTypeClass = Class.forName("org.fiware.tmforum.migration.loader.EntityType", true, legacyCL);
			Method entityGetter = legacyLoaderClass.getMethod("getAll", entityTypeClass);
			System.out.println("Current TCCL before invocation: " + Thread.currentThread().getContextClassLoader());

			Class<?> updateWriterClass = Class.forName("org.fiware.tmforum.migration.writer.UpdateWriter", true, writerCL);
			Object updateWriterObject = updateWriterClass.getConstructor().newInstance();
			Method entitySetter = updateWriterClass.getMethod("writeUpdate", Object.class);

			records.stream()
					.forEach(type -> updateType(parentClassLoader, legacyCL, writerCL, type, legacyLoaderObject, entityGetter, updateWriterObject, entitySetter));

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void loadAnnotationsInShared(ClassLoader parentClassLoader, ClassLoader legacyClassLoader) throws Exception {

		SHARED_CLASSES.forEach(sharedClass -> {
			try (InputStream in = legacyClassLoader.getResourceAsStream(sharedClass.resourceName)) {
				byte[] annotationBytes = in.readAllBytes();
				Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
				defineClass.setAccessible(true);

				Class<?> annotationInParent = (Class<?>) defineClass.invoke(
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
		try {
			Thread.currentThread().setContextClassLoader(newLoader);
			for (Object e : theEntities) {
				try {
					entitySetter.invoke(writerObject, e);
				} catch (Exception ex) {
					log.warn("Was not able to transfer entity: {} - Reason: {}", e, ex);
				}
			}
		} finally {
			Thread.currentThread().setContextClassLoader(parentClassLoader);
		}

	}

	private record SharedClass(String className, String resourceName) {
	}
}

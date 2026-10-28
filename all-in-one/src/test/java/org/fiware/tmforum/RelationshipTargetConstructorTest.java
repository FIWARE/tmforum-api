package org.fiware.tmforum;

import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the non-strict read path: the mapper builds an unresolvable relationship target through its String
 * constructor, so every target that is fetched on read (not {@code fromProperties}) must declare one.
 */
class RelationshipTargetConstructorTest {

	private static final String BASE_PACKAGE_PATH = "org/fiware/tmforum";
	private static final String CLASS_SUFFIX = ".class";
	private static final String FILE_PROTOCOL = "file";
	private static final String JAR_PROTOCOL = "jar";
	private static final Set<AttributeType> RELATIONSHIP_TYPES =
			EnumSet.of(AttributeType.RELATIONSHIP, AttributeType.RELATIONSHIP_LIST);

	@Test
	void fetchedRelationshipTargetsDeclareAStringConstructor() throws Exception {
		Set<String> classNames = findClassNames();
		assertFalse(classNames.isEmpty(), "No classes found under " + BASE_PACKAGE_PATH);

		Set<String> fetchedRelationships = new TreeSet<>();
		Set<String> offenders = new TreeSet<>();
		ClassLoader classLoader = getClass().getClassLoader();
		for (String className : classNames) {
			for (Method method : Class.forName(className, false, classLoader).getDeclaredMethods()) {
				AttributeSetter setter = method.getAnnotation(AttributeSetter.class);
				if (setter == null || setter.fromProperties() || !RELATIONSHIP_TYPES.contains(setter.value())) {
					continue;
				}
				String relationship = className + "#" + method.getName();
				fetchedRelationships.add(relationship);
				if (!hasStringConstructor(setter.targetClass())) {
					offenders.add(relationship + " -> " + setter.targetClass().getName());
				}
			}
		}

		assertFalse(fetchedRelationships.isEmpty(), "Expected to find relationships that are fetched on read.");
		assertTrue(offenders.isEmpty(),
				"Relationship targets without a String constructor cannot be read id-only:\n"
						+ String.join("\n", offenders));
	}

	private static boolean hasStringConstructor(Class<?> targetClass) {
		try {
			targetClass.getDeclaredConstructor(String.class);
			return true;
		} catch (NoSuchMethodException e) {
			return false;
		}
	}

	private Set<String> findClassNames() throws IOException, URISyntaxException {
		Set<String> classNames = new TreeSet<>();
		for (URL root : Collections.list(getClass().getClassLoader().getResources(BASE_PACKAGE_PATH))) {
			switch (root.getProtocol()) {
				case FILE_PROTOCOL -> addFromDirectory(Path.of(root.toURI()), classNames);
				case JAR_PROTOCOL -> addFromJar(root, classNames);
				default -> throw new IllegalStateException("Unsupported classpath entry: " + root);
			}
		}
		return classNames;
	}

	private static void addFromDirectory(Path packageRoot, Set<String> classNames) throws IOException {
		try (Stream<Path> files = Files.walk(packageRoot)) {
			files.map(file -> BASE_PACKAGE_PATH + "/" + packageRoot.relativize(file).toString().replace('\\', '/'))
					.filter(RelationshipTargetConstructorTest::isClassFile)
					.forEach(entry -> classNames.add(toClassName(entry)));
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}

	private static void addFromJar(URL root, Set<String> classNames) throws IOException {
		JarURLConnection connection = (JarURLConnection) root.openConnection();
		connection.setUseCaches(false);
		try (JarFile jar = connection.getJarFile()) {
			jar.stream()
					.map(entry -> entry.getName())
					.filter(name -> name.startsWith(BASE_PACKAGE_PATH + "/"))
					.filter(RelationshipTargetConstructorTest::isClassFile)
					.forEach(entry -> classNames.add(toClassName(entry)));
		}
	}

	private static boolean isClassFile(String entry) {
		return entry.endsWith(CLASS_SUFFIX) && !entry.endsWith("module-info" + CLASS_SUFFIX);
	}

	private static String toClassName(String entry) {
		return entry.substring(0, entry.length() - CLASS_SUFFIX.length()).replace('/', '.');
	}
}

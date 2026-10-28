package org.fiware.tmforum;

import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;

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
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Collects the relationships that are fetched from storage on read, i.e. every {@link AttributeSetter} of type
 * relationship without {@code fromProperties}, from every class on the test classpath. The all-in-one module
 * compiles every API module, so the scan sees all of them.
 */
final class FetchedRelationshipTargets {

	private static final String BASE_PACKAGE_PATH = "org/fiware/tmforum";
	private static final String CLASS_SUFFIX = ".class";
	private static final String FILE_PROTOCOL = "file";
	private static final String JAR_PROTOCOL = "jar";
	private static final Set<AttributeType> RELATIONSHIP_TYPES =
			EnumSet.of(AttributeType.RELATIONSHIP, AttributeType.RELATIONSHIP_LIST);

	private FetchedRelationshipTargets() {
	}

	/**
	 * The fetched relationships, keyed by {@code <declaring class>#<setter>} so that failures name them.
	 */
	static TreeMap<String, Class<?>> find() throws IOException, URISyntaxException, ClassNotFoundException {
		ClassLoader classLoader = FetchedRelationshipTargets.class.getClassLoader();
		Set<String> classNames = findClassNames(classLoader);
		if (classNames.isEmpty()) {
			throw new IllegalStateException("No classes found under " + BASE_PACKAGE_PATH);
		}
		TreeMap<String, Class<?>> relationships = new TreeMap<>();
		for (String className : classNames) {
			for (Method method : Class.forName(className, false, classLoader).getDeclaredMethods()) {
				AttributeSetter setter = method.getAnnotation(AttributeSetter.class);
				if (setter == null || setter.fromProperties() || !RELATIONSHIP_TYPES.contains(setter.value())) {
					continue;
				}
				relationships.put(className + "#" + method.getName(), setter.targetClass());
			}
		}
		if (relationships.isEmpty()) {
			throw new IllegalStateException("Expected to find relationships that are fetched on read.");
		}
		return relationships;
	}

	private static Set<String> findClassNames(ClassLoader classLoader) throws IOException, URISyntaxException {
		Set<String> classNames = new TreeSet<>();
		for (URL root : Collections.list(classLoader.getResources(BASE_PACKAGE_PATH))) {
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
					.filter(FetchedRelationshipTargets::isClassFile)
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
					.filter(FetchedRelationshipTargets::isClassFile)
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

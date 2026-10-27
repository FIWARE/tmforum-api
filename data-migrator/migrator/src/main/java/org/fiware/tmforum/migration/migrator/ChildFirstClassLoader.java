package org.fiware.tmforum.migration.migrator;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * Loads classes of the given packages from its own jars before asking the parent, all others parent-first.
 * Lets a child keep library versions that clash with the parent's, e.g. the legacy-loader's logback 1.2 and slf4j 1.7.
 */
public class ChildFirstClassLoader extends URLClassLoader {

	private final List<String> childFirstPackages;

	public ChildFirstClassLoader(URL[] urls, ClassLoader parent, List<String> childFirstPackages) {
		super(urls, parent);
		this.childFirstPackages = childFirstPackages;
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (childFirstPackages.stream().noneMatch(name::startsWith)) {
			return super.loadClass(name, resolve);
		}
		synchronized (getClassLoadingLock(name)) {
			Class<?> loadedClass = findLoadedClass(name);
			if (loadedClass == null) {
				try {
					loadedClass = findClass(name);
				} catch (ClassNotFoundException e) {
					return super.loadClass(name, resolve);
				}
			}
			if (resolve) {
				resolveClass(loadedClass);
			}
			return loadedClass;
		}
	}
}

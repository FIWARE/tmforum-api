package org.fiware.tmforum.migration.migrator;

import java.net.MalformedURLException;

public class Application {

	public static void main(String[] args) {
		Migrator migrator = new Migrator();
		try {
			migrator.runUpdate();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		System.exit(0);
	}

}

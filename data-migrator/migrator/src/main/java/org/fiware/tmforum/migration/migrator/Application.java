package org.fiware.tmforum.migration.migrator;

import io.micronaut.runtime.Micronaut;
import org.fiware.tmforum.party.Application;

public class Migrator {

	public static void main(String[] args) {
		Micronaut.run(Application.class, args);
	}

}

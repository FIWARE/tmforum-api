package org.fiware.tmforum.softwaremanagement;

import io.micronaut.context.annotation.Factory;
import io.micronaut.runtime.Micronaut;

/**
 * Base application as starting point for the Software Management API (TMF730).
 */
@Factory
public class Application {

	/**
	 * Start the Micronaut application.
	 *
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		Micronaut.run(Application.class, args);
	}
}

package org.fiware.tmforum.resourceordering;

import io.micronaut.context.annotation.Factory;
import io.micronaut.runtime.Micronaut;

/**
 * Base application as starting point for the Resource Order Management API.
 */
@Factory
public class Application {

	public static void main(String[] args) {
		Micronaut.run(Application.class, args);
	}
}

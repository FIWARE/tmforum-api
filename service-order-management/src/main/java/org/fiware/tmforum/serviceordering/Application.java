package org.fiware.tmforum.serviceordering;

import io.micronaut.context.annotation.Factory;
import org.fiware.tmforum.common.TmForumApplication;

/**
 * Base application as starting point for the Service Order Management API.
 */
@Factory
public class Application {

	/**
	 * Entry point for the Service Order Management API application.
	 *
	 * @param args command-line arguments
	 */
	public static void main(String[] args) {
		TmForumApplication.run(Application.class, args);
	}
}

package org.fiware.tmforum.softwaremanagement;

import io.micronaut.context.annotation.Factory;
import org.fiware.tmforum.common.TmForumApplication;

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
		TmForumApplication.run(Application.class, args);
	}
}

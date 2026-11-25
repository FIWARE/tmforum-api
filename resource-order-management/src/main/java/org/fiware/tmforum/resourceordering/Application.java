package org.fiware.tmforum.resourceordering;

import io.micronaut.context.annotation.Factory;
import org.fiware.tmforum.common.TmForumApplication;

/**
 * Base application as starting point for the Resource Order Management API.
 */
@Factory
public class Application {

	public static void main(String[] args) {
		TmForumApplication.run(Application.class, args);
	}
}

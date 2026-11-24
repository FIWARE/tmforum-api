package org.fiware.tmforum.usagemanagement;

import io.micronaut.context.annotation.Factory;
import org.fiware.tmforum.common.TmForumApplication;

/**
 * Base application as starting point
 */
@Factory
public class Application {

	public static void main(String[] args) {
		TmForumApplication.run(Application.class, args);
	}

}

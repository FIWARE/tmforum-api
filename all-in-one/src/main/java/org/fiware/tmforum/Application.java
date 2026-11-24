package org.fiware.tmforum;

import io.micronaut.context.annotation.Factory;
import org.fiware.tmforum.common.TmForumApplication;

/**
 * All-in-one application that combines all TMForum API modules into a single server.
 */
@Factory
public class Application {

    public static void main(String[] args) {
        TmForumApplication.run(Application.class, args);
    }
}

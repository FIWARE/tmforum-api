package org.fiware.tmforum.common.notification.checkers;

import org.fiware.ngsi.model.EntityVO;

public interface EventChecker {
    boolean wasFired(EntityVO entityVO);
    String getEventTypeSuffix();
}

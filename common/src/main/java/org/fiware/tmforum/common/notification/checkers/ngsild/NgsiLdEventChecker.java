package org.fiware.tmforum.common.notification.checkers.ngsild;

import org.fiware.ngsi.model.EntityVO;

public interface NgsiLdEventChecker {
    boolean wasFired(EntityVO entityVO);
    String getEventTypeSuffix();
}

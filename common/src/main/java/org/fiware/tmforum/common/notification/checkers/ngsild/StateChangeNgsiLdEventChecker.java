package org.fiware.tmforum.common.notification.checkers.ngsild;

import org.fiware.ngsi.model.EntityVO;

public record StateChangeNgsiLdEventChecker(String eventType) implements NgsiLdEventChecker {
    @Override
    public boolean wasFired(EntityVO entityVO) {
        return false;
    }
}

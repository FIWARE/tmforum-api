package org.fiware.tmforum.common.notification.checkers;

import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.notification.EventConstants;

public class DeleteEventChecker implements EventChecker {

    @Override
    public boolean wasFired(EntityVO entityVO) {
        return entityVO.getDeletedAt() != null;
    }

    @Override
    public String getEventTypeSuffix() {
        return EventConstants.DELETE_EVENT_SUFFIX;
    }
}

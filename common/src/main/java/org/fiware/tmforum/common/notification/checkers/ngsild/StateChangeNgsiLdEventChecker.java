package org.fiware.tmforum.common.notification.checkers.ngsild;

import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.notification.EventConstants;

public class StateChangeNgsiLdEventChecker implements NgsiLdEventChecker {
    @Override
    public boolean wasFired(EntityVO entityVO) {
        return false;
    }

    @Override
    public String getEventTypeSuffix() {
        return EventConstants.STATE_CHANGE_EVENT_SUFFIX;
    }
}

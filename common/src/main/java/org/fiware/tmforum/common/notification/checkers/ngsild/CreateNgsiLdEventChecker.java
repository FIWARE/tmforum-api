package org.fiware.tmforum.common.notification.checkers.ngsild;

import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.notification.EventConstants;

public class CreateNgsiLdEventChecker implements NgsiLdEventChecker {
    @Override
    public boolean wasFired(EntityVO entityVO) {
        return false;
    }

    @Override
    public String eventTypeSuffix() {
        return EventConstants.CREATE_EVENT_SUFFIX;
    }
}

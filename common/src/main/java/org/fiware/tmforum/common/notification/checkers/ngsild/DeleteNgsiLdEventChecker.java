package org.fiware.tmforum.common.notification.checkers.ngsild;

import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.notification.EventConstants;

public class DeleteNgsiLdEventChecker implements NgsiLdEventChecker {

    @Override
    public boolean wasFired(EntityVO entityVO) {
        return entityVO.getDeletedAt() != null;
    }

    @Override
    public String eventTypeSuffix() {
        return EventConstants.DELETE_EVENT_SUFFIX;
    }
}

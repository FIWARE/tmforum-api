package org.fiware.tmforum.common.notification.checkers.ngsild;

import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.notification.EventConstants;

public class AttributeValueChangeNgsiLdEventChecker implements NgsiLdEventChecker {
    @Override
    public boolean wasFired(EntityVO entityVO) {
        return false;
    }

    @Override
    public String getEventTypeSuffix() {
        return EventConstants.ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX;
    }
}

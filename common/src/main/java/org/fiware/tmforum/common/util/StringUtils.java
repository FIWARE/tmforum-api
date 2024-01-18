package org.fiware.tmforum.common.util;

import org.fiware.tmforum.common.notification.EventConstants;

public class StringUtils {
    public static String getEventGroupName(String eventType) {
        return eventType
                .replace(EventConstants.CREATE_EVENT_SUFFIX, "")
                .replace(EventConstants.DELETE_EVENT_SUFFIX, "")
                .replace(EventConstants.STATE_CHANGE_EVENT_SUFFIX, "")
                .replace(EventConstants.ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX, "")
                .replace(EventConstants.INFORMATION_REQUIRED_EVENT_SUFFIX, "")
                .replace(EventConstants.CHANGE_EVENT_SUFFIX, "");
    }
}

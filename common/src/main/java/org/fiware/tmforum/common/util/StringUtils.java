package org.fiware.tmforum.common.util;

import org.fiware.tmforum.common.EventConstants;

import java.util.Arrays;

public class StringUtils {
    public static String toCamelCase(String dashedLowerCase) {
        if (dashedLowerCase == null || dashedLowerCase.isEmpty()) {
            return "";
        }
        return String.join("", Arrays.stream(dashedLowerCase.split("-"))
                .map(StringUtils::capitalize).toList());
    }

    public static String decapitalize(String string) {
        if (string == null || string.isEmpty()) {
            return string;
        }

        char[] c = string.toCharArray();
        c[0] = Character.toLowerCase(c[0]);

        return new String(c);
    }

    public static String capitalize(String string) {
        if (string == null || string.isEmpty()) {
            return string;
        }

        char[] c = string.toCharArray();
        c[0] = Character.toUpperCase(c[0]);

        return new String(c);
    }

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

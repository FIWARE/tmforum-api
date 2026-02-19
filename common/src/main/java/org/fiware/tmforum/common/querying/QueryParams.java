package org.fiware.tmforum.common.querying;

import java.util.Map;

public record QueryParams(String id, String type, String query, Map<String, String> booleanFilters) {
}

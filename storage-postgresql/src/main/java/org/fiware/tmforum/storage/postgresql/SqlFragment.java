package org.fiware.tmforum.storage.postgresql;

import java.util.List;
import java.util.Objects;

/**
 * Rendered SQL with the values of its numbered parameters, in parameter order.
 */
record SqlFragment(String sql, List<Object> bindings) {

    SqlFragment {
        Objects.requireNonNull(sql, "sql");
        bindings = List.copyOf(bindings);
    }
}

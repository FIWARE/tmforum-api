package org.fiware.tmforum.accesspolicy;

/**
 * One raw policy found on an entity. {@code location} is a diagnostic path such as
 * {@code productOfferingTerm[0].accessPolicy}; {@code value} is the JSON tree (maps, lists, scalars).
 */
public record CarriedPolicy(String location, Object value) {
}

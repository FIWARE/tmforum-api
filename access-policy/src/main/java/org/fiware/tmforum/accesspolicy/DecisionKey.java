package org.fiware.tmforum.accesspolicy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The cache key of one PDP decision: a hex SHA-256 over policy, scope, action and subject, plus the resource only
 * when {@link ResourceConstraintDetector} flags the policy. Hex keeps identities out of the cache's key space.
 */
public record DecisionKey(String value) {

	private static final String UID = "uid";
	private static final String SCOPE = "scope";
	private static final String ACTION = "action";
	private static final String IDENTITY = "identity";
	private static final String FINGERPRINT = "fingerprint";
	private static final String POLICY = "policy";
	private static final String RESOURCE = "resource";

	private static final String RESOURCE_TYPE = "type";
	private static final String RESOURCE_ID = "id";
	private static final String RESOURCE_CATALOG = "catalog";
	private static final String RESOURCE_CATEGORY = "category";
	private static final String RESOURCE_RELATED_PARTY = "relatedParty";
	private static final String PARTY_ROLE = "role";
	private static final String PARTY_DID = "did";

	public DecisionKey {
		Objects.requireNonNull(value, "value");
	}

	/** Builds the key for one PDP request; {@code policy} must be JSON-shaped (maps, lists, scalars). */
	public static DecisionKey of(String policyUid, Map<String, Object> policy, String scope, String action,
			ValidationRequest.Resource resource, Subject subject) {
		Objects.requireNonNull(subject, "subject");
		Map<String, Object> parts = new LinkedHashMap<>();
		putIfPresent(parts, UID, policyUid);
		putIfPresent(parts, SCOPE, scope);
		putIfPresent(parts, ACTION, action);
		putIfPresent(parts, IDENTITY, subject.identity());
		putIfPresent(parts, FINGERPRINT, subject.credentialFingerprint());
		putIfPresent(parts, POLICY, policy == null ? null : CanonicalJson.digest(policy));
		if (ResourceConstraintDetector.constrainsResource(policy)) {
			putIfPresent(parts, RESOURCE, resource == null ? null : CanonicalJson.digest(resourceMap(resource)));
		}
		return new DecisionKey(CanonicalJson.digest(parts));
	}

	/** The plain digest, which is also the form Redis stores the key under. */
	@Override
	public String toString() {
		return value;
	}

	// Built by hand, so a new Resource field only enters the key deliberately (guarded by DecisionKeyTest).
	static Map<String, Object> resourceMap(ValidationRequest.Resource resource) {
		Map<String, Object> map = new LinkedHashMap<>();
		putIfPresent(map, RESOURCE_TYPE, resource.type());
		putIfPresent(map, RESOURCE_ID, resource.id());
		putIfPresent(map, RESOURCE_CATALOG, resource.catalog());
		putIfPresent(map, RESOURCE_CATEGORY, resource.category());
		if (resource.relatedParty() != null) {
			List<Map<String, Object>> parties = resource.relatedParty().stream()
					.map(DecisionKey::relatedPartyMap)
					.toList();
			map.put(RESOURCE_RELATED_PARTY, parties);
		}
		return map;
	}

	static Map<String, Object> relatedPartyMap(ValidationRequest.RelatedParty party) {
		Map<String, Object> map = new LinkedHashMap<>();
		if (party != null) {
			putIfPresent(map, PARTY_ROLE, party.role());
			putIfPresent(map, PARTY_DID, party.did());
		}
		return map;
	}

	private static void putIfPresent(Map<String, Object> map, String name, Object value) {
		if (value != null) {
			map.put(name, value);
		}
	}
}

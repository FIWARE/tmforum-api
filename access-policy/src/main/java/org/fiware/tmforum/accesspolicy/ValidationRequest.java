package org.fiware.tmforum.accesspolicy;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The body sent to the PDP, mirroring POLICY_EXAMPLES.md § What the PDP receives. Every record pins
 * {@code NON_NULL}, so the wire shape does not depend on the deploying app's serialization inclusion.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidationRequest(Map<String, Object> policy, JsonInput jsonInput) {

	public static final String PROTOCOL = "tmf";
	public static final String VERIFIABLE_CREDENTIAL_CLAIM = "verifiableCredential";
	public static final String VC_CLAIMS_KEY = "vc";

	/** Builds the request for {@code subject}; {@code policy} is passed through untouched. */
	public static ValidationRequest of(Map<String, Object> policy, String scope, String action, Resource resource,
			Subject subject) {
		Objects.requireNonNull(subject, "subject");
		RequestSubject requestSubject = new RequestSubject(subject.identity(),
				Map.of(VC_CLAIMS_KEY, presentedCredentials(subject)));
		return new ValidationRequest(policy,
				new JsonInput(new Payload(scope, PROTOCOL, action, resource), requestSubject));
	}

	// The credentials are handed over as presented, null elements included; interpreting them is the PDP's job (AP-1).
	private static List<Object> presentedCredentials(Subject subject) {
		Object claim = subject.claims().get(VERIFIABLE_CREDENTIAL_CLAIM);
		if (claim == null) {
			return List.of();
		}
		if (claim instanceof List<?> credentials) {
			return Collections.unmodifiableList(credentials);
		}
		return List.of(claim);
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record JsonInput(Payload payload, RequestSubject subject) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Payload(String scope, String protocol, String action, Resource resource) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Resource(String type, String id, String catalog, List<String> category,
			List<RelatedParty> relatedParty) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record RelatedParty(String role, String did) {
	}

	/** {@code identity} is absent for an anonymous caller. */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record RequestSubject(String identity, Map<String, Object> claims) {
	}
}

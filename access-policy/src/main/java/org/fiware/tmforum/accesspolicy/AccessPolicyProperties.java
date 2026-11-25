package org.fiware.tmforum.accesspolicy;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

import java.time.Duration;
import java.util.List;

/**
 * Configuration of the policy-based access control, see doc/access-policies/ARCHITECTURE.md § Configuration.
 */
@ConfigurationProperties(AccessPolicyProperties.PREFIX)
@Data
public class AccessPolicyProperties {

	public static final String PREFIX = "access-policy";

	public static final int DEFAULT_MAX_SCAN = 10000;
	public static final int DEFAULT_OVERFETCH_FACTOR = 4;
	public static final Duration DEFAULT_DECISION_TTL = Duration.ofSeconds(60);
	public static final String DEFAULT_PDP_PATH = "/validate";
	public static final Duration DEFAULT_PDP_TIMEOUT = Duration.ofSeconds(2);
	public static final String DEFAULT_IDENTITY_CLAIM = "verifiableCredential.issuer";

	/**
	 * Switches the whole feature. Off by default; while off, none of its beans exist.
	 */
	private boolean enabled = false;

	/**
	 * {@code observe} decides and audits but filters nothing, to measure a policy's impact before it bites.
	 */
	private Mode mode = Mode.ENFORCE;

	/**
	 * Decision for an entity no policy governs. {@code allow} keeps today's behaviour without any PDP call.
	 */
	private Decision defaultDecision = Decision.ALLOW;

	/**
	 * Decision for a caller without token. Moot while {@link #bypassUnauthenticated} is on.
	 */
	private Decision anonymousDecision = Decision.ALLOW;

	/**
	 * Lets callers without token pass unfiltered, so infrastructure callers keep working unchanged.
	 */
	private boolean bypassUnauthenticated = true;

	/**
	 * Roles whose holders are never filtered.
	 */
	private List<String> bypassRoles = List.of();

	/**
	 * Subjects that are never filtered.
	 */
	private List<String> bypassSubjects = List.of();

	/**
	 * Lets the owner of an entity always see it.
	 */
	private boolean ownerExempt = true;

	/**
	 * Derives a catalog's visibility from its offerings. Off by default, the upward rule costs an extra query.
	 */
	private boolean deriveCatalogVisibility = false;

	/**
	 * How {@code X-Total-Count} is computed. {@code exact} degrades to {@code omit} past {@link #maxScan}.
	 */
	private TotalCount totalCount = TotalCount.EXACT;

	/**
	 * Maximum number of entities read to compute an exact total.
	 */
	private int maxScan = DEFAULT_MAX_SCAN;

	/**
	 * Multiple of the page size read per storage call when refilling a filtered page.
	 */
	private int overfetchFactor = DEFAULT_OVERFETCH_FACTOR;

	/**
	 * How long a PDP decision is cached. A unit is mandatory.
	 */
	private Duration decisionTtl = DEFAULT_DECISION_TTL;

	/**
	 * Whether an unreachable PDP allows. Off by default: it denies, a list answers an empty page.
	 */
	private boolean failOpen = false;

	private Pdp pdp = new Pdp();

	private Jwt jwt = new Jwt();

	private Audit audit = new Audit();

	/**
	 * Whether decisions are enforced or only observed.
	 */
	public enum Mode {
		ENFORCE, OBSERVE
	}

	/**
	 * Outcome of an access decision.
	 */
	public enum Decision {
		ALLOW, DENY
	}

	/**
	 * Strategy for the total count of a filtered collection.
	 */
	public enum TotalCount {
		EXACT, OMIT, UPSTREAM
	}

	/**
	 * Endpoint of the external policy decision point.
	 */
	@ConfigurationProperties("pdp")
	@Data
	public static class Pdp {

		/**
		 * Base URL of the PDP. No default.
		 */
		private String url;

		/**
		 * Path of the decision endpoint below {@link #url}.
		 */
		private String path = DEFAULT_PDP_PATH;

		/**
		 * Timeout of a single decision request.
		 */
		private Duration timeout = DEFAULT_PDP_TIMEOUT;
	}

	/**
	 * Validation of the caller's bearer token.
	 */
	@ConfigurationProperties("jwt")
	@Data
	public static class Jwt {

		/**
		 * Where the token's signing keys are published. No default.
		 */
		private String jwksUri;

		/**
		 * Expected {@code iss} of the token. No default.
		 */
		private String issuer;

		/**
		 * Expected {@code aud} of the token, unchecked when unset.
		 */
		private String audience;

		/**
		 * Claim path identifying the subject.
		 */
		private String identityClaim = DEFAULT_IDENTITY_CLAIM;
	}

	/**
	 * Audit log of decisions.
	 */
	@ConfigurationProperties("audit")
	@Data
	public static class Audit {

		/**
		 * Whether each decision is written to the audit log.
		 */
		private boolean enabled = false;
	}
}

package org.fiware.tmforum.accesspolicy;

import org.fiware.tmforum.accesspolicy.JsonLdKeys.Bindings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.fiware.tmforum.accesspolicy.JsonLdKeys.CONTEXT;
import static org.fiware.tmforum.accesspolicy.JsonLdKeys.DSPACE_NAMESPACE;
import static org.fiware.tmforum.accesspolicy.JsonLdKeys.ID;
import static org.fiware.tmforum.accesspolicy.JsonLdKeys.ODRL_CONTEXT;
import static org.fiware.tmforum.accesspolicy.JsonLdKeys.ODRL_NAMESPACE;
import static org.fiware.tmforum.accesspolicy.JsonLdKeys.TYPE;
import static org.fiware.tmforum.accesspolicy.JsonLdKeys.VALUE;
import static org.fiware.tmforum.accesspolicy.JsonLdKeys.collectContexts;
import static org.fiware.tmforum.accesspolicy.JsonLdKeys.expand;
import static org.fiware.tmforum.accesspolicy.JsonLdKeys.expandKey;
import static org.fiware.tmforum.accesspolicy.JsonLdKeys.isAbsolute;
import static org.fiware.tmforum.accesspolicy.JsonLdKeys.isKeyword;
import static org.fiware.tmforum.accesspolicy.JsonLdKeys.operandIri;

/**
 * Narrows carried policies to the rules that declare one {@code dspace:scope}, keyed by {@code odrl:uid}. Fail-safe:
 * a policy it cannot positively interpret comes back as an {@link UnreadablePolicy}, never as "no rule at this scope".
 */
final class ScopedPolicyResolution {

	static final String NO_CARRIED_POLICIES = "the carrier returned no policy list";
	static final String CARRIER_FAILED = "the carrier failed: ";

	private static final String ODRL_UID = ODRL_NAMESPACE + "uid";
	private static final Set<String> RULE_IRIS = Set.of(
			ODRL_NAMESPACE + "permission",
			ODRL_NAMESPACE + "prohibition",
			ODRL_NAMESPACE + "obligation");
	// ODRL 2.2 § 2.7: the parent's rules, and their scopes, belong to this policy but are never seen here.
	private static final String ODRL_INHERIT_FROM = ODRL_NAMESPACE + "inheritFrom";
	private static final String DSPACE_SCOPE = DSPACE_NAMESPACE + "scope";
	private static final String SCOPE = "scope";
	// A DSP scope the EDC connector evaluates and this project deliberately ignores (POLICY_EXAMPLES.md § Scoping).
	private static final String TRANSFER_PROCESS_SCOPE = "transfer.process";
	// Any other value is a typo whose rule would silently drop and leave the entity public.
	private static final Set<String> KNOWN_SCOPES = Stream.concat(
					Stream.of(PolicyScope.values()).map(PolicyScope::value), Stream.of(TRANSFER_PROCESS_SCOPE))
			.collect(Collectors.toUnmodifiableSet());
	private static final String LEFT_OPERAND = "leftOperand";
	private static final Set<String> POLICY_KEYWORDS = Set.of(CONTEXT, ID, TYPE);
	private static final Set<String> RULE_KEYWORDS = Set.of(ID, TYPE);

	// The property terms of http://www.w3.org/ns/odrl.jsonld (fetched 2026-11-27), each bound to odrl:<term>. Its
	// class, action and operand terms are left out: they never stand as keys, so one there stays uninterpreted.
	private static final Set<String> ODRL_PROPERTY_TERMS = Set.of(
			"profile", "inheritFrom", "conflict", "relation", "hasPolicy", "target", "output", "partOf", "source",
			"function", "assignee", "assigner", "assigneeOf", "assignerOf", "attributedParty", "attributingParty",
			"compensatedParty", "compensatingParty", "consentingParty", "consentedParty", "informedParty",
			"informingParty", "trackingParty", "trackedParty", "contractingParty", "contractedParty", "action",
			"includedIn", "implies", "permission", "prohibition", "obligation", "duty", "consequence", "remedy",
			"constraint", "refinement", "operator", "rightOperand", "rightOperandReference", "leftOperand", "unit",
			"status", "and", "or", "xone", "andSequence");
	// The terms of the same context that do not expand to odrl:<term>.
	private static final Map<String, String> ODRL_CONTEXT_ALIASES = Map.of(
			"uid", ID,
			"type", TYPE,
			"dataType", ODRL_NAMESPACE + "datatype");
	private static final Map<String, String> ODRL_CONTEXT_TERMS = Stream.concat(
					ODRL_PROPERTY_TERMS.stream().map(term -> Map.entry(term, ODRL_NAMESPACE + term)),
					ODRL_CONTEXT_ALIASES.entrySet().stream())
			.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

	private static final String UNKNOWN_LOCATION = "<unknown>";
	private static final String LOCATION_SEPARATOR = ", ";
	private static final String NULL_CARRIED_POLICY = "the carrier returned a null policy";
	private static final String NOT_AN_OBJECT = "the policy is not a JSON object";
	private static final String NO_UID = "the policy has no odrl:uid";
	private static final String UNREADABLE_UID = "the policy's odrl:uid is not a single IRI";
	private static final String UNTRUSTED_CONTEXT = "the policy names an untrusted remote @context";
	private static final String VOCAB_CONTEXT = "the policy's @context sets @vocab";
	private static final String UNINTERPRETED_KEY = "uninterpreted key '%s'";
	private static final String KEYWORD_NOT_ALLOWED = "keyword '%s' is not allowed here";
	private static final String RULE_NOT_OBJECT = "a rule is neither an object nor a list of objects";
	private static final String KEYWORD_ONLY_RULE = "a rule holds nothing but keywords";
	private static final String WRONG_SCOPE_IRI = "a rule's scope key '%s' is not " + DSPACE_SCOPE;
	private static final String INHERITED_RULES = "the policy inherits rules through odrl:inheritFrom";
	private static final String SCOPE_GIVEN_TWICE = "a rule declares its scope twice";
	private static final String UNREADABLE_SCOPE = "a rule's scope is not a single string";
	private static final String UNKNOWN_SCOPE = "unknown scope value '%s'";
	private static final String MISPLACED_SCOPE = "scope key '%s' outside a top-level rule";
	private static final String UNREADABLE_LEFT_OPERAND = "a leftOperand is not a single IRI";
	private static final String SCOPE_LEFT_OPERAND = "scope used as a leftOperand";
	private static final String CONFLICTING_UID = "conflicting policies share odrl:uid";
	private static final String UNEXPECTED = "unexpected failure: ";

	private ScopedPolicyResolution() {
	}

	/** Validates every carried policy, collapses equal uids, fails conflicting ones, then narrows to {@code scope}. */
	static ResolvedPolicies resolve(List<CarriedPolicy> carried, PolicyScope scope) {
		List<Reading> readings = carried.stream().map(ScopedPolicyResolution::read).toList();
		Map<String, List<Reading>> byUid = new HashMap<>();
		readings.stream().filter(reading -> reading.uid() != null)
				.forEach(reading -> byUid.computeIfAbsent(reading.uid(), uid -> new ArrayList<>()).add(reading));

		Map<String, Map<String, Object>> policies = new LinkedHashMap<>();
		List<UnreadablePolicy> unreadable = new ArrayList<>();
		Set<String> settled = new HashSet<>();
		for (Reading reading : readings) {
			if (reading.uid() == null) {
				unreadable.add(reading.unreadable());
				continue;
			}
			if (!settled.add(reading.uid())) {
				continue;
			}
			List<Reading> sharing = byUid.get(reading.uid());
			if (sharing.stream().map(Reading::raw).distinct().count() > 1) {
				String locations = sharing.stream().map(Reading::location)
						.collect(Collectors.joining(LOCATION_SEPARATOR));
				unreadable.add(new UnreadablePolicy(locations, reading.uid(), CONFLICTING_UID));
			} else if (reading.reason() != null) {
				unreadable.add(reading.unreadable());
			} else {
				Map<String, Object> narrowed = narrow(reading, scope);
				if (narrowed != null) {
					policies.put(reading.uid(), narrowed);
				}
			}
		}
		return new ResolvedPolicies(policies, unreadable);
	}

	private static Reading read(CarriedPolicy carried) {
		if (carried == null) {
			return new Reading(UNKNOWN_LOCATION, null, null, null, NULL_CARRIED_POLICY);
		}
		String uid = null;
		try {
			if (!(carried.value() instanceof Map<?, ?> policy)) {
				throw new Unreadable(NOT_AN_OBJECT);
			}
			Bindings bindings = collectContexts(policy);
			uid = uid(policy, bindings);
			if (uid == null) {
				throw new Unreadable(NO_UID);
			}
			return new Reading(carried.location(), policy, uid, rules(policy, bindings), null);
		} catch (Unreadable e) {
			return new Reading(carried.location(), carried.value(), uid, null, e.getMessage());
		} catch (RuntimeException e) {
			return new Reading(carried.location(), carried.value(), uid, null, UNEXPECTED + e);
		}
	}

	// odrl:uid first, else @id (which odrl.jsonld's bare uid aliases); several keys must agree.
	private static String uid(Map<?, ?> policy, Bindings bindings) {
		List<String> uids = new ArrayList<>();
		List<String> ids = new ArrayList<>();
		policy.forEach((key, value) -> {
			String iri = interpret(String.valueOf(key), bindings);
			if (ODRL_UID.equals(iri)) {
				uids.add(operandIri(value));
			} else if (ID.equals(iri)) {
				ids.add(operandIri(value));
			}
		});
		List<String> chosen = uids.isEmpty() ? ids : uids;
		if (chosen.isEmpty()) {
			return null;
		}
		if (chosen.contains(null) || new HashSet<>(chosen).size() > 1) {
			throw new Unreadable(UNREADABLE_UID);
		}
		return chosen.getFirst();
	}

	/** Checks the whole policy at every scope; the rule keys it returns are the policy's own key strings. */
	private static Map<String, RuleKey> rules(Map<?, ?> policy, Bindings bindings) {
		// Such a context could alias any key, even a bare one, to a rule or to dspace:scope.
		if (bindings.untrustedRemoteContext()) {
			throw new Unreadable(UNTRUSTED_CONTEXT);
		}
		if (bindings.vocab()) {
			throw new Unreadable(VOCAB_CONTEXT);
		}
		Map<String, RuleKey> rules = new HashMap<>();
		for (Map.Entry<?, ?> entry : policy.entrySet()) {
			String key = String.valueOf(entry.getKey());
			if (CONTEXT.equals(key)) {
				continue;
			}
			String iri = interpretedKey(key, bindings);
			if (isKeyword(iri)) {
				requireAllowed(iri, POLICY_KEYWORDS);
			} else if (RULE_IRIS.contains(iri)) {
				rules.put(key, ruleKey(entry.getValue(), bindings));
			} else if (ODRL_INHERIT_FROM.equals(iri)) {
				throw new Unreadable(INHERITED_RULES);
			} else {
				checkNested(key, entry.getValue(), bindings);
			}
		}
		return rules;
	}

	private static RuleKey ruleKey(Object value, Bindings bindings) {
		boolean single = value instanceof Map<?, ?>;
		List<?> elements = single ? List.of(value) : value instanceof List<?> list ? list : null;
		if (elements == null) {
			throw new Unreadable(RULE_NOT_OBJECT);
		}
		List<ScopedRule> rules = new ArrayList<>();
		for (Object element : elements) {
			if (!(element instanceof Map<?, ?> rule)) {
				throw new Unreadable(RULE_NOT_OBJECT);
			}
			rules.add(new ScopedRule(rule, scope(rule, bindings)));
		}
		return new RuleKey(single, rules);
	}

	/** The rule's scope, null if it declares none. */
	private static String scope(Map<?, ?> rule, Bindings bindings) {
		String scope = null;
		boolean scoped = false;
		boolean holdsTerm = false;
		for (Map.Entry<?, ?> entry : rule.entrySet()) {
			String key = String.valueOf(entry.getKey());
			String iri = interpretedKey(key, bindings);
			if (isKeyword(iri)) {
				requireAllowed(iri, RULE_KEYWORDS);
				continue;
			}
			holdsTerm = true;
			if (!SCOPE.equals(localName(key, iri))) {
				checkNested(key, entry.getValue(), bindings);
				continue;
			}
			if (!DSPACE_SCOPE.equals(iri)) {
				throw new Unreadable(WRONG_SCOPE_IRI.formatted(key));
			}
			if (scoped) {
				throw new Unreadable(SCOPE_GIVEN_TWICE);
			}
			scoped = true;
			scope = scopeValue(entry.getValue());
			if (!KNOWN_SCOPES.contains(scope)) {
				throw new Unreadable(UNKNOWN_SCOPE.formatted(scope));
			}
		}
		// A bare node reference like {"@id": "urn:rule:1"} keeps its scope elsewhere.
		if (!holdsTerm) {
			throw new Unreadable(KEYWORD_ONLY_RULE);
		}
		return scope;
	}

	private static String scopeValue(Object value) {
		if (value instanceof List<?> list && list.size() == 1) {
			return scopeValue(list.getFirst());
		}
		if (value instanceof String scope) {
			return scope;
		}
		if (value instanceof Map<?, ?> literal && literal.get(VALUE) instanceof String scope) {
			return scope;
		}
		throw new Unreadable(UNREADABLE_SCOPE);
	}

	// Anything below a top-level rule: a scope there would be understood by its author and ignored here.
	private static void checkNested(String key, Object value, Bindings bindings) {
		if (CONTEXT.equals(key)) {
			return;
		}
		String name = localName(key, expandKey(key, bindings));
		if (SCOPE.equals(name)) {
			throw new Unreadable(MISPLACED_SCOPE.formatted(key));
		}
		if (LEFT_OPERAND.equals(name)) {
			String operand = operandIri(value);
			if (operand == null) {
				throw new Unreadable(UNREADABLE_LEFT_OPERAND);
			}
			if (SCOPE.equals(localName(operand, expand(operand, bindings)))) {
				throw new Unreadable(SCOPE_LEFT_OPERAND);
			}
		}
		checkNested(value, bindings);
	}

	private static void checkNested(Object node, Bindings bindings) {
		if (node instanceof List<?> list) {
			list.forEach(element -> checkNested(element, bindings));
		} else if (node instanceof Map<?, ?> map) {
			map.forEach((key, value) -> checkNested(String.valueOf(key), value, bindings));
		}
	}

	private static String interpretedKey(String key, Bindings bindings) {
		String iri = interpret(key, bindings);
		if (iri == null) {
			throw new Unreadable(UNINTERPRETED_KEY.formatted(key));
		}
		return iri;
	}

	/** The key's IRI or keyword; null unless an inline binding, a prefix or a trusted context's term defines it. */
	private static String interpret(String key, Bindings bindings) {
		String expanded = expandKey(key, bindings);
		if (expanded == null || isKeyword(expanded) || isAbsolute(expanded)) {
			return expanded;
		}
		return bindings.namesContext(ODRL_CONTEXT) ? ODRL_CONTEXT_TERMS.get(expanded) : null;
	}

	private static void requireAllowed(String keyword, Set<String> allowed) {
		if (!allowed.contains(keyword)) {
			throw new Unreadable(KEYWORD_NOT_ALLOWED.formatted(keyword));
		}
	}

	// A string that does not expand still has a local name, so a bare "scope" is caught too.
	private static String localName(String raw, String expanded) {
		return JsonLdKeys.localName(expanded != null ? expanded : raw);
	}

	private static Map<String, Object> narrow(Reading reading, PolicyScope scope) {
		Map<String, Object> narrowed = new LinkedHashMap<>();
		boolean ruled = false;
		for (Map.Entry<?, ?> entry : ((Map<?, ?>) reading.raw()).entrySet()) {
			String key = String.valueOf(entry.getKey());
			RuleKey ruleKey = reading.rules().get(key);
			if (ruleKey == null) {
				narrowed.put(key, entry.getValue());
				continue;
			}
			List<Object> kept = ruleKey.rules().stream()
					.filter(rule -> scope.value().equals(rule.scope()))
					.map(ScopedRule::rule)
					.toList();
			if (!kept.isEmpty()) {
				ruled = true;
				narrowed.put(key, ruleKey.single() ? kept.getFirst() : kept);
			}
		}
		return ruled ? Collections.unmodifiableMap(narrowed) : null;
	}

	/** One carried policy after validation; {@code reason} is set when it is unreadable. */
	private record Reading(String location, Object raw, String uid, Map<String, RuleKey> rules, String reason) {

		UnreadablePolicy unreadable() {
			return new UnreadablePolicy(location, uid, reason);
		}
	}

	/** The rules under one rule key; {@code single} when the key held one object rather than a list. */
	private record RuleKey(boolean single, List<ScopedRule> rules) {
	}

	private record ScopedRule(Object rule, String scope) {
	}

	private static final class Unreadable extends RuntimeException {

		Unreadable(String reason) {
			super(reason, null, false, false);
		}
	}
}

package org.fiware.tmforum.common.validation;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.configuration.ReferenceProperties;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.common.domain.ReferenceValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceClassifierTest {

	private static final String ID_PREFIX = "urn:ngsi-ld:category:partner-a-";
	private static final String HREF_PREFIX = "https://partner-a.example.org/";
	private static final String LOCAL_ID = "urn:ngsi-ld:category:1f6e2b0c-0000-4000-8000-000000000000";
	private static final String EXTERNAL_ID = ID_PREFIX + "42";
	private static final String EXTERNAL_HREF = HREF_PREFIX + "tmf-api/productCatalogManagement/v4/category/42";
	private static final String FOREIGN_HREF = "https://untrusted.example.org/category/42";
	private static final String OVERLAP_WARNING = "overlaps server-generated local ids";

	@ParameterizedTest(name = "{0}")
	@MethodSource("classifications")
	public void classify(String description, List<String> idPrefixes, List<String> hrefPrefixes,
			ReferencedEntity ref, ReferenceScope expected) {
		assertEquals(expected, classifier(idPrefixes, hrefPrefixes).classify(ref));
	}

	private static Stream<Arguments> classifications() {
		List<String> ids = List.of(ID_PREFIX);
		List<String> hrefs = List.of(HREF_PREFIX);
		return Stream.of(
				Arguments.of("id prefix match", ids, hrefs, refEntity(EXTERNAL_ID, null), ReferenceScope.EXTERNAL),
				Arguments.of("id prefix wins over href prefix", ids, hrefs, refEntity(EXTERNAL_ID, EXTERNAL_HREF),
						ReferenceScope.EXTERNAL),
				Arguments.of("href prefix match", ids, hrefs, refEntity(LOCAL_ID, EXTERNAL_HREF),
						ReferenceScope.EXTERNAL_BY_HREF),
				Arguments.of("no match", ids, hrefs, refEntity(LOCAL_ID, null), ReferenceScope.LOCAL),
				Arguments.of("null href", List.of(), hrefs, refEntity(LOCAL_ID, null), ReferenceScope.LOCAL),
				Arguments.of("empty config", List.of(), List.of(), refEntity(EXTERNAL_ID, EXTERNAL_HREF),
						ReferenceScope.LOCAL),
				Arguments.of("foreign host not in allow-list", ids, hrefs, refEntity(LOCAL_ID, FOREIGN_HREF),
						ReferenceScope.LOCAL),
				Arguments.of("reference value matched by href", ids, hrefs, referenceValue(LOCAL_ID, EXTERNAL_HREF),
						ReferenceScope.EXTERNAL_BY_HREF),
				Arguments.of("subclass shadowing href matched by href", ids, hrefs,
						shadowingRef(LOCAL_ID, EXTERNAL_HREF), ReferenceScope.EXTERNAL_BY_HREF),
				Arguments.of("blank entries are ignored", List.of(" ", ""), List.of("", HREF_PREFIX),
						refEntity(LOCAL_ID, FOREIGN_HREF), ReferenceScope.LOCAL),
				Arguments.of("prefixes are trimmed", List.of(" " + ID_PREFIX + " "), List.of(),
						refEntity(EXTERNAL_ID, null), ReferenceScope.EXTERNAL));
	}

	@Test
	public void isExternalOnlyConsidersIdPrefixes() {
		ReferenceClassifier classifier = classifier(List.of(ID_PREFIX), List.of(HREF_PREFIX));

		assertTrue(classifier.isExternal(URI.create(EXTERNAL_ID)));
		assertFalse(classifier.isExternal(URI.create(LOCAL_ID)));
		assertFalse(classifier.isExternal(null));
	}

	@ParameterizedTest(name = "{0} warns: {1}")
	@MethodSource("overlappingPrefixes")
	public void warnsAboutIdPrefixesOverlappingLocalIds(String prefix, boolean expectWarning) {
		Logger logger = (Logger) LoggerFactory.getLogger(ReferenceClassifier.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		try {
			classifier(List.of(prefix), List.of());
		} finally {
			logger.detachAppender(appender);
		}

		boolean warned = appender.list.stream()
				.anyMatch(event -> event.getLevel() == Level.WARN
						&& event.getFormattedMessage().contains(OVERLAP_WARNING));
		assertEquals(expectWarning, warned);
	}

	private static Stream<Arguments> overlappingPrefixes() {
		return Stream.of(
				Arguments.of("urn:", true),
				Arguments.of("urn:ngsi-ld:", true),
				Arguments.of("urn:ngsi-ld:category:", true),
				Arguments.of("urn:ngsi-ld:category:a1", true),
				Arguments.of(ID_PREFIX, false));
	}

	private static ReferenceClassifier classifier(List<String> idPrefixes, List<String> hrefPrefixes) {
		ReferenceProperties properties = new ReferenceProperties();
		properties.setExternalIdPrefixes(idPrefixes);
		properties.setExternalHrefPrefixes(hrefPrefixes);
		return new ReferenceClassifier(properties);
	}

	private static ReferencedEntity refEntity(String id, String href) {
		TestRef ref = new TestRef(id);
		ref.setHref(href == null ? null : URI.create(href));
		return ref;
	}

	private static ReferencedEntity referenceValue(String id, String href) {
		ReferenceValue ref = new ReferenceValue();
		ref.setId(URI.create(id));
		ref.setHref(URI.create(href));
		return ref;
	}

	private static ReferencedEntity shadowingRef(String id, String href) {
		ShadowingRef ref = new ShadowingRef(id);
		ref.setHref(URI.create(href));
		return ref;
	}

	private static class TestRef extends RefEntity {

		TestRef(String id) {
			super(id);
		}

		@Override
		public List<String> getReferencedTypes() {
			return List.of("category");
		}
	}

	/** Redeclares href like usage-management's UsageSpecificationRef, so RefEntity's own field stays null. */
	private static class ShadowingRef extends TestRef {

		@Getter
		@Setter
		private URI href;

		ShadowingRef(String id) {
			super(id);
		}
	}
}

package org.fiware.tmforum.common.validation;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.configuration.ReferenceProperties;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.common.domain.ReferenceValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReferenceClassifierTest {

	private static final String SERVER_HOST = "http://tmf.local:8632";
	private static final String PUBLIC_HOST = "tmf.example.org";
	private static final String FOREIGN_HOST = "partner-a.example.org";
	private static final String ID = "urn:ngsi-ld:category:1f6e2b0c-0000-4000-8000-000000000000";
	private static final String SERVER_HREF = SERVER_HOST + "/tmf-api/productCatalogManagement/v4/category/1";
	private static final String PUBLIC_HREF = "https://TMF.example.org/tmf-api/productCatalogManagement/v4/category/1";
	private static final String FOREIGN_HREF = "https://" + FOREIGN_HOST + "/category/42";
	private static final String FOREIGN_HREF_UPPER_CASE = "https://PARTNER-A.example.org/category/42";
	private static final String UNKNOWN_HREF = "https://partner-b.example.org/category/42";
	private static final String OTHER_SERVER_HOST = "http://tmf-2.local:8632";
	private static final String SCHEME_LESS_SERVER_HOST = "tmf.local:8632";
	private static final String INVALID_SERVER_HOST = "not a uri";
	private static final int REPEATED_CALLS = 3;

	@ParameterizedTest(name = "{0}")
	@MethodSource("classifications")
	public void classify(String description, String serverHost, List<String> localHosts, List<String> externalHosts,
			ReferencedEntity ref, ReferenceScope expected) {
		assertEquals(expected, classifier(serverHost, localHosts, externalHosts).classify(ref));
	}

	private static Stream<Arguments> classifications() {
		List<String> localHosts = List.of(PUBLIC_HOST);
		List<String> externalHosts = List.of(FOREIGN_HOST);
		return Stream.of(
				Arguments.of("null href", SERVER_HOST, localHosts, externalHosts, refEntity(null),
						ReferenceScope.LOCAL),
				Arguments.of("urn href", SERVER_HOST, localHosts, externalHosts, refEntity(ID),
						ReferenceScope.LOCAL),
				Arguments.of("relative href", SERVER_HOST, localHosts, externalHosts, refEntity("/category/1"),
						ReferenceScope.LOCAL),
				Arguments.of("href to the server host", SERVER_HOST, localHosts, externalHosts,
						refEntity(SERVER_HREF), ReferenceScope.LOCAL),
				Arguments.of("href to a configured local host, case-insensitive", SERVER_HOST, localHosts,
						externalHosts, refEntity(PUBLIC_HREF), ReferenceScope.LOCAL),
				Arguments.of("href to a configured external host", SERVER_HOST, localHosts, externalHosts,
						refEntity(FOREIGN_HREF), ReferenceScope.EXTERNAL),
				Arguments.of("href to a configured external host, case-insensitive", SERVER_HOST, localHosts,
						externalHosts, refEntity(FOREIGN_HREF_UPPER_CASE), ReferenceScope.EXTERNAL),
				// the href is client-supplied, so an unlisted host must not switch validation off
				Arguments.of("href to an unlisted host", SERVER_HOST, localHosts, externalHosts,
						refEntity(UNKNOWN_HREF), ReferenceScope.LOCAL),
				Arguments.of("no external hosts configured", SERVER_HOST, localHosts, List.of(),
						refEntity(FOREIGN_HREF), ReferenceScope.LOCAL),
				Arguments.of("no config at all", null, List.of(), List.of(), refEntity(FOREIGN_HREF),
						ReferenceScope.LOCAL),
				Arguments.of("a local host configured as external stays local", SERVER_HOST, List.of(PUBLIC_HOST),
						List.of(PUBLIC_HOST), refEntity(PUBLIC_HREF), ReferenceScope.LOCAL),
				Arguments.of("the server host configured as external stays local", SERVER_HOST, List.of(),
						List.of("tmf.local"), refEntity(SERVER_HREF), ReferenceScope.LOCAL),
				Arguments.of("invalid server host", INVALID_SERVER_HOST, List.of(), List.of("tmf.local"),
						refEntity(SERVER_HREF), ReferenceScope.EXTERNAL),
				Arguments.of("scheme-less server host", SCHEME_LESS_SERVER_HOST, List.of(), List.of("tmf.local"),
						refEntity(SERVER_HREF), ReferenceScope.EXTERNAL),
				Arguments.of("local hosts are trimmed, blank entries ignored", null,
						Arrays.asList(" " + PUBLIC_HOST + " ", "", null), externalHosts, refEntity(PUBLIC_HREF),
						ReferenceScope.LOCAL),
				Arguments.of("external hosts are trimmed, blank entries ignored", SERVER_HOST, List.of(),
						Arrays.asList(" " + FOREIGN_HOST + " ", "", null), refEntity(FOREIGN_HREF),
						ReferenceScope.EXTERNAL),
				Arguments.of("reference value with external href", SERVER_HOST, localHosts, externalHosts,
						referenceValue(FOREIGN_HREF), ReferenceScope.EXTERNAL),
				Arguments.of("subclass shadowing href", SERVER_HOST, localHosts, externalHosts,
						shadowingRef(FOREIGN_HREF), ReferenceScope.EXTERNAL));
	}

	// TestServerHostConfigurer sets the server host only once the embedded server is up
	@Test
	public void serverHostIsReadPerCall() {
		GeneralProperties generalProperties = new GeneralProperties();
		ReferenceClassifier classifier = new ReferenceClassifier(externalHostProperties(List.of("tmf.local")),
				generalProperties);
		generalProperties.setServerHost(SERVER_HOST);

		assertEquals(ReferenceScope.LOCAL, classifier.classify(refEntity(SERVER_HREF)));
	}

	@Test
	public void changedServerHostIsReparsed() {
		GeneralProperties generalProperties = new GeneralProperties();
		generalProperties.setServerHost(SERVER_HOST);
		ReferenceClassifier classifier = new ReferenceClassifier(externalHostProperties(List.of("tmf.local")),
				generalProperties);
		assertEquals(ReferenceScope.LOCAL, classifier.classify(refEntity(SERVER_HREF)));

		generalProperties.setServerHost(OTHER_SERVER_HOST);

		assertEquals(ReferenceScope.EXTERNAL, classifier.classify(refEntity(SERVER_HREF)));
	}

	@ParameterizedTest
	@ValueSource(strings = {INVALID_SERVER_HOST, SCHEME_LESS_SERVER_HOST})
	public void unusableServerHostIsWarnedAboutOnce(String serverHost) {
		List<ILoggingEvent> events = recordClassifierLog(() -> {
			ReferenceClassifier classifier = classifier(serverHost, List.of(), List.of("tmf.local"));
			for (int i = 0; i < REPEATED_CALLS; i++) {
				classifier.classify(refEntity(SERVER_HREF));
			}
		});

		assertEquals(1, events.stream().filter(event -> event.getLevel() == Level.WARN).count());
	}

	@Test
	public void aHostConfiguredAsLocalAndExternalIsWarnedAboutOnStartup() {
		List<ILoggingEvent> events = recordClassifierLog(
				() -> classifier(SERVER_HOST, List.of(PUBLIC_HOST), List.of(PUBLIC_HOST, FOREIGN_HOST)));

		assertEquals(1, events.stream()
				.filter(event -> event.getLevel() == Level.WARN)
				.filter(event -> event.getFormattedMessage().contains(PUBLIC_HOST))
				.count());
	}

	private static List<ILoggingEvent> recordClassifierLog(Runnable runnable) {
		Logger classifierLogger = (Logger) LoggerFactory.getLogger(ReferenceClassifier.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		classifierLogger.addAppender(appender);
		try {
			runnable.run();
		} finally {
			classifierLogger.detachAppender(appender);
		}
		return appender.list;
	}

	private static ReferenceClassifier classifier(String serverHost, List<String> localHosts,
			List<String> externalHosts) {
		ReferenceProperties referenceProperties = new ReferenceProperties();
		referenceProperties.setLocalHosts(localHosts);
		referenceProperties.setExternalHosts(externalHosts);
		GeneralProperties generalProperties = new GeneralProperties();
		generalProperties.setServerHost(serverHost);
		return new ReferenceClassifier(referenceProperties, generalProperties);
	}

	private static ReferenceProperties externalHostProperties(List<String> externalHosts) {
		ReferenceProperties referenceProperties = new ReferenceProperties();
		referenceProperties.setExternalHosts(externalHosts);
		return referenceProperties;
	}

	private static ReferencedEntity refEntity(String href) {
		TestRef ref = new TestRef(ID);
		ref.setHref(href == null ? null : URI.create(href));
		return ref;
	}

	private static ReferencedEntity referenceValue(String href) {
		ReferenceValue ref = new ReferenceValue();
		ref.setId(URI.create(ID));
		ref.setHref(URI.create(href));
		return ref;
	}

	private static ReferencedEntity shadowingRef(String href) {
		ShadowingRef ref = new ShadowingRef(ID);
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

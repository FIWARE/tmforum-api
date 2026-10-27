package org.fiware.tmforum.usagemanagement.domain;

import org.fiware.tmforum.common.configuration.ReferenceProperties;
import org.fiware.tmforum.common.validation.ReferenceClassifier;
import org.fiware.tmforum.common.validation.ReferenceScope;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UsageSpecificationRefTest {

	private static final String HREF_PREFIX = "https://partner-a.example.org/";

	// UsageSpecificationRef redeclares href, so RefEntity's own field stays null
	@Test
	public void classifiedByItsOwnHref() {
		UsageSpecificationRef ref = new UsageSpecificationRef("urn:ngsi-ld:usage-specification:1");
		ref.setHref(URI.create(HREF_PREFIX + "usageSpecification/1"));
		ReferenceProperties properties = new ReferenceProperties();
		properties.setExternalHrefPrefixes(List.of(HREF_PREFIX));

		assertEquals(ReferenceScope.EXTERNAL_BY_HREF, new ReferenceClassifier(properties).classify(ref));
	}
}

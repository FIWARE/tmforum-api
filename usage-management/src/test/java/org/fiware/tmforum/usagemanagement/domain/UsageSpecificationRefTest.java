package org.fiware.tmforum.usagemanagement.domain;

import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.configuration.ReferenceProperties;
import org.fiware.tmforum.common.validation.ReferenceClassifier;
import org.fiware.tmforum.common.validation.ReferenceScope;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UsageSpecificationRefTest {

	private static final String FOREIGN_HREF = "https://partner-a.example.org/usageSpecification/1";

	// UsageSpecificationRef redeclares href, so RefEntity's own field stays null
	@Test
	public void classifiedByItsOwnHref() {
		UsageSpecificationRef ref = new UsageSpecificationRef("urn:ngsi-ld:usage-specification:1");
		ref.setHref(URI.create(FOREIGN_HREF));
		ReferenceClassifier classifier = new ReferenceClassifier(new ReferenceProperties(), new GeneralProperties());

		assertEquals(ReferenceScope.EXTERNAL, classifier.classify(ref));
	}
}

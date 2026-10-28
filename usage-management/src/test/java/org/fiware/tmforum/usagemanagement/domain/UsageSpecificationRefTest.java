package org.fiware.tmforum.usagemanagement.domain;

import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.configuration.ReferenceProperties;
import org.fiware.tmforum.common.validation.ReferenceClassifier;
import org.fiware.tmforum.common.validation.ReferenceScope;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UsageSpecificationRefTest {

	private static final String EXTERNAL_HOST = "partner-a.example.org";
	private static final String FOREIGN_HREF = "https://" + EXTERNAL_HOST + "/usageSpecification/1";

	// UsageSpecificationRef redeclares href, so RefEntity's own field stays null
	@Test
	public void classifiedByItsOwnHref() {
		UsageSpecificationRef ref = new UsageSpecificationRef("urn:ngsi-ld:usage-specification:1");
		ref.setHref(URI.create(FOREIGN_HREF));
		ReferenceProperties referenceProperties = new ReferenceProperties();
		referenceProperties.setExternalHosts(List.of(EXTERNAL_HOST));
		ReferenceClassifier classifier = new ReferenceClassifier(referenceProperties, new GeneralProperties());

		assertEquals(ReferenceScope.EXTERNAL, classifier.classify(ref));
	}
}

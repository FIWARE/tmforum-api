package org.fiware.tmforum.common.domain.subscription;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@code query}/{@code rawQuery} are inherently key=value-shaped and would otherwise contain
 * NGSI-LD's "Forbidden Characters" (e.g. '='). The escaped getters/setters used by the mapping
 * layer must transparently escape/unescape, while the plain ones keep working with the original
 * value.
 */
class TMForumSubscriptionTest {

	@Test
	public void theEscapedQueryIsUrlEncodedWhileThePlainGetterStaysReadable() {
		TMForumSubscription subscription = new TMForumSubscription("urn:ngsi-ld:tm-forum-subscription:1");

		subscription.setQuery("callback=http://example.org&type=product");

		assertEquals("callback=http://example.org&type=product", subscription.getQuery());
		assertEquals("callback%3Dhttp%3A%2F%2Fexample.org%26type%3Dproduct", subscription.getEscapedQuery());
	}

	@Test
	public void settingTheEscapedQueryUnescapesItForThePlainGetter() {
		TMForumSubscription subscription = new TMForumSubscription("urn:ngsi-ld:tm-forum-subscription:1");

		subscription.setEscapedQuery("callback%3Dhttp%3A%2F%2Fexample.org");

		assertEquals("callback=http://example.org", subscription.getQuery());
	}

	@Test
	public void theEscapedRawQueryRoundTripsTheSameWay() {
		TMForumSubscription subscription = new TMForumSubscription("urn:ngsi-ld:tm-forum-subscription:1");

		subscription.setRawQuery("a=b;c=d");

		assertEquals("a%3Db%3Bc%3Dd", subscription.getEscapedRawQuery());
		subscription.setEscapedRawQuery(subscription.getEscapedRawQuery());
		assertEquals("a=b;c=d", subscription.getRawQuery());
	}

	@Test
	public void nullQueryValuesArePassedThroughUnchanged() {
		TMForumSubscription subscription = new TMForumSubscription("urn:ngsi-ld:tm-forum-subscription:1");

		assertNull(subscription.getEscapedQuery());
		assertNull(subscription.getEscapedRawQuery());
	}
}

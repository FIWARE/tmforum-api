package org.fiware.tmforum.customerbillmanagement.domain;

import org.fiware.tmforum.common.test.NgsiLdRoundTrip;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Pins that a stored {@link CustomerBill#getPaymentMethod()} reads back as written. Its setter used to name the
 * generated {@code PaymentMethodRefVO}, which has no String constructor, so the read failed (ticket 88).
 */
class PaymentMethodRoundTripTest {

	private static final String BILL_ID = "urn:ngsi-ld:customer-bill:b";
	private static final String PAYMENT_METHOD_ID = "urn:ngsi-ld:payment-method:p";
	private static final String PAYMENT_METHOD_TYPE = "PaymentMethod";

	@ParameterizedTest(name = "{0}")
	@MethodSource("paymentMethods")
	public void paymentMethodSurvivesTheRoundTrip(String description, PaymentMethodRef paymentMethod) {
		CustomerBill customerBill = new CustomerBill(BILL_ID);
		customerBill.setPaymentMethod(paymentMethod);

		CustomerBill read = NgsiLdRoundTrip.roundTrip(customerBill, CustomerBill.class);

		assertNotNull(read.getPaymentMethod(), description);
		assertEquals(NgsiLdRoundTrip.toTree(paymentMethod), NgsiLdRoundTrip.toTree(read.getPaymentMethod()),
				description);
	}

	private static Stream<Arguments> paymentMethods() {
		PaymentMethodRef full = new PaymentMethodRef(PAYMENT_METHOD_ID);
		full.setHref(URI.create("https://external.org/payment-method/p"));
		full.setName("payment method");
		full.setAtReferredType(PAYMENT_METHOD_TYPE);
		return Stream.of(
				Arguments.of("id only", new PaymentMethodRef(PAYMENT_METHOD_ID)),
				Arguments.of("every field", full));
	}
}

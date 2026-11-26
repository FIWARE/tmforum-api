package org.fiware.tmforum.account.domain;

import org.fiware.tmforum.common.test.NgsiLdRoundTrip;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Pins that a stored {@code defaultPaymentMethod} reads back as written. Its setters used to name the generated
 * {@code PaymentMethodRefVO}, which has no String constructor, so the read failed (ticket 88).
 */
class DefaultPaymentMethodRoundTripTest {

	private static final String ACCOUNT_ID = "urn:ngsi-ld:account:a";
	private static final String PAYMENT_METHOD_ID = "urn:ngsi-ld:payment-method:p";
	private static final String PAYMENT_METHOD_TYPE = "PaymentMethod";

	@ParameterizedTest(name = "{0}")
	@MethodSource("accounts")
	public <T> void defaultPaymentMethodSurvivesTheRoundTrip(String description, T account, Class<T> accountClass,
			Function<T, PaymentMethodRef> defaultPaymentMethod) {
		T read = NgsiLdRoundTrip.roundTrip(account, accountClass);

		assertNotNull(defaultPaymentMethod.apply(read), description);
		assertEquals(NgsiLdRoundTrip.toTree(defaultPaymentMethod.apply(account)),
				NgsiLdRoundTrip.toTree(defaultPaymentMethod.apply(read)), description);
	}

	private static Stream<Arguments> accounts() {
		return Stream.of(idOnlyPaymentMethod(), fullPaymentMethod()).flatMap(paymentMethod -> {
			BillingAccount billingAccount = new BillingAccount(ACCOUNT_ID);
			billingAccount.setDefaultPaymentMethod(paymentMethod);
			PartyAccount partyAccount = new PartyAccount(ACCOUNT_ID);
			partyAccount.setDefaultPaymentMethod(paymentMethod);
			SettlementAccount settlementAccount = new SettlementAccount(ACCOUNT_ID);
			settlementAccount.setDefaultPaymentMethod(paymentMethod);
			String fields = paymentMethod.getName() == null ? "id only" : "every field";
			return Stream.of(
					Arguments.of("BillingAccount " + fields, billingAccount, BillingAccount.class,
							(Function<BillingAccount, PaymentMethodRef>) BillingAccount::getDefaultPaymentMethod),
					Arguments.of("PartyAccount " + fields, partyAccount, PartyAccount.class,
							(Function<PartyAccount, PaymentMethodRef>) PartyAccount::getDefaultPaymentMethod),
					Arguments.of("SettlementAccount " + fields, settlementAccount, SettlementAccount.class,
							(Function<SettlementAccount, PaymentMethodRef>) SettlementAccount::getDefaultPaymentMethod));
		});
	}

	private static PaymentMethodRef idOnlyPaymentMethod() {
		return new PaymentMethodRef(PAYMENT_METHOD_ID);
	}

	private static PaymentMethodRef fullPaymentMethod() {
		PaymentMethodRef paymentMethod = new PaymentMethodRef(PAYMENT_METHOD_ID);
		paymentMethod.setHref(URI.create("https://external.org/payment-method/p"));
		paymentMethod.setName("payment method");
		paymentMethod.setAtReferredType(PAYMENT_METHOD_TYPE);
		return paymentMethod;
	}
}

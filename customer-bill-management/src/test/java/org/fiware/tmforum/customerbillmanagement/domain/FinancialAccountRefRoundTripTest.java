package org.fiware.tmforum.customerbillmanagement.domain;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.EntityId;
import io.github.wistefan.mapping.annotations.EntityType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.Money;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.common.test.NgsiLdRoundTrip;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.time.Instant;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Pins that the customer-bill {@link FinancialAccountRef} stores its balance list inline and reads it back as {@link AccountBalance}s.
 */
class FinancialAccountRefRoundTripTest {

	private static final String HOLDER_TYPE = "holder";
	private static final String HOLDER_ID = "urn:ngsi-ld:holder:h";
	private static final String FINANCIAL_ACCOUNT_ID = "urn:ngsi-ld:financial-account:f";
	private static final String FINANCIAL_ACCOUNT_TYPE = "financial-account";
	private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
	private static final float BALANCE = 42.5f;

	@ParameterizedTest(name = "{0}")
	@MethodSource("refs")
	public void everyFieldSurvivesTheRoundTrip(String description, FinancialAccountRef financialAccount) {
		Holder holder = new Holder(HOLDER_ID);
		holder.setFinancialAccount(financialAccount);

		Holder read = NgsiLdRoundTrip.roundTrip(holder, Holder.class);

		assertEquals(NgsiLdRoundTrip.toTree(financialAccount), NgsiLdRoundTrip.toTree(read.getFinancialAccount()),
				description);
		if (financialAccount.getAccountBalance() != null) {
			read.getFinancialAccount().getAccountBalance()
					.forEach(balance -> assertInstanceOf(AccountBalance.class, balance, description));
		}
	}

	private static Stream<Arguments> refs() {
		return Stream.of(
				Arguments.of("id only", new FinancialAccountRef(FINANCIAL_ACCOUNT_ID)),
				Arguments.of("one balance", fullRef(1)),
				Arguments.of("several balances", fullRef(3)));
	}

	private static FinancialAccountRef fullRef(int balanceCount) {
		FinancialAccountRef financialAccount = new FinancialAccountRef(FINANCIAL_ACCOUNT_ID);
		financialAccount.setHref(URI.create("https://external.org/financial-account/f"));
		financialAccount.setName("financial account");
		financialAccount.setAtReferredType(FINANCIAL_ACCOUNT_TYPE);
		financialAccount.setAccountBalance(IntStream.range(0, balanceCount)
				.mapToObj(FinancialAccountRefRoundTripTest::balance)
				.toList());
		return financialAccount;
	}

	private static AccountBalance balance(int index) {
		Money amount = new Money();
		amount.setUnit("EUR");
		amount.setTmfValue(BALANCE + index);
		TimePeriod validFor = new TimePeriod();
		validFor.setStartDateTime(START);
		AccountBalance accountBalance = new AccountBalance();
		accountBalance.setBalanceType("deposit-" + index);
		accountBalance.setAmount(amount);
		accountBalance.setValidFor(validFor);
		return accountBalance;
	}

	/** Entity holding the ref read from its properties, as every relationship will be. */
	@MappingEnabled(entityType = HOLDER_TYPE)
	public static class Holder {

		@Getter(onMethod = @__({@EntityId}))
		private final URI id;

		@Getter(onMethod = @__({@EntityType}))
		private final String type = HOLDER_TYPE;

		@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "financialAccount")}))
		@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "financialAccount",
				fromProperties = true, targetClass = FinancialAccountRef.class)}))
		private FinancialAccountRef financialAccount;

		public Holder(String id) {
			this.id = URI.create(id);
		}
	}
}

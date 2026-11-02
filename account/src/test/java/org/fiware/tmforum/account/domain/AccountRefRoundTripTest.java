package org.fiware.tmforum.account.domain;

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
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins that every field of the account module's {@link FinancialAccountRef} and {@link AccountRef} is stored inline in the relationship, so it reads back without the target.
 */
class AccountRefRoundTripTest {

	private static final String HOLDER_TYPE = "holder";
	private static final String HOLDER_ID = "urn:ngsi-ld:holder:h";
	private static final String FINANCIAL_ACCOUNT_ID = "urn:ngsi-ld:financial-account:f";
	private static final String ACCOUNT_ID = "urn:ngsi-ld:party-account:a";
	private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
	private static final float BALANCE = 42.5f;

	@ParameterizedTest(name = "{0}")
	@MethodSource("refs")
	public void everyFieldSurvivesTheRoundTrip(String description, Holder holder, Function<Holder, Object> ref) {
		Holder read = NgsiLdRoundTrip.roundTrip(holder, Holder.class);

		assertEquals(NgsiLdRoundTrip.toTree(ref.apply(holder)), NgsiLdRoundTrip.toTree(ref.apply(read)), description);
	}

	private static Stream<Arguments> refs() {
		Money amount = new Money();
		amount.setUnit("EUR");
		amount.setTmfValue(BALANCE);
		TimePeriod validFor = new TimePeriod();
		validFor.setStartDateTime(START);
		AccountBalance accountBalance = new AccountBalance();
		accountBalance.setBalanceType("deposit");
		accountBalance.setAmount(amount);
		accountBalance.setValidFor(validFor);
		FinancialAccountRef financialAccount = new FinancialAccountRef(FINANCIAL_ACCOUNT_ID);
		financialAccount.setHref(URI.create("https://external.org/financial-account/f"));
		financialAccount.setName("financial account");
		financialAccount.setAtReferredType(FinancialAccount.TYPE_FINANCIALAC);
		financialAccount.setAccountBalance(accountBalance);
		AccountRef account = new AccountRef(ACCOUNT_ID);
		account.setHref(URI.create("https://external.org/party-account/a"));
		account.setName("account");
		account.setAtReferredType(PartyAccount.TYPE_PARTYAC);
		account.setDescription("the account");

		Holder idOnlyFinancialAccount = new Holder(HOLDER_ID);
		idOnlyFinancialAccount.setFinancialAccount(new FinancialAccountRef(FINANCIAL_ACCOUNT_ID));
		Holder fullFinancialAccount = new Holder(HOLDER_ID);
		fullFinancialAccount.setFinancialAccount(financialAccount);
		Holder idOnlyAccount = new Holder(HOLDER_ID);
		idOnlyAccount.setAccount(new AccountRef(ACCOUNT_ID));
		Holder fullAccount = new Holder(HOLDER_ID);
		fullAccount.setAccount(account);
		return Stream.of(
				Arguments.of("FinancialAccountRef id only", idOnlyFinancialAccount,
						(Function<Holder, Object>) Holder::getFinancialAccount),
				Arguments.of("FinancialAccountRef every field", fullFinancialAccount,
						(Function<Holder, Object>) Holder::getFinancialAccount),
				Arguments.of("AccountRef id only", idOnlyAccount, (Function<Holder, Object>) Holder::getAccount),
				Arguments.of("AccountRef every field", fullAccount, (Function<Holder, Object>) Holder::getAccount));
	}

	/** Entity holding each ref read from its properties, as every relationship will be. */
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

		@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "account")}))
		@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "account",
				fromProperties = true, targetClass = AccountRef.class)}))
		private AccountRef account;

		public Holder(String id) {
			this.id = URI.create(id);
		}
	}
}

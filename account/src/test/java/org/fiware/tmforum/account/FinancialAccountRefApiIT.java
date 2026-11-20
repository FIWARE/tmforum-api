package org.fiware.tmforum.account;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.account.api.BillingAccountApiTestClient;
import org.fiware.account.api.FinancialAccountApiTestClient;
import org.fiware.account.model.AccountBalanceVO;
import org.fiware.account.model.BillingAccountCreateVO;
import org.fiware.account.model.BillingAccountCreateVOTestExample;
import org.fiware.account.model.BillingAccountUpdateVO;
import org.fiware.account.model.BillingAccountUpdateVOTestExample;
import org.fiware.account.model.BillingAccountVO;
import org.fiware.account.model.FinancialAccountCreateVO;
import org.fiware.account.model.FinancialAccountCreateVOTestExample;
import org.fiware.account.model.FinancialAccountRefVO;
import org.fiware.account.model.FinancialAccountVO;
import org.fiware.account.model.MoneyVO;
import org.fiware.account.model.TimePeriodVO;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.account.domain.BillingAccount;
import org.fiware.tmforum.account.domain.FinancialAccount;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Round-trip of a billing account's financialAccount reference to a stored financial account through the API.
 * Before ticket 88 the read resolved the target, and a target with several balances failed it, since its balance
 * list cannot map to the reference's single accountBalance.
 */
@MicronautTest(packages = {"org.fiware.tmforum.account"}, contextBuilder = FirstMatchConfigContextBuilder.class)
public class FinancialAccountRefApiIT extends AbstractApiIT {

	private static final String BALANCE_TYPE = "deposit";
	private static final String CURRENCY = "EUR";
	private static final Instant VALID_FROM = Instant.parse("2026-01-01T00:00:00Z");
	// written into the reference's name and balance amount, so a read-back tells the patched payload from the created one
	private static final String CREATE = "create";
	private static final String PATCH = "patch";
	private static final float CREATE_AMOUNT = 10.5f;
	private static final float PATCH_AMOUNT = 20.25f;

	/**
	 * How much of the reference a test case posts.
	 */
	enum Payload {
		// the id only
		MINIMAL,
		// every field, with an accountBalance that differs from the target's
		FULL
	}

	private final BillingAccountApiTestClient billingAccountApiTestClient;
	private final FinancialAccountApiTestClient financialAccountApiTestClient;

	public FinancialAccountRefApiIT(EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper,
			GeneralProperties generalProperties, BillingAccountApiTestClient billingAccountApiTestClient,
			FinancialAccountApiTestClient financialAccountApiTestClient) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.billingAccountApiTestClient = billingAccountApiTestClient;
		this.financialAccountApiTestClient = financialAccountApiTestClient;
	}

	@MockBean(TMForumEventHandler.class)
	public TMForumEventHandler eventHandler() {
		TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);
		when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
		when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());
		return eventHandler;
	}

	private static Stream<Arguments> references() {
		return Stream.of(
				Arguments.of("A minimal reference to an account with one balance should round-trip.", Payload.MINIMAL,
						1),
				Arguments.of("A full reference to an account with one balance should round-trip.", Payload.FULL, 1),
				Arguments.of("A minimal reference to an account with several balances should round-trip.",
						Payload.MINIMAL, 2),
				Arguments.of("A full reference to an account with several balances should round-trip.", Payload.FULL,
						2));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("references")
	public void financialAccountReferenceRoundTrips(String message, Payload payload, int targetBalances)
			throws Exception {
		String targetId = createFinancialAccount(targetBalances);

		FinancialAccountRefVO created = reference(targetId, payload, CREATE, CREATE_AMOUNT);
		BillingAccountCreateVO createVO = BillingAccountCreateVOTestExample.build().atSchemaLocation(null)
				.billStructure(null)
				.defaultPaymentMethod(null)
				.financialAccount(created);
		HttpResponse<BillingAccountVO> createResponse = callAndCatch(
				() -> billingAccountApiTestClient.createBillingAccount(null, createVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), message);
		String id = createResponse.body().getId();
		assertRoundTrip(created, createResponse.body(), "create response: " + message);
		assertRoundTrip(created, retrieve(id), "retrieval after create: " + message);

		FinancialAccountRefVO patched = reference(targetId, payload, PATCH, PATCH_AMOUNT);
		BillingAccountUpdateVO updateVO = BillingAccountUpdateVOTestExample.build().atSchemaLocation(null)
				.billStructure(null)
				.defaultPaymentMethod(null)
				.financialAccount(patched);
		HttpResponse<BillingAccountVO> patchResponse = callAndCatch(
				() -> billingAccountApiTestClient.patchBillingAccount(null, id, updateVO));
		assertEquals(HttpStatus.OK, patchResponse.getStatus(), message);
		assertRoundTrip(patched, patchResponse.body(), "patch response: " + message);
		assertRoundTrip(patched, retrieve(id), "retrieval after patch: " + message);
	}

	private static FinancialAccountRefVO reference(String targetId, Payload payload, String stage, float amount) {
		FinancialAccountRefVO reference = new FinancialAccountRefVO().id(targetId);
		if (payload == Payload.MINIMAL) {
			return reference;
		}
		return reference
				.href(targetId)
				.name(String.format("%s (%s)", targetId, stage))
				.atReferredType(FinancialAccount.TYPE_FINANCIALAC)
				.accountBalance(balance(amount));
	}

	private static AccountBalanceVO balance(float amount) {
		return new AccountBalanceVO()
				.balanceType(BALANCE_TYPE)
				.amount(new MoneyVO().unit(CURRENCY).value(amount))
				.validFor(new TimePeriodVO().startDateTime(VALID_FROM));
	}

	private void assertRoundTrip(FinancialAccountRefVO posted, BillingAccountVO readBack, String message) {
		assertEquals(objectMapper.valueToTree(posted), objectMapper.valueToTree(readBack.getFinancialAccount()),
				message);
	}

	private String createFinancialAccount(int balances) throws Exception {
		FinancialAccountCreateVO createVO = FinancialAccountCreateVOTestExample.build().atSchemaLocation(null)
				.accountBalance(IntStream.range(0, balances).mapToObj(i -> balance(CREATE_AMOUNT + i)).toList());
		HttpResponse<FinancialAccountVO> response = callAndCatch(
				() -> financialAccountApiTestClient.createFinancialAccount(null, createVO));
		assertEquals(HttpStatus.CREATED, response.getStatus(), "The target financial account should have been created.");
		return response.body().getId();
	}

	private BillingAccountVO retrieve(String id) throws Exception {
		HttpResponse<BillingAccountVO> retrieved = callAndCatch(
				() -> billingAccountApiTestClient.retrieveBillingAccount(null, id, null));
		assertEquals(HttpStatus.OK, retrieved.getStatus(), "The billing account should be retrievable.");
		return retrieved.body();
	}

	@Override
	protected String getEntityType() {
		return BillingAccount.TYPE_BILLINGAC;
	}
}

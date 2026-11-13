package org.fiware.tmforum.customerbillmanagement;

import io.github.wistefan.mapping.JavaObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.customerbillmanagement.api.CustomerBillApiTestClient;
import org.fiware.customerbillmanagement.model.AccountBalanceVO;
import org.fiware.customerbillmanagement.model.CustomerBillVO;
import org.fiware.customerbillmanagement.model.CustomerBillVOTestExample;
import org.fiware.customerbillmanagement.model.FinancialAccountRefVO;
import org.fiware.customerbillmanagement.model.MoneyVO;
import org.fiware.customerbillmanagement.model.TimePeriodVO;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.BrokerOnly;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.customerbillmanagement.domain.CustomerBill;
import org.fiware.tmforum.storage.EntityRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins that {@link FinancialAccountRefVO#getAccountBalance()} is not stored by any backend yet (its encoding is an
 * open decision, see {@code EntityReadRepository.get}), while the other fields of the reference round-trip.
 */
@MicronautTest(packages = {"org.fiware.tmforum.customerbillmanagement"}, contextBuilder = FirstMatchConfigContextBuilder.class)
public class FinancialAccountRefIT extends AbstractApiIT {

	private static final String FINANCIAL_ACCOUNT_ID = "urn:ngsi-ld:financial-account:f";
	private static final String FINANCIAL_ACCOUNT_HREF = "https://external.org/financial-account/f";
	private static final String FINANCIAL_ACCOUNT_NAME = "financial account";
	private static final String FINANCIAL_ACCOUNT_REFERRED_TYPE = "FinancialAccount";
	private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
	private static final float BALANCE = 42.5f;
	private static final int SEVERAL = 3;

	private final CustomerBillApiTestClient customerBillApiTestClient;
	private final EntitiesApiClient entitiesApiClient;
	private final EntityRepository entityRepository;
	private final JavaObjectMapper javaObjectMapper;
	private final TMForumMapper tmForumMapper;

	public FinancialAccountRefIT(CustomerBillApiTestClient customerBillApiTestClient,
			EntitiesApiClient entitiesApiClient, EntityRepository entityRepository, JavaObjectMapper javaObjectMapper,
			TMForumMapper tmForumMapper, ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.customerBillApiTestClient = customerBillApiTestClient;
		this.entitiesApiClient = entitiesApiClient;
		this.entityRepository = entityRepository;
		this.javaObjectMapper = javaObjectMapper;
		this.tmForumMapper = tmForumMapper;
	}

	@MockBean(TMForumEventHandler.class)
	public TMForumEventHandler eventHandler() {
		TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);
		when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
		when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());
		return eventHandler;
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("balanceCounts")
	public void accountBalancesAreNotStored(String message, int balanceCount) throws Exception {
		FinancialAccountRefVO financialAccount = financialAccount(balanceCount);
		String billId = "urn:ngsi-ld:customer-bill:" + UUID.randomUUID();
		entityRepository.createDomainEntity(tmForumMapper.map(bill(billId, financialAccount))).block();

		FinancialAccountRefVO read = retrieveFinancialAccount(billId, message);
		assertNull(read.getAccountBalance(), message);
	}

	/**
	 * Kept as the reproduction of the Orion-LD merge. Re-enabling it needs the accountBalance getter change, which
	 * waits on the open encoding decision in the ticket 88 plan's Step 1 result; until then it fails on every broker.
	 */
	@Disabled("accountBalance is not stored inline until its encoding is decided; with a PROPERTY getter Orion-LD merges the instances and the read fails with 400.")
	@BrokerOnly
	@ParameterizedTest(name = "{0}")
	@MethodSource("balanceCounts")
	public void accountBalancesSurviveTheBroker(String message, int balanceCount) throws Exception {
		FinancialAccountRefVO financialAccount = financialAccount(balanceCount);
		String billId = "urn:ngsi-ld:customer-bill:" + UUID.randomUUID();
		entitiesApiClient.createEntity(javaObjectMapper.toEntityVO(tmForumMapper.map(bill(billId, financialAccount))), null)
				.block();

		FinancialAccountRefVO read = retrieveFinancialAccount(billId, message);
		assertNotNull(read.getAccountBalance(), message);
		// NGSI-LD does not guarantee the order of the instances of a multi-instance attribute
		assertEquals(sortedByType(financialAccount.getAccountBalance()), sortedByType(read.getAccountBalance()), message);
	}

	private static FinancialAccountRefVO financialAccount(int balanceCount) {
		return new FinancialAccountRefVO()
				.id(FINANCIAL_ACCOUNT_ID)
				.href(FINANCIAL_ACCOUNT_HREF)
				.name(FINANCIAL_ACCOUNT_NAME)
				.atReferredType(FINANCIAL_ACCOUNT_REFERRED_TYPE)
				.accountBalance(IntStream.range(0, balanceCount).mapToObj(FinancialAccountRefIT::balance).toList());
	}

	private static CustomerBillVO bill(String billId, FinancialAccountRefVO financialAccount) {
		return CustomerBillVOTestExample.build().atSchemaLocation(null).id(billId)
				.relatedParty(null)
				.financialAccount(financialAccount)
				.billingAccount(null)
				.paymentMethod(null);
	}

	// asserts the fields every backend stores
	private FinancialAccountRefVO retrieveFinancialAccount(String billId, String message) throws Exception {
		HttpResponse<CustomerBillVO> response = callAndCatch(
				() -> customerBillApiTestClient.retrieveCustomerBill(null, billId, null));
		assertEquals(HttpStatus.OK, response.getStatus(), message);
		FinancialAccountRefVO read = response.body().getFinancialAccount();
		assertEquals(FINANCIAL_ACCOUNT_ID, read.getId(), message);
		assertEquals(FINANCIAL_ACCOUNT_HREF, read.getHref(), message);
		assertEquals(FINANCIAL_ACCOUNT_NAME, read.getName(), message);
		assertEquals(FINANCIAL_ACCOUNT_REFERRED_TYPE, read.getAtReferredType(), message);
		return read;
	}

	@Override
	protected String getEntityType() {
		return CustomerBill.TYPE_CUSTOMER_BILL;
	}

	private static Stream<Arguments> balanceCounts() {
		return Stream.of(
				Arguments.of("A single balance should be returned.", 1),
				Arguments.of("Every one of several balances should be returned.", SEVERAL));
	}

	private static AccountBalanceVO balance(int index) {
		return new AccountBalanceVO()
				.balanceType("deposit-" + index)
				.amount(new MoneyVO().unit("EUR").value(BALANCE + index))
				.validFor(new TimePeriodVO().startDateTime(START));
	}

	private static List<AccountBalanceVO> sortedByType(List<AccountBalanceVO> balances) {
		return balances.stream()
				.sorted(Comparator.comparing(AccountBalanceVO::getBalanceType))
				.toList();
	}
}

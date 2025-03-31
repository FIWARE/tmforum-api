package org.fiware.tmforum.quote;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.quote.api.QuoteApiTestClient;
import org.fiware.quote.api.QuoteApiTestSpec;
import org.fiware.quote.model.*;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.product.Quote;
import org.fiware.tmforum.product.QuoteItemState;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest(packages = {"org.fiware.tmforum.quote"})
public class QuoteApiIT extends AbstractApiIT implements QuoteApiTestSpec {

	public final QuoteApiTestClient quoteApiTestClient;

	private String message;
	private String fieldsParameter;
	private QuoteCreateVO quoteCreateVO;
	private QuoteUpdateVO quoteUpdateVO;
	private QuoteVO expectedQuote;

	private Clock clock = mock(Clock.class);

	@MockBean(Clock.class)
	public Clock clock() {
		return clock;
	}

	@MockBean(TMForumEventHandler.class)
	public TMForumEventHandler eventHandler() {
		TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);

		when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
		when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());

		return eventHandler;
	}

	public QuoteApiIT(QuoteApiTestClient quoteApiTestClient,
					  EntitiesApiClient entitiesApiClient,
					  ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.quoteApiTestClient = quoteApiTestClient;
	}

	@ParameterizedTest
	@MethodSource("provideValidQuotes")
	public void createQuote201(String message, QuoteCreateVO quoteCreateVO,
							   QuoteVO expectedQuote)
			throws Exception {
		this.message = message;
		this.quoteCreateVO = quoteCreateVO;
		this.expectedQuote = expectedQuote;
		createQuote201();
	}

	@Override
	public void createQuote200() throws Exception {
		// unused, only 201 can be reached on creation.
	}

	@Override
	public void createQuote201() throws Exception {

		Instant now = Instant.now();
		when(clock.instant()).thenReturn(now);

		HttpResponse<QuoteVO> productVOHttpResponse = callAndCatch(
				() -> quoteApiTestClient.createQuote(null, quoteCreateVO));
		assertEquals(HttpStatus.CREATED, productVOHttpResponse.getStatus(), message);
		String rfId = productVOHttpResponse.body().getId();
		expectedQuote.setId(rfId);
		expectedQuote.setHref(rfId);
		expectedQuote.setQuoteDate(now);
		assertEquals(expectedQuote, productVOHttpResponse.body(), message);
	}

	private static Stream<Arguments> provideValidQuotes() {
		Instant now = Instant.now();
		return Stream.of(
				Arguments.of("An empty quote should have been created.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteVOTestExample.build()
								.atSchemaLocation(null)
								.validFor(null)
								.state(QuoteStateTypeVO.IN_PROGRESS)
								.quoteTotalPrice(null)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))),
				Arguments.of("A quote with an expected fullfilment date should have been created.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.expectedFulfillmentStartDate(now)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteVOTestExample.build()
								.atSchemaLocation(null)
								.expectedFulfillmentStartDate(now)
								.validFor(null)
								.state(QuoteStateTypeVO.IN_PROGRESS)
								.quoteTotalPrice(null)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))),
				Arguments.of("A quote with an expected fullfilment start date should have been created.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.requestedQuoteCompletionDate(now)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteVOTestExample.build()
								.atSchemaLocation(null)
								.requestedQuoteCompletionDate(now)
								.validFor(null)
								.state(QuoteStateTypeVO.IN_PROGRESS)
								.quoteTotalPrice(null)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))),
				Arguments.of("A quote with an instant sync should have been created.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.instantSyncQuote(true)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteVOTestExample.build()
								.atSchemaLocation(null)
								.instantSyncQuote(true)
								.validFor(null)
								.state(QuoteStateTypeVO.IN_PROGRESS)
								.quoteTotalPrice(null)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))),
				Arguments.of("A quote with an authorization should have been created.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.authorization(List.of(AuthorizationVOTestExample.build().atSchemaLocation(null)))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteVOTestExample.build()
								.atSchemaLocation(null)
								.authorization(List.of(AuthorizationVOTestExample.build().atSchemaLocation(null)))
								.validFor(null)
								.state(QuoteStateTypeVO.IN_PROGRESS)
								.quoteTotalPrice(null)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))),
				Arguments.of("A quote with a contactMedium should have been created.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.contactMedium(List.of(ContactMediumVOTestExample.build()
										.atSchemaLocation(null)
										.characteristic(MediumCharacteristicVOTestExample.build().atSchemaLocation(null))))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteVOTestExample.build()
								.atSchemaLocation(null)
								.contactMedium(List.of(ContactMediumVOTestExample.build()
										.atSchemaLocation(null)
										.characteristic(MediumCharacteristicVOTestExample.build().atSchemaLocation(null))))
								.validFor(null)
								.state(QuoteStateTypeVO.IN_PROGRESS)
								.quoteTotalPrice(null)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))),
				Arguments.of("A quote with a note should have been created.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.note(List.of(NoteVOTestExample.build().atSchemaLocation(null)))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteVOTestExample.build()
								.atSchemaLocation(null)
								.note(List.of(NoteVOTestExample.build().atSchemaLocation(null)))
								.validFor(null)
								.state(QuoteStateTypeVO.IN_PROGRESS)
								.quoteTotalPrice(null)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))),
				Arguments.of("A quote with a quotePrice should have been created.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteVOTestExample.build()
								.atSchemaLocation(null)
								.validFor(null)
								.state(QuoteStateTypeVO.IN_PROGRESS)
								.quoteTotalPrice(null)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))),
				Arguments.of("A quote with a quoteItem.note should have been created.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.note(List.of(NoteVOTestExample.build().atSchemaLocation(null)))
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteVOTestExample.build()
								.atSchemaLocation(null)
								.validFor(null)
								.state(QuoteStateTypeVO.IN_PROGRESS)
								.quoteTotalPrice(null)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.note(List.of(NoteVOTestExample.build().atSchemaLocation(null)))
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))),
				Arguments.of("A quote with a quoteItem.quoteItem should have been created.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.quoteItem(List.of(QuoteItemVOTestExample.build()
												.state(QuoteItemState.IN_PROGRESS.getValue())
												.atSchemaLocation(null)
												.product(null)
												.productOffering(null)
												.productOfferingQualificationItem(null)))
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteVOTestExample.build()
								.atSchemaLocation(null)
								.validFor(null)
								.state(QuoteStateTypeVO.IN_PROGRESS)
								.quoteTotalPrice(null)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.quoteItem(List.of(QuoteItemVOTestExample.build()
												.state(QuoteItemState.IN_PROGRESS.getValue())
												.atSchemaLocation(null)
												.product(null)
												.productOffering(null)
												.productOfferingQualificationItem(null))).product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))),
				Arguments.of("A quote with a quoteItem.authorization should have been created.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.quoteItemAuthorization(List.of(AuthorizationVOTestExample.build().atSchemaLocation(null)))
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteVOTestExample.build()
								.atSchemaLocation(null)
								.validFor(null)
								.state(QuoteStateTypeVO.IN_PROGRESS)
								.quoteTotalPrice(null)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.quoteItemAuthorization(List.of(AuthorizationVOTestExample.build().atSchemaLocation(null)))
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))),
				Arguments.of("A quote with a quoteItem.quoteItemPrice should have been created.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.quoteItemPrice(List.of(QuotePriceVOTestExample.build()
												.atSchemaLocation(null)
												.price(null)
												.productOfferingPrice(null)))
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteVOTestExample.build()
								.atSchemaLocation(null)
								.validFor(null)
								.state(QuoteStateTypeVO.IN_PROGRESS)
								.quoteTotalPrice(null)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.quoteItemPrice(List.of(QuotePriceVOTestExample.build()
												.atSchemaLocation(null)
												.price(null)
												.productOfferingPrice(null)))
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))),
				Arguments.of("A quote with a quoteItem.quoteItemPrice and a price should have been created.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.quoteItemPrice(List.of(QuotePriceVOTestExample.build()
												.atSchemaLocation(null)
												.price(PriceVOTestExample.build().atSchemaLocation(null))
												.productOfferingPrice(null)))
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteVOTestExample.build()
								.atSchemaLocation(null)
								.validFor(null)
								.state(QuoteStateTypeVO.IN_PROGRESS)
								.quoteTotalPrice(null)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.quoteItemPrice(List.of(QuotePriceVOTestExample.build()
												.atSchemaLocation(null)
												.price(PriceVOTestExample.build().atSchemaLocation(null))
												.productOfferingPrice(null)))
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))),
				Arguments.of("A quote with a quoteItem.quoteItemPrice and a price alteration should have been created.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.quoteItemPrice(List.of(QuotePriceVOTestExample.build()
												.atSchemaLocation(null)
												.price(null)
												.priceAlteration(List.of(PriceAlterationVOTestExample.build()
														.atSchemaLocation(null)
														.price(null)
														.productOfferingPrice(null)))
												.productOfferingPrice(null)))
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteVOTestExample.build()
								.atSchemaLocation(null)
								.validFor(null)
								.state(QuoteStateTypeVO.IN_PROGRESS)
								.quoteTotalPrice(null)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.quoteItemPrice(List.of(QuotePriceVOTestExample.build()
												.atSchemaLocation(null)
												.price(null)
												.priceAlteration(List.of(PriceAlterationVOTestExample.build()
														.atSchemaLocation(null)
														.price(null)
														.productOfferingPrice(null)))
												.productOfferingPrice(null)))
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))));

	}

	@ParameterizedTest
	@MethodSource("provideInvalidQuotes")
	public void createQuote400(String message, QuoteCreateVO invalidCreateVO) throws Exception {
		this.message = message;
		this.quoteCreateVO = invalidCreateVO;
		createQuote400();
	}

	@Override
	public void createQuote400() throws Exception {
		HttpResponse<QuoteVO> creationResponse = callAndCatch(
				() -> quoteApiTestClient.createQuote(null, quoteCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidQuotes() {
		return Stream.of(
				Arguments.of("A quote with invalid related parties should not be created.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.relatedParty(List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null)))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))
				),
				Arguments.of("A quote with invalid agreements should not be created.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.agreement(List.of(AgreementRefVOTestExample.build().atSchemaLocation(null)))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))
				),
				Arguments.of("A quote with invalid authorization should not be created.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.authorization(List.of(AuthorizationVOTestExample.build()
										.atSchemaLocation(null)
										.approver(List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null)))))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))
				),
				Arguments.of("A quote with invalid billingAccount should not be created.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.billingAccount(List.of(BillingAccountRefVOTestExample.build().atSchemaLocation(null)))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))
				),
				Arguments.of("A quote with invalid productOfferingQualification should not be created.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.productOfferingQualification(List.of(ProductOfferingQualificationRefVOTestExample.build()
										.atSchemaLocation(null)))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))
				),
				Arguments.of("A quote without a quoteItem should not be created.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.quoteItem(List.of())
				),
				Arguments.of("A quote with invalid quoteItem.appointment should not be created.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.productOfferingQualification(List.of(ProductOfferingQualificationRefVOTestExample.build()
										.atSchemaLocation(null)))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.appointment(List.of(AppointmentRefVOTestExample.build().atSchemaLocation(null)))
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))
				),
				Arguments.of("A quote with invalid quoteItem.product should not be created.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.productOfferingQualification(List.of(ProductOfferingQualificationRefVOTestExample.build()
										.atSchemaLocation(null)))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(ProductRefOrValueVOTestExample.build().atSchemaLocation(null))
										.productOffering(null)
										.productOfferingQualificationItem(null)))
				),
				Arguments.of("A quote with invalid quoteItem.productOffering should not be created.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.productOfferingQualification(List.of(ProductOfferingQualificationRefVOTestExample.build()
										.atSchemaLocation(null)))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(ProductOfferingRefVOTestExample.build().atSchemaLocation(null))
										.productOfferingQualificationItem(null)))
				),
				Arguments.of("A quote with invalid quoteItem.productOfferingQualificationItem should not be created.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.productOfferingQualification(List.of(ProductOfferingQualificationRefVOTestExample.build()
										.atSchemaLocation(null)))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(ProductOfferingQualificationItemRefVOTestExample.build()
												.atSchemaLocation(null))))
				),
				Arguments.of("A quote with invalid quoteItem.authorization should not be created.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.productOfferingQualification(List.of(ProductOfferingQualificationRefVOTestExample.build()
										.atSchemaLocation(null)))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.quoteItemAuthorization(List.of(AuthorizationVOTestExample.build()
												.atSchemaLocation(null)
												.approver(List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null)))))
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))
				),
				Arguments.of("A quote with invalid quoteItem.quoteItemPrice should not be created.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.productOfferingQualification(List.of(ProductOfferingQualificationRefVOTestExample.build()
										.atSchemaLocation(null)))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.quoteItemPrice(List.of(QuotePriceVOTestExample.build()
												.atSchemaLocation(null)
												.productOfferingPrice(ProductOfferingPriceRefVOTestExample.build().atSchemaLocation(null))))
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))
				),
				Arguments.of("A quote with invalid quoteItem.relatedParty should not be created.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.productOfferingQualification(List.of(ProductOfferingQualificationRefVOTestExample.build()
										.atSchemaLocation(null)))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.relatedParty(List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null)))
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))
				),
				Arguments.of("A quote with invalid quoteItem.quoteItem should not be created.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.productOfferingQualification(List.of(ProductOfferingQualificationRefVOTestExample.build()
										.atSchemaLocation(null)))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.quoteItem(List.of(QuoteItemVOTestExample.build()
												.state(QuoteItemState.IN_PROGRESS.getValue())
												.atSchemaLocation(null)
												.relatedParty(List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null)))
												.product(null)
												.productOffering(null)
												.productOfferingQualificationItem(null)))
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))
				)
		);
	}


	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createQuote401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createQuote403() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void createQuote405() throws Exception {
	}

	@Disabled("No implicit creation, impossible state.")
	@Test
	@Override
	public void createQuote409() throws Exception {

	}

	@Override
	public void createQuote500() throws Exception {

	}

	@Test
	@Override
	public void deleteQuote204() throws Exception {
		QuoteCreateVO emptyCreate = QuoteCreateVOTestExample.build()
				.atSchemaLocation(null)
				.quoteItem(List.of(QuoteItemVOTestExample.build()
						.state(QuoteItemState.IN_PROGRESS.getValue())
						.atSchemaLocation(null)
						.product(null)
						.productOffering(null)
						.productOfferingQualificationItem(null)));

		HttpResponse<QuoteVO> createResponse = quoteApiTestClient.createQuote(null, emptyCreate);
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The quote should have been created first.");

		String quoteId = createResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> quoteApiTestClient.deleteQuote(null, quoteId)).getStatus(),
				"The quote should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> quoteApiTestClient.retrieveQuote(null, quoteId, null)).status(),
				"The quote should not exist anymore.");
	}

	@Disabled("400 is impossible to happen on deletion with the current implementation.")
	@Test
	@Override
	public void deleteQuote400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteQuote401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteQuote403() throws Exception {

	}

	@Test
	@Override
	public void deleteQuote404() throws Exception {
		HttpResponse<?> notFoundResponse = callAndCatch(
				() -> quoteApiTestClient.deleteQuote(null, "urn:ngsi-ld:quote:no-pop"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such quote should exist.");

		Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		notFoundResponse = callAndCatch(() -> quoteApiTestClient.deleteQuote(null, "invalid-id"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such quote should exist.");

		optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void deleteQuote405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void deleteQuote409() throws Exception {

	}

	@Override
	public void deleteQuote500() throws Exception {

	}

	@Test
	@Override
	public void listQuote200() throws Exception {
		Instant now = Instant.now();
		when(clock.instant()).thenReturn(now);
		List<QuoteVO> expectedQuotes = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			QuoteCreateVO quoteCreateVO = QuoteCreateVOTestExample.build()
					.atSchemaLocation(null)
					.quoteItem(List.of(QuoteItemVOTestExample.build()
							.state(QuoteItemState.IN_PROGRESS.getValue())
							.atSchemaLocation(null)
							.product(null)
							.productOffering(null)
							.productOfferingQualificationItem(null)));

			String id = quoteApiTestClient.createQuote(null, quoteCreateVO)
					.body().getId();
			QuoteVO quoteVO = QuoteVOTestExample.build()
					.id(id)
					.href(id)
					.atSchemaLocation(null)
					.quoteDate(now)
					.validFor(null)
					.state(QuoteStateTypeVO.IN_PROGRESS)
					.quoteTotalPrice(null)
					.quoteItem(List.of(QuoteItemVOTestExample.build()
							.state(QuoteItemState.IN_PROGRESS.getValue())
							.atSchemaLocation(null)
							.product(null)
							.productOffering(null)
							.productOfferingQualificationItem(null)));
			expectedQuotes.add(quoteVO);
		}

		HttpResponse<List<QuoteVO>> quoteResponse = callAndCatch(
				() -> quoteApiTestClient.listQuote(null, null, null, null));

		assertEquals(HttpStatus.OK, quoteResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedQuotes.size(), quoteResponse.getBody().get().size(),
				"All quote should have been returned.");
		List<QuoteVO> retrievedQuotes = quoteResponse.getBody().get();

		Map<String, QuoteVO> retrievedMap = retrievedQuotes.stream()
				.collect(Collectors.toMap(quote -> quote.getId(),
						quote -> quote));

		expectedQuotes.stream()
				.forEach(
						expectedQuote -> assertTrue(
								retrievedMap.containsKey(expectedQuote.getId()),
								String.format("All created quote should be returned - Missing: %s.",
										expectedQuote,
										retrievedQuotes)));
		expectedQuotes.stream().forEach(
				expectedQuote -> assertEquals(expectedQuote,
						retrievedMap.get(expectedQuote.getId()),
						"The correct quote should be retrieved."));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<QuoteVO>> firstPartResponse = callAndCatch(
				() -> quoteApiTestClient.listQuote(null, null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<QuoteVO>> secondPartResponse = callAndCatch(
				() -> quoteApiTestClient.listQuote(null, null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returend.");

		retrievedQuotes.clear();
		retrievedQuotes.addAll(firstPartResponse.body());
		retrievedQuotes.addAll(secondPartResponse.body());
		expectedQuotes.stream()
				.forEach(
						expectedQuote -> assertTrue(
								retrievedMap.containsKey(expectedQuote.getId()),
								String.format("All created quote should be returned - Missing: %s.",
										expectedQuote)));
		expectedQuotes.stream().forEach(
				expectedQuote -> assertEquals(expectedQuote,
						retrievedMap.get(expectedQuote.getId()),
						"The correct quote should be retrieved."));
	}

	@Test
	@Override
	public void listQuote400() throws Exception {
		HttpResponse<List<QuoteVO>> badRequestResponse = callAndCatch(
				() -> quoteApiTestClient.listQuote(null, null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(() -> quoteApiTestClient.listQuote(null, null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative limits are impossible.");
		optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listQuote401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listQuote403() throws Exception {

	}

	@Disabled("Not found is not possible here, will be answered with an empty list instead.")
	@Test
	@Override
	public void listQuote404() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void listQuote405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void listQuote409() throws Exception {

	}

	@Override
	public void listQuote500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideQuoteUpdates")
	public void patchQuote200(String message, QuoteCreateVO quoteCreateVO, QuoteUpdateVO quoteUpdateVO,
							  QuoteVO expectedQuote) throws Exception {
		this.message = message;
		this.quoteCreateVO = quoteCreateVO;
		this.quoteUpdateVO = quoteUpdateVO;
		this.expectedQuote = expectedQuote;
		patchQuote200();
	}

	@Override
	public void patchQuote200() throws Exception {

		Instant now = Instant.now();
		when(clock.instant()).thenReturn(now);

		HttpResponse<QuoteVO> createResponse = callAndCatch(
				() -> quoteApiTestClient.createQuote(null, quoteCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The quote should have been created first.");

		String quoteId = createResponse.body().getId();

		HttpResponse<QuoteVO> updateResponse = callAndCatch(
				() -> quoteApiTestClient.patchQuote(null, quoteId, quoteUpdateVO));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

		QuoteVO updatedQuote = updateResponse.body();
		expectedQuote.href(quoteId).id(quoteId).quoteDate(now);

		assertEquals(expectedQuote, updatedQuote, message);
	}

	private static Stream<Arguments> provideQuoteUpdates() {
		Instant now = Instant.now();
		Instant next = Instant.now().plus(Duration.of(10, ChronoUnit.DAYS));
		return Stream.of(
				Arguments.of("A quoteItem should have been updated.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteUpdateVOTestExample.build()
								.atSchemaLocation(null)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.REJECTED.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteVOTestExample.build()
								.atSchemaLocation(null)
								.state(QuoteStateTypeVO.IN_PROGRESS)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.REJECTED.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))),
				Arguments.of("The expectedFulfillmentStartDate should have been updated.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.expectedFulfillmentStartDate(now)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteUpdateVOTestExample.build()
								.atSchemaLocation(null)
								.expectedFulfillmentStartDate(next)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteVOTestExample.build()
								.atSchemaLocation(null)
								.expectedFulfillmentStartDate(next)
								.state(QuoteStateTypeVO.IN_PROGRESS)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))),
				Arguments.of("The requestedQuoteCompletionDate should have been updated.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.requestedQuoteCompletionDate(now)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteUpdateVOTestExample.build()
								.atSchemaLocation(null)
								.requestedQuoteCompletionDate(next)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteVOTestExample.build()
								.atSchemaLocation(null)
								.requestedQuoteCompletionDate(next)
								.state(QuoteStateTypeVO.IN_PROGRESS)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))),
				Arguments.of("The instant sync should have been updated.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.instantSyncQuote(true)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteUpdateVOTestExample.build()
								.atSchemaLocation(null)
								.instantSyncQuote(false)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteVOTestExample.build()
								.atSchemaLocation(null)
								.instantSyncQuote(false)
								.state(QuoteStateTypeVO.IN_PROGRESS)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))),
				Arguments.of("The authorization should have been updated.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.authorization(List.of(AuthorizationVOTestExample.build().atSchemaLocation(null)))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteUpdateVOTestExample.build()
								.atSchemaLocation(null)
								.authorization(List.of(AuthorizationVOTestExample.build()
										.atSchemaLocation(null)
										.name("MyNewName")))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteVOTestExample.build()
								.atSchemaLocation(null)
								.authorization(List.of(AuthorizationVOTestExample.build()
										.atSchemaLocation(null)
										.name("MyNewName")))
								.state(QuoteStateTypeVO.IN_PROGRESS)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))),
				Arguments.of("The contactMedium should have been updated.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.contactMedium(List.of(ContactMediumVOTestExample.build()
										.atSchemaLocation(null)
										.characteristic(MediumCharacteristicVOTestExample.build().atSchemaLocation(null))))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteUpdateVOTestExample.build()
								.atSchemaLocation(null)
								.contactMedium(List.of(ContactMediumVOTestExample.build()
										.atSchemaLocation(null)
										.characteristic(MediumCharacteristicVOTestExample.build()
												.atSchemaLocation(null)
												.city("Dresden"))))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteVOTestExample.build()
								.atSchemaLocation(null)
								.contactMedium(List.of(ContactMediumVOTestExample.build()
										.atSchemaLocation(null)
										.characteristic(MediumCharacteristicVOTestExample.build()
												.atSchemaLocation(null)
												.city("Dresden"))))
								.state(QuoteStateTypeVO.IN_PROGRESS)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))),
				Arguments.of("The note should have been updated.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.note(List.of(NoteVOTestExample.build().atSchemaLocation(null)))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteUpdateVOTestExample.build()
								.atSchemaLocation(null)
								.note(List.of(NoteVOTestExample.build()
										.atSchemaLocation(null)
										.text("My new text.")))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteVOTestExample.build()
								.atSchemaLocation(null)
								.note(List.of(NoteVOTestExample.build()
										.atSchemaLocation(null)
										.text("My new text.")))
								.state(QuoteStateTypeVO.IN_PROGRESS)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))),
				Arguments.of("The quoteItem.note should have been updated.",
						QuoteCreateVOTestExample.build()
								.atSchemaLocation(null)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.note(List.of(NoteVOTestExample.build().atSchemaLocation(null)))
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteUpdateVOTestExample.build()
								.atSchemaLocation(null)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.note(List.of(NoteVOTestExample.build()
												.atSchemaLocation(null)
												.text("My new text")))
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null))),
						QuoteVOTestExample.build()
								.atSchemaLocation(null)
								.state(QuoteStateTypeVO.IN_PROGRESS)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.note(List.of(NoteVOTestExample.build()
												.atSchemaLocation(null)
												.text("My new text"))).product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))));
	}

	@ParameterizedTest
	@MethodSource("provideInvalidUpdates")
	public void patchQuote400(String message, QuoteUpdateVO invalidUpdateVO) throws Exception {
		this.message = message;
		this.quoteUpdateVO = invalidUpdateVO;
		patchQuote400();
	}

	@Override
	public void patchQuote400() throws Exception {
		//first create
		QuoteCreateVO quoteCreateVO = QuoteCreateVOTestExample.build()
				.atSchemaLocation(null)
				.quoteItem(List.of(QuoteItemVOTestExample.build()
						.state(QuoteItemState.IN_PROGRESS.getValue())
						.atSchemaLocation(null)
						.product(null)
						.productOffering(null)
						.productOfferingQualificationItem(null)));

		HttpResponse<QuoteVO> createResponse = callAndCatch(
				() -> quoteApiTestClient.createQuote(null, quoteCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The product order should have been created first.");

		String productId = createResponse.body().getId();

		HttpResponse<QuoteVO> updateResponse = callAndCatch(
				() -> quoteApiTestClient.patchQuote(null, productId, quoteUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

		Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
	}

	private static Stream<Arguments> provideInvalidUpdates() {
		return Stream.of(
				Arguments.of("A quote with invalid related parties should not be updated.",
						QuoteUpdateVOTestExample.build()
								.atSchemaLocation(null)
								.relatedParty(List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null)))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))
				),
				Arguments.of("A quote with invalid agreements should not be updated.",
						QuoteUpdateVOTestExample.build()
								.atSchemaLocation(null)
								.agreement(List.of(AgreementRefVOTestExample.build().atSchemaLocation(null)))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))
				),
				Arguments.of("A quote with invalid authorization should not be updated.",
						QuoteUpdateVOTestExample.build()
								.atSchemaLocation(null)
								.authorization(List.of(AuthorizationVOTestExample.build()
										.atSchemaLocation(null)
										.approver(List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null)))))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))
				),
				Arguments.of("A quote with invalid billingAccount should not be updated.",
						QuoteUpdateVOTestExample.build()
								.atSchemaLocation(null)
								.billingAccount(List.of(BillingAccountRefVOTestExample.build().atSchemaLocation(null)))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))
				),
				Arguments.of("A quote with invalid productOfferingQualification should not be updated.",
						QuoteUpdateVOTestExample.build()
								.atSchemaLocation(null)
								.productOfferingQualification(List.of(ProductOfferingQualificationRefVOTestExample.build()
										.atSchemaLocation(null)))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))
				),
				Arguments.of("A quote with invalid quoteItem.appointment should not be updated.",
						QuoteUpdateVOTestExample.build()
								.atSchemaLocation(null)
								.productOfferingQualification(List.of(ProductOfferingQualificationRefVOTestExample.build()
										.atSchemaLocation(null)))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.appointment(List.of(AppointmentRefVOTestExample.build().atSchemaLocation(null)))
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))
				),
				Arguments.of("A quote with invalid quoteItem.product should not be updated.",
						QuoteUpdateVOTestExample.build()
								.atSchemaLocation(null)
								.productOfferingQualification(List.of(ProductOfferingQualificationRefVOTestExample.build()
										.atSchemaLocation(null)))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(ProductRefOrValueVOTestExample.build().atSchemaLocation(null))
										.productOffering(null)
										.productOfferingQualificationItem(null)))
				),
				Arguments.of("A quote with invalid quoteItem.productOffering should not be updated.",
						QuoteUpdateVOTestExample.build()
								.atSchemaLocation(null)
								.productOfferingQualification(List.of(ProductOfferingQualificationRefVOTestExample.build()
										.atSchemaLocation(null)))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(ProductOfferingRefVOTestExample.build().atSchemaLocation(null))
										.productOfferingQualificationItem(null)))
				),
				Arguments.of("A quote with invalid quoteItem.productOfferingQualificationItem should not be updated.",
						QuoteUpdateVOTestExample.build()
								.atSchemaLocation(null)
								.productOfferingQualification(List.of(ProductOfferingQualificationRefVOTestExample.build()
										.atSchemaLocation(null)))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(ProductOfferingQualificationItemRefVOTestExample.build()
												.atSchemaLocation(null))))
				),
				Arguments.of("A quote with invalid quoteItem.authorization should not be updated.",
						QuoteUpdateVOTestExample.build()
								.atSchemaLocation(null)
								.productOfferingQualification(List.of(ProductOfferingQualificationRefVOTestExample.build()
										.atSchemaLocation(null)))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.quoteItemAuthorization(List.of(AuthorizationVOTestExample.build()
												.atSchemaLocation(null)
												.approver(List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null)))))
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))
				),
				Arguments.of("A quote with invalid quoteItem.quoteItemPrice should not be updated.",
						QuoteUpdateVOTestExample.build()
								.atSchemaLocation(null)
								.productOfferingQualification(List.of(ProductOfferingQualificationRefVOTestExample.build()
										.atSchemaLocation(null)))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.quoteItemPrice(List.of(QuotePriceVOTestExample.build()
												.atSchemaLocation(null)
												.productOfferingPrice(ProductOfferingPriceRefVOTestExample.build().atSchemaLocation(null))))
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))
				),
				Arguments.of("A quote with invalid quoteItem.relatedParty should not be updated.",
						QuoteUpdateVOTestExample.build()
								.atSchemaLocation(null)
								.productOfferingQualification(List.of(ProductOfferingQualificationRefVOTestExample.build()
										.atSchemaLocation(null)))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.relatedParty(List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null)))
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))
				),
				Arguments.of("A quote with invalid quoteItem.quoteItem should not be updated.",
						QuoteUpdateVOTestExample.build()
								.atSchemaLocation(null)
								.productOfferingQualification(List.of(ProductOfferingQualificationRefVOTestExample.build()
										.atSchemaLocation(null)))
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.quoteItem(List.of(QuoteItemVOTestExample.build()
												.state(QuoteItemState.IN_PROGRESS.getValue())
												.atSchemaLocation(null)
												.relatedParty(List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null)))
												.product(null)
												.productOffering(null)
												.productOfferingQualificationItem(null)))
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))
				)
		);
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchQuote401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchQuote403() throws Exception {

	}

	@Test
	@Override
	public void patchQuote404() throws Exception {
		QuoteUpdateVO quoteUpdateVO = QuoteUpdateVOTestExample.build()
				.atSchemaLocation(null)
				.quoteItem(List.of(QuoteItemVOTestExample.build()
						.state(QuoteItemState.REJECTED.getValue())
						.atSchemaLocation(null)
						.product(null)
						.productOffering(null)
						.productOfferingQualificationItem(null)));
		assertEquals(
				HttpStatus.NOT_FOUND,
				callAndCatch(
						() -> quoteApiTestClient.patchQuote(null, "urn:ngsi-ld:quote:not-existent",
								quoteUpdateVO)).getStatus(),
				"Non existent quotes should not be updated.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void patchQuote405() throws Exception {

	}

	@Override
	public void patchQuote409() throws Exception {
	}

	@Override
	public void patchQuote500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideFieldParameters")
	public void retrieveQuote200(String message, String fields, QuoteVO expectedQuote)
			throws Exception {
		this.fieldsParameter = fields;
		this.message = message;
		this.expectedQuote = expectedQuote;
		retrieveQuote200();
	}

	@Override
	public void retrieveQuote200() throws Exception {

		Instant now = Instant.now();
		when(clock.instant()).thenReturn(now);

		QuoteCreateVO quoteCreateVO = QuoteCreateVOTestExample.build()
				.atSchemaLocation(null)
				.quoteItem(List.of(QuoteItemVOTestExample.build()
						.state(QuoteItemState.IN_PROGRESS.getValue())
						.atSchemaLocation(null)
						.product(null)
						.productOffering(null)
						.productOfferingQualificationItem(null)));
		HttpResponse<QuoteVO> createResponse = callAndCatch(
				() -> quoteApiTestClient.createQuote(null, quoteCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), message);
		String id = createResponse.body().getId();

		expectedQuote
				.id(id)
				.href(id);

		if (fieldsParameter == null || fieldsParameter == "quoteDate" || fieldsParameter.isEmpty()) {
			expectedQuote.quoteDate(now);
		}

		//then retrieve
		HttpResponse<QuoteVO> retrievedQuote = callAndCatch(
				() -> quoteApiTestClient.retrieveQuote(null, id, fieldsParameter));
		assertEquals(HttpStatus.OK, retrievedQuote.getStatus(), message);
		assertEquals(expectedQuote, retrievedQuote.body(), message);
	}

	private static Stream<Arguments> provideFieldParameters() {
		return Stream.of(
				Arguments.of("Without a fields parameter everything should be returned.", null,
						QuoteVOTestExample.build()
								.atSchemaLocation(null)
								.validFor(null)
								.state(QuoteStateTypeVO.IN_PROGRESS)
								.quoteTotalPrice(null)
								.quoteItem(List.of(QuoteItemVOTestExample.build()
										.state(QuoteItemState.IN_PROGRESS.getValue())
										.atSchemaLocation(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)))),
				Arguments.of("Only quoteDate and the mandatory parameters should have been included.", "quoteDate",
						QuoteVOTestExample.build()
								.atSchemaLocation(null)
								.category(null)
								.description(null)
								.externalId(null)
								.instantSyncQuote(null)
								.version(null)
								.agreement(null)
								.authorization(null)
								.billingAccount(null)
								.contactMedium(null)
								.note(null)
								.productOfferingQualification(null)
								.relatedParty(null)
								.atType(null)
								.atBaseType(null)
								.validFor(null)
								.state(null)
								.quoteTotalPrice(null)
								.quoteItem(List.of())
				),
				Arguments.of("Only the mandatory parameters should have been included when a non-existent field was requested.",
						"nothingToSeeHere",
						QuoteVOTestExample.build()
								.atSchemaLocation(null)
								.category(null)
								.description(null)
								.externalId(null)
								.instantSyncQuote(null)
								.version(null)
								.agreement(null)
								.authorization(null)
								.billingAccount(null)
								.contactMedium(null)
								.note(null)
								.productOfferingQualification(null)
								.relatedParty(null)
								.atType(null)
								.atBaseType(null)
								.validFor(null)
								.state(null)
								.quoteTotalPrice(null)
								.quoteDate(null)
								.quoteItem(List.of())
				),
				Arguments.of("Only description, externalId and the mandatory parameters should have been included.",
						"description,externalId",
						QuoteVOTestExample.build()
								.atSchemaLocation(null)
								.category(null)
								.instantSyncQuote(null)
								.version(null)
								.agreement(null)
								.authorization(null)
								.billingAccount(null)
								.contactMedium(null)
								.note(null)
								.productOfferingQualification(null)
								.relatedParty(null)
								.atType(null)
								.atBaseType(null)
								.validFor(null)
								.state(null)
								.quoteTotalPrice(null)
								.quoteDate(null)
								.quoteItem(List.of())
				)
		);
	}

	@Disabled("400 cannot happen, only 404")
	@Test
	@Override
	public void retrieveQuote400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveQuote401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveQuote403() throws Exception {

	}

	@Test
	@Override
	public void retrieveQuote404() throws Exception {
		HttpResponse<QuoteVO> response = callAndCatch(
				() -> quoteApiTestClient.retrieveQuote(null, "urn:ngsi-ld:product-function:non-existent",
						null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such product-catalog should exist.");

		Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void retrieveQuote405() throws Exception {

	}

	@Disabled("Conflict not possible on retrieval")
	@Test
	@Override
	public void retrieveQuote409() throws Exception {

	}

	@Override
	public void retrieveQuote500() throws Exception {

	}

	@Override
	protected String getEntityType() {
		return Quote.TYPE_QUOTE;
	}
}


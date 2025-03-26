package org.fiware.tmforum.quote.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.quote.api.QuoteApi;
import org.fiware.quote.model.QuoteCreateVO;
import org.fiware.quote.model.QuoteUpdateVO;
import org.fiware.quote.model.QuoteVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.product.PriceAlteration;
import org.fiware.tmforum.quote.TMForumMapper;
import org.fiware.tmforum.quote.domain.Authorization;
import org.fiware.tmforum.quote.domain.Quote;
import org.fiware.tmforum.quote.domain.QuoteItem;
import org.fiware.tmforum.quote.domain.QuotePrice;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.*;

@Slf4j
@Controller("${general.basepath:/}")
public class QuoteApiController extends AbstractApiController<Quote> implements QuoteApi {

	private final TMForumMapper tmForumMapper;
	private final Clock clock;

	public QuoteApiController(QueryParser queryParser, ReferenceValidationService validationService,
							  TmForumRepository productCatalogRepository, TMForumMapper tmForumMapper, Clock clock, TMForumEventHandler eventHandler) {
		super(queryParser, validationService, productCatalogRepository, eventHandler);
		this.tmForumMapper = tmForumMapper;
		this.clock = clock;
	}


	@Override
	public Mono<HttpResponse<QuoteVO>> createQuote(@NonNull QuoteCreateVO quoteCreateVO) {
		Quote quote = tmForumMapper.map(
				tmForumMapper.map(quoteCreateVO,
						IdHelper.toNgsiLd(UUID.randomUUID().toString(), Quote.TYPE_QUOTE)));
		quote.setQuoteDate(clock.instant());

		if (quote.getQuoteItem() != null || quote.getQuoteItem().isEmpty()) {
			throw new TmForumException("Quotes need at least one QuoteItem.", TmForumExceptionReason.INVALID_DATA);
		}

		return create(getCheckingMono(quote), Quote.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}


	private Mono<Quote> getCheckingMono(Quote quote) {
		List<List<? extends ReferencedEntity>> references = new ArrayList<>();
		references.add(quote.getAgreement());
		references.add(quote.getBillingAccount());
		references.add(quote.getProductOfferingQualification());
		references.add(quote.getRelatedParty());

		if (quote.getAuthorization() != null && !quote.getAuthorization().isEmpty()) {
			references.add(quote.getAuthorization()
					.stream()
					.map(Authorization::getApprover)
					.filter(Objects::nonNull)
					.flatMap(List::stream)
					.toList());
		}
		quote.getQuoteItem().stream().map(this::getReferencesForQuoteItem).forEach(references::addAll);

		Optional.ofNullable(quote.getQuoteTotalPrice()).map(this::getReferencesForQuotePrice).ifPresent(references::addAll);

		return getCheckingMono(quote, references)
				.onErrorMap(throwable ->
						new TmForumException(
								String.format("Was not able to create quote %s", quote.getId()),
								throwable,
								TmForumExceptionReason.INVALID_RELATIONSHIP));
	}

	private List<List<? extends ReferencedEntity>> getReferencesForQuotePrice(List<QuotePrice> quotePrice) {
		List<List<? extends ReferencedEntity>> references = new ArrayList<>();

		quotePrice
				.forEach(qip -> {
					Optional.ofNullable(qip.getProductOfferingPrice()).map(List::of).ifPresent(references::add);
					if (qip.getPriceAlteration() != null) {
						references.add(qip.getPriceAlteration()
								.stream()
								.map(PriceAlteration::getProductOfferingPrice)
								.filter(Objects::nonNull)
								.toList());
					}
				});
		return references;
	}

	private List<List<? extends ReferencedEntity>> getReferencesForQuoteItem(QuoteItem quoteItem) {
		List<List<? extends ReferencedEntity>> references = new ArrayList<>();

		Optional.ofNullable(quoteItem.getAppointment()).ifPresent(references::add);
		Optional.ofNullable(quoteItem.getRelatedParty()).ifPresent(references::add);
		Optional.ofNullable(quoteItem.getProductOffering()).map(List::of).ifPresent(references::add);
		Optional.ofNullable(quoteItem.getProductOfferingQualificationItem()).map(List::of).ifPresent(references::add);
		if (quoteItem.getQuoteItem() != null) {
			quoteItem.getQuoteItem().stream().map(this::getReferencesForQuoteItem).forEach(references::addAll);
		}
		if (quoteItem.getQuoteItemAuthorization() != null) {
			quoteItem.getQuoteItemAuthorization().stream()
					.map(Authorization::getApprover)
					.filter(Objects::nonNull)
					.forEach(references::add);
		}

		Optional.ofNullable(quoteItem.getQuoteItemPrice()).map(this::getReferencesForQuotePrice).ifPresent(references::addAll);

		return references;
	}

	@Override
	public Mono<HttpResponse<Object>> deleteQuote(@NonNull String id) {
		return delete(id);
	}

	@Override
	public Mono<HttpResponse<List<QuoteVO>>> listQuote(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
		return list(offset, limit, Quote.TYPE_QUOTE, Quote.class)
				.map(quoteStream -> quoteStream
						.map(tmForumMapper::map)
						.toList())
				.switchIfEmpty(Mono.just(List.of()))
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<QuoteVO>> patchQuote(@NonNull String id, @NonNull QuoteUpdateVO quoteUpdateVO) {
		// non-ngsi-ld ids cannot exist.
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such quote cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
		}

		Quote quote = tmForumMapper.map(quoteUpdateVO, id);

		// the list is not allowed to be emptied
		if (Optional.ofNullable(quote.getQuoteItem()).map(List::isEmpty).orElse(false)) {
			quote.setQuoteItem(null);
		}

		return patch(id, quote, getCheckingMono(quote), Quote.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<QuoteVO>> retrieveQuote(@NonNull String id,
													 @Nullable String fields) {
		return retrieve(id, Quote.class)
				.switchIfEmpty(Mono.error(new TmForumException("No such quote exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}
}


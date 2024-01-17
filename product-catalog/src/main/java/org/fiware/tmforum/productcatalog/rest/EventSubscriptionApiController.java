package org.fiware.tmforum.productcatalog.rest;

import io.github.wistefan.mapping.EntityVOMapper;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.productcatalog.api.EventsSubscriptionApi;
import org.fiware.productcatalog.model.EventSubscriptionInputVO;
import org.fiware.productcatalog.model.EventSubscriptionVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractSubscriptionApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.product.Category;
import org.fiware.tmforum.product.ProductOffering;
import org.fiware.tmforum.product.ProductOfferingPrice;
import org.fiware.tmforum.product.ProductSpecification;
import org.fiware.tmforum.productcatalog.TMForumMapper;
import org.fiware.tmforum.productcatalog.domain.Catalog;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import static org.fiware.tmforum.common.notification.EventConstants.*;

@Slf4j
@Controller("${general.basepath:/}")
public class EventSubscriptionApiController extends AbstractSubscriptionApiController implements EventsSubscriptionApi {
    private final TMForumMapper tmForumMapper;
	private static final Map<String, String> EVENT_GROUP_TO_ENTITY_NAME_MAPPING = Map.ofEntries(
		entry(EVENT_GROUP_CATALOG, Catalog.TYPE_CATALOG),
		entry(EVENT_GROUP_CATEGORY, Category.TYPE_CATEGORY),
		entry(EVENT_GROUP_PRODUCT_OFFERING, ProductOffering.TYPE_PRODUCT_OFFERING),
		entry(EVENT_GROUP_PRODUCT_OFFERING_PRICE, ProductOfferingPrice.TYPE_PRODUCT_OFFERING_PRICE),
		entry(EVENT_GROUP_PRODUCT_SPECIFICATION, ProductSpecification.TYPE_PRODUCT_SPECIFICATION)
	);
    private static final List<String> EVENT_GROUPS = List.of(EVENT_GROUP_CATALOG, EVENT_GROUP_CATEGORY,
            EVENT_GROUP_PRODUCT_OFFERING, EVENT_GROUP_PRODUCT_OFFERING_PRICE, EVENT_GROUP_PRODUCT_SPECIFICATION);
    private static final Map<String, Class<?>> ENTITY_NAME_TO_ENTITY_CLASS_MAPPING = Map.ofEntries(
        entry(Catalog.TYPE_CATALOG, Catalog.class),
        entry(Category.TYPE_CATEGORY, Category.class),
        entry(ProductOffering.TYPE_PRODUCT_OFFERING, ProductOffering.class),
        entry(ProductOfferingPrice.TYPE_PRODUCT_OFFERING_PRICE, ProductOfferingPrice.class),
        entry(ProductSpecification.TYPE_PRODUCT_SPECIFICATION, ProductSpecification.class)
    );

    public EventSubscriptionApiController(QueryParser queryParser, ReferenceValidationService validationService,
                                          TmForumRepository repository, TMForumMapper tmForumMapper,
                                          EventHandler eventHandler, GeneralProperties generalProperties,
                                          EntityVOMapper entityVOMapper) {
        super(queryParser, validationService, repository, EVENT_GROUP_TO_ENTITY_NAME_MAPPING,
                ENTITY_NAME_TO_ENTITY_CLASS_MAPPING, eventHandler, generalProperties, entityVOMapper);
        this.tmForumMapper = tmForumMapper;
    }

    @Override
    public Mono<HttpResponse<EventSubscriptionVO>> registerListener(
            @NonNull EventSubscriptionInputVO eventSubscriptionInputVO) {
        TMForumSubscription subscription = buildSubscription(eventSubscriptionInputVO.getCallback(),
                eventSubscriptionInputVO.getQuery(), EVENT_GROUPS);

        return create(subscription)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);
    }

    @Override
    public Mono<HttpResponse<Object>> unregisterListener(@NonNull String id) {
        return delete(id);
    }
}

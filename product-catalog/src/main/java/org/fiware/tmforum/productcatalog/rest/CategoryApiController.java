package org.fiware.tmforum.productcatalog.rest;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.productcatalog.api.CategoryApi;
import org.fiware.productcatalog.model.CategoryCreateVO;
import org.fiware.productcatalog.model.CategoryUpdateVO;
import org.fiware.productcatalog.model.CategoryVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.productcatalog.TMForumMapper;
import org.fiware.tmforum.productcatalog.domain.Category;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class CategoryApiController extends AbstractApiController<Category> implements CategoryApi {

	private final TMForumMapper tmForumMapper;
	private final Clock clock;

	public CategoryApiController(ReferenceValidationService validationService,
			TmForumRepository productCatalogRepository,
			TMForumMapper tmForumMapper, Clock clock) {
		super(validationService, productCatalogRepository);
		this.tmForumMapper = tmForumMapper;
		this.clock = clock;
	}

	@Override
	public Mono<HttpResponse<CategoryVO>> createCategory(CategoryCreateVO categoryCreateVO) {
		Category category = tmForumMapper.map(tmForumMapper.map(categoryCreateVO,
				IdHelper.toNgsiLd(UUID.randomUUID().toString(), Category.TYPE_CATEGORY)));
		category.setLastUpdate(clock.instant());

		return create(getCheckingMono(category), Category.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}

	private Mono<Category> getCheckingMono(Category category) {
		List<List<? extends ReferencedEntity>> references = new ArrayList<>();
		Optional.ofNullable(category.getSubCategory()).ifPresent(references::add);
		Optional.ofNullable(category.getParentId()).ifPresent(sub -> references.add(List.of(sub)));
		Optional.ofNullable(category.getProductOffering()).ifPresent(references::add);

		return getCheckingMono(category, references)
				.onErrorMap(throwable -> new TmForumException(
						String.format("Was not able to create category %s", category.getId()), throwable,
						TmForumExceptionReason.INVALID_RELATIONSHIP));
	}

	@Override
	public Mono<HttpResponse<Object>> deleteCategory(String id) {
		return delete(id);
	}

	@Override
	public Mono<HttpResponse<List<CategoryVO>>> listCategory(@Nullable String fields, @Nullable Integer offset,
			@Nullable Integer limit) {
		return list(offset, limit, Category.TYPE_CATEGORY, Category.class)
				.map(categoryStream -> categoryStream.map(tmForumMapper::map).toList())
				.switchIfEmpty(Mono.just(List.of()))
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<CategoryVO>> patchCategory(String id, CategoryUpdateVO categoryUpdateVO) {

		// non-ngsi-ld ids cannot exist.
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such category cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
		}
		Category updatedCategory = tmForumMapper.map(tmForumMapper.map(categoryUpdateVO, id));
		updatedCategory.setLastUpdate(clock.instant());
		Mono<Category> checkingMono = getCheckingMono(updatedCategory);
		return patch(id, updatedCategory, checkingMono, Category.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<CategoryVO>> retrieveCategory(String id, @Nullable String fields) {
		return retrieve(id, Category.class)
				.switchIfEmpty(Mono.error(new TmForumException("No such catalog exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}
}

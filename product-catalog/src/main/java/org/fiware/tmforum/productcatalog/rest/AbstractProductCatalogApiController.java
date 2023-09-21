package org.fiware.tmforum.productcatalog.rest;

import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.product.ProductSpecificationCharacteristicValueUse;
import org.fiware.tmforum.product.ProductSpecificationRef;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class AbstractProductCatalogApiController<T> extends AbstractApiController<T> {

	public AbstractProductCatalogApiController(
			ReferenceValidationService validationService,
			TmForumRepository repository, EventHandler eventHandler) {
		super(validationService, repository, eventHandler);
	}

	protected List<ProductSpecificationRef> validateProdSpecCharValueUse(
			List<ProductSpecificationCharacteristicValueUse> productSpecificationCharacteristicValueUses) {
		List<String> ids = productSpecificationCharacteristicValueUses.stream()
				.map(ProductSpecificationCharacteristicValueUse::getId)
				.toList();
		if (ids.size() != new HashSet<>(ids).size()) {
			throw new TmForumException(String.format("Duplicate ids are not allowed - ids: %s", ids),
					TmForumExceptionReason.INVALID_DATA);
		}
		return productSpecificationCharacteristicValueUses.stream()
				.map(ProductSpecificationCharacteristicValueUse::getProductSpecification)
				.filter(Objects::nonNull)
				.toList();
	}
}

package org.fiware.tmforum.productcatalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.wistefan.mapping.MappingException;
import lombok.extern.slf4j.Slf4j;
import org.fiware.productcatalog.model.*;
import org.fiware.tmforum.common.domain.Money;
import org.fiware.tmforum.common.domain.TaxItem;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.BaseMapper;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.product.*;
import org.fiware.tmforum.productcatalog.domain.Catalog;
import org.fiware.tmforum.service.CharacteristicValueSpecification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Mapper between the internal model and api-domain objects
 */
@Slf4j
@Mapper(componentModel = "jsr330", uses = {IdHelper.class, MappingHelper.class})
public abstract class TMForumMapper extends BaseMapper {

	private static final String PRODUCT_SPEC_CHARACTERISTIC_EXT = "productSpecCharacteristic_ext";
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	// catalog

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract CatalogVO map(CatalogCreateVO catalogCreateVO, URI id);

	public abstract CatalogVO map(Catalog catalog);

	@Mapping(target = "href", source = "id")
	public abstract Catalog map(CatalogVO catalogVO);

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract CatalogVO map(CatalogUpdateVO catalogUpdateVO, String id);

	// category

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract CategoryVO map(CategoryCreateVO categoryCreateVO, URI id);

	@Mapping(target = "parentId", qualifiedByName = "fromCategoryRef")
	public abstract CategoryVO map(Category category);

	@Mapping(target = "href", source = "id")
	@Mapping(target = "parentId", source = "categoryVO.parentId", qualifiedByName = "toCategoryRef")
	public abstract Category map(CategoryVO categoryVO);

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract CategoryVO map(CategoryUpdateVO categoryUpdateVO, String id);

	// product offering

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract ProductOfferingVO map(ProductOfferingCreateVO productOfferingCreateVO, URI id);

	public abstract ProductOfferingVO map(ProductOffering productOfferingVO);

	@Mapping(target = "href", source = "id")
	public abstract ProductOffering map(ProductOfferingVO productOfferingVO);

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract ProductOfferingVO map(ProductOfferingUpdateVO productOfferingUpdateVO, String id);

	// product offering price

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract ProductOfferingPriceVO map(ProductOfferingPriceCreateVO productOfferingPriceCreateVO, URI id);

	public abstract ProductOfferingPriceVO map(ProductOfferingPrice productOfferingPrice);

	@Mapping(target = "href", source = "id")
	public abstract ProductOfferingPrice map(ProductOfferingPriceVO productOfferingPriceVO);

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract ProductOfferingPriceVO map(ProductOfferingPriceUpdateVO productOfferingPriceUpdateVO, String id);

	// product specification

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract ProductSpecificationVO map(ProductSpecificationCreateVO productSpecificationCreateVO, URI id);

	public abstract ProductSpecificationVO map(ProductSpecification productSpecification);

	@Mapping(target = "href", source = "id")
	public abstract ProductSpecification map(ProductSpecificationVO productSpecificationVO);

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract ProductSpecificationVO map(ProductSpecificationUpdateVO productSpecificationUpdateVO, String id);

	/**
	 * After mapping ProductSpecification to VO, merge productSpecCharacteristic_ext from
	 * additionalProperties into the productSpecCharacteristic list.
	 */
	@org.mapstruct.AfterMapping
	protected void mergeProductSpecCharacteristicExt(
			ProductSpecification source,
			@org.mapstruct.MappingTarget ProductSpecificationVO target) {

		if (source.getAdditionalProperties() == null) {
			return;
		}

		// Find productSpecCharacteristic_ext in additionalProperties
		source.getAdditionalProperties().stream()
				.filter(prop -> PRODUCT_SPEC_CHARACTERISTIC_EXT.equals(prop.getName()))
				.findFirst()
				.ifPresent(extProperty -> {
					Object value = extProperty.getValue();
					List<ProductSpecificationCharacteristicVO> extCharacteristics = convertToCharacteristicVOList(value);

					if (!extCharacteristics.isEmpty()) {
						if (target.getProductSpecCharacteristic() == null) {
							target.setProductSpecCharacteristic(new ArrayList<>(extCharacteristics));
						} else {
							target.getProductSpecCharacteristic().addAll(extCharacteristics);
						}
						log.debug("Merged {} characteristics from productSpecCharacteristic_ext",
								extCharacteristics.size());
					}
				});
	}

	/**
	 * Convert the raw value from additionalProperties to a list of ProductSpecificationCharacteristicVO.
	 * Handles both single objects and lists.
	 */
	@SuppressWarnings("unchecked")
	private List<ProductSpecificationCharacteristicVO> convertToCharacteristicVOList(Object value) {
		List<ProductSpecificationCharacteristicVO> result = new ArrayList<>();

		if (value == null) {
			return result;
		}

		try {
			if (value instanceof List<?> list) {
				for (Object item : list) {
					ProductSpecificationCharacteristicVO vo = convertToCharacteristicVO(item);
					if (vo != null) {
						result.add(vo);
					}
				}
			} else {
				// Single object (NGSI-LD flattens single-element lists)
				ProductSpecificationCharacteristicVO vo = convertToCharacteristicVO(value);
				if (vo != null) {
					result.add(vo);
				}
			}
		} catch (Exception e) {
			log.warn("Failed to convert productSpecCharacteristic_ext: {}", e.getMessage());
		}

		return result;
	}

	/**
	 * Convert a single object (Map or already typed) to ProductSpecificationCharacteristicVO.
	 * Handles renaming of extension property names to standard TMF names.
	 */
	@SuppressWarnings("unchecked")
	private ProductSpecificationCharacteristicVO convertToCharacteristicVO(Object item) {
		if (item == null) {
			return null;
		}

		try {
			if (item instanceof Map) {
				Map<String, Object> map = new java.util.HashMap<>((Map<String, Object>) item);

				// Rename nested productSpecCharacteristic_ext to standard TMF productSpecCharacteristicValue
				if (map.containsKey(PRODUCT_SPEC_CHARACTERISTIC_EXT)) {
					map.put("productSpecCharacteristicValue", map.remove(PRODUCT_SPEC_CHARACTERISTIC_EXT));
				}

				// Use Jackson to convert Map to VO
				return OBJECT_MAPPER.convertValue(map, ProductSpecificationCharacteristicVO.class);
			} else if (item instanceof ProductSpecificationCharacteristicVO vo) {
				return vo;
			} else {
				log.warn("Unexpected type in productSpecCharacteristic_ext: {}", item.getClass().getName());
				return null;
			}
		} catch (Exception e) {
			log.warn("Failed to convert characteristic: {}", e.getMessage());
			return null;
		}
	}

	@Mapping(target = "query", source = "rawQuery")
	public abstract EventSubscriptionVO map(TMForumSubscription subscription);

	@Mapping(target = "tmfValue", source = "value")
	public abstract Money map(MoneyVO moneyVO);

	@Mapping(target = "value", source = "tmfValue")
	public abstract MoneyVO map(Money money);

	@Mapping(target = "tmfId", source = "id")
	public abstract PricingLogicAlgorithm map(PricingLogicAlgorithmVO pricingLogicAlgorithmVO);

	@Mapping(target = "id", source = "tmfId")
	public abstract PricingLogicAlgorithmVO map(PricingLogicAlgorithm pricingLogicAlgorithm);

	@Mapping(target = "tmfId", source = "id")
	public abstract TaxItem map(TaxItemVO taxItemVO);

	@Mapping(target = "id", source = "tmfId")
	public abstract TaxItemVO map(TaxItem taxItem);

	@Mapping(target = "tmfId", source = "id")
	public abstract ProductSpecificationCharacteristic map(ProductSpecificationCharacteristicVO productSpecificationCharacteristicVO);

	@Mapping(target = "id", source = "tmfId")
	public abstract ProductSpecificationCharacteristicVO map(ProductSpecificationCharacteristic productSpecificationCharacteristic);

	@Mapping(target = "tmfValue", source = "value")
	public abstract CharacteristicValueSpecification map(CharacteristicValueSpecificationVO characteristicVO);

	@Mapping(target = "value", source = "tmfValue")
	public abstract CharacteristicValueSpecificationVO map(CharacteristicValueSpecification characteristic);

	@org.mapstruct.AfterMapping
	protected void afterMappingCharacteristic(ProductSpecificationCharacteristicVO source, @org.mapstruct.MappingTarget ProductSpecificationCharacteristic target) {
		copyAdditionalPropertiesFromVO(source, target.getAdditionalProperties());
	}

	@org.mapstruct.AfterMapping
	protected void afterMappingCharacteristic(ProductSpecificationCharacteristic source, @org.mapstruct.MappingTarget ProductSpecificationCharacteristicVO target) {
		copyAdditionalPropertiesToVO(source.getAdditionalProperties(), target);
	}

	@org.mapstruct.AfterMapping
	protected void afterMappingCharacteristicValue(CharacteristicValueSpecificationVO source, @org.mapstruct.MappingTarget CharacteristicValueSpecification target) {
		copyAdditionalPropertiesFromVO(source, target.getAdditionalProperties());
	}

	@org.mapstruct.AfterMapping
	protected void afterMappingCharacteristicValue(CharacteristicValueSpecification source, @org.mapstruct.MappingTarget CharacteristicValueSpecificationVO target) {
		copyAdditionalPropertiesToVO(source.getAdditionalProperties(), target);
	}

	@Mapping(target = "tmfId", source = "id")
	public abstract ProductSpecificationCharacteristicValueUse map(ProductSpecificationCharacteristicValueUseVO characteristicVO);

	@Mapping(target = "id", source = "tmfId")
	public abstract ProductSpecificationCharacteristicValueUseVO map(ProductSpecificationCharacteristicValueUse characteristic);

	@Mapping(target = "tmfId", source = "id")
	public abstract ProductSpecificationCharacteristicRelationship map(ProductSpecificationCharacteristicRelationshipVO characteristicVO);

	@Mapping(target = "id", source = "tmfId")
	public abstract ProductSpecificationCharacteristicRelationshipVO map(ProductSpecificationCharacteristicRelationship characteristic);
	
	public URL map(String value) {
		if (value == null) {
			return null;
		}
		try {
			return new URL(value);
		} catch (MalformedURLException e) {
			throw new MappingException(String.format("%s is not a URL.", value), e);
		}
	}

	public String map(URL value) {
		if (value == null) {
			return null;
		}
		return value.toString();
	}

	public URI mapToURI(String value) {
		if (value == null) {
			return null;
		}
		return URI.create(value);
	}

	public String mapFromURI(URI value) {
		if (value == null) {
			return null;
		}
		return value.toString();
	}
}



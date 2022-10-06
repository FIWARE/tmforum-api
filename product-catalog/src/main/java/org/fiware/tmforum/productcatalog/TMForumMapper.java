package org.fiware.tmforum.productcatalog;

import org.fiware.party.model.AttachmentVO;
import org.fiware.party.model.CharacteristicVO;
import org.fiware.party.model.ContactMediumVO;
import org.fiware.party.model.DisabilityVO;
import org.fiware.party.model.ExternalReferenceVO;
import org.fiware.party.model.IndividualCreateVO;
import org.fiware.party.model.IndividualIdentificationVO;
import org.fiware.party.model.IndividualStateTypeVO;
import org.fiware.party.model.IndividualUpdateVO;
import org.fiware.party.model.IndividualVO;
import org.fiware.party.model.LanguageAbilityVO;
import org.fiware.party.model.MediumCharacteristicVO;
import org.fiware.party.model.OrganizationChildRelationshipVO;
import org.fiware.party.model.OrganizationCreateVO;
import org.fiware.party.model.OrganizationParentRelationshipVO;
import org.fiware.party.model.OrganizationRefVO;
import org.fiware.party.model.OrganizationUpdateVO;
import org.fiware.party.model.OrganizationVO;
import org.fiware.party.model.OtherNameIndividualVO;
import org.fiware.party.model.OtherNameOrganizationVO;
import org.fiware.party.model.PartyCreditProfileVO;
import org.fiware.party.model.QuantityVO;
import org.fiware.party.model.RelatedPartyVO;
import org.fiware.party.model.SkillVO;
import org.fiware.party.model.TaxDefinitionVO;
import org.fiware.party.model.TaxExemptionCertificateVO;
import org.fiware.party.model.TimePeriodVO;
import org.fiware.productcatalog.model.CatalogCreateVO;
import org.fiware.productcatalog.model.CatalogUpdateVO;
import org.fiware.productcatalog.model.CatalogVO;
import org.fiware.productcatalog.model.CategoryCreateVO;
import org.fiware.productcatalog.model.CategoryUpdateVO;
import org.fiware.productcatalog.model.CategoryVO;
import org.fiware.productcatalog.model.ProductOfferingCreateVO;
import org.fiware.productcatalog.model.ProductOfferingUpdateVO;
import org.fiware.productcatalog.model.ProductOfferingVO;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.mapping.MappingException;
import org.fiware.tmforum.party.domain.Attachment;
import org.fiware.tmforum.party.domain.Characteristic;
import org.fiware.tmforum.party.domain.ContactMedium;
import org.fiware.tmforum.party.domain.ExternalReference;
import org.fiware.tmforum.party.domain.MediumCharacteristic;
import org.fiware.tmforum.party.domain.PartyCreditProfile;
import org.fiware.tmforum.party.domain.Quantity;
import org.fiware.tmforum.party.domain.RelatedParty;
import org.fiware.tmforum.party.domain.TaxDefinition;
import org.fiware.tmforum.party.domain.TaxExemptionCertificate;
import org.fiware.tmforum.party.domain.TimePeriod;
import org.fiware.tmforum.party.domain.individual.Disability;
import org.fiware.tmforum.party.domain.individual.Individual;
import org.fiware.tmforum.party.domain.individual.IndividualIdentification;
import org.fiware.tmforum.party.domain.individual.IndividualState;
import org.fiware.tmforum.party.domain.individual.LanguageAbility;
import org.fiware.tmforum.party.domain.individual.OtherIndividualName;
import org.fiware.tmforum.party.domain.individual.Skill;
import org.fiware.tmforum.party.domain.organization.Organization;
import org.fiware.tmforum.party.domain.organization.OrganizationChildRelationship;
import org.fiware.tmforum.party.domain.organization.OrganizationParentRelationship;
import org.fiware.tmforum.party.domain.organization.OtherOrganizationName;
import org.fiware.tmforum.productcatalog.domain.Catalog;
import org.fiware.tmforum.productcatalog.domain.Category;
import org.fiware.tmforum.productcatalog.domain.CategoryRef;
import org.fiware.tmforum.productcatalog.domain.ProductOffering;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Mapper between the internal model and api-domain objects
 */
@Mapper(componentModel = "jsr330", uses = {IdHelper.class, MappingHelper.class})
public interface TMForumMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    CatalogVO map(CatalogCreateVO catalogCreateVO, URI id);

    CatalogVO map(Catalog catalog);

    @Mapping(target = "href", source = "id")
    Catalog map(CatalogVO catalogVO);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    CatalogVO map(CatalogUpdateVO catalogUpdateVO, String id);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    CategoryVO map(CategoryCreateVO categoryCreateVO, URI id);

    @Mapping(target = "parentId", qualifiedByName = "fromCategoryRef")
    CategoryVO map(Category category);

    @Mapping(target = "href", source = "id")
    @Mapping(target = "parentId", source = "categoryVO.parentId", qualifiedByName = "toCategoryRef")
    Category map(CategoryVO categoryVO);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    CategoryVO map(CategoryUpdateVO categoryUpdateVO, String id);


    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    ProductOfferingVO map(ProductOfferingCreateVO productOfferingCreateVO, URI id);

    ProductOfferingVO map(ProductOffering productOfferingVO);

    @Mapping(target = "href", source = "id")
    ProductOffering map(ProductOfferingVO categoryVO);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    ProductOfferingVO map(ProductOfferingUpdateVO categoryUpdateVO, String id);

    default URL map(String value) {
        if (value == null) {
            return null;
        }
        try {
            return new URL(value);
        } catch (MalformedURLException e) {
            throw new MappingException(String.format("%s is not a URL.", value), e);
        }
    }

    default String map(URL value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    default URI mapToURI(String value) {
        if (value == null) {
            return null;
        }
        return URI.create(value);
    }

    default String mapFromURI(URI value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }
}



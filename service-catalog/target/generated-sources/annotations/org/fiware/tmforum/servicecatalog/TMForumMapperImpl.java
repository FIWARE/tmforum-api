package org.fiware.tmforum.servicecatalog;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import javax.inject.Named;
import javax.inject.Singleton;
import org.fiware.servicecatalog.model.AssociationSpecificationRefVO;
import org.fiware.servicecatalog.model.AttachmentRefOrValueVO;
import org.fiware.servicecatalog.model.CharacteristicSpecificationRelationshipVO;
import org.fiware.servicecatalog.model.CharacteristicSpecificationVO;
import org.fiware.servicecatalog.model.CharacteristicValueSpecificationVO;
import org.fiware.servicecatalog.model.ConstraintRefVO;
import org.fiware.servicecatalog.model.EntitySpecificationRelationshipVO;
import org.fiware.servicecatalog.model.EventSubscriptionVO;
import org.fiware.servicecatalog.model.FeatureSpecificationCharacteristicRelationshipVO;
import org.fiware.servicecatalog.model.FeatureSpecificationCharacteristicVO;
import org.fiware.servicecatalog.model.FeatureSpecificationRelationshipVO;
import org.fiware.servicecatalog.model.FeatureSpecificationVO;
import org.fiware.servicecatalog.model.QuantityVO;
import org.fiware.servicecatalog.model.RelatedPartyVO;
import org.fiware.servicecatalog.model.ResourceSpecificationRefVO;
import org.fiware.servicecatalog.model.ServiceCandidateCreateVO;
import org.fiware.servicecatalog.model.ServiceCandidateRefVO;
import org.fiware.servicecatalog.model.ServiceCandidateUpdateVO;
import org.fiware.servicecatalog.model.ServiceCandidateVO;
import org.fiware.servicecatalog.model.ServiceCatalogCreateVO;
import org.fiware.servicecatalog.model.ServiceCatalogUpdateVO;
import org.fiware.servicecatalog.model.ServiceCatalogVO;
import org.fiware.servicecatalog.model.ServiceCategoryCreateVO;
import org.fiware.servicecatalog.model.ServiceCategoryRefVO;
import org.fiware.servicecatalog.model.ServiceCategoryUpdateVO;
import org.fiware.servicecatalog.model.ServiceCategoryVO;
import org.fiware.servicecatalog.model.ServiceLevelSpecificationRefVO;
import org.fiware.servicecatalog.model.ServiceSpecRelationshipVO;
import org.fiware.servicecatalog.model.ServiceSpecificationCreateVO;
import org.fiware.servicecatalog.model.ServiceSpecificationRefVO;
import org.fiware.servicecatalog.model.ServiceSpecificationUpdateVO;
import org.fiware.servicecatalog.model.ServiceSpecificationVO;
import org.fiware.servicecatalog.model.TargetEntitySchemaVO;
import org.fiware.servicecatalog.model.TimePeriodVO;
import org.fiware.tmforum.common.domain.AttachmentRefOrValue;
import org.fiware.tmforum.common.domain.ConstraintRef;
import org.fiware.tmforum.common.domain.Quantity;
import org.fiware.tmforum.common.domain.RelatedParty;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.resource.CharacteristicValue;
import org.fiware.tmforum.resource.FeatureSpecificationCharacteristicRelationship;
import org.fiware.tmforum.resource.ResourceSpecificationRef;
import org.fiware.tmforum.service.AssociationSpecificationRef;
import org.fiware.tmforum.service.CharacteristicSpecification;
import org.fiware.tmforum.service.CharacteristicSpecificationRelationship;
import org.fiware.tmforum.service.CharacteristicValueSpecification;
import org.fiware.tmforum.service.EntitySpecificationRelationship;
import org.fiware.tmforum.service.FeatureSpecification;
import org.fiware.tmforum.service.FeatureSpecificationCharacteristic;
import org.fiware.tmforum.service.FeatureSpecificationRelationship;
import org.fiware.tmforum.service.ServiceCandidate;
import org.fiware.tmforum.service.ServiceCandidateRef;
import org.fiware.tmforum.service.ServiceCategory;
import org.fiware.tmforum.service.ServiceCategoryRef;
import org.fiware.tmforum.service.ServiceLevelSpecificationRef;
import org.fiware.tmforum.service.ServiceSpecificationRef;
import org.fiware.tmforum.service.ServiceSpecificationRelationship;
import org.fiware.tmforum.service.TargetEntitySchema;
import org.fiware.tmforum.servicecatalog.domain.ServiceCatalog;
import org.fiware.tmforum.servicecatalog.domain.ServiceSpecification;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2023-11-22T12:14:09+0100",
    comments = "version: 1.4.2.Final, compiler: javac, environment: Java 17.0.9 (Amazon.com Inc.)"
)
@Singleton
@Named
public class TMForumMapperImpl implements TMForumMapper {

    @Override
    public ServiceCatalogVO map(ServiceCatalogCreateVO serviceCatalogCreateVO, URI id) {
        if ( serviceCatalogCreateVO == null && id == null ) {
            return null;
        }

        ServiceCatalogVO serviceCatalogVO = new ServiceCatalogVO();

        if ( serviceCatalogCreateVO != null ) {
            serviceCatalogVO.setDescription( serviceCatalogCreateVO.getDescription() );
            serviceCatalogVO.setLastUpdate( serviceCatalogCreateVO.getLastUpdate() );
            serviceCatalogVO.setLifecycleStatus( serviceCatalogCreateVO.getLifecycleStatus() );
            serviceCatalogVO.setName( serviceCatalogCreateVO.getName() );
            serviceCatalogVO.setVersion( serviceCatalogCreateVO.getVersion() );
            List<ServiceCategoryRefVO> list = serviceCatalogCreateVO.getCategory();
            if ( list != null ) {
                serviceCatalogVO.setCategory( new ArrayList<ServiceCategoryRefVO>( list ) );
            }
            List<RelatedPartyVO> list1 = serviceCatalogCreateVO.getRelatedParty();
            if ( list1 != null ) {
                serviceCatalogVO.setRelatedParty( new ArrayList<RelatedPartyVO>( list1 ) );
            }
            serviceCatalogVO.setValidFor( serviceCatalogCreateVO.getValidFor() );
            serviceCatalogVO.setAtBaseType( serviceCatalogCreateVO.getAtBaseType() );
            serviceCatalogVO.setAtSchemaLocation( serviceCatalogCreateVO.getAtSchemaLocation() );
            serviceCatalogVO.setAtType( serviceCatalogCreateVO.getAtType() );
        }
        if ( id != null ) {
            serviceCatalogVO.setId( mapFromURI( id ) );
            serviceCatalogVO.setHref( id );
        }

        return serviceCatalogVO;
    }

    @Override
    public ServiceCatalogVO map(ServiceCatalog serviceCatalog) {
        if ( serviceCatalog == null ) {
            return null;
        }

        ServiceCatalogVO serviceCatalogVO = new ServiceCatalogVO();

        serviceCatalogVO.setId( mapFromURI( serviceCatalog.getId() ) );
        serviceCatalogVO.setHref( serviceCatalog.getHref() );
        serviceCatalogVO.setDescription( serviceCatalog.getDescription() );
        serviceCatalogVO.setLastUpdate( serviceCatalog.getLastUpdate() );
        serviceCatalogVO.setLifecycleStatus( serviceCatalog.getLifecycleStatus() );
        serviceCatalogVO.setName( serviceCatalog.getName() );
        serviceCatalogVO.setVersion( serviceCatalog.getVersion() );
        serviceCatalogVO.setCategory( serviceCategoryRefListToServiceCategoryRefVOList( serviceCatalog.getCategory() ) );
        serviceCatalogVO.setRelatedParty( relatedPartyListToRelatedPartyVOList( serviceCatalog.getRelatedParty() ) );
        serviceCatalogVO.setValidFor( timePeriodToTimePeriodVO( serviceCatalog.getValidFor() ) );
        serviceCatalogVO.setAtBaseType( serviceCatalog.getAtBaseType() );
        serviceCatalogVO.setAtSchemaLocation( serviceCatalog.getAtSchemaLocation() );
        serviceCatalogVO.setAtType( serviceCatalog.getAtType() );

        return serviceCatalogVO;
    }

    @Override
    public ServiceCatalog map(ServiceCatalogVO serviceCatalogVO) {
        if ( serviceCatalogVO == null ) {
            return null;
        }

        String id = null;

        id = serviceCatalogVO.getId();

        ServiceCatalog serviceCatalog = new ServiceCatalog( id );

        serviceCatalog.setAtBaseType( serviceCatalogVO.getAtBaseType() );
        serviceCatalog.setAtSchemaLocation( serviceCatalogVO.getAtSchemaLocation() );
        serviceCatalog.setAtType( serviceCatalogVO.getAtType() );
        serviceCatalog.setHref( serviceCatalogVO.getHref() );
        serviceCatalog.setDescription( serviceCatalogVO.getDescription() );
        serviceCatalog.setLastUpdate( serviceCatalogVO.getLastUpdate() );
        serviceCatalog.setLifecycleStatus( serviceCatalogVO.getLifecycleStatus() );
        serviceCatalog.setName( serviceCatalogVO.getName() );
        serviceCatalog.setVersion( serviceCatalogVO.getVersion() );
        serviceCatalog.setCategory( serviceCategoryRefVOListToServiceCategoryRefList( serviceCatalogVO.getCategory() ) );
        serviceCatalog.setRelatedParty( relatedPartyVOListToRelatedPartyList( serviceCatalogVO.getRelatedParty() ) );
        serviceCatalog.setValidFor( timePeriodVOToTimePeriod( serviceCatalogVO.getValidFor() ) );

        return serviceCatalog;
    }

    @Override
    public ServiceCatalog map(ServiceCatalogUpdateVO serviceCatalogUpdateVO, String id) {
        if ( serviceCatalogUpdateVO == null && id == null ) {
            return null;
        }

        String id1 = null;
        if ( id != null ) {
            id1 = id;
        }

        ServiceCatalog serviceCatalog = new ServiceCatalog( id1 );

        if ( serviceCatalogUpdateVO != null ) {
            serviceCatalog.setAtBaseType( serviceCatalogUpdateVO.getAtBaseType() );
            serviceCatalog.setAtSchemaLocation( serviceCatalogUpdateVO.getAtSchemaLocation() );
            serviceCatalog.setAtType( serviceCatalogUpdateVO.getAtType() );
            serviceCatalog.setDescription( serviceCatalogUpdateVO.getDescription() );
            serviceCatalog.setLifecycleStatus( serviceCatalogUpdateVO.getLifecycleStatus() );
            serviceCatalog.setName( serviceCatalogUpdateVO.getName() );
            serviceCatalog.setVersion( serviceCatalogUpdateVO.getVersion() );
            serviceCatalog.setCategory( serviceCategoryRefVOListToServiceCategoryRefList( serviceCatalogUpdateVO.getCategory() ) );
            serviceCatalog.setRelatedParty( relatedPartyVOListToRelatedPartyList( serviceCatalogUpdateVO.getRelatedParty() ) );
            serviceCatalog.setValidFor( timePeriodVOToTimePeriod( serviceCatalogUpdateVO.getValidFor() ) );
        }

        return serviceCatalog;
    }

    @Override
    public ServiceCandidateVO map(ServiceCandidateCreateVO serviceCandidateCreateVO, URI id) {
        if ( serviceCandidateCreateVO == null && id == null ) {
            return null;
        }

        ServiceCandidateVO serviceCandidateVO = new ServiceCandidateVO();

        if ( serviceCandidateCreateVO != null ) {
            serviceCandidateVO.setDescription( serviceCandidateCreateVO.getDescription() );
            serviceCandidateVO.setLastUpdate( serviceCandidateCreateVO.getLastUpdate() );
            serviceCandidateVO.setLifecycleStatus( serviceCandidateCreateVO.getLifecycleStatus() );
            serviceCandidateVO.setName( serviceCandidateCreateVO.getName() );
            serviceCandidateVO.setVersion( serviceCandidateCreateVO.getVersion() );
            List<ServiceCategoryRefVO> list = serviceCandidateCreateVO.getCategory();
            if ( list != null ) {
                serviceCandidateVO.setCategory( new ArrayList<ServiceCategoryRefVO>( list ) );
            }
            serviceCandidateVO.setServiceSpecification( serviceCandidateCreateVO.getServiceSpecification() );
            serviceCandidateVO.setValidFor( serviceCandidateCreateVO.getValidFor() );
            serviceCandidateVO.setAtBaseType( serviceCandidateCreateVO.getAtBaseType() );
            serviceCandidateVO.setAtSchemaLocation( serviceCandidateCreateVO.getAtSchemaLocation() );
            serviceCandidateVO.setAtType( serviceCandidateCreateVO.getAtType() );
        }
        if ( id != null ) {
            serviceCandidateVO.setId( mapFromURI( id ) );
            serviceCandidateVO.setHref( id );
        }

        return serviceCandidateVO;
    }

    @Override
    public ServiceCandidateVO map(ServiceCandidate serviceCandidate) {
        if ( serviceCandidate == null ) {
            return null;
        }

        ServiceCandidateVO serviceCandidateVO = new ServiceCandidateVO();

        serviceCandidateVO.setId( mapFromURI( serviceCandidate.getId() ) );
        serviceCandidateVO.setHref( serviceCandidate.getHref() );
        serviceCandidateVO.setDescription( serviceCandidate.getDescription() );
        serviceCandidateVO.setLastUpdate( serviceCandidate.getLastUpdate() );
        serviceCandidateVO.setLifecycleStatus( serviceCandidate.getLifecycleStatus() );
        serviceCandidateVO.setName( serviceCandidate.getName() );
        serviceCandidateVO.setVersion( serviceCandidate.getVersion() );
        serviceCandidateVO.setCategory( serviceCategoryRefListToServiceCategoryRefVOList( serviceCandidate.getCategory() ) );
        serviceCandidateVO.setServiceSpecification( serviceSpecificationRefToServiceSpecificationRefVO( serviceCandidate.getServiceSpecification() ) );
        serviceCandidateVO.setValidFor( timePeriodToTimePeriodVO( serviceCandidate.getValidFor() ) );
        serviceCandidateVO.setAtBaseType( serviceCandidate.getAtBaseType() );
        serviceCandidateVO.setAtSchemaLocation( serviceCandidate.getAtSchemaLocation() );
        serviceCandidateVO.setAtType( serviceCandidate.getAtType() );

        return serviceCandidateVO;
    }

    @Override
    public ServiceCandidate map(ServiceCandidateVO serviceCandidateVO) {
        if ( serviceCandidateVO == null ) {
            return null;
        }

        String id = null;

        id = serviceCandidateVO.getId();

        ServiceCandidate serviceCandidate = new ServiceCandidate( id );

        serviceCandidate.setAtBaseType( serviceCandidateVO.getAtBaseType() );
        serviceCandidate.setAtSchemaLocation( serviceCandidateVO.getAtSchemaLocation() );
        serviceCandidate.setAtType( serviceCandidateVO.getAtType() );
        serviceCandidate.setHref( serviceCandidateVO.getHref() );
        serviceCandidate.setDescription( serviceCandidateVO.getDescription() );
        serviceCandidate.setLastUpdate( serviceCandidateVO.getLastUpdate() );
        serviceCandidate.setLifecycleStatus( serviceCandidateVO.getLifecycleStatus() );
        serviceCandidate.setName( serviceCandidateVO.getName() );
        serviceCandidate.setVersion( serviceCandidateVO.getVersion() );
        serviceCandidate.setCategory( serviceCategoryRefVOListToServiceCategoryRefList( serviceCandidateVO.getCategory() ) );
        serviceCandidate.setServiceSpecification( serviceSpecificationRefVOToServiceSpecificationRef( serviceCandidateVO.getServiceSpecification() ) );
        serviceCandidate.setValidFor( timePeriodVOToTimePeriod( serviceCandidateVO.getValidFor() ) );

        return serviceCandidate;
    }

    @Override
    public ServiceCandidate map(ServiceCandidateUpdateVO serviceCandidateUpdateVO, String id) {
        if ( serviceCandidateUpdateVO == null && id == null ) {
            return null;
        }

        String id1 = null;
        if ( id != null ) {
            id1 = id;
        }

        ServiceCandidate serviceCandidate = new ServiceCandidate( id1 );

        if ( serviceCandidateUpdateVO != null ) {
            serviceCandidate.setAtBaseType( serviceCandidateUpdateVO.getAtBaseType() );
            serviceCandidate.setAtSchemaLocation( serviceCandidateUpdateVO.getAtSchemaLocation() );
            serviceCandidate.setAtType( serviceCandidateUpdateVO.getAtType() );
            serviceCandidate.setDescription( serviceCandidateUpdateVO.getDescription() );
            serviceCandidate.setLifecycleStatus( serviceCandidateUpdateVO.getLifecycleStatus() );
            serviceCandidate.setName( serviceCandidateUpdateVO.getName() );
            serviceCandidate.setVersion( serviceCandidateUpdateVO.getVersion() );
            serviceCandidate.setCategory( serviceCategoryRefVOListToServiceCategoryRefList( serviceCandidateUpdateVO.getCategory() ) );
            serviceCandidate.setServiceSpecification( serviceSpecificationRefVOToServiceSpecificationRef( serviceCandidateUpdateVO.getServiceSpecification() ) );
            serviceCandidate.setValidFor( timePeriodVOToTimePeriod( serviceCandidateUpdateVO.getValidFor() ) );
        }

        return serviceCandidate;
    }

    @Override
    public ServiceCategoryVO map(ServiceCategoryCreateVO serviceCategoryCreateVO, URI id) {
        if ( serviceCategoryCreateVO == null && id == null ) {
            return null;
        }

        ServiceCategoryVO serviceCategoryVO = new ServiceCategoryVO();

        if ( serviceCategoryCreateVO != null ) {
            serviceCategoryVO.setDescription( serviceCategoryCreateVO.getDescription() );
            serviceCategoryVO.setIsRoot( serviceCategoryCreateVO.getIsRoot() );
            serviceCategoryVO.setLastUpdate( serviceCategoryCreateVO.getLastUpdate() );
            serviceCategoryVO.setLifecycleStatus( serviceCategoryCreateVO.getLifecycleStatus() );
            serviceCategoryVO.setName( serviceCategoryCreateVO.getName() );
            serviceCategoryVO.setParentId( serviceCategoryCreateVO.getParentId() );
            serviceCategoryVO.setVersion( serviceCategoryCreateVO.getVersion() );
            List<ServiceCategoryRefVO> list = serviceCategoryCreateVO.getCategory();
            if ( list != null ) {
                serviceCategoryVO.setCategory( new ArrayList<ServiceCategoryRefVO>( list ) );
            }
            List<ServiceCandidateRefVO> list1 = serviceCategoryCreateVO.getServiceCandidate();
            if ( list1 != null ) {
                serviceCategoryVO.setServiceCandidate( new ArrayList<ServiceCandidateRefVO>( list1 ) );
            }
            serviceCategoryVO.setValidFor( serviceCategoryCreateVO.getValidFor() );
            serviceCategoryVO.setAtBaseType( serviceCategoryCreateVO.getAtBaseType() );
            serviceCategoryVO.setAtSchemaLocation( serviceCategoryCreateVO.getAtSchemaLocation() );
            serviceCategoryVO.setAtType( serviceCategoryCreateVO.getAtType() );
        }
        if ( id != null ) {
            serviceCategoryVO.setId( mapFromURI( id ) );
            serviceCategoryVO.setHref( id );
        }

        return serviceCategoryVO;
    }

    @Override
    public ServiceCategoryVO map(ServiceCategory serviceCategory) {
        if ( serviceCategory == null ) {
            return null;
        }

        ServiceCategoryVO serviceCategoryVO = new ServiceCategoryVO();

        serviceCategoryVO.setId( mapFromURI( serviceCategory.getId() ) );
        serviceCategoryVO.setHref( serviceCategory.getHref() );
        serviceCategoryVO.setDescription( serviceCategory.getDescription() );
        serviceCategoryVO.setIsRoot( serviceCategory.getIsRoot() );
        serviceCategoryVO.setLastUpdate( serviceCategory.getLastUpdate() );
        serviceCategoryVO.setLifecycleStatus( serviceCategory.getLifecycleStatus() );
        serviceCategoryVO.setName( serviceCategory.getName() );
        serviceCategoryVO.setParentId( mapFromServiceCategoryRef( serviceCategory.getParentId() ) );
        serviceCategoryVO.setVersion( serviceCategory.getVersion() );
        serviceCategoryVO.setCategory( serviceCategoryRefListToServiceCategoryRefVOList( serviceCategory.getCategory() ) );
        serviceCategoryVO.setServiceCandidate( serviceCandidateRefListToServiceCandidateRefVOList( serviceCategory.getServiceCandidate() ) );
        serviceCategoryVO.setValidFor( timePeriodToTimePeriodVO( serviceCategory.getValidFor() ) );
        serviceCategoryVO.setAtBaseType( serviceCategory.getAtBaseType() );
        serviceCategoryVO.setAtSchemaLocation( serviceCategory.getAtSchemaLocation() );
        serviceCategoryVO.setAtType( serviceCategory.getAtType() );

        return serviceCategoryVO;
    }

    @Override
    public ServiceCategory map(ServiceCategoryVO serviceCandidateVO) {
        if ( serviceCandidateVO == null ) {
            return null;
        }

        String id = null;

        id = serviceCandidateVO.getId();

        ServiceCategory serviceCategory = new ServiceCategory( id );

        serviceCategory.setAtBaseType( serviceCandidateVO.getAtBaseType() );
        serviceCategory.setAtSchemaLocation( serviceCandidateVO.getAtSchemaLocation() );
        serviceCategory.setAtType( serviceCandidateVO.getAtType() );
        serviceCategory.setHref( serviceCandidateVO.getHref() );
        serviceCategory.setDescription( serviceCandidateVO.getDescription() );
        serviceCategory.setIsRoot( serviceCandidateVO.getIsRoot() );
        serviceCategory.setLastUpdate( serviceCandidateVO.getLastUpdate() );
        serviceCategory.setLifecycleStatus( serviceCandidateVO.getLifecycleStatus() );
        serviceCategory.setName( serviceCandidateVO.getName() );
        serviceCategory.setParentId( mapFromServiceCategoryRefId( serviceCandidateVO.getParentId() ) );
        serviceCategory.setVersion( serviceCandidateVO.getVersion() );
        serviceCategory.setCategory( serviceCategoryRefVOListToServiceCategoryRefList( serviceCandidateVO.getCategory() ) );
        serviceCategory.setServiceCandidate( serviceCandidateRefVOListToServiceCandidateRefList( serviceCandidateVO.getServiceCandidate() ) );
        serviceCategory.setValidFor( timePeriodVOToTimePeriod( serviceCandidateVO.getValidFor() ) );

        return serviceCategory;
    }

    @Override
    public ServiceCategory map(ServiceCategoryUpdateVO serviceCategoryUpdateVO, String id) {
        if ( serviceCategoryUpdateVO == null && id == null ) {
            return null;
        }

        String id1 = null;
        if ( id != null ) {
            id1 = id;
        }

        ServiceCategory serviceCategory = new ServiceCategory( id1 );

        if ( serviceCategoryUpdateVO != null ) {
            serviceCategory.setAtBaseType( serviceCategoryUpdateVO.getAtBaseType() );
            serviceCategory.setAtSchemaLocation( serviceCategoryUpdateVO.getAtSchemaLocation() );
            serviceCategory.setAtType( serviceCategoryUpdateVO.getAtType() );
            serviceCategory.setDescription( serviceCategoryUpdateVO.getDescription() );
            serviceCategory.setIsRoot( serviceCategoryUpdateVO.getIsRoot() );
            serviceCategory.setLifecycleStatus( serviceCategoryUpdateVO.getLifecycleStatus() );
            serviceCategory.setName( serviceCategoryUpdateVO.getName() );
            serviceCategory.setParentId( mapFromServiceCategoryRefId( serviceCategoryUpdateVO.getParentId() ) );
            serviceCategory.setVersion( serviceCategoryUpdateVO.getVersion() );
            serviceCategory.setCategory( serviceCategoryRefVOListToServiceCategoryRefList( serviceCategoryUpdateVO.getCategory() ) );
            serviceCategory.setServiceCandidate( serviceCandidateRefVOListToServiceCandidateRefList( serviceCategoryUpdateVO.getServiceCandidate() ) );
            serviceCategory.setValidFor( timePeriodVOToTimePeriod( serviceCategoryUpdateVO.getValidFor() ) );
        }

        return serviceCategory;
    }

    @Override
    public ServiceSpecificationVO map(ServiceSpecificationCreateVO serviceSpecificationCreateVO, URI id) {
        if ( serviceSpecificationCreateVO == null && id == null ) {
            return null;
        }

        ServiceSpecificationVO serviceSpecificationVO = new ServiceSpecificationVO();

        if ( serviceSpecificationCreateVO != null ) {
            serviceSpecificationVO.setDescription( serviceSpecificationCreateVO.getDescription() );
            serviceSpecificationVO.setIsBundle( serviceSpecificationCreateVO.getIsBundle() );
            serviceSpecificationVO.setLastUpdate( serviceSpecificationCreateVO.getLastUpdate() );
            serviceSpecificationVO.setLifecycleStatus( serviceSpecificationCreateVO.getLifecycleStatus() );
            serviceSpecificationVO.setName( serviceSpecificationCreateVO.getName() );
            serviceSpecificationVO.setVersion( serviceSpecificationCreateVO.getVersion() );
            List<AttachmentRefOrValueVO> list = serviceSpecificationCreateVO.getAttachment();
            if ( list != null ) {
                serviceSpecificationVO.setAttachment( new ArrayList<AttachmentRefOrValueVO>( list ) );
            }
            List<ConstraintRefVO> list1 = serviceSpecificationCreateVO.getConstraint();
            if ( list1 != null ) {
                serviceSpecificationVO.setConstraint( new ArrayList<ConstraintRefVO>( list1 ) );
            }
            List<EntitySpecificationRelationshipVO> list2 = serviceSpecificationCreateVO.getEntitySpecRelationship();
            if ( list2 != null ) {
                serviceSpecificationVO.setEntitySpecRelationship( new ArrayList<EntitySpecificationRelationshipVO>( list2 ) );
            }
            List<FeatureSpecificationVO> list3 = serviceSpecificationCreateVO.getFeatureSpecification();
            if ( list3 != null ) {
                serviceSpecificationVO.setFeatureSpecification( new ArrayList<FeatureSpecificationVO>( list3 ) );
            }
            List<RelatedPartyVO> list4 = serviceSpecificationCreateVO.getRelatedParty();
            if ( list4 != null ) {
                serviceSpecificationVO.setRelatedParty( new ArrayList<RelatedPartyVO>( list4 ) );
            }
            List<ResourceSpecificationRefVO> list5 = serviceSpecificationCreateVO.getResourceSpecification();
            if ( list5 != null ) {
                serviceSpecificationVO.setResourceSpecification( new ArrayList<ResourceSpecificationRefVO>( list5 ) );
            }
            List<ServiceLevelSpecificationRefVO> list6 = serviceSpecificationCreateVO.getServiceLevelSpecification();
            if ( list6 != null ) {
                serviceSpecificationVO.setServiceLevelSpecification( new ArrayList<ServiceLevelSpecificationRefVO>( list6 ) );
            }
            List<ServiceSpecRelationshipVO> list7 = serviceSpecificationCreateVO.getServiceSpecRelationship();
            if ( list7 != null ) {
                serviceSpecificationVO.setServiceSpecRelationship( new ArrayList<ServiceSpecRelationshipVO>( list7 ) );
            }
            List<CharacteristicSpecificationVO> list8 = serviceSpecificationCreateVO.getSpecCharacteristic();
            if ( list8 != null ) {
                serviceSpecificationVO.setSpecCharacteristic( new ArrayList<CharacteristicSpecificationVO>( list8 ) );
            }
            serviceSpecificationVO.setTargetEntitySchema( serviceSpecificationCreateVO.getTargetEntitySchema() );
            serviceSpecificationVO.setValidFor( serviceSpecificationCreateVO.getValidFor() );
            serviceSpecificationVO.setAtBaseType( serviceSpecificationCreateVO.getAtBaseType() );
            serviceSpecificationVO.setAtSchemaLocation( serviceSpecificationCreateVO.getAtSchemaLocation() );
            serviceSpecificationVO.setAtType( serviceSpecificationCreateVO.getAtType() );
        }
        if ( id != null ) {
            serviceSpecificationVO.setId( mapFromURI( id ) );
            serviceSpecificationVO.setHref( id );
        }

        return serviceSpecificationVO;
    }

    @Override
    public ServiceSpecificationVO map(ServiceSpecification serviceSpecification) {
        if ( serviceSpecification == null ) {
            return null;
        }

        ServiceSpecificationVO serviceSpecificationVO = new ServiceSpecificationVO();

        serviceSpecificationVO.setId( mapFromURI( serviceSpecification.getId() ) );
        serviceSpecificationVO.setHref( serviceSpecification.getHref() );
        serviceSpecificationVO.setDescription( serviceSpecification.getDescription() );
        serviceSpecificationVO.setIsBundle( serviceSpecification.getIsBundle() );
        serviceSpecificationVO.setLastUpdate( serviceSpecification.getLastUpdate() );
        serviceSpecificationVO.setLifecycleStatus( serviceSpecification.getLifecycleStatus() );
        serviceSpecificationVO.setName( serviceSpecification.getName() );
        serviceSpecificationVO.setVersion( serviceSpecification.getVersion() );
        serviceSpecificationVO.setAttachment( attachmentRefOrValueListToAttachmentRefOrValueVOList( serviceSpecification.getAttachment() ) );
        serviceSpecificationVO.setEntitySpecRelationship( entitySpecificationRelationshipListToEntitySpecificationRelationshipVOList( serviceSpecification.getEntitySpecRelationship() ) );
        serviceSpecificationVO.setFeatureSpecification( featureSpecificationListToFeatureSpecificationVOList( serviceSpecification.getFeatureSpecification() ) );
        serviceSpecificationVO.setRelatedParty( relatedPartyListToRelatedPartyVOList( serviceSpecification.getRelatedParty() ) );
        serviceSpecificationVO.setResourceSpecification( resourceSpecificationRefListToResourceSpecificationRefVOList( serviceSpecification.getResourceSpecification() ) );
        serviceSpecificationVO.setServiceLevelSpecification( serviceLevelSpecificationRefListToServiceLevelSpecificationRefVOList( serviceSpecification.getServiceLevelSpecification() ) );
        serviceSpecificationVO.setServiceSpecRelationship( serviceSpecificationRelationshipListToServiceSpecRelationshipVOList( serviceSpecification.getServiceSpecRelationship() ) );
        serviceSpecificationVO.setSpecCharacteristic( characteristicSpecificationListToCharacteristicSpecificationVOList( serviceSpecification.getSpecCharacteristic() ) );
        serviceSpecificationVO.setTargetEntitySchema( targetEntitySchemaToTargetEntitySchemaVO( serviceSpecification.getTargetEntitySchema() ) );
        serviceSpecificationVO.setValidFor( timePeriodToTimePeriodVO( serviceSpecification.getValidFor() ) );
        serviceSpecificationVO.setAtBaseType( serviceSpecification.getAtBaseType() );
        serviceSpecificationVO.setAtSchemaLocation( serviceSpecification.getAtSchemaLocation() );
        serviceSpecificationVO.setAtType( serviceSpecification.getAtType() );

        return serviceSpecificationVO;
    }

    @Override
    public ServiceSpecification map(ServiceSpecificationVO serviceSpecificationVO) {
        if ( serviceSpecificationVO == null ) {
            return null;
        }

        String id = null;

        id = serviceSpecificationVO.getId();

        ServiceSpecification serviceSpecification = new ServiceSpecification( id );

        serviceSpecification.setAtBaseType( serviceSpecificationVO.getAtBaseType() );
        serviceSpecification.setAtSchemaLocation( serviceSpecificationVO.getAtSchemaLocation() );
        serviceSpecification.setAtType( serviceSpecificationVO.getAtType() );
        serviceSpecification.setHref( serviceSpecificationVO.getHref() );
        serviceSpecification.setDescription( serviceSpecificationVO.getDescription() );
        serviceSpecification.setIsBundle( serviceSpecificationVO.getIsBundle() );
        serviceSpecification.setLastUpdate( serviceSpecificationVO.getLastUpdate() );
        serviceSpecification.setLifecycleStatus( serviceSpecificationVO.getLifecycleStatus() );
        serviceSpecification.setName( serviceSpecificationVO.getName() );
        serviceSpecification.setVersion( serviceSpecificationVO.getVersion() );
        serviceSpecification.setAttachment( attachmentRefOrValueVOListToAttachmentRefOrValueList( serviceSpecificationVO.getAttachment() ) );
        serviceSpecification.setEntitySpecRelationship( entitySpecificationRelationshipVOListToEntitySpecificationRelationshipList( serviceSpecificationVO.getEntitySpecRelationship() ) );
        serviceSpecification.setFeatureSpecification( featureSpecificationVOListToFeatureSpecificationList( serviceSpecificationVO.getFeatureSpecification() ) );
        serviceSpecification.setRelatedParty( relatedPartyVOListToRelatedPartyList( serviceSpecificationVO.getRelatedParty() ) );
        serviceSpecification.setResourceSpecification( resourceSpecificationRefVOListToResourceSpecificationRefList( serviceSpecificationVO.getResourceSpecification() ) );
        serviceSpecification.setServiceLevelSpecification( serviceLevelSpecificationRefVOListToServiceLevelSpecificationRefList( serviceSpecificationVO.getServiceLevelSpecification() ) );
        serviceSpecification.setServiceSpecRelationship( serviceSpecRelationshipVOListToServiceSpecificationRelationshipList( serviceSpecificationVO.getServiceSpecRelationship() ) );
        serviceSpecification.setSpecCharacteristic( characteristicSpecificationVOListToCharacteristicSpecificationList( serviceSpecificationVO.getSpecCharacteristic() ) );
        serviceSpecification.setTargetEntitySchema( targetEntitySchemaVOToTargetEntitySchema( serviceSpecificationVO.getTargetEntitySchema() ) );
        serviceSpecification.setValidFor( timePeriodVOToTimePeriod( serviceSpecificationVO.getValidFor() ) );

        return serviceSpecification;
    }

    @Override
    public ServiceSpecification map(ServiceSpecificationUpdateVO serviceSpecificationUpdateVO, String id) {
        if ( serviceSpecificationUpdateVO == null && id == null ) {
            return null;
        }

        String id1 = null;
        if ( id != null ) {
            id1 = id;
        }

        ServiceSpecification serviceSpecification = new ServiceSpecification( id1 );

        if ( serviceSpecificationUpdateVO != null ) {
            serviceSpecification.setAtBaseType( serviceSpecificationUpdateVO.getAtBaseType() );
            serviceSpecification.setAtSchemaLocation( serviceSpecificationUpdateVO.getAtSchemaLocation() );
            serviceSpecification.setAtType( serviceSpecificationUpdateVO.getAtType() );
            serviceSpecification.setDescription( serviceSpecificationUpdateVO.getDescription() );
            serviceSpecification.setIsBundle( serviceSpecificationUpdateVO.getIsBundle() );
            serviceSpecification.setLifecycleStatus( serviceSpecificationUpdateVO.getLifecycleStatus() );
            serviceSpecification.setName( serviceSpecificationUpdateVO.getName() );
            serviceSpecification.setVersion( serviceSpecificationUpdateVO.getVersion() );
            serviceSpecification.setAttachment( attachmentRefOrValueVOListToAttachmentRefOrValueList( serviceSpecificationUpdateVO.getAttachment() ) );
            serviceSpecification.setEntitySpecRelationship( entitySpecificationRelationshipVOListToEntitySpecificationRelationshipList( serviceSpecificationUpdateVO.getEntitySpecRelationship() ) );
            serviceSpecification.setFeatureSpecification( featureSpecificationVOListToFeatureSpecificationList( serviceSpecificationUpdateVO.getFeatureSpecification() ) );
            serviceSpecification.setRelatedParty( relatedPartyVOListToRelatedPartyList( serviceSpecificationUpdateVO.getRelatedParty() ) );
            serviceSpecification.setResourceSpecification( resourceSpecificationRefVOListToResourceSpecificationRefList( serviceSpecificationUpdateVO.getResourceSpecification() ) );
            serviceSpecification.setServiceLevelSpecification( serviceLevelSpecificationRefVOListToServiceLevelSpecificationRefList( serviceSpecificationUpdateVO.getServiceLevelSpecification() ) );
            serviceSpecification.setServiceSpecRelationship( serviceSpecRelationshipVOListToServiceSpecificationRelationshipList( serviceSpecificationUpdateVO.getServiceSpecRelationship() ) );
            serviceSpecification.setSpecCharacteristic( characteristicSpecificationVOListToCharacteristicSpecificationList( serviceSpecificationUpdateVO.getSpecCharacteristic() ) );
            serviceSpecification.setTargetEntitySchema( targetEntitySchemaVOToTargetEntitySchema( serviceSpecificationUpdateVO.getTargetEntitySchema() ) );
            serviceSpecification.setValidFor( timePeriodVOToTimePeriod( serviceSpecificationUpdateVO.getValidFor() ) );
        }

        return serviceSpecification;
    }

    @Override
    public EventSubscriptionVO map(Subscription subscription) {
        if ( subscription == null ) {
            return null;
        }

        EventSubscriptionVO eventSubscriptionVO = new EventSubscriptionVO();

        eventSubscriptionVO.setQuery( subscription.getRawQuery() );
        eventSubscriptionVO.setId( mapFromURI( subscription.getId() ) );
        eventSubscriptionVO.setCallback( mapFromURI( subscription.getCallback() ) );

        return eventSubscriptionVO;
    }

    protected ServiceCategoryRefVO serviceCategoryRefToServiceCategoryRefVO(ServiceCategoryRef serviceCategoryRef) {
        if ( serviceCategoryRef == null ) {
            return null;
        }

        ServiceCategoryRefVO serviceCategoryRefVO = new ServiceCategoryRefVO();

        serviceCategoryRefVO.setId( mapFromURI( serviceCategoryRef.getId() ) );
        serviceCategoryRefVO.setHref( serviceCategoryRef.getHref() );
        serviceCategoryRefVO.setName( serviceCategoryRef.getName() );
        serviceCategoryRefVO.setVersion( serviceCategoryRef.getVersion() );
        serviceCategoryRefVO.setAtBaseType( serviceCategoryRef.getAtBaseType() );
        serviceCategoryRefVO.setAtSchemaLocation( serviceCategoryRef.getAtSchemaLocation() );
        serviceCategoryRefVO.setAtType( serviceCategoryRef.getAtType() );
        serviceCategoryRefVO.setAtReferredType( serviceCategoryRef.getAtReferredType() );

        return serviceCategoryRefVO;
    }

    protected List<ServiceCategoryRefVO> serviceCategoryRefListToServiceCategoryRefVOList(List<ServiceCategoryRef> list) {
        if ( list == null ) {
            return null;
        }

        List<ServiceCategoryRefVO> list1 = new ArrayList<ServiceCategoryRefVO>( list.size() );
        for ( ServiceCategoryRef serviceCategoryRef : list ) {
            list1.add( serviceCategoryRefToServiceCategoryRefVO( serviceCategoryRef ) );
        }

        return list1;
    }

    protected RelatedPartyVO relatedPartyToRelatedPartyVO(RelatedParty relatedParty) {
        if ( relatedParty == null ) {
            return null;
        }

        RelatedPartyVO relatedPartyVO = new RelatedPartyVO();

        relatedPartyVO.setId( mapFromURI( relatedParty.getId() ) );
        relatedPartyVO.setHref( relatedParty.getHref() );
        relatedPartyVO.setName( relatedParty.getName() );
        relatedPartyVO.setRole( relatedParty.getRole() );
        relatedPartyVO.setAtBaseType( relatedParty.getAtBaseType() );
        relatedPartyVO.setAtSchemaLocation( relatedParty.getAtSchemaLocation() );
        relatedPartyVO.setAtType( relatedParty.getAtType() );
        relatedPartyVO.setAtReferredType( relatedParty.getAtReferredType() );

        return relatedPartyVO;
    }

    protected List<RelatedPartyVO> relatedPartyListToRelatedPartyVOList(List<RelatedParty> list) {
        if ( list == null ) {
            return null;
        }

        List<RelatedPartyVO> list1 = new ArrayList<RelatedPartyVO>( list.size() );
        for ( RelatedParty relatedParty : list ) {
            list1.add( relatedPartyToRelatedPartyVO( relatedParty ) );
        }

        return list1;
    }

    protected TimePeriodVO timePeriodToTimePeriodVO(TimePeriod timePeriod) {
        if ( timePeriod == null ) {
            return null;
        }

        TimePeriodVO timePeriodVO = new TimePeriodVO();

        timePeriodVO.setEndDateTime( timePeriod.getEndDateTime() );
        timePeriodVO.setStartDateTime( timePeriod.getStartDateTime() );

        return timePeriodVO;
    }

    protected ServiceCategoryRef serviceCategoryRefVOToServiceCategoryRef(ServiceCategoryRefVO serviceCategoryRefVO) {
        if ( serviceCategoryRefVO == null ) {
            return null;
        }

        String id = null;

        id = serviceCategoryRefVO.getId();

        ServiceCategoryRef serviceCategoryRef = new ServiceCategoryRef( id );

        serviceCategoryRef.setAtBaseType( serviceCategoryRefVO.getAtBaseType() );
        serviceCategoryRef.setAtSchemaLocation( serviceCategoryRefVO.getAtSchemaLocation() );
        serviceCategoryRef.setAtType( serviceCategoryRefVO.getAtType() );
        serviceCategoryRef.setHref( serviceCategoryRefVO.getHref() );
        serviceCategoryRef.setName( serviceCategoryRefVO.getName() );
        serviceCategoryRef.setAtReferredType( serviceCategoryRefVO.getAtReferredType() );
        serviceCategoryRef.setVersion( serviceCategoryRefVO.getVersion() );

        return serviceCategoryRef;
    }

    protected List<ServiceCategoryRef> serviceCategoryRefVOListToServiceCategoryRefList(List<ServiceCategoryRefVO> list) {
        if ( list == null ) {
            return null;
        }

        List<ServiceCategoryRef> list1 = new ArrayList<ServiceCategoryRef>( list.size() );
        for ( ServiceCategoryRefVO serviceCategoryRefVO : list ) {
            list1.add( serviceCategoryRefVOToServiceCategoryRef( serviceCategoryRefVO ) );
        }

        return list1;
    }

    protected RelatedParty relatedPartyVOToRelatedParty(RelatedPartyVO relatedPartyVO) {
        if ( relatedPartyVO == null ) {
            return null;
        }

        String id = null;

        id = relatedPartyVO.getId();

        RelatedParty relatedParty = new RelatedParty( id );

        relatedParty.setAtBaseType( relatedPartyVO.getAtBaseType() );
        relatedParty.setAtSchemaLocation( relatedPartyVO.getAtSchemaLocation() );
        relatedParty.setAtType( relatedPartyVO.getAtType() );
        relatedParty.setHref( relatedPartyVO.getHref() );
        relatedParty.setAtReferredType( relatedPartyVO.getAtReferredType() );
        relatedParty.setName( relatedPartyVO.getName() );
        relatedParty.setRole( relatedPartyVO.getRole() );

        return relatedParty;
    }

    protected List<RelatedParty> relatedPartyVOListToRelatedPartyList(List<RelatedPartyVO> list) {
        if ( list == null ) {
            return null;
        }

        List<RelatedParty> list1 = new ArrayList<RelatedParty>( list.size() );
        for ( RelatedPartyVO relatedPartyVO : list ) {
            list1.add( relatedPartyVOToRelatedParty( relatedPartyVO ) );
        }

        return list1;
    }

    protected TimePeriod timePeriodVOToTimePeriod(TimePeriodVO timePeriodVO) {
        if ( timePeriodVO == null ) {
            return null;
        }

        TimePeriod timePeriod = new TimePeriod();

        timePeriod.setEndDateTime( timePeriodVO.getEndDateTime() );
        timePeriod.setStartDateTime( timePeriodVO.getStartDateTime() );

        return timePeriod;
    }

    protected ServiceSpecificationRefVO serviceSpecificationRefToServiceSpecificationRefVO(ServiceSpecificationRef serviceSpecificationRef) {
        if ( serviceSpecificationRef == null ) {
            return null;
        }

        ServiceSpecificationRefVO serviceSpecificationRefVO = new ServiceSpecificationRefVO();

        serviceSpecificationRefVO.setId( mapFromURI( serviceSpecificationRef.getId() ) );
        serviceSpecificationRefVO.setHref( serviceSpecificationRef.getHref() );
        serviceSpecificationRefVO.setName( serviceSpecificationRef.getName() );
        serviceSpecificationRefVO.setVersion( serviceSpecificationRef.getVersion() );
        serviceSpecificationRefVO.setAtBaseType( serviceSpecificationRef.getAtBaseType() );
        serviceSpecificationRefVO.setAtSchemaLocation( serviceSpecificationRef.getAtSchemaLocation() );
        serviceSpecificationRefVO.setAtType( serviceSpecificationRef.getAtType() );
        serviceSpecificationRefVO.setAtReferredType( serviceSpecificationRef.getAtReferredType() );

        return serviceSpecificationRefVO;
    }

    protected ServiceSpecificationRef serviceSpecificationRefVOToServiceSpecificationRef(ServiceSpecificationRefVO serviceSpecificationRefVO) {
        if ( serviceSpecificationRefVO == null ) {
            return null;
        }

        String id = null;

        id = serviceSpecificationRefVO.getId();

        ServiceSpecificationRef serviceSpecificationRef = new ServiceSpecificationRef( id );

        serviceSpecificationRef.setAtBaseType( serviceSpecificationRefVO.getAtBaseType() );
        serviceSpecificationRef.setAtSchemaLocation( serviceSpecificationRefVO.getAtSchemaLocation() );
        serviceSpecificationRef.setAtType( serviceSpecificationRefVO.getAtType() );
        serviceSpecificationRef.setHref( serviceSpecificationRefVO.getHref() );
        serviceSpecificationRef.setName( serviceSpecificationRefVO.getName() );
        serviceSpecificationRef.setAtReferredType( serviceSpecificationRefVO.getAtReferredType() );
        serviceSpecificationRef.setVersion( serviceSpecificationRefVO.getVersion() );

        return serviceSpecificationRef;
    }

    protected ServiceCandidateRefVO serviceCandidateRefToServiceCandidateRefVO(ServiceCandidateRef serviceCandidateRef) {
        if ( serviceCandidateRef == null ) {
            return null;
        }

        ServiceCandidateRefVO serviceCandidateRefVO = new ServiceCandidateRefVO();

        serviceCandidateRefVO.setId( mapFromURI( serviceCandidateRef.getId() ) );
        serviceCandidateRefVO.setHref( serviceCandidateRef.getHref() );
        serviceCandidateRefVO.setName( serviceCandidateRef.getName() );
        serviceCandidateRefVO.setVersion( serviceCandidateRef.getVersion() );
        serviceCandidateRefVO.setAtBaseType( serviceCandidateRef.getAtBaseType() );
        serviceCandidateRefVO.setAtSchemaLocation( serviceCandidateRef.getAtSchemaLocation() );
        serviceCandidateRefVO.setAtType( serviceCandidateRef.getAtType() );
        serviceCandidateRefVO.setAtReferredType( serviceCandidateRef.getAtReferredType() );

        return serviceCandidateRefVO;
    }

    protected List<ServiceCandidateRefVO> serviceCandidateRefListToServiceCandidateRefVOList(List<ServiceCandidateRef> list) {
        if ( list == null ) {
            return null;
        }

        List<ServiceCandidateRefVO> list1 = new ArrayList<ServiceCandidateRefVO>( list.size() );
        for ( ServiceCandidateRef serviceCandidateRef : list ) {
            list1.add( serviceCandidateRefToServiceCandidateRefVO( serviceCandidateRef ) );
        }

        return list1;
    }

    protected ServiceCandidateRef serviceCandidateRefVOToServiceCandidateRef(ServiceCandidateRefVO serviceCandidateRefVO) {
        if ( serviceCandidateRefVO == null ) {
            return null;
        }

        URI id = null;

        id = mapToURI( serviceCandidateRefVO.getId() );

        ServiceCandidateRef serviceCandidateRef = new ServiceCandidateRef( id );

        serviceCandidateRef.setAtBaseType( serviceCandidateRefVO.getAtBaseType() );
        serviceCandidateRef.setAtSchemaLocation( serviceCandidateRefVO.getAtSchemaLocation() );
        serviceCandidateRef.setAtType( serviceCandidateRefVO.getAtType() );
        serviceCandidateRef.setHref( serviceCandidateRefVO.getHref() );
        serviceCandidateRef.setName( serviceCandidateRefVO.getName() );
        serviceCandidateRef.setAtReferredType( serviceCandidateRefVO.getAtReferredType() );
        serviceCandidateRef.setVersion( serviceCandidateRefVO.getVersion() );

        return serviceCandidateRef;
    }

    protected List<ServiceCandidateRef> serviceCandidateRefVOListToServiceCandidateRefList(List<ServiceCandidateRefVO> list) {
        if ( list == null ) {
            return null;
        }

        List<ServiceCandidateRef> list1 = new ArrayList<ServiceCandidateRef>( list.size() );
        for ( ServiceCandidateRefVO serviceCandidateRefVO : list ) {
            list1.add( serviceCandidateRefVOToServiceCandidateRef( serviceCandidateRefVO ) );
        }

        return list1;
    }

    protected QuantityVO quantityToQuantityVO(Quantity quantity) {
        if ( quantity == null ) {
            return null;
        }

        QuantityVO quantityVO = new QuantityVO();

        quantityVO.setAmount( quantity.getAmount() );
        quantityVO.setUnits( quantity.getUnits() );

        return quantityVO;
    }

    protected AttachmentRefOrValueVO attachmentRefOrValueToAttachmentRefOrValueVO(AttachmentRefOrValue attachmentRefOrValue) {
        if ( attachmentRefOrValue == null ) {
            return null;
        }

        AttachmentRefOrValueVO attachmentRefOrValueVO = new AttachmentRefOrValueVO();

        attachmentRefOrValueVO.setId( mapFromURI( attachmentRefOrValue.getId() ) );
        attachmentRefOrValueVO.setHref( attachmentRefOrValue.getHref() );
        attachmentRefOrValueVO.setAttachmentType( attachmentRefOrValue.getAttachmentType() );
        attachmentRefOrValueVO.setContent( attachmentRefOrValue.getContent() );
        attachmentRefOrValueVO.setDescription( attachmentRefOrValue.getDescription() );
        attachmentRefOrValueVO.setMimeType( attachmentRefOrValue.getMimeType() );
        attachmentRefOrValueVO.setName( attachmentRefOrValue.getName() );
        attachmentRefOrValueVO.setUrl( mapToURI( map( attachmentRefOrValue.getUrl() ) ) );
        attachmentRefOrValueVO.setSize( quantityToQuantityVO( attachmentRefOrValue.getSize() ) );
        attachmentRefOrValueVO.setValidFor( timePeriodToTimePeriodVO( attachmentRefOrValue.getValidFor() ) );
        attachmentRefOrValueVO.setAtBaseType( attachmentRefOrValue.getAtBaseType() );
        attachmentRefOrValueVO.setAtSchemaLocation( attachmentRefOrValue.getAtSchemaLocation() );
        attachmentRefOrValueVO.setAtType( attachmentRefOrValue.getAtType() );
        attachmentRefOrValueVO.setAtReferredType( attachmentRefOrValue.getAtReferredType() );

        return attachmentRefOrValueVO;
    }

    protected List<AttachmentRefOrValueVO> attachmentRefOrValueListToAttachmentRefOrValueVOList(List<AttachmentRefOrValue> list) {
        if ( list == null ) {
            return null;
        }

        List<AttachmentRefOrValueVO> list1 = new ArrayList<AttachmentRefOrValueVO>( list.size() );
        for ( AttachmentRefOrValue attachmentRefOrValue : list ) {
            list1.add( attachmentRefOrValueToAttachmentRefOrValueVO( attachmentRefOrValue ) );
        }

        return list1;
    }

    protected AssociationSpecificationRefVO associationSpecificationRefToAssociationSpecificationRefVO(AssociationSpecificationRef associationSpecificationRef) {
        if ( associationSpecificationRef == null ) {
            return null;
        }

        AssociationSpecificationRefVO associationSpecificationRefVO = new AssociationSpecificationRefVO();

        associationSpecificationRefVO.setId( mapFromURI( associationSpecificationRef.getId() ) );
        associationSpecificationRefVO.setHref( associationSpecificationRef.getHref() );
        associationSpecificationRefVO.setName( associationSpecificationRef.getName() );
        associationSpecificationRefVO.setAtBaseType( associationSpecificationRef.getAtBaseType() );
        associationSpecificationRefVO.setAtSchemaLocation( associationSpecificationRef.getAtSchemaLocation() );
        associationSpecificationRefVO.setAtType( associationSpecificationRef.getAtType() );
        associationSpecificationRefVO.setAtReferredType( associationSpecificationRef.getAtReferredType() );

        return associationSpecificationRefVO;
    }

    protected EntitySpecificationRelationshipVO entitySpecificationRelationshipToEntitySpecificationRelationshipVO(EntitySpecificationRelationship entitySpecificationRelationship) {
        if ( entitySpecificationRelationship == null ) {
            return null;
        }

        EntitySpecificationRelationshipVO entitySpecificationRelationshipVO = new EntitySpecificationRelationshipVO();

        entitySpecificationRelationshipVO.setId( mapFromURI( entitySpecificationRelationship.getId() ) );
        entitySpecificationRelationshipVO.setHref( entitySpecificationRelationship.getHref() );
        entitySpecificationRelationshipVO.setName( entitySpecificationRelationship.getName() );
        entitySpecificationRelationshipVO.setRelationshipType( entitySpecificationRelationship.getRelationshipType() );
        entitySpecificationRelationshipVO.setRole( entitySpecificationRelationship.getRole() );
        entitySpecificationRelationshipVO.setAssociationSpec( associationSpecificationRefToAssociationSpecificationRefVO( entitySpecificationRelationship.getAssociationSpec() ) );
        entitySpecificationRelationshipVO.setValidFor( timePeriodToTimePeriodVO( entitySpecificationRelationship.getValidFor() ) );
        entitySpecificationRelationshipVO.setAtBaseType( entitySpecificationRelationship.getAtBaseType() );
        entitySpecificationRelationshipVO.setAtSchemaLocation( entitySpecificationRelationship.getAtSchemaLocation() );
        entitySpecificationRelationshipVO.setAtType( entitySpecificationRelationship.getAtType() );
        entitySpecificationRelationshipVO.setAtReferredType( entitySpecificationRelationship.getAtReferredType() );

        return entitySpecificationRelationshipVO;
    }

    protected List<EntitySpecificationRelationshipVO> entitySpecificationRelationshipListToEntitySpecificationRelationshipVOList(List<EntitySpecificationRelationship> list) {
        if ( list == null ) {
            return null;
        }

        List<EntitySpecificationRelationshipVO> list1 = new ArrayList<EntitySpecificationRelationshipVO>( list.size() );
        for ( EntitySpecificationRelationship entitySpecificationRelationship : list ) {
            list1.add( entitySpecificationRelationshipToEntitySpecificationRelationshipVO( entitySpecificationRelationship ) );
        }

        return list1;
    }

    protected ConstraintRefVO constraintRefToConstraintRefVO(ConstraintRef constraintRef) {
        if ( constraintRef == null ) {
            return null;
        }

        ConstraintRefVO constraintRefVO = new ConstraintRefVO();

        constraintRefVO.setId( mapFromURI( constraintRef.getId() ) );
        constraintRefVO.setHref( constraintRef.getHref() );
        constraintRefVO.setName( constraintRef.getName() );
        constraintRefVO.setVersion( constraintRef.getVersion() );
        constraintRefVO.setAtBaseType( constraintRef.getAtBaseType() );
        constraintRefVO.setAtSchemaLocation( constraintRef.getAtSchemaLocation() );
        constraintRefVO.setAtType( constraintRef.getAtType() );
        constraintRefVO.setAtReferredType( constraintRef.getAtReferredType() );

        return constraintRefVO;
    }

    protected List<ConstraintRefVO> constraintRefListToConstraintRefVOList(List<ConstraintRef> list) {
        if ( list == null ) {
            return null;
        }

        List<ConstraintRefVO> list1 = new ArrayList<ConstraintRefVO>( list.size() );
        for ( ConstraintRef constraintRef : list ) {
            list1.add( constraintRefToConstraintRefVO( constraintRef ) );
        }

        return list1;
    }

    protected FeatureSpecificationCharacteristicRelationshipVO featureSpecificationCharacteristicRelationshipToFeatureSpecificationCharacteristicRelationshipVO(FeatureSpecificationCharacteristicRelationship featureSpecificationCharacteristicRelationship) {
        if ( featureSpecificationCharacteristicRelationship == null ) {
            return null;
        }

        FeatureSpecificationCharacteristicRelationshipVO featureSpecificationCharacteristicRelationshipVO = new FeatureSpecificationCharacteristicRelationshipVO();

        featureSpecificationCharacteristicRelationshipVO.setCharacteristicId( featureSpecificationCharacteristicRelationship.getCharacteristicId() );
        featureSpecificationCharacteristicRelationshipVO.setFeatureId( featureSpecificationCharacteristicRelationship.getFeatureId() );
        featureSpecificationCharacteristicRelationshipVO.setName( featureSpecificationCharacteristicRelationship.getName() );
        featureSpecificationCharacteristicRelationshipVO.setRelationshipType( featureSpecificationCharacteristicRelationship.getRelationshipType() );
        featureSpecificationCharacteristicRelationshipVO.setResourceSpecificationHref( mapToURI( featureSpecificationCharacteristicRelationship.getResourceSpecificationHref() ) );
        featureSpecificationCharacteristicRelationshipVO.setResourceSpecificationId( mapFromResourceSpecificationRef( featureSpecificationCharacteristicRelationship.getResourceSpecificationId() ) );
        featureSpecificationCharacteristicRelationshipVO.setValidFor( timePeriodToTimePeriodVO( featureSpecificationCharacteristicRelationship.getValidFor() ) );

        return featureSpecificationCharacteristicRelationshipVO;
    }

    protected List<FeatureSpecificationCharacteristicRelationshipVO> featureSpecificationCharacteristicRelationshipListToFeatureSpecificationCharacteristicRelationshipVOList(List<FeatureSpecificationCharacteristicRelationship> list) {
        if ( list == null ) {
            return null;
        }

        List<FeatureSpecificationCharacteristicRelationshipVO> list1 = new ArrayList<FeatureSpecificationCharacteristicRelationshipVO>( list.size() );
        for ( FeatureSpecificationCharacteristicRelationship featureSpecificationCharacteristicRelationship : list ) {
            list1.add( featureSpecificationCharacteristicRelationshipToFeatureSpecificationCharacteristicRelationshipVO( featureSpecificationCharacteristicRelationship ) );
        }

        return list1;
    }

    protected CharacteristicValueSpecificationVO characteristicValueToCharacteristicValueSpecificationVO(CharacteristicValue characteristicValue) {
        if ( characteristicValue == null ) {
            return null;
        }

        CharacteristicValueSpecificationVO characteristicValueSpecificationVO = new CharacteristicValueSpecificationVO();

        characteristicValueSpecificationVO.setIsDefault( characteristicValue.getIsDefault() );
        characteristicValueSpecificationVO.setRangeInterval( characteristicValue.getRangeInterval() );
        characteristicValueSpecificationVO.setRegex( characteristicValue.getRegex() );
        characteristicValueSpecificationVO.setUnitOfMeasure( characteristicValue.getUnitOfMeasure() );
        characteristicValueSpecificationVO.setValueFrom( characteristicValue.getValueFrom() );
        characteristicValueSpecificationVO.setValueTo( characteristicValue.getValueTo() );
        characteristicValueSpecificationVO.setValueType( characteristicValue.getValueType() );
        characteristicValueSpecificationVO.setValidFor( timePeriodToTimePeriodVO( characteristicValue.getValidFor() ) );
        characteristicValueSpecificationVO.setValue( characteristicValue.getValue() );
        characteristicValueSpecificationVO.setAtBaseType( characteristicValue.getAtBaseType() );
        characteristicValueSpecificationVO.setAtSchemaLocation( characteristicValue.getAtSchemaLocation() );
        characteristicValueSpecificationVO.setAtType( characteristicValue.getAtType() );

        return characteristicValueSpecificationVO;
    }

    protected List<CharacteristicValueSpecificationVO> characteristicValueListToCharacteristicValueSpecificationVOList(List<CharacteristicValue> list) {
        if ( list == null ) {
            return null;
        }

        List<CharacteristicValueSpecificationVO> list1 = new ArrayList<CharacteristicValueSpecificationVO>( list.size() );
        for ( CharacteristicValue characteristicValue : list ) {
            list1.add( characteristicValueToCharacteristicValueSpecificationVO( characteristicValue ) );
        }

        return list1;
    }

    protected FeatureSpecificationCharacteristicVO featureSpecificationCharacteristicToFeatureSpecificationCharacteristicVO(FeatureSpecificationCharacteristic featureSpecificationCharacteristic) {
        if ( featureSpecificationCharacteristic == null ) {
            return null;
        }

        FeatureSpecificationCharacteristicVO featureSpecificationCharacteristicVO = new FeatureSpecificationCharacteristicVO();

        featureSpecificationCharacteristicVO.setId( featureSpecificationCharacteristic.getId() );
        featureSpecificationCharacteristicVO.setConfigurable( featureSpecificationCharacteristic.getConfigurable() );
        featureSpecificationCharacteristicVO.setDescription( featureSpecificationCharacteristic.getDescription() );
        featureSpecificationCharacteristicVO.setExtensible( featureSpecificationCharacteristic.getExtensible() );
        featureSpecificationCharacteristicVO.setIsUnique( featureSpecificationCharacteristic.getIsUnique() );
        featureSpecificationCharacteristicVO.setMaxCardinality( featureSpecificationCharacteristic.getMaxCardinality() );
        featureSpecificationCharacteristicVO.setMinCardinality( featureSpecificationCharacteristic.getMinCardinality() );
        featureSpecificationCharacteristicVO.setName( featureSpecificationCharacteristic.getName() );
        featureSpecificationCharacteristicVO.setRegex( featureSpecificationCharacteristic.getRegex() );
        featureSpecificationCharacteristicVO.setValueType( featureSpecificationCharacteristic.getValueType() );
        featureSpecificationCharacteristicVO.setFeatureSpecCharRelationship( featureSpecificationCharacteristicRelationshipListToFeatureSpecificationCharacteristicRelationshipVOList( featureSpecificationCharacteristic.getFeatureSpecCharRelationship() ) );
        featureSpecificationCharacteristicVO.setFeatureSpecCharacteristicValue( characteristicValueListToCharacteristicValueSpecificationVOList( featureSpecificationCharacteristic.getFeatureSpecCharacteristicValue() ) );
        featureSpecificationCharacteristicVO.setValidFor( timePeriodToTimePeriodVO( featureSpecificationCharacteristic.getValidFor() ) );
        featureSpecificationCharacteristicVO.setAtBaseType( featureSpecificationCharacteristic.getAtBaseType() );
        featureSpecificationCharacteristicVO.setAtSchemaLocation( featureSpecificationCharacteristic.getAtSchemaLocation() );
        featureSpecificationCharacteristicVO.setAtType( featureSpecificationCharacteristic.getAtType() );
        featureSpecificationCharacteristicVO.setAtValueSchemaLocation( featureSpecificationCharacteristic.getAtValueSchemaLocation() );

        return featureSpecificationCharacteristicVO;
    }

    protected List<FeatureSpecificationCharacteristicVO> featureSpecificationCharacteristicListToFeatureSpecificationCharacteristicVOList(List<FeatureSpecificationCharacteristic> list) {
        if ( list == null ) {
            return null;
        }

        List<FeatureSpecificationCharacteristicVO> list1 = new ArrayList<FeatureSpecificationCharacteristicVO>( list.size() );
        for ( FeatureSpecificationCharacteristic featureSpecificationCharacteristic : list ) {
            list1.add( featureSpecificationCharacteristicToFeatureSpecificationCharacteristicVO( featureSpecificationCharacteristic ) );
        }

        return list1;
    }

    protected FeatureSpecificationRelationshipVO featureSpecificationRelationshipToFeatureSpecificationRelationshipVO(FeatureSpecificationRelationship featureSpecificationRelationship) {
        if ( featureSpecificationRelationship == null ) {
            return null;
        }

        FeatureSpecificationRelationshipVO featureSpecificationRelationshipVO = new FeatureSpecificationRelationshipVO();

        featureSpecificationRelationshipVO.setFeatureId( featureSpecificationRelationship.getFeatureId() );
        featureSpecificationRelationshipVO.setName( featureSpecificationRelationship.getName() );
        featureSpecificationRelationshipVO.setParentSpecificationHref( featureSpecificationRelationship.getParentSpecificationHref() );
        featureSpecificationRelationshipVO.setParentSpecificationId( featureSpecificationRelationship.getParentSpecificationId() );
        featureSpecificationRelationshipVO.setRelationshipType( featureSpecificationRelationship.getRelationshipType() );
        featureSpecificationRelationshipVO.setValidFor( timePeriodToTimePeriodVO( featureSpecificationRelationship.getValidFor() ) );

        return featureSpecificationRelationshipVO;
    }

    protected List<FeatureSpecificationRelationshipVO> featureSpecificationRelationshipListToFeatureSpecificationRelationshipVOList(List<FeatureSpecificationRelationship> list) {
        if ( list == null ) {
            return null;
        }

        List<FeatureSpecificationRelationshipVO> list1 = new ArrayList<FeatureSpecificationRelationshipVO>( list.size() );
        for ( FeatureSpecificationRelationship featureSpecificationRelationship : list ) {
            list1.add( featureSpecificationRelationshipToFeatureSpecificationRelationshipVO( featureSpecificationRelationship ) );
        }

        return list1;
    }

    protected FeatureSpecificationVO featureSpecificationToFeatureSpecificationVO(FeatureSpecification featureSpecification) {
        if ( featureSpecification == null ) {
            return null;
        }

        FeatureSpecificationVO featureSpecificationVO = new FeatureSpecificationVO();

        featureSpecificationVO.setId( featureSpecification.getId() );
        featureSpecificationVO.setIsBundle( featureSpecification.getIsBundle() );
        featureSpecificationVO.setIsEnabled( featureSpecification.getIsEnabled() );
        featureSpecificationVO.setName( featureSpecification.getName() );
        featureSpecificationVO.setVersion( featureSpecification.getVersion() );
        featureSpecificationVO.setConstraint( constraintRefListToConstraintRefVOList( featureSpecification.getConstraint() ) );
        featureSpecificationVO.setFeatureSpecCharacteristic( featureSpecificationCharacteristicListToFeatureSpecificationCharacteristicVOList( featureSpecification.getFeatureSpecCharacteristic() ) );
        featureSpecificationVO.setFeatureSpecRelationship( featureSpecificationRelationshipListToFeatureSpecificationRelationshipVOList( featureSpecification.getFeatureSpecRelationship() ) );
        featureSpecificationVO.setValidFor( timePeriodToTimePeriodVO( featureSpecification.getValidFor() ) );

        return featureSpecificationVO;
    }

    protected List<FeatureSpecificationVO> featureSpecificationListToFeatureSpecificationVOList(List<FeatureSpecification> list) {
        if ( list == null ) {
            return null;
        }

        List<FeatureSpecificationVO> list1 = new ArrayList<FeatureSpecificationVO>( list.size() );
        for ( FeatureSpecification featureSpecification : list ) {
            list1.add( featureSpecificationToFeatureSpecificationVO( featureSpecification ) );
        }

        return list1;
    }

    protected ResourceSpecificationRefVO resourceSpecificationRefToResourceSpecificationRefVO(ResourceSpecificationRef resourceSpecificationRef) {
        if ( resourceSpecificationRef == null ) {
            return null;
        }

        ResourceSpecificationRefVO resourceSpecificationRefVO = new ResourceSpecificationRefVO();

        resourceSpecificationRefVO.setId( mapFromURI( resourceSpecificationRef.getId() ) );
        resourceSpecificationRefVO.setHref( resourceSpecificationRef.getHref() );
        resourceSpecificationRefVO.setName( resourceSpecificationRef.getName() );
        resourceSpecificationRefVO.setAtBaseType( resourceSpecificationRef.getAtBaseType() );
        resourceSpecificationRefVO.setAtSchemaLocation( resourceSpecificationRef.getAtSchemaLocation() );
        resourceSpecificationRefVO.setAtType( resourceSpecificationRef.getAtType() );
        resourceSpecificationRefVO.setAtReferredType( resourceSpecificationRef.getAtReferredType() );

        return resourceSpecificationRefVO;
    }

    protected List<ResourceSpecificationRefVO> resourceSpecificationRefListToResourceSpecificationRefVOList(List<ResourceSpecificationRef> list) {
        if ( list == null ) {
            return null;
        }

        List<ResourceSpecificationRefVO> list1 = new ArrayList<ResourceSpecificationRefVO>( list.size() );
        for ( ResourceSpecificationRef resourceSpecificationRef : list ) {
            list1.add( resourceSpecificationRefToResourceSpecificationRefVO( resourceSpecificationRef ) );
        }

        return list1;
    }

    protected ServiceLevelSpecificationRefVO serviceLevelSpecificationRefToServiceLevelSpecificationRefVO(ServiceLevelSpecificationRef serviceLevelSpecificationRef) {
        if ( serviceLevelSpecificationRef == null ) {
            return null;
        }

        ServiceLevelSpecificationRefVO serviceLevelSpecificationRefVO = new ServiceLevelSpecificationRefVO();

        serviceLevelSpecificationRefVO.setId( mapFromURI( serviceLevelSpecificationRef.getId() ) );
        serviceLevelSpecificationRefVO.setHref( serviceLevelSpecificationRef.getHref() );
        serviceLevelSpecificationRefVO.setName( serviceLevelSpecificationRef.getName() );
        serviceLevelSpecificationRefVO.setAtBaseType( serviceLevelSpecificationRef.getAtBaseType() );
        serviceLevelSpecificationRefVO.setAtSchemaLocation( serviceLevelSpecificationRef.getAtSchemaLocation() );
        serviceLevelSpecificationRefVO.setAtType( serviceLevelSpecificationRef.getAtType() );
        serviceLevelSpecificationRefVO.setAtReferredType( serviceLevelSpecificationRef.getAtReferredType() );

        return serviceLevelSpecificationRefVO;
    }

    protected List<ServiceLevelSpecificationRefVO> serviceLevelSpecificationRefListToServiceLevelSpecificationRefVOList(List<ServiceLevelSpecificationRef> list) {
        if ( list == null ) {
            return null;
        }

        List<ServiceLevelSpecificationRefVO> list1 = new ArrayList<ServiceLevelSpecificationRefVO>( list.size() );
        for ( ServiceLevelSpecificationRef serviceLevelSpecificationRef : list ) {
            list1.add( serviceLevelSpecificationRefToServiceLevelSpecificationRefVO( serviceLevelSpecificationRef ) );
        }

        return list1;
    }

    protected ServiceSpecRelationshipVO serviceSpecificationRelationshipToServiceSpecRelationshipVO(ServiceSpecificationRelationship serviceSpecificationRelationship) {
        if ( serviceSpecificationRelationship == null ) {
            return null;
        }

        ServiceSpecRelationshipVO serviceSpecRelationshipVO = new ServiceSpecRelationshipVO();

        serviceSpecRelationshipVO.setId( mapFromURI( serviceSpecificationRelationship.getId() ) );
        serviceSpecRelationshipVO.setHref( serviceSpecificationRelationship.getHref() );
        serviceSpecRelationshipVO.setName( serviceSpecificationRelationship.getName() );
        serviceSpecRelationshipVO.setRelationshipType( serviceSpecificationRelationship.getRelationshipType() );
        serviceSpecRelationshipVO.setRole( serviceSpecificationRelationship.getRole() );
        serviceSpecRelationshipVO.setValidFor( timePeriodToTimePeriodVO( serviceSpecificationRelationship.getValidFor() ) );
        serviceSpecRelationshipVO.setAtBaseType( serviceSpecificationRelationship.getAtBaseType() );
        serviceSpecRelationshipVO.setAtSchemaLocation( serviceSpecificationRelationship.getAtSchemaLocation() );
        serviceSpecRelationshipVO.setAtType( serviceSpecificationRelationship.getAtType() );
        serviceSpecRelationshipVO.setAtReferredType( serviceSpecificationRelationship.getAtReferredType() );

        return serviceSpecRelationshipVO;
    }

    protected List<ServiceSpecRelationshipVO> serviceSpecificationRelationshipListToServiceSpecRelationshipVOList(List<ServiceSpecificationRelationship> list) {
        if ( list == null ) {
            return null;
        }

        List<ServiceSpecRelationshipVO> list1 = new ArrayList<ServiceSpecRelationshipVO>( list.size() );
        for ( ServiceSpecificationRelationship serviceSpecificationRelationship : list ) {
            list1.add( serviceSpecificationRelationshipToServiceSpecRelationshipVO( serviceSpecificationRelationship ) );
        }

        return list1;
    }

    protected CharacteristicSpecificationRelationshipVO characteristicSpecificationRelationshipToCharacteristicSpecificationRelationshipVO(CharacteristicSpecificationRelationship characteristicSpecificationRelationship) {
        if ( characteristicSpecificationRelationship == null ) {
            return null;
        }

        CharacteristicSpecificationRelationshipVO characteristicSpecificationRelationshipVO = new CharacteristicSpecificationRelationshipVO();

        characteristicSpecificationRelationshipVO.setCharacteristicSpecificationId( characteristicSpecificationRelationship.getCharacteristicSpecificationId() );
        characteristicSpecificationRelationshipVO.setName( characteristicSpecificationRelationship.getName() );
        characteristicSpecificationRelationshipVO.setParentSpecificationHref( characteristicSpecificationRelationship.getParentSpecificationHref() );
        characteristicSpecificationRelationshipVO.setParentSpecificationId( characteristicSpecificationRelationship.getParentSpecificationId() );
        characteristicSpecificationRelationshipVO.setRelationshipType( characteristicSpecificationRelationship.getRelationshipType() );
        characteristicSpecificationRelationshipVO.setValidFor( timePeriodToTimePeriodVO( characteristicSpecificationRelationship.getValidFor() ) );

        return characteristicSpecificationRelationshipVO;
    }

    protected List<CharacteristicSpecificationRelationshipVO> characteristicSpecificationRelationshipListToCharacteristicSpecificationRelationshipVOList(List<CharacteristicSpecificationRelationship> list) {
        if ( list == null ) {
            return null;
        }

        List<CharacteristicSpecificationRelationshipVO> list1 = new ArrayList<CharacteristicSpecificationRelationshipVO>( list.size() );
        for ( CharacteristicSpecificationRelationship characteristicSpecificationRelationship : list ) {
            list1.add( characteristicSpecificationRelationshipToCharacteristicSpecificationRelationshipVO( characteristicSpecificationRelationship ) );
        }

        return list1;
    }

    protected CharacteristicValueSpecificationVO characteristicValueSpecificationToCharacteristicValueSpecificationVO(CharacteristicValueSpecification characteristicValueSpecification) {
        if ( characteristicValueSpecification == null ) {
            return null;
        }

        CharacteristicValueSpecificationVO characteristicValueSpecificationVO = new CharacteristicValueSpecificationVO();

        characteristicValueSpecificationVO.setIsDefault( characteristicValueSpecification.getIsDefault() );
        characteristicValueSpecificationVO.setRangeInterval( characteristicValueSpecification.getRangeInterval() );
        characteristicValueSpecificationVO.setRegex( characteristicValueSpecification.getRegex() );
        characteristicValueSpecificationVO.setUnitOfMeasure( characteristicValueSpecification.getUnitOfMeasure() );
        characteristicValueSpecificationVO.setValueFrom( characteristicValueSpecification.getValueFrom() );
        characteristicValueSpecificationVO.setValueTo( characteristicValueSpecification.getValueTo() );
        characteristicValueSpecificationVO.setValueType( characteristicValueSpecification.getValueType() );
        characteristicValueSpecificationVO.setValidFor( timePeriodToTimePeriodVO( characteristicValueSpecification.getValidFor() ) );
        characteristicValueSpecificationVO.setValue( characteristicValueSpecification.getValue() );
        characteristicValueSpecificationVO.setAtBaseType( characteristicValueSpecification.getAtBaseType() );
        characteristicValueSpecificationVO.setAtSchemaLocation( characteristicValueSpecification.getAtSchemaLocation() );
        characteristicValueSpecificationVO.setAtType( characteristicValueSpecification.getAtType() );

        return characteristicValueSpecificationVO;
    }

    protected List<CharacteristicValueSpecificationVO> characteristicValueSpecificationListToCharacteristicValueSpecificationVOList(List<CharacteristicValueSpecification> list) {
        if ( list == null ) {
            return null;
        }

        List<CharacteristicValueSpecificationVO> list1 = new ArrayList<CharacteristicValueSpecificationVO>( list.size() );
        for ( CharacteristicValueSpecification characteristicValueSpecification : list ) {
            list1.add( characteristicValueSpecificationToCharacteristicValueSpecificationVO( characteristicValueSpecification ) );
        }

        return list1;
    }

    protected CharacteristicSpecificationVO characteristicSpecificationToCharacteristicSpecificationVO(CharacteristicSpecification characteristicSpecification) {
        if ( characteristicSpecification == null ) {
            return null;
        }

        CharacteristicSpecificationVO characteristicSpecificationVO = new CharacteristicSpecificationVO();

        characteristicSpecificationVO.setId( characteristicSpecification.getId() );
        characteristicSpecificationVO.setConfigurable( characteristicSpecification.getConfigurable() );
        characteristicSpecificationVO.setDescription( characteristicSpecification.getDescription() );
        characteristicSpecificationVO.setExtensible( characteristicSpecification.getExtensible() );
        characteristicSpecificationVO.setIsUnique( characteristicSpecification.getIsUnique() );
        characteristicSpecificationVO.setMaxCardinality( characteristicSpecification.getMaxCardinality() );
        characteristicSpecificationVO.setMinCardinality( characteristicSpecification.getMinCardinality() );
        characteristicSpecificationVO.setName( characteristicSpecification.getName() );
        characteristicSpecificationVO.setRegex( characteristicSpecification.getRegex() );
        characteristicSpecificationVO.setValueType( characteristicSpecification.getValueType() );
        characteristicSpecificationVO.setCharSpecRelationship( characteristicSpecificationRelationshipListToCharacteristicSpecificationRelationshipVOList( characteristicSpecification.getCharSpecRelationship() ) );
        characteristicSpecificationVO.setCharacteristicValueSpecification( characteristicValueSpecificationListToCharacteristicValueSpecificationVOList( characteristicSpecification.getCharacteristicValueSpecification() ) );
        characteristicSpecificationVO.setValidFor( timePeriodToTimePeriodVO( characteristicSpecification.getValidFor() ) );
        characteristicSpecificationVO.setAtBaseType( characteristicSpecification.getAtBaseType() );
        characteristicSpecificationVO.setAtSchemaLocation( characteristicSpecification.getAtSchemaLocation() );
        characteristicSpecificationVO.setAtType( characteristicSpecification.getAtType() );
        characteristicSpecificationVO.setAtValueSchemaLocation( characteristicSpecification.getAtValueSchemaLocation() );

        return characteristicSpecificationVO;
    }

    protected List<CharacteristicSpecificationVO> characteristicSpecificationListToCharacteristicSpecificationVOList(List<CharacteristicSpecification> list) {
        if ( list == null ) {
            return null;
        }

        List<CharacteristicSpecificationVO> list1 = new ArrayList<CharacteristicSpecificationVO>( list.size() );
        for ( CharacteristicSpecification characteristicSpecification : list ) {
            list1.add( characteristicSpecificationToCharacteristicSpecificationVO( characteristicSpecification ) );
        }

        return list1;
    }

    protected TargetEntitySchemaVO targetEntitySchemaToTargetEntitySchemaVO(TargetEntitySchema targetEntitySchema) {
        if ( targetEntitySchema == null ) {
            return null;
        }

        TargetEntitySchemaVO targetEntitySchemaVO = new TargetEntitySchemaVO();

        targetEntitySchemaVO.setAtSchemaLocation( targetEntitySchema.getAtSchemaLocation() );
        targetEntitySchemaVO.setAtType( targetEntitySchema.getAtType() );

        return targetEntitySchemaVO;
    }

    protected Quantity quantityVOToQuantity(QuantityVO quantityVO) {
        if ( quantityVO == null ) {
            return null;
        }

        Quantity quantity = new Quantity();

        quantity.setAmount( quantityVO.getAmount() );
        quantity.setUnits( quantityVO.getUnits() );

        return quantity;
    }

    protected AttachmentRefOrValue attachmentRefOrValueVOToAttachmentRefOrValue(AttachmentRefOrValueVO attachmentRefOrValueVO) {
        if ( attachmentRefOrValueVO == null ) {
            return null;
        }

        AttachmentRefOrValue attachmentRefOrValue = new AttachmentRefOrValue();

        attachmentRefOrValue.setAtBaseType( attachmentRefOrValueVO.getAtBaseType() );
        attachmentRefOrValue.setAtSchemaLocation( attachmentRefOrValueVO.getAtSchemaLocation() );
        attachmentRefOrValue.setAtType( attachmentRefOrValueVO.getAtType() );
        attachmentRefOrValue.setId( mapToURI( attachmentRefOrValueVO.getId() ) );
        attachmentRefOrValue.setHref( attachmentRefOrValueVO.getHref() );
        attachmentRefOrValue.setAttachmentType( attachmentRefOrValueVO.getAttachmentType() );
        attachmentRefOrValue.setContent( attachmentRefOrValueVO.getContent() );
        attachmentRefOrValue.setDescription( attachmentRefOrValueVO.getDescription() );
        attachmentRefOrValue.setMimeType( attachmentRefOrValueVO.getMimeType() );
        attachmentRefOrValue.setUrl( map( mapFromURI( attachmentRefOrValueVO.getUrl() ) ) );
        attachmentRefOrValue.setSize( quantityVOToQuantity( attachmentRefOrValueVO.getSize() ) );
        attachmentRefOrValue.setValidFor( timePeriodVOToTimePeriod( attachmentRefOrValueVO.getValidFor() ) );
        attachmentRefOrValue.setName( attachmentRefOrValueVO.getName() );
        attachmentRefOrValue.setAtReferredType( attachmentRefOrValueVO.getAtReferredType() );

        return attachmentRefOrValue;
    }

    protected List<AttachmentRefOrValue> attachmentRefOrValueVOListToAttachmentRefOrValueList(List<AttachmentRefOrValueVO> list) {
        if ( list == null ) {
            return null;
        }

        List<AttachmentRefOrValue> list1 = new ArrayList<AttachmentRefOrValue>( list.size() );
        for ( AttachmentRefOrValueVO attachmentRefOrValueVO : list ) {
            list1.add( attachmentRefOrValueVOToAttachmentRefOrValue( attachmentRefOrValueVO ) );
        }

        return list1;
    }

    protected AssociationSpecificationRef associationSpecificationRefVOToAssociationSpecificationRef(AssociationSpecificationRefVO associationSpecificationRefVO) {
        if ( associationSpecificationRefVO == null ) {
            return null;
        }

        URI id = null;

        id = mapToURI( associationSpecificationRefVO.getId() );

        AssociationSpecificationRef associationSpecificationRef = new AssociationSpecificationRef( id );

        associationSpecificationRef.setAtBaseType( associationSpecificationRefVO.getAtBaseType() );
        associationSpecificationRef.setAtSchemaLocation( associationSpecificationRefVO.getAtSchemaLocation() );
        associationSpecificationRef.setAtType( associationSpecificationRefVO.getAtType() );
        associationSpecificationRef.setHref( associationSpecificationRefVO.getHref() );
        associationSpecificationRef.setName( associationSpecificationRefVO.getName() );
        associationSpecificationRef.setAtReferredType( associationSpecificationRefVO.getAtReferredType() );

        return associationSpecificationRef;
    }

    protected EntitySpecificationRelationship entitySpecificationRelationshipVOToEntitySpecificationRelationship(EntitySpecificationRelationshipVO entitySpecificationRelationshipVO) {
        if ( entitySpecificationRelationshipVO == null ) {
            return null;
        }

        URI id = null;

        id = mapToURI( entitySpecificationRelationshipVO.getId() );

        EntitySpecificationRelationship entitySpecificationRelationship = new EntitySpecificationRelationship( id );

        entitySpecificationRelationship.setAtBaseType( entitySpecificationRelationshipVO.getAtBaseType() );
        entitySpecificationRelationship.setAtSchemaLocation( entitySpecificationRelationshipVO.getAtSchemaLocation() );
        entitySpecificationRelationship.setAtType( entitySpecificationRelationshipVO.getAtType() );
        entitySpecificationRelationship.setHref( entitySpecificationRelationshipVO.getHref() );
        entitySpecificationRelationship.setName( entitySpecificationRelationshipVO.getName() );
        entitySpecificationRelationship.setAtReferredType( entitySpecificationRelationshipVO.getAtReferredType() );
        entitySpecificationRelationship.setRelationshipType( entitySpecificationRelationshipVO.getRelationshipType() );
        entitySpecificationRelationship.setRole( entitySpecificationRelationshipVO.getRole() );
        entitySpecificationRelationship.setAssociationSpec( associationSpecificationRefVOToAssociationSpecificationRef( entitySpecificationRelationshipVO.getAssociationSpec() ) );
        entitySpecificationRelationship.setValidFor( timePeriodVOToTimePeriod( entitySpecificationRelationshipVO.getValidFor() ) );

        return entitySpecificationRelationship;
    }

    protected List<EntitySpecificationRelationship> entitySpecificationRelationshipVOListToEntitySpecificationRelationshipList(List<EntitySpecificationRelationshipVO> list) {
        if ( list == null ) {
            return null;
        }

        List<EntitySpecificationRelationship> list1 = new ArrayList<EntitySpecificationRelationship>( list.size() );
        for ( EntitySpecificationRelationshipVO entitySpecificationRelationshipVO : list ) {
            list1.add( entitySpecificationRelationshipVOToEntitySpecificationRelationship( entitySpecificationRelationshipVO ) );
        }

        return list1;
    }

    protected ConstraintRef constraintRefVOToConstraintRef(ConstraintRefVO constraintRefVO) {
        if ( constraintRefVO == null ) {
            return null;
        }

        String id = null;

        id = constraintRefVO.getId();

        ConstraintRef constraintRef = new ConstraintRef( id );

        constraintRef.setAtBaseType( constraintRefVO.getAtBaseType() );
        constraintRef.setAtSchemaLocation( constraintRefVO.getAtSchemaLocation() );
        constraintRef.setAtType( constraintRefVO.getAtType() );
        constraintRef.setHref( constraintRefVO.getHref() );
        constraintRef.setName( constraintRefVO.getName() );
        constraintRef.setAtReferredType( constraintRefVO.getAtReferredType() );
        constraintRef.setVersion( constraintRefVO.getVersion() );

        return constraintRef;
    }

    protected List<ConstraintRef> constraintRefVOListToConstraintRefList(List<ConstraintRefVO> list) {
        if ( list == null ) {
            return null;
        }

        List<ConstraintRef> list1 = new ArrayList<ConstraintRef>( list.size() );
        for ( ConstraintRefVO constraintRefVO : list ) {
            list1.add( constraintRefVOToConstraintRef( constraintRefVO ) );
        }

        return list1;
    }

    protected CharacteristicValue characteristicValueSpecificationVOToCharacteristicValue(CharacteristicValueSpecificationVO characteristicValueSpecificationVO) {
        if ( characteristicValueSpecificationVO == null ) {
            return null;
        }

        CharacteristicValue characteristicValue = new CharacteristicValue();

        characteristicValue.setIsDefault( characteristicValueSpecificationVO.getIsDefault() );
        characteristicValue.setRangeInterval( characteristicValueSpecificationVO.getRangeInterval() );
        characteristicValue.setRegex( characteristicValueSpecificationVO.getRegex() );
        characteristicValue.setUnitOfMeasure( characteristicValueSpecificationVO.getUnitOfMeasure() );
        characteristicValue.setValueFrom( characteristicValueSpecificationVO.getValueFrom() );
        characteristicValue.setValueTo( characteristicValueSpecificationVO.getValueTo() );
        characteristicValue.setValueType( characteristicValueSpecificationVO.getValueType() );
        characteristicValue.setValue( characteristicValueSpecificationVO.getValue() );
        characteristicValue.setValidFor( timePeriodVOToTimePeriod( characteristicValueSpecificationVO.getValidFor() ) );
        characteristicValue.setAtBaseType( characteristicValueSpecificationVO.getAtBaseType() );
        characteristicValue.setAtSchemaLocation( characteristicValueSpecificationVO.getAtSchemaLocation() );
        characteristicValue.setAtType( characteristicValueSpecificationVO.getAtType() );

        return characteristicValue;
    }

    protected List<CharacteristicValue> characteristicValueSpecificationVOListToCharacteristicValueList(List<CharacteristicValueSpecificationVO> list) {
        if ( list == null ) {
            return null;
        }

        List<CharacteristicValue> list1 = new ArrayList<CharacteristicValue>( list.size() );
        for ( CharacteristicValueSpecificationVO characteristicValueSpecificationVO : list ) {
            list1.add( characteristicValueSpecificationVOToCharacteristicValue( characteristicValueSpecificationVO ) );
        }

        return list1;
    }

    protected FeatureSpecificationCharacteristicRelationship featureSpecificationCharacteristicRelationshipVOToFeatureSpecificationCharacteristicRelationship(FeatureSpecificationCharacteristicRelationshipVO featureSpecificationCharacteristicRelationshipVO) {
        if ( featureSpecificationCharacteristicRelationshipVO == null ) {
            return null;
        }

        FeatureSpecificationCharacteristicRelationship featureSpecificationCharacteristicRelationship = new FeatureSpecificationCharacteristicRelationship();

        featureSpecificationCharacteristicRelationship.setCharacteristicId( featureSpecificationCharacteristicRelationshipVO.getCharacteristicId() );
        featureSpecificationCharacteristicRelationship.setFeatureId( featureSpecificationCharacteristicRelationshipVO.getFeatureId() );
        featureSpecificationCharacteristicRelationship.setName( featureSpecificationCharacteristicRelationshipVO.getName() );
        featureSpecificationCharacteristicRelationship.setRelationshipType( featureSpecificationCharacteristicRelationshipVO.getRelationshipType() );
        featureSpecificationCharacteristicRelationship.setResourceSpecificationHref( mapFromURI( featureSpecificationCharacteristicRelationshipVO.getResourceSpecificationHref() ) );
        featureSpecificationCharacteristicRelationship.setResourceSpecificationId( mapFromResourceSpecId( featureSpecificationCharacteristicRelationshipVO.getResourceSpecificationId() ) );
        featureSpecificationCharacteristicRelationship.setValidFor( timePeriodVOToTimePeriod( featureSpecificationCharacteristicRelationshipVO.getValidFor() ) );

        return featureSpecificationCharacteristicRelationship;
    }

    protected List<FeatureSpecificationCharacteristicRelationship> featureSpecificationCharacteristicRelationshipVOListToFeatureSpecificationCharacteristicRelationshipList(List<FeatureSpecificationCharacteristicRelationshipVO> list) {
        if ( list == null ) {
            return null;
        }

        List<FeatureSpecificationCharacteristicRelationship> list1 = new ArrayList<FeatureSpecificationCharacteristicRelationship>( list.size() );
        for ( FeatureSpecificationCharacteristicRelationshipVO featureSpecificationCharacteristicRelationshipVO : list ) {
            list1.add( featureSpecificationCharacteristicRelationshipVOToFeatureSpecificationCharacteristicRelationship( featureSpecificationCharacteristicRelationshipVO ) );
        }

        return list1;
    }

    protected FeatureSpecificationCharacteristic featureSpecificationCharacteristicVOToFeatureSpecificationCharacteristic(FeatureSpecificationCharacteristicVO featureSpecificationCharacteristicVO) {
        if ( featureSpecificationCharacteristicVO == null ) {
            return null;
        }

        FeatureSpecificationCharacteristic featureSpecificationCharacteristic = new FeatureSpecificationCharacteristic();

        featureSpecificationCharacteristic.setId( featureSpecificationCharacteristicVO.getId() );
        featureSpecificationCharacteristic.setDescription( featureSpecificationCharacteristicVO.getDescription() );
        featureSpecificationCharacteristic.setConfigurable( featureSpecificationCharacteristicVO.getConfigurable() );
        featureSpecificationCharacteristic.setExtensible( featureSpecificationCharacteristicVO.getExtensible() );
        featureSpecificationCharacteristic.setIsUnique( featureSpecificationCharacteristicVO.getIsUnique() );
        featureSpecificationCharacteristic.setMaxCardinality( featureSpecificationCharacteristicVO.getMaxCardinality() );
        featureSpecificationCharacteristic.setMinCardinality( featureSpecificationCharacteristicVO.getMinCardinality() );
        featureSpecificationCharacteristic.setName( featureSpecificationCharacteristicVO.getName() );
        featureSpecificationCharacteristic.setRegex( featureSpecificationCharacteristicVO.getRegex() );
        featureSpecificationCharacteristic.setValueType( featureSpecificationCharacteristicVO.getValueType() );
        featureSpecificationCharacteristic.setFeatureSpecCharacteristicValue( characteristicValueSpecificationVOListToCharacteristicValueList( featureSpecificationCharacteristicVO.getFeatureSpecCharacteristicValue() ) );
        featureSpecificationCharacteristic.setFeatureSpecCharRelationship( featureSpecificationCharacteristicRelationshipVOListToFeatureSpecificationCharacteristicRelationshipList( featureSpecificationCharacteristicVO.getFeatureSpecCharRelationship() ) );
        featureSpecificationCharacteristic.setValidFor( timePeriodVOToTimePeriod( featureSpecificationCharacteristicVO.getValidFor() ) );
        featureSpecificationCharacteristic.setAtBaseType( featureSpecificationCharacteristicVO.getAtBaseType() );
        featureSpecificationCharacteristic.setAtSchemaLocation( featureSpecificationCharacteristicVO.getAtSchemaLocation() );
        featureSpecificationCharacteristic.setAtType( featureSpecificationCharacteristicVO.getAtType() );
        featureSpecificationCharacteristic.setAtValueSchemaLocation( featureSpecificationCharacteristicVO.getAtValueSchemaLocation() );

        return featureSpecificationCharacteristic;
    }

    protected List<FeatureSpecificationCharacteristic> featureSpecificationCharacteristicVOListToFeatureSpecificationCharacteristicList(List<FeatureSpecificationCharacteristicVO> list) {
        if ( list == null ) {
            return null;
        }

        List<FeatureSpecificationCharacteristic> list1 = new ArrayList<FeatureSpecificationCharacteristic>( list.size() );
        for ( FeatureSpecificationCharacteristicVO featureSpecificationCharacteristicVO : list ) {
            list1.add( featureSpecificationCharacteristicVOToFeatureSpecificationCharacteristic( featureSpecificationCharacteristicVO ) );
        }

        return list1;
    }

    protected FeatureSpecificationRelationship featureSpecificationRelationshipVOToFeatureSpecificationRelationship(FeatureSpecificationRelationshipVO featureSpecificationRelationshipVO) {
        if ( featureSpecificationRelationshipVO == null ) {
            return null;
        }

        FeatureSpecificationRelationship featureSpecificationRelationship = new FeatureSpecificationRelationship();

        featureSpecificationRelationship.setFeatureId( featureSpecificationRelationshipVO.getFeatureId() );
        featureSpecificationRelationship.setName( featureSpecificationRelationshipVO.getName() );
        featureSpecificationRelationship.setRelationshipType( featureSpecificationRelationshipVO.getRelationshipType() );
        featureSpecificationRelationship.setParentSpecificationHref( featureSpecificationRelationshipVO.getParentSpecificationHref() );
        featureSpecificationRelationship.setParentSpecificationId( featureSpecificationRelationshipVO.getParentSpecificationId() );
        featureSpecificationRelationship.setValidFor( timePeriodVOToTimePeriod( featureSpecificationRelationshipVO.getValidFor() ) );

        return featureSpecificationRelationship;
    }

    protected List<FeatureSpecificationRelationship> featureSpecificationRelationshipVOListToFeatureSpecificationRelationshipList(List<FeatureSpecificationRelationshipVO> list) {
        if ( list == null ) {
            return null;
        }

        List<FeatureSpecificationRelationship> list1 = new ArrayList<FeatureSpecificationRelationship>( list.size() );
        for ( FeatureSpecificationRelationshipVO featureSpecificationRelationshipVO : list ) {
            list1.add( featureSpecificationRelationshipVOToFeatureSpecificationRelationship( featureSpecificationRelationshipVO ) );
        }

        return list1;
    }

    protected FeatureSpecification featureSpecificationVOToFeatureSpecification(FeatureSpecificationVO featureSpecificationVO) {
        if ( featureSpecificationVO == null ) {
            return null;
        }

        FeatureSpecification featureSpecification = new FeatureSpecification();

        featureSpecification.setId( featureSpecificationVO.getId() );
        featureSpecification.setIsBundle( featureSpecificationVO.getIsBundle() );
        featureSpecification.setIsEnabled( featureSpecificationVO.getIsEnabled() );
        featureSpecification.setName( featureSpecificationVO.getName() );
        featureSpecification.setVersion( featureSpecificationVO.getVersion() );
        featureSpecification.setConstraint( constraintRefVOListToConstraintRefList( featureSpecificationVO.getConstraint() ) );
        featureSpecification.setFeatureSpecCharacteristic( featureSpecificationCharacteristicVOListToFeatureSpecificationCharacteristicList( featureSpecificationVO.getFeatureSpecCharacteristic() ) );
        featureSpecification.setFeatureSpecRelationship( featureSpecificationRelationshipVOListToFeatureSpecificationRelationshipList( featureSpecificationVO.getFeatureSpecRelationship() ) );
        featureSpecification.setValidFor( timePeriodVOToTimePeriod( featureSpecificationVO.getValidFor() ) );

        return featureSpecification;
    }

    protected List<FeatureSpecification> featureSpecificationVOListToFeatureSpecificationList(List<FeatureSpecificationVO> list) {
        if ( list == null ) {
            return null;
        }

        List<FeatureSpecification> list1 = new ArrayList<FeatureSpecification>( list.size() );
        for ( FeatureSpecificationVO featureSpecificationVO : list ) {
            list1.add( featureSpecificationVOToFeatureSpecification( featureSpecificationVO ) );
        }

        return list1;
    }

    protected ResourceSpecificationRef resourceSpecificationRefVOToResourceSpecificationRef(ResourceSpecificationRefVO resourceSpecificationRefVO) {
        if ( resourceSpecificationRefVO == null ) {
            return null;
        }

        String id = null;

        id = resourceSpecificationRefVO.getId();

        ResourceSpecificationRef resourceSpecificationRef = new ResourceSpecificationRef( id );

        resourceSpecificationRef.setAtBaseType( resourceSpecificationRefVO.getAtBaseType() );
        resourceSpecificationRef.setAtSchemaLocation( resourceSpecificationRefVO.getAtSchemaLocation() );
        resourceSpecificationRef.setAtType( resourceSpecificationRefVO.getAtType() );
        resourceSpecificationRef.setHref( resourceSpecificationRefVO.getHref() );
        resourceSpecificationRef.setName( resourceSpecificationRefVO.getName() );
        resourceSpecificationRef.setAtReferredType( resourceSpecificationRefVO.getAtReferredType() );

        return resourceSpecificationRef;
    }

    protected List<ResourceSpecificationRef> resourceSpecificationRefVOListToResourceSpecificationRefList(List<ResourceSpecificationRefVO> list) {
        if ( list == null ) {
            return null;
        }

        List<ResourceSpecificationRef> list1 = new ArrayList<ResourceSpecificationRef>( list.size() );
        for ( ResourceSpecificationRefVO resourceSpecificationRefVO : list ) {
            list1.add( resourceSpecificationRefVOToResourceSpecificationRef( resourceSpecificationRefVO ) );
        }

        return list1;
    }

    protected ServiceLevelSpecificationRef serviceLevelSpecificationRefVOToServiceLevelSpecificationRef(ServiceLevelSpecificationRefVO serviceLevelSpecificationRefVO) {
        if ( serviceLevelSpecificationRefVO == null ) {
            return null;
        }

        URI id = null;

        id = mapToURI( serviceLevelSpecificationRefVO.getId() );

        ServiceLevelSpecificationRef serviceLevelSpecificationRef = new ServiceLevelSpecificationRef( id );

        serviceLevelSpecificationRef.setAtBaseType( serviceLevelSpecificationRefVO.getAtBaseType() );
        serviceLevelSpecificationRef.setAtSchemaLocation( serviceLevelSpecificationRefVO.getAtSchemaLocation() );
        serviceLevelSpecificationRef.setAtType( serviceLevelSpecificationRefVO.getAtType() );
        serviceLevelSpecificationRef.setHref( serviceLevelSpecificationRefVO.getHref() );
        serviceLevelSpecificationRef.setName( serviceLevelSpecificationRefVO.getName() );
        serviceLevelSpecificationRef.setAtReferredType( serviceLevelSpecificationRefVO.getAtReferredType() );

        return serviceLevelSpecificationRef;
    }

    protected List<ServiceLevelSpecificationRef> serviceLevelSpecificationRefVOListToServiceLevelSpecificationRefList(List<ServiceLevelSpecificationRefVO> list) {
        if ( list == null ) {
            return null;
        }

        List<ServiceLevelSpecificationRef> list1 = new ArrayList<ServiceLevelSpecificationRef>( list.size() );
        for ( ServiceLevelSpecificationRefVO serviceLevelSpecificationRefVO : list ) {
            list1.add( serviceLevelSpecificationRefVOToServiceLevelSpecificationRef( serviceLevelSpecificationRefVO ) );
        }

        return list1;
    }

    protected ServiceSpecificationRelationship serviceSpecRelationshipVOToServiceSpecificationRelationship(ServiceSpecRelationshipVO serviceSpecRelationshipVO) {
        if ( serviceSpecRelationshipVO == null ) {
            return null;
        }

        URI id = null;

        id = mapToURI( serviceSpecRelationshipVO.getId() );

        ServiceSpecificationRelationship serviceSpecificationRelationship = new ServiceSpecificationRelationship( id );

        serviceSpecificationRelationship.setAtBaseType( serviceSpecRelationshipVO.getAtBaseType() );
        serviceSpecificationRelationship.setAtSchemaLocation( serviceSpecRelationshipVO.getAtSchemaLocation() );
        serviceSpecificationRelationship.setAtType( serviceSpecRelationshipVO.getAtType() );
        serviceSpecificationRelationship.setHref( serviceSpecRelationshipVO.getHref() );
        serviceSpecificationRelationship.setName( serviceSpecRelationshipVO.getName() );
        serviceSpecificationRelationship.setAtReferredType( serviceSpecRelationshipVO.getAtReferredType() );
        serviceSpecificationRelationship.setRelationshipType( serviceSpecRelationshipVO.getRelationshipType() );
        serviceSpecificationRelationship.setRole( serviceSpecRelationshipVO.getRole() );
        serviceSpecificationRelationship.setValidFor( timePeriodVOToTimePeriod( serviceSpecRelationshipVO.getValidFor() ) );

        return serviceSpecificationRelationship;
    }

    protected List<ServiceSpecificationRelationship> serviceSpecRelationshipVOListToServiceSpecificationRelationshipList(List<ServiceSpecRelationshipVO> list) {
        if ( list == null ) {
            return null;
        }

        List<ServiceSpecificationRelationship> list1 = new ArrayList<ServiceSpecificationRelationship>( list.size() );
        for ( ServiceSpecRelationshipVO serviceSpecRelationshipVO : list ) {
            list1.add( serviceSpecRelationshipVOToServiceSpecificationRelationship( serviceSpecRelationshipVO ) );
        }

        return list1;
    }

    protected CharacteristicSpecificationRelationship characteristicSpecificationRelationshipVOToCharacteristicSpecificationRelationship(CharacteristicSpecificationRelationshipVO characteristicSpecificationRelationshipVO) {
        if ( characteristicSpecificationRelationshipVO == null ) {
            return null;
        }

        CharacteristicSpecificationRelationship characteristicSpecificationRelationship = new CharacteristicSpecificationRelationship();

        characteristicSpecificationRelationship.setCharacteristicSpecificationId( characteristicSpecificationRelationshipVO.getCharacteristicSpecificationId() );
        characteristicSpecificationRelationship.setName( characteristicSpecificationRelationshipVO.getName() );
        characteristicSpecificationRelationship.setParentSpecificationHref( characteristicSpecificationRelationshipVO.getParentSpecificationHref() );
        characteristicSpecificationRelationship.setParentSpecificationId( characteristicSpecificationRelationshipVO.getParentSpecificationId() );
        characteristicSpecificationRelationship.setRelationshipType( characteristicSpecificationRelationshipVO.getRelationshipType() );
        characteristicSpecificationRelationship.setValidFor( timePeriodVOToTimePeriod( characteristicSpecificationRelationshipVO.getValidFor() ) );

        return characteristicSpecificationRelationship;
    }

    protected List<CharacteristicSpecificationRelationship> characteristicSpecificationRelationshipVOListToCharacteristicSpecificationRelationshipList(List<CharacteristicSpecificationRelationshipVO> list) {
        if ( list == null ) {
            return null;
        }

        List<CharacteristicSpecificationRelationship> list1 = new ArrayList<CharacteristicSpecificationRelationship>( list.size() );
        for ( CharacteristicSpecificationRelationshipVO characteristicSpecificationRelationshipVO : list ) {
            list1.add( characteristicSpecificationRelationshipVOToCharacteristicSpecificationRelationship( characteristicSpecificationRelationshipVO ) );
        }

        return list1;
    }

    protected CharacteristicValueSpecification characteristicValueSpecificationVOToCharacteristicValueSpecification(CharacteristicValueSpecificationVO characteristicValueSpecificationVO) {
        if ( characteristicValueSpecificationVO == null ) {
            return null;
        }

        CharacteristicValueSpecification characteristicValueSpecification = new CharacteristicValueSpecification();

        characteristicValueSpecification.setIsDefault( characteristicValueSpecificationVO.getIsDefault() );
        characteristicValueSpecification.setRangeInterval( characteristicValueSpecificationVO.getRangeInterval() );
        characteristicValueSpecification.setRegex( characteristicValueSpecificationVO.getRegex() );
        characteristicValueSpecification.setUnitOfMeasure( characteristicValueSpecificationVO.getUnitOfMeasure() );
        characteristicValueSpecification.setValueFrom( characteristicValueSpecificationVO.getValueFrom() );
        characteristicValueSpecification.setValueTo( characteristicValueSpecificationVO.getValueTo() );
        characteristicValueSpecification.setValueType( characteristicValueSpecificationVO.getValueType() );
        characteristicValueSpecification.setValidFor( timePeriodVOToTimePeriod( characteristicValueSpecificationVO.getValidFor() ) );
        characteristicValueSpecification.setValue( characteristicValueSpecificationVO.getValue() );
        characteristicValueSpecification.setAtBaseType( characteristicValueSpecificationVO.getAtBaseType() );
        characteristicValueSpecification.setAtSchemaLocation( characteristicValueSpecificationVO.getAtSchemaLocation() );
        characteristicValueSpecification.setAtType( characteristicValueSpecificationVO.getAtType() );

        return characteristicValueSpecification;
    }

    protected List<CharacteristicValueSpecification> characteristicValueSpecificationVOListToCharacteristicValueSpecificationList(List<CharacteristicValueSpecificationVO> list) {
        if ( list == null ) {
            return null;
        }

        List<CharacteristicValueSpecification> list1 = new ArrayList<CharacteristicValueSpecification>( list.size() );
        for ( CharacteristicValueSpecificationVO characteristicValueSpecificationVO : list ) {
            list1.add( characteristicValueSpecificationVOToCharacteristicValueSpecification( characteristicValueSpecificationVO ) );
        }

        return list1;
    }

    protected CharacteristicSpecification characteristicSpecificationVOToCharacteristicSpecification(CharacteristicSpecificationVO characteristicSpecificationVO) {
        if ( characteristicSpecificationVO == null ) {
            return null;
        }

        CharacteristicSpecification characteristicSpecification = new CharacteristicSpecification();

        characteristicSpecification.setId( characteristicSpecificationVO.getId() );
        characteristicSpecification.setConfigurable( characteristicSpecificationVO.getConfigurable() );
        characteristicSpecification.setDescription( characteristicSpecificationVO.getDescription() );
        characteristicSpecification.setExtensible( characteristicSpecificationVO.getExtensible() );
        characteristicSpecification.setIsUnique( characteristicSpecificationVO.getIsUnique() );
        characteristicSpecification.setMaxCardinality( characteristicSpecificationVO.getMaxCardinality() );
        characteristicSpecification.setMinCardinality( characteristicSpecificationVO.getMinCardinality() );
        characteristicSpecification.setName( characteristicSpecificationVO.getName() );
        characteristicSpecification.setRegex( characteristicSpecificationVO.getRegex() );
        characteristicSpecification.setValueType( characteristicSpecificationVO.getValueType() );
        characteristicSpecification.setCharSpecRelationship( characteristicSpecificationRelationshipVOListToCharacteristicSpecificationRelationshipList( characteristicSpecificationVO.getCharSpecRelationship() ) );
        characteristicSpecification.setCharacteristicValueSpecification( characteristicValueSpecificationVOListToCharacteristicValueSpecificationList( characteristicSpecificationVO.getCharacteristicValueSpecification() ) );
        characteristicSpecification.setValidFor( timePeriodVOToTimePeriod( characteristicSpecificationVO.getValidFor() ) );
        characteristicSpecification.setAtBaseType( characteristicSpecificationVO.getAtBaseType() );
        characteristicSpecification.setAtSchemaLocation( characteristicSpecificationVO.getAtSchemaLocation() );
        characteristicSpecification.setAtType( characteristicSpecificationVO.getAtType() );
        characteristicSpecification.setAtValueSchemaLocation( characteristicSpecificationVO.getAtValueSchemaLocation() );

        return characteristicSpecification;
    }

    protected List<CharacteristicSpecification> characteristicSpecificationVOListToCharacteristicSpecificationList(List<CharacteristicSpecificationVO> list) {
        if ( list == null ) {
            return null;
        }

        List<CharacteristicSpecification> list1 = new ArrayList<CharacteristicSpecification>( list.size() );
        for ( CharacteristicSpecificationVO characteristicSpecificationVO : list ) {
            list1.add( characteristicSpecificationVOToCharacteristicSpecification( characteristicSpecificationVO ) );
        }

        return list1;
    }

    protected TargetEntitySchema targetEntitySchemaVOToTargetEntitySchema(TargetEntitySchemaVO targetEntitySchemaVO) {
        if ( targetEntitySchemaVO == null ) {
            return null;
        }

        TargetEntitySchema targetEntitySchema = new TargetEntitySchema();

        targetEntitySchema.setAtSchemaLocation( targetEntitySchemaVO.getAtSchemaLocation() );
        targetEntitySchema.setAtType( targetEntitySchemaVO.getAtType() );

        return targetEntitySchema;
    }
}

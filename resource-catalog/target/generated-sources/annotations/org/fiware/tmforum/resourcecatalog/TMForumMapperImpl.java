package org.fiware.tmforum.resourcecatalog;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import javax.inject.Named;
import javax.inject.Singleton;
import org.fiware.resourcecatalog.model.AttachmentRefOrValueVO;
import org.fiware.resourcecatalog.model.CharacteristicValueSpecificationVO;
import org.fiware.resourcecatalog.model.ConstraintRefVO;
import org.fiware.resourcecatalog.model.EventSubscriptionVO;
import org.fiware.resourcecatalog.model.FeatureSpecificationCharacteristicRelationshipVO;
import org.fiware.resourcecatalog.model.FeatureSpecificationCharacteristicVO;
import org.fiware.resourcecatalog.model.FeatureSpecificationRelationshipVO;
import org.fiware.resourcecatalog.model.FeatureSpecificationVO;
import org.fiware.resourcecatalog.model.QuantityVO;
import org.fiware.resourcecatalog.model.RelatedPartyVO;
import org.fiware.resourcecatalog.model.ResourceCandidateCreateVO;
import org.fiware.resourcecatalog.model.ResourceCandidateRefVO;
import org.fiware.resourcecatalog.model.ResourceCandidateUpdateVO;
import org.fiware.resourcecatalog.model.ResourceCandidateVO;
import org.fiware.resourcecatalog.model.ResourceCatalogCreateVO;
import org.fiware.resourcecatalog.model.ResourceCatalogUpdateVO;
import org.fiware.resourcecatalog.model.ResourceCatalogVO;
import org.fiware.resourcecatalog.model.ResourceCategoryCreateVO;
import org.fiware.resourcecatalog.model.ResourceCategoryRefVO;
import org.fiware.resourcecatalog.model.ResourceCategoryUpdateVO;
import org.fiware.resourcecatalog.model.ResourceCategoryVO;
import org.fiware.resourcecatalog.model.ResourceSpecificationCharacteristicRelationshipVO;
import org.fiware.resourcecatalog.model.ResourceSpecificationCharacteristicVO;
import org.fiware.resourcecatalog.model.ResourceSpecificationCreateVO;
import org.fiware.resourcecatalog.model.ResourceSpecificationRefVO;
import org.fiware.resourcecatalog.model.ResourceSpecificationRelationshipVO;
import org.fiware.resourcecatalog.model.ResourceSpecificationUpdateVO;
import org.fiware.resourcecatalog.model.ResourceSpecificationVO;
import org.fiware.resourcecatalog.model.TargetResourceSchemaVO;
import org.fiware.resourcecatalog.model.TimePeriodVO;
import org.fiware.tmforum.common.domain.AttachmentRefOrValue;
import org.fiware.tmforum.common.domain.ConstraintRef;
import org.fiware.tmforum.common.domain.Quantity;
import org.fiware.tmforum.common.domain.RelatedParty;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.resource.CharacteristicValue;
import org.fiware.tmforum.resource.FeatureSpecification;
import org.fiware.tmforum.resource.FeatureSpecificationCharacteristic;
import org.fiware.tmforum.resource.FeatureSpecificationCharacteristicRelationship;
import org.fiware.tmforum.resource.FeatureSpecificationRelationship;
import org.fiware.tmforum.resource.ResourceCandidate;
import org.fiware.tmforum.resource.ResourceCandidateRef;
import org.fiware.tmforum.resource.ResourceCategory;
import org.fiware.tmforum.resource.ResourceCategoryRef;
import org.fiware.tmforum.resource.ResourceSpecification;
import org.fiware.tmforum.resource.ResourceSpecificationCharacteristic;
import org.fiware.tmforum.resource.ResourceSpecificationCharacteristicRelationship;
import org.fiware.tmforum.resource.ResourceSpecificationRef;
import org.fiware.tmforum.resource.TargetResourceSchema;
import org.fiware.tmforum.resourcecatalog.domain.ResourceCatalog;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2023-11-22T12:13:41+0100",
    comments = "version: 1.4.2.Final, compiler: javac, environment: Java 17.0.9 (Amazon.com Inc.)"
)
@Singleton
@Named
public class TMForumMapperImpl implements TMForumMapper {

    @Override
    public ResourceCatalogVO map(ResourceCatalogCreateVO resourceCatalogCreateVO, URI id) {
        if ( resourceCatalogCreateVO == null && id == null ) {
            return null;
        }

        ResourceCatalogVO resourceCatalogVO = new ResourceCatalogVO();

        if ( resourceCatalogCreateVO != null ) {
            resourceCatalogVO.setDescription( resourceCatalogCreateVO.getDescription() );
            resourceCatalogVO.setLastUpdate( resourceCatalogCreateVO.getLastUpdate() );
            resourceCatalogVO.setLifecycleStatus( resourceCatalogCreateVO.getLifecycleStatus() );
            resourceCatalogVO.setName( resourceCatalogCreateVO.getName() );
            resourceCatalogVO.setVersion( resourceCatalogCreateVO.getVersion() );
            List<ResourceCategoryRefVO> list = resourceCatalogCreateVO.getCategory();
            if ( list != null ) {
                resourceCatalogVO.setCategory( new ArrayList<ResourceCategoryRefVO>( list ) );
            }
            List<RelatedPartyVO> list1 = resourceCatalogCreateVO.getRelatedParty();
            if ( list1 != null ) {
                resourceCatalogVO.setRelatedParty( new ArrayList<RelatedPartyVO>( list1 ) );
            }
            resourceCatalogVO.setValidFor( resourceCatalogCreateVO.getValidFor() );
            resourceCatalogVO.setAtBaseType( resourceCatalogCreateVO.getAtBaseType() );
            resourceCatalogVO.setAtSchemaLocation( resourceCatalogCreateVO.getAtSchemaLocation() );
            resourceCatalogVO.setAtType( resourceCatalogCreateVO.getAtType() );
        }
        if ( id != null ) {
            resourceCatalogVO.setId( mapFromURI( id ) );
            resourceCatalogVO.setHref( id );
        }

        return resourceCatalogVO;
    }

    @Override
    public ResourceCatalogVO map(ResourceCatalog resourceCatalog) {
        if ( resourceCatalog == null ) {
            return null;
        }

        ResourceCatalogVO resourceCatalogVO = new ResourceCatalogVO();

        resourceCatalogVO.setId( mapFromURI( resourceCatalog.getId() ) );
        resourceCatalogVO.setHref( resourceCatalog.getHref() );
        resourceCatalogVO.setDescription( resourceCatalog.getDescription() );
        resourceCatalogVO.setLastUpdate( resourceCatalog.getLastUpdate() );
        resourceCatalogVO.setLifecycleStatus( resourceCatalog.getLifecycleStatus() );
        resourceCatalogVO.setName( resourceCatalog.getName() );
        resourceCatalogVO.setVersion( resourceCatalog.getVersion() );
        resourceCatalogVO.setCategory( resourceCategoryRefListToResourceCategoryRefVOList( resourceCatalog.getCategory() ) );
        resourceCatalogVO.setRelatedParty( relatedPartyListToRelatedPartyVOList( resourceCatalog.getRelatedParty() ) );
        resourceCatalogVO.setValidFor( timePeriodToTimePeriodVO( resourceCatalog.getValidFor() ) );
        resourceCatalogVO.setAtBaseType( resourceCatalog.getAtBaseType() );
        resourceCatalogVO.setAtSchemaLocation( resourceCatalog.getAtSchemaLocation() );
        resourceCatalogVO.setAtType( resourceCatalog.getAtType() );

        return resourceCatalogVO;
    }

    @Override
    public ResourceCatalog map(ResourceCatalogVO resourceCatalogVO) {
        if ( resourceCatalogVO == null ) {
            return null;
        }

        String id = null;

        id = resourceCatalogVO.getId();

        ResourceCatalog resourceCatalog = new ResourceCatalog( id );

        resourceCatalog.setAtBaseType( resourceCatalogVO.getAtBaseType() );
        resourceCatalog.setAtSchemaLocation( resourceCatalogVO.getAtSchemaLocation() );
        resourceCatalog.setAtType( resourceCatalogVO.getAtType() );
        resourceCatalog.setHref( resourceCatalogVO.getHref() );
        resourceCatalog.setDescription( resourceCatalogVO.getDescription() );
        resourceCatalog.setLastUpdate( resourceCatalogVO.getLastUpdate() );
        resourceCatalog.setLifecycleStatus( resourceCatalogVO.getLifecycleStatus() );
        resourceCatalog.setName( resourceCatalogVO.getName() );
        resourceCatalog.setVersion( resourceCatalogVO.getVersion() );
        resourceCatalog.setCategory( resourceCategoryRefVOListToResourceCategoryRefList( resourceCatalogVO.getCategory() ) );
        resourceCatalog.setRelatedParty( relatedPartyVOListToRelatedPartyList( resourceCatalogVO.getRelatedParty() ) );
        resourceCatalog.setValidFor( timePeriodVOToTimePeriod( resourceCatalogVO.getValidFor() ) );

        return resourceCatalog;
    }

    @Override
    public ResourceCatalog map(ResourceCatalogUpdateVO resourceCatalogUpdateVO, String id) {
        if ( resourceCatalogUpdateVO == null && id == null ) {
            return null;
        }

        String id1 = null;
        if ( id != null ) {
            id1 = id;
        }

        ResourceCatalog resourceCatalog = new ResourceCatalog( id1 );

        if ( resourceCatalogUpdateVO != null ) {
            resourceCatalog.setAtBaseType( resourceCatalogUpdateVO.getAtBaseType() );
            resourceCatalog.setAtSchemaLocation( resourceCatalogUpdateVO.getAtSchemaLocation() );
            resourceCatalog.setAtType( resourceCatalogUpdateVO.getAtType() );
            resourceCatalog.setDescription( resourceCatalogUpdateVO.getDescription() );
            resourceCatalog.setLastUpdate( resourceCatalogUpdateVO.getLastUpdate() );
            resourceCatalog.setLifecycleStatus( resourceCatalogUpdateVO.getLifecycleStatus() );
            resourceCatalog.setName( resourceCatalogUpdateVO.getName() );
            resourceCatalog.setVersion( resourceCatalogUpdateVO.getVersion() );
            resourceCatalog.setCategory( resourceCategoryRefVOListToResourceCategoryRefList( resourceCatalogUpdateVO.getCategory() ) );
            resourceCatalog.setRelatedParty( relatedPartyVOListToRelatedPartyList( resourceCatalogUpdateVO.getRelatedParty() ) );
            resourceCatalog.setValidFor( timePeriodVOToTimePeriod( resourceCatalogUpdateVO.getValidFor() ) );
        }

        return resourceCatalog;
    }

    @Override
    public ResourceSpecificationVO map(ResourceSpecificationCreateVO resourceSpecificationCreateVO, URI id) {
        if ( resourceSpecificationCreateVO == null && id == null ) {
            return null;
        }

        ResourceSpecificationVO resourceSpecificationVO = new ResourceSpecificationVO();

        if ( resourceSpecificationCreateVO != null ) {
            resourceSpecificationVO.setCategory( resourceSpecificationCreateVO.getCategory() );
            resourceSpecificationVO.setDescription( resourceSpecificationCreateVO.getDescription() );
            resourceSpecificationVO.setIsBundle( resourceSpecificationCreateVO.getIsBundle() );
            resourceSpecificationVO.setLastUpdate( resourceSpecificationCreateVO.getLastUpdate() );
            resourceSpecificationVO.setLifecycleStatus( resourceSpecificationCreateVO.getLifecycleStatus() );
            resourceSpecificationVO.setName( resourceSpecificationCreateVO.getName() );
            resourceSpecificationVO.setVersion( resourceSpecificationCreateVO.getVersion() );
            List<AttachmentRefOrValueVO> list = resourceSpecificationCreateVO.getAttachment();
            if ( list != null ) {
                resourceSpecificationVO.setAttachment( new ArrayList<AttachmentRefOrValueVO>( list ) );
            }
            List<FeatureSpecificationVO> list1 = resourceSpecificationCreateVO.getFeatureSpecification();
            if ( list1 != null ) {
                resourceSpecificationVO.setFeatureSpecification( new ArrayList<FeatureSpecificationVO>( list1 ) );
            }
            List<RelatedPartyVO> list2 = resourceSpecificationCreateVO.getRelatedParty();
            if ( list2 != null ) {
                resourceSpecificationVO.setRelatedParty( new ArrayList<RelatedPartyVO>( list2 ) );
            }
            List<ResourceSpecificationCharacteristicVO> list3 = resourceSpecificationCreateVO.getResourceSpecCharacteristic();
            if ( list3 != null ) {
                resourceSpecificationVO.setResourceSpecCharacteristic( new ArrayList<ResourceSpecificationCharacteristicVO>( list3 ) );
            }
            List<ResourceSpecificationRelationshipVO> list4 = resourceSpecificationCreateVO.getResourceSpecRelationship();
            if ( list4 != null ) {
                resourceSpecificationVO.setResourceSpecRelationship( new ArrayList<ResourceSpecificationRelationshipVO>( list4 ) );
            }
            resourceSpecificationVO.setTargetResourceSchema( resourceSpecificationCreateVO.getTargetResourceSchema() );
            resourceSpecificationVO.setValidFor( resourceSpecificationCreateVO.getValidFor() );
            resourceSpecificationVO.setAtBaseType( resourceSpecificationCreateVO.getAtBaseType() );
            resourceSpecificationVO.setAtSchemaLocation( resourceSpecificationCreateVO.getAtSchemaLocation() );
            resourceSpecificationVO.setAtType( resourceSpecificationCreateVO.getAtType() );
        }
        if ( id != null ) {
            resourceSpecificationVO.setId( mapFromURI( id ) );
            resourceSpecificationVO.setHref( id );
        }

        return resourceSpecificationVO;
    }

    @Override
    public ResourceSpecificationVO map(ResourceSpecification resourceSpecification) {
        if ( resourceSpecification == null ) {
            return null;
        }

        ResourceSpecificationVO resourceSpecificationVO = new ResourceSpecificationVO();

        resourceSpecificationVO.setId( mapFromURI( resourceSpecification.getId() ) );
        resourceSpecificationVO.setHref( resourceSpecification.getHref() );
        resourceSpecificationVO.setCategory( resourceSpecification.getCategory() );
        resourceSpecificationVO.setDescription( resourceSpecification.getDescription() );
        resourceSpecificationVO.setIsBundle( resourceSpecification.getIsBundle() );
        resourceSpecificationVO.setLastUpdate( resourceSpecification.getLastUpdate() );
        resourceSpecificationVO.setLifecycleStatus( resourceSpecification.getLifecycleStatus() );
        resourceSpecificationVO.setName( resourceSpecification.getName() );
        resourceSpecificationVO.setVersion( resourceSpecification.getVersion() );
        resourceSpecificationVO.setAttachment( attachmentRefOrValueListToAttachmentRefOrValueVOList( resourceSpecification.getAttachment() ) );
        resourceSpecificationVO.setFeatureSpecification( featureSpecificationListToFeatureSpecificationVOList( resourceSpecification.getFeatureSpecification() ) );
        resourceSpecificationVO.setRelatedParty( relatedPartyListToRelatedPartyVOList( resourceSpecification.getRelatedParty() ) );
        resourceSpecificationVO.setResourceSpecCharacteristic( resourceSpecificationCharacteristicListToResourceSpecificationCharacteristicVOList( resourceSpecification.getResourceSpecCharacteristic() ) );
        resourceSpecificationVO.setTargetResourceSchema( targetResourceSchemaToTargetResourceSchemaVO( resourceSpecification.getTargetResourceSchema() ) );
        resourceSpecificationVO.setValidFor( timePeriodToTimePeriodVO( resourceSpecification.getValidFor() ) );
        resourceSpecificationVO.setAtBaseType( resourceSpecification.getAtBaseType() );
        resourceSpecificationVO.setAtSchemaLocation( resourceSpecification.getAtSchemaLocation() );
        resourceSpecificationVO.setAtType( resourceSpecification.getAtType() );

        return resourceSpecificationVO;
    }

    @Override
    public ResourceSpecification map(ResourceSpecificationVO resourceCandidateVO) {
        if ( resourceCandidateVO == null ) {
            return null;
        }

        String id = null;

        id = resourceCandidateVO.getId();

        ResourceSpecification resourceSpecification = new ResourceSpecification( id );

        resourceSpecification.setAtBaseType( resourceCandidateVO.getAtBaseType() );
        resourceSpecification.setAtSchemaLocation( resourceCandidateVO.getAtSchemaLocation() );
        resourceSpecification.setAtType( resourceCandidateVO.getAtType() );
        resourceSpecification.setHref( resourceCandidateVO.getHref() );
        resourceSpecification.setCategory( resourceCandidateVO.getCategory() );
        resourceSpecification.setDescription( resourceCandidateVO.getDescription() );
        resourceSpecification.setIsBundle( resourceCandidateVO.getIsBundle() );
        resourceSpecification.setLastUpdate( resourceCandidateVO.getLastUpdate() );
        resourceSpecification.setLifecycleStatus( resourceCandidateVO.getLifecycleStatus() );
        resourceSpecification.setName( resourceCandidateVO.getName() );
        resourceSpecification.setVersion( resourceCandidateVO.getVersion() );
        resourceSpecification.setAttachment( attachmentRefOrValueVOListToAttachmentRefOrValueList( resourceCandidateVO.getAttachment() ) );
        resourceSpecification.setFeatureSpecification( featureSpecificationVOListToFeatureSpecificationList( resourceCandidateVO.getFeatureSpecification() ) );
        resourceSpecification.setRelatedParty( relatedPartyVOListToRelatedPartyList( resourceCandidateVO.getRelatedParty() ) );
        resourceSpecification.setResourceSpecCharacteristic( resourceSpecificationCharacteristicVOListToResourceSpecificationCharacteristicList( resourceCandidateVO.getResourceSpecCharacteristic() ) );
        resourceSpecification.setTargetResourceSchema( targetResourceSchemaVOToTargetResourceSchema( resourceCandidateVO.getTargetResourceSchema() ) );
        resourceSpecification.setValidFor( timePeriodVOToTimePeriod( resourceCandidateVO.getValidFor() ) );

        return resourceSpecification;
    }

    @Override
    public ResourceSpecification map(ResourceSpecificationUpdateVO resourceSpecificationUpdateVO, String id) {
        if ( resourceSpecificationUpdateVO == null && id == null ) {
            return null;
        }

        String id1 = null;
        if ( id != null ) {
            id1 = id;
        }

        ResourceSpecification resourceSpecification = new ResourceSpecification( id1 );

        if ( resourceSpecificationUpdateVO != null ) {
            resourceSpecification.setAtBaseType( resourceSpecificationUpdateVO.getAtBaseType() );
            resourceSpecification.setAtSchemaLocation( resourceSpecificationUpdateVO.getAtSchemaLocation() );
            resourceSpecification.setAtType( resourceSpecificationUpdateVO.getAtType() );
            resourceSpecification.setCategory( resourceSpecificationUpdateVO.getCategory() );
            resourceSpecification.setDescription( resourceSpecificationUpdateVO.getDescription() );
            resourceSpecification.setIsBundle( resourceSpecificationUpdateVO.getIsBundle() );
            resourceSpecification.setLastUpdate( resourceSpecificationUpdateVO.getLastUpdate() );
            resourceSpecification.setLifecycleStatus( resourceSpecificationUpdateVO.getLifecycleStatus() );
            resourceSpecification.setName( resourceSpecificationUpdateVO.getName() );
            resourceSpecification.setVersion( resourceSpecificationUpdateVO.getVersion() );
            resourceSpecification.setAttachment( attachmentRefOrValueVOListToAttachmentRefOrValueList( resourceSpecificationUpdateVO.getAttachment() ) );
            resourceSpecification.setFeatureSpecification( featureSpecificationVOListToFeatureSpecificationList( resourceSpecificationUpdateVO.getFeatureSpecification() ) );
            resourceSpecification.setRelatedParty( relatedPartyVOListToRelatedPartyList( resourceSpecificationUpdateVO.getRelatedParty() ) );
            resourceSpecification.setResourceSpecCharacteristic( resourceSpecificationCharacteristicVOListToResourceSpecificationCharacteristicList( resourceSpecificationUpdateVO.getResourceSpecCharacteristic() ) );
            resourceSpecification.setTargetResourceSchema( targetResourceSchemaVOToTargetResourceSchema( resourceSpecificationUpdateVO.getTargetResourceSchema() ) );
            resourceSpecification.setValidFor( timePeriodVOToTimePeriod( resourceSpecificationUpdateVO.getValidFor() ) );
        }

        return resourceSpecification;
    }

    @Override
    public ResourceCandidateVO map(ResourceCandidateCreateVO resourceCandidateCreateVO, URI id) {
        if ( resourceCandidateCreateVO == null && id == null ) {
            return null;
        }

        ResourceCandidateVO resourceCandidateVO = new ResourceCandidateVO();

        if ( resourceCandidateCreateVO != null ) {
            resourceCandidateVO.setDescription( resourceCandidateCreateVO.getDescription() );
            resourceCandidateVO.setLastUpdate( resourceCandidateCreateVO.getLastUpdate() );
            resourceCandidateVO.setLifecycleStatus( resourceCandidateCreateVO.getLifecycleStatus() );
            resourceCandidateVO.setName( resourceCandidateCreateVO.getName() );
            resourceCandidateVO.setVersion( resourceCandidateCreateVO.getVersion() );
            List<ResourceCategoryRefVO> list = resourceCandidateCreateVO.getCategory();
            if ( list != null ) {
                resourceCandidateVO.setCategory( new ArrayList<ResourceCategoryRefVO>( list ) );
            }
            resourceCandidateVO.setResourceSpecification( resourceCandidateCreateVO.getResourceSpecification() );
            resourceCandidateVO.setValidFor( resourceCandidateCreateVO.getValidFor() );
            resourceCandidateVO.setAtBaseType( resourceCandidateCreateVO.getAtBaseType() );
            resourceCandidateVO.setAtSchemaLocation( resourceCandidateCreateVO.getAtSchemaLocation() );
            resourceCandidateVO.setAtType( resourceCandidateCreateVO.getAtType() );
        }
        if ( id != null ) {
            resourceCandidateVO.setId( mapFromURI( id ) );
            resourceCandidateVO.setHref( id );
        }

        return resourceCandidateVO;
    }

    @Override
    public ResourceCandidateVO map(ResourceCandidate resourceCandidate) {
        if ( resourceCandidate == null ) {
            return null;
        }

        ResourceCandidateVO resourceCandidateVO = new ResourceCandidateVO();

        resourceCandidateVO.setId( mapFromURI( resourceCandidate.getId() ) );
        resourceCandidateVO.setHref( resourceCandidate.getHref() );
        resourceCandidateVO.setDescription( resourceCandidate.getDescription() );
        resourceCandidateVO.setLastUpdate( resourceCandidate.getLastUpdate() );
        resourceCandidateVO.setLifecycleStatus( resourceCandidate.getLifecycleStatus() );
        resourceCandidateVO.setName( resourceCandidate.getName() );
        resourceCandidateVO.setVersion( resourceCandidate.getVersion() );
        resourceCandidateVO.setCategory( resourceCategoryRefListToResourceCategoryRefVOList( resourceCandidate.getCategory() ) );
        resourceCandidateVO.setResourceSpecification( resourceSpecificationRefToResourceSpecificationRefVO( resourceCandidate.getResourceSpecification() ) );
        resourceCandidateVO.setValidFor( timePeriodToTimePeriodVO( resourceCandidate.getValidFor() ) );
        resourceCandidateVO.setAtBaseType( resourceCandidate.getAtBaseType() );
        resourceCandidateVO.setAtSchemaLocation( resourceCandidate.getAtSchemaLocation() );
        resourceCandidateVO.setAtType( resourceCandidate.getAtType() );

        return resourceCandidateVO;
    }

    @Override
    public ResourceCandidate map(ResourceCandidateVO resourceCandidateVO) {
        if ( resourceCandidateVO == null ) {
            return null;
        }

        String id = null;

        id = resourceCandidateVO.getId();

        ResourceCandidate resourceCandidate = new ResourceCandidate( id );

        resourceCandidate.setAtBaseType( resourceCandidateVO.getAtBaseType() );
        resourceCandidate.setAtSchemaLocation( resourceCandidateVO.getAtSchemaLocation() );
        resourceCandidate.setAtType( resourceCandidateVO.getAtType() );
        resourceCandidate.setHref( resourceCandidateVO.getHref() );
        resourceCandidate.setDescription( resourceCandidateVO.getDescription() );
        resourceCandidate.setLastUpdate( resourceCandidateVO.getLastUpdate() );
        resourceCandidate.setLifecycleStatus( resourceCandidateVO.getLifecycleStatus() );
        resourceCandidate.setName( resourceCandidateVO.getName() );
        resourceCandidate.setVersion( resourceCandidateVO.getVersion() );
        resourceCandidate.setCategory( resourceCategoryRefVOListToResourceCategoryRefList( resourceCandidateVO.getCategory() ) );
        resourceCandidate.setResourceSpecification( resourceSpecificationRefVOToResourceSpecificationRef( resourceCandidateVO.getResourceSpecification() ) );
        resourceCandidate.setValidFor( timePeriodVOToTimePeriod( resourceCandidateVO.getValidFor() ) );

        return resourceCandidate;
    }

    @Override
    public ResourceCandidate map(ResourceCandidateUpdateVO resourceCandidateUpdateVO, String id) {
        if ( resourceCandidateUpdateVO == null && id == null ) {
            return null;
        }

        String id1 = null;
        if ( id != null ) {
            id1 = id;
        }

        ResourceCandidate resourceCandidate = new ResourceCandidate( id1 );

        if ( resourceCandidateUpdateVO != null ) {
            resourceCandidate.setAtBaseType( resourceCandidateUpdateVO.getAtBaseType() );
            resourceCandidate.setAtSchemaLocation( resourceCandidateUpdateVO.getAtSchemaLocation() );
            resourceCandidate.setAtType( resourceCandidateUpdateVO.getAtType() );
            resourceCandidate.setDescription( resourceCandidateUpdateVO.getDescription() );
            resourceCandidate.setLastUpdate( resourceCandidateUpdateVO.getLastUpdate() );
            resourceCandidate.setLifecycleStatus( resourceCandidateUpdateVO.getLifecycleStatus() );
            resourceCandidate.setName( resourceCandidateUpdateVO.getName() );
            resourceCandidate.setVersion( resourceCandidateUpdateVO.getVersion() );
            resourceCandidate.setCategory( resourceCategoryRefVOListToResourceCategoryRefList( resourceCandidateUpdateVO.getCategory() ) );
            resourceCandidate.setResourceSpecification( resourceSpecificationRefVOToResourceSpecificationRef( resourceCandidateUpdateVO.getResourceSpecification() ) );
            resourceCandidate.setValidFor( timePeriodVOToTimePeriod( resourceCandidateUpdateVO.getValidFor() ) );
        }

        return resourceCandidate;
    }

    @Override
    public ResourceCategoryVO map(ResourceCategoryCreateVO resourceCategoryCreateVO, URI id) {
        if ( resourceCategoryCreateVO == null && id == null ) {
            return null;
        }

        ResourceCategoryVO resourceCategoryVO = new ResourceCategoryVO();

        if ( resourceCategoryCreateVO != null ) {
            resourceCategoryVO.setDescription( resourceCategoryCreateVO.getDescription() );
            resourceCategoryVO.setIsRoot( resourceCategoryCreateVO.getIsRoot() );
            resourceCategoryVO.setLastUpdate( resourceCategoryCreateVO.getLastUpdate() );
            resourceCategoryVO.setLifecycleStatus( resourceCategoryCreateVO.getLifecycleStatus() );
            resourceCategoryVO.setName( resourceCategoryCreateVO.getName() );
            resourceCategoryVO.setParentId( resourceCategoryCreateVO.getParentId() );
            resourceCategoryVO.setVersion( resourceCategoryCreateVO.getVersion() );
            List<ResourceCategoryRefVO> list = resourceCategoryCreateVO.getCategory();
            if ( list != null ) {
                resourceCategoryVO.setCategory( new ArrayList<ResourceCategoryRefVO>( list ) );
            }
            List<RelatedPartyVO> list1 = resourceCategoryCreateVO.getRelatedParty();
            if ( list1 != null ) {
                resourceCategoryVO.setRelatedParty( new ArrayList<RelatedPartyVO>( list1 ) );
            }
            List<ResourceCandidateRefVO> list2 = resourceCategoryCreateVO.getResourceCandidate();
            if ( list2 != null ) {
                resourceCategoryVO.setResourceCandidate( new ArrayList<ResourceCandidateRefVO>( list2 ) );
            }
            resourceCategoryVO.setValidFor( resourceCategoryCreateVO.getValidFor() );
            resourceCategoryVO.setAtBaseType( resourceCategoryCreateVO.getAtBaseType() );
            resourceCategoryVO.setAtSchemaLocation( resourceCategoryCreateVO.getAtSchemaLocation() );
            resourceCategoryVO.setAtType( resourceCategoryCreateVO.getAtType() );
        }
        if ( id != null ) {
            resourceCategoryVO.setId( mapFromURI( id ) );
            resourceCategoryVO.setHref( id );
        }

        return resourceCategoryVO;
    }

    @Override
    public ResourceCategoryVO map(ResourceCategory resourceCategory) {
        if ( resourceCategory == null ) {
            return null;
        }

        ResourceCategoryVO resourceCategoryVO = new ResourceCategoryVO();

        resourceCategoryVO.setId( mapFromURI( resourceCategory.getId() ) );
        resourceCategoryVO.setHref( resourceCategory.getHref() );
        resourceCategoryVO.setDescription( resourceCategory.getDescription() );
        resourceCategoryVO.setIsRoot( resourceCategory.getIsRoot() );
        resourceCategoryVO.setLastUpdate( resourceCategory.getLastUpdate() );
        resourceCategoryVO.setLifecycleStatus( resourceCategory.getLifecycleStatus() );
        resourceCategoryVO.setName( resourceCategory.getName() );
        resourceCategoryVO.setParentId( mapFromCategoryRef( resourceCategory.getParentId() ) );
        resourceCategoryVO.setVersion( resourceCategory.getVersion() );
        resourceCategoryVO.setCategory( resourceCategoryRefListToResourceCategoryRefVOList( resourceCategory.getCategory() ) );
        resourceCategoryVO.setRelatedParty( relatedPartyListToRelatedPartyVOList( resourceCategory.getRelatedParty() ) );
        resourceCategoryVO.setResourceCandidate( resourceCandidateRefListToResourceCandidateRefVOList( resourceCategory.getResourceCandidate() ) );
        resourceCategoryVO.setValidFor( timePeriodToTimePeriodVO( resourceCategory.getValidFor() ) );
        resourceCategoryVO.setAtBaseType( resourceCategory.getAtBaseType() );
        resourceCategoryVO.setAtSchemaLocation( resourceCategory.getAtSchemaLocation() );
        resourceCategoryVO.setAtType( resourceCategory.getAtType() );

        return resourceCategoryVO;
    }

    @Override
    public ResourceCategory map(ResourceCategoryVO resourceCategoryVO) {
        if ( resourceCategoryVO == null ) {
            return null;
        }

        String id = null;

        id = resourceCategoryVO.getId();

        ResourceCategory resourceCategory = new ResourceCategory( id );

        resourceCategory.setAtBaseType( resourceCategoryVO.getAtBaseType() );
        resourceCategory.setAtSchemaLocation( resourceCategoryVO.getAtSchemaLocation() );
        resourceCategory.setAtType( resourceCategoryVO.getAtType() );
        resourceCategory.setHref( resourceCategoryVO.getHref() );
        resourceCategory.setDescription( resourceCategoryVO.getDescription() );
        resourceCategory.setIsRoot( resourceCategoryVO.getIsRoot() );
        resourceCategory.setLastUpdate( resourceCategoryVO.getLastUpdate() );
        resourceCategory.setLifecycleStatus( resourceCategoryVO.getLifecycleStatus() );
        resourceCategory.setName( resourceCategoryVO.getName() );
        resourceCategory.setParentId( mapFromCategoryId( resourceCategoryVO.getParentId() ) );
        resourceCategory.setVersion( resourceCategoryVO.getVersion() );
        resourceCategory.setCategory( resourceCategoryRefVOListToResourceCategoryRefList( resourceCategoryVO.getCategory() ) );
        resourceCategory.setRelatedParty( relatedPartyVOListToRelatedPartyList( resourceCategoryVO.getRelatedParty() ) );
        resourceCategory.setResourceCandidate( resourceCandidateRefVOListToResourceCandidateRefList( resourceCategoryVO.getResourceCandidate() ) );
        resourceCategory.setValidFor( timePeriodVOToTimePeriod( resourceCategoryVO.getValidFor() ) );

        return resourceCategory;
    }

    @Override
    public ResourceCategory map(ResourceCategoryUpdateVO resourceCategoryUpdateVO, String id) {
        if ( resourceCategoryUpdateVO == null && id == null ) {
            return null;
        }

        String id1 = null;
        if ( id != null ) {
            id1 = id;
        }

        ResourceCategory resourceCategory = new ResourceCategory( id1 );

        if ( resourceCategoryUpdateVO != null ) {
            resourceCategory.setAtBaseType( resourceCategoryUpdateVO.getAtBaseType() );
            resourceCategory.setAtSchemaLocation( resourceCategoryUpdateVO.getAtSchemaLocation() );
            resourceCategory.setAtType( resourceCategoryUpdateVO.getAtType() );
            resourceCategory.setDescription( resourceCategoryUpdateVO.getDescription() );
            resourceCategory.setIsRoot( resourceCategoryUpdateVO.getIsRoot() );
            resourceCategory.setLastUpdate( resourceCategoryUpdateVO.getLastUpdate() );
            resourceCategory.setLifecycleStatus( resourceCategoryUpdateVO.getLifecycleStatus() );
            resourceCategory.setName( resourceCategoryUpdateVO.getName() );
            resourceCategory.setParentId( mapFromCategoryId( resourceCategoryUpdateVO.getParentId() ) );
            resourceCategory.setVersion( resourceCategoryUpdateVO.getVersion() );
            resourceCategory.setCategory( resourceCategoryRefVOListToResourceCategoryRefList( resourceCategoryUpdateVO.getCategory() ) );
            resourceCategory.setRelatedParty( relatedPartyVOListToRelatedPartyList( resourceCategoryUpdateVO.getRelatedParty() ) );
            resourceCategory.setResourceCandidate( resourceCandidateRefVOListToResourceCandidateRefList( resourceCategoryUpdateVO.getResourceCandidate() ) );
            resourceCategory.setValidFor( timePeriodVOToTimePeriod( resourceCategoryUpdateVO.getValidFor() ) );
        }

        return resourceCategory;
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

    protected ResourceCategoryRefVO resourceCategoryRefToResourceCategoryRefVO(ResourceCategoryRef resourceCategoryRef) {
        if ( resourceCategoryRef == null ) {
            return null;
        }

        ResourceCategoryRefVO resourceCategoryRefVO = new ResourceCategoryRefVO();

        resourceCategoryRefVO.setId( mapFromURI( resourceCategoryRef.getId() ) );
        resourceCategoryRefVO.setHref( resourceCategoryRef.getHref() );
        resourceCategoryRefVO.setName( resourceCategoryRef.getName() );
        resourceCategoryRefVO.setVersion( resourceCategoryRef.getVersion() );
        resourceCategoryRefVO.setAtBaseType( resourceCategoryRef.getAtBaseType() );
        resourceCategoryRefVO.setAtSchemaLocation( resourceCategoryRef.getAtSchemaLocation() );
        resourceCategoryRefVO.setAtType( resourceCategoryRef.getAtType() );
        resourceCategoryRefVO.setAtReferredType( resourceCategoryRef.getAtReferredType() );

        return resourceCategoryRefVO;
    }

    protected List<ResourceCategoryRefVO> resourceCategoryRefListToResourceCategoryRefVOList(List<ResourceCategoryRef> list) {
        if ( list == null ) {
            return null;
        }

        List<ResourceCategoryRefVO> list1 = new ArrayList<ResourceCategoryRefVO>( list.size() );
        for ( ResourceCategoryRef resourceCategoryRef : list ) {
            list1.add( resourceCategoryRefToResourceCategoryRefVO( resourceCategoryRef ) );
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

    protected ResourceCategoryRef resourceCategoryRefVOToResourceCategoryRef(ResourceCategoryRefVO resourceCategoryRefVO) {
        if ( resourceCategoryRefVO == null ) {
            return null;
        }

        URI id = null;

        id = mapToURI( resourceCategoryRefVO.getId() );

        ResourceCategoryRef resourceCategoryRef = new ResourceCategoryRef( id );

        resourceCategoryRef.setAtBaseType( resourceCategoryRefVO.getAtBaseType() );
        resourceCategoryRef.setAtSchemaLocation( resourceCategoryRefVO.getAtSchemaLocation() );
        resourceCategoryRef.setAtType( resourceCategoryRefVO.getAtType() );
        resourceCategoryRef.setHref( resourceCategoryRefVO.getHref() );
        resourceCategoryRef.setName( resourceCategoryRefVO.getName() );
        resourceCategoryRef.setAtReferredType( resourceCategoryRefVO.getAtReferredType() );
        resourceCategoryRef.setVersion( resourceCategoryRefVO.getVersion() );

        return resourceCategoryRef;
    }

    protected List<ResourceCategoryRef> resourceCategoryRefVOListToResourceCategoryRefList(List<ResourceCategoryRefVO> list) {
        if ( list == null ) {
            return null;
        }

        List<ResourceCategoryRef> list1 = new ArrayList<ResourceCategoryRef>( list.size() );
        for ( ResourceCategoryRefVO resourceCategoryRefVO : list ) {
            list1.add( resourceCategoryRefVOToResourceCategoryRef( resourceCategoryRefVO ) );
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
        featureSpecificationCharacteristicRelationshipVO.setAtBaseType( featureSpecificationCharacteristicRelationship.getAtBaseType() );
        featureSpecificationCharacteristicRelationshipVO.setAtSchemaLocation( featureSpecificationCharacteristicRelationship.getAtSchemaLocation() );
        featureSpecificationCharacteristicRelationshipVO.setAtType( featureSpecificationCharacteristicRelationship.getAtType() );

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
        featureSpecificationRelationshipVO.setRelationshipType( featureSpecificationRelationship.getRelationshipType() );
        featureSpecificationRelationshipVO.setValidFor( timePeriodToTimePeriodVO( featureSpecificationRelationship.getValidFor() ) );
        featureSpecificationRelationshipVO.setAtBaseType( featureSpecificationRelationship.getAtBaseType() );
        featureSpecificationRelationshipVO.setAtSchemaLocation( featureSpecificationRelationship.getAtSchemaLocation() );
        featureSpecificationRelationshipVO.setAtType( featureSpecificationRelationship.getAtType() );

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
        featureSpecificationVO.setAtBaseType( featureSpecification.getAtBaseType() );
        featureSpecificationVO.setAtSchemaLocation( featureSpecification.getAtSchemaLocation() );
        featureSpecificationVO.setAtType( featureSpecification.getAtType() );

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

    protected ResourceSpecificationCharacteristicRelationshipVO resourceSpecificationCharacteristicRelationshipToResourceSpecificationCharacteristicRelationshipVO(ResourceSpecificationCharacteristicRelationship resourceSpecificationCharacteristicRelationship) {
        if ( resourceSpecificationCharacteristicRelationship == null ) {
            return null;
        }

        ResourceSpecificationCharacteristicRelationshipVO resourceSpecificationCharacteristicRelationshipVO = new ResourceSpecificationCharacteristicRelationshipVO();

        resourceSpecificationCharacteristicRelationshipVO.setCharacteristicSpecificationId( resourceSpecificationCharacteristicRelationship.getCharacteristicSpecificationId() );
        resourceSpecificationCharacteristicRelationshipVO.setName( resourceSpecificationCharacteristicRelationship.getName() );
        resourceSpecificationCharacteristicRelationshipVO.setRelationshipType( resourceSpecificationCharacteristicRelationship.getRelationshipType() );
        resourceSpecificationCharacteristicRelationshipVO.setResourceSpecificationHref( mapToURI( resourceSpecificationCharacteristicRelationship.getResourceSpecificationHref() ) );
        resourceSpecificationCharacteristicRelationshipVO.setResourceSpecificationId( mapFromResourceSpecificationRef( resourceSpecificationCharacteristicRelationship.getResourceSpecificationId() ) );
        resourceSpecificationCharacteristicRelationshipVO.setValidFor( timePeriodToTimePeriodVO( resourceSpecificationCharacteristicRelationship.getValidFor() ) );
        resourceSpecificationCharacteristicRelationshipVO.setAtBaseType( resourceSpecificationCharacteristicRelationship.getAtBaseType() );
        resourceSpecificationCharacteristicRelationshipVO.setAtSchemaLocation( resourceSpecificationCharacteristicRelationship.getAtSchemaLocation() );
        resourceSpecificationCharacteristicRelationshipVO.setAtType( resourceSpecificationCharacteristicRelationship.getAtType() );

        return resourceSpecificationCharacteristicRelationshipVO;
    }

    protected List<ResourceSpecificationCharacteristicRelationshipVO> resourceSpecificationCharacteristicRelationshipListToResourceSpecificationCharacteristicRelationshipVOList(List<ResourceSpecificationCharacteristicRelationship> list) {
        if ( list == null ) {
            return null;
        }

        List<ResourceSpecificationCharacteristicRelationshipVO> list1 = new ArrayList<ResourceSpecificationCharacteristicRelationshipVO>( list.size() );
        for ( ResourceSpecificationCharacteristicRelationship resourceSpecificationCharacteristicRelationship : list ) {
            list1.add( resourceSpecificationCharacteristicRelationshipToResourceSpecificationCharacteristicRelationshipVO( resourceSpecificationCharacteristicRelationship ) );
        }

        return list1;
    }

    protected ResourceSpecificationCharacteristicVO resourceSpecificationCharacteristicToResourceSpecificationCharacteristicVO(ResourceSpecificationCharacteristic resourceSpecificationCharacteristic) {
        if ( resourceSpecificationCharacteristic == null ) {
            return null;
        }

        ResourceSpecificationCharacteristicVO resourceSpecificationCharacteristicVO = new ResourceSpecificationCharacteristicVO();

        resourceSpecificationCharacteristicVO.setId( resourceSpecificationCharacteristic.getId() );
        resourceSpecificationCharacteristicVO.setConfigurable( resourceSpecificationCharacteristic.getConfigurable() );
        resourceSpecificationCharacteristicVO.setDescription( resourceSpecificationCharacteristic.getDescription() );
        resourceSpecificationCharacteristicVO.setExtensible( resourceSpecificationCharacteristic.getExtensible() );
        resourceSpecificationCharacteristicVO.setIsUnique( resourceSpecificationCharacteristic.getIsUnique() );
        resourceSpecificationCharacteristicVO.setMaxCardinality( resourceSpecificationCharacteristic.getMaxCardinality() );
        resourceSpecificationCharacteristicVO.setMinCardinality( resourceSpecificationCharacteristic.getMinCardinality() );
        resourceSpecificationCharacteristicVO.setName( resourceSpecificationCharacteristic.getName() );
        resourceSpecificationCharacteristicVO.setRegex( resourceSpecificationCharacteristic.getRegex() );
        resourceSpecificationCharacteristicVO.setValueType( resourceSpecificationCharacteristic.getValueType() );
        resourceSpecificationCharacteristicVO.setResourceSpecCharRelationship( resourceSpecificationCharacteristicRelationshipListToResourceSpecificationCharacteristicRelationshipVOList( resourceSpecificationCharacteristic.getResourceSpecCharRelationship() ) );
        resourceSpecificationCharacteristicVO.setResourceSpecCharacteristicValue( characteristicValueListToCharacteristicValueSpecificationVOList( resourceSpecificationCharacteristic.getResourceSpecCharacteristicValue() ) );
        resourceSpecificationCharacteristicVO.setValidFor( timePeriodToTimePeriodVO( resourceSpecificationCharacteristic.getValidFor() ) );

        return resourceSpecificationCharacteristicVO;
    }

    protected List<ResourceSpecificationCharacteristicVO> resourceSpecificationCharacteristicListToResourceSpecificationCharacteristicVOList(List<ResourceSpecificationCharacteristic> list) {
        if ( list == null ) {
            return null;
        }

        List<ResourceSpecificationCharacteristicVO> list1 = new ArrayList<ResourceSpecificationCharacteristicVO>( list.size() );
        for ( ResourceSpecificationCharacteristic resourceSpecificationCharacteristic : list ) {
            list1.add( resourceSpecificationCharacteristicToResourceSpecificationCharacteristicVO( resourceSpecificationCharacteristic ) );
        }

        return list1;
    }

    protected TargetResourceSchemaVO targetResourceSchemaToTargetResourceSchemaVO(TargetResourceSchema targetResourceSchema) {
        if ( targetResourceSchema == null ) {
            return null;
        }

        TargetResourceSchemaVO targetResourceSchemaVO = new TargetResourceSchemaVO();

        targetResourceSchemaVO.setAtSchemaLocation( mapToURI( targetResourceSchema.getAtSchemaLocation() ) );
        targetResourceSchemaVO.setAtType( targetResourceSchema.getAtType() );

        return targetResourceSchemaVO;
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
        featureSpecificationCharacteristicRelationship.setAtBaseType( featureSpecificationCharacteristicRelationshipVO.getAtBaseType() );
        featureSpecificationCharacteristicRelationship.setAtSchemaLocation( featureSpecificationCharacteristicRelationshipVO.getAtSchemaLocation() );
        featureSpecificationCharacteristicRelationship.setAtType( featureSpecificationCharacteristicRelationshipVO.getAtType() );

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
        featureSpecificationRelationship.setValidFor( timePeriodVOToTimePeriod( featureSpecificationRelationshipVO.getValidFor() ) );
        featureSpecificationRelationship.setAtBaseType( featureSpecificationRelationshipVO.getAtBaseType() );
        featureSpecificationRelationship.setAtSchemaLocation( featureSpecificationRelationshipVO.getAtSchemaLocation() );
        featureSpecificationRelationship.setAtType( featureSpecificationRelationshipVO.getAtType() );

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
        featureSpecification.setAtBaseType( featureSpecificationVO.getAtBaseType() );
        featureSpecification.setAtSchemaLocation( featureSpecificationVO.getAtSchemaLocation() );
        featureSpecification.setAtType( featureSpecificationVO.getAtType() );

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

    protected ResourceSpecificationCharacteristicRelationship resourceSpecificationCharacteristicRelationshipVOToResourceSpecificationCharacteristicRelationship(ResourceSpecificationCharacteristicRelationshipVO resourceSpecificationCharacteristicRelationshipVO) {
        if ( resourceSpecificationCharacteristicRelationshipVO == null ) {
            return null;
        }

        ResourceSpecificationCharacteristicRelationship resourceSpecificationCharacteristicRelationship = new ResourceSpecificationCharacteristicRelationship();

        resourceSpecificationCharacteristicRelationship.setCharacteristicSpecificationId( resourceSpecificationCharacteristicRelationshipVO.getCharacteristicSpecificationId() );
        resourceSpecificationCharacteristicRelationship.setName( resourceSpecificationCharacteristicRelationshipVO.getName() );
        resourceSpecificationCharacteristicRelationship.setRelationshipType( resourceSpecificationCharacteristicRelationshipVO.getRelationshipType() );
        resourceSpecificationCharacteristicRelationship.setResourceSpecificationHref( mapFromURI( resourceSpecificationCharacteristicRelationshipVO.getResourceSpecificationHref() ) );
        resourceSpecificationCharacteristicRelationship.setResourceSpecificationId( mapFromResourceSpecId( resourceSpecificationCharacteristicRelationshipVO.getResourceSpecificationId() ) );
        resourceSpecificationCharacteristicRelationship.setValidFor( timePeriodVOToTimePeriod( resourceSpecificationCharacteristicRelationshipVO.getValidFor() ) );
        resourceSpecificationCharacteristicRelationship.setAtBaseType( resourceSpecificationCharacteristicRelationshipVO.getAtBaseType() );
        resourceSpecificationCharacteristicRelationship.setAtSchemaLocation( resourceSpecificationCharacteristicRelationshipVO.getAtSchemaLocation() );
        resourceSpecificationCharacteristicRelationship.setAtType( resourceSpecificationCharacteristicRelationshipVO.getAtType() );

        return resourceSpecificationCharacteristicRelationship;
    }

    protected List<ResourceSpecificationCharacteristicRelationship> resourceSpecificationCharacteristicRelationshipVOListToResourceSpecificationCharacteristicRelationshipList(List<ResourceSpecificationCharacteristicRelationshipVO> list) {
        if ( list == null ) {
            return null;
        }

        List<ResourceSpecificationCharacteristicRelationship> list1 = new ArrayList<ResourceSpecificationCharacteristicRelationship>( list.size() );
        for ( ResourceSpecificationCharacteristicRelationshipVO resourceSpecificationCharacteristicRelationshipVO : list ) {
            list1.add( resourceSpecificationCharacteristicRelationshipVOToResourceSpecificationCharacteristicRelationship( resourceSpecificationCharacteristicRelationshipVO ) );
        }

        return list1;
    }

    protected ResourceSpecificationCharacteristic resourceSpecificationCharacteristicVOToResourceSpecificationCharacteristic(ResourceSpecificationCharacteristicVO resourceSpecificationCharacteristicVO) {
        if ( resourceSpecificationCharacteristicVO == null ) {
            return null;
        }

        ResourceSpecificationCharacteristic resourceSpecificationCharacteristic = new ResourceSpecificationCharacteristic();

        resourceSpecificationCharacteristic.setId( resourceSpecificationCharacteristicVO.getId() );
        resourceSpecificationCharacteristic.setConfigurable( resourceSpecificationCharacteristicVO.getConfigurable() );
        resourceSpecificationCharacteristic.setExtensible( resourceSpecificationCharacteristicVO.getExtensible() );
        resourceSpecificationCharacteristic.setIsUnique( resourceSpecificationCharacteristicVO.getIsUnique() );
        resourceSpecificationCharacteristic.setName( resourceSpecificationCharacteristicVO.getName() );
        resourceSpecificationCharacteristic.setRegex( resourceSpecificationCharacteristicVO.getRegex() );
        resourceSpecificationCharacteristic.setValueType( resourceSpecificationCharacteristicVO.getValueType() );
        resourceSpecificationCharacteristic.setDescription( resourceSpecificationCharacteristicVO.getDescription() );
        resourceSpecificationCharacteristic.setMaxCardinality( resourceSpecificationCharacteristicVO.getMaxCardinality() );
        resourceSpecificationCharacteristic.setMinCardinality( resourceSpecificationCharacteristicVO.getMinCardinality() );
        resourceSpecificationCharacteristic.setResourceSpecCharRelationship( resourceSpecificationCharacteristicRelationshipVOListToResourceSpecificationCharacteristicRelationshipList( resourceSpecificationCharacteristicVO.getResourceSpecCharRelationship() ) );
        resourceSpecificationCharacteristic.setResourceSpecCharacteristicValue( characteristicValueSpecificationVOListToCharacteristicValueList( resourceSpecificationCharacteristicVO.getResourceSpecCharacteristicValue() ) );
        resourceSpecificationCharacteristic.setValidFor( timePeriodVOToTimePeriod( resourceSpecificationCharacteristicVO.getValidFor() ) );

        return resourceSpecificationCharacteristic;
    }

    protected List<ResourceSpecificationCharacteristic> resourceSpecificationCharacteristicVOListToResourceSpecificationCharacteristicList(List<ResourceSpecificationCharacteristicVO> list) {
        if ( list == null ) {
            return null;
        }

        List<ResourceSpecificationCharacteristic> list1 = new ArrayList<ResourceSpecificationCharacteristic>( list.size() );
        for ( ResourceSpecificationCharacteristicVO resourceSpecificationCharacteristicVO : list ) {
            list1.add( resourceSpecificationCharacteristicVOToResourceSpecificationCharacteristic( resourceSpecificationCharacteristicVO ) );
        }

        return list1;
    }

    protected TargetResourceSchema targetResourceSchemaVOToTargetResourceSchema(TargetResourceSchemaVO targetResourceSchemaVO) {
        if ( targetResourceSchemaVO == null ) {
            return null;
        }

        TargetResourceSchema targetResourceSchema = new TargetResourceSchema();

        targetResourceSchema.setAtSchemaLocation( mapFromURI( targetResourceSchemaVO.getAtSchemaLocation() ) );
        targetResourceSchema.setAtType( targetResourceSchemaVO.getAtType() );

        return targetResourceSchema;
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

    protected ResourceCandidateRefVO resourceCandidateRefToResourceCandidateRefVO(ResourceCandidateRef resourceCandidateRef) {
        if ( resourceCandidateRef == null ) {
            return null;
        }

        ResourceCandidateRefVO resourceCandidateRefVO = new ResourceCandidateRefVO();

        resourceCandidateRefVO.setId( mapFromURI( resourceCandidateRef.getId() ) );
        resourceCandidateRefVO.setHref( resourceCandidateRef.getHref() );
        resourceCandidateRefVO.setName( resourceCandidateRef.getName() );
        resourceCandidateRefVO.setVersion( resourceCandidateRef.getVersion() );
        resourceCandidateRefVO.setAtBaseType( resourceCandidateRef.getAtBaseType() );
        resourceCandidateRefVO.setAtSchemaLocation( resourceCandidateRef.getAtSchemaLocation() );
        resourceCandidateRefVO.setAtType( resourceCandidateRef.getAtType() );
        resourceCandidateRefVO.setAtReferredType( resourceCandidateRef.getAtReferredType() );

        return resourceCandidateRefVO;
    }

    protected List<ResourceCandidateRefVO> resourceCandidateRefListToResourceCandidateRefVOList(List<ResourceCandidateRef> list) {
        if ( list == null ) {
            return null;
        }

        List<ResourceCandidateRefVO> list1 = new ArrayList<ResourceCandidateRefVO>( list.size() );
        for ( ResourceCandidateRef resourceCandidateRef : list ) {
            list1.add( resourceCandidateRefToResourceCandidateRefVO( resourceCandidateRef ) );
        }

        return list1;
    }

    protected ResourceCandidateRef resourceCandidateRefVOToResourceCandidateRef(ResourceCandidateRefVO resourceCandidateRefVO) {
        if ( resourceCandidateRefVO == null ) {
            return null;
        }

        String id = null;

        id = resourceCandidateRefVO.getId();

        ResourceCandidateRef resourceCandidateRef = new ResourceCandidateRef( id );

        resourceCandidateRef.setAtBaseType( resourceCandidateRefVO.getAtBaseType() );
        resourceCandidateRef.setAtSchemaLocation( resourceCandidateRefVO.getAtSchemaLocation() );
        resourceCandidateRef.setAtType( resourceCandidateRefVO.getAtType() );
        resourceCandidateRef.setHref( resourceCandidateRefVO.getHref() );
        resourceCandidateRef.setName( resourceCandidateRefVO.getName() );
        resourceCandidateRef.setAtReferredType( resourceCandidateRefVO.getAtReferredType() );
        resourceCandidateRef.setVersion( resourceCandidateRefVO.getVersion() );

        return resourceCandidateRef;
    }

    protected List<ResourceCandidateRef> resourceCandidateRefVOListToResourceCandidateRefList(List<ResourceCandidateRefVO> list) {
        if ( list == null ) {
            return null;
        }

        List<ResourceCandidateRef> list1 = new ArrayList<ResourceCandidateRef>( list.size() );
        for ( ResourceCandidateRefVO resourceCandidateRefVO : list ) {
            list1.add( resourceCandidateRefVOToResourceCandidateRef( resourceCandidateRefVO ) );
        }

        return list1;
    }
}

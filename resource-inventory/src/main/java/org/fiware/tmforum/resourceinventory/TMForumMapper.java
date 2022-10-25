package org.fiware.tmforum.resourceinventory;

import org.fiware.resourceinventory.model.ResourceCreateVO;
import org.fiware.resourceinventory.model.ResourceUpdateVO;
import org.fiware.resourceinventory.model.ResourceVO;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.resourceinventory.domain.Resource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.net.URI;

/**
 * Mapper between the internal model and api-domain objects
 */
@Mapper(componentModel = "jsr330", uses = IdHelper.class)
public interface TMForumMapper {

    // resource catalog

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    ResourceVO map(ResourceCreateVO resourceCreateVO, URI id);

    ResourceVO map(Resource resource);

    Resource map(ResourceVO resourceVO);

    @Mapping(target = "id", source = "id")
    Resource map(ResourceUpdateVO resourceUpdateVO, String id);
//
//
//
//    default URL map(String value) {
//        if (value == null) {
//            return null;
//        }
//        try {
//            return new URL(value);
//        } catch (MalformedURLException e) {
//            throw new MappingException(String.format("%s is not a URL.", value), e);
//        }
//    }
//
//    default String map(URL value) {
//        if (value == null) {
//            return null;
//        }
//        return value.toString();
//    }
//
//    default URI mapToURI(String value) {
//        if (value == null) {
//            return null;
//        }
//        return URI.create(value);
//    }
//
//    default String mapFromURI(URI value) {
//        if (value == null) {
//            return null;
//        }
//        return value.toString();
//    }
//
//    default FeatureRef mapFromFeatureId(String id) {
//        if (id == null) {
//            return null;
//        }
//        return new FeatureRef(IdHelper.toNgsiLd(id, "feature"));
//    }
//
//    default ResourceSpecificationRef mapFromResourceSpecId(String id) {
//        if (id == null) {
//            return null;
//        }
//        return new ResourceSpecificationRef(IdHelper.toNgsiLd(id, ResourceSpecification.TYPE_RESOURCE_SPECIFICATION));
//    }
//
//    default String mapFromResourceSpecificationRef(ResourceSpecificationRef resourceSpecificationRef) {
//        if (resourceSpecificationRef == null) {
//            return null;
//        }
//        return IdHelper.fromNgsiLd(resourceSpecificationRef.getId());
//    }
}



package org.fiware.tmforum.serviceinventory;

import org.fiware.tmforum.resource.ResourceRef;
import org.fiware.tmforum.service.*;
import org.fiware.tmforum.serviceinventory.domain.ServiceRefOrValue;
import org.mapstruct.Named;

import java.net.URI;
import java.util.Optional;

@Named("MappingHelper")
public class MappingHelper {


    @Named("toResourceRef")
    public static ResourceRef toResourceRef(String parentId) {
        if (parentId == null) {
            return null;
        }
        return new ResourceRef(parentId);
    }

    @Named("fromResourceRef")
    public static String fromResourceRef(ResourceRef parentId) {
        if (parentId == null) {
            return null;
        }
        return Optional.ofNullable(parentId.getEntityId()).map(URI::toString).orElse(null);
    }
}

package org.fiware.tmforum.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.util.ArrayList;
import java.util.List;

@MappingEnabled(entityType = {
        Resource.TYPE_RESOURCE,
        PhysicalResource.TYPE_PHYSICAL_RESOURCE,
        LogicalResource.TYPE_LOGICAL_RESOURCE,
        SoftwareResource.TYPE_SOFTWARE_RESOURCE,
        InstalledSoftware.TYPE_INSTALLED_SOFTWARE,
        SoftwareSupportPackage.TYPE_SOFTWARE_SUPPORT_PACKAGE,
        ApiResource.TYPE_API_RESOURCE,
        HostingPlatformRequirement.TYPE_HOSTING_PLATFORM_REQUIREMENT
})
@EqualsAndHashCode(callSuper = true)
public class ResourceRef extends RefEntity {

    public ResourceRef(@JsonProperty("id") String id) {
        super(id);
    }

    @Override
    @JsonIgnore
    public List<String> getReferencedTypes() {
        return new ArrayList<>(List.of(
                Resource.TYPE_RESOURCE,
                PhysicalResource.TYPE_PHYSICAL_RESOURCE,
                LogicalResource.TYPE_LOGICAL_RESOURCE,
                SoftwareResource.TYPE_SOFTWARE_RESOURCE,
                InstalledSoftware.TYPE_INSTALLED_SOFTWARE,
                SoftwareSupportPackage.TYPE_SOFTWARE_SUPPORT_PACKAGE,
                ApiResource.TYPE_API_RESOURCE,
                HostingPlatformRequirement.TYPE_HOSTING_PLATFORM_REQUIREMENT
        ));
    }
}

package org.fiware.tmforum.softwaremanagement.bean;

import jakarta.inject.Singleton;
import org.fiware.softwaremanagement.model.APISpecificationVO;
import org.fiware.softwaremanagement.model.APIVO;
import org.fiware.softwaremanagement.model.ConnectionSpecificationVO;
import org.fiware.softwaremanagement.model.ConnectionVO;
import org.fiware.softwaremanagement.model.HostingPlatformRequirementSpecificationVO;
import org.fiware.softwaremanagement.model.HostingPlatformRequirementVO;
import org.fiware.softwaremanagement.model.InstalledSoftwareVO;
import org.fiware.softwaremanagement.model.LogicalResourceSpecificationVO;
import org.fiware.softwaremanagement.model.LogicalResourceVO;
import org.fiware.softwaremanagement.model.PhysicalResourceSpecificationVO;
import org.fiware.softwaremanagement.model.PhysicalResourceVO;
import org.fiware.softwaremanagement.model.ResourceGraphSpecificationVO;
import org.fiware.softwaremanagement.model.ResourceGraphVO;
import org.fiware.softwaremanagement.model.ResourceSpecificationVO;
import org.fiware.softwaremanagement.model.ResourceVO;
import org.fiware.softwaremanagement.model.SoftwareResourceSpecificationVO;
import org.fiware.softwaremanagement.model.SoftwareResourceVO;
import org.fiware.softwaremanagement.model.SoftwareSpecificationVO;
import org.fiware.softwaremanagement.model.SoftwareSupportPackageSpecificationVO;
import org.fiware.softwaremanagement.model.SoftwareSupportPackageVO;
import org.fiware.tmforum.common.mapping.FieldCleaningModule;

import java.util.List;

/**
 * Applies the {@code fields} query parameter to this API's VOs.
 */
@Singleton
public class VOFieldCleaningModule extends FieldCleaningModule {

    public VOFieldCleaningModule() {
        super(List.of(
                ResourceVO.class,
                SoftwareResourceVO.class,
                ResourceGraphVO.class,
                PhysicalResourceVO.class,
                PhysicalResourceSpecificationVO.class,
                LogicalResourceVO.class,
                LogicalResourceSpecificationVO.class,
                InstalledSoftwareVO.class,
                HostingPlatformRequirementVO.class,
                HostingPlatformRequirementSpecificationVO.class,
                ConnectionVO.class,
                ConnectionSpecificationVO.class,
                APIVO.class,
                APISpecificationVO.class,
                ResourceGraphSpecificationVO.class,
                ResourceSpecificationVO.class,
                SoftwareResourceSpecificationVO.class,
                SoftwareSpecificationVO.class,
                SoftwareSupportPackageSpecificationVO.class,
                SoftwareSupportPackageVO.class));
    }
}

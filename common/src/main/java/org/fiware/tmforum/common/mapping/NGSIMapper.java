package org.fiware.tmforum.common.mapping;

import org.fiware.ngsi.model.EntityFragmentVO;
import org.fiware.ngsi.model.EntityVO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jsr330")
public interface NGSIMapper {

    default EntityFragmentVO map(EntityVO entityVO) {
        if (entityVO == null) {
            return null;
        }

        EntityFragmentVO entityFragmentVO = new EntityFragmentVO();
        if (entityFragmentVO.getAdditionalProperties() != null) {
            entityVO.getAdditionalProperties().clear();
        }
        entityVO.getAdditionalProperties().entrySet().forEach(entry -> entityFragmentVO.setAdditionalProperties(entry.getKey(), entry.getValue()));

        entityFragmentVO.setAtContext(entityVO.getAtContext());
        entityFragmentVO.setLocation(entityVO.getLocation());
        entityFragmentVO.setObservationSpace(entityVO.getObservationSpace());
        entityFragmentVO.setOperationSpace(entityVO.getOperationSpace());
        entityFragmentVO.setType(entityVO.getType());
        entityFragmentVO.setCreatedAt(entityVO.getCreatedAt());
        entityFragmentVO.setModifiedAt(entityVO.getModifiedAt());

        entityFragmentVO.setId(null);

        return entityFragmentVO;
    }

}

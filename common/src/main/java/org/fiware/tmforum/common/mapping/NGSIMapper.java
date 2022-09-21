package org.fiware.tmforum.common.mapping;

import org.fiware.ngsi.model.EntityFragmentVO;
import org.fiware.ngsi.model.EntityVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "jsr330")
public interface NGSIMapper {

	@Mapping(target = "id", expression = "java(null)")
	EntityFragmentVO map(EntityVO entityVO);

}

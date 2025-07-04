package org.fiware.tmforum.partyrole;

import org.fiware.partyRole.model.PartyRoleCreateVO;
import org.fiware.partyRole.model.PartyRoleUpdateVO;
import org.fiware.partyRole.model.PartyRoleVO;
import org.fiware.partyRole.model.TimePeriodVO;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.common.mapping.BaseMapper;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.partyrole.domain.PartyRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

@Mapper(componentModel = "jsr330", uses = {IdHelper.class})
public abstract class TMForumMapper extends BaseMapper {

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract PartyRoleVO map(PartyRoleCreateVO partyRoleCreateVO, URI id);

	@Mapping(target = "id", source = "id")
	public abstract PartyRole map(PartyRoleUpdateVO partyRoleUpdateVO, String id);

	@Mapping(target = "id", source = "id")
	public abstract PartyRole map(PartyRoleVO partyRoleUpdateVO);

	@Mapping(target = "id", source = "id")
	public abstract PartyRoleVO map(PartyRole partyRoleUpdateVO);


	public abstract TimePeriod map(TimePeriodVO value);

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

	public URL mapToURL(String value) {
		if (value == null) {
			return null;
		}
		try {
			return new URL(value);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String mapFromURI(URI value) {
		if (value == null) {
			return null;
		}
		return value.toString();
	}

}

package org.fiware.tmforum.common.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Data
public class TaxDefinition {

	private String id;
	private String name;
	private String taxType;
	private String atBaseType;
	private String atSchemaLocation;
	private String atType;
	private String atReferredType;

}

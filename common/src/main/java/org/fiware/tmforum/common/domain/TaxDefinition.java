package org.fiware.tmforum.common.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.mapping.IdHelper;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Data
public class TaxDefinition {

	private String id;
	private String name;
	private String taxType;
	private String atBaseType;
	private URI atSchemaLocation;
	private String atType;
	private String atReferredType;

}

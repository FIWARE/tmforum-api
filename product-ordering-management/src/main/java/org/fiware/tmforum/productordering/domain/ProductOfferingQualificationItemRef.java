package org.fiware.tmforum.productordering.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.mapping.annotations.DatasetId;
import org.fiware.tmforum.mapping.annotations.RelationshipObject;

import java.net.URI;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProductOfferingQualificationItemRef extends Entity implements ReferencedEntity {

	private String id;
	private URI href;
	private String name;
	private URI productOfferingQualificationHref;
	private URI productOfferingQualificationId;
	private String productOfferingQualificationName;
	private String atReferredType;

	@Override public List<String> getReferencedTypes() {
		return List.of(atReferredType);
	}

	@Override public URI getEntityId() {
		return getProductOfferingQualificationId();
	}
}

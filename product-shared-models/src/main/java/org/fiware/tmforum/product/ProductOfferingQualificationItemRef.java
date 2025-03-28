package org.fiware.tmforum.product;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.validation.ReferencedEntity;

import java.net.URI;
import java.util.ArrayList;
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
		return new ArrayList<>(List.of(atReferredType));
	}

	@Override public URI getEntityId() {
		return getProductOfferingQualificationId();
	}
}

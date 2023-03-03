package org.fiware.tmforum.productordering.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.validation.ReferencedEntity;

import java.net.URI;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class QuoteItemRef extends Entity implements ReferencedEntity {

	private String id;
	private URI href;
	private String name;
	private URI quoteHref;
	private URI quoteId;
	private String quoteName;
	private String atReferredType;

	@Override public List<String> getReferencedTypes() {
		return List.of(getAtReferredType());
	}

	@Override public URI getEntityId() {
		return quoteId;
	}
}

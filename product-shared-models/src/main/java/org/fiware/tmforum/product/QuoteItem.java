package org.fiware.tmforum.product;

import lombok.Data;
import org.fiware.tmforum.common.domain.AttachmentRefOrValue;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.RelatedParty;
import org.fiware.tmforum.resource.Note;

import java.util.List;

@Data
public class QuoteItem extends Entity {

	private String tmfId;
	private String action;
	private Integer quantity;
	private QuoteItemState state;
	private List<AppointmentRef> appointment;
	private List<AttachmentRefOrValue> attachment;
	private List<Note> note;
	private ProductRefOrValue product;
	private ProductOfferingRef productOffering;
	private ProductOfferingQualificationItemRef productOfferingQualificationItem;
	private List<QuoteItem> quoteItem;
	private List<Authorization> quoteItemAuthorization;
	private List<QuotePrice> quoteItemPrice;
	private List<QuoteItemRelationship> quoteItemRelationship;
	private List<RelatedParty> relatedParty;
}

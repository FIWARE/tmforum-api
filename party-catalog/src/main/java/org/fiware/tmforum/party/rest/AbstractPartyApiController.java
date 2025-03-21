package org.fiware.tmforum.party.rest;

import org.fiware.tmforum.common.domain.TaxDefinition;
import org.fiware.tmforum.common.domain.TaxExemptionCertificate;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class AbstractPartyApiController<T> extends AbstractApiController<T> {

	public AbstractPartyApiController(QueryParser queryParser, ReferenceValidationService validationService,
									  TmForumRepository repository, TMForumEventHandler eventHandler) {
		super(queryParser, validationService, repository, eventHandler);
	}

	protected void validateTaxExemptions(List<TaxExemptionCertificate> taxExemptionCertificates) {
		List<String> taxExIds = taxExemptionCertificates.stream()
				.filter(Objects::nonNull)
				.map(TaxExemptionCertificate::getCertificateId)
				.toList();
		if (taxExIds.size() != new HashSet<>(taxExIds).size()) {
			throw new TmForumException(String.format("Duplicate tax exemption ids are not allowed - ids: %s", taxExIds),
					TmForumExceptionReason.INVALID_DATA);
		}
		List<String> taxDefIds = taxExemptionCertificates.stream()
				.filter(Objects::nonNull)
				.map(TaxExemptionCertificate::getTaxDefinition)
				.filter(Objects::nonNull)
				.flatMap(List::stream)
				.filter(Objects::nonNull)
				.map(TaxDefinition::getId)
				.toList();
		if (taxDefIds.size() != new HashSet<>(taxDefIds).size()) {
			throw new TmForumException(
					String.format("Duplicate tax definition ids are not allowed - ids: %s", taxDefIds),
					TmForumExceptionReason.INVALID_DATA);
		}
	}

}

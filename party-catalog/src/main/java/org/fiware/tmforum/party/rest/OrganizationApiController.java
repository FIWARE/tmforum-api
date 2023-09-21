package org.fiware.tmforum.party.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.party.api.OrganizationApi;
import org.fiware.party.model.OrganizationCreateVO;
import org.fiware.party.model.OrganizationUpdateVO;
import org.fiware.party.model.OrganizationVO;
import org.fiware.tmforum.common.EventHandler;
import org.fiware.tmforum.common.exception.DeletionException;
import org.fiware.tmforum.common.exception.DeletionExceptionReason;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.party.TMForumMapper;
import org.fiware.tmforum.party.domain.organization.Organization;
import org.fiware.tmforum.party.domain.organization.OrganizationIdentification;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class OrganizationApiController extends AbstractPartyApiController<Organization> implements OrganizationApi {

	private final TMForumMapper tmForumMapper;

	public OrganizationApiController(TmForumRepository partyRepository, ReferenceValidationService validationService,
			TMForumMapper tmForumMapper, EventHandler eventHandler) {
		super(validationService, partyRepository, eventHandler);
		this.tmForumMapper = tmForumMapper;
	}

	@Override
	public Mono<HttpResponse<OrganizationVO>> createOrganization(OrganizationCreateVO organizationCreateVO) {

		Organization organization = tmForumMapper.map(tmForumMapper.map(organizationCreateVO,
				IdHelper.toNgsiLd(UUID.randomUUID().toString(), Organization.TYPE_ORGANIZATION)));

		return create(getCheckingMono(organization), Organization.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}

	private Mono<Organization> getCheckingMono(Organization organization) {
		Optional.ofNullable(organization.getTaxExemptionCertificate()).ifPresent(this::validateTaxExemptions);
		Optional.ofNullable(organization.getOrganizationIdentification())
				.ifPresent(this::validateOrganizationIdentifications);

		List<List<? extends ReferencedEntity>> references = new ArrayList<>();
		references.add(organization.getRelatedParty());
		references.add(organization.getOrganizationChildRelationship());
		Optional.ofNullable(organization.getOrganizationParentRelationship())
				.ifPresent(parent -> references.add(List.of(parent)));

		return getCheckingMono(organization, references)
				.onErrorMap(throwable ->
						new TmForumException(
								String.format("Was not able to create organization %s", organization.getId()),
								throwable,
								TmForumExceptionReason.INVALID_RELATIONSHIP));
	}

	private void validateOrganizationIdentifications(List<OrganizationIdentification> organizationIdentifications) {
		List<String> organizationIds = organizationIdentifications
				.stream()
				.map(OrganizationIdentification::getIdentificationId)
				.toList();
		if (organizationIds.size() != new HashSet<>(organizationIds).size()) {
			throw new TmForumException(
					String.format("Duplicate organization identification ids are not allowed - ids: %s",
							organizationIds), TmForumExceptionReason.INVALID_DATA);
		}
	}

	@Override
	public Mono<HttpResponse<Object>> deleteOrganization(String id) {

		if (!IdHelper.isNgsiLdId(id)) {
			throw new DeletionException("Did not receive a valid id, such organization cannot exist.",
					DeletionExceptionReason.NOT_FOUND);
		}

		return repository.deleteDomainEntity(URI.create(id))
				.then(Mono.just(HttpResponse.noContent()));
	}

	@Override
	public Mono<HttpResponse<List<OrganizationVO>>> listOrganization(@Nullable String fields, @Nullable Integer offset,
			@Nullable Integer limit) {

		return list(offset, limit, Organization.TYPE_ORGANIZATION, Organization.class)
				.map(organizationStream -> organizationStream.map(tmForumMapper::map).toList())
				.switchIfEmpty(Mono.just(List.of()))
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<OrganizationVO>> patchOrganization(@NonNull String id,
			@NonNull OrganizationUpdateVO organizationUpdateVO) {
		// non-ngsi-ld ids cannot exist.
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such organization cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
		}
		Organization organization = tmForumMapper.map(organizationUpdateVO, id);

		return patch(id, organization, getCheckingMono(organization), Organization.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<OrganizationVO>> retrieveOrganization(@NonNull String id, @Nullable String fields) {

		return retrieve(id, Organization.class)
				.switchIfEmpty(
						Mono.error(
								new TmForumException("No such organization exists.", TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}
}

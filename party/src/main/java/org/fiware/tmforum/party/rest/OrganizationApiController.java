package org.fiware.tmforum.party.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.reactivex.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.party.api.OrganizationApi;
import org.fiware.party.model.OrganizationCreateVO;
import org.fiware.party.model.OrganizationUpdateVO;
import org.fiware.party.model.OrganizationVO;
import org.fiware.tmforum.common.exception.NonExistentReferenceException;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.party.TMForumMapper;
import org.fiware.tmforum.party.domain.TaxExemptionCertificate;
import org.fiware.tmforum.party.domain.organization.Organization;
import org.fiware.tmforum.party.exception.PartyCreationException;
import org.fiware.tmforum.party.repository.PartyRepository;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
@RequiredArgsConstructor
public class OrganizationApiController implements OrganizationApi {

	private final TMForumMapper tmForumMapper;
	private final PartyRepository partyRepository;
	private final ReferenceValidationService validationService;

	@Override
	public Single<HttpResponse<OrganizationVO>> createOrganization(OrganizationCreateVO organizationCreateVO) {

		OrganizationVO organizationVO = tmForumMapper.map(organizationCreateVO, IdHelper.toNgsiLd(UUID.randomUUID().toString(), Organization.TYPE_ORGANIZATION));
		Organization organization = tmForumMapper.map(organizationVO);

		Single<Organization> organizationSingle = Single.just(organization);
		Single<Organization> checkingSingle;
		try {

			if (organization.getOrganizationChildRelationship() != null && !organization.getOrganizationChildRelationship().isEmpty()) {
				checkingSingle = validationService.getCheckingSingleOrThrow(organization.getOrganizationChildRelationship(), organization);

				organizationSingle = Single.zip(organizationSingle, checkingSingle, (p1, p2) -> p1);
			}
			if (organization.getOrganizationParentRelationship() != null) {
				checkingSingle = validationService.getCheckingSingleOrThrow(List.of(organization.getOrganizationParentRelationship()), organization);
				organizationSingle = Single.zip(organizationSingle, checkingSingle, (p1, p2) -> p1);
			}

			if (organization.getRelatedParty() != null && !organization.getRelatedParty().isEmpty()) {
				checkingSingle = validationService.getCheckingSingleOrThrow(organization.getRelatedParty(), organization);
				organizationSingle = Single.zip(organizationSingle, checkingSingle, (p1, p2) -> p1);
			}
		} catch (NonExistentReferenceException e) {
			throw new PartyCreationException(String.format("Was not able to create organization %s", organization.getId()), e);
		}

		List<TaxExemptionCertificate> taxExemptionCertificates = Optional.ofNullable(organization.getTaxExemptionCertificate()).orElseGet(List::of);
		if (!taxExemptionCertificates.isEmpty()) {
			Single<List<TaxExemptionCertificate>> taxExemptionCertificatesSingles =
					Single.zip(
							taxExemptionCertificates.stream()
									.map(partyRepository::createTaxExemptionCertificate)
									.toList(),
							t -> Arrays.stream(t).map(TaxExemptionCertificate.class::cast).toList());

			Single<Organization> updatingSingle = taxExemptionCertificatesSingles
					.map(updatedTaxExemptions -> {
						organization.setTaxExemptionCertificate(updatedTaxExemptions);
						return organization;
					});
			organizationSingle = Single.zip(organizationSingle, updatingSingle, (p1, p2) -> p1);
		}

		return organizationSingle
				.flatMap(orgToCreate -> partyRepository.createOrganization(orgToCreate).toSingleDefault(orgToCreate))
				.cast(Organization.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}


	@Override
	public Single<HttpResponse<Object>> deleteOrganization(String id) {
		return partyRepository.deleteParty(IdHelper.toNgsiLd(id, Organization.TYPE_ORGANIZATION)).toSingleDefault(HttpResponse.noContent());
	}

	@Override
	public Single<HttpResponse<List<OrganizationVO>>> listOrganization(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
		return partyRepository.findOrganizations()
				.map(List::stream)
				.map(organizationStream -> organizationStream.map(tmForumMapper::map).toList())
				.map(HttpResponse::ok);

	}

	@Override
	public Single<HttpResponse<OrganizationVO>> patchOrganization(String id, OrganizationUpdateVO organization) {
		// implement proper patch
		return null;
	}

	@Override
	public Single<HttpResponse<OrganizationVO>> retrieveOrganization(String id, @Nullable String fields) {
		// non-ngsi-ld ids cannot exist.
		if (!IdHelper.isNgsiLdId(id)) {
			return Single.just(HttpResponse.notFound());
		}


		return partyRepository
				.getOrganization(URI.create(id))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok)
				.switchIfEmpty(Single.just(HttpResponse.notFound()))
				.map(HttpResponse.class::cast);
	}
}

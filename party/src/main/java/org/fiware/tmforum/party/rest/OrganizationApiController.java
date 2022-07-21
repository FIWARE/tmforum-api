package org.fiware.tmforum.party.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.party.api.OrganizationApi;
import org.fiware.party.model.OrganizationCreateVO;
import org.fiware.party.model.OrganizationUpdateVO;
import org.fiware.party.model.OrganizationVO;
import org.fiware.tmforum.common.ValidationService;
import org.fiware.tmforum.party.TMForumMapper;
import org.fiware.tmforum.party.domain.TaxExemptionCertificate;
import org.fiware.tmforum.party.domain.organization.Organization;
import org.fiware.tmforum.party.repository.PartyRepository;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Controller
@RequiredArgsConstructor
public class OrganizationApiController implements OrganizationApi {

	private final TMForumMapper tmForumMapper;
	private final PartyRepository partyRepository;
	private final ValidationService validationService;

	@Override
	public Single<HttpResponse<OrganizationVO>> createOrganization(OrganizationCreateVO organizationCreateVO) {

		OrganizationVO organizationVO = tmForumMapper.map(organizationCreateVO);
		Organization organization = tmForumMapper.map(organizationVO);

		Single<Organization> organizationSingle = Single.just(organization);
		if (organization.getOrganizationChildRelationship() != null && !organization.getOrganizationChildRelationship().isEmpty()) {
			Single<Organization> checkingSingle = validationService.getCheckingSingle(organization.getOrganizationChildRelationship(), organization);
			organizationSingle = Single.zip(organizationSingle, checkingSingle, (p1, p2) -> p1);
		}
		if (organization.getOrganizationParentRelationship() != null) {
			Single<Organization> checkingSingle = validationService.getCheckingSingle(List.of(organization.getOrganizationParentRelationship()), organization);
			organizationSingle = Single.zip(organizationSingle, checkingSingle, (p1, p2) -> p1);
		}

		if (organization.getRelatedParty() != null && !organization.getRelatedParty().isEmpty()) {
			Single<Organization> checkingSingle = validationService.getCheckingSingle(organization.getRelatedParty(), organization);
			organizationSingle = Single.zip(organizationSingle, checkingSingle, (p1, p2) -> p1);
		}

		List<TaxExemptionCertificate> taxExemptionCertificates = Optional.ofNullable(organization.getTaxExemptionCertificate()).orElseGet(List::of);
		if (!taxExemptionCertificates.isEmpty()) {
			Single<List<TaxExemptionCertificate>> taxExemptionCertificatesSingles =
					Single.zip(taxExemptionCertificates.stream().map(partyRepository::getOrCreate).toList(), t -> Arrays.stream(t).map(TaxExemptionCertificate.class::cast).toList());

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
				.subscribeOn(Schedulers.io())
				.map(HttpResponse::created);
	}


	@Override
	public Single<HttpResponse<Object>> deleteOrganization(String id) {
		return partyRepository.deleteParty(id).toSingleDefault(HttpResponse.noContent());
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
		return partyRepository
				.getOrganization(id)
				.map(tmForumMapper::map)
				.toSingle()
				.map(HttpResponse::ok);
	}
}

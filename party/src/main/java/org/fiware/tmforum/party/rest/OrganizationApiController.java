package org.fiware.tmforum.party.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.party.api.OrganizationApi;
import org.fiware.party.model.OrganizationCreateVO;
import org.fiware.party.model.OrganizationUpdateVO;
import org.fiware.party.model.OrganizationVO;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.party.TMForumMapper;
import org.fiware.tmforum.party.domain.TaxExemptionCertificate;
import org.fiware.tmforum.party.domain.individual.Individual;
import org.fiware.tmforum.party.domain.organization.Organization;
import org.fiware.tmforum.party.exception.PartyCreationException;
import org.fiware.tmforum.party.exception.PartyDeletionException;
import org.fiware.tmforum.party.exception.PartyExceptionReason;
import org.fiware.tmforum.party.exception.PartyUpdateException;
import org.fiware.tmforum.party.repository.PartyRepository;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.fiware.tmforum.common.CommonConstants.DEFAULT_LIMIT;
import static org.fiware.tmforum.common.CommonConstants.DEFAULT_OFFSET;

@Slf4j
@Controller("${general.basepath:/}")
public class OrganizationApiController extends AbstractApiController implements OrganizationApi {


    public OrganizationApiController(TMForumMapper tmForumMapper, PartyRepository partyRepository, ReferenceValidationService validationService) {
        super(tmForumMapper, partyRepository, validationService);
    }

    @Override
    public Mono<HttpResponse<OrganizationVO>> createOrganization(OrganizationCreateVO organizationCreateVO) {

        OrganizationVO organizationVO = tmForumMapper.map(organizationCreateVO, IdHelper.toNgsiLd(UUID.randomUUID().toString(), Organization.TYPE_ORGANIZATION));
        Organization organization = tmForumMapper.map(organizationVO);

        Mono<Organization> checkingMono = getCheckingMono(organization);
        checkingMono = taxExemptionHandlingMono(organization, checkingMono, organization.getTaxExemptionCertificate(), organization::setTaxExemptionCertificate, false);

        return checkingMono
                .flatMap(orgToCreate -> partyRepository.createOrganization(orgToCreate).then(Mono.just(orgToCreate)))
                .onErrorMap(t -> {
                    if (t instanceof HttpClientResponseException e) {
                        return switch (e.getStatus()) {
                            case CONFLICT -> new PartyCreationException(String.format("Conflict on creating the organization: %s", e.getMessage()), PartyExceptionReason.CONFLICT);
                            case BAD_REQUEST -> new PartyCreationException(String.format("Did not receive a valid organization: %s.", e.getMessage()), PartyExceptionReason.INVALID_DATA);
                            default -> new PartyCreationException(String.format("Unspecified downstream error: %s", e.getMessage()), PartyExceptionReason.UNKNOWN);
                        };
                    } else {
                        return t;
                    }
                })
                .cast(Organization.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);
    }


    private Mono<Organization> getCheckingMono(Organization organization) {
        Mono<Organization> organizationMono = Mono.just(organization);
        Mono<Organization> checkingMono;
        if (organization.getOrganizationChildRelationship() != null && !organization.getOrganizationChildRelationship().isEmpty()) {
            checkingMono = validationService.getCheckingMono(organization.getOrganizationChildRelationship(), organization)
                    .onErrorMap(throwable -> new PartyCreationException(String.format("Was not able to create organization %s", organization.getId()), throwable, PartyExceptionReason.INVALID_RELATIONSHIP));

            organizationMono = Mono.zip(organizationMono, checkingMono, (p1, p2) -> p1);
        }
        if (organization.getOrganizationParentRelationship() != null) {
            checkingMono = validationService.getCheckingMono(List.of(organization.getOrganizationParentRelationship()), organization)
                    .onErrorMap(throwable -> new PartyCreationException(String.format("Was not able to create organization %s", organization.getId()), throwable, PartyExceptionReason.INVALID_RELATIONSHIP));
            organizationMono = Mono.zip(organizationMono, checkingMono, (p1, p2) -> p1);
        }

        if (organization.getRelatedParty() != null && !organization.getRelatedParty().isEmpty()) {
            checkingMono = validationService.getCheckingMono(organization.getRelatedParty(), organization)
                    .onErrorMap(throwable -> new PartyCreationException(String.format("Was not able to create organization %s", organization.getId()), throwable, PartyExceptionReason.INVALID_RELATIONSHIP));
            organizationMono = Mono.zip(organizationMono, checkingMono, (p1, p2) -> p1);
        }
        return organizationMono;
    }

    @Override
    public Mono<HttpResponse<Object>> deleteOrganization(String id) {

        if (!IdHelper.isNgsiLdId(id)) {
            throw new PartyDeletionException("Did not receive a valid id, such organization cannot exist.", PartyExceptionReason.NOT_FOUND);
        }

        return partyRepository.deleteParty(URI.create(id))
                .then(Mono.just(HttpResponse.noContent()));
    }

    @Override
    public Mono<HttpResponse<List<OrganizationVO>>> listOrganization(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
        offset = Optional.ofNullable(offset).orElse(DEFAULT_OFFSET);
        limit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT);

        return partyRepository.findOrganizations(offset, limit)
                .map(List::stream)
                .map(organizationStream -> organizationStream.map(tmForumMapper::map).toList())
                .map(HttpResponse::ok);

    }

    @Override
    public Mono<HttpResponse<OrganizationVO>> patchOrganization(String id, OrganizationUpdateVO organizationUpdateVO) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new PartyUpdateException("Did not receive a valid id, such organization cannot exist.", PartyExceptionReason.NOT_FOUND);
        }

        Organization updatedOrganization = tmForumMapper.map(tmForumMapper.map(organizationUpdateVO, id));

        URI idUri = URI.create(id);
        return partyRepository
                .getOrganization(idUri)
                .flatMap(organization ->
                        taxExemptionHandlingMono(
                                organization,
                                getCheckingMono(updatedOrganization),
                                updatedOrganization.getTaxExemptionCertificate(),
                                organization::setTaxExemptionCertificate,
                                true)
                                .flatMap(uo -> partyRepository.updateParty(id, updatedOrganization)
                                        .then(partyRepository.getOrganization(idUri))
                                        .map(tmForumMapper::map)
                                        .map(HttpResponse::ok)
                                        .onErrorMap(error -> new PartyUpdateException("Was not able to update organization.", PartyExceptionReason.UNKNOWN)))
                )
                .map(HttpResponse.class::cast);
    }

    @Override
    public Mono<HttpResponse<OrganizationVO>> retrieveOrganization(String id, @Nullable String fields) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new PartyDeletionException("Did not receive a valid id, such organization cannot exist.", PartyExceptionReason.NOT_FOUND);
        }

        return partyRepository
                .getOrganization(URI.create(id))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok)
                .switchIfEmpty(Mono.error(new PartyDeletionException("No such organization exists.", PartyExceptionReason.NOT_FOUND)))
                .map(HttpResponse.class::cast);
    }
}

package org.fiware.tmforum.account.domain;

import lombok.Data;
import org.fiware.tmforum.common.domain.Money;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.common.domain.RelatedParty;
import org.fiware.tmforum.common.domain.ContactMedium;
import java.util.List;

import java.net.URI;

@Data
public class Contact {

    private String contactName;
    private String contactType;
    private String partyRoleType;
    private List<ContactMedium> contactMedium;
    private RelatedParty relatedParty;
    private TimePeriod validFor;
    private String atBaseType;
    private URI atSchemaLocation;
    private String atType;
}
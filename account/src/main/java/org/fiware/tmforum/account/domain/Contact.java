package org.fiware.tmforum.account.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.*;

import java.util.List;

import java.net.URI;

@Data
@EqualsAndHashCode(callSuper = true)
public class Contact extends Entity {

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
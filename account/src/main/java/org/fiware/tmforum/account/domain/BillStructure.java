package org.fiware.tmforum.account.domain;

import lombok.Data;
import org.fiware.tmforum.common.domain.Money;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.net.URI;

@Data
public class BillStructure {

    private String relationshipType;
    private AccountRef account;
    private TimePeriod validFor;
    private String atBaseType;
    private URI atSchemaLocation;
    private String atType;
}
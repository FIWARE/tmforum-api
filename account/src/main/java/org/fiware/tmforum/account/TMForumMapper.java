package org.fiware.tmforum.account;

import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.BaseMapper;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.account.model.*;
import org.fiware.tmforum.account.domain.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Mapper between the internal model and api-domain objects
 */
@Mapper(componentModel = "jsr330", uses = { IdHelper.class, MappingHelper.class })
public abstract class TMForumMapper extends BaseMapper {

    //BillFormat

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract BillFormatVO map(BillFormatCreateVO billFormatCreateVO, URI id);

    public abstract BillFormatVO map(BillFormat billFormat);

    @Mapping(target = "href", source = "id")
    public abstract BillFormat map(BillFormatVO billFormatVO);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract BillFormatVO map(BillFormatUpdateVO billFormatUpdateVO, String id);

    //BillingAccount

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract BillingAccountVO map(BillingAccountCreateVO billingAccountCreateVO, URI id);

    public abstract BillingAccountVO map(BillingAccount billingAccount);

    @Mapping(target = "href", source = "id")
    public abstract BillingAccount map(BillingAccountVO billingAccountVO);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract BillingAccountVO map(BillingAccountUpdateVO billingAccountUpdateVO, String id);

    //BillingCycleSpecification

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract BillingCycleSpecificationVO map(BillingCycleSpecificationCreateVO billingCycleSpecificationCreateVO, URI id);

    public abstract BillingCycleSpecificationVO map(BillingCycleSpecification billingCycleSpecification);

    @Mapping(target = "href", source = "id")
    public abstract BillingCycleSpecification map(BillingCycleSpecificationVO billingCycleSpecificationVO);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract BillingCycleSpecificationVO map(BillingCycleSpecificationUpdateVO billingCycleSpecificationUpdateVO, String id);

    //BillPresentationMedia

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract BillPresentationMediaVO map(BillPresentationMediaCreateVO billPresentationMediaCreateVO, URI id);

    public abstract BillPresentationMediaVO map(BillPresentationMedia billPresentationMedia);

    @Mapping(target = "href", source = "id")
    public abstract BillPresentationMedia map(BillPresentationMediaVO billPresentationMediaVO);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract BillPresentationMediaVO map(BillPresentationMediaUpdateVO billPresentationMediaUpdateVO, String id);

    //FinancialAccount

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract FinancialAccountVO map(FinancialAccountCreateVO financialAccountCreateVO, URI id);

    public abstract FinancialAccountVO map(FinancialAccount financialAccount);

    @Mapping(target = "href", source = "id")
    public abstract FinancialAccount map(FinancialAccountVO financialAccountVO);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract FinancialAccountVO map(FinancialAccountUpdateVO financialAccountUpdateVO, String id);

    //PartyAccount

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract PartyAccountVO map(PartyAccountCreateVO partyAccountCreateVO, URI id);

    public abstract PartyAccountVO map(PartyAccount partyAccount);

    @Mapping(target = "href", source = "id")
    public abstract PartyAccount map(PartyAccountVO partyAccountVO);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract PartyAccountVO map(PartyAccountUpdateVO partyAccountUpdateVO, String id);

    //SettlementAccount

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract SettlementAccountVO map(SettlementAccountCreateVO settlementAccountCreateVO, URI id);

    public abstract SettlementAccountVO map(SettlementAccount settlementAccount);

    @Mapping(target = "href", source = "id")
    public abstract SettlementAccount map(SettlementAccountVO settlementAccountVO);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract SettlementAccountVO map(SettlementAccountUpdateVO settlementAccountUpdateVO, String id);

    @Mapping(target = "query", source = "rawQuery")
    public abstract EventSubscriptionVO map(TMForumSubscription subscription);

    public abstract TimePeriod map(TimePeriodVO value);

    public String map(URL value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    public URI mapToURI(String value) {
        if (value == null) {
            return null;
        }
        return URI.create(value);
    }

    public URL mapToURL(String value) {
        if (value == null) {
            return null;
        }
        try {
            return new URL(value);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String mapFromURI(URI value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

}

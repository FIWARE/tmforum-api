package org.fiware.tmforum.common.notification;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

public class EventConstants {
    public final static String EVENT_GROUP_PRODUCT = "Product";
    public final static String EVENT_GROUP_CATALOG = "Catalog";
    public final static String EVENT_GROUP_CATEGORY = "Category";
    public final static String EVENT_GROUP_PRODUCT_OFFERING = "ProductOffering";
    public final static String EVENT_GROUP_PRODUCT_OFFERING_PRICE = "ProductOfferingPrice";
    public final static String EVENT_GROUP_PRODUCT_SPECIFICATION = "ProductSpecification";
    public final static String EVENT_GROUP_SERVICE_CANDIDATE = "ServiceCandidate";
    public final static String EVENT_GROUP_SERVICE_CATALOG = "ServiceCatalog";
    public final static String EVENT_GROUP_SERVICE_CATEGORY = "ServiceCategory";
    public final static String EVENT_GROUP_SERVICE_SPECIFICATION = "ServiceSpecification";
    public final static String EVENT_GROUP_RESOURCE = "Resource";
    public final static String EVENT_GROUP_HEAL = "Heal";
    public final static String EVENT_GROUP_MIGRATE = "Migrate";
    public final static String EVENT_GROUP_MONITOR = "Monitor";
    public final static String EVENT_GROUP_RESOURCE_FUNCTION = "ResourceFunction";
    public final static String EVENT_GROUP_SCALE = "Scale";
    public final static String EVENT_GROUP_RESOURCE_CANDIDATE = "ResourceCandidate";
    public final static String EVENT_GROUP_RESOURCE_CATALOG = "ResourceCatalog";
    public final static String EVENT_GROUP_RESOURCE_CATEGORY = "ResourceCategory";
    public final static String EVENT_GROUP_RESOURCE_SPECIFICATION = "ResourceSpecification";
    public final static String EVENT_GROUP_CANCEL_PRODUCT_ORDER = "CancelProductOrder";
    public final static String EVENT_GROUP_PRODUCT_ORDER = "ProductOrder";
    public final static String EVENT_GROUP_INDIVIDUAL = "Individual";
    public final static String EVENT_GROUP_ORGANIZATION = "Organization";
    public final static String EVENT_GROUP_CUSTOMER = "Customer";
    public final static String EVENT_GROUP_AGREEMENT = "Agreement";
    public final static String EVENT_GROUP_AGREEMENT_SPECIFICATION = "AgreementSpecification";
    public final static String EVENT_GROUP_BILL_FORMAT = "BillFormat";
    public final static String EVENT_GROUP_BILLING_ACCOUNT = "BillingAccount";
    public final static String EVENT_GROUP_BILLING_CYCLE_SPECIFICATION = "BillingCycleSpecification";
    public final static String EVENT_GROUP_BILL_PRESENTATION_MEDIA = "BillPresentationMedia";
    public final static String EVENT_GROUP_FINANCIAL_ACCOUNT = "FinancialAccount";
    public final static String EVENT_GROUP_PARTY_ACCOUNT = "PartyAccount";
    public final static String EVENT_GROUP_SETTLEMENT_ACCOUNT = "SettlementAccount";

    public final static String EVENT_GROUP_USAGE = "Usage";
    public final static String EVENT_GROUP_USAGE_SPECIFICATION = "UsageSpecification";

    public static final String CREATE_EVENT_SUFFIX = "CreateEvent";
    public static final String ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX = "AttributeValueChangeEvent";
    public static final String STATE_CHANGE_EVENT_SUFFIX = "StateChangeEvent";
    public static final String DELETE_EVENT_SUFFIX = "DeleteEvent";
    public static final String CHANGE_EVENT_SUFFIX = "ChangeEvent";
    public static final String INFORMATION_REQUIRED_EVENT_SUFFIX = "InformationRequiredEvent";
    public static final String EVENT_GROUP_SERVICE = "Service";

    public static final Map<String, List<String>> ALLOWED_EVENT_TYPES = Map.ofEntries(
            entry(EVENT_GROUP_PRODUCT, List.of(CREATE_EVENT_SUFFIX, ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX,
                    STATE_CHANGE_EVENT_SUFFIX, DELETE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_AGREEMENT, List.of(CREATE_EVENT_SUFFIX, ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX,
                    STATE_CHANGE_EVENT_SUFFIX, DELETE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_AGREEMENT_SPECIFICATION, List.of(CREATE_EVENT_SUFFIX, ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX,
                    STATE_CHANGE_EVENT_SUFFIX, DELETE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_CUSTOMER, List.of(CREATE_EVENT_SUFFIX, ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX,
                    STATE_CHANGE_EVENT_SUFFIX, DELETE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_INDIVIDUAL, List.of(CREATE_EVENT_SUFFIX, ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX,
                    STATE_CHANGE_EVENT_SUFFIX, DELETE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_ORGANIZATION, List.of(CREATE_EVENT_SUFFIX, ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX,
                    STATE_CHANGE_EVENT_SUFFIX, DELETE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_CATALOG, List.of(CREATE_EVENT_SUFFIX, ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX,
                    STATE_CHANGE_EVENT_SUFFIX, DELETE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_CATEGORY, List.of(CREATE_EVENT_SUFFIX, ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX,
                    STATE_CHANGE_EVENT_SUFFIX, DELETE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_PRODUCT_OFFERING, List.of(CREATE_EVENT_SUFFIX, ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX,
                    STATE_CHANGE_EVENT_SUFFIX, DELETE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_PRODUCT_OFFERING_PRICE, List.of(CREATE_EVENT_SUFFIX, ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX,
                    STATE_CHANGE_EVENT_SUFFIX, DELETE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_PRODUCT_SPECIFICATION, List.of(CREATE_EVENT_SUFFIX, ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX,
                    STATE_CHANGE_EVENT_SUFFIX, DELETE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_CANCEL_PRODUCT_ORDER, List.of(CREATE_EVENT_SUFFIX, INFORMATION_REQUIRED_EVENT_SUFFIX,
                    STATE_CHANGE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_PRODUCT_ORDER, List.of(CREATE_EVENT_SUFFIX, ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX,
                    STATE_CHANGE_EVENT_SUFFIX, DELETE_EVENT_SUFFIX, INFORMATION_REQUIRED_EVENT_SUFFIX)),
            entry(EVENT_GROUP_RESOURCE_CANDIDATE, List.of(CREATE_EVENT_SUFFIX, CHANGE_EVENT_SUFFIX,
                    DELETE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_RESOURCE_CATALOG, List.of(CREATE_EVENT_SUFFIX, CHANGE_EVENT_SUFFIX,
                    DELETE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_RESOURCE_CATEGORY, List.of(CREATE_EVENT_SUFFIX, CHANGE_EVENT_SUFFIX,
                    DELETE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_RESOURCE_SPECIFICATION, List.of(CREATE_EVENT_SUFFIX, CHANGE_EVENT_SUFFIX,
                    DELETE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_HEAL, List.of(CREATE_EVENT_SUFFIX, STATE_CHANGE_EVENT_SUFFIX,
                    DELETE_EVENT_SUFFIX, ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_MIGRATE, List.of(CREATE_EVENT_SUFFIX, STATE_CHANGE_EVENT_SUFFIX,
                    DELETE_EVENT_SUFFIX, ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_MONITOR, List.of(CREATE_EVENT_SUFFIX, STATE_CHANGE_EVENT_SUFFIX,
                    DELETE_EVENT_SUFFIX, ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_RESOURCE_FUNCTION, List.of(CREATE_EVENT_SUFFIX, STATE_CHANGE_EVENT_SUFFIX,
                    DELETE_EVENT_SUFFIX, ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_SCALE, List.of(CREATE_EVENT_SUFFIX, STATE_CHANGE_EVENT_SUFFIX,
                    DELETE_EVENT_SUFFIX, ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_RESOURCE, List.of(CREATE_EVENT_SUFFIX, STATE_CHANGE_EVENT_SUFFIX,
                    DELETE_EVENT_SUFFIX, ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_SERVICE_CANDIDATE, List.of(CREATE_EVENT_SUFFIX, CHANGE_EVENT_SUFFIX,
                    DELETE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_SERVICE_CATALOG, List.of(CREATE_EVENT_SUFFIX, CHANGE_EVENT_SUFFIX,
                    DELETE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_SERVICE_CATEGORY, List.of(CREATE_EVENT_SUFFIX, CHANGE_EVENT_SUFFIX,
                    DELETE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_SERVICE_SPECIFICATION, List.of(CREATE_EVENT_SUFFIX, CHANGE_EVENT_SUFFIX,
                    DELETE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_USAGE, List.of(CREATE_EVENT_SUFFIX, CHANGE_EVENT_SUFFIX,
                    DELETE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_USAGE_SPECIFICATION, List.of(CREATE_EVENT_SUFFIX, CHANGE_EVENT_SUFFIX,
                    DELETE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_BILL_FORMAT, List.of(CREATE_EVENT_SUFFIX, CHANGE_EVENT_SUFFIX,
                    DELETE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_BILLING_ACCOUNT, List.of(CREATE_EVENT_SUFFIX, CHANGE_EVENT_SUFFIX,
                    DELETE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_BILLING_CYCLE_SPECIFICATION, List.of(CREATE_EVENT_SUFFIX, CHANGE_EVENT_SUFFIX,
                    DELETE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_BILL_PRESENTATION_MEDIA, List.of(CREATE_EVENT_SUFFIX, CHANGE_EVENT_SUFFIX,
                    DELETE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_FINANCIAL_ACCOUNT, List.of(CREATE_EVENT_SUFFIX, CHANGE_EVENT_SUFFIX,
                    DELETE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_PARTY_ACCOUNT, List.of(CREATE_EVENT_SUFFIX, CHANGE_EVENT_SUFFIX,
                    DELETE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_SETTLEMENT_ACCOUNT, List.of(CREATE_EVENT_SUFFIX, CHANGE_EVENT_SUFFIX,
                    DELETE_EVENT_SUFFIX)),
            entry(EVENT_GROUP_SERVICE, List.of(CREATE_EVENT_SUFFIX, CHANGE_EVENT_SUFFIX,
                    DELETE_EVENT_SUFFIX))
    );
}

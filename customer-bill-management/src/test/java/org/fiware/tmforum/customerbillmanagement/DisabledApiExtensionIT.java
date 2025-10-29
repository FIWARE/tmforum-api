package org.fiware.tmforum.customerbillmanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.customerbillmanagement.api.ext.AppliedCustomerBillingRateExtensionApiTestClient;
import org.fiware.customerbillmanagement.api.AppliedCustomerBillingRateApiTestSpec;
import org.fiware.customerbillmanagement.model.AppliedCustomerBillingRateCreateVOTestExample;
import org.fiware.customerbillmanagement.model.AppliedCustomerBillingRateUpdateVO;
import org.fiware.customerbillmanagement.model.AppliedCustomerBillingRateUpdateVOTestExample;
import org.fiware.customerbillmanagement.model.AppliedCustomerBillingRateVO;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.customerbillmanagement.configuration.ApiExtensionProperties;
import org.fiware.tmforum.customerbillmanagement.domain.AppliedCustomerBillingRate;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest(packages = {"org.fiware.tmforum.customerbillmanagement"})
public class DisabledApiExtensionIT extends AbstractApiIT {

    private final AppliedCustomerBillingRateExtensionApiTestClient appliedCustomerBillingRateExtensionApiTestClient;
    private final ApiExtensionProperties apiExtensionProperties;

    protected DisabledApiExtensionIT(EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper, GeneralProperties generalProperties, AppliedCustomerBillingRateExtensionApiTestClient appliedCustomerBillingRateExtensionApiTestClient,
                                     ApiExtensionProperties apiExtensionProperties) {
        super(entitiesApiClient, objectMapper, generalProperties);
        this.appliedCustomerBillingRateExtensionApiTestClient = appliedCustomerBillingRateExtensionApiTestClient;
        this.apiExtensionProperties = apiExtensionProperties;
    }

    @Test
    public void createAppliedCustomerBillingRate405() throws Exception {
        HttpResponse<AppliedCustomerBillingRateVO> appliedCustomerBillingRateVOHttpResponse = callAndCatch(
                () -> appliedCustomerBillingRateExtensionApiTestClient.createAppliedCustomerBillingRate(null, AppliedCustomerBillingRateCreateVOTestExample.build().atSchemaLocation(null)));
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, appliedCustomerBillingRateVOHttpResponse.getStatus(), "When the extension API is not enabled, creation should not be supported.");
    }

    @Test
    public void updateAppliedCustomerBillingRate405() throws Exception {
        AppliedCustomerBillingRateUpdateVO updateVO = AppliedCustomerBillingRateUpdateVOTestExample.build().atSchemaLocation(null);

        assertEquals(
                HttpStatus.METHOD_NOT_ALLOWED,
                callAndCatch(() -> appliedCustomerBillingRateExtensionApiTestClient.updateAppliedCustomerBillingRate(null,
                        "urn:ngsi-ld:applied-customer-billing-rate:some-id", updateVO)).getStatus(),
                "When the extension API is not enabled, updates should not be supported.)");

    }

    @Override
    protected String getEntityType() {
        return AppliedCustomerBillingRate.TYPE_APPLIED_CUSTOMER_BILLING_RATE;
    }
}

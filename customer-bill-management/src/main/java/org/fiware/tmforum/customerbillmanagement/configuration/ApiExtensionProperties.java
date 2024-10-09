package org.fiware.tmforum.customerbillmanagement.configuration;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

@ConfigurationProperties("apiExtension")
@Data
public class ApiExtensionProperties {

    /*
     * when enabled, the {@class ExtendedAppliedCustomerBillingRateApiController} is activated and
     * AppliedCustomerBillingRates can be created and updated.
     */
    private boolean enabled = false;
}

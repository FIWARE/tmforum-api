package org.fiware.tmforum.common.health;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.management.endpoint.health.HealthEndpoint;

import java.util.List;

@ConfigurationProperties(HealthEndpoint.PREFIX + ".broker")
@Requires(property = HealthEndpoint.PREFIX + ".broker.enabled", notEquals = StringUtils.FALSE)
public record NgsiLdBrokerHealthConfiguration (boolean enabled, List<Integer> allowedResponseCodes){

}

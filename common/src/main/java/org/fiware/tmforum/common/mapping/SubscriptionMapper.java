package org.fiware.tmforum.common.mapping;

import org.fiware.ngsi.model.EndpointVO;
import org.fiware.ngsi.model.KeyValuePairVO;
import org.fiware.ngsi.model.NotificationParamsVO;
import org.fiware.ngsi.model.SubscriptionVO;
import org.fiware.tmforum.common.domain.subscription.KeyValuePair;
import org.fiware.tmforum.common.domain.subscription.NotificationParams;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Qualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

@Mapper(componentModel = "jsr330", uses = IdHelper.class)
public interface SubscriptionMapper {

	SubscriptionVO map(Subscription subscription);

	Subscription map(SubscriptionVO subscriptionVO);

	@Mapping(target = "attributes", qualifiedBy = EmptySetToNull.class)
	NotificationParamsVO map(NotificationParams notificationParams);

	default SubscriptionVO.Type mapType(String type) {
		return SubscriptionVO.Type.toEnum(type);
	}

	default SubscriptionVO.Status mapStatus(String status) {
		return SubscriptionVO.Status.toEnum(status);
	}

	default EndpointVO.Accept mapAccept(String accept) {
		return EndpointVO.Accept.toEnum(accept);
	}

	@EmptySetToNull
	default Set<String> emptySetToNull(Set<String> s) {
		return s.isEmpty() ? null : s;
	}

	@Qualifier
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.CLASS)
	public @interface EmptySetToNull {
	}


	@Mapping(source = "key", target = "pairKey")
	KeyValuePair map(KeyValuePairVO keyValuePairVO);

	@Mapping(source = "pairKey", target = "key")
	KeyValuePairVO map(KeyValuePair keyValuePair);
}

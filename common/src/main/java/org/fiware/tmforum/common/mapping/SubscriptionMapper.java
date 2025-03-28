package org.fiware.tmforum.common.mapping;

import org.fiware.ngsi.model.EndpointVO;
import org.fiware.ngsi.model.KeyValuePairVO;
import org.fiware.ngsi.model.SubscriptionVO;
import org.fiware.tmforum.common.domain.subscription.KeyValuePair;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "jsr330", uses = IdHelper.class)
public interface SubscriptionMapper {

	SubscriptionVO map(Subscription subscription);

	Subscription map(SubscriptionVO subscriptionVO);

	default SubscriptionVO.Type mapType(String type) {
		return SubscriptionVO.Type.toEnum(type);
	}

	default SubscriptionVO.Status mapStatus(String status) {
		return SubscriptionVO.Status.toEnum(status);
	}

	default EndpointVO.Accept mapAccept(String accept) {
		return EndpointVO.Accept.toEnum(accept);
	}

	@Mapping(source = "key", target = "pairKey")
	KeyValuePair map(KeyValuePairVO keyValuePairVO);

	@Mapping(source = "pairKey", target = "key")
	KeyValuePairVO map(KeyValuePair keyValuePair);
}

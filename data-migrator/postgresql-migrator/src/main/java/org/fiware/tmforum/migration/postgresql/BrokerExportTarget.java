package org.fiware.tmforum.migration.postgresql;

import io.micronaut.context.ApplicationContext;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.configuration.NgsiLdBrokerProperties;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.SubscriptionMapper;
import org.fiware.tmforum.common.repository.NgsiLdEntityRepository;
import org.fiware.tmforum.common.repository.SubscriptionRepository;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Writes into the source context's broker through the application's NGSI-LD adapter and subscription repository, so
 * entities and hub subscriptions land exactly as the broker-backed services write them.
 */
class BrokerExportTarget implements ExportTarget {

	private final BrokerSource broker;
	private final NgsiLdEntityRepository repository;
	private final SubscriptionRepository subscriptionRepository;
	private final SubscriptionMapper subscriptionMapper;
	private final GeneralProperties generalProperties;
	private final NgsiLdBrokerProperties brokerProperties;

	BrokerExportTarget(ApplicationContext source, BrokerSource broker) {
		this.broker = broker;
		this.repository = source.getBean(NgsiLdEntityRepository.class);
		this.subscriptionRepository = source.getBean(SubscriptionRepository.class);
		this.subscriptionMapper = source.getBean(SubscriptionMapper.class);
		this.generalProperties = source.getBean(GeneralProperties.class);
		this.brokerProperties = source.getBean(NgsiLdBrokerProperties.class);
	}

	@Override
	public long count(String type) {
		return broker.count(type);
	}

	@Override
	public Optional<TMForumSubscription> hub(URI id) {
		return broker.entity(id).map(entity -> broker.toDomain(entity, TMForumSubscription.class));
	}

	@Override
	public void create(Object domainEntity) {
		repository.createDomainEntity(domainEntity).block();
	}

	@Override
	public void registerSubscription(Subscription subscription) {
		subscriptionRepository.createSubscription(
				subscriptionMapper.map(subscription).atContext(generalProperties.getContextUrl()),
				brokerProperties.getTenant()).block();
	}

	@Override
	public List<BrokerSubscription> subscriptions(int pageSize) {
		return broker.subscriptions(pageSize);
	}

	@Override
	public boolean deleteSubscription(URI subscriptionId) {
		return broker.deleteSubscription(subscriptionId);
	}
}

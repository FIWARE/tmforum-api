package org.fiware.tmforum.common.notification;

import org.fiware.tmforum.common.rest.CharacterizationEntity;
import org.fiware.tmforum.storage.EntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DirectEntityEventPublisherTest {

	private static final String ENTITY_ID = "urn:ngsi-ld:characterization-entity:1";
	private static final URI ENTITY_URI = URI.create(ENTITY_ID);

	private EntityRepository repository;
	private TMForumEventHandler eventHandler;
	private DirectEntityEventPublisher publisher;

	@BeforeEach
	void setUp() {
		repository = mock(EntityRepository.class);
		eventHandler = mock(TMForumEventHandler.class);
		publisher = new DirectEntityEventPublisher(repository, eventHandler);
	}

	@Test
	void createWritesThenFiresTheCreateEvent() {
		CharacterizationEntity entity = new CharacterizationEntity(ENTITY_ID);
		when(repository.createDomainEntity(entity)).thenReturn(Mono.empty());
		when(eventHandler.handleCreateEvent(entity)).thenReturn(Mono.empty());

		publisher.createWithEvent(entity).block();

		InOrder order = inOrder(repository, eventHandler);
		order.verify(repository).createDomainEntity(entity);
		order.verify(eventHandler).handleCreateEvent(entity);
	}

	@Test
	void createFiresNoEventWhenTheWriteFails() {
		CharacterizationEntity entity = new CharacterizationEntity(ENTITY_ID);
		when(repository.createDomainEntity(entity)).thenReturn(Mono.error(new IllegalStateException()));

		publisher.createWithEvent(entity).onErrorResume(e -> Mono.empty()).block();

		verifyNoInteractions(eventHandler);
	}

	@Test
	void updateReadsOldStateWritesRereadsAndFiresTheUpdateEvent() {
		CharacterizationEntity patch = new CharacterizationEntity(ENTITY_ID);
		CharacterizationEntity oldState = new CharacterizationEntity(ENTITY_ID);
		CharacterizationEntity newState = new CharacterizationEntity(ENTITY_ID);
		when(repository.get(ENTITY_URI, CharacterizationEntity.class))
				.thenReturn(Mono.just(oldState), Mono.just(newState));
		when(repository.updateDomainEntity(ENTITY_ID, patch)).thenReturn(Mono.empty());
		when(eventHandler.handleUpdateEvent(newState, oldState)).thenReturn(Mono.empty());

		CharacterizationEntity result =
				publisher.updateWithEvent(ENTITY_ID, patch, CharacterizationEntity.class).block();

		assertSame(newState, result);
		InOrder order = inOrder(repository, eventHandler);
		order.verify(repository).get(ENTITY_URI, CharacterizationEntity.class);
		order.verify(repository).updateDomainEntity(ENTITY_ID, patch);
		order.verify(repository).get(ENTITY_URI, CharacterizationEntity.class);
		order.verify(eventHandler).handleUpdateEvent(newState, oldState);
	}

	@Test
	void deleteFiresNoEventBecauseTheBrokerSubscriptionsDeliverIt() {
		when(repository.deleteDomainEntity(ENTITY_URI)).thenReturn(Mono.empty());

		publisher.deleteWithEvent(ENTITY_URI).block();

		verify(repository).deleteDomainEntity(ENTITY_URI);
		verifyNoMoreInteractions(repository);
		verifyNoInteractions(eventHandler);
	}

	@Test
	void anEventFailureDoesNotFailTheWrite() {
		CharacterizationEntity entity = new CharacterizationEntity(ENTITY_ID);
		when(repository.createDomainEntity(any())).thenReturn(Mono.empty());
		when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.error(new IllegalStateException()));

		publisher.createWithEvent(entity).block();

		verify(eventHandler).handleCreateEvent(entity);
	}
}

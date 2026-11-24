package org.fiware.tmforum.migration.postgresql;

import org.fiware.ngsi.model.EntityVO;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class EntitySampleTest {

	private static final int ENTITIES = 200;
	private static final long SHUFFLE_SEED = 7;

	@ParameterizedTest
	@CsvSource({"0, 1", "10, 1", "10, 2", "250, 1"})
	void picksTheSameIdsWhateverTheOrder(int size, long seed) {
		List<EntityVO> entities = IntStream.range(0, ENTITIES)
				.mapToObj(i -> new EntityVO().id(URI.create("urn:ngsi-ld:test:" + i)))
				.toList();
		List<EntityVO> shuffled = new ArrayList<>(entities);
		Collections.shuffle(shuffled, new Random(SHUFFLE_SEED));

		List<URI> inOrder = sample(entities, size, seed);

		assertEquals(Math.min(size, ENTITIES), inOrder.size());
		assertEquals(inOrder, sample(shuffled, size, seed));
	}

	@ParameterizedTest
	@CsvSource({"10, 1, 2"})
	void anotherSeedPicksOtherIds(int size, long seed, long otherSeed) {
		List<EntityVO> entities = IntStream.range(0, ENTITIES)
				.mapToObj(i -> new EntityVO().id(URI.create("urn:ngsi-ld:test:" + i)))
				.toList();

		assertNotEquals(sample(entities, size, seed), sample(entities, size, otherSeed));
	}

	private static List<URI> sample(List<EntityVO> entities, int size, long seed) {
		EntitySample sample = new EntitySample(size, seed);
		entities.forEach(sample::offer);
		return sample.entities().stream().map(EntityVO::getId).toList();
	}
}

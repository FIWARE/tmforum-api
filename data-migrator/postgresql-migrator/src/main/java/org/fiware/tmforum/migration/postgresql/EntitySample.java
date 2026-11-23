package org.fiware.tmforum.migration.postgresql;

import org.fiware.ngsi.model.EntityVO;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.UUID;

/**
 * A random sample of at most {@code size} entities: those with the lowest seeded hash of their id. The same seed
 * picks the same ids whatever order the broker pages in, which plain reservoir sampling would not.
 */
final class EntitySample {

	private final int size;
	private final long seed;
	// highest rank on top, so it is the one a lower-ranked offer replaces
	private final PriorityQueue<Candidate> candidates =
			new PriorityQueue<>(Comparator.comparingLong(Candidate::rank).reversed());

	EntitySample(int size, long seed) {
		this.size = size;
		this.seed = seed;
	}

	void offer(EntityVO entity) {
		if (size == 0) {
			return;
		}
		long rank = rank(seed, entity.getId());
		if (candidates.size() < size) {
			candidates.add(new Candidate(rank, entity));
		} else if (rank < candidates.peek().rank()) {
			candidates.poll();
			candidates.add(new Candidate(rank, entity));
		}
	}

	List<EntityVO> entities() {
		return candidates.stream().sorted(Comparator.comparingLong(Candidate::rank)).map(Candidate::entity).toList();
	}

	static long rank(long seed, URI id) {
		return UUID.nameUUIDFromBytes((seed + ":" + id).getBytes(StandardCharsets.UTF_8)).getMostSignificantBits();
	}

	private record Candidate(long rank, EntityVO entity) {
	}
}

package org.fiware.tmforum.common;

import io.reactivex.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.exception.NonExistentReferenceException;
import org.fiware.tmforum.common.repository.ReferencesRepository;
import org.fiware.tmforum.mapping.annotations.ReferencedEntity;

import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class ValidationService {

	private final ReferencesRepository referencesRepository;

	public <T extends ReferencedEntity> Single<Boolean> checkReferenceExists(List<T> references) {

		return Single.zip(
				references.stream()
						.map(ref -> referencesRepository.referenceExists(ref.getId().toString(), ref.getReferencedTypes())
								// we want true in case its there
								.isEmpty().map(res -> !res))
						.toList(),
				t -> Arrays.stream(t).map(Boolean.class::cast).anyMatch(b -> b));
	}

	public <T extends ReferencedEntity, R> Single<R> getCheckingSingle(List<T> refs, R defaultValue) {
		return checkReferenceExists(refs)
				.map(res -> {
					if (Boolean.FALSE.equals(res)) {
						throw new NonExistentReferenceException(String.format("Invalid reference from %s was referenced", defaultValue));
					}
					return defaultValue;
				});
	}

}

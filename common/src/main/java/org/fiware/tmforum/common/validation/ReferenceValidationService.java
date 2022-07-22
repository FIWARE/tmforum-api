package org.fiware.tmforum.common.validation;

import io.reactivex.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.exception.NonExistentReferenceException;
import org.fiware.tmforum.common.repository.ReferencesRepository;

import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;

/**
 * Service to support validation of referential integrity.
 */
@Slf4j
@Singleton
@RequiredArgsConstructor
public class ReferenceValidationService {

	private final ReferencesRepository referencesRepository;

	/**
	 * Check if all referenced entities exist. The checks will be executed asynchronously to not block each other
	 * on sequential calls to the NGSI-LD api
	 *
	 * @param references a list of referenced objects to be checked for existence
	 * @param <T>        concrete type of the referenced entity
	 * @return a single, emitting the check result
	 */
	public <T extends ReferencedEntity> Single<Boolean> checkReferenceExists(List<T> references) {

		return Single.zip(
				references.stream()
						.map(ref -> referencesRepository.referenceExists(ref.getId().toString(), ref.getReferencedTypes())
								// we want true in case its there
								.isEmpty().map(res -> !res))
						.toList(),
				t -> Arrays.stream(t).map(Boolean.class::cast).anyMatch(b -> b));
	}

	/**
	 * Convenience method for checking and returning a given object for easier integration in an async-chain
	 *
	 * @param refs         the references to check
	 * @param returnObject the object to be returned in case all references exist
	 * @param <T>          actual type of the references to check
	 * @param <R>          type of the object to be returened
	 * @exception NonExistentReferenceException will be thrown in case a reference does not exist
	 * @return a single emitting the returnObject
	 */
	public <T extends ReferencedEntity, R> Single<R> getCheckingSingleOrThrow(List<T> refs, R returnObject) throws NonExistentReferenceException {
		return checkReferenceExists(refs)
				.map(res -> {
					if (Boolean.FALSE.equals(res)) {
						throw new NonExistentReferenceException(String.format("Invalid reference from %s was referenced", returnObject));
					}
					return returnObject;
				});
	}

}

package org.fiware.tmforum.common.repository;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.client.annotation.Client;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

/**
 * Retrieval of entities as plain json, which the client of the mapping library does not offer. Needed to read the
 * stored attribute instances before an update.
 */
@Client(id = "ngsi")
public interface EntityAttributesApiClient {

	/**
	 * Retrieve an entity as plain json. Used to read the stored attribute instances without mapping them to the
	 * VOs, which fails for attributes mixing instance types, e.g. leftovers of earlier writes.
	 */
	@Get("/entities/{entityId}")
	@Consumes({"application/json"})
	Mono<HttpResponse<Map<String, Object>>> retrieveEntityAsJson(@PathVariable("entityId") URI entityId,
																 @Nullable @Header("NGSILD-Tenant") String ngsildTenant,
																 @Nullable @Header("Link") String link);
}

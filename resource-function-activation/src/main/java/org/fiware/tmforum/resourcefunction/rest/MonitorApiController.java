package org.fiware.tmforum.resourcefunction.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import org.fiware.resourcefunction.api.MonitorApi;
import org.fiware.resourcefunction.model.MonitorVO;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.resourcefunction.TMForumMapper;
import org.fiware.tmforum.resourcefunction.domain.Monitor;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.List;

//TODO: This is currently untested. It needs to be clarified how such a resource can be created.
@Controller("${general.basepath:/}")
public class MonitorApiController extends AbstractApiController<Monitor> implements MonitorApi {

	private final TMForumMapper tmForumMapper;

	public MonitorApiController(QueryParser queryParser, ReferenceValidationService validationService,
			TmForumRepository resourceCatalogRepository,
			TMForumMapper tmForumMapper, EventHandler eventHandler) {
		super(queryParser, validationService, resourceCatalogRepository, eventHandler);
		this.tmForumMapper = tmForumMapper;
	}

	@Override
	public Mono<HttpResponse<List<MonitorVO>>> listMonitor(@Nullable String fields, @Nullable Integer offset,
			@Nullable Integer limit) {
		return list(offset, limit, Monitor.TYPE_MONITOR, Monitor.class)
				.map(monitorStream -> monitorStream.map(tmForumMapper::map).toList())
				.switchIfEmpty(Mono.just(List.of()))
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<MonitorVO>> retrieveMonitor(String id, @Nullable String fields) {
		return retrieve(id, Monitor.class)
				.switchIfEmpty(Mono.error(new TmForumException("No such monitor exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}
}

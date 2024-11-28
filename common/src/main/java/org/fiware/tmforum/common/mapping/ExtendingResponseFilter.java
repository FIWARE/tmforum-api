package org.fiware.tmforum.common.mapping;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.reactivex.Flowable;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;

import java.util.Map;

@RequiredArgsConstructor
@Filter("/**")
public class ExtendingResponseFilter implements HttpServerFilter {

    private final EntityExtender entityExtender;
    private final ObjectMapper objectMapper;

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        return Flowable.fromPublisher(chain.proceed(request))
                .map(httpResponse -> {
                    if (httpResponse.getBody().isEmpty()) {
                        return httpResponse;
                    }
                    int requestId = request.hashCode();
                    if (!entityExtender.containsRequest(requestId))
                    {
                        return httpResponse;
                    } //else {
//                        Map<String, Object> theBody = objectMapper.convertValue(httpResponse.body(), new TypeReference<Map<String, Object>>() {
//                        });
//                      //  e.getExtensions().forEach((key, value) -> theBody.put(key, value));
//                        httpResponse.body(theBody);
//                        return httpResponse;
//                    }
                    return httpResponse;
                });
    }
}

package se.sadmir.apigw.filters;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Gsealy
 * @date 2019/1/7 10:19
 * 
 * Should be replaced with: https://github.com/spring-cloud/spring-cloud-gateway/blob/master/spring-cloud-gateway-core/src/main/java/org/springframework/cloud/gateway/filter/factory/rewrite/ModifyRequestBodyGatewayFilterFactory.java
 * Sadmir
 */
// @Component   // Uncomment to enable but _all_ requests will be affected!!! Used by LoggingSimpleGatewayFilterFactory
public class CacheBodyGatewayFilter implements Ordered, GlobalFilter {

  final Logger logger = LoggerFactory.getLogger(SecurityAbcGatewayFilterFactory.class);
  public static final String CACHE_REQUEST_BODY_OBJECT_KEY = "cachedRequestBodyObject";
  public static final String CORRELATION_ID_KEY = "skv-correlation-id";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    
    // exchange.getRequest().getHeaders().add(CORRELATION_ID_KEY, UUID.randomUUID().toString());

    if (exchange.getRequest().getHeaders().getContentType() == null) {
        // logger.debug("contentType: null, method: " + exchange.getRequest().getMethodValue());
        return chain.filter(exchange);
    } else {
        logger.debug("contentType " + exchange.getRequest().getHeaders().getContentType().getType());
        return DataBufferUtils.join(exchange.getRequest().getBody())
            .flatMap(dataBuffer -> {
                DataBufferUtils.retain(dataBuffer);
                Flux<DataBuffer> cachedFlux = Flux
                    .defer(() -> Flux.just(dataBuffer.slice(0, dataBuffer.readableByteCount())));
                ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
                    @Override
                    public Flux<DataBuffer> getBody() {
                        return cachedFlux;
                    }
                };
                exchange.getAttributes().put(CACHE_REQUEST_BODY_OBJECT_KEY, cachedFlux);

                return chain.filter(exchange.mutate().request(mutatedRequest).build());
          });
    }
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
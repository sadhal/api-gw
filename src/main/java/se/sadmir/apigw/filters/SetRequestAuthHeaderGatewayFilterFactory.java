package se.sadmir.apigw.filters;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;


@Component
public class SetRequestAuthHeaderGatewayFilterFactory extends AbstractGatewayFilterFactory<SetRequestAuthHeaderGatewayFilterFactory.Config> {

    final Logger logger = LoggerFactory.getLogger(SecurityAbcGatewayFilterFactory.class);

    public static final String CORRELATION_ID_KEY = "skv-correlation-id";
    public static final String BASE_MSG = "baseMessage";
    public static final String PRE_LOGGER = "preLogger";
    public static final String POST_LOGGER = "postLogger";

    public SetRequestAuthHeaderGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList(BASE_MSG, PRE_LOGGER, POST_LOGGER);
    }

    @Override
    public GatewayFilter apply(final Config config) {
        return new GatewayFilter() {

            @Override
            public Mono<Void> filter(final ServerWebExchange exchange, final GatewayFilterChain chain) {
                final ServerHttpRequest request = exchange.getRequest().mutate().headers(httpHeaders -> httpHeaders
                        .set("Authorization", getInternalToken(httpHeaders.get("Authorization")))).build();

                return chain.filter(exchange.mutate().request(request).build());
            }

            private String getInternalToken(final List<String> list) {
                final String opaqueToken = list.get(0);
                final String internalToken = opaqueToken + ".payload.signature";
                return internalToken;
            }
        };
    }

    public static class Config {
        private String baseMessage;
        private boolean preLogger;
        private boolean postLogger;

        public Config() {
        };

        public Config(final String baseMessage, final boolean preLogger, final boolean postLogger) {
            super();
            this.baseMessage = baseMessage;
            this.preLogger = preLogger;
            this.postLogger = postLogger;
        }

        public String getBaseMessage() {
            return this.baseMessage;
        }

        public boolean isPreLogger() {
            return preLogger;
        }

        public boolean isPostLogger() {
            return postLogger;
        }

        public void setBaseMessage(final String baseMessage) {
            this.baseMessage = baseMessage;
        }

        public void setPreLogger(final boolean preLogger) {
            this.preLogger = preLogger;
        }

        public void setPostLogger(final boolean postLogger) {
            this.postLogger = postLogger;
        }
    }
}
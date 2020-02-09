package se.sadmir.apigw.filters;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;


@Component
public class LoggingNoBodyGatewayFilterFactory extends AbstractGatewayFilterFactory<LoggingNoBodyGatewayFilterFactory.Config> {

    final Logger logger = LoggerFactory.getLogger(LoggingNoBodyGatewayFilterFactory.class);

    public static final String CACHE_REQUEST_BODY_OBJECT_KEY = "cachedRequestBodyObject";
    public static final String CORRELATION_ID_KEY = "skv-correlation-id";

    public LoggingNoBodyGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList(Config.field_1, Config.field_2);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter((exchange, chain) -> {
            logger.info("Enter-record in for req.path {}, req.method {}, req.params {}, corrId {}",
                            exchange.getRequest().getPath().value(), exchange.getRequest().getMethodValue(),
                            exchange.getRequest().getQueryParams(),
                            exchange.getRequest().getHeaders().get(CORRELATION_ID_KEY));

            return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                    // https://github.com/spring-cloud/spring-cloud-gateway/blob/master/spring-cloud-gateway-core/src/main/java/org/springframework/cloud/gateway/filter/factory/rewrite/ModifyResponseBodyGatewayFilterFactory.java
                    logger.info("Enter-record out, corrId {}, resp.statusCode {}, hashOnly {}",
                    exchange.getRequest().getHeaders().get(CORRELATION_ID_KEY),
                    exchange.getResponse().getStatusCode().value(),
                    config.isHashLogOnlyOut()
                    );    
                }));
        }, 1);
    }

    public static class Config {
        private String contentTypeResponse;
        private boolean hashLogOnlyOut;

        public static final String field_1 = "contentTypeResponse";
        public static final String field_2 = "hashLogOnlyOut";

        public Config() {
        };

        public Config(final String contentTypeResponse, final boolean hashLogOnlyOut) {
            super();
            this.setContentTypeResponse(contentTypeResponse);
            this.hashLogOnlyOut= hashLogOnlyOut;
        }

        public String getContentTypeResponse() {
            return contentTypeResponse;
        }

        public boolean isHashLogOnlyOut() {
            return hashLogOnlyOut;
        }

        public void setContentTypeResponse(String contentTypeResponse) {
            this.contentTypeResponse = contentTypeResponse;
        }

        public void setHashLogOnlyOut(boolean hashLogOnlyOut) {
            this.hashLogOnlyOut = hashLogOnlyOut;
        }
    }
}
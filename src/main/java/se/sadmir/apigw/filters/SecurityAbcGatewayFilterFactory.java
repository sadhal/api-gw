package se.sadmir.apigw.filters;

import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

// import reactor.core.publisher.Mono;

/**
 * https://www.baeldung.com/spring-cloud-custom-gateway-filters#1-defining-the-gatewayfilterfactory
 * https://github.com/eugenp/tutorials/tree/master/spring-cloud/spring-cloud-gateway
 * 
 */
@Component
public class SecurityAbcGatewayFilterFactory extends AbstractGatewayFilterFactory<SecurityAbcGatewayFilterFactory.Config> {

    final Logger logger = LoggerFactory.getLogger(SecurityAbcGatewayFilterFactory.class);

    public static final String CORRELATION_ID_KEY = "skv-correlation-id";
    public static final String BASE_MSG = "baseMessage";
    public static final String PRE_LOGGER = "preLogger";
    public static final String POST_LOGGER = "postLogger";

    public SecurityAbcGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList(BASE_MSG, PRE_LOGGER, POST_LOGGER);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter((exchange, chain) -> {

            String auth = exchange.getRequest().getHeaders().getOrDefault("Authorization", List.of("No authorization")).get(0);
            // logger.debug("Pre GatewayFilter security: request path: " + exchange.getRequest().getPath().value() + ", request method: " + exchange.getRequest().getMethod().toString() + ", auth: " + exchange.getRequest().getHeaders().getOrDefault("Authorization", List.of("No authorization")));
            logger.info("Authentication-record for Service {}, corrId {}, req.path {}, req.method {}, req.headers.authorization {}",
                    config.getBaseMessage(),
                    exchange.getRequest().getHeaders().get(CORRELATION_ID_KEY),
                    exchange.getRequest().getPath().value(),
                    exchange.getRequest().getMethodValue(),
                    auth);
            
            if (auth.startsWith("No authorization")) {
                // set UNAUTHORIZED 401 response and stop the processing
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            return chain.filter(exchange);

        }, 1);
    }

    public static class Config {
        private String baseMessage;
        private boolean preLogger;
        private boolean postLogger;

        public Config() {
        };

        public Config(String baseMessage, boolean preLogger, boolean postLogger) {
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

        public void setBaseMessage(String baseMessage) {
            this.baseMessage = baseMessage;
        }

        public void setPreLogger(boolean preLogger) {
            this.preLogger = preLogger;
        }

        public void setPostLogger(boolean postLogger) {
            this.postLogger = postLogger;
        }
    }
}
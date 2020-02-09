package se.sadmir.apigw.filters;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.bouncycastle.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * https://www.baeldung.com/spring-cloud-custom-gateway-filters#1-defining-the-gatewayfilterfactory
 * https://github.com/eugenp/tutorials/tree/master/spring-cloud/spring-cloud-gateway
 * 
 */
@Component
public class LoggingSimpleGatewayFilterFactory extends AbstractGatewayFilterFactory<LoggingSimpleGatewayFilterFactory.Config> {

    final Logger logger = LoggerFactory.getLogger(LoggingSimpleGatewayFilterFactory.class);

    public static final String CACHE_REQUEST_BODY_OBJECT_KEY = "cachedRequestBodyObject";
    public static final String CORRELATION_ID_KEY = "skv-correlation-id";
    public static final String BASE_MSG = "baseMessage";
    public static final String PRE_LOGGER = "preLogger";
    public static final String POST_LOGGER = "postLogger";

    public LoggingSimpleGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList(BASE_MSG, PRE_LOGGER, POST_LOGGER);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter((exchange, chain) -> {

            Flux<DataBuffer> cachedBody = exchange.getAttribute(CACHE_REQUEST_BODY_OBJECT_KEY); // or https://github.com/spring-cloud/spring-cloud-gateway/blob/master/spring-cloud-gateway-core/src/main/java/org/springframework/cloud/gateway/filter/factory/rewrite/ModifyRequestBodyGatewayFilterFactory.java
            String rawBody = "N/A";
            
            if (cachedBody != null) {
                rawBody = toRaw(cachedBody);
            }

            if (config.isPreLogger())
                logger.info("Enter-record in for Service {}, corrId {}, req.path {}, req.method {}, req.body {}, req.params {}",
                    config.getBaseMessage(),
                    exchange.getRequest().getHeaders().get(CORRELATION_ID_KEY),
                    exchange.getRequest().getPath().value(),
                    exchange.getRequest().getMethodValue(),
                    rawBody,
                    exchange.getRequest().getQueryParams());

            return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                    if (config.isPostLogger())
                        // https://github.com/spring-cloud/spring-cloud-gateway/blob/master/spring-cloud-gateway-core/src/main/java/org/springframework/cloud/gateway/filter/factory/rewrite/ModifyResponseBodyGatewayFilterFactory.java
                        logger.info("Enter-record out, corrId {}, resp.statusCode {}",
                            exchange.getRequest().getHeaders().get(CORRELATION_ID_KEY),
                            exchange.getResponse().getStatusCode().value()
                            );
                }));
        }, 1);
    }

    private static String toRaw(Flux<DataBuffer> body) {
        AtomicReference<String> rawRef = new AtomicReference<>();
        body.subscribe(buffer -> {
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            DataBufferUtils.release(buffer);
            rawRef.set(Strings.fromUTF8ByteArray(bytes));
        });
        return rawRef.get();
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
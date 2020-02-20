package se.sadmir.apigw.filters;

import java.util.Arrays;
import java.util.List;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.CachedBodyOutputMessage;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * GatewayFilter that modifies the request body.
 */
@Component
public class LoggingWithBodyGatewayFilterFactory
        extends AbstractGatewayFilterFactory<LoggingWithBodyGatewayFilterFactory.Config> {

    final Logger logger = LoggerFactory.getLogger(LoggingWithBodyGatewayFilterFactory.class);
    public static final String CORRELATION_ID_KEY = "skv-correlation-id";

    private final List<HttpMessageReader<?>> messageReaders;

    public LoggingWithBodyGatewayFilterFactory() {
        super(Config.class);
        this.messageReaders = HandlerStrategies.withDefaults().messageReaders();
    }

    @Deprecated
    public LoggingWithBodyGatewayFilterFactory(ServerCodecConfigurer codecConfigurer) {
        this();
    }

    @Override
    @SuppressWarnings("unchecked")
    public GatewayFilter apply(Config config) {
        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                ServerRequest serverRequest = ServerRequest.create(exchange, messageReaders);

                Mono<String> modifiedBody = serverRequest.bodyToMono(String.class).flatMap(o -> {
                    logger.info("Enter-record in for req.path {}, req.method {}, req.body {}, req.params {}, corrId {}",
                            exchange.getRequest().getPath().value(), exchange.getRequest().getMethodValue(), o,
                            exchange.getRequest().getQueryParams(),
                            exchange.getRequest().getHeaders().get(CORRELATION_ID_KEY));
                    return Mono.just(o);
                });

                BodyInserter<Mono<String>, ReactiveHttpOutputMessage> bodyInserter = BodyInserters
                        .fromPublisher(modifiedBody, String.class);
                HttpHeaders headers = new HttpHeaders();
                headers.putAll(exchange.getRequest().getHeaders());

                // the new content type will be computed by bodyInserter
                // and then set in the request decorator
                headers.remove(HttpHeaders.CONTENT_LENGTH);

                // if the body is changing content types, set it here, to the bodyInserter
                // will know about it
                // if (config.getContentType() != null) {
                // headers.set(HttpHeaders.CONTENT_TYPE, config.getContentType());
                // }
                CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(exchange, headers);
                return bodyInserter.insert(outputMessage, new BodyInserterContext())
                        // .log("modify_request", Level.INFO)
                        .then(Mono.defer(() -> {
                            ServerHttpRequest decorator = decorate(exchange, headers, outputMessage);
                            return chain.filter(exchange.mutate().request(decorator).build())
                                    .then(Mono.fromRunnable(() -> {
                                        // https://github.com/spring-cloud/spring-cloud-gateway/blob/master/spring-cloud-gateway-core/src/main/java/org/springframework/cloud/gateway/filter/factory/rewrite/ModifyResponseBodyGatewayFilterFactory.java
                                        logger.info("Enter-record out, corrId {}, resp.statusCode {}",
                                                exchange.getRequest().getHeaders().get(CORRELATION_ID_KEY),
                                                exchange.getResponse().getStatusCode().value());
                                    }));
                        }));

                /*
                 * return DataBufferUtils.join(exchange.getRequest().getBody())
                 * .flatMap(dataBuffer -> { DataBufferUtils.retain(dataBuffer); Flux<DataBuffer>
                 * cachedFlux = Flux .defer(() -> Flux.just(dataBuffer.slice(0,
                 * dataBuffer.readableByteCount()))); ServerHttpRequest mutatedRequest = new
                 * ServerHttpRequestDecorator( exchange.getRequest()) {
                 * 
                 * @Override public Flux<DataBuffer> getBody() { return cachedFlux; }
                 * 
                 * };
                 * 
                 * logger.
                 * info("Enter-record in for Service {}, corrId {}, req.path {}, req.method {}, req.body {}, req.params {}"
                 * , config.getBaseMessage(),
                 * exchange.getRequest().getHeaders().get(CORRELATION_ID_KEY),
                 * exchange.getRequest().getPath().value(),
                 * exchange.getRequest().getMethodValue(), toRaw(cachedFlux),
                 * exchange.getRequest().getQueryParams());
                 * 
                 * return chain.filter(exchange.mutate().request(mutatedRequest).build())
                 * .then(Mono.fromRunnable(() -> { //
                 * https://github.com/spring-cloud/spring-cloud-gateway/blob/master/spring-cloud
                 * -gateway-core/src/main/java/org/springframework/cloud/gateway/filter/factory/
                 * rewrite/ModifyResponseBodyGatewayFilterFactory.java
                 * logger.info("Enter-record out, corrId {}, resp.statusCode {}",
                 * exchange.getRequest().getHeaders().get(CORRELATION_ID_KEY),
                 * exchange.getResponse().getStatusCode().value() ); })) ; });
                 * 
                 */

            }

            @Override
            public List<String> shortcutFieldOrder() {
                return Arrays.asList(Config.field_1, Config.field_2, Config.field_3, Config.field_4);
            }
        };
    }

    /*
     * private static String toRaw(Flux<DataBuffer> body) { AtomicReference<String>
     * rawRef = new AtomicReference<>(); body.subscribe(buffer -> { byte[] bytes =
     * new byte[buffer.readableByteCount()]; buffer.read(bytes);
     * DataBufferUtils.release(buffer);
     * rawRef.set(Strings.fromUTF8ByteArray(bytes)); }); return rawRef.get(); }
     */

    ServerHttpRequestDecorator decorate(ServerWebExchange exchange, HttpHeaders headers,
            CachedBodyOutputMessage outputMessage) {
        return new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public HttpHeaders getHeaders() {
                long contentLength = headers.getContentLength();
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.putAll(super.getHeaders());
                if (contentLength > 0) {
                    httpHeaders.setContentLength(contentLength);
                } else {
                    // TODO: this causes a 'HTTP/1.1 411 Length Required' // on
                    // httpbin.org
                    httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
                }
                return httpHeaders;
            }

            @Override
            public Flux<DataBuffer> getBody() {
                return outputMessage.getBody();
            }
        };
    }

    public static class Config {
        private String contentTypePostPut;
        private String contentTypeResponse;
        private boolean hashLogOnlyIn;
        private boolean hashLogOnlyOut;

        public static final String field_1 = "contentTypePostPut";
        public static final String field_2 = "contentTypeResponse";
        public static final String field_3 = "hashLogOnlyIn";
        public static final String field_4 = "hashLogOnlyOut";

        public Config() {
        };

        public Config(final String contentTypePostPut, final String contentTypeResponse, final boolean hashLogOnlyIn, final boolean hashLogOnlyOut) {
            super();
            this.contentTypePostPut = contentTypePostPut;
            this.setContentTypeResponse(contentTypeResponse);
            this.hashLogOnlyIn = hashLogOnlyIn;
            this.hashLogOnlyOut= hashLogOnlyOut;
        }

        public String getContentTypePostPut() {
            return this.contentTypePostPut;
        }

        public String getContentTypeResponse() {
            return contentTypeResponse;
        }

        public boolean isHashLogOnlyIn() {
            return hashLogOnlyIn;
        }

        public boolean isHashLogOnlyOut() {
            return hashLogOnlyOut;
        }

        public void setContentTypePostPut(final String contentTypePostPut) {
            this.contentTypePostPut = contentTypePostPut;
        }

        public void setContentTypeResponse(String contentTypeResponse) {
            this.contentTypeResponse = contentTypeResponse;
        }

        public void setHashLogOnlyIn(final boolean hashLogOnlyIn) {
            this.hashLogOnlyIn = hashLogOnlyIn;
        }

        public void setHashLogOnlyOut(boolean hashLogOnlyOut) {
            this.hashLogOnlyOut = hashLogOnlyOut;
        }
    }
}
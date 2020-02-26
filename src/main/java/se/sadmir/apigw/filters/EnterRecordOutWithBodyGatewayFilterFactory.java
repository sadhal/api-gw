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
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * GatewayFilter that modifies the request body.
 */
@Component
public class EnterRecordOutWithBodyGatewayFilterFactory
        extends AbstractGatewayFilterFactory<EnterRecordOutWithBodyGatewayFilterFactory.Config> {

    final Logger logger = LoggerFactory.getLogger(EnterRecordOutWithBodyGatewayFilterFactory.class);
    public static final String CORRELATION_ID_KEY = "skv-correlation-id";

    private final List<HttpMessageReader<?>> messageReaders;

    @Nullable
	private final ServerCodecConfigurer codecConfigurer;

    public EnterRecordOutWithBodyGatewayFilterFactory() {
        this(null);
    }

    // @Deprecated
    public EnterRecordOutWithBodyGatewayFilterFactory(ServerCodecConfigurer codecConfigurer) {
        super(Config.class);
        this.messageReaders = HandlerStrategies.withDefaults().messageReaders();
        this.codecConfigurer = codecConfigurer;
    }

    @Override
    @SuppressWarnings("unchecked")
    public GatewayFilter apply(Config config) {
        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

                return chain.filter(exchange.mutate().response(decorate(exchange)).build());
            }

            @SuppressWarnings("unchecked")
            ServerHttpResponse decorate(ServerWebExchange exchange) {
                return new ServerHttpResponseDecorator(exchange.getResponse()) {

                    @Override
                    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {

                        HttpHeaders httpHeaders = new HttpHeaders();
                        // explicitly add it in this way instead of
                        // 'httpHeaders.setContentType(originalResponseContentType)'
                        // this will prevent exception in case of using non-standard media
                        // types like "Content-Type: image"
                        // httpHeaders.add(HttpHeaders.CONTENT_TYPE,
                        //         originalResponseContentType);

                        ClientResponse clientResponse = prepareClientResponse(body,
                                httpHeaders);

                        // TODO: flux or mono
                        Mono<String> modifiedBody = clientResponse.bodyToMono(String.class)
                                .flatMap(originalBody -> {
                                    logger.info("Enter-record out, corrId {}, resp.statusCode {}, body {}",
                                                exchange.getRequest().getHeaders().get(CORRELATION_ID_KEY),
                                                exchange.getResponse().getStatusCode().value(),
                                                originalBody
                                                );

                                    return Mono.just(originalBody);
                                })
                                ;

                        BodyInserter bodyInserter = BodyInserters.fromPublisher(modifiedBody,
                                String.class);
                        CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(
                                exchange, exchange.getResponse().getHeaders());
                        return bodyInserter.insert(outputMessage, new BodyInserterContext())
                                .then(Mono.defer(() -> {
                                    Flux<DataBuffer> messageBody = outputMessage.getBody();
                                    HttpHeaders headers = getDelegate().getHeaders();
                                    if (!headers.containsKey(HttpHeaders.TRANSFER_ENCODING)) {
                                        messageBody = messageBody.doOnNext(data -> headers
                                                .setContentLength(data.readableByteCount()));
                                    }
                                    // TODO: fail if isStreamingMediaType?
                                    return getDelegate().writeWith(messageBody);
                                }));
                    }

                    @Override
                    public Mono<Void> writeAndFlushWith(
                            Publisher<? extends Publisher<? extends DataBuffer>> body) {
                        return writeWith(Flux.from(body).flatMapSequential(p -> p));
                    }

                    private ClientResponse prepareClientResponse(
                            Publisher<? extends DataBuffer> body, HttpHeaders httpHeaders) {
                        ClientResponse.Builder builder;
                        if (codecConfigurer != null) {
                            builder = ClientResponse.create(
                                    exchange.getResponse().getStatusCode(),
                                    codecConfigurer.getReaders());
                        }
                        else {
                            builder = ClientResponse
                                    .create(exchange.getResponse().getStatusCode());
                        }
                        return builder.headers(headers -> headers.putAll(httpHeaders))
                                .body(Flux.from(body)).build();
                    }

                };
            }

            @Override
            public List<String> shortcutFieldOrder() {
                return Arrays.asList(Config.field_1, Config.field_2, Config.field_3, Config.field_4);
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
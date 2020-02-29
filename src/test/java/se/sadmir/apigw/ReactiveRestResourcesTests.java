package se.sadmir.apigw;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;


// @RunWith(SpringRunner.class)
@WebFluxTest(DemoApplication.class)
public class ReactiveRestResourcesTests {
    
    @Autowired
    WebTestClient webTestClient;

    @Test 
    public void testGetEmpoyeeStream() {
        
        webTestClient.get()
            .uri("/employee-stream")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus().isOk()
            // .expectBody()
            // .toString()
            // .expectBody(Flux.class)
            // .value(employee1 -> employee1.getId(), equals(100))
            ;
    }
}
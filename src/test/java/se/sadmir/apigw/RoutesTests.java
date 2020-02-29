package se.sadmir.apigw;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class RoutesTests {
    
    @Autowired
    WebTestClient webTestClient;


    @Test
    public void testRoute1_no_logging_route1() throws Exception {

        webTestClient
            .get().uri("/get")
            // .header("User-group", "ablue")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            // .jsonPath("$.headers.Hello").isEqualTo("World")
            .jsonPath("$.headers.Host").isEqualTo("httpbin.org")
            .jsonPath("$.url").exists()
            // .isEqualTo("https://localhost:38845/get")
            ;
    }
}
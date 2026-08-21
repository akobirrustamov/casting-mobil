package com.example.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Kontekst ko'tarilishini tekshiradi.
 *
 * Avval bu test yiqilardi: tirik PostgreSQL talab qilinardi. Endi "test" profili
 * xotiradagi H2 ni beradi (src/test/resources/application-test.properties).
 */
@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }

}

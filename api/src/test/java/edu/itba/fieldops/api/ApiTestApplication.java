package edu.itba.fieldops.api;

import edu.itba.fieldops.api.config.JacksonConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(JacksonConfig.class)
class ApiTestApplication {
}

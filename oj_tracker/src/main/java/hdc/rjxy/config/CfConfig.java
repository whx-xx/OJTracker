package hdc.rjxy.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import hdc.rjxy.cf.CodeforcesClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CfConfig {

    @Bean
    public CodeforcesClient codeforcesClient(ObjectMapper objectMapper) {
        return new CodeforcesClient(objectMapper);
    }
}

package in.sellzon.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient identityRestClient(@Value("${sellzon.services.identity.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    public RestClient catalogRestClient(@Value("${sellzon.services.catalog.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    public RestClient cartRestClient(@Value("${sellzon.services.cart.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}
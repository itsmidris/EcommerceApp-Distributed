package in.sellzon.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public RestClient identityRestClient(
            RestClient.Builder builder,
            @Value("${sellzon.services.identity.base-url}") String baseUrl) {

        return builder
                .baseUrl(baseUrl)
                .build();
    }

    @Bean
    public RestClient catalogRestClient(
            RestClient.Builder builder,
            @Value("${sellzon.services.catalog.base-url}") String baseUrl) {

        return builder
                .baseUrl(baseUrl)
                .build();
    }

    @Bean
    public RestClient cartRestClient(
            RestClient.Builder builder,
            @Value("${sellzon.services.cart.base-url}") String baseUrl) {

        return builder
                .baseUrl(baseUrl)
                .build();
    }
}
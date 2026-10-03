package in.sellzon.identity.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
//import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Duration;
import java.util.UUID;

@Configuration
public class AuthorizationServerConfig {

    @Value("${CATALOG_SERVICE_CLIENT_SECRET:catalog-service-dev-secret}")
    private String catalogServiceClientSecret;

    @Value("${INVENTORY_SERVICE_CLIENT_SECRET:inventory-service-dev-secret}")
    private String inventoryServiceClientSecret;

    @Value("${ORDER_SERVICE_CLIENT_SECRET:order-service-dev-secret}")
    private String orderServiceClientSecret;

    @Value("${PAYMENT_SERVICE_CLIENT_SECRET:payment-service-dev-secret}")
    private String paymentServiceClientSecret;

    /**
     * OAuth2 Authorization Server endpoints.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .oauth2AuthorizationServer(authorizationServer -> {
                    http.securityMatcher(authorizationServer.getEndpointsMatcher());
                    authorizationServer.oidc(Customizer.withDefaults());
                })
                .authorizeHttpRequests(authorize ->
                        authorize.anyRequest().authenticated()
                );

        return http.build();
    }


    /**
     * OAuth2 clients used for service-to-service communication.
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(PasswordEncoder passwordEncoder) {

        RegisteredClient catalogService =
                RegisteredClient
                        .withId(UUID.randomUUID().toString())
                        .clientId("catalog-service")
                        .clientSecret(
                                passwordEncoder.encode(catalogServiceClientSecret))
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                        .scope("identity.internal")
                        .scope("catalog.internal")
                        .clientSettings(
                                ClientSettings
                                        .builder()
                                        .requireAuthorizationConsent(false)
                                        .build())
                        .tokenSettings(
                                TokenSettings
                                        .builder()
                                        .accessTokenTimeToLive(Duration.ofMinutes(15))
                                        .build())
                        .build();


        RegisteredClient inventoryService =
                RegisteredClient
                        .withId(UUID.randomUUID().toString())
                        .clientId("inventory-service")
                        .clientSecret(
                                passwordEncoder.encode(inventoryServiceClientSecret))
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                        .scope("catalog.internal")
                        .scope("inventory.internal")
                        .clientSettings(
                                ClientSettings
                                        .builder()
                                        .requireAuthorizationConsent(false)
                                        .build())
                        .tokenSettings(
                                TokenSettings
                                        .builder()
                                        .accessTokenTimeToLive(Duration.ofMinutes(15))
                                        .build())
                        .build();


        RegisteredClient orderService =
                RegisteredClient
                        .withId(UUID.randomUUID().toString())
                        .clientId("order-service")
                        .clientSecret(
                                passwordEncoder.encode(orderServiceClientSecret))
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                        .scope("catalog.internal")
                        .scope("inventory.internal")
                        .scope("payment.internal")
                        .scope("order.internal")
                        .clientSettings(
                                ClientSettings
                                        .builder()
                                        .requireAuthorizationConsent(false)
                                        .build())
                        .tokenSettings(
                                TokenSettings
                                        .builder()
                                        .accessTokenTimeToLive(Duration.ofMinutes(15))
                                        .build())
                        .build();


        RegisteredClient paymentService =
                RegisteredClient
                        .withId(UUID.randomUUID().toString())
                        .clientId("payment-service")
                        .clientSecret(
                                passwordEncoder.encode(paymentServiceClientSecret))
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                        .scope("payment.internal")
                        .clientSettings(
                                ClientSettings
                                        .builder()
                                        .requireAuthorizationConsent(false)
                                        .build())
                        .tokenSettings(
                                TokenSettings
                                        .builder()
                                        .accessTokenTimeToLive(Duration.ofMinutes(15))
                                        .build())
                        .build();


        return new InMemoryRegisteredClientRepository(
                catalogService,
                inventoryService,
                orderService,
                paymentService
        );
    }


    /**
     * Use Sellzon's existing RSA key pair.
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource(RSAKey rsaKey) {

        JWKSet jwkSet = new JWKSet(rsaKey);

        return new ImmutableJWKSet<>(jwkSet);
    }


    /**
     * JWT decoder used by Authorization Server.
     */
//    @Bean
//    public JwtDecoder authorizationServerJwtDecoder(JWKSource<SecurityContext> jwkSource) {
//
//        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
//    }


    /**
     * OAuth2 issuer.
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {

        return AuthorizationServerSettings
                .builder()
                .issuer("http://localhost:8081")
                .build();
    }
}

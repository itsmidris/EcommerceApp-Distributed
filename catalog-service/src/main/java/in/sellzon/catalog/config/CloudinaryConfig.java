package in.sellzon.catalog.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "sellzon.storage.provider", havingValue = "cloudinary")
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary(
            @Value("${sellzon.storage.cloudinary.cloud-name}") String cloudName,
            @Value("${sellzon.storage.cloudinary.api-key}") String apiKey,
            @Value("${sellzon.storage.cloudinary.api-secret}") String apiSecret) {

        if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
            throw new IllegalStateException(
                    "Cloudinary configuration is required when sellzon.storage.provider=cloudinary"
            );
        }

        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }
}

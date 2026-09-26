package in.sellzon.catalog.product.util;

import in.sellzon.catalog.product.repository.ProductRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class MarketplaceProductIdGenerator {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int RANDOM_LENGTH = 10;
    private final SecureRandom random = new SecureRandom();

    private final ProductRepository productRepository;

    public MarketplaceProductIdGenerator(
            ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public String generate() {

        String marketplaceProductId;

        do {
            StringBuilder value = new StringBuilder(RANDOM_LENGTH);
            for (int i = 0; i < RANDOM_LENGTH; i++) {
                int index = random.nextInt(CHARACTERS.length());
                value.append(CHARACTERS.charAt(index));
            }
            marketplaceProductId = value.toString();
        } while (productRepository.existsByMarketplaceProductId(marketplaceProductId));

        return marketplaceProductId;
    }
}

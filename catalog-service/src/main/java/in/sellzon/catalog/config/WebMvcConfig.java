package in.sellzon.catalog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${sellzon.storage.image-dir:uploads/products}")
    private String imageDirectory;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        String imageLocation =
                Paths.get(imageDirectory)
                        .toAbsolutePath()
                        .normalize()
                        .toUri()
                        .toString();

        registry
                .addResourceHandler("/uploads/products/**")
                .addResourceLocations(imageLocation);
    }
}
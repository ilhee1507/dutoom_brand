package com.dutoom.shop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.time.Duration;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final ShopProperties props;

    public WebConfig(ShopProperties props) { this.props = props; }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(Path.of(props.uploadDir()).toAbsolutePath().toUri().toString())
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)));
    }
}

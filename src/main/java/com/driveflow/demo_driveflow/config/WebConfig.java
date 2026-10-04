package com.driveflow.demo_driveflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${upload.path:uploads}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        File dir = uploadDir.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir.toString().replace("\\", "/") + "/");
    }
}

package com.driveflow.demo_driveflow.vehicle;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of(".jpg", ".jpeg", ".png", ".webp", ".gif");

    @Value("${upload.path:uploads}")
    private String uploadBaseDir;

    public String storeVehicleImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFilename = file.getOriginalFilename();
        String extension = ".jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            if (ALLOWED_EXTENSIONS.contains(ext)) {
                extension = ext;
            }
        }

        // Validate MIME type
        String contentType = file.getContentType();
        if (contentType != null && !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Invalid file format. Only image files (JPG, PNG, WebP) are accepted.");
        }

        String fileName = "vehicle_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

        Path targetDir = Paths.get(uploadBaseDir, "vehicles").toAbsolutePath().normalize();
        File dirFile = targetDir.toFile();
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }

        Path targetPath = targetDir.resolve(fileName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // Also copy to src/main/resources/static/uploads/vehicles and target/classes/static/uploads/vehicles if available
        try {
            Path staticSrcDir = Paths.get("src", "main", "resources", "static", "uploads", "vehicles").toAbsolutePath().normalize();
            if (staticSrcDir.toFile().exists() || staticSrcDir.toFile().mkdirs()) {
                Files.copy(file.getInputStream(), staticSrcDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ignored) {}

        try {
            Path targetClassesDir = Paths.get("target", "classes", "static", "uploads", "vehicles").toAbsolutePath().normalize();
            if (targetClassesDir.toFile().exists() || targetClassesDir.toFile().mkdirs()) {
                Files.copy(file.getInputStream(), targetClassesDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ignored) {}

        return "/uploads/vehicles/" + fileName;
    }
}

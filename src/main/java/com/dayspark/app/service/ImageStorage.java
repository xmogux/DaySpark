package com.dayspark.app.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Stores completion photos under {@code app.upload-dir} and serves them as {@code /uploads/<file>}. */
@Component
public class ImageStorage {

    static final String WEB_PREFIX = "/uploads/";
    private static final List<String> ALLOWED = List.of(".jpg", ".jpeg", ".png", ".webp", ".gif");

    private final Path root;

    public ImageStorage(@Value("${app.upload-dir:uploads}") String uploadDir) throws IOException {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    /** Saves the image under a random name and returns its web path, or {@code null} when no file was sent. */
    public String save(MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) return null;
        String original = image.getOriginalFilename() == null ? "image" : image.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String extension = dot >= 0 ? original.substring(dot).toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED.contains(extension)) {
            throw new IllegalArgumentException("JPG, PNG, WEBP, GIF 이미지만 업로드할 수 있습니다.");
        }
        Path target = resolve(UUID.randomUUID() + extension);
        try (InputStream in = image.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return WEB_PREFIX + target.getFileName();
    }

    /** Deletes a stored image. A missing file is ignored: the database record matters more than the file. */
    public void deleteQuietly(String webPath) {
        if (webPath == null || !webPath.startsWith(WEB_PREFIX)) return;
        try {
            Files.deleteIfExists(resolve(webPath.substring(WEB_PREFIX.length())));
        } catch (IOException | IllegalArgumentException ignored) {
            // Already removed or outside the upload folder: nothing to delete.
        }
    }

    private Path resolve(String filename) {
        Path target = root.resolve(filename).normalize();
        if (!target.startsWith(root)) throw new IllegalArgumentException("올바르지 않은 업로드 경로입니다.");
        return target;
    }
}

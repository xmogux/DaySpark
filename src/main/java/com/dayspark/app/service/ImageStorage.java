package com.dayspark.app.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
            byte[] header = in.readNBytes(12);
            if (!matchesImageSignature(header, extension)) {
                throw new IllegalArgumentException("사진의 실제 파일 형식이 확장자와 일치하지 않습니다.");
            }
            try (OutputStream out = Files.newOutputStream(target)) {
                out.write(header);
                in.transferTo(out);
            }
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(target);
            throw e;
        }
        return WEB_PREFIX + target.getFileName();
    }

    /** Remove a newly uploaded file if its database transaction rolls back. */
    public void deleteOnRollback(String webPath) {
        if (webPath == null) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) deleteQuietly(webPath);
                }
            });
        }
    }

    /** Delete a recorded file only after the corresponding database commit. */
    public void deleteAfterCommit(String webPath) {
        if (webPath == null) return;
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteQuietly(webPath);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                deleteQuietly(webPath);
            }
        });
    }

    private static boolean matchesImageSignature(byte[] header, String extension) {
        return switch (extension) {
            case ".png" -> startsWith(header, new byte[] {
                (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
            });
            case ".jpg", ".jpeg" -> startsWith(header, new byte[] {
                (byte) 0xff, (byte) 0xd8, (byte) 0xff
            });
            case ".gif" -> startsWith(header, "GIF87a".getBytes(StandardCharsets.US_ASCII))
                || startsWith(header, "GIF89a".getBytes(StandardCharsets.US_ASCII));
            case ".webp" -> header.length >= 12
                && startsWith(header, "RIFF".getBytes(StandardCharsets.US_ASCII))
                && Arrays.equals(Arrays.copyOfRange(header, 8, 12), "WEBP".getBytes(StandardCharsets.US_ASCII));
            default -> false;
        };
    }

    private static boolean startsWith(byte[] value, byte[] prefix) {
        return value.length >= prefix.length
            && Arrays.equals(Arrays.copyOfRange(value, 0, prefix.length), prefix);
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

package com.saasplatform.ai.multimodal;

import com.saasplatform.exception.BadRequestException;
import com.saasplatform.exception.PayloadTooLargeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Component
public class MultimodalFileValidator {

    @Value("${app.ai.multimodal.max-file-size-bytes:10485760}") // 10MB default
    private long maxFileSizeBytes;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "webp",
            "pdf",
            "txt", "md", "json", "csv",
            "js", "jsx", "ts", "tsx", "py", "java", "sql"
    );

    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            "exe", "bat", "cmd", "sh", "bin", "dll", "jar", "elf", "com", "vbs", "msi", "ps1", "apk", "scr", "pif"
    );

    public MultimodalAttachment validateAndConvert(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be null or empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BadRequestException("File must have a valid filename");
        }

        // Sanitize path traversal
        String cleanName = originalFilename.replaceAll("[\\\\/]", "").trim();
        if (cleanName.isEmpty() || cleanName.startsWith(".")) {
            throw new BadRequestException("Invalid filename format");
        }

        // Validate File Size
        if (file.getSize() > maxFileSizeBytes) {
            log.warn("File '{}' exceeds maximum allowed size: {} bytes > {} bytes", cleanName, file.getSize(), maxFileSizeBytes);
            throw new PayloadTooLargeException(String.format(
                    "File '%s' exceeds the maximum allowed size of %d MB",
                    cleanName, maxFileSizeBytes / (1024 * 1024)
            ));
        }

        // Validate Extension
        String extension = "";
        int dotIndex = cleanName.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < cleanName.length() - 1) {
            extension = cleanName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        }

        if (BLOCKED_EXTENSIONS.contains(extension)) {
            log.warn("Security Alert: Executable file upload rejected: {}", cleanName);
            throw new BadRequestException("Executable and binary script files are strictly prohibited");
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            log.warn("Unsupported file extension rejected: {}", extension);
            throw new BadRequestException("Unsupported file type '." + extension + "'. Allowed types: PNG, JPG, WEBP, PDF, TXT, MD, JSON, CSV, JS, PY, JAVA, SQL");
        }

        try {
            byte[] bytes = file.getBytes();
            String contentType = file.getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = resolveMimeTypeFromExtension(extension);
            }

            boolean isImage = extension.equals("png") || extension.equals("jpg") || extension.equals("jpeg") || extension.equals("webp");
            boolean isPdf = extension.equals("pdf");
            boolean isText = !isImage && !isPdf;

            String base64Data = null;
            String textContent = null;

            if (isImage || isPdf) {
                base64Data = Base64.getEncoder().encodeToString(bytes);
            } else {
                textContent = new String(bytes, StandardCharsets.UTF_8);
            }

            return MultimodalAttachment.builder()
                    .fileName(cleanName)
                    .contentType(contentType)
                    .fileSizeBytes(file.getSize())
                    .bytes(bytes)
                    .base64Data(base64Data)
                    .textContent(textContent)
                    .image(isImage)
                    .pdf(isPdf)
                    .textDocument(isText)
                    .build();

        } catch (IOException e) {
            log.error("Failed to read uploaded file bytes for {}: {}", cleanName, e.getMessage(), e);
            throw new BadRequestException("Failed to process uploaded file: " + cleanName);
        }
    }

    private String resolveMimeTypeFromExtension(String ext) {
        return switch (ext) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            case "json" -> "application/json";
            case "csv" -> "text/csv";
            case "md" -> "text/markdown";
            case "js", "jsx" -> "application/javascript";
            case "ts", "tsx" -> "text/typescript";
            case "py" -> "text/x-python";
            case "java" -> "text/x-java-source";
            case "sql" -> "application/sql";
            default -> "text/plain";
        };
    }
}

package com.doc.impl;

import com.doc.dto.document.DocumentExpiryResponseDto;
import com.doc.em.DocumentExpiryStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class DocumentExpiryService {

    private final DocumentTextExtractorService documentTextExtractorService;
    private final ExpiryDateExtractor expiryDateExtractor;

    public DocumentExpiryService(
            DocumentTextExtractorService documentTextExtractorService,
            ExpiryDateExtractor expiryDateExtractor
    ) {
        this.documentTextExtractorService = documentTextExtractorService;
        this.expiryDateExtractor = expiryDateExtractor;
    }

    public DocumentExpiryResponseDto checkExpiry(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is required");
        }

        try {
            String extractedText = documentTextExtractorService.extractText(file);

            return processExtractedText(
                    file.getOriginalFilename(),
                    extractedText
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to check document expiry: " + e.getMessage(), e);
        }
    }

    public DocumentExpiryResponseDto checkExpiryFromUrl(String fileUrl) {

        if (fileUrl == null || fileUrl.isBlank()) {
            throw new RuntimeException("File URL is required");
        }

        try {
            DownloadedFile downloadedFile = downloadFileFromUrl(fileUrl);

            String extractedText = documentTextExtractorService.extractText(
                    downloadedFile.fileName(),
                    downloadedFile.contentType(),
                    downloadedFile.fileBytes()
            );

            return processExtractedText(
                    downloadedFile.fileName(),
                    extractedText
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to check document expiry from URL: " + e.getMessage(), e);
        }
    }

    private DocumentExpiryResponseDto processExtractedText(
            String fileName,
            String extractedText
    ) {

        if (extractedText == null || extractedText.isBlank()) {
            return new DocumentExpiryResponseDto(
                    fileName,
                    null,
                    DocumentExpiryStatus.UNKNOWN,
                    null,
                    true,
                    "No readable text found in document. Manual review required.",
                    LocalDateTime.now()
            );
        }

        ExpiryDateExtractor.ExpiryDateMatch expiryMatch =
                expiryDateExtractor.extractExpiryDate(extractedText);

        if (expiryMatch.expiryDate() == null) {
            return new DocumentExpiryResponseDto(
                    fileName,
                    null,
                    DocumentExpiryStatus.UNKNOWN,
                    null,
                    true,
                    "Expiry date not found. Manual review required.",
                    LocalDateTime.now()
            );
        }

        LocalDate today = LocalDate.now();
        LocalDate expiryDate = expiryMatch.expiryDate();

        DocumentExpiryStatus status;

        if (expiryDate.isBefore(today)) {
            status = DocumentExpiryStatus.EXPIRED;
        } else if (!expiryDate.isAfter(today.plusDays(30))) {
            status = DocumentExpiryStatus.EXPIRING_SOON;
        } else {
            status = DocumentExpiryStatus.VALID;
        }

        return new DocumentExpiryResponseDto(
                fileName,
                expiryDate,
                status,
                expiryMatch.matchedText(),
                false,
                "Expiry date extracted successfully.",
                LocalDateTime.now()
        );
    }

    private DownloadedFile downloadFileFromUrl(String fileUrl) throws Exception {

        URI uri = URI.create(fileUrl);

        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .header("User-Agent", "Mozilla/5.0")
                .build();

        HttpResponse<byte[]> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofByteArray()
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException(
                    "Unable to download file. HTTP Status: " + response.statusCode()
            );
        }

        byte[] fileBytes = response.body();

        if (fileBytes == null || fileBytes.length == 0) {
            throw new RuntimeException("Downloaded file is empty");
        }

        String contentType = response.headers()
                .firstValue("Content-Type")
                .orElse(null);

        String fileName = extractFileNameFromUrl(fileUrl);

        return new DownloadedFile(
                fileName,
                contentType,
                fileBytes
        );
    }

    private String extractFileNameFromUrl(String fileUrl) {

        try {
            URI uri = URI.create(fileUrl);
            String path = uri.getPath();

            if (path == null || path.isBlank()) {
                return "downloaded-document";
            }

            String fileName = path.substring(path.lastIndexOf("/") + 1);

            if (fileName == null || fileName.isBlank()) {
                return "downloaded-document";
            }

            return URLDecoder.decode(fileName, StandardCharsets.UTF_8);

        } catch (Exception e) {
            return "downloaded-document";
        }
    }

    private record DownloadedFile(
            String fileName,
            String contentType,
            byte[] fileBytes
    ) {
    }
}
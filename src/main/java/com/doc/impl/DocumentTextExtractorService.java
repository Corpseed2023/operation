package com.doc.impl;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

@Service
public class DocumentTextExtractorService {

    private final OcrService ocrService;

    public DocumentTextExtractorService(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    /**
     * Used when user uploads actual file using multipart/form-data.
     */
    public String extractText(MultipartFile file) throws Exception {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is required");
        }

        return extractText(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getBytes()
        );
    }

    /**
     * Used when file is downloaded from S3 URL or any file URL.
     */
    public String extractText(
            String fileName,
            String contentType,
            byte[] fileBytes
    ) throws Exception {

        if (fileBytes == null || fileBytes.length == 0) {
            throw new RuntimeException("File bytes are empty");
        }

        String lowerFileName = fileName == null ? "" : fileName.toLowerCase();
        String safeContentType = contentType == null ? "" : contentType.toLowerCase();

        if (lowerFileName.endsWith(".pdf") || "application/pdf".equalsIgnoreCase(safeContentType)) {
            return extractTextFromPdf(fileBytes);
        }

        if (lowerFileName.endsWith(".docx")
                || safeContentType.contains("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            return extractTextFromDocx(fileBytes);
        }

        if (lowerFileName.endsWith(".doc")
                || "application/msword".equalsIgnoreCase(safeContentType)) {
            return extractTextFromDoc(fileBytes);
        }

        if (isImageFile(lowerFileName, safeContentType)) {
            return ocrService.extractTextFromImage(fileBytes);
        }

        throw new RuntimeException(
                "Unsupported file type. FileName: " + fileName + ", Content-Type: " + contentType
        );
    }

    private boolean isImageFile(String lowerFileName, String contentType) {
        return lowerFileName.endsWith(".jpg")
                || lowerFileName.endsWith(".jpeg")
                || lowerFileName.endsWith(".png")
                || lowerFileName.endsWith(".webp")
                || lowerFileName.endsWith(".bmp")
                || lowerFileName.endsWith(".tiff")
                || lowerFileName.endsWith(".tif")
                || contentType.startsWith("image/");
    }

    private String extractTextFromPdf(byte[] fileBytes) throws Exception {

        try (PDDocument document = Loader.loadPDF(fileBytes)) {

            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            String text = pdfTextStripper.getText(document);

            /*
             * Normal PDF case:
             * If PDF has selectable text, return it directly.
             */
            if (text != null && text.trim().length() > 50) {
                return text;
            }

            /*
             * Scanned PDF case:
             * Convert every PDF page into image and run OCR.
             */
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            StringBuilder scannedPdfText = new StringBuilder();

            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = pdfRenderer.renderImageWithDPI(page, 250);
                String pageText = ocrService.extractTextFromBufferedImage(image);
                scannedPdfText.append(pageText).append("\n");
            }

            return scannedPdfText.toString();
        }
    }

    private String extractTextFromDocx(byte[] fileBytes) throws Exception {

        try (
                ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);
                XWPFDocument document = new XWPFDocument(inputStream);
                XWPFWordExtractor extractor = new XWPFWordExtractor(document)
        ) {
            return extractor.getText();
        }
    }

    private String extractTextFromDoc(byte[] fileBytes) throws Exception {

        try (
                ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);
                HWPFDocument document = new HWPFDocument(inputStream);
                WordExtractor extractor = new WordExtractor(document)
        ) {
            return extractor.getText();
        }
    }
}
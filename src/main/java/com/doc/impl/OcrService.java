package com.doc.impl;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

@Service
public class OcrService {

    @Value("${tesseract.datapath:}")
    private String tessDataPath;

    @Value("${tesseract.language:eng}")
    private String language;

    public String extractTextFromImage(byte[] imageBytes) throws Exception {

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes)) {

            BufferedImage image = ImageIO.read(inputStream);

            if (image == null) {
                throw new RuntimeException("Invalid image file. Unable to read image.");
            }

            return extractTextFromBufferedImage(image);
        }
    }

    public String extractTextFromBufferedImage(BufferedImage image) {

        try {
            Tesseract tesseract = new Tesseract();

            /*
             * Set this only if needed.
             *
             * Windows example:
             * C:/Program Files/Tesseract-OCR/tessdata
             *
             * Linux example:
             * /usr/share/tesseract-ocr/4.00/tessdata
             */
            if (tessDataPath != null && !tessDataPath.isBlank()) {
                tesseract.setDatapath(tessDataPath);
            }

            tesseract.setLanguage(language);

            return tesseract.doOCR(image);

        } catch (TesseractException e) {
            throw new RuntimeException("OCR failed: " + e.getMessage(), e);
        }
    }
}
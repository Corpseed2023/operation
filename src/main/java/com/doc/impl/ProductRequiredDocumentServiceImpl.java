    package com.doc.impl;

    import com.amazonaws.services.s3.AmazonS3;
    import com.amazonaws.services.s3.model.AmazonS3Exception;
    import com.amazonaws.services.s3.model.S3Object;
    import com.doc.dto.document.ProductRequiredDocumentRequestDto;
    import com.doc.dto.document.ProductRequiredDocumentResponseDto;
    import com.doc.em.DocumentExpiryType;
    import com.doc.entity.document.ProductRequiredDocuments;
    import com.doc.entity.user.User;
    import com.doc.exception.ResourceNotFoundException;
    import com.doc.exception.ValidationException;
    import com.doc.repository.UserRepository;
    import com.doc.repository.documentRepo.ProductRequiredDocumentRepository;
    import com.doc.service.ProductRequiredDocumentService;
    import org.apache.poi.ss.usermodel.WorkbookFactory;
    import org.apache.poi.ss.usermodel.Workbook;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.data.domain.*;
    import org.springframework.http.HttpStatus;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;
    import org.springframework.util.StringUtils;
    import org.springframework.web.server.ResponseStatusException;
    import org.apache.poi.ss.usermodel.Cell;
    import org.apache.poi.ss.usermodel.Row;
    import org.apache.poi.ss.usermodel.Sheet;
    import org.apache.commons.csv.CSVFormat;
    import org.apache.commons.csv.CSVParser;
    import org.apache.commons.csv.CSVRecord;
    import java.io.InputStreamReader;

    import java.nio.charset.StandardCharsets;
    import java.time.LocalDate;
    import java.util.*;

    import java.io.IOException;
    import java.io.InputStream;

    @Service
    @Transactional
    public class ProductRequiredDocumentServiceImpl implements ProductRequiredDocumentService {

        private static final Logger log = LoggerFactory.getLogger(ProductRequiredDocumentServiceImpl.class);

        private final ProductRequiredDocumentRepository productRequiredDocumentRepository;
        private final AmazonS3 amazonS3;

        @Value("${aws.s3.bucket-name}")
        private String bucketName;

        @Value("${aws_path}")
        private String s3BaseUrl;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        public ProductRequiredDocumentServiceImpl(
                ProductRequiredDocumentRepository productRequiredDocumentRepository,
                AmazonS3 amazonS3) {
            this.productRequiredDocumentRepository = productRequiredDocumentRepository;
            this.amazonS3 = amazonS3;
        }

        @Override
        public ProductRequiredDocumentResponseDto create(ProductRequiredDocumentRequestDto dto) {
            validateUniqueConstraint(dto, null);

            ProductRequiredDocuments entity = new ProductRequiredDocuments();
            mapDtoToEntity(dto, entity);
            entity.setActive(true);
            entity.setDeleted(false);
            entity.setCreatedBy(dto.getCreatedBy());
            entity.setUpdatedBy(dto.getUpdatedBy());
            entity.setCreatedDate(new Date());
            entity.setUpdatedDate(new Date());

            entity = productRequiredDocumentRepository.save(entity);
            return mapToResponseDto(entity);
        }

        @Override
        public ProductRequiredDocumentResponseDto update(Long id, ProductRequiredDocumentRequestDto dto) {
            ProductRequiredDocuments entity = findActiveById(id);

            validateUniqueConstraint(dto, id);

            mapDtoToEntity(dto, entity);
            entity.setUpdatedBy(dto.getUpdatedBy());

            entity = productRequiredDocumentRepository.save(entity);
            return mapToResponseDto(entity);
        }


        @Override
        @Transactional
        public List<ProductRequiredDocumentResponseDto> importFromS3(String s3Url, Long createdBy) {

            if (!StringUtils.hasText(s3Url) || !s3Url.startsWith(s3BaseUrl)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid S3 URL – must start with: " + s3BaseUrl);
            }

            User user = userRepository.findById(createdBy)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("User not found", "USER_NOT_FOUND"));

            String key = s3Url.substring(s3BaseUrl.length());
            if (key.startsWith("/")) key = key.substring(1);

            String lowerKey = key.toLowerCase();
            boolean isExcel = lowerKey.endsWith(".xlsx") || lowerKey.endsWith(".xls");

            List<ProductRequiredDocumentResponseDto> successful = new ArrayList<>();
            List<ProductRequiredDocuments> batch = new ArrayList<>(200);

            try (S3Object s3Object = amazonS3.getObject(bucketName, key);
                 InputStream is = s3Object.getObjectContent()) {

                if (isExcel) {
                    processExcel(is, user, batch, successful);
                } else {
                    processCsv(is, user, batch, successful);

                }

                // Final batch flush
                if (!batch.isEmpty()) {
                    flushBatch(batch, successful);
                }

                log.info("Imported {} documents from {} ({})", successful.size(), s3Url, isExcel ? "Excel" : "CSV");
                return successful;

            } catch (AmazonS3Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot access S3 file", e);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read file", e);
            }
        }

        private void processCsv(InputStream is, User user,
                                List<ProductRequiredDocuments> batch,
                                List<ProductRequiredDocumentResponseDto> successful)
                throws IOException {

            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                 CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT
                         .builder()
                         .setHeader()               // automatically detect header row
                         .setSkipHeaderRecord(true) // skip header in data iteration
                         .setIgnoreHeaderCase(true)
                         .setTrim(true)
                         .build())) {

                // Header → column index map
                Map<String, Integer> columnMap = parser.getHeaderMap();

                if (columnMap == null || columnMap.isEmpty()) {
                    throw new IllegalArgumentException("CSV file has no header row");
                }

                log.info("Detected CSV columns: {}", columnMap.keySet());

                int rowNum = 1;
                for (CSVRecord record : parser) {
                    rowNum++;

                    try {
                        ProductRequiredDocumentRequestDto dto =
                                mapCsvRecordToDto(record, columnMap, user.getId());

                        validateUniqueConstraint(dto, null);

                        ProductRequiredDocuments entity = new ProductRequiredDocuments();
                        mapDtoToEntity(dto, entity);
                        entity.setActive(true);
                        entity.setDeleted(false);
                        entity.setCreatedDate(new Date());
                        entity.setUpdatedDate(new Date());

                        entity.setCreatedBy(user.getId());
                        entity.setUpdatedBy(user.getId());
                        batch.add(entity);

                        if (batch.size() >= 200) {
                            flushBatch(batch, successful);
                        }

                    } catch (ValidationException vex) {
                        log.warn("CSV row {} skipped: {}", rowNum, vex.getMessage());
                    } catch (Exception ex) {
                        log.error("CSV row {} failed: {}", rowNum, ex.getMessage(), ex);
                    }
                }
            }
        }

        private ProductRequiredDocumentRequestDto mapCsvRecordToDto(CSVRecord record,
                                                                    Map<String, Integer> colMap,
                                                                    Long createdBy) {
            ProductRequiredDocumentRequestDto dto = new ProductRequiredDocumentRequestDto();

            dto.setName(getCsvString(record, colMap, "name", "document_name", "documentname", "doc_name"));
            dto.setDescription(getCsvString(record, colMap, "description", "document_description", "document_descition", "desc"));
            dto.setType(getCsvString(record, colMap, "type", "document_type"));
            dto.setCountry(getCsvString(record, colMap, "country"));
            dto.setCentralName(getCsvString(record, colMap, "centralname", "central_name"));
            dto.setStateName(getCsvString(record, colMap, "statename", "state_name"));
            dto.setAllowedFormats(getCsvString(record, colMap, "allowedformats", "allowed_formats"));
            dto.setApplicability(getCsvString(record, colMap, "applicability", "applicable_to", "applies_to"));
            dto.setRemarks(getCsvString(record, colMap, "remarks", "notes", "remark", "note"));


            String expiryStr = getCsvString(record, colMap, "expirytype", "expiry_type", "expiry");
            DocumentExpiryType expiryType = DocumentExpiryType.UNKNOWN;

            if (StringUtils.hasText(expiryStr)) {
                try {
                    expiryType = DocumentExpiryType.valueOf(expiryStr.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid expiryType in CSV: '{}'", expiryStr);
                    expiryType = DocumentExpiryType.UNKNOWN;
                }
            }
            dto.setExpiryType(expiryType);

            dto.setMandatory(getCsvBoolean(record, colMap, "mandatory", "required", "is_mandatory"));
            dto.setMaxValidityYears(getCsvInteger(record, colMap, "maxvalidityyears", "max_validity_years"));
            dto.setMinFileSizeKb(getCsvInteger(record, colMap, "minfilesizekb", "min_file_size_kb"));

            dto.setCreatedBy(createdBy);
            dto.setUpdatedBy(createdBy);

            if (!StringUtils.hasText(dto.getName())) {
                throw new ValidationException("Name / Document_Name is required", "ERR_NAME_REQUIRED");
            }

            if (dto.getExpiryType() == null || dto.getExpiryType() == DocumentExpiryType.UNKNOWN) {
                throw new ValidationException("Valid expiryType is required", "ERR_EXPIRY_TYPE_REQUIRED");
            }

            return dto;
        }

        // CSV-specific helper methods
        private String getCsvString(CSVRecord record, Map<String, Integer> map, String... possibleKeys) {
            for (String key : possibleKeys) {
                Integer idx = map.get(key.toLowerCase());
                if (idx != null && idx < record.size()) {
                    String val = record.get(idx).trim();
                    return val.isEmpty() ? null : val;
                }
            }
            return null;
        }

        private Boolean getCsvBoolean(CSVRecord record, Map<String, Integer> map, String... keys) {
            String val = getCsvString(record, map, keys);
            if (val == null) return false;
            return "true".equalsIgnoreCase(val) || "yes".equalsIgnoreCase(val) || "1".equals(val);
        }

        private Integer getCsvInteger(CSVRecord record, Map<String, Integer> map, String... keys) {
            String val = getCsvString(record, map, keys);
            if (!StringUtils.hasText(val)) return null;
            try {
                return Integer.parseInt(val.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private void flushBatch(List<ProductRequiredDocuments> batch, List<ProductRequiredDocumentResponseDto> successful) {
            var saved = productRequiredDocumentRepository.saveAll(batch);
            saved.forEach(e -> successful.add(mapToResponseDto(e)));
            batch.clear();
        }
        private void processExcel(InputStream is, User user,
                                  List<ProductRequiredDocuments> batch,
                                  List<ProductRequiredDocumentResponseDto> successful) throws IOException {

            try (Workbook workbook = WorkbookFactory.create(is)) {
                Sheet sheet = workbook.getSheetAt(0);
                if (sheet == null) throw new IllegalArgumentException("No sheets found");

                Row headerRow = sheet.getRow(0);
                if (headerRow == null) throw new IllegalArgumentException("No header row");

                Map<String, Integer> columnMap = new HashMap<>();
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    Cell cell = headerRow.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    String header = cell.getStringCellValue().trim().toLowerCase().replaceAll("\\s+", "_");
                    if (!header.isEmpty()) {
                        columnMap.put(header, i);
                    }
                }

                log.info("Detected columns: {}", columnMap.keySet());

                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    try {
                        ProductRequiredDocumentRequestDto dto =
                                mapRowToDto(row, columnMap, user.getId());
                        validateUniqueConstraint(dto, null);

                        ProductRequiredDocuments entity = new ProductRequiredDocuments();
                        mapDtoToEntity(dto, entity);
                        entity.setActive(true);
                        entity.setDeleted(false);
                        entity.setCreatedDate(new Date());
                        entity.setUpdatedDate(new Date());

                        entity.setCreatedBy(user.getId());
                        entity.setUpdatedBy(user.getId());

                        batch.add(entity);

                        if (batch.size() >= 200) {
                            flushBatch(batch, successful);
                        }

                    } catch (ValidationException vex) {
                        log.warn("Row {} skipped: {}", i + 1, vex.getMessage());
                    } catch (Exception ex) {
                        log.error("Row {} failed: {}", i + 1, ex.getMessage(), ex);
                    }
                }
            }
        }

        private ProductRequiredDocumentRequestDto mapRowToDto(Row row, Map<String, Integer> colMap, Long createdBy) {
            ProductRequiredDocumentRequestDto dto = new ProductRequiredDocumentRequestDto();

            dto.setName(getCellString(row, colMap, "name", "document_name", "documentname", "doc_name"));
            dto.setDescription(getCellString(row, colMap, "description", "document_description", "document_descition", "desc"));
            dto.setType(getCellString(row, colMap, "type", "document_type"));
            dto.setCountry(getCellString(row, colMap, "country"));
            dto.setCentralName(getCellString(row, colMap, "centralname", "central_name"));
            dto.setStateName(getCellString(row, colMap, "statename", "state_name"));
            dto.setAllowedFormats(getCellString(row, colMap, "allowedformats", "allowed_formats"));

            // ─── Special handling for enum ──────────────────────────────────────
            String expiryStr = getCellString(row, colMap, "expirytype", "expiry_type", "expiry");
            if (StringUtils.hasText(expiryStr)) {
                try {
                    dto.setExpiryType(DocumentExpiryType.valueOf(expiryStr.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid expiryType value: '{}'", expiryStr);
                    dto.setExpiryType(DocumentExpiryType.UNKNOWN);
                }
            } else {
                dto.setExpiryType(DocumentExpiryType.UNKNOWN);
            }

            dto.setMandatory(getCellBoolean(row, colMap, "mandatory", "required", "is_mandatory"));
            dto.setMaxValidityYears(getCellInteger(row, colMap, "maxvalidityyears", "max_validity_years"));
            dto.setMinFileSizeKb(getCellInteger(row, colMap, "minfilesizekb", "min_file_size_kb"));

            dto.setCreatedBy(createdBy);
            dto.setUpdatedBy(createdBy);

            if (!StringUtils.hasText(dto.getName())) {
                throw new ValidationException("Name / Document_Name is required", "ERR_NAME_REQUIRED");
            }

            // Optional: add check for mandatory fields like type, expiryType
            if (dto.getExpiryType() == null || dto.getExpiryType() == DocumentExpiryType.UNKNOWN) {
                throw new ValidationException("Valid expiryType is required", "ERR_EXPIRY_TYPE_REQUIRED");
            }

            return dto;
        }

        private String getCellString(Row row, Map<String, Integer> map, String... possibleKeys) {
            for (String key : possibleKeys) {
                Integer idx = map.get(key.toLowerCase());
                if (idx != null) {
                    Cell cell = row.getCell(idx);
                    if (cell != null) {
                        return switch (cell.getCellType()) {
                            case STRING -> cell.getStringCellValue().trim();
                            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
                            default -> "";
                        };
                    }
                }
            }
            return null;
        }

        private Boolean getCellBoolean(Row row, Map<String, Integer> map, String... keys) {
            String val = getCellString(row, map, keys);
            if (val == null) return false;
            return "true".equalsIgnoreCase(val) || "yes".equalsIgnoreCase(val) || "1".equals(val);
        }

        private Integer getCellInteger(Row row, Map<String, Integer> map, String... keys) {
            String val = getCellString(row, map, keys);
            if (!StringUtils.hasText(val)) return null;
            try {
                return Integer.parseInt(val.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }


        @Override
        public List<ProductRequiredDocumentResponseDto> getActivePaginated(Long userId, int page, int size) {
            page = Math.max(page, 1);
            size = size > 0 ? size : 20;

            User user = userRepository.findById(userId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("User not found", "USER_NOT_FOUND"));


            Pageable pageable = PageRequest.of(page - 1, size, Sort.by("name").ascending());
            return productRequiredDocumentRepository.findAllByIsDeletedFalseAndIsActiveTrue(pageable)  // FIXED
                    .getContent()
                    .stream()
                    .map(this::mapToResponseDto)
                    .toList();
        }

        private ProductRequiredDocuments findActiveById(Long id) {
            return productRequiredDocumentRepository.findByIdAndIsDeletedFalse(id)  // FIXED
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Required document template not found with ID: " + id,
                            "ERR_REQ_DOC_NOT_FOUND"));
        }

        // Unique constraint validation
        private void validateUniqueConstraint(ProductRequiredDocumentRequestDto dto, Long excludeId) {
            String name = dto.getName() != null ? dto.getName().trim() : "";
            String country = dto.getCountry() != null ? dto.getCountry().trim() : "";
            String centralName = dto.getCentralName() != null ? dto.getCentralName().trim() : "";
            String stateName = dto.getStateName() != null ? dto.getStateName().trim() : "";

            boolean exists = excludeId == null
                    ? productRequiredDocumentRepository.existsByNameAndCountryAndCentralNameAndStateNameAndIsDeletedFalse(
                    name, country, centralName, stateName)
                    : productRequiredDocumentRepository.existsByNameAndCountryAndCentralNameAndStateNameAndIsDeletedFalseAndIdNot(
                    name, country, centralName, stateName, excludeId);

            if (exists) {
                throw new ValidationException(
                        "A document template with the same name and location already exists.",
                        "ERR_DUPLICATE_REQ_DOCUMENT");
            }
        }

        private void mapDtoToEntity(ProductRequiredDocumentRequestDto dto, ProductRequiredDocuments entity) {
            entity.setName(dto.getName().trim());
            entity.setDescription(dto.getDescription());
            entity.setType(dto.getType());
            entity.setCountry(dto.getCountry() != null ? dto.getCountry().trim() : "");
            entity.setCentralName(dto.getCentralName() != null ? dto.getCentralName().trim() : "");
            entity.setStateName(dto.getStateName() != null ? dto.getStateName().trim() : "");
            entity.setExpiryType(dto.getExpiryType());
            entity.setMandatory(dto.isMandatory());
            entity.setMaxValidityYears(dto.getMaxValidityYears());

            // File size fields
            entity.setMinFileSizeKb(dto.getMinFileSizeKb());
            entity.setMaxFileSizeKb(dto.getMaxFileSizeKb());   // ← NEW

            entity.setAllowedFormats(dto.getAllowedFormats());

            // NEW FIELDS
            entity.setApplicability(dto.getApplicability());
            entity.setRemarks(dto.getRemarks());
        }
        private ProductRequiredDocumentResponseDto mapToResponseDto(ProductRequiredDocuments entity) {
            ProductRequiredDocumentResponseDto dto = new ProductRequiredDocumentResponseDto();

            dto.setId(entity.getId());
            dto.setName(entity.getName());
            dto.setDescription(entity.getDescription());
            dto.setType(entity.getType());
            dto.setCountry(entity.getCountry());
            dto.setCentralName(entity.getCentralName());
            dto.setStateName(entity.getStateName());
            dto.setExpiryType(entity.getExpiryType());
            dto.setMandatory(entity.isMandatory());
            dto.setMaxValidityYears(entity.getMaxValidityYears());

            dto.setMinFileSizeKb(entity.getMinFileSizeKb());
            dto.setMaxFileSizeKb(entity.getMaxFileSizeKb());   // ← NEW

            dto.setAllowedFormats(entity.getAllowedFormats());

            // NEW FIELDS
            dto.setApplicability(entity.getApplicability());
            dto.setRemarks(entity.getRemarks());

            dto.setCreatedBy(entity.getCreatedBy());
            dto.setUpdatedBy(entity.getUpdatedBy());
            dto.setCreatedDate(entity.getCreatedDate());
            dto.setUpdatedDate(entity.getUpdatedDate());
            dto.setActive(entity.isActive());

            return dto;
        }
    }
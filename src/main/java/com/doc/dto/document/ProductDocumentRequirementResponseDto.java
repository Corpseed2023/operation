package com.doc.dto.document;

import com.doc.dto.document.ProductDocumentGroupDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// ProductDocumentRequirementResponseDto.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDocumentRequirementResponseDto {
    private Long productId;
    private String productName;
    private List<ProductDocumentGroupDto> documentGroups;  // Changed from flat list
}
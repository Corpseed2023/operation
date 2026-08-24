package com.doc.dto.vendor;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VendorMappedProductDto {

    private Long productId;

    private String productName;

    private boolean mappingActive;

    private boolean productActive;
}

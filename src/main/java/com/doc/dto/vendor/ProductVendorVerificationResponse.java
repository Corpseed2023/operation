package com.doc.dto.vendor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductVendorVerificationResponse {

   /* private Long productId;
    private List<VendorVerificationResponse> verifiedVendors;
    private List<VendorVerificationResponse> notVerifiedVendors;*/

    private Long productId;

    private Integer verified;

    private Integer notVerified;
}
